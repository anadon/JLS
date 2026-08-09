# Issue #373: TASK-0008: a synthesized net keeps its name when an unrelated element is inserted, and a probe name that a waveform viewer cannot parse is refused where it is typed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What the issue is really for

Two ends, stated plainly: (1) a JLS net name should be a thing an outside artifact —
an SDF annotation, a board constraint, a lab handout, a diff — can be keyed on; (2) a
name JLS emits should never be one its own consumer cannot parse. Both are right, both
are on the project's arc (#129 plain-text saves, #165/#166 identity and canonical order,
the reproducible-build ethos the README spends four paragraphs on), and neither is served
today. I am endorsing the *ends*. I am rejecting the mechanism, and I am explicitly
disregarding the acceptance criteria that make "the nine sites read `getStableId()`" and
"one rule, `Util.isValidName`, at five attach sites" the definition of done.

## The mechanism does not achieve the end for the population that matters

`ElementId` is not one thing. `mintFresh()` gives `<per-install replica>:<counter>`
(`src/jls/elem/ElementId.java:36-60`); `legacy(long)` gives `legacy:<position in file
order>`, assigned in `src/jls/Circuit.java:1322-1334` to every element of every file that
lacks a saved stable id. Two consequences the issue never confronts:

- **For legacy files, stable id *is* save order.** Every pre-#165 course circuit, and
  every one of this issue's own golden fixtures, gets `legacy:N` where N is file position.
  Insert an element early, and every later element's `legacy` counter shifts — the exact
  arithmetic of O3, with `getID()` swapped for a differently-spelled positional index.
  This is not hypothetical: `test/jls/hdl/HdlCircuitBuilder.java` emits no `sid` line
  (grep for `sid`/`stable` in it returns nothing), so *the entire 79-file HDL golden
  corpus is named off `legacy:<position>`*. `NetNameStabilityTest` as specified in §8,
  built on that builder, is red before the change and red after it. H1 is refuted at the
  fixture, before the algorithm is reached.
- **For fresh files, the name stops being reproducible across installs.** Today
  `comb.v` exported by any JLS on earth contains `net_3`/`net_4`/`net_5`, because the
  dense index is a function of the canonical order alone. After this change it contains a
  digest of a per-install random replica id. `pom.xml`'s surefire `argLine` (`:270`,
  `:283`) pins only `java.awt.headless`; nothing pins `jls.replicaId`. Two students who
  draw the same circuit get netlists that can never be diffed or hashed against each
  other or against an instructor's reference — and the autograder/container audience the
  README advertises is precisely who loses that.

So the issue trades **inter-circuit reproducibility** for **intra-circuit edit
invariance** and never names the trade. Both are "stability". They are in tension, and
this project's whole character — byte-reproducible jar, `.buildinfo`, #166, a Nix flake —
says the one being discarded is the one it cares about more.

## A better seam: name the net from the circuit, not from the element's identity

**Alternative A — structural (content-derived) names.** Derive the synthesized suffix
from a canonical descriptor of the driving element's *local cone*: its `SaveTags` tag, its
persisted `Attribute` values, the output port index, and the user-visible names on its
fan-in boundary (port names, jump aliases, register names), ordered by the canonical order
#166 already provides. Then:

- invariant under any edit outside that cone — the property #336 actually sells;
- identical across installs, across legacy and fresh files, across `-savetext` round
  trips — because no replica id and no counter appear;
- no privacy surface, so #336's OQ1 ("digest vs. raw id", the thing said to *block
  filing*) simply evaporates: there is no id to leak;
- names that carry meaning (`net_and3_2`, not `net_a91f4c`).

Collisions between structurally indistinguishable nets are real and are broken exactly
where the issue says — in `HdlNames.synth` — but the tiebreak must be ordered by the
structural key, with the stable id demoted to a last-resort comparator. The issue's whole
scheme survives as the *tiebreak*, which is the right size for it.

Cheap variant if the descriptor is too much work now: rank the driving element within its
own type class under canonical order — `and_1`, `not_1`, `mux_1`. Not fully invariant
(a same-typed insertion still shifts), but readable, cross-install reproducible, and a
one-line change to the existing pass. Naming it here matters because it shows invariance
is not the only objective function; the issue optimizes it alone and gives up two others.

## The reframing that makes half the issue disappear: probes *are* net names

`grep -rn probe src/jls/hdl/` returns **nothing**. The exporter's precedence chain
(`src/jls/hdl/HdlExporter.java:280-350`) is port name → jump alias → `<reg>_q`/`_nq` →
synthesized, and it skips the one user-supplied net label JLS already has: the probe name.
A probe is a name a user typed, on a wire, saved in the file, GUI-attachable, and already
the VCD's variable name. Put it in the chain between jump alias and register outputs and:

