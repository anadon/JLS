# Issue #731: TASK-C542-2: thickness, dash and glyph carry wire state when colour carries nothing — a registry-keyed state-to-encoding map with a totality test
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

The end is not "more ink channels." It is that **a wire's state should be a nameable
thing JLS owns, with colour as one projection of it among several.** The proof is in
the issue graph, not in taste: #747 (TASK-C546-1) says it "consumes TASK-C542-2's
registry-keyed state-to-encoding data for describing state rather than inventing a
second vocabulary." A prose narrator for a blind student cannot consume
`BasicStroke(3.0f)` and `float[]{4,3}`. What it can consume is *the state's name and
meaning*. So the downstream sibling has already told us what this task must actually
produce: a state vocabulary with pluggable projections — visual mark, spoken phrase,
VCD character, tactile texture — of which the stroke table is the smallest one.

I endorse that outcome. I am explicitly disregarding AC-1's five-state inventory,
AC-2's "registry + scratch-state transcript" apparatus, and AC-3's flat injectivity
as written. Reasons and replacements below.

## Where the issue as written pulls against the project's arc

**1. The named pattern is borrowed from a problem this task does not have.**
`src/jls/elem/ElementRegistry.java` earns its manual-registration-plus-totality-test
shape because element types are *open* (~35 rows today, #212 will let third parties
add more) and arrive through reflective load, so the compiler can never see the set.
Wire states are the opposite: a closed set of three, drawn by one method
(`WireRenderer.draw`, `strokeFor`), that does not grow when an element is added. The
Outcome's promise — "a new element ... that lacks an encoding fails the build" — is a
failure this task's scope cannot produce. Copying the registry shape here imports its
costs (a runtime map, a reflective totality test, a scratch-state transcript ritual)
to guarantee something Java 25 gives for free with an exhaustive `switch` over an
`enum`/sealed type: a new state that lacks an encoding then fails **compilation**, in
every consuming surface at once, with no test to maintain.

**2. There is already a named seam for exactly this, and the catalog forbids a
parallel one.** `docs/extension-points.md` lists, as *pending*, `gui.theme` —
"theme/preferences object replacing `JLSInfo` statics," home package `jls.edit`,
cardinality one active, owned by #76 — under the explicit rule "**Pending seams are
named here first** ... so nobody invents a parallel mechanism in the meantime." A new
"registry-keyed state-to-encoding map" living beside `Theme` is precisely the parallel
mechanism that rule exists to prevent. The Outcome says the right thing ("composed
over the existing `Theme` seam") and then the ACs describe something adjacent to it.

**3. The state inventory is about to be obsoleted by the project's own roadmap.**
`docs/capability-roadmap/sweep-01-values-and-logic.md` opens with "JLS's value domain
is the narrowest waist in the whole program" and makes **V1 — a per-bit four-state
value type (`0/1/X/Z`) replacing `@Nullable BitSet`** the single highest-leverage
change in the sweep (sixteen standards directly). Meanwhile `docs/simulation-semantics.md`
§9 says flatly "There is no wired-AND/OR and no conflict (X) state," so #731's "error"
state does not exist, and "bus value" is not a state either — `strokeFor` collapses
every `BitSet` to zero/non-zero. Freezing a five-row hand-listed table now buys a
guarantee against the wrong set: it will neither catch today's real gaps nor survive
V1's arrival, when states become per-bit and mixed vectors appear.

## Reframe 1 — ship the datum, get the totality for free

Cut at the value domain, headless (`jls.core` is already the AWT-free home; the
`HeadlessCoreRatchetTest` boundary keeps it honest):

```java
// jls.core — no AWT
public enum WireStateKind { FLOATING, ZERO, NONZERO }   // grows to X/weak under V1
public record WireState(WireStateKind kind, @Nullable BitSet value, int width) {
    public static WireState of(@Nullable BitSet value, int width) { … }
    public String shortName();   // "floating", "0", "0x2A (8 bits)"  → #747, status line
    public char   vcdChar();     // already open-coded in BatchSimulator
}
// jls.edit
public record WireMark(float strokeWidth, float @Nullable [] dash, Glyph glyph,
                       ColorRole colour) { }
```

with resolution by exhaustive switch, no map and no registration line. When V1 adds
`UNKNOWN`, the build breaks in the renderer, the VCD writer, the trace window and the
narrator simultaneously — which is the guarantee AC-2 was reaching for, delivered
across four surfaces instead of one, at zero maintenance cost. `WireRenderer` keeps
its verbatim-moved geometry; it just stops re-deriving state from `BitSet` nullness,
and so does everything downstream. This is the same move #77 made for drawing and #78
made for loading: name the thing, then let the seams read it.

## Reframe 2 — the encoding is a `Theme` component, and monochrome is a shipped theme

`Theme` is today ten `Color` roles plus `apply()` writing `JLSInfo.Palette` statics.
Widen the role from `Color` to `WireMark` (colour *plus* stroke/dash/glyph) and three
things collapse into one:

- **Injectivity becomes a theme invariant**, asserted in `ThemeTest` next to the
  existing 25 CIE76 delta-E floor, iterated over `Theme.all()` — the very loop #729
  AC-3 is already adding for tritanopia. One test, two ratchets, no new apparatus.
- **"Survives monochrome printing and projection washout" stops being an argument and
  becomes a product**: a shipped `MONOCHROME` (print/projector) theme whose colour
  roles are all black and whose marks do the whole job. That is a feature a lab
  instructor can *choose*, testable by the ordinary theme machinery, and it is the
  honest way to prove the claim — far stronger than asserting greyscale properties of
  a colour theme.
- **AC-4's pixel-identity gate becomes trivially true by construction**: `DEFAULT`
  keeps today's marks (`3.0f` round-cap thick, `1.0f` thin, `{4,3}` dash, the
  `WireEndRenderer` ring), so there is nothing to gate — the default theme's marks
  *are* the current constants, moved. No screenshot golden needed for this task.

