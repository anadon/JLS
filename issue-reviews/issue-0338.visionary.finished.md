# Issue #338: FEAT-010 (RESIDUAL): every installer leg is a required check, its runtime is pinned by digest, and each format's reproducibility is either proven or bounded in writing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip away the six criteria and the DAG prose and one sentence remains: **a user
should be able to believe what JLS says about the bytes it ships them.** Two
things threaten that — the release ships fewer artifacts than it claims (the
hazard the workflow names itself at `release.yml:673-676`), and the project's
documents assert properties the artifacts do not have. That goal sits squarely on
the project's arc: JLS's documentary culture is already "claims are pinned by
tests, not prose" (`HelpTopicsTest`, `CliFlagTableTest`, `FileFormatSpecTest`,
`ExtensionPointCatalogTest`). This is that instinct applied to the release.
Endorsed as a goal without reservation.

The route is wrong in two places, and expensively so: 4–7 maintainer-weeks on a
feature the issue itself proves no capstone requires (`serves_capstones: []`;
#296 and #300 both say "beneficial, not required"). The same goal is reachable in
roughly one week along a different seam — and the cheaper route protects a *more*
important property than the expensive one does.

## Finding 1 — the most user-harmful defect here is a README sentence, and criterion 5 points away from it

`README.md:31-36` tells a first-year student, present tense, that the installers
"are Authenticode-signed through SignPath.io's open-source program, so the
publisher shown by Windows is **SignPath Foundation**" and that an installer
naming no publisher should not be run without extra verification. No signed msi
has ever shipped — `SIGNPATH_ENROLLED` has never been true, blocked on a
maintainer account action since 2026-07-18 (comment of 2026-08-08). So the README
**teaches users to trust an unsigned installer and pre-arms them to dismiss the
Windows warning that is telling them the truth.** Inverted security advice with a
live audience. The fix is a five-line edit needing no enrollment, no runner, no
decision. Criterion 5 is instead written as "verified by a check that fails" —
the branch that cannot be reached until a third party acts.

`README.md:54-60` has the mirror defect in the safe direction: it says the
installers are "*not* byte-reproducible" and that only the jar and `bom.json`
reproduce, while `docs/reproducibility.md` §1/§5 records deb, rpm and AppImage as
bit-reproducible on both architectures, hard-gated at `ci.yml:866` and `:923`.
The README understates the project's own achievement by three formats. Two
shipped documents, two contradictory verdicts.

## Finding 2 — the reframing: assert the claim table, not the bytes

Criterion 4 and IC-3 demand a per-format verdict for all five formats and price
the dmg half at 1–2 weeks. But `docs/reproducibility.md` §1 **already is** a
five-row verdict table, and §5 already gives each row its reason: Linux three,
yes and gated; msi no, because WiX regenerates ProductCode and component GUIDs
with no override; dmg no, because the HFS+ round-trip that would pin the volume
header yields an image `hdiutil` rejects as corrupt. The verdicts exist. Nothing
makes them *stay* true.

So IC-3's failing state — "a format with no verdict is a failing check" — is not
reached by writing more verdicts. It is reached by one JUnit test in the existing
ratchet idiom, `ReproducibilityClaimsTest`: parse the format list from
`scripts/build-installer.sh`, the claim table from `docs/reproducibility.md` §1,
the reproducibility and signing sentences from `README.md`, and the job names
from `ci.yml`; fail unless every format has exactly one row, every "Yes" row
names a job that exists, no "No" row is contradicted by README prose, and the
README's signing claim matches whether a signing gate can actually run. That
discharges IC-3 in full, fixes Finding 1 permanently rather than once, and turns
invariant 5 ("no claim is narrowed silently") from an aspiration into a
mechanism. Days, not weeks; no macOS hardware. Comment 3 lists a
`ReproducibilityScopeTest` among #471's residual — the idea is already in the
tree. My claim is that it is not a residual, it is **the deliverable**.

**I am explicitly disregarding criterion 4's symmetry over five formats.** The
formats do not share an adversary. The jar's reproducibility is load-bearing
because the jar enters other people's dependency graphs; a dmg's byte-identity
defends nothing that the per-file SHA-256 plus the SLSA build-provenance
attestation does not defend better — and §5 already says exactly that ("Their
integrity model, in the meantime, is *attestation-backed*"). Bounding koly-field
byte ranges for a format the project has already, correctly, decided to defend by
attestation is symmetry for its own sake.

## Finding 3 — `continue-on-error` is the wrong instrument, in both directions

Criterion 1 arms four advisory legs at 1–2 weeks of real-runner verification. But
the hazard is a **missing asset**, not a red leg. Arming is a poor instrument
either way: armed, a flaky `macos-latest` runner turns a tag push red *after* the
release published — which invariant 3 forbids acting on, so the red is noise a
bus-factor-1 maintainer learns to ignore; and armed or not,
`continue-on-error` catches a *failed* leg but not a skipped one, a cancelled
one, or one whose upload matched a different glob.

The elegant instrument does not exist today: **a release-completeness gate.**
`needs: installers`, `if: always()`, enumerate the published assets, assert the
expected set — five installers across their architectures, the matching
`SHA256SUMS-installers-*`, the jar, `bom.json`, the `.buildinfo` — fail naming
what is absent. One job, no flags, no manual per-platform walk, and it catches
every way an asset can vanish rather than the one way flags cover.

The pattern is **already in the repo and the issue never notices**:
`verify-windows-signatures`'s own comment says *"The exactly-two msi requirement
doubles as a missing-asset alarm."* Two consequences. Generalize it into its own
job rather than a side effect of signing. And — a live defect nobody has written
down — that alarm sits behind `if: env.SIGNPATH_ENROLLED == 'true'`, never true.
**The only missing-asset detection in the entire release pipeline is currently
dead code.** The hazard this feature is built around is today wholly undefended,
and flipping four matrix flags is not the shortest path to defending it.

## Finding 4 — IC-2 asserts a string, and the string is a safety feature

IC-2 is `grep -rn "UNVERIFIED\|PLACEHOLDER"` returning nothing. But
`build-installer.sh:317` reads:

```sh
elif [ -n "$JBR_SHA256" ] && [ "${JBR_SHA256#UNVERIFIED-}" = "$JBR_SHA256" ]; then
```

The `UNVERIFIED-` prefix is a load-bearing sentinel: the script refuses to fetch
unless the digest lacks it, falling back loudly to the build JDK's jlink image.
The placeholder is a **fail-closed default** — better engineering than most
digest pins, which fail open when someone edits a URL and forgets the hash. IC-2
can be satisfied by deleting the sentinel while leaving the property broken, and
violated by a correct design. Assert the property instead: every fetch in the
script is dominated by a `sha256sum -c` on the fetched bytes (the `appimagetool`
path at `:249` already is). Local, offline, permanent, and it survives a sixth
download being added.

The question never asked: **why bundle a JetBrains Runtime into the Linux
installers at all?** It serves one row of the README matrix (Wayland-native,
`DISPLAY` unset), at the price of a third-party fetch on the release path, a
per-architecture digest to maintain, a licence surface, and an extra
reproducibility input. Shipping the Temurin jlink image and pointing
Wayland-native users at `JLS_JBR_HOME`, the flake, or a JBR-flavored AppImage
would make criterion 3 largely evaporate along with #285 and half of #443. That
belongs as option (c) in OQ-4, which currently offers only two options that both
presuppose the bundling.

## Alignment with the project's arc

The issue is honest that no capstone requires it and that the fallback
beneficiary is "the release process itself." Taken seriously that is a sizing
verdict: against #78's element registry (sixteen manual steps to add an element,
per ARCHITECTURE.md), the HDL trajectory, and the RISC-V work, a 4–7 mw hardening
program on artifacts a student never inspects is not where a single maintainer's
next two months belong. The ~1 week version below is.

The meta-structure is also a cost the issue counts as a benefit. It claims to
collapse seven distribution issues into one acceptance surface, but it is the
eighth, has spawned #443 and #471, adopted #134, accreted four boundary comments
longer than the pipeline they describe, and carries an unresolved contradiction:
§6 calls #443 → #471 "a **real** dependency, not convention," while #471 records
them as independent because `ci.yml`'s reproducibility jobs invoke
`build-installer.sh` directly and never read `release.yml`'s flags. **#471 is
right** — `ci.yml:866` and `:923` call the script themselves and are already hard
gates. §6's central ordering argument is false, so the two-cut decomposition has
no critical path and §6 can be deleted. At bus factor 1 that adjudication is
worth more than any of the six criteria.

## The concrete alternative

Retitle to *"the release's claims about its own artifacts are true, and are
pinned by tests."* Deliver: (1) correct the README's Authenticode sentence to
future tense today; (2) `ReproducibilityClaimsTest` cross-checking format list ↔
`docs/reproducibility.md` §1 ↔ README prose ↔ existing CI job names; (3) a
release-completeness job, `if: always()`, replacing the dead alarm inside
`verify-windows-signatures`; (4) a script-local assertion that every fetch is
digest-verified, replacing IC-2; (5) keep criterion 5's check wired and
pending-enrollment exactly as OQ-3(a) recommends. Drop criterion 4's five-format
symmetry and the dmg bounding; keep the existing Linux gates. Keep IC-4's
clean-machine walk as #284's per-release ritual, not a blocker. Roughly 1–1.5 mw
— and it defends the one property in this cluster with a live victim, which the
4–7 mw plan does not defend at all.
