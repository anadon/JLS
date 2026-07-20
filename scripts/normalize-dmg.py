#!/usr/bin/env python3
# Normalize the checksum-safe non-deterministic bytes of a jpackage/hdiutil
# built macOS dmg (#191).
#
# Two builds of one commit produce different dmg files because the UDIF
# envelope and the HFS+ volume inside it embed per-build values that
# jpackage (and the hdiutil sequence it drives) expose no control over.
# docs/dmg-reproducibility.md enumerates the full variance set; this tool
# owns exactly the two regions that can be rewritten *without* invalidating
# a UDIF checksum, because the hard rule of dmg normalization is: never
# touch a byte that a UDIF DataChecksum or MasterChecksum covers, or the
# image fails `hdiutil verify`/`attach`.  There are precisely two
# checksum-safe windows, and this tool implements one operation for each:
#
#   koly  --  the 512-byte UDIF trailer ("koly block") at the very end of
#             the dmg carries a 16-byte SegmentID (a UUID at trailer
#             offset 64) freshly randomized by every `hdiutil convert`.
#             It lives *outside* every UDIF checksum (DataChecksum and
#             MasterChecksum cover the data fork/blkx data, not the
#             trailer), so it is pinned on the final compressed dmg
#             without corrupting the image (E1 in the variance inventory).
#
#   hfs   --  the HFS+ primary volume header (1024 bytes into the volume)
#             and its alternate copy (1024 bytes before the volume end)
#             hold createDate/modifyDate/backupDate/checkedDate
#             (wall-clock) and a 64-bit volume UUID in finderInfo[6..7]
#             (randomized at volume creation) — F1 in the inventory.
#             hdiutil's UDRW output wraps the volume in a partition map
#             (GPT or APM), so the header is located at partition_start +
#             1024 by walking that map (with a bare single-volume image as
#             a fallback); each candidate is confirmed by the HFS+/HFSX
#             signature at BOTH the primary and alternate header before any
#             byte is touched.  These are patched on the *uncompressed*
#             read-write image BEFORE it is re-compressed with `hdiutil
#             convert`, so the UDIF checksums are (re)computed over the
#             already-normalized bytes.  Patching a finished UDZO image
#             would corrupt it.
#
# Both rewrites are same-length, in-place, and content-derived: the pinned
# SegmentID and volume UUID are taken from the SHA-256 of the image with
# exactly the volatile bytes masked, so two builds that differ only in the
# volatile set converge to identical bytes, and any genuine content change
# yields a fresh identifier.  Anything structurally unexpected is a hard
# error, never a silent skip — a file this tool exits 0 on has had its full
# in-scope volatile set normalized.  Deliberately NOT touched: allocation
# state (writeCount, nextAllocation, the bitmap), the data-fork bytes of a
# compressed image, and the `.DS_Store` bookmark blob — those are the
# documented bounded residual (F4/F6), owned by docs/dmg-reproducibility.md
# and the provenance attestation, not by a brittle rewrite.
#
# Usage:
#   normalize-dmg.py koly FILE.dmg
#   normalize-dmg.py hfs [--source-date-epoch N] RAW_OR_UDRW_IMAGE
#   normalize-dmg.py --self-test
#
# Stdlib only, so it runs on a bare GitHub runner (macOS or Linux) with no
# pip installs.  --self-test builds synthetic UDIF-trailer and HFS+-volume
# images in memory and proves convergence (two inputs differing only in the
# volatile set normalize to identical bytes), idempotence, content
# sensitivity of the derived identifiers, and refusal of malformed input;
# the macOS lane of scripts/build-installer.sh runs it before trusting the
# tool with a real dmg.

import hashlib
import os
import struct
import sys


class NormalizeError(Exception):
	"""Structural problem that makes normalization unsafe; the file is left untouched."""


# --- UDIF trailer ("koly block") -------------------------------------------

KOLY_MAGIC = b"koly"
KOLY_SIZE = 512
SEGMENT_ID_OFF = 64  # trailer-relative offset of the 16-byte SegmentID UUID
SEGMENT_ID_LEN = 16


