# JLS — Java Logic Simulator

[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/anadon/JLS/badge)](https://scorecard.dev/viewer/?uri=github.com/anadon/JLS)

JLS is an educational digital logic circuit editor and simulator. Students
draw circuits from gates, wires, registers, memories, state machines, and
other elements, then simulate them interactively (with a signal-trace window)
or in batch mode from the command line. It was written by David A. Poplawski
(Michigan Technological University); this repository is a maintained fork of
his JLS 4.1.

## Installing JLS

The [Releases page](https://github.com/anadon/JLS/releases) carries
self-contained installers with a bundled Java runtime — no JDK needed:

- **Linux:** `jls_<version>_<arch>.deb` (`sudo apt install ./jls_*.deb`) or
  `jls-<version>*.rpm`, for x86_64 (`amd64`) and ARM (`arm64`/`aarch64`).
  JLS appears in the applications menu.
- **Linux (any distro):** `JLS-<version>-x86_64.AppImage` or
  `JLS-<version>-aarch64.AppImage` — no installation: `chmod +x` it and
  run it (`--appimage-extract-and-run` if FUSE is absent). The `.jls`
  association comes along only if your desktop integrates AppImages
  (e.g. via AppImageLauncher).
- **NixOS / Nix:** the repository is a flake — `nix run github:anadon/JLS`
  runs it, `nix profile install github:anadon/JLS` installs the `jls`
  command with menu entry and `.jls` association. (The deb/rpm/AppImage
  assets do not fit NixOS; the flake builds from source instead.)
- **Windows:** `JLS-<version>-x86_64.msi`, or `JLS-<version>-aarch64.msi`
  for Windows on ARM (per-user install, no admin rights needed).
  The installers are Authenticode-signed through
  [SignPath.io](https://signpath.io)'s open-source program, so the
  publisher shown by Windows is **SignPath Foundation** — if an installer
  names any other publisher, or none (releases before signing was
  enabled), do not run it before verifying the download against
  `SHA256SUMS-installers-windows-<arch>`.
- **macOS:** `JLS-<version>-aarch64.dmg` (Apple silicon). The app is
  unsigned by choice, not oversight — signing requires paid Apple
  Developer Program enrollment, which this free university tool
  deliberately forgoes (#128, #135). Gatekeeper therefore blocks a
  plain double-click the first time: right-click (Control-click) the
  app and choose "Open", then confirm — needed only once. Intel Macs:
  use the jar below.
- **RISC-V:** no installer (nothing exists to build one on), but the jar
  below runs on any riscv64 JDK 25+, and the container image ships a
  `linux/riscv64` variant.

Installing associates `.jls` circuit files with JLS: double-click a `.jls`
file and it opens in the editor. Each installer has a sha256 entry in the
`SHA256SUMS-installers-<os>-<arch>` release asset, and each carries a
signed build-provenance attestation, checkable with
`gh attestation verify JLS-<version>-<arch>.<ext> --repo anadon/JLS`.
Note the scope of each guarantee: the checksums identify the exact bytes
that were published and the attestation proves those bytes came from this
repository's release workflow at a given commit — but the installers are
*not* byte-reproducible (the native packaging tools embed wall-clock
state), so rebuilding the same commit yourself will produce different
checksums. That is expected; the jar and `bom.json` are the
byte-reproducible artifacts (CI rebuilds and re-checks them on every
push), while installer integrity rests on the attestation.

The rpm and the AppImage intentionally carry no project-held GPG
signature (issue #136 — see `SECURITY.md`'s "Release artifact signing &
verification" for the custody rationale): a single-maintainer signing key
would add rotation/revocation risk without adding a guarantee beyond what
the checksums and attestation above already give you. Use the checksums
and provenance attestation described above to verify the rpm and
AppImage, the same as the deb — Debian tooling verifies signed
*repository* metadata rather than individual `.deb` files, so the deb has
never carried an embedded signature either.

## Running JLS from the jar (no installer)

The plain jar remains the portable path — for lab machines you cannot
install onto, or if you already have a Java runtime (JDK/JRE 25 or newer):

- **From a release:** download `jls-<version>.jar` from the
  [Releases page](https://github.com/anadon/JLS/releases) and run:

  ```sh
  java -jar jls-<version>.jar
  ```

  The jar is self-contained — no other files are needed.

  Every release also ships a `SHA256SUMS` file and a CycloneDX software
  bill of materials (`bom.json`) listing exactly what is bundled inside
  the jar. To verify a download, put `SHA256SUMS` next to the jar and run
  `sha256sum -c SHA256SUMS`; releases additionally carry signed build
  provenance, checkable with
  `gh attestation verify jls-<version>.jar --repo anadon/JLS`.
  The jar and BOM are bit-for-bit reproducible from the tagged source:
  each release publishes a `.buildinfo` recording the exact build
  environment, and [docs/reproducibility.md](docs/reproducibility.md)
  gives the independent-rebuild recipe.

- **From the Maven registry:** releases are also published to GitHub
  Packages (`maven.pkg.github.com/anadon/JLS`, artifact
  `io.github.anadon:jls`) for Maven tooling. GitHub requires an access
  token even for public downloads there, so plain-download users should
  prefer the Releases page.

- **Container image (batch mode only):** for autograders and CI,
  `ghcr.io/anadon/jls` runs the headless surface — test vectors, VCD
  waveform export, circuit image export, HDL export — with no local
  Java runtime:

  ```sh
  docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests circuit.jls
  ```

  Multi-arch: `linux/amd64`, `linux/arm64`, and `linux/riscv64` under
  one tag. The image is headless by construction (no display stack);
  use an installer or the jar for the GUI editor.

  Images are signed (keyless cosign, bound to this repository's release
  workflow) and carry build-provenance attestations:

  ```sh
  cosign verify ghcr.io/anadon/jls:<version> \
    --certificate-identity-regexp='^https://github.com/anadon/JLS/' \
    --certificate-oidc-issuer=https://token.actions.githubusercontent.com
  gh attestation verify oci://ghcr.io/anadon/jls:<version> --repo anadon/JLS
  ```

- **Command-line options:** `java -jar jls-<version>.jar -h` prints the full
  list, including batch mode (`-b`), test-input files (`-t`), simulation time
  limits (`-d`), VCD waveform export (`-vcd`, for GTKWave/Surfer and
  autograders), image export (`-i`, PNG named after the circuit file by
  default, or pass an output path such as `-i out.jpg` for JPEG or
  `-i out.svg` for resolution-independent SVG — the right format for
  slides, lab reports and hosted docs),
  Verilog export (`-export out.v circuit.jls` writes the drawn circuit
  as a structural Verilog-2005 module — a deployment bridge, not an HDL
  tutorial; note JLS's two-state-plus-HiZ semantics),
  plain-text re-save (`-savetext out.jls circuit.jls` rewrites the
  circuit uncompressed, for version-control diffs and for JLS forks
  without an XZ reader — see "Circuit file compatibility" below), and
  printing (`-p`/`-v`/`-r`).
  Diagnostics go to stderr as one `jls: error: ...` line; exit status is
  0 on success, 1 on runtime failure, and 2 on a usage error.
  The batch interface — the `-t` test-vector grammar, the watched-element
  output format, and the VCD profile — is a documented stability contract:
  see [`docs/batch-interface.md`](docs/batch-interface.md).

### Wayland

JLS runs natively on Wayland via OpenJDK's experimental Wayland toolkit
(`WLToolkit`, Project Wakefield), currently shipped by [JetBrains
Runtime](https://github.com/JetBrains/JetBrainsRuntime). On a
Wayland-only session (`WAYLAND_DISPLAY` set, `DISPLAY` unset) JLS selects
that toolkit automatically at startup when the Java runtime provides it —
the same `java -jar` command as everywhere else, no flags. On a runtime
without it (stock OpenJDK today), JLS prints one `jls: error:` line naming
the actual problem and the two ways out: run under XWayland by setting
`DISPLAY`, or use a JBR/Wakefield build. Sessions with `DISPLAY` set
(X11 or XWayland), Windows, macOS, and all headless modes (batch, image
and Verilog export) are untouched.

The JVM property `-Djls.toolkit=default|wayland` overrides the detection
in either direction. CI exercises this end to end: the `gui-wayland` lane
boots the GUI on a JBR under a headless sway compositor and screenshots
it via [`scripts/wayland-rig.sh`](scripts/wayland-rig.sh) (issue #101),
which also reproduces the setup locally or in the dev container.

The supported desktop matrix:

| Desktop / session | AWT toolkit | Java runtime | Status |
|---|---|---|---|
| Windows | Win32 | any JDK 25+ | supported |
| macOS | Cocoa | any JDK 25+ | supported |
| Linux, X11 session | XToolkit | any JDK 25+ | supported |
| Linux, Wayland via XWayland (`DISPLAY` set) | XToolkit | any JDK 25+ | supported |
| Linux, Wayland-native (`DISPLAY` unset) | `WLToolkit` (experimental) | JBR / Wakefield builds | supported |
| Headless (batch, image/Verilog export) | none | any JDK 25+ | supported |

The Wayland-native row is verified two ways: CI's headless-sway lane on
every push, and — because a headless software-rendered rig can diverge
from real GPU-backed compositors — a scripted once-per-release
spot-check on a physical GNOME (Mutter) or KDE (KWin) desktop:
[`docs/wayland-desktop-checklist.md`](docs/wayland-desktop-checklist.md).

## Building from source

The build uses Maven and JDK 25+:

```sh
mvn verify          # compile (warnings are errors), run tests, SpotBugs
java -jar target/jls-*.jar
```

Sources live in the historical `src/` layout (not `src/main/java`); tests are
under `test/`. Continuous integration builds every push on JDK 25, plus an
advisory (non-blocking) build on the newest GA feature release for early
warning. The Java floor follows the current LTS at the time of each raise
and is revisited once per LTS cycle. Pushing a `v*` tag publishes a GitHub
Release with the runnable jar and the per-OS installers
(`scripts/build-installer.sh` is the single recipe used both locally and
by CI).

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
- **ImageMagick** — inspect and compare the images written by batch
  image export (`-i`) and screenshots taken with `grim`.
- **[Icarus Verilog](https://steveicarus.github.io/iverilog/)
  (`iverilog`), GHDL, Yosys** — external HDL toolchain for the HDL
  export/import roadmap
  ([docs/hdl-support-research.md](docs/hdl-support-research.md), issues #33
  and #59). The HDL-export validation tests compile the generated Verilog
  with `iverilog` when it is installed and skip cleanly when it is not
  (CI installs it on its runners); `ghdl` will play the same role for the
  future VHDL emitter; Yosys synthesizes Verilog to the JSON netlists
  planned for import.

All of these are stock packages on Debian/Ubuntu
(`apt install xz-utils zip unzip sway grim wtype wayland-utils fontconfig
fonts-dejavu-core imagemagick iverilog ghdl yosys`).

**Displaying the GUI without X11.** The Maven test suite and batch mode run
fully headless (`java.awt.headless=true`) and need no display server of any
kind. The interactive GUI is another matter: stock OpenJDK's Swing toolkit
on Linux only speaks X11, so on the Wayland compositor above it needs a JDK
that includes OpenJDK's experimental Wayland toolkit (Project Wakefield) —
currently shipped by [JetBrains
Runtime](https://github.com/JetBrains/JetBrainsRuntime). On such a runtime
JLS selects `WLToolkit` by itself on Wayland-only sessions (see "Wayland"
above); the underlying JDK-level switch, useful for other Swing programs
or to force the toolkit when `DISPLAY` is also set, is:

```sh
java -Dawt.toolkit.name=WLToolkit -jar target/jls-*.jar
```

[`scripts/wayland-rig.sh`](scripts/wayland-rig.sh) automates the whole
first-light experiment (issue #101): headless sway up, a minimal
[`HelloSwingControl`](scripts/HelloSwingControl.java) frame mapped first
(so a failure is classified as JLS-side — exit 1 — or upstream JBR/sway —
exit 2), then JLS launched on a JBR (`JBR_HOME=...`), window presence
asserted via `swaymsg`, screenshot and logs collected into an artifacts
directory. CI's `gui-wayland` lane runs exactly this script — on every
push/PR and on a nightly cron that runs only this lane. The development
container below accepts a `JBR_URL` build argument to bake that runtime
in.

### Development container

[`.devcontainer/Dockerfile`](.devcontainer/Dockerfile) builds an image with
Maven, Temurin JDK 25, and all of the optional tools above — and no X11
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

- **XZ-compressed text** — what current versions of JLS write by default
  when saving. Despite the plain `.jls` name, these files are XZ data (they
  start with the `ý7zXZ` magic bytes) wrapping a line-oriented text
  description of the circuit.
- **Zip archive** — the original JLS format: a zip containing a single
  `JLSCircuit` entry with the same text description.
- **Plain text** — the uncompressed circuit description itself. Current JLS
  writes it on request: choose the plain-text file type in File > Save As,
  or run `jls -savetext out.jls circuit.jls` to rewrite an existing file.
  Plain-text saves diff cleanly in version control and open in JLS forks
  that dropped the XZ reader (the 4.6–4.10 fork lineage); saves stay
  XZ-compressed unless you opt in.

Inside whichever container, circuit text written by current JLS begins with
a `FORMAT 1` version line ahead of the top-level `CIRCUIT` line; files from
older versions have no such line and still load.

**Forward-compatibility caveat:** current JLS saves Memory initial contents
run-length encoded. The upstream JLS 4.1 loader does not understand that
encoding and **silently drops initial memory contents** when opening such a
file — the circuit loads, but memories start empty. If a file must
round-trip through JLS 4.1, avoid Memory initial values or re-enter them
there.

Editor checkpoint files (`.jls~`) are used for crash recovery. They are
written in the same XZ format as regular saves (older versions wrote them
as zip archives; the loader still accepts those). If you process `.jls`
files with external tools, sniff the content rather than trusting the
extension.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — contributor's map: module
  layout, save/load pipeline, editor and threading model, error
  contracts, test layout, and recorded scope decisions.
- [docs/simulation-semantics.md](docs/simulation-semantics.md) —
  normative spec of the simulation model: time, events, delays, edge
  triggering, tri-state/HiZ.
- [docs/batch-interface.md](docs/batch-interface.md) — normative spec
  of the batch/grading interface: `-t` grammar, output format, VCD.
- [docs/file-format.md](docs/file-format.md) — normative spec of the
  `.jls` save format: containers, grammar, element tags, versioning.
- [CONTRIBUTING.md](CONTRIBUTING.md) — how to build, test, and submit
  changes.
- [SECURITY.md](SECURITY.md) — threat model and reporting.
- [CHANGELOG.md](CHANGELOG.md) — user-visible changes per release.

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
