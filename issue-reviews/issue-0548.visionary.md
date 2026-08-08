# Issue #548: FEAT-C27-2: an Examples menu ships at least ten curated circuits — each one loads, simulates, and carries a caption and a suggested exercise
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) measures one thing: *time from first hearing of JLS to a running, understood
circuit, under ten minutes, without reading anything longer than a caption.* #548 is the
substrate for that number. The corpus must exist — verified: `resources/` holds only `help/`
and `packaging/`, there is no `resources/samples/`, and the only `.jls` files in the tree are
three test fixtures plus `riscv/gui/cpu.jls`. A tool that teaches circuit drawing and ships
zero circuits is the defect. Building the corpus is right and it should be built once.

But the issue conflates three separable things under one feature: **the corpus**, **its
metadata**, and **one particular surface (a top-level menu)**. Its own dedup comment
(2026-08-04) insists "#548 owns the corpus; #551/#552/#573 own surfaces over it" — and then
#548 keeps a surface anyway, the one surface that costs a permanent menu-bar slot. That seam
is cut in the wrong place, and four downstream issues will inherit the cut.

## Reframe 1 — the corpus is (circuit, declared metadata) pairs, not circuits with text glued on

#381 P8 defines the caption as "a header `Text` element" inside the circuit; #548 AC-3 inherits
that ("carries a caption element") and AC-4 adds an exercise. Follow it forward: a caption and
an exercise now live *on the canvas of the circuit the student is supposed to study and
modify*. The Examples menu must open or parse each file to know what to say about it; #551's
gallery re-extracts the same strings out of SVG; #573's browser page re-extracts them again;
#552's lessons restate them a fourth time; and category membership (AC-2) exists only as tribal
knowledge in a filename convention.

Ship instead a declared index next to the circuits — `resources/samples/samples.properties` (or
one small `.toml`) carrying, per entry: id, file, title, category, one-line caption, one-line
exercise, and **order**. That is the single authority. Every surface reads the same rows.
AC-2's category coverage becomes an assertion over data instead of a reviewer's eyeball. AC-4's
"no longer than a caption" becomes a length check on a field. #381 §7.5's "package-private
sample enumerator, must not become public API" is exactly the right instinct — but with four
consumers already filed, the enumeration *is* the product's internal contract and deserves to
be a named, tested one (still not a supported external query). Keep the in-circuit `Text`
header if you like it for standalone opens, but make the test assert it *matches* the index
row; one authority, one drift check.

## Reframe 2 — the menu is the wrong discovery surface, and PF-3 already owns the right one

