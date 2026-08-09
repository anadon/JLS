# Issue #599: FEAT-C38-3: the Basys-3 question gets a written answer — supported with its toolchain named, or refused with the cost arithmetic — so the board the ASEE courses actually own stops being unaddressed
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#599 is filed `tier:feature` and has two children already in flight — #645
(TASK-C599-1, the decision document) and #647 (TASK-C599-2, the code/recipe
consequence) — both of which explicitly declare `part_of_feature: 599` in
their own machine blocks. But #599's own body does not follow this
repository's `feature.md` template at all, fails that template's own test
for being a feature rather than a label, states its motivating premise as
settled fact when the repo's own research doc says the opposite, and leans
on two "compliance" artifacts (a D8 cost table, KC-38-1) that do not exist
anywhere in this checkout. Every one of these problems was independently
found for the child task #645 by a sibling review
(`issue-reviews/issue-0645.adversarial.md`); this review shows they
originate at the feature level, not just in one task's wording.

## Findings, by severity

### 1. (High) #599 fails its own repository's structural test for being a feature at all

`.github/ISSUE_TEMPLATE/feature.md` states plainly: *"A feature is not a
folder. It must assert at least one integration criterion... and its
machine block must name at least one child in `requires_tasks` or
`planned_tasks`. Failing either test, it is a label, not a feature — do
not file it."* #599's machine block is:

```yaml
feat_id: FEAT-C38-3
serves_capstones: [522]
band_mw: "2-4"
ordering_after: [264, 416]
```

