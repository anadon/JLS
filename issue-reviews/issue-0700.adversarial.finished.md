# Issue #700: TASK-C534-3: a fired trigger opens the chronogram centred on its capture, and the capture leaves through the existing VCD path and no other
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#700 is TASK-C534-3, the third of three tasks splitting FEAT-C23-5 (#534,
PF-5 of capstone CAP-23 / #504) into the analyzer element (#696,
TASK-C534-1), the word generator (#698, TASK-C534-2), and this task: wiring
a fired trigger to the chronogram and confirming the capture leaves only
through the existing VCD writer. The split is sensible and the dependency
on #696 is correctly declared. But the issue re-exports two problems its
own ancestors already had unfixed (an unquantified, self-referential cost
tolerance; a vague "no second trace writer" claim), adds a sim-thread→UI
correctness gap the project's own architecture doc flags as mandatory
discipline, and overlaps un-coordinated with its sibling #698 on the exact
property both call "VCD-only."

## Findings, most severe first

### 1. (High) AC-4's "AC-5 closed-cost tolerance" is unquantified and circular, unfixed across three issues

> "the AC-5 closed-cost tolerance is unmoved"

This traces to CAP-23's own AC-5 (#504): *"kernel event throughput and the
first-year adder flow match baseline within **measured tolerance**"* — no
number. #696 (TASK-C534-1, this task's own declared dependency) repeats the
same self-reference verbatim: *"matches baseline within CAP-23 AC-5's
tolerance."* The sibling review of #534 (`issue-reviews/issue-0534.adversarial.md`,
finding 3) already flagged this exact defect — *"The clause names its own
criterion as the source of the number it needs... gameable: an implementer
can write any threshold they like"* — and recommended pinning a concrete
number before work starts. That recommendation was not acted on when #534
was split into #696/#698/#700: #700 imports the same unquantified tolerance
a third time, now as a non-regression bar for its own AC-4. As written, no
one can determine whether AC-4 passes, because "AC-5's tolerance" resolves
to nothing numeric anywhere in the repo (`grep -rn "K9"` and every
`docs/*.md` file confirm no threshold is recorded).
**Recommendation:** before #700 can close, a REPLAN comment on #504 must
pin a concrete number/method (e.g. "≤2% regression vs. a named timing
harness, median of N runs"); #700's AC-4 should cite that number directly
rather than re-deferring it.

### 2. (High) AC-1's sim-thread → Swing transition ignores the project's own documented threading discipline

AC-1 requires that "an analyzer firing during an interactive run opens or
scrolls the chronogram." An analyzer fires on the simulation thread — per
`ARCHITECTURE.md`: *"Interactive simulation runs on a dedicated thread (the
`"Runner"` thread)... UI work initiated from the sim thread is routed
through `SwingUtilities.invokeLater`... Follow this discipline for any new
sim-thread → UI interaction."* Opening or re-centering a chronogram panel
is exactly that kind of interaction, and the project has built tooling
specifically to catch violations of this rule (`EdtViolationDetector`,
`test/jls/ui/package-info.java`: *"The display suites run with
`EdtViolationDetector` installed... so off-EDT Swing access fails the test
that provoked it."*). #700 says nothing about this seam — not in the
Outcome, not in AC-1, not as a boundary note — despite being precisely the
kind of change the architecture doc calls out by name.
**Recommendation:** add an explicit AC (or strengthen AC-1) requiring the
trigger→chronogram path route through `invokeLater` and be exercised under
`EdtViolationDetector`.

### 3. (Medium) AC-2's "no second trace writer exists" is structurally vague and gameable

> "a test asserts no second trace writer exists for instrument captures"

There is no `VcdWriter` class to assert the absence of a second instance
of — VCD emission today is a method, `BatchSimulator.writeVcd()`
(`src/jls/sim/BatchSimulator.java:359`), tested by
`VcdExportGoldenTest`. "No second trace writer" is a structural/negative
claim about the whole `jls.sim`/`jls.edit` surface, and the issue does not
say what the test actually inspects: a reflective scan for
VCD-format-shaped code, a single-call-site assertion on `writeVcd`, or
something else. A narrowly-written check (e.g. "no second class named
`*VcdWriter*`") would pass even if an instrument capture path grew its own
inline VCD-record-formatting logic under a different name — the exact
"same bytes, same code" property the Outcome paragraph claims, undermined
by letter-of-the-AC compliance.
**Recommendation:** name the mechanism (e.g. "the capture path's only
call into VCD-shaped I/O is `BatchSimulator.writeVcd`, asserted by
[test/technique]") rather than leaving "no second trace writer" as an
unmeasured structural assertion.

### 4. (Medium) Sibling task #698 claims the identical "VCD-only, no second writer" property with no cross-reference or ordering

#698 (TASK-C534-2, word generator, filed the same day, two issue numbers
away) AC-4: *"Captures and generated stimulus export through the existing
VCD path only — no new trace format is introduced (FST explicitly out of
scope)."* #700 AC-2 makes the same architectural claim for analyzer
captures. Both are true statements about the same shared `writeVcd` code
path, but neither issue references the other, and `ordering_after` on
#700 lists only `[TASK-C534-1, TASK-C527-2]` — not TASK-C534-2 (#698).
Whichever of #698/#700 lands second either duplicates the "singular VCD
path" test or must modify the first one's test to also cover its own
element type, and nothing in either issue assigns that responsibility.
**Recommendation:** either merge the "VCD-only" assertion into one shared
test owned by whichever of #696/#698/#700 lands first (with the others
extending it), or add an explicit cross-reference/ordering note so the
second implementer knows to extend rather than duplicate.

### 5. (Low-Medium) "Opens (or scrolls)" leaves the open-vs-scroll rule implicit

> "the chronogram opens (or scrolls) centred on the capture window"

#680 (TASK-C527-2, the chronogram panel this task depends on) AC-4: *"The
panel is default-hidden."* So on a fresh interactive run the panel must be
**opened**; on a subsequent trigger with the panel already visible, it
presumably **scrolls**. #700 never states this rule — it just offers
"opens or scrolls" as an undifferentiated pair — so an implementation that
always re-opens (discarding the student's manual scroll position or
grouping) and one that only ever scrolls (leaving a fresh run's hidden
panel closed) are both arguably compliant with AC-1's literal wording.
CAP-23's own AC-1 (#504) says only *"the docked chronogram open centered on
it"* — no scroll branch — so #700 is also quietly looser than the capstone
criterion it's supposed to satisfy.
**Recommendation:** state the rule explicitly: open if hidden, re-center
(scroll) if already visible, and note this refines rather than weakens
CAP-23 AC-1.

### 6. (Low) Children's cost bands sum slightly over the parent's declared line item

#504 prices PF-5 at "3–4 mw" as a single line. Split across children:
#696 1.5–2, #698 1–1.5, #700 1 → 3.5–4.5 mw. The high end (4.5) exceeds the
parent's stated ceiling (4) by half a maintainer-week. Minor, but the kind
of drift the #504/#508 cost-review chain (cited in
`issue-reviews/issue-0504.adversarial.md`, finding 2) already criticizes
elsewhere in this same capstone's paper trail.
**Recommendation:** note the overrun in a REPLAN comment, or trim #700's
1 mw slightly given its narrow, well-bounded scope.

