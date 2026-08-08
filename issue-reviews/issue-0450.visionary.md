# Issue #450: TASK-0055: the 74-series and DIP teaching set exists as licensed, provenance-carrying data on the classpath, with the electrical columns shipped inert
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus and the claim is: **JLS should stop knowing only about logic and start
knowing about parts** — so that a drawn `AndGate` can become section B of a 74LS08, and a BOM,
a wiring list, a fan-out check and a KiCad netlist all become *queries* rather than tables
hard-coded into emitters. That claim is well aligned with the project's arc.
`docs/capability-roadmap/sweep-06-physical-boundary.md` explicitly rehabilitates the third
category — "being a legitimate front end to somebody else's physical flow" — while keeping JLS
out of computing physical data, and the footprint-name-only rule (#349 invariant 1, #400 P9)
holds exactly that line. The "a target is data, adding one is a table entry, never new code"
precedent already ships in `src/jls/hdl/board/` (`Board`, `Boards`, `PinBindings`), and
`src/jls/hdl/board/Boards.java:20-30` is the honest warning the issue correctly cites: one
hardcoded entry, transcribed from vendor documentation, that works only because there is one
board. Nothing here pulls against the trajectory. The question is whether the *route* is right,
and in four places it is not.

## R1 — The seam between #400 and #450 is cut in the wrong place

#349 §2 justifies the split as "a schema defect is a redesign, a transcription defect is a wrong
pin number." True, but that is not the split the two issues actually make. #400 §6/§8 requires it
to ship `resources/parts/74series.parts` *with a `SCHEMA` header*, and #400 P1 demands
`PartLibrary.forElement(ElementType, int bits)` return a `PartPackage` — unsatisfiable without
rows. So #400 ships data anyway, and #450 then adds more rows to a file that already exists. The
"schema vs data" boundary is nominal.

The real fault line is **mechanism vs facts**: parse/aggregate/version-refuse/override/policy-
totality on one side, pin numbers on the other. Cut there instead:

- #400 ships the mechanism plus a deliberately tiny worked example chosen to *stress the schema*,
  not to be useful: **7400** (four identical disjoint sections) and **74139** (two sections
  sharing nothing) or **74153** (two multiplexers sharing select lines). The second one is the
  entire content of #349 Open Question 1 / #400 H1. Two rows settle whether `shared` pins are
  expressible; sixty rows do not settle it any better, and discovering it at row 40 is the
  expensive path.
- #450 becomes purely "grow the table to the named set, with per-row provenance and a
  cross-check" — and its self-imposed rule "no Java is added here except tests" (§7.4, §7.5)
  becomes true rather than aspirational.

## R2 — The row key should be (function number × family), not part number

This is the concrete alternative design I most want on the record, and it must land on #400
**before** #400 executes, because #450 forbids itself from touching schema fields.

#400 §7.4 keys `PartPackage` on part number with `family` as a column. The named teaching set is
~15 *function numbers* (00/02/04/08/32/86/74/138/139/153/157/161/163/245/283/373/374). Across
74LS/74HC/74HCT/74S/74ALS/74F that is 60–100 rows, each repeating an **identical** pin count,
pin-name vector, section map, section-equivalence class and footprint. Then #450 P5 asks for a
hand-maintained `substitution set` with closure and per-relation directionality (Open Question 4)
— an O(n²) edge set encoding a fact that is O(1): *the same function number in another family is
pin-compatible; only the drive figures differ.*

Model it as it actually is:

```
FunctionNumber  →  pin count, pin names, sections, equivalence class, footprint class
Family          →  electrical figures, floating-input behaviour, substitution lattice
Part            =  FunctionNumber × Family      (74LS08 = 08 × LS)
```

What this buys, all of it structural rather than tested-for:

- P5 becomes **vacuous** — a dangling substitution is not expressible.
- Open Question 4 (symmetric vs one-way) is answered by the family lattice's drive figures
  instead of asserted per edge, which is where "74LS→74HC is one-way on drive" actually lives.
- #349 Open Question 2 ("what is the default subfamily, and is it declared?") is answered by
  construction; there is no default to get wrong in parallel transcription.
- The transcription — where all cost and all error live — shrinks roughly 5×, and #349's own
  "bus factor 1 acquires a parts-curation job forever" risk shrinks with it.
- P3's pin arithmetic and P6's equivalence check run over ~15 objects, not ~90, so a failure
  message names one thing.

## R3 — Do not transcribe the electrical columns at all; ship the slot empty

Three artifacts exist in #450 solely to keep these columns harmless: a dedicated test class
(`ElectricalColumnsAreInertTest`), an ArchUnit call-site rule (Open Question 5), and a
falsification branch (H3) that redefines "inert" if the loader must read them. #450 §10 concedes
the check can pass *vacuously* while a fan-out number leaks from elsewhere. That is a lot of
machinery guarding a loaded gun whose only justification is "FEAT-027 can start reading them
without any data change."

Under R2 the electrical figures are ~6 rows of **family** data, not ~90 rows of part data.
Re-transcribing six rows when #341 lands costs an hour. So: define the columns in the schema
(no version bump later — #400's `Electrical` type stays), and **ship them empty**, with the
library test asserting the column is empty for every row until #341 lands. Absence is a data
assertion over one shipped file: it cannot be satisfied vacuously, it needs no ArchUnit rule, no
production-call-site archaeology, and no redefinition branch. An empty column cannot produce a
number the simulator contradicts. P8, H3, one test class and one blocking open question all
disappear, and invariant 3 of #349 is enforced *more* tightly, not less.

## R4 — Make the cross-check mechanical; it is the real deliverable

The issue spends its weight on licensing: an allow-list, a `NOTICE` file, per-entry license tags,
a new `LicenseAuditTest`, H1's copyrightability analysis. But H1 is almost certainly right (pin
numbering is fact), the repository is GPL-3.0-or-later, and #349's cost basis already names two
GPLv3 **Java** upstreams carrying exactly this data — Logisim-Evolution's `std/ttl/` (69
`Ttl74xxx` files of 108) and hneemann's Digital. That risk is largely disposed of in a sentence.

