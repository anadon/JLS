# Issue #440: TASK-0019: the editor decomposition is a written plan measured against HEAD, and jls.edit stops being the one package whose coverage nothing defends
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The task is small and well-bounded in code terms — write one plan doc, add
one JaCoCo `PACKAGE` rule, add an exemption list, add one inventory test,
delete two paragraphs of prose — and touches no production Java. Nearly
every factual citation I could check against this checkout resolved exactly
(package counts, floor values, file existence, quoted prose, line numbers
for `SimpleEditor.java:155-156`, `:1121`, `:2306-2354`). That accuracy is
real work and should be credited. The concerns below are about the
evidence trail's own integrity, the dependency graph's currency, and a
process-cost mismatch between the deliverable's size and the specification's.

## Findings, most severe first

**1. `evidence_commit` does not exist in this repository's history — the citation apparatus is unverifiable by the mechanism the issue itself prescribes.**
`git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails ("could not
get object info"), and `git log --all --oneline | grep 2d0ca9d` returns
nothing against 268 total commits in this (non-shallow: `git
rev-parse --is-shallow-repository` → `true`, but still checked — see caveat
below) repo. The issue's own "supersession check" discipline (rule 6, cited
throughout §1 and §11) depends on a contributor running `git show
2d0ca9d:path` to re-derive every O1-O10 citation before trusting it; that
command cannot succeed here. The companion hash `839fb3a` (cited as "HEAD
at filing") is equally unresolvable (`fatal: Not a valid object name`).
Every observation in the issue happens to match current HEAD byte-for-byte
in the places I spot-checked (package-info count = 18, floored count = 4,
`SimpleEditor.java` = 5852 lines, `grep -ci exempt` = 0 in both files,
CONTRIBUTING.md:83-108 quoted verbatim, `test/jls/ui/` = 34 files / 25
`@Tag("display")`), but that agreement is coincidental from the reviewer's
vantage point, not something the issue's own verification instructions
could have confirmed — a contributor following rule 6 literally has no
commit to check against and must fall back to trusting HEAD, which the
issue nowhere licenses as a substitute.
*Caveat:* the repo is a shallow clone in this sandbox, so it is possible
the missing commit is merely unreached history rather than fabricated;
either way, the practical effect for whoever picks this up in this
environment is the same — the pinned commit is not resolvable, and the
issue should say what to do when it isn't (fall back to HEAD, note the
divergence, etc.) rather than silently assume `git show <hash>:` always
works.
**Recommendation:** either pin to a commit verified reachable in the
canonical clone the executor will use, or add an explicit fallback
instruction ("if `2d0ca9d` is unreachable, re-derive against HEAD and
record that substitution in the PR") so rule 6 has a defined failure mode.

**2. The dependency graph the issue draws is already stale, by its own numbering scheme, five days after filing.**
§ Status & Dependencies states "**TASK-0020** ... None of those four is
filed yet; a link pass adds the edges" and the mermaid graph shows
`T0020["TASK-0020 ... (filed concurrently)"]` as a peer node. In fact
TASK-0020 *was* filed concurrently as issue #441 (created 2026-08-03,
four minutes after #440) — but #441 was closed 2026-08-08 as a duplicate,
superseded by #84 ("Lower number wins, so #84 survives and this one closes
into it... Migrated to #84 in full"), which now carries the
`blocked_by: [440]` edge that #441 declared. So the concrete issue that
"blocks" on #440 today is #84, not a not-yet-filed TASK-0020 — a fact
#440's own text and diagram do not (and structurally cannot, being a
snapshot) reflect. This isn't fatal — parent feature #316 already shows
`I84 -.-|does the extraction #84 tracks| T0020` — but an executor picking
up #440 today who trusts #440's own "None of those four is filed yet"
line will look for the wrong successor issue.
**Recommendation:** when this issue is worked, re-check `blocks`/`related`
against current issue state (exactly the rule-6 discipline the issue
demands of its own *observations*) before relying on the dependency prose.

