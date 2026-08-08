# Issue #745: TASK-C544-5: the NVDA path ships as documentation and a manual checklist, stated as manual and never counted as automated coverage
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The instinct behind this task is sound and, unusually for this fleet, already has in-repo precedent: `docs/standards-adoption/03-accessibility-conformance.md` independently argues for exactly this kind of dated, re-runnable manual AT checklist and warns against ACRs "written from the test suite and never from a screen reader." But the task's `ordering_after` field is inconsistent with what its own acceptance criteria require, it never engages with a documented, currently-true packaging defect that would make the checklist unusable against the shipped Windows installer, and "executable and dated" has no verification mechanism beyond trusting whoever fills in the file.

## Findings, most severe first

### 1. (High) `ordering_after` names only TASK-C544-2, but AC1 and AC4 require TASK-C544-3 and TASK-C544-4 to exist first

The YAML front matter declares `ordering_after: [TASK-C544-2]` (#739, element-graph traversal only). But:

- AC1 requires the checklist to cover "element traversal, **connection announcement and simulation state announcement**." Signal-state announcement is TASK-C544-3's (#741) entire scope — not built by #739.
- AC4 requires "No claim in the checklist is stronger than what **the Orca automation asserts** for the same capability." The Orca automation that asserts anything — including state-change speech — is `OrcaLabSessionTest`, which is TASK-C544-4's (#743) deliverable, explicitly `ordering_after: [TASK-C544-3]`, i.e. two tasks downstream of what #745 declares as its only dependency.

So a contributor who honors the stated ordering can start #745 as soon as #739 lands, write a checklist row for "simulation state announcement," and have literally nothing to check it against — #741 hasn't shipped the capability and #743 hasn't shipped the automated assertion AC4 says the checklist must not exceed. The dependency chain the sibling tasks themselves establish (#737 → #739 → #741 → #743) is real and documented in each of those issues' own `ordering_after`; #745 is the one task in the set that doesn't follow it.

**Recommendation:** change `ordering_after` to `[TASK-C544-3, TASK-C544-4]` (or at minimum `[TASK-C544-4]`, since it's the one whose assertions AC4 references), and drop the "simulation state announcement" row (or gate it explicitly) until #741/#743 exist.

### 2. (High) The task never engages with the documented Windows Access Bridge gap that would make the checklist fail on the shipped artifact it's implicitly meant to validate

`docs/standards-adoption/03-accessibility-conformance.md:130-186` documents, with a file/line citation, that `scripts/build-installer.sh`'s `jdeps --print-module-deps` derivation of the jlink module set can never include `jdk.accessibility` (nothing in the jar statically references it), so **every currently-produced Windows `.msi` bundles a runtime with no Java Access Bridge — NVDA gets nothing at all from an installed JLS.** Only `java -jar` on a full external JDK works today. #745's acceptance criteria say nothing about which build a checklist run must target, and nothing about this prerequisite fix landing first. A tester who does the obvious thing — install the released `.msi` and run NVDA against it, exactly as a blind student would — gets silence on every row, not because the checklist or the underlying feature is wrong, but because of an unrelated, already-diagnosed packaging bug this issue doesn't cite, order after, or even acknowledge exists.

**Recommendation:** either add the Access Bridge fix (or its tracking issue, once filed) to `ordering_after`, or have the checklist's own preamble state explicitly which artifact it must be run against (`java -jar` on a full JDK, not the `.msi`) until that fix ships — and say so as plainly as AC criterion 4 asks the automated/manual boundary to be stated.

### 3. (Medium) "Executable and dated" (AC3) has no mechanism forcing the record to reflect a real session

AC3: "The checklist is executable and dated: a run records the NVDA version, the Windows version and the outcome per row." Nothing enforces that the recorded run actually happened against a live NVDA session rather than being filled in from what the author expects NVDA to say (a real risk given finding #2 — if the tester never gets the bridge working, the path of least resistance is to write plausible-sounding "PASS" rows rather than document total silence). `docs/standards-adoption/03-accessibility-conformance.md` names this exact failure mode for the sibling ACR effort ("Mitigation: no ACR row may be rated above 'Not Evaluated' ... without a dated AT session") and proposes a parallel `docs/accessibility-at-checklist.md` with per-platform, per-release audit logging. #745 doesn't cite that precedent or adopt its discipline (e.g., requiring a transcript, a screenshot, or an AT-SPI/UIA capture alongside the date, the way TASK-C544-1's AC4 requires "the captured (or absent) announcement is committed as evidence, so the verdict rests on a transcript rather than on an impression"). As written, a checklist with fabricated or optimistic dated rows satisfies AC3's literal text.

**Recommendation:** borrow TASK-C544-1's own evidence bar — require a committed artifact (screen recording, NVDA speech log, or equivalent) alongside each dated run, not just a table cell.

### 4. (Medium) Unclear whether this duplicates the multi-platform checklist already planned elsewhere in the tree

`docs/standards-adoption/03-accessibility-conformance.md`'s "What only a human can do" section already specs a single `docs/accessibility-at-checklist.md` covering Orca (Linux), NVDA/JAWS (Windows), and VoiceOver (macOS) in one document with one per-platform-per-release row format. #745 asks for "an in-tree NVDA checklist" as its own artifact, with no file path given and no reference to that existing plan. It's unclear whether #745's deliverable *is* the NVDA rows of that planned document, a standalone file that will need reconciling with it later, or a third thing. Two independently-evolving checklists for the same underlying AT session is a realistic outcome if both pieces of work proceed without one citing the other.

**Recommendation:** either point AC1 at `docs/accessibility-at-checklist.md` explicitly as the target file, or state why a separate NVDA-only document is preferred, and cross-link the two.

### 5. (Low) AC4's equivalence test ("no claim... stronger than what the Orca automation asserts") is a same-content assumption across two different AT stacks with no stated basis

Orca (AT-SPI2/ATK) and NVDA (Java Access Bridge/IAccessible2) consume the same underlying `AccessibleContext` data through different bridges and have their own verbosity/phrasing conventions; "no claim stronger than Orca" is a reasonable ceiling on *capability* (don't claim NVDA hears something Orca-verified capability doesn't produce) but is easy to misapply as *literal transcript* equivalence, which the two AT stacks won't actually produce. The issue doesn't disambiguate "same capability" from "same wording," and a checklist author without a working NVDA session (see finding #2) has every incentive to just copy Orca's asserted strings into the NVDA rows, which would misrepresent NVDA's actual output the first time anyone checks.

**Recommendation:** state explicitly that parity is at the level of information conveyed (name/role/connection/state), not verbatim phrasing, and that each NVDA row's "expected spoken output" column must be written from an actual NVDA session, not derived from the Orca test's assertion strings.

## What's solid

- The core goal — "Windows screen-reader users are served honestly rather than over-claimed" — is a real, well-motivated outcome, and stating the automated/manual boundary "in the document itself" (AC2) is exactly the right discipline for a self-asserted VPAT input (#547 depends on this document not overclaiming).
- Scoping NVDA to documentation-plus-checklist rather than pretending it's automatable in this project's CI is honest and matches the platform reality (Windows CI lane has no scripted screen-reader capability in this repo).
- `band_mw: 0.5` is plausible sizing for a documentation-only task, assuming its actual prerequisites (see finding #1) are already done by the time it starts.

## Bottom line

The task states a good, narrow goal, but its declared ordering doesn't match what its own acceptance criteria need, and it's silent on a documented, concrete reason (the missing `jdk.accessibility` module in the Windows installer) the checklist could be executed in good faith today and still produce nothing. Fix the `ordering_after` list and either gate on or disclose the Access Bridge gap before this is workable as scoped.
