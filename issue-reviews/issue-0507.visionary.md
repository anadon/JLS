# Issue #507: CAP-26: a colorblind student and a blind student complete the same JLS lab — one through a verified palette with redundant encoding, one through spoken navigation, prose narrative and a swell-paper tactile export
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the six planned features away and the claim is: **JLS should be the first
schematic simulator where a blind student can do the assigned lab, not a
workaround for it.** That is a real and correctly-chosen goal. It is also
consistent with the project's stated identity — a pedagogy tool for a course
ecosystem — in a way that most of the capstone roster is not: this one changes
who can take the course.

I endorse the goal without reservation. I do not endorse the route, and I am
explicitly disregarding one acceptance constraint (K9 / KC-26-4) because as
written it forbids the single cheapest fix the outcome depends on.

## The trajectory the issue is being judged against

JLS's architecture has one dominant commitment, visible everywhere:
**the model is real, the view is a thin shell over it.** `Simulator` and
`BatchSimulator` import no AWT (`HeadlessCoreRatchetTest`); undo is save/load
(`CircuitSnapshot`); every editor mutation is migrating to a closed, validated,
serializable op vocabulary in `jls.collab.op` that is explicitly headless
(`ArchitectureRulesTest.collabLayersAreHeadless`); the batch/CLI surface is a
*documented stability contract* while SVG export deliberately is not. The
project's leverage has always come from adding a **new front end over the model**,
never from making the canvas smarter.

CAP-26's PF-3 does the opposite. "Screen-reader circuit navigation with spoken
element, connection and live signal-state announcements" is, concretely, a
custom `AccessibleContext` over `SimpleEditor`'s custom-painted canvas plus live
change notification — the one place in the codebase where the picture, not the
model, is the source of truth. It is the most expensive (5–8 mw of a 14–23 mw
band), most platform-fragile part of the plan, and the capstone knows it: the
whole band is gated behind an Orca feasibility spike, and KC-26-2 exists solely
to survive its failure.

The project already reached the same conclusion and wrote it down. Its own
playbook, `docs/standards-adoption/03-accessibility-conformance.md` §6, costs
the canvas accessible-tree route at **8–15 maintainer-days for a credible v1,
plus permanent maintenance on every new element type, plus a genuine risk that
java-atk-wrapper surfaces none of it to Orca**, and recommends against building
it. CAP-26 cites neither that document nor its recommendation.

## Reframing 1 (the main one): equivalent facilitation, not a talking canvas

Do not make the drawing speak. **Ship a second, equal-power, textual view of the
circuit** — a focusable outline (`JTree`/`JList`/`JTable`) of elements, names,
values, and connections, beside the canvas, editing through `OpSink.submit`.

Why this is the better cut, not just the cheaper one:

- Stock Swing components carry working `AccessibleContext` implementations on
  **all three AT stacks today** — Java Access Bridge, NSAccessibility, AT-SPI —
  with zero bridge-specific work. The custom-canvas route has to earn each one.
- "Live signal-state announcement" stops being a live-region research problem
  and becomes a table model update on rows the reader already tracks. KC-26-2
  largely evaporates rather than being survived.
- It composes with the op layer instead of fighting the 4k-line mouse state
  machine: an outline that submits `AddElements`/`AddWire`/`SetElementConfig` is
  a *third* client of the vocabulary #163 is already building for. That is the
  seam the project has been cutting toward for a year.
- Revised 508 **E101.2 Equivalent Facilitation** explicitly contemplates it, so
  the VPAT story is stronger, not weaker.
- The playbook prices it at **3–4 days** against 8–15, and recommends gating the
  full JAAPI canvas tree on an actual user request.

The one thing lost is spatial reading of the drawing — which the canvas route
does not deliver either, since bounds-hit-testing through an AT bridge is
precisely the part most likely to fail the spike.

## Reframing 2: PF-3 and PF-4 are one thing, not two

