# Issue #614: TASK-C558-2: every Digital element maps to a JLS element by semantics from a written table, with the name only ever a hint
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its format, #614 is the load-bearing claim of the whole four-format
migration program (#556 `.circ`-shaped report → #558 `.dig`, plus the `.cv` and
Falstad siblings): **JLS will only accept a foreign circuit whose meaning it can
account for, and it will say so in the vocabulary of meaning rather than of
labels.** That is the same claim `docs/simulation-semantics.md` makes internally,
that `SaveTags`/`ElementRegistry` make about the save grammar (tag text never
reaches `Class.forName`), and that `LoadError`/`TellUser` make about failure.
The issue is not really about Digital. It is about whether JLS's element
vocabulary is a *specified semantic domain* that foreign models are translated
into, or a bag of ~35 Java classes with suggestive names. #614 is where that gets
decided for the second time, and the second time is when the discipline either
becomes a shared mechanism or fractures into per-format dialects.

Judged on that arc, the direction is right and the work is required. Three things
about the *route* are worth changing, and one of them is close to free.

## Where the issue pulls against the project's own idiom

**AC-1 quietly inverts the doc/code contract the sibling task already fixed.**
#451 (TASK-0054, the `.circ` sibling this issue explicitly inherits from) states
the discipline as P7: `docs/logisim-construct-map.md` is *pinned against the code
in both directions* by `ConstructMapTest` — "the discipline
`ExtensionPointCatalogTest` applies to `docs/extension-points.md`." That is the
repo's idiom everywhere: `ElementRegistryTest` over `ElementRegistry`,
`HelpTopicsTest` over `Map.jhm`/`JLSHelpTOC.xml`, `ExtensionPointCatalogTest` over
`docs/extension-points.md`, `CliFlagTableTest` over the `FLAGS` table. Code holds
the truth; a test forbids the document from drifting from it, in both directions.

#614 AC-1 instead says "the code reads it rather than restating it," making a
`docs/` markdown table a runtime data source. That is a *different* mechanism
from its own sibling, for the same program, three weeks apart. Concretely it
means a prose edit can change simulation-affecting behaviour, that the parse of
the markdown becomes a second untrusted-ish input, and that `.circ` and `.dig`
importers ship two incompatible answers to the same question. **Reframe AC-1** to
the #451 shape: the mapping lives in typed code (or a versioned classpath data
file under `resources/`, the `FEAT-040`/part-data idiom), and a both-directions
test pins the reviewable `docs/` table against it. The instructor still gets a
document; the build still fails when a row and a mapping disagree.

**AC-2's rule is stated one notch off from what it means.** A table keyed by
Digital's element type name *is* keyed by a name. The real invariant is: **there
is no default rule from name equality to element identity** — every row is
curated, and an unlisted foreign type refuses. State it that way and the test
becomes writable: assert that removing a row turns a mapping into a refusal
rather than into a name-matched fallback. Also, the collision the issue inherits
is `.circ`'s, not `.dig`'s. The `.dig` collisions must be enumerated fresh; the
in-repo evidence already names the trap shape — JLS's `ShiftRegister` is
combinational with no clock and no stored value (`docs/simulation-semantics.md`
§6.3, `HdlExporter.java:84`), and JLS's `Register` has fixed edge semantics and
no reset pin (§8.1; `docs/hdl-support-research.md` §7.2 records async-reset FFs as
a loud-reject gap). Those, plus `Memory`'s async single-port/tri-state shape, are
the concrete rows where Digital's same-named parts diverge. Name them in the AC;
inheriting "there exists a collision" as a fact is how the check rots into a
single fixture nobody revisits.

## Reframing 1 (the important one): the source file already contains the oracle

