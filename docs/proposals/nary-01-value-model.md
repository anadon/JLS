# N-ary wire value model: signed bounded integer alphabets with bundles

**Status: proposal — input for issue elaboration, not normative.** Written
2026-08-08 from a maintainer design discussion on the
`claude/nary-logic-engine-redesign-ib1l1e` branch. Companion documents:
[`nary-02-format-and-migration.md`](nary-02-format-and-migration.md) (save
format and migration path) and
[`nary-03-issue-handoff.md`](nary-03-issue-handoff.md) (tracker actions,
decision log, open questions). Nothing here supersedes
`docs/simulation-semantics.md` until the work lands and that document is
rewritten through its own process.

## 1. The model, in one paragraph

A wire position holds **exactly one value from a declared, contiguous,
bounded integer interval `[lo, hi]`** (`lo <= hi`, both signed) at any
instant, plus the three meta-states **X** (unknown), **Z** (undriven), and
**U** (never assigned) inherited from the four-state value core (#322). A
net is a **bundle**: `(interval, width)` — `width >= 1` positions, all
speaking the same interval. The alphabet size is `N = hi - lo + 1 >= 2`.
Today's model is the special case `interval = [0, 1]`.

Examples:

| logic | interval | N |
|---|---|---|
| binary (today) | `[0, 1]` | 2 |
| balanced ternary | `[-1, +1]` | 3 |
| unbalanced ternary | `[0, 2]` | 3 |
| quaternary | `[0, 3]` | 4 |
| byte-as-symbol wire | `[0, 255]` | 256 |
| word-as-symbol wire | `[0, 2^32 - 1]` | 2^32 |

The signed interval is the deliberate generalization over the earlier
filed formulation (#344, which is radix `0..r-1`, `r <= 5`): balanced
ternary becomes **native** (`[-1,+1]` digits), not a display convention
over `{0,1,2}`, and signed single-digit levels are expressible directly.

## 2. What was considered and rejected on the way here

Recorded so the next session does not re-derive it:

1. **Arbitrary N states drawn from a discontiguous set of real values**
   (e.g. `{-1.2, 0.0, 3.3, 5.0}`). Rejected by the maintainer after
   discussion: it drags physical levels (volts) into a logic simulator's
   value domain, requires first-class `Domain` objects with canonical
   real serialization in the save format, opens an analog seam
   (quantizers, thresholds) that belongs to the analog program (FEAT-049
   / #331) rather than the value domain, and none of the kernel
   semantics ever consult the levels — only their ordering. A future
   analog boundary can quantize a solver voltage into an interval symbol
   without the alphabet knowing about volts. This mirrors CAP-03's own
   scoping: "JLS is word-level, so it can host ternary architecture and
   cannot host ternary devices" (#295).
2. **Radix bounded at 5 by plane arithmetic** (the filed #344 design:
   `ceil(log2(r+3))` planes, refusal at radix 6). Rejected *as a scope
   limit* and retained *as a representation fast path* — see §6. The
   maintainer requires arbitrary bounded N as a hard requirement.
3. **Unsigned `0..N-1` alphabets only.** Rejected: negative digit values
   are required (balanced ternary is the flagship case).
4. **Per-position mixed intervals within one net.** Rejected: buys
   nothing, breaks the width machinery. Mixed alphabets in one *drawing*
   compose through explicit bridge elements (§7), exactly as CAP-03
   scoped mixed-radix composition.

## 3. Meta-states and initialization

Every interval implicitly gains X/Z/U as per-position sentinel states,
orthogonal to the alphabet — the same three states #322 gives binary.
Semantics:

- A floating (undriven) non-binary input reads **Z**, and elements
  propagate **X** from it. There is **no `zeroFill()` analogue for
  non-binary nets**: the binary zero-coercion sites are a compatibility
  behavior being preserved for binary (keystone B's migration mechanic),
  not a rule to generalize. `lo` is *not* "the default value"; nothing
  makes 0 (or `lo`) special at initialization. Uninitialized state is
  **U** where #322's stage machinery enables it, else X.
- This deliberately makes the common first-year mistake (unconnected
  input) *visible* on N-ary nets from day one, consistent with the P1
  program's pedagogy argument (`docs/capability-roadmap/README.md`,
  P1).

## 4. Ordering, numerals, and the operator kernel

The only structure the kernel uses is the integer **ordering** of the
interval. Kernel operations (one module, written once, read by every
engine — the TASK-0060 discipline from #344/#361):

- `min(a, b)`, `max(a, b)` — MVL conjunction/disjunction
  (Łukasiewicz/Post style), defined for every interval.
- `reflect(v) = lo + hi - v` — complement by reflection. For `[0,1]`
  this is NOT; for `[-1,+1]` it is balanced-ternary negation.
- `cycle(v, k)` — cyclic successor: wrap within `[lo, hi]`.
- `literal(v, [a, b])` — Allen-Givone interval literal: `hi` if
  `a <= v <= b`, else `lo`.
- equality / comparison — over values, X-propagating.

Each collapses **exactly** to today's binary operation at `[0,1]` — the
bit-for-bit identity that #344 IC-4 / #361 invariant 3 already require,
which is what lets the kernel be adopted without moving a golden.
Arithmetic beyond this (adders with carry, saturating vs. wrapping sum)
is **element-level**, not kernel-level, and each arithmetic element
states its own semantics.

**Bundle numerals.** A width-`w` bundle over `[lo, hi]` is read as the
positional numeral `sum(v_i * N^i)`. Because the digit set is `N`
consecutive integers, this representation is injective (it is a shifted
base-`N` system; for symmetric intervals it is the classical
balanced-radix numeral). This single rule gives display, `-t` vector
literals, and `Constant` values a well-defined signed interpretation for
every interval, with two's-complement display remaining a
*presentation* option for `[0,1]` exactly as today.

## 5. Connection and resolution rules

- **Interval equality is validated, never widened.** A net's interval is
  the interval all attached puts agree on; disagreement is an error
  naming both intervals — at the four editor connection sites, at net
  construction/recheck, and at load. This is #344's criterion 2
  verbatim, generalized from radix to interval. The width fold keeps
  today's `max` behavior, unchanged for binary and non-binary alike.
- **Non-binary nets are single-driver, initially.** A second driver on a
  net with `interval != [0,1]` is refused at net construction and at
  load (same diagnostic surface as the interval mismatch). Rationale:
  contention over an arbitrary alphabet has no default meaning; a
  declared resolution function is user data (the VHDL model) and is a
  later, additive feature if demand appears. Binary nets keep the #322
  strength/resolution fold untouched — tri-state buses, pulls,
  open-drain are binary-domain concepts and stay there.
- Tri-state-ness, strength, and the wired-logic vocabulary therefore
  **do not exist** on non-binary nets in this iteration; the editor
  refuses tri-state drivers on them.

## 6. Representation: generic contract, specialized layout

The **contract** is total: every interval gets identical semantics. The
**representation** is tiered, and this split is load-bearing — keystone
C's measurements (`docs/capability-roadmap/keystone-c-performance.md`)
make the binary hot path's layout a hard constraint (RV32I golden corpus
byte-identical, warm event loop within noise; #344 IC-4):

1. **Fast tier — the #322 plane layout, unchanged.** `[0,1]` (and, if
   the implementation finds it free, any interval with `N <= 5` via the
   three-plane code space, storing offsets `v - lo`) rides
   `Word(width, a, b, u)` / `Wide` exactly as keystone A designed.
   Binary circuits allocate and compute exactly as the four-state core
   does; the interval is `[0,1]` by construction and costs zero bytes.
2. **Generic tier — per-position symbol codes.** For everything else, an
   immutable value of `width` codes, each code either an offset
   `0..N-1` or a sentinel (suggested encoding: `N`=X, `N+1`=Z, `N+2`=U).
   Backing store is an implementation choice (packed `long[]` lanes by
   code width, or `int[]`), constrained to: immutable, canonical
   (equal values are `equals`), interned constants for common cases,
   allocation-light. It pays its own cost and is never on the binary
   path.

The kernel is written once against the *contract*; the fast tier's
plane-parallel implementations are an optimization whose agreement with
the generic tier is enforced by a differential test (the #344 IC-5
pattern: seeded vector corpus, exact agreement, seed recorded). **Two
operator implementations that must agree forever is the acknowledged
cost of arbitrary N**; the differential oracle is the mitigation, and it
must exist from the first commit that has both tiers.

**Bounds.** `N` may run to `2^31` (interval endpoints are `int`; `N` and
offsets fit `long` arithmetic). There is no kernel-level cliff. Cliffs
are **element-level and refused per element with the arithmetic stated**
— e.g. an N-ary `TruthTable` is `N^inputs` rows and refuses when the
table exceeds a stated budget; a `Display` renders any N; a bridge
element's table is `O(N)` or closed-form. This replaces #344's
kernel-level radix-6 refusal.

## 7. Element vocabulary (delta over #361)

#361's family transfers with these amendments:

- **Order-based gates** (min/max/literal), **negation modes** (reflect,
  cyclic, diminished) — generalize as in §4, parameterized by interval.
- **Adder** — element-level semantics per numeral system of §4;
  balanced-ternary adder as filed (lane-packed fast path, per-digit
  differential reference).
- **Bridge element** (the generalized Splitter/Binder): converts between
  `(interval A, width w)` and `(interval B, width u)` bundles where the
  numeral ranges are compatible — e.g. one `[0,255]` digit ↔ eight
  `[0,1]` digits; a `[-1,+1]`-width-5 bundle ↔ its binary encoding.
  X/Z on any consumed position makes affected produced positions X.
  This element is what makes mixed-alphabet drawings compose and what
  keeps large-N "bus-as-symbol" wires usable next to gate-level logic.
- **Constant, Display, Truth table** — interval-aware; truth table
  budget-refused per §6.
- **Splitter/Binder/Extend on non-binary nets**: Splitter/Binder subset
  positions of a bundle (interval preserved); `Extend`'s replication is
  binary-only until a signed-extension semantics is decided (open
  question in `nary-03`).
- **No radix/interval attribute is added to any existing element type**
  (#361 invariant 1): every pre-existing type reports `[0,1]`, asserted
  over the registry. New capability arrives as new types, default-hidden
  in the palette (#361's TASK-0105 view dimension).

## 8. Invariants (carried forward, restated for the interval model)

1. `[0,1]` is byte-identical: the entire golden corpus passes unchanged
   at every landing.
2. No measurable warm-event-loop regression for binary circuits —
   benchmarked against a named baseline, not asserted.
3. The interval is validated, never widened — no `max`, no first-wins,
   anywhere, at any commit.
4. No drawable circuit can reach `interval != [0,1]` until the element
   family lands (safety property during migration).
5. The fast-tier and generic-tier kernels agree on a seeded differential
   corpus, enforced in CI from the first dual-tier commit.
6. Sequencing: the interval accessors are reserved **inside** the #322
   value-representation migration (see `nary-02` §4) — the single most
   important ordering rule, inherited from #344 §6.
