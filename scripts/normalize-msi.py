#!/usr/bin/env python3
# Normalize the non-deterministic bytes of a jpackage/WiX-built MSI (#190).
#
# Two builds of one commit produce different msi files because the MSI
# format embeds per-build values that jpackage exposes no control over:
#
#   1. the package code GUID (SummaryInformation PID_REVNUMBER, which the
#      WiX linker generates randomly per link),
#   2. FILETIME properties in the SummaryInformation stream
#      (PID_LASTPRINTED / PID_CREATE_DTM / PID_LASTSAVE_DTM),
#   3. creation/modification FILETIMEs on every compound-file (CFB)
#      directory entry, and
#   4. per-member DOS date/time stamps inside the embedded cabinet
#      stream(s), taken from the staged tree's mtimes at packaging time.
#
# This script rewrites exactly those regions in place, with same-length
# patches only (the file's structure, offsets and size never change):
#
#   - CFB directory timestamps are zeroed ("not set" per MS-CFB),
#   - SummaryInformation FILETIMEs and CAB member times are clamped to
#     SOURCE_DATE_EPOCH (env var, or --source-date-epoch),
#   - the package code is replaced by a GUID derived from the SHA-256 of
#     the file itself with the package-code bytes masked -- so equal
#     inputs get equal package codes, and any content change yields a
#     fresh package code (which is what Windows Installer's caching
#     semantics require of distinct packages).
#
# It deliberately does NOT touch the ProductCode/UpgradeCode rows in the
# MSI database (jpackage derives those name-based, so they are expected
# to be stable already; the Windows double-build measurement verifies
# that).  Anything structurally unexpected is a hard error, never a
# silent skip: a file this script exits 0 on has had the full known
# volatile set normalized.
#
# Usage:
#   normalize-msi.py [--source-date-epoch N] FILE.msi
#   normalize-msi.py --self-test
#   normalize-msi.py --diff A.msi B.msi   # attribute a residual two-build
#                                         # divergence to the exact stream
#                                         # (and, in the cabinet, to CFFILE
#                                         # metadata vs compressed CFDATA)
#
# Stdlib only, so it runs on a bare GitHub runner (Linux/macOS/Windows
# git-bash) with no pip installs.  --self-test builds synthetic
# MSI-shaped compound files in memory and proves convergence (two inputs
# differing only in the volatile set normalize to identical bytes),
# idempotence, content-sensitivity of the derived GUID, and refusal of
# malformed input; the Windows lane of scripts/build-installer.sh runs
# it before trusting the tool with a real msi.

import hashlib
import os
import re
import struct
import sys

# --- CFB (MS-CFB compound file) constants ----------------------------------

CFB_MAGIC = b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1"
ENDOFCHAIN = 0xFFFFFFFE
FREESECT = 0xFFFFFFFF
FATSECT = 0xFFFFFFFD
DIFSECT = 0xFFFFFFFC
DIRENTRY_SIZE = 128
SUMMARY_NAME = "\x05SummaryInformation"

# SummaryInformation property ids and the property types we accept
PID_LASTPRINTED = 11
PID_CREATE_DTM = 12
PID_LASTSAVE_DTM = 13
PID_REVNUMBER = 9  # the MSI package code
VT_LPSTR = 30
VT_FILETIME = 64

GUID_RE = re.compile(rb"^\{[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}\}$")

EPOCH_TO_FILETIME = 11644473600  # seconds between 1601-01-01 and 1970-01-01
DOS_EPOCH_FLOOR = 315532800  # 1980-01-01T00:00:00Z, the earliest DOS time


class NormalizeError(Exception):
	"""Structural problem that makes normalization unsafe; the file is left untouched."""


def u16(buf, off):
	"""Read a little-endian uint16 at off."""
	return struct.unpack_from("<H", buf, off)[0]


def u32(buf, off):
	"""Read a little-endian uint32 at off."""
	return struct.unpack_from("<I", buf, off)[0]


def filetime_bytes(epoch):
	"""Encode a unix epoch as an 8-byte Windows FILETIME."""
	return struct.pack("<Q", (epoch + EPOCH_TO_FILETIME) * 10_000_000)


