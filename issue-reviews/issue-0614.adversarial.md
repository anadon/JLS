# Issue #614: TASK-C558-2: every Digital element maps to a JLS element by semantics from a written table, with the name only ever a hint
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#614 (TASK-C558-2) is the element-mapping child of #558 (FEAT-C29-2, the
`.dig`/Digital importer), sitting between #612 (TASK-C558-1, the hardened
XML parse) and #615 (TASK-C558-3, generics). Its four ACs — written mapping
table, semantics-not-name matching, explicit refusal over approximation,
and a structural net-partition check — are individually reasonable and
correctly reuse #323 (FEAT-025)'s name-collision precedent. The problems
are what the issue leaves unassigned: it asks for a structural connectivity
proof (AC-4) without anyone owning the connectivity-derivation algorithm
that proof requires, and it sits at exactly the point in the pipeline where
the parent's "no partial circuit" discipline (#558 AC-5) would have to be
enforced, but never mentions it — and the family's own child-task roster
suggests nobody else is assigned to either.

## Findings, most severe first

**1. [High] AC-4 demands a structural net-partition match against the source, but no child task owns computing Digital's own connectivity rule — and this repo's own research already shows that rule is non-trivial.**
AC-4: *"an imported circuit's net partition matches the source's, asserted
structurally rather than visually."* #612 (TASK-C558-1) scopes itself as
"Parsing only" and produces "a parsed, in-memory model" — it does not claim
to resolve connectivity. #614 doesn't claim it either; it just asserts the
partitions must match. Compare #323 (FEAT-025), the direct analog for
`.circ`: it spells out the source tool's connectivity model explicitly —
*"purely geometric: components connect by coordinate... computed as the
component origin plus a rule-derived offset δ(k, n, s) for component kind
k, input count n and body size s"* — precisely because an importer that
doesn't replicate that offset rule "produces circuits that import silently
disconnected — the worst failure mode available, because the file opens
and looks right." This repo's own verified research confirms Digital's
`.dig` format needs the same treatment: `docs/capability-roadmap/lf-06-diff-merge-vcs.md:650-652`,
sourced from a direct fetch of a real Digital file, states *"elements carry
no identifier at all, identity is `<pos x= y=>`, and wires are
`<wire><p1 x= y=/><p2 x= y=/></wire>`"* — i.e. coordinate-implied
connectivity, the same geometric model as `.circ`. Neither #612 nor #614
names who derives Digital's per-component port-offset rule, and #614's own
AC-4 is therefore unimplementable as a structural assertion until that gap
is filled. As written, an implementer could satisfy AC-4 by comparing net
*counts* on a handful of trivial fixtures while never deriving the general
offset rule — passing the letter of "asserted structurally" while leaving
exactly the silent-disconnection risk #323 was written to prevent.
**Recommendation:** add an AC (or a cross-reference to wherever it's
supposed to live) that names the connectivity-derivation rule as a
deliverable, mirroring #323 §3's explicit δ(k,n,s) treatment, before this
task is picked up.

**2. [High] The no-partial-circuit discipline is exactly this task's problem, and it isn't mentioned — nor assigned anywhere in the filed child-task roster.**
#558 (the parent) AC-5 requires import be "undoable and never silently
rewrites semantics; no partial circuit is ever emitted (the NetlistImporter
discipline FEAT-025 inherits applies here too)." #612's own boundary notes
assign this explicitly: *"the report and the atomic-import discipline are
TASK-C558-5."* But #558's body states only three child tasks exist —
"#612, #614, #615" (confirmed: no TASK-C558-4 or TASK-C558-5 issue was
found). #614 is the stage where AC-3's refusal case actually happens — "a
Digital element with no semantically equivalent JLS element is refused by
name and appears in the report as a refusal" — and it says nothing about
what happens to the *circuit* when that fires mid-import: does the whole
import abort per NetlistImporter's discipline, or does the mapper keep
going and emit a circuit missing that one element (and whatever wires
touched it)? As worded, AC-3 is satisfiable either way — an implementation
that "refuses and reports" element 37 of 50 while still emitting a circuit
built from the other 49 passes AC-3 literally while producing precisely
the silent-partial-circuit failure mode #558 AC-5 and #323 both exist to
rule out. **Recommendation:** either add an explicit AC to #614 stating
the whole-import-refuses-on-any-element-refusal rule (or its alternative,
argued), or file TASK-C558-4/TASK-C558-5 now and give #614 a real
dependency on the one that owns atomicity.

