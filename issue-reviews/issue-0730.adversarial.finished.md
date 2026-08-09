# Issue #730: TASK-C554-3: the suite's output is machine-readable, so the perf doc and the scheduled lane consume it without anyone hand-editing a number
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

TASK-C554-3 is the third of three sub-tasks under FEAT-C28-1 (#554): #726 (TASK-C554-1)
builds the benchmark harness/command, #728 (TASK-C554-2) tracks the fixtures in-tree, and
this issue (#730) defines a stable machine-readable results file — schema, per-fixture
throughput/node-count/clocking-regime/environment fields — consumed by `docs/performance.md`
(#555) and the scheduled staleness lane (#557). None of #554, #555, #557, #726, #728, or
#442 exist in the tree yet; there is no `docs/performance.md`, no benchmark suite, and no
`simulation-budget.properties`. This is a from-scratch, forward-looking design task.

## Findings, most severe first

**1. AC2 is unverifiable at the time the work is actually done — a genuine circularity.**
The acceptance criterion reads: *"Every field #555's doc publishes and every field #557's
lane compares is present in that file; no consumer needs a value the format does not
carry."* #730's own `ordering_after` only lists `["TASK-C554-1", "TASK-C554-2"]` (#726,
#728) — it does **not** order after #555 or #557. Those two issues are themselves open,
unimplemented, and their bodies only sketch fields at a high level ("events/s and cycles/s
at stated node counts... hardware, JDK, flags" for #555; "compares against TASK-0026's
ceiling bands" for #557). Whoever implements #730 has to *guess* the complete field set two
not-yet-built consumers will need, then call the schema "stable" (per #730's own Outcome:
"stable, documented machine-readable result format"). If #555 or #557's real implementation
later needs a field #730 didn't anticipate — a git SHA, a warm-up-rep count, a statistical
method tag — the "stable" schema breaks and #730 has to be reopened or hand-patched, which
directly undermines the stated purpose ("a published number is never transcribed by hand").
As written, AC2 can be checked off by an implementer's best guess and cannot be objectively
verified until #555 and #557 land — an ordering gap, not just a wording nit.
**Recommendation:** either order #730 after #555 and #557 (inverting the current pipeline,
since a schema is easier to get right once its two consumers are specified), or explicitly
version the schema (e.g. a `schema_version` field plus an additive-only-fields contract) so
"stable" means "backward compatible," not "frozen and correct on the first attempt."

**2. The Boundary claim ("output contract only") contradicts AC3's actual scope.**
The Boundary section says: *"Output contract only; the doc is #555 and the lane is #557."*
But AC3 requires: *"The environment fields (hardware, JDK, flags) are captured by the
harness rather than typed in, so a number cannot be published under the wrong
methodology."* Capturing hardware/JDK/flags automatically is not a schema/format decision —
it is executable detection logic that has to live inside the benchmark harness (JDK version
via `System.getProperty`, hardware via CPU model/core count detection, which is nontrivial
and platform-dependent given README.md's supported matrix of Linux/macOS/Windows/RISC-V/
NixOS across x86_64, arm64 and riscv64). That is harness work. Sibling issue #726
(TASK-C554-1) explicitly claims ownership of "the harness" in its own Boundary line
("The harness. Fixtures are TASK-C554-2; the machine-readable output contract is
TASK-C554-3.") and yet #726's acceptance criteria say nothing about capturing environment
data — only command, clocking regime, and run-to-run tolerance. So the actual
environment-capture code is claimed by neither issue's stated boundary, or claimed by both
implicitly, which risks either a gap (nobody builds it) or duplicated/conflicting
implementations landing under #726 and #730 independently.
**Recommendation:** either move AC3 (or at least the *capture logic*, as opposed to *where
it's serialized*) explicitly into #726's scope, or amend #730's boundary to admit that it
also touches the harness, not just the output format.

**3. No field for measurement variance/tolerance, despite two upstream sources establishing
that raw single numbers are misleading here.** #726 (TASK-C554-1) AC4 requires: *"Running
the suite twice on the same machine produces figures within a stated run-to-run tolerance,
and that tolerance is reported alongside the numbers."* #442 (TASK-0026) independently
measured a **4.2x spread in ns/event across three reps in one JVM on one machine with no
code change** (issue #442, section O4) and concluded ceilings must be taken as
best-of-N-with-warm-up, never as raw single-run numbers, specifically because a two-sided
band or a raw figure "does not survive" that spread. #730's Outcome/AC list — "throughput
figures, the node count, the clocking regime, and the full environment" — never mentions
variance, rep count, or the aggregation method (mean vs. min vs. best-of-N). If #557's
staleness lane is meant to compare #730's suite output against #442's ceiling bands (as
#557's own body says: *"runs the committed benchmark suite ... against the ceiling bands
built by TASK-0026"*), and the two systems use different aggregation statistics (#442 is
explicitly ceiling/min-of-N-only; #730's file has no stated statistic), the comparison is
apples-to-oranges and #557 either can't be built cleanly on top of #730's schema or will
silently re-derive its own aggregation, defeating the "not build twice" principle #557
itself asks for. **Recommendation:** the schema must carry rep count, aggregation method,
and the tolerance/spread #726 AC4 already requires it to compute — this is a concrete,
inexpensive fix to an otherwise real gap.

**4. AC4's "committed consumer" requirement is gameable as worded.** *"A consumer reading
the file requires no hand-editing step, demonstrated by at least one committed consumer."*
Given the Boundary excludes building #555's doc or #557's lane, the in-scope "consumer"
is undefined — a one-line `cat results.json | jq '.fixtures[0].events_per_s'` smoke script
technically satisfies "no hand-editing" and "committed" without exercising the fields that
actually matter (environment block, clocking regime, multiple fixtures). **Recommendation:**
name what the token consumer must render (e.g., "renders at least one fixture's throughput,
node count, clocking regime, and JDK version") so the criterion can't be satisfied
vacuously.

**5. `band_mw: 0.5` likely underestimates the true scope once findings #2 and #3 above are
folded in.** As a pure "write a schema doc + one trivial reader," 0.5 is plausible. As
"design a stable schema that anticipates two unbuilt consumers, add real cross-platform
hardware/JDK/flag auto-detection to the harness, and settle an aggregation-statistic
question shared with #442's ceiling bands," it is not. This is a downstream consequence of
findings #1–#3, not a separate defect, but worth flagging since `band_mw` sizing is used
elsewhere in this repo's planning documents (e.g. `docs/capability-roadmap/`) to schedule
work.

## What's solid

- The Boundary's explicit fencing of ceiling bands and `simulation-budget.properties` to
  #442 is well-drawn and avoids the one duplication risk (a second gate) that #442, #554,
  and #557 all separately warn against building twice.
- The `ordering_after` on #726 and #728 (harness and fixtures before output contract) is
  the right sequencing for the parts of the schema those two issues *do* specify (events/s,
  cycles/s, node counts, clocking regime).
- Splitting "prose doc" (#555), "machine-readable contract" (#730) and "CI enforcement"
  (#557) into separate task-tier tickets is a reasonable decomposition in principle, even
  though the field-ownership seam between #726/#730 (finding #2) shows the split wasn't
  drawn quite cleanly enough in practice.
- The underlying motivation — "a published number is never transcribed by hand" — is a
  sound goal and matches this repo's existing discipline around ratchets/gates (see #442's
  very thorough treatment of the same general problem).

## Bottom line

The task's intent and most of its boundary are reasonable, but AC2 is not checkable against
anything that exists yet (a real ordering defect, not just ambiguous wording), AC3 quietly
claims harness-implementation scope that the Boundary disclaims and that a sibling issue's
own boundary also doesn't claim, and the schema as scoped omits the variance/aggregation
field that two other issues in this exact chain (#726 AC4, #442 O4) already established is
necessary. Recommend re-scoping before implementation: resolve the #726/#730 ownership
boundary for environment capture, add a variance/aggregation field to the schema, and either
reorder after #555/#557 or add explicit schema-versioning language so "stable" is honest.
