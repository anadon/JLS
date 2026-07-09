# JLS — Java Logic Simulator

JLS is an educational digital logic circuit editor and simulator. Students
draw circuits from gates, wires, registers, memories, state machines, and
other elements, then simulate them interactively (with a signal-trace window)
or in batch mode from the command line. It was written by David A. Poplawski
(Michigan Technological University); this repository is a maintained fork of
his JLS 4.1.

## Running JLS

JLS is a desktop Swing application and needs a Java runtime (JDK/JRE 17 or
newer).

- **From a release:** download `jls-<version>.jar` from the
  [Releases page](https://github.com/anadon/JLS/releases) and run:

  ```sh
  java -jar jls-<version>.jar
  ```

  The jar is self-contained — no other files are needed.

- **Command-line options:** `java -jar jls-<version>.jar -h` prints the full
  list, including batch mode (`-b`), test-input files (`-t`), simulation time
  limits (`-d`), image export (`-i`), and printing (`-p`/`-v`/`-r`).
  Diagnostics go to stderr as one `jls: error: ...` line; exit status is
  0 on success, 1 on runtime failure, and 2 on a usage error.

## Building from source

The build uses Maven and JDK 17+:

```sh
mvn verify          # compile (warnings are errors), run tests, SpotBugs
java -jar target/jls-*.jar
```

Sources live in the historical `src/` layout (not `src/main/java`); tests are
under `test/`. Continuous integration builds every push on JDK 17 and 21.
Pushing a `v*` tag publishes a GitHub Release with the runnable jar.

### Optional development tools

Nothing beyond Maven and a JDK is required to build, test, or run JLS.
The following tools are useful when working *on* JLS, and are what the
development container image (below) installs:

- **xz-utils, zip, unzip** — unpack and repack `.jls` circuit files by hand.
  Current saves are XZ data (`xzcat circuit.jls`); legacy saves are zip
  archives with a single `JLSCircuit` entry (see "Circuit files" below).
- **sway, grim, wtype, wayland-utils** — a Wayland compositor that can run
  on a machine with no display or GPU
  (`WLR_BACKENDS=headless WLR_LIBINPUT_NO_DEVICES=1 WLR_RENDERER=pixman sway`),
  plus screenshots (`grim`), synthetic keyboard input (`wtype`), and display
  diagnostics (`wayland-info`). X11 is deliberately not part of this
  project's tooling: no X server, no XWayland, no X11 utilities.
- **fontconfig, fonts-dejavu-core** — text rendering needs at least one
  installed font (even fully headless batch image export); minimal container
  images often have none.
- **ImageMagick** — inspect and compare the JPEG images written by batch
  image export (`-i`) and screenshots taken with `grim`.
- **Icarus Verilog (`iverilog`), GHDL, Yosys** — external HDL toolchain for
  the HDL export/import roadmap
  ([docs/hdl-support-research.md](docs/hdl-support-research.md), issues #33
  and #59): compile and simulate exported Verilog, analyze exported VHDL,
  and synthesize Verilog to the JSON netlists used for import.

All of these are stock packages on Debian/Ubuntu
(`apt install xz-utils zip unzip sway grim wtype wayland-utils fontconfig
fonts-dejavu-core imagemagick iverilog ghdl yosys`).

**Displaying the GUI without X11.** The Maven test suite and batch mode run
fully headless (`java.awt.headless=true`) and need no display server of any
kind. The interactive GUI is another matter: stock OpenJDK's Swing toolkit
on Linux only speaks X11, so on the Wayland compositor above it needs a JDK
that includes OpenJDK's experimental Wayland toolkit (Project Wakefield) —
currently shipped by [JetBrains
Runtime](https://github.com/JetBrains/JetBrainsRuntime) — enabled with:

```sh
java -Dawt.toolkit.name=WLToolkit -jar target/jls-*.jar
```

The development container below accepts a `JBR_URL` build argument to bake
that runtime in.

### Development container

[`.devcontainer/Dockerfile`](.devcontainer/Dockerfile) builds an image with
Maven, Temurin JDK 21, and all of the optional tools above — and no X11
components. VS Code and GitHub Codespaces pick it up automatically via
[`.devcontainer/devcontainer.json`](.devcontainer/devcontainer.json); to use
it directly:

```sh
docker build -f .devcontainer/Dockerfile -t jls-dev .
docker run --rm -it -v "$PWD":/workspace jls-dev
```

To run the GUI inside the container (on the headless Wayland compositor),
build with `--build-arg JBR_URL=<JetBrains Runtime linux-x64 tar.gz URL>`
and use `/opt/jbr/bin/java -Dawt.toolkit.name=WLToolkit` as shown in the
Dockerfile's comments.

## Circuit files

JLS saves circuits as `.jls` files. **The extension has meant different
container formats over time**, and the loader accepts all of them by
sniffing the actual content:

- **XZ-compressed text** — what current versions of JLS write when saving.
  Despite the plain `.jls` name, these files are XZ data (they start with the
  `ý7zXZ` magic bytes) wrapping a line-oriented text description of the
  circuit.
- **Zip archive** — the original JLS format: a zip containing a single
  `JLSCircuit` entry with the same text description.
- **Plain text** — the uncompressed circuit description itself.

Editor checkpoint files (`.jls~`) are used for crash recovery. They are
written in the same XZ format as regular saves (older versions wrote them
as zip archives; the loader still accepts those). If you process `.jls`
files with external tools, sniff the content rather than trusting the
extension.

## License and provenance

JLS is free software under the **GNU General Public License v3.0** (see
[LICENSE](LICENSE)). The original author, David A. Poplawski, released the
JLS source under GPLv3; his signed grant is preserved in this repository as
[pop_GPLv3.pdf](pop_GPLv3.pdf).

## Contributing

Issues and pull requests are welcome at
[github.com/anadon/JLS](https://github.com/anadon/JLS). The open issues
include a maintenance and modernization roadmap (see the tracking issue) if
you are looking for somewhere to start. Changes should keep `mvn verify`
green — the build treats compiler warnings as errors and runs the test suite
and SpotBugs.
