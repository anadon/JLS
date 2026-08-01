# THE GITHUB ISSUE MAP

Every open issue in `anadon/jls` mapped to the plan ids that touch it, the
plan ids that have no issue, and the issues nothing in the plan touches. This
is a **planning** artifact: it proposes which plan items would close which
issues. It does not change any issue and no issue was created.

**Verification method.** The 34 open issues were enumerated by
`list_issues(state=OPEN)`, paginated ten at a time to exhaustion; the API
reported `totalCount: 34` and returned 34. The 127 closed issues were
enumerated the same way. Every number cited anywhere under `docs/plan/` was
checked against those two lists. Three cited numbers - **#233, #242 and
#244** - are **pull requests, not issues**, and the documents citing them were
corrected to say so. No cited number was fabricated and none is missing from
the tracker.

**Two columns, two strengths.** *Cites* means the id carries the issue as a
row in its own `## Related GitHub issues` table with a stated relationship -
this is the formal link. *Mentions* means the id refers to the issue in prose
(evidence, notes, a trap) without claiming a relationship. Both are listed
because a prose mention is often where the real technical anchor is, but only
the first is a claim about closure.

---

## OPEN ISSUES -> PLAN IDS

### #265 - CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-02 | cap | depends on |
| CAP-06 | cap | depends on |
| CAP-09 | cap | depends on |
| CAP-14 | cap | depends on |
| CAP-15 | cap | depends on |
| FEAT-007 | feat | closes |
| TASK-0015 | task | depends on |
| TASK-0016 | task | overlaps |
| TASK-0017 | task | closes |
| TASK-0018 | task | overlaps |
| TASK-0027 | task | overlaps |
| TASK-0051 | task | overlaps |
| TASK-0075 | task | overlaps |
| TASK-0080 | task | overlaps |
| TASK-0098 | task | depends on |
| TASK-0100 | task | overlaps |

### #264 - Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215)

| plan id | level | relationship |
|---|---|---|
| CAP-07 | cap | depends on |
| CAP-13 | cap | overlaps |
| CAP-15 | cap | depends on |
| FEAT-004 | feat | overlaps |
| FEAT-021 | feat | overlaps |
| FEAT-023 | feat | closes |
| FEAT-044 | feat | overlaps |
| FEAT-046 | feat | overlaps |
| TASK-0044 | task | informs |
| TASK-0051 | task | depends on |
| TASK-0052 | task | closes |
| TASK-0085 | task | overlaps |
| TASK-0086 | task | overlaps |
| TASK-0089 | task | overlaps |
| TASK-0090 | task | overlaps |
| TASK-0091 | task | informs |
| TASK-0094 | task | overlaps |
| TASK-0095 | task | overlaps |
| TASK-0100 | task | overlaps |

### #232 - Simulation hot path: per-signal java.util.BitSet allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | overlaps |
| CAP-02 | cap | depends on |
| CAP-03 | cap | depends on |
| CAP-04 | cap | depends on |
| CAP-05 | cap | depends on |
| CAP-08 | cap | depends on |
| CAP-09 | cap | depends on |
| CAP-10 | cap | overlaps |
| CAP-11 | cap | overlaps |
| CAP-12 | cap | overlaps |
| CAP-15 | cap | informs |
| CAP-17 | cap | overlaps |
| FEAT-005 | feat | overlaps |
| FEAT-006 | feat | informs |
| FEAT-009 | feat | informs |
| FEAT-026 | feat | closes |
| FEAT-030 | feat | depends on |
| FEAT-054 | feat | overlaps |
| TASK-0009 | task | overlaps |
| TASK-0010 | task | informs |
| TASK-0013 | task | overlaps |
| TASK-0022 | task | informs |
| TASK-0023 | task | informs |
| TASK-0026 | task | depends on |
| TASK-0033 | task | informs |
| TASK-0056 | task | closes |
| TASK-0057 | task | overlaps |
| TASK-0063 | task | overlaps |
| TASK-0064 | task | overlaps |
| TASK-0070 | task | informs |
| TASK-0074 | task | overlaps |
| TASK-0076 | task | overlaps |
| TASK-0078 | task | overlaps |
| TASK-0079 | task | overlaps |
| TASK-0080 | task | informs |
| TASK-0083 | task | informs |
| TASK-0087 | task | informs |
| TASK-0088 | task | informs |
| TASK-0093 | task | informs |

