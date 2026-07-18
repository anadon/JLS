#!/usr/bin/env bash
# Measure macOS dmg reproducibility by double-building (issue #191).
#
# Builds the installer twice on the same working tree and attributes
# every difference between the two dmgs:
#
#   sha256.txt             headline verdict (P1)
#   koly-{a,b}.hex,        UDIF trailer diff; segment-id-{a,b}.txt
#   koly.diff              isolate the random per-convert SegmentID
#   imageinfo.diff         hdiutil imageinfo envelope comparison
#   raw-diff-ranges.txt    both dmgs converted to raw (UDTO), cmp'd,
#                          differing bytes aggregated into contiguous
#                          ranges, labeled when they fall inside the
#                          primary/alternate HFS+ volume header
#   payload-content.diff   per-file sha256 of the mounted volumes
#   payload-metadata.diff  per-node mode/size/mtime/birthtime
#   volume-info.diff       diskutil info (captures the volume UUID)
#   SUMMARY.txt            counts and the verdict
#
# Everything lands in target/dmg-repro/.  The second build reuses the
# first build's jar (JLS_SKIP_BUILD=1) so jar-lane variance — already
# double-build-gated in ci.yml (#44) — cannot pollute the attribution.
#
# This is a measurement, not a gate: it exits 0 whether or not the
# images match, and nonzero only when tooling fails.  See
# docs/dmg-reproducibility.md for the variance inventory this feeds.
#
# Usage (macOS only): bash scripts/measure-dmg-repro.sh

set -euo pipefail

if [ "$(uname -s)" != "Darwin" ]; then
	echo "measure-dmg-repro.sh: requires macOS (hdiutil)" >&2
	exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

OUT="target/dmg-repro"
rm -rf "$OUT"
mkdir -p "$OUT"
SUMMARY="$OUT/SUMMARY.txt"
: > "$SUMMARY"

note() {
	echo "$*" | tee -a "$SUMMARY"
}

# best-effort unmount of anything this script attached
MNT="$OUT/mnt"
cleanup() {
	if [ -d "$MNT" ]; then
		hdiutil detach "$MNT" -quiet -force 2>/dev/null || true
	fi
}
trap cleanup EXIT

grab_dmg() {
	# copy the single dmg the installer build produced
	local dest="$1"
	local dmg
	dmg="$(ls target/installer/dist/JLS-*.dmg)"
	cp "$dmg" "$dest"
}

# --- two builds of one tree -------------------------------------------------
echo "==> build 1/2"
bash scripts/build-installer.sh
grab_dmg "$OUT/a.dmg"

echo "==> build 2/2 (same jar: JLS_SKIP_BUILD=1)"
JLS_SKIP_BUILD=1 bash scripts/build-installer.sh
grab_dmg "$OUT/b.dmg"

note "dmg double-build measurement: $(date -u +%Y-%m-%dT%H:%M:%SZ), macOS $(sw_vers -productVersion), $(uname -m)"
note "commit: $(git rev-parse HEAD 2>/dev/null || echo unknown)"
note ""

# --- headline: P1 -----------------------------------------------------------
shasum -a 256 "$OUT/a.dmg" "$OUT/b.dmg" | tee "$OUT/sha256.txt" >> "$SUMMARY"
A_SUM="$(awk 'NR==1{print $1}' "$OUT/sha256.txt")"
B_SUM="$(awk 'NR==2{print $1}' "$OUT/sha256.txt")"
if [ "$A_SUM" = "$B_SUM" ]; then
	note ""
	note "RESULT: byte-identical (P3 reached) — a CI double-build gate is now justified"
	exit 0
fi
note ""
note "RESULT: dmgs differ (P1 confirmed); attribution follows"

# --- E1: koly trailer / SegmentID -------------------------------------------
for side in a b; do
	tail -c 512 "$OUT/$side.dmg" | xxd > "$OUT/koly-$side.hex"
	# SegmentID: 16 bytes at trailer offsets 64..79
	tail -c 512 "$OUT/$side.dmg" | dd bs=1 skip=64 count=16 2>/dev/null \
		| xxd -p > "$OUT/segment-id-$side.txt"
done
diff "$OUT/koly-a.hex" "$OUT/koly-b.hex" > "$OUT/koly.diff" || true
note "koly trailer: $(grep -c '^<' "$OUT/koly.diff" || true) differing hex-dump lines (see koly.diff)"
note "SegmentID a: $(cat "$OUT/segment-id-a.txt")"
note "SegmentID b: $(cat "$OUT/segment-id-b.txt")"

