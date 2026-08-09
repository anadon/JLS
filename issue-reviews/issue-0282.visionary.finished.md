# Issue #282: Editor gestures: migrate placement, wiring, and paste commits behind the OpSink seam via preview-then-commit (op layer #167)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is not "four gestures stop calling `markChanged` directly." The end, stated
plainly in #167 §1 and in `docs/operation-layer.md` line 3-14, is a **total** seam:
*every* mutation observable as a validated, invertible, serializable command, so that
#171 can replicate it, Stage 2 can invert it precisely, and #223/#224 can hang an
observer on it. Totality is the whole value. A seam that sees 70% of mutations is not
70% of a collaboration substrate; it is zero of one, because the peer that misses a
wire merge diverges exactly as badly as the peer that misses everything.

Judged against that end, the method this issue prescribes cannot get there, and the
evidence is already on master.

## The structural problem with preview-then-commit plan builders

`moveSelectionPlan` (`src/jls/edit/SimpleEditor.java:1053-1116`) is the pattern #282
proposes to replicate four more times. Read what it actually does: it returns `null` if
*any* selected element has an attached put, and `null` if any selected put's post-move
position coincides with a non-selected end or put. In a circuit under construction —
which is every circuit a student edits after the first thirty seconds — gates being
dragged are wired. **The op path fires only for relocations of unwired elements.** The
inventory row reads "migrated"; the seam sees the degenerate case.

Two costs follow, and they compound with each gesture #282 adds:

1. **Each plan builder is a predictive model of the mutator it replaces.** To prove "this
   drop forms no connection," `moveSelectionPlan` re-implements `connect()`'s
   neighbourhood query — `getIndexBounds().grow(SPACING)`, then the coincidence scan —
   in a second place, with the comment "Mirror `connect()`'s neighbourhood query."
   Placement, wiring, and paste each need their own mirror of `fixPosition` +
   `connect()` + `removeCoLinear()`. Those mirrors must track the originals forever, and
   nothing in the build enforces that they do. This is duplication of the most
   safety-critical geometry logic in the editor, added deliberately, four more times.
2. **The fallback is permanent, and the issue sanctions it.** §10 makes "documented
   inline fallback" the sanctioned outcome; Open Questions recommends fallback-first for
   connect-forming drops. #167's I1 (no un-owned inline row) then becomes unreachable by
   construction, not by accident. The program's own closure criterion is being traded
   away in its largest task.

The cost is not small either: `DeleteGestureTest` + `MoveGestureTest` are 1,199 lines for
two gestures. #282 is four-plus gestures of the same shape.

## Reframing 1 (primary): derive the plan by differencing, not by predicting

Everything needed to make plan-building **observational instead of predictive** already
exists on master, and #282 never considers it.

- `CircuitSnapshot.capture` already serializes the whole circuit into canonical save text
  on *every* gesture (`SimpleEditor:5497` → `markChanged` → snapshot). The "before" state
  is already computed and paid for. There is no new per-gesture cost to pay.
- #165 gives stable ids that survive save/load (`StableElementIdTest#undoRestorePreservesIds`),
  so a diff keyed on stable id distinguishes *moved* from *removed-and-re-added* — the
  one thing a naive diff would lose is the one thing the project already fixed.
