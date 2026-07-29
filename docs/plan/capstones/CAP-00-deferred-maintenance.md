# CAP-00 - Deferred maintenance: a decade of it

**Status:** proposed | **Priority:** 1 | **Marginal cost:** 24-42 mw |
**Standalone cost:** 24-42 mw

## Outcome

The known-defect backlog at HEAD is closed under standing ratchets: silent data
loss becomes a diagnostic, the quadratic and whole-run-in-memory paths become
linear and streaming, every CI job has a timeout and every supported platform is
a required check, the unfloored editor package carries a coverage floor, and a
user installs JLS without bringing a JDK - so that none of these is a tax the
other sixteen capstones pay again.

## Acceptance test

**SEEN.** A maintainer clones the repository fresh on Linux, macOS and Windows,
runs `mvn clean verify`, and all three go green inside the required-lane budget.
The 16 open issues this capstone closes (listed below) are closed. A first-run
user double-clicks an installer, gets JLS with no JDK of their own, opens a
`.jls` by double-clicking it, and drives the editor to a working adder without
touching the mouse.

**CHECK - `DeferredMaintenanceRatchetTest`**, one named suite whose arms each
fail today for a verified reason:

1. *Registry totality.* For every table in TASK-0001's committed inventory, the
   key set equals `ElementRegistry.all()` exactly (35 types at HEAD,
   `src/jls/elem/ElementRegistry.java:38-77`) and no arm falls through. Adding a
   36th type fails the build until every table is updated.
2. *Silent-data-loss corpus.* Loading a fixture carrying an attribute no element
   declares raises a diagnostic naming file, line, element and attribute.
   Asserts the diagnostic **text**, not a boolean.
3. *CI totality lint.* Every job in every file under `.github/workflows/` has a
   `timeout-minutes`. Count today: 0 of 6 files.
4. *Coverage floor existence.* `jls.edit` has a JaCoCo rule in `pom.xml` with a
   nonzero minimum, and it is greater than or equal to the value recorded by
   TASK-0019. Today the package is explicitly exempted (`pom.xml:408-409`).
5. *Diff ratchet.* Inserting one element into a committed 10,000-element fixture
   changes at most K lines of the saved plain-text file, K pinned by TASK-0005.
   Today one insertion renumbers every later element.
6. *Complexity ratchet.* The stimulus parse and the wire-end fixup, benchmarked
   at 1x and 10x input, scale within a stated linear tolerance.
7. *Event-drop assertion.* An event polled past the time limit is observable in
   a resumed run rather than removed from the duplicate-check set and dropped.
8. *Platform parity.* The full suite is a required check on all three OSes.

## Demo slice

