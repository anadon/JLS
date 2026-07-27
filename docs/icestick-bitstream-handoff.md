# iCEstick bitstream handoff

This is the end-to-end recipe for taking a JLS circuit onto a Lattice
iCEstick (iCE40-HX1K, TQ144 package): JLS exports the RTL and the pin
constraints, and the open iCE40 toolchain does synthesis, place & route,
packing, and flashing.

**JLS does not build bitstreams.** It emits HDL and a constraint file and
stops there (issue #215 H2: delegate, do not reimplement). Everything
downstream of the export is external open-source tooling, wrapped for
convenience by [`scripts/icestick-handoff.sh`](../scripts/icestick-handoff.sh)
but never reimplemented inside JLS.

## Prerequisites

The open iCE40 flow, all from YosysHQ / project-icestorm:

| Tool | Role | Where |
|------|------|-------|
| `yosys` | synthesis (RTL → iCE40 netlist) | <https://github.com/YosysHQ/yosys> |
| `nextpnr-ice40` | place & route for the iCE40-HX1K | <https://github.com/YosysHQ/nextpnr> |
| `icepack` | netlist → bitstream packing | <https://github.com/YosysHQ/icestorm> |
| `openFPGALoader` *(preferred)* | flash the board over USB | <https://github.com/trabucayre/openFPGALoader> |
| `iceprog` *(fallback)* | flash the board over USB | ships with project-icestorm |

On many distributions these are a single package install (for example
`apt-get install yosys nextpnr fpga-icestorm`); the [oss-cad-suite](https://github.com/YosysHQ/oss-cad-suite-build)
bundle ships all of them, `openFPGALoader` included.

The handoff script performs an all-or-nothing preflight: it reports
*every* missing tool in one pass — each with its role and where to get
it — and exits nonzero before running anything, mirroring `PcfEmitter`'s
error aggregation. The programmer is only required when you pass
`--flash`.

## Step 1 — export the RTL and pin constraints from JLS

JLS emits the Verilog and the iCEstick `.pcf` (see the `-export` /
`-board` / `-pins` options wired in `JLSStart`):

```sh
jls -export design.v -board icestick -pins pins.txt design.jls
```

- `design.v` — the exported Verilog.
- `-board icestick` — selects `Boards.ICESTICK` (iCE40-HX1K / TQ144), so
  the emitted constraint file is a `.pcf` with `set_io` lines placed on
  that device's real package pins.
- `-pins pins.txt` — your port-to-pin bindings; the emitter fails with an
  aggregated error listing every unbound or mis-bound port (issue #213
  P3) rather than producing a partial constraint file.

This produces `design.v` and, alongside it, the `.pcf` (referred to as
`design.pcf` below).

## Step 2 — hand off to the bitstream toolchain

The wrapper carries the export the rest of the way:

```sh
scripts/icestick-handoff.sh design.v design.pcf
# or, to program the board immediately after packing:
scripts/icestick-handoff.sh --flash design.v design.pcf
```

By default the top module is the `.v` basename (`design.v` → `design`);
override it with `-t/--top NAME`, and redirect the intermediate/output
files with `-o/--out DIR`. The script echoes each stage command it runs
with a `==>` prefix.

### The raw commands it wraps

Nothing here is JLS-specific; the wrapper only fills in the iCEstick's
device/package flags (hard-coded to match `Boards.ICESTICK`) and the
per-tool preflight. You can run the same flow by hand:

```sh
# synthesis: RTL -> iCE40 netlist
yosys -p 'synth_ice40 -top design -json design.json' design.v

# place & route on the iCE40-HX1K in the TQ144 package
nextpnr-ice40 --hx1k --package tq144 \
    --pcf design.pcf --json design.json --asc design.asc

# pack the routed design into a bitstream
icepack design.asc design.bin

# flash the board (openFPGALoader preferred; iceprog is the fallback)
openFPGALoader -b ice40_generic design.bin
# or:
iceprog design.bin
```

## External-tool honesty note

JLS's contribution to this pipeline is exactly two files — the Verilog
and the `.pcf` — plus the wrapper's argument bookkeeping and preflight.
Synthesis (`yosys`), place & route (`nextpnr-ice40`), bitstream packing
(`icepack`), and flashing (`openFPGALoader` / `iceprog`) are all external
open-source tools, invoked, not reimplemented. The device and package
(iCE40-HX1K / TQ144) are the only board facts the wrapper hard-codes, and
they are the same facts `Boards.ICESTICK` uses to emit the constraints.

The wrapper's control flow — the preflight and the tool-chaining — is
regression-guarded by
[`scripts/icestick-handoff-selftest.sh`](../scripts/icestick-handoff-selftest.sh),
a pure-shell harness that stubs the toolchain on `PATH` and runs in CI.
The self-test proves the *control flow*, not that the exact `yosys` /
`nextpnr-ice40` / `icepack` arguments synthesize a real design; that is
validated on a machine with the toolchain installed (and, for the flash
step, with a board attached).

## Manual flash version record (#215 P2)

To be filled in from a real flash, once hardware is present:

| Date | Board | `yosys` | `nextpnr-ice40` | `icepack` | Programmer | Result |
|------|-------|---------|-----------------|-----------|------------|--------|
| _TBD_ | Lattice iCEstick (iCE40-HX1K, TQ144) | _TBD_ | _TBD_ | _TBD_ | _TBD_ | _TBD_ |
