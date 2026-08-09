# Issue #506: CAP-25: one batch invocation over the same 300 submissions flags every planted copied pair with the matched subcircuits shown side by side — and 50 independent correct solutions all score below threshold
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

CAP-25 is a well-constructed capstone specification with genuine discipline
(falsifiable kill criteria, an evidence-not-verdict ethical gate, a
no-second-canonicalizer rule). It is also a document under active,
partially-automated maintenance (four "coverage verification"/REPLAN comments
in five days), and that process has itself introduced or missed defects the
issue's own rigor is supposed to prevent. Findings below are ordered by
severity.

## Findings

### 1. (Medium) The Background section's evidence command does not reproduce, even at the pinned commit
The issue states as verification:
```
$ grep -rli "winnow\|weisfeiler\|plagiar\|fingerprint" src/ test/ docs/ | wc -l
0
```
Running this exact command against the working tree returns **19** matching
files (`src/jls/collab/net/IdentityKey.java`, `SocketSession.java`,
`KnownPeers.java`, `Handshake.java`, `SecureLink.java`,
`docs/capability-roadmap/lf-06-diff-merge-vcs.md`, several tests, etc.). I
confirmed this isn't a post-filing addition: `git show
828822672fc3a8e2cb6da25192472079f04c29dd:src/jls/collab/net/IdentityKey.java`
(the pinned `evidence_commit`) already contains `/** The fingerprint, computed
once: SHA-256 of the public key. */`. The hits are all a homonym — SSH-style
cryptographic key fingerprints in the collaboration/networking module — not
schematic-similarity fingerprinting, so the *conclusion* ("no
plagiarism/similarity tooling exists") is probably still correct. But the
issue presents a specific, copy-pasteable, falsifiable command as its proof,
and that command is false as written at the very commit it cites. Neither of
the two "Feature-coverage verification" comments (2026-08-04) caught this,
even though both explicitly re-ran tracker searches "per PF". Recommend
narrowing the pattern (e.g. exclude `collab/`, or require `graph.*fingerprint`
context) and re-stating the count.

### 2. (Medium) KC-25-1, a hard kill gate on 14–21 mw of scope, is statistically underpowered by the demo slice's own design
KC-25-1: "if the demo slice cannot separate planted pairs from independent
solutions on the 30-submission synthetic corpus with any threshold... stop
before PF-3 is funded — the capstone's premise is **empirically false** at
that point." The demo slice that produces this verdict (#880 → #883/#884/#885)
plants exactly **3** copied pairs against 432 independent pairs (`C(30,2) =
435`). #885 AC-4 itself concedes "n=3 is stated as the limit it is... cannot
support a confidence claim," yet KC-25-1's own wording on #506 claims a
non-separation result makes the premise "empirically false," not merely
"not demonstrated." A single unlucky transform draw among 3 samples can kill
a legitimately viable capstone; a single lucky draw can pass a premise that
fails at scale. Nothing on #506 itself flags this tension between the kill
criterion's confident language and the acknowledged n=3 statistical floor. If
the gate is meant to be taken as strictly as "stop, the premise is false,"
the demo slice should plant more than one instance per transform class before
the gate fires — or KC-25-1's wording on #506 should be softened to match
what n=3 can actually support.

### 3. (Medium) AC-1's "top 15" criterion is satisfiable by luck, not by a stated margin
AC-1 (`PlantedPairRecallTest`): "All 12 planted pairs across 6 transform
classes rank in the top 15 over the 300-submission corpus, with zero
known-independent pairs above threshold." Ranking-in-top-15 is compatible
with planted pairs scoring only marginally above the 44,850-pair (`C(300,2)`)
background — i.e. technically passing while the score signal is weak evidence
of copying. The issue's own §3 risk 2 acknowledges small-assignment
submissions are "intrinsically similar," which is exactly the condition that
would crowd the top-15 with coincidentally-similar independent work at some
corpus compositions, without an explicit AC-1 failure. Contrast with #885
AC-1 (a downstream task), which requires the score gap/overlap to be "stated
explicitly as a number" — that same discipline is absent from AC-1 on this
issue. Recommend AC-1 add a minimum score margin between the lowest-ranked
planted pair and the highest-ranked non-planted pair, not just an ordinal
rank threshold.

### 4. (Low-Medium) `related` list is incomplete relative to the issue's own body
The machine block's `related: [300, 334, 356, 437]` omits **#340 (FEAT-016)**,
which §3 risk 5 discusses by name and number as a live cross-feature
integration risk ("Subcircuit identity interacts with FEAT-016 (#340)... if
FEAT-016's identity machinery lands, PF-1 must state its relation to it").
Both 2026-08-04 coverage-verification comments confirm "no stale links in
`related`" but that check only verifies listed issues resolve — it doesn't
check that everything the body substantively cites is listed. #340 should be
in `related` (as the 2026-08-08 REPLAN comment itself later engages with,
noting #357 as the more immediate counterpart risk).

### 5. (Low) A self-referential `blocks` field shipped in the very decomposition pass that claims completeness
The 2026-08-08 comment titled "CAP-25 now has a complete path to being
started" filed #883, which carries `blocks: [883, 884]` — **#883 lists
itself** as one of the issues it blocks. This is downstream of #506, not on
#506 directly, but it was produced by the same review pass that asserts on
#506 "Reference rows verified... no stale links" and touts the mirrored-edge
machine-block discipline as the guarantee of tracker coherence. A self-loop
in a `blocks` array is exactly the class of bookkeeping error that discipline
is supposed to catch, and undercuts confidence in the surrounding
completeness claims. Worth a housekeeping fix on #883 and a mechanical
(scripted) validator for `blocks`/`blocked_by` self-references, since prose
review evidently missed it.

### 6. (Low) The "cheap premise test" may itself stall on the same blocker it exists to route around
#884 (erasure layer for the demo slice, not independently fetched but
referenced by #880/#883/#885) is required to build "on the existing canonical
form" per KC-25-0-2, i.e. on #356 (FEAT-012). I confirmed #356's own
`planned_tasks` (TASK-0005/0031/0032) are **all still "not yet filed"** and
#356 is `blocked_by: [319, 334]`, both open with no filed children of their
own visible in the fetched issue bodies. So the "cheap, 2-3 mw, tests the
premise before funding 14-21 mw" rationale for #880 may not actually be
startable yet — it inherits the same multi-hop unfiled dependency chain
(#356 → #319/#334) that blocks the full capstone, just one layer removed. No
comment on #506 states this explicitly; each coverage pass checks only
whether #356 itself is "open but not landed," not whether #356's *own*
prerequisites have any filed work either.

## What's solid

- The kill-criteria structure (KC-25-1..4) is unusually rigorous and
  genuinely falsifiable, not decorative.
- KC-25-3's ship-blocking evidence-not-verdict framing, paired with AC-3's
  mechanical wording audit, is a sound ethical safeguard for a tool whose
  misuse risk (false plagiarism accusations) is real and named explicitly.
- The no-second-canonicalizer rule (KC-25-2) is the right call and is
  consistently enforced down through #880/#883's own kill criteria.
- Scope discipline is good: Open Question 1 explicitly rejects building a
  storage/archive service, and KC-25-4 explicitly forbids reaching for
  distributed machinery instead of sharding.
- The most recent REPLAN passes (2026-08-08) show real diligence: correctly
  diagnosing that this capstone was the only one in the tracker with zero
  filed descendants, and choosing to fund the cheapest falsifiable premise
  test first rather than pre-committing 14-21 mw.

## Recommendation

Do not block the current demo-slice work (#880/#883/#884/#885) on the above —
none of these findings invalidate the sequencing decision to test the premise
before funding PF-1..PF-6. But before PF-3 is ever funded off an #885
verdict, (a) fix the evidence grep and re-verify the "nothing exists" claim,
(b) either widen the demo slice's planted-pair count or soften KC-25-1's
"empirically false" language to match n=3's real statistical power, (c) add
an explicit score-margin requirement to AC-1, and (d) confirm #356/#319/#334
have at least one filed task before treating #884 as unblocked.
