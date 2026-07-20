#!/usr/bin/env bash
# x11-rig-selftest.sh - logic self-test for scripts/x11-rig.sh.
#
# The real rig (x11-rig.sh) needs Xvfb, ImageMagick, xdotool/xwininfo, and
# the JLS jar to boot a real window. Its *control flow* - the failure
# classification that decides between exit 0 (JLS window mapped + non-blank
# screenshot), exit 1 (JLS-side failure), and exit 2 (environment/Xvfb
# blocker) - is pure shell and must not regress silently.
#
# This harness drives the unmodified rig against a stub toolchain
# (fake Xvfb/xdotool/xwininfo/import/identify/compare + a fake java we
# script per case) and asserts the rig classifies each scenario with the
# documented exit code. It needs no real X server, no GUI, and no network,
# so it runs in the sandbox and as a fast CI check on every event.
#
# Usage:  scripts/x11-rig-selftest.sh
# Exit 0 when every scenario classified correctly; 1 otherwise.

set -euo pipefail

RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
RIG="$RIG_DIR/x11-rig.sh"
[ -x "$RIG" ] || { echo "selftest: $RIG is missing or not executable" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

STUB_BIN="$WORK/bin"
mkdir -p "$STUB_BIN"

# --- stub Xvfb: the rig runs `Xvfb :N -displayfd 1 ...` and reads the
# chosen display number from the process's stdout (fd 1). The stub echoes a
# number and idles until killed, unless STUB_XVFB_FAIL=1, which exits
# immediately WITHOUT reporting a display (models an X server that will not
# start -> an environment fault, exit 2).
cat > "$STUB_BIN/Xvfb" <<'EOF'
#!/usr/bin/env bash
if [ "${STUB_XVFB_FAIL:-0}" = 1 ]; then
  echo 'Fatal server error: stub Xvfb refusing to start (selftest)' >&2
  exit 1
fi
echo "${STUB_DISPLAY_NUM:-99}"    # -displayfd 1 target = our stdout
trap 'exit 0' TERM INT
while true; do sleep 1; done
EOF

# --- stub xdotool: the rig searches for a *visible* window by pid
# (authoritative) then by name. The stub returns empty for --pid (so the
# rig exercises its name fallback) and, for --name, a window id keyed on the
# scenario:
#   HelloSwingControl -> id unless STUB_CONTROL_MAPS=0 (control never maps)
#   JLS               -> id only when STUB_JLS_MAPS=1 (else no JLS window)
cat > "$STUB_BIN/xdotool" <<'EOF'
#!/usr/bin/env bash
mode="" ; name=""
while [ $# -gt 0 ]; do
  case "$1" in
    --pid)  mode=pid; shift 2 ;;
    --name) mode=name; name="${2:-}"; shift 2 ;;
    search|--onlyvisible) shift ;;
    *) shift ;;
  esac
done
[ "$mode" = pid ] && exit 0   # force the rig onto its name fallback
case "$name" in
  *HelloSwingControl*) [ "${STUB_CONTROL_MAPS:-1}" = 1 ] && echo 4200001 ;;
  *JLS*)               [ "${STUB_JLS_MAPS:-1}" = 1 ]     && echo 4200002 ;;
esac
exit 0
EOF

# --- stub xwininfo: the rig confirms the chosen id is mapped by grepping
# for "Map State: IsViewable". The stub reports IsViewable unless
# STUB_UNMAPPED=1 (models a chosen id that is not actually viewable).
cat > "$STUB_BIN/xwininfo" <<'EOF'
#!/usr/bin/env bash
id=""
while [ $# -gt 0 ]; do case "$1" in -id) id="${2:-}"; shift 2 ;; *) shift ;; esac; done
echo "xwininfo: Window id: $id \"stub window\""
if [ "${STUB_UNMAPPED:-0}" = 1 ]; then
  echo "  Map State: IsUnMapped"
else
  echo "  Map State: IsViewable"
fi
echo "  Width: 970"
echo "  Height: 600"
EOF

# --- stub import: write a byte-valid 1x1 PNG so the evidence/screenshot
# steps succeed. The real non-blank verdict is decided by the identify stub.
cat > "$STUB_BIN/import" <<'EOF'
#!/usr/bin/env bash
out="${!#}"   # import's last argument is the output path
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$out"
EOF

# --- stub identify: reports the unique-color count the rig's non-blank gate
# reads. 186 (a real window) by default; 1 (blank) for the JLS window shot
# when STUB_JLS_BLANK=1 (models a mapped-but-blank JLS window -> exit 1).
cat > "$STUB_BIN/identify" <<'EOF'
#!/usr/bin/env bash
file="${!#}"
case "$file" in
  *jls-window*) [ "${STUB_JLS_BLANK:-0}" = 1 ] && { echo 1; exit 0; } ;;
