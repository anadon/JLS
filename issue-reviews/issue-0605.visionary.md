# Issue #605: TASK-C486-2: a net can say how long it physically is — an optional declared length that is never derived from drawn pixels, and an older reader that still opens the circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machinery and the claim is: *JLS should be able to hold a physical fact about a
signal that the drawing cannot supply, so a later check can tell a student their wire
stopped being a wire.* That claim is right, and the refusal at its centre — never derive
length from canvas pixels — is the best-argued paragraph in the whole CAP-18 stack. A
schematic is a topology drawing; publishing a px→mm scalar would be the same class of
convenient falsehood as the zero-delay wire. Nothing below disputes that.

What I dispute is everything the issue attaches that refusal to: *where* the number lives,
*what* it is keyed to, and *which* prerequisites it therefore inherits. On all three the
issue cuts against the seam this project has already chosen.

## The seam is wrong: `WireNet` is the one object in the wiring model with no identity

`src/jls/elem/WireNet.java:16` — `public class WireNet {` — does not extend `Element`. It
has no `id`, no `sid`, no `save`, no `setValue`. It is a *derived quotient*, rebuilt from
scratch every time anything happens: `src/jls/Circuit.java:1345-1394` re-partitions all wire
ends on every load; `SimpleEditor.java:833` and `:4389`, `Util.java:162` and
`collab/op/AddWire.java:177` mint fresh ones during editing; `WireNet.makeNet` (`:97`) splits
one and `WireNet.absorb` (`:251`) merges two. Wires themselves are not saved at all
(`Wire.save` at `:123` is literally `// do nothing`; `docs/file-format.md:330-335`
reconstructs them from mutual `ref wire` references on `WireEnd`s).

So the issue's own evidence — "at `2d0ca9d` the whole of `WireNet.java:22-30` is ends, wires,
bits, hasinput, triState: no length" — proves less than it thinks. Those five fields are all
*recomputed*, every one of them, from the saved `WireEnd` records. A sixth field placed
beside them is not a durable attribute; it is a value that evaporates at the next
`makeNet`/`absorb`. Adding a declared length there means inventing net persistence, which is
a far larger change than "an optional section", and it collides head-on with the sibling
feature the issue does not list.

**#336 (FEAT-004), which this issue omits from `ordering_after`, already ruled on this.**
Its Data Contract says, verbatim: *"Tracks durably. Nothing new in the save format. No new
record kind, no new element, no format-version bump. Net names are derived at emission time,
never stored."* The parent #486 lists #336 in `blocked_by`; #605 lists only `[367, 319]`. The
parent's justification for the #336 edge is that *the report* keys on net names — but it is
the *storage* that needs a key even more urgently, because a stored attribute without a
stable referent is worse than no attribute.

## The unasked question: what happens when a net splits or merges?