There is no `tier: feature` key, no `requires_tasks`, no `planned_tasks`,
no `blocked_by`/`blocks` (the schema's actual ordering-edge field names),
no `related`, no mermaid graph, no §1 Capability Statement & Scope
Boundary, no §2 Decomposition & Rationale table, no §3 Feature-Level
Interface & Data Contract, no §4 Global Invariants, no §5 Integration
Criteria & Evidence Plan, no §6 Sequencing & Parallelism, no §7
Re-planning Protocol, and no Completion Criteria checklist. The body is
instead shaped exactly like the `scientific_task.md` template (Outcome /
Acceptance criteria / Boundary and reference notes). Compare #264, a
correctly-formed sibling feature in the same area (fetched for this
review): full machine block with `tier: feature`, `requires_tasks`,
`planned_tasks`, a mermaid graph, an explicit "ordering-cycle walk"
paragraph, and all seven numbered sections plus a Completion Criteria
checklist. #599 is not a smaller version of that shape; it is a different,
task-shaped document wearing a `tier:feature` label.

**Recommendation:** either refile #599 against `feature.md` in full (name
#645/#647 in `requires_tasks`, add the missing sections) or relabel it
`tier:task` and fold its content into whichever of #645/#647 actually owns
the decision.

### 2. (High) The composition edge is one-directional — #599 doesn't know it has children

#645's machine block reads `part_of_feature: 599        # FEAT-C38-3 — the
Basys-3 question gets a written answer`, and #647's reads the same. But
#599's own body never cites #645 or #647 by number anywhere — not in the
YAML block, not in prose, not in "Boundary and reference notes" (which
does cite #416, #264, #59, and KC-38-1 by number, but not its own
children). The template is explicit that this is not cosmetic: *"the
task's `part_of_feature` field is authoritative, and a roster that
disagrees must REPLAN."* `issue_read get_comments` on #599 returns an
empty list — zero comments, so no REPLAN reconciling this has happened.
GitHub's own `has_children`/`has_parent` flags on #599 are both `false`,
confirming the relationship exists only as unlinked prose inside the
children, not as a tracked structural edge anywhere.

**Recommendation:** add `requires_tasks: [645, 647]` to #599's machine
block (or `planned_tasks` entries resolving to them) and open a REPLAN
comment stating the reconciliation, per the template's own rule C.

### 3. (High) The Outcome states the demand premise as settled fact; the repo's own research says it is an open question

#599's Outcome: *"the board the ASEE-documented courses actually own"* —
repeated in the title itself (*"the board the ASEE courses actually own
stops being unaddressed"*). Grepping the full tree for "ASEE" turns up
nothing outside `issue-reviews/`; no course roster, survey, or citation
exists in this checkout. Meanwhile
`docs/standards-adoption/OPEN-QUESTIONS.md:119` explicitly records *"Which
boards are actually wanted"* as an unresolved open question and calls
Basys 3, DE10-Lite, and ULX3S merely **"illustrative"**.
`docs/standards-adoption/06-fpga-constraint-formats.md`'s own go/no-go
section says: *"Do NOT do this if: No user has asked for a specific
board... The correct trigger is a course or a user naming a board they
own."* So the single research document in this repo that actually studied
board demand contradicts the premise the feature's title asserts as given.
This is not merely a wording nit carried down from the child task (already
flagged at `issue-reviews/issue-0645.adversarial.md` finding 1) — it is
baked into the *feature-level* charter and its title, meaning the reason
this feature exists at all is an unverified claim stated as fact.

**Recommendation:** rewrite the title and Outcome to state the
course-ownership claim as something to verify (or drop), and cite
`OPEN-QUESTIONS.md:119`'s existing open-question framing instead of
overriding it silently.

### 4. (High) AC-2/AC-5 depend on artifacts that don't exist in this repository

AC-2 requires the refusal branch's arithmetic be "per the D8 cost table,"
in "D8/D10-compliant form"; AC-5 says the decision names the boundary "so
KC-38-1 is not re-litigated per reader." Grepping the tree for `D8`, `D10`,
and `KC-38` (outside `issue-reviews/`) finds nothing but one unrelated
`#### D8. Editor UX` heading in `docs/capability-roadmap/lf-01-parameterization.md`
— a section-numbering convention in a different document, not a
cost-arithmetic schema or a keystone-conflict registry. No file defines
what "D8/D10-compliant form" means, and no file defines "KC-38-1" as
anything other than a bare label other issues (#522, #645) also assume is
already established. A reader who has only this repository cannot check
compliance with either. This is the same fabricated-citation pattern the
child task #645 was independently flagged for
(`issue-reviews/issue-0645.adversarial.md` finding 3) and that this fleet's
own review corpus has flagged repeatedly elsewhere (e.g.
`issue-0349.adversarial.md`, `issue-0453.adversarial.md`) — here it
originates at the feature that both child tasks inherited it from.

**Recommendation:** either point AC-2/AC-5 at a real in-repo cost model
(e.g. the reasoning-per-line sizing tables already in
`06-fpga-constraint-formats.md` or `11-costed-rejections.md`) or strike the
"D8/D10" and "KC-38-1" references and restate the requirement in this
project's own terms.

### 5. (Medium) AC-1's "supported" branch is satisfiable by naming a toolchain nobody has evaluated

AC-1 only requires the toolchain be named with "its version basis stated."
`openXC7` (the presumed open Artix-7 alternative to Vivado) appears
**nowhere** else in this repository: not in `docs/hdl-support-research.md`,
not in `docs/standards-adoption/06-fpga-constraint-formats.md` (which did
extensive, dated research on XDC/Vivado specifically and never mentions
it), not in `flake.nix`. A document could satisfy AC-1's letter with one
sentence naming openXC7 and a version number while never checking whether
it actually supports the Basys-3's exact part (`XC7A35T-1CPG236C`) or is
mature enough to trust — the identical gap already flagged for the child
task (`issue-0645.adversarial.md` finding 2).

**Recommendation:** require AC-1's supported branch to name what was
actually checked (part coverage, maturity, license), not just the
toolchain's name and version.

### 6. (Medium) #599 ignores the one existing in-tree design that already targets Basys-3, and could silently conflict with it

`docs/standards-adoption/06-fpga-constraint-formats.md` already contains a
detailed, Vivado-based implementation plan that names **Basys 3** as the
recommended first XDC board entry (its "Recommended first entries" list
and its 8–10 maintainer-day sizing table both price an XDC emitter +
Basys-3 entry explicitly), reasons in a Vivado-or-nothing world throughout,
and states outright: *"Vivado and Quartus cannot be in CI, and that is
final."* #599 never cites this document, even though it is the single
most detailed piece of prior art on exactly the board this feature is
about, and even though the visionary sibling review of #645
(`issue-reviews/issue-0645.visionary.md`) independently flagged that
section 06's Vivado-only framing is precisely the assumption that
generates every cost figure a refusal would cite. A decision document
produced under #599 could land a verdict that directly contradicts or
duplicates section 06's existing plan (e.g. picking openXC7 while section
06 has already sized and staged a Vivado/XDC path) without any acceptance
criterion requiring reconciliation between the two.

**Recommendation:** add an acceptance criterion requiring the decision
document to explicitly reconcile with (supersede, adopt, or narrow)
section 06's existing Basys-3/XDC plan, not just cite #416 and #264.

### 7. (Medium) `band_mw: "2-4"` is asserted with no independent derivation, and conflates two different cost regimes

The two children sum to the same range (#645: 0.5–1 mw, #647: 1.5–3 mw =
2–4 mw total), so the arithmetic is internally consistent, but that is
additive bookkeeping, not validation of the underlying estimate. The
capstone #522's own adversarial review (finding 7, quoted here for
convenience) already flagged that this same figure "conflates 'decide and
document' cost with 'build and demonstrate' cost inside one PF and one
band" and that it "silently assumes openXC7 ... is mature enough to avoid
the vendor tool — a nontrivial technical bet." #599 inherits that
ambiguity unchanged: #647's "supported" branch is priced the same whether
the underlying toolchain turns out to be Vivado (manual-only,
never-machine-validated per section 06) or openXC7 (potentially
CI-checkable, per the #645 visionary review) — two regimes with materially
different maintenance costs that this feature's single band number erases.

**Recommendation:** split the band into a bounded "decision memo" line
(what AC-1/AC-2 actually cost) and a separately gated line for the
technical bet (openXC7 due diligence), as the #522 review already
recommended one level up.

### 8. (Low) AC-1's discoverability test can fail on the issue's own spelling

AC-1 and the title both use the hyphenated "Basys-3." Every existing
in-repo reference (`docs/hdl-support-research.md`,
`06-fpga-constraint-formats.md`) spells it "Basys 3" (space, matching
Digilent's own product name) or "BASYS3." A document written in the
project's existing house spelling would not literally satisfy a search
for the string "Basys-3" as AC-1 specifies it.

**Recommendation:** state the search term spelling-insensitively ("Basys
3", "Basys-3", or "BASYS3").

### 9. (Low) `ordering_after` is not a field in this repo's own schema

#599 (and both of its children) use `ordering_after: [264, 416]`, but
`feature.md`'s schema names the ordering fields `blocked_by`/`blocks`, and
`scientific_task.md` presumably matches (the correctly-formed #264 uses
`blocked_by: []` / `blocks: []`). None of #599/#645/#647's machine blocks
would parse against the documented schema, which would break any tooling
that walks `blocked_by`/`blocks` edges the way the template's rule A
requires. This is a consistent convention across the whole C599 cluster
rather than a #599-specific slip, but #599 is the feature that should have
set the correct shape for its children to follow.

**Recommendation:** rename `ordering_after` to `blocked_by` across #599,
#645, and #647 in one pass.

### 10. (Low) The Logisim-Evolution `#91` citation is an unverifiable external reference

AC-5 leans on "KC-38-1" (see finding 4) which in turn (per #645) cites
Logisim-Evolution's issue #91 as "the recorded cost of owning vendor-tool
detection forever." That issue lives in a different GitHub repository
(`logisim-evolution/logisim-evolution`), was not reachable from this
review session, and is not summarized or verified anywhere in this
repository's own research documents. A reader confined to this tree cannot
check the claim it is supposed to back.

**Recommendation:** add a verified, dated summary of what that external
issue actually says to an in-repo research document before treating it as
a citable authority.

## What's solid

- The core instinct — that a documented, priced refusal is a complete and
  successful outcome, and unrecorded silence is not — is sound and matches
  this project's own practice (`docs/standards-adoption/11-costed-rejections.md`:
  "a rejection with no price on it is a shrug").
- Requiring the decision to state plainly what a Basys-3 owner can and
  cannot do with JLS *today*, independent of which way the verdict lands,
  is a genuinely useful, demand-independent deliverable.
- Explicitly fencing off #416's ECP5/ULX3S scope and #59's exported
  Verilog as untouched is the right scope discipline for a decision-level
  feature, and AC-4's "no golden of theirs moves" is a concrete,
  checkable non-regression bar.
