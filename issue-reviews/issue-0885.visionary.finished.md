# Issue #885: TASK-C880-3: the separation is reported as two distributions and their overlap, and KC-25-1 gets its written answer on #506
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#885 is filed as the third of three tasks under #880, but it is not a
measurement task. #884 produces every number it reports. #885's real product
is a **decision-recording device**: it converts CAP-25's KC-25-1 from a
sentence in a deferred capstone body into an obligation someone must discharge
in writing before 4–6 mw of research work (PF-3) can be funded. The "Why the
verdict is a comment and not a document" section says so plainly, and it is
correct: the report is evidence, the comment is the decision.

That instinct is well aligned with JLS's grain. `ARCHITECTURE.md` already runs
a "Recorded decisions" section — Internationalization: non-goal;
Plugin mechanism: removed (5.0.0, #80); Simulation execution strategy:
discrete-event interpreter is the sole strategy — and
`docs/grand-architecture.md` §9 opens with "Firm boundaries are as
load-bearing as layers; re-proposing these re-litigates a settled decision."
This project already believes that a recorded negative is a first-class
artifact. #885 is that belief applied to a kill criterion. Endorsed on that
axis without reservation.

Four things pull against it, in increasing order of how much I think they
matter.

## 1. The binary verdict has a hole that funds PF-3 off a vacuous measurement

AC-5 permits exactly two forms and forbids a hedge. AC-4 simultaneously
requires that a degenerate null — "the independent solutions too few and too
alike to overlap at all" (#880 KC-25-0-1) — be reported as the finding. These
two criteria collide, and the collision fails open in the dangerous direction.

If the 27 independent solutions collapse to a handful of isomorphism classes,
every independent pair scores at or near ceiling, the three planted pairs also
score at ceiling, and the intervals *do not intersect only because there is
almost nothing in the independent distribution to intersect with*. Under AC-5
as written the honest executor is forced to pick "separation achieved at scale
30, PF-3 may be funded" — because it is literally true — and 4–6 mw gets
funded off a measurement that measured nothing. The prose of AC-4 catches this;
the verdict grammar of AC-5 does not, and AC-5 is the criterion that gates the
money.

The honest outcome space at n=3 is three-valued, not two: separates / does not
separate / **underpowered — the measurement did not run**. #880 already names
the third case; #885 should permit its own third verdict form, worded so it
does *not* authorize PF-3:

> "measurement underpowered at scale 30 — null side degenerate, KC-25-1
> neither fires nor clears; PF-3 stays unfunded, and the smallest scale at
> which the measurement would be meaningful is N."

This is not a hedge. A hedge is a fourth thing: "probably separates." An
explicit "we did not learn the answer, and here is why" is the strongest
possible form of the evidence-not-verdict discipline #885 correctly imposes on
students in AC-6 — turned on the project's own decisions, where it belongs.
Also note that with three planted pairs the finest resolvable overlap fraction
is 1/3; AC-1's "overlap stated explicitly as a number" can only ever emit
0, 1/3, 2/3, or 1, and the report should say that rather than print a decimal
that implies resolution it does not have.

## 2. The pre-registration is the valuable part, and it is buried behind two blockers

AC-3 is the single highest-value criterion in this issue and it is the only one
that needs nothing built. Yet it lives in an issue `blocked_by: [884]`, which is
`blocked_by: [883]` and `ordering_after: [356]` — and #356 is `blocked_by`
#334 and #319, both open, its three tasks unfiled. I checked the tree:
`grep -rn "sref\|sprobe" src/ test/` returns nothing, so the stable-id
reference form TASK-0005/#436 introduces does not exist at HEAD; neither does
any canonical form an erasure layer could sit on. The realistic near-term
outcome of picking up #884 is its own stated discharge — "record exactly which
part is missing" — after which #885 never fires and KC-25-1 goes unanswered
indefinitely.

AC-3 asks for the separation criterion to be committed "in the same change as
#884's scoring code or earlier." Same-change commit order is weak evidence:
one rebase, one amend, and the ordering is whatever the author wants it to be.
A pre-registration committed **now**, weeks before the corpus in #883 exists
and long before any score does, is unfalsifiable evidence of the same claim,
and costs approximately nothing.

Concrete reframing: hoist AC-3 into its own commit landed with or before #883,
under something like `docs/similarity-preregistration.md`, and reduce #885's
AC-3 to "the pre-registration commit predates the corpus commit, verified by
`git log`." This makes the discipline stronger, unblocks the cheapest and most
durable part of the chain today, and leaves #885 as pure execution of a
criterion already fixed. It also means that if the chain stalls at #884 — the
likely case — the project still banked the part that mattered.

## 3. There is a cheaper measurement that could make #883 and #884 unnecessary

CAP-25's premise is not "JLS's canonicalizer can separate." It is "independent
correct solutions to a small assignment are structurally distinguishable from
copies of one another." That is a claim about **the combinatorics of the
assignment**, not about any canonical form. It can be attacked directly, and
much earlier.

For #883's recommended assignment (4-bit comparator or 2-bit ALU slice),
enumerate correct implementations within the shipped palette and count the
distinct graph-isomorphism classes under the position/name erasure relation —
by hand or with a throwaway script over a few dozen drawings. If a 4-bit
comparator has, say, four genuinely distinct shapes and everything else is a
relabeling of one of them, then **no fingerprint over any canonical form
separates at that assignment size**, KC-25-1's degenerate-null branch fires,
and it fires without a corpus, without an erasure layer, and without waiting on
#356. That is CAP-25's own §3 risk 2 ("there are only so many correct 4-gate
answers") stated as a measurement instead of a worry, at roughly 0.2 mw instead
of 2–3.

This is the reframing that can make the problem disappear. Run it as a
pre-flight before #883 is authored. Three outcomes: the class count is
degenerate (KC-25-1 fires now, chain closes, ~2.5 mw saved); the class count is
rich (the premise survives its cheapest test and #883 proceeds with a *chosen*
assignment size rather than a guessed one, which also answers #883's Open
Question 1 with data); or the enumeration itself is ambiguous, which is a
finding about the assignment and feeds #883 AC-1 directly. I would file this
ahead of #883 and let it gate the other two tasks. It does not replace #885 —
the verdict obligation still needs somewhere to land — but it may well be what
#885 ends up reporting.

## 4. The chain's most valuable output for JLS is not the separation number

CAP-25's Abstract names the real architectural dividend: "the canonical netlist
-graph form is the semantic-diff substrate (lf-06/P11, FEAT-012 #356 lineage)
gaining its **second consumer**, which is how a substrate proves it is one."
That is the sentence that connects this capstone to JLS's actual trajectory.
Schematic plagiarism detection is nowhere in `docs/grand-architecture.md` §2's
three latent futures (CPU teaching tool, FPGA bridge, collaborative editor);
CAP-25 is deferred under #508. What is on the critical path is the canonical
form itself, which #334, #356, #436, #409 and #491 all converge on and which
today has exactly one hypothetical consumer.

The highest-value artifact this three-task chain can produce, for JLS as a
project, is not a distribution overlap. It is a **design-pressure report from a
second consumer onto a substrate whose own tasks are not yet filed**: "here is
precisely which part of the canonical form cannot carry position/name erasure,
and here is what #436's permanent-id discipline would have to look like for it
to." Delivered *before* #356's tasks are written, that report is worth more
than the separation number and is deliverable even in the stall case. #884's
Ordering section already permits it as a discharge; it treats it as a
consolation prize.

#885 currently carries none of this. Its AC-5 verdict grammar has no form for
"the measurement could not be attempted because the substrate is not ready" —
which, given the state of #334/#356/#436 at HEAD, is the single most likely
thing that actually happens. That is a fourth permitted form, and it should
land on #506 with the same force as the other three, because it is equally a
decision: it re-sequences CAP-25 behind the diff lineage, which is exactly what
KC-25-2 prescribes.

## What I would keep exactly as written

AC-2 (a null result is a legible pass), AC-4's named antipattern (do not grow
the corpus to 300 to rescue a result), AC-6 (no verdict vocabulary, adopted
from the first commit rather than retrofitted), and the Boundary section's
refusal to derive any shippable threshold. Those four are the issue's spine and
they are right. The reframings above do not touch them; they extend the same
honesty to the cases the issue's own two-outcome grammar cannot express, and
they move the cheap, unblocked, durable work in front of the expensive blocked
work instead of behind it.
