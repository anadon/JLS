#!/usr/bin/env bash
# macos-rig-selftest.sh - logic self-test for scripts/macos-rig.sh
# (issue #265 Stage 9).
#
# The real rig (macos-rig.sh) can only run on a macOS runner with a live
# WindowServer, screencapture/sips/osascript, and the JLS jar. Its *control
# flow* - the failure classification that decides between exit 0 (JLS window
# mapped + non-blank screenshot, OR the DEGRADED-mode window-map when Screen
# Recording/TCC is withheld), exit 1 (JLS-side failure), and exit 2 (environment:
# readiness capture fails outright, the control window never maps, or System
# Events permission withheld) - is pure shell and must not regress silently
# before first light on CI. A blank CONTROL-window capture (with readiness OK and
# the control window mapped) is the TCC signature and now enters DEGRADED MODE
# (exit 0 on a JLS window-map), NOT exit 2 - #265 Stage 9.
#
# This harness drives the unmodified rig against a stub toolchain (fake
# screencapture/sips/osascript/python3 + a fake java we script per case) and
# asserts the rig classifies each scenario with the documented exit code. It
# needs no macOS, no GUI, and no network, so it runs in the sandbox and as a
# fast CI check on every event (exactly like x11-rig-selftest.sh).
#
# Usage:  scripts/macos-rig-selftest.sh
# Exit 0 when every scenario classified correctly; 1 otherwise.

set -euo pipefail

RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
RIG="$RIG_DIR/macos-rig.sh"
[ -x "$RIG" ] || { echo "selftest: $RIG is missing or not executable" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

STUB_BIN="$WORK/bin"
mkdir -p "$STUB_BIN"

# --- stub osascript: the rig calls `osascript FILE TITLE PID` and reads a
# window count on stdout. STUB_OSA_DENIED=1 models Accessibility/automation
# permission withheld (osascript exits non-zero -> the rig's exit-2 path). The
# count keys on the title: HelloSwingControl maps unless STUB_CONTROL_MAPS=0,
# JLS maps only when STUB_JLS_MAPS=1.
cat > "$STUB_BIN/osascript" <<'EOF'
#!/usr/bin/env bash
if [ "${STUB_OSA_DENIED:-0}" = 1 ]; then
  echo 'execution error: Not authorized to send Apple events to System Events. (-1743)' >&2
  exit 1
fi
title="${2:-}"
case "$title" in
  *HelloSwingControl*) [ "${STUB_CONTROL_MAPS:-1}" = 1 ] && echo 1 || echo 0 ;;
  *JLS*)               [ "${STUB_JLS_MAPS:-1}" = 1 ]     && echo 1 || echo 0 ;;
  *)                   echo 0 ;;
esac
exit 0
EOF

# --- stub screencapture: `screencapture -x -t png OUT`. Write a byte to the
# output path (last arg) so the evidence steps succeed; blankness is decided by
# the identify-equivalent (the python stub), matching the x11 selftest split.
# STUB_CAPTURE_FAIL=1 makes every capture exit non-zero, modelling a runner where
# even the desktop readiness screenshot fails outright (the rig's exit-2 path).
cat > "$STUB_BIN/screencapture" <<'EOF'
#!/usr/bin/env bash
if [ "${STUB_CAPTURE_FAIL:-0}" = 1 ]; then
  echo 'stub screencapture: capture failed (selftest)' >&2
  exit 1
fi
out="${!#}"
printf 'stub-capture' > "$out"
EOF

# --- stub sips: `sips -s format bmp SRC --out OUT`. Create OUT (a fake bmp)
# preserving the source basename token (control/jls/desktop) so the python
# color stub can key on it, exactly as the real transcode would.
cat > "$STUB_BIN/sips" <<'EOF'
#!/usr/bin/env bash
out=""
while [ $# -gt 0 ]; do
  case "$1" in --out) out="${2:-}"; shift 2 ;; *) shift ;; esac
done
[ -n "$out" ] && printf 'stub-bmp' > "$out"
exit 0
EOF

# --- stub python3: two modes, distinguished by how many .bmp files are passed.
# colors mode (one .bmp): reports the unique-color count the rig's non-blank
# gate reads - 186 (a real frame) by default; 1 (blank) for the control shot
# when STUB_CONTROL_BLANK=1 or the JLS shot when STUB_JLS_BLANK=1. diff mode
# (two .bmp): reports the sampled changed-pixel count from STUB_DIFF.
cat > "$STUB_BIN/python3" <<'EOF'
#!/usr/bin/env bash
bmps=()
for a in "$@"; do case "$a" in *.bmp) bmps+=("$a") ;; esac; done
if [ "${#bmps[@]}" -ge 2 ]; then
  echo "${STUB_DIFF:-59998}"; exit 0
fi
file="${bmps[0]:-}"
case "$file" in
  *control*) [ "${STUB_CONTROL_BLANK:-0}" = 1 ] && { echo 1; exit 0; } ;;
  *jls*|*desktop-after*) [ "${STUB_JLS_BLANK:-0}" = 1 ] && { echo 1; exit 0; } ;;
