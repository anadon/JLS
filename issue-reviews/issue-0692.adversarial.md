# Issue #692: TASK-C524-4: the verdict envelope is byte-identical across container boundaries — no timestamp, no ordering wobble, no locale in a grading artifact
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings

### 1. [High] The parent feature's own body, and all three of its acknowledged children, do not know this task exists — and its cost silently blows the parent's budget

#692's machine block declares `part_of_feature: 524`, but #524 (FEAT-C21-1)'s body enumerates the CLI-contract-freeze work only through five ACs, and its own comment thread (`issuecomment-5176015975`, `issuecomment-5227296462`) — both dated the same day as this review — names exactly three children: #686 (TASK-C524-1), #687 (TASK-C524-2), #690 (TASK-C524-3). Each of those three issues' own "Boundary" section cross-references the other two by number and by exact scope (e.g. #687: *"the queryable version is TASK-C524-3"*) but none mentions a fourth task. `get_parent` on #692 returns `{"parent": null}` — there is no GitHub-native link either. #524's own `band_mw: 2-3` sums *exactly* from the three known children (1-1.5 + 0.5-1 + 0.5 = 2-3); the prior review of #524 (`issue-reviews/issue-0524.adversarial.md`) explicitly credits this as evidence the cost accounting is "internally consistent... unlike sibling issue #369." Adding #692's declared `band_mw: 0.5-1` on top pushes the feature's true cost to 2.5-4, exceeding the parent's stated ceiling of 3 by up to a full unit, with no edit to #524 recording it.

**Recommendation:** either register #692 as a GitHub sub-issue of #524 and bump #524's `band_mw` to 2.5-4 (or the correct sum), or — if #692 is meant to supersede/absorb into one of the three known children instead of standing alone — say so explicitly and close the redundant path. As filed, a scheduler reading #524 literally believes the CLI-contract-freeze feature costs 2-3 mw and has three children; #692 contradicts both facts without touching the record that makes the claim.

### 2. [High] AC1 duplicates a completion criterion #466 already owns, with no cross-reference and no arbitration of which issue's test is authoritative

#692 AC1: *"Timestamp, hostname, duration and any other nondeterministic xUnit attribute is either omitted from the envelope or emitted as a declared fixed value, with the choice documented in the contract."* This is, clause for clause, the same commitment #466 (TASK-0111 — the issue that actually builds `GradeReport`) already makes in its own body: §7.6 states the report carries *"no `timestamp`, `hostname` or `time` attributes"*; H5/P7 name the assertion (`theReportContainsNoTimestampHostNameOrDuration()`); Open Question 4 pre-answers the "declared fixed value" fallback (*"fixed documented placeholders... the answer must be in §2.5"*); and #466's own Definition of Done lists, as an unchecked box, *"An emitted report contains no timestamp, host name or duration, asserted directly."* #692 does not cite #466 anywhere, does not say whether its own test consumes #466's assertion or duplicates it, and does not say which issue's closure is authoritative if both ship the assertion independently. This is the identical shape of defect #524's own review process already caught once and had to resolve explicitly by maintainer comment (AC-2 vs. #531's AC-3, `issuecomment-5227296462` §2) — a criterion claimed by two issues, neither closable purely on the other's evidence, with a live risk that "the transcript exists only once" while both issues get marked done against it.

**Recommendation:** state explicitly that #692 *consumes* #466's no-timestamp/no-hostname guarantee as a precondition (and should therefore `ordering_after` #466, not just transitively through #686 — see Finding 3) rather than re-testing it, reserving #692's own scope for the parts #466 does not cover: locale pinning, ordering/map-iteration determinism, path separators, and the cross-container harness itself.

### 3. [Medium-High] The one test AC1 actually needs — running the real grading lab twice — has no direct dependency on the issue that builds the lab

