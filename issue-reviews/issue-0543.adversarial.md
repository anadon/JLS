# Issue #543: FEAT-C26-2: an instructor previews a handout as their colorblind students see it — protanopia, deuteranopia and tritanopia simulated in-app over the live canvas
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

#543 (FEAT-C26-2) proposes a built-in CVD preview mode that filters the
live editing/simulation canvas at the framebuffer level, per CAP-26
(#507) Open Question 5's recommended default, so an instructor can vet a
handout the way a colorblind student will actually see it. Filed
2026-08-04 with `tier:feature`. Its three comments (through 2026-08-08)
progressively absorb a duplicate task (#736), then split the feature into
two new tasks (#876, #877). The technical core is sound — see #876/#877's
own adversarial reviews, already on file — but the issue *as it currently
stands* (title, body, labels, state) is stale, structurally orphaned from
the work that supersedes it, and carries an unbudgeted feasibility risk
its own comments never surface.

## Findings, most severe first

**1. The issue's body is stale relative to its own comment history, and nothing structural marks the successor scope.**
#543's "Acceptance criteria" section (four bullets) is the version an
implementer reading only the issue sees. But comment 2
(`#issuecomment-5226989691`, 2026-08-08) records that this exact scope
was independently re-filed as #736, absorbed as a near-duplicate with
*stronger* criteria (e.g. AC-1 gains "not a static snapshot", AC-2 gains
a named-test requirement, AC-4 splits into a byte-identity clause and a
separate cost clause), and comment 3 then splits that stronger scope into
#876 (transform) and #877 (surface) — neither of which #543's body
mentions. I confirmed via `issue_read(get_sub_issues)` on #543 that it
returns `[]`, and #876's own review independently confirms
`get_parent(#876)` returns `null`: the "roster, filed" table in comment 3
is prose only, not a GitHub sub-issue link. `issue_read(get_labels)` on
#543 shows only `enhancement, area:gui, area:ux, tier:feature` — no
"superseded"/"tracking" marker. Anyone who queries #543 directly (the
title still reads as a live, standalone spec) and does not read all three
comments plus cross-reference #876 and #877 will implement the weaker,
already-superseded AC set — the identical failure mode the absorption
comment itself was written to prevent.
*Recommendation:* either close #543 with a pointer comment ("superseded
by #876+#877") and actually wire the sub-issue links, or edit the body's
AC section to say "see #876/#877" — the project has a stated norm against
silently editing bodies, but a redirect note is not a silent edit of
scope, it is a correction of a now-false "this is current" implication.

**2. The live-paint-architecture feasibility risk behind "transform the live editing/simulation canvas" is never surfaced in #543, and it undermines the `band_mw: 1-2` estimate #543 sets and both children inherit.**
#543's only cost signal is `band_mw: 1-2` in its own front matter, later
split "0.5-1 / 0.5-1" across #876/#877 in comment 3 with no re-costing.
#877's independent review (`issue-reviews/issue-0877.adversarial.md`,
finding 2) establishes that `SimpleEditor.paintComponent`
(`src/jls/edit/SimpleEditor.java:2448-2524`) draws straight to the
on-screen `Graphics2D` Swing hands it, and the method's own comment says
the paint-pass `Graphics` "is NOT cached — Swing may dispose it after
this call." Turning that into "draw to a buffer, filter, blit" is a paint
pipeline restructuring with `firstDraw`/undo-snapshot adjacency risk
(lines 2519-2522), not "one filter, one test." This obstacle exists in
the *current* checkout, at filing time — #543 could have found it with
the same grep-and-read #877's reviewer did. Because #543 is the issue
that set the original 1-2 mw figure now inherited by both children
unexamined, the underestimate originates here, not downstream.
*Recommendation:* re-derive #543's cost band (or explicitly disclaim it
in favor of #876/#877's, now that those exist) against the actual paint
architecture before anyone treats 1-2 mw as load-bearing.

**3. AC-4's "K9" citation is used without definition and does not resolve anywhere in this repository.**
#543's own text: *"rendering is byte-identical to the shipped default
(K9 — no cost to existing users)."* #876's review (finding 1) already
established that `K9`/`D9` appear nowhere in `docs/`, `.java` sources, or
`ISSUE-AMBIGUITIES-2026-07.md` — only in other synthetic planning issues
— and that where #507 *does* gloss similar codes (`KC-26-4`: pixel-
identical theme; `D9`: "audience-fit objections withdrawn"), neither is
actually a per-frame-cost claim. That confusion originates in #543: this
issue is the one that first attaches "K9" to a cost-not-just-bytes claim
in this cluster, and it does so as an unglossed parenthetical with no
citation to where K9 is defined.
*Recommendation:* either cite the source of "K9" explicitly (which
document, which line) or drop the shorthand and state the cost
requirement in words, as #876 was told to do in its own review.

