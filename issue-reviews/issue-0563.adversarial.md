# Issue #563: FEAT-C31-1: a drawn combinational circuit reads out as its truth table, within a stated input bound that refuses with arithmetic instead of hanging
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The outcome — extract a truth table from a drawn combinational region,
bounded and refusing cleanly above the bound — is a reasonable, narrow
slice, and the "golden-tested against exhaustive simulation" framing in
AC-1 reuses infrastructure the repo already trusts
(`BatchSimulationGoldenTest` lineage). The problems are not with the
goal; they are with the issue's own currency, its dependency graph, and
several acceptance criteria that don't survive a skeptical read.

## Findings, most severe first

### 1. The issue body still asserts a dependency graph its own comments say is false

The machine block and "Boundary and reference notes" in the **body**
state:

> `ordering_after: ["#306 CAP-09's combinational-subgraph extractor"]`
> "the combinational-subgraph extractor … is CAP-09's component — one
> extractor, two consumers; do not build it twice… Any extractor
> capability gaps found here are filed against #306's component, not
> reimplemented here."

The issue's own third comment (2026-08-08, "ORDERING CORRECTION")
retracts this: it searched the tracker (`combinational subgraph`,
`combinational-subgraph`, `combinational cone`, `extractor`) and found
"Nothing in the tracker delivers that component… The component is
asserted at capstone level and owned by no filed issue," and files a
brand-new #872 to replace #306 as the actual dependency. The comment is
explicit about the failure mode this causes: "an executor picking up
#641 is told not to build the extractor, finds nothing to consume, and
either stalls indefinitely or builds it anyway."

The **body was never edited** to reflect this. Anyone reading the issue
as normally surfaced (title + body — e.g. `gh issue view`, the web UI
above the fold, or a triage script that reads body only) gets the wrong
prerequisite. This is exactly the failure the corrective comment warns
about, reproduced by the issue's own presentation. **Recommendation:**
edit the body's `ordering_after` and boundary note to point at #872
before this is picked up; don't rely on a comment three levels deep to
override the spec.

### 2. The real critical path and cost are absent from the issue's own estimate

Per the (uneditied-into-body) correction, #563 now depends on #872
(filed 2026-08-08, `band_mw: "1-2"`, unimplemented), which itself
declares `ordering_after: [468]` — TASK-0007, a five-copy net-partition
unification (`src/jls/Circuit.java`, `src/jls/Util.java`,
`src/jls/collab/op/AddWire.java`, `src/jls/hdl/HdlExporter.java`,
`test/jls/ui/CircuitAssert.java`) that is itself a substantial,
carefully-scoped refactor with its own multi-page acceptance criteria
and is *also* unimplemented. So the actual chain to ship #563 is
`#468 → #872 → #563`, none of which appears in #563's `band_mw: "2-3"`
estimate or its `ordering_after` field (which names only #306, itself
now known to be wrong). A reviewer costing this issue at face value will
under-budget by at least two unstarted, non-trivial prerequisite issues.
**Recommendation:** state the transitive dependency chain and either
fold its cost into the estimate or explicitly flag the estimate as
"post-prerequisites."

### 3. AC-2's bound N is never specified, making the criterion untestable and gameable

> "AC-2: The input-count bound is stated in the UI and enforced; above N
> inputs the tool refuses with the row-count arithmetic, never hangs."

No value or formula for N appears in #563, in the parent capstone #515,
or in #872. As written, an implementation could set N=1 (or N=0) and
trivially satisfy "the tool refuses above N inputs, never hangs" while
the feature is useless for any real 4-input golden test in AC-1 (which
would then also need N≥4, an implicit constraint nowhere stated as a
lower bound). Conversely an implementer could set N absurdly high
(N=30) and technically satisfy the letter of AC-2 while making "never
hangs" untested in practice up to a very slow bound. **Recommendation:**
pin N to a concrete number or a formula tied to a measured time/memory
budget (e.g., "N such that 2^N row generation completes within Xs on
reference hardware"), and add a lower-bound constraint (N ≥ 4, matching
AC-1) so the two criteria can't be satisfied in mutual isolation.

### 4. AC-3 mixes ownership in a way that invites a hollow pass

> "AC-3: A selection containing sequential elements or feedback is
> rejected with a named diagnostic identifying the offending element,
> not a wrong table."

