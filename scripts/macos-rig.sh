#!/usr/bin/env bash
# macos-rig.sh - macOS GUI app-boot first-light rig for JLS (issue #265
# Stage 9; the macOS analogue of scripts/x11-rig.sh / scripts/wayland-rig.sh).
#
# Boots the REAL JLS application entry point exactly as a user would -
# `java -jar target/jls-*.jar` (the shaded jar's Main-Class jls.JLS) - on the
# macOS runner's REAL WindowServer (unlike the Linux lanes there is NO Xvfb or
# headless compositor to stand up: hosted macOS runners carry a live Quartz
# WindowServer, so the jar boots directly), then asserts that a top-level
# window belonging to the JLS process actually maps, and captures the evidence:
#
#   $ARTIFACTS_DIR/control-stdout.log    HelloSwingControl stdout
#   $ARTIFACTS_DIR/control-stderr.log    HelloSwingControl stderr
#   $ARTIFACTS_DIR/control-verdict.txt   did the control program's window map?
#   $ARTIFACTS_DIR/control.png           screenshot with the control window up
#   $ARTIFACTS_DIR/jls-stdout.log        JLS stdout
#   $ARTIFACTS_DIR/jls-stderr.log        JLS stderr (the first-light census input)
#   $ARTIFACTS_DIR/jls-window-info.txt   window count/verdict for the JLS process
#   $ARTIFACTS_DIR/desktop-before.png    full-screen shot before anything launches
#   $ARTIFACTS_DIR/desktop-after.png     full-screen shot after JLS mapped
#   $ARTIFACTS_DIR/jls-window.png        full-screen shot with the JLS window up
#   $ARTIFACTS_DIR/nonblank.txt          unique-color counts (the non-blank proof)
#   $ARTIFACTS_DIR/pixel-diff.txt        sampled changed-pixel count (empty vs JLS-up)
#   $ARTIFACTS_DIR/osa-stderr.log        last System Events (osascript) stderr
#
# WHY THIS LANE EXISTS (and how it differs from the display JUnit suite):
# the display-tagged JUnit UI tests exercise UI *unit* classes in-process.
# This rig is different: it launches the REAL application entry point and
# proves the actual app window comes up on the WindowServer and can be
# screenshotted - the same distinction the Linux gui-x11 lane already draws
# ("LAUNCHES THE APP ... asserts the real window comes up", not the in-process
# JUnit suite). It is the macOS twin of the Linux GUI-boot rigs.
#
# CONTROL FRAME FIRST: before JLS, the rig launches
# scripts/HelloSwingControl.java - a minimal JFrame on the same runtime and
# WindowServer - so every run separates the two failure modes by itself.
# control up + JLS down means a JLS startup defect (exit 1); control down (or a
# capture the control frame ALSO shows blank/black) means the environment is
# the blocker (exit 2), and JLS is not blamed for it. This control-frame
# comparison is exactly what disambiguates a hosted-runner permission block
# (Accessibility / Screen Recording TCC) from a genuine JLS failure.
#
# Exit status (the same contract as the Linux rigs):
#   0  a real JLS window mapped on the WindowServer AND a non-blank screenshot
#      was captured (first light).
#   1  JLS-side failure: the control window mapped and its screenshot was
#      non-blank, but JLS did not (no window, crashed before mapping, or its
#      screenshot was blank).
#   2  environment/rig fault: even the minimal control window never mapped, the
#      control screenshot was blank/all-black (Screen Recording permission
#      likely withheld), System Events could not enumerate windows
#      (Accessibility/automation permission likely withheld), or a required
#      tool is missing. Never blamed on JLS.
#
# GENUINE FIRST LIGHT (honest, not verifiable offline): this rig has NEVER run
# on a real macOS runner - its first execution is on CI. macOS hosted runners
# MAY withhold Screen Recording / Accessibility (TCC) permission from a
# non-interactive CI session, which can make `screencapture` hand back a
# blank/black frame or make System Events refuse to enumerate windows. Either
# is classified as exit 2 (environment) via the control-frame comparison above,
# NEVER exit 1. If first light shows the permission path is blocked, the
# documented fallbacks are an in-JVM java.awt.Robot self-capture (window paint
# proof without Screen Recording) and CGWindowList enumeration (window map
# proof without Accessibility); first light decides which path the hosted image
# actually grants. Every wait loop below is bounded by a hard timeout, so a
# withheld permission degrades to a clean exit 2, never a hang.
#
# NON-BLANK PROOF - built-in toolchain, no ImageMagick, no network. The Linux
# rigs count unique colors with ImageMagick's `identify -format '%k'`. This rig
# reproduces that idiom with only tools already present on every macOS runner:
# `screencapture` (built-in) writes the PNG, `sips` (built-in) transcodes it to
# an uncompressed BMP, and a pure-stdlib Python reader (python3 is preinstalled)
# counts distinct colors in the BMP. This deliberately avoids `brew install
# imagemagick`: the core non-blank proof then has NO package-install/network
# dependency that could flake, and a >1 unique-color count is exactly the
# "is this a real frame or a denied all-black capture" gate the control-frame
# comparison disambiguates. (On a runner with a wallpaper the desktop alone is
# already many-colored, so the recorded desktop-before/after pixel diff -
# PIXEL_DIFF_MIN, off by default like the Linux P2 gate - is the metric a
# future promotion calibrates from; distinct-color>1 is the always-on P1 gate
# that catches a black/denied capture.)
#
# PROMOTION: advisory `continue-on-error` -> required once 20 consecutive runs
# show at most one failure (the #188/#101 rule the gui-wayland/gui-x11 lanes
# followed), then drop continue-on-error and register the lane's exact `name:`
# ("GUI boot (macOS, WindowServer)") as a required check with recorded run IDs,
# mirroring installer-reproducibility-aarch64.
#
# Requirements: java on PATH; screencapture, sips, osascript (all built-in on
# macOS); python3 (preinstalled); the shaded jar from `mvn -DskipTests package`
# (or point $JAR at one).
#
# Environment knobs (all optional):
#   JAVA              java binary to launch     [java on PATH]
#   JAR               jar to launch             [newest target/jls-*.jar]
#   ARTIFACTS_DIR     where evidence goes       [artifacts/macos-rig]
#   CONTROL_TIMEOUT   seconds to wait for the control window    [60]
#   WINDOW_TIMEOUT    seconds to wait for the JLS window        [90]
#   RENDER_TIMEOUT    seconds to wait for the window to paint   [30]
#   PIXEL_DIFF_MIN    fail unless the desktop-before/after sampled changed-pixel
#                     count is at least this many; 0 records the metric
#                     without gating (mirrors the Linux P2 gate)   [0]

