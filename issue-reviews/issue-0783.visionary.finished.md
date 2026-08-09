# Issue #783: TASK-C589-2: every guarantee in the white paper names the test that would fail if it stopped being true, the limits get their own section, and CAP-21's kits link it under a live link check
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#783 is the enforcement half of #589's white paper. Its real subject is not a document —
it is a claim about how JLS should relate to its own promises: **a guarantee JLS states
should be a thing that can fail loudly, not a sentence someone wrote.** That instinct is
the strongest idea in the whole CAP-36/CAP-21 cluster, and the #589 dedup comment already
identifies it as the load-bearing constraint (it is what makes #591's reviewer-runnable
appendix tractable).

The instinct is right. The *unit of work* is wrong three times over, and one pair of
acceptance criteria in the #589 lineage silently contradict each other.

## The contradiction nobody flagged

#781/AC-1 requires the paper be "self-contained enough to read as a PDF handout with no
repository access." #783/AC-3 requires every guarantee to "point at the conformance suite,
ratchet or test that would fail if it stopped holding, so a skeptical reader checks instead
of believing." A PDF that names `CliContractConformanceTest` gives a reader with no repo
access nothing to check — it gives them a second thing to take on faith, dressed as
evidence. As written, AC-3 is a *citation* discipline that reads like a *verification*
discipline.

