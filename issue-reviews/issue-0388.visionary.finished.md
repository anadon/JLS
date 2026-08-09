# Issue #388: TASK-0050 (RESIDUAL): the readability rubric becomes a gate over real imports, hierarchy instances get placed, and an import can actually be started and undone
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of apparatus, #388 asks for one thing: **make the import path real end to
end** — a user can start an import, the result is a schematic they can read, and
the readability claim is measured rather than asserted. That goal is correct and
under-served: `src/jls/hdl/imp/NetlistImporter.java:70` is today reachable only
from tests, and `LayoutMetrics`' five thresholds have never met a real import.

But the issue reaches that goal by bundling four things with different blockers,
different owners, and different epistemic status: (1) a rubric gate over the
corpus, (2) hierarchy placement, (3) a core-scale measurement campaign, (4) an
import entry point plus editor ergonomics. Two of the four have a radically
simpler route; one duplicates an open sibling under the same parent; one is not
executable and will not be for a long time. The bundle is the problem.

## Reframe 1 — import is not an editor gesture; it is a *container*

The single most important architectural fact about the shipped importer is one
the issue never uses: **`NetlistImporter` does not touch the editor at all.**
`ImportResult` carries a `String saveText()` (`src/jls/hdl/imp/ImportResult.java:14`),
emitted at `NetlistImporter.java:805` as ordinary `CIRCUIT … ELEMENT … ENDCIRCUIT`
text. Its own javadoc says it is "ready to load through the real loader".

Now read the README's central claim about `.jls`: *"The extension has meant
different container formats over time, and the loader accepts all of them by
sniffing the actual content"* — XZ, then zip, then plain text, in
`FileAbstractor.openCircuit` (ARCHITECTURE.md, "The save/load pipeline"). JLS
already owns a content-sniffing front door and treats "what format is this?" as
the loader's business.

**A Yosys netlist is a fourth thing that front door can sniff.** Register the
importer as an input the load path recognizes, and every deliverable in §7.1 and
the whole ergonomics half evaporate:

- `jls design.json` opens the imported schematic. No `-import` flag exists to
  name, so Open Question 4 — declared an execution blocker — disappears.
- File→Open with a `.json` filter is the GUI entry. No new menu action, no
  `SimpleEditor` growth, so the "record the dispatcher line count" DoD item and
  the whole friction with #84 disappear.
- "One Ctrl-Z empties the canvas" (P6, H4, `ImportUndoTest`) becomes moot: you
  *opened a document*, you did not *edit* one. Closing it is the undo. H4 — "can
  the existing undo model group an import?" — never needs to be answered.
- Every existing batch flag composes for free and unasked:
  `jls -b -t tests design.json`, `jls -i design.svg design.json`,
  `jls -savetext design.jls design.json`. That last command **is** the
  netlist→`.jls` converter, already spelled, already tested by `CliFlagTableTest`.

This is strictly more capable than the issue's design and strictly less code.
The issue's §7.1 asserts "two new external surfaces"; this route adds zero.

Two obligations the reframe creates, which the issue's design also owes but never
states: import failure must land in the **`LoadError` taxonomy** (ARCHITECTURE.md,
"Error-reporting contracts") rather than a private `ImportException` channel —
`MALFORMED` with the `CellValidator` violations as the hint is the natural
mapping, and it inherits the #38 hostile-input caps for free; and `ImportSummary`
becomes a load-time note surfaced through `TellUser`, not a bespoke modal.

If the maintainer wants an explicit flag anyway, note that O4 is misdiagnosed:
`-import` is not "eaten by `-i` under longest-match" — it is eaten because
**there is no `import` row in `FLAGS`**. `JLSStart.java:753-757` documents
longest-match precisely so `-vcd` beats `-v`. Adding the row resolves it. Open
Question 4 is answered by the table's own javadoc, not open.

## Reframe 2 — measure the layouter's growth curve, not one core

H2 ("the thresholds survive at core scale") is correctly identified as the
feature's honest unknown. The chosen experiment cannot run, and would be weak if
it could.

It cannot run because the mapper realizes exactly five cell kinds —
`$not/$and/$or/$xor/$mux` (`NetlistImporter.java:234-257`) — against nineteen the
validator accepts. No `$dff`, no arithmetic, no memory, no hierarchy. A published
core is orders of magnitude of mapper work away; #320 and TASK-0048 do not close
that gap, they open it.

It would be weak because the largest fixture buildable from five combinational
gate kinds is a **tree** — near-planar, low fan-out, no feedback. A layered
layouter passes trees trivially. §11 lists corpus bias but misses this: *a large
fixture synthesized from the realizable vocabulary is structurally the easy case*,
so passing it is near-zero evidence about $\overline{\chi}$ and $\lambda$ growth.

