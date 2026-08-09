# Issue #598: FEAT-C38-2: every refusal on the way to the board names the fix — unassigned pins, wrong directions, un-exportable elements and an absent toolchain each get a specific diagnostic before the cable is touched
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its four-class enumeration, #598 is a bet that **the classroom
value of the board flow lives in its failure path, not its success path**.
That bet is correct and it is the sharpest thing in CAP-38 (#522). A student
who reaches a blinking LED learns one thing; a student who is told "port
`led[2]` is bound to pin `SW1`, which is an input on this board" learns the
thing the course is actually about. #522's evidence section names
Logisim-Evolution's board flow as simultaneously its best card and its top
reliability complaint — so the differentiator was never "have a flow", it was
"have a flow whose refusals are legible". #598 is the issue that owns that
differentiator. Endorsed as an *outcome*.

The design, though, cuts along the wrong seam, and the acceptance criteria
encode the wrong seam into tests that will be expensive to unwind.

## Finding 1 — the "four refusal classes as one layer" is not one layer today

Read against HEAD, the four classes are in four different states:

| Class | Where it lives now | What #598 actually adds |
|---|---|---|
| Unassigned / mis-shaped pin | `src/jls/hdl/board/PcfEmitter.java:115-119`, `PinBindings.parse` | nothing structural — it is done, and well |
| Un-exportable element | `HdlExporter` reject set → `HdlExportException` | nothing structural — also done |
| Missing external tool | `scripts/icestick-handoff.sh:100-137` (bash) | **must be rebuilt in Java** — see Finding 2 |
| Wrong pin direction | *nowhere* | **a new board-model capability** — see Finding 3 |

So the issue's own framing ("the existing all-or-nothing disciplines are the
model and are reused rather than re-derived") is true of two classes,
impossible for the third, and vacuous for the fourth. Filing them as one
uniform layer hides the fact that two of the four are the real work and
neither is a diagnostic-text problem.

## Finding 2 — AC-4 cannot be met by extending #264's preflight; it can only be met by replacing it

AC-4 asks for "one diagnostic vocabulary, two surfaces, pinned by a test",
and the boundary note says #264 owns the tool preflight and must be extended,
not rebuilt. Those two sentences contradict each other in this tree:

- the only production tool-presence check is `command -v` inside a **bash
  script**. JLS ships an `.msi` and a `.dmg`; neither carries bash. The GUI
  dialog on Windows can never render the script's text because the script
  never runs.
- `jls.hdl.ToolLocator` — the class that would give Java the same answer —
  lives at `test/jls/hdl/ToolLocator.java:46`. It is **test scope**. Nothing
  in `src/` can call it.

The honest conclusion is that the tool preflight must move into `src/` (a
promoted `ToolLocator` plus a `Toolchain` descriptor per board), and
`icestick-handoff.sh` must then *call* it (`jls --preflight -board icestick`)
rather than keep its own copy. Two independently-maintained preflights that
must agree on message text, one of them unreachable from half the shipped
platforms, is precisely the Logisim-Evolution #91 failure mode this issue was
filed to avoid — reproduced one level up, inside JLS.

## Finding 3 — "wrong direction" is the only genuinely new capability here, and it is mis-scoped

`Board` is `record Board(String name, String fpga, Format format,
Map<String,String> pins)` (`src/jls/hdl/board/Board.java:26`) — pin name to
physical location, and nothing else. There is no direction, no role, no
"this pin is wired to a pushbutton". Detecting a wrong-direction binding is
therefore not a diagnostic that can be added to `PcfEmitter`; it needs the
board table to gain per-pin capability data first.

That matters more than it sounds, because **PCF `set_io` carries no direction
either**. yosys/nextpnr will happily build a bitstream that drives a pin
wired to a switch. This is the one refusal in the set that *no downstream
tool will ever produce* — it is uniquely JLS's to catch, it is the literal
content of "checked before the cable is touched", and it is the highest-value
quarter of this issue. It is currently one bullet in a diagnostics ticket.

It also belongs with #416/#264 rather than here: pin **roles** (LED, SWITCH,
BUTTON, CLK, PMOD, SEG) are what make PF-1's pin-assignment dialog usable at
all — a picker that offers "LED0..LED4" beats a picker that offers 144 pin
numbers plus a validator that scolds you afterwards. Basys-3 (PF-3) is
unusable without role data. Recommend splitting pin-role data out as a small
board-table change consumed by PF-1's dialog, PF-2's validator, and #416's
second board alike.

## The reframing — a preflight *value*, not four throws

The issue's mechanism is: each subsystem throws a better string at the moment
it is reached. Three consequences follow that nobody wants:

1. **Refusals cannot aggregate across classes.** AC-2 requires aggregation
   *within* a class, which the code already does. But the student experience
   is governed by aggregation *across* classes, and the current pipeline
   order (`JLSStart.java:414-433`: build model → emit HDL → emit PCF, then
   the script preflights) makes that structurally impossible. A student with
   a Memory, two unbound pins, and no yosys installed today does three
   fix-and-rerun round trips. That is the actual defect, and #598 as written
   does not fix it.
2. **AC-4 becomes a string-equality test.** Pinning "the GUI shows the same
   text as the CLI" by asserting literal diagnostic strings in two places is
   the brittle encoding of a property that should hold *by construction*.
3. **AC-3's "nothing on disk" has to be re-audited forever.** It happens to
   be true now (`JLSStart.java:414-433` validates everything before the first
   write; `:438-452` is temp-and-rename), but it is an emergent property of
   statement order in a 1400-line method, not a guarantee.

