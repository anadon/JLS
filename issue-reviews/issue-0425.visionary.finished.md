# Issue #425: TASK-0080: the guest-visible byte stream is written to its own file handle and compared byte for byte, and changing the clock period changes no output byte
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and three claims are underneath: (a) JLS can run a machine large
enough to boot Linux and emit bytes; (b) those bytes are reproducible enough to commit
and `cmp`; (c) two implementations of the same machine agree, with the byte stream
standing in for per-instruction comparison because interrupts break instruction
alignment. (a) and (b) are the right end state and they strengthen the project's arc in
a way the issue does not claim for itself. (c) is overclaimed, and the mechanism chosen
to defend it — the clock-period falsification guard — does not test what the issue
believes it tests.

The arc fit is stronger than #425 argues. `ARCHITECTURE.md`'s recorded simulation-strategy
decision names its own revisit trigger — *"a concrete CPU-scale design on the `riscv/`
trajectory (#200/#201/#202) that is unusably slow interactively"* — and binds any future
levelized/compiled pass to agree *bit-for-bit with the #202 RV32I integration golden*. A
structural Linux boot at 1.2–6.0 h **is** that trigger, and this task's transcript is the
natural successor to `riscv-sum1to10.jls` as that differential oracle. That is the real
strategic value here, and it argues for making the transcript a cheap, reusable,
required-gate artifact rather than one nightly `cmp`.

## The load-bearing half does not bear load

§5 P3 calls `clockPeriodDoesNotChangeAnyOutputByte()` "the cheapest test in the programme"
and gates everything on it. Read against the code, it is a tautology wearing a
falsification test's clothes.

1. **`--clock-period` does not "change every simulated time."** It scales `Clock.cycleTime`
   (`src/jls/elem/Clock.java`, `private int cycleTime`) and leaves every element's
   propagation delay untouched. What actually changes is the *ratio* of clock period to
   logic delay — the design's timing margin. For any multiplier ≥ 1 that is a pure margin
   increase, so a green result is guaranteed by construction and tells you nothing.
2. **Nothing in a drawn machine can observe simulated time.** `Simulator.now`
   (`src/jls/sim/Simulator.java:36`, `:228`) reaches elements only as the scheduling base
   in `react(long now, …)`. Across all of `src/jls/elem/`, no element converts `now` into a
   circuit value. A guest can count *cycles* (mtime, `rdcycle`, a UART divisor) but cannot
   read *time*. Scaling the period scales edge timestamps, not edge counts.
3. **`-d 0` removes the one thing that was sensitive.** The loop guard is
   `now <= maxTime` (`Simulator.java:217`). With a finite `-d`, scaling the period
   truncates the run and *would* change bytes. With `-d 0` — which P2/P3 both require —
   the run is purely cycle-indexed and time is a pure output. The task deliberately
   deletes the guard's only real sensitivity and then calls the result a proof.
4. **§14's demonstration cannot be constructed.** The Definition of Done requires the guard
   "shown failing on a deliberately time-encoding golden." The obvious construction is to
   re-enable `printk` timestamps — but `printk` timestamps derive from mtime, which counts
   clock *edges*. Scale the period and those timestamps are byte-identical. The golden is
   maximally time-encoding and the guard stays green. A criterion whose demonstration is
   unconstructible is not a criterion.
5. **Open Question 4's fractional multiplier is a false-alarm generator and is partly
   unrepresentable.** Below 1× the guard can go red for a *correct* reason — a genuine
   setup-time violation, which is exactly what a digital simulator should show — while §10
   instructs the reader to "stop: the golden encodes time." Also `cycleTime` is `int` and
   `Clock.checkOneTime` requires `1 <= oneTime < cycleTime`; 0.5× of a cycle time of 2 is
   unrepresentable.

The invariant §7.10 actually writes down — $T$ constant in $\tau$ — is a *units* change:
scale `cycleTime` **and every propagation delay** by $k$. That is trivially true over
integer event times and measures the simulator, not the machine.

## Reframing 1 — the guard is a property of the machine's boundary, and it does not need Linux

The honest question is "can simulated time reach a guest-visible byte?" In an RV32 SoC the
answer is a short, enumerable list of boundaries: mtime/mtimecmp, the cycle/instret CSRs,
and any UART divisor that turns baud into byte timing. That makes the right check **static
and structural**: over the elaborated design, assert that every element whose output
depends on `now` is on a declared list, each entry carrying a reason — the same shape as the
exclusion-set ratchet, and the same shape as `HeadlessCoreRatchetTest`. Seconds, in the
required gate, on every push.

The dynamic complement is equally cheap and does not need a kernel: a program that prints a
fixed string, takes a timer interrupt, prints it again, run at 1× and 10×. Two seconds of
simulation, on a fixture that **already exists** (`test/fixtures/riscv-sum1to10.jls`, pinned
by `test/jls/RiscvCpuGoldenTest`).

This is the single highest-leverage change to the issue. §8 says write the guard **first** —
but as specified it cannot be written at all until `--transcript`, `-console replay:`,
`-d 0`, `machines/` and a pinned Linux image all exist (#395, #392, #378, TASK-0069,
TASK-0012). The cheapest test in the programme is currently parked behind the five most
expensive dependencies in the tree. **Split it out.** A `clock-period invariance` task
against the existing RV32 fixture has zero blockers, lands this month, and delivers the
invariant to the whole simulator instead of to one nightly boot.

I am explicitly disregarding §14's "written first and shown failing on a deliberately
time-encoding golden" as written: it is unconstructible (point 4) and it is sequenced
last-by-construction despite being labelled first.

## Reframing 2 — cut the transcript seam at the element, not at the CLI

§7.1 adds two `FlagSpec` rows. Two problems and one better seam.

The mechanical problem: `JLSStart.parseCommandLine` (`:845-858`) matches single-dash flags
by longest-prefix over `arg.substring(1)`, and `--` is already the end-of-flags marker
(`:838`). The whole existing table is single-dash long options — `-vcd`, `-export`,
`-savetext`, `-board`. `--transcript` and `--clock-period` would be the first double-dash
options in the tool, and the issue's own P2 command mixes conventions in one line
(`-console replay:…` beside `--transcript`). Either commit to a double-dash family
deliberately (a real, documentable decision touching `usage()`, `docs/batch-interface.md` §1
and `CliFlagTableTest`) or name them `-transcript` and `-clockperiod`. Do not let it happen
by accident in a task about byte-exactness.

The design problem: JLS **already** writes a byte-comparable, own-file-handle,
golden-pinned artifact out of batch mode — the VCD file, with `VcdExportGoldenTest` and
`docs/vcd-interop.md` behind it. A UART TX stream is a *projection* of a watched net,
sampled at its strobe edges. Framing `--transcript` as a named projection over the existing
`BatchSimulator` probe/sample seam reuses cross-platform byte discipline, the `-text` path
question, and the golden-provenance habit, and it disarms O3 the way `-vcd` already
disarmed it — nobody worries that a malformed `-t` file corrupts a VCD.

The better seam is one level further out: make the console tap an **element**
(`SerialPort`/`ConsoleTap`) whose sink is a save-format attribute. Then the machine declares
where its console goes, the same `.jls` works in the GUI and in batch with no flag at all,
TASK-0069's console pane is the GUI rendering of that element rather than a parallel
mechanism, and "live interaction is demonstrated, never asserted" becomes a property of the
element's two bindings rather than a policy note. The cost is real (`ARCHITECTURE.md`'s
sixteen-step element checklist, and #78 is the recorded direction for shrinking it) — but
this is precisely the kind of capability that should be a drawable thing, not a CLI verb.
The issue's "no new Java API, no GUI change" self-limitation is what forecloses it, and that
limitation is not defended anywhere.

## Reframing 3 — the stream regime is a demonstration, not the parity claim

§7.10 forbids per-instruction comparison and fences the decision: *"do not let a reviewer ask
for one."* The reasoning is sound as far as it goes — a timer interrupt lands between
different instructions on two tiers, so retirement indices desynchronize. But the conclusion
overshoots. Interrupts break alignment *at* interrupt boundaries; they do not break it
*between* them. The regime the issue never considers is **per-instruction comparison within
interrupt-free windows, re-synchronized at each interrupt boundary** — strictly stronger than
either regime alone, and a natural home for #423's comparator.

This matters because a kernel log is a function of software control flow, not
microarchitecture. Two implementations can differ in interrupt latency, memory ordering and
every architectural register and still print the same 40 KB. `cmp` passing is a magnificent
*demonstration* that both machines ran Linux; it is a weak *oracle* for structural parity.
§11 half-admits this ("says nothing about state the stream does not project") and then §13
and the Abstract say "that single `cmp` is the entire parity claim at Linux scale" four
times. Pick one. The demonstration is worth doing on its own merits — just stop calling it
the claim.

## Reframing 4 — the guest image is a flake output, not a fixture

Open Question 3 recommends generate-on-demand from a pinned recipe. Endorse hard, and name
the mechanism the repository already has: **`flake.nix` is at the root**. Nix is the most
credible existing answer to "byte-reproducible kernel + DTB + initramfs," and expressing
#395's image as a flake output makes the large-fixture question disappear — the lane
evaluates a derivation, nothing multi-megabyte enters git, and the pin is a lock-file hash
rather than a prose recipe. This also keeps faith with README's identity: the jar and
`bom.json` are the byte-reproducible artifacts and the project is loud about which
guarantees are which. A committed kernel blob would be the first artifact in the tree with
neither reproducibility nor attestation.

## Reframing 5 — generalize the provenance ratchet

P5 is the best idea in the issue and it is scoped too narrowly. "No transcript-derived
fixture was produced in live console mode" is one instance of "every committed fixture
records how it was made." The tree already needs this: `riscv/` fixtures are generated by
Python commands recorded only in prose (#202 §4, "generated by recorded `riscv/` commands").
Make it a fixture-provenance manifest over `test/fixtures/**` — regeneration command, mode,
tool version, per fixture — checked by one ratchet test. Same cost, project-wide payoff,
and it is landable now with no blockers.

## Where the issue pulls against the arc

- **Advisory-forever risk.** §7.12 claim 5 keeps the lane non-required until #265 settles
  cross-platform byte identity. A multi-hour nightly that can never gate is a lane whose red
  nobody is obliged to read. If the guard is split out per Reframing 1, the *invariant* is
  required and the *boot* is advisory — which is the correct division.
- **Cost concentration.** Five blockers plus two unfiled prerequisites, an unwritten
  `docs/parity-contract.md` (absent from `docs/`), an unwritten
  `docs/virtual-hardware-parity.md`, a nonexistent `machines/` tree, and a 1.2–6.0 h manual
  expedition — all to produce one `cmp`. Every genuinely reusable idea in the issue (the
  guard, the provenance ratchet, the exclusion ratchet) is independent of all of it.

## Recommendation

Keep the end state. Re-cut it as three issues: (1) **clock/time-observability invariance**
on the existing RV32 fixture, static list plus 10× dynamic check, required gate, zero
blockers, landable now; (2) **fixture-provenance manifest** over `test/fixtures/**`,
generalized from P5, zero blockers; (3) **the Linux boot transcript and its golden**, keeping
its dependency chain and its advisory lane, with the transcript sink built as a projection of
the existing probe/sample seam (or, better, as a console-tap element) and its double-dash CLI
convention decided deliberately. Drop the framing that a single `cmp` is the parity claim;
call it the demonstration and let #423 carry the claim on interrupt-free windows.
