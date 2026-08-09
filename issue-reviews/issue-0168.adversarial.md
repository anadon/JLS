# Issue #168: P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification (collab Stage 1a)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The headless crypto/transport core this issue claims as "prior work" checks out against the repo: `src/jls/collab/net/{IdentityKey,Handshake,Sas,SecureLink,KnownPeers,Transport,SocketSession,SessionListener,LoopbackTransport,Crypto}.java` all exist, `Crypto.java` really does AES-256-GCM (not the superseded ChaCha20-Poly1305 claim), `test/jls/SocketConfinementRatchetTest.java` and `test/jls/BootListenerHygieneTest.java` exist and match their described purpose, `ArchitectureRulesTest.collabLayersAreHeadless`/`transportKnowsNothingOfCircuits` exist, `docs/collab-handshake-review.md` exists (262 lines), and there is indeed no `jls.collab.ui` package and no glyph asset anywhere in the tree. The issue is largely honest about what's landed. The problems are mostly in how the *remaining* scope is tracked and specified, not in the delivered crypto.

## Findings (numbered by severity)

**1. (High) The machine-readable plan is already stale versus the issue's own comment history.**
The body's YAML block still reads:
```
planned_tasks:
  - "jls.collab.ui dialogs: Share/Start-session ... Join, Verify ... key-change warning ..."
  - "Two-machine LAN join+verify manual record ..."
```
i.e. two unfiled placeholder strings. But comment 5227019162 (2026-08-08, the newest comment) states the work was actually filed on 2026-08-04 as **four** separate issues: #814 (glyph images), #815 (Share/Join), #816 (Verify/key-change dialog), #817 (LAN record) — and I confirmed all four exist with exactly that content via `issue_read`. The issue's own Re-planning Protocol (§7) says "`planned_tasks` entries resolve to numbers via REPLAN when filed," and Completion Criteria requires "`planned_tasks` empty (each resolved to a filed issue...)". Four days after filing, no REPLAN comment or body edit reconciled this — the body a reader opens today still describes a two-task plan that no longer matches what was actually filed. Anyone closing out §7/Completion Criteria by reading the body alone will certify a false state.
*Recommendation:* Require a REPLAN edit to the body's `planned_tasks`/`requires_tasks` before any further child work is claimed against this issue.

**2. (High) The decomposition rationale directly contradicts what was actually filed.**
Section 2 states as settled reasoning: *"An alternative cut (glyph images as their own task) was rejected: the Verify dialog is untestable without the images, so they ship together."* Yet #814 (`TASK-C168-1`, glyph images) and #816 (`TASK-C168-3`, Verify dialog) are two separate issues, with #816 merely listing `ordering_after: ["TASK-C168-1", "TASK-C168-2"]` — precisely the split the body says was rejected. Either the rationale in §2 is wrong and needs to be struck, or the actual filing (#814/#816) violates the feature's own recorded design decision and needs to be re-merged. Right now both statements are live in the tracker simultaneously, which is a genuine internal contradiction, not just a formatting lag.
*Recommendation:* Pick one: update §2's rationale to match reality, or flag #814/#816 for REPLAN merge.

**3. (Medium) Structured sub-issue linkage is empty; tracking is comment-prose-only.**
`issue_read(method=get)` on #168 reports `has_children: false`, and `get_sub_issues` returns `[]`. #814–#817 each carry `part_of_feature: 168` only inside a free-text YAML fence in their body — not as a GitHub sub-issue relationship, and not listed in #168's own `requires_tasks`/`blocked_by` arrays (both `[]`). Any tool or reviewer using the structured relationship API (as this review did) will see zero linked children and disagree with the narrative in comment 5227019162. This is exactly the kind of drift the issue's own Completion Criteria bullet ("Machine block, roster table, and mermaid graph agree with reality at close") is trying to prevent, and it is already failing pre-close.
*Recommendation:* Attach #814–#817 as real GitHub sub-issues, or at minimum list their numbers in `requires_tasks`.

**4. (Medium) The mermaid dependency graph is wrong for the plan that was actually filed.**
The graph shows a linear `P1 (dialogs) → P2 (LAN record) → F168`, i.e. one dialog task then one manual task. The real filed order is #814 (glyphs) and #815 (Share/Join) with no stated ordering between them, then #816 (Verify dialog) ordered after *both*, then #817 (LAN record) ordered after #816 — a 4-node chain/fan-in, not the 2-node chain drawn. A reader relying on the diagram (which the issue explicitly offers as authoritative sequencing) will misjudge what can run in parallel.
*Recommendation:* Regenerate the graph from the four filed issues' `ordering_after` fields.

