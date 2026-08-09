# Issue #478: TASK-0077: a drawn register has a real reset with declared polarity, and both HDL emitters export it or refuse by name — never a register whose reset was quietly dropped
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is one of the fleet's unusually well-evidenced issues: every code citation (O1-O8) was checked
against `master` at `5311625` and is byte-accurate — the register's seven attributes, `react`'s
positional `inputs.get(1)`/`inputs.get(0)` reads, `copy`'s two-input/two-output copy, `HdlExporter`'s
`ins.get(1)`/`ins.get(0)` reads, the `type` setter's "leave unchanged on unknown value" idiom, the
67-file / 3-pair golden count, `RegisterStatement`'s `public final` fields, and `VhdlEmitter`'s
`claim(regName)` call are all exactly as quoted (line numbers shifted +58 from the branch-pinned
`evidence_commit`, which the issue's own comment already corrects). The design is unusually specific
about mechanism (§7.10's stage-by-stage transform), which is a real strength. That said, several
things need attention before execution.

## Findings, most severe first

### 1. [HIGH] The "no FORMAT bump" claim is declared "Not open" while the issue's own parent feature marks the identical question open and blocking

The issue states, in §7.1: *"No new tag, so no FORMAT version cost — a new attribute on an existing
element type is 'no bump needed' by the format's own rule,"* and in §9 Open Questions: *"Not open:
... no FORMAT bump (7.1)."*

But `docs/file-format.md` §9 — the very rule cited — immediately qualifies the "no bump needed" list
with a **silent-drop caveat**: *"Writers SHOULD therefore prefer a version bump over an 'ignorable'
attribute whenever dropping the attribute would change simulation behavior,"* and names `Memory`'s
`sync` attribute (the exact precedent H1/O-observation of #478 leans on) as the open, unresolved
instance of this class: *"whether files containing it should declare a bumped FORMAT version is an
open question tracked with issue #199's follow-ups."*

More directly: #478 declares `part_of_feature: 327`. Issue #327 (FEAT-037, the parent) says in its
own body: *"The real hazard is the opposite of a version conflict ... TASK-0077 must say **which one
it relies on** rather than assuming a bump protects it,"* and lists as Open Question 2, explicitly
**"Blocks filing children"**: *"Which discipline protects the new attributes from silent loss? ...
The two features must agree rather than each assuming."* Sibling issue #398 (TASK-0078) treats the
structurally identical hazard for `Clock.phase` as unresolved too, cross-referencing "#327 Open
Question 2" and marking it "Blocks execution."

#478 does not cite #327's Open Question 2 anywhere, does not name which discipline (fail-loud loader
vs. section-versioning epoch) protects the new `reset`/`rstpol` attributes, and instead asserts
settledness. Given #327 states this question blocks *filing* children, and dropping a reset silently
is exactly the harm #478's own "Intended Audience & Impact" section calls "the sharper harm" (an
exported register that doesn't match the drawn circuit's intent), this is a real contradiction between
the issue and its own declared governing documents, not a nitpick.

**Recommendation:** Before work starts, resolve #327 Open Question 2 (or get an explicit waiver
comment on #327 justifying (a) fail-loud-loader-only protection is sufficient for this task), then
update #478's §7.1/§9 to state the discipline instead of declaring the question moot.

### 2. [MEDIUM] The "export it or refuse by name" contract has no falsifiable prediction that ever exercises the refusal path

§7.10 stage 5 states emission is "total ... over {none,sync,async}×{high,low} for both languages, or
the configuration throws" `HdlExportException`. But P10 requires an unknown `reset`/`rstpol` string to
be caught as a **load** diagnostic (`IllegalArgumentException`-style, per the `Memory.setValue`
precedent at `src/jls/elem/Memory.java:376-380`), which means by the time a `Register` reaches export,
its `(reset, rstpol)` pair is always one of the 6 legal combinations. H4 requires both emitters to
handle all fields to compile at all. Nothing in §5 Predictions (P3-P12) or §14 DoD asks for a test
that actually drives the emitter into the `throw HdlExportException` branch. As specified, the
"refuse by name" half of the contract's headline claim can ship as dead code that no test exercises —
exactly the kind of gap the issue itself is trying to close for the *current* silent-drop bug.

**Recommendation:** Either add a prediction that constructs a `RegisterStatement` combination neither
emitter can express (if one is meant to exist, e.g. a target-language limitation) and asserts the
throw, or state explicitly in §7.11 that the totality is closed (no unreachable throw is expected) so
reviewers don't go looking for a test that was never meant to exist.

### 3. [MEDIUM] Adding a conditional third pin isn't reconciled with the register's one-shot, fixed-size layout

`Register.init()` (`src/jls/elem/Register.java:199-267`) computes `width`/`height` exactly once,
guarded by `if (width == 0 && height == 0)`, with `height = 5*s` for LEFT/RIGHT and `6*s` for UP/DOWN
— sized only for the two existing pins per side. The Method section's instruction — *"Add the `R`
input, appended last in all four orientation arms, created only when `reset != none`"* — says nothing
about how the fixed height formula grows to fit a third pin, nor what happens when a user toggles
`reset` on/off via the dialog **after** the element is already placed (at which point `width`/`height`
are already non-zero and the `if` guard above will not recompute them). This is exactly the kind of
implementation detail the issue is otherwise fastidious about (down to short-circuit suppression and
reset-net collision routing), and it's absent from §7.4's public-interface description of the `R`
input.

**Recommendation:** Add an explicit note to §7.4/§8 about geometry recomputation on attribute change
(does toggling reset force a resize? does it move existing D/C/Q/notQ pins, which would break already
-drawn wires?) before implementation, not discovered mid-PR.

### 4. [LOW-MEDIUM] The compile-oracle gate (P8) has no stated fallback if `iverilog`/`ghdl` are absent in the implementer's environment

The issue itself flags the risk (§6, "apparatus risk": a headless run today shows
"Skipped: 25" with exit 0) and correctly makes P8/DoD require recorded *executed* counts, not just a
green build. But it doesn't say what an implementer or reviewer should do if those tools genuinely
aren't installed in their environment — is the PR blocked entirely, or does the "no new skip" bar only
apply relative to whatever the CI environment already provides? As written, this criterion is solid
methodologically but operationally underspecified for anyone without iverilog/ghdl locally.

**Recommendation:** State where iverilog/ghdl are guaranteed available (which CI job/lane) and note
that local skip is acceptable as long as that CI lane's executed counts are the ones recorded.

## Solid, one line each

- O1-O8's code citations are all verified accurate against current `master` (adjusted for the
  documented +58 line shift) — unusually well grounded for this fleet.