- the "watch `net_5`" audience in the Abstract gets `watch carry_out` instead of `watch
  <digest>` — the pedagogy problem the whole issue is about is *solved*, not renumbered;
- the VCD and the Verilog name the same net with the same identifier. The
  "external-simulator VCD comparison" audience appears in the Abstract of both #373 and
  #336 and is served by neither as written: today the two artifacts name the same net
  differently, and after this task they still do. A cross-tool comparison keyed on a name
  cannot work until this edge exists;
- the two halves of this issue stop being two coincidences that both touch strings and
  become one feature — **the named signal, with two exits**;
- every user who dislikes whatever synthesized scheme wins gets an escape hatch, which
  defuses the epoch/breakage policy (OQ2) for everyone who cares enough to be broken by it.

Cost: a `Wire.getProbeName()` lookup over the group's wire ends and one `names.reserve()`
call, on top of a golden regeneration this issue is already paying for. This is the single
highest-value change in the vicinity and it is not in the issue.

## Legalize at the exit; do not narrow what a student may type

H3 is false by inspection, and the issue's own §11 half-notices it. `Util.isValidName`
(`src/jls/Util.java:219-234`) accepts any `Character.isLetter`, i.e. all Unicode letters;
IEEE 1364 §18 simple identifiers are ASCII `[A-Za-z_][A-Za-z0-9_$]*`. `π` passes attach
validation and still cannot appear in a `$var`. So the "one rule everywhere" ambition
fails on its first test — but the deeper point is that it was the wrong shape:

- JLS already owns the right pattern. `HdlNames.sanitize` (`src/jls/hdl/HdlNames.java:136`)
  legalizes arbitrary text at the HDL boundary and `renames()` reports the mapping;
  `BatchSimulator.toVcd` already legalizes *collisions* with `_probe`
  (`src/jls/sim/BatchSimulator.java:401-411`). Boundary legalization is the house style.
- The roadmap adds exits with different identifier classes — #321 Yosys JSON, #366
  KiCad/gEDA, the VHDL emitter's own pass. Making the input rule the intersection of every
  future exit ratchets a student-facing restriction tighter each time an emitter lands.
- It is a one-way door that does not even close: §7.11 correctly makes the load path a
  diagnostic, so a hostile or historical file still carries a bad name into memory and the
  `$var` guard is load-bearing regardless. The attach-time refusal buys an earlier error
  message and nothing else, at the price of five call sites and four distinct failure
  behaviours.

Better: **warn** at attach (the GUI already has the re-prompt loop), **legalize** per exit,
**report** the rename map. P4 still holds strictly. P3's apparatus mostly deletes itself.

## Enforce by type, not by grep

"`git grep 'getID()' -- src/jls/hdl/` returns no naming site, and the grep output is
pasted" is a convention, and conventions regress. The arc here (TASK-0005, #353) is toward
deleting the dense index outright. Restrict `Element.getID()`/`setID()` to the save/load
package — or route it through an accessor only `Circuit` holds — so `jls.hdl` cannot
call it at all. ARCHITECTURE.md already prefers exactly this (`HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `ExtensionPointCatalogTest`). A compile error is a better
completion criterion than a pasted grep.

## What I would do instead, in order

1. **Add probe names to the exporter's naming chain.** Additive, cheap, strictly
   improves both artifacts, unifies the issue's two halves. Land it alone.
2. **Fix the fixtures first.** Make `HdlCircuitBuilder` (and the corpus) carry explicit
   stable ids, or the stability test proves nothing whichever scheme wins.
3. **Choose the synthesized scheme against three properties, not one** — edit
   invariance, cross-install reproducibility, readability — and prefer structural
   derivation (A). Reproducibility is currently *held* and would be *lost*; that regression
   belongs in the decision record even if the decision goes the other way.
4. **Move probe validation to legalization at each exit**, warning at attach.
5. **Freeze last.** The issue's stated deliverable is the freeze ("the freeze is the
   deliverable, not the digest"). A frozen wrong convention with a declared epoch is worse
   than an unfrozen one, and this issue's sequencing writes the document in step two of
   §8, before any of the above is known.

The parent costs this at 0.5–1 maintainer-week. The issue is ~500 lines of apparatus over
it, and that apparatus is what made the fixture blocker, the reproducibility regression,
and the unconsulted probe name invisible — every one of them is a fact about the code the
document walks past. Shrink the ceremony, widen the question.
