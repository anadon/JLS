# Issue #560: FEAT-C28-4: JLS, Digital and Logisim-Evolution run the same workloads on the same machine with a published harness — and the reported table includes at least one workload a competitor wins
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Not speed. #512 says it plainly: the survey (#510) scored JLS 2/5 on scale/perf
"for lack of receipts, not lack of speed — the deficit is epistemic." #560 is a
credibility instrument. Its product is not a number, it is the demonstrated
willingness to measure yourself against rivals on their terms and publish the
result unedited (KC-28-1). That is genuinely aligned with this project's whole
character — reproducible jars, `.buildinfo`, attestations, normative specs for
the save format and the batch interface, a recorded-decisions section in
ARCHITECTURE.md that says what was *declined* and why. JLS's differentiator is
that its claims are checkable. #560 extends that to comparative claims. Endorse
the outcome.

The design, though, cuts along the wrong seam, and two of the four acceptance
criteria encode proxies rather than the thing they want.

## Reframing 1: build a portable workload corpus, and get the benchmark for free

The expensive, valuable, hard part of #560 is not timing. Timing is
`time.time()` around a subprocess — `riscv/bench_kernel.py:59-62` already does
it. The hard part is the *same workloads* clause. JLS reads `.jls`,
Digital reads `.dig`, Logisim-Evolution reads `.circ`, and none of them import
the others. AC-2 and AC-4 are therefore load-bearing on a problem the issue
never names: **three hand-authored circuits are not the same circuit**, they are
three drawings someone believes are equivalent. Every fairness guarantee in this
feature rests on that belief.

The project already owns the machinery to make it a fact rather than a belief.
`riscv/jlsbuild.py` is a netlist compiler with a tool-neutral core — `Circuit`,
`El`, `Port`, connect-output-to-input — and its own docstring records why:
"the simulator wires elements purely by (element-id, put-name) references —
geometry is irrelevant to simulation." `.jls` emission is the *backend*, not the
model. `riscv/build_cpu.py` then builds a full structural RV32I on top of it.

Add `.dig` and `.circ` backends to that netlist model and the whole shape of
#560 changes:

- **AC-4 (no strawman) becomes structural.** The three circuits are provably one
  netlist, emitted three ways. Nobody has to be trusted about fairness.
- **AC-2 (same workloads) stops being an assertion** and becomes a property of
  the generator, checkable by a test that asserts the three emissions have
  identical element and net counts.
