# Issue #782: TASK-C553-1: "coming from Logisim-Evolution" and "coming from Digital" — two one-pagers mapping the gestures a switcher already knows
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two prose pages, half a maintainer-week, is not a claim about documentation. It is a
claim about **where JLS loses**: #510 scored the learning on-ramp 2/5 and every one of
seven teardowns independently concluded that a switcher bounces before discovering the
one axis where JLS is category-best. The pages are a bet that the bounce is a
*translation* failure — the switcher does not know where the thing they know lives here.

That bet is right, and at 0.5–1 mw it is the cheapest thing on the board. My argument is
not with the outcome; it is that this task, as scoped, throws away most of what it
produces. The research needed to write "coming from Logisim-Evolution" honestly *is* the
`.circ` importer's element-mapping table and loss vocabulary, and the issue's AC-5 fence
— drawn around code, but landing around the vocabulary too — guarantees it gets written
twice, years apart, by different hands. The reframing below keeps the fence on code and
moves the durable part of the work to where four other issues can consume it.

## Reframing 1 — author the mapping as data; the page is one rendering of it

The genuinely hard content here is a construct-by-construct map, and it is not the shape
of #545's table. A capability table has one row per capability; a migration guide needs
one row per *construct*, with a gesture and a loss note. Some real rows, from this tree:

- Logisim/Digital **Tunnel** → JLS `JumpStart`/`JumpEnd` (`src/jls/elem/JumpStart.java`,
  `JumpEnd.java`) — a *directed pair*, not a set of same-named nodes. Genuinely different;
  a switcher who types the same label twice gets nothing.
- **Where subcircuits live** — the single biggest mental-model break, and invisible in any
  comparison table. Logisim: one `.circ` project holds a tree of circuits, other projects
  load as libraries. Digital: a `.dig` file on disk *is* a placeable component, by
  reference. JLS: several circuits are tabs of one `.jls`, and `SimpleEditor.doImport`
  (`src/jls/edit/SimpleEditor.java:5463`, `finishImport` at `:679`) **copies** the
  imported circuit into the current one — the code's own comment at `:516` calls it
  "silently copies." A Digital user's reflex ("edit the component file, every user
  updates") is simply false here. This one sentence is worth more than a page of
  palette equivalences.
- Logisim **Combinational Analysis** → JLS `TruthTable` element — near-equivalent, and a
  win worth naming.
- Logisim's inferred bit widths / **Splitter** → JLS explicit widths plus the
  `Splitter`/`Binder` pair.
- Logisim **Poke tool** → JLS interactive simulation clicking (no separate tool mode).

