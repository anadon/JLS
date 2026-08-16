**Capstone:** CAP-24 (#505) — camera-ready figure export (print SVG, CircuiTikZ, timing figure + WaveJSON, filmstrip, handout bundle)
**Verdict: ready-with-gaps** — start with #540/#723 and #536's demo slice now; six named gaps below should be closed in flight, none of them blocks the first fundable step.

## 1. Decomposition

**Feature level: complete and correctly wired.** All six `requires_features` (#536, #537, #538, #539, #540, #541) are filed, open, `serves_capstones: [505]`, and native sub-issues of #505. Each carries a filed task roster (#707/#709/#711, #712/#714, #716/#718, #720/#722, #723/#725, #874/#875 — all open). The §2 sufficiency table maps each §1 observation to an owner, and the post-review redirects (#539 filmstrip redefinition, #540 ElementRenderers-totality redirect, #541 `-t`-derived-run model) are consistently propagated across the feature bodies and most tasks. #874/#875 have been re-scoped to the current #541 design as #541's cross-issue note records (the stale "reported, not applied" line in #875's disposition predates #874's rewrite and is now moot).

**Gap D1 — #538's primary artifact has no owning task.** CAP AC-3 (`TimingSceneGoldenTest`: timing trace rendered through JLS's own sink) is #538 AC-1's core, but neither task under #538 owns it: #716 (TASK-C538-1) owns `RunTrace` + the **WaveJSON** writer/golden only, #718 (TASK-C538-2) owns the batch `-wavejson` flag. The native timing-**SVG** scene (`Trace`/`TraceGeometry` pointed at the sink) — the artifact #538 explicitly promotes over WaveJSON — is task-orphaned.

**Gap D2 — #538 AC-6 is self-acknowledged unowned.** #538's own boundary note: the `scripts/vcd-to-wavejson` CLI "remains to be filed; until it is, AC-6 is unowned by any task under this feature." CAP §1 step 3 requires exactly this CLI. TASK-C538-3 needs filing before #538 can close.

**Gap D3 — #536's core work is deferred out of its only general task with no follow-on filed.** #536 (4–6 mw, the root of the whole DAG) has three tasks: #707 was redirected to a grid-quantization *experiment only* (0.5–1 mw), explicitly deferring the sink's real substance — `JlsFont`/bundled-TTF + pure-Java `hmtx`/`cmap` metrics ("exceeds this task's original band by itself") and the `RenderProfile` threading over ~126 call sites ("also exceeds this task's band") — "rather than silently absorbed," but to no filed task. #709 owns only the print profile on the SVG back end (1–1.5 mw). #711 is band-0/suspended. Nothing at task level owns: the sink interface itself, the AWT back end, the self-emitted SVG serializer, the `-figure` flag (#536 AC-4), glyph coverage/diff locality (#536 AC-2), the KC-24-4 timing ratchet (#536 AC-5), or creating the hazard-demo fixture (#536 AC-1 — which #709, #716, #720, #722, #874, #875 all consume and all explicitly disclaim owning). Since #505's machine block funds features (requires_tasks_exception is empty) this does not block starting the demo slice, but the roster under #536 does not currently compose to #536's own ACs.

**Gap D4 — foreground-color-sweep ownership is contradicted three ways.** #505 §2/Cost says #540 *is* "finishing the #76 foreground-color sweep (~126 call sites)"; #536 AC-3 says the sweep is "delivered as part of #540"; #540 AC-6 says the opposite — it "consumes whatever foreground-color routing … already exists at pickup time; does not re-implement the … sweep, which is owned elsewhere (tracked under #76's decomposition, currently assigned to #289)." #289 is open, outside `requires_features`, and unfunded by this capstone. If #540's reading stands, monochrome print styling (#536 AC-3a) has an unowned prerequisite inside the capstone's promised outcome; if #505's stands, #540's band (0.5–1 mw) is wrong. One REPLAN must pick an owner.

No closed-but-still-required edges found; no double-owned artifact beyond a benign `HandoutBundleTest` overlap (#874 AC-1 produces-artifacts half vs #875 AC-1 file-presence check — they cross-reference each other, acceptable).

## 2. Acceptance-criteria composition

- **AC-1** → #541 AC-1, split #874 (artifacts) / #875 AC-4 (LaTeX build, shared TeX lane with #714). Composes.
- **AC-2** → #536 AC-2 + #537 AC-3 + #538 AC-4, extended to the filmstrip by #539 AC-3/#720 AC-2. Composes, and the children *strengthen* it (glyph-coverage fail-closed, diff locality, `Locale.ROOT` formatting). The accepted residual (anchored text renders at viewer-dependent widths) is consistently disclosed in #505 §3.1 and #536.
- **AC-3** → #538 AC-1 — feature-level covered, task-level orphaned (Gap D1).
- **AC-4 — stale against #540's redirect (Gap A1).** #505 AC-4 names `PrintSymbolTotalityTest`, a "missing-print-symbol warning" channel, and a scratch-element falsification transcript ("AC-4's falsification transcript recorded" is also a Completion Criterion). #540/#723 replaced all three: the mechanism is now `ElementRendererTotalityTest` over `ElementRegistry` tags, "there is no separate 'print symbol'", and #723 AC-3 explicitly substitutes the standing `FieldExtend`/`RegisterFile` red case for a transcript ("no separate committed transcript is needed"). Every child could pass while #505's AC-4/completion checklist, read literally, fails. #709 AC-4 also still cites "#723's `PrintSymbolTotalityTest`", a test name #723 no longer defines. The substance (a registry-keyed totality ratchet + `ElementDrawSmokeTest` print parameterization, #540 AC-7) survives; the capstone text needs a REPLAN to re-name the mechanism.
- **AC-5** → #539 AC-1 / #720 AC-1, with the budget (linear scaling cap + 512 KiB) now concrete and asserted. Composes.

Gap-free otherwise: #541 AC-2/AC-5 (run arity, planted-mismatch provenance) close the "every child green, capstone fails" seam the original recorded-run design had.

## 3. Dependency chains

Feature DAG (#540 → #536 → {#537, #538, #539} → #541 → #505) is acyclic and consistent across the mermaid graph, each feature's `ordering_after`, and #536's `ordering_after: [FEAT-C24-5]`. Task-level ordering (#723 → #707 → #709 (also after #725); #712 → #714; #716 → #718; #874 → #875) is acyclic.

**Gap C1 — stale edge on the TikZ critical path: #712 `ordering_after: [TASK-C536-3]`.** TASK-C536-3 is #711 — suspended at band 0, its own disposition recommending closure as not-planned/superseded (PDF was removed by #505's OQ-3 resolution). The core CircuiTikZ task is therefore nominally blocked by a task that will never run. #874 already dropped its #711 edge for exactly this reason; #712's should point at #536's sink work (or the feature) instead. Relatedly, #711 remains an open native child of #536 with a title ("PDF comes out of the same deterministic renderer") that contradicts the capstone — harmless to start, but it will trip #505's "machine block, roster table and graph agree with reality at close" criterion unless closed or re-titled.

External prerequisites: the only one is the TeX Live CI lane, which KC-24-3 bounds (pinned, cached, dev/CI-only, Linux-only per #537 AC-2, with a named descope fallback) and #714/#875 fund in-tree — acceptable. WaveDrom/Node are correctly kept out of every required path. #369 (WaveJSON-as-expectation) and #405 are correctly reference-only, both open.

## 4. Staleness and gaps

- **Evidence commit resolves.** `8288226` (2026-08-02) is on `master` (ancestor of head `c5cee1b`, 2026-08-05). Spot-checked claims are accurate at that commit: `getFontMetrics` in exactly 31 `src/` files; `Theme.java` 162 lines; `Trace.java` 626 lines with `MAX_RETAINED_CHANGES = 100_000` (line 32); `SvgExportTest#exportingTwiceIsByteIdentical` and `ElementDrawSmokeTest#everyElementDrawsOnTheSvgExportPath` exist; `-i` FlagSpec text matches; `BatchSimulator` "only 0, 1 and z … 'x' never does" javadoc at ~line 523; `FieldExtend`/`RegisterFile` present in `Palette.java` with no renderer in `ElementRenderers`/`BuiltinElementRenderers` (confirming #723's standing-red premise); fixtures directory contains exactly the three circuits the issues list; zero `tikz|circuitikz` hits repo-wide and zero `wavedrom|wavejson` hits in `src/`/`test/`. This corpus's code citations are unusually trustworthy.
- **Gap S1 — cost bands do not sum to the capstone's claim.** #505 states "nearer 5–8 mw than the original 12–19," but the filed feature bands sum to 10–16 mw (4–6 + 2–3 + 2–3 + 0.5–1 + 0.5–1 + 1–2), and #707's disposition prices #536's deferred bundled-font/metrics follow-on as exceeding 2–2.5 mw *by itself* — while #536's own band comment claims "no separate text-metrics line." The 5–8 band is only reachable on the qualitative discounts ("well under", "a fraction of") the roster rows assert but the child headers do not carry. Not start-blocking (the 2–3 mw demo slice is priced and KC-24-1 gates further funding), but the standalone band should be reconciled at the first REPLAN.
- Open questions: both remaining OQs are ride-alongs; the three formerly blocking OQs (symbol standard, PDF, video) are resolved and consistently propagated (with the AC-4 naming exception above). Nothing blocks start.
- Kill criteria are live and well-placed: KC-24-1 gates all downstream funding on the demo slice, which is exactly the right first move.

## Verdict: ready-with-gaps

Start now: #723 (no blockers, fixes a shipping silent-render bug) and #536's demo slice (sink + AWT back end + anchored-text SVG + gate/wire/pin schematic scene), which retires KC-24-1 before anything else is funded. Close in flight, ideally in one REPLAN pass on #505:

1. File TASK-C538-3 (`scripts/vcd-to-wavejson`) and a timing-scene task under #538 (Gaps D1, D2).
2. File the #536 follow-on tasks #707 deferred (JlsFont/bundled metrics, RenderProfile threading, sink+back ends, fixture, `-figure`, KC-24-4 ratchet) or record that #536 carries them feature-direct (Gap D3).
3. Adjudicate foreground-sweep ownership: #540-in-scope vs #289-prerequisite; update #505 §2, #536 AC-3, #540 AC-6 to agree (Gap D4).
4. Re-point #712's `ordering_after` off suspended #711; close or re-title #711 (Gap C1).
5. REPLAN #505 AC-4/completion-checklist language to the `ElementRendererTotalityTest` mechanism; fix #709 AC-4's retired test name (Gap A1).
6. Reconcile the 5–8 mw standalone band with the 10–16 mw roster sum (Gap S1).