Arms 1, 2, 3 and 7 alone: TASK-0002 (3 d) + TASK-0004 (2 d) + TASK-0015 (1 d) +
TASK-0011 (3 d) + TASK-0012 (2 d) = **~2.5 maintainer-weeks**. It ships four
standing ratchets that can never silently regress, and it needs nothing from any
other feature. This is the slice to fund first because it is the one that makes
the rest measurable.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | required/beneficial |
|---|---|---|---|
| FEAT-001 | Registry-keyed table totality discipline | The two defects already fixed on this branch (`970db41`, `b54e6ee`) were both missing rows in registry-keyed tables; without a lint the third one is already being written | required |
| FEAT-002 | Fail-loud loader and attribute dispatch | `Element.setValue` returns silently on an unknown attribute; this is the silent-data-loss path that makes every other file-format guarantee unverifiable | required |
| FEAT-003 | Uncompressed canonical default with stable-id references | Save-time reference ids are dense and reassigned on every save, so review of a circuit change is not possible today | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | The stimulus parse is 80% of end-to-end wall time and the dominant allocator of every long run | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | Batch pause is identical to stop and the time limit is a silent ceiling; both are ordinary defects, not design | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | No workflow has a timeout and only one platform is required; the gate is not measuring what it claims to | required |
| FEAT-009 | The measurement gate and a tracked calibration fixture | The only CPU-scale performance anchor in the tree is untracked, which is what blocks deleting `riscv/` per D5 | required |
| FEAT-011 | Accessibility, keyboard operability and onboarding | Three open issues on the editor surface, all older than a year, all sharing one test substrate | required |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | The largest single maintenance debt in the tree and the reason `jls.edit` is unfloored; marked beneficial only because its full 12-20 mw is booked against CAP-01 and CAP-04, which also require it | beneficial |
| FEAT-010 | Deterministic native installers and file association | Seven open issues; the bring-your-own-JDK barrier is the first thing a new user hits. Booked against CAP-06 | beneficial |
| FEAT-012 | Semantic merge safety and per-kind merge rules | A three-way merge can produce a file that parses and is semantically corrupt; booked against CAP-01 | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | closes |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | overlaps (its value-representation half is FEAT-026, outside CAP-00) |
| 191 | Deterministic macOS installer: reproducible (or bounded-residual) dmg | closes |
| 190 | Deterministic Windows installer: reproducible (or bounded-residual) msi | closes |
| 188 | Deterministic native installers: per-format byte-reproducibility program | closes |
| 185 | Reproducible Builds conformance: independent-rebuild verification, published `.buildinfo`, and a declared reproducible-artifact scope | closes |
| 184 | Release-artifact reproducibility gaps: container apt pinning, installer `SOURCE_DATE_EPOCH`, and a BOM reproducibility guard in CI | closes |
| 162 | UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | closes |
| 134 | Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing) | closes |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | closes |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | closes |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | closes |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | closes |
| 82 | Distribution: jpackage installers per OS and `.jls` file association | closes |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs (its registry half already shipped; FEAT-001 builds on it) |
| 76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences | closes |
| 75 | Keyboard operability and accessibility | closes |
| 73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | closes |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps (its allowlist is a registry-keyed table, so FEAT-001 serves it) |
| - | The silent-data-loss path (FEAT-002) | **no issue** |
| - | The quadratic and materializing paths (FEAT-005) | **no issue** |
| - | The diff-amplification and format work (FEAT-003, FEAT-012) | **no issue** |
| - | The tracked calibration fixture that blocks deleting `riscv/` (FEAT-009) | **no issue** |
| - | CAP-00 itself, as a sequencing program | **no issue** |

## Open decisions

1. **Does `jls.edit` get a floor before or after decomposition?**
   Recommendation: **before.** Set it at the measured headless value and enter
   the raise-only ratchet (TASK-0019). Reason: decomposition without a
   regression detector is refactoring on faith, and `pom.xml:408-409` already
   states the exemption is conditional on this work.
2. **Is XZ write retained as an explicit user option under D1?**
   Recommendation: **yes**, as an explicit flag. Reason: the mechanism is
   already implemented and tested in `FileAbstractor`; removing it costs work
   and buys nothing.
3. **Do `.jls~` autosave checkpoints and in-memory undo snapshots stay
   compressed?** Recommendation: **yes.** Reason: their constraint is write
   volume and memory pressure, not diffability - D1's rationale does not reach
   them.
4. **Strict byte-reproducibility or a declared bounded residual per installer
   format?** Recommendation: **declared bounded residual, published.** Reason:
   #185 already frames it that way and a strict claim JLS cannot hold is worse
   than a bounded one it can.
5. **Which JDKs are in the required matrix once three platforms are required?**
   Recommendation: canonical JDK plus one leading-edge leg per platform.
   Reason: #233's finding that zero-margin floors flake across the matrix means
   matrix width has a real recurring cost.

## Kill criteria

- **KC-00-1.** The required feature set exceeds **63 mw** (1.5x the top of the
  band) with fewer than 11 of the 16 issues it closes actually closed: stop, and re-scope
  CAP-00 to FEAT-001, 002, 005, 006, 007 only (10-18 mw), which is the subset
  that is purely defect closure.
- **KC-00-2.** K9, restated numerically and continuous. Per-edit GUI cost
  regresses above the HEAD measurements of 58 ms at 10,000 elements or 552 ms
  at 100,000 elements, or GUI startup regresses at all: the responsible feature
  stops until it is back under. This outranks every other criterion here.
