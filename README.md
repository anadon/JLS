# JLS — Java Logic Simulator

[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/anadon/JLS/badge)](https://scorecard.dev/viewer/?uri=github.com/anadon/JLS)

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
  limits (`-d`), VCD waveform export (`-vcd`, for GTKWave/Surfer and
  autograders), image export (`-i`, PNG named after the circuit file by
  default, or pass an output path such as `-i out.jpg` for JPEG),
  Verilog export (`-export out.v circuit.jls` writes the drawn circuit
  as a structural Verilog-2005 module — a deployment bridge, not an HDL
  tutorial; note JLS's two-state-plus-HiZ semantics), and
  printing (`-p`/`-v`/`-r`).
  Diagnostics go to stderr as one `jls: error: ...` line; exit status is
  0 on success, 1 on runtime failure, and 2 on a usage error.
  The batch interface — the `-t` test-vector grammar, the watched-element
  output format, and the VCD profile — is a documented stability contract:
  see [`docs/batch-interface.md`](docs/batch-interface.md).

## Building from source

The build uses Maven and JDK 17+:

```sh
mvn verify          # compile (warnings are errors), run tests, SpotBugs
java -jar target/jls-*.jar
```

Sources live in the historical `src/` layout (not `src/main/java`); tests are
under `test/`. Continuous integration builds every push on JDK 17 and 21.
Pushing a `v*` tag publishes a GitHub Release with the runnable jar.

**Optional development tools:** the HDL-export validation tests compile the
generated Verilog with [Icarus Verilog](https://steveicarus.github.io/iverilog/)
(`iverilog`) when it is installed and skip cleanly when it is not; `ghdl`
will play the same role for the future VHDL emitter. Install both for full
coverage (`apt-get install iverilog ghdl` on Debian/Ubuntu) — CI installs
them on its runners.

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