All three dissolve under one reframing: **make the refusal a value, and give
it a single renderer.** The project already did exactly this once and
recorded it as an architectural contract — `ARCHITECTURE.md` "Error-reporting
contracts": `LoadError` is a fixed category taxonomy plus location, detail,
and an actionable hint, published through `JLSInfo.setLoadError`, with the
legacy string "a derived view, so every front end shows the same message."
#598 does not mention `LoadError` anywhere, and it is the seam it wants.

Concretely:

- `jls.hdl.board.BoardPreflight.check(Circuit, Board, PinBindings, Toolchain)`
  → `List<Diagnostic>`, a pure function with **no I/O and no writes**, running
  all four checks unconditionally and returning everything at once.
- `Diagnostic` shaped like `LoadError`: `Kind` (`UNBOUND_PORT`,
  `WRONG_DIRECTION`, `UNEXPORTABLE_ELEMENT`, `MISSING_TOOL`, …), subject
  (port / element + grid location / tool name), and `fix` — the actionable
  hint, which is the thing AC-1 is really asking for.
- one formatter. CLI renders each diagnostic as a `jls: error: …` line per
  the #42 contract; the GUI dialog renders the same list through `TellUser`
  (the only sanctioned dialog site, `src/jls/TellUser.java`). AC-4 is then
  true because there is one string-producing function, and the test asserts
  *that*, not that two hand-written texts match.
- `HdlExportException` (`src/jls/hdl/HdlExportException.java`, 25 lines,
  message-only) gains the diagnostic list beside its message, so existing
  callers keep working while structured consumers appear.

This makes AC-1 richer (assert on `Kind` + subject + fix, which survives
rewording), AC-2 stronger (aggregation across classes, not just within one),
AC-3 trivially true (the check verb has no write path at all), AC-4 true by
construction, and AC-5 checkable (a success path that returns a non-empty
diagnostic list is a bug the type system makes visible).

It also hands #522 two acceptance criteria for free: AC-3 of the capstone
wants a CI lane, and `jls --preflight` *is* that lane — hardware-free,
toolchain-free, exit 0/1, runnable on every push.

## Second alternative — the check registry, and where it is going anyway

If the four checks are a `List<Check>` rather than four call sites, sweep-06's
change **G** (`docs/capability-roadmap/sweep-06-physical-boundary.md`,
"Electrical rule checking") is additive rather than a second system: undriven
inputs, multiply-driven non-tri-state nets, combinational loops, width
mismatches, fanout limits, registers with no reset in an ASIC-bound design.
The sweep names ERC as "the one thing JLS most conspicuously lacks against
Digital and Logisim-evolution" — and #598's four refusals are its first four
rows. Building them as four bespoke throws forfeits that.

The out-of-the-box version, once the check is a pure function of
(Circuit, Board, Bindings): run it **continuously in the editor** under a
selected board target, the way a compiler underlines errors. Unassigned pins
and unsupported elements become visible while drawing, and the export dialog
stops being where students discover bad news. That is a strictly better
pedagogical artifact than a good modal dialog, and the preflight-as-value
design is the only one that makes it cheap.

## Finding 4 — do not let excellent refusals make a wall comfortable

Sweep-06 measured, at HEAD, that the repository's own flagship design cannot
export at all: `-export cpu.v riscv/build/addi.jls` refuses on Memory,
SubCircuit and ShiftRegister, and `HdlPolicyTest.memoryIsRejectedByName` /
`.subCircuitIsRejectedCleanly` **pin that rejection as intended behaviour**.
The teaching inversion is in the tree right now: a student who structures a
design with subcircuits is refused; one who draws a flat 1000-element mess
gets through.

#598 will make that refusal beautifully worded. It should therefore say
explicitly what its diagnostic *cannot* say: the un-exportable-element class
must name the tracking issue and its status ("Memory export is not
implemented yet — #59"), never imply the student drew something wrong. And
CAP-38 should record that its classroom outcome is capped at "whatever fits
on an iCEstick without a subcircuit or a memory" until sweep-06's change A
lands. A refusal layer is not a substitute for the coverage work, and it is
the kind of feature that can quietly become one.

## What I am setting aside from the stated criteria, and why

- **AC-1's "specific diagnostic *text*"** — I would not test text. Test
  `Kind` + subject + presence of a fix. Text-equality tests make rewording a
  diagnostic (the most common improvement anyone will ever make to this
  feature) a test-breaking change, which trains contributors to stop
  improving diagnostics. The property worth pinning is that both surfaces go
  through one formatter, plus one golden covering the full rendered block.
- **The "extend #264's preflight, do not rebuild" boundary** — not honourable
  as stated. The tool preflight has to be rebuilt in Java for the GUI half to
  exist at all; the shell script should become a client of it. Say so in the
  issue rather than discovering it in review.
- **"Four refusal classes as one layer"** — three are a rendering change over
  work that exists; the fourth (wrong direction) is a board-data change that
  belongs beside #264/#416. Filing them as one band hides a real dependency.

## Verdict

**endorse-with-reframing.** The outcome — every refusal names the offender
and the fix, before hardware — is the right differentiator for CAP-38 and
pulls with the project's arc, not against it. But cut the seam at a
structured `Diagnostic` value with one formatter (the `LoadError` pattern
this project already ratified), promote tool detection out of bash and out of
test scope, split pin-role data into the board table where PF-1 and #416 both
need it, and let the four checks be the first four rows of a registry that
sweep-06's ERC work extends. Same outcome; less code; and the acceptance
criteria stop being tests and start being types.