- H1 (write-only-when-enabled, byte-identical resave) correctly cites and matches the real precedent
  (`Memory`'s `sync` attribute, `src/jls/elem/Memory.java:383`).
- O6's citation of the `type` setter's "leave unchanged on unknown value" idiom is verbatim-accurate,
  and correctly identified as the anti-pattern to avoid (P10) — the fix mechanism it points to
  (`IllegalArgumentException` at load, per `Memory.setValue`) already exists in the codebase.
- H4's "make fields `public final`, break both emitters at compile time" is a genuinely good,
  cheap-to-verify design constraint, and `HdlModel.RegisterStatement`'s current constructor
  (`src/jls/hdl/HdlModel.java:401-441`) is exactly as described, so the mechanism is real, not
  aspirational.
- Scope exclusions (O8: `ShiftRegister`, `RegisterFile`; §398's clock domains; §448's import side) are
  each backed by a real, correctly-characterized sibling/related issue rather than asserted blindly.
- The evidence-pin correction (issue comment, and cross-checked against #493) is accurate and already
  applied — line numbers cited in this review reflect it.

## Verdict rationale

The technical design (§7.10's transform stages, the positional-index trap, the golden-test plan) is
unusually rigorous and well-grounded in the actual codebase. But finding #1 is a real, evidenced
contradiction between this issue and its own declared parent feature over a question #327 explicitly
marks as blocking — that alone should stop execution until resolved, which is why this doesn't rate
plain "sound." Findings #2-#4 are fixable spec gaps, not fundamental flaws. Hence
**sound-with-concerns**: the mechanism is trustworthy, but the FORMAT-bump question must be resolved
against #327/#398 before work starts, and the dead-refusal-path and geometry gaps should be closed in
the issue text.
