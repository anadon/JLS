#!/usr/bin/env bash
# x11-rig.sh - headless X11 first-light GUI rig for JLS.
#
# Boots the JLS GUI on the platform default AWT toolkit
# (sun.awt.X11.XToolkit) against a *headless* Xvfb X server - no GPU, no
# physical display, no window manager - then asserts that a window
# belonging to the JLS process actually maps, and captures the evidence:
#
#   $ARTIFACTS_DIR/xvfb.log             X server log
#   $ARTIFACTS_DIR/control-stdout.log   HelloSwingControl stdout
#   $ARTIFACTS_DIR/control-stderr.log   HelloSwingControl stderr
#   $ARTIFACTS_DIR/control-verdict.txt  did the control program's window map?
#   $ARTIFACTS_DIR/control.png          screenshot with the control window up
#   $ARTIFACTS_DIR/jls-stdout.log       JLS stdout
#   $ARTIFACTS_DIR/jls-stderr.log       JLS stderr (the first-light census input)
#   $ARTIFACTS_DIR/jls-window-info.txt  xwininfo of the mapped JLS window
#   $ARTIFACTS_DIR/desktop-before.png   root screenshot before anything launches
#   $ARTIFACTS_DIR/desktop-after.png    root screenshot after JLS mapped
#   $ARTIFACTS_DIR/jls-window.png       screenshot of just the JLS window
#   $ARTIFACTS_DIR/nonblank.txt         unique-color counts (the non-blank proof)
#   $ARTIFACTS_DIR/pixel-diff.txt       ImageMagick AE metric (empty vs JLS-up)
#
# WHY THIS LANE EXISTS (and how it differs from the xvfb JUnit suite):
# the main build already runs the display-tagged JUnit UI tests under
# `xvfb-run -a mvn -B verify -Djls.test.headless=false`. That exercises UI
# *unit* tests in-process. This rig is different: it launches the REAL
# application entry point (jls.JLS, the shaded jar's Main-Class) exactly as
# a user would - `java -jar target/jls-*.jar` - and proves the actual app
# window comes up under headless X11 and can be screenshotted. It is the
# X11 twin of the Wayland first-light rig (scripts/wayland-rig.sh): X11 via
# Xvfb is the reliable, always-available GUI path, so this lane is a
# genuine GUI-boot smoke test rather than the advisory/fragile Wayland leg.
#
# Before JLS, the rig launches scripts/HelloSwingControl.java - a minimal
# JFrame on the same runtime, toolkit, and X server - so every run
# separates the two failure modes by itself: control up + JLS down means a
# JLS startup defect; control down means the environment (Xvfb/AWT) is the
# blocker, and JLS is not blamed for it.
#
# Exit status:
#   0  a real JLS window mapped under Xvfb AND a non-blank screenshot was
#      captured (first light).
#   1  JLS-side failure: the control window mapped but JLS did not (no
#      window, crashed before mapping, or its window was blank).
#   2  environment/upstream failure: even the minimal control window never
#      mapped, or the rig could not bring up Xvfb / a required tool is
#      missing. Xvfb is normally rock-solid, so this points at the runner.
#
# Toolkit note: under Xvfb the platform default toolkit is correct. The rig
# sets DISPLAY to the Xvfb server, forces java.awt.headless=false, and
# does NOT pass -Dawt.toolkit.name (that selects Wayland's WLToolkit and is
# wrong here). It also unsets WAYLAND_DISPLAY in the child environment so
# JLS's startup toolkit policy (jls.ToolkitPolicy) keeps the X11 default:
# that policy selects WLToolkit only on a Wayland-ONLY session (WAYLAND_DISPLAY
# set, DISPLAY unset); with DISPLAY set and no WAYLAND_DISPLAY it returns
# DEFAULT, so no app change is needed.
#
# Requirements: Xvfb, xdotool, xwininfo, and ImageMagick's `import`/`identify`
# on PATH; a JDK on PATH; the shaded jar from `mvn -DskipTests package` (or
# point $JAR at one). ImageMagick's `compare` is optional unless
# PIXEL_DIFF_MIN is set (the pixel diff is always recorded; it also gates
# once PIXEL_DIFF_MIN is calibrated from the first green run's observed AE).
#
# Environment knobs (all optional):
#   JAVA              java binary to launch     [java on PATH]
#   JAR               jar to launch             [newest target/jls-*.jar]
#   ARTIFACTS_DIR     where evidence goes       [artifacts/x11-rig]
#   XVFB_DISPLAY_BASE starting display number Xvfb tries        [99]
#   XVFB_GEOMETRY     Xvfb screen geometry WxHxDEPTH            [1280x800x24]
#   XVFB_TIMEOUT      seconds to wait for the X server          [30]
#   CONTROL_TIMEOUT   seconds to wait for the control window    [60]
#   WINDOW_TIMEOUT    seconds to wait for the JLS window        [90]
#   RENDER_TIMEOUT    seconds to wait for the window to paint   [30]
#   PIXEL_DIFF_MIN    fail unless the desktop-before/after AE pixel
#                     difference is at least this many pixels; 0
#                     records the metric without gating         [0]