An outline row ("AND gate `g3`, inputs from `a` and `carry`, output drives `sum`,
currently 1"), a prose narrative sentence, and a tactile SVG label are the same
function of the circuit at three verbosities. Build **one `CircuitDescription`
renderer over the element/net graph** and give it three sinks: rows for the
outline view, paragraphs for the export, labels+geometry for the tactile
profile. Filed as PF-3 and PF-4 they are two pipelines with two decay paths and
two totality tests; as one they share a registry-keyed totality test (which the
landed `src/jls/elem/ElementRegistry.java` now makes trivial to write) and one
determinism golden. The tactile SVG is then a *render profile* of the existing
`CircuitRenderer`, not an "export pipeline."

## Reframing 3: the lab path a blind engineer would actually pick is text

Blind hardware engineers work in HDL text, not spoken schematics. JLS already
has: plain-text `.jls` saves that diff in version control, structural Verilog
export (`-export`), `-t` test vectors, VCD, and a documented batch contract —
plus an HDL *import* roadmap (#33/#59, Yosys JSON netlists). A text-authored
circuit that round-trips into a `.jls` the instructor grades with the same
autograder is a complete lab path built entirely on the project's **strongest**
surface, and it is largely already funded by other issues. CAP-26 never
considers it. If the course requires drawing, the outline view covers that leg;
if it does not, text is a better student experience than any narration.

## Reframing 4: a conformance ledger, not a VPAT generator

PF-5's real invention is "no claim without a named passing test." That rule
should not be spent on one document. `docs/standards-adoption/` holds eleven
playbooks (RISC-V compliance, waveform formats, IP-XACT, IEC/IEEE symbols,
CRA/supply chain, tool qualification) each of which ends in a claim someone will
be asked to back. Build a **generic claims→evidence ledger** (a table of
claim rows, each naming a test; one test asserting every row's test exists and
passes; renderers per report) and the ACR is its first consumer. Same cost,
n-fold reuse, and it strengthens the standards program rather than annexing one
corner of it.

## Reframing 5: put the CVD oracle on the headless path

PF-2 as filed (#543) is a framebuffer filter in the GUI. But AC-1's actual
requirement is *automated screenshot analysis in CI*, and JLS already exports
PNG/SVG headlessly (`-i`). A `--simulate-cvd=deuteranopia|protanopia|tritanopia`
post-filter on the export path gives CI its oracle, gives instructors a
checkable artifact for their handouts, needs no display, and honors the
headless-core discipline. The in-app preview then becomes a thin view of the
same filter — which is the ordering this project uses everywhere else.

## Where I disregard the stated acceptance criteria

**K9 / KC-26-4 ("default visual theme pixel-unchanged for existing users,
gating every PF-1 commit") should be struck.** The project's own analysis
computes that `Theme.DEFAULT` fails WCAG 1.4.11 today: `nonZero` `#E69F00` at
2.10:1 and `watch` `#56B4E9` at 2.31:1 against the white canvas, and — worse —
the #75 keyboard caret is painted in `selectionColor` `(240,240,240)`, the same
value as the grid, at **1.14:1**. The keyboard path this entire capstone stands
on has an indicator a low-vision user cannot see. A "pixel-unchanged default"
gate forbids fixing that. `Theme.CLASSIC` already exists precisely so no user is
forced off the old colors; that is the compatibility promise, and it is enough.
Replace K9 with "CLASSIC stays byte-identical; DEFAULT changes are announced in
CHANGELOG.md," and keep the ΔE≥25 constraint joint with a new ≥3:1 floor in one
run.

## The blockers the capstone does not mention, and must

Each is red today, cheap, and gates the outcome as stated:

1. **`scripts/build-installer.sh:145` derives the jlink module set from
   `jdeps --print-module-deps`.** `jdk.accessibility` is a static-dependency
   invisible module and appears nowhere in that script — so **every shipped
   Windows MSI bundles a runtime with no Java Access Bridge, and NVDA/JAWS get
   nothing from an installed JLS.** §1 step 2's "NVDA documented" is
   unachievable on the primary Windows distribution until a one-line change
   lands. No open issue surfaced by search owns this.
2. **`src/jls/edit/Trace.java` is 626 lines of `MouseListener`/
   `MouseMotionListener` with no `setFocusable`, no key bindings, and no
   accessible wiring.** The trace window is where signal state over time
   actually lives; it is a WCAG 2.1.1 **Level A** failure and it is the natural,
   stock-Swing home for "spoken signal-state changes" — roughly two days, versus
   the live-region work PF-3 proposes.
3. **`CircuitRenderer`'s SVG export emits no `<title>`/`<desc>`.** PF-4's tactile
   SVG builds on this renderer; injecting a title and a pin/element inventory is
   a golden update (SVG is not a stability contract) that serves sighted docs
   too, and it is the first brick of the description renderer in Reframing 2.

## Duplication and pull-against

- PF-3 vs **#355 TASK-0029**: #355 already owns "an `AccessibleContext` over the
  editor canvas exposing each drawn element as an accessible child," and names
  the reach of that scene model as *the* decision that sets its cost. #544's
  "extends, does not re-own" is a label, not a boundary. Under Reframing 1 the
  conflict dissolves: #355 keeps the canvas tree (gated on demand), CAP-26 takes
  the outline view, and they stop bidding for the same work.
- PF-1's redundant encoding rides the same ~126 hardcoded-black call sites that
  #289/#381's dark variant must sweep. One sweep, two consumers — or two sweeps
  and a merge conflict.

## What to keep exactly as written

The no-test-no-claim rule (KC-26-3), the AC-5 falsification requirement, the
operability ratchet (PF-6), and the refusal to over-claim in prose are the best
parts of this issue and are what make it more honest than most accessibility
work anywhere. Keep them; move them onto a smaller, sooner, more portable
target.

## Suggested shape

Tier 0 (~3 weeks, unblocks everything): Access Bridge module + properties, the
contrast/caret fixes with the joint ΔE+contrast test, help `alt`/`lang` + an
accessibility help topic, SVG `<title>`/`<desc>`, PF-6's ratchet armed.
Tier 1 (~1 week): the accessible outline view over `OpSink` + the description
renderer; Trace made focusable and readable. Tier 2: prose narrative and tactile
SVG as sinks of the same renderer; CVD filter on the export path; the
conformance ledger with the ACR as its first report. Tier 3, gated on a real
user request: the full canvas JAAPI tree and live announcements.
