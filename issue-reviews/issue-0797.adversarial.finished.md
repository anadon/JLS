# Issue #797: TASK-C586-2: a hand-committed screenshot and a manifest entry pointing at something that no longer exists both fail the build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Two ratchet checks over the capture manifest #796 (TASK-C586-1) is supposed to
introduce: (1) fail the build if any image referenced by README/hosted
manual/in-jar help has no manifest entry, (2) fail the build, naming the
target, if a manifest entry points at a window/pane/circuit that no longer
exists. AC-3 demands both failure modes be demonstrated red-then-green with
recorded transcripts; AC-4 demands the check not silently ride on #101's two
known weaknesses (record-only pixel gate, JBR-download fail-open) without
naming #411.

## Findings, most severe first

### 1. [HIGH] AC-1's "any image" scope contradicts the feature it implements, and is unsatisfiable as literally written

The parent feature (#586) scopes itself to **screenshots**: "AC-2: no
*screenshot* referenced by README, the hosted manual or the in-jar help is a
hand-committed file." #797's AC-1 drops that qualifier: "any image
referenced by README, the hosted manual or the in-jar help must trace to a
manifest entry." That widened wording sweeps in images that are not GUI
screenshots and that no headless-sway capture rig can regenerate.

Evidence: `resources/help/elements/keypad.html:11` (`<img src=keypad.jpg>`),
`:18` and `:24` (`up.gif`/`down.gif`), plus six wiring-help pages
(`resources/help/elements/wiring/{const,extend,output,input,start,end,bundle}.html`)
that embed toolbar-icon glyphs (`const.gif`, `extend.gif`, `opin.gif`,
`ipin.gif`, `jumpstart.gif`, `jumpend.gif`, `bind.gif`) — hand-drawn UI icons,
not captures of a window/pane/circuit. `scripts/wayland-rig.sh` has no notion
of "capture this icon"; it boots one JLS instance and screenshots the whole
mapped window (`scripts/wayland-rig.sh:309-335`). A literal AC-1 check would
immediately flag all nine as manifest-less and redden the build on day one,
forcing either (a) fabricating nonsensical window/pane/circuit manifest
entries for static icon glyphs, or (b) a silent, unstated carve-out —
exactly the kind of scope ambiguity the issue's own framing ("no image
outlives the UI it claims to show... intended") is supposed to prevent.
**Recommendation:** scope AC-1 to "screenshot" as #586 does (or explicitly
enumerate an exclusion class — icon/glyph images that predate and are
independent of the capture manifest — and say so in the acceptance
criterion, not leave it to the implementer to guess).

### 2. [HIGH] AC-1 checks a target that does not exist yet ("the hosted manual")

`ARCHITECTURE.md:252` records as a settled decision: "Help delivery: in-jar
now, hosted docs are the planned future... Hosted, versioned web
documentation is the planned future direction." No hosted manual exists in
this tree (`docs/` has no site-build output, no `docs/manual/`, nothing
matching); `CHANGELOG.md:667` and `docs/library-survey-2026-07.md:254`
confirm the same. Writing an acceptance criterion against "the hosted
manual" today means either the check is vacuous (nothing to scan, always
passes — false confidence that this leg of the ratchet works) or the
implementer invents a stand-in target not asked for anywhere in this issue.
Either way, a reader of a green build cannot tell which happened. This same
gap has already been flagged for two sibling issues by other reviewers
(`issue-reviews/issue-0781.adversarial.md:79-80`, `issue-reviews/issue-0519.adversarial.md:10`) —
it is not a one-off oversight, it is copy-pasted boilerplate from #586 into
every C586 task without adjusting for what's actually in the tree.
**Recommendation:** drop "the hosted manual" from AC-1 until #519 (hosted
docs) lands, or mark it explicitly N/A with a forward pointer, matching the
discipline #797's own AC-4 asks of #411's weaknesses.

### 3. [MEDIUM-HIGH] AC-2 presumes a window/pane addressing capability that doesn't exist in the rig or in the codebase

