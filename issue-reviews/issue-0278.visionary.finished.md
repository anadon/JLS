# Issue #278: RV32I integration-golden breadth: promote the fib and memtest directed programs into committed fixtures run by a parameterized RiscvCpuGoldenTest
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal is "widen the strongest regression net the suite has" from one program to
three. The real stake is larger and is recorded elsewhere: `ARCHITECTURE.md`'s
"Simulation execution strategy" decision (#221) binds *any* future levelized/compiled
evaluation pass to "agree bit-for-bit with the #202 RV32I integration golden run as a
differential oracle," and `docs/grand-architecture.md` §2/§6 names the `riscv/` CPU-scale
trajectory as the exact pressure that would trigger that second strategy. So the artifact
#278 is building is not a test — it is the **equivalence oracle for a second simulator
kernel**, plus the acceptance gate for the HDL-export oracle in #202.

An oracle with that job is judged by *how cheaply it grows*, not by whether it has three
entries today. Three hand-curated fixtures with hand-transcribed expectation tables is an
oracle whose marginal program costs ~120 KB and a Java edit. That is the wrong cost curve
for the role, and the issue's own Open Question ("promote 2 programs or all 11?") is the
symptom: the question only exists because each program is expensive.

## Reframing 1 (primary): the program is data; the circuit is the fixture

I checked the fixture. `test/fixtures/riscv-sum1to10.jls` is 9,360 lines / 1,038 elements
(810 `WireEnd`, 43 `Mux`, 34 `Splitter`, 32 `Register`, 3 `Memory`, …). Of that, the bytes
that depend on which program is running are **one attribute on one element**:

```
ELEMENT Memory
 String name "imem"
 int cap 32
 String init "0 93\n1 100113\n2 b00193\n3 2080b3\n4 110113\n5 fe314ce3\n6 102023"
```

Everything else is program-independent hardware. Committing `riscv-fib.jls` and
`riscv-memtest.jls` copies ~9,300 identical netlist lines twice — roughly 240 KB of repo
growth — to carry about 200 bytes of actual new information, and it multiplies §11's own
stated staleness threat by three: when `riscv/` tooling changes its emitted netlist (or
when #202's open question about rebuilding the CPU on #201's `RegisterFile`/`FieldExtend`
resolves *yes*), every fixture must be regenerated and every 9k-line diff re-reviewed.
This also pulls against the repo's own instinct: plain-text saves exist precisely so
circuits "diff cleanly in version control" (README, "Circuit files").

The split is available today with **no production-code change**, which matters because
§10's stop condition is exactly "if fixtures cannot be produced without Java-side generator
changes, REPLAN on #202":

- `Memory.setInitialValue(String)` is public — `src/jls/elem/Memory.java:270`.
- `Memory.initSim` re-reads `initialValue` at simulation start and copies it into the
  running store — `src/jls/elem/Memory.java:1294` (`initOK(initialValue, capacity, bits, true)`)
  then `:1309` (`mem = initMem.copy()`). `BatchSimulator` calls `initSim` per run.

So the harness becomes: load **one** committed CPU fixture, find the `imem` Memory, set its
init text from a tiny committed program file, run. Concretely, `test/fixtures/riscv-cpu.jls`
(one netlist) plus `test/fixtures/riscv-programs/{sum1to10,fib,memtest,…}.imem` at ~7 lines
each. `clockVector()` in `RiscvCpuGoldenTest` is already program-independent; only `STEPS`
varies, and it comes from the program's data file.

What this buys, in order of importance:

1. **The next task on #202's critical path becomes possible.** #202 says the planned Java
   fuzzer port "reuses #278's harness." A randomized program *cannot have a committed
   fixture* — so a harness parameterized over committed fixtures is precisely the shape the
   fuzzer port cannot reuse, and #278 as written would have to be substantially redone.
   With the fixture/program split, the fuzzer port needs only a Java assembler + emulator;
   it never needs a Java netlist generator (the hardware is the committed fixture).
2. **"2 or 11?" dissolves.** At ~50 bytes and zero Java per program, promote all 11 from
   `riscv/verify.py:97`. The Open Question and its "revisit with measured fixture sizes"
   follow-up both disappear.
3. **The #201 rebuild question decouples.** Rebuilding the CPU on `RegisterFile`/`FieldExtend`
   regenerates one fixture, not N, so #202's recommended-default "no, don't rebuild yet"
   stops being partly a cost-of-churn argument.

Honest costs of the reframing, and how to pay them:

- The test no longer proves `make_cpu.py` emits a correct netlist *for fib specifically*.
  That is fine: the netlist is program-independent by construction, `make_cpu.py` ships in
  no artifact, and one committed fixture still exercises the whole `Circuit.load` /
  `finishLoad` path on a 1,038-element circuit.
- Capacity headroom: the committed fixture is `imem cap 32` / `dmem cap 16`. Regenerate the
  single fixture at generous caps (the cap only affects the init string, which is RLE'd —
  `Memory.encodeInitRLE`, `:457` — so an all-zero 256-word dmem is nearly free in bytes).
- **A real trap to guard:** `initSim` on a bad/oversized init text *warns and zeroes*
  (`:1294-1305`, "all zeros assumed") rather than failing. A silently zeroed ROM yields a
  CPU that executes nothing, which is a green run for any expectation table that happens to
  be mostly zeros. The harness must assert the program actually landed — read back an imem
  word, or assert the PC advanced by `4 * steps` — before asserting architectural state.
  This hazard exists in the committed-fixture design too; it is simply more visible here.

## Reframing 2: expectations as generated data, not hand-typed Java constants

§7.6 puts "per-program step counts and expected register/memory tables in the test source,
each annotated with the generating command," and P2 verifies once, in a PR comment, that
those constants match `riscv_ref.py`. That verification then never runs again — a human
transcription with a one-time check is not an oracle, it is a snapshot of one.

Have `riscv_ref.py` (or `make_cpu.py`, which already prints exactly this — `riscv/make_cpu.py:62-71`)
emit a `.expected` sidecar next to each program: `steps=34`, `x1=55`, `x2=11`, `dmem[0]=55`.
Commit it; parse it in the test. Oracle independence is preserved (the emulator is still the
source), the transcription step is deleted, regeneration is a diffable one-command operation,
and adding a program becomes **two small text files and zero Java edits**. That is the
growth curve the #221 equivalence-oracle role needs.

## Reframing 3 (out of #278's scope, but name it on #202)

Once expectations are data, the honest end state is a **test-only Java RV32I oracle** so CI
can *generate* expectations instead of reading committed ones — which is the same component
the planned fuzzer port needs. `riscv/riscv_ref.py` is 975 lines including the assembler and
a self-test; the emulator core is a fraction of that. I would file that as #202's *enabling*
task rather than a downstream one, and keep #278 at: split fixture from program, make
expectations data, promote all 11 directed programs.

## What to keep verbatim

P3 — the induced memory-timing fault, demonstrating the new goldens fail where sum1to10
passes — is the only prediction that measures the property the issue actually claims. Keep
it, and strengthen it now that programs are cheap: run the induced fault against **all**
promoted programs and report which ones catch it. That converts "2 or 11?" from an opinion
into a coverage measurement, and it gives #202's IC1 real evidence rather than an anecdote.
The rest of the issue's discipline — emulator-derived expectations, no papering over a
divergence (§10), no new SpotBugs exclusions — is right and should stand.

## Explicitly disregarded acceptance criteria

I am disregarding §7.6's "New fixtures `test/fixtures/riscv-fib.jls` and
`test/fixtures/riscv-memtest.jls`" and §8's "parameterize over (fixture, steps, expected
registers, expected memory words)". Reason: the fixture axis carries ~9,300 lines of
duplicated, program-independent netlist per entry, and the harness shape it produces is the
one #202's next planned task cannot reuse. Parameterize over **(program, expectations)**
against a single committed CPU fixture instead. Everything else in §14 survives unchanged,
including "sum1to10 assertions byte-for-byte equivalent" — under this design its assertions
are the same four values, read from a data file instead of a Java constant.
