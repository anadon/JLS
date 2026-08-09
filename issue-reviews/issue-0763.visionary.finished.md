# Issue #763: TASK-C577-2: a CI lane loads, simulates and grades every CSE 260M fixture on every change — and a circuit that grades differently than on the origin fork is a named finding, never a quiet deletion
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its CI vocabulary, #763 is not a testing task. It is the
project's first **migration-fidelity instrument**: the artifact that lets
this fork tell Dr. Siever, in numbers rather than adjectives, "your
course's circuits behave here exactly as they behave on bsiever/JLS, and
where they don't, here is the list and why." #509 calls the corpus item
2 of "well enough matured" and says it "should come first"; #577's
round-2 ordering correction makes AC-1/AC-2 the entry point of the whole
WashU track. That is the point of leverage. The CI lane is the delivery
mechanism, and the issue has mistaken the mechanism for the outcome — it
spends all four acceptance criteria on lane mechanics and none on the
report an instructor would read.

That framing error is what produces the three design problems below, and
correcting it dissolves all three.

## The trajectory this lands in

- `test/fixtures/` today holds three files and an empty `legacy-4.1/`
  whose README is a deferral note. This corpus would be the largest
  fixture set the repo has ever carried, arriving before #378 (TASK-0016)
  has written the fixture-size cap it is meant to be adjudicated by.
- The repo's established idiom for exactly this problem is not a
  workflow: it is a **ratchet test read from the source tree** —
  `HeadlessCoreRatchetTest` (shrinking BASELINE), `NotificationRatchetTest`,
  `PackageInfoRatchetTest`, `SocketConfinementRatchetTest`,
  `HelpTopicsTest`'s link checker, and #378's own proposed
  `FixturePolicyRatchetTest`. Nine ratchets, zero of them a bespoke lane.
- The repo's established oracle idiom is a golden: `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `VcdExportGoldenTest`, `RiscvCpuGoldenTest`.
- `docs/grand-architecture.md` names the batch surface as one of two
  co-equal front ends and #77 (`jls.core`) as the keystone. A corpus that
  must load and simulate headlessly on every change is a genuine forcing
  function for that keystone, which is a real argument *for* this work.

So the goal is aligned. The instrument is not.

## Reframing 1 — there should be no new lane; this is a ratchet in `mvn verify`

I am explicitly disregarding AC-4. A separate workflow is the *weakest*
possible answer to the issue's own headline anxiety. A lane keyed on
paths, living in its own file, can be deleted in the same pull request
that deletes the fixtures — and then nothing is red, because the thing
that would have gone red no longer exists. The issue guards against a
quiet fixture deletion while leaving the guard itself quietly deletable.

Put the census where the project already puts its anti-vacuity: a JUnit
ratchet in the required suite, `test/jls/CorpusCensusRatchetTest.java`,
with a named constant per corpus and the standard grow-only comment
("never lower this number without a disposition line naming the finding
and the issue"). Then:

- it runs on Linux, Windows and macOS lanes that already exist and are
  already required;
- a contributor reproduces the failure locally with one `mvn test -Dtest=…`
  rather than by pushing and waiting for a workflow;
- AC-4's dependency on the unwritten CI lane-budget policy (#378, still
  open, still holding four blocking open questions) evaporates, which
  matters because #577's ordering correction declares AC-1/AC-2 *ready
  now* — and a lane-budget dependency would silently re-block them;
- the guard and the thing guarded live in the same commit-visible suite.

Cost: fixture loads and short batch runs are milliseconds each. A corpus
of a few dozen circuits is not a lane's worth of work; it is a test
class. If the corpus later grows past the required lane's budget, #378
already supplies `@Tag("longrun")` — but that is a decision to make from
a measurement, not in advance.

## Reframing 2 — "grades differently than on the origin fork" wants a recorded witness, not a live fork

Read literally, AC-2 requires CI to build and run a third-party fork of
JLS on every change to compare against. That is a foreign build, a
pinned commit, a second toolchain, and — worse — an oracle that moves
under you whenever bsiever pushes.

The elegant version is to capture the origin fork's behaviour **once**,
at corpus-import time (which is TASK-C577-1's work, not this issue's),
as a committed witness beside each fixture:

```
test/fixtures/cse260m/<lab>.jls
test/fixtures/cse260m/<lab>.witness   # stdout, exit status, VCD digest,
                                      # + provenance: fork commit, JDK, date
```

