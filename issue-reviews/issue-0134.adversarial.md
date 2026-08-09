# Issue #134: Authenticode-sign the Windows installers (SignPath OSS / Azure Trusted Signing)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what was checked

The issue claims the CI half landed via PR #194 at `2eb3e0c` and cites specific
line numbers at commit `29afb26`. Verified directly against the checked-out
repo at HEAD: `git log` confirms `2eb3e0c` ("Merge pull request #194 …") is on
master; `.github/workflows/release.yml` matches the cited content almost
line-for-line (`SIGNPATH_ENROLLED` at line 331, `force-sign` input at line 32,
`verify-windows-signatures` job at line 679, the job-scoped `actions: read`
grant at line 322, the SHA-pinned SignPath action at line 596); `README.md`
lines 31–36 still assert present-tense signing exactly as quoted. The issue's
factual grounding in the current codebase is accurate — most of this review's
findings are about the acceptance criteria and process, not about
misdescribed code.

## Findings, most severe first

**1. (High) The Definition of Done lets a self-acknowledged, live "inverted security advice" defect persist indefinitely, and it currently is persisting.**
The issue's own Observation 3 and DoD state: "either the first signed release
makes the present-tense claim true, or the claim is corrected — the defect
must not survive close." But close is gated entirely on a maintainer-only
account action (SignPath enrollment) with no deadline. The issue's own most
recent comments make the stakes explicit: comment `5227306676` (2026-08-08)
states "a user following the README literally should refuse every release
JLS has ever published" and recommends "**Correct the README now,
unconditionally**… this is the one item in this issue that is not blocked on
the maintainer's SignPath enrollment"; a follow-up comment `5227474245`
(same day) repeats the recommendation almost verbatim. Despite that,
`README.md:31-36` at HEAD is byte-identical to the version both comments
flag as defective — the fix was never applied. The DoD's "must not survive
close" framing means the acceptance test can be satisfied by leaving false,
safety-relevant guidance live for however long enrollment takes (weeks so
far, no committed timeline), which is precisely a case of "the stated
verification could pass while the real goal fails": the issue can close
clean while users were misled for its entire open duration.
**Recommendation:** split the README correction into its own immediately-actionable
task (or just do it as a same-day PR) independent of the signing rollout,
and add an interim DoD box: "README does not assert present-tense signing
while unsigned" as a standing invariant, not just a close-time check.

**2. (Medium) Machine-readable metadata block is stale relative to the issue's own latest comment.**
The body's `Status & Dependencies` YAML still reads `part_of_feature: none`.
The final comment (`5227474245`, 2026-08-08) explicitly says: "Corrected
field: `part_of_feature: 338`" and explains that with `none`, "a capstone
reviewing its installer story does not see it." The body was edited for a
template-conformance pass earlier (comment `5154440185`) but was never
updated for this later correction. Any tooling — or reviewer — that reads
only the body's machine block (the documented source of truth for this
project's `tier:task` template) gets the wrong parent. **Recommendation:**
edit the issue body's YAML block to `part_of_feature: 338`, not just the
comment thread.

**3. (Medium) H2 (checksums/attestation/release cover the signed bytes) has never been exercised against the real signing action, and the pipeline's file-handling makes the untested case unsafe if wrong.**
The claimed evidence for H2 (comment `4998376046`) is a hand-rolled
`openssl`/`osslsigncode` substitute run locally — not the pinned
`signpath/github-action-submit-signing-request@b9d91ea…` actually wired into
the workflow. The downstream steps that are supposed to operate on "the
signed bytes" glob the whole directory rather than a named file:
`sha256sum *` (release.yml:642), `subject-path: target/installer/dist/*`
(release.yml:629), `files: target/installer/dist/*`
(release.yml:653). H2's entire premise — "signing mutates the file... so it
must precede those steps" — depends on the unverified assumption that
SignPath's `output-artifact-directory` overwrites the original filename
in place rather than depositing a second file alongside it. If that
assumption is wrong, checksums/attestation/the published release would
silently pick up both the unsigned and signed msi (or the wrong one,
depending on sort order), and none of the described tests (which never ran
the real action) would have caught it. The mandatory `force-sign` dry-run
is the only place this would surface, but nothing in the issue's Method
tells whoever runs it to check for a stray second `*.msi` file in the
directory afterward. **Recommendation:** add an explicit assertion step
(`ls target/installer/dist/*.msi | wc -l` == 1 per leg, or filename-diff
before/after signing) to the force-sign proof step, and record it in the
DoD's falsification criteria alongside the existing three.

