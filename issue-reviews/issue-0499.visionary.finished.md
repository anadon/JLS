# Issue #499: Virtual-hardware / virtual-logic parity, part 1 of 3: what parity means, the two tiers and their honest wall-clock bands, the prior art, and layers L0-L4
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of its content, #499 is an **archival act**, not a work item. It has no
deliverable, no acceptance criteria, and nothing for a contributor to do. Its claim is:
*the programme-level synthesis of the virtual-hardware study must survive the deletion of
`claude/jls-virtual-hardware-linux-njsoma`, and the way to make it survive is to paste
124 KB of it into three GitHub issue bodies.*

The first half of that claim is right. The second half is where the issue goes wrong, and
it goes wrong by never considering the obvious move.

## The alternative the issue never considers: commit the file

The issue's own reasoning is `link to a branch → branch dies → link preserves nothing →
therefore inline`. That syllogism has a missing branch. The options are not {link, inline}.
They are {link, **commit**, inline}, and the middle one is a two-minute operation:

```
git checkout claude/jls-virtual-hardware-linux-njsoma -- docs/virtual-hardware-parity.md
```

on a PR into `master`. The repository is the archive. Everything the rescue works hard to
approximate — durability, citability, line anchors — is what a tracked file gives for free.

There is no doctrinal obstacle. This document's own siblings are already in the tree.
`docs/capability-roadmap/` carries `keystone-c-performance.md` (the measured engine
constants this document divides by), `lf-02-compiled-evaluation.md` (which *is* L1 and L9,
already written, already in-tree, already anchored to `ARCHITECTURE.md:341-368`), six
capability sweeps, and `AMENDMENT.md` with programmes **P7–P13** — whose numbering the
rescued document's **P14–P21** silently continues. JLS already hosts a whole genre of
dated, explicitly non-normative studies on `master`: `hdl-support-research.md`,
`collaborative-editing-research.md`, `library-survey-2026-07.md`,
`flatlaf-evaluation-2026-07.md`, `mutation-testing-trial-2026-07.md`,
`ISSUE-AMBIGUITIES-2026-07.md` at the repo root. The correct resting place for this text is
`docs/capability-roadmap/lf-09-virtual-hardware-parity.md`, status banner intact, next to
the P7–P13 corpus it extends and the lf-02 document it duplicates.

Meanwhile the chosen medium is worse in ways the project has already documented:

1. **The tracker is the one medium this corpus has recorded as corrupting.** #493 warns:
   "Reading a body with the tracker's read tool corrupts tag-shaped runs, and a full-body
   update writes that corruption back to storage — see #489." This document is unusually
   dense with tag-shaped runs: `1 << 22`, `now <= maxTime`, `limit <= 0`,
   `Map<Class<?>, String>`. Fetching #499 through the API today returns `1 &lt;&lt; 22`
   and `now &lt;= maxTime`. The header's promise — "inline and verbatim", "every line of
   the original document appears across parts 1–3" — is already false at the character
   level, in exactly the spans a reader would copy into code.
2. **The three-way split is a property of the medium, not the content.** So is the
   hand-maintained `*(original file lines N–M)*` shim, an unverifiable substitute for what
   `git grep -n` would give back automatically to the 77 citing issues.
3. **It exits the project's own provenance story.** The README sells byte-reproducible
   jars, `.buildinfo`, and signed attestations. An issue body is in no tarball, no source
   jar, no offline clone, has no `blame`, no diff, and no signature.

**Concrete redirect:** one PR adding all three rescued documents to
`docs/capability-roadmap/`; close #494–#499 as superseded, each pointing at its committed
path. That collapses six inlining issues into one commit and makes the "195 unrecoverable
branch-only planning docs" line in #493 shrink by three.

## Disregarding the stated framing: what in here is actually worth keeping

The rescue treats the 124 KB as indivisible because "77 issues cite it." I am disregarding
that criterion. Citation count measures how much scaffolding was built on the document, not
how much of it is load-bearing. By the document's *own* argument, almost none of it is: every
wall-clock figure is a quotient with an unmeasured constant in the denominator, and §1.3
disclosure (3) admits every interactive figure is "an upper bound of unknown tightness."
A programme whose numbers all divide by four things nobody measured is not a plan; it is a
list of experiments with a narrative attached. Three things survive that filter:

**1. L0, minus the narrative.** Nine cheap experiments, most of which pay off for the
*existing* roadmap regardless of Linux. Two are outright defect reports about `master`,
both of which I verified at HEAD and neither of which needs this document to be actionable:

- `Simulator.runEventLoop` polls the head event and calls `dupCheck.remove(event)` *before*
  testing `now > maxTime`, then breaks — the event is evicted from dedup and discarded with
  no record it existed (`src/jls/sim/Simulator.java`, the poll/limit-test sequence in
  `runEventLoop`). Harmless today, silent corruption under any future checkpoint.
- `BatchSimulator.pause(boolean)` sets `stopping = true` — byte-identical to `stop()`, with
  `@param which Ignored.` in its javadoc.

L0(h) — cross-platform simulation determinism — is the highest-value item in the entire
document and has nothing whatever to do with Linux. Nothing in the tree asserts that a
simulation is bit-identical across a JDK or OS change, yet `docs/batch-interface.md` is sold
as a stability contract to autograders and CI. Diffing one circuit's VCD across the three CI
platforms that already exist as jobs is one day of work and protects a promise the project
is already making to third parties. File it standalone.

**2. The fidelity boundary (L4), decoupled from the boot.** This is the one genuinely
excellent idea in the corpus, and §2.1 states its real justification better than the Linux
framing ever does: **JLS already ships unchecked behavioral abstraction.** `Adder`, `Memory`,
`TruthTable`, `StateMachine`, `ShiftRegister`, `RegisterFile` and `FieldExtend` each compute
an arbitrary function in one `react()` with no mechanism asserting they agree with any
structural referent. That is a present-tense gap in an *educational* tool, at student scale,
today.

Reframed as a pedagogical capability rather than as layer four of a nine-layer bring-up
programme, it becomes something the project's arc actually wants: *a student draws a
ripple-carry adder; JLS proves it observationally equal to the built-in `Adder` and says so.*
That is a lab assignment, an autograder check, and a natural extension of the `-t` grading
surface the README already advertises. The document concedes this itself in L4's point 4 —
"provable at student scale on day one … with zero RISC-V and zero Linux" — and then buries
it under L5–L9. Sequencing inverted: ship the equivalence harness and
`docs/abstraction-levels.md` (the retroactive articulation applied to the seven elements
above) as a standalone capability. It needs no `Console`, no `-d 0`, no memory-image file
section, no FORMAT 3, and it does not reopen #221.

**3. The prior-art survey and its rule 4**, which the programme then violates. Rule 4 says:
*use an oracle that does not share authorship with the design.* The programme's answer is a
hand-written behavioral RV32 core, written by the same maintainer as the drawn one, at bus
factor 1.

## The out-of-box route the document refuses to see

The document defines itself against QEMU ("anyone who wants a native-feel shell wants QEMU
and should use QEMU") and never considers the option sitting in its own speed ladder:
**Verilator, at 1.2 M cycles/s — roughly 150× the structural tier's best projected figure.**

JLS already has an HDL exporter (`-export out.v`), `docs/hdl-support-research.md`, owned
roadmap issues #33/#59/#492, and iverilog/GHDL/Yosys wired into the dev container and CI.
The elegant seam is therefore: **JLS draws the SoC; Verilator runs it.** Export the drawn
machine, boot Linux under Verilator in minutes rather than hours, and use that
independently-authored toolchain as the parity oracle — satisfying rule 4 by construction
instead of by promissory note. Under that framing, L1's constant-factor stack, L2's capacity
work, L9's levelized pass and most of L4's harness all become unnecessary *for the boot goal*,
and what remains is a single question the project already wants answered: can `HdlExporter`
emit `SubCircuit`, `Memory` and `ShiftRegister`? That gap is real (the roadmap README records
the omission) — but closing it is already an owned programme with beneficiaries far beyond
this study, whereas L1–L9 spawn **eight unowned programmes** and 30–45 maintainer-weeks with,
by the document's own admission, "no work breakdown supporting that figure … anywhere."

## Where this pulls against the project's arc

The tell is in L0's own file list: `riscv/build/k2000.jls`, the performance anchor for every
number in `keystone-c-performance.md`, is **untracked** (`riscv/.gitignore` line 1 is
`build/`). The trajectory's own evidence is unreproducible from a clone. A programme that
proposes nine layers, a format bump, a new normative determinism invariant, the first host
I/O door in `src/`, and the reopening of a recorded decision (#221) — built on a benchmark
that is not in git — has its priorities exactly inverted. Commit the fixture, run L0(a),
L0(b), L0(f) and L0(h), and let the measured α and `k` decide whether L2–L9 ever deserve to
be filed. That is one to two weeks, it is honest, and it is the only part of this document
that is not a quotient.

## Summary

The instinct to preserve is right; the mechanism is wrong and the resting place is wrong.
Commit the three documents to `docs/capability-roadmap/` and close #494–#499 as superseded.
Then extract the three things that stand on their own — the L0 measurements (with the two
verified defects and cross-platform determinism filed separately), the fidelity boundary as
a student-scale equivalence-checking feature, and the HDL-export-to-Verilator reframing of
the boot goal — and let the Linux programme itself stay what its status line says it is: a
proposal nobody has costed.
