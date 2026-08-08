# Issue #764: TASK-C548-1: an Examples menu entry lists the shipped circuits from the classpath — one top-level entry, no other default-view load
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) measures one thing: a stranger reaching a *running, understood* circuit in
ten minutes. #764 is the presentation slice of that. The menu is the cheapest part of it
and the least scarce: the scarce assets are (a) an addressable, described corpus and
(b) an open path that can read a circuit that is not a file on disk. #764 spends its
acceptance criteria almost entirely on the widget and glances past both. I endorse
shipping an Examples entry — it is the right lever — but the task should be re-cut so
that what it leaves behind is the corpus contract and the byte-source open path, with the
menu as a thin projection.

## Reframing 1 — ship the manifest first; the menu is a projection of it

The C548 ordering is inverted. #764 (menu) precedes #766 (corpus, whose AC-3 demands
"each circuit is categorized in data the menu reads") which precedes #768 (captions and
exercises the menu must show). That is three passes over the same menu-building code, and
the data contract — the one durable artifact — is defined last, by the two issues that
come after the consumer.

Worse, #381 §7.5 assumes a "classpath index of `resources/samples/`" as if enumeration
were free. It is not: a `ClassLoader` cannot portably list a directory resource inside a
jar, and `pom.xml:147-153` maps `resources/` to the jar root, so `resources/samples/*.jls`
becomes `/samples/*.jls` with no listable parent. The project already solved this problem
once and the answer is a committed index: `resources/help/Map.jhm` plus
`JLSHelpTOC.xml`, read by `jls.Help` (`Help.java:106,180`) and kept honest by
`HelpTopicsTest`'s completeness check. Do the same here.

Concretely: land `resources/samples/index` (or `examples.xml`, matching the help TOC
shape) as the *first* artifact of C548, with one row per example — resource path,
display name, category, caption, exercise, provenance. Then:

- #764's menu is `manifest → JMenu`, ~40 lines, and never changes again;
- #766's "categories in data" is a column that already exists;
- #768's captions and exercises are two more columns, with the length bound asserted over
  the manifest rather than over circuit internals;
- PF-4's gallery page, PF-3's welcome pane, and CAP-32's browser (#516) are three further
  projections of the same table rather than three re-implementations of enumeration.

One corpus, four projections, one completeness test. That is the same discipline
ARCHITECTURE.md's "Extension points: the typed seam catalog" section already records for
code seams, applied to content.

## Reframing 2 — the real prize is a byte-source open path, not a menu

AC-2 says the example opens "through the standard reader from the classpath". No such
path exists. `FileAbstractor.openCircuit(String filePath)` (`src/jls/FileAbstractor.java:99`)
does `File file = new File(filePath); if (!file.isFile())` → `LoadError.IO_ERROR`, and the
three sniffing probes (`readXZ`/`readZip`/`readText`) each take a `File`. A classpath
resource has no `File`. The three ways out are not equivalent:

1. extract the resource to a temp file and open that — pollutes, and hands the editor a
   circuit whose recorded directory is a temp dir (see Reframing 3);
2. add `FileAbstractor.openCircuit(byte[] bytes, String label)` carrying the same
   container-sniffing chain and the same #38 hostile-input caps, with the existing
   `String` overload reading the file and delegating;
3. leave `FileAbstractor` alone and let the Examples menu parse circuits itself — the
   "second sample mechanism" #548 explicitly forbids.

