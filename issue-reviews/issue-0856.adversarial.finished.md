# Issue #856: TASK-C580-2: a tagged release opens the winget-pkgs PR on its own, and `winget install jls` on a clean Windows box installs the attested MSI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what's being attacked

#856 is TASK-C580-2 under FEAT-C34-2 (#580), ordered after TASK-C580-1 (#855, the
manifest generator). It asks for (AC-1) unattended submission of a winget manifest
PR to `microsoft/winget-pkgs` on tag, (AC-2) a one-time real-machine
`winget install` verification against the attested checksum, (AC-3) that
verification linked from #580, (AC-4) loud failure on rejection, (AC-5) minimally
scoped, documented credentials. Nothing under `winget/`, `wingetcreate`, or
`komac` exists in-tree yet (`grep -r winget` over the repo returns only prior
issue-review files) — this is greenfield work resting entirely on #855 landing
first.

## Findings, most severe first

**1. (High) AC-1's "no manual step beyond approval" does not say whose approval, and that ambiguity is gameable.**
Quoted: "A tagged release produces a manifest PR against `microsoft/winget-pkgs`
with no manual step beyond approval." If "approval" means the winget-pkgs
maintainers' own PR review (the only approval actually outside this project's
control), the criterion is meaningful. But as written it equally admits a reading
where a JLS maintainer manually runs the generator, manually opens the PR, and
then some other "approval" gate is satisfied — which is exactly the "someone has
to remember three days later" failure the Outcome paragraph says this task exists
to prevent. Recommendation: rewrite AC-1 to state explicitly that every step up to
and including opening the PR against `microsoft/winget-pkgs` is triggered by the
release workflow with zero human action, and that the only manual step in the
entire loop is the upstream reviewer's merge decision.

**2. (High) AC-3 makes task completion depend on a third party's uncontrolled review timeline, and the 0.25-0.5 mw band doesn't obviously price that in.**
"The end-to-end verification is done once on a real release and linked from #580,
including the submitted PR" requires a real `microsoft/winget-pkgs` PR to have
gone through upstream's own automated validation (and possibly manual review) —
a process this repository has no control over and that can stall for anywhere
from minutes to weeks if the manifest fails their validation on the first
attempt. The task treats this as a fixed, one-time cost inside a 0.25-0.5 mw
band; nothing in the issue acknowledges that closing AC-3 may simply be blocked
on an external party's schedule. Recommendation: state a fallback/timeout policy
(e.g., "if the PR isn't merged within N days, file a follow-up rather than
holding this task open indefinitely") so the acceptance criterion can't silently
convert into "wait for Microsoft."

**3. (Medium) AC-4's "never as a silent no-op" is an unfalsifiable universal claim.**
You cannot test a negative over the space of all possible failure modes. As
written, any reviewer can claim AC-4 is met after testing two or three scripted
failures (bad token, malformed manifest) while an unenumerated failure mode
(e.g., upstream repo renamed, rate-limited fork API, PR silently auto-closed by
a stale-bot) still produces exactly the silent-no-op outcome the criterion
forbids. Recommendation: replace "never as a silent no-op" with an enumerated
list of failure scenarios the workflow must be proven to catch (auth/token
failure, schema/validation failure, PR rejected or closed by a reviewer,
network/API failure), each with a required loud-failure behavior.

**4. (Medium) The issue is silent about the one pre-condition most likely to sink it: the Windows msi leg it's certifying is still `experimental: true` in CI.**
`.github/workflows/release.yml:301-303` marks the `windows-latest` (x86_64) msi
leg `experimental: true` with `continue-on-error: true` (confirmed also by
#443's Observation O3, itself still open), meaning a broken msi can currently
ship without failing a release. #580 (the parent feature) at least surfaces an
analogous risk for signing in its Boundary section and, as of a same-day
(2026-08-08) round-2 ordering comment on #580, explicitly documents a "degraded
mode" for the sibling AC-1 covering the Authenticode-signing gap (#134) —
but that correction lives only on #580's comment thread, not on #856. An
engineer picking up #856 in isolation has no way to learn that the artifact
they're wiring into a public package manager is still running on an
unpromoted, can't-fail-the-release installer leg. Recommendation: either add
`443` to `ordering_after`/`related`, or restate the risk directly on #856 the
way #580's Boundary section does — don't rely on a reader finding a sibling
issue's comment thread.

**5. (Medium) AC-2's "a clean Windows machine" doesn't say which architecture, and the ambiguity is gameable given #855's singular "installer URL."**
The release pipeline builds two independent Windows legs — x86_64
(`windows-latest`) and aarch64 (`windows-11-arm`, on Zulu since Temurin ships no
Windows-aarch64 JDK 25) — both still `experimental: true`. #855's own AC-1
speaks of "the version/installer/locale manifest triple" (singular installer),
leaving open whether the generated manifest — and therefore this task's
one-time verification — covers one architecture or both. As written, a
verification run against x86_64 alone satisfies AC-2's literal text while
leaving ARM64 installs silently unverified (or silently unlisted). Recommendation: name the architecture(s) in scope explicitly.

**6. (Low) AC-5's credential requirement doesn't name the identity that opens the PR.**
"Credentials for submission are held as workflow secrets with the minimum scope
needed... documented so a second maintainer could re-establish it" is reasonable
but doesn't specify whether this is a personal PAT (tying automation to one
person's GitHub account, 2FA, and org membership) or a dedicated
machine/bot identity or GitHub App (the standard bus-factor mitigation the
community tooling — `komac`, `wingetcreate` — is generally used with).
Recommendation: name the intended credential shape, not just its documentation
requirement.

**7. (Low) No package identifier is reserved or named anywhere in this task chain.**
Winget package IDs (`Publisher.PackageName`) must be unique across
`winget-pkgs`; neither #855 nor #856 states the intended identifier (candidates:
`anadon.JLS`, `Poplawski.JLS`). Left unstated, the executor could pick something
that collides with an existing unrelated `JLS` listing or is inconsistent
between the generator (#855) and the submission automation (#856).
Recommendation: pin the identifier in #855 (or here) before submission
automation is built against it.

## What's solid

- The `ordering_after: ["TASK-C580-1"]` edge correctly captures the real
  dependency on the manifest generator; no phantom or missing edge there.
- AC-2's use of "attested release asset" (not "signed") is actually the more
  careful choice — it ties the checksum comparison to the existing build-
  provenance attestation already shipped for every installer (README.md:50-52),
  not to Authenticode signing (#134), which is a separate, still-unenrolled
  effort. Read literally, #856 does not need #134 to close, unlike the looser
  "the signed one" language in #580's Outcome prose — worth confirming this
  reading is the intended one, since it's easy to conflate the two.
- AC-3's requirement to link the submitted PR from #580 gives the closure a
  concrete, checkable artifact rather than a self-reported "done."

## Recommendation

Sound in structure and correctly scoped relative to #855, but not ready to
hand to an executor as-is: tighten AC-1's "approval" language, add a fallback
policy for AC-3's external-review dependency, replace AC-4's unfalsifiable
"never" with an enumerated failure list, and either add `443` as a named risk/
dependency or restate #580's degraded-mode note directly on this issue so the
Windows-installer-leg risk isn't only discoverable via a sibling issue's
comment thread.
