# Issue #853: TASK-C579-3: a tagged release updates the Flathub manifest by itself, with the approval click the only human step — proven once end to end
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C579-3 is the "submit to Flathub and prove the update automation
once, live" step in the chain CAP-34 (#518) → FEAT-C34-1 (#579) →
TASK-C579-1 (#849, manifest) → TASK-C579-2 (#852, MIME/portal) →
TASK-C579-3 (#853, this issue) → TASK-C579-4 (#854, listing polish).
Taken alone the five ACs read as reasonable engineering. Taken against
the rest of the repository and the issue's own prose, three things are
wrong: the task proceeds as though the project's own standards
analysis didn't already recommend against this channel; its own AC-1
contains an escape hatch that lets the task close without ever doing
the thing the Outcome paragraph says must be proven; and the
0.25–0.5 mw effort band is incompatible with what the task actually
requires (two separate waits on third parties it does not control).

## Findings, most severe first

**1. (Critical) The whole Flathub effort contradicts the project's own recorded analysis, and this issue does not acknowledge it.**
`docs/standards-adoption/10-desktop-and-housekeeping.md:17` states the
verdict plainly: "AppStream metainfo (#175) | Do it in the **reduced**
form: one metainfo file, shipped in deb/rpm/AppImage, validated in CI.
**Do not** pursue Flathub." The "Flathub: recommend no" section
(lines 81–102) gives four concrete reasons, two of which land directly
on this task's own subject matter: the sandbox is "hostile to the
tool's actual job" — JLS writes `<circuit>.jls~` next to the user's
file (`src/jls/edit/Editor.java:103`,
`src/jls/edit/SimpleEditor.java:5388`) and is driven from shells and
autograders in batch mode, and under Flatpak that means
`--filesystem=home` ("which reviewers push back on") and a
`flatpak run` prefix that "breaks every command line in `README.md`
and `docs/batch-interface.md`"; and it "contradicts the recorded
deployment model: single self-contained jar plus per-OS installers, no
install step assumed, no network." The same document also names the
real cost this task is pricing: "Ongoing burden: a manifest repo to
keep building against runtime updates, which is the real cost and the
reason this playbook says no" (line 634–636). The document recommends
recording the decline as an `ARCHITECTURE.md` "Recorded decisions"
entry titled "Flathub: not pursued" (line 349) — that entry does not
exist (verified by reading `ARCHITECTURE.md`'s "Recorded decisions"
section in full: it covers i18n, help delivery, look-and-feel, the
plugin mechanism/boundary, extension points, and simulation strategy,
never Flathub). Neither #518, #579, #849, #852, nor this issue cites,
disputes, or overturns that verdict — the task chain simply proceeds
as if the internal "no" was never written.
**Recommendation:** resolve this at the capstone (#579) or feature
level before doing submission work: either write an explicit reversal
(a new `ARCHITECTURE.md` decision superseding the standards-adoption
entry, with the `.jls~`-under-sandbox and batch-CLI objections
answered) or shelve TASK-C579-3/4. Submitting to Flathub review while
the project's own analysis says not to is work spent re-litigating a
question already answered.

**2. (High) AC-1's escape hatch lets the task close without ever proving the thing the Outcome paragraph insists on.**
The Outcome text is unambiguous: "This is verified once, on a real
release, because propagation automation that has never propagated
anything is a plan rather than a channel." It also states as fact
that "The app is submitted to Flathub and passes their review." But
AC-1 hedges the same claim into an either/or: "The app is submitted to
Flathub and accepted, **or** the reason submission is
refused/blocked is recorded on #579 with the next step named." If
Flathub declines the submission — plausible, per finding 1's sandbox
objections, which are exactly the kind of thing Flathub reviewers push
back on — AC-1 is satisfied by a paragraph on #579, and AC-2 through
AC-5 (which all presuppose an accepted, installable, updating app)
become vacuous. Nothing in the issue says what happens to AC-2–AC-5 in
that branch: are they waived, deferred, or does the task simply not
close? As written, a maintainer can satisfy the letter of AC-1 with
"refused; next step: reconsider later" and close the task having
proven exactly the "plan rather than a channel" the Outcome paragraph
explicitly rejects as insufficient.
**Recommendation:** either drop the refusal branch from AC-1 (forcing
the task to stay open until the channel is real, consistent with the
Outcome text) or explicitly state that a recorded refusal closes the
*entire* task as "declined," not just AC-1, with AC-2–AC-5 struck
rather than left dangling.

**3. (High) The 0.25–0.5 mw band prices a task that is mostly waiting on two third parties it does not control.**
AC-1 requires a Flathub review outcome; the standards-adoption
document itself flags review turnaround as unverified but "commonly
reported as days to a few weeks" (line 632–634) — calendar time, not
maintainer-effort. AC-3 requires "a tagged upstream release," i.e. the
project's own next real release cycle, to actually fire — another
externally-paced event this task cannot accelerate by spending more
maintainer-hours. `band_mw: "0.25-0.5"` measures active work, but nothing
in that band accounts for a task that cannot be closed faster than
"the next time both a Flathub reviewer and a JLS release happen to
align." This is a category mismatch between the estimation unit used
throughout this issue chain and what AC-1/AC-3 actually gate on, and
it sits on top of #579's own AC-5 admission that the *ongoing*
per-cycle cost might exceed 0.5 mw and trigger a kill (KC-34-1) — i.e.
the feature's own kill-threshold band and this task's total band are
the same number, for two different things (this task's one-time setup
vs. the feature's steady-state cost).
**Recommendation:** split the band into "maintainer-active work"
(writing the automation, filing the submission) and a separately
tracked calendar/elapsed-time estimate, and do not let a long review
queue or a distant next release silently blow the mw budget without
anyone noticing, since mw is the only unit the rest of the chain
prices against.

**4. (Medium) AC-4's "never hand-copied" is a perpetual claim verified, at most, once — with no ratchet, unlike this project's convention for durable invariants.**
AC-4 reads: "The version and checksum in the updated manifest are read
from the release's published assets, never hand-copied." "Never" is a
standing invariant, but the issue's own verification model (AC-3: "a
tagged upstream release … demonstrated end to end once") only proves
it held for one release. `ARCHITECTURE.md` documents this project's
established pattern for exactly this kind of durable claim — ratchet
tests that fail the build the moment the invariant is violated again
(`NotificationRatchetTest` for the `TellUser` contract,
`HeadlessCoreRatchetTest` for `jls.sim`'s AWT-freedom,
`LookAndFeelPolicyTest` for the LAF fallback). No such mechanism is
required here; a future maintainer could hand-fix a broken automated
PR "just this once" and nothing would ever catch it, silently
reverting the property AC-4 claims.
**Recommendation:** require the update automation itself to be the
only path capable of writing version/checksum fields (e.g. generated
file with a header comment plus a CI check that diffs the generated
vs. committed manifest on every push to the flathub repo), not merely
a one-time demonstration.

**5. (Medium) AC-3's cross-repository automation implies a new credential whose compromise is a real supply-chain hazard, and AC-5 doesn't cover it.**
Per the standards-adoption document, a Flathub submission lives in a
separate `flathub/io.github.anadon.JLS` repository (line 85–86), not
in this repo. For "a tagged upstream release results in an updated
Flathub manifest with no manual step beyond approval" (AC-3) to be
true, something running from this repository's release pipeline must
be able to open a PR against that external, Flathub-org-owned
repository — which means a new secret (a PAT or deploy key scoped to
that repo) added to CI. AC-5 only covers the case of "assets missing
or checksums don't match" failing visibly; it says nothing about the
credential itself being a new attack surface — if that secret leaks,
an attacker can push a manifest update that Flathub's own bot may
build and publish, i.e. a supply-chain path this project's release
pipeline does not have today. The issue doesn't mention the credential
at all, let alone its storage, scope, or rotation.
**Recommendation:** name the credential mechanism explicitly in an AC
(scope, storage, and whether Flathub's own update-bot — which needs no
JLS-side secret — is used instead of custom CI), and treat a leaked
or over-scoped credential as a failure mode AC-5-adjacent language
should cover.

**6. (Medium) AC-1 acceptance is not obviously achievable given how the manifest it depends on (#849) is built.**
#849 (TASK-C579-1, the manifest this task submits) is scoped to fetch
"the published Linux release asset by URL and sha256 — no rebuild from
source inside the manifest" (#849 AC-1). Flathub review is well known
for pushing back on binary-only bundles that don't build from source
inside the sandboxed build environment; the standards-adoption
document itself calls the sandbox model a mismatch for this project's
build story. If Flathub review requires source builds (or an
`extra-data` module with its own restrictions and manual-download
warnings, which #849 doesn't mention), AC-1's "accepted" branch may
simply not be available with the manifest shape #849 specifies —
pushing every instance of this task down the refusal branch that
finding 2 already shows is under-specified.
**Recommendation:** confirm Flathub's current policy on binary-fetch
manifests (`extra-data` vs. build-from-source expectations) before
committing to #849's approach, and have this task's AC-1 acceptance
criteria reference that confirmation rather than assume it.

## What's solid

- AC-2 ("`flatpak install` … launches and opens a `.jls` file, on
  every architecture the manifest declares") is concrete and directly
  testable against #849's output — no notes.
- AC-5 ("a release whose assets are missing or whose checksums do not
  match fails the update visibly instead of publishing a manifest
  pointing at the wrong bytes") states the right failure posture —
  fail loud, never silently ship wrong bytes — consistent with the
  project's existing checksum/attestation discipline described in
  `README.md`'s "Installing JLS" section.
- The `ordering_after` on TASK-C579-1 (#849) and TASK-C579-2 (#852) is
  correct: the manifest and the sandboxed file-association work both
  have to exist before a live submission makes sense, and both sibling
  issues do exist under those exact task IDs (verified: #849, #852).
