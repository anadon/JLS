# Issue #313: CAP-18: a drawn net that is electrically long says so, shows its reflections, and exports a constraint a real board tool enforces
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the machine block, the claim is: **JLS should be able to state the
boundary of its own model, and should be able to hand the consequence of that
boundary to a tool that can act on it.** Everything else here — the lattice
voltages, the `.kicad_dru`, the 51-89 mw — is instrumentation for that one idea.
It is the strongest kind of capstone, because a teaching tool that cannot say
"the thing I just showed you is not true above here" is teaching a lie with good
production values. The 150 mm jumper at 2.1x critical length for a 74AC part is
the most defensible sentence in the whole programme.

That claim is also, on the evidence, *already invited by the project's own
roadmap*. `docs/capability-roadmap/README.md` §6(a) excludes PCB (#140–#146) as
"KiCad's domain," and then records the near-miss verbatim: "IPC-D-356A is a
netlist format and JLS has a netlist, so the emitter would be a printer. It
still stays out because **a bare-board test netlist without a board layout has
no consumer**." CAP-18's step 5 supplies exactly that missing consumer. This is
not a capstone pushing against the exclusion; it is the capstone that satisfies
the exclusion's own release condition. Worth saying plainly, because the issue
never makes this argument and it is the best one available to it.

## Where it strengthens the arc, and where it pulls against it

Two of the three rungs sit cleanly on the trajectory `docs/grand-architecture.md`
draws — "orchestrate external tools, never reimplement," one enforced-headless
core, printers over an IR. FEAT-060's `.kicad_dru` writer is the same shape as
`src/jls/hdl/board/PcfEmitter.java` (data-not-code, shipped, #213) and lands on
an already-typed seam: `docs/extension-points.md:32` catalogues `hdl.exporter`
(`jls.hdl.HdlEmitter`) as typed now. Nothing architectural is being invented.

The third rung is where JLS's identity is genuinely at stake, and the issue
knows it — K18-4, AC-6, §3 risk 6 are all guardrails around the same fact. But
the guardrails treat the drawn element as the destination and the headless form
as the *degraded* outcome. I think that is backwards, and it is the reframing
this review exists to propose.

## Reframing 1 — the lint is not a signal-integrity feature; it is JLS's missing check surface

FEAT-058 is scoped as "the SI lint," priced as "private to this capstone"
(3-6 mw of an 11-20 mw marginal band), and its report is invented as
`jls -check design.jls`. **That flag does not exist.** `JLSStart.FLAGS`
(`src/jls/JLSStart.java:759-787`) is `h b i s t d p v r vcd export board pins
savetext` — there is no check mode at all, and `docs/extension-points.md:28-36`
catalogues seven seams, none of which is a design-rule seam. So FEAT-058 is not
one lint: it is a batch-mode verb, a rule-result vocabulary, and a report format,
with a signal-integrity rule as its first tenant.

Cutting along that seam instead changes what the capstone buys. The
"not assessable" verdict of AC-1/AC-7 is not an SI concept — it is the correct
general semantics for *any* rule whose inputs are optional, and it wants to be
defined once (`PASS | FAIL | NOT-ASSESSABLE`, with vacuity a first-class result)
rather than re-litigated per lint. The tenants are already queued elsewhere in
the corpus: FEAT-041's electrical-loading checks (the DC sibling this issue names
itself), FEAT-042's manufacturability gate, unconnected inputs, width mismatch,
the fanout awareness `docs/capability-roadmap/sweep-02-timing.md:110` records as
absent. Under this framing the electrical-length rule is a few dozen lines on a
shared surface, the surface is funded once by whichever capstone gets there
first, and the 3-6 mw "private" row splits into a spine row and a genuinely
private rule.

**A duplication the issue takes credit for without recording:** it prices
FEAT-058 at 3-6 rather than 8-9 on the grounds that the edge-rate half "is
already owed by the SDF, Liberty and SDC rows of that same roadmap sweep." That
is right — P4's `DelayModel` (`docs/capability-roadmap/README.md:391+`) owes
rise/fall per arc and physical time units, and the Liberty row owes slew
explicitly. But the discount is taken while `blocked_by` is `[315, 334, 337,
349, 365]` — registry totality, canonical text, headless mutation, package
library, packing — and `requires_features` carries FEAT-047 (time *base*), not
whatever feature owns the *delay model*. So the edge-rate attribute currently has
no recorded owner on the P4 side. If FEAT-058 mints a transition time privately,
JLS acquires two edge-rate concepts and a merge; if it lands as P4's slew, the
discount is real and the seam is right. Either the dependency is recorded or the
discount is not earned.

## Reframing 2 — the closed-form line should be a headless kernel; the drawn element should leave the required set

§3 risk 1 states the problem exactly: the element is the cheapest rung and the
only one that commits permanent public surface — a frozen save tag, a mandatory
palette row under a totality test I confirmed still green at HEAD
(`test/jls/edit/PaletteContractTest.java:44-45,61-65`; 35 registered types
against 32 palette entries), a K9 obligation. The issue's answer is to *sequence*
that risk (permanence ordering) and to hold `K18-4: stop at the headless CSV
form` as the failure mode.

**I would make the headless form the goal rather than the fallback.** The
closed-form superposition is a pure function — a source ramp, two reflection
coefficients, one delay, `V_k = V_final·(1 − Γ_s^k)`. It has an exact internal
oracle at 1e-12, it needs no element, no dialog, no renderer, no palette row, no
format version, and — decisively — **no real-valued trace row**. §3 risk 4 prices
that row at 0.5-1 mw and calls it "an amendment to the recorded 'GTKWave is the
analog view, zero JLS GUI code' position." It is much more than that. The
two-states-plus-HiZ value domain is normative in `docs/simulation-semantics.md`
§2 and pinned by `VcdExportGoldenTest`, and `ARCHITECTURE.md`'s recorded #221
decision binds *any* future execution strategy to bit-for-bit agreement with that
domain. 0.5-1 mw is very likely the most under-priced number in the issue
relative to its architectural consequence.

The pedagogy also does not need the widget, and is arguably better without it.
A student who drops a transmission-line element and watches it ring has learned
to operate a component. A student who runs a report against **the net they
already drew** and reads "this net is 41x electrically long; here is the far-end
waveform your ideal wire is hiding from you" has learned that their existing
drawing was wrong — which is the actual lesson, and it is the one the drawn
element dilutes by making the regime an opt-in part you have to know to pick.
The issue already chose exactly this shape for the eye diagram ("emit the eye
density matrix, bathtub table, TIE record as goldened CSV, zero GUI"). The same
reasoning applies one rung down and it does not apply it.

Concretely: keep the kernel, the four-termination corpus, AC-2 and AC-3 as a
headless report and goldened CSV inside the check surface of reframing 1. Move
the drawn `TLine` element out of `requires_features` entirely and behind a
#212-style demand gate, where a first-year's palette is protected by *absence*
rather than by a context-derived visibility rule that K18-4 has to police.

## Reframing 3 — the physics has no independent oracle, and there is a cheap one the issue never considers

AC-2 admits its own limit: since the implementation *is* the closed form, the
1e-12 assertion "catches transcription, regeneration and floating-point-order
errors, not modelling errors." AC-5's KiCad DRC is named as the independent
check, but it adjudicates a *length rule*, not a waveform. So nothing outside
JLS ever checks the physics — in a capstone whose entire premise is that JLS
should stop asserting things it cannot back.

The out-of-scope table rules out ngspice twice: as a **CI tolerance oracle above
1 GHz** (correct, with the maintainers' own quote) and as **source to absorb**
(`TXL`/`CPL` quantise time to integer picoseconds). It never considers the third
option: **emit a deck.** JLS already emits Verilog, VCD, PCF and — under
FEAT-042 — KiCad netlists; a printer that writes the exact `T` device the issue's
own measurement validates to −0.005% is the same class of artifact. Run it under
the shipped skip-when-absent idiom (`ToolLocator.findOnPath` +
`Assumptions.assumeTrue`, the pattern `IverilogCompileTest` and AC-5 already
use), at 74AC/breadboard edge rates where ngspice's frequency-independence
limits are nowhere near binding — which is precisely the CAP-04 case that
motivates the capstone. That is a real differential oracle for the model, for
roughly the cost of a printer, and it strengthens the one claim the capstone
currently cannot defend. It is a complement to the in-jar kernel, not a
replacement: the single-self-contained-jar constraint (`grand-architecture.md`
§1) means the lesson must not require an install, but the *test* may.

I would also invert the last two rungs. Sequencing the externally-contingent
rung (FEAT-060, whose headline claim Open Question 3 already concedes is
partly undeliverable — KiCad's `NETCLASS` has no impedance field) ahead of the
one with an exact internal oracle front-loads the risk into the middle of the
chain. Under reframings 1 and 2 the kernel carries no permanence cost, so the
original reason for putting it last evaporates: **check surface + length rule →
closed-form kernel and report → constraint export.**

## What I am explicitly disregarding, and why

- **AC-6 (`SiPaletteVisibilityTest`) and K18-4.** If no element is registered,
  there is no palette row to suppress and no visibility rule to police. The
  pedagogy floor is held by absence, which is stronger than by a test. AC-6
  becomes vacuous rather than failed.
- **§3 risk 4's real-valued trace row, and its 0.5-1 mw.** Out of scope under
  reframing 2, and I would not fund it here at any price — it is a value-domain
  change that belongs to the analog capstones (#303/#305) with their own
  argument, not smuggled in as a prerequisite of showing one waveform.
- **The DoD line "the permanence ordering … either followed or its departure
  REPLANned with the §3 risk 1 argument answered."** I am proposing a departure,
  and the answer to risk 1 is not a different order — it is that under reframing
  2 rung 3 commits no permanent surface at all, so the argument no longer has a
  subject.

## What I would not touch

The falsification guard is exemplary and I verified its live half: 35 registered
types against 32 palette entries at HEAD, `WireNet`'s field set with no length or
impedance, `simulation-semantics.md` §1's dimensionless time. The permanence
instinct behind the ordering is right even though I would re-order. The
out-of-scope table — BER by brute force, femtosecond stepping, `WireNet` as a
distributed net kind, drawn pixel length as physical, IBIS-AMI binaries, 3-D
full-wave — is the best example of "refuse the method, keep the capability" in
this corpus, and the `WireNet.java:405` cardinality argument (one value per net
against 6.9 in flight) is the single cleanest piece of reasoning in the issue.
Note that `docs/plan/` and commit `2d0ca9d` are not present in this checkout, so
the feature-document and REGISTRY claims of the comment thread were not
independently verifiable here; every claim I did check against `src/`, `test/`
and `docs/` held.

## Verdict

**endorse-with-reframing.** The goal is right and the project is already leaning
toward it. Two of three rungs are cut along the wrong seam: the lint should be
the first tenant of a general `-check` design-rule surface with its edge-rate
attribute landing as P4's slew, and the transmission line should be a headless
closed-form kernel with the drawn element demoted out of the required set and
behind a demand gate. Reordered, the capstone delivers its whole claim without
committing a single byte of permanent public surface, which is the outcome its
own §3 risk 1 says it wants.
