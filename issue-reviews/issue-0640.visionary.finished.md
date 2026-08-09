# Issue #640: TASK-C598-2: an un-exportable element and an absent tool are both caught before the cable is touched — each named with its role, its source and nothing left on disk
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the acceptance criteria away and the goal is one sentence: **before a
student plugs in the board, JLS should be able to say whether this design, on
this machine, can reach hardware — and if not, exactly what to fix.** That is
a readiness *predicate over (circuit, board, bindings, environment)*. #640
instead specifies two unrelated error paths that happen to fire early, and
asks them to agree on wording. The end is right; the route is wrong, and the
route is what gets built.

## What the repository already decided (and the issue never cites)

1. **JLS already has this taxonomy pattern, and it is the recorded contract.**
   `LoadError` (#58, `src/jls/LoadError.java`, ARCHITECTURE.md "Error-reporting
   contracts") is a fixed category taxonomy with location, detail and an
   actionable hint, published once so "every front end shows the same message."
   #640 AC-5 ("the diagnostics join the same vocabulary … renderable by one
   surface with no paraphrasing") is asking for `LoadError` and calling it a
   wording convention. `TellUser` (#81) plus `NotificationRatchetTest` is the
   funnel that already makes "two surfaces, one text" mechanically enforceable.
   A typed refusal record renders through `TellUser`; a semicolon-joined
   `HdlExportException` message plus a bash bullet list cannot.
2. **"Four refusal classes" is already false at HEAD.** `PcfEmitter.emit`
   (`src/jls/hdl/board/PcfEmitter.java`) emits at least five distinct
   diagnostics today: unbound port bit, unknown board pin, one pin claimed
   twice, wrong scalar/indexed form, and a key naming no port at all. A closed
   enumeration of four classes is obsolete before the task starts. A taxonomy
   with a totality test is the shape that survives — exactly the argument #492
   makes for `HdlExporter`'s buckets, in the same subsystem, in the same month.
3. **`ToolLocator` already exists** — `test/jls/hdl/ToolLocator.java` — a
   cross-platform PATH probe with `PATHEXT` handling, written (#111) precisely
   because five test classes had each grown their own copy. #640 proposes to
   grow a sixth, in bash.

## The load-bearing objection: the toolchain half is on the wrong side of the boundary

`docs/icestick-bitstream-handoff.md:31-35` describes a preflight that lives in
`scripts/icestick-handoff.sh` — a bash script. Extending it means:

- **Windows and macOS students cannot run it.** README ships an MSI and a DMG;
  the board flow's preflight would be reachable only from a POSIX shell.
- **The GUI cannot surface it.** #643 requires the dialog to show *the text the
  headless path reports*, with no paraphrasing. If the headless path is a shell
  script, #643 must either spawn bash from Swing or paraphrase — and paraphrase
  is what AC-5 forbids. #640 as written hands #643 an unsatisfiable constraint.
- **It duplicates knowledge that already exists in three places** and will need
  a fourth: the prerequisites table in `docs/icestick-bitstream-handoff.md:18-25`,
  the `need` calls in `scripts/icestick-handoff.sh:112-118`, the assume-skip
  probes in `test/jls/hdl/` via `ToolLocator`, and soon #61 (Yosys import) and
  #63 (GHDL/Icarus) which sit on the same subprocess boundary the #222 decision
  ratified.

`Boards` is the model to copy: ARCHITECTURE-adjacent code comments call a board
"deliberately just data — (name, format, pin map) — so adding a board is adding
a table entry, never new code." A toolchain is the same shape: (tool, role,
where to get it, which stage needs it, optional-unless-`--flash`). Put that
table in `src/jls/hdl/board/` (or `jls.tool`) next to `Boards`, promote
`ToolLocator` out of `test/`, and the preflight is ~40 lines of Java that serves
the CLI, the future GUI, the shell wrapper (`jls --check-toolchain`, or a test
asserting the script's `need` lines match the table), the golden-test skips, and
CI diagnostics — one source of truth, cross-platform, unit-testable headlessly.
This does not violate #215 H2 ("delegate, do not reimplement"): knowing that
`yosys` exists and what it is for is data, not synthesis.

## Half of #640 is already done

`scripts/icestick-handoff-selftest.sh` cases (a), (b) and (d) already assert
every missing tool named with role and source in one pass, before any stage
runs, with the programmer required only under `--flash`. That is AC-2 and AC-4,
implemented and CI-guarded. A task whose criteria are half satisfied at HEAD
will be "completed" by writing assertions for existing behavior. The genuinely
new work is: (i) carrying #492's rejection *reason* to the user, (ii) proving
nothing lands on disk, (iii) unifying the vocabulary — and (i) depends on #492,
which is open and **not in `ordering_after`**. As written, AC-1 asserts on text
that does not exist in the tree.

## The reframing that dissolves AC-3

AC-3 asks a test to prove four absences: no export, no constraint emission, no
toolchain invocation, nothing written. Proving absence is the weakest kind of
test — it passes for free until someone adds a write, and then it depends on the
test having anticipated the path.

Make readiness a **verb with no writer**:

```
jls -board icestick -pins pins.txt -check design.jls
```

A `BoardReadiness.check(...)` returning a `Report` of typed `Refusal` records —
pure function, no `Files.write` reachable from it, no `ProcessBuilder` beyond
PATH probing. Then "nothing is written on refusal" is a property of the call
graph, not of a test's vigilance; the export path becomes `if (report.isClean())
{ write }`, mirroring the plan-then-commit discipline `PcfEmitter` (returns
text, never writes) and `FileAbstractor` (temp-then-rename) already use.

And it is *better pedagogy than the error path it replaces*. A refusal answers a
question the student already asked; a readiness report is runnable **before**
the lab: which ports are bound, which are not, which elements the exporter
refuses and why, which tools are present with versions. It converts a wall into
a checklist, gives instructors a one-line lab-setup verification, and — rendered
as machine-readable output under the `docs/batch-interface.md` stability
contract — gives autograders something to consume. Capstone #522 is served far
better by "the student can check readiness on their own laptop" than by four
well-worded exceptions.

## Concrete recut of FEAT-C38-2

I am explicitly disregarding #640's AC-2/AC-4 framing ("extend the shell
preflight") and its pairing of the element class with the tool class:

- **TASK-A — design refusals become one taxonomy.** Merge #638 and #640's
  element half. All design-level refusals (unassigned port, wrong direction,
  unknown pin, double-bound pin, malformed binding form, un-exportable element)
  become `Refusal(category, subject, location, reason, fix)` — `LoadError`'s
  shape — aggregated, stably ordered, with a totality test over the categories
  in the spirit of #492. Blocked by #492 for the element reasons; declare it.
- **TASK-B — environment readiness becomes Java.** `ToolLocator` to `src/`, a
  `Toolchain` data table beside `Boards`, one probe. The shell script keeps its
  selftest but consumes the table instead of restating it. Serves #61, #63,
  #111, CI.
- **TASK-C (#643) — render.** `Refusal` → `TellUser` for the GUI, → one
  `jls: error:` line per the #42 CLI contract headlessly, → structured output
  for graders. "One vocabulary, two surfaces" becomes a ratchet test that no
  refusal is constructed outside the taxonomy, not a string comparison between
  a Java message and a bash bullet.

## What I would keep unchanged

The all-or-nothing instinct, the insistence on asserting specific text rather
than the fact of failure, the `--flash` distinction (a student without hardware
still gets a bitstream), and naming Logisim-Evolution's silent-toolchain failure
as the anti-pattern. Those are the right values. They deserve a data structure,
not four hand-written strings across two languages.
