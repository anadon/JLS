# Issue #363: FEAT-035: a running simulation can be written to disk and resumed as the byte-identical continuation — same time, same pending events, same memory and register contents
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The issue is well-grounded: every code-level claim I checked against the working
tree holds up (`Circuit.save` at `src/jls/Circuit.java:1466` takes only a
`PrintWriter`; `Register.setValue` at `src/jls/elem/Register.java:311-329`
resets `currentValue` from the authored `initialValue`; `Memory`'s running
store (`WordStore mem`/`initMem`, `src/jls/elem/Memory.java:982,987`) is
`@Nullable` and rebuilt by `initSim` at `:1245`; `BatchSimulator.pause`
(`:87-90`) is byte-for-byte `stop()` (`:75-78`); `SimEvent.sequence` is
`private static long` at `src/jls/sim/SimEvent.java:85`; the file-format
grammar at `docs/file-format.md:125-135` has no binary item kind). The
`blocked_by`/`blocks` mirrors to #319, #333 and #350 are all present and
consistent in both directions — no half-edges found. The decomposition
(test-first, engine-before-elements, shared-task discipline) is a coherent
answer to a genuinely hard problem. That said, several concrete risks would
let the stated verification pass while the real goal — a truly
byte-identical, trustworthy resume — silently fails.

## Findings, by severity

**1. (High) The one field explicitly named for serialization has a
non-deterministic identity in its own hash.** The issue lists "the
duplicate-check state" among the engine state TASK-0074 must capture,
backed by `Set<SimEvent> dupCheck` (`Simulator.java:27`). But
`SimEvent.hashCode()` mixes in `System.identityHashCode(callBack)`
(`SimEvent.java:189`, comment at `:178`: "the callback's identity hash").
Identity hash codes are not guaranteed stable or reproducible across JVM
invocations (HotSpot's default generators are heap-layout- or
PRNG-derived), so `HashSet` bucket/iteration order for `dupCheck` is not a
deterministic function of simulation content. If TASK-0074's serializer
walks the set in iteration order, two serializations of the *same* live
`Σ_t` in two different process runs can legitimately produce different
bytes — directly violating the "byte identity, not structural equivalence"
standard the issue sets as its own definition of correctness (§3, the
commuting-square clause). The issue never states that `dupCheck` must be
serialized in a canonical, content-derived order (e.g., sorted by
`(time, seq)`) rather than iteration order. This is exactly the kind of
"one uncaptured mutable field" the issue itself warns about in §2, sitting
unaddressed in the one field it names explicitly.
**Recommendation:** add an explicit requirement to TASK-0074 (or as a
Global Invariant) that `dupCheck` is serialized in a canonical order
independent of `Object`/identity hash codes, and add a CI check that
re-serializing an unchanged live simulator twice (in two separate JVM
processes) yields identical bytes — not just that resume-then-reserialize
is a fixed point within one process.

**2. (Med-High) The critical-path task is gated on an open question that
duplicates an already-unresolved open question in its own prerequisite.**
§ Open Questions Q1 ("Section or sidecar?") explicitly "Blocks filing
TASK-0074" — the single largest costed line item (2 wk) on the critical
path. But #319 (FEAT-013, the sole `blocked_by` entry) carries its *own*
unresolved Open Question 4: "Where does a guest image live — this frame,
or a sidecar?" — the same bulk-binary-residence axis, phrased almost
identically, and also explicitly blocking (#319's Q4: "Blocks integration
of I7"). #363 never cross-references #319's Q4 or proposes that the two
be decided together. Two issues each carrying an independent unresolved
decision about where large binary payloads live is a real risk that they
get answered differently (sidecar vs. section) by two different people at
two different times, which #319's own §7 calls out generally ("a second
file kind is a second compatibility surface") but #363 doesn't apply to
itself.
**Recommendation:** have #363's Open Question 1 explicitly say "resolve
jointly with #319 Open Question 4" (or merge them into one decision
record), so the maintainer answers the residence question once for both
the guest-image and checkpoint consumers rather than twice.

