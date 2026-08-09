# Issue #73: First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## Summary judgment

The issue is open, but its own most recent comment says plainly that it is not
a work item and nobody should act on it directly. Treating #73 as an
assignable unit of work — which is exactly what this review's framing asks —
reproduces the failure mode the issue's own history already flagged three
times. The findings below are ordered by how much damage each would do to a
team that picked this issue up and executed it as written.

## Findings

### 1. (Critical) The issue instructs readers not to work it, yet remains open and reads as actionable

The final comment (2026-08-08, id 5227545571) states outright: *"This is a
superseded-scope pointer, not a work item... No task should be filed against
it, and no executor should pick it up directly; anyone routed here by a stale
cross-reference should follow the table above."* That table routes all five
title clauses to #545, #548, #550, #551, #552 (16 downstream tasks: #381,
#760, #762, #764, #766, #768, #770, #771, #777, #779, #780, etc.).

Despite this, the issue is `"state":"open"`, carries a title that reads as a
normal feature ask, and a body whose Completion Criteria checklist, machine
block, and "Decomposition & Rationale" table read as if the three
`planned_tasks` were still open and unassigned. Nothing in the title, labels,
or body signals "do not implement" — only the last of ten comments does, and
only if a reader reads the whole thread. An engineer (or an automated agent)
skimming the issue body alone would start building the empty-state panel,
duplicating #381's TASK-0030 work.

**Recommendation:** Close #73 (or retitle it e.g. "META: superseded — see
#381/#545/#548/#550/#551/#552") and edit the body itself, not just add
another comment. A ten-comment thread ending in "don't work this" is not a
substitute for the issue state reflecting that.

### 2. (High) The machine-readable header is self-acknowledged stale and has been left false for at least 4 days across 3 REPLAN comments

The body's YAML block still reads:
```yaml
requires_tasks: []      # no children filed yet — see planned_tasks
planned_tasks:
  - "Empty-state panel..."
  - "Five bundled sample circuits..."
  - "README onboarding pass..."
blocked_by: []
```
Comment 5175804376 (2026-08-04) recommends: *"Resolve #73's `planned_tasks`
by `REPLAN:` to name #381 ... rather than filing a parallel task — otherwise
the panel and the sample circuits get built twice."* Comment 5181501328
(same day) repeats: *"That has not happened: `requires_tasks` is still `[]`
and `planned_tasks` still lists three unfiled entries whose work is now filed
elsewhere."* Comment 5227545571 (2026-08-08) again notes the roster problem
and stops short of fixing it, calling the disposition table "the thing that
was absent" rather than editing the header. The issue's own §7 protocol
requires: *"Filing any planned task → REPLAN: comment resolving it to a
number in requires_tasks."* This has been violated for at least three
successive audit passes on the same issue. A dependency-graph tool or agent
parsing the YAML block (rather than reading all ten comments) will conclude
this feature is unblocked, unfiled, and needs three fresh tasks — exactly
wrong, since ~16 tasks already exist across 5 other issues.

**Recommendation:** Do not accept "recorded in a comment" as done; the
Definition of Done itself requires "Machine block, roster table, and mermaid
graph agree with reality at close" (final DoD line) — that criterion already
fails today, by the issue's own evidence.

### 3. (High) IC1 acceptance criterion requires human-subject logistics with no owner, budget, or timeline — the issue admits this itself

Section 5's IC1 gate: *"five screen-recorded volunteers who have never seen
JLS must each reach a running counter simulation unaided within ten minutes
of launch... Success in ≥ 4/5 attempts."* This is not something `mvn verify`
or any CI lane can produce. The Open Questions section confirms it: *"IC1
volunteer logistics (external — human subjects): who recruits the five naive
volunteers and when. ... Blocks close-out, not filing children."* So the
issue structurally cannot reach its own Definition of Done through ordinary
engineering work — it is gated on an unrecruited, unscheduled, unbudgeted
human trial with no named owner beyond a "recommended default" of "maintainer
recruits." For a single-maintainer project (per ARCHITECTURE.md's
internationalization decision: "single-maintainer pedagogy tool"), this is a
realistic feasibility risk, not a formality — the issue could sit "done except
for IC1" indefinitely, which is arguably already happening (created 2026-07-08,
still open a month later with zero children).

**Recommendation:** Either scope IC1 out to its own tracked, resourced issue
with an explicit owner and date, or accept that #73 (and whichever successor
inherits IC1 — the disposition table says #73 itself keeps IC1) cannot close
on any predictable schedule.

### 4. (Medium) IC1's sample size and protocol make the pass/fail criterion easy to satisfy without validating the real goal

