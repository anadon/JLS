# Issue #781: TASK-C589-1: the grading-contract white paper exists as one instructor-facing document — stability, determinism and provenance, readable as a PDF handout with no repository access
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not a document. The end is **adoption without trust**: a course committee that
will never open the repository must be able to bet a semester's grades on JLS.
#589 and #781 assume the instrument for that is a self-contained PDF. That is
the assumption worth attacking, because JLS is a project whose entire documentary
culture is the *opposite* of self-contained.

`ARCHITECTURE.md:5-7` — "specifics carry a `file` / method anchor so you can
verify rather than trust." `docs/batch-interface.md:13-17` — "Every claim below
is stated from the implementation and carries a code anchor... If code and
document disagree, that is a bug in one of them." `docs/reproducibility.md` §1
is a table that says **No** in three rows, on purpose. This project does not sell
promises; it publishes falsifiable claims next to the job that falsifies them.

"Self-contained enough to survive being forwarded as a PDF with no repository
access" is, structurally, a request for a **detached copy of three contracts that
already exist** (`docs/batch-interface.md`, `docs/reproducibility.md`,
`docs/simulation-semantics.md`) plus a README's worth of provenance prose. A
detached copy has exactly one long-run behavior: it drifts, and it drifts in the
direction of flattering. Section (b) will say "byte-identical" in a PDF long
after the CI lane that checked it went advisory. #783 (the sibling task) tries to
hold this closed by requiring each guarantee to name its test — but that is a
review chore imposed on a document whose format was chosen to be un-checkable.

The project has already solved this exact problem once and recorded the solution.
`ARCHITECTURE.md`, "Help delivery" (recorded 2026-07): the discipline is *not*
"write a second copy for the other audience," it is "keep the canonical content
in a form that can be **published** to the other audience without rewriting," with
`HelpTopicsTest`'s link checker keeping it truthful. #781 proposes the pattern
that decision explicitly declined.

## Reframing 1 — compile the paper, do not author it

`docs/whitepapers/grading-contract.md` should be a **build product**, not a file a
human edits. Mark normative spans in the three canonical documents
(`<!-- contract: stability -->` … or a front-matter `contract-claim:` block per
clause), write *only* the instructor-facing framing and glue by hand, and have a
script assemble the handout plus a generated enforcement table
(claim → `test/jls/BatchSimulationGoldenTest.java`, `VcdExportGoldenTest.java`,
the `reproducibility` job in `.github/workflows/ci.yml`, …).

The payoff is structural, not cosmetic:
- there is one copy of every guarantee, so drift is impossible rather than
  policed;
- #783's AC ("every guarantee names its enforcement") becomes a **generator
  invariant** — a claim span with no `enforced-by` anchor fails the build — instead
  of a thing a reviewer must notice;
- #781's own AC-5 ("no guarantee is invented here") is enforced by construction:
  you cannot write a paragraph that has no canonical source, because you are not
  writing paragraphs;
- the PDF stays a PDF. Pandoc over the generated markdown, published on tag. AC-1
  is fully satisfied.

This is the same move `CliFlagTableTest` already makes for `-h` (the flag table is
the single source, docs are checked against it) and that `ExtensionPointCatalogTest`
makes for `docs/extension-points.md`. #781 is asking for a fourth normative surface
in a project that has consistently refused to have two of anything.

## Reframing 2 — the deliverable is a receipt, not a claim

The more interesting reframe. A committee reading "JLS guarantees byte-identical
batch output" is being asked to trust. A committee running

```sh
docker run --rm ghcr.io/anadon/jls --contract
```

and watching *their own machine* print the contract version, the axes it just
re-verified locally, the build provenance of the binary in front of them, and the
named exceptions — is not trusting anything. That is a categorically stronger
instrument than a PDF, it is unique among JLS's peers (Logisim-Evolution and
Digital ship neither a contract nor a provenance story), and it is the thing
CAP-21's four kits would actually want to embed.

#524 already requires "the contract version is queryable from the CLI itself, so
an adapter can refuse an incompatible build with a named error." Extending that
from a version string to a self-describing contract report is a small delta on
work already ordered *before* this task, and it turns the white paper from the
whole product into the explanatory companion around a self-verifying artifact.

**This is not speculative decoration — the gap is load-bearing today.**
`JLSStart.FLAGS` (`src/jls/JLSStart.java:759-789`) has no `--version` flag at
all. A batch grading run emits nothing that identifies the build that produced
it. So section (c) — "how a score traces back to a circuit, a vector set and a
build" — cannot be written honestly at HEAD except as *"record the jar's sha256
yourself, out of band, because the run will not tell you."* Under #781's own
AC-5 that is a feature to file, and it is a very small one: a provenance line (or
`--provenance` sidecar) carrying version, contract version, and build id. Filing
and landing that is worth more to a real course than the paragraph describing
its absence.

## What section (b) is going to run into

The determinism section is the one that will not survive contact with the
enforcement-naming discipline. At HEAD the cross-platform evidence is
**advisory**: `.github/workflows/ci.yml` marks the `windows` job
`continue-on-error: true` (line 155) and the `macos` job likewise (line 263), and
the JDK 26 leg of `build` is advisory by design. The golden tests
(`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, `RiscvCpuGoldenTest`) do run
on all three platforms — but only the Linux/x86_64/JDK-25 leg can turn CI red.
The multi-arch container (`linux/arm64`, `linux/riscv64`) and the container axis
generally have no batch-output determinism check at all.

So the honest sentence is "byte-identical across rerun and JDK on Linux x86_64
(gated); observed but not gated on Windows, macOS, arm64, riscv64; unverified in
container." That is a weak sales document — and it points straight at the highest-
value artifact hiding inside this whole feature: **a determinism conformance lane**
that runs one circuit + vector set across the arch/JDK/container matrix and
asserts byte-identical stdout and VCD. That is a modest CI job. It converts
section (b) from a hedge into a claim, gives #783 something real to cite, and is
worth more to CAP-21 than the paper it supports.

## Alignment

The work strengthens the arc — CAP-06 (#300) makes grading a verdict rather than
a string diff, #524 freezes the interface, and someone has to tell instructors
what they are getting. It does not duplicate #591 (the boundary note on #589 is
correct: source-to-derivative, different failure modes). It pulls against the
project only in *form*: a hand-authored detached restatement in a codebase whose
distinguishing virtue is that nothing is stated twice.

## Concretely, what I would do instead

1. Land the `--version`/provenance-in-output feature first (tiny; unblocks an
   honest section (c)), and the determinism conformance lane second (unblocks an
   honest section (b)). Both are prerequisites #781's AC-5 implies but does not name.
2. Make `docs/whitepapers/grading-contract.md` generated from tagged spans in the
   three canonical docs plus a generated enforcement table; hand-write only the
   framing. Publish the PDF on tag.
3. Fold "self-contained handout" and "runnable receipt" into one deliverable: the
   PDF's first page is what `jls --contract` prints, so the document and the tool
   cannot disagree.

**I am disregarding AC-1's "self-contained" framing as the design driver.** Not
the outcome — an instructor must still be able to read one thing and know what is
promised — but the inference that self-contained-for-the-reader requires
detached-in-the-tree. It does not, and in this project it should not.
