# Issue #325: FEAT-031: one subcircuit instance runs as drawn logic or as a fast implementation of the same definition, and a harness proves the two agree
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four tasks away and two different things are inside this issue.

The first is a **falsifiable equivalence boundary**: a `Boundary` type whose signature
admits only ports and values, an observation function `Ω_B(M)[n]` indexed rather than
timestamped, a coverage rule, a null-toggle gate, a committed knowingly-wrong binding the
harness must reject, and a refusal list instead of silent degradation. This is the best
idea in the issue and possibly the best idea in this tier of the plan. JLS today has
*no* behavioural equivalence check across any boundary at all —
`docs/capability-roadmap/sweep-02-timing.md:126` records that
`test/jls/hdl/IverilogCompileTest.java:32` only compiles the exported Verilog and never
runs `vvp`, so "no behavioural equivalence between JLS and any external simulator is
checked anywhere in the tree." `ARCHITECTURE.md:359-368` already gestures at the concept
— an "Equivalence criterion (binding on any future pass)" naming
`docs/simulation-semantics.md` §2/§6/§7/§8/§9 and the #202 differential oracle — but it
is prose, with no type, no harness and no way to fail. `Ω_B` is the missing
formalization of a clause the project has already written down and cannot currently
enforce.

The second is a **per-instance saved fidelity attribute selecting a hand-written Java
implementation of a drawn block**. This is the part I want to push back on, because the
project's own long-form planning documents specify a different and stronger route to the
same end, and this issue never argues against it.

## The fork in the road the issue does not name

There are two ways to make a drawn block fast, and they differ in kind:

- **(A) Derive the fast arm from the drawn logic.** Elaborate-to-flat, levelize, run a
  statically-ordered pass. `docs/capability-roadmap/lf-02-compiled-evaluation.md` is 756
  lines specifying exactly this, with measurements behind it
  (keystone C: 4.32 ns/node with plane arrays vs 22.01 with `BitSet[]`; 82.3% of events
  are zero-delay `PinChanged`; `SubCircuit.react` at `src/jls/elem/SubCircuit.java:620-635`
  pays a `HashMap` lookup, a `BitSet` clone and a queue round-trip **per input pin to
  model zero elapsed time**). Equivalence is by construction; a differential harness is a
  regression net, not the load-bearing proof. Every block a student draws gets faster.
- **(B) Substitute a separately-written implementation, then prove agreement.** What
  #325 builds. Equivalence is by test. Only maintainer-blessed blocks get faster, because
  the implementation set is explicitly "closed, core-internal, sealed."

#325's §1 out-of-scope disposes of (A) in one line ("the compiled binding's engine")
while its own demonstration is stated as "an ALU subcircuit, **drawn versus compiled**",
and §2's four rejected alternatives are all cuts of (B) plus one element-shaped framing.
The strongest argument against the issue as written is its own legitimacy argument: it
cites `Adder`, `Memory` and `RegisterFile` as lumped-behaviour precedents. Those are
precedents for (B) — and they are *already the mechanism*. `RegisterFile`
(`src/jls/elem/RegisterFile.java:20-28`) "collapses the ~95 elements … into a single
first-class element." A sealed core-internal binding set keyed to definitions is a way of
adding more `RegisterFile`s without adding palette entries. That is a real convenience,
but it is not "a large design bringable-up one block at a time" — a student's own drawn
block can never have a binding, by construction of the sealed set. The stated audience
benefit does not follow from the stated mechanism.

## Reframings