set -euo pipefail

log() { printf 'x11-rig: %s\n' "$*" >&2; }
# environment/rig faults are exit 2: Xvfb is always-available, so a setup
# failure here means the runner, not JLS. JLS-side failures use exit 1
# explicitly at their own call sites.
die_env() { printf 'x11-rig: error: %s\n' "$*" >&2; exit 2; }

# ---------------------------------------------------------------- config
ARTIFACTS_DIR="${ARTIFACTS_DIR:-artifacts/x11-rig}"
XVFB_DISPLAY_BASE="${XVFB_DISPLAY_BASE:-99}"
XVFB_GEOMETRY="${XVFB_GEOMETRY:-1280x800x24}"
XVFB_TIMEOUT="${XVFB_TIMEOUT:-30}"
CONTROL_TIMEOUT="${CONTROL_TIMEOUT:-60}"
WINDOW_TIMEOUT="${WINDOW_TIMEOUT:-90}"
# a mapped window paints a beat later; poll the screenshot until it renders
RENDER_TIMEOUT="${RENDER_TIMEOUT:-30}"
PIXEL_DIFF_MIN="${PIXEL_DIFF_MIN:-0}"

# the control program lives next to this script, wherever it is run from
RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTROL_SRC="$RIG_DIR/HelloSwingControl.java"

# Xvfb, the window-detect tools, and ImageMagick are all mandatory; their
# absence is an environment fault (exit 2), not a JLS failure.
for tool in Xvfb xdotool xwininfo import identify; do
	command -v "$tool" >/dev/null 2>&1 || die_env "$tool is not installed"
done

JAVA="${JAVA:-java}"
command -v "$JAVA" >/dev/null 2>&1 || die_env "no java found (set JAVA= or add one to PATH)"

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
	die_env "no jar found; run 'mvn -DskipTests package' first or set JAR="
fi

mkdir -p "$ARTIFACTS_DIR"
log "artifacts: $ARTIFACTS_DIR"
log "runtime:   $("$JAVA" -version 2>&1 | grep -iv 'picked up' | head -n 1)"
log "jar:       $JAR"

# ------------------------------------------------------------- processes
XVFB_PID=""
CONTROL_PID=""
JLS_PID=""
# invoked via trap, which shellcheck cannot see (SC2317 false positive)
# shellcheck disable=SC2317
cleanup() {
	status=$?
	# kill the clients first so Xvfb does not log their teardown as errors
	for pid in "$JLS_PID" "$CONTROL_PID" "$XVFB_PID"; do
		if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
			kill "$pid" 2>/dev/null || true
		fi
	done
	# bounded reap; Xvfb occasionally needs a moment to drop its lock
	for _ in 1 2 3 4 5; do
		{ [ -z "$XVFB_PID" ] || ! kill -0 "$XVFB_PID" 2>/dev/null; } && break
		sleep 1
	done
	exit "$status"
}
trap cleanup EXIT INT TERM

# ------------------------------------------------------------- start Xvfb
# `-displayfd 1` makes Xvfb auto-select a free display starting at
# XVFB_DISPLAY_BASE and print the chosen number on fd 1, so two rigs on one
# runner never collide on a display number. There is NO window manager:
# bare Xvfb maps Swing windows and screenshots them fine (verified), and a
# WM is deliberately avoided to keep the lane minimal and deterministic.
DISPLAY_FILE="$ARTIFACTS_DIR/xvfb-display.txt"
: > "$DISPLAY_FILE"
log "starting Xvfb (base :$XVFB_DISPLAY_BASE, geometry $XVFB_GEOMETRY)"
Xvfb ":$XVFB_DISPLAY_BASE" -displayfd 1 -screen 0 "$XVFB_GEOMETRY" \
	> "$DISPLAY_FILE" 2> "$ARTIFACTS_DIR/xvfb.log" &
