# Issue #741: TASK-C544-3: signal-state changes are announced as they happen during simulation — or the reduced set is recorded as a named exception, never papered over
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#741 is the third task in the FEAT-C26-3 (#544) screen-reader chain: navigation
(#739) → this task (live/on-demand announcement). It reads as tightly scoped
and honest about its own fallback ("never papered over" appears twice across
#544 and #741). The problems are not in the prose's intent but in what it
assumes is already standing beneath it, and in one governance conflict with
the feature issue it belongs to.

## Findings (most severe first)

### 1. Critical — the task is scheduled on top of a foundation the codebase does not have, and #741 does not restate the correction already recorded on its own prerequisite

AC-2 requires announcements to go "through the same accessible model
TASK-C544-2 traverses; no second announcement channel exists." That accessible
model is #380's (static accessible scene: one child per element, roles,
wire relations). A verified grep against HEAD confirms it does not exist yet:

```
$ git grep -n 'public AccessibleContext getAccessibleContext' -- src/   → exit 1 (no match)
$ git grep -rn 'AccessibleRelation|getAccessibleChildrenCount|getAccessibleChild(' -- src/ test/   → exit 1 (no match)
```

This is not a new observation — an earlier adversarial review comment on
#739 (`issuecomment-5227338900`, 2026-08-08) already established the real
chain is **#316 → #380 → #737 → #739**, and that #739's own `ordering_after`
was wrong for omitting #380 and #737. #741's `ordering_after: [TASK-C544-2]`
(i.e. `[739]`) repeats exactly the same omission one link further down the
chain: it names #739 as sufficient without acknowledging that #739 was itself
flagged as not-yet-startable. An executor who reads #741 in isolation (as
task issues are meant to be read) has no way to know the ground under AC-2's
"the same accessible model" is unbuilt.

**Recommendation:** add `380, 737` (or simply `739` once #739's own
correction is resolved and closed) to `ordering_after`, and add a line
cross-referencing the #739 correction comment so the dependency is visible
without archaeology.

### 2. High — AC-3's fallback conflicts with #544's stated decision authority for the same event

AC-3: "If TASK-C544-1's spike found live announcement unreachable through
Swing, **this task instead delivers** the on-demand state query and the
reduced set is recorded by name as a VPAT exception for #547 to consume."

But #544 (the parent feature) already assigns that same branch elsewhere:

> "Live announcements unreachable through Swing → re-scope to navigation +
> on-demand state query, record the exception by name in the VPAT
> (FEAT-C26-5), **REPLAN #507** re-deriving whether §1 step 2 still holds."

