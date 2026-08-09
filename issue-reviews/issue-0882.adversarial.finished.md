# Issue #882: TASK-C367-1: a circuit may declare one physical time unit, recomputed from the integer tick every time — and declaring nothing saves byte-identically
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The issue is open, well-grounded in the current tree, and its central design
(an optional top-level `TIMEBASE`, non-accumulating conversion, format bump
gated on the field being set) is internally coherent and matches VCD's own
grammar. Every load-bearing code observation (O1–O6) checks out against the
checked-out working tree: `Circuit.loadCircuit`'s token loop really does
reject anything but `ELEMENT`/`ENDCIRCUIT` (`src/jls/Circuit.java:896-899`),
`BatchSimulator` really does hardcode `$timescale 1 ns $end`
(`src/jls/sim/BatchSimulator.java:423`), `docs/simulation-semantics.md:26-29`
really does call the `1 ns` mapping "nominal", `FORMAT_VERSION` is `2`
(`Circuit.java:102`) with `formatVersionNeeded()` maxing only
`el.saveFormatVersion()` (`Circuit.java:1580-1587`), a nested subcircuit
really does suppress the `FORMAT`/`CIRCUIT` header write
(`Circuit.java:1479-1484`), and `jls.core.TimeBase` really does not exist
(`src/jls/core/` holds eight unrelated geometry files, confirmed by listing).
That is an unusually high hit rate for an evidence table, and it earns the
issue real credit. The problems below are about traceability between
issues and about acceptance criteria that don't actually bind the executor,
not about whether the underlying feature idea is sound.

## Findings, most severe first

**1. The issue makes a checkable claim about #682 that is false.**
The Boundary section states: *"This task is not #682… The `blocks: [682]`
edge is recorded on both ends."* I fetched #682 (TASK-C527-3, open) and its
full machine block is:

```yaml
task_id: TASK-C527-3
part_of_feature: 527
band_mw: 1
ordering_after: [TASK-C527-2]
```

There is no `blocked_by` field at all, let alone one naming `882` or `431`.
#882's own claim that the edge is mirrored on #682 is not true today. If an
executor takes the sentence at face value and skips adding the mirror edge
to #682 (reasonable, since the issue says it's already done), the two
issues stay silently out of sync — exactly the "half-edge" failure mode
#319's evidence text elsewhere calls out as the defect a Link pass exists
to prevent. **Recommendation:** strike the "recorded on both ends" claim or
make adding `blocked_by: [882]` to #682 an explicit item in this task's own
Definition-of-Done, not an inherited assumption.

**2. #682, the one issue this task is supposed to unblock, points at a dead
issue number for the thing #882 provides.** #682's body reads: *"When the
circuit declares a physical time unit (FEAT-047 #367, TASK-0101 #431)…"*
— but #431 is `state: closed`, `state_reason: duplicate` (confirmed by
fetching it directly), closed into #367 on 2026-08-04, which is the entire
reason #882 exists as a "fresh task" in the first place. After #882 lands,
#682's prose will still send a reader to a closed duplicate rather than to
#882 (or #367). Nothing in #882's Acceptance Criteria, Boundary, or
Ordering sections requires fixing #682's stale citation — the issue only
promises a mirrored *edge*, not a corrected *reference*. **Recommendation:**
add "update #682's prose reference from #431 to #882/#367" to this task's
completion checklist.

**3. The cross-issue "Definition-of-Done" obligation on #319 is
unenforceable by anything `mvn verify` or a reviewer checks mechanically.**
Open Question 1 says the version-mechanism decision "must be recorded on
**#319 as well as here**, and that mirror is a Definition-of-Done line, not
a courtesy" — but this obligation lives in prose under "Open questions," not
in the numbered Acceptance Criteria (AC-1..AC-7). An executor can satisfy
every AC, get `mvn verify` green, and close the PR without ever touching
#319; nothing in the seven ACs or the boundary section would fail. This is
a classic case of the stated verification passing while the real goal (one
version mechanism, not two, tree-wide) silently fails. **Recommendation:**
promote the #319 mirror comment to an AC or an explicit completion-checklist
line inside #882 itself, the way #367/#431 did for themselves.

