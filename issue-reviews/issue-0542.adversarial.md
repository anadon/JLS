# Issue #542: FEAT-C26-1: every wire state survives grayscale — tritanopia joins the verified set, and thickness, dash and glyph carry state when color carries nothing
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

#542 (FEAT-C26-1) is PF-1 of capstone CAP-26 (#507): add tritanopia to
`ThemeTest`'s existing deuteranopia/protanopia delta-E ratchet, make every
wire state (high/low/HiZ/bus/error) distinguishable with color removed
entirely (grayscale, not just CVD-simulated), route the state-to-encoding
mapping through a registry with a build-failing totality test, and hold
the default theme pixel-identical throughout. Filed 2026-08-04, one
follow-up comment the same day. The boundary reasoning against #76 is
sound, but the issue has an uncited duplicate sub-task, an acceptance
criterion whose apparatus and test fixture do not exist in this checkout,
an acceptance criterion that conflates two different extensibility axes,
and a cross-issue ordering conflict (shared with #543) that is disclosed
but still unresolved.

## Findings, most severe first

**1. [High] AC-1 restates a task that is already filed as this issue's own child, without citing it, and the structural sub-issue link is missing.**
`issue_read(get_sub_issues, #542)` returns `[]`. But #729
(`TASK-C542-1`) declares `part_of_feature: 542` in its own machine block
and its Outcome is near-verbatim #542's AC-1: "Tritanopia is added to
ThemeTest's verified CVD set at the existing delta-E floor" (#542) vs.
"[t]his task adds the tritanopia transform and extends the assertion to
it, across every shipped theme" (#729). `issue_read(get_parent, #729)`
returns `null` — the relationship exists only as prose in #729's own
front matter, nowhere in #542's body or comment. #729's own AC set is
strictly more specific than #542's one-liner (cites a derivation source,
iterates the shipped-theme list instead of hardcoding it, has an explicit
no-lowering-the-floor clause). An implementer reading #542 alone has no
way to discover #729 exists, and could duplicate 0.5-1 mw of already-
scoped work, or work from the weaker restated criterion instead of #729's
stronger one.
*Recommendation:* link #729 as a real GitHub sub-issue of #542, and have
AC-1 point at it ("see #729") rather than re-deriving a weaker paraphrase.