**4. The Open-Question-5 "blocks filing" gate was bypassed by explicit maintainer directive, not resolved — and #507's own body still reads as if the gate is live.**
#543's boundary notes say plainly: *"CAP-26 marks Open Question 5 as
blocking PF-2's filing; this issue is filed under today's
capstone-coverage directive... If the maintainer decides otherwise,
resolve by REPLAN on #507."* This is at least disclosed, not
smuggled — #507's single comment (`#issuecomment-5174524981`) flags
exactly this under "Contradictions / flags for the maintainer" item 1.
But #507's body, as currently written, still lists Open Question 5 with
"Recommended default: framebuffer-level filter... **Blocks PF-2's
filing.**" with no REPLAN comment recording a decision — the capstone's
own §5 protocol requires "Every response ends in a REPLAN: comment
here," and the one comment on #507 is a coverage audit, not a REPLAN
closing OQ5. The gate reads as still-blocking in the one document
(#507) that is supposed to be the canonical decision record, while #543
proceeded anyway.
*Recommendation:* file the REPLAN on #507 that OQ5's default was
accepted (or is provisional pending maintainer sign-off), so #507's own
open-questions table stops contradicting the fact that #543/#876/#877
already exist and are being worked.

**5. The #542 ordering-inversion risk is self-identified and still explicitly unresolved as of the latest comment.**
Comment 1 on #543 flags that #543 declares `ordering_after: [FEAT-C26-1]`
(#542) while #542 declares `ordering_after: []` yet needs the same CVD
transform for its own AC-2 — a circular dependency between siblings.
Comment 3 repeats verbatim: *"The #542 ordering inversion is still open
and is not silently decided... It is not resolved by this comment and
should not be treated as resolved."* #876's independent review (finding
3) confirms the concrete consequence: `ThemeTest.java:158-167` already
hardcodes protanopia/deuteranopia matrices for #542's delta-E ratchet,
#729 (open) will add tritanopia to that same test-only set, and #876
separately needs all three matrices in production code — with no shared
source of truth required by any issue's acceptance criteria. This is a
real, disclosed-but-live duplication risk that #543 (as the issue that
introduced the inverted ordering edge) has not moved to resolve after
three comments.
*Recommendation:* file the REPLAN on #507 this thread keeps promising
and pick one canonical owner for the matrix constants before either
#876 or #542's downstream tasks land.

**6. AC-1's "selectable in-app" has no discoverability, reachability, or verification method in #543 itself — later criteria (keyboard reachability, "not a static snapshot") were added only in #736/#877, not here.**
Taken at face value, #543's four bullets could be satisfied by an
internal, code-only toggle wired to nothing a user can reach — nothing
in #543 requires a menu entry, keyboard shortcut, or accessible name (the
`#877`-added criterion 4, keyboard reachability, exists nowhere in #543).
Given finding 1 (implementers may work from #543's body directly), this
gap is not hypothetical.
*Recommendation:* covered by fixing finding 1 — point implementers at
#877, which already carries the stronger criteria.

## What's solid

- The framebuffer-vs-theme-level distinction is real and well-reasoned:
  `Theme.java`'s `install()` (lines 147-160) only ever rewrites
  `JLSInfo.Palette` statics that drawing code reads by convention — it
  has no hook into a rendered pixel buffer, so a theme-level
  implementation genuinely would miss the hardcoded call sites, and the
  issue correctly identifies that as the reason to filter at the
  framebuffer instead.
- Tying the instructor-facing preview to the same code path as the CI
  screenshot apparatus (AC-3) is a good engineering principle stated up
  front — it forecloses the cheap-but-useless "cosmetic preview,
  separately-tested CI transform" implementation before anyone builds it.
- The "K9 — no cost to existing users" *intent* (pixel-identical default
  rendering) is a legitimate, precedented invariant — it mirrors
  `Theme.CLASSIC`'s already-enforced "reproduces the pre-#76 look
  exactly" guarantee — even though its citation (finding 3) is broken.
- The issue is honest about not deciding the #542 boundary or the
  maintainer-override path for OQ5, and routes both through `REPLAN:` on
  #507 rather than silently assuming an answer — the right instinct, even
  though (finding 4, 5) neither has actually been closed out yet.

## Verdict

**needs-rework.** The technical premise is sound and its two successor
tasks (#876, #877) have already been reviewed as sound-with-concerns on
their own merits. But #543 itself, as a standalone artifact someone could
pick up today, is stale: its body doesn't reflect the stronger criteria
its own comments established, its children are not structurally linked
(`get_sub_issues` empty), its cost band was never re-derived against the
live-paint-architecture risk it should have caught before filing, its
"K9" citation doesn't resolve, and two disclosed cross-issue
dependencies (#507 OQ5, #542 ordering) remain open three comments in.
Before anyone starts work under the #543 label, close the gap between
what the issue says and what the thread has already decided.
