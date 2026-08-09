# Issue #559: FEAT-C29-3: a CircuitVerse .cv project opens in JLS with its subcircuits intact, and every construct that only looks preserved — starting with queue-priority delay — is named in the report
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "JLS reads one more file format." Per #510 §3, the CircuitVerse row's winnable
segment is *instructors who grade at scale, timing/HDL courses burned by engine bugs,
and offline/locked-down labs* — and its minimum bar is four items, of which the `.cv`
importer is one. The issue's own outcome sentence names the audience correctly
("grading-burned CircuitVerse instructor") and then delivers a construct-level importer
that touches grading nowhere. That gap is the review.

The deeper claim is sound and I endorse it: JLS's differentiator is honesty about
semantics (a normative `docs/simulation-semantics.md`, golden-pinned, an RV32I
differential oracle recorded in ARCHITECTURE.md), and an importer that admits what it
cannot preserve is the same value expressed at the migration boundary. AC-3 is the best
sentence in the whole CAP-29 family. It is also, as written, in the wrong place, at the
wrong granularity, and pointed at the second-most-important divergence.

## 1. The spine already ships; it is being generalized from the wrong end

`src/jls/hdl/imp/` exists at head: `NetlistImporter.java` (1,067 lines),
`ImportResult`, `ImportSummary`, the documented no-partial-circuit discipline, and an
engine-neutral layout seam (`jls.hdl.layout.SchematicLayouter`). #323 §"Background"
already identifies this as the structural precedent and its Open Question 4 asks who
promotes `NetlistImporter.Builder` (it is `private static final class` at line 410).

Yet #559's `ordering_after` chains it behind #556, which chains behind #323, which is
`blocked_by: [314, 349]` and whose Open Question 2 — a GPL "or-later" question about
absorbing the incumbent's geometric connectivity code — explicitly **blocks filing
children**. So the plan routes the format #510 calls "the single highest-leverage
migration lever" behind 74-series DIP part-data transcription and a licence question,
for a JSON importer that needs neither part data nor a single line of Logisim code.

**Reframing:** generalize the import spine from the code that exists, not from the code
that doesn't. `.cv` is the cheapest format in the family (plain JSON, no XML hardening,
no XStream, no geometric connectivity rules) and is therefore the right *first* new
importer — the one that promotes `NetlistImporter.Builder`, establishes
`jls.imp.ForeignCircuit` as the shared construct model, and hands #556 a second real
consumer to generalize *from* instead of a hypothetical one to generalize *for*. #556
generalizes only the report — the shadow of the mapping. The body (construct model,
hierarchy resolution, layout fallback, no-partial emission, totality bookkeeping,
undo integration) is what three importers will otherwise implement three times.

## 2. There is no import surface at all — and "Import" is already taken

`NetlistImporter` is referenced by nothing outside `test/` (#510 fn. 2 says the same:
"wired to no CLI flag or menu, hence unreachable by users"). AC-5's "import is undoable"
presumes a File→Import path that does not exist in `SimpleEditor`/`Editor`/`JLSStart`.

Worse, the word is occupied: `SimpleEditor.addToImportMenu` / `Editor.doImport` /
`Circuit.isImported()` already mean *pull a subcircuit in from another open circuit*
(`Palette.java:20` calls out the Import button as the one toolbar control outside its
table). Adding a foreign-format "Import" collides with an existing, load-bearing verb
in the same menu bar.

**Concrete alternative:** CAP-29 needs a PF-0 that #559 must not invent privately — one
migration surface (menu item plus a `-import <file>` CLI flag, headless-clean so
autograders can use it), one report viewer, one undo integration, named
"Open foreign circuit…" or "Migrate…", never "Import". Landing it with `.cv` makes the
already-shipped Yosys importer reachable as a side effect: the cheapest score in #510.

## 3. AC-3 is the wrong granularity, and it contradicts the contract it inherits

#556 AC-2 states the totality assertion as an **equality**: `C_src \ C_out = R`. AC-3
asks for a construct that *is* imported (so it is in `C_out`) to nevertheless appear in
`R`. Under the inherited schema that is a violation, and #559's own boundary comment
forbids it from forking or varying that schema. The honest resolution is not local:
the shared contract needs a third disposition — `realized-with-divergence` — and it
must be added in #556, by #556, before #559 can meet AC-3.

Granularity is the bigger problem. Flagging every element carrying a `delay` attribute
produces a report that fires on nearly every project and trains instructors to ignore
it — precisely the failure #323 §3 warns about ("a construct reported but actually
realised is a report that trains instructors to ignore it"). The claim worth making is
per-circuit, not per-attribute: *this project contains N feedback paths / multiply-driven
nets whose CircuitVerse behaviour depended on event-queue ordering; JLS will simulate it
under `docs/simulation-semantics.md` and may differ.* That is computable from the
imported netlist, it is falsifiable, and it degrades gracefully to silence on the
combinational circuits where the two tools genuinely agree.

