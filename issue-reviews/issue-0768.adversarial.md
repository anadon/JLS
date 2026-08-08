# Issue #768: TASK-C548-3: every example carries a caption and a suggested exercise, and a shipped example without either fails the build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#768 (TASK-C548-3, part of feature #548, `ordering_after: [TASK-C548-2]`
i.e. #766) is the metadata task in the CAP-27 (#511) on-ramp chain: every
curated example must carry a caption and a suggested exercise, both
machine-asserted (a test fails when either is missing), the exercise
bounded by the caption's length, and the caption also embedded in the
opened circuit itself as a header `Text` element.

## Findings, most severe first

**1. [High] AC-1 requires the caption be "readable" via the Examples
menu, but the menu task that precedes this one in the chain defines no
caption-display surface at all — AC-1 is satisfiable by data the user
never sees.**
Quoted AC-1: "Every example carries a caption and a suggested exercise,
both present in a form the Examples menu and the gallery can read." The
menu itself is built by #764 (TASK-C548-1), whose five acceptance
criteria are entirely about listing and opening circuits ("A single
top-level Examples entry appears in the menu bar and lists the shipped
circuits, discoverable with no prior knowledge"; "Selecting an entry
opens the circuit through the standard reader") — nowhere does #764
require the menu to display, tooltip, or otherwise surface a caption to
the student before they open the file. #768 does not order itself
relative to #764, amend it, or add a criterion requiring the menu
surface itself be extended to show the caption. As written, "the menu
can read" the metadata is satisfied by the data being technically
parseable by menu code (e.g. loaded into a `Map` at startup and never
rendered), while the stated pedagogical goal — a student "readable
without opening anything longer" than the caption, per the issue's own
Outcome section — is never delivered through the menu at all, only
through AC-4's in-circuit `Text` element (which requires opening the
file, the exact thing the Outcome section says should be avoidable).
Recommendation: either add an AC requiring the menu (or a companion task
against #764) to actually display the caption text before/at selection
time, or narrow AC-1's language to "machine-readable" and drop the
implication that a student reads it from the menu.

**2. [High] The caption/exercise metadata format is never specified,
and a downstream consumer already assumes a stable schema exists.**
#768's whole job, per its own Outcome, is defining "the metadata" that
"is asserted" — but the issue never says what that metadata *is*: a
sidecar file per circuit, a Java constant table, fields parsed out of
the header `Text` element from AC-4, a new `SaveTags`-registered
element? `src/jls/elem/SaveTags.java` and `src/jls/elem/Text.java` show
the existing save-format machinery this would have to hook into, but
#768 doesn't name a mechanism. This is not a hypothetical gap: #773
(TASK-C551-1, gallery renderer, `ordering_after: [TASK-C548-3]`) already
commits to "pulling captions from #548's metadata" as a stable read
surface, and my sibling review of #773 independently flagged that its
own AC-3 ("no second copy of any caption text") is unverifiable because
"the metadata format itself is not yet defined." #768 is the one issue
positioned to close that gap and doesn't. Recommendation: #768 should
name the concrete artifact (e.g., "captions and exercises are attributes
on the sample's `Text` element, parsed by a shared accessor both the
menu and the gallery script call" or an explicit sidecar format) rather
than leaving "a form ... [that] can read" undefined for two consumers to
guess independently.

**3. [Medium] AC-3's length bound is circular and has no absolute unit
— it can be satisfied while defeating its own stated purpose.**
Quoted: "Each exercise is no longer than its caption (CAP-27's 'without
reading anything longer than a caption'), asserted mechanically as a
length bound." The Outcome section frames this as "both no longer than
a caption" — but the caption is itself one of "both," so "no longer than
a caption" is not a meaningful bound on the caption; only the
exercise-vs-caption relative comparison in AC-3 is actually checkable.
Nowhere in #768, #548, or CAP-27 (#511, whose body confirms the "without
reading anything longer than a caption" line is the capstone's framing
prose, not a spec) is there a numeric bound — no character count, word
count, or line count. A caption of 500 characters and an exercise of 499
characters would pass AC-3's "mechanical length bound" while violating
the entire premise (an on-ramp readable in under ten minutes without
reading anything long). Recommendation: state an absolute cap (e.g., "a
caption is ≤120 characters, one line") in addition to the relative
exercise-vs-caption check, and specify the unit (characters, not words
or lines, to make the JUnit assertion unambiguous).

**4. [Medium] AC-4 duplicates #381 P8 and #548 AC-3 without citing
either, leaving unclear which issue actually builds the header `Text`
element versus merely asserting it.**
#768 AC-4: "The caption is also visible in the opened circuit itself (a
header `Text` element or equivalent)." #381 (TASK-0030) already commits
to this as P8: "each carries a header `Text` element naming what it
demonstrates," and #548 (the parent feature) AC-3 says every example
"carries a caption element." #768 cites neither #381 nor #548 by number
in its body — only `part_of_feature: 548` in front matter — and doesn't
state whether it is the *implementation* of #548 AC-3/AC-4 or a
duplicate obligation. This is the same failure mode my sibling review of
#771 flagged for #550/#770 (identical AC text in a feature issue and its
task issue, with no statement of which is authoritative). Concretely:
#766 (TASK-C548-2, #768's own immediate prerequisite) builds the ten
curated circuits and its four ACs never mention a header `Text` element
— if #766 ships circuits without one (a legitimate reading of its own
ACs), #768 inherits an undeclared obligation to retroactively edit
circuit files #766 already shipped, which #768's Outcome section doesn't
mention as in scope. Recommendation: #768 should state explicitly that
AC-4 is the authoritative implementation of #381 P8 / #548 AC-3's
caption-element requirement (not a re-specification), and #766 or #768
should say which task actually authors the `Text` element into each
`.jls` file.

