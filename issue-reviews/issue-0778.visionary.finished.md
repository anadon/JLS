# Issue #778: TASK-C588-3: each note's JLS claims ship a runnable appendix a stranger reproduces, and a claim found fixed upstream is retracted rather than defended
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the marketing framing off #588 and #778 is asking for something the project
already believes in deeply: **a claim JLS makes in prose must be mechanically
falsifiable, and it must decay loudly rather than quietly.** That is the same
instinct behind `docs/batch-interface.md` ("if code and document disagree, that is
a bug in one of them"), `FileFormatSpecTest` (drift test between spec §7's markdown
tag table and `SaveTags`), `ExtensionPointCatalogTest` (cross-checks the catalog
table against the constants in both directions), `HelpTopicsTest` (link checker +
palette-coverage completeness), `CliFlagTableTest`, and `docs/reproducibility.md`
(a *declared* artifact set with a verification recipe, and an explicit table of
what does **not** reproduce). The house style is already: no unverified sentence
survives in a normative document.

So the goal is aligned. What is misaligned is the *shape* of the apparatus the
acceptance criteria describe. AC-2 asks for "an appendix directory per note"; AC-4
asks for "a documented recheck step"; AC-5 asks for "a review checklist". Those are
three bespoke, human-executed artifacts in a repository whose entire character is
that such things get turned into a test. #778 is the one place in feature 588 where
that conversion should happen, and as written it stops one inch short.

## Reframing 1: the appendix is not a directory, it is the repo's existing executable-docs seam

`examples/autograde/autograde.py` + `test/jls/AutogradeBridgeExampleTest.java` is
already exactly AC-2, built and shipped for issue #216: a committed fixture
(`test/fixtures/fork-4.6-shiftregister.jls`), a single documented command that
`docs/vcd-interop.md` §1 quotes verbatim, quoted expected stdout, and a JUnit test
that spawns the real CLI and asserts the doc's claim — with `Assumptions` gating so
the suite stays green where the toolchain is absent. The comparison notes need
*nothing new*. They need to be the second and third customers of that seam.

Concretely, instead of two appendix directories:

- Add one general drift test — call it `DocCommandGoldenTest` — that walks
  `docs/**/*.md`, finds fenced blocks carrying a marker (e.g. ```` ```sh jls-run ````
  followed by a ```` ```text expected ```` block), runs the command against the
  test-JVM classpath the way `AutogradeBridgeExampleTest` already does, and diffs
  byte-for-byte. Fixtures live where fixtures already live (`test/fixtures/`), not
  in a doc-adjacent appendix tree that will drift out of the fixture conventions.
- The first commit of that test retro-arms `docs/vcd-interop.md` §1 (whose quoted
  stdout is currently prose a reader must trust) and any runnable block in
  `docs/batch-interface.md`. That is real value delivered *before* #774/#776 exist.
- The comparison notes then satisfy AC-2 by construction, with zero note-specific
  machinery, and — this is the point — so does every future document that quotes
  JLS output, including the #589 white paper and PF-4 venue paper that #588 says
  are coming.

This matters for sequencing too. #778 orders after #774/#776, which order after
#300 (the grading verdict must be shipped behavior first) and #512/#560. Bespoke
appendix apparatus built to that schedule is apparatus that cannot be built until
a capstone lands and is useful to exactly two files. The harness framing inverts
that: it is buildable today against docs that already exist, and the notes plug in
whenever they land.

## Reframing 2: the freshness gate is the real content, and "documented step" is not a gate

