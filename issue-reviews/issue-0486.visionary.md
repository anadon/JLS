# Issue #486: FEAT-058: a net can say how long it is and a driver how fast it switches, and `jls -check` says whether the lumped model the design is simulated under is still valid
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machine block and the DAG bookkeeping and the claim is: **JLS should
be able to tell a student that the thing they drew as a wire is no longer a
wire.** That claim is correct, it is aligned with the project's whole arc, and
the roadmap has already conceded the principle in P4's own words — *"a violation
can be **reported** without being **modelled**, and reporting is 90% of the
teaching value"* (`docs/capability-roadmap/README.md` §P4). Invariant 4 ("this
feature reports; it does not re-model") is the same sentence applied to
interconnect. The out-of-scope list is disciplined, the refusal to derive length
from canvas pixels is right and well argued, and exposing `k` as a declared
parameter rather than hard-coding Johnson's divisor is the single best design
decision in the issue — it is the difference between teaching physics and
teaching one author's tolerance.

The evidence is also sound. I checked it rather than trusting it:
`src/jls/elem/WireNet.java:22-30` is exactly the five fields quoted, with no
length and no impedance; `src/jls/elem/Adder.java:33` is
`private static final int defaultPropDelay = 30;`;
`docs/simulation-semantics.md:26` is the dimensionless-time clause;
`docs/file-format.md:220-222` is the silent-ignore valve. Nothing here is
inflated.

So the goal is endorsed. What I am reframing is the **seam**, and with it the
dependency chain and the shape of the delivered artifact.

## Objection 1 — the feature as cut delivers a capability that is inert by construction

The Completion Criteria include, as a *deliverable*:

> `jls -check` over the shipped `examples/` corpus prints "not assessable" on
> every circuit and no other verdict

and §1 states the same for the whole corpus. The issue frames this as a
falsification guard. It is not one. A falsification guard is a test that *could*
fail and would tell you something if it did. "The feature is silent everywhere"
is a test that the feature is **dormant**, and it will keep passing forever,
because the two facts it needs — a per-net length and a per-driver edge rate —
must be typed in by hand, per net, by a student who has to already suspect the
answer before the tool will give it to them.

That inverts the stated audience. "The most common unexplained lab failure" is
unexplained precisely because the student does not know to look. A lint that
fires only after you declare the two quantities whose relationship *is the
lesson* is a lint for people who already passed the course.

## Objection 2 — the edge rate is being installed at the wrong seam, and the roadmap already names the right one

