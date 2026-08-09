# Issue #382: TASK-0037: an op applies with no drawing context, and every editor gesture goes through the op vocabulary
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two capabilities hide behind one signature change. (1) *Construction*: a program,
an importer (#304), a grader (#300), a generated RV32 machine (#326) must be able
to build and mutate a circuit on a machine with no display. (2) *Convergence*:
a replica applying a peer's op must land on the same circuit the peer did (#352,
#171). The parent feature #337 states both, and states the mechanism correctly in
its §3: *"in the GUI a `FontMetrics`-backed one, in headless a **deterministic**
one."* #382 drops the word "deterministic" and substitutes `null`.

That substitution is the whole review. Everything else in the issue is careful
and correct.

## 1. The nullable contract is the defect, not the invariant to preserve

H1 promises to preserve "null means skip sizing" exactly, and §11 defends the
nullability as load-bearing. Both are true statements about a contract that does
not survive contact with either capability above.

`src/jls/elem/Pin.java` `init` and `src/jls/elem/Constant.java` `init` size
themselves only `if (g != null)`, and only when `width == 0 && height == 0`. That
guard is right for *load* — the file already carried the width. It is wrong for
*construction*: a freshly built `Pin` under null metrics keeps `width = height = 0`,
and `Constant.init` then places its `Output` at `x = width`, i.e. at the origin.
Worse, `src/jls/elem/Pin.java` `rotate` does `width = 0; height = 0; init(g);` —
so `RotateElement.apply(circuit, null)` silently *destroys* geometry on an
already-correct element. Only `Gate` and `WireEnd` override
`sizeIsRecomputedOnLoad` (`src/jls/elem/Element.java:669-678`), so for everything
else that zero is then written to the save.

Consequences for the issue's own predictions:

- **P2** ("apply every kind with a null `TextMetrics`; observe no exception and
  the expected mutation") passes a rotate that zeroes an element. "No exception"
  is not the property that matters.
- **P4** ("apply-then-invert with no drawing context; observe byte identity")
  should *fail* for `RotateElement` and `FlipElement` on any font-sized element:
  rotate CW then CCW under null metrics leaves `width` at 0 where the pre-op save
  had a real width. The existing suite never sees this — every call site in
  `test/jls/collab/op/CircuitOpTest.java` passes a `BufferedImage` `Graphics2D`
  (`graphics()`, `:212`), and nothing exercises null today.
- **#337's integration criterion 1** (GUI/headless byte parity for the same
  construction sequence) is unreachable by this design, because the headless path
  has no metrics at all.

The issue's H1 falsification response points at "a second AWT dependency the seam
does not cover." That would be the wrong diagnosis. The right one: *null is not a
headless metrics implementation.*

#337 already knew this. Its Open Question 4 — *"Does the headless `TextMetrics`
implementation ship in main or test? Recommended default: `src/`."* — is marked
**"Blocks TASK-0037."** #382 declares `blocked_by: []`, never cites the question,
and ships no implementation. `grep -rln "implements TextMetrics" src test` returns
exactly one file: `src/jls/edit/SwingTextMetrics.java`.

## 2. P3 measures a tautology; the invariant worth pinning is unmeasured

The commuting square in §7.10 closes by construction and everyone knows it: today
each of the five sites literally computes `SwingTextMetrics.forGraphics(g)`
in-place. Moving that call from callee to caller cannot change a pixel unless an
op reads something off `Graphics` besides font metrics, which is H2's job. P3 —
"once through a `SwingTextMetrics` built from a `BufferedImage`, once through the
same metrics passed directly" — compares an expression to itself.

The invariant actually worth a golden is **metrics-independence**: apply the same
op sequence under two *different* `TextMetrics` implementations and observe the
same canonical save. That test cannot pass today, and its failure is the finding
the whole feature turns on. It is also the property #352 needs: convergence is
currently a function of the local font, and swapping `Graphics` for a *locally
obtained* `TextMetrics` leaves it exactly as local as it was. The issue's own
framing — *"applying through AWT makes convergence a function of the local
toolkit — a correctness problem"* — is right, and the prescription does not treat
it.

This also sits astride the project's strongest cultural commitment. The README
sells byte-reproducible jars, `SHA256SUMS`, provenance attestation, `-savetext`
for clean version-control diffs; ARCHITECTURE.md and #166/#98 sell canonical,
deterministic saves. That a circuit's saved bytes silently depend on the fonts
installed on the drawing machine is a latent reproducibility hole in a project
that treats reproducibility as a headline feature. Closing it is a bigger, more
JLS-shaped win than "no `java.awt` import in `src/jls/collab/`."

## 3. The second half re-files two already-filed tasks

The four gestures are **#282** ("Editor gestures: migrate placement, wiring, and
paste commits behind the OpSink seam via preview-then-commit") and **#283**
("Dialog commits: route quick-edit and element edit dialogs through
`SetElementConfig`"). Both are open, both are sub-issues of #167, both carry
per-site permalinks, plan-builder shapes, fallback policies and their own open
questions — more operational detail than #382's single checklist bullet. #337 saw
this coming and wrote it down: *"Any TASK-0037 filing must reconcile with them
rather than re-file the same work."* #382's `related` list is
`[352, 167, 170, 163, 224, 77]`. Neither number appears.

The bundling argument #337 gives ("moving `apply` off `Graphics` forces every call
site open") does not hold either: the four inline gestures are, by definition, the
sites that *do not* call `apply` today. They are opened by the migration, not by
the signature.

And the security framing is overstated. #170's element-type allowlist is already
meaningful: hostile input enters only through `CircuitOpReader`, which is closed
today whatever the local placement drag does. What inline gestures actually break
is replication fidelity and precise undo — a #163/#352 concern, not a #170 one.

## 4. The reframing I would build instead

**A. Ship `jls.core.FixedTextMetrics` (or equivalent) in `src/`, and make it the
default at the op boundary.** A table of advance widths for the ambient editor
font, committed as a resource, plus ascent/descent/height. `SwingTextMetrics`
stays what the editor supplies; the deterministic one is what batch, import,
grading, replicas and tests supply. `TextMetricsParityTest` gains a sibling that
asserts the table agrees with the platform `FontMetrics` under the display tag —
and when it does not, that divergence is now *visible* instead of silently baked
into somebody's saved circuit. This is #337 Open Question 4 answered the way #337
recommends, and it turns the parameter **non-null**, deleting the entire null
branch, §11's second threat, and Open Question 4 of #382 ("one metrics per batch
or per op?" — per session, installed once).

**B. Or make geometry data rather than derivation.** `AddElements` and
`SetElementConfig` already carry the element's *whole save block*, and every
`init` skips sizing when `width`/`height` are non-zero. If op-carried blocks
always include the size the originating machine computed, three of the five call
sites in `src/jls/collab/op/` need no metrics at all, and every replica reproduces
the originator's geometry byte-for-byte with no font table anywhere. Only
`RotateElement`/`FlipElement` remain, and those want a dimension swap rather than
a re-derivation (`Pin.rotate`'s `width = 0; init(g)` is the thing to fix, not to
plumb through). This is the smaller change and it makes the convergence problem
disappear rather than relocating it — at the cost of dropping the #21 size-omission
optimization for op-carried blocks, which is a byte-count question, not a
correctness one.

**C. Either way, install the metrics on the context, not the call.** A
`TextMetrics` handed to a session/`Circuit` once, read by `apply`, collapses
`apply(Circuit, TextMetrics)` to `apply(Circuit)` — a pure data→data mutation,
which is what a network vocabulary and a CRDT both want, and what makes
`OpSink.submit`'s per-op plumbing question vanish.

A separate flag, small but symptomatic of altitude: §7.4 specifies
`SwingTextMetrics.of(getGraphics())`. `of` is the non-null-safe factory
(`src/jls/edit/SwingTextMetrics.java:54`); `getGraphics()` returns null on a
non-displayable component, where `forGraphics` returns null today and `of` will
not. The plan swaps a null-safe call for an unsafe one in the one place the GUI
touches.

## 5. Acceptance criteria I am explicitly disregarding

- **H1 / §11's "`TextMetrics` must stay nullable."** Inverted: nullability is the
  thing to remove. Keep a null-tolerant *load* path (`Circuit.finishLoad(null)` at
  `src/jls/Circuit.java:1026`, `src/jls/JLSStart.java:226` etc. rely on it); make
  the *op* parameter non-null.
- **P3.** Replace with the metrics-independence golden of §2. Two different
  `TextMetrics` implementations, one op sequence, one canonical save.
- **The gesture-migration half of §8, P5, and the `everyGestureRowIsMigratedOrExplained`
  criterion.** Those belong to #282 and #283. If they are stale, amend them; do
  not re-file them.
- **The "12 files, not 5" correction.** Accurate and irrelevant. Eleven of the
  twelve are an import line; the count is not a risk signal, and the issue spends
  three sections on it while never asking what a headless op should *measure with*.

## 6. What survives unchanged

The direction is right and well-aligned: `jls.collab.op` must not import AWT, the
`HeadlessCoreRatchetTest` prefix addition with `BASELINE = Set.of()` is exactly
the right enforcement mechanism (and O5 is correct that the ArchUnit rule is not
it — note `ArchitectureRulesTest.collabLayersAreHeadless`'s own javadoc claims
`jls.collab.op` "is born clean," which it demonstrably is not). Tightening that
rule to `java.awt..`/`jls.edit..` rides along fine. The reflective enumeration
over the sealed permits list is a good pattern and should stay.

Reduced to its defensible core, this is a one-week task: ship a deterministic
`TextMetrics` in `jls.core`, make `apply` take it non-null, delete the five
adapter calls, add the ratchet prefix, and pin metrics-independence with a golden.
That task is worth doing now and unblocks TASK-0038. The rest is #282 and #283.