AC-2 requires failing loudly when a manifest entry "names a window, pane or
circuit that no longer exists." For that to be checkable, something must
know the current, ground-truth set of valid window/pane names. No such
registry exists: `grep` for window classes under `src/` turns up ad hoc
Swing frames/dialogs (`SimpleEditor`, `MemTrace`, `KeyPad`, `About`,
`TextDialog`, …) with no naming enum, and `test/jls/ui/package-info.java`
describes the UI-harness Layer 2 (Swing interaction) as "present, growing" —
not complete. Separately, `scripts/wayland-rig.sh` today launches JLS with
no circuit argument at all (`grep -n "circuit" scripts/wayland-rig.sh`
returns nothing) — it boots a blank editor and screenshots whatever's
mapped; there is no per-pane targeting or "load this circuit" capability to
validate against. #586, the parent, is explicit that reaching a non-default
pane depends on #91's interaction scripting and that this dependency "must
be stated rather than smuggled into this rig" — #797 never states it. Its
sibling #798 does call out an analogous gap (`issue-reviews/issue-0798.adversarial.md:78-90`),
which underscores that this omission in #797 is a real gap, not a
formality. **Recommendation:** either narrow AC-2 to "circuit" existence
(checkable today against files on disk) and explicitly defer window/pane
validation to whatever #796 or #91 delivers, or state the #91 dependency
plainly as #586 requires.

### 4. [MEDIUM] AC-3's demonstration requirement is a one-time proof, not a standing regression guard, and is gameable

"Both failure modes are demonstrated... with the transcripts recorded before
any pass is counted" asks for a PR-time demo, not a permanent test that
keeps failing if the check regresses to a no-op. Contrast #411 (this
project's own convention for this class of ratchet): "New regression tests
fail at the pre-change commit and pass at the fix commit" — worded as a
standing artifact in the test suite, and #411 O5/§7.11 explicitly worries
about "a scanner that passes because it found nothing" (the
`HeadlessCoreRatchetTest` empty-baseline failure mode). #797's AC-3 could be
satisfied by pasting two transcripts into a PR description and then landing
a scanner with a matching bug (e.g., a glob that silently matches zero
files after a later docs reorganization) — nothing in the acceptance
criteria requires the two demonstrations to persist as executable
self-tests the way `scripts/wayland-rig-selftest.sh` preserves the rig's own
failure-classification tests. **Recommendation:** require the two planted
scenarios to ship as permanent, always-run test fixtures (e.g. a
`*RatchetTest` or a self-test script case per the #411 idiom), not just a
one-time transcript.

### 5. [LOW-MEDIUM] Tight, undeclared coupling to #796's not-yet-landed schema

AC-2's vocabulary ("window, pane or circuit") presumes specific field names
that only #796 (TASK-C586-1, also open) will define — #796's own AC-1 says
only "naming circuit, window/pane and theme, in a documented schema"
without fixing the field names. `ordering_after: [TASK-C586-1]` correctly
sequences the work, so this isn't a logical contradiction, but it does mean
#797's acceptance criteria are written against a schema that doesn't exist
yet and could still change shape before #797 is picked up — worth flagging
so whoever implements #797 re-derives the field names from #796's actual
landed schema rather than this issue's paraphrase of it.

### 6. [LOW] Sizing looks light for the coordination surface

`band_mw: 0.5-1` for a task that must: parse three documentation surfaces,
consume a schema owned by a sibling open issue, extend or query the
wayland-sway rig for existence validation, produce two independently
red/green demonstrations, and cross-reference #411's known weaknesses by
name — is a smaller estimate than the comparably-scoped #411 (also framed
as a narrow "residual" but ballooning to a 14-section spec once fully
worked out). Not a correctness defect, but a feasibility flag if the
estimate is used to gate how much implementation latitude is granted.

## What's solid

- AC-4's requirement to name #411 rather than silently inheriting its two
  documented weaknesses (record-only `PIXEL_DIFF_MIN`, JBR-download
  fail-open) is well-grounded — both weaknesses are real and current at
  `.github/workflows/ci.yml` per #411's own citations, and requiring the
  check to say so in its own output is the right instinct, matching this
  repo's established "state the gap, don't hide it" convention.
- `ordering_after: [TASK-C586-1]` is correct and necessary — #797 cannot be
  meaningfully implemented before #796's manifest schema exists.
- Title and body are internally consistent (title's two clauses map cleanly
  to AC-1 and AC-2), and the issue stays narrowly scoped to the two checks
  rather than re-absorbing #796's or #798's work.
