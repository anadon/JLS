# Issue #134: Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and #134 asks for one thing: **a first-year student on a
managed Windows lab machine should be able to install JLS without an
interstitial or a policy block.** That is squarely inside the project's arc —
`docs/grand-architecture.md:247-250` names a "DISTRIBUTION & SUPPLY CHAIN"
cross-cutting band and lists #134 in it by number, so the goal is not an
outlier.

But the issue conflates that goal with a second one it has already achieved
elsewhere: *artifact integrity*. Integrity is settled and settled well —
`SECURITY.md:58-101` records a deliberate **keyless** doctrine (Sigstore
build-provenance attestation over every artifact, per-OS `SHA256SUMS-*`, no
long-lived project key), #133 signs the container with keyless cosign
(`release.yml:232-234`), #135 resolved macOS as *unsigned by choice*, and #136
resolved rpm/AppImage as *no project-held GPG key, none planned*. Authenticode
buys JLS **zero** additional integrity. Its entire value is one thing: moving
a Windows OS policy dial. Everything below follows from taking that seriously.

## Alignment check: #134 is the one place the doctrine bends

SECURITY.md §3 states the project's own decision rule for adding a signing
mechanism: *"Detached GPG signatures are added only on concrete downstream
need … not a general 'signing is best practice' preference."* #134 was never
put through that test. It should be — and it plausibly passes, but the passing
argument is not the one the issue makes:

- **Weak argument (the issue's):** SmartScreen shows "unknown publisher."
  The issue's own P3 and §11 concede this is not fixed by signing —
  SmartScreen reputation is per-publisher-and-volume, an OSS-tier chained
  signature under the shared **SignPath Foundation** identity accrues nothing
  to JLS specifically, and at a university tool's download volume the
  interstitial may persist indefinitely. The headline user benefit is
  explicitly unassertable, by the issue's own text.
- **Strong argument (unstated):** managed-deployment and AppLocker/WDAC
  policies that reject unsigned MSIs *outright*. That is a hard gate, not a
  UX annoyance, and it is exactly the "concrete downstream need" shape §3
  demands. It is mentioned once in "Intended Audience" and never developed
  into evidence.

Cost side, also unstated: #134 adds a **long-lived credential** (`SIGNPATH_API_TOKEN`)
and an **external service in the release critical path** — a new
single-point-of-failure and a rotation/succession burden on a single-maintainer
project. The issue's repeated "no private key material" line answers a
narrower objection than #136's custody reasoning actually raises. It also
adopts a **publisher identity that is not JLS's**: every Windows user is told
the software's publisher is a foundation they have never heard of. That is a
defensible trade, but it is a trade, and #134 records it nowhere.

One tension the issue never checks, in its favor: signing embeds a timestamp
and mutates the file, which normally fights byte-reproducibility. Here it costs
nothing — `docs/windows-msi-determinism.md` already concludes the msi is *not*
byte-reproducible with jpackage/WiX and that the CI check was removed. Worth
stating explicitly, since the re-homed parent (#338, per comment 5227474245)
is the reproducibility feature and a reader will otherwise assume a conflict.

## Alternative framing 1 (the one I would take): let the channel, not the signature, carry the trust

The interstitial is a property of the **browser-download path** — Mark-of-the-Web
on a file fetched from a release page. #580 (`winget install jls`, already in
flight and already consuming this MSI by attested checksum via #855) changes
the path, not the file. A package-manager install is a fundamentally different
trust conversation: the channel vouches, the manifest pins the sha256, and the
"unknown publisher" question may never be posed to the student at all.

This is testable before spending any custody budget, and the test is cheap:

1. Land #580's manifest with the current *unsigned* MSI.
2. On a clean Windows VM: `winget install jls`, and record whether any
   SmartScreen/publisher prompt appears; then
   `Get-Content -Stream Zone.Identifier` on the downloaded MSI to see whether
   MOTW was applied at all.
3. Separately, test the hard gate: install the unsigned MSI under a default
   WDAC/AppLocker publisher-rule policy and record the failure.

Outcome (2) tells you how much of #134's *stated* benefit #580 already
delivers for free. Outcome (3) is the concrete downstream need that would
justify enrollment under SECURITY.md §3. Today #134 is being pursued on an
assumption neither has been measured. **I am explicitly setting aside the
issue's Definition of Done here:** its criteria measure whether a signature
verifies (P1/P2 — `osslsigncode`, `sha256sum`), which is instrumentation, not
outcome. Nothing in the DoD measures whether a student's install got easier.
The two experiments above are the acceptance criteria this issue should have.

## Alternative framing 2: the pattern that produced this issue is itself the defect

#134 is the second instance of a repeatable shape in this repo: *land complete,
secret-gated CI machinery for a feature whose activation requires a custody
action nobody has committed to, then wait.* The first instance is visible right
now and has gone bad:

- `release.yml:384-428` and `:530-560` carry a full GPG signing **and**
  verification pipeline for #136 — `RELEASE_GPG_KEY`, `RELEASE_GPG_PASSPHRASE`,
  `rpm -K` gate, AppImage `.sha256_sig` ELF-section check — roughly 60 lines in
  the release critical path.
- `SECURITY.md:60-62` says the opposite: *"no project-held, long-lived GPG
  signing key exists, and none is planned … resolves #136."*
- The GPG block compares against `resources/packaging/RELEASE-KEY.asc`, which
  **does not exist** in the tree (`ls resources/packaging/` — no `.asc`).

So master carries dead machinery for a decision that was reversed, and it is
load-bearing enough to be read as intent by the next contributor. #134 is on
the same trajectory: dormant since 2026-07-18, activation date unknown. The
architectural correction is a rule, not a patch — **custody-blocked CI does not
land on master; it lands on a branch with the runbook, and master gets one
sentence saying the artifact is unsigned.** Dormant-but-wired is only worth its
cost when activation is scheduled. Recommend filing the #136 workflow/doctrine
contradiction as its own issue immediately; it is a live inconsistency in the
project's most sensitive file, discovered en route, and #134's own DoD says
such discoveries get filed rather than absorbed.

## One concrete factual addition the issue missed

Observation 3 names only `README.md:31-32`. The same false present-tense claim
lives in a second file: **`SECURITY.md:68-71`** — *"Windows installers
additionally get Authenticode signing through SignPath.io's open-source
program (#134)"* — inside the document that defines the project's trust model.
Any correction must cover both, or the reconciliation criterion closes while
the security policy still misstates what ships.

## Recommended restructuring

1. **Now, unblocked:** correct the tense in `README.md` *and* `SECURITY.md`
   ("wired, dormant, releases to date are unsigned; verify by checksum and
   attestation"). Both sibling comments already argue this; the addition here
   is that SECURITY.md is in scope too. Split it out so it can merge today —
   a doc-truth fix must not be held hostage to an account signup.
2. **Next, cheap:** run the two #580 experiments above. Record the numbers on
   #134 and #580 (the missing `related` edge both recent comments flag).
3. **Then, decide under SECURITY.md §3:** if a real WDAC/policy block or a
   channel requirement is demonstrated, enroll — with the credential-rotation
   and third-party-identity trade recorded as a custody decision, not waved off
   as "no key material." If not, resolve #134 the way #135 and #136 were
   resolved — *unsigned by choice, with the reasoning written down* — and
   delete the dormant SignPath steps along with the dead #136 GPG block.

Either terminus is a good outcome. The current state — open, dormant,
doc-false, undated — is the only bad one, and it is the one the issue's
structure actively sustains.
