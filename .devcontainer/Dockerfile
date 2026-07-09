# Development container for JLS.
#
# None of this is needed to build or run JLS itself — `mvn verify` on any
# JDK 17+ machine is enough (see README "Building from source"). This image
# adds the optional development tools described in the README section
# "Optional development tools", so that circuit-file inspection, headless
# GUI work, and HDL validation (roadmap issues #33/#59) all work out of the
# box:
#
#   docker build -f .devcontainer/Dockerfile -t jls-dev .
#   docker run --rm -it -v "$PWD":/workspace jls-dev
#
# VS Code / GitHub Codespaces pick it up automatically via
# .devcontainer/devcontainer.json.

# Maven + Temurin JDK 21 (the newer of the two JDKs CI builds on).
FROM maven:3.9-eclipse-temurin-21

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        # version control (the maven image does not ship it)
        git \
        ca-certificates \
        # .jls circuit files are XZ data (current) or zip archives (legacy);
        # these let you unpack and repack them by hand
        xz-utils \
        zip \
        unzip \
        # virtual X server so the Swing GUI can run in the container:
        # xvfb-run java -jar target/jls-*.jar
        xvfb \
        xauth \
        # xdpyinfo/xwd for checking the display and taking screenshots
        x11-utils \
        # scripted keyboard/mouse input for driving the GUI under Xvfb
        xdotool \
        # Swing text rendering needs at least one font installed
        fontconfig \
        fonts-dejavu-core \
        # inspect/compare the JPEGs written by batch image export (-i)
        # and screenshots taken under Xvfb
        imagemagick \
        # HDL toolchain for the export/import roadmap (#33, #59):
        # compile+simulate Verilog, analyze VHDL, synthesize to JSON netlists
        iverilog \
        ghdl \
        yosys \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
