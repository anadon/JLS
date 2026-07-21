#!/usr/bin/env bash
# Compile (if needed) and run a GuiDriver-family main under a headless display.
# Usage: run.sh <MainClass> [args...]
#   DISPLAY      - X display to drive (default :99)
#   JLS_GUI_OUT  - directory for generated .jls / screenshots (default: a temp dir)
set -u
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$ROOT"
export DISPLAY=${DISPLAY:-:99}
OUT=${JLS_GUI_OUT:-$(mktemp -d)}
export JLS_GUI_OUT="$OUT"
JAR=$(ls target/jls-*.jar 2>/dev/null | grep -v original | head -1)
if [ -z "$JAR" ]; then echo "build the jar first: mvn -q package -DskipTests" >&2; exit 1; fi
MAIN="${1:?main class}"; shift || true
pkill -9 -f 'jls-.*\.jar' 2>/dev/null
pkill -9 -f "riscv/gui/out $MAIN" 2>/dev/null
rm -f "$ROOT/JLSerror"
sleep 1
timeout 240 java -Djava.awt.headless=false \
  -cp "$JAR:riscv/gui/out" "$MAIN" "$@" >"$OUT/run.out" 2>"$OUT/run.err"
code=$?
echo "EXIT=$code (artifacts in $OUT)"
echo "----- stdout -----"
cat "$OUT/run.out"
echo "----- stderr (filtered) -----"
grep -v 'Picked up JAVA_TOOL' "$OUT/run.err" | tail -40
exit 0
