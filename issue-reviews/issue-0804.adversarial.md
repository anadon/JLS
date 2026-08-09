# Issue #804: TASK-C593-1: additive and subtractive selection and rubber-band over a mixed element/wire set, landing in the decomposed collaborators and nowhere near SimpleEditor
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for shift/ctrl-style additive/subtractive selection and mixed
element/wire rubber-band selection, gated to land outside `SimpleEditor`
(KC-37-1) and pinned by headless/#91-harness tests. The intent is reasonable
and the test-first, no-`SimpleEditor` constraints are well-formed as written.
But the issue's own dependency graph is self-contradictory across issues, one
acceptance criterion (AC-4) currently points at nothing that will actually
exist, and the fix already applied to it lives only in a comment rather than
in the body an executor or an indexing agent would read first.

## Findings, most severe first

**1. AC-4 depends on an ArchUnit rule that its own corrected predecessor issue does not commit to producing.**
AC-4 reads: *"asserted by the ArchUnit-style rule #441 establishes (KC-37-1,
not waivable at task level)"*. #441 was closed 2026-08-08 as a duplicate of
#84, and a same-day comment on #804 redirects AC-4 to read "#84" instead.
But #84's body (fetched directly, open) commits to a **GoF State-object**
extraction — "one object per state," a `State` interface, nine concrete
classes — and its Definition of Done (§14) has no line mentioning ArchUnit,
`transitionsTouchNoAwtOrSwingType()`, or any dependency-boundary test at all.
The ArchUnit rule AC-4 needs was #441's own deliverable (§8 bullet: *"Add
`src/jls/edit/MouseMachine.java`... `test/jls/edit/MouseMachineTest.java`:
... `transitionsTouchNoAwtOrSwingType()` (P4, ArchUnit per O11)"*), not
something #84 ever promised. Redirecting the citation from #441 to #84 does
not make the rule exist — it points AC-4 at an issue whose own text contains
no such commitment. AC-4 is therefore unassertable by anything currently in
the issue graph, correction included.
**Recommendation:** before #804 can close on AC-4, either #84's scope must be
amended to add the ArchUnit boundary test explicitly (with its own
completion-criteria line), or #804's AC-4 must name a different, real vehicle
for the "not-inside-SimpleEditor" assertion.

**2. The dependency graph is internally inconsistent about what blocks #84.**
The 2026-08-08 correction comment on #804 asserts: *"#84 is itself
`blocked_by: [440]`, because `jls.edit` is deliberately unfloored... The
full chain into this task: #440 → #84 → #804 → #593 → #521."* But #84's own
machine-readable header (fetched directly) states `blocked_by: []`, and its
body's supersession check says only the former #43/#74/#75/#167 holds "all
landed." #84's text never mentions #440. The `blocked_by: [440]` relationship
was true of **#441** (now closed/merged into #84), and the correction
comment infers it should transfer to #84 by association — but #84 itself was
never edited to record it. So one of two things is true: either #84 is
missing a real blocking dependency it should declare (a defect in #84, which
#804 is silently relying on), or the correction comment's inferred chain is
wrong and #804's actual prerequisite depth is different from what it claims.
Either way, #804 cannot be planned against a chain that the chain's own
nodes disagree about.
**Recommendation:** flag #84 for a `blocked_by` fix (or an explicit waiver
comment) before treating #804's corrected ordering as settled.