CAP-27 PF-3 builds a first-run experience offering New / Open Example / Tutorial, and #381's P2
already specifies that panel with shared `Action` identity (P9). A person who has never seen
JLS is looking at that panel, not scanning an eighth top-level menu. AC-5 asserts K9/D9 holds
because "the menu adds one top-level entry" — but the honest reading of D9 ("the first-year
must never SEE the ECE/EE machinery") is that a permanent chrome slot spent on a thing a user
needs exactly once is a poor trade, paid by every user forever. The menu bar is already File /
Edit / Element / Simulator / View / Global / Help.

Concrete alternative: **the corpus surfaces through PF-3's welcome pane (primary) and
`File → Open Example…` (secondary, next to Open), with a Help-menu pointer beside Tutorial** —
zero new top-level entries, one discovery path for the naive user, one for the returning one.
If the maintainer wants the menu anyway, that is a taste call, but it should be made against
PF-3's panel rather than before it, which means #548's ordering should note PF-3 as a peer to
coordinate with, not just #381 as a predecessor.

## Reframe 3 — the corpus is a path, not a set of ten

"≥10" is a count standing in for an outcome, and it is gameable in both directions: ten trivial
circuits pass; and a flat list of ten unordered items is itself a choice problem for someone
with no prior knowledge. CAP-27 is measured on the *first* circuit, not the tenth. Order the
corpus explicitly (`01-and-or-not`, `02-full-adder`, `03-counter`, …) and let the menu render
it in that order. Two things fall out for free: #552's stepped lessons become annotations of an
existing spine rather than newly-authored parallel content, and CAP-33's course kits get a
syllabus order they would otherwise have to invent. I would restate AC-1 as "a first-time user
reaches a simulating circuit from item 1 in under N clicks" and demote the count to a
sufficiency floor.

## Reframe 4 — exercises should be checkable, because JLS already has the mechanism

AC-4 gives each example a one-line suggested exercise and stops. But this project's most
distinctive shipped asset is the *documented, stability-contracted* batch/grading interface:
`-t` test vectors, watched-element output, VCD export (`docs/batch-interface.md`,
`examples/autograde/autograde.py`, the container image sold to autograders in the README).
Ship a `.tests` file beside each example and the exercise stops being a suggestion and becomes
a loop: try it, run it, see pass/fail. This costs little — the test vectors are also the
natural fixture for AC-3's "simulates under the batch simulator in a headless test", so the
same artifact discharges the acceptance criterion *and* the pedagogy. It is also the cheapest
bridge that has ever been available between CAP-27 (on-ramp) and CAP-33 (#517, course kits):
the curated corpus becomes the seed grading corpus rather than a second, later authorship.
This is the single biggest thing the issue never considered.

## Reframe 5 — the RV32I "showcase" is a credibility artifact, not an example, and it is generated

Verified: the full RV32I CPU does not exist as a file. `riscv/make_cpu.py` *generates* it from
assembly into `build/*.jls`; what is checked in is `riscv/gui/cpu.jls`, an 8.8 KB single-cycle
*accumulator* CPU running `addi x1,x1,3` — impressive as a GUI-automation proof, but not RV32I.
So AC-2's "plus the RV32I showcase … in the set" has three bad resolutions and one good one:
(a) check in a generated multi-hundred-element circuit that silently drifts from its generator
and from `verify.py`'s reference-emulator oracle; (b) ship the accumulator CPU labelled RV32I,
which is untrue; (c) ship nothing and fail AC-2. The good one: **treat RV32I as PF-1/PF-4
material — an SVG render plus the "verified instruction-by-instruction against a reference
emulator" story in the README and gallery — and, if it must be openable, ship it as an artifact
regenerated by `riscv/` tooling in the build with the existing differential verification as its
gate.** A hand-copied blob under `resources/samples/` is a second source of truth for a circuit
that already has a rigorous one.

There is also an unexamined risk. ARCHITECTURE.md's recorded simulation-strategy decision names
its own revisit trigger: "a concrete CPU-scale design on the `riscv/` trajectory that is
unusably slow interactively." Putting a CPU one menu click from a first launch makes that
trigger part of the *on-ramp*: an example that opens and crawls is worse for CAP-27 than no
example. Any RV32I menu entry needs an interactive-responsiveness gate stated up front, or it
belongs behind a "advanced / this is big" separation.

## Duplication the issue is walking into

The jar already ships four stepped build-along pages: `src/jls/tutorial/tutorial{1..4}.html`,
titled Introduction / 4-Bit Counter / Full Adder / Sign Extension, with per-step JPEGs teaching
the student to *draw* a half-adder and compose it into a full adder. #73's proposed sample list
is full adder, N-bit counter, mux, subcircuit, FSM. #552 proposes to author stepped lessons for
the first three circuits of this corpus. So the trajectory is: prose lessons that describe
circuits which don't ship, plus circuits that ship without lessons, plus new lessons authored
beside both. The elegant move is to *connect* them — tutorial page N gets a "open this circuit"
action bound to corpus item N — which retro-fits value into an asset already in the jar,
collapses an existing duplication instead of adding one, and shrinks #552 to an extension of
working material rather than fresh authorship (its own KC-27-2 escape hatch then rarely fires).

## What I would keep exactly as written

The mechanism discipline is right and hard-won: one `resources/samples/` tree, classpath reads
only (#130 / `SeedDirectoryTest`), loading through the ordinary reader with no special path,
extend #381's set in place rather than forking a second mechanism, fresh authorship for
licensing per the 2026-07-17 decision. The dedup comment's boundary against #551/#552/#573 is
sound in spirit; my quarrel is only that #548 kept a surface for itself while asserting it owns
only the corpus.

## Summary of the alternative I am proposing

Rename the deliverable from "an Examples menu" to **"a curated, ordered, self-describing,
self-checking example corpus with one enumeration seam"**: circuits + a declared metadata index
+ per-example test vectors, ordered as a path, surfaced by PF-3's welcome pane and
`File → Open Example…` rather than a new top-level menu, with RV32I demoted to gallery/README
credibility material regenerated by `riscv/` tooling. I am explicitly setting aside AC-1's
"top-level menu bar entry" and AC-2's "RV32I in the in-tool set" — both are mechanisms
mistaken for outcomes, and both are cheaper and truer when re-cut this way. Everything else in
the issue survives the reframing intact.
