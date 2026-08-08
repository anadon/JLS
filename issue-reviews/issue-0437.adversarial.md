# Issue #437: TASK-0006: a saved circuit is plain canonical text by default, and the autosave and undo containers are decided rather than inherited
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The core technical claim is verified against the live tree, not just the
cited (and, as discussed below, unreachable) commit: `Circuit.java` still
initializes `saveContainer = FileAbstractor.Container.XZ`, the two-argument
`FileAbstractor.writeCircuit` still hardcodes `Container.XZ`,
`SimpleEditor.writeCheckpointInBackground` still calls the two-argument
overload (inheriting whatever the default is rather than choosing), and
`CircuitSnapshot` still deflates unconditionally. The five `FileAbstractorTest`
methods named (`aFreshCircuitDefaultsToTheXZContainer`,
`defaultWriteIsStillXZCompressed`, `plainTextWriteIsTheBareCircuitText`,
`bothContainersLoadTheSameCircuitText`, `plainTextWriteReplacesAnXZFileAtomically`)
all exist with the quoted bodies. The scope-out items (`MAX_CIRCUIT_TEXT_BYTES`
untouched, no `FORMAT` bump, XZ retained as an option) are correctly reasoned.
So the *content* of the issue is sound. The problems below are about
evidentiary hygiene, dependency-chain feasibility, and criteria that are not
actually pinned yet.

## Findings, most severe first

