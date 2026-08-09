# Issue #478: TASK-0077: a drawn register has a real reset with declared polarity, and both HDL emitters export it or refuse by name — never a register whose reset was quietly dropped
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two ends are braided together here, and only one of them is load-bearing.

The **pedagogical** end — "a register with no reset is not how any real flip-flop
behaves" — is real but weak on its own evidence. In simulation, JLS already has a
defined power-on state: `initSim` drives `init` onto Q at t=0, and
`test/jls/RiscvCpuGoldenTest.java` runs the drawn RV32I fixture
(`test/fixtures/riscv-sum1to10.jls`) for 34 instructions to a deterministic
`x1 = 55` using nothing but `init`. The issue's fourth audience bullet — "a drawn
CPU with no honest reset has no defined instruction 0" — is contradicted by a
green test in this tree. (It also cites `docs/parity-contract.md` and
`docs/virtual-hardware-parity.md`, neither of which exists on `master`; the
evidence-pin comment concedes those citations cannot be re-pinned at all.)

The **export/synthesis** end is the one that actually justifies the work, and
#327 states it more sharply than #478 does: *an initial value is not a reset.*
Synthesis discards `init`; a Tiny Tapeout wrapper's mandatory `rst_n` has nothing
to bind to; `$adff` from an imported Yosys netlist has no counterpart. That is a
fidelity gap at the boundary between the drawn circuit and real hardware, and
that boundary is where the whole project's arc now points (Verilog/VHDL export,
`iverilog`/`ghdl` oracles in CI, `docs/hdl-support-research.md`'s import
programme, #328's shuttle handoff). Sharpening the argument to *"reset is an
export-fidelity defect, not a simulation defect"* does not weaken the case — it
is what makes the priority defensible and what tells the implementer which tests
are the real gate (P7/P8, not P3–P6).

So: right end, aligned with the trajectory, endorse. What follows is where the
cut is one element too narrow and where the issue privately re-solves problems the
codebase has already solved.

## Reframing 1 — the trap is already solved; #478 duplicates the solution privately

Section 7.5 proposes "private helpers `getInput("D")`, `getInput("C")`,
`getInput("R")`" inside `Register`. Those helpers already exist as public API:
`LogicElement.getInput(String)` and `getOutput(String)` at
`/home/user/JLS/src/jls/elem/LogicElement.java:399` and `:419`. `Memory`,
`RegisterFile` and `StateMachine` already use them
(`Memory.java:1343`, `RegisterFile.java:516`, `StateMachine.java:730`). Adding
private duplicates inside `Register` is a step *away* from the shared vocabulary
#398 will need.

More importantly, the trap is not Register-shaped. A census of `inputs.get(`/
`outputs.get(` across `src/` returns positional indexing in `Adder` (six sites),
`ShiftRegister`, `Display`, `InputPin`, `OutputPin`, `NotGate`, `DelayGate` — and
the index-listing `copy()` idiom (`it.inputs.add(inputs.get(0).copy(it)); …`) is
in `Register.java:444-447`, `InputPin.java:111`, `OutputPin.java:101` and
elsewhere. Every one of them will silently drop a pin the day its element grows
one. #478 fixes three sites in one file and leaves the class of defect intact.

The elegant version costs barely more and is squarely in this project's idiom:

1. A generic `Element.copyPuts(Element to)` that copies *all* puts, deleting the
   per-element index lists everywhere (not just `Register`). One method, and
   `copy()` in a dozen elements gets shorter.
2. A **ratchet test** — `PutLookupRatchetTest` — forbidding `inputs.get(` /
   `outputs.get(` inside `jls.elem` outside the puts-management code. The
   repository already has seven of these (`HeadlessCoreRatchetTest`,
   `NotificationRatchetTest`, `NullMarkedRatchetTest`, `PointerApiRatchetTest`,
   `PackageInfoRatchetTest`, `SocketConfinementRatchetTest`,
   `CollabSecurityRatchetTest`); this is the established mechanism for exactly
   this kind of "passes every test, breaks silently later" hazard (Threat T1).

That converts #327's Global Invariant 3 — *"`Clocked` returns the named pin,
looked up by name, never by index"* — from a thing a reviewer must remember into
a thing the build enforces. #478's step-one ("convert the three sites first, run
the suite, nothing changes") is the right instinct executed at one-thirtieth of
the right scope.

## Reframing 2 — the capability interface belongs in this task, not the next one

#327 sequences TASK-0077 (reset on Register) strictly before TASK-0078
(`jls.elem.Clocked`), calling it "necessity, not convention." I think the
dependency runs the other way, and the artifact of getting it backwards is
already visible in #478 §7.4: `Register.getResetKind()` and
`Register.isResetActiveHigh()` as bare methods on one concrete class, with
`HdlExporter` and the dialog `instanceof Register`-ing to reach them.

That is precisely the "base knows its leaves" smell that `Timed`'s own javadoc
(`src/jls/elem/Timed.java:16-21`) says the #78 capability interfaces exist to
retire. Four element types are sequential and none has a reset: `Register`,
`RegisterFile` (`Input("C")`, `RegisterFile.java:154`), `StateMachine`
(`Input("clock")`, `:198`), and sync-write `Memory` (`Input("clock")`, `:196`).
#426 will want reset on `RegisterFile`. #398 will want the clock pin on all four.

A `jls.elem.Sequential` capability in the `Timed` idiom — ~40 lines, headless, no
AWT — declaring the *named* clock pin, the active edge, the reset mode, the reset
polarity and the reset value, with `Register` as its first implementor, gives:

- `HdlExporter` one reset-emission code path instead of one per element type;
- #426 a reset for `RegisterFile` as a second implementation rather than a second
  design;