## What's solid

- **The dependency on TASK-C534-1 (#696)** is correctly declared — the
  analyzer element must exist before anything can consume its trigger —
  and matches the actual split of #534's original scope.
- **AC-3** (byte-identical capture across headless and interactive runs)
  follows the project's established golden-comparison idiom
  (`VcdExportGoldenTest`, `BatchSimulationGoldenTest`) and is concretely
  falsifiable as stated, independent of finding 4's naming/ownership gap.
- **The "grading path consumes the same VCD" framing is factually accurate**,
  not aspirational: `examples/autograde/autograde.py` genuinely parses the
  emitted VCD (`parse_vcd_final_values`) as one of its two documented
  grading surfaces, so #700's Outcome claim rests on a real, already-shipped
  pattern rather than a hoped-for one.
- **Scope is appropriately narrow** for `band_mw: 1` — this is an
  integration/wiring task over element and panel work that other tasks
  (#696, #680) already own, and #700 does not attempt to re-litigate either.

## Note

This session read GitHub issues #700, #504, #534, #696, #698, #680 and the
local files `ARCHITECTURE.md`, `README.md`, `test/jls/ui/package-info.java`,
`src/jls/sim/BatchSimulator.java`, `docs/batch-interface.md`,
`examples/autograde/autograde.py`, and the prior adversarial review at
`issue-reviews/issue-0534.adversarial.md`.
