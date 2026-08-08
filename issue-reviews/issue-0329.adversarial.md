# Issue #329: FEAT-043: a second canvas places parts on a solderless breadboard, and the placed arrangement — not the schematic — drives the simulation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a feature-level planning issue (FEAT-043) that decomposes into two "shared" prerequisite tasks (TASK-0036, TASK-0105) and two feature-specific tasks (TASK-0092: the canvas, TASK-0093: the consistency check and physical binding). It is heavily formalized (set-theoretic net-partition algebra, an iff contract for the physical binding) and its concrete code citations are, on inspection, accurate. The central problem is that the issue's own text is already stale relative to its own comment thread: two of its four constituent tasks have been filed as separate issues and the issue's roster/machine block was never updated to say so, which undermines the DAG-integrity claims the issue makes about itself.

## Findings, most severe first

### 1. [Critical] The issue's central "no issue exists" claim is false at read time, contradicted by its own comments, and breaks its own DAG-integrity invariant

The body states plainly: **"There is no issue for the breadboard canvas, the consistency check or the physical binding."** The roster table lists TASK-0092 and TASK-0093 as `not filed`, and the YAML machine block's `planned_tasks` still describes both as "ABSENT at 2d0ca9d" with no issue number.

This is false. The issue's own third comment (posted 2026-08-08T16:18:31Z, the same timestamp as the issue's `updated_at`) states in a table: `#401 (TASK-0092)` and `#396 (TASK-0093)` are filed, open issues, each explicitly declaring `part_of_feature: 329` in its own YAML block, and each fully speced (7-section interface contracts, hypotheses, predictions, Definition-of-Done checklists). I fetched both directly and confirmed: #401 (`state: open`, `part_of_feature: 329`) is TASK-0092 verbatim, and #396 (`state: open`, `part_of_feature: 329`) is TASK-0093 verbatim.

Consequences:
- The "Link phase" paragraph's claim — *"Every edge in `blocked_by` and `blocks` above is written on both issues... the closure is: none"* — is not true of the current graph: #401 and #396 both point at #329 via `part_of_feature`, but #329's `blocked_by: [316, 318, 341, 365]` / `blocks: []` lists neither. The issue asserts a DAG property it has not actually re-checked against its own children.
- The mermaid graph's `T92 --> F`, `T93 --> F` edges are drawn against placeholder nodes labelled "(planned, not filed)" when concrete, filed, actively-speced issues already exist for exactly that content.
- Completion Criterion 1, *"`planned_tasks` empty (each resolved to a filed issue or descoped)"*, is already partially satisfiable today and the issue doesn't know it.

**Recommendation:** Before any further planning work rides on this issue, edit #329's roster, machine block, and mermaid graph to reference #401 and #396 by number, and re-run the DAG-closure check against the corrected edge set. Until that happens, anyone scheduling off `blocked_by`/`blocks` on #329 is working from a graph the issue itself no longer believes.

### 2. [High] The cost estimate admits a 2.25x–3.75x gap covered by literally nothing

Open Question 4 states the band is "9-15 maintainer-weeks," sums the four named tasks to "8 wk" (TASK-0036 and TASK-0105 shared and counted once, "unshared remainder of 4 wk"), and says outright: *"a gap of 2.25x to 3.75x. All three figures are printed and no row is adjusted to make the band true... Do not read 4 wk, or 8 wk, as this feature."* The gap is attributed to "the residual named in §2: the part-placement vocabulary across the package library, which **no task id names**."

This is scope creep by omission, stated in the issue's own words: the majority of the feature's own declared cost band corresponds to work with no task, no filed issue, no acceptance criteria, and no owner — "UNOWNED" (Open Question 5) on top of that. A reviewer cannot evaluate feasibility of a "9-15 mw" feature when 5-11 of those weeks are unaccounted for by any decomposition.

**Recommendation:** File the residual (or descope it explicitly with a named successor) before this issue is treated as ready to schedule. An estimate band with an admitted, unticketed 2-4x gap is not a commitment, it's a placeholder wearing a number.

### 3. [High] The blocking architectural decision (Open Question 1) was bypassed by the issue's own children before being ratified

