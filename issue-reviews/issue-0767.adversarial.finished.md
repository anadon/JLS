# Issue #767: TASK-C578-1: "course kit" becomes a written layout — labs, vectors, schedule and rubric, each part named required or optional
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#767 (TASK-C578-1, `part_of_feature: 578`, `band_mw: 0.5-1`, `ordering_after: [575, 576]`) wants
the "course kit" concept turned into a written directory-layout + metadata spec (required vs.
optional parts named), derived from the worked Donzellini pack (#575) rather than invented ahead
of it, with the metadata carrying at minimum kit identity/version, JLS version range, contained
labs, and content license, plus a stated relationship to #576's assignment workflow. It is the
first of (at least) three visible task-tier slices of #578 (FEAT-C33-4): #769 builds the
validator against this spec, #772 adds the license + authoring doc + external review. Nothing in
the tree today (`grep -rli "donzellini\|course kit"`) is a course-kit artifact — this is a
greenfield spec.

## Findings, most severe first

**1. [High] The title and Outcome define a kit as four parts — labs, vectors, schedule, rubric —
but AC-2's metadata requirement only names one of them (labs), so a spec that satisfies every
checkbox can still omit "schedule" and "rubric" from the metadata entirely.**
Quoted title: `"course kit" becomes a written layout — labs, vectors, schedule and rubric, each
part named required or optional`. Quoted AC-2: "The metadata carries at minimum: kit identity and
version, the JLS version range it targets, the labs it contains, and its content license." Neither
"schedule" nor "rubric" appears anywhere in AC-2, and AC-1's "each required and optional part
named" is generic enough that a spec author can satisfy it by listing schedule/rubric as bare
files in a directory tree with no corresponding metadata field, purpose statement, or format
commitment. Downstream, #769's validator can only check what #767's spec actually commits to
machine-checkable form — if schedule/rubric are never given a metadata contract, the validator
(and CI) can never verify a kit's schedule or rubric is present, correctly named, or well-formed;
it can only verify labs and license. The stated Outcome ("kit = labs + grading vectors + schedule
and rubric... every part is named and marked required or optional") is then only half-delivered by
what AC-2 actually forces into existence. Recommendation: add schedule and rubric to AC-2's
metadata floor (at minimum: presence, location, and a format/purpose statement), or explicitly
scope them out of the metadata (free-form files only) and say so, rather than leaving the gap
implicit.

**2. [High] The core acceptance criterion — "derived from the worked pack, not invented ahead of
it" — has no test, no artifact, and no way to be falsified after the fact.**
Quoted AC-3: "The specification is derived from the worked Donzellini pack (#575) rather than
invented ahead of it, and any place the pack does not fit is resolved in the spec, not waived."
There is no proposed check (git history order, a changelog note, a review sign-off) that
distinguishes "I built #575 first and then wrote the spec to match it" from "I wrote the spec from
imagination and then quietly made #575 conform, or vice versa." Both produce an identical final
state: a spec document and a pack that agree. Compounding this, #575 is itself unstarted (its own
AC-1, "at least 8 labs ship," is unmet — `grep` confirms zero kit content in tree), so this AC's
precondition does not yet exist. `ordering_after: [575, 576]` in the YAML block is prose, not
anything CI or tooling in this repo enforces (there is no dependency-graph checker in
`test/` or `scripts/` for issue ordering) — nothing stops #767 from being implemented today, before
#575 has a single lab, at which point AC-3 becomes either unsatisfiable or satisfied by fiat.
Recommendation: make AC-3 machine-checkable (e.g., the spec document must cite specific #575
artifacts by path with a stated "resolved how" note for every place they diverge) rather than
relying on unverifiable authorship-order narrative, and state plainly that #767 cannot be closed
before #575 ships its first lab.

**3. [Medium] The outer usability bar in the Outcome is never tested by this issue's own
acceptance criteria — it is silently deferred to a different, later task.**
Quoted Outcome: "Written well enough that an instructor outside this project can author one
without asking a maintainer what goes where." None of AC-1 through AC-4 requires any outside
person to attempt authoring a kit from the spec; that test only appears in #772 (TASK-C578-3,
`ordering_after: ["TASK-C578-2", 509]`), which brings in "a named external instructor" — an issue
that sits two tasks and an unenforced ordering chain away. #767 can be checked off in full while
shipping a spec that is internally consistent but incomprehensible to an outside reader, and
nothing in #767 itself would catch that; the gap surfaces only if and when #772 executes.
Recommendation: either fold a lightweight outside-reader smoke test into #767's own AC set (e.g.,
a non-author drafts a toy kit from the spec alone, as a cheap gate before the heavier #772 review),
or state explicitly in #767 that the usability claim is unverified until #772 lands, so it isn't
read as delivered here.

