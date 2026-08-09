# Issue #686: TASK-C524-1: the headless CLI contract is written down and made executable — invocation, every exit status, artifact paths and the xUnit schema, each clause held by a conformance test
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is actually for

Strip the task down and the payload is two sentences from its capstone, #502: *step 4*
— per-student score vectors byte-identical across Gradescope, Classroom, PrairieLearn
and nbgrader — and *step 5* — a seeded contract violation reddens CI before any adapter
test runs. Everything in #686 exists to make those two observations possible. The
contract document is not the deliverable; **"four adapters cannot drift apart"** is.
That reframing decides several design questions the issue leaves open, and it disagrees
with the issue in a few places.

The instinct is right and it is squarely in the project's grain. JLS already runs a
house pattern for exactly this: a normative markdown document plus a build-failing
drift test that reads it. `test/jls/FileFormatSpecTest.java:52` reads
`docs/file-format.md`; `ExtensionPointCatalogTest` cross-checks `docs/extension-points.md`
against code constants in both directions; `docs/standards-adoption/04-tool-qualification-and-scope.md`
states the rule outright — *"in this repo, a documentation claim that is not pinned by
a drift test is not evidence."* #686 is the fourth instance of a pattern the repo has
already ratified. That is why the verdict is not `rethink`.

## Reframing 1: one document, not two

The Outcome says `docs/batch-interface.md` is *"the starting material, not the artifact."*
Read literally that mints a second normative document covering the same surface —
invocation, exit statuses, artifact paths — while `batch-interface.md` keeps its own
§1 exit table, §6 stability promise, and its own pins (`CliFlagTableTest`,
`CliSmokeTest`, `VcdExportGoldenTest`, `BatchSimulationGoldenTest`). Two normative
sources for one interface is the precise failure mode the drift-test discipline exists
to prevent, and nothing in the repo has ever done it: one surface, one normative doc
(`file-format.md`, `simulation-semantics.md`, `batch-interface.md`), one drift test.

Cheaper and strictly better: **promote `batch-interface.md` in place.** Give its
clauses stable IDs, add the missing sections (status 3, artifact paths, the report
schema, the exclusion list), and write `CliContractConformanceTest` as the drift test
that reads *that* file. Every existing citation from `src/jls/JLSStart.java:664`,
`src/jls/sim/BatchSimulator.java:374`, `ARCHITECTURE.md` and the README keeps resolving.
Nobody ever has to answer "which document wins?" — and "starting material, not artifact"
was never an architectural requirement, only a rhetorical one.

## Reframing 2: build the clause-coverage mechanism once, not for the CLI

The genuinely novel thing in #686 is not the document, it is AC-2's inversion: *a
clause with no covering test fails the suite*. That is a mechanism, and it is being
built bespoke for one document. The repo has four normative documents and four
hand-written, mutually inconsistent drift tests; each new contract re-invents the
plumbing.

Cut along that seam instead. One `NormativeClauseCoverageTest` that (a) scans every
document declared normative for clause anchors (`[C-1.3]`-style, already legal
markdown), (b) collects clause tags from tests — a `@Clause("batch:C-1.3")` annotation
or a naming convention, (c) fails on an unclaimed clause *and* on an orphan tag naming
a clause that no longer exists. The delta over writing the CLI-only version is small;
the payoff is that `file-format.md`, `simulation-semantics.md`, `extension-points.md`,
the collab op vocabulary and every future contract inherit it. Orphan-tag detection is
the half the issue omits and the half that catches the realistic failure: a clause gets
reworded, the test keeps passing against text that no longer exists.

## Reframing 3: freeze a named profile, and let a reference adapter define it

The title says *"the headless CLI contract."* The headless CLI is fourteen flags —
image export, SVG, Verilog/VHDL export, `-board`/`-pins`, `-savetext`, three printer
flags, VCD. CAP-21's four adapters need roughly three things: invoke a graded run,
read a verdict artifact, read an exit status. Freezing the whole CLI as JLS's first
formally frozen public interface buys enormous liability for that. #502 already names
the hazard as KC-21-4 — a freeze that blocks FEAT-053's own evolution is worse than no
freeze — and TASK-C524-2's ratchet is then asked to unwind it with semver.

