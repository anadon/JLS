# Issue #800: TASK-C587-2: element pages assert their ports and parameters against the registry descriptor, and hotkey accuracy generalizes past the one table it covers today
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Two acceptance criteria, each extending an existing ratchet test:
AC-1/AC-3 (issue numbering): check documented element ports/parameters against
"the registry descriptor"; AC-2: extend `HotkeysHelpAccuracyTest`'s single-page,
non-mac check to "every documented accelerator anywhere in the content tree,"
mac included. Both existing tests must keep passing unmodified (AC-4).

## Findings, most severe first

### 1. [HIGH] AC-1's "registry descriptor" does not carry port or parameter data — the premise is false on the current tree

The issue says: *"Each element page's documented ports and parameters are
checked against the registry descriptor."* The only thing in the codebase
called a registry is `ElementType`/`ElementRegistry`
(`src/jls/elem/ElementType.java`, `src/jls/elem/ElementRegistry.java`).
`ElementType`'s fields are exactly: `tag`, `aliases`, `elementClass`,
`factory` (`ElementType.java:33-43`) — no ports, no parameters.
`PaletteEntry` (`src/jls/edit/PaletteEntry.java:27-49`) adds only `group`,
`iconName`, `fallbackText`, `tooltip`, `helpTopic` — still nothing about
ports or parameters.

The real port data is created imperatively inside each element's `init()`,
e.g. `Adder.init` (`src/jls/elem/Adder.java:103-107`):
```
inputs.add(new Input("A",this,a.x(),a.y(),bits));
inputs.add(new Input("B",this,b.x(),b.y(),bits));
```
`inputs`/`outputs` are `protected Vector` fields on `LogicElement`
(`src/jls/elem/LogicElement.java:33,35`), populated only after
instantiation *and* calling `init()` with a `TextMetrics`. Parameter data
lives in each element's overridden `savedAttributes()`
(`Element.java:316`), again an instance method, not registry state.

So there is no static descriptor to "check against" — the task actually
requires headlessly instantiating every registered element, calling
`init()`, and reflecting into `inputs`/`outputs`/`savedAttributes()`. That
is a different (and larger) mechanism than "the registry descriptor," and
the issue never names it. `ARCHITECTURE.md`'s own "Adding an element today"
list (lines 115-145) still enumerates `init`, `Attribute` entries, and the
help page as separate touchpoints — it does not describe any single
descriptor a doc-checker could diff against.

**Recommendation:** rewrite AC-1 to name the actual mechanism (headless
instantiate-and-introspect, or a new port/parameter descriptor added to
`ElementType` first), not "the registry descriptor," which currently means
something narrower (tag/alias/class/factory only).

### 2. [HIGH] AC-2's "every documented accelerator anywhere in the content tree" is untestable with the only mechanism the issue extends, and is disproven by content already in the repo

`HotkeysHelpAccuracyTest` finds accelerators with a strict two-cell-table
regex (`ROW`, `HotkeysHelpAccuracyTest.java:78-79`,
`<tr>\s*<td>...</td>\s*<td>...</td>\s*</tr>`), applied to exactly one file.
Other help pages already document accelerators in free prose, in at least
four distinct, mutually inconsistent spellings, none extractable by that
regex:

- `resources/help/editor/editing/cutcopydel.html`: *"type ctrl-X or ctrl-C"*
  — capitalized letters, unlike hotkeys.html's lowercase `ctrl-x`/`ctrl-c`.
- `resources/help/editor/editing/paste.html`: *"type the ctrl-V key"*.
- `resources/help/editor/editing/undoredo.html`: *"typing CTRL-z"* and
  *"CTRL-y (on macOS, shift-CMD-z; the old CMD-y also still works)"* — an
  all-caps convention, and a mac spelling (`shift-CMD-z`) that differs in
  case from hotkeys.html's own parenthetical (`shift-cmd-z`).
- `resources/help/editor/editing/keyboard.html`: `<b>r</b>`,
  `<b>shift-r</b>`, `<b>f</b>`, `<b>F5</b>` inline in ordered-list prose.