def normalize_koly(buf):
	"""Pin the UDIF trailer SegmentID; returns (new_bytes, stats)."""
	if len(buf) < KOLY_SIZE:
		raise NormalizeError("file is shorter than a UDIF trailer (%d bytes)" % len(buf))
	trailer = len(buf) - KOLY_SIZE
	if buf[trailer : trailer + 4] != KOLY_MAGIC:
		raise NormalizeError("no 'koly' UDIF trailer at end of file")
	seg = trailer + SEGMENT_ID_OFF
	# derive a content-stable SegmentID: hash the whole image with the
	# SegmentID bytes masked, so equal images get equal ids and any change
	# yields a fresh one (mirrors normalize-msi.py's package-code rule)
	masked = bytearray(buf)
	masked[seg : seg + SEGMENT_ID_LEN] = b"\x00" * SEGMENT_ID_LEN
	pinned = hashlib.sha256(bytes(masked)).digest()[:SEGMENT_ID_LEN]
	out = bytearray(buf)
	changed = out[seg : seg + SEGMENT_ID_LEN] != pinned
	out[seg : seg + SEGMENT_ID_LEN] = pinned
	return bytes(out), {
		"segment_id": pinned.hex(),
		"changed": changed,
	}


# --- HFS+ volume header ----------------------------------------------------

HFS_PLUS_SIG = b"H+"  # HFSPlusVolumeHeader
HFSX_SIG = b"HX"  # HFSXVolumeHeader
VOLUME_HEADER_OFF = 1024  # bytes into the volume; alternate is size-1024
SECTOR = 512
# field offsets relative to the volume header start
OFF_CREATE_DATE = 16
OFF_MODIFY_DATE = 20
OFF_BACKUP_DATE = 24
OFF_CHECKED_DATE = 28
DATE_OFFSETS = (OFF_CREATE_DATE, OFF_MODIFY_DATE, OFF_BACKUP_DATE, OFF_CHECKED_DATE)
OFF_FINDER_INFO = 80  # UInt32 finderInfo[8]
OFF_VOLUME_UUID = OFF_FINDER_INFO + 24  # finderInfo[6..7] hold the 64-bit UUID
VOLUME_UUID_LEN = 8

# HFS+ epoch is 1904-01-01 00:00 GMT; seconds from there to the unix epoch
HFS_EPOCH_DELTA = 2082844800


def hfs_timestamp(epoch):
	"""Encode a unix epoch as a big-endian HFS+ 4-byte timestamp (seconds since 1904, clamped to the format range)."""
	t = epoch + HFS_EPOCH_DELTA
	if t < 0:
		t = 0
	if t > 0xFFFFFFFF:
		t = 0xFFFFFFFF
	return struct.pack(">I", t)


def _header_ok(buf, base):
	"""True when a bare HFS+/HFSX volume header sits at buf[base:]."""
	return 0 <= base and base + SECTOR <= len(buf) and buf[base : base + 2] in (HFS_PLUS_SIG, HFSX_SIG)


# --- partition-map location ------------------------------------------------
# hdiutil's UDRW output is not a bare volume: it wraps the HFS+ volume in a
# GUID Partition Table (GPT) or an Apple Partition Map (APM), so the volume
# header sits at partition_start + 1024, not image offset 1024. Locate the
# HFS+ volume by walking whichever map is present, then fall back to a bare
# image. Partition entries are validated by the actual HFS+/HFSX signature
# at the computed primary AND alternate header, so a wrong map/blocksize is
# rejected rather than silently patching the wrong bytes.

GPT_SIG = b"EFI PART"
APM_SIG = b"PM\x00\x00"[:2]  # "PM" partition-map-entry signature


