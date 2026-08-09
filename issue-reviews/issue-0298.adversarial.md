# Issue #298: CAP-05: a drawn circuit leaves JLS as a netlist KiCad imports with zero hand editing, and the board built from it comes back working
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue is

A capstone specification (one of ~19 `tier:capstone` issues) demanding that JLS
export a KiCad-importable netlist for a hand-drawn circuit, that the resulting
board pass DRC, get fabbed, and — as a literal acceptance criterion (AC-7) — get
populated, clocked, and photographed working. It composes eleven other open
feature issues (#315, #318, #319, #334, #336, #339, #341, #349, #358, #365,
#366) and carries an elaborate self-auditing apparatus (machine-readable
`blocked_by`/`blocks`, a mermaid DAG, kill criteria, a "REPLAN:" protocol).

## Findings, most severe first

**1. [Critical] The Definition of Done is currently unsatisfiable, and the issue's own comment thread already flagged this without the body being fixed.**
Every row of the Cost table cites a source file under `docs/plan/features/` or
`docs/plan/capstones/CAP-05-*.md`. Verified against this checkout: `docs/plan`
does not exist anywhere in the repo (`ls docs/plan` → "No such file or
directory"). The issue's own 2026-08-03T20:37:44Z comment confirms this
independently: "citations into `docs/plan/**`... cannot be re-pinned at all —
those 195 files do not exist on `master`." Yet the Definition of Done still
requires "Every cited evidence document and permalink resolves on the default
branch at close," and the Cost section's 46-78 mw headline number is entirely
sourced to those absent files. As written, this issue cannot be closed against
its own stated completion bar. *Recommendation:* either merge the branch that
carries `docs/plan/**`, or re-source every Cost citation to something that
resolves on `master` today, before any further work is scheduled against this
number.

**2. [Critical] Live "mirror obligations" point at a closed duplicate issue.**
`related` lists `#307`, and §3/§5/Definition-of-Done all say "any `REPLAN:`
touching #366, #349 or #365 must land on both #298 and #307," with ownership
"recorded on #297 and #307 as well." I fetched #307 directly:
`state: closed, state_reason: duplicate`. The issue's own 2026-08-04 comment
concedes exactly this ("Live mirror obligations onto a closed issue... the
next REPLAN here should retire the two-sided wording") but the retraction was
never applied to the body — the machine block, §3 and the DoD checklist still
read as if #307 is an active peer. Anyone executing this issue by its literal
text would try to post synchronized comments to a closed duplicate.
*Recommendation:* a `REPLAN:` comment (which the issue's own protocol calls
for) should strip the #307 mirror language from the body, not just from a
later comment.

**3. [High] AC-7 is an acceptance criterion that cannot be verified by CI, cheaply gamed, and expensive/slow to attempt on a project that repeatedly documents itself as single-maintainer.** `ARCHITECTURE.md:243`, `README.md:64` and `SECURITY.md:87` all independently describe JLS as a single-maintainer pedagogy tool. AC-7 requires a human to order a real PCB (~$30, "three weeks later"), populate it, clock it, and attach a photo plus `kicad-cli` output — with no specified chain of custody tying the photographed board back to a specific commit's `sap1-alu.net`/`sap1-alu.kicad_pcb` beyond "attached." "The board works" could be satisfied by any board that happens to add two numbers; the criterion as written does not bind the artifact to the repository state under review. This is exactly the "verification could pass while the real goal fails" pattern: automated AC-1 through AC-6 could all be green while AC-7 is quietly waived (the issue even has a `WAIVED:` escape hatch for exactly this), and the capstone's actual selling point — "a real board works" — is the one criterion structurally least likely to ever be exercised.

**4. [High] Scope and cost are large relative to the project's stated capacity, and the issue documents its own prior estimate as wrong by ~3x.** The Cost section states the earlier standalone figure of "15-23 mw" is "superseded" — the real sum of the eleven required rows is 46-78 maintainer-weeks, none of which is discounted for the two features (#315, #334) marked "residual"/partially shipped (finding 7 below). That is roughly a year of full-time-equivalent work gating a single capstone, on top of the eleven prerequisite issues, all still open and unstarted. Given the single-maintainer framing found throughout the repo's own docs, funding this exactly as scoped is a real feasibility risk the issue does not itself weigh against alternatives (e.g., shipping only AC-1/AC-2 and demoting AC-7 to aspirational, which Open Question wording elsewhere in the corpus gestures at but this issue does not adopt).

**5. [Med] KC-05-1's kill threshold depends on the same unreachable evidence file as finding 1.** The 9-of-35 "no cascadable realization" baseline and the 10-of-35-or-10-of-36 kill trigger are sourced to `docs/plan/evidence/capstone-plan.md:558-566` at commit `3a81a4a`, a commit/branch not present in this checkout and, per finding 1, not on `master` either. The 9-of-35 count is independently re-derivable against `ElementRegistry.java` (I re-ran it: `grep -c "new ElementType(" src/jls/elem/ElementRegistry.java` → 35, and the nine named types — `Display`, `FieldExtend`, `Memory`, `RegisterFile`, `SigGen`, `StateMachine`, `SubCircuit`, `TestGen`, `TruthTable` — are all present), so the baseline itself checks out, but the kill criterion's *trigger condition* still cites an unreachable document as its authority, which is the same defect as finding 1 in miniature.

**6. [Med] The Cost table's headline sum is not discounted for two rows the issue itself calls partially shipped.** #315 and #334 are each marked "residual" and "partially shipped" in §2, yet the Cost table prices both at their full corpus bands (1-2 and 2-4 mw respectively) and the printed 46-78 mw sum includes them undiscounted. The issue names this gap itself ("Open Question 6... No residual re-estimate exists in the corpus, so 46-78 mw is an upper bound") but the headline figure quoted at the top of §Cost and referenced by KC-05-4's 24 mw trigger is the inflated one, not the upper-bound-labeled one — a reader skimming the table gets the bigger, less accurate number.

**7. [Low] Hidden assumption baked into the walk-through as if it were a functional observation.** §1 step 5 states the board is fabbed "for roughly $30" with a "three weeks later" turnaround — a claim about a third-party fab market, not about JLS. It is harmless as color but is phrased inside the numbered "a reviewer executes this walk-through and observes each result" sequence, blurring an external economic assumption with the actual pass/fail steps (1-4, which are legitimately observable).

## What is solid

- The core technical premise checks out against the live tree, not just the
  stale `evidence_commit`: `grep -rniE "footprint|refdes|pinout" src/` returns
  0 hits today; `src/jls/JLSStart.java:780-781` is still exactly the quoted
  `-export` FlagSpec (HDL-only); `Put` is still `sealed ... permits Input,
  Output` (`src/jls/elem/Put.java:15-16`); `Element.java:21-24` still shows the
  file-local `id` reassigned every save alongside the permanent `stableId`.
  The issue's code citations are unusually well-grounded.
- The dependency-graph self-audit is genuinely rigorous: two separate comments
  independently re-derive the mermaid graph from each required feature's own
  filed `blocked_by`/`blocks` and catch six wrongly-drawn and seven missing
  edges — a level of internal consistency-checking most issues never attempt.
- The `examples/sap1-alu.jls` fixture gap is honestly disclosed rather than
  hidden (confirmed: `git ls-tree -r --name-only HEAD examples/` lists only
  `examples/autograde/autograde.py`), and the issue states plainly that
  producing this fixture is part of its own deliverable.
- The IR-layer-for-cascade-nets argument in §3 (KC-05-2) is sound engineering
  reasoning: an emitter-side cascade rule genuinely would make the partition
  round-trip check (AC-1 check 5) ill-defined, and sequencing #336 before
  #365/#366 in the filed graph is the right fix.
