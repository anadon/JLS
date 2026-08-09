# Issue #311: CAP-16: a decade of Logisim-Evolution course material opens in JLS as working, readable circuits with every loss named
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

#311 is a "capstone" tracking/coordination issue: it carries no code-level
work of its own and closes only when six required feature issues (#314,
#315, #323, #340, #342, #369) and six ordering-blocker issues (#316, #319,
#320, #321, #347, #349) — twelve issues total, all currently open — land,
plus a corpus measurement, a licence answer from a third party, and a
30-50 "maintainer-week" (mw) cost band clear. The goal (open a `.circ`
file, report every loss, replay vectors, score layout) is legitimate and
several of its sub-requirements (XXE hardening, the migration-report
totality test, reuse of `NetlistImporter`'s shape) are genuinely solid
engineering. But the issue as filed has structural problems severe enough
that it should not be treated as ready to schedule work against.

## Findings, most severe first

**1. The evidentiary basis this issue's §2/§5 cross-checks depend on does not exist in the reviewable repository.**
§2 and comment `5161369861` repeatedly cite `docs/plan/capstones/CAP-16-logisim-evolution-migration-parity.md:62-73` and each required feature's own `docs/plan/features/*.md` as the independent third source that makes the "twelve gradings, three sources, no conflict" claim checkable. I ran `ls docs/plan` against the checked-out tree: `No such file or directory`. The issue's own comment `5171442518` (2026-08-03) confirms this is not an oversight: "citations into `docs/plan/**`... cannot be re-pinned at all — those 195 files do not exist on `master`." So the primary corroborating evidence for the required-vs-beneficial split, and for the competing FEAT-025 cost estimate, is permanently unverifiable by anyone who only has the repository — it lived on a branch that, per that same comment, "will not be merged and will be deleted." A capstone whose sufficiency argument leans on documents nobody can read is not independently checkable; it is asserted.
**Recommendation:** either restore the cited planning corpus into the repo (e.g. `docs/plan/`) before this issue is treated as gating anything, or strike every citation into it and re-derive §2/§5's claims from artifacts that actually exist on `master`.

**2. `evidence_commit` is pinned to a doomed commit, and the body's own line citations are already stale on `master`.**
The body's `evidence_commit: 2d0ca9d...` and its citation `src/jls/hdl/HdlExporter.java:83-84` do not match `master` (`bd54461`), where the same text sits at `:78-79` — confirmed by direct `sed -n` read of the file. The issue's own comment thread patches this by saying "re-read by content, not by line number," but the *body* — which is what an implementer opens first — was never edited to match. The Completion Criteria checklist itself requires "Every cited evidence document and permalink resolves on the default branch at close," a bar several of the issue's own citations already fail today. This is a live example of the gap between "looks rigorous" (a YAML machine block, line-pinned citations) and "is rigorous" (citations that resolve).
**Recommendation:** re-pin `evidence_commit` to a `master` commit and re-verify every `file:line` citation against it before this issue is worked, not only "at close."

**3. Feasibility: 30-50 maintainer-weeks (confirmed unit via #323: "maintainer-weeks") for one of nineteen declared capstones, on a project ARCHITECTURE.md itself calls a "single-maintainer pedagogy tool" (`ARCHITECTURE.md:242-244`).**
That is 7-12 months of one person's full-time effort for this capstone alone, and the issue's own Cost section carries two cost bands that disagree by up to 2x on the dominant line (FEAT-025: 6-12 mw vs. 12-18 mw), instructing the reader to "treat the higher band until the two-day measurement's run" — a measurement that, across four comments spanning 2026-08-02 through 2026-08-04, has still not been reported as run. Nothing in the issue weighs this cost against the other eighteen capstones' combined demand on the same single maintainer, and nothing revisits whether a capstone this large should be one GitHub issue rather than its own tracked initiative with a checkpoint before the full band is committed.
**Recommendation:** run the promised two-day corpus measurement before treating any part of the 30-50 mw band as committed, and add an explicit relative-priority statement against the other eighteen capstones so a single maintainer has a basis for sequencing.

**4. #311 itself is not actionable — it is a pure bookkeeping wrapper, and its "Completion Criteria" prove it.**
Every checklist item under "Completion Criteria" is about other issues closing, machine blocks staying internally consistent, or evidence "resolving" — none is a testable engineering deliverable owned by #311 itself. An implementer cannot make progress on #311 directly; #369 alone (FEAT-053) carries a 14-issue transitive `blocked_by` closure per §2's own DAG walk. This means #311 functions as a label over twelve other open issues, several already multi-week efforts in their own right, rather than a schedulable unit of work. That is a legitimate role for a tracking issue, but the issue is filed with the full weight of an implementation spec (outcome statement, acceptance criteria, kill criteria) despite having no work of its own to do — which invites someone to spend review effort on an issue that cannot itself be closed by writing code.
**Recommendation:** either relabel this explicitly as a pure tracking/rollup issue (and slim its spec accordingly), or extract a genuinely closeable slice (e.g. the "increment 1, 3-5 mw" structural-subset import named in Cost) as the actual actionable issue, with #311 kept as the umbrella.

**5. The licence gate is wired backwards: the higher-risk default does not trigger the kill criterion.**
Open Question 1/2 recommends, by default, *absorbing* Logisim-Evolution's GPLv3-only (no "or later") port-geometry logic "and record the licence consequence" — which the issue itself says "changes JLS's own licence posture project-wide." Yet KC-16-5 only fires "if upstream's licence answer is GPL-3.0-only and the maintainer is unwilling to give up 'or later'" — i.e. the kill/re-cost path requires an explicit refusal, while the *default* recommended action (absorb now, ask later) is the one path that does not require the maintainer to consciously accept the consequence before it happens. A one-way, project-wide licence downgrade should be the thing that requires affirmative sign-off, not the thing that happens unless someone actively objects.
**Recommendation:** invert the default — require the "or later" answer from upstream *before* any geometry absorption lands, not merely "recommended... before any absorption" in prose that a later implementer can read past.

**6. AC-1/KC-16-1's corpus requirement raises a copyright question the issue never asks, despite being unusually careful about the adjacent software-licence question.**
AC-1 and KC-16-1 require committing "at least 30 `.circ` files drawn from at least 3 independent public course repositories, listed with their provenance in the same commit as the table" into this repository. The issue devotes an entire Open Question (and a kill criterion, KC-16-5) to whether absorbing Logisim-Evolution's *software* licence is safe, but says nothing about the copyright status of the *course material itself* — an instructor's or student's `.circ` lab files, typically not licensed for redistribution, being copied wholesale into a public GPL repository's test corpus. This is a distinct legal exposure from the one the issue spent the most words on.
**Recommendation:** add an explicit provenance/licence check for each corpus file (e.g. restrict to files under an OSI/CC-BY-style licence, or get affirmative permission) before AC-1's corpus is committed.

**7. Gameable acceptance criterion: KC-16-1's "load-bearing for connectivity" threshold has no operational definition.**
KC-16-1's trigger is "more than 50% of those files contain at least one construct outside the increment-1 subset... **that is load-bearing for connectivity**." Whether a given construct is "load-bearing for connectivity" in a given file is left to implementer judgement, with no independent check specified. An implementer under schedule pressure to avoid triggering a re-cost/kill event has a direct incentive to classify constructs as non-load-bearing.
**Recommendation:** replace with a mechanical test, e.g. "the net partition of the naive per-file reading differs from the reading with the construct present," so the 50% figure is computed the same way by any reader.

**8. AC-6 cannot be evaluated as filed — the floor it references does not exist yet.**
"Observe: scores above the declared floor, with the floor and the rubric committed" — but the rubric and floor are explicitly deferred to #342's residual scope, not owned by #311. This is disclosed (the issue is honest that FEAT-022's residual is the owner), but it means AC-6 is currently a placeholder criterion that cannot gate anything until a sibling issue supplies a number this issue does not have.

## What holds up

- Every independently-checkable technical citation in the body matches the current `master` checkout exactly: `grep -rli logisim src/` → 0, no `javax.xml`/`DocumentBuilder`/`XMLStreamReader`/`org.w3c.dom` in `src/`, the `ShiftRegister`/#122 comment in `HdlExporter.java`, `docs/file-format.md`'s "Unknown attribute names are silently ignored" line, and the exact line counts for `NetlistImporter.java` (1,067), `ImportResult.java` (52), `ImportSummary.java` (102), `ImportException.java` (32) and `SimpleEditor.java` (5,852). The line-number drift noted in Finding 2 is real but narrow; the substance of every citation I checked is accurate.
- AC-5 (XXE hardening, one test per named vector) is concrete, testable, and correctly ship-blocking given this is genuinely the first XML parser in the codebase — a well-scoped security requirement.
- AC-2's falsification requirement ("the test must be shown red against a build with the report suppressed... recorded") is a real anti-gaming discipline against a vacuously-passing "nothing is dropped" test, and is exactly the kind of check this fleet is looking for elsewhere and often not finding.
- The name-collision hazard (KC-16-3, `ShiftRegister` mapping combinational-by-name onto the source tool's sequential component) is a correctly-identified, concrete correctness trap grounded in shipped code, not a hypothetical.
