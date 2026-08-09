# Issue #626: TASK-C559-4: CircuitVerse's queue-priority delay imports flagged, never presented as equivalent — the constructs that only look preserved get named
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#626 is the fourth of five tasks (TASK-C559-1..5 = #621/#622/#624/#626/#628)
under feature FEAT-C29-3 (#559, CircuitVerse `.cv` import), itself under
capstone CAP-29 (#513). Its job: when the importer maps CircuitVerse's
`delay` attribute onto a JLS delay, don't let the resulting circuit pass as
equivalent — emit a `mapped-with-caveat` report entry saying the source
semantics were queue-priority, not propagation delay, and extend the same
treatment to any other CircuitVerse construct with the same "looks preserved,
isn't" failure shape. The core motivation is real and grounded: JLS's own
delay model is genuinely transport-delay-based
(`docs/simulation-semantics.md` §6.2, §7 — "all delay in JLS lives in
elements", "transport delay: a pulse narrower than the propagation delay is
not swallowed"), so mapping a queue-priority scheduling attribute onto it is
a real semantic mismatch, not a manufactured one. The boundary note
correctly declines to close that gap in JLS's simulator. But the issue has
one open-ended acceptance criterion masquerading as a bounded one, a cost
band that doesn't account for it, and a dependency chain that is unbuilt
several layers down without that being surfaced here.

## Findings, most severe first

### 1. [HIGH] AC-3 is an unbounded research task wearing a checkbox

> "AC-3: A survey of the remaining CircuitVerse constructs for the same
> failure shape lands as a written list, and each one found gets the same
> treatment — the `delay` attribute is the first instance, not the
> definition of the class."

This asks for an audit of *every* CircuitVerse construct (there are roughly
two dozen element types plus subcircuit/wire/label constructs) against a
failure shape defined only by example ("translates into something JLS
accepts and simulates, but does not mean the same thing"). No methodology is
named (read CircuitVerse's source? its own bug tracker, as #559's sibling
work at #510 §3 already did for `delay` via CircuitVerse issue #1412? a
manual side-by-side of every element's simulation model?), no source of
truth is pinned, and no completion criterion exists beyond "a written list."
An implementer can check three obviously-fine gates, write "found nothing
else," and AC-3 passes — while a real divergence (e.g., CircuitVerse's
`SequentialCircuit` delta-cycle ordering, its documented subcircuit-node
disconnect bug cited in #559's own review corpus, or its clock/reset
initialization order) goes unflagged and there is no test that can catch the
omission, because "the survey found nothing" and "the survey missed
something" produce an identical fixture. This is the same class of problem
`docs/simulation-semantics.md`'s own testing discipline exists to prevent
elsewhere in this codebase, but AC-3 has no golden anchor at all.

**Recommendation:** either scope AC-3 down to "delay only" (matching the
task's title and the bulk of AC-1/AC-2/AC-4) and move the broader survey to
its own task with its own budget and a named methodology, or keep it here
but require a concrete source of truth (e.g., "cross-referenced against
CircuitVerse's own issue tracker for known behavioral-divergence reports, as
#510 did for `delay`") and a checklist artifact reviewers can audit for
completeness, not just existence.

### 2. [HIGH] The cost band doesn't reflect what AC-3 actually asks for, and the dependency chain is unbuilt three layers down

`band_mw: "1"`. In that budget the task must: build report-entry emission
against a shared schema (TASK-C556-1, #608) that is itself unbuilt (no
`src/jls/imp/`, no report infrastructure anywhere in this tree — confirmed
by `Glob`/`Grep`); consume the element-mapping table from TASK-C559-2 (#622),
also unbuilt and itself waiting on TASK-C559-1 (#621, JSON parsing), also
unbuilt; produce and defend a fixture; *and*, per Finding 1, conduct an
open-ended construct survey. #556's own review in this corpus already flags
sibling tasks in this exact family for under-costing against unbuilt
prerequisites (see `issue-reviews/issue-0556.adversarial.md` finding 5, and
`issue-reviews/issue-0628.adversarial.md`/`issue-0633.adversarial.md` on the
same pattern for TASK-C559-5 and its cousin). #626 repeats the pattern with
no caveat: `ordering_after` names only TASK-C559-2 and TASK-C556-1, not the
fact that both of those are themselves gated on unlanded work (#621, #323,
#314), so the 1-mw estimate is priced as if two direct prerequisites are the
whole story.

**Recommendation:** state the transitive dependency explicitly (down through
#621/#608/#323/#314) and add the same "estimate assumes prerequisite
infrastructure already exists; re-cost if it doesn't" caveat #556's own
review recommended for that issue.

### 3. [MED] AC-1's "every imported ... delay value" is a universal claim verified by a single fixture

> "AC-1: Every imported CircuitVerse `delay` value produces a report entry
> ..."
> "AC-4: A fixture asserts a circuit using `delay` imports **and** reports;
> a version that imports silently fails the test."

AC-4 is a good, mutation-resistant test in isolation (a version that drops
the caveat-emission logic fails it — credit below), but it is *one* fixture
proving the behavior on presumably one or a handful of `delay`-bearing
elements. "Every imported ... value" is not established by that; it is
established by inspecting the code path (does the mapper call the report
emitter on every element with a `delay` field, or only the ones the fixture
happens to exercise, e.g., only gates and not registers/flip-flops if
CircuitVerse's `delay` attribute is per-element-type-scoped differently). A
single golden fixture that happens to cover the common case can pass while a
less-common `delay`-bearing element type is silently missed. This is the
identical shape of criticism `issue-0559.adversarial.md` (finding 2) raised
against the parent feature's AC-1/AC-2 — it recurs here uncorrected.

**Recommendation:** either require the fixture to cover every distinct
CircuitVerse element type that carries a `delay` attribute (enumerated,
not "a circuit"), or add a structural/reflective check that every mapper
branch touching a `delay`-bearing source type routes through the caveat
emitter, so coverage isn't fixture-dependent.

### 4. [MED] The `location` field this task needs has only been validated against non-JSON sources

AC-1 requires the report entry to name "the element, its location." The
location field's expressiveness is TASK-C556-1's (#608) job, and #608 AC-4
states its proof case is "the Falstad text format (#561) is the worked
example the document uses to prove that" — a line/token-oriented plain-text
format, not JSON. #626 would be among the first consumers needing a
JSON-shaped location (element index / net ID / JSON pointer into the `.cv`
document), and nothing in #608 or #626 confirms that shape was considered
when the schema was designed. This mirrors a concern already raised against
#608's shared schema in `issue-0633.adversarial.md` finding 4 ("designed
against Falstad as a hypothetical worked example, not against what
[the consumer] actually emits").

**Recommendation:** when #626 is actually picked up, verify #608's
`location` schema round-trips a JSON-document position before relying on it;
flag back to #608 if it doesn't.

### 5. [LOW] `area:sim` label sits oddly against a boundary note that explicitly disclaims touching simulation

The issue carries `area:sim` alongside `area:core`, but its own boundary
note says: "This does not attempt to reproduce CircuitVerse's queue-priority
semantics in JLS ... closing it would be a simulation-semantics feature and
is out of scope." The task only *reports on* a delay/simulation mismatch; it
doesn't touch `jls.sim`. Minor, but a contributor filtering by `area:sim`
for simulator work would be pointed at a reporting/import task with zero
`jls.sim` changes.

**Recommendation:** drop `area:sim`, or add a one-line note clarifying the
label reflects "about simulation semantics" rather than "changes simulation
code."

## What's solid

- The core technical claim is correctly grounded: JLS's delay model really
  is transport-delay-based (`docs/simulation-semantics.md` §6.2/§7,
  `DelayGate.java`), so a queue-priority-to-propagation-delay mapping really
  is the "runs and is subtly not the student's" hazard the issue describes —
  not a strawman.
- The boundary note is precise and correctly scoped: it explicitly declines
  to reproduce CircuitVerse semantics in JLS, keeping this a
  reporting/honesty task rather than a simulation feature — the right
  boundary, and consistent with #559 AC-3's identical framing.
- No contradiction with sibling task #622 (TASK-C559-2): #622's own boundary
  note says "The queue-priority `delay` attribute is deliberately not
  handled here ... TASK-C559-4 owns saying so," which matches #626 exactly —
  the ownership split between the two tasks is clean and mutually
  consistent (verified by reading #622 directly, not just #626's citation).
- AC-2's use of the closed `mapped-with-caveat` vocabulary value (rather
  than free text) is correctly inherited from TASK-C556-1 (#608) AC-1's
  defined vocabulary (`mapped` / `mapped-with-caveat` / `refused` /
  `dropped-by-design`) — a grading script really can branch on it once #608
  exists.
- AC-4's fixture design ("a version that imports silently fails the test")
  is a sound mutation-testing framing, not just an existence check — good
  practice given Finding 3's scope caveat.
- The AC-1 citation to "FEAT-C29-3 AC-3" checks out: #559's actual AC-3 text
  ("documented as not semantics-preserving ... imported, but flagged, never
  presented as equivalent") matches what #626 restates, with no drift.
