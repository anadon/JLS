# Issue #472: TASK-0035: a net, a group and a nested instance can be named — one addressing key that stays unique when a subcircuit definition stops being copied per instance
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the claim is: *JLS's things need names that a second
process, a second machine, and a second view can agree on.* That is the right
claim and it is on the project's main arc. `docs/grand-architecture.md` §2 funds
three trajectories (CPU teaching tool, FPGA bridge, P2P collaborative editor)
and §3 names the op layer as the mutation seam all of them observe. An op whose
subject cannot be named is not an op. FEAT-014 (#318) is the addressing residual
after #165/#166 shipped element identity, and #472 is its naming half. Endorsed
at that altitude, without reservation.

The reservations are about the route, and there are four. Three of them make
work disappear rather than add it.

## 1. The view does not belong in the key

`view:instancePath:sid` puts a view token in every address in the design, and
then Open Question 1 — "what is the closed view-token set?" — is declared
execution-blocking, with a coupling to #383 and to FEAT-008's palettes so tight
that the issue has to write "the two must not mint two vocabularies."

But a view is a property of the *table*, not of the subject. An element inside a
subcircuit instance is the same element in the schematic, on the breadboard, and
on the analog canvas; only its **geometry** differs per view, and #383's
deliverable is already *a section per view* whose header names the view. Row keys
inside a `VIEW schematic` section do not need to repeat `schematic:`. Meanwhile
every non-geometric consumer the issue lists — `Ops.resolve`, probe binding,
elaborator diagnostics, package binding — carries a constant `schematic:` prefix
forever and must decide, at every equality comparison, whether views matter.

Drop the view field and:

- Open Question 1 dissolves. There is no vocabulary in the key, so there cannot
  be two of them; §4 invariant 6 of #318 is satisfied by construction.
- H4 and P4 stop being compatibility clauses. With the grammar
  `key = [ sid "/" ]* sid` — the instance path and the addressed sid as one
  `/`-separated chain, last component wins — the flat address of an element *is
  its bare `sid`*, character for character. Not "compares equal to"; identical.
  Every existing string that names an element stays a legal key.
- The delimiter collision goes away. As specified, `:` separates the key's three
  fields while `sid` is itself `replica:counter` and path components are sids
  too, so `schematic::legacy:3` and `a:1/c:3:b:2` are parseable only by counting
  colons from both ends. This is a normative grammar third-party tools must
  implement (§7.1). `/` for the chain has no such hazard, and the empty path is
  the absence of a separator rather than a doubled one.

## 2. Net identity is under-specified where it is hardest: merge and split

Nets are not durable artefacts. They are connected components, and the two
things users actually do to them are **merge** (`WireNet.absorb`,
`src/jls/elem/WireNet.java:251` — drawing one wire joins two nets) and **split**
(`WireNet.makeNet`, `:97` — cutting one wire makes two). §7.10 has five stages
and none of them is merge or split. §10's falsification criteria don't touch it.
P6 tests save/load, P9 tests an *unrelated* insertion. The one operation that
makes minted net identity hard is not in the design.

Minting does not answer it. When A absorbs B, one of two minted ids must die,
and every op, probe and geometry row bound to the loser silently retargets —
exactly the failure H2 invokes against derived ids, now caused by the cure. When
a net splits, one half keeps the id by fiat and the other gets a fresh one; there
is no content-determined way to pick the survivor, so the answer becomes a
function of edit history — which #318 §2 already rejected under
"mint net ids lazily" for breaking the #166 canonical-save property.

So H2's asymmetry with H5 is the seam, and I think it is cut on the wrong side.
The issue derives group ids (min member net id) because "a group is not an
addressable durable thing — it is the equivalence class." A net is *also* an
equivalence class. What differs is only that its members happen to be persisted
and a group's don't.

## 3. The reframing: name nets by witness, persist nothing

Concretely, and disregarding §7.7, §7.12 and the `nid` bullet of §14 for the
reasons below:

- **`ItemKey` = (instancePath, sid), and it addresses elements only.** Pure,
  format-free, no blockers.
- **A net is addressed by any member's sid** — `net(endSid)`, resolved as "the
  net containing this end". Its *canonical* form for sorting and for table keys
  is the minimum member end sid, which is precisely the derivation §7.10 stage 3
  already accepts for groups. One rule, applied consistently to both equivalence
  classes, instead of two rules whose asymmetry the issue itself flags in a note.
- **No `nid` attribute. No format change at all.**

What this buys, all of it deleted work: no new saved attribute; no golden
regeneration; no format-epoch negotiation with #436/#437 (Open Question 2, also
declared execution-blocking); no pre-`nid` fixture; no legacy net minting pass
(§7.10 stage 4); no hostile-`nid` diagnostics (§7.11 rows 1 and 4); no dependency
on the `ElementId.parse` counter guard; and Threat T1 — "the golden regeneration
hides a second change", correctly identified as the worst risk here — is not
mitigated but removed.

