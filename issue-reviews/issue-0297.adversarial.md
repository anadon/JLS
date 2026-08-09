# Issue #297: CAP-04: a drawn CPU becomes a buildable 74-series breadboard whose real electrical behavior — floating, contention, fan-out — simulates as wired
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

This is a "capstone" (CAP-04) in a fleet of 19 machine-generated capstone issues (#295-#313) sitting atop a further layer of 57 "feature" issues (#314-#370), all produced by an automated planning process ("Claude Code") that files, REPLANs, and cross-checks itself in the comment thread. The issue is open, well-formed, and internally rigorous by the standards of its own genre — but that genre is the problem. Below are the concrete defects found by checking its claims against this checkout (`e7731bd`, branch `claude/github-issue-review-agents-j99xga`).

## Findings, most severe first

**1. The cost table's entire evidentiary base does not exist on any mergeable branch — and the issue's Definition of Done still treats this as an open checklist item rather than a blocker.**
Every row in the "Cost" section cites `docs/plan/features/FEAT-0xx-*.md:3`. None of these files exist in this repo:
```
$ git show HEAD:docs/plan/features/FEAT-001-registry-keyed-table-totality.md
fatal: path 'docs/plan/features/FEAT-001-registry-keyed-table-totality.md' does not exist in 'HEAD'
$ test -d docs/plan && echo exists || echo "docs/plan MISSING"
docs/plan MISSING
```
The issue's own comment thread (`#5171440712`) already admits this: "citations into `docs/plan/**` … cannot be re-pinned at all — those 195 files do not exist on `master`." Yet the Cost section states these bands as settled numbers (86-132 mw standalone, 31-46 mw marginal, 7-14 mw demo slice) used to gate scope decisions in §5's Re-planning Protocol, and the Definition of Done merely requires them to "resolve on the default branch **at close**" — i.e., the issue can sit open indefinitely, be worked against, and have its scope/cost decisions made using numbers that are currently unverifiable by anyone reading the tracker today. An 86-132 person-week estimate that cannot presently be checked against its own source is not a specification a team can plan a sprint against; it's a promissory note.
**Recommendation:** either merge the evidence branch before this issue is actioned, or strip the numeric cost bands down to what's actually verifiable in-tree and mark the rest `TBD` until the source lands.

**2. Core deliverables named in the Outcome Statement do not exist and are self-admittedly invented by the issue, not scoped as new work items in the required-feature table.**
Step 1 of §1 requires `jls -breadboard examples/sap1.jls -lib 74ls -o plan/`. Neither the flag nor the fixture exists:
```
$ grep -n "breadboard" src/jls/JLSStart.java   → 0 hits (near 1200-line FLAGS table)
$ find . -iname "*sap1*" -o -iname "*sap-1*"    → 0 results
```
The issue notes the fixture gap itself ("examples/sap1.jls does not exist... The fixture is part of this capstone's deliverable"), but the CLI flag `-breadboard` and the fixture-building work are *not* separately called out as required-feature rows or costed line items — they're smuggled into "Tier A, step 1" of the walkthrough as if they fall out of FEAT-041/FEAT-040 for free. Building a correct, published SAP-1 circuit in JLS's format and wiring a new top-level CLI mode is real, uncosted work.
**Recommendation:** either fold fixture authoring and the `-breadboard` CLI surface into a named required feature with its own cost band, or explicitly flag them as this issue's own residual scope in the Cost section.

**3. AC-3's "falsification guard" is a good idea but is gameable as written.**
> "an undriven 74LS173 CLK resolves to `1` at `pull` strength; an undriven 74HC173 CLK resolves to `X`; the same pin driven LOW resolves to `0` at `strong` strength… The third assertion is the guard."
The guard only checks that the three results are *pairwise distinct integers* — nothing in AC-3 as stated pins the *direction* of strength comparison (i.e., that `strong` actually beats `pull` in resolution, not just that they're labeled differently) or that `X` on CMOS is computed from the four-state core's actual undriven-net semantics rather than hard-coded per-family in the test fixture. An implementation could special-case "if family == 74HC and pin undriven, return X" without a real strength lattice underneath, and AC-3 as written would go green. AC-3 needs an explicit assertion that the resolution is computed by the *general* fold/lattice mechanism (e.g., by testing a second, unrelated open-drain/pull-up net through the same code path), not merely that three specific numbers differ.
**Recommendation:** add an assertion in AC-3 that reuses the same resolution function on at least one net topology not authored specifically for this test, to rule out hard-coding.

**4. AC-1 check 2 (netlist equivalence) and its performance budget (KC-04-2) rest on a fixture ladder that doesn't exist and a "reference machine" doc that doesn't exist in this checkout.**
KC-04-2 cites `docs/machine-calibration.md §2.1` for the reference hardware; that file is absent (`find docs -iname "*machine-calibration*"` → 0 results) — same evidence-branch problem as Finding 1, but here it directly gates a hard pass/fail wall-clock number (60s) and a complexity check (>4x time for 2x package count across 7/35/140-package fixtures) that also don't exist yet. Nobody can currently run KC-04-2 as specified.

**5. "AC-7 — the plan is followable by a human" is explicitly non-automatable, and the Definition of Done makes it a hard prerequisite ("board photographed and run transcript attached") — but nothing in the issue defines who qualifies as "a person who has not seen the design," what happens on a failed build, or how many independent build attempts are required before the criterion counts as met.** A single successful build by one cooperative, technically literate human is weak evidence that a random first-year student can follow the plan; the issue calls this "the only criterion that tests whether the wiring list is usable" and then specifies it as a one-shot, ungraded procedure with no sample size or failure-handling protocol. This is exactly the kind of acceptance criterion that can be satisfied by a friendly, motivated tester while still shipping a wiring list that's unusable for the actual target audience (first-year students).
**Recommendation:** specify a minimum n (e.g., 2-3 independent builders with no prior exposure to the design) and a definition of "followable" failure (e.g., any wiring error attributable to plan ambiguity fails the criterion).

**6. Scope-creep risk baked into "one trajectory" framing is asserted, not enforced by any test the issue itself owns.** D9/K9 ("progressive disclosure is a system property... the first-year palette must be byte-identical with the breadboard view off") is correctly flagged in §3 as a *joint* invariant that "no single feature's tests detect," and AC-2 bolts on a palette-contract extension to patch this. But the mechanism is additive test coverage on a test file (`PaletteContractTest.java`) that already exists and is owned by no single required feature — the issue never assigns ownership of *maintaining* this invariant across FEAT-043's implementation, so regressions are only caught if whoever writes FEAT-043 remembers this issue's prose. This is a real, self-acknowledged risk with a weak mitigation (a shared test file, not a build-time gate).

**7. Feasibility: this is an 11-feature, 86-132 "maintainer-week" dependency chain gated behind two admittedly-unfinished 5800-line refactors (`SimpleEditor` decomposition, #316) and a headless mutation layer whose apply() signature still takes `Graphics` today** (`src/jls/collab/op/CircuitOp.java:51`, verified: `void apply(Circuit circuit, Graphics g) throws OpRejected;`). The issue is honest about this ("the application entry point still takes a graphics context") but the scale of prerequisite work — a second full canvas, a four-state value core, a strength lattice, a new file-format section with backward-compat skipping, and a package/pinout library, all before a single board can be built — makes this one of the largest single asks in the tracker with no partial-delivery milestone before Tier A (the "demo slice") is reached. The Kill Criteria (KC-04-1 through KC-04-5) are well-designed abort conditions, which is a genuine strength, but they don't reduce the up-front cost of getting to the first checkpoint.

**8. Internal cross-issue dependency risk: the "single owner" resolution for the shared package-layer contract (#298) is asserted but unverifiable from this issue alone.** §3 states "#298 stays in `related`... any `REPLAN:` touching #349 or #365 still lands on this issue as well as on #298 and #307" — i.e., correctness depends on discipline across three separate issues staying synchronized by convention (a shared `REPLAN:` posting habit), which is exactly the failure mode ("two libraries that agree by convention") the same section says it's trying to avoid for the *code*. The issue has not eliminated that failure mode for the *governance* layer, only for the artifact.

## What's solid (one line each)

- The `blocked_by`/`blocks` DAG walk in §2 is genuinely re-derived from the eleven features' own machine blocks and is internally consistent with what's checked (verified structurally, not spot-checked against all 57 issues here).
- The code citations that *are* checkable against this repo are accurate: `LogicElement.java:473/480` (undriven-input-is-0 semantics), `Put.java:16-17` (sealed `Input`/`Output` hierarchy), `ElementRegistry.java` (35 registered types, confirmed by count), `SimpleEditor.java` (5852 lines, confirmed), `CircuitOp.java:34-51` (sealed interface + `Graphics`-taking `apply`, confirmed), and the `src/jls/hdl/board/*.java` file sizes (98/199/125/159 lines, confirmed) all match this checkout exactly.
- The three-way falling-back "residual" bookkeeping for #315/#316/#337 (shipped-part-as-Background vs. remaining-scope-as-plan) is a reasonable way to avoid issue-number churn.
- Kill Criteria are concrete, numeric, and would actually stop wasted work if triggered — better than most acceptance-criteria sections in this tracker.
- The minimality argument in §2 ("remove FEAT-X and step Y becomes unsatisfiable") is genuinely argued per-feature rather than asserted wholesale.

## Verdict rationale

The issue is procedurally the most rigorous artifact in this tracker's genre — but "needs-rework" rather than "sound-with-concerns" because two of the findings (1: the cost/evidence base is unverifiable in this repo today, and 2: core deliverables like the `-breadboard` flag and the SAP-1 fixture are real uncosted work smuggled into an existing feature's step) are not cosmetic. They mean a team picking this up today cannot verify the budget it's being asked to commit to, and would discover mid-flight that "Tier A step 1" requires building things the required-feature table never priced. Fix the evidence-branch merge and re-cost the fixture/CLI work explicitly before treating the Cost and Definition-of-Done sections as actionable.
