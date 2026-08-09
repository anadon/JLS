# Issue #723: TASK-C540-1: print symbols are a registry-keyed mapping with a totality ratchet — a new element type without one fails the build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

TASK-C540-1 is the "mechanism" half of FEAT-C24-5 (#540, itself PF-5 of
capstone CAP-24 / #505): build a registry-keyed `ElementType → print symbol`
mapping plus a `PrintSymbolTotalityTest` that starts red, before TASK-C540-2
(#725) authors the actual symbols against it. Grounded against
`ARCHITECTURE.md`, `src/jls/elem/ElementRegistry.java`,
`src/jls/elem/GateOutline.java`, `test/jls/edit/PaletteContractTest.java`,
`src/jls/JLSStart.java`, `docs/standards-adoption/01-iec-ieee-symbols.md`,
and parent/sibling issues #505 (CAP-24), #315 (FEAT-001), #540, #725. No
"print symbol" or `PrintSymbolTotalityTest` concept exists anywhere in
`src/` or `test/` today.

## Findings, most severe first

**1. The issue's central factual claim — "the FEAT-001 registry-totality
pattern reused, not a new one" — is false against the current tree, and
`ordering_after: []` hides it.**
#315 (FEAT-001) is the issue that would produce a *reusable* totality
mechanism. Its own body states both of its tasks are unfiled: "TASK-0001
(planned, not filed)... TASK-0002 (planned, not filed)" and pins evidence
that `git grep -l RegistryTotalityTestBase ... returns 0 files`. Confirmed
independently: `grep -rl RegistryTotalityTestBase src/ test/` returns
nothing in this checkout, and #315's only sub-issue (#492) is an unrelated
HDL-export-policy totality test, not TASK-0001/0002. What exists today is
six *ad hoc* totality tests (`ElementRegistryTest`, `PaletteContractTest`,
etc.), each hand-rolled, not a base class anything can extend. "Reused, not
a new one" therefore overstates what #723 can actually build on — it will
necessarily hand-roll a seventh ad hoc totality test, exactly the outcome
#315 was filed to stop happening again, and `ordering_after: []` records no
dependency on #315 landing first even as a soft note. **Recommendation:**
correct the Outcome section to "hand-rolled in the established pattern of
the six existing totality tests, pending #315's reusable base," and add an
explicit non-blocking note (as #540's review already recommended for the
parent feature) that this test is a rebase-onto-#315 candidate later.

**2. "Print symbol" collides with an existing, unrelated "print" feature
this codebase already ships, and neither #505 nor #723 disambiguates it.**
`src/jls/JLSStart.java` already has a literal print-to-physical-printer
pathway: a `Print...` menu (`:1468`), `assemblePrintBook`/`print(boolean)`
(`:2400-2421`+), a CLI printer-name flag `-p` (`:121`), and a headless
`printCirc` path (`:2888`) that all route through `PrinterJob` and —
critically — `CircuitRenderer.of(ed.getCircuit())` (`:2414,2417`), the same
renderer that backs the *screen*-styled `-i`/SVG export (#154). In other
words, "printing" in this codebase today already means "send the
screen-styled rendering to a printer/CLI job," not a distinct visual theme.
CAP-24's own Background section (#505) discusses only the `-i` image
exporter as prior art and never mentions this existing `Print...`/`-p`
machinery at all. #723 inherits the ambiguous term wholesale: does the new
"print symbol"/print-styled export interact with, replace, or ignore the
existing `Print...` menu item and `-p` flag? Nothing says so. A reader
skimming the tracker would reasonably expect "print symbol" work to touch
the thing already named `print` in the running application; it doesn't, and
that's never stated. **Recommendation:** add one sentence disambiguating
"print symbol"/"print theme" from the existing `PrinterJob`-based
`Print...`/`-p` feature, and note explicitly whether CAP-24's work is
expected to ever reach that pathway (the natural long-term expectation is
"yes, eventually" — camera-ready output should be what physically prints
too — which makes the omission more than cosmetic).

**3. AC-4's "shared seam" premise doesn't hold for most of the registry
today, so the acceptance criterion cannot be written as a real test yet.**
AC-4 requires "the mapping shares the screen symbol vocabulary's seam...
and a test asserts no third vocabulary exists." The one place a headless,
data-driven "screen symbol" concept genuinely exists is `GateOutline`
(`src/jls/elem/GateOutline.java`) — and by its own doc comment it covers
exactly six leaves: "AndGate, OrGate, NandGate, NorGate, NotGate, XorGate."
The other ~29 registered types (Mux, Decoder, Adder, Register, Memory,
ShiftRegister, StateMachine, TruthTable, …) draw themselves through bespoke
`Graphics2D`/Swing painting code with no equivalent headless data seam to
"share." A test that asserts "no third vocabulary exists" needs a concrete
predicate over concrete types on both sides; today one side (the screen
symbol) doesn't exist as data for 83% of the registry. As written this AC
is unfalsifiable at task-open time — an implementer cannot tell what
passing it even looks like beyond the 6 gate leaves. **Recommendation**:
scope AC-4 explicitly to the `GateOutline`-covered subset for this task, and
record the remaining ~29 types as a named, tracked gap (or fold "extend
`GateOutline`'s seam to non-gate types" into #723/#725's scope explicitly,
since right now it's assumed for free).

**4. Gameable totality — AC-1/AC-2 check presence, not content, and #723
sets up the exact scaffolding the sibling reviews already warned about.**
AC-2 only requires the test to "fail when any type has no print symbol."
Nothing constrains what a "print symbol" value must contain. A trivial
implementation — map every `ElementType` to the same placeholder marker —
turns the ratchet green while delivering none of the stated Outcome ("print
and screen are two views of one vocabulary"). This mirrors the same finding
already made against #540 and #725 (both reviewed in this repo,
`issue-reviews/issue-0540.adversarial.md` finding 3,
`issue-reviews/issue-0725.adversarial.md` finding 3); #723 doesn't cause the
gaming risk (that's #725's authoring job) but it is the issue that builds
the mechanism permitting it, and could close that door cheaply by requiring
the mapped type be non-trivial (e.g., a `GateOutline`-shaped record with at
least one drawn segment) rather than merely non-null. **Recommendation:**
have `PrintSymbolTotalityTest` assert the mapped value is structurally
non-empty (not just present), so a placeholder marker cannot pass.

**5. "Every registered element type" runs into the same undeclared
exemption set this codebase already maintains, with no exemption list
given.** `test/jls/edit/PaletteContractTest.java:44-45` excludes
`SubCircuit`, `WireEnd`, `TestGen` from palette/visual treatment for stated
structural reasons. AC-2/AC-3 here say "every registered element type" (the
full 35-entry `ElementRegistry.ALL`), not "every palette element" (32).
"Print symbol" is close to meaningless for `WireEnd` (a wire endpoint, never
a drawn body) and dubious for `TestGen` (batch-only, never placed via the
GUI). #723 doesn't say whether it reuses `PaletteContractTest`'s exemption
set, invents a new one, or truly means all 35 — leaving an implementer to
decide unilaterally, which risks silently violating AC-1's own "no
element's print symbol is maintained outside it" totality claim (a decision
made ad hoc isn't "in the registry"). **Recommendation:** state the
exemption set explicitly before work starts — most naturally, reuse
`NON_PALETTE_TAGS` with a one-line rationale for why or why not.

**6. Undefined artifact shape.** The issue never says what a "print symbol"
*is* as data: an enum tag, a `GateOutline`-style geometry record, a
qualifying-symbol string (`&`, `≥1`), an SVG fragment? AC-1's "registry-keyed
mapping" and AC-2's totality test cannot be reviewed for feasibility or cost
without this, and two implementers reading this issue in isolation would
build incompatible things that #725 (which depends on this task landing
first) would then have to pick between.

**7. The blocking-precondition problem on #505 is attenuated here, not
absent — worth a lower-severity flag rather than a hard stop.** CAP-24 Open
Question 1 ("Print symbol standard: ANSI/IEEE-91 distinctive shapes only, or
also IEC rectangular? ... Blocks PF-5's filing") has no `REPLAN:` resolving
it on #505 as of this review (confirmed: #505 carries exactly one comment,
the 2026-08-04 verification pass, which flags OQ-1 as still unadjudicated).
Unlike #540 and #725, #723 does not author any symbol content and its AC-1
mapping type could plausibly be defined abstractly enough to survive either
outcome of OQ-1 — so the blocking relationship is weaker here than for its
siblings. But it isn't zero: AC-1's "print symbol" still needs *some*
concrete shape (finding 6), and that shape is influenced by whether IEC
rectangular symbols are ever a second theme through the same seam (per
#505's OQ-1 recommended default) or never happen. Building the mapping type
before that's settled risks a reshape once #725/#536 land.
**Recommendation:** note explicitly in #723 (not just inherit silently)
that its mapping type is deliberately standard-agnostic pending OQ-1, so the
choice is visible rather than accidental.

## What's solid

- AC-3's falsification requirement (a red run recorded before any AC-4 pass
  is counted) is genuinely good practice and matches this project's
  existing test-integrity culture (e.g. `HeadlessCoreRatchetTest`'s
  self-testing baseline, and the same discipline credited in the #540/#725
  reviews).
- AC-1's explicit escape hatch — "if some type cannot be registry-keyed,
  stop and re-derive on #505" (citing KC-24-2, which is a real kill
  criterion in #505's body) — is the right shape of guardrail and correctly
  cites its source.
- Sequencing this mechanism task before the authoring task (#725) so
  symbols get written against a red test rather than a review checklist is
  a sound engineering call, and #725's own `ordering_after: [TASK-C540-1]`
  correctly points back at this issue.
- Scoping AC-2's test as a pure registry/totality check rather than routing
  through an export pipeline (contrast with #540's AC-1, which the sibling
  review found conflated with the not-yet-built #536 exporter) is the right
  choice and avoids that specific problem here.

## Bottom line

#723 makes one real improvement over its parent (#540) and sibling (#725)
by not entangling its acceptance criteria with the unbuilt print exporter —
that risk is genuinely retired here. But it introduces a factual error of
its own (finding 1: FEAT-001 has no reusable mechanism to "reuse," contrary
to the issue's own Outcome section), rests AC-4 on a screen/print seam that
only exists for 6 of 35 registered types (finding 3), never names what a
"print symbol" is as data (finding 6), and never disambiguates its central
term from a same-named, already-shipping feature in this exact codebase
(finding 2). Any of findings 1–3 should be resolved — by correcting the
issue text, narrowing AC-4's scope, or naming the data shape — before
implementation starts.
