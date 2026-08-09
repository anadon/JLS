# Issue #801: TASK-C587-3: the docu-tests run against the generated targets, and four planted wrong claims are recorded as a committed negative-check record
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two properties, bundled because they both happen to be the last things left in
FEAT-C35-4 (#587):

1. **Target parity** — a claim cannot be true in the in-jar tree and false on
   the hosted site.
2. **Non-vacuity** — the whole #587 ratchet family is proven to actually bite,
   not merely to pass.

Both are right things to want. The project's arc supports them: ARCHITECTURE.md's
recorded "Help delivery" decision already names hosted docs as the planned future
and says the `HelpTopicsTest` link discipline exists precisely so "the same tree
can be published to the web without rewriting." #587 is the natural next turn of
that screw — from *links resolve* to *claims are true*. Nothing here pulls against
the project.

What I want to reframe is the two **mechanisms**, because both are the weaker of
the options actually available to this codebase, and one of them (AC-1) quietly
re-admits the failure mode FEAT-C35-1 exists to abolish.

## Reframing 1: parity is a property of the generator, not a loop in every assertion

AC-1 says *every* docu-test asserts against *both* generated targets. That is
O(assertions × targets) work, and the target count is not fixed at two: #585 is
the hosted site, and the same content tree is a plausible source for a printable
manual, a `--help`-adjacent text export, or a per-release offline zip. Every new
target then re-multiplies every assertion.

Worse, the coupling is uglier than the AC admits. The in-jar target is HTML 3.2
for a `JEditorPane`; the site target is templated pages with nav chrome and
permalinks. The same hotkey claim will render as `<td>Ctrl-Z</td>` in one and
something else in the other, so "run the same assertion against both" means
either a second extractor per target or brittle regexes tuned to both — and
today's extractor is exactly that brittle: `HotkeysHelpAccuracyTest` spends 383
lines and a `<tr>\s*<td>…` regex to pin *one* table.

The better seam, and the one FEAT-C35-1 has already cut for free: **the generator
emits a machine-readable claim index alongside each target.** One sidecar per
target — every claim it rendered, as data: `{kind: hotkey|flag|element-port|…,
source_file, anchor, asserted_value}`. Then:

- Docu-tests consume the index, never the rendered HTML. No regexes over markup;
  no per-target extractor.
- Parity becomes **one** assertion, not a loop in N tests: the two indexes are
  equal. That scales to any number of targets and makes "true in one, false in
  the other" structurally impossible rather than empirically sampled.
- AC-3 ("the message names the file and the contradicted source of truth") falls
  out of the data model instead of being a formatting requirement repeated in
  every assertion — the claim already carries its file and anchor.
- TASK-C584-3's AC-4 (topic-set parity between targets) becomes the degenerate
  case of the same check, rather than a separate hand-written test.

I am explicitly disregarding AC-1 as written. "Every docu-test asserts against
both targets" states the *evidence* for parity; "the targets' claim indexes are
equal, and the docu-tests run once against the index" states the *property*, and
is cheaper, more general, and stronger.

## Reframing 2: generate the volatile claims and there is nothing left to assert

The three claim families #587 chases — hotkey tables, the flag table, element
port/parameter tables — are all **derived data**. They exist in the help tree
only because a human transcribed them from `EditOp`, `JLSStart.FLAGS`, and the
element model. A ratchet tells you the transcription went stale *after* it went
stale; single-sourcing means it cannot.

FEAT-C35-1 is already building a generator over a plain-text source form. The
marginal cost of transclusion directives — `{{hotkey-table}}`, `{{flag-table}}`,
`{{element-ports Register}}` — expanded at build time from the live tables is
small, and the project has already ratified exactly this move once: `src-filtered/`
exists so `version.properties` is Maven-filtered from `pom.xml` rather than
hand-kept and tested for agreement.

Do that, and three of the four planted defects in AC-2 become *unrepresentable*:
you cannot plant a wrong hotkey or a stale port list in a table nobody types.
What remains needing a ratchet is the residue — hand-written prose ("stated
behaviour"), page existence, and the flag *narrative* in `docs/batch-interface.md`
and the README, which are outside the help generator and genuinely must be
asserted. That residue is small and honest. Concretely I would ask #801 (and its
parent #587) to state, per claim family, whether it is **generated** or
**ratcheted**, and treat "ratcheted" as the fallback rather than the default.

One oracle caveat the family should settle before either route: for element ports
there is currently *no* descriptor to read. `ElementType` deliberately carries
only tag/class/factory ("GUI concerns — palette icon, category, help topic,
creation dialog — belong to a separate GUI-side palette entry and never appear
here"), and `PaletteEntry` carries a help topic but no ports. The real source of
truth is an instantiated element's `Put` list plus its `Attribute` registrations.
#587's own boundary comment already recommends one shared accessor for `FLAGS`;
the same rule should apply here, or the docu-tests will hand-roll a second,
divergent notion of "what ports an element has."

## Reframing 3: a committed transcript is a photograph of a test

AC-2 asks for a committed record of four red CI runs; AC-4 then asks that the
record be regenerable. AC-4 is a confession: an artifact that must be regenerable
to stay true is an artifact that should have been **executable**. A pasted
transcript is unverifiable (nothing checks it corresponds to today's code),
rots at the first refactor, and is exactly the "recorded prose" idiom this
repository has repeatedly outgrown.

The house idiom already exists, twice ratified:

- `test/jls/ui/package-info.java`: *"every helper assertion in this package is
  itself pinned by at least one deliberately-failing test (assert-the-assertion,
  via `assertThrows(AssertionError.class, …)`) so the harness cannot silently
  pass on an empty circuit."*
- `scripts/wayland-rig-selftest.sh` (and the macos/windows/x11 twins): drive the
  **unmodified** real rig against stubs and assert each scenario is classified
  correctly — on every CI event, with no network and no real hardware.

The equivalent here is a `DocuTestMeaningfulnessTest`: for each defect class,
copy the generated claim index (or target dir) to a temp path, mutate one claim,
run the corresponding docu-test programmatically, assert it fails **and** assert
the message names the file and the contradicted source of truth. That is the same
evidence AC-2 wants, continuously true, and it pins AC-3 for real — a transcript
checks the message text once, at authoring time.

And once claims are data (Reframing 1), the four planted defects stop being four
anecdotes. A generic claim mutator enumerates *every* claim and asserts each is
killed: a **doc mutation score**, exactly the strength-vs-coverage lesson this
project already paid for and wrote down in
`docs/mutation-testing-trial-2026-07.md` ("the mutation score (39 %) sits far
below the line-coverage figure (58 %) — reach without strength"). Four hand-picked
mutants is the doc-corpus version of trusting line coverage.

## Where I land

Endorse the outcome; reframe both mechanisms.

- Keep: the closing property that docs cannot lie in one target and tell the
  truth in another, and that the ratchet family is provably non-vacuous.
- Replace AC-1 with: the generator emits a claim index per target; one assertion
  proves the indexes equal; docu-tests run once against the index.
- Replace AC-2/AC-4 with: a committed, CI-run meaningfulness test that plants
  defects programmatically and asserts red-with-the-right-message; drop the
  transcript file entirely (its content is the test's assertions).
- Keep AC-3 verbatim — but note it becomes nearly free under the claim index
  rather than a per-assertion discipline.
- Add, at feature level (#587): a per-claim-family decision of **generated vs
  ratcheted**, with generation preferred wherever the claim is derived data.

If only one of these is taken, take the third: a transcript is the one deliverable
here that will be stale before the feature closes, and the repository already
knows how to do better.
