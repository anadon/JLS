# Issue #678: TASK-C527-1: the simulation event stream gains a tap seam that costs nothing when nobody is listening, proven red against an unconditional build first
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context checked
Repo at `/home/user/JLS`, `README.md`, `ARCHITECTURE.md`, `src/jls/sim/Simulator.java`,
`src/jls/sim/SimEvent.java`, `src/jls/sim/package-info.java`,
`src/jls/edit/InteractiveSimulator.java`, `src/jls/edit/Trace.java`,
`test/jls/HeadlessCoreRatchetTest.java`, `test/jls/sim/`, and the cited/related
issues #476 (TASK-0063, blocked/referenced by AC4), #527 (FEAT-C23-1, the
owning feature), #504 (CAP-23, the owning capstone), and the sibling
`issue-0680.adversarial.md` already on disk (TASK-C527-2, which consumes
this task's seam).

## Findings, by severity

**1. (High) The acceptance-criteria test `ChronogramClosedCostTest` is claimed as a deliverable by three different issues in the same chain, with no stated ownership.**
#678 AC2: "`ChronogramClosedCostTest` exists and compares kernel event
throughput and the first-year adder flow against the recorded baseline."
#527 (the parent feature) AC4, verbatim: "`ChronogramClosedCostTest` (CAP-23
AC-5, K9): with the chronogram closed, kernel event throughput and the
first-year adder flow match baseline." #504 (the capstone) AC-5, again
verbatim: "`ChronogramClosedCostTest`... kernel event throughput and the
first-year adder flow match baseline." Three issues at three tiers each list
the identically-named test class with identical assertions as something
*they* deliver. As written, whichever of #678/#527/#504 lands last either (a)
finds the class already exists and has nothing to add for its own AC, or (b)
writes a second `ChronogramClosedCostTest` and collides on the class name.
Neither outcome is distinguishable from "criterion met" under the issue's own
text, so a reviewer cannot tell which issue is actually supposed to write the
test body. Recommend: #678 should state explicitly that it is the canonical
owner of `ChronogramClosedCostTest`, and #527/#504 should be edited to cite
it as inherited/re-verified rather than repeating it as a fresh AC.

**2. (High) AC1 ("same instruction sequence") and AC2 ("stated, measured tolerance") assert two different, weakly-related things, and only the weaker one is checkable.**
AC1 demands the retire path "executes the same instruction sequence it does
today" with unsubscribed consumers. That is a claim about emitted
bytecode/JIT-compiled machine code, not something a JUnit test can observe
directly — no test in this tree (or plausibly written for this task)
disassembles `runEventLoop`. AC2 settles for "within a stated, measured
tolerance" on throughput, which is the only part actually testable. A
consumer-registry field added to `Simulator` and iterated every retire (even
over an empty list) *does* change the bytecode of `runEventLoop` — "same
instruction sequence" is close to false on its face unless read loosely as
"same after JIT dead-code elimination," which is not guaranteed and not what
the text says. Implementations can pass AC2 (throughput within tolerance,
which a cheap empty-collection check will do) while AC1 as literally written
is neither verified nor falsifiable. Recommend: drop AC1's instruction-level
language and let AC2's measured-tolerance test be the single, honestly-scoped
acceptance bar — or add a documented micro-benchmark method (JMH,
disassembly diff) if the stronger claim is actually wanted.

