# Issue #618: TASK-C490-1: a drawn line between two nets carries an impedance and a delay, and its far end reflects — the closed-form superposition, with its truncation term count reported
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C490-1 is the "closed-form transmission-line element" child task of
FEAT-059 (#490), itself the third rung of CAP-18 (#313). The physics and
the citations that are checkable against the repo (`WireNet.java:405`,
the sealed `Element` hierarchy, the registry/palette counts) all check
out exactly. But the task is filed as ready-to-work while two of its own
stated prerequisites, plus one unstated one, have landed zero code, and
its acceptance criteria never gate on the numeric correctness that its
parent feature treats as the whole point of the exercise (K18-1). Both
are fixable by rewording, not by re-deriving the physics.

## Findings, most severe first

### 1. (Critical — feasibility) The task cannot be honestly completed today; a load-bearing input attribute does not exist, and its source feature isn't even listed as a dependency

AC1 requires `Td in seconds against FEAT-047's time base`. `src/jls/core/`
at HEAD contains only `Bounds.java, Geometry.java, GridPoint.java,
GridSize.java, Orientation.java, SegmentGeometry.java, TextMetrics.java,
package-info.java` — no `TimeBase.java`, and `grep -r TimeBase src/`
returns nothing. FEAT-047 (#367) is open with "Nothing of this feature
exists" recorded in its own evidence section. `docs/simulation-semantics.md:26-27`
is still normative: *"Simulation time is a dimensionless non-negative
64-bit integer... Time units are abstract; nothing binds them to
seconds."* So AC1's own literal claim — Td expressed in seconds against
a declared time base — has no target to bind to.

Worse, AC2 requires *"a piecewise-linear ramp of the declared `t_r`"*.
That attribute belongs to FEAT-058 (#486), which is also open, with its
own evidence stating the transition-time attribute is "Not filed" and
`Adder.java:33`'s `defaultPropDelay` is still "one integer, no unit, no
transition time." **#618's YAML front matter lists `ordering_after:
[367, 487]` — #486 appears nowhere**, even though #490's own "Consumes"
section says this element "Consumes... FEAT-058's two declared attributes
(#486)" and #490 elsewhere states "487... permanence ordering, not
data" (i.e., 487 is listed for scheduling, not because #618 reads its
output). The one dependency that actually supplies AC2's input data is
absent from the task's own dependency list.

Recommendation: add #486 to `ordering_after` explicitly (even if inherited
transitively through #487's `blocked_by`), and do not schedule this task
for execution until #367 and #486 have landed real code — a task that
asks for "Td in seconds" and "the declared t_r" cannot be graded against
anything until those exist.

### 2. (High — gameable acceptance criteria) No acceptance criterion requires the superposition to be numerically correct

FEAT-059 (#490) treats this exact scope — "the closed-form
transmission-line element" — as gated by K18-1: *"The golden corpus and
the analytic cross-check must be green **before** the dialog, the
renderer and the palette entry are built... agreeing with the closed-form
lattice to **1e-12 relative**."* #490 even ships the acceptance fixture
(3.3 V / 50 Ω / 345.6 ps / 10 Ω into open → peaks 5.5000, 1.8333, 4.2778,
2.6481 V...).

None of that appears in #618. AC1–5 as written can be satisfied by an
element that registers, saves/loads, "simulates" (produces *some* output),
reports *some* term count, and never touches `WireNet.propagate` — without
ever checking that the computed voltages match the closed form. "Evaluated
as an exact geometric superposition" (body text) is an implementation
description, not a test oracle; nothing in the AC list names a value, a
tolerance, or a golden to check against. As filed, an implementer could
land a plausible-looking but physically wrong reflection model and every
stated AC would still pass.

Recommendation: fold in (or explicitly cross-reference and block on) the
"four-termination golden corpus and its independent analytic cross-check"
task from #490's own decomposition, with the specific worked values
(5.5000 V, 1.8333 V, 4.2778 V, 2.6481 V, 4.3675 V at 1 ns edge) and the
1e-12 relative tolerance, as an acceptance criterion of *this* task, not
a deferred one.

### 3. (High — internal contradiction) The truncation formula, as stated, does not produce "1 term" for a terminated line

AC3 gives `N = ceil(log tol / log |Gamma_s * Gamma_L|)` and in the same
sentence asserts *"1 term for any terminated one"*. But for a matched
termination `Gamma_s = 0` or `Gamma_L = 0`, so `|Gamma_s * Gamma_L| = 0`,
and `log(0) = -Infinity` (IEEE 754 double, the type this codebase uses
throughout — see `BitSetUtils`/`SimEvent` for the numeric idiom). Plugging
in: `ceil(log(tol) / -Infinity) = ceil(0) = 0`, not 1. The formula as
literally written yields **zero** terms for the terminated case the AC
itself calls out as needing one — a naive implementation either emits no
terms (silently wrong output: V stays at 0 instead of settling to
`V_final`) or must special-case `Gamma_s*Gamma_L == 0` to clamp `N` to at
least 1, and the AC never says so.

Recommendation: state the clamp explicitly — `N = max(1, ceil(...))`, or
equivalent — rather than leaving the reader to notice the formula
contradicts its own worked example.

### 4. (Medium — underspecified) What the element drives onto the *far* ordinary net is never stated

AC1 says the element "simulates between two ordinary nets." Per
`docs/simulation-semantics.md` §2, an ordinary net's value is a two-state
`BitSet`-or-null — there is no continuous voltage in that domain. But the
entire pedagogical payload of this feature is a continuous, ringing
reflected voltage (5.5 V on a 3.3 V rail, etc.), which per #490's own
decomposition is surfaced through a *separate* task (the real-valued
trace-window row) that #618 does not claim. AC1 never says what BitSet
value — if any, and by what voltage threshold — the far end drives onto
the downstream ordinary net for other elements' `react` calls to consume.
Without that contract, a literal, minimal implementation could satisfy
"simulates between two ordinary nets" by ignoring the reflection physics
entirely for logic-level purposes and only reporting the analog numbers
on the side, which would be a materially weaker element than the one
described in the abstract.

Recommendation: state explicitly whether AC1 requires only the analog
side (deferring any digital pass-through to a later task) or whether the
element must also drive a thresholded digital value onto the far net, and
name the threshold.

### 5. (Medium — evidence quality) The "66 lines across 12 files" baseline this task is graded against is a half-share of a two-element commit, not a measurement of one element

The commit cited by #490 as the empirical basis (`git show --stat
38a0544`) adds **two** element types simultaneously (`RegisterFile.java`,
569 lines; `FieldExtend.java`, 486 lines) sharing the same 12
registration-surface files. Excluding the two element-class files, the
remaining insertions across those 12 files total 133 lines (plus 30
deletions, dropped from the average entirely) — `133 / 2 ≈ 66.5`, rounded
to "66." So the number isn't a measured single-element tax; it's an
average over a batch commit that shared file-touch overhead between two
elements, with the deletion side of the diff discarded. AC5 asks that
*"the registration tax is counted at landing against the measured
66-lines-across-12-files figure, with any difference explained rather
than absorbed"* — but this element also needs scope the reference commit
never touched (a new value domain / real-valued trace hookup, if AC1 is
read broadly per finding 4), so there's good reason to expect it to cost
more, and the AC doesn't acknowledge the baseline's own derivation is an
estimate rather than ground truth.

Recommendation: either re-derive the baseline from a genuinely
single-element commit, or reword AC5 to say explicitly that 66/12 is a
halved two-element average, so "explained rather than absorbed" isn't
graded against a number that looks more precise than it is.

### 6. (Low) Sibling global invariant (K9: palette count must not rise) isn't restated here

#490's invariant 4 is a hard constraint on this exact rung: *"The palette
count does not rise... at `2d0ca9d` the registry holds 35 types against
32 palette entries... after this feature the registry count rises and
the palette count must not."* Verified live at HEAD: `ElementRegistry`
has 35 `new ElementType(...)` rows, `Palette.java` has 32 `entry(Group.`
rows — the same figures, confirming this element hasn't landed yet.
AC1's "places" could be read as "gets a normal palette button," which
would break `PaletteContractTest.paletteIsTotalOverTheElementRegistry`
unless gated by the (separately-scoped, in #490) context-derived
visibility rule. #618 doesn't mention this constraint at all, so a reader
working from this task alone could reasonably ship a naive palette entry.

Recommendation: add a line noting that "places" means placeable via the
editor/test harness, not necessarily a default-visible palette button,
and cross-reference the K9 invariant.

## What's solid

- AC4 (`WireNet.propagate` untouched, no format-version bump, every
  golden byte-identical) is precise, falsifiable by `git diff` plus the
  existing round-trip/golden suite, and consistent with how the
  `RegisterFile`/`FieldExtend` precedent (#163, commit `38a0544`) landed
  a new element type without a version bump.
- The `WireNet.java:405` citation (`private @Nullable BitSet value = new
  BitSet(1);`) is verified verbatim against HEAD, and the element-vs-net-kind
  reasoning built on it is sound given the codebase's one-value-per-net
  model.
- Licensing: the parent feature (#490) documents that the kernel is
  original textbook derivation with no absorbed code, so this task
  carries no GPL-compatibility exposure.
- The `Gamma_s = -2/3`, 52-terms-at-1e-9 worked figure in AC3 is
  independently correct arithmetic (`ln(1e-9)/ln(2/3) ≈ 51.1 → 52`) for
  the case it's actually describing (open far end).
