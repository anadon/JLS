# Issue #409: TASK-0031: a file that parses but is structurally corrupt is reported by name, so a merge can never hand back a circuit nobody drew
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two things are bundled here, and only one of them is a task.

The first is a **loader defect that destroys user data**. `src/jls/elem/WireEnd.java:104`
puts the attachment-resolution block inside the wire loop opened at `:102`, so a wire end
with zero `ref wire` items never resolves its `attach`/`put` declaration. The consequence
O2 measures is not "a corrupt file loads quietly" — it is that JLS reads a declared
connection, discards it, and then **re-saves the file without it**, because
`WireEnd.save` (`src/jls/elem/WireEnd.java:597`) emits `ref attach` only when
`isAttached()`. The file the student opens is not the file the student had.

The second is a **capability**: JLS has no answer to "is this circuit well-formed?" that
lives anywhere except the editor's mouse handler. That is the real gap, it is the "or"
branch of #356, and it is worth building.

The issue merges them, and the merge is what I want to change. It treats the loader
defect as *evidence for* the validator, recommends Open Question 1 option (a) — teach
`WireEnd` to carry a residue recording that the loader failed to do its job, so the new
checker can observe the failure — and pushes the one-line fix into "adjacent work
discovered en route... filed as new issues." That inverts the leverage. Below: three
reframings, ordered by how much they change.

## 1. Fix the nesting, do not build a subsystem to observe it

Option (b) is a dedent. It is also, on inspection, a **security fix**, which the issue
does not say. The three throws of O5 that the issue is careful to preserve — dangling
attach element, non-existent named put, and the double-attachment guard
`p.isAttached() && p.getAttached() != this` — all live inside the same mis-nested block.
So at HEAD **none of them runs for a wire end with zero wire references**. The
double-attachment guard exists specifically because #58 found that silently honoring the
last attachment simulates a subtly wrong circuit; #38 and #170 classify `.jls` bytes as
hostile input. A hostile file therefore reaches an unguarded path today by the trivial
expedient of omitting `ref wire`. The issue's §7.11 asserts those four rejections stay
load failures (P8) while its own O1 shows one of them is conditionally unreachable — and
its plan leaves that unreachable for however long TASK-0005 → TASK-0031 takes.