**3. (High) AC1's own baseline commit, `2d0ca9d`, does not exist in this repository.**
`git cat-file -e 2d0ca9d` fails ("Not a valid object name"); `git log --all
--oneline | grep 2d0ca9d` finds nothing across all 267 commits reachable from
HEAD. AC1 pins its "no per-event branch on a live collection that was empty"
claim to "2d0ca9d's baseline" and the sibling task #476 pins its entire
warm-loop-percentage evidence (O1-O7, the 47.7%/151.8ns/318ns figures) to the
same hash with specific line-number citations. If that commit is unreachable
in this tree, the cited line numbers (`Simulator.java L23-L27`, L165-L170,
L215-L243 in #476; the implicit baseline in #678 AC1) cannot be independently
re-derived by whoever picks up the task, exactly the "supersession check"
#476 itself says is required (its own "rule 6"). Recommend: re-anchor to a
commit that actually exists in the tree before work starts, or state plainly
that the hash is aspirational/from a different branch and needs
re-verification.

**4. (Medium) The 47.7% figure is borrowed from a different code path than the one this issue's tap sits on, and using it here implies a false causal link.**
"the kernel already spends 47.7% of warm loop time on event bookkeeping" is
lifted from #476, where it is specifically `PriorityQueue`/`HashSet`
insert-dequeue-dedup cost (`Simulator.post`/`runEventLoop`'s `dupCheck`
calls) — not the retire-path notification point where a consumer tap would
sit (after `event.getCallBack().react(...)`, i.e. `Simulator.java:239-241`).
The two are adjacent but distinct regions of the same loop; #476 explicitly
lists itself and #475 (TASK-0056, "the other 37.6%") as covering the *rest*
of the profile, and neither leaves room for "tap overhead" as a labelled
share. Citing the 47.7% number here reads as if the observer seam is
competing for the same expensive real estate the queue replacement is fixing,
which overstates the urgency and could mislead whoever measures AC2 into
conflating "queue is fast" with "tap is free." Recommend: either cite a
figure that actually measures the retire/notify point, or drop the borrowed
statistic and rely on AC2's own before/after measurement to justify the
"costs nothing" claim on its own evidence.

**5. (Medium) No coordination is declared with #476 despite both tasks editing the same hot loop in `Simulator.java`.**
#476 replaces `eventQueue`/`dupCheck` and rewrites `post()` and
`runEventLoop()` (`Simulator.java:165-170`, `217-243`) in place; #678's tap
necessarily inserts a notification point in the same loop, most naturally
right after the same `event.getCallBack().react(...)` call at line 239 or in
`afterEvent`. #476's own related-work table calls out exactly this kind of
collision for its sibling #393 ("both touch `post`, so whichever lands second
rebases") but #678 lists `ordering_after: []` and never mentions #476 as a
file-level neighbor, only as a downstream consumer of the seam's *interface*
(AC4). A consumer-interface change and a queue-swap landing in either order
against the same ~80-line method is a realistic merge/rebase cost this issue
doesn't budget for. Recommend: add #476 to a `related:` (not necessarily
`ordering_after:`) list acknowledging the same-file collision risk.

**6. (Low) AC5's "gain no code on their path" is not a checkable predicate as stated.**
"Headless and batch runs register no consumer and gain no code on their
path" — "no code" is undefined (zero bytecode diff? zero object allocation?
zero additional method calls?). `BatchSimulator`/headless mode already never
instantiates `InteractiveSimulator` (enforced by
`HeadlessCoreRatchetTest`'s `CORE_PACKAGE_PREFIXES`), so this criterion is
almost certainly satisfiable trivially regardless of implementation quality.
Recommend restating as a concrete, measurable predicate (e.g., "the consumer
registry field is `null` or size 0 on the batch path, verified by a
reflective/assertion check"), matching the rigor AC1/AC2 aim for elsewhere.

## What's solid

- The core technical premise — that `SimEvent`'s existing `(time, seq,
  callBack, todo)` identity/value shape (`SimEvent.java`) is exactly what a
  queue-agnostic consumer interface should be defined over, so it survives
  #476's queue swap — is correct and matches the actual field layout.
- AC3's falsify-before-fix discipline (red transcript against an
  unconditionally-enabled tap, committed first) is a sound practice and is
  consistent with how #476 structures its own predictions/falsification
  sections.
- AC4's decoupling requirement (signature names no concrete queue type) is
  the right constraint given `Simulator.eventQueue`'s declared type is
  scheduled to change under #476; it correctly anticipates that dependency
  without hard-blocking on it.
- The `band_mw: 1-2` estimate is internally consistent with the parent
  feature #527's total (`4-6`) once #680's `2-3` and #682's `~1` are added,
  per the sibling review already in `issue-reviews/issue-0680.adversarial.md`.
- The general shape of "free when unsubscribed via a hook that dispatches to
  an empty method" already has precedent in this exact codebase
  (`Simulator.afterEvent`, overridden by `InteractiveSimulator` and
  `BatchSimulator` but a no-op in the base class) — the approach is not
  novel engineering, which is a point in its favor, not against it.

## Recommendation

Resolve the three-way `ChronogramClosedCostTest` ownership ambiguity across
#678/#527/#504 before work starts (finding 1); narrow AC1 to something a test
can actually check or drop it in favor of AC2 alone (finding 2); re-anchor
the baseline commit citation to a hash that exists in this repository
(finding 3); stop borrowing #476's queue-specific 47.7% figure to motivate a
different code region (finding 4); and note the same-file collision risk
with #476 (finding 5).
