# Issue #880: FEAT-C25-0: the schematic-similarity premise is measured before it is funded — a 30-submission synthetic corpus with planted pairs separates from independent solutions, or CAP-25 stops
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#880 is a "premise gate" feature meant to cheaply test, before funding CAP-25's
14–21 mw programme, whether a position/name-invariant fingerprint can separate
3 planted-copy pairs from independent solutions in a 30-submission synthetic
corpus. The self-restraint (small scope, negative results count as a pass,
explicit boundary against PF-1–PF-6) is real and well-argued. But the issue
was filed today directly against the repo's own five-day-old product-direction
ruling, its load-bearing dependency is misattributed and doesn't exist yet in
any form, and its test design has a leakage problem that makes a "premise
validated" verdict close to foregone rather than a genuine measurement.

## Findings, most severe first

**1. Filed against the repo's own explicit "Defer" disposition and planning moratorium, on unverifiable authority.**
Issue #508 (2026-08-03, same OWNER account), the commissioned product-direction
review, places CAP-25 (#506) in **"Defer (priced backlog, free on the graph)"**
and states as a **process finding**: *"Planning ratchet: no new
tier:feature/tier:task until two capstones close."* #880 is a brand-new
`tier:feature` (label `tier:feature`) filed 2026-08-08, five days later, with
no evidence in #508 or #506 that two capstones have since closed. Its
justification — *"per the maintainer ruling of 2026-08-04 the capstone's
deferral is not grounds for leaving it without work"* — cites a ruling whose
exact text does not appear in either #506's or #508's comment history as
fetched; the closest artifacts are two 2026-08-04 "feature-coverage
verification" comments on #506, neither of which contains that sentence or
overrides the moratorium. An issue that reinterprets or overrides a
maintainer's five-day-old, explicitly reasoned scheduling decision needs to
quote that override verbatim and link it, not paraphrase it from memory.
**Recommendation:** before work starts, get an explicit, citable maintainer
comment confirming (a) the moratorium's two-capstones-closed condition is met
or waived, and (b) "Defer" plus "must have a filed child" are not in tension —
right now the issue resolves that tension unilaterally.

**2. The named dependency is misattributed, and the same thread admits it.**
AC-2 requires erasure to be "applied to the canonical representation the
semantic-diff lineage owns (**#356** and its task set...)," and the machine
block sets `ordering_after: [356]` only. But #356's own body places "the
uncompressed canonical text container and canonical ordering" **out of its
scope**, owned instead by **#334** (FEAT-003): *"This feature consumes
TASK-0005 from there; it does not own the container decision."* #506's own
2026-08-04 comments independently reach the same conclusion twice — *"the
canonical text/ordering substrate itself is owned by FEAT-003 #334 per #356's
own scope boundary"* and *"PF-1's filing must therefore record its edge
against both #334 and #356, not #356 alone."* #880 was filed after both of
those comments and still only orders after #356. **Recommendation:** add
`334` to `ordering_after` (or explain in the issue why #334 is not a blocker
for a 3-transform-class demo slice) before task #884 is picked up.

**3. The dependency named does not exist in the tree, and is itself blocked by two more open, unbuilt issues.**
`grep -rli "canonical\|winnow\|weisfeiler\|fingerprint\|plagiar" src/ test/ docs/`
returns zero files with a canonical-form/fingerprinting hit (checked directly
against this checkout). #356's own body confirms this at its evidence commit:
*"There is no validator,"* *"There is no merge machinery of any kind,"*
*"The `sref` item kind TASK-0005 introduces does not exist"* — and #356 is
itself `blocked_by: [319, 334]`, both open and, per the same ambiguities
review, unstarted. AC-2's phrasing — "the erasure layer is built on **the
existing** canonical form" — describes something that is not existing.
KC-25-0-2 does hedge for exactly this ("if the shared canonical form is not
reachable... stop"), which is good self-awareness, but as filed the almost
certain outcome of task #884 is an immediate KC-25-0-2 stop, discoverable by
reading #356 rather than by spending any of the "2-3 mw" budget. **This is a
feasibility/cost risk**, not just a wording nit: the issue's premise ("measure
before funding") only holds if the measurement can actually run.

**4. Self-undermining "measure it cheaply" framing.**
The Ordering section is explicit that CAP-25 deliberately does *not* order
behind CAP-06 (#300) so as to keep the premise test cheap: *"Ordering a
premise test behind a full grading capstone would defeat the point of
testing the premise cheaply."* Yet the same issue happily hard-orders behind
#356 — a feature that, per finding 3, is equally unbuilt and additionally
gated behind two more open issues. No argument is given for why #356 clears
the "cheap enough to depend on" bar that #300 does not. A demo slice built
directly against `Circuit`'s existing in-memory element/`WireNet` graph
(`src/jls/Circuit.java`, `src/jls/elem/`) — skipping the not-yet-existent
stable-id/canonical-text layer entirely — would have been at least as cheap
and would not import #356's own two open blockers into a "premise gate"
that's supposed to avoid exactly that kind of coupling.

**5. Test design leaks the answer: the planted transforms are chosen to be exactly what the fix targets.**
Open Question 2's recommended default plants copies via "moved components,
renamed wires, and inserted no-op buffers — the three cheapest to apply and
the three a text differ most obviously fails," while transform class 4
(subcircuit repackaging — the hardest case) is explicitly excluded because it
"most depends on PF-1's flattening policy." AC-2 requires the discriminator
to be built specifically as **position and name erasure**. Testing a
position/name-erasure tool against copies disguised only by moving
components and renaming wires is close to tautological: the fixture set was
picked, by the same people, to match what the fix already does by
construction. A "separation achieved" verdict from this design is weak
evidence toward the capstone's actual, harder premise (evading a determined,
disguising adversary across all six declared transform classes) — closer to a
unit test of the erasure code than to a measurement of the capstone's premise.
**Recommendation:** state this limitation explicitly in AC-4/AC-5's reporting,
or include at least one transform not targeted by position/name erasure
(e.g. no-op buffer insertion alone, isolated from the position/name changes)
as a genuine held-out case.

**6. n=3 planted pairs, one per transform class, with a binary verdict.**
AC-4/AC-5 ask for a distribution of "the 3 planted pairs" vs. the independent
pairs and a single up/down verdict (AC-5: "no third outcome"). With exactly
one instance per transform class there is no way to tell a transform that is
reliably caught from one that happened to be caught (or missed) once by
chance. The kill-criterion language ("recall and null overlap irreducibly")
implies a statistical claim that three single-instance data points cannot
actually support. Nothing in AC-4/AC-5 requires this caveat to be stated
in the written verdict that lands on #506 and gates PF-3 funding — the binary
form is gameable by omission: a technically "legible pass" of AC-5 could read
as more conclusive than three data points justify.

**7. Corpus generation is unspecified and left to whoever picks up #883.**
Open Question 1 ("what generates the independent solutions?") and Open
Question 2 ("which three transform classes?") are both marked "recommended
default... rides along" rather than resolved in the issue, and "one small
assignment" is never named or bounded (no gate count, no complexity floor).
KC-25-0-1 exists precisely because a too-small/degenerate assignment produces
a meaningless null model — but the choice of assignment, which directly
controls whether that failure mode triggers, is deferred to task #883's
implementer with no acceptance-side constraint on it. Two different
implementers could reasonably pick assignments an order of magnitude apart in
solution-space size and both technically satisfy AC-1.

**8. KC-25-0-1's escape hatch is itself untestable.**
If the corpus is judged too small, the issue requires the finding to "state
the smallest scale at which the measurement *would* be meaningful," while
explicitly forbidding scaling up the corpus to check that claim ("Do not
silently grow the corpus to 300"). Nothing in AC-4 or KC-25-0-1 requires that
number to be derived from any method (power analysis, extrapolation, etc.)
— as written it can be an unsupported guess and still constitute a "legible
pass" of the feature. This is the one acceptance criterion in the issue that
cannot be checked against evidence produced inside the issue's own boundary.

**9. AC-5's verdict feeds a future filing that the moratorium (finding 1) would then block.**
If separation is achieved, CAP-25's own re-planning protocol (§5 in #506)
says to file PF-1...PF-6 (14–21 mw) next. #880 doesn't address how that
filing squares with #508's still-presumably-live "no new tier:feature/task
until two capstones close" ratchet — the same tension flagged in finding 1,
just deferred one step further down the chain instead of resolved.

## What's solid (one line each)

- AC-3 (byte-identical determinism across two runs) is concrete and
  mechanically checkable — no notes.
- AC-6 (synthetic fixtures only, no verdict vocabulary, no judgment about a
  person) is a sound, low-risk privacy/ethics guard and is easy to verify
  against a score table + plot.
- The Boundary section's explicit exclusions (not PF-1/2/3/4/5/6) are unusually
  disciplined scope-limiting and make the issue easy to hold to its stated size.
- Treating "separation not achieved" as a legitimate, complete discharge
  (rather than a failure to be rerun until it looks better) is good
  methodological hygiene, in principle — undercut in practice by findings 5–8
  above, which make a false-positive "achieved" verdict more likely than the
  framing suggests.
