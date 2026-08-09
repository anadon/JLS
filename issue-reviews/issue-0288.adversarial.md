# Issue #288: GUI HDL export: a File-menu Export entry over the existing HdlExporter, keyboard-reachable and harness-drivable
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The core idea is small and well-bounded: add one `JMenuItem` to `fileMenu()`
that calls the existing headless `HdlExporter.export` and writes the
result, with no changes inside `jls.hdl`. That boundary is real and the
citations to `HdlExporter.java`, `JLSStart.java`'s `-export` handling, and
the "Export Image" precedent all check out against the checked-out tree.
The problems are in the acceptance criteria and in an unresolved
contradiction between this issue and #386, both of which are picked up
below with file/line evidence.

## Findings, most severe first

### 1. (High) Undisclosed contradiction with #386 on whether the export may block the EDT
Issue #288 §7.9 is explicit and prescriptive: *"Synchronous, EDT-only,
matching the existing 'Export Image' item. If export of large circuits
proves slow enough to jank, that is a recorded deviation, not a silent
SwingWorker addition."* That is a instruction not to add async handling.

But #386 (TASK-0051), whose contract this issue's own comment (2026-08-08)
says #288 "should adopt verbatim," states the opposite in its own §7.9:
*"The editor action runs on the EDT and must perform the export off the
EDT if it is long enough to block, matching whatever the existing
long-running editor actions do."*

The ownership comment enumerates exactly five items to adopt from #386
(suffix handling, byte-identical output, menu placement, the display-tag
test, and the `ProcessBuilder`-stays-out-of-`src/` invariant) and
concurrency is not among them — but the comment's framing ("the contract
the menu item must satisfy... adopt verbatim") is broad enough that a
reader could reasonably take the whole of #386's §7 along with it. An
implementer following #288's own body builds a synchronous EDT action and
treats jank as an accepted, recorded deviation; an implementer who also
reads #386 as instructed adds off-EDT handling to avoid exactly the jank
#288 just said was acceptable. Two contributors implementing this issue in
good faith from the stated inputs can produce contradictory code, and
neither is unambiguously wrong per the text as written.
**Recommendation:** resolve explicitly in #288's body (not only by
reference) whether off-EDT execution is required, permitted, or
disallowed, and strike the conflicting sentence in whichever issue is not
authoritative.

### 2. (High) `MenuBarSpecTest` is a golden full-menu-tree test that this issue never mentions, and adding the item breaks it
`test/jls/ui/MenuBarSpecTest.java:67-79` hardcodes the entire File menu as
a literal text block (`EXPECTED_MENU_TREE`), including the exact line
`\tExport Image` immediately before `\tClose`. Issue #91 (§ Background)
independently confirms this is a delivered, load-bearing acceptance test:
*"P3 (menu-bar expectation table) — delivered: `test/jls/ui/MenuBarSpecTest.java#L56` renders the full menu bar ... against a declared table."*

Adding "Export HDL…" to the File menu, per §8's first checklist item,
necessarily changes the rendered tree and fails this test until
`EXPECTED_MENU_TREE` is edited — yet #288's §8 Method checklist never
mentions `MenuBarSpecTest`, and its Completion Criteria include the line
*"Existing tests pass unmodified."* Taken literally that criterion is
false for this change: an existing test **must** be modified (the golden
table updated) for the suite to stay green after the new item lands. The
issue conflates two different things — "assertions in already-passing
tests don't need to change to make this land" (true for almost everything
else) and "no existing test file needs an edit" (false, specifically for
this one file) — without ever naming the one file where it's false.
**Recommendation:** add `test/jls/ui/MenuBarSpecTest.java`'s
`EXPECTED_MENU_TREE` update to §8's checklist explicitly, and rephrase the
DoD bullet so it doesn't read as contradicted by the PR's own diff.

### 3. (Medium) "stable component name per docs/component-naming.md" cites a document with no scheme for File-menu items
`docs/component-naming.md` defines exactly two naming families: palette
buttons/mirror-menu items (`palette.<slug>`, `menu.elements.<slug>`) and
element dialog fields (`dialog.<slug>.<field>`). It says nothing about
menu-bar items under File/Edit/Element/Simulator. Confirmed by
`grep -n setName src/jls/JLSStart.java`, which returns exactly one hit —
`circ.setName(textSaveName)` on a `Circuit`, unrelated to any `JMenuItem`
— i.e. **no File-menu item in the current tree carries a stable component
name today**, despite #75's claim that "menu surfaces carry stable
component names for the #91 harness." P3 asks the implementer to add one
"per docs/component-naming.md," but the document simply has no row for
this case, so whatever name is chosen (`menu.file.exporthdl`? `file.exportHdl`?)
is invented on the spot, not derived from a documented scheme the way the
issue implies. The "headless source-scan pin test" §8 asks for can only
pin whatever ad hoc string the implementer picks — it verifies internal
consistency, not conformance to a spec, because no spec for this artifact
type exists.
**Recommendation:** either extend `docs/component-naming.md` with a
`menu.<menuSlug>.<itemSlug>` convention as part of this task (small, and
it removes the ambiguity for every future menu item too), or drop the
"per docs/component-naming.md" wording and just state the literal name to
use.

