# Issue #471: TASK-0028 (RESIDUAL): the msi and dmg get a gate over a property that can actually hold, and no reproducibility claim survives without one — the BOM guard and the independent rebuild already shipped
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the six checklists away and one sentence survives: **JLS should not be able to
publish a claim it does not check.** That is not a packaging concern; it is the same
idea as `ArchitectureRulesTest`, `HeadlessCoreRatchetTest`, `FileFormatSpecTest`,
`ExtensionPointCatalogTest`, `CollabSecurityRatchetTest` — a repo-wide culture of
turning maintained-by-care properties into enforced ones. On that axis the issue is
squarely on the project's arc and the research question in §3 is the right question.

Three things are wrong with *where* it cuts, and each is checkable from the tree
without running CI.

## R1 — the drift already happened, in the file the proposed test does not read

`docs/reproducibility.md` §1 row 4 claims deb/rpm/AppImage are **"Yes — gated by CI"**,
and §5 says so explicitly. `README.md:53-60` says the opposite, in the section every
downloader reads:

> "but the installers are *not* byte-reproducible (the native packaging tools embed
> wall-clock state), so rebuilding the same commit yourself will produce different
> checksums. That is expected; the jar and `bom.json` are the byte-reproducible
> artifacts"

That is unqualified — it covers the deb/rpm/AppImage that `installer-reproducibility`
and `installer-reproducibility-aarch64` have gated since #189. A second live falsehood
sits at `README.md:29-36`: "The installers **are** Authenticode-signed through
SignPath.io's open-source program, so the publisher shown by Windows is SignPath
Foundation" — present tense, while O4 establishes `SIGNPATH_ENROLLED` is false and every
shipped msi names no publisher. The README then instructs users to distrust exactly the
installer it actually ships.

`ReproducibilityScopeTest` as specified (§7.3: reads `docs/reproducibility.md` §1 and
`ci.yml` job names) catches neither. The issue's own thesis — a claim outrunning its
gate is the failure mode — is refuted by its own scope: the two claims that have
outrun reality today are outside it. **The unit of enforcement is a claim, not a
document.** Build a small in-tree claim registry (one machine-readable table: artifact,
scope token, gate job or `release.yml` flag, residual text, owning issue) and check
*every* surface that restates it — `README.md`, `docs/reproducibility.md` §1 and §5,
`SECURITY.md` §"Release artifact signing". A signing claim gated on
`secrets.SIGNPATH_API_TOKEN != ''` is a machine-readable fact just as much as a job
name is; the README sentence should be generated from the same registry, or fail.

## R2 — the CliFlagTableTest precedent is a *generation* precedent, not a parsing one

