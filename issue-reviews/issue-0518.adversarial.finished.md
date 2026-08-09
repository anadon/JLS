# Issue #518: CAP-34: JLS is one command away on every mainstream channel — Flathub, winget, Homebrew — and release-asset downloads become the measured adoption KPI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the issue

CAP-34 is a non-code capstone: five planned features (Flathub, winget, Homebrew
cask, a download-count KPI script, and an optional Debian-ITP go/no-go),
`band_mw: 4-7`, citing #508 (product review) and #510 (niche survey) as
evidence that JLS ships zero package-manager channels while its closest
competitor ships four. A same-session comment already fanned this out into
five child issues (#579-#583) via a "Phase-B coverage workflow" pass and
self-flagged two contradictions. This review covers #518 as filed; it does
not re-verify #579-#583 individually.

## Findings, most severe first

**1. [HIGH] AC-1's "attested asset" guarantee outruns the pipeline it depends on.**
AC-1 reads: *"Each shipped channel installs a working JLS whose artifact
checksum matches the attested release asset (no channel-specific rebuilds
without provenance)."* PF-2 explicitly says the winget manifest sits "over
the signed MSI." But at HEAD, signing is not live: `SIGNPATH_ENROLLED` in
`.github/workflows/release.yml:331,684` is `false` unless SignPath secrets
are set, and both Windows legs (and the macOS/ARM legs) are `experimental:
true` with `continue-on-error: ${{ matrix.experimental }}`
(`release.yml:293-312`) — meaning a broken or absent MSI/dmg currently
cannot fail a release. The issue's own comment already half-admits this
("PF-2 names 'the signed MSI', but the MSI is not signed today... recorded
on #580 as a stated risk rather than resolved") but #518 itself never
narrows AC-1's wording, and `ordering_after: []` explicitly declines to gate
on #443 (the task that promotes those legs and arms signing prerequisites).
An acceptance criterion that references "the attested release asset" while
the asset pipeline that would make that attestation trustworthy is
optional/experimental is not verifiable as written — a reviewer can check
"checksums match" trivially even while the underlying artifact is an
unsigned, non-required-check build. **Recommendation:** either add #443 to
`ordering_after`, or rewrite AC-1 to state plainly what is and isn't
guaranteed today (e.g., "checksum matches the release asset; Authenticode/
notarization signing is tracked separately per #134/#443 and is not a
precondition of this capstone").

**2. [HIGH] The title's KPI claim is in tension with the capstone's own PF-1–3 goal.**
The title states release-asset downloads "become the measured adoption
KPI," and PF-4 collects them "via the public API." But PF-1–3's entire
point is to move installs *off* GitHub's release-asset counter and onto
Flathub/winget/Homebrew — ecosystems with their own (differently-shaped,
partly private) telemetry. Flathub distributes via its own OSTree
repository after the initial build fetch, so a Flathub install does not
increment GitHub's per-asset download counter the way a direct jar/dmg/msi
download does; winget-pkgs exposes no public per-manifest install
telemetry at all; Homebrew's own analytics are opt-out and aggregate, not
per-release. None of PF-1–4's text acknowledges that succeeding at the
capstone's headline outcome (more install paths) will decouple, not
inflate, the very number PF-4 is chartered to report as "the" KPI — nor
does it define how (or whether) per-channel numbers get reconciled into
one figure. AC-3 only requires "≥4 consecutive scheduled runs recorded
in-tree" — that checks the script's cron reliability, not that the number
it reports remains a meaningful proxy for adoption once multi-channel
distribution exists. **Recommendation:** PF-4 should either scope itself
explicitly as "GitHub release-asset downloads only, understood to
under-count/exclude channel installs" or commit to per-channel metric
collection (which is a materially larger ask than "0.5-1 mw").

**3. [MED] KC-34-1's kill threshold has no defined measurement method.**
*"A channel whose review/update process costs >0.5 mw per release cycle is
dropped with the arithmetic recorded."* Nothing in #518 says who logs
review-turnaround time, over how many cycles before a verdict is drawn, or
how wall-clock PR-review friction on a third party's moderation queue
(winget-pkgs, homebrew-cask) converts into "maintainer-weeks" on a
bus-factor-1 project. As written this is gameable in both directions: skip
logging and the kill criterion never fires, or cite one bad cycle to drop a
channel prematurely. **Recommendation:** specify the measurement (e.g.,
logged hours per version-bump PR, averaged over ≥3 releases) before this is
usable as a gate rather than a post-hoc rationalization.

**4. [MED] AC-2's "verified once end-to-end" is a weak bar for the actual risk.**
*"A release propagates to all shipped channels with zero manual steps
beyond approval clicks, verified once end-to-end."* The real risk to this
outcome is not the happy path (which one green run does prove) but
intermittent third-party review friction: winget-pkgs and homebrew-cask
version-bump PRs go through automated policy bots and human moderators who
can request changes, not just "approval clicks" — and #518 records no
fallback for a bump PR that stalls in that queue. A single successful
end-to-end run is exactly the kind of evidence that could pass while the
steady-state process still requires manual intervention on the second or
third release. **Recommendation:** require N≥3 consecutive automated
propagations, or explicitly define "manual step" to exclude/include
moderator-requested-changes cycles.

**5. [MED] Cost estimate diverges from the cited evidence without reconciliation.**
#508 §item 7 (the review this capstone cites as its `evidence`) prices
"Distribution (≈1–2 mw): Flathub/winget/Homebrew, SVG gallery,
download-count KPI" — i.e., roughly the same scope bundled with an SVG
gallery — at 1-2 mw total. CAP-34 itself bands at **4-7 mw** (and the
summed PF ranges, 1-2 + 0.5-1 + 0.5-1 + 0.5-1 + [1-2 optional], land in
that neighborhood too), 2-4x the source estimate, without a line
reconciling the discrepancy. Separately, #508's estimate folds "SVG
gallery" into the same bucket, but #518's body never claims that scope
(it correctly lives in CAP-27/#511 PF-4) — the capstone silently narrows
its own cited evidence's scope in one direction (drops the gallery) while
inflating it in another (the mw band), and neither move is called out.
**Recommendation:** a one-line note explaining why the filed band departs
from the cited evidence's figure.

**6. [LOW] Admitted-but-unresolved dependency on #443.**
The capstone's own filed comment concludes "CAP-34's `ordering_after: []`
is defensible... but AC-1's evidentiary strength does depend on #443
landing. Recorded here rather than converted into an ordering edge
unilaterally." That is an honest flag, but it leaves the actual decision
unmade — #518 ships with a formal dependency graph that its own author
believes understates a real risk. A reviewer approving #518's plan as
independently startable should know this isn't fully settled.

**7. [LOW] #518 is now mostly a rollup shell; the substantive work moved to #579-#583.**
The comment thread shows the capstone's own planned features have already
been filed out to five child issues in the same session that authored
#518. That is reasonable capstone hygiene, but it means evaluating #518 in
isolation (as this review's scope requires) cannot vouch for whether PF-1
through PF-5 are individually well-specified — that burden now sits on
#579-#583, none of which this review inspected. A "sound" verdict on #518
is a verdict on the rollup and its cross-references, not on the buildable
work.

## What's solid (no action needed)

- KC-34-2 (Debian ITP: no self-NMU maintenance at bus factor 1) is a
  concrete, well-reasoned kill criterion that correctly refuses to
  over-commit a single maintainer to a second packaging obligation.
- PF-3's requirement to carry the Gatekeeper caveat text "verbatim from
  README" correctly reuses existing, already-vetted user-facing language
  instead of inventing new messaging for the same unsigned-macOS-binary
  fact.
- The filing comment demonstrates real tracker hygiene: it searched for
  duplicates before filing, cross-referenced #443/#338/#511's actual scope
  boundaries, and self-reported two contradictions rather than quietly
  resolving them in the capstone's favor.

## Verdict rationale

Not `needs-rework`: the capstone is well cross-referenced, the individual
PFs are reasonably scoped and bounded, and its own comment thread already
surfaces two of this review's contradictions rather than hiding them.
Not `sound`: AC-1 and the title's KPI framing both contain real, unresolved
internal tensions that a careless implementer could satisfy on paper
without the stated outcome actually holding (unsigned "attested" artifacts;
a KPI that measures less of what it claims to measure the more successful
the capstone is). `sound-with-concerns` — proceed, but narrow AC-1's
wording, define KC-34-1's measurement, and resolve the KPI-scope tension in
PF-4 before treating those acceptance criteria as gates.
