# Issue #820: TASK-C567-1: the issue chooser offers a plain bug report and a plain feature request, and a stranger fills either one without learning what a tier is
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#820 is the executable task that turns FEAT-C30-1 (#567) AC-1/AC-2/AC-4
into a diff: two plain issue templates plus a live demo filing. The band
math checks out (#820 0.25-0.5 mw + #821 0.25-0.5 mw = #567's 0.5-1 mw,
and #821 correctly owns #567's AC-3/CONTRIBUTING half — a sensible split).
But the task inherits, and in one respect worsens, a problem already
flagged on its parent: it re-adds a template pair that was deliberately
removed by a named commit, without a word of acknowledgment. On top of
that it carries its own execution-level risks: a gameable demo-filing
criterion and a live tension with the repo's documented attachment-based
security threat.

## Findings, most severe first

### 1. [Critical] The task silently reverses a recorded decision it never names

`.github/ISSUE_TEMPLATE/` today holds exactly `capstone.md`, `feature.md`,
`scientific_task.md` — confirmed by directory listing. Per the adversarial
review of #820's parent (`/home/user/JLS/issue-reviews/issue-0567.adversarial.md`,
finding 1, itself grounded in `git show 1fc40be`), that state is not an
accident: commit `1fc40be` (2026-07-21) deliberately deleted
`bug_report.md` and `feature_request.md` with the message "Remove the
bug_report and feature_request issue templates. Keep only the
scientific-task template, so every issue is filed with a falsifiable
hypothesis and evidence-backed completion criteria." #567 at least
gestures at prior art (inaccurately — it cites #69 as the only relevant
history and omits the later removal). #820's body doesn't mention #69,
`1fc40be`, or any history at all; AC-1 just says the templates "appear in
the chooser alongside the planning-corpus templates, which are unchanged,"
as if this were greenfield work. Whoever picks up #820 will not learn from
the task itself that they are reversing a three-week-old, reasoned,
committed project decision — they'd need to independently discover
`1fc40be`. `feature.md` rule C requires plan reversals to carry a
`REPLAN:` comment stating what changed and why; nothing analogous exists
here, and #820 is where the reversal actually lands in the tree.

