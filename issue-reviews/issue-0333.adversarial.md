# Issue #333: FEAT-056: partitions on separate hosts exchange boundary events, and the result is byte-identical to running the design whole
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is a well-argued design document for a conservative distributed-simulation
barrier protocol, and most of its code citations check out against the checked-out
tree. But its own machine-readable state (`requires_tasks`, `planned_tasks`,
`blocked_by`) is stale relative to the live tracker — six child tasks already exist
and one blocking dependency has already closed — and a document the issue leans on
as load-bearing evidence for a hard blocking criterion does not exist anywhere in the
repository. Both are the kind of drift this issue's own re-planning protocol exists
to catch, and neither has been caught.

## Findings, most severe first

### 1. `docs/parity-contract.md` — the cited evidence for criterion 8 does not exist in the tree

The issue cites `docs/parity-contract.md:469-477` three times as load-bearing:
in criterion 8 ("`docs/parity-contract.md:469-477` states '**One determinism
assumption remains unverified**'..."), in Open Question 3 ("blocks integration"),
and implicitly in invariant coverage for criteria 1/2/5, which the issue says "rest
on" that assumption. I checked the working tree at `/home/user/JLS`:

```
$ find . -iname "*parity*"
./test/jls/edit/TextMetricsParityTest.java
./test/jls/elem/GateOutlineParityTest.java
./test/jls/DrawCullingParityTest.java
./test/jls/core/BoundsGeometryParityTest.java
./test/jls/core/SegmentGeometryParityTest.java
```

No `docs/parity-contract.md` anywhere, in any case variant, and no `docs/plan/`
directory of any kind exists. By contrast, every *other* document this issue and its
siblings cite resolves cleanly: `docs/grand-architecture.md` (494 lines),
`docs/operation-layer.md` (165 lines), `docs/file-format.md` (549 lines),
`docs/collab-handshake-review.md` (262 lines) all exist. This is not "the planning
corpus is fictional" — it is one specific, specifically-line-numbered citation that
is absent while its neighbors are present.

The child task that was actually filed to satisfy criterion 8 makes this worse:
`TASK-C333-1` (#830) AC-2 reads "the 'remains unverified' sentence at
`docs/parity-contract.md:469-477` is **replaced** by the measured answer" — i.e. the
task's own acceptance criterion requires editing a file that does not exist. Either
the document needs to be created from scratch first (in which case the issue should
say so, not cite it as settled fact), or it lives somewhere this checkout doesn't
see, in which case the citation is unverifiable by anyone working from `main`.

**Recommendation:** before this issue or #830 proceeds, resolve whether
`docs/parity-contract.md` exists on some other branch/location or needs to be
written. If it needs to be written, that is new work not currently on any roster,
and criterion 8 / Open Question 3 / invariant coverage all need to be restated
against whatever document actually carries the cross-platform-determinism claim.

### 2. The issue's own `planned_tasks`/`requires_tasks` are stale — six child tasks already exist, uncounted

The issue's Completion Criteria requires `planned_tasks` to end up "empty (each
resolved to a filed issue or descoped)" via a `REPLAN:` comment, and §7 states "each
planned scope resolved to a filed issue number by REPLAN when it is filed." Searching
the tracker:

```
TASK-C333-1 (#830, open) — part_of_feature: 333 — cross-platform determinism experiment
TASK-C333-2 (#832, open) — part_of_feature: 333 — boundary-event marshalling over Transport
TASK-C333-3 (#834, open) — part_of_feature: 333 — the conservative barrier / advance rule
TASK-C333-4 (#836, open) — part_of_feature: 333 — lookahead refusal by name
TASK-C333-5 (#838, open) — part_of_feature: 333 — the partition-count invariance suite
TASK-C333-6 (#839, open) — part_of_feature: 333 — checkpoint/resume for a partitioned run
```

All six were filed 2026-08-04T15:34–15:35Z, all declare `part_of_feature: 333`. Yet
issue #333's body — last edited the same day but earlier (its one comment is
timestamped 07:48:28Z, before these tasks existed) — still shows `requires_tasks: []`,
`planned_tasks` listing four scopes all marked "not filed" / "ABSENT," and the
Decomposition table's Status column reading "not filed" for every row. No `REPLAN:`
comment recording the filing exists on #333 as of this review. A reader who trusts
only #333's body (which is what the issue explicitly asks reviewers to do — "should
be cited as a fact rather than re-argued") would not know this decomposition already
happened, and could duplicate it or plan against a cost basis (see #3) that the real
children have already superseded.

This is exactly the failure mode §7 of #333 itself warns against ("a half-edge is the
defect the Link phase exists to prevent") — except here it's a whole missing roster
update, not a half-edge.

**Recommendation:** post the `REPLAN:` this issue's own process requires, updating
`requires_tasks` to `[830, 832, 834, 836, 838, 839]`, emptying `planned_tasks`, and
correcting the Decomposition table before anyone is assigned further work here.

### 3. Cost band is silently blown by the tasks that already exist

Issue #333 states "the band is **unvalidated by decomposition**" and that pricing the
four scopes is "the correct next step," implying the 10-18 mw band is still an open
question. But the six filed tasks each carry a `band_mw` figure:

```
TASK-C333-1: 1-2      TASK-C333-4: 2-3
TASK-C333-2: 3-5      TASK-C333-5: 3-4
TASK-C333-3: 4-6      TASK-C333-6: 3-5
```

Sum: 16-25 mw against a stated 10-18 mw band — already over budget at the high end
before any task has landed, and barely inside it at the low end with zero
contingency. This is precisely the kind of gap the issue's own Open Question 4 and
§7's cost-reconciliation rule exist to surface, and it has not been surfaced on this
issue. (Compare: sibling issue #363 does print its band-vs-sum gap explicitly; #333
has the data available via its own children and has not done the same arithmetic.)

**Recommendation:** print the 16-25 mw sum against the 10-18 mw band in a comment,
per the issue's own Completion Criteria ("Each of the four `planned_tasks` scopes has
been priced, and the sum printed against the 10-18 mw band").

### 4. `blocked_by` still names a closed-as-duplicate issue in the machine block itself

The one existing comment (2026-08-04) explains that `#348` closed as a duplicate of
`#169` and that the `blocked_by` edge should become `[318, 332, 169, 363]` — but adds
"the machine block is the maintainer's to edit," i.e. the YAML in the issue body is
*deliberately* left stale, on the theory that the comment is the authoritative
record. I fetched #348 directly and confirmed `state: closed, state_reason:
duplicate`. Whatever the project's convention, an engineer or tool that parses only
the YAML block (which several of this issue's own siblings instruct readers to treat
as authoritative — "should be cited as a fact rather than re-argued") will chase a
closed issue as a live blocker. Given finding #2 shows the YAML is *also* stale on
`planned_tasks`/`requires_tasks` with no comment excusing that lapse, the "the
comment is the source of truth" convention is not being applied consistently even by
the issue's own author, which undermines trusting it here either.

**Recommendation:** either fix the YAML directly (the comment's own suggested edit)
or establish and follow one consistent rule for which field is authoritative when
they disagree.

### 5. Criterion 4's "declared threshold" is unspecified, and its acceptance test is gameable as written

Criterion 4: "A design whose lookahead is too low for the discipline is refused by
name rather than run slowly and silently." Open Question 2 leaves the threshold
itself undecided ("derived, with a floor" — recommended but not fixed), and the child
task `TASK-C333-4` (#836) AC-3 only requires "the threshold is declared once, as a
named constant, with its basis recorded" — it does not require the basis to be
principled, only recorded. A trivially permissive threshold (e.g. a floor of 1 time
unit) would satisfy every stated acceptance criterion (AC-1..AC-5 of #836, and
criterion 4 of #333) while providing essentially no protection against the "mystery
slowdown" scenario criterion 4 exists to prevent — the refusal would just never fire
in practice. Nothing in #333 or #836 requires evidence that the chosen threshold
actually catches a real degenerate design.

**Recommendation:** add an acceptance criterion requiring a fixture whose lookahead
is deliberately near-zero (e.g. a boundary crossing a zero-delay combinational path)
to trigger the refusal, so the threshold is exercised by a case that would otherwise
silently serialize, not merely declared.

### 6. Criterion 1's "not recoverable from the result" is asserted, not falsifiable by the stated test alone

Criterion 1's stronger clause — "the partition count is not recoverable from the
result" — is formalized as $O \perp n$, and the issue itself flags (§5, prediction 3)
that "no child asserts this alone" and that each child could pass its own tests
"while leaking a partition id into a header." The mitigation is TASK-C333-5 (#838)
AC-3: "a structural check on the artefact format asserts no partition identifier,
count, or field derived from either appears in any output." A structural grep/schema
check catches an explicit field but not, e.g., partition-count-dependent floating
scheduling artifacts, timing jitter encoded in event ordering that happens to be
stable per-count but distinguishable statistically across many runs, or count
correlated with an incidental byte such as a buffer-size-dependent padding choice.
The issue's own criterion is an equality over *four* specific counts (1, 2, 4, 8);
"not recoverable" is a much stronger universal claim than "matches at these four
points," and nothing in the evidence plan distinguishes them. This is not a reason to
block the issue, but the wording overclaims relative to what its own acceptance test
can establish.

**Recommendation:** either narrow the completion criterion to "byte-identical at
counts 1, 2, 4, 8" (which is what's actually tested) or add a specific adversarial
test that tries to construct a partition-count side channel (e.g. via timing metadata
or ordering) rather than relying on a purely structural absence check.

### 7. `git grep` evidence commit `2d0ca9d` is unreachable in this checkout

Every quantitative claim in the issue is anchored to `evidence_commit:
2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`. That commit is not resolvable in this
checkout (`git log --all --oneline | grep 2d0ca9d` returns nothing; the tree here
has its own linear history topped by `5b05d67`, likely a fresh/detached history for
review purposes). I was able to independently confirm the two specific line-numbered
citations that matter most (`Transport.java:38`, `ArchitectureRulesTest.java:249`)
against the current tree, and both check out exactly. This is worth flagging as a
lower-severity provenance gap (the reviewer cannot literally replay the cited `git
grep` commands against the commit named), not as evidence the underlying claims are
wrong — spot-checks of the two load-bearing citations pass.

## What's solid (one line each)

- The core technical judgment — conservative barrier over optimistic/rollback,
  justified by the measured absence of cancel/withdraw/rollback in `src/jls/sim/` —
  is correct and independently verified: `grep -rniE "cancel|withdraw|rollback"
  src/jls/sim/` returns nothing in this tree either.
- Reusing `jls.collab.net.Transport`/`LoopbackTransport`/`ChaosTransport` rather than
  building a second networking stack is well-motivated and the architecture-rule
  citation (`ArchitectureRulesTest.java:249-263`) is accurate.
- The scope boundary against #332 (partitioning) and #363 (checkpoint mechanism) is
  clean and the comment's overlap adjudication with #332 is a genuinely useful
  disambiguation, not padding.
- `ChaosTransport`'s deterministic bounded-holdback design (confirmed at
  `test/jls/collab/net/ChaosTransport.java:19-23`) is a legitimately good fit for
  criterion 2 and is accurately described.
- The "no tolerance-based fallback for a digital simulation" stance is right for this
  domain and correctly rejects the tempting-but-wrong alternative.

## Verdict rationale

The design content is sound, but the issue as it currently stands misrepresents its
own state to anyone relying on it: it claims an unpriced, unfiled decomposition that
has in fact already been filed and priced (over budget), it cites a controlling
document that does not exist in the repository, and it carries a `blocked_by` entry
the author already knows is wrong. These are process defects in the issue's own
terms, not just style complaints — the issue's Completion Criteria explicitly demand
the roster and cost sum be kept current, and they are not. `needs-rework`: not because
the mechanism is wrong, but because the issue must be reconciled with the tracker's
actual state (and the missing document resolved) before it's safe to hand to an
implementer as written.