def _gpt_partitions(buf, bs=SECTOR):
	"""(start_byte, size_byte) for each GPT partition entry, or None if not GPT."""
	if len(buf) < 2 * bs or buf[bs : bs + 8] != GPT_SIG:
		return None
	part_lba = struct.unpack_from("<Q", buf, bs + 72)[0]
	num = struct.unpack_from("<I", buf, bs + 80)[0]
	esize = struct.unpack_from("<I", buf, bs + 84)[0]
	if esize < 128 or num == 0 or num > 4096:
		return None
	out = []
	table = part_lba * bs
	for i in range(num):
		e = table + i * esize
		if e + 56 > len(buf):
			break
		if buf[e : e + 16] == b"\x00" * 16:
			continue  # unused entry
		first = struct.unpack_from("<Q", buf, e + 32)[0]
		last = struct.unpack_from("<Q", buf, e + 40)[0]
		if last >= first:
			out.append((first * bs, (last - first + 1) * bs))
	return out or None


def _apm_partitions(buf, bs=SECTOR):
	"""(start_byte, size_byte) for each Apple Partition Map entry, or None if not APM."""
	if len(buf) < 2 * bs or buf[0:2] != b"ER":  # driver descriptor record
		return None
	if buf[bs : bs + 2] != APM_SIG:
		return None
	map_blocks = struct.unpack_from(">I", buf, bs + 4)[0]
	out = []
	for i in range(1, min(map_blocks, 256) + 1):
		e = i * bs
		if e + 32 > len(buf) or buf[e : e + 2] != APM_SIG:
			break
		pystart = struct.unpack_from(">I", buf, e + 8)[0]
		blkcnt = struct.unpack_from(">I", buf, e + 12)[0]
		if blkcnt:
			out.append((pystart * bs, blkcnt * bs))
	return out or None


def _locate_hfs(buf):
	"""Return (primary_offset, alt_offset) of the HFS+ volume header, honoring a partition map.

	The alternate header is 1024 bytes before the end of the *volume*
	(partition), not the image. Falls back to a bare single-volume image.
	"""
	# bare image: header at 1024, alternate 1024 before the image end
	if _header_ok(buf, VOLUME_HEADER_OFF) and _header_ok(buf, len(buf) - VOLUME_HEADER_OFF):
		return VOLUME_HEADER_OFF, len(buf) - VOLUME_HEADER_OFF
	# partition-mapped image (what hdiutil actually produces)
	parts = _gpt_partitions(buf) or _apm_partitions(buf)
	if parts:
		for start, psize in parts:
			primary = start + VOLUME_HEADER_OFF
			alt = start + psize - VOLUME_HEADER_OFF
			if _header_ok(buf, primary) and _header_ok(buf, alt) and alt >= primary + SECTOR:
				return primary, alt
	raise NormalizeError(
		"no HFS+/HFSX volume header found (checked bare offset %d and every "
		"GPT/APM partition) — the image layout is unsupported" % VOLUME_HEADER_OFF
	)


def normalize_hfs(buf, epoch):
	"""Clamp volume-header dates and pin the volume UUID on the HFS+ volume (bare or partition-mapped); returns (new_bytes, stats)."""
	if len(buf) < VOLUME_HEADER_OFF + 2 * SECTOR:
		raise NormalizeError("image too small to hold a volume header (%d bytes)" % len(buf))
	primary, alt = _locate_hfs(buf)

	ts = hfs_timestamp(epoch)
	# derive a content-stable volume UUID from the image with every volatile
	# field (both headers' dates and UUIDs) masked out
	masked = bytearray(buf)
	for base in (primary, alt):
		for off in DATE_OFFSETS:
			masked[base + off : base + off + 4] = b"\x00\x00\x00\x00"
		masked[base + OFF_VOLUME_UUID : base + OFF_VOLUME_UUID + VOLUME_UUID_LEN] = b"\x00" * VOLUME_UUID_LEN
	uuid = hashlib.sha256(bytes(masked)).digest()[:VOLUME_UUID_LEN]

	out = bytearray(buf)
	changed = False
	for base in (primary, alt):
		for off in DATE_OFFSETS:
			if out[base + off : base + off + 4] != ts:
				changed = True
			out[base + off : base + off + 4] = ts
		u = base + OFF_VOLUME_UUID
		if out[u : u + VOLUME_UUID_LEN] != uuid:
			changed = True
		out[u : u + VOLUME_UUID_LEN] = uuid
	return bytes(out), {
		"volume_uuid": uuid.hex(),
		"hfs_timestamp": struct.unpack(">I", ts)[0],
		"changed": changed,
	}