#544 treats a failed spike as a feature-level re-scope event that forces a
REPLAN of the capstone (#507). #741 treats the identical trigger as a
task-level fallback it executes unilaterally ("this task instead delivers").
These are two different authorities resolving the same branch: does a failed
#737 spike stop at a REPLAN of #507 before any task proceeds, or does #741
just quietly reroute itself to the reduced scope and carry on? As written, an
executor could satisfy #741 to the letter while skipping the REPLAN #544
requires — or could stall #741 waiting on a REPLAN that #741's own text says
isn't needed. Compounding this: #741's `ordering_after` doesn't list #737 at
all, so nothing in this issue's metadata even makes the spike's outcome a
precondition for starting — the AC-3 conditional reads as informational, not
enforced.

**Recommendation:** either (a) have #741 explicitly defer to #544/#507's
REPLAN outcome and add `737` to `ordering_after`, or (b) if task-level
self-rerouting is intended, delete the conflicting REPLAN language from #544
so there is one authority, not two.

### 3. Medium — AC-1's "stated coalescing rule" is required to exist but not specified, and no test is named

Compare to sibling issues: #544 names `OrcaLabSessionTest` (CAP-26 AC-2),
#547 names `VpatCoverageTest` (CAP-26 AC-4), #739 names headless
traversal assertions in its own AC-3. #741 names **no test class or
method** for either AC-1 (coalescing) or AC-4 (zero-cost path). "A stated
coalescing rule" is satisfiable by writing literally any rule down — e.g.
"announce at most once per 10 minutes" — which would pass AC-1's letter
while producing an announcement path that is accessibility theater. Nothing
in the acceptance criteria ties the rule to a property a test can check
(e.g. "N toggles within window W collapse to exactly one utterance carrying
the final state").

**Recommendation:** name a concrete assertion, e.g. a headless test that
fast-toggles a signal and asserts the announcement queue emits exactly one
coalesced event per debounce window, mirroring the pattern #739 already
uses for headless assertion against the accessible model.

### 4. Medium — AC-4 ("costs nothing measurable") is an unfalsifiable zero-tolerance criterion

"With no assistive technology attached, the announcement path costs nothing
measurable in the simulation loop" has no stated benchmark, workload, or
threshold. Taken literally, no profiler reading is ever exactly zero, so the
criterion can never be definitively met — and conversely, "nothing
measurable" is trivially claimable by simply not measuring. `ARCHITECTURE.md`
documents a hot-plane rule for `jls.sim` core precisely because performance
claims there need to be pinned (`docs/grand-architecture.md` §6, cited at
ARCHITECTURE.md:341-358), but the interactive simulation loop this task
would hook into is `InteractiveSimulator`, which — contrary to
`ARCHITECTURE.md`'s own module map (line 60, "`InteractiveSimulator` (GUI;
...)" listed under `jls.sim`) — actually lives at
`src/jls/edit/InteractiveSimulator.java`, not `src/jls/sim/`. That the
architecture doc itself misstates where this loop lives is a small but real
signal that "the simulation loop" as a target for a performance guarantee is
less precisely pinned down in this repo than AC-4 assumes.

**Recommendation:** replace "costs nothing measurable" with a stated
budget and harness (e.g. "adds <X% to the #554 batch-suite timing golden
with no AT attached, gated by test Y"), and fix the `ARCHITECTURE.md`
module-location error separately so the target of the guarantee is
unambiguous.

### 5. Medium — "the focused element and its connections" is undefined and collides with an existing, differently-scoped feature

AC-1 scopes announcement volume to "the focused element and its
connections." The codebase already has a **Watch** mechanism — `Ctrl+W`
toggles a `ToggleWatched` op (`src/jls/edit/SimpleEditor.java:1690`,
documented in `docs/keyboard-a11y-verification.md` row "Ctrl/Cmd+W →
Watch") — that drives the existing trace-window UI
(`InteractiveSimulator`'s `traceMap`/`wireMap`, `src/jls/edit/
InteractiveSimulator.java:93-96`). That is JLS's current answer to "which
signals is the student attending to," and it is a *set* a student builds
up, not a single traversal cursor. #741 doesn't say whether "focused"
means: (a) #739's single keyboard-traversal cursor, (b) the existing
multi-element Watch set, or (c) something new uniting the two. These have
materially different implementations and UX — (a) implies exactly one
active announcement source that moves as the student navigates (and raises
the question of whether traversal is even usable while a simulation is
running), while (b) implies zero or many simultaneous sources selected
before the run starts. The issue's Outcome text ("changes to the signal
states a student is attending to") does not resolve this.

**Recommendation:** state explicitly whether "focused" reuses Watch, reuses
#739's traversal cursor, or is a new concept, and if new, add an AC for how
a student enters/exits it.

## What's solid

- The "never papered over" discipline (AC-3) — recording a Swing limitation
  as a named VPAT exception rather than silently shipping a smaller feature —
  is a sound, testable-in-spirit norm and consistent with #547's design.
- AC-2's "no second announcement channel" constraint is the right thing to
  demand in principle; it just currently points at a model that doesn't
  exist yet (see Finding 1).
- Keeping this task's scope narrow (announcement policy, not traversal or
  VPAT authoring) and delegating those to #739/#547 respectively is correctly
  boundaried and avoids scope overlap with its siblings.

## Verdict rationale

`needs-rework`: the core idea is fine, but the issue (a) is missing
declared prerequisites already proven necessary one link up the same chain,
(b) has a live authority conflict with its parent feature over who decides
what happens on a failed spike, and (c) carries two acceptance criteria
(coalescing rule, zero-cost path) that are stated as musts without a
falsifiable test — exactly the "verification could pass while the real goal
fails" pattern this review is asked to hunt for.
