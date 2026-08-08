# Issue #754: TASK-C547-2: the VPAT/ACR draft is generated, not authored — WCAG 2.2 AA under the desktop mapping, with Swing's limits listed as exceptions by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what was checked

#754 is a `tier:task` leaf under FEAT-C26-5 (#547), itself serving CAP-26
(#507, the accessibility capstone). It asks for a generator that turns a
criterion→test mapping (built by its sibling task TASK-C547-1, #753) into a
VPAT/ACR draft. I read #754, its parent #547, grandparent capstone #507, and
the five sibling TASK-C544-* issues (#737, #739, #741, #743, #745) plus
TASK-C549-1/2 (#756, #758) that #753 depends on. I also read
`docs/standards-adoption/03-accessibility-conformance.md` (the project's own
VPAT authoring playbook, ~920 lines) and `docs/standards-landscape.md`
§11.6/§12.d, which #754's language is drawn from nearly verbatim.

## Findings, most severe first

**1. [High] No task in this tree fixes the one prerequisite the project's own playbook calls non-negotiable, and #754 doesn't depend on it.**
`docs/standards-adoption/03-accessibility-conformance.md` §1 identifies that
`scripts/build-installer.sh:143-146` derives the jlink module set via
`jdeps --print-module-deps`, which can never include `jdk.accessibility` (no
static reference exists), so "every `.msi` produced by the release pipeline
bundles a runtime with no Java Access Bridge... NVDA and JAWS get nothing at
all from an installed JLS." The same document states explicitly, in its
"Do NOT do this if" section: *"The §1 bridge fix is not landed first...
Fix, then report."* I searched the whole issue tracker for `jdk.accessibility`
and `AccessBridgeModuleTest` (the pinning test the doc names) and found zero
matches, in this task tree or anywhere else. #754's own `ordering_after`
lists only `[TASK-C547-1]` (#753). As scoped, #754 can be executed to
completion — `VpatCoverageTest` green, every claimed row backed by a passing
JAAPI test run on a full JDK in CI — and still ship a VPAT rating 502.3.\*/
11.5.2.\*/4.1.2 "Supports" for a Windows distribution whose actual installer
delivers nothing to NVDA. This is failure mode #1 from the playbook's own
"Top three ways this goes wrong" list, reproduced almost exactly by the gap
between what #754 tests (JAAPI conformance under `mvn verify`) and what it
would claim (installer-level AT support). Recommendation: add
`ordering_after` (or a `blocked_by`) entry for a task fixing the module set,
or narrow AC-1/AC-2 to state explicitly that installer-runtime rows require
the bridge fix as a separate, named precondition before they can be claimed.

**2. [High] The acceptance criteria depend on sibling tasks that the machine-readable ordering graph never names.**
AC-2 requires "any live-announcement reduction recorded by TASK-C544-3" (#741)
to appear as a named exception. AC-3 requires NVDA-manual-checklist claims to
be visually/textually distinct — a categorization TASK-C544-5 (#745) is the
one that actually establishes. Neither #741 nor #745 appears anywhere in
#754's `ordering_after: [TASK-C547-1]`. #507 (CAP-26) explicitly runs on a
REPLAN discipline keyed off this machine block ("Every response ends in a
`REPLAN:` comment"; "A planned feature is filed → REPLAN resolving the PF
entry"), so an incomplete dependency list is not cosmetic here — it's the
field this project's own process uses to decide what must land first. As
written, #754 could be picked up and "completed" against a generator that
has nothing from #741/#745 to consume yet, silently degrading AC-2/AC-3 into
no-ops (there's nothing to name as an exception if #741 hasn't run its spike
yet). Recommendation: add #741 and #745 to `ordering_after`, or make AC-2/AC-3
explicitly conditional with a stated fallback.

**3. [Medium] AC-4's "cannot be raised by a prose edit" is unfalsifiable as stated — no named test, unlike its siblings.**
AC-1, AC-2 (via #753/CAP-26 AC-4 `VpatCoverageTest`) and the KC-26-3 criterion
all cite a specific mechanism. AC-4 says the claim-strength string is
"produced by the generator, so it cannot be raised by a prose edit" but names
no test. A generator that runs once, with its Markdown/HTML output then
committed and hand-edited afterward, satisfies "produced by the generator"
on day one and is silently no longer true on day two — nothing in #754 (a
regenerate-and-diff CI check, a provenance header, a checked-in "generated,
do not edit" guard test) is specified to catch that. This is exactly the
staleness failure mode #3 the standards-adoption doc warns about generically
("the published document silently becomes false") applied to the one field
(claim strength) this issue singles out as most load-bearing.
Recommendation: name the enforcement mechanism (e.g., a test that
regenerates the doc into a temp path and diffs against the committed copy).

**4. [Medium] AC-1 is gameable toward a hollow-but-technically-honest document.**
"The document contains no claimed criterion without a named passing test" is
satisfied by a generator that claims almost nothing — rating every criterion
Not Applicable, Not Evaluated, or manual-exception. That passes the letter of
AC-1 while defeating the Outcome section's own framing: "the procurement
document institutions actually request." Nothing in the criteria sets a
floor on how much of the WCAG 2.2 A+AA set must actually resolve to a real
Supports/Partially-Supports rating backed by evidence, versus how much can
be waved through as "no test, so not claimed." Recommendation: add a
criterion requiring the generated ACR to show a minimum count (or list) of
non-"Not Evaluated" rows, so an empty-but-honest document can't pass.

**5. [Low] Scope is WCAG-only where the project's own playbook recommends a three-table document at no extra cost.**
`docs/standards-adoption/03-accessibility-conformance.md` lines 27-37
recommends "VPAT 2.5 INT (International) Edition... contains all three
tables in one document... and it costs no extra work: the three tables share
the WCAG rows" — and separately warns "procurement offices ask for 'your
VPAT' and reject documents that read as marketing" when the vocabulary or
scope is off. #754 (following CAP-26 Open Question 2's recommended default)
targets only "WCAG 2.2 AA under its desktop mapping." This is an inherited
decision from #507, not a defect #754 introduces, and #507 gives it a REPLAN
escape hatch — but since #754 is the task that actually builds the generator,
it is where the INT-vs-WCAG-only tradeoff gets locked into code, and the
issue doesn't flag the tradeoff or the "costs no extra work" claim from the
sourced playbook.

**6. [Low, informational] Numbering-collision trap inherited into this issue's own references.** `docs/standards-landscape.md` opens with an explicit warning that its own `#209`-style registry numbers collide with real GitHub issue numbers (e.g. registry `#213` ≠ GitHub #213). #754 mixes "CAP-26 Open Question 2" (a subsection of issue #507, not a standalone doc) with direct issue refs (`#507`) and task IDs (`TASK-C547-1`). Anyone implementing #754 without re-reading #507's full body could misresolve "CAP-26 Open Question 2" as an external citation rather than a specific paragraph in #507. Not a defect, just a legibility hazard the issue could have avoided with a direct anchor/quote.

## What's solid

- The core mechanism — a criterion→test mapping that fails the build when a
  claimed criterion lacks a named passing test — is a good match for this
  repo's existing golden-file/ratchet-test house style (`ThemeTest`,
  `HelpTopicsTest`, the pattern `AccessBridgeModuleTest`/`VpatCoverageTest`
  are explicitly modeled on in the playbook).
- KC-26-3 ("if the generator cannot be made mechanical, no hand-authored VPAT
  ships under CAP-26's name, and that outcome is recorded on #507") is a
  genuine, well-specified kill switch against the worst failure mode
  (a fabricated compliance document) — clearly written, traceable to a real
  GitHub issue for the record.
- The task/feature/capstone decomposition (#754 generates from a mapping
  #753 builds) is a sensible split, not scope-duplication, despite the
  near-identical acceptance-criteria language at the #547/#754 tiers.

## Verdict rationale

Sound in structure and consistent with the codebase's actual accessibility
gaps and the project's own playbook, but it ships two real holes: it has no
declared dependency on the one fix (`jdk.accessibility`/Access Bridge) that
same playbook calls a hard precondition for publishing any ACR, and its
`ordering_after` field doesn't capture dependencies its own acceptance
criteria assume. Both are fixable by editing the issue's metadata and
acceptance criteria rather than by rethinking the task — hence
sound-with-concerns, not needs-rework.
