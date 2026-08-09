# Issue #439: TASK-0013: memory capacity is a byte budget with stated headroom, and initializing a memory stops allocating a second copy of it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and the ask is one sentence: *a drawn RV32I machine with realistic guest
RAM should fit in a student's JVM, and the number that decides whether it fits should be
honest.* That goal is squarely on the project's arc — #202/#326's drawn machine, #364's
"byte lanes on Memory", #354's unannounced-ceiling work all need it. I am endorsing the
goal. I am rejecting both mechanisms the issue picks to reach it, because a single
different seam reaches the goal further, deletes most of this issue's own surface, and
makes four of its five Open Questions stop existing.

## The two mechanisms, and why each is the wrong shape

### 1. The byte budget budgets against the defect instead of removing it

O1 is the sharpest observation in the issue and the issue then walks past it. `bits` does
not appear in the size arm of the predicate — but it also does not appear in the
*representation*. `DenseWordStore` is `long[capacity]` plus `BitSet(capacity)` whatever
`bits` is (`src/jls/elem/Memory.java:1074-1087`), so a 4-bit memory pays 8.125 bytes for a
half-byte word: **16.25x its guest size**. The issue measures this (O3: identical
34,078,720 across bits ∈ {4,32,64}), names it as the reason the *decision* is wrong, and
then keeps `B_dense(n) = 8.125n` as a fixed law of nature to size a budget against.

Make the representation `bits`-aware and the arithmetic collapses. Choose the backing
primitive from `bits` — `byte[]` for ≤8, `short[]` for ≤16, `int[]` for ≤32, `long[]` for
≤64 — and the 16 MiB 32-bit guest costs 16,777,216 + 524,288 = **17,301,504 bytes**
instead of 34,078,720; the 4-bit memory costs 2 MiB + presence instead of 34 MB. §7.10's
$B_{\mathrm{dense}}(G) = 2.03125\,G$ becomes $\approx 1.03\,G$. The issue's recommended
$B_{\max} = 68{,}157{,}440$ with $h = 2$ would then admit a **64 MiB** 32-bit guest at the
same heap, without a headroom argument, without a system property, and without a report.

