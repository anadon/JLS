# Issue #816: TASK-C168-3: both peers see the same seven glyphs before Confirm is reachable, and a changed key for a known peer is loud rather than convenient
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Build the Verify dialog (`jls.collab.ui`, which does not exist yet) that
renders `Sas`'s seven glyph images/words, gates Confirm on both panels having
rendered with no keyboard-default bypass, and wires `KnownPeers.Trust` into
visible UI behavior (silent reconnect on VERIFIED, a distinct loud warning on
KEY_CHANGED). Five acceptance criteria, all plausible in isolation; several
have real gaps once checked against the code they depend on.

## Findings, most severe first

**1. The stated dependency isn't actually enforced — this can be picked up before its prerequisites land.**
The YAML header declares `ordering_after: ["TASK-C168-1 (glyph assets)",
"TASK-C168-2 (Share/Join dialogs)"]`, but the GitHub issue itself reports
`"issue_dependencies_summary":{"blocked_by":0,...}` — no real `blocked_by`
link exists to #814 or #815. Both prerequisites are open, unstarted work:
`jls.collab.ui` has no code yet (confirmed: `find src/jls/collab -type d`
shows only `net`, `session`, `op`, `crdt` — no `ui`), and #814's glyph images
don't exist (`Sas.java:16-21` javadoc explicitly says the images are "a
follow-up asset task"). A reviewer or contributor working the open-issue
queue by number, priority, or label (all three of #814/#815/#816 share
`area:collab`+`tier:task`) has no tooling signal stopping them from starting
#816 first and hitting a hard wall — there is no package to add the dialog
to and no images to render. **Recommendation:** file the actual GitHub
"blocked by" relationship (sub-issue/dependency link, not just prose) before
this is picked up.

**2. AC-1's "#101 precedent" citation overstates what that rig actually does — real cost is hidden.**
AC-1: *"asserted in a display-tagged test, plus screenshots captured on the
sway/xvfb rig (#101 precedent)."* #101's `scripts/wayland-rig.sh` /
`x11-rig.sh` boot **one** JLS main window and screenshot it, gated by
`swaymsg -t get_tree` / `xdotool` finding a single mapped window, with a
calibrated whole-frame pixel-diff. There is no existing machinery for: (a)
driving two live `Handshake`-completing peers to the point both show a
Verify dialog, (b) locating and screenshotting two distinct dialog windows
in the same CI run, or (c) asserting glyph-image equality between them. The
closer in-repo precedent is `test/jls/ui/GuiConstructionObservationTest.java`,
which screenshots a single window with `Robot`/`BufferedImage` inside one
GUI-boot lane — itself single-instance. Citing #101 as precedent makes a
two-peer, dual-dialog, pixel-comparison CI capability sound like reuse of
existing infrastructure when it is new infrastructure. **Recommendation:**
scope AC-1 honestly as "new dual-instance screenshot harness, modeled on but
not reusing #101," and size the task accordingly (this alone likely exceeds
the stated `band_mw: "0.5-1"`).

**3. AC-2's "no key binding can confirm" is either untestable-as-gameable or contradicts the project's own accessibility commitments — the wording doesn't say which.**
*"Confirm is disabled until both glyph panels have rendered, and no key
binding (Enter, default button) can confirm without an explicit click; a
test drives the negative case."* Two readings diverge sharply:
- **Literal reading** — Confirm can *only* ever be activated by a mouse
  click, never by keyboard (Enter, Space-on-focused-button, or any other
  binding) — directly conflicts with `docs/standards-adoption/03-accessibility-conformance.md`
  and `docs/keyboard-a11y-verification.md`, which treat keyboard operability
  (WCAG 2.1.1/2.1.2) as a load-bearing, test-pinned property elsewhere in
  this codebase (e.g. `ElementFormDialog.java:217` deliberately sets a
  default button for Enter-to-confirm on ordinary dialogs — the opposite
  policy). A literal implementation makes the Verify dialog unusable for a
  keyboard-only user, which is a regression this project has otherwise gone
  out of its way to avoid.
- **Intended reading** (most consistent with the prose's actual goal —
  "cannot dismiss the man-in-the-middle check by reflex") — keyboard
  activation is fine *after* both glyph panels render and the button is
  genuinely focused/enabled; what's forbidden is a *default*-button/Enter
  binding that fires before or independent of that state, and reflex
  dismissal via a stray Enter keystroke typed before the user has looked.

