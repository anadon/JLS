# Issue #631: TASK-C561-2: Falstad's logic subset becomes a working JLS circuit, with labeled nodes surviving as net structure
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#631 is the mapping-layer task of FEAT-C29-4 (#561): it takes the parsed model
TASK-C561-1 (#629) produces and turns the logic subset into real JLS elements
and nets. The acceptance criteria are more concrete than the parent feature's
(AC-2's "structurally asserted" net-merging claim is a real improvement over
prose), but the task has a load-bearing gap between what it assumes its sole
named prerequisite delivers and what that prerequisite's own acceptance
criteria actually promise, an unresolved boundary between "refuse" (AC-4) and
"named loss by design" (boundary notes) that determines whether an analog
element crashes the import or passes through quietly, and the same
single-circuit gameability already flagged on both the parent feature (#561)
and the sibling closing task (#633). No Falstad code, parser, or mapping
table exists anywhere in this checkout (`grep -ril falstad src/ test/ docs/`
returns nothing but prior review files), so every claim below is about the
spec.

## Findings, most severe first

### 1. [HIGH] AC-2 depends on label data that #629, its only named prerequisite, never commits to producing

`ordering_after: ["TASK-C561-1 (the parsed model this maps from)"]` — #631's
entire mapping stage is built on the in-memory model #629 (TASK-C561-1)
produces. But #629's outcome text and all four of its ACs are silent on
labels: AC-1 covers malformed/hostile-input refusal, AC-2 covers numeric
overflow, AC-3 says no partial model on failed parse, AC-4 promises only "a
source location per element expressive enough for #556's `location` field."
Nowhere does #629 commit to extracting a Falstad labeled-node's label text as
a first-class, queryable field of the parsed model. If TASK-C561-1's grammar
treats a label as opaque trailing text inside an element's parameter list
(which is exactly how Falstad's own compact format stores it — as a
positional string field, no different in kind from any other parameter),
#631 AC-2 ("two elements joined only by a shared label are in one net,
asserted structurally") has nothing to build against, and the gap surfaces
for the first time during #631's own implementation rather than being
caught at spec time.

**Recommendation:** add an AC to #629 (or amend #631's `ordering_after` note)
that explicitly commits the parsed model to exposing label text as a keyed,
queryable field per element — not just a source location — since #631's
central acceptance criterion is unimplementable without it.

### 2. [HIGH] AC-4's "refuses by name" and the boundary notes' "named loss by design" are not reconciled by any stated classification step

AC-4: *"A logic element outside the mapped subset refuses by name rather
than mapping to the nearest-looking JLS element."* Boundary notes: *"Analog
elements are not mapped here or anywhere — they are named losses by
design."* Both statements describe what happens to an element the mapping
table doesn't cover — but they prescribe opposite outcomes: AC-4 says
refuse (a hard failure that CAP-29 AC-5 and #561 AC-4 elsewhere define as
loud refusal of the whole operation), the boundary note says pass through
as a soft, reported, non-fatal loss. The text avoids a literal contradiction
only via the qualifier "a **logic** element" in AC-4 — implying some prior
classification step has already sorted every Falstad element code into
logic/analog/unmapped-logic before AC-4's refuse-or-pass branch runs. But no
issue in the TASK-C561 family (#629, #631, #633) defines that classification
step, names where it lives, or specifies what happens on a
misclassification. Two concrete failure modes follow directly: (a) a
genuinely analog element gets miscategorized as "unmapped logic" and AC-4
aborts an import the feature's entire premise says should succeed with a
named loss; (b) a genuinely out-of-subset logic element (e.g. a "custom
logic" truth-table block, which the outcome text's enumerated list — gates,
FFs, counters, logic inputs/outputs, labeled nodes — does not mention) gets
miscategorized as "analog" and silently passes through unreported, which is
precisely the "look right and be disconnected/dropped" failure the issue's
own Outcome paragraph exists to prevent.

**Recommendation:** name the classification table (ideally the same
"written, reviewable table" AC-1 already requires, with a third column for
disposition: mapped / refuse / analog-loss) and make it exhaustive over the
element codes #629's parser accepts, so "outside the mapped subset" cannot
mean two different things depending on which element it is.

### 3. [MED] AC-1's "one row per Falstad element code" has no totality test, reproducing a gap the parent feature's own review already flagged

AC-1 requires the mapping table live in `docs/` with "one row per Falstad
element code," but nothing requires that the table's rows be checked against
the actual set of element codes #629's parser recognizes (a bidirectional
equality: every code the parser accepts has a row, every row corresponds to
a code the parser accepts). The #561 (parent feature) review already
identified this exact gap for AC-2 there, citing CAP-16's `HdlExporter.java`
shift-register name-collision precedent as proof that a non-total,
non-registry-keyed map is "exactly how a construct gets dropped silently."
#631 restates the same unenforced prose requirement one level down, at the
task that actually implements it.

**Recommendation:** require a test that walks the table and the parser's
element-code enum/constant set and asserts set equality in both directions,
not just that a markdown table exists and looks plausible.

### 4. [MED] AC-3's single fixture is gameable on both circuit selection and oracle independence

AC-3: *"An imported logic circuit simulates; a fixture asserts its outputs
against the expected truth of the source circuit rather than only that it
loaded."* Two independent weaknesses: first, one implementer-chosen circuit
(same gap the #561 and #633 reviews already flagged for their own
single-circuit ACs) can trivially avoid hard cases — multi-driver labeled
nodes, a labeled node that is also conventionally wired to something else,
edge-triggered elements with nontrivial timing. Second, and specific to this
AC: "the expected truth of the source circuit" names no independent oracle.
Nothing stops the fixture's expected values from being back-computed by
running the freshly-imported JLS circuit itself and recording whatever comes
out — which would satisfy the letter of AC-3 (asserts real output values,
not just "it loaded") while proving nothing about correctness, since the
oracle and the thing under test would be the same simulation run.

**Recommendation:** require the expected truth table be derived
independently of the JLS import (hand-computed from the Falstad circuit's
documented behavior, or from Falstad/CircuitJS1's own simulator output) and
committed alongside the fixture, and require N≥2-3 circuits mirroring the
corpus recommendation already made against #561 and #633.

### 5. [LOW] AC-2's "two elements" wording under-specifies the N-way case

Falstad labels are not limited to pairs — any number of elements can share
one label and must land in a single net. AC-2's illustrative "two elements
joined only by a shared label are in one net" reads naturally as the minimal
test case, but as the literal acceptance text it could be satisfied by an
implementation that only merges labels pairwise (e.g., correctly joins the
first two occurrences of a label but drops a third), since a test built
strictly to the letter of "two elements" would never exercise that path.

**Recommendation:** reword to "all elements sharing a label are in one net"
and require a fixture with at least three same-labeled elements, not two.

### 6. Dialect/version ambiguity is inherited, not introduced, here — one-line note

The parent feature review (#561 Finding 4) already flagged that "the Falstad
text format" spans multiple lineages (original Java-applet exporter vs. the
actively-developed CircuitJS1 fork) with no pinned target version. #631's
"gates, flip-flops, counters, logic inputs and outputs, labeled nodes"
enumeration is a reasonable subset either way, but AC-4's refusal path is
the mechanism that has to absorb whatever the pinned version turns out to
be — worth flagging as inherited risk rather than re-arguing here.

### 7. The mapped-subset scope and the "not a layout task" boundary are solid — no issue

Restricting geometry translation to "legible placement" and excluding
layout fidelity is an appropriately scoped non-goal for a semantics-mapping
task, and AC-4's principle — refuse by name rather than guess a
nearest-looking element — is the correct anti-pattern guard (it is exactly
what CAP-16's shift-register precedent, cited in Finding 3, argues against
doing).

## Verdict rationale

AC-2 and AC-4 are the two ACs that most directly test the issue's own
stated purpose (net structure survives, no silent misrouting), and both have
concrete gaps: AC-2 rests on a prerequisite that never promises to deliver
the data it needs (Finding 1), and AC-4's refuse/pass split is undefined at
exactly the boundary the issue itself calls out as the interesting case
("or the imported circuit will look right and be disconnected"). Findings 3
and 4 reproduce gameability already identified on this task's parent and
sibling, unaddressed here despite the pattern being established elsewhere in
the same task family. These are fixable by naming a classification table and
tightening three ACs, not by discarding the task. **needs-rework.**
