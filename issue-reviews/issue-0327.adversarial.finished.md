# Issue #327: FEAT-037: a drawn register has an honest reset that exports as a reset, and every clock-domain crossing in a design is reported
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The engineering content is good: the reset-vs-initial-value distinction is
real (ASIC synthesis discards `init`, confirmed at `src/jls/elem/Register.java:723`
where `initSim` only ever sets `currentValue` from `initialValue`, with no
reset input anywhere — `grep -in reset` on that file turns up only prose and
`resetPropDelay`), the formal semantics in §3 are precise and match how real
sync/async resets behave, and the "explicitly out of scope" list correctly
keeps this issue from swallowing #291/#292/#199/#125. The problems are
procedural: the issue's own tracking machinery has already drifted from
reality, one of its self-declared filing gates was not honored, its headline
acceptance number (false-positive rate) has no threshold, and a chunk of
required work has no owner at all.

## Findings, most severe first

### 1. The issue body is already stale about its own children's filing status

The Decomposition table and the `planned_tasks` YAML block both say
TASK-0077 and TASK-0078 are "not filed" / "planned, not filed." But the
issue's own single comment (2026-08-04, `issuecomment-5181355607`) records:

> `#327 = #478 + #398`
> **#478 — TASK-0077:** "a drawn register has a real reset..."
> **#398 — TASK-0078:** "a clock stops being an ordinary wire..."

Both #478 and #398 are confirmed open, filed 2026-08-03 — a day *before*
that comment and four days before this review. The issue body was never
edited to reflect it: `planned_tasks` is not empty, the Decomposition
table still says "not filed," and `requires_tasks: []` never picked up
either child. The issue's own Completion Criteria demand exactly the
opposite: "`planned_tasks` empty (each resolved to a filed issue...)" and
"Machine block, roster table, and mermaid graph agree with reality." A
reviewer who reads only the body (reasonable, given the comment is easy to
miss under this much prose) will materially misjudge where this feature
stands. **Recommendation:** edit the issue body now — move both tasks into
`requires_tasks`, empty `planned_tasks`, update the roster table and the
mermaid graph — rather than leaving the correction stranded in a comment.

### 2. Open Question 2's self-declared filing gate was not honored

The issue states, of "Which discipline protects the new attributes from
silent loss?": **"Blocks filing children."** Yet both children were filed
without it being resolved. Confirmation from the children themselves:
#398's own §2 O7 says *"#327 Open Question 2 asks which discipline
protects it"* and its Open Questions section repeats the identical
question, still marked **"Blocks execution."** #478 §7.1 simply asserts
"No `FORMAT` bump" under the current silent-drop rule without recording
which discipline (fail-loud loader vs. section-versioning epoch) was
chosen. So the gate #327 itself wrote — decide this *before* filing
children — was skipped, and the unresolved question was pushed downstream
into two now-open task issues instead. If the answer later comes out "the
epoch feature," TASK-0077's `.jls` file-format doc edit and TASK-0078's
`phase` attribute doc edit are both plausibly wrong as currently scoped.
**Recommendation:** resolve Open Question 2 on #327 now (it names its own
recommended default, option (b)) and confirm both children's file-format
sections against the resolution before either lands.

### 3. The published false-positive rate has no threshold, so the acceptance criterion is gameable as written

