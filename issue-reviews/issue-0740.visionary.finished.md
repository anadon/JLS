# Issue #740: TASK-C560-1: a committed harness runs JLS, Digital and Logisim-Evolution on the same workloads on the same machine, each at its own recommended settings
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-28 (#512) states the premise plainly: "the deficit is epistemic." Digital publishes
"120 kHz simulated processor clock" and wins the performance conversation by default;
JLS publishes nothing despite having `riscv/bench_kernel.py` in tree. #740 is the
instrument that turns that asymmetry into a measurement. The end is right and I endorse
it. Honesty-as-product (KC-28-1) is one of the healthiest commitments in this tracker.

What I do not endorse is the shape: #740 is filed as *a second harness*, measured in
*a unit that does not survive crossing tool boundaries*, with *no correctness gate*, on
*the one axis where JLS is architecturally weakest*. Four reframings follow. The last
is the one I would actually fight for.

## 1. events/s is a category error across tools — and #740 inherits it silently

#554 AC-1 makes the suite emit "events/s and cycles/s per fixture." #740 orders after
#554 and says "the same workloads," implying it consumes that format (#742's table
certainly will). But a JLS `SimEvent` (`docs/simulation-semantics.md` §3), a Digital
propagation step, and a Logisim-Evolution tick are three different objects. An
events/s column with three tools in it is not a comparison; it is three numbers in
different units printed adjacently, and it is exactly the kind of thing a hostile
reader on Hacker News dismantles in one comment — destroying the credibility the
capstone exists to build.

Only two quantities are genuinely commensurable here:

- **wall-clock seconds for a workload run to a fixed, asserted observable outcome**, and
- **simulated clock cycles per wall second**, where the workload has a clock.

The second is precisely the unit Digital's own claim is stated in, which is why it is
the right rebuttal. The harness's cross-tool schema should carry those two and mark
events/s as a *per-tool internal diagnostic*, never a comparison column. This is not a
detail; it decides whether #742's table is citable.

## 2. The missing acceptance criterion: outputs must agree, or the row is void

Four ACs govern fairness of *configuration*. None governs whether the three tools
computed the same thing. `riscv/bench_kernel.py`'s own docstring already names the
failure mode — "a run that is fast because it stopped computing is visible rather than
silent" — and `riscv/fuzz_diff.py` institutionalizes it as differential testing against
a reference. A cross-tool timing harness without output equality is strictly weaker
than the harness the repo already has.

Add: **every adapter must emit the workload's observable output vector, all adapters
must agree, and a disagreement voids the timing row and files a semantics finding
instead of a number.** This costs almost nothing to build (all three tools have
headless output paths) and it converts the harness's worst failure mode into its most
valuable output — see §4.

## 3. Cut the seam at the adapter, not at a second harness

#740 as filed creates a cross-tool harness *beside* #554's suite. That is a duplicated
runner, a duplicated fixture loader, a duplicated result schema, and a standing risk
that #554 AC-4's format and #740's format drift. The better seam is the one the project
already chose for itself in `docs/grand-architecture.md` §4: a descriptor + factory
table, closed today, opened when a second provider arrives.

Concretely — and this is a change to *#554*, made now, before it ships:

```
bench/
  workloads/…               # tool-neutral: circuit sources per tool + one vector file
                            #   + one golden output vector
  adapters/jls.toml         # tool, release tag, artifact URL, sha256, invocation,
  adapters/digital.toml     #   recommended-settings block + citation URL for it
  adapters/logisim.toml
  run.py                    # one runner: for each (workload × adapter) → one JSON row
```

JLS becomes *adapter #1*, not a privileged first-party path. #554 is then "build the
runner and the JLS adapter"; #740 collapses to "add two adapters, pin them, cite their
settings" — which is genuinely the 0.5–1 mw the band claims, whereas building a second
harness is not. It also makes #742 trivially correct: it renders whatever rows exist.

Two things the adapter descriptor must carry that #740 never mentions, both of which
are this repo's own existing discipline rather than new invention:

- **A pinned sha256 for every downloaded competitor artifact.** `.github/workflows/ci.yml:173`
  (`OSS_CAD_SHA256`) is the established pattern; a benchmark that fetches an unpinned
  jar is not reproducible in the sense AC-1 claims. The counter-example at
  `ci.yml:380` (`JBR_SHA256: "UNVERIFIED-PLACEHOLDER-…"`) shows how easily this slips.
- **The competitor's own licence and redistribution posture**, since nothing here may
  be vendored into a GPL-3.0-or-later tree.

## 4. The prize the issue walks past: a cross-simulator semantics corpus

