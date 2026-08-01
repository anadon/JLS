# FEAT-036 - Byte lanes on `Memory` and capacity as a byte budget

**Status:** proposed | **Cost:** 3-7 mw | **Owner program:** P2 |
**Spine rank:** S20

## Capability delivered

A drawn processor can perform a single-cycle read-modify-write on a sub-word
field, because the memory element accepts a write mask that says which byte
lanes of the presented word are actually written. Separately, memory capacity
stops being expressed as a word count with a hard fallback at a fixed word
threshold and becomes a byte budget with headroom stated against the guest
image the plan actually intends to run, and initialization stops doubling heap
by materializing a second copy of the initial contents.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | A drawn core needs single-cycle read-modify-write, and a guest image is a byte budget, not a word count |
| CAP-03 | required | The monitor's storage and the block image need a byte budget, not a word-count cliff |
| CAP-08 | required | An imported core's memory does sub-word writes, and its image is a byte budget |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-013 | A megabyte-scale initial image cannot ride as hex text under the decompressed-text cap. The raw bulk-image section is where memory contents go, and that section is section-framed |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0013 | Memory capacity as a byte budget, initialized copy-on-write | The budget itself and the end of the initialization heap doubling |
| TASK-0034 | The raw bulk-image section | Where a large initial image lives, with the size arithmetic written against the text cap |
| TASK-0076 | Write-mask input on memory | The byte-lane mask that makes single-cycle read-modify-write expressible |

## Acceptance criteria

1. A memory element accepts a write mask input whose bits select byte lanes,
   and a masked write leaves the unselected lanes byte-identical.
2. A circuit performing a sub-word store completes it in one clock, asserted by
   a simulation golden rather than by inspection.
3. Capacity is stated and checked in bytes. The threshold at which the dense
   store falls back to a sparse one is a named constant with a recorded reason,
   and the fallback is reported rather than silent.
4. Initializing a memory of size N does not transiently allocate a second copy
   of N; the improvement is asserted against a measured bound, not asserted in
   prose.
5. An initial image large enough to exceed the decompressed text cap is
   representable through the bulk section and round-trips byte-identically.
6. Every existing memory golden remains byte-identical.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Byte lanes, the byte budget and the copy-on-write initialization | **no issue** |
| 199 | Memory: optional synchronous (clock-edge) write mode for glitch-safe RAM in combinational datapaths | informs, **closed** - it added the `sync` attribute this feature's mask input sits beside, and it is one of the two recorded live instances of an attribute an older reader silently ignores |
| 20 | Memory efficiency: Memory element word storage (~100 bytes/word) and unbounded simulation histories | informs, **closed** - the original statement of the per-word footprint that makes a byte budget necessary |

## Design notes

The word ceiling is routinely mis-cited and the correct citation matters. The
`1 << 22` figure is `DENSE_CAPACITY_LIMIT`, the threshold at which the dense
store falls back to a sparse map - not a hard four-million-word cap. A separate
constant, `MAX_INIT_WORDS = 1L << 24`, bounds initialization. Any document that
says "the memory ceiling is `1 << 22`" without naming `DENSE_CAPACITY_LIMIT`
will be read as claiming a cap the code does not impose.

The three tasks are shared: TASK-0013 is also FEAT-006's capacity work and
TASK-0034 is also FEAT-013's bulk section. Their weeks are counted once, at the
task level. This feature's small band reflects that it is mostly the mask input
plus the arithmetic and the reasons; the storage work is paid for elsewhere.

## Risks

- **A mask input is a new port on a heavily used element.** Every existing
  circuit must load with the port absent and behave exactly as before, or the
  golden corpus moves.
- **"Byte budget" invites an unbounded promise.** The budget must be stated as
  a number with headroom over a named target image, not as "large".
- **Copy-on-write initialization is easy to claim and hard to witness.** The
  acceptance needs a package-visible witness that two memories share backing,
  because heap-delta assertions are not deterministic under coverage and JIT.

## Evidence

- The two constants, correctly named: `src/jls/elem/Memory.java:1224`
  (`DENSE_CAPACITY_LIMIT = 1 << 22`, the dense-store fallback threshold, used at
  `:1234`) and `src/jls/elem/Memory.java:94` (`MAX_INIT_WORDS = 1L << 24`, used
  at `:424`, `:458`, `:491`, `:606`).
- The decompressed-text cap the bulk section exists to get around:
  `src/jls/FileAbstractor.java:65`.
- Spine placement S20: `10-capstone-plan.md` §2.1.
- Owner: P2 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 3-7 mw; TASK-0013, TASK-0034 and TASK-0076
  total 4.5 wk, of which TASK-0013 and TASK-0034 are shared with FEAT-006 and
  FEAT-013 respectively and counted once at the task level. The band and the
  unshared remainder agree.