H3 places the new test "in the family of the existing `CliFlagTableTest` drift tests".
Read `test/jls/CliFlagTableTest.java`: it never parses prose. `JLSStart.commandLineFlags()`
is the single table, `usageText()` is *generated* from it, and the test asserts
generated == emitted. The prose cannot drift because there is no second copy of the
prose. Applying the actual precedent dissolves half of this issue: threat T3 (permissive
parser), the failure row for "table shape unparseable", the `theTableParsesAndHasRows()`
concrete-row-count guard, and falsification path H3 ("restructure the table into a
machine-readable block") are all artifacts of choosing to parse a hand-written Markdown
table. Make the registry the source, render §1 from it, and assert the checked-in
document equals the rendering. That is fewer moving parts, a better failure message
(a diff, not "offending line"), and it is the pattern a reviewer of this repo already
knows.

I am explicitly disregarding the acceptance criteria that require
`theTableParsesAndHasRows()` with a concrete row count and a robust §1 parser. They are
criteria for a design that should not be chosen.

## R3 — the dmg hypothesis (H2/P4) is refuted by reading `scripts/normalize-dmg.py`

H2 treats the koly `SegmentID` as a *pinned* field that can be gated in isolation. It is
not pinned to a constant — it is **content-derived**
(`scripts/normalize-dmg.py:83-100`):

```python
masked[seg : seg + SEGMENT_ID_LEN] = b"\x00" * SEGMENT_ID_LEN
pinned = hashlib.sha256(bytes(masked)).digest()[:SEGMENT_ID_LEN]
```

`SegmentID = sha256(D with SegmentID masked)`. Therefore
`κ(D₁) = κ(D₂) ⟺ D₁ = D₂` up to those 16 bytes — the proposed dmg gate *is* the
whole-file gate, wearing a different name. And `macos-installer-reproducibility` already
measures that the two dmgs differ (HFS+ dates/UUID stay unclamped because the full pass
corrupts the image). So P4 fails by construction, and it fails in precisely the way T1
warns against: a gate that cannot pass, reintroduced as rigor. The remaining two
deliverables of the dmg bullet — `hdiutil verify` and the double build — are already in
the job at `ci.yml:1064-1145`. There is no dmg work here; there is a `Bounded residual`
row to write.

## The reframing: cut at the payload/wrapper seam *before* the packager

`scripts/build-installer.sh:150-190` stages `$INPUT` (the reproducible jar) and
`$RUNTIME` (jlink), clamps both to `SOURCE_DATE_EPOCH`, and hands the same tree to
`jpackage --type deb|rpm|msi|dmg|app-image`. **Every installer on every OS is a wrapper
over one staged tree.** The issue instead proposes to reach *into* a finished msi with a
new CFB/cabinet extractor — described in §6 as "the one genuinely new mechanism" — to
recover a property that was on the disk, in plain files, one step earlier.

Two consequences, both better than the issue's plan:

1. **Payload determinism** is one cheap step, identical on all three OSes: canonical
   digest of the staged tree, computed twice around a re-run of the *staging phase
   only* (jlink is seconds; no jpackage, no WiX, no `hdiutil`). Windows and macOS get
   the same gate Linux gets, in a fraction of the runner minutes the proposed
   `installer-payload-reproducibility` double msi build would cost.
2. **Embedding fidelity** is the property the issue actually wants and never names:
   assert that what the packager embedded *equals the staged reference*, from **one**
   build — `cab(M) == staged tree`, `data.tar` == staged tree, mounted app bundle ==
   staged tree. This is strictly stronger than "the same twice" (it catches a packager
   that mangles the payload identically on both builds), it needs no second msi build,
   and it cannot be vacuous the way T2 fears, because the expected digest is a concrete
   value printed in the log rather than a comparison of two possibly-empty extractions.

Under this seam the container image stops being Open Question 1: its app layer is the
same staged tree and gates identically; the apt layers are wrapper and stay a written
bounded residual. One vocabulary — *payload gated, wrapper bounded* — covers deb, rpm,
AppImage, msi, dmg and the image, instead of five bespoke arguments. And it survives
runner drift (T4) far better: a jpackage or WiX change moves the wrapper, not the gate.

The honest cost: a staged-tree gate does not prove the msi's *cabinet* is stable
build-to-build. The fidelity check in (2) recovers that and more — and if only the
proposed cabinet extractor gives it, extraction belongs behind
`scripts/extract-msi-payload.sh` as a fidelity comparator against the reference digest,
not as half of a double-build lane.

## Alignment, and one thing worth saying plainly

The trust half of FEAT-010 is worth doing; a fork of an educational simulator that can
prove what it publishes is a genuinely distinctive thing to be. But the issue spends its
weight on the two artifacts with the *least* reachable property (msi, dmg) while the two
demonstrably false claims in the tree sit in the README, and it proposes a new binary
container reader for a project whose stated value is "students draw circuits and
simulate them". Ordered by user harm: (a) the README's signing sentence, (b) the
README/§1 contradiction, (c) the registry + generation, (d) staged-tree payload gate,
(e) fidelity checks, (f) everything else. Items (a)-(c) are a day and remove real
misinformation; the issue's headline deliverable is (d)/(e) in a more expensive form.

#134 is handled correctly here — a cost with an owner, per D10 — but its residual is not
only enrolment: until someone enrols, the README asserts a signature that does not
exist. That is an engineer's fix today, and it belongs in this task.

## Verdict

**endorse-with-reframing.** The research question stands; the mechanism should change.
Keep: the no-claim-without-a-gate invariant, the scope column, the `Payload-reproducible`
/ `Bounded residual` vocabulary, `timeout-minutes` on anything new, promote-nothing.
Replace: prose-parsing with registry-plus-generation (R2); the msi cabinet double-build
with staged-tree determinism plus one-build fidelity; the dmg koly gate with a written
bounded-residual row (R3). Add: README and `SECURITY.md` to the enforced claim surface,
and fix the two claims that are false today (R1).
