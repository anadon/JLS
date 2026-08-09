# Issue #595: FEAT-C37-4: every editor refusal names the fix instead of the rule — Issie's width-inference philosophy applied to JLS's connect, width and name diagnostics, and identifier rules relaxed to what engineers actually write
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two unrelated things are bundled under one slogan ("when the editor says no, it says
what would make it yes"):

1. **Diagnostic quality** for connect/width refusals — an editor-interaction concern.
2. **The JLS identifier domain** — a cross-surface interop concern spanning
   `docs/file-format.md` §3.1, `docs/batch-interface.md`, the VCD profile, and
   `jls/hdl/HdlNames`.

They share a sentence, not a seam. The end both serve is worth funding. The route the
issue picks — write better English at the existing refusal sites, then loosen
`Util.isValidName` — is the weakest available route to either, and for (1) it is not
even sufficient to satisfy its own AC-1.

## Ground truth at HEAD

Every connect/width refusal is a gesture-time veto inside `SimpleEditor`'s anonymous
mouse-state inner class, reported by assigning a `String`:

- `src/jls/edit/SimpleEditor.java:1264` — `private String overlapMessage = "";`
- refusal sites at `:3995, :4001, :4007, :4015, :4023, :4114, :4120, :4134, :4142,
  :4150, :4233, :4239, :4247, :4255, :4262, :4291` — `"Bits don't match"`,
  `"Can't make a wire loop"`, `"Multiple connects to same wire not implemented"`, …
- rendered by `info.setText(overlapMessage)` at `:1999, :3130, :3558, :3847` — a
  transient status line during a drag.

Width itself has **no provenance**. `src/jls/elem/WireNet.java:230-233` is
`setBits(int) { this.bits = Math.max(this.bits, bits); }` and `:275-280` recomputes it
as a max-fold over attached puts. The net remembers a number, never who fixed it.

Names: `src/jls/Util.java:219-234` — letter, then letters/digits/underscore. Call sites
are eight dialogs plus `JLSStart`, each inventing its own text; the worst are
`src/jls/elem/TruthTable.java:1130` (`TellUser.error(null,"invalid name","Error")`) and
`src/jls/elem/Wire.java:475` (`TellUser.prompt(null, "Invalid name, try again")` — a
re-prompt loop that never says why).

## Why AC-1 cannot be reached by editing strings

AC-1 demands each refusal "names both disagreeing parties with their locations". At a
`"Bits don't match"` site the code holds the incoming put and `net.getBits()` — an
integer with no witness. To name the other party you must first make the model
*remember* it. So AC-1's real content is a **model change** (width unification with a
witness), disguised as a copy-editing task. Funding it as copy-editing produces
messages that say "this pin is 8 bits, this wire is 4" — which is roughly the
information already on screen — and the row gets scored HAVE while the actual gap
stays open.

Worse, the analogy to #510's Issie citation is inverted. Issie's messages are good
*because* Issie runs a whole-sheet width inference and can therefore exhibit a conflict
path. JLS never lets an inconsistent state exist: the connection simply does not
happen. Adopting Issie's message style without Issie's model is cargo cult. Decide
which philosophy you actually want, and say so in the issue.

## The reframing I would fund instead

**A. Diagnostics become values, and the fix becomes an executable op.**

The project already has the exemplar: `LoadError` (`src/jls/LoadError.java`) — fixed
category taxonomy, location, detail, actionable hint, one publication channel, pinned
by `LoadErrorReportingTest`, with `NotificationRatchetTest` keeping raw `JOptionPane`
from reappearing (ARCHITECTURE.md, "Error-reporting contracts"). Editor-time refusals
deserve the identical treatment: a `jls.core` `Diagnostic` record carrying
`{category, partyA(element, put, location), partyB, detail, repair}`. Then:

- AC-1's corpus asserts **structured fields**, not English prose — immune to wording
  churn, and far stronger evidence than "the string contains 'bits'".
- Rendering is one call site, so the status line, a future error panel, batch stderr,
  and the collab surface all agree by construction.
- The i18n non-goal (ARCHITECTURE.md, recorded 2026-07) stays intact and gets cheaper,
  because the only English left is one formatter.

The move that makes this more than a refactor: **`Diagnostic.repair` is a
`jls.collab.op.CircuitOp`**. #167/#337's op layer already guarantees `apply` then
`invert().apply` returns the canonical save to its prior bytes (`docs/operation-layer.md:21-23`).
A refusal that carries `SetBitsOp(andGate, 8)` or `InsertExtendOp(...)` does not
*describe* the reconciling edit — it *is* the reconciling edit, one click, undoable for
free. That is an IDE quick-fix, and it is the version of "names the fix" that a student
actually benefits from. It also gives the acceptance test a real oracle: apply the
repair and assert the connection now succeeds, rather than grepping a sentence.

**B. Width gets a witness, i.e. inference proper.**

Replace the `Math.max` fold with union-find over puts where each class records the put
that fixed the width. This is small (WireNet is ~500 lines and already partitions nets)
and it is the enabling change for far more than messages: width-polymorphic elements,
dialogs that stop asking for a bit width the circuit already implies, and honest
reporting for `NetlistImporter` (`src/jls/hdl/imp/NetlistImporter.java:281` already
computes exactly this kind of mismatch and reports it as prose). Do this and AC-1's
message is a projection of the model, not a composition of two ints.

