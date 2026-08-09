# Issue #404: TASK-0003: an attribute name no element declares becomes a named load diagnostic instead of vanishing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Strip the 14 sections away and #404 is buying one sentence: **"if it loaded, it is all
there" must be checkable rather than assumed.** Its parent (#314) says so plainly, and it
is the right sentence to buy. It is the same discipline that already runs through this
project's arc — `SaveTags` instead of `Class.forName`, `ElementRegistry` totality tests
instead of a "no element type named…" surprise, `PaletteContractTest` instead of an NPE at
startup, `LoadError` categories instead of a stack trace. JLS's recorded direction is
*turn runtime surprises into build failures, and turn silence into named diagnostics*. A
loader permitted to disagree with the file it just read is the largest remaining hole in
that program, and it is worth closing. The goal is endorsed without reservation.

What I am not endorsing is the **seam**. #404 cuts along the return type of a 20-wide
virtual dispatch. That is the most invasive, least reusable cut available, it hardens the
very code #23 exists to retire, and it produces a signal that is unverifiable by
construction. There is a cheaper cut that yields strictly more.

## The reframing: declare consumable names; stop asking methods to confess

Today the loader asks each element *"did you take this?"* and the element answers by
control flow. The proposal makes that answer explicit. The alternative is to make the
element **declare, as data, what it is able to take** — and let the loader decide.

Concretely: `Attribute` already carries `name` (`src/jls/elem/Attribute.java:26`); add a
public accessor, and give `Element` a `consumableNames()` returning the union of
`savedAttributes()` names plus, for the not-yet-migrated types, a small literal set. Better
still, hang it off `ElementType` in `src/jls/elem/ElementRegistry.java`, which #78 already
established as the home for per-type metadata. The loader's five arms then become **one**
membership test, in one place, with no signature change anywhere.

Why this is better rather than merely different:

1. **The boolean signal is unverifiable; a name set is data you can cross-check.** Twenty
   hand-edited `return` statements are twenty new opportunities to return the wrong
   literal — and a wrongly-`true` return is a *silent drop that now also lies*, i.e. this
   issue's own defect reintroduced inside its own fix, invisible to every test it plans to
   write. `src/jls/elem/LogicElement.java:83` is already the shape that gets this wrong
   under hand-editing: it snoops `x`/`y` and then falls through to `super` unconditionally
   rather than through an `else`, so it does not match the O5 template the issue tells the
   executor to apply. A declared name set has no such failure mode.
2. **It closes H3's hole instead of accepting it as a threat.** §11 concedes that
   `savedAttributes()` completeness is *assumed*, and routes the risk to TASK-0001. With
   names as data, "every name the writer emits is consumable" is a property test over
   `ElementRegistry.all()` — mechanical, no corpus, no inventory document. The threat
   stops being a threat.
3. **It gets the other direction free.** #314 exiles "written but never read" to FEAT-013.
   With a declared set, *both* directions are one bidirectional property over the same
   data. The bucket largely evaporates rather than becoming another feature.
4. **It carries #78 forward instead of pulling against it.** #404 as written makes the
   handwritten `setValue` if-chain the *sanctioned* way to answer the consumption question
   — it hands 11 unmigrated classes a fresh, load-bearing contract and thereby raises the
   cost of ever deleting them. That pulls against #23's stated purpose ("a single
   declaration drives saving, copying, and load dispatch, so the three can no longer drift
   apart") and against #78's descriptor program. The name-set route pays down the same
   debt: each class converted to declarative attributes shrinks its literal set to empty.
5. **It is smaller.** Zero signature changes, zero override ripple, one call-site change,
   one new accessor. #404 budgets 20+ mechanical edits across 12 files to obtain a weaker
   signal.

## The second reframing: a load *report*, not a second global error channel

§7.4 adds an eighth `LoadError.Category` and a parallel warning list. `LoadError`'s own
javadoc says it is "a structured description of a single circuit-**load failure**", and it
is published through `JLSInfo.setLoadError`, which front ends render as *"can't open X:
…"*. Putting non-fatal notes into that type silently falsifies every existing consumer's
assumption that a `LoadError` means the load failed — which is precisely the class of
defect this issue exists to eliminate, reintroduced one level up. And #58's actual finding
was that **global mutable load-error state goes stale**; #404 proposes to park a second
channel next to it and then relegates "where does the warning list live?" to Open Question
3, "rides along."

