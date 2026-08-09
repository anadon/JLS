# Issue #517: CAP-33: an instructor adopts JLS by adopting a course, not a tool — a textbook-mapped lab pack, guided lessons, and an assignment starter/submit workflow ship in-tree
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

CAP-33 is well-cited (its `evidence` and `related` pointers to #510 and #509
check out verbatim against those issues) and its scope boundary against
#502/CAP-21 ("CAP-21 makes a lab autograde on four platforms, CAP-33
provides the labs") is precise and consistently repeated across the four
already-filed sub-issues (#575–#578). But the capstone carries an
unresolved self-flagged contradiction, an external dependency with no
fallback plan, and capstone-level acceptance criteria that are looser than
what the issue's own prose promises — three separate ways the stated
verification could pass while the real goal ("an instructor adopts a
course") does not.

## Findings, most severe first

### 1. (High) The demo slice's completion is gated on an external party's unwritten agreement, with no fallback recorded in this issue
`demo_slice: "PF-3 CSE 260M corpus as fixtures, 1-2 mw — real course, real demand evidence (#509)"`. But #509's only recorded commitment from the reference customer is conditional: *"Dr. Siever has been contacted and is interested in this fork if it becomes well enough matured"* — not an agreement. The sub-issue this capstone's demo slice depends on, #577, makes the dependency explicit: *"AC-3: Content licensing is settled in writing with Dr. Siever before any adapted material ships; absent agreement, the fixtures land as compatibility fixtures only and the kit half is held."* Capstone AC-3 (*"A named external instructor... reviews the kit and their feedback is recorded and addressed or refused by name"*) and AC-4 (clean kit licensing) both terminate on the same unresourced, uncommitted external party. Nothing in #517 names a timeline, a fallback if Dr. Siever never responds, or a second candidate instructor. As written, the capstone can stall indefinitely on a dependency the project doesn't control, and the issue gives no escape hatch (contrast with KC-33-1, which does have one for the #300 dependency).
**Recommendation:** add a kill/fallback criterion for AC-3/AC-4 symmetric to KC-33-1 — e.g., "if no licensing agreement or reviewer commitment materializes within N weeks, ship PF-1/PF-2/PF-4 as originally-authored-content-only and hold PF-3."

### 2. (Medium) AC-1 is gameable relative to the issue's own promised outcome
The Outcome section promises *"a chapter-mapped lab pack aligned to a standard textbook."* But capstone AC-1 only requires: *"≥8 labs spanning combinational → sequential → FSM → small datapath, each loading, simulating, and autograding out of the box."* Nothing in AC-1 requires textbook-chapter mapping, exercise prose, or a stated time budget — those requirements exist only one level down, in #575's AC-1/AC-3/AC-4. A literal reading lets someone satisfy CAP-33's own AC-1 with eight minimal, unmapped autograding labs (e.g., a handful of gate circuits) that pass CI while failing the capstone's stated "textbook-mapped" premise, since the capstone's completion criteria are checked against the capstone's own AC list, not silently against a child issue's stricter one.
**Recommendation:** either restate the chapter-mapping/time-budget requirement directly in CAP-33's AC-1, or add an explicit clause that AC-1 is satisfied only via #575's stricter AC set — don't rely on the reader inferring it.

### 3. (Medium) AC-4's licensing requirement is unspecified enough to be gamed
*"AC-4: Kit content carries clean licensing (course materials under a stated open license distinct from code)."* No license family is named. "A stated open license" is satisfied in letter by anything the author labels "open" — including a non-free-in-practice license (e.g., CC-BY-NC, or a license with a field-of-use restriction) that would still technically be "distinct from [GPLv3] code" and "stated." Given the kit content is meant to be freely redistributable and remixable by third-party instructors (per #578's whole purpose — "a third party can author a course kit from the documented convention"), an ND or NC term would defeat that goal while passing AC-4 as literally written. There is also no treatment of the fact that starter `.jls` circuit files sit at the code/content boundary (they are simultaneously distributable artifacts under whatever the project's normal `.jls`-file convention is and "course content").
**Recommendation:** name the license (e.g., CC-BY-4.0, as is conventional for open educational resources) or explicitly delegate the choice to #578 with a stated minimum-freedom bar (must permit redistribution and modification at minimum).

### 4. (Medium) Self-flagged, still-unresolved contradiction between the title and the planned-feature inventory
The title advertises "guided lessons" as a first-class deliverable, and the issue's own maintainer comment catches this: *"The title and Outcome promise 'guided lessons', but planned_features lists only PF-1..4, none of which is a lessons feature... Worth an owner decision: adopt #552 as a served-capstone cross-link, or add a PF row."* That decision is still open in the issue as it stands — no PF row exists for lessons, and #552 (CAP-27 PF-5) is only loosely cross-referenced ("shares content with," not "delivers"). An implementer who completes AC-1 through AC-4 exactly as written can legitimately claim CAP-33 done while the title's "guided lessons" clause never shipped under this issue at all — or, in the opposite failure mode, someone reads the title literally and scope-creeps into re-building lesson tooling that #552 already owns.
**Recommendation:** resolve explicitly before work starts — either drop "guided lessons" from the title/Outcome (it's #552/CAP-27's job), or add a PF-5 row here that names #552 as the satisfying dependency with an ordering edge, matching the rigor already used for the CAP-06 dependency.

### 5. (Medium) Stale machine-readable metadata that the issue's own comment identifies but the body still carries
The yaml block reads `planned_features: [PF-1 unfiled, PF-2 unfiled, PF-3 unfiled, PF-4 unfiled]` and `ordering_after: [..., "#511 CAP-27 PF-5 (lesson tooling/content shape is shared)"]`. Both are stale: the coverage-verification comment on this very issue records all four PFs as filed (`#575`, `#576`, `#577`, `#578`) and that "#511 CAP-27 PF-5... now resolves concretely to #552." The comment explicitly declines to fix the body ("issue bodies are not rewritten by this workflow, per #489"), so the stale pointers persist indefinitely. Any tooling, reviewer, or contributor that reads the yaml block at face value — rather than reading every comment first — will chase dead "unfiled" labels and an unresolved feature-slot reference instead of the concrete issues that actually exist.
**Recommendation:** a one-line maintainer edit to the yaml (updating the four PF slots to their issue numbers and `#511 CAP-27 PF-5` to `#552`) removes a known trap for anyone acting on this issue without reading the full comment thread first.

### 6. (Medium) Feasibility/cost risk: content authorship is a different kind of labor than the `mw` estimates assume, and no author is named
PF-1's `3-5 mw` covers writing "≥8 labs spanning combinational → sequential → FSM → small datapath" with exercise prose and grading vectors mapped to a specific external textbook's chapter structure — pedagogical content authorship requiring domain/teaching expertise, not the software-engineering work `maintainer-week` estimates elsewhere in this tracker (e.g. `docs/capability-roadmap/*.md`) are calibrated against. No author or reviewer capacity is named anywhere in #517 or #575; KC-33-2's quality gate ("two consecutive failed reviews... pull it from the pack") assumes a pool of "non-author" testers that also doesn't exist yet on this project (a single-maintainer pedagogy tool per `ARCHITECTURE.md`'s i18n section). The estimate and the kill criterion both assume infrastructure (reviewers, testers) this issue never establishes.
**Recommendation:** name who authors and who reviews the labs (even if it's "the maintainer, cross-checked by Dr. Siever's #509 relationship") before committing to the `3-5 mw` band, or flag the estimate as provisional pending a resourcing decision.

### 7. (Low) KC-33-2's review mechanism is operationally undefined
*"a lab that cannot be completed by a non-author in its stated time budget is defective; two consecutive failed reviews on a lab pull it from the pack."* Undefined: who counts as a qualifying "non-author" reviewer, how failures are recorded, and whether this pool is the same one AC-3 uses (the single named external instructor) or a separate one. As written, two different people idly commenting "seemed hard" could satisfy — or two rigorous timed trials could be required — and both readings are consistent with the text, which makes the kill criterion unenforceable as a hard gate.
**Recommendation:** define "review" (a timed trial by a specific role) and clarify whether it draws from the same reviewer pool as AC-3.

### 8. (Low) Copyright/legal exposure of textbook-chapter alignment is never addressed
The pack is explicitly built to mirror "the Donzellini Springer digital-design text" chapter-by-chapter, motivated by DEEDS's closed format ("no importer is possible for a closed format, so the course must port even though the files cannot"). PF-1 disclaims copying DEEDS assets ("Original content, no DEEDS assets"), but neither #517 nor #575 has an AC addressing whether replicating a copyrighted textbook's chapter organization/exercise sequencing (as opposed to its content) carries derivative-work risk, nor whether Springer or the textbook author was ever consulted (contrast with the Dr. Siever content-licensing conversation, which *is* tracked). This is a real-but-manageable risk given the stated originality discipline, but it is currently unverified by any acceptance criterion.
**Recommendation:** add a line to #575 or #517's AC set confirming no textbook material (text, figures, or verbatim exercise structure) is reproduced, reviewed once by someone other than the author.

## What's solid (one line each)

- Citations check out: the `evidence` block's claims about #510 §3 and #509 are accurate paraphrases of those issues' actual text — no misquote found.
- The CAP-21/CAP-33 boundary ("CAP-21 makes a lab autograde on four platforms, CAP-33 provides the labs") is stated once here and then honored consistently and correctly in all four child issues (#575–#578).
- KC-33-1's graceful-degradation plan against the #300 (CAP-06) dependency is genuinely good hygiene — it names the exact fallback contract (today's three-exit-status behavior) rather than leaving PF-2 blocked indefinitely, which is exactly what AC-3/AC-4 (Finding 1) lack.
- AC-2's CI walk-through (distribute → mutate a submission → grade → per-student verdict with a planted failure caught) is concretely testable and hard to game as stated.
