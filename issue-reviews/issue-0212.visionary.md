# Issue #212: Element-provider plugin API: discover external ElementType descriptors via ServiceLoader atop the #78 registry (the recorded replacement for the removed XML loader, #80 H2)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the mechanism away and the stated end is one sentence from § Intended Audience:
an instructor wants *"a course-specific element (a custom ALU, a bespoke bus element)
loadable in students' stock JLS without forking and rebuilding the jar."*

Everything else in the issue — `ElementProvider`, `ServiceLoaderDiscovery`,
`ForeignElement`, `-providers <dir>`, `providerId:tag` namespacing, a child class
loader, `networkInputCannotNameADiscoveredType()`, a `CollabSecurityRatchetTest`
pinned site, the #569 publication layer with its three slices — is machinery in
service of that sentence. The second stated beneficiary is explicitly rhetorical:
*"so 'can JLS be extended?' has a real answer instead of a launcher-replacement
stub."* That is a goal about the project's self-image, not a user outcome, and it
should not be allowed to carry a security surface.

Judged against that end, the issue has picked the most expensive of at least three
available routes, and it never considers the other two.

## The reframing: an ALU is a circuit, not a class

JLS already ships three ways to define an element's behaviour as *data* on the
existing simulation kernel:

- **`SubCircuit`** — `src/jls/elem/SubCircuit.java`, already in `LogicElement`'s
  permits list, already an `ElementRegistry` row. `SimpleEditor.doImport`
  (`src/jls/edit/SimpleEditor.java:5463`) copies a named circuit in as a placeable
  imported subcircuit.
- **`TruthTable`** — arbitrary combinational behaviour, saved as `String input` /
  `String output` / `pair` rows (`src/jls/elem/TruthTable.java:196-209`). A custom
  ALU is literally this.
- **`StateMachine`** — arbitrary sequential behaviour, likewise data.

A "course-specific ALU" and a "bespoke bus element" are both inside that set. So is
almost everything an instructor in a digital-logic course would want. The issue's
§1 claim that *"today that is impossible without a source edit"* is true only of the
*descriptor table*; it is false of the *capability*. What is actually missing is not
an extension path — it is **distribution and palette presence for the data-defined
elements JLS already has**. Imports today are per-open-circuit, discovered from
whatever the editor happens to have open, invisible in the palette, and unnamed as a
distributable unit.

**Concrete alternative — the circuit library.** A library is a directory or a
plain zip of `.jls` circuits plus a small manifest naming a palette group, an icon,
and a version. JLS reads it through the loader it already has, contributes
`PaletteEntry` rows for each member, and placing one performs today's import: the
definition is **copied into the student's circuit**. The whole feature is a reader,
a palette contribution, and a menu — no SPI, no class loader, no permits change, no
`apiVersion`, no trust document, no #569 publication promise.

## Why the library route is the one that fits JLS's arc

**1. It preserves self-contained save files; the plugin route deliberately breaks
them.** `Circuit.save` writes imported subcircuits into the same file
(`src/jls/Circuit.java:1476-1478` — *"always saved through their imported circuit"*).
A student's `.jls` opens anywhere. The plugin route's own §5 I1 states the opposite
as an accepted outcome: *"without the jar the same file yields today's clean 'no
element type named …' error"* (`Circuit.java:918`). For an educational tool whose
files are submitted to autograders, emailed to TAs, and opened in the container
image (`ghcr.io/anadon/jls`), a save format that can reference external code is a
**portability regression sold as a feature**. The README's own compatibility
section agonizes over JLS 4.1 silently dropping memory contents; this introduces a
strictly worse version of that failure by design.

**2. It costs no trust boundary.** The loader is already hardened against hostile
input (issue #38, `UntrustedFileHardeningTest`, the `LoadError` taxonomy,
container sniffing in `FileAbstractor`). A library is *more circuit files*. The
security story is one already told, tested, and shipped. The plugin route's story is
"JLS now has an unsandboxed in-process code-execution path," mitigated by a
default-closed flag and a parent-delegating child loader — good mitigations, but the
surface is permanent and the project's whole distribution identity (reproducible
jar, cosign, attestations, SBOM, SignPath) is built on *knowing what is inside the
artifact*.

**3. It leaves `ElementVocabulary` alone.** The absorbed #399 content records P10 —
`A ∩ D = ∅`, the collab allowlist must never admit a discovered tag
(`src/jls/collab/op/ElementVocabulary.java:39-46`). That invariant exists only
because discovery creates the hazard. A library element is a `SubCircuit`, already
in the allowlist, already expressible in the op vocabulary. The hazard never forms.

