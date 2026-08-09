# Issue #562: FEAT-C29-5: a Digital circuit's embedded test cases arrive as runnable -t vector files that pass with the same verdicts Digital reports — the instructor's grading suite survives the migration
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The boundary carve-outs (test-vector scope split from #323, runner/panel split from #466/#214) are internally consistent with the issues they cite. But the core acceptance criteria rest on a channel — the `-t` vector format — that structurally cannot carry the thing AC-1/AC-4 demand ("verdicts"), and the feature this issue silently needs to close that gap is not in its own `ordering_after`. A second, independently verified contract mismatch: AC-2 routes untranslatable-construct losses into "the shared FEAT-C29-1 report" (#556), but #556's own body enumerates its consumers and #562 is not among them.

## Findings, most severe first

### 1. AC-1/AC-4 require "verdict parity" through `-t`, but `-t` has no verdict concept, and the feature that would add one is missing from `ordering_after`

AC-1: *"its translated vectors pass under `-t` with the same verdicts Digital reports on the source."* AC-4: *"Verdict parity is asserted mechanically... not by visual inspection."*

`docs/batch-interface.md` §2.2 gives the `-t` grammar as `file ::= { signal }`, `signal ::= name initial { step } "end"` — every production drives an `InputPin`; there is no expected-output production, no comparison, and batch mode's three exit statuses (0/1/2, §1) do not include "ran and disagreed." #369 (FEAT-053, open) states this explicitly and is the issue that would fix it: *"the batch engine has no verdict at all... per-vector pass/fail is not a UI task over an existing verdict — the verdict does not exist yet."* #466 (TASK-0111, the filed child of #369 that would add the expectations channel) is itself open and, per this fleet's own review of it, self-contradictory between its body and its "REVISION" comment (`issue-reviews/issue-0466.adversarial.md`).

`-t` files alone, as this issue's Outcome describes them, can reproduce Digital's *input stimulus* but cannot express or check Digital's *expected outputs* — there is nothing in the current batch interface to compare against. Yet #562's `ordering_after` names only `FEAT-C29-2` (#558) and `FEAT-C29-1` (#556); #466/#369 appear nowhere in the machine block, only in a boundary-note aside ("the grading/expectation harness itself is owned by the batch line (TASK-0111 #466...)"). A boundary note is not an ordering edge — an implementer or scheduler reading the YAML alone would not know AC-1/AC-4 are unsatisfiable until #466 lands, and #466 is not even confirmed to be a coherent target yet.

**Recommendation:** add #466 (and arguably #369) to `ordering_after` as a hard prerequisite, and rewrite AC-1/AC-4 to state explicitly what "verdict parity" means given `-t` alone can only be diffed on watched-element stdout output (§3 of batch-interface.md), not asserted pass/fail — unless the intent actually is to also emit an expectations file per #466's contract, in which case the Outcome text ("translate into runnable JLS `-t` vector files") undersells the real deliverable by naming only one of the two artifacts required.

### 2. AC-2 outsources loss-reporting to a contract that does not list this issue as a consumer

AC-2: *"Every test construct that does not translate... is a named, located, explained loss in the shared FEAT-C29-1 report."* FEAT-C29-1 is #556. #556's own body, under "Boundary and reference notes," states: *"Consumers: FEAT-C29-2 (.dig), FEAT-C29-3 (.cv), FEAT-C29-4 (Falstad text)."* FEAT-C29-5 (#562) is absent from that list, despite #562 depending on it for its second acceptance criterion. Either #556 needs updating to acknowledge a fourth consumer whose losses are *test-construct* dispositions rather than *circuit-element* dispositions (a materially different taxonomy — Digital's test DSL and its circuit-element set are different vocabularies), or #562's AC-2 is pointing at a schema that was never designed to receive this shape of loss. This is the same "shared contract's own consumer list disagrees with a claimed consumer" pattern the fleet's dedup pass already flagged once on this thread (comment: "#556's... where any real convergence would show up") without actually checking whether #556 lists #562 — it doesn't.

**Recommendation:** either add FEAT-C29-5 to #556's consumer list and confirm the schema (construct → disposition → location → explanation) actually fits *test-language* constructs, not just circuit elements, or #562 needs its own loss taxonomy and AC-2 should stop claiming reuse of #556's contract.

### 3. `ordering_after` names dependencies by feat_id only, with no issue numbers — unlike the project's own documented linking convention

`ordering_after: ["FEAT-C29-2 (.dig importer...)", "FEAT-C29-1 (losses in translation...)"]` — no `#558`/`#556` anywhere in the YAML, even though `serves_capstones: [513]` in the same block does use a bare number. Compare #323 (FEAT-025), which documents an explicit "LINK PASS" discipline: *"`blocked_by` and `blocks` now carry filed issue numbers... every edge written here is mirrored on the issue it names."* #562 predates or skips that pass for its own two dependencies, so a scheduler walking the DAG mechanically (as #323's own process describes doing) cannot resolve FEAT-C29-2/FEAT-C29-1 to #558/#556 from this issue alone — it has to cross-reference the sibling issues or the dedup comment to recover the numbers. Sibling #556 and #558 both number at least some of *their* cross-references inline. Minor on its own, but combined with Finding 1 (a real, unlisted dependency), the ordering graph as written is not trustworthy for scheduling.

**Recommendation:** run the numbered link pass on this issue's `ordering_after` before it leaves provisional status.

### 4. AC-4's bar ("at least one real published .dig circuit") is gameable against the issue's own stated purpose

The Outcome's framing is "the instructor's grading suite survives the migration" and "migrating circuits without their tests migrates half a course" — a claim about grading suites in general. AC-4 only requires "at least one real published `.dig` circuit with tests." An implementer can satisfy AC-2 and AC-4 simultaneously and vacuously by choosing a single small fixture whose embedded tests use only constructs that already map cleanly to `-t`'s four productions (initial value, `for`, `until`, `end`) — no named losses because there's nothing untranslatable in the chosen sample, no evidence the translator handles anything harder. Digital's own test language (loops, expected-output assertions, generated/random data — not verified in this repo, general knowledge of the tool) is plausibly far richer than one cherry-picked circuit would need to exercise. The letter of AC-2+AC-4 can pass while the Outcome's actual claim ("the grading suite survives migration") remains untested for anything beyond the simplest case.

**Recommendation:** require the corpus fixture to be chosen for coverage of the test-language surface (or require N ≥ 3 circuits spanning documented construct categories), not just existence of one.

### 5. AC-4's fixture requirement raises an unaddressed licensing question this project already takes seriously elsewhere

Sibling feature #323 (FEAT-025) devotes an entire "Open Question" to exactly this: absorbing third-party circuit/course material into a GPLv3 repo needs its license checked first (*"The licence question must be settled before absorbing any source... Blocks filing children"*). #562's AC-4 requires committing "at least one real published `.dig` circuit with tests" as a fixture (implied, since AC-4 says the parity assertion must be mechanical, i.e. CI-run against a committed file) but never raises licensing, attribution, or whether the source instructor/course consented to redistribution. This is the same category of risk #323 flagged for `.circ` part data; #562 doesn't inherit that caution.

**Recommendation:** add a licensing/provenance check for the fixture circuit to the acceptance criteria or boundary notes, mirroring #323's Open Question 2.

### 6. Band (2-3 mw) is optimistic against the dependency stack it actually requires

This feature needs: (a) #558's still-undesigned "preserved test sections" data model (#558 itself is 4-6 mw and unstarted), (b) a translator from Digital's test DSL into `-t`, (c) a loss taxonomy conforming to #556's schema (which per Finding 2 doesn't yet account for this consumer), and (d) a verdict-parity oracle that, per Finding 1, needs #466's not-yet-existing expectations engine. Sibling capstone body #513 lists PF-2 (`.dig` element mapping alone) at 4-6 mw; this issue is priced lower while depending on strictly more unbuilt infrastructure downstream of it. The project's own CAP-29/#323/#369 documents repeatedly show bands and task sums disagreeing by 1.5-3.75x once actually decomposed — nothing here suggests this issue's estimate has had that scrutiny yet.

**Recommendation:** re-price after #558 and #466 have concrete data contracts, per the project's own "measurement before estimate" discipline (#323 Open Question 1 practice).

### 7. Boundary carve-out from #323 and #466/#214 is accurate — no issue

The claim that #323 "explicitly scoped out 'the test-vector half of a course'" checks out verbatim against #323's own text (*"The test-vector half of a course... migrating circuits without the assignment's test vectors migrates half a course, but the expectation surface and the grading harness belong to the autograding feature; #214 (open) is its GUI front-end"*), and the #466/#214 split matches #466's own title (TASK-0111) and #369's decomposition. Solid citation.

### 8. AC-3 (byte-determinism) is concrete and testable — no issue

A clear, mechanically checkable requirement consistent with the project's existing reproducibility ethos (README's bit-for-bit jar/BOM discussion). No notes.

## Verdict rationale

The scoping and citations that are checkable against sibling issues mostly hold up (Findings 7-8), but the two load-bearing acceptance criteria (AC-1's verdict parity, AC-2's shared-report routing) each depend on another issue's infrastructure that either doesn't exist yet and isn't listed as a dependency (#466, Finding 1) or doesn't list this issue as a consumer (#556, Finding 2). Those aren't stylistic nits — they mean AC-1 and AC-4 as literally written cannot be satisfied by `-t` output alone, and AC-2 points at a contract that hasn't agreed to receive this issue's losses. Combined with a gameable AC-4 bar (Finding 4) and an unaddressed licensing question for the required fixture (Finding 5), this needs rework before an implementer could execute it as a single coherent spec. **needs-rework.**
