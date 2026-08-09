# Issue #285: Linux installer runtime: arm the pinned JBR sha256s and Wayland-verify a JBR-bundled deb
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

Replace two `UNVERIFIED-PLACEHOLDER` sha256 pins in `scripts/build-installer.sh`
(one per Linux arch, for the `jbrsdk` JetBrains Runtime tarball) with real
digests, confirm the armed pin makes `build-installer.sh` bundle a JBR-derived
runtime instead of falling back to the build-JDK jlink image, and verify the
resulting deb boots under native Wayland (`WLToolkit`) rather than XWayland.
I verified the two cited code locations
([scripts/build-installer.sh:289-346](../scripts/build-installer.sh),
[.github/workflows/release.yml:269-276](../.github/workflows/release.yml))
still match the issue's description at HEAD (26297e8), well past the cited
`evidence_commit` 29afb26 — the citations are accurate, not stale.

## Findings, most severe first

### 1. [High] The proposed P3 verification path does not exercise the artifact the issue claims to verify

H2 claims "the resulting installed deb launches under a native Wayland session
using WLToolkit," and the Method's third checkbox says to "Wayland-verify P3
via #101's rig or a real Wayland session." But `scripts/wayland-rig.sh`'s own
header is explicit about what it launches:

> `# Requirements: sway, swaymsg, grim, jq; a JBR under $JBR_HOME; the shaded`
> `# jar from `mvn -DskipTests package` (or point $JAR at one).`
> `#   JBR_HOME        JetBrains Runtime root (must contain bin/java)  [required]`

That is: `$JBR_HOME/bin/java -jar $JAR` against the raw shaded jar. The
artifact this issue is actually about is materially different — a jpackage
`.deb` whose bundled runtime is a `jlink`-trimmed image built from the
`jbrsdk` tarball via `select_linux_runtime` in `build-installer.sh:330-341`
(module trimming, `--strip-debug`, etc.), invoked through the jpackage
generated launcher and the `%f`-carrying `.desktop` entry — not a bare `java
-jar`. Pointing `#101`'s rig at some JBR and calling that "Wayland-verified
the deb" would satisfy the checkbox while leaving the actual jlink-trimmed,
jpackage-launched runtime path completely unexercised: a bug specific to the
trimmed module set (a missing module the rig's full JBR papers over) or the
jpackage launcher's JVM args would pass this criterion and still ship broken.
**Recommendation:** state explicitly that P3 must launch the *installed* deb
(`dpkg -i` then invoke via the desktop entry or its generated launcher binary,
not `java -jar`), or extend the rig with a mode that points at the
jlink-trimmed image `select_linux_runtime` actually produces.

### 2. [High] The stated "only blocker" is contradicted by the repo's own CI, undermining the feasibility premise

Materials & Apparatus and the Open Questions section both frame this as
"fully specified... the only blocker is environmental (access to an
unproxied machine)." But `.github/workflows/ci.yml:378-379` states, in the
maintainer's own words:

> `# The authoring sandbox cannot reach cache-redirector.jetbrains.com`
> `# (egress-policy 403); CI runners have ordinary egress.`

