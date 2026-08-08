# Issue #449: TASK-0048: a multi-module netlist imports as nested subcircuits instead of being refused, so a design written in more than one module can actually run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is "lift two refusals in `NetlistImporter`". The actual
end is: **an imported design keeps the boundaries its author drew**. That end is
right and it is load-bearing for the project's arc — #61's import stage, the
`riscv/` CPU trajectory, and `docs/capability-roadmap/lf-01-parameterization.md`,
which opens by observing that JLS's own flagship processor
(`riscv/build/addi.jls`, 228 logic elements) contains **zero** `SubCircuit`
elements (lf-01 §"The workaround, which is the strongest evidence", line 67).
A JLS that cannot receive hierarchy from outside, and whose own biggest design
never uses hierarchy inside, is a simulator whose module concept is decorative.
So: endorse the capability without reservation.

What I do not endorse is the seam the issue cuts along, or its position in the
queue. Both are stated as settled in §7.4 and §8; I am disregarding those
acceptance items, and the Open Questions built on top of them, for the reasons
below.

## Reframing 1 — build the hierarchy in the object model, not in the text emitter

The issue's whole difficulty budget is spent on invariants that the object model
already enforces for free.

Verified on `master` (the evidence-pin comment on this issue warns `2d0ca9d` is
branch-only; these are re-derived by content):

