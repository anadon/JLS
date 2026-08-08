# Issue #565: FEAT-C31-3: a typed or edited truth table becomes a drawn, laid-out two-level circuit whose extracted table round-trips identically
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The synthesis direction (table → circuit) is genuinely absent from the codebase and is the
correct half of CAP-31 (#515) to build. But the issue body as it stands today still contains
defects that two of its own review comments already diagnosed and proposed fixes for — the fixes
live only in comment prose, not in the issue's actual Outcome/AC text — plus additional problems
those comments did not cover: a broken formal issue-graph link to this feature's own task
decomposition, and an acceptance-criteria gap that lets an implementation dodge the minimizer
dependency the Outcome claims to require.

## Findings, most severe first

**1. HIGH — AC-3 is still unfalsifiable in the live issue body; the fix exists only as an unmerged comment.**
Quoted as filed: *"The generated layout is placed and routed legibly using the #62 heuristic-layouter
lineage **where available**; the drawn result is an ordinary editable circuit, not a locked
artifact."* `#62`'s own tracking issue shows its layout engine (`SchematicLayouter`,
`HeuristicLayeredLayouter` — confirmed present at `src/jls/hdl/layout/*.java`) is landed on master,
but the readability/timing rubric that proves it (#290: IC1–IC6, IC9, IC10) is listed "No — built by
#290" for every row. So "where available" resolves to "code exists, unverified against any legibility
bound" — an implementation that places every element at the origin and calls the landed (but
unproven) layouter would satisfy AC-3 as literally worded. Comment #5227255089 on this issue already
found this and proposed an AC-3a/AC-3b split (unconditional editability vs. conditional layout with a
`WAIVED:` disposition). That split is a good fix — but it is sitting in a comment, not in the issue's
AC list. **Recommendation:** edit the issue body to replace AC-3 with the AC-3a/AC-3b split before
anyone picks this up; a comment fix that never lands in the body will be missed by an executor who
only reads the current AC list.

**2. HIGH — no formal sub-issue link between this feature and its own task decomposition.**
`get_sub_issues(565)` returns `[]` and `has_children: false` on #565, yet #652 (TASK-C565-1, table
entry), #653 (TASK-C565-2, netlist construction), and #654 (TASK-C565-3, layout integration) each
declare `part_of_feature: 565` in their own YAML front matter — and `get_parent(653)` returns
`{"parent": null}`. The relationship exists only as prose convention, not as GitHub's actual
sub-issue graph. Any tooling, dashboard, or agent that walks the real parent/child API (rather than
grepping YAML) will see #565 as scopeless and its three tasks as orphaned — closure tracking and
"is this feature done" queries will be silently wrong. **Recommendation:** link #652/#653/#654 as
GitHub sub-issues of #565 (the `sub_issue_write` tool exists in this toolset), not just via
`part_of_feature` text.

