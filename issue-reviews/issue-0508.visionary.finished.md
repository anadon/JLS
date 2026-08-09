# Issue #508: Product & direction review, August 2026: decisive verdict, capstone dispositions, and the wedge sequence
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its table, #508 makes one claim about what JLS should become: **not a
breadth-first CS→ECE→EE span, but an adopted classroom tool with a narrow defensible
identity** — deterministic grading, accessibility, and one spectacular demo — bought
with cheap slices instead of a 1,100-maintainer-week programme. That claim is correct,
and it is the most valuable document this project has produced. The diagnosis (zero
adoption here, live users on `bsiever/JLS`, arithmetic that cannot close against bus
factor 1) is exactly the thing a project like this normally refuses to write down.

I am not reviewing the disposition table row by row; per this lens I disregard the
per-capstone slice arithmetic as the operative artifact, because the operative artifact
turned out to be something else. What follows judges the *shape* of the plan.

## The thing the issue cannot see about itself

#508 diagnosed custody inversion — "planning prose belongs in `docs/` on master, not
30–60KB issue bodies" — and then, in its own first comment, inlined the entire report
into a GitHub comment "for survivability." Five days later, on this checkout:

- `docs/reviews/` **does not exist**. The report #508's body links to is still branch-only.
- `docs/plan/` **does not exist**. Queue item 0's second half is undone.
- `.github/ISSUE_TEMPLATE/` holds `capstone.md`, `feature.md`, `scientific_task.md` — no
  human-facing bug template. §5's own finding, unfixed.

In the same five days the tracker grew by 12 capstones, 77 features and 269 tasks to
~600 open issues, and 1,210 review agents were spawned over it (`issue-reviews/README.md`).
The ≈0 mw items the review called *highest leverage* are the ones that did not happen;
the expensive meta-work is the one that did. This is not hypocrisy, it is a mechanism:
**planning is the work agents can do unsupervised, so planning is the work that gets
done.** The review priced everything in maintainer-weeks but the only genuinely scarce
resource — maintainer attention — is spent by the planning machinery before any wedge
reaches it.

The 2026-08-04 withdrawal of the planning ratchet reasons that "suppressing the record
does not reduce the work." True of records. But Phase D did not record pre-existing
work; it *invented* 269 tasks, 3.7% of them self-admittedly improper, alongside 75
contradictions, 35 gaps and 140 adversarial findings. The tracker is not a map of a
large programme. It is a generator of one. The maintainer's ruling should stand — count
is the wrong instrument — but the brake has to go somewhere, and the right place is
proposed below.

## Reframing 1 — send the diff, not the audit

The review's #1 item is "contact Bill Siever." As filed it became #509 (fork-delta
audit), #571 (re-engage PR authors), #577 (corpus fixtures) — and a prose/machine-block
deadlock that made the highest-leverage item in the whole plan unstartable until an
ordering agent found it four days later.

The goal is not a conversation. The goal is that **the bytes WashU students download are
this repository's bytes.** The shortest path is not an audit document; it is a pull
request to `bsiever/JLS` carrying the things that fork does not have and cannot easily
get — reproducible jar, signed installers on five platforms, the `-t`/VCD stability
contract, the headless ratchets. Run the delta audit *as a PR*, not as a deliverable
that precedes one. An unsolicited PR that makes someone's course installers better is a
stronger opening than any email, and it is the only version of item 1 whose failure mode
is informative.

Corollary the plan should absorb: if the merge lands, CAP-34 distribution, CAP-27 shop
window and CAP-33 course kit are largely *downstream of whose URL is on the course page*
and should not be funded ahead of that answer. Three capstones are contingent on one PR.

## Reframing 2 — one artifact, not four wedges

Wedges 3–7 are sequenced as parallel slices across CAP-06/09/21/23/24/33, each with its
own band, gate and shared-UX edge. An instructor does not evaluate slices. Ship instead
**one reference lab**: a single published assignment (the shift-register or an ALU),
with a grading spec that is provably complete, a counterexample replay, a Gradescope
container, a chronogram/figure output, and one command that runs the whole thing from a
clean clone. That single deliverable *is* the CAP-06 verdict slice, the CAP-09 floor,
the CAP-21 kit, a CAP-24 figure and a CAP-33 lab — but as a vertical artifact whose
parts cannot be independently deferred, and which an instructor can judge in ten
minutes. It collapses queue rows 2, 6, 6b and 7 into one thing with one owner.

