# Issue #603: TASK-C486-1: a driver can say how fast its output changes — a transition time beside the existing propagation delay, absent by default, with every golden byte-identical
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machine block away and the ask is one sentence: *the electrical-length
lint needs a number to multiply by a velocity, so JLS can teach that edge rate —
not clock rate — decides whether a drawn wire is still a wire.* That end is
excellent, cheap, and genuinely the highest teaching-value-per-word item in the
high-frequency programme. Nothing below disputes it.

The design chosen to reach it is the problem. "A transition time **beside the
existing propagation delay, at the same arc granularity**" is not a small
formulation — it decides which files are touched, which feature blocks the work,
what the acceptance evidence has to be, and what a student sees in a dialog. I
believe every one of those four falls out wrong, and that a different seam gets
the same pedagogy for less code, less blocking, and more downstream leverage.

## The seam the issue picks does not exist

There is no "arc granularity" in JLS to be at the same granularity as.
`propDelay` is one `int` per element, applied uniformly across every input→output
arc and both edges (`docs/capability-roadmap/README.md:394-396`). It is
replicated by hand: nine `private static final int defaultPropDelay` constants
(`src/jls/elem/Adder.java:33`, `Register.java:54`, `Mux.java:35`,
`TriState.java:33`, `Decoder.java:33`, `FieldExtend.java:54`,
`RegisterFile.java:56`, `ShiftRegister.java:101`, `StateMachine.java:39`), plus
`Gate.Kind.defaultDelay` and `Memory.defaultAccessTime = 100`, each with its own
`Attribute.IntAttribute("delay")` get/set pair (`src/jls/elem/Gate.java:316-328`
is the archetype) and its own dialog row.

Executing AC 1 literally means writing that pattern twice more across the ~17
`Timed` implementers: two fields, two `Attribute` subclasses, two save-grammar
rows, two dialog decisions per class. That is roughly 60 new hand-maintained
sites for two numbers **the simulator will never read**. And it re-instantiates,
at triple width, exactly the per-class duplication `src/jls/elem/Timed.java`
exists to retire — the capability interface #78 introduced so that "an element
that owns a timing value declares that fact once, in the type system." The
project is walking away from this pattern; the task walks back into it.

## The better route is already committed in this repository

`docs/capability-roadmap/sweep-02-timing.md:457` — **Change G, a technology-library
layer**: a `TechLibrary` mapping (element kind, width, pin count) → timing data,
a circuit-level `library` attribute, `Circuit.resetAllDelays` (`Circuit.java:1721`)
becoming "apply library", and **two shipped built-ins: `jls_default` (today's
constants, so nothing changes) and one datasheet-derived teaching library**.

Put the edge rate there and every hard part of #603 dissolves:

- **Absent-by-default becomes free rather than tested.** No library declared ⇒
  `jls_default` ⇒ today's constants ⇒ nothing to be byte-identical *about*.
  AC 2 and AC 3 stop being a tree-wide negative someone must construct and
  become a property of the data path.
- **It matches what a student actually knows.** #486's own headline numbers —
  74AC at t_r = 2 ns giving l_crit = 70 mm on a breadboard, 74LS at 18 ns giving
  630 mm — are *part-family* facts. No first-year knows their drawn adder's
  transition time in picoseconds; they can absolutely pick "74AC" from a menu and
  watch the verdict flip. CAP-18's demo is a *technology comparison*, and Change G
  names technology comparison as its own pedagogical payoff.
- **It is the change three standards actually want.** The same sweep line that
  #486 quotes ("JLS has one integer, no slew, no load, no fanout awareness",
  `sweep-02-timing.md:110`) classifies the Liberty gap as **A + F + G** — the
  library layer is a named blocker, and Change G's text states it also dissolves
  the C2 "a JLS drawing has no cells" objection to SDF. A per-element `t_r` field
  unblocks none of that.
- **One format surface, one review.** A circuit-level library name (plus an
  optional per-element override) is one optional section under #319, not two
  attributes on seventeen element types.

## The "degenerate case of DelayModel" argument is technically inverted

AC 5 asks for a note showing a scalar `t_r` is the degenerate entry of a per-arc
min:typ:max table. It cannot be written honestly, because that is not where slew
lives in any of the three cited standards:

- **Liberty**: output transition is `rise_transition`/`fall_transition`, a *2-D
  table indexed by input slew and output load* — a derived quantity, not a
  declared one, and not a min:typ:max delay entry. The repo states this at
  `sweep-02-timing.md:110`.