**Recommendation:** add a line to #820 citing `1fc40be` and stating why
the single-template policy is being narrowed (e.g., "the hypothesis-
required policy serves the planning corpus; drive-by reports are a
different audience per #508 §5") before any implementer starts.

### 2. [High] AC-4's demo filing is gameable and leaves an undefined artifact behind

"One report is filed end to end through the bug template and linked from
#567... the resulting issue is actionable without a follow-up round of
questions." Nothing constrains who files it — the same person who wrote
the template can file the demo report themselves, immediately satisfying
the letter of AC-4 while proving nothing about whether a genuine outsider
finds the form usable (this mirrors, at the execution layer, finding 2 of
the #567 adversarial review). Worse, the criterion doesn't say what the
demo issue *is*: if it reports a real bug, it becomes a permanent tracker
entry with no stated disposition (fix it? leave it open indefinitely as a
"demo"?); if it's fabricated to have something to file, the demonstration
is dishonest by construction and the "actionable without follow-up
questions" bar is trivially gameable — a self-authored report will of
course be actionable to its own author.

**Recommendation:** specify the demo report must be a genuine,
previously-unfiled, reproducible defect, filed and evaluated by someone
other than the template's author, with an explicit note on what happens
to the resulting issue afterward (triage into the normal backlog, not
left as a demo fixture).

### 3. [High] AC-2's zero-friction pressure risks re-dropping a security control the deleted template carried

The Outcome text describes the bug template asking for "what happened,
what was expected, version, platform, and a file or steps" — inviting a
stranger to attach a file. `SECURITY.md:3-37` documents an active,
named social-engineering campaign using malicious zip attachments
disguised as patches on issues/PRs, and `CONTRIBUTING.md:23-24` states
plainly: "attachments on issues/comments are never applied — do not ask
maintainers to extract archives." Per the #567 review (finding 4), the
original, deleted `bug_report.md` explicitly warned "paste circuit text
inline or link a gist — per SECURITY.md, maintainers do not open attached
archives." #820's AC-2 ("no field... requires knowledge of tier/ID/band
conventions... every required field can be answered by someone who has
only used the program") says nothing about preserving that warning, and
an implementer optimizing purely for AC-2's five-minute/no-jargon bar
could legitimately treat the SECURITY.md caveat as friction and drop it —
silently regressing a control the project has already had to state twice
elsewhere in the tree.

**Recommendation:** add an explicit AC requiring the bug template to
restate or link the no-attachments-opened rule inline (e.g., "paste
circuit text or link a gist — do not attach files").

### 4. [Medium] AC-2 and AC-3 are unfalsifiable as written, unlike AC-5

AC-5 is mechanically checkable (YAML renders in the chooser, labels
exist — confirmed here: the `bug` label already exists in the repo via
`get_label`, so that half is fine). AC-2 ("no field... requires
knowledge... every required field can be answered by someone who has
only used the program") and AC-3 ("ordering and descriptions make the
intended audience obvious at a glance") have no stated judge, test, or
proxy. A grep of `test/` for `ISSUE_TEMPLATE` returns nothing — there is
no automated completeness/jargon check analogous to `HelpTopicsTest`'s
palette-coverage test. As written, an implementer can self-certify AC-2/
AC-3 by eyeballing their own draft, exactly the failure mode `docs/`-tier
issues in this repo are usually built to resist.

**Recommendation:** tie AC-2 to an observable proxy (fixed max field
count, or a completion check by someone who hasn't read CONTRIBUTING),
and AC-3 to a concrete rule (e.g., the human templates must sort before
the planning templates in `config.yml`, plus distinct `name:` prefixes).

### 5. [Medium] The new "Feature request" template collides with the existing "Feature" template in the same chooser

`.github/ISSUE_TEMPLATE/feature.md:2` already declares `name: Feature`
(the tier:feature planning template, 212 lines with machine-block YAML).
AC-1's new feature-request template will sit in the same chooser list. A
drive-by human — the exact person AC-3 is trying to route correctly —
sees "Feature" and "Feature request" side by side with only the `about:`
one-liner to disambiguate, and no `config.yml` exists in the repo today
to set chooser ordering or descriptions (confirmed: directory listing of
`.github/ISSUE_TEMPLATE/` has no `config.yml`). Picking wrong lands the
person in the 212-line scientific-corpus feature template — precisely
what AC-2/AC-3 are meant to prevent. Neither AC-1 nor AC-3 names this
specific collision or requires a `config.yml`.

**Recommendation:** either rename the planning template's `name:` (e.g.
"Feature (planning corpus)") or add `config.yml` with explicit ordering/
descriptions, and make it part of AC-3's text rather than leaving it to
implementer judgment.

### 6. [Low] The AC split across #820/#821 isn't recorded on the parent

#567 has a single comment (a dedup boundary note against #571); nothing
on #567 documents that its AC-1/AC-2/AC-4 landed in #820 while AC-3
landed in #821. `feature.md` rule D asks child tasks to mirror `STATUS:`/
plan state back onto the parent; a REPLAN-style note recording the split
would let a reader of #567 alone understand the decomposition without
finding both child issues independently. Not a defect in #820's own
scope, but a traceability gap #820 could close cheaply by cross-linking
#821 in its Related/Notes.

**Recommendation:** add a one-line cross-reference to #821 in #820 (and
vice versa) so the split is discoverable from either issue.

## What's solid

- **Band arithmetic is consistent**: #820 (0.25-0.5 mw) + #821 (0.25-0.5
  mw) sums to #567's stated 0.5-1 mw band — the decomposition isn't
  silently inflating or shrinking scope.
- **The task/feature split itself is sensible**: #820 owns the templates
  and demo filing; #821 separately owns the CONTRIBUTING quickstart
  (#567's AC-3) with its own CI doc-test enforcement — a cleaner
  separation of concerns than doing both in one task.
- **AC-1's "alongside, not replacing" framing** is unambiguous about
  scope: the planning-corpus templates are explicitly out of scope for
  modification.
- **AC-5's label-existence check is real and already passes**: `bug` and
  `enhancement` labels both exist in the repo today (confirmed via
  `get_label`), so that half of AC-5 needs no new label creation.

## Note on scope

This review covers #820 only. #567 (the parent feature) and #821 (the
sibling task) carry their own, partially overlapping findings — see
`/home/user/JLS/issue-reviews/issue-0567.adversarial.md` for the
feature-level history/gaming/chooser-collision analysis this review
draws on and applies at the task's execution layer.
