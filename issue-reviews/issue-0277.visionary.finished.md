# Issue #277: Registry-driven dispatch: element, palette, and exporter consumers read the boot ExtensionRegistry snapshot instead of their static tables
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal is to close a loop: PR #272 landed the *contribute* half, so close the
*consume* half and retire the "deliberately not part of this slice" note at
`docs/extension-points.md` L68-71. Judged on its own terms it is unarguable — a registry
nothing reads is dead weight, and `grep -rn "contributions(" src/ | grep -v module/\|boot/`
returning nothing is a genuine failing observation.

But the *purpose* behind the loop is stated once, in §Intended Audience: this is "the
precondition for external `ElementType` discovery (#212)." That is the whole justification
for spending effort on a change whose own §7.12 promises byte-identical everything. So the
right question is not "do consumers read the registry" but **"what shape must dispatch have
for #212 to be a contribution rather than a fork?"** — and when you ask that, three things
about the plan as written stop looking right.

## Problem 1: the arrow points the wrong way, so the duplication survives

Today the data flows `ElementRegistry.ALL` → `CoreModule.register` → registry, and stops.
This task adds `→ consumer`. The result is a **round trip**: the static table remains the
authority, the registry becomes a verbatim copy of it, and dispatch reads the copy. Nothing
is deduplicated; one hop of indirection is added, and the §10 falsification criterion
("registry order differs from static-table order") exists *only because* of the round trip.
An order mismatch between a list and a copy of that list is not a real hazard — it is an
artifact of a design that keeps two authorities.

The end-state §13 gestures at is the right one — "static tables become contribution sources
only" — but §8's method never does the deletion, so the round trip is what actually lands.

**Concretely, invert it.** `ElementRegistry.ALL` and the `Palette` table become
package-private literals inside `CoreModule` / `GuiModule` (or stay where they are but lose
their public `all()` / `entries()` accessors). The *only* public catalogue is the one built
from the registry snapshot at boot. `ElementRegistry.forTag` keeps its signature and its 3
call sites (`Circuit.java:918`, `Palette.java:218`, `ElementBlocks.java:118`) and simply
resolves against the boot-populated index instead of a class-initialized literal. One
authority, no ordering question, no new indirection at any call site, and the P3 test still
has exactly the same thing to prove.

`ExtensionRegistry` is already shaped for this: `contributions()` returns an immutable
ordered snapshot (`ExtensionRegistry.java` L112), and `PaletteEntry` carries its own
`Palette.Group`, so grouped toolbar order is fully reconstructible from a flat contribution
list. Nothing in the mechanism forces the round trip; only the method section does.

## Problem 2: the plan contradicts #212's recorded interface, and nobody has reconciled it

#212 §3 (Feature-Level Interface & Data Contract) states plainly:

> **Modifies:** `ElementRegistry` lookup (`all()`/`forTag`) becomes "built-ins +
> discovered", built-ins authoritative on tag conflict; the palette table gains discovered
> `PaletteEntry` rows via `Palette`.

That is the *inverted* design above — `forTag` stays the lookup and its **contents** change.
This issue's §13 says the opposite: "the registry is the single dispatch source for the four
typed-now seams", i.e. `forTag` leaves the dispatch path. Two open issues, one of them named
here as the beneficiary, have contradictory pictures of the same three methods. Whichever
wins, #212's §3 is stale-on-deviation by its own §7 rule and this task should say so.

I think #212 is right and #277 is wrong on this point, for a reason bigger than either:
tag→type resolution has exactly one sensible public spelling, and `ElementRegistry.forTag`
is it. Making every caller of a *name lookup* go through a registry-of-extension-points
accessor is exposing the plumbing at the tap.

## Problem 3: a boot-global in `Circuit.load` is a step back toward `JLSInfo`

The adversarial comment already routed the accessor to #403's static `JlsModules.runtime()`
with fail-fast on pre-boot read. Take that seriously as an architectural commitment, because
§7.11 makes it load-bearing: "A consumer reading before boot completes is a programming
error and must fail fast, not fall back silently."

`Circuit.load` is the most-reached method in the tree. There are 240 test files; exactly one
(`JlsModulesBootTest`) boots modules. `ElementBlocks.load` runs on the collab op path;
`CircuitSnapshot` runs on the undo path. Routing `forTag` through a process-global that
throws unless someone called `JlsModules.boot()` first converts a pure function into a
temporally-coupled service-locator read, and pushes the cost onto every future test that
wants to parse a circuit. `ARCHITECTURE.md` §"Preferences / theme contributor" and
`docs/extension-points.md` both record that the project is trying to *retire* exactly this
pattern — the 28 `public static` fields of `JLSInfo` — and `grand-architecture.md` §4.3
picks sealed interfaces + records precisely so "the compiler enforces exhaustiveness…
instead of a runtime XML parse". A global that fails at runtime if you forgot to boot is the
runtime-parse failure mode wearing a Java type.

Under the inverted design the temporal coupling shrinks to one place — the boot-time
population of the element index — instead of appearing at every dispatch site, and the
fail-fast contract has one owner instead of four.

## Problem 4: the pin that makes H1 safe is the pin #212 must break

`JlsModulesBootTest.coreContributesEveryRegisteredElementType` asserts
`assertEquals(ElementRegistry.all(), contributed)`; `guiContributesEveryPaletteEntry`
asserts `assertEquals(Palette.entries(), contributed)`. H1 leans on these ("pins that boot
contributions mirror the static tables exactly"). But **identity with the static table is
exactly what an external provider destroys** — the first #212 fixture jar turns both
assertions red. So the safety argument for this task is built on assertions whose whole
content is the duplication the feature exists to remove. Under the inverted design those
tests naturally become "the boot catalogue ⊇ the built-in contributions, built-ins first",
which survives #212 unchanged.

## The one place I would deliberately break §7.12's "no observable change"

`HdlEmitter` already declares `fileExtension()` (`HdlEmitter.java:27`). Nothing calls it.
Selection is a hardcoded ternary at `JLSStart.java:382-385`: `.v` → Verilog, else VHDL.

The contract *already* carries its own selection key, and the seam is `many` cardinality per
the catalog. Rewriting that ternary as "pick the contributed emitter whose
`fileExtension()` matches the output file's extension; error listing the available
extensions when none matches" is a five-line change that makes #213/#215 and any future
emitter **purely additive** — contribute it and it is selectable. That is the first time in
this whole programme that contributing to a point does something a user can see, and it is
the single most persuasive demonstration the extension-point catalog could ship.

**I am explicitly disregarding §7.1/§7.12's "N/A — no CLI change" and §14's "existing tests
pass unmodified" for this one seam.** Exporting `foo.v` and `foo.vhd` stays byte-identical;
what changes is that `foo.sv` stops silently emitting VHDL and instead says which languages
exist. If a test pins the current silent fallthrough, that test is pinning a bug and should
change with a recorded rationale. Parity is a good default; here it is the thing preventing
the issue from having a point.

## What I would keep, unchanged

- **P3, the consumer-visibility test.** The adversarial comment is right that this is the
  irreducible core: the only assertion in the tree that a contribution reaches user-visible
  dispatch. Under the inverted design it is *easier* to write (contribute an extra
  `ElementType` before boot; assert `forTag` resolves it), and it still fails at `29afb26`.
- **The `blocked_by: [403]` correction**, and dropping §8's accessor bullet.
- **`collab.op-observer` out of scope** — agreed, for the stronger reason that wiring a read
  path over an empty list is unfalsifiable ceremony.

## Recommended reframing, as a method section

1. Move the built-in `ElementType` and `PaletteEntry` literals into their contributing
   modules; drop `ElementRegistry.all()` / `Palette.entries()` from the public surface (or
   redefine them as views over the boot catalogue).
2. Have `ElementRegistry`'s tag index and `Palette`'s group ordering be **built once from
   the boot snapshot**. Dispatch sites at `Circuit.java:918`, `Palette.java:218`,
   `ElementBlocks.java:118`, `SimpleEditor.java:2312` keep their current spelling.
3. Replace the `JLSStart.java:382-385` ternary with `fileExtension()`-keyed selection over
   the contributed emitters; add the "unknown extension" diagnostic.
4. P3 test as written; retarget `JlsModulesBootTest`'s two equality pins to superset-plus-
   order so #212 does not have to rewrite them.
5. Reconcile #212 §3 with whatever lands, on #212, before closing this.

If step 1 proves too large for one task, split it — but do not land steps 2-4 on top of an
un-inverted table, because that is the version that adds indirection, keeps the duplication,
manufactures the §10 ordering hazard, and hands #212 a dispatch path it then has to undo.
