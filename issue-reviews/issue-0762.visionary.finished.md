# Issue #762: TASK-C545-2: an honest feature comparison against the four incumbents, and every badge that answers no real question is removed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task language away and #762 is the one place in the CAP-27 shop-window
program where JLS has to say out loud *what it is relative to the field*. #760 shows
the product, #548/#551/#552 show circuits, #550 fixes first launch — all of them
answer "what does this do." Only #762 answers "why this and not the tool I already
have," which is the question #510 §2 says gets answered against JLS in the first ten
minutes today.

That makes the artifact strategically load-bearing, and it makes the *shape* of the
artifact matter more than the issue admits. The issue specifies a four-column feature
comparison table. A feature matrix is the format that scores tools by row count — and
by #510's own honest self-assessment, JLS loses row count decisively: 2/5 on-ramp,
2/5 scale, 2/5 hierarchy, 2/5 extensibility, 1/5 community, against Digital's 5/5s
and Logisim-Evolution's community. JLS is category-best on exactly one axis
(testing/grading, 5/5) and unique on two more that no matrix row captures well
(a normative written semantics contract; reproducible signed builds). Publishing a
matrix invites the reader to count, and counting is the one game JLS cannot win.

This is not an argument for dishonesty. It is an argument that the honest thing and
the persuasive thing converge on a *different artifact*, and the issue never
considers it.

## Reframe 1: lead with routing, not with a matrix

The strongest, cheapest, most defensible thing JLS can publish — and the thing
literally no competitor in #510's survey has — is a **"use something else if"
block**:

> Use **Falstad** if you want analog intuition in a browser tab; its solver does
> things JLS deliberately does not. Use **CircuitVerse** if zero-install in a
> student's Chromebook is the requirement. Use **Digital** today if you need
> parameterized hierarchy or maximum raw simulation throughput. Use
> **Logisim-Evolution** if your course is already built on `.circ` and its
> community. Use **JLS** if you grade circuits from a command line and need the
> grading contract, the timing semantics, and the build to be written down and
> pinned by tests.

That paragraph is honest by construction (it *begins* with losing), it satisfies
AC-2 more convincingly than a single losing table row, it survives contact with a
skeptical instructor, and it costs a fifth of what maintaining a 12-row matrix costs.
#510 §3's per-competitor "winnable segment" analysis is exactly this content already
written; the issue asks for it to be reshaped into a matrix that discards the
segmentation.

Keep a table, but small — five or six rows chosen where the axis is *decision-
relevant to the routing above* (grading interface, timing semantics, hierarchy/
parameterization, waveform view, install/zero-install, format longevity), not twenty
rows chosen to be comprehensive. Comprehensiveness is what makes a comparison table
rot.

## Reframe 2: the table must join the `CliFlagTableTest` family, or it will rot

This is the deepest misalignment with the project's trajectory. JLS's whole identity
in ARCHITECTURE.md is *claims pinned by oracles*: `CliFlagTableTest` cross-checks the
CLI table against the code, `ExtensionPointCatalogTest` checks the seam catalog "in
both directions," `HelpTopicsTest` link-checks help, golden tests pin the semantics
spec, the build is byte-reproducible and CI re-checks it. Every normative claim in
this repository has a mechanism that fails the build when it drifts.

The comparison table, as #762 specifies it, would be the single most
externally-visible claim surface in the project and the *only* one with no
mechanism. And there is direct evidence this exact failure has already happened:
#510's adversarial verification found JLS's own "no HDL import" claim stale —
`jls.hdl.imp.NetlistImporter` had shipped and nobody updated the prose. A
hand-maintained matrix about four moving third-party targets will drift within one
release cycle, and a drifted honesty table is worse than no table.

Concrete design, in the project's existing idiom:

- One structured source: `docs/comparison/matrix.yaml` (or a fenced block in
  `docs/comparison.md`) with, per cell, `value`, `evidence`, `as_of`.
- `evidence` for a JLS cell must resolve to an in-tree oracle: a test class name, a
  doc anchor, a CLI flag row, a file path. `evidence` for a competitor cell must be a
  dated citation with an `as_of` date.