# --- shared CLI plumbing ----------------------------------------------------


def source_date_epoch(argv_value):
	"""Resolve the timestamp to clamp to: --source-date-epoch flag, else env, else 0."""
	if argv_value is not None:
		return int(argv_value)
	return int(os.environ.get("SOURCE_DATE_EPOCH", "0"))


def _apply(path, transform):
	"""Read path, run transform(buf) -> (out, stats), write back only if changed, print a summary."""
	with open(path, "rb") as f:
		buf = f.read()
	out, stats = transform(buf)
	if stats["changed"]:
		tmp = path + ".tmp"
		with open(tmp, "wb") as f:
			f.write(out)
		os.replace(tmp, path)
	return stats


# --- self test --------------------------------------------------------------


def _synthetic_koly(seg_id, payload_seed):
	"""Build a minimal file ending in a UDIF 'koly' trailer with the given SegmentID."""
	# some deterministic-from-seed "data fork" so different payloads hash differently
	rng = payload_seed
	body = bytearray()
	while len(body) < 2048:
		rng = (rng * 6364136223846793005 + 1442695040888963407) & (2**64 - 1)
		body += struct.pack("<Q", rng)
	trailer = bytearray(KOLY_SIZE)
	trailer[0:4] = KOLY_MAGIC
	struct.pack_into(">II", trailer, 4, 4, KOLY_SIZE)  # Version, HeaderSize
	trailer[SEGMENT_ID_OFF : SEGMENT_ID_OFF + SEGMENT_ID_LEN] = seg_id
	return bytes(body) + bytes(trailer)


def _synthetic_hfs(dates, uuid, sig, payload_seed):
	"""Build a minimal bare HFS+ image: header at 1024, alternate at size-1024, filler between."""
	size = 8 * SECTOR  # 4096 bytes: room for both headers plus a gap
	img = bytearray(size)
	# deterministic-from-seed catalog filler between the headers
	rng = payload_seed
	for i in range(VOLUME_HEADER_OFF + SECTOR, size - VOLUME_HEADER_OFF, 8):
		rng = (rng * 6364136223846793005 + 1442695040888963407) & (2**64 - 1)
		struct.pack_into("<Q", img, i, rng)

	def write_header(base):
		img[base : base + 2] = sig
		struct.pack_into(">H", img, base + 2, 4)  # version
		for off, val in zip(DATE_OFFSETS, dates):
			struct.pack_into(">I", img, base + off, val)
		img[base + OFF_VOLUME_UUID : base + OFF_VOLUME_UUID + VOLUME_UUID_LEN] = uuid

	write_header(VOLUME_HEADER_OFF)
	write_header(size - VOLUME_HEADER_OFF)
	return bytes(img)


# Apple_HFS GPT type GUID 48465300-0000-11AA-AA11-00306543ECAC (mixed-endian)
_APPLE_HFS_TYPE = bytes(
	[0x00, 0x53, 0x46, 0x48, 0x00, 0x00, 0xAA, 0x11,
	 0xAA, 0x11, 0x00, 0x30, 0x65, 0x43, 0xEC, 0xAC]
)


