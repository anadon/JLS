# Issue #84: Decompose SimpleEditor — residual: extract the 9-state mouse interaction machine as GoF State objects
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the eleven-comment sediment and three distinct ends are bundled here:

1. **Testability without a display** — #316's capability statement, the pom's own
   admission that `jls.edit` is unfloored "until the #91/#84 work makes editor code
   testable" (`pom.xml:408-411`).
2. **Additivity** — new input modes (#75 keyboard construction, #401 breadboard canvas,
   the #804/#805/#806 compound-selection band) should be contributions, not surgery.
3. **Defect prevention** — the #37 class (dead code hiding in dispatch) and the absorbed
   O4 experiment (a tenth enum constant compiles clean, silently gets no message).

"Nine GoF State objects" is one candidate *mechanism* for those three ends. The issue has
promoted it to the outcome — it is in the title, in §7.4/§7.5, and in P4's
`grep "switch (currentState)" → 0`. That promotion is why the 2026-08-08 escalation
deadlocked: (a) objects and (b) a table are being argued as if the project has to pick a
noun before it can start. It doesn't. It picked the wrong seam to argue about.

## The seam that matters is toolkit vs. controller, not switch vs. polymorphism

The coupling that hurts is not `switch (currentState)`. It is that `EditWindow` is a
**non-static inner `JPanel`** (`SimpleEditor.java:1121`) that owns the gesture fields
(`currentState` at `:1218`, `x`/`y`/`moveOriginX`/`sx`/`sy`, `wireEnd`/`wire`/`net`/`prev`)
*and* the toolkit (`message`, `repaint()`, `getGraphics()`, `viewport.toModel`), and mutates
the circuit inline (`circuit.remove(we)`, `removeCoLinear()`, `markChanged()` at
`:2801-2836`). Nine package-private classes in `jls.edit`, each handed a `Context` that
exposes `message`/`repaint`/`getGraphics`, would satisfy every box in §14 and change
nothing about that. The absorbed §D already saw this — "capturing the editor would
reproduce the coupling in a new file and *pass every test in this issue while defeating its
purpose*." When a design's own completion criteria admit they can be satisfied by a
non-solution, the criteria are measuring the wrong invariant.

The invariant worth stating is one line: **the gesture logic must compile and run with no
`java.awt` or `javax.swing` on its classpath.** That is precisely the `HeadlessCoreRatchetTest`
move (#77) applied to the gui module's inside, and it is the only property that all three
ends above actually need.

## Concrete alternative: a headless `GestureController`, internal shape unconstrained

Cut here instead:

```
AWT MouseEvent/KeyStroke ──► EditWindow (adapter, stays in SimpleEditor)
                              · focus, pan, viewport.toModel, autoGrow  ← :2572-2600, already this shape
                              ▼  PointerDown/Drag/Up(modelX, modelY, button, mods), KeyCommand, Cancel
                        GestureController  (jls.edit, AWT-free, owns currentState +
                              ▼             wireEnd/wire/net/prev/moveOrigin, no Component)
                     List<CircuitOp>  +  ViewIntent(message, MessageTone, cursor, repaint)
                              ▼
                        OpSink.submitAll  /  EditWindow applies the intents
```

Consequences that the current framing does not get:

- **The (a)/(b)/(c) fork dissolves.** Whether the controller's internals are nine classes, a
  `switch` expression, or a table is a private implementation choice, reversible in an
  afternoon, requiring no maintainer adjudication and no title edit. What is public and
  load-bearing is the input alphabet, the op output, and the view-intent record.
- **#401 gets what it needs.** "A gesture machine not welded to one `JPanel`" is literally
  this signature; nine state objects that call `repaint()` and `message.setText` are not.
- **#804/#593's ArchUnit rule has an obvious subject** — one class, one package boundary,
  rather than nine types plus records that a later contributor can quietly widen.
- **The nine status strings** become a field of a value record, assertable by string, exactly
  as the absorbed §D asks — without deciding anything about polymorphism.

I'd also let the executor decide state-shape *last*, after the handlers are ported: the
right internal form is legible only once the branches are out of the toolkit.

## I am disregarding P4 (`switch (currentState)` → 0), and §7.4/§7.5's nine-class contract

P4 is a proxy metric for "polymorphism happened." Held as a gate it is actively harmful: the
strongest defect either issue documents (O4 — a tenth constant compiles clean under
warnings-as-errors) is *best* fixed by an exhaustive `switch` **expression** over a promoted
top-level enum, which P4 forbids by grep. The project already ratified sealed exhaustive
dispatch as a direction (#95, `docs/grand-architecture.md` §5 "sealed exhaustive dispatch").
A criterion that outlaws the mechanism the project chose, in order to satisfy a pattern name
in a title, is a criterion serving the noun rather than the goal. Replace P4 with: *no gesture
decision remains in a class that references `java.awt` or `javax.swing`.*

Similarly §7.5's "package-private so they do not become load-bearing API" is solving a
problem the controller cut doesn't have — one public class, everything else private.

## Second reframing: characterize first, extract second (and get a durable asset)

§8 extracts in steps and adds per-state tests afterwards; H1's behavior-preservation rests on
nine named suites being honest, which §11 concedes is a threat for `option` and `selecting`.
Invert it. Every gesture already ends in circuit state, and `DeleteGestureTest` already uses a
canonical-save oracle. So:

1. Define a trivial text `GestureScript` — a list of the input records above.
2. At the pre-change commit, replay scripts covering all nine states through the existing
   listener chain; snapshot canonical save bytes as goldens.
3. Extract. Replay the same scripts through the controller. Byte diff *is* H1.

This costs days, not weeks, and it leaves an asset the tracker wants three more times: it is
#91's cheap Layer-2 surface, it is the regression net for #804/#805/#806's new gestures, and
its record format is nearly the op stream #163 replicates. Characterization tests before a
refactor is unremarkable practice; what makes it visionary here is that in *this* codebase
the oracle already exists and the output vocabulary is already serializable.

## Where the work pulls against the arc, and one misfiling

- **Ordering is real and the body still denies it.** `CircuitOp.apply(Circuit, Graphics)`
  (`src/jls/collab/op/CircuitOp.java:51`) still demands a graphics context. Extract today and
  the states are headless only until they submit an op. The body's `blocked_by: []` is false
  three ways over (#440's floor, #337's `Graphics`-free ops, and #382). Doing this before the
  op layer is toolkit-free produces nine classes that still need a display to assert — the
  exact outcome the issue exists to prevent.
- **`app.command` (#223) is misfiled here.** A command/activation extension point is about
  which operations exist and how they are triggered — that is the landed Action layer (#75)
  plus #220's vocabulary. It has nothing to do with the nine-state gesture machine. Reading
  (a) in the 2026-08-08 comment is correct; carrying that row on this issue gates #223's
  close-out on unrelated work. Re-home it.
- **Sediment as a signal.** Seventeen comments, three of which supersede a machine block that
  was never edited, and a design fork escalated but unanswered. The issue is now
  unexecutable as written — not because the goal is wrong but because the goal was written
  down as a pattern name and everything since has argued the pattern. Rewriting the outcome
  as the AWT-free boundary makes most of the accumulated conflict moot rather than
  adjudicated.

## What I endorse unchanged

The retirement of the line-count target (correct, and the better replacement is "no gesture
decision lives in a toolkit-referencing class," not another number); the wire-cancel
unification (`:1387-1418` vs `:2801-2836` are near-verbatim twins — under the controller cut
they are one `Cancel` input, and P2 survives intact); H3's **stop** condition on
`invalidateIndex` against the Agda-backed spatial-index proofs; and the union behavior pin.
The end state in §13 — "SimpleEditor stops being the file every interaction fix must tiptoe
through" — is exactly right and is the sentence the rest of the issue should have been
derived from.
