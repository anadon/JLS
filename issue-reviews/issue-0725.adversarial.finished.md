# Issue #725: TASK-C540-2: every registered element type gets an ANSI/IEEE-91 distinctive print symbol, and the palette sweep exports with zero warnings
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

TASK-C540-2 (part of feature #540 / FEAT-C24-5, itself under capstone #505 /
CAP-24) is the "author the symbols" half of a two-task split: TASK-C540-1
(#723, open) is supposed to land first and build a registry-keyed
element-type→print-symbol mapping plus a `PrintSymbolTotalityTest` that
starts red; #725 then authors real ANSI/IEEE-91 symbols to turn it green.
Grounded against `ARCHITECTURE.md`, `src/jls/elem/ElementRegistry.java` (35
registered types), `test/jls/edit/PaletteContractTest.java`,
`docs/standards-adoption/01-iec-ieee-symbols.md`, and this repo's own prior
adversarial review of the parent feature, `issue-reviews/issue-0540.adversarial.md`.
No "print symbol" concept exists anywhere in `src/` or `test/` today.

## Findings, most severe first

**1. Filed under a blocking precondition its own parent feature admits is still unresolved, and #725 restates rather than resolves it.**
CAP-24 (#505) Open Question 1 — "Print symbol standard: ANSI/IEEE-91
distinctive shapes only, or also IEC rectangular? ... Blocks PF-5's filing"
— has never been adjudicated by a `REPLAN:` comment. The sole comment on
#505 (2026-08-04) confirms this is live: *"Open Question 1 is marked 'Blocks
PF-5's filing'... yet #540... [is] filed with the recommended default[]
recorded rather than the question[] resolved... the decisions remain
unadjudicated."* #725's own body does the same thing one level down: *"CAP-24
Open Question 1's recommended default — recorded here so the choice is
adjudicated rather than defaulted silently."* Recording a default is not
adjudicating it. As of this review there is still no REPLAN comment on #505.
**Recommendation:** do not start #725 until #505 carries a REPLAN resolving
OQ-1 (or an explicit waiver of the blocking rule); otherwise the whole
symbol set gets authored against a foundational choice that could be
overturned afterward.

**2. AC-1 and AC-4 assume a rendering/export pipeline that #725 never names and does not order after.**
AC-1 requires "the palette-sweep export completes with zero
missing-print-symbol warnings" and AC-4 requires "committed visual goldens
cover the whole sweep." Two candidate pipelines exist in this repo: the
existing screen exporter (#154, `CircuitRenderer.exportImage`) and the
not-yet-built print-styled exporter (feature #536, tasks #707/#709, both
open, neither an `ordering_after` entry here). If AC-1/AC-4 mean the
existing #154 path, the issue should say so explicitly, since "print
symbol" strongly implies the print pipeline that #536 is building
separately and on a different task chain. If they mean #536's pipeline,
`ordering_after` is missing a real dependency (#707 and/or #709) and the
task cannot be verified standalone. This is the same ambiguity flagged
against the parent feature (#540 review, finding 2) — restating it here
because #725 had the chance to resolve it at the task level and didn't.
**Recommendation:** name the concrete export mechanism AC-1/AC-4 run
through, and add it to `ordering_after` if it is #536's pipeline.

**3. Gameable acceptance criteria — nothing in AC-1/AC-2 distinguishes a standard-grounded symbol from a placeholder.**
AC-2 says symbols "follow ANSI/IEEE-91 distinctive shapes where the standard
defines one; every type with no standard shape has its chosen depiction
recorded with a reason." Nothing requires the recorded reason to cite a
clause, nothing requires `docs/symbol-conformance.md` (the artifact
`docs/standards-adoption/01-iec-ieee-symbols.md` calls "the deliverable" for
any real conformance claim) to exist, and nothing checks a symbol against
the actual standard text — which, per that same research document, nobody
in this repo has purchased or read: *"The project draws rectangles with `&`
in them, nobody buys the standard, the matrix is written from Wikipedia...
This is the *likely* outcome if the document purchase is deferred 'until
later.'"* A developer can satisfy AC-1's zero-warnings gate and AC-2's
"recorded with a reason" clause for all 35 types with un-cited, unverified
choices and the ratchet goes green while the stated ANSI/IEEE-91 grounding
is fiction. **Recommendation:** require each AC-2 entry to cite a clause (or
explicitly "no clause, JLS-local") in a committed matrix file, and gate
merge on that file existing — not just on the totality test passing.

**4. "Every registered element type" collides with an exemption set this codebase already maintains, and #725 gives no exemption list.**
`test/jls/edit/PaletteContractTest.java:44-45` excludes `SubCircuit`,
`WireEnd`, `TestGen` from palette/visual treatment for stated structural
reasons (WireEnd is a wire endpoint, not a drawn body; TestGen is a
batch-only stand-in; SubCircuit is placed via Import). AC-1/AC-3 say "every
registered element type," which is the full 35-entry `ElementRegistry.ALL`,
not the palette-minus-exemptions set. An "authored print symbol" for
`WireEnd` is close to meaningless. #725 doesn't say whether it inherits
`PaletteContractTest`'s exemption set, invents a new one, or genuinely means
all 35 — leaving that decision to the implementer, which risks a mapping
that either fabricates symbols for non-visual types or silently diverges
from the registry-totality claim AC-1 makes. **Recommendation:** state the
exemption set explicitly (reusing `NON_PALETTE_TAGS` unless there's a reason
not to) before authoring starts.

**5. AC-5 is unfalsifiable.**
"The symbol data is usable by the IEC/IEEE symbol-conformance roadmap items
(#43/#44) without duplication" names no test, no interface contract, and no
concrete shape the data must take to satisfy it. #43/#44 are themselves
long-horizon, currently-unscoped roadmap items (per
`docs/standards-adoption/01-iec-ieee-symbols.md`'s own "do-it-if, 8-12
maintainer-days, no user has asked" verdict). A criterion that can only be
checked once a different, unscheduled body of work eventually lands is not
verifiable at the time #725 is reviewed for completion. **Recommendation:**
either drop AC-5 from this task's acceptance criteria and move it to a
design note, or replace it with a concrete, checkable-now shape requirement
(e.g., "symbol geometry is expressed via `GateOutline`/`GateOutline.Label`,
not a parallel type").

**6. Neither #725 nor its prerequisite #723 budgets time to obtain the primary standards documents, which the project's own research explicitly calls non-optional.**
`docs/standards-adoption/01-iec-ieee-symbols.md`: *"Plus 0.5-1 day of reading
the primary documents before any code, which is not optional and is not
padding."* Combined, #723 (0.5-1 mw) + #725 (1-1.5 mw) land in roughly the
same ballpark as that document's 8-12 maintainer-day estimate for a
materially overlapping slice, so the totals are not as badly reconciled as
the parent feature's review found — but neither task lists "obtain
IEEE 91-1984/91a-1991" as a step or a cost line, and AC-2's standard-grounded
framing depends on it. Without it, finding 3 above isn't a hypothetical risk,
it's the default outcome. **Recommendation:** add an explicit line item (or a
blocking sub-task) for acquiring the standard before AC-2 work starts, or
downgrade AC-2's framing to "customary distinctive shapes" and drop the
ANSI/IEEE-91 conformance language entirely.

**7. Golden-file format and cross-platform determinism are unaddressed, and the deterministic-font substrate isn't an ordering dependency.**
AC-4's "committed visual goldens" doesn't say SVG, PNG, or path-only, and the
adjacent research document spends a full section on exactly this risk:
JFreeSVG's `<text>` position is font-metric-derived, so a golden with label
glyphs (`&`, `≥1`, `Σ`, `Q̄`) will drift across the Linux/Windows/macOS CI
matrix unless generated against a fixed, JLS-owned font. That substrate is
being built separately in TASK-C536-1 (#707, open) — not an `ordering_after`
entry of #725. If #725's goldens are authored before #707 lands, they are
likely to need regeneration or to flake on a CI runner with a different
font fallback. **Recommendation:** either add #707 to `ordering_after`, or
restrict AC-4's goldens to path-data assertions (no text elements), matching
the mitigation the research document already worked out.

## What's solid

- Explicitly deferring IEC rectangular symbols to "a possible later second
  theme through the same seam" correctly avoids scope creep into a second
  symbol vocabulary within this task.
- AC-3's requirement that geometry be data "consumed through TASK-C540-1's
  mapping" rather than special-cased per renderer matches the existing
  `Theme`-seam precedent in this codebase (statics rewritten centrally, read
  by renderers) and the design call already made in
  `docs/standards-adoption/01-iec-ieee-symbols.md` (a render mode, not a
  per-element strategy).
- The `ordering_after: [TASK-C540-1]` dependency is real, correctly scoped,
  and points at an issue (#723) that actually exists and is open — unlike
  AC-1/AC-4's missing dependency (finding 2), this one is done right.
- AC-2's instruction to record a reason for every non-standard depiction is
  good discipline in principle, even though nothing currently verifies the
  reason is accurate (finding 3).

## Bottom line

The task-level split from #540 into a mechanism task (#723) and an
authoring task (#725) is a reasonable decomposition, and #725's own
dependency on #723 is correctly stated. But #725 inherits the parent
feature's unresolved filing precondition (finding 1) essentially verbatim,
its lead acceptance criteria depend on an export/rendering pipeline it
neither names nor orders after (finding 2), and none of its criteria can
distinguish a real ANSI/IEEE-91-grounded symbol set from placeholder boxes
with invented justifications (findings 3 and 6). Any one of these should
block implementation until resolved by REPLAN or a rewritten AC set.
