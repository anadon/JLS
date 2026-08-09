# Issue #653: TASK-C565-2: a minimized table becomes a two-level netlist built only from elements already in the palette
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task's core premise — synthesize a two-level AND/OR/NOT netlist from an
already-minimized structured expression, using only registry elements — is
technically feasible (`AndGate`/`OrGate`/`NotGate` support arbitrary
`setNumInputs`, `Wire`/`WireEnd` are plain constructible objects, no element
count ceiling exists) and no synthesis code exists yet
(`grep -rin "quine\|mccluskey\|synthes" src/ test/` finds nothing but this
issue family's own text). But the acceptance criteria contain a cross-issue
citation that points at the wrong criterion, an escape hatch that lets the
task's one distinguishing piece of engineering (shared-term reuse) be
abandoned by fiat, an `ordering_after` dependency that is not a real code
dependency, and a determinism criterion with no stated definition of
"same netlist" to test against.

## Findings, most severe first

**1. HIGH — AC-1's citation points at the wrong acceptance criterion in the parent feature, and the two candidates it could mean are materially different in cost.**
Quoted: *"AC-1: A minimized multi-output table produces a connected two-level
netlist whose simulation matches the table on every row (FEAT-C31-3 AC-2)."*
Fetching #565 (FEAT-C31-3) directly: its **AC-1** is *"A truth table
synthesizes to a drawn circuit whose extracted table (via FEAT-C31-1)
round-trips identically"* — the closest match to "matches the table on every
row." Its **AC-2** is *"The synthesized circuit is two-level over the
minimized form from FEAT-C31-2 and uses only existing palette elements — no
new element types"* — which is what #653's own **AC-2** already covers, not
AC-1. The citation is off by one criterion. This is not cosmetic: the two
possible readings differ hugely in cost. "Matches the table on every row" via
direct `BatchSimulator` comparison against the structured minimized expression
is buildable now, independent of the unbuilt combinational-cone extractor
(#872) that #565's actual AC-1 (extraction round-trip) depends on per the
#565 review's finding 5. If a reader trusts the parenthetical over the prose
and treats this as literally discharging #565 AC-2 (already #653's own AC-2),
the netlist-vs-table correctness check AC-1's prose actually promises could
be silently dropped as "already covered." **Recommendation:** fix the
citation to `(FEAT-C31-3 AC-1)` and state explicitly that AC-1 here is
verified by direct simulation of the synthesized netlist, not by running the
not-yet-built #563/#872 extraction path (that full round-trip is #655's job).

**2. HIGH — AC-3's own escape hatch lets this task's only real optimization be skipped with a sentence, and nothing downstream forces it back.**
Quoted: *"AC-3: Shared product terms across outputs are instantiated once
rather than duplicated per output, **or the duplication is a recorded
decision with its reason.**"* The "or" clause makes AC-3 satisfiable by
writing one sentence ("duplicated for implementation simplicity") instead of
building cross-output term-sharing detection — the one piece of this task
that is actual algorithmic work beyond mechanical instantiation. Nothing
downstream closes the loophole: #655 (TASK-C565-4, the round-trip task)
fixtures *"a table with shared product terms"* (its AC-3) but only asserts
round-trip identity, not that sharing occurred — a fully-duplicated netlist
round-trips identically to a shared one. So the capability the Outcome
narrative implicitly promises (an efficient two-level netlist, not merely a
correct one) has zero tests forcing it to exist. **Recommendation:** either
drop the escape hatch and require sharing, or keep it but require the
"recorded decision" to name a tracking issue (not just a sentence in a
commit), and add a term/gate-count assertion on a shared-term fixture the way
the #564 review recommended for the minimizer itself.

**3. MEDIUM — `ordering_after: ["TASK-C565-1"]` (#652, table entry/editing) is not a real code dependency and repeats a pattern already flagged on a sibling issue.**
This task's actual input is #648's (TASK-C564-1) structured minimized
expression — confirmed by #648 AC-4: *"The expression is a structured value,
not a formatted string, so #565's synthesis can build a netlist from it
without re-parsing text."* #652 is interactive GUI table entry/editing; it
touches none of that data path. Sequencing #653 behind #652 needlessly
couples netlist-construction logic to unrelated editor UI work landing first.
The #564 review already made exactly this point (its finding 4) about a
different issue in the same family citing an over-strict `ordering_after`.
**Recommendation:** drop `TASK-C565-1` from `ordering_after`; keep
`TASK-C564-1` (the real dependency) and note the minimizer's structured
output can be exercised via hand-built fixtures independent of #652.

**4. MEDIUM — AC-4's determinism claim ("same netlist, element for element") has no stated comparison method, and is gameable by a shallow check.**
Quoted: *"AC-4: Synthesis is deterministic: the same table produces the same
netlist, element for element."* "Element for element" could mean (a) an
identical multiset of element types and counts, (b) identical wiring
topology (which gate output feeds which gate input), or (c) byte-identical
save-format text. The boundary notes explicitly exclude geometry ("this task
produces structure, not geometry"), so (c) is presumably not intended, but
nothing says so, and (a) alone is a materially weaker — and separately
satisfiable — bar than (b): a shuffled but topologically-different wiring
between two runs could still yield equal type/count multisets. Given AC-3's
sharing decision (finding 2) already introduces one source of run-to-run
choice (which duplicate to keep, or whether to dedupe at all), and #648's own
AC-3 determinism only pins the *minimizer's* tie-break, not this task's
gate/wire construction order, an implementer iterating literal sets or
shared-term maps in an unstable order could pass a loose interpretation of
AC-4 while failing a strict one. **Recommendation:** state the comparison
explicitly — e.g. "isomorphic wiring topology, verified by a canonical-form
comparison test," or "identical save-format ELEMENT/WIRE record sequence
modulo position" — and pin the same tie-break discipline #648 AC-3 already
requires upstream.

**5. LOW — no test coverage contract for name collisions between the table's declared pin names and internally synthesized element names.**
The Outcome promises "the input and output pins the table declared"; nothing
in the ACs addresses what happens when a table's declared signal name
collides with whatever naming scheme synthesized AND/OR/inverter elements
use internally, or when two outputs share a product term whose synthesized
element needs one name shared across two consumers. `TruthTable`'s own
`inputNames`/`outputNames` (`src/jls/elem/TruthTable.java:76-78, 610-654`)
only dedupe *within* the table; nothing in #653 states that synthesized
internal element names are drawn from a disjoint namespace or verified
unique circuit-wide before save. **Recommendation:** add a fixture with a
signal name likely to collide with a generated internal name (e.g. an input
literally named `and1`) and assert the synthesized circuit still saves and
loads cleanly.

**6. LOW — the sub-issue graph is broken exactly as already diagnosed for this feature's siblings.**
`get_parent(653)` returns `{"parent": null}` despite the YAML front matter's
own `part_of_feature: 565`. This is the same defect the #565 review already
raised (its finding 2) for #652/#653/#654 collectively; it is restated here
because it is independently verifiable on #653 itself and affects this
issue's own discoverability from #565, not just the reviewed sibling's.
**Recommendation:** same as the prior review — link via `sub_issue_write`,
not just prose.

## What's solid

- **The "no new element type" constraint is correctly grounded**: #315
  (FEAT-001) confirms adding a 36th `ElementType` requires a row in six-plus
  registry-keyed tables (`ElementRegistry.all()`, `PaletteContractTest`,
  etc.) — the Outcome's rationale for the constraint is accurate, not
  hand-waved, and the constraint itself (AC-2) is mechanically testable
  today against the shipped registry.
- **The geometry/placement boundary is honestly drawn and consistent with the family**: deferring layout to #654 (TASK-C565-3) rather than smuggling partial layout logic into this task keeps the two concerns separable, matching how #565's own boundary notes split synthesis from layout.
- **Feasibility of the mechanical part is real**: `Gate.setNumInputs` is
  unbounded and `Wire(WireEnd, WireEnd)` / `WireEnd(Circuit)` are plain
  constructible objects (`src/jls/elem/Wire.java:42`,
  `src/jls/elem/WireEnd.java:71`) — nothing in the element model blocks
  building a netlist programmatically without going through the GUI.

## Verdict rationale

`needs-rework`: the direction and boundary are sound and nothing here says
the task shouldn't be built, but three of the four acceptance criteria have
concrete defects an implementer or reviewer would trip on — a miscited
cross-reference (AC-1), a self-defeating escape hatch on the one criterion
that asks for real engineering (AC-3), and an underspecified comparison
method (AC-4) — plus an unnecessary sequencing dependency. All are text-only
fixes to the issue body; none requires re-scoping the task.