XVFB_PID=$!

# wait for Xvfb to report its display number
DISPLAY_NUM=""
elapsed=0
while [ "$elapsed" -lt "$XVFB_TIMEOUT" ]; do
	kill -0 "$XVFB_PID" 2>/dev/null \
		|| { tail -n 20 "$ARTIFACTS_DIR/xvfb.log" >&2 || true; \
			die_env "Xvfb exited before reporting a display (see xvfb.log)"; }
	candidate="$(tr -d '[:space:]' < "$DISPLAY_FILE" 2>/dev/null || true)"
	if [ -n "$candidate" ]; then
		DISPLAY_NUM="$candidate"
		break
	fi
	sleep 1
	elapsed=$((elapsed + 1))
done
[ -n "$DISPLAY_NUM" ] || die_env "Xvfb did not report a display after ${XVFB_TIMEOUT}s (see xvfb.log)"
DISPLAY=":$DISPLAY_NUM"
export DISPLAY
# the child GUIs (and JLS's ToolkitPolicy) must NOT see a Wayland session,
# or the policy could flip to WLToolkit; belt-and-braces even though the
# rig usually runs where WAYLAND_DISPLAY is already unset
unset WAYLAND_DISPLAY || true
log "X server up: DISPLAY=$DISPLAY"

# --------------------------------------------------- X server readiness gate
# Before launching ANY client, prove the display is genuinely usable: a real
# tool must connect and a root screenshot must be written. This mirrors the
# Wayland rig's readiness gate; a failure here is an environment fault
# (exit 2), never blamed on JLS. The empty-desktop baseline it captures also
# feeds the P2 pixel comparison later.
if ! import -display "$DISPLAY" -window root "$ARTIFACTS_DIR/desktop-before.png" \
		2> "$ARTIFACTS_DIR/import-probe-stderr.log"; then
	probe_err="$(cat "$ARTIFACTS_DIR/import-probe-stderr.log" 2>/dev/null || true)"
	log "----- import probe stderr -----"
	printf '%s\n' "$probe_err" >&2
	die_env "could not screenshot the Xvfb root (DISPLAY=$DISPLAY): ${probe_err:-<no stderr>}. Environment/rig fault, not JLS."
fi
log "readiness gate passed: Xvfb root screenshot OK"

# unique_colors FILE -> prints the ImageMagick unique-color count (%k), or
# 0 when the file is missing/unreadable. A solid (blank) image has 1 colour;
# any real rendered window has many, so >1 is the always-on non-blank gate.
unique_colors() {
	if [ -s "$1" ]; then
		identify -format '%k' "$1" 2>/dev/null || echo 0
	else
		echo 0
	fi
}

# wait_for_window PID NAME_REGEX TIMEOUT
# Polls for a *mapped* (visible) window belonging to PID - authoritative,
# AWT sets _NET_WM_PID so `xdotool search --pid` matches - falling back to a
# visible window whose title matches NAME_REGEX. `--onlyvisible` is REQUIRED:
# without it a name search can also return a 1x1 IsUnMapped helper window and
# head -1 could pick that instead of the real frame. The chosen id is
# confirmed IsViewable via xwininfo. Sets WINDOW_ID and WAITED_SECONDS on
# success. Returns 0 (mapped), 1 (timeout), or 2 (the process exited first).
wait_for_window() {
	wfw_pid=$1
	wfw_regex=$2
	wfw_timeout=$3
	wfw_elapsed=0
	while [ "$wfw_elapsed" -lt "$wfw_timeout" ]; do
		kill -0 "$wfw_pid" 2>/dev/null || return 2
		wfw_id="$(xdotool search --onlyvisible --pid "$wfw_pid" 2>/dev/null | head -n 1 || true)"
		if [ -z "$wfw_id" ]; then
			wfw_id="$(xdotool search --onlyvisible --name "$wfw_regex" 2>/dev/null | head -n 1 || true)"
		fi
		if [ -n "$wfw_id" ] \
				&& xwininfo -id "$wfw_id" 2>/dev/null \
					| grep -q 'Map State: IsViewable'; then
			WINDOW_ID="$wfw_id"
			WAITED_SECONDS=$wfw_elapsed
			return 0
		fi
		sleep 1
		wfw_elapsed=$((wfw_elapsed + 1))
	done
	return 1
} # end of wait_for_window function

