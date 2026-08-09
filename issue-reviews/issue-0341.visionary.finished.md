# Issue #341: FEAT-027: a net has a kind and a driver has a strength, so open-drain buses, pull-ups and floating inputs behave the way the bench behaves
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four-vocabulary apparatus away and one sentence remains: *JLS cannot draw
I²C.* `docs/capability-roadmap/README.md:54` states it without hedging — I²C is
definitionally open-drain wired-AND with pull-ups, its multi-master arbitration *is*
wired-AND, and "the lab is not hard today; it is impossible." The same document's P1
vignette (`README.md:1244`) is a student adding a pull-up and an open-drain buffer and
watching a bus stop depending on which wire they drew first. That end is real, it is
squarely on the project's largest recorded program (P1, 28-36 wk), and #341 is the
right issue to own it. Nothing below argues against funding it.

The argument below is about *shape*. The project already recorded a design for this
exact capability, and #341 does not implement it — it implements a heavier one, and
the three ruling comments on the issue have spent their entire length litigating
consequences that the recorded design does not have.

## The reframing: one field on `Output`, not four vocabularies

`docs/capability-roadmap/keystone-a-design.md` §5.2 and `README.md:142-146` specify:

```java
// on Output, defaulting to (STRONG, STRONG) == today's behaviour
private byte strength0 = 6;   // Verilog level driving 0
private byte strength1 = 6;   // Verilog level driving 1
```

and then say, verbatim: *"A `driverKind` enum survives only as a **dialog preset over
the pair**, not as a kernel concept."* Under that pair:

| #341 / #387 concept | under the recorded design |
|---|---|
| `Strength` (5 levels, OQ3 "is PULL one level or two?") | Verilog's 8 levels, already numbered, already externally validated |
| `DriverKind {PUSH_PULL, OPEN_DRAIN, OPEN_SOURCE, PULL}` | four *values* of the pair: `(6,6)`, `(6,0)`, `(0,6)`, `(0,5)` |
| `PullUp` / `PullDown` as special elements | a constant-1 `Output` at `(highz, pull)` — ordinary elements |
| `NetKind {WIRE, WAND, WOR, TRI}` | a derived display label; `tri0`/`tri1` are an implicit pull driver |
| saved must-understand attribute + global `FORMAT` bump (ruling §4b) | one existing-style `Attribute` on `Output`, or a distinct element |

Two consequences worth stating plainly.

