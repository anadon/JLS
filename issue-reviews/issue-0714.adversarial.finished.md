# Issue #714: TASK-C537-2: every element CircuiTikZ cannot draw natively has a named row in an in-tree approximation table, and a sample document proves the export in CI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is the second of a two-task split of FEAT-C24-2 (#537): TASK-C537-1
(#712) builds the CircuiTikZ exporter itself ("no TikZ code exists in the
tree today"), and this issue (TASK-C537-2) adds the approximation table and
the CI-verified sample document on top of it. The ordering is sound, but the
issue's core enforcement mechanism — "registry-keyed" completeness — is
built on infrastructure that `ARCHITECTURE.md` explicitly says does not
exist yet, and two of its four acceptance criteria are unverifiable without
artifacts #712 hasn't produced.

## Findings, most severe first

1. **AC-2's central mechanism ("registry-keyed") names infrastructure the
   codebase does not have.** `ARCHITECTURE.md` §"Adding an element today"
   states plainly: "There is no element registry yet — issue #78 will
   introduce one and collapse most of this." AC-2 reads: "The table is
   registry-keyed: a newly registered element type with no row fails the
   build rather than exporting as an unexplained box." There is no
   `jls.elem` registry to key against today, and #78 is not listed as a
   prerequisite in this issue's `ordering_after` (only `TASK-C537-1`/#712
   is). Either the issue means something looser than a real registry (a
   reflective scan over `jls.elem`, analogous to `SaveTagsTest.java` and
   `ElementConstructorContractTest.java`, which already enumerate element
   classes without a formal registry) or it silently depends on #78 landing
   first. As written, a reader cannot tell which, and "registry-keyed" is a
   term of art the issue borrows without owning the gap. Recommendation:
   either name the concrete mechanism (e.g., "a reflective sweep of
   `jls.elem` `LogicElement`/`Element` subclasses, following the
   `SaveTagsTest` pattern") or add #78 as an explicit `ordering_after`
   prerequisite and accept the schedule hit.

2. **AC-1/AC-3 depend on artifacts TASK-C537-1 (#712) has not produced, and
   that issue's own acceptance criteria don't obviously supply them.** AC-3
   requires "an in-tree LaTeX sample document consuming the generated TikZ"
   to build in CI, but #712's ACs describe only exporting *one* fixture (the
   still-nonexistent "hazard-demo circuit" — confirmed absent from the tree
   by grep, same gap flagged in the #537 adversarial review) and say
   nothing about the approximated-element coverage this issue's AC-1 table
   is supposed to document. If the hazard-demo fixture is flat and simple
   (as #537's review notes it's undefined), it may exercise zero of the
   "memories, state machines, truth-table displays" this issue names as
   motivating examples — meaning the CI sample document (AC-3) could build
   green while never actually routing through the approximation path AC-1
   and AC-2 exist to police. Recommendation: name a second fixture (or
   extend the hazard-demo one) that deliberately contains at least one
   approximated element type, and require the CI sample document to
   include it, not just "a" TikZ output.

3. **AC-4's mutation-test claim is procedurally underspecified and
   gameable.** "A deliberately removed table row turns the build red, and
   that red run's transcript is recorded before any pass is counted." This
   describes a manual, one-time ratchet demonstration (remove a row, show
   red, record it, restore it) rather than an automated, repeatable CI
   check — unlike, e.g., `HeadlessCoreRatchetTest` or
   `NotificationRatchetTest`, which are themselves tests that run on every
   push. As stated, AC-4 could be satisfied once at merge time with a
   pasted CI log, and nothing stops a later PR from silently breaking the
   registry-keyed check (finding 1) without anyone re-running the removal
   drill. Recommendation: state explicitly whether AC-4 requires a
   permanent test (e.g., a `MissingApproximationRowTest` that asserts the
   build-time check rejects an unregistered/row-less type) or is a one-time
   manual gate satisfied by a linked CI run — and if the latter, say so, so
   reviewers don't expect a standing test.

4. **"Every registered element type not rendered natively by CircuiTikZ" is
   not a closed set anywhere in this issue or #712.** `ARCHITECTURE.md`
   lists ~30 concrete `jls.elem` classes. CircuiTikZ has native symbols for
   basic gates, but ambiguous middle cases exist in this tree —
   `RegisterFile`, `ShiftRegister`, `Decoder`, `Mux`, `FieldExtend`,
   `Group`, bus splitters/mergers — where "natively renderable" is a
   judgment call the issue never makes. Because AC-1 only names three
   illustrative examples ("memories, state machines, truth-table
   displays"), a table covering only those three while omitting a dozen
   other non-trivial elements would satisfy a literal reading. This
   compounds finding 1: without a concrete enumeration mechanism, there is
   no way to check the delivered table against "every" element type except
   by manual inspection. Recommendation: have #712 or this issue publish
   the native/approximated split as a reviewable list before either PR
   lands, not implicitly through the table's final row count.

5. **CI toolchain cost is inherited, unpriced, and this issue is the one
   that actually spends it.** #537's adversarial review already flagged
   that no LaTeX/TeX Live reference exists anywhere in
   `.github/workflows/*.yml` today (confirmed: `grep -ri
   "tikz\|circuitikz"` across the repo matches only other reviewers'
   review files, nothing in source or CI config). AC-3 is the concrete task
   that must stand up a three-platform (or at least one-platform) LaTeX
   build job — but band_mw is `1-1.5`, the same order of magnitude as
   #712's exporter-writing task, with no line item for "install and cache a
   working TeX Live + circuitikz in CI." Recommendation: name the LaTeX
   action/distribution and add explicit CI wall-clock/caching budget to the
   estimate, or scope AC-3 down to a single CI platform.

6. **The removed-row failure mode (AC-2/AC-4) isn't distinguished from
   ordinary compile/test breakage in the acceptance text.** "Fails the
   build" doesn't say the failure message must name the missing element
   type; a generic assertion failure or stack trace would technically
   satisfy "fails the build" while leaving a contributor to guess why. This
   is the same category of gap the repo's own `LoadError` taxonomy
   discipline was built to avoid (`ARCHITECTURE.md` "Error-reporting
   contracts": structured categories, not stack traces). Recommendation:
   require the failure to name the specific unregistered element type in
   its message, mirroring the `LoadError`/`SaveTagsTest` pattern already
   used elsewhere in this codebase.

## What's solid

- The dependency ordering (`ordering_after: [TASK-C537-1]`) is directionally
  correct — the table naturally follows the exporter it documents — one
  line, moving on.
- Framing this as "the honesty half" (naming losses rather than silently
  degrading output) is the right instinct and consistent with the parent
  capstone's stated approach; no objection to the goal itself.
- Scoping this task narrowly (table + CI proof, not the exporter) is a
  reasonable split of #537 and avoids one task trying to do everything.

## Bottom line

The goal — an honest, enforced approximation table — is sound, but the
issue leans on a "registry-keyed" enforcement mechanism that contradicts
`ARCHITECTURE.md`'s explicit statement that no element registry exists
pre-#78, depends on fixtures and exporter output from #712 that may not
exercise the approximated-element path at all, and states its central
ratchet criterion (AC-4) in language that doesn't distinguish a one-time
manual demonstration from a standing automated test. None of this blocks
filing, but the registry terminology needs to be resolved (or #78 added as
a prerequisite) and AC-4's mechanism needs to be pinned down before
implementation starts.
