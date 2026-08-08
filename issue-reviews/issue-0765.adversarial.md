# Issue #765: TASK-C577-3: content licensing is settled in writing before any adapted CSE 260M material ships, and the adapted kit conforms to the convention and runs the cohort workflow
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #765 (open, no comments) plus its cited context: parent #577 (with both
comments), #578, #576, and #509 (with its one comment). Grepped the checked-out
tree for any existing trace of this work (`siever`, `260M`, `bsiever`, `kit`,
`fixture`) — nothing exists yet: `test/fixtures/` holds only `legacy-4.1`,
`riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`, and
`headless-canary-gate.jls`; there is no course-kit convention, validator, or
distribute/collect tooling anywhere in `src/` or `test/`.

## Findings, most severe first

### 1. The issue's own `ordering_after` is stale relative to the maintainer's own correction on #577 — a confirmed missing dependency

#765 declares `ordering_after: ["TASK-C577-2", 578, 576]` — no mention of #509.
But #577's second comment (2026-08-08, same day as this issue was filed) is an
explicit "ORDERING CORRECTION" that resolves a dependency cycle between #577
and #509 and states the corrected global order in the maintainer's own words:

> "**#577 AC-1/AC-2 → #509 AC-3 → #509 AC-1/AC-4 → #577 AC-3/AC-4.**"

and gives the per-criterion table showing #577's AC-3 (content licensing) and
AC-4 (the adapted kit) both need #509. #765 is explicitly the task-level split
of exactly that AC-3/AC-4 slice — its own title is "content licensing... and
the adapted kit conforms to the convention and runs the cohort workflow," word
for word #577 AC-3/AC-4. Yet #765's machine block never lists #509 as a
dependency. Either #765 was filed before the correction landed and was never
reconciled, or the correction was intentionally not propagated down — either
way the task as written can be picked up and "started" without anyone
noticing it is blocked on #509 AC-1 (Siever confirming criteria) and AC-4 ("one
course offering runs on this fork's releases" — an entire semester's live
pilot). That is a real, evidenced planning defect, not a nitpick: the same
kind of silent cycle/gap the maintainer already had to correct once on the
parent issue has reappeared one level down.

**Recommendation:** add `509` to `ordering_after` (or an explicit
`ordering_after_by_criterion` block mirroring #577's), and state plainly
whether AC-1 here can start before #509 AC-4 (full course offering) completes,
or only after.

### 2. AC-1 is not an engineering deliverable — it is a real-world legal negotiation with an external, non-project party, and the issue offers no process, contact protocol, or timeout

> "A written content-licensing agreement with Dr. Siever is recorded before
> any adapted material ships; absent it, the kit half is explicitly held..."

Nothing in this repository, and no amount of code review or CI, can make Dr.
Siever sign an agreement. #509's own comment describes the relationship as
"Dr. Siever has been contacted and is interested in this fork **if it becomes
well enough matured**" — a conditional, non-committal statement, not an
active negotiation. #765 treats "settle content licensing in writing" as a
checkbox task alongside CI-testable engineering criteria (AC-2, AC-3) with no
owner, no channel, no deadline, and no fallback process if Siever is
unresponsive or WashU's institutional counsel needs to be involved (see
finding 4). Filing this as a `tier:task` with `band_mw: 0.5-1` implies it is
schedulable engineering work; the actual critical path is someone else's
calendar and institutional process, entirely outside the maintainer's
control.

**Recommendation:** split the licensing-outreach action (send the agreement,
track response) from the engineering integration work, and do not carry a
maintainer-week estimate on the part that isn't the maintainer's to schedule.

### 3. AC-1's fallback is trivially gameable — "held" and "shipped" cost the same one sentence

> "Absent that agreement the fixtures stay fixtures and the kit is held,
> which is a legitimate outcome recorded rather than a blocker worked
> around."

This is honest in intent (no incentive to launder unlicensed content — see
"solid" list below), but as written it makes the acceptance criterion
satisfiable by doing nothing: recording "no agreement yet, kit held" in one
line closes AC-1 exactly as completely as actually landing a signed
agreement. Nothing in the AC distinguishes "we tried and it's genuinely
pending" from "we never asked." Since AC-2/AC-3/AC-4 (the substantive kit
work) are downstream of AC-1 succeeding, and AC-1 can always be satisfied by
the negative branch, the entire issue can be closed as "complete" with zero
kit shipped and zero validation performed. A reviewer checking the box
"AC-1 done" learns nothing about whether real progress happened.

**Recommendation:** require the "held" outcome to record what was actually
attempted (date of outreach, channel, response received/not) rather than
accepting a bare assertion, and don't let AC-1's completion (in either
branch) count toward closing the issue if AC-2–AC-4 are still open — split
"licensing status recorded" from "issue resolved."

### 4. Hidden assumption: Dr. Siever personally has authority to license the material

