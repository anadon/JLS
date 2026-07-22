# Open-Issue Ambiguities & Decisions — July 2026

Scope: every one of the **20 open issues** (#33, #59, #61–#63, #73, #75–#78, #82, #84, #86,
#91, #93–#96, #101, #134), read together with their comments, the working
tree at `2c0ee59`, and the prior audit passes.

Purpose: surface **every genuine decision or ambiguity** a maintainer must resolve before work can
proceed, **fill in the ones answerable from the code/docs/best-practice**, and flag the rest with
concrete options and a recommended default — in a form ready to paste back into each issue.

Legend for each decision point:
- **RESOLVED** — settled on 2026-07-17 by a maintainer directive, verified external state, or applied
  industry best practice. No further input needed; recorded here so the issue can be updated.
- **FILLED** — answerable now; the answer + its one-line basis is given. Just needs your ✔/✗.
- **NEEDS-MAINTAINER** — a real choice only you can make (cost, custody, policy, taste, or external
  state). Concrete options + a recommended default are given.

---

## Update — 2026-07-17: directives applied, ambiguity surface reduced

Three maintainer directives were applied and the external unknowns were actually checked, collapsing
most of the open surface:

- **"check-at-pickup = just do it."** → the two external unknowns were verified live (2026-07-17):
  - **#93 is NOT blocked.** Error Prone 2.50.0 runs on JDK 21+ (supports 25); NullAway 0.13.7 needs
    only EP 2.36.0+/JDK 17+, handles JDK 27-EA, and its JSpecify mode *recommends* Java 25. **Wire
    EP 2.50.0 + NullAway 0.13.7 directly** (with the `--add-exports`/`-J` compiler flags EP needs on a
    modern JDK). No annotation-only interim required; #93 no longer gates #94/#95.
  - **#62's ELK GPLv3 relicense is merged** (issue #1185 *closed* via PR #1184) but **0.12.0 has not
    shipped** (latest is v0.11.0, EPL-2.0). Resolution: **skip the hand-rolled Sugiyama layouter and
    use the out-of-process ELK runner now** — a separate process exchanging JSON is license-clean even
    under EPL-2.0, so nothing waits on the relicense. Switch to in-process linking later if 0.12.0
    ships (a simplification, not a blocker).
- **"Design taste → apply settled industry practice."** → every design-taste decision below is now
  **RESOLVED** to the industry-standard answer (focus model, zoom, Swing theming, event equality,
  co-sim transport, Windows fixes, UI-test framework, HDL export shape). Two of these mildly revise an
  earlier owner adjudication (FlatLaf-primary over system-L&F-default in #76; `SimEvent` stays a class
  rather than becoming a record in #94) — both are flagged so you can veto, but the recommendation is
  the mainstream practice.

**What genuinely still needs you** shrank to the §9 short-list: enroll the project in SignPath (free,
Windows #134); and the pure project-management triage on #33 (close/reparent/re-scope). Everything
else is settled below.

---

## 0. Read this first — three findings that reframe the whole backlog

1. **Much of the backlog has already shipped; several issues are bookkeeping-stale, not blocked.**
   v5.0.0–v5.0.4 are tagged and released (CHANGELOG at `5.0.5-SNAPSHOT`). Landed since the issue
   bodies were written: the FORMAT header + spec + `FileFormatSpecTest` (#79 core), the `TellUser`
   reporter + `NotificationRatchetTest` (#81), the headless-core ratchet (#77), the KeyPad half of
   #86, HDL Stage-1 export (#59), the Wayland code idioms (#100 sub-issues #102–#105), the #56 edge
   goldens / JaCoCo ratchet / CLI smoke tests, and the Layer-1 UI harness (#91). **Before deciding
   anything, re-scope or close the stale issues** — details under #33, #86.

2. **A few decisions, made once, resolve many issues.** See §1 (cross-cutting) — settling the
   modernization sequencing, the save-format tag-freeze rule, the Memory-timing/xz-coercion pair,
   and the single-maintainer custody stance collapses ~10 downstream questions.

3. ~~The only genuine *external* blockers are two...~~ **Both were checked on 2026-07-17 and neither
   blocks (see the Update banner above):** Error Prone 2.50.0 + NullAway 0.13.7 support JDK 25 today
   (#93 unblocked), and ELK's relicense is merged so #62 proceeds via the out-of-process runner now.

---

## 1. Cross-cutting decisions (resolve once, apply everywhere)

- **X1 — Modernization sequencing.** FILLED (from #96 comment, endorse as-is): `#92 (done)` →
  `#93 (nullness)` → `(#94 ∥ #95)`, with **#94's `SimEvent` record and #95's `SimEvent.todo`
  sealing shipping as one coordinated PR**, and #95's dispatch conversion serialized against #84's
  SimpleEditor rewrite. Architecture then rides on top: `#93(core slice)` → **#77** → **#78** →
  **#79 tag-decoupling** → the #94/#95 renames. Editor UX is a separate wave: **#76 → #74 → #75 →
  #84 → #86** (#75's menu/accelerator half is L&F-independent and may land earlier).

- **X2 — Save-format tag-freeze rule (unblocks every rename in #77/#78/#79/#94/#95).** FILLED: the
  canonical **write-tag is data, frozen as today's simple class name**; renaming/moving a Java class
  becomes an **alias-table entry**, never a changed tag, never a format-version bump. Consequence:
  **land #78's registry + #79's alias table before any element class/package rename touches a
  persisted type.** The record conversions in #94 (`SimEvent`, `GridTransform`, `LoadError`) are
  *not* serialized by class name, so they are #79-independent.

- **X3 — Memory timing + x/z coercion, decided once for #59/#61/#63.** RESOLVED (best practice):
  **`time 0` (zero-delay) for imported async-read memories** (the load path already accepts it,
  `Memory.java:307-308`) + a **single x/z rule — `x`→0, `z`→disabled/floating** — across export (#59),
  import (#61), and co-sim (#63). Settles five listed sub-items together.

- **X4 — Single-maintainer custody stance (all of §5 signing).** FILLED context: every signing
  key/cert/account resolves to one person (bus-factor 1). State this in each custody record; it is
  the deciding argument in several signing choices and the reason "no long-lived key at all" is a
  legitimate close-with-rationale outcome for #136.

- **X5 — `#91` is the acceptance vehicle for all UX issues.** FILLED: dismissal/menu-content (#86),
  keyboard dispatch (#75), viewport mapping (#74), empty-state presence (#73), visual/semantic
  assertions (#76), decomposed-collaborator tests (#84) all land their P-criteria as #91 harness
  tests. Editor-smoke coverage from #56 should move here too.

---

## 2. Modern-Java program — #96, #93, #94, #95

Settled (do not re-litigate): #92 closed (Java 25 LTS baseline, enforcer `[25,)`, CI 25 + advisory
26); #80 plugin loader deleted (closed-world met); sequencing per X1.

### #96 — Java-25 baseline tracking issue
- **Build-time budget baseline for the 1.5× `mvn verify` gate.** RESOLVED (measure the variable under
  test): baseline is **post-#92 / pre-#93** (isolates Error Prone's cost); publish it in #93's first PR.
- **Must the advisory Java-26 CI leg stay green under the new analyzers?** RESOLVED (don't gate on an
  upstream analyzer's release cadence): run **EP/NullAway only on the enforced 25 leg**; keep 26 a
  plain-compile advisory leg. Record as the LTS-raise policy.
- Java version/toolchain: FILLED — Java 25 LTS, shipped in #92; the "stop at 21" was rejected.

### #93 — JSpecify + NullAway
- **Error Prone/NullAway version supporting JDK 25.** RESOLVED (verified 2026-07-17) — **not a
  blocker.** Pin **Error Prone 2.50.0** (runs on JDK 21+, supports 25) + **NullAway 0.13.7** (requires
  only EP 2.36.0+/JDK 17+; JSpecify mode recommends Java 25). Wire both directly with the
  `--add-exports jdk.compiler/...`/`-J` flags EP needs on a modern JDK. The annotation-only interim is
  no longer necessary, and #93 no longer gates #94/#95. Basis: google/error-prone releases + uber/NullAway
  release notes.
- Enforcement model: FILLED — **hard `-Werror` gate from day one, per-package** via the ratchet
  (`OnlyNullMarked`; a package is enforced the moment its `package-info` gets `@NullMarked`, never
  un-marks). Fallback is per-package: if a package blows the 20%-suppression ceiling (likely
  `jls.edit`), retreat that package to new-code-only and document it.
- Rollout order: FILLED — `jls` root + `jls.sim` → `jls.elem` → `jls.edit` last; use `@Initializer`
  for the `init()`-assigns-fields idiom, not blanket `@SuppressWarnings`.
- `JLSInfo` 26 static fields: FILLED — annotate `@Nullable` + lifecycle javadoc **now**, before #77
  (annotations transfer; this is #77's "down payment").

### #94 — Records / value semantics / retire `Cloneable`
- **The element hierarchy CANNOT become records (the key clarification).** FILLED — every element
  extends a class (`Element`/`LogicElement`/`Gate`), and a `record` can extend no other class. H1's
  "≥10 record conversions" must come from **standalone value carriers**: `SimEvent`, `GridTransform`,
  `SpatialIndex` entries, `LoadError`, `SimEvent.todo` payloads, batch/test-vector rows. Explicitly
  **exclude the element hierarchy and the `Attribute` behavior objects** (identity+behavior, not
  values) from the survey so the count isn't chased into a wall.
- **`SimEvent`-as-record breaks the simulator's dedup `HashSet`.** RESOLVED (best practice) —
  **keep `SimEvent` a plain `final class`; do not make it a record.** Its equality is intentionally
  non-structural (`equals` excludes `seq`; `hashCode` is `(int)time`, `SimEvent.java:75-101`), and the
  settled guidance (Effective Java; the records JEP) is *don't declare a record whose defining feature —
  structural equality — you immediately override*. Take the record win on the genuinely-structural
  carriers instead (`GridTransform`, `LoadError`, and the `todo` payload records below). Still
  **write the dedup regression test first** (two same-`(time,callBack,todo)`/different-`seq` events →
  one survives) — it guards the behavior regardless. This revises the earlier "record with hand-written
  equals" idea; flag for veto, but a class is the cleaner mainstream choice here.
- Save-format coupling scope: FILLED — the record candidates are **not** class-name-serialized, so
  #94's core work is **#79-independent**; the #79 gate only bites a rename of a *serialized* `jls.elem`
  type (which can't be a record anyway). State this so #94 isn't needlessly serialized behind #79.
- `Cloneable` retirement scope: FILLED — the 67 hits are two populations: **`BitSet.clone()`** on JDK
  types (correct value-copies — leave alone) vs genuine **element deep-copy** (`Element.copy()` —
  route through #78's `copy()` contract). No type actually `implements Cloneable` with a broken
  `clone()`. This narrows the stated "61 clone sites" sharply.
- `final`-by-default: FILLED — touched/new fields+locals become `final` unless mutation is the point;
  **no repo-wide reformat commit**; convention recorded in `CONTRIBUTING.md`.

### #95 — Seal the element hierarchy + exhaustive switch dispatch
- The exact `permits` tree: FILLED (derived from the tree; `Element` is never instantiated →
  `abstract sealed`). Root: `Element permits LogicElement, DisplayElement, Wire`; `LogicElement`
  permits the 4 abstract intermediates (`Gate`, `Group`, `Pin`, `SigSim`) + ~18 concrete leaves;
  `Gate permits AndGate,NandGate,NorGate,NotGate,OrGate,XorGate,Extend,DelayGate`;
  `Group permits Binder,Splitter`; `Pin permits InputPin,OutputPin`; `SigSim permits SigGen,TestGen`;
  `DisplayElement permits Text`. Leaves become `final`. Seal bottom-up (`Gate` first). Sealing is
  declaration-only; the reflective loader is indifferent to `sealed` — confirms zero behavioral change.
- `Put`/`WireNet` are separate roots: FILLED — `Put (permits Input, Output)` and `WireNet` are **not**
  under `Element`; seal them independently and don't treat their `instanceof` probes as element dispatch.
  Capability interfaces (`TriProp`, `Reacts`, `Printable`) stay interfaces.
- **`SimEvent.todo` sealing scope (the largest genuine #95 decision).** RESOLVED (best practice) —
  **full seal, co-shipped with #95's `Element` sealing as one coordinated PR** (note: `SimEvent` itself
  stays a class per #94 above; only its `todo` field gets typed). Replace `Object todo` with a
  **sealed `Payload` interface + records** (`PinChanged`, `NewValue(BitSet)`, `TriStateOff`,
  `StateChanged`) — the named records eliminate the `null`/`"off"` sentinels — and convert the ~21
  `react()` consumers to exhaustive `switch` with record-pattern destructuring (JEP 440/441, the whole
  point of the modern-Java program). Pin the `BatchSimulator` truth-table goldens before/after. Census:
  ~30 post-sites, mostly `BitSet`, plus the `String "off"` (`TriState.java:637`), a `StateMachine` state
  object, and a `null` ("input pin changed", `WireNet.java:487`). If the blast radius proves too large
  mid-PR, fall back to sealing only the `Element` hierarchy — but the default is the full seal.
- Orientation exhaustiveness: FILLED (dependency) — #78 owns which of the two `Orientation` enums
  survives; #95 sweeps switches over the survivor to exhaustive **after** #78.
- Census reconciliation: FILLED (method) — the tree has **193** `instanceof` sites (not the body's
  166; top: SimpleEditor 87, HdlExporter 22, Circuit 15). Classify all 193 into dispatch→switch /
  test-and-cast→pattern / capability→interface, and key the acceptance exhibit to the dispatch set.

---

## 3. Architecture & save format — #77, #78

Landed (not re-surfaced): #77 ratchet; #78 v5.0.0 source break authorized + unify `JLSInfo.Orientation`.

### #77 — Extract headless `jls.core`
- **`InteractiveSimulator`: shed Swing in place, or move GUI-side?** RESOLVED (best practice —
  headless-engine/GUI-shell separation): **split `Simulator` into a core abstract engine
  (`runEventLoop` + the #25 hooks) + a GUI `InteractiveSimulator` subclass** that owns the Swing/print
  surface; `BatchSimulator` stays core (its `javax.print` import follows the interactive concerns out).
- **`jls.elem` model/render split + where geometry gets text metrics.** RESOLVED (best practice —
  model/view separation): **renderer-per-element (GUI-side)** consuming model geometry, plus a core
  **`TextMetrics` interface** (Swing `FontMetrics` impl + a deterministic headless impl for tests).
  Large per-element refactor — keep it on the ratchet, one element at a time.
- **Literal `jls.core.*` rename vs enforce-clean-in-place.** RESOLVED (best practice — don't take a
  churn-heavy rename that risks the on-disk format): **keep the current package names, enforce the
  boundary with the ratchet test, declare them "the core" now.** A physical `jls.core.*` rename is
  optional polish, and only *after* #79's alias table owns the tag namespace (renaming `jls.elem`
  before that breaks `Class.forName` routing for every legacy file).
- **Where core emits user diagnostics** (`TellUser` is Swing-coupled). RESOLVED (best practice —
  core returns data, edges render it): **core returns `LoadError`/result objects; the GUI translates
  to `TellUser`** (extend the existing `Circuit.load` pattern — no core reporter interface needed).
  Retiring the 40 `JLSInfo.frame` dialog-parent uses is the residual #77 work.
- Sequencing vs modern-Java: FILLED — land #92 + the `jls`/`jls.sim` slice of #93 **before** the bulk
  #77 extraction so the core is written once in the new idiom.

### #78 — Element descriptor & registry
- **Descriptor location under #77.** RESOLVED (standard core/GUI separation): **two-layer split** —
  core `ElementType` (tag, aliases, factory, capabilities) + GUI `PaletteEntry` (icon, category, dialog,
  help). The core table is what #79's alias decoupling and #61's HDL cell-map consume.
- **Registration: manual list vs derived from #95's sealed `permits`?** RESOLVED (avoid the soft cycle
  with #95): **manual `ElementRegistry` first** (unblocks #79/palette with no #95 dependency), then
  adopt permit-derivation for a compiler-enforced totality check **after** #95 seals.
- Capability interface set: FILLED — `Rotatable`, `Timed`, `Watchable`, `QuickEditable` (move the
  `instanceof Memory` timing branch into `Timed`); open edge: also make `canChange/change` an
  `Editable` interface and delete the `Wire/WireEnd` special-case.
- Orientation unification casing: FILLED — unify on **`JLSInfo.Orientation` (UP/DOWN/LEFT/RIGHT)**;
  saved values are already uppercase (spec §7) so the switch is **byte-identical on save**; delete the
  lowercase `Gate.Orientation` in-memory duplicate.

---

## 4. Editor UX — #86, #84, #73, #75, #76

State: `SimpleEditor.java` is now **4,477 lines** (issues cite 4,119); 9-state enum at `:495`; undo
depth hardcoded `10`; `circuitsize=1000`; zero `java.util.prefs`; KeyPad half of #86 landed. All
`SimpleEditor.java:NNNN` citations below have drifted — re-derive at implementation time.

### #86 — KeyPad / subcircuit / context menus (KeyPad P1–P3 done; do NOT reopen "global right-click hook")
- **Subcircuit tab: discoverability-only vs true read-only inspect?** RESOLVED (matches owner
  comment-3; standard "editing a child context disables the parent + shows why" pattern): **keep
  `setEnabled(false)` + a dialog-adjacent banner on the parent tab.** True read-only inspection, if
  ever wanted, is a separate follow-up.
- **Legacy right-click gestures when the context menu claims BUTTON3?** RESOLVED (standard desktop
  idiom — right-click *is* the context menu): **migrate the legacy gestures to menu items and suppress
  the menu mid-gesture** (`placing/moving/startwire/drawire`); build it inside #84's extracted state
  machine.
- Menu contents: FILLED floor (rotate/flip/delete/properties via #75 Actions); open edge — build the
  menu **per-hit-test from element capability traits** (#78 descriptor) so only applicable actions show.
- Blocked? FILLED — remaining #86 work depends on #75's Action layer; the subcircuit-banner half is
  independent and can land now.

### #84 — Decompose SimpleEditor (target structure decided; each extraction = 1 PR)
- **Undo cost model: keep 10 whole-circuit snapshots or move to delta?** RESOLVED (best practice —
  don't pre-optimize; whole-circuit snapshots are simple and correct at classroom scale): **keep the
  snapshot model**, and only revisit if profiling a 95th-percentile circuit shows a real cost (median
  snapshot > ~16 ms or stack > ~50 MB). Record the measurement in the extraction PR; don't build a
  delta engine speculatively.
- Done target: FILLED — **`SimpleEditor.java` < ~1,500 lines**, no test regressions; flag: file is
  4,477 lines now, re-baseline the §8 line table.
- **State machine behind what interface?** RESOLVED (GoF State pattern — the settled choice for a
  mode-heavy interaction machine): **state-per-class** (`State` interface with `Idle`/`Placing`/
  `Moving`/… impls) — makes #75's keyboard modes purely additive and kills the giant `switch`; unify
  the two duplicated Esc paths into it.
- Undo depth pref: FILLED — extract `UndoManager` now, read depth from #76's preference, degrade to
  `JLSInfo.undoStackDepth = 10` until #76 lands.
- Sequencing: FILLED — after #43/#74/#75 (same `SimpleEditor:1295-3200` region); consume #75 Actions;
  host #86.

### #73 — First-run onboarding (owner: samples from MTU site; Edit menu→#75; tutorial already resizable)
- **Which specific sample circuits?** RESOLVED (covers the pedagogy spread; author fresh so licensing
  is clean): **full adder (combinational), N-bit counter (sequential), a mux demo, a
  subcircuit-instantiating circuit, and a traffic-light/vending FSM**, under `resources/samples/`, via
  File→Open Sample, each with a header Text element; must load on a clean install (depends on #34).
  (Swap any for an existing test fixture where one already fits.)
- Empty-state panel: FILLED — Swing panel in the tab area (New / Open sample / Tutorial / Open recent);
  **omit "Open recent" until #76** lands; presence → #91.
- **Tutorial refresh scope: minimal nav here vs the M5 web-docs rework?** RESOLVED (best practice —
  fix staleness now, don't fold in a docs-platform rework): **minimal here** (next/prev + larger initial
  dialog size + applet/`isDemo`/orphan-`tutorial.html` cleanup); **defer** the "should docs be versioned
  web pages" question (audit M5) out of #73.
- README screenshots: FILLED — capture **after #76's theme lands** (so they don't rot); two shots +
  overview + honest positioning paragraph + tutorial pointer.

### #75 — Keyboard operability (owner adjudicated 5 pts: keep old bindings as aliases day-one; menu/accel L&F-independent; `:483-484` is `matchJump` not rotate; #75 owns the Action layer + Edit menu; keyboard-flow→#91)
- **Exact accelerator/mnemonic scheme + what wire-start rebinds to.** RESOLVED (platform convention):
  **N/O/S/Shift+S** for file ops; **W = close** (old wire/watch meaning kept as an alias per the
  adjudication); **Q = quit**; **redo = Shift+Cmd+Z (macOS) / Ctrl+Y (Win/Linux)**; **rotate R /
  Shift+R**, **flip F**; **wire-start = `E`**; menu mnemonics **F/E/L/V/S/H**.
- **Focus model: remove focus-follows-mouse, or keep it as an augmentation?** RESOLVED (industry
  standard — focus-follows-mouse is an X11 window-manager idiom, *not* desktop-app behavior; standard
  is click/Tab focus): **remove the `mouseEntered` focus grab, make the canvas focusable on
  tab-select/click, and route global accelerators via `WHEN_IN_FOCUSED_WINDOW`** so shortcuts fire
  regardless of pointer location; audit dialog modality (those bindings can fire while a dialog is up).
- **Is the deep half (H2: keyboard placement + arrow-nudge + port-to-port wiring) in scope, or ship H1
  first?** RESOLVED (standard incremental delivery): **ship H1 (menu bar + accelerators + focus fix +
  rotate/flip) first**; deliver keyboard place/move/rotate/simulate with it if cheap; **gate full
  keyboard wiring behind a feasibility spike** (the model may lack a port-selection metaphor — the
  issue's own refutation clause).
- Canvas a11y scope: FILLED — full screen-reader support for the **canvas** is **out of scope**; this
  issue delivers menu/dialog a11y only.

### #76 — Visual ergonomics / HiDPI / L&F / dark / prefs (owner adjudicated: blue/orange CVD palette + classic selectable; system L&F default + dark + `java.util.prefs`; drop the "renders tiny" HiDPI premise; sequence #76→#74→#75)
- **Default L&F + dark-mode mechanism.** RESOLVED (best practice — FlatLaf is the de-facto standard
  for modern Swing theming, incl. IntelliJ-style apps, and gives consistent cross-platform light/dark
  where system L&Fs, especially GTK, do not): **use FlatLaf as the primary L&F on all three OSes** with
  **one `theme` preference {system, light, dark}** switching the FlatLaf variant and the `Theme` canvas
  variant together ("system" auto-detects). Delete the `System.exit(1)` on L&F-load failure (make it a
  fallback). *This mildly revises the earlier "system-L&F-default, FlatLaf-as-fallback" adjudication —
  flagged for veto, but FlatLaf-primary is the mainstream Swing choice and removes the per-platform
  L&F-quality gamble entirely.*
- **Preferences storage: `java.util.prefs` vs a visible properties file.** RESOLVED (standard, zero-dep):
  **`java.util.prefs`** (OS-native backend, no hand-rolled pathing, in-memory sandbox fallback). Persist:
  theme, colors, undo depth, last directory (dep #34), recent files, window geometry, per-window zoom.
- Second visual channel: FILLED — value wires thicker, Hi-Z dashed, touching endpoints a distinct glyph
  (color-independent); minor open edge — resolve the exact thick-vs-dash mapping empirically against a
  deuteranopia/protanopia screenshot-filter run.
- Settings dialog scope: FILLED — theme, colors, undo depth, recent files, last directory; `Theme` is a
  record of semantic roles with light+dark variants.

---

## 5. Code-signing & distribution — #134

Cross-cutting (X4): single-maintainer custody. **#134 edits the `installers` job in `release.yml`** and
touches #82's README "unsigned" text — keep that README hunk consistent.

### #134 — Windows Authenticode  (free; unaffected by the Apple decision)
- **SignPath.io OSS tier vs Azure Trusted Signing?** RESOLVED (SignPath OSS is the standard, free path
  for open-source Windows signing): **SignPath.io OSS tier** — free; publisher "SignPath Foundation"
  known before enrollment (README can name it now); the service holds the key; its "must build from
  public CI" requirement is already met. (Azure ≈ $10/mo with own legal name; classic OV/EV certs are
  rejected post-2023.) **The remaining action is an account step, not a decision: enroll the project in
  SignPath's OSS program.**
- **SignPath approval mode.** RESOLVED — **auto-approval** on the release signing policy (else the
  action times out at 600 s on an unattended release); configure it in the SignPath console at setup.
- **`verify-windows-signatures` strict exactly-two-msi gate.** RESOLVED (best practice — a missing
  installer should be loud): **keep strict**; it turns a silently-absent installer into a red job.
- README fallback clause for pre-signing releases: FILLED — **keep** it until unsigned releases age out.
- Unsigned intermediate artifact retention: RESOLVED — `retention-days: 1`.
- Dry-run `force-sign` `workflow_dispatch` boolean: FILLED — adopt as drafted.

---

## 6. GUI / Wayland / UI testing — #101, #91

### #101 — Wayland CI rig (rig + `gui-wayland` lane exist; `JBR_SHA256` fill + first run already tracked)
- **No minimal-Swing control program though §9's falsification requires one.** RESOLVED (make the rig
  self-diagnosing): **add an always-on `HelloSwing` control step alongside JLS** so every run separates
  a JLS-startup defect from an upstream JBR/sway blocker.
- **P2 pixel-diff computed but not gated.** RESOLVED (empirical threshold, standard for screenshot
  gates): **set the threshold from the first green run's actual AE value** (~10% of observed), not the
  blind 1% guess.
- **Promotion to a required check — nothing schedules the "20 consecutive runs."** RESOLVED: **add a
  nightly `schedule:` cron** running only `gui-wayland`, so the stability count accrues independent of
  push cadence.
- weston fallback (named in §9, not wired): RESOLVED — add **reactively**, only if sway is shown to be
  the blocker (don't pre-build a second compositor path).

### #91 — Automated UI test harness (Layer 1 already built & green)
- **Layer-2 framework: AssertJ-Swing vs `java.awt.Robot`?** RESOLVED (best practice — AssertJ-Swing is
  dormant/untested against modern JDKs; `Robot` is the stock, maintained primitive): **`java.awt.Robot`
  + `SwingUtilities.invokeAndWait`**, with a thin fixture layer and a `dispatchEvent`-only tier as the
  flake retreat.
- **Which display backend runs Layer-2 — Xvfb (X11) vs the #100/#101 "no X11" program.** RESOLVED
  (standard headless-AWT CI + parity check): **Xvfb + `Robot` for interaction tests** (most mature for
  synthetic input) **plus the #101 sway+JBR rig for startup/render parity**. Reconcile the wording in
  the issue (it currently reads Xvfb-only) — Xvfb here is test-only infra, not a shipped dependency, so
  it doesn't reintroduce X11 into the product.
- Layer-2 sequencing after #84: FILLED — honor the stated order (Layer 1 done; Layer 2 not before #84's
  state-machine extraction).
- **Flakiness quarantine policy (P5).** RESOLVED (standard CI hygiene): `@Tag("ui-interaction")`
  **excluded from the required gate; one flake → quarantine + file an issue**; reuse #101's 20-run bar
  to promote a test into the required set. No bounded-retry masking.
- P6 pilot (which UX issue converts a manual criterion first): RESOLVED — **#75** (menu accelerators;
  no rendering layer needed, cheapest against the existing Layer-1 infra).

---

## 7. HDL interoperability — #59, #61, #62, #63

Stage-1 export shipped. Settle the **Memory-timing / x/z-coercion pair once** (X3).

### #59 — HDL umbrella
- Non-synthesizable export policy: FILLED (resolved in code — `HdlExporter.java:364-388` +
  `HdlPolicyTest`): warn-skip = Display/SigGen/Pause/Stop/Text/TestGen; topology = Wire/WireEnd/Jump;
  reject = SubCircuit/Memory/Mux/Decoder/StateMachine/TruthTable.
- **SubCircuit handling — rejected today, but classrooms are heavily hierarchical.** RESOLVED
  (standard hierarchical HDL export — Digital/Logisim-evolution both emit a module per subcircuit type):
  **module-per-subcircuit-type + instantiate.** (Inline flattening is an acceptable fast interim if the
  hierarchical walk proves slow, but the hierarchical form is the target.)
- **Synthesizable elements still in the reject bucket (Mux/Decoder/StateMachine/TruthTable/Memory).**
  RESOLVED (standard synth templates): **add all with the conventional templates** — Mux/Decoder/
  TruthTable → `case`/`casez`; StateMachine → a generated `case` block with **binary state encoding**
  (the default; add one-hot later only if a corpus needs it); Memory → an array with zero-delay async
  read (rides X3's `time 0`) + `$readmemh`-style init. Land the combinational three first (lowest risk),
  then StateMachine and Memory.
- Target dialects: FILLED — Verilog-2005 structural + VHDL (ghdl `--std=93/02` verified);
  SystemVerilog/SystemC out of scope.

### #61 — Stage 2 Yosys import
- **Memory async-read timing** (accessTime defaults to 100 units; async ROM is zero-delay → waveform
  parity fails). RESOLVED (X3 — standard for async ROM): **import Memory with `time 0`** (the load path
  already accepts it, `Memory.java:307-308`) + a zero-delay-read parity test. This also settles #59's
  Memory export sub-decision.
- **Clocked / multi-port memories** (JLS is async single-port). RESOLVED (standard scoping): **reject
  with a teachable message at launch**; an **opt-in `memory_map` bit-blast with a hard size cap
  (≤256 words)** is a later add.
- Register async-clear pin vs keep `$adff` rejected: FILLED (#61 comment) — **keep `$adff` rejected**,
  corpus-evidence-driven, deferred (`Register` has only D,C).
- x/z coercion: FILLED — `x`→0, `z`→disabled TriState; keep identical to #63 (X3).

### #62 — Auto-layout companion
- **ELK relicensing / hand-roll-vs-link.** RESOLVED (checked 2026-07-17): the GPLv3 relicense is
  **merged** (issue #1185 *closed* via PR #1184) but **0.12.0 has not shipped** (latest v0.11.0). It
  doesn't matter — **use the out-of-process ELK runner now** (a separate process exchanging JSON is
  license-clean even under EPL-2.0). **Do NOT hand-roll a Sugiyama layouter.** Switch to in-process ELK
  linking later if 0.12.0 ships with the relicense (a simplification, not a blocker). This removes the
  single most cost-inverting risk in the HDL workstream.
- Escalation rubric (now the *acceptance* rubric for the ELK output): FILLED — 0 overlaps hard-fail;
  ≤0.5 crossings/net; ≤2.0× Manhattan wire-length; ≥90% L-to-R; ≤4× hand-drawn bbox. JLS side owns
  grid-snap, port offsets, and WireEnd chains behind a fixed layout interface.
- **Three showcase golden layouts.** RESOLVED (narrow): commit the **ALU-slice, counter, and small-FSM**
  as authentic hand-drawn `.jls` goldens under `test/resources/hdl/layout-goldens/`, curated by whoever
  lands #62; wire them into the §7 metrics harness + the round-trip test.
- Cycle/feedback + grid invariants: FILLED — break feedback at register outputs, route back-edges;
  12-px grid, WireEnds at exact port offsets, round-trip identical; import arrives as one undoable
  pre-selected unit.

### #63 — Stage 3 co-simulation
- **Combinational-path-through-black-box fixpoint iteration.** RESOLVED (standard event-sim technique):
  **delta-cycle to convergence reusing JLS's own post/react settle loop** (each subprocess exchange
  pure), with a bounded delta-cycle cap → oscillation error, loop **detection as a diagnostic**, and
  caching of identical-input exchanges to bound latency (P3).
- **Co-sim transport (subprocess vs VPI/DPI vs cocotb).** RESOLVED (standard, and what Digital proves):
  **per-component GHDL/Icarus subprocess over stdin/stdout behind a transport interface.** Record in the
  issue why VPI (native build + JNI + per-platform fragility) and cocotb (a Python runtime) are rejected;
  keep the protocol behind the interface so a VPI path could replace it if latency (P3) ever demands.
- x/z coercion into 2-state: FILLED — adopt #61's rule verbatim (X3), with a per-component warning.
- Port-scan fallback + failure/lifecycle surface: FILLED — scanner defers to Yosys `write_json`, never
  becomes a parser; the addendum fully specifies the states, content-hash policy, ~5 s timeout,
  no-zombie reaping, and the editable-without-tools guarantee.

---

## 8. Test gaps & release tracking — #33

Context: v5.0.0–v5.0.4 shipped; #33 is 25/26 sub-issues closed (only #59 open). Test count is now
**65 files / ~399 `@Test`** (not the body's "70 tests/12 classes").

### #33 — Post-audit / v5.0.0 tracker
- **The release already shipped while the stated policy was "cut the RC only once the tracker is empty,"
  and #56/#59 are still open.** NEEDS-MAINTAINER (the central live contradiction). Recommend
  **amending the ship-policy paragraph** to record that v5.0.0 shipped with #56/#59 deliberately
  deferred as non-blocking, **then closing #33** and reparenting #56/#59 to a post-1.0 backlog (#59 is
  its own multi-stage HDL umbrella and shouldn't hold the correctness tracker open).
- Exit criterion met? FILLED — **yes**: the coverage ratchet is live and enforced, a shipped artifact
  self-reports version+license, Phases 1–5 are closed. Nothing technical blocks closing #33.
- Residual sub-issues must-have vs deferred: FILLED — **both #56 and #59 are non-release-blocking**;
  label them `post-1.0`.
- The 4 ordering/link defects: FILLED — **already resolved** (2026-07-08 "punch list applied"). The only
  residue is owner-UI: the **severity/phase label taxonomy + milestone don't exist yet**, and the
  #80/#48 deprecation-window calendar dates are unrecorded (mark #80's window closed — removal shipped).
- "#56 corpus feeds #57/#38/#79/#60/#61" dependency still binding? FILLED — **no**; mark it satisfied so
  the corpus isn't mis-triaged as an upstream blocker.

---

## 9. What actually still needs you (after 2026-07-17)

The design-taste, external-state, and Apple decisions are all **RESOLVED** above. What's left is a
short list — one account/custody item and the project-management triage on the stale tracker.
Everything else in this document is settled and just needs your ✔ when the work is picked up.

**Account / custody (free, but a human step):**
1. **#134 — enroll the project in SignPath's OSS program** (the signer, publisher, and auto-approval
   policy are all decided; this is the account action) and set the release policy to auto-approve.

**Project-management triage on the stale tracker (your call on your own project):**
2. **#33 — reconcile the "cut the RC once the tracker is empty" policy with the already-shipped
   v5.0.0**, then close #33 and reparent #59 as non-blocking post-1.0 work. Also: create the
   severity/phase label taxonomy + milestone, and mark #80's deprecation window closed (removal shipped).

That's the whole remaining decision surface. Optional confirmations you can also veto if you disagree:
the two best-practice calls that revise an earlier adjudication — **FlatLaf-primary over
system-L&F-default (#76)** and **`SimEvent` stays a class rather than a record (#94)**.
