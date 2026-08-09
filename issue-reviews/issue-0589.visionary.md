# Issue #589: FEAT-C36-2: the grading-contract white paper exists as one instructor-facing document — batch interface stability, determinism guarantees and provenance, in a form a course committee reads and CAP-21's kits link
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not a document. A **trust transfer**: moving JLS's guarantees out of the heads of
people who read `docs/batch-interface.md` and into the hands of someone who will
decide, in a meeting, whether a department's grade book depends on this tool. The
deliverable is instructor collateral only incidentally; the underlying need is
*evidence that survives forwarding* — a promise that stays checkable after it has
been detached from the repository, the maintainer, and the moment.

That need is squarely on the project's arc. JLS's actual differentiator is not
features; it is the house rule stated flatly in
`docs/standards-adoption/04-tool-qualification-and-scope.md`: *"a documentation
claim that is not pinned by a drift test is not evidence."* That rule is already
lived — `docs/batch-interface.md` anchors every claim to a file/method and names
its golden test; `test/jls/ExtensionPointCatalogTest.java` cross-checks
`docs/extension-points.md` against code constants in both directions;
`test/jls/FileFormatSpecTest.java` does the same for the save format;
`docs/reproducibility.md` §1 states in a table which artifacts *do not* reproduce.
AC-3 and AC-5 of this issue are that house rule and that table, applied to prose
aimed at a new audience. So: the goal is right, and the project has the standing
to make the claim.

## Where the framing is wrong

**The paper as specified is the first document in this tree with no anchors of its
own.** Every other normative document points at code; this one points at other
documents' pointers. AC-3 asks a human author to name, per claim, the test that
would fail — and then nothing keeps that annotation true. Delete
`BatchSimulationGoldenTest.watchedElementsPrintInNameOrder` in 2028 and
`batch-interface.md` §5 goes stale (bad); the white paper, a PDF on a committee
member's laptop, goes *silently false* (worse, and it is the artifact the
department relied on). The issue's own AC-4 forbids CAP-21's kits from
paraphrasing the paper — but the paper as specified paraphrases
`batch-interface.md` §6 and `reproducibility.md` §5. The same anti-paraphrase
instinct, applied one level up, invalidates the deliverable's shape.

**The duplication is not one document, it is five.** Counting what is currently
planned or shipped, the batch stability promise gets stated in
`docs/batch-interface.md` §6 (shipped), #524's frozen CLI contract plus its
ratchet policy (planned), this paper (planned), CAP-21's four kit READMEs
(planned, told to link rather than restate), and #591's reproducibility appendix
(planned) — plus `docs/intended-use.md` from the tool-qualification playbook,
which will state the same scope boundary in negative form. That is six
maintenance obligations over one body of fact, in a project that recorded i18n as
a non-goal on precisely the "ongoing tax with no requesting user" reasoning
(`ARCHITECTURE.md`, i18n decision). The prose-per-audience axis is the wrong seam.

## The reframing: cut along guarantees, not documents

Make the primary artifact a **guarantee registry** — in-tree, machine-readable,
one row per promise:

| field | example |
|---|---|
| `id` | `JLS-G-BATCH-STDOUT-ORDER` |
| `claim` | watched-element stdout order is element-name order, Unicode code point |
| `axes` | rerun, machine, platform (Linux/Windows/macOS/arm64) |
| `exceptions` | none |
| `enforced_by` | `test/jls/BatchSimulationGoldenTest#watchedElementsPrintInNameOrder` |
| `on_breach` | CHANGELOG entry + major bump or compat flag (`batch-interface.md` §6) |
| `since` | v4.x |

Then a `GuaranteeRegistryTest` in the exact shape of `ExtensionPointCatalogTest`:
every `enforced_by` must resolve to a test that exists and runs; every guarantee
must have a non-empty enforcement or an open issue number in its place. AC-3 stops
being an editorial promise and becomes structurally unbreakable — the build fails
when a guarantee outlives its enforcement, which is the only failure mode that
matters on a multi-year horizon.

Everything downstream then becomes a *rendering*, not a restatement:

- **The white paper** (this issue) renders the registry's normative core into
  instructor prose. AC-2's three sections are three filters over the same table.