**4. [Medium] The generalized "any third-party instructor can author a kit" goal is not
grounded in the project's one evidenced demand signal, and the issue doesn't note the mismatch.**
Per #517 (CAP-33) and #509, the only named, concrete demand for course-kit work is Dr. Siever's
WashU CSE 260M migration — which is fundamentally a *corpus-adaptation* problem (#577: port an
existing course's circuits and provenance under this fork) rather than a *write-from-nothing*
authoring problem. #767's spec is scoped for a hypothetical future third-party author with no
existing course, a materially larger design target than the one real prospect on record. This
isn't necessarily wrong — #578's Outcome explicitly wants the general convention — but #767
presents the third-party-authoring bar as self-evidently right-sized without weighing it against
the evidence base the whole CAP-33 capstone cites, and a reviewer following the citation chain
(#767→#578→#517→#509) finds the generalization is asserted, not derived. Recommendation: note in
#767 (or #578) which requirements are validated against the WashU case and which are purely
speculative for an unnamed future author, so scope-trimming under time pressure has somewhere to
cut that isn't guesswork.

**5. [Medium] Feasibility: the visible task-tier decomposition of #578 does not sum to #578's own
acceptance criteria, and #767 doesn't flag the shortfall.**
#578 has five ACs; the three visible task-tier children (#767 AC-1/AC-2, #769 AC-2/validator,
#772 AC-4/AC-5 license+review) cover AC-1, AC-2, AC-4, and AC-5. #578's AC-3 — "One complete
worked course ships as a conforming kit and is walked end to end through the FEAT-C33-2
workflow" — appears in none of the three task bodies read for this review. Either a fourth task
exists uncited by any of #767/#769/#772's `ordering_after` chains, or #578's AC-3 is currently
unassigned to any task-tier work. Separately, the three visible tasks each carry `band_mw: 0.5-1`
(1.5-3 mw combined), well under #578's own stated `band_mw: 3-4`, which is consistent with a
missing AC-3 task rather than an accounting error. #767, as the first task in the chain and the
one the others `ordering_after` against, is the natural place to note this gap; it doesn't.
Recommendation: confirm whether a TASK-C578-4 (or equivalent) exists for AC-3, and if not, file
it or fold the worked-course walkthrough into one of the three existing tasks explicitly.

**6. [Low] No artifact location is named for the spec itself, inviting a "technically in tree"
minimal-effort satisfaction of AC-1.**
AC-1 says the layout and metadata schema are "specified in tree" but never names where — unlike
sibling normative docs in this repo (`docs/batch-interface.md`, `docs/file-format.md`,
ARCHITECTURE.md's own convention of file/method anchors), there's no `docs/course-kit-format.md`
or similar named target, and no requirement that the spec follow the repo's existing pattern of
normative documents with anchored citations. A one-paragraph README section would technically
close AC-1 as worded. Recommendation: name the target path and require it follow the same
normative-doc convention as `docs/batch-interface.md` / `docs/file-format.md`, so it lands as a
citable spec rather than prose.

## What's solid

- `ordering_after: [575, 576]` correctly lists *both* real prerequisites — unlike its own sibling
  tasks #769 and #772, which (per adjacent reviews of those issues) only order after the previous
  task in the chain and silently drop the transitive dependency on #575 actually existing. #767
  gets this right.
- The Boundary section cleanly excludes platform delivery (#502) and the grading engine (#300) and
  correctly notes the build-along lesson shape is shared with #552 rather than redefined — no
  scope bleed into adjacent capstones there.
- AC-4 (stating which #576 workflow parts a kit's metadata is consumed by vs. ignored by) is a
  concrete, checkable requirement and correctly identifies the one integration point that would
  otherwise be assumed rather than stated.

## Verdict

needs-rework — the schema/Outcome mismatch on schedule and rubric (finding 1) and the
unfalsifiable "derived, not invented" acceptance criterion (finding 2) are both fixable with
concrete wording changes, but as written the issue can be marked complete while delivering a kit
convention that structurally cannot enforce two of its own four named kit parts.
