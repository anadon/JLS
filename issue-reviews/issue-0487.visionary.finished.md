# Issue #487: FEAT-060: electrical intent leaves JLS as a rule file an external DRC enforces — a board routed 25% over its declared maximum length fails, and the shortened one passes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the machine block and the DAG walk and the claim is one sentence: **JLS should be
able to state something about the physical world and let a tool it does not control render
the verdict.** That is the real payload, and it is the best idea in CAP-18 (#313). Every
other criterion in that capstone is JLS grading its own homework — a golden compared to a
golden, a closed form compared to itself (AC-2 admits as much: 1e-12 "is a round-trip bound
rather than a physics bound"). #487's criterion 1 is the only one where a failure means JLS
was *wrong* rather than *inconsistent*. Endorse that end without reservation.

The permanence-before-cost sequencing (lint → constraint export → element, with the cheapest
rung deliberately last because it is the only one freezing a save tag, a palette entry and a
K9 obligation) is likewise correct and rarer than it should be. Keep it.

Three things about the route, though, pull against the project's own arc.

## Reframing 1 — this is not a new emitter, it is the second row in a mechanism that ships today

The issue cites `src/jls/hdl/board/` only as "the data-not-code precedent this emitter
copies" (Evidence 3) — precedent for a *golden test*. That undersells what is on disk.
`src/jls/JLSStart.java:387-470` already implements, end to end, the exact shape #487
proposes: one model walk produces both artifacts; the constraint text is fully validated
*before* the primary file is written so a bad binding aborts both; the constraint file lands
next to the primary output named by the target format's extension; both writes go through
temp-and-rename. `Board` is a record `(name, fpga, Format, pins)` whose `Format` enum
carries the extension and whose javadoc says outright that new constants land "when their
emitters land". `PinBindings` owns all-or-nothing validation. `test/jls/hdl/ToolLocator.java`
plus `Assumptions.assumeTrue` is the shipped external-adjudication idiom.

And the project has already ruled on how the second format arrives.
`docs/standards-adoption/06-fpga-constraint-formats.md:77`:

> **`PcfEmitter` must be generalized before a second format is added**; adding XDC by
> copy-paste would fork the all-or-nothing validation logic, which is the part that must
> never diverge between formats.

#487 proposes a second format (`.kicad_dru`) with a bespoke flag (`-si`), a bespoke emitter,
and a bespoke acceptance test, and never mentions `Board`, `Boards`, `Board.Format`,
`PinBindings`, or that instruction. Meanwhile #82/#213 wants XDC/QSF/LPF, #366 wants
KiCad/gEDA netlists, and the shuttle work (sweep-06 change F) explicitly proposes
generalizing `jls.hdl.board` "from `(name, fpga, format, pin map)` to a target descriptor".
That is four issues converging on one capability nobody has named:

> **Emit the target-specific intent files a downstream flow needs — all-or-nothing
> validated, byte-deterministic, accepted by the target's own parser.**

The elegant route is to build *that* seam once — a `Target` descriptor plus a
`ConstraintEmitter` contribution to the typed extension-point catalog
(`docs/extension-points.md`), with each target's accepted keyword set as **data** — and then
"SI constraints to KiCad" is a target row and a keyword table, not a 5.5-9.5 mw feature. The
CLI follows: `-si` is the fourth artifact-specific flag after `-export`/`-board`/`-pins`, and
that does not scale; a target descriptor emits its whole bundle. #487 is the right forcing
function for the generalization — it is the second constraint format, which is the exact
trigger 06-fpga-constraint-formats named — but it should be *scoped as* that generalization,
not as a parallel subsystem that will have to be merged into it later.

## Reframing 2 — do not persist routed length; stream it

Back-annotation is the scope that costs the most and buys the least as designed, and it is
the one that pulls hardest against the trajectory. `docs/capability-roadmap/sweep-06-physical-boundary.md:570-576`
puts PCB squarely in "what genuinely stays out — this is KiCad's domain and KiCad is
excellent at it," and the boundary it draws through Tier 7 separates *computing* physical
data (out), *reading* it (a viewer), and *being a front end to someone else's flow* (in, on
the PcfEmitter precedent). Emitting `.kicad_dru` is unambiguously the third. Storing routed
geometry in the `.jls` as a per-view section is the second drifting toward the first: it
gives JLS a persistent model of a board it cannot draw, cannot re-derive, and — decisively —
**cannot invalidate**.

That is the same failure the issue rejects sidecars for. Its own words: a sidecar "has no
mechanism to keep it in step with the circuit it describes, and the failure mode is a stale
constraint set silently applied to a changed design." A back-annotated length is a sidecar
that got smuggled *inside* the file. Reroute the board and the number in the `.jls` is
silently wrong, and no `must-understand` flag helps, because the section is present and
parses fine. Open Question 4 ("lengths only in v1") bounds the volume, not the staleness.

Concrete alternative: **routed length is a transient input to the check, not a saved datum.**

    jls -check --routed routed-lengths.csv design.jls

FEAT-058's verdict is recomputed against measured geometry at the moment of checking, the
source file and its mtime are named in the output, and the number never enters the save
format. This satisfies integration criterion 5 verbatim ("the verdict computed from the
routed number, not the declared one") and CAP-18's spirit, while deleting **FEAT-014 (#318)
from this feature's critical path entirely** — the dependency the issue itself flags as the
most dangerous cost line ("the half that shipped is not the half this feature reads",
11-17 mw carried as an upper bound on #313). A CSV keyed by net name needs stable net naming
(#336, already required) and nothing else: no per-view geometry section, no instance-path
addressing, no new save surface. **I am explicitly disregarding the fourth planned scope's
acceptance criteria as written**, because the goal behind them — closing the loop from routed
geometry back to the lint — is fully reachable without committing a permanent file-format
section, and this is the one rung of CAP-18 whose whole justification is that it commits the
least permanent surface.

## Reframing 3 — settle format criticality before its first consumer defines it

The strongest reasoning in the issue is K18-5: `docs/file-format.md:220-222` is normative
that unknown attributes are silently dropped, and a dropped *lint input* is harmless while a
dropped *constraint* is a silently unmanufactured requirement. That argument is correct and
it proves more than the issue claims. It says JLS's format has exactly one valve, it is
fail-open, and **every** future must-not-drop datum re-litigates this. Open Question 3
(FEAT-047 #367's whole-file epoch vs FEAT-013 #319's per-section flags, potentially
coexisting) and the boundary-note comment (the joint contract is recorded on #487 but not on
#319) are two faces of the same gap: nobody owns format-criticality governance, so the first
consumer will define it by accident and freeze it.

That should not "ride along". It should be settled on #319 as a general rule — a per-section
criticality axis with a documented refusal diagnostic, of which SI constraints are the first
instance and a regression test — before this feature's section is designed. Otherwise #487
inherits authorship of a mechanism its own §1 says it must not own.

## Arc check, and the one thing worth saying out loud about scale

sweep-06 opens by measuring that the repository's flagship design cannot reach step one of
the open flow: `-export` on the RV32I CPU rejects Memory, SubCircuit and ShiftRegister, and
`HdlPolicyTest` *pins that rejection as intended behaviour*. `PcfEmitter` can only constrain
what JLS itself emitted. A constraint emitter sits two layers downstream of an exporter that
today covers 21 of 33 element types with no hierarchy — so "electrical intent survives the
crossing to a real board" is, until sweep-06's Change A lands, a claim about circuits of a
few dozen gates. That does not invalidate the feature (a clock net on a small board is a
perfectly honest fixture, and CAP-04's 150 mm jumper at 2.1x critical length is a genuinely
excellent lesson), but it does mean the arc-level sentence in #313's abstract is currently
larger than what any executor can deliver, and the closing REPLAN should say so rather than
let the capstone read as though the crossing works for real designs.

Finally, the pattern this issue invents deserves a name and a home beyond itself. JLS now has
three instances of *externally adjudicated claims* — nextpnr parsing a PCF, GHDL/iverilog
compiling emitted HDL, KiCad's DRC judging a rule file — each with its own ad-hoc harness.
Promoting that to a project-wide standard (one locator, one digest-pinned container
convention, one dated acceptance ledger in the style of `docs/wayland-desktop-checklist.md`,
one rule that no interop claim ships stronger than what an external tool checked) is worth
more to JLS's trajectory than the `.kicad_dru` file is. #487's global invariant 5 already
states that rule for itself; it should be stated for the project.

## Bottom line

Endorse the end and the sequencing. Reframe the means: make this the generalization of
`jls.hdl.board` into a target/constraint-emitter seam that #82, #213, #366 and the shuttle
work all land on; make routed length a CLI-supplied input to `-check` rather than a saved
section, dropping #318 from the critical path; and hand format-criticality back to #319 as a
general rule before this becomes its defining consumer.
