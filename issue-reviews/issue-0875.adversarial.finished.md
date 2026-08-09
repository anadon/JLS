# Issue #875: TASK-C541-2: the in-tree LaTeX handout builds in CI from one bundle, and two figures in it cannot come from two runs
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#875 is the consumer/self-consistency half of a two-task split off #541
(FEAT-C24-6), filed the same day (2026-08-08) as its sibling #874
(TASK-C541-1, the command/arity half). The split itself is well-reasoned and
matches the disposition comment on #541 almost exactly. The problems are in
the acceptance criteria: one clause reintroduces wording a sibling comment
thread had just corrected on the same day, the central self-consistency
mechanism (AC-2) depends on a "run identity" concept that is defined nowhere
in the visible chain, and the CI-toolchain risk the issue itself flags
(criterion 5) carries no fallback the way every sibling task's toolchain risk
does.

## Findings, most severe first

### 1. (High) AC-1's "all five artifact kinds" reintroduces wording a same-day correction on #541 explicitly retracted

Issue #875 AC-1: *"`HandoutBundleTest` (CAP-24 AC-1) runs the bundle export
for the hazard demo, asserts **all five artifact kinds** are present..."*

But the disposition thread on #541 — the very thread that produced #875 —
contains a follow-up comment (id 5227057625, posted 2026-08-08T16:43:03Z,
about an hour before #875 was filed at 17:42:26Z) that says, in terms:

> "#727's AC-1 reads *'the bundle export for the hazard demo produces **all
> artifact kinds**'* — no count... That is not a sloppy paraphrase; it is the
> only phrasing that survives §A.4 without an edit. If #508's recommendation
> to cut PF-4 animation is adopted by `REPLAN:` on #505, a criterion that
> says 'all five' is **false as written** the moment the REPLAN lands...
> **Adopt #727's wording.** Whichever disposition... AC-1 should read
> 'produces all artifact kinds', with the count carried by the roster."

This is not a hypothetical. #508 (the product-direction review, still open,
last updated the same day) already recommends cutting PF-4 for CAP-24's
realistic scope: *"CAP-24 #505 figure-export slice — 2-3 mw slice retires the
hard risk; **cut PF-4 animation**; ≈9-14 mw realistic."* #875's own Boundary
note is aware of the mechanism ("If the PF-4 animation cut is adopted by
`REPLAN:` on #505, criterion 1's 'five artifact kinds' becomes four by that
REPLAN") — but the binding AC-1 text still hardcodes "five," precisely the
error the sibling correction was written to prevent. (Sibling #874 has the
identical defect in its own AC-1, so this is not an #875-only slip, but it is
present in #875 as filed.)

**Recommendation:** change AC-1 to "asserts all artifact kinds are present"
(matching #727's retained wording), with the count implicit in the CAP-24
roster rather than literal in the criterion.

### 2. (High) The self-consistency check's foundation — "run identity" — is undefined, and its cited authority is off-topic

AC-2 is the load-bearing criterion: *"The test reads the run identity
TASK-C541-1 records on each artifact and fails if any two artifacts in one
bundle derive from different runs, or if any artifact carries no run
identity."* This entire task is organized around reading that field. But:

- #874 (TASK-C541-1) criterion 5 only says *"Every artifact in the bundle
  records the identity of the run it derived from"* — no format, no storage
  location, no encoding. SVG, PDF, TikZ source, and WaveJSON are four
  different file formats (plus APNG if PF-4 survives); a "run identity"
  readable and comparable across all of them (especially the binary PDF) is
  a real design problem that neither task specifies.
- The phrase "recording-is-the-contract" is cited to "#498 §7.2" in both
  #874's and #875's ancestry (#541's Outcome section). Checked: #498 §7.2 is
  titled *"`docs/vcd-interop.md` and #63 — recording, not reopening"* and is
  about whether a **live interactive console session** may be the grading
  input versus its **replay transcript** — a policy about interactive-vs-batch
  grading determinism, unrelated to figure-export provenance metadata. The
  citation reuses a slogan ("the recording is the contract") from a
  different normative discussion; it establishes no format, no field, no
  mechanism for what #875 needs to read.

A contributor picking this up has no specification to implement against for
the one piece of data the whole task hinges on, and the AC's own citation
trail does not resolve it.

**Recommendation:** before #875 is workable, #874 (or a shared doc) must
specify the run-identity encoding per artifact kind — e.g., an embedded
comment/metadata field for SVG/TikZ/WaveJSON and an XMP or Info-dictionary
field for PDF — or #875 should be blocked on that being nailed down, not
just on #874 closing structurally.

### 3. (Medium) The LaTeX CI toolchain has no fallback, unlike every sibling's named toolchain risk

Criterion 5 states, correctly and self-aware: *"a LaTeX toolchain is the kind
of dependency that turns a lane into a silent multi-hour job."* Checked
in-tree: `grep -rli "texlive|latex|pdflatex" .github .devcontainer` returns
nothing — no LaTeX toolchain exists anywhere in this repository's CI or dev
container today, and `git grep timeout-minutes .github/workflows` returns
zero hits across all six workflow files (TASK-0015/#374, cited by this
criterion, is itself unlanded — confirmed via issue #374, open). So AC-5
cites a mechanism (#374's workflow-timeout ratchet) that does not exist yet,
and #875 does not order after #374.

Contrast with every sibling task that introduces a new toolchain dependency:
- #711 (PDF renderer): "if byte-identical output... cannot be achieved,
  work stops and the determinism claim is re-planned... the red transcript
  is recorded" (KC-24-1).
- #718 (WaveDrom pin): "If that cannot hold, the rendered-SVG golden is
  dropped and the WaveJSON artifact is kept (KC-24-3), with the decision
  recorded."

#875 names the risk (a LaTeX toolchain can silently balloon a CI lane) but,
unlike its cousins, names no kill criterion or degraded mode if the install
proves flaky, slow, or platform-fragile in CI — only "time-bounded," which
bounds the symptom, not the underlying risk.

**Recommendation:** add a fallback clause analogous to KC-24-1/KC-24-3 — e.g.
"if the LaTeX toolchain cannot be kept reliable/fast in CI within N minutes,
the LaTeX-build criterion is descoped to a local/manual build check, recorded
by REPLAN," and either add an explicit `timeout-minutes` on the new job
directly (not gated on #374) or add #374 to `ordering_after`.

### 4. (Medium) The "hazard demo" fixture this test depends on does not exist in-tree, and no issue in the chain owns creating it

`find`/`grep` across the repository for "hazard" (circuit files, docs, code)
returns nothing. AC-1 requires `HandoutBundleTest` to "run the bundle export
for the hazard demo" — but the fixture circuit is assumed, not owned, across
the entire visible chain: #505 (capstone) references it in its walkthrough,
#711/#722 each say "the hazard-demo circuit/run" as if pre-existing, and
#874/#875 both consume it without naming who authors it. This is a shared
risk across CAP-24, not unique to #875, but #875 is the task that actually
needs the fixture to exist and simulate a real hazard for the "figures can
disagree" defect to be demonstrable at all (a fixture with no real hazard
condition would make the planted-defect check in AC-4 less convincing). No
`band_mw` line item anywhere covers authoring it.

**Recommendation:** before work starts, confirm which issue (likely #536's
lineage) is responsible for landing the hazard-demo `.jls` fixture and its
recorded run artifact, and add that as an explicit `ordering_after` entry or
a stated assumption with a named owner.

### 5. (Low) AC-4's "recorded once" names no location or format for the negative-check transcript

*"A planted defect is recorded: regenerating one figure from a second run
and re-running the test turns it red... Recorded once, as the negative check
that makes criterion 2 non-vacuous."* This project's established convention
for exactly this kind of falsification evidence (see #374 §8/§14: "paste
both runs," and #714 AC-4: "that red run's transcript is recorded") is to
require the red-then-green transcript pasted into the landing PR. #875 says
"recorded" without saying where — a contributor could satisfy the letter of
AC-4 with a code comment and no pasted transcript, which is weaker than the
house norm the rest of this feature cluster follows.

**Recommendation:** state explicitly that the red run (and the green run
after the fix, if any code changes in response) is pasted into the PR body,
matching #711/#718/#722/#374's convention.

### 6. (Low) Cost band plausibility

`band_mw: 0.5-1` (0.5-1 maintainer-weeks) covers: consuming an undefined
run-identity format across four-to-five artifact kinds, authoring a new
in-tree LaTeX document, standing up a LaTeX CI leg from zero existing
toolchain infrastructure, writing the self-consistency assertion, and
executing + recording a planted-defect negative check. Each piece is small,
but first-time LaTeX-in-CI setup (package selection, caching, platform
quirks) historically eats disproportionate time relative to its apparent
simplicity, and this task is the one that pays that setup cost for the whole
CAP-24 cluster (findings #714 shares the LaTeX leg but doesn't establish it
first). Not disqualifying, but worth flagging as optimistic.

## What's solid (no action needed)

- The command/consumer task split matches the #541 disposition comment's
  own reasoning exactly and is independently checkable: #874's arity refusal
  needs no LaTeX toolchain; #875's LaTeX build needs no arity logic. Real
  seam, not a cosmetic one.
- AC-2's non-vacuity framing — *"Asserting only that five files exist would
  let the exact defect the feature exists to prevent pass green"* — correctly
  identifies and forecloses the classic gameable-test trap for this kind of
  consistency check. The design intent is sound even where the wording
  (Finding 1) and the underlying mechanism (Finding 2) are not yet ready.
- The boundary note correctly distinguishes this document from #714's
  CircuiTikZ sample document and explicitly allows CI-leg sharing while
  requiring each to "keep failing for its own reasons" — this is the right
  discipline and avoids a false-positive-green risk if the two were merged
  carelessly.
- Grounding the "own no exporter, own no bundle-layout" boundary against
  #874 is consistent with #874's own text and with the capstone's
  composition-only framing throughout #505/#541's history.

## Verdict rationale

`needs-rework`: the task decomposition and the self-consistency intent are
correct, but the issue as written asks a contributor to (a) implement an
AC-1 wording a sibling correction on the same thread had already retracted,
(b) build a consistency check against a "run identity" concept with no
defined format anywhere in the chain, including a stale citation to an
unrelated normative section, and (c) add a new CI toolchain dependency with
a named risk but no named fallback, unlike every sibling task's equivalent
risk. These are editorial and specification gaps, not a flawed premise —
fixable without re-scoping the task.
