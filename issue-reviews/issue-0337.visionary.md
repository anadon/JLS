# Issue #337: FEAT-015 (RESIDUAL): every circuit mutation applies with no windowing system present, and a program can build a circuit without emitting save text
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the two task rows away and the claim underneath is: *a circuit should be a value a
program can compute with, deterministically, without a display.* Four capstones (#297,
#299, #300, #304) all need that one thing. The `java.awt.Graphics` parameter on
`CircuitOp.apply` is a symptom of it, not the thing itself, and the issue's framing —
"substitute `TextMetrics` for `Graphics`" — quietly preserves the actual obstacle while
removing its most visible token.

The obstacle is that **element geometry is a function of the host's font metrics**, and
geometry is saved. `src/jls/elem/Pin.java:135-159` and `src/jls/elem/Register.java:199-220`
are the pattern: `width = Math.max((fm.stringWidth(" " + name + " ") + s/2)/s*s, 2*s) + …`.
Sixteen classes in `src/jls/elem/` call `stringWidth`; only three
(`WireEnd`, `Gate`, `Element`) override `sizeIsRecomputedOnLoad`
(`src/jls/elem/Element.java:667-679`), so the other thirteen **persist** the pixel numbers
they derived from whatever font the creating machine had. Two peers running #171's
replication, or a grader and a student, apply the identical `AddElements` and get different
saved bytes. That is a divergence bug the issue never names, and it is exactly the thing
§5 criterion 1 (GUI/headless byte parity) will collide with the first time it is written.

## Reframing 1 — delete the parameter instead of retyping it

Look at what `Graphics` is used *for* inside the op layer. Every single use is one
expression:

```
src/jls/collab/op/AddElements.java:61       el.init(SwingTextMetrics.forGraphics(g));
src/jls/collab/op/FlipElement.java:26       rot.flip(SwingTextMetrics.forGraphics(g));
src/jls/collab/op/RotateElement.java:31     …, SwingTextMetrics.forGraphics(g));
src/jls/collab/op/SetElementConfig.java:66,120  fresh.init(SwingTextMetrics.forGraphics(g));
```

Nothing draws. `Graphics` is a font-metrics factory and nothing else, and the sink it feeds
(`Element.init(@Nullable TextMetrics)`) already accepts null — which is why `JLSStart`
already runs the whole headless batch surface through `finishLoad(null)` at five call sites.
So TASK-0037's stated content is roughly a day of typing, not a two-week row, and the 4-7
week band is priced against the wrong risk.

The interesting move is one step further out: **make the model's sizing deterministic and
the metrics parameter disappears entirely**, leaving `void apply(Circuit circuit)`.
Concretely, put a canonical metric table in `jls.core` (the ambient editor font's advance
widths, fixed by the project, versioned) and have `init`/`rotate`/`flip` measure against it;
`jls.edit.SwingTextMetrics` survives only for *drawing*. Three things fall out:

- §5 criterion 1 stops being a test and becomes a structural property. Under the issue as
  written, byte parity between the GUI path and a headless JVM is only true if the headless
  `TextMetrics` returns the same integers as the user's live `FontMetrics` — which is not a
  property anyone can guarantee across platforms, DPI, and font substitution. The issue's
  own §3 equation ("∀ g₁, g₂ … ⟹ the parameter is removable") is asserted, not established;
  today it is false for the thirteen classes that persist their size.
- #171/#169 get op determinism for free, which they need and which is not currently on
  anyone's plan.
- `jls.collab` enters the headless ratchet trivially: the only forbidden imports in the
  package are `java.awt.Graphics` and `jls.edit.SwingTextMetrics`
  (`test/jls/HeadlessCoreRatchetTest.java:56-58` forbids both), and this deletes both.

The invariant-1 risk ("saved bytes do not move") is smaller than it looks, and checkably so:
sizing runs only under `if (width == 0 && height == 0)`, i.e. at element *creation*, never on
load of an existing file. Every tracked fixture and golden re-saves byte-identically by
construction; only newly created elements could land in a different grid bucket, and the
`(w + s/2)/s*s` quantization to `Geometry.SPACING` makes even that rare for the default font.
The "labels might overflow a canonically-sized box" objection is not a new failure mode
either: a file created on a wide-font machine already opens on a narrow-font machine with
its stored width intact. This change makes an existing nondeterminism deterministic. The one
genuine open question is `Text`, which chooses its own font family — `TextMetrics.Provider`
should survive for that single case (and `Text` persists its size anyway).

## Reframing 2 — the verb set is fighting the architecture it sits on

§1 item 5 wants "the emit-text-and-reparse idiom … gone from the in-tree generative paths",
and §5 criterion 3 wants `CircuitTextBuilder`'s 24 files down to zero. But the op vocabulary
*is* built on emitted save text: `AddElements(List<String> blocks)`
(`src/jls/collab/op/AddElements.java:38`), where each block is "the exact byte form the
element's own save method writes" — deliberately, so an added element is indistinguishable
from a loaded one. Any construction verb set must therefore emit element blocks and hand
them to `AddElements`. That is what `test/jls/CircuitTextBuilder.java` already does, minus
the op terminal.

So the elegant route is not to build a second construction language and migrate 23 test files
onto it. It is: **promote `CircuitTextBuilder` out of `test/` into main, and give it an op
terminal** — `builder.toOps()` / `builder.applyTo(OpSink)`. Consequences:

- Open Question 1 answers itself: the verbs live where the blocks live, `jls.collab.op` (or
  `jls.core` if grading/import want them without the collab dependency). No new package, no
  `jls.api`, no extensibility story opened.
- Open Question 2 evaporates. Criterion 3 flips from "24 files must migrate" to "zero files
  migrate; the builder moved and grew a method", and the 3 weeks of headroom §Cost reserves
  for that migration is not needed.
- The 23 existing consumers become, for free, the regression suite for the verb set — a
  larger and more honest corpus than the single new golden §5 criterion 5 asks for.
- The builder's existing habit of emitting explicit `int width 24 / int height 24`
  (`test/jls/CircuitTextBuilder.java:19-25`) is exactly the font-independence property
  Reframing 1 generalizes. The two halves converge instead of being two tasks.

## Reframing 3 — derive the verbs, don't write them

If verbs are hand-written per element type, ARCHITECTURE.md's "sixteen places to add an
element" list becomes seventeen, and that pulls directly against #78. But #78 has already
partly landed: `src/jls/elem/ElementRegistry.java` enumerates every loadable type with a
factory, and `Attribute` already declares every persisted parameter with its type and save
name. A single generic builder — `b.element("Register").set("bits", 8).at(x, y)` — validated
against `ElementRegistry.forTag` and the `Attribute` registry, covers every element that
exists and every element anyone adds later, with `ElementVocabulary` as the network-facing
allowlist it already is. That is one class, not thirty verbs, and it *reduces* the
add-an-element burden rather than adding to it.

## What I am disregarding, and why

- **§5 criterion 1 as an integration test.** Under Reframing 1 it is a type-level property
  (no metrics reach `apply`), so asserting it by running two JVMs and diffing bytes tests the
  test harness, not the system. Keep a byte-parity test, but point it at *creation* across
  two different `TextMetrics` implementations — that is the assertion that would have caught
  the divergence bug.
- **§5 criterion 3's zero-consumer target.** Replaced by "the builder is in main and every
  generative consumer reaches ops through it." The format-level tests that are genuinely
  *about* the text stay on the text; that is not debt, that is the right test.
- **The strict TASK-0037 → TASK-0038 critical path.** With the builder-promotion framing the
  two are near-independent: promoting the builder and adding `applyTo(OpSink)` does not care
  what `apply`'s signature is, because the builder produces blocks either way. §6's
  justification ("building it against a `Graphics`-taking boundary means building it twice")
  only holds for a from-scratch verb set.

## Where the issue is right, and should not be softened

Criterion 2 — every one of the 13 `markChanged()` sites at `SimpleEditor.java` either behind
`OpSink` or on a reviewed deferral list — is the load-bearing one, and the comment's note
that it is the same measurement as #167's inventory closure taken from the other side is
correct and worth keeping. A vocabulary with four gestures still inline is not a vocabulary,
and #169/#171 cannot be specified against it. The §1 boundary against #316 ("a weeks-scale
feature must not be gated behind a months-scale one") is also exactly right and is the single
best judgment call in the issue.

## Net

The direction is right and the project needs it; the seam is cut one layer too shallow. Cut
at *determinism of the model* rather than at the type of a parameter, and let the existing
block builder become the construction path rather than its casualty. Both halves get smaller,
two open questions disappear, and #171 inherits a property it would otherwise have to
discover the hard way.
