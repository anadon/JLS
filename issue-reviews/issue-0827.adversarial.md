# Issue #827: TASK-C569-3: someone who is not the maintainer follows the published walkthrough to a working element jar, from outside this repository
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue asks for

TASK-C569-3, the third slice of FEAT-C30-4 (#569), wants a human-in-the-loop
validation event: a non-maintainer builds an out-of-tree element jar for JLS
using only the published documentation from #825/#826, and every point
where they had to guess becomes a doc fix (AC-3/AC-4). It is explicitly
ordered after #212 (the ServiceLoader element-provider mechanism) and after
TASK-C569-1/2 (#825/#826, the catalog document and its stability labels).

## Findings, most severe first

### 1. The issue's own machine-readable block is already stale, and nobody fixed the body

The YAML front-matter still reads:

```yaml
ordering_after: ["#212 / #399 (the external element-jar path)", "TASK-C569-1", "TASK-C569-2"]
```

But issue #399 was closed 2026-08-08 as `duplicate`/`duplicate_of: 212`, and
this issue's own comment thread (comment
[#827#issuecomment-5227334021](https://github.com/anadon/JLS/issues/827#issuecomment-5227334021))
says as much: *"This comment supersedes the `ordering_after` line and
AC-3."* A comment that says it "supersedes" body text does not actually
change the body text — the YAML block, which is the part any dependency
tooling would parse, still names a closed issue as a live ordering
dependency and still uses the ambiguous `#212 / #399` slash-pair the same
comment calls out as "already ambiguous." A prior automated review flagged
this and the fix was never applied to the source of truth.

**Recommendation:** edit the issue body's machine block directly —
`ordering_after: ["#212 (the external element-jar path)", "#825", "#826"]`
— rather than leaving a superseding comment as the only correct copy.
Anything that trusts the body (a REPLAN script, a future dedup pass) will
silently pick up the dangling `#399` reference otherwise.

### 2. AC-3 is unfalsifiable as written, and its fix lives only in a comment, not in the acceptance criteria

AC-3, verbatim: *"A person other than the maintainer completes the
walkthrough using only the published docs; their run is recorded on #569,
including every point where they had to ask or guess."* Nothing bounds
*when* such a person appears. The same review comment thread on this issue
already establishes the risk is not hypothetical: *"On a repository whose
last two external contributors both bounced (#514's own evidence: `#4/#5`,
`#187`), 'find a non-maintainer' is precisely the scarce resource this
capstone exists to create."* README.md itself frames JLS as *"a
single-maintainer pedagogy tool"* — there is no established outside
contributor pipeline to draw a volunteer from.

The comment thread proposes a real fix (a fallback maintainer-on-a-clean-
machine run, plus a two-quarter kill criterion), but that fix is sitting in
a comment, not in the issue body's Acceptance Criteria section. Anyone
reading the issue body alone — which is what "published walkthrough" /
"capstone AC-6" framing implies gets acted on — still sees an
open-ended, recruitment-gated AC-3 with no exit condition.

**Recommendation:** fold AC-3′ (primary path / fallback / kill criterion)
from the comment into the actual AC-3 text before work starts, and give the
recruitment dependency (tracked as #571/TASK-C571-3, #831, per the same
comment) an explicit, non-blocking cross-reference here so this issue
doesn't quietly stall waiting on a task it isn't ordered after.

### 3. AC-3/AC-4 are gameable — the evaluator and the volunteer are not independent

AC-3 asks for "every point where they had to ask or guess" to be recorded;
AC-4 says each such gap gets closed or recorded as a known limitation
"with an owner" before the task closes. Neither criterion specifies who
recruits the volunteer, how much contact the volunteer has with the
maintainer during the attempt, or any time-boxing. A maintainer-recruited,
maintainer-adjacent volunteer (a lab student, a colleague) who gets
informal side-channel help would produce a clean "no gaps found" record
that satisfies AC-3/AC-4 to the letter while telling you nothing about
whether a true stranger could follow the docs. "Recorded as a known
limitation with an owner" is also an unbounded escape hatch — assigning an
owner is not the same as fixing the doc gap, and nothing here caps how many
limitations may accumulate before the task is allowed to close.

**Recommendation:** specify the volunteer must have no prior contact with
the maintainer about this walkthrough during the attempt (async-only,
questions logged verbatim rather than answered live), and cap the number/
severity of "known limitation" escapes that AC-4 will tolerate before this
task must stay open.

### 4. Missing acceptance criterion: the walkthrough must carry the trust/security disclosure, not just stability labels

AC-5 requires the walkthrough to state "the stability labels of the seams
it uses." It says nothing about the far more consequential fact recorded
in `ARCHITECTURE.md`'s "Plugin trust boundary" section (line ~303-308):
external element providers ship in-process via classpath `ServiceLoader`
behind a **"trusted extension" opt-in**, and *"such a jar has full JVM
authority; that must be stated plainly wherever the opt-in is offered."*
This walkthrough is precisely "wherever the opt-in is offered" for a
first-time external reader — the one place ARCHITECTURE.md's own rule
says the disclosure is mandatory — yet no AC in #827 requires it to appear
there. #212's own Completion Criteria requires the trust model to be
documented "in `docs/`" generally, but #827 is the human-facing walkthrough
that a stranger will actually act on, and it is the natural place for this
review to check the disclosure actually reached the reader, not just the
reference docs.

**Recommendation:** add an AC requiring the walkthrough to state the #222
trust stance in the reader's own path (not just link to it), verified as
part of the AC-3 non-maintainer run (did they notice/understand they were
granting the plugin full JVM authority?).

### 5. Feasibility: the whole task is contingent on a mechanism that does not exist yet, and that mechanism's own history is unstable

`ordering_after` correctly lists #212 first, which is sound sequencing on
its face. But #212 at the time of this review is still `state: open`,
`tier:feature`, its three child tasks are listed as **"Unfiled —
demand-gated"** in its own body, and `docs/extension-points.md` line 30
still shows the `elem.element-provider` seam as *"typed now (#78 shipped;
#212 external)"* — i.e. the external half has not shipped. #212's comment
history from the same day as this review (comments
5227004799/5227007727/5227084516/5227465853) shows it actively absorbing
issue #399, re-deriving its own dependency graph, and inheriting an
"orphaned task" correction from a third issue (#330/#403) — this is a
mechanism still churning at the architecture-decision level, not settled
code. A `band_mw: "0.5-1"` estimate for #827 implicitly assumes #212's SPI,
`ForeignElement`, the `-providers` flag, and the namespaced-tag grammar are
stable and working by the time #827 starts; nothing in #827 verifies that
assumption or restates it as a precondition beyond the bare ordering
edge.

**Recommendation:** treat `band_mw: "0.5-1"` as provisional and revisit it
once #212 actually lands — the estimate should not be trusted for
scheduling while its sole hard prerequisite is still at the "planned, not
filed" stage.

### 6. Scope overlap with #825 on "compiles in CI" — unclear which issue owns the example project

#827's own AC-2: *"The example project is buildable as written — its
sources are kept in-tree or in a companion repository and compiled in CI
against the published API so the walkthrough cannot rot."* #825's AC-3:
*"Each example is real code that compiles against the published API —
examples are compiled in CI, not pasted prose."* Both issues assert
ownership of "the example compiles in CI" without cross-referencing each
other on this specific point (#827's Notes section cross-references #223/
#399/#212 for mechanism ownership, but not #825 for the CI-compiled-example
overlap). It is not clear whether #827's "example project" is the same
artifact as #825's "one worked example" for the element-provider seam, or
a second, larger example that duplicates the CI-compilation commitment.
The prior review comment on this issue already flags a related ambiguity
("AC-2's 'companion repository' needs a decision, not a default") without
resolving the #825 overlap.

**Recommendation:** state explicitly in #827 whether its walkthrough's
example project *is* #825's element-provider worked example (in which case
AC-2 should say so and drop the redundant "compiled in CI" restatement) or
a distinct, larger demo (in which case the maintenance-cost doubling should
be acknowledged).

## What's solid

- The ordering (#212 → TASK-C569-1/2 → this task) is the right sequencing
  in principle: you cannot validate a walkthrough for a mechanism and docs
  that don't exist yet.
- AC-1, AC-2 (modulo finding 6) and AC-5 (modulo finding 4) are concrete
  and checkable once their prerequisites land.
- Framing this as a real-external-follower validation rather than a
  self-certified "we wrote docs" checkbox is the right instinct for a
  documentation quality gate — the failure mode it targets (docs that only
  the author can follow) is real and worth guarding against.

## Note on the existing review comment

A prior adversarial pass already left a substantive comment on this issue
(2026-08-08, id 5227334021) covering the #399 dangling reference, the
AC-3 unfalsifiability, and the AC-2 companion-repository ambiguity. This
review concurs with all three and treats them as confirmed rather than
re-deriving them from scratch, while adding: the fix for #1/#2 was never
applied to the issue body itself (finding 1), a gaming/independence gap in
AC-3/AC-4 (finding 3), a missing security-disclosure acceptance criterion
(finding 4), and the #825 CI-compilation overlap (finding 6).
