#!/usr/bin/env bash
# wayland-rig.sh - first-light Wayland GUI rig for JLS (issue #101).
#
# Boots the JLS GUI on a Wayland-capable Java runtime (JetBrains Runtime
# with OpenJDK's experimental Wakefield WLToolkit) against a *headless*
# sway compositor - no X11, no GPU, no physical display - then asserts
# that a window belonging to the JLS process actually appears in the
# compositor's tree, and captures the evidence:
#
#   $ARTIFACTS_DIR/sway.log             compositor log
#   $ARTIFACTS_DIR/control-stdout.log   HelloSwingControl stdout
#   $ARTIFACTS_DIR/control-stderr.log   HelloSwingControl stderr
#   $ARTIFACTS_DIR/control-verdict.txt  did the control program's window map?
#   $ARTIFACTS_DIR/control.png          screenshot with the control window up
#   $ARTIFACTS_DIR/jls-stdout.log       JLS stdout
#   $ARTIFACTS_DIR/jls-stderr.log       JLS stderr (the first-light census input)
#   $ARTIFACTS_DIR/tree.json            swaymsg -t get_tree at the end
#   $ARTIFACTS_DIR/desktop-before.png   screenshot before anything launches
#   $ARTIFACTS_DIR/desktop-after.png    screenshot after the window appeared
#   $ARTIFACTS_DIR/pixel-diff.txt       ImageMagick AE metric
#
# Before JLS, the rig launches scripts/HelloSwingControl.java - a minimal
# JFrame on the same runtime, toolkit, and compositor - so every run
# separates the two failure modes by itself (issue #101 section 9):
# control up + JLS down means a JLS startup defect; control down means
# the blocker is upstream (JBR/Wakefield or sway), and JLS is not even
# attempted.
#
# Exit status: 0 when the JLS window appeared; 2 when the control
# program's window never appeared (upstream blocker); 1 otherwise
# (JLS-side failure or rig error).
#
# Requirements: sway, swaymsg, grim, jq; a JBR under $JBR_HOME; the shaded
# jar from `mvn -DskipTests package` (or point $JAR at one). ImageMagick's
# `compare` is optional unless PIXEL_DIFF_MIN is set (pixel diff is always
# recorded; it also gates - #101 P2 - once PIXEL_DIFF_MIN is calibrated
# from the first green run's observed AE value).
#
# Environment knobs (all optional except JBR_HOME):
#   JBR_HOME        JetBrains Runtime root (must contain bin/java)  [required]
#   JAR             jar to launch          [newest target/jls-*.jar]
#   ARTIFACTS_DIR   where evidence goes    [artifacts/wayland-rig]
#   SWAY_TIMEOUT    seconds to wait for the compositor socket    [30]
#   CONTROL_TIMEOUT seconds to wait for the control window       [60]
#   WINDOW_TIMEOUT  seconds to wait for the JLS window           [90]
#   PIXEL_DIFF_MIN  fail unless the desktop-before/after AE pixel
#                   difference is at least this many pixels; 0
#                   records the metric without gating         [0]

set -euo pipefail