**3. (Med-High) "Written first" round-trip test conflicts with the
project's blanket "mvn verify green" norm.** §6 says TASK-0075 (the
round-trip property, `replay(ckpt[i]) == ckpt[i+1]`) should be "written
first ... it is a test that fails until TASK-0074 lands." Global Invariant
6 in the same issue requires "`mvn verify` green, coverage floors held" at
every landing, and README.md states the same as a repo-wide norm
("Changes should keep `mvn verify` green"). The issue never says how a
test that is *known to fail* before TASK-0074 exists coexists with that
gate — no `@Disabled`, no separate non-required CI lane, no "land as a
stub returning UNKNOWN" convention is specified. As written, either (a)
TASK-0075 cannot actually land first without breaking the green-build
invariant every child is bound by, or (b) it lands disabled/stubbed, which
quietly defeats the stated purpose ("written before the thing it
verifies" as "the feature's central claim," per §7's note). This is a
concrete acceptance-criteria gap in the part of the plan the issue itself
calls central.
**Recommendation:** state explicitly how TASK-0075 lands pre-TASK-0074
(disabled with a tracking issue, `@Tag("checkpoint-pending")` excluded
from the required `mvn verify` run, etc.), and add that mechanism to
Global Invariant 6 or its own Definition-of-Done line.

