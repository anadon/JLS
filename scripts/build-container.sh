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
#   SOURCE_DATE_EPOCH  override the timestamp stamped into the image
#                      config/history/layers; defaults to the HEAD
#                      commit time (issue #184)
#
# Tags <version> always, and `latest` only for stable releases (no
# pre-release/SNAPSHOT suffix).  Pushing is the workflow's job, not ours.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DOCKER="${DOCKER:-docker}"
IMAGE="${IMAGE:-ghcr.io/anadon/jls}"

# Tie image timestamps to the source revision, not build wall-clock
# (issue #184).  BuildKit clamps image-config and history timestamps to
# the SOURCE_DATE_EPOCH build arg; on the push path the
# rewrite-timestamp=true output option below additionally rewrites
# layer-tarball mtimes to it.  Together with the Dockerfile's apt
# snapshot pin this ties the JLS-controlled layers to the commit; the
# digest-pinned base image layers were already fixed.
if [ -z "${SOURCE_DATE_EPOCH:-}" ]; then
	SOURCE_DATE_EPOCH="$(git log -1 --pretty=%ct)"
fi
export SOURCE_DATE_EPOCH
build_args=(--build-arg "SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH}")
echo "==> SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH}"

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
		# --output type=image,push=true is the long form of --push;
		# rewrite-timestamp=true makes BuildKit rewrite layer-tarball
		# mtimes to SOURCE_DATE_EPOCH so pushed layers are
		# revision-stamped, not wall-clock-stamped (#184)
		push_flag=(--output "type=image,push=true,rewrite-timestamp=true")
	else
		echo "==> PLATFORMS without PUSH=1: build-only, results stay in the buildx cache"
	fi
	echo "==> $DOCKER buildx build --platform ${PLATFORMS}"
	# --metadata-file: surfaces the manifest-list digest; on PUSH=1 the
	# workflow signs and attests by digest (tags are mutable, #133)
	"$DOCKER" buildx build --platform "$PLATFORMS" "${tags[@]}" "${push_flag[@]}" \
		"${build_args[@]}" --metadata-file "$STAGE/metadata.json" "$STAGE"
	if [ "${PUSH:-0}" = "1" ]; then
		DIGEST="$(sed -n 's/.*"containerimage.digest": *"\([^"]*\)".*/\1/p' "$STAGE/metadata.json" | head -1)"
		echo "==> pushed digest: ${DIGEST}"
		printf '%s' "$DIGEST" > "$STAGE/digest"
	fi
else
	echo "==> $DOCKER build"
	"$DOCKER" build "${tags[@]}" "${build_args[@]}" "$STAGE"

	echo "==> built:"
	"$DOCKER" image ls "$IMAGE"
fi
