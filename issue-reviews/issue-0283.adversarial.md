# Issue #283: Dialog commits: route quick-edit and element edit dialogs through SetElementConfig behind the OpSink seam (op layer #167)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-grounded: every file/line citation checked
against the repo at HEAD (`53116252116b9e74bbdf64d7df1f5e08b4e1768b`)
resolved exactly as claimed (`SimpleEditor.java:747-752`,
`ClockDialog.java:77`, `ConstantDialog.java:86`, `MemoryDialog.java:368`,
`SetElementConfig.java:51` and `:40-46`, `CircuitOpReader.java:161`,
`SigGenDialog.java:71`, `TruthTable.java:439`,
`ArchitectureRulesTest.java:150`, and the `grep -rn "SetElementConfig"
src/jls/edit/ src/jls/elem/` no-match claim). That accuracy is real and
should be credited. The problems are structural: the issue's own
"Open Questions" section undercuts its stated end-state, and the
acceptance criteria as written can pass without the stated goal being
achieved for the dominant real-world case.

## Findings (most severe first)

### 1. [Critical] Conclusion contradicts the issue's own recommended default for wired elements
`SetElementConfig.requireUnwired` (`src/jls/collab/op/SetElementConfig.java:174-181`)
unconditionally rejects reconfiguring any element with a wire on **any**
put, confirmed by the existing test `CircuitOpTest` ("a wired element
may not be reconfigured (fresh puts would orphan its wire)",
`test/jls/collab/op/CircuitOpTest.java:1225-1234`). `Clock`, `Constant`,
and `Memory` — the three dialogs §2 obs 2 names as the direct-bypass
sites to migrate — are all output-bearing elements whose entire
purpose is to drive something else; in any circuit a student would
plausibly edit, they are wired. The issue's own Open Questions section
says: *"Recommended default: keep the rejection and inline fallback in
this task; revisit with evidence."* Per §7.4's contract ("on
`OpRejected`, fall back to the in-place path"), that means the
`markChanged()` bypass calls flagged in §2 obs 2 as the bug **stay in
the code, unchanged, as the fallback**, and fire for essentially every
real wired-element edit. This directly contradicts §13's stated end
state — *"every non-ordered-row dialog commit is an op"* — and the
Abstract's framing that after this fix "dialog edits" become visible
to the op seam. As scoped, they mostly don't; the issue should either
state plainly that the migration only covers unwired/floating elements
(a minority case) or make H2 (the wired-element composition) in-scope
rather than a rides-along Open Question.
**Recommendation:** rewrite §13/Abstract to state the real, narrower
scope, or fold the `RemoveWire`/`SetElementConfig`/`AddWire`
composition (H2) into this task's Method rather than deferring it.

