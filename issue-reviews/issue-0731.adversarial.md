# Issue #731: TASK-C542-2: thickness, dash and glyph carry wire state when colour carries nothing — a registry-keyed state-to-encoding map with a totality test
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

TASK-C542-2 (part of feature #542) wants redundant non-colour encoding —
thickness, dash, glyph — for "every distinct wire state — high, low, HiZ,
bus value, error", routed through a registry-keyed mapping with a totality
test and an injectivity test, holding the default theme pixel-unchanged,
built on `WireRenderer`/`WireEndRenderer` (#76) rather than a parallel
mechanism. The anchoring on existing code is accurate and the injectivity
requirement is well-designed, but the issue's own state inventory doesn't
match the codebase's documented value domain, it conflates two unrelated
"registry" concepts under one borrowed term, and its stated ordering
dependency on TASK-C542-1 (#729) is unenforced and unexplained.

## Findings, most severe first

**1. [High] The five-state inventory ("high, low, HiZ, bus value, error") does not match JLS's documented value domain, and "error" specifically contradicts a recorded design decision.**
`docs/simulation-semantics.md` §2 titles the domain "two states plus HiZ"
and §9 states plainly: "There is no wired-AND/OR and no conflict (X)
state" (line 439-440) — bus conflicts are surfaced once through
`TellUser` (a transient notification, re-armed when the conflict clears),
not as a persistent per-wire render state. So "error" as a fifth
*wire-render* state to encode does not exist in the simulator's own
normative spec, and building one would either contradict §9's recorded
decision or require a spec change this task never proposes. "Bus value"
fares no better: wires already carry an arbitrary `BitSet`
(`Wire.getValue()`), and `WireRenderer.strokeFor`
(`src/jls/edit/WireRenderer.java:43-55`) already collapses that to
exactly three cases — HiZ (`null`, dashed), non-zero (thick), all-zero
(thin) — with no separate "bus" branch; a multi-bit non-zero value is not
visually distinct from a single-bit `1`. The issue never defines what
"distinct wire state" means beyond the loose five-word list, so AC-1
("every distinct wire state has a non-colour encoding") is unverifiable
against a state set that doesn't demonstrably exist as five things.
*Recommendation:* replace the ad hoc list with the actual value domain
(§2/§9: driven-0, driven-1, HiZ — plus, if wanted, "bus conflict" defined
as the transient `TellUser`-reported condition, explicitly scoped as
notification-only, not a steady-state encoding) before any registry is
designed around it.

**2. [High] The "registry-keyed... FEAT-001 lineage" language and the Outcome's "a new element or wire state that lacks an encoding fails the build" conflate two unrelated extensibility axes — the same defect already flagged, unfixed, one level up in #542.**
The registry-with-totality pattern this task cites by name
(`ElementRegistry`/`ElementRegistryTest`) is real and keyed by *element
type*: `src/jls/elem/ElementRegistry.java:38-77` lists ~30 concrete
classes (`Adder`, `AndGate`, … `XorGate`), and its own javadoc (lines
16-22) describes totality over "every concrete `Element` subclass," not
wire value states. Wire states are a closed, fixed set drawn by one
class (`WireRenderer.draw`, lines 57-91) that does not grow when a new
element type is added — the Outcome's phrase "a new element ... that
lacks an encoding" describes a build failure this task's own scope
cannot trigger. This exact conflation was already raised as finding 3 in
this task's own parent feature review (`issue-reviews/issue-0542.adversarial.md`,
lines 67-88); #731 restates the same flawed framing in a child task
instead of correcting it.
*Recommendation:* drop the "FEAT-001 lineage"/registry-of-element-types
framing entirely for a five-or-fewer-state closed enum with an
exhaustiveness test (e.g. a switch with no default, or a
`Map` literal plus a test that iterates a `WireState` enum) — cite
`ElementRegistry` only as prior art for "how we've done totality
before," not as the mechanism to reuse.

**3. [High] The `ordering_after: [TASK-C542-1]` dependency is structurally unenforced and never justified.**
`issue_read(get_sub_issues, owner=anadon, repo=jls, issue_number=542)`
returns `[]`, and `issue_read(get_parent, #731)` returns `null` — neither
#731 nor its declared predecessor #729 is linked to #542 (or to each
other) through GitHub's actual sub-issue mechanism; `ordering_after:
[TASK-C542-1]` exists only as YAML prose inside #731's own body. Nothing
stops #731 being picked up before #729 lands, and the issue gives no
reason *why* the ordering matters: #729's scope (adding a tritanopia
transform to `ThemeTest`'s existing CVD assertions, per its own body) and
#731's scope (stroke/dash/glyph encoding in `WireRenderer`/
`WireEndRenderer`) touch different test files and different code paths
with no stated shared resource or interface. A reviewer cannot verify
the dependency is real, only that it is asserted.
*Recommendation:* either link #729 as a real GitHub-tracked predecessor
of #731 (or #542), or state the concrete reason for the ordering (e.g.
"both tasks touch `ThemeTest`'s per-theme iteration helper and would
merge-conflict on it") so the constraint is checkable instead of taken
on faith.

**4. [Medium] AC-4's "pixel-unchanged... gated on every commit" bar repeats an ambiguity this project has already been burned by once, unresolved.**
`ISSUE-AMBIGUITIES-2026-07.md` §6 (#101) records that a literal
pixel-identity assertion over real rendering was unreliable
(anti-aliasing/font noise) and was resolved by measuring an actual
tolerance from the first green run rather than guessing. #731's AC-4
says "pixel-unchanged" / "gated on every commit" with no statement of
whether this means cheap `Theme.DEFAULT` field-equality (as
`ThemeTest.applyRewritesTheJLSInfoStatics` already checks) or an actual
rendered-canvas diff, and if the latter, no tolerance. Left as-is this
either produces a flaky per-commit gate or invites an unstated loose
tolerance later that could let a real default-theme regression through
silently — a way the stated verification could pass while the real goal
(no visible change for existing users) fails. This is the same finding
already raised against the parent feature (#542 finding 4); #731
inherits it verbatim without resolving it at the task level either.
*Recommendation:* state explicitly which check AC-4 means; if a rendered
diff, adopt #101's measured-tolerance approach rather than an unstated
exact match.

**5. [Low] "the transcript recorded" (AC-2) names an artifact/convention that does not exist anywhere in the repo.**
No test harness under `test/` produces or checks a "transcript" for a
totality-test failure (a repo-wide search for "transcript" turns up only
the collaborative-editing/CRDT subsystem — `src/jls/collab/net/*`,
`docs/collaborative-editing-research.md` — an unrelated feature area).
As written, "with the transcript recorded" reads as a specific
verification step but has no defined meaning to build or check against,
so it can be satisfied by any assertion failure message an implementer
chooses to write, or quietly dropped.
*Recommendation:* either name the actual mechanism (e.g. "the JUnit
failure message must name the missing state") or drop the phrase.

**6. [Low] AC-1 alone is satisfiable by a no-op mapping; AC-1 and AC-3 should cross-reference so the gap isn't left implicit.**
AC-1 only requires "a registry-keyed mapping" to exist per state — it
does not itself require the encodings to differ from each other or from
today's defaults. Only AC-3 (injectivity) forces real differentiation.
This is a defensible split (presence, then distinctness, as two
separately testable properties) but as written a reader can satisfy AC-1
with a trivial "every state maps to the same stroke" registry and only
discover the requirement is empty once they reach AC-3 — worth an
explicit one-line cross-reference rather than leaving it implicit.

## What's solid

- Anchoring explicitly on `WireRenderer.strokeFor` and
  `WireEndRenderer`'s existing touch-ring as "the starting point, not a
  parallel mechanism" is accurate to the checkout
  (`src/jls/edit/WireRenderer.java:33-55`,
  `src/jls/edit/WireEndRenderer.java:58-65`) and avoids the "second
  screenshot matrix" duplication trap #542's review warned about.
- AC-3's injectivity requirement (no two states share an encoding, tested
  as such rather than merely "some encoding exists") is good test design
  and directly closes the gap noted in finding 6.
- AC-5's stop condition (glyph escalation that would wreck sighted
  legibility halts work for capstone re-derivation) is a genuine,
  falsifiable kill criterion, not an open-ended escape hatch.
- Scoping to the `Theme` seam rather than inventing a parallel styling
  mechanism is the right integration point given `Theme.apply()`'s
  existing role rewriting `JLSInfo` statics.

## Verdict

**needs-rework.** The mechanical pieces this task can control (build on
`WireRenderer`/`WireEndRenderer`, injectivity testing, a stop condition)
are sound. But the task's foundation is shaky: it asserts a five-state
wire-value inventory that contradicts the project's own normative
simulation semantics (finding 1, "error" specifically forbidden by
recorded decision), borrows a registry pattern that is keyed on an
unrelated axis (finding 2, already flagged once at the parent-feature
level and not fixed here), and declares an ordering dependency with no
structural link and no stated reason (finding 3). None of this is
unfixable, but AC-1 in particular cannot be implemented correctly until
someone decides what the actual state set is.
