# Issue #634: TASK-C597-2: pins are assigned in a dialog the board definition itself validates — an unknown pin, a wrong direction or a double-bound pin is refused at entry, not at export
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "a dialog". The end is: **the legality of a port-to-pin binding
becomes a thing JLS knows, rather than a thing JLS discovers while writing a
file.** Everything the issue asks for — entry-time refusal, GUI/CLI agreement,
no duplicated logic — is downstream of that one property. Judged against the
project's arc this is squarely aligned: #264 established "board = data, adding a
board is a table entry"; #223 established typed seams over ad-hoc ones; #598
wants one diagnostic vocabulary across two surfaces. A validation *model* is the
natural next member of that family. A Swing form is not.

The trouble is that the issue names an authority that does not exist yet, and
then forbids creating it.

## The blocking finding: `Board` cannot validate direction, and nothing validates it

Read the substrate as it actually is at head:

- `src/jls/hdl/board/Board.java:26` — `record Board(String name, String fpga,
  Format format, Map<String, String> pins)`. The pin map is **name → physical
  location**. There is no direction, no capability, no "LED1 is output-only".
  That knowledge exists in `Boards.java` **only as source comments** ("12 MHz
  oscillator", "LEDs D1..D4", "FTDI channel B UART").
- `src/jls/hdl/board/PinBindings.java:52` — `parse` checks *file shape only*
  (two tokens, no key bound twice); its own javadoc says matching against real
  ports and real pins "is `PcfEmitter`'s job".
- `src/jls/hdl/board/PcfEmitter.java:60-120` — the only validator in the tree.
  It catches unknown pin (`board.pins().get(pin) == null`) and pin-claimed-twice
  (`pinToKey.putIfAbsent`). **Direction is never checked**: `port.direction()`
  is read at line 74 solely to write a `# input foo <- pin LED1` provenance
  comment. `grep -n direction src/jls/hdl/board/*.java` returns five hits, all
  cosmetic.

So AC-1 and AC-4 are jointly unsatisfiable as written. AC-1 wants
wrong-direction refusal "with the board definition as the authority"; AC-4 wants
zero validation logic outside `jls.hdl.board`. There is no wrong-direction check
in `jls.hdl.board` to call, and #597's own adversarial comment (2026-08-08)
already ruled that this feature "must not add a registration API to
`jls.hdl.board` under cover of putting a GUI on it". A task author who takes all
five criteria literally has exactly one escape: write the direction rule in the
dialog. That is the drift AC-4 exists to prevent, arrived at by following the
issue.

Worse, the second-order effect is silent: today `jls -export -board icestick
-pins bad.txt` will happily bind an `OutputPin` to `CLK`. The GUI is being asked
to be stricter than the contract it is supposed to be a surface over — which
means the "same relation, checked the same way" claim in the Outcome is false
before anyone writes a line.

## Reframing A (primary): ship the checker, not the dialog

Cut the seam one layer lower. The task becomes a **headless** deliverable in
`jls.hdl.board`, and the dialog becomes a renderer of it:

1. `Board.pins` moves from `Map<String,String>` to `Map<String, Pin>` where
   `record Pin(String name, String location, Capability capability, String role)`
   and `Capability = {IN, OUT, INOUT}`. This turns the comments in `Boards.java`
   into data — the same move #264's H2 already made for locations, applied to the
   one field it missed. Adding a board is still a table entry.
2. Extract `PcfEmitter.emit`'s aggregation loop into
   `PinPlan.check(HdlModel, Board, PinBindings) -> List<Refusal>`, where
   `record Refusal(Kind kind, String subject, String message, String fix)` and
   `Kind` is exactly #598's enumeration (`UNASSIGNED_PORT`, `WRONG_DIRECTION`,
   `UNKNOWN_PIN`, `PIN_CLAIMED_TWICE`, …). `emit` becomes `check` then render —
   same aggregated all-or-nothing behaviour, same golden output, now with a
   structured result instead of a joined string.

What that buys, all at once:

- **AC-1 becomes a two-line dialog:** re-`check` on each edit, show the
  `Refusal` list, disable OK while it is non-empty. No rule is restated in GUI code.
- **AC-4 stops needing a test to be true.** "No validation logic duplicated" is
  a claim about structure; when there is exactly one `check` and the GUI has no
  branch on pin names, the assertion is trivial rather than clever.
- **#598 collapses from a feature to a rendering choice.** Its AC-4 ("GUI and
  headless report the same words, pinned by a test") is a tautology when both
  call `Refusal.message()`. Its AC-1/AC-2 land as unit tests on `check`, with no
  GUI at all. That is a genuine simplification of the sibling feature, not a
  land-grab from it — #598 already says its diagnostics "are testable headlessly
  and can land ahead of it".
- **The CLI gets better, not just wrapped.** `-pins` files start getting
  direction checking for free. A GUI task that leaves the headless path strictly
  more correct is the shape this project rewards; a GUI task that is stricter
  than its own CLI is a bug factory.

Ordering consequence: the current `ordering_after: [264, TASK-C597-1]` is wrong
in spirit. The real prerequisite is the checker, and the `Board.Pin` widening
belongs to **#264** (it owns the board data model and has an open Stage 2 that
will re-derive this anyway for ECP5/LPF). File it there, order this after it,
and this task shrinks to a form with no rules in it.

