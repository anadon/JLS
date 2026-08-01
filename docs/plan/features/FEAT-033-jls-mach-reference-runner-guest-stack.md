# FEAT-033 - `jls.mach`, the reference runner and the guest software stack

**Status:** proposed | **Cost:** 14-22 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

The architectural model of the machine lives in a pure leaf package that
depends on nothing in the editor or the simulator, born under the strongest
coverage bar in the tree, together with a reference runner that executes it and
the pinned guest software - kernel, device tree, initramfs - that the runner
must run. This is the independent counterparty that makes a parity claim mean
something: without it, the same author writes both the drawn machine and the
thing it is compared against, and agreement proves only that one person was
consistent.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | Supplies the parity counterparty and the pinned kernel, device tree and initramfs |
| CAP-03 | required | The independent emulator that is the counterparty; without it the same person authors both sides with no method |
| CAP-09 | beneficial | The reference an unfamiliar design is checked against |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-013 | A pinned guest image has to live somewhere. Either it rides in an optional binary section or it is a sidecar with a recorded digest; both answers are section-framing decisions, and the must-understand policy is what stops an older reader from silently losing it |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0070 | The machine package and its reference runner | The leaf package and the independent implementation that is the parity counterparty |
| TASK-0071 | Guest image build, pinning and residence | The image itself, built reproducibly, with the clock and calibration pinned and a decided answer for where it lives |

## Acceptance criteria

1. The package depends on no editor and no simulator type, asserted by an
   architecture rule rather than by convention.
2. The package is created with a coverage floor at the strongest bar the tree
   uses, not added to the ratchet later.
3. The reference runner executes a program and reports architectural state at
   instruction granularity, headless.
4. The guest image is built by a recorded, re-runnable procedure whose output
   is byte-identical across two runs, with every input pinned by digest.
5. Where the image lives is decided and tested - in a file section or as a
   sidecar with a recorded digest - and a load with the image absent reports it
   by name rather than failing obscurely.
6. The runner and the drawn machine share no code. A change to one cannot
   mechanically change the other, which is the property that makes the parity
   comparison informative.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - #202's oracle needs a counterparty, which is this feature; the worked example itself is FEAT-038 |
| - | The machine package, the reference runner and the guest software stack | **no issue** |

## Design notes

The layer stack this feature sits in simultaneously specifies a lane that boots
a guest and diffs a console stream, and excludes committed guest images. One of
those two has to be withdrawn, and TASK-0071 withdraws the exclusion and records
why. That is a maintainer decision the plan cannot make on its own, and it
should be ratified before the task starts rather than discovered inside it.

The reason this is one feature and not three is stated in the registry's
deduplication record: the model without a runner is untestable and the runner
without an image runs nothing. Splitting them produces two things that cannot
be demonstrated.

The independence in criterion 6 is the entire value. It is also the thing most
easily lost by convenience: the first time the runner imports a helper from the
drawn side to avoid duplicating a decode table, the parity claim quietly
becomes a self-comparison.

## Risks

- **Independence decays.** Nothing mechanical prevents the runner and the drawn
  machine from converging on shared helpers over time; only the architecture
  rule in criterion 1 and review discipline do.
- **A committed guest image is a large tracked binary.** It interacts directly
  with the fixture-size policy and with clone times, and the decision in
  criterion 5 is where that cost lands.
- **Bus factor 1 authorship of both sides.** Even with no shared code, one
  author's misreading of the specification lands identically on both sides. The
  mitigation is an external conformance corpus, not more tests.

## Evidence

- The parity discipline this feature serves: `docs/parity-contract.md` (whose
  status line records that it binds nothing until a decision block is recorded)
  and `docs/virtual-hardware-parity.md`.
- The calibration record the runner's cost model divides by:
  `docs/machine-calibration.md`.
- Issue #202, open, verified against `list_issues(state=OPEN)`.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 14-22 mw; TASK-0070 and TASK-0071 total 4 wk.
  The two tasks are the package seam and the image pipeline; the band prices an
  instruction-complete reference implementation and a bootable guest stack
  behind them. No task id names that residual. Do not read 4 wk as the feature.