The issue and its parents assume a single named instructor's signature
settles "content licensing." #509 states the material is WashU's *course*
(CSE 260M), taught by Siever using labs that (per #577/#578's cross-reference
to "the Donzellini-mapped pack") are themselves apparently built against "the
standard text" — i.e., there may be a third-party textbook's IP baked into
the lab content, and the labs are university course material that a
university's tech-transfer/legal office, not an individual faculty member,
often actually controls. Nothing in #765 asks whether Siever is the rights
holder, whether WashU needs to countersign, or whether any lab derives from
copyrighted textbook problems that a personal agreement with Siever cannot
clear.

**Recommendation:** AC-1 should require the agreement to state, or the issue
to record separately, who the material's actual rights holder is (Siever
personally vs. WashU) before treating "Siever's written agreement" as
sufficient.

### 5. AC-2 and the "kit end-to-end" AC depend on two feature-tier issues that do not exist in the tree yet, and one of them (#578) doesn't even have its own convention written

> "The adapted kit validates against #578's kit convention with no
> exceptions (AC-4)."

#578 (band_mw 3-4) is itself an open, unimplemented feature: "the kit layout
and metadata are specified in tree... A validator checks a kit directory
against the convention" — none of that exists yet (confirmed by repo grep).
Likewise #576 (band_mw 2-3, the distribute→collect→grade workflow) is
unimplemented. #765 treats validating "with no exceptions" against a
not-yet-specified convention as a testable acceptance criterion, but the
convention's shape — and therefore what "no exceptions" even means — is not
fixed. If #578 ships a loose validator, #765 can trivially pass against a
weak bar; if #578's design changes after #765 is scoped, #765's AC-2 changes
meaning out from under it with no mechanism in #765 to notice.

**Recommendation:** either block #765 from being worked at all until #578 and
#576 have shipped a stable convention/tool (which the `ordering_after` field
nominally already does), or explicitly note in #765 that its AC-2/AC-3 wording
is provisional pending #578's/#576's actual shape, so a future editor doesn't
treat the current phrasing as frozen.

### 6. AC-1's corpus itself does not exist yet either — the "adapted material" this task licenses hasn't landed

#765 talks about "adapted CSE 260M material" and "the kit," but the raw
corpus this is adapted *from* is TASK-C577-1 (#761, "the CSE 260M corpus
lands in tree as committed compatibility fixtures") and TASK-C577-2 (#763,
the CI grading lane) — both open, both unimplemented (confirmed: no
`260M`/`siever` hits anywhere in `test/` or `src/`). #765 lists TASK-C577-2 in
`ordering_after`, which is correct and good, but the acceptance criteria are
written in the present/near-future tense ("the adapted kit conforms...",
"the kit is walked end to end...") as though the fixture corpus were a
settled input. This is consistent with the stated ordering, so it is a minor
point rather than a contradiction — flagged only because a task filed this far
ahead of its own inputs existing invites premature engineering work if picked
up before #761/#763 land.

### 7. "Content licensing here is kept distinct from code licensing, and both are stated" is unverifiable as written

No artifact, location, or format is specified — not a LICENSE file, not a
header convention, not a docs page. "Both are stated" could be satisfied by a
single ambiguous sentence in a README, which would technically close the
criterion while leaving real redistribution questions (can another instructor
reuse WashU-derived material commercially? under what terms?) unanswered.
This matters more than a typical vague-AC nitpick because content-licensing
ambiguity is exactly the hazard AC-1 through AC-4 exist to prevent.

**Recommendation:** name where the statement must live (e.g., a
`LICENSE-CONTENT` file or a stated field in #578's kit-metadata convention)
and require it to name the license by SPDX-style identifier or equivalent,
not just prose.

## What's solid

- The "held, not worked around" framing for a missing licensing agreement is
  the right instinct — it explicitly rejects the temptation to ship
  unlicensed derivative course material under schedule pressure, and #577's
  comment thread shows this was deliberately preserved through a round-2
  ordering correction rather than dropped.
- Separating content licensing from code licensing is correct given the repo
  is GPL-3.0-or-later (`README.md` "License and provenance") and course
  material licensing is an orthogonal question; keeping them explicitly
  distinct avoids a real category-confusion hazard.
- Scoping discipline is good: the Boundary section correctly pushes the
  adoption relationship, maturity criteria, fork-delta audit and migration to
  #509 rather than re-litigating them here, matching #577's own boundary
  note.

## Verdict rationale

The issue's honest fallback and clean scope boundary are real strengths, but
the acceptance criteria as filed have a confirmed stale dependency (finding
1, directly contradicted by the maintainer's own same-day correction on the
parent issue), a core criterion (AC-1) that is not engineering-verifiable and
is trivially gameable in its negative branch (findings 2–3), and two more
criteria (AC-2/AC-4-equivalent) that reference a convention and workflow that
do not exist yet and whose eventual shape is unknown (finding 5). None of
this means the underlying goal is wrong — it means the issue needs another
editing pass before anyone should pick it up: reconcile the ordering, tighten
AC-1 into something an audit trail can verify, and either wait for #578/#576
to stabilize or mark #765's AC-2 wording provisional.