- **AC-4's kit links become guarantee IDs.** A kit README citing
  `JLS-G-BATCH-STDOUT-ORDER` can be checked for *resolution*, not just for HTTP
  200. Note that AC-4 as written quietly requires CI infrastructure that does not
  exist: `HelpTopicsTest`'s link checker covers in-jar help only, and no workflow
  in `.github/workflows/ci.yml` link-checks `docs/*.md`. Under the registry
  framing the check is a string lookup against a file already in the tree — a
  genuinely non-code deliverable, which the boundary note claims this is.
- **#524's conformance suite** and this registry are the same object seen twice.
  #524 already requires "the contract version is queryable from the CLI"; that is
  the registry with a version stamp. Sequencing this issue *after* #524 is right,
  but the two should share one artifact rather than #524 producing a contract and
  #589 producing a paper about the contract.
- **#591's reproducibility appendix** — correctly kept separate per the pass-1
  boundary note — gets its content for free. The note's closing observation, that
  #589's enforcement discipline is what makes #591 tractable, is exactly the
  argument for making the discipline a data structure rather than a writing habit.

## The out-of-the-box alternative: make the paper runnable

The strongest instructor-facing artifact this project could ship is not read, it
is *executed*. `jls --guarantees` prints the registry with the contract version;
`jls --verify-contract` re-runs the enforcement set on the instructor's own lab
machine and prints a pass/fail table. A committee that reads a PDF is trusting. An
instructor who runs one command on the actual grading box and gets "14/14
guarantees verified, contract v1, JDK 25.0.3, linux-x86_64" has *already done*
what AC-3 hopes a skeptical reader might. The precedent is in-house and explicit:
the tool-qualification playbook requires its scope statement to live in the
shipped binary, reachable without network, via `jls -h`, pinned by a build-failing
test — because a README paragraph nobody maintains is not the instrument. #589 as
written puts the promise only in a document, which is the weaker half of a pattern
this project already knows how to do properly.

## On AC-1's "stable URL"

`ARCHITECTURE.md` records that hosted web docs are the *planned* future; today the
normative home is repo markdown on GitHub. So "stable URL" resolves to a
default-branch blob URL — which mutates and is not versioned. But a department
asking "what were we promised?" during a grade dispute is asking about a
*release*, not about HEAD. Reuse the seam that already works: ship
`grading-contract-<version>.pdf` and `guarantees-<version>.json` as release
assets, alongside `SHA256SUMS`, `bom.json` and the `.buildinfo`. The contract is
then versioned exactly like the thing it describes, attestable with the same
`gh attestation verify` incantation the README already teaches, and answerable in
2029. No docs site required.

## The honest-holes benefit

AC-2(b) demands determinism "across rerun, machine, JDK, platform." Today the
evidence is uneven: platform is strong (goldens run on Linux, Windows, macOS and
arm64 legs), JDK is weak (the non-25 matrix leg is `continue-on-error` — an
advisory lane cannot back a guarantee), and `reproducibility.md` §5 already
excludes msi, dmg and the container image. The boundary note rightly forbids
inventing guarantees here. Written as prose, that constraint produces an awkward
paper. Written as a registry, it produces the *most credible possible* artifact: a
table where every row names either a test or an open issue number. A committee
trusts a table with visible holes far more than a paper with none — and each hole
becomes a filed feature instead of a paragraph someone talked themselves into.

## Verdict

**endorse-with-reframing.** The outcome is right, the audience is right, and the
enforcement discipline in AC-3 is the best idea in the issue. I am not
disregarding the acceptance criteria — all five survive and get stronger: AC-1
gains release-versioned publication, AC-2 becomes a query over structured data,
AC-3 becomes build-enforced rather than author-enforced, AC-4 becomes checkable
without new link-checking infrastructure, AC-5 becomes the registry's exception
column rendered rather than a section an author must remember to write. What
changes is the primary deliverable: **build the guarantee registry and its drift
test first; the white paper is its instructor-facing rendering, and
`jls --verify-contract` is its runnable one.** Under that framing this issue
stops being the sixth restatement of one contract and becomes the thing that
collapses the other five.