AC1's fixture is *"the same lab twice in two different containers"*; the only "lab" concept in the codebase is #466's `examples/autograde/lab-01/` plus its `-check`/`-report`/`GradeReport` machinery, none of which exists yet (#466 is open; `grep` for `GradeReport`/`Expectations` under `src/`, `test/` returns nothing — only capability-roadmap docs). #692's `ordering_after` names only `["TASK-C524-1"]` (#686, the contract-document task), not #466. #686 does carry `ordering_after: [369, 466]` itself, but #524's own review thread flags that exact edge as defective and only "mirrored" in a comment, never edited into #686's body — so the chain #692 → #686 → #466 currently runs through a link the project's own process has already marked broken. #692 inherits that fragility silently, one hop removed, without naming #466 itself as a prerequisite.

**Recommendation:** add #466 directly to #692's `ordering_after`, independent of whatever #686's edge turns out to be, since AC1's fixture cannot exist without it.

### 4. [Medium] AC1's literal fixture is a nontrivial CI build with no scaffold to reuse, and the declared band likely underprices it

Taken literally, AC1 requires containers that differ in hostname *and* locale *and* timezone *and* user id, running the identical grading lab, with byte-for-byte report comparison. None of the existing workflows (`ci.yml`, `release.yml`, `repro-installers.yml`, `mutation.yml`, `codeql.yml`, `scorecard.yml`) build anything like a parameterized multi-container test matrix. The closest prior art, `docs/reproducibility.md`, is instructive but not reusable as-is: it declares timezone/locale/umask/wall-clock time "verified irrelevant" for the **jar build**, gated by `project.build.outputTimestamp` and JDK/Maven pinning — a build-artifact story, not a runtime-grading-output story, and #692 doesn't cite it. Standing up genuine multi-container variation (distinct uid, locale packages installed, tz database entries, hostnames) inside GitHub Actions is real infrastructure work; `band_mw: 0.5-1` is the same size as #690 (TASK-C524-3), whose scope is a single CLI flag plus one conformance test — a much smaller lift than authoring a cross-container harness from nothing.

**Recommendation:** either scope AC1 down to "two processes with env-simulated hostname/LANG/TZ/user.name overrides on one runner" (cheap, still catches real bugs) and say so explicitly, or size the task to match genuine multi-container infrastructure work.

### 5. [Medium] "Locale-sensitive formatting is pinned explicitly" is satisfied by testing exactly one locale

AC bullet 3: *"running under a comma-decimal locale changes no byte."* As worded this is gameable: an implementer can pin `Locale.forLanguageTag("de-DE")` (or any single comma-decimal locale), pass that one test, and never exercise RTL locales, grouping-separator locales (e.g. `hi-IN`'s non-uniform grouping), or `Locale.TURKISH`'s well-known `i`/`İ` case-folding trap — none of which are comma-decimal but all of which are classic sources of silent locale leakage in exactly this kind of report-writer code. The AC's own framing ("a comma-decimal locale") smuggles in the assumption that decimal-separator swap is the only failure mode worth naming.

**Recommendation:** either broaden the AC to name a small locale matrix (comma-decimal, RTL, and Turkish-i as the three classic traps) or state explicitly that decimal-separator swap is the only locale hazard in scope and why the others don't apply to this report format.

### 6. [Low] "Container boundaries" is undefined and this repo has two live meanings for "container"

The issue title and AC1 both say "container boundaries" / "two different containers" without saying whether this means literal Docker/OCI containers (the repo already ships `ghcr.io/anadon/jls`, referenced in README's "Container image (batch mode only)" section) or is loose shorthand for "different machines/CI runners." The two have materially different achievable guarantees — a Docker image pins libc/locale-data version, a bare GitHub Actions runner does not — so the choice affects what the test can actually prove.

**Recommendation:** state explicitly whether the test targets the shipped `ghcr.io/anadon/jls` image (recommended, since that is the actual autograder deployment vehicle per the README) or arbitrary hosts.

## What's solid

- The overall goal — extending the existing byte-identical-goldens discipline from simulation output to the whole verdict envelope — is a natural and well-motivated fit with this codebase's existing reproducibility culture (`docs/reproducibility.md`'s jar/BOM bit-reproducibility, `BatchSimulationGoldenTest`, `VcdExportGoldenTest`).
- The Boundary line ("what a test *says* is CAP-06's; how it is serialized is here") is the correct scope split in principle and matches #524's own framing of AC-4, even though Finding 2 shows the practical line with #466 is drawn in the wrong place.
- The escape-hatch AC ("any element that genuinely cannot be made deterministic is listed by name... rather than left to be discovered by an adapter") is well-designed: it prevents unbounded goldplating while still forcing an explicit, checkable list rather than silent gaps.