def dos_datetime(epoch):
	"""Encode a unix epoch as (date, time) uint16s per the DOS/CAB convention (UTC, floored to 1980)."""
	import time as _time

	t = _time.gmtime(max(epoch, DOS_EPOCH_FLOOR))
	date = ((t.tm_year - 1980) << 9) | (t.tm_mon << 5) | t.tm_mday
	tim = (t.tm_hour << 11) | (t.tm_min << 5) | (t.tm_sec // 2)
	return date, tim


class Cfb:
	"""Just enough MS-CFB parsing to map stream offsets to file offsets for in-place patching."""

	def __init__(self, buf):
		"""Parse the header, FAT, miniFAT and directory of the compound file in buf."""
		if len(buf) < 512 or buf[:8] != CFB_MAGIC:
			raise NormalizeError("not a compound file (bad magic)")
		self.orig_len = len(buf)
		self.sector_size = 1 << u16(buf, 30)
		self.mini_size = 1 << u16(buf, 32)
		self.mini_cutoff = u32(buf, 56)
		# A conforming MS-CFB writer (jpackage/WiX for large installers) may
		# leave the physical file ending mid-sector: the final sector begins
		# within the file but only part of it is present on disk.  Windows and
		# olefile tolerate this by zero-filling reads past EOF.  Mirror that by
		# padding the in-memory *parse* buffer up to a whole-sector multiple, so
		# a FAT chain that legitimately ends on that partial sector is readable.
		# The padding lives only in self.buf (the parse view); normalize()
		# rebuilds its output from the caller's original-length buffer, so the
		# written file never grows and normalization stays idempotent.
		pad = (-len(buf)) % self.sector_size
		if pad:
			buf = buf + b"\x00" * pad
		self.buf = buf
		# highest sector number that is *fully* present in the padded buffer
		self.max_sector = len(self.buf) // self.sector_size - 2
		self.fat = self._read_fat()
		self.minifat = self._read_chain_words(u32(buf, 60))
		self.dir_sectors = self._chain(u32(buf, 48))
		self.entries = self._read_dir()
		root = self.entries[0]
		if root[1] != 5:
			raise NormalizeError("first directory entry is not the root storage")
		self.mini_runs = self._runs(self._chain(root[2]), self.sector_size)

	def _sector_off(self, sector):
		"""File offset of a regular sector."""
		if sector > self.max_sector:
			raise NormalizeError("sector %d beyond end of file" % sector)
		return self.sector_size * (sector + 1)

	def _read_fat(self):
		"""Collect the FAT as one flat list of uint32 entries, following the header DIFAT."""
		buf = self.buf
		fat_sectors = []
		for i in range(min(109, u32(buf, 44))):
			fat_sectors.append(u32(buf, 76 + 4 * i))
		difat = u32(buf, 68)
		seen = 0
		while difat not in (ENDOFCHAIN, FREESECT):
			seen += 1
			if seen > self.max_sector + 1:
				raise NormalizeError("DIFAT chain cycle")
			off = self._sector_off(difat)
			per = self.sector_size // 4 - 1
			for i in range(per):
				s = u32(buf, off + 4 * i)
				if s != FREESECT:
					fat_sectors.append(s)
			difat = u32(buf, off + 4 * per)
		fat = []
		for s in fat_sectors:
			off = self._sector_off(s)
			fat.extend(struct.unpack_from("<%dI" % (self.sector_size // 4), buf, off))
		return fat

	def _chain(self, start):
		"""Follow a FAT sector chain from start; returns the list of sector numbers."""
		out = []
		s = start
		while s != ENDOFCHAIN:
			if s >= len(self.fat) or s in (FREESECT, FATSECT, DIFSECT):
				raise NormalizeError("broken FAT chain at sector %#x" % s)
			out.append(s)
			if len(out) > self.max_sector + 1:
				raise NormalizeError("FAT chain cycle")
			s = self.fat[s]
		return out

	def _read_chain_words(self, start):
		"""Read a whole FAT chain (e.g. the miniFAT) as a flat list of uint32s."""
		if start in (ENDOFCHAIN, FREESECT):
			return []
		words = []
		for s in self._chain(start):
			off = self._sector_off(s)
			words.extend(struct.unpack_from("<%dI" % (self.sector_size // 4), self.buf, off))
		return words

	def _read_dir(self):
		"""Read all directory entries as (name, type, start, size, entry_file_offset) tuples."""
		entries = []
		for s in self.dir_sectors:
			off = self._sector_off(s)
			for i in range(self.sector_size // DIRENTRY_SIZE):
				e = off + i * DIRENTRY_SIZE
				etype = self.buf[e + 66]
				namelen = u16(self.buf, e + 64)
				if etype == 0 or namelen < 2 or namelen > 64:
					continue
				name = self.buf[e : e + namelen - 2].decode("utf-16-le", "replace")
				start = u32(self.buf, e + 116)
				size = u32(self.buf, e + 120)  # low 32 bits; MSIs are < 4 GiB
				entries.append((name, etype, start, size, e))
		if not entries:
			raise NormalizeError("empty compound-file directory")
		return entries

	def _runs(self, sectors, unit):
		"""Turn a sector list into (file_offset, length) runs of the given unit size."""
		return [(self._sector_off(s), unit) for s in sectors]

	def stream_runs(self, entry):
		"""Map a stream entry to its (file_offset, length) runs, honoring the mini-stream cutoff."""
		name, etype, start, size, _ = entry
		if size == 0:
			return []
		if size >= self.mini_cutoff:
			return self._runs(self._chain(start), self.sector_size)
		# mini stream: chain through the miniFAT, then translate each
		# mini sector into the root entry's mini-stream runs
		runs = []
		s = start
		steps = 0
		while s != ENDOFCHAIN:
			if s >= len(self.minifat) or s == FREESECT:
				raise NormalizeError("broken miniFAT chain in stream %r" % name)
			runs.append((self.translate(self.mini_runs, s * self.mini_size), self.mini_size))
			steps += 1
			if steps > len(self.minifat) + 1:
				raise NormalizeError("miniFAT chain cycle in stream %r" % name)
			s = self.minifat[s]
		return runs

	@staticmethod
	def translate(runs, stream_off):
		"""Translate a stream-relative offset into a file offset via runs."""
		for foff, length in runs:
			if stream_off < length:
				return foff + stream_off
			stream_off -= length
		raise NormalizeError("offset beyond stream")

	@staticmethod
	def read(buf, runs, size):
		"""Read the first size bytes of a stream described by runs."""
		out = bytearray()
		for foff, length in runs:
			out += buf[foff : foff + min(length, size - len(out))]
			if len(out) >= size:
				break
		if len(out) < size:
			raise NormalizeError("stream shorter than its directory size")
		return bytes(out)

	@staticmethod
	def patch_offsets(runs, stream_off, length):
		"""File offsets, byte by byte grouped into contiguous ranges, for [stream_off, stream_off+length)."""
		ranges = []
		for i in range(length):
			f = Cfb.translate(runs, stream_off + i)
			if ranges and ranges[-1][0] + ranges[-1][1] == f:
				ranges[-1][1] += 1
			else:
				ranges.append([f, 1])
		return ranges

	@staticmethod
	def stream_patches(runs, stream_off, data):
		"""Split one stream-relative replacement into per-file-offset (offset, bytes) patches."""
		out = []
		pos = 0
		for foff, ln in Cfb.patch_offsets(runs, stream_off, len(data)):
			out.append((foff, data[pos : pos + ln]))
			pos += ln
		return out


def collect_patches(cfb, epoch):
	"""Compute every in-place patch except the package code.

	Returns (patches, guid_ranges) where patches is a list of
	(file_offset, replacement_bytes) and guid_ranges is the list of
	(file_offset, length) ranges holding the package-code GUID string.
	"""
	buf = cfb.buf
	patches = []

	# 1. zero the create/modify FILETIMEs on every directory entry
	for name, etype, start, size, e in cfb.entries:
		if buf[e + 100 : e + 116] != b"\x00" * 16:
			patches.append((e + 100, b"\x00" * 16))

	# 2. SummaryInformation: clamp FILETIME properties, locate the package code
	summary = [en for en in cfb.entries if en[0] == SUMMARY_NAME and en[1] == 2]
	if len(summary) != 1:
		raise NormalizeError("expected exactly one SummaryInformation stream, found %d" % len(summary))
	entry = summary[0]
	runs = cfb.stream_runs(entry)
	data = Cfb.read(buf, runs, entry[3])
	if len(data) < 48 or u16(data, 0) != 0xFFFE:
		raise NormalizeError("malformed property-set stream header")
	if u32(data, 24) < 1:
		raise NormalizeError("property-set stream has no sections")
	soff = u32(data, 44)  # first (and only relevant) section
	if soff + 8 > len(data):
		raise NormalizeError("property-set section offset out of range")
	nprops = u32(data, soff + 4)
	guid_ranges = None
	ft = filetime_bytes(epoch)
	for i in range(nprops):
		pid = u32(data, soff + 8 + 8 * i)
		poff = soff + u32(data, soff + 12 + 8 * i)
		if pid in (PID_LASTPRINTED, PID_CREATE_DTM, PID_LASTSAVE_DTM):
			if u32(data, poff) != VT_FILETIME:
				raise NormalizeError("property %d is not VT_FILETIME" % pid)
			if data[poff + 4 : poff + 12] != ft:
				patches.extend(Cfb.stream_patches(runs, poff + 4, ft))
		elif pid == PID_REVNUMBER:
			if u32(data, poff) != VT_LPSTR:
				raise NormalizeError("package code property is not VT_LPSTR")
			slen = u32(data, poff + 4)
			if not 38 <= slen <= 40:
				raise NormalizeError("package code has unexpected length %d" % slen)
			s = data[poff + 8 : poff + 8 + 38]
			if not GUID_RE.match(s):
				raise NormalizeError("package code %r is not a braced GUID" % s)
			guid_ranges = Cfb.patch_offsets(runs, poff + 8, 38)
	if guid_ranges is None:
		raise NormalizeError("SummaryInformation has no package code (PID 9)")

	# 3. embedded cabinets: clamp every CFFILE date/time
	dos_date, dos_time = dos_datetime(epoch)
	cab_members = 0
	for en in cfb.entries:
		name, etype, start, size, _ = en
		if etype != 2 or size < 44:
			continue
		cruns = cfb.stream_runs(en)
		cab = Cfb.read(buf, cruns, size)
		if cab[:4] != b"MSCF":
			continue
		flags = u16(cab, 30)
		if flags != 0:
			raise NormalizeError("cabinet stream %r has flags %#x (reserve/multi-part unsupported)" % (name, flags))
		off = u32(cab, 16)  # coffFiles
		nfiles = u16(cab, 28)
		want = struct.pack("<HH", dos_date, dos_time)
		for _i in range(nfiles):
			if off + 16 > len(cab):
				raise NormalizeError("cabinet stream %r: CFFILE beyond stream end" % name)
			if cab[off + 10 : off + 14] != want:
				patches.extend(Cfb.stream_patches(cruns, off + 10, want))
			end = cab.index(b"\x00", off + 16) + 1  # skip past szName
			off = end
			cab_members += 1
	return patches, guid_ranges, cab_members


def derived_guid(buf, guid_ranges):
	"""Derive the deterministic package code: SHA-256 of the file with the GUID bytes masked."""
	masked = bytearray(buf)
	for foff, ln in guid_ranges:
		masked[foff : foff + ln] = b"\x00" * ln
	d = hashlib.sha256(bytes(masked)).digest()
	h = d[:16].hex().upper()
	return ("{%s-%s-%s-%s-%s}" % (h[0:8], h[8:12], h[12:16], h[16:20], h[20:32])).encode("ascii")


def normalize(buf, epoch):
	"""Normalize one MSI image; returns (new_bytes, stats dict)."""
	try:
		cfb = Cfb(bytes(buf))
		patches, guid_ranges, cab_members = collect_patches(cfb, epoch)
	except (struct.error, IndexError, ValueError) as e:
		raise NormalizeError("malformed compound file: %s" % e) from e
	# Every rewrite must land in bytes that physically exist in the input.  The
	# parse buffer may have been zero-padded past EOF for a partial final
	# sector; a patch or GUID range in that padded tail would extend the output
	# and break length-preservation/idempotence, so refuse rather than grow.
	for foff, rep in patches:
		if foff + len(rep) > cfb.orig_len:
			raise NormalizeError("patch at %d extends past end of file" % foff)
	for foff, ln in guid_ranges:
		if foff + ln > cfb.orig_len:
			raise NormalizeError("package-code range at %d extends past end of file" % foff)
	out = bytearray(buf)
	for foff, rep in patches:
		out[foff : foff + len(rep)] = rep
	guid = derived_guid(bytes(out), guid_ranges)
	changed_guid = False
	pos = 0
	for foff, ln in guid_ranges:
		if out[foff : foff + ln] != guid[pos : pos + ln]:
			changed_guid = True
		out[foff : foff + ln] = guid[pos : pos + ln]
		pos += ln
	return bytes(out), {
		"patches": len(patches),
		"cab_members": cab_members,
		"package_code": guid.decode("ascii"),
		"changed": bool(patches) or changed_guid,
	}


def _stream_inventory(cfb):
	"""Ordered list of (name, size, sha256hex, bytes) for every stream entry."""
	out = []
	for en in cfb.entries:
		name, etype, start, size, _ = en
		if etype != 2:  # streams only, skip storages
			continue
		data = Cfb.read(cfb.buf, cfb.stream_runs(en), size) if size else b""
		out.append((name, size, hashlib.sha256(data).hexdigest(), data))
	return out


def _first_diff(a, b):
	"""Index of the first differing byte, or -1 if equal; len(shorter) if one is a prefix."""
	for i in range(min(len(a), len(b))):
		if a[i] != b[i]:
			return i
	return -1 if len(a) == len(b) else min(len(a), len(b))


def _cab_files(cab):
	"""Parse a CAB's CFFILE table into (name, cbFile, uoffFolderStart, iFolder, date, time, attribs)."""
	files = []
	if cab[:4] != b"MSCF":
		return files
	off = u32(cab, 16)  # coffFiles
	nfiles = u16(cab, 28)
	for _ in range(nfiles):
		if off + 16 > len(cab):
			break
		rec = (u32(cab, off), u32(cab, off + 4), u16(cab, off + 8),
			u16(cab, off + 10), u16(cab, off + 12), u16(cab, off + 14))
		nul = cab.find(b"\x00", off + 16)
		if nul < 0:
			break
		name = cab[off + 16 : nul].decode("latin-1", "replace")
		files.append((name,) + rec)
		off = nul + 1
	return files


def _describe_cab_divergence(a, b):
	"""Localize where two differing CAB streams diverge: metadata vs compressed payload."""
	lines = []
	for tag, cab in (("A", a), ("B", b)):
		if cab[:4] != b"MSCF":
			lines.append("    %s: not a cabinet (%d bytes)" % (tag, len(cab)))
			continue
		lines.append("    %s: MSCF len=%d cbCabinet=%d coffFiles=%d nfiles=%d firstCFDATA=%d"
			% (tag, len(cab), u32(cab, 8), u32(cab, 16), u16(cab, 28),
			   u32(cab, 36) if len(cab) >= 40 else -1))
	fa, fb = _cab_files(a), _cab_files(b)
	if [f[0] for f in fa] != [f[0] for f in fb]:
		lines.append("    CFFILE name/order DIFFERS between builds (payload sequencing is non-deterministic)")
	else:
		meta = [fa[i][0] for i in range(min(len(fa), len(fb))) if fa[i] != fb[i]]
		if meta:
			lines.append("    CFFILE metadata differs for %d member(s): %s"
				% (len(meta), ", ".join(meta[:8]) + (" ..." if len(meta) > 8 else "")))
		else:
			lines.append("    CFFILE table (names, order, sizes, times, attribs) is IDENTICAL")
	d = _first_diff(a, b)
	if d >= 0 and a[:4] == b"MSCF" and len(a) >= 40:
		coff_files = u32(a, 16)
		first_cfdata = u32(a, 36)
		if d < coff_files:
			region = "CFHEADER/CFFOLDER"
		elif d < first_cfdata:
			region = "CFFILE table"
		else:
			region = "CFDATA (compressed payload/checksums)"
		lines.append("    first differing byte at CAB offset %d -> %s" % (d, region))
	return "\n".join(lines)


# MSI encodes table/stream names into the private Unicode range 0x3800-0x4840
# (two base-64 symbols per code unit, then singles), so a raw name carries
# characters no 8-bit console codec can print.  Decode to the real name.
_MSI_B64 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._"


def _decode_streamname(name):
	"""Decode an MSI-mangled stream/table name to its readable form.

	0x4840 is the leading sentinel marking a database table stream; the
	0x3800-0x47FF range packs two base-64 symbols per code unit and
	0x4800-0x483F one, so e.g. the Property table's stream decodes to
	'Property'.  Anything else (\\x05SummaryInformation, etc.) is literal.
	"""
	out = []
	for ch in name:
		c = ord(ch)
		if c == 0x4840:  # table sentinel, carries no character
			continue
		if 0x3800 <= c < 0x4800:
			c -= 0x3800
			out.append(_MSI_B64[c & 0x3F])
			out.append(_MSI_B64[(c >> 6) & 0x3F])
		elif 0x4800 <= c < 0x4840:
			out.append(_MSI_B64[(c - 0x4800) & 0x3F])
		else:
			out.append(ch)
	return "".join(out)


def _safe_name(name):
	"""A console-safe label for a stream: decoded MSI name, ASCII-escaped, table-tagged."""
	decoded = _decode_streamname(name)
	tag = " [table]" if name and ord(name[0]) == 0x4840 else ""
	shown = decoded.encode("ascii", "backslashreplace").decode("ascii")
	return "%r%s" % (shown, tag)


def diff_msis(path_a, path_b):
	"""Print a per-stream divergence report for two (normalized) MSIs; returns exit code."""
	# MSI stream names carry private-use Unicode; force a codec that can render
	# them so the diagnostic never dies on a Windows cp1252 console.
	for stream in (sys.stdout, sys.stderr):
		try:
			stream.reconfigure(encoding="utf-8", errors="backslashreplace")
		except (AttributeError, ValueError):
			pass
	with open(path_a, "rb") as f:
		a = f.read()
	with open(path_b, "rb") as f:
		b = f.read()
	if a == b:
		print("normalize-msi diff: %s and %s are byte-identical" % (path_a, path_b))
		return 0
	print("normalize-msi diff: %s vs %s (len %d vs %d)" % (path_a, path_b, len(a), len(b)))
	try:
		ia = {n: (sz, h, data) for n, sz, h, data in _stream_inventory(Cfb(a))}
		ib = {n: (sz, h, data) for n, sz, h, data in _stream_inventory(Cfb(b))}
	except NormalizeError as e:
		print("  (could not parse as CFB: %s) first byte diff at %d" % (e, _first_diff(a, b)))
		return 0
	names = sorted(set(ia) | set(ib))
	any_stream_diff = False
	for name in names:
		if name not in ia or name not in ib:
			print("  stream %s present in only one build" % _safe_name(name))
			any_stream_diff = True
			continue
		sza, ha, da = ia[name]
		szb, hb, db = ib[name]
		if ha == hb:
			continue
		any_stream_diff = True
		print("  stream %s DIFFERS (size %d vs %d, first byte diff at %d)"
			% (_safe_name(name), sza, szb, _first_diff(da, db)))
		if da[:4] == b"MSCF" or db[:4] == b"MSCF":
			print(_describe_cab_divergence(da, db))
	if not any_stream_diff:
		print("  no per-stream content difference found; divergence is in CFB envelope "
			"(FAT/dir/free sectors) at byte %d" % _first_diff(a, b))
	return 0


def source_date_epoch(argv_value):
	"""Resolve the timestamp to clamp to: --source-date-epoch flag, else env, else 0."""
	if argv_value is not None:
		return int(argv_value)
	return int(os.environ.get("SOURCE_DATE_EPOCH", "0"))


# --- self test --------------------------------------------------------------


def _synthetic_msi(guid, epoch_times, cab_times, payload_seed, sector_size=512, truncate_tail=False):
	"""Build a minimal but structurally valid MSI-shaped compound file for the self-test.

	sector_size selects the CFB layout: 512 -> version 3, 4096 -> version 4.
	truncate_tail drops all but a few bytes of the final (cabinet) sector so the
	image ends mid-sector, reproducing the real jpackage/WiX MSI shape from #190
	(a FAT chain that legitimately ends on a partially-present final sector)."""

	def dirent(name, etype, start, size, ctime=0, mtime=0, child=FREESECT):
		n = name.encode("utf-16-le")
		e = bytearray(128)
		e[0 : len(n)] = n
		struct.pack_into("<H", e, 64, len(n) + 2)
		e[66] = etype
		e[67] = 1
		struct.pack_into("<III", e, 68, FREESECT, FREESECT, child)
		struct.pack_into("<QQ", e, 100, ctime, mtime)
		struct.pack_into("<IQ", e, 116, start, size)
		return bytes(e)

	# SummaryInformation property set: PIDs 9, 11, 12, 13
	ft = filetime_bytes(epoch_times)
	sec = struct.pack("<II", 124, 4)
	sec += struct.pack("<II", PID_REVNUMBER, 40)
	sec += struct.pack("<II", PID_LASTPRINTED, 88)
	sec += struct.pack("<II", PID_CREATE_DTM, 100)
	sec += struct.pack("<II", PID_LASTSAVE_DTM, 112)
	sec += struct.pack("<II", VT_LPSTR, 39) + guid + b"\x00\x00"
	for _ in range(3):
		sec += struct.pack("<I", VT_FILETIME) + ft
	fmtid = struct.pack("<IHH8B", 0xF29F85E0, 0x4FF9, 0x1068, 0xAB, 0x91, 0x08, 0x00, 0x2B, 0x27, 0xB3, 0xD9)
	summary = struct.pack("<HHI", 0xFFFE, 0, 0x00020005) + b"\x00" * 16 + struct.pack("<I", 1) + fmtid + struct.pack("<I", 48) + sec
	assert len(summary) == 172

	# embedded cabinet: header + one folder + two CFFILEs + payload padding
	dd, dt = dos_datetime(cab_times)
	cab = bytearray()
	cab += b"MSCF" + struct.pack("<II", 0, 4608) + struct.pack("<II", 0, 44) + struct.pack("<I", 0)
	cab += struct.pack("<BBHHHHH", 3, 1, 1, 2, 0, 0x1234, 0)
	cab += struct.pack("<IH", 66, 0x4B4D)  # CFFOLDER: coffCabStart, cCFData|typeCompress
	cab += struct.pack("<H", 0)
	for nm in (b"a.txt", b"b.bin"):
		cab += struct.pack("<IIHHHH", 100, 0, 0, dd, dt, 0x20) + nm + b"\x00"
	rng = payload_seed
	while len(cab) < 4608:
		rng = (rng * 6364136223846793005 + 1442695040888963407) & (2**64 - 1)
		cab += struct.pack("<Q", rng)
	cab = bytes(cab[:4608])

	# Lay the streams out over whole sectors of the chosen size.  The sector
	# map is fixed regardless of sector size: 0=FAT, 1=directory, 2=miniFAT,
	# 3=mini-stream container, 4.. = the cabinet's data-sector chain.
	ss = sector_size
	entries_per_fat = ss // 4
	ncab = (len(cab) + ss - 1) // ss  # cabinet data sectors
	cab_first = 4
	total_sectors = cab_first + ncab
	assert total_sectors <= entries_per_fat, "self-test layout needs a single DIFAT-free FAT sector"

	fat = [FREESECT] * entries_per_fat
	fat[0] = FATSECT  # the FAT itself
	fat[1] = ENDOFCHAIN  # directory
	fat[2] = ENDOFCHAIN  # miniFAT
	fat[3] = ENDOFCHAIN  # mini-stream container
	for i in range(ncab):
		s = cab_first + i
		fat[s] = ENDOFCHAIN if i == ncab - 1 else s + 1
	minifat = [1, 2, ENDOFCHAIN] + [FREESECT] * (entries_per_fat - 3)

	major = 3 if ss == 512 else 4
	sector_shift = ss.bit_length() - 1
	hdr = bytearray(512)
	hdr[:8] = CFB_MAGIC
	struct.pack_into("<HHHHH", hdr, 24, 0x3E, major, 0xFFFE, sector_shift, 6)
	struct.pack_into("<IIIIIIII", hdr, 40, 0, 1, 1, 0, 4096, 2, 1, ENDOFCHAIN)
	struct.pack_into("<I", hdr, 72, 0)
	struct.pack_into("<I", hdr, 76, 0)
	for i in range(1, 109):
		struct.pack_into("<I", hdr, 76 + 4 * i, FREESECT)
	# the 512-byte header occupies the whole first sector of the file
	hdr = bytes(hdr) + b"\x00" * (ss - 512)

	def sector(data):
		assert len(data) <= ss
		return data + b"\x00" * (ss - len(data))

	fat_sec = sector(struct.pack("<%dI" % entries_per_fat, *fat))
	dir_data = (
		dirent("Root Entry", 5, 3, 192, ctime=epoch_times, mtime=epoch_times, child=2)
		+ dirent(SUMMARY_NAME, 2, 0, 172, mtime=epoch_times)
		+ dirent("Disk1Cab", 2, cab_first, 4608, mtime=epoch_times)
	)
	dir_sec = sector(dir_data)
	minifat_sec = sector(struct.pack("<%dI" % entries_per_fat, *minifat))
	mini_sec = sector(summary)  # mini-stream container: summary in mini sectors 0..2
	cab_region = cab + b"\x00" * (ncab * ss - len(cab))

	image = hdr + fat_sec + dir_sec + minifat_sec + mini_sec + cab_region
	assert len(image) == (1 + total_sectors) * ss
	if truncate_tail:
		# End the physical file a few bytes into the final cab sector, so the
		# last referenced sector begins within the file but is not fully
		# present -- the exact #190 shape.  Only zero payload padding is lost.
		image = image[: len(image) - ss + 16]
	return image


def self_test():
	"""Prove convergence, idempotence, content sensitivity and malformed-input refusal."""
	g1 = b"{11111111-2222-3333-4444-555555555555}"
	g2 = b"{AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE}"
	a = _synthetic_msi(g1, 1700000001, 1710000002, payload_seed=42)
	b = _synthetic_msi(g2, 1650000003, 1660000004, payload_seed=42)
	assert a != b
	epoch = 1500000000
	na, sa = normalize(a, epoch)
	nb, sb = normalize(b, epoch)
	assert na == nb, "two builds differing only in volatile bytes must converge"
	assert sa["changed"] and sa["cab_members"] == 2
	# idempotence: a normalized file is a fixed point
	nc, sc = normalize(na, epoch)
	assert nc == na and not sc["changed"], "normalization must be idempotent"
	# content sensitivity: different payload -> different package code
	c = _synthetic_msi(g1, 1700000001, 1710000002, payload_seed=43)
	ncc, scc = normalize(c, epoch)
	assert scc["package_code"] != sa["package_code"], "package code must track content"
	assert GUID_RE.match(sa["package_code"].encode("ascii"))
	# #190 regression: large real installers are version-4 (4096-byte-sector)
	# compound files whose physical length is not a whole number of sectors, so
	# the final cabinet sector begins within the file but is only partly present
	# (olefile/Windows zero-fill the tail; the parser must too).  Cover both
	# sector sizes, aligned and truncated, proving parse + convergence +
	# idempotence + length-preservation on every combination.  Before the fix
	# the truncated cases raised "sector N beyond end of file".
	for ss in (512, 4096):
		for trunc in (False, True):
			va = _synthetic_msi(g1, 1700000001, 1710000002, payload_seed=42, sector_size=ss, truncate_tail=trunc)
			vb = _synthetic_msi(g2, 1650000003, 1660000004, payload_seed=42, sector_size=ss, truncate_tail=trunc)
			if trunc:
				assert len(va) % ss != 0, "truncated shape must end mid-sector"
			nva, sva = normalize(va, epoch)
			nvb, svb = normalize(vb, epoch)
			assert nva == nvb, "sector=%d trunc=%s builds must converge" % (ss, trunc)
			assert sva["cab_members"] == 2 and sva["changed"]
			assert len(nva) == len(va), "normalization must preserve file length"
			nvc, svc = normalize(nva, epoch)
			assert nvc == nva and not svc["changed"], "sector=%d trunc=%s must be idempotent" % (ss, trunc)
	# --diff diagnostic: two builds differing only in payload localize to the
	# cabinet's compressed CFDATA, with the CFFILE table reported identical;
	# equal inputs report no per-stream difference.
	da = _synthetic_msi(g1, 1700000001, 1710000002, payload_seed=7, sector_size=4096)
	db = _synthetic_msi(g2, 1700000001, 1710000002, payload_seed=8, sector_size=4096)
	nda, _ = normalize(da, epoch)
	ndb, _ = normalize(db, epoch)
	assert nda != ndb, "distinct payloads must not converge"
	inv = {n: (sz, h, data) for n, sz, h, data in _stream_inventory(Cfb(nda))}
	assert SUMMARY_NAME in inv, "self-test image must carry a SummaryInformation stream"
	cab_a = next(d for n, (sz, h, d) in inv.items() if d[:4] == b"MSCF")
	cab_b = next(d for n, sz, h, d in _stream_inventory(Cfb(ndb)) if d[:4] == b"MSCF")
	report = _describe_cab_divergence(cab_a, cab_b)
	assert "CFDATA" in report and "IDENTICAL" in report, report
	assert _first_diff(b"abc", b"abc") == -1 and _first_diff(b"abc", b"abd") == 2
	# refusal: non-CFB and truncated inputs raise, never return garbage
	for bad in (b"not an msi at all" + b"\x00" * 600, a[:700]):
		try:
			normalize(bad, epoch)
		except NormalizeError:
			pass
		else:
			raise AssertionError("malformed input must be refused")
	print("normalize-msi self-test: ok (converged %d-byte images, %d patches)" % (len(na), sa["patches"]))


def main(argv):
	"""Command-line entry point; returns the process exit code."""
	args = list(argv[1:])
	epoch_arg = None
	if args and args[0] == "--self-test":
		self_test()
		return 0
	if args and args[0] == "--diff":
		if len(args) != 3:
			print("usage: normalize-msi.py --diff A.msi B.msi", file=sys.stderr)
			return 2
		return diff_msis(args[1], args[2])
	if len(args) >= 2 and args[0] == "--source-date-epoch":
		epoch_arg = args[1]
		args = args[2:]
	if len(args) != 1:
		print("usage: normalize-msi.py [--source-date-epoch N] FILE.msi | --self-test"
			" | --diff A.msi B.msi", file=sys.stderr)
		return 2
	path = args[0]
	try:
		epoch = source_date_epoch(epoch_arg)
	except ValueError:
		print("normalize-msi: --source-date-epoch/SOURCE_DATE_EPOCH must be an integer", file=sys.stderr)
		return 2
	with open(path, "rb") as f:
		buf = f.read()
	try:
		out, stats = normalize(buf, epoch)
	except NormalizeError as e:
		print("normalize-msi: %s: %s" % (path, e), file=sys.stderr)
		return 1
	if stats["changed"]:
		tmp = path + ".tmp"
		with open(tmp, "wb") as f:
			f.write(out)
		os.replace(tmp, path)
	print(
		"normalize-msi: %s: %s (%d timestamp patches, %d cab members checked, package code %s)"
		% (path, "normalized" if stats["changed"] else "already normalized", stats["patches"], stats["cab_members"], stats["package_code"])
	)
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv))