def _wrap_gpt(volume, part_lba=8, bs=SECTOR):
	"""Embed a bare HFS+ volume inside a minimal GPT-partitioned image (for the self-test)."""
	vol_sectors = len(volume) // bs
	size = (part_lba + vol_sectors + 8) * bs  # tail room for a backup GPT
	img = bytearray(size)
	img[bs : bs + 8] = GPT_SIG
	struct.pack_into("<Q", img, bs + 72, 2)  # partition-entry array starts at LBA 2
	struct.pack_into("<I", img, bs + 80, 4)  # number of entries
	struct.pack_into("<I", img, bs + 84, 128)  # entry size
	e = 2 * bs
	img[e : e + 16] = _APPLE_HFS_TYPE
	struct.pack_into("<Q", img, e + 32, part_lba)
	struct.pack_into("<Q", img, e + 40, part_lba + vol_sectors - 1)
	img[part_lba * bs : part_lba * bs + len(volume)] = volume
	return bytes(img)


def _wrap_apm(volume, part_lba=8, bs=SECTOR):
	"""Embed a bare HFS+ volume inside a minimal APM-partitioned image (for the self-test)."""
	vol_sectors = len(volume) // bs
	size = (part_lba + vol_sectors + 4) * bs
	img = bytearray(size)
	img[0:2] = b"ER"  # driver descriptor record
	e = bs  # first partition-map entry at block 1
	img[e : e + 2] = APM_SIG
	struct.pack_into(">I", img, e + 4, 1)  # pmMapBlkCnt (one entry)
	struct.pack_into(">I", img, e + 8, part_lba)  # pmPyPartStart
	struct.pack_into(">I", img, e + 12, vol_sectors)  # pmPartBlkCnt
	img[part_lba * bs : part_lba * bs + len(volume)] = volume
	return bytes(img)


def self_test():
	"""Prove convergence, idempotence, content sensitivity and malformed-input refusal for both modes."""
	# --- koly mode ---
	a = _synthetic_koly(bytes(range(0x10, 0x20)), payload_seed=7)
	b = _synthetic_koly(bytes(range(0xA0, 0xB0)), payload_seed=7)
	assert a != b
	na, sa = normalize_koly(a)
	nb, sb = normalize_koly(b)
	assert na == nb, "two koly builds differing only in SegmentID must converge"
	assert sa["changed"]
	nc, sc = normalize_koly(na)
	assert nc == na and not sc["changed"], "koly normalization must be idempotent"
	# content sensitivity: a different data fork -> a different SegmentID
	d = _synthetic_koly(bytes(range(0x10, 0x20)), payload_seed=8)
	_, sd = normalize_koly(d)
	assert sd["segment_id"] != sa["segment_id"], "SegmentID must track content"
	for bad in (b"short", b"x" * 600):
		try:
			normalize_koly(bad)
		except NormalizeError:
			pass
		else:
			raise AssertionError("malformed koly input must be refused")

	# --- hfs mode ---
	epoch = 1500000000
	ha = _synthetic_hfs((0x11111111, 0x22222222, 0x33333333, 0x44444444), bytes(range(0, 8)), HFS_PLUS_SIG, payload_seed=3)
	hb = _synthetic_hfs((0xAAAAAAAA, 0xBBBBBBBB, 0xCCCCCCCC, 0xDDDDDDDD), bytes(range(8, 16)), HFS_PLUS_SIG, payload_seed=3)
	assert ha != hb
	nha, sha = normalize_hfs(ha, epoch)
	nhb, shb = normalize_hfs(hb, epoch)
	assert nha == nhb, "two HFS+ builds differing only in dates/UUID must converge"
	assert sha["changed"]
	# both headers carry the same clamped timestamp and derived UUID
	want_ts = hfs_timestamp(epoch)
	for base in (VOLUME_HEADER_OFF, len(nha) - VOLUME_HEADER_OFF):
		for off in DATE_OFFSETS:
			assert nha[base + off : base + off + 4] == want_ts
	# idempotence
	nhc, shc = normalize_hfs(nha, epoch)
	assert nhc == nha and not shc["changed"], "HFS+ normalization must be idempotent"
	# content sensitivity: a different catalog -> a different volume UUID
	hd = _synthetic_hfs((0x11111111, 0x22222222, 0x33333333, 0x44444444), bytes(range(0, 8)), HFS_PLUS_SIG, payload_seed=4)
	_, shd = normalize_hfs(hd, epoch)
	assert shd["volume_uuid"] != sha["volume_uuid"], "volume UUID must track content"
	# HFSX signature is accepted too
	hx = _synthetic_hfs((1, 2, 3, 4), bytes(range(0, 8)), HFSX_SIG, payload_seed=5)
	normalize_hfs(hx, epoch)

	# partition-mapped images (what hdiutil actually produces): the HFS+
	# volume lives at partition_start + 1024, located via the GPT or APM,
	# and two builds differing only in the volatile set still converge.
	for wrap in (_wrap_gpt, _wrap_apm):
		ga = wrap(_synthetic_hfs((0x111, 0x222, 0x333, 0x444), bytes(range(0, 8)), HFS_PLUS_SIG, payload_seed=3))
		gb = wrap(_synthetic_hfs((0x999, 0xAAA, 0xBBB, 0xCCC), bytes(range(8, 16)), HFS_PLUS_SIG, payload_seed=3))
		assert ga != gb, "%s: distinct volatile sets must start different" % wrap.__name__
		nga, sga = normalize_hfs(ga, epoch)
		ngb, _ = normalize_hfs(gb, epoch)
		assert nga == ngb, "%s: two partition-mapped builds must converge" % wrap.__name__
		assert sga["changed"]
		# the located volume header carries the clamped timestamp
		pri, _alt = _locate_hfs(nga)
		assert nga[pri + OFF_CREATE_DATE : pri + OFF_CREATE_DATE + 4] == want_ts
		# and normalization is idempotent through the partition map
		ngc, sgc = normalize_hfs(nga, epoch)
		assert ngc == nga and not sgc["changed"], "%s: must be idempotent" % wrap.__name__
	# refusal: no signature, and too-small images
	nosig = bytearray(ha)
	nosig[VOLUME_HEADER_OFF : VOLUME_HEADER_OFF + 2] = b"ZZ"
	for bad, why in ((bytes(nosig), "missing signature"), (b"\x00" * 1600, "too small / no header")):
		try:
			normalize_hfs(bad, epoch)
		except NormalizeError:
			pass
		else:
			raise AssertionError("malformed hfs input must be refused (%s)" % why)

	print(
		"normalize-dmg self-test: ok (koly converged %d-byte images; hfs converged %d-byte images)"
		% (len(na), len(nha))
	)


