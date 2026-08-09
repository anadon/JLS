# Issue #588: FEAT-C36-1: two head-to-head comparison notes publish with runnable appendices — every claim about a competitor's grading or timing cites their own tracker and reproduces on a stranger's machine
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the deliverable away and the end is this: **JLS's one category-best axis
must stop being a claim and start being a thing a stranger can check.** #510
scores JLS 5/5 on testing/grading — "no competitor documents grading semantics
at all" — and 1/5 on community. #520 CAP-36's whole thesis is that in this
niche prominence flows from citable evidence, not features. #588 is the cheapest
lever on that gap.

The goal is right and I endorse funding it now. What I do not endorse is the
*shape*: two essays, a frozen list of nine competitor issue numbers, and an
appendix bolted underneath. That shape duplicates infrastructure #560 is already
building, has no enforcement behind its own kill criterion, ships one note that
is premature, and writes in a genre this project does not have when it already
has a better one.

## Where it pulls against the trajectory

**1. It duplicates #560 and draws the boundary on the wrong axis.** The
"Boundary notes" split #588 from #560 along perf-vs-correctness: "performance
numbers are not re-derived here — cite #512/#560." But read AC-2 next to #560's
AC-1/AC-2 and they are the same engineering object: committed fixtures, pinned
competitor versions, stated settings, one documented command, reproduces on a
third party's machine, at least one row where a competitor wins (#588 AC-5,
#560 AC-3 — even the honesty clause is duplicated). The natural seam is not
*perf vs correctness*, it is **harness vs prose**. There should be one cross-tool
harness that runs JLS, Digital, Logisim-Evolution and CircuitVerse over a shared
workload set and emits both timing rows and verdict rows; #560's table and
#588's appendices are two reports from it. As written, #588 will grow a second
fixture layout, a second version-pinning convention and a second "clean
checkout" runner, and then declare a boundary to avoid noticing.

**2. The freshness gate has no mechanism, only a promise.** AC-4 and KC-36-1 are
the strongest ideas in the capstone — a claim fixed upstream is retracted, not
defended. AC-4 discharges them with "a documented recheck step exists." On a
single-maintainer project a documented manual recheck against nine external
trackers is a decay clock with a nice label. This repo does not otherwise work
this way: `docs/batch-interface.md` is normative *because* `BatchSimulationGoldenTest`
and `VcdExportGoldenTest` pin it; `docs/simulation-semantics.md` is normative
because golden tests fail when it drifts. Every load-bearing document in JLS is
backed by an executable that breaks. The comparison notes, as specified, are the
first exception — and they are the documents most exposed to external drift.

**3. The citation list is the note's weakest evidence, not its strongest.**
"Competitor tracker says X" is an assertion about a bug report, not about the
tool. It ages the moment they ship. A harness that *runs* Logisim-Evolution's
`-test` over a sequential-circuit vector file and records what came out is
strictly stronger evidence than citing their #598, and it self-freshens. Keep
the citations as courtesy context — "their maintainers agree, see #1546" — and
demote them from the load-bearing role AC-3 assigns them.

**4. AC-5 is a patch on a problem the structure created.** A document organized
as nine competitor failures reads as an attack in any voice; "name at least one
advantage" is a fig leaf stapled to it. Restructure and the tone requirement
becomes free — see below.

**5. The timing note is premature; the grading note is ripe.** #510 records that
JLS is the only surveyed tool with a timing engine and **no waveform UI**
(chronogram, CAP-23 #504, named blocking in five of seven teardowns), and that
JLS's 2-state+HiZ domain sits below Digital's per-bit HiZ and DigitalJS's
x-propagation. A note claiming timing honesty invites "show me," and the honest
answer today is "export a VCD and open GTKWave" — plus, under AC-5, two conceded
losses. Net effect on an evaluator: ambiguous at best. Grading is the opposite:
5/5, uncontested, competitors' own trackers concede it.

**6. Its own source material is not reproducible.** #588 sources the required
citations from `docs/reviews/evidence/2026-08-niche-survey/` "in this repo."
That path does not exist at HEAD — #510 says it lives on branch
`claude/jls-project-review-505pnf`. An issue whose thesis is "a third party
reproduces the whole note without asking us anything" rests on evidence a third
party cannot open. Merge the evidence directory or the notes cannot honor AC-3
in the spirit it was written.

## The reframing I would build instead

**Disregarding AC-1 (two notes, both now) and AC-3's fixed citation list as the
primary evidence mechanism.** Three moves:

**A. Publish a spec and a conformance matrix, not a comparison.** Write
`docs/grading-contract-conformance.md`: what a grading contract must guarantee —
byte-stable output for a fixed input, an exit status that means *ran fine, answer
wrong* (#300's fourth status), vectors that can drive sequential circuits,
don't-care inputs, a versioned vector format, a stability promise with a
CHANGELOG rule. Then a matrix scoring every tool *by running the suite*, JLS in
the same table under the same severity. This is the project's native genre —
normative prose pinned by executables — and it flips the rhetoric: instead of
"they are worse," it is "here is the bar; here is who clears it, including where
we don't." AC-5 satisfies itself. And it produces an object with a real chance
of being cited, which is CAP-36's actual objective: a conformance suite for
autograding logic simulators is a far more publishable PF-4 artifact than "our
tool vs theirs."

**B. Make the appendix a suite, and put it under the #560 harness.** One
`comparison/` tree: circuits, vectors, expected outputs, per-tool adapters,
pinned versions, one command. CI runs the JLS lane every push (byte-for-byte —
the determinism claim is then continuously proved, not asserted) and the
competitor lanes on a schedule against their current releases. When a competitor
fixes something, the lane's recorded output changes and the build tells you to
retract. That is KC-36-1 as a mechanism instead of a vow.

**C. The appendix circuits and the missing examples are the same asset.** #510's
gate 1 and gate 2 — shop window, kill the empty `JTabbedPane`, ship discoverable
examples — note that `examples/` today contains exactly one file, the three-line
stdout diff `autograde.py` that #300 exists to replace. The circuits that best
demonstrate grading determinism (a sequential circuit test vectors must drive, a
don't-care case, a NAND latch that trips oscillation heuristics) are precisely
the circuits that should appear in an Examples menu with a "run the grader"
button. Author them once; ship them in the examples gallery, the conformance
suite, and the note's appendix. That converts a documentation feature into
partial payment on the on-ramp gate that #510 says currently gates everything.

**Sequencing.** Grading note/matrix now, on the back of #300's verdict slice.
Timing evidence held until #504's chronogram slice lands, and then written as
"timing you can diff" — VCD export, golden-pinned, third-party-openable — which
is a determinism claim wearing timing clothes and is the version JLS can actually
win today.

## One caution on the larger arc

CAP-36 is writing-heavy and engineering-light, which makes it tempting to run
ahead of the shop window. Do not let it. An instructor who reads a persuasive
note, downloads JLS, and lands on an empty tabbed pane with no example circuits
has been converted into a *documented* bounce. The note's value is bounded above
by what happens in the ten minutes after it works.
