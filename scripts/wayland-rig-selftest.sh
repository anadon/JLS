#!/usr/bin/env bash
# wayland-rig-selftest.sh - logic self-test for scripts/wayland-rig.sh
# (issue #101).
#
# The real rig (wayland-rig.sh) can only run where a Wayland-capable
# JetBrains Runtime and a sway compositor exist - i.e. in the CI
# gui-wayland lane, once the JBR tarball is fetchable.  Its *control
# flow* - the section-9 failure classification that decides between
# exit 0 (JLS window mapped), exit 1 (JLS-side failure), and exit 2
# (upstream JBR/sway blocker) - is pure shell and must not regress
# silently between now and first light.
#
# This harness drives the unmodified rig against a stub toolchain
# (fake sway/swaymsg/grim + a fake java that we script per case) and
# asserts the rig classifies each scenario with the documented exit
# code.  It needs no JBR, no real compositor, and no network, so it
# runs in this sandbox and as a fast CI check on every event.
#
# Usage:  scripts/wayland-rig-selftest.sh
# Exit 0 when every scenario classified correctly; 1 otherwise.

set -euo pipefail

RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
RIG="$RIG_DIR/wayland-rig.sh"
[ -x "$RIG" ] || { echo "selftest: $RIG is missing or not executable" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "selftest: jq is required (the rig parses the tree with it)" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "selftest: python3 is required (the sway stub binds the sockets)" >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

STUB_BIN="$WORK/bin"
mkdir -p "$STUB_BIN"

# --- stub sway: bind the two unix sockets the rig waits for, then idle.
# The rig polls XDG_RUNTIME_DIR for a wayland-* socket (to adopt as
# WAYLAND_DISPLAY) and a sway-ipc.*.sock (SWAYSOCK); both must be real
# AF_UNIX sockets (the rig tests `[ -S ]`).
cat > "$STUB_BIN/sway" <<'EOF'
#!/usr/bin/env bash
exec python3 - "$@" <<'PY'
import os, socket, signal, time, sys
run = os.environ["XDG_RUNTIME_DIR"]
socks = []
for name in ("wayland-1", "sway-ipc.1.sock"):
    p = os.path.join(run, name)
    try: os.unlink(p)
    except FileNotFoundError: pass
    s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    s.bind(p); s.listen(1)
    socks.append(s)
signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
while True:
    time.sleep(1)
PY
EOF

# --- stub swaymsg: answer the two IPC queries the rig makes.
#   -t get_outputs -> the readiness gate needs one active output, unless
#                     STUB_NO_OUTPUT=1 (models a compositor that came up
#                     with no usable output -> a rig/compositor error).
#   -t get_tree    -> a compositor tree per STUB_TREE_MODE:
#                       both         -> HelloSwingControl and JLS present (success)
#                       control-only -> only HelloSwingControl present
#                       empty        -> no windows (upstream blocker: control never maps)
cat > "$STUB_BIN/swaymsg" <<'EOF'
#!/usr/bin/env bash
type=""
while [ $# -gt 0 ]; do
  case "$1" in
    -t) type="${2:-}"; shift 2 ;;
    *)  shift ;;
  esac
done
if [ "$type" = "get_outputs" ]; then
  if [ "${STUB_NO_OUTPUT:-0}" = 1 ]; then
    echo '[]'
  else
    echo '[{"name":"HEADLESS-1","active":true,"current_mode":{"width":1280,"height":800}}]'
  fi
  exit 0
fi
# get_tree (default)
case "${STUB_TREE_MODE:-empty}" in
  both)         echo '{"nodes":[{"name":"HelloSwingControl","pid":1},{"name":"JLS 4.3","pid":2}]}' ;;
  control-only) echo '{"nodes":[{"name":"HelloSwingControl","pid":1}]}' ;;
  *)            echo '{"nodes":[]}' ;;
esac
EOF

