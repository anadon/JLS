# Issue #365: FEAT-041: a drawn design becomes a buildable parts list — every element assigned a package and section deterministically, word-level parts cascaded into real slices, with the BOM, the wiring list and two datasheet checks
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the BOM, the wiring list and the two checks, FEAT-041 asks for one thing:
**a durable correspondence between a thing a student drew and a thing a student can hold.**
Everything the issue enumerates as its deliverable is a *report printed off that
correspondence* — the BOM is a group-by, the wiring list is a join, the loading check is a
sum. The correspondence is the asset.

The project already knows this and says it better than the issue does.
`docs/capability-roadmap/README.md` (the cross-probing paragraph): *"JLS knows which cell
instance corresponds to which drawn element, and no external viewer can ever know that…
E without D is a worse KLayout. E with D is a thing that does not exist."* That is the
strongest sentence anyone has written about why this program belongs in JLS rather than in
KiCad, and #365 never invokes it. It matters, because it is the argument that survives
contact with `docs/capability-roadmap/sweep-06-physical-boundary.md`, which declines the
whole PCB tier as *"different tool class — this is KiCad's domain and KiCad is excellent at
it"* and refuses IPC-D-356A on the ground that *"a bare-board test netlist without a board
layout has no consumer."* #365 is not refuted by that sweep, but it is not obviously
distinguished from it either, and the issue should say out loud which side of that line it
stands on. The answer is the correspondence: KiCad can pack, route and fabricate; it cannot
tell you which gate on your schematic is U3.2 *because you drew it*.