Witness addressing is also *more* honest than a minted id under merge and split,
which is the case that decides this. After a merge, two witnesses name one net;
after a split, each witness names its own half. That is what a user means. A
single minted id cannot express either. For CRDT convergence (#171/#279/#280),
two replicas naming one net through different witnesses is a resolve-time union
of two addresses onto one subject, not a data-model problem.

The honest cost: deleting the witness end orphans the binding. Two answers.
First, deleting the end a probe sits on already deletes the probe. Second, and
more important — see §4 — the bindings that matter today do not bind to nets at
all.

## 4. The identity that is actually broken today, and this issue does not mention

`src/jls/elem/Wire.java:120-126` — "Wires don't get saved" — and `Wire extends
Element` (`:16`) carries a `stableId` minted at construction
(`src/jls/elem/Element.java:24`). Wires are rebuilt at load by
`WireEnd.init` (`new Wire(this,end)`, `src/jls/elem/WireEnd.java:146`), so every
wire gets a **fresh sid on every load**. And `AttachProbe`
(`src/jls/collab/op/AttachProbe.java:18`) addresses its subject by exactly that
sid.

So the op layer's most user-visible durable binding — a named probe on a wire —
is already identity-broken across save/load, at master, today. #472 proposes a
new persisted identity for a *derived* object while the *drawn* object that op
records already reference re-mints its identity on every reopen. That inversion
is the strongest argument for the reframing: put persistence where the artefact
is (a wire's sid, which needs one line in `WireEnd.save`'s existing per-end block
and no new concept), and derive names for the equivalence classes above it.

## 5. Split the task at the cost seam, and check the "one key" claim

As filed, the free half waits on the expensive half. `ItemKey`, `Ops.resolve`
descent into `SubCircuit.getSubCircuit()`, and moving the load-time uniqueness
check to `(instancePath, sid)` require **no format change and no blocker** — the
instance path is derived from the in-memory nesting chain, and the stricter check
reads data that is already there. That half unblocks #383 and #167 immediately.
Everything expensive and contested is on the `nid` side, and it is the side
blocked on #468 (a five-site refactor) and gated on two "blocks execution" open
questions. File them separately.

Finally, one claim to test before anything is built on it. §"Intended Audience"
says "the key is the thing every later table is keyed by." Under #447's shared
definitions that is probably false for geometry: a gate inside a *shared*
definition has one position belonging to the definition, while a probe on that
gate inside instance I₁ belongs to the instance. Geometry wants
`(definition, sid)`; probes want `(instancePath, sid)`. Two scopes, one grammar.
If #383's table is keyed by the instance-scoped form, moving a gate inside a
shared subcircuit will need to propagate to every instance — or it won't, and
sharing will be silently broken. Worth deciding in #318 now, while the key costs
nothing to change.

## What I would keep unchanged

The instance path derived rather than persisted; the empty path as the flat case;
`jls.core` as the home (consistent with the existing `jls.core` geometry types
and with grand-architecture §3's headless kernel); refusing a compatibility
`resolve(Circuit, ElementId)` overload so the compiler finds every call site;
and Threat T3's hard line against a second coordinate pair on `Element`. Also
correct, and worth stating: the evidence-pin comment is not cosmetic. The
`ElementId.parse` counter guard quoted in O5 does not exist on master — `parse`
ends at `return new ElementId(replica, counter)`
(`src/jls/elem/ElementId.java:268`) — so #491 is a real prerequisite for any
minting story, and the element-id case has the defect today that O5 warns about
for nets. Under the reframing above, that dependency also disappears.
