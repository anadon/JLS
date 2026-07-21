#!/usr/bin/env bash
# Compile (if needed) and run a GuiDriver-family main under the Xvfb display.
# Usage: run.sh <MainClass> [args...]
set -u
cd /home/user/JLS
export DISPLAY=:99
SCR=/tmp/claude-0/-home-user-JLS/5629c65d-470b-5c3b-a567-4f9019babece/scratchpad
JAR=target/jls-5.0.5-SNAPSHOT.jar
MAIN="${1:?main class}"; shift || true
pkill -9 -f 'jls-5.0.5' 2>/dev/null
pkill -9 -f 'GuiDriver\|CpuBuilder\|RiscvBuild\|WireDebug\|MiniAdder\|Probe' 2>/dev/null
rm -f /home/user/JLS/JLSerror
sleep 1
timeout 240 java -Djava.awt.headless=false \
  -cp "$JAR:riscv/gui/out" "$MAIN" "$@" >"$SCR/run.out" 2>"$SCR/run.err"
code=$?
echo "EXIT=$code"
echo "----- stdout -----"
cat "$SCR/run.out"
echo "----- stderr (filtered) -----"
grep -v 'Picked up JAVA_TOOL' "$SCR/run.err" | tail -40
exit 0