This is also why the feature is aligned rather than a fourth trajectory bolted onto three.
`docs/grand-architecture.md` §2 funds CPU teaching (`riscv/`), FPGA deployment (#213/#264)
and collaboration (#163). The physical-build program converges with the first two on the
same asset — a drawn element graph whose nodes have permanent identity — and its capstone
(CAP-04, #297) targets SAP-1, not a new audience. Per that capstone's own D9 note this is
one trajectory (CS → ECE → EE), not a second product. I read the direction as correct.

## The reframe: the pack plan must be authored, not derived

§4 invariant 7 says *"no saved-format change (the pack plan is derived, not authored)."*
That is the single load-bearing choice in the issue, and I believe it is wrong. The whole
edifice of §1 criteria 2 and 3 — refdes purity, additive-only diffs — exists to compensate
for it, and it does not compensate enough.

**Deletion is not covered.** Criterion 3 promises additive-only diff for *inserting* one
unrelated gate. First-fit over stable-id order gives no such promise for deletion: delete
inverter e2 from a design packed as `U1{e1,e2,e3,e4,e5,e6} U2{e7}`, regenerate, and e7
falls into U1's freed section. The chip a student labelled U2 with a Sharpie is now U1.6.
§4 invariant 2 calls refdes churn *"the single most damaging failure mode"* and then
defends against half the edit space.

**The collaboration trajectory breaks it outright.** I checked the ordering the issue rests
on. `ElementId.compareTo` (`src/jls/elem/ElementId.java:278-285`) sorts by *replica string
first*, then counter, and the replica id is *"a fresh random draw (32 hex digits from a
UUID)"* per install (`:41-56`). For a single-author file the order is creation order and
insertion really is additive. The moment a second author touches the design — which is
exactly what #163 and the shipped `src/jls/collab/` are for — that author's entire element
set interleaves into the sort at a position decided by a random hex string, and first-fit
reassigns wholesale. Criterion 2's *"running it on a machine with a different replica id
produces the same output"* is true only for a design no second machine ever edited. The
purest invariant in the issue is not stable under the project's own funded trajectory.

**The alternative:** make the plan a committed, reconciled artifact — a lockfile, or
precisely KiCad's annotation model, which exists because pure derivation was tried and
regretted across the whole EDA industry. First run derives; every later run *reconciles*:
elements already bound keep their refdes and section, new elements get new ones, deleted
elements leave holes that are only reclaimed by an explicit `--reannotate`. This is
strictly stronger than what the issue asks for:

- criterion 3 (additive-only under insertion) becomes stability under *every* edit
  including deletion, redraw, and multi-author merge;
- criterion 2 (replica independence) becomes trivially true — the plan is in the repo;
- Open Question 1 ("how does a user bind a non-decomposable element to a part?") stops
  being a filing blocker: a hand-written row in the plan *is* the user binding, and the
  `Memory`-to-62256 case the issue worries about needs no new mechanism;
- §4 invariant 1's "determinism above optimality" dissolves (below).

The cost is one file to version, and the issue's own §3 already says *"Ephemeral. Nothing —
every output of this feature is intended to be committed, diffed and re-imported."* The
plan is already being treated as durable state; invariant 7 just refuses to name it as
such. **I am explicitly setting aside §1 criteria 2 and 3 as the acceptance shape** — they
are proxies for board-label stability, and the proxy is weaker than the property.

## Determinism versus optimality is a false dichotomy

§4 invariant 1: *"Packing is a first-fit… not an optimizer. Minimizing package count would
make refdes assignment depend on a search; determinism is worth more than one spare
74LS04."* Purity is a property of the *function*, not of the algorithm class. A bin-packing
optimizer with a fixed tie-break is exactly as pure as first-fit. CAP-04 is 35 packages
across two breadboards; parts cost money and rows are finite, and "one spare 74LS04"
understates what a hex-inverter-heavy design leaves on the floor. Once the plan is
authored (above), the algorithm can even improve between releases without moving a single
existing refdes. Restate the invariant as *"the packing function is pure and its version is
recorded in the plan."*

## The loading check is in the wrong feature and shipping years too late

TASK-0088 reads FEAT-040's electrical columns. FEAT-040 (#349) §4 invariant 3 forbids
exactly that: *"The electrical columns are inert… no pass may interpret them until the
strength model exists. This is an invariant, not a recommendation"* — pinned by its IC-7 as
an architecture test that fails when a non-test reader appears. #365's own dedup comment
records the collision and leaves it. Two features cannot both be right here.

The visionary resolution is not to arbitrate but to notice that the check does not belong
in a *packing* feature at all. Its most valuable half needs no part library, no netlist IR,
no packing, and no strength model:

- **Floating inputs** are a pure `WireNet` predicate — an `Input` in a net with zero
  drivers. The mis-teaching it corrects is in the tree today at
  `src/jls/elem/LogicElement.java:472-482` (*"Initialize all inputs to 0"*), which CAP-04
  names as the thing a breadboard punishes and JLS rewards.
- **Fan-out** as a *count* against a per-family budget catches the 200-input NOT gate the
  roadmap complains about, with datasheet-accurate unit loads a later refinement.

A `jls -drc` doing both is roughly a week against HEAD, before #349, before #336, before
this feature — and it delivers most of the audience impact §"Intended Audience" claims
("anyone who has ever miscounted fan-out", "the failure students actually hit"). The issue
argues correctly that the check must not wait for FEAT-027; it then makes it wait for two
features and two tasks anyway by routing it through the *physical* netlist. Split it out,
ship it now, and let TASK-0088 shrink to "replace the default unit loads with datasheet
figures" once #349 and #341 both exist.

## The alternative the issue never considers: techmap

Cascading an 8-bit adder into two 74LS83s is technology mapping. The tree already locates
and drives Yosys (`src/jls/hdl/yosys/YosysLocator.java`, `CellValidator`, `YosysNetlist`,
`src/jls/hdl/imp/NetlistImporter`), and `ARCHITECTURE.md`'s recorded plugin decision names
that subprocess boundary as settled policy — *"orchestrate external tools, never
reimplement HDL semantics."* Yosys `techmap` against a 74-series liberty is a real, used
flow in the TTL-CPU community. It would delete most of TASK-0087: the "synthetic
inter-slice nets" concept evaporates, because techmap output has ordinary nets and the
carry chain is just wires.

I do not recommend taking it, and the reason is worth writing on the issue: techmap
destroys the drawn-element ↔ instance correspondence that is this feature's entire product,
and it makes refdes depend on ABC's search — the failure mode §4 invariant 2 exists to
prevent. But §2's "Rejected:" list rejects only two internal task splits and never mentions
the external-tool route the project has a standing policy about. That omission will be
raised by someone eventually; answer it in the issue.

The *good* half of the idea survives independently: **use Liberty as the part-library
schema** instead of inventing one. FEAT-040's Open Question 1 (sections that share pins —
dual decoders, dual muxes with a common select) is a solved problem in Liberty, and
`docs/capability-roadmap/README.md` already wants a Liberty subset reader for sky130. One
reader, two libraries, and the 74-series work pays for the silicon work.

## One seam, not three

`jls.hdl.board` already *is* the physical-binding stack: `Board` as
`(name, fpga, format, pins)`, `Boards`, `PinBindings` with its all-or-nothing discipline,
`PcfEmitter` — shipped in #213. The capability roadmap explicitly recommends generalizing
`Board` into *"a target descriptor that can carry a wrapper template"* so a Tiny Tapeout
tile is the same shape as an iCEstick. A 74LS DIP target is that same shape a third time.

#365 instead opens a parallel `jls.pkg` with its own binding concept, and copies
`PinBindings`'s error aggregation *by imitation* rather than by reuse (§3 "Consumes" cites
the javadoc, not the class). Open Question 2 ("what is the reserved synthetic-net prefix?")
is likewise already answered in-tree: `src/jls/hdl/HdlNames.java` implements deterministic
legalization with reserved-word and collision suffixing, one instance per namespace. That
is the mechanism, and it does not need a new parse-error rule.

