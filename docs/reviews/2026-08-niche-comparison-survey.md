# Niche Comparison Survey — August 2026

**Charge (maintainer-directed):** JLS should become attractive enough to draw
each competitor's user and developer base through (1) easy on-ramp,
(2) technical superiority, (3) elegance of implementation. This survey
assesses exactly that, head-to-head, per competitor.

**Method.** Seven teardowns on an identical 12-dimension rubric (scores 1–5,
each justified from sources): Logisim-Evolution, Digital (hneemann),
CircuitVerse, Falstad CircuitJS1, Issie + DigitalJS, a small-competitor group
(DEEDS, Logic.ly, Gate Lab, DigiSim, simulator.io, LogicCircuit), and JLS
itself scored with identical severity. Each teardown mined the target's own
issue tracker/forums for its users' recurring complaints — the pull levers —
and profiled its developer community. The JLS baseline row was then
adversarially verified against the repository (11 of 12 scores confirmed;
corrections applied below). Companion to the August 2026 product review
(`2026-08-product-direction-review.md`, issue #508).

---

## 1. The score matrix

| Dimension | Logisim-Evo | Digital | CircuitVerse | Falstad | Issie/DigJS¹ | Small grp¹ | **JLS** |
|---|---|---|---|---|---|---|---|
| On-ramp: install | 4 | 2.5 | 5 | 5 | 5 | 5 | **4** |
| On-ramp: learning | 4 | 4.5 | 4 | 5 | 4 | 5 | **2** |
| Sim semantics | 3 | 4.5 | 2 | 2 | 4 | 3 | **4** |
| Scale/perf | 3 | 5 | 2 | 2 | 3 | 2 | **2** |
| Hierarchy/reuse | 4 | 5 | 3 | 2 | 5 | 3 | **2** |
| Testing/grading | 3 | 4.5 | 2 | 1 | 3 | 2 | **5** |
| HDL interop | 4 | 5 | 2 | 1 | 5 | 3 | **3²** |
| Extensibility | 3 | 4 | 2 | 3 | 4 | 2 | **2** |
| Code elegance | 2 | 4 | 2 | 2 | 4 | 2 | **4** |
| Contributor experience | 4 | 2 | 3 | 3 | 3 | 1 | **3** |
| Community size | 5 | 4 | 5 | 5 | 3 | 3 | **1** |
| Momentum | 5 | 2 | 4 | 4 | 4 | 2 | **4** |

¹ Grouped rows score the best-in-group (worst-case threat) per dimension.
² Shipped state. Adversarial verification found the "no HDL import" claim
stale: a Yosys-JSON netlist importer (`jls.hdl.imp.NetlistImporter`, #61) and
a CLI board/PCF flow (`-board`/`-pins`) exist at head of main with test
suites — but are wired to no CLI flag or menu, hence unreachable by users.
3–4 at head-of-main; releasing and surfacing them is cheap score.

Other verification corrections carried into this survey: the bsiever-fork
"closes without save prompt" complaint is already fixed in this fork
(`Editor.shutdown()` prompts); the mutation gate is blocking-thresholds **on a
weekly cadence**, not per-PR; commit cadence is "near-daily high-volume," not
daily; a benchmark harness exists (`riscv/bench_kernel.py`) — the perf gap is
*publishing*, not measuring; the first-run experience is *worse* than the
baseline claimed (an empty `JTabbedPane`, not even a blank canvas, with no
welcome, no tutorial nudge, no starter circuit); and **no example circuits
ship where a user can find them** (`examples/` holds only the autograde
script; all .jls files are test fixtures or the unsurfaced RV32I showcase).

## 2. The three levers, honestly scored

**Easy on-ramp — JLS's worst axis, and it is self-inflicted.** Install is
best-in-desktop-class (signed no-JDK installers on every OS beat Digital's
bring-your-own-JRE zip and Issie's Windows/macOS-only zips) but cedes the
ceiling to every browser tool. Learning on-ramp (2/5) is near the category
floor: zero README screenshots, an empty first launch, four static tutorial
pages behind a menu, HTML 3.2 help, no discoverable examples. Every teardown
independently produced the same bounce list: a switcher from *any* competitor
leaves in the first ten minutes without ever discovering the parts of JLS
that are genuinely superior. **The on-ramp gap is days-to-weeks of work and
gates everything else in this survey.**

**Technical superiority — real, but narrow and invisible.** JLS is
category-best on exactly one axis: testing/grading (5/5 — the documented `-t`
stability contract, VCD profile, deterministic exit codes, headless
multi-arch container; no competitor documents grading semantics at all). It
is top-tier on sim-semantics *rigor* (a normative, golden-test-pinned
semantics spec nobody else has; delay-accurate where CircuitVerse's delay is
a queue priority and Falstad's digital behavior is emergent analog) — but the
2-state+HiZ value domain caps it below Digital (per-bit HiZ) and DigitalJS
(per-bit 3-valued x-propagation). JLS *loses* today on: hierarchy
(no parameterization vs Digital's generics and Issie v6's parameter/library
catalog), scale/perf receipts (Digital publishes 120 kHz; JLS publishes
nothing), waveforms (no chronogram — even CircuitVerse has one), FPGA flow
(Logisim-Evolution's board download; Digital's BASYS3/TinyFPGA), and circuit
analysis (Digital's Quine-McCluskey/truth-table synthesis, FSM editors in
Digital/DEEDS/Issie).

**Elegance of implementation — JLS's strongest under-leveraged card.** At 4/5
JLS ties Digital and beats everyone else: JDK 25, sealed dispatch, JSpecify/
NullAway, mutation-tested, reproducible builds, normative architecture docs —
against Logisim-Evolution's 217k-LoC legacy `com.cburch` codebase with ~4%
test ratio and 2,000-line god files, CircuitVerse's four-year half-finished
jQuery→Vue rewrite, Falstad's zero-test GWT. Two caveats keep it from
converting: the 5,852-line `SimpleEditor` is worse than anything in Digital's
codebase (max 2,957, zero TODOs) and loses the code-inspection duel at first
contact — and Falstad *just executed* exactly this refactor (CirSim now 687
lines). Elegance draws developers only when paired with contributor
experience, where JLS is unproven (zero external PR throughput; the 255-issue
spec-prose tracker reads as an internal monologue; no good-first-issue
funnel).

## 3. Per-competitor verdicts

| Competitor | Their users' loudest pains (from their own trackers) | Winnable segment | Minimum bar |
|---|---|---|---|
| **Logisim-Evolution** (7.4k★, org-run, accelerating) | GUI-redraw-throttled simulation (#786); "haphazard" CLI verification, vectors can't test sequential circuits (#1546/#598); FPGA export emits invalid VHDL (#1871); 4.0 file-format breakage; false oscillation halts (#2454); stale Burch-era docs | Autograding instructors (their tracker admits the weakness; third-party Gradescope glue proves demand). Head-on user pull otherwise unrealistic — incumbent with momentum | CAP-16 .circ importer + grading kit + the white-paper comparison of grading contracts |
| **Digital** (5.9k★, best-in-class tech, **declining**: 3 commits 2026 YTD, no release in 23 mo, 97.7% one man) | Un-googleable name (#151, open 8 yrs); no dark mode (#1477); can't dive into subcircuit live (#84, open 9 yrs); UI clunk catalog (#882); bring-your-own-JRE; **contributor PRs rejected wholesale** (#1464 new-UI, #1470 closed unmerged; CONTRIBUTING discourages PRs) | **Contributors: winnable NOW** — the rejected-PR pool is named and reachable. Instructors: winnable on 1–2 yr horizon as "the maintained successor" if the stall becomes visible | Devs: public extension API, demonstrated <1-week PR turnaround, kill the god class. Users: .dig importer (plain XStream XML) + parameterization + FSM/synthesis parity + published benchmark within ~2× of 120 kHz |
| **CircuitVerse** (1.2k★/2.1k forks, 1.1M circuits, GSoC-driven) | Crash-and-lose-work (since #34; GSoC'23 shipped crash recovery); delay is queue-priority, timing untrustworthy (#1412); Verilog round-trip produces wrong logic (#5328); subcircuit nodes silently disconnect (#349); manual-only grading | Instructors who grade at scale (their LTI passes back *hand-entered* grades); timing/HDL courses burned by engine bugs; offline/locked-down labs. Browser-first students **not winnable without a web story** | .cv JSON importer (format is plain JSON) + chronogram + browser-runnable demo + head-to-head correctness/grading write-up |
| **Falstad** (~5.2k★ combined + uncountable web embeds) | Subcircuits silently fail (#400/#134); lag on non-trivial circuits; "Convergence failed" shown to beginners; ring counters unpredictable (#364); no offline first-party build | The digital-coursework overflow — courses that outgrow an analog solver the moment they need deterministic timing, working hierarchy, grading, HDL. **The analog/intuition core is not winnable and should not be contested** | Falstad-text importer for the logic subset (~2 wks); chronogram (its users are trained on live scopes); a "from Falstad" migration page |
| **Issie** (Imperial-captive) **/ DigitalJS** (maintenance-mode library) | Issie: crashes on ordinary edits (#588), no Linux binary, save-format breakage mid-course; DigitalJS: viewer-not-editor (#106), bitrot (#96/#113), multi-year issue latency | Issie: non-Imperial evaluators who hit the Windows/macOS wall; anyone needing autograding or real timing. DigitalJS: **complement, not rival** — its BSD-2 yosys2digitaljs mapping is directly reusable to de-risk JLS's own Yosys import (#61/FEAT-020) | Steal the ideas: Issie's width-inference error messages and v6 parameter/library model; DigitalJS's x-propagation. Optional Issie-JSON importer |
| **DEEDS / Logic.ly / hosted entrants / LogicCircuit** | DEEDS: 32-bit-Windows-only, closed, single academic — sunset clock ticking; hosted tools: data-hostage risk (simulator.io frozen), paywalls; LogicCircuit: Windows-only WPF | **DEEDS instructors at forced-migration moments** — highest value, and time is on JLS's side; hosted-tool teachers burned by freezes (sell *permanence*: local files, GPL, runs in 20 years) | Textbook-mapped lab pack (Donzellini chapters — no importer is possible, port the *course*); DigiSim-style assignment starter/submit workflow atop existing batch grading; chronogram + FSM tutorial content |

## 4. The universal gates — what every teardown said independently

Seven independent analyses each produced a minimum bar, and four items appear
in essentially every list. In cost order:

1. **Shop window (days).** README screenshots + animated GIF, a circuit
   gallery with SVG renders, shipped example circuits discoverable from the
   GUI, a comparison table. JLS currently loses every evaluation before
   first launch — social proof fails at 3 stars and zero images.
2. **First-run experience (small).** Kill the empty-`JTabbedPane` launch:
   open a starter circuit or the tutorial; add an examples menu. (Old #73's
   residual, TASK-0030 #381 — this survey upgrades it from polish to
   *gate*.)
3. **Chronogram panel (CAP-23 slice, 3–4 mw).** Named as a blocking deficit
   in five of seven teardowns. JLS's flagship claim is timing rigor, and it
   is the only tool in the survey whose superior timing engine has *no
   waveform UI* — even CircuitVerse ships one. This is the single highest
   leverage technical item, and it was already priority 5 in #508's queue;
   the survey promotes it to co-equal with the grading wedge.
4. **Publish the benchmark (≤1 mw).** The harness exists
   (`riscv/bench_kernel.py`); Digital's 120 kHz claim wins arguments by
   default because JLS publishes nothing. Measure, publish methodology,
   state the number — or concede the perf conversation indefinitely.

Then the migration levers, in value order: **.dig importer** (Digital's
format is plain XStream XML; the successor play depends on it), **.circ**
(CAP-16, already planned), **.cv JSON** (CircuitVerse, plain JSON),
**Falstad text** (logic subset, ~2 weeks). Recommendation: CAP-16's scope
should become an *importer family* program with shared loss-naming report
infrastructure — the per-format marginal cost after the first importer is
small, and each format unlocks a different user base.

**The web-presence question, stated honestly.** Every web-side teardown
(CircuitVerse, Falstad, hosted entrants, DigitalJS) names zero-install as the
unbridgeable moat, and the K-12/GCSE and browser-student segments are
permanently unreachable without *some* browser-runnable artifact. CAP-19
(single-file HTML export) was closed not-planned at maintainer direction on
2026-08-03 and this survey does not relitigate that scope — but it must
record: a *read-only browser demo* (e.g. CheerpJ-wrapped jar, or even a
hosted screenshot-plus-VCD viewer) is a much smaller ask than CAP-19's full
export capstone, would cut evaluation cost to competitor levels, and is the
one lane where every rival's advantage is structural rather than earned. If
adoption stalls after the universal gates land, this is the first closure
worth revisiting.

## 5. Drawing the developer base

The survey's clearest strategic finding: **there is almost no developer
community anywhere in this niche to draw — except one, and it is available
now.**

- Logisim-Evolution is the only healthy dev community (147 contributors,
  1–4-day PR merges) — not poachable, and not worth poaching.
- Falstad, DigitalJS, Issie, LogicCircuit, and the whole closed group are
  bus-factor-1 with structurally no retained contributors.
- **Digital has a named, reachable pool of demonstrably motivated, rejected
  contributors**: 26 open PRs, ambitious modernization PRs (#1464 new UI,
  #1470 keybindings) closed unmerged in late 2025, a CONTRIBUTING that
  discourages PRs, CI invisible to GitHub forks, and a 95% velocity collapse
  since 2021 with no successor plan. These developers want to work on a
  Java digital-logic simulator and have been told no.

The dev-draw play, concretely: (1) finish the SimpleEditor decomposition so
the elegance pitch survives repo inspection — Digital wins that duel today;
(2) stand up a good-first-issue funnel and a plain contribution template (the
current spec-prose tracker is optimized for the AI workflow and repels
humans; both audiences can be served with two templates); (3) implement
Digital's most-wanted rejected features as headline items — dark mode
(#1477 there; #289 here), live subcircuit dive (#84 there), keybinding
settings — then invite those PR authors by name; (4) demonstrate <1-week PR
turnaround on the first external PRs, and re-engage the two 2026 contributors
this repo already bounced (their fixes are merged-by-other-means; say so on
their PRs and invite them back); (5) publish the extension-point API (#223)
as a stability-stated plugin story Digital never offered.

**Positioning statement the survey supports:** *JLS is the maintained, modern
successor in the Digital tradition — delay-accurate simulation with a written
semantics contract, autograding as a documented interface, reproducible
signed builds — welcoming the contributors and courses that Digital's decline
is stranding.* The window is real but not permanent: Digital is one
motivated fork away from that role being claimed by someone else.

## 6. Reconciliation with the #508 queue

The survey confirms the #508 wedge sequence and adjusts emphasis:

- **Promote** the shop-window/first-run work (#381 + examples + screenshots)
  from unranked polish to item 0-adjacent — it gates every pull in this
  document and costs days.
- **Confirm** CAP-23's chronogram slice at high priority (five teardowns name
  it) and CAP-06/09/21 grading (JLS's only 5/5 axis; every rival's tracker
  documents the pain it answers).
- **Elevate** benchmark publication (≤1 mw) into the near-term set.
- **Expand** CAP-16 into the importer-family program (.circ, .dig, .cv,
  Falstad text) with shared loss-report infrastructure.
- **Add** the Digital-successor positioning and contributor-recruitment play
  (near-zero engineering cost, mostly writing and outreach).
- **Record** the browser-demo question as the standing revisit trigger on
  the CAP-19 closure.

Full per-competitor teardown evidence (facts, scores with justifications,
complaint lists with issue links, dev-community profiles, switch analyses) is
preserved in `docs/reviews/evidence/2026-08-niche-survey/`.