esac
echo 186
EOF

# --- stub java: -version prints one line; the control run idles until killed;
# the JLS run idles unless STUB_JLS_CRASH=1, which makes it exit non-zero
# *before* mapping a window (the exit-1 JLS-side-failure case).
cat > "$STUB_BIN/java" <<'EOF'
#!/usr/bin/env bash
for a in "$@"; do
  [ "$a" = "-version" ] && { echo 'stub-jdk version "25" (macos-rig selftest)' >&2; exit 0; }
done
is_jar=0
for a in "$@"; do [ "$a" = "-jar" ] && is_jar=1; done
if [ "$is_jar" = 1 ] && [ "${STUB_JLS_CRASH:-0}" = 1 ]; then
  echo 'java.awt.AWTError: stub JLS-side startup failure (selftest)' >&2
  exit 1
fi
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
		   CONTROL_TIMEOUT=3 WINDOW_TIMEOUT=5 RENDER_TIMEOUT=2 \
		   "$@" "$RIG" ) > "$rundir/rig.out" 2>&1 || got=$?
	if [ "$got" = "$want" ]; then
		echo "selftest: PASS  $name (exit $got)"
	else
		echo "selftest: FAIL  $name (expected exit $want, got $got)"
		echo "----- rig output ($name) -----"; sed 's/^/    /' "$rundir/rig.out"
		fail=1
	fi
}

# 1) success: control maps + non-blank, JLS maps + non-blank -> exit 0
run_case success 0 env
# 2) JLS-side failure: control ok, JLS crashes before a window -> exit 1
run_case jls-crash 1 env STUB_JLS_MAPS=0 STUB_JLS_CRASH=1
# 3) JLS-side failure: control ok, JLS never maps a window -> exit 1
run_case jls-nowindow 1 env STUB_JLS_MAPS=0
# 4) JLS-side failure: JLS window maps but the screenshot is blank -> exit 1
run_case jls-blank 1 env STUB_JLS_BLANK=1
# 5) environment blocker: even the control window never maps -> exit 2
run_case env-no-control 2 env STUB_CONTROL_MAPS=0
# 6) environment blocker: System Events permission withheld -> exit 2
run_case env-osa-denied 2 env STUB_OSA_DENIED=1
# 7) environment blocker: the desktop readiness screenshot fails outright -> exit 2
#    (distinct from the control-blank TCC signature: this breaks the capture and
#    the window-map proof, so it stays a hard environment fault)
run_case env-readiness-capture-fail 2 env STUB_CAPTURE_FAIL=1
# 8) DEGRADED MODE (the #265 Stage 9 fix): desktop readiness capture worked and
#    the control window MAPPED, but its capture is blank (Screen Recording / TCC
#    withheld). This no longer exits 2 - the rig warns, launches the real jar,
#    and succeeds on the window-map proof alone (pixel proofs skipped) -> exit 0.
run_case degraded-control-blank 0 env STUB_CONTROL_BLANK=1
# 9) DEGRADED MODE but the JLS window never maps: the control mapped, so a JLS
#    no-show is still JLS-side -> exit 1 (not exit 2)
run_case degraded-jls-nowindow 1 env STUB_CONTROL_BLANK=1 STUB_JLS_MAPS=0
# 10) P2 gate armed and satisfied: many pixels differ -> exit 0
run_case p2-pass 0 env PIXEL_DIFF_MIN=1000 STUB_DIFF=59998
# 11) P2 gate armed and violated: too few pixels differ -> exit 1 (JLS-side)
run_case p2-fail 1 env PIXEL_DIFF_MIN=1000 STUB_DIFF=5

# the degraded-control-blank run must emit the ::warning annotation and NOT the
# old exit-2 "environment fault" message - assert both.
degraded_out="$WORK/run-degraded-control-blank/rig.out"
if ! grep -q '::warning title=macOS pixel proof degraded' "$degraded_out"; then
	echo "selftest: FAIL  degraded-control-blank did not emit the ::warning pixel-proof annotation"; fail=1
fi
if ! grep -q 'window-map is the gate this run' "$degraded_out"; then
	echo "selftest: FAIL  degraded-control-blank warning missing the 'window-map is the gate' text"; fail=1
fi
if grep -q 'Screen Recording permission is likely withheld' "$degraded_out"; then
	echo "selftest: FAIL  degraded-control-blank still took the old exit-2 environment-fault path"; fail=1
fi

# spot-check the evidence the success run is supposed to leave behind
succ="$WORK/run-success/artifacts"
for f in control-verdict.txt desktop-before.png jls-window.png nonblank.txt jls-window-info.txt pixel-diff.txt; do
	if [ ! -s "$succ/$f" ]; then
		echo "selftest: FAIL  success run did not produce $f"; fail=1
	fi
done

[ "$fail" = 0 ] && echo "selftest: all scenarios classified correctly" \
	|| echo "selftest: one or more scenarios misclassified"
exit "$fail"
