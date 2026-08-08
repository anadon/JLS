# Issue #748: TASK-C575-3: the small-datapath labs complete the pack at eight or more, spanning combinational through datapath
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#575 bets that instructors adopt a course, not a tool, and #517 CAP-33 prices
that bet. #748 is the task where the bet is settled, because the datapath
chapters are the ones a DEEDS instructor cannot get anywhere else and the ones
that are hardest to grade. The count in the title is not the goal; the goal is
that the pack's *last* labs are the ones an instructor trusts most, since those
are the weeks the course is really bought for.

Read that way, three things in this issue are wrong about the route, and one
asset already in this repository makes the right route much cheaper.

## 1. `riscv/` already contains the datapath end of this pack

The issue asks for "ALU slice, register-and-bus arrangement, a controlled
datapath" and never mentions `/home/user/JLS/riscv/`, which holds:

- `riscv/build_cpu.py` — a working single-cycle RV32I datapath built from
  ordinary JLS elements (Adder, Mux, Register, Memory, ShiftRegister,
  Splitter/Binder, Decoder), i.e. an ALU, a register file with read muxes, a
  control ROM and a writeback bus — exactly the three labs, already drawn;
- `riscv/jlsbuild.py` — a netlist compiler emitting FORMAT 1 `.jls` text;
- `riscv/riscv_ref.py` + `riscv/verify.py` — an independent emulator and a
  scoreboard that diffs register file and memory against it;
- `riscv/fuzz_diff.py` — randomized differential testing over hundreds of
  generated programs;
- `riscv/examples/*.clk.txt` — clock stimulus in the literal `-t` grammar
  (`clk 0 until 1000 1 until 2000 0 … end`), which is the one thing #746
  singles out as hard.

ARCHITECTURE.md (§"Simulation execution strategy", ~line 354) already names the
`riscv/` trajectory as a first-class direction of the project. Authoring three
datapath labs from scratch beside it is duplication of the expensive part.

**Concrete alternative: build the datapath labs by subtraction, not addition.**
Take the CPU `build_cpu.py` already produces, blank one block (the ALU; then the
register file's read/write path; then the control ROM), and ship the hole as the
lab. Then:

- the reference solution is not authored — it is the block that was removed;
- the grading oracle is not a hand-written vector file — it is `riscv_ref.py`
  through `verify.py`, which grades a *program*, not three stdout lines;
- the planted defect is not planted — it is a mutation of the removed block,
  and `fuzz_diff.py` finds the distinguishing stimulus, so the CI lane's
  reference-green/defect-red claim is produced rather than asserted;
- the pack's headline stops being "eight labs" and becomes "your students build
  a processor that runs `sum1to10.s`," which is the sentence that actually sells
  a course.

## 2. The oracle this task inherits does not survive a datapath

`docs/capability-roadmap/lf-04-formal-and-grading.md` is blunt about the present
grading model: the `-t` grammar has "not one production [that] mentions an
output," there is no exit status meaning "the run completed and the answer was
wrong," and the shipped criterion in `examples/autograde/autograde.py` is three
literal stdout lines for one input vector — "a submission that is wrong on 254 of
the 256 possible inputs and right on that one passes."

For #744's combinational labs that is survivable by enumeration. For an 8-bit
ALU slice or a controlled datapath it is not: the reachable state space is
astronomically larger than any hand-written vector file, and a hand-planted
defect only proves those vectors catch *that* defect. lf-04 names this failure
mode using this repository's own code — `fuzz_diff.py`'s hand-tuned weights are
called out as "a coverage argument written in a code comment, unmeasured."

So #748 must not silently inherit "grading vectors decide the submission" from
#744. Either it sequences hard after the #300 CAP-06 verdict slice (which #575's
own `ordering_after` already names) and authors specs with verdicts,
counterexamples and a coverage figure — or it adopts the differential-model
oracle from `riscv/`. Both are fine; assuming the golden-diff pattern scales is
not. This is the single highest-value change to the issue.

## 3. The missing seam: a lab is a sealed shell, not a circuit

`docs/batch-interface.md` §2.2 says `-t` names must match input pins in the
*top-level* circuit ("input pins inside subcircuits are not reachable"), and §3.2
says stdout prints only watched `Register`, `Memory`, `OutputPin`, by name. For
a *controlled* datapath that is the whole design problem: either the control
lines are top-level pins — in which case the instructor supplies the control and
the lab is no longer about control — or the FSM is internal, and grading depends
on the student not renaming internal registers.

Nothing in #744's layout, as described, resolves this. The lab format needs an
explicit **shell contract**: a fixed top-level circuit (pin names, widths,
watched-element names) the student may not alter, containing a stub subcircuit
they fill. That contract is checkable — it is precisely the kind of thing #578
AC-2's kit validator should report "by name" — and without it a renamed pin
produces today's fatal `no input pin for signal x`, printed to *stdout* per the
known deviation in §1, which is an appalling first grading experience for a
student. #748 is where this is discovered, so #748 should either define it or
file it against #744 rather than working around it three times.

## 4. Drop the count; ship a spine

I am explicitly disregarding AC-1 as written. "At least 8 labs" is an aggregate,
and #751 exists only to stop that aggregate from being padded — with a manual
non-author review protocol that a single-maintainer project cannot staff.

Make the last three labs a **spine** instead: each lab's reference solution is
the next lab's supplied subcircuit (ALU slice → register-and-bus → controlled
datapath → the CPU in `riscv/gui/cpu.jls`). A spine cannot be padded, because a
missing link breaks the next lab's build — the property #751 is trying to buy
with volunteer labour comes free from the dependency structure. AC-4's chapter
coverage table then falls out of the graph instead of being written as a
defence, and "which chapters are deliberately not covered" becomes a statement
about where the spine stops rather than an apology.

## Where I am arguing against myself

Generation has a real cost the reframing must pay. `jlsbuild.py` says geometry
is irrelevant to simulation — a generated circuit may be unreadable when opened
in the editor, and a datapath lab whose reference solution is spaghetti teaches
nothing. So: generate the *reference and the stimulus*, hand-draw the *starter
shell* the student sees, and hold generated references to netlist review, not
drawing review. Second, promoting `jlsbuild.py` from a `riscv/`-local stdlib
script to a supported kit-authoring tool is a genuine architectural commitment
(cf. the extension-point catalog and the removed plugin mechanism) — it belongs
to #578's kit convention and should be filed there, not smuggled in under this
task's "content only" boundary.

## What I would change in the issue

- Replace AC-1's count with: the pack terminates in a spine ending at a datapath
  that executes a small program, with each lab's reference solution consumed by
  the next.
- Add an AC: the datapath labs' grading criterion is a verdict against a
  reference model or a #300 spec with a coverage figure — not a golden stdout
  diff — and the planted defect is found by search, not by hand.
- Add an AC: each lab declares a shell contract (pins, widths, watched names)
  that grading depends on, and the kit validator checks it.
- Keep AC-2 (verified against the tagged release artifact) exactly as written;
  it is the strongest line in the issue and generation makes it cheaper, since
  the generator can be run against the released jar.
