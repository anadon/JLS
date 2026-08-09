# Issue #456: TASK-0075: resuming from a checkpoint reproduces the next one byte for byte, and a deliberately dropped field fails the gate
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is technically well-grounded — every code citation I could check
(`SimEvent.java:86-87`, `BatchSimulator.java:420-422`, `Memory.java:1384-1386`,
`Register.java:692-698`, `StateMachine.java:658-659`, `Simulator.java:230-233`,
`WireNet.java:405-407`, and the `test/fixtures/` size of 4 files / 127,330
bytes) matches the current tree exactly. The methodological core — assert on
bytes, not a field comparator; require a committed null that must fail; treat
the VCD tail as a genuinely independent observable — is sound engineering.
The problems are in what the issue asks a future executor to commit to
*before* the thing it is testing has been designed, and in one internal
inconsistency in the null's own construction.

## Findings, most severe first

**1. (High) The stated `blocked_by` radically understates the real critical path, and the issue nonetheless pre-specifies implementation details against an undesigned codec.**
`blocked_by: [426]` is true but incomplete. #426 (TASK-0074) is itself
`blocked_by: [410]` **and** "TASK-0033 (section framing … is not yet filed)".
TASK-0033 belongs to #319 (FEAT-013), which is itself `blocked_by: [334]`.
So the real chain to make #456 startable is at minimum #334 → #319 →
TASK-0033 (unfiled) → #426 (also gated on #410, whose "suspended vs.
terminated" decision is still an open question) → #456. None of this is
visible from #456's own machine block, which names one blocker. Despite
that distance, #456 already prescribes exact test method names
(`resumingFromEachCheckpointReproducesTheNextByteForByte`, etc.), an exact
field to drop in the null (`Register.currentC`), and exact fixture counts —
specification detail that presumes a codec shape (#426) and a section
framing (TASK-0033) that do not exist yet and whose own open questions
(e.g. #319 Open Question 1: "frame inside text grammar, or multi-member
container?") are unresolved. If either upstream design goes a different
way, several of #456's "Must be built" specifics (byte-offset diffing, the
null's shape) may need to be rewritten rather than merely implemented.
**Recommendation:** either mark #456 explicitly `blocked_by: [426, 410, 319]`
(with 319 carrying the still-unfiled TASK-0033), or trim #456 to the
property statement and defer the concrete test/fixture prescription to
when #426 lands.

**2. (High) The null's target field is inconsistent with the issue's own evidence for what makes Register state "in-flight."**
O4 establishes `Register.toBeValue` (`Register.java:693`) as the
representative in-flight field for the "Register with a pending
`toBeValue`" fixture class, and P5 requires that fixture to be
"demonstrated to reach its state" — i.e. `toBeValue` non-null at a
checkpoint. But Stage 5 / P3, the null (arguably "the single most
important artifact" per §9), drops a *different* field:
`Register.currentC` ("the most recent value seen on the clock (C) input,"
`Register.java:696-697`). Nothing in the issue establishes that `currentC`
differs from its default at any checkpoint any of the six fixtures take —
the issue never traces `currentC` to a fixture the way it traces
`toBeValue`. H1's own falsification clause warns exactly about this: "if
the null passes, the gate is vacuous for that field... add a fixture that
reaches `currentC`." The issue is aware of the general risk but picked a
field it has not shown is reached, when the field it *has* shown is
reached (`toBeValue`) was sitting right there as the more defensible
choice. **Recommendation:** either retarget the null to `toBeValue` (already
justified by O4/P5), or add the missing justification for why `currentC` is
reachable at a differentiating checkpoint in one of the six fixtures.

**3. (Medium) P6's "fraction of the state space reached" is not a well-defined, checkable quantity.**
P6 requires the javadoc to state "what fraction of the state space the
fixtures actually reach," and the Data Collection section calls an
unquantified claim "exactly the reviewed-list failure mode this task
replaces." But no method is given for *computing* that fraction — the
simulator's state space (BitSets, queue contents, per-element fields
across ~30 element types) has no declared denominator. As written, a
future author can satisfy P6 by writing any plausible-sounding percentage
in a javadoc comment with no way for a reviewer to check it, which is the
same "green check mark replaces an unexamined assumption" failure mode
the issue itself is trying to prevent everywhere else. **Recommendation:**
replace the fraction with a checkable, enumerable claim — e.g., "N of M
non-final instance fields swept by the (separately-tracked) field map are
exercised by at least one fixture," tying it to #426's promised field-map
deliverable (P5 there) rather than to an unmeasurable "state space."

**4. (Medium) The long-run CI lane the suite must land in doesn't exist, and is itself blocked on an unfiled prerequisite — another understated dependency.**
The Method checklist and Completion Criteria both require "the suite runs
in the long-run lane, not the required fast lane" and cite #378
(TASK-0016) as the lane's owner, but #378 is currently open, has no
`longrun` tag or workflow anywhere in the tree (confirmed:
`grep -rn longrun` under `.github/workflows` and `pom.xml` returns
nothing), and is itself `blocked_by` an unfiled TASK-0015 (explicit
per-job timeouts). #456 lists #378 only under `related`, not as a
blocker, even though the suite cannot be correctly placed until #378
lands. This mirrors Finding 1's pattern of a listed-as-non-blocking issue
that is functionally a hard prerequisite. **Recommendation:** state plainly
in #456 that landing is also gated on #378, or accept the suite
temporarily running in the fast lane with an explicit follow-up to
relocate it once #378 exists (and say so, rather than silently assuming
the lane will be ready).

**5. (Medium) Every file:line citation is pinned to a commit that does not exist in this checkout, so nothing here is independently re-derivable from the stated evidence.**
The issue cites `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`
and a claimed branch HEAD `839fb3a` "at filing." Neither resolves:
`git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and the short
forms `2d0ca9d` / `839fb3a` all fail with "Not a valid object name" in
this repository. I was able to corroborate the substance of every citation
against current HEAD (`5b05d67`) instead, and it lines up closely (the
line numbers are off by at most a couple of lines in some files), so this
is not evidence of fabrication — but it means a reviewer cannot use the
issue's own stated evidence commit to check its claims, and the same
unresolvable hash appears identically in #426 and #363, suggesting a
shared, unreachable snapshot across the whole planning corpus this issue
was generated from. **Recommendation:** cite a resolvable tag/commit, or
note explicitly that the evidence commit is not present in the public
history and citations should be re-derived at HEAD (the issue's own rule 6
"re-verify... if HEAD had moved" already asks for this, so make it
mandatory rather than a caveat).

## What's solid (no action needed)

- The core methodological choice — byte comparison over a field comparator,
  because "a comparator written by the codec's author tests `ser` against
  the author's model of Σ rather than against Σ" — is exactly right and
  well-argued.
- Requiring the null to name "the fixture and the first differing byte
  offset," and explicitly rejecting "the null passing because the whole
  suite errored out" as satisfying P3, closes an obvious gaming vector.
- O2 (the `SimEvent.sequence` static-field hazard) is a genuine, verified
  landmine that would otherwise produce a confusing, misattributed VCD-tail
  failure; flagging it up front and requiring an explicit sequencing
  assertion is good defensive design.
- P8 ("no checkpoint artifact contains a wall-clock stamp") is concrete and
  trivially checkable, and O3's citation of the VCD header's deliberate
  `$date`/`$version` omission is accurate and directly supports it.
- The `.jls~` autosave naming-collision warning (citing
  `ARCHITECTURE.md:108-113` correctly) is a real, easy-to-hit trap this
  issue correctly pre-empts.

## Note on scope/cost (not a defect in #456 itself)

FEAT-035's (#363) own roster prices TASK-0075 at "1 wk," but the DoD here
runs to roughly twenty checklist items: six purpose-built `.jls` fixtures
each requiring proof of reachability, a committed and maintained null codec
variant, a VCD-tail harness, a coverage javadoc, an explicit sequencing
assertion, a filed follow-up issue (H3's per-field sweep), and placement in
a CI lane that doesn't exist yet. That estimate lives in #363, not #456,
but an executor picking up #456 alone should not expect it to fit in a
week once #426 actually lands.
