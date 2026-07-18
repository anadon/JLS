# JLS batch-mode container image (headless), for autograders and CI that
# want `docker run` instead of a local Java runtime.  GUI use is out of
# scope — no display stack is included.  Built and published to
# ghcr.io/anadon/jls by the release workflow's container-image job via
# scripts/build-container.sh, which stages the shaded jar next to this
# file (the build context contains exactly jls.jar and this Dockerfile).
#
#   docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests circuit.jls
#
# Multi-arch: linux/amd64, linux/arm64, linux/riscv64.  Both stages use
# ubuntu:26.04 + the archive's OpenJDK 25 because it is the one
# JDK-25-bearing base published for all three architectures
# (eclipse-temurin and bellsoft/liberica images have no riscv64), and a
# single FROM keeps the jlink runtime and the final stage on the same
# glibc on every platform.
#
# Multi-stage: jdeps derives the module set from the shaded jar and jlink
# trims a runtime from it (the same recipe as scripts/build-installer.sh),
# so the final image carries ~50 MB of JVM instead of a full JDK.

# Reproducibility (issue #184): the base image is digest-pinned, and apt
# installs come from the Ubuntu snapshot service (snapshot.ubuntu.com) at
# a fixed archive state instead of the moving mirror head, so package
# versions cannot float between two builds of the same commit.  apt's
# vendor config maps archive.ubuntu.com/security/ports to the snapshot
# host, covering amd64 and the arm64/riscv64 ports alike.  Bump
# APT_SNAPSHOT deliberately (with the base-image digest) to take security
# updates; the date mirrors the pom's project.build.outputTimestamp pin.
# SOURCE_DATE_EPOCH is supplied by scripts/build-container.sh from the
# HEAD commit time; BuildKit clamps image-config and history timestamps
# to it (layer-tarball mtimes additionally need the push path's
# rewrite-timestamp=true output option).
ARG APT_SNAPSHOT=20260716T000000Z
ARG SOURCE_DATE_EPOCH

# binutils: the archive's JDK carries unstripped native libraries, so
# jlink --strip-debug shells out to objcopy (Temurin's jmods come
# pre-stripped, which is why the old Temurin build stage never needed it)
FROM ubuntu:26.04@sha256:3131b4cc82a783df6c9df078f86e01819a13594b865c2cad47bd1bca2b7063bb AS runtime
ARG APT_SNAPSHOT
RUN echo "APT::Snapshot \"${APT_SNAPSHOT}\";" > /etc/apt/apt.conf.d/50snapshot \
    && apt-get -o Acquire::Retries=3 update \
    && apt-get -o Acquire::Retries=3 install -y --no-install-recommends \
        openjdk-25-jdk-headless binutils \
    && rm -rf /var/lib/apt/lists/*
COPY jls.jar /build/jls.jar
RUN set -eux; \
    MODULES="$(jdeps --print-module-deps --ignore-missing-deps /build/jls.jar)"; \
    jlink --add-modules "$MODULES" --strip-debug --no-header-files \
          --no-man-pages --compress zip-6 --output /build/runtime

# fontconfig + DejaVu let batch image export (-i) render text headlessly
# (Acquire::Retries: the arm64/riscv64 ports mirror drops connections
# often enough under QEMU-length builds that one retry pass is not enough)
FROM ubuntu:26.04@sha256:3131b4cc82a783df6c9df078f86e01819a13594b865c2cad47bd1bca2b7063bb
ARG APT_SNAPSHOT
# the snapshot pin is dropped after installing so derived images and
# debugging shells get the normal moving archive, not a frozen one
RUN echo "APT::Snapshot \"${APT_SNAPSHOT}\";" > /etc/apt/apt.conf.d/50snapshot \
    && apt-get -o Acquire::Retries=3 update \
    && apt-get -o Acquire::Retries=3 install -y --no-install-recommends \
        fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* /etc/apt/apt.conf.d/50snapshot

# the source label links the ghcr package to the repository
LABEL org.opencontainers.image.source="https://github.com/anadon/JLS" \
      org.opencontainers.image.description="JLS batch mode (headless): test vectors, VCD waveform export, circuit image export, HDL export" \
      org.opencontainers.image.licenses="GPL-3.0-only"

COPY --from=runtime /build/runtime /opt/jls/runtime
COPY jls.jar /opt/jls/jls.jar

# batch runs read and write files relative to the caller's mount
WORKDIR /work
ENTRYPOINT ["/opt/jls/runtime/bin/java", "-Djava.awt.headless=true", "-jar", "/opt/jls/jls.jar"]
CMD ["-h"]
