# Issue #540: FEAT-C24-5: every palette element has a print symbol — a palette-sweep export emits zero warnings, and a new element without one fails the build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

FEAT-C24-5 (PF-5 of capstone #505/CAP-24) wants a registry-keyed mapping from
every `ElementType` to a "print symbol," a `PrintSymbolTotalityTest`
(CAP-24 AC-4) whose palette-sweep export must emit zero missing-print-symbol
warnings, and a build failure when a new element type ships without one.
Grounded against `ARCHITECTURE.md`, `src/jls/elem/ElementRegistry.java`,
`src/jls/edit/PaletteContractTest.java`, `docs/standards-adoption/01-iec-ieee-symbols.md`,
and the parent issues #505 (CAP-24) and #315 (FEAT-001). No "print symbol,"
"PrintSymbolTotalityTest," or "SymbolSet" concept exists anywhere in `src/`
or `test/` today — this is greenfield.

## Findings, most severe first

**1. Filed in explicit violation of its own stated filing precondition, and the parent capstone's own audit already flagged this as unresolved.**
CAP-24 (#505) Open Question 1 reads: *"Print symbol standard: ANSI/IEEE-91
distinctive shapes only, or also IEC rectangular?... **Blocks PF-5's
filing.**"* Issue #540 was filed anyway, recording the recommended default
in AC-4 rather than resolving the question, and says so itself: *"OQ-1 is
marked as blocking this PF's filing; this issue records the default so the
decision is adjudicated explicitly, not defaulted silently."* That sentence
concedes the precondition is unmet — it doesn't satisfy it. The capstone's
own 2026-08-04 verification comment on #505 lists this as a live
"Contradiction / stale state": *"Filing preconditions vs. filings: Open
Question 1 is marked 'Blocks PF-5's filing'... yet #540... [is] filed with
the recommended default[] recorded rather than the question[] resolved...
the decisions remain unadjudicated."* No `REPLAN:` comment resolving OQ-1
exists on #505 as of this review. **Recommendation:** do not start
implementation until a `REPLAN:` comment on #505 formally resolves OQ-1 (or
explicitly waives the blocking rule); otherwise the work products of #540
inherit an unadjudicated foundational choice (ANSI-only vs. ANSI+IEC) that
could be overturned after the mapping is built.

**2. AC-1 requires an export mechanism that is deliberately sequenced to not exist yet, and the issue never says what runs instead.**
CAP-24's own dependency graph states `PF5 --> PF1` (PF-5 lands *before*
PF-1), and PF-1 — the actual print-styled SVG/PDF exporter — is tracked
separately as #536, still open. The only export path that exists today is
the screen-styled one from #154 (`CircuitRenderer.exportImage`), which
draws editor colors and the existing distinctive shapes, not a "print"
theme. Yet AC-1 says: *"a palette-sweep circuit containing every registered
element type exports with zero missing-print-symbol warnings"* — as if a
print-aware export pipeline capable of raising per-element warnings already
exists to run this sweep through. It doesn't, and #540 doesn't scope
building one (that's costed separately under PF-1's 4-6 mw). Either (a) AC-1
quietly requires a slice of PF-1's scope, which blows the stated 1-2 mw
band and duplicates work #536 is supposed to own, or (b) "export" here
really means a headless registry/data-walk with no rendering at all, in
which case the AC's wording ("exports," "warnings") is misleading and needs
to be rewritten before anyone can build or verify it. As written, a
reviewer cannot tell which of these two very different deliverables is
being asked for. **Recommendation:** either add `ordering_after: [536]`
(build PF-5's ratchet against PF-1's real exporter) or rewrite AC-1 to name
the concrete no-render mechanism (e.g., "every `ElementType` resolves
through `PrintSymbolRegistry` with no `Optional.empty()`") that this issue
can actually deliver standalone.

**3. Gameable acceptance criteria — totality is checkable, correctness isn't.**
Nothing in AC-1–AC-4 requires a print symbol be accurate to the standard it
invokes. `docs/standards-adoption/01-iec-ieee-symbols.md` (the project's own
grounding document for this symbol family) records that roughly 20 of the
35 registered element types have "no standard symbol" and are meant to be
"a plain rectangle with a JLS-local designation" — and explicitly warns
about "conformance theater": *"The project draws rectangles with `&` in
them, nobody buys the standard, the matrix is written from Wikipedia, and
JLS claims IEC 60617-12 conformance it cannot defend... the *likely* outcome
if the document purchase is deferred 'until later'."* AC-4 ties #540's
default symbol set explicitly to that same ANSI/IEEE-91 standard, but adds
no criterion requiring the primary documents be consulted, no reference to
`docs/symbol-conformance.md` (the matrix that same playbook calls "the
deliverable" for a real claim), and no check that a symbol is anything more
than *present*. A developer can satisfy "zero missing-print-symbol
warnings" by mapping every element — including the seven gates the standard
actually specifies precisely — to a placeholder box, and the ratchet goes
green while the actual outcome ("keeps print and screen renderings two
views of one symbol vocabulary" per the issue's own Outcome section) is not
delivered. **Recommendation:** either explicitly scope AC-4 as
existence-only and drop the ANSI/IEEE-91 framing from the acceptance
criteria (push correctness to #536/#537's conformance work), or add a
criterion that ties each mapped symbol to a matrix entry that names its
source clause.

**4. Title/body scope mismatch: "every palette element" vs. "every registered element type" are not the same set, and the difference is already load-bearing elsewhere in the tree.**
`test/jls/edit/PaletteContractTest.java:44-45` defines
`NON_PALETTE_TAGS = Set.of("SubCircuit", "WireEnd", "TestGen")` — three
registered types deliberately excluded from the palette (`SubCircuit` is
placed via Import, `WireEnd` is only ever a wire endpoint, `TestGen` is a
batch-only stand-in). The issue title says "every **palette** element has a
print symbol"; AC-1 and AC-3 say "every **registered** element type." A
"print symbol" is close to meaningless for `WireEnd` (not a drawn schematic
body) and dubious for `TestGen` (never placed via the GUI at all). The
issue doesn't say which of the two established sets (32 vs. 35) the mapping
must cover, so an implementer has to invent an exemption list on the spot —
which directly conflicts with AC-3's own requirement that "no element's
print symbol is hand-maintained outside the registry mapping." **Recommendation:**
pick one set explicitly (registry-total, matching AC-3's own equality-not-containment
convention established by #315/`PaletteContractTest`) and name the exemptions,
with reasons, up front.

**5. Redundant-gate risk the repo's own review already surfaced, with no dependency recorded against it.**
The issue's single comment (the project's automated cluster-review pass)
already found that `PrintSymbolTotalityTest` overlaps the general
registry-totality mechanism in #315 (FEAT-001) and recommended it "extend
#315's totality base rather than hand-rolling the set arithmetic," warning
that otherwise "it *is* one gate implemented twice." But #315 is itself
unimplemented — its own body states both of its tasks (TASK-0001: the
inventory, TASK-0002: the reusable JUnit base) are "not filed." #540
records `ordering_after: []`, i.e., no dependency on #315 landing first.
If #540 executes before #315, it will produce exactly the hand-rolled,
soon-to-be-duplicated totality test the review comment warned about, and
whichever lands second pays a rebase cost neither issue's cost band
(1-2 mw here) accounts for. **Recommendation:** add an explicit ordering
note (even if not a hard `blocked_by`) acknowledging the rebase-onto-#315
cost, consistent with the comment's own advice that "if this feature lands
first it should assert equality too so the later rebase onto the base
class is mechanical."

**6. Cost band likely optimistic, unreconciled against the more careful adjacent estimate.**
"Rides the existing registry-totality test pattern" overstates what exists:
per #315, the tree has six ad hoc totality tests, not a reusable base class.
#540's 1-2 maintainer-week band has to cover inventing the mapping type,
the totality test, the falsification-transcript process (AC-2), *and*
credible symbol choices for at least 8 non-trivial elements (Mux, Decoder,
Adder, Register, ShiftRegister, Memory, TriState, plus 6 gate leaves). The
project's own, more detailed estimate for a comparable (arguably narrower,
rendering-focused) slice of the same symbol work is 8-12 maintainer-*days*
in `docs/standards-adoption/01-iec-ieee-symbols.md` — the two numbers are
never reconciled, and that document's own priority call is explicit:
*"this competes with the VPAT/ACR item for the same budget... If you can
only do one this quarter, do the VPAT... No user has asked and no course is
waiting."* #540 doesn't address that competing-priority claim at all.

**7. Undefined artifact shape.** The issue never states what a "print
symbol" *is* as data — an enum tag, a `GateOutline`-style path record, a
qualifying-symbol string, an SVG fragment? AC-3's "registry-keyed mapping"
and AC-1's "missing-print-symbol warning" can't be reviewed for feasibility
without this, and different implementers would build incompatible things.

## What's solid

- AC-2's falsification requirement (a red run recorded before any pass
  counts) is genuinely good practice and matches the project's existing
  test-integrity culture (e.g. `HeadlessCoreRatchetTest`'s self-testing
  baseline).
- AC-3's registry-keyed requirement, including its explicit escape hatch
  ("if some type cannot be registry-keyed, stop and re-derive on #505"), is
  consistent with the equality-not-containment convention #315 and
  `PaletteContractTest` already establish in this codebase — the right
  shape of test, whatever it ends up extending.
- Scoping the symbol-set choice as "one global render mode... not
  per-element" (inherited from CAP-24, not restated in #540 but implied by
  AC-4) matches the correct design call already made in
  `docs/standards-adoption/01-iec-ieee-symbols.md`.

## Bottom line

The ratchet pattern itself is sound and matches how this codebase already
guards registry totality. But the issue was filed against its own declared
blocking precondition (finding 1), its lead acceptance criterion assumes an
export pipeline that is deliberately sequenced to not exist at the time
this issue would be built (finding 2), and none of its acceptance criteria
can distinguish a real ANSI/IEEE-91-grounded symbol set from a placeholder
rectangle (finding 3) — three problems any one of which should block
implementation until resolved by REPLAN or a rewritten AC set.