**3. MEDIUM — AC-1/AC-2 can be satisfied without ever consuming FEAT-C31-2's minimizer, defeating the stated Outcome.**
The Outcome promises a circuit "two-level over the minimized form from FEAT-C31-2" (#564, Quine–McCluskey — confirmed unimplemented: `grep -rn "QuineMcCluskey\|Quine-McCluskey" src/` returns nothing). AC-1 only checks that the extracted table round-trips identically; AC-2 only checks "two-level" and "existing palette elements." Neither AC names a test tying gate/term count to FEAT-C31-2's specific minimized output. A canonical (unminimized) two-level SoP built directly from the truth table rows is still two-level, still uses only palette gates, and still round-trips identically on extraction — it would pass AC-1 and AC-2 as worded while silently bypassing #564 entirely, which is exactly the dependency the Outcome and `ordering_after` claim exists. **Recommendation:** add a criterion that golden-tests synthesized term/gate count against FEAT-C31-2's minimizer output on at least one non-trivial (don't-care-bearing) table, not just functional round-trip equivalence.

**4. MEDIUM — AC-4's input bound doesn't bound the thing AC-3's layout claim depends on.**
AC-4 bounds *table* size (input count, 2^N rows). But #62's layout rubric is explicitly scale-limited: IC10 covers "at least one deliberately large import near the ~150-element bound" and IC5's area threshold is reference-relative at that same scale. Nothing in #565 bounds *output* count or the resulting synthesized *element* count (inputs × outputs × minimized product terms can produce far more than 150 elements while N stays well under any sane input-count refusal threshold). A table that passes AC-4's refusal check can still synthesize a circuit outside the only scale #62's layouter has (or will have) been proven legible at, silently invalidating AC-3 for exactly the cases where layout quality matters most. **Recommendation:** either bound synthesized element count directly, or state explicitly that AC-3's legibility claim is only asserted up to #62's proven scale and refuse (per AC-4's own pattern) above it.

**5. LOW — dependency chain for AC-1 is two issues deeper than `ordering_after` shows, and none of it is built.**
The boundary notes point to FEAT-C31-1 (#563), which itself depends on the combinational-cone
extractor — which, per #872 (TASK-C563-0, filed the same day as this issue's review comments),
**did not exist as a filed component until that task filed it**: "No filed issue delivered that
extractor" (#872's own words) and `grep -rin "combinational\|cone" src/` at review time finds only
the shipped `TruthTable` element, nothing that extracts a cone from drawn logic. So AC-1's
verification path is #565 → #563 → #872 (cone extractor, unbuilt) and separately → #564 (minimizer,
unbuilt) → #653/#654 (this feature's own unbuilt tasks). This is normal for capstone-family planning
issues, but #565's own `ordering_after` list only names one hop ("FEAT-C31-1"), understating how much
has to land before this feature's demo-critical AC-1 is even testable. **Recommendation:** no action
required beyond awareness — flag at pickup time that #872, #564, and #563's own AC-1 must be green
first, not just "ordered after."

**6. LOW — citation inconsistency in the machine block.**
`ordering_after` lists `"#62 HDL-import heuristic layouter lineage"` (numbered) alongside
`"FEAT-C31-2 minimization (synthesis draws the minimized form)"` (unnumbered). FEAT-C31-2 does exist
(#564, confirmed via search), so this isn't a dangling reference, but the inconsistent citation style
costs a reader a search to resolve what a sibling entry gives for free. Minor; fix in passing.

## What's solid

- **AC-4's premise is correct**: `src/jls/elem/TruthTable.java` genuinely has no input-count ceiling today — `grep -n "MAX\|maxInputs"` returns nothing, and `addInput` (line 610) unconditionally doubles row count with only a duplicate-name check, no bound. Adding a bound here is real, needed work, not a restated no-op.
- **The "layout is reused, not rebuilt" boundary is correctly scoped and licensing-safe**: it defers entirely to #62's lineage, which has already settled the ELK/GPL-incompatibility question out-of-process; #565 introduces no new licensing surface by inheriting that decision rather than re-deciding it.
- **AC-5 (headless batch synthesis)** fits the project's existing batch-interface pattern (`-t`, `-i`, `-export` flags documented in README/`docs/batch-interface.md`) — a plausible, low-risk extension, not a new architectural surface.
- **The prior review comments (#5227255089, #5227514263) are substantively correct** on the Outcome-sentence bundling (shipped table entry vs. absent synthesis) and the #872 ordering re-point; I did not find grounds to dispute either.

## Verdict rationale

`needs-rework`: the core direction is sound and two real defects were already caught by prior review, but neither fix has been applied to the issue body itself (AC-3 as filed is still unfalsifiable), the sub-issue graph linking this feature to its own tasks is broken at the API level, and the acceptance criteria as written admit an implementation that skips the stated minimizer dependency entirely. None of this blocks the feature's premise; all of it blocks trusting the AC list as a pickup-ready spec.
