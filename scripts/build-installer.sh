#!/usr/bin/env bash
# Build a self-contained JLS installer with jpackage (issue #82).
#
# One recipe, used both locally and by .github/workflows/release.yml, so the
# two cannot drift:
#
#   shaded jar (mvn package)
#     -> jdeps --print-module-deps       derive the minimal module set
#     -> jlink                           trim a runtime image
#     -> jpackage --type <per OS>        deb/rpm + AppImage (Linux), msi
#                                        (Windows), dmg (macOS), with a .jls
#                                        file association and desktop metadata
#
# Requirements: JDK 17+ on PATH (jdeps/jlink/jpackage ship with the JDK;
# jpackage is final since JDK 16, JEP 392), Maven, and per platform:
#   Linux:   dpkg-deb for --type deb; rpmbuild (optional) enables --type rpm
#   Windows: WiX Toolset for --type msi (preinstalled on GitHub runners)
#   macOS:   nothing extra for --type dmg
#
# Environment:
#   JLS_SKIP_BUILD=1   reuse an existing target/jls-*.jar instead of
#                      running Maven (CI builds/tests in a prior step)
#   APPIMAGETOOL=path  use this appimagetool instead of downloading the
#                      pinned one (Linux x86_64 only)
#
# Outputs land in target/installer/dist/.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Prefer the JDK named by JAVA_HOME so local runs and CI agree on the
# toolchain that both builds the runtime image and gets bundled into it.
if [ -n "${JAVA_HOME:-}" ]; then
	export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==> toolchain"
java -version
jpackage --version

# --- version and metadata, single-sourced from the pom (#36) ---------------
VERSION="$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)"
ABOUT_URL="$(mvn -B -q help:evaluate -Dexpression=project.url -DforceStdout)"
# pom description spans lines; collapse whitespace for installer metadata
DESCRIPTION="$(mvn -B -q help:evaluate -Dexpression=project.description -DforceStdout \
	| tr '\n' ' ' | tr -s ' ' | sed 's/^ //; s/ $//')"
VENDOR="$(mvn -B -q help:evaluate -Dexpression=project.groupId -DforceStdout)"

# jpackage --app-version must be numeric on Windows (MSI ProductVersion) and
# is used as the deb/rpm version on Linux: strip any pre-release suffix
# (e.g. 5.0.0-rc.1 -> 5.0.0).  The release workflow's tag guard already
# refuses SNAPSHOT releases; local snapshot builds just get the base triple.
APP_VERSION="$(printf '%s' "$VERSION" | sed -E 's/^([0-9]+(\.[0-9]+){0,2}).*$/\1/')"

# normalized machine architecture for artifact names (macOS says arm64
# where Linux says aarch64); deb/rpm carry their own arch fields, but
# msi/dmg/AppImage names need it to keep multi-arch releases collision-free.
# On Windows-on-ARM the git-bash uname is an emulated x64 binary and
# reports x86_64, so the runner's own RUNNER_ARCH wins when present.
ARCH="${RUNNER_ARCH:-$(uname -m)}"
case "$ARCH" in
	ARM64 | arm64) ARCH=aarch64 ;;
	X64 | amd64) ARCH=x86_64 ;;
esac

echo "==> version ${VERSION} (installer version ${APP_VERSION}, arch ${ARCH})"

# --- shaded jar -------------------------------------------------------------
if [ "${JLS_SKIP_BUILD:-0}" != "1" ]; then
	echo "==> mvn package"
	mvn -B -q package
fi
JAR="$(ls target/jls-*.jar)"
echo "==> jar: ${JAR}"

# --- module set via jdeps ---------------------------------------------------
# Derived from the shaded jar, not hardcoded: currently
# java.base,java.desktop (no java.xml — the help TOC is hand-parsed, and
# there is no java.util.prefs usage).
MODULES="$(jdeps --print-module-deps --ignore-missing-deps "$JAR")"
echo "==> modules: ${MODULES}"

# --- trimmed runtime via jlink ----------------------------------------------
STAGE="target/installer"
RUNTIME="$STAGE/runtime"
INPUT="$STAGE/input"
DIST="$STAGE/dist"
rm -rf "$STAGE"
mkdir -p "$INPUT" "$DIST"
cp "$JAR" "$INPUT/"

echo "==> jlink"
jlink \
	--add-modules "$MODULES" \
	--strip-debug \
	--no-header-files \
	--no-man-pages \
	--compress zip-6 \
	--output "$RUNTIME"
echo "==> runtime size: $(du -sh "$RUNTIME" | cut -f1)"

# --- jpackage ---------------------------------------------------------------
package() {
	local type="$1"
	shift
	echo "==> jpackage --type ${type}"
	jpackage \
		--type "$type" \
		--name JLS \
		--app-version "$APP_VERSION" \
		--input "$INPUT" \
		--main-jar "$(basename "$JAR")" \
		--runtime-image "$RUNTIME" \
		--description "$DESCRIPTION" \
		--vendor "$VENDOR" \
		--about-url "$ABOUT_URL" \
		--license-file LICENSE \
		--dest "$DIST" \
		"$@"
}