AC-4 is the criterion that will decide whether these notes age into an asset or an
embarrassment, and it is the one specified most weakly. The notes will assert the
state of roughly eleven external tracker issues (Logisim-Evolution #1546, #598,
#950, #1123, #441, #185, #2454; CircuitVerse #1412, #5328, #1753, #2198). Every one
of those is third-party mutable state. A "documented recheck step" is a promise
that a single maintainer will perform a manual ritual forever; the issue's own
outcome text calls this making freshness "a gate rather than a promise", and then
specifies a promise.

The mechanical version is small and fits the repo's existing shape:

- `docs/comparisons/citations.yaml` — one record per claim: project, issue number,
  URL, the exact quoted sentence, the upstream state observed, competitor release
  tested, date checked, and which note/paragraph depends on it.
- An in-suite test (`ComparisonCitationLedgerTest`) that is fully offline: every
  tracker link appearing in `docs/comparisons/*.md` must have a ledger row and vice
  versa, every row must carry a date and version, and any row older than N months
  fails. That is a `HelpTopicsTest`-class link/coverage checker — no network, safe
  in `mvn verify`.
- A **separate scheduled workflow lane** that does touch the network, queries the
  two public trackers, and opens an issue when any cited issue's state flips. CI
  already has the pattern and the discipline for this: `ci.yml` runs a nightly cron
  that executes only the `gui-wayland` lane, in its own concurrency group, with
  green-history annotations in the file. A weekly `comparison-freshness` lane
  belongs there, never in `mvn verify` — the test suite must stay hermetic.

One correction to the criterion itself: *closed upstream is not fixed*. Trackers
close issues as stale, duplicate, wontfix, and by bot. The gate should be "state
changed → a human re-verifies against a real release → retract or re-date", and
the retraction procedure AC-4 asks to be written down should say that explicitly.
Auto-retraction on a closure event would make the notes wrong in the opposite
direction, which is a worse failure than staleness.

## Reframing 3: AC-5's checklist should be a heading, not a document

"A review checklist enforces AC-5" is process theater in a single-maintainer repo
where the author, the reviewer, and the checklist owner are the same person. The
guarantee AC-5 wants — no note ships without naming a competitor advantage — is one
assertion: require a `## Where they are better` section in every file under
`docs/comparisons/`, non-empty, and assert it in the same offline ledger test. Same
guarantee, no ritual, and it survives the maintainer's attention wandering, which
is the only failure mode that matters here.

## The tension worth stating out loud

ARCHITECTURE.md's recorded decisions have a consistent voice: i18n is declined as
"a large, ongoing tax with no requesting user"; the plugin loader was removed
rather than maintained; a second simulation strategy is refused until a concrete
trigger fires. Every one of those decisions names its revisit trigger. Feature 588
introduces the project's first genre of document whose truth depends on parties
JLS does not control — a perpetual tax by construction — and neither #588 nor #778
names a stop condition. #778 is the right place to add one, because it owns the
freshness discipline:

- **Stale rule.** If a note is not rechecked within the ledger's window, CI marks
  it stale in-place (a banner the doc-lint inserts or demands) and the docs-site
  entry is downgraded — decay is the *expected* steady state and should have a
  defined, non-embarrassing resting position, not an assumed recheck.
- **Sunset trigger.** State what retires the notes entirely: e.g. the #589 white
  paper superseding them, or the cited upstream issues mostly resolving. A
  comparison note whose competitor claims have all been fixed is a note that should
  be deleted with a CHANGELOG line, and saying so now costs one sentence.

There is also a durable-content argument that belongs to #774/#776 but that #778's
apparatus should be designed to permit: the slowest-decaying comparison is
**contract versus contract**, not bug versus bug. JLS's genuine, hard-to-copy
position is that it publishes a normative batch interface with an explicit
stability promise, byte-for-byte deterministic VCD pinned by a spec-derived parser,
a declared reproducible artifact set, and a documented error/exit contract. That
claim needs no upstream tracker to stay true and requires zero recheck. Bug
citations are legitimate supporting evidence, but if they are the spine, the
maintenance burden AC-4 creates scales with the competitors' issue velocity rather
than with JLS's. If #774/#776 come back with bug-list-shaped drafts, #778's ledger
will be the thing that hurts — and that is the signal to reweight the prose, not to
buy a bigger ledger.

## Verdict

Endorse the goal without reservation; reframe the build. Do not create two appendix
directories, a recheck procedure document, and a review checklist. Create one
executable-docs golden harness (generalizing `AutogradeBridgeExampleTest`), one
citation ledger with an offline coverage/age test plus a scheduled network lane,
and one required section heading. The stated acceptance criteria are all satisfied
by that route; the difference is that the result is infrastructure the whole docs
tree inherits rather than scaffolding around two files, and it is buildable now
instead of after #300, #512, #560, #774, and #776. Add the stale-and-sunset rule as
a fifth criterion — this feature's real risk is not that the notes are unverifiable
on day one, it is that they are unmaintained on day four hundred.

## Files consulted

- `/home/user/JLS/README.md`, `/home/user/JLS/ARCHITECTURE.md`
- `/home/user/JLS/docs/vcd-interop.md`, `/home/user/JLS/docs/batch-interface.md`,
  `/home/user/JLS/docs/reproducibility.md`
- `/home/user/JLS/examples/autograde/autograde.py`,
  `/home/user/JLS/test/jls/AutogradeBridgeExampleTest.java`
- `/home/user/JLS/test/jls/FileFormatSpecTest.java`,
  `/home/user/JLS/test/jls/ExtensionPointCatalogTest.java`
- `/home/user/JLS/.github/workflows/ci.yml` (nightly cron lane pattern)
- Issues #588 (parent feature), #774, #776 (sibling prose tasks)
