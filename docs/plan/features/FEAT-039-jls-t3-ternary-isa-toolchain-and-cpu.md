# FEAT-039 - JLS-T3: the ternary ISA, toolchain and drawn CPU

**Status:** proposed | **Cost:** 18-30 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A balanced-ternary instruction set exists as a written specification with a
conformance corpus, an independent reference emulator executes it, an assembler
and disassembler ship inside the jar so no external toolchain is required, and
a processor drawn from the N-ary element family runs programs written for it.
It is the demonstration that the radix work is a real capability rather than a
representation change: a machine that is not binary, drawn, simulated, verified
and programmed with the tool's own tools.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-03 | required | The ISA, its reference emulator, the in-jar assembler, and the drawn CPU - this is the capstone artifact |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-029 | The drawn machine is built from the N-ary element family; without those element types there is nothing to draw it out of |
| FEAT-033 | The reference emulator is an inhabitant of the machine package and uses its runner seam; the independence discipline is the same one |
| FEAT-034 | Agreement between the drawn machine and the emulator is per retired instruction, through the same harness |
| FEAT-032 | The monitor's output is a console byte stream; without the host byte port the machine has no observable behavior |
| FEAT-015 | The machine is constructed programmatically rather than dragged, which is what makes it regenerable |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0081 | Specify the ternary ISA and its conformance corpus | Everything else is written against the specification; the three-way branch and the division rule are the two places an unwritten specification produces two different machines |
| TASK-0082 | The ternary reference emulator and assembler | The independent counterparty and the toolchain that makes programs writable |
| TASK-0083 | Draw the ternary CPU | The drawn machine, against a stated element census |
| TASK-0084 | The monitor program | The hand-written program that runs on the drawn machine and prints |
| TASK-0038 | Programmatic circuit construction verbs | Shared with FEAT-015 and FEAT-038: the supported way to build a machine from a program |

## Acceptance criteria

1. The instruction set is a written document covering encoding, the three-way
   branch, the exact division and rounding rule, and the behavior of every
   undefined value case.
2. A conformance corpus exercises each specified case, and the corpus is
   executable against any implementation of the specification.
3. The reference emulator passes the conformance corpus and shares no code with
   the drawn machine.
4. The assembler and disassembler ship in the jar, round-trip every corpus
   program, and require no external tool.
5. The drawn machine agrees with the emulator per retired instruction on the
   declared architectural fields.
6. The monitor runs on the drawn machine and prints to the console, and its
   transcript is byte-compared across runs.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The ternary ISA, its toolchain and the drawn machine | **no issue** |
| - | The whole N-ary program, of which this is the application | **no issue** |

## Design notes

The one decision that changes this feature's shape is whether the drawn machine
is built at native radix 3 or at binary-encoded ternary. Binary-encoded ternary
makes a sixteen-trit bus a thirty-two-bit bus and lets the machine be drawn
before the radix work completes; native radix makes the machine a demonstration
of the radix system rather than of an encoding. The tasks assume the former as
a fallback and name the cost difference; the choice is the maintainer's and it
should be made before TASK-0083 starts, not inside it.

The specification in criterion 1 is not documentation of an implementation. It
precedes both implementations and is what makes their agreement meaningful; if
it is written after the emulator, the emulator is the specification and the
comparison is a self-comparison.

## Risks

- **Two implementations by one author.** The same misreading lands on both
  sides. The conformance corpus is the only mitigation and it must be written
  from the specification, not from either implementation's behavior.
- **The named tasks total eight weeks against an 18-30 mw band.** Even the
  corpus's own pricing of the ISA and emulator exceeds the task costs. The
  residual is real work with no id.
- **Balanced ternary division has more than one defensible rule.** Choosing
  late means every program written before the choice is wrong afterwards.

## Evidence

- The radix determination that scopes the native-versus-encoded choice and the
  stage structure this feature consumes: `07-mvl-determination.md`.
- The element family it is drawn from: FEAT-029, whose tasks TASK-0060 through
  TASK-0062 create the operators, the element types and the interop.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 18-30 mw; the five named tasks total 10 wk, one
  of which (TASK-0038) is shared with FEAT-015 and FEAT-038 and counted once.
  The residual is the depth of the emulator, the assembler and the drawn machine
  beyond their leading two-week slices; no task id names it. Do not read 10 wk
  as the feature.