- **SDC**: edge rate at the boundary is `set_input_transition` /
  `set_driving_cell` — a **constraint on the design boundary**, not an attribute
  of an interior element.
- **SDF**: carries `IOPATH` delays and has no slew at all.

So a declared scalar `t_r` is the degenerate case of **a characterised library
cell**, not of a per-arc delay table. Writing the note AC 5 demands would record
a false lineage and guarantee the migration the task says it exists to avoid.
(Related, and worth fixing before anyone implements: `#87 / #89 / #93` are
`docs/standards-landscape.md` **entry numbers** (`:297` is Liberty), not GitHub
issues. GitHub #93 is a closed NullAway rollout; #87 is a closed review PR. AC 5
as written points an implementer at the wrong three documents.)

## The blocking dependency is an artifact of the seam, not of the physics

`ordering_after: [367]` is justified as "a transition time in seconds is
meaningless against a dimensionless tick." That holds only if `t_r` must be
commensurate with `propDelay`'s ticks — i.e. only under the "beside the existing
delay" framing. The lint's arithmetic is `l_crit = v·t_r/6`: seconds times metres
per second, both SI, self-contained. An edge rate consumed by a *lint* needs no
tick base at all. Cut it as an annotation and this task can start today instead
of waiting on a 2-3 mw feature whose own task (TASK-0101) is still unfiled.

## The unnamed alignment risk: an inert knob is a pedagogical falsehood

#486 refuses a pixels-to-millimetres scalar because it "would install exactly the
class of falsehood the roadmap's own text condemns in zero-delay wires." A `t_r`
field placed *next to* "Propagation delay" in the gate dialog is the same class of
falsehood. JLS is two-state with no X (`simulation-semantics.md` §2), wires are
ideal and instantaneous (§6.1), delays are pure transport (§6.2). A student can
type 1 fs or 1 second into that field and every waveform is bit-identical — AC 2
and AC 3 *guarantee* it. Adjacency in a dialog is a claim about meaning.

Whatever lands must therefore be self-describing as a **statement about the part
you claim to be building from**, not as a simulator parameter. "This circuit is
built from 74AC" carries its own honesty; a `t_r` box beside `delay` does not.

## Concrete alternatives, in order of preference

1. **Library-first (recommended).** File this as the first, deliberately thin
   slice of Change G: `TechLibrary` in `jls.core` or `jls.hdl`, a circuit-level
   `library` attribute, two built-ins (`jls_default` = today's constants; one
   datasheet family carrying t_PD **and** t_r/t_f), and lookup routed through the
   existing `Timed` capability rather than new per-class fields. Cost is plausibly
   inside 1-2 mw for the annotation-only half, because the delay half changes
   nothing while `jls_default` is selected. Downstream: Liberty, ALF and the SDF
   C2 objection all move.
2. **Net-annotation minimal (fallback if 1 is too big for the band).** Put the
   edge rate where the lint consumes it — **on the net, beside the declared
   length**, collapsing #486's two attribute scopes into one optional section, one
   grammar, one review. This removes entirely the risk #486 cited as its reason
   for cutting them apart (a format change and a delay-model change in one
   review), because the delay model is not touched. Honest cost: a net's edge rate
   is really its driver's, so a multi-driver net must say whose — for a
   conservative lint, "the fastest declared driver", with a circuit-level default
   and a per-net override.
3. **As written.** Only if a maintainer decision explicitly accepts ~60 new
   hand-maintained sites, a false DelayModel lineage in AC 5, and a #367 block
   that the alternatives do not incur.

## What I am disregarding, and why

I am setting aside **AC 1's "at the same arc granularity as its existing
propagation delay"** and **AC 5's DelayModel-degeneracy note**. The first names a
granularity JLS does not have and, executed literally, expands the exact
duplication `Timed` was created to remove. The second asks for a derivation that
is false against Liberty, SDC and SDF as this repository itself describes them.
AC 2, AC 3 and AC 4 I keep unchanged — they are right, and both alternatives
satisfy them more cheaply than the stated design does.

## Verdict

**rethink.** Right end, wrong seam. Keep the goal (an edge rate JLS can state and
a lint can read, absent by default), keep the pedagogy ("edge rate, not clock
rate"), and re-cut the work as a technology/annotation declaration rather than a
new scalar bolted to seventeen element classes beside a delay the simulator will
never relate it to.
