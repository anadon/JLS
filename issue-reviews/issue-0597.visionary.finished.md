# Issue #597: FEAT-C38-1: a student picks a board, assigns pins in a dialog the board itself validates, and clicks once — the headless board flow becomes a File-menu path with no terminal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its ACs, the claim is: *the classroom endpoint is a blinking board, and
JLS's path to one is invisible to the people the tool is for.* That is right, and it
is the one claim in CAP-38 (#522) that no other issue owns. The "surface over the
existing headless path, not a second implementation" framing is also right, and it is
consistent with the settled stance in `docs/grand-architecture.md:55-57` ("orchestrate
external tools, never reimplement") and with the module graph already in tree
(`src/jls/boot/HdlModule.java`, `GuiModule.java`).

What I do not accept is the shape of the deliverable. As written, the feature is *a
modal session*: pick a board, fill a table, press a button, throw the table away. Three
reframings below make most of that session unnecessary, make two of the ACs true by
construction rather than by test, and make #416's second board actually free instead of
free-at-one-layer. I take the two correction comments as read; they fix real defects
(AC-3 vs. the zero-`ProcessBuilder` invariant, AC-1 vs. the absent registration seam)
but both fix the surface without questioning it.

## Reframing 1 (primary): a pin binding is part of the design, not a dialog session

Today the binding lives only in a `-pins` file (`PinBindings.parse`, 98 lines, keys of
the form `port` / `port[bit]`). The issue proposes a GUI that produces the same text.
Consider instead: **make the board pin an `Attribute` on `InputPin`/`OutputPin`/`Clock`,
and the target board a circuit-level property.** JLS already has the machinery —
`jls.elem.Attribute` (#23/#52) drives save, copy and load dispatch from one declaration;
`Circuit.readFormatHeader` already negotiates format versions (#79), so adding a
`String boardpin` attribute is a `FORMAT` bump and a row, not a new subsystem; the
round-trip suites cover it automatically.

What falls out:

- The pin-assignment dialog mostly **disappears**. You bind a pin where you already
  double-click to name it. What remains is at most a read-only overview table plus a
  "bind all / check" action — a much smaller, much more accessible surface for #91.
- Bindings **survive**: save/load, undo (`CircuitSnapshot` rides the same pipeline),
  version control, a diff, an autograder submission, and — for free — the collab
  operation vocabulary (`docs/operation-layer.md`), because an attribute change is
  already an op. A dialog-session binding survives none of that.
- **AC-2 becomes structural.** "GUI and CLI emit byte-identical constraint files" stops
  being a test you can regress and becomes true because both paths project the same
  in-model bindings through `PcfEmitter.emit`. The `-pins` file demotes to an
  import/override — the autograder-friendly serialization of data the circuit already
  carries, not the source of truth.
- The board picker becomes "what is this design targeted at", which is what a classroom
  assignment actually *is* ("submit a `.jls` that fits the iCEstick"), not a per-export
  question.

Costs, stated honestly: it couples a design to a board (mitigate with a per-board map
keyed by board name, or accept single-target — classroom reality), it is a save-format
change, and it is bigger than 2–3 mw. **I am explicitly disregarding the stated
acceptance criteria here**: AC-2's byte-identity test and AC-4's widget inventory are
criteria for the wrong artifact if the binding is model data, because most of the widgets
they govern no longer exist.

## Reframing 2: one export path, not two

The CLI has exactly one verb with two modifiers — `-export out.v` plus optional
`-board`/`-pins`, dispatched as one branch at `src/jls/JLSStart.java:387-427`. The issue
proposes a **second** File-menu entry parallel to #288's "Export HDL…", with its own
chooser, its own accessible-name inventory, its own harness surface. That is a GUI whose
shape does not match the CLI it is wrapping, and AC-5 then has to *police* a divergence
the design created.

Simpler: **#288's Export HDL dialog grows an optional "target board" section.** One menu
entry, one chooser, one flow; board-and-pins is a modifier, exactly as on the command
line. AC-5 ("no logic duplicated out of `jls.hdl.board` or `scripts/`") becomes nearly
vacuous, the a11y/harness surface halves, and #288's §13 out-of-scope line is discharged
by extension rather than by a sibling entry. This does not conflict with #288 landing
first — it is the natural second commit on the same item.

## Reframing 3: emit a project, not a command line

The corrected AC-3 (print a copyable handoff command line) is right to refuse
`ProcessBuilder` in `src/` — #359 §4 invariant 1 and the script's own header ("delegate,
do not reimplement") both bind — but it reaches for the weakest possible artifact. A
shell one-liner is not copyable on Windows, is not consumable by CI, and is not gradable.

Better: **one action writes a self-contained handoff directory** — `design.v`,
`design.pcf`, a generated `Makefile`/`build.sh`, and a README naming the four tools and
where to get them. JLS still spawns nothing; the *artifact* carries the handoff. This is
what CAP-38's PF-4 (hardware-free CI) wants to consume, it is diffable and submittable,
and "no terminal to produce everything you need" is an honest claim rather than a
hedged one.

## The extensibility claim is true one layer up and false one layer down

AC-1 says a board added by #416 "appears with no GUI change". True for the picker (it
reads `Boards.all()`), and the second comment correctly reduces it to a structural test.
But follow the flow one step further: `scripts/icestick-handoff.sh` **hard-codes**
`--hx1k --package tq144` and says so in its header ("hard-coded to match
Boards.ICESTICK"), because `Board` carries the device only as prose —
`fpga = "Lattice iCE40-HX1K, TQ144 package"` (`src/jls/hdl/board/Board.java:26-27`,
`Boards.java:34`). So a second board appears in the picker and then hits an iCEstick-only
handoff. The extensibility this feature advertises stops at the dialog boundary.

Concrete fix, and it belongs to #264 rather than here: promote the device/package (and
the format's tool flags) from the `fpga` prose string to machine fields on `Board`, and
generate the handoff recipe from that record. Then reframing 3's project directory is
board-parameterized by construction and #416's second board is genuinely free end to end.

## The reach problem nobody in this thread names

`HdlExporter` rejects `SubCircuit` and `Memory` (`HdlExporter.java:83-86`, `EXPORTED` at
`:418-424`), and `HdlPolicyTest` pins those refusals as *intended*. So the outcome this
feature sells — "a student takes a drawn circuit to a board" — is bounded to flat,
memoryless designs. `docs/capability-roadmap/sweep-06-physical-boundary.md:124-133` says
it outright: #215's value is "capped at whatever a student can draw without a subcircuit
or a memory", and "a student who structures their design well is punished by the
exporter". A polished picker over an exporter that refuses hierarchy is a beautiful door
onto a small room.

I do not think that blocks this feature — a blinky demo is a real demo and the surface is
cheap. But two things follow. First, the feature should **state its reach** rather than
imply generality, and the dialog should refuse a subcircuit/memory design at *entry*
(the same "validate at the moment, not at export" principle AC-2 already asserts for
pins; #598 owns the words). Second, at the capstone level, hierarchical export outranks
PF-3's second board for classroom value, and #522 should be sequenced accordingly.

Related, and worth saying once: the terminal is not the only barrier. Installing yosys,
nextpnr-ice40, icepack and openFPGALoader is itself a terminal act. This feature removes
the smaller half of the terminal. The larger half is a pinned, documented one-install
toolchain story (oss-cad-suite), which is docs work, not GUI work, and is cheaper than
everything above.

## What I would keep unchanged

- The "surface over the substrate, file defects upstream" discipline, and the #264/#598
  boundary comment — that comment is the best-reasoned thing in the thread.
- KC-38-2 (consumed rather than rebuilt if #264 Stage 2 lands the flow). Concrete
  trigger, correct escape hatch.
- The refusal to drive vendor toolchains, and the corrected AC-3's zero-subprocess
  guard. That invariant is load-bearing for the offline single-jar promise.

## Suggested restatement of the outcome

> A drawn circuit carries its own board target and pin bindings as part of the design.
> The existing Export HDL dialog, when a target board is set, writes a complete,
> board-parameterized handoff directory the open toolchain consumes unmodified. The
> `-pins` file remains the headless import/export of the same data, so GUI and CLI cannot
> disagree — not because a test compares them, but because there is only one binding.
