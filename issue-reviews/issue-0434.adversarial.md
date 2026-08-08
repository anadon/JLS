# Issue #434: TASK-0102: two drawable level converters and a lock-step contract in which the digital loop owns the clock — an exact crossing tick, a ramped D-A, and no rollback machinery anywhere
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary judgment

The engineering core — invert time ownership so the discrete loop is the
sole clock, treat the analog region as a `Clock`-style self-scheduling
element, bisect to the integer tick lattice so the digital stream is
timestep-invariant — is a genuinely good design and is argued carefully. The
observations that were checked against the current tree hold up exactly (no
`Adc`/`Dac`/`AnalogRegion`/`nextEventTime` anywhere in `src/`, the 24-permit
sealed list, the 35-registered/32-palette split, `Clock.java`'s two `post`
call sites, `SimEvent.equals` excluding `seq`). But the issue's own
dependency bookkeeping is wrong and stale in ways that make it unsafe to
pick up as filed: it misattributes which feature owns its stated
prerequisite, and both of its stated blockers have since been filed as
separate open issues that `blocked_by` does not list.

## Findings, most severe first

### 1. TASK-0105's owning feature is misattributed, and contradicts this issue's own parent

The Status block says `TASK-0105 ... owned by FEAT-050` was left out of
`blocked_by` only because it "is not yet filed," and §12 repeats "TASK-0105
(not yet filed, owned by FEAT-050 / #330)." I fetched #330: its title is
*"FEAT-050: the module registry decides what the program does — subsystems
dispatch through declared seams, and an element type can arrive from
outside the tree."* It is about plugin/module dispatch and discovery; it
contains no mention of palettes, views, or toolbars anywhere in its body.
It is also **`state: closed`, `state_reason: duplicate`**. Meanwhile this
issue's own parent, #368 (FEAT-048), records the correct ordering edges as
`blocked_by: [331, 351]` — #331 is FEAT-049, "a student draws analog
devices the way they already draw gates," which is the feature the
per-view-palette task actually belongs to. This is confirmed independently:
TASK-0105 has since been filed as #482 with `part_of_feature: 331`, not
330. #434 cites a dead, unrelated issue as the authority for a hard
blocking prerequisite, and disagrees with its own parent issue while doing
it.
**Recommendation:** strike the FEAT-050/#330 citation; re-point at #331 to
match #368, and re-file the `blocked_by` entry against the now-real #482
(see finding 2).

### 2. Both stated prerequisites have since been filed and are open, but `blocked_by: []` was never updated

The Status block reads `blocked_by: []` with a comment explaining "TASK-0097
... and TASK-0105 ... are both genuine prerequisites and neither is filed
yet." That was true when #434 was opened, but is false now: TASK-0097 is
filed as **#463** ("a headless MNA transient solver in pure Java...",
open, unlanded) and TASK-0105 is filed as **#482** (open, unlanded,
itself blocked by #383). Per this issue's own rule 6 ("re-verify O1/O2 at
the executing checkout; if superseded, say so"), a reviewer picking this up
today has to independently discover both issues before they can even
confirm the task is still blocked — the issue as written asserts a
dependency state that a `search_issues` query for "TASK-0097" or "TASK-0105"
immediately contradicts.
**Recommendation:** update `blocked_by: [463, 482]` (mirrored on both) and
strike the "neither is filed yet" language; do not let the machine block
and the prose diverge from the tracker.

### 3. The load-bearing normative documents this issue cites do not exist in the checked-out repository

