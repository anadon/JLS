# Issue #610: TASK-C556-2: the dropped set equals the reported set, asserted once in shared infrastructure — and the .circ report round-trips through the shared schema losslessly
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#610 asks for two things: (1) a reusable, importer-parameterized "totality"
assertion (dropped set == reported set, both directions) in shared
infrastructure, and (2) re-expressing the *existing* `.circ` migration
report through that shared schema with a lossless, field-by-field
round-trip. Both halves presuppose artifacts that do not exist anywhere in
this repository yet, and the issue's own dependency notation is too weak
to stop someone from picking it up before they do.

## Findings, most severe first

**1. The task's premise is false against the current tree: there is no
`.circ` report to re-express.** AC-3 requires "#323's `.circ` report
round-trips through the shared schema with no information loss,
demonstrated by comparing the re-expressed report against the original
field by field," and AC-4 says "only the report's carrier moves." A
carrier can only move contents that exist. I verified directly:
`find src -ipath "*circ*"` and `find . -iname "MigrationReport.java" -o
-iname "*ConstructMap*"` return nothing; there is no `CircReader`, no
`MigrationReport`, no `docs/logisim-construct-map.md`. The feature that
would produce them, #323 (FEAT-025), and its implementing task, #451
(TASK-0054), are both **open**, and #451's own body documents the current
behavior as `jls -b foo.circ` exiting 1 with "is not a valid circuit file
name" — i.e. `.circ` files are rejected before any parsing happens, let
alone report generation. AC-3 as written cannot be satisfied until #451
lands; right now there is nothing to compare "the re-expressed report"
against.
*Recommendation:* Convert the `ordering_after: [..., 323, 451]` hint into
an explicit hard precondition, mirroring #451's own `blocked_by: [404]`
pattern, and add a "re-verify observations before starting" step (as
#451 §8 does) so an executor who reaches this issue first stops rather
than fabricates a report to satisfy the letter of AC-3.

**2. `ordering_after` is not an enforced dependency — it's prose inside a
YAML fence in the issue body.** GitHub has no relationship linking #610
to #323/#451/#608 (their own `blocked_by`/`blocks` fields, where used by
sibling task #451, are absent here). Nothing stops this issue from being
picked up, worked, and merged before its prerequisites exist, especially
in an automated/fleet execution context where issues are triaged by
number or label rather than by re-reading three other issues' bodies to
reconstruct a dependency graph. Given finding #1, that failure mode is
not hypothetical — it is the default outcome of taking this issue at face
value.
*Recommendation:* Either use GitHub's actual issue-dependency relationship
or add a blocking checklist item at the top of the issue body that a CI
gate / reviewer can mechanically check ("#451 closed" and "#608 closed").

**3. "Parameterised by importer, not copied per format" (AC-1) is asked
for before a second format-consumer exists to parameterize over.** At
filing time, the only concrete importer in the tree is the unrelated
Yosys/Verilog `NetlistImporter` (`src/jls/hdl/imp/NetlistImporter.java`),
which this family of issues explicitly does not touch. The `.circ`
importer (#451) is unbuilt; its declared siblings `.dig` (#558), `.cv`
(#559), and the Falstad-text importer (#561) are also open, unbuilt
feature-tier issues (verified #558 directly: `ordering_after` names
FEAT-C29-1 itself as a prerequisite, so it lands *after* #610, not
before). A "parameterized, not copied" design built and accepted against
zero real call sites is unfalsifiable — there is no way to demonstrate
the abstraction actually generalizes versus merely being shaped to fit
one imagined future consumer. This is classic premature abstraction risk
dressed as an acceptance criterion.
*Recommendation:* Either relax AC-1 to "designed to be parameterized,
verified against a second synthetic (test-only) importer" so the claim is
checkable now, or defer the "parameterised" requirement to whichever
importer actually lands second (#558/#559/#561) and let that PR prove
genericity by using it.

**4. AC-2's "mutation test" is underspecified and gameable.** "A mutation
test proves each direction is armed rather than vacuous" doesn't say
whether this means the repo's existing opt-in PIT profile
(`pom.xml:752-812`, 80% threshold, `docs/mutation-testing-trial-2026-07.md`)
or a bespoke pair of hand-written adversarial unit tests (one fixture
where something is dropped-but-unreported, one where something is
reported-but-not-dropped). Compare this to sibling task #451, which pins
the exact fixture shape it needs ("a fixture seeded with three
deliberately unmappable components"). A minimal, technically-compliant
implementation could add one trivial PIT-covered branch and call it done
while the assertion's two logical directions remain thinly tested.
*Recommendation:* Name the two required fixtures explicitly, the way #451
does, e.g. "one fixture with a construct dropped and omitted from the
report; one fixture with a construct reported as dropped that the
importer actually realized — both must fail the assertion by name."

**5. The issue omits the merge/supersession rule its own family carries.**
Both the parent feature #556 and the sibling task #608 state: "Per CAP-29's
sibling rule: if #311/#323's REPLAN prefers absorbing this generalization,
it merges there, lower number winning." #610 is a child of #556 and
presumably inherits this risk (the whole FEAT-C29-1 generalization —
and therefore this task — could be merged into #323 and closed as
duplicate effort), but #610's own body never states it. A reviewer or
executor scoped only to #610 (as this review is) has no way to know the
ground under this task can move.
*Recommendation:* Restate the inherited merge-risk clause in #610 itself,
not just in its ancestors.

**6. Cost estimate is untethered from its prerequisites' own track
record.** `band_mw: "1"` for designing importer-generic shared
infrastructure, writing bidirectional-mutation-proof tests, and a
lossless field-by-field round-trip of a report format that doesn't exist
yet. #323 (the grandparent feature) documents its own estimate already
missed by 1.5–3x ("the band exceeds the row sum by 1.5x at the low edge
and 3x at the high edge") and flags that as unresolved. A downstream task
inheriting that unstable estimate base, while also carrying an unpriced
"design a general parameterization" requirement (finding #3), should not
be taken as reliably 1-week work.
*Recommendation:* Treat the 1mw figure as provisional until #451 and #608
land and their actual costs are known; do not schedule against it as-is.

## What is solid

- The core totality definition — `Reported = Seen \ Realized`, asserted
  as a two-directional equality rather than either inclusion — is precise
  and directly traceable to #323 §3's formal statement; it is a real,
  checkable property, not vague aspiration.
- AC-4 ("the `.circ` importer's mapping decisions are unchanged by this
  task — only the report's carrier moves") correctly and narrowly scopes
  the boundary against #323/#451, which is exactly the right thing to
  pin down given how much semantic content lives in those issues.
- Labeling (`enhancement`, `area:core`, `area:test`, `tier:task`) matches
  the content; this is genuinely test/infrastructure work, not a user-facing
  feature in itself.

## Verdict rationale

`needs-rework`: not `should-not-proceed`, because the underlying idea
(one shared, bidirectionally-tested totality assertion instead of N
per-format reimplementations) is sound and worth having. But the issue as
written is actionable only as a no-op today — its primary acceptance
criterion (AC-3, the round-trip) cannot be satisfied until #451 lands, its
dependency on that fact is expressed as unenforced prose, and its
generality requirement (AC-1) can't be meaningfully validated against the
zero other importers currently in existence. It should be reworked to
either (a) state a hard, mechanically-checked precondition on #451/#608
and be re-filed as ready once those land, or (b) be explicitly folded
into #451/#608 itself so the report and its totality test are built and
tested together against real data from day one, rather than speculatively
generalized against a report that doesn't exist.