Also mentioned in prose by: FEAT-031, TASK-0011, TASK-0059, TASK-0060, TASK-0066, TASK-0072, TASK-0077.

### #224 - Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | informs |
| FEAT-050 | feat | depends on |
| TASK-0037 | task | informs |
| TASK-0040 | task | informs |
| TASK-0067 | task | informs |
| TASK-0106 | task | depends on |
| TASK-0107 | task | tracking |

### #223 - Extension-point catalog: enumerate and type the seams modules contribute to (element provider, palette contributor, exporter, op observer, ...)

| plan id | level | relationship |
|---|---|---|
| CAP-06 | cap | overlaps |
| CAP-10 | cap | depends on |
| CAP-11 | cap | depends on |
| CAP-12 | cap | depends on |
| FEAT-032 | feat | depends on |
| FEAT-045 | feat | overlaps |
| FEAT-050 | feat | closes |
| TASK-0065 | task | overlaps |
| TASK-0067 | task | overlaps |
| TASK-0106 | task | closes |

### #214 - In-editor test panel: a GUI front-end over the batch -t test-vector engine (Digital-parity, HDL-independent)

| plan id | level | relationship |
|---|---|---|
| CAP-04 | cap | overlaps |
| CAP-06 | cap | closes |
| CAP-09 | cap | overlaps |
| CAP-16 | cap | depends on |
| FEAT-025 | feat | informs |
| FEAT-053 | feat | closes |
| FEAT-057 | feat | overlaps |
| TASK-0014 | task | overlaps |
| TASK-0021 | task | overlaps |
| TASK-0068 | task | informs |
| TASK-0072 | task | overlaps |
| TASK-0073 | task | overlaps |
| TASK-0084 | task | overlaps |
| TASK-0086 | task | informs |
| TASK-0111 | task | closes |

### #212 - Element-provider plugin API: discover external ElementType descriptors via ServiceLoader atop the #78 registry (the recorded replacement for the removed XML loader, #80 H2)

| plan id | level | relationship |
|---|---|---|
| CAP-10 | cap | informs |
| CAP-11 | cap | informs |
| CAP-12 | cap | informs |
| CAP-16 | cap | overlaps |
| FEAT-050 | feat | closes |
| TASK-0038 | task | informs |
| TASK-0040 | task | overlaps |
| TASK-0067 | task | overlaps |
| TASK-0106 | task | depends on |
| TASK-0107 | task | closes |

### #202 - RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle

| plan id | level | relationship |
|---|---|---|
| CAP-02 | cap | closes |
| CAP-07 | cap | informs |
| CAP-08 | cap | overlaps |
| CAP-09 | cap | closes |
| CAP-15 | cap | overlaps |
| FEAT-009 | feat | closes |
| FEAT-023 | feat | overlaps |
| FEAT-032 | feat | overlaps |
| FEAT-033 | feat | overlaps |
| FEAT-034 | feat | closes |
| FEAT-038 | feat | closes |
| TASK-0012 | task | depends on |
| TASK-0013 | task | depends on |
| TASK-0014 | task | depends on |
| TASK-0016 | task | depends on |
| TASK-0022 | task | depends on |
| TASK-0024 | task | depends on |
| TASK-0025 | task | overlaps |
| TASK-0034 | task | overlaps |
| TASK-0038 | task | overlaps |
| TASK-0043 | task | depends on |
| TASK-0044 | task | depends on |
| TASK-0066 | task | depends on |
| TASK-0068 | task | overlaps |
| TASK-0069 | task | overlaps |
| TASK-0070 | task | depends on |
| TASK-0071 | task | overlaps |
| TASK-0072 | task | depends on |
| TASK-0073 | task | closes |
| TASK-0074 | task | depends on |
| TASK-0076 | task | depends on |
| TASK-0077 | task | depends on |
| TASK-0079 | task | closes |
| TASK-0080 | task | closes |
| TASK-0082 | task | informs |
| TASK-0111 | task | overlaps |
| TASK-0112 | task | overlaps |