# --- AppImage (Linux x86_64 / aarch64) --------------------------------------
# jpackage --type app-image lays out launcher + bundled runtime; appimagetool
# folds that tree into one self-mounting executable that runs on any distro
# (including ones the deb/rpm cannot target).  appimagetool itself is pinned
# by version and sha256 per architecture, matching the release pipeline's
# supply-chain posture.
APPIMAGETOOL_VERSION="1.9.1"
case "$ARCH" in
	x86_64) APPIMAGETOOL_SHA256="ed4ce84f0d9caff66f50bcca6ff6f35aae54ce8135408b3fa33abfc3cb384eb0" ;;
	aarch64) APPIMAGETOOL_SHA256="f0837e7448a0c1e4e650a93bb3e85802546e60654ef287576f46c71c126a9158" ;;
	*) APPIMAGETOOL_SHA256="" ;;
esac
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/${APPIMAGETOOL_VERSION}/appimagetool-${ARCH}.AppImage"

build_appimage() {
	# app-image is the raw launcher tree: installer-only flags such as
	# --license-file or --file-associations are rejected here, so this
	# does not go through package()
	echo "==> jpackage --type app-image"
	jpackage \
		--type app-image \
		--name JLS \
		--app-version "$APP_VERSION" \
		--input "$INPUT" \
		--main-jar "$(basename "$JAR")" \
		--runtime-image "$RUNTIME" \
		--icon resources/packaging/jls.png \
		--dest "$STAGE/appimage"

	# AppDir contract: an AppRun entry point, exactly one top-level
	# .desktop, and a top-level icon.  The jpackage launcher resolves its
	# app directory through /proc/self/exe, so a symlinked AppRun works.
	local appdir="$STAGE/appimage/JLS"
	ln -s bin/JLS "$appdir/AppRun"
	cp resources/packaging/jls.png "$appdir/jls.png"
	ln -s jls.png "$appdir/.DirIcon"
	cat > "$appdir/JLS.desktop" <<-EOF
		[Desktop Entry]
		Type=Application
		Name=JLS
		Comment=Educational digital logic circuit editor and simulator
		Exec=JLS %f
		Icon=jls
		Categories=Education;Electronics;
		MimeType=application/x-jls-circuit;
		Terminal=false
	EOF

	local tool="${APPIMAGETOOL:-}"
	if [ -z "$tool" ]; then
		tool="$STAGE/appimagetool"
		echo "==> fetching appimagetool ${APPIMAGETOOL_VERSION} (${ARCH})"
		curl -fsSL -o "$tool" "$APPIMAGETOOL_URL"
		echo "${APPIMAGETOOL_SHA256}  ${tool}" | sha256sum -c -
		chmod +x "$tool"
	fi
	# --appimage-extract-and-run: appimagetool is itself an AppImage and
	# would otherwise need FUSE, absent on CI runners and NixOS
	echo "==> appimagetool"
	ARCH="$ARCH" "$tool" --appimage-extract-and-run \
		"$appdir" "$DIST/JLS-${VERSION}-${ARCH}.AppImage"
}

case "$(uname -s)" in
	Linux)
		# --resource-dir overrides the generated .desktop entry: the JDK
		# default Exec line has no %f field code, so double-clicked .jls
		# files would never reach argv (see resource-dir-linux/JLS.desktop)
		linux_flags=(
			--icon resources/packaging/jls.png
			--file-associations resources/packaging/jls-association-linux.properties
			--resource-dir resources/packaging/resource-dir-linux
			--linux-shortcut
			--linux-menu-group "Education;Electronics;"
		)
		package deb "${linux_flags[@]}"
		if command -v rpmbuild >/dev/null 2>&1; then
			package rpm "${linux_flags[@]}"
		else
			echo "==> rpmbuild not found; skipping --type rpm"
		fi
		if [ -n "$APPIMAGETOOL_SHA256" ]; then
			build_appimage
		else
			echo "==> no appimagetool pin for ${ARCH}; skipping AppImage"
		fi
		;;
	Darwin)
		# --mac-package-identifier keeps the bundle id stable across releases
		package dmg \
			--icon resources/packaging/jls.icns \
			--file-associations resources/packaging/jls-association-macos.properties \
			--mac-package-identifier io.github.anadon.jls
		# jpackage names the dmg JLS-<version>.dmg with no architecture;
		# suffix it so Apple-silicon and Intel builds cannot collide
		mv "$DIST/JLS-${APP_VERSION}.dmg" "$DIST/JLS-${APP_VERSION}-${ARCH}.dmg"
		;;
	MINGW* | MSYS* | CYGWIN*)
		# Per-user install: no admin rights needed (student/lab machines);
		# WiX Toolset must be on PATH for --type msi.
		package msi \
			--icon resources/packaging/jls.ico \
			--file-associations resources/packaging/jls-association-windows.properties \
			--win-menu \
			--win-shortcut \
			--win-per-user-install
		# same collision-proofing as the dmg
		mv "$DIST/JLS-${APP_VERSION}.msi" "$DIST/JLS-${APP_VERSION}-${ARCH}.msi"
		;;
	*)
		echo "unsupported platform: $(uname -s)" >&2
		exit 1
		;;
esac

echo "==> installers in ${DIST}:"
ls -lh "$DIST"
