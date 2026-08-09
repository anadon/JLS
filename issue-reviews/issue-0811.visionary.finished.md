# Issue #811: TASK-C596-1: wire coloring lands as a scored catalog item — Digital #1308 closed or refused by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the process scaffolding (score order, stop-loss, ratchets) and #811 is one
claim: **a wire needs an identity channel that is independent of its value.** In a
rat's-nest schematic the question "which line is the address bus?" is not answered
by anything JLS draws today, because every visual channel a wire has —
hue *and* stroke — is already spent on state (`src/jls/edit/WireRenderer.java:43-91`:
hue from `Palette.wireOff/nonZero/wireZero`, stroke dashed/thick/thin from
`strokeFor`). Digital #1308's users reached for "let me color it" because color is
the only knob a schematic editor conventionally exposes. Color is the *request*;
identity is the *need*. Everything below follows from taking the need as primary.

## What HEAD already gives this item for free

Three of the five acceptance criteria are already discharged by landed
architecture, which changes how this item should be scheduled and scoped:

- **AC-4 (outside `SimpleEditor`)** is not a risk here. The wire-drawing seam is
  already extracted (`WireRenderer implements ElementRenderer`), the context menu
  is already a headless policy class (`src/jls/edit/OptionMenuPolicy.java`, with
  `ATTACH_PROBE_LABEL` as the exact precedent for a new wire-scoped item), and the
  operation is already an enum-backed shared action (`src/jls/edit/EditOp.java:35`).
  #596's "hard gate" on #316/#84 does not bind this particular row. Say so, and let
  the item be fundable ahead of the rest of the bucket.
- **AC-1 (byte-identical when unassigned)** is free by following the probe pattern:
  `WireEnd.save` (`src/jls/elem/WireEnd.java:586-617`) emits `probe <id> "name"`
  only when `wire.hasProbe()`. A conditional colour line is the same shape. There
  is no new format risk to buy down here.
- **AC-2 (grayscale/CVD assertions still pass)** is already true by construction —
  *provided the user colour touches hue only*. #76 put the whole state alphabet
  into the stroke channel; remove all colour and high/low/HiZ remain
  thick/thin/dashed. #542's assertions cannot fail on a coloured fixture unless the
  implementation also touches `strokeFor`.

What is left is the one thing the issue asserts but does not design.

## The contradiction at the centre, and a way out

The Outcome says an assigned colour is "an additional channel over the state
encoding, never a replacement for it." A 1-pixel line has one hue. If the user's
colour is painted, the state hue is *gone* — replaced, not added. The issue's own
premise is unimplementable as literally worded.

Two honest resolutions, and I would take the second:

1. **Accept the swap.** Colour becomes the identity channel, stroke keeps state.
   Defensible, and it is what #76 already set up. But it silently downgrades the
   default state encoding for every coloured wire, and it makes CVD safety a
   property of whatever hue the user picked — unprovable in `ThemeTest`.
2. **Casing render.** Draw the identity colour as a wider stroke *underneath* the
   state-coloured line — the road-casing idiom from map rendering. Two visible
   hues on one wire, state on top and unchanged, identity as a halo. This is
   genuinely additive, it is ~6 lines inside `WireRenderer.draw` before the
   existing `g2.drawLine`, and it makes AC-2 a theorem rather than a fixture test.
   It also survives grayscale as a *third* channel (casing width), which is more
   than the issue promises.

## Reframing 1: persist a palette slot, not an RGB triple

This is the point I would insist on. `Theme` (`src/jls/Theme.java`) is ten semantic
roles precisely because #76 spent real effort proving that ~126 hardcoded colour
call sites were a defect, and a dark variant is explicitly waiting on that sweep
(the record's javadoc says so). Writing a raw `java.awt.Color` into a user's `.jls`
file re-introduces hardcoded colour **in user data, where it can never be swept**:
every coloured circuit saved in 2026 will look wrong on the dark theme, and no
future maintainer can fix it without rewriting other people's files.

Persist an *index into a named accent ramp* — eight Okabe-Ito slots, resolved
through `Theme` at draw time. Consequences: dark mode gets coloured wires for free;
CVD distinguishability is proved once in `ThemeTest` over the ramp instead of
per-fixture; the file format gains an integer, not a colour space; and the user gets
a palette that cannot produce an illegible or state-colliding choice. A colour
*picker* would be the naive implementation and the one that pulls hardest against
the project's trajectory. Offer eight swatches.

## Reframing 2: net-scoped, wire-anchored

`WireNet` is derived, never persisted — but `WireNet.propagate`
(`src/jls/elem/WireNet.java:512-527`) already reads a per-*wire* probe and treats it
as a *net*-level fact for VCD. That is the exact pattern: store the slot on one
wire, resolve it at net scope in the renderer via `w.getNet()`. Per-segment colour
would mean an L-shaped run is two colours, one save line per segment, and an
undefined answer when nets merge or split under editing — a case #811 has no
criterion for. Net scope makes the merge rule statable in one sentence and keeps a
coloured bus at one saved line.

## Reframing 3 (the one that could delete the feature)

If colour is really identity, JLS already has an identity primitive that is
persisted, user-assigned, appears in the trace window, and flows into VCD export and
the `-t` batch contract: **the probe name.** Derive the accent slot deterministically
from the probe name. Naming `addr` colours it, in the editor *and* in the waveform
viewer, with the same hue in both — which is the thing GTKWave/Surfer users
actually want and `docs/vcd-interop.md` currently cannot offer.

Under this framing there is nothing new to persist (AC-1 vanishes), nothing new for
CVD to prove (AC-2 vanishes), no merge semantics to invent, no new menu item, and
the editor's colour affordance becomes a *view of a model fact* rather than a second
uncoordinated store of user intent. It is strictly less code than the issue
describes and strictly more aligned with the riscv/#200 trace trajectory.

I am not asking to disregard the acceptance criteria wholesale — but AC-1's
"user-assignable and persisted" is a *solution* smuggled into an outcome, and #592's
catalog row is where the underlying complaint (aesthetic grouping vs. disambiguation)
is supposed to be cited. **#811 should read its own catalog row before committing to
"assignable".** If the cited complaints are about telling wires apart, the derived
colour wins and the item costs a third of its estimate. If they are genuinely about
free choice, ship the eight-slot assignable version with derived colour as the
default. Both paths close Digital #1308 by name.

## Alignment

The item pulls with the arc, not against it — it rides seams (`WireRenderer`,
`EditOp`, `OptionMenuPolicy`, `Theme`) the project built for exactly this. The one
real hazard is the RGB-in-the-save-file version, which would quietly re-open #76 in
a place #76 can never reach. Constrain it to a semantic slot and this is a good,
small, correctly-placed piece of work.
