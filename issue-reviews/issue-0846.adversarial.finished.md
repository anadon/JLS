# Issue #846: TASK-C370-3: runtime state moves out of per-element objects into the primitive columns, the element author's contract is unchanged, and every simulation golden is byte-identical
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

TASK-C370-3, the third of six child tasks under FEAT-054 (#370), is the task the
feature is named for: move per-element simulation runtime state out of the
per-element object graph and into primitive-typed columns indexed by a dense
element index (introduced by TASK-C370-2, #843), while leaving the
element-author-facing `react` contract unchanged and every simulation golden
byte-identical. It has no comments and zero reactions; nothing has been
discussed since filing (2026-08-04).

## Findings, most severe first

**1. [High] The "react bodies don't change" premise conflicts with how the
current code actually reads per-element state, and the issue never explains
the mechanism that would reconcile the two.**

`Memory.java:1374` reads:
```java
writeGate = lastClock == 0 && clock == 1;
```
`lastClock` (`Memory.java:996`) is a plain `private int` field, read by raw
field access directly inside `react()` — not through a getter. If TASK-C370-3
moves `lastClock` into a primitive column indexed by element (which is
exactly what AC-1 demands — "per-element runtime state is read and written
through the columns, and the per-element object no longer holds it"), this
line has to become something like `columns.getLastClock(index)`. That is a
body-level rewrite of `react()`, not a signature change.

#370 §4 invariant 2 states plainly: "A child that requires every `react`
body to be rewritten has changed the contract, whatever else it achieved."
#846's own AC-2 only guards "none of the 27 `react` implementations changes
**shape**" — leaving "shape" undefined, so a body-only rewrite (same
signature, every field access replaced by a column accessor call) can pass
AC-2 while contradicting the parent invariant it is supposed to honor. The
issue owes either (a) a concrete indirection design (e.g., keep the fields
as-is on the object but have them proxy to the columns, so `react()` bodies
are genuinely untouched) or (b) an explicit acknowledgment that bodies will
change and a reconciliation with #370 invariant 2's wording. Neither is
present. AC-4 ("compiles and behaves identically... demonstrated with a real
element") checks the outside-in *effect*, not this internal mechanism, so it
will not catch the gap.

**2. [High] Hidden assumption: not all per-element runtime state is a fixed
scalar that fits a "column indexed by element." Memory's runtime state is
variable-size, and the issue never scopes what "runtime state" includes.**

`Memory.java:982-1224` shows the element's real runtime payload is a
`WordStore` — either `DenseWordStore` (`long[] words`, `Memory.java:1075`,
sized up to `DENSE_CAPACITY_LIMIT = 1 << 22`, `:1224`) or `SparseWordStore`
(`Map<Integer,BitSet>`, `:1159`) — plus `activity`, a bounded
`LinkedList<WriteRecord>` (`:1023`). None of this is a per-element scalar;
it is per-instance-sized, already-primitive-backed, and can dwarf the
`6.8 objects / 12-16 B header` arithmetic #370 uses to justify the whole
feature. AC-1's wording ("per-element runtime state is read and written
through the columns") does not say whether `mem`/`initMem`/`activity` are in
scope. If they are excluded, the task should say so and explain why (they
already avoid per-element object-graph overhead). If they are included, the
"column indexed by element" model as described does not obviously
accommodate variable-length per-element payloads, and the issue gives no
design for it. As written, AC-4 lets the demonstration element be a trivial
one (e.g. `Constant` or `Gate`, both scalar-state) and never force the hard
case, so the criterion can pass while the element type most likely to matter
for the capacity goal is left untouched.

**3. [Medium] AC-3 is a measurement, not a gate — the task can be accepted
even if it doesn't help.**

AC-3 requires the re-measured per-element heap to be "recorded" and "stated
as a range against both baselines," and explicitly says "the number is
committed regardless" if #370's Open Question 2 (the byte budget) is still
open. Checking #370 (issue #370) directly: Open Question 2 is listed as
still open and unchecked in its Definition of Done. So the actual capacity
kill criterion (K17-1, ≤150 B/element) is evaluated only later, at #370/#312,
not by this task. That means TASK-C370-3 can be merged, all five of its ACs
satisfied, and the achieved factor could be marginal or even a regression
(e.g. if columns are added *alongside* rather than *instead of* the object
fields during a messy migration) — nothing in #846 itself blocks that
outcome. This is a legitimate design choice (measure-then-gate-later is
#370's own stated pattern) but a reviewer should not read "AC-3 green" as
"the feature achieved its purpose."

**4. [Medium] The issue's own boundary note concedes it leaves the tree in a
state that violates its parent feature's invariant, for an unbounded
duration, inside a feature whose cost band is explicitly unowned.**

"Boundary notes": "The editor and the spatial index still read through
whatever compatibility path this PR leaves them; reconciling those readers
is TASK-C370-5 and TASK-C370-6, and #370 invariant 4 is not satisfied until
they land." #370 §4 invariant 4 reads: "No second representation maintained
by discipline. Either the direct readers consume the flat state, or they
consume a view provably in agreement with it. 'They are kept in step' is not
a design." Per #370's own "Cost" section, the whole feature is "Owner:
**UNOWNED** — this feature was added with CAP-17 after the capability
roadmap was committed, so no program pays for it." If TASK-C370-5 (#850) and
TASK-C370-6 (#851) — both still open, unstarted — are never funded, #846
leaves a permanent, unreconciled dual representation: exactly the
anti-pattern invariant 4 exists to forbid. The issue doesn't state a fallback
or a time-box for this exposure.

**5. [Low/Medium] The prerequisite ordering is asserted only in prose, not
enforced anywhere GitHub can check it.**

`issue_dependencies_summary` for #846 reports `blocked_by: 0, blocking: 0`,
and the same is true for #843 (TASK-C370-2, the task #846's own front matter
lists as `ordering_after`), #842, #850, #851. `has_parent`/`has_children` are
both `false` for #846 and for #370 (the feature it claims to be part of via
`part_of_feature: 370`). Nothing in GitHub's actual issue graph stops #846
from being picked up and merged before #843 lands, which would strand
TASK-C370-3 with no index/column store to migrate into. The ordering is
real and well-reasoned in the prose ("the index and the columns must exist
before state can move into them") but entirely unenforced.

**6. [Low] AC-1's verification standard is unspecified.**

"asserted structurally, not by inspection" doesn't say what structural check
would satisfy it. Compare TASK-C370-4 (#848), which is concrete about this
exact class of property: "the test fails if either side introduces a private
element→index map, asserted structurally (architecture rule or
equivalent)." #846 doesn't give AC-1 the same treatment — a reflection check
for "no fields on `Element` subclasses" would pass even if the state simply
moved into `Put`/`Input`/`Output`/`WireEnd` objects (still heap objects with
headers) rather than into the columns, defeating the point without failing
the letter of AC-1.

## What's solid

- AC-2 (byte-identical golden corpus) and AC-5 (`mvn verify`, no new SpotBugs
  exclusions, headless ratchet, no `.jls` format change) are concrete and
  map directly onto existing repo infrastructure — `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `VcdExportGoldenTest`, and
  `test/jls/HeadlessCoreRatchetTest.java` (all cited accurately in
  ARCHITECTURE.md and confirmed present in the tree).
- The "27 `react` implementations" figure is accurate: `grep` for `react(`
  under `src/jls/elem` returns exactly 27 files, matching #370's claim
  verbatim. The issue's factual premises are grounded in the real codebase,
  not invented.
- `Circuit.java:48` (`private Set<Element> elements = new HashSet<Element>();`)
  confirms the object-graph starting point the issue describes, even though
  the exact line cited in #370 ("47-48") is now a single line 48 post-generics
  — a cosmetic drift, not a substantive error.

## Recommendation

Before this task is picked up: (1) specify the indirection mechanism that
lets `react()` bodies stay untouched while backing storage moves (or concede
they won't and reconcile with #370 invariant 2); (2) scope AC-1 explicitly
against variable-size per-element payloads like `Memory`'s `WordStore`, and
require the AC-4 demonstration element to include at least one such case,
not only a trivial scalar-state element; (3) define the AC-1 structural
assertion precisely, mirroring TASK-C370-4's architecture-rule pattern; (4)
either land #846 only once #850/#851 are scheduled, or state explicitly how
long the tree is allowed to sit with an unreconciled dual representation.