This is the reframing I care most about. To run one workload on three simulators you
must first agree what the workload *means* — and JLS is the only one of the three with
a normative semantics document (`docs/simulation-semantics.md`: §8 edge triggering,
§9 tri-state and multi-driver resolution, §4 oscillation/termination, §6.2 element
delay discipline). The moment three simulators run the same circuit and must agree on
outputs, you have built a **differential oracle across the tool class** — the same
instrument `fuzz_diff.py` already applies against the RV32I reference emulator, lifted
one level.

That corpus is worth considerably more than the speed table. It is:

- direct feedstock for **CAP-36 (#520) PF-1**, whose minimum bar is "a published
  head-to-head *correctness/grading* write-up" with a runnable appendix — #520 is
  currently ordered *after* #512 on the assumption that perf receipts are what CAP-28
  hands over. Under this reframing CAP-28 hands over correctness receipts too, and
  CAP-36's demo slice gets cheaper, not just earlier;
- validation of `docs/simulation-semantics.md` against the outside world rather than
  only against its own goldens (§12);
- a defensible answer to the fairness objection: a disagreement is a *finding about the
  workload's specification*, published as such, not an accusation about a competitor.

Design consequence: each workload directory should hold a **golden output vector and a
prose note on which semantic choices it depends on**, so that a disagreement is
classifiable (specification ambiguity vs. tool bug vs. adapter error) rather than an
unexplained mismatch.

## 5. On the axis of the fight — and where I disregard the stated criteria

I am explicitly setting aside the framing that this harness's output is a *speed* table.

`docs/capability-roadmap/keystone-c-performance.md` measures JLS's own loop honestly:
37.6% of samples in `BitSet`, **47.7% in the event queue** (`PriorityQueue` 22.3% +
`dupCheck` `HashSet` 25.4%), 4.9% in actual logic. JLS is a pure discrete-event
interpreter that has not yet landed its value-domain or queue work (#476/#475). #740
therefore proposes to open the comparison on the single axis where JLS is least
differentiated and Digital is most practiced — before the engine stack lands. #560's
boundary note anticipates this ("re-running after they land is a re-publication…not a
reason to delay"), and KC-28-1 makes publishing an unfavourable number mandatory. Good.
But choosing *only* that axis is a strategic error the ACs encode without examining.

`docs/grand-architecture.md` §3 and §7 name what JLS actually leads on: an
**enforced-headless core** — "the one discipline on which JLS is already *ahead* of
Digital and Logisim (it has `HeadlessCoreRatchetTest`; they grew GUI-entangled cores
and pay for it)" — plus a batch interface that is a written stability contract
(`docs/batch-interface.md`) with specified exit codes and stream discipline. The
audience for this table is an instructor deciding what to autograde 200 submissions
with. Their question is not events/s. It is: *does it run headless at all, does it exit
nonzero on failure, is the output format something my script may depend on next
semester, and how long does a class-sized batch take?*

So: keep every row #740 and #742 specify — the throughput answer to 120 kHz is
required, and KC-28-1 forbids softening it — but make the harness's unit of work an
**end-to-end headless run of a specified workload**, and let the table carry, beside
wall-clock and cycles/s, the columns that fall out of running the harness at all and
cost nothing extra: display required (yes/no), exit-status discipline, output-format
stability commitment, and setup steps to first result. Those are *measurements the
harness necessarily makes* while doing its stated job, and they are the columns JLS
wins on truthfully. A table where JLS loses the speed row and wins the operability rows
is both more honest and more persuasive than one that loses a single number.

## 6. One liability to design against now

Three third-party jars pinned to named releases become stale the moment any of them
ships. #557's staleness discipline covers JLS's own numbers; there is no scheduled lane
that can plausibly run competitor jars (network, licence, flakiness), and there should
not be one. Make the cross-tool result an explicitly **dated snapshot**: the harness
stamps its output with measurement date, host, and every pinned version+sha256, and
`docs/performance.md` renders those inline. #742 AC-4 already asks for date and harness
commit — extend it to "the table states the competitor versions it measured *in the
table itself*, so a reader in 2028 can see at a glance that it is a 2026 snapshot."
That converts an aging liability into an honest artifact.

## Summary of the reframing

Endorse the goal. Rebuild it as: **one runner with a tool-adapter descriptor, built in
#554 rather than beside it; output equality as a gate, not an afterthought; wall-clock
and simulated cycles/s as the only cross-tool numeric columns; disagreements published
as semantics findings that feed CAP-36; and the operability columns the harness gets
for free carried alongside the speed row it may well lose.**
