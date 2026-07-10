#!/usr/bin/env bash
# Build a self-contained native installer for the 2015-era JLS 4.1.5 with
# jpackage -- the "legacy" archival release.
#
# JLS 4.1.5 predates the Maven build, the module system, and the jpackage
# recipe in scripts/build-installer.sh.  Its sources live only in history
# (the last pre-modernization commit, tagged v4.1.5), it is an Eclipse
# project compiled against a vendored JavaHelp jar (lib/jhall.jar) and an
# in-tree copy of the XZ codec (xz/), and its entry point is jls.JLS.
#
# This script reconstructs a runnable, self-contained artifact from that
# historical tree and wraps it in the same kind of installer the modern
# build produces, so the classic version is downloadable as a native app
# for every desktop OS:
#
#   git archive v4.1.5      recover the 2015 source tree from history
#     -> javac              compile src/ + xz/ (the applet is excluded:
#                           it is the only user of the removed Applet API
#                           and nothing else references it)
#     -> jar                assemble one self-contained jar: app classes,
#                           the JavaHelp runtime, and the bundled help set,
#                           Main-Class jls.JLS
#     -> jlink              trim a runtime image (java.base, java.desktop,
#                           java.xml -- java.xml is reached by the startup
#                           plugin scan in jls.JLS)
#     -> jpackage           deb/rpm (Linux), msi (Windows), dmg (macOS),
#                           reusing the modern packaging resources (icons,
#                           .jls file association) at the repository root
#
# The installer metadata deliberately matches the modern build (name JLS,
# vendor, bundle id), so the archival build is simply an older *version*
# (4.1.5) of the same application rather than a separate program.
#
# Requirements: JDK 21+ on PATH (jdeps/jlink/jpackage ship with the JDK),
# a full git clone (this script reads history), and per platform:
#   Linux:   dpkg-deb for --type deb; rpmbuild (optional) enables --type rpm
#   Windows: WiX Toolset for --type msi (preinstalled on GitHub runners)
#   macOS:   nothing extra for --type dmg
#
# Outputs land in target/installer-legacy/dist/.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Prefer the JDK named by JAVA_HOME so local runs and CI agree on the
# toolchain that both builds the runtime image and gets bundled into it.
if [ -n "${JAVA_HOME:-}" ]; then
	export PATH="$JAVA_HOME/bin:$PATH"
fi

# The 2015 source tree, pinned by commit so the build is reproducible no
# matter which ref triggered it (this SHA is the tag v4.1.5).
LEGACY_REF="${LEGACY_REF:-ed14ecde824a232de60059e49ea8c9562492328f}"

# --- installer metadata (no pom at 4.1.5; single-sourced here) --------------
NAME="JLS"
APP_VERSION="4.1.5"
VENDOR="io.github.anadon"
ABOUT_URL="https://github.com/anadon/JLS"
DESCRIPTION="JLS 4.1.5, an educational digital logic circuit editor and simulator, originally by David A. Poplawski (Michigan Technological University). This is an archival build of the last pre-modernization (2015) release."

echo "==> toolchain"
java -version
jpackage --version
echo "==> legacy source ref: ${LEGACY_REF} (JLS ${APP_VERSION})"

STAGE="target/installer-legacy"
SRC="$STAGE/src"
CLASSES="$STAGE/classes"
JARDIR="$STAGE/jar"
CONTENT="$STAGE/content"
RUNTIME="$STAGE/runtime"
INPUT="$STAGE/input"
DIST="$STAGE/dist"
rm -rf "$STAGE"
mkdir -p "$SRC" "$CLASSES" "$JARDIR" "$CONTENT" "$INPUT" "$DIST"

# --- recover the 2015 source tree from history ------------------------------
echo "==> git archive ${LEGACY_REF}"
git archive "$LEGACY_REF" | tar -x -C "$SRC"