Meanwhile the risk both #349 (IC-8) and #400 (§11) name as the *unfixable* one — "a wrong pin
number produces a board that cannot work and a check that says nothing" — is delegated to a
**manual** per-entry comparison against a second source, performed sixty-odd times by one tired
maintainer. That is backwards.

Reframe: **generate, don't transcribe.** Write a build-time or scripts/ generator that reads both
GPLv3 upstreams, projects each into the `.parts` grammar, and emits **only the intersection**;
every disagreement becomes an omission carrying both readings. Then:

- IC-8 stops being a procedure and becomes a build step that cannot be skipped or fatigued.
- Provenance is a byproduct — each row names both sources and both licenses, so P2/P9 and the
  `NOTICE` file fall out of the generator rather than out of discipline.
- Re-absorbing a corrected upstream is a re-run, not a re-read, which is the answer to #349's
  curation-burden REPLAN trigger.
- It matches how this repository already establishes truth: ARCHITECTURE.md pins any future
  simulation strategy to "agree bit-for-bit with the #202 RV32I integration golden run as a
  differential oracle." Differential agreement is already the house epistemology; use it here.

Note the honest limit: neither upstream carries KiCad footprint strings, so the footprint column
is single-sourced from KiCad's naming convention (`Package_DIP:DIP-14_W7.62mm`) regardless. That
is a naming scheme, not a creative work, and the failure mode is a name that resolves to nothing
in the board tool — visible, not silent. Say so rather than implying the cross-check covers it.
Also note that CC-BY-SA-4.0 is one-way compatible with GPLv3 under Creative Commons' own
compatibility declaration; #450's recommended $L_{\mathrm{allowed}}$ omits it and #349 Open
Question 4 treats a share-alike source as flatly burdensome. Worth stating deliberately.

## R5 — A checkable inconsistency, today, between #450 and its blocker

#450 §7.6 requires each row to carry **equivalent-section sets** and a **substitution set**; P5
and P6 quantify over both. #400 §7.4's `PartPackage` tuple is *part number, family, pin count,
`List<Section>`, power pins, footprint, default value, `Electrical`, optional `Cascade`,
`Provenance`* — it carries **neither**. #349 §3's feature tuple does carry both ($g$ and
$\mathrm{subs}$), so the columns were dropped between feature and schema task, not by decision.

As written, #450 lands on a schema that cannot hold two of its own acceptance criteria, and #450
forbids fixing that in place ("If the transcription reveals a missing field, that is a finding to
raise on #400, **not** a field to add here"). Raise it on #400 now, while #400 is still unstarted
— together with R2, since R2 is the design that makes both columns cheap and P5 vacuous.

## Disregarded acceptance criteria, and why

I am explicitly setting aside three of #450's stated criteria:

- **P8 / H3 / the inertness test class** — superseded by R3's empty-column assertion, which is
  strictly stronger and an order of magnitude less machinery.
- **P5 as a checked closure over hand-written substitution edges** — superseded by R2's family
  lattice, under which the property holds by construction.
- **"§8's transcription is the method"** — superseded by R4's generator. The named teaching set
  should be the generator's *coverage target*, not a hand-typed list.

Everything else stands: P2, P3, P4, P6, P7, P9, P10 are all right, and P10 in particular (the
library must reach the installer resource set, O5) is the sort of failure that is invisible in CI
and total in the field — keep it exactly as written.

## Verdict

**endorse-with-reframing.** The capability is right, the timing is right, the scope discipline
(no geometry, footprint as an opaque string) is right, and the audience argument is real. But
route the work as: (1) fix the #400 schema key to function × family and restore the equivalence
and substitution columns *before* #400 executes; (2) let #400 ship two stress-case rows so the
shared-pin question is settled by data; (3) ship the electrical slot empty rather than inert; and
(4) make #450 a generator over two GPLv3 upstreams that emits the intersection, so the one risk
this project has repeatedly identified as uncatchable becomes a build step.
