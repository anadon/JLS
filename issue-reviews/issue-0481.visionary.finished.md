# Issue #481: TASK-0098: the analog solver produces the same bytes on Linux, macOS and Windows — five determinism controls, each a failing build rather than a convention
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of its apparatus, #481 buys one thing: **the right to commit an analog
result as a golden.** Everything else — five ArchUnit/scan rules, a spec-derived
reader, a grid-flip comparator, a replica pin, a new CI job, a new doc — is
scaffolding around that one claim. And #351 is explicit that the claim is also
the programme's go/no-go: if analog bits are not portable, porting SPICE into
Java rather than orchestrating one loses its justification.

I endorse the goal without reservation. I think the issue's shape is wrong in
five places, and that a smaller, earlier, more general artifact gets the same
end. Below I say explicitly where I am setting the acceptance criteria aside.

## 1. The scheduled falsification tests the hypothesis most likely to be true

The issue sells itself as an early go/no-go: "falsifiable **here, for two
weeks**, rather than in month ten." But §7.10 stage 1 already proves — from the
JLS spec, not from measurement — that with JEP 306 strictness the only escapes
from bit-portability are (i) transcendental library functions, closed by
`StrictMath`, and (ii) evaluation order, closed by writing the loop in one
order. IEEE 754 binary64 add/sub/mul/div/sqrt are correctly rounded *by
specification*; Java never contracts FMA implicitly; there is no extended
precision left. §1.3's own spike (identical digest across `-Xint`, `-UseFMA`,
`UseAVX=0`, SerialGC, JDK 21 and 25) is consistent with that. H1 is not really a
research question — it is a theorem modulo an enumerated escape list.

So the experiment #481 schedules is near-certain to come back green, and a
near-certain experiment carries almost no information. Meanwhile the hypothesis
that *could* kill the analog programme is stated one issue up and left for later:
#351 Open Question 5 — "a solver that runs four demonstration circuits and fails
on homework is worse than no solver, **because it is claimed**." Newton
convergence on circuits a nineteen-year-old drew, with a floating ground and a
diode backwards, is the real risk, and it lands in #464/#331 at month ten.

**If the programme gets one two-week de-risking slot, spend it on convergence,
not on determinism.** Twenty real student-drawn analog circuits through a rough
kernel, counting how many converge without hand-tuning, is the falsifiable
experiment whose red outcome would actually change the programme's shape.

Worse, #481 defeats its own stated urgency: it is `blocked_by` #463, the entire
solver core. An experiment that can only run after the thing it is meant to
de-risk is built is not an early experiment. The portability question needs no
`jls.analog` at all — a throwaway 200-line dense-MNA kernel in a scratch branch,
digested on the three existing CI legs, answers it in days. Do that first,
unblocked, and let the rules land afterwards as ordinary engineering.

## 2. The cross-platform digest job already exists — it is called `mvn verify`

`.github/workflows/ci.yml` already runs the full suite on three legs: `build:`
(ubuntu-latest, x86-64, JDK 25 + 26), `windows:` (windows-latest, x86-64, JDK 25
+ 26, `continue-on-error: true` with a documented promotion rule), and `macos:`
(macos-latest — which is **aarch64** Apple silicon, `mvn -B verify`). Every
committed golden in the tree — `VcdExportGoldenTest`, `BatchSimulationGoldenTest`,
`DeterministicSaveTest`, `RiscvCpuGoldenTest` — is *already* asserted on three
operating systems and two instruction set architectures, on every push, and has
been for as long as those legs have existed.

`CrossRuntimeDigestTest` is therefore not a new capability. Write it as an
ordinary JUnit test with a committed constant and the existing legs assert it
cross-platform for free. That deletion is not cosmetic:

- the new job disappears, so **P12 and threat T5 disappear, and `blocked_by`
  #374 (tree-wide `timeout-minutes`) is no longer a blocker** — halving the
  dependency set;
