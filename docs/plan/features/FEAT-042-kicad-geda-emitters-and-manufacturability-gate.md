# FEAT-042 - KiCad and gEDA netlist emitters with a manufacturability gate

**Status:** proposed | **Cost:** 5-10 mw | **Owner program:** P3 |
**Spine rank:** -

## Capability delivered

A drawn circuit leaves JLS as something a real PCB tool opens without hand
editing: a schematic in the open gEDA format that KiCad imports, and a netlist
whose component and node records name physical packages and pins. Alongside the
emitters, a gate answers the question the emitters raise - whether the design
as drawn can actually be fabricated - with named rules and a machine-readable
report, so "it exported" and "it can be built" stop being the same claim.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | beneficial | Shares the cascade rule and the footprint column; the breadboard capstone consumes the same package layer |
| CAP-05 | required | The netlist itself, plus the check that says whether the board can be built |
| CAP-13 | required | The emitters are this capstone's spine; the gate is their acceptance criterion |
| CAP-18 | required | The signal-integrity constraint file rides alongside this netlist and names the same nets; a constraint on a net the board tool never heard of is inert by construction. Added 2026-08-03 under D16: the filed issue #366 declares `serves_capstones: [298, 307, 313]` and #313 carries 366 in `requires_features`; this table's omission was a transcription defect |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-004 | A netlist node is a net. Emitting one from the exporter's private union-find would make a fourth copy of the partition and would name nets by save order |
| FEAT-040 | A component record names a package and a footprint. Neither exists until the part library exists as data |
| FEAT-041 | A netlist node names a package pin, which a word-wide drawn element does not have until packing and width decomposition have assigned one |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0085 | The package data schema and footprint binding | The mechanism binding a logic element to a package, footprint name and part value; every component record is written from it |
| TASK-0089 | The PCB-tool netlist emitter | The netlist the target tool accepts without hand editing, with a golden |
| TASK-0090 | The open-schematic emitter | The schematic path, with derived symbol geometry, reference designators and sequential pin numbers |
| TASK-0091 | The manufacturability gate | The check that says whether the design can be fabricated, with named rules |

## Acceptance criteria

1. An emitted schematic opens in the target tool with symbols visible and
   connectivity intact, verified by opening it, not by parsing it.
2. An emitted netlist imports into the PCB editor and every component and node
   resolves, with no hand editing between export and import.
3. Emission is deterministic: the same circuit emits byte-identical output
   across runs and platforms, pinned by goldens.
4. Every element type has a declared physical disposition, including an
   explicit "cannot be placed" with a reason; the disposition table is total
   over the element registry.
5. The manufacturability gate reports per named rule, with the offending
   elements identified, as machine-readable output.
6. A design that fails the gate does not silently emit a netlist that a fab
   house would reject; the failure is reported before the artifact is written.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The KiCad and gEDA emitters and the manufacturability gate | **no issue** |
| - | The whole physical program, of which this is the output stage | **no issue** |

## Design notes

There are two routes to KiCad and the evidence corpus disagrees about which one
this feature is. One evidence document prices a KiCad `.net` netlist print
directly; another declares that path strictly dominated and routes KiCad
through a gEDA schematic that KiCad's importer reads. This feature's title spans
both and its tasks contain both. The decision belongs to CAP-13, which records
it as an open decision with a recommendation - schematic first, netlist second,
over the same partition - and it must be made before either task is funded,
because the two orders buy different things first.

The cheap schematic route rests on an unfalsified premise: that KiCad's gEDA
importer installs embedded symbols. That claim was read in KiCad's source and
has never been run. A hand-written ten-line schematic opened once in KiCad
settles it in an afternoon and is the single highest-value hour in this feature.

Positional pin order per element type is the only silent-when-wrong item in this
area: a mis-ordered component record parses, imports, and yields the wrong
board. It has no separate id and it belongs here.

## Risks

- **Acceptance by a real tool cannot be asserted from inside JLS.** Criterion 1
  and criterion 2 need a human or an external tool in the loop, which makes
  them the weakest checks in the plan unless the tool is armed in CI.
- **The dominated route may be funded first** if the open decision is left
  unmade, spending weeks on the path the evidence rates lower.
- **Footprint data is licensed data.** Every part record needs attribution and a
  license notice, and getting that wrong is not a technical failure mode.

## Evidence

- No physical vocabulary at HEAD: a search of `src/` for footprint, refdes or
  pinout returns nothing.
- The existing determinism precedent for an emitted artifact:
  `src/jls/hdl/board/PcfEmitter.java` and its byte-deterministic golden under
  `test/resources/hdl/board/`.
- The shared partition this feature emits over: FEAT-004, whose TASK-0007
  extracts the walk that today exists in three separate copies.
- Owner: P3 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 5-10 mw; TASK-0085, TASK-0089, TASK-0090 and
  TASK-0091 total 7 wk, of which TASK-0085 is shared with FEAT-040 and counted
  once at the task level. Band and task sum agree within the band.