### 4. (Medium) P2/P4's "byte-identical to the CLI" claim has no test for the unrecognized-extension case, which is exactly where GUI and CLI are likely to diverge
The CLI hard-validates the export suffix at parse time and exits with a
usage error for anything other than `.v`/`.vhd`/`.vhdl`
(`JLSStart.java:1088-1092`: `usageError("option -export output file must
end in .v, .vhd or .vhdl: " + opnd)`, exit status 2). `Export Image`'s
`JFileChooser` precedent this issue asks to mirror
(`JLSStart.java:2992-3025`) only ever offers one fixed extension (`.jpg`)
and coerces the typed name by stripping/asserting that suffix — it has no
analogue for "choose between two-plus extensions and reject anything
else." Swing `JFileChooser` also does not reliably prevent a user from
typing a filename with no extension or an unrelated one even when a
`FileFilter` is installed. Nothing in §5 (Predictions) or §8 (Method)
asks for a test where the chosen path doesn't end in `.v`/`.vhd`/`.vhdl`.
An implementation that silently defaults to Verilog (or silently accepts
any extension and always emits Verilog text into it) would pass every
stated prediction — P2 and P4 as tested only cover the "correct extension
chosen" path — while diverging from the CLI's reject-and-exit-2 behavior
for the untested case, quietly breaking the "byte-identical … for the
same circuit" claim for real users who mistype an extension.
**Recommendation:** add an explicit prediction/test for an
unrecognized-or-missing extension in the GUI chooser, and state whether
the expected behavior is "reject, matching CLI" or "silently default,"
because right now neither is specified.

### 5. (Medium) The falsification/escalation path names a closed issue as an active owner
§10 (Falsification Criteria) says: *"If the GUI path cannot produce
identical output without modifying `jls.hdl`, H1 is wrong — stop, comment
here and on #75, and re-scope with #59 (which owns exporter semantics)
before touching the exporter."* Confirmed via the GitHub API: **#59 is
CLOSED** (it is #60's parent, and #60 itself closed 2026-07-09). The issue
never acknowledges this — §12 (Related Work) still describes #59 in the
present tense as the semantics owner. Re-scoping "with" a closed issue
means either reopening it (undiscussed here) or routing the finding
somewhere else undefined by this text. Low probability of triggering
(H1 is likely to hold, per the Observations), but the escalation path as
written doesn't actually work if it's needed.
**Recommendation:** name the live issue (or milestone) that inherits #59's
authority for future exporter-semantics disputes, or state that #59 would
need reopening.

### 6. (Low) Ownership ruling lives only on #288; #386 has not (yet) been amended to match
The 2026-08-08 comment on #288 rules that #288 (not #386's `P3`/
`test/jls/edit/HdlExportMenuTest.java`/§8 menu step, not #758's AC-5) owns
this deliverable, and says #386's items "should be discharged by a
`STATUS:` comment pointing here." Fetching #386 directly shows its body
and Definition of Done still list the full menu-item deliverable
undiminished — including its own checkbox *"The GUI export action
produces byte-identical output to the CLI for the same circuit and
suffix"* — with no discharge comment posted there as of this review. The
boundary is sound in principle (lowest-number-wins is a reasonable rule,
and the content match is real) but is currently asymmetric: anyone
picking up #386 without independently finding the comment on #288 would
still see a fully-specified, undischarged menu-item task and could
duplicate the work. This is a process risk, not a defect in #288's own
design.
**Recommendation:** the discharge comment on #386 should land before (or
atomically with) work starting on #288, not be left as a "should."

### 7. (Low) Cited line numbers have already drifted by one line
§1 cites the Export Image precedent at `JLSStart.java:1524-1528`; at the
pinned-and-current checkout it starts at line 1525 (`JMenuItem exportItem
= new JMenuItem("Export Image");`). The issue itself anticipates this
("Line numbers ... drift ... re-derive before executing (rule 6)"), so
it's self-disclosed and not a real defect — noted only so a reviewer
doesn't assume the citations were re-verified for this cycle without
checking.

## What's solid (one line each)

- The no-`jls.hdl`-changes boundary (H1) is real and checkable: `HdlExporter.export`/`buildModel` are already GUI-free and public, so the GUI path is pure composition, not new logic.
- The write pattern asked for (temp file + atomic rename, nothing partial on failure) matches the existing CLI export path at `JLSStart.java:438-462` almost exactly, so there's a working template to copy.
- P4's "rejection surfaces every offender, writes nothing" requirement matches `HdlExporter`'s documented, already-tested policy (`HdlExporter.java:87-91`, `HdlPolicyTest#rejectionListsEveryOffenderInOneMessage`) — nothing new to invent there.
- Explicitly deferring board/pin-constraint UI (#213) and exporter semantics (#59) keeps this task's surface area genuinely small; that scope discipline is stated and held throughout the issue body.
- The falsification gate ("if H1 is refuted, stop and re-scope before touching the exporter") is a good discipline in principle, even though its named escalation target (#59) is stale (Finding 5).
