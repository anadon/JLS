# Issue #652: TASK-C565-1: a student types and edits a truth table directly, within the same bound the analysis path enforces
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue's own comment thread already ran an adversarial pass and caught
the headline defect: AC-1 asserted work (`jls.elem.TruthTable` +
`jls.edit.TruthTableEditor`) that is already shipped on master. I
independently verified that finding against the checked-out tree at
`5b05d67` (with `8288226`, the comment's repinned evidence commit,
confirmed an ancestor) and it holds. That correction is good work, but it
is incomplete: it introduces a new, unresolved contradiction with a
sibling issue's body, and it leaves the one criterion it left standing
(AC-2) referencing a bound value that does not exist anywhere in the
tracker. Neither the original issue nor its two corrective comments fix
either problem.

## Findings, most severe first

### 1. The corrective comment's own dependency fix collides with #644's unedited body

Comment 1 (2026-08-08) rewrites the `ordering_after` guidance: *"#644's
view is not what this makes editable — an editable surface already
exists. … Read it as `ordering_after: [641]`."* But #644 (TASK-C563-3,
the issue #652's own body names as its dependency) has its own boundary
note, unedited and still live: *"Table editing for synthesis is #565's
TASK-C565-1 \[i.e. this issue\] — read-only here."* That is #644
asserting the exact relationship comment 1 just declared false. Neither
issue's body has been updated to resolve the conflict, so a reader who
opens #644 gets told #652 makes its view editable, and a reader who opens
#652's comments gets told the opposite. This is the identical failure
mode #563's adversarial review flagged in that issue (finding #1: "the
issue body still asserts a dependency graph its own comments say is
false") — it recurs here, one hop over, uncaught by the correction that
just happened. **Recommendation:** before pickup, reconcile #644 and #652
in the same pass — either edit #644's boundary note to drop the
"editable via #652" claim, or edit #652 to explicitly supersede it, not
just assert the new reading in a comment on the other issue.

### 2. AC-2's bound is unspecified anywhere in the graph it cites

> "AC-2: Above N inputs the editor refuses with the row-count arithmetic
> and never begins allocating rows (CAP-31 AC-3), using the same bound as
> #563 and #564."

