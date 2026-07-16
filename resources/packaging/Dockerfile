# JLS batch-mode container image (headless), for autograders and CI that
# want `docker run` instead of a local Java runtime.  GUI use is out of
# scope — no display stack is included.  Built and published to
# ghcr.io/anadon/jls by the release workflow's container-image job via
# scripts/build-container.sh, which stages the shaded jar next to this
# file (the build context contains exactly jls.jar and this Dockerfile).
#
#   docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests circuit.jls
#
# Multi-stage: jdeps derives the module set from the shaded jar and jlink
# trims a runtime from it (the same recipe as scripts/build-installer.sh),
# so the final image carries ~50 MB of JVM instead of a full JDK.

FROM eclipse-temurin:25 AS runtime
COPY jls.jar /build/jls.jar
RUN set -eux; \
    MODULES="$(jdeps --print-module-deps --ignore-missing-deps /build/jls.jar)"; \
    jlink --add-modules "$MODULES" --strip-debug --no-header-files \
          --no-man-pages --compress zip-6 --output /build/runtime

# ubuntu:24.04 is the same base eclipse-temurin:25 builds on, so the
# jlink runtime meets the glibc it was linked against; fontconfig +
# DejaVu let batch image export (-i) render text fully headlessly
FROM ubuntu:24.04
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

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
