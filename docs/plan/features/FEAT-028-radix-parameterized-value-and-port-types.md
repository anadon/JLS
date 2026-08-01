# FEAT-028 - Radix-parameterized value and port type system

**Status:** proposed | **Cost:** 8-12 mw | **Owner program:** P1 |
**Spine rank:** -

## Capability delivered

A port and a net know what alphabet they speak, not only how many positions they
carry, and the editor refuses to connect a ternary port to a binary one for the
same reason and at the same moment it already refuses to connect a 4-bit port to
an 8-bit one. Values in radix 3, 4 and 5 become expressible and computable at
the kernel level with no change to storage, no change to the primitive-long
fast path, and radix 2 provably byte-identical to what shipped. Nothing
user-visible arrives from this feature alone; it is the type system the N-ary
element family is drawn on top of.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-03 | required | radix as a property of a value and a net, validated at connection, with radix 2 provably unchanged |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-026 | Radix 3 and 4 with full X/Z/U ride the three bit planes of P1's `Word` record. Without that record there is no spare code-point space and radix becomes a storage migration instead of a validation rule |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0056 | Widen the value permits and migrate the value representation | Shared with FEAT-026 and FEAT-030: the permit widening is where the radix accessors are reserved |
| TASK-0059 | Radix on ports and nets, validated not widened | Carries radix on puts and nets and checks it above the width check at the editor connection sites |
| TASK-0060 | The higher-radix operator kernel | Shared with FEAT-029: min, max, complement, cyclic and literal over the planes, plus the lane-packed balanced-ternary adder |

## Acceptance criteria

1. `Put` and `WireNet` carry a radix. Every existing element reports 2, and a
   test asserts that over `ElementRegistry.all()` so a new element type cannot
   silently default.
2. Radix is **validated, never widened**. `WireNet.makeNet` and
   `WireNet.recheck` reject a mixed-radix net; they do not take a maximum the
   way width does.
3. The editor refuses a mixed-radix connection at the same four connection
   sites that today refuse mismatched widths, with a message naming **both**
   radices - not a generic refusal.
4. The refusal is also a load-time refusal: a hand-edited file declaring a
   mixed-radix net is rejected with a diagnostic naming file, element and both
   radices.
5. Radix 2 is byte-identical. The entire existing golden corpus passes
   unchanged, and a benchmark shows no measurable regression in the warm event
   loop.
6. The operator kernel is written once over the planes and covers min, max,
   complement, cyclic shift and literal for radices 2, 3, 4 and 5; radix >= 6
   is refused with the arithmetic reason stated.
7. A differential test compares the lane-packed balanced-ternary adder against a
   per-digit reference implementation over a large seeded vector corpus.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the entire N-ary program | **no issue** - neither this feature nor FEAT-029 has a tracker entry |

## Design notes

The whole feature rests on one arithmetic fact, and it is worth stating once
because it is what makes the cost 8-12 weeks rather than a storage epoch: a
radix-*r* alphabet with full X/Z/U needs `ceil(log2(r+3))` planes, so r=3 needs
6 code points, r=4 needs 7, r=5 needs 8 - all three fit the three planes P1's
`Word` already allocates, of which P1 itself uses five. The fourth-plane cliff
is at **radix 6**, and that is why N is bounded. It is arithmetic, not
preference. (`07-mvl-determination.md` §0, §1.4.)

The second design decision is placement. Radix goes where width already is:
`docs/simulation-semantics.md:59-61` already says bit width is a property of
elements and wire nets rather than of the value, and "reading code interprets a
value at the reader's declared width". Generalize that sentence from *how many
positions* to *how many positions, in what alphabet*, and the value type never
learns about radix at all - which is why radix 2 costs literally nothing.

Radix differs from width in exactly one place and it must not be missed:
`WireNet.recheck` takes `Math.max` over attached puts' widths
(`src/jls/elem/WireNet.java:280`) and `makeNet` seeds `net.bits` from the first
put (`:139`). Radix has no maximum. Mixed radix is an error, not a widening.

Stage 0 of the MVL determination - about one week - must happen *inside* P1:
re-anchor the semantics document as an alphabet-parameterized statement, add
`radix()` returning 2 to `Put` and `WireNet`, and freeze `Word`'s field list in
writing. Its cost is the governance motion at bus factor 1, not the two
accessors. If P1 ships without it, the remaining 7-11 weeks become a second
value migration.

## Risks

- **`getBits()` is 89 call sites.** Every one of them is a place where a reader
  interprets a value at its own declared width, and every one is a candidate
  site for the same mistake in the radix dimension. TASK-0059's policy over
  those call sites is the real work, not the two accessors.
- **A widening `recheck` would be silent and fatal.** If radix accidentally
  follows width's `Math.max` idiom, a ternary port connected to a binary net
  produces a net that is neither, with no diagnostic. Acceptance criterion 2
  exists for this and should be the first test written.
- **This feature ships nothing a user can see.** 8-12 maintainer-weeks with no
  drawable capability is a real morale and prioritization hazard at bus factor
  1. `07-mvl-determination.md` §7.2 recommends shipping the cheap
  data-only preview first so the program has an artifact before the elements
  land.
- **Device-tier expectations.** Native radix makes JLS good at ternary
  *architecture*. It reaches nothing at the device tier - there is no energy
  model and simulation time is dimensionless. Say so in the documentation that
  ships with it, or the capability will be misread.

## Evidence

- Plane arithmetic and the radix-6 cliff: `07-mvl-determination.md` §0 and
  §1.4; the three-plane record itself at
  `docs/capability-roadmap/README.md:126-140`.
- The placement argument, quoting HEAD: `docs/simulation-semantics.md:57-61`.
- The one place radix is not like width: `src/jls/elem/WireNet.java:139`
  (`net.bits = put.getBits()`), `:280` (`bits = Math.max(p.getBits(),bits)`),
  with `makeNet` at `:97` and `recheck` at `:272`.
- The four editor connection sites, verified at `addc6c5`:
  `src/jls/edit/SimpleEditor.java:4015`, `:4142`, `:4247`, `:4358`, all
  `overlapMessage = "Bits don't match"`.
- 89 `getBits()` call sites across `src/` at `addc6c5`.
- Cost: `07-mvl-determination.md` §1.1 stages 0 (~1 wk), 1 (3-5 wk) and 2
  (4-6 wk) = 8-12 wk.
- Measured kernel performance that justifies the lane-packed adder: 9.79 ns/op
  done right versus 178.32 ns/op done naively for 32-digit balanced-ternary
  addition, verified against a per-digit reference on 200,000 random vector
  pairs (`07-mvl-determination.md` §1.4, `Add3Swar.java`).
- **Cost reconciliation.** Band 8-12 mw. Tasks named for it: TASK-0056,
  TASK-0059, TASK-0060, totalling 5.5 wk. The named tasks are the leading,
  dividable slices of this feature, not the whole of it; the residual has no
  task id, because the registry's task space is closed at TASK-0112. Do not
  read 5.5 wk as the feature. Shared tasks counted once at the task level:
  TASK-0056, TASK-0060.