## Reframing B: one orchestration, so byte-identity is not a claim to test

AC-2 asks for a test proving two paths emit byte-identical files. Ask why two
paths exist. The board-aware export sequence is inline in
`src/jls/JLSStart.java:392-430` — read bindings, `HdlExporter.buildModel`,
`emitter.emit`, `PcfEmitter.emit`, then temp-file-and-rename at `:470`. None of
it is reachable from Swing, so the GUI must restate it, and AC-2 is a guard
against the divergence that restating causes.

Lift that block into `jls.hdl.BoardExport` — `Result(hdlText, constraintText,
warnings)`, no I/O policy, no `ProcessBuilder` (the #359 §4 no-subprocess-in-
`src/` invariant stands; the adversarial comment on #597 corrected AC-3 for
exactly this). Then `JLSStart` and the dialog are two callers of one function,
byte-identity is arithmetic rather than evidence, and the interesting test
becomes the structural one (the GUI holds no emitter call of its own) — which is
AC-4 again. Two acceptance criteria merge into one and one whole class of future
skew stops being possible. #636 inherits the same façade and stops needing its
own AC-3.

## Reframing C (the different seam): a pin binding is circuit state, not dialog state

The out-of-the-box option the issue never considers. JLS's module ports *are*
elements: `HdlExporter.buildModel` derives every port from `InputPin`/
`OutputPin` (`HdlExporter.java:260-295`), and the binding key is literally the
pin element's name. JLS already has declarative per-element persisted parameters
(`Attribute`, #52), per-element dialogs, undo via `CircuitSnapshot`, and a
`dialog.pin.*` naming prefix in `docs/component-naming.md`.

So: give `InputPin`/`OutputPin` a board-qualified `pin` attribute, assigned from
the element's own dialog or a canvas right-click, with the board recorded once
on the circuit. The `-pins` file becomes an *import/export projection* of circuit
state rather than the only place it can live.

- Direction validation becomes local and unarguable: an `OutputPin` offers only
  `OUT`/`INOUT` pins. The wrong-direction class disappears at the point of
  assignment instead of being reported there.
- **Bindings survive save/load.** The issue as written loses a 20-pin assignment
  every time the student closes the editor — a real, recurring classroom cost that
  no acceptance criterion mentions, and the single most likely reason a student
  goes back to the terminal after using the dialog once.
- Undo/redo, copy/paste and checkpointing come free through the existing
  snapshot pipeline.
- Double-binding becomes a circuit-level property the editor can even show on
  canvas, next to the pins involved.

Honest costs, stated: a `FORMAT` bump plus `docs/file-format.md`, `SaveTags` and
`AllElementsRoundTripTest` work; a decision about multi-board designs (key the
attribute by board name, or accept one target board per circuit). That is real
scope, and it is why I do not make it the primary recommendation. But it is the
architecture that makes the whole feature *stick*, and if #264 Stage 2 or #522
ever wants "reopen the circuit and re-flash it", this is the version that
already works. At minimum, record it as the recorded direction so the dialog is
built as a *view over ports* rather than a modal that owns a `Map<String,String>`
— those two designs look identical on screen and differ completely underneath.

## Reframing D: the best refusal is one that cannot be expressed

AC-1's ideal is "refused at entry". One notch better is "unrepresentable". A
table with one row per port bit and a combo box per row, populated from
`board.pins()` filtered by capability and by pins not already claimed, makes
unknown-pin, wrong-direction and double-bound **impossible to enter** rather
than refused on entry. The only remaining class is the unassigned port, which is
a completeness state (OK disabled, rows highlighted), not a refusal. Free text
survives only where it must — a `-pins` file loaded per AC-3 — and that is
precisely the path where `PinPlan.check` and #598's aggregated report earn their
keep. This also fixes an unstated regression risk: a dialog that refuses the
first bad binding is worse than the CLI it wraps, whose whole discipline is
"report the full repair job at once".

## On the decomposition

#632/#634/#636 cut one dialog into three horizontal slices: the frame, the form,
the button. #264's §2 records the lesson against exactly this — "per-board
vertical slices … the horizontal cut is exactly what let #213 and #215 drift
apart". A vertical cut of the same work: **(1) headless — `Board.Pin` capability
+ `PinPlan.check` + `BoardExport` façade, no GUI; (2) the whole dialog —
picker, table, action — as a thin view.** Task 1 is testable with no display and
is where all the risk lives; task 2 is mostly `docs/component-naming.md`
compliance (`dialog.board.*`) and #91 harness wiring, which AC-5 already gets
right and which is the least controversial part of the issue.

## Explicitly disregarded, and why

- **AC-1 as written** — the wrong-direction clause is unsatisfiable at head.
  Keep the *outcome*; make it true by giving `Board` pin capability (in #264) and
  by Reframing D, which removes most of the refusal path rather than implementing it.
- **AC-2 as written** — replace "test that the two paths agree byte-for-byte"
  with "there is one path"; keep a golden on `BoardExport` output.
- **AC-4 as written** — "asserted by a test" is the weak form. Make it
  structural: the GUI package imports `jls.hdl.board` and calls `check`/`emit`,
  and contains no pin name, no direction rule and no format string.
- **AC-3 and AC-5 stand unchanged.** Round-tripping through `-pins` keeps the
  autograder contract honest, and the naming/keyboard requirement is the #288
  standard correctly applied.