The correction comment reassigns detection to #872 ("AC-3 is now #872's
AC-2/AC-3, not this feature's… This feature's AC-3 stands as an
integration criterion"), but the acceptance-criteria list in the body
is unchanged and still reads as if #563 must produce the diagnostic
itself. Because the two documents disagree on scope, a minimal
implementation could satisfy #563's AC-3 by wiring through whatever
#872 happens to return, with no test asserting the diagnostic's actual
content or that #563's UI/batch surface renders it usefully — the
"integration" framing has no acceptance test of its own in this issue
(no golden or fixture is named). **Recommendation:** either import
#872's AC-2/AC-3 text verbatim as a cross-reference with a concrete
integration test named here, or drop AC-3 from #563 entirely and let
#642 (named in the comments as owning "presentation" of the refusal)
carry it.

### 5. AC-4 invokes a stability contract without meeting its own bar

> "AC-4: Truth-table extraction is headless-callable via a batch flag
> with machine-readable output, consistent with the batch-interface
> contract."

`docs/batch-interface.md` opens by declaring itself normative and "a
stability contract": *"any change to them requires a CHANGELOG entry
and either a major version bump or a compatibility flag that preserves
the old behavior."* #563 proposes a new batch flag and a new
machine-readable output format but specifies neither the flag's name,
its output grammar, its exit-code behavior, nor a CHANGELOG/versioning
obligation — all things the document it cites as the bar treats as
load-bearing. As written, "consistent with the batch-interface
contract" is a slogan an implementer could satisfy by inventing any ad
hoc format, since nothing here constrains it. **Recommendation:** either
specify the flag and output format now, or explicitly defer that design
to a named follow-up task (as #872 did for the extractor) rather than
gesture at the contract.

### 6. Silent collision with the existing `TruthTable` *element*

The repo already ships `jls.elem.TruthTable` (1,491 lines,
`src/jls/elem/TruthTable.java`) plus `jls.edit.TruthTableEditor`,
`TruthTableDialog`, `TruthTableRenderer`, `TruthTablePrintable`, and
`DisplayBool` — a full editable truth-table *element* a student can
place and hand-author (`addInput`, `addOutput`, `toggleOutput`,
`makeDontCare`, etc., per the issue's own corrective comment). #563
introduces a second, read-only "table view" that is *extracted* from a
drawn combinational region, not authored. The issue never states
whether the new view reuses the existing `TruthTableRenderer`/
`DisplayBool` rendering machinery or builds parallel UI, nor whether the
result is ever materialized as a `jls.elem.TruthTable` instance (which
would pull in ARCHITECTURE.md's ~16-touchpoint "new element" tax,
wildly exceeding the 2-3 mw estimate) versus a transient dialog. Given
the naming overlap, this is a concrete way for an implementer to build
the wrong thing at several times the budgeted cost. **Recommendation:**
state explicitly that the extraction view is read-only/non-persisted
and, if it reuses `TruthTableRenderer`/`DisplayBool`, name that as a
design constraint.

### 7. Process smell: the issue's own evidence chain has already needed one factual correction

The three comments are a self-correcting chain from the same automated
review process: comment 2 cites evidence at "master `07a0bea`"; comment
3 retracts that specific commit as "not on master" (`git merge-base
--is-ancestor 07a0bea origin/master` → `NOT-AN-ANCESTOR`) and repins to
a different SHA. The underlying claims survive the correction, but it
means at least one previous citation in this exact issue's history was
wrong and shipped uncaught for a period. That's a reason for whoever
picks this up to re-derive line/commit citations independently rather
than trusting any single comment's `grep`/commit claims at face value,
including this review's own citations above.

## What's solid (no action needed)

- AC-1's "golden-tested against exhaustive simulation" is a sound,
  reusable verification strategy consistent with existing goldens.
- `band_mw: "2-3"` for the feature slice as originally scoped (extractor
  excluded) is internally consistent with #515's `PF-1 — Truth-table
  extraction (2–3 mw)` — no drift there.
- The three-way dedup against #564/#565 (comment 1) is a clean,
  non-overlapping split by direction (circuit→table vs table→expression
  vs table→circuit); no merge-should-have-happened complaint here.
- Absence of an existing analysis path is correctly verified
  (`grep -rn "Analyze\|analyze" src/ --include=*.java` matches only a
  `VhdlEmitter` comment) — the feature is not solving an already-solved
  problem.
