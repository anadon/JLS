# Issue #344: FEAT-028: a port and a net know what alphabet they speak, and the editor refuses a ternary-to-binary connection for the same reason it already refuses 4-bit-to-8-bit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the machinery and there are two independent goods bundled into one feature:

**(A) A typed connection boundary.** Ports declare what they carry; the editor
refuses a connection whose ends disagree, naming both sides; the same rule fires
at load. This is a *type system for the wiring graph*, and JLS half-has one
already: `Put.bits` with the documented convention "0 implies arbitrary"
(`src/jls/elem/Put.java:33`), a wildcard honored at all four editor sites
(`src/jls/edit/SimpleEditor.java:4014`, `:4140`, `:4245`, `:4356`, each guarded
`bits1 > 0 && bits2 > 0`), and a join rule on nets.

**(B) A non-binary value domain.** Balanced ternary as a first-class alphabet,
computed by a plane-parallel operator kernel, so JLS becomes good at ternary
*architecture*.

(A) is a repair and generalization of something the project already has and
already gets wrong. (B) is a new research capability whose only beneficiary,
CAP-03 (#295), was closed, and whose replacement beneficiary CAP-39 (#888) was
filed the same day this issue was replanned. Those two goods have very different
standing against the project's arc, and the feature prices and sequences them as
one thing. My reframing separates them.

## The arc check

The README sells JLS as an educational editor/simulator with a modernization
roadmap; ARCHITECTURE.md's recorded decisions consistently choose the *narrow,
already-demanded* option (i18n declined for want of a requesting user; the
compiled simulation pass declined as premature; plugins removed rather than
generalized). The issue itself concedes "No open issue covers the N-ary
programme" and "Nothing user-visible arrives from this feature alone." Against
that grain, 8–12 mw (replanned to 12–20) at bus factor 1, delivering zero
drawable capability, is the single most out-of-character purchase in the
tracker. Part (A), by contrast, is squarely on the arc: it hardens a boundary
that #78's element registry (already landed as
`src/jls/elem/ElementRegistry.java`, 35 types — ARCHITECTURE.md line 118 is
stale in saying no registry exists) now makes cheap to sweep.

## Reframing 1 — one signal type with a unification rule, not a second scalar tag checked "above" width

The issue's §3 asks for radix to be checked *above* the width check at four
sites, for radix to be validated where width is widened, and (Open Question 3)
for a policy plus an architecture test over 78 `getBits()` call sites. All three
are symptoms of adding a parallel dimension rather than replacing the dimension.

Cut the seam differently: introduce **`SignalType`** — today `(width, domain)`,
tomorrow whatever else ports must agree on — with a single `unify(a, b)`
returning either a type or a mismatch describing *which component* disagreed and
with what values. Then:

- The four editor sites become one call each, and the "which check goes first"
  question evaporates: the mismatch object names the component that failed.
  A future third component costs zero editor edits.
