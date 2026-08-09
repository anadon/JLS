# Issue #518: CAP-34: JLS is one command away on every mainstream channel — Flathub, winget, Homebrew — and release-asset downloads become the measured adoption KPI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What the issue is really for

Two distinct wants are welded together under one capstone: (a) *reduce the cost of
getting JLS onto a machine*, and (b) *know whether anyone is adopting it*. Both are
real. The chosen route — three third-party app stores plus a scheduled download-count
scraper — serves neither well, and on the project's own evidence it aims at the
dimension JLS is already strongest on while measuring a population that structurally
excludes JLS's only known users.

## Where it pulls against the project's arc

**1. It over-invests in JLS's best axis and prices itself against its own warrant.**
#510 scores install **4/5 — "best-in-desktop-class"** and names *learning* on-ramp
(2/5) and community (1/5) as the floors. #508 budgets this exact item — "Flathub/
winget/Homebrew, SVG gallery, download-count KPI" — at **≈1–2 mw**, ranked **last of
seven**. CAP-34 bands the same work at **4–7 mw**, a 3–4× inflation over the review it
cites as evidence, inside a project whose central verdict is "the arithmetic does not
close" (~1,100 mw against bus factor 1). #508 also set a planning ratchet — "no new
tier:feature/tier:task until two capstones close" — and the Phase-B comment filed five
(#579–#583) against it. A capstone that quadruples its evidence issue's own price and
breaks its evidence issue's own process rule is not executing #508; it is outvoting it.

**2. The premise "JLS ships zero channels" is false, and the two it does ship are the
ones worth copying.** `flake.nix` publishes a real package (`mainProgram = "jls"`,
desktop entry, `.jls` association) — `nix profile install github:anadon/JLS` is already
one command. `ghcr.io/anadon/jls` is a second, signed and multi-arch, serving the
autograder segment. Both are **self-hosted: no gatekeeper, no notability gate, no
per-release PR into someone else's repo, no vendor who can change policy under you.**
JLS's distribution model has already selected the right shape for bus factor 1. CAP-34
proposes abandoning that shape in favor of three vendor relationships, then adds a kill
criterion (KC-34-1, the CAP-21 four-vendor-drift lesson) that only fires *after* the
cost is sunk.

**3. The KPI cannot see the users.** #508's headline finding is that the live user base
— WashU CSE 260M — runs the **bsiever fork**, from a course page. Release-asset download
counts on `anadon/JLS` are therefore guaranteed to read near-zero *while adoption is
happening*, and a KPI that is blind to your only known deployment is worse than stars:
stars would at least move if those students starred. For an educational tool the
distribution channel is the syllabus, not the package manager. No winget manifest
reaches a student whose lab handout links elsewhere.

## Concrete blockers the issue does not consider

- **PF-3 is likely inadmissible, not merely costly.** homebrew-cask's acceptable-casks
  notability gate (as I know it: ~30 forks / 30 watchers / 75 stars for a GitHub-hosted
  project) is checked before review. #508 records **3 stars, 9 forks**. Verify the
  current text before spending anything — but note the irony: **PF-3 depends on the
  exact metric PF-4 exists to abolish.**
- **AC-1 contradicts Flathub's own policy.** Flathub review pushes open-source apps
  toward building *from source* in the sandbox; "checksum matches the attested release
  asset (no channel-specific rebuilds)" describes a repack Flathub does not want.
  Separately, the freedesktop `org.freedesktop.Sdk.Extension.openjdk` versions lag —
  JLS's floor is **JDK 25**, so PF-1 probably has to jlink its own runtime inside the
  sandbox. That is a channel-specific rebuild by construction. AC-1 as written kills
  PF-1; the honest invariant is *provenance from the same tag*, not byte-identity.
- **PF-2 rides an asset that may not exist.** `release.yml:300–310` marks the msi legs
  `experimental: true` under `continue-on-error`, and SignPath enrollment gates signing
  at `:584`. A winget manifest is a URL plus a sha256; pointed at a leg that can silently
  vanish, it publishes a broken install. The coverage comment spotted this (tension 2)
  and recorded it. `ordering_after: []` is wrong: **winget is genuinely after #443.**

## The alternative: own your channels, and count courses

I am explicitly disregarding AC-1, AC-3, and PF-3/PF-5 as written. A better-aligned
programme at roughly #508's original 1–2 mw:

1. **Homebrew tap, not core cask** (`brew install anadon/jls/jls`) — one command, no
   notability gate, no third-party reviewer, `brew bump` automation you own. Add a
   **Scoop bucket** the same way if Windows coverage beyond winget matters. This is the
   flake/ghcr pattern applied to two more ecosystems.
2. **winget only, among gatekept channels** — it is the one where the community repo is
   the sole path, the manifest *is* checksum identity (AC-1 satisfied natively), and
   `winget-releaser` makes per-release cost genuinely ~0. Sequence it after #443.
3. **Maven Central for `io.github.anadon:jls`.** The README concedes GitHub Packages
   "requires an access token even for public downloads" — that is a real, self-inflicted
   wall in front of the *one axis JLS scores 5/5 on*. Central publication makes JLS a
   declarable dependency for autograders and course CI. One-time namespace verification
   via the `io.github.anadon` identity; no recurring vendor drift.
4. **A `anadon/jls` GitHub Action in the Marketplace.** `uses: anadon/jls@v5` running
   `-t` vectors against `.jls` files in a course repo is the true "one command" for
   instructors, on the axis where JLS beats every competitor. The Marketplace listing is
   a discovery surface with screenshots (satisfies AC-4's intent) at the cost of a tag.
5. **Replace the KPI with an adopters ledger.** `ADOPTERS.md` — named courses,
   institutions, and forks-in-use — plus ghcr pull counts and release downloads as
   *leading indicators*, not the metric. This makes #508's item 1 (contact Siever/WashU,
   ≈0 mw, "highest leverage") the instrument rather than an unrelated errand, and it can
   register adoption that happens on a fork. Fold the download numbers into the release
   workflow (record the prior release's counts when cutting a new one): the API returns
   cumulative counts on demand, so **AC-3's scheduled cron buys only commit noise and a
   graph nobody requested.**

## The bigger reframe worth naming

#510 says the browser tools' zero-install advantage is "structural rather than earned,"
and that a **CheerpJ-wrapped read-only demo** would cut evaluation cost to competitor
levels — sitting in the same 4–7 mw band CAP-34 claims. Zero commands beats one command,
on every OS, with no vendor, and it fixes the *evaluation* problem (2/5 learning on-ramp)
that #510 calls the gate on everything else. If 4–7 mw of distribution money is going to
be spent, that is where it buys a dimension JLS does not already have, rather than a
fifth path to a dimension it scores 4/5 on. CAP-19's closure was maintainer-directed and
I do not relitigate it; I record that CAP-34 is competing for the same budget as the
revisit #510 explicitly reserved.

## If the issue proceeds as written

Minimum edits to make it defensible: correct the "ships zero channels" premise (flake,
ghcr); restate AC-1 as tag-provenance rather than checksum-identity; add
`ordering_after: [#443]` for PF-2; check the Homebrew notability gate before PF-3 gets a
minute-week; drop AC-3's cron; and reconcile the 4–7 mw band with #508's 1–2 mw before
the ratchet it violates is invoked against it.
