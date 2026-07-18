#!/bin/bash
# Provision the Java 25 build baseline for Claude Code on the web
# (issue #92: the pom enforces JDK [25,) via requireJavaVersion, but
# the web session container ships JDK 21). Installs the Ubuntu
# openjdk-25 packages once — the container state is cached after the
# hook completes — and points JAVA_HOME at them for the session.
# Local (non-web) sessions are left untouched.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

JVM_DIR=/usr/lib/jvm/java-25-openjdk-amd64
if [ ! -f "$JVM_DIR/lib/libawt_xawt.so" ]; then
  # the image's package lists can be stale (404s on point releases);
  # refresh best-effort — third-party PPA signature failures must not
  # abort the JDK install
  sudo apt-get update -qq || true
  # the full (non-headless) JDK: the display-tagged UI suite needs
  # libawt_xawt; xvfb + a font let it actually run (issue #162):
  #   xvfb-run -a mvn -B test -Djls.test.headless=false
  sudo apt-get install -y -qq openjdk-25-jdk xvfb fonts-dejavu-core
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export JAVA_HOME=$JVM_DIR"
    echo "export PATH=$JVM_DIR/bin:\$PATH"
  } >> "$CLAUDE_ENV_FILE"
fi
