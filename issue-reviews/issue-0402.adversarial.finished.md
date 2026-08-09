# Issue #402: TASK-0099: controlled sources, time-varying waveforms and a small model-card grammar turn the bare analog solver into something a teaching lab can build circuits with
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## 1. The cited evidence commit does not exist in this repository (severe)

Every "Verified at `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`" claim in the issue — O1 through O4, all of §2 (Observations), the whole falsifiability apparatus — is pinned to a commit hash that is not reachable in this repo:

```
$ git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7
fatal: git cat-file: could not get object info
$ git log --all --oneline | grep -i 2d0ca9d
(no output)
```

`git log --all --oneline | wc -l` returns 276 commits; the hash simply is not among them, nor is `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`, the commit the issue cites for `docs/plan/evidence/BRIEF.md` (D8, D10) and, on sibling issue #351, for `docs/plan/evidence/analog-determination.md` (the cost basis). `docs/plan/` does not exist anywhere in the tree today (`find docs -iname '*plan*'` returns nothing), and there is no `BRIEF.md` anywhere in history. The issue's entire rationale — "D8 is the enabling ruling," "per D10 the absence of the capability is the premise of the question" — rests on a document that cannot be located, quoted from a commit that does not exist. **Recommendation:** before this issue is actionable, the author must re-pin every citation to a real, resolvable commit, or strike the D8/D10 argument and restate the scoping rationale in the issue body itself.

## 2. The one honest premise happens to still be true, which makes the fabricated pinning easy to miss

`src/jls/analog` genuinely does not exist at current HEAD (`ls src/jls` lists `boot, collab, core, edit, elem, hdl, images, module, sim, tutorial, util` — no `analog`), so O1's headline claim ("there is no analog package") is not false, just unverifiably sourced. This is worse than an obviously wrong issue: a reviewer who spot-checks only the conclusion, not the citation, will wave it through. Treat the unresolvable commit as a blocking defect regardless of whether the surface conclusion happens to hold.

## 3. `blocked_by: []` contradicts the issue's own prose (high)

Section "Status & Dependencies" declares `blocked_by: []` and calls TASK-0097 "no FILED prerequisite; the unfiled one is named below." But the same section then says: "The dependency is **real, not conventional**: controlled sources are matrix **stamps** and waveforms feed the timestep controller's **breakpoint set**. Both read structures only TASK-0097 creates, and neither is meaningful without a solve." Materials & Apparatus repeats it: "Must exist first: TASK-0097's MNA assembly, Newton iteration, escape ladder and timestep controller. Stamps and breakpoints are meaningless without them." Sibling feature #351 confirms TASK-0097 is "unfiled" and is on 351's own critical path (`TASK-0097 → TASK-0098 → TASK-0100`), and #331 independently states the solver core "is a separate feature and a hard prerequisite." So this is a genuine, load-bearing, blocking dependency on an issue that does not exist yet, encoded in a machine-readable field as if there were none. Any automation, triage board, or "ready work" query that trusts `blocked_by` will surface #402 as startable when it is not — every one of P1–P9 requires a solver that isn't there. **Recommendation:** either file TASK-0097 first and set `blocked_by: [TASK-0097's number]`, or explicitly mark #402 as not-ready in its own status block instead of relying on prose the tooling won't read.

## 4. Acceptance criteria are gameable at exactly the places the issue itself flags as risky

The issue is unusually self-aware about two failure modes (H2/SFFM/AM breakpoints, and the card-grammar scope creep) but the stated verification for both is weak enough to pass while missing the real goal:

- **P3/P9 (waveform breakpoints).** "Falsification Criteria" for H2 says the fallback is "ship the waveform as unsupported and say so" if `SFFM`/`AM` breakpoints aren't closed-form. But Open Question 2's "recommended default" is the same non-decision restated, and nothing in the Definition of Done actually requires `SFFM` and `AM` to ship at all — P3 says "must hold after" for all six waveforms, yet the falsification path explicitly permits shipping fewer than six under the label "unsupported." A contributor could close this issue having shipped four working waveforms and two stubs that throw "unsupported," technically satisfying "no waveform ships with an empty breakpoint set" while failing the abstract's promise of "six time-varying independent sources." The DoD checklist item ("All six waveforms are pure functions of time and each publishes a non-empty, exact breakpoint set") contradicts the falsification criterion's own escape hatch — pick one.
- **H3/card grammar (§10, Open Question 1).** The refusal set and grammar boundary is explicitly called "the scope creep risk in this whole programme," yet the acceptance test (P5) only checks that unrecognized cards are *reported*, not that the grammar stayed small. Nothing stops a contributor from quietly recognizing five more card types (each individually justified as "a realistic teaching card needs it") while every P5/P10 check still passes, because P5 tests error reporting, not grammar size. The only real guard is human judgment at review time ("a documented decision"), which is exactly the gate the issue elsewhere insists on replacing with tests.