**3. [Medium] AC-2's collision fixture is asserted, not demonstrated, unlike its own cited precedent.**
AC-2 requires "a fixture containing a name that collides across the two
tools." #323, the rule this AC explicitly inherits, grounds the same
requirement in a real, already-shipped, verified collision: JLS's own
`HdlExporter.java:84` documents that `ShiftRegister` "holds no state" in
JLS while the same-named component is sequential in the source tool — a
concrete, checkable instance. #614 names no equivalent verified pairing
between Digital's component set (on the order of 50+ kinds, per the
sibling #558 review) and JLS's ~35 elements. As written, AC-2 can be
satisfied with a synthetic, invented collision that proves nothing about
what a real Digital circuit will do — the exact "gameable acceptance
criterion" pattern the family's own #323/#558 reviews already flagged
elsewhere in this cluster. **Recommendation:** identify at least one real
Digital/JLS name collision (or state that a survey found none, which would
itself be worth recording) before AC-2 is treated as scoped.

**4. [Medium] AC-1's citation to "FEAT-C29-2 AC-2" doesn't match what that AC actually says.**
AC-1 attributes "the code reads it rather than restating it" requirement
to `(FEAT-C29-2 AC-2)`. #558's actual AC-2 text is: *"The element mapping
is a written, reviewable table; mapping is by semantics with the name as a
hint... no mapping by name alone"* — it says nothing about a code/table
consistency requirement. That specific "code reads, doesn't restate"
obligation is original to #614, not sourced from the citation given. Minor
on its own, but it's the kind of provenance sloppiness the sibling review
of #558 already flagged as a broader pattern in this task family (thin
relative to #323's rigor). **Recommendation:** drop the parenthetical or
point it at the correct source; don't cite a parent AC for a clause the
parent doesn't contain.

**5. [Low] No `blocked_by`/`blocks` DAG edges, and the one dependency given is only half-resolved.**
`ordering_after` mixes a bare issue number (`323`) with a text label that
has no issue number attached (`"TASK-C558-1 (the parsed model this maps
from)"` — actually #612, but not written as such). #323 itself uses fully
mirrored `blocked_by`/`blocks` edges specifically so a DAG walk can find
them programmatically; #614 (like its siblings #612/#615/#558) uses the
free-text convention instead, so an automated consistency walk over
`blocked_by` would silently miss this whole task family. Already flagged
at the #558 level; repeated here without correction. **Recommendation:**
convert to `blocked_by: [612, 323]` with mirrored edges on both.

**6. [Low] Point-estimate cost with no derivation.**
`band_mw: "2"` is a bare point value, unlike every sibling/parent in this
family which at least gives a range (`"1-2"`, `"4-6"`) — a single number
reads as false precision for a task that includes writing a full semantic
mapping table across an unmeasured number of Digital component kinds, a
name-collision fixture, refusal-path plumbing, and a structural
connectivity proof (finding 1) whose cost depends on work nobody has
scoped yet. No task-row derivation is shown, matching the same critique
the sibling #558 review already made of the parent's un-derived band.
**Recommendation:** give a range and a one-line basis, or state it's
provisional pending the corpus/connectivity work in finding 1.

## What's solid

- AC-2's semantics-over-name rule is a correct, well-motivated reuse of
  #323's real `ShiftRegister` precedent, even though the fixture itself
  isn't yet grounded (finding 3).
- AC-3's refuse-don't-approximate discipline is consistent with the parent
  (#558 AC-3) and the sibling generics task (#615 AC-2) — the family is at
  least internally consistent on this point.
- The two boundary notes are correctly scoped and non-overlapping: generics
  are cleanly deferred to #615, and the report *carrier* is correctly left
  to #556 while #614 only owns report *content* — neither restates work
  another issue already owns.