Cut the seam at *target*, not at *package*: `jls.hdl.target` (or an extended
`jls.hdl.board`) with FPGA board, shuttle tile and through-hole DIP as three descriptors
over one binding, one error-aggregation path, one name namespace. §4 invariant 6's real
requirement — headless, zero AWT — is satisfied there already (`HeadlessCoreRatchetTest`).

## Smaller notes

- **TASK-0078 (clock domains) is borrowed weight.** §2 justifies it as *"a decomposed
  element's clock is distributed across packages."* But the physical concern there is skew
  and clock fan-out across N packages — arithmetic, i.e. TASK-0088's job — not the
  *design*-level domain-crossing report TASK-0078 produces. Let FEAT-037 (#327) own it
  outright and drop it from this roster; §6 already concedes it "shares nothing with the
  packing chain."
- **§5 criterion 5 (registry-swept totality) is the best thing in the issue.** Sweeping
  `ElementRegistry` (35 types confirmed at HEAD) rather than a fixed list, so type 36
  cannot ship into a silently-ignoring packer, is precisely the FEAT-001 (#315) discipline
  and should be copied by every sibling in this cluster.
- **`grep -rniE "footprint|refdes|pinout|breadboard" src/` still returns 0 at HEAD.** The
  issue's premise holds; nothing has drifted since `2d0ca9d`.
- **Process weight.** ~600 lines of planning apparatus for 5.5 weeks of unshared work, at
  bus factor 1. The DAG bookkeeping is immaculate and the reframes above are the kind of
  thing it is not designed to surface.

## Verdict

**endorse-with-reframing.** The direction is right and the feature is the load-bearing one
in the physical program. Three changes before the children are filed: (1) make the pack
plan a saved, reconciled artifact and retire §1 criteria 2–3 in favour of stability under
deletion and multi-author merge; (2) split the floating-input and fan-out checks into an
early standalone DRC that ships against HEAD, resolving the collision with #349's inert-
columns invariant by removing the dependency rather than arguing it; (3) build on
`jls.hdl.board`'s target seam and `HdlNames` rather than opening a third parallel physical-
binding stack in `jls.pkg`. Record the techmap rejection, and lead the issue with the
correspondence argument — it is the only one KiCad cannot answer.