**Concrete alternative — the paper ships its verifier.** Give the reader one command that
runs against *their* installed build and prints a pass/fail table of the very guarantees
the paper states: `scripts/verify-guarantees.sh` (or a `--verify-contract` mode riding on
#524's "contract version is queryable from the CLI"). Then AC-3 is discharged for the
audience #589 actually names — a course committee that will never open the repository —
and the same script is #591's AC-4 appendix nearly for free. The project already has the
idiom and the taste for this: `wayland-rig-selftest.sh`, `macos-rig-selftest.sh`,
`x11-rig-selftest.sh`, `icestick-handoff-selftest.sh` — JLS habitually makes the checking
apparatus itself checkable. A guarantee-verification story that stops at "here is a test
name" is below this project's own bar.

## Reframing 1 — the binding belongs in a register, not in the paper

The white paper is a *derivative* surface. The guarantees it restates originate in
`docs/batch-interface.md`, `docs/simulation-semantics.md`, `docs/file-format.md`,
`docs/reproducibility.md`, and the README's checksum/attestation paragraph. Annotating the
paper leaves those originals unannotated and creates a fifth place a promise can drift.

And the pattern #783 wants **already exists, hand-maintained**:
`docs/batch-interface.md` §5 ("Relationship to the golden tests") names
`BatchSimulationGoldenTest`, `SequentialGoldenTest`, `VcdExportGoldenTest`,
`CliFlagTableTest`, `CliSmokeTest`; §6 states a frozen stability promise with a versioning
rule. That is AC-3 and half of #781, shipped, for the exact contract the paper is about.
Nothing checks it. Rename a golden method and §5 lies silently. Add a §6 clause and no
enforcer is required. So the honest reading of #783 is not "annotate a new document" — it
is **"make §5 a machine-checked artifact and let every surface render from it."**

JLS's house answer to this shape is a bidirectional catalog test, not a per-document
assertion: `ExtensionPointCatalogTest` (doc table ⇄ `ExtensionPoint` constants, both
directions), `CliFlagTableTest` (one authoritative table; usage text generated, so a second
hand-maintained list cannot exist), `FileFormatSpecTest`, `HelpTopicsTest`.

**Design.** One guarantee register — records, not prose — each carrying:

| field | why |
|---|---|
| `id` | `BATCH.TGRAMMAR.FROZEN`, `DET.JAR.BYTE_IDENTICAL`, `CLI.EXIT.STATUS3`, `PROV.ATTESTATION` |
| `statement` | one normative sentence |
| `axes` | rerun / machine / JDK / platform / container — #781 already demands these |
| `exceptions` | **required, may not be empty-by-omission** (see Reframing 2) |
| `enforcer` | test class + method, ratchet, or CI lane |
| `cadence` | every push / nightly / per release / manual per release |
| `state` | `enforced` / `asserted-unenforced` / `aspirational:#NNN` |

`GuaranteeRegisterTest` then asserts, in **both** directions: every named enforcer resolves
to a real `@Test` method (reflection, as `HelpTopicsTest` and `FileFormatSpecTest` already
do), and every claim-bearing surface — white paper, the four kits, README, in-jar help —
cites only ids that exist. The direction #783 omits is the expensive one: a *new* guarantee
appearing in prose with no id. Be honest that a test cannot fully catch that; a review
checklist plus "no contract sentence without an id" as a CONTRIBUTING rule is the real
control.

**`cadence` is the field that makes this more than bookkeeping.** The paper's three sections
draw enforcers from three maturity levels: stability is per-push (goldens, CLI tests);
determinism is per-push for the jar/BOM but the README already concedes installers are *not*
byte-reproducible; provenance (cosign, `gh attestation`, SHA256SUMS) is enforced by a
workflow that runs **only on tag push**, and the Wayland row is a *manual once-per-release
spot-check*. "Names its enforcing test, and the enforcer is running" is satisfiable by
pointing at a checklist a human does four times a year — and the paper would read as though
CI proved it. Enforcement strength is itself a claim; a skeptical reader is owed it.

## Reframing 2 — the limits section should not exist

AC-5 asks for a limits section "placed and weighted comparably to the guarantees." A prose
limits section is the single most decay-prone artifact you can commission: when a feature
lands, someone's diff touches the strengths; nobody's diff touches the limits appendix. Six
months on it is a museum, and the instructor learns the boundary from the failed midterm
anyway — the exact outcome AC-5 exists to prevent.

**Make `exceptions` a required field on every guarantee record and delete the section.**
Then it is structurally impossible to state a guarantee without stating its boundary, and
the limits render wherever the guarantee renders — in the paper, in each kit, in the
verifier's output — always adjacent to the claim they qualify, never as an appendix a reader
skips.

The README is the existence proof that this form is better. Its most trustworthy prose is
exactly this shape: *"the checksums identify the exact bytes… but the installers are* not
*byte-reproducible"*; the macOS-signing paragraph (unsigned **by choice, not oversight**);
the rpm/AppImage GPG paragraph. Each concedes inline, at the point of claim. None of them
lives in a "limits" section, and that is why they land. A paper that hives its concessions
into §7 has learned the wrong lesson from its own README.

## Reframing 3 — a link check is the wrong guard for AC-4

AC-4's failure mode is not a dead link. It is a **live link sitting next to a stale
paraphrase** — a kit README that says "scores are byte-identical across platforms" beside a
working link to a paper that has since qualified the claim. A link checker passes that
forever.

The kits are already doc-tested end-to-end in CI (CAP-21 AC-5 `TemplateDocTest`, in each of
#525/#526/#528/#530). So use the `CliFlagTableTest` move: the kit README carries a
**generated block** between markers, rendered from the register; the test asserts the
rendered block matches. A kit then *cannot* paraphrase — drift shows up as a diff, and there
is no link to rot. If a link check survives at all it needs a self-test, because on this
project's own standard a checker that has never been observed failing is not evidence
(`*-rig-selftest.sh`, and #524's own seeded-violation falsification requirement).

## Sequencing — the mechanism should lead, not trail

`ordering_after: [TASK-C589-1, 525, 526, 528, 530]` puts a 0.5–1 mw box at the end of a
chain running #369/#466 → #524 → four adapters → #781 → here. Roughly 15+ mw of prerequisite
before the first guarantee is machine-checked. Meanwhile the register is useful *today*
against docs that already exist and already make frozen promises: batch-interface §5/§6,
`docs/reproducibility.md`, simulation-semantics' normative status, the README's
attestation-scope paragraph. Land the register first and the ordering inverts: #781's paper
becomes substantially *rendered* rather than written, each kit gets its generated block as it
lands, AC-4 dissolves, and #524's ratchet has somewhere to record its versioning policy as a
citable record instead of a paragraph.

There is also convergence evidence: the visionary pass on #784 independently proposes a
capability-claim ledger for the migration pages and names #783 as "asking for the same
machinery from the other end." Two issues arriving at "claims must be derived, not typed"
from opposite directions is the signal that the seam is one level below both. One register
should serve both — capability claims (*does JLS do X?*) and guarantee claims (*does JLS
promise X, and how strongly?*) are the same record with different fields.

## What I am disregarding, explicitly

**AC-5 as a section.** Do not write a limits section; make exceptions a mandatory field.
The stated goal — instructors learn the boundary from us — is better served by a form where
omitting the boundary is a build failure than by a section whose weight is a matter of
editorial intent.

**AC-4's link check.** Replace with generated transclusion into the kit READMEs, checked by
the doc-tests those kits already carry. A link check guards the harmless direction and, if
kept, needs its own self-test to count as evidence here.

**AC-3's scope.** "Every guarantee in the paper" is too narrow. Bind guarantees at their
normative source and render the paper from the register — otherwise §5 of
`docs/batch-interface.md` stays an unchecked hand-maintained list while its restatement in
the white paper is the only verified copy, which is precisely backwards.

I endorse the issue's purpose without reservation. It is the difference between a project
that documents its promises and one that cannot break them quietly, and it is the most
transferable idea in this cluster. It just should not be a footnote-adding pass on one
markdown file.