n=5 volunteers, threshold 4/5, ten-minute cap, "guided only by what the app
and README surface." Nothing in the text specifies volunteer selection
criteria (a CS-major convenience sample would trivially pass; a genuinely
naive non-technical student might not), nothing specifies what counts as
"unaided" in practice (can the observer answer "what do I click?"; is a
desktop-icon double-click itself part of the measured task?), and nothing
specifies which of the five *planned* sample circuits volunteers are steered
toward for "a running counter simulation" — the counter is one of five named
samples (#548/#381 territory), so the trial's target artifact depends on
downstream issues #73 itself no longer directly owns. The honesty note
correctly flags that there is no baseline/control ("P1's ... near-0 baseline
... was never measured ... restated forward-only"), which is good practice,
but it also means IC1 can never support the comparative claim the feature's
own Abstract implicitly promises ("this feature makes the first ten minutes
self-guided" — self-guided *compared to what*, if there's no prior
measurement?). A team could hit 4/5 and still not know whether onboarding
materially improved over the status quo.

**Recommendation:** Add a volunteer-selection rubric (e.g. "no prior digital
logic or JLS/Logisim experience") and define "unaided" precisely (no
observer hints beyond generic encouragement) before the trial is run, not
after — otherwise IC1 is gameable in exactly the direction that would make
the feature look successful regardless of real onboarding quality.

### 5. (Medium) Scope-vs-title mismatch: "applet-era cleanup" is one stale javadoc line, not a workstream

The title lists five co-equal clauses ("...applet-era cleanup, README
screenshots"), but comment 5227545571 measured it directly: `grep -rniE
"applet|JApplet|getAppletContext|codebase" src/` returns exactly one hit,
`src/jls/edit/SimpleEditor.java:452` — a comment ("Get reference to top so
applet can put menu in."). I re-ran the equivalent grep against
`/home/user/JLS/src` and confirm the same single hit. The comment concludes,
correctly, *"This does not warrant a task... it should be swept up [in #84 or
#316/#440] rather than filed as work in its own right."* Yet the title
still elevates this to one-fifth of the feature's name, which will keep
misleading anyone triaging by title alone (including, evidently, an earlier
audit pass that treated it as a separate deliverable worth a `grep` gate in
IC3).

**Recommendation:** Drop "applet-era cleanup" from the title on any
successor/retitle; it is not a scoped deliverable at this size.

### 6. (Low) Duplicate-work hazard from the fan-out is real, not hypothetical, and #73 is the least specific of the six related issues

Comment 5175804376 documents the overlap explicitly for four pairs
(#73↔#550, #73↔#548, #73↔#545, #73↔#552) and one true duplicate-risk pair
(#73↔#355/#381: *"the same implementation work is described twice"*).
Six issues now describe overlapping onboarding surfaces with different
acceptance criteria; #73 is the oldest, vaguest, and (per finding 1) is the
one explicitly marked as superseded. Anyone assigned "#73" without reading
the full thread has a good chance of re-implementing work #381/#548/#550
already own, producing merge conflicts or duplicate `resources/samples/`
mechanisms.

**Recommendation:** treat #73 as closed-in-spirit; route any actual
onboarding work through #381/#545/#548/#550/#551/#552 as the comment table
already prescribes.

## Things that check out

- Citations verified against the repo at the cited commit-relative paths:
  `JLSStart.java:2188` is `TellUser.prompt(this, "Enter circuit name...")`
  inside `newCircuit()` — matches the "bare name prompt" claim.
  `JLSStart.java:1256-1264` shows `bar.add(editMenu()); bar.add(elementMenu());`
  with a comment crediting #75 — matches the "Edit menu re-homed" claim.
  `resources/` contains only `help/**` and `packaging/**`, no `samples/` —
  matches "today resources/ holds only help/ and packaging/."
- The applet-grep claim (IC3, and comment 5227545571's measurement) is
  independently reproducible: exactly one hit, at the exact line cited.
- The re-planning discipline visible in the thread (explicit REPLAN comments,
  dead-citation repair, restoring an accidentally-weakened ten-minute bound)
  is a genuinely good practice pattern even though the underlying
  header-update step was skipped (finding 2) — the intent and process are
  sound, the execution of that one step is not.
- Section 4's global invariants (green `mvn verify`, byte-identical
  round-trip, unchanged batch/headless CLI, no new SpotBugs exclusions) are
  concrete, testable, and consistent with `ARCHITECTURE.md`'s documented
  invariants (headless core, `TellUser`-only dialogs, etc.) — no conflict
  found with the actual codebase.