**4. AC-6 bundles one well-specified, testable requirement with one
unspecified, untestable requirement.** AC-6 reads: *"The VCD `$timescale`
follows the declared base (second golden added, existing two asserted
unchanged); **the waveform axis and delay-dialog suffixes render physical
time**."* The VCD half has a named test vehicle (a new
`VcdExportGoldenTest`-style golden). The GUI half — `Trace.java`'s axis
labelling and the delay-dialog suffixes — names no test at all, and
`ARCHITECTURE.md`'s own test-layout section says the UI harness's "Layer 1
(present) is headless model assertions" only; layers 2/3 (Swing-under-Xvfb,
render-to-image) are "reserved," i.e. not built. There is today no
automated way to assert that an axis label actually says "ns" instead of a
raw tick count. An executor can ship AC-1 through AC-5 and AC-7 complete,
skip the GUI label work entirely, and nothing catches it before a human
eyeballs a screenshot that this issue doesn't even require taking (compare
#431's evidence text, which explicitly asked for "one screenshot of the
waveform axis labelled in physical time" as manual verification — that line
did not carry over into #882's Acceptance Criteria). **Recommendation:**
either name a concrete verification step for the GUI half (a manual
screenshot requirement, as #431 had, restated explicitly) or split AC-6 into
a testable VCD criterion and a separately-tracked GUI criterion so the gap
is visible rather than silently absorbed into one bullet.

**5. The "block-structure justification" AC-7 asks for is not actually
supplied by the policy it's amending.** `docs/file-format.md` §9's existing
taxonomy (confirmed at lines 420-466) only names two bump triggers: a new
*item kind* other than `int/long/Int/String/ref/pair/probe`, or "any change
to the block structure, escaping rules, or the meaning of an existing
record." A brand-new top-level record (`TIMEBASE`, a sibling of `CIRCUIT`
and `FORMAT`, not an item inside an `ELEMENT`) doesn't cleanly fall under
either bullet as written — it's neither a new item kind nor a change to an
*existing* record's meaning. AC-7 correctly requires this gap be closed
("the block-structure justification for the bump written down"), but
because the justification text doesn't exist yet, an executor could satisfy
AC-7 with a single perfunctory sentence that doesn't actually extend §9's
taxonomy to cover "new top-level record kind" as a category, and no test
would catch the difference between a real taxonomy extension and a
one-off special case. **Recommendation:** name the specific taxonomy
extension explicitly (e.g. "add a third §9 bullet: a new top-level record
kind always bumps") rather than leaving the wording to the executor.

**6. Feasibility/scope note (not a defect, but worth surfacing).** AC-1
through AC-7 collectively touch: a new value type (`jls.core.TimeBase`),
the loader's token grammar, the format-version predicate, the VCD emitter
plus a new golden, GUI label rendering in at least two places (waveform
axis, delay dialogs), three normative docs files, `FileFormatSpecTest`, and
the CHANGELOG — all gated by an open, unresolved "Open Question 1" (whole-file
bump vs. riding #319's future per-section flag) that the issue itself says
"blocks execution of the version-policy step." This is a lot of surface for
one task in a "2-3 mw" band, and the GUI-rendering slice (finding 4) is the
part most likely to get silently dropped under that pressure. Not a
should-not-proceed item, but worth the executor budgeting time for the
untested GUI half explicitly rather than assuming AC-1..AC-5/AC-7 = done.

## What's solid (no further action needed)

- The core design — optional field, absent-by-default, recompute-not-accumulate,
  version bump gated on the field being set — is internally consistent and
  matches the existing `FORMAT`-suppression precedent for subcircuits
  (`Circuit.java:1479-1484`, O5) and the existing conditional-bump precedent
  for version 2 itself (`docs/file-format.md`'s own version-2 entry: "Writers
  emit `FORMAT 2` only for files that contain a vertical group").
- `blocked_by: []` is honest: every file, class, and doc section the issue
  cites exists on the checked-out tree today: `src/jls/Circuit.java`,
  `src/jls/sim/BatchSimulator.java`, `docs/simulation-semantics.md`,
  `docs/batch-interface.md`, `docs/file-format.md` §9, `test/jls/FileFormatSpecTest.java`,
  and the two VCD goldens in `test/jls/VcdExportGoldenTest.java`
  (`clockedRegisterVcdMatchesGoldenByteForByte`,
  `testVectorStimulusVcdMatchesGoldenAndCoversHiZ`) all resolve as described.
- AC-2's dual assertion (`seconds(n) == seconds(1) * n` exactly, *and* that
  naive summation is *not* equal at some scale) is a genuinely well-designed,
  hard-to-game test: it forces the executor to demonstrate the floating-point
  hazard exists rather than merely asserting an inequality that could pass
  vacuously.
- The exactness-limit arithmetic (2^53 ticks ≈ 2.5 hours at 1 ps, 2^63 ticks
  ≈ 106.8 days; 292 years / 104 days at 1 ns) is numerically correct — verified
  independently.
- The provenance chain (#431 → closed duplicate into #367 on 2026-08-04 →
  #882 filed as the "fresh task") checks out exactly as narrated when both
  issues are fetched directly.

## Verdict rationale

`sound-with-concerns`: the technical design is well-evidenced and internally
consistent, but findings 1-3 are concrete, checkable defects in the issue's
own cross-references and process obligations (a false claim about a mirrored
edge, a stale downstream citation this task won't fix, and an unenforced
Definition-of-Done item), and finding 4 is a real gap between what AC-6
claims to verify and what it actually can verify given the project's current
UI-test ceiling. None of these block starting the work, but all four should
be closed before the task is marked done, or the "nothing happened for
existing users, everything happened for the tree's own bookkeeping" promise
this issue makes about itself will not actually hold.
