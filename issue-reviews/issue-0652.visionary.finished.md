# Issue #652: TASK-C565-1: a student types and edits a truth table directly, within the same bound the analysis path enforces
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Strip the ACs away and #652 is asking one question: **is `jls.elem.TruthTable` a
full citizen of CAP-31's analysis loop, or a legacy element that the new
analysis path will grow a parallel copy of?** Everything else in the task —
entry, don't-cares, the bound, keyboard reach — is downstream of that answer.
The task is well-placed in the arc: CAP-31 (#515) closes a loop, and a loop
with two different table objects in it is not a loop.

The corrected reading in the first comment is right about the facts and I
build on it rather than re-litigate: the editing surface ships
(`src/jls/elem/TruthTable.java`, `src/jls/edit/TruthTableEditor.java`,
`src/jls/edit/DisplayBool.java`), so AC-1 is green before work starts. Where I
part company is on what should replace it. The comment's answer — bolt a cap
onto `addInput`, write one converter, retrofit key bindings onto `DisplayBool`
— is three patches. There is a single move that delivers all three and leaves
the codebase smaller.

## Reframing 1: one table widget, used twice — not a view (#644) plus an editor (#652)

`DisplayBool` is a `JPanel implements MouseListener` that hand-lays `Entry`
cells and hit-tests them in a double loop (`mousePressed`, `:303`). It has no
focus traversal, no `InputMap`, no per-cell accessible name. #652 AC-4 asks for
keyboard operability here; **#644 AC-4 asks for exactly the same property on a
second, read-only table surface that has not been built yet.** Two surfaces,
two a11y bills, two rendering paths, one shared risk of drifting apart — and
the repo already carries a standing a11y contract
(`docs/keyboard-a11y-verification.md`, `docs/standards-adoption/03-accessibility-conformance.md`)
that both would have to satisfy independently.

The elegant cut: **one `JTable` over an `AbstractTableModel` backed by
`jls.elem.TruthTable`**, editable in #652's dialog and `isCellEditable() ==
false` in #644's view. Swing's `JTable` brings arrow/Tab traversal, cell
editors, selection, scroll-to-cell and `AccessibleTable` for free — AC-4 stops
being a feature and becomes a wiring exercise, and #644 AC-4 is satisfied by
the same class with a flag. Notably `grep -l "JTable\|AbstractTableModel" src/`
returns **nothing**: JLS hand-rolls every grid. That is a defensible habit for
the canvas, where the model is geometric; it is not defensible for a
rectangular grid of ternary values that a screen reader must narrate. Signal
headers become `TableColumn` headers with the existing rename/move/delete popup
attached, which also fixes the current oddity that clicking a *header* opens a
destructive menu with no keyboard equivalent at all.

This also protects the arc: `SetElementConfig`
(`src/jls/collab/op/SetElementConfig.java`) explicitly names "truth table" as an
ordered-row editor whose **commit is one op**, so the shipped dialog is already
integrated with the operation layer (#167) and therefore with collaborative
editing (#163). Any new table-editing surface built *outside* the element's
dialog-commit path silently exits that vocabulary. Reusing the element is not
just cheaper; it is the only option that stays inside the spine.

## Reframing 2: the table is already a cube list, so the 2^N bound is in the wrong place

AC-2 is the part I would disregard as written, and the reason is in the code.

`react` (`TruthTable.java:1404`) treats an input-column value of `2` as a
**wildcard** (`if (table[row][col] == 2) continue;`) and takes the first
matching row. `makeDontCare` (`:869`) collapses two rows into one and calls
`removeRow`. So the shipped model is not a dense 2^N truth table at all — it is
a **first-match product-term (cube) list** whose row count is bounded by what
the user actually distinguishes, not by 2^N. Dense allocation happens in
exactly one place: `addInput` doubles `rows` unconditionally (`:661`,
`newRows = rows*2`).

In cube terms, "add an input this table does not yet constrain" is *append one
column of `2`* — O(rows), semantically identical to the doubled table under
`react`'s matching rule, and it saves and loads unchanged (`setValue` appends
the name, `setPair` writes cells, `rows`/`cols` ride the attribute registry).
**Fix `addInput` and the row explosion this AC exists to cap simply does not
occur on the entry path.** A student can declare twelve inputs and constrain
four of them; today that costs 4096 rows, and there is no reason it should.

There is in-repo precedent for exactly this judgment call: `Memory` does not cap
the user's address width — it caps *eager allocation*
(`DENSE_CAPACITY_LIMIT = 1 << 22`, `Memory.java:1224`) and falls back to
`SparseWordStore`. JLS's settled answer to "2^N does not fit" is to stop
materializing 2^N, not to forbid N.

Where 2^N is genuinely irreducible is downstream: exhaustive simulation of
every input vector (#641/#642) and Quine–McCluskey minimization (#564, whose
blowup is worse than 2^N). Those bounds are real and belong to those stages.
Forcing the *editor* to adopt the tightest of them — which is what "entry
bounds match the analysis bounds exactly" means — makes hand entry hostage to
the minimizer and, worse, makes `jls.elem.TruthTable` (a general-purpose
simulation element with users who never touch CAP-31) narrower than it is
today. That is a regression dressed as an acceptance criterion.

**Concrete replacement for AC-2:** a single pure policy holder — the
established shape here is `DeleteKeyPolicy` / `KeyboardConstructionPolicy`
(headless, Swing-free, unit-testable) and the "the string lives here, once"
discipline already used for `entryConstraint` (`TruthTable.java:52-64`).
It answers, per stage, "what does N inputs cost, and can this stage do it",
and owns the one wording of the arithmetic. The editor refuses only where
*allocation* would blow up (and after the `addInput` fix, that is far out);
it **names the downstream stage and its arithmetic** when N exceeds the
minimizer's or the extractor's bound, at the moment the input is added rather
than at synthesis time. The student still never builds a table nothing
downstream will accept — they are told which stage says no and why — without
the editor being crippled to the weakest link.

One correctness note the bound discussion must not lose: the load path
(`setValue`/`setPair`) never goes through `addInput`, so a cap there cannot
reject existing `.jls` files. That is the right outcome (pre-fork files load
unchanged is a project invariant), but it means the bound is *not* a model
invariant — any programmatic table builder, including #653's synthesis and the
batch surface, must call the same policy explicitly. Put it in a shared pure
class or it will be enforced in exactly one of the three places that need it.

## Reframing 3: make the element's encoding the wire format, and AC-3's "conversion step" disappears

AC-3 wants an extracted table (#563) to open in the editor "without a
conversion step or a second representation". The first comment proposes #563
yield a plain value plus one converter — right instinct about not allocating a
drawable element in headless batch (#646), but it still concedes two
representations.

The stronger move: **#641 emits the table in `jls.elem.TruthTable`'s own
encoding** — input names, output names, and `int[][]` rows of `0|1|2`, inputs
first — as a plain headless record carrying no `Circuit` and no AWT. Then
"conversion" is `setTable(...)`, not translation; #655's round-trip comparison
(AC-1: "identical") is an array compare in one encoding rather than a
cross-format equivalence argument; and #564's minimizer consumes cubes, which
is what a minimizer wants anyway. The element becomes a *view* onto the
canonical table value, and the second representation the AC fears never gets
created. This decision does need to be made before #641 is executed, exactly as
the comment says.

While there: **#655 AC-2's "stated don't-care round-trip rule" already exists in
shipped code** — output `x` resolves to `0` (`react`, `:1447`), input `x` is a
wildcard under first-match precedence. #655 should pin the shipped rule as a
golden, not invent a new one that silently contradicts fifteen years of saved
circuits.

## What I would hold, and what I would drop

- **Hold:** AC-3 (one representation), AC-4 (keyboard + accessible names) —
  these are the residual and they carry the loop.
- **Hold with a new implementation seam:** AC-4 via `JTable`/`TableModel`
  shared with #644, not key bindings retrofitted onto `DisplayBool`.
- **Drop AC-1** (green on arrival, and its title invites a duplicate editor).
- **Disregard AC-2 as written.** Replace with: `addInput` appends a wildcard
  column instead of doubling; one pure bounds policy naming per-stage
  arithmetic in one wording; the editor warns with the *named downstream stage*
  rather than adopting its ceiling.
- **Banding:** `band_mw: "1"` was priced for building an editor. The reframed
  work is a `TableModel` + `JTable` swap (shared with #644, so charge it once),
  a ten-line `addInput` change plus goldens proving wildcard-column equivalence
  under `react`, and one bounds policy class. That is not a week of
  re-implementing `DisplayBool`, and the `JTable` swap should be booked against
  #644/#652 jointly or it will be paid twice.
