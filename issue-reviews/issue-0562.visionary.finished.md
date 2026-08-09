# Issue #562: FEAT-C29-5: a Digital circuit's embedded test cases arrive as runnable -t vector files that pass with the same verdicts Digital reports — the instructor's grading suite survives the migration
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The end is right and it is the most valuable end in CAP-29 (#513): a course
does not move because its pictures move, it moves because its *grading*
moves. PF-5 is correctly identified as "the piece that actually converts
courses." Nothing below disputes that outcome. What I dispute is the target
it points at.

## The structural fact the issue never confronts

`-t` has no verdict, and the project already knows it. `docs/batch-interface.md`
§2.2 has four productions — `file`, `signal`, `step`, `initial` — and every
terminal in them (`name`, `value`, `duration`, `time`) is an *input*. There is
no expected-value production, no comparison, no pass/fail. `src/jls/elem/SigSim.java`
posts events at parse time and compares nothing; the string "expected" appears
in it three times, all inside parser error messages (lines 137, 163, 167).
#369 states this in its own words and pins it at `2d0ca9d`: *"the verdict does
not exist yet."*

So AC-1 — "its translated vectors pass under `-t` with the same verdicts
Digital reports" — is not merely hard, it is not expressible. A `-t` file
cannot pass or fail. It can only be replayed.

Follow that through and the issue's own acceptance criteria turn against its
outcome. Digital's test-case element holds a *program*, not a stimulus table:
input **and expected-output** columns, don't-care `x`, a `C` clock-pulse
column, `let` / `loop` / `repeat` / `bits`
(`docs/capability-roadmap/sweep-04-verification.md:243-249`, flagged there as
recalled-not-verified). Translating that into `-t` can carry only the input
columns. Every expected-output column becomes a named loss under AC-2. The
feature would then ship, honestly and totally reported, having migrated the
stimulus and dropped the grading suite — satisfying AC-1 through AC-4 while
falsifying the sentence in the title. A specification whose criteria can all
be met by a build that defeats its own outcome is pointing at the wrong
artifact.

## The dependency that is missing from `ordering_after`

`ordering_after` names FEAT-C29-2 (#558) and FEAT-C29-1 (#556). The real hard
gate is #369 / TASK-0111 — the expectations channel, the PASS/FAIL/UNRUN
lattice, the report schema — and it is absent. The dedup comment on this issue
draws exactly the right boundary (#562 produces, #369 owns the verdict) and
then does not draw the ordering edge that boundary implies. This is not
bookkeeping: #369 is banded 9-15 mw and `blocked_by: [316, 321, 347]`, none of
whose tasks are filed. A 2-3 mw translator whose value is entirely gated on an
unstarted 9-15 mw feature is not a 2-3 mw item on any schedule a maintainer
would recognize.

## Where the trajectory actually points

The project has already decided `-t` is the surface to leave, not the surface
to arrive at. `sweep-04-verification.md:245-248` says of Logisim-evolution's
CSV vectors: *"which is what JLS already has in `-t`, and which is exactly the
surface this change is meant to move beyond."* `docs/batch-interface.md` §6
freezes the `-t` grammar as a stability contract; #369 invariant 1 makes it
"literally untouched"; #214's definition of done forbids touching it. #562
proposes to build the flagship migration bridge onto that frozen, deliberately
superseded surface. That is the pull against the arc.

## Reframing 1 (the one I would actually take): read, don't emit

Do not write a source-to-source translator. Write a **reader of Digital's test
program that drives the JLS runner directly**, against #369's expectations
channel, with no emitted intermediate artifact at all.

What evaporates:

- **AC-3 disappears entirely.** Byte-determinism of generated vector files and
  "never churns an instructor's repository" are problems created solely by
  choosing to emit a file. With no emitted file there is one source of truth —
  the instructor's original `.dig` — instead of a checked-in generated artifact
  shadowing it. **I am explicitly disregarding AC-3**, not because it is wrong
  but because the correct design makes it meaningless.
- **The unrolling losses disappear.** `loop` / `repeat` / `let` / `bits` need
  never be expanded into flat vectors; they are interpreted. A construct that
  today would be a "loss" because `-t` cannot express iteration is simply
  executed.
- **The clock-column problem disappears.** Digital tests are cycle-based; `-t`
  is absolute-time (`for d` / `until t`, §2.3). An emitter must invent a clock
  period and hope the #558 importer exposed Digital's clock as a top-level
  `InputPin` — `-t` names must match *top-level* input pins exactly (§2.2), and
  a circuit clocked by a JLS `Clock` element has no such pin to drive. A reader
  can post clock edges against the circuit it is running.
- **AC-4 gets stronger evidence.** Verdict parity is asserted over the *same
  bytes* run in both tools, with no translated intermediate to blame for a
  disagreement.

The emitter is not lost, it is deferred and improved: once the expectations
format exists and the reader works, emitting to it is a projection of a
validated in-memory model rather than an unvalidated source-to-source pass.

## Reframing 2: make the `.dig` corpus the requirements input to #369's schema

This is the highest-leverage move available and it is nearly free. #369's Open
Question 1 — does a separate expectations file behind a new flag satisfy #214 —
is asked with *no external requirements pressure at all*; the schema would
otherwise be designed against JLS's own imagination. A corpus of real
instructor `.dig` test sections is precisely the missing forcing function.
Invert the stated ordering: rather than #562 consuming a format designed
without it, make "a real Digital test corpus translates into this schema with
principled, not accidental, losses" a gate on freezing the schema. #369's own
re-planning protocol warns that the report schema becomes a compatibility
surface "the moment a script parses it" — a schema frozen before anyone checked
it against the one foreign test language JLS has committed to importing is a
compatibility break waiting to be discovered by an instructor.

## Reframing 3: the interim measurement that de-risks both

While #369 is unbuilt, the parity claim is still checkable *outside* JLS today.
Translate the input columns only, run batch with `-vcd` (deterministic, golden-
pinned, §4), and compare observed values against Digital's expected columns with
a small bridge — the pattern `docs/vcd-interop.md` already documents. That
produces, in a fraction of the band, the number nobody has: *what fraction of a
real Digital test corpus would pass in JLS at all.* Under KC-29-1 that number is
worth more than the translator, because it is what tells a maintainer whether
the verdict channel is worth 9-15 mw. This is the CAP-29 `demo_slice` discipline
applied to PF-5.

## What I would keep verbatim

AC-2 is the best sentence in the issue and survives every reframing: every
untranslatable construct is a named, located, explained loss in #556's shared
contract, never a silently thinner run. It is the same discipline as FEAT-025's
report-totality equality ($C_{src} \setminus C_{out} = R$) and it belongs here.
AC-4's "mechanically, not by visual inspection" likewise.

## Verdict

**redirect.** The outcome — an instructor's grading suite survives the move —
is endorsed without reservation and is correctly identified as the conversion
lever. The mechanism is aimed at a frozen surface that cannot hold a verdict,
under an ordering that omits its only real prerequisite. Re-target it: a `.dig`
test *executor* over #369's verdict channel, with the `.dig` corpus promoted
from downstream consumer to schema-design oracle, and an external VCD bridge as
the interim measurement. Drop AC-3; re-word AC-1 to name #369 as its gate.