**R1 — Ship the boundary, not the toggle, and give it clients that already exist.**
TASK-0066 is separable and should go first *as its own feature*, with its first three
consumers being oracles the tree already has: drawn circuit vs its Verilog export under
`iverilog` (closing the `IverilogCompileTest` gap above), drawn RV32 vs
`riscv/riscv_ref.py` (#202/#278), and later structural engine vs compiled engine. Each is
a real disagreement source, so the null test is exercised against genuine divergence
rather than a synthetic wrong binding written to be caught. This delivers value with
zero format risk, no sealed registry, and no banner protocol — and it de-risks everything
downstream, because #295 and #301 rest on the harness, not on the attribute.

**R2 — Make the fast arm derived by default; hand-written arms are the exception that
the (already-proven) harness admits.** Per-subcircuit elaborate-to-flat is lf-02's Mode T
at subcircuit granularity. It produces the same user-visible outcome ("this boundary runs
fast"), works for every drawn block, and makes `Ω_B` equality a property you expect
rather than a property you assert. If CAP-02's Linux boot still needs a hand-written
behavioural SoC, it arrives afterwards as a named special case whose extraordinary claim
is carried by a harness that has already caught real bugs.

**R3 — Take the selection out of the file.** I am explicitly disregarding acceptance
criterion 1's "saved with the file", criterion 7's instructor-restriction surface and
integration criterion I6. Which implementation runs is a property of a *run*, not of a
circuit. Saving it makes a `.jls` file's meaning depend on a core-internal sealed enum
whose membership varies by JLS version — precisely the hazard `Circuit.readFormatHeader`
and the #79 format-negotiation discipline exist to prevent — and Open Question 2 (which
epoch?) is that hazard surfacing, treated as a scheduling problem instead of a design
smell. Worse, I6 exists only to defend against a hazard the design creates: if a
student's file can carry a binding, a student can hand in a lab that "works"
behaviourally, so the instructor needs a lockout. Put the selection in the run
configuration the batch interface already owns (`docs/batch-interface.md` is a normative
stability contract, and the autograder owns the command line), plus an editor-session
preference for GUI persistence, and the cheat vector, the lockout surface, the epoch
question and global invariants 1 and 2 all evaporate together.

**R4 — Ratify into `ARCHITECTURE.md`'s #221 decision block, not into a second normative
document.** Open Question 1 asks whether to ratify `docs/parity-contract.md`. Better: the
#221 block already owns "Equivalence criterion (binding on any future pass)" and its
revisit trigger; amend it with `Ω_B`, the coverage rule and the falsification gate. One
decision block that a contributor already reads beats a parallel contract that must be
kept in sync with it. (`docs/capability-roadmap/README.md:1005-1018` independently asks
for exactly this amendment, including quantifying #221's untestable trigger.)

**R5 — Generalize the banner into the outcome-line contract.** "This result did not come
from drawn structure" is not specific to behavioural bindings; it is equally true of a
compiled engine, an HDL round-trip and a reference-emulator comparison. Make it one row
in `docs/batch-interface.md` with one test, not a feature-local protocol.

**R6 — Derive the refusal set instead of enumerating it.** The five named refusals
(`DelayGate` as a delay line, order-dependent `TriState`, level-sensitive `Memory` write,
incommensurable `Clock`s, a block that does not settle) are five instances of one
predicate. The indexed-not-timestamped observable only quotients delay *inside* the
boundary if nothing downstream is sensitive to when the boundary's outputs arrive — which
holds exactly when the block is synchronous with respect to the declared sync net and
settles before the next sampling instant. State that as an admissibility precondition and
the refusal list becomes a theorem, not a list somebody must remember to extend. It also
gives the feature its true name: this is **synchronous block abstraction**, not
"fidelity", and the honest name would have made R2 and R3 obvious.

## Grounding note

I could not verify this issue's normative substrate in the checkout at
`/home/user/JLS`. `docs/parity-contract.md` — named in §3 as the source for the
observation function, permitted-to-differ set, harness shape, null test and refusal list,
"referenced, never restated", with line anchors — **does not exist in this tree**; the
only files mentioning the name are sibling reviews under `issue-reviews/`. Neither
`docs/plan/features/` (the corpus the LINK PASS comment derives every ordering edge from)
nor `machines/` exists here either. This may be checkout vintage rather than absence —
the evidence commit is `2d0ca9d` and I did not run git — but as things stand the feature's
entire §3 contract points at a document a reviewer cannot read, which is itself an
argument for R4: fold the contract into `ARCHITECTURE.md`, where it cannot go missing.

One substantive slip visible without that document: the coverage rule's corner set
"widths 1/31/32/33/63/64/65" varies a property of the *boundary*, not of a stimulus
vector — a fixed boundary's port widths are fixed. §3 reads as though written for a
general-purpose harness rather than for this feature's toggle, which is further evidence
the harness wants to be its own thing (R1).

## Where this lands

The boundary discipline strengthens the project's arc and should be built. The
per-instance saved fidelity attribute duplicates `RegisterFile`'s answer in a hidden
registry, pulls against the derivation route the in-tree roadmap specifies with
measurements, and puts a version-fragile implementation selector inside a file format the
project has worked hard to keep negotiable. Ship TASK-0066 first and separately, derive
the fast arm, and keep the file out of it.