- #398 its `Clocked` contract already satisfied by construction (it can extend or
  subsume `Sequential`), collapsing "TASK-0077 blocks TASK-0078" from a hard
  ordering into a parallelizable one;
- the H4 compile-break mechanism promoted from `RegisterStatement`'s constructor
  to the type system, where it also protects the elements #478 declares out of
  scope.

Concretely: **I would move `Sequential` into this task**, over #327 §6's stated
sequencing. It is cheaper here (one implementor to get right) than after
`Register` has shipped a Register-shaped API that three siblings must be
retrofitted around.

## Reframing 3 — generalize "export it or refuse by name" into an invariant

The best sentence in #478 is its title clause: *never a register whose reset was
quietly dropped.* H4 is a clever mechanism for it — `public final` fields in one
package-private constructor, so both emitters fail to compile. But H4 protects
exactly one attribute of one element, once, and only against the *next* commit.
The general defect is: **an element attribute that the HDL model has no slot for
is silently absent from the export, and nothing fails.**

`HdlExporter` already has a warn-and-skip list (`:430`, "Element classes with no
HDL meaning") and a warning vocabulary (`:206`, `:495`, `:584`, `:782`, `:872`).
What it does not have is totality over *attributes*. `ElementRegistry` (#78,
`src/jls/elem/ElementRegistry.java`) now enumerates every loadable element type,
and every type carries a declarative `OWN_ATTRIBUTES` list. That makes the
following test cheap and, I think, the single highest-leverage artifact this
issue could produce:

> `HdlFidelityCoverageTest` — for every registered element type, every attribute
> name in `OWN_ATTRIBUTES` must appear in a declared disposition table with one
> of three verdicts: **carried** (named IR field), **irrelevant** (geometry,
> `x`/`y`/`watch`/`trpos`), or **refuses** (export throws `HdlExportException`
> naming the attribute). An attribute with no declared disposition fails the
> build.

That is the project-wide form of the issue's own slogan, it is enforced by the
same registry-totality mechanism `ElementRegistryTest` already uses, and it would
have caught this gap years earlier. It also converts #327's Global Invariant 5
into a test rather than a promise. I would rather ship this plus a narrower reset
than ship the reset alone.

## Reframing 4 — the door #478 opens is the door it leaves open

After this task lands: `reset != none` exports as a reset (good), and
`reset == none` still exports `init` as an initial value that ASIC synthesis
**silently discards** — the exact failure mode the issue exists to end, still
present in the same code path, for every existing circuit in the corpus. §7.11's
table has no row for it.

One line closes it, and it belongs here: when a register has a non-zero `init`
and `reset == none`, `model.addWarning(describe(el) + " has an initial value but
no reset; synthesis discards initial values")`. Cost: a warning string and a
golden line. Value: every pre-existing drawn circuit — including the RV32I
fixture — tells its author the truth at export time instead of at silicon.

## Reframing 5 — the repo already recorded an alternative the issue never engages

`docs/hdl-support-research.md:374-377` and `:465-466` say, in the project's own
words: *"async-reset flip-flops (`$adff` family …) — either teach sync reset,
which `dffunmap` handles exactly, or grow Register with an async-clear pin as a
contained element change."* Yosys's `dffunmap` lowers clock-enable and
**sync-reset** flip-flops to `$dff + $mux` *exactly*, at word level.

Two consequences #478 should state rather than silently override:

- The **import** side (#448, #61) never needs a `sync` mode on `Register` —
  `dffunmap` will have already turned sync reset into a mux the current element
  set realizes today. Only `$adff` genuinely requires a new pin. So the `sync`
  arm is justified by pedagogy and export readability, **not** by the import
  programme, and the issue's §12 row for #448 overstates its own necessity.
- The zero-format-change alternative for sync reset is "draw a mux in front of
  D," and it is already the recorded fallback. I do not endorse taking it — a
  drawn mux teaches multiplexing, not reset, and once the `R` pin exists the
  `sync` arm is nearly free — but a task whose entire method section is
  "convert, add, emit" owes its own repository's prior analysis an explicit
  rebuttal. Add one paragraph to §1.

## What I would keep exactly as written

- **H1** (write attributes only when `reset != none`) and the `Memory.sync`
  precedent. Byte-identical re-save is the right invariant and the right proof.
- **H3** (reset value *is* `init`, no second constant). Correct, and correct for
  the right reason: a second constant is a format addition with an ongoing
  teaching cost and no requesting user. #327's falsification path (report on
  #327, do not add unilaterally) is the right escape hatch.
- **Reset wins over capture on the same edge** (Open Question 1, option a), and
  writing it into `docs/simulation-semantics.md` §8.1 rather than leaving it in
  the implementation. §8.1's worked derivation is cited by four named goldens;
  extending it in prose, with the explicit "nothing changes when reset is
  absent" sentence, is exactly right.
- **P8's insistence that the compile oracles are shown to have *executed***. A
  skipped oracle reporting green is the sharpest observation in the document and
  generalizes well beyond this task.
- **T5's refusal to half-do `RegisterFile`.** Correct — provided Reframing 2
  lands, because then #426 inherits a design instead of inventing one.

## Net

The end is right and squarely on the project's arc: JLS is becoming a bridge to
real hardware, and a register whose reset evaporates at the bridge is a defect in
the bridge. The cut is too narrow in three specific, cheap-to-widen ways — the
put-lookup ratchet instead of three hand conversions, the `Sequential` capability
instead of two `Register` methods, and an attribute-disposition coverage test
instead of one constructor's compile break. Each of those is where the *next*
three issues in this feature (#398, #426, #448) will otherwise pay the same cost
again. Take the reframings; keep the acceptance criteria, which are unusually
good.
