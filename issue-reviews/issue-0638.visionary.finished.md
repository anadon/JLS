# Issue #638: TASK-C598-1: an unassigned port and a wrong-direction binding each name themselves and their fix — and a design with several reports all of them in one failure
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the five acceptance criteria away and the ask is: *nobody should discover a pin
mistake by watching a board not blink.* That serves CAP-38 (#522) exactly — the classroom
outcome is "draw, click, board does it," and the named anti-pattern is
Logisim-Evolution's silent toolchain failure. The intent is right and belongs on JLS's arc.

But the task as scoped delivers much less than it appears to, and the two criteria that
carry the real weight (AC2, AC5) are aimed at the wrong seam.

## Three of the five criteria are already met at HEAD

- AC1 (unassigned port names the port and the fix): `PcfEmitter.java:82-86` emits
  `port "led[1]" (...) has no pin binding; add a line "led[1] <pin>"`, and
  `UnbindablePortsTest.aMissingBindingNamesThePortAndTheRepair` already asserts on that text.
- AC3 (aggregation in stable order): `PcfEmitter.java:69,111-119` collects every offender;
  the order is already deterministic (model port order, then binding-file order). That is a
  *documentation* task, not an implementation one.
- AC4 (nothing on disk): `JLSStart.java` computes `constraintText` at ~427 and writes at ~470;
  the throw precedes every write. Enforced by construction.

So the net-new work is AC2 and AC5. The issue's "Outcome" prose ("closed headlessly so they
can land ahead of any GUI") reads as if all four were new. A reviewer sizing this at
0.5-1 mw is sizing the wrong thing: AC2 is not a diagnostic, it is **new board data**, and
AC5 is not wording discipline, it is **a missing type**.

## Reframe 1 — AC5 cannot be met with prose; JLS already solved this once

AC5 asks that diagnostics "live in one vocabulary that a second surface can render verbatim."
Today the board flow has three independent refusal sites that each do
`String.join("; ", errors)` into a flat English blob:

- `HdlExporter.java:194-196` (unexportable elements)
- `PcfEmitter.java:115-119` (unbindable ports)
- `PinBindings.java:81-84` (malformed binding lines)

...plus a fourth, `scripts/icestick-handoff.sh`'s missing-tool preflight, which is bash and
shares no vocabulary with any of them. #598 covers all four as one layer. A GUI dialog
(TASK-C598-3) rendering `e.getMessage()` "verbatim" gets one semicolon-spliced run-on
sentence, which is precisely the thing a dialog must not be — so C598-3 will paraphrase,
and AC5 fails at the moment it is tested.

JLS has already made this decision, in the other direction, and recorded it:
`LoadError` (`src/jls/LoadError.java`, #58) is a record of *(category from a fixed taxonomy,
detail, line, element, hint)*, published through `JLSInfo.setLoadError` so "every front end
shows the same message" (ARCHITECTURE.md, "Error-reporting contracts"). `TellUser` (#81) is
the single dialog surface, ratchet-enforced. The board subsystem is quietly growing a third,
string-based error contract alongside those two.

**The design I would build instead:** an `HdlDiagnostic` record — category (`UNBOUND_PORT`,
`WRONG_DIRECTION`, `UNKNOWN_PIN`, `PIN_CLAIMED_TWICE`, `MALFORMED_LINE`,
`UNEXPORTABLE_ELEMENT`, `TOOL_MISSING`), subject (the port bit / pin / element / tool name),
detail, and hint — and `HdlExportException` carrying `List<HdlDiagnostic>` with `getMessage()`
as a *rendering* of it. Then:

- AC5 is free: the GUI iterates the list; `TellUser` formats it; nothing is paraphrased.
- AC3's "stable order" is a list property, trivially assertable, instead of a substring-order
  claim about a joined string.
- The CLI can emit the same list as JSON on request. That matters more than it sounds: the
  README and `docs/batch-interface.md` name autograders as a first-class audience, and today
  an autograder that wants to tell a student *why* their export failed must regex English.
- The bash preflight can emit the same record shape, closing #598's fourth class in the same
  vocabulary rather than by coincidence of wording.

**I am explicitly disregarding AC1's "asserted on the specific diagnostic text, not merely
that an exception was thrown."** That bar is the *opposite* of the recorded LoadError
decision, whose taxonomy exists precisely so that "tests assert on these labels, keeping the
detail wording free to improve" (`LoadError.java`, Category javadoc). Eleven tests in
`UnbindablePortsTest` already substring-match English; every one of them is a lock on
wording that CAP-38's GUI half will want to rewrite for a dialog. Assert on
*category + subject + presence of a hint*, and pin the rendered English once, in one golden,
where changing it is a deliberate act rather than eleven broken tests. That satisfies the
issue's real intent ("never generic, never silent") without cementing the prose.

## Reframe 2 — "wrong direction" is the wrong name for the check worth having

AC2 wants "an output port bound to an input-only pin or the reverse... with the board
definition cited as the authority." `Board` (`src/jls/hdl/board/Board.java:26-27`) is
`(name, fpga, format, Map<String,String> pins)` — pin name to location, nothing else. There
is no authority to cite. #213's H2 made that deliberate: "a board is just data... adding a
board is adding a table entry, never new code." AC2 silently amends that hypothesis and does
not say so.

Worse, on an iCE40 the direction framing is not even true. Nearly every TQ144 pin is
bidirectional I/O; direction is a fact about *what the board wires to the pin*, not about the
pin. Of the 33 `Boards.ICESTICK` entries, 16 are header pins (`PMOD*`, `J1_*`, `J3_*`) that
are legitimately either direction — a naive input-only/output-only typing would either refuse
valid designs or say nothing about the pins that matter.

The check actually worth having is a **contention interlock**, and it is a stronger claim than
direction typing: binding a JLS *output* to a pin that already has an external driver —
`CLK` (12 MHz oscillator), `UART_RX`, `IR_RXD`, a button — puts two drivers on one net. That
is the one class of pin error that can be physically harmful and is guaranteed to be silent.
Symmetrically, binding an *input* to `LED1..LED5` reads a pin whose only load is an LED, which
is merely useless, not dangerous. So the model is three-valued and physical, not two-valued
and syntactic:

```
enum PinRole { DRIVEN_BY_BOARD, DRIVES_BOARD_LOAD, FREE_IO }
```

with a one-line human description per pin ("driven by the on-board 12 MHz oscillator") that
the diagnostic quotes. The refusal then reads as hardware fact — *"port `count[0]` is an
output bound to pin CLK, which is driven by the on-board 12 MHz oscillator; driving it from
the FPGA would contend with the oscillator"* — instead of as a type error. That is the message
that teaches, and it is what "the board definition cited as the authority" should have meant.

**Timing argument for doing this now rather than later:** the metadata cost is per board table,
and there is exactly one board table today. #264's Stage 2 (ECP5/LPF) and CAP-38's PF-3
(Basys-3/XDC) each add another. Retrofitting per-pin roles across three transcribed tables is
strictly worse than establishing the field while `Boards.ICESTICK` is alone. This is the cheap
moment, and the issue does not notice that it has one.

## Reframe 3 — the highest-leverage move is to stop asking students to type a pin file

Eight distinct error classes exist in `PinBindings` + `PcfEmitter` (malformed line, key bound
twice, unknown port, unknown pin, indexed form on a scalar port, scalar form on a wide port,
bit index out of range, pin claimed twice, plus the unbound-port case). Every single one of
them exists *because the binding file is hand-typed free text in a grammar JLS invented*.
#638 proposes to make the refusals excellent. The more elegant route to the same end is to
make most of them unreachable.

Two concrete pieces, both cheap, both reusing code that already exists:

1. **`jls -pins-template -board icestick design.jls`** — walk `model.ports()` (the exact loop
   at `PcfEmitter.java:73-81`, ~30 lines reused) and emit one line per port bit with the pin
   field blank and the board's `FREE_IO` pins listed in a trailing comment. A student who
   starts from that file cannot produce the unknown-port, wrong-index-form, out-of-range, or
   malformed-line errors at all. This is a smaller change than AC2 and removes more round trips.
2. **The GUI dialog (PF-1) as a combo box per port bit, populated from role-compatible pins.**
   Once the pin set is a picker rather than a text field, unknown-pin, wrong-direction, and
   pin-claimed-twice become *structurally unrepresentable*. The refusal path remains, as the
   headless/autograder fallback — but it is the fallback, not the headline.

This inverts the ordering #598 assumes ("the diagnostics themselves are testable headlessly
and can land ahead of [the GUI]"). Landing ahead is a fine *schedule*; it should not become a
*justification* for perfecting refusals that the picker will make unreachable. The template
generator is the piece that pays off in both worlds and belongs in this task.

## What I would ship as TASK-C598-1

1. `HdlDiagnostic` record + `HdlExportException` carrying a list; all three existing refusal
   sites (`HdlExporter`, `PcfEmitter`, `PinBindings`) migrated; `getMessage()` becomes a
   rendering. Existing goldens unchanged.
2. `PinRole` on board pins with a human description; contention interlock as the AC2 check,
   framed physically, with `FREE_IO` on all header pins so no valid design is refused.
3. `-pins-template` emission reusing the `PcfEmitter` port walk.
4. Tests asserting category + subject + hint-presence; one golden pinning the rendered English.
5. AC3/AC4 recorded as already-holding contracts in `docs/`, not re-implemented.

That is above the stated 0.5-1 mw band — honestly, 1.5-2 — and I would rather see the band
corrected than the scope trimmed to two diagnostics and a comment about "vocabulary." The
trimmed version cements a string-blob error contract in the exact subsystem that #598 AC-4 and
CAP-38 PF-1 are about to demand structure from, and it books the board-metadata decision at
the most expensive possible moment (after two more board tables exist).

## Alignment summary

| | |
|---|---|
| Strengthens the arc | Yes — CAP-38's outcome depends on named refusals |
| Duplicates existing work | Partly — AC1/AC3/AC4 are largely landed and tested |
| Pulls against the arc | Yes, in two places: AC1's assert-the-prose bar contradicts the recorded `LoadError` taxonomy decision; AC2 amends #213's H2 ("a board is just data") without saying so |
| Missing seam | The diagnostic *type*. AC5 names the need and then asks for prose discipline instead of a record |
