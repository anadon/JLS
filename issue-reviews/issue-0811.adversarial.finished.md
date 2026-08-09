# Issue #811: TASK-C596-1: wire coloring lands as a scored catalog item — Digital #1308 closed or refused by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

**1. Every dependency this task actually needs is still open — including the one it declares.**
`ordering_after: [TASK-C592-2]` names #803 as the sole predecessor. #803
(*"each catalog row carries a funding score, a named acceptance vehicle and a
stop-loss column"*) is itself **open**. AC-3 says the item is "drawn from
#592's catalog and taken in score order" and that its "estimate and actual
are recorded" against #803's stop-loss column — but the catalog rows,
scores, and stop-loss column #803 is supposed to produce do not exist yet.
There is nothing to draw from and no column to record against. Two further
dependencies the issue never names in `ordering_after` are also open:
#542 (the CVD/grayscale test apparatus AC-2 requires, see #2 below) and #316
(the `SimpleEditor` decomposition AC-4 requires, see #3 below). A task filed
as workable today actually rests on three separate pieces of unfinished
scaffolding, only one of which it acknowledges.
**Recommendation:** either re-scope `ordering_after` to include #803, #542,
and #316 explicitly (making #811 wait, as #596 itself says it should — "if
#316 stalls, this feature waits"), or strip AC-2/AC-3's dependencies on
those issues' unshipped artifacts down to what can actually be checked today.

**2. AC-2 tests against a feature (#542) that hasn't shipped, using a fixture that doesn't exist.**
> "the grayscale and CVD distinguishability assertions (#542) still pass on a
> fully coloured fixture"

#542 (*"every wire state survives grayscale — tritanopia joins the verified
set..."*) is an **open** feature whose own acceptance criteria describe
apparatus that isn't built: a `CvdStateDistinguishabilityTest` screenshot
test, tritanopia added to the CVD set, a registry-keyed state-to-encoding
totality test. None of that exists at HEAD. What *does* exist is
`test/jls/ThemeTest.java`, which runs CIE76 delta-E checks over exactly six
fixed `Theme` colors (touch/highlight/nonZero/wireOff/wireZero/background,
`ThemeTest.java:132-141`) — a global palette, not a per-wire "fixture."
There is no mechanism today for a "fully coloured fixture" (a circuit where
every wire carries a user-assigned color) to be fed into any CVD test.
Read literally, AC-2 is unsatisfiable until #542 lands; read loosely, it's
gameable — `ThemeTest` will keep passing regardless of what #811 does,
because it never touches per-wire color, so "the #542 assertions still pass"
is true by construction and proves nothing about the new feature.
**Recommendation:** either block #811 on #542 landing first (so a real
fixture-based assertion exists to extend), or replace AC-2 with a
self-contained assertion this task can actually build and pin (e.g., extend
`WireValueChannelTest` — which already proves the stroke channel is
value-driven independent of color — to also assert it independent of an
*assigned* color).

**3. AC-4's "lands outside SimpleEditor" has no available implementation path in the current codebase.**
The only existing precedent for a comparable per-wire, user-assigned
attribute is probe naming, and it lives entirely inside `SimpleEditor`: the
menu item is built and named at `SimpleEditor.java:1158-1313`, the
accelerator is bound at `:1433-1434`, and the actual assignment logic is
`doProbe()` at `:5220-5256` — all inside the 5,852-line class KC-37-1 wants
this feature to avoid. The underlying mutation (`AttachProbe`/`RemoveProbe`)
already lives in the decomposed `jls.collab.op` package, but the *trigger* —
context menu, dialog invocation — does not. The extraction that would give a
new per-element property picker a home outside `SimpleEditor`
(#316's TASK-0020, "the nine-state machine becomes a class... no drawing
calls in its transitions") is explicitly **not filed** and is
**blocked by #317 and #337** per #316's own dependency graph. #596 (this
task's parent feature) calls this "a hard gate" and says the feature should
wait if #316 stalls; #811 carries the letter of AC-4 ("lands outside
SimpleEditor") without carrying that waiting clause.
**Recommendation:** either add #316 (or its TASK-0020) to `ordering_after`
so #811 is honestly blocked, or say plainly how a wire-color trigger reaches
the user without going through `SimpleEditor`'s existing menu/mouse
machinery — today no such path exists.

**4. Persistence granularity for "a wire's colour" is never specified, and conflicts with how wires are actually saved.**
`Wire.save()` is a documented no-op: *"Wires don't get saved"*
(`src/jls/elem/Wire.java:119-126`). The persisted element is `WireEnd`
(`ELEMENT WireEnd`, `WireEnd.java:586-589`); `Wire` segments are
reconstructed from `WireEnd` connectivity during `finishLoad`. AC-1 says "a
wire can be assigned a colour... persisted with the circuit" but never says
whether the color attaches to a `WireEnd` (and if so, which end, when a
wire has two, and what happens on a multi-segment `WireNet`?), to an entire
`WireNet` (many `Wire`s, many `WireEnd`s, no single element to hang an
attribute off in the current save format), or to something new. This isn't
a stylistic gap — it directly determines whether the feature colors one
segment or a whole net, which is a materially different user-facing
behavior and a materially different save-format change.
**Recommendation:** state the persistence unit explicitly (net vs. segment)
before work starts; note the `docs/file-format.md` change this implies and
get it reviewed against `FileFormatSpecTest`/`SaveTagsTest` up front.

**5. The compositing mechanism ("additional channel... never a replacement") is asserted, not designed, and the two readings available today diverge sharply in cost and in what AC-2 even means.**
`WireRenderer.draw` (`WireRenderer.java:57-91`) picks exactly one AWT
`Color` for the whole wire based on state (touch/highlight/off/nonZero/
zero) via `JLSInfo.Palette`; there is no dual-channel color rendering
anywhere in the drawing code today. Two literal readings of "composes over,
never replaces" are available and the issue picks neither:
  (a) the assigned color *replaces* the state-driven fill color outright,
      and grayscale/CVD distinguishability is carried entirely by the
      pre-existing stroke channel (dash/thickness, `strokeFor`,
      `WireValueChannelTest`) — cheap, but then AC-2 is vacuous, since that
      channel's distinguishability is already proven and untouched by this
      feature; or
  (b) some blended/dual-encoding render (e.g., color for identity, a
      second visual property for state) — this requires new rendering
      machinery nothing in `WireRenderer` today provides, at real cost.
Nothing in the issue chooses between these, so an implementer can satisfy
the letter of AC-2 by picking (a) and doing nothing new, or by picking (b)
and doing substantially more work than the 0.5-1 mw band implies (see #6).
**Recommendation:** pick (a) or (b) explicitly in the issue body; if (a),
say so and drop AC-2's dependency on #542 entirely since it adds nothing
verifiable.

**6. AC-3/AC-5's process criteria ("estimate vs. actual," "K9 holds," "cost ratchets... unmoved") name no mechanism this repository can check.**
A search of `src/`, `test/`, and `docs/` for "cost ratchet," "startup
cost," or "per-edit cost" returns nothing — the only hits in the whole
checkout are other reviewers' own review output files
(`issue-reviews/issue-08*.md`), not repository artifacts. There is no CI
job, JUnit test, or documented convention that defines what "no new
default-visible complexity" means numerically or how it's measured, so
AC-5 cannot fail a build; it can only be asserted true in a closing comment.
Likewise AC-3's "estimate and actual are recorded... exceeding 1.5x stops
the item" names no ledger, file, or test — "recorded" where, checked by
what? As written, both criteria are self-certifying prose, indistinguishable
in enforcement from simply not having them.
**Recommendation:** either point to the actual mechanism (a doc, a script,
a CI gate) that makes K9 and the 1.5x stop-loss checkable, or drop the
claim that they are enforced and describe them as review-time checklist
items instead.

**7. Band estimate (0.5-1 mw) looks thin once the real dependency chain is honestly counted.**
Taking reading (b) from #5, or even reading (a) done honestly, the scope is:
a new persisted attribute (with the save-format question from #4 resolved),
`Attribute`/dialog wiring for a color picker, `WireRenderer` changes, a new
`#91`-harness or headless test, and — per AC-4 — routing all of it through
whatever seam #316 eventually produces rather than `SimpleEditor`'s existing
(and only) per-element-property mechanism. At the low end of 0.5-1 mw this
is tight even ignoring #316; honestly priced with #316 as a hard
prerequisite, it's close to infeasible at this band.

## What's solid

- AC-1's "unset colour saves byte-identical" is a proven, testable pattern:
  optional persisted fields that emit nothing when unset already exist in
  this format (e.g. `Wire`'s own `probeName`), so this half of AC-1 is sound
  as stated.
- The underlying motivation — a colour channel must never become the sole
  carrier of wire state, because that regresses exactly the accessibility
  work issue #76 shipped (`WireRenderer.strokeFor`,
  `WireValueChannelTest`) — is well-grounded in real, already-merged prior
  art, even though the acceptance criterion built on top of it (#2, #5
  above) is not.