CI then compares against the witness — an ordinary golden, exactly the
shape of `VcdExportGoldenTest`. This is strictly better than a live
differential: divergences become attributable to a stated baseline; the
witness file is the natural home for a disposition ("diverges; this fork
is correct per `docs/simulation-semantics.md` §N; finding F-03"), so
AC-2's "named finding" becomes **data in the tree** rather than prose in
an issue thread that no future reader will find; and the fixture, the
baseline and the finding are one reviewable unit that cannot be
half-deleted.

## Reframing 3 — the oracle should be VCD-digest equivalence, not "grading"

The issue says "grades" three times, and JLS cannot grade. Per
`docs/batch-interface.md` §1–3, batch mode prints watched-element values;
there is no expected-value language, no verdict, and no exit status
meaning "the run completed and the answer was wrong" — that absence is
the entire thesis of CAP-06 (#300). The sibling lab-grading task #744 is
correctly `ordering_after: [300]`; #763 is not ordered on #300 at all
while using the same verb. Worse, course lab circuits do not generally
ship `-t` vectors in JLS's grammar — #509 item 5 records that CSE 260M's
checking machinery is external and unexamined. As written, AC-1 is
either blocked on #300 or blocked on authoring vectors for someone
else's labs.

The way out is to notice that grading is the wrong oracle anyway. What
this corpus needs to prove is *behavioural identity*, and the repo
already ships a documented, byte-stable, whole-run observable: `-vcd`,
an IEEE 1364 §18 waveform under a stability contract. Hash a
canonicalised VCD per fixture and the oracle becomes strictly stronger
than any grade — it compares every probe across all of simulated time,
not one settled line — while depending on nothing that does not exist
today. Decompose AC-1 honestly:

| Layer | Oracle | Available today? |
|---|---|---|
| loads | no `LoadError`; element/net census matches witness | yes |
| simulates | terminates under `-d`, no error, settles | yes |
| behaves identically | canonicalised VCD digest vs. witness | yes |
| *grades* | verdict + counterexample | no — #300 |

Land the first three now; let the fourth arrive as a widening of the
same manifest when #300 does.

## Reframing 4 — one corpus harness, not one lane per corpus

#763, #744 (Donzellini labs green/planted-defect red), #578's kit
validator, the deferred `legacy-4.1/` corpus and the `riscv/` fixtures
are five instances of one mechanism: *walk a directory of circuits,
replay each against a recorded expectation, report a table*. If #763
ships a CSE-260M-shaped lane, #744 will ship a Donzellini-shaped one and
the third will copy whichever it found first.

Build the general thing here, because here is where it is first needed:
a `corpus.toml`-style manifest per corpus directory (census, per-fixture
provenance, licence basis, expected status, disposition for known
divergence) and one `CorpusConformanceTest` that walks every manifest it
finds. #763 then reduces to "add a directory and a manifest", #744 to
the same, and the empty `legacy-4.1/` corpus finally has a shape to land
into if it is ever acquired. The project has done this before — the
`ElementRegistry` and `ExtensionPointCatalogTest` are the same instinct
applied to elements.

## The deliverable the issue omits, and should own

A green check persuades no instructor. The manifest makes a one-page
generated report nearly free — *N circuits, N loaded, N waveform-identical,
k divergences, each named with its disposition* — and **that** is the
artifact that discharges #509's AC-3 and carries the "well enough
matured" conversation. Regenerate it into `docs/` and pin it with a test
the way the project already pins `docs/batch-interface.md` with goldens.
I would make it AC-1 and demote the lane mechanics to an implementation
note.

## Smaller alignments worth taking

- **Store the corpus as plain-text saves.** `-savetext` exists and the
  README already recommends plain text for version control. A quiet
  deletion or mutation of a fixture then shows up as a readable diff
  rather than an opaque XZ blob — an anti-vacuity gain at zero cost, and
  it keeps the corpus well under whatever cap #378 settles on.
  Keep `.gitattributes`' `*.jls -text` so EOLs stay exact.
- **Declare the corpus's byte budget in this issue**, since #378 has not
  yet chosen a cap and this is the first artifact large enough to force
  the question.
- **The licence-ground disposition is the same machinery as the
  load-ground one** (#577 comment §4). One disposition vocabulary in the
  manifest covers "omitted: redistribution unclear" and "diverges:
  finding F-03" alike.

## Verdict

**endorse-with-reframing.** The outcome — an external-baseline
compatibility proof that cannot go green by shrinking — is right, is item
zero of the project's highest-leverage track, and pulls with the
headless-core keystone rather than against it. Three of the four
acceptance criteria should be rewritten before execution: no new lane
(census as a ratchet in the required suite, AC-4 struck), no live-fork
differential (recorded witnesses committed with the fixtures), and no
"grading" (VCD-digest equivalence now, verdicts when #300 lands) — with
the generated fidelity report promoted to the issue's primary artifact
and the harness written once for every corpus the tracker already
implies.