**C. Reconsider the veto itself (the out-of-the-box option).**

The most Issie-like design is to *allow* the inconsistent connection, paint the net as
conflicted, and list the conflict with its repair. Benefits: the student's intent is
preserved instead of silently discarded; the diagnostic is persistent and inspectable
instead of a status line that vanishes with the drag; and the repair op has somewhere
to live. Cost: a circuit can now be temporarily invalid, which touches save, simulate
and collab. I am not asserting this is right — I am asserting the issue never
considered it, and that "keep the veto" vs. "allow and mark" is the actual design
decision hiding under AC-1. Pick one explicitly.

## The identifier half is misfiled, and its constraint list is wrong

I checked the #318 cross-flag from the dedup comment: **#318 does not own identifier
grammar.** It owns `view:instancePath:sid` addressing, net identity and per-view
geometry. So this issue does own the name domain — but its framing of the constraint is
wrong in both directions:

- **The `.jls` grammar forbids almost nothing.** Element/pin/wire names are `String`
  attributes — quoted with full escaping (`docs/file-format.md` §6), so any character
  already round-trips byte-faithfully. AC-3's round-trip criterion is nearly vacuous
  for those. The one genuine grammar rule is §3.1, and it applies to **CIRCUIT names**,
  which double as **file base names** (`Util.isValidFileName`) — so the citation AC-2
  wants there is a *filesystem* constraint, not a grammar one. Citing "the grammar" for
  it would be false.
- **The real constraints live in the batch contract, and the issue never names them.**
  `docs/batch-interface.md`: the `-t` file is whitespace-tokenized and truncated at the
  first `#` (§, lines 69-73), so a pin name containing whitespace or `#` is
  unaddressable as a test-vector signal. The VCD profile emits
  `$var wire <bits> <code> <name> [<bits-1>:0] $end` and uses `.` as the hierarchy
  separator via `getFullName` — so `.`, whitespace, and especially `[`/`]` (the very
  bus-subscript form engineers want, `Q[3:0]`) collide with the emitted syntax. These
  are the REFUSE rows AC-2 should be producing, and they are invisible from where the
  issue is looking.
- **HDL is not an acceptance constraint — it is a silent renaming.**
  `src/jls/hdl/HdlNames.java:136-159` maps every illegal character to `_` and
  uniquifies. So AC-3's "the HDL path also accepts it" is satisfied trivially and
  meaninglessly: the export accepts everything and quietly renames it. The interesting
  alternative the issue misses: **Verilog-2005 escaped identifiers (`\Q[3:0] `) and
  VHDL extended identifiers (`\Q[3:0]\`) are legal**, so relaxed names can survive to
  HDL verbatim instead of being mangled. Emitting escaped identifiers is a smaller
  change than the message work and delivers more of the stated outcome.

Concretely: `nRESET` and `RESET_N` **already pass today**. The relaxation that matters
is punctuation — `~Q`, `Q'`, `/RESET`, `Q[3:0]` — which is exactly where the batch/VCD
collisions bite. The honest shape of this half is a mini-spec: *one* identifier
character class defined once in `jls.core`, each excluded character annotated with the
surface that excludes it and the downstream reason, every call site validating against
that constant and quoting the reason. That is the "stated constraint" AC-2 is groping
for, and it is a format/interop deliverable, not a UX one.

## Alignment with the project arc, and the #316 ordering

The arc is right: JLS's recorded style is *contracts as data, pinned by goldens and
ratchet tests* (LoadError taxonomy, `SaveTags`, the CLI flag table, the batch spec).
Diagnostics-as-values is that same move applied to the last unstructured error surface
in the tree; prose-assertion tests would be the first place the project departed from
its own discipline.

On ordering, I would push back on the "waits on #316" gate. Extracting connection
legality (`canConnect`, the `overlapMessage` sites) out of `SimpleEditor` into a
headless `ConnectionRules` collaborator returning `Diagnostic`s **is** a clean first
slice of the decomposition, not something that must follow it — and it is the slice
with the best test story available today, since `test/jls/ui/EditorGestureSupport`
already drives real mouse events through the canvas. Leading #316 with this cut gives
the decomposition a worked example; waiting for #316 to finish leaves the god class one
more reason to stay whole.

## What I am disregarding, and why

I am disregarding **AC-1's "asserted against expected message content"** — assert
structured `Diagnostic` fields and the applied repair op instead; prose assertions
certify wording, not behavior. I am disregarding **AC-3's HDL clause as written** —
"the HDL path accepts it" is already true and worthless; the criterion should be
"survives to HDL unrenamed via escaped identifiers, or the rename is reported". And I
am disregarding **AC-2's "file-format or grammar constraint"** as the justification
vocabulary — the binding constraints are the batch `-t` tokenizer, the VCD `$var`
syntax, and the filesystem, none of which is the `.jls` grammar.

## Recommendation

Split into two issues: (i) *Diagnostics are values with executable repairs* — the
`Diagnostic` type, width provenance in `WireNet`, `ConnectionRules` extracted as a
#316 slice, repair ops for the width and duplicate-driver cases; (ii) *One JLS
identifier domain* — the character class specified once, each exclusion cited to the
surface that imposes it, escaped-identifier HDL emission, and the `-t`/VCD collisions
recorded as REFUSE rows in #592. Keep the bsiever-fork #4 close-or-refuse obligation on
(ii). The outcome sentence at the top of #595 survives both, unchanged.
