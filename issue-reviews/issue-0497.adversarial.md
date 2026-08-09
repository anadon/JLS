# Issue #497: Virtual-hardware / virtual-logic parity, part 2 of 3: layers L5-L9, the governance band, the ranked gap list, and the eight unowned programmes P14-P21
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is not a work item; it is a 124 KB archival dump of a deleted branch's
design document, filed to preserve text before the branch disappears. Its
technical due diligence is unusually strong — the great majority of the
file/line citations I spot-checked resolved exactly as claimed — but the
issue has no acceptance criterion of its own (it cannot be "done," only
ever left open), rests on decision labels (D1/D3/D4/D5, K5/K8/K9) that are
unverifiable from this issue or this repo, contains at least one factual
error in a "verified at HEAD" gap-list entry, and contradicts itself on
whether its own flagship "unowned, can-start-today" programme actually has
no dependencies.

## Findings (most severe first)

### 1. [High] A "verified at HEAD" gap-list entry is factually wrong
Gap list item #9 states: *"`DENSE_CAPACITY_LIMIT` is exactly 16 MiB with
zero headroom."* At HEAD, `src/jls/elem/Memory.java:1224` defines
`DENSE_CAPACITY_LIMIT = 1 << 22`, and the code's own adjacent comment
(`Memory.java:1221-1223`) reads *"past this many words (32 MB of longs)
assume sparse use."* `1<<22` words × 8 bytes/word = 32 MiB, not 16 MiB —
off by exactly 2×. The section header preceding the whole gap list says
*"Every gap verified at HEAD."* That claim is false for at least this
entry. This matters beyond the single number: the document's credibility
is built entirely on the promise that its citations are ground truth
rather than branch-era paraphrase, and this is a case where a reader who
trusts the document without checking would carry a wrong constant into
planning P1-S0/P12.
**Recommendation:** re-derive every numeric gap-list claim against HEAD
before this rescue is treated as authoritative (not just this one), and
correct #9 to 32 MiB.