**3. Architecture mismatch between what #804/#593 describe and what #84 will actually build.**
#804's Outcome text (uncorrected) says selection logic is "driven through
#441's interaction machine," and #593 (the parent feature, also uncorrected)
repeats "driven through #441's `MouseMachine`/`InteractionState`." #441's
design was a single `MouseMachine` class with a `switch` **expression**
returning an immutable `Transition` record (§7.4 of #441). #84's design —
the issue that survives — is explicitly a **GoF State pattern**: nine
separate state objects implementing a shared interface (§7.4–7.5 of #84),
which is a different extraction shape (dispatch-by-polymorphism vs.
dispatch-by-switch-expression-in-one-class). These are not interchangeable
implementations from the caller's point of view: whether "decomposed
collaborators" means "call `MouseMachine.transition(...)`" or "call
`currentState.mousePressed(...)` on a `State` object" changes what #804's
"new selection state and logic" is supposed to plug into. Neither #804 nor
#593 has been updated to reconcile which shape #84 will actually deliver, so
an executor picking up #804 today has two contradictory architectural
targets to build against, only one of which (#84's) will exist.
**Recommendation:** #804 should name the concrete extension point once #84's
design is fixed, rather than citing "#441's interaction machine" (dangling)
or assuming #84 without checking its shape matches.

**4. AC-1's "resolution rule ... stated ... rather than left as observed behaviour" is satisfiable by documentation alone, not by correctness.**
AC-1 requires only that *some* resolution rule for additive/subtractive
selection be written into a catalog row — it does not say what that rule
must be. The Outcome promises behavior "the way every other editor taught a
switcher it behaves," but nothing in #804 pins the actual convention (e.g.
shift-click adds vs. shift-drag adds vs. ctrl/cmd-toggle, and — since JLS
ships on macOS per README — whether Ctrl or Cmd is the modifier on that
platform). An implementer could wire shift-click to *toggle* rather than
*add*, document that choice in the catalog row, and satisfy AC-1 to the
letter while contradicting the stated goal and every incumbent's actual
convention. The catalog row this defers to (#592/#803) is itself unpublished
(see Finding 5) and carries no baseline convention to inherit.
**Recommendation:** either #804 or the catalog row it depends on should name
the target convention (e.g. "shift-click/shift-drag = add, ctrl/cmd-click =
toggle, matching Digital/Logisim-Evolution") as a fact to implement against,
not just a fact to eventually record.

**5. #804 sits at the bottom of an unlanded, multi-hop dependency stack, and its cost band does not account for that.**
Verified against the live repo and issue tracker: `SimpleEditor.java`'s
selection code (`mousePressed`/`mouseDragged`, ~L2609–L2720 and ~L3570–L3627)
still contains the inline nine-state machine — no `MouseMachine`,
`InteractionState`, or GoF `State` type exists under `src/jls/edit/` today.
#440 (the plan + `jls.edit` coverage floor #84 needs first) is open and,
per its own body, not yet executed. #84 (the extraction #804 must land
inside) is open and, per its own supersession check, not yet started. #592's
parity catalog — the doc AC-5 requires every landed behavior to have a
scored row in — does not exist under `docs/` (checked directly; no
ergonomics/parity/selection catalog file is present). #803 (which defines
*how* rows get scored, including the acceptance-vehicle column AC-5
implicitly needs) is itself open. So #804's realistic critical path is
`#440 → #84 → (catalog exists via #592/#803) → #804`, three or four unlanded
issues deep, yet `band_mw: 1-1.5` prices only #804's own work with no
schedule-risk note for the stack underneath it.
**Recommendation:** either state the band as conditional on the prerequisite
chain landing, or note explicitly that #804 cannot be picked up in isolation
regardless of the ordering_after fix.

**6. The AC-4/Outcome fix lives only in a comment, not in the issue body.**
The body text an executor reads first — and what any indexing or search
tooling over open issues will surface — still says "#441's interaction
machine" in the Outcome and cites "#441" by number in AC-4. The corrective
comment is dated the same day and is easy to miss in a quick skim (it opens
"STALE REFERENCE — CORRECTION" but does not edit the body). A second-order
risk: the comment's own correction (Finding 1) is itself not fully accurate,
so even a careful reader who does find the comment ends up building against
a still-wrong target.
**Recommendation:** edit the issue body directly rather than relying on a
superseding comment; GitHub issues used as a durable task specification
should not require reading a comment thread to find the operative
acceptance criteria.

## What's solid

- AC-3 (test-first, must fail at the pre-change commit, no screenshot-only or
  manual-only proof) is concretely checkable and consistent with the
  project's established discipline (`#91`, `#84`'s own falsification
  criteria use the same pattern).
- The "nothing lands inside `SimpleEditor`" boundary (AC-4's intent, KC-37-1)
  is a reasonable, consistently-stated constraint across #316/#84/#593/#804
  even though its assertion mechanism (Finding 1) is currently unfixed.
- AC-2's core claim is less of a gap than the Outcome text implies: current
  `SimpleEditor.java` rubber-band selection (`mouseDragged`, ~L3592–L3622)
  already applies one containment rule (`Element.isInside`) uniformly to
  elements and wires via the spatial index, since `Wire`/`WireEnd` are
  `Element`s. The genuinely absent piece, verified by grep, is additive/
  subtractive modifier handling — no `isShiftDown`/`isControlDown` gates
  selection anywhere in the file's mouse handlers today (the one
  `isShiftDown` hit at L3924 is a scrollbar-orientation check, unrelated).