**5. (Medium) I3's "loud warning" and #816's "structurally distinct" criteria are not concretely testable.**
Global Invariant / Integration Criterion I2 requires "a KEY_CHANGED peer produces the loud-warning dialog path," and #816 AC-3 requires it be "visually and structurally distinct from the ordinary verify path." Contrast this with the same document's crisp, gameable-proof language for the Verify dialog itself ("Confirm is disabled until both glyph panels have rendered and there is no Enter-to-confirm default"). "Loud" and "structurally distinct" have no analogous bar — a dialog differing only in title-bar text technically satisfies "distinct" while still being one reflexive click away from being ignored, which is precisely the MITM-acceptance failure mode SAS verification exists to prevent.
*Recommendation:* Define "loud" operationally (e.g., non-default-focused affirmative control, mandatory secondary confirmation, or a forced delay) before #816 is graded against it.

**6. (Medium) The sole non-automatable acceptance gate (#817, two-machine LAN) has no owner or trigger.**
The issue itself concedes I3 "cannot be CI-gated at all" and that merging it with the dialog slice "would couple a mergeable code slice to a manual procedure." Every other comment on this issue (9 of 9) is machine-generated ("Generated by Claude Code"), and the whole visible workflow is an autonomous agent fleet. Nothing in #168 or #817 names who performs the manual two-machine run, on what cadence, or what happens if no human volunteers — a real risk that a project running almost entirely on agent-driven advancement stalls indefinitely on the one step agents structurally cannot perform themselves.
*Recommendation:* Name an owner/trigger condition for #817 explicitly, distinct from "queued for the next work cycle."

**7. (Low) Glyph-licensing contingency is awkwardly homed.**
§7's Re-planning Protocol only routes a glyph-licensing failure through the "Dialog child REFUTED" trigger ("glyph rendering unreliable even as bundled images"), but a *licensing* failure (no GPLv3-compatible set obtainable — a real possibility for a themed 64-word/glyph set) is a different failure mode than a rendering-reliability problem, and #814 AC-3's own escalation clause ("record the gap and stop; do not vendor") isn't cross-referenced from §7. A licensing dead-end could sit unresolved without triggering either the letter or the obviously-intended spirit of the re-planning protocol.
*Recommendation:* Add a licensing-failure trigger to §7 distinct from the rendering-reliability one, or reference #814 AC-3 directly.

## What's solid (no rework needed)

- The "prior work" claims are independently verifiable and accurate: file paths, the AES-256-GCM correction, the absence of `jls.collab.ui`/glyph assets, and the existence of `docs/collab-handshake-review.md` all check out against the current tree.
- Invariant 3 (no listener outside explicit Share) is enforced by both a structural ratchet (`SocketConfinementRatchetTest`) and a runtime behavioral test (`BootListenerHygieneTest`), a genuinely solid belt-and-suspenders pair, not just a stated intention.
- The #168-vs-#169 boundary comment (5227019162) is a good-faith, detailed attempt to prevent double-counting `jls.collab.ui` work across two issues — the underlying tracking mechanics (finding 3) are what's weak, not the intent.
- Scope boundary against #167/#169/#170/#155 (op vocabulary, roster/floor-control, NAT/discovery, general arch hardening) is clearly and correctly drawn; no obvious scope creep in the *stated* remaining work itself.

## Bottom line

The delivered crypto/transport core is real and matches its description. The remaining-scope bookkeeping inside the issue body, however, has fallen behind its own comment thread in a way that creates two provably contradictory readings of "what's planned" (findings 1–2) and leaves the feature's structured tracking (sub-issues, dependency graph) disagreeing with the prose truth (findings 3–4). Two acceptance criteria (the "loud warning" language) are soft enough to be satisfied without achieving the actual security goal (finding 5), and the one human-gated criterion has no named owner in an otherwise fully agent-driven pipeline (finding 6). None of this blocks continued work on #814/#815 (glyphs, Share/Join — independent, well-specified), but #168 itself needs a REPLAN pass to reconcile its body with reality before it can be trusted as the source of truth its own Completion Criteria demand it be.
