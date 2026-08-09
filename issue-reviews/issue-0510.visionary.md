# Issue #510: Niche comparison survey, August 2026: head-to-head teardowns, winnable segments per competitor, and the universal gates to drawing their users and developers
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is, mechanically

Not a work item. It is a strategy memo whose output has already been spent: the
first comment converts it into 12 new capstones (#511–#522) and a 41-row
gap→owner table; the third comment (2026-08-08) reports that the table's chains
now reach 77 features and 269 tasks. So a review of #510 cannot usefully argue
about its acceptance criteria — there are none. The only question worth asking
is the one the lens demands: **is the thing this document says JLS should become
the right thing?**

## What it is really for

The charge is stated as a distribution problem: draw each competitor's users and
developers via on-ramp, technical superiority, elegance. The document then
scores JLS on twelve dimensions borrowed wholesale from the competitors it is
measuring, and — inevitably, because that is what borrowed rubrics do — produces
a catch-up program. JLS is 1–2 on community, momentum, on-ramp learning,
scale/perf, hierarchy, extensibility, so the plan becomes: buy a chronogram, buy
parameterization, buy FSM synthesis, buy four importers, buy a browser demo, buy
truth-table minimization, buy packaging channels, buy a contributor funnel. Each
of those is individually defensible. Together they are a multi-year program to
be second-best at eleven things in order to win a matrix that exists nowhere
outside this repository.

The document's own evidence points somewhere else and does not follow it. JLS is
category-best on exactly one axis — testing/grading, 5/5, where the survey notes
*no competitor documents grading semantics at all* — and that axis is not a
feature. It is a **contract**: `docs/batch-interface.md` (normative `-t` grammar,
watched-element output, VCD profile), `docs/simulation-semantics.md` (normative
time/delay/edge/HiZ model), goldens that pin both
(`test/jls/BatchSimulationGoldenTest`, `SequentialGoldenTest`,
`VcdExportGoldenTest`), deterministic exit codes (`JLSStart.usageError`, 0/1/2),
byte-reproducible jar, signed provenance, and a multi-arch headless container.
Nothing else in the niche has any of that, and — critically — none of it
requires a user to like JLS's canvas.

## Reframe 1: stop competing as an editor; become the substrate

**The alternative framing: JLS is the reference semantics and grading engine of
this niche, and the importers are its front door rather than its migration
ramp.**

The survey's whole §3 is organized around *switching* — winnable segments,
minimum bars, bounce lists, "a switcher from any competitor leaves in the first
ten minutes." Switching is the hardest possible adoption ask, and it is the one
where JLS's structural disadvantages (no browser, no community, empty first
launch) all bind at once. Under the substrate framing nobody switches. An
instructor keeps Logisim-Evolution or CircuitVerse or Digital as the drawing
tool their course already uses, and runs

    docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests circuit.circ

