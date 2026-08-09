# Issue #830: TASK-C333-1: one circuit runs on all three CI platforms and its waveform dumps are diffed, so every byte-identity claim downstream rests on a measurement instead of an assumption
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is right and it is not the one the acceptance criteria describe. #333 needs to
be able to say "byte-identical" without an asterisk. #830 reads that need as *an
unmeasured fact* and proposes an observation. The tree says it is *an unenforced
property* and wants a construction. Those two framings diverge hard on what happens
when the diff is non-empty, and that divergence is where this task goes wrong.

## The tree already runs most of this experiment, continuously

`test/jls/VcdExportGoldenTest.java` compares the emitted VCD against a committed
constant (`WAVE_GOLDEN`, `STIM_GOLDEN`) *byte for byte*, including a real subprocess
CLI run reading `out.vcd` back off disk (`:358-361`). `BatchSimulationGoldenTest.
watchedElementsPrintInNameOrder` does the same for stdout. `SequentialGoldenTest`,
`ElementSimulationGoldenTest` and `RiscvCpuGoldenTest` pin simulation semantics.
`.github/workflows/ci.yml` runs `mvn -B verify` on `ubuntu-latest` (JDK 25/26),
`windows-latest` (JDK 25/26, `:143`) and `macos-latest` (JDK 25, `:260`). Equality
against one committed representative on N platforms *is* pairwise equality across
those N platforms, transitively — and it attributes a divergence to a platform by
which lane goes red, which a fourth job diffing three uploaded artifacts does worse.
AC-1 proposes to build machinery that is strictly weaker than machinery already
running on every push.

The genuine gap is not evidence, it is **authority**: both off-Linux lanes carry
`continue-on-error: true`. A determinism divergence today is a yellow advisory nobody
reads. AC-1 says the job fails "on any difference"; AC-4 forbids promoting any lane.
On an advisory lane a failing step fails nothing, so the two criteria cancel.

## The refutation branch would fire on a bug, and would downgrade the wrong thing

`src/jls/sim/BatchSimulator.java:571` is `System.out.println(reason + " at " + now)` —
platform line separator, so CRLF on Windows. Every other batch printer already uses
explicit LF (`Pin.printValue:288` is `System.out.printf("%s Pin %s: %s\n", ...)`; the
VCD builder appends `'\n'` and writes UTF-8 bytes at `:368`). The project already
*knows* this: `VcdExportGoldenTest.java:355` reads

```java
assertEquals(WAVE_STDOUT_GOLDEN, stdout.replace("\r\n", "\n"),
        "batch stdout must match the committed golden");
```

That normalization is the tree conceding the exact byte difference #830 sets out to
discover. So AC-1's Linux↔Windows watched-output diff fails at the first newline, on
the first run, deterministically — and AC-3 then obliges a `REPLAN:` restating #333's
criteria 1, 2 and 5 as single-platform guarantees. A one-character emitter
inconsistency would permanently narrow a distributed-simulation contract. That is the
failure mode this task most needs to not have.

It also pulls against the project's own demonstrated habit. The two prior
platform/ordering divergences were fixed at source, not recorded as facts: the #72
`HashSet` iteration order (`docs/batch-interface.md:157`, now pinned to name order)
and the macOS `VK_DELETE` glyph (#265, fixed by canonical VK-name normalization).
#265 invariant 4 states the rule outright — failures are fixed at the source, never
muted. #830 institutionalizes the opposite reflex.

## AC-2 is unexecutable and points at the wrong document

`docs/parity-contract.md` does not exist. `docs/` holds 31 files; none is it, and
`*parity*` in the tree matches only `test/jls/edit/TextMetricsParityTest.java` and
`test/jls/elem/GateOutlineParityTest.java`. AC-2 requires replacing a sentence in a
file that isn't there. The interesting point is not the broken citation — it is that
the promise belongs somewhere else entirely. `docs/batch-interface.md` §4 already
promises "Newlines are `\n` and the file is written as UTF-8" for the VCD (`:306`);
§3 makes no such promise for stdout; §6 nonetheless freezes "any byte a conforming
consumer could observe." The shipped, normative contract that autograders read is
therefore already self-contradictory on Windows, and no planning document can fix
that. One sentence in §3 beats a measurement recorded in a planning file.

## The alternative: make it true, then make it gate

I am disregarding AC-1 through AC-4 and proposing this instead.

1. **Fix the divergence at source.** `BatchSimulator.displayOutcome:571` emits an
   explicit `\n` like every sibling printer, and `VcdExportGoldenTest.java:355` drops
   `.replace("\r\n", "\n")` so the golden asserts real bytes. Add to
   `docs/batch-interface.md` §3 the sentence §4 already carries. Cost: minutes. This
   alone removes the only divergence anyone can currently name.
2. **Assert against the committed representative, not pairwise.** No new job, no
   artifact upload/download, no bespoke first-differing-byte reporter. The goldens are
   the canonical bytes; each lane's failure message already names the differing
   content.
3. **Add a narrow required gate — `Determinism (all platforms)`.** A 3-OS matrix
   running only a `@Tag("determinism")` surefire group (the five golden suites), which
   *can* be required from day one precisely because it excludes the display, HDL and
   coverage suites that force #265's whole-lane staging. This does not violate #265
   invariant 1: #265 stages *whole-suite* parity; a determinism subset is a different
   and far smaller promotion object. It is the seam #830 needed and did not cut,
   because it accepted "lane" as the unit of promotion.
4. **Generalize with the idiom the project already owns.** `test/jls/
   ArchitectureRulesTest.java` carries eleven bytecode rules (socket confinement at
   `:249`, no Java serialization at `:202`). A twelfth — no `PrintStream.println`, no
   locale-defaulting `format`/`printf`/`toLowerCase`, no default-charset writer, no
   `Date`/`currentTimeMillis`, no `HashMap`/`HashSet` iteration feeding order, in the
   batch-output path — converts "unverified assumption" into "structurally
   unreachable." The codebase is already most of the way there (`Locale.ROOT` at
   `JLSStart.java:383,819,1087,1102`; `Integer.toHexString` rather than `%x`).

## Scope honesty, whichever route is taken

Three GitHub-hosted runners are en_US.UTF-8, Temurin, x86_64 plus macOS-arm64. The
sentence #830 wants to retire claims bit-identity "across a JDK upgrade or across
operating systems"; the README ships riscv64 containers and aarch64 installers, and
CI has an `ubuntu-24.04-arm` runner. A green on three homogeneous runners licenses
"no divergence on the sampled configurations," and #333 will cite it as the stronger
claim. An invariant has no sample size; a datum does. If the measurement route is
kept anyway, the recorded answer must carry its own scope, or it becomes exactly the
kind of unearned assumption it was filed to remove.

## Trajectory

Duplicative in its evidence half, actively harmful in its refutation half, correct in
its instinct. Keep the goal — retire the asterisk on "byte-identical" — and replace
the program: fix the printer, delete the normalization, promise the newline in the
shipped contract, gate a narrow determinism tag on three OSes, and encode the ban in
the architecture rules. Then #333's open question 3 answers itself, and answers
*yes*, instead of resolving to a downgrade.