esac
echo 186
EOF

# --- stub compare: emit an AE pixel-difference count on stderr (as real
# ImageMagick does). STUB_AE controls it so the P2 gate can be exercised.
cat > "$STUB_BIN/compare" <<'EOF'
#!/usr/bin/env bash
echo "${STUB_AE:-581770}" >&2
exit 1   # nonzero because the images differ - what the rig expects
EOF

# --- stub java: -version prints one line; the control run idles until
# killed; the JLS run idles unless STUB_JLS_CRASH=1, which makes it exit
# nonzero *before* mapping a window (the exit-1 JLS-side-failure case).
cat > "$STUB_BIN/java" <<'EOF'
#!/usr/bin/env bash
for a in "$@"; do
  [ "$a" = "-version" ] && { echo 'stub-jdk version "25" (x11-rig selftest)' >&2; exit 0; }
done
is_jar=0
for a in "$@"; do [ "$a" = "-jar" ] && is_jar=1; done
if [ "$is_jar" = 1 ] && [ "${STUB_JLS_CRASH:-0}" = 1 ]; then
  echo 'java.awt.AWTError: stub JLS-side startup failure (selftest)' >&2
  exit 1
fi
# control program, or a non-crashing JLS: stay up until the rig kills us
trap 'exit 0' TERM INT
while true; do sleep 1; done
EOF

chmod +x "$STUB_BIN"/*

# a placeholder jar the rig can find
mkdir -p "$WORK/target"; : > "$WORK/target/jls-selftest.jar"

fail=0
# run_case NAME EXPECTED_EXIT   (scenario knobs passed via the environment)
run_case() {
	name=$1; want=$2; shift 2
	rundir="$WORK/run-$name"; mkdir -p "$rundir/target"
	cp "$WORK/target/jls-selftest.jar" "$rundir/target/"
	got=0
	( cd "$rundir" \
		&& PATH="$STUB_BIN:$PATH" \
		   ARTIFACTS_DIR="$rundir/artifacts" \
		   XVFB_TIMEOUT=5 CONTROL_TIMEOUT=3 WINDOW_TIMEOUT=5 \
		   "$@" "$RIG" ) > "$rundir/rig.out" 2>&1 || got=$?
	if [ "$got" = "$want" ]; then
		echo "selftest: PASS  $name (exit $got)"
	else
		echo "selftest: FAIL  $name (expected exit $want, got $got)"
		echo "----- rig output ($name) -----"; sed 's/^/    /' "$rundir/rig.out"
		fail=1
	fi
}

# 1) success: control maps, JLS maps, non-blank screenshot -> exit 0
run_case success 0 env
# 2) JLS-side failure: control maps, JLS crashes before a window -> exit 1
run_case jls-crash 1 env STUB_JLS_MAPS=0 STUB_JLS_CRASH=1
# 3) JLS-side failure: control maps, JLS never maps a window -> exit 1
run_case jls-nowindow 1 env STUB_JLS_MAPS=0
# 4) JLS-side failure: JLS window maps but the screenshot is blank -> exit 1
run_case jls-blank 1 env STUB_JLS_BLANK=1
# 5) environment blocker: even the control window never maps -> exit 2
run_case env-no-control 2 env STUB_CONTROL_MAPS=0
# 6) environment blocker: Xvfb refuses to start -> exit 2
run_case env-no-xvfb 2 env STUB_XVFB_FAIL=1
# 7) P2 gate armed and satisfied: many pixels differ -> exit 0
run_case p2-pass 0 env PIXEL_DIFF_MIN=1000 STUB_AE=581770
# 8) P2 gate armed and violated: too few pixels differ -> exit 1 (JLS-side)
run_case p2-fail 1 env PIXEL_DIFF_MIN=1000 STUB_AE=5

# spot-check the evidence the success run is supposed to leave behind
succ="$WORK/run-success/artifacts"
for f in control-verdict.txt desktop-before.png desktop-after.png jls-window.png nonblank.txt jls-window-info.txt; do
	if [ ! -s "$succ/$f" ]; then
		echo "selftest: FAIL  success run did not produce $f"; fail=1
	fi
done

[ "$fail" = 0 ] && echo "selftest: all scenarios classified correctly" \
	|| echo "selftest: one or more scenarios misclassified"
exit "$fail"
