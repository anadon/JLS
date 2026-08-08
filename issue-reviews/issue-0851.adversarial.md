# Issue #851: TASK-C370-6: the editor reads the flat state through a view proven in agreement, and per-edit cost and startup time do not regress — the criterion that can veto the whole feature
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

TASK-C370-6 is the last of six machine-generated child tasks of FEAT-054
(#370, "flat, primitive-typed array layout for runtime state"): #842
(measure), #843 (index+columns), #846 (migrate state into columns), #848
(index/queue agreement), #850 (spatial-index reconciliation), #851 (this
issue, editor reconciliation). It asks that `SimpleEditor` stop reading
`Circuit`/`Element` objects directly and instead read "the flat state or a
view provably in agreement with it," with a hard gate: any measured
regression in editor startup time or per-edit cost is refused, not traded,
per #370 invariant 3.

## Findings, most severe first

**1. AC-3's "measurement gate's method" does not exist for this metric anywhere in the dependency chain — the AC is currently unimplementable as written.**
AC-3 requires "startup time and per-edit cost … measured … by the
measurement gate's method." The measurement gate is FEAT-009 (#335). Its
own `planned_tasks` are all "not filed," it is `blocked_by: [353]`
(unfetched, FEAT-005), and its § 1 scope statement enumerates exactly what
it measures: "per-cycle active fraction, CPI and events-per-instruction,"
"ns/event and bytes/event," and golden byte-identity — engine-level
constants, with no mention of GUI startup time or per-edit interaction
latency anywhere in #335's body. The sibling task that does cite "the
measurement gate's method" verbatim, TASK-C370-1 (#842), measures only
per-element live heap on a generated design — also not editor timing. A
repo-wide grep for `startup time|per-edit|perEdit` in `src/`, `test/`,
`docs/` returns nothing. So no task in the entire #370 family ever defines
a method or a named fixture for editor startup/per-edit measurement, yet
#851 requires citing one that exists. Whoever picks this up must either
invent the method (at which point it is not "the measurement gate's
method") or block indefinitely on infrastructure this issue never orders
behind. **Recommendation:** either file the editor-timing measurement
method as an explicit task under #335 or #370 before #851 is picked up, or
rewrite AC-3 to say #851 defines its own method and names its own
fixtures, and say so plainly rather than borrowing #335's authority.

**2. `ordering_after` omits real prerequisites this issue's own ACs depend on.**
`ordering_after` lists only `TASK-C370-3`. Two dependencies are missing:
(a) #335 (the measurement gate), which AC-3 explicitly delegates to — see
finding 1; the ordering block should surface that dependency, not bury it
inside prose. (b) TASK-C370-4 (#848), the task that pins "one indexing
scheme … asserted by a test rather than held by convention." #851 builds
an editor-facing *view* directly on top of the per-element index; if that
index contract is not yet locked in by #848's structural test, the view
built here can be invalidated by whatever TASK-C370-4 finds (e.g. a
private mapping it forces to be removed), forcing rework of #851's output.
#370 itself only argues rows 3 (index agreement) and 4 (direct readers)
are *mutually independent of each other* once the layout (row 2) lands —
it never argues the editor's view is independent of the index contract
being test-pinned. **Recommendation:** add `#335` and `TASK-C370-4 (#848)`
to `ordering_after`, or explicitly justify why the view can be built
before the index contract is pinned.

**3. AC-3/AC-4's regression criterion is gameable in both directions — no tolerance, run count, or statistical method is defined anywhere.**
"A measured regression fails this task" and "raises a REPLAN" — but
"regression" is never quantified. Nothing in #851, #370, or #335 states a
tolerance band, a minimum run count, or a significance test. JVM
measurements (JIT warm-up, GC pauses, first-paint cost) routinely vary by
double-digit percentages run to run. As written: a single noisy run could
either (a) be dismissed as "not really a regression" with no criterion to
contest that judgment call, defeating AC-4's intent that regressions are
"not resolved by choosing friendlier fixtures/interpretations," or (b) a
genuine noise spike could trigger a REPLAN on #370 that — per #370 §4
invariant 3 and §7 — can refuse the *entire* 12-20 mw feature outright,
after #842/#843/#846/#848/#850 have already landed. That is a large blast
radius resting on an undefined threshold. **Recommendation:** specify N
repetitions, an aggregation statistic (e.g. trimmed mean/median), and an
explicit percentage band before this task starts, consistent with how
#335 itself insists elsewhere that "no number is published without its
clocking regime."

**4. AC-5 references a package, `jls.ui`, that does not exist in this codebase.**
"`jls.edit`/`jls.ui` remains the only AWT layer touched." `grep -rn
"package jls.ui" src` and a directory search both return nothing; the only
GUI layer named anywhere is `jls.edit` — confirmed both by
`ARCHITECTURE.md`'s module layout ("`jls.edit` — the editor") and by
`test/jls/HeadlessCoreRatchetTest.java`'s forbidden-import pattern, which
enumerates exactly `java.awt.*`, `javax.swing.*`, `jls.edit.*` and nothing
called `jls.ui`. As written this criterion cannot be checked against the
current tree — it either presupposes a package split no other issue in
this chain (#842/#843/#846/#848/#850/#370) proposes, or it is a copy-paste
artifact from an unrelated template. **Recommendation:** drop `jls.ui` or
justify its introduction; if the intent was just "the editor package,"
say `jls.edit` alone.

**5. AC-2's "asserts it cannot occur" overclaims what one test can establish.**
A single test that "mutate[s] runtime state, read[s] through the editor's
path without an explicit sync" and asserts no divergence demonstrates
absence of divergence for the mutated field/element type in that one
scenario — not general absence of a second representation across
`SimpleEditor` as a whole. `SimpleEditor` is 5,852 lines
(`src/jls/edit/SimpleEditor.java`) with dozens of direct model-read call
sites (`circuit.getElements()` at line 883/910, `circuit.elementsAt(...)`
at 2617/2672/3241/3347, `circuit.elementsNear(...)` at 364/1094/3592, and
more) spanning ~30 element types. A narrow, cherry-picked mutation test
could pass while other reads still go through a stale or partially-synced
path. This is the same "true when written, false six months later"
pattern #370 itself warns about for the *index* agreement (which is why
TASK-C370-4/#848 requires a *structural* check, not just a sampled test).
**Recommendation:** require either an architectural rule (no editor-side
field cache of model state, checked structurally like #848's AC-2) or a
parameterized test sweeping a representative sample of element
types/fields, not a single example.

**6. Cost-band plausibility.** `band_mw: "3-5"` for converting an
extensive direct-read surface (finding 5) into a provably-agreeing view,
writing the divergence test, and building an editor-timing measurement
harness essentially from nothing (finding 1) looks tight next to sibling
TASK-C370-3 (#846, the core migration itself, at "5-8" mw) and
TASK-C370-5 (#850, the spatial-index half of the same "direct readers"
row, at "2-4" mw) which has an existing, much smaller `SpatialIndex.java`
(242 lines) to rework versus `SimpleEditor.java`'s 5,852. Worth
re-checking the band once finding 1's missing infrastructure is costed
in.

## What is solid (no rework needed)

- The dependency on TASK-C370-3 (#846) is real and correctly stated:
  there is no flat state to build a view over until the migration lands.
- Including `HeadlessCoreRatchetTest` green in AC-5 is consistent with
  #370 invariant 5; the test exists and enforces exactly the AWT-boundary
  concern this task risks (`test/jls/HeadlessCoreRatchetTest.java`).
- The premise that the editor reads model internals directly is verifiable
  in the current tree: `Circuit.getElements()`
  (`src/jls/Circuit.java:432-435`) returns `Collections.unmodifiableSet`
  wrapping the live, mutable `Set<Element>` — the wrapper is immutable but
  the `Element` objects inside it are not, so the editor observes live
  model mutation exactly as #370 describes.
- The framing that this AC can veto the whole feature is a faithful,
  non-inflated restatement of #370 §4 invariant 3 and §7's re-planning
  protocol, not an invented escalation — the issue does not overstate its
  own authority here.
- AC-1's "no second representation kept in agreement by discipline" is
  directly grounded in #370 invariant 4 and Open Question 3 ("provably"
  must mean a test, not a comment), not a freestanding invention.
