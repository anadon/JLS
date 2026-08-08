# Issue #751: TASK-C575-4: every lab is completed by a non-author inside its stated time budget, and a lab that fails two reviews is pulled rather than padded
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

CAP-33 (#517) bets that an instructor adopts a course, not a tool. That bet is
decided by trust in content the instructor has not yet taught. #575 AC-1 gives a
number — eight labs — and a number is exactly the kind of claim that decays into
a metric. #751 is the only thing in the whole feature standing between "eight
labs" and "eight directories." The instinct is right and the issue deserves to
exist. Everything below is about the route, not the destination.

## The mechanism depends on a resource this project structurally does not have

ARCHITECTURE.md records JLS as "a single-maintainer pedagogy tool" (the i18n
non-goal); SECURITY.md's key-custody rationale says the same. AC-2 — "Every
shipped lab has at least one recorded non-author completion within its stated
time budget" — therefore requires recruiting eight-plus volunteers for 45–90
minutes each, and it sits on the critical path of the pack's release. The
`band_mw: 0.5-1` prices the maintainer's work of writing a protocol and filing
records; it does not price the thing the AC actually consumes, which is other
people's unpaid attention. "Two consecutive reviews" further implies a cadence,
so this is a perpetual levy, not a one-time cost.

A gate whose satisfaction lies outside the project's control, placed in front of
the deliverable the capstone exists to produce, has two stable outcomes: it is
quietly dropped, or it is satisfied nominally (the maintainer's colleague clicks
through, 40 minutes, pass). Both leave KC-33-2 unenforced while looking enforced,
which is worse than not having the gate.

## AC-3 and AC-4 contradict each other

AC-3: a failing lab is pulled, "the removal is recorded rather than the budget
being quietly raised." AC-4: "Time budgets in the labs are updated to the
observed values where the review shows them wrong." A review that overruns is
simultaneously an instruction to pull the lab and an instruction to raise the
budget, with no rule saying which fires. That is not a drafting slip; it is the
symptom of asking one number (duration) to be both a measurement and a quality
verdict. Split those two roles and the contradiction disappears.

## Reframe 1 — the deliverable should be an independent build, not a stopwatch reading

Replace "a recorded completion time" with "the reviewer's own circuit, committed
to the tree." Per lab: `reference.jls`, `solution-b.jls` (built by a non-author
from the prose alone), and a small `review.yaml` (reviewer, date, minutes
observed, count of questions the reviewer had to ask the author, verdict).

This is strictly stronger given how JLS actually grades:

- `docs/batch-interface.md` §2.2: `-t` drives *top-level* `InputPin`s matched by
  exact name. §3.2: stdout prints only watched `Register`, `Memory`, `OutputPin`,
  in name order. A lab's grading vectors are therefore coupled to the reference
  solution's pin names, widths, and — wherever an internal register is watched —
  its topology. Vectors authored against one's own circuit silently encode that
  circuit. TASK-C575-1's CI lane cannot see this: the reference-green and
  planted-defect-red variants are both derived from the same drawing. A second,
  independently built implementation is the only thing that exposes it, and it
  exposes it mechanically, on every CI run, forever.
- The defect class it catches is the real failure mode of lab prose —
  underspecification (which pin names? what width? enable active-high?) — not
  duration. Ambiguity is what makes a lab unusable by a non-author; slowness is
  usually just a wrong estimate.
- The artifact is auditable and durable. "62 minutes" is unfalsifiable and rots
  the moment the lab is edited. `solution-b.jls` is re-graded by CI after every
  format or semantics change.
- It is already the project's own arc. `docs/capability-roadmap/lf-04-formal-and-grading.md`
  argues at length that vector grading is weak and that equivalence against a
  reference is the direction ("grading against a bug" when the reference itself
  is defective). A second independent implementation per lab is precisely the
  second input an equivalence-based grader will want, and in the meantime it is a
  usable differential oracle: grade `solution-b` with the lab's vectors, and
  grade `reference` against `solution-b`'s watch set.

The reviewer's effort is nearly unchanged — they were going to build the circuit
anyway; the marginal cost is committing the file they built.

## Reframe 2 — the binary gate is unaided completability; time is data

Failure = the reviewer could not produce a vector-passing circuit from the prose
alone without asking the author a question. Questions are counted; any non-zero
count is a prose defect to fix, and two consecutive unaided-completion failures
pull the lab. Duration is recorded and the declared budget is *always* set to the
observed value, unconditionally — AC-4 becomes consistent and AC-3's
contradiction evaporates.

This also removes a perverse incentive the issue creates. Under "over budget =
failure," the cheapest defence is a generously padded budget. KC-33-2 wanted to
stop padded counts; as drafted, #751 moves the padding one field over into the
budget, where it is less visible.

## Reframe 3 — the seam is wrong: this belongs to the kit convention (#578)

#578 specifies kit layout (AC-1), ships a validator CI runs over *every* kit
(AC-2), and defines kit content licensing (AC-4). The review record is a kit part
exactly like schedule and rubric. Put the slot in #578's schema and the check
("every lab carries a review record; report missing or stale ones by name") in
#578's validator; #751 then shrinks to writing the protocol and populating the
slot for the Donzellini pack.

Payoffs: third-party kit authors inherit the honesty mechanism, where today they
inherit nothing — and franchising the convention is the entire point of #578.
The AC's "with the recorded time in tree" acquires a defined location instead of
being a per-pack invention. #577's CSE 260M corpus and #552's lesson content can
use the same slot. The cost is an ordering change: #751 should come after #578,
not merely after #748.

## Reframe 4 — the reference customer is the measuring instrument

#509 (CSE 260M) and #578 AC-5's named external instructor bring something no
recruited reviewer can: a cohort of students who will do these labs anyway. One
term produces a per-lab distribution of completion times, which is what a "time
budget" honestly is — a single reviewer's stopwatch is a sample of size one
presented as a promise. Ship budgets marked provisional with a one-row-per-run
`timings.tsv` anyone can append to by PR, so the honest number accumulates in
public rather than being gated on before ship. Reframe 1's independent build stays
the pre-ship gate (it tests sufficiency of the prose, which must be right before
students see it); cohort data is the post-ship correcting mechanism. This inverts
the issue's dependency: it stops blocking release on volunteer labor and starts
harvesting the labor that adoption itself generates.

## What I am disregarding, and why

AC-2 as written — a recorded non-author completion *within budget* for every lab,
as a precondition for shipping. I am disregarding "within budget" as the pass
condition (it measures the estimate, not the lab) and the framing of the record as
a time rather than an artifact. The stronger version of the same intent is: no lab
ships until a non-author has independently built a circuit that passes its
vectors, unaided, and that circuit is in the tree. AC-4 I would make unconditional
rather than conditional on failure.

## What should be kept exactly as written

KC-33-2's intent and its uncompromising form — removal is recorded, counts are not
padded. The "recorded, not quietly changed" discipline is the project's house style
(the recorded-decisions section of ARCHITECTURE.md, the revisit triggers) and it
belongs here. The boundary against #578 AC-5 is correctly drawn: reviewing the
*convention* is not reviewing the *labs*.

## Risks of my own proposal

Two solution circuits per lab is two artifacts to migrate when the save format or
simulation semantics change; the existing round-trip and golden suites make that a
CI signal rather than a surprise, and a stale `solution-b` failing loudly is the
desired behavior. Where a starter circuit is nearly complete, `solution-b` may be
near-identical to the reference and prove little — say so per lab and drop the
requirement explicitly, recorded, rather than letting it become ritual.