§2 rejects folding the transition time into P4's structured-delay work
(#87/#89/#93) because a full per-arc min:typ:max `DelayModel` is an order of
magnitude larger than a scalar. That rejection is correct — and it aims at the
wrong target. The `DelayModel` (roadmap Change **A**) is not where an edge rate
lives. Change **G** is:

> **Change G — A technology-library layer.** A `TechLibrary` object: a named,
> versioned mapping from (element kind, bit width, pin count) to a `DelayModel`
> … A circuit gains a `library` attribute. … JLS ships two built-ins:
> `jls_default` (today's constants, so nothing changes) and one
> datasheet-derived TTL-ish library for teaching.
> — `docs/capability-roadmap/sweep-02-timing.md:457-467`

The issue never mentions Change G. But every worked number in the issue is a
*part-family* number, not a per-driver number: 74AC → 2 ns, 74LS → 18 ns. Nobody
characterises an individual gate's slew on a schematic; they pick a family. Edge
rate is a **library datum with a circuit-wide default**, which is exactly the
object the roadmap already planned to build for #87/#89, and exactly the object
whose absence sweep-06 identifies as the real Tier-6 blocker (*"the reason is a
missing cell abstraction, not a missing parser"*).

Put the edge rate on a per-circuit `library` attribute with a per-element
override and three consequences follow at once: the student declares **one**
thing instead of N; the lint becomes assessable on every design that names a
family; and the attribute lands in the place #87, #89 and #93 will consume it,
instead of beside a scalar they will later have to migrate off. The issue's own
Re-planning Protocol worries about exactly this failure ("if the transition time
lands in a shape P4's eventual `DelayModel` cannot absorb as its degenerate
case…"). Landing it on the library seam removes the worry rather than monitoring
it.

## Objection 3 — all three `blocked_by` edges are softer than declared, and one contradicts the issue's own evidence

This matters because it is what prices a five-line formula at 3-6 maintainer-
weeks *behind* five multi-week predecessors (#367, #336, #319, plus #315 and
#334 transitively). The issue's whole self-justification is "first by permanence
**and** cheapest deliverable in the programme." As chained, it is neither
startable nor cheap.

- **#367 (physical time base).** Justified as *"the lint multiplies a time by a
  velocity; simulation time is dimensionless."* But the lint never touches
  simulation time. Invariant 4 forbids it: nothing here enters the event queue.
  `t_r` is a declared physical quantity parsed from a string (`50ps`), and
  `l_crit` is millimetres. A dimensionless `long now` is irrelevant to arithmetic
  that never reads `now`. The real dependency is only the *format bump*
  piggyback — a scheduling convenience, not a necessity.
- **#319 (per-section versioning / must-understand).** The issue's own Evidence
  item 4 argues the opposite of its own dependency: *"A dropped lint input is
  fail-open and harmless"*, which is precisely why `docs/file-format.md:220-222`'s
  existing silent-ignore valve is already the correct semantics for these two
  attributes. #319 is genuinely required for **#487's constraints**, and the
  issue says so. It is not required here.
- **#336 (stable net naming).** Real, but weaker than stated. `ElementId`
  permanent identity already **ships** (`src/jls/elem/Element.java:24`, persisted
  as the `sid` attribute). A rung-1 report can key on the driving element's
  stable id and pin — "the net driven by `CLK` of Register `sid=…`" — which is
  stable across save/load today. Pretty net names are a #487/#490 requirement.

If those three edges are relaxed to what they actually bear, this feature is
`blocked_by: []` and startable at HEAD. That is a large enough change to the
programme's shape that it should be tested rather than assumed.

## The reframing I would build instead

**Rung 1 becomes a design-check *surface*, and electrical length becomes its
first rule.**

1. **Build `jls -check` as a general design-rule-check engine, not as a delivery
   vehicle for one lint.** A headless `jls.check` package: a rule registry, per-
   rule severity, a machine-readable report, one exit-status contract. This is the
   same move `ElementRegistry` (#78) already made for elements and the same one
   `grand-architecture.md` §3 argues for generally, and it lands naturally on the
   headless-kernel side of #77. The batch/grading front end is co-equal in this
   project; an autograder that can say *"your submission has a combinational
   loop"* is worth more than one more waveform.
2. **Ship it with rules that fire on day one.** Combinational-loop detection —
   which the roadmap notes *"falls out free and which JLS today does not have at
   all"* — plus undriven/multi-driver nets (#98's territory) and fanout/loading
   (#365). Now `-check` is a capability on the shipped `examples/` corpus rather
   than a command that prints "not assessable" 200 times. The SI rule can be
   dormant without the *surface* being dormant.
3. **Declare the physics once, at circuit scope, not per net.** A circuit-level
   realization profile: part family (→ edge rate, via Change G's `TechLibrary`)
   and medium (→ propagation velocity: breadboard 0.7c, FR-4 stripline
   1.4457e8 m/s, internal). One dialog field each. This also dissolves Open
   Question 2 entirely — the velocity is declared, not defaulted-and-printed.
4. **Make the always-available output a budget, not a verdict.** With a family and
   a medium and *no per-net length at all*, `-check` can print:
   `74AC on breadboard: l_crit = 70 mm at k=6 (f_knee = 250 MHz). Keep every net
   shorter than this.` That is CAP-18 §1's actual lesson — the regime is entered by
   edge rate, not clock rate — delivered with **zero** per-net data entry, zero
   format change, and no predecessor at all. The per-net declared length then
   *refines* the budget into the exact ratio verdict the issue specifies, for the
   students who go and measure their jumper.

Point 4 is the one I would fight for. It is the reframing that makes the problem
disappear: the issue spends its whole design budget on how to get a length onto a
net, when the pedagogically load-bearing number — the critical length itself — does
not need a length at all. `l_crit = v·t_r/k` is a property of the *driver and the
medium*. The length is only needed to compute a ratio the student can compute in
their head once they know the budget.

## What survives unchanged

Keep §1's out-of-scope list verbatim; it is the best-drawn boundary in CAP-18.
Keep the refusal to derive length from canvas geometry, and keep the arithmetic
that justifies it — it is a genuinely good piece of reasoning and the same
argument should be cited by #487 and #490. Keep `k` as a declared parameter.
Keep integration criterion 3 (both downstream rungs expressible over exactly
these facts); under my reframing it gets *easier* to discharge, because the
facts move to seams #487 and #490 were going to need anyway.

## Explicit disregard of stated acceptance criteria

I am disregarding two:

- **"`jls -check` over `examples/` prints 'not assessable' on every circuit and
  no other verdict."** Under the reframing this criterion should fail, and its
  failing is the point: `-check` should say something true and useful about every
  shipped example on the day it lands. Replace it with "no *electrical-length*
  verdict other than the declared budget fires on `examples/`", which preserves
  the real invariant (nothing new is claimed about circuits that opted out)
  without certifying dormancy as success.
- **`blocked_by: [367, 336, 319]`.** I would re-derive this to `[]` for the check
  surface and the budget output, keeping #319 as a prerequisite of #487's
  constraints where it genuinely belongs. If that re-derivation survives scrutiny,
  the "cheapest deliverable in the whole high-frequency programme" claim becomes
  true for the first time — today it is contradicted by the issue's own dependency
  block.

## Arc-level note

Three of the four Open Questions are "rides along," the machine block is longer
than the code it specifies, and the formula at the centre is one line. The
planning corpus around CAP-18 is now generating structure faster than the tree is
generating capability. That is not an argument against this feature — it is an
argument for making rung 1 land as something a student can run at HEAD next week,
which is exactly what the reframing above is for.