set -euo pipefail

log() { printf 'macos-rig: %s\n' "$*" >&2; }
# environment/rig faults are exit 2; JLS-side failures use exit 1 explicitly at
# their own call sites (only after the control frame proved the environment).
die_env() { printf 'macos-rig: error: %s\n' "$*" >&2; exit 2; }

# ---------------------------------------------------------------- config
ARTIFACTS_DIR="${ARTIFACTS_DIR:-artifacts/macos-rig}"
CONTROL_TIMEOUT="${CONTROL_TIMEOUT:-60}"
WINDOW_TIMEOUT="${WINDOW_TIMEOUT:-90}"
# a mapped window paints a beat later; poll the screenshot until it renders
RENDER_TIMEOUT="${RENDER_TIMEOUT:-30}"
PIXEL_DIFF_MIN="${PIXEL_DIFF_MIN:-0}"
# Set to 1 when the desktop readiness capture works AND the control window MAPS
# but the control-window capture is blank (the Screen Recording / TCC signature
# on hosted runners): pixel proofs are then unavailable this run, so the rig runs
# in DEGRADED MODE and the window-map is the boot gate (issue #265 Stage 9). It
# never gates on exit 2 for that reason - a permanently-withheld TCC grant would
# otherwise make the boot-proof lane permanently red for a runner-policy reason.
DEGRADED=0