log() { printf 'wayland-rig: %s\n' "$*" >&2; }
die() { printf 'wayland-rig: error: %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- config
ARTIFACTS_DIR="${ARTIFACTS_DIR:-artifacts/wayland-rig}"
SWAY_TIMEOUT="${SWAY_TIMEOUT:-30}"
CONTROL_TIMEOUT="${CONTROL_TIMEOUT:-60}"
WINDOW_TIMEOUT="${WINDOW_TIMEOUT:-90}"
PIXEL_DIFF_MIN="${PIXEL_DIFF_MIN:-0}"

# the control program lives next to this script, wherever it is run from
RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTROL_SRC="$RIG_DIR/HelloSwingControl.java"

for tool in sway swaymsg grim jq; do
	command -v "$tool" >/dev/null 2>&1 || die "$tool is not installed"
done

[ -n "${JBR_HOME:-}" ] || die "set JBR_HOME to a JetBrains Runtime root (a JDK that ships WLToolkit)"
JAVA="$JBR_HOME/bin/java"
[ -x "$JAVA" ] || die "no executable java at $JAVA"

# newest shaded jar unless the caller picked one
if [ -z "${JAR:-}" ]; then
	JAR=""
	for candidate in target/jls-*.jar; do
		[ -f "$candidate" ] || continue
		if [ -z "$JAR" ] || [ "$candidate" -nt "$JAR" ]; then
			JAR="$candidate"
		fi
	done
fi
if [ -z "${JAR:-}" ] || [ ! -f "$JAR" ]; then
	die "no jar found; run 'mvn -DskipTests package' first or set JAR="
fi

mkdir -p "$ARTIFACTS_DIR"
log "artifacts: $ARTIFACTS_DIR"
log "runtime:   $("$JAVA" -version 2>&1 | head -n 1)"
log "jar:       $JAR"

# The compositor and every client must share ONE runtime dir that holds
# exactly one wayland-* socket. Reusing the inherited XDG_RUNTIME_DIR is
# unsafe: on a shared CI runner (/run/user/UID) it can already hold a
# stale or foreign wayland-* socket, and the rig's filename-based socket
# adoption below would then export that dead name instead of this sway's
# real socket - so every client gets "failed to create display" and the
# lane mis-blames upstream JBR (issue #101). Always use a fresh, private,
# rig-owned dir so exactly one wayland-* socket exists and adoption is
# unambiguous. sway also refuses to run without a writable dir it owns,
# which this satisfies on runners that have none.
RIG_RUNTIME_DIR="$(mktemp -d "${TMPDIR:-/tmp}/wayland-rig-run.XXXXXX")"
XDG_RUNTIME_DIR="$RIG_RUNTIME_DIR"
export XDG_RUNTIME_DIR
chmod 700 "$XDG_RUNTIME_DIR"
log "private XDG_RUNTIME_DIR: $XDG_RUNTIME_DIR"

# a minimal config: the stock /etc/sway/config assumes wallpapers, idle
# helpers, and input devices that a headless CI runner does not have
SWAY_CONFIG="$ARTIFACTS_DIR/sway-config"
cat > "$SWAY_CONFIG" <<'EOF'
# minimal headless config for the JLS first-light rig (issue #101).
# Pin the headless output deterministically instead of trusting wlroots'
# default output autocreation: a wlroots build that defaults to 0 headless
# outputs would otherwise leave the compositor with no wl_output, and every
# client (grim's screenshot, the JBR control frame) would fail. Paired with
# WLR_HEADLESS_OUTPUTS=1 in sway's environment (which creates HEADLESS-1),
# this names, sizes, and enables it. Nothing external is spawned.
output HEADLESS-1 mode 1280x800 position 0 0
output HEADLESS-1 enable
EOF

# ------------------------------------------------------------- processes
SWAY_PID=""
CONTROL_PID=""
JLS_PID=""
# invoked via trap, which shellcheck cannot see (SC2317 false positive)
# shellcheck disable=SC2317
cleanup() {
	status=$?
	# kill the clients first so sway does not log their teardown as errors
	for pid in "$JLS_PID" "$CONTROL_PID" "$SWAY_PID"; do
		if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
			kill "$pid" 2>/dev/null || true
		fi
	done
	# bounded reap; sway occasionally needs a moment to drop the socket
	for _ in 1 2 3 4 5; do
		{ [ -z "$SWAY_PID" ] || ! kill -0 "$SWAY_PID" 2>/dev/null; } && break
		sleep 1
	done
	# remove the private runtime dir we created (sockets and all)
	if [ -n "${RIG_RUNTIME_DIR:-}" ]; then
		rm -rf "$RIG_RUNTIME_DIR" 2>/dev/null || true
	fi
	exit "$status"
}
trap cleanup EXIT INT TERM

# start the compositor on wlroots' headless backend with the pixman
# software renderer - verified working in the dev container (issue #101)
log "starting headless sway"
env WLR_BACKENDS=headless WLR_LIBINPUT_NO_DEVICES=1 WLR_RENDERER=pixman \
	WLR_HEADLESS_OUTPUTS=1 \
	sway -c "$SWAY_CONFIG" > "$ARTIFACTS_DIR/sway.log" 2>&1 &
SWAY_PID=$!

# wait for the compositor's wayland socket to appear, then adopt it
WAYLAND_DISPLAY=""
elapsed=0
while [ "$elapsed" -lt "$SWAY_TIMEOUT" ]; do
	kill -0 "$SWAY_PID" 2>/dev/null \
		|| { tail -n 20 "$ARTIFACTS_DIR/sway.log" >&2 || true; \
			die "sway exited before creating a socket (see sway.log)"; }
	for sock in "$XDG_RUNTIME_DIR"/wayland-*; do
		if [ -S "$sock" ]; then
			WAYLAND_DISPLAY="$(basename "$sock")"
			break 2
		fi
	done
	sleep 1
	elapsed=$((elapsed + 1))
done
[ -n "$WAYLAND_DISPLAY" ] || die "no wayland socket after ${SWAY_TIMEOUT}s (see sway.log)"
export WAYLAND_DISPLAY
log "compositor up: WAYLAND_DISPLAY=$WAYLAND_DISPLAY"

# swaymsg needs the IPC socket; from outside sway's own environment it
# must be located explicitly
SWAYSOCK=""
for sock in "$XDG_RUNTIME_DIR"/sway-ipc.*.sock; do
	[ -S "$sock" ] && SWAYSOCK="$sock"
done
[ -n "$SWAYSOCK" ] || die "sway is running but no IPC socket was found in $XDG_RUNTIME_DIR"
export SWAYSOCK

# ---------------------------------------------- compositor readiness gate
# Before launching ANY client, prove the compositor is genuinely usable:
# (a) it must have an active output, and (b) a real client must be able to
# connect to the adopted socket and screenshot. This gate replaces the old
# "baseline screenshot, warn and continue", which swallowed a failed grim
# and carried a dead compositor forward - later mis-attributing it to
# upstream JBR/Wakefield as an exit-2 blocker (issue #101). A failure here
# is a RIG/COMPOSITOR error (exit 1); it must never fall through to the
# section-9 upstream classification.
log "waiting for an active compositor output"
outputs_ready=""
elapsed=0
while [ "$elapsed" -lt "$SWAY_TIMEOUT" ]; do
	if swaymsg -t get_outputs 2>/dev/null \
			| jq -e '[.. | objects | select(.active? == true)] | length > 0' \
			> /dev/null 2>&1; then
		outputs_ready=yes
		break
	fi
	sleep 1
	elapsed=$((elapsed + 1))
done
swaymsg -t get_outputs > "$ARTIFACTS_DIR/outputs.json" 2>/dev/null || true
if [ -z "$outputs_ready" ]; then
	tail -n 20 "$ARTIFACTS_DIR/sway.log" >&2 || true
	die "compositor has no active output after ${SWAY_TIMEOUT}s (rig/compositor error, not JLS: headless output not created; see sway.log and outputs.json)"
fi

# The empty-desktop baseline for the P2 pixel comparison doubles as the
# connect+screenshot probe: require grim to connect to the adopted socket
# AND write a file. Its failure text is diagnostic and is classified here,
# not blamed on JBR: "failed to create display" = a socket/connection fault
# (the adopted WAYLAND_DISPLAY is wrong or dead); "no wl_output" = no usable
# output. Either way it is a rig/compositor fault.
if ! grim "$ARTIFACTS_DIR/desktop-before.png" \
		2> "$ARTIFACTS_DIR/grim-probe-stderr.log"; then
	grim_err="$(cat "$ARTIFACTS_DIR/grim-probe-stderr.log" 2>/dev/null || true)"
	log "----- grim probe stderr -----"
	printf '%s\n' "$grim_err" >&2
	case "$grim_err" in
		*"failed to create display"*)
			die "grim could not connect to the compositor (WAYLAND_DISPLAY=$WAYLAND_DISPLAY, XDG_RUNTIME_DIR=$XDG_RUNTIME_DIR): socket/connection fault - a wrong or dead wayland socket was adopted. Rig/compositor error, not JLS." ;;
		*"no wl_output"*)
			die "grim connected but the compositor exposes no usable output (headless output misconfigured). Rig/compositor error, not JLS." ;;
		*)
			die "grim failed to capture the baseline screenshot: ${grim_err:-<no stderr>}. Rig/compositor error, not JLS." ;;
	esac
