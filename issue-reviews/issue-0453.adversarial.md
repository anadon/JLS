# Issue #453: TASK-0061: the first four N-ary element types exist as placeable, savable, simulable elements — with the binary boundary crossed only through a declared bridge
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-engineered as a document: its concrete code
citations (element registration surface, `HdlExporter`'s exact-class
`EXPORTED` set, `TruthTable`'s digit-2 don't-care collision, the named
enable-like ports) all check out against the current `master` checkout. But
the document rests its entire justification on evidence that either does
not exist in this repository or has not actually landed, and the batch it
proposes to ship is gated behind two open, unimplemented blockers whose own
prerequisite was closed as a duplicate rather than merged. The engineering
plan is sound in isolation; the foundation it claims to stand on is not
verifiable and, as measured against `src/`, is not there yet.

## Findings, most severe first

### 1. [Critical — feasibility] The blocking chain is unbuilt, and its root was closed as a duplicate, not landed

`blocked_by: [419, 422]` is correct as a dependency declaration, but both
are open and neither's deliverable exists in `src/`:

- `git grep -in radix src/jls/elem/Put.java src/jls/elem/WireNet.java` — no
  hits. `Put.getRadix()`/`WireNet` radix fields (#419's whole deliverable)
  do not exist.
- `ls src/jls/core/` — `Bounds.java, Geometry.java, GridPoint.java,
  GridSize.java, Orientation.java, SegmentGeometry.java, TextMetrics.java,
  package-info.java`. No `RadixOps.java`, `RadixAlphabet.java`,
  `LogicVector.java` (#422's and #419's shared prerequisite kernel).

Worse, #419 and #422 both depend on "TASK-0056" (the value-representation
widening), which is now filed as #475 — and #475 is **closed as
`duplicate`**, not merged. Its own deliverable (`jls.core.LogicVector`,
the reserved `getRadix()`/`getDigits()` stubs) is also absent from `src/`.
So #453 sits at the end of a three-deep dependency chain in which none of
the foundation is in the tree, and the chain's root was closed through the
tracker's "duplicate" mechanism rather than through a landing PR — a sign
the planning corpus itself has drifted or is confused about what actually
shipped. **Recommendation:** before this issue is picked up, verify what
#475 was a duplicate *of*, confirm that successor issue actually delivered
`Put.getRadix()`/`WireNet` radix and the `RadixOps`/`RadixAlphabet` kernel,
and only then treat #419/#422 as landed. As written, Method step 1 ("Confirm
#419 and #422 have landed") is the only defense against exactly this, and
right now the answer is "they have not."

### 2. [Critical — evidentiary integrity] The cited rationale documents do not exist anywhere in this repository's history

The issue's family-choice rationale, the "types not attributes" argument,
the pedagogic-hazard reasoning, and the eight-port refusal list are all
attributed to `07-mvl-determination.md` §§4.1/4.2/4.4, and D9/D10 are
quoted from `docs/plan/evidence/BRIEF.md`. Both are absent from this
checkout (`find . -iname "*mvl*determination*"`, `find . -iname
"BRIEF.md"` — no hits), and neither has ever existed in any of the 268
commits reachable from local refs (`git log --all --diff-filter=A -- "**
BRIEF.md"` and `-- "**mvl-determination**"` both return nothing). The
issue itself concedes BRIEF.md is "not present at the evidence commit
`2d0ca9d`" — but it is not present *anywhere*, including the landing
commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc` it names, which is also
not part of this repository's commit graph. An executor cannot check the
MIN_MAX-default argument, the D9/D10 quotes, or the refusal list against
their claimed source; they are asked to trust a paraphrase of a document
nobody in this checkout can read. Given the issue's own warning that the
cited document's *verdict* was reached under reasoning BRIEF §12 D10
forbids, the inability to audit the source at all is a serious gap, not a
formality. **Recommendation:** either attach/commit the cited evidence
document to this repository before execution, or re-derive the family
choice and refusal list from first principles inside this issue, as D10
itself demands.

### 3. [High — internal contradiction] D10's "re-derive, don't inherit the verdict" rule is stated but not followed

The issue quotes D10's operative clause — "the question to answer is
always 'what is the path, and what does it cost' — never 'does the
current state justify it'" — and says the MVL determination's *survey
evidence* stands while its *refusal reasoning* "does not, and must not be
re-imported." Yet every concrete design decision this issue ships —
`MIN_MAX` as the default family, the exact eight-port enable/refusal list,
`RadixBridge`-first sequencing — is asserted as already-settled, sourced
straight from that same document's §§4.1/4.2/4.4, with no independent
re-derivation performed inside this issue. A default family and a refusal
list are conclusions, not raw measurements; treating them as "survey
evidence" that merely needs restating is the exact conflation D10 was
written to prevent. H2's math (§7.10 Stage 1) does re-derive the
*collapse* property mathematically, which is good — but it does not
re-derive *why MIN_MAX over LUKASIEWICZ/POST is the right family to ship
at all*, which is inherited wholesale.

### 4. [Medium — gameable acceptance criterion] The coverage-floor batch-size gate is self-administered and rests on a number the issue itself distrusts

H3/P10 require stopping at three types if `jls.elem` coverage approaches
the floor, "measured after each element, headless, canonical JDK." That's
a good instinct, but the only enforcement is "recorded in the PR" — there
is no independent measurement step in the Completion Criteria. O5's own
"~4–6 classes per release" headroom estimate is explicitly flagged as "a
corpus figure the executor must re-measure, not a verified one," and a
stale `jacoco.csv` in the checkout (noted as inadmissible by the issue
itself for an unrecorded execution basis) is cited anyway as directionally
favorable. The whole "batch is four, not three" premise floats on a
self-reported number from the same party incentivized to ship four.
**Recommendation:** the PR template or a CI check, not narrative
recording, should gate this.

### 5. [Medium — deferred decisions dressed as resolved] Four of five "Open Questions" marked "Blocks execution" ship with only a recommended default, not an actual decision

Questions 1, 2, 3, and 5 in § Open Questions & Decisions Needed are each
marked "Blocks execution," but each is answered only with "Recommended
default: X." The Completion Criteria checklist item — "Every decision in
Open Questions & Decisions Needed is resolved (or explicitly deferred),
none left blocking" — can be satisfied by silently adopting the stated
default with no further scrutiny, converting a decision gate into a
formality that is trivially "passed." Two of these matter more than
narrative weight suggests: Q2 (which XOR generalization the help text
names) is a hard prerequisite for P12's pedagogical-honesty requirement,
and Q5 (does `RadixBridge` declare both radices, or infer one) governs
behavior of "the single point of semantic failure" the Threats section
itself flags as the most dangerous component in the batch. Neither should
ship on a default chosen inside the same document that also says "rides
along" for lower-stakes items.

### 6. [Medium — testability gap the issue itself half-admits] "Opt-in" palette placement has no enforcement mechanism

P9 and Global Invariant 2 require the default first-year palette to be
byte-unchanged, which is testable and good. But "opt-in" as a *UX*
property (not merely "absent from the default view") has no test named
beyond `PalettePedagogyRatchetTest`, which doesn't exist in the repo yet
and whose scope isn't pinned down — Open Question 4 admits placement is
still undecided, and the Threats section admits "a group that is opt-in
but visually adjacent to the default still violates progressive disclosure
in spirit" with no test proposed to catch that failure mode. As written, a
palette that technically satisfies "not in the default view" but is one
scroll away could pass every named test while missing the stated intent.

## What holds up (verified against `src/`, briefly)

- **O2 (types fail closed, attributes fail open)** — verified directly:
  `HdlExporter.EXPORTED` is an exact-class `Set.of(...)` (`src/jls/hdl/
  HdlExporter.java:422`), and an element in none of `EXPORTED`/`SKIPPED`/
  `TOPOLOGY` throws `HdlExportException` naming it (`:190-194`). The
  argument is real and correctly cited.
- **O4 (registration surface)** — `ElementRegistry.ALL` has exactly 35
  entries in `src/jls/elem/ElementRegistry.java`, matching the issue's
  count; `ElementVocabulary`, `SaveTags.WRITABLE`, and `Palette.ENTRIES`
  all exist as separate registration sites, also matching.
- **O6 (`TruthTable` digit-2 collision)** — verified verbatim at
  `src/jls/elem/TruthTable.java:79`.
- **Port names for the refusal list** (`Register.C`, `Memory.CS/OE/WE`,
  `TriState` control orientation) — all exist in source as named.
- **The corrective follow-up comment** (evidence-pin drift to `master`
  `828822672fc3a8e2cb6da25192472079f04c29dd`) checks out: current
  `HdlExporter.EXPORTED` starts at line 422 as the comment states, and
  `SaveTags.WRITABLE` on master indeed lacks `RegisterFile`/`FieldExtend`.

## Bottom line

The plan itself — RadixBridge first, `MIN_MAX` default with the exact
radix-2 collapse proof, digit-2-never-folds, per-element model tests
shipped in the same commit, a written-out (not computed) truth table —
is a genuinely careful design once its foundation exists. But right now
none of that foundation is in the tree, its blocking issues are open, the
prerequisite beneath *those* was closed as a duplicate rather than landed,
and the rationale documents this issue cites for its most consequential
design choices cannot be read by anyone working from this checkout. This
needs the dependency chain actually verified-landed and the cited
evidence either committed or re-derived before it is actionable, not
merely re-checked for stale line numbers.