- `Circuit.save` (`/home/user/JLS/src/jls/Circuit.java:1466`) writes
  `FORMAT` **only** when `subElement == null` — a nested block structurally
  cannot carry one (`:1478-1484`, comment: "nested subcircuit blocks … never
  repeat it").
- The same method reassigns element ids densely from 0 **per block**, in stable
  order, immediately before writing (`:1497-1503`).
- `SubCircuit.save` (`/home/user/JLS/src/jls/elem/SubCircuit.java:282-289`)
  writes the nested `CIRCUIT` block by delegating to `Circuit.save`, and
  `saveFormatVersion` (`:296-301`) propagates the version requirement upward.
- `SubCircuit.init` (`:175-260`) derives the box width/height and the put set
  from the nested circuit's `InputPin`/`OutputPin` elements, sorted by name and
  alternated — with no `Graphics`/`FontMetrics` use, i.e. headless-safe.

Now compare the issue's plan: P3 asserts "no `FORMAT` line, ids restart at 0";
H3 hypothesizes that per-definition id namespacing suffices; O8/§8 change the
`Builder`'s id counter from per-file to per-definition; Open Question 3 asks how
to size an instance box and answers "reimplement `SubCircuit.init`'s arithmetic
so a realized instance looks like a drawn one". Every one of those is a
hand-rolled restatement of behaviour that lives twenty lines apart in
`Circuit.save` and `SubCircuit.init`.

**Alternative design.** Make the flat mapper produce a `Circuit`, not a
`String`, and compose hierarchy with the model:

1. Realize each reachable module bottom-up with today's unchanged flat path,
   loading its emitted text through `Circuit.load` + `finishLoad` — exactly what
   `test/jls/hdl/imp/NetlistImporterTest.java:70-72` already does headlessly.
2. For each instantiating cell: `new SubCircuit(parent)`, `setSubCircuit(child)`,
   `setImported`, `init` — the same three-line move the editor already makes at
   `SimpleEditor.finishImport` (`/home/user/JLS/src/jls/edit/SimpleEditor.java:679-697`).
3. Read `getWidth()/getHeight()` off the initialized instance and feed *that* to
   `LayoutGraph` for the parent's layout. Lay out parents after children.
4. Save the root with `Circuit.save`.

Consequences, which are the argument:

- P3 becomes unfalsifiable-by-construction instead of a test; H3 evaporates; the
  `Builder` id-counter change in §8 is deleted work.
- Open Question 3 ("how is an instance's box sized?", marked *blocks execution*)
  dissolves — the answer is "ask the element".
- P4 (bind by name, not position) stops being a risk: `inmap`/`outmap` are built
  by `SubCircuit.init` from pin names. In the text path P4 is worse than the
  issue admits — wires attach by **put name** (`String put "…"` in the emitted
  `WireEnd` blocks), and those names come from `init`'s alphabetical
  input/output alternation, so a hand-emitted `SubCircuit` block must
  bit-for-bit predict an ordering the loader will recompute anyway.
- P2 ("loads and re-saves identically") stops being the headline risk and
  becomes a corollary of `DeterministicSaveTest` / `StableElementIdTest`, which
  already pin canonical bytes for every other writer.

The blocker is self-imposed: §7.4 declares "no public API is added,
`importNetlist` keeps its signature". `importNetlist` has **zero non-test
callers** in the tree (grep over `src/` and `test/`: only
`NetlistImporterTest:56` and `ImportPipelineTest:71`). There is no compatibility
to preserve. This is the cheapest moment this seam will ever be moved, and the
issue spends that freedom on preserving a signature nobody depends on.

There is a further, larger reason. lf-01 records that JLS already maintains a
**second** implementation of the normative save format — `riscv/jlsbuild.py`,
322 lines of Python, kept honest by `riscv/test_primitives.py` — because there
is no in-tree programmatic circuit builder. `NetlistImporter.Builder` is the
**third**. Reframing 1 turns the importer into the first customer of an
object-level construction path instead of growing writer number three; #357
(definition sharing) and lf-01's parameterization will need exactly that path,
and neither can be built on top of a string emitter.

## Reframing 2 — the audience named in this issue cannot import anything today

`ImportSummary`'s own javadoc (`/home/user/JLS/src/jls/hdl/imp/ImportSummary.java:12-13`)
says it plainly: "the File->Import UI (**not built in this increment**) renders
it as the one-shot summary dialog". `JLSStart.fileImport` (`:2473-2554`) opens
`.jls` files, not netlists; there is no `-import` flag in the CLI table; the
Yosys pipeline string (`hierarchy -auto-top`, no `flatten`) exists only inside
`test/jls/hdl/imp/ImportPipelineTest.java`. Netlist import is, at HEAD, a
test-only capability.

That undercuts §"Intended Audience & Impact" — the student importing a design
she wrote has no button — and makes §9's "Manual verification with platform: one
GUI import of a two-module design" literally unperformable; the executor would
have to rebuild the O3 harness and call it a GUI import. It also weakens the
issue's most persuasive line, "the design arrives hierarchical and is then
rejected for being hierarchical": nothing *arrives* anywhere yet.

**Sequencing claim:** the highest-leverage next increment on this arc is wiring
the existing flat importer to File→Import and a batch flag, with the summary
dialog. That one change converts every mapper increment already landed — and
#448's flip-flops, and this issue's hierarchy — from latent to usable. Doing it
first also gives this task a real acceptance test instead of a probe. I would
make it a `blocked_by` edge, or at minimum file it and say so here; the issue's
`blocked_by: []` is accurate about code dependencies and misleading about value.

## Reframing 3 — measure hierarchy against `flatten`, not against a baseline that does not exist

H4 says definition duplication is the cost driver, §7.10 derives Θ(k^d), and P5
requires reporting definition and instance counts separately. The arithmetic is
right but the comparison is against a shared-definition JLS that does not exist
and is not proposed here. The status-quo alternative is Yosys `flatten`, which
produces **the same** duplicated gates minus the wrapper elements — lf-01 line
37: "JLS's hierarchy is elaborated-by-copy already". So hierarchy import does
not cost element count relative to what users do today; it buys named,
navigable boundaries at a small constant surcharge.

Say that in the issue, and change the recorded measurement (§8, §9) to the A/B
that actually prices #357 and lf-01: **element count of the same design imported
hierarchically vs. imported after `flatten`**, plus load time. As written, H4's
number tells no one anything they can act on.

## What survives untouched

The parts of this issue I would keep verbatim regardless of seam: the cycle
refusal that names the path rather than overflowing the stack (P6); the
depth/element bound carrying the **computed** figure (P7); the `legalize`
collision refusal naming both originals (P9) — correctly flagged as the
highest-severity latent defect here; unreached modules as informational, not
fatal (P10); reachability from the root only, matching `hierarchy -auto-top`
(Open Question 2). Those are policy decisions about hostile input and they are
independent of whether hierarchy is composed in text or in objects. Keep the
`top`-attribute string check byte-for-byte, too — that observation is worth the
whole §7.2 note.

One correction of fact for the executor: `CellValidator` already classifies a
non-`$` type as "a hierarchy instance of a user module" and lets it through
(`/home/user/JLS/src/jls/hdl/yosys/CellValidator.java:204-205`), so the
gatekeeper is not a third structural refusal — H2 looks safe.

## Verdict

**endorse-with-reframing.** The capability is right and overdue. Reject §7.4's
signature freeze and the §8 items that follow from it (per-definition id
counter, hand-sized instance boxes, P3 as an assertion): compose the hierarchy
through `Circuit`/`SubCircuit` and let `Circuit.save` write it, which deletes
three of five Open Questions and moves the importer onto the construction path
#357 and lf-01 will need anyway. Land — or at least file and sequence — the
File→Import surface first, so the students named in the abstract can reach any
of this. And re-aim the recorded measurement at hierarchy-vs-`flatten`, which
is the number that prices the shared-definition work this issue correctly
refuses to invent.
