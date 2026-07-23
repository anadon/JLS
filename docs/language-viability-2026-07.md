# Language & toolkit viability for a JLS migration (July 2026)

*Should JLS be recoded in another language — C++ (compile-time optimization),
Rust (typing sophistication), Haskell (longevity/correctness), Agda (verified
software), or some other point on the Pareto curve? This document answers that
with a structured comparison rather than a preference. It complements the
recorded no-rewrite decision (#96) and the forward architecture
([`grand-architecture.md`](grand-architecture.md)) by showing the **work** behind
the verdict: a 14-candidate comparison matrix over ten migration-impact
dimensions, plus the measured performance evidence that motivated the question.*

## Why this exists

A firmer answer than "no" was asked for, specifically on performance grounds
(the claim that Rust is ~10× Java on
[programming-language-benchmarks](https://programming-language-benchmarks.vercel.app/rust-vs-java)).
This document is the firmer answer. Its two load-bearing results:

1. **The "large circuits are slow" bottleneck is algorithmic, not linguistic** —
   and every realistic performance win is capturable in pure Java. Measured
   below, and filed as #231 and #232.
2. **Across 14 candidate languages scored on ten dimensions, staying on Java 25
   and modernizing in place (#96) is the global optimum** — a Pareto frontier
   point that strictly dominates 8 of the 13 alternatives outright, including
   the runner-up.

## Method

Candidates were enumerated (the four named languages plus Kotlin, Scala 3,
C#/.NET, OCaml, Zig, Go, Swift, Nim, Lean4, and a "Java 25, modernize in place"
baseline so the no-migration option competes on equal footing). Each was scored
1–5 on ten dimensions anchored to JLS's actual constraints — **not** the language
in the abstract — then every analysis was passed through an adversarial
verification step charged with catching both over-optimism (hand-waving the GUI
reimplementation cost) and over-pessimism, with score corrections folded back in.
A final synthesis produced the matrix and determination. This is an
AI-assisted study; the numbers are judgments, but the performance findings
(§"The performance finding") are measured and reproducible, and the structural
facts (LOC, Swing coupling, contracts) are drawn from the tree at commit
`02dbd3c`.

The ten dimensions: **sim-core-fit**, **gui-maturity** (for a dense hit-tested
schematic canvas — where ~47% of the code lives), **typing-gain over modern
Java-25** (sealed + records + JSpecify/NullAway, i.e. the #93–96 program),
**memory-safety**, **deployment** (single self-contained offline artifact incl.
riscv64), **contributor-pool** (first-year students in a Java course; a single
maintainer), **verification-affordance**, **ecosystem-longevity**,
**kernel-only-path** (extract *only* the sim kernel via FFI, keep the Java GUI),
and **contract-and-effort** (parity effort + risk of regressing the byte-exact
`.jls`/batch/VCD contracts).

## The comparison matrix

Scores are 1–5 (5 best **for JLS's objective**), verifier-adjusted. Total is out
of 50.

| Language | sim-core | gui | typing-gain | mem-safe | deploy | contrib | verif | eco | kernel | contract | **Total** | Full rewrite | Kernel-only (FFI) |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|---|---|
| **Java 25** (Swing+FlatLaf) | 5 | 4 | 2 | 5 | 5 | 5 | 4 | 5 | 5 | 4 | **44** | viable (= modernize-in-place #96) | n/a — no foreign language; #77 is in-JVM |
| **Kotlin** (reuse Swing) | 3 | 4 | 2 | 5 | 4 | 2 | 3 | 3 | 3 | 3 | **32** | uneconomic | weak (shared heap, no real FFI) |
| **C#/.NET** (Avalonia) | 4 | 3 | 2 | 4 | 2 | 2 | 3 | 3 | 2 | 2 | **27** | uneconomic | weak (cross-runtime CLR↔JVM) |
| **Scala 3** (reuse Swing) | 3 | 3 | 3 | 4 | 4 | 1 | 3 | 2 | 2 | 1 | **26** | uneconomic | weak (same-JVM, no seam) |
| **OCaml** (lablgtk3) | 4 | 2 | 3 | 4 | 2 | 1 | 4 | 2 | 3 | 1 | **26** | not-viable | weak (dual-GC; 63-bit int wrinkle) |
| **Nim** (gintro/GTK4) | 4 | 2 | 2 | 3 | 3 | 2 | 2 | 2 | 4 | 2 | **26** | uneconomic | defensible (clean C-ABI, thin RT) |
| **Haskell** (monomer) | 3 | 2 | 3 | 5 | 2 | 1 | 4 | 2 | 2 | 1 | **25** | not-viable | weak (embed GHC RTS = 2nd GC) |
| **C++** (Qt QGraphicsView) | 5 | 4 | 1 | 1 | 2 | 1 | 3 | 3 | 3 | 1 | **24** | uneconomic | weak (clean seam, chatty upcalls) |
| **Rust** (egui/Slint) | 4 | 1 | 2 | 3 | 3 | 1 | 4 | 2 | 3 | 1 | **24** | uneconomic | defensible (cdylib via Panama) |
| **Zig** (Dear ImGui) | 5 | 1 | 2 | 2 | 3 | 1 | 2 | 1 | 5 | 1 | **23** | not-viable | weak (cleanest FFI, no live problem) |
| **Go** (Gio) | 3 | 2 | 1 | 4 | 3 | 2 | 2 | 3 | 2 | 1 | **23** | uneconomic | weak (2 GCs/schedulers, cgo) |
| **Lean4** (none) | 2 | 1 | 5 | 2 | 2 | 1 | 5 | 2 | 2 | 1 | **23** | not-viable | weak (verified-kernel idea, impractical) |
| **Agda** (none) | 1 | 1 | 3 | 5 | 1 | 1 | 5 | 2 | 2 | 1 | **22** | not-viable | *compelling — but only as surgical proof, not executable extraction* |
| **Swift** (SwiftGtk/Adwaita) | 3 | 1 | 2 | 3 | 1 | 1 | 2 | 2 | 2 | 1 | **18** | not-viable | weak (ship ARC RT; weak riscv64) |

## Ranking, by fitness for JLS's objective

1. **Java 25 (44)** — the incumbent *is* the target, so "full rewrite" collapses
   into modernize-in-place. Captures every language-independent win with zero
   FFI, keeps the 47%-GUI surface, the single-jar supply chain, and the
   Java-course audience, and adds #93–96 as compiler-enforced properties. The
   recorded #96 decision, and the correct one.
2. **Kotlin (32)** — the only non-Java candidate that keeps Swing verbatim in one
   JVM jar and lets the tests stay Java; but it buys essentially nothing over
   Java-25 (same JIT ceiling, marginal typing gain) while adding a second
   compiler to a solo Java build.
3. **C#/.NET (27)** — strong sim-core fit and Java-familiar, but NativeAOT has no
   riscv64 target, breaking the multi-arch container mandate by itself, and it
   exits the JVM ecosystem for a ~1.5–3× constant.
4. **Scala 3 (26)** — smoothest JVM rewrite mechanically (records/sealed native,
   tests stay executable), but sbt/Scala are inaccessible to the audience and it
   delivers no performance-ceiling gain.
5. **Nim (26)** — best solo-maintainer native-kernel story (light ORC runtime,
   clean C-ABI, single static binary), but no mature GUI for the dense canvas and
   a small high-churn ecosystem make a full rewrite indefensible.
6. **Rust (24)** — highest-scoring native candidate on the axes that matter for a
   *kernel* (memory-safety, verification, a clean Panama seam), but pre-1.0 GUI
   (egui/Slint), the borrow-checker-versus-cyclic-graph friction, and pool
   collapse make the full rewrite uneconomic; the residual is a 2–3× constant.
7. **OCaml (26)** — idiomatic discrete-event style and good verification
   affordance, but lablgtk3 is generations behind Swing and the 63-bit tagged int
   reintroduces boxing in exactly the hot path #94 is meant to de-allocate.
8. **C++ (24)** — the best raw substrate (Qt `QGraphicsView` is a superb
   dense-canvas engine), but surrenders Java's memory safety to UB and evicts the
   entire contributor pool.
9. **Go (23)** — single-binary and memory-safe, but regresses the type system
   *below* Java-25 (no sealed sums, no records, nil hazards) and Gio is a fraction
   of Swing's maturity; the worst FFI shape for a chatty discrete-event loop.
10. **Haskell (25 pts, lower fitness)** — elegant value/event semantics and
    strong verification, but monomer is immature, riscv64 GHC is shaky, and
    embedding the GHC RTS in the JVM is a hostile kernel boundary; purity is an
    aesthetic, not a product, win here.
11. **Lean4 (23)** — unmatched typing/verification, but literally no desktop GUI
    target for 47% of the code; interesting only as a verified sim kernel, and
    even that is undercut by the per-event `react()` callback entanglement.
12. **Zig (23)** — the cleanest C-ABI kernel seam of the set, but 0.x churn, no
    retained-mode GUI, and no live problem left once the Java fixes land.
13. **Agda (22)** — not an application language (no GUI, no production runtime).
    It earns its place *only* by extending the existing
    `SpatialIndexCorrectness` surgical-proof pattern to simulation invariants —
    verification, not migration.
14. **Swift (18)** — no toolkit both mature and cross-platform, deployment fails
    on riscv64/single-artifact, and Apple-governed churn on Linux/Windows is the
    worst 10–20-year bet.

## Pareto analysis

**Frontier points** (non-dominated):

- **Java 25** — the global optimum: top or joint-top on 8 of 10 axes and the
  highest total (44). Dominates 8 of the 13 alternatives outright.
- **Scala 3, OCaml, Haskell, Agda, Lean4** sit on the frontier only via a single
  specialist axis each — typing-gain (Scala 3), typing+verification (OCaml,
  Lean4), memory-safety+verification (Haskell, Agda) — none of which is
  product-decisive for JLS, and all of which Java either matches or renders moot
  through the surgical Agda/ProofBridge prong it already runs.

**Strictly dominated by Java 25** (Java scores ≥ on every axis): Kotlin, C#/.NET,
Scala's non-typing axes, C++, Rust, Nim, Go, Zig, Swift. The recurring reason is
that the axes where these languages *could* win — raw throughput, memory
footprint — are either non-binding (see below) or matched by Java's in-JVM kernel
path, while the axes they lose — GUI maturity for this canvas, deployment,
contributor pool, contract-preservation effort — are exactly where JLS's value
concentrates.

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
the top-scoring alternative (Kotlin, 32) is strictly dominated by staying put
(44).

**No sim-kernel-only FFI extraction now** — it solves a performance problem that
will not exist once the Java algorithmic fixes land, and the event loop's
per-event upcalls into 70 Java element `react()` methods make any near-term
kernel seam catastrophically chatty. The extractable core is exactly the part the
free hash fix already makes fast.

**Continue modernize-in-place per #96** — the target *is* the incumbent, so
"rewrite in the best language" and "modernize Java" are the same program, and it
uniquely preserves the single-jar product, the JVM supply chain, the 47%-GUI
Swing surface, the 36k-LOC executable-spec test suite, and the Java-course
contributor pool at zero migration cost.

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

- All three Java algorithmic fixes have shipped **and** a profiled, warmed-JVM
  benchmark on a real target design (e.g. the RV32I datapath) still misses a hard
  latency SLO by the ~2–3× a native constant would close — then, and only then,
  reopen sim-kernel-only FFI extraction (Zig/Rust/Nim via Panama), never a full
  rewrite.
- The event loop is refactored so the hot path no longer upcalls per-event into
  the 70 element `react()` methods — removing the "chatty boundary" objection that
  currently sinks every kernel-only option.
- Swing/Java2D is announced for removal from the JDK, or FlatLaf is abandoned with
  no maintained equivalent — forcing a GUI-toolkit reckoning, but toward JavaFX or
  another in-ecosystem canvas, still not a language migration.
- The audience/maintainer constraint changes: contributors fluent in a native or
  typed-functional stack arrive, or the first-year-Java-course mandate is dropped
  — reweighting contributor-pool, the single axis doing the most work to sink the
  native candidates.
- The hard deployment constraints relax — riscv64 dropped, or the
  single-self-contained-reproducible-artifact mandate abandoned — lifting the
  deployment penalty on C#/.NET, Rust, and Nim.
- Verification requirements escalate to machine-checked in-language sim semantics
  as a build gate — favoring the surgical Agda/Lean4 proof prong, still not a
  rewrite of shippable code.
- A future measurement overturns the load-bearing performance model — i.e. the
  bottleneck is shown to be a tight numeric inner loop with no allocation or
  graph-walking, where the JVM genuinely trails native ~10× — resurrecting the
  native-kernel case on evidence rather than benchmark folklore.

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
