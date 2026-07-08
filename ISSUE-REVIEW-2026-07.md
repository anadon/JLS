# Open-Issue Review — July 2026

Scope: all 52 open issues (#33–#63, #65–#86) reviewed for rigor, scientific/experimental
structure, falsifiability, testability, correctness against the codebase, and correctness
against current external literature/tooling; then cross-validated for mutual consistency;
then assessed for campaign readiness.

Method: every issue's file:line citations and behavioral claims were re-derived from the
working tree at `422f6ae` (master merged through PR #83). External claims (Yosys, ELK,
GHDL/Icarus, CodeQL, Scorecard, CycloneDX, JaCoCo, jpackage, Contributor Covenant,
JEP 263, IEEE 1364, Lööw OOPSLA'25) were verified against primary sources as of
2026-07-08. One numeric claim (#50) was reproduced empirically with a compiled test.

Result: **19 issues fully sound, 32 sound with correctable nits, 1 defective (#86)**.
On ~200 spot-checks, file:line citations were accurate to the line in all but a handful
of cases — an unusually high factual hit rate. The corpus's uniform 12-section scientific
template (hypothesis, predictions, falsification criteria, threats to validity) is doing
real work: in both cases where a *proposed fix* was wrong (#53, #45), the issue's own
test design would have caught the flawed fix before merge.

---

## 1. Verdict table

| # | Title (abbrev.) | Verdict | Action needed before implementation |
|---|---|---|---|
| 33 | Post-audit tracking issue | SOUND-WITH-NITS | Fix 4 ordering/link defects (§4) |
| 34 | Opened circuits never get a directory | SOUND | — |
| 35 | Wire hover repaints (degenerate getRect) | SOUND | Nit: rubber-band harm is under-repaint on unselect-outside path, not "defeated optimization"; add intersects/overlap regression check |
| 36 | Single-source version tracking | SOUND | — |
| 37 | Popup handlers dead (merge-conflict brace) | SOUND | Smoke test must not rely on accelerators (popup accelerators don't fire in Swing) |
| 38 | Harden untrusted circuit-file parsing | SOUND-WITH-NITS | Type-check wording (`Element`, not `LogicElement`); distinguish OOME propagation (zip path crashes, RLE path is caught by `catch(Error)`); coordinate 5-line overlap with #55 |
| 39 | Undo/redo leaves gesture state stale | SOUND-WITH-NITS | P4 presumes Esc-cancel semantics that don't exist for move/rubber-band — define Esc first (#75) or reword P4 |
| 40 | Remove MTU EULA gate | SOUND | Owns the 3 Eula fixes currently duplicated in #51 |
| 41 | Changed flag cleared before write | SOUND | — |
| 42 | Batch/CLI headless crashes, arg indexing | SOUND-WITH-NITS | "Corrupt exit-status" overstated (exit is still 1; harm is diagnostics); sites :231/:241 are `-i`, not `-b`; reporter must be TellUser-seeded per #81 |
| 43 | Editor hot paths (canConnect, repaints) | SOUND | Repaint-site list not exhaustive (also :2108, :2312, :2376) |
| 44 | Release pipeline hardening | SOUND-WITH-NITS | "Zero tags" now stale; manual re-push of a v* tag would fire the unhardened pipeline today |
| 45 | Checkpoint writer resurrects .jls~ | SOUND-WITH-NITS | "Recover from checkpoint" prompt doesn't exist (P3 untestable as written); primary fix variant insufficient — use generation-counter/fence |
| 46 | Plugin loading CWD/NPE/XML | SOUND | **Blocked by #80** (not stated in body); XXE test fixture markdown was eaten |
| 47 | Docs & CI hygiene | SOUND | Owns the Circuit.java javadoc conflict-markers item (also listed in #51) |
| 48 | Print mode never loads the circuit | SOUND-WITH-NITS | `-v` description wrong (top-level-only to *named* printer; no preview/default printer); `finishLoad`-failure path currently exits 0 |
| 49 | Interactive simulator threading | SOUND | Trace failure taxonomy: IOOBE/unsafe publication, not ConcurrentModificationException |
| 50 | BitSetUtils.Create(long) silent zeroes | SOUND-WITH-NITS | Empirically confirmed (failures at k=49,50,52,53,54,56,57,60,61). "Guaranteed at k≥53" overstated; k=63 fails via long overflow → NaN, not log rounding — sweep must include k=63 |
| 51 | Low-severity grab-bag | SOUND-WITH-NITS | Prune 3 duplicated ownerships: Eula→#40, Clock/TruthTable/Group→#52, javadoc markers→#47; Group throws IOOBE not NPE for negatives |
| 52 | Element parameter validation | SOUND-WITH-NITS | Owns Clock/TruthTable/Group; "all 70 tests" stale (71 @Test in 16 classes); Group taxonomy as in #51 |
| 53 | Backslash unescape order | SOUND-WITH-NITS | **Headline fix is wrong**: reverse-order replace is not a correct inverse either (counterexample: literal `\`+`n`). Only the single left-to-right scan variant is sound |
| 54 | SpotBugs baseline scoping | SOUND-WITH-NITS | Body markdown ate the `<Class>` tags in three places |
| 55 | getConstructors()[0] order dependence | SOUND | Coordinate 5-line overlap with #38 |
| 56 | Test-suite gaps | SOUND-WITH-NITS | Legacy-4.1 corpus sourcing is a single point of failure with no stated fallback; "70 tests/12 classes" stale; JaCoCo checkbox superseded by #66 (not marked in body) |
| 57 | Display legacy orient 0 corruption | SOUND | — |
| 58 | Load-error reporting unreliable | SOUND | Addendum doubles scope (design spec vs 5 small fixes) — a conscious decision for the implementer |
| 59 | HDL umbrella | SOUND-WITH-NITS | Non-synthesizable element policy (Display/SigGen/Pause/Stop/Text) never enumerated |
| 60 | HDL Stage 1: export | SOUND-WITH-NITS | "Entire element set" trivially false for sim-control elements; **JumpStart/JumpEnd net aliasing missing from the export walk** (correctness, not template) |
| 61 | HDL Stage 2: Yosys import | SOUND | Memory `accessTime` ≠ zero-delay async read: batch parity passes, waveform-timing parity won't (one line in mapping table) |
| 62 | Stage 2 companion: auto-layout | SOUND-WITH-NITS | ELK license premise is expiring: eclipse-elk/elk#1185 targets GPLv3 secondary license for 0.12.0 — add re-evaluation checkpoint |
| 63 | HDL Stage 3: co-simulation | SOUND-WITH-NITS | Combinational-path-through-black-box fixpoint iteration under-specified (the hardest correctness problem); no alternatives-considered vs VPI/cocotb |
| 65 | SBOM / CycloneDX | SOUND-WITH-NITS | Default output is `target/bom.json`, not `*.cdx.json` — P1 as written fails against a correct implementation; `actions/attest-sbom` deprecated → `actions/attest` |
| 66 | JaCoCo coverage ratchet | SOUND | Must land before Phase 4 closes (see #33 defect) |
| 67 | CodeQL + secret scanning | SOUND-WITH-NITS | `build-mode: none` (GA for Java since 2024) sidesteps the autobuild concern |
| 68 | Supply-chain posture / Scorecard | SOUND-WITH-NITS | Observations bullet wrong: release.yml *does* declare workflow-level `contents: write` (:9-10) — the defect is scoping, as its own checklist says |
| 69 | Community health files | SOUND-WITH-NITS | Contributor Covenant 3.0 (July 2025) is current, not 2.1 |
| 70 | Shipped help content broken | SOUND-WITH-NITS | "Six orphans": **5 of 6 refuted** (linked via inline hrefs); only `editor/circuits/overview.html` is a true orphan; the issue's own reachability test contradicts its inventory — define "orphan" or fix P1 |
| 71 | CLI self-documentation | SOUND-WITH-NITS | Unclosed paren is on the `-pprinter` line (:487), not the example; pixel-golden P4 is brittle across JDK font rendering |
| 72 | Batch mode as grading API | SOUND-WITH-NITS | P1 byte-exact golden likely **fails at HEAD**: watched-element order rides `HashSet` iteration (`Circuit.java:53`) — fix ordering first; trace accumulation is gated on `-r`; goldens provenance is #14, not #16 |
| 73 | First-run onboarding | SOUND-WITH-NITS | Tutorial dialog is already resizable (fixed *initial* size is the issue); baseline for the n=5 trial asserted, not measured; Edit menu belongs to #75 |
| 74 | Zoom and canvas growth | SOUND | — |
| 75 | Keyboard operability & accessibility | SOUND-WITH-NITS | **:483-484 is `matchJump`'s commented accelerator, not rotate/flip's** — "un-comment and finish" would bind R to wire-end matching, colliding with the issue's own rotate-on-R proposal |
| 76 | Visual ergonomics / HiDPI / L&F | SOUND-WITH-NITS | "Renders tiny on modern displays" is wrong for a JDK-17 app (JEP 263 auto-scaling since JDK 9) — P2 likely already passes at HEAD; the real HiDPI defects are blurry upscaled 32px GIFs and fractional-scale artifacts — reframe P2 |
| 77 | Extract headless jls.core | SOUND | Reference counts ~5-10% high (audit counted unqualified in-file uses) — immaterial |
| 78 | Element descriptor/registry | SOUND | — |
| 79 | Save-format stewardship | SOUND | Spec should state nested subcircuit CIRCUIT blocks don't get FORMAT headers; executors must `git fetch --tags` first |
| 80 | Plugin mechanism decision | SOUND | Start the call-for-users clock immediately (it needs a release cycle) |
| 81 | Notification/dialog consolidation | SOUND-WITH-NITS | **~160 is line-count inflation: 93 actual `JOptionPane.show*` call sites across exactly 20 files** (State 19, TruthTable 14, JLSStart 14, Editor 7, Memory 8, SimpleEditor 6); TellUser call sites are 4, not ~7 — rebuild the migration inventory before writing the ratchet baseline |
| 82 | jpackage distribution | SOUND-WITH-NITS | jpackage final in JDK 16 (JEP 392), not 17; no `java.util.prefs` usage exists — derive module list with `jdeps`; `java.xml` needed unless #80 lands first |
| 84 | Decompose SimpleEditor | SOUND | All counts reproduce exactly (4,119 lines; 9 states; 305-line dispatcher) |
| 85 | Normative documentation | SOUND-WITH-NITS | "21 missing element pages" over-counts: the 61-file denominator includes abstract/support classes; real user-facing gap ≈ a dozen — key the completeness test to registry-eligible elements |
| 86 | KeyPad right-click hijack; tab disable | **DEFECTIVE** | See §2 — central mechanism refuted; needs rewrite |

---

## 2. The one defective issue: #86

The KeyPad half's central claim — a "global right-click hook" that dismisses the keypad
"regardless of where the click lands," colliding with the editor's context-menu gesture —
is refuted by the code. The BUTTON3 `MouseAdapter` at `KeyPad.java:130-141` is attached
only to the keypad's own digit and reset buttons; there is no `Toolkit.addAWTEventListener`
anywhere in the codebase. A right-click on the editor canvas cannot reach that listener,
so the collision described in H1/P1 is impossible and P1 tests a scenario that already
passes. The misleading source comment at `KeyPad.java:129` ("make right click anywhere
close window") appears to have been taken at face value by ERGONOMICS-AUDIT finding U10
and amplified into a concrete-sounding failure mode.

The *actual* KeyPad defects are nearly the opposite: no click-outside dismissal at all,
no Esc handling, and right-click-on-a-digit as a hidden nonstandard dismiss gesture.
The remedy H1 proposes (standard popup dismissal + Esc) remains reasonable, but the
hypothesis, observation, and P1 must be rewritten around the real behavior.

Also overstated in the same issue: the subcircuit-tab disable is not "silent — no
explanation"; `SimpleEditor.java:3647` sets a persistent top-bar label ("editting is
disabled while a subcircuit is being modified"). Easy to miss, but present. The H2 half
(read-only mode instead of disable) survives with that correction.

This is the only place in 52 issues where "reproduce the reasoning from source" failed
at step one. Notably, every AUDIT-2026-07-derived issue verified cleanly while the one
refuted mechanism traces to ERGONOMICS-AUDIT-2026-07 — the two audits evidently had
different verification standards, so remaining unverified U-series claims deserve extra
scrutiny during implementation.

---

## 3. Corrections that prevent wrong implementations

These are the findings where following the issue verbatim produces a bad outcome
(details in the verdict table; per-issue comments have been posted):

1. **#53** — the "reverse the three replace() calls" fix is itself incorrect: sequential
   global replaces cannot invert a shared-escape-character scheme in any order
   (counterexample: literal `\`+`n` saved as `\\n` mis-decodes to `\`+LF under reverse
   order). Implement the issue's *alternative* mechanism: a single left-to-right scan
   over escape pairs. The issue's own randomized round-trip property test would catch
   the bad variant — run it first. The fix must also cover the duplicated decoder in
   the probe branch (`Circuit.java:585-589`).
2. **#45** — P3 references a "recover from checkpoint" launch prompt that does not exist
   (recovery is manual via the file chooser's `.jls~` filter). Reword P3 to an on-disk
   observable. And the first proposed fix (remove the coalescing-map entry before
   delete) fails its own all-interleavings bar — the writer removes the map entry
   *before* writing (`SimpleEditor.java:161/165`) — so implement the generation-counter/
   fence design directly.
3. **#75** — the commented-out accelerator at `SimpleEditor.java:483-484` belongs to
   `matchJump` ("Create Matching End"), not rotate/flip. "Un-comment and finish" would
   bind R to wire-end matching and collide with the issue's own rotate-on-R proposal.
4. **#81** — rebuild the migration inventory from `JOptionPane\.show` matches (93 sites,
   20 files) before writing the forbidden-import ratchet, or the baseline will be wrong.
5. **#70** — resolve the orphan-definition contradiction: 5 of the 6 "orphans" are
   reachable via inline links, so the proposed TOC-reachability test would pass them
   and falsify P1's inventory as written.
6. **#76** — drop the "renders tiny" premise (JEP 263 gives automatic HiDPI scaling on
   JDK 9+); re-aim the HiDPI half at bitmap-icon crispness and fractional-scale QA.
7. **#50** — extend the sweep to k=63: the `(1L<<63)-1`-adjacent failure is `value+1`
   long overflow → `Math.log(negative)` = NaN → `(int)NaN` = 0, a different mechanism
   than log rounding, and the currently specified k≤62 sweep would miss it.
8. **#65** — the CycloneDX plugin's default output is `target/bom.json`; either set
   `<outputName>` or fix the `target/*.cdx.json` glob and P1's expected filename.
9. **#72** — condition P1 on first fixing watched-element output ordering
   (`Circuit.elements` is a `HashSet`, so byte-exact goldens plausibly fail at HEAD);
   decouple trace accumulation from the `-r` printer flag before adding `-vcd`.
10. **#60** — add JumpStart/JumpEnd net-aliasing to the export walk (exports of any
    circuit using jumps are otherwise wrong) and declare the policy for
    Display/SigGen/Pause/Stop/Text (ignore/warn/reject).
11. **#48** — correct the `-v` flag description (top-level-only print to the *named*
    printer; there is no preview/default-printer variant) and note the `finishLoad`
    failure path currently exits 0.
12. **#39** — P4's "identical to Esc-then-undo" is unimplementable for move/rubber-band
    until Esc-cancel semantics exist for those gestures (only wire drawing has Esc
    today, `SimpleEditor.java:596-632`).

## 4. Cross-issue consistency

### 4.1 Conflicts requiring an edit (no true contradictions of substance)

- **#46 vs #80** — the only live prescription conflict: #46 hardens plugin code #80
  deletes. #33 records the gate ("#80 before #46") and #80 predicts "#46 closes as
  superseded," but **#46's own body never mentions #80**. An executor picking up #46
  cold would harden dead code. Add a blocked-by header to #46. Related: #33's Phase-8
  internal order ("#77 → #78 → #80") contradicts its own Phase-4 gate — #80's
  delete-decision does not need #77/#78; pull it into the Phase-4 timeframe and start
  its call-for-users clock now.
- **#51 vs #52** — Clock, TruthTable, and Group appear verbatim in both checklists with
  *different remedy shapes* (≤10-line spot fixes vs the two-entry-point validation
  contract). #52 owns all three; strike from #51.
- **#51 vs #40** — #51 repairs three Eula defects #40 deletes. #40 first; strike from #51.
- **#51 vs #47** — Circuit.java javadoc conflict-markers appear in both. #47 owns it.
- **#33 internal** — its falsification criteria require #66's coverage ratchet to be
  live "when Phase 4 closes," but the phase table puts #66 in Phase 7. Move the
  JaCoCo-wiring half of #66 into Phase 4.
- **#33 vs #75** — #33 schedules #75's cheap half before #76, while #75 §10 sequences
  it after the system-L&F flag (part of #76). Declare the menu-bar work L&F-independent
  or reorder; record the choice in both.

Verified *non*-conflicts: version numbering (#36/#44/#33/#48 all agree: next shipped
release is v4.3.0); #42 vs #81 (minimal contract vs app-wide migration, mutually cited);
#55 now vs #78 later (both state the order); #84 vs the SimpleEditor bug fixes
(sequencing declared on both sides).

### 4.2 Ownership assignments for jointly-claimed work

| Work item | Claimed by | Owner |
|---|---|---|
| Clock/TruthTable/Group validation | #51, #52 | **#52** |
| Eula fixes | #51, #40 | **#40** (by deletion) |
| Circuit.java javadoc markers | #51, #47 | **#47** |
| Shared `Action` layer / dispatcher retirement | #75, #84 | **#75** creates Actions; **#84** consumes |
| Edit menu | #73, #75 | **#75**; #73 depends |
| Viewport / device-scale transform | #74, #76 | **#74** owns Viewport; **#76** owns prefs/L&F |
| Open→save integration test | #34, #56 | **#34** writes it; #56 verifies presence |
| Batch-CLI smoke harness | #42, #56, #71, #72 | **#42** builds; others extend |
| JaCoCo wiring / argLine fix | #56, #66 | **#66** (mark #56's checkbox superseded) |
| Legacy `.jls` corpus | #56, #57 | **#56** corpus; #57 may ship a synthetic interim fixture |
| Headless-aware reporter | #42, #81 | **#42** minimal contract, TellUser-seeded; **#81** migration |
| Type-check-before-instantiation (5 lines in Circuit.java) | #38, #55 | First to land closes the other's checkbox |
| Save-tag alias table | #78, #79 | **#78** implements; **#79** documents |
| `-t` stimulus format spec | #60, #63, #72 | **#72** spec lands before/with #60's testbench generator |

### 4.3 Cross-reference defects to fix

- Wrong "#NN-class" placeholders (drafted before numbers were final): #77 §7 "#77-class
  dialog helper" → **#81**; #78 §1 "#79-class plugin question" → **#80**; #78 §7
  "#80-class save-format issue" → **#79**; #71 §11 "#71-class grading-API work" → **#72**.
- Missing back-references: #46→#80 (critical), #56→#66, #61→#78.
- **22 issues carry dead evidence links**: 17 cite
  `blob/claude/version-tags-issue-framing-cf7jzu/ERGONOMICS-AUDIT-2026-07.md` and 5 cite
  `blob/claude/project-code-audit-1gh4s8/docs/hdl-support-research.md`. Both branches
  are deleted; both documents are on master. Rewrite all such links to `blob/master/...`.
- No references to nonexistent issues (nothing cites #64 or #83, which are PR numbers).

### 4.4 Dependency ordering (no cycles found)

- **Wave 0 (anytime, parallel-safe):** #47, #54, #69, #67; #80's call-for-users notice;
  #77's architecture-ratchet test.
- **Wave 1 (data loss/corruption, 4.3-blocking):** #34+#41 together, then #45; #53,
  #57, #50, #55, #58.
- **Wave 2 (restore features + contracts):** #37, #42, #48 (after #42); #52 (after #58);
  #38; #51 (pruned); #35, #39, #49.
- **Wave 3 (quality floor):** #56 corpus + edge-trigger goldens; #66 (must close within
  Phase 4); #46 resolution per #80's outcome.
- **Wave 4 (ship v4.3.0):** #36, #40 → #44 (+#79's FORMAT-header slice riding the tag)
  → #65, #82, #68; #70, #71, #72 land in this window so release docs tell the truth.
- **Wave 5 (post-ship UX):** #73 (after #34, #70); #76 → #74 → #75 → #84 → #86 (after
  rewrite, after #39); #81 and #85 interleave; #43 (after #35 and #49).
- **Wave 6 (architecture & HDL):** #77 (seams #49/#58/#36/#76 landed) → #78 → #79 full
  spec; #59 gate → #60 → #61 + #62 → #63 (hard dependency: after #49).

Execution collision warnings: #43, #74, #75, #84 all edit the same
`SimpleEditor.java:1295-3200` region — serialize them or expect heavy merge conflicts.
Similarly #44, #47, #65, #66, #67, #68 all edit `ci.yml`/`release.yml`; no issue orders
those edits among themselves — designate #68 as the integrating owner of workflow
permissions and land #47's trigger/concurrency change first.

## 5. Campaign readiness

Ready in substance: every issue carries pinned file:line evidence, falsifiable
acceptance criteria, and a test-first method; an executor can pick up nearly any issue
cold. Remaining gaps, in priority order:

1. **Fix the dead evidence links** (§4.3) — 22 issues cite deleted branches; the
   documents are already on master, so this is pure link rewriting.
2. **Post the per-issue corrections** (§3) so no executor implements the wrong fix
   (done — see issue comments), and rewrite #86.
3. **Update #33**: move #66 into Phase 4; decouple #80 from Phase 8 and start its
   clock; make the #79 Phase-5/Phase-8 split explicit in the table; resolve the
   #75/#76 ordering; refresh the dead ERGONOMICS link; add a `tracking` label.
4. **Prune the duplicated checklists** (#51 → #40/#47/#52; #56 → #66) and add the
   missing blocked-by note to #46.
5. **Add machine-readable prioritization**: severity labels, a v4.3.0 milestone
   containing Waves 0–4 (+#79's header slice), and phase labels. Currently the
   taxonomy is a flat bug/enhancement/documentation set. Label nits: #55's hazard is
   latent at HEAD (annotate so it isn't triaged as an active High); #33 needs
   `tracking`.
6. **Record calendar gates**: #80 and #48's deprecation windows are release-cycle
   clocks — record start/end dates in the issues when started.
7. **Plan a line-number refresh** for #74/#75/#84/#86 after Waves 1–2 land (five
   issues edit SimpleEditor.java; the shared `9afcce0` pin will drift).
8. **Stale figures to correct opportunistically**: "70 tests/12 classes" (#52, #56) —
   now 71 `@Test` in 16 classes; #44's "zero tags"; #81's counts; #85's denominator.

With items 1–4 done, the campaign is executable in the wave order of §4.4.
