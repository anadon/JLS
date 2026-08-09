# Issue #392: TASK-0079: the RV32 machine exists as drawn boundaries with a tested element census, and its ALU and register file are green against an independent reference
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two goals are fused here, and they do not want the same artifact.

**Goal A (curriculum).** Its re-homed parent #202 and the absorbed FEAT-038 both state it as
"a general-purpose processor exists as a circuit a person can open, read and step through."
That is the README's own mission — "an educational digital logic circuit editor and
simulator" — and it is satisfied by a *readable* machine that ships where a student finds it.

**Goal B (parity).** The dependency web tells a different story: #477 (`jls.mach`), #390
(`RetireRecord`), #423 (comparator), #395 (guest image), `docs/machine-calibration.md`'s
16550/CLINT/Sv32/Linux-boot calibration. That programme wants a *second implementation* to
diff against, and the census's "Sv32 ≈ 750" and the `csr`/`clint` boundaries are its
fingerprints, not a student's.

This task's seven-boundary SoC is the union of both. Neither goal needs the union, and the
union is what makes FEAT-038's own band 12–26 maintainer-weeks with this task explicitly
"not the machine."

## The value ordering is inverted

Today, `test/fixtures/riscv-sum1to10.jls` is a working single-cycle RV32I processor with a
green integration golden (`RiscvCpuGoldenTest`, 34 cycles) and an independent oracle that
already exists and already found nothing wrong (`riscv/riscv_ref.py`, 11 directed programs,
a randomized differential fuzzer, per-element validation). The *only* things standing
between that artifact and Goal A are #62 (auto-layout — the generated circuit's coordinates
overlap) and #73 (ship it as a first-run sample). #202 says this itself: its planned
sample-circuit direction is gated on exactly those two.

#392 delivers no part of Goal A. Nothing it produces is installed, laid out, helped, or
openable by a student who did not clone the repository. `machines/` at repo root (Open
Question 3) is a maintainer artifact wearing a curriculum label, and §11 concedes the
generated half is unreadable until #62 lands. Meanwhile it costs #412 (op verbs), #477 (a
whole pure ISA package under the tree's strictest coverage floors), #457 (memory byte
lanes), #389, plus an undecomposed residual of five more boundaries — and only *then* does
the student-facing payoff still wait on #62 and #73.

**Redirect 1.** Land #62's layout pass over the *existing* generated CPU and ship it via
#73. That discharges the capability statement of the re-homed parent with the machine that
already exists and already has a green golden, at a fraction of the cost, and it makes every
later boundary decomposition an improvement on a shipped thing rather than a prerequisite
for one.

## The differential seam is cut in the wrong place

H1 asserts each boundary is "independently verifiable against `jls.mach`." But #477 §7.4
publishes `jls.mach` as `step(ArchState, MemoryView) -> StepResult`: an *instruction*-granular
pure function. There is no `alu(op, a, b)` and no register-file model in that interface, by
design — its purity and its 0.930/0.920/0.845 floor depend on being small.

So per-boundary differentials resolve one of two ways. Either #477 grows a microarchitectural
decomposition shaped to match the drawn machine's cut — in which case the "independent"
counterparty is a mirror of the design under test, which is precisely the failure #477's own
Threat T2 names — or #392 writes its own per-boundary references, in which case the
independence claim is about #392's second implementation, not #477's.

The two boundaries chosen for green are exactly the two that escape this, because the ISA
spec itself defines them: an ALU op and "x0 reads zero" are spec-level statements, not
microarchitectural ones. **The method is validated on its two easiest cases and claims
generality it has not earned.** Decode, load/store, CSR and CLINT have no spec-level
per-boundary counterparty; their cut is a design choice, and a differential across it tests
that #392 and #477 made the same choice.

