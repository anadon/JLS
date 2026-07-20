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
#   JLS_JBR_HOME=path  Linux only: bundle this JetBrains Runtime root
#                      (must contain bin/java) instead of downloading
#                      the pinned one — see the JBR block below
#   SOURCE_DATE_EPOCH  override the build timestamp (seconds since the
#                      epoch); defaults to the pom's
#                      project.build.outputTimestamp so installers and
#                      jar share one source-derived clock (#44, #188)
#   JLS_SIGN_KEY=fpr   GPG fingerprint of the release signing key (#136,
#                      Linux only): the rpm gets an embedded rpmsign
#                      signature and the AppImage an embedded
#                      appimagetool --sign signature, so `rpm -K` and
#                      AppImage validators verify natively.  The key must
#                      sign unattended (the release workflow presets the
#                      passphrase in an ephemeral gpg-agent); unset —
#                      the normal case for local builds — everything
#                      still builds, just unsigned.
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

# --- reproducibility plumbing (#188) ----------------------------------------
# Byte-reproducibility needs every timestamp the packagers see to be derived
# from the source, not the build wall clock.  Single source of truth is the
# pom's project.build.outputTimestamp — the same stamp that already makes the
# jar reproducible (#44) — exported as SOURCE_DATE_EPOCH, the cross-tool
# convention (https://reproducible-builds.org/specs/source-date-epoch/) that
# dpkg-deb, rpmbuild, mksquashfs (inside appimagetool) and friends honor.
# A caller-supplied SOURCE_DATE_EPOCH wins, so a release lane can pin the
# tag's commit date instead.
if [ -z "${SOURCE_DATE_EPOCH:-}" ]; then
	POM_STAMP="$(mvn -B -q help:evaluate \
		-Dexpression=project.build.outputTimestamp -DforceStdout)"
	case "$POM_STAMP" in
		# Maven also accepts a raw epoch-seconds value for the property
		'' | null)
			echo "project.build.outputTimestamp missing from pom" >&2
			exit 1
			;;
		*[!0-9]*)
			# ISO-8601 (e.g. 2026-07-16T00:00:00Z): GNU date parses it
			# with -d; BSD date (macOS) needs -j -f with the layout
			SOURCE_DATE_EPOCH="$(date -u -d "$POM_STAMP" +%s 2>/dev/null \
				|| date -u -j -f '%Y-%m-%dT%H:%M:%SZ' "$POM_STAMP" +%s)"
			;;
		*)
			SOURCE_DATE_EPOCH="$POM_STAMP"
			;;
	esac
fi
export SOURCE_DATE_EPOCH
echo "==> SOURCE_DATE_EPOCH ${SOURCE_DATE_EPOCH}"

# touch -t is the portable clamp (GNU and BSD both take it; -d @epoch is
# GNU-only), so render the epoch once in touch's CCYYMMDDhhmm.SS format
CLAMP_STAMP="$(date -u -d "@${SOURCE_DATE_EPOCH}" +%Y%m%d%H%M.%S 2>/dev/null \
	|| date -u -r "${SOURCE_DATE_EPOCH}" +%Y%m%d%H%M.%S)"

# Clamp every mtime under the given trees to SOURCE_DATE_EPOCH.  The staged
# input/, runtime/, and app-image trees are (re)created at build time, so
# without this each build hands the native packagers fresh wall-clock mtimes
# — the archive members of deb/rpm/AppImage would differ every run.  -h
# clamps symlinks themselves (AppRun, .DirIcon) instead of chasing targets.
clamp_mtimes() {
	find "$@" -exec touch -h -t "$CLAMP_STAMP" {} +
}

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

