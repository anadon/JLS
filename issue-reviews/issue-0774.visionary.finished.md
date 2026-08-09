# Issue #774: TASK-C588-1: the grading-determinism note publishes, with every competitor claim cited to that competitor's own tracker
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

CAP-36 (#520) rests on one finding from the niche survey: in this corner of the
world, adoption flows from published, citable evidence and instructor
word-of-mouth, not from features. JLS's real edges — a frozen batch interface,
byte-deterministic output, reproducible builds, goldens that fail when a
promise breaks — are currently invisible to the only audience that matters,
because they live in `docs/batch-interface.md` §6 where no instructor will ever
look. #774 is the cheapest slice of fixing that. The instinct is right and the
0.5–1 mw price is right. I endorse publishing this note.

What I want to change is *what the note argues* and *where it lands*. As
scoped, the issue produces a perishable bug-catalog in a directory its audience
does not browse. Three reframings follow; the first is the one I care about.

## Reframing 1 — argue the structure, not the bug list (this is the whole review)

AC-3's required citation set is entirely bug-shaped: Logisim-Evolution #1546,
#598, #950, #1123, #441, #185; CircuitVerse's manual educator flow. Every one
of those is a defect that upstream can fix. AC-4 and KC-36-1 then correctly
require that a fixed claim be *retracted, not defended*. Compose those two and
read the note's five-year trajectory: it decays into retractions, and a
single-maintainer project has signed up for an unbounded recheck duty against
two actively developed competitors. The freshness gate is punted to
TASK-C588-3, but the *obligation* is created here, at publication.

The reframing that makes this problem disappear: **make the bugs evidence for a
structural claim rather than the claim itself.**

The durable, non-perishable, and frankly more damaging sentence is:

> Logisim-Evolution's command-line verification is not covered by a published
> stability contract, and its output is not pinned by byte-exact golden tests.
> The consequences are visible in its own tracker: [#1546], [#441], [#185],
> [#598], [#950], [#1123].

That sentence stays true until the competitor publishes a contract and pins
their output — at which point it is not a retraction, it is a good outcome the
note can celebrate in one line. It satisfies AC-3 verbatim (every named issue
is still cited and linked), it satisfies AC-5's fairness demand better than a
defect list does, and it is the argument JLS is actually entitled to make. Six
bugs in `-test` are an anecdote; six bugs in `-test` *plus* the observation that
nothing in that project would have failed when they were introduced is a thesis.

The same move applies to CircuitVerse: the claim is not "grading is manual"
(they know, it's in their own docs, and it is a design choice for a browser
tool) — it is "grade passback carries a hand-entered number, so the LMS record
has no machine-checkable provenance back to a circuit, a vector set and a
build." That is a structural statement about the provenance chain, and it is
exactly the axis #589's white paper is built on.

## Reframing 2 — the honest thesis today is about the substrate, not about grading

The issue is titled and scoped as a *grading* comparison, ordered after #300.
Check what #300 says about the present: today's shipped grading story is
`examples/autograde/autograde.py`, and the tree confirms it — `examples/`
contains exactly one file, a Python script that compares three literal stdout
lines for one input vector (`examples/autograde/autograde.py:53-57`). A
submission wrong on every other vector and right on that one passes.

So the note as scoped cannot be written until a 12–20 mw capstone lands, and
its `ordering_after: [300, 512]` is honest about that. But the claim JLS can
make *today*, with no new code, is stronger than the one it is waiting for:

- `docs/batch-interface.md` §6 freezes the `-t` grammar, the stdout format and
  the VCD profile, with a stated change rule (CHANGELOG + major bump, or a
  compatibility flag).
- §4 states the VCD is byte-deterministic and §5 names the tests that enforce
  it: `VcdExportGoldenTest`, `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder`,
  `CliFlagTableTest`, `CliSmokeTest`.
- The jar and BOM are bit-reproducible with a `.buildinfo` and an independent
  rebuild recipe, and releases carry provenance attestations.

Neither competitor publishes any of that. That is the comparison, and it is
about the *substrate a grader is built on*, not about the grader. I would
retitle the note accordingly — "what your autograder is standing on" rather
than "grading determinism" — and unblock it from #300 entirely. #300 then
*strengthens* the note when it lands (add the verdict paragraph) instead of
gating it. A 0.5 mw document that has to wait behind a 12–20 mw capstone is not
a 0.5 mw document.

## Reframing 3 — the rubric genre beats the comparison genre, and it is more on-brand

The most persuasive and most JLS-shaped version of this note is not a
comparison at all. It is: **"A grading contract: what one is, and how to check
whether your simulator has one."** Five questions an instructor can apply to
any tool — is the output format specified anywhere? is it frozen across
versions, and under what rule? does a test fail if it changes? can a score be
traced to a circuit, a vector set and a build? what happens when the promise
breaks? — with the three tools scored against them and the competitors' own
trackers appearing as worked examples of what happens when the answer is "no".

This genre is what this project already writes. `docs/standards-adoption/11-costed-rejections.md`,
ARCHITECTURE.md's recorded decisions with revisit triggers, and README's flat
statements about the unsigned macOS build and the deliberately absent GPG
signature are all the same voice: *here is exactly what we do and do not
promise, and why.* A head-to-head note is a different, weaker genre — it reads
as marketing precisely to the skeptical reader you most want to convince. The
rubric is more useful to the instructor, harder to accuse of bias, ages far
better, and lets a losing row be stated without it feeling like a concession.

## Two things the issue should reuse rather than reinvent

- **The citation discipline already exists in-tree.** `docs/capability-roadmap/lf-04-formal-and-grading.md`
  carries `**[verified: README fetched this pass]**` annotations against
  upstream URLs (e.g. lines 534-537, 889-892), and `docs/standards-landscape.md`
  and `docs/hdl-support-research.md` do the same. TASK-C588-3's freshness gate
  should extend that convention, not invent a second one.
- **The evidence files #588 sources from do not exist.** #588 says the
  citations come from `docs/reviews/evidence/2026-08-niche-survey/`; there is
  no `docs/reviews/` in the tree. The six issue numbers therefore reach the
  writer as transcription from an issue body. They must be re-fetched and
  re-read at write time regardless — so cite upstream directly and drop the
  evidence-file indirection, which can only go stale between the survey and the
  note.

## The distribution problem the issue does not see

AC-1 requires the note be "reachable from the docs site." **There is no docs
site.** ARCHITECTURE.md records hosted documentation as the *planned future*;
today `docs/*.md` is read on GitHub and the in-jar help is HTML 3.2 for
students. An instructor comparing simulators does not browse
`github.com/anadon/JLS/blob/master/docs/comparisons/`. Written as specified,
this note's realistic readership is contributors.

That is not a reason to skip it — the note has to exist before it can be
distributed — but the capstone should be honest that the artifact is one third
of the value and the channel is the other two thirds. Concretely, within this
band: a linked section in README (which *is* the project's front door), and a
single-page GitHub Pages publish out of `docs/`, which also discharges AC-1's
premise and pays forward the recorded "hosted docs are the planned future"
direction. Longer term the capstone's own evidence (#508: prominence flows from
papers) says PF-4 is where this content actually reaches the audience — which
argues for writing the note in a form that can be lifted into the paper's
related-work section rather than as standalone marketing prose.

## On AC-5, concretely

AC-5 is the best-designed criterion in the issue and a reviewer should hold it
to a real standard, not a token one. The honest losses in *grading specifically*
are: CircuitVerse requires zero installation, runs in a browser on a
Chromebook, and already has working LTI integration with the LMS — for an
instructor whose blocker is "300 students cannot install software," that beats
every determinism argument JLS can make. Logisim-Evolution has the installed
base, the course materials, and an FPGA/HDL flow JLS is still building. If the
note's named advantage is smaller than one of those, it has not met AC-5.

## Verdict

Endorse the publication; reframe the argument. Cite the structure and use the
bugs as its evidence (Reframing 1), retitle around the substrate and unblock
from #300 (Reframing 2), and prefer the rubric genre (Reframing 3). I am not
disregarding the acceptance criteria — AC-1, AC-3 and AC-5 survive all three
reframings unchanged, and AC-4's retraction duty shrinks to near zero, which is
the point.
