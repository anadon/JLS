# Issue #283: Dialog commits: route quick-edit and element edit dialogs through SetElementConfig behind the OpSink seam (op layer #167)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Not "make three dialogs call a different method." The end this serves is #163:
every edit that changes a circuit must be expressible as a data-only op so it
can be replicated, attributed, and precisely undone. `docs/operation-layer.md`
states it plainly — the vocabulary *is* the replication protocol. A gesture that
does not travel as an op is a gesture collaboration cannot see.

Judged against that end, the issue as written would land its checkboxes and
still leave the goal unreached. Three structural reasons follow.

## 1. The recommended default makes the deliverable inert

I am explicitly disregarding the Open Questions default ("keep the rejection and
inline fallback in this task; revisit with evidence"). That default is not a
conservative choice — it is the choice that empties the task.

`SetElementConfig.requireUnwired` (`src/jls/collab/op/SetElementConfig.java`
L174-L189) rejects any element with a wire on any put. Now look at what §8 says
to migrate first: `quickReset`, whose only real caller is
`ElementQuickMenus.apply` (`src/jls/edit/ElementQuickMenus.java:83`) — the
Constant 0 / 1 / +1 / −1 quick menu. A Constant exists to drive something. Its
output is wired in every circuit a student ever draws. The op path is rejected
100% of the time; the inline fallback runs; the inventory row flips to
**migrated**; nothing replicates.

The same holds for Clock (output wired), Memory (address and data buses wired),
Register. The set of elements a user opens a dialog on *and* which have no wire
attached is essentially "elements placed thirty seconds ago." Shipping this task
under the recommended default buys a fallback path, a parity test suite, and two
documentation rows that overstate reality — and it burns the "dialog commits are
migrated" line item that #167 will later need to spend again.

The evidence the Open Questions asks for already exists and is above. Do not
defer that decision; it is the decision.

## 2. The rejection is a symptom of an identity model, not a hard constraint

`apply` (L55-L74) reconfigures by `old.remove(circuit)` → `ElementBlocks.load` →
`init` → `addElement`. It mints a new `Element` with new `Put` objects, so every
`WireEnd` pointing at the old puts is orphaned — hence the rejection. The
constraint is not "wired elements cannot be reconfigured"; it is "this op has no
way to name a put across a replacement."

Two routes out, both better than the fallback:

**(a) Stable put addressing — the one I would build.** #165 gave elements stable
ids. Puts have names already (`new Output("output", this, …)`, Constant.init
L89-L91). Extend the stable-id idea one level: a put is addressed
`(elementStableId, putName, index)`. Then `SetElementConfig` on a wired element
becomes: detach ends by put address, replace, rebind by the same address; reject
only when the reconfigured block's *put set* differs (a bit-width change that
alters arity), in which case compose `RemoveWire` / `SetElementConfig` /
`AddWire` as H2 imagines. Inversion stays byte-exact because the rebinding is
computed from addresses, not object identity.

This is not extra scope smuggled in — it is the missing primitive that also
blocks the two gestures `docs/operation-layer.md` §"What lands next" item 1 lists
as *next*: placement's `connect()` and the wire-attach finish. Every one of those
is stuck on "wires refer to puts by object reference." Build put addressing once
and three deferred rows unblock; keep deferring it and each dependent task
re-litigates the same fallback.

**(b) In-place parameter reapply.** Apply the block's `setValue` lines to the
living element instead of replacing it, so puts survive untouched. Cheaper, but
`init` *adds* puts (`Constant.init` L88-L91, `Clock.init` L139+), so it is not
re-entrant; you would need a geometry-only reinit across ~30 element classes.
Worth measuring, but (a) is the durable seam.

## 3. The migration site list comes from a method with a proven blind spot

§8 lists `quickReset`, `ClockDialog`, `ConstantDialog`, `MemoryDialog`, then
"the remaining element edit dialogs." Two problems.

