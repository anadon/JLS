# Issue #429: TASK-0094: one export writes the design, a tt_um_* wrapper matching the shuttle's fixed signature and its info.yaml — or writes none of them and names every problem
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is not a `tt_um_*.v` file. The end is #302/CAP-07: a thing a student drew
comes back as a chip they can hold, and — per `docs/capability-roadmap/sweep-06-physical-boundary.md:386-392`
— the *sober* payload on the way there is the budget: 8 in, 8 out, 8 bidirectional,
one clock, one reset, one tile. "Fit your design in a tile" is the lesson;
the wrapper is the receipt. The roadmap's own verdict (`README.md:1152-1187`) is
"In. Real. Cheap. And strictly gated." I agree with all four words, and the
argument below is entirely about *which* route reaches that end and in what order.

## 1. The wrapper is a module instantiation, and #429 proposes to hand-write the one construct JLS's IR is missing

`HdlModel` at HEAD has no instantiation statement (`src/jls/hdl/HdlModel.java` —
eleven statement kinds, none of them an instance), and `SubCircuit` sits in the
exporter's reject map with the reason spelled out: *"the HDL model has no
module-instantiation statement, so hierarchy cannot be rendered"*
(`src/jls/hdl/HdlExporter.java:465-468`, per #358 §1).

The shuttle wrapper *is* a module that instantiates another module. §7.4 says so:
"instantiating the JLS-exported module inside it". So `ShuttleWrapperEmitter`
would become the first and only place in the tree that knows how to write a
Verilog port header and a port-mapped instantiation — as hand-assembled text,
outside `HdlModel`, outside `VerilogEmitter`, outside the sealed visitor that
`ARCHITECTURE.md` and #358 §4 invariant 3 treat as the safety mechanism for
exactly this class of change.

And #358 (FEAT-018) already owns that seam, names this exact consumer —
*"a shuttle entry is a wrapper module instantiating the student's top module"* —
and lists #302 in `serves_capstones`. **#429 cites neither #358 nor #292**, in
`blocked_by`, in `related`, or in §12's Related Work table, which is otherwise
ten rows long. That is not a missing link; it is a missed reframing.

**Concrete alternative (A).** Make the wrapper the *first consumer* of FEAT-018's
`InstanceStatement` rather than a bypass of it. Then:

- the wrapper is ~30 lines that build an `HdlModel` — eight `Port`s, one
  instance statement, a handful of constant assigns for the tie-offs — and
  `VerilogEmitter` renders it;
- it inherits legalization (`HdlNames`), the generated-file header, determinism,
  and the existing golden regime for free, instead of re-implementing each;
- a VHDL wrapper is free if anyone ever wants one;
- P4 ("no emitted wire left dangling") becomes a property of a
  `ConstantStatement`, not of a hand-built string;
- there is exactly one place in JLS that emits Verilog, forever.

The cost of not doing this is two Verilog writers that must be kept in agreement
by review. That is precisely the failure #358 §4.3 and the sealed-visitor
discipline exist to prevent.

## 2. The binding constraint on this trajectory is export coverage, and #429 does not name it

`blocked_by` names #416, TASK-0049 (bidirectional) and TASK-0077 (reset). All
three are real. But the constraint that decides whether *any* design a student
actually drew can reach a shuttle is the reject list: `SubCircuit` **and**
`Memory` are refused (`HdlExporter.java:88`). Subcircuits are how a real JLS
circuit is built. The roadmap says it plainly at `README.md:1176-1178`:
*"Without P3's export coverage, the only designs that can be taped out are gate
toys — the flagship CPU does not export at all."*

So as scoped, #429 delivers a shuttle wrapper around the set of designs that are
flat, subcircuit-free and memoryless. The issue's §11 lists six threats to
validity and this is not among them. Sequencing behind #358/#292 fixes both this
and finding 1 with one edge.

## 3. What #429 gets right is the refusals — and they are worth far more, far earlier, decoupled from everything else

§7.11's four refusals are the best part of this issue: no clock bound; over the
I/O budget; registers with no reset; bidirectionals with no output enable. Every
one of them is a **property of the drawn circuit**, computable today, at
`2d0ca9d`, with no wrapper, no `Boards` row, no `Format` constant, no template
digest, no INOUT direction, no reset attribute (absence is exactly what you
report), and no shuttle-submission calendar.

**Concrete alternative (B), and the one I would fund first.** Ship the refusals
alone as a target-profile check — `jls -check silicon design.jls`, or a
`-target` profile the exporter consults — that names every problem in one
message and exits 2. It is a week, it is unblocked by all five prerequisites,
and it delivers the pedagogy sweep-06 actually argues for: the budget as a
design constraint, and the reset lie made visible. `sweep-06:169-176` says JLS
today *actively mis-teaches* reset; a check that says "6 registers have no reset;
ASIC synthesis discards the initial value `reg [3:0] count = 4'h0;` this export
emits" teaches the lesson at the moment the student can act on it, and needs
`Register` to gain nothing.

It also has room for the refusal #429's list is missing. `VerilogEmitter.triState`
emits `assign net_2 = en ? a : 4'bz;` (`test/resources/hdl/tristate.v:12`).
Internal tri-state nets do not exist inside a TT tile — the tri-state lives in the
`uio_*` pads and nowhere else. A student who draws the tri-state bus JLS
documents as a core semantic (`docs/simulation-semantics.md` §2, §9) has an
unsubmittable design, and none of §7.11's four refusals catches it. A
silicon-readiness check is the artifact that can grow that fifth refusal without
renegotiating a wrapper contract.

Sequenced this way, the check is also the acceptance gate the wrapper later
consumes, so nothing is thrown away.

## 4. Do not put someone else's moving signature inside `src/` and the goldens

§7.2 and #328's invariant 1 already concede the target *moves on a cadence the
project does not control* — hence a digest pin, a `REPLAN:` protocol for drift,
and P3's admittedly-transcribed expected port list which §9 then asks a human to
re-read against the template by eye. That is a lot of machinery to hold a foreign
constant still.

**Concrete alternative (C): invert the dependency.** Let the fixed signature be
*data the user supplies*, in the same file family as `-pins`: a target-signature
description (port names, widths, directions, tie-off rule), with the Tiny Tapeout
one shipped under `examples/` or `resources/` and dated. Then:

- JLS's goldens pin **JLS's behavior** — "given this signature and these
  bindings, this wrapper" — which is what a golden should pin, instead of
  pinning a third party's schema revision, which is what P2 currently does;
- a shuttle rename, respin or successor program is an example-file update, not a
  source change plus a digest re-pin plus golden churn plus a `REPLAN:`;
- #328's Open Question 1 ("which program, which revision?") stops blocking
  execution, and Open Question 2 ("`Boards` row or sibling target?") dissolves —
  it is neither; it is an input;
- an instructor targeting a different program, a course-specific FPGA harness, or
  next year's shuttle gets it without a JLS release.

This is the same discipline `Boards` already follows for pin maps (data, not
code) applied one level up, and it is more faithful to the project's
"single-maintainer, offline jar, no acquired dependencies" arc than compiling a
foreign organization's 2026 port list into `Boards.java`.

## 5. The `Format` enum trap is a symptom being celebrated as a virtue

`Board.Format` carries a file *extension* because a PCF is a sidecar named by
substitution off the `.v` path (`JLSStart.java:465-473`). `TT_WRAPPER` has no
extension: it is two-or-more files with fixed names. §7.1 predicts this breaks
the dispatch and calls it "the intended trap, not a defect" — but the compile
break here is not exhaustiveness catching a forgotten case, it is a type telling
you the axis is wrong.

**Concrete alternative (D).** Change the emitter contract from
`String emit(...)` + extension substitution to `List<GeneratedFile> emit(...)`
where a `GeneratedFile` is (relative path, content). PCF returns one; the shuttle
returns however many. Consequences, all of them simplifications:

- there is no `Format` switch left to add a `default` arm to, so P6's totality
  assertion and the scratch-branch demonstration of a compile break are
  unnecessary ceremony over a mechanism that no longer needs guarding;
- H3's atomic-set problem collapses: stage one temp **directory**, rename once.
  §7.10's "honest limit" (three renames are not atomic as a set), §7.11's last
  bullet, and §11's fifth threat all disappear rather than being documented.
  #328's Open Question 5 already recommends this and marks it "rides along" — it
  is not a rider, it is the design;
- the real deliverable becomes expressible. A Tiny Tapeout submission is a
  *project directory* (`src/project.v`, `info.yaml`, `docs/info.md`, `test/`),
  and `info.yaml` carries a `source_files` **list**, not eleven scalars — H2's
  framing understates its own object, and §7.4's field roster omits
  `source_files` and `top_module` entirely.

## 6. The artifact only JLS can produce is the one nobody asked for

§2's Intended Audience says the student path takes "Verilog, `info.yaml`, a
testbench and a docs file". #429 generates two of four and hands the other two
to a document (TASK-0095). But the testbench is the one artifact JLS is uniquely
positioned to generate: it already owns the student's stimulus and expected
results — the `-t` test-vector grammar and the VCD profile are a documented
stability contract (`docs/batch-interface.md`), and `BatchSimulator` already
produces the traces. Deriving the shuttle testbench from the vectors the student
already wrote is what makes the downstream CI mean something, and no other tool
in the chain can do it, because no other tool has the vectors. That is a more
interesting issue than this one and it should exist.

## What I am disregarding, and why

I am setting aside these stated acceptance criteria:

- **P1 / §7.1's `Board.Format.TT_WRAPPER` and the `tinytapeout` `Boards` row.**
  Under (C) and (D) the shuttle is neither a format nor a board; forcing it into
  a record whose field is literally named `fpga` and whose pins map names to
  package pads is a stretch #328 OQ2 already flags and this task inherits rather
  than fixes.
- **P6 and the no-default-arm demonstration.** Guarding a switch that alternative
  (D) deletes.
- **§7.10's residual atomicity limit and §11's fifth threat.** Solved, not
  documented, by staging a directory.
- **§7.4's hand-written `ShuttleWrapperEmitter`.** Replaced by an `HdlModel`
  transformation under (A).

What survives intact and should be kept verbatim: the all-or-nothing aggregation
(O7), the reuse of `PinBindings` and the `-board`/`-pins` pair rule (O6, H1) —
which is genuinely elegant and the best insight in the issue — the refusal-message
tests asserting text rather than booleans (§9), the golden regime (O8), P10's
byte-identical `blinky_icestick.pcf`, and the flat refusal to acquire a layout
geometry model, which is exactly right and worth restating wherever it is
challenged.

## Recommended shape

1. **Now, unblocked:** the silicon-readiness check (B), including the tri-state
   refusal #429's list misses. Delivers the budget lesson and the reset lesson
   without waiting on anyone.
2. **Then:** #292/#358's instantiation statement — the seam that unblocks
   subcircuit export (the actual constraint, finding 2) *and* makes the wrapper
   a 30-line model transform (finding 1). One edge, two problems.
3. **Then:** the wrapper and metadata, emitted as a project directory from a
   user-supplied target signature, through a multi-file emitter contract
   (A + C + D), with this issue's refusals already in place as its gate.

The end state #429 describes is the right end state. The route needs re-cutting,
and three of its five open questions stop blocking once it is.