Generalize it once more and AC-3 stops being a CircuitVerse quirk: *every* source tool
has a timing model that is not JLS's (Logisim's, Digital's gate delays, Falstad's
emergent analog). The shared contract should carry a `semantics-delta` disposition and
each importer should ship a one-page per-format timing-delta document. Written that way,
the same honesty covers `.circ` and `.dig`, where it is currently unguarded.

## 4. AC-2's real hazard is definitional identity, not nesting

AC-2 treats hierarchy as the safe part and delay as the danger. The code says otherwise.
`Circuit.subElement` is a **single** `@Nullable SubCircuit` (`Circuit.java:49-50`), and
`SubCircuit.copy()` (line 332) deep-copies the whole nested circuit into a fresh
`Circuit` for each placement. JLS subcircuits are copy-by-value: there is no shared
definition. CircuitVerse scopes are referenced by id and reused across a project.

So a project reusing one scope in five places imports as five independent copies:
nesting preserved, *reuse* destroyed. The instructor who fixes a bug in the shared
subcircuit now fixes it five times, and the five copies silently drift. #510 scores JLS
hierarchy/reuse 2/5 against CircuitVerse's 3/5 — this import runs from a stronger reuse
model into a weaker one, exactly the situation the honesty report exists for. AC-2's
"unmappable subcircuit constructs refuse by name" does not catch it, because the
construct *is* mappable; only its identity is not. Name this in the report as the
flagship divergence, alongside (arguably ahead of) queue-priority delay.

## 5. The corpus is the asset — make `.cv` the family's measuring instrument

CAP-16 gates its estimate on a corpus measurement (KC-16-1, #323 I6). CAP-29 has only a
stop-loss (KC-29-1) — no measurement gate at all, despite `.cv` having by far the
cheapest corpus in the niche: ~1.1M public CircuitVerse circuits (#510 §1).

**Out-of-the-box reordering:** ship the parse-only half first — `.cv` JSON → construct
histogram, no JLS realization, no UI, no mapping decisions — and let the measured
construct frequencies design the mapping tables for the whole family. That is a few days
of work, it converts CAP-29's estimates into numbers before 13–20 mw are committed, and
it makes `.cv` the capstone's instrument rather than its third feature. It also answers
the question nobody has data on: what fraction of real student projects use constructs
that survive at all.

## 6. Missing slice: the vectors, which are what actually convert a course

CAP-29 gives `.dig` a whole planned feature for test-vector translation (PF-5, "the
piece that actually converts courses") and gives `.cv` nothing equivalent — even though
`.cv`'s named audience is the grading-burned instructor and JLS's only 5/5 axis is
autograding. CircuitVerse ships a testbench feature whose data travels in the project
JSON; if that holds on inspection of a real export, translating it to `-t` vector files
outranks AC-3 for the stated audience, and it is the one deliverable that turns
"my circuits open" into "my course runs." Verify it against a published project before
the mapping table is fixed; if the data is not there, record that as the reason the
`.cv` path stops at circuits.

## 7. Two smaller gaps

- **Format versioning is absent from the ACs.** `.cv` is emitted by a live web app
  mid-rewrite; its JSON has drifted and will drift again. JLS already has the right
  precedent — `Circuit.readFormatHeader` refuses a newer `FORMAT` as `NEWER_FORMAT`
  rather than misparsing (#79). Pin the accepted `.cv` schema version and refuse
  unknown ones loudly; best-effort parsing of an unpinned moving target is how silent
  losses get manufactured.
- **KC-29-1's downgrade ("a documented external-conversion recipe") is treated as
  failure.** For the parse/measurement half it is arguably the *better* default: a
  standalone converter absorbs upstream format churn without touching JLS's release
  cadence. In-tree wins once the shared surface from §2 exists, because the report and
  the undo integration are the product. Say which half lives where, deliberately.

## What I am disregarding, and why

I am setting aside AC-3 as written (a per-attribute flag on `delay`) and the
`ordering_after` chain. The first is a true statement made at a granularity that will
train its readers to ignore it, and it needs a schema change in #556 to be legal at all;
the second sequences the cheapest, highest-leverage format behind an unbuilt XML importer
that is itself blocked on a licence question. The outcome the issue is reaching for —
a CircuitVerse course that opens in JLS with its losses named — is right, and worth more
than either.

**Verdict: endorse-with-reframing.** Keep the feature; move it to the front of CAP-29 as
the importer that promotes the shipping `jls.hdl.imp` spine and the shared import
surface; recast AC-3 as a per-circuit timing-divergence verdict backed by a
`realized-with-divergence` disposition owned by #556; add subcircuit definitional
identity as a named divergence under AC-2; put the corpus histogram and the testbench/
vector question ahead of mapping breadth.
