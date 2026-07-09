#!/usr/bin/env bash
# wayland-rig.sh - first-light Wayland GUI rig for JLS (issue #101).
#
# Boots the JLS GUI on a Wayland-capable Java runtime (JetBrains Runtime
# with OpenJDK's experimental Wakefield WLToolkit) against a *headless*
# sway compositor - no X11, no GPU, no physical display - then asserts
# that a window belonging to the JLS process actually appears in the
# compositor's tree, and captures the evidence:
#
#   $ARTIFACTS_DIR/sway.log            compositor log
#   $ARTIFACTS_DIR/jls-stdout.log      JLS stdout
#   $ARTIFACTS_DIR/jls-stderr.log      JLS stderr (the first-light census input)
#   $ARTIFACTS_DIR/tree.json           swaymsg -t get_tree at the end
#   $ARTIFACTS_DIR/desktop-before.png  screenshot before JLS launches
#   $ARTIFACTS_DIR/desktop-after.png   screenshot after the window appeared
#   $ARTIFACTS_DIR/pixel-diff.txt      ImageMagick AE metric (informational)
#
# Exit status: 0 when the JLS window appeared, nonzero otherwise.
#
# Requirements: sway, swaymsg, grim, jq; a JBR under $JBR_HOME; the shaded
# jar from `mvn -DskipTests package` (or point $JAR at one). ImageMagick's
# `compare` is optional (pixel diff is recorded, not gated - gating on it
# is #101 P2 and rides once the lane is stable).
#
# Environment knobs (all optional except JBR_HOME):
#   JBR_HOME       JetBrains Runtime root (must contain bin/java)  [required]
#   JAR            jar to launch          [newest target/jls-*.jar]
#   ARTIFACTS_DIR  where evidence goes    [artifacts/wayland-rig]
#   SWAY_TIMEOUT   seconds to wait for the compositor socket   [30]
#   WINDOW_TIMEOUT seconds to wait for the JLS window          [90]

set -euo pipefail

log() { printf 'wayland-rig: %s\n' "$*" >&2; }
die() { printf 'wayland-rig: error: %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------- config
ARTIFACTS_DIR="${ARTIFACTS_DIR:-artifacts/wayland-rig}"
SWAY_TIMEOUT="${SWAY_TIMEOUT:-30}"
WINDOW_TIMEOUT="${WINDOW_TIMEOUT:-90}"

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

# sway refuses to run without a writable XDG_RUNTIME_DIR it owns; CI
# runners often have none
if [ -z "${XDG_RUNTIME_DIR:-}" ] || [ ! -w "${XDG_RUNTIME_DIR:-/nonexistent}" ]; then
	XDG_RUNTIME_DIR="$(mktemp -d)"
	export XDG_RUNTIME_DIR
	log "XDG_RUNTIME_DIR not usable; using $XDG_RUNTIME_DIR"
fi
chmod 700 "$XDG_RUNTIME_DIR"

# a minimal config: the stock /etc/sway/config assumes wallpapers, idle
# helpers, and input devices that a headless CI runner does not have
SWAY_CONFIG="$ARTIFACTS_DIR/sway-config"
cat > "$SWAY_CONFIG" <<'EOF'
# minimal headless config for the JLS first-light rig (issue #101)
# (intentionally empty: defaults are fine, and nothing external is spawned)
EOF

# ------------------------------------------------------------- processes
SWAY_PID=""
JLS_PID=""
# invoked via trap, which shellcheck cannot see (SC2317 false positive)
# shellcheck disable=SC2317
cleanup() {
	status=$?
	# kill JLS first so sway does not log a client teardown as an error
	for pid in "$JLS_PID" "$SWAY_PID"; do
		if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
			kill "$pid" 2>/dev/null || true
		fi
	done
	# bounded reap; sway occasionally needs a moment to drop the socket
	for _ in 1 2 3 4 5; do
		{ [ -z "$SWAY_PID" ] || ! kill -0 "$SWAY_PID" 2>/dev/null; } && break
		sleep 1
	done
	exit "$status"
}
trap cleanup EXIT INT TERM

# start the compositor on wlroots' headless backend with the pixman
# software renderer - verified working in the dev container (issue #101)
log "starting headless sway"
env WLR_BACKENDS=headless WLR_LIBINPUT_NO_DEVICES=1 WLR_RENDERER=pixman \
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

# empty-desktop baseline for the P2 pixel comparison
grim "$ARTIFACTS_DIR/desktop-before.png" \
	|| log "warning: baseline screenshot failed (continuing)"

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
elapsed=0
while [ "$elapsed" -lt "$WINDOW_TIMEOUT" ]; do
	if ! kill -0 "$JLS_PID" 2>/dev/null; then
		swaymsg -t get_tree > "$ARTIFACTS_DIR/tree.json" 2>/dev/null || true
		log "----- last lines of jls-stderr.log -----"
		tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
		die "JLS exited before mapping a window (see jls-stderr.log)"
	fi
	if swaymsg -t get_tree 2>/dev/null \
			| jq -e --argjson pid "$JLS_PID" \
				'[.. | objects | select((.pid? == $pid)
					or ((.name? // "") | test("JLS")))] | length > 0' \
			> /dev/null; then
		found=yes
		break
	fi
	sleep 1
	elapsed=$((elapsed + 1))
done

# ------------------------------------------------------------- evidence
swaymsg -t get_tree > "$ARTIFACTS_DIR/tree.json" 2>/dev/null || true
grim "$ARTIFACTS_DIR/desktop-after.png" \
	|| log "warning: final screenshot failed"

if [ -z "$found" ]; then
	log "----- last lines of jls-stderr.log -----"
	tail -n 40 "$ARTIFACTS_DIR/jls-stderr.log" >&2 || true
	die "no JLS window in the compositor tree after ${WINDOW_TIMEOUT}s"
fi
log "JLS window is up after ${elapsed}s"

# informational pixel difference (P2 groundwork): how many pixels changed
# between the empty desktop and the desktop with JLS on it
if command -v compare > /dev/null 2>&1 \
		&& [ -f "$ARTIFACTS_DIR/desktop-before.png" ] \
		&& [ -f "$ARTIFACTS_DIR/desktop-after.png" ]; then
	# `compare -metric AE` prints the count to stderr and exits nonzero
	# when images differ - which is exactly what we hope for here
	diff_count="$(compare -metric AE "$ARTIFACTS_DIR/desktop-before.png" \
		"$ARTIFACTS_DIR/desktop-after.png" null: 2>&1 || true)"
	printf '%s\n' "$diff_count" > "$ARTIFACTS_DIR/pixel-diff.txt"
	log "pixels differing from empty desktop (AE): $diff_count"
fi

log "first light: success"
exit 0
