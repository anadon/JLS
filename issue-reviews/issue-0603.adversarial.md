# Issue #603: TASK-C486-1: a driver can say how fast its output changes — a transition time beside the existing propagation delay, absent by default, with every golden byte-identical
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core ask — an optional, absent-by-default `t_r`/`t_f` pair beside the
existing scalar `propDelay`, with every golden byte-identical — is a sound,
cheap, reversible change. But several of the five acceptance criteria cite
prerequisite infrastructure or tracker issues that do not exist, do not say
what they are claimed to say, or contradict the parent feature's own
machine-readable dependency graph. As written, the issue cannot be honestly
closed against its own criteria without either fixing those citations or
quietly reinterpreting them, and it inherits at least one factual error from
its parent (#486) rather than catching it.

## Findings, most severe first

### 1. [Critical] AC5 cites three issues as the Liberty/SDF/SDC "owners" of transition time; none of the three has anything to do with timing formats

AC5: *"The shape is written down against P4's `DelayModel` (#87 / #89 / #93
named): a short note states how a scalar `t_r` becomes the degenerate entry
of a per-arc min:typ:max table."* This text is copied verbatim from #486's
`related` block, which glosses them as *"SDF (#89), Liberty (#87) and SDC
(#93) each owe it independently."*

Fetched all three directly:
- **#87** is a closed PR, *"Add open-issue review: rigor, consistency, and
  campaign readiness (July 2026)"* — a review of 52 unrelated issues.
- **#89** is a closed Dependabot PR, *"Bump the actions group with 5
  updates"* — a GitHub Actions version bump.
- **#93** is a closed issue, *"Null safety as a compile-time property:
  complete the NullAway/@NullMarked rollout"* — a static-analysis rollout,
  already merged.

None mentions Liberty, SDF, SDC, or a `DelayModel`. A repo-wide search for
`Liberty SDF SDC DelayModel in:title,body` returns only #486 and #313
themselves — **no issue in the tracker actually owns this timing-format
work**. AC5 asks an implementer to write a note reconciling this task
against issues that, on inspection, say nothing about the subject. As
literally stated the criterion is unsatisfiable in good faith: anyone who
actually opens #87/#89/#93 before writing the note will find nothing to
reconcile against, and anyone who writes the note without opening them is
exactly the "claimed in review" failure mode AC2 explicitly tries to rule
out for goldens. **Recommendation:** strike the `#87/#89/#93` citation (or
replace it with real issue numbers if the correct ones exist under
different IDs) before work starts; do not let an implementer paper over a
dangling citation with a plausible-sounding note.

### 2. [Critical] AC4 requires a save/load contract (#319) that does not exist yet, and #603's own header omits it as a prerequisite

AC4: *"...a reader that does not understand them degrades per FEAT-013's
(#319) optional-section contract rather than silently mangling them."*
FEAT-013 (#319, fetched) is **open**, and its own three tasks
(TASK-0033/0034/0071) are each still `"Not filed"` — the per-section
must-understand frame, `SectionFrame`, and must-understand flags do not
exist in the tree. The *current* mechanism, confirmed in
`docs/file-format.md:220-222`:

```
**Unknown attribute names are silently ignored.** The reader offers
each item's name and value to the element; if no declared attribute
consumes it, the value is dropped without error.
```

is exactly the global silent-drop valve AC4 says this feature must *not*
degrade through. So AC4 commits this task to a behavior — a "clean
diagnostic naming the skipped optional section" — that no code path in the
repository can currently produce, because the mechanism that would produce
it is itself an unimplemented, unstarted feature.

Compounding this: #603's own YAML header lists only `ordering_after: [367]`
— **#319 is not named as a prerequisite at all**, even though #486 (the
parent feature this task is `part_of`) explicitly declares
`blocked_by: [367, 336, 319]` including #319 for exactly this reason (the
attributes "ride as an OPTIONAL section" per #486 §3). This task silently
drops one of its parent's two hard format-related prerequisites.
**Recommendation:** either add #319 to `ordering_after`/`blocked_by` and
treat this task as blocked until #319 lands, or rewrite AC4 to describe
degradation via the format's *existing* silent-drop behavior (which is
achievable today) and record the upgrade to #319's contract as a follow-up
once #319 exists — but do not leave the criterion pointing at
infrastructure that isn't there.

### 3. [High] #603 adds an ordering edge to #367 that its own parent's machine-readable graph does not draw for this task

#603's header: `ordering_after: [367]  # FEAT-047 physical time base: a
transition time in seconds is meaningless against a dimensionless tick`.

But #486's mermaid graph (fetched, reproduced verbatim) draws:

```
F047 --> T2
F013 --> T2
F004 --> T3
T1 --> T3
T2 --> T3
T1 --> F058
```

`T1` is exactly this task ("Transition time on the delay model"), and it
has **no incoming edge from F047 (#367)** — only `T2` (the declared net
length, a different, not-yet-filed task) is gated on #367. #486's own
Re-planning Protocol is explicit that ordering edges are supposed to be
mirrored and deliberate: *"An ordering edge changes ... obliges the same
REPLAN to write the mirror on the far issue; a half-edge is the defect the
Link pass exists to prevent."* #603 introduces exactly such a half-edge:
a dependency on #367 that #486's own DAG walk does not authorize for this
specific child task, with no REPLAN comment on #486 recording the change.
Either #486's graph is wrong (missing an edge for T1) or #603 has invented
a dependency the parent didn't intend — the two documents disagree and
nobody has reconciled them. **Recommendation:** resolve the discrepancy
explicitly (REPLAN on #486 if #367 really should gate T1, or drop the
`ordering_after` line on #603 if it shouldn't) before starting.

### 4. [High] AC1's "in seconds against FEAT-047's declared time base" is unverifiable today and is a gameable criterion as written

`docs/simulation-semantics.md:26-30` is normative: *"Simulation time is a
dimensionless non-negative 64-bit integer ... Time units are abstract;
nothing binds them to seconds."* #367 (FEAT-047), which would introduce the
`TimeBase` type this criterion depends on, is open with zero code landed
(`src/jls/core/` contains no `TimeBase.java`; confirmed by #367's own
evidence section). So there is currently no such thing as a value "in
seconds against FEAT-047's declared time base" anywhere in JLS.

The realistic implementation is a plain tick-count field, exactly like
`propDelay` (an `int`, no unit). AC1 doesn't say this is acceptable, but it
doesn't rule it out either — nothing in the criterion's wording forces an
implementer to make the seconds-ness real versus merely aspirational
(a comment, a Javadoc claim, a variable named `t_r_seconds` holding a raw
tick count). That is precisely the shape of a criterion that can be
satisfied on paper while missing the real goal: a reviewer checking "does
`t_r` declare itself in seconds" cannot actually test that today, only
inspect prose. **Recommendation:** rewrite AC1 to state plainly that `t_r`/
`t_f` are stored as dimensionless tick counts now (the same units as
`propDelay`), with the seconds interpretation deferred to #367, and drop
the "in seconds" language until #367 actually exists — or block this task
on #367 as finding 3 suggests, making the two inconsistencies resolve the
same way.

### 5. [Medium] "A delay-carrying element" is scope-ambiguous, and the only cited evidence covers 1 of at least 12 sites

The Outcome section anchors its evidence entirely to `Adder.java:33` and
`:261`. But `propDelay`/`defaultPropDelay`-style fields exist in at least
twelve files: `Adder.java`, `Gate.java`, `Mux.java`, `Register.java`,
`RegisterFile.java`, `ShiftRegister.java`, `StateMachine.java`,
`TruthTable.java`, `Decoder.java`, `DelayGate.java`, `TriState.java`,
`FieldExtend.java` (confirmed by grep). AC1 says *"A delay-carrying
element accepts..."* (singular, indefinite), which is compatible with two
very different readings:

- **(a) Every delay-carrying element must gain `t_r`/`t_f`** — in which
  case the `band_mw: "1-2"` cost estimate is very likely undersized: per
  ARCHITECTURE.md, touching even *one* existing element's persisted
  attribute set costs several of the "sixteen places" (save, `setValue`/
  `Attribute` entries, dialog, reset-to-default, round-trip fixture);
  doing that consistently across twelve classes at 1-2 maintainer-weeks is
  optimistic, and no evidence anchor exists for the other eleven.
- **(b) Only Adder needs to change, as a proof of concept** — in which
  case AC3 ("An element that declares neither value behaves
  indistinguishably...") is trivially true for every element that was
  never touched, which is a gameable, near-vacuous acceptance test: an
  implementer could add `t_r`/`t_f` to `Adder` alone, leave the other
  eleven completely untouched, and satisfy every stated criterion while
  the outcome ("edge rate exists as a declarable fact" for delay-carrying
  elements generally, which FEAT-058's lint and FEAT-059 are said to
  consume) is not actually delivered.

**Recommendation:** state explicitly which delay-carrying elements are in
scope for this task (all of them, or a named subset), and if it's a
subset, say which elements FEAT-058's lint and FEAT-059 (#490) are allowed
to assume have the attribute versus not.

### 6. [Medium] "Same arc granularity as its existing propagation delay" assumes a per-arc model that doesn't exist at any of the current call sites

`propDelay` on `Adder`, `Gate`, `Mux`, `Decoder`, etc. is one scalar per
*element instance* (in Adder's case, `bits * defaultPropDelay`), not a
value keyed per input-pin/output-pin arc the way #367's future `DelayModel`
(cited in #486) is described. "At the same arc granularity as its existing
propagation delay" is therefore not a precise instruction — the existing
granularity, at every current site, is "one number for the whole element,"
not "one number per arc." An implementer following the letter of AC1 could
reasonably add a single scalar `t_r`/`t_f` per element (matching current
practice) or could infer a need for a nascent per-pin-pair map (matching
the vocabulary borrowed from the eventual `DelayModel`); the issue doesn't
disambiguate, and getting it wrong either overbuilds (new arc-keyed
infrastructure nobody else asked for yet) or produces something AC5's
"degenerate case of the per-arc table" note can't actually make true.
**Recommendation:** state plainly that "arc granularity" here means
"one scalar per element instance, same as `propDelay`," matching what
Adder.java actually has today.

## What's solid

- **AC2/AC3 (golden byte-identity, absent-by-default no-op)** are
  concretely testable against the codebase's existing golden
  infrastructure (`BatchSimulationGoldenTest`, `SequentialGoldenTest`,
  `VcdExportGoldenTest`, `AllElementsRoundTripTest` per ARCHITECTURE.md),
  and match the "no format version, no palette change" discipline #486
  and #490 both establish as a pattern. This is a good, hard-to-game
  criterion.
- **The code citations that are checkable are accurate.** `Adder.java:33`
  is exactly `private static final int defaultPropDelay = 30;` and `:261`
  is exactly `propDelay = bits * defaultPropDelay;` — verified against
  HEAD. The issue is well-grounded where it cites live code.
- **The underlying design intent (degenerate case of a future per-arc
  min:typ:max table, never a second mechanism)** is a reasonable
  engineering position and consistent with how #486 frames the same
  choice.

## Bottom line

The instinct behind this task — a cheap, reversible, byte-identical-by-
default addition — is right, and the parts that are purely mechanical
(goldens, no-op default) are well specified. But three of five acceptance
criteria lean on things that turn out not to hold up under inspection: a
citation to unrelated/wrong issues (AC5), a dependency on unimplemented
save-format infrastructure that the task doesn't even declare as a
blocker (AC4), and a unit claim ("in seconds") the codebase cannot express
yet (AC1), compounded by an ordering edge (#367) that contradicts the
parent feature's own dependency graph for this specific task. These need
to be resolved — not silently worked around by whoever picks this up —
before implementation starts.