**1. The single-strength-per-driver model does not actually express open-drain, and
#387 §7.10 papers over it.** It writes an open-drain driver as `(STRONG, 0)` when
active and `(HIGHZ, ·)` when off — but "off" there silently means *driving 1*, which
is not the same thing as the tri-state enable being deasserted. So `DriverKind` is not
a classification; it is a *value transformer* that must run between the element's
computed output and the fold, **per bit**, because a 4-bit open-drain driver emitting
`0b1010` must release bits 3 and 1 while pulling bits 2 and 0 low. Neither #341 nor
#387 says this anywhere. Under the pair it is not a mechanism at all: `s_i = v_i ?
strength1 : strength0`, one array lookup per bit, correct by construction. The issue's
own §3 fold formula is already written per-bit over `(s_i, v_i)` — it has no honest
source for a per-bit `s_i` without the pair.

**2. The format fight dissolves.** The ruling comment of 2026-08-08 split net kind
(derived, no bump) from driver kind (saved, must-understand, **global `FORMAT` bump**,
scope increase on #387 stated but unpriced). That bump exists only because `DriverKind`
was promoted to a saved model concept. Two cheaper routes, either of which removes it:
(a) it is a strength pair on `Output`, persisted through the existing `Attribute`
registry the same way every other element parameter is; or (b) — the out-of-the-box
option nobody has raised — **open-drain ships as its own element**, an open-collector
buffer, the way a bench user reaches for a 7407 rather than flipping a bit on a 7400.
Unknown *tags* are already a hard error (`ARCHITECTURE.md`, save/load pipeline step 3;
`SaveTags`/`ElementRegistry` totality), so route (b) buys must-understand refusal
**by name, at zero format versions**, which is exactly what §1 criterion 5 asks for and
what the ruling had to spend a `FORMAT` bump to get. Route (b) is also more honest
pedagogy: open-drain is a property of a part you choose, not a checkbox on a gate.

## Two blocking Open Questions that the reframing answers for free

- **OQ2 (TTL floating-high vs CMOS floating-X, declared to block TASK-0058).** This is
  not a driver-side question and a "technology axis on `DriverKind`" is the wrong
  place for it. A TTL input reads high when floating *because its input structure pulls
  up*. Under strength-on-driver that is literally an internal `(weak, 1)` contribution
  the input adds to the net — zero new vocabulary, and CMOS adds nothing, so it floats
  and #322's four-state core reports X. The question stops blocking anything.
- **OQ3 (is `PULL` one lattice level or two?)** and #387's OQ4 (*"where does `SUPPLY`
  come from?"*, resolved as "define it and leave it unused") both vanish on adopting
  Verilog's existing 0-7 numbering: bus keepers land at `large`/`medium`, nothing is
  renumbered later, and — per `README.md:53` — the EVCD revisit trigger this feature
  *is* gets a 1:1 strength dump instead of a mapping table invented against a 5-level
  private lattice.

## The one thing missing from #341, #387 and all three comments

`keystone-a-design.md` §5.2 flags exactly one exception and calls it load-bearing:
the `TriProp` implementors (pins, splitter, binder — `src/jls/elem/TriProp.java`,
`WireNet.setTriState`) must become **strength-transparent**, forwarding the resolved
strength rather than re-driving at their own, because *"without that, an open-drain bus
stops working the moment it crosses a subcircuit boundary."* #341 §3 cites `TriProp`
only as *precedent* for net kind being an edit-time property; #387 mentions it only in
its O7 "does `PullUp` implement `TriProp`" question. Nobody owns strength transparency
— and every realistic I²C lab is drawn as subcircuits (master, slave, bus), so the
capstone payload (#297, #298) fails at the boundary with all criteria green.

Second unowned edit: `ARCHITECTURE.md`'s recorded decision #221 binds any future
execution strategy to *"the two-states-plus-HiZ value domain and multi-driver/tri-state
resolution (§2, §9)"* as its equivalence criterion. This feature rewrites §9. The DoD
names `docs/simulation-semantics.md` §9 and stops there; the recorded decision's own
text goes stale at the same commit and must be edited with it.

## Trajectory: strengthens, does not duplicate — with one drag

This is P1-S3 in the roadmap's own decomposition (`README.md:851`, `AMENDMENT.md:819`),
priced there at the same 6-9 wk, feeding I²C (#22), EVCD (#67), boundary scan, and the
IEEE 91 output qualifiers the conformance claim currently carves out. It is not
duplicative and it pulls in the project's direction. The drag is the vocabulary
inflation: `ARCHITECTURE.md`'s "adding an element today (the honest list)" is sixteen
places, #387 measures the last two-element commit at fourteen files, and #341's roster
adds two elements *plus* three enums *plus* a saved attribute *plus* a `FORMAT` bump —
against a 2 wk exclusive row and a 6-9 wk band whose gap the ruling comment §3 already
identifies as "this feature's real residual." Cutting to one field on `Output` plus two
constant-driver elements is where that residual gets recovered.

## Explicit disagreement with stated acceptance criteria

I am disregarding **§1 criterion 5** ("net kind is a saved, versioned property, and a
reader that does not understand a net kind refuses the file") in the form the ruling
comment re-targeted it — a saved, must-understand `DriverKind` with a global `FORMAT`
bump. Net kind should not be model state at all (an HDL declares `wand` because it
cannot see the drivers; a schematic editor *can* see them — it is a status-line and DRC
label derived in `recheck`, exactly as #387 §7.10 computes it), and driver strength
should reach must-understand refusal through the frozen tag table, not a version bump.
I am likewise disregarding **OQ2 as a blocker** on TASK-0058 per the argument above.

## Verdict

**endorse-with-reframing.** Fund the capability; it is the gate on the project's most
distinctive pedagogical claim. Before #387 executes: (1) reconcile the model with
`keystone-a-design.md` §5.2 — strength pair on `Output`, `DriverKind` demoted to a
dialog preset, `NetKind` to a derived label — or record on this issue why the recorded
design is being overridden; (2) adopt Verilog's 8-level numbering and close OQ3, #387's
OQ4 and the EVCD mapping in one stroke; (3) give strength transparency across `TriProp`
an owner, or the I²C capstone fails at the first subcircuit boundary; (4) add
`ARCHITECTURE.md`'s #221 equivalence criterion to the DoD's document edits.