## Reframing 3 — the oracle is already in the build

The review's most damning technical finding is real and I verified it:
`examples/autograde/autograde.py` grades **three hardcoded final values from one input
vector** (`181 >> 2`, lines 44–56). The shipped grading story is a single-vector spot
check; a submission wrong on 255 of 256 inputs passes.

The plan's answer is CAP-09's "combinational equivalence with replayable
counterexamples" (8–11 mw) plus a co-designed exit-status lattice. There is a cheaper
seam nobody named. JLS already exports structural Verilog (`-export out.v`), and CI
already installs `iverilog` and GHDL for exactly this family of tests (README, "Optional
development tools"). A **differential oracle** — exhaustive or randomised vectors driven
through both JLS and `iverilog` on JLS's own export, diffed, first mismatch minimised
and replayed as a `-t` file — gives counterexample-grade verdicts for combinational
circuits without writing an equivalence checker at all, and doubles as the strongest
possible regression net for the engine work (#484). It also converts the Verilog
exporter from "deployment bridge" into load-bearing infrastructure, which is a better
answer to "is plain Verilog export table stakes?" than the landscape section gives.

## Reframing 4 — delete the ordering graph

The round-2 comment is the clearest evidence in the thread: a cycle on queue item 0
invisible to every prior pass because one half was in a machine block and the other in
prose; features ordered behind capstones (which close last by definition); a 0.5 mw task
sitting behind a Swing refactor; stale `part_of_feature` fields that a field search finds
and a comment thread contradicts; an owed body-reconciliation pass the program "could not
do to itself." None of these are mistakes by the agents involved. They are the
predictable output of encoding a dependency graph across 600 mutable bodies with no
validator.

A plan whose ordering constraints are discoverable only by re-reading the corpus is not a
plan; it is a corpus. The radical simplification: **drop `ordering_after`/`blocked_by`
from issue bodies entirely and keep one ordered list in one file on master**
(`docs/plan/QUEUE.md`). A list is acyclic by construction, diffable, greppable, reviewable
in a PR, and has exactly one writer per change. It makes ordering-coherence rounds,
tier-illegal edges, stale-field drift and the body-reconciliation debt *permanently
disappear* rather than scheduling their repair. This is also the honest form of the
maintainer's ruling: keep every issue, impose zero limit on count, and stop asking the
issue bodies to be a database they cannot be.

And the matching brake, since count is off the table: **one open milestone at a time.**
Milestones are currently at zero use despite 34 milestone-scale outcomes. WIP on
execution is already the review's own recommendation; a single open milestone is that
recommendation in a form GitHub enforces for free.

## Where this pulls with the project's arc, and where against

With: the engineering culture here — ratchets, mutation gates, reproducible builds, a
*documented* batch stability contract, `HeadlessCoreRatchetTest` — is genuinely
category-leading, and ARCHITECTURE.md is honest in a way most projects never manage
("Adding an element today (the honest list)... roughly sixteen places"). That culture is
the asset the adoption thesis rests on, and every wedge above rides it.

Against: #508 is itself an instance of the defect class it names — an untiered
documentation issue with no closure criterion, now the anchor for five comments and a
340-issue restructuring program. It should be closed once the report lands in
`docs/reviews/` on master, with the queue living in `docs/plan/QUEUE.md` and this issue
number cited from there. A strategy document that lives in a comment thread it cannot
close is the custody inversion, one level up.

## What I would fund on Monday

1. The `bsiever/JLS` PR (not the audit). Days, not weeks.
2. `docs/reviews/` + `docs/plan/QUEUE.md` on master; close #508. ~1 mw, unblocks nothing
   and preserves everything.
3. The differential oracle over the existing Verilog export + `iverilog`, and the one
   reference lab built on it.
4. Nothing else until (1) has an answer.

Endorse the verdict. Reframe the plan from a graph into a list, the wedges into one
artifact, the outreach into a patch, and the equivalence engine into a toolchain diff.
