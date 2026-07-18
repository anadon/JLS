# PIT Mutation-Testing Trial (issue #161)

*2026-07-18. The scoped trial that issue #161 gated on the coverage
program (#159) reaching bundle LINE ≥ 0.50 — the gate was crossed
2026-07-17 (55.5 % line under the #162 display substrate). This
records the trial per the issue's §7 method: apparatus verification,
the scoped run, survivor triage with the assertions it produced, and
the adopt verdict with the H2 caveat. The verdict line also lives in
[`library-survey-2026-07.md`](library-survey-2026-07.md).*

## 1. Apparatus verification (survey rule 4, re-checked at gate time)

Checked against Maven Central metadata on 2026-07-18:

- `org.pitest:pitest-maven` **1.25.7**, last published 2026-07-08 —
  actively maintained, releasing on a weeks-cadence.
- `org.pitest:pitest-junit5-plugin` **1.2.3** (2025-05) — slower
  cadence, but verified working below against JUnit **6.1.2**.
- JDK 25 compatibility: verified empirically — the full scoped run
  completes on OpenJDK 25.0.3 with zero `NON_VIABLE` / `RUN_ERROR`
  mutants.
- One classpath note: pitest builds its own test classpath and does
  **not** inject `junit-platform-launcher` the way Surefire does; the
  trial added `org.junit.platform:junit-platform-launcher:6.1.2`
  (test scope) alongside the plugin. Without it the JUnit 5 plugin
  cannot discover tests.

## 2. Trial configuration

Scoped exactly as issue #161 §7 prescribes — the covered, non-Swing
core — and run headless so the measurement is substrate-independent:

- `targetClasses`: `jls.sim.*`, `jls.BitSetUtils`, `jls.Util`,
  `jls.SpatialIndex`, `jls.collab.op.*`
- `excludedGroups`: `display` (the #162 dialog/gesture suites need a
  display; their target code is outside this scope anyway)
- `jvmArgs`: `-Djava.awt.headless=true`; `threads`: 4; default
  mutator set.

## 3. Results — P1 (baseline, before any new tests)

| Measure | Value |
|---|---|
| Mutants generated | 1173 |
| Detected (killed + timed out) | 452 (**39 %** mutation score) |
| No coverage | 495 |
| Survived | 226 |
| **Test strength** (detected / covered) | **67 %** |
| Line coverage of mutated classes | 58 % |
| Wall clock (4 threads, containerized CI-like host) | **1 m 55 s** (2 m 27 s worst of three runs) |

H1 confirmed on both halves: the mutation score (39 %) sits far below
the line-coverage figure (58 %) — reach without strength — and the
survivor list was *not* dominated by equivalent mutants (§4).

Survivors by class: `jls.sim.Trace` 82, `jls.sim.InteractiveSimulator`
(+inner classes) 61, `jls.SpatialIndex` 21, `jls.Util` 20,
`jls.collab.op.CircuitOpReader` 12, `jls.BitSetUtils` 10,
`jls.sim.SimEvent` 7, `jls.sim.BatchSimulator` 5, `jls.sim.Simulator`
4, rest ≤ 3.

## 4. Survivor triage — P2

The three most assertion-shaped pools (SimEvent, BitSetUtils,
CircuitOpReader — 29 survivors) were triaged into the issue's (a)
missing assertion / (b) equivalent mutant / (c) dead code classes:

**(a) Missing assertions → 13 killing tests written.**

- `jls.sim.SimEvent` — all 7 survivors, the class's *entire*
  `compareTo`/`equals`/`hashCode` contract: the event queue exercises
  these constantly (every simulation), but nothing asserted their
  results, so PIT could return 0 from `compareTo` untouched. The
  same-time-events-pop-in-creation-order tiebreak is what makes
  simulation deterministic. → `test/jls/sim/SimEventContractTest.java`
  (6 tests, 7/7 killed).
- `jls.BitSetUtils.Create(long)` line 42 (`64 - nlz` → `64 + nlz`) —
  a genuinely subtle catch: Java's shift-count masking makes
  `value >> 64 == value`, so the mutant plants a mirror copy of every
  bit at index 64+, and the existing `Create`/`ToLong` round-trip
  tests are *blind* to it because `ToLong`'s `pow` accumulator
  overflows to 0 at index 64. → cardinality/length assertions in
  `BitSetUtilsCreateTest`.
- `jls.BitSetUtils.SumCarry` line 221 (negated final carry-out) —
  same blindness: the mutant sets a spurious bit at index 64 on every
  carry-free add, invisible to every `ToLong`-projecting golden. →
  `test/jls/BitSetUtilsSumCarryTest.java` (exact `BitSet`-equality
  oracle, 4 tests).
- `jls.collab.op.CircuitOpReader` — the 3 surviving `requireFields`
  removals (ToggleWatched / RemoveProbe / FlipElement): every existing
  hostile input for those kinds died earlier in the parse loop, so
  the per-kind field-shape check itself was unpinned; plus the
  `MAX_IDS` `>=` boundary, unpinned on both sides. → 3 hostile inputs
  and an exact-limit test (10,000 ids parse, 10,001 rejected) in
  `CircuitOpTest`.

**(b) Equivalent mutants → 11 identified, no exclusions filed yet.**

- 6 loop-boundary mutants (`<` → `<=` in `Create`×2, `ToInt`,
  `ToLong`, `ToBigInteger`, `SumCarry`): the extra iteration
  sets/reads a guaranteed-false bit — a no-op on `BitSet` content.
- 2 `sum.set(index, false)` call removals in `SumCarry`: setting
  false on an unset bit is a no-op.
- 2 in the reader's `MAX_LINES` guard (boundary + counter
  increment): the guard is unreachable belt-and-braces —
  `MAX_LINES = MAX_IDS + 16`, but the per-field duplicate rejection
  caps a parseable block at `MAX_IDS + 4` lines, so no input reaches
  it. Kept as defense-in-depth, not worth an exclusion comment yet.
- 1 `clip` length boundary (cosmetic: affects only whether a
  60-character error-message excerpt gains an ellipsis).

**(c) Dead code → none requiring a cleanup issue** (the `MAX_LINES`
guard above is deliberate defense-in-depth, not accidental).

### Post-triage re-run

| Measure | Baseline | After the 13 new tests |
|---|---|---|
| Detected | 452 (39 %) | **468 (40 %)** |
| Survived | 226 | 213 |
| Test strength | 67 % | **69 %** |

(Detected delta +16: 13 targeted kills plus collateral coverage;
±1 run-to-run jitter between `KILLED` and `TIMED_OUT`
classification.) The remaining large pools — `Trace` (82),
`InteractiveSimulator` (61), `SpatialIndex` (21), `Util` (20) — are
mostly waveform-painting and geometry code reached by smoke tests;
they are the natural next triage targets, best taken with their
packages' coverage work rather than cold.

## 5. H2 — incremental analysis: **refuted for the OSS core**

`-DwithHistory=true` on pitest 1.25.7 fails with:

    History has been enabled but no history plugin has been
    installed/activated.
    If you are using https://www.arcmutate.com remember to activate
    the history plugin with +arcmutate_history

Incremental analysis has moved out of the open-source core into the
commercial arcmutate history plugin, so the per-PR-delta design H2
envisioned is not available. The issue's §9 fallback applies:
**scheduled or on-demand runs, not per-PR**. The sting is minor at
the trial scope — the full scoped run is ~2 minutes, cheaper than
the xvfb display leg CI already carries — but it should be a
non-blocking report either way, and the scope should be widened
deliberately (mutation runtime ≈ mutants × covering-test time).

## 6. Verdict and adopted shape

**Adopt** — H1 confirmed (39 % score vs 58 % line coverage; 13 real
missing assertions in the first triage pass; equivalent mutants a
small minority), P1–P2 delivered, P3 (incremental) replaced by the
§9 fallback. Wiring (for the pom.xml integration pass): add the
following as a **profile** (`mvn -Ppitest test-compile
org.pitest:pitest-maven:mutationCoverage`), mirroring the
`errorprone` trial-gate convention — non-blocking report first, a
`mutationThreshold` ratchet only after a few scheduled runs establish
the noise floor:

```xml
<profile>
  <id>pitest</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.pitest</groupId>
        <artifactId>pitest-maven</artifactId>
        <version>1.25.7</version>
        <dependencies>
          <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.3</version>
          </dependency>
        </dependencies>
        <configuration>
          <targetClasses>
            <param>jls.sim.*</param>
            <param>jls.BitSetUtils</param>
            <param>jls.Util</param>
            <param>jls.SpatialIndex</param>
            <param>jls.collab.op.*</param>
          </targetClasses>
          <excludedGroups>
            <param>display</param>
          </excludedGroups>
          <jvmArgs>
            <value>-Djava.awt.headless=true</value>
          </jvmArgs>
          <threads>4</threads>
          <timestampedReports>false</timestampedReports>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

plus `org.junit.platform:junit-platform-launcher:6.1.2` (test scope,
see §1 — align its version with the JUnit dependency whenever that is
bumped). Scope-expansion note from the #161 comment thread: the
display-only suites (#162) cover `jls.edit`/dialog code that a
headless PIT run cannot kill mutants in — if the scope ever grows
past the core above, the run must move under the same
`xvfb-run … -Djls.test.headless=false` substrate CI uses, or those
packages will drown the report in unkillable survivors.
