# Issue #567: FEAT-C30-1: a drive-by human files a usable bug report in five minutes — plain issue templates and a ten-line quickstart sit above the contract prose
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is open, well-scoped on its face, and cheap. But it rests on a
factual claim in its own Notes section — "this feature does not reopen or
rewrite [#69's] work" — that the repository's git history directly
contradicts. That is a real problem, not a nitpick: the exact pair of
templates this issue proposes to add already existed and was deliberately,
reasoned-ly removed three weeks before this issue was filed. Below that,
AC-4 is gameable in a way that would let the issue claim it validated the
capstone's real target while proving nothing of the kind, and there are a
handful of smaller specification gaps.

## Findings, most severe first

### 1. [Critical] The issue misrepresents its own prior art — it reverses a deliberate, reasoned decision without acknowledging or rebutting it

`git log --diff-filter=D -- .github/ISSUE_TEMPLATE/` shows commit
`1fc40be` (2026-07-21, three weeks before #567 was filed):

> "Remove the bug_report and feature_request issue templates. Keep only
> the scientific-task template, so every issue is filed with a
> falsifiable hypothesis and evidence-backed completion criteria."

That commit deleted `.github/ISSUE_TEMPLATE/bug_report.md` and
`feature_request.md` — a plain bug template and a plain feature-request
template, functionally the exact pair AC-1 asks for again. #567's Notes
section says:

> "Prior art: #69 (closed) shipped the original community-health files
> and templates. This feature does not reopen or rewrite that work; it
> adds the human-plain pair alongside the planning-corpus templates..."

This is false as stated: #69 shipped the plain pair, `1fc40be` deliberately
un-shipped it in favor of a single-template, hypothesis-required policy,
and #567 now re-proposes precisely what `1fc40be` removed — without citing
that commit, without stating why the "every issue needs a falsifiable
hypothesis" rationale no longer applies to drive-by bug reports, and
without the REPLAN-style decision note the project's own template
conventions require for reversing a recorded plan choice (`feature.md`
rule C: "Plan changes ... are edited only together with a `REPLAN:`
comment stating what changed and why"). A maintainer picking this up would
have no way to know, from the issue text alone, that they are reversing
their own July decision rather than doing something novel.

**Recommendation:** amend the Notes section to name `1fc40be` explicitly
and state the argument for reversing it (e.g., "the hypothesis-required
policy serves the planning corpus, not drive-by reports; splitting the two
audiences resolves the tension #508 §5 named" — which is roughly the
actual argument, but the issue never makes it against the commit it is
overriding).

### 2. [High] AC-4 is satisfiable by internal theater and doesn't establish what it claims to establish

Capstone #514's real AC-1 is: "Both templates live; **a drive-by human
bug report arrives through one** without reading any planning template" —
i.e., a genuine outside filer. #567's AC-4 waters this down to "one
drive-by-style bug report filed through the template end-to-end" with no
constraint on who files it. As written, the implementer (very likely the
same agent or maintainer who just wrote the template) can file the demo
issue themselves, close AC-4, and the issue reads as if it "demonstrated
the Capstone AC-1 path" — while proving nothing about whether an actual
unfamiliar outsider finds the template usable. This is exactly the
gameable-acceptance-criterion pattern: the stated verification (a filed
issue exists) can pass while the real goal (a genuine drive-by human could
use this) fails silently.

**Recommendation:** either drop AC-4 from this feature-level issue
(genuine external arrival is #514's job to verify, over a rolling quarter,
not a single self-administered dry run at feature-close), or rewrite it to
require a filer who is not the PR author/maintainer and say so explicitly
in the closing evidence.

### 3. [Medium] Chooser-level naming collision undermines AC-2's own goal before a single field is read

`.github/ISSUE_TEMPLATE/feature.md:2` already declares `name: Feature`
(the planning-corpus tier:feature template). AC-1's new "feature-request"
template will sit in the same chooser. A drive-by human — precisely the
person AC-2 says should need zero tracker-convention knowledge — will see
both "Feature" and "Feature request" in the same list with only the
`about:` one-liner to tell them apart. That is a worse trap than any
individual form field: picking wrong here lands the person in the
373/212-line scientific-task/feature templates AC-1 is explicitly trying
to keep them out of. Neither AC-1 nor AC-2 says anything about
chooser-level disambiguation (there is no `.github/ISSUE_TEMPLATE/config.yml`
in the repo today to set ordering or a blank-issue link either).

**Recommendation:** add an AC covering the chooser presentation itself —
either a `config.yml` with explicit descriptions/ordering, or renaming the
planning template's `name:` (e.g., "Feature (planning)") so the two never
compete on a bare label.

### 4. [Medium] Silent regression of a security control the deleted template carried

The removed `bug_report.md` (recovered via `git show 1fc40be`) included:
"paste circuit text inline or link a gist — per SECURITY.md, maintainers
do not open attached archives." `SECURITY.md:3-37` documents an active,
named social-engineering campaign using malicious zip attachments on
issues/PRs, and `CONTRIBUTING.md:23-24` repeats the same rule. #567's AC-2
("no field demands knowledge of the tracker's tier/ID conventions") says
nothing about preserving this warning, and a template author optimizing
purely for "five minutes, no jargon" could legitimately drop it as
friction — silently regressing a control #69 deliberately put in the
contributor's direct path.

**Recommendation:** add an explicit AC that the new bug template restates
or links the no-attachments rule inline, matching the precedent it is
replacing.

### 5. [Medium] Possible conflict with the maintainer's own just-stated planning ratchet

#508 (filed 2026-08-03, one day before #567, same author, OWNER) lists
under "Process findings (act on these)": "Planning ratchet: no new
tier:feature/tier:task until two capstones close." #567 is filed the next
day carrying label `tier:feature`. The issue doesn't state whether two
capstones closed in the interim or why this filing is exempt (e.g., as a
PF-slice of an already-open capstone rather than a freestanding new
feature — a defensible reading, but the issue never makes it).

**Recommendation:** state the ratchet-exemption rationale in the issue
body, or fold this AC set directly into #514 rather than carrying it as a
separate tier:feature issue.

### 6. [Low] AC-2 has no falsification procedure

"No field demands knowledge of the tracker's tier/ID conventions" is a
negative, subjective claim with no stated judge or test. It can be
satisfied by the author's own read-through while a genuine newcomer still
trips on domain jargon the templates would inevitably carry (e.g.
"propagation delay", "golden", "FORMAT header") that isn't a *tier/ID*
convention but is exactly the kind of insider language AC-2 is trying to
screen out. #69's own falsification design was more concrete: "P2: the
next externally-filed issue arrives with repro steps and version info
because the form asked." #567 doesn't reuse or replace that mechanism.

**Recommendation:** tie AC-2 to an observable proxy — a fixed max field
count, or a completion check by someone who hasn't read CONTRIBUTING —
rather than leaving it to author self-assessment.

### 7. [Low] AC-3's premise overstates the current gap

`CONTRIBUTING.md:1-15` already opens with a `## Getting started` block
containing the build command (`mvn verify`, covering build+test) and
`java -jar` *before* any contract prose — contrary to the Outcome
section's framing ("the first screen ... is an invitation rather than an
internal monologue," implying none exists today). The actual gap is
narrower: no `git clone` line, and no mention of opening a PR, in the
first ten lines.

**Recommendation:** scope AC-3 to the real delta (add clone + PR lines to
the existing quickstart block) rather than implying a full restructure.

## What's solid

- **AC-1's "alongside, not replacing" framing** is unambiguous and
  correctly scoped — it doesn't ask to touch the planning-corpus
  templates.
- **Cost estimate (0.5–1 mw) is plausible and probably conservative**:
  the two templates being proposed already exist verbatim in git history
  (`git show 1fc40be`) and can be restored/adapted rather than authored
  from scratch, which the issue doesn't mention but which works in its
  favor.
- **Capstone linkage** (`serves_capstones: CAP-30 #514 (PF-1)`) is
  correctly cited and matches #514's own PF-1 description word-for-word.
- **Atomic scope**: templates + a CONTRIBUTING edit is a genuinely
  single-PR-sized unit, not scope creep.
