# Issue #643: TASK-C598-3: one diagnostic vocabulary, two surfaces — the GUI dialog shows the text the headless path reports, and no path can claim success when a stage did not run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the acceptance criteria away and the claim underneath is: *the board flow must
never lie about having worked, and must say the same true thing wherever a student is
standing.* That is the right claim, it is the reason CAP-38 (#522) exists at all —
Logisim-Evolution's board flow is simultaneously its best card and its top reliability
complaint — and nothing below argues against it.

What I do argue against is the mechanism. #643 proposes to achieve "one vocabulary, two
surfaces" by building a second surface and then *testing that the two strings match*.
JLS has already solved this exact problem twice, and both times it solved it by making
the second string not exist.

- `LoadError` (#58, `src/jls/LoadError.java`): a record with a `render()` method, and
  ARCHITECTURE.md:189-191 states the principle in one sentence — the legacy string "is a
  derived view, so every front end shows the same message."
- `TellUser` (#81, `src/jls/TellUser.java`): the single seam. `TellUser.error` already
  prints `jls: error: <message>` headless and shows *that same `message` String* in a
  dialog interactively. Byte-equality across the two surfaces is not a test there; it is
  the shape of the method. `NotificationRatchetTest` keeps it the only seam.

So the GUI/CLI equality this task wants is, for three of the four refusal classes, one
routing decision away — not a new dialog plus a comparison test.

## Reframing 1: a `Refusal` value, not a matched pair of strings

Give the board flow a `LoadError`-shaped record — class (unassigned port / wrong
direction / un-exportable element / absent tool), subject (the port, element, or tool
name), detail, fix, optional location — with one `render()`. Both surfaces render the
same value through `TellUser`. Then:

- #643 AC-1 is satisfied by construction. There is no second formatter to drift.
- The test that survives is not "drive both and diff", which only covers the four cases
  someone remembered to enumerate; it is a **ratchet** in the `NotificationRatchetTest` /
  `HdlPolicyTest` family asserting that no board-flow code path formats a refusal except
  `Refusal.render()`. That is a stronger guarantee and it holds for refusal class five.

**I am explicitly disregarding AC-1 as written.** A test that compares two independently
produced strings certifies that a duplication is currently in sync; it institutionalizes
the duplication. Replace it with: every refusal class has a headless test on its specific
text (that is #638 AC-1 and #640 AC-1 — already owned upstream, do not re-file), plus the
single-producer ratchet here.

## Reframing 2: the fourth class crosses a language boundary — do not clone it, publish it

AC-1 says all four classes render byte-equal. The absent-toolchain diagnostic is produced
by **bash**: `scripts/icestick-handoff.sh` lines 100-137, the `need()` loop building
`"$1 - $2 (get it from $3)"` under the header "the iCE40 toolchain is incomplete; install
the following and re-run:". To get those exact bytes into a Swing dialog you must either

(a) reimplement the tool table in Java — which #598's boundary note ("extend and surface
them, do not rebuild them") and #597 AC-5 forbid, and which drifts the first time someone
adds a tool to the script; or

(b) treat the script's stderr as the diagnostic and render it verbatim.

(b) is right but weak (unstructured text in a dialog). The strong version: **make the tool
table data, the way `Boards` already makes boards data.** `Board.java`'s own javadoc states
the principle — "a board is deliberately just data … adding a board is adding a table entry
in `Boards`, never new code." A board's *flow* is equally data: the ordered tools, each with
role and source URL, and which of them are needed only for `--flash`. Note the duplication
already exists in the tree: `src/jls/hdl/yosys/YosysLocator.java` is a Java-side PATH search
for yosys, and the script does `command -v yosys` for the same tool with a different message.

Concretely: attach the tool table to the `Board` record (or a `Toolchain` beside it), have
Java own preflight and emit `Refusal` values, and have the script call JLS for its preflight
rather than owning a parallel `need()` list — or, if the script must stay standalone, have it
emit the machine-readable form JLS parses. One producer, two renderings, no cross-language
clone. This also pays forward: PF-3's Basys-3/vendor-toolchain decision and #416's second
board become table entries instead of a second preflight, and it is the same generalization
`docs/capability-roadmap/sweep-06-physical-boundary.md` §F asks for (Board → target descriptor).

## Reframing 3: AC-3 wants a type, not a stub test

"No path can report success when a stage did not run … asserted by a test that stubs each
stage into a no-op in turn" tests for the absence of a bug on the paths you thought to stub.
Make it unrepresentable instead. Model the flow as stages returning a sealed result —
`sealed interface StageOutcome permits Produced, Refused`, where `Produced` **carries the
artifact** (the `.v`, the `.pcf`, the `.bin` path) — and derive the success report from
possessing artifacts, not from "no exception was thrown". A stubbed no-op stage then cannot
type-check into a success report: it has nothing to hand over.

This is idiomatic for this repo, not novelty. JDK 25 is the floor; `LoadError` is already a
record with a closed `Category` enum; `CircuitOp`/`OpSink` already run validate-then-apply so
"a rejected op leaves the circuit byte-identical" is pinned; `JLSStart`'s export path already
uses temp-and-rename so a partial artifact cannot exist. Keep the stub test as a cheap
regression net; do not let it be the guarantee.

## Reframing 4: two of the four refusals should be unreachable, not well-worded

The issue's own justification is pedagogic: a green-looking result makes the student debug
the circuit instead of the setup. The strongest form of that is not a better dialog — it is
not being able to arrive at the failure with bindings already invested.

- #597 AC-2 already has the pin dialog validate against the board definition *at entry*. If
  it does, wrong-direction and unassigned-port refusals are structurally unreachable from the
  GUI, and AC-1's demand that all four classes "render in the GUI" forces you to build and
  test display paths that production can never reach. Name that tension rather than coding
  around it.
- The absent-toolchain check should run when the flow is *opened*, surfaced as a disabled
  action with a visible reason — the pattern the codebase already uses for SubCircuit's
  disabled banner — not as a refusal at the end of a hopeful click.
- Worth flagging upstream: `Board` is `(name, fpga, format, Map<String,String> pins)` with no
  per-pin direction, so #638 AC-2's "wrong-direction binding, with the board definition cited
  as the authority" has no authority to cite yet. Two of your four classes are not equally
  real; one needs `Board` extended in #264/#638 before #643 can render it.

## Reframing 5: AC-4 is asking for a persisted value, not dialog state

"Retry without re-entering bindings already given" is dialog-session bookkeeping only if
bindings live in the dialog. #597 AC-2 already requires the GUI to produce bindings
byte-identical to the headless `-pins` file. Make that file (or a binding block in the `.jls`)
the dialog's model rather than its output, and AC-4 disappears: retry is free, bindings survive
a crash, a student can hand their GUI-made bindings to a grader's CLI, and retargeting to
#416's second board is editing a value instead of re-entering twenty pins.

## One thing to fix in `TellUser` while you are here

AC-2 wants aggregated refusals shown as several, in order, untruncated. `TellUser.error` takes
a `String`; six unassigned ports plus a scroll pane plus copy-to-clipboard is not a
`JOptionPane` message string, and `NotificationRatchetTest` forbids reaching past the seam.
The right move is to teach the seam a list shape — `TellUser.refusals(parent, title,
List<Refusal>)` — not to grant the board dialog an exemption. #59/#492's export rejections,
#288's error list, and sweep-06 §G's future ERC (which the roadmap already says "reports
through the existing `LoadError`-shaped structured-diagnostic discipline and the `TellUser`
boundary") are the second, third and fourth consumers of exactly that method.

## Alignment verdict

This task pulls with the project's arc, not against it, and it is not duplicative of #638/#640
— it is their surface. But as written it would ship the fourth bespoke diagnostic dialog in a
codebase whose recorded architecture keeps insisting on one structured value, one renderer, one
seam. Build the `Refusal` value and the `TellUser` list method (both fit inside the 0.5-1 mw
band), route the GUI through them, make the toolchain table data, and type the stage outcomes.
The board-flow-specific dialog class that AC-1 implies should not exist; the problems-view it
wants to become should be filed as the successor surface so nothing built here has to be
demolished to get there.