# --- entry point ------------------------------------------------------------


def main(argv):
	"""Command-line entry point; returns the process exit code."""
	args = list(argv[1:])
	if args and args[0] == "--self-test":
		self_test()
		return 0
	if not args:
		print("usage: normalize-dmg.py koly FILE | hfs [--source-date-epoch N] FILE | --self-test", file=sys.stderr)
		return 2
	mode = args[0]
	args = args[1:]
	epoch_arg = None
	if mode == "hfs" and len(args) >= 2 and args[0] == "--source-date-epoch":
		epoch_arg = args[1]
		args = args[2:]
	if mode not in ("koly", "hfs") or len(args) != 1:
		print("usage: normalize-dmg.py koly FILE | hfs [--source-date-epoch N] FILE | --self-test", file=sys.stderr)
		return 2
	path = args[0]
	try:
		if mode == "koly":
			stats = _apply(path, normalize_koly)
			what = "SegmentID %s" % stats["segment_id"]
		else:
			try:
				epoch = source_date_epoch(epoch_arg)
			except ValueError:
				print("normalize-dmg: --source-date-epoch/SOURCE_DATE_EPOCH must be an integer", file=sys.stderr)
				return 2
			stats = _apply(path, lambda buf: normalize_hfs(buf, epoch))
			what = "volume UUID %s, dates -> %d" % (stats["volume_uuid"], stats["hfs_timestamp"])
	except NormalizeError as e:
		print("normalize-dmg: %s: %s" % (path, e), file=sys.stderr)
		return 1
	print(
		"normalize-dmg: %s: %s (%s; %s)"
		% (path, mode, "normalized" if stats["changed"] else "already normalized", what)
	)
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv))