The AC text doesn't disambiguate, and "a test drives the negative case" can
be satisfied by either reading without revealing the conflict — a test that
merely asserts "pressing Enter with the dialog freshly opened does nothing"
passes under both readings, while an implementation that then also blocks
Tab+Space keyboard activation forever would pass the same test and ship an
accessibility regression uncaught. **Recommendation:** rewrite AC-2 to
explicitly require a keyboard-operable Confirm (Tab-focus + Space/Enter)
that is provably reachable only after both panels render and only via
non-default (no `setDefaultButton`) key activation, and add a positive
keyboard-confirm assertion alongside the negative one.

**4. The KEY_CHANGED warning path has no equivalent anti-reflex protection — the highest-risk case is the least constrained.**
AC-2's click-discipline (disabled-until-rendered, no default-Enter) is
scoped to "Confirm" on the ordinary Verify dialog. AC-3 requires the
KEY_CHANGED dialog be "visually and structurally distinct" but imposes none
of AC-2's discipline on *it* — nothing stops an implementation from giving
the warning dialog a single "Trust anyway" button with a default binding,
satisfying "distinct" purely on color/copy while offering an easier
one-click bypass than the path this issue is hardening. A changed key is
the scenario closest to an actual live MITM or peer re-installation; leaving
its accept-path unconstrained while over-specifying the benign-verify path
is backwards from a threat-modeling standpoint. **Recommendation:** AC-3
should require the KEY_CHANGED path carry at least the same click-discipline
as AC-2 (no default button, explicit non-reflex confirmation), not just
different visuals.

**5. AC-4 covers "declining" but not "closing the dialog any other way."**
*"Confirming writes the peer as VERIFIED and declining leaves the store
unchanged and the session closed."* Window-manager close (the titlebar X),
Escape, or Alt+F4 are not "declining" under a literal reading, and nothing
in the AC forces those paths through the same "session closed, store
unchanged" contract. An implementation could leave the socket open (a
partially-authenticated `SecureLink` sitting idle) if the user closes the
window instead of clicking a Decline button — exactly the "partially
trusted state" the AC says it wants to prevent, just reached by a different
door. **Recommendation:** AC-4 should say "declining, or closing the dialog
by any means other than Confirm."

**6. AC-1 has no fallback if TASK-C168-1 (glyph images) can't be closed as specified.**
#814's own AC-3 admits the image-sourcing effort might fail: *"if no
compatible set can be obtained, the gap is recorded and escalated to the
maintainer rather than vendored."* `Sas.java`'s API already exposes
`words()` independent of any image, so a text-only Verify dialog is
technically buildable without images. But #816's AC-1 hard-requires "the
same seven glyph **images**... asserted in a display-tagged test" with no
text-only contingency mentioned. If #814 stalls on licensing (a real,
acknowledged possibility in its own text), #816 has no documented degraded
path and would simply block. **Recommendation:** either #816 should state a
text-only fallback UI is acceptable pending #814, or the two should be
merged/re-sequenced so the licensing risk is resolved before #816 is scoped
as image-dependent.

## What's solid

- The core security property (both AC-1's glyph-match and AC-4's
  no-partial-trust) is well-grounded in the actual crypto: the SAS derives
  from the full-transcript final hash (verified in `Handshake.java`'s
  documented derivation and independently reviewed in
  `docs/collab-handshake-review.md` §3), so "both dialogs show the same
  seven glyphs" is a real, meaningful test of transcript integrity, not
  theater.
- `KnownPeers.check()`'s three-way `VERIFIED`/`UNKNOWN`/`KEY_CHANGED` return
  (`KnownPeers.java:149-160`) already gives the UI exactly the signal AC-3
  needs with no further net-layer work required — the task is UI-only, as
  claimed.
- AC-5's "no Swing below `jls.collab.ui`" is already a live, enforced
  ArchUnit rule (`ArchitectureRulesTest.java:150` `collabLayersAreHeadless`,
  `:181` for the analogous op-layer rule), so this criterion is trivially
  checkable rather than aspirational.

## Verdict rationale

Not `should-not-proceed` — the underlying crypto and trust-store plumbing
this issue's UI sits on top of is sound and the feature is worth building.
Not `sound-with-concerns` — findings #3 and #4 are not nitpicks, they're
places where the acceptance criteria as written can be satisfied by an
implementation that is either accessibility-regressing or actually weaker
on the MITM-warning path than the ordinary path, which is the opposite of
the issue's stated intent. Recommend `needs-rework`: tighten AC-2/AC-3's
click-discipline language, add the "any dialog exit" clause to AC-4, and
make the #814/#101 dependencies and reuse claims accurate before this is
picked up for implementation.
