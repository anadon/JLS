# Issue #549: FEAT-C26-6: a keyboard-unreachable dialog fails the build — the standing a11y checklist becomes a CI ratchet over reachability and accessible names
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings

**1. [High] "K9" is cited as a settled constraint but resolves nowhere in this checkout.**
AC bullet 4 reads: "The default visual theme and existing-user experience are unchanged by
the gate itself (K9)." Nothing in this repo — README.md, ARCHITECTURE.md, CONTRIBUTING.md,
docs/, or the issue body itself — defines what "K9" names. It is presumably a shared
kill-criterion label from an external planning corpus (parent #507 also uses it in
`KC-26-4 (K9)`), but a reviewer or implementer working from this checkout alone cannot verify
what the constraint actually requires beyond the one paraphrase given. This is not unique to
#549: sibling issues in the same C26 family (#552, #766, #771, #780) hit the identical
undefined-shorthand problem for K9/D9, confirming this is a corpus-grounding gap rather than
a one-off phrasing choice. **Recommendation:** either inline the actual K9 text (as #381 is
reported to do for a sibling issue) or drop the citation and state the constraint in full.

**2. [High] The falsification requirement is gameable — "recorded" has no defined artifact.**
"A seeded keyboard-unreachable dialog in a scratch branch fails the build, and that red run is
recorded before any ratchet pass is counted" specifies no format, location, or verification
mechanism for "recorded." Compare this to the project's own established practice in
`docs/keyboard-a11y-verification.md` §"Red-on-break evidence (re-runnable)", which lists
concrete, re-runnable mutations and their exact failure signal (e.g. "Delete the `setup()`
`requestFocusInWindow()` handoff → the faithful keyboard-construction tests hard-red"). As
written, #549's AC could be satisfied by a sentence in a PR description asserting the red run
happened, with no reviewer able to reproduce or audit it. **Recommendation:** require the same
reproducible-evidence pattern #75 already set (a checklist-doc section naming the exact
mutation and the exact test that goes red), or a CI artifact link, not a prose claim.

**3. [Medium-High] A single seeded regression is not proof the gate generalizes.**
The AC is phrased around one seeded dialog ("a seeded keyboard-unreachable dialog... fails the
build"). Nothing requires the mechanism to generalize across the roughly two dozen dialog
classes this repo already tracks for completeness — see
`test/jls/ui/DialogCoverageRatchetTest.java`'s `SWEPT`/`REPRESENTED` sets, which exist
precisely because the project learned that per-dialog coverage rots silently without a sweep.
A gate that special-cases detection of the one fixture used to prove the AC (e.g. by checking
one component's focusability) can pass forever afterward while a different, unswept dialog
regresses unreachability through a different mechanism (disabled tab index, a custom
paint-only control, a modal focus trap). **Recommendation:** require the same sweep-and-ratchet
shape as `DialogCoverageRatchetTest` — completeness over the dialog/component set, not a single
fixture — or explicitly scope the AC to "at least this one class of regression" and say so.

**4. [Medium] Wrong prior issue cited for "accessible-name coverage."**
The AC says accessible-name-coverage ratcheting extends "the component-identity work landed in
#210." But #210's own text is explicit that it is *not* accessible-name work: its §11 Related
Work states "#75 (keyboard/accessibility — accessible names already added there)" and its §10
Threats to Validity states "This does not by itself make the dialogs keyboard-operable... here
it is only identity/labelling." #210 shipped `setName()` (programmatic lookup identity) and
`JLabel.setLabelFor()` (label association); accessible *names*
(`getAccessibleContext().setAccessibleName()`) were #75's `AbstractButton`-scoped invariant 4
(KeyPad pin tests). These are mechanically different Swing APIs. An implementer following #549's
citation could build the gate against `getName()` instead of the accessible-name API — the wrong
property. Separately, the AC doesn't say whether a `setLabelFor`-derived (computed) accessible
name satisfies "has an accessible name," or whether an explicit `setAccessibleName()` call is
required — a real ambiguity that risks false positives against #210-era dialog fields that are
correctly labelled but never call `setAccessibleName()` directly.

**5. [Medium] A cross-issue ordering defect is self-diagnosed but left unresolved.**
The issue's own follow-up comment states that #547 (FEAT-C26-5, the VPAT generator) omits this
feature from its `ordering_after`, even though #547's central claim ("no criterion claimed
without a passing test") is unsatisfiable for keyboard-operability/accessible-name criteria
until this gate's tests exist. The comment recommends a `REPLAN:` on #507 to fix it. As reviewed,
that REPLAN has not landed — #507's machine block and #547's machine block still disagree with
#549's own stated logical dependency. #549 does not block itself on that fix, so a scheduler
reading #507/#547 today would still plan #547 without waiting on #549.

**6. [Medium] "Demo slice… jointly with FEAT-C26-1" is a dependency that isn't declared as one.**
The Boundary section's funding note bundles this ratchet with FEAT-C26-1's "four-core-state
encoding" and "the grayscale screenshot test" into one funded slice, but `ordering_after` in the
machine block is `[]`, and FEAT-C26-1 appears nowhere in Acceptance Criteria. It's unclear
whether "jointly" means the two features must land in the same PR/commit (an undeclared
sequencing edge missing from the machine block) or merely that they're funded together while
shipping independently (in which case ownership of the grayscale screenshot test — whose AC does
it satisfy, #549's or FEAT-C26-1's — is unstated).

**7. [Low-Medium] ARCHITECTURE.md, which this issue implicitly relies on for feasibility, is stale about the very substrate this gate needs.**
ARCHITECTURE.md says test/jls/ui/ "Layer 2 (Swing harness under Xvfb) and 3 (render-to-image)
are reserved" — i.e., not yet built. But `test/jls/ui/package-info.java` (the file
ARCHITECTURE.md itself tells contributors to read before adding UI assertions) says Layer 2 is
"present, growing," naming real, already-shipped infrastructure this ratchet would sit on
(`FocusFaithfulKeyboardTest`, `EditorGestureSupport`, the #162 Xvfb CI lane already wired into
`.github/workflows/ci.yml`). This is good news for feasibility (the hard part already exists),
but #549 doesn't correct or even cite that substrate, so a contributor trusting the (wrong)
top-level architecture doc could badly overestimate the cost of `band_mw: 1-2`, or a reviewer
could wrongly reject the issue as infeasible on ARCHITECTURE.md's authority.

## What's solid

- The Boundary section correctly and explicitly declines to re-own #75's named residual
  (File-Close accelerator, #288 HDL-export entry, the human AT pass) — clean scope hygiene, no
  silent absorption.
- "The remainder stay an explicitly listed manual checklist, so the gate never claims more than
  it tests" matches the project's own established honesty discipline, already visible in
  `docs/keyboard-a11y-verification.md`'s "Deliberately out of scope / deferred" section.
- The one-comment self-audit (pass 2) that surfaced finding 5 above is itself good practice —
  the issue caught a real cross-issue defect; it just hasn't propagated the fix.

## Verdict

**needs-rework.** The core idea (turn an existing, well-evidenced manual checklist into a CI
gate) is sound and the substrate genuinely exists (finding 7 is good news, not a blocker). But
the acceptance criteria as written are gameable on the two axes that matter most for a gate
whose entire purpose is trustworthiness — the falsification "record" (finding 2) and the
single-seed generalization (finding 3) — and one AC cites the wrong prior issue for the
mechanism it's extending (finding 4). Fix findings 1–4 before implementation starts; findings
5–7 are real but don't block a rewrite of the AC text.