**2. [High] AC-2's apparatus and test subject do not exist in the current checkout, and the issue does not name either as a prerequisite.**
AC-2 requires "an automated screenshot test showing every state pair
distinguishable... under all three CVD simulations and in full grayscale"
run against "the shipped adder lab." Neither exists: `find -iname
"*.jls"` across the repo returns only `test/fixtures/riscv-sum1to10.jls`,
`fork-4.6-shiftregister.jls`, `headless-canary-gate.jls`, and
`riscv/gui/cpu.jls` — no adder circuit, and `resources/samples/` (where
#73's roadmap would put one) doesn't exist at all. On the apparatus side,
`test/jls/ui/package-info.java` documents Layer 3 as "starter present":
`RenderAssert` only asserts an element paints and stays inside its index
bounds, swept by `RenderBoundsTest` — a containment check, not a
color/CVD-distinguishability analysis over a rendered scene. `ARCHITECTURE.md`
independently calls Layers 2-3 "reserved." #507's own Background section
runs `grep -rli "tritanop\|vpat\|tactile\|swell" src/ test/ docs/` and
gets `0` — confirming, at the capstone level, that nothing on this path
is built yet. The issue's boundary notes correctly flag that the same
apparatus is described three times (#76's planned task, this issue's
AC-2, #543's AC-3) and say "do not build a second screenshot matrix" —
but that recommendation presumes at least one exists to reuse; at filing
time, zero do.
*Recommendation:* add the adder-lab fixture and the CVD/grayscale
screenshot-diff apparatus as explicit, budgeted line items of this
issue (or a named prerequisite task) rather than assuming AC-2 is
"just write an assertion" over infrastructure that's actually a new build.

**3. [Medium] AC-3's totality claim conflates wire value-states with element types — two different, non-analogous extensibility axes — and the mismatch either makes the criterion vacuous or hides real scope creep.**
AC-3: "The state-to-encoding mapping is registry-keyed with a totality
test (FEAT-001 lineage), so a new element type that lacks an encoding
fails the build instead of silently regressing coverage." But the
issue's own title and Outcome scope this feature to "every wire state" —
a fixed 5-state set (touch/highlight/nonZero/wireOff/wireZero) drawn by
one class. `WireRenderer.draw` (`src/jls/edit/WireRenderer.java:57-91`)
is currently a hardcoded `if`/`else` chain over exactly those states, not
a registry over element types. The actual registry-with-totality pattern
this issue cites (`ElementRegistry`/`ElementRegistryTest`, "FEAT-001
lineage") is keyed by *element type* (~30+ pluggable classes: gates,
Memory, StateMachine, ...) — the axis that grows when someone adds a new
element. Wire value-states don't grow when a new gate class is added, so
"a new element type that lacks an encoding fails the build" describes a
scenario this issue's own scope (wire states only) cannot produce.
*Recommendation:* either narrow AC-3 to what can actually vary (e.g. a
new *wire state*, of which none are currently planned, making the
totality test permanently green and low-value), or — if the real intent
is that other element types' color-only semantic fills (StateMachine's
`initialState`, `watch`) also need redundant encoding and registry
coverage — say so explicitly and price that materially larger sweep
instead of letting it ride in under "every wire state."

**4. [Medium] KC-26-4/K9's "pixel-unchanged, gated on every commit" bar is unspecified in a way that is either unbuildable or gameable, and the project's own prior art already hit this exact problem.**
`ISSUE-AMBIGUITIES-2026-07.md` §6 (#101) records: "P2 pixel-diff computed
but not gated... RESOLVED: set the threshold from the first green run's
actual AE value (~10% of observed), not the blind 1% guess" — this
project already learned that literal pixel-identity assertions across a
real rendering path are unreliable (anti-aliasing/font rendering noise)
and settled for a measured tolerance instead. #542 states the bar as
"pixel-unchanged"/"pixel-identical" with no tolerance discussion, and
doesn't say whether it means (a) `Theme.DEFAULT` field-value equality —
cheap and already how `ThemeTest.applyRewritesTheJLSInfoStatics` checks
things — or (b) an actual rendered-canvas image diff like the Wayland
rig's. If (b) without a stated tolerance, the criterion either produces a
flaky gate (any AA/font variance fails "every commit") or gets an
unstated loose tolerance bolted on later that could let a real default-
theme regression through unnoticed — precisely the "verification could
pass while the real goal fails" failure mode this lens is asked to hunt.
*Recommendation:* state explicitly which of (a)/(b) is meant, and if
(b), adopt #101's measured-tolerance discipline rather than an unstated
exact-match.

**5. [Low] The "~126 hardcoded-black call sites" figure used to bound the render sweep doesn't match the checkout or the project's own sibling task.**
`src/jls/Theme.java:29` javadoc: "still drawn with hardcoded black in
~126 call sites." #542's boundary notes cite this figure verbatim. But
#76's own already-filed task #289 states "~113-site hardcoded-black
foreground sweep," and `grep -rn "Color\.black\|Color\.BLACK"
src --include=*.java` on this checkout returns exactly **113** matches —
matching #289, not the 126 the issue relies on.
*Recommendation:* cite 113 (matching #289 and the live grep) or explain
the ~12% discrepancy; a stale bound doesn't invalidate the issue but
shows the foundational number wasn't checked against the tree the issue
claims to have read.

**6. [Low-medium] The CVD-transform ownership conflict with #543 is disclosed but still unresolved, and #542's own `ordering_after: []` is the field that's wrong under the recommended resolution.**
#542's sole comment (pass 2, self-review) states plainly: "this issue
declares `ordering_after: []`, but AC-2 requires the screenshot test to
run 'under all three CVD simulations.' The CVD simulation is #543's
deliverable... Read literally, this feature must build a CVD transform
and #543 must then replace it with the framebuffer one — the duplication
both issues were written to avoid," and recommends resolving via a
`REPLAN:` on #507. No such REPLAN exists: #507 carries exactly one
comment (the 2026-08-04 coverage audit), not a decision closing this
question, and #543's own review independently confirms (finding 5) the
conflict is still open three comments into that thread.
*Recommendation:* same fix named in both issues' own comments — file the
REPLAN on #507 assigning the CVD-transform primitive to one owner before
work starts on either #542's AC-2 or #543's AC-3.

## What's solid

- The boundary against #76 is well-argued, not asserted: the comment
  distinguishes "#76 owns the shipped floor" from "#542 owns the ratchet
  built over it" with specific citations (`29afb26`, `ThemeTest`'s
  existing two-dichromacy floor) rather than a bare "not a duplicate."
- KC-26-1 (stop and re-derive if glyph escalation can't reach grayscale
  distinguishability without wrecking sighted legibility) is a genuine,
  falsifiable kill criterion, not a rubber stamp.
- The demo-slice funding split (2-3 mw jointly with #549: four core
  states + grayscale test + the ratchet armed, ahead of the 5-8 mw
  screen-reader band) is sound sequencing — it front-loads the cheap,
  high-value slice and retires the theming-totality risk before the
  expensive work.
- The downstream-consumer list (#543, #546, #547, #596) for the
  registry-keyed encoding is concrete and correctly identifies who
  breaks if this issue's data shape changes later.

## Verdict

**needs-rework.** The core idea — redundant non-color encoding plus a
tritanopia-complete, registry-backed, grayscale-safe ratchet over the
existing Theme seam — is sound and well-bounded against #76. But an
implementer picking this up today runs into: an uncited duplicate of
already-filed #729 (finding 1); an acceptance criterion (AC-2) whose test
fixture and test apparatus are both absent from the repo and unbudgeted
as such (finding 2); an acceptance criterion (AC-3) that cites the wrong
extensibility axis for its own stated scope (finding 3); an unspecified
pixel-identity bar this project has already learned the hard way needs a
tolerance, not an exact match (finding 4); and a self-disclosed,
still-open ownership conflict with #543 over who builds the shared CVD
transform (finding 6). None of these invalidate the feature; all of them
need a body edit or a REPLAN before work starts.
