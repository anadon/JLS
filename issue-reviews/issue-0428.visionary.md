# Issue #428: TASK-0081: JLS-T3 exists as a written instruction set with a conformance corpus that decides whether an implementation obeys it — before either implementation is written
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Spec-before-implementation is the best single judgement anywhere in the ternary programme, and it is
cheap: no `src/` change, no element, no `.jls` byte, no CLI surface. Even if #345 and #295 were
descoped tomorrow, a written balanced-ternary ISA with a conformance corpus is a reusable artifact —
a lab handout, a #202 sibling, a spec other people can implement. That asymmetry (near-zero
reversibility cost, durable value) is why this task should be written *now*, ahead of the substrate
features it is nominally downstream of. I endorse the sequencing without reservation.

What I am reframing is the *content* of the document: what it specifies, how it states rules, and
what the pre-implementation checks are allowed to be checks *of*. Four of the reframings below are
architectural; one is a gap I believe blocks execution.

## Reframe 1 — the document should transcribe a published ISA, not invent one

§1 argues that a self-written oracle is fine because RISC-V's Sail and ARM's ASL were also written by
the ISA's authors. True, and irrelevant: those authors were specifying a machine that would be
implemented by thousands of strangers, which is the pressure that finds underspecification. JLS-T3
will be implemented by one person twice. §11 names this ("two implementations by one author") and
offers the corpus as the only mitigation — but a corpus written by the same author from the same
reading of the same document does not break the symmetry; it only records it.

