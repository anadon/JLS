# Issue #646: TASK-C563-4: truth-table extraction runs headless behind a batch flag with machine-readable output
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Add a batch-mode flag that extracts a truth table from a combinational region
of a circuit with no display present, writing a machine-readable, byte-
deterministic table to a named path, with documented flags/exit codes/schema
alongside `docs/batch-interface.md`. Ordered after TASK-C563-1 (#641, table
enumeration) and TASK-C563-2 (#642, bound/refusal), under parent #563
(FEAT-C31-1) and grandparent #515 (CAP-31).

## Findings, most severe first

**1. (High) The core noun of the task — "a region" — has no non-interactive
definition anywhere in its own dependency chain.** The Outcome says the flag
"names a circuit and a region." But the component that actually identifies a
combinational region is TASK-C563-0 (#872, filed the same day as this
review), and its own Outcome states the pass takes *"a user selection (or a
whole circuit)"* — `src/jls/edit/SimpleEditor.java` selection is a mouse-drag
rectangle over `Element` objects (`SimpleEditor.State.selecting`, drag/drop
handling around lines 779-870), with no serialized, textual, or file-based
representation anywhere in the codebase (confirmed by grep: `selection` hits
are all GUI/editor-side). Neither TASK-C563-1 (#641) nor TASK-C563-2 (#642)
defines a headless region syntax either — both talk about "a selected
combinational region" in the same GUI-selection vocabulary. So AC-1 as
written ("writes a machine-readable table") is satisfiable by a flag that
only ever operates on "whole circuit" — literally supported by #872's own
"(or a whole circuit)" escape hatch — without ever implementing the region
naming the Outcome paragraph promises, and without anyone noticing, because
none of AC-1 through AC-4 actually tests that a sub-circuit region argument
resolves correctly. AC-3's third refusal category, "unresolvable region," is
meaningless without a defined syntax for what a *resolvable* region argument
looks like. **Recommendation:** either scope this task explicitly to
whole-circuit extraction only (drop "region" from the Outcome, defer
region-addressing to a follow-up once #872/#641/#644's GUI selection model
has a designed serialization), or add an AC that pins the headless region
syntax itself (e.g., a named-selection list, element-id list, or bounding
box) with a golden test — currently nothing in the four-task chain owns that
design.

**2. (Medium) The flag shape conflicts with the codebase's one-operand-per-
flag model, and the issue doesn't say which pattern to follow.** `FlagSpec`
(`src/jls/JLSStart.java:719-748`) gives every flag exactly one optional
operand name (`operandName` is a single `String`, `Arity` is NONE/REQUIRED/
OPTIONAL). The circuit file is already the positional operand of batch mode
(`jls -b ... circuit.jls`, `docs/batch-interface.md` §1). A flag that "names
a circuit and a region and writes a table to a named path" bundles three
pieces of information under language that reads like a single flag — that
has no precedent in `FLAGS` (`JLSStart.java:759-789`); the closest analogue,
`-export`/`-board`/`-pins`, solves a similar multi-parameter need with three
*cooperating* flags, not one flag with three operands. The issue doesn't
choose between "one flag with a compound operand string" (requires a
`FlagSpec` structural change) and "several new cooperating flags" (requires
its own arity/exclusion declarations per the boundary comment's point 1),
and an implementer could plausibly pick either without failing any stated
AC. **Recommendation:** the issue (or a design note on #563) should commit
to one shape before this task is picked up, given three sibling tasks
(#651, #656, #660) are adding flags to the same table concurrently.

**3. (Medium) "A documented non-zero exit status" doesn't require the three
refusal categories to be distinguishable by exit code, only by prose.**
AC-3 lists three distinct refusal causes (over bound, sequential content,
unresolvable region) but only requires "a documented non-zero exit status
… and a diagnostic on the error stream" — singular, undifferentiated. A
grading script (this task's whole stated purpose — "a grading script gets
the same table the student sees") that wants to distinguish "your circuit is
too big" from "your circuit has a flip-flop where a gate belongs" from "your
region argument doesn't parse" has to string-match stderr text, which
carries no format stability guarantee under this AC (unlike, say,
`LoadError`'s fixed taxonomy in `ARCHITECTURE.md`'s error-reporting
section). This is exactly the kind of criterion that can pass by an
implementation that does `exit(1)` with three different but unpinned
messages. **Recommendation:** either require distinct, documented exit
codes per refusal category (the CLI contract work in #524/#686 is adding a
status 3 for verdicts — this is the natural place to also settle a refusal
taxonomy), or explicitly require the diagnostic *text* to follow a stated,
tested format (category-prefixed, e.g.) so it's a golden, not free text.

**4. (Medium) The task's own coordination note admits an ordering hazard
that the issue's `ordering_after` doesn't guard against.** The issue's
boundary line says "The batch CLI's stability promise is #524 (FEAT-C21-1);
this task adds a flag under that contract, it does not amend it." But #524
is open with every acceptance-criteria checkbox unticked, and both of its
own children — #686 (write the contract + conformance suite) and #687 (the
compatibility ratchet) — are open too. There is, today, no frozen contract
to be "under." The comment thread on #646 itself acknowledges the risk
generically ("landing before [the freeze] means the freeze must enumerate
them") but #646's `ordering_after` YAML lists only `TASK-C563-1` and
`TASK-C563-2` — it does not order after #524 or #686. If #646 lands first
(plausible: it's tier:task, small, while #524 is tier:feature), its ad hoc
exit-code/schema choices become retrofit material for #686's freeze rather
than being designed against a stable target, and the same is separately
true for #651/#656/#660. **Recommendation:** either add #524/#686 to
`ordering_after` explicitly, or state — as #660's boundary note does for a
different open dependency — that this task proceeds against
`docs/batch-interface.md` as it stands and accepts retrofit risk.

**5. (Low, not a defect) "CAP-31 AC-5" is cited accurately.** Verified
against #515's body: AC-5 reads "All analysis is headless-callable (batch
flag), so graders can use it — consistent with the batch-interface
contract" — a faithful match to AC-1's citation. Earlier in this review I
suspected a similarly loose citation for #372 ("mutual-exclusion declaration
to FlagSpec"); on reading #372's full body that claim also checks out (it's
one line item of a much larger registry-totality task). Flagging that I
checked both, since fabricated/stale cross-issue citations are a common
failure mode in this tracker's machine-generated boundary comments and
neither instance here is one.

## What's solid

- AC-2 (byte-determinism for goldens) matches the repo's established
  discipline verbatim (`VcdExportGoldenTest`, `BatchSimulationGoldenTest`,
  `docs/batch-interface.md` §4's "two identical runs produce identical
  bytes" clause) — nothing new to invent here.
- AC-4 (document the flag/exit codes/schema alongside the existing batch
  flags) mirrors the `CliFlagTableTest` drift-guard pattern already enforced
  for every other flag — a low-risk, well-precedented requirement.
- The task correctly refuses to re-litigate or duplicate the combinational-
  extractor logic (#872/CAP-09's shared component) — the boundary note's
  "does not amend the contract" framing for #524 is directionally right in
  intent even though the target it defers to isn't built yet (finding 4).
- Ordering after TASK-C563-1/TASK-C563-2 rather than trying to build
  enumeration or the bound check itself is the correct scope discipline.

## Bottom line

The two structural documents this task depends on for its two load-bearing
nouns — "region" (headless-addressable selection) and "the batch-interface
contract" (frozen exit/schema surface) — do not yet exist anywhere in the
tracker or the tree. The acceptance criteria as written can be satisfied by
an implementation that quietly narrows "region" to "whole circuit only" and
that leaves the three refusal categories exit-code-indistinguishable,
without failing any stated AC. That's a rework of the criteria, not a reason
to block the underlying task indefinitely.