- **KC-00-3.** TASK-0019 plus TASK-0020 (3.5 wk) does not produce a `jls.edit`
  floor above 0: FEAT-008 is cut from CAP-00 and carried entirely by CAP-01,
  and #84/#91/#162 are re-scoped as a separate program.
- **KC-00-4.** Three platforms cannot be made required because the full suite
  exceeds the hosted six-hour ceiling on any one of them: the long-run lane
  becomes scheduled-only and the required check stays the fast lane, recorded
  as an accepted cost rather than left implicit.
- **KC-00-5.** The installer residual cannot be bounded and stated for any
  format: that format ships unreproducible with the residual documented; the
  reproducibility claim is narrowed rather than dropped.

## Evidence

- Silent data loss: `src/jls/elem/Element.java:344-351` - `setValue(String,int)`
  iterates `savedAttributes()` and returns with no diagnostic when nothing
  matches; the loader calls it unconditionally at `src/jls/Circuit.java:1067,
  1078, 1089, 1105, 1116`. Verified at HEAD `b54e6ee`.
- Dense save ids: `src/jls/elem/Element.java:21-22` - "The file-local reference
  index, reassigned on every save."
- Past-limit event drop: `src/jls/sim/Simulator.java:224-232` - the event is
  polled and removed from `dupCheck` **before** the `now > maxTime` break, so it
  is discarded and cannot be re-posted.
- Batch pause equals stop: `src/jls/sim/BatchSimulator.java:87-90` sets
  `stopping = true`, identical to `stop()` at `:75-78`.
- Quadratic stimulus parse: `src/jls/elem/SigSim.java:64-74` - four `+=`
  concatenations inside the per-line loop.
- CI: six workflow files under `.github/workflows/`, zero occurrences of
  `timeout-minutes` (grep, verified at HEAD).
- `jls.edit` unfloored: `pom.xml:408-409`, "jls.edit is deliberately unfloored
  until the #91/#84 work makes editor code testable."
- Element registry: 35 types, `src/jls/elem/ElementRegistry.java:38-77`; 27
  `react` implementations under `src/jls/elem/`. Both supersede the stale counts
  in `docs/capability-roadmap/`.
- `SimpleEditor` is **5,852 lines** at HEAD, not the 4,119 in #84's title - the
  debt grew by 42% while the issue sat open. That growth rate is the argument
  for priority 1.
- Format container mechanism already exists: `src/jls/FileAbstractor.java:43-53,
  180-230`; the 64 MiB cap is measured against decompressed text at
  `FileAbstractor.java:65`. D1 is therefore a policy change, not a project.
- Programs: FEAT-001, 004 sit under P3; FEAT-005 under P1; FEAT-006 under P2;
  FEAT-003 and FEAT-012 under P11 (`docs/capability-roadmap/AMENDMENT.md:423`).
  FEAT-002, 007, 008, 009, 010, 011 are **UNOWNED** - the committed roadmap does
  not pay for the majority of this capstone, which is the structural reason it
  was deferred for a decade.
- Cost band arithmetic, stated rather than asserted: the eight required features
  sum to 23-42 mw (FEAT-001 1-2, FEAT-002 1-2, FEAT-003 2-4, FEAT-005 2-3,
  FEAT-006 3-5, FEAT-007 3-6, FEAT-009 5-10, FEAT-011 6-10); the registry's
  24-42 mw band is that subset. Funding all eleven consuming features to
  completion inside CAP-00 is **52-91 mw**; the difference is booked against
  CAP-01, CAP-04 and CAP-06, which require FEAT-008, FEAT-010 and FEAT-012
  anyway. Basis for the unit: ~200-250 lines of shipped-and-tested code per
  maintainer-week at the 93.0/92.0/84.5 JaCoCo package aggregate plus the 80/82
  PIT bar.
- Binding decisions: D1 (uncompressed default), D2 (diff stability is a
  requirement), D5 (`riscv/` will be deleted), D6 (fixes land immediately, not
  gated on the core extraction), D10 (path and cost, never precedent).
