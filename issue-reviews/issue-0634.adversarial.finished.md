# Issue #634: TASK-C597-2: pins are assigned in a dialog the board definition itself validates — an unknown pin, a wrong direction or a double-bound pin is refused at entry, not at export
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is a task under FEAT-C38-1 (#597), which puts a File-menu pin-assignment
dialog over the already-landed `jls.hdl.board` substrate (`Board`, `Boards`,
`PinBindings`, `PcfEmitter`, all present and unchanged since #264/#213). The
"surface, not a second implementation" framing and the byte-identical-output
discipline are sound engineering instincts. But acceptance criterion 1's
headline claim — that the dialog refuses "a binding in the wrong direction" at
entry, using "the board definition as the authority" — describes a validation
that does not exist anywhere in the codebase the issue points to, and cannot
be derived from it without adding new state and logic the issue never
mentions. A second criterion (round-trip through a `-pins` file) quietly
assumes a serializer that also does not exist. Both gaps make it possible to
"complete" this task while technically satisfying the letter of the ACs.

## Findings

### 1. [Critical] AC-1's "wrong direction" check has no basis in the board model or the emitter, and is untestable as literally stated

`Board` (`src/jls/hdl/board/Board.java:26-27`) is `record Board(String name,
String fpga, Format format, Map<String, String> pins)` — the pin map is
`pin name -> physical location string`. No pin carries a direction, a
capability tag, or any electrical constraint. `Boards.ICESTICK`
(`src/jls/hdl/board/Boards.java:34-78`) confirms this: `CLK`, `LED1..5`,
`UART_RX`, `UART_TX`, and every Pmod/header pin are all just `name -> location`.

`PcfEmitter.emit` (`src/jls/hdl/board/PcfEmitter.java:58-119`) is the complete
validation surface the issue calls "the board definition itself" and demands
the dialog reuse (AC-4). Its error list is exhaustive in the code: unbound
port bit (line 82-86), pin not present on the board (88-94), pin claimed by
two keys (95-101), and unclaimed/malformed binding keys diagnosed by
`diagnoseLeftover` (111-177: wrong scalar/indexed form, out-of-range bit,
unknown port name). There is no direction comparison anywhere in this method,
and `test/jls/hdl/board/UnbindablePortsTest.java`'s test names (missing
binding, unknown pin, non-existent port, indexed-on-scalar, unindexed-on-wide,
out-of-range index, one-pin-two-ports, malformed line, duplicate key) confirm
the same: **no test, and no code path, treats direction as bindable-or-not**.

This is not local drift — the identical sentence ("unknown pin, wrong
direction, double-bound pin refused at entry") appears verbatim in the parent
feature #597's AC-2, so the false premise is inherited, not introduced here.

Consequence for acceptance: as written, AC-1 can be satisfied vacuously —
implement checks for unknown-pin and double-bound (both real, both present in
`PcfEmitter`), skip "wrong direction" because there is nothing to check
against, and no test can prove the omission because no fixture can construct
a "wrong-direction" binding under the current `Board` schema. Alternatively, an
implementer who takes the AC literally must invent a per-pin direction
attribute (schema change to `Board`/`Boards`) and a new comparison — logic
that does not exist in `jls.hdl.board` today — which then collides with AC-4
("no validation logic is duplicated out of `jls.hdl.board`... the dialog
calls the existing `Board`/`PinBindings` code paths"): the *existing* code
paths do not do this, so satisfying AC-1 truthfully means either extending
`jls.hdl.board` (undisclosed scope, needs its own design: is a board's UART_RX
input-only? are LEDs output-only? is that even true for a raw FPGA pin, which
is direction-agnostic until the HDL declares it?) or implementing the check
GUI-side (violating AC-4's letter). The issue resolves neither path.

**Recommendation:** Before implementation, either (a) strike "wrong direction"
from AC-1 and file a separate design issue for board-side pin-capability
metadata if it's wanted, or (b) add the missing piece explicitly to this
issue's scope: a `Board`/`Boards` schema change adding per-pin allowed
direction(s), a `PcfEmitter`/new validator check for it, golden/unit tests
proving a real wrong-direction rejection, and an explicit statement of which
iCEstick pins are direction-restricted (none are, physically — a raw FPGA
ball has no fixed direction until the design drives or reads it — so this may
not even be a coherent hardware concept for this board's pins, only for
board-level function like a button vs. an LED).

### 2. [High] AC-3's round-trip claim assumes a `-pins` serializer that does not exist, and "unchanged" is not defined precisely enough to test

`PinBindings` (`src/jls/hdl/board/PinBindings.java`) has exactly one public
entry point besides the constructor: `static PinBindings parse(List<String>
lines)` (line 52) and a read-only `asMap()` (line 94). There is no method
that renders a `PinBindings` back to `-pins` file text anywhere in
`src/jls/hdl/board/` or `src/jls/JLSStart.java` (confirmed by grep for
`writePins`/`toPinsFile`/`emit.*[Pp]ins`across `src/`). AC-3 ("The bindings
the dialog produces are expressible as a `-pins` file and round-trip through
it") therefore requires building a brand-new serializer that is not part of
"the existing `Board`/`PinBindings` code paths" the Outcome section says the
dialog merely surfaces — this is new production code in `jls.hdl.board`,
unacknowledged as scope, with its own format questions (comment preservation?
blank-line preservation? key ordering when bindings originate purely from
GUI clicks and never had a "file order" to begin with?).

Separately, `parse` (lines 58-67) strips `#` comments and blank lines before
building the map, so for any `-pins` file that contains either, "a file
loaded into the dialog and re-emitted is unchanged" cannot mean byte-identical
text — only semantically-equivalent bindings. The AC does not say which is
meant. A byte-identical reading makes the criterion unsatisfiable for
realistic input files; a semantic reading makes it nearly free (map
equality) and worth stating as such so nobody burns time chasing literal
byte parity.

**Recommendation:** Specify the new serializer as explicit scope (its own
line item, its own tests, ideally sharing a canonical line format with
`PinBindings.parse`'s grammar), and reword AC-3 to "the loaded and re-emitted
binding *map* is unchanged" (semantic) unless byte-identical text on
comment-free, canonically-formatted files is really what's wanted — in which
case say that.

### 3. [Medium] AC-1's three named failure cases omit the one `PcfEmitter` actually exercises most in tests: an unbound (missing) port

`CliBoardExportTest.anUnbindablePortWritesNothingAtAll`
(`test/jls/hdl/board/CliBoardExportTest.java:108-126`) and
`PcfEmitter.emit` line 82-86 show that the single most common real failure —
a port the user simply never got to — is a first-class, heavily-tested error
case in the existing substrate, yet AC-1 lists only "unknown pin, wrong
direction, pin already bound" as the things refused at entry. It's a
reasonable inference that a dialog also can't be committed with unbound
ports, but the issue never says so, and "cannot be committed in an invalid
state" is vague enough that an implementer could read it as "the three named
cases only" and ship a dialog that lets you commit with ports silently
unbound (which then fails at export after all — exactly the outcome the
issue's title says this task eliminates). Make the missing-binding case
explicit in AC-1 rather than leaving it to be inferred from a different test
file.

### 4. [Low] Sequencing: the dialog this task builds has no landed place to be opened from

`ordering_after: [264, "TASK-C597-1"]` is honored in spirit — #264's
constraint/emission substrate is landed at `29afb26` per #264's own table, so
that dependency is real and satisfied. But TASK-C597-1 (#632, the File-menu
entry + board picker this dialog would presumably be invoked from) is open
and unimplemented, and so is #288 (the more general "Export HDL…" File-menu
seam #597 says this feature extends). None of this blocks writing the dialog
class and its headless (Layer 1) tests, but AC-5's "drivable headlessly by
the #91 harness" implies at least a Layer-2/3 (Xvfb) integration test driving
the dialog through a real menu path — that test has nothing to hang off until
#632 lands. Low severity because the repo's own convention (#264: "ordering
is convention... not necessity") tolerates parallel work; flagging so the PR
doesn't silently defer AC-5's harness-drivability check to "later" without
saying so.

### 5. [Low] Cosmetic: the machine block's `band_mw` value is HTML-entity-escaped in the raw issue body

The fenced yaml block renders `band_mw: &#34;1-1.5&#34;` (visible in the raw
API body as `&#34;`) instead of `band_mw: "1-1.5"` — a double-encoding
artifact of whatever generated this batch of task issues. Harmless once
GitHub renders it, but worth a note if the same pipeline is regenerating
issues in bulk, since it suggests the yaml block isn't being round-tripped
through a real yaml serializer.

## What's solid

- AC-2 (byte-identical GUI/CLI constraint files) is a strong, testable, non-gameable
  criterion precisely because `PcfEmitter.emit` is a pure function of
  `(HdlModel, Board, PinBindings)` — the natural implementation (dialog builds
  a `PinBindings` and calls the same `emit`) satisfies it by construction, not
  by coincidence.
- The "board is a value, not code" boundary (#264) and this task's promise not
  to touch `jls.hdl.board`'s validation shape (aside from Finding 1's gap) are
  consistent with the recorded architecture.
- AC-5's component-naming/keyboard-reachability requirement matches the
  existing, working `docs/component-naming.md` convention and `ElementFormDialog`
  pattern — no new mechanism needed, just an application of it.
