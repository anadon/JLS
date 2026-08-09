# Issue #620: TASK-C490-2: the trace window gains a real-valued row and a headless CSV form, because the value domain today admits only a BitSet or null
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-anchored on the single fact it cites (`docs/simulation-semantics.md:44`
does say the value domain is `BitSet` or null) and it is honestly scoped against its
sibling planning documents (#490, #303, #305). But it understates how deep the "BitSet
only" constraint actually runs, leaves the one genuinely new surface (a headless CSV
format) completely undesigned, and its cross-issue bookkeeping is already stale at the
moment of filing. As written, an implementer could satisfy every acceptance criterion
literally while leaving the real goal — a reflection waveform a student can actually
read, headless or not — unmet.

## Findings, most severe first

### 1. "A change to the row model and the headless dump path only" undersells the actual blast radius — no real-valued signal exists anywhere upstream of the row

The issue's only cited evidence is a documentation line
(`docs/simulation-semantics.md:44`). The actual constraint is load-bearing code, not
just prose, and it is pervasive:

- `src/jls/elem/Output.java:108,121,136` — `setValue(@Nullable BitSet)`,
  `getValue(): @Nullable BitSet`, `propagate(@Nullable BitSet, ...)`.
- `src/jls/sim/TraceSample.java:19` — `public record TraceSample(long time, BitSet value)`,
  consumed by `BatchSimulator`, the VCD exporter, and `jls.BatchTracePrinter`.
- `src/jls/edit/Trace.java:52` — `private record Change(BitSet value, long when)`, the
  GUI row's own storage type.