Freeze less. Declare a **grading profile**: a named, versioned *subset* of existing CLI
behaviour (no new syntax, no new flags) that is the frozen thing; everything else stays
at `batch-interface.md` §6's existing, weaker "CHANGELOG + major bump or compat flag"
promise. Two consequences follow for free:

- **AC-4 stops being prose.** The issue's fourth criterion — say what is *not* contract —
  is the highest-value clause and the least testable; the adversarial comment on the
  issue is right to demand a falsifier. Under a profile it needs none: out-of-profile is
  out-of-contract *by construction*, and the exclusion list becomes a short list of
  traps inside the profile's blast radius (the `-t` parse-error stdout deviation of
  `batch-interface.md:43-49`, `displayOutcome`'s four strings).
- **The profile can be defined by a reference adapter rather than by a document.** Ship
  one in-tree `jls-grade` adapter — the thing PF-2..PF-5 are thin platform shells over
  — and the contract's extent is exactly what that adapter reads. This is the strongest
  available answer to #502 step 4: four adapters produce identical scores because there
  is only one parser of JLS output in the world, and the platform shells transform an
  already-parsed verdict record. Under the issue as written, four independently written
  adapters each parse the frozen surface and parity is asserted by a 300-submission
  corpus test after the fact.

## Reframing 4: the canonical artifact is the verdict record, not the XML

AC-3 pins the xUnit schema *"as a schema artifact, not described in prose alone."*
But #466 deliberately emits a *deviation* — no `timestamp`, `hostname` or `time`,
because those three are what break byte-determinism. So a stock JUnit XSD rejects the
very reports being frozen, and a JLS-authored XSD is a schema that describes exactly
one writer and validates nothing an in-tree golden does not already validate. This
invests precision at the wrong layer.

The load-bearing object is the verdict record `(name, when, expected, observed, passed)`
— #466 §7.5 already makes it a real type, and all four platforms transform it into
their own JSON within milliseconds of reading it. Freeze *that*: a small JLS-owned,
versioned artifact with an in-tree schema, byte-deterministic and golden-pinned. Then
declare xUnit XML a **rendering** of it, pinned by goldens, with conformance stated as
"reads correctly in the JUnit-family consumers we actually test against" and a test
that runs one or two of them. That is a claim an adapter author can rely on; "validates
against this XSD we wrote ourselves" is not.

## Alignment: the freeze is early relative to this project's own culture

`ARCHITECTURE.md`'s recorded decisions are a consistent record of *not* building
boundaries before a consumer exists: out-of-process plugin isolation "reserved for a
future untrusted-provider case, not built speculatively"; no second simulation strategy
until a real design is unusably slow; i18n declined until an instructor asks. #686
freezes an interface whose verdict half does not exist yet (`-check` is an unknown
option at HEAD; there is no `GradeReport`), for adapters that do not exist, at a project
that has never shipped a formally versioned interface. #502 itself proposes the sane
sequence — a 2–3 mw demo slice, "PF-1 draft contract + PF-2 Gradescope template over
the existing three-exit-status behavior … before the freeze."

Take it. Land the document and `CliContractConformanceTest` now — they are cheap, they
are the house pattern, and they pay off immediately as drift protection. Land the
*normative freeze* — semver, deprecation window, the ratchet of TASK-C524-2 — only
after one real adapter has been written against the contract and reported back what it
actually needed. Writing the contract and declaring it frozen are separable acts, and
this issue is quietly bundling them.

## Explicitly disregarded

I am setting aside AC-3 as written (pin an xUnit schema artifact) in favour of pinning
the verdict record and treating xUnit as a golden-pinned rendering; and setting aside
the Outcome's insistence on a new document distinct from `docs/batch-interface.md`.
Both changes serve #502's actual observations better than the literal criteria do.

## Keep

The Boundary is well drawn: consuming #466's semantics rather than choosing them is
correct, and the adversarial comment's finding that `ordering_after` should be `[466]`
alone is right — nothing here needs the SimpleEditor decomposition or a Yosys writer.
AC-2's inversion (an untested clause fails the suite, rather than being carried as
prose) is the best idea in the issue and should survive every reframing above.
