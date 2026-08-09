# Issue #646: TASK-C563-4: truth-table extraction runs headless behind a batch flag with machine-readable output
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its four ACs, #646 makes one claim: *the analysis loop CAP-31 (#515)
buys must not be GUI-only, because the people who most need it — instructors and
autograders — never open a window.* That claim is correct and it is the same claim
that produced `-b`, `-t`, `-vcd`, `-i` and `-export`. ARCHITECTURE.md's headless
discipline (`jls.sim` imports no AWT, `HeadlessCoreRatchetTest`, issue #77) and
#641's own framing — "the table is a value, not a view" — mean the honest cost of
this task is a flag, an output writer, and a doc section. The 1 mw band is right.

So I endorse the goal. What I want reframed is *what the flag emits* and *what it
takes as an argument*, because as written both invent something JLS already has,
and both make the sibling tasks (#651, #656, #660) harder than they need to be.

## Reframing 1 — the output schema already exists; do not mint a second one

The issue asks for "a machine-readable table" and the boundary comment says
whichever of the four headless surfaces lands first "sets the output encoding".
That is framed as a free choice. It is not: **JLS has shipped a truth-table data
type for its whole life.** `jls.elem.TruthTable` (1491 lines,
`src/jls/elem/TruthTable.java`) is a simulatable element whose serialization is
already normative in `docs/file-format.md` (§element tags, `TruthTable` row:
`String input`/`output` items name columns, `pair` items carry cells in row-major
order), already round-trip tested (`AllElementsRoundTripTest`), already editable
in `src/jls/edit/TruthTableEditor.java`, and already deterministic on save (the
format spec's save-time canonicalization sorts blocks by stable id, #165).

Make the canonical artifact of `-truthtable` a `.jls` file containing one
`TruthTable` element carrying the extracted table, with the frontier signal names
as its column names. Consequences, all of them good:

- **#656 AC-3 is satisfied by construction**, not by coordination. "The table input
  format is the same one TASK-C563-4 writes, so extraction and synthesis compose on
  the command line without a converter" stops being a cross-issue promise and
  becomes a tautology — synthesis reads a circuit file, which JLS can already load.
- **The freeze obligation shrinks.** #686 freezes the batch contract; a brand-new
  table schema is new frozen surface. A `.jls` file is surface already frozen by
  `docs/file-format.md`, which has its own `FORMAT` version negotiation (#79) and
  its own conformance tests. AC-4's documentation home moves from "invent a schema
  section in `docs/batch-interface.md`" to "one flag row plus a pointer".
- **The artifact is executable, which buys a far better oracle than AC-2.** A
  `.jls` holding the extracted table can be simulated. That makes
  extracted-table-vs-original-cone an *exhaustive differential* check — the same
  oracle #651 wants for minimization and #655 wants for round-trip — rather than a
  byte comparison against a golden that only proves the writer is stable.
- **The student and the grader see literally the same object.** #644's view work
  shrinks toward "populate the existing truth-table surface", and a student can
  open the grader's artifact in JLS and drop it on a canvas.

Two details this framing must state rather than discover: `TruthTable` entries use
`2` for don't-care (`src/jls/elem/TruthTable.java`), so an extracted table must
declare that it never emits `2` (extraction produces total functions); and #641's
AC-4 multi-bit convention has to agree with the element's per-signal width model.

For graders who want a flat table, add a projection selected by the output path's
extension — `-truthtable out.csv` — following the convention `JLSStart` already
uses twice: `-i` picks PNG/JPEG/SVG by extension (`JLSStart` ~line 1038, issue
#154) and `-export` picks Verilog vs VHDL the same way (~line 781). That is an
existing idiom, not a new `--format` concept, and the CSV is a lossy view of a
canonical artifact rather than a second source of truth.

## Reframing 2 — delete the "region" argument

"A batch flag that names a circuit **and a region**" is the sentence that will cost
a week. Headless, there is no canvas selection. The only region vocabulary that
exists is stable ids (`Element.getStableId`, persisted as `sid`, `docs/file-format.md`
§8), and a grading script that must pass `sid` lists is a script that must first
parse the `.jls` it is grading. No instructor will write it.

The batch surface has never addressed anything geometrically. It addresses things
**by name**: `-t` drives *top-level input pins by name*; watched output is printed
in element-name order (`docs/batch-interface.md` §3.2); `-s` param files say
`ELEMENT <name> WATCHED true`. Follow that:

- **Default: no region argument at all.** The analyzed cone is the top-level
  circuit's combinational cone from its `InputPin`s to its `OutputPin`s. This is
  exactly the grading contract — the assignment specifies the pin names, the
  student's file must honor them — and it makes AC-2's determinism trivial, because
  the frontier is a named pin set with an obvious order.
- **Optional: a named subcircuit instance** (`-truthtable out.jls -sub adder circ.jls`),
  reusing the dotted qualifier vocabulary batch output already prints.
- **Escape hatch, only on demand:** an explicit region list belongs in the `-s`
  param file, which is the existing extensible "extra instructions" channel, not in
  new flag syntax.

This is not a scope cut; it is the same outcome reached by a route where the hard
part evaporates. It also removes AC-3's "unresolvable region" refusal class
entirely — an unknown `-sub` name is an ordinary named-lookup failure.

## Reframing 3 — the grader's real question is a verdict, not a table

A table is an intermediate. What an autograder wants is *does this circuit compute
the assigned function*, and today it must diff my table against a reference by
hand. Four new verbs are landing on one CLI (#646 extract, #651 minimize, #656
synthesize, #660 FSM) and none of them answers that question. I am **not** asking
#646 to grow an equivalence flag — that is CAP-09/#306 territory and inventing it
here would duplicate a capstone. I am asking that #646 be built so the verdict is
one short step away, which Reframing 1 delivers for free: extract to a `.jls`
table, and the comparison is circuit-vs-circuit inside machinery #306 already
needs. If only one of the four verbs ever ships, it should be the one that prints
`PASS`/`FAIL`; #646 is its cheapest enabler, and that is the strongest argument
for doing it first rather than fourth.

## Where the work strengthens the arc, and where it pulls

Strengthens: it keeps CAP-31 from becoming a GUI feature with a headless
afterthought, and it forces #641's cone → table pass to stay a value-returning
library call — the discipline that ARCHITECTURE.md's headless-core section and
#872's AC-5 both demand. Failure mode to name in the task: if the CLI path grows
*any* enumeration logic of its own, the GUI and batch tables can disagree, which is
precisely the bug the "same table the student sees" outcome exists to prevent. One
enumerator, two thin consumers, asserted by a test that runs the same fixture
through both.

Pulls: `docs/picocli-evaluation-2026-07.md` rejected picocli "for the current CLI
shape" and named the re-evaluation trigger as *"if and when the CLI grows
subcommands."* Four analysis verbs arriving at a 14-flag, 3069-line `JLSStart` is
that trigger firing, and it will fire flag-by-flag with nobody looking at it. The
boundary comment already assigns "whichever lands first sets the output encoding";
extend that mandate — whichever lands first also settles the *naming shape* of the
analysis verbs (one `-analyze <kind>` family vs. four sibling flags), once, in
writing, rather than four times by accident.

## Two acceptance criteria I would rewrite

- **AC-2 is the weakest available oracle.** Byte-determinism proves the writer is
  stable, not that the table is right. Keep it, but subordinate it: the load-bearing
  criterion is that the emitted table, simulated exhaustively, agrees with the
  original cone on every input combination. That is achievable only under
  Reframing 1, which is the main reason I argue for it.
- **AC-3 is missing #656's best clause.** #656 AC-4 requires "write no partial
  circuit file"; #646 AC-3 only forbids a partial table on stdout. A refusal
  discovered mid-write must leave *no output file*, not a truncated one.
  `FileAbstractor` already writes via temp-file-and-rename precisely for this;
  reuse it and state the criterion symmetrically with #656.

## Dependency reality worth one line

#646 declares `ordering_after: [TASK-C563-1, TASK-C563-2]`, but the component both
of those consume — the cone extractor — was only filed on 2026-08-08 as #872, four
days after this task, and #872 itself is gated on #468. The chain under #646 is
three deep and its root is days old. That does not change the verdict; it changes
what "1 mw" means in calendar terms, and an executor who picks this up expecting to
start today will find nothing to call.