Adopt Setun / Setun-70 instead (as the #345 review also urged), or any documented balanced-ternary
architecture. Consequences at *this* task's level, all of them good:

- Open Question 2 (`DIV` rounding on a non-power-of-three) and Open Question 3 (`MOD`'s sign
  convention) stop being preferences to defend and become lookups with a historical answer. Both are
  currently marked **blocks execution**, and both are exactly the kind of choice where a bespoke
  answer is indefensible-by-construction: whatever is picked, nobody can say it is wrong.
- H1's totality claim gets an external witness. Today "every case an implementer could resolve two
  ways is enumerable in advance" is checkable only by the author who enumerated them.
- Integration criterion "a third implementation could be checked" acquires a referent.

If the bespoke ISA is kept, the document should say plainly in its header that no third-party
implementation exists or is expected, so a reader does not mistake the corpus's *form* of
implementation-independence for the *fact* of it.

## Reframe 2 — replace P10 (no pseudocode) with a positive rule

P10 — "the document contains no pseudocode a Java implementer would copy verbatim into a circuit" —
is a negative, reviewer-judgement rule that buys less than it costs. A shared misreading travels
through prose at least as well as through pseudocode, and prose is the *more* ambiguous carrier, so
P10 works directly against H1's totality requirement: the one rule most likely to leave a case
undecided is the rule forbidding the most precise available notation.

The issue already demonstrates the right answer without naming it. §7.10 states `val`, `read`,
`SHR3` and `BR3` as **total mathematical functions**, and mathematics is copy-proof in the only sense
that matters — it has no implementation shape, so neither the Java author nor the circuit author can
transcribe it without doing the design work. Make that the rule:

> Every semantic rule is stated as a total function over trit vectors or over `val(w) ∈ ℤ`, with a
> declared domain and codomain, no imperative control flow, and no named local state.

That is positively checkable, it strengthens H1 instead of fighting it, and it removes a review step
(§8's last checkbox) that currently carries the "non-circularity guarantee" on one person's judgement.

## Reframe 3 — the encoding is not total, and the strongest pre-implementation check is missing

H2's claim is that meaningful checks exist before any implementation does. It is right, and the
predictions under-deliver on it. P1–P4 compare the document to its own manifest (hygiene: real, but
it is drift detection, not evidence the ISA is well-specified), and P5 round-trips only *corpus*
words — a sample of the encoding space checked against a decoder written from the same table.

The check that is actually available, and that no implementation is needed for: **decode totality and
injectivity over the whole space.** 3^16 = 43,046,721 words enumerate in seconds. Assert that every
legal word maps to exactly one of {a named instruction with named operands, a named illegal-encoding
trap}, and that no two instruction forms overlap. That is a theorem about the document, mechanically
checkable at spec time, and it is the difference between "the encoding table is complete enough to
implement from" (P5's stated intent) and "the encoding table is complete."

It is not hypothetical that this would find things. Reading §7.6's own table:

- **`BR3` has no encoding form.** R-form is `[op:3][rd:3][rs1:3][rs2:3][fn:4]`, I-form is
  `[op:3][rd:3][rs1:3][imm7:7]`. `BR3` needs `rs1` plus *three* offsets. Neither form can hold it. A
  third (B-)form is required and is not declared — and once declared, `op:3 + rs1:3` leaves 10 trits
  for three offsets, so each offset is ~3 trits: a range of about ±13 instructions. The ISA's
  identity instruction is therefore a *short* branch that needs a `JMP` trampoline for anything
  further, which is a real architectural consequence the document must state and P9 does not ask for.
- **The opcode arithmetic is internally inconsistent.** "3 trits of opcode = 27 primary opcodes; the
  1-trit function modifier in the R-form spare field gives 81" describes 27 × 3, but the R-form
  allots `fn:4` — 81 function codes, hence 2,187 R-form points. With ~28 instructions defined, the
  overwhelming majority of the encoding space is unassigned.
- **Unassigned encodings are absent from §7.11.** The failure table enumerates illegal *lane codes*
  (`00`) exhaustively, sixteen entries, one per lane — and never says what an unassigned *opcode*
  does. That is the single most common trap in any real ISA and it is the one undefined case the task
  forgot, in a task whose thesis is that no case is left to the implementation.

## Reframe 4 — the mixed-radix boundary is the address path, not `LDB`/`STB`

§7.6 asks the document to say "plainly" that `LDB`/`STB` are "the mixed-radix boundary inside the
CPU." I think that names the wrong boundary and hides the expensive one.

Memory is "byte-addressed, little-endian, 32-bit words, **addresses are binary**," and a pointer
lives in a ternary register. So every `LDW`/`STW`/`LDB`/`STB` must turn a balanced-ternary word into
a binary index — and if the address is `val(w)`, that is a 16-lane weighted sum over powers of three:
a ×3-plus-add chain roughly fifteen stages deep, per access. That block is plausibly larger than the
whole ALU, and it lands on TASK-0083, which is the most expensive task in the feature. The document
must decide it, and §7.11 does not list it.

The alternatives are genuinely different machines, and picking one here is the highest-leverage
sentence this document can contain:

1. **`address = val(w)`** — clean semantics, honest pointer arithmetic, one shared converter block.
   Name the converter as a *teaching artifact*: "the radix boundary is a real drawable circuit and
   here is what it costs" is a better lesson than any byte-lane story, and it is the one thing a
   student cannot learn from a binary machine.
2. **Address space of 3^k words** with the trit vector mapped lane-wise onto the `Memory` element's
   binary address port — no converter, but `+1` on a pointer is no longer `+1` on the address unless
   the mapping is the identity on `val`, which it is not. This is the trap, and the document should
   say so rather than let TASK-0083 discover it.

Whichever is chosen, `LDB`/`STB` are a value-width boundary, not a radix boundary; the address path
is where the radices actually meet.

## Reframe 5 — layer the ISA, and let the corpus's schema be dictated by the circuit

Two smaller cuts that make everything downstream cheaper:

**Profiles.** ~28 instructions in seven families is a *systems* ISA, and the binary-interop family
(`BAND BOR BXOR BSHL BSHR BMUL64H`) plus `LDB`/`STB` exist almost entirely to make `char`, strings
and a console work — i.e. to serve TASK-0084's monitor, which the #345 review recommends replacing
with a self-check ROM and three demo programs. Cut the monitor and that whole family loses its
client. Specify **JLS-T3 Core** (`ADD SUB NEG MIN MAX INV LIT SHL3 SHR3 LDW STW JMP JAL BR3 HLT`) as
the conformance target TASK-0083 must meet, and everything else as a declared extension. RISC-V's own
base-plus-extension device is exactly this, the corpus tiers already have the shape for it, and it
means the drawn machine can be *conforming* while being roughly half the size.

**Corpus format.** The corpus is a TSV parsed by `test/jls/mach/t3/IsaSpecCorpusTest` — a Java test,
on the emulator's home turf. The drawn machine cannot eat that; it eats `-t` test vectors and reports
through watched elements, per `docs/batch-interface.md`, which is a documented stability contract and
is precisely how `riscv/verify.py` compares a drawn RV32I core against its reference today. Choosing
the schema for the Java side is how the emulator quietly becomes the corpus's reference frame — the
exact failure §11 warns about, relocated from the spec into the harness. Require the manifest to
carry enough structure to generate **both** projections (JUnit rows and `-t` vectors against a
declared pin map), or define the `-t` projection as primary, since the circuit is the constrained
consumer.

## Alignment with the project's arc

Honest ledger. In favour: JLS is "an educational digital logic circuit editor and simulator" (README
line 5), and a drawn machine that is not binary is squarely within that. `riscv/` proves the
spec-plus-independent-reference-plus-drawn-machine method works in this repository.

Against, and the task should acknowledge it: **`docs/` contains no occurrence of "ternary" at HEAD.**
The capability roadmap's own values-and-logic sweep (`sweep-01`, V1–V8) is entirely four-state binary
and IEEE 1164; multi-valued radix is not on it. `docs/plan/**`, `docs/parity-contract.md` and the
`BRIEF.md` §13 that carries Decision D10 do not exist in the working tree, so §1's D10 argument,
§6's `docs/parity-contract.md` §3 citation and the Definition of Done's "every cited evidence
document resolves on the default branch" fail together for any reader of this repository. Two smaller
notes: "the binary semantics this ISA collapses to at radix 2" is loose — a *value type* collapses at
radix 2; a balanced ISA does not (balanced base 2 is non-adjacent form, not binary). And O4's "16 MiB
with zero headroom" is a fact about `DENSE_CAPACITY_LIMIT = 1 << 22` (`src/jls/elem/Memory.java:1224`)
that I confirm, but it is a *ceiling*, not a target; Core needs kilowords, and sizing the memory model
to exactly fill the dense store is the tail wagging.

## What I would not touch

The spec-first cut; the corpus-as-data-with-a-manifest; the four T-null models and especially (d),
the `00`-read-as-`0` model, whose separating argument in §7.10 (the two readers agree on the entire
image of β) is the sharpest reasoning in the issue; the insistence that the T-null tier assert report
*text* rather than a boolean; pinning the generator and not just the seed; and H1's refutation
protocol — an undecided case comes back here and is fixed with a version bump, never resolved inside
an implementation. That last rule is what makes the whole two-implementations argument mean anything.

## Verdict

**endorse-with-reframing.** Write the document first — that is right and it is the cheapest correct
thing in the programme. But I am explicitly disregarding two stated criteria: **P10** (replace "no
pseudocode" with "every rule is a total mathematical function"), and the **~28-instruction, seven-
family scope** (specify a Core profile as TASK-0083's conformance target and demote binary interop
and byte access to an extension that ships only if the monitor does). Before execution, three things
must be added that the issue does not have: a declared **B-form** for `BR3` with its offset range and
trampoline consequence; a decision on the **ternary-word-to-binary-address** path, with its cost named;
and **unassigned encodings** in the §7.11 table with a defined trap. Then make P5 a totality-and-
injectivity check over the full 3^16 space rather than a round-trip over corpus samples — that is the
theorem H2 promises, and it is available for free before a line of either implementation exists.
