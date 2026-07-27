#!/usr/bin/env bash
# icestick-handoff-selftest.sh - logic self-test for
# scripts/icestick-handoff.sh (issue #264).
#
# The real handoff (icestick-handoff.sh) can only run where the open
# iCE40 toolchain (yosys, nextpnr-ice40, icepack, and a programmer) is
# installed - and only reach hardware where an iCEstick is plugged in.
# Its *control flow*, though - the all-or-nothing preflight that names
# every missing tool in one pass, the --flash-only programmer check, and
# the yosys -> nextpnr-ice40 -> icepack -> programmer chain - is pure
# shell and must not regress silently.
#
# This harness drives the unmodified handoff script against a hermetic
# stub PATH (fake yosys/nextpnr-ice40/icepack/openFPGALoader plus only
# the coreutils the script itself needs) and asserts:
#   (a) each single missing tool prints that tool's preflight message
#       and exits nonzero;
#   (b) all-missing lists every gap in one nonzero exit;
#   (c) the fully-stubbed happy path reaches icepack, and with --flash
#       reaches the programmer;
#   (d) a missing programmer only matters under --flash.
# It needs no real toolchain, no hardware, and no network, so it runs in
# the sandbox and as a fast CI check.
#
# Usage:  scripts/icestick-handoff-selftest.sh
# Exit 0 when every scenario behaves as documented; 1 otherwise.

set -euo pipefail

SELF_DIR="$(cd "$(dirname "$0")" && pwd)"
HANDOFF="$SELF_DIR/icestick-handoff.sh"
[ -x "$HANDOFF" ] || { echo "selftest: $HANDOFF is missing or not executable" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# --- a hermetic base PATH: only the coreutils the handoff script needs.
# Restricting PATH to this dir means a tool is "missing" iff we did not
# stub it - independent of whatever the CI runner happens to have
# installed (the build lane installs a real yosys, for instance).
CORE_BIN="$WORK/core"
mkdir -p "$CORE_BIN"
for tool in bash sh mkdir basename dirname sed cat rm; do
	p="$(command -v "$tool" 2>/dev/null)" || {
		echo "selftest: cannot locate '$tool' to build the hermetic PATH" >&2
		exit 1
	}
	ln -sf "$p" "$CORE_BIN/$tool"
done

# fake export inputs (the script only checks they exist)
: > "$WORK/design.v"
: > "$WORK/design.pcf"

# make_stub DIR NAME  - a fake FPGA tool that records that it ran.
make_stub() {
	dir="$1"; name="$2"
	cat > "$dir/$name" <<EOF
#!/usr/bin/env bash
echo "$name" >> "\$STUB_LOG"
exit 0
EOF
	chmod +x "$dir/$name"
}

fail=0

# run_case NAME EXPECT_EXIT "flash?" "stubs..." - builds a stub bin with
# the named FPGA tools present, runs the handoff, and records exit code,
# combined output, and the tool-run log for the assertions that follow.
declare OUT LOG GOT
run_case() {
	name="$1"; want="$2"; flash="$3"; shift 3
	casedir="$WORK/case-$name"
	bin="$casedir/bin"
	mkdir -p "$bin"
	# every case gets the coreutils; FPGA tools only if listed
	for f in "$CORE_BIN"/*; do ln -sf "$f" "$bin/"; done
	for tool in "$@"; do make_stub "$bin" "$tool"; done

	LOG="$casedir/ran.log"; : > "$LOG"
	OUT="$casedir/out.txt"
	args=("$WORK/design.v" "$WORK/design.pcf")
	[ "$flash" = flash ] && args=(--flash "${args[@]}")

	GOT=0
	PATH="$bin" STUB_LOG="$LOG" "$HANDOFF" "${args[@]}" > "$OUT" 2>&1 || GOT=$?

	if [ "$GOT" != "$want" ]; then
		echo "selftest: FAIL  $name (expected exit $want, got $GOT)"
		echo "----- handoff output ($name) -----"; sed 's/^/    /' "$OUT"
		fail=1
		return 1
	fi
	return 0
}

# expect_out NAME "needle" - the last case's output must contain needle.
expect_out() {
	if ! grep -qF -- "$2" "$OUT"; then
		echo "selftest: FAIL  $1 (output missing: $2)"
		echo "----- handoff output -----"; sed 's/^/    /' "$OUT"
		fail=1
	fi
}

# expect_ran NAME "tool" / expect_not_ran - assert the run log.
expect_ran() {
	if ! grep -qxF -- "$2" "$LOG"; then
		echo "selftest: FAIL  $1 (expected $2 to run; it did not)"
		fail=1
	fi
}
expect_not_ran() {
	if grep -qxF -- "$2" "$LOG"; then
		echo "selftest: FAIL  $1 ($2 ran but should not have)"
		fail=1
	fi
}

ALL_SYNTH=(yosys nextpnr-ice40 icepack)

# (a) each single missing tool -> its own preflight message, nonzero exit
if run_case miss-yosys 1 noflash nextpnr-ice40 icepack; then
	expect_out miss-yosys "yosys - synthesis"
fi
if run_case miss-nextpnr 1 noflash yosys icepack; then
	expect_out miss-nextpnr "nextpnr-ice40 - place & route"
fi
if run_case miss-icepack 1 noflash yosys nextpnr-ice40; then
	expect_out miss-icepack "icepack - netlist"
fi

# (b) all synthesis tools missing -> every gap named in one exit
if run_case miss-all 1 noflash; then
	expect_out miss-all "yosys - synthesis"
	expect_out miss-all "nextpnr-ice40 - place & route"
	expect_out miss-all "icepack - netlist"
fi

# (d) a missing programmer is fine WITHOUT --flash ...
if run_case build-only 0 noflash "${ALL_SYNTH[@]}"; then
	expect_ran build-only icepack
	expect_out build-only "bitstream:"
fi
# ... but is a hard error WITH --flash (all synth tools present)
if run_case flash-no-prog 1 flash "${ALL_SYNTH[@]}"; then
	expect_out flash-no-prog "openFPGALoader or iceprog"
fi

# (c) happy path with --flash: reaches icepack AND the programmer
if run_case flash-ok 0 flash "${ALL_SYNTH[@]}" openFPGALoader; then
	expect_ran flash-ok icepack
	expect_ran flash-ok openFPGALoader
fi
# and the iceprog fallback is honoured when openFPGALoader is absent
if run_case flash-iceprog 0 flash "${ALL_SYNTH[@]}" iceprog; then
	expect_ran flash-iceprog iceprog
	expect_not_ran flash-iceprog openFPGALoader
fi

[ "$fail" = 0 ] && echo "selftest: all scenarios behaved as documented" \
	|| echo "selftest: one or more scenarios misbehaved"
exit "$fail"
