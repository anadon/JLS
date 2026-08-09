# Issue #291: HDL export: lift Memory off the reject list — RAM/ROM array with zero-delay async read, #199 sync-write process, in both languages
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its machine block, the claim is: *JLS should be a schematic front end whose
drawings survive the trip to a real toolchain, and storage is the last thing in the way.*
That is squarely in the project's arc — README sells `-export` as "a deployment bridge",
`docs/hdl-support-research.md` already sketches "Memory → array + init from the existing
contents model", and the import half of the bridge (`jls.hdl.yosys.CellValidator`) has
already committed, in code, to what a JLS memory *is* in HDL terms. Endorse the goal.

The reframing is about where the seam gets cut. As written, this issue is scoped as
"delete one entry from a policy bucket and add one IR node for one element". Three things
in the repository say the natural unit is bigger and simpler than that, and one thing in
the issue says the unit is smaller than it claims.

## 1. The IR node should be a storage array, not a Memory node — RegisterFile is the same construct

`RegisterFile` (`src/jls/elem/RegisterFile.java:23-33`) is documented as "N words, each
`bits` wide, with a configurable number of asynchronous read ports and clock-synchronous
write ports". That is *exactly* the H1 sentence of this issue with the port counts turned
into parameters. `Memory` is the degenerate case: one async read port, zero or one
rising-edge write port, plus an output gate and an initial-contents image.

`CellValidator.checkMemory` (`src/jls/hdl/yosys/CellValidator.java:249-274`) already
decides importability by exactly those parameters — `RD_PORTS`, `RD_CLK_ENABLE`,
`WR_PORTS`, `WR_CLK_ENABLE`, `WR_CLK_POLARITY`. The import side has already generalized;
the export side is being asked to specialize.

Concrete alternative: add one `HdlModel.ArrayStatement` — `words`, `bits`, `List<ReadPort>`
(address operand, data net), `List<WritePort>` (address, data, enable, clock operand,
edge), optional `init` image, optional `zeroReadsAsZero`. `Memory` builds one with 1/0..1
ports; `RegisterFile` builds one with N/M. Both emitters render one construct. This is not
speculative generality: #873 AC-4 already asks whether `RegisterFile`'s "needs a
technology-specific primitive" refusal survives, and KC-2 anticipates the duplication as a
*hazard to be avoided by deferral*. Cut the seam here and KC-2 stops being a risk and
becomes the design — AC-2 and AC-4 are discharged by one node, and #873's remaining work
is `FieldExtend`, which is unrelated bit-slicing.

Cost of the reframe is small (the port lists are the only extra structure); cost of not
doing it is a second, near-identical emitter pass in three months plus a `RegisterFile`
refusal message that AC-4 already predicts will be false.

## 2. The issue's model of Memory is incomplete — and the missing part is already in the IR

H1 is "array declaration plus a continuous read plus a rising-edge write process". Read
`Memory.react` and that is not what a JLS memory does:

- The output is a **tri-state bus driver**, active-low on both `CS` and `OE`
  (`src/jls/elem/Memory.java:1393`); otherwise the element posts `TriStateOff` and drives
  null (`:1403`, `:1467-1470`). ROM and RAM even have different port shapes (`:184-196`).
- An out-of-range address drives HiZ, not zero (`:1446-1450`) — and a *write* to an
  out-of-range address is silently dropped (`:1420`).
- A never-written word reads as zeros, not X (`:1456`). That is load-bearing for golden
  parity and for `iverilog`, whose default array init is X.

None of this appears in §4, §7, or §8. The good news is that it argues *for* a smaller
change than the issue proposes: `TriStateStatement` already exists
(`src/jls/hdl/HdlModel.java:317-350`) and both emitters already render HiZ. The array node
should therefore emit a **plain internal read net** and nothing else; the exporter composes
the existing tri-state statement with control `~(cs | oe)` around it. Output gating,
range-checking and HiZ stay out of the new node entirely — which is also what keeps it
reusable for `RegisterFile`, whose read ports have no output enable at all.

## 3. `accessTime` defaults to 100, and the "time 0" story papers over it

`defaultAccessTime = 100` (`Memory.java:53`). Every memory a student draws without touching
the timing field has a 100-unit access delay. The issue rides #59's "zero-delay async read"
decision and relegates the mismatch to "documented in an emitted comment" (§7.1, §11).