**4. (Med) The stated round-trip property can be satisfied by a
consistently-wrong serializer.** §3's commuting square
(`deser(ser(Σt)) = Σt`) and §5 criterion 1 are checked purely against the
serializer's own output — `ser`/`deser` are each other's only oracle. A
serializer that silently and *deterministically* drops or canonicalizes a
field (e.g., always re-deriving something instead of round-tripping it)
still satisfies `deser(ser(x)) == x_reconstructed` and produces an
identical waveform tail, because the "error" is reproduced identically on
every run — nothing in §5 cross-checks Σ_t against an independent
state-equality definition (e.g., structural field-by-field diff of the
live simulator against a freshly-`initSim`'d one at the same logical
point, for at least the resumed run vs. an *un-checkpointed* continuation
computed a different way). This is the same failure mode the issue names
in §2 ("a checkpoint that resumes to a nearly identical continuation ...
is exactly what a code review does not catch") but for the *deterministic*
sub-case, which the stated CI plan does not close.
**Recommendation:** add an integration criterion that compares the
resumed run's output against an independently-produced uninterrupted run
using a *different* code path than the serializer under test (e.g., the
existing golden suite run to completion with no checkpoint at all,
diffed against checkpoint-at-t + resume), not only checkpoint-to-checkpoint
self-consistency.

**5. (Med) The largest, least-bounded piece of scope carries no estimate
and is presented alongside four well-bounded tasks as if comparable.**
The residual — "the per-element-kind state mapping across the registered
vocabulary, and the refusal list it implies" — covers roughly 35 element
types (per the issue's own count) with no task id (registry closed at
TASK-0112), no estimate, and no roster row beyond "Not filed." The issue
is commendably explicit about this in the Cost Reconciliation ("Do not
read 6.5 wk as the feature... the gap is the fifth roster row"), but the
top-line framing ("Four tasks in front of a residual") and the roster
table still read as if 6.5 wk / 4 tasks were the shape of the work, when
by the issue's own math the unshared, estimated remainder is 3 wk and the
actual long pole (Memory's dense/sparse `WordStore`, wire nets,
subcircuit boundaries, any future host-I/O element) is unsized.
**Recommendation:** before work starts, size the residual by a quick pass
over the current ~30-35 element classes (ARCHITECTURE.md's own "Adding an
element today" count), even roughly, so "sound-with-concerns" doesn't
quietly become "underscoped" six weeks in.

**6. (Med) The evidentiary base cited for quantitative claims (not the
code claims) is unreachable from the checked-out tree or from `master`.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is **not** an
ancestor of `master`/HEAD (verified: `git merge-base --is-ancestor 2d0ca9d
HEAD` fails); it exists only on the unmerged branch
`origin/claude/jls-virtual-hardware-linux-njsoma`. I independently
spot-checked every `src/jls/*.java` citation against current HEAD and they
all still hold (the divergence point, PR #294, is close enough that code
drift is minimal) — so the *code* evidence is trustworthy in practice.
But the qualitative citations — `docs/plan/evidence/BRIEF.md` §7's "fatal"
grading of the serialization gap, and the specific numbers attributed to
`diff-stability.md` R6 — point to files that do not exist on `master`,
do not exist in the current working tree, and were later **deleted even
from the side branch that introduced them** (`742da74 "docs: remove the
planning corpus now that it is encoded in issues"`). They are recoverable
only via `git show 3a81a4a:docs/plan/evidence/diff-stability.md` on a
branch most contributors will never check out. A reviewer cannot casually
verify the cited numbers.
**Recommendation:** either inline the specific quoted figures directly
into the issue body (already partly done) and drop the unreachable
document citation, or restore the evidence doc to a location reachable
from `master`.

**7. (Low-Med) The one concrete example given for the refusal list names
a feature that does not exist yet.** The Capability Statement says "A run
holding an open host byte port ... refuses to checkpoint with the
reason" — but `grep -rn "byte port\|BytePort" src/` returns nothing; no
such element exists in the codebase (confirmed also by #347's own
evidence section: "no host byte port and no Console element exist").
Using a not-yet-built element as the sole worked example of "named
refusal" means the one illustration of the refusal-list contract can't be
checked against anything real today.
**Recommendation:** either use an example that exists today (e.g., a
hypothetical future element generically, without naming host-byte-port
specifically) or note explicitly that the example is forward-looking.

**8. (Low) Process overhead is uncounted.** The issue's formal apparatus
(a commuting-square equation, a Σ_t state calculus, mirrored `blocked_by`/
`blocks` edges across a 100+-issue DAG) is heavy for a project
ARCHITECTURE.md itself describes as a "single-maintainer pedagogy tool."
None of the cost bands account for the ongoing tax of keeping this
document network internally consistent as other features land or
re-plan around it (see #319's own multi-paragraph edge-removal example
for #343, which is exactly this tax being paid once already).
**Recommendation:** no action required for filing, but the eventual
close-out should budget time for DAG upkeep, not just the coding tasks.

## What's solid

- The core problem statement is accurate and load-bearing: `Circuit.save`
  genuinely has no simulator/state argument, and `Register`/`Memory`
  genuinely discard running state — verified directly against source.
- `blocked_by`/`blocks` edges to #319, #333, #350 are correctly mirrored
  in both directions; no orphaned or contradictory ordering edges found.
- Scoping out #19 (editor autosave `.jls~`) as a different artifact is
  correct and pre-empts a real naming collision.
- The TASK-0075-before-TASK-0074 methodological stance (test as the
  definition of done, not a verification afterthought) is the right
  instinct for a feature whose entire value proposition is byte identity
  — the gap is only in how that stance interacts with CI gating (finding 3).
- The self-diagnosed cost-band-vs-row-sum mismatch (finding 5) is honest
  disclosure, not concealment — commendable even though the underlying
  risk remains real.

## Verdict rationale

Not `should-not-proceed`: the problem is real, the code-level grounding is
accurate, and the decomposition is defensible. Not `needs-rework`: no
finding here invalidates the plan's shape — they are gaps and one
concrete correctness risk (the `dupCheck` identity-hash issue, finding 1)
that should be fixed **before** TASK-0074's serializer is designed, not
reasons to redo the roster. `sound-with-concerns` fits: proceed, but
resolve findings 1-3 (determinism of `dupCheck`, the duplicated open
question with #319, and the test-vs-green-CI interaction) before or
during TASK-0074, and size the residual (finding 5) before treating 6.5 wk
as the budget.