§1 cites `docs/plan/evidence/BRIEF.md` §12 for decision D9 and
`11-analog-determination.md` §§2.5–2.6 for decisions D-A7/D-A8, the crossing
formula, and the parameter lists — "referenced, not re-derived." Neither
file exists anywhere under `/home/user/JLS`; there is no `docs/plan`
directory at all in this checkout (verified by `find`). The two documents
this task's entire mathematical contract (§7.10's crossing formula,
hysteresis rule, ramp equations, step-cap regimes) is delegated to are
unreachable, so an implementer cannot actually verify the derivation this
issue asks them to trust — only the sibling issue #368 happens to quote a
few lines of D-A7 inline, which is not the same as the cited section
resolving. (Caveat: this checkout is a shallow clone — 269 commits, oldest
reachable commit newer than the cited `evidence_commit` — so the *commit*
citations 2d0ca9d.../3a81a4a7... are inconclusive rather than disproven.
The missing *files on the current tree* are not a shallow-clone artifact,
since the completion criteria require these docs to "resolve on the default
branch at close," which is a tree-content claim, not a history claim.)
**Recommendation:** commit `docs/plan/evidence/BRIEF.md` and the
analog-determination doc to the tree before this task is worked, or replace
"referenced, not re-derived" with the actual derivation inline.

### 4. Scope is one "task" only in name; the real prerequisite chain is two more large, unlanded features

TASK-0097 (#463, the MNA solver) has its own open sub-dependents (#481
determinism, #397 external-oracle nightly, #402 controlled sources, #464
homework-grade convergence) none of which are closed. TASK-0105 (#482, the
palette view dimension) is itself blocked on #383. So "done means: a drawn
`Dac -> RC -> Adc` loop runs to a stated tolerance" is not reachable by
landing this issue's own checklist — it requires a functioning transient
solver that does not exist yet, which is 4-6 maintainer-weeks in its own
right per #351's cost section, plus the palette-view mechanism, plus the
domain-alphabet check named as a hard co-requirement by #368 §1 ("the
domain check landing without the bridges would make legal designs
undrawable... they ship together") — a mechanism this issue's own
`Materials & Apparatus` section does not list as required reading even
though #368 makes it a release-blocking pair.
**Recommendation:** the issue is honest that these are prerequisites, but
undersells the total critical path; a reader should not conclude this is a
2-week task in isolation (its own cost citation elsewhere puts TASK-0102 at
2 wk, but that number only covers this issue's own checklist, not the path
to make it runnable).

### 5. P9's completion bar has no numeric threshold, so a marginal fix can satisfy the letter without the goal

P9 requires "a 44.1 kHz `Dac` against a fast clock does not produce one
analog visit per queued digital event," motivated by "on the order of a
million wasted solver visits per genuine boundary event — arithmetically
fatal." But P9 only forbids the exact 1:1 ratio; an implementation that
reduces visits from 1.0/event to 0.9/event technically satisfies "does not
produce one... per queued digital event" while remaining "arithmetically
fatal" by the issue's own framing. Contrast with P4, which is a strong,
genuinely hard-to-game anti-cheat criterion (exact integer equality of
emitted ticks across a 10x LTE-tolerance spread) — P9 should be held to the
same standard.
**Recommendation:** replace P9 with a stated numeric bound, e.g. "at most
O(1) analog visits per genuine boundary event, independent of clock/Dac
frequency ratio," and assert it with a measured count in the test, the way
§9 already proposes recording "analog visits per genuine boundary event."

### What checks out

- **O1/O2 (the load refusal, the missing `nextEventTime()`)** reproduce
  exactly at current HEAD: no `Adc`/`Dac`/`AnalogRegion`/`nextEventTime`
  anywhere under `src/` (`grep` confirms zero hits), consistent with the
  claim that `src/` and `test/` are byte-identical between the pinned
  commit and HEAD.
- **O3 (sealed permits, registry count)**: `LogicElement.java`'s permits
  list matches the quoted 24 entries verbatim at current HEAD;
  `ElementRegistry.ALL` has exactly 35 entries (`grep -c "new ElementType"`
  = 35), matching O3/O4's arithmetic.
- **O5/O7 (Clock idiom, SimEvent dedup)**: `Clock.java:392,421` and
  `SimEvent`'s `equals`/`seq` exclusion match as quoted.
- **The core design idea (inverted time ownership deletes rollback) and the
  crossing/hysteresis/ramp math (§7.10)** are well-argued and internally
  consistent, and the falsification criteria (§10) are genuinely
  falsifiable rather than decorative.
- **P5/P6 (validation refusals for `vlow>vhigh`, `tdelay<1`)** are concrete,
  testable, and correctly identify real termination hazards (same-instant
  loops) rather than asserting them by fiat.

## Verdict

**needs-rework.** The design is sound, but the issue cannot be picked up as
written: its stated blocking dependency (TASK-0105) is attributed to the
wrong, closed-as-duplicate feature and contradicts its own parent issue
(#368); both of its stated prerequisites have since been filed as separate
open issues (#463, #482) that `blocked_by` does not reflect; and its
normative basis documents are absent from the tree it is meant to be
implemented against. These are bookkeeping defects, not indictments of the
underlying engineering, but per this project's own rule 6 ("re-verify at
the executing checkout... if superseded, that half is superseded") the
issue fails its own supersession check today and needs a dependency-graph
pass before an executor should start on it.
