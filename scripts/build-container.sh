#!/usr/bin/env bash
# Build the JLS batch-mode container image (headless; see
# resources/packaging/Dockerfile).  One recipe, used both locally and by
# .github/workflows/release.yml, so the two cannot drift:
#
#   shaded jar (mvn package)
#     -> stage jls.jar + Dockerfile into a minimal build context
#     -> docker build   (jdeps/jlink run inside the Dockerfile)
#
# Environment:
#   JLS_SKIP_BUILD=1   reuse an existing target/jls-*.jar instead of
#                      running Maven (CI builds/tests in a prior step)
#   IMAGE              image name (default ghcr.io/anadon/jls)
#   DOCKER             container tool (default docker; podman works)
#   PLATFORMS          comma-separated buildx platforms (e.g.
#                      linux/amd64,linux/arm64,linux/riscv64) — switches
#                      to `docker buildx build`; needs QEMU binfmt for
#                      non-native entries
#   PUSH=1             with PLATFORMS: push the multi-arch manifest
#                      (buildx cannot --load a multi-arch result, so
#                      without PUSH=1 the build only proves the
#                      platforms compile — nothing lands in `docker
#                      image ls`)
#
# Tags <version> always, and `latest` only for stable releases (no
# pre-release/SNAPSHOT suffix).  Pushing is the workflow's job, not ours.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DOCKER="${DOCKER:-docker}"
IMAGE="${IMAGE:-ghcr.io/anadon/jls}"

VERSION="$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)"
echo "==> version ${VERSION}"

if [ "${JLS_SKIP_BUILD:-0}" != "1" ]; then
	echo "==> mvn package"
	mvn -B -q package
fi
JAR="$(ls target/jls-*.jar)"
echo "==> jar: ${JAR}"

# minimal build context: exactly the jar and the Dockerfile
STAGE="target/container"
rm -rf "$STAGE"
mkdir -p "$STAGE"
cp "$JAR" "$STAGE/jls.jar"
cp resources/packaging/Dockerfile "$STAGE/Dockerfile"

tags=(-t "$IMAGE:$VERSION")
if [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	tags+=(-t "$IMAGE:latest")
fi

if [ -n "${PLATFORMS:-}" ]; then
	push_flag=()
	if [ "${PUSH:-0}" = "1" ]; then
		push_flag=(--push)
	else
		echo "==> PLATFORMS without PUSH=1: build-only, results stay in the buildx cache"
	fi
	echo "==> $DOCKER buildx build --platform ${PLATFORMS}"
	"$DOCKER" buildx build --platform "$PLATFORMS" "${tags[@]}" "${push_flag[@]}" "$STAGE"
else
	echo "==> $DOCKER build"
	"$DOCKER" build "${tags[@]}" "$STAGE"

	echo "==> built:"
	"$DOCKER" image ls "$IMAGE"
fi
