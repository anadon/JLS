# Issue #343: FEAT-033: a parity claim gets an independent counterparty — a pure architectural model, a reference runner that executes it, and a reproducibly built guest stack
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machine block and the DAG walk away and the claim is one sentence: *a parity
result is worthless if the same mind produced both sides of it.* That is correct, it is
the right thing to worry about, and #301's whole headline ("the same golden shared
between a fast behavioral tier and a slow structural tier") is unfalsifiable without
some answer to it. The goal survives review intact.

The *design* does not. The issue converts "independent" — an epistemic property about
whose understanding of the RISC-V spec is being tested — into criterion 6, a syntactic
property about type graphs:

$$\mathrm{deps}(M_{\text{ref}}) \cap \mathrm{deps}(M_{\text{drawn}}) \subseteq \mathcal{B}$$

This is precise, mechanically checkable by ArchUnit, and measures the wrong quantity. A
second RV32I implementation written by the same author, from the same reading of the
same manual, in the same week, satisfies criterion 6 *perfectly* and buys almost nothing.
The issue knows this — Open Question 4 says it in as many words ("one author's misreading
of the specification lands identically on both implementations. The mitigation is an
external conformance corpus, not more tests") — and then files that mitigation as
**"rides along."** Fourteen to twenty-two maintainer-weeks are budgeted for the mitigation
that does not work; the one that does is a parenthesis.

## The implementation census nobody has printed

Counting RV32 architectural implementations this trajectory will have authored, all by
one person:

1. `riscv/riscv_ref.py` — 975 lines, exists today, RV32I complete *including* `lb/lh/lbu/lhu/sb/sh`, which the drawn core deliberately lacks. Already drives `verify.py` (directed) and `fuzz_diff.py` (randomized differential, hundreds of programs), and already produced the expected values in `test/jls/RiscvCpuGoldenTest.java`.
2. The drawn structural machine (#202, absorbing #326).
3. The **behavioral tier** — #301's abstract makes the shared golden a behavioral-vs-structural comparison on the *same `.jls` file* with one attribute flipped (FEAT-031/#325). Something has to execute that tier.
4. `jls.mach` — this issue.

The comment of 2026-08-04 rejects (1) by quoting `docs/machine-calibration.md:87`: it "was
written by the same author as the design under test, so it is a self-consistency oracle,
not an independent one." That sentence is true. It is also true, word for word, of
`jls.mach`. The rejection argument refutes the replacement. Worse, on every axis except
the one criterion 6 chose to formalize, the Python reference is *more* independent than
the proposed Java one: different language, different runtime, different type system,
different numeric model, outside `mvn verify`, no shared build, no shared coverage tooling,
no shared null-annotation discipline. Moving the oracle from Python into `src/jls/mach/`
is a **net reduction** in independence, dressed as an increase, because the metric was
chosen to reward in-tree Java purity rather than uncorrelated authorship.

## The alternative the issue never considers

The project has already written this down and ranked it. `docs/standards-landscape.md`
§13.1 item 2 puts `riscv-arch-test`/RISCOF against the `riscv/` CPU second in the entire
standards document, and says (line 238) *"`riscv/verify.py` already does differential
testing against a reference emulator, which is what RISCOF formalizes"* and (line 745)
that it is *"the only entry in this document that would let JLS make a conformance claim
about a design, not about itself."* #343 does not cite that document once.

**Concrete alternative framing — "the counterparty is not ours."** Make the parity
counterparty an *external* artifact the project did not author and cannot silently agree
with:

- **Oracle:** Spike or the Sail RISC-V golden model, invoked as a subprocess emitting a
  commit log. Both are the RISC-V ecosystem's own reference; neither has ever heard of
  this repository.
- **Corpus:** `riscv-arch-test` under RISCOF, plus the existing `fuzz_diff.py` generator
  retargeted to emit signatures the external oracle can score.
- **Seam:** exactly the boundary ARCHITECTURE.md already ratified. The recorded plugin
  trust-boundary decision (2026-07-26, #222) puts Yosys, GHDL/Icarus and ELK on a
  subprocess boundary and keeps them there; the HDL-export tests already compile with
  `iverilog` when present and *skip cleanly when it is not*. An external ISA oracle is the
  same shape of dependency, tested the same way, with a pinned digest instead of a pinned
  guest kernel.

Under this framing criterion 1 and criterion 6 become *trivially, unarguably* satisfied —
the counterparty is not Java, not in this tree, not in this build, not by this author —
and no ArchUnit rule is needed to defend a property that is true by construction. The
architecture rule exists to protect a fragile arrangement; delete the fragility and the
rule's job disappears. That is the reframing that makes the problem vanish rather than
formalizing it.

## What `jls.mach` should shrink to

Not zero. The residue is real and worth having, and it is small:

- A **`RetireRecord` / architectural-state seam** — one value type, no cycle/pipeline/cache
  fields — that both the drawn machine's probe trace and *any* external oracle's commit log
  normalize into. That is the actual reusable asset, and #347 already owns the comparator
  over it. `jls.mach` becomes an adapter layer, not a machine.
- The **ternary machine (#345)** genuinely needs an in-tree emulator, because no external
  oracle exists for an ISA the project invents. That is the honest justification for the
  package, and it is a *different* justification from parity — it should be stated as such
  rather than smuggled in as "an inhabitant of this package."
- The `@NullMarked` `package-info.java` and a coverage rule set at creation: keep both,
  they cost hours. Note that ArchUnit and the pin-a-rule-before-the-package-exists pattern
  (`allowEmptyShould`, used twice in `test/jls/ArchitectureRulesTest.java` for
  `jls.collab.net` and the replication packages) already exist, so criterion 1's mechanism
  is a half-day, not an invention. The issue prices the package seam at 2 wk; the seam is
  cheap and the *machine* is what costs, which §Cost admits as its 10–18 mw residual.

## The guest stack is a different feature and pulls against a recorded decision

TASK-0071 — kernel, device tree, initramfs, byte-reproducible, digest-pinned sidecar —
is bundled here on the argument that "the runner without an image runs nothing." That is
false for the counterparty role: a reference runner is demonstrable against RV32I test
programs, which the tree already has (`riscv/examples/`, `test/fixtures/riscv-sum1to10.jls`).
The guest image is needed by #301's *boot* claim, not by this feature's *independence*
claim, and bundling them means the independence work cannot land until a Linux image
builds reproducibly.

It also collides with a decision ARCHITECTURE.md records as settled. #221 (2026-07-26)
makes the event-queue interpreter **the sole** simulation strategy, and names its own
revisit trigger: *"a concrete CPU-scale design on the `riscv/` trajectory (#200/#201/#202)
that is unusably slow interactively."* #301 measures the structural boot at 468–485 events
per retired instruction over 4.0×10⁷ retired instructions — 1.7 hours, and that is *after*
FEAT-030's 2.26× constant-factor work. So the guest stack either fires #221's revisit
trigger (which is fine, but should be stated and the follow-up filed) or accepts that the
structural side of the boot-parity claim runs in a lane no student will ever exercise.
#343 mentions neither #221 nor the interpreter-only decision anywhere. A feature that
implies reopening a recorded architectural decision must say so; that is what the recorded
decisions section is for.

Unbundling also dissolves Open Question 1's live half (commit the blob or rebuild it).
There is no blob to place until there is a boot feature to place it for, and the README's
central promise — *"The jar is self-contained — no other files are needed"* — is a real
constraint the sidecar answer is quietly negotiating against.

## Acceptance criteria I am disregarding, and why

I am setting aside criterion 1 and criterion 6 **as stated**, and §5 prediction 2
("independence survives a refactor"). All three defend a property that a same-author
in-tree reimplementation cannot deliver, and that an external oracle delivers for free.
I am also setting aside §5 prediction 3 (two-host image builds) as belonging to whatever
feature owns the boot, not this one. §5 prediction 1 (seed a one-instruction difference,
observe the failure named) I would keep and strengthen: seed it against the *external*
oracle, which is the only version of that test where a passing run is surprising.

## Verdict

**rethink.** The end — a parity claim that can fail — is right and central to the project's
whole architecture-teaching arc. The route is inverted. Invert it back: adopt an external
conformance oracle over the subprocess seam #222 already sanctioned, keep the existing
`riscv/riscv_ref.py` as a second cheap voice rather than deleting its role, shrink
`jls.mach` to the `RetireRecord` seam plus the ternary emulator that genuinely has no
external counterparty, and file the guest stack as its own feature under #301 where its
cost, its `#221` implications, and its residence question all belong.

**Grounding note.** `src/jls/mach/`, `test/jls/mach/` and `scripts/build-guest-image.sh`
are indeed absent at this checkout, and `pom.xml`'s bundle floors (0.545/0.535/0.505) and
`jls.sim` package rule (0.930/0.920/0.845) verify as quoted. But `docs/parity-contract.md`
and `docs/machine-calibration.md` — the sources for §Related work's parity-status quotation
and for every calibration number #301 and this issue lean on — **do not exist on the
default branch**; neither does `docs/plan/`. Every number in the cost and boundary
arguments above is therefore currently unverifiable from the repository a contributor
would clone.