**3. Specification overhead is disproportionate to the deliverable's actual complexity, which is a real cost/feasibility risk.**
The four "must be built" artifacts (§6) are: a Markdown plan, one
`PACKAGE` rule in `pom.xml`, an exemption data structure, and one JUnit
test with two private helpers. That is a small, mechanical task. The issue
spec is nonetheless ~14,000 words, including LaTeX set-theoretic notation
in §7.10 for what is operationally two `comm -23` invocations (§7.10 Stages
1-3 formalize "list packages minus floored packages minus exempt packages"
as $F \cup X = P$), four falsifiable hypotheses, seven predictions, a
concurrency-model section for a build-time-only JUnit test that "touches
no shared mutable state, holds no lock, and starts no thread," and 19
top-level Definition-of-Done checkboxes. None of this is *wrong*, but the
ratio of process artifact to code artifact is the highest-cost element of
the issue: an executor (human or agent) will spend more effort satisfying
the specification's bookkeeping (recording two `mvn clean verify` runs
with JDK version, pasting red/green P4 output, reconciling the mermaid
graph, resolving four Open Questions) than performing the actual change.
**Recommendation:** none required for correctness, but worth naming as a
real cost before assigning this — the effort estimate should account for
the paperwork, not just the four artifacts.

**4. P4's anti-vacuity check is easy to satisfy without actually validating the floor's tightness.**
§8 says: "Make a deliberate coverage-lowering edit to `jls.edit`, run `mvn
clean verify`, record the red output... revert the edit." Nothing in P4,
§9, or the Completion Criteria requires the induced drop to be *close* to
the floor's epsilon margin — an executor can delete one already-marginal
test method in the single best-covered class, produce a large, easy red
run, and technically satisfy every checkbox, while never exercising
whether the chosen epsilon (O9: "at least 0.5-1.0 point") actually catches
a realistic small regression. A floor that only trips on gross deletions
but not on the kind of regression #37 (the dead-popup bug motivating this
whole task) exemplifies would pass every stated acceptance check.
**Recommendation:** require the P4 edit to be a *small*, single-method or
single-branch deletion representative of a plausible accidental regression,
and require the PR to state how close the induced red run came to the
floor (not just that it went red).

**5. Minor: the `jls.edit`-floor Open Question defers a decision the issue's own H1 falsification branch treats as load-bearing, without a hard deadline.**
Open Question 1 ("headless or display-inclusive basis") is marked "Blocks
execution," which is correct, but H1's falsification outcome ("do not
force a floor at ~0%... keep `jls.edit` in the exemption list... make the
floor a completion criterion of TASK-0020/TASK-0021 instead") is stated as
acceptable and still closes P1/P2/P5/P6. That is a legitimate designed
escape hatch, not a defect — flagging only because a lazy executor could
lean on "H1 refuted" as a way to avoid ever adding the floor while still
checking every box, and the issue does not require the headless
measurement itself to be published anywhere if H1 is refuted (only that
the exemption reason states the number). A reviewer of the resulting PR
should specifically demand the raw measured triple even in the refutation
branch, since the issue's Data Collection section (§9) already asks for
it but Completion Criteria doesn't cross-reference that specific
requirement back to the H1-refuted path explicitly.

## What's solid (one line each)

- Every quantitative claim I could independently check (18 packages, 4
  floored, 0 exemption-list hits, `SimpleEditor.java` = 5852 lines, the
  four existing floor values, `test/jls/ui/` = 34/25) matched the actual
  repository exactly.
- Scope discipline is good: explicitly excludes UndoManager/Palette/EditOp/
  the registry-driven toolbar from re-planning, and these are verified
  already extracted (`UndoManager.java` 239 lines, `Palette.java` 264,
  `EditOp.java` 155, `makeElements` at `:2306-2354` — all confirmed).
- No production Java is touched, which caps the behavioral-regression
  blast radius to essentially zero regardless of how the plan/floor/test
  work turns out.
- The measurement-basis trap (O8) and the slash-vs-dot-form silent-match
  trap (O10) are both real, previously-lived hazards in this repo's own
  history (the #233 incident is genuine, confirmed in `pom.xml`'s existing
  ratchet comments) and the issue correctly refuses to let either slide.
- CONTRIBUTING.md and pom.xml quotes throughout the issue (epsilon rule,
  headless-only rule, the `jls.edit` exemption prose) are verbatim-accurate
  against the current files.

## Verdict rationale

`sound-with-concerns`: the code-facing scope is small, well-evidenced, and
low-risk, but the issue's evidence trail depends on a commit hash this
checkout cannot resolve, its own dependency graph is already out of sync
with the issue it names as a concurrent filing, and the specification's
process weight is large relative to the actual deliverable — none of which
blocks the work, but all of which should be corrected or acknowledged
before or during execution rather than discovered by the executor.