in their autograder, because JLS is the only tool in the niche whose grading
behavior is a citable, versioned, golden-pinned contract, and the only one whose
timing model is written down normatively rather than emergent (Falstad) or a
queue priority (CircuitVerse #1412). That is adoption *without* the on-ramp gate
the survey correctly identifies as blocking, because the adopter is an
instructor with a CI pipeline, not a student clicking a canvas.

This inverts the priority of CAP-29 (#513, the importer family). The survey
ranks importers as "migration levers, in value order" — instruments of
conversion. Under the substrate framing they are the product surface: every
importer widens the set of courses JLS can grade *for* without asking anyone to
change tools, and the shared loss-naming report infrastructure the survey
already proposes becomes the honest interface ("here is what JLS could not
faithfully read, by name") rather than a migration consolation prize. It also
re-prices them: an importer that only needs to feed the headless batch path does
not need editor fidelity, dialogs, geometry, or round-trip save — a very
different and much cheaper deliverable than "open a .dig in the GUI."

It also changes what "technical superiority" has to mean. As a substrate, the
things that matter are the per-bit X/Z value domain (#322), the loss-naming
importers, and published performance receipts — because grading correctness and
throughput are the product. The chronogram, dark mode, FSM editors and
truth-table minimizers are editor-competition items that a substrate strategy
can *defer* rather than treat as blocking gates in five of seven teardowns.

## Reframe 2: the developer-draw play is aimed at the wrong pool

§5's play is to recruit Digital's rejected contributors by implementing their
rejected features (dark mode, live subcircuit dive, keybindings) and inviting the
PR authors by name. That is a pool of roughly two to three identifiable people
whose demonstrated motivation was to modernize *Digital's* UI, and it asks JLS to
absorb the exact contributor-onboarding cost the survey itself measures as
prohibitive: 30–60KB spec-prose issue bodies, a ~700-line template rule system,
a tracker the second comment concedes "reads as an internal monologue."

The honest reading of this repository's revealed development model — a
maintainer plus an agent fleet, of which these 1,210 issue reviews are the
current instance — is that JLS does not need a contributor commons to out-ship
this niche. Logisim-Evolution has 147 contributors and a 217k-LoC legacy
`com.cburch` codebase at ~4% test ratio; that is what the commons buys here. The
durable contributor story for a bus-factor-1 project is not PR throughput, which
one maintainer can technically promise and cannot institutionalize, but the
**typed extension seam already recorded in ARCHITECTURE.md** (#222 trust
boundary, #223 `jls.module.ExtensionPoint` catalog in `docs/extension-points.md`,
the `ServiceLoader` direction left behind by the #80 plugin removal). Third
parties who can ship an element provider, an importer, or a grading adapter
without merge rights are worth more than five rescued PR authors, and that is a
capability JLS has designed and not shipped. CAP-30 #514 PF-4 contains this; it
is ranked below the recruitment theater rather than above it.

## Reframe 3: the four universal gates should be cut out of the capstone machinery

§4 says the shop window and first-run gaps are "days" and "small," and that they
gate everything else in the document. The response was to route them through
CAP-27 #511 → six features → sixteen tasks. A shop window that requires a
capstone has already lost the argument its own document makes. The visionary
move is boring and I will state it plainly: **one branch, this week, owned by no
capstone** — three screenshots and a GIF in README.md, four or five example
circuits under `examples/circuits/` surfaced by a File > Examples menu item, a
starter circuit or the tutorial on empty launch instead of the bare
`JTabbedPane` (`JLSStart.java:1274`), and the `riscv/bench_kernel.py` numbers
pasted into README with the machine named. That is a day or two of work that
changes every first impression in the document, and it should not be scheduled
behind a planning artifact.

The third comment supplies the corroborating evidence: gate 3 (chronogram) turns
out to depend on a physical time unit "nothing in JLS can declare" (#882 → #682),
unrecorded on either end until 2026-08-08. The gate the survey priced at 3–4
maintainer-weeks has an unbounded prerequisite; the gates it priced in days are
the ones actually reachable. That asymmetry argues for shipping the cheap gates
outside the machinery and letting the expensive one stay in it.

## Reframe 4: the web question is about artifacts, not a port

The survey frames zero-install as "the unbridgeable moat" and proposes a
CheerpJ-wrapped read-only demo (CAP-32 #516). There is a smaller and better-aimed
version of the same idea that requires no port at all: JLS already emits SVG
(`-i out.svg`) and VCD (`-vcd`) headlessly, and #886 puts a circuit in a URL
fragment. The evaluation-cost problem is solved by *every JLS artifact being a
link* — a gallery of SVG renders, waveform views rendered from VCD, a shareable
fragment URL — all of which are static files produced by the batch path an
autograder already runs. That is the web presence a substrate strategy needs,
and it composes with Reframe 1 rather than competing with it: CI produces the
artifacts, the web is where they land. A browser-runnable *editor* remains the
correct closure (CAP-19) precisely because it serves the segment the substrate
framing does not target.

## Ground truth checked against the tree

- **Footnote 2 is half stale in the other direction.** `-board`/`-pins` are not
  unreachable: they are live entries in `JLSStart.FLAGS` (`JLSStart.java:782–786`),
  validated at `:1096–1105`, enforced as an `-export` pair at `:908–916`, and
  printed by `-h`. What is genuinely unsurfaced is `jls.hdl.imp.NetlistImporter`
  — there is no `-import` flag. The board flow's real defect is that README's CLI
  list never mentions it. A footnote produced by the survey's own adversarial
  verification pass is wrong against HEAD; that is the cost of evidence living on
  a branch (`docs/reviews/evidence/2026-08-niche-survey/` does not exist on main).
- **The elegance claim is drifting in the docs, not just the code.**
  ARCHITECTURE.md:48 says `SimpleEditor` is "~4k lines"; it is 5,852
  (`src/jls/edit/SimpleEditor.java`). The survey's "loses the code-inspection duel
  at first contact" point is stronger than it states — the architecture map a new
  contributor reads understates the god class by 45%.
- **"No discoverable examples" confirmed exactly.** Four `.jls` files exist in the
  tree: three test fixtures under `test/fixtures/` and `riscv/gui/cpu.jls`. There
  is no `examples/circuits/`; `examples/` holds one autograder script.

## What I am disregarding, and what I am not

I am disregarding §6's reconciliation ordering — specifically the promotion of
the shop window to "item-0-adjacent" *within* the capstone queue, and the framing
of importers as migration levers ranked by conversion value. The first should
leave the queue entirely; the second should be re-ranked as the primary product
surface with a headless-only fidelity bar.

I am not disregarding the teardowns. The per-competitor evidence is the best
strategic artifact this project has produced: Digital's decline is real and
correctly read, Falstad's analog core is correctly declared un-contestable,
DigitalJS is correctly reclassified as a complement, and the refusal list
(mobile, hosted LMS, analog re-compete, i18n) is disciplined and holding. Those
findings survive every reframing above; only the conclusion drawn from them
changes. The one operational request I would add: commit the teardown evidence
into `docs/` on main. A competitive analysis whose sources live on a feature
branch, in a repository that has already discovered one unresolvable
`evidence_commit` (per the third comment), is an asset the project can lose by
accident — and it is also the raw material CAP-36 #520's head-to-head write-ups
will need.