Criterion 6 requires "machine-readable output," and I6 / the DoD both say
"the crossing check's false-positive rate over the corpus is published as
a number." Nowhere in #327 is a maximum acceptable rate stated. The child
task (#398) admits this explicitly in its own Open Question 4: *"H4 has no
threshold yet, and without one 'the check is usable' is unfalsifiable... 
setting a threshold before measuring invites tuning the measurement."*
That's a reasonable methodological stance for the *task*, but it means
#327's own Completion Criteria checkbox — "the false-positive rate...is
published as a number" — is satisfied by publishing *any* number,
including one so high the check would (by the issue's own §7 language)
"get turned off." A closing agent could tick that box while the
crossing-check capability described in §1 item 5 is unusable in practice.
**Recommendation:** #327 should either set a numeric ceiling itself or
explicitly delegate threshold-setting to a named decision point that
gates close-out, not just publication.

### 4. Sequencing story is currently inconsistent with the filed children's own machine blocks

§6 states plainly: "TASK-0077 before TASK-0078 is necessity, not
convention: the reset input is a pin domain propagation must classify."
But #478 (TASK-0077) declares `blocked_by: []`, and #398 (TASK-0078)
declares `blocked_by: [336]` only — #478 is not in it. #398's own text
explains this as a known gap: *"Unfiled prerequisite. **TASK-0077**...is
being filed concurrently...A link pass adds it."* TASK-0077 is no longer
unfiled (it's #478, open since 2026-08-03), so the promised link-pass edit
appears not to have landed yet. As currently declared, nothing stops
TASK-0078 from being picked up before TASK-0077, which would violate the
"necessity, not convention" ordering #327 insists on. **Recommendation:**
add the #478 → #398 `blocked_by` mirror now; it is a one-line edit and the
issue already flagged it as owed.

### 5. A large, unowned "residual" carries most of the risk, and no task id can be minted for it

Open Question 1 is refreshingly honest that the two named tasks sum to
3.5 maintainer-weeks against a corpus-derived band of 13–18 weeks (3.7x–5.1x
off), and that the gap is "the residual named in §2, which **no task id
names**" — carrying domains through both HDL emitters, the exporter's
policy, and the golden corpus. The issue further notes "the registry's
task space is closed at TASK-0112," so this residual structurally *cannot*
become a normal filed task under the project's own numbering discipline
until something changes. That means the feature's global invariants
(§4) and most of the Integration Criteria (I2–I6) — which are explicitly
about the residual, not about either named task — currently have no
assigned owner, no filed issue, and no re-banding commitment beyond "do
this after TASK-0078 lands." Two clean task landings could leave this
feature nowhere near closeable. This is disclosed, not hidden, which is to
the issue's credit, but it is still a real feasibility risk that a
scheduler reading only the roster table (which lists just two rows) would
miss. **Recommendation:** file the residual as a tracked (if informally
numbered, or under a new registry range) placeholder issue now rather than
waiting for TASK-0078 to land, so it has *some* address before work starts
piling up behind it.

### 6. Evidence is pinned to a commit this checkout cannot reach

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not
resolve here (`git cat-file -t <sha>` fails, and it is absent from
`git log` and every local/remote branch). Every "ABSENT at 2d0ca9d" claim
and embedded `git grep`/`git show` command in this issue and its two
children is consequently unreproducible by a reviewer working from this
clone — you have to take the pasted output on faith. Spot-checking what
*can* be checked against current `HEAD` (the `Register.java:272-273`
`OWN_ATTRIBUTES` declaration, the `:723` `initSim` line, the absence of a
`Clocked` interface, the `Timed.java:25` javadoc quote) all matched, so
the substance is plausible — but the verification mechanism the issue
itself insists on ("citations re-derived if HEAD had moved," rule 6) can't
actually be exercised against the SHA as given. **Recommendation:** note
in the issue that the evidence commit is not reachable from `origin`
(likely squashed/rebased history), or replace it with one that is, so a
future closer can actually run the cited commands.

## What's solid (one line each)

- The core distinction driving the feature — an `init` value is not a
  reset, and synthesis discards it — is correct and well evidenced against
  the actual `Register.java` code.
- §3's formal reset semantics (mode × polarity × reset input × clock edge)
  match standard sync/async flip-flop behavior, including the subtlety
  that async reset is *not* clock-gated.
- The backward-compatibility invariant (`reset mode "none"` byte-identical
  simulation for pre-feature files) is concrete, testable, and correctly
  promoted from criterion to global invariant.
- The out-of-scope list is disciplined: it correctly refuses to relitigate
  #125 (default trigger mode) or absorb #291/#292 (reject-list lifting),
  keeping this issue's blast radius bounded.
- `blocked_by`/`blocks` mirrors with #336 and #328 are correctly
  bidirectional and consistent with those issues' own machine blocks.

## Verdict rationale

Not `needs-rework`: the technical design is sound and the scope boundary is
disciplined. Not `sound` outright: two of the issue's own self-imposed
process gates (filing sync, the Open-Question-2 filing block) have already
been missed in the four days since filing, the marquee acceptance number
(false-positive rate) is unbounded as written, and a large slice of the
work has no owner. `sound-with-concerns` — proceed, but fix findings 1, 2
and 4 (all cheap, mechanical edits) before more work lands on top of the
current inconsistent state, and resolve 3 and 5 before this issue is
allowed to close.