# --- stub grim: write a byte-valid 1x1 PNG so the evidence steps succeed,
# unless STUB_GRIM_FAIL=1, which prints grim's real "failed to create
# display" connection error and exits nonzero (the readiness-gate probe
# failure: a rig/compositor error, NOT an upstream JBR blocker).
cat > "$STUB_BIN/grim" <<'EOF'
#!/usr/bin/env bash
if [ "${STUB_GRIM_FAIL:-0}" = 1 ]; then
  echo 'failed to create display' >&2
  exit 1
fi
out="${!#}"   # grim's last argument is the output path
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$out"
EOF

# --- stub java: -version prints one line; the control run idles until
# killed; the JLS run idles unless STUB_JLS_CRASH=1, which makes it exit
# nonzero *before* mapping a window (the exit-1 JLS-side-failure case).
cat > "$STUB_BIN/java" <<'EOF'
#!/usr/bin/env bash
for a in "$@"; do
  [ "$a" = "-version" ] && { echo 'stub-jbr version "25.0.3" (wayland-rig selftest)' >&2; exit 0; }
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

# fake JBR home + a placeholder jar the rig can find
JBR="$WORK/jbr"; mkdir -p "$JBR/bin"; cp "$STUB_BIN/java" "$JBR/bin/java"
mkdir -p "$WORK/target"; : > "$WORK/target/jls-selftest.jar"

fail=0
# run_case NAME EXPECTED_EXIT   (scenario knobs passed via the environment)
run_case() {
	name=$1; want=$2; shift 2
	rundir="$WORK/run-$name"; mkdir -p "$rundir/target"
	cp "$WORK/target/jls-selftest.jar" "$rundir/target/"
	got=0
	( cd "$rundir" \
		&& PATH="$STUB_BIN:$PATH" JBR_HOME="$JBR" \
		   ARTIFACTS_DIR="$rundir/artifacts" \
		   SWAY_TIMEOUT=10 CONTROL_TIMEOUT=3 WINDOW_TIMEOUT=5 \
		   "$@" "$RIG" ) > "$rundir/rig.out" 2>&1 || got=$?
	if [ "$got" = "$want" ]; then
		echo "selftest: PASS  $name (exit $got)"
	else
		echo "selftest: FAIL  $name (expected exit $want, got $got)"
		echo "----- rig output ($name) -----"; sed 's/^/    /' "$rundir/rig.out"
		fail=1
	fi
}

# 1) success: control maps, JLS maps -> exit 0
run_case success 0 env STUB_TREE_MODE=both
# 2) JLS-side failure: control maps, JLS crashes before a window -> exit 1
run_case jls-fail 1 env STUB_TREE_MODE=control-only STUB_JLS_CRASH=1
# 3) upstream blocker: the control window never maps -> exit 2
run_case upstream 2 env STUB_TREE_MODE=empty
# 4) rig/compositor error: the compositor came up but grim cannot connect
#    (dead/wrong socket). The readiness gate must catch this as a rig error
#    (exit 1) and NEVER let it fall through to the exit-2 "upstream JBR"
#    classification - the exact CI misdiagnosis this fix removes (issue #101).
run_case gate-grim-fail 1 env STUB_TREE_MODE=empty STUB_GRIM_FAIL=1
# 5) rig/compositor error: the compositor exposes no active output. The gate
#    must fail as a rig error (exit 1), not an upstream blocker.
run_case gate-no-output 1 env STUB_TREE_MODE=empty STUB_NO_OUTPUT=1 SWAY_TIMEOUT=2

# spot-check the evidence the success run is supposed to leave behind
succ="$WORK/run-success/artifacts"
for f in control-verdict.txt desktop-before.png desktop-after.png tree.json; do
	if [ ! -s "$succ/$f" ]; then
		echo "selftest: FAIL  success run did not produce $f"; fail=1
	fi
done

[ "$fail" = 0 ] && echo "selftest: all scenarios classified correctly" \
	|| echo "selftest: one or more scenarios misclassified"
exit "$fail"
