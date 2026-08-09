# Issue #625: TASK-C490-4: a first-year drawing an adder never meets the transmission line — visibility derived from context, not an exclusion list, and an overshoot that names the receiving element
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two ends are bundled here, and they have nothing to do with each other. The
first is a pedagogy floor: JLS's default experience must stay a first-year's
adder canvas no matter what advanced machinery the SI programme adds. That end
is right, it is the most durable thing in CAP-18, and it deserves defending.
The second is a diagnostic that turns a computed overshoot into a sentence a
student can read. That is good UX work with a settled home (`TellUser`, the
`jls: error:` CLI contract, `ARCHITECTURE.md` § Error-reporting contracts) and
it is only in this issue because both halves happen to touch a first-year.

The mechanism the issue chooses to defend the first end — "visibility derived
from context, not an exclusion list" — is where I part company, and the reason
is architectural, not stylistic.

## The load-bearing distinction the issue never makes

`NON_PALETTE_TAGS` is not an exclusion list. It is a documented semantic
category, spelled out in `test/jls/edit/PaletteContractTest.java:36-45`: types
that are **not user-placeable at all**. `SubCircuit` is created by the Import
button, `WireEnd` only exists as a wire's endpoint, and `TestGen` — read
`src/jls/elem/ElementRegistry.java:31-37` — "is the batch-mode stand-in for a
signal generator" that "JLS never saves." `TestGen` is precedent for exactly
the thing FEAT-059's own stop condition describes: **a registered, loadable,
batch-only element type with no GUI surface at all.**

So there are two different states, and the issue silently assumes the harder
one:

1. **Not placeable.** The transmission line lives in hand-authored and
   fixture `.jls` files, is exercised headlessly, and has no toolbar
   presence. Correct classification: a fourth row in that set, plus the
   javadoc sentence saying why. Cost: two lines. The pedagogy floor is
   structural — there is no button to hide.
2. **Placeable but not in the beginner toolbar.** A student in the SI lab can
   draw one; a first-year cannot see it. This genuinely is not what
   `NON_PALETTE_TAGS` means, and inventing a predicate here would be wrong.

