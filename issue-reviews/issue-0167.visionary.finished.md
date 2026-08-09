# Issue #167: Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the Stage-0b framing and #167 is one claim: *JLS should have a typed, total,
replayable description of what changes a circuit.* That claim is right and it is
load-bearing well beyond collaboration — `docs/grand-architecture.md` §3 names it one
of three triad members, #223 hangs `collab.op-observer` off it, #337/#412 want a
construction verb set that is the same vocabulary with a friendlier face, and precise
undo (#18's successor) is its most obvious first customer. I endorse the destination.

What I want to reframe is the *route*. The remaining scope (#282, #283, plus two
unfiled rows) is "walk the mouse state machine gesture by gesture until a table in
`docs/operation-layer.md` has no inline rows left." That route buys the weakest form
of the property it is after, and the codebase is already holding the materials for a
much stronger one.

## Three observations that undercut the current route

**1. The single entry point already existed; what is being bought is semantics, and
the semantics are mostly being discarded anyway.** The original audit found it
itself: site 16, `EditWindow.markChanged`, is "the funnel every site above reaches."
It still is — every one of the 13 mutation-marking sites in
`src/jls/edit/SimpleEditor.java` reaches it, and `OpSink.submit` (`SimpleEditor.java`
~5547) is literally `op.apply(circuit, getGraphics()); markChanged();`. The choke
point is free. What the vocabulary adds is typed intent — except that 7 of the 11
kinds do not carry intent. `AddElements`, `AddWire`, `RemoveElements` (inverse),
`RemoveWire` (inverse), `SetElementConfig`, and `AddWire.survivors` all carry
**save-format text blocks keyed by stable id**. `SetElementConfig`'s own javadoc
concedes why: "an element's whole reconfigured save block *is* the change unit."
The vocabulary has been converging, PR by PR, on *a keyed structural diff of the
canonical save*. Only `MoveElements(dx,dy)`, `RotateElement(cw)`, `FlipElement`,
`ToggleWatched`, and the probe pair are genuinely intentional — and those are the five
cheapest gestures, all already migrated.

**2. The completion criterion is a ledger, not a ratchet — which is off-key for this
project.** I1 is "the inventory table in `docs/operation-layer.md` contains no
'gesture still inline' row." Nothing in the build can fail when a new mutation site
appears. Compare how JLS enforces every *other* boundary it cares about:
`HeadlessCoreRatchetTest`, `ElementRegistryTest`'s totality check,
`NotificationRatchetTest` for `JOptionPane`, `ElementConstructorContractTest`,
`ExtensionPointCatalogTest` cross-checking both directions, and eleven ArchUnit rules
in `test/jls/ArchitectureRulesTest.java`. This project's whole idiom is "boundaries
are enforced, not aspirational" (grand-architecture §10, verbatim). The op layer is
the one triad member whose totality property is maintained by hand in a markdown
table. A future contributor adding an element dialog will not consult that table.

**3. The seam is fighting back, and there is physical evidence.** Two symptoms:
(a) PR #273's preview-then-commit for moves has the gesture mutate the circuit live,
then **restore the pre-drag positions** so the op can re-apply the same change
symbolically. Undoing a real mutation to redo it as a command is a design telling you
the seam is downstream of where the mutation actually happens. #282 proposes to
extend that dance to placement, wiring, and paste.
(b) `jls.collab.op` — the "headless by construction" layer — imports the *editor*
package: `AddElements`, `RotateElement`, `FlipElement`, and `SetElementConfig` all
call `jls.edit.SwingTextMetrics.forGraphics(g)`.
`ArchitectureRulesTest.collabLayersAreHeadless` does not catch it, because it bans
`javax.swing..` and `SwingTextMetrics` is AWT-only. So the collab layer depends on
`jls.edit` today, unnoticed, purely because ops apply through a `Graphics`. #337 owns
removing that parameter; the boundary comments defer it to *after* #282/#283, which
means every new plan builder is written against the wrong signature first.

## The reframing: brackets and a derived journal, enforced by a ratchet

The alternative the issue never considers is that **ops should be derived, not
authored**, with authored ops reserved for the few gestures where intent is real.

Concretely:

- **Gestures declare boundaries, not plans.** Replace `deleteSelectionPlan` /
  `moveSelectionPlan` / the four builders #282 would add with a transaction bracket:
  `try (var t = ops.gesture("paste")) { ...existing inline code, verbatim... }`.
  No preview-then-commit, no restore-then-reapply, no `@Nullable List` fallback
  protocol, no per-gesture surgery.
- **The op stream falls out of the canonical save at bracket close.** A keyed diff of
  the pre-bracket and post-bracket canonical text — grouped by stable id (#165),
  compared by the #166 oracle — yields exactly the block-carrying kinds the vocabulary
  already has: elements appeared (`AddElements`), disappeared (`RemoveElements`),
  changed in place (`SetElementConfig`), nets appeared/disappeared (`AddWire`/
  `RemoveWire`). Byte-exact inverses are free and total, because the diff *is* a pair
  of byte states.
- **The cost is already paid.** Every `markChanged` already calls `pushCopy()` →
  `CircuitSnapshot.capture(circuit)`, which serializes the whole circuit to canonical
  save text and deflates it (`src/jls/edit/CircuitSnapshot.java`). The pre- and
  post-images this design needs are the undo stack's existing top and the snapshot
  about to be pushed. There is no new serialization pass, only a diff over text JLS
  writes anyway.
- **Intent survives where it matters, as an annotation.** A gesture that knows it is a
  pure translate calls `t.intent(new MoveElements(ids, dx, dy))`; the diff verifies the
  annotation reproduces the observed bytes and prefers it. Rotate, flip, watch, and
  probe keep their kinds. This inverts the current partial function (op if expressible,
  inline fallback otherwise) into a total function with an optional refinement.
- **Totality becomes enforceable.** With a bracket in place, an ArchUnit rule in the
  existing suite — "nothing outside a gesture bracket calls `Circuit.markChanged`" —
  is writable, and a runtime assertion "no `markChanged` outside an open bracket"
  is writable today, before any of this. That is I1 as a ratchet instead of a table.

What this makes disappear: #282's four gesture reworks and its H2 wire-draw-cancel
question (a cancelled gesture's bracket closes with an empty diff, by construction);
#283 entirely (dialog commits mutate in place — the diff sees it); the ordered-rows
vocabulary Open Question (`EditOrderedRows` vs `SetElementConfig` block-replace is
moot when the answer is "whatever the bytes say"); the unfiled `ImportSubcircuit`
design; and the "documented inline fallback" sanctioned outcome in §7, which is the
current design admitting it may never be total.

## A second reframing: give the seam a live consumer now

`src/jls/boot/CollabModule.java` registers **nothing**: "JLS ships no built-in
`OpSink` observer today." Twenty-one source files, ~2,600 lines, PIT thresholds at
mutation 80, and the sole consumer is `SimpleEditor` submitting to itself. #167
explicitly defers precise undo to Stage 2, so the standalone-beneficiary argument in
§7's descope clause is currently vacuous.

The cheapest real consumer is also the one most aligned with what JLS actually is.
The README's strongest, most-used surface is the batch/grading interface
(`docs/batch-interface.md`, the container image, autograders). A `jls -diff a.jls
b.jls` that emits an op list — and its inverse, `jls -apply ops.txt circuit.jls` —
would (a) give instructors circuit-level diffs and student edit replays today,
(b) exercise every kind's serialization and inverse against real circuits far harder
than `CircuitOpTest` does, (c) be *the same diff engine* the reframing above needs,
and (d) validate the network grammar (#170) before a single byte crosses a socket.
That is a shipped feature for JLS's actual audience, paid for by infrastructure that
currently has none.

## What I am disregarding, and why

I am setting aside I1-as-written and the #282/#283 decomposition. They are a correct
plan for the wrong problem: they optimize "how many rows of a table say migrated"
when the property that matters is "no mutation can escape the seam, now or in five
years." A table can reach zero inline rows and the very next element dialog
reintroduces one silently. The bracket-plus-diff design reaches totality in one
change and keeps it under build enforcement.

I am *not* disregarding: the sealed vocabulary, the strict `CircuitOpReader`, the
`ElementBlocks`/`NetBlocks` transplant machinery, the byte-exact inverse property, or
the §4 invariants. All of that survives the reframing unchanged — the diff *produces*
those kinds; it does not replace them.

## Recommendation

1. Reorder against #337: land the `Graphics` → `TextMetrics` substitution (#382)
   **before** any further gesture work, and add an ArchUnit rule that
   `jls.collab..` outside `jls.collab.ui` may not depend on `jls.edit..` — the four
   `SwingTextMetrics` imports listed above are a live, uncaught layering inversion.
2. Prototype the bracket + canonical-save diff against the two gestures already
   migrated (delete, move) as a differential oracle: the derived op plan must equal
   `deleteSelectionPlan`'s / `moveSelectionPlan`'s authored plan. If it does, #282 and
   #283 collapse to bracket placement and both become one small PR instead of two
   state-machine reworks.
3. Convert I1 from an inventory table to a ratchet test, whatever the outcome of (2).
4. File the `-diff`/`-apply` batch consumer as a peer of #282 so the vocabulary earns
   its keep before #171 exists.
5. Only if (2) refutes — i.e. some gesture's post-state genuinely cannot be diffed
   into the vocabulary — fall back to the issue as written, with that refutation
   recorded as the justification the current plan currently lacks.
