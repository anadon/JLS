# Issue #889: TASK-C361-1: a value-range crossing is declared data — the numeral rule is the default, everything else is a named mapping that is total or refuses by value, and no crossing is ever implicit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the mechanism, #889 asserts one thing: **when two ports speak different
alphabets, the circuit must say what the crossing means, and the tool must never
guess.** That is the correct principle, it is the same principle #419 O9 already
states for radix, and it is squarely aligned with the project's deepest recorded
commitment — the anti-`coercedX` discipline (`src/jls/hdl/imp/ImportSummary.java`,
quoted in #419 O9, #453 H5, and `docs/capability-roadmap/sweep-01-values-and-logic.md`
under #75). Nothing below disputes the end.

What I do dispute is the shape of the mechanism, and the assumption that this
mechanism is N-ary-only. Both are fixable, and fixing them makes #889 worth more
than it currently claims.

## Reframing 1 — `numeral` is not a kind; it is Splitter/Binder in alphabet space

The four `kind` values are not four cases of one thing. `ordinal`, `table` and
`partial` are all **arity 1→1 symbol relabelings**: one source digit in, one target
digit or a refusal out. `numeral` (`sum(v_i * N^i)`, "one `[0,255]` digit ↔ eight
`[0,1]` digits") is **arity n→1 regrouping** — it changes how many wires there are,
not what a symbol means. JLS already has a name for that operation at width: it is
`Splitter`/`Binder` (`src/jls/elem/Splitter.java`, `Binder.java`), and #453 itself
says the bridge's role "is exactly `Splitter`/`Binder`'s for width."

Folding both into `(sourceInterval, targetInterval, kind, data)` produces a record
whose field validity depends on its tag: `numeral` carries no data but demands
"numeral-range compatibility"; `ordinal` demands `N_s == N_t` and carries no data;
`table` demands exactly `N_s` entries; `partial` demands a sentinel vocabulary. That
is a sum type flattened into a tagged struct, in a codebase whose stated direction is
the opposite (sealed interfaces + records + exhaustive dispatch — `SimEvent.Payload`
as the precedent, `docs/grand-architecture.md` §4.3, #95, cited in sweep-01 V1).

**Concrete alternative.** Two independent facilities:
- `Regroup(sourceAlphabet, targetAlphabet, digitsPerSymbol)` — closed form, no data,
  the numeral rule, and the only thing that changes wire counts. This belongs with
  the bridge element's geometry, not in a mapping table.
- `Relabel` — a total function from source symbol to `target | refuse`, over equal
  arity. Nothing else.

## Reframing 2 — `ordinal`, `table` and `partial` are one type, not three

Once regrouping is separated out, the remaining three kinds collapse. A `Relabel` is
an array of length `N_s` whose entries are target symbols or a reserved `UNMAPPED`
sentinel. Then:

- `ordinal` is a **constructor**, not a runtime kind: `Relabel.ordinal(src, tgt)`
  fills the array. Whether the array is *stored* densely or as an affine rule is a
  storage-encoding question, exactly as `Memory`'s `initrle` already answers for
  memory contents (`docs/file-format.md` §9, silent-drop caveat).
- `partial` is not a kind either; it is the observation that some entries are
  `UNMAPPED`. AC-3's named diagnostic becomes a property of every mapping.
- **AC-2 disappears.** "A `table` whose length is not `N_s` is refused at load" is a
  load-time check in the issue's framing; in this framing it is a record's canonical
  constructor precondition, and there is no representation of a wrong-length mapping
  anywhere in the program. A whole acceptance criterion turns into an unrepresentable
  state — that is the test that a seam was cut in the right place.

## Reframing 3 — the generic case should be drawn, not hidden

This is the point I would press hardest. #889 makes an arbitrary symbol permutation
an **invisible named attribute**. A student printing a circuit, or reading a
classmate's, cannot see that `0` on this wire means `−1`. That pulls against the arc
the whole project runs on: JLS's thesis is that the drawing is the truth. Sweep-01 V3
makes the same call explicitly for pull-ups — "Drawable, which is better than a net
attribute" — and #453's entire types-not-attributes argument (O2/O3: new classes fail
closed, new attributes fail open) is the same argument one level down.

JLS already has a drawable, savable, editable, round-trip-tested "arbitrary function
from input symbols to output symbols": `TruthTable` — and #453's batch 2 already
plans `MvlTruthTable`, whose don't-care/digit-`2` collision (#453 O6) is being solved
anyway. A `[0,2]`→`[0,1]` thresholding *is* a one-input truth table.

**So the maintainer's directive — "make sure there's a generic mapping table for
other value range mappings to use" — may already have an answer that costs nothing
new: the generic mapping table is an element.** It gets the editor UI, the save
grammar, `AllElementsRoundTripTest`, the fails-closed HDL classification, the #340
identity story and the palette-disclosure ratchet for free. #889 then shrinks to the
two closed-form crossings (`Regroup`, `ordinal`) that genuinely cannot be drawn as
tables because they are constant-space by construction.

I am explicitly setting aside AC-2, AC-5 and AC-6 as written under this framing: they
are acceptance criteria for a bespoke data mechanism that, on this route, is not
built.

## Where the issue argues against itself

1. **The size guard forbids the use case that opens the issue.** The motivating table
   lists "symbol permutations / Gray-like encodings" as needing an explicit table.
   Gray codes matter at width — and a `[0,255]` permutation is 256 rows, refused by
   the budget, with the diagnostic naming `numeral`/`ordinal` as alternatives that
   *cannot express a permutation*. The guard should be about encoding (affine rule +
   exception list) or about a per-drawing budget, not a per-table row cliff.
2. **Three competing defaults for the same crossing now exist in three documents.**
   #453 Stage 4 defines the bridge as digit-identity with `d ≥ N` refused
   (`u(d) = d`, `v(d) = ⊥`). #887/#889 make the *numeral* rule the default. #889's
   `ordinal` is a third reading. Under intervals with `lo ≠ 0` these disagree. Which
   one a user gets currently depends on which issue lands first. Settle this in one
   place before either ships; the boundaries section claims #453 and #889 "neither
   subsumes the other," but on this point they overlap and conflict.
3. **AC-4 depends on #322, which is not in `ordering_after`.** X/Z/U behaviour per
   mapping kind is untestable until the four-state core exists (the issue says so
   itself: "U per #322's stage machinery"), yet ordering names only [878, 419].
   Either add the edge or condition AC-4.
4. **A stored digest is a field that can lie.** If storage is data and the
   serialization is canonical (`DeterministicSaveTest` discipline, AC-5), identity is
   *derivable*. Storing it creates a new failure mode — digest and data disagreeing —
   and a new load-time check to catch it. Derive, never store.
5. **"No new item kind" does not settle where a *named* mapping lives.** A mapping
   named once and referenced by two elements, or shared between drawings, is a new
   top-level block, and `docs/file-format.md` §9 makes block-structure changes a
   mandatory `FORMAT` bump. AC-7 ("a binary circuit's saved bytes do not move") is
   satisfiable either way, but the bump question is unaddressed. If mappings are
   per-bridge attributes on a *new* element type, #453 O3's loud-refusal property
   protects you and no bump is needed — another reason to keep the mapping on the
   element rather than in a shared library nobody has asked for yet.

## The alignment claim I would actually make for this work

Read against the project's own priority statement, the N-ary program is the weaker
of two value-domain programs. `sweep-01-values-and-logic.md` says plainly: "JLS's
value domain is the narrowest waist in the whole program… Every other sweep's items
are features. This one is the program" — and it prices V1 (four-state) as unlocking
sixteen standards directly, twenty-four with dependents. The N-ary program unlocks
none of them; its capstone beneficiary CAP-03 (#295) was closed *not planned*, and
#888 describes itself as "the disposition for #344/#361 that #295's closure left
dangling." A sub-sub-task of a re-homed program should not be inventing new core
mechanisms speculatively.

But there is a version of #889 that is *stronger* than the four-state program's own
plan. Sweep-01 V4 describes the IEEE 1164 nine-value layer as "a presentation and
interchange mapping, not a fifth mechanism" — i.e. **exactly a declared symbol
relabeling between alphabets.** So is `TO_01`/`TO_X01` (#27). So is the Yosys import
policy that currently produces `coercedX` (#75) — that is a partial mapping with the
unmapped values silently folded, the precise defect #889 exists to outlaw. So is the
`-t` grammar's future `x`/`z`/`-` tokens (V7).

**Build the relabeling layer over an `Alphabet` abstraction that four-state and N-ary
both instantiate, and require its first two clients to be 1164's `TO_X01` and the
Yosys import policy — not the CAP-39 fixture.** That converts a speculative N-ary
sub-mechanism into infrastructure that pays down the project's stated narrowest waist,
and it gives the mechanism two real users before a third is invented. AC-1's fixture
then becomes the third demonstration rather than the definition of done.

## Recommended shipping order

1. #419 alone: refusal, no crossing at all. Already the highest-value 90%.
2. The bridge with **exactly one** closed-form crossing, settled against #453 Stage 4.
3. The drawn table element (`MvlTruthTable`, batch 2) as the generic case.
4. Named/shared mappings, digests, budgets — only when two drawings actually need to
   share one, and never before the `Alphabet` seam serves the four-state program too.