### 2. [High] Acceptance criteria are gameable given finding #1
P1 (byte parity), P2 (undo snapshot), and the DoD bullet "existing
tests pass unmodified... except tests whose asserted behavior this
issue intentionally changes" can all be satisfied by exercising only
unwired, synthetic fixtures — exactly the shape `CircuitOpTest` already
uses. Nothing in §5/§9/§14 requires demonstrating parity, or even
counting, coverage on a **wired** element (the common case), because
that path is defined to keep falling back to the pre-existing inline
code. An implementer can close this issue, flip the `docs/operation-layer.md`
rows to "migrated" (§8's last bullet), and ship a change that alters
behavior for zero elements in any circuit a student has actually built
with a wired Clock/Constant/Memory — while every stated prediction is
green.
**Recommendation:** add a prediction/DoD item that reports the
wired-vs-unwired split observed by a representative test circuit (or
at minimum an explicit acknowledgment that P1/P2 are unfalsifiable for
the wired case under this task's scope).

### 3. [Medium] The cited safety net doesn't cover the threat it's cited for
§11 Threats to Validity: *"Dialogs constructed headlessly may shortcut
the real commit path — keep the display-tagged dialog tests in the
loop."* The only display-tagged dialog tests that exist today
(`test/jls/ui/DialogConstructionSmokeTest.java`, `@Tag("display")` at
line 54) are construct-and-cancel smoke tests — `constructAndCancel("Clock")`
(line 170), `constructAndCancel("Constant")` (175),
`constructAndCancel("Memory")` (190) — none of them drive the OK/commit
path at all (no `markChanged`/commit assertions in that file). So the
named mitigation currently offers zero coverage against the risk it's
invoked to address; every commit-path test for this task must be
written from scratch, which the issue doesn't call out as a gap (it
implies these tests are an existing safety net, not that they need to
be built).
**Recommendation:** state explicitly that commit-path coverage for
these three dialogs is greenfield, not "kept in the loop."

### 4. [Medium] Object-identity replacement after commit is unaddressed
§7.10 describes the flow as "restore pre-state → `SetElementConfig(id,
post-block)` → `OpSink.submit` → validate/apply." But
`SetElementConfig.apply` (`SetElementConfig.java:63-73`) does
`old.remove(circuit)` and then constructs a **new** `Element` instance
via `ElementBlocks.load`/`circuit.addElement(fresh)` — not the object
the dialog was editing. §7 never discusses how editor-held references
to the pre-commit instance (current selection, `SpatialIndex` entries,
any listener registered against the old object) get repointed at the
new instance. P1 (canonical-save byte parity) cannot catch a
stale-selection or stale-reference bug because it only compares saved
bytes, not live editor state after the dialog closes.
**Recommendation:** add an explicit interface note on how
post-commit selection/spatial-index state is reconciled with the
replaced instance, and a test that asserts the editor's selection
after commit resolves to the new element.

### 5. [Low] Stale/contradictory javadoc left unflagged
`SetElementConfig`'s own class javadoc (`SetElementConfig.java:20-22`)
claims to be "the commit-time op behind every attribute dialog **and
every ordered-row editor** (state machine, truth table, signal
generator)," while `docs/operation-layer.md` row 137 and this issue's
§12 both say ordered-row edits are a separate, still-deferred
`EditOrderedRows` kind. Parent #167 already tracks this exact tension
as an open, unresolved question ("SetElementConfig's javadoc already
claims 'every ordered-row editor'"). #283 correctly stays out of that
scope but never mentions the contradiction its own dependency carries,
so a contributor picking up this task from the javadoc alone could
reasonably (and wrongly) conclude ordered-row dialogs are in scope.
**Recommendation:** add a one-line pointer to #167's Open Questions
so the javadoc/doc mismatch doesn't re-surface as confusion mid-task.

### 6. [Low] Doc citation drift will be carried forward
`docs/operation-layer.md` row 135 cites `CircuitOpReader.java:157` for
the `SetElementConfig` reader case; at HEAD (and presumably at the
evidence commit) that case is at line 161 — a small pre-existing drift
in a table this issue is required to edit (§8's last bullet, §14). The
issue's own line-number discipline is otherwise excellent, but nothing
in the Method asks for a citation refresh pass over the rows being
touched.
**Recommendation:** re-derive citations in the touched
`docs/operation-layer.md` rows while editing them, per this issue's
own "line numbers drift — re-derive at pickup" principle (§11).

### 7. [Low] Process weight vs. actual behavioral surface
§14's nine-item Definition of Done (full `mvn verify`, PIT thresholds,
zero new SpotBugs exclusions, doc updates, `STATUS:` comment on #167,
etc.) is heavyweight for a change whose actual behavior-changing
surface — per finding #1 — is limited to unwired elements. Not wrong,
but reviewers approving this issue for pickup should weigh the
process cost against the narrow real-world payoff the issue itself
concedes in its Open Questions.

## What's solid

- File/line citations are exceptionally accurate — verified against
  the live repo, not just plausible-sounding.
- The falsification criteria (§10) and the "keep it inline + REPLAN"
  fallback for H1 is a sound, low-risk escape hatch if a dialog's
  state truly isn't block-representable.
- Scope boundary against ordered-row editors (§12) is consistent with
  the parent feature #167's task decomposition and dependency graph
  (no cycle; #167 lists #283 in `requires_tasks` with no back-edge).
- The `OpRejected` → inline-fallback contract (§7.11) is a reasonable,
  already-precedented pattern (matches `docs/operation-layer.md`'s
  general `submit` contract).

## Verdict rationale

`needs-rework`: not because the plan is infeasible — it is buildable
exactly as written — but because the issue's Conclusion overstates
what the work achieves (finding #1), and as a direct consequence its
acceptance criteria can be satisfied without exercising the case that
matters for real students' circuits (finding #2). Both should be
fixed before implementation starts, or the "done" state will
technically satisfy every checkbox while leaving the problem the
Abstract describes largely unsolved for wired elements.
