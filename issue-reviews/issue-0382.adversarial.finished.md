# Issue #382: TASK-0037: an op applies with no drawing context, and every editor gesture goes through the op vocabulary
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The `Graphics → TextMetrics` signature substitution (O1–O7, H1–H3) is a well-evidenced, low-risk piece of work: every observation I re-derived against the current checkout matched exactly (12 forbidden-import files, 5 `SwingTextMetrics.forGraphics` call sites, the exact line numbers in `CircuitOp.java`, `SimpleEditor.java:5547`, `quickReset` at `:747`, the ratchet's empty `BASELINE` and missing `jls.collab` prefix, `ArchitectureRulesTest`'s Swing-only ban). That half of the issue is sound. The problem is the second half — "migrate the four inline gestures" — which silently duplicates two already-filed, already-open sibling tasks, and a citation of a "consumer" issue that was closed as a duplicate the day after this issue was filed.

## Findings, most severe first

### 1. [Critical] The gesture-migration scope duplicates #282 and #283, which this issue never mentions, in direct violation of its own parent feature's explicit instruction

The issue's Method section commits to: *"Migrate the four inline gestures to `OpSink`: placement drop ..., paste ..., wire-attach finish ..., and the dialog and quick-edit commits (`SetElementConfig`). Budget the bulk of the effort here, not on the signature change."*

This is exactly the scope of two issues filed the day before this one:
- **#282** ("Editor gestures: migrate placement, wiring, and paste commits behind the OpSink seam via preview-then-commit," filed 2026-08-02, open, `part_of_feature: 167`) — placement drop, matching-JumpEnd drop, wire-draw commit, paste, connect-forming move drops.
- **#283** ("Dialog commits: route quick-edit and element edit dialogs through SetElementConfig behind the OpSink seam," filed 2026-08-02, open, `part_of_feature: 167`) — `quickReset`, `ClockDialog`/`ConstantDialog`/`MemoryDialog`, and the remaining element dialogs.

Both are already full research-method issues with their own hypotheses, predictions, DoD checklists, and — critically — their own `part_of_feature: 167` ownership, distinct from this issue's `part_of_feature: 337`. Their existence is not obscure: this issue's own declared parent, **#337 (FEAT-015)**, states in its Related Work section: *"**#282**, **#283** — the two open tasks that move editor gestures and dialog commits behind the `OpSink` seam. These are TASK-0037's criterion-3 gestures, already filed. Any TASK-0037 filing must reconcile with them rather than re-file the same work."* #337's Sequencing section goes further, warning of a live file-level collision: *"#282 and #283 ... can run concurrently with the signature work by separate agents, provided the `OpSink` adapter at `SimpleEditor.java:5497` is not being rewritten at the same moment."*

Issue #382 contains zero references to #282 or #283 — not in its `related` list (`[352, 167, 170, 163, 224, 77]`), not in § Related Work, not in the Method steps that re-describe the same four gestures from scratch. This is precisely the failure mode #337 warned against. As filed, a contributor picking up #382 either (a) unknowingly re-implements #282/#283's work under a different issue number, producing duplicate PRs and merge conflicts at `SimpleEditor.java:5547` (`OpSink` adapter) and the gesture commit sites, or (b) discovers the overlap mid-work and has to stop and reconcile scope — exactly the outcome §7 Re-planning of #337 tries to prevent.

**Recommendation:** Before this issue is picked up, either (a) narrow it to the signature change only (O1–O3, H1–H3, P1/P2/P3/P6) and close the gesture-migration bullet with a pointer to #282/#283, or (b) explicitly supersede #282/#283 here with a `REPLAN:` comment on #337 and both sibling issues reassigning their scope into #382. Do not execute the Method section as written without first resolving which issue owns the four gestures.

### 2. [High] `related: [352, ...]` and the "consumer" framing of #352 are stale — #352 was closed as a duplicate the day after this issue was filed

The issue's Ownership section states: *"FEAT-015 (#337) and FEAT-052 (#352) both name this task. ... #352 is the consumer — a replica cannot converge through the local toolkit — and is named in `related`."* And in § Related Work: *"**#352 (FEAT-052)** — consumer. A vocabulary cannot be closed until it is complete, and a replica cannot converge through the local toolkit."*

I fetched #352 directly: `state: closed`, `state_reason: duplicate`, closed 2026-08-04T07:47:21Z — one day *after* #382 was filed (2026-08-03T14:22:35Z). Its closing comment explicitly says it was split: the replication half went to **#171**, the hardening half went to **#170**, both "the survivor of record." So the "consumer" that #382 cites by number no longer exists as an open feature; the actual current consumers of this task's headless op-application boundary are #171 (replication) and #170 (hardening), neither of which #382 currently names anywhere in its body.

This is exactly the kind of drift #382 itself warns about for the "five files" figure (§ Threats to Validity: *"Anyone reusing it downstream will under-scope..."*) — the same discipline was not applied to its own `related` list before this review. It doesn't invalidate the technical work, but anyone landing this issue and following its own Completion Criteria item *"Landing reported on #337 with a `STATUS:` comment, and on #352 as the consumer"* will find #352 closed-as-duplicate and have to redirect that status comment to #171/#170 — an avoidable stumble the issue should have caught, particularly since #337 (its own cited parent) already lists `blocks: [..., 352]` and would need the same correction.

**Recommendation:** Update `related` to replace 352 with 171 and 170, and fix the Completion Criteria bullet that names #352 as the STATUS-comment target before this issue is worked.