The stated objection to (b) is that "previously-lossy files load differently." Consider
which files: a `WireEnd` block carrying `attach` + `put` + zero `wire` items. JLS itself
cannot write one (`save` guards on `isAttached()`, and an attached end reached through the
editor's connect gesture always has the wire that carried it there). The affected set is
exactly the corrupt-merge set — the population this whole feature exists to serve — and
for them "loads differently" means "keeps the connection instead of deleting it." That is
the fix, not a risk of the fix.

And it dissolves the issue's own worst threat to validity (§11, second bullet: "the check
runs after `finishLoad`, so it sees a circuit the load path has already normalised"). With
the block dedented, the put resolves, the object graph is truthful, and the flagship
finding becomes an ordinary read over the loaded circuit — an *attached end driving no
wire* — with **no residue field, no new load-time state, and no `SemanticCheck` access to
`loadAttach`/`loadPut`**. Option (a) buys the checker a memory of a bug; option (b)
removes the bug and leaves the checker a simpler question to ask.

**Concretely: file the dedent as its own defect issue, land it this week, and rebase this
task on the fixed loader.** It is hours of work, it stops data loss now, it restores a
guard, and it deletes Open Question 1 — which the issue marks *blocks execution*.

## 2. The check set has a principled source, and the issue is not using it

The title says "structurally corrupt in ways the editor would never let anyone draw."
That sentence names the closure argument, and then the issue declines to use it: H1's six
classes are "chosen from measured merge failure modes, not hypotheticals," with #356's
nine-scenario matrix as the completeness oracle. An enumeration of observed failures has
no closure property. "Everything the editor's connect gesture refuses" does.

Look at what `SimpleEditor.canConnect(WireEnd, Put)` (`src/jls/edit/SimpleEditor.java:4229`)
already enforces, per gesture: end must be dangling; put must be unattached; bit widths
must agree; no second input on a net unless both sides are tri-state; no tri-state/normal
mixing in a bundle; no multiple drivers. That is *four of this task's six classes* plus
two the task never lists — stated once, in code, by the person who defined what "drawable"
means. #356 §1 already records the smell: the same three-line width check appears
identically at `:4014`, `:4141`, `:4246`, `:4357`. This task proposes to write it a fifth
time, in a different vocabulary, over a different data shape, and then §11 worries that its
copy will drift from the simulator on tri-state circuits.

**The seam to cut along is a single predicate module — `jls.core.CircuitRules` — that
answers legality over circuit state, with two callers:** the editor asking about a
hypothetical post-gesture state, and the load path asking about a whole circuit. Then:

- The four duplicated width checks collapse into one, which is cleanup #356 has already
  flagged and nobody owns.
- Drift between "what the editor allows" and "what the checker demands" becomes
  structurally impossible rather than a discipline the §11 bullet hopes for.
- #356's Open Question 3 — "which invariants are format-level rather than editor-level?"
  — is answered by construction: *all of them are*, because there is one set. That
  question is currently marked as blocking arming.
- H2 (zero false positives on the corpus) gets an argument rather than a fixture count.
  A rule the editor enforces on every gesture cannot fire on a file the editor wrote;
  three tracked fixtures (O9) is not evidence, it is a spot check, and §11 admits it.
- The one class with no editor counterpart — reference integrity over `sref` — stays
  where it belongs, in the loader, and is the only part genuinely blocked on TASK-0005.

This also unblocks the task's other execution-blocking question. Open Question 2 asks
whether the check runs on `CircuitSnapshot` restore, and worries about per-undo cost. With
rules single-sourced, an undo snapshot is not untrusted input: it was produced by this
process from a state the gesture guard already validated. The hazard #356 invariant 5
names is the *loader's leniency*, not the snapshot's provenance — and the right coverage
for it is a test property (every generated circuit round-trips and validates clean, in
`GenerativeRoundTripFuzzTest`), not a runtime cost on every Ctrl-Z. Check at the boundary
where foreign bytes enter; assert the interior in the suite.

## 3. Half these findings are a tax on how a wire is encoded — say so before #334 freezes

`Wire.save` (`src/jls/elem/Wire.java:123`) is a no-op: *"Wires don't get saved."* A wire is
an edge stored twice, as reciprocal `ref wire` items on two `WireEnd` records. Every one of
the merge scenarios driving this task is a consequence of that: one side deletes a record,
the other keeps its half, and no textual merge can see that the two halves were one object.
O2 is precisely "the edge lost one endpoint." A dangling `ref wire` is "the edge lost the
other." Interestingly, an *asymmetric* `ref wire` heals silently today, because
`WireEnd.init:147-149` calls `addWire` on both ends — so the format is redundant, and the
loader already treats one side as authoritative.

If a wire were a single record — `ELEMENT Wire` with `sref end1`, `sref end2`, and its
probe — then "one side deleted the wire, the other kept the end" is a delete/modify
conflict on one record, which git reports natively, with no validator, no rule row and no
finding category. The problem does not get detected better; it stops existing.

This task correctly disclaims format ownership. But #334 (FEAT-003) is *actively reopening*
canonical text and reference form, and TASK-0032 is about to write a per-record-kind merge
rule table over whatever kinds exist when it runs. Freezing "a wire is an invariant coupling
two `WireEnd` records" into a validator, a rule table and a git driver is a decision made by
default. **Post one comment on #334 asking whether the wire edge becomes a record in the
`sref` epoch, before TASK-0032 writes rules against the two-record form.** Cost: one
comment. Value: possibly deleting two of the six classes and one rule row permanently.

## 4. Verification: search for the classes instead of enumerating them

H1 is refutable only by someone imagining a scenario the six classes miss — a falsifier
that depends on the falsifier's imagination. The repo already has the machinery for a
better one: `ContainerMutationFuzzTest` and `GenerativeRoundTripFuzzTest`. Generate a
circuit, canonical-save it, apply a *merge-shaped* line mutation family (delete a record,
duplicate a record, graft a record from a sibling save, drop one item line), reload, and
assert the trichotomy #356 actually promises: **load fails, or the check reports, or the
result is a circuit the gesture guard would have permitted**. Any input landing outside
those three is a named, reproducible refutation of H1, found by search rather than by
committee. That is also the only form of evidence that makes "$|S| = 2$ is really 3" (O4)
a converged number rather than the next surprise.

## Where I agree without reservation

Findings as data with a stable rendering, not booleans. No `default:` arm. Purity asserted
against the canonical-save oracle rather than assumed. One code path for the person, the CI
lane and the merge driver. `-check` on stderr because `docs/batch-interface.md` §1 promises
stdout is results — that is the kind of contract-awareness this tracker does well. And the
exit-code answer in Open Question 3 is right and generalizes: "loaded with findings" is not
a runtime failure and must not reuse code 1; give it its own code and record it, because
ARCHITECTURE.md's 0/1/2 table is the thing every autograder reads.

## What I would do with this issue

Keep it. Change its shape:

1. Split out the `WireEnd.init` dedent as a standalone loader defect and land it first.
   It stops data loss, restores the double-attachment guard for zero-wire ends, and
   deletes Open Question 1 (currently execution-blocking).
2. Reframe the deliverable as **extracting** the invariant set the editor already
   enforces into one shared predicate module, with the load path as a second caller —
   not as authoring a parallel one. This answers H2's completeness, kills the drift
   threat, collapses a four-way duplication, and resolves #356 Open Question 3.
3. Replace H1's enumerated six with the mutation-fuzz trichotomy above; keep the six as
   the named categories the search is expected to land in.
4. Answer Open Question 2 as "load boundary only, with a round-trip property in the
   suite" rather than "run it on every undo."
5. Post the wire-as-one-record question on #334 before TASK-0032 begins.

I am explicitly setting aside the completion criterion that the nesting fix be deferred to
a follow-up issue, and the recommended answer (a) to Open Question 1. Adding a field to the
model whose only purpose is to remember that the loader dropped something is the wrong
direction for a project whose stated arc is fewer places where a circuit's meaning is
defined, not more.
