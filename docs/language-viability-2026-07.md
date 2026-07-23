# Language & toolkit viability for a JLS migration (July 2026)

*Should JLS be recoded in another language — C++ (compile-time optimization),
Rust (typing sophistication), Haskell (longevity/correctness), Agda (verified
software), or some other point on the Pareto curve? This document answers that
with a structured comparison rather than a preference. It complements the
recorded no-rewrite decision (#96) and the forward architecture
([`grand-architecture.md`](grand-architecture.md)) by showing the **work** behind
the verdict: a full-rewrite comparison matrix over eight migration-impact
dimensions, plus the measured performance evidence that motivated the question.*

## Scope & revisions

- **Framing: full rewrite vs. stay.** Each non-Java candidate is evaluated as a
  *strict full rewrite*; the Java 25 row is the baseline (do not rewrite —
  modernize in place per #96). A partial "extract only the sim kernel via FFI"
  path is **out of scope** for this study.
- **2026-07-23 revision.** Two dimensions from the initial ten were dropped at the
  maintainer's direction and one was re-scored:
  - **contributor-pool** — removed; not a concern for this project.
  - **kernel-only-path** — removed; only a full rewrite is in consideration.
  - **deployment** — re-scored *without* riscv64 as a required target
    architecture (see the deployment re-score table below).
  The determination is unchanged by these edits (it never rested on those axes),
  but the margins narrowed and several native candidates rose; both are recorded
  below honestly.

## Why this exists

A firmer answer than "no" was asked for, specifically on performance grounds
(the claim that Rust is ~10× Java on
[programming-language-benchmarks](https://programming-language-benchmarks.vercel.app/rust-vs-java)).
This document is the firmer answer. Its two load-bearing results:

1. **The "large circuits are slow" bottleneck is algorithmic, not linguistic** —
   and every realistic performance win is capturable in pure Java. Measured
   below, and filed as #231 and #232.
2. **Across 14 candidate languages scored on eight dimensions, staying on Java 25
   and modernizing in place (#96) is the global optimum** — a Pareto frontier
   point that strictly dominates 8 of the 13 alternatives outright, including
   the runner-up, and leads the field by 7 points on a 40-point scale.

## Method

Candidates were enumerated (the four named languages plus Kotlin, Scala 3,
C#/.NET, OCaml, Zig, Go, Swift, Nim, Lean4, and a "Java 25, modernize in place"
baseline so the no-migration option competes on equal footing). Each was scored
1–5 on dimensions anchored to JLS's actual constraints — **not** the language in
the abstract — then every analysis was passed through an adversarial verification
step charged with catching both over-optimism (hand-waving the GUI
reimplementation cost) and over-pessimism, with score corrections folded back in.
A final synthesis produced the matrix and determination. This is an AI-assisted
study; the numbers are judgments, but the performance findings (§"The performance
finding") are measured and reproducible, and the structural facts (LOC, Swing
coupling, contracts) are drawn from the tree at commit `02dbd3c`.

The eight scored dimensions (after the 2026-07-23 revision): **sim-core-fit**,
**gui-maturity** (for a dense hit-tested schematic canvas — where ~47% of the
code lives), **typing-gain over modern Java-25** (sealed + records +
JSpecify/NullAway, i.e. the #93–96 program), **memory-safety**, **deployment**
(single self-contained offline artifact across amd64/arm64 + Windows/macOS/Linux;
riscv64 *not* required), **verification-affordance**, **ecosystem-longevity**, and
**contract-and-effort** (parity effort + risk of regressing the byte-exact
`.jls`/batch/VCD contracts).

## The comparison matrix

Scores are 1–5 (5 best **for JLS's objective**), verifier-adjusted, with
deployment re-scored per the revision. Total is out of 40.

| Language | sim-core | gui | typing-gain | mem-safe | deploy | verif | eco | contract | **Total** | Full rewrite |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|---|
| **Java 25** (Swing+FlatLaf) | 5 | 4 | 2 | 5 | 5 | 4 | 5 | 4 | **34** | baseline — modernize in place (#96) |
| **Kotlin** (reuse Swing) | 3 | 4 | 2 | 5 | 4 | 3 | 3 | 3 | **27** | uneconomic |
| **C#/.NET** (Avalonia) | 4 | 3 | 2 | 4 | 4 | 3 | 3 | 2 | **25** | uneconomic |
| **Scala 3** (reuse Swing) | 3 | 3 | 3 | 4 | 4 | 3 | 2 | 1 | **23** | uneconomic |
| **Haskell** (monomer) | 3 | 2 | 3 | 5 | 3 | 4 | 2 | 1 | **23** | not-viable |
| **OCaml** (lablgtk3) | 4 | 2 | 3 | 4 | 2 | 4 | 2 | 1 | **22** | not-viable |
| **C++** (Qt QGraphicsView) | 5 | 4 | 1 | 1 | 3 | 3 | 3 | 1 | **21** | uneconomic |
| **Rust** (egui/Slint) | 4 | 1 | 2 | 3 | 4 | 4 | 2 | 1 | **21** | uneconomic |
| **Lean4** (none) | 2 | 1 | 5 | 2 | 3 | 5 | 2 | 1 | **21** | not-viable |
| **Nim** (gintro/GTK4) | 4 | 2 | 2 | 3 | 3 | 2 | 2 | 2 | **20** | uneconomic |
| **Go** (Gio) | 3 | 2 | 1 | 4 | 3 | 2 | 3 | 1 | **19** | uneconomic |
| **Agda** (none) | 1 | 1 | 3 | 5 | 1 | 5 | 2 | 1 | **19** | not-viable |
| **Zig** (Dear ImGui) | 5 | 1 | 2 | 2 | 3 | 2 | 1 | 1 | **17** | not-viable |
| **Swift** (SwiftGtk/Adwaita) | 3 | 1 | 2 | 3 | 2 | 2 | 2 | 1 | **16** | not-viable |

### Deployment re-score (riscv64 no longer required)

| Language | was | now | reason for the change (riscv64 removed) |
|---|:--:|:--:|---|
| C#/.NET | 2 | **4** | riscv64/NativeAOT gap was *the* driver of the 2; NativeAOT single-file self-contained is clean on amd64/arm64 + Windows/macOS. |
| Rust | 3 | **4** | removes the software-render-only riscv64-GUI caveat; clean, reproducible static binaries elsewhere. |
| C++ | 2 | **3** | drops the riscv64 Qt-toolchain burden; still loses the single-jar model to N per-arch builds + a full supply-chain rebuild. |
| Haskell | 2 | **3** | removes the shaky unregisterised-riscv64-GHC hard failure; native binaries are workable on amd64/arm64. |
| Lean4 | 2 | **3** | removes the unproven riscv64 leg; Lean emits C then a self-contained native binary. |
| Swift | 1 | **2** | removes the riscv64 hard blocker; Linux/Windows Foundation+runtime bundling stays weak. |
| Java 25 | 5 | 5 | unchanged — riscv64 was a bonus, never the reason for the 5. |
| Kotlin, Scala 3 | 4 | 4 | unchanged — single-jar preserved regardless; cost was stdlib size. |
| OCaml | 2 | 2 | unchanged — riscv64 backend already worked; penalty is the heavy system-GTK runtime dependency. |
| Nim, Zig, Go | 3 | 3 | unchanged — riscv64 was fine (Nim/Zig) or only helped the headless core (Go's Gio GUI is cgo, per-platform). |
| Agda | 1 | 1 | unchanged — a build-time proof artifact, not a deployable application. |

## Ranking, by fitness for JLS's objective

1. **Java 25 (34)** — the incumbent *is* the target, so "full rewrite" collapses
   into modernize-in-place. Captures every language-independent performance win
   with zero rewrite, keeps the 47%-GUI surface, the single-jar supply chain, and
   the 36k-LOC executable-spec test suite, and adds #93–96 as compiler-enforced
   properties. The recorded #96 decision, and the correct one.
2. **Kotlin (27)** — the only non-Java candidate that keeps Swing verbatim in one
   JVM jar and lets the tests stay Java; but it buys essentially nothing over
   Java-25 (same JIT ceiling, marginal typing gain) while adding a second compiler
   to a solo Java build.
3. **C#/.NET (25)** — the biggest beneficiary of the revision (deployment 2→4 once
   riscv64 is dropped) and now the strongest non-JVM option. Genuinely strong
   sim-core fit and Java-familiar; but a full rewrite still re-engineers the entire
   JVM-shaped supply chain and rebuilds the working Swing GUI for a ~1.5–3×
   constant it doesn't need.
4. **Scala 3 (23)** — smoothest JVM rewrite mechanically (records/sealed native,
   tests stay executable), but delivers no performance-ceiling gain and adds a
   version-locked runtime to the shaded jar/SBOM.
5. **Haskell (23)** — elegant value/event semantics, memory-safe, strong
   verification; but monomer is immature and embedding the GHC RTS is a hostile
   boundary. Purity is an aesthetic, not a product, win here.
6. **OCaml (22)** — idiomatic discrete-event style and good verification
   affordance, but lablgtk3 is generations behind Swing and the 63-bit tagged int
   reintroduces boxing in exactly the hot path #94 is meant to de-allocate.
7. **C++ (21)** — the best raw substrate (Qt `QGraphicsView` is a superb
   dense-canvas engine), but surrenders Java's memory safety to UB and rebuilds the
   whole product for a performance win that is algorithmic and Java-capturable.
8. **Rust (21)** — highest-scoring native candidate on the axes that matter for a
   simulation core (memory-safety, verification), but pre-1.0 GUI (egui/Slint) and
   borrow-checker-versus-cyclic-graph friction make the full rewrite uneconomic;
   the residual is a 2–3× constant.
9. **Lean4 (21)** — unmatched typing/verification, but literally no desktop GUI
   target for 47% of the code and a proof-assistant runtime; interesting only as a
   verified sim kernel, which this full-rewrite framing excludes.
10. **Nim (20)** — small native binaries and clean C interop, but no mature GUI for
    the dense canvas and a small high-churn ecosystem make a full rewrite
    indefensible.
11. **Go (19)** — single-binary and memory-safe, but regresses the type system
    *below* Java-25 (no sealed sums, no records, nil hazards) and Gio is a fraction
    of Swing's maturity.
12. **Agda (19)** — not an application language (no GUI, no production runtime). It
    earns its place *only* by extending the existing `SpatialIndexCorrectness`
    surgical-proof pattern to simulation invariants — verification, not migration.
13. **Zig (17)** — an excellent systems substrate, but 0.x churn and no
    retained-mode GUI for a GUI-first product, and the performance problem it would
    address is gone once the Java fixes land.
14. **Swift (16)** — no toolkit both mature and cross-platform, weak Linux/Windows
    deployment, and Apple-governed churn is the worst 10–20-year bet.

## Pareto analysis

**Frontier points** (non-dominated):

- **Java 25 (34)** — the global optimum: top or joint-top on 6 of 8 axes and the
  highest total. Its only sub-maximal axes are typing-gain (2) and gui (4).
- **Scala 3, OCaml, Haskell, Agda, Lean4** sit on the frontier *only* via
  typing-gain > 2 (Java's single weak axis) — i.e. typing/verification specialism.
  None is product-decisive for JLS, and all lose decisively on total; Java
  neutralizes their edge through the surgical Agda/ProofBridge prong it already
  runs.

**Strictly dominated by Java 25** (Java scores ≥ on all eight axes): **Kotlin,
C#/.NET, C++, Rust, Nim, Go, Zig, Swift** — 8 of the 13 alternatives, unchanged
from the ten-dimension version. The recurring reason survives the re-weighting:
the axes where these languages *could* win — raw throughput, memory footprint —
are non-binding (see below), while the axes they lose — GUI maturity for this
canvas, contract-preservation effort, ecosystem longevity — are where JLS's value
concentrates. Dropping contributor-pool and riscv64 narrowed the totals but moved
no candidate off Java's domination set.

## The performance finding

**The "Rust is 10× Java" motivation does not survive contact with the measured
evidence, and cannot justify a language change for JLS.** The headline slowness
is algorithmic:

- `SimEvent.hashCode()` returns `(int)time` (`src/jls/sim/SimEvent.java:107-110`)
  and the simulator's dedup `HashSet` is keyed on it
  (`src/jls/sim/Simulator.java:27`). A synchronous clock edge posts thousands of
  events at one tick → every event collides into one bucket → `add`/`remove`
  degrade to O(k) → the dedup set is **O(events-per-tick²)**. Measured on a
  faithful model (JDK 25, warmed): 32k events/tick spends **11.5 s** in the
  shipped hash vs **20 ms** with a mixing hash — a **582×**, quadratic-curve win
  that is **free and stays in Java**. A Rust port copying `hash = time` would be
  equally quadratic. Filed as **#231**.

| events/tick | shipped `hash=time` | mixing hash | ratio |
|---|---|---|---|
| 1,000 | 4.3 ms | 0.7 ms | 6× |
| 4,000 | 67 ms | 2.2 ms | 30× |
| 16,000 | 2,086 ms | 6.1 ms | 345× |
| 32,000 | 11,534 ms | 19.8 ms | 582× |

**The speedup ladder** — largest first, all Java-native except the last:

1. **Mixing hash** removes an O(n²) term — free, Java (#231).
2. **Value-typed `(long,width)` signals** kill the per-change `BitSet`
   allocation/GC — large constant, Java (#232, sibling to #94).
3. **Levelized/compiled sim strategy** — ~100× on CPU-scale designs, Java, behind
   the #77 boundary (#221).
4. **Java → Rust of already-optimized code** — a 2–3× *constant* factor only, and
   only after (1)–(3) are done.

The benchmark-game 10× figure measures tight scalar loops plus startup/footprint,
not graph-walking + allocation + hash-set membership, which is JLS's actual hot
path; a warmed JVM on that workload runs ~1.5–3× off native, not 10×. The entire
realistic performance envelope — roughly three-to-four orders of magnitude of
headroom — is capturable in pure Java with zero FFI. Crediting any migration for
it double-counts wins the incumbent already banks. **Performance is not a reason
to leave Java; it is a reason to fix the hash first.**

## Determination

**No full rewrite** in any candidate — every one is uneconomic-to-not-viable, and
the top-scoring alternative (Kotlin, 27) is strictly dominated by staying put
(34).

With contributor-pool and riscv64 removed from the weighting, the decision no
longer rests on any soft axis. It rests on three hard facts the re-weighting does
not touch:

1. **The GUI is 47% of the code and it works.** A full rewrite in any candidate
   must re-derive a dense hit-tested schematic canvas (the ~5.8k-line
   `SimpleEditor`), ~30 element dialogs, undo, and a trace window in a GUI toolkit
   that is, in every non-JVM case, less mature than 25 years of Swing. This cost
   is paid in full by every rewrite and zero by staying.
2. **The 36k-LOC test suite is the executable spec, and the `.jls`/batch/VCD
   contracts are byte-exact.** A rewrite must reproduce them across a language
   boundary; modernize-in-place refactors under them, test-guarded.
3. **The performance motivation is algorithmic** (see above) and capturable in
   Java. No language change is credited for it without double-counting.

**Continue modernize-in-place per #96** — the target *is* the incumbent, so
"rewrite in the best language" and "modernize Java" are the same program.

**Sequence:**

1. **Now** — land the free O(n²) dedup-hash fix (#231), the single largest
   measured win, test-guarded by the simulation/VCD goldens.
2. **#232 / #94** — value-typed `(long,width)` signal representation to remove
   `BitSet` allocation/GC.
3. **#221 behind #77** — stand up the levelized/compiled sim as a second strategy
   (~100×, ArchUnit-enforced, no contract change).
4. **#93 / #95** — drive the JSpecify+NullAway null-safety sweep across the 188
   files and sealed exhaustive dispatch on the 70 element classes, completing the
   compiler-enforced-properties program — kept honest by the test suite and
   ArchUnit tripwires so #93–96 do not stall as perpetual "someday" refactors.
5. **Extend the surgical verification prong** — apply the existing
   `SpatialIndexCorrectness.agda` / `ProofBridgeTest` pattern to simulation
   invariants (dedup/coalescing correctness, event-ordering totality, VCD-profile
   determinism), machine-checked, pinned to Java by test assumptions, extracting
   no executable code. **Agda stays a scalpel, never a host.**

## Revisit triggers

This determination flips only if:

- **The GUI cost collapses.** Swing/Java2D is announced for removal from the JDK,
  or FlatLaf is abandoned with no maintained equivalent — forcing a GUI-toolkit
  reckoning. Even then the move is toward JavaFX or another in-ecosystem canvas
  first, not a language migration.
- **The performance model is overturned by measurement.** After the three Java
  fixes ship, a profiled warmed-JVM benchmark on a real target design (e.g. the
  RV32I datapath) shows the residual bottleneck is a tight numeric inner loop with
  no allocation or graph-walking — the one shape where the JVM genuinely trails
  native by ~10×. That would resurrect the native case on evidence rather than
  benchmark folklore. (Under the strict full-rewrite framing this study adopts,
  the response would still be scoped native work, evaluated afresh, not a
  whole-product rewrite.)
- **Verification requirements escalate** to machine-checked in-language simulation
  semantics as a build gate — favoring the surgical Agda/Lean4 proof prong, still
  not a rewrite of shippable code.

*(The earlier triggers about the contributor pool and riscv64 deployment are
retired: those constraints have been lifted and folded into the scoring above, so
they can no longer flip the result.)*

## Related

- #96 — no language rewrite (the recorded decision this study confirms and
  quantifies).
- [`grand-architecture.md`](grand-architecture.md) §6 — the cold/hot-plane split
  and the levelized-evaluation reservation.
- #231 — the O(n²) dedup-hash fix (the flagship performance win).
- #232 — value-typed signal representation (the allocation/GC win).
- #221 — levelized/compiled simulation strategy for CPU-scale designs.
- #77 — headless-core extraction (the in-JVM boundary that hosts the sim
  strategy).
- #93 / #94 / #95 — the compiler-enforced-properties program.
- `proofs/SpatialIndexCorrectness.agda`, `test/jls/ProofBridgeTest.java` — the
  surgical-verification pattern to extend, not replace.
