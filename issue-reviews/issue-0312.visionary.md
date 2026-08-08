# Issue #312: CAP-17: a design too large for any one machine elaborates, simulates and reports as one design across a cluster — and a campaign of thousands runs across a grid
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the 62 KB of planning apparatus and #312 is three unrelated wishes in one wrapper:

1. **Loading and simulating stop being superlinear** (FEAT-005 #353) — a real defect, present at
   HEAD, cheap, and useful to every user.
2. **Many independent runs dispatch and aggregate** (FEAT-057 #350) — a grading/verification
   workflow that touches no simulation internals at all.
3. **One design is partitioned across hosts and simulated as a unit** (FEAT-004/014/054/055/056/035,
   plus the measurement and checkpoint spine) — a new distributed simulator.

Only (3) is what the title claims, and only (3) carries the 38–62 mw marginal band, the transport,
the barrier discipline, the byte-identity-across-N obligation, and the 1,440-partition-file problem.
(1) and (2) are hostages: they are the parts with users, and they are being carried by a capstone
whose own acceptance criterion AC-6 promises that **nothing changes for anyone who uses JLS today**.
When a capstone's pedagogy guard is "zero new palette entries, no measurable difference in the
default experience," it has told you it is not a JLS feature — it is a different product sharing a
file format.

## The trajectory it is measured against

- The largest circuit in this repository is `test/fixtures/riscv-sum1to10.jls` at **1,038 `ELEMENT`
  records**; `riscv/gui/cpu.jls` is 52 at the top level. The issue's own calibration fixture is a
  1,551-element CPU. The project's most ambitious artifact — a hand-drawn RV32I datapath, the thing
  `riscv/README.md` calls "exactly what JLS is *for*" — is **10^3** elements.
- The first wall is at ~1.65×10^5, the file cap at ~6.9×10^5. Those are **100×** and **450×** past
  the largest design anyone in this ecosystem has ever drawn.
- The acceptance target is 10^9 and the ambition 10^10: **six to seven orders of magnitude** past
  every real artifact, and the issue concedes in Background that no public design reaches it
  (XiangShan ≈ 20 VU19Ps ≈ 1.8×10^8 cells) so "a generated stressor is unavoidable." A capability
  whose only demonstrable workload is a generator written to demonstrate it is a tautology, not a
  capability.
- `ARCHITECTURE.md` already records the governing decision (#221): the event-queue interpreter is
  JLS's **sole** execution strategy, and the revisit trigger is "a concrete CPU-scale design on the
  `riscv/` trajectory that is unusably slow interactively." That trigger has not fired. The project
  declined to build a *levelized compiled pass on one machine* as premature; #312 proposes to skip
  that and build a *distributed* one. This pulls against a decision that is written down with its
  rationale, and #312 never cites it.
- #508 (the commissioned product review) dispositions this capstone **Defer (split)** on independent
  grounds: adoption of this repo is zero, the live users are a course on `bsiever/JLS`, and the filed
  programme prices at ~22 maintainer-years against bus factor 1.

One grounding fact worth recording: **`docs/plan/` does not exist at HEAD.** Every §2 status cell,
every Cost row and every "corrected" total in this issue cites `docs/plan/features/**` or
`docs/plan/REGISTRY.md`, none of which is on master (that is #493, flagged as "corpus bleed" in
#508). The entire cost apparatus — three comments of arithmetic about arithmetic — is unauditable
from the checkout it claims to be verified against.

## The reframing that makes most of the problem disappear

**JLS already counts a 4 GiB RAM as one element.** `src/jls/elem/Memory.java` holds contents in a
`Map<Integer,BitSet>` — behavioral storage behind a single model node. The 10^9 figure is a count of
*drawn gates*, not of simulated state, and JLS's own most-used element already proves that the way
to hold a large system in a small model is a behavioral leaf, not a partitioned host.

That is exactly the mechanism #312 lists as **FEAT-031 #325 "beneficial, and deliberately outside
the required set"** — dismissed because "the partitioner cuts on nets, not on fidelity boundaries."
That reasoning is backwards: it treats the partitioner as the fixed point and asks whether fidelity
helps it. Invert it. Mixed-fidelity abstraction is how every commercial flow reaches these scales
(behavioral memories and IP blocks, not billion-gate flat netlists), it needs no transport, no
barrier protocol, no null-message lookahead, no cross-partition cycle refusal, and — decisively — it
is verifiable by ordinary single-process golden tests, which is the discipline this project is
actually good at. The seam to cut along is **fidelity, not hosts.**

## Concrete alternatives, in the order I would fund them

**A. "No design a JLS user can draw ever hits a wall." (~5–10 mw, replaces eight of eleven rows.)**
Ship FEAT-005 (#353) — the `LinkedList` → `LinkedHashSet` swap at `src/jls/Circuit.java:1345`/`:1369`
is real and I confirmed it at HEAD — plus the recursive-walk fix, the spatial-index rebuild
(currently orphaned, Open Question 6), and a **save-side** size check. Then re-measure and stop.
While there: `MAX_CIRCUIT_TEXT_BYTES` is not a capacity policy, it is zip-bomb hardening (#38,
`UntrustedFileHardeningTest`). Splitting a design into ≥1,440 files to get under a *decompression*
cap is fighting a safety control with an architecture. The correct fix is a ratio/streaming guard
that bounds expansion relative to input, decoupling "hostile archive" from "big circuit" — one
class, and wall (C) evaporates without a partitioned model existing. Add the flat representation
(#370) only if a measured design actually needs it; at 150 B/element a commodity 16 GB heap holds
~10^8 elements, which is past XiangShan and 5 orders past anything drawn here.

**B. Campaign = an artifact schema, not a runner. (~2–4 mw, and it is the piece with users.)**
JLS should not own a scheduler. `-j 200` dispatch is solved by `xargs -P`, a CI matrix, Gradescope,
and the multi-arch container this project already publishes (`ghcr.io/anadon/jls`). The genuinely
JLS-shaped part of FEAT-057 is the **stable machine-readable per-run result record** — a `-report
out.json` beside `-t`, versioned under `docs/batch-interface.md`'s existing stability contract, plus
a trivial merge. That serves CAP-06 (#300), CAP-09 (#306) and CAP-21 (#502) immediately, is one more
row in a document that is already normative, and leaves no long-lived subsystem for a
bus-factor-1 maintainer to keep green. AC-2 ("job scheduling is not observable") becomes free: there
is no scheduler to be observable. This is #508's split, made smaller than #508 made it.

**C. If reach past one host ever matters, hand off rather than distribute.** JLS already ships
`-export out.v` (structural Verilog-2005) and the HDL roadmap already sits on Yosys/Icarus/GHDL
subprocess boundaries (#61/#63, ratified in ARCHITECTURE.md's plugin-trust decision). A design too
big for JLS is a design for Verilator — free, compiled, orders faster, maintained by others. The
strategic move is to make that handoff excellent (SubCircuit/Memory/ShiftRegister export, which
`HdlPolicyTest` currently pins as *deliberately rejected*), not to build a Java interpreter that
races Verilator across a cluster. #312's Background reaches the same conclusion from the industry
side and then declines to draw it: "a distributed software simulator at that scale would be doing
something the incumbents do not." At 10^9 gates the incumbents use emulators because software
simulation is the wrong tool, not because nobody thought of it.

## What I am disregarding, and why

I am disregarding AC-1, AC-3, AC-4, AC-7, AC-8, K17-1/2/5, the eleven-row roster, the five ordering
prerequisites and the whole cost reconciliation. They are internally careful — the falsify-first
discipline in AC-3 (show the guard red before counting a pass) is genuinely good practice that
deserves to be lifted out of this issue and into the project's testing culture generally. But they
all price and validate a subsystem that should not be built, and refining them further consumes the
one resource this project is provably short of. Two of this issue's three comments are about the
issue's own bookkeeping; #508's process finding names the pattern precisely.

**What would change my mind:** a named user with a design above ~10^6 drawn elements that JLS is the
right tool for, or the #221 revisit trigger firing on a real `riscv/` design. Neither exists today.

## Disposition

Redirect. Close the capacity/distribution axis (or convert it to a recorded, dated **non-goal** in
ARCHITECTURE.md alongside i18n and the plugin loader, with "a drawn design exceeds 10^6 elements" as
the re-entry trigger — this project's best habit is writing down what it declined and why). Re-home
FEAT-005 (#353) as a standing-priority defect on its own, not as row one of a capstone. Re-home the
campaign axis to CAP-06/#300 and CAP-09/#306 as a result-artifact schema per (B). Keep #325's
fidelity work as the recorded direction if scale ever becomes real. Nothing of value is lost, and
the two things in #312 that have users ship in weeks instead of years.
