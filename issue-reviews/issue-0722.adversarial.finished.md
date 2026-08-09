# Issue #722: TASK-C539-2: N clock cycles of a selected canvas region become an animation with signal-value overlays, from a recorded run and no other source
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

#722 is TASK-C539-2, the "capture half" of FEAT-C24-4 (#539, PF-4 of the
CAP-24 print-figure capstone #505): turning a selected canvas region and a
cycle count into an APNG/GIF with signal overlays. Its sibling, TASK-C539-1
(#720), is the "encoder half" (pure-Java APNG/GIF writer). #722 is
`ordering_after: [TASK-C539-1]`, correctly sequenced behind it.

Critically, #539 itself already carries a "Disposition note" that #508 (the
product-direction review) recommends **cutting** PF-4 entirely, and that
funding must wait on an explicit `REPLAN:` comment on #505. #722 repeats this
same warning in one line ("Recorded before work starts: #508 recommends
cutting FEAT-C24-4 entirely. Do not fund ahead of an explicit REPLAN on
#505"). I fetched #508, #505, and #539 directly to check this chain.

## Findings, most severe first

**1. (High) The task is fully implementation-ready despite its own stated
gate not being cleared, and as of today that gate is still open.** #505's
only comment (2026-08-04) says the PF-4 cut is "pending" adjudication and
"the adjudicating REPLAN on this issue has not happened yet." #508's most
recent comment (2026-08-08, the "ORDERING-COHERENCE REVIEW round 2") lists
"7 | CAP-24 #505 figure-export slice | after CAP-23 slice (shared UX) |
unchanged" — the PF-4 cut recommendation is neither adopted nor withdrawn as
of the current date. Four days after #722 was filed, the precondition it
names as blocking is still unmet, yet #722 carries a full, testable AC list
with a named test class (`AnimationCaptureTest`), exactly the shape of a
ready-to-pick-up task. A contributor grepping open `tier:task` issues for
work has no structural signal (label, `blocked_by` field, or draft/hold
marker) that this one is provisional — only a sentence of prose above the
AC section. Recommendation: either add a machine-readable block (e.g.
`blocked_by: [REPLAN on 505]`) or a literal marker on the AC heading itself,
not just the preceding paragraph — the sibling review of #539 flagged the
identical structural gap and it was not fixed before #722 was filed on top
of it.

**2. (High) AC-1 and AC-3/AC-4's "recorded-run artifact" input has no
defined type anywhere in the codebase, and this task does not scope adding
one.** Grepping `src/` and `test/` for `Recording`/`RecordedRun`/
`SessionRecording`/`Replay` returns nothing; `docs/*.md` and
`ARCHITECTURE.md` mention no such concept either. `BatchSimulator`
(`src/jls/sim/BatchSimulator.java`) accumulates `TraceSample`s and drives VCD
export, which is the closest existing candidate, but #722 never names it.
The phrase "recorded run" is inherited from #498 §7.2 — a document whose own
header states "It is explicitly non-normative... Nothing in it may be cited
as settled policy" and which describes a **future, unbuilt** M2 milestone
("A GUI session records and replays in batch byte-identically... a new
`SessionBoundaryRatchetTest`"), not a shipped artifact. A task titled "the
capture half" cannot specify what it captures *from* without naming this.
Recommendation: name the concrete input type (existing `TraceSample`/VCD
export, or an explicit new format) before this task is actionable.

**3. (High) AC-3's citation of "no other source" and the parenthetical
appeal to #498 §7.2 is a misapplied citation — verified by reading §7.2
directly.** §7.2 ("`docs/vcd-interop.md` and #63 — recording, not
reopening") is a correction about whether **live interactive GUI grading**
should be a supported autograding surface, i.e. an interactive-vs-batch
*grading determinism* question. It never mentions figures, exports,
schematics, animation, or multi-artifact consistency. The requirement that
an animation must come from the same run as the rest of a handout (the
actual point #722 is making) is independently and correctly argued in
#505's own §3 risk 4 ("The bundle must come from one recorded run") — #722
does not need #498 §7.2 to support this claim and citing it borrows
authority the source document does not carry for this use. The sibling
issues #539 and #541 make the identical misapplied citation for the same
reason; #722 is the third instance of the same error propagating downstream
unexamined.

**4. (High) AC-4 depends on a sibling feature this task does not declare as
a dependency.** AC-4: "Frame rendering reuses the print/figure render path
rather than screen-grabbing the editor." The only rendering path that exists
today, `src/jls/edit/CircuitRenderer.java`, draws the **screen-styled**
circuit (confirmed: it is the same code path `-i` image export uses per
`JLSStart.java`, and #505's own Background section states plainly "Static
image export ships, screen-styled... Nothing print-styled"). The
print-styled render path AC-4 requires is PF-1's deliverable, filed
separately as FEAT-C24-1 (#536), which is open and unlanded. #722's machine
block declares only `ordering_after: [TASK-C539-1]` — it says nothing about
#536. This is exactly the failure mode #508's 2026-08-08 round-2 comment
spent an entire section correcting elsewhere in the tracker ("name the
criterion, not the issue... every citation of [a chokepoint] found so far
was over-broad at first" / missing edges caused real scheduling deadlocks).
#722 has the same class of bug in the other direction: a real dependency
that is *absent* from `ordering_after` rather than mis-scoped. If #722 is
picked up before #536 lands, AC-4 is either unimplementable or gets
satisfied by quietly reusing the screen-styled path — which would produce
exactly the animation/static-figure mismatch AC-4 exists to prevent.
Recommendation: add `536` to `ordering_after` (or `requires_features`).

**5. (Medium) AC-2's "declared size budget" is never declared, here or in
either issue it's copied from.** Checked #722, #539 (its immediate parent),
and #505 (AC-5, the ultimate source) — none states a number, range, or unit.
"Stays under the declared size budget" with no budget on record is not
falsifiable: an implementer can pick an arbitrarily generous number (or wire
up no real enforcement and just print the observed size) and the letter of
AC-2 is satisfied while the actual goal — an artifact small enough to live
in a course-repo git history — goes unverified. This is the same defect
already flagged on #539 and #720; #722 is a third copy of it, unfixed.

**6. (Medium) AC-1's headline claims have no test coverage; only AC-2 names
a test.** AC-1 promises an 8-cycle hazard-demo capture, a GIF alternative,
and correct signal-value overlays. `AnimationCaptureTest` (AC-2) is scoped
only to "a 32-cycle capture['s]... deterministic frame count and timing
metadata... under the declared size budget" — nothing about overlay
correctness, the hazard-demo scenario, or the GIF path specifically. An
implementation could ship `AnimationCaptureTest` green, produce zero legible
overlays and no working GIF path, and by this issue's own written contract
that would count as satisfying AC-1. AC-3 ("a test asserts no path reads
live simulator state") also names no test class; it's plausible via the
project's established `HeadlessCoreRatchetTest`-style import/bytecode-scan
pattern (the codebase already does exactly this for a different boundary —
`src/jls/sim/Simulator.java`'s headless-core ratchet, per
`ARCHITECTURE.md`), but the AC itself doesn't say so, so what actually gets
built and asserted is left to the implementer's judgment.

**7. (Medium) AC-1's fixture, "the hazard-demo run," does not exist in the
repository and this task does not scope creating it.** Repo-wide search for
"hazard-demo" / "hazard_demo" turns up nothing except this fleet's own prior
review files (which quote the issue text back). `test/fixtures/` holds
`riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`, and
`headless-canary-gate.jls`; no hazard-related circuit exists anywhere in the
tree. Neither `ordering_after` nor any prose in #722 brings fixture creation
into scope, so AC-1 as literally written cannot be executed against the
current repository.

**8. (Low) Label mismatch, inherited from the parent feature.** #722 carries
`area:gui` (with `area:test`) but its own title frames the work as
batch/CLI-shaped ("from a recorded run and no other source" — explicitly
*not* a live-editor capture). #539's sibling review already flagged the
identical mislabel one level up; #722 repeats it rather than correcting it,
and correctness here matters more than at the feature level since a task
issue is what a contributor actually files a PR against.

## What holds up

- The disposition-note honesty is real: #722 does surface the #508 cut
  recommendation rather than silently proceeding, and the quote is faithful
  to #508's actual text (verified directly).
- The TASK-C539-1/TASK-C539-2 split (encoder vs. capture) is a sensible,
  independently testable decomposition, and the two declared bands (1–1.5 mw
  each) sum cleanly to #539's own 2–3 mw estimate — no arithmetic drift here,
  unlike several sampled capstones #508 flags elsewhere in the tracker.
- `ordering_after: [TASK-C539-1]` is the one dependency edge #722 does
  declare, and it is correct: the capture task genuinely needs the encoder
  to exist first for any end-to-end test.
- The MP4/pure-Java boundary is inherited consistently from #505's Open
  Question 4 through #539 into this task without drift.

## Verdict rationale

`needs-rework`: this is a downstream task built on top of a feature (#539)
whose own review already found the load-bearing citation misapplied, the
input format undefined, the fixture missing, and the size budget
undeclared — and #722 inherits all four defects verbatim rather than
resolving any of them at the task level, where they would need to be
resolved before a PR could actually close this issue. It adds one new,
task-specific defect of its own (the undeclared #536 dependency in AC-4) and
sharpens a structural one already flagged upstream: this is now three
issues deep (#539 → #722, plus #720 alongside) presenting REPLAN-gated,
not-yet-adjudicated work as pick-up-ready without a machine-checkable hold.
None of this rises to `should-not-proceed` — the task boundary, cost
accounting, and MP4 exclusion are all sound, and the disposition note means
no one is being deceived about the funding question, only under-warned about
it structurally. Before implementation: resolve the #505 REPLAN first (per
the issue's own instruction), then in the same pass name the recorded-run
input type, name or file the hazard-demo fixture, declare the actual size
budget number, add `536` to the dependency edges, and add test coverage for
GIF output and overlay correctness.