# --- envelope metadata ------------------------------------------------------
hdiutil imageinfo "$OUT/a.dmg" > "$OUT/imageinfo-a.txt"
hdiutil imageinfo "$OUT/b.dmg" > "$OUT/imageinfo-b.txt"
diff "$OUT/imageinfo-a.txt" "$OUT/imageinfo-b.txt" > "$OUT/imageinfo.diff" || true
note "imageinfo: $(grep -c '^<' "$OUT/imageinfo.diff" || true) differing lines (see imageinfo.diff)"

# --- F1/F6: raw filesystem byte attribution ---------------------------------
# UDZO recompression cascades any early difference over the whole file;
# comparing the decompressed raw images localizes the true differences.
for side in a b; do
	hdiutil convert "$OUT/$side.dmg" -quiet -format UDTO -o "$OUT/$side" 2>> "$OUT/raw-cmp-notes.txt"
done
A_RAW="$OUT/a.cdr"
B_RAW="$OUT/b.cdr"
A_SIZE="$(stat -f %z "$A_RAW")"
B_SIZE="$(stat -f %z "$B_RAW")"
note "raw image sizes: a=$A_SIZE b=$B_SIZE"
if [ "$A_SIZE" != "$B_SIZE" ]; then
	note "raw sizes differ: allocation layout is not stable (F6); cmp covers the common prefix only"
fi
# cmp exits 1 on any difference; keep the pipeline alive for awk
(cmp -l "$A_RAW" "$B_RAW" 2>> "$OUT/raw-cmp-notes.txt" || true) | awk -v size="$A_SIZE" '
	function label(s) {
		if (s >= 1024 && s < 1536)
			return "  <- primary HFS+ volume header"
		if (s >= size - 1024 && s < size - 512)
			return "  <- alternate HFS+ volume header"
		return ""
	}
	{
		o = $1 - 1 # cmp -l offsets are 1-based
		total++
		if (started && o == prev + 1) { prev = o; next }
		if (started)
			print first "-" prev " (" prev - first + 1 " bytes)" label(first)
		first = o; prev = o; started = 1
	}
	END {
		if (started)
			print first "-" prev " (" prev - first + 1 " bytes)" label(first)
		print "total differing bytes: " total + 0
	}' > "$OUT/raw-diff-ranges.txt"
note "raw differing regions: $(awk 'END{print NR-1}' "$OUT/raw-diff-ranges.txt") (see raw-diff-ranges.txt)"
note "$(tail -n 1 "$OUT/raw-diff-ranges.txt")"

# --- F2-F5, F7: payload content and metadata --------------------------------
# Mount sequentially (both volumes are named JLS) and manifest each.
# The dmg carries a udifrez license, so attach prompts for agreement;
# feed it a Y.
for side in a b; do
	mkdir -p "$MNT"
	printf 'Y\n' | hdiutil attach "$OUT/$side.dmg" -readonly -nobrowse \
		-noautoopen -mountpoint "$MNT" -quiet
	(cd "$MNT" && find . -type f -exec shasum -a 256 {} + 2>/dev/null | sort -k 2) \
		> "$OUT/payload-content-$side.txt" || true
	(cd "$MNT" && find . -exec stat -f '%N|%Sp|%z|mod=%Sm|birth=%SB' \
		-t '%Y-%m-%dT%H:%M:%S' {} \; 2>/dev/null | sort) \
		> "$OUT/payload-metadata-$side.txt" || true
	diskutil info "$MNT" > "$OUT/volume-info-$side.txt" || true
	hdiutil detach "$MNT" -quiet || hdiutil detach "$MNT" -quiet -force
	rmdir "$MNT" 2>/dev/null || true
done
diff "$OUT/payload-content-a.txt" "$OUT/payload-content-b.txt" \
	> "$OUT/payload-content.diff" || true
diff "$OUT/payload-metadata-a.txt" "$OUT/payload-metadata-b.txt" \
	> "$OUT/payload-metadata.diff" || true
diff "$OUT/volume-info-a.txt" "$OUT/volume-info-b.txt" \
	> "$OUT/volume-info.diff" || true
note "payload files with differing content: $(grep -c '^<' "$OUT/payload-content.diff" || true)"
note "payload nodes with differing metadata: $(grep -c '^<' "$OUT/payload-metadata.diff" || true)"
note "volume-info differing lines: $(grep -c '^<' "$OUT/volume-info.diff" || true) (volume UUID lives here)"
note ""
note "full evidence in $OUT/ — attach SUMMARY.txt and the diffs to issue #191"