# jlink writes wall-clock mtimes (and the copied jar keeps its own); clamp
# the whole staged tree before any packager sees it (#188)
clamp_mtimes "$INPUT" "$RUNTIME"

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

	# the AppDir is assembled fresh each build (jpackage copy + the files
	# above): clamp it so mksquashfs inside appimagetool — which honors
	# SOURCE_DATE_EPOCH for the filesystem timestamp — sees stable mtimes
	clamp_mtimes "$appdir"

	local tool="${APPIMAGETOOL:-}"
	if [ -z "$tool" ]; then
		tool="$STAGE/appimagetool"
		echo "==> fetching appimagetool ${APPIMAGETOOL_VERSION} (${ARCH})"
		curl -fsSL -o "$tool" "$APPIMAGETOOL_URL"
		echo "${APPIMAGETOOL_SHA256}  ${tool}" | sha256sum -c -
		chmod +x "$tool"
	fi
	# Embedded GPG signature (#136): appimagetool fills the runtime's
	# reserved .sha256_sig/.sig_key ELF sections, which AppImage
	# validators check and the launcher never reads — gated on
	# JLS_SIGN_KEY so local builds without the release key keep working.
	local sign_flags=()
	if [ -n "${JLS_SIGN_KEY:-}" ]; then
		sign_flags=(--sign --sign-key "$JLS_SIGN_KEY")
	fi

	# --appimage-extract-and-run: appimagetool is itself an AppImage and
	# would otherwise need FUSE, absent on CI runners and NixOS
	echo "==> appimagetool"
	ARCH="$ARCH" "$tool" --appimage-extract-and-run \
		"${sign_flags[@]}" \
		"$appdir" "$DIST/JLS-${VERSION}-${ARCH}.AppImage"
}

# --- Linux bundled runtime: JetBrains Runtime (Wayland) ---------------------
# Adjudicated on issue #82 (2026-07-17, from the #100 handoff): the Linux
# installers must bundle a runtime that carries Swing's Wayland toolkit
# (WLToolkit, Project Wakefield), which today only JetBrains Runtime ships
# — mainline OpenJDK does not.  JBR is a JetBrains OpenJDK fork under
# GPLv2 + Classpath Exception, so redistribution is permitted.  Revisit if
# mainline OpenJDK gains Wayland support.
#
# Same pin-and-placeholder convention as ci.yml's gui-wayland lane (issue
# #101): the sha256 could not be computed from the authoring environment
# (cache-redirector.jetbrains.com is proxy-blocked there), so until a real
# checksum is pinned the build falls back LOUDLY to the build JDK's jlink
# image above (X11/XWayland only) rather than fetch an unverified archive.
# To arm the JBR path, run
#   curl -fsSL <JBR_URL> | sha256sum
# from an unproxied machine and replace the placeholder for each arch.
#
# The jbrsdk flavor is pinned (not jbr) because it ships jmods, which lets
# jlink trim the bundled image to the jdeps-derived module set;
# select_linux_runtime copes with either layout regardless.
JBR_VERSION="25.0.3"
JBR_BUILD="b508.16"
case "$ARCH" in
	x86_64) JBR_PLATFORM="linux-x64" ;;
	aarch64) JBR_PLATFORM="linux-aarch64" ;;
	*) JBR_PLATFORM="" ;;
esac
JBR_URL="https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-${JBR_VERSION}-${JBR_PLATFORM}-${JBR_BUILD}.tar.gz"
case "$JBR_PLATFORM" in
	linux-x64) JBR_SHA256="UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101" ;;
	linux-aarch64) JBR_SHA256="UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101" ;;
	*) JBR_SHA256="" ;;
esac

# Repoint $RUNTIME at a JBR-derived image when a JBR is available: an
# explicit JLS_JBR_HOME wins; otherwise a pinned-and-verified download.
# With jmods present the image is jlink-trimmed to $MODULES (using the
# JBR's own jlink so linker and modules agree); a runtime-only JBR is
# bundled whole.  Without any JBR the already-built build-JDK image
# ships, with a warning that Wayland-only sessions will need XWayland.
select_linux_runtime() {
	local jbr=""
	if [ -n "${JLS_JBR_HOME:-}" ]; then
		if [ ! -x "$JLS_JBR_HOME/bin/java" ]; then
			echo "JLS_JBR_HOME=$JLS_JBR_HOME has no executable bin/java" >&2
			exit 1
		fi
		jbr="$JLS_JBR_HOME"
	elif [ -n "$JBR_SHA256" ] && [ "${JBR_SHA256#UNVERIFIED-}" = "$JBR_SHA256" ]; then
		echo "==> fetching JetBrains Runtime ${JBR_VERSION}${JBR_BUILD} (${JBR_PLATFORM})"
		curl -fsSL -o "$STAGE/jbr.tar.gz" "$JBR_URL"
		echo "${JBR_SHA256}  ${STAGE}/jbr.tar.gz" | sha256sum -c -
		mkdir -p "$STAGE/jbr"
		tar -xzf "$STAGE/jbr.tar.gz" -C "$STAGE/jbr" --strip-components=1
		jbr="$STAGE/jbr"
	else
		echo "==> WARNING: no JetBrains Runtime available (the JBR sha256 pin is a placeholder — see issue #101);" >&2
		echo "==>          bundling the build JDK's runtime instead: Wayland-only sessions will need XWayland" >&2
		return 0
	fi
	"$jbr/bin/java" -version
	if [ -d "$jbr/jmods" ]; then
		echo "==> jlink (JetBrains Runtime)"
		rm -rf "$STAGE/jbr-runtime"
		"$jbr/bin/jlink" \
			--module-path "$jbr/jmods" \
			--add-modules "$MODULES" \
			--strip-debug \
			--no-header-files \
			--no-man-pages \
			--compress zip-6 \
			--output "$STAGE/jbr-runtime"
		RUNTIME="$STAGE/jbr-runtime"
	else
		RUNTIME="$jbr"
	fi
	echo "==> Linux runtime: JBR image at ${RUNTIME} ($(du -sh "$RUNTIME" | cut -f1))"
}