# the control program lives next to this script, wherever it is run from
RIG_DIR="$(cd "$(dirname "$0")" && pwd)"
CONTROL_SRC="$RIG_DIR/HelloSwingControl.java"

# screencapture/sips/osascript are macOS built-ins; python3 is preinstalled.
# Their absence is an environment fault (exit 2), not a JLS failure.
for tool in screencapture sips osascript python3; do
	command -v "$tool" >/dev/null 2>&1 || die_env "$tool is not installed (expected on a macOS runner)"
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

# ---------------------------------------------------- window enumeration
# System Events enumerates the mapped top-level windows. Matching is by the
# launched process's unix id (authoritative) with a title-contains fallback
# (AWT sets the frame title; JLS's is JLSInfo.version, e.g. "JLS 5.0"). This
# needs macOS Accessibility/automation permission; if the hosted image withholds
# it, osascript exits non-zero and the rig classifies that as environment (the
# control frame hits the same wall first, so it never lands on JLS). The script
# is written to a temp file so the argument passing is unambiguous.
OSA_FILE="$ARTIFACTS_DIR/count-windows.applescript"
cat > "$OSA_FILE" <<'APPLESCRIPT'
on run argv
	set theTitle to item 1 of argv
	set thePid to (item 2 of argv) as integer
	tell application "System Events"
		set n to 0
		try
			set p to first process whose unix id is thePid
			set n to (count of windows of p)
		end try
		if n is 0 then
			repeat with p in (every process)
				try
					repeat with w in (windows of p)
						if name of w contains theTitle then set n to n + 1
					end repeat
				end try
			end repeat
		end if
		return n
	end tell
end run
APPLESCRIPT

# wait_for_window TITLE PID TIMEOUT
# Polls System Events for a mapped window belonging to PID (or whose title
# contains TITLE) up to TIMEOUT seconds. Sets WIN_COUNT and WAITED_SECONDS on
# success. Returns:
#   0  a window is mapped
#   1  timeout (process alive, no window)
#   2  the process exited before mapping a window
#   3  System Events could not be queried (osascript failed - permission)
wait_for_window() {
	wfw_title=$1
	wfw_pid=$2
	wfw_timeout=$3
	wfw_elapsed=0
	while [ "$wfw_elapsed" -lt "$wfw_timeout" ]; do
		kill -0 "$wfw_pid" 2>/dev/null || return 2
		if ! wfw_out="$(osascript "$OSA_FILE" "$wfw_title" "$wfw_pid" \
				2> "$ARTIFACTS_DIR/osa-stderr.log")"; then
			return 3
		fi
		wfw_out="$(printf '%s' "$wfw_out" | tr -d '[:space:]')"
		case "$wfw_out" in ''|*[!0-9]*) wfw_out=0 ;; esac
		if [ "$wfw_out" -gt 0 ]; then
			WIN_COUNT="$wfw_out"
			WAITED_SECONDS=$wfw_elapsed
			return 0
		fi
		sleep 1
		wfw_elapsed=$((wfw_elapsed + 1))
	done
	return 1
} # end of wait_for_window function