Also mentioned in prose by: TASK-0083.

### #191 - Deterministic macOS installer: reproducible (or bounded-residual) dmg

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| FEAT-010 | feat | closes |
| TASK-0028 | task | closes |

### #190 - Deterministic Windows installer: reproducible (or bounded-residual) msi

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| FEAT-010 | feat | closes |
| TASK-0028 | task | closes |

### #188 - Deterministic native installers: per-format byte-reproducibility program

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| FEAT-010 | feat | tracking |
| TASK-0028 | task | closes |

### #185 - Reproducible Builds conformance: independent-rebuild verification, published .buildinfo, and a declared reproducible-artifact scope

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| FEAT-010 | feat | closes |
| TASK-0028 | task | closes |

### #184 - Release-artifact reproducibility gaps: container apt pinning, installer SOURCE_DATE_EPOCH, and a BOM reproducibility guard in CI

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| FEAT-010 | feat | closes |
| TASK-0028 | task | closes |
| TASK-0071 | task | informs |

### #171 - Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | closes |
| CAP-06 | cap | overlaps |
| FEAT-003 | feat | overlaps |
| FEAT-012 | feat | overlaps |
| FEAT-052 | feat | closes |
| TASK-0005 | task | overlaps |
| TASK-0032 | task | depends on |
| TASK-0109 | task | depends on |
| TASK-0110 | task | closes |

### #170 - Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting)

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | overlaps |
| CAP-01 | cap | closes |
| FEAT-012 | feat | overlaps |
| FEAT-052 | feat | closes |
| TASK-0001 | task | overlaps |
| TASK-0031 | task | overlaps |
| TASK-0032 | task | overlaps |
| TASK-0036 | task | depends on |
| TASK-0037 | task | depends on |
| TASK-0038 | task | overlaps |
| TASK-0107 | task | overlaps |
| TASK-0108 | task | overlaps |
| TASK-0110 | task | closes |

Also mentioned in prose by: TASK-0092.

### #169 - Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | closes |
| CAP-06 | cap | overlaps |
| FEAT-051 | feat | closes |
| TASK-0108 | task | closes |
| TASK-0109 | task | depends on |

### #168 - P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification (collab Stage 1a)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | closes |
| FEAT-051 | feat | closes |
| TASK-0108 | task | closes |

### #167 - Operation layer: reify editor mutations as invertible, serializable commands behind one entry point (collab Stage 0b)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | depends on |
| CAP-04 | cap | depends on |
| CAP-05 | cap | depends on |
| FEAT-003 | feat | informs |
| FEAT-015 | feat | closes |
| FEAT-052 | feat | depends on |
| TASK-0032 | task | depends on |
| TASK-0035 | task | depends on |
| TASK-0036 | task | overlaps |
| TASK-0037 | task | closes |
| TASK-0038 | task | depends on |
| TASK-0092 | task | overlaps |
| TASK-0109 | task | depends on |
| TASK-0110 | task | depends on |

### #163 - Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue)

| plan id | level | relationship |
|---|---|---|
| CAP-01 | cap | tracking |
| FEAT-003 | feat | informs |
| FEAT-012 | feat | informs |
| FEAT-013 | feat | informs |
| FEAT-014 | feat | informs |
| FEAT-015 | feat | depends on |
| FEAT-051 | feat | tracking |
| FEAT-052 | feat | tracking |
| TASK-0032 | task | tracking |
| TASK-0035 | task | informs |
| TASK-0036 | task | informs |
| TASK-0037 | task | tracking |
| TASK-0041 | task | informs |
| TASK-0108 | task | tracking |
| TASK-0109 | task | tracking |
| TASK-0110 | task | tracking |