Neither #605 nor #486 asks it, and it is the question that decides the design. A student
declares 50 mm on net A and 80 mm on net B, then drops a wire joining them: `absorb` runs and
one net remains. Which length survives? A student cuts a 50 mm net in half: `makeNet` runs and
two nets exist, each — under a naive field — still claiming 50 mm, i.e. the declaration has
silently manufactured 50 mm of copper. AC 4 ("moving, lengthening or re-routing a drawn wire
changes no declared length") is satisfied by that behaviour and is therefore *not* the guard
it looks like: it pins the case where nothing bad happens and is silent on the two cases where
something does. Under the alternative below, the answer falls out for free.

## The alternative: an annotation keyed on a stable net name, not a field inside the circuit

Every physical/timing fact in this tool class already works this way — SDF, SAIF, Liberty, SDC
are *all* separate artifacts keyed by instance and net name, and `docs/capability-roadmap/sweep-02-timing.md`
treats them as such throughout. #336's Intended Audience says the quiet part out loud:
*"Anyone keying an external annotation on a JLS net name — SAIF, SDF, an external-simulator
VCD comparison, a shuttle pin binding. The name becomes a stability promise instead of an
accident of save order."* #336 is building the annotation key. #605 should be the first
consumer of that key, not a parallel mechanism inside the file.

Concretely, the reframing:

- **A declared-physical-facts annotation**, a small self-describing text artifact mapping
  `net-name → {length, medium-or-velocity}` (the `-check` input, the same way `-t` is the
  batch simulator's input). Read via `-check design.jls --physical design.phys` or a
  conventionally-named sidecar.
- **`ordering_after: [336]`**, not `[367, 319]`. The prerequisite cone collapses: today #605
  waits on #319 → #334 → #315 *and* #367, four features and a container rewrite, to record the
  string "50 mm". #336 alone is the honest predecessor because it is the one that supplies the
  referent. (#367 remains a real prerequisite of the *lint* — `l_crit = v·t_r/6` multiplies a
  time — but not of recording a length. A physical length carries its own unit token and does
  not need a time base; the issue's dependency on #367 is piggybacking on its format bump, and
  that piggyback is precisely what makes a 1-2 mw task wait on a 2-3 mw one.)
- **"Never derived from geometry" becomes structural, not defended.** An annotation file
  contains no coordinates; there is nothing for a later contributor to "fix". AC 4's test and
  its documented arithmetic remain worth writing, but they stop being the only thing standing
  between the project and a px→mm scalar.
- **Split/merge degrades correctly and identically to SDF.** After a merge, one of the two
  names no longer denotes; its annotation is reported unmatched and the net reads *not
  assessable*. That is the same fail-open degradation #486 already chose as its safety
  property, obtained by construction instead of by policy.

This is also the project's own recorded precedent, not an import. #319's machine block carries
maintainer ruling **D15**: *"the guest image is a SIDECAR FILE whose digest the circuit
records. A sidecar is not a section, so #343 consumes no mechanism from this frame and does not
gate on it."* #319's Open Question 4 recommends the same default. #605 reaches for the section
frame where the project has twice reached for a sidecar.

**I am explicitly disregarding acceptance criteria 2 and 3.** AC 2 (written as an optional
per-section-versioned section rather than an ordinary attribute) and AC 3 (an older reader opens
an annotated circuit with a clean diagnostic naming the skipped section) both presuppose the
inline route. Worse, AC 3's guarantee is aimed at a population that does not exist: "a reader
that predates the attribute" means, in the real world, JLS 4.1 and the 4.6–4.10 fork lineage —
every one of which predates #319 as well, and will therefore either silently ignore or report
malformed, never "clean diagnostic naming the skipped optional section". The only reader that
can satisfy AC 3 is a hypothetical build that postdates #319 and predates #605. Under the
sidecar, *every* existing reader in the world opens the annotated circuit unchanged, with no
diagnostic needed and no format work done — the guarantee AC 3 is trying to buy, delivered for
free and to a real population. AC 5 (goldens byte-identical, historical files load) likewise
stops being a test and becomes a tautology.

## The larger arc: this attribute is the first row of a constraint layer, and should be built as one

`sweep-02-timing.md` names the missing enablers as **C** (constraint object model), **F** (time
units) and **G** (technology-library layer). #486 promises that FEAT-059 (#490) and FEAT-060
(#487) both consume exactly these two attributes and add no third — asserted as integration
criterion 3 and, as written, a hope. A named-signal annotation with an explicit **provenance**
field (`declared` | `back-annotated` | `measured`) discharges that hope mechanically: #487's
back-annotation from a routed board stops being a new mechanism and becomes a new provenance
value on the same record, and #487's stated reason for being a separate feature — *"a lint input
may ride the silent-ignore valve; a constraint may not"* — dissolves, because a sidecar has no
valve to ride. One record shape, three consumers, no second format decision.

## The uncomfortable pedagogy point, and a 0.2 mw route to the actual goal

The audience #486 names is *"students whose circuit works in the simulator and fails on the
breadboard."* That student does not know why it failed, so they will not type `150 mm` onto a
net. AC 1 (absent by default) plus the lint's silence therefore guarantee that this feature
prints *not assessable* for every real user, forever; its only firing demo is a hand-authored
fixture. Two consequences worth acting on:

1. **The honest source of a length is a physical artifact, not a declaration.** Deriving length
   from *schematic* pixels is a lie; deriving it from a **breadboard** model (CAP-04, #297) is
   not — a breadboard has a real pitch and a jumper has a real length. #486 computes its own
   headline number that way (a 150 mm jumper, 2.1× critical length for 74AC). The refusal to
   derive from a lying source argues for a *truthful* source, not for a manual one. Consider
   inverting the chain: board/breadboard geometry → back-annotation → lint, with the declared
   annotation as the fallback for designs that have no physical model yet.
2. **The lesson does not need per-net data at all.** The stated pedagogy — *the verdict is keyed
   on edge rate, not clock rate* — is a pure function of `t_r` and `v`. A `jls -check` (or a help
   page) that prints the critical-length table for the common families (74LS → 434 mm FR-4 /
   630 mm breadboard; 74AC → 48 mm / 70 mm; and the 20 ps / 0.48 mm modern row) delivers the
   entire misconception-correcting payload to 100% of users, needs no per-net attribute, no
   format change, and none of #367, #319, #334, #315. It is worth filing that as its own tiny
   task and *then* asking whether the per-net declaration has earned a format decision.

## What I would keep unchanged

The refusal and its arithmetic (AC 4's documentation clause); absent-by-default (AC 1); the
"not assessable, never PASS" degradation; the strictness constant `k` as a declared parameter
rather than a literal; and the decision to cut the length away from the transition time. Those
are all correct and none of them depend on the storage medium.

## Recommendation

Keep the outcome, move the seam. Re-file the scope as: *a declared-physical-facts annotation
keyed on #336's stable net names, with an explicit provenance field, read by `-check`, never
containing geometry* — `ordering_after: [336]`, with #367 relocated to the lint task where the
arithmetic actually needs it, and #319 dropped. Record on #486 that its integration criterion 4
(older-reader survival) is discharged trivially by the sidecar and that its criterion 3
(two attributes sufficient for both downstream rungs) is discharged structurally by provenance.
If the maintainer wants the attribute inline in `.jls` eventually, that is a migration once #319
lands and has a real user — not a precondition for saying "50 mm" today.