Option 2 is the right seam and its value far exceeds this task. Every future feature that
must load a circuit that is not a local file needs it: collaboration receiving a circuit
over the wire (#170 lineage), HDL/JSON netlist import (#61/#62), drag-and-drop, the CAP-32
circuit browser, and `SampleCircuitsTest` itself (#381 P8/P3), whose classpath assertion
becomes a one-liner instead of a temp-file dance. Samples are a few KB
(`riscv/gui/cpu.jls` is 8.9 KB), so a fully-buffered `byte[]` source is safe and keeps the
caps trivially enforceable. **I would make this the headline deliverable of #764** and
treat the menu as the demonstration that it works.

## Reframing 3 — an example is "New from example", not "Open"

The acceptance criteria guard the *read* side (#130, never `user.dir`) and say nothing
about the write side, which is where the real defect lives. `Circuit.setDirectory` feeds
two writers:

- `Editor.java:365` — `circuit.getDirectory() + "/" + circuit.getName() + ".jls"`;
- `SimpleEditor.java:5528` — `circ.getDirectory() + "/" + circ.getName() + ".jls~"`,
  built on the EDT and written by the background checkpoint thread.

`JLSStart.open` sets the directory from the file's parent (`JLSStart.java:2291`); a
classpath open has no parent. Leave it empty and JLS checkpoints a student's edited
example to `/full_adder.jls~` — filesystem root — from a daemon thread, every autosave,
silently. The fix is to make an opened example behave exactly like `newCircuit`
(`JLSStart.java:2205`): `circ.setDirectory(Util.defaultDirectory())` (i.e. `user.home`,
the #130-sanctioned seed) and mark it unsaved so the first Save is effectively a Save As.
That framing is also better pedagogy — an example is a starting point a student mutates,
not a document they own — and it makes AC-2's `user.dir` concern moot rather than merely
tested.

One adjacent hazard, worth an AC line: `newCircuit` calls `duplicateName` before creating
a circuit; `open` does not. A student who already has their own `full_adder` open and then
picks the bundled `full_adder` gets two same-named circuits in the tab pane and in every
other editor's import menu (`JLSStart.setupEditor` → `addToImportMenu`). Route the example
open through the same duplicate-name guard, or suffix on collision.

## On AC-3's shared `Action`s

As written this criterion can be satisfied vacuously. Today only the Edit and Element
menus reuse the editor's shared `Action`s (`JLSStart.java:1259` comment); File and Help
are anonymous `ActionListener`s (`JLSStart.java:1385ff`, `2074ff`). There is no existing
shared `Action` for "open example X" to reuse, so a compliant implementation can simply
construct one `Action` per item inside the menu builder and satisfy the letter of #381 P9
while sharing nothing. The identity that matters is *cross-surface*: the welcome pane's
"Open example" (#381 P2) and this menu must resolve to the same objects. Provide a single
`OpenExampleAction(ExampleRef)` factory owned by neither surface — keyed by manifest row —
and assert the welcome pane's instances come from that registry.

## The alternative I considered and would run alongside

JLS already ships a learning-content mechanism the Examples menu duplicates in miniature.
`Tutorial.PAGES`/`TITLES` (`src/jls/Tutorial.java:34-46`) are Introduction, 4-Bit Counter,
Full Adder, Sign Extension — literally the first three circuits #766's corpus wants, with
prose already written and link-checked. A flat menu of eleven file names delivers
"running"; it does not deliver CAP-27's "understood", and a tooltip is the wrong home for
a suggested exercise.

So: give each tutorial page an "Open this circuit" button bound to the same
`OpenExampleAction`, and give each example a help topic under `resources/help/` carrying
its caption and exercise — reusing `HelpTopicsTest`'s link checker and reachability
assertions instead of inventing a parallel metadata format in #768. That is most of
CAP-27 AC-5 ("lesson 1 completable from on-screen prompts") for a fraction of PF-5's
band, and it makes the Examples menu the discovery surface while the help tree remains the
explanation surface. Both read the same manifest. I would do this rather than choose
between the two.

## Where I disregard the stated criteria

**AC-1's flat "single top-level entry" listing ≥10 circuits.** Since #766 mandates
categories in data anyway, spend them: `Examples ▸ Combinational | Sequential | FSM |
Datapath | Showcase`, easiest first, with the RV32I CPU behind Showcase. A first-year
drawing an adder should not meet a 32-bit processor and a two-gate demo as peers in one
undifferentiated list — that flattening *is* conceptual load, which is precisely what K9
forbids, so AC-4 and a flat AC-1 are in tension. Better still, the top level holds four or
five recommended items plus "More examples…" opening the #516 browser once it exists.

**AC-5 (not constructed headless) is already true and costs nothing** — the menu bar is
built only in the GUI frame constructor, and every one-shot mode sets
`java.awt.headless=true` and never gets there (`JLSStart.java:171,284,366,481`). The
startup risk it should have named instead is KC-27-1: do not scan or parse circuits
eagerly at startup. Read the manifest lazily on first menu open, or bake it at build time;
never touch the `.jls` payloads until a user picks one.

## Trajectory

This pulls with the project's arc rather than against it, and duplicates nothing once the
manifest is shared. Resist one temptation: a `gui.example-provider` extension point.
#222/#223 record that seams get typed when a consumer exists; there is exactly one
provider here and speculatively typing it would be the first violation of a rule the
project just wrote down.