### #162 - UI-layer coverage: a CI display substrate for #91 layers 2-3, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-01 | cap | depends on |
| CAP-04 | cap | depends on |
| FEAT-008 | feat | closes |
| FEAT-011 | feat | overlaps |
| TASK-0001 | task | overlaps |
| TASK-0017 | task | overlaps |
| TASK-0019 | task | depends on |
| TASK-0021 | task | closes |
| TASK-0029 | task | depends on |
| TASK-0030 | task | depends on |
| TASK-0051 | task | overlaps |
| TASK-0053 | task | overlaps |
| TASK-0069 | task | depends on |
| TASK-0092 | task | depends on |
| TASK-0111 | task | overlaps |

Also mentioned in prose by: CAP-09.

### #134 - Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing)

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| FEAT-010 | feat | closes |
| TASK-0028 | task | closes |

### #111 - Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg (ex: Windows test-suite failures, fixed)

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-15 | cap | depends on |
| FEAT-007 | feat | closes |
| TASK-0015 | task | depends on |
| TASK-0017 | task | closes |
| TASK-0027 | task | overlaps |
| TASK-0051 | task | overlaps |
| TASK-0073 | task | informs |
| TASK-0075 | task | overlaps |
| TASK-0098 | task | depends on |

Also mentioned in prose by: TASK-0016, TASK-0025, TASK-0032.

### #101 - Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| FEAT-007 | feat | closes |
| FEAT-008 | feat | overlaps |
| TASK-0017 | task | informs |
| TASK-0018 | task | closes |
| TASK-0027 | task | depends on |
| TASK-0108 | task | overlaps |

Also mentioned in prose by: TASK-0080.

### #91 - Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-01 | cap | depends on |
| CAP-04 | cap | depends on |
| FEAT-008 | feat | closes |
| TASK-0019 | task | depends on |
| TASK-0020 | task | depends on |
| TASK-0021 | task | closes |
| TASK-0029 | task | depends on |
| TASK-0069 | task | depends on |
| TASK-0092 | task | depends on |
| TASK-0108 | task | depends on |
| TASK-0111 | task | depends on |

Also mentioned in prose by: CAP-09, TASK-0001, TASK-0017.

### #84 - Decompose SimpleEditor: 4,119 lines, a 9-state mouse machine, a 305-line source== dispatcher that already caused #37, and whole-circuit undo snapshots

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-01 | cap | depends on |
| CAP-04 | cap | depends on |
| CAP-16 | cap | depends on |
| CAP-17 | cap | overlaps |
| FEAT-008 | feat | closes |
| FEAT-015 | feat | overlaps |
| FEAT-043 | feat | overlaps |
| TASK-0019 | task | depends on |
| TASK-0020 | task | closes |
| TASK-0021 | task | depends on |
| TASK-0029 | task | overlaps |
| TASK-0050 | task | overlaps |
| TASK-0092 | task | depends on |
| TASK-0105 | task | overlaps |
| TASK-0106 | task | overlaps |

### #82 - Distribution: jpackage installers per OS and .jls file association - remove the bring-your-own-JDK barrier

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | closes |
| FEAT-010 | feat | closes |
| TASK-0027 | task | closes |

### #78 - Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | informs |
| CAP-04 | cap | informs |
| CAP-05 | cap | informs |
| CAP-13 | cap | informs |
| CAP-16 | cap | informs |
| FEAT-001 | feat | informs |
| FEAT-049 | feat | informs |
| FEAT-050 | feat | informs |
| TASK-0001 | task | informs |
| TASK-0002 | task | informs |
| TASK-0019 | task | informs |
| TASK-0039 | task | informs |
| TASK-0040 | task | informs |
| TASK-0049 | task | informs |
| TASK-0055 | task | informs |
| TASK-0058 | task | informs |
| TASK-0061 | task | informs |
| TASK-0068 | task | informs |
| TASK-0078 | task | informs |
| TASK-0085 | task | informs |
| TASK-0096 | task | informs |
| TASK-0099 | task | informs |
| TASK-0105 | task | informs |
| TASK-0106 | task | informs |
| TASK-0107 | task | informs |
| TASK-0110 | task | informs |

Also mentioned in prose by: TASK-0003, TASK-0032, TASK-0038, TASK-0067.

### #76 - Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| CAP-12 | cap | informs |
| FEAT-011 | feat | closes |
| TASK-0030 | task | closes |