For a lookup table this is fine. For the stated headline consumer it is not: §Intended
Audience claims this unblocks "the export side of the #202 differential oracle". A
differential oracle compares traces; a memory whose JLS read lands 100 units late and whose
HDL read lands at delta-zero does not compare unless the harness samples only at settled
cycle boundaries. That is a real design constraint on #202's comparator, and it belongs in
writing on #202 rather than in a comment inside generated Verilog.

Also: #292 (`SubCircuit`) is not yet landed, and per this issue's own third comment must
land *first*. A real RV32I drawn in JLS is hierarchical. So Memory export does not unblock
#202; `SubCircuit` does, and this is one of two prerequisites. I would delete the oracle
justification from the Abstract. The honest and sufficient value is smaller and truer:
*ROM/RAM-bearing student circuits stop hitting a wall at export.*

## 4. External-file initialization is unhandled, and answering it answers Open Question 2

`Memory` can be initialized from a file path read at `initSim` (`Memory.java:1245-1275`),
not at load. §7.3 says the exporter consumes "capacity, width, contents, and `syncWrite`" —
but for a file-initialized memory the exporter holds a *path*, not contents, and §7.2
restricts external I/O to "validation only". Three options, and the third is best:
refuse file-initialized memories (a wall for exactly the CPU-image use case #202 wants);
read the file during export (a new I/O surface in a class that has none); or **always emit
the initial image to a sidecar hex file** and reference it with `$readmemh` / VHDL
`textio`. That subsumes Open Question 2 (inline vs file) by choosing file-based
unconditionally: one code path, no size threshold to litigate, the sidecar diffs in git,
and it is the same artifact a student would hand to a synthesis flow anyway. The cost is
that `-export out.v` now writes two files — which is a CLI contract change worth deciding
deliberately rather than discovering.

## 5. The bucket itself is the wrong mechanism — put the disposition on the element type

The comment thread is more interesting than the issue. It records that `REJECTED` does not
exist on `master`, that #492 must build it, that #375 wants a registry-totality lint, that
#372 wants a GUI inventory, and that #201 added `RegisterFile` and `FieldExtend` with *no
export decision at all*. Four issues, one root cause: HDL disposition lives in a hand-kept
`Set.of(...)` in `HdlExporter` (`:422-428`) mirrored by a prose Javadoc list (`:80-92`) and
a policy test, all of which a new element can be added without touching.

`ElementRegistry` already solved this shape of problem. Its Javadoc (`ElementRegistry.java:16-22`)
says registration is one line per type and "forgetting the line is a build failure, not a
... load error discovered by a user". Add a fifth field to `ElementType` — the HDL
disposition (`EXPORTED` / `SKIPPED` / `TOPOLOGY` / `REFUSED(reason)`) — and the existing
registry-integrity test makes totality structural. Drift like #201's becomes
unrepresentable, #492's totality test becomes the registry test that already exists, #375's
lint disappears, and this issue's diff shrinks to: change one enum on one line, add the
array node, add the emitters. I would rather see that one-line-per-element table land as
part of #492 than see a `REJECTED` map built and then immediately edited by #291, #292 and
#420 in three directions.

## Explicit disagreements with the acceptance criteria

I am not disregarding §14 wholesale, but two items should change:

- **P4 / §8 "move `Memory.class` into `EXPORTED`"** — do not, if the registry reframing in
  §5 lands; the disposition should not be in `HdlExporter` at all. If #492 lands first as
  written, this is a temporary shape and the issue should say so.
- **The DoD's silence on RegisterFile** — if §1 is adopted, this issue's DoD should require
  that the array node be *parameterized*, with a `RegisterFile` follow-up that adds no new
  IR node. Without that requirement the specialized node will get built and the
  generalization will never happen.

Everything else in §14 is good discipline and should stand — particularly re-pointing
`memoryIsRejectedByName` rather than deleting it, and KC-1's "narrow the claim in writing,
do not widen the tolerance" on the read-during-write delta-cycle hazard. On that hazard:
if the array node emits a pure combinational read and writes commit in a separate clocked
process, `iverilog` and `ghdl` agree by construction on same-cycle read-during-write
(the read sees the old word in both, because the nonblocking/signal-assignment update lands
after the delta). The divergence risk the issue fears mostly evaporates once output gating
is composed out of the node rather than baked into it — one more reason to cut the seam
where §2 suggests.

## Verdict

**endorse-with-reframing.** The destination is right and overdue. Build a parameterized
storage-array IR node rather than a Memory node, compose HiZ from the existing
`TriStateStatement` rather than modeling it, emit initial contents to a sidecar
unconditionally, move the export disposition onto `ElementType`, and drop the #202-oracle
justification in favor of the plainer one that is actually true.
