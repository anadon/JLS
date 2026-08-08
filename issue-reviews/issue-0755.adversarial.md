# Issue #755: TASK-C576-1: the distribution and submission layouts are specified in tree, with a worked example an instructor copies
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

A 0.5 mw documentation task: specify two directory layouts (instructor
distribution, student submission) in tree, with a worked example, so that
TASK-C576-2 (#757) has a contract to grade against. Scope is genuinely
small and self-contained, and the repo's own conventions (no hosted
service, no accounts — README's installer story, ARCHITECTURE.md's
"help delivery: in-jar" decision) already support the stated boundary.
The task is fundamentally sound; the concerns below are about issue
hygiene (a correction living only in a comment, not the body) and two
specification gaps a spec-only task must not leave implicit.

## Findings, most severe first

**1. The ordering correction lives only in a comment; the issue body a
future reader opens first still shows the wrong edge.**
The visible issue body still carries the machine block exactly as filed:
```yaml
task_id: TASK-C576-1
band_mw: 0.5
ordering_after: [300, 369]
```
The 2026-08-08 comment (`author_association: OWNER`, same account) states
this "supersedes the `ordering_after` line" and that the corrected value
is `ordering_after: []` / `blocks: [757]`, with the reasoning that #300 is
`tier:capstone` (confirmed: `mcp__github__issue_read` on #300 returns
`"title":"CAP-06..."` with capstone framing) and that ordering a task
behind a capstone is equivalent to "last." That reasoning is sound, but
the *body* — the part most tooling and most human skimmers treat as
authoritative — was never edited to match. Anyone triaging by machine
block alone (a script filtering on `ordering_after`, or a contributor who
doesn't read all comments) will still see this task gated behind #300 and
#369's three-issue dependency chain (#316, #321, #347) and skip it as
blocked, defeating the comment's own point that "this task is ready now."
**Recommendation:** edit the issue body's machine block directly (or add
a `~~struck~~` + corrected block) rather than relying on a superseding
comment; the repo's own process (visible in #300/#502/#466) treats
machine blocks as the authoritative status, so a comment-only correction
is a known-fragile pattern here.

**2. AC-1's "how a submission identifies its student" is a privacy/PII
surface with zero guidance, and the worked example is required to be
git-committed.**
AC-1: *"including how a submission identifies its student"*, and AC-2:
*"A worked example of both layouts is committed."* Nothing in the issue
or its comment addresses what identifier goes in a **committed, public**
worked example — a real name, a student ID, an email, a pseudonym? JLS is
GPLv3, hosted on public GitHub; if an instructor copies the shipped
worked example's identifier convention verbatim into a real course
repository (the issue's own stated purpose — "an instructor copies
rather than derives"), a naive identifier scheme (e.g., `firstname-
lastname/`) becomes a template for accidentally committing PII to a
public course repo every semester. The correcting comment's §4 addresses
*only* the forward-compatibility angle ("must not assume... report
keying") and is silent on privacy. **Recommendation:** add an explicit
acceptance-criterion note that the worked example uses a synthetic,
non-PII identifier (e.g., an opaque roster ID or `student-01`) and that
the spec states this as a recommendation, not just an implementation
detail left to the instructor.

**3. AC-3 ("what is stable and what an instructor may vary") asks this
task to write a contract for a consumer that doesn't exist yet.**
TASK-C576-2 (#757) is `ordering_after: ["TASK-C576-1", 369, 466]` — it
consumes #466 (TASK-0111), which is itself a ~30-page unimplemented
design (`Expectations`, `TestVectorRunner`, `GradeReport`, exit status 3
— none of it exists in the tree today; `docs/batch-interface.md` has
exactly three exit statuses, confirmed by reading the file). This task is
asked to freeze "what an instructor may vary" in the submission layout
*before* the engine that will read it is designed, let alone built. The
correcting comment recognizes the risk in principle ("under-promising is
the safe error") but that is guidance, not a criterion — nothing in the
checklist forces the spec to mark fields explicitly "provisional, pending
#757" rather than presenting them as settled. If #757/#466 lands with
different needs (e.g., one expectations file per submission vs. one per
lab, a metadata format the layout didn't anticipate), this "stable"
contract gets revised anyway, and downstream instructors who "copied
without editing anything but names" inherit the churn. **Recommendation:**
require the spec to tag each layout element as `fixed` or
`provisional (owner: #757)` explicitly, not just in prose discipline.

**4. AC-4's "no hosted service... is required or implied anywhere" is an
absence claim with no falsification method.**
Unlike AC-1/AC-2 (checkable: does the tree contain the files?), AC-4 asks
the reviewer to prove a negative about *implication*, not just literal
content — there's no test, grep pattern, or reviewer checklist named for
"implied." A spec could pass a literal reading (no URL, no server binary)
while still implicitly assuming a shared drive, an LMS folder-sync
convention, or timestamps that only make sense under a hosted submission
portal, and AC-4 as worded gives no way to catch that except a reviewer's
subjective judgment. This is a minor criterion-quality issue, not a
correctness one — the repo's actual practice (no service anywhere, per
README and ARCHITECTURE.md's recorded decisions) makes an accidental
service-shaped spec unlikely, but the criterion itself is unfalsifiable
as written. **Recommendation:** anchor it to something checkable, e.g.
"the worked example round-trips through `cp -r`, `zip`, and a plain USB
drive with no network access," which is testable.

**5. Feasibility/scope note — task is otherwise well-bounded.** The
Boundary section correctly excludes TASK-C576-2's grading command and
TASK-C576-3's CI walk, and correctly points to #502 (CAP-21) for
platform-native submission rather than trying to reconcile with
Gradescope/Classroom conventions here. That separation is honest about
what a 0.5 mw task can deliver and matches the repo's stated non-goal of
building any hosted service. One line worth flagging for the assignee
though, not the issue itself: #502 CAP-21 will eventually need its own
submission shape per platform (Gradescope zip upload, Classroom repo-per-
student) — this task's layout is not required to anticipate that, and the
issue correctly doesn't claim it does, but a future reader should not
mistake this spec as CAP-21's contract too.

## What's solid

- Scope is minimal and matches its cost band: a document plus a
  directory tree plus a worked example is genuinely ~0.5 mw work, and the
  Boundary section keeps it from creeping into TASK-C576-2/3 territory.
- The "no hosted service" framing is consistent with the rest of the
  repo's recorded philosophy (self-contained jar, no accounts, in-jar
  help as the deployment model) — this task isn't introducing a new
  architectural stance, just applying an existing one.
- The correcting comment's technical argument (a task cannot meaningfully
  wait on a capstone; the dependency direction was backwards) is correct
  and, if applied to the machine block per Finding 1, resolves what would
  otherwise be a real scheduling contradiction.

## Verdict

sound-with-concerns — the task itself is small, well-bounded, and its
premise (write the contract before the engine, but mark what's
provisional) is reasonable. The concerns are: (1) the load-bearing
ordering correction isn't reflected in the issue body, which is an
operational hazard for anyone not reading every comment; (2) the PII
angle of a committed, copyable worked example is unaddressed; and (3)
AC-3's "stability" promise is being made against an unbuilt consumer with
no explicit provisional-tagging requirement. None of these block the
work — they're refinements the assignee should fold in before or during
implementation.