### #75 - Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| FEAT-011 | feat | closes |
| TASK-0020 | task | informs |
| TASK-0029 | task | closes |
| TASK-0030 | task | overlaps |
| TASK-0105 | task | overlaps |
| TASK-0111 | task | overlaps |

### #73 - First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots

| plan id | level | relationship |
|---|---|---|
| CAP-00 | cap | closes |
| CAP-06 | cap | depends on |
| CAP-16 | cap | overlaps |
| FEAT-011 | feat | closes |
| TASK-0030 | task | closes |
| TASK-0079 | task | overlaps |

### #63 - HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation

| plan id | level | relationship |
|---|---|---|
| CAP-08 | cap | overlaps |
| CAP-09 | cap | depends on |
| CAP-10 | cap | informs |
| CAP-11 | cap | informs |
| CAP-12 | cap | informs |
| CAP-15 | cap | closes |
| FEAT-020 | feat | overlaps |
| FEAT-021 | feat | informs |
| FEAT-022 | feat | informs |
| FEAT-023 | feat | overlaps |
| FEAT-024 | feat | closes |
| FEAT-032 | feat | informs |
| FEAT-045 | feat | informs |
| TASK-0043 | task | overlaps |
| TASK-0044 | task | overlaps |
| TASK-0049 | task | overlaps |
| TASK-0051 | task | informs |
| TASK-0053 | task | closes |
| TASK-0062 | task | informs |
| TASK-0069 | task | informs |
| TASK-0096 | task | informs |

Also mentioned in prose by: FEAT-034.

### #62 - HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process)

| plan id | level | relationship |
|---|---|---|
| CAP-08 | cap | closes |
| CAP-15 | cap | overlaps |
| CAP-16 | cap | overlaps |
| FEAT-020 | feat | depends on |
| FEAT-022 | feat | closes |
| FEAT-025 | feat | informs |
| TASK-0047 | task | informs |
| TASK-0048 | task | overlaps |
| TASK-0050 | task | closes |
| TASK-0079 | task | depends on |
| TASK-0083 | task | depends on |

Also mentioned in prose by: TASK-0089.

### #61 - HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline)

| plan id | level | relationship |
|---|---|---|
| CAP-02 | cap | overlaps |
| CAP-08 | cap | closes |
| CAP-09 | cap | informs |
| CAP-15 | cap | closes |
| FEAT-019 | feat | informs |
| FEAT-020 | feat | closes |
| FEAT-022 | feat | depends on |
| FEAT-025 | feat | informs |
| TASK-0042 | task | informs |
| TASK-0043 | task | informs |
| TASK-0045 | task | informs |
| TASK-0047 | task | closes |
| TASK-0048 | task | closes |
| TASK-0049 | task | overlaps |
| TASK-0050 | task | depends on |
| TASK-0051 | task | informs |
| TASK-0054 | task | informs |
| TASK-0077 | task | informs |
| TASK-0100 | task | informs |
| TASK-0112 | task | overlaps |

Also mentioned in prose by: TASK-0046, TASK-0089.

### #59 - HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope for import

| plan id | level | relationship |
|---|---|---|
| CAP-05 | cap | overlaps |
| CAP-07 | cap | overlaps |
| CAP-08 | cap | overlaps |
| CAP-13 | cap | informs |
| CAP-14 | cap | informs |
| CAP-15 | cap | tracking |
| FEAT-004 | feat | informs |
| FEAT-018 | feat | depends on |
| FEAT-019 | feat | overlaps |
| FEAT-020 | feat | tracking |
| FEAT-021 | feat | informs |
| FEAT-023 | feat | informs |
| TASK-0007 | task | informs |
| TASK-0008 | task | overlaps |
| TASK-0039 | task | informs |
| TASK-0041 | task | overlaps |
| TASK-0042 | task | overlaps |
| TASK-0043 | task | overlaps |
| TASK-0044 | task | overlaps |
| TASK-0045 | task | overlaps |
| TASK-0046 | task | overlaps |
| TASK-0047 | task | overlaps |
| TASK-0048 | task | overlaps |
| TASK-0052 | task | overlaps |
| TASK-0053 | task | overlaps |
| TASK-0062 | task | overlaps |
| TASK-0077 | task | overlaps |
| TASK-0090 | task | informs |
| TASK-0094 | task | informs |

