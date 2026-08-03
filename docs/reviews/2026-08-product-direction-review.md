# JLS Product & Direction Review — August 2026

**Scope.** A commissioned, no-sacred-cows product-owner review of JLS and the
development direction encoded in the `tier:capstone` issues, assessing
project/product fit, long-term health, and the viability of public prominence.
Method: deep-reads of all 26 open capstones (plus the three culled the same
evening), competitor and neighbor research across ~25 tools and ecosystems,
measurement of JLS's actual public footprint, a codebase and delivery-capacity
audit, a governance/process review, and an adversarial two-lens synthesis.
Everything below argues by cost, path, and arithmetic per D10 (#485) — nothing
is refused for absence of demand; several things are *sequenced* by evidence of
demand, which D9's own caveat expressly legitimizes.

---

## 1. Executive verdict

**The fork is viable. The roadmap, treated as a commitment, is not. The
premise survives review; the portfolio does not.**

Three facts govern everything else:

1. **The product is real and unusually well-built.** 82k lines, 240 test
   files, mutation-tested, reproducible builds, signed installers on every OS,
   a documented batch-grading stability contract, headless-core ratchets. The
   engineering discipline is genuinely ahead of both incumbent competitors in
   its category. Nothing in this review found rot; it found over-planning.

2. **Adoption of this repository is zero, and the one live user is elsewhere.**
   anadon/JLS: 3 stars, 9 forks, 1 watcher, 0 externally-authored issues, 1
   merged external human PR since 2014. Meanwhile *JLS the lineage is alive*:
   Washington University in St. Louis runs CSE 260M on JLS **via the
   bsiever/JLS fork** (course site instructs students to download from
   bsiever's releases; an ACM Computing Frontiers 2025 paper documents the
   effort). Two external humans brought PRs to this repo in 2026
   (AmityWilder #4/#5, Dodothereal #187) and all three were closed unmerged.
   The only user community JLS has is currently routed around this repo.

3. **The delivery arithmetic does not close.** The tracker holds 26 open
   capstones, 83 features, 138 tasks. Sampled standalone capstone bands run
   29–52 mw (CAP-00), 67–101 mw (CAP-15), 159–253 mw (CAP-02). Bottom-up, the
   filed programme is **~600–1,700 maintainer-weeks (central ≈ 1,100 mw ≈ 22
   maintainer-years as priced)** against a bus factor of exactly 1 and a
   velocity baseline consisting of one four-week AI-assisted sprint that was
   already tapering in its final week. AI generation does not lift the bound,
   because the binding constraint is one human's review bandwidth and the
   permanent maintenance surface each shipped capstone adds (the current
   surface alone — 5 installer legs, 14 CI jobs/push, Wayland rig, repro
   guards — costs an estimated 5–13 mw/year to keep green).

The category itself is not a dead end: hneemann's Digital (5.9k stars) and
Logisim-Evolution (7.4k stars, org-run, term-aligned releases) prove a Java
desktop educational simulator can reach prominence — but both did it through
course adoption and a clear single-purpose identity, never through breadth.
The market is moving web-first (every entrant since ~2018 is browser-based)
and HDL-first (Verilator/cocotb/HDLBits/TinyTapeout are pulling ECE
curricula); a Java desktop tool wins in 2026 only by owning capabilities
nobody serves, and by meeting instructors where they already are.

**Decision: keep the project; invert the plan.** Stop treating the tracker as
a delivery commitment; treat it as a priced option book. Cap work-in-progress
at three capstones. Spend the next two quarters on the adoption wedges and one
prominence flare, all of which are cheap slices of existing capstones — and on
reconciling with the one institution actually using JLS.

---

## 2. What is actually differentiated (and what is not)

Verified against the landscape (Logisim-Evolution, Digital, CircuitVerse,
Falstad, DEEDS, Logic.ly, Wokwi, TinkerCAD, DigitalJS, Issie, Gate Lab,
DigiSim, nandgame/Turing Complete, HDLBits, TinyTapeout, IncluSim):

**Genuinely unserved lanes JLS could own**
- **Deterministic, artifact-grade autograding** — nobody ships an open,
  reproducible, first-party vector+VCD+verdict grading pipeline. Digital's
  embedded tests are closest; the third-party Logisim/Gradescope glue market
  proves the pain. JLS's `-t`/VCD/reproducibility stack is already the moat's
  foundation.
- **Accessibility** — no mainstream logic simulator is screen-reader usable;
  IncluSim (CHI 2025) is research proving the gap. ADA Title II WCAG 2.1 AA
  deadlines (April 2026/2027) turn this into a procurement requirement for US
  public institutions. JLS's shipped Okabe-Ito delta-E-ratcheted palette and
  keyboard operability already lead the category.
- **A drawn CPU that boots Linux** — no drawn-editor tool has ever booted an
  OS. This is the one story shaped like front-page news, and its behavioral
  demo slice is ~10 mw under D15's sidecar ruling.
- **The CS→ECE→EE span** (logic→board→chip in one tool) — real and unoccupied
  (D9 is right), but only monetizable through the wedges above; built
  breadth-first it is 600+ mw of unpurchased optionality.
- **Camera-ready figure export** (CircuiTikZ/WaveJSON from real runs) — empty
  category-wide; figures propagate into other people's course materials with
  attribution, which is free prominence.

**Already served better elsewhere — do not re-compete**
- The generic gate-level editor (Logisim-Evolution, Digital), web/zero-install
  delivery (CircuitVerse et al.), plain Verilog export (now table stakes),
  hosted collaboration + LMS platform (CircuitVerse shipped LTI/grade-sync in
  2021), mixed-signal teaching intuition (Falstad, free and dominant),
  gamified NAND→CPU ladders (nandgame, Turing Complete, Nand2Tetris at 400+
  institutions).

---

## 3. Recommended direction — the next two quarters

In priority order. Items 1–2 cost roughly nothing and address the review's
most damning finding; items 3–6 total **≈ 30–45 mw**, inside one person's
plausible half-year at measured AI-assisted rates.

1. **Reconcile with the live user base (≈0 mw, highest leverage).** Contact
   Bill Siever (WashU, bsiever/JLS) — the modernized base here (installers,
   batch grading, reproducibility) versus his fork's install base and course
   materials is a merger where both sides win. Re-engage the two bounced 2026
   PR authors. One adopting course that publishes is worth more prominence
   than any feature on the board. Also warm leads: the MTU/GVSU lineage
   (Poplawski/Kurmas courses are JLS's historical users).
2. **Stop the corpus bleed (≈1 mw).** Re-land the two fixes stranded on the
   condemned branch (RegisterFile/FieldExtend tag registration; creation-
   counter collision — tracked as #488/#491, D6). Commit the planning corpus
   (`docs/plan/**`) to master before the branch is deleted so the cost
   evidence every band cites stops dangling (#493). Decisions land on master
   in the same PR that cites them, from now on.
3. **Grading integrity + verdicts (CAP-06 slice + CAP-09 floor, ≈ 12–18 mw).**
   The shipped autograde example provably passes a wrong-on-255-of-256-vectors
   submission today; CAP-09's floor slice (combinational equivalence with
   replayable counterexamples) plus CAP-06's verdict/report slice fix the
   flagship capability's weakest measured claim and are the institutional
   wedge. Co-design the exit-status lattice once across both. Then CAP-21's
   Gradescope-first kit (2–3 mw slice) is the distribution channel.
4. **The prominence flare (CAP-02 behavioral slice, ≈ 10 mw).** Reference
   runner + guest stack + console on the D15 sidecar path: "a CPU drawn in a
   schematic editor boots Linux to a shell," shipped as a reproducible
   one-command artifact with a writeup. Do not start FEAT-026 or FEAT-038;
   the full parity programme waits for measured wall-clocks.
5. **Accessibility, CVD half (CAP-26 PF-1/2/6, ≈ 5–9 mw)** now — it rides
   shipped substrate and serves 8% of male students in every section. Spend a
   1 mw Orca feasibility spike before committing the blind-path 5–8 mw
   (KC-26-2); publish the VPAT posture either way.
6. **Debug-loop parity (CAP-23 slice, ≈ 3–4 mw).** Minimal chronogram +
   cross-probe closes the objective feature gap vs Logisim-Evolution and
   Digital; the cause-chain inspector (PF-3) afterward is genuine
   differentiation — but not before TASK-0063's queue seam exists.
7. **Distribution (≈1–2 mw, no capstone owns it).** Flathub, winget, Homebrew
   cask over the existing installers; a circuit gallery with SVG renders;
   release-asset download counts as the KPI instead of stars.

**Gate everything else on adoption evidence.** CAP-16 (Logisim import) is the
strongest *conversion* wedge on the board — run its own 2-day corpus
measurement now, but sequence the importer behind the loud-loader (#314) and
verdict (#369) spine so migrated circuits land in a tool that can prove they
behave identically. Success condition for the whole strategy: **external
courses running JLS labs within 18 months.** If that fails with these wedges
shipped, the bottleneck is provably channel, not capability — the answer would
be a SIGCSE/WCAE paper and direct instructor outreach, not more capstones.

---

## 4. Capstone dispositions

Dispositions applied or recommended. "Slice" figures are from each issue's own
bands. Marginal bands assume the shared spine is funded elsewhere — they are
not independent.

| Capstone | Issue | Disposition | Basis (cost/path) |
|---|---|---|---|
| CAP-00 maintenance ratchets | #296 | **Keep-core** | Fund the 10–18 mw defect-closure subset first; hold FEAT-008's 12–20 mw editor decomposition to its own 3.5-week checkpoint |
| CAP-06 grading verdicts | #300 | **Keep-core** | 4–7 mw demo slice on the shipped `-t` engine; drop the 25–36 mw handout-library half from near-term scope; re-adjudicate #369's blocked_by edges |
| CAP-09 formal verdicts | #306 | **Keep-core** | 8–11 mw floor (combinational equiv + counterexamples) repairs the false-pass hole; four-state tier waits |
| CAP-21 four-LMS kit | #502 | **Keep-core** | 2–3 mw Gradescope-first slice after CAP-06; adapters two-at-a-time |
| CAP-23 logic analyzer / cause-chain | #504 | **Keep-core** | 3–4 mw slice now (parity), PF-3 cause-chain after #476's queue seam; cut PF-6 viewer-sync |
| Grand architecture | #224 | **Keep-core, REPLAN** | Strike #168–#171 collab chain from requires (60–70% of remaining cost); remainder ≈ 8–12 mw |
| CAP-02 Linux boot | #301 | **Keep-strategic** | ~10 mw behavioral slice only; structural tier decided by measurement (KC-02-2/5) |
| CAP-16 Logisim import | #311 | **Keep-strategic** | Free 2-day corpus measurement now; importer (6–18 mw marginal) behind #314/#369 |
| CAP-26 accessibility | #507 | **Keep-strategic** | CVD half (5–9 mw) now; 1 mw Orca spike gates the blind-path 9–14 mw |
| CAP-24 figure export | #505 | **Keep-strategic** | 2–3 mw slice retires the hard risk; cut PF-4 animation; ≈ 9–14 mw realistic |
| CAP-05 KiCad netlist/board | #298 | **Keep-strategic** | 5–9 mw demo slice; owns the package layer CAP-04/13 consume; 24 mw stop-loss stands |
| CAP-07 chip shuttle | #302 | **Keep-strategic** | 1.5–2 mw wrapper slice to a real TT submission before funding FEAT-044 |
| CAP-15 HDL toolchain oracle | #310 | **Keep-strategic** | Oracle-on-flat + hierarchy (≈ 8–13 mw); REPLAN so FEAT-026 (28–36 mw) must justify itself |
| CAP-10 audio out | #308 | **Keep-strategic** | 3–4.5 mw solver-free rung settles the host-door design cheaply; analog rung stays behind CAP-12's gates |
| CAP-01 P2P multi-view collab | #299 | **Merged → #163** | Closed as duplicate; its own body pre-authorizes the collapse; AC-2 becomes a stretch criterion on #163 |
| CAP-11 audio in | #303 | **Merged → #308** | Closed as duplicate; marginal content ≈ 2–3 mw + one door review; demo slice survives as #308's read rung |
| CAP-13 KiCad round-trip | #307 | **Merged → #298** | Closed as duplicate; 7/8 features already #298's; isomorphism check becomes #298's verification tier |
| CAP-03 balanced-ternary CPU | #295 | **Closed (not planned)** | Cheapest honest route is 35–55 mw *after* CAP-02's 159–253 mw spine, for the plan's own priority-17-of-18 outcome; ternary pedagogy re-enters as a ~1–2 mw examples increment when FEAT-026 exists; T-null idea ports into FEAT-034's harness |
| #163 P2P collab tracking | #163 | **Defer** | Finish #167 op-funnel (independent consumers); solve the recorded pain (file merge) via FEAT-012 semantic merge; re-fund networking only after CAP-23/26 slices land, pilot logistics solved first |
| CAP-04 74-series breadboard | #297 | **Defer** | Tier-A headless slice (7–14 mw) later, after CAP-05's package layer; tiers B/C sit behind ~60–90 mw of substrate |
| CAP-08 import real RV32 core | #304 | **Defer** | 3–5 mw mesh-synthesis slice under #61 someday; 113–175 mw standalone is the largest sum in its set |
| CAP-12 mixed-signal PPG | #305 | **Defer** | 61.5–94 mw against Falstad (free, dominant); if revisited, its own 3.5–5 mw calibration + week-8 determinism gate decide |
| CAP-14 ngspice parity | #309 | **Defer** | Validation for a solver that doesn't exist; ship the 0.25–0.5 mw `write_spice` recipe; REPLAN if FEAT-046 lands |
| CAP-17 cluster/grid | #312 | **Defer / split** | Campaign axis (FEAT-057, 6–8 mw) belongs to CAP-06/09; FEAT-005 (2–3 mw) fund immediately; capacity axis waits on K17-1's measurement |
| CAP-18 transmission lines | #313 | **Defer** | Fund #486's lint alone (3–6 mw) when FEAT-047 lands; reflections lesson is Falstad-covered; element rung last per its own permanence ordering |
| CAP-25 plagiarism detection | #506 | **Defer** | Hard edge into unshipped FEAT-012; null-model premise is acknowledged research (KC-25-1); false-accusation risk is the worst failure mode an unadopted grading tool can ship |
| CAP-19 HTML export | #500 | Closed (maintainer, 2026-08-03) | Recorded: forecloses the zero-install lane deliberately — the one closure worth revisiting if adoption stalls on distribution |
| CAP-20 NAND→RV32 campaign | #501 | Closed (maintainer, 2026-08-03) | Recorded: self-learners are the only segment no surviving capstone addresses |
| CAP-22 RISC-V arch-test | #503 | Closed (maintainer, 2026-08-03) | Recorded: coherent retreat from RISC-V surface per D5 |

Cross-cutting engine note: the measured engine stack (queue/dedup bookkeeping
= 47.7% of warm-loop time, O(n²) stimulus parse, ~2.26× semantics-preserving
speedup priced in #484; Digital runs ~6–120× faster on the same JVM) is the
one investment that pays into *every* tier and capstone. TASK-0063/0056 and
FEAT-005 deserve standing priority ahead of any new capability surface.

---

## 5. Process and governance findings

The epistemic discipline (observed-failure-before-filing, supersession checks,
command-backed claims, D10's cost rule) is excellent — keep it. The container
and ceremony are liabilities:

- **The tracker is optimized for a single AI reader and hostile to every other
  audience.** 30–60KB issue bodies, a ~700-line template rule system, LaTeX
  transform mandates, mirrored-comment protocols. No drive-by human can file a
  conforming issue; narrative titles defeat search. Within three weeks the
  process consumed three self-inflicted remediation cycles (dead evidence pins
  #493, read-path corruption #489, doc rescue #484/#494–#499) — roughly one
  cycle of repair per cycle of planning.
- **Custody inversion.** The durable store (git) was scheduled for deletion
  while the ephemeral store (issue bodies) became "the only surviving copy" of
  the decision record and measured ground truth. Fix: planning prose lives in
  `docs/` on master; issues become thin (scope, acceptance criteria, link).
- **Hygiene defects to clear:** verbatim FEAT≡TASK duplicate (#360/#420);
  near-verbatim pairs (#352/#467, #348/#433, #369/#466, #320/#448, #357/#447);
  triple coverage between the old tracking generation (#163/#224, #61–63,
  #82/#184/#185/#188, #232) and the new CAP/FEAT/TASK series; zero milestones
  in use despite 26 milestone-scale outcomes; ten untiered documentation
  issues that can never meaningfully close; `ISSUE-AMBIGUITIES-2026-07.md`
  unmarked as superseded.
- **Machine-generated arithmetic needs spot-audit before filing:** three of
  four sampled capstones had published-as-binding cost bands below their own
  row sums, later corrected in comments.
- **Planning ratchet:** no new tier:feature/tier:task issues until two
  capstones close. The tracker is currently growing faster than the product.

---

## 6. What was done as part of this review

- This report committed to `docs/reviews/`.
- Meta-issue filed summarizing the review and disposition table.
- Closed as duplicate, per the merge dispositions above: #299 (→ #163),
  #303 (→ #308), #307 (→ #298), each with the surviving criteria named.
- Closed as not planned: #295 (CAP-03), with the re-entry path recorded.
- All other dispositions are recommendations recorded here and in the
  meta-issue; deferred capstones remain open — deferral is free on the graph
  (nothing downstream blocks on them) and closure would destroy priced
  option value.

**The one-sentence summary:** JLS's problem is not that it dreams too big —
the span thesis is sound and several dreams are genuinely unclaimed — it is
that 22 maintainer-years of plans are racing one maintainer's attention while
the only classroom actually running JLS does so from someone else's fork;
ship the four cheap wedges, reconcile with that classroom, and let adoption
evidence promote everything else off the backlog.
