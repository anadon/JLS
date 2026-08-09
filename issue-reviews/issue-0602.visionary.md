# Issue #602: TASK-C332-3: a cut net names the same signal on both sides, and that name does not depend on which partition it landed in
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and the claim is: *when a design stops being one file, a signal
must not stop being one signal.* That is correct, load-bearing, and the precondition
for everything #332 sells downstream — the equivalence harness (#606) compares two
runs only if the two runs agree on what a signal is, and #333's transport addresses
boundary traffic by something.

But the issue answers a stronger question than it asked. It proposes that each part
*derive* a name for a cut net from #336's synthesized naming function, and that the
two derivations then be checked for agreement at load. Deriving the same answer twice
and reconciling is the expensive route to identity. Naming once and referring is the
cheap one, and JLS's existing vocabulary already contains the naming mechanisms.

## Three things the issue does not account for

**1. The stated motivation is already false at HEAD.** The Outcome says "a watched
element is unnameable across a re-partition." Watched elements are not named by
synthesized net names anywhere in the shipping product. `docs/batch-interface.md`
§3.2 names them by the dotted subcircuit qualifier plus the element's own name
(`Output Pin QUAL.name: VALUE`), and §4.1 gives VCD one signal per watched element
plus one per *probed* net — a probe being a user-typed string saved on a `WireEnd`.
Element identity across a re-partition is already carried by `ElementId`
(`src/jls/elem/ElementId.java`, #165), which is minted once and survives save/load,
undo, and checkpoint recovery. Synthesized `net_<id>` names exist only inside HDL
export artifacts, whose audience is #336's, not #332's. So the failure this task
exists to prevent is not the one it describes; the real exposure is narrower and
sharper — the boundary description itself, and anything #333 later addresses across
it.

**2. AC-3 is not locally computable under AC-2's sibling.** #336 fixes
name(n) = `net_` δ(min⪯ {sid(e) : e ∈ drv(n)}) — a function of the net's *driver
set*. A cut net has drivers in some parts and none in others. TASK-C332-2 (#601)
requires that load hold one part resident and forbids any structure sized by the sum
of parts (#332 invariant 3). A part holding no driver of a cut net therefore cannot
evaluate #336's function at all without the driver set arriving from somewhere. The
case where this bites hardest is precisely the case partitioning is wanted for: a
tri-state bus split across parts has its drivers spread over several parts by
construction (`docs/simulation-semantics.md` §9; `WireNet.loadTriState` is already
threaded through `Circuit.finishLoad`). AC-3 and #332 invariant 3 are in direct
tension on the shape of design that motivates the feature.

**3. AC-4 institutionalizes the redundancy it then polices.** "A boundary description
whose two sides disagree on a name is refused at load, with both names and both
parts" is only reachable if both sides durably record a name. That is a derived value
stored in N places, and the acceptance criterion is a consistency check over the
denormalization. #336 explicitly promises the opposite — "Net names are derived at
emission time, never stored", "Tracks durably: nothing new in the save format" — so
#602 as written is the first place in the plan that breaks #336's own data contract,
while citing #336 as its authority.

## Reframing A — freeze identity at cut time; parts refer, they do not derive

Partitioning and loading are different events with different budgets. **Partitioning
happens once, at authoring time, with the whole design resident** — the author
declared the cuts, so the partitioner already holds `D` and can see every driver of
every cut net. **Loading happens many times and must stream.** The issue treats naming
as a load-time derivation and then has to defend it against the streaming bound.

Cut the seam at cut time instead:

- The **boundary description is the sole authority** for cut-net identity. It carries,
  per cut net, one record: a boundary handle, the name (computed once, by the
  partitioner, from whatever #336 settles on), the bit width, and the tri-state flag.
- **Parts do not name cut nets.** A part's boundary pin carries a handle into the
  boundary description and nothing else. There is no second derivation, so there is
  nothing to disagree.
- AC-1 becomes true by construction rather than by an equality test over the cut set.
- AC-4 degrades from a semantic reconciliation to referential integrity — "part 3
  references handle 17, the boundary description has no handle 17" — which is a
  cheaper diagnostic, is total rather than sampled, and is the same class of check
  the loader already performs when wire ends resolve refs in `finishLoad`.
- AC-2 (re-partition invariance) stops being a property to test and becomes a
  property of the *input*: the boundary description is regenerated from the same
  design, so the name moves only if a driver moved.
- The #601 streaming bound is unaffected: the boundary description was already
  admitted resident in #332's own bound `max_i M(D_i) + M(B) + c`.

This is the standard "make the illegal state unrepresentable" move, and it deletes
roughly half of this task's stated work.

## Reframing B — the cut is a hierarchy boundary, and JLS already has one

Worth an explicit decision on #332 rather than a note here. JLS ships a partitioning
concept: `SubCircuit` (`src/jls/elem/SubCircuit.java`), with an `inmap`/`outmap`
binding of the element's puts to the subcircuit's `InputPin`/`OutputPin` ports, and
`Circuit.save` already writes an imported subcircuit as its own self-contained
`CIRCUIT name … ENDCIRCUIT` block, nested but never repeated
(`src/jls/Circuit.java:1476-1484`). Hoisting those blocks to their own files is a
textual operation on a boundary the format already draws.

Under that framing:

- A **part is a subcircuit**; the **boundary description is the instance-port binding**
  JLS already serializes.
- A **cut net's identity is a port name** — a user-typed string, which
  `HdlExporter`'s own precedence already ranks above every synthesized name
  ("the input-pin or clock port name when the net is a port, else the (smallest) jump
  name, … else a synthesized `net_<id>`", javadoc at `src/jls/hdl/HdlExporter.java`
  ~:93-102) and which #336 invariant 6 promises is never rewritten.
- AC-1, AC-2 and AC-3 all collapse: a user string cannot disagree with itself, does
  not move when the cut moves, and needs no digest.
- The addressing story for #333 and the batch surface unify — the dotted qualifier of
  `docs/batch-interface.md` §3.2 *is* the cross-part address, already golden-tested.

Note the structural mismatch this exposes: #336 scopes hierarchy **out** explicitly
("This feature partitions one flat circuit"; hierarchical net structure belongs to
#358 FEAT-018). #602 therefore asks a deliberately flat-circuit naming feature to
solve a problem that is inherently about module interfaces. If FEAT-055's parts are
going to look like modules to #333 anyway, the dependency edge that matters is #358,
not #336.

## Reframing C — refuse an anonymous cut, adopt it regardless of A or B

#332's founding principle is "author-declared cuts first, because that makes the
mechanism testable without also solving partition quality." Apply the same principle
one level down: **naming a boundary is part of declaring one.** Refuse, at partition
time, any declared cut that passes through a net carrying no user-visible name — no
port, no jump alias, no probe. The author adds a `JumpStart`/probe and the cut
becomes legal. Cost to the author: one name per boundary signal, which they arguably
owe anyone who will later read a waveform of that boundary. Payoff: every cut net is
user-named, and the entire synthesized-naming question is out of the critical path.

This also merges cleanly with the sibling refusal task (#604) — one partition-time
diagnostic path, two rules ("this cut crosses a combinational cycle", "this cut
crosses an unnamed net") — rather than a separate load-time refusal in this task.

## Trajectory

The outcome strengthens the arc; the route pulls against three parts of it — #336's
"never stored" data contract, #601's no-whole-design-index invariant, and the
project's consistent preference for user-supplied names over synthesized ones. It
also duplicates, in a new naming scheme, an identity mechanism (`ElementId`, ports,
jump aliases, probes) that already ships and is already what every observable surface
keys on.

## Disregarded criteria, and what replaces them

I am explicitly disregarding **AC-3** and **AC-4** as written. AC-3 mandates a
derivation that a part cannot perform under #601's bound; AC-4 mandates a
reconciliation that only exists because AC-3 mandated two derivations.

Proposed replacements:

- **AC-1′** Each cut net appears exactly once in the boundary description, and every
  part references it by handle only. Asserted by a structural test: no part file
  contains a name for a cut net.
- **AC-2′** Re-partitioning the same design along a different cut produces boundary
  records whose names are unchanged for every surviving net.
- **AC-3′** The name in a boundary record is a user-supplied name (port, jump alias,
  or probe) where one exists; a declared cut through a net with no user name is
  refused at partition time, naming the net's endpoints and the two parts.
- **AC-4′** A part referencing a handle absent from the boundary description is
  refused at load, naming the part and the handle.

With those, this task shrinks to an invariant plus one refusal riding on
TASK-C332-1's artifact form (#600), the 2-3 mw band should come down, and the
`ordering_after: 336` edge can be dropped — which also unblocks this work from
#336's four open questions, two of which are currently marked as blocking the filing
of its own children.
