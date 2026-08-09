# Issue #713: TASK-C530-1: an nbgrader unit's hidden cells grade a JLS lab by subprocess through the frozen contract, with no live session anywhere
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

TASK-C530-1 is the notebook-and-grading-cells half of #530 (FEAT-C21-5, the
nbgrader adapter for CAP-21, #502): ship an nbgrader unit whose hidden cells
shell out to a pinned JLS build in batch mode and grade from the recorded
xUnit/exit-status artifacts alone, refusing rather than driving a live
session. `band_mw: 0.5-1`, `ordering_after: [524]`. The core mechanism is
sound and precedented (`examples/autograde/autograde.py`,
`test/jls/AutogradeBridgeExampleTest.java` already grade JLS purely by
subprocess + artifact inspection). The problems are all in what the
acceptance criteria assume exists, how testable they are as worded, and one
piece of scope that appears nowhere upstream.

## Findings, most severe first

**1. (High) Every AC is written as if the "frozen CLI contract" and "xUnit
schema" already exist; neither does, and the one issue that would create them
is itself flagged `needs-rework`.** AC-1 and AC-3 both key off "the frozen
CLI contract" / "the xUnit schema." Verified against `src/jls/JLSStart.java:759-789`:
the `FLAGS` table has exactly 14 entries, none named `-check`/`-report`/`-equiv`,
and `docs/batch-interface.md:36-40` documents exactly three exit statuses
(0/1/2) — no status 3, no xUnit report flag. All of this is `ordering_after: [524]`'s
(FEAT-C21-1) job, and this fleet's own review of #524
(`issue-reviews/issue-0524.adversarial.md`, verdict `needs-rework`) found its
body still names a stale, disputed `ordering_after` and an AC-1 checkbox that
is "already false as literally worded." #713 states none of this: it names
#524 as its sole blocker but gives no indication that #524's own artifact is
currently unstable, nor that CAP-21's mermaid graph (`F053 --> PF1 --> PF5`)
puts the still-open #369 (FEAT-053) upstream of #524 too. A `band_mw: 0.5-1`
estimate next to a dependency chain that is several unlanded, contested
issues deep is the same disclosure gap this fleet already flagged at the
parent-feature level (#530's own review, finding 1). Recommend: state
explicitly, as #524's child tasks do for themselves, that the estimate is
marginal-only and point at #524/#369 for the real critical path.

**2. (Medium-High) AC-2's "instrumented build asserts no interactive session
is ever opened" names no mechanism, and the ambiguity is the same one
already caught in #530.** Quoting AC-2 verbatim: *"An instrumented build
asserts no interactive session is ever opened during a full autograde run
(CAP-21 AC-4)."* #530's own review (finding 2) flagged this identical
sentence (it is inherited near-verbatim from #530's AC-1) as unspecified:
is "the build" the pinned JLS jar (verify no `jls.edit`/Swing class ever
loads during the subprocess call — the `HeadlessCoreCanaryTest` idiom
`ARCHITECTURE.md` already documents) or the notebook kernel (verify no
long-lived JLS process handle survives across cells)? As worded, an
implementer can satisfy the checkbox by asserting something trivial (e.g.
"the Python cells never import a JLS Java API") without ever forking a JVM
and checking what actually loaded. #713 repeats the sentence without fixing
the ambiguity #530's review already surfaced against the same wording.
Recommend naming the concrete check, ideally by extending
`HeadlessCoreCanaryTest`'s class-load assertion to the pinned grading build.

**3. (Medium-High) KC-21-2's framing is decorative: there is no live protocol
for this adapter to reach for, so the kill criterion can never fire.** The
outcome text frames the unit as proof that "if grading from a notebook turns
out to need a live protocol, the adapter stops rather than acquiring one
(KC-21-2)." But `docs/vcd-interop.md` (quoted in #498 §7.2, fetched this
pass) states plainly: *"Not offered: live co-simulation … was evaluated and
rejected — see issue #63."* JLS's batch surface is subprocess + recorded
artifacts, full stop; there is no alternative live-protocol door available
to open even if an implementer wanted to cut a corner. A kill criterion that
describes a temptation nobody can act on is not testing anything — it reads
as evidence of rigor without being falsifiable. (This is the same structural
point the fleet's visionary review of #530 already made under
"Reframing 1," phrased there as "a kill criterion that cannot fire is
decoration, not a constraint.") Recommend dropping the KC-21-2 framing from
the outcome text, or rephrasing it as what it actually is: a statement that
the adapter has no live-session door to begin with, not a constraint an
implementer must actively resist.

**4. (Medium) AC-4 (digest pinning + refuse-by-name on an incompatible
contract version) is scope invented at the task level with no upstream
source and an unstated dependency.** #530 (the parent feature #713 belongs
to) lists exactly four ACs, and none of them mention a build digest or a
contract-version refusal — #713's AC-4 has no antecedent in #530, in CAP-21
(#502), or in #498. "Refuses by name against an incompatible contract
version" presupposes a queryable contract-version identifier, which per
#524's own review is owned specifically by its child task #690
(TASK-C524-3, "contract version queryable from the CLI... works on a build
with no circuit, no lab and no license state") — not by #524's other two
children. #713's `ordering_after: [524]` names only the parent feature, so
formally an implementer following the machine block waits for #524 to
close, but nothing pins the dependency to #690 specifically, and #524's own
Definition of Done permits closing with an entry "removed via a REPLAN
comment" rather than landed. If #690 is descoped or delayed independently
of #524's other children, AC-4 has no version string to refuse against.
Recommend either dropping AC-4 (fold digest pinning into the existing
SHA256SUMS/attestation pattern `README.md` already documents, and defer the
version-refusal half to whichever issue actually ships #690) or naming #690
explicitly in `ordering_after`.

**5. (Medium) AC-2's citation of "(CAP-21 AC-4)" implies this task alone
discharges it; CAP-21's own text says otherwise.** CAP-21 (#502) §4 defines
AC-4 (`RecordedArtifactOnlyTest`) as: *"An instrumented build asserts every
grading verdict on every platform derives from recorded batch artifacts...
Spans PF-5 + all adapters."* "All adapters" means Gradescope (#525/PF-2),
Classroom (#526/PF-3) and PrairieLearn (#528/PF-4) too, not just the
nbgrader unit. #713 checkboxes an AC-4 clause scoped to its own instrumented
build only; taken literally, ticking it proves nothing about the other three
platforms CAP-21's AC-4 also requires. This is a minor wording risk (the
capstone-level criterion is clearly a roll-up), but as filed a reviewer
closing #713 could reasonably believe AC-4 is fully discharged when only
one-fifth of its scope (PF-5's slice) actually is. Recommend rephrasing the
checkbox to "...contributes PF-5's slice of CAP-21 AC-4" or similar.

**6. (Low) AC-3's "scrapes no incidental stdout" names no verification
test, mirroring a gap already flagged in the parent issue.** Unlike AC-1/
AC-2/AC-4, which at least gesture at a mechanism (nbgrader's autograde path,
an instrumented build, digest pinning), AC-3 is a code-review-only
constraint. #530's own review (finding 6) flagged the identical property at
the feature level and recommended a `CliContractConformanceTest`-style check
that the adapter only touches documented artifact paths/schema fields. #713
inherits the gap unchanged at the task level, where it is arguably more
important since #713 is the issue that actually writes the grading cells.

**7. (Low) The "CAP-21 fixture lab" AC-1 grades against does not exist in
the tree and is not this issue's to create.** `grep -rli "lab-as-data" .`
and a search for any committed fixture-lab file return nothing under
`test/fixtures/` beyond unrelated `.jls` circuits. Per #502 §"Background,"
the fixture lab (starter circuit + hidden vectors, "CAP-06 lab-as-data
format") is CAP-06's/#466's deliverable, and #466 is open. #713 doesn't
name this dependency at all — it says "the CAP-21 fixture lab" as though it
were a stable, already-defined artifact to point cells at. Low severity
because the same silent assumption already exists in #530 and #715 and
CAP-21's own mermaid graph implies the ordering; still worth an explicit
line in this issue given it's the one that actually writes cells against
the fixture.

**8. (Low) No toolchain-provisioning story, same gap already flagged in
sibling #715.** Building "an nbgrader example unit... runnable through
nbgrader's ordinary autograde path" requires a Python/Jupyter/nbgrader
toolchain; the repo currently has zero Python footprint (no
`requirements.txt`, no Python entries in the dev container, README's
"Optional development tools" list is Maven/JDK plus native packagers only).
Sibling issue #715's own adversarial review (finding 3) flagged this same
gap for the CI doc-test lane; #713's Boundary section, which explicitly
carves the "CI doc-test lane" out to #715, doesn't mention that #713 itself
still needs some way to *author and validate* the unit locally before #715
ever runs it in CI. Worth one sentence naming where the toolchain comes
from.

## What's solid

- The core grading mechanism — subprocess invocation of batch mode,
  artifact-only grading, no live session — is exactly what the tree already
  demonstrates works (`examples/autograde/autograde.py`,
  `test/jls/AutogradeBridgeExampleTest.java`), and is consistent with
  #498 §7.2's "the recording, not the session, is the contract" framing.
- The Boundary line citing "#498 §8 exclusion 7" for "no JLS-operated
  service" is accurate: exclusion 7 reads "A server, a network dependency,
  an install step, or a plugin execution surface ahead of demand" —
  verified against #498's body, quoted correctly.
- `ordering_after: [524]` correctly identifies the direct blocker and
  matches CAP-21's own filing-time mermaid graph (`PF1 --> PF5`).
- The division of labor with sibling #715 (parity-vector emission and the CI
  doc-test lane carved out as TASK-C530-2) is clean and, cross-checked
  against #715's own scope description, does not double-claim anything.

## Note

Most of these findings are wording/traceability gaps, not design flaws: the
grading approach itself is the right one and already has a working
precedent in-tree. The concerning part is that #713's acceptance criteria
read as though the frozen contract, the xUnit schema, and the CAP-21
fixture lab already exist and are stable, when all three are still owned by
open, in-flux upstream issues (#524, #369, #466) — an implementer picking
this up today has nothing concrete to point cells at yet.