- `WireNet.java:405` (cited correctly by the parent issue #490) —
  `private @Nullable BitSet value = new BitSet(1);`, one value per net.

Every one of these is a concrete, non-generic type binding to `BitSet`, from the wire
net up through the batch sample record to the GUI row's record. Criterion 5 promises
"no new element type," which is fine, but that also means this task ships with **no
producer of a real value anywhere in the type system** — `Output`/`WireNet` cannot carry
one, and the element that would (the transmission line) is a separate, not-yet-filed
task with no ordering dependency declared here (`ordering_after: []`). So satisfying
criterion 1 ("a signal whose value is real-valued renders as a row") requires either (a)
a synthetic/test-only injection path that bypasses `Output`/`WireNet` entirely, or (b) a
second, parallel sample/value channel alongside the `BitSet` one. Both are materially
larger and more architecturally consequential than "the row model and the headless dump
path," and the issue is silent on which is intended.

**Recommendation:** name the actual seam — e.g. a sealed `TraceValue` (BitSet-valued |
real-valued) that both `TraceSample` and `Trace.Change` are rewritten to hold, plus how a
test harness is expected to produce a real-valued sample without an element to source it
from. Without that, "row model only" is not a credible scope boundary.

### 2. The one genuinely new surface — the CSV form — has zero design and no test named, unlike everything else in this issue family

`docs/batch-interface.md:3` declares itself "**normative, and a stability contract**,"
covering exactly three things: the `-t` grammar, the watched-element stdout format, and
the VCD profile. A grep across `src/` and `docs/` turns up **zero** existing uses of
"csv" as a JLS output format — this issue introduces a fourth batch surface with:

- no flag name,
- no column layout,
- no numeric formatting/precision (the parent issue #490's own worked example prints
  4-decimal values like `5.5000` / `1.8333` — is that the CSV precision, the GUI display
  precision, both, neither?),
- no interaction with the existing `-r`/`-vcd`/`-t` flags,
- and no completion-criteria update to `docs/batch-interface.md` itself, despite that
  document being the one this repo treats as a stability contract.

Contrast this with #490's own convention of naming a pinning test for every acceptance
criterion (`*Pinned by:* ReflectionGoldenTest`, `EdgeRateCollapseTest`, ...). Issue #620
names **no test anywhere** for any of its five criteria. That is a real regression in
rigor relative to the planning culture this repo otherwise enforces (see also
`test/jls/ArchitectureRulesTest.java`, `HeadlessCoreRatchetTest`, `PaletteContractTest` —
this codebase pins architectural claims with source-scanning tests as a matter of
course).

**Recommendation:** add a criterion naming the CSV format precisely (delimiter, header
row, timestamp units, float format) and a test that pins it, and add a completion item
to update `docs/batch-interface.md`.

### 3. Criterion 2's "byte-identical to the rendered data" is a category error for real values, and is gameable either direction

"Byte-identical" is meaningful for the existing BitSet rows because both the trace and
the VCD/stdout paths ultimately serialize the same integer bits. For a real-valued row,
the GUI renders *pixels* (`Trace extends JPanel`, `paintComponent`), not bytes — there is
no natural "byte" representation of a rendered analog trace to compare a CSV against.
The only sane reading is "the numeric samples backing the row and the CSV are identical
values," but as written an implementer can satisfy a loose reading (values agree "close
enough" after independent rounding in each path) or an implementer arguing in bad faith
can claim a strict reading is unsatisfiable "because pixels aren't bytes" and ship
nothing comparable. Neither outcome is caught by the stated criterion.

**Recommendation:** rewrite criterion 2 against the underlying sample values (e.g. "the
`double` sequence backing the row equals, bit-for-bit, the `double` sequence written to
CSV, formatted per criterion N's format spec") rather than "the rendered data."

### 4. Cross-issue bookkeeping this issue depends on is already stale as of filing

Issue #620's own YAML declares `part_of_feature: 490`, and criterion 4 leans on "#490's
completion criteria." But #490 (fetched live, `updated_at: 2026-08-04T07:57:54Z`, seven
hours *before* #620 was opened at `15:10:04Z`) still lists this exact task as:

> `planned — a real-valued trace row | ... | Not filed`

in its own decomposition table, and #620 is not linked as a GitHub sub-issue of #490
(`issue_read get_parent` on #620 returns `null`; #620 itself reports `has_parent: false`,
`has_children: false`). #490's own re-planning protocol requires "Update the roster and
`planned_tasks` in the same edit as the REPLAN comment; a filed child moves from
`planned_tasks` to `requires_tasks` by number" — that update has not happened. So the
"funding ledger" criterion 4 asks the implementer to trust (record funding on #303/#305
"per #490's completion criteria") is itself pointing at a stale, unreconciled table one
hop away. This is exactly the kind of drift the planning framework in ARCHITECTURE.md's
"Recorded decisions" style is designed to prevent, and it has already happened on the
first hop.

**Recommendation:** before work starts, file the missing sub-issue link and update
#490's roster table/`planned_tasks` entry to point at #620 by number, per #490's own
protocol — otherwise criterion 4 is unenforceable (nothing in CI checks GitHub issue
prose).

### 5. Criterion 4 is process bookkeeping wearing an acceptance-criterion costume

"The row's funding is recorded on #303 and #305 so it is not paid for twice" is not
verifiable by any test, build, or code review — it is verified by a human reading two
other open issues' comment threads. Nothing stops a PR from merging with criteria 1-3, 5
green and criterion 4 simply forgotten (or claimed "done" without the actual comments
existing). Given finding 4 above, this is not a hypothetical: the ledger is already out
of sync one hop up the chain.

**Recommendation:** either drop criterion 4 from the *engineering* acceptance list and
track it as a closing-checklist item (as #490 itself does, under "Completion Criteria"),
or make it mechanically checkable (e.g. a script that greps #303/#305 via the GitHub API
for a reference to this issue number before the PR is allowed to close).

### 6. "The same cursor and zoom behaviour every other row has" may not be verifiable by anything CI currently runs

Per `ARCHITECTURE.md`'s Test layout section, the UI-verification harness's "Layer 2
(Swing harness under Xvfb) and 3 (render-to-image) are reserved" — only Layer 1
(headless model assertions) exists today, and `Trace` is a live `JPanel` with
`MouseListener`/`MouseMotionListener` (`src/jls/edit/Trace.java:21`) whose cursor/zoom
behavior lives in mouse-event handling and `paintComponent`, not in an already-extracted
model class the way `TraceGeometry` extracted the tic-spacing math. Criterion 1 as
written can only be checked today by manual inspection or by extracting yet another
headless-testable geometry/model class analogous to `TraceGeometry` — which the issue
does not ask for and does not budget (band is 0.5-1 mw, consistent with #490's own
pricing, but that budget assumed the existing test infrastructure could pin the claim).

**Recommendation:** either scope criterion 1 down to what `TraceGeometry`-style
extraction can pin headlessly, or explicitly acknowledge the criterion is
manually-verified only and say so, rather than implying it is CI-enforceable alongside
the golden-based criteria 2-3.

## What is solid (no rework needed)

- The core motivating claim is accurate: `docs/simulation-semantics.md:44` really does
  say value = `BitSet` or null, verified by direct read.
- Criterion 5's "no new palette entry/element/window" is a strong, mechanically-enforced
  boundary — `test/jls/edit/PaletteContractTest.java` already asserts registry-count ==
  palette-count, so this criterion is effectively self-checking as long as no element is
  added.
- The `band_mw: "0.5-1"` figure matches #490's own pricing for this exact row
  ("priced at 0.5-1 mw as its own row"), so there is no budget contradiction between the
  two documents.
- `ordering_after: []` matches #490's sequencing section, which explicitly calls the
  trace row "independent of the element ... can be executed concurrently by a second
  agent from day one." No contradiction there.
- Criterion 3 (byte-identical existing goldens, existing rows unchanged) is concretely
  testable today against `BatchSimulationGoldenTest`, `SequentialGoldenTest`, and
  `VcdExportGoldenTest`, and is the right kind of non-regression guard for a
  domain-widening change.

## Bottom line

The issue is not wrong about the problem, and its scope discipline (no element, no
window, no palette entry) is genuinely good instinct. But criteria 1 and 2 — the two that
actually matter for "a reflection is testable with no display and no window" — are
underspecified enough that they can be gamed in either direction, the CSV format is
invented out of whole cloth against a document the project calls a stability contract
without saying so, and the cross-issue funding ledger it leans on is already inconsistent
with the issue it claims to be funded by. Needs a rework pass on criteria 1, 2, and 4
before this is startable as scoped.
