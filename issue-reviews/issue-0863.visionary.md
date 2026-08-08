# Issue #863: TASK-C582-3: one line of policy names release-asset downloads as the adoption KPI and says whether store counters are in the number — so stars stop being quoted
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the YAML and the five ACs and the ask is: *stop letting an unowned number
stand in for the question "is anyone using JLS?"* That is a good instinct and it
is cheap. But the issue answers a narrower question than the one #508 asked.
#508's success condition is explicit: **"external courses running JLS labs
within 18 months, or the bottleneck is provably channel, not capability."** The
finding underneath it is that adoption of `anadon/JLS` is zero while the one
live user base — WashU CSE 260M — runs on the `bsiever/JLS` fork. Nothing in a
release-asset download series can distinguish "a course adopted JLS" from "a
Hacker News thread" from "a mirror re-fetched the jar." At the scale JLS
actually operates (3 stars, 0 external issues), the metric this issue installs
has noise wider than any signal it could carry, and it is being installed as
*the* KPI — the number that, per AC-5, gets re-decided rather than drifted.

Swapping stars for downloads replaces a vanity number with a slightly less
vain one. Neither changes a decision. That is the test a KPI has to pass.

## Finding 1 — the KPI should be courses; downloads is an instrument, not the KPI

The reframe I would take: **name *known adopting courses/instructors* as the
adoption KPI, and release-asset downloads as its only automated instrument.**
Concretely, `docs/adoption/courses.md`: one row per known adopter — institution,
course, term first seen, evidence URL (syllabus, lab handout, paper), fork or
upstream, date last checked. Hand-curated, zero automation, and it is the
artifact that actually moves: WashU/bsiever is row one *today*, before any
script runs, and #508's item 1 ("contact Bill Siever, ≈0 mw, highest leverage")
writes row two. That file makes the 18-month success condition directly
readable; the download series never will.

This is not a rejection of #861/#862 — the collector is worth its 0.5 mw as a
background series. It is a demotion of downloads from "the adoption KPI" to
"the leading indicator we can automate," which is what it honestly is.

## Finding 2 — the artifact already has a genre; do not invent a new one

`ARCHITECTURE.md:233` is a **"Recorded decisions"** section whose entry shape is
exactly what ACs 1/2/3/5 describe: a decision, its rationale, its scope, and a
named **revisit trigger** (i18n non-goal, help delivery, FlatLaf default, plugin
trust boundary, sole simulation strategy). AC-5 — "what would make the metric
wrong enough to replace" — *is* a revisit trigger, in the project's own words.
Three existing planning documents already prescribe this destination for
decisions of this size (`docs/standards-adoption/07-waveform-formats.md:146`,
`09-cra-and-supply-chain.md:129`, `01-iec-ieee-symbols.md:282`).

A freestanding `docs/adoption-metric.md` is a new document in a `docs/` tree
whose other 20 files are normative specs and research reports. It has no
gravity, nothing links it, and no test touches it — the exact profile of a
document that goes stale unnoticed. One ~14-line recorded decision satisfies
AC-1, AC-2, AC-3 and AC-5 in the place contributors already read, and costs
less than the issue's own 0.25 mw band.

Sketch, so the alternative is concrete rather than gestural:

> ### Adoption measurement: courses are the KPI, downloads the instrument (recorded 2026-08, #508/#863)
> Adoption is measured as **known adopting courses** (`docs/adoption/courses.md`,
> hand-curated, evidence-linked), instrumented by **release-asset download
> counts** (`docs/adoption/downloads.tsv`, collected by `scripts/…`, #861/#862).
> Stars are not an adoption measure and are not quoted as one. Store counters
> (Flathub/winget/Homebrew) are **separate columns, never summed into the
> release total**; a channel whose counter is unavailable is recorded as
> `unavailable`, never 0. Known limits: counts are cumulative, include bots,
> mirrors and re-downloads, and one careful user downloads four assets.
> **Revisit trigger:** a named course adopts JLS and the download series shows
> no attributable step (the instrument is blind), or a channel's own counter
> becomes the dominant install path.

## Finding 3 — AC-4 is vacuous as written (verified)

`grep -rniE '\bstars?\b|stargazer'` across the tree returns exactly one hit,
`riscv/README.md:132`, describing a star-shaped net. **No file in this
repository quotes stars as adoption.** The only place stars are quoted is
#508's own issue body and the planning prose around it — which a documentation
task cannot edit and should not try to. AC-4 will be closed by doing nothing,
or by an implementer inventing scope. Replace it with the one thing that would
be true and useful: state in the decision that stars are not an adoption
measure (as above), so the next reader has a citation to point at.

## Finding 4 — AC-3 is #861 AC-4, written twice

#861 AC-4 already requires the data file to document "what each count means and
its known limitations (counts are cumulative, mirrors are not visible, and so
on)." #863 AC-3 requires the policy doc to name "cumulative counts, bots,
mirrors, re-downloads." Two artifacts, one paragraph, guaranteed to drift —
and drift *in the caveats*, which is the half of a metric that stops people
over-reading it. The limitations belong in exactly one place: the schema header
of the data file, with the decision entry linking to it. This is a
one-sentence fix, but it is the kind of duplication #508's process findings
flagged as the tracker's characteristic failure.

## Finding 5 — "per-asset downloads" summed is a broken number before it is a wrong one

A JLS release ships the jar, `bom.json`, `.buildinfo`, `SHA256SUMS`, per-arch
deb/rpm/AppImage/msi/dmg, and per-OS installer checksums
(`.github/workflows/release.yml`). A single user who follows the README's own
verification instructions downloads four assets; an autograder pulls the
container (not a release asset at all, so the most adoption-implying artifact
JLS ships is *invisible* to this metric); `SHA256SUMS` and `bom.json` fetches
are supply-chain tooling, not people. If the policy is going to name one
number, it must name *which assets count* — I would count installers and the
jar, and report verification assets separately or not at all — and it should
say plainly that the container image is the strongest available adoption signal
and is not currently countable. That sentence is worth more than everything
else in the document.

## What I am disregarding, and why

I am setting aside AC-4 (vacuous — nothing to update) and the premise of AC-1
that release-asset downloads *are* the adoption KPI. Executing #508's item 7
literally produces a document that satisfies its own ACs while leaving #508's
success condition unmeasured — the exact failure mode the review was
commissioned to prevent. The recommendation was a line item in a two-quarter
plan whose first and highest-leverage item was "reconcile with the live user
base"; the metric should be pointed at that.

## Alignment note

The work does not pull against the project's arc, but it does illustrate its
current drag: #508 recorded "no new tier:feature/tier:task until two capstones
close," and #863 was filed the following day as the third task splitting a
0.5–1 mw feature, where the third task is one paragraph ordered after another
task. Folding this into #861 as a sixth AC ("the decision entry lands in
`ARCHITECTURE.md`, linking the data file") removes an issue, removes the
duplication in Finding 4, and lands the policy alongside the number it
describes.

**Endorse-with-reframing**: write the decision, but write it as a recorded
decision in `ARCHITECTURE.md`, name courses as the KPI with downloads as its
instrument, say which assets count and that the container is uncounted, and
close this issue into #861 rather than shipping it separately.