Also mentioned in prose by: TASK-0089.

---

## PLAN IDS WITH NO OPEN ISSUE

Recorded because the gap is information. Each of these documents carries an
explicit "no issue" row rather than a blank field. An id appears here when its
issue table names no **open** issue; several of them cite closed issues as
recorded decisions, which is a different thing.

**Capstones (0).** None.

**Features (18).** FEAT-002, FEAT-016, FEAT-017, FEAT-027, FEAT-028, FEAT-029, FEAT-031, FEAT-035, FEAT-036, FEAT-037, FEAT-039, FEAT-040, FEAT-041, FEAT-042, FEAT-047, FEAT-048, FEAT-055, FEAT-056

**Tasks (12).** TASK-0003, TASK-0004, TASK-0006, TASK-0011, TASK-0059, TASK-0060, TASK-0081, TASK-0097, TASK-0101, TASK-0102, TASK-0103, TASK-0104

The structural findings behind that list, stated once:

- **Sixteen of the eighteen capstones have no tracking issue.** Only CAP-01
  has one (#163) and only CAP-15 has substantial tracker coverage of its
  spine. CAP-00 - the maintainer's stated highest priority - has none, despite
  closing sixteen open issues. That absence is the structural reason a decade
  of maintenance stayed deferred: the constituent issues existed; the program
  that would sequence them did not.
- **The silent-data-loss path has no issue.** FEAT-002 and its tasks.
- **The quadratic and materializing paths have no issue.** FEAT-005 and its
  tasks.
- **The diff-amplification, merge-safety and internal-versioning work has no
  issue** - the whole of maintainer decisions D1, D2 and D3. FEAT-003,
  FEAT-012, FEAT-013, FEAT-014.
- **The entire analog program has no issue.** FEAT-045 through FEAT-049 and
  every task under them; a search of the tracker for analog returns nothing.
- **The entire physical program has no issue.** FEAT-040 through FEAT-044 -
  the package library, packing, the emitters, the breadboard canvas and the
  shuttle wrapper.
- **The entire N-ary program has no issue.** FEAT-028, FEAT-029, FEAT-039.
- **The parity, device and machine layers have no issue.** #232 covers only
  the value representation, not the queue, the zero-delay closure, the
  fidelity boundary or the parity harness.
- **The migration-parity path has no issue.** FEAT-025.
- **Distributed execution has no issue.** CAP-17 and FEAT-054 through
  FEAT-057, added after the registry closed.

---

## OPEN ISSUES NOTHING IN THE PLAN TOUCHES

**None.** All 34 open issues are touched by at least one plan id, and 34 of
34 are carried as a formal row in at least one document's issue table.

That is a finding rather than a convenience. CAP-00 was scoped from HEAD
defects and absorbed the whole distribution, CI, GUI and accessibility
backlog, which is where most of the open tracker lives; CAP-15 absorbed the
HDL staging issues; CAP-01 absorbed the collaboration issues. The tracker and
the plan cover the same ground from opposite ends - the tracker is dense
exactly where the plan is cheap, and silent exactly where the plan is
expensive.

---

## A CAUTION ON NUMBERS CITED BUT NOT OPEN

The evidence corpus and several documents cite closed issues as recorded
decisions or as prior work. That is correct and useful; what is not correct is
citing them as open. Every such citation under `docs/plan/` labels the issue
closed. The numbers involved, all verified closed against the 127-issue closed
list: #17, #20, #21, #25, #37, #38, #39, #47, #49, #51, #52, #55, #56, #57,
#58, #71, #72, #77, #79, #80, #94, #95, #98, #100, #103, #105, #122, #125,
#126, #128, #129, #130, #135, #136, #153, #159, #165, #166, #180, #181, #189,
#198, #199, #200, #201, #210, #213, #215, #216, #220, #221, #222, #231.

#213 and #215 were consolidated into #264; #221 and #231 are recorded
decisions, not open work. **#233, #242 and #244 are pull requests.** Verify a
number before citing it; a wrong number is worse than none.
