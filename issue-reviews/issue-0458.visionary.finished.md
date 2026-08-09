# Issue #458: TASK-0082: JLS-T3 gets an independent in-jar emulator, assembler and disassembler — the counterparty a drawn ternary CPU can be wrong against
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Three purposes are bundled here and they are not equally load-bearing:

1. **Give TASK-0083's drawn ternary CPU something to be wrong against.** This is the real one, and the issue's central instinct — an independent, deterministic, pure-function oracle in the jar — is correct and squarely on JLS's trajectory. The single self-contained offline jar is the deployment model the README builds its whole install story on; an assembler that shelled out would break it. Endorsed without qualification.
2. **Give students a way to write T3 programs.** Also real, but it is a *program-artifact* problem, not a *file-format* problem, and the issue solves it as the latter.
3. **Prove `RetireRecord` is ISA-parameterized (#347 IC-6).** This is the weakest justification attached to the largest artifact. #347 already says this is its own close-out criterion and that TASK-0082 "provides the second counterparty but its own criteria are about the emulator." It should not shape this task's data contract, and right now it does (see §3).

The trajectory this joins — README, ARCHITECTURE.md — is: textual formats with typed rejection, table-driven loading with no per-element switch, a documented batch/grading contract, one offline jar, external toolchains kept behind subprocess boundaries. The issue honours the jar constraint and the serialization ban. It pulls against the other three, in three places that are each a cheaper design if cut differently.

## 1. `.timg` is the wrong artifact, and the right one already ships

The issue cites `ArchitectureRulesTest` (O4) as authority for a *byte* grammar. Read the quoted `.because()` clause again: *"network and file payloads are textual save-format grammar with typed rejection, never Java object serialization."* The rule says **textual**; the issue reads it as **not-`ObjectOutputStream`** and then mints magic bytes, a spec-version field, a section table, and a hand-rolled per-field rejection layer that duplicates what `LoadError`'s taxonomy (ARCHITECTURE.md, "Error-reporting contracts") already does for every other payload JLS reads.

Worse, it cuts the seam in a place that damages the parity claim this task exists to serve. `src/jls/elem/Memory.java:817` (`initOK`) already defines JLS's memory-image format: `#`-commented lines of `<hex address> <hex value>`, with per-line diagnostics. `Memory` carries both an `initFile` name and an `init`/`initrle` save attribute (`docs/file-format.md:307`). **TASK-0083's drawn machine has no other way to get a program into its ROM.** So `.timg` guarantees two loaders for one program: the emulator reads `.timg`, the circuit reads memory-init text, and every disagreement between those two paths is either a false parity failure or — much worse — an agreement that hides a real one.

**Reframing: the assembler's output artifact *is* JLS memory-image text.** Consequences, all good:

- The oracle and the drawn machine execute literally the same bytes, loaded once. The independence being asserted is independence of *implementation*, not of *program loading*; making the program path shared strengthens H1 rather than weakening it.
- §7.7's whole migration story, §7.11's four `.timg` failure modes, and the "refused by name" version policy (Open Question 2) evaporate. If a version stamp is wanted, a required leading `# jls-t3 v1` line is a comment to `Memory.initOK` and a checked header to the emulator — a version with zero new format.
- Assembled programs diff in git, which is the exact property the README argues for when it recommends `-savetext` plain-text saves.
- Entry point and sections are `.org`/label conventions in the assembly source, not header fields in a binary container.

**I am explicitly disregarding the DoD line "`.timg` is a byte grammar with typed rejection at every header field"** and Open Question 2 with it. They are answers to a question that should not be asked.

## 2. One encoding table, three views — not two hand-written inverses

P8 makes assemble→disassemble round-tripping an acceptance test, which treats the two directions as independently written functions that happen to agree. JLS's own precedent is the opposite: `Circuit.load` has no per-element switch and resolves through the frozen table in `src/jls/elem/SaveTags.java:110`.

**Reframing: a single declarative encoding table (mnemonic, form, field offsets and widths, operand kinds) is the source; assembling, disassembling and decoding are three views over it.** Round-trip fidelity becomes a property of the table rather than a coincidence, and the table is small enough to be read against §7.6's field layout in one sitting.

The stronger version: #428's P5 already requires *"a pure decoder written from the field table in the document."* That decoder and this task's decoder should be **the same object**, driven by the same table — ideally parsed out of the spec's encoding table, which would upgrade #428's drift check from mnemonic-set equality to field-level totality. Two decoders written from one document, one in the corpus test and one in the emulator, is a drift surface introduced for no reason inside a task whose entire thesis is non-circularity.

## 3. Make the retirement record radix-native, not BET-shaped

§7.6 fixes the T3 record as `{order, pc_before, pc_after, insn_word, rd_index, rd_value, trap}` with `insn_word` and `rd_value` as raw 32-bit BET words, and Open Question 4 defends that. This bakes an *encoding* decision into the *comparison alphabet* before #345's Open Question 1 (native radix 3 vs. binary-encoded ternary for the drawn datapath) has been answered — and #345 says explicitly that this decision "must be made before TASK-0083 starts, not inside it." #428 settles BET for the *word encoding* and leaves the *datapath* open.

If TASK-0083 draws a native-radix datapath, the drawn side must synthesize a BET view for no reason except to be comparable. That is a value field carrying an encoding artifact — structurally the same hazard H2 fears in the timing direction, and §10's remedy for it ("narrow the comparator, not the record") does not apply, because the problem is in the record.

**Reframing: architectural values in the record are trit vectors (an opaque `TritWord` value type), and `Bet` is a codec at the memory/wire boundary only.** Costs nothing today, survives either answer to Open Question 1, and makes `illegalLanes` what it should be: a property of a *boundary read*, not of an architectural value. Note also that `Bet` and the illegal-lane discipline are load-bearing *only under the BET fallback* — a fact this task should state, since it currently reads as though BET were the ISA itself.

Related: this issue asserts P11 by reflection over `RetireRecord`'s components and proposes its field set, but #390 (TASK-0072) owns that type's mechanism and appears only in `related`. A task that proposes the shape of another task's type is depending on it. That edge should be real.

## 4. Four nulls measure four rules; the project already has the tool that measures all of them

The T-null models are #428's contract and should ship. But "the harness can detect a knowingly wrong implementation" generalizes badly from four hand-picked subclasses, and JLS already ran the general version: `docs/mutation-testing-trial-2026-07.md` records a PIT trial reaching an adopt verdict, with a measured 39 % mutation score against 58 % line coverage on the core — precisely the "reach without strength" gap the nulls are groping at.

**Concrete addition, near-free:** run PIT over `jls.mach.t3.*` with the conformance corpus as the suite and record the mutation score in the PR next to §9's coverage fraction. That number is what makes "the corpus is strong" checkable across *every* rule, and it is a far better answer to §11's "if the corpus is thin, this task's green run is thin, and the weakness is invisible from here" than anything currently in the plan. The four nulls stay as the report-text contract; the score is the breadth measure.

## 5. Two smaller reframings

- **`neg()` orientation.** §10 H4 says "the swap is the definition and the arithmetic form is the check." For an *oracle*, that is backwards: the oracle's primary implementation should be the one most obviously derived from the spec's value map, i.e. the arithmetic form, with the plane swap checked against it. The plane-swap-as-zero-gates argument is a *drawn-machine* argument and belongs to TASK-0083's timing story, which §7.10 Stage 3 admits. Keep both implementations; invert which one is normative here.
- **The combinational corpus does not need Java at all.** The 9- and 81-case exhaustive tiers over ternary ops are truth tables. `docs/batch-interface.md` §2 already specifies a normative, student-runnable test-vector grammar driven by `-b -t`, and #369/FEAT-053 is heading there anyway. Running those tiers against a drawn ternary ALU through the existing batch interface costs no new harness and gives a student something they can run on their own circuit the day they draw it. The Java emulator is then needed only for the *sequential*, per-retirement half — which is a smaller, sharper artifact and an easier one to keep pure.

## 6. Sequencing

The critical path is spec → record mechanism → package → emulator. Two of those four are not filed: **TASK-0070 does not exist as an issue**, and its ownership is genuinely ambiguous (#343 FEAT-033 calls it "the `jls.mach` leaf package and reference-runner seam"; #347 lists TASK-0070 on its own roster; this issue calls it a blocker and notes it is unfiled). #390 is depended on in substance and listed as related. The highest-value next action is not to start this task — it is to file TASK-0070 and settle #390's record shape, because §7.6 and P11 are proposals about someone else's type and §7.12's "no grace period" coverage floor is someone else's rule.

## Verdict

**endorse-with-reframing.** The core is right and belongs: an in-jar, subprocess-free, single-threaded, pure-function oracle whose codec never hides an illegal lane, whose `step` has no loop and no timing, whose traps name their rules, and whose assembler reports every malformed line at once. Keep all of that. Re-cut three deliverables — program artifact as memory-image text rather than `.timg`; asm/disasm/decode as three views of one table shared with #428's decoder; record values as trit vectors rather than raw BET words — and add a mutation score beside the four nulls. That removes an entire file format, an entire hostile-input surface, an entire versioning-and-migration story, and one loader divergence sitting directly under the parity claim, while making the result survive a decision (#345 OQ1) that has not been taken yet.