# --- compile src/ + xz/, excluding the applet -------------------------------
# JLSApplet is the sole user of the Applet API (removed in current JDKs) and
# is referenced by nothing else, so the desktop app is complete without it.
echo "==> javac"
find "$SRC/src" "$SRC/xz" -name '*.java' ! -name 'JLSApplet.java' > "$STAGE/sources.txt"
echo "==> compiling $(wc -l < "$STAGE/sources.txt") sources"
javac -encoding UTF-8 -nowarn -Xlint:none \
	-cp "$SRC/lib/jhall.jar" \
	-d "$CLASSES" \
	@"$STAGE/sources.txt"

# --- assemble one self-contained jar ----------------------------------------
# Layer order: JavaHelp runtime first, then app classes, then the help set.
echo "==> assemble self-contained jar"
# JavaHelp runtime, minus its manifest and signatures (a merged jar must not
# carry another jar's signature files); META-INF/services is preserved.
( cd "$CONTENT" && jar xf "$ROOT/$SRC/lib/jhall.jar" )
rm -f "$CONTENT/META-INF/MANIFEST.MF" \
      "$CONTENT"/META-INF/*.SF "$CONTENT"/META-INF/*.RSA "$CONTENT"/META-INF/*.DSA 2>/dev/null || true
# application (and XZ) classes
cp -r "$CLASSES/." "$CONTENT/"
# bundled help set (loaded from the classpath as help/JLSHelp.hs), minus the
# CVS metadata and Windows Explorer droppings the 2015 tree still carried
cp -r "$SRC/resources/." "$CONTENT/"
find "$CONTENT" -type d -name CVS -prune -exec rm -rf {} + 2>/dev/null || true
find "$CONTENT" -name 'Thumbs.db' -delete 2>/dev/null || true

JAR="$JARDIR/jls-${APP_VERSION}.jar"
printf 'Manifest-Version: 1.0\nMain-Class: jls.JLS\nImplementation-Title: JLS\nImplementation-Version: %s\n' "$APP_VERSION" > "$STAGE/manifest.txt"
( cd "$CONTENT" && jar cfm "$ROOT/$JAR" "$ROOT/$STAGE/manifest.txt" . )
cp "$JAR" "$INPUT/"
echo "==> jar: ${JAR} ($(du -h "$JAR" | cut -f1))"

# --- trimmed runtime via jlink ----------------------------------------------
# Explicit module set: the JavaHelp jar drags in many optional deps (JSP tag
# libraries, the JDIC browser) that JLS never uses, which makes
# `jdeps --print-module-deps` unreliable here; the app itself needs only
# java.desktop (Swing/AWT) and java.xml (the startup plugin scan in jls.JLS).
MODULES="java.base,java.desktop,java.xml"
echo "==> jlink modules: ${MODULES}"
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
		--name "$NAME" \
		--app-version "$APP_VERSION" \
		--input "$INPUT" \
		--main-jar "$(basename "$JAR")" \
		--runtime-image "$RUNTIME" \
		--description "$DESCRIPTION" \
		--vendor "$VENDOR" \
		--about-url "$ABOUT_URL" \
		--license-file "$SRC/LICENSE" \
		--dest "$DIST" \
		"$@"
}

case "$(uname -s)" in
	Linux)
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
		;;
	Darwin)
		package dmg \
			--icon resources/packaging/jls.icns \
			--file-associations resources/packaging/jls-association-macos.properties \
			--mac-package-identifier io.github.anadon.jls
		;;
	MINGW* | MSYS* | CYGWIN*)
		package msi \
			--icon resources/packaging/jls.ico \
			--file-associations resources/packaging/jls-association-windows.properties \
			--win-menu \
			--win-shortcut \
			--win-per-user-install
		;;
	*)
		echo "unsupported platform: $(uname -s)" >&2
		exit 1
		;;
esac

echo "==> installers in ${DIST}:"
ls -lh "$DIST"