- Even inside hotkeys.html itself, the Watch mac variant ("cmd-w on
  macOS") is stated in a prose paragraph below the table, not in the
  table's Key cell that the current parser reads.

None of this is reachable by extending `ROW`-style parsing to "more pages."
As worded, AC-2/AC-3 either (a) requires building free-text
accelerator-extraction and normalization across at least four distinct
existing spelling conventions — a materially larger task than "generalize
past the one table" suggests — or (b) will be satisfied by an implementer
narrowly re-scoping "content tree" to mean "other pages that happen to have
a two-column table" (there are none), silently leaving the four pages above
uncovered while claiming the AC met. That is the gameable-criteria failure
mode this lens is asked to hunt for.

**Recommendation:** either explicitly scope AC-2/3 to structured
tables/lists with a named extraction grammar, or require the four
prose pages above to be enumerated as in-scope fixtures with their exact
current text quoted, so "planted wrong hotkey outside hotkeys.html" has an
unambiguous home and the checker's coverage claim is falsifiable.

### 3. [MEDIUM] Contradicts `ARCHITECTURE.md`'s stated (stale) claim that no element registry exists

`ARCHITECTURE.md:117-118` says: *"There is no element registry yet — issue
#78 will introduce one and collapse most of this."* But `ElementRegistry.java`
has existed since 2026-07-18 (`git log --diff-filter=A`), predating this
architecture line, and issue #78 (open, read in full) documents that the
registry landed months ago — #78's only remaining scope is converting four
runtime-throw stub methods to compile-time obligations, unrelated to
whether a registry exists. #800 implicitly relies on the registry already
existing (correct), but the onboarding document a contributor is told to
read first (`ARCHITECTURE.md`) asserts the opposite. #800 doesn't need to
fix this, but landing it while the doc still says "no registry yet" leaves
two contradictory statements in the tree about the exact subsystem #800
touches.

**Recommendation:** add a line item to #800 (or a prerequisite) to correct
`ARCHITECTURE.md:117-118`.

### 4. [MEDIUM] `band_mw: 0.5-1` likely undersizes the actual work once findings 1 and 2 are accounted for

The band implies "extend an existing check to more rows." Finding 1 shows
AC-1 actually needs new headless instantiate-and-introspect plumbing over
every registered element; finding 2 shows AC-2/3 needs new free-text
extraction/normalization across at least four inconsistent existing
conventions. Both are new subsystems, not row-count extensions.

**Recommendation:** re-estimate after the extraction grammar (finding 2)
and the port/parameter source-of-truth (finding 1) are actually specified;
consider whether this should split into two tasks along the same line the
outcome text already draws (ports/parameters vs. hotkeys).

### 5. [LOW] "Planted defect" framing assumes a clean baseline that doesn't exist

AC-3 (of #800) asks for one planted wrong hotkey "outside hotkeys.html" to
turn CI red. But the baseline outside hotkeys.html already contains several
pre-existing, differently-cased accelerator mentions (finding 2). The issue
doesn't say whether the new checker must first tolerate/normalize those
existing spellings (out of stated scope) or whether they should be fixed as
part of this task — that ambiguity should be resolved up front, not
discovered mid-implementation when the checker starts flagging pre-existing
prose as "wrong."

## What's solid

- AC-4 ("`HelpTopicsTest` and `HotkeysHelpAccuracyTest` keep passing
  unmodified") is a clean, mechanically checkable non-regression
  constraint.
- The planted-defect-with-recorded-CI-transcript verification discipline is
  sound and consistent with the sibling task #799's pattern.
- Scope is correctly bounded to extending two named, already-reviewed test
  files rather than inventing new test infrastructure from a blank page.

## Verdict

**needs-rework.** AC-1's stated mechanism ("the registry descriptor") does
not hold the data it claims to check against, and AC-2/AC-3's "every
documented accelerator anywhere in the content tree" is both technically
underspecified for the current table-only parser and already contradicted
by four help pages' worth of real, differently-formatted prose accelerator
mentions in the repo today. Both need concrete rewording (name the actual
introspection mechanism; name the extraction grammar and its in-scope
fixtures) before implementation should start.