Now note who else needs exactly that table: CAP-16 (#311)/#323/#451 cannot write a `.circ`
importer without it, and #513 PF-1 wants a format-agnostic *loss report* whose vocabulary
is the loss column. **Concrete proposal:** the deliverable of this task is
`docs/migration/logisim-evolution.md` + `digital.md` **containing a machine-readable
construct table** (source construct → JLS equivalent → gesture → example id → loss note →
owning issue), with the prose being connective tissue around it. Authoring a requirements
table is not importer work; no parser, no format code, AC-5's fence holds. What changes is
that the map becomes a design input the importer inherits instead of a doc the importer
later contradicts.

(#784's visionary review argues the same "claims must be derived, not typed" seam from the
capability side and proposes a ledger. The construct map is the second half of the same
per-competitor record: `{capability claims → #545's table, construct map → these pages and
the importers, deficits → both}`. One record per competitor, four renderings.)

## Reframing 2 — the Digital half is aimed at the segment #510 says is not coming yet

#510 §3 and §5 are unambiguous: Digital's *users* are winnable "on a 1–2 yr horizon **if
the stall becomes visible**"; Digital's *contributors* are "winnable NOW" and are called
the survey's clearest strategic finding — a named, reachable pool with two ambitious PRs
closed unmerged in late 2025. Meanwhile a gesture map is the *thinnest* artifact for this
audience: JLS and Digital are both Java Swing schematic editors whose drawing gestures
already rhyme. What a Digital user cannot guess is the deficit ledger (no per-bit HiZ —
`docs/simulation-semantics.md`'s two-state+HiZ domain; no parameterized circuits; no FSM
editor or truth-table synthesis parity; no published benchmark against their 120 kHz; no
FPGA flow) and the maintenance story.

So: **the Digital page should be organized as a deficit ledger plus a maintenance record,
with the gesture map demoted to an appendix, and it should carry a contributor section** —
the typed extension-point catalog (`docs/extension-points.md`, #223), the seam discipline
in ARCHITECTURE.md, `CONTRIBUTING.md`, PR turnaround, and the two features their tracker
has wanted for years (dark mode, their #1477 / our #289; live subcircuit dive, their #84).
That section costs a paragraph and is the highest-leverage prose in this whole capstone.

One constraint I would write into the task: **never characterize Digital as declining on a
public JLS page.** State JLS's own facts — release cadence, signed reproducible builds, PR
turnaround, a normative semantics spec — and let a reader who already knows Digital's
tracker draw the conclusion. #510's positioning statement ("the maintained, modern
successor in the Digital tradition") survives that discipline; a page that reads as an
obituary for a one-man project poisons exactly the contributor pool it is courting.

## Reframing 3 — the out-of-the-box route: make the corpus the guide

The most elegant version of this task may not be a page at all. If #548's ≥10 curated
circuits were deliberately chosen as **the circuits a switcher already built in the tool
they are leaving** — Logisim's own tutorial sequence, Digital's standard examples — then
the switcher opens a file, recognizes the circuit, and learns the mapping by
pattern-matching in seconds, having read nothing. That is CAP-27's actual bar ("without
reading anything longer than a caption"), which a five-minute page structurally cannot
meet. The pages then shrink to a one-screen construct table plus "open Examples → Ripple
Adder; it is the one you already built."

This also repairs the dependency inversion in AC-2. As filed, the page must link a shipped
example per major concept while `ordering_after: []` and no user-facing example ships
today (`examples/` holds only `autograde/`; the four `.jls` files in the tree are test
fixtures and the unsurfaced `riscv/gui/cpu.jls`). Treating the corpus as the substrate
rather than as citations makes #548's curation the deliverable's real dependency and says
so honestly.

## The success metric is wrong

AC-1 asks for "readable in under five minutes" inside a window CAP-27 measures at ten. A
page that consumes half the budget it exists to protect is not obviously a win. The metric
should be **time to first running circuit**, and each page should be spined on one worked
crossing: *your Logisim adder, redrawn here in four minutes, simulated, then graded with
`-t`* — ending on `docs/batch-interface.md`, JLS's only 5/5 axis and the thing no rival
documents at all. That is a lesson, and it shares a spine with CAP-27 PF-5; deciding now
that the migration page and lesson 1 are the same artifact in two framings avoids
authoring the content twice.

## Deficits go stale in the dangerous direction

AC-3's "what JLS does not yet do" is the most valuable sentence on each page and the most
perishable. When CAP-23's chronogram or parameterization lands, a page still saying "JLS
has no waveform view" is worse than no page — it is a maintained document lying about a
shipped feature, in front of the exact audience being courted. Make every deficit line a
row keyed to the issue that closes it, and fail the check when a named deficit's issue is
closed. Note this is the *opposite* direction from #784's importer-slot guard, and it is
the direction #510's adversarial pass actually caught in this repo (`jls.hdl.imp`
shipped, reachable from nothing). Deficits with issue numbers also read as confidence
rather than apology.

## What I am explicitly disregarding

- **AC-2's "links at least one shipped example per major concept"** as a citation
  requirement — replaced by Reframing 3: the examples are the guide, and this task is
  ordered after #548 or it ships with the links stubbed and says so.
- **AC-1's implied symmetric shape for the two pages.** Logisim-Evolution gets the
  construct map (the concepts really do differ); Digital gets a deficit ledger plus a
  contributor invitation, per #510 §5.
- **AC-4's "reference #545's table" as the whole consistency story** — referencing is a
  discipline, not a mechanism. One per-competitor record rendered into both surfaces makes
  disagreement impossible instead of merely discouraged.

## Verdict

**endorse-with-reframing.** Fund it now — it is small, it is on the critical path of the
cheapest capstone, and the fence against importer work is the right fence. But change what
it produces: a machine-readable construct map (Logisim) and a deficit-plus-contributor
ledger (Digital), sourced from one per-competitor record, spined on a worked crossing that
ends at the grading interface, with deficits keyed to their closing issues. The prose is
the byproduct; the durable artifact is the mapping the `.circ` importer will otherwise pay
to discover a second time.