- `ComparisonTableTest` asserts: every JLS-column `evidence` resolves (test class
  exists, path exists, anchor exists); at least one row has JLS behind (AC-2 becomes
  machine-checked, not "fails review as dishonest" by a reviewer's judgment); no
  competitor `as_of` older than N months without a `stale:` acknowledgement rendered
  in the output; and the rendered markdown in README and in each #553 page matches
  the generated output byte-for-byte.
- Rendering is a build step; README and the switcher pages carry generated blocks
  between markers, the way any generated-doc pipeline does.

That last bullet is not decoration — it is the only way AC-4 is achievable at all,
which is the next point.

## Reframe 3: AC-4 as written is unimplementable

AC-4 requires the content to "live in one place and be referenced by #553's switcher
pages and by the gallery, not copied into them." GitHub-rendered Markdown has no
transclusion. There are exactly three options: (a) link out — which defeats #545's
premise that a stranger sees the comparison while scrolling, before reading anything
or clicking; (b) copy — which violates AC-4 and guarantees five divergent tables;
(c) generate from one source into all surfaces with a drift check. The issue asks
for (a)/(b) semantics and expects (c)'s properties. Say (c) explicitly, or AC-4 is a
review argument waiting to happen.

Note also that the switcher pages (#553) and the README want *different* cuts of the
same data: the README wants four columns at once, a "coming from Falstad" page wants
one column plus that audience's specific gaps. A single YAML source renders both cuts
trivially; a single Markdown table renders neither well.

## Reframe 4: AC-1's evidence anchor does not cover the table

Checked against the tree: `docs/hdl-support-research.md` mentions Digital 30 times,
Logisim 6 times, **CircuitVerse 0 times, Falstad 0 times**. It is scoped to HDL
export/import and explicitly dated "current as of 2026-07-08 … moving targets." It
cannot support rows on install, learning curve, grading, waveforms, hierarchy, or
community for any competitor, and cannot support *any* claim about half the
competitors named in the title.

The real evidence base is #510's per-competitor teardowns, and #510 says that
evidence lives at `docs/reviews/evidence/2026-08-niche-survey/` on branch
`claude/jls-project-review-505pnf`. That directory does not exist on main (`docs/`
has no `reviews/`). So the prerequisite this task actually has is: **land the
teardown evidence on main first** — as `docs/comparison/evidence/` — and point AC-1
at it. Without that, "honest per the #510 teardowns" means "honest per a branch," and
a fresh clone cannot check a single competitor claim, which is precisely the #73 §4
invariant AC-2 of the parent feature invokes.

## On the badge half: I am disregarding AC-3 as written

AC-3 says "decorative badges are removed." The README today carries exactly **one**
badge — OpenSSF Scorecard, line 3 — and it is not decorative. There is nothing to
remove. As written, the badge acceptance criterion is satisfied by an empty diff, and
a task that can be closed by doing nothing is a task that will be closed by doing
nothing.

The real question underneath it is the inverse: the README's genuinely
differentiating, machine-verifiable facts — reproducible builds re-checked per push,
signed provenance attestations, a documented `-t` grading contract, a normative
semantics spec — are buried at lines 60–330 of a 368-line document that opens with
six installer bullet points. A stranger sees an installer manual. Invert AC-3 to:
*the header strip carries at most four signals, each one a claim no competitor can
make, each linking to the artifact that proves it* — reproducible build, provenance,
license, latest release. Whether those are shields.io images or a one-line prose
strip is an aesthetic detail; the requirement is that the first screen states the
positioning #510 §5 recommends ("the maintained, modern successor in the Digital
tradition") rather than describing `.deb` filenames.

## Where the reframed version strengthens the larger arc

Tie each table row to a capability-roadmap item, and the comparison table stops being
marketing and becomes the roadmap's **public scoreboard**: the chronogram row
(CAP-23), the parameterization row (LF-01), the perf row (`riscv/bench_kernel.py`,
unpublished per #510 §4) each name the issue that flips them. One artifact then
serves honesty, positioning, and roadmap accountability at once, and every gate that
lands visibly changes a published cell. That is the elegant version of this issue,
and it is the version that makes the table's inevitable losing rows read as a
commitment rather than a confession.

## Recommendation

Endorse the goal without reservation; it is the right artifact and CAP-27 needs it.
Rewrite the criteria: (1) evidence on main before claims (`docs/comparison/evidence/`
from #510), replacing AC-1's hdl-support-research anchor which covers two of four
competitors and none of the non-HDL rows; (2) one structured source rendered into
README/#553/#551 with `ComparisonTableTest` in the `CliFlagTableTest` idiom, which
makes AC-2 and AC-4 mechanically true instead of reviewer-adjudicated; (3) lead the
section with a "use something else if" routing block and cap the table at six
decision-relevant rows; (4) replace AC-3 with a positive badge/header specification,
since there are no decorative badges to remove.