### 2. [High] The issue has no acceptance criterion and cannot be closed or falsified
Every other filed issue in this repo (per `ARCHITECTURE.md`'s and
`CONTRIBUTING.md`'s norms) is a unit of work with a done-state. This one
is explicitly *"non-normative"* (stated twice) and its only stated purpose
is preservation — there is no checklist, no "close when," no owner, no
milestone. That means: (a) it can never be marked resolved on its own
terms, since "preserve this text" has no failure mode once posted; (b) it
also can never be shown wrong in the way a normal issue can, because there
is nothing to test against. As a tracker artifact this is a 2,000-line,
permanently-open reference document masquerading as an issue, and 76
other issues (per its own audit) are apparently about to receive siblings
in the same shape. That is a maintainability hazard for the tracker
itself, independent of the content's accuracy.
**Recommendation:** either close this issue immediately after filing (its
job — preservation — is complete the moment the text lands, and it says
so itself: *"the rescue is therefore complete"*), or file the durable
content as a `docs/` file in a real PR instead of leaving it open as an
uncloseable reference issue.

### 3. [Medium-High] P21 contradicts its own "no dependencies, starts today" claim
P21's dependency line reads: *"Depends on: nothing in JLS. It can start
immediately and in parallel."* But two sections earlier, the same
document ("Where the image lives, and the contradiction that must be
resolved") states the nightly CI lane P21 is costed to unblock requires
committing a guest image, which conflicts with an existing exclusion on
committed images/checkpoints/LFS, and that resolving this *"requires
explicitly reopening the exclusion, which is a decision, not an oversight
to route around."* A decision that must be explicitly reopened by the
maintainer is a dependency. P21's roll-up entry ("the thing that boots,"
4-6 weeks, floor 4-6 weeks) is therefore not truly startable in parallel
without that governance step landing first — the document just asserted
both things three paragraphs apart.
**Recommendation:** add the exclusion-reopening decision as an explicit
P21 dependency, or state plainly that only the guest-image *build* (not
its CI-lane payoff) is dependency-free.

### 4. [Medium] Load-bearing decision/kill-criterion labels are unverifiable from this issue or this repo
The document repeatedly cites `D1`, `D3`, `D4`, `D5` (recorded decisions)
and `K5`, `K8`, `K9` (kill criteria) as settled facts — e.g. *"D5 deletes
riscv/"* gates L6's reference-oracle plan and P19's ordering constraint;
*"K5, the criterion that would abandon the Linux target, is written
against"* the 93/92/84.5 self-imposed coverage bar. None of these labels
resolve to anything in this repository: there is no `docs/` file, no
committed decision log, and no `D5`/`K5`/`K8`/`K9` string anywhere under
`docs/` at HEAD. Per this issue's own table, they live in **part 3**,
filed as a separate issue not yet available for cross-checking at review
time. A reader evaluating #497 in isolation is asked to accept "D5" as
fact with no way to confirm it is ratified policy rather than a
branch-only proposal that (like the parity contract) was never merged.
**Recommendation:** when part 3 lands, this issue should be edited to
link it, and any claim gated on an unratified D/K label should say so
inline rather than reading as settled.

### 5. [Medium] Scope and feasibility: ~155-250 maintainer-weeks against a project that documents itself as bus-factor 1
The roll-up totals 51-90 weeks (floor 24-36) for P14-P21 alone, plus
105-159 weeks drawn from the wider roadmap, for an honest total of
"three to five maintainer-years at bus factor 1" — the document's own
words. `ARCHITECTURE.md`'s recorded decision on #221 (discrete-event
interpreter is the sole simulation strategy) treats a second execution
strategy — which L9/Mode C requires — as *"premature optimization until
CPU-scale designs are actually common,"* with the revisit trigger being
*"a concrete CPU-scale design ... that is unusably slow interactively."*
This programme's own purpose is to create that CPU-scale design in the
first place, so L9 is proposing to spend part of a multi-year programme
building the very trigger condition its prerequisite (#221) says must
exist before L9 is justified. The document is aware enough to gate L9
behind measuring α first, which mitigates this, but the circularity
between "build the case that justifies the CPU-scale second engine" and
"the second engine is premature until a CPU-scale case exists" is never
named as such.
**Recommendation:** state the #221 circularity explicitly in L9 rather
than leaving it implicit in the "gated, deliberately last" framing.

### 6. [Medium] The programme's own justification is satisfiable by its cheapest floor, not its stated goal
L6 states its shipped-before-Linux payoff — a diagnostic naming the first
retirement-record divergence — *"is a better datapath assignment than
anything in this software class ... and it justifies the program on its
own."* That sentence sets up a rhetorical off-ramp: because the document
itself declares the 3-4 week floor deliverable sufficient justification,
an implementer (or a future maintainer under time pressure) could ship
only that diagnostic, declare the programme's justification satisfied per
this issue's own words, and never reach L7/L8's actual Linux boot — the
thing 22 of the 29 gap-list rows exist to unblock. No falsifiable gate
("justified" means X, not Y) is offered for that claim.
**Recommendation:** if "justifies the program on its own" is meant
rhetorically (to sell the programme, not as an exit criterion), say so;
otherwise this sentence should not appear next to programme-level cost
bands where it reads as an acceptance criterion.

### 7. [Low] Gap-list severity rubric is undefined
The 29-row gap list assigns `fatal` / `major` / `moderate` / `unknown,
load-bearing` with no stated criteria for the boundary. Row 5 (byte
lanes, blocks Linux boot directly) and row 18 (HDL export omits
`RegisterFile`/`FieldExtend`, blocks this programme's own "no HDL-round-trip
claim" requirement) are both hard blockers for their respective goals, yet
rated `fatal` and `major` respectively, with no rule distinguishing them.
Ranking without a rubric will be re-litigated the first time someone
reprioritizes.
**Recommendation:** add one sentence defining what separates `fatal` from
`major` (e.g. "fatal = blocks the whole objective; major = blocks one
layer").

## What's solid

- Nearly every checkable file/line citation resolved exactly as stated on
  spot-check: `HdlExporter.EXPORTED` (22 classes, omits `RegisterFile` and
  `FieldExtend` — `src/jls/hdl/HdlExporter.java:422-428`), `LogicElement`'s
  24 sealed permits (`src/jls/elem/LogicElement.java:17-21`), the JaCoCo
  floors verbatim (`pom.xml`'s `jls.sim` 0.930/0.920/0.845, `jls.elem`
  0.730/0.700/0.585, `jls.collab.op` 0.905/0.895/0.750, bundle
  0.545/0.535/0.505), the PIT `jls.sim.*` glob catching `jls.sim.equiv`
  automatically, `Simulator.probeSample`/`afterEvent` at the cited lines,
  `Circuit.addElement` (`:342`) → `Util.partition` (`:145`), the nightly
  cron's single-lane convention (`.github/workflows/ci.yml:8-13,22-25`),
  `ElementSimulationGoldenTest`'s `RegisterFile` `EXEMPT` entry, and the
  four named ratchet tests (`HeadlessCoreRatchetTest`,
  `NullMarkedRatchetTest`, `PackageInfoRatchetTest`, `SealedHierarchyTest`,
  `ExtensionPointCatalogTest`) all existing at the paths given.
- The self-flagged reference-oracle weakness in L6 ("the harness needs a
  reference that does not share authorship with the design, or it proves
  the generator self-consistent") is honest and correctly identifies a
  real validity threat rather than hiding it.
- The "contradiction that must be resolved" section (guest image vs. the
  exclusion policy) is a genuinely useful catch — the document finds its
  own inconsistency; it just doesn't propagate the consequence into P21's
  dependency line (finding #3).
- Cost bands are explicitly and repeatedly labeled "not measurements,"
  which is the right hedge for analogy-based estimates.

## Verdict rationale

`needs-rework`: the technical grounding is real and mostly accurate, but
this is filed as an issue with none of an issue's properties — no
acceptance criterion, no owner, no closable state — while simultaneously
asking readers to accept unverifiable cross-issue decision labels as
settled and containing at least one concretely wrong "verified" figure.
Before this is treated as a citable source for the ~17 issues that already
reference it, the numeric claims should get a second verification pass and
the document should be either closed (its preservation job is complete) or
converted into a linkable `docs/` artifact rather than an open issue.
