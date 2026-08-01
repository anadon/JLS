# FEAT-038 - The drawn structural RV32 machine

**Status:** proposed | **Cost:** 12-26 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A general-purpose processor exists as a circuit drawn in the editor - datapath,
control, memory interface and console - that a person can open, read and step
through, and that was brought up one boundary at a time against an independent
reference rather than debugged as a whole. It is the artifact that makes the
boot capstone a demonstration rather than an assertion, and it is the largest
worked example the project would ship.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | This is the capstone artifact - a machine a person can open and read |
| CAP-08 | beneficial | A drawn machine of known shape is what an imported third-party core is compared against for size, speed and correctness |
| CAP-09 | beneficial | A design of known-correct provenance to calibrate the verification suite against before it is pointed at an unfamiliar one |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-015 | The machine is too large to place by hand reproducibly; it is constructed through the programmatic op verbs, which is also what makes it regenerable |
| FEAT-033 | The reference the bring-up is checked against, boundary by boundary. Without it the bring-up has no oracle |
| FEAT-034 | The comparison itself: per-retired-instruction agreement is how a boundary is declared green |
| FEAT-031 | Bring-up proceeds by binding one boundary structural while the rest stays behavioral; without a per-instance fidelity toggle the machine only ever runs whole |
| FEAT-036 | The core's memory interface performs sub-word writes and holds an image sized in bytes |
| FEAT-032 | The machine's observable output is a console byte stream; without the host byte port there is nothing to compare |
| FEAT-007 | The end-to-end run does not fit in a required fast lane and needs the long-run lane |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0038 | Programmatic circuit construction verbs | The machine is built by a program, not by dragging; this is the supported way to do that |
| TASK-0079 | Draw the machine and bring it up boundary by boundary | The drawing itself and the bring-up method, against a stated element census |
| TASK-0080 | The headless boot run and its transcript comparison | The end-to-end run in the long-run lane with a byte-compared transcript |

## Acceptance criteria

1. The machine loads as an ordinary `.jls` file and is readable in the editor
   at a stated zoom without a legend.
2. Each declared boundary - fetch, decode, register file, load/store, control
   and status, timer - is brought up separately, and the record of which
   boundaries are green is data in the tree, not prose.
3. Running the machine against the reference agrees per retired instruction on
   the declared architectural fields, with the exclusion set enumerated.
4. The construction is regenerable: re-running the construction program
   produces the byte-identical file.
5. The end-to-end run executes in the long-run lane within a stated wall-clock
   budget and produces a byte-compared transcript.
6. A deliberate falsification - perturbing the clock period, or a single
   decode arm - turns the comparison red. A comparison that cannot be made to
   fail is not evidence.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes - this feature is the sample circuit and the integration golden halves; the oracle producer belongs to FEAT-034 |
| - | The bring-up method and the boundary record | **no issue** |

## Design notes

The band's width - 12 to 26 mw, the widest ratio in the plan - is honest and
should not be narrowed by wishing. It reflects an element census whose
constant is unmeasured until FEAT-009 lands, and a bring-up cost that depends
on how many boundaries turn out to be wrong the first time.

The named tasks total four weeks and the band is three to six times that. The
residual is the per-boundary drawing and debugging of decode, load/store,
control-and-status and the timer, which no task id in the closed space names.
A reader must not take "two weeks" for "the machine".

Whether the machine is committed as a literal file or produced by an in-tree
construction program is not a style question: it decides whether the artifact
is reviewable, whether it can be regenerated after a format epoch, and how it
interacts with the fixture-size policy. Criterion 4 chooses regenerable.

## Risks

- **A drawn machine is a fixture that every format change must migrate.** It is
  the single largest hostage the plan creates.
- **Bring-up cost is unbounded without the boundary discipline.** Debugging a
  whole CPU at once against a transcript diff is the failure mode that makes
  this feature 26 mw rather than 12.
- **Readability and size are in tension.** A machine drawn small enough to read
  may be too abstract to run; the census is the mediator and it is unmeasured.

## Evidence

- The performance and element-count anchor this feature's census divides by:
  `docs/machine-calibration.md`; the fixture that anchors it,
  `test/fixtures/riscv-sum1to10.jls`, run by `test/jls/RiscvCpuGoldenTest.java`.
- The parity discipline it is brought up against: `docs/parity-contract.md`
  (recorded as non-normative at HEAD) and `docs/virtual-hardware-parity.md`.
- Issue #202, open, verified against `list_issues(state=OPEN)`.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 12-26 mw; TASK-0038, TASK-0079 and TASK-0080
  total 6 wk, and TASK-0038 is shared with FEAT-015 and FEAT-039. The residual
  is the per-boundary drawing and bring-up of the remaining boundaries, which
  has no task id. Do not read 6 wk as the feature.