The seam to cut along instead: **the layouter's input is `LayoutGraph`, not a
netlist.** `LayoutGraph.connect(net, src, srcPort, tgt, tgtPort, feedback)` is
public (`LayoutGraph.java:239`). A test can generate adversarial graphs *today* —
crossbars, high-fan-in reduction trees, shift registers with real back-edges — and
plot the five metrics against $|G|$ from 10 to 10,000 nodes, with zero dependency
on #320, TASK-0048, or a named core. That yields the *shape* of the degradation
and the $|G|$ at which each threshold breaks, which is what H2 actually asks, and
it is a better answer than one measured point on one core. Open Question 2 ("which
core?") stops blocking anything.

Same lever fixes P7. The issue says the first feedback-edge assertion "Requires
#320". False as stated: *the rubric over a real import* requires #320; *the
layouter routes a back-edge outside the layer band* does not — pass `true` to
`connect` in a test. Today no caller anywhere passes `true`
(`LayoutInvariantsTest.java:33` passes `false`), so the layouter's back-edge path
and the rubric's feedback exemption are dead code. **This is the single most
valuable hour in the issue and it is currently gated behind a blocker it does not
need.** Do it first, alone, this week.

## Where the work pulls against the project's arc

- **It duplicates #290.** #290 is open, `part_of_feature: 62`, and its §8 is
  item-for-item this issue's items 2–3, down to the ~150-element fixture and the
  1-second timing bound. Both now hang off #62 after the re-home. The issue knows
  this and makes the ownership question an absolute execution blocker — which
  means a filed task that cannot start. Two overlapping tasks under one parent
  with a recorded "do not execute in parallel" is a planning defect, not a
  dependency to be adjudicated later.
- **It pushes new surface into `SimpleEditor` at the moment #84 is trying to pull
  surface out.** Note the §7.12 premise is already stale against `master`:
  `getSource()` appears twice in `SimpleEditor`, and menu items are wired through
  `editAction(EditOp.…)` (`SimpleEditor.java:605, 1313-1348`). The 305-line
  source-identity dispatcher is gone. Reframe 1 keeps it gone.
- **It re-derives ergonomics the editor already has.** `paste(Circuit from)`
  (`SimpleEditor.java:4976`) already lands a foreign circuit selected, draggable,
  and inside one undo snapshot. If import-into-an-existing-circuit is ever wanted,
  the design is `Circuit.load(saveText)` → `paste(that)` — three lines, not a new
  undo model. H4 is answered by reading the code. (Its real cost is honest and
  worth naming: `paste` refuses on name collisions, so the importer would need a
  renaming pass — still far cheaper than a second undo model.)

## What I would fund instead, in order

1. **Feedback back-edge assertion, standalone.** Synthetic `LayoutGraph` with
   `feedback = true`; assert `LayoutInvariants.check` empty and the exemption
   fires. No blockers. Retires the oldest untested path in the package.
2. **Execute #290 as written.** It already owns the corpus, the goldens, the
   compactness bound, the timing bound. Close #388's items 2–3 into it. One
   comment, no adjudication needed.
3. **Vocabulary totality (P8) as a standing guard**, with the kind set derived
   from the mapper's dispatch. This is the item with the longest half-life: it
   makes every future #320 increment carry its own layout evidence. It belongs in
   the mapper's test suite, not a layout corpus task.
4. **The synthetic scale curve** (Reframe 2) in place of P9/OQ2.
5. **Loader-sniff import** (Reframe 1) in place of P2/P6/OQ4/OQ5 and §7.1.
6. **Hierarchy placement** behind TASK-0048, filed separately. #63's black box —
   one placed rectangle — is the right shape and the issue correctly says so; that
   is a five-line placement case, not a co-equal quarter of a task.

## Explicitly disregarded acceptance criteria

I am disregarding these, and why:

- **P2, P6, H4, `ImportUndoTest`, the summary dialog, the dispatcher line-count
  DoD item, Open Questions 4 and 5.** Superseded by Reframe 1. Building a bespoke
  import entry point with its own undo grouping when the loader already sniffs
  content and `paste` already groups is duplicating two existing mechanisms.
- **P9 and Open Question 2 (core scale, named core).** Not executable at a
  five-cell mapper, and weak evidence even when it becomes executable. Replaced by
  the synthetic growth curve, which answers H2 better and answers it now.
- **"P7 requires #320."** Withdrawn as a dependency; it requires one test.

## What holds

The framing that the rubric must become a verdict rather than five constants is
right, and `boundingBoxArea`-with-no-threshold (O7) is a genuine hole worth the
compactness bound. The refusal to rewrite `HeuristicLayeredLayouter` (P10), the
refusal to relax a constant to go green (§7.11), the determinism invariant
(§7.9), and the human trace trial as a validity check on the rubric itself are
all exactly right and should survive into whatever replaces this issue. So should
the ELK in-process prohibition — it is a license fact, not a preference, and it is
already ratified in ARCHITECTURE.md's plugin-trust-boundary decision.