**Redirect 2.** Make the *retirement trace* the only differential object — it is
instruction-granular, spec-defined, already the parity object of the whole cluster (#390),
and genuinely independent. Then per-boundary claims become **fault-sensitivity** claims
rather than pretend-differentials: perturb one gate inside `alu.jls`, assert the trace
diverges. That is a stronger statement than the issue's own goal ("whatever is drawn is
checked") because it proves every drawn element is *load-bearing*, not merely counted — and
the repo already runs PIT with an 80/82 threshold and has `docs/mutation-testing-trial-2026-07.md`,
so the idiom is in-house.

## The census-as-budget is a second copy of the truth

P3 asserts exact per-tag element counts against a hand-written markdown table, parsed by a
bespoke package-private parser (§7.5). Consider what that protects against. The `.jls` files
are tracked in git — silent growth already shows in a diff. Behaviour change already shows in
the golden. What the census adds is a hand-maintained restatement of a fact the repository
already holds, which must be edited on every legitimate change, and whose failure mode H2
already anticipates ("noise rather than signal"). Prose that a test *parses* is durable state
that can drift; prose a test *emits* cannot.

**Redirect 3.** Invert both documents. `CENSUS.md` becomes a generated report the test
writes (or a `git diff --exit-code` check on a generated file), so it is always true by
construction. `BRINGUP.md`'s invariant — P7's `green ⇒ harness ∈ M` — stops needing a
markdown grammar at all: enumerate the `SubCircuit` instances from the loaded top level,
enumerate the differential tests by annotation, and assert a **bijection**. That is P7 and P8
together, strictly stronger (it catches a boundary with *no* row, which P7 cannot), with
zero new grammar and no drift surface.

## D5 asks for a property; the plan pays for a rewrite

D5's operative concern is real and well-put: a generated circuit must be one "the editor
could not have made" — bypassing validation, undo, and the collaboration observers is a
genuine hazard, and O6 shows the raw-constructor path is wide open. But that concern is a
*property of the committed artifact*, and properties are cheaper to assert than to
reimplement. A test that loads any committed `.jls`, asserts it is a fixed point of
load→save (`DeterministicSaveTest`'s idiom), and re-validates it through the same rules
`OpSink` enforces, discharges D5 for *any* producer — Python, Java verbs, or a human with a
mouse. With that test in place, #412 stops being a blocker for this task, and the deletion of
`riscv/` (#413) stops taking a working differential harness with it before its replacement
exists.

## The one hypothesis worth keeping, and the one that is missing

P2 — the `RegisterFile` behavioural golden, replacing the exemption at
`test/jls/ElementSimulationGoldenTest.java:546` — is unambiguously good, independent of every
dependency in the machine block, and roughly a day's work. It is currently buried under a
12-26-week feature. File it as its own issue and land it this week; it needs nothing from
`machines/`, `jls.mach`, byte lanes, or op verbs.

What is *missing* is the highest-value fact this task could produce. `ARCHITECTURE.md`'s
recorded decision on simulation strategy (#221) names its own revisit trigger: "a concrete
CPU-scale design on the `riscv/` trajectory that is unusably slow interactively." A ~580-element
machine with a measured events-per-cycle figure either fires that trigger or closes it — and
it decides whether #325's fidelity toggle has a reason to exist at all, since a per-instance
"run this behaviorally" switch only pays when the drawn version is too slow to step. The
issue has that measurement as a *column in a table* (§7.6, events-per-cycle) rather than as a
hypothesis with a consequence. Promote it to H5 with an explicit reporting duty against #221
and #232; it is worth more to the project's trajectory than every census row combined.

## If the programme proceeds anyway

Split the artifact along the seam the two goals actually have:

- **Curriculum machine** — hand-drawn, laid out, shipped in-jar via #73, with a help page;
  no CSRs, no CLINT, no MMU hedge; small enough that a human authored every element, which
  caps it far below 580 and makes "a person can read it" true rather than aspirational.
- **Parity machine** — never drawn at all. The parity claim between `jls.mach` and the
  simulator does not require a *drawn RV32*; it requires circuits whose semantics both
  models agree on, and #423 already names the cheap and genuinely external resolution
  (committed riscv-tests ELFs plus reference signatures). The drawn SoC's role in the parity
  programme is decorative, and dropping it removes the largest single cost in the cluster.

I am explicitly disregarding this issue's acceptance criteria — the seven-boundary tree, the
census budget, the two parsed markdown ledgers, and the per-boundary differential verdicts.
The goal above them ("whatever is drawn is checked", and a processor a person can open) is
right; the artifact proposed to reach it is the most expensive route available, and it
reaches Goal A last.