- the advisory-versus-required confusion (T4, #265, #111) stops being this
  issue's problem, because it is already exactly the status of the legs;
- there is one promotion rule for the whole matrix instead of two.

I am disregarding P9's "one test, two jobs" framing, P12, and the §8 step "add
the analog-goldens CI job". The right count is one test, zero new jobs.

## 3. Five open deny-lists should be one closed allow-list

D-1/D-2/D-3/D-5 are all the same rule wearing four hats: *the solver may touch
nothing whose behaviour is not a function of its inputs*. Expressed as banned
substrings, that rule is open-ended, and the issue's lists already leak:

- **`Set.of`, `Map.of`, `Collectors.toUnmodifiableSet/Map`.** `ImmutableCollections`
  randomizes iteration order per JVM run from a `SALT` seeded at class init.
  This is *strictly worse* than `HashSet` — it varies between two runs on the
  same machine — and P6 bans only `new HashSet`/`new HashMap`.
- **`Collectors.toMap`** (HashMap-backed), **`stream().parallel()`**,
  **`Executors.*`**, **`CompletableFuture`** — none caught by a ban on `new
  Thread(`, `parallelStream`, `ForkJoinPool`, `Arrays.parallelSort`.
- **`Math.random`, `ThreadLocalRandom`, `Instant.now`, `LocalDateTime.now`,
  `System.identityHashCode`** — none caught by P8's four names.
- **`String.format("%f", …)`** — locale-dependent decimal separator. P13 bans
  only `Double.toString`, so a golden written under `de_DE` gets commas.

The tree already has the closed-form idiom and it is one method long:
`ArchitectureRulesTest.coreDependsOnNoGuiClasses()` (test/jls/ArchitectureRulesTest.java:122)
uses ArchUnit's package predicates. Invert it:

```java
classes().that().resideInAPackage("jls.analog..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("jls.analog..", "java.lang..", "java.util.function..",
        "java.util.List", "java.util.ArrayList", "java.util.Arrays",
        "java.util.Objects", "java.util.Comparator");
```

One assertion, closed rather than open, subsumes D-1, D-2, D-3 and D-5, and its
failure message names the offending class and the offending dependency without
any hand-written message. `java.lang` still admits `Math`, so D-1's
`sqrt`/`abs`-only carve-out stays a second, narrow scan — and that one is
genuinely a source scan, because it is method-level. Two rules, not five, and
the surviving pair is the pair that cannot be circumvented by a construct nobody
thought to add to a list. D-4 stays as it is: an assertion on the permutation
vector, which is the only one of the five that is a *semantic* property and is
correctly tested as one.

This also answers H2's anticipated refutation. The issue predicts D-3 will fail
to be expressible because "hash-ordered iteration reaching the matrix" is a
dataflow property. Under an allow-list it is not a dataflow property at all —
the hash-ordered types are simply not on the classpath of that package.

## 4. Pinning `JLS_REPLICA_ID` fixes CI and leaves the student broken

O3 and stage 3 are the best observation in the issue, and the prescribed fix is
the wrong one. `Circuit.getElementsInStableOrder()` (src/jls/Circuit.java:479)
sorts by `Element::getStableId`, and a stable id is `(replica, counter)` — a
**provenance** key, not a content key. Its javadoc claims the output is "a pure
function of circuit content"; that is true of one file loaded twice, and false of
two students who drew the same circuit.

Pinning `JLS_REPLICA_ID` in CI and in a test base class makes the *CI* digest
stable. It does nothing for the case in the issue's own Intended Audience:
"a grade that depends on which laptop ran the simulation is not a grade."
Two students who draw identical circuits still get different accumulation orders,
hence different last-ulp results, hence different waveforms — and the instructor
diffing against a reference gets a mismatch that no CI pin can touch.

The reframe that makes the hazard disappear rather than masking it: **the
elaborator must order devices by a content-derived canonical key, not by stable
id.** Type tag, then grid coordinates, then port connectivity — the same class of
key #166's canonical serialization already establishes and `Circuit.stateHash()`
already exposes. Then $\prec$ is a function of the drawing, T3's "masked by
`legacy:N` sorting after every hex digit" accident evaporates, H5/P11 are
unnecessary, and the guarantee is the one the audience was promised. Yes, this is
a real design decision that belongs on #463 rather than here — which is itself
an argument that #481 is filed against the wrong seam.

## 5. Two of its deliverables belong to siblings

- **`GridFlipComparator` (P10, H4).** #351 §5 assigns this verbatim: "IC-5 — a
  grid flip is a third outcome … *Does not exist; TASK-0100 builds it*." #481 P10
  builds it anyway. The three-valued outcome is excellent design and I endorse it
  without qualification — in #397, where the two-run comparison lives.
- **The replica pin (P11).** #351 §5 IC-8: "*this issue's close-out owns it
  because it spans CI configuration and the golden format*," and Open Question 6
  is still open at the feature level. Two issues owning one pin is how a pin ends
  up half-landed.

## 6. The doctrine is project-wide; `docs/analog-determinism.md` makes it a silo

JLS already has more determinism machinery than most projects its size: stable
ids (#165), canonical byte-identical save (#166, `DeterministicSaveTest`),
a byte-deterministic IEEE 1364 §18 VCD writer (#66), reproducible builds with a
published `.buildinfo` (#185, `docs/reproducibility.md`), an Agda proof of the
spatial index, and seven source-scanning ratchets. What it does *not* have is one
place that says **what JLS guarantees byte-identical, to whom, and how each
guarantee is enforced.**

Write `docs/determinism.md` — one doctrine, with build reproducibility, save
canonicality, waveform export, and floating-point kernels as sections. Make the
rule test `NumericDeterminismRatchetTest`, parameterized over a set of kernel
packages that today contains exactly `jls.analog`. The second entry is already
foreseen: ARCHITECTURE.md's #221 decision reserves a levelized compiled
evaluation pass behind a named revisit trigger, and binds it to agree
"bit-for-bit with the #202 RV32I integration golden." That pass will face this
identical rule set. Building the mechanism once, general, costs the same two
weeks and yields an asset that survives the analog programme being cancelled.

Which matters, because the roadmap currently says it should be. `docs/capability-roadmap/README.md`
§6 "What still stays out" puts continuous-time analog under **(a) different tool
class** — "Supporting these means being a SPICE-class solver — a different tool,
not a deeper digital model" — and sweep-02, sweep-03 and sweep-06 each repeat it
independently ("No continuous-time solver, and none should be added"). `AMENDMENT.md`
does not overturn it, and `docs/plan/evidence/` does not exist in the tree. FEAT-046
reverses a four-times-recorded scope decision at a band of 17.5–26 maintainer-weeks
with a 3.25× unallocated gap. That reversal may well be right — but it is not #481's
to make, and the more of #481's value is analog-specific, the more of it is staked
on a bet the project's own documents have not yet agreed to.

## 7. Two smaller design notes

- **Canonicalizing `-0.0` to `0.0` (stage 4, P13) is the loosening T7 forbids,
  wearing an encoding's clothes.** If Linux emits `0.0` and macOS emits `-0.0`
  for the same node voltage, the sign of a subtraction or the direction of an
  underflow differed. That is a determinism finding, and `canon` deletes it.
  Use `doubleToRawLongBits`, keep the comparison exact, and let a zero-sign
  mismatch be investigated. (Relatedly, `Double.toHexString` prints every NaN
  payload as `NaN`; if NaN can reach a golden at all, the bits form is required.)
- **Ship one golden form, not two.** A digest has nothing to read, so H3's
  independent spec-derived reader earns nothing against it; a readable hex-float
  trace is what you actually want in hand when a leg goes red at 2am. Commit the
  trace, derive the digest from it, and let `GoldenReader` be both the oracle and
  the debugging tool. Two committed artifact classes means two regeneration
  protocols and two ways to drift.

## What I would keep unchanged

The three-valued grid-flip outcome (in #397). The refusal to accept a tolerance
for a byte golden (T7, §10 H1). The requirement that each rule be *seen* to fire
on an injected violation (T2) — the single most under-practiced discipline in
this kind of work. The insistence that goldens carry solver statistics in the
header so a sub-tolerance convergence regression still fails. The clean-baseline,
no-exemption-list stance (O2, §7.12). These are the parts worth carrying into
whatever this issue becomes.

## The counter-proposal, concretely

1. **This week, unblocked:** a scratch dense-MNA kernel, digested on the three
   existing CI legs. Answer H1 before #463 rather than after it. Record the
   go/no-go on #351.
2. **Move the two-week de-risking slot to convergence** — twenty student-drawn
   circuits, count the ones that converge unassisted. That is the experiment whose
   red result changes the programme's shape.
3. **Refile this task as `docs/determinism.md` plus `NumericDeterminismRatchetTest`,
   parameterized by kernel package:** one ArchUnit allow-list rule, one method-level
   `Math`/`StrictMath` scan, one permutation-vector assertion. No new CI job. Drops
   `blocked_by` #374.
4. **Move the content-derived elaboration order onto #463** as a design constraint,
   and let the `JLS_REPLICA_ID` pin land once, on #351's close-out, where the
   feature already assigns it.
5. **Return `GridFlipComparator` to #397.**

Same end state, roughly half the artifacts, one fewer blocker, and the mechanism
outlives the analog bet instead of dying with it.
