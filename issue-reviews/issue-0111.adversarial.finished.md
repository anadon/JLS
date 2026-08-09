# Issue #111: Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg (ex: Windows test-suite failures, fixed)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

A `tier:feature` tracker that began 2026-07-16 as a 3-bug Windows CI defect
report (CRLF round-trip, a leaked file handle, a CRLF-mangled test golden —
all fixed and merged by 2026-07-18), was reopened 2026-07-28 and rewritten
into a 7-stage (W1–W7) Windows CI-parity program, then run through three
more rounds of automated "dedup"/"REPLAN" bookkeeping comments through
2026-08-08. The underlying technical claims I could check (file paths, line
numbers, test names) are accurate against the current tree — see "What's
solid" below. The problems are almost entirely in the tracker's own
bookkeeping, not in the engineering it describes.

## Findings, most severe first

**1. (High) The body's machine block and DoD are stale and contradict the comment thread it sits on top of.**
The body's `Status & Dependency Graph` still reads `requires_tasks: []  #
no children filed yet` and every `planned_tasks` line (W1/W2/W4/W6/W7) is
listed as unresolved, own-scope work. But the comment thread shows: six
children were filed (#661–#666, all `part_of_feature: 111`) on 2026-08-04;
then on 2026-08-08 four of them (#661 W1, #662 W2, #663 W3, #664 W4) were
closed `state_reason: duplicate`, folded into #406 and #386 instead. I
verified #661 directly — `state: closed, state_reason: duplicate`. Only
#665 and #666 remain live children of #111. The issue's own design promises
a reader the body is "accurate ... without reading the comment stream"
(comment 5154461211) — that promise is now false. Recommendation: land the
REPLAN the 2026-08-08 comment (5227055611) explicitly calls for, updating
`requires_tasks`/`planned_tasks` to reflect that W1–W4 execute via #406/#386
and only W6/W7 are #111's direct children.

**2. (High) A DoD checkbox is gameable precisely because of finding 1.**
`- [ ] Every entry in requires_tasks closed ... via REPLAN` is trivially,
vacuously true today because `requires_tasks: []` in the body — nothing in
the DoD forces the body to first be corrected to list the real children
before that box is checked off. Anyone closing on the letter of the DoD
could tick it without any of #665/#666's work existing. Recommendation: the
close-out checklist should require the machine block to be re-derived from
the live GitHub graph (not just eyeballed), before that box can be honestly
checked.

**3. (Medium) The `blocked_by: []` field is technically true but conceals the real critical path.**
#111's two heaviest remaining stages (W1 promote-to-required, W4 JaCoCo
floor) no longer execute under #111 at all — they execute under #406
(confirmed by fetching #406: its body explicitly states "closes this
feature's Stages W1 and W4"). #406 in turn is `blocked_by: [374]` — issue
#374 fixes the fact that zero of six GitHub workflow files carry
`timeout-minutes` today (confirmed: `grep -c timeout-minutes
.github/workflows/*.yml` returns nothing in this checkout). None of that
transitive chain (#111 → #406 → #374) is visible anywhere in #111's own
`blocked_by`/`blocks` fields or DoD. A reader relying on #111's machine
block to answer "what's actually blocking Windows-required-check?" gets no
answer at all.

**4. (Medium) The "20 consecutive runs, at most one failure" promotion gate is manually curated and its exclusions are argued after the fact, not mechanically defined.**
The rule itself (borrowed from #101/#188) is reasonable, but nothing in
#111 defines what counts as a countable "failure" versus an excludable
"flake"/"non-lane event." The one worked example this issue cites (the
gui-wayland record, quoted via #406 O5) already required a human judgment
call to exclude `30226493722` as "not a lane flake" — i.e., the rule is
applied by narrative, not by a script. A well-intentioned but sloppy
promotion could exclude a real regression by re-describing it as
environmental, and no test or CI check would catch that; only careful human
reading of the run-ID list would. This is a real, if modest, gameability
gap in an otherwise sound mechanism.

**5. (Medium) Scope/process disproportion.** For a project whose own README
calls it a "single-maintainer pedagogy tool," #111 has accumulated 13
comments of automated cross-issue deduplication, "cluster C" audits, and
REPLAN cycles — and, per finding 1, four of the six child tasks it spawned
turned out to be duplicates of a sibling task (#406) that should have been
checked before filing. The net delivered engineering under #111's direct
ownership at this point is two lanes (#665, #666); everything else has been
re-homed. The overhead-to-output ratio here is worth a maintainer sanity
check before more child issues are spawned from this template.

**6. (Low) Issue-identity overload.** The issue explicitly and deliberately
reuses the number/identity of a closed, unrelated defect report ("Windows
test-suite failures, fixed," closed 2026-07-18) for an open-ended feature
tracker reopened ten days later. The issue defends this choice in its own
Background section, so it isn't an oversight, but it does mean any external
permalink (this repo's own `ToolLocator.java` Javadoc cites "issue #111
Stage W2 blocker," `.gitattributes` cites "issue #111") now points at a
large, still-evolving governance document rather than the specific fixed
defect that motivated the citation. Minor but real archaeology hazard.

## What's solid (verified, no note needed beyond this)

- All checked file:line citations match HEAD of this checkout:
  `continue-on-error: true` at `ci.yml:156`; `OSS_CAD_URL`/`OSS_CAD_SHA256`
  at `ci.yml:164/173`; the Windows build step at `ci.yml:244`; the
  `windows-gui` job header at `ci.yml:722-726`.
- `.gitattributes` L1–11 exists exactly as quoted and does name issue #111
  as its rationale.
- `test/jls/hdl/ToolLocator.java`, `IverilogCompileTest.java`,
  `GhdlCompileTest.java`, `FileHandleReleaseTest.java`, and
  `UntrustedFileHardeningTest.java` all exist and their described behavior
  (PATHEXT/.exe resolution, `/proc`-gated `assumeTrue` skips) matches the
  code.
- The three originally-closed defects (CRLF round-trip, leaked handle,
  golden CRLF rewrite) are correctly described as fixed-and-gate-covered
  and are not being reopened by this tracker — that separation is
  consistently maintained across every comment.
- The invariants in §4 of the body (never drop `continue-on-error` before a
  burn-in record; Linux stays sole JaCoCo/SpotBugs authority until W4; fix
  at source, not by broad platform muting) are sound engineering discipline
  and are consistently referenced by the child tasks that inherit them.

## Recommendation

Do not treat #111's body as ground truth for planning purposes until a
REPLAN corrects `requires_tasks`/`planned_tasks` to the real state (#665,
#666 open; W1–W4 delegated to #406/#386; #406 itself gated on #374). The
underlying CI engineering plan is sound; the tracking artifact describing
it is not currently trustworthy on its own.