Open Question 1 (whether a breadboard strip is represented by synthetic wire ends grafted onto the pinned, determinism-critical `WireNet` class, or by changing that pinned class) is marked **"Blocks filing children."** Yet TASK-0092 (#401) was filed anyway, with a self-selected default (option (a), synthetic wires) and this justification: *"filing proceeded because... withholding the issue pending a decision the issue exists to frame is the circularity D10 forbids."*

That's a real tension worth naming plainly: #329 sets a gate ("Blocks filing children"), and the very next issue in the chain overrides the gate with its own reasoning rather than the "maintainer must choose" ratification #329 itself demands. Either the gate wasn't actually blocking (in which case #329's Open Questions section overstates its own authority), or it was bypassed. This is exactly the kind of soft self-override that produces two child tasks quietly built on an unratified assumption, discovered only when `WireNet`'s pinned determinism test (`WireNet.java:19-22`, confirmed accurate against source) breaks under implementation — at which point, per §7, both breadboard tasks move "by weeks."

**Recommendation:** Get the maintainer's actual ratification recorded on #329 (not inferred/defaulted on a child) before implementation of #401 begins, or explicitly note on #329 that the gate was downgraded to "recommended default, revisit if refuted" and accept the risk in writing.

### 4. [Medium] I4 and I6 are acknowledged orphan criteria, and the claim that no child covers them is itself questionable

The issue is honest that I4 (the two-way iff between physical/schematic simulation divergence) and I6 (the pedagogy ratchet as a standing test) are asserted only by "this issue's close-out — no single child asserts both directions," an abstract non-actor with no task ID, no PR, and no CI gate of its own. That's a real gap: nothing stops #401 and #396 both closing green while I4/I6 remain permanently unverified, since neither one's Definition of Done claims ownership of the *composed* feature-level property.

But cross-checking against #396 (TASK-0093) undercuts the "no single child" framing: #396's own predictions P3 (correct fixture → empty report / presumably identical simulations), P6 ("run the mis-wired fixture under both bindings; observe the outputs differ **at, and only at**, the nets the report named"), and P7 (SCHEMATIC-binding byte-identity across the golden corpus) collectively cover most of the substance of I4's iff, just not phrased as one named test. So #329 either understates what #396 already commits to, or #396 doesn't go far enough (e.g., it may never explicitly test the "identical when placement matches schematic" direction as a standing assertion, only via golden-corpus regression under the default binding). Either way, the ownership of I4 is genuinely ambiguous between the two documents, which is a coordination risk, not a solid design.

**Recommendation:** Name I4's and I6's owning artifact explicitly — either fold them into #396/#401's Definition of Done with cross-references, or create a real closing task/checklist item with its own test file, rather than leaving "this issue's close-out" as the responsible party.

### 5. [Medium] The feature's one genuinely novel deliverable (C6_CONTENTION) may ship as a no-op, and the underlying simulator behavior it depends on is itself non-physical

#396's own Observation O2 (independently plausible, not verifiable by me without running code) claims that swapping the file order of two `WireEnd` blocks in a bus-conflict fixture — with no electrical change — flips the simulated output from `0x5` to `0xA`, because bus-conflict resolution is "first active driver in net order," i.e., positional/order-dependent rather than physically grounded. #329 criterion 5 already pre-concedes that contention reporting may ship "explicitly unimplemented" if the engine can't back it up, and #396 confirms this is likely (C6 depends on #341/#387's strength lattice). So the feature whose entire pitch is *"a contention the schematic hides becomes visible"* may land with contention detection off, leaving the delivered feature as "second canvas + a consistency checker that can find split/merged nets and unplaced parts" — materially less than the abstract promises. That's not dishonest (the issue is explicit about the escape hatch), but it is a real risk to flag: the headline capability is the most likely one to be cut.

**Recommendation:** Track C6's landed/unimplemented status as a first-class fact on #329's close-out, and don't let "physical binding" in the title imply contention detection shipped unless it actually did.

### 6. [Low] Minor citation imprecision

`CircuitOp.java:34-37` is quoted as spanning "over 21 files in the package" — the package (`src/jls/collab/op/`) contains exactly 21 `.java` files, not "over 21." Trivial, but in an issue whose entire rhetorical style is "every number is checked," an inexact quantifier stands out. Not worth blocking on.

## What's solid

- All load-bearing code citations check out exactly against the working tree: `WireNet.java:19-22` (the determinism-comment + `LinkedHashSet` field), `CircuitOp.java:34-37` (the sealed permits list, verbatim), `PaletteContractTest.java:48` (method name) and its `NON_PALETTE_TAGS` set at `:44-45`, `Palette.java:123-188` (32 entries counted exactly), `pom.xml:408` ("jls.edit is deliberately unfloored" verbatim), and `SimpleEditor.java` at 5,852 lines. This issue's evidence discipline is genuinely good where it's checkable.
- The scope boundary against #84, #91, #162, #167, #232, #78 is coherent and each cited issue's relationship is accurately characterized (spot-checked #84: open, correctly described as owning the SimpleEditor decomposition residual, not this feature's job).
- The three "boundary note" comments distinguishing #329 from #331 and from #401/#396 are careful, well-reasoned deduplication work and correctly identify real near-duplicates without merging genuinely distinct outcomes.
- The formal net-partition / consistency-check algebra (§3) is internally consistent and matches what #396 actually implements against.

## Verdict rationale

`needs-rework`, not `should-not-proceed`: the technical substance is sound and the code grounding is accurate, but the issue is currently self-contradictory about its own decomposition state (finding 1), carries an unticketed cost gap of 2-4x (finding 2), and has a blocking architectural gate that its own child already sidestepped (finding 3). These are process/bookkeeping defects rather than design flaws, but they are exactly the kind of defect that causes a fleet of dependent issues (#401, #396, and eventually #297) to schedule against a graph that doesn't reflect reality. Fix the roster/DAG sync and name an owner for the residual before treating this issue as ready.
