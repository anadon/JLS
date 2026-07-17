# Development container for JLS.
#
# None of this is needed to build or run JLS itself — `mvn verify` on any
# JDK 25+ machine is enough (see README "Building from source"). This image
# adds the optional development tools described in the README section
# "Optional development tools": circuit-file inspection, HDL validation
# (roadmap issues #33/#59), and display tooling for GUI work.
#
# Project policy: X11 must not be a dependency. This image contains no X
# server, no XWayland, and no X11 utilities; on-display work goes through
# a headless Wayland compositor instead (see README).
#
#   docker build -f .devcontainer/Dockerfile -t jls-dev .
#   docker run --rm -it -v "$PWD":/workspace jls-dev
#
# VS Code / GitHub Codespaces pick it up automatically via
# .devcontainer/devcontainer.json.

# Maven + Temurin JDK 25 (the required CI baseline, current LTS — #92).
FROM maven:3-eclipse-temurin-26@sha256:d5617b9a6307e1b51dc7c55edf09bacb66f1c91fb861949c34a3a0d4e16bd241

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        # version control and downloads (the maven image ships neither)
        git \
        curl \
        ca-certificates \
        # .jls circuit files are XZ data (current) or zip archives (legacy);
        # these let you unpack and repack them by hand
        xz-utils \
        zip \
        unzip \
        # Wayland-only display stack: a wlroots compositor that runs
        # headless (WLR_BACKENDS=headless WLR_LIBINPUT_NO_DEVICES=1
        # WLR_RENDERER=pixman sway), screenshots (grim), synthetic
        # keyboard input (wtype), display diagnostics (wayland-info)
        sway \
        grim \
        wtype \
        wayland-utils \
        # text rendering needs at least one installed font — including
        # fully headless batch image export (-i)
        fontconfig \
        fonts-dejavu-core \
        # inspect/compare the JPEGs written by batch image export (-i)
        # and screenshots taken with grim
        imagemagick \
        # HDL toolchain for the export/import roadmap (#33, #59):
        # compile+simulate Verilog, analyze VHDL, synthesize to JSON netlists
        iverilog \
        ghdl \
        yosys \
    && rm -rf /var/lib/apt/lists/*

# Stock OpenJDK's Swing toolkit on Linux speaks X11 only, so with the
# Temurin JDK above the interactive GUI cannot display on the Wayland
# compositor (headless work — the test suite, batch mode, image export —
# is unaffected). JetBrains Runtime ships OpenJDK's experimental Wayland
# toolkit (Project Wakefield). To bake it in, pass its tarball URL:
#
#   docker build -f .devcontainer/Dockerfile -t jls-dev \
#     --build-arg JBR_URL=https://cache-redirector.jetbrains.com/intellij-jbr/jbr-25.0.3-linux-x64-b508.16.tar.gz .
#
# and run the GUI on the compositor with:
#
#   /opt/jbr/bin/java -Dawt.toolkit.name=WLToolkit -jar target/jls-*.jar
ARG JBR_URL=
RUN if [ -n "$JBR_URL" ]; then \
        mkdir -p /opt/jbr \
        && curl -fsSL "$JBR_URL" | tar -xz -C /opt/jbr --strip-components=1 \
        && /opt/jbr/bin/java -version; \
    fi

WORKDIR /workspace