The `gui-wayland` job already downloads a sibling tarball from the identical
host (`cache-redirector.jetbrains.com/intellij-jbr/...`) on every push and
nightly cron, and even prints the observed digest to the log for a
maintainer to copy (ci.yml:427-429, and the comment at L370-372 explicitly
invites copying it "from a green log"). The only reason this doesn't already
solve #285 is that CI fetches the `jbr` (runtime-only) flavor for the Wayland
rig, while `build-installer.sh` needs the `jbrsdk` (jmods) flavor's digest —
a different URL on the same reachable host. A one-line addition to an
existing workflow (or a one-off `workflow_dispatch` step) would produce the
verified digest with zero need for a human's "unproxied machine." The issue
never considers this, and as written invites a contributor to believe
physical/network access is the hard blocker when it demonstrably is not.
**Recommendation:** drop or revise the "only environmental blocker" framing;
add a CI step (or point at gui-wayland's own proven egress) to fetch+hash the
`jbrsdk` tarballs instead of requiring a human to do it from home.

### 3. [Medium] Asymmetric verification coverage between the two architectures the issue commits to arming

Observation 1 requires arming placeholders for **both** `linux-x64` and
`linux-aarch64` ("likewise linux-aarch64"). But the only automated Wayland
verification rig in the repo — `#101`'s `gui-wayland` CI lane — is x86_64-only
(`.github/workflows/ci.yml:353-462`; no aarch64 counterpart exists anywhere
in `ci.yml`, confirmed by grep). The Method's P3 step falls back to "a real
Wayland session" for cases the rig can't cover, but the parent issue #82
itself flags that "no ARM hardware available for the manual double-click
check" is an *open, unresolved* question, tentatively recommending a `WAIVED:`
for ARM manual checks on #284 — with no analogous waiver proposed here for
#285's aarch64 Wayland verification. The Definition of Done ("P1-P3 verified;
commands/logs recorded") doesn't distinguish per-arch, so it's ambiguous
whether an aarch64-only-P1/P2 (no real Wayland boot evidence) counts as done.
**Recommendation:** either scope aarch64 Wayland verification out explicitly
(mirroring #82's pending ARM waiver) or say where the hardware/session comes
from.

### 4. [Medium] Completion criteria for the digest itself are effectively self-attested

"Both placeholders replaced with digests whose fetch command + output are
recorded in the PR" is satisfied by pasting text into a PR description.
Nothing in the stated criteria requires an independent, automated
re-derivation of the digest (e.g., a CI job hitting the same URL and
asserting equality) before merge. Section 7.2 explicitly frames the input as
"treated as hostile until digest-verified" — appropriate rigor for a
supply-chain pin — but the verification method proposed for *arming* that
pin is exactly the kind of unverifiable human claim the rest of the design
is trying to avoid trusting blindly. A reviewer has no way to distinguish a
genuine fetch from a copy-pasted or fabricated hash short of trusting the PR
author. **Recommendation:** pair the human-recorded fetch with a CI
assertion step (which finding 2 shows is trivially available) so the merged
pin is machine-reverified, not just narrated.

### 5. [Low] Repeats a stale doc figure without flagging it

Materials & Apparatus says "standard build toolchain (JDK 17+, per
`scripts/build-installer.sh` header)" — accurately quoting
`build-installer.sh:14`, but that figure is stale against the rest of the
project, which has moved its floor to JDK 25+ (README.md:187,
CONTRIBUTING.md:13, `ci.yml` "Set up JDK 25"). Not introduced by this issue
and not blocking, but an issue this rigorously cross-referenced could have
caught and flagged the drift rather than propagating it verbatim.

## What's solid

- The licensing question (GPLv2+Classpath Exception permitting redistribution
  of JBR) is correctly deferred to the already-closed #82 adjudication rather
  than re-litigated — appropriate scope discipline.
- The fail-closed/fail-loud fallback behavior (`build-installer.sh:317-328`)
  is real and correctly cited; the issue correctly treats "keep the fallback
  working" as an invariant to spot-check, not just an assumption.
- Digest verification via `sha256sum -c` before extraction
  (`build-installer.sh:319-320`) is real and correctly cited — the pin
  actually gates extraction, it isn't decorative.
- Scope is honestly narrow: "two string constants change; no code paths, APIs,
  or data structures are added" (§7.4-7.10) matches what the diff would
  actually be.
- The falsification criteria (§10) are genuinely falsifiable and the
  disposition on failure (REFUTE, keep fallback) is spelled out rather than
  left implicit.

## Verdict rationale

The core task (arm two hashes, confirm the code path, spot-check the
fallback) is simple and well-anchored to real code. But the two High findings
are not nitpicks: one means the proposed acceptance evidence for the issue's
actual title claim ("Wayland-verify a JBR-bundled deb") can be satisfied
without ever touching the deb, and the other means the issue's own stated
blocker for *starting* the work is contradicted by evidence already sitting
in this repository's CI config. Both are cheap to fix in the issue text
before anyone picks this up. **sound-with-concerns.**