This also unifies with the sibling task. FEAT-036 is *titled* "byte lanes on Memory" and
implements byte lanes nowhere in the representation. TASK-0076's masked write
($s' = (v \wedge M(m)) \vee (s \wedge \lnot M(m))$, #364 §3) is, over a `byte[]`-lane
store, a per-lane array write; over today's store it is a `BitSet` → `long` → mask →
`BitSet` round-trip. One representation change makes the feature's own headline capability
fall out.

### 2. The all-or-nothing overlay expires on the first store instruction

§7.10 stage 2 states the post-`initSim` heap as $B_{\mathrm{dense}}(n) + O(1)$. It never
states the heap after the first `put`, and §7.8 says the overlay's own backing is
"materialized on the first `put`". So there are two unstated outcomes: the delta is dense
(heap returns to 68,157,440 — the saving is gone) or the delta is a map (a machine writing
4M words pays ~100 bytes/word, far worse than today). Open Question 4 asks whether COW
backs *sparse* initializations; nobody asks what $D$ **is**.

This matters because of who §Intended Audience names: "students and instructors running a
drawn processor with realistic RAM." For that user the first write is the first simulated
store instruction, microseconds into the run. The measured 34,078,720-byte win therefore
accrues, in full and forever, to memories that are **never written** — `Type.ROM`, which
`printChangedValues` already special-cases at `:905` — and to RAM it accrues for one
instruction. P3 and P4 as written cannot detect this: they check sharing before one write
and non-sharing after it. The acceptance criteria are satisfiable by a design that delivers
nothing to the audience the issue was written for.

### 3. `initMem` does not need to exist for the whole run at all

I traced every reader. After `initSim`, `initMem` is read in exactly one place:
`printChangedValues` (`Memory.java:909-932`), reached from
`JLSStart.displayResults` (`:681`) once at end of run for watched memories. That is a
**report**, not a simulation path.

So the honest statement of O2's defect is not "initialization clones eagerly" — it is *JLS
keeps a fully materialized 34 MB structure alive for an entire multi-hour run in order to
serve one diff that runs once, when the encoded image it was built from is still sitting in
the element as `initialValue`/`fileName`, already bounded by `MAX_INIT_WORDS`.* Re-decode it
when the report is asked for and the halving is **permanent** rather than expiring at the
first write — and the issue's entire risk surface goes with it: no shared mutable base, no
"read-only after initSim" javadoc obligation (§7.9), no §7.11 "one new corruption path", no
`sharesBackingWith` witness (§7.5), no H3 to refute.

Better still: if `mem` carries a delta, `printChangedValues` enumerates $\operatorname{dom} D$
instead of every present address. Today it walks all 4.19M addresses of a fully-initialized
guest and allocates two `BitSet`s each (`:921`, `:932`) to discover which ~thousand changed.
The delta makes that report O(writes) and preserves the output exactly — an echoed write
still lands in $D$ and is still filtered by the `equals` check, so
`printChangedValuesReportsNoChangesForAnEchoedWrite` stays green. The overlay's real value
is that it is the missing *"what did this run change"* model, which `docs/batch-interface.md`
already contracts; the heap saving is a side effect.

## The concrete alternative: one paged store, no decision site

Replace `DenseWordStore` + `SparseWordStore` + `DENSE_CAPACITY_LIMIT` + `newWordStore()`'s
predicate with **one** `WordStore`: a page table (`Object[]`, `null` = untouched) over
fixed-size pages, each page a `bits`-selected primitive array plus its own presence
`BitSet`, allocated on first touch. `WordStore`'s four methods (O8) are exactly the seam;
nothing outside `Memory` sees it.

| regime | today | #439 as written | paged, bits-aware |
|---|---|---|---|
| 16 MiB 32-bit guest, untouched | 34 MB × 2 | 34 MB | ~0 |
| same, program touches 1 MiB | 68 MB | 68 MB (post-write) | ~1 MB |
| same, fully written | 68 MB | 68 MB | ~17 MB |
| 4-bit, 4M words | 68 MB | 68 MB | ~2.5 MB |
| 64 MiB 32-bit guest | sparse cliff, ~100 B/word | sparse cliff | ~66 MB, no cliff |

Copy-on-write becomes **page-granular and permanent**: `mem`'s table starts pointing at
`initMem`'s pages; a write copies one 256 KiB page and writes into it. A long run pays only
for the pages it wrote — which is what the issue's §Abstract actually promises and its
design cannot deliver.

What disappears: the budget constant, the headroom factor $h$, the system property, the
malformed-property fallback path, the fallback report and its channel, the
`sharesBackingWith` witness, and Open Questions 1, 2, 3 and 5. H4 becomes vacuous — there is
no fallback left to accidentally turn into a validity rule, so
`DialogValidationTest#memoryCapacityRuleIsOneStringOnTwoSurfaces` cannot be tripped by this
work at all. What survives unchanged: O6's ascending `addresses()` union, O7's
empty-`BitSet` truncation, `MAX_INIT_WORDS`, every saved byte, and `#232`'s ownership of the
per-read `BitSet` signature (a packed store makes #232's eventual fix *easier*, since the
value is already a primitive).

I am explicitly disregarding these stated acceptance criteria: §14's "the budget constant's
javadoc states $B_{\max}$, the headroom factor, the resulting word count", Open Question 1's
blocking status (mirrored to #354), and #364 §5 criterion 3's structural sharing witness.
Under paged COW there is no budget constant to document, no headroom to declare, and sharing
is a continuous per-page property rather than a one-shot boolean — the honest witness is
"pages allocated by this store", which is also a number the resource report below wants.

## Two smaller reframings, valid even if the paged store is rejected

**The budget, if one must exist, is `Runtime.maxMemory()`, not a new property.** A compiled
68,157,440 is correct on exactly the machine it was chosen on; the same jar runs on a 512 MB
autograder container and an 8 GB workstation. The knob that already exists and that every lab
and CI image already sets is `-Xmx`. Deriving $B_{\max}$ as a fraction of the heap the JVM
was actually given is self-tuning, needs no documentation of a magic number, and is safe
*precisely because* the issue proves the store choice is observationally neutral (P4, P9).
Adding `jls.memory.dense.budget.bytes` beside `jls.laf` and `jls.toolkit` puts a
storage-strategy tuning knob in a user-facing namespace that no student will find and no
instructor will set.

**The fallback report belongs to a run-level resource summary, not to `newWordStore`.**
Open Question 3 is visibly struggling — `TellUser` is GUI-coupled, stderr collides with the
CLI contract (ARCHITECTURE.md "Error-reporting contracts"), an element diagnostic is
load-time machinery being borrowed for an elaboration-time fact. That difficulty is the
signal: JLS has no place to say *what this circuit costs*. #354's "no unannounced ceiling" is
asking for exactly that surface. Build one resource line (memory backing bytes, element
count, history retention) that batch mode emits and a golden can assert, and this issue's
report is one field in it rather than a second, memory-only diagnostic channel.

## Trajectory check

ARCHITECTURE.md's recorded #221 decision names the revisit trigger for a second simulation
strategy: "a concrete CPU-scale design on the riscv/ trajectory that is unusably slow
interactively." Worth asking before funding 1.5 weeks here: for #202, is heap the binding
constraint, or is it simulated cycles per second? 34 MB is not what stops a 16 MiB guest on a
2026 laptop; event throughput and the per-read `BitSet` churn (#232 — which this issue
explicitly refuses to touch) plausibly are. If the ceiling that actually blocks the audience
is throughput, #232 is the higher-leverage sibling for the same users, and the paged
bits-aware store above is the change that helps both at once. The issue's cost/benefit should
be argued against that alternative, not against the status quo.

Minor grounding note: `docs/machine-calibration.md` — the source of the 16 MiB named target
image the whole budget is sized against — is not present in this checkout's `docs/`. A budget
whose headroom is declared over a document that does not resolve on the default branch fails
§14's own "every cited evidence document resolves at close" criterion before work starts.