### 1. (High) The evidence commit the whole issue is pinned to does not exist in this repository, and the issue's own comment thread proves the pin is unstable
`git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails with "not a
valid commit name" against this checkout — every one of O1–O10's ~15
GitHub permalinks (`.../blob/2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7/...`)
points at a commit this repository cannot resolve. The issue's own second
comment (2026-08-08T18:29) confirms this is a real, live problem, not a
reviewer artifact: it retracts a *correction* made in the first comment
(2026-08-08T17:26) that had already tried to re-pin evidence to
`07a0bea`, because `07a0bea` "is not on master" and lives only on a branch
"#493 says is being deleted." Two re-pins inside one day, on the filing
day itself. The Completion Criteria explicitly require "Every cited
evidence document and permalink resolves on the default branch at close —
no branch-path links, no deleted docs" — the issue fails its own DoD item
before an executor has written a line of code. **Recommendation:** before
work starts, re-derive every O1–O10 citation by content search (`git grep`)
against current `HEAD`, as the second comment instructs, and replace every
permalink in the issue body itself (not just in comments) with either
line-free prose citations or a link to a real, retained commit. I did this
independently for O1, O2, O4, O5, O6, O10 and the FileAbstractorTest
citations — the *content* holds up — but the issue as filed cannot be
trusted at face value by anyone who follows its own links.

### 2. (High) The actual gating condition for starting this task lives in a same-day comment, not in the issue's own `blocked_by` block or Definition of Done
The issue body's Status & Dependencies block says only `blocked_by: [436]`,
and the Completion Criteria checkbox says "Every `blocked_by` entry ...
has landed (#436)." But the first comment — posted the same day the issue
itself carries a last-`updated_at` timestamp of 2026-08-08T18:29 —
sharpens that to: "The precondition this task waits on is now 'constant
δ, demonstrated on a wired, interleaved-replica fixture' — not merely
'sref has landed'... If #436 lands the reference form without the `id`
change, its ratchet is either red or measured on the wrong fixture, and
the premise for flipping the container is not yet true." That is a
materially stronger bar than "#436 is closed," and it is not reflected
anywhere in #437's own YAML, Predictions, or Definition-of-Done text. An
executor who reads only the issue body (reasonable — comments are not
normally treated as part of the spec) would pick up #437 the moment #436's
checkboxes are ticked, without checking the δ(800)/δ(200) ratio the
maintainer says is now the real gate. **Recommendation:** fold the ratio
criterion into #437's own `blocked_by` note and DoD checklist, not just
into a comment, or the acceptance bar silently drifts depending on who
reads what.

### 3. (Medium) The hard dependency chain makes this issue not independently actionable, and the estimate math is optimistic
#437 is `blocked_by: [436]`. #436 is itself `blocked_by: [315]` (FEAT-001,
registry-table totality) and carries three of its own Open Questions,
one of which (H2, whether the `id` base attribute itself must stop being
emitted per-block) is stated as capable of forcing a second format change
if confirmed — i.e., #436's own scope could grow before it lands. #334
(the parent feature) prices #436 at 2 maintainer-weeks and #437 at 1,
for a 3-week band — but that arithmetic assumes #436 lands clean on the
first attempt with no H2-triggered rework and no #315 slippage. None of
that is #437's fault, but a reviewer scoped to #437 alone should flag it:
this issue is not "ready to start," and its own Method section's first
step ("Re-verify O1–O10 ... If P1/P2 no longer fail, this issue is
superseded") is the only self-check that would catch #436 having silently
changed the premise. **Recommendation:** treat #437 as not pickable
until #436 has a closing `STATUS:` comment on #334 confirming the ratio
criterion, per the maintainer's own comment #1 here.

### 4. (Medium) Several "must hold after" predictions are not actually pinned by the document that states them
P5 ("Trigger an autosave and sniff the resulting `.jls~`; observe the
container this task *declares*") does not state what that container is —
it defers to Open Question 2, which only offers a "recommended" (XZ) and
is marked "Blocks execution, not filing." That is defensible sequencing
(decide-then-test is fine), but as filed, P5 is not falsifiable against
the issue text alone: a PR could pick either container, both pass
"P5 asserted... against a literal Container," and both would technically
satisfy the letter of the prediction. The Definition of Done compounds
this with several purely procedural, self-reported checkboxes ("recorded
in the PR," "outcome... recorded," "each new entry is justified") that a
reviewer cannot verify from the code or test suite alone — they depend on
trusting the PR author's prose. None of this is unique to #437 (it's the
house style for this repo's tier:task issues), but it does mean the
"acceptance criteria" are partly a paperwork audit rather than a
test-suite gate. **Recommendation:** before close, Open Question 2 should
be resolved and its answer written as a literal expected value inside
P5's own text (not only into `docs/file-format.md`), so the prediction is
checkable without cross-referencing a separate decision record.

### 5. (Low) One of the five "Open Questions & Decisions Needed" is already answered by the current tree, undermining confidence in the currency of the rest
Open Question 5 asks whether a compressed file filter needs to be added
to the Save As chooser "because inverting O5 without one leaves no way to
choose XZ from the GUI." Reading `src/jls/edit/Editor.java` (current
`saveAs`, ~lines 108–152) shows the chooser already offers **two**
separate, already-choosable filters — `filter` ("JLS Circuit Files (XZ
compressed, the default)") and `textFilter` ("JLS Circuit Files (plain
text: diffable, fork-readable)") — both added via
`chooser.addChoosableFileFilter`. The GUI mechanism this Open Question
worries about not existing already exists; only the *default* filter
selection and the two label strings need to flip. This is not a
correctness bug in the issue (the Method step still correctly describes
inverting the mapping), but it is more evidence that the document was
drafted against a snapshot of the tree that has since diverged, in the
same vein as Finding 1. **Recommendation:** strike or amend Open
Question 5 to reflect that the chooser plumbing exists; only the
selection default and label text need to move.

### 6. (Low) Scope-to-ceremony ratio is disproportionate to the change itself, by the issue's own admission
The issue quotes BRIEF.md D1 approvingly: "The mechanism already exists
and is already tested... Changing the default is a POLICY change plus
test updates, not an implementation project." The document then runs to
14 numbered sections, formal transformation notation ($w: T \times C \to
B$) for a two-line enum-default flip, a `flowchart` and a data-flow
equation block, five blocking-or-riding-along Open Questions, and a
~22-item Definition of Done. This is house style for the repo's task
tier and not unique to #437, but worth flagging on its own merits: the
heavier the ceremony, the more places (as Findings 1, 2, and 5 show) for
staleness to creep in between filing and pickup. **Recommendation:** none
beyond what's already noted — this is a process observation, not a
blocking defect.

## What's solid (no action needed)

- **H1/H2/H4 (the technical hypotheses) hold against the live code**: O1 and
  O2 really are the only two default-selection sites; `openCircuit`'s
  sniff order is name-independent (confirmed in `FileAbstractor.java`);
  `MAX_CIRCUIT_TEXT_BYTES` is measured post-decompression and is
  explicitly and correctly left untouched.
- **The autosave/undo policy split (deliverable items 2–3) is well-reasoned**:
  making `writeCheckpointInBackground`'s container an explicit literal
  argument, and recording (not changing) `CircuitSnapshot`'s deflation as
  a heap-pressure decision unrelated to on-disk diffability, are both
  low-risk, well-scoped, and independently testable.
- **No security/licensing hazard**: this is a pure default-value and
  container-selection change; the untrusted-input hardening path (#38) is
  correctly identified as orthogonal and is not touched.
- **The #436 → #437 ordering rationale itself is sound and is corroborated
  independently on both #436 and #334**: flipping the container before the
  reference form lands would expose a Θ(N) diff artifact to git without
  fixing it, which really would produce a worse outcome than the status
  quo XZ blob.

## Bottom line

The mechanics of the proposed change are correct and low-risk once its
prerequisite lands. The issue is not currently actionable on its own
terms (Finding 3), the document's evidentiary citations are unreliable as
filed (Finding 1), and the real gating bar for pickup lives in a comment
rather than the tracked spec (Finding 2) — those three together are why
this is "sound-with-concerns" rather than "sound." None of the findings
rise to "needs-rework": the technical plan itself does not need to change,
but the issue's bookkeeping does before an executor should start.
