# Issue #868: TASK-C590-3: one release actually goes out under the checklist, with the writeup and the posting links in its release notes — a checklist nobody has run proves nothing
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

TASK-C590-3 is the third of three tasks under FEAT-C36-3 (#590), itself
under capstone CAP-36 (#520). The chain is: #866 publishes a "positioning
paragraph" in the README and site about page; #867 writes an in-tree
release-announcement checklist; #868 (this issue) requires actually
*executing* that checklist for a real release — writeup, demonstrable
artifact, competitor citations re-verified and dated, posting links
recorded in the release notes — and fixing the checklist wherever it
proved wrong. Confirmed against the checkout: neither #866's paragraph
nor #867's `docs/release/announcement-checklist.md` exists yet
(`grep -n -i "maintained successor\|Digital tradition" README.md` and
`src/jls/About.java` both empty; `docs/release/` does not exist). This
issue is, correctly, not startable yet — see Finding 1.

## Findings

**1. (High) The task is unstartable as filed and nothing in the repo enforces the ordering it depends on.**
The YAML header states `ordering_after: ["TASK-C590-1 (the statement)",
"TASK-C590-2 (the checklist)"]` — i.e. #866 and #867. AC-1 requires "a
release goes out under the checklist," but there is no checklist in-tree
to run (`docs/release/announcement-checklist.md` does not exist) and no
published positioning statement for the writeup to point back to. This
ordering is prose-only: nothing (label, GitHub "blocked by" relationship,
CI check) prevents someone from picking up #868 first and shipping a
"release announcement" that satisfies AC-1's letter with no checklist and
no positioning paragraph behind it. Recommendation: add an explicit
GitHub blocking relationship (sub-issue/blocked-by) to #866 and #867, not
just an `ordering_after` YAML comment, and have this issue's AC-1 name
the checklist file path so compliance can be checked against a concrete
artifact rather than a claim.

**2. (High) AC-3's re-verification burden is open-ended and will recur every time this task's "spirit" is invoked again.**
"Every competitor comparison in the writeup is cited to the competitor's
own tracker and re-checked against their current release at posting
time, with the check dated." The evidentiary base this points at is
#510's teardown (Logisim-Evolution, Digital, CircuitVerse, Falstad,
Issie/DigitalJS, plus a small-competitor group), which cites specific
issue numbers on five-plus external trackers (e.g. Logisim-Evolution
#786/#1546/#1871/#2454, Digital #151/#1477/#84/#882/#1464/#1470,
CircuitVerse #1412/#5328/#349/#34, Falstad #400/#134/#364). None of
these are owned by this project; any could close, get relabeled, or be
fixed between #510's survey date (2026-08-04) and whenever a release
actually ships. #520's own kill criterion (KC-36-1) says a stale
comparison "retracts, not defends" — but #868 has no mechanism (link
checker, dated-snapshot requirement, or review gate) forcing that
retraction to actually happen before the release goes out; it relies on
the same person who wrote the comparison to also catch that it went
stale. Recommendation: require the writeup's competitor citations to
list a fetch/check timestamp inline (AC-3 half-does this — "with the
check dated" — but doesn't specify a machine-checkable format or who/what
verifies the date is current at posting time versus retroactively
back-dated).

**3. (Medium) AC-1/AC-2/AC-4 are entirely self-attested with no independent verification, which is a sharp contrast with the rest of this codebase's culture.**
ARCHITECTURE.md documents a project that ratchets almost everything
through automated tests (`HeadlessCoreRatchetTest`,
`ElementConstructorContractTest`, `SaveTagsTest`, `NotificationRatchetTest`,
`FormatHeaderTest`, etc.) specifically so claims can't regress silently.
This task's acceptance criteria have no analogous check: "the writeup
carries a demonstrable artifact" (AC-2), "every point where the checklist
proved wrong... is fixed" (AC-4), and "the checklist is exercised" (AC-1)
are all judged by the same single maintainer who executes them, writing
their own release notes as the record of success. The issue's own title
argues "a checklist nobody has run proves nothing" — but a checklist run
and graded by the same person only one notch above that bar. Recommendation:
name a concrete, checkable artifact per AC (e.g. AC-1 could require the
release-notes URL and the checklist file's git SHA at time of use to be
cross-linked; AC-2's "demonstrable artifact" could require a link a
reviewer can click, which the writeup should already have, making the
proof-of-satisfaction identical to the deliverable — reasonable, but say
so explicitly in the AC).

**4. (Medium) Cost/feasibility: this cannot be scheduled — it depends on an external, undated "flare moment."**
#590's body ties execution to "a genuine flare moment... the Linux-boot
demo, #508 item 4." #868's own `band_mw: "0.25-0.5"` prices only the
writing/posting labor, not the wait for that demo to exist and work, nor
the multi-tracker citation audit in Finding 2, nor AC-4's open-ended
"fix every checklist gap" work. Compare to the parent capstone #520,
which prices the whole PF-3 slice ("Positioning and announcements") at
0.5-1 mw — #868 alone claims a similar order of magnitude for just the
execution leg. This is optimistic even before accounting for the
single-maintainer bus-factor risk #510 itself documents ("zero external
PR throughput"): all of writing the demo, the writeup, the citation
audit, and the checklist fixes lands on one person who is simultaneously
carrying the engineering backlog. Recommendation: re-band with the wait
time and audit cost included, or explicitly scope AC-1 to piggyback on
the next release regardless of whether it's a genuine "flare moment,"
removing the dependency on an unscheduled event.

**5. (Low) Terminology collision with #520 PF-4's "venue."**
#867 AC-1 and #868's outcome both use "venues" to mean promotion channels
(Reddit, forums, mailing lists) that "this niche actually reads." #520
PF-4 uses "venue" for a peer-reviewed academic venue (SIGCSE/WCAE/ASEE).
Both trace to the same capstone (CAP-36) and a future contributor
skimming issues could conflate "posted to venues" (this task, low bar)
with "submitted to a venue" (PF-4, a materially harder, separate,
unfiled deliverable). Recommendation: #868 or #867 should disambiguate
in-text ("promotion venues, not peer-review venues — see PF-4 for that").

**6. (Low) Community-relations exposure is real but out of this task's scope to fix.**
AC-3 requires citing named issues on competitor trackers (Digital,
Logisim-Evolution, CircuitVerse, Falstad, Issie/DigitalJS) as part of a
public comparison writeup. #510 §5, which #868 inherits its evidentiary
base from, is candid that part of the strategy is recruiting a rival
single-maintainer project's "named, reachable pool of demonstrably
motivated, rejected contributors" (Digital's). Publishing a
citation-heavy competitor comparison sourced from their own bug trackers
while simultaneously courting their contributor base is likely to read
as adversarial to that project's maintainer, and could generate blowback
disproportionate to a 0.25-0.5 mw task. This isn't fixable inside #868 —
it's inherited scope — but the task as filed does not flag the risk or
ask for a tone/reciprocity check before posting, and AC-5 ("reception is
explicitly out of scope") specifically forecloses treating a negative
reaction as a signal to reconsider. Recommendation: at minimum, note in
the checklist (#867) or here that a competitor-comparison post should be
reviewed for tone before the "posting" step, not just for factual
accuracy.

## What's solid

- AC-5 ("reception is explicitly out of scope") is a legitimately good
  call — it stops the task from silently mutating into a marketing-KPI
  chase, which the #590 comment thread (boundary note vs #582) explicitly
  guards against by keeping download-count instrumentation in a
  different issue.
- The core idea — that a checklist which has never been run is
  unverified process, and the fix is to run it once for real — is sound
  engineering discipline, consistent with this repo's general preference
  for enforced/ratcheted claims over aspirational documentation.
- The dependency ordering on #866/#867 is the right shape (statement and
  checklist before execution), even though nothing enforces it (Finding 1).

## Verdict rationale

`needs-rework`: the task is currently unstartable against the actual
repo state (Finding 1), its central verification criterion (AC-3) has no
mechanism forcing the re-check it mandates (Finding 2), and its cost
estimate does not obviously cover the audit and wait-time work the AC's
imply (Finding 4). None of these are fatal to the concept, but the issue
as filed should not be picked up until the ordering is made structural
and AC-3's dating/verification mechanism is made concrete.