# ---------------------------------------------------- control experiment
# Map a minimal JFrame on the same runtime/toolkit/X server first. If even
# this cannot map a window the environment (Xvfb/AWT) is the blocker and JLS
# is not attempted (exit 2); if it maps and JLS later fails, the defect is in
# JLS's startup path (exit 1). HelloSwingControl is reused as-is; unlike the
# Wayland rig we pass NO -Dawt.toolkit.name (the X11 default is correct).
[ -f "$CONTROL_SRC" ] || die_env "control program missing: $CONTROL_SRC"
log "launching HelloSwingControl (environment-blocker control)"
"$JAVA" -Djava.awt.headless=false "$CONTROL_SRC" \
	> "$ARTIFACTS_DIR/control-stdout.log" 2> "$ARTIFACTS_DIR/control-stderr.log" &
CONTROL_PID=$!

control_status=0
wait_for_window "$CONTROL_PID" "HelloSwingControl" "$CONTROL_TIMEOUT" \
	|| control_status=$?
if [ "$control_status" -eq 0 ]; then
	printf 'control window mapped after %ss (id %s)\n' "$WAITED_SECONDS" "$WINDOW_ID" \
		> "$ARTIFACTS_DIR/control-verdict.txt"
	log "control window is up after ${WAITED_SECONDS}s (id $WINDOW_ID)"
	import -display "$DISPLAY" -window root "$ARTIFACTS_DIR/control.png" \
		|| log "warning: control screenshot failed (continuing)"
else
	if [ "$control_status" -eq 2 ]; then
		printf 'control process exited before mapping a window\n'
	else
		printf 'no control window after %ss\n' "$CONTROL_TIMEOUT"
	fi > "$ARTIFACTS_DIR/control-verdict.txt"
	log "----- last lines of control-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/control-stderr.log" >&2 || true
	die_env "the minimal Swing control failed to map a window; the blocker is the environment (Xvfb/AWT), not JLS"
fi

# clear the desktop again so JLS gets the same empty baseline
kill "$CONTROL_PID" 2>/dev/null || true
wait "$CONTROL_PID" 2>/dev/null || true
CONTROL_PID=""

# --------------------------------------------------------------- launch
# The real application entry point, launched exactly as a user would via the
# jar's Main-Class (jls.JLS). headless=false and DISPLAY are set; NO
# -Dawt.toolkit.name (Wayland-only); WAYLAND_DISPLAY already unset above.
log "launching JLS (real app entry point, jar Main-Class jls.JLS)"
"$JAVA" -Djava.awt.headless=false -jar "$JAR" \
	> "$ARTIFACTS_DIR/jls-stdout.log" 2> "$ARTIFACTS_DIR/jls-stderr.log" &
JLS_PID=$!

# ----------------------------------------------- wait for the JLS window
# The X server must grow a mapped window belonging to the JLS process,
# matched by pid (authoritative) or by a title containing "JLS" (belt and
# braces: the title is JLSInfo.version, e.g. "JLS 5.0").
found=""
jls_status=0
wait_for_window "$JLS_PID" "JLS" "$WINDOW_TIMEOUT" || jls_status=$?
if [ "$jls_status" -eq 0 ]; then
	found=yes
elif [ "$jls_status" -eq 2 ]; then
	import -display "$DISPLAY" -window root "$ARTIFACTS_DIR/desktop-after.png" 2>/dev/null || true
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	# control mapped, JLS did not: JLS-side failure (exit 1)
	printf 'x11-rig: error: JLS exited before mapping a window (see jls-stderr.log; the control mapped, so this is a JLS-side failure)\n' >&2
	exit 1
fi

# ------------------------------------------------------------- evidence
import -display "$DISPLAY" -window root "$ARTIFACTS_DIR/desktop-after.png" \
	|| log "warning: final root screenshot failed"

if [ -z "$found" ]; then
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	printf 'x11-rig: error: no JLS window mapped after %ss (the control mapped, so this is a JLS-side failure)\n' "$WINDOW_TIMEOUT" >&2
	exit 1
fi
log "JLS window is up after ${WAITED_SECONDS}s (id $WINDOW_ID)"

# record the window geometry/state as proof it is a real mapped frame
xwininfo -id "$WINDOW_ID" > "$ARTIFACTS_DIR/jls-window-info.txt" 2>&1 || true