### 3. [Medium] P3's byte-parity guarantee is only as strong as an unstated font-matching assumption, and the issue's own Threats section flags this without closing the gap in the acceptance criteria

P3 requires: apply once through `SwingTextMetrics` built from "a headless-safe `BufferedImage` graphics" and once through the same metrics passed directly, and observe byte-identical canonical saves. § Threats to Validity honestly flags: *"A `BufferedImage` graphics is not a screen graphics. P3's comparison is only meaningful if the `BufferedImage` is created with the same font settings the editor uses; otherwise it compares two headless paths and proves nothing about the editor."*

But the only enforcement mechanism offered is a Completion Criteria bullet asking that "the `BufferedImage` graphics' font settings [be] stated" — a documentation requirement, not a test. Nothing in P1–P6 or the DoD actually asserts that the `BufferedImage`'s font rendering matches what the live Swing editor on a real display (or the CI runner's headless X server) produces. Font metrics are notoriously platform- and JVM-font-config-dependent (this is the exact class of hazard `TextMetricsParityTest` was built to pin for the *original* `Graphics`-based path, per O6). It is entirely possible to satisfy P3 literally — two headless paths agreeing with each other — while the substitution silently changes geometry relative to the actual GUI path on some platform, which is the one thing §4/DoD says must never happen ("Saved bytes are byte-identical for every gesture, before and after"). The issue's own DoD line *"P3's byte comparison recorded for all four sizing-sensitive kinds, with the `BufferedImage` graphics' font settings stated"* is gameable: recording the font settings is not the same as proving they match the editor's.

**Recommendation:** Tie P3's `BufferedImage` construction to the same font source `SimpleEditor.getGraphics()`/`SwingTextMetrics.of` actually uses (e.g., derive the `BufferedImage`'s `Graphics2D` font from `UIManager`/the editor's own default font), or add a fourth check that runs the display-tagged gesture path (already planned at the end of § Method) and diffs its canonical save against the headless path's, rather than treating "the display-tagged execution" as a separate, unlinked checklist item.

### 4. [Medium] Cost/scope-creep risk compounding Finding 1: "budget the bulk of the effort here" for scope that, if genuinely absorbed from #282/#283, is priced nowhere in this issue

#337 prices TASK-0037 at a flat 2 maintainer-weeks, described as "mechanical" for the signature change ("this is why the band is weeks and not months: this is a mechanical substitution against an already-designed seam"). #282 and #283, as independent issues, each carry their own full experimental-design weight (preview-then-commit redesign for placement/wiring/paste with an unresolved Open Question on connect-forming composite plans; a separate wired-element policy decision for dialog commits). If #382 is meant to absorb that scope (as its Method section implies), the 2-week estimate in #337 is stale and needs re-derivation; if it is not meant to absorb that scope, the "budget the bulk of the effort here" instruction is simply wrong about what #382 should spend its effort on. Either way this is downstream of Finding 1 and should be resolved together with it.

### 5. [Low] Minor internal ambiguity: the "matching JumpEnd creation" inventory row is folded into "placement drop" without saying so

`docs/operation-layer.md`'s inventory table lists "Matching JumpEnd creation, context menu" as its own row, separate from "Placement drop," with the note that its commit point *is* the later drop ("the created end stays mouse-attached in `chosen` state, so the commit point is the later drop"). #382's Method section only lists four gesture bullets and doesn't mention JumpEnd creation at all. The omission is almost certainly intentional (the row's own text explains why it rides along with placement drop), but #382 never says so, leaving a reader to independently reconstruct that a fifth inventory row is silently covered rather than dropped. A one-line note would remove the ambiguity, especially given P5's reflective/table cross-check is meant to catch exactly this kind of unowned row.

## What's solid (no rework needed)

- **O1–O3 (the `Graphics` parameter, the sealed permits list, the five call sites) are accurate** — verified byte-for-byte against the current tree, not just the pinned evidence commit.
- **O4/O5 (ratchet doesn't cover `jls.collab`, ArchUnit only bans `javax.swing..`)** — both confirmed by direct inspection of `HeadlessCoreRatchetTest.java` and `ArchitectureRulesTest.java`.
- **The "five files vs twelve" self-correction (§2) is a genuine, checked improvement** over a stale corpus figure, and the issue is explicit that downstream reuse of "five" would under-scope — good practice, applied inconsistently only for #352 (Finding 2).
- **The nullable-`TextMetrics`-preserves-null-`Graphics`-semantics design (O6, H1) is real and already shipped**; `SwingTextMetrics.forGraphics` and `TextMetrics`'s javadoc match the issue's description exactly.
- **The self-verification discipline** ("Re-verify O2, O3 and O4 at the working checkout before step 1") correctly anticipates exactly the kind of tree drift I found (unrelated files changed since the evidence commit) and instructs the implementer to re-derive rather than trust the pinned numbers blindly — this is the right response to staleness, just not applied to the issue's own cross-references.
- **`AddElements`'s scratch-circuit dry-run claim (§11) and `SetElementConfig`'s double-init-same-metrics requirement** are both accurate against the current source (`AddElements.java:92-152`, `SetElementConfig.java:66,120`).

## Verdict rationale

The technical core (signature substitution) is sound and low-risk. But the issue as filed instructs an implementer to redo work that two open, already-filed sibling issues already own, without disclosing that overlap, in direct contradiction to an explicit warning in its own cited parent issue (#337). That is not a cosmetic problem — executing the Method section as written risks wasted work, merge conflicts, or a contributor unknowingly closing #282/#283's scope under the wrong issue number. Combined with the stale #352 reference, this needs a scope reconciliation pass before anyone starts implementation.
