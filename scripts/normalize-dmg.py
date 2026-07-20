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
#   hfs   --  the HFS+ primary volume header (bytes 1024-1535 of the raw
#             volume) and its alternate copy (the second-to-last sector)
#             hold createDate/modifyDate/backupDate/checkedDate
#             (wall-clock) and a 64-bit volume UUID in finderInfo[6..7]
#             (randomized at volume creation) — F1 in the inventory.
#             These are patched on the *uncompressed* read-write image
#             BEFORE it is re-compressed with `hdiutil convert`, so the
#             UDIF checksums are (re)computed over the already-normalized
#             bytes.  Patching them on a finished UDZO image would corrupt
#             it; this mode refuses to run on anything that is not a bare
#             HFS+/HFSX volume with the signature at offset 1024.
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
	return base + SECTOR <= len(buf) and buf[base : base + 2] in (HFS_PLUS_SIG, HFSX_SIG)


def normalize_hfs(buf, epoch):
	"""Clamp volume-header dates and pin the volume UUID on a bare HFS+ image; returns (new_bytes, stats)."""
	size = len(buf)
	primary = VOLUME_HEADER_OFF
	alt = size - VOLUME_HEADER_OFF
	if size < VOLUME_HEADER_OFF + 2 * SECTOR:
		raise NormalizeError("image too small to hold both volume headers (%d bytes)" % size)
	if not _header_ok(buf, primary):
		raise NormalizeError(
			"no HFS+/HFSX signature at offset %d — not a bare HFS+ image "
			"(a partition-mapped image is unsupported; run hfs mode on the "
			"raw single-volume UDRW hdiutil produces)" % primary
		)
	if not _header_ok(buf, alt):
		raise NormalizeError("no HFS+/HFSX signature in the alternate volume header at offset %d" % alt)
	if alt < primary + SECTOR:
		raise NormalizeError("primary and alternate volume headers overlap")

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
