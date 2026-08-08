# Issue #860: TASK-C581-3: a tagged release bumps the cask's version and sha256 without a human, so the cask cannot quietly point at a stale dmg
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of mechanism, #860 wants one property: **a `brew install --cask jls` can
never serve bytes older than the newest release, and nobody has to remember
anything for that to hold.** Everything else in the issue — a release-workflow
step, a tap credential, a PR bot, a red workflow — is one candidate mechanism for
that property, and it is the mechanism the title has already committed to.

The parent capstone is sharper than the task. CAP-34 (#518) AC-2 says "a release
propagates to all shipped channels with zero manual steps," and KC-34-1 says a
channel costing >0.5 mw per release cycle **is dropped**. So the real objective
function is not "automate the bump" — it is "make Homebrew's per-release marginal
cost round to zero, and prove it." #860 AC-4 exists to feed that kill criterion.
A design that costs zero forever beats a design that costs a little and is
measured carefully.

## The reframing: the update should be pulled, not pushed

#860 assumes JLS pushes into the cask. Homebrew is one of the ecosystems that
already pulls. A cask carries a `livecheck` block; Homebrew's own BrewTestBot
runs scheduled version detection over `homebrew/cask` and opens the
version+sha256 bump PRs itself, recomputing the sha256 by fetching the artifact.
JLS's release URL is the easiest possible case for that machinery: a plain
`releases/download/v<version>/JLS-<version>-aarch64.dmg` with the version in both
the tag and the filename (`scripts/build-installer.sh:470`), which the standard
GitHub-latest livecheck strategy resolves without a custom regex.

Under that design, the deliverable of #860 is **roughly ten lines of Ruby inside
the cask #858 writes, and no workflow, no secret, and no job at all in
`release.yml`.** Trace the ACs against it:

- AC-1 ("no manual step beyond approval") becomes literally true, and the
  approval is Homebrew's, which is the approval that actually gates what users
  install.
- AC-2 gets *stronger*, and stops being circular. Reading the sha256 out of the
  project's own `SHA256SUMS-installers-macos-aarch64` compares a number to itself
  — the same job that names the file generated it (`release.yml:635-649`), so the
  "mismatch" branch has no reachable scenario. A bump that independently fetches
  the published dmg and hashes the bytes Homebrew will actually serve is the
  non-circular check. The project's checksums asset and the provenance
  attestation then have a real job: a *cross-check* against an independently
  computed hash, which is the only arrangement in which a mismatch means
  anything.
- AC-3 ("red workflow, not a stale-but-green listing") stops being a thing that
  can be gotten wrong. Note how badly it fares under the push design: the macOS
  installer leg is `experimental: true` under `continue-on-error: ${{
  matrix.experimental }}` (`release.yml:289-312`), so today a release can publish
  with **no dmg at all** and stay green. A cask-update step bolted into that job
  inherits that greenness; a step in a new job inherits a new cross-job ordering
  problem. The failure mode AC-3 names is structurally native to the push seam
  and absent from the pull seam.
- AC-4's arithmetic collapses to a row reading "0.0 mw, upstream-maintained,"
  which is the strongest possible evidence against KC-34-1 rather than a number
  that has to stay under a threshold release after release.
- AC-5 ("a second maintainer could re-establish it, including tap credentials or
  bot configuration") mostly evaporates, because there is no credential and no
  bot to re-establish — the reproduction instructions are "the livecheck block is
  in the cask."

If #858 is forced onto a project-owned tap, the same inversion still applies, one
level down: put a scheduled `brew livecheck` / `brew bump-cask-pr` workflow **in
the tap repository**, where it needs only that repo's own `GITHUB_TOKEN`. The
credential problem in #860 exists entirely because the actor and the target are
in different repositories. Move the actor to the target and it is gone. This also
makes #860 **independent of #858's undecided fork in the road**, which the
current framing cannot claim — the issue as written has two different meanings
depending on a decision that has not been made.

## A second, deeper reframe: a formula over the jar, not a cask over the dmg

Worth putting on the table because it dissolves more than #860. FEAT-C34-3 (#581)
inherits three unpleasant facts from the dmg: it is unsigned by choice, so every
user must be talked through Gatekeeper (README:37-43); it is **aarch64 only**, so
Intel Mac users are told to go use the jar; and installers are explicitly *not*
byte-reproducible (README:53-60), so the cask pins bytes whose only guarantee is
the attestation.

A Homebrew **formula** that installs `jls-<version>.jar` with a launcher script
and `depends_on "openjdk"` inverts all three. No `.app` bundle means no
Gatekeeper prompt at all — which deletes the Gatekeeper caveat, deletes #581 AC-3
(the caveats-must-equal-the-README drift check), and deletes #859 outright. The
jar is arch-independent, so Intel Macs are served by the same line. And the jar
**is** the byte-reproducible artifact this project already stakes its
reproducibility claims on (README:92-95, `docs/reproducibility.md`), so the
sha256 in the formula is a hash anyone can independently regenerate from source —
strictly better provenance than the cask can offer.

I am not certain this beats the cask: `brew install --cask` is the idiom a macOS
user expects for a GUI app, homebrew/core's policy on GUI-only software pushes it
toward casks, and losing the `.jls` file association and Dock icon is a real UX
loss for the classroom audience. But it is a live option no issue in the C581
chain considered, and the honest comparison — one command, no Gatekeeper theater,
Intel included, reproducible hash, versus a proper macOS app bundle — belongs in
#858's recorded decision alongside the tap-versus-homebrew/cask arithmetic. The
two can also coexist: a tap can carry both, and the formula is the answer for the
Intel users the dmg abandons. That comparison is a paragraph of writing; it is
cheaper than either implementation.

## Where the issue pulls against the project's arc

**The custody precedent.** #136 refused a project-held GPG signing key on the
reasoning that "a single-maintainer signing key would add rotation/revocation
risk without adding a guarantee beyond what the checksums and attestation
already give you" (README:62-70, SECURITY.md). A long-lived PAT or GitHub App
with push rights to an external packaging repo, held in `release.yml`, is the
same shape of liability — a credential this project must rotate and can leak —
acquired to type two lines into a text file that a bot elsewhere would write for
free. ARCHITECTURE.md's recorded decisions read the same way repeatedly (plugin
loader removed rather than kept; second simulation strategy refused; i18n
declined; out-of-process isolation *reserved*, not built). The consistent
instinct is "do not own machinery whose value does not exceed its custody cost."
The push design contradicts that instinct; the pull design honors it.

**The duplication.** #853 (Flathub), #856 (winget), and #860 (Homebrew) are three
independently-filed tasks that each build a bump-and-push pipeline against a
different external repo with a different credential. That is one mechanism
implemented three times, three secrets, three ways to be silently stale — and it
is exactly the "four-vendor drift lesson from CAP-21's KC-21-3" that CAP-34's own
kill criteria invoke. The right seam is not a shared push helper; it is the
observation that **two of the three ecosystems already pull.** Flathub has
`flatpak-external-data-checker` (an `x-checker-data` block in the manifest, run
on Flathub's infrastructure); Homebrew has livecheck plus BrewTestBot. Only
winget genuinely lacks a first-party upstream watcher and genuinely needs the
push. Recognizing that turns three bespoke pipelines into one pipeline (winget)
plus two declarative blocks — and it is a CAP-34-level insight that #860 is the
clearest place to notice.

## What the issue gets right and should keep

- The failure mode it names is the correct one to design against. "Silently
  stale" is worse than "loudly broken," and it matches ARCHITECTURE.md's
  error-reporting philosophy exactly.
- Refusing to publish an unverified cask (AC-2's spirit) is right; only its
  chosen comparison is circular.
- Making the cost measurable (AC-4) rather than asserted is the discipline that
  makes CAP-34's kill criteria real rather than decorative. Keep it — the row
  just gets to read zero.
- Ordering after #858 and #859 is correct sequencing.

## Concretely, what I would change

I am disregarding the issue's central stated mechanism — "a release-workflow step
updates the cask" — and with it the parts of AC-1/AC-3/AC-5 that presuppose a
project-held credential and a project-side workflow status. The outcome
("releases propagate, staleness cannot be silent, cost is recorded") stands
unchanged; the implementation should be:

1. Reframe #860 as **"the cask declares how to find its own updates"**: a
   `livecheck` block in the cask, plus a committed one-page note recording that
   the update loop is upstream-maintained and how to verify it is running.
2. Add an AC that the sha256 is computed by hashing the fetched dmg and
   **cross-checked** against `SHA256SUMS-installers-macos-aarch64` and the
   provenance attestation — the non-circular form of AC-2.
3. Replace AC-3's red-workflow requirement with a **staleness detector**: a
   scheduled check comparing the cask's live version against the latest release
   tag, failing after a grace window. This is a handful of lines, it is
   path-independent (works for homebrew/cask and for a tap), and unlike a
   release-time step it catches staleness caused by *anything* — a rejected
   upstream PR, a livecheck regression, a failed macOS leg — rather than only by
   its own step erroring.
4. Fold the credential question into #858 rather than leaving it implicit, and
   note in #858 that the pull design needs none.
5. Ask #858 to record the formula-versus-cask comparison above before committing
   to the dmg path.

Verdict `rethink`: the goal is right and worth having, the ordering is right, but
the mechanism named in the title is the one piece of this that should not be
built.