- `WireNet` stops storing a domain (honoring the maintainer's correction) *and*
  stops storing a widened width. `net.type()` becomes a **derived query** —
  fold `unify` over attached ports — never stored, never widened. There is
  nothing to widen because there is no field, which is exactly the structural
  guarantee the correction comment was reaching for.
- This fixes a latent defect that predates ternary: `WireNet` has **two
  disagreeing width policies today** — `makeNet` takes last-put-wins
  (`src/jls/elem/WireNet.java:139`) while `recheck` takes `Math.max`
  (`:280`). One rule replaces both.
- Open Question 3 dissolves. You do not audit 78 `getBits()` call sites with a
  policy and an ArchUnit test; you delete `getBits()` in favor of `type()` and
  let the compiler enumerate the sites once, permanently.

This reframing is representation-agnostic: it holds whether the domain is a
signed interval, a radix, or Reframing 2's encoding.

## Reframing 2 — the alphabet is a presentation-and-constraint over the existing bit vector, not a new value algebra

I would drop the plane-parallel operator kernel entirely, and with it TASK-0060,
§3's `P(r) = ⌈log₂(r+3)⌉` arithmetic, criterion 6, IC-5, IC-6, the generic
symbol tier introduced by the REPLAN, and the permanent two-implementations-
must-agree-forever oracle that CAP-39's KC-39-2 already names as an accepted
cost. Replace them with:

> A **domain** is a declared symbol set plus its bit encoding. Binary is the
> identity domain. A ternary port declares 3 symbols at 2 bits/digit. Values on
> the wire remain the bit vectors JLS has always propagated.

What this buys:

- **Radix-2 byte-identity becomes true by construction rather than by
  benchmark.** Global invariants 1, 2, 3 and 6 and IC-4 stop being risks to
  manage and become facts; the plane-count freeze conflict flagged on #878 stops
  being this feature's problem.
- **The kernel disappears.** A balanced-ternary adder is an *element* whose
  `react` computes on encoded vectors — one implementation, no differential
  oracle, no dual tier. If a student's ternary datapath is ever slow, optimize
  that element then, with a measurement, exactly as recorded decision #221
  requires for the simulation strategy generally.
- **CAP-39's `[0,255]` bus is free.** Under the interval model, `[0,255]`
  requires the new generic symbol tier — to express a byte bus, which JLS has
  shipped for twenty years. That is the clearest evidence the interval model
  re-derives width as a special case of alphabet and then pays to reimplement
  it. Under Reframing 2 it is an 8-bit bus with a decimal presentation, and the
  "bridge element" is a relabel, free at simulation time.
- **It is continuous with the codebase.** `Constant` already persists a display
  radix (`src/jls/elem/Constant.java:36`, "the radix the value is displayed in
  (2, 10 or 16)"). A domain is that concept promoted from one element's
  rendering to a port's declared type, plus a legality constraint. The issue
  files this fact under "radix means something else here"; I read it as the
  design already present in the tree.

The honest cost, stated rather than hidden: the maintainer explicitly wanted
balanced ternary "native, not a display convention." Under Reframing 2 a ternary
digit occupies 2 bits with one illegal code point. A student cannot observe this
through a probe, a gate, a display, or VCD — only through the bit-structural
elements: `Splitter`, `Binder`, `Extend`, `FieldExtend`. Those must refuse
non-binary ports. But CAP-39 already defers `Extend` (its Q3), and the program
already refuses tri-state, strength and multi-driver on non-binary nets (D6), so
the seal costs one more refusal of the same kind rather than a new principle.
Illegal code points are what #322's X exists for once it lands; until then they
are refused at the element boundary.

## What I am disregarding, and why

I am disregarding §1 criteria 6 and 7, IC-5, IC-6, and the whole of TASK-0060 as
stated — not because they are wrong on their own terms, but because they are the
price of a value-representation choice I think the project should not make. They
commit a single-maintainer pedagogy tool to owning a numerics kernel, a lane-
packed prefix-carry adder, a 200k-vector differential corpus, and (post-REPLAN)
two operator tiers that must agree forever, in the hottest code in the program,
for a capability with no requesting user. Recorded decision #221 declined a
second execution strategy on exactly this reasoning; a second *value* strategy
is the same purchase with a worse blast radius.

## Two design holes the thread has not closed

1. **The port-only correction breaks sites 1–3.** `WireEnd.getBits()` delegates
   to the net (`src/jls/elem/WireEnd.java:373`), and three of the four editor
   sites compare wire-end to wire-end or wire-end to put. With no net domain
   there is nothing for a wire end to report, so "a straightforward port-pair
   check" is not available there. The fix is Reframing 1's derived query: the
   net has no domain *field*, but `net.type()` computes one on demand. Comments
   2 and 3 reconcile cleanly under that reading and under no other I can see.
2. **The wildcard has no domain story.** `Constant` creates a 0-bit — i.e.
   width-polymorphic — output (`src/jls/elem/Constant.java:91`) and the editor
   honors the wildcard at all four sites. Criterion 1 ("every existing element
   reports 2/`[0,1]`") is therefore wrong for the polymorphic elements: a
   `Constant` should report *any domain*, inferred from its peers, or it becomes
   binary-only the moment the registry sweep asserts a concrete value on it.
   A unification lattice with a top element handles this; a scalar equality
   check cannot.

## Smallest thing worth shipping first

If Reframing 1 lands alone — `SignalType`, `unify`, derived `net.type()`, one
refusal message naming the disagreeing component, the registry sweep over 35
types, `getBits()` retired — the project gets a real repair (two disagreeing
width policies collapse to one), #78's registry gains a typed port contract,
the never-widen invariant becomes structural, and every downstream alphabet
question stays open at zero committed cost. That is the artifact Open Question 4
is asking for, and it is worth doing whether or not ternary is ever drawn.
