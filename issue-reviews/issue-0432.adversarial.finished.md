# Issue #432: TASK-0095: the path from a drawn design to a submitted shuttle entry is written down, self-tested with no external tools, and walked once with the record filled in
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-grounded where it can be checked against the live tree —
the iCEstick precedent it copies (`docs/icestick-bitstream-handoff.md`,
`scripts/icestick-handoff.sh`, `scripts/icestick-handoff-selftest.sh`,
`PcfEmitter.java`) matches the quoted text and line ranges exactly, the six
`_TBD_` cells at `docs/icestick-bitstream-handoff.md:119` are real, the
CI lane it wants to join (`.github/workflows/ci.yml:56`) is real, and the
parent feature (#328, open) and #264 (open) corroborate the dependency
story: TASK-0094 really is unfiled, `blocked_by` on #328 really does list
327/339/359. That said, the issue has one self-inflicted process defect
(an untraceable evidence commit), one real internal contradiction between
its own acceptance-test design and its own "green build" completion
criterion, and a feasibility gap around treating a real silicon-shuttle
submission as a two-week, schedulable line item.

## Findings, most severe first

**1. (High) The evidence commit the entire citation apparatus is pinned to does not exist in this repository.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and the D8
commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc` both fail to resolve:
`git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` → `fatal: bad
object`, same for the D8 hash. Compare with sibling issue #264, whose
`evidence_commit: 29afb26` *does* resolve (`git cat-file -t 29afb26` →
`commit`). The issue explicitly leans on this pinning for rigor — "Re-derived
at `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`; `docs/`, `scripts/` and
`src/` are byte-identical between that commit and the current
default-branch tip" — but that claim is unfalsifiable if the commit
cannot be located at all (not merely "not on this branch" — genuinely
absent from `git log --all`, 269 commits). The individually-quoted
snippets (PcfEmitter.java L14-22, icestick-handoff.sh L1-12,
icestick-bitstream-handoff.md L113-119) happen to still match HEAD, so
the content claims survive, but the verification mechanism the issue
insists on ("citations re-derived if HEAD had moved", rule 6 in the DoD)
cannot itself be exercised by a reviewer. **Recommendation:** replace the
evidence commit with one that actually resolves (e.g. `29afb26` or HEAD at
filing time) before this issue is picked up; an unresolvable commit invites
an implementer to silently substitute their own base and never notice a
discrepancy.

**2. (High) The acceptance test's intentional "stay red" state conflicts with the DoD's "mvn verify green" requirement, and the no-window fallback is underspecified.**
§5 P1 and §14 both say, correctly per H4, that
`theSubmissionRecordHasNoTbdCells()` "remains red until the walk,
deliberately." But §14 also lists as an unconditional checkbox: "`mvn
verify` green (tests + SpotBugs, warnings-as-errors)". A JUnit test that
is red fails `mvn verify` by definition — there is no described middle
state. The issue's own escape hatch (Open Question 1) is to leave the
test "failing with a `WAIVED:` comment" if no shuttle window is open —
but that is *also* not green. The issue never says how the failing test
and a green build coexist: is the test `@Disabled` with the WAIVED
comment (in which case H4's stated refutation condition — "the test
being satisfiable without a real submission" — is trivially met, because
a disabled test doesn't need to be "satisfied," it's just skipped, and
CI reports success either way)? Or does `mvn verify` actually stay red
and the DoD checkbox go unchecked indefinitely? Both readings are
internally consistent with *some* of the text and contradict the rest.
**Recommendation:** the issue needs to pick one mechanism explicitly — e.g.
"the WAIVED path uses `@Disabled(\"WAIVED: #<successor issue>\")`, and
`mvn verify` green in that state is the documented, accepted meaning of
'DoD item N is waived'" — and say so in §7.11/§14, not leave it inferable.
As written, an implementer can satisfy "mvn verify green" by disabling
the one test the entire issue exists to make un-gameable (H4's own stated
failure mode), and the issue's text does not clearly forbid that.

**3. (Medium) `blocked_by: []` is a documented misdirection for anything that reads only the machine block.**
The YAML says `blocked_by: []` and separately, in a comment on the same
line, "TASK-0094 ... is a genuine prerequisite ... and its issue is not
yet filed." §328 (the parent feature, confirmed open) lists TASK-0094 as
"not filed" too, so the underlying fact is accurate — but a task whose
real blocker is expressed only in a YAML comment (justified by "task rule
8," which is not itself visible in this issue) is one dashboard query away
from being picked up by an agent or contributor who filters on `blocked_by`
and finds nothing. The issue's own Method checklist item 2 ("Confirm
TASK-0094 has landed") is the only enforcement, and it is manual. **Recommendation:**
either put TASK-0094 in `blocked_by` once it is filed (the issue already
plans to — "Task-to-task edges remaining for the link pass: one —
TASK-0094 ... blocks this task" — so this is presumably transitional) or,
until then, add a bot-readable marker (a label, or a checkbox at the top)
rather than relying on prose buried after "blocked_by: []".

**4. (Medium) The "two weeks" cost estimate absorbs a real external submission cycle it cannot control.**
§11 states: "Two weeks buys the document, the script, the self-test, the
drift tests and **one** walk." A real shuttle/tapeout submission (the
issue's own language — "template's CI," "LibreLane," "GDS," "physical
chip" per #302) is bound to an external program's cadence; real programs
of this shape run submission rounds on the order of months, not weeks,
and a CI-side LibreLane run failing for utilization or timing reasons
(a risk the issue itself names in Open Question 2 — "a large design
risks failing for utilization reasons unrelated to the path") can consume
the entire window without producing a walk. The issue does have a release
valve (record a "completed dry run... leave the test failing with
WAIVED"), which is the right shape of mitigation — but then the "two
weeks" cost claim and "the walk happened" DoD line are in tension with
each other: the schedule assumes success, the completion criteria assume
either success or an indefinitely-red test (see finding 2). **Recommendation:**
state explicitly that the two-week estimate covers the non-walk work only
and that the walk itself is calendar-bound and excluded from that budget,
matching the more careful language #328 §6 already uses ("scheduled
rather than estimated").

**5. (Medium) No mention of cost, ownership, or licensing for the real submission.**
The task's terminal deliverable is an actual chip-shuttle submission —
someone's name/account goes on it, some shuttle programs charge per tile
or require institutional sponsorship, and the "template repository" a
student clones is very likely licensed differently from JLS itself
(JLS is GPL-3.0-or-later per README.md; open-hardware shuttle templates
are typically Apache-2.0 or CERN-OHL, per common practice in this space).
Neither §7 (Interface & Data Contract) nor §11 (Threats to Validity)
addresses who bears the submission cost, under what account/identity the
walk is performed, or whether wrapping GPL-licensed-adjacent generated
RTL into an Apache/CERN-OHL template creates any obligation worth a
sentence. Given the project is explicit elsewhere about licensing
provenance (README.md "License and provenance" section, `pop_GPLv3.pdf`),
this silence on the shuttle template's licensing is a gap worth closing
before a maintainer signs up for and pays for a real submission slot.
**Recommendation:** add one line to §7.2 or §11 naming the template's
license and confirming JLS-generated RTL under it is unproblematic, and
name who bears any submission cost.

**6. (Low) P8 (documenting the transformations LibreLane performs) is the one prediction with no drift test.**
Every other must-hold prediction (P2 set-equality, P3-P7 self-test
behavior) has a named enforcement mechanism. P8 — "the document states
what the open flow does to the design — constant propagation, technology
re-mapping, drive-strength replication and renaming" — is checked by
nothing but human review at write time; if the upstream LibreLane flow's
behavior changes, nothing in this issue's test suite would catch the
document going stale, unlike P2's drift test for file names. This is a
minor asymmetry worth naming explicitly as an accepted gap (or folding
into a lighter documentation-only check) rather than leaving it looking
as rigorously pinned as its neighbors.

## What holds up

- The precedent-matching is genuine, not asserted: the exact file/line
  citations for `icestick-handoff.sh`, `icestick-handoff-selftest.sh`, and
  `PcfEmitter.java` all check out against the current tree.
- O2's correction (six `_TBD_` cells, not five) is independently
  verifiable and correct: `git grep -o "_TBD_" docs/icestick-bitstream-handoff.md | wc -l` → 6, all on the one row at line 119.
- The refuse-rather-than-overwrite design for the template clone (P6,
  §7.11) is a sound, appropriately paranoid answer to the one real
  filesystem hazard the script touches.
- The scope boundary (no JLS-side synthesis/PnR/GDS) is consistent with
  the parent feature #328's Global Invariant 1 and the closed #215's H2,
  and is stated as a cost judgment rather than a policy per D8's framing
  (even though D8's own commit is unverifiable — finding 1).
- Cross-checking against #328 (open) confirms the dependency graph
  claims in #432's own machine block are accurate: TASK-0094 unfiled,
  #327/#339/#359 as #328's real `blocked_by`.