case "$(uname -s)" in
	Linux)
		select_linux_runtime
		# --resource-dir overrides three of jpackage's generated deb
		# templates (JLS.desktop, postinst, prerm): the JDK default
		# .desktop Exec line has no %f field code, so double-clicked
		# .jls files would never reach argv (resource-dir-linux/JLS.desktop);
		# and the default postinst/prerm hard-fail the whole
		# install/removal under xdg-desktop-menu on any host without
		# desktop-menu infrastructure (a plain server, or CI), aborting
		# before the .jls mime association -- the actual point of this
		# flag block -- ever registers (resource-dir-linux/postinst,
		# .../prerm)
		linux_flags=(
			--icon resources/packaging/jls.png
			--file-associations resources/packaging/jls-association-linux.properties
			--resource-dir resources/packaging/resource-dir-linux
			--linux-shortcut
			--linux-menu-group "Education;Electronics;"
		)
		# jpackage restages input/ and runtime/ into its own build tree;
		# clamping here plus dpkg-deb/rpmbuild's own SOURCE_DATE_EPOCH
		# clamping pins every packaged mtime (#189)
		clamp_mtimes "$INPUT" "$RUNTIME"
		package deb "${linux_flags[@]}"
		if command -v rpmbuild >/dev/null 2>&1; then
			# jpackage offers no rpmbuild passthrough, and rpm stamps
			# the build host and build time into the header: stage an
			# .rpmmacros that pins BUILDHOST and derives BUILDTIME plus
			# payload mtimes from SOURCE_DATE_EPOCH, and point HOME at
			# it for just this invocation (#189)
			mkdir -p "$STAGE/rpm-home"
			printf '%s\n' \
				'%_buildhost reproducible' \
				'%use_source_date_epoch_as_buildtime 1' \
				'%clamp_mtime_to_source_date_epoch 1' \
				> "$STAGE/rpm-home/.rpmmacros"
			( export HOME="$STAGE/rpm-home"; package rpm "${linux_flags[@]}" )
			# Embedded GPG signature (#136).  rpmsign mutates the file
			# in place, which is why the workflow signs before its
			# attestation and checksum steps (both run after this
			# script).  Signing was asked for, so a missing rpmsign is
			# an error, not a skip — never ship unsigned by surprise.
			if [ -n "${JLS_SIGN_KEY:-}" ]; then
				echo "==> rpmsign"
				rpmsign --define "_gpg_name $JLS_SIGN_KEY" \
					--addsign "$DIST"/jls-*.rpm
			fi
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
		# Determinism (#191): hdiutil embeds a per-build random UDIF
		# SegmentID plus HFS+ volume dates/UUID that jpackage exposes no
		# control over, so two builds of one commit differ.  Route A of
		# docs/dmg-reproducibility.md — normalize-dmg.py rewrites exactly
		# the checksum-safe volatile set.  As with the msi lane its
		# self-test runs first, so a broken tool can never touch the dmg,
		# and any structural surprise is a hard error, not a silent skip.
		# Escape hatch: JLS_SKIP_DMG_NORMALIZE=1.
		DMG="$DIST/JLS-${APP_VERSION}.dmg"
		if [ "${JLS_SKIP_DMG_NORMALIZE:-0}" != "1" ]; then
			PYTHON="$(command -v python3 || command -v python || true)"
			if [ -z "$PYTHON" ]; then
				echo "error: python is required to normalize the dmg (#191);" >&2
				echo "       set JLS_SKIP_DMG_NORMALIZE=1 to build a non-deterministic dmg" >&2
				exit 1
			fi
			"$PYTHON" scripts/normalize-dmg.py --self-test
			SDE="${SOURCE_DATE_EPOCH:-$(git log -1 --pretty=%ct 2>/dev/null || echo 0)}"
			# Full HFS+ envelope pass (F1/F2/F3/F5): opt-in, because the
			# read-write round-trip below is not yet verified on a real
			# Mac (docs/dmg-reproducibility.md §4/§5) — it stays behind a
			# flag so a maintainer validates it on macOS before it enters
			# the default release path.  It patches the *uncompressed* UDRW
			# image before re-compressing, the only checksum-safe window
			# for the volume header.
			if [ "${JLS_DMG_FULL_NORMALIZE:-0}" = "1" ]; then
				echo "==> dmg full-normalize (HFS+ envelope, opt-in)"
				WORK="$STAGE/dmg-work"
				rm -rf "$WORK"
				mkdir -p "$WORK"
				# preserve the SLA (license) resource across the round-trip
				hdiutil udifderez -xml "$DMG" > "$WORK/sla.plist"
				hdiutil convert "$DMG" -quiet -format UDRW -o "$WORK/rw.dmg"
				# clamp node dates and drop the volume's fseventsd UUID
				# inside the mounted image, before re-imaging
				printf 'Y\n' | hdiutil attach "$WORK/rw.dmg" -quiet -nobrowse \
					-noautoopen -owners off -mountpoint "$WORK/mnt"
				rm -rf "$WORK/mnt/.fseventsd" 2>/dev/null || true
				find "$WORK/mnt" -exec touch -h -t "$CLAMP_STAMP" {} + 2>/dev/null || true
				hdiutil detach "$WORK/mnt" -quiet || hdiutil detach "$WORK/mnt" -quiet -force
				# pin volume-header dates + UUID on the raw read-write image
				"$PYTHON" scripts/normalize-dmg.py hfs \
					--source-date-epoch "$SDE" "$WORK/rw.dmg"
				# re-compress (level pinned, not inherited) and restore the SLA
				hdiutil convert "$WORK/rw.dmg" -quiet -format UDZO \
					-imagekey zlib-level=9 -o "$WORK/out.dmg"
				if [ -s "$WORK/sla.plist" ]; then
					hdiutil udifrez "$WORK/out.dmg" -xml "$WORK/sla.plist" || true
				fi
				mv "$WORK/out.dmg" "$DMG"
			fi
			# Always pin the UDIF SegmentID on the final compressed image
			# (E1): it lives outside every UDIF checksum, so this is safe on
			# the shipped dmg with no round-trip and no risk to the license
			# resource.
			"$PYTHON" scripts/normalize-dmg.py koly "$DMG"
		fi
		# jpackage names the dmg JLS-<version>.dmg with no architecture;
		# suffix it so Apple-silicon and Intel builds cannot collide
		mv "$DMG" "$DIST/JLS-${APP_VERSION}-${ARCH}.dmg"
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
		# Determinism (#190): WiX embeds a per-build random package code
		# GUID plus build timestamps that jpackage exposes no control
		# over, so two builds of one commit differ.  normalize-msi.py
		# rewrites exactly that volatile set in place (content-derived
		# package code, SOURCE_DATE_EPOCH-clamped timestamps); its
		# self-test runs first so a broken tool can never touch the msi,
		# and any structural surprise is a hard error, not a silent
		# skip.  See docs/windows-msi-determinism.md; escape hatch:
		# JLS_SKIP_MSI_NORMALIZE=1.
		if [ "${JLS_SKIP_MSI_NORMALIZE:-0}" != "1" ]; then
			PYTHON="$(command -v python3 || command -v python || true)"
			if [ -z "$PYTHON" ]; then
				echo "error: python is required to normalize the msi (#190);" >&2
				echo "       set JLS_SKIP_MSI_NORMALIZE=1 to build a non-deterministic msi" >&2
				exit 1
			fi
			"$PYTHON" scripts/normalize-msi.py --self-test
			# clamp to the commit date until #188's shared
			# SOURCE_DATE_EPOCH export lands and takes precedence
			SDE="${SOURCE_DATE_EPOCH:-$(git log -1 --pretty=%ct 2>/dev/null || echo 0)}"
			"$PYTHON" scripts/normalize-msi.py \
				--source-date-epoch "$SDE" "$DIST/JLS-${APP_VERSION}.msi"
		fi
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