- **The corpus outlives the benchmark.** Three independent implementations of
  digital simulation semantics, driven by one netlist, is a differential oracle.
  That serves `docs/simulation-semantics.md` (which is normative and currently
  has only JLS to check itself against), ARCHITECTURE.md's binding equivalence
  criterion for any future compiled pass (recorded 2026-07-26, #221 — today it
  can only cite the #202 RV32I golden), #588's grading/timing-correctness notes,
  and HDL-export validation. `riscv/fuzz_diff.py` is the same idea against a
  Python reference emulator; this is that idea aimed at real competitors.
- **Timing becomes a flag on the runner**, not a programme.

Under this framing the deliverable is "a cross-tool workload corpus and runner,
one of whose outputs is a timing table." That is a strictly larger asset for
roughly the same work, because the portability problem has to be solved either
way — the issue as written just solves it by hand, once, throwaway.

## Reframing 2: the unit is not comparable, and the table must say so

"120 kHz simulated processor clock" is a number about Digital's semantics as
much as Digital's code. JLS charges for per-element propagation delays,
event-ordered `(time, seq)` retirement, and multi-driver/tri-state resolution
(`docs/simulation-semantics.md` §2/§6/§7/§9). Logisim-Evolution propagates in
ticks. A wall-clock cycles/s column across three tools compares *what each tool
chose to compute*, not how well it computes it. A table that is honest in method
and misleading in substance is worse for this project than no table, because
this project's entire pitch is that its numbers mean what they say.

Concretely: every row of the published table carries a semantics column stating
what that tool computed for that workload, and the corpus contains at least one
workload where JLS's extra work is *visible* — a tri-state bus needing
resolution, or a delay-sensitive race whose outcome differs between the models.
Then a competitor win stops being a ritual concession and becomes an
explanation: here is what the extra nanoseconds bought you, and here is where
they bought you nothing.

The same column is where the #221 decision belongs. ARCHITECTURE.md records
that the discrete-event interpreter is the *sole* strategy, deliberately, with
an explicit revisit trigger ("a concrete CPU-scale design on the `riscv/`
trajectory that is unusably slow interactively"). #560 as written imports a
competitor's success metric into a project that has already recorded, on
purpose, that CPU-scale throughput is not its target. Publishing a loss you
declined to optimize for is fine — publishing it *without stating the decision*
silently reframes JLS as a tool that lost a race it never entered. One footnote
converts a loss into a position.

## Reframing 3: reuse the reproducibility machinery instead of prose

AC-1 wants a third party to re-run the head-to-head; AC-4 wants versions pinned.
The issue's answer is a committed harness plus stated versions in prose. But
this repository already pins whole toolchains for a living: a Nix flake, a
multi-arch container image, byte-reproducible jars with `.buildinfo`, cosign and
attestation verification — all documented in README.md and
`docs/reproducibility.md`. Ship the head-to-head as a flake output (or an image)
that fetches Digital and Logisim-Evolution by URL-and-hash. Then "versions
pinned" is a lockfile fact, "recommended settings" is a checked-in derivation,
and "a third party can re-run it" is one command instead of a page of
instructions. Don't vendor competitor jars — fetch-by-hash keeps the licensing
question out of the tree entirely.

## Disregarding AC-3 as written

AC-3 — "at least one workload where a competitor beats JLS" — is a proxy for
honesty, and a weak one in both directions. It is satisfiable by including one
workload chosen because it loses, and it is vacuous if JLS loses everything.
Worse, it is a criterion about the *result*, which means it can be satisfied by
selecting workloads after seeing timings — the exact failure mode KC-28-1 exists
to prevent.

Replace it with the mechanism that actually delivers what it wants:
**pre-register the workload set.** Commit the corpus and the list of workloads,
in a dated commit, *before the first timing run*, and publish all of them. That
is stronger than "at least one loss," it is checkable from git history, it
matches the discipline the rest of this repository already applies to its own
claims, and it makes the honesty structural rather than editorial. Whether a
competitor wins somewhere then stops being an acceptance criterion and becomes
what it should be: an observation.

## Ordering hazard the issue does not state

#560 orders after #554. But #554 AC-3 adds "2-3 smaller standard circuits" as
tracked fixtures, and #554 AC-2 only requires they live outside `riscv/` — it
says nothing about *how* they are authored. If #554 lands them as hand-drawn
`.jls` files, #560 inherits three hand-authorings per fixture and the reframing
above is no longer available cheaply. Similarly, #413 re-homes the CPU-scale
*fixture*; nothing in #413, #554 or #560 re-homes `riscv/jlsbuild.py`, so the
netlist compiler — the one component that makes cross-tool emission tractable —
is currently slated to be deleted with `riscv/` (D5) as collateral.

Two concrete asks, both cheap if made now and expensive later: re-home the
generator, not just the fixture; and require #554's fixtures to be *generated*
from the tool-neutral description with the emitted `.jls` committed alongside,
so `.dig` and `.circ` backends are purely additive.

## Verdict

endorse-with-reframing. The outcome is right and the honesty discipline is this
project's best asset. Change the seam from "benchmark harness" to "portable
workload corpus with a timing runner"; add a semantics column and the #221
decision to the table so the comparison means something; ship the re-run path
through the flake/container machinery the project already has; and replace
AC-3's "at least one loss" with a pre-registered, fully-published workload set.