**5. [Medium] The full dependency chain is three unstarted issues deep,
understated by citing only the immediate predecessor.**
`ordering_after: [TASK-C548-2]` names only #766. But #766 depends on
#764 (TASK-C548-1, the Examples menu), which depends on #381 (TASK-0030,
the source of the 3-5 sample baseline). None of `resources/samples/`
exists in this checkout (`Glob resources/samples/**` returns nothing,
matching #381's own O3: "`resources/samples/` does not exist today"),
and #764/#766/#381 are all still open. So #768's every acceptance
criterion is currently unverifiable against real content — normal for
staged backlog work, but the issue gives no hint that a picker-upper is
actually three issues away from having anything to write a test against,
which is a planning hazard identical to the one flagged in my sibling
review of #773 for the same chain one hop further out.

**6. [Low] AC-2's "with the transcript recorded" names no location or
format.** "A scratch example added without one turns it red with the
transcript recorded" — recorded where? A PR comment, a committed file, a
CI log artifact? Nothing pins it to something durable the way, e.g.,
`test/jls/AllElementsRoundTripTest`'s fixtures are pinned to files. This
is the same gap my sibling review of #771 flagged for its AC-2 "and its
derivation is recorded." Low severity but easy to close: require the
red-state transcript to land as a comment or fixture in the test file
itself.

**7. [Low] "the gallery" (#551/#773) is referenced only generically in
AC-1, never by issue number, despite #768 being upstream of it.**
#773 (TASK-C551-1) explicitly builds against "#548's metadata" and
`ordering_after: [TASK-C548-3]` — i.e. it depends on #768's output shape
— but #768 never cites #773 or #551, so the metadata schema (finding 2)
risks being designed without its one concrete known consumer in mind.
Low severity since `ordering_after` correctly points the dependency the
right direction (#773 waits on #768), but a two-way citation would catch
schema mismatches earlier.

## What's solid

- AC-2's red-before-green ratchet (a scratch example without a caption
  demonstrably fails, transcript included) matches the project's
  established test discipline, e.g. #381 P1's documented pre-fix failure
  output.
- AC-4's "header `Text` element or equivalent" is a lightweight,
  already-supported mechanism (`src/jls/elem/Text.java`,
  `src/jls/elem/SaveTags.java`) rather than inventing new save-format
  machinery — a sound implementation choice on its own terms, independent
  of the ownership ambiguity in finding 4.
- Scope is appropriately narrow: #768 doesn't try to build the menu
  (#764) or the gallery (#551/#773) itself, only the metadata layer
  between them — the decomposition is directionally right even though
  finding 1 shows a real gap at the menu boundary.
- Labels (`enhancement`, `area:test`, `area:ux`, `tier:task`) match the
  issue's actual content.

## Verdict rationale

`needs-rework`: the task's shape (assert captions/exercises exist,
bound exercise length, embed the caption in-circuit) is reasonable, but
findings 1-2 are load-bearing gaps — the issue never specifies the
metadata format it exists to define, and its own acceptance criterion
for menu-readability can pass without a student ever seeing a caption
before opening a file, which contradicts the issue's stated purpose.
Findings 3-4 show the length bound and the `Text`-element requirement
are each either circular/unspecified or an uncredited duplicate of
upstream issues' criteria. None of this is fatal to the task's existence,
but as written it can be closed by an implementation that is internally
consistent and green while missing the actual on-ramp goal CAP-27 exists
to serve.
