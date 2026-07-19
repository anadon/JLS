# Open-Issue Progress Pass — 2026-07-19

Automated assess → adversarial-review → update pass over all 42 open issues, run
against master HEAD `2eb3e0c` (post-#194 merge). Each issue was assessed by one
agent (issue thread + codebase evidence), independently challenged by an adversarial
reviewer that re-verified every claim against the tree and the thread, and only
review-approved actions were executed on GitHub.

**Outcome:** 22 issues received a verified status comment, 1 closed as completed (#79), 19 left untouched (thread already accurate), 0 failures.

| Issue | Reviewer verdict | Action | Result |
|---|---|---|---|
| #192 | approve | comment | [comment](https://github.com/anadon/JLS/issues/192#issuecomment-5016281638) |
| #191 | revise | comment | [comment](https://github.com/anadon/JLS/issues/191#issuecomment-5016279634) |
| #190 | approve | no-action | no change |
| #189 | revise | comment | [comment](https://github.com/anadon/JLS/issues/189#issuecomment-5016279576) |
| #188 | revise | comment | [comment](https://github.com/anadon/JLS/issues/188#issuecomment-5016280548) |
| #185 | approve | comment | [comment](https://github.com/anadon/JLS/issues/185#issuecomment-5016280509) |
| #184 | revise | comment | [comment](https://github.com/anadon/JLS/issues/184#issuecomment-5016281302) |
| #171 | approve | no-action | no change |
| #170 | revise | comment | [comment](https://github.com/anadon/JLS/issues/170#issuecomment-5016282256) |
| #169 | approve | no-action | no change |
| #168 | approve | no-action | no change |
| #167 | revise | comment | [comment](https://github.com/anadon/JLS/issues/167#issuecomment-5016282705) |
| #163 | revise | comment | [comment](https://github.com/anadon/JLS/issues/163#issuecomment-5016283022) |
| #162 | revise | comment | [comment](https://github.com/anadon/JLS/issues/162#issuecomment-5016283939) |
| #161 | approve | no-action | no change |
| #159 | revise | comment | [comment](https://github.com/anadon/JLS/issues/159#issuecomment-5016284081) |
| #153 | approve | no-action | no change |
| #136 | revise | comment | [comment](https://github.com/anadon/JLS/issues/136#issuecomment-5016284942) |
| #134 | revise | comment | [comment](https://github.com/anadon/JLS/issues/134#issuecomment-5016285130) |
| #101 | revise | comment | [comment](https://github.com/anadon/JLS/issues/101#issuecomment-5016286035) |
| #100 | approve | no-action | no change |
| #96 | approve | no-action | no change |
| #95 | revise | comment | [comment](https://github.com/anadon/JLS/issues/95#issuecomment-5016286156) |
| #94 | approve | no-action | no change |
| #93 | revise | comment | [comment](https://github.com/anadon/JLS/issues/93#issuecomment-5016286955) |
| #91 | approve | no-action | no change |
| #86 | approve | no-action | no change |
| #84 | revise | comment | [comment](https://github.com/anadon/JLS/issues/84#issuecomment-5016287844) |
| #82 | approve | no-action | no change |
| #79 | approve | close-completed | closed as completed — [comment](https://github.com/anadon/JLS/issues/79#issuecomment-5016287021) |
| #78 | approve | no-action | no change |
| #77 | approve | no-action | no change |
| #76 | approve | no-action | no change |
| #75 | approve | no-action | no change |
| #74 | approve | comment | [comment](https://github.com/anadon/JLS/issues/74#issuecomment-5016288153) |
| #73 | approve | comment | [comment](https://github.com/anadon/JLS/issues/73#issuecomment-5016288727) |
| #63 | approve | comment | [comment](https://github.com/anadon/JLS/issues/63#issuecomment-5016289001) |
| #62 | approve | comment | [comment](https://github.com/anadon/JLS/issues/62#issuecomment-5016289673) |
| #61 | approve | comment | [comment](https://github.com/anadon/JLS/issues/61#issuecomment-5016290067) |
| #59 | approve | no-action | no change |
| #56 | approve | no-action | no change |
| #33 | approve | no-action | no change |

## Per-issue findings

### #192

The issue's endgame items (private-member doc pass completion, @see decision, gate flip to private) remain open, and the two prior comments accurately describe the slice work (jls.sim, jls.edit clean). However, investigation at master HEAD 2eb3e0c uncovered material new facts the thread does not know: (1) the doclint gate is currently not binding at all — maven-javadoc-plugin 3.12.0's failOnWarnings detects warnings by matching the last stderr line against the regex `\d+ warnings?`, and javadoc 25 prints the summary with digit grouping ("1,422 warnings"), so any count >= 1,000 silently passes; (2) the generated test-tree @see tags produce 1,130 doclet "reference not found" warnings at the gate's default protected visibility (not only under -private as the pom scope note and issue premise claim), which is what pushed the count over the 1,000 cliff; (3) protected level is not clean — 292 …

**Action:** comment — https://github.com/anadon/JLS/issues/192#issuecomment-5016281638

### #191

The issue asks for a measured (double-build) determination of dmg non-determinism, feasible normalization, and either a CI gate or a documented bounded residual. At master 2eb3e0c the groundwork is fully landed: docs/dmg-reproducibility.md (variance inventory E1/F1-F7, Route A/B normalization plan), scripts/measure-dmg-repro.sh (turnkey macOS double-build attribution harness), #188's shared SOURCE_DATE_EPOCH derivation + staged-tree mtime clamp in scripts/build-installer.sh, and a report-only CI probe (.github/workflows/repro-installers.yml) with a macos-latest dmg double-build + diffoscope leg. Not done: no P1/P2 measurement has actually been run or recorded, no normalization implemented, no CI gate, and docs/reproducibility.md §5 still (correctly) lists installers as not reproducible. Material change since the newest comment (2026-07-18T22:07Z): that comment placed the work on branch …

Adversarial review corrections:
- Draft comment (and claim 3) asserted a 'git commit-time fallback' in the SOURCE_DATE_EPOCH derivation chain; scripts/build-installer.sh lines 100-104 actually hard-error (exit 1) when the pom's project.build.outputTimestamp is missing — the only git-commit-time fallback is the now-dead msi-lane default at line 436. Corrected in the final comment.
- Claim 8's evidence overstated: 'only JLS_SKIP_BUILD and JLS_SKIP_MSI_NORMALIZE exist as env switches' — APPIMAGETOOL, JLS_JBR_HOME, JLS_SIGN_KEY, and SOURCE_DATE_EPOCH are also env switches; the substantive point (no clamp opt-out) is correct. Internal claim only, not in posted text.
- The P1-baseline sentence implied the probe workflow could simply be run 'at a pre-#194 commit', but repro-installers.yml itself only landed in PR #194, so a dispatch cannot target a pre-#194 tree directly; a local Mac run of scripts/measure-dmg-repro.sh at an older commit or a throwaway branch is needed. Clarified.
- Cosmetic: claim 3 cites build-installer.sh lines 91-131 for the derivation; the code block actually spans ~96-131 (the section comment starts at 87).
- Everything else verified: merge topology (aebeaa7/b23123a inside PR #194 branch, not on pre-merge master 957ad48), empty git log 34d92bf..2eb3e0c over the five dmg-relevant paths, all cited workflow/script/doc line references and line counts, the stale status block, the absence of any recorded measurement, and that the thread's single comment never mentions the macOS CI leg — so the comment adds …

**Action:** comment — https://github.com/anadon/JLS/issues/191#issuecomment-5016279634

### #190

The issue asks for a measured determinism story for the jpackage/WiX msi: enumerate the volatile bytes, normalize what is normalizable, then either a Windows double-build CI gate (byte-identical) or an honestly documented bounded residual. The current tree (HEAD 2eb3e0c) already contains the Linux-side apparatus: /home/user/JLS/scripts/normalize-msi.py (in-place, same-length rewrites of package code GUID, SummaryInformation FILETIMEs, CFB directory timestamps, CAB member times; --self-test; leaves ProductCode/UpgradeCode alone), integration into the Windows lane of /home/user/JLS/scripts/build-installer.sh (lines ~419-440, self-test before touching the real msi, JLS_SKIP_MSI_NORMALIZE=1 escape hatch), the volatile-set documentation in /home/user/JLS/docs/windows-msi-determinism.md (explicitly making no reproducibility claim per #188 §10), and a report-only double-build probe workflow …

**Action:** no-action

### #189

The implementation described in the newest comment (2026-07-18T22:07Z) is fully merged to master at 2eb3e0c via PR #194 (commit 34d92bf is an ancestor of HEAD): scripts/build-installer.sh exports SOURCE_DATE_EPOCH (lines 96-117), clamps mtimes of input/, runtime/, and the AppDir (clamp_mtimes at lines 121-130, applied at 169, 242, 364), and builds the rpm under a staged .rpmmacros pinning %_buildhost/%use_source_date_epoch_as_buildtime/%clamp_mtime_to_source_date_epoch (lines 372-378). The CI gate exists as the installer-reproducibility job in .github/workflows/ci.yml (lines 342-388): double build, sha256 diff, diffoscope on mismatch, rpm installed so the rpm leg always runs. What changed since the newest comment: that comment named "gate green in CI (x86_64 and aarch64 legs)" as the remaining item and said the AppImage leg was never exercised (appimagetool download blocked in the …

Adversarial review corrections:
- Overclaim: the draft comment called the master runs "the first empirical confirmation of H1", but the gate job has no event condition and also ran green on the PR #194 branch runs 29664716829 (2026-07-18T23:12Z) and 29664959586 (23:20Z) before the master push run completed at 23:32Z; the master runs are therefore not literally the first green gate executions (all were still after the newest …
- Minor evidence imprecision in the internal claims (not in the comment text): clamp_mtimes is defined at build-installer.sh lines 129-131 (CLAMP_STAMP at 121-122), not "121-130"; harmless since the posted comment cites only line 242, which is correct.
- All other factual claims reproduced from primary evidence: ancestry of 34d92bf, script line numbers, rpm macros, ci.yml gate at 342-388 with rpm installed, both green master runs at 2eb3e0c (including verifying the scheduled run actually executed the gate job — it did, job 88165793540, unlike the other jobs which skip on schedule), repro-installers.yml zero runs with arm leg at line 44 and cron …

**Action:** comment — https://github.com/anadon/JLS/issues/189#issuecomment-5016279576

### #188

The issue's shared plumbing (§7 items 1-3) is fully merged to master (2eb3e0c via PR #194): scripts/build-installer.sh derives SOURCE_DATE_EPOCH from the pom's project.build.outputTimestamp and clamps all staged mtimes; .github/workflows/repro-installers.yml is the report-only 5-leg diffoscope probe; ci.yml has a hard Linux installer double-build gate. Since the newest comment (2026-07-18 22:07 UTC, written against branch tip 34d92bf): (1) PR #194 merged to master at 23:28 UTC, and (2) CI run 29676892246 on master (2026-07-19 06:43 UTC) ran the 'Linux installer reproducibility' job green — proving deb, rpm, AND AppImage byte-identical across two builds on ubuntu-latest, whereas the comment had only sandbox evidence for deb, an unverified rpm gate, and no AppImage measurement at all (appimagetool was proxy-blocked in its sandbox). This confirms H1 for all three Linux formats and P1 for …

Adversarial review corrections:
- Claim 1 misstates PR #194's tip: the merge commit 2eb3e0c's second parent is 9114898, not 34d92bf — three commits (1e7d26a CHANGELOG, e807068 CI/CodeQL fixes, 9114898 AEAD swap) landed on the branch after 34d92bf and before merge. The issue comment was written at tip 34d92bf, but 'tip 34d92bf merged' is wrong. (The draft comment text itself never cites 34d92bf, so this was latent, not public.)
- The draft comment's '#189 looks ready to close against its own criteria' overclaims: #189 §10 explicitly asserts reproducibility per arch on its own leg, and the aarch64 leg has never been measured — the hard gate runs only on ubuntu-latest x86_64 and the repro-installers.yml probe (which carries the ubuntu-24.04-arm leg) has zero runs. #189's P3 (contents unchanged) also has no recorded …
- Misquote: docs/reproducibility.md's §1 table cell reads '**No** — see §5', not '**No** — not reproducible'; the 'not reproducible' wording is §5 prose. Adjusted so quoted text matches the file.
- Line-citation drift: the staged .rpmmacros block is build-installer.sh lines 372-377 (macro at 376), not 370-376. Corrected.
- Verified (not problems, for the record): run 29676892246 job 88165793540 logs show identical sha256 for deb, rpm, AND AppImage across both builds on master 2eb3e0c; repro-installers.yml has total_count 0 runs; sub_issues_summary is 0/3; the comment adds genuinely new information over the 2026-07-18T22:07Z comment (which had only sandbox deb evidence, an unverified rpm gate, and no AppImage …

**Action:** comment — https://github.com/anadon/JLS/issues/188#issuecomment-5016280548

### #185

The work described in the issue landed on master: PR #194 merged branch claude/advance-github-issues-hjrpyq, making HEAD 2eb3e0c. The tree now contains (a) the upgraded reproducibility gate in .github/workflows/ci.yml (job "reproducibility", lines ~267-341): same-runner double-build pre-filter plus an independent perturbed rebuild (different workspace path, TZ=Pacific/Kiritimati, LC_ALL=C, umask 077) that requires jar AND bom.json to match byte-for-byte and uploads diverged jars for diffoscope; (b) release.yml runs maven-artifact-plugin:3.6.0:buildinfo in the same Maven session, adds the .buildinfo to SHA256SUMS, and uploads it as a release asset; (c) docs/reproducibility.md declares the reproducible set (jar, BOM), the per-release toolchain-record policy (exact JDK/Maven pinned in each release's .buildinfo), third-party verification recipes, and the #184/#189 boundary for …

**Action:** comment — https://github.com/anadon/JLS/issues/185#issuecomment-5016280509

### #184

All three findings are implemented in the current tree at 2eb3e0c: (C) the CI reproducibility job builds and diffs bom.json alongside the jar, including a same-runner rebuild and an independent perturbed rebuild (.github/workflows/ci.yml lines 283-342); (B) resources/packaging/Dockerfile pins apt installs to snapshot.ubuntu.com at APT_SNAPSHOT=20260716T000000Z (lines 33-63) and scripts/build-container.sh exports SOURCE_DATE_EPOCH as a build-arg plus rewrite-timestamp=true on the buildx push path (lines 46-51, 82); (A) scripts/build-installer.sh derives and exports SOURCE_DATE_EPOCH from the pom's outputTimestamp with caller override (lines 96-117), and README.md frames installer integrity as attestation-backed, not byte-reproducible (lines 51-60). Since the newest comment (2026-07-18T22:07:54Z, which said the issue stays open pending P2 container validation and CI-green confirmation), …

Adversarial review corrections:
- Refuted: the claim that both reproducibility jobs succeeded twice. On scheduled run 29676892246 the 'Reproducible build check' job (88165793798) was SKIPPED, not successful — ci.yml line 285 gates it with `if: github.event_name != 'schedule'`. Only 'Linux installer reproducibility' ran and passed on the scheduled run; the jar/BOM job passed once, on push run 29665293624. The draft comment's …
- Overstatement: 'validation lands with the next release's container-image job' — the container-image job also builds (without pushing) on workflow_dispatch dry runs of release.yml, and a single release build exercises the pin but does not itself demonstrate P2, which as written requires comparing layer digests across two builds.
- Trivial imprecision: the installer-reproducibility job spans ci.yml lines 349-388, not 349-386 (evidence citation only; not in the comment text).
- All other claims verified against the repo tree at 2eb3e0c, the full issue thread (1 comment), GitHub Actions runs/jobs, release history, and commit 7edd009.

**Action:** comment — https://github.com/anadon/JLS/issues/184#issuecomment-5016281302

### #171

The issue asks for full Stage 2 simultaneous editing: per-kind CRDT merge rules, anti-entropy resync, log compaction/snapshots, OpSink gossip integration (token retirement), collaborative undo with an ARCHITECTURE.md decision entry, and the P1-P4 convergence suite against the #166 oracle. The current tree (master, 2eb3e0c) contains only the replication substrate groundwork: src/jls/collab/crdt/ with VectorClock, OpId, OpEnvelope, CausalBuffer plus 22 tests in test/jls/collab/crdt/ — exactly what the single existing comment (2026-07-18, the newest) already reports. Since that comment, the only changes were: PR #194 (the branch the comment cites) merging into master, a CI test-table fix (e807068), and an AEAD swap from ChaCha20-Poly1305 to AES-256-GCM in src/jls/collab/net/ (9114898) — the latter belongs to the #168/#170 transport/hardening scope, not this issue's crdt work; …

**Action:** no-action

### #170

The single existing comment (2026-07-18 22:08Z) describes #170's H1/P1/P3/P4 groundwork as "integrated on branch claude/advance-github-issues-hjrpyq, tip 34d92bf". That branch has since merged to master via PR #194 (merge commit 2eb3e0c, 2026-07-18 23:28Z — after the comment), so everything the comment describes is now on master: ElementVocabulary allowlist + TestGen witness test, CollabSecurityRatchetTest/SocketConfinementRatchetTest/ArchitectureRulesTest ratchets, docs/collab-vocabulary.md, SECURITY.md threat entries, and ElementBlocks routing through requireAllowed + the #78 registry. Three post-comment commits landed on the PR (CHANGELOG, CodeQL/CI fixes, ChaCha20→AES-256-GCM AEAD swap) — all #168 transport scope, none touching #170 surface. Additionally, several H2 bounds already exist on master (SecureLink 1 MiB frame cap, CausalBuffer 10,000-op backlog cap, …

Adversarial review corrections:
- Draft comment claims all three post-comment commits (1e7d26a, e807068, 9114898) 'are #168 transport scope' — only 9114898 (AEAD swap) is; e807068 touches src/jls/hdl/scan/* and test/jls/ui/MenuBarSpecTest.java, and 1e7d26a is CHANGELOG-only. The substantive claim (none touch #170 surface) is correct, but the scoping label is wrong.
- Draft comment lists 'boundary-value tests for the cap set' as still open — refuted: SecureLinkTest exercises at-cap and over-cap frames (MAX_PAYLOAD_BYTES and MAX_PAYLOAD_BYTES+1, lines 70/177/180), CausalBufferTest.pendingOverflowIsRefused fills the buffer to exactly 10,000 and asserts refusal of the next envelope, and CircuitOpTest pins the MAX_IDS '>=' boundary (~line 415). Posting this …
- Minor evidence drift in the internal claims (not in posted text): SecureLink cap enforcement is at lines 150 and 194-197, not 190-197; the misbehavior grep under src/jls/collab also hits a CausalBuffer javadoc reference to a future misbehavior policy, not solely VectorClock 'counter' semantics. Claim substance (no implementation exists) still holds.

**Action:** comment — https://github.com/anadon/JLS/issues/170#issuecomment-5016282256

### #169

The issue asks for a full shared-session v1: replicated roster lifecycle, heartbeat reachability, snapshot broadcast via markChanged, token-gated write path, presence overlays, peer panel, any-member forwarding, chaos-tested loopback schedules, and a manual three-machine session. The tree at HEAD 2eb3e0c has the transport-free session-state core: /home/user/JLS/src/jls/collab/session/ (Roster, SessionEntry, EntryKind with ADMIT/LEAVE/EJECT/TOKEN_GRANT/TOKEN_CLAIM, PeerId, ReachabilityTracker) with tests RosterTest, RosterConvergenceTest (1000 seeded schedules, SCHEDULES=1000), and ReachabilityTrackerTest under /home/user/JLS/test/jls/collab/session/. Not yet implemented: snapshot broadcast wiring in SimpleEditor.markChanged (only comment references to the future collab layer), token gating in OpSink, presence, peer panel, transport binding, and the manual session. Since the newest …

**Action:** no-action

### #168

Stage 1a's headless core is on master at 2eb3e0c (merge of PR #194): jls.collab.net contains IdentityKey, Handshake, Crypto (AES-256-GCM per the correction comment), Sas (7 glyphs from a 64-word table), SecureLink, KnownPeers, plus HandshakeRejected/FrameRejected; tests exist for each (test/jls/collab/net/), the socket-confinement ratchet (test/jls/SocketConfinementRatchetTest.java) is in place, and SECURITY.md has the "Collaboration transport (issue #168, Stage 1a)" section. Still missing, exactly as the newest comment's remaining-work list states: no socket/listener code (grep for Socket under src/jls/collab hits only package-info.java prose), no jls.collab.ui join/verify/key-change dialogs, no bundled glyph images, no two-machine LAN record, no §13 second-reader handshake review. The only change since the newest comment (2026-07-18 23:17 UTC) is the merge of that same branch to …

**Action:** no-action

### #167

The issue asks for a closed CircuitOp vocabulary behind one OpSink entry point, migrated gesture-by-gesture. Master (2eb3e0c) now has: the sealed CircuitOp/OpSink/OpRejected/CircuitOpReader core plus 8 op kinds (ToggleWatched, AttachProbe/RemoveProbe, RotateElement, FlipElement, MoveElements, AddElements, RemoveElements) with ElementBlocks/ElementVocabulary transplant machinery in src/jls/collab/op/; six gestures migrated through submitOp in SimpleEditor; 21 tests in test/jls/collab/op/CircuitOpTest.java covering P1 parity, P2 inverse-restores-bytes, P3 round-trips and hostile rejections; and the in-tree inventory docs/operation-layer.md. Still open: wiring vocabulary (AddWire/RemoveWire), preview-then-commit migration of move/placement/paste/delete gestures (their ops exist but gestures mutate inline), SetAttributes for dialog commits, EditOrderedRows, ImportSubcircuit. Material change …

Adversarial review corrections:
- Draft claims 'two commits between branch tip 34d92bf and the merge'; git log --graph 34d92bf..2eb3e0c shows three branch-side commits: 1e7d26a (CHANGELOG record), e807068 (CI/CodeQL fixes), 9114898 (AEAD swap). The empty diff over the op-layer paths still covers all three, so the conclusion survives but the count had to be corrected.
- Draft comment presents 'EditOrderedRows and ImportSubcircuit' as item (4) of docs/operation-layer.md's 'What lands next' order; the doc's actual item 4 is the deferral of precise (op-inverse) undo to Stage 2, and EditOrderedRows/ImportSubcircuit appear only as deferred rows in the mutation-site inventory table. Corrected the attribution.
- Minor: the claim '15 source files ... (+ package-info.java)' describes 16 files in src/jls/collab/op/; the enumeration itself is complete so this is wording only, and the test package also contains ElementVocabularyTest.java beyond the cited CircuitOpTest.java (the comment's '21 tests in CircuitOpTest' is accurate as written). Neither required a change to the comment text.

**Action:** comment — https://github.com/anadon/JLS/issues/167#issuecomment-5016282705

### #163

The newest comment (2026-07-18 22:08) described the multi-agent session's work as sitting on branch claude/advance-github-issues-hjrpyq at tip 34d92bf, pending merge. Since then, PR #194 merged that branch to master (HEAD 2eb3e0c), carrying two post-comment commits: e807068 (CI + CodeQL fixes) and 9114898 (AEAD cipher changed from ChaCha20-Poly1305 to AES-256-GCM by owner decision, frame format unchanged). Master now contains all four collab packages (jls.collab.net/session/op/crdt, 36 source files), their test suites, the ArchUnit layering rules, docs/operation-layer.md, and SECURITY.md collab entries. Sub-issues #165/#166 are closed-completed; #167–#171 remain open with substantial work (full op vocabulary, transport wiring, session UI, CRDT replication). The tracking issue is nowhere near complete, so no close; but the thread's newest comment is now stale on merge status and records …

Adversarial review corrections:
- Draft says 'two post-comment commits' but three commits post-date the 22:08 UTC comment: the changelog commit 1e7d26a (22:13 UTC) also landed after it, alongside e807068 and 9114898. Corrected the comment wording to 'two substantive changes' so it no longer implies only two commits landed.
- The internal notes claim the newest comment 'records the wrong AEAD cipher' — overstated: that comment says only 'AEAD framing' and never names ChaCha20-Poly1305; the pre-swap cipher was recorded in repo docs at 34d92bf, not in the thread. The posted text itself makes no such misattribution, so no substantive change was needed there.
- All other claims reproduced from evidence: merge of PR #194 at 2eb3e0c (23:28 UTC > comment's 22:08 UTC); commit 9114898's message confirms the CodeQL-allowlist rationale, owner decision, and unchanged 32-byte key / 12-byte counter-nonce / 16-byte tag frame shape; Crypto.java lines 23/85/120 and SECURITY.md reference AES-256-GCM with no residual ChaCha mentions; 36 .java files across the four …

**Action:** comment — https://github.com/anadon/JLS/issues/163#issuecomment-5016283022

### #162

Every item on the issue's §7 method checklist is now on master (HEAD 2eb3e0c = merge of PR #194): xvfb substrate decision wired in pom.xml (display-tests surefire execution gated on jls.test.headless, main execution stays headless and excludes the display group) and ci.yml (best-effort xvfb install, xvfb-run wrapper); package-info.java layer-2 edit recording the survey rejections; DialogConstructionSmokeTest (24 dialog families) plus DialogCoverageRatchetTest (bytecode completeness ratchet over all ElementDialog subclasses with two documented exemptions); EdtViolationDetector/EdtViolationDetectorTest (P2); InteractiveSimulatorSmokeTest (P3, @Tag("display")); RenderAssert/RenderBoundsTest (layer-3 starter). Material changes since the newest comment (2026-07-18 22:12Z, which described branch tip 34d92bf): PR #194 merged to master at 23:28Z, including post-comment commit e807068 that fixed …

Adversarial review corrections:
- Opening sentence implies all thread-described work was branch-only until PR #194 merged; in fact the substrate/sweep/ratchet work from the first two comments had already merged to master via PRs #176 (2026-07-17) and #193 (2026-07-18 15:17Z) — only the newest comment's batch (34d92bf plus fix e807068) arrived via #194.
- Claim 'every §7 checklist item is on master' overstates: the first §7 checkbox explicitly includes 'record stability over repeated runs (H2's criterion)', which is not done — only the decision/documentation half of that checkbox is met, as the draft's own remaining list concedes.
- All other factual claims reproduced from the repo tree and issue thread: merge timing (2eb3e0c at 23:28Z vs newest comment 22:12Z), e807068 file list, ratchet structure (24 swept, 3 represented, 2 exemptions), 23 ElementDialog source files, pom.xml/ci.yml wiring, package-info contents, display tag placement — no refutations.

**Action:** comment — https://github.com/anadon/JLS/issues/162#issuecomment-5016283939

### #161

The issue gates PIT adoption on 50% line coverage; the gate was crossed 2026-07-17 (first comment) and the scoped trial was completed 2026-07-18 (second/newest comment). Everything that comment describes is present on master at 2eb3e0c via the PR #194 merge: the trial record docs/mutation-testing-trial-2026-07.md (with the ready-to-paste pitest pom profile), the Adopt verdict in the PIT row of docs/library-survey-2026-07.md (line 266), and the 13 new killing tests (test/jls/sim/SimEventContractTest.java, test/jls/BitSetUtilsSumCarryTest.java, additions to BitSetUtilsCreateTest.java and test/jls/collab/op/CircuitOpTest.java). Exactly as the comment states as "remaining for closure", pom.xml still has no pitest profile and no junit-platform-launcher test dependency (0 matches; only a comment at line ~306 referencing the gate), and no .github/workflows file mentions pitest/mutation. …

**Action:** no-action

### #159

All structural items of §7 are complete on master at HEAD 2eb3e0c: the bundle rule has a BRANCH limit (pom.xml minimum 0.365 alongside INSTRUCTION 0.420 / LINE 0.400), per-package PACKAGE-element floors exist for jls (0.510/0.495/0.550), jls.sim (0.435/0.430/0.340), jls.elem (0.410/0.400/0.350), and jls.collab.op (0.880/0.890/0.700) with dot-form includes, and the climb convention is contributor-visible in CONTRIBUTING.md ("Coverage ratchet" section). What changed since the newest comment (2026-07-18 22:09 UTC): PR #194 merged to master (2eb3e0c, 23:28 UTC same day), carrying the described increment (TruthTable model extraction + TruthTableModelTest, CONTRIBUTING section) plus two post-comment commits (e807068 touching jls.hdl.scan and a UI test; 9114898 swapping the collab AEAD in jls.collab.net) — but the floor raise the comment prescribed for merge time was NOT applied: bundle floors …

Adversarial review corrections:
- Slack figure misstated: draft comment said floors sit 'about 2.5 points' below the last measurement, but actual slack at HEAD is 3.2/3.2/3.7 points for the bundle (45.20/43.24/40.18 measured vs 0.420/0.400/0.365 floors) and 4.1 points for jls.elem line (44.1 vs 0.400); the 2.5 figure conflated the suggested-raise delta with the measured slack — corrected to '3-4 points'.
- Minor unflagged ambiguity: the pom ratchet comment ties the 50% PIT gate to the display-substrate number, while CONTRIBUTING.md and the two newest thread comments pin it to the headless figure; the draft follows the headless convention, which is the governing (newest, contributor-visible) one, so no change made.
- All other claims reproduced from evidence: merge 2eb3e0c (PR #194, 23:28 UTC, after the 22:09 UTC comment, 34d92bf ancestor), pom floor values and line numbers, dot-form PACKAGE includes, jls.edit unfloored, CONTRIBUTING.md line 59 section content, TruthTableModelTest.java existence, e807068/9114898 file stats not touching floored packages or pom.xml, and comment 5013112790's numbers …

**Action:** comment — https://github.com/anadon/JLS/issues/159#issuecomment-5016284081

### #153

The evaluation the issue asks for is complete and merged to master (HEAD 2eb3e0c via PR #194, which merged branch claude/advance-github-issues-hjrpyq tip 34d92bf — the exact branch the newest comment cites). docs/flatlaf-evaluation-2026-07.md contains the full measured evaluation (jar size, zero transitive deps, JDK 25 headless install + runtime light/dark switching, comparables rejected, hardcoded-color audit) with verdict "recommend ADOPT, gated on the cross-OS screenshot matrix"; the -Djls.laf=metal|system|<class> seam is in src/jls/JLSStart.java (default still Metal, broken selection warns and falls back instead of exiting) and is pinned by test/jls/LookAndFeelPolicyTest.java. FlatLaf is NOT yet a dependency (no flatlaf entry in pom.xml). Remaining work is exactly what the comment listed: cross-OS visual QA (needs real Windows/macOS/Linux displays), the adopt decision, and the …

**Action:** no-action

### #136

The issue asks for a key-custody decision, then GPG signing of the rpm (rpmsign) and AppImage (appimagetool --sign). All code scaffolding is now on master: PR #194 (merge commit 2eb3e0c, 2026-07-18 23:28 UTC) merged branch claude/advance-github-issues-hjrpyq, adding the release.yml "Import release signing key" and "Verify release signatures" steps, JLS_SIGN_KEY-gated rpmsign and appimagetool --sign in scripts/build-installer.sh, and README/CHANGELOG documentation. Everything is inert until the RELEASE_GPG_KEY/RELEASE_GPG_PASSPHRASE secrets and resources/packaging/RELEASE-KEY.asc exist — the key has not been generated (no RELEASE-KEY.asc in the tree; README still shows FINGERPRINT-PENDING). The newest existing comment (2026-07-18 22:09 UTC) predates the merge and described the work as sitting on the unmerged branch, so "the diff is now on master" is materially new. The blocking item is …

Adversarial review corrections:
- Draft comment asserted 'the RELEASE_GPG_KEY/RELEASE_GPG_PASSPHRASE secrets are unset' as verified fact; repository secrets cannot be read via the API and this was not (and cannot be) reproduced from evidence. Reworded to rest on what is verifiable: no RELEASE-KEY.asc committed and the README fingerprint still FINGERPRINT-PENDING, i.e. no key has been generated.
- Partial duplication with the newest existing comment (2026-07-18 22:09 UTC), which already lists the same release.yml/build-installer.sh/README content and the same blocking decision; retained the comment because the merged-to-master fact (PR #194, merge 2eb3e0c, ~80 minutes after that comment) materially corrects the thread's stale 'integrated on branch, tip 34d92bf' state, but tightened the …
- Minor attribution fix: the 'ordered before attestation/checksums' property holds because signing runs inside the Build-installer step and the attestation step (release.yml line 524) runs after — moved that clause so it is not attributed to build-installer.sh alone.

**Action:** comment — https://github.com/anadon/JLS/issues/136#issuecomment-5016284942

### #134

The CI side of the issue is fully implemented on master at HEAD 2eb3e0c: .github/workflows/release.yml contains secret-gated SignPath signing steps on both Windows legs (upload unsigned msi with retention-days:1, Sign msi via SignPath action SHA-pinned at v2.2, signed re-upload), placed before attestation/checksums (H2 ordering), a force-sign workflow_dispatch input, an actions:read job permission, and a strict two-msi verify-windows-signatures job using osslsigncode; README.md:29-37 replaces the "Run anyway" SmartScreen guidance with the SignPath Foundation publisher expectation. All steps skip cleanly until SIGNPATH_API_TOKEN/SIGNPATH_ORGANIZATION_ID secrets exist. What changed since the newest comment (2026-07-18T22:09:33Z, which located the work on branch claude/advance-github-issues-hjrpyq tip 34d92bf): PR #194 merged that branch to master at 2026-07-18T23:28:35Z (merge commit …

Adversarial review corrections:
- Factual error in the assessment's evidence: it asserts the latest release is v5.0.3, but the GitHub releases API shows v5.0.4 (published 2026-07-16T15:16:17Z) is the latest. v5.0.4 still predates the signing merge, so the substantive conclusion (no signed release exists, signing never executed) survives, but the stated fact is wrong and must not propagate into a posted comment.
- Substantial duplication: about 80% of the draft comment restates implementation details already present in the newest issue comment (2026-07-18T22:09:33Z) — signing-step placement before attestation/checksums, force-sign input, retention-days: 1, actions: read grant, verify-windows-signatures job, README SignPath Foundation wording, skip-until-enrolled behavior, and a near-verbatim copy of that …
- Minor line-number imprecision: 'Upload unsigned msi for signing (line ~483)' — the step name is at line 481 (483 is its if: guard); immaterial given the tilde, and the corrected comment cites no fragile line numbers.
- All other claims verified against the repo tree, git history, PR #194 API data, and the full issue thread: merge timestamp 23:28:35Z postdates newest comment 22:09:33Z; c149533 is an ancestor of 2eb3e0c; CHANGELOG line 32 records the scaffolding; close-completed is correctly ruled out (issue §13 requires recorded P1/P2 on a signed release — secrets absent, no release tagged since the merge).

**Action:** comment — https://github.com/anadon/JLS/issues/134#issuecomment-5016285130

### #101

The issue asks for a Wayland GUI CI rig (headless sway + JBR WLToolkit, screenshot, P1/P2 assertions, first-light report). Master at 2eb3e0c already contains the full rig: scripts/wayland-rig.sh (headless sway, HelloSwingControl control step with exit-code failure classification per section 9, window-presence assertion via swaymsg/jq, grim screenshots, pixel-diff with PIXEL_DIFF_MIN gate), scripts/HelloSwingControl.java, a gui-wayland job in .github/workflows/ci.yml (lines 155-245) with SHA-pinned actions, artifact upload, and a nightly cron (17 4 * * *) that runs only this lane, plus README documentation. What remains: the JBR_SHA256 pin in ci.yml:187 is still the fail-closed UNVERIFIED placeholder, so the lane skips at the download step and no first-light run/report exists yet; P3's 20-run record has not started. Material change since the newest comment (2026-07-18 22:09 UTC): that …

Adversarial review corrections:
- Refuted: the claim that the nightly cron 'runs only gui-wayland'. The installer-reproducibility job (.github/workflows/ci.yml line 349) has no `if: github.event_name != 'schedule'` gate and no `needs`, so it also runs on schedule events — contradicting the draft (claim 4 cites gates at lines 30/116/140/285 but misses the job at 349) and ci.yml's own header comment ('every other job skips on …
- Stale framing: the draft comment predicts 'the very next push or nightly run' as future, but the first nightly had already fired before this review (run 29676892246, schedule event on 2eb3e0c, 2026-07-19 06:43 UTC) — gui-wayland ran, the download step emitted the placeholder ::notice and skipped, exactly as designed. The comment should cite this observed run rather than predict it.
- Rendering bug in draft text: `&lt;JBR_URL&gt;` HTML entities inside a backtick code span are not decoded by GitHub Markdown and would display literally; must be real angle brackets.
- Minor evidence imprecision (not in posted text): git log --follow shows scripts/wayland-rig.sh last modified in 90489ce (created in 83cd4d6, also touched by 7a06c2b), not 'last change in 83cd4d6'; README's Wayland section starts at line 162, not 165.
- All other claims verified: merge 2eb3e0c at 2026-07-18 23:28:35 UTC postdates newest comment (22:09:36 UTC); rig files present (301/57 lines); ci.yml lines 12-13, 24, 166, 169, 187, 193, 214-218, 237, 239-245 all exact; e807068/9114898 touch no rig files; no ci.yml/scripts changes in 34d92bf..2eb3e0c; master is the default branch; JBR CDN 403 reproduced live on 2026-07-19.

**Action:** comment — https://github.com/anadon/JLS/issues/101#issuecomment-5016286035

### #100

Issue #100 is the Wayland-native GUI tracking issue. Sub-issues #102-#105 are closed (4/5 complete per sub_issues_summary); only #101 (CI rig first light) remains. At HEAD 2eb3e0c the code side is done exactly as the newest comment (2026-07-18) states: the three banned APIs (MouseInfo.getPointerInfo, getLocationOnScreen, getScreenSize) are at 0 in src/ and ratcheted; toolkit auto-selection, Placement, KeyPad rebuild, census doc, README supported-desktop matrix, and docs/wayland-desktop-checklist.md all exist. The sole blocker remains the JBR_SHA256 placeholder in .github/workflows/ci.yml (line 187), which requires a maintainer to compute the checksum from an open network. The only commits since the tip that comment described (34d92bf → 2eb3e0c: e807068, 9114898) touch collab crypto (AES-256-GCM), HDL scanners, and a menu test — nothing relevant to this issue. The JBR URL is still …

**Action:** no-action

### #96

Tracking issue for the Java-25/Kotlin-practices program. Sub-issue #92 (Java 25 baseline) is closed and verified in-tree; #93/#94/#95 are open and in progress. The newest comment (2026-07-18 22:09Z) accurately describes the in-progress state: NullAway+JSpecify on the default build with jls.sim and jls.util @NullMarked, SimEvent immutability pinned by SimEventDedupTest, Trace.Change as a record, sealed Element hierarchy pinned by SealedHierarchyTest. The only change since that comment is that the branch it references (claude/advance-github-issues-hjrpyq) merged to master as PR #194 (merge commit 2eb3e0c, ~80 minutes after the comment) with two follow-up commits (CI/CodeQL fixes and an unrelated collab AEAD swap). No tracker-level status change: still 1 of 4 sub-issues complete, program correctly in "watch sub-issues to completion" mode.

**Action:** no-action

### #95

The core sealing work is done and now on master. The newest issue comment (2026-07-18 22:09 UTC) reported the sealing integrated on branch claude/advance-github-issues-hjrpyq at tip 34d92bf; since then, PR #194 merged that branch into master (merge commit 2eb3e0c, 2026-07-18 23:28 UTC), so the work is no longer branch-only. Verified on master: Element/LogicElement/Gate/Group/Pin/SigSim/DisplayElement and separate root Put are sealed with explicit permits per the adjudicated 2026-07-17 tree; SealedHierarchyTest pins it; HdlExporter.gateOp is an exhaustive no-default pattern switch over sealed Gate. Also landed in the same merge: #78's Orientation unification (single JLSInfo.Orientation, Gate.Orientation deleted), which unblocks the "Orientation sweep after #78" item the comment listed as gated. Still outstanding on master: SimEvent.todo remains an untyped Object (no sealed …

Adversarial review corrections:
- Draft comment claimed 'the two commits added after tip 34d92bf inside that merge (e807068, 9114898) touched collab crypto and the HDL scanners only' — refuted: git log shows three post-tip branch commits (1e7d26a, e807068, 9114898), and git diff 34d92bf..2eb3e0c also touches CHANGELOG.md and SECURITY.md in addition to the collab crypto files, HDL scanners, package-info, and MenuBarSpecTest. The …
- Minor (assessment notes only, not the posted text): the 'Still outstanding' list partially duplicates the prior comment's own 'Remaining:' line; retained because it is now re-verified against master rather than the branch, and the master-merge + #78-unblock facts are genuinely new to the thread.
- All other claims reproduced from evidence: sealed permits tree on master (Element.java:21-22, LogicElement, Gate, Group, Pin, SigSim, DisplayElement, Put.java:14), SealedHierarchyTest reflection pin, HdlExporter.gateOp:683 exhaustive no-default switch, single JLSInfo.Orientation with Gate.java:4 import, SimEvent.java:25 untyped Object todo, instanceof counts (SimpleEditor 88 / Circuit 17 / …

**Action:** comment — https://github.com/anadon/JLS/issues/95#issuecomment-5016286156

### #94

Increment 1 described in the newest comment (2026-07-18 22:10) is fully present in master at 2eb3e0c: the branch claude/advance-github-issues-hjrpyq it referenced was merged via PR #194, which is exactly the current HEAD. SimEvent remains a plain final class with final fields and an explanatory comment (src/jls/sim/SimEvent.java); the dedup regression test exists (test/jls/sim/SimEventDedupTest.java); Trace.Change, SimpleEditor.WireStart, ToolkitPolicy.Decision are records; TraceSample record exists (src/jls/sim/TraceSample.java); CONTRIBUTING.md carries the value-semantics convention. The remaining candidates the comment lists (LoadError, HdlModel.Port/Net/Operand, HdlExporter.Result, Help.TocEntry) are all still plain final classes — i.e. still pending, exactly as stated. The only commits after the comment (e807068 CI/CodeQL fixes, 9114898 collab AEAD swap, merge 2eb3e0c) touch collab …

**Action:** no-action

### #93

The issue's first-tranche work is fully landed on master. The newest comment (2026-07-18 22:10 UTC) described the work as "integrated on branch claude/advance-github-issues-hjrpyq, tip 34d92bf" — i.e. on a branch, pending merge. Since then, that branch merged to master via PR #194 (merge commit 2eb3e0c, 2026-07-18 23:28 UTC), so everything the comment claims is now on the default branch: Error Prone 2.50.0 + NullAway 0.13.7 on the default -Werror compile with OnlyNullMarked+JSpecifyMode, JSpecify 1.0.0 dependency, @NullMarked on jls.sim and jls.util, NullMarkedRatchetTest + SimulatorNullContractTest, and the CONTRIBUTING ratchet convention. The two post-comment commits inside the PR (e807068 CI/CodeQL fix, 9114898 collab AEAD swap) do not touch the nullness setup. Remaining scope is real: src/jls/package-info.java is not @NullMarked, JLSInfo.java has zero @Nullable annotations, and …

Adversarial review corrections:
- Draft comment substantially re-enumerates the previous comment's content (versions, compiler flags, package list, test names, CONTRIBUTING) when the only genuinely new fact is the merge to master; trimmed to avoid near-duplication of the thread.
- Minor citation drift: CONTRIBUTING.md ratchet section spans roughly lines 43-51, not 44-50 as the evidence stated (immaterial; the section exists as described).
- All other factual claims reproduced exactly: timestamps (comment 22:10:22Z vs merge 23:28:35Z), 34d92bf ancestry of 2eb3e0c, pom.xml line 163 NullAway args, EP 2.50.0 / NullAway 0.13.7 / jspecify 1.0.0, @NullMarked exactly on jls.sim and jls.util, both tests present, ratchet MARKED list, jls root package-info unmarked, JLSInfo with zero @Nullable, and the two post-comment commits (e807068, …

**Action:** comment — https://github.com/anadon/JLS/issues/93#issuecomment-5016286955

### #91

The issue's layered UI-test harness is substantially built and the thread tracks it accurately. At HEAD 2eb3e0c (merge of PR #194, tip 34d92bf): Layer 1 model assertions (P1) exist in test/jls/ui/CircuitAssert.java, GeometryAssert.java, UiHarnessPilotTest.java; Layer 2 synthetic-event gesture tests exist in EditorGestureTest.java + EditorGestureSupport.java (landed b80cd6a); P3 menu-bar expectation-table test exists as MenuBarSpecTest.java; Layer-3 starter exists as RenderAssert.java + RenderBoundsTest.java. Still outstanding, exactly as the newest comment states: P2 (Robot-driven palette click + canvas drop — no Robot test exists; EditorGestureSupport.java:32 explicitly defers palette placement), P4 (blocked on #86's context menus), P5's 20-consecutive-run promotion of the display suite to a required CI gate (.github/workflows/ci.yml lines 5, 113, 165 document the bar as pending; …

**Action:** no-action

### #86

Of the issue's three verified defects, two are fixed and test-pinned on master HEAD 2eb3e0c: (H1/P1-P3) the KeyPad has Esc-on-root-pane dismissal, focus-loss/click-outside dismissal via WindowFocusListener, and no BUTTON3 listener remains (src/jls/KeyPad.java:124-131; pinned by test/jls/ui/KeyPadDismissalTest.java); (H2/P4) the parent editor shows a prominent bold warning-yellow banner naming parent and subcircuit while disabled, cleared on re-enable (src/jls/edit/SimpleEditor.java:142,417-425,654-676,4590; src/jls/edit/Editor.java:371-375; pinned by test/jls/ui/SubcircuitDisabledBannerTest.java). (H3/P5) Action-layer context menus remain unimplemented — only the legacy optionMenu/newMenu popups exist (SimpleEditor.java:820,845), and the stated dependency #75 is still open. Nothing material changed since the newest comment (2026-07-18 22:10 UTC): it described exactly this state on …

**Action:** no-action

### #84

Issue asks for staged decomposition of SimpleEditor (Action layer, State-machine extraction, UndoManager, palette via #78 registry, #86 context menus) with a <~1,500-line target. Current tree (master 2eb3e0c): UndoManager step is DONE and merged — /home/user/JLS/src/jls/edit/UndoManager.java (239 lines, IntSupplier depth degrading to JLSInfo.undoStackDepth as the #76 seam), CircuitSnapshot.java, and /home/user/JLS/test/jls/edit/UndoManagerTest.java (8 @Test methods); SimpleEditor delegates via a single undoManager field. Viewport.java (#74) and DeleteKeyPolicy.java are also extracted. Still remaining: the 9-state enum State is still inside SimpleEditor (:704) with setState on EditWindow; the source== popup dispatcher (the #37 failure site) is intact at :2170-2427 with 19 getSource()== branches; makeElements() still hand-enumerates the toolbar palette (:1543); SimpleEditor is now 5,155 …

Adversarial review corrections:
- Key-binding AbstractAction range misstated: draft comment says :1011-1331, but the Action definitions extend through flipKey at :1477 (approx :1011-1490); corrected.
- Claim that the Action-layer step 'reduces to converting this one popup dispatcher' oversimplifies: section 7 requires shared Action objects joint with #75, and the existing key-binding AbstractActions were not verified to be shareable with menu/popup paths; reworded to the verified fact that all 19 remaining getSource()== sites are in the popup dispatcher.
- Growth attribution incomplete: draft credits only #75/#86/#192 (net +260 in SimpleEditor), but most of the 4,477 -> 5,155 growth came from the repo-wide Javadoc pass (c4f3e6f, e1597ee) and the #167 operation-layer slice (4e843a9) earlier on the same branch; also the evidence's '+179 lines' for 8e75454 is total changed lines (143 insertions / 36 deletions, net +107). The #84 increment a47a1fa was …
- ElementRegistry attribution: it was added in commit 6092703 ('Advance #78'), not 34d92bf (which only routed ElementBlocks through it); citation fixed to the PR and the adding commit.
- Dispatcher range ':2170-2427' imprecise: branch heads span :2179-2425 and the actionPerformed method ends at :2444; restated precisely.

**Action:** comment — https://github.com/anadon/JLS/issues/84#issuecomment-5016287844

### #82

The issue asks for jpackage installers per OS with .jls file association shipped from the tag-triggered release pipeline. The current tree at HEAD 2eb3e0c already has: /home/user/JLS/scripts/build-installer.sh (jar → jdeps module derivation → jlink runtime → jpackage with .jls file association, JBR bundling for Linux with pinned jbrsdk-25.0.3 but UNVERIFIED-PLACEHOLDER sha256s and JLS_JBR_HOME override), /home/user/JLS/scripts/GenerateIcons.java (checked-in icon generation), a five-leg installer matrix in /home/user/JLS/.github/workflows/release.yml (Linux deb/rpm verified, msi/dmg/Windows legs still experimental: true with continue-on-error), and README install docs including the macOS "unsigned by choice" stance (README.md:38-41). The newest comment (2026-07-18 22:10 UTC) describes exactly this state, including the remaining work: arm the JBR sha256 pins, Wayland-verify a JBR-bundled …

**Action:** no-action

### #79

The issue asked for (1) a FORMAT version header with negotiation, (2) a normative save-format spec, (3) type tags decoupled from Java class names, plus legacy-4.1 characterization (P3/P4). At HEAD 2eb3e0c (merge of PR #194) all three code/doc items are present in master: Circuit.java writes a FORMAT header and refuses newer versions with a NEWER_FORMAT error (headerless = v0), docs/file-format.md is a full normative spec (containers, grammar, escaping, 31/32-tag table, version history 0/1/2, evolution policy incl. the #47 RLE caveat and tag-stability rule) linked from README, and tag resolution routes through jls.elem.SaveTags (frozen tag table + alias map) and ElementRegistry.forTag — Class.forName("jls.elem."+tag) is gone from the save-file load path. Tests FormatHeaderTest, FileFormatSpecTest, SaveTagsTest, ElementRegistryTest guard all of it. The residual P3/P4 legacy-corpus work …

**Action:** close-completed — https://github.com/anadon/JLS/issues/79#issuecomment-5016287021

### #78

The issue's first two staged slices are complete and in master at 2eb3e0c: (1) core ElementType + ElementRegistry covering all 33 loadable types, with Circuit.load routed through it (Class.forName hardcode removed, Circuit.java:879-884) and an ElementRegistryTest integrity suite; (2) H3 orientation unification — only JLSInfo.Orientation remains (JLSInfo.java:86), the lowercase Gate.Orientation duplicate is deleted. Still outstanding, exactly as the newest comment lists: palette/toolbar generation from descriptors (SimpleEditor.makeElements() at line 1543 is still hand-enumerated), H2 abstract-ification (throw/print stubs remain at Element.java:387 and LogicElement.java:432), capability interfaces (none of Rotatable/Timed/Watchable/QuickEditable/Editable exist; instanceof Memory branch still at Element.java:856), pin faces, and the #79 alias-table doc. The only change since the newest …

**Action:** no-action

### #77

The issue asks for an enforced-headless jls.core (model + simulation + persistence with zero AWT/Swing/jls.edit imports), decomposition of JLSInfo, and a Circuit model/render split. At HEAD 2eb3e0c the work is partially done and in active progress: the HeadlessCoreRatchetTest architecture ratchet exists and is enforced; Simulator and BatchSimulator are clean of AWT/Swing/jls.edit (batch trace printing extracted to GUI-side BatchTracePrinter over a core TraceSample record, pinned by BatchTracePrinterTest); the ratchet baseline still grandfathers 57 files (Circuit, 53 jls.elem files, InteractiveSimulator/MemTrace/Trace). Circuit still holds the Editor back-reference (setEditor/getEditor) and JLSInfo remains a mutable static hub (frame still used ~43 times as dialog parent). The newest comment (2026-07-18, posted from branch tip 34d92bf) already describes exactly this state; that branch …

**Action:** no-action

### #76

Issue #76 asks for CVD-safe semantic colors with a second visual channel, HiDPI/system L&F work, dark mode, and persistent preferences. The newest comment (2026-07-18T22:11Z) already reports the first implementation slice, and every claim in it verifies at HEAD 2eb3e0c: Theme record with Okabe-Ito palette and classic-scheme fallback (src/jls/Theme.java), the H1 dichromacy-simulation test (test/jls/ThemeTest.java), second channel (thick non-zero / dashed HiZ strokes in src/jls/elem/Wire.java:124-145; ring glyph in src/jls/elem/WireEnd.java:234), java.util.prefs-backed UserPrefs with in-memory fallback persisting theme + grid/background colors (src/jls/UserPrefs.java), and the -Djls.laf=metal|system|<class> policy with fallback instead of System.exit(1) (src/jls/JLSStart.java:760-823, test/jls/LookAndFeelPolicyTest.java). The comment's remaining-work list is still accurate: no FlatLaf in …

**Action:** no-action

### #75

The issue asks for keyboard operability: menu accelerators/mnemonics, a fixed focus model, rotate/flip bindings, platform-convention fixes, and (H2) keyboard-only circuit construction. The H1 slice shipped and is on master at 2eb3e0c (merge of PR #194 from branch claude/advance-github-issues-hjrpyq, tip 34d92bf): a headless-testable MenuAcceleratorPolicy class with tests, File-menu accelerators (Cmd/Ctrl N/O/S/Shift+S/Q) wired WHEN_IN_FOCUSED_WINDOW, mnemonics on all four menus and every item, the mouseEntered focus-follows-mouse grab removed (canvas now focusable on click/tab-select), rotate R/Shift+R and flip F bound and shown in menu items, macOS redo = Shift+Cmd+Z with Cmd+Y alias, and dialog accessible descriptions in ElementDialog. Still open: H2 keyboard-only construction (behind a feasibility spike), the shared Action layer that #73/#84/#86 consume, the File>Close accelerator …

**Action:** no-action

### #74

The issue asks for editor zoom/pan (Viewport transform) plus canvas auto-grow replacing the fixed 1000px square and manual 10% grow button. Current master (2eb3e0c) contains the Viewport coordinate core: `src/jls/edit/Viewport.java` (pure, headless-testable class encoding the adjudicated decisions — [0.25,4.0] clamp, 1.15x cursor-centered wheel step, 25-400% keyboard ladder, fit-to-circuit, model-space hit-testing contract) and `test/jls/edit/ViewportTest.java` (25 @Test methods). Editor wiring is NOT done: `SimpleEditor` never references `Viewport` (its only "viewport" is the JScrollPane's, line 4790), there is no MouseWheelListener/zoom handling, the 10% grow button and `increaseSize()` remain (SimpleEditor.java:441-453, 599-606), and `JLSInfo.circuitsize` is still the fixed 1000 (JLSInfo.java:48). Material change since the newest comment (2026-07-18 22:11 UTC): that comment placed …

**Action:** comment — https://github.com/anadon/JLS/issues/74#issuecomment-5016288153

### #73

The tutorial-refresh slice (the minimal in-scope tutorial work per the 2026-07-17 decisions comment) is fully landed at master HEAD 2eb3e0c: Tutorial.java has Previous/Next navigation, per-page titles, a 760x640 initial size, and no isDemo parameter; the orphaned 21 KB tutorial.html is deleted; no applet/network references remain in src/jls/tutorial (prediction P3 satisfied); TutorialContentTest and TutorialNavigationTest guard it. Everything else in the issue remains open: no welcome/empty-state panel, no resources/samples directory or File→Open Sample menu item, README has zero screenshots (only the OpenSSF Scorecard badge), no feature-overview/positioning paragraph, no Edit menu (menu bar is File/Simulator/Global/Help), and the usability trial has not run. Material change since the newest comment (2026-07-18T22:11Z): that comment reported the work as "integrated on branch …

**Action:** comment — https://github.com/anadon/JLS/issues/73#issuecomment-5016288727

### #63

Issue asks for a full Stage-3 black-box HDL component: header scanners, Yosys fallback, component element with failure-state UX (P4-P6), subprocess harness/transport, event-loop integration, x/z coercion, parity/latency evidence. Current tree (master HEAD 2eb3e0c) contains only §7 items 1-2: the header scanners in /home/user/JLS/src/jls/hdl/scan (VerilogHeaderScanner, VhdlEntityScanner, ScannedModule, ScannedPort, HdlScanException), their tests in /home/user/JLS/test/jls/hdl/scan (21 + 15 test methods), and the Yosys cross-check corpus in /home/user/JLS/test/resources/hdl/scan. No component element, no co-simulation/transport/harness code, no production Yosys fallback exists (grep for cosim/HdlComponent hits only javadoc). Material change since the newest comment (posted 22:11 UTC 2026-07-18): PR #194 merged to master at 23:28 UTC (commit 2eb3e0c), so the work the comment placed on …

**Action:** comment — https://github.com/anadon/JLS/issues/63#issuecomment-5016289001

### #62

The issue asks for a layout seam plus a heuristic layered layouter (later adjudicated to an out-of-process ELK runner instead), a quantified rubric harness, goldens, and #61 integration. Current master (2eb3e0c) already contains the engine-neutral layout seam and executable rubric: src/jls/hdl/layout/ (SchematicLayouter, LayoutGraph, LayoutResult, LayoutInvariants, LayoutMetrics, LayoutException; 1173 lines) with 25 tests under test/jls/hdl/layout/. Per the 2026-07-17 adjudication no Sugiyama layouter was or will be written. Still absent: any SchematicLayouter implementation (the ELK runner), the test/resources/hdl/layout-goldens/ directory and three MTU-sourced goldens, #61 importer integration, and the corpus metrics run. Material change since the newest comment (2026-07-18 22:11Z): the branch that comment cited (claude/advance-github-issues-hjrpyq, tip 34d92bf) merged to master ~77 …

**Action:** comment — https://github.com/anadon/JLS/issues/62#issuecomment-5016289673

### #61

The issue asks for full Verilog import via external Yosys write_json netlists. Current tree (master HEAD 2eb3e0c) contains only the front-end groundwork described in the newest comment (2026-07-18): package src/jls/hdl/yosys/ with JsonValue.java (strict JSON subset parser), YosysNetlist.java (typed netlist model with 0/1/x/z bit sentinels and src attributes), NetlistFormatException.java, CellValidator.java + CellViolation.java (restricted-cell validator with teachable reject messages for async reset, set/reset storage, $mul/$div/$mod/$pow, clocked/multi-port memories), YosysVersion.java (0.38 minimum floor) and YosysLocator.java (PATH detection), plus 40 @Test unit tests in test/jls/hdl/yosys/. Still absent: cancellable Yosys subprocess runner, jls_map.v techmap library, cell-to-element mapper, Splitter/Binder mesh synthesis, save-format emission, import UI (File > Import > Verilog), …

**Action:** comment — https://github.com/anadon/JLS/issues/61#issuecomment-5016290067

### #59

Issue #59 is the staged HDL-interop tracking issue (parent #33; sub-issues #60-#63, 1 of 4 complete). Stage 1 export is shipped and incrementally growing: /home/user/JLS/src/jls/hdl/ contains HdlExporter/HdlModel/VerilogEmitter/VhdlEmitter plus the newer Mux/Decoder SelectStatement support described in the newest comment (2026-07-18 22:12 UTC), with goldens for mux/mux3/decoder in both languages under /home/user/JLS/test/resources/hdl/ and tests in /home/user/JLS/test/jls/hdl/ (golden, policy, CLI, iverilog/ghdl-gated compile tests). The element policy in HdlExporter.java still rejects SubCircuit, Memory, StateMachine, TruthTable — exactly the remaining Stage-1 ledger the newest comment lists. Sub-issue infrastructure for #61/#62/#63 (src/jls/hdl/yosys, layout, scan) is also in tree. The only changes since the newest comment: the branch it described (claude/advance-github-issues-hjrpyq, …

**Action:** no-action

### #56

Nearly all of the issue's checklist has landed on master (2eb3e0c): edge-trigger/latch-discriminator goldens and a 3-state StateMachine golden (test/jls/SequentialGoldenTest.java), the .gitignore fixture exemption (!test/fixtures/**/*.jls at .gitignore:10), JaCoCo wired with @{argLine}-prefixed surefire argLine (pom.xml:248,261,270), CLI smoke/flag/image-export tests (test/jls/CliSmokeTest.java, CliFlagTableTest.java, CliImageExportTest.java), headless printing coverage (test/jls/PrintPathSmokeTest.java, PrintPageOrderTest.java), and the editor-level open→edit→save + clipboard smoke test (test/jls/ui/EditorSaveAndClipboardTest.java on the #91 Layer-2 harness). Remaining, exactly as the newest comment (2026-07-18T22:12Z) states: (1) an authentic JLS 4.1 legacy corpus — the only committed fixture is test/fixtures/fork-4.6-shiftregister.jls, which is this fork's own writer output and …

**Action:** no-action

### #33

Tracker #33 has 28 sub-issues, 24 closed; the four open children are #56 (test-suite gaps), #59 (HDL umbrella), #185 (reproducible builds), #188 (deterministic installers) — exactly what the newest comment (2026-07-18 22:12 UTC) reports. The program's substance is done: v5.0.0–v5.0.4 shipped, the #66 coverage ratchet is enforced in pom.xml, version/license identity is pinned by VersionIdentityTest/FormatHeaderTest, and pinned regression tests for the critical/high findings exist in test/jls. What remains to close the tracker is owner bookkeeping already proposed twice in-thread (ship-policy amendment, label taxonomy/milestone, #80 deprecation-window note, disposition/reparenting of #185/#188). Since the newest comment, only three commits landed (e807068 CI/CodeQL fixes, 9114898 collab AES-GCM swap, merge 2eb3e0c) — none touch the four open children or change any claim in the thread.

**Action:** no-action
