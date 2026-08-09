# Issue #809: TASK-C595-1: every connect, width and name refusal names both disagreeing parties, their locations, and the edit that reconciles them
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

**1. AC-4's "outside `SimpleEditor`" requirement targets code whose only sanctioned extraction path is unfiled and blocked.**
Every connect/width refusal this issue is actually about lives inside
`SimpleEditor.java`, not beside it. `overlapMessage` is a field of
`EditWindow`, a `private class` nested directly inside `SimpleEditor`
(`SimpleEditor.java:1121,1264`), and every one of the connect-refusal
strings this issue targets — `"Bits don't match"` (:4015, :4142, :4247,
:4358), `"Can't connect output to output"` (:4365), `"Wire already has an
input"` (:4255), `"Wire will have multiple inputs"` (:4292),
`"Multiple connects to same wire not implemented"` (:4134) — is set inside
`EditWindow.canConnect(...)` methods and surfaced via
`info.setText(overlapMessage)`, all within `SimpleEditor.java`. AC-4 says
"a message the editor renders may come from anywhere except `SimpleEditor`
(KC-37-1)." The only issue that extracts this exact logic out of
`SimpleEditor` is #316 (FEAT-008), and #316's own body states TASK-0020
("the nine-state machine becomes a class... no drawing calls in its
transitions") is **not filed** and the feature is `blocked_by: [317, 337]`
— both open. #809's own `ordering_after: [TASK-C592-2]` does not name #316
at all, even though #809's parent feature #595 explicitly lists
`ordering_after: [#592, #316]` and calls #316 "a hard gate... if #316
stalls, this feature waits (KC-37-1)." #809 carries the letter of AC-4
without carrying its parent's own waiting clause.
**Recommendation:** add #316 (or its TASK-0020) to `ordering_after`, or
strip AC-4 down to what's achievable without it (e.g. permit new message
text inside `EditWindow` for now, tracked as a debt item against #316's
eventual extraction).

