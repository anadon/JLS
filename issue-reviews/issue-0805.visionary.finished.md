# Issue #805: TASK-C593-2: group drag, align, distribute and duplicate — each preserving connectivity or refusing with a named reason
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated outcome is switcher parity (CAP-37 via #593/#592). The stated *hard* property
is narrower and more interesting: a group manipulation must never silently disconnect
anything. That property is not a GUI property. It is a statement about the circuit model —
"this mutation preserves the net partition, or it is refused with a name" — and JLS already
built the machine that states exactly that, one layer down, for a different reason.

`jls.collab.op` (#167, `docs/operation-layer.md`) is a closed, sealed vocabulary of
data-only records where every op validates atomically, mutates only if the whole thing is
valid, is byte-exactly invertible, and refuses by throwing `OpRejected(message)` — a named
reason. `RemoveWire`/`AddWire` already reason at *net* granularity, including recomputing
survivor components. `SimpleEditor.deleteSelectionPlan` and `moveSelectionPlan` are already
static, Swing-free planners that map a selection to an op plan. #805 proposes to re-derive
connectivity preservation four more times, per gesture, in yet-to-exist collaborators,
behind a four-deep dependency chain. That is the wrong seam.

## The reframing

**1. Make "preserves connectivity or refuses by name" one mechanism, not four promises.**
JLS has a canonical byte-exact save (#166) and a net serializer (`NetBlocks`). A
connectivity fingerprint — the multiset of nets, each as its sorted set of attached
`(ElementId, put)` anchors — is a dozen lines over existing machinery. Then define one
transactional wrapper: plan → snapshot fingerprint → apply batch → re-fingerprint → if the
partition changed and the gesture did not declare the change, roll back and report the
first differing net by name. Group drag, align, distribute, duplicate, paste, group rotate,
group flip, breadboard placement (#329), netlist import (`jls.hdl.imp`) all inherit it.
Better still, the property becomes *property-testable*: fuzz random selections × random
deltas against the invariant, rather than four hand-written tests pinning four gestures.
AC-1..3 as written invite four bespoke guards that will drift apart; this makes them one
oracle that cannot drift.

**2. The op vocabulary gap the issue never names.** `MoveElements(ids, dx, dy)` is one delta
over many ids. Align and distribute are *per-element* deltas and cannot be expressed in the
vocabulary at all. This is a design decision on a sealed interface (`CircuitOp permits …`),
the `OP <kind>` save grammar, `CircuitOpReader`, and the future collab wire surface (#163,
#170) — it must be made once, at feature level, not discovered inside a task. The elegant
move is to generalize rather than add kinds: `MoveElements(List<Move>)` where
`Move = (ElementId, dx, dy)`, with today's single-delta form as the degenerate case. Then
**group drag, align, distribute, keyboard nudge and snap-to-grid are all one op kind**, and
duplicate is `AddElements` + `AddWire` — the two kinds paste already uses. Four behaviours
collapse to one generalized op plus one new planner each. That is the version of this task
that makes the problem disappear.

**3. `submitAll` is not a transaction, and this task is where that bites.** The editor's
override (`SimpleEditor.java:5563-5570`) applies each op in order and calls `markChanged()`
*after* the loop. `OpSink`'s own javadoc admits it: "ops before it in the list have been
applied and recorded, ops after it have not been touched." So a batch that rejects halfway
leaves the circuit partially mutated with no undo snapshot pushed. Every gesture in #805
except a pure single-delta drag is inherently multi-op. Shipping align/distribute/duplicate
on top of a non-atomic batch would manufacture the exact failure mode the issue exists to
make impossible — silent partial mutation, unrecoverable by undo. **Batch atomicity is a
prerequisite, and it belongs in the op layer where it serves everything, not in a GUI
collaborator where it serves four gestures.** File it; block #805 on it instead of on #316.

**4. Invert the #316 gate.** AC-5 says nothing lands in `SimpleEditor`, and if #316 stalls
this task waits. #316 is `blocked_by: [317, 337]`, its TASK-0019/TASK-0020 are "not filed",
and its own body records `SimpleEditor` growing 4,119 → 5,852 lines *while five extractions
succeeded*. Waiting is not a neutral act here. But the premise is wrong: the connectivity
work is not mouse-machine work. `moveSelectionPlan`/`deleteSelectionPlan` are already
static and Swing-free — they are trapped inside `SimpleEditor` only by file placement. Move
them, plus the new planners and the fingerprint invariant, into a fresh headless package
(`jls.edit.plan`, or `jls.core.edit` if the AWT-free rule is wanted), exactly the pattern
`UndoManager` established under #84/U7 with an injected `Restorer`. That **removes** several
hundred lines from `SimpleEditor` and lands a real slice of #316's decomposition as a side
effect — it is pro-decomposition, not a KC-37-1 violation, and it needs neither #337's
`Graphics`-removal nor #441's `MouseMachine`. Only the gesture→planner binding needs the
interaction machine, and that half is small. Split the task on that seam and the four-deep
block chain mostly evaporates.

**5. Duplicate should not be a fifth code path.** `EditOp` already has CUT/COPY/PASTE,
`Element.copy()` exists, and `AddElements` already carries paste's exact validation rules
(fresh stable ids, name collisions, jump-start/jump-end sourcing). AC-3's "no accidental
connection to the original" *is* a paste placement rule. If duplicate gets its own clone
semantics, JLS will have two clone paths that disagree within a year. Define duplicate as
paste-at-offset over the same planner, and fix the placement rule once for both.

**6. Align already exists in this repo, with a different rule.** `StateMachineDialog`
(`:609-660`) aligns selected states horizontally/vertically by *averaging* their
coordinates. Most incumbents align to a bounding-box edge or the anchor element. Landing a
second, differently-behaved align on the main canvas is a parity defect the catalog will
not catch, because #592 scores JLS against Logisim/Digital/CircuitVerse, not against
itself. AC-2's "documented reference rules" should be one rule, stated once, and the state
machine editor should be brought onto it (or its divergence recorded as deliberate).

## The out-of-the-box alternative worth catalogueing

Generic align/distribute is a *drawing-tool* affordance. In a schematic the user's actual
goal is "make it tidy without breaking it", and tidiness in a schematic means **straight
wires**, not equal spacing. JLS is unusually well placed to serve that directly: it has a
net model, put coordinates, and `removeCoLinear()` already. **Align-to-net** — snap the
selection so that connected puts become colinear, straightening the wires between them —
is a gesture no incumbent offers, it is strictly more useful than "align left", and it
*improves* connectivity quality rather than merely promising not to damage it. I am not
proposing it as a replacement for parity align (CAP-37's arc is explicitly parity, and a
switcher will look for the familiar buttons first), but as a row #592's catalog should
carry as a differentiator once the generalized move op exists — at which point it is a
different planner over the same op and costs almost nothing.

## Where I disregard the stated acceptance criteria

- **AC-5's "if #316 stalls, this task waits"**: reject as written. The connectivity
  half is headless model work that pays #316 down; only the gesture binding waits.
  Reformulate as "nothing lands *in* `SimpleEditor`; the planners land in a new headless
  package and take existing planner code with them."
- **AC-1..3's per-behaviour connectivity promises**: replace with one invariant plus one
  transactional applier, and per-behaviour tests that assert *the invariant holds*, not
  that each gesture independently reimplemented it.
- **Add an AC** the issue is missing: the op vocabulary change (per-element deltas) is
  designed and landed at feature level, with its reader/grammar/ratchet consequences, before
  either task builds on it.

## Verdict

**endorse-with-reframing.** The capability is right and squarely on the project's arc —
switcher parity gated by a scored catalog, with the connectivity property as the thing that
makes it more than cosmetics. The route is wrong in a way that matters: it re-derives, in a
GUI layer that does not exist yet, a guarantee the op layer was built to give; it needs a
vocabulary generalization it never names; it sits on a batch mechanism that is not atomic;
and it gates itself on a decomposition it could instead advance. Reframed around
`MoveElements(List<Move>)`, a transactional `submitAll`, a net-partition fingerprint, and a
headless planner package, this task gets smaller, unblocks itself, and leaves behind a
mechanism that every future group manipulation in JLS inherits for free.