The issue's objection is aesthetic — "a fourth string to a hard-coded
exclusion set" — and it commits 0.5-1 mw of mechanism design to avoid a
classification that, for state 1, is *correct*. Nowhere does the issue argue
that the element must be placeable. Its parent (#490) explicitly names the
headless CSV form as a complete outcome. **Decide the shape first and most of
this task evaporates.**

## Where "context" would have to come from — and why it does not exist

"Derived from context" is undefined, and JLS has no context to derive from. A
grep for preferences, profiles, or an advanced/beginner mode across
`src/jls/edit/` and `src/jls/JLSInfo.java` returns two comments in
`UndoManager` about a preference that does not exist. The three honest
carriers of "context" are all named, owned, and *elsewhere*:

- `gui.theme` — "Preferences / theme contributor ... replacing `JLSInfo`
  statics", `docs/extension-points.md`, status **pending**, owned by #76.
- `app.command` — the lazy activation vocabulary, **pending**, owned by #84
  with #220's runtime.
- The module set itself — `jls.module.ModuleRuntime` / `ExtensionRegistry`,
  shipped as data structures (#220/#223).

Whatever this task builds in 0.5-1 mw will therefore be a bespoke predicate
answering to none of them. The one context JLS *does* have is the open circuit,
and a circuit-derived rule has a bootstrapping defect the issue never
considers: "show the element if the circuit already contains one" means you can
never place the first one.

## The route the issue never looked at: the seam is already cut

`docs/extension-points.md` already declares **two independent seams** —
`elem.element-provider` (contract `jls.elem.ElementType`, home `jls.elem`) and
`gui.palette-contributor` (contract `jls.edit.PaletteEntry`, home `jls.edit`),
both "typed now". That separation *is* the answer to "a green test currently
enforces the violation." The registry and the palette are two inventories on
purpose; the static `NON_PALETTE_TAGS` bridge exists only because, in that same
document's words, wiring the built-ins through an `ExtensionRegistry` "is
deliberately **not** part of this slice — it is a follow-on integration slice
of #220/#224."

The elegant route is therefore: **make the transmission line the first tenant
of the module boundary JLS has already designed.** An SI module contributes to
`elem.element-provider` *unconditionally* — so a `.jls` file containing the
element loads in every build, which the issue's framing quietly risks turning
into an `UNKNOWN_ELEMENT` load error for the first-year who opens a classmate's
file — and contributes to `gui.palette-contributor` *only when active*.
`PaletteContractTest` then becomes total over the **active contribution set**
instead of over a static registry minus a hard-coded set. That satisfies AC-1's
stated intent exactly, deletes the exclusion set's future growth for every
element to come, and makes the pedagogy floor a property of what was never
contributed rather than a predicate someone can get wrong. It also strengthens
#212 (external element providers) and #84 rather than pulling against them.

Honest cost note: this is bigger than 0.5-1 mw. But it is work the roadmap
already owes, and it is the difference between a mechanism JLS keeps and one
built for a single element by a task that is explicitly allowed to stop.

If that integration cannot be funded now, the light fallback is a **tier column
on `PaletteEntry`** (CORE / ADVANCED): the palette stays total over the
registry, nothing is excluded, the default toolbar renders CORE. One field, one
test predicate, and it generalises to the next hidden element instead of
generalising to nothing.

## Why the "palette count stays 32" invariant is the wrong ratchet

AC-2 pins the pedagogy floor to `grep -c "entry(Group\." src/jls/edit/Palette.java`
returning 32 forever. That freezes the *source text* of one file as a K9
invariant, and it is wrong in both directions. It is too strong: any future
element from an unrelated programme — the HDL import roadmap (#33/#59), CAP-04
— legitimately adds a palette row and would break an "invariant" that has
nothing to do with signal integrity. And it is too weak: it says nothing about
what a first-year actually sees, which is the thing K9 protects. The durable
assertion is over `Palette.entries()` filtered by tier (or by active module),
plus a `test/jls/ui/` layer-1 assertion that no SI surface is reachable from
the default experience. Assert on the model, never on a grep count.

While measuring: five of the eight toolbar groups are already exactly at
capacity — GATES 8/8, TIMING 2/2, TEST 2/2, COMPLEX 2/2, ANNOTATION 1/1
(`src/jls/edit/Palette.java:36-60`, `:123-188`). The toolbar is full. "Where
does a new element go?" is not a favour to first-years; it is a structural
constraint JLS will hit for every element it ever adds again. That is one more
reason to solve it once, generally, rather than as an SI-shaped exception. A
visible entry also costs an icon gif and a help page under `#85`'s completeness
contract (`test/jls/HelpTopicsTest.java:167-206`) — a palette row has never
been "one row".

## Disregarding the stated acceptance criteria, explicitly

- **AC-1 and AC-2 as written: drop.** The visibility rule is either
  unnecessary (state 1) or belongs to #220/#223/#76/#84 (state 2). The grep
  ratchet should not be created at all.
- **AC-5 is the plan, not the fallback.** "Stop at the headless CSV form" is
  where the value/permanence ratio is best: the 5.500 V / 3.300 V / 4.368 V
  lesson lands through the batch interface JLS already documents as a
  stability contract (`docs/batch-interface.md`) and already exports to
  GTKWave/Surfer (`docs/vcd-interop.md`), with zero frozen GUI surface, zero
  palette pressure, zero K9 risk. A lab that ships as a committed `.jls`
  fixture plus a container invocation teaches the same thing to more students
  (autograders, CI, RISC-V, no display) than a toolbar button ever will.
- **AC-4 is good and should be its own issue.** The overshoot diagnostic
  naming the receiving element and the peak is genuinely valuable, is
  independently testable headless, and has a settled home in the `TellUser` /
  `jls: error:` contracts. Bundling it with an architecture question means one
  review gates two unrelated risks, and it is the half most likely to be lost
  if the visibility half stops under K18-4.

## What I would file instead

1. **The overshoot diagnostic** — element + trace-row scope, `TellUser`-routed,
   asserted headless and with the trace window open. Ships regardless.
2. **A one-line classification of the transmission line as batch-only**, in
   `NON_PALETTE_TAGS` with its rationale sentence, matching the `TestGen`
   precedent. This *is* the pedagogy floor for the headless deliverable.
3. **A separate, properly-sized issue** — owned with #220/#224 rather than
   with CAP-18 — wiring `ElementRegistry` and `Palette` through
   `ExtensionRegistry`, with the SI element as its first tenant and the
   tier column as its stated fallback. That is where "visibility derived from
   context" becomes true for every element JLS will ever add, instead of for
   one.

The end this issue defends is the right end. The mechanism it picks is a
one-off in a codebase that has already written down, catalogued, and half-built
the general version.