**2. `ordering_after: [TASK-C592-2]` names an open issue, and AC-3's catalog-correspondence requirement rests on scaffolding that doesn't exist in the repo at all.**
TASK-C592-2 is #803, confirmed **open**. AC-3 requires each landed message
to "correspond to a scored GAP row in #592's catalog." But #592's AC-1
deliverable — a catalog "published under `docs/`... graded HAVE / GAP /
REFUSE" — has no artifact anywhere in this checkout: `find docs -iname
"*parity*" -o -iname "*catalog*"` and a repo-wide grep for "Issie" (the
issue's own cited source of the messaging philosophy) both return nothing
outside issue-tracker prose. #803 is the task that would even add *scoring*
to catalog rows — and it too is open. So AC-3 asks for correspondence to a
row, in a catalog, with a score, none of which exist yet at two separate
levels. This is the identical structural defect this fleet's #811 review
found in #811/#803's other dependent task — #809 has the same broken
dependency, not a one-off.
**Recommendation:** block #809 on #592's catalog actually existing with
rows for connect/width/name refusals (not merely on #803's scoring
mechanic), and drop or rephrase AC-3 until then.

**3. AC-1's "every connect, width and name refusal" is an unbounded surface with no completeness bar, inviting partial compliance.**
The actual call-site count: at least 15 distinct `overlapMessage` refusal
strings across `EditWindow.canConnect` methods (`SimpleEditor.java:3995
-4365`), plus 53 `reject(...)` call sites across ~19 dialog classes
(`grep -c "reject(" src/jls/edit/*.java`) covering name/parameter
validation (`PinDialog.java`, `SubCircuitDialog.java`,
`JumpStartDialog.java`, etc.). "Every... refusal" names no completeness
mechanism — no ratchet test enumerating call sites (the way
`DialogCoverageRatchetTest` derives its count from `ElementRegistry`) — so
a corpus (AC-2) covering a handful of cases passes AC-1's letter while
dozens of untouched `reject("...")`/`overlapMessage = "..."` sites keep
their old rule-restating text.
**Recommendation:** enumerate the refusal call sites up front (a
grep-driven inventory, committed) and require the corpus to cover all of
them, with a ratchet test against new unrewritten sites — mirroring the
pattern #316's `DialogCoverageRatchetTest` already establishes in this
codebase.

**4. "Names both disagreeing parties... and states a concrete reconciling edit" is not a checkable format contract, so the corpus test can pass while the real goal fails.**
Several `overlapMessage` strings are reused verbatim across structurally
different call sites (`"Bits don't match"` at four separate locations:
wire-to-wire :4015/:4142, wire-to-put :4247, put-to-put :4358). Nothing in
AC-1 or AC-2 defines what "concrete reconciling edit" means as a testable
predicate — a corpus assertion that string-matches an expected substring
(e.g. "resize to N bits") passes even if the substituted "edit" is generic
boilerplate rather than derived from the actual two elements in conflict.
The stated verification (content matching against a committed corpus)
checks that *some* text pattern appears; it does not check that the text
is actually derived from the two real disagreeing parties, so a
low-effort implementation (templated strings with coordinates dropped in)
satisfies the letter without delivering what the Outcome section promises.
**Recommendation:** make at least a subset of corpus assertions
parametric/generative (assert the message contains the *actual* computed
element names/coordinates/bit-widths from the test fixture, not a fixed
literal), so a generic template can't pass by coincidence.

**5. "Location" is undefined for the anonymous elements that dominate the actual refusal surface.**
Most `canConnect` refusals fire on bare `Wire`/`WireEnd` objects, which
generally carry no user-assigned name (`Wire.save()` is a documented
no-op — wires aren't even persisted as named entities;
`src/jls/elem/Wire.java:119-126`). AC-1 requires "their locations" but the
issue never specifies what identifies an unnamed wire or gate: raw (x,y)
model coordinates, element type + coordinates, or nearest named neighbor.
This is exactly the case (unnamed wires and gates) that will be the bulk
of the corpus, and it's the one case the issue gives no guidance on.
**Recommendation:** state the location format explicitly (e.g. "type +
(x,y) in model units, falling back to nearest named element within N
grid units") before the corpus is written, so different refusal sites
don't invent incompatible conventions.

**6. KC-37-1 is cited as a binding constraint but has no independent, checkable repo artifact.**
KC-37-1 appears only inside the prose of #592, #595, #316 (as a review of
theirs is quoted) — there is no `docs/` file, code comment, or test named
`KC-37-1` anywhere in this checkout to check the constraint against.
This is a minor evidence gap rather than a defect in #809 itself (it's
inherited from the parent feature's own convention), but it means a
reviewer verifying AC-4 compliance has only issue-tracker text to go on,
not a repo-resident rule.
**Recommendation:** low priority; if KC-37-1 is meant to gate real PRs,
give it a durable home in `docs/` (even a one-line policy note) rather
than living only in issue bodies.

## What's solid

- The core diagnosis is accurate and verified against the codebase: current
  connect refusals genuinely are bare rule restatements with no location or
  fix — `"Bits don't match"`, `"Can't connect output to output"`, `"Wire
  already has an input"` (`SimpleEditor.java:4142,4365,4255`) — and current
  name refusals genuinely don't name the conflicting element —
  `reject("Name already used, try again")` (`PinDialog.java:186`). The
  motivating problem is real, not invented.
- AC-2's testing philosophy ("a message appeared" should stop counting as a
  pass) is a legitimate, well-aimed criticism of a common testing anti-pattern
  and is worth holding the implementation to.
- Feasibility for the *headless-test* half of AC-3/AC-4 is better than #316's
  ARCHITECTURE.md summary suggests: `test/jls/ui/` already ships 34 files
  including `EditorGestureTest`/`EditorGestureSupport`, so a mechanism for
  asserting gesture-driven status-line text exists today — the corpus
  infrastructure this issue needs isn't a from-scratch build, even though the
  "outside `SimpleEditor`" source location (finding 1) still is.
