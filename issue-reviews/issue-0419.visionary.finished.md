# Issue #419: TASK-0059: a net whose puts disagree on radix is refused by name instead of silently becoming one of them
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of apparatus, the claim is: **a port should declare what it can carry, and joining
two ports that disagree should be impossible rather than silently resolved.** That claim is
right, it is already half-true (width), and it is exactly where the project's own trajectory
points. `docs/capability-roadmap/sweep-01-values-and-logic.md` opens with "JLS's value domain
is the narrowest waist in the whole program… Every other sweep's items are features. This one
is the program." The two maintainer comments on this issue have already improved it twice:
the domain became an interval, and then the net stopped carrying one at all. Both moves were
toward the same seam. This review pushes three steps further along that same line, and the
last of those steps changes the answer to Open Question 2 and deletes two of the five
enforcement points.

I endorse the end state. I am reframing the route, and I am disregarding §8's method,
§7.4's shim, §7.10's four-site ordering rule, and Open Questions 1, 2 and 4 — with reasons
below for each.

## Reframe 1 — the enforcement seam is the operation layer, not four editor gestures

This is the finding that matters most, and it is a fact about HEAD, not a preference.

`docs/operation-layer.md` (#167) records the direction: *"Every editor mutation is being
migrated from inline state-machine code to a closed vocabulary of validated, invertible,
serializable commands (`jls.collab.op.CircuitOp`) applied through one entry point
(`jls.collab.op.OpSink`)."* The op that joins ports into a net already exists:
`/home/user/JLS/src/jls/collab/op/AddWire.java`. Its `validate(Circuit)` at `:235` is a real
rejection seam that already carries the exact idiom this issue wants:

```java
// the net's tri-state flag is per net, so every end must agree
throw new OpRejected("every end of a net must agree on "
        + "the net's tri-state flag");
```

and its `apply` at `:189` does `net.setBits(vendPut.getBits())` — **with no width check at
all.** So:

- P4's headline test ("parameterized over the four sites, so a fifth connection site added
  later fails by omission") is already failing by omission. The fifth site is `AddWire`, and
  it exists today.
- `AddWire` is the untrusted network surface (#170: *"this grammar is the future network
  surface"*). An editor-only check protects the one path where the input is trusted and
  leaves the one where it is not.
- The four `SimpleEditor` sites are scheduled for deletion by #167. Ordering a new
  unconditional check above a guarded one inside four gesture handlers is investment in code
  the recorded roadmap intends to remove.

**Concrete alternative:** put the domain-agreement rejection in `AddWire.validate`, beside
the tri-state agreement it already performs, and give `SimpleEditor`'s four sites a single
shared helper that calls the same predicate. Add the width check to `AddWire` in the same
motion — it is a live gap that this issue's own reasoning condemns.

## Reframe 2 — one `PortType`, not a second property bolted above the first

§7.10 mandates "the radix clause is first and is unconditional; the width clause keeps its
`> 0` guards." That rule is correct for property two of two. It does not survive property
three. The project has already committed, in writing, to at least three more declarative
port properties: drive strength (`keystone-a-design.md` §2 axis 2 — "strength is a property
of a driver and an observable of a net"), direction/`inout` (sweep-01 #4 IP-XACT, #129 JTAG,
V6), and the meta-state set `X/Z/U` (keystone A's A4, "recommended"). Each will arrive
wanting to be inserted immediately above the previous check at every connection site.

**Concrete alternative:** introduce one value in `jls.core` —

```java
public record PortType(int positions, Domain domain) {
    /** Empty if p may drive q; otherwise the user-facing refusal. */
    public static Optional<String> refuse(PortType driver, PortType sink) { … }
}
```

— and have every connection path call `PortType.refuse`. The ordering of clauses becomes an
internal detail of one method instead of a rule four call sites must be tested for obeying.
Property five is then free, and `RadixConnectionRefusalTest`'s parameterization guards the
axis that actually grows (properties) instead of the axis the roadmap is shrinking (sites).
This deletes O5's duplication rather than doubling it, which is the opposite of what §8
prescribes.

## Reframe 3 — make it unrepresentable; drop three of the five checks

The maintainer's second comment already discovered the shape: the domain is a port property,
nets carry values and width only. Follow it one step further. If the domain lives on ports,
and connection is the only way ports join a net, then `makeNet`, `recheck` and
`initSimulation` are re-derivations of an invariant already established at the join. Two
enforcement points suffice and they map onto seams that already exist:

1. **`AddWire.validate`** (and its `SimpleEditor` delegate) — all live mutation.
2. **`Circuit.finishLoad`** (`src/jls/Circuit.java:1300`) — all file input. ARCHITECTURE.md
   already names it as the validation phase, and the `LoadError` taxonomy (`ELEMENT_ERROR`,
   with location, detail and hint) is exactly the diagnostic surface §7.6 asks for. The issue
   invents a bespoke load error in `makeNet` instead of using it.

P6's `initSimulation` throw becomes an `assert`, which is what §7.11 already admits it is
("an internal-error path, not a user diagnostic"). Open Question 1 — "how do `makeNet` and
`recheck` unify?" — is marked **blocks execution** and dissolves entirely, as the second
comment already noticed for a different reason.

## Reframe 4 — the loud `getBits()` shim measures a name, not a type

§7.4 makes `Put.getBits()` throw and calls the resulting audit "this task's real work"
(Open Question 4). The 78-call-site figure that sizes that work is a count of the *string*
`.getBits()`, and the tree has **14 separate declarations** of `public int getBits()` — the
issue says so itself in O6 and then treats the total as one surface. Sampling
`/home/user/JLS/src/jls`, the top callers are `HdlExporter` (15), `SimpleEditor` (13),
`BatchSimulator` (6), `BatchTracePrinter` (5) — and most of those are `mem.getBits()`,
`reg.getBits()`, `group.getBits()`, `el.getBits()`, `net.getBits()`, `wire.getBits()`, not
`Put.getBits()`. `MemoryDialog.java:160` and `RegisterDialog.java:163` will never be affected
by a shim on `Put`. The audit is scoped by grep over a name shared by seven unrelated types.

More fundamentally: the shim is a runtime tripwire for a state the same issue proves cannot
occur ("the second branch is unreachable from any existing test — which is H3"), whose whole
evidence base is constructed tests (§11). The first maintainer comment already found the
better mechanism and applied it to values only: *"make it structural, not a runtime check…
a non-binary value then cannot reach a binary gate — it does not typecheck — at zero runtime
cost."* The same argument retires the shim. A reader that assumes a binary port should take
a binary port; that is a signature, checked once by javac, not an exception waiting for a
circuit the release cannot contain.

Note also that **width is not the binary-only concept the shim implies.** A 5-trit bus has
five positions. `positions` is meaningful at any domain; what is binary-only is the
*interpretation* of those positions as a `BitSet`. Throwing from the width accessor conflates
the two, and it is the one place in this issue where radix genuinely is being confused with
width rather than carefully distinguished from it.

## Reframe 5 — rendering belongs to the domain, which dissolves Open Question 2

Open Question 2 asks whether `Util.convert` should render via `BigInteger.toString(base)` or
throw. **Option (a) is wrong for the one domain this whole programme exists to serve.**
#361 and #345 are balanced ternary; the amendment's own example interval is `[-1,+1]`.
`BigInteger.toString(3)` emits digits `0 1 2` — it cannot render a balanced-ternary word at
all, and a student reading `210` where the machine holds `+0-` has been shown a lie of
exactly the class O9 forbids.

**Concrete alternative:** rendering is a method on the domain (`Domain.render(value)`), and
`Util.convert` keeps the three display bases it actually has, refusing others. The question
stops being a coin-flip between two bad options because the right answer lives somewhere else.

While there: the issue's reachability argument for O1 is wrong but the bug is real for a
better reason. `Util.convert`'s `base` always comes from `getBase()`, and every call site
(`ConstantRenderer.java:52`, `ConstantDialog.java:341,419`, `RegisterDialog.java:232`,
`Constant.java:75`) is fed 2/10/16 — the driver reaches base 8 only by calling the public
method directly. But `Constant.base` is a **saved attribute with an unvalidated setter**
(`src/jls/elem/Constant.java:130-146`), so a hand-edited file carrying `int base 8` loads and
renders empty. That is a real hostile-input bug, it is a two-line fix in the `Attribute`
setter plus a `LoadError`, it is worth shipping this week, and it has nothing to do with
radix. Harvest it as its own issue.

## Reframe 6 — the collision to name before either programme lands

`#419` sits inside #344 → #361 → #345 (balanced ternary). `#322` / keystone A is the
four-state programme the roadmap calls "the program". **Both generalize the same field on
the same class through the same accessor at the same four editor sites**, and they are
being designed independently. The amendment's interval `[lo, hi]` cannot express `X`, `Z` or
`U`; keystone A's three planes cannot express a trit. If both land as written, `Put` ends up
with an interval *and* a plane count, and JLS has two incompatible answers to "what can this
wire carry."

**Concrete alternative:** declare the port domain once as `(contiguous signed interval of
defined symbols) × (set of meta-states)`. Binary is `([0,1], {X,Z,U})`; balanced ternary is
`([-1,+1], {X,Z,U})`. Keystone A's planes then *encode* a domain rather than being a second
property beside it, and #344's plane arithmetic `⌈log₂(r+3)⌉ ≤ 3` becomes a derived fact
about that encoding instead of an independent bound. This is the one thing #419 could
contribute that no other issue in either programme is positioned to contribute, and it is
worth more than the shim, the four-site ordering rule and the K9 ratchet combined.

## Two smaller notes

- **§7.11's "release-sequencing hazard" is not novel and does not need a release checklist.**
  JLS already ships elements whose ports deliberately disagree on a declared property:
  `Splitter`, `Binder`, `Extend`, `FieldExtend` are width-crossing elements, legal because
  the crossing happens *inside* an element rather than *on* a net. `RadixBridge` is that
  pattern with a different property. Model it on `Splitter`, file it in #361 with the other
  elements, and the scheduling obligation evaporates.
- **Zero user-visible capability ships here, by design**, in service of a programme whose
  own parent records "No open issue covers the N-ary programme" and whose only capstone is
  CAP-03. That is defensible for a foundation — but only if the foundation is the one the
  *other* programme also stands on. Reframe 6 is what makes that true; without it, this is a
  radix-shaped seam that keystone A will have to work around.

## Bottom line

The end state — a port declares its alphabet, disagreement is refused by name at the moment
of the join, no coercion is ever defined — is right and I endorse it. The route should be:
one `PortType` predicate; enforced at `AddWire.validate` and `Circuit.finishLoad`; no
`getBits()` shim; rendering on the domain; and the domain defined jointly with #322 so the
two value-domain programmes converge on one type instead of two. That route is a smaller
diff than §8's, covers a surface §8 misses entirely, and survives the #167 migration that
§8's four edits do not.
