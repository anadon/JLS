# Issue #417: TASK-0039: two copies of one drawing get the same definition digest wherever they were dragged, and two different circuits claiming one version string are refused by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

One sentence survives the machine block: **a subcircuit definition should have a name
that means something outside the file it happens to live in.** That claim is right, it
is well-aligned with the arc `docs/grand-architecture.md` §2 describes (a datapath
teaching tool, an FPGA bridge, a collaborative editor — all three want to say "these
two things are the same module"), and D7's "circuit libraries are DATA, not plugins"
is the correct framing for how to get there.

Everything after that sentence is a *mechanism* — a bespoke text canonicalizer, a
per-instance stored digest, four invented name fields, and a hard load refusal — and
the mechanism is what I am disregarding. I am explicitly setting aside P5's refusal
policy, §7.1's stored `defdigest`, §7.3's invented defaults, and the private
canonicalizer of §7.10/§7.5. The goal is worth building; this is not the shape to
build it in.

## 1. JLS already ships this digest, and the issue's own grep hid it

O1 greps for `defid|defdigest|DefinitionId|DefinitionDigest` and reports zero hits —
a grep that can only find the thing it is about to name. What is actually in the tree
is `Circuit.stateHash()` (`/home/user/JLS/src/jls/Circuit.java:1548`):

> "A hash of this circuit's canonical serialized form (#166) … the SHA-256 of the
> canonical save text, in lowercase hex."

That is `DefinitionDigest.of` minus the elision step: same algorithm, same encoding,
same headless purity, same JDK dependency, already tested by
`jls.DeterministicSaveTest#stateHashIsContentDetermined`. Landed for #163 as the
collaborative-editing convergence oracle.

So the real design question is not "what new class computes a hash" but **"what
projections of the canonical text does this project need, and where does that
knowledge live?"** There are already three consumers of that text as a data
structure: `stateHash` (convergence), `CircuitSnapshot` (undo — deflated save text,
`/home/user/JLS/src/jls/edit/CircuitSnapshot.java:33`), and `jls.collab.op.NetBlocks`,
which *parses* `" String sid \""` lines out of it
(`/home/user/JLS/src/jls/collab/op/NetBlocks.java:226,323`). §7.5 makes the
canonicalizer `private static` inside a `final` class precisely so no second caller
invents a second digest space — but the second digest space is the one this issue
creates, and it creates it beside three existing consumers that already needed a
projection API and never got one.

**Alternative framing (concrete):** put the seam on `Circuit`, not in `jls.elem`.
`Circuit.stateHash()` becomes `Circuit.digest(Projection)` with two named projections
— `EXACT` (today's, unchanged, what #163 needs) and `STRUCTURAL` (placement- and
sid-independent, what identity needs) — and the projection is a public, documented,
versioned concept because `docs/file-format.md` will have to describe it anyway.
`SubCircuit` then calls that; it does not own it. This is smaller than the issue as
written, it puts the knowledge where the emitter's own invariants live, and it gives
#292/#358 and the collab stack one vocabulary instead of two 64-hex strings that look
identical and mean different things.

## 2. The projection is defined against imagined bytes, which is what happens at the wrong layer

Three symptoms, all from the same cause:

- **O2's evidence shows `int sid 10`; the emitter writes `String sid "js:1"`.** `sid`
  is a `StringAttribute` (`/home/user/JLS/src/jls/elem/Element.java`, base list ends
  `… fixed, trpos, sid`), and real values are namespaced (`"pin:1"`, `"we:1"` — see
  `test/jls/collab/op/CircuitOpTest.java:118-145`, `docs/file-format.md:376-395`).
  §7.10's substitution `" int sid " · ord` would emit a line the format does not
  define. The whole observed failure is a synthetic fixture, not a save.
- **The rank substitution is redundant with a rank the writer already emits.**
  `Circuit.save` sorts by (wire-last, stable id) and then assigns `id = 0,1,2,…` in
  exactly that order (`/home/user/JLS/src/jls/Circuit.java:1492-1505`). So
  `ord_C(e)` *is* the emitted `id`. Eliding the `sid` line outright gives the same
  discriminating power as replacing it with its rank, with no rank assigner, no
  second traversal, and no new line shape. The apparatus of §7.10 stage 2 exists to
  recompute something the writer already wrote down.
- **The elision set is a hand-maintained list of five names racing a growing
  format.** H2 and the `Memory` caveat are honest about this; the falsification
  criterion for H2 is "find the element that got away." A projection defined *by the
  writer* (attributes declare whether they are structural; `Attribute` is already the
  one declaration driving save, copy and load dispatch — issue #23) cannot get away,
  because a new attribute must state its class or fail to compile.

## 3. Identity by reference makes most of this problem disappear

The deeper reframing. JLS has no definition object at all: a `SubCircuit` instance
owns a mutable `Circuit` and writes it out inline
(`/home/user/JLS/src/jls/elem/SubCircuit.java:282-289`), and `copy()` deep-copies it
into a fresh `Circuit` with fresh sids (`:333-360`). The digest is a way to *detect*
that two objects were once one thing. FEAT-017 (#357) plans to make them literally
one thing — a definition table, an instance holding a reference.

Once that lands, P1 and P2 are vacuous: two instances of one definition are one
serialized definition, so no elision is needed to see they are equal. What remains
needed is exactly the *cross-file* case: did the file I referenced change under me?
And for **that** question you want the opposite of an elided projection — you want
`stateHash` over the exact bytes, because a moved element in a file you did not write
is information, not noise. In other words:

- the intra-file half of this task is machinery for a representation #357 deletes;
- the cross-file half needs no new digest function at all, only `stateHash` and a
  place to record it (which is #340's IC-7/IC-8 import row, priced separately at 3-6 mw);
- and the claimed ordering "identity must precede the definition/instance split"
  (#340 §6) is only true for the cross-file half. An intra-file reference resolves
  against a *table key*, not a hash. The critical path #340 draws is partly an
  artifact of having chosen a hash as the identity.

**Concrete alternative sequencing:** land the four-field name (three lines of state
plus a round-trip test — the cheap, genuinely missing thing that IP-XACT, Yosys module
names and `.circ` library refs all key on), reuse `stateHash` as the integrity check on
cross-file references, and let the structural projection wait until either #357 has
landed (where it may be unnecessary) or #292 needs it to deduplicate HDL modules (where
its requirements will be stated by a real consumer rather than guessed).

## 4. Two identity systems, and the collision policy is the bill for having both

`defid` is nominal identity; `defdigest` is content identity. Ship both and they can
disagree, which is why §7.10 stage 4 needs a four-case decision table at all. The
issue then reads its own worst case correctly in §11 — "P5's refusal will fire on
files whose authors both left the defaults" — and keeps the policy anyway.

Follow it through for the actual user. Defaults are `local:local:<name>:0.0.0` for
everyone. Two students' half-adders, both untouched defaults, pasted into one file.
Today that file loads. After this task it is **refused by name**. JLS's arc is
fail-loud on corruption and forgiving of beginner data; this inverts that on the most
common classroom operation there is.

The parent already saw it: #340 Open Question 1 recommends hard failure *for a library
resolved by reference* and diagnostic-plus-rename *for a definition inlined in a
circuit file*, "with the asymmetry stated normatively." #417 takes the harsh half and
applies it to the lenient case.

**The reframing that removes the failure mode entirely:** an identity claim binds only
when an author makes one. No `defid` = no identity = today's behaviour, exactly as
#340 invariant 4 already requires ("A definition with no identifier is never given
invented values … no child may default `vendor` to a hostname, a user name, or a file
path"). A defaulted `local:local:adder:0.0.0` is an invented value by any reading.
Delete the defaults and the collision refusal becomes unreachable except when two
people deliberately asserted the same VLNV — at which point hard refusal is obviously
right and nobody argues.

## 5. Where a single opaque digest is too coarse to be useful

Even granting a structural digest, one hash over the whole body cannot express what
the consumers need. Split it:

- **interface digest** — port names, widths, directions, order. This is what
  substitutability means, what `HdlModel.Port` already carries field-for-field, and
  what a Verilog module identity actually is.
- **body digest** — everything else.

Then: same `defid` + same interface + different body = a patched implementation, which
a library *can* accept and report; same `defid` + different interface = genuinely
incompatible, refuse. The single digest collapses both into "differs" and is therefore
forced to the harshest policy because it cannot tell breaking from benign. It also
dissolves Open Question 2 (P4, "are element names structure?") — *pin* names are
interface, an internal gate's label is body, and the question only looks hard because
one hash has to answer for both.

Related: §7.10 hashes everything the writer emits except five geometry names, so a
`Text` annotation, a probe attachment or a display attribute changes a definition's
identity. #340 Open Question 3 recommends the opposite ("exclude everything a renderer
reads … a comment should not change a definition's identity") and marks it as
*blocking the filing of TASK-0039*. This issue silently takes option (b), and the
consequence lands on the flagship user: an instructor who adds a comment to a shipped
lab handout has changed its identity while its version string still says `1.0.0`.

## 6. Three of the parent's nine invariants are contradicted

Not bookkeeping — each is the parent recording a design decision this task reverses:

- **#340 invariant 6, "the digest is derived, never stored. No child may cache it into
  the saved bytes."** §7.1 writes `defdigest` into every `SubCircuit` block. §7.11 then
  has to specify that a stored digest is a claim to be recomputed and overridden — i.e.
  the field is durable state that is never trusted. N instances now carry N copies of a
  claim that can drift, which is the *same failure one level up* from the one the task
  exists to fix.
- **#340 invariant 4, no invented identifiers** — see §4 above.
- **#340 invariant 2, "save output is byte-identical for a circuit whose identity
  fields are unset."** P10 concedes two added lines per `SubCircuit` block and
  regenerates goldens for it.

A task that contradicts three of its parent's invariants is usually a task designed
around the file format when its parent was designing around the model. That is the
single-sentence diagnosis of this issue.

## What I would build instead

1. `DefinitionId` as a record with parse/format and no defaults — absence is a state.
   (Keep §7.4's rejection of `:` in a field; keep version opaque; keep
   `docs/component-naming.md` as its home, noting that file currently documents Swing
   component names for #210 and will need a second section or a sibling document.)
2. `Circuit.digest(Projection)` with `EXACT` (= today's `stateHash`) and `STRUCTURAL`,
   the projection driven by `Attribute` declarations rather than a name list, and
   `sid` elided rather than rank-substituted since `id` already carries the rank.
3. No stored `defdigest`; recompute at every use, per the parent's invariant 6.
4. Collision policy: refuse only when two *authored* identities collide; report, never
   refuse, when identities are absent — restoring #340's stated asymmetry.
5. Defer the structural projection's exact contents until #292 or #357 states them as
   a consumer, and split interface from body when it does.

That is a smaller task, it strengthens a seam three existing subsystems already lean
on, and it leaves the expensive decision — what structural identity *means* — to be
made by the first thing that has to live with it.