# ------------------------------------------------- non-blank proof (P1)
# A mapped window is not enough - the screenshot must not be a blank frame.
# Swing shows the frame a beat before it paints its content, so on a slow
# runner an immediate capture can catch a still-blank window (seen in CI:
# window mapped, screenshot 1 colour). Poll the window screenshot until it
# has more than one unique colour - i.e. the app actually rendered - up to
# RENDER_TIMEOUT seconds. A window that genuinely never paints times out
# here and is still caught as a JLS-side failure by the gate below.
render_elapsed=0
win_colors=0
while : ; do
	import -display "$DISPLAY" -window "$WINDOW_ID" "$ARTIFACTS_DIR/jls-window.png" 2>/dev/null \
		|| log "warning: JLS window screenshot failed"
	win_colors="$(unique_colors "$ARTIFACTS_DIR/jls-window.png")"
	case "$win_colors" in ''|*[!0-9]*) win_colors=0 ;; esac
	{ [ "$win_colors" -gt 1 ] || [ "$render_elapsed" -ge "$RENDER_TIMEOUT" ]; } && break
	sleep 1
	render_elapsed=$((render_elapsed + 1))
done
# refresh the root shot so it reflects the now-painted window too
import -display "$DISPLAY" -window root "$ARTIFACTS_DIR/desktop-after.png" 2>/dev/null \
	|| log "warning: final root screenshot failed"
root_colors="$(unique_colors "$ARTIFACTS_DIR/desktop-after.png")"
{
	printf 'desktop-after unique colors: %s\n' "$root_colors"
	printf 'jls-window unique colors:    %s\n' "$win_colors"
	printf 'render wait: %ss\n' "$render_elapsed"
} > "$ARTIFACTS_DIR/nonblank.txt"
log "non-blank check after ${render_elapsed}s: root=$root_colors colors, window=$win_colors colors"

if [ -s "$ARTIFACTS_DIR/jls-window.png" ]; then
	gate_colors="$win_colors"
	gate_what="the JLS window screenshot"
else
	gate_colors="$root_colors"
	gate_what="the root screenshot"
fi
case "$gate_colors" in
	''|*[!0-9]*)
		printf 'x11-rig: error: could not read a unique-color count for %s (got %s); treating as a JLS-side failure\n' "$gate_what" "$gate_colors" >&2
		exit 1 ;;
esac
if [ "$gate_colors" -le 1 ]; then
	printf 'x11-rig: error: %s is blank (%s unique color); the JLS window mapped but rendered nothing - JLS-side failure\n' "$gate_what" "$gate_colors" >&2
	exit 1
fi

# --------------------------------------------- P2 pixel difference (optional)
# How many pixels changed between the empty desktop and the desktop with JLS
# on it. Always recorded; it gates only once PIXEL_DIFF_MIN is calibrated
# from the first green run's observed AE value (not guessed blind).
if command -v compare > /dev/null 2>&1 \
		&& [ -s "$ARTIFACTS_DIR/desktop-before.png" ] \
		&& [ -s "$ARTIFACTS_DIR/desktop-after.png" ]; then
	# `compare -metric AE` prints the count to stderr and exits nonzero when
	# images differ - exactly what we hope for here
	diff_count="$(compare -metric AE "$ARTIFACTS_DIR/desktop-before.png" \
		"$ARTIFACTS_DIR/desktop-after.png" null: 2>&1 || true)"
	# ImageMagick may append a total after a space (e.g. "581770 (0.00887)")
	diff_count="${diff_count%% *}"
	printf '%s\n' "$diff_count" > "$ARTIFACTS_DIR/pixel-diff.txt"
	log "pixels differing from empty desktop (AE): $diff_count"
	if [ "$PIXEL_DIFF_MIN" -gt 0 ] 2>/dev/null; then
		case "$diff_count" in
			''|*[!0-9]*)
				printf 'x11-rig: error: P2 gate is armed (PIXEL_DIFF_MIN=%s) but the AE metric was unreadable: %s\n' "$PIXEL_DIFF_MIN" "$diff_count" >&2
				exit 1 ;;
		esac
		if [ "$diff_count" -lt "$PIXEL_DIFF_MIN" ]; then
			printf 'x11-rig: error: P2 failed: only %s pixels differ from the empty desktop (need >= %s) - JLS-side failure\n' "$diff_count" "$PIXEL_DIFF_MIN" >&2
			exit 1
		fi
	fi
elif [ "${PIXEL_DIFF_MIN:-0}" -gt 0 ] 2>/dev/null; then
	die_env "P2 gate is armed (PIXEL_DIFF_MIN=$PIXEL_DIFF_MIN) but ImageMagick 'compare' or a screenshot is missing"
fi

log "first light: success"
exit 0