## 5. Cost/scope is entangled with an unpriced, unfiled sibling task with a 3.25x planning gap

Parent feature #351 records its own task-row sum (8.0 mw across TASK-0097/98/99/100) against a stated band of 17.5–26 mw — a 3.25x gap the issue itself calls "the largest gap of the features in this pass" and explicitly refuses to reconcile ("no row has been adjusted to make the band true"). #402/TASK-0099 is one of those four rows, priced at a flat "2 wk" with no visible derivation in #402 itself (the number appears only in #351's cost table, not here). A reviewer approving #402 in isolation has no cost justification in the issue being reviewed; they must cross-reference #351 and then discover the estimate is explicitly unreconciled.

## 6. `POLY(n)` "native" stamping commits to a specific matrix-size invariant without demonstrating it's achievable

§7.10 and H1/P2 require `POLY(n)` to stamp via its Jacobian with "no auxiliary node introduced," and Falsification Criterion H1 says an auxiliary-node expansion is "acceptable only if recorded, because it changes the matrix size and therefore #351's determinism baseline." This makes the *preferred* outcome (native stamping) also the *unverified* one — there's no citation of any comparable implementation (SPICE's own `POLY` is typically expanded or handled via explicit multi-partial-derivative code, and getting a general n-ary polynomial's full Jacobian stamped correctly without an auxiliary node is a non-trivial linear-algebra/codegen problem, not a small addition). The issue asserts feasibility ("H1 ... expressible natively") without showing a worked example beyond the 2-source VCVS. Given `SpiceNumber` is scoped at "~40 lines" but `POLY(n)`'s native Jacobian stamping isn't scoped at all, the size estimate is asymmetric and understates the harder half of the work.

## 7. Format-compatibility claim ("zero format version") is asserted, not demonstrated against the loader's actual unknown-item behavior

O4/P7 claim saving analog parameters as `String` costs "zero format version." §7.7 itself immediately undercuts this: "a reader that does not know the item kind falls under the existing unknown-attribute rule — which is the silent-drop hazard #314 owns and which this task must name rather than assume away." So the issue both claims zero-cost compatibility and names, in the same section, an open silent-data-loss hazard for older readers opening newer files — this is not a contradiction in fact (adding a new item kind genuinely doesn't require a `FORMAT` bump under the existing scheme) but the "zero format version" framing in the Abstract and O4 undersells a real compatibility risk that the issue's own §7.7 flags as unresolved and hands off with no owner beyond "must name rather than assume away." Naming a hazard is not closing it; Definition of Done never requires a decision on it, only that it be named.

## 8. Things that are solid

- The observation that `1M` vs `1MEG` needs exactly one shared parser is a real, well-known SPICE hazard and the "one implementation, grep-enforced" design (P10) is a genuinely testable, hard-to-game acceptance check.
- Modeling the card reader on `PinBindings.java`'s collect-and-report-together idiom is accurate: `src/jls/hdl/board/PinBindings.java:38-39` really does say "Every malformed line is collected and reported together, so the user learns the full repair job," confirmed by reading the file directly.
- The per-package JaCoCo floor / `@NullMarked` package convention cited for `jls.analog` matches real, existing patterns in `pom.xml` (multiple `PACKAGE`-scoped JaCoCo rules already present) and in other headless leaf packages — this part of the plan is not invented.
- Explicitly listing what's out of scope (deck parser, BSIM models, solver itself, drawn palette, A2D/D2A bridges) is good discipline and matches the boundaries stated in the related features (#351, #331).

## Bottom line

The issue cannot be started today: its own text names a hard, unfiled prerequisite (TASK-0097) while the machine-readable `blocked_by` field says otherwise, and its entire observational evidence base cites a git commit and a planning document that do not exist anywhere in this repository's history. Even setting the sourcing problem aside, two of its own named risk areas (waveform breakpoint coverage, card-grammar scope creep) have acceptance criteria loose enough to be satisfied while missing the stated goal. Recommend the author re-file with real commit citations, either an actual TASK-0097 issue number in `blocked_by` or an explicit "not ready" status, and tightened P3/P5 criteria before this is picked up.
