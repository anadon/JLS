# Issue #666: TASK-C111-6: `GUI boot (Windows, WindowStation)` earns its required check — a blank window or an unbootable jar turns the merge red
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Promote the already-scaffolded `windows-gui` lane (`ci.yml:722-760`, `scripts/windows-rig.ps1`) from advisory to a required branch-protection check: accrue a 20-run record (≤1 failure) on parent issue #111, drop `continue-on-error`, register the byte-stable name `GUI boot (Windows, WindowStation)`, and keep the exit 0/1/2 taxonomy intact (with a documented `java.awt.Robot` fallback if the non-blank-screenshot gate proves unreachable).

## Findings, most severe first

**1. [High] The promotion step this issue asks for has a live, unaddressed six-hour-hang risk, and the issue is silent about it.**
`git grep timeout-minutes .github/workflows/ci.yml` returns zero matches — confirmed directly (`Grep` over `ci.yml`, no hits). Issue #374 (TASK-0015) independently audited this at commit `2d0ca9d` and explicitly lists `windows-gui` among the 23 offending jobs (`MISSING timeout-minutes: .github/workflows/ci.yml :: windows-gui`), and its `blocks:` field/body argues that **registering a required check with no job timeout converts a hung runner into a silent 360-minute (GitHub hosted default) block on every PR** — precisely the change #666's second acceptance-criteria bullet ("`continue-on-error` is dropped and the check is registered in branch protection") would make. `windows-rig.ps1`'s own internal wait loops are bounded (60s/90s/30s), but nothing bounds the enclosing job — a hung `mvn -B -DskipTests package` (network-dependent Maven resolution) or a wedged `Start-Process`/screenshot call on a bad runner image has no backstop. #111's own newest comment (2026-08-08T17:20:34Z, posted *after* #666 was filed) says outright: "#374 … still blocks every promotion here … That must land before #661 or #666 drops `continue-on-error`." #666's `ordering_after: []` and its acceptance criteria never mention #374.
*Recommendation:* add `374` to `ordering_after` and make "the job carries an explicit `timeout-minutes`" a fifth acceptance criterion before the `continue-on-error` drop.

**2. [High] `ordering_after: []` is contradicted by the parent issue's own most recent comment, and that comment's "recorded on both endpoints" claim does not hold up.**
#666's machine block reads `ordering_after: []   # rig scaffolding landed (PR #266); record accrues independently of W1`, which faithfully echoes #111's original §6 ("W7's record accrues independently of the headless path"). But #111's newest comment (same day, 2026-08-08T17:20:34Z — two hours after #666 was filed at 15:18:34Z) asserts a *different* dependency: "**#406 precedes #665 and #666** — the file-handle probe work and the GUI-boot rig are only meaningful once `Build (Windows, JDK 25)` blocks," and explicitly claims this was "recorded on both endpoints." I fetched #666's comments directly (`issue_read get_comments`) and it has **zero comments** — the claimed endpoint note was never actually posted there. So a contributor picking up #666 right now sees stale sequencing in the issue body and no confirmation of the newer, contradicting claim anywhere on #666 itself.
*Recommendation:* before starting work, get the maintainer to state authoritatively whether #666 is gated on #406 or genuinely independent of it (as originally designed) — right now the two live issues disagree with each other.

**3. [Medium] The 20-run recording format under-specifies durability, contrary to the project's own established convention.**
AC bullet 1 requires the record be "recorded on #111 … with every run ID" — an issue comment. But the one precedent this repo has for exactly this kind of promotion (`gui-wayland`, cited approvingly in #406 O5) embeds the run-ID record as a **permanent comment inside `ci.yml` itself** (`ci.yml:328-346`), with the stated rationale "the promotion record lives in the workflow comment, deliberately, so it cannot be lost the way a PR description can." #666's AC doesn't ask for the ci.yml-embedded form, so it can be satisfied by an issue comment alone — the exact failure mode the project already designed around.
*Recommendation:* AC bullet 1 should require the `ci.yml:328-346`-style workflow comment, with the issue comment as a secondary/summary record, matching the `gui-wayland` template this rig was explicitly built to imitate (`windows-rig.ps1` header: "the Windows twin of the Linux/macOS GUI-boot rigs").

**4. [Medium] "at most one failure" in 20 runs is gameable because the issue doesn't say which runs count.**
The `gui-wayland` precedent (O5, quoted in #406) explicitly *excluded* a shared-infrastructure failure ("run 30226493722 … a whole-master `mvn package` breakage where every build/installer/GUI job failed … not a lane flake") from its 20-run tally. #666 gives no equivalent rule for what disqualifies a run (shared jar-build breakage, a runner-image outage, a self-test failure vs. a rig failure, etc.), so the "at most one failure" bar can be satisfied by post-hoc reclassifying inconvenient reds as "not real failures" — an acceptance criterion that can pass on paper while masking a genuinely flaky lane.
*Recommendation:* state the disqualification rule up front (mirror the `gui-wayland` precedent's carve-out for shared/global breakage only, nothing rig-specific).

**5. [Low] The rig's own doc comments are already stale about whether it has run before, and #666 inherits that ambiguity uncritically.**
`scripts/windows-rig.ps1:48-52` states "this rig has **NEVER** run on a real Windows runner - its first execution is on CI." But `ci.yml`'s job comment (lines 710-721) narrates a completed first-light run (`30322375242`) that already executed the (buggy) rig and got misclassified as exit 1 before the `Start-JavaProcess` scoping bug was fixed. #666 doesn't address whether that already-run, now-superseded execution counts toward, or must be excluded from, the 20-run window, nor does it flag that the script's header comment needs updating once real runs exist.
*Recommendation:* fold "reconcile `windows-rig.ps1`'s stale header claim" into the task, and explicitly exclude the pre-fix run from the 20-run count (it exercised a known-buggy rig, not the current one).

**6. [Low] Label taxonomy nit.** Labels include `area:ux`, but this is pure CI/test-infrastructure work with no editor, dialog, or rendering surface touched — worth a maintainer glance, not a blocker.

## What's solid (verified, not just asserted)

- The exit-code taxonomy in AC bullet 3 matches the actual implementation byte-for-byte: `Invoke-GuiBootRig` in `scripts/windows-rig.ps1` (lines 241-334) really does return 2 for any rig/environment fault (readiness capture failure, control-frame no-map, blank control screenshot, unexpected exception) and 1 only once the control frame has already mapped and rendered non-blank — matching "#111 invariant 6" as stated in #111's Global Invariants §4.6.
- The byte-stable check name `GUI boot (Windows, WindowStation)` is exactly what's registered at `ci.yml:723` (`name: GUI boot (Windows, WindowStation)`) — no drift between the issue text and the workflow.
- The `java.awt.Robot` fallback / re-plan clause (AC bullet 4) is not invented scope — it verbatim mirrors #111's own Re-planning Protocol ("if the non-blank screenshot gate proves unreachable … the in-JVM `java.awt.Robot` self-capture is the named fallback path").
- The Boundary section's cross-references check out: #265 really is the macOS Stage-9 analogue, and #411 (TASK-0018) really does own the Linux rigs' runtime pinning, per the cluster-C dedup comment on #111.
- The "not the `@Tag("display")` unit suite (TASK-C111-3)" boundary is accurate and matches how `ci.yml` and `ARCHITECTURE.md` distinguish app-boot smoke from the in-process UI suite.