fi
log "readiness gate passed: active output + grim baseline screenshot OK"

# wait_for_window PID NAME_REGEX TIMEOUT
# Polls the compositor tree for a window belonging to PID (authoritative)
# or whose name matches NAME_REGEX (belt and braces).  Returns 0 when the
# window appeared, 1 on timeout, 2 when the process exited first.
wait_for_window() {
	wfw_pid=$1
	wfw_regex=$2
	wfw_timeout=$3
	wfw_elapsed=0
	while [ "$wfw_elapsed" -lt "$wfw_timeout" ]; do
		kill -0 "$wfw_pid" 2>/dev/null || return 2
		if swaymsg -t get_tree 2>/dev/null \
				| jq -e --argjson pid "$wfw_pid" --arg re "$wfw_regex" \
					'[.. | objects | select((.pid? == $pid)
						or ((.name? // "") | test($re)))] | length > 0' \
				> /dev/null; then
			WAITED_SECONDS=$wfw_elapsed
			return 0
		fi
		sleep 1
		wfw_elapsed=$((wfw_elapsed + 1))
	done
	return 1
} # end of wait_for_window function

# ---------------------------------------------------- control experiment
# Issue #101 section 9: before JLS, map a minimal JFrame on the exact
# same runtime/toolkit/compositor.  If even this cannot map a window the
# blocker is upstream (JBR/Wakefield or sway) and JLS is not attempted;
# if it maps and JLS later fails, the defect is in JLS's startup path.
[ -f "$CONTROL_SRC" ] || die "control program missing: $CONTROL_SRC"
log "launching HelloSwingControl (upstream-blocker control)"
env -u DISPLAY "$JAVA" -Dawt.toolkit.name=WLToolkit "$CONTROL_SRC" \
	> "$ARTIFACTS_DIR/control-stdout.log" 2> "$ARTIFACTS_DIR/control-stderr.log" &
CONTROL_PID=$!

control_status=0
wait_for_window "$CONTROL_PID" "HelloSwingControl" "$CONTROL_TIMEOUT" \
	|| control_status=$?
if [ "$control_status" -eq 0 ]; then
	printf 'control window mapped after %ss\n' "$WAITED_SECONDS" \
		> "$ARTIFACTS_DIR/control-verdict.txt"
	log "control window is up after ${WAITED_SECONDS}s"
	grim "$ARTIFACTS_DIR/control.png" \
		|| log "warning: control screenshot failed (continuing)"
else
	if [ "$control_status" -eq 2 ]; then
		printf 'control process exited before mapping a window\n'
	else
		printf 'no control window after %ss\n' "$CONTROL_TIMEOUT"
	fi > "$ARTIFACTS_DIR/control-verdict.txt"
	swaymsg -t get_tree > "$ARTIFACTS_DIR/tree.json" 2>/dev/null || true
	log "----- last lines of control-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/control-stderr.log" >&2 || true
	log "error: the minimal Swing control failed to map a window;" \
		"the blocker is upstream (JBR/Wakefield or sway), not JLS (issue #101 section 9)"
	exit 2
fi

# clear the desktop again so JLS gets the same empty baseline
kill "$CONTROL_PID" 2>/dev/null || true
wait "$CONTROL_PID" 2>/dev/null || true
CONTROL_PID=""

# --------------------------------------------------------------- launch
# -Dawt.toolkit.name=WLToolkit is what selects Wakefield's toolkit; JLS's
# own startup policy (issue #105) would also set it on this Wayland-only
# session, but the rig pins it explicitly so the lane tests the toolkit,
# not the policy
log "launching JLS"
env -u DISPLAY "$JAVA" -Dawt.toolkit.name=WLToolkit -jar "$JAR" \
	> "$ARTIFACTS_DIR/jls-stdout.log" 2> "$ARTIFACTS_DIR/jls-stderr.log" &
JLS_PID=$!

# ----------------------------------------------- wait for the JLS window
# P1 (issue #101): the compositor tree must grow a window belonging to
# the JLS process. Matched by pid (authoritative) or by a name containing
# "JLS" (belt and braces: the title is JLSInfo.version, e.g. "JLS 4.3").
found=""
jls_status=0
wait_for_window "$JLS_PID" "JLS" "$WINDOW_TIMEOUT" || jls_status=$?
if [ "$jls_status" -eq 0 ]; then
	found=yes
elif [ "$jls_status" -eq 2 ]; then
	swaymsg -t get_tree > "$ARTIFACTS_DIR/tree.json" 2>/dev/null || true
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	die "JLS exited before mapping a window (see jls-stderr.log; the control mapped, so this is a JLS-side failure)"
fi

# ------------------------------------------------------------- evidence
swaymsg -t get_tree > "$ARTIFACTS_DIR/tree.json" 2>/dev/null || true
grim "$ARTIFACTS_DIR/desktop-after.png" \
	|| log "warning: final screenshot failed"

if [ -z "$found" ]; then
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	die "no JLS window in the compositor tree after ${WINDOW_TIMEOUT}s (the control mapped, so this is a JLS-side failure)"
fi
log "JLS window is up after ${WAITED_SECONDS}s"

# P2 pixel difference: how many pixels changed between the empty desktop
# and the desktop with JLS on it.  Always recorded; it gates only once
# PIXEL_DIFF_MIN is calibrated (issue #101: set from the first green
# run's observed AE value, roughly a tenth of it, not guessed blind).
if command -v compare > /dev/null 2>&1 \
		&& [ -f "$ARTIFACTS_DIR/desktop-before.png" ] \
		&& [ -f "$ARTIFACTS_DIR/desktop-after.png" ]; then
	# `compare -metric AE` prints the count to stderr and exits nonzero
	# when images differ - which is exactly what we hope for here
	diff_count="$(compare -metric AE "$ARTIFACTS_DIR/desktop-before.png" \
		"$ARTIFACTS_DIR/desktop-after.png" null: 2>&1 || true)"
	printf '%s\n' "$diff_count" > "$ARTIFACTS_DIR/pixel-diff.txt"
	log "pixels differing from empty desktop (AE): $diff_count"
	if [ "$PIXEL_DIFF_MIN" -gt 0 ] 2>/dev/null; then
		case "$diff_count" in
			''|*[!0-9]*)
				die "P2 gate is armed (PIXEL_DIFF_MIN=$PIXEL_DIFF_MIN) but the AE metric was unreadable: '$diff_count'" ;;
		esac
		[ "$diff_count" -ge "$PIXEL_DIFF_MIN" ] \
			|| die "P2 failed: only $diff_count pixels differ from the empty desktop (need >= $PIXEL_DIFF_MIN)"
	fi
elif [ "${PIXEL_DIFF_MIN:-0}" -gt 0 ] 2>/dev/null; then
	die "P2 gate is armed (PIXEL_DIFF_MIN=$PIXEL_DIFF_MIN) but ImageMagick 'compare' or a screenshot is missing"
fi

log "first light: success"
exit 0