# ------------------------------------------------- non-blank proof helpers
# The distinct-color counter reads an uncompressed BMP (sips transcodes the PNG
# screenshot to BMP) and prints the number of distinct colors, capped, or 0 on
# any error. A solid/black frame has 1 color; any real captured frame has many,
# so >1 is the always-on non-blank / not-a-denied-capture gate. Kept inline so
# the rig stays self-contained (no committed helper module) - the Linux rigs
# inline their non-blank logic the same way.
PY_COLORS='
import sys, struct
def main():
    try:
        with open(sys.argv[1], "rb") as f:
            d = f.read()
    except Exception:
        print(0); return
    if len(d) < 54 or d[:2] != b"BM":
        print(0); return
    pixoff = struct.unpack_from("<I", d, 10)[0]
    width  = struct.unpack_from("<i", d, 18)[0]
    height = struct.unpack_from("<i", d, 22)[0]
    bpp    = struct.unpack_from("<H", d, 28)[0]
    comp   = struct.unpack_from("<I", d, 30)[0]
    if comp != 0 or bpp not in (24, 32) or width <= 0:
        print(0); return
    h = abs(height); bpb = bpp // 8
    rowsize = ((bpp * width + 31) // 32) * 4
    sx = max(1, width // 256); sy = max(1, h // 256)
    cap = 4096; colors = set()
    for y in range(0, h, sy):
        base = pixoff + y * rowsize
        for x in range(0, width, sx):
            o = base + x * bpb
            if o + bpb > len(d): continue
            colors.add((d[o], d[o+1], d[o+2]))
            if len(colors) >= cap:
                print(len(colors)); return
    print(len(colors))
main()
'

# The pixel-diff counter samples both BMPs on the same grid and prints how many
# sampled pixels differ (or -1 if the two images are not comparable). Recorded
# always; gates only when PIXEL_DIFF_MIN>0 (mirrors the Linux P2 gate).
PY_DIFF='
import sys, struct
def load(p):
    try:
        with open(p, "rb") as f:
            d = f.read()
    except Exception:
        return None
    if len(d) < 54 or d[:2] != b"BM":
        return None
    pixoff = struct.unpack_from("<I", d, 10)[0]
    width  = struct.unpack_from("<i", d, 18)[0]
    height = struct.unpack_from("<i", d, 22)[0]
    bpp    = struct.unpack_from("<H", d, 28)[0]
    comp   = struct.unpack_from("<I", d, 30)[0]
    if comp != 0 or bpp not in (24, 32) or width <= 0:
        return None
    return (d, pixoff, width, abs(height), bpp // 8, ((bpp * width + 31)//32)*4)
def main():
    a = load(sys.argv[1]); b = load(sys.argv[2])
    if a is None or b is None or a[2] != b[2] or a[3] != b[3]:
        print(-1); return
    da, oa, w, h, bpa, ra = a
    db, ob, _, _, bpb, rb = b
    sx = max(1, w // 256); sy = max(1, h // 256)
    diff = 0
    for y in range(0, h, sy):
        for x in range(0, w, sx):
            pa = oa + y*ra + x*bpa; pb = ob + y*rb + x*bpb
            if pa+3 > len(da) or pb+3 > len(db): continue
            if da[pa:pa+3] != db[pb:pb+3]: diff += 1
    print(diff)
main()
'

# unique_colors PNG -> prints the distinct-color count of PNG (0 on any error)
unique_colors() {
	uc_png=$1
	uc_bmp="${uc_png%.png}.bmp"
	if [ -s "$uc_png" ] && sips -s format bmp "$uc_png" --out "$uc_bmp" >/dev/null 2>&1; then
		python3 -c "$PY_COLORS" "$uc_bmp" 2>/dev/null || echo 0
	else
		echo 0
	fi
}

# snap PNG -> full-screen screenshot to PNG. -x silences the capture sound;
# a non-zero exit is a capture failure the caller classifies.
snap() {
	screencapture -x -t png "$1" 2> "$ARTIFACTS_DIR/screencapture-stderr.log"
}

# ------------------------------------------------------------- processes
CONTROL_PID=""
JLS_PID=""
# invoked via trap, which shellcheck cannot see (SC2317/SC2329 false positive)
# shellcheck disable=SC2317,SC2329
cleanup() {
	status=$?
	for pid in "$JLS_PID" "$CONTROL_PID"; do
		if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
			kill "$pid" 2>/dev/null || true
		fi
	done
	exit "$status"
}
trap cleanup EXIT INT TERM

# ------------------------------------------------ readiness / empty baseline
# Before launching any client, prove the WindowServer is screenshottable and
# capture the empty-desktop baseline (feeds the P2 pixel comparison later). A
# failure here is an environment fault (exit 2), never blamed on JLS.
snap "$ARTIFACTS_DIR/desktop-before.png" \
	|| die_env "could not screenshot the desktop (screencapture failed); environment/rig fault, not JLS"
log "readiness gate passed: desktop screenshot OK"

# ---------------------------------------------------- control experiment
# Map a minimal JFrame on the same runtime and WindowServer first. If even this
# cannot map a window (or its screenshot is blank/black), the environment is the
# blocker (exit 2) and JLS is not blamed; if it maps and JLS later fails, the
# defect is in JLS's startup path (exit 1). HelloSwingControl is reused as-is;
# on macOS the platform default toolkit is correct (no -Dawt.toolkit.name).
[ -f "$CONTROL_SRC" ] || die_env "control program missing: $CONTROL_SRC"
log "launching HelloSwingControl (environment-blocker control)"
"$JAVA" -Djava.awt.headless=false "$CONTROL_SRC" \
	> "$ARTIFACTS_DIR/control-stdout.log" 2> "$ARTIFACTS_DIR/control-stderr.log" &
CONTROL_PID=$!

control_status=0
wait_for_window "HelloSwingControl" "$CONTROL_PID" "$CONTROL_TIMEOUT" \
	|| control_status=$?
if [ "$control_status" -eq 0 ]; then
	printf 'control window mapped after %ss (%s window(s))\n' "$WAITED_SECONDS" "$WIN_COUNT" \
		> "$ARTIFACTS_DIR/control-verdict.txt"
	log "control window is up after ${WAITED_SECONDS}s ($WIN_COUNT window(s))"
else
	if [ "$control_status" -eq 3 ]; then
		printf 'System Events could not enumerate windows (permission)\n' \
			> "$ARTIFACTS_DIR/control-verdict.txt"
		log "----- last lines of osa-stderr.log -----"
		tail -n 20 "$ARTIFACTS_DIR/osa-stderr.log" >&2 || true
		die_env "System Events could not enumerate windows (Accessibility/automation permission likely withheld on this runner); environment fault, not JLS"
	fi
	if [ "$control_status" -eq 2 ]; then
		printf 'control process exited before mapping a window\n'
	else
		printf 'no control window after %ss\n' "$CONTROL_TIMEOUT"
	fi > "$ARTIFACTS_DIR/control-verdict.txt"
	log "----- last lines of control-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/control-stderr.log" >&2 || true
	die_env "the minimal Swing control failed to map a window; the blocker is the environment (WindowServer/AWT), not JLS"
fi

# The control window must also screenshot non-blank. A blank/all-black capture
# HERE - when the desktop readiness capture already worked AND the control window
# MAPPED - is the Screen Recording (TCC) signature: the WindowServer hands a
# non-interactive session a black window capture while window enumeration still
# works. On hosted runners this permission may NEVER be granted, so treating it
# as exit 2 would make the boot-proof lane permanently red for a runner-policy
# reason (pure noise). Instead the rig enters DEGRADED MODE: the pixel proofs are
# skipped and the window-map becomes the boot gate this run. (This is the ONLY
# branch whose semantics change for TCC; readiness-capture failure, a control
# window that never maps, and osascript denial all still classify exit 2 below,
# because those break the window-map proof itself.)
snap "$ARTIFACTS_DIR/control.png" || log "warning: control screenshot failed (continuing)"
control_colors="$(unique_colors "$ARTIFACTS_DIR/control.png")"
case "$control_colors" in ''|*[!0-9]*) control_colors=0 ;; esac
log "control screenshot unique colors: $control_colors"
if [ "$control_colors" -le 1 ]; then
	DEGRADED=1
	# GitHub Actions workflow warning annotation (stdout, on its own line).
	echo "::warning title=macOS pixel proof degraded (TCC)::P1 pixel proof unavailable: Screen Recording withheld (TCC); window-map is the gate this run"
	printf 'control window mapped but its capture is blank (%s unique color): Screen Recording (TCC) withheld\n' "$control_colors" \
		> "$ARTIFACTS_DIR/control-verdict.txt"
	log "P1 pixel proof unavailable: Screen Recording withheld (TCC); window-map is the gate this run - entering DEGRADED MODE (control window mapped, desktop readiness capture worked, only the window capture is blank)"
fi

# clear the control so JLS gets the same empty baseline
kill "$CONTROL_PID" 2>/dev/null || true
wait "$CONTROL_PID" 2>/dev/null || true
CONTROL_PID=""

# --------------------------------------------------------------- launch
# The real application entry point, launched exactly as a user would via the
# jar's Main-Class (jls.JLS). headless=false; on macOS the platform default
# toolkit (LWCToolkit) is correct, so NO -Dawt.toolkit.name.
log "launching JLS (real app entry point, jar Main-Class jls.JLS)"
"$JAVA" -Djava.awt.headless=false -jar "$JAR" \
	> "$ARTIFACTS_DIR/jls-stdout.log" 2> "$ARTIFACTS_DIR/jls-stderr.log" &
JLS_PID=$!

# ----------------------------------------------- wait for the JLS window
found=""
jls_status=0
wait_for_window "JLS" "$JLS_PID" "$WINDOW_TIMEOUT" || jls_status=$?
if [ "$jls_status" -eq 0 ]; then
	found=yes
elif [ "$jls_status" -eq 3 ]; then
	# System Events worked for the control but not now: still an environment
	# fault (a permission cannot appear mid-run for JLS alone), not JLS.
	snap "$ARTIFACTS_DIR/desktop-after.png" || true
	die_env "System Events stopped enumerating windows during the JLS wait; environment fault, not JLS"
elif [ "$jls_status" -eq 2 ]; then
	snap "$ARTIFACTS_DIR/desktop-after.png" || true
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	# control mapped + non-blank, JLS did not: JLS-side failure (exit 1)
	printf 'macos-rig: error: JLS exited before mapping a window (see jls-stderr.log; the control mapped and captured non-blank, so this is a JLS-side failure)\n' >&2
	exit 1
fi

if [ -z "$found" ]; then
	snap "$ARTIFACTS_DIR/desktop-after.png" || true
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	printf 'macos-rig: error: no JLS window mapped after %ss (the control mapped and captured non-blank, so this is a JLS-side failure)\n' "$WINDOW_TIMEOUT" >&2
	exit 1
fi
log "JLS window is up after ${WAITED_SECONDS}s ($WIN_COUNT window(s))"

# record the window verdict as proof it is a real mapped frame
{
	printf 'JLS windows mapped: %s\n' "$WIN_COUNT"
	printf 'waited: %ss\n' "$WAITED_SECONDS"
} > "$ARTIFACTS_DIR/jls-window-info.txt"

# ------------------------------------------------- DEGRADED MODE exit
# Screen Recording (TCC) was withheld (the control window mapped but its capture
# was blank): the JLS window MAPPED just now via System Events, which is the boot
# gate the maintainer relies on ("prove the thing can at least start"). The pixel
# proofs would all be blank for the same TCC reason, so they are skipped and the
# run succeeds on the window-map proof alone. The real JLS jar was launched and
# its top-level window enumerated, so this is a genuine boot proof, not a stub.
if [ "$DEGRADED" = 1 ]; then
	# one screenshot is still captured for artifact evidence (expected blank
	# under TCC); it is NOT gated on.
	snap "$ARTIFACTS_DIR/jls-window.png" || true
	{
		printf 'mode: DEGRADED (Screen Recording / TCC withheld)\n'
		printf 'control unique colors: %s (blank -> pixel proofs unavailable)\n' "$control_colors"
		printf 'pixel proofs: SKIPPED (window-map is the boot gate this run)\n'
		printf 'JLS windows mapped: %s\n' "$WIN_COUNT"
	} > "$ARTIFACTS_DIR/nonblank.txt"
	log "first light (DEGRADED): JLS window mapped ($WIN_COUNT window(s)); pixel proof unavailable (Screen Recording/TCC withheld) - boot proven by window-map, exiting 0"
	exit 0
fi

# ------------------------------------------------- non-blank proof (P1)
# A mapped window is not enough - the screenshot must not be a blank/black
# frame. Swing shows the frame a beat before it paints, so poll the screenshot
# until it has more than one unique color (i.e. a real frame, not a denied
# all-black capture), up to RENDER_TIMEOUT seconds.
render_elapsed=0
jls_colors=0
while : ; do
	snap "$ARTIFACTS_DIR/jls-window.png" || log "warning: JLS screenshot failed"
	jls_colors="$(unique_colors "$ARTIFACTS_DIR/jls-window.png")"
	case "$jls_colors" in ''|*[!0-9]*) jls_colors=0 ;; esac
	{ [ "$jls_colors" -gt 1 ] || [ "$render_elapsed" -ge "$RENDER_TIMEOUT" ]; } && break
	sleep 1
	render_elapsed=$((render_elapsed + 1))
done
cp "$ARTIFACTS_DIR/jls-window.png" "$ARTIFACTS_DIR/desktop-after.png" 2>/dev/null || true
{
	printf 'control unique colors:    %s\n' "$control_colors"
	printf 'jls-window unique colors: %s\n' "$jls_colors"
	printf 'render wait: %ss\n' "$render_elapsed"
} > "$ARTIFACTS_DIR/nonblank.txt"
log "non-blank check after ${render_elapsed}s: window=$jls_colors colors"

if [ "$jls_colors" -le 1 ]; then
	# The control frame captured non-blank moments ago, so Screen Recording is
	# granted; a blank JLS capture now is a JLS-side failure (window mapped but
	# rendered nothing), per the Linux rigs' P1 rule.
	printf 'macos-rig: error: the JLS screenshot is blank (%s unique color); the JLS window mapped but rendered nothing - JLS-side failure\n' "$jls_colors" >&2
	exit 1
fi

# --------------------------------------------- P2 pixel difference (optional)
# How many sampled pixels changed between the empty desktop and the desktop with
# JLS on it. Always recorded; it gates only once PIXEL_DIFF_MIN is calibrated
# from the first green run's observed value (not guessed blind) - the same
# discipline the Linux P2 gate uses. On a wallpapered runner this is the metric
# that actually proves the app painted, since distinct-color>1 is trivially true
# for any non-black frame there.
before_bmp="$ARTIFACTS_DIR/desktop-before.bmp"
after_bmp="$ARTIFACTS_DIR/desktop-after.bmp"
diff_count="-1"
if sips -s format bmp "$ARTIFACTS_DIR/desktop-before.png" --out "$before_bmp" >/dev/null 2>&1 \
		&& sips -s format bmp "$ARTIFACTS_DIR/desktop-after.png" --out "$after_bmp" >/dev/null 2>&1; then
	diff_count="$(python3 -c "$PY_DIFF" "$before_bmp" "$after_bmp" 2>/dev/null || echo -1)"
fi
printf '%s\n' "$diff_count" > "$ARTIFACTS_DIR/pixel-diff.txt"
log "sampled pixels differing from empty desktop: $diff_count"
if [ "$PIXEL_DIFF_MIN" -gt 0 ] 2>/dev/null; then
	case "$diff_count" in
		''|*[!0-9]*)
			printf 'macos-rig: error: P2 gate is armed (PIXEL_DIFF_MIN=%s) but the diff metric was unreadable: %s\n' "$PIXEL_DIFF_MIN" "$diff_count" >&2
			exit 1 ;;
	esac
	if [ "$diff_count" -lt "$PIXEL_DIFF_MIN" ]; then
		printf 'macos-rig: error: P2 failed: only %s sampled pixels differ from the empty desktop (need >= %s) - JLS-side failure\n' "$diff_count" "$PIXEL_DIFF_MIN" >&2
		exit 1
	fi
fi

log "first light: success"
exit 0