It also keeps #289 (dark variant) and #543's print path on one seam rather than three,
and it moves toward `gui.theme` retiring `JLSInfo.Palette` statics instead of growing
them with `hizDash`-style additions.

## Reframe 3 — channels should be orthogonal, not injective

AC-3 ("no two states share the same non-colour encoding") assumes a flat alphabet.
The renderer's real branch set is a **product**: interaction state (`touching`,
`highlighted`, plain) × value state (floating, zero, non-zero). Today the value
channel rides the stroke and the interaction channel rides the colour, and they
compose correctly by accident — `strokeFor(value)` is applied on every branch, so a
highlighted non-zero wire is still thick. But interaction state has *no* non-colour
channel on the wire body at all (`WireEndRenderer` gives `touching` a ring; the wire
gets nothing, and `highlighted` gets nothing anywhere). That is the actual grayscale
hole in the current code, and #731's five-item list does not mention it.

So the invariant worth testing is not injectivity over three rows (nearly vacuous) but
**channel orthogonality**: value is read from stroke geometry, interaction from a halo
or end glyph, and the composed mark is injective over the cross product. That is
falsifiable, it catches the real regression (a future state stealing the other
channel's variable), and it is what makes selection legible to a low-vision or
projector-bound user — the case a keyboard-first editor cares about most.

## Why this defuses KC-26-1 rather than triggering it

KC-26-1 anticipates glyph escalation wrecking legibility. It only triggers if you
insist on encoding bus *values* as marks — an unbounded alphabet on a 3–4 slot
channel, which cannot work. With the datum in hand, a bus's state is a number and
gets drawn as a number (JLS already draws probe labels on wires in `WireRenderer`),
leaving the mark alphabet at three. The kill criterion then guards a boundary nobody
needs to approach, which is the right outcome for a kill criterion.

## What I keep, and the smallest first commit

Keep: the outcome sentence, the "compose over `Theme`, not a parallel mechanism"
instruction (take it literally), the anchoring on `WireRenderer`/`WireEndRenderer`,
and the instinct that this must be machine-guarded rather than reviewed.

Drop: the five-state list, the element-registry pattern, the runtime totality test and
its scratch-state transcript, the per-commit pixel gate, and flat injectivity.

First commit, ~0.5 mw and independently valuable even if the rest is re-derived:
`WireState` in `jls.core` with `of/shortName/vcdChar`, `WireRenderer.strokeFor` and
`BatchSimulator`'s HiZ branch rewritten to switch on it, and `WireValueChannelTest`
retargeted at the switch's exhaustiveness. That single commit hands #747 the
vocabulary it was promised, gives #734's screenshot test named states to assert on,
and puts the V1 four-state migration on a seam instead of a `grep` for `!= null`.
