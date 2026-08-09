# Issue #493: The evidence_commit every filed issue declares (2d0ca9d) is on a branch that will be deleted — read citations at master 8288226 instead, except in seven code files and 195 planning docs
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## What this issue actually is

Not a code-change request: it is a meta/process ledger asserting that the `evidence_commit`
(`2d0ca9d…`) baked into ~198 other filed issues (#295–#492) lives only on a doomed branch, and
telling readers how to re-derive citations against `master` (pinned here to `8288226…`). It has
already grown two long corrective comments in five days as new problems surfaced. Label:
`documentation`. There is no code diff to review — the review below is of the claims and of the
issue's own audit discipline, since that discipline is the entire deliverable.

## Verification performed

Using the checked-out repo (`git fetch --unshallow` was needed; the default clone here is shallow),
I independently re-ran the load-bearing measurements:

- `git merge-base --is-ancestor 8288226… origin/master` → **ANCESTOR** (confirmed reachable, current
  `origin/master` tip is now `c5cee1b`, later than `8288226` — consistent with time passing).
- `git diff --stat 2d0ca9d… 8288226… -- src/ test/ pom.xml .github/workflows/` → produces **exactly**
  the seven-file, `227` deletions / `1` insertion stat table quoted in §1. Confirmed accurate.
- `src/jls/hdl/HdlExporter.java:460` at `2d0ca9d` is indeed
  `private static final Map<Class<?>, String> REJECTED = Map.of(`; at `8288226` the `EXPORTED` /
  `SKIPPED` / `TOPOLOGY` sets sit at lines 422/431/436 as claimed, and there is no `REJECTED` bucket.
  Confirmed accurate.
- #489, cited as authority for "do not bulk-rewrite issue bodies," is real, open, and its findings
  (`issue_read` strips tag-shaped runs, `search_issues` is lossless) check out on inspection.

So the central technical claim — the seven-file rule and the master pin — is solid. The findings
below are about what is wrong *around* that solid core.

## Findings, most severe first

### 1. The header count in §5 does not match its own table (internal arithmetic contradiction)

> "**43** of the 198 filed issues cite at least one of the seven files with a line anchor or a
> branch-only symbol. Each has been commented individually."

The three buckets listed immediately under that sentence are: "Wrong about master" (29 issues
listed), "Anchor survives, line number shifts" (14 issues listed), "no action" (9 issues listed).
I counted every `#NNNN` token in each list: 29, 14, and 9, respectively — summing to **52**, not 43
(29+14=43 only if the 9 "no action" issues are silently excluded from the section's own declared
scope, "issues carrying an affected citation," despite being listed inside that same section).
Either the summary number is wrong, or the "no action" bucket doesn't belong under a heading that
promises "each has been commented individually" — in which case a reader can't tell from the issue
alone whether those 9 actually got a comment or not. **Recommendation:** state the count as
29 + 14 + 9 = 52 explicitly, or split the "no action" bucket into its own un-numbered aside, and
confirm (not assert) that all 52 — not just 43 — received the individual comment the prose promises.

### 2. The issue's own follow-up comment repeats the exact failure mode §3/§6 warn against

Comment 2 (`#issuecomment-5227507950`) §4 claims commit `3a81a4a` is "worse than the seven files of
§3 because it cannot be re-pinned at all," evidenced by:
```
$ git cat-file -t 3a81a4a7d6a0f108ec201e632732d308cc02b3fc
fatal: git cat-file: could not get object info
```
I reproduced that exact failure in a **shallow** clone of this repo, then ran `git fetch --unshallow`
and re-checked: `3a81a4a` resolves fine (`git cat-file -t` → `commit`), it sits on
`origin/claude/jls-virtual-hardware-linux-njsoma` — a branch that is still present on `origin` right
now, not deleted — and `git show 3a81a4a:docs/plan/evidence/BRIEF.md` returns the full file. The
"unrecoverable" classification is not a property of the commit; it's an artifact of not fetching the
branch, in a repo the comment itself was written to warn is prone to exactly this ("a working clone
… has a stale local `master` ref … which is how a branch-only commit acquires the label 'master' in
a comment that is otherwise careful"). The comment's own prescribed check (`git rev-parse
origin/master` + `merge-base --is-ancestor`) was not run against `3a81a4a` before declaring it dead;
had it been, the branch-existence check would have caught this. This matters because the
"unrecoverable measurement" framing is then used to invalidate #475's falsification-gate claim on
#232 — a substantive downstream conclusion resting on a premise ("nobody can open it") that a plain
`git fetch` disproves. **Recommendation:** re-verify #232's H1/H3 gate status against the actually
fetchable `3a81a4a` tree before treating it as closed/open on the strength of this issue's comment.

### 3. Unbounded, self-perpetuating scope with no termination condition

Filed 2026-08-03 declaring one dead pin (`2d0ca9d`) and one canonical replacement (`8288226`).
By 2026-08-08 (5 days later) it had grown a second commit found to be equally dead (`07a0bea`, on
another to-be-deleted review branch), a third label found to be merely non-canonical-but-fine
(`29afb26`), and a fourth commit (`3a81a4a`) reclassified as unrecoverable (see finding 2). Each
discovery triggers "commented individually" on another 10, then more, downstream issues. Nothing in
the issue proposes a systemic fix (a CI check that rejects a non-`origin/master`-ancestor commit hash
in a new issue body, a bot that canonicalizes `evidence_commit` fields, or simply stopping the
practice of citing ephemeral working-branch commits from filed issues going forward). As written,
this is a standing invitation for a fifth, sixth, seventh dead pin to surface, each requiring another
"RULING" comment and another wave of per-issue comments across a corpus that is already at 198+
issues. **Recommendation:** either close the loop with a mechanical safeguard (e.g., a pre-flight
`merge-base --is-ancestor … origin/master` check documented as mandatory before an issue is filed
with an `evidence_commit`), or scope this issue explicitly to "corrections as of 2026-08-08" and file
a fresh, separate issue for each future recurrence rather than open-endedly amending this one.

### 4. The "durable replacement pin" is itself hosted on the fragile medium it complains about

The issue's entire thesis is that citations into ephemeral git branches rot. Its own proposed fix is
to re-home the authoritative pin into … a GitHub issue's body and comment thread, editable, subject to
the very `issue_read`-corruption bug documented in #489 (which this issue explicitly invokes and
works around by refusing to bulk-rewrite bodies), and with no independent backup if the issue were
ever closed, locked, or (per #489) accidentally corrupted by a future rewrite. A short paragraph in
`ARCHITECTURE.md` or `CONTRIBUTING.md` recording "canonical evidence commit: `8288226`, superseding
`2d0ca9d`" would survive issue-tracker churn and git-history rewrites in the same way the rest of the
repo's normative docs are described (README: "Repo documents … are the normative home for
contracts"). **Recommendation:** promote the §1 table (dead commit → surviving commit) into a repo
file, and have this issue merely point at it.

### 5. Downstream, unverifiable "77 of 198" / "80 of 198" claims

> "77 of the 198 filed issues cite at least one of the 195 branch-only planning documents … and 80
> cite `ARCHITECTURE.md` or `docs/file-format.md`."

No script, query, or artifact is attached; these are bare assertions layered on top of the 43-vs-52
discrepancy in finding 1, which was itself in the same section's arithmetic. Given finding 1 shows a
counting error already happened once in this issue, these two unverified counts carry a lower prior
than the seven-file/master-ancestry claims that I could and did check independently.
**Recommendation:** attach the query or script used to produce these counts (even a one-line
`grep -rl` over exported issue bodies) so a future auditor can re-run it rather than re-trust it.

## What is solid (no further action needed)

- The seven-file / 227-line diff-stat table (§1): verified byte-for-byte against the repo.
- `8288226` is a genuine ancestor of current `origin/master`. `2d0ca9d` resolves as a real commit
  once the shallow clone is unshallowed, and `git branch -a --contains` places it only on
  `origin/claude/jls-virtual-hardware-linux-njsoma` — not on `master`, and not an ancestor of
  `origin/master` per `merge-base --is-ancestor`. The "branch-only, not on master" characterization
  is correct; only the "will 404 once deleted" framing needs the caveat in finding 2 (dead-branch
  claims in this thread have not held up under a fetch-first check).
- The `HdlExporter.java` `REJECTED`/`EXPORTED`/`SKIPPED`/`TOPOLOGY` line-number claims (§3): verified.
- The `29afb26` ancestry claim (§5 and comment 2 §2): verified — it is an ancestor of `8288226`,
  differing only in three `.github/ISSUE_TEMPLATE/*` files.
- The citation of #489 as the reason not to do full-body rewrites: #489 is real, open, and its
  findings are independently corroborated by its own before/after table.

## Bottom line

The core measurement this issue exists to deliver (the dead pin, the live replacement, the seven
files that are the sharp edge) is accurate and independently reproducible from the repo as checked
out. What undermines confidence is the audit apparatus around that core: a self-inconsistent count in
§5, a follow-up "RULING" comment that reproduces — in the act of correcting a citation-hygiene bug —
the same shallow-clone/branch-existence mistake it is warning readers to stop making, and an
open-ended scope that has already required two rounds of tracker-wide follow-up comments with no
proposed stopping condition. None of this invalidates the seven-file/master-ancestry pin itself, but
anyone relying on this issue's downstream classifications (which of the 52-or-43 issues got fixed,
whether #232's gates are actually still open) should re-verify rather than trust the count as given.
