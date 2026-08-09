# Issue #564: FEAT-C31-2: a truth table&#39;s minimized sum-of-products expressions display and export — exact Quine–McCluskey within a stated bound, refused by name above it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the tier apparatus and #564 asks for one capability: **JLS should be able to
turn a Boolean function into a minimal two-level cover, exactly, or say honestly that
it cannot.** That capability belongs in JLS. It is named as a competitive loss in #510
§2, it is the analysis half of the loop CAP-31 (#515) exists to close, and the tree
already contains a costed recommendation for exactly it —
`docs/standards-adoption/11-costed-rejections.md:455-460`, "Minimization: do not write
Espresso. Recommend Quine–McCluskey with don't-care support, capped at a documented
input count (≤ 12–14 inputs per output cone)". #564 and that document agree on the
algorithm, the don't-care support, and the refusal discipline. Endorsed on substance.

What I want to change is three things about *shape*: where the capability lives, what
it produces, and what it says above the bound. All three are visible from the tree, and
two of them are load-bearing enough that the feature as written would ship a narrower
thing than the project already knows it needs.

## Reframing 1 (load-bearing): the minimizer's first consumer already ships, and it is not #563

The issue's `ordering_after` puts #564 behind FEAT-C31-1 (#563) because "its table is
this feature's input". That is only half true. JLS **already ships a truth table you
enter by hand** — `src/jls/elem/TruthTable.java` (1,491 lines), placeable, savable,
simulable, with its own editor (`src/jls/edit/TruthTableEditor.java`,
`src/jls/edit/DisplayBool.java`). Its entries are already ternary: `0`, `1`, or `2` for
don't-care (`TruthTable.java:79`).

More than that: **the element already implements one manual step of Quine–McCluskey.**
`TruthTable.makeDontCare(row, col)` (`:869`) finds the row differing only in that
column (`findMatchingRow`, `:910`), sets an `X`, and deletes the redundant row —
literally a cube merge. It is wired to a click in the table view
(`src/jls/elem/InputVal.java:41`) and pinned by
`test/jls/elem/TruthTableModelTest.java:207` (`makeDontCareCollapsesTheMatchingRowPair`).
A student minimizing a JLS truth table today does Quine–McCluskey by hand, one merge at
a time, inside JLS, and JLS checks each merge for them.

So the honest description of #564 is not "a new expression pane downstream of a new
extraction view". It is **"automate the gesture the element already offers."** That
reframing has consequences worth taking:

- It **unblocks #564 from #563 entirely.** Half this feature — the entire hand-entered
  path, which is also the only path where don't-cares can exist at all (see Reframing 3)
  — ships against code at HEAD, with no extractor, no cone analysis, no new view.
- It gives the work a **user before the capstone completes**. CAP-31 is banded 8–13 mw;
  a "Minimize" button in the existing edit dialog is deliverable inside 2 mw and is
  visible to every instructor who already teaches with the `TruthTable` element.
- Undo is **free**: table edits already flow through `CircuitSnapshot`, so minimization
  is undoable through the ordinary save/load path with no new machinery
  (`ARCHITECTURE.md`, "The save/load pipeline").

## Reframing 2 (load-bearing): produce a value, not a string — and put it in a leaf package

AC-4 asks for "exportable (plain text at minimum)". Aiming at text is aiming low, and
the tree already demonstrates the better shape. `jls.hdl` builds a typed `HdlModel` and
then renders it through `VerilogEmitter`, `VhdlEmitter`, `PcfEmitter`; the model is the
value and the text is one view of it. The minimizer should follow the same seam:

> a `jls.logic` (or `jls.bool`) leaf package — **zero dependencies, no AWT, no
> `jls.elem`** — exporting a cube representation (`long` care-mask / value-mask pairs)
> and a `SopCover` result value.

Renderers, none of which the minimizer knows about:

| Consumer | What it renders `SopCover` to | Status in tree |
|---|---|---|
| #564's own display/export | algebraic text, per output | this issue |
| #565 (FEAT-C31-3) | a drawn two-level circuit | filed |
| HDL export | `assign y = ...` instead of a `casez` chain | `HdlExporter.buildTruthTable:774` exists |
| **#83, JEDEC/GAL path** | a galette/GALasm `.pld` equation file | `docs/capability-roadmap/README.md:708`, `sweep-03-elements-and-hdl.md:59` |
| the shipped element | a rewritten, row-collapsed `TruthTable` | `makeDontCare` today |

That fifth row is the one #564 never considers and the one I would ship first: the
minimizer's most natural output is **another JLS truth table**, not prose. And the
fourth row is the argument that this is core infrastructure rather than a CAP-31
feature: `sweep-03-elements-and-hdl.md:59` records #83 as blocked on precisely "a small
two-level minimizer", and `11-costed-rejections.md:580` already costs it at 3–4
maintainer-days inside a different program. **#564 is not building a CAP-31 component;
it is building a component with at least four filed consumers.** Say so, put it in a
leaf package, and the duplication risk against #83 disappears instead of being
discovered later.

A leaf package also keeps `HeadlessCoreRatchetTest` and the `jls.sim` AWT-free rule
(`ARCHITECTURE.md`, issue #77) satisfied by construction, and makes AC-4's headless
requirement a non-event rather than a design constraint.

## Reframing 3 (correctness, not taste): "the truth table" is not a Boolean function today

This is the one place I think the acceptance criteria are not merely narrow but
mutually unsatisfiable against shipped code, and the tree already says so.

`docs/capability-roadmap/lf-04-formal-and-grading.md:175-183` states it plainly:
`TruthTable` is a **priority** structure — `react` breaks at the first matching row —
so overlapping rows mean an if-then-else chain in row order, *not* a sum of products;
and on **no** matching row it holds its previous outputs, "which makes it a latch,
which makes a combinational reference secretly sequential". I re-derived both against
HEAD: the first-match `break` is in `react`'s `PinChanged` arm, and the hold is
`TruthTable.java:~1432` (`if (matchingRow < 0) { return; }`). `HdlModel.java:592` and
`VerilogEmitter.java:396` encode the same priority semantics as `casez`.

And output don't-care is not a don't-care at all: `TruthTable.java:1447-1449`, "don't
care becomes false", `outValue = 0`, mirrored in `HdlExporter.java:792` and `:823`.

Consequences for #564 exactly as written:

- **AC-1 and AC-2 collide.** AC-2 says don't-care rows are honored; AC-1 golden-tests
  the minimized expression against exhaustive simulation of the original. If the
  minimizer treats an output `X` as free and picks `1`, AC-1 fails — because the
  simulator picks `0`. Any table with an output don't-care fails one criterion or the
  other. Neither AC mentions the other.
- **Exhaustive simulation cannot produce don't-cares.** A fully-simulated circuit is a
  totally-specified function. So AC-2's don't-care clause is reachable *only* on the
  hand-entered path — the path Reframing 1 says to build first and the issue treats as
  an aside.
- **Overlapping or incomplete hand-entered tables have no SoP** until priority is
  expanded and the uncovered patterns are named.

The fix is small and it makes the tool better before anything is minimized: before
minimizing, **normalize the table** — expand priority to a flat function, and if rows
are missing, refuse by naming the uncovered input patterns (lf-04 already identifies
that check as wanted, and it is trivial over cubes). Then state one sentence #564 is
missing: *the minimized expression is minimal for the function JLS simulates, in which
an output X is 0; a "treat output X as free" mode is offered and its result is
explicitly not equivalent to the drawn element until the four-state value core (#322)
lands.* Without that sentence the feature ships a golden test that is either wrong or
vacuous.

## Reframing 4 (the out-of-the-box one): refusal is not the only honest answer above the bound

AC-3 and KC-31-1 set up a binary: exact below the bound, refuse above it, never a
heuristic. The dichotomy is false, and CAP-09 (#306) already contains the better
vocabulary — "a proof, a replayable counterexample, or an honest UNKNOWN".

Exact two-level minimization is prime generation (iterated consensus — cheap, and its
blowup is measurable as a prime count) followed by **minimum unate cover**, which is
branch-and-bound. Branch-and-bound is an *anytime exact* algorithm: at any moment it
holds a best cover found and a proved lower bound. So above the bound the tool can
return something strictly better than "no":

> `12 terms — proved within 1 term of minimal (search budget exhausted: 2.1M nodes,
> 8,214 prime implicants)`

That is not Espresso. It is not a heuristic. It is the exact algorithm with a
certificate attached, and KC-31-1 does not forbid it — KC-31-1 forbids an
*uncertified* answer. Pedagogically it is far better than a refusal: the student sees a
cover, learns that minimality is a search, and sees the gap named. It also composes
with the rest of the project instead of inventing a private failure mode, and it makes
AC-3's "never hangs" a property of a **work budget**, not of an input-count threshold.

Which matters, because an N-based bound is the wrong bound. Prime-implicant count and
cover difficulty depend on the *function*, not on N: a 14-input function can be
trivial and an 8-input one pathological. A fixed N both refuses tractable work and
admits intractable work. Bound on primes generated and search nodes, and quote N only
as the advisory guidance `11-costed-rejections.md:455-460` already gives.

## Smaller things worth deciding while the seam is open

- **Per-output vs. multi-output minimization.** The issue says "per output" and that is
  the right classroom answer, but it should be stated as a decision, because #565
  synthesizes a two-level circuit from the result and a real PLA *shares* product terms
  across outputs. Per-output minimization plus shared-term extraction at draw time is a
  clean split; deciding it silently is not.
- **POS is nearly free.** Minimize the complement with the same engine and apply
  De Morgan. Textbooks teach both forms; the marginal cost is a wrapper. Likewise a
  K-map view — `docs/capability-roadmap/README.md:190` already wants don't-cares
  visible on a K-map — is a rendering of the same cube set, not a second feature.
- **Do not make the minimizer an extension point.** `docs/extension-points.md` and the
  #222 decision counsel against speculative seams, and a `Minimizer` point is precisely
  the crack Espresso would climb through. Keep KC-31-1 as policy in code; record the
  extension point as the revisit trigger if a course ever asks.
- **Task decomposition gap.** #648–#651 cover the algorithm, the bound, the display and
  the headless/differential test. **No task owns the table-semantics normalization** of
  Reframing 3, which is the prerequisite for #651's differential test meaning anything.

## Verdict

**endorse-with-reframing.** The capability is right, cheap, wanted by four filed
consumers, and pre-costed in the tree. Reframed: build it as a dependency-free
`jls.logic` cube library producing a typed `SopCover`; ship its first consumer as
"Minimize" inside the *existing* `TruthTable` editor, independent of #563; normalize
priority/latch/output-X semantics before claiming equivalence, and reconcile AC-1 with
AC-2 explicitly; and replace the input-count refusal with a work budget that returns an
anytime-exact cover plus a proved optimality gap — an answer CAP-09's own vocabulary
already knows how to describe, and a better one than silence.