**The N-site plan duplicates work #77 already finished.** There is exactly one
funnel: `ElementDialogs.change` (`src/jls/edit/ElementDialogs.java` L67-L86),
called from `SimpleEditor.doModify` (`src/jls/edit/SimpleEditor.java:5200`). The
"bypasses the editor funnel" framing in §2 obs 2 is inaccurate — Clock, Constant
and Memory bypass `SimpleEditor.markChanged`, but all three still route through
`ElementDialogs.change`. `BuiltinElementRenderers` registers exactly eight change
dialogs (L44, 49, 56, 70, 75, 80, 114, 121); three are ordered-row and out of
scope, leaving **five**: Clock, Constant, Memory, Register, Text. Wrapping the
funnel plus `quickReset` is *two* call sites covering all six gestures, versus
five dialog-class edits that will drift. Cut at the seam the project just built.

**The `markChanged`-grep inventory misses real mutations.** `doTiming`
(`SimpleEditor.java:5258-5273`) opens `DelayChangeDialog`, which calls
`element.setDelay(temp)` (`DelayChangeDialog.java:86`) and marks *nothing* —
neither `Circuit.markChanged` nor the editor's. A propagation-delay change
therefore alters the save block, is not undoable, and does not even set the dirty
flag. `TextDialog` has no `markChanged` either. Neither appears in
`docs/operation-layer.md`'s inventory table, because that table was derived from
"every `markChanged()` call site" — a method that structurally cannot see a
mutation that forgot to mark.

The invariant worth wanting is not "every `markChanged` maps to an op kind" but
**"every path that changes an element's canonical bytes goes through `OpSink`."**
That is testable directly, and it is the test I would make the deliverable: drive
each registered change dialog and quick-menu action headlessly against the #166
canonical oracle, assert bytes-changed ⟹ an op was observed at the sink. It finds
`doTiming` on day one and it stays true for dialogs not yet written. Replace §5
P1/P2's per-dialog parity cases with this one property plus the undo case.

## Also worth naming: the restore dance, and a latent undo bug

H1's "serialize pre, let the dialog mutate, serialize post, restore pre, submit
post" runs the element through three states per commit and is not safe as
specified. `MemoryDialog.validateAndAccept` (L355-L357) mutates the circuit's
*name registry* — `removeName(old)` / `addName(new)` — outside the element's save
block. Restoring the pre-block does not undo that, so the restore step leaves the
registry describing a name the element no longer has, and
`SetElementConfig.requireNameFree` (L203-L217) then evaluates against corrupted
state. §11 does not list this.

The seam that makes it vanish: **dialogs should never touch the live element.**
`ElementBlocks` already loads a block against a scratch `Circuit`. Run the dialog
against that detached copy, and commit = serialize the copy → one op. No pre-
capture, no restore, no half-mutated live element on `OpRejected`, and cancel
becomes trivially free rather than "byte-identical snapshots drop out." It also
kills a class of bug the current design tolerates by luck. The cost is that
dialogs needing circuit context (name uniqueness, `resizeToFit`) must take it as
a parameter — a small, mechanical change across five classes, and the right
shape for #84's editor decomposition anyway.

Separately: because `ElementDialogs.change`'s callers never invoke the editor's
`markChanged` (`SimpleEditor.java:5497`, the only path that calls `pushCopy`),
and the dialogs call `Circuit.markChanged` (dirty flag only, `Circuit.java:293`),
**dialog edits appear to take no undo snapshot at all today**. P2 would fix that
silently. It deserves its own line — as a bug this task closes, with a test — not
a side effect of a plumbing change.

## The plan I would land instead

1. Put addressing + wire rebinding in `SetElementConfig`; delete
   `requireUnwired` for put-set-preserving reconfigures. This is the task.
2. Wrap `ElementDialogs.change` and `quickReset` — two sites, all six gestures.
3. Dialogs edit a detached copy; commit serializes it. Drop the restore dance.
4. One property test at the sink (bytes changed ⟹ op observed), replacing the
   per-dialog parity matrix; it immediately catches `doTiming` and `TextDialog`.
5. Update the inventory rows *and* add the missing ones; report the undo gap to
   #167 as a contract deviation its plan must reconcile.

If step 1 is judged too large for one task, split it out and block #283 on it —
but do not land #283 without it. The alternative is a "migrated" row that is
true of the code and false of every real circuit.
