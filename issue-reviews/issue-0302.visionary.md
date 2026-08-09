# Issue #302: CAP-07: a design drawn in JLS comes back from an open shuttle as a physical chip the student can hold
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Underneath 53k characters of tier bookkeeping is a claim I agree with: *JLS should
stop being a tool whose output stops at text.* The trajectory supports it —
`docs/capability-roadmap/sweep-06-physical-boundary.md` §F, the roadmap's own
verdict at `docs/capability-roadmap/README.md:1152` ("In. Real. Cheap. And
strictly gated."), and the fact that the FPGA half of this already shipped
(`src/jls/hdl/board/`, 609 lines; `scripts/icestick-handoff.sh`;
`docs/icestick-bitstream-handoff.md`). The three vocabulary gaps it names are
real and independently verified in the tree today: `HdlModel.java:28-33` has two
`Direction` constants, `Register.java` has no reset of any kind, and
`HdlExporter`'s javadoc at `:88` still rejects `SubCircuit`.

But the issue picks its outcome by *artifact* — a chip in a hand — and then
derives everything else from that choice. Three consequences follow, and each is
a place where a different cut is available.

## Reframing 1 — the chip is the trophy; target-honest export is the payload

The single most valuable sentence in this issue is not about silicon. It is that
`test/resources/hdl/counter.v:21` emits `reg [3:0] count = 4'h0;`, which FPGA
synthesis honours and ASIC synthesis discards. That is not a missing feature. It
is a **construct in JLS's shipped goldens whose truth value depends on a target
JLS does not name** — the roadmap calls it "the single most dangerous paper-over
in the tree with respect to silicon" and notes that JLS's own goldens bless it.

That defect is falsifiable *today*, in-tree, with zero shuttle involvement: run
the exported design through synthesis and re-simulate; observe that JLS's
simulation and the synthesized netlist disagree about power-up. The idiom already
exists (`test/jls/hdl/GhdlCompileTest.java`, `IverilogCompileTest`, `ToolLocator`)
and FEAT-023 (#359) is the issue that arms it. The returned chip adds no
information to that experiment — it is the same fact, twelve months later, at the
mercy of a third party.

The spine feature agrees with me and the capstone does not notice. #328's global
invariant 5 reads verbatim: **"A returned chip is not a returned test. Silicon
coming back does not by itself demonstrate the drawn design was correct;
acceptance rests on the pre-submission flow, with the chip as the demonstration.
Any child that inverts this is asserting something the evidence does not
support."** CAP-07's Definition of Done then requires "AC-7 performed on returned
physical silicon … no substitute (simulation, FPGA, emulation) satisfies it."
That is the inversion invariant 5 forbids, written into the parent's close-out.

**I am explicitly disregarding AC-7 as an acceptance criterion.** Keep it as a
milestone with no code depending on it. Make the capstone's close-out the event
that actually carries engineering information — *a submission the shuttle's own
CI accepted and built* (AC-4 + AC-5) — and let the chip arrive whenever it
arrives. As written, the Definition of Done cannot be discharged for months after
the work is done, by a program that has already killed one predecessor
(Efabless, March 2025, recorded in the issue's own Open Question 7). A terminal
criterion gated on a third party's continued existence is not a criterion.

## Reframing 2 — the perishable part is data, not a Java enum constant

The plan puts the shuttle inside the jar: #328 §3 modifies `Board.Format`'s enum,
adds a `Boards` row, and relies on invariant 3 ("no `default` arm in a format
dispatch switch") to make exhaustiveness a compile-time gate. That is good Java
and the wrong seam for *this* target, and KC-07-1 is the issue half-noticing:
it prices "the emitter cannot track the template" and offers one all-or-nothing
escape (ship a recipe instead), decided later, under deadline.

Look at what is actually stable and what actually moves:

- **Stable** — walk the port set `HdlExporter.buildModel` produces, bind it to a
  fixed external interface, validate all-or-nothing, refuse naming every offender.
  That is `PinBindings.java` (98 lines) plus `PcfEmitter.java` (199 lines),
  shipped, tested, and target-independent.
- **Perishable, on an annual cadence nobody here controls** — which signal names,
  which widths, which `info.yaml` keys, which flow-config stanza, which template
  digest.

Today JLS already reads a **user-authored text file** to drive an export:
`-pins`, parsed by `PinBindings.parse`, reporting every malformed line at once.
Extending that to a pinned *target descriptor* — signature signal list with
widths and directions, metadata key set, digest — is the same mechanism, not a
new one. `Board.Format` gains one constant for the *family* (a wrapper-plus-
metadata target), exhaustiveness survives, and the shuttle-of-the-year becomes a
vendored data file under `test/resources/` with a digest test.

The property this buys is the one the issue never considers. JLS's deployment
model is a **single offline jar, version-locked to the binary** — ratified in
ARCHITECTURE.md's help-delivery decision and visible throughout the README's
installer story. If the shuttle signature is compiled in, then *the shuttle a
student can submit to is a function of which installer their lab happened to
deploy*. A course on last semester's `.deb` cannot submit to this semester's
shuttle, and the fix is a Windows MSI rebuild and a release. With the target as
pinned data, an instructor updates one file. That single fact is worth more than
the whole of AC-1.

I note the objection honestly: `Boards.java`'s javadoc records #213's H2 — the
table "grows on demand rather than through a general board-description format."
That rationale is sound for a physical package's pin map, which is a fact that
never changes after the board ships. It does not transfer to a target whose
definition is re-cut every shuttle cycle. H2 was decided against a different
perishability regime, and CAP-07's own KC-07-1 is the evidence for saying so.

## Reframing 3 — four capstones are re-litigating one program

CAP-07 requires six features. Five of them (#327, #336, #339, #358, #359) serve
four to eight capstones each by their own `serves_capstones` lists; exactly one
(#328) is private, at 11.5–18 mw registry band and **4 exclusively-owned weeks**
by #328's own reconciliation. So a "38.5–61 mw capstone" is, in work terms, one
shared program plus roughly a month.

Open Question 8 asks the right question and answers "siblings" because "a chip, a
toolchain matrix and a board are three different artifacts." True of the
artifacts; false of the work. The cost of the sibling answer is already being
paid in this issue's own comment thread: the same six-feature sufficiency
argument, the same DAG walk, and the same arrow adjudication were performed here
and — per the ADJUDICATED comment — "the same edge was withdrawn on #310 with the
identical verdict."

The alternative cut is one capability: **JLS's export is trustworthy enough to
hand to a flow JLS does not control**, with pluggable targets (icestick shipped;
tinytapeout, PCB, second FPGA as data descriptors per Reframing 2). CAP-05,
CAP-07 and CAP-15 become *target instances* of one program with one sufficiency
argument, one integration-risk section, and one set of system ACs — AC-4 (local
pre-check agrees with the external authority), AC-6 (names survive a boundary),
AC-8 (byte-identical export) are already written target-agnostically here and
would be stated once. The chip, the board and the matrix stay three separate
demonstrations, which is what they are.

## Reframing 4 — the issue kept the commodity and deferred the differentiator

The roadmap is explicit about what only JLS can do: cross-probing. "Click a NAND
you drew; its two `sky130_fd_sc_hd__nand2_1` instances light up in the layout …
*E without D is a worse KLayout. E with D is a thing that does not exist*"
(`docs/capability-roadmap/README.md:635-660`). CAP-07's Open Question 2 disposes
of the layout view with "the paragraph and the link, at zero maintainer-weeks."

So the capstone retains the part any competent contributor could write as a
200-line shell script — a fixed-signature wrapper and a YAML file — and defers
the part no other tool in the category can ever have. AC-6 ("names survive a
boundary JLS does not control") is the seed of cross-probing and is filed as a
traceability *check* rather than as the beginning of a product. I would keep AC-6
and reread it as the first increment of the differentiator, not as bookkeeping
against a cell report.

## What I would fund next month

The issue's own Cost section already contains the answer and buries it last:
the **demo slice, 1.5–2 mw** — wrapper emitter plus binding file over a design
with an explicit reset already drawn, producing a submittable artifact *before*
FEAT-037 or FEAT-021 exists. Run it against a real submission window this
semester. It will discover the true requirements in weeks, for the price of a
rounding error on a 38.5–61 mw plan, and it directly tests the one thing no
amount of §2 argument can: whether the shuttle's CI accepts what JLS emits.

Note the tension this exposes: if the demo slice works at all, §2's minimality
claim ("remove FEAT-021 and the capstone fires KC-07-3") is overstated — the demo
slice is the counterexample, printed in the same document. That is not a defect
in the demo slice. It is evidence that the required set was derived from the
ideal outcome rather than from a walked path, which is exactly the failure mode
`docs/icestick-bitstream-handoff.md:119`'s six `_TBD_` cells already record.

## What I endorse without change

The six required features. Every one is correctness work JLS owes its users
regardless of silicon: a register that cannot express reset is wrong in
simulation before it is wrong in fabrication; a two-constant `Direction` enum
against an importer that already models three (`YosysNetlist.java:136`,
`ScannedPort.java:19`) is an asymmetry that will bite a round trip; a flattening
exporter makes every non-trivial design unexportable. Fund them on their own
merits — which the sibling capstones already establish — and the chip becomes a
consequence rather than a justification.