**4. (Medium) Hidden assumption: SignPath OSS enrollment is treated as a formality, not a gated approval.**
§6 (Materials & Apparatus) calls the maintainer's SignPath account "the sole
remaining blocker — cannot be created by CI or an agent," but nowhere
addresses that SignPath's OSS tier is an application subject to their own
review, with no committed SLA and no guarantee of acceptance. The originally
researched fallback (Azure Trusted Signing, ~$10/mo) is mentioned only as
a rejected-by-preference alternative in §1, not preserved as a contingency
if SignPath enrollment stalls or is declined. **Recommendation:** state
explicitly what happens if SignPath enrollment doesn't go through in a
reasonable window (e.g., fall back to Azure Trusted Signing, or formally
adopt the "claim corrected, ship unsigned indefinitely" branch).

**5. (Low) `blocked_by: []` understates the actual blocking condition for automated triage.**
The comment attached to the field concedes the real blocker: "a maintainer
account action (SignPath enrollment), not an issue." That's true in the
narrow sense that no other *issue* number blocks it, but any dashboard or
agent fleet that filters on `blocked_by` to find issues ready for
autonomous work will see this as unblocked, when in fact zero remaining DoD
boxes are actionable by an agent. **Recommendation:** a sentinel value
(e.g. `blocked_by: [maintainer-action]`) would make automated triage
accurate instead of technically-true-but-misleading.

**6. (Low) Process/scope proportionality.** The remaining work is "enroll in a
service, set two secrets, run a dry-run, tag a release, paste command
output" — yet the issue carries a full 14-section scientific-task template
(hypotheses, falsification criteria, concurrency model, six N/A interface
subsections, a mermaid diagram) whose design content was entirely settled by
a single 2026-07-17 comment. This isn't wrong, but it's a lot of ceremony
around zero remaining decisions, and it makes the one live actionable
finding (#1 above) harder to spot on a skim.

**7. (Low, noted not disputed) `actions: read` broadens a job that also holds
`contents: write`/`id-token: write`/`attestations: write`.** This is
correctly job-scoped rather than repo-wide (verified at release.yml:308-322,
consistent with #68's discipline), and the trade was explicitly reasoned
about in the landing comment, so this is a note for completeness under the
adversarial lens rather than an unaddressed defect: a compromised or
re-tagged SignPath action version could use `actions: read` to enumerate or
pull other artifacts from the same run. SHA-pinning mitigates re-tagging;
nothing in the issue discusses artifact-enumeration blast radius if the pin
itself were ever bypassed by mistake.

## What's solid

- Every technical citation checked against HEAD (line numbers, gating logic,
  job permissions, action pins) is accurate — no drift or fabrication found.
- Scope is well-bounded: no Java/simulation surface touched, siblings
  #133/#135/#136 correctly cited as closed and non-overlapping.
- The falsification criteria (§10) are genuinely falsifiable and the
  `force-sign` dry-run is a sound mechanism for proving credentials before
  a release depends on them.
- Failure-mode ordering (signing before checksums/attestation, verify job
  failing loudly *after* publish rather than blocking the release) is a
  reasonable, explicitly-justified design choice.

## Verdict rationale

`sound-with-concerns`: the technical design and its description of the
codebase are accurate and the remaining work is legitimately maintainer-only.
But the acceptance criteria have a real, currently-manifesting gap — a
security-relevant documentation defect that the issue's own audit trail
calls urgent and trivially fixable remains live at HEAD despite two
same-day comments recommending an immediate fix — plus a stale metadata
field and an unverified core assumption (H2) about how the real signing
action behaves. None of these block the maintainer's account-level next
step, but all should be addressed before this issue is treated as
"just waiting on one click."
