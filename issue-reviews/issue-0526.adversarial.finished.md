# Issue #526: FEAT-C21-3: a repo generated from the in-tree Classroom starter grades itself on push — the jls-grade Action annotates failing tests on the exact circuit files and reports Classroom points
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being proposed

A GitHub Classroom Action (`jls-grade`) plus a starter-repo template: on push, run the pinned JLS build against hidden test vectors, place Checks-API annotations on the failing circuit files, and report Classroom points, with a cached pinned build. Serves CAP-21 (#502) PF-3, ordered after FEAT-C21-1 (#524, the frozen CLI contract), transitively behind FEAT-053 (#369) / TASK-0111 (#466).

## Findings, most severe first

### 1. (High) Two of the five acceptance criteria cannot be verified from this issue alone, and the missing piece isn't in `ordering_after`

AC-1 grades "a fixture-class submission," and AC-3 requires the Action's summary to be "byte-identical to the other adapters' score vectors from the same xUnit input (feeds CAP-21 AC-1)." The issue's own comment concedes this: *"AC-3 here is marked '(feeds CAP-21 AC-1)' — this adapter supplies an input, #531 performs the comparison."* But #531 (FEAT-C21-6, the hermetic 300-submission fixture and `CrossPlatformScoreParityTest`) is the thing that actually builds the fixture-class corpus and runs the four-way diff — and #531's own scope says it runs "containerized in CI against every adapter," i.e. it needs #526 (and #525/#528/#530) to already exist. Neither issue's `ordering_after` records this edge in either direction (#526 lists only `["FEAT-C21-1"]`; #531 lists only `["FEAT-C21-1"]` too). So: whoever picks up #526 either (a) has to invent an ad hoc "fixture-class submission" to satisfy AC-1/AC-3 that may never reconcile with #531's canonical corpus, or (b) cannot actually close AC-3 until #531 lands, in which case #526's own AC list is partly not this issue's to satisfy. As written, #526 can be marked "done" against criteria it cannot itself check.
**Recommendation:** either drop AC-3 from #526 entirely (it's CAP-21 AC-1, already tracked on #531) and leave only "supplies xUnit input in the documented shape" as this issue's criterion, or add the `#531 blocks #526`/`#526 blocks #531` edge explicitly and say which fixture corpus AC-1 uses before #531 exists.

### 2. (High) Hidden-vector leakage through the annotation channel is a real risk and is entirely unaddressed

The outcome text says the Action grades against "the lab's hidden vectors." But the xUnit report this issue consumes (`GradeReport`, defined in #466 §7.6) puts "the expected and observed values" into each failing `testcase`. If the Classroom Action turns those failures into check annotations visible on the student's own commit/PR (which is the entire point of "annotates failing tests on the exact circuit files"), a student can push, read the annotation, see the expected value for a hidden vector, and hardcode it — repeating per vector until the "hidden" suite is fully exposed. Gradescope's sibling issue (#525) explicitly names a "visible/hidden test split" in its AC-1; #526 never mentions visible/hidden at all, for either the Classroom points report or the annotations. Given the two adapters are supposed to produce "byte-identical" score vectors from the same input (AC-3), this is also an internal inconsistency in the sibling set: #525 treats visible/hidden as a first-class concern, #526 doesn't.
**Recommendation:** add an acceptance criterion requiring hidden-vector failures to be redacted (pass/fail + which watched element, no expected/observed values) in the annotation surface, and state explicitly what "Classroom points" exposes vs. what stays server-side.

### 3. (Medium-High) "Annotates failing tests on the exact circuit files" collides with the repo's own documented default file format

`README.md`'s "Circuit files" section states plainly that current JLS writes `.jls` as XZ-compressed binary by default (*"Despite the plain `.jls` name, these files are XZ data"*), with plain-text save as an opt-in (`-savetext` / File > Save As). GitHub's Checks API renders annotations against a file's rendered diff/text view; a binary XZ blob has no such view, so a check annotation "on the exact circuit file" degenerates to a file-level pointer at best, not the line-level annotation the phrase implies. The issue never states that the starter template must force plain-text saves (or that the Action must decompress-then-reference a synthetic path) — this is a load-bearing precondition that's simply missing.
**Recommendation:** state explicitly in the boundary notes that the starter template pins plain-text `.jls` saves (or documents the annotation as file-level only), and add a criterion pinning which of the two it is.

### 4. (Medium) The entire dependency chain this issue sits on is unbuilt at HEAD, and the issue's own 2-3 mw band only covers the adapter shim

Verified directly: `docs/batch-interface.md` §1 at HEAD lists exactly three exit statuses (0/1/2) and no xUnit output; `git grep`-style checks cited in #466 confirm no `Expectations`/`Assert`/`Cover`/`-check`/`-report` exist in source. #526 correctly declares `ordering_after: ["FEAT-C21-1"]` and notes it's "transitively behind #369/#466," so this isn't a contradiction — but #369 alone is costed at a 9-15 mw band with its own unfiled sub-tasks (TASK-0021, TASK-0112) and open ordering prerequisites (#316, #321, #347), before #524's 2-3 mw and before #526's own 2-3 mw even start. The visible cost figure on #526 badly understates how much unscheduled, unstarted machinery has to land first; there's no comment on #526 acknowledging this "the runway isn't built" state, unlike #502 which does discuss it (blocked_by/kill criteria).
**Recommendation:** not a defect in #526's own scope, but the issue should carry a one-line pointer to #502's blocked_by/§5 for a reader who lands on #526 without first reading the capstone — right now a reader could reasonably think "band_mw 2-3" is the real cost of shipping this feature.

### 5. (Low-Medium) No mention of the GitHub Actions permission needed to write check annotations

Posting Checks API annotations from a workflow requires `permissions: checks: write` (or equivalent) on the `GITHUB_TOKEN`; GitHub Classroom-generated repos and their default workflow permissions vary by org/Classroom configuration and this is a common source of "Action ran, annotation silently didn't appear" bug reports in similar tooling (Autograding Action, etc.). Not called out anywhere in the acceptance criteria or boundary notes.
**Recommendation:** add to the boundary notes, or fold into AC-1 ("...and the check-run permission is documented in the template's workflow file").

### 6. (Low) "The exact circuit files" (plural) has no clear referent

`docs/file-format.md` confirms subcircuits nest as `CIRCUIT` blocks *inside* one file, not as separate files on disk — JLS has no multi-file circuit format. A typical lab submission is a single `.jls` file, in which case "annotates failing tests on the exact circuit files" reduces to "the one file," and the plural either implies multi-lab repos (several `.jls` files per assignment) that are never described, or is just loose phrasing carried over from the capstone's language. Minor, but worth pinning down before someone designs a data model around a plurality that may not exist.

## What's solid

- Correctly scopes marketplace publication (Open Question 3 on #502) as blocking shipping only, not development — a clean, non-blocking deferral that keeps the issue actionable now.
- The "files-only kit: no JLS-operated service anywhere (#498 §8 exclusion 7)" citation checks out against the actual exclusion text ("A server, a network dependency, an install step, or a plugin execution surface ahead of demand").
- "Pin and cache a specific JLS build" is grounded in real, already-shipped infrastructure — `ghcr.io/anadon/jls` is a documented, existing multi-arch container image built for exactly this batch/CI use case (README, "Container image (batch mode only)"), so this isn't a green-field integration.
- AC-4 (recorded-artifact-only, never an interactive session, citing #498 §7.2) matches the actual, carefully-argued recorded decision in #498 §7.2 — accurate citation, not a stretch.
- AC-2's "never an interactive session" boundary and AC-5's "drift surfaces in CI, not a live course" both mirror real, load-bearing risk language from the parent capstone (#502 risk 1, KC-21-3) rather than inventing new claims.

## Verdict rationale

Two structural problems (the untestable cross-adapter AC and the hidden-vector leakage gap) are significant enough that a team picking this up today would either produce a security-relevant regression (leaking hidden test answers to the student being graded) or discover mid-implementation that AC-3 has no way to close without a sibling issue nobody has scheduled a dependency on. Combined with the unstated plain-text-save precondition for annotations to even be meaningful, this needs rework before implementation starts, though nothing here suggests the feature should be killed — the scope, exclusions, and citations to #498/#502 are otherwise sound.