**4. It does not need #78 closeout, #223's API freeze, #403, or #277.** The issue's
own critical path is *demand-gate REPLAN → file → T1 → {T2,T3}*, sitting behind
`blocked_by: [78, 223]` plus the newly-owed #403, and downstream of a dispatch change
(#277) that the 2026-08-08 roster comment warns must not be inverted. The library
route is downstream of nothing. It could ship this month.

## The residual — and how small it is

The honest residual for *code* is behaviour the simulation kernel cannot express as
data: a peripheral, an I/O device, a host-touching element. The project has already
decided those cases separately and **not** in this issue's favour:

- The host byte port (#324) is recorded as **decision D7: not a plugin seam**.
- `docs/grand-architecture.md:200-212` reserves **out-of-process IPC** for untrusted
  third-party providers and external tooling, and ARCHITECTURE.md:295 ratifies it.
- Verilog/Yosys-JSON import (#61/#62, the `hdl.importer` row still `pending` in
  `docs/extension-points.md`) is a *second* already-catalogued route by which an
  externally-authored block becomes a placeable JLS element — and one with a real
  named ecosystem behind it, unlike a JLS-specific SPI with zero requesters.

Note that the `ForeignElement` design imported from #399 concedes the argument from
the inside. It delegates `react`, pin geometry, and **a bounded attribute map** —
i.e. geometry and state are already data, and the one genuinely code-shaped thing
left is `react`. JLS has two data languages for `react` shipping today. The
`ForeignElement` shape is one honest step away from "an element whose behaviour is
declared, not compiled," which would collapse the SPI, the class loader, the
namespacing grammar, and the trust document into a parser.

## Disregarding the stated acceptance criteria

I am setting aside I1–I4 and most of the Definition of Done, and saying why: they are
all *mechanism* predictions (a fixture jar round-trips; the seam's contributions equal
built-ins ∪ discovered; the trust doc quotes #222). Every one can go green while no
instructor is any better off, and the issue admits this — *"Not a student-facing
change until a provider ships."* A feature whose complete acceptance is compatible
with zero users has the wrong acceptance criteria. The criterion that matters is
**"a named instructor distributed a course-specific element and students used it,"**
and the library route reaches that criterion faster, cheaper, and without the
portability and trust costs.

I am also declining the D10 argument as applied here. *"Demand gates apply to
third-party asks, NOT to the maintainer's roadmap"* is a sound rule, but it was used
to retire the single control holding back the one item in the tree whose own body
gives its non-mechanical justification as *making "can JLS be extended?" have a real
answer*. D10 removes the gate; it does not supply a beneficiary. The roster comment
already records that no capstone requires this work.

One process observation belongs in a trajectory review: this issue has absorbed #330
and #399, spawned standing boundary records against #569 (+#825/#826/#827), inherited
#403 and an open question about #277, and accumulated ~90 KB of planning prose across
fourteen comments — for a feature with zero lines of code and zero named requesters.
The plan has become the artifact. That is itself evidence the framing is wrong: the
routes above are small enough that they would have been *built* in the time spent
deciding how to decide about this one.

## What to keep

Nothing here is wasted. Keep, verbatim, and re-home:

- The **sealed-hierarchy finding** and the rejection of unsealing `LogicElement`
  (`Element.java:17-18`, `LogicElement.java:17-21`). It is a permanent architectural
  fact and the strongest content in the issue.
- **Default-closed activation** and the "opt-in by invocation, never by a file"
  posture — apply it to library loading too, though the stakes drop enormously.
- **`A ∩ D = ∅`** as a standing rule for any future external-name path.
- **Deterministic contribution order** and the hot-plane purity rule (*"the registry
  decides that a type exists, never what happens per event"*) — those belong to #223
  and #403 regardless of this issue's fate.

## Concrete next moves

1. File a small feature: **circuit libraries** — a distributable directory/zip of
   `.jls` subcircuits with a manifest, contributed as `PaletteEntry` rows, placed by
   the existing copy-in import so files stay self-contained. Acceptance: a named
   instructor ships one and a student opens the resulting file in stock JLS.
2. Retitle and narrow #212 to its true residual: **"external element behaviour that
   the simulation kernel cannot express as data"** — and require, before any code
   lands, one named element that (a) a real course wants and (b) `TruthTable`,
   `StateMachine`, `SubCircuit`, a circuit library, and Verilog import all fail to
   express. If no such element can be named, close it with that finding recorded;
   that is a genuine architectural result, not a defeat.
3. If such an element *is* named, evaluate it against the out-of-process reservation
   already in grand-architecture §4.3 before reaching for the in-process SPI. The
   in-process variant is the one that cannot be walked back.
4. Notify #224 and #569 that this feature's mechanism is under reframing, so #825–#827
   are not written against a surface that may not exist.