AC-2/AC-3 ask a human to certify semantic equivalence in a table review. That is
the weakest oracle in the program — `#451` §11 says so outright ("a wrongly-mapped
construct produces a circuit that loads and simulates ... the map's completeness
is itself an assumption").

But `.dig` is not `.circ`. **Digital files carry embedded test sections** —
TASK-C558-1 (#612) AC-4 already requires them preserved byte-recoverably, and
#562 translates them to `-t` vector files. JLS already has the runner
(`docs/batch-interface.md` §2, `BatchSimulator`, the golden-test harness). So the
migrated circuit can be graded by the instructor's own expected values, from the
same file, with no Digital installation and no hand-written fixture:

> import → translate the preserved test section → run under `-t` → the verdicts
> must match the values Digital recorded in the file.

That converts "mapped by semantics" from a reviewed assertion into an executed
one, per real circuit, and it costs only a thin, test-only slice of #562's
translator pulled forward. **I am disregarding the current ordering** that puts
#562 downstream of #558 as an accessory: the test-vector translator is not an
accessory, it is this task's oracle, and the program is stronger if a minimal
read-only slice of it lands as #614's verification substrate. Every mapping row
then carries an evidence column: either a `docs/simulation-semantics.md` section
that argues the equivalence, or a vector fixture that demonstrates it. Rows with
neither are `mapped-with-caveat` by construction (#619 AC-5 already invented that
category — this gives it a decision procedure instead of a judgement call).

## Reframing 2: AC-4's connectivity check is self-referential as written

"An imported circuit's net partition matches the source's" — but *the source's
partition is computed by our own parser*. If the `.dig` geometry/port-offset model
is wrong, both sides are wrong identically and the test is green. #323 §3 is
explicit that this is the worst available failure mode ("silently disconnected —
because the file opens and looks right"), and it is exactly the class of bug a
self-comparison cannot see. AC-4 needs one independent witness. In order of cost:
(a) the embedded test vectors above (behavioural, catches disconnection
immediately); (b) Digital's own exported netlist for the same file, compared
structurally; (c) at minimum, a hand-authored partition for two corpus circuits,
committed as data rather than derived.

## Reframing 3: the alternative route the issue never considers

Digital ships Verilog/VHDL export. JLS already has a *complete* foreign-netlist
import path — `jls.hdl.yosys` + `NetlistImporter` (1,067 lines) + the layout seam.
So there is a route that needs no `.dig` element table at all:
`.dig` → Digital's own exporter → Yosys `write_json` → existing importer.

It is not the primary route, and it should be written down as rejected-with-reason
rather than left unconsidered. It loses geometry (#617's whole deliverable),
flattens hierarchy, requires two external tools an instructor does not have, and
above all destroys the per-construct loss report that is the entire product here
(a Yosys netlist has already discarded the constructs the report must name).
**But it is precisely KC-29-1's declared fallback** ("downgrades to a documented
external-conversion recipe"), and it is a ready-made differential oracle for
Reframing 2(b). Pre-costing it now — one paragraph, one scripted recipe — makes
the kill criterion executable instead of aspirational, and buys the independent
witness AC-4 lacks.

## Reframing 4: one construct registry, not four format tables

#556 generalizes the *report* to one contract so four importers do not emit three
dialects. The same argument applies one layer down and is not being made: four
importers will otherwise grow four mapping mechanisms, four totality tests, and
four notions of "refusal". The natural seam — and the one `docs/extension-points.md`
already reserves as `hdl.importer`, *"cell-map/layout contract to be defined"* —
is a single foreign-construct registry: rows of `(format, foreign type, attribute
predicate) → realization | refusal(reason, successor issue)`, with **refusal as
the default disposition** so totality is structural rather than asserted. That
gives `.cv` and Falstad a table to fill in rather than a mechanism to invent, and
it makes #614 exactly what its own boundary note wants it to be for the report:
*content, not carrier*. Filling that seam is arguably #556's business (it already
owns "one voice"); either way it should be decided before #614 writes the second
bespoke mechanism, because after the third it will not be re-decided.

## Ordering and cost

`ordering_after: [TASK-C558-1, 323]` makes this task hostage to a 6–12 mw feature
whose own open questions (licence, corpus procurement, the `NetlistImporter.Builder`
promotion) are unresolved. What #614 actually needs from #323 is the *rule*, the
promoted builder, and the report machinery — i.e. #451 and #404 (the loud loader,
without which "realized" is not observable and every totality claim is confidently
incomplete). Narrow the edge to those; otherwise the Digital play stalls behind
the Logisim corpus run for reasons that have nothing to do with Digital.
The 2 mw band is credible only under default-refuse: Digital's element set is
several times JLS's ~35, so a "row per Digital element type" table is mostly
refusals. Write it that way deliberately — a short mapped list plus a total
refusal rule is both cheaper and more honest than a long table of approximations.

## Verdict

**endorse-with-reframing.** The goal is right and central. Keep AC-2's spirit and
AC-3 unchanged. Change AC-1 to the `ConstructMapTest` doc-pinning discipline the
sibling already chose; restate AC-2 as "no default-by-name-equality" with the
`.dig` collisions enumerated fresh; strengthen AC-4 with an oracle that is not our
own parser; and pull a read-only slice of #562's vector translator forward so the
mapping is graded by the instructor's own test data rather than by a table review.
