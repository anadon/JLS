# Issue #427: TASK-0087: an 8-bit adder becomes two cascaded parts with one synthetic carry net in the IR, not one component no factory can build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## 1. (Critical) The cited evidence has already been retracted by the tracker itself, and independent verification confirms the code doesn't exist on master

The issue pins every observation to `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and its sole comment
(#427#issuecomment-5171410986, filed by the repo owner) says plainly: that commit "exists only on a branch that will not be merged and will be deleted," the real equivalent is `master@8288226`, and two of this issue's own anchors are branch-only:

> `src/jls/hdl/HdlExporter.java:428-478` — 34 cited lines absent from `master`
> `test/jls/hdl/HdlPolicyTest.java:392` — 1 cited line absent from `master`
> "on `master` the shape is three buckets (`EXPORTED` `:421`, `SKIPPED` `:431`, `TOPOLOGY` `:436`), there is no `REJECTED` at `:460`, and the test to copy has not been written."

I verified this independently against HEAD (`db5ddc86`, 2026-08-08): `src/jls/hdl/HdlExporter.java:419-450` has exactly three buckets — `EXPORTED`, `SKIPPED`, `TOPOLOGY` — with **no `REJECTED` set anywhere in the file** (`grep -n REJECTED` returns nothing), and `test/jls/hdl/HdlPolicyTest.java` has no method named `exportPolicyIsTotalOverTheElementRegistry`. Observation **O3** ("carries four buckets... `REJECTED` at `:460`") is simply false on the branch a contributor will actually check out, and the Method step "**Copy the shape and the test, not the contents**" points at a shape and a test that don't exist. A companion issue, #492 ("HDL export policy is not total over the element registry"), exists specifically because the real, three-bucket policy on master is *not* total — which directly undercuts O3's premise that there is a proven four-bucket totality pattern worth copying.
**Recommendation:** this issue needs its O1–O7 anchors re-derived against master before anyone picks it up, exactly as rule 6 in its own checklist demands — but that re-derivation has not happened yet, so the issue as filed is not safe to hand to an implementer today. (O1, O2, O5, O6 did check out correctly against master — `Adder.java:33/261`, the absence of `jls.pkg`, `Element.java:619`, and the 35-entry/9-non-decomposable registry census all verified.)

## 2. (High) The task cannot start — and says so — yet nothing marks it blocked in a way a triager would see before assigning it

`blocked_by: [400]` and #400 (TASK-0085, the `Cascade`/slice-width schema) is **open, not merged**. The issue's own Method step 1 is "Confirm **#400** has landed... or record waivers per rule 10," and Open Questions #2 and #3 are both marked "**Blocks execution**." So by the issue's own admission, none of the implementation steps in §8 can be responsibly started: there is no `Cascade` record to read a slice width from, no decided collision-proof naming codomain (Q2), and no decided residue-tie data source (Q3). A second prerequisite, TASK-0007 (the single net-partition-walk extraction), is described as "**NOT YET FILEABLE AS AN EDGE**... being filed concurrently; a link pass adds it" — it has since been filed as #468, but #427's `blocked_by` list was never updated to include it, so the dependency graph in the issue is already stale relative to the tracker state a reader can see today.
**Recommendation:** either close this as not-ready and re-file once #400 and #468 land, or explicitly re-label it "blocked / do not assign" until the two Open Questions are resolved. As filed, a triager skimming the YAML and title could easily hand this to an engineer as "ready" work.

## 3. (Medium) "Names the `-parts` escape" is a load-bearing, undefined term

O6, §7.11, §7.4, and the Definition-of-Done all require refusal diagnostics for the nine non-decomposable types to "name the `-parts` escape" (e.g., "each verified present in the registry list... the diagnostic must name the `-parts` escape rather than saying no"). I searched the CLI flag table (`JLSStart.java`) and `docs/batch-interface.md` for `-parts`: **it does not exist anywhere in the codebase or docs.** Nothing in this issue or its cited dependencies (#400, #394) defines what the `-parts` escape actually is (a CLI flag? a `PartBinding` API? a GUI dialog?), so a completion-criteria checkbox ("nine non-decomposable types are refused with a diagnostic naming the `-parts` escape") can be satisfied by any string containing the substring `-parts` — including one that names a mechanism that was never built. This is exactly the kind of gameable acceptance criterion the adversarial lens is meant to catch: the check as worded verifies text content, not that the named escape hatch is real or usable.
**Recommendation:** either point `-parts` at a concrete, already-specified interface (in #400 or #394) before this issue is worked, or replace the criterion with one that asserts the escape mechanism exists and is exercised by a test, not just quoted in a message string.

## 4. (Medium) Heavy, unacknowledged overlap with sibling issues risks duplicate or conflicting work

#492 ("HDL export policy is not total over the element registry: an element type nobody classified is rejected with no reason") and #873 ("every element JLS can simulate but refuses to export gets an export or a permanent, reasoned refusal — Memory, RegisterFile and FieldExtend leave the reject bucket or state why they never will") both target essentially the same problem this issue's O3/O6/P7 lean on: totality of a registry-keyed classification table, and actionable refusal messages for exactly the same two element types (`Memory`, `RegisterFile`). None of the three issues cross-reference each other in their Related Work sections (#427 lists 394/336/349/329/366/315/232, but not #492 or #873). If #492's fix changes `HdlExporter`'s bucket shape or diagnostic wording before #427 is implemented, §8's instruction to "copy the shape" points at a moving target a second time.
**Recommendation:** add #492 and #873 to Related Work and resolve ordering/ownership explicitly, or this issue's diagnostic-wording assumptions will drift again exactly as they already have once.

## 5. (Low-Medium) Registry stability assumption undercut by a concurrent issue

O6 treats the current `ElementRegistry` (35 types, 9 non-decomposable) as settled ground truth for P7's totality test. But #488 ("JLS writes two element tags — FieldExtend and RegisterFile — that its own frozen tag table and its own normative file-format spec both say do not exist") — open concurrently — calls the registry status of two of those exact nine types (`FieldExtend`, `RegisterFile`) into question at the save-format level. This doesn't invalidate O6 (I confirmed both are present in `ElementRegistry.java` today), but it means the "nine non-decomposable types" list this issue's decisions are keyed to may itself be in flux from an unrelated, unreferenced issue.
**Recommendation:** note the dependency on #488's resolution, or accept the risk explicitly.

## 6. (Low) Scope: the spec is large relative to what #400/#394 have actually delivered

Sections 6–14 commit to a fairly large new subsystem (`jls.pkg`, three new IR types, a data-driven decomposition table, a provenance model, family-substitution awareness) gated entirely behind two other large, unlanded specs (#400, #394) in a tracker that already has 130+ open "TASK-/FEAT-" issues of similar structure. There's real risk of building an elaborate IR against a `Cascade` record whose shape isn't finalized (Falsification H1 explicitly anticipates the record needing to change for look-ahead chains), meaning rework is already priced in by the issue's own falsification criteria — which is honest, but also means "sound design" here is contingent on a schema that doesn't exist yet.

## What's solid

- The core arithmetic (§7.10, P1) is simple and directly falsifiable — slice count and synthetic-net count are pure functions of `(w, k)`, and the conservation law (P8/H3) is a good, hard-to-game invariant.
- Explicitly scoping out timing (§10, O1) and `.jls` format changes (§7.12.1, O7) is correct and prevents obvious overreach.
- The residue-tie requirement (P5, "tied not omitted") reflects real hardware failure modes (floating TTL inputs) and is concretely testable.
- Ordering risk against #394's goldens (§7.12.5) is called out honestly with a recommended default, rather than left implicit.

## Bottom line

The issue is well-formed in structure but two of its load-bearing evidence citations are already known-wrong per the project's own retraction comment, its own prerequisite (#400) has not landed, its own Open Questions are marked "blocks execution" and unresolved, and one of its completion-criteria phrases (`-parts` escape) refers to a mechanism that doesn't exist anywhere in the repo. This needs the citations re-derived and the blocking dependencies actually closed before it is workable; as filed it should not be picked up.