- `ElementBlocks` and `NetBlocks` already emit exactly the per-element and per-net
  canonical bytes such a diff would compare. The diff's alphabet is implemented, tested,
  and under blocking PIT thresholds (PR #263).

Concretely, replace the four plan builders with one gesture wrapper:

```
commitGesture(() -> { ...existing inline commit, verbatim... })
```

which (a) holds the pre-gesture element-block map and net-block set, (b) runs the inline
mutation unchanged, (c) classifies the delta into the existing vocabulary — id absent→present
= `AddElements`; present→absent = `RemoveElements`; same id, uniform geometry delta across a
group = `MoveElements`; same id, other block change = `SetElementConfig`; net set delta =
`RemoveWire`/`AddWire` — (d) publishes that plan to the sink's observers, (e) `markChanged()`
once.

The properties this buys are strictly stronger than §5's predictions:

- **P1 becomes a mechanism-level invariant, not four hand-written parity tests.** Verify by
  replaying the derived plan against the previous snapshot (restore it, apply, canonical-save)
  and comparing bytes with the live circuit. That is the #166 oracle already in place, run as
  a property over *every* gesture the suite exercises, including gestures nobody wrote a
  parity test for.
- **Coverage is total, and the fallback is loud instead of permanent.** A gesture the
  vocabulary cannot express fails the replay check and is reported as a bug, rather than
  silently returning `null` and settling into the inventory as a sanctioned apology.
- **Connect-forming drops come free.** The composite plan §8 defers to "a later slice"
  (`MoveElements` + `AddWire`/`RemoveWire`) is just what the diff yields when a drag
  re-forms a net. The Open Question dissolves.
- **#283, ordered rows, and subcircuit import ride the same mechanism.** Dialog commits are
  a block change on one id — that is `SetElementConfig` by observation, with no commit hook
  threaded through five dialog classes. #167's two "planned" tasks stop needing bespoke
  designs; only the *vocabulary* question (`EditOrderedRows` vs block replace) remains, and
  that is a Stage 2 merge-semantics decision, exactly where #167 already puts it.

The honest cost of this reframing: the seam changes character from **op-as-command**
(`submit` = validate → apply → record) to **op-as-journal** (the mutation happened; these ops
describe it faithfully, proven by replay). For replication that is sufficient and arguably
better — peers apply the journal, and remote application still runs the full `apply`
validation path, so hostile-input strictness is untouched. For Stage 2 precise undo it is
also sufficient: a byte-exact inverse of a faithful description inverts the real change.
What it gives up is the *compile-time* story "mutation can only happen through ops." That
story is not true today anyway (14 `markChanged` sites, a manual ledger in a markdown table),
and Reframing 3 below buys it back properly.

## Reframing 2 (complementary): the circuit should never hold uncommitted geometry

H2 — "the two wire-draw-cancel `markChanged` sites are pure compensation" — is treating a
symptom. Those sites exist because the in-progress wire is *added to the circuit* while the
user is still drawing it (`SimpleEditor:2814-2825` removes it again on cancel), and the
matching-JumpEnd path does the same (`circuit.addElement(nel); ... markChanged();` at
:2553-2561, with the element left mouse-attached in `chosen` state). The document is being
used as the scratchpad.

The elegant cut is to give gestures a **ghost layer**: in-progress geometry lives in an
editor-local overlay that `paintComponent` draws and the model never sees. Then cancel is
free *by construction* rather than by compensation, commit is a pure construction from the
overlay (no restore-then-reapply double mutation), and H2 is not a hypothesis to falsify but
a theorem. It also removes a real hazard the issue does not name: with the element live in
the circuit mid-gesture, an autosave checkpoint or a concurrent remote op observes a
half-drawn wire.

This seam is also the one #84 should be cutting, and it pays forward into collaboration:
the overlay is precisely the thing you would broadcast as *ephemeral presence* ("Sam is
drawing a wire here") without polluting the replicated document — a feature #163 will want
and currently has no place to put.

## Reframing 3 (noted): make the ledger a ratchet

`docs/operation-layer.md`'s inventory is a hand-maintained table. This repo has a better
idiom for exactly this problem — `HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`ArchitectureRulesTest.collabLayersAreHeadless`. Whatever seam wins, close it with a
ratchet test that fails when a new mutation site appears outside the sanctioned path.
Without one, I1 is asserted once at close and rots on the next PR.

## Alignment with the larger arc

One thing worth saying plainly. #282 states its own user impact as "Students editing
circuits experience no change," and it sits two levels below a P2P collaboration program
whose `net/`, `crdt/`, and `session/` skeletons already exist, in a single-maintainer
classroom tool whose README trajectory is packaging, HDL export, and RISC-V. That does not
make the op layer wrong — the op-observer seam serves undo and #224 standalone, as #167's
re-planning protocol already anticipates. It does mean the *cost* of this task is the whole
argument, and the version in front of us is the expensive one: four more hand-audited
mirrors of `connect()`, ~2,000 lines of gesture tests, and a coverage ceiling that leaves
the seam untrustworthy for the consumer it was built for.

## What I would change, explicitly

I am disregarding §8's per-gesture checklist and the Open Questions' "fallback first"
default, and with them P1/P2/P3 as per-gesture predictions. In their place:

1. Prototype the differencing recorder against the **already-migrated** delete and move
   gestures first. Success criterion: the derived plan is byte-equal to what
   `deleteSelectionPlan`/`moveSelectionPlan` produce, on the existing 1,199 lines of tests,
   *including* the drops where `moveSelectionPlan` currently returns `null`. That is a
   cheap, falsifiable, one-PR experiment against a known-good oracle, and it either kills
   this reframing outright or retires two plan builders and unblocks everything else.
2. If it holds, migrate the #282 gestures by wrapping their existing commits — no plan
   builders written, no `connect()` logic duplicated — and let the replay check be the
   parity evidence.
3. File the ghost-layer change as the real owner of H2 and of the wire-draw cancel rows,
   scoped with #84.
4. Add the mutation-site ratchet test before ticking I1.

If step 1 fails, the failure is itself the most valuable output #167 could get: it names the
exact mutation the canonical form cannot express, which is a vocabulary gap, not a gesture
gap — and that is a much better thing to learn now than after four bespoke plan builders
have been written and must be maintained.
