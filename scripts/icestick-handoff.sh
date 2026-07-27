#!/usr/bin/env bash
# icestick-handoff.sh - external-tool handoff from a JLS iCEstick export
# to a flashed bitstream (issue #264, Stage 1).
#
# JLS emits the RTL and the pin constraints; it does NOT synthesize,
# place, route, pack, or flash anything (issue #215 H2: delegate, do
# not reimplement).  This script is the thin wrapper that carries the
# Verilog + PCF that
#
#     jls -export design.v -board icestick -pins pins.txt design.jls
#
# already produces (see JLSStart's -export/-board/-pins CLI) the rest of
# the way, using the open iCE40 toolchain:
#
#     design.v + design.pcf
#       -> yosys           (synth_ice40)              -> design.json
#       -> nextpnr-ice40   (--hx1k --package tq144)   -> design.asc
#       -> icepack                                    -> design.bin
#       -> openFPGALoader / iceprog   (only with --flash)
#
# The iCE40-HX1K / TQ144 target is hard-coded to match Boards.ICESTICK
# (jls.hdl.board): the same device the PCF was emitted for.
#
# Preflight is all-or-nothing (mirroring PcfEmitter's error
# aggregation): every missing tool is reported in one pass with its
# role and where to get it, then a single nonzero exit - so you fix the
# whole gap once instead of one apt-get per failed run.
#
# Usage:
#   scripts/icestick-handoff.sh [options] design.v design.pcf
#
# Options:
#   -t, --top NAME    top module name (default: the .v basename)
#   -o, --out DIR     output directory for .json/.asc/.bin
#                     (default: alongside design.v)
#   -f, --flash       program the board after packing (openFPGALoader,
#                     falling back to iceprog)
#   -h, --help        this help
#
# Exit 0 on success; nonzero on a missing tool, a bad argument, or a
# failed toolchain stage.

set -euo pipefail

PROG="$(basename "$0")"

usage() {
	sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'
}

# --- argument parsing --------------------------------------------------
TOP=""
OUTDIR=""
FLASH=0
POSITIONAL=()

while [ $# -gt 0 ]; do
	case "$1" in
		-t|--top)   TOP="${2:-}"; shift 2 ;;
		-o|--out)   OUTDIR="${2:-}"; shift 2 ;;
		-f|--flash) FLASH=1; shift ;;
		-h|--help)  usage; exit 0 ;;
		--)         shift; while [ $# -gt 0 ]; do POSITIONAL+=("$1"); shift; done ;;
		-*)         echo "$PROG: unknown option: $1" >&2; usage >&2; exit 2 ;;
		*)          POSITIONAL+=("$1"); shift ;;
	esac
done

if [ "${#POSITIONAL[@]}" -ne 2 ]; then
	echo "$PROG: expected exactly two positional arguments (design.v design.pcf)," \
		"got ${#POSITIONAL[@]}" >&2
	usage >&2
	exit 2
fi

VERILOG="${POSITIONAL[0]}"
PCF="${POSITIONAL[1]}"

for f in "$VERILOG" "$PCF"; do
	[ -f "$f" ] || { echo "$PROG: input not found: $f" >&2; exit 2; }
done

# top module defaults to the Verilog basename (design.v -> design)
if [ -z "$TOP" ]; then
	TOP="$(basename "$VERILOG")"
	TOP="${TOP%.*}"
fi

# outputs land alongside the Verilog unless -o was given
if [ -z "$OUTDIR" ]; then
	OUTDIR="$(cd "$(dirname "$VERILOG")" && pwd)"
fi
mkdir -p "$OUTDIR"

BASE="$(basename "$VERILOG")"; BASE="${BASE%.*}"
JSON="$OUTDIR/$BASE.json"
ASC="$OUTDIR/$BASE.asc"
BIN="$OUTDIR/$BASE.bin"

# --- preflight: collect EVERY missing tool, then fail once -------------
# Each entry names the tool, its role in the flow, and where to get it.
missing=()

need() {
	# need TOOL "role" "where to get it"
	command -v "$1" >/dev/null 2>&1 && return 0
	missing+=("$1 - $2 (get it from $3)")
}

need yosys        "synthesis (RTL -> iCE40 netlist)" \
	"YosysHQ/yosys - https://github.com/YosysHQ/yosys"
need nextpnr-ice40 "place & route for the iCE40-HX1K" \
	"YosysHQ/nextpnr - https://github.com/YosysHQ/nextpnr"
need icepack      "netlist -> bitstream packing" \
	"project-icestorm (icestorm) - https://github.com/YosysHQ/icestorm"

# The programmer is only needed with --flash; without it, a missing
# programmer must NOT block a build-only run.
PROGRAMMER=""
if [ "$FLASH" -eq 1 ]; then
	if command -v openFPGALoader >/dev/null 2>&1; then
		PROGRAMMER="openFPGALoader"
	elif command -v iceprog >/dev/null 2>&1; then
		PROGRAMMER="iceprog"
	else
		missing+=("openFPGALoader or iceprog - flashing the iCEstick over USB" \
			"(openFPGALoader: trabucayre/openFPGALoader; iceprog ships with project-icestorm)")
	fi
fi

if [ "${#missing[@]}" -gt 0 ]; then
	echo "$PROG: the iCE40 toolchain is incomplete; install the following and re-run:" >&2
	for m in "${missing[@]}"; do
		echo "  - $m" >&2
	done
	exit 1
fi

# --- the flow ----------------------------------------------------------
run() {
	echo "==> $*"
	"$@"
}

run yosys -p "synth_ice40 -top $TOP -json $JSON" "$VERILOG"
run nextpnr-ice40 --hx1k --package tq144 --pcf "$PCF" --json "$JSON" --asc "$ASC"
run icepack "$ASC" "$BIN"

echo "==> bitstream: $BIN"

if [ "$FLASH" -eq 1 ]; then
	case "$PROGRAMMER" in
		openFPGALoader) run openFPGALoader -b ice40_generic "$BIN" ;;
		iceprog)        run iceprog "$BIN" ;;
	esac
	echo "==> flashed the iCEstick with $BIN via $PROGRAMMER"
fi
