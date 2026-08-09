# Issue #472: TASK-0035: a net, a group and a nested instance can be named — one addressing key that stays unique when a subcircuit definition stops being copied per instance
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-formed as a spec (grammar, transformations, failure modes, falsification
criteria all present and largely internally consistent), and its core observations about the
current tree check out against `master` — but its evidentiary basis is partly false, one of its
two real dependencies is undeclared, and it bundles four separable changes (a new type, a format
change, a semantics change to a uniqueness check, and a signature change touching 9 call sites)
under one "task" with a self-certified, hard-to-audit completion checklist.

## Findings, most severe first

**1. (Critical) O5's cited counter-advance guard does not exist on `master`; the issue already
carries a maintainer comment saying so, and independent verification confirms it.**
O5 quotes `ElementId.java:268-283` showing a `NEXT_COUNTER.getAndUpdate(...)` guard inside `parse`
and says "**Net ids must go through this same path**, or a second run of one install can re-mint
an id the file already declares." I read `src/jls/elem/ElementId.java:245-269` on `master`
(`8288226`) directly: `parse` ends at line 268 with `return new ElementId(replica, counter);` —
there is no counter advance anywhere in the method. The guard is branch-only code from the deleted
`evidence_commit` (`2d0ca9d`); it is the *proposed* fix in issue #491 ("`ElementId.parse` never
advances the creation counter..."), which is open and unlanded. Issue #493 (the tracker's own
evidence-pin correction) independently lists #472 among the 29 issues that are "**Wrong about
`master`** — quotes or relies on branch-only code." **Recommendation:** strike O5's "this same path
already exists" framing; either fold #491's fix into this task's own scope, or add #491 to
`blocked_by`.

**2. (High) `blocked_by` is incomplete — #491 is a real prerequisite and is not listed.**
The YAML dependency block lists only `blocked_by: [468]`. But per finding 1, this task's own
Materials section says net-id minting "must go through this same path" as element ids, and its P11
("A net id parsed from a file advances the process counter") is only satisfiable if that guard
exists. Since it doesn't exist on `master` and #491 is the only issue that adds it, #472 has an
undeclared blocker. As filed, an executor following the Method checklist (which checks only "has
#468 landed?") could implement net-id minting by copying `ElementId.parse`'s *current*, buggy
behavior — reproducing #491's exact defect for net ids: a second run of one install could re-mint a
net id the file already declares, and JLS would write a `.jls` file it then refuses to reopen.
**Recommendation:** add 491 to `blocked_by`, or make P11's guard part of this task's own diff.

**3. (Medium-High) The format-epoch coordination this issue itself calls a "real cost" is not
enforced anywhere.** §7.12 says goldens must be "coordinated with #436 and #437 so the format
epoch regenerates once, not three times — this is a scheduling obligation with a real cost, not a
nicety," but Open Question 2 resolves it as "whichever of the three lands first owns the
regeneration... Rides along" with no assigned owner, no blocking relationship, and no mechanism
that prevents three independent PRs from each regenerating the same golden corpus. The stated risk
is not mitigated by the stated design — only by hoping maintainers coordinate out-of-band.
**Recommendation:** either make this task `blocked_by` (or explicitly ordered against) #436/#437,
or accept and state plainly that triple regeneration is possible.

**4. (Medium) Gameable completion criteria via self-attestation.** Most of the 20-item Definition
of Done ("recorded in the PR," "outcome... recorded," "any deviation recorded as an issue
comment") is unenforced by any test or tool — it depends on the executor's own honesty and
diligence to report accurately. Concretely gameable examples: "No changes outside the scope of
section 8 (Method)" has no diff-scoping check; "the diff contains only added `nid` lines" (Threat
T1's mitigation) is described as a "spot-check diff," i.e., manual and sampling-based, not an
assertion the test suite runs. A PR could regenerate goldens with an unrelated behavioral change
folded in and the checklist would still be tickable as long as the author claims to have looked.
**Recommendation:** turn at least T1's spot-check into an automated assertion (e.g., a script that
diffs old/new goldens and fails if any line other than an added `nid` attribute changed) rather
than leaving it to PR narrative.

**5. (Medium) Four separable changes bundled as one task, raising blast radius and rollback cost.**
The task ships, in one PR: (a) `jls.core.ItemKey` (new type, no dependency on anything else), (b)
persisted net identity (`nid`, a format change touching every net-bearing file), (c) derived group
identity (a distinct data-modeling decision, see finding 6), and (d) migrating the load-time
uniqueness check plus changing `Ops.resolve`'s signature across all 9 current call sites
(`RemoveElements`, `RemoveWire`, `RotateElement`, `SetElementConfig`, `ToggleWatched`, `AddWire`,
`AttachProbe`, `FlipElement`, `MoveElements`, confirmed by grep). Any one of (a)-(d) could land and
be verified independently; as filed, a defect or a stalled Open Question in one (e.g., Open
Question 1's view-token-set bikeshed, which "blocks execution of the spec edit") stalls the whole
task, including the parts that have no such dependency.

**6. (Low-Medium) Group identity has the exact durability hazard H2 argues against for nets, waved
away by an unverifiable "no consumer" claim.** H2 argues nets must mint-and-persist an id rather
than derive one, specifically because a derived id "changes when that member is deleted, so an op
recorded against the net... silently retargets." H5 then gives groups a *derived* id (`min` over
member net ids) with precisely that property — deleting the lowest-id member net silently renames
the group. The issue's own falsification criterion for H5 acknowledges this ("refuted if group ids
must be stable across a member net's deletion — i.e., if some consumer binds to a group") and
resolves it by asserting no such consumer exists yet. That's true today (`jls.elem.Group` is
unrelated, per Threat T2), but it is exactly the kind of assumption H2 says not to trust for nets,
applied inconsistently to groups because the near-term cost of being wrong is currently zero. If
#383 (per-view geometry) or a future probe-binding feature ever binds to a fused group rather than
a net, this becomes a second identity-stability bug with the same shape as #491.

**7. (Low) Central justification is speculative infrastructure for a feature that doesn't exist
yet, and may need to change again.** The motivating claim — "bare `sid` uniqueness is an accident
of copying... exactly what shared definitions will remove" — depends on #447 (TASK-0041, shared
subcircuit definitions), which is open and unimplemented; I confirmed it is a substantial,
independent, unlanded task. #472 is not `blocked_by` #447 (correctly listed as `related` only,
since the grammar should exist before the feature needs it), but H1's own falsification criterion
concedes the grammar may be wrong: "the key needs a further component (most likely the definition
id)... **before anything persists it**." Landing a permanent format attribute (`nid`) now, ahead of
the feature whose actual shape would validate the grammar, risks a second format churn/epoch bump
shortly after #447 lands, which is a cost this issue does not budget for.

## What holds up

- O1-O4 (no `ItemKey` type exists, `Wire.save` is a documented no-op, `Ops.resolve` is a flat
  single-circuit scan, and the uniqueness check is textually per-`CIRCUIT`-block) all check out
  verbatim against the current tree (`src/jls/elem/Wire.java:119-126`,
  `src/jls/collab/op/Ops.java:33-43`, `docs/file-format.md:394-396`).
- The "no `FORMAT` bump needed" analysis is applied correctly against the format's own stated rule
  (`docs/file-format.md:427-437`), which I verified matches the quoted text.
- P3-P10 are concrete, mechanically checkable unit-test predicates, not vague aspirations — the
  strongest part of the issue.
- The empty-instance-path backward-compatibility argument (H4) is sound and the flat case costing
  nothing is a reasonable, checkable claim.