That question is the task. `Circuit.load` returns `boolean` (`src/jls/Circuit.java:692`)
and speaks through a static. The durable move is: **load returns a `LoadReport`** — an
immutable record (#94 value semantics) carrying an outcome plus an ordered list of
diagnostics, each with a severity. `LoadError` becomes the fatal subset; unconsumed
attributes are the first *non-fatal* kind. The reason to do it here rather than later is
that four downstream consumers already want exactly this object and are each about to
invent their own:

- **#323 / TASK-0054** wants a migration report naming every dropped `.circ` construct —
  that is a `LoadReport` with a different diagnostic kind.
- **README's own forward-compat caveat** ("JLS 4.1 … **silently drops initial memory
  contents**" for RLE-encoded memory) is the identical disease in a second place, and a
  `LoadReport` is where a "this file carries data your JLS re-encoded" note belongs.
- **Collaborative merge (#170/FEAT-012)** and **checkpoint recovery** both need to know
  what a load did, not just whether it succeeded.
- **Batch autograders** need it on stderr as structured lines, which §7.1 already
  half-designs.

Cut the seam at "a load produces a report" and the unconsumed-attribute case becomes a
five-line contribution to it. Cut it at "four methods return boolean" and every one of
those consumers still has to invent the report later, against a loader whose only new
vocabulary is one enum constant.

## Where the issue is factually off, in a way that matters to the design

`git grep -n "public void setValue" -- src/jls/` at HEAD returns **26** lines, and the
issue reads that as "26 overrides across 17 files." It is not. Six of the 26 are unrelated
methods that the loose pattern swept up: `Input.setValue(BitSet)`,
`Output.setValue(BitSet)`, `WireNet.setValue(BitSet)` (simulation signal setters),
`Constant.setValue(BigInteger)` (a dialog setter, `Constant.java:234`), plus
`Group.setValues` and `FieldExtend.setValues`. The real loader-path surface is **20
declarations across 12 files** — `Element`, `Extend`, `Group`, `JumpEnd`, `LogicElement`,
`Memory`, `Pin`, `State`, `StateMachine`, `SubCircuit`, `TruthTable`, `WireEnd`. Five of
the 17 files O4 names have no loader `setValue` at all.

**I am explicitly disregarding acceptance criterion P6** (and #314's identically worded
DoD line): a build ratchet asserting `git grep "public void setValue" -- src/jls/` returns
zero is unsatisfiable without renaming or mutilating four unrelated simulation/dialog
methods, and if satisfied it permanently bans a legitimate method name across the tree. It
is a grep pretending to be an invariant. The invariant actually wanted is *"no element can
be offered an attribute name without the loader knowing whether it was consumed"* — which
under the reframing is enforced by there being exactly one call site, and is testable
directly: a registry-driven test asserting every registered type reports a non-empty,
save-consistent consumable-name set. That is the same shape as
`CapabilityInterfaceTest`/`PinFaceContractTest`, which is how this codebase already
enforces per-type totality.

The signal in the noise, though, is worth keeping: `setValue` is one name doing two
unrelated jobs — loader attribute injection and runtime signal assignment. Under the
reframing no rename is forced, but if the executor ever does thread a boolean, rename the
loader protocol (`acceptAttribute`) first, or the grep confusion above becomes permanent.

## What to keep exactly as written

The compatibility spine is right and should survive any reframing: load-and-report rather
than refuse (O6 is correctly read — the §5 forward-compatibility valve is the format's
feature, not its bug); byte-identical re-save as the oracle; the aggregation cap so a
hostile file cannot turn the diagnostic into #38's DoS surface; stderr in batch because
`docs/batch-interface.md` reserves stdout; no `FORMAT` bump. The refusal to widen scope
when H1 is refuted (§10) is exemplary discipline. And the observation quality — O3 in
particular, two files with two extra attribute lines producing an identical `stateHash` —
is the best kind of evidence: it makes the defect undeniable in one line.

## Concrete recommendation

Land the *end state* of #404, via: (a) `Attribute.name()` + `Element.consumableNames()`
surfaced on `ElementType`; (b) one membership check in `Circuit`'s element-body loop, with
the report accumulated on a returned `LoadReport` rather than a static; (c) a
registry-driven totality test that every registered type's writer-emitted names are a
subset of its consumable names — which subsumes TASK-0001's inventory for this table and
lets TASK-0004's corpus shrink to a generative property over `CircuitTextBuilder` (the
tree already has `GenerativeRoundTripFuzzTest` to model it on). Replace P6 with that test.
Keep P1–P5 and P7 verbatim; they are good predictions and hold unchanged under this
design. Nothing here changes what a user sees — it changes how much of the rest of the
roadmap gets built for free.