`N` is never given a value. Tracing every issue this depends on:
CAP-31/#515 AC-3 says only "above N inputs the tool refuses" (also
unbound); #642 (TASK-C563-2, the task that actually owns "the bound's
numeric value is a stated decision recorded with this task") states no
number either; #563's own adversarial review independently flagged the
same gap ("No value or formula for N appears in #563, in the parent
capstone #515, or in #872"). #652 doesn't even cite #642 — the task that
is supposed to be the source of truth for the number — only the parent
features #563/#564 in prose. As written, an implementer can pick any N
(including a degenerate N=1) and "use the same bound as #563 and #564"
trivially, because there is no bound on record to match against. This
makes AC-2 untestable in isolation and gameable: a CI check asserting
"refuses above N" passes for any self-consistent N the implementer
invents, and nothing catches drift between this editor's N and whatever
#563/#642 eventually ship. **Recommendation:** do not schedule AC-2 work
until #642 (or #515 AC-3) pins a concrete N or formula; until then, add
an explicit blocking note here rather than "using the same bound" as if
one already exists.

### 3. AC-3's "no conversion step, no second representation" is unverified against what #563/#641 will actually produce

> "AC-3: An extracted table (#563) opens in the editor and edits without
> a conversion step or a second representation."

Comment 1's own recommended default contradicts the letter of AC-3: *"The
recommended default is: #563 produces a plain value (a table plus its
frontier), and this task supplies one conversion into
`jls.elem.TruthTable`."* A conversion routine — from #641's extraction
value into a `jls.elem.TruthTable` instance — is exactly a "conversion
step," even if it's a thin one. AC-3 as literally worded would be
satisfied only if #641's output *is* a live `TruthTable`, which comment 1
explicitly argues against (for good reason: materializing a drawable GUI
element in headless batch mode, #646, is the wrong coupling). So the
criterion and the comment's own recommended design are in tension: build
to the comment's advice and AC-3 fails a literal read; build to satisfy
AC-3 literally and you get the coupling problem the comment warns off.
**Recommendation:** reword AC-3 to say what's actually meant — "one
documented conversion from #641's table value into `jls.elem.TruthTable`,
exercised by a round-trip test with #641's fixture" — rather than "no
conversion step," which nothing in the corrected design will satisfy.

### 4. AC-4's "keyboard-operable end to end" has no acceptance bar, and the corrected scope narrows it without re-specifying one

Comment 1 narrows AC-4 to `DisplayBool`'s missing keyboard/a11y surface,
which I confirmed: `src/jls/edit/DisplayBool.java` implements only
`MouseListener` (`:39`, `:59`) — no `KeyListener`, no focus-traversal
override, no `AccessibleContext` anywhere in the file. That narrowing is
correct and evidence-backed. But neither the original AC-4 nor the
comment states what "keyboard-operable end to end" means operationally
for a cell grid: which keys move focus (arrows? Tab per-cell, which would
be ~2^N × cols stops for a large table?), what key toggles a cell through
0/1/x, whether Tab should even enter the grid cell-by-cell versus
row-by-row. `ARCHITECTURE.md`'s UI test-layer note (`test/jls/ui/`) says
layer 2 (Swing harness under Xvfb) is "reserved," so there's no existing
harness pattern this can point to for what "passes." Without a concrete
interaction spec, "keyboard-operable end to end" can be satisfied by
wiring Tab-to-next-cell and calling it done, while leaving unusable an
N=8 table with 256 rows. **Recommendation:** pin the key bindings and
focus model explicitly (e.g., arrow-key cell navigation + a toggle key,
not Tab-per-cell) before this is picked up, and state which existing
`test/jls/ui/` layer the verification lands in.

### 5. `band_mw: "1"` is stale for the corrected scope, and no comment re-estimates it

Comment 1 flags this directly ("Re-band at pickup; do not spend a week
re-implementing `DisplayBool`") but doesn't give a number, and neither
comment updates the machine block's `band_mw: "1"`. The corrected scope
— an unspecified bound (finding #2, blocked on another issue), a
conversion routine whose target shape is still unresolved with a sibling
issue (finding #1), and a full keyboard/focus/a11y retrofit of a
mouse-only `JPanel` (finding #4, itself underspecified) — is a
plausible-but-unverified 1 mw. Since two of the three deliverables are
blocked on external decisions that don't exist yet, the honest estimate
right now is "unbandable until #642 pins N and #644/#652 stop
disagreeing," not "1." **Recommendation:** either re-band explicitly or
mark the issue blocked rather than leaving a stale point estimate that
will anchor whoever picks it up.

## What's solid (no action needed)

- The core AC-1 correction (already-shipped feature identified and
  re-scoped) is accurate: `addInput`/`addOutput`/`toggleOutput`/
  `makeDontCare`/`undoDontCare`/`renameInput`/`moveInput*`/`getTable` all
  verified present in `src/jls/elem/TruthTable.java`, registered in
  `ElementRegistry`/`SaveTags`, exercised by `AllElementsRoundTripTest`.
- The observation that `TruthTable.addInput` has no ceiling today (`grep
  -n "MAX\|maxInputs" src/jls/elem/TruthTable.java` → nothing) is
  correct and is a real, well-scoped gap worth closing once N exists.
- The evidence-pin correction in comment 2 (repinning `07a0bea` to
  `8288226` after catching it wasn't on master) is itself good practice —
  I re-verified `8288226` is an ancestor of current HEAD (`5b05d67`)
  before trusting either comment's citations, per that comment's own
  advice.
- AC-2's "never begins allocating rows" clause (refuse before the
  `addInput` array-doubling in `TruthTable.java:658` onward runs) is a
  concrete, implementable, testable shape for *how* the refusal must
  behave, independent of the still-missing N.
