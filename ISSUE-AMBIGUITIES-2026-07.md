# Open-Issue Ambiguities & Decisions — July 2026

Scope: every one of the **29 open issues** (#33, #56, #59, #61–#63, #73–#79, #81, #82, #84, #86,
#91, #93–#96, #100, #101, #111, #128, #134–#136), read together with their comments, the working
tree at `2c0ee59`, and the prior audit docs (`ISSUE-REVIEW-2026-07.md`, `AUDIT-2026-07.md`,
`ERGONOMICS-AUDIT-2026-07.md`, `MAINTENANCE_AUDIT.md`).

Purpose: surface **every genuine decision or ambiguity** a maintainer must resolve before work can
proceed, **fill in the ones answerable from the code/docs/best-practice**, and flag the rest with
concrete options and a recommended default — in a form ready to paste back into each issue.

Legend for each decision point:
- **FILLED** — answerable now; the answer + its one-line basis is given. Just needs your ✔/✗.
- **NEEDS-MAINTAINER** — a real choice only you can make (cost, custody, policy, taste, or external
  state). Concrete options + a recommended default are given.

---

## 0. Read this first — three findings that reframe the whole backlog

1. **Much of the backlog has already shipped; several issues are bookkeeping-stale, not blocked.**
   v5.0.0–v5.0.4 are tagged and released (CHANGELOG at `5.0.5-SNAPSHOT`). Landed since the issue
   bodies were written: the FORMAT header + spec + `FileFormatSpecTest` (#79 core), the `TellUser`
   reporter + `NotificationRatchetTest` (#81), the headless-core ratchet (#77), the KeyPad half of
   #86, HDL Stage-1 export (#59), the Wayland code idioms (#100 sub-issues #102–#105), the #56 edge
   goldens / JaCoCo ratchet / CLI smoke tests, and the Layer-1 UI harness (#91). **Before deciding
   anything, re-scope or close the stale issues** — details under #33, #56, #79, #81, #86, #100.

2. **A few decisions, made once, resolve many issues.** See §1 (cross-cutting) — settling the
   modernization sequencing, the save-format tag-freeze rule, the Memory-timing/xz-coercion pair,
   and the single-maintainer custody stance collapses ~10 downstream questions.

3. **The only genuine *external* blockers are two:** whether Error Prone/NullAway support JDK 25 at
   pickup time (#93, gates #94/#95), and the ELK GPLv3 relicense state of `eclipse-elk/elk#1185`
   (#62). Both have a documented "do the cheap slice now" fallback so nothing else stalls on them.

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

- **X3 — Memory timing + x/z coercion, decided once for #59/#61/#63.** NEEDS-MAINTAINER (small):
  pick **`time 0` (zero-delay) for imported async-read memories** (the load path already accepts it,
  `Memory.java:307-308`) vs **batch-only parity with a documented timing divergence**; and adopt a
  **single x/z rule — `x`→0, `z`→disabled/floating** — across import (#61) and co-sim (#63).
  Recommend `time 0` + the single coercion rule; this settles five listed sub-items together.

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
- **Build-time budget baseline for the 1.5× `mvn verify` gate.** NEEDS-MAINTAINER. Measure against
  **post-#92 / pre-#93** (isolates Error Prone's cost, the variable under test), not pre-#92.
- **Must the advisory Java-26 CI leg stay green under the new analyzers?** NEEDS-MAINTAINER. Run
  EP/NullAway **only on the enforced 25 leg**; keep 26 a plain-compile advisory leg (don't gate the
  build on an upstream analyzer's release cadence). Record as policy.
- Java version/toolchain: FILLED — Java 25 LTS, shipped in #92; the "stop at 21" was rejected.

### #93 — JSpecify + NullAway
- **Error Prone/NullAway version supporting JDK 25 (highest-risk, external).** NEEDS-MAINTAINER.
  If no released EP supports 25 at pickup, **land the annotation-only slice first**
  (`@NullMarked`/`@Nullable` + `package-info.java`, zero toolchain risk, doubles as `JLSInfo`
  lifecycle docs) and enable NullAway enforcement when EP-25 ships. Confirm required
  `--add-exports`/`-J` compiler flags before wiring.
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
- **`SimEvent`-as-record breaks the simulator's dedup `HashSet`.** NEEDS-MAINTAINER (latent-defect
  adjacent — this *is* the issue's predicted P2/P3). Current `equals` deliberately excludes `seq` and
  `hashCode` is `(int)time` (`SimEvent.java:75-101`); a default record would structural-equal over
  all four components incl. the always-unique `seq`, so `dupCheck` would never coalesce again.
  Recommend: convert to a record but **hand-write `equals`/`hashCode`/`compareTo`** to preserve
  today's `(time,callBack,todo)` dedup + `(time,seq)` order, and **write the dedup regression test
  first** (two same-`(time,callBack,todo)`/different-`seq` events → one survives).
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
- **`SimEvent.todo` sealing scope (the largest genuine #95 decision).** NEEDS-MAINTAINER. Payload
  census: overwhelmingly `BitSet`, plus a `String "off"` sentinel (`TriState.java:637`), a
  `StateMachine` state object, and a `null` sentinel ("input pin changed", `WireNet.java:487`) —
  across ~30 post-sites and ~21 `react()` consumers. Recommend **full seal, tightly scoped, co-shipped
  with #94's `SimEvent` record as one PR**: a sealed `Payload` interface with records
  (`PinChanged`, `NewValue(BitSet)`, `TriStateOff`, `StateChanged`), exhaustive `switch` in `react`,
  named records replacing the `null`/`"off"` sentinels, sim goldens pinned before/after. If blast
  radius proves too large mid-PR, fall back to sealing only the `Element` hierarchy in #95.
- Orientation exhaustiveness: FILLED (dependency) — #78 owns which of the two `Orientation` enums
  survives; #95 sweeps switches over the survivor to exhaustive **after** #78.
- Census reconciliation: FILLED (method) — the tree has **193** `instanceof` sites (not the body's
  166; top: SimpleEditor 87, HdlExporter 22, Circuit 15). Classify all 193 into dispatch→switch /
  test-and-cast→pattern / capability→interface, and key the acceptance exhibit to the dispatch set.

---

## 3. Architecture & save format — #77, #78, #79, #81

Landed (not re-surfaced): #77 ratchet; #78 v5.0.0 source break authorized + unify `JLSInfo.Orientation`;
#79 FORMAT header/version-negotiation/spec/test; #81 `TellUser` + ratchet (only `TellUser.java` itself
still touches `JOptionPane`).

### #77 — Extract headless `jls.core`
- **`InteractiveSimulator`: shed Swing in place, or move GUI-side?** NEEDS-MAINTAINER (the ratchet
  javadoc leaves it open). Recommend **split `Simulator` into a core abstract engine + a GUI
  `InteractiveSimulator` subclass**; `BatchSimulator` stays core (its `javax.print` import follows out).
- **`jls.elem` model/render split + where geometry gets text metrics.** NEEDS-MAINTAINER. Recommend
  **renderer-per-element (GUI-side)** consuming model geometry + a core **`TextMetrics` interface**
  (Swing `FontMetrics` impl + deterministic headless impl). Large per-element refactor — keep it on
  the ratchet one element at a time.
- **Literal `jls.core.*` rename vs enforce-clean-in-place.** NEEDS-MAINTAINER + hard constraint:
  renaming `jls.elem` breaks `Class.forName` tag routing **until #79's alias table owns the
  namespace**. Recommend **keep the names, enforce clean, declare "the core" now**; physical rename is
  optional polish after #78/#79.
- **Where core emits user diagnostics** (`TellUser` is Swing-coupled). NEEDS-MAINTAINER. Recommend
  **core returns `LoadError`/result objects; the GUI translates to `TellUser`** (extend the existing
  `Circuit.load` pattern). Retiring the 40 `JLSInfo.frame` dialog-parent uses is the residual #77 work.
- Sequencing vs modern-Java: FILLED — land #92 + the `jls`/`jls.sim` slice of #93 **before** the bulk
  #77 extraction so the core is written once in the new idiom.

### #78 — Element descriptor & registry
- **Descriptor location under #77.** NEEDS-MAINTAINER. Recommend a **two-layer split**: core
  `ElementType` (tag, aliases, factory, capabilities) + GUI `PaletteEntry` (icon, category, dialog,
  help). The core table is what #79's alias decoupling and #61's HDL cell-map consume.
- **Registration: manual list vs derived from #95's sealed `permits`?** NEEDS-MAINTAINER (soft cycle
  with #95). Recommend **manual `ElementRegistry` first** (unblocks #79/palette, no #95 dep), then
  adopt permit-derivation for a compiler-enforced totality check **after** #95 seals.
- Capability interface set: FILLED — `Rotatable`, `Timed`, `Watchable`, `QuickEditable` (move the
  `instanceof Memory` timing branch into `Timed`); open edge: also make `canChange/change` an
  `Editable` interface and delete the `Wire/WireEnd` special-case.
- Orientation unification casing: FILLED — unify on **`JLSInfo.Orientation` (UP/DOWN/LEFT/RIGHT)**;
  saved values are already uppercase (spec §7) so the switch is **byte-identical on save**; delete the
  lowercase `Gate.Orientation` in-memory duplicate.

### #79 — Save-format stewardship
- Does decoupling tags from class names break compat / need a version bump? FILLED — **No** (X2): the
  canonical write-tag stays frozen; the alias table replaces `Class.forName`; no old reader sees a new
  tag ⇒ no bump. (The `edu.mtu.cs.jls`→`jls` rename already survived this way.)
- Protecting the format across #94/#95/#77 renames: FILLED (X2 coordination rule) — **land #78 registry
  + #79 alias table before any rename touches a persisted element.**
- Legacy-4.1 reader characterization (P3) a ship gate? FILLED — **no**; the header already shipped ahead
  of it, and P3 is blocked only on sourcing an authentic 4.1 jar. **Move P3 to #56's corpus work.**
- `TestGen`/non-conformant tags: FILLED — register as **loadable-but-not-writable** (a `synthetic`
  flag on `ElementType`) so the registry-integrity test doesn't demand icon/palette/help entries.

### #81 — Notification consolidation (substantially done)
- Reporter API/headless selection: FILLED (implemented — `TellUser` verbs; `interactive() = !batch &&
  !headless`; stderr + safe defaults).
- **Adopt #58's structured `Message`, or is plain `String` final?** NEEDS-MAINTAINER (low). Recommend
  **leave as-is**: `LoadError` (structured, core, load path) + `TellUser` (string, GUI, elsewhere) is a
  clean two-channel split under #77; close the #58-integration checkbox as satisfied by `LoadError`.
- Residual: batch "safe defaults" (`confirm`→no, `prompt`→null) can silently proceed where a hard error
  might be more correct. FILLED recommendation — audit the `confirmOrCancel`/`prompt` sites reachable in
  batch as a small follow-up; don't block closing #81.

---

## 4. Editor UX — #86, #84, #74, #73, #75, #76

State: `SimpleEditor.java` is now **4,477 lines** (issues cite 4,119); 9-state enum at `:495`; undo
depth hardcoded `10`; `circuitsize=1000`; zero `java.util.prefs`; KeyPad half of #86 landed. All
`SimpleEditor.java:NNNN` citations below have drifted — re-derive at implementation time.

### #86 — KeyPad / subcircuit / context menus (KeyPad P1–P3 done; do NOT reopen "global right-click hook")
- **Subcircuit tab: discoverability-only vs true read-only inspect?** NEEDS-MAINTAINER. Owner comment-3
  narrows it to "discoverability, not behavior." Recommend **(a) keep `setEnabled(false)` + a
  dialog-adjacent banner on the parent tab**; spin true read-only inspection into a follow-up.
- **Legacy right-click gestures when the context menu claims BUTTON3?** NEEDS-MAINTAINER. Recommend
  **migrate gestures to menu items + suppress the menu mid-gesture** (`placing/moving/startwire/drawire`);
  build it inside #84's extracted state machine.
- Menu contents: FILLED floor (rotate/flip/delete/properties via #75 Actions); open edge — build the
  menu **per-hit-test from element capability traits** (#78 descriptor) so only applicable actions show.
- Blocked? FILLED — remaining #86 work depends on #75's Action layer; the subcircuit-banner half is
  independent and can land now.

### #84 — Decompose SimpleEditor (target structure decided; each extraction = 1 PR)
- **Undo cost model: keep 10 whole-circuit snapshots or move to delta?** NEEDS-MAINTAINER,
  measurement-gated. Recommend the gate: **keep unless median snapshot > ~16 ms or undo-stack memory
  > ~50 MB at the 95th-percentile classroom circuit**; record the number in the PR either way.
- Done target: FILLED — **`SimpleEditor.java` < ~1,500 lines**, no test regressions; flag: file is
  4,477 lines now, re-baseline the §8 line table.
- **State machine behind what interface?** NEEDS-MAINTAINER. Recommend **state-per-class** (`State`
  interface with `Idle`/`Placing`/`Moving`/… impls) — makes #75's keyboard modes purely additive and
  kills the giant `switch`; unify the two duplicated Esc paths into it.
- Undo depth pref: FILLED — extract `UndoManager` now, read depth from #76's preference, degrade to
  `JLSInfo.undoStackDepth = 10` until #76 lands.
- Sequencing: FILLED — after #43/#74/#75 (same `SimpleEditor:1295-3200` region); consume #75 Actions;
  host #86.

### #74 — Zoom & canvas growth (owner APPROVED gestures: wheel=scroll, Ctrl+wheel=zoom-at-cursor, mid/space-drag=pan, range [0.25,4.0], retire grow button, `Viewport` owned here)
- **Zoom step granularity/increment?** NEEDS-MAINTAINER. Recommend **(c) continuous ~1.15–1.2× per
  wheel notch, keyboard snapping to a fixed ladder** (so Ctrl+0 → exactly 100%, and #91's screenshot
  grid lands on predictable percentages).
- **Exact zoom keybindings?** NEEDS-MAINTAINER (co-design with #75). Recommend **Ctrl/Cmd+= in, +− out,
  +0 reset, +9/Fit**, in a View menu owned jointly with #75.
- **Does the canvas become unbounded? auto-shrink?** NEEDS-MAINTAINER. Recommend **(a) unbounded
  auto-grow with a generous margin, no shrink, saved coords stay absolute** (byte-identical files);
  fit-to-circuit is the "reset the view" affordance instead of shrinking the model.
- Pan gesture: FILLED — **both** space-drag and middle-drag (owner).
- Fractional-zoom hit-testing: FILLED — **hit-test in model space only** (invert the transform at each
  mouse entry); scale fonts/strokes with the `AffineTransform`; legibility pass at 25%/400%.

### #73 — First-run onboarding (owner: samples from MTU site; Edit menu→#75; tutorial already resizable)
- **Which specific sample circuits?** NEEDS-MAINTAINER (spread decided; final pick open). Recommend
  **full adder, N-bit counter, mux demo, a subcircuit-instantiating circuit, a traffic-light/vending
  FSM**, under `resources/samples/`, via File→Open Sample, each with a header Text element; must load
  on a clean install (depends on #34).
- Empty-state panel: FILLED — Swing panel in the tab area (New / Open sample / Tutorial / Open recent);
  **omit "Open recent" until #76** lands; presence → #91.
- **Tutorial refresh scope: minimal nav here vs the M5 web-docs rework?** NEEDS-MAINTAINER. Recommend
  **minimal here** (next/prev + larger initial dialog size + applet/`isDemo`/orphan-`tutorial.html`
  cleanup); explicitly **defer** the "should docs be versioned web pages" question (audit M5) out of #73.
- README screenshots: FILLED — capture **after #76's theme lands** (so they don't rot); two shots +
  overview + honest positioning paragraph + tutorial pointer.

### #75 — Keyboard operability (owner adjudicated 5 pts: keep old bindings as aliases day-one; menu/accel L&F-independent; `:483-484` is `matchJump` not rotate; #75 owns the Action layer + Edit menu; keyboard-flow→#91)
- **Exact accelerator/mnemonic scheme + what wire-start rebinds to.** NEEDS-MAINTAINER (one real hole).
  Recommend adopting the proposed set (N/O/S/Shift+S; W=close with old meaning aliased; Q=quit; redo
  Shift+Cmd+Z mac / Ctrl+Y win; rotate R/Shift+R; flip F) and **binding wire-start to `E`**; assign
  non-colliding menu mnemonics **F/E/L/V/S/H**.
- **Focus model: remove focus-follows-mouse, or keep it as an augmentation?** NEEDS-MAINTAINER (the
  task's explicit question; the adjudication did *not* rule on it). Recommend **(a) remove the
  `mouseEntered` focus grab, make the canvas focusable on tab-select/click, and route global
  accelerators via `WHEN_IN_FOCUSED_WINDOW`** so shortcuts fire regardless of pointer location; audit
  dialog modality (those bindings can fire while a dialog is up).
- **Is the deep half (H2: keyboard placement + arrow-nudge + port-to-port wiring) in scope, or ship H1
  first?** NEEDS-MAINTAINER. Recommend **ship H1 (menu bar + accelerators + focus fix + rotate/flip)
  first**, deliver keyboard place/move/rotate/simulate if cheap, and **gate full keyboard wiring behind
  a feasibility spike** (the model may lack a port-selection metaphor — the issue's own refutation clause).
- Canvas a11y scope: FILLED — full screen-reader support for the **canvas** is **out of scope**; this
  issue delivers menu/dialog a11y only.

### #76 — Visual ergonomics / HiDPI / L&F / dark / prefs (owner adjudicated: blue/orange CVD palette + classic selectable; system L&F default + dark + `java.util.prefs`; drop the "renders tiny" HiDPI premise; sequence #76→#74→#75)
- **Dark-mode mechanism: OS-following vs explicit preference, and how chrome (L&F) and canvas (`Theme`)
  stay in agreement.** NEEDS-MAINTAINER. Recommend **one `theme` preference {system, light, dark} that
  switches the FlatLaf variant and the `Theme` canvas variant together**; "system" auto-detects. Robust
  even where GTK exposes no clean dark signal.
- **Default L&F per platform given GTK's roughness.** NEEDS-MAINTAINER. Recommend **try system L&F on
  all three OSes, fall back to FlatLaf on failure, log which was used**; ship **FlatLaf as the Linux
  default only if the CI-Linux screenshot audit shows GTK breaking a dialog**. Delete the
  `System.exit(1)` on L&F-load failure (make it a fallback).
- **Preferences storage: `java.util.prefs` vs a visible properties file.** NEEDS-MAINTAINER. Recommend
  **`java.util.prefs`** (approved, OS-native backend, no new pathing code, sandbox fallback already
  contemplated). Persist: theme, colors, undo depth, last directory (dep #34), recent files, window
  geometry, per-window zoom.
- Second visual channel: FILLED — value wires thicker, Hi-Z dashed, touching endpoints a distinct glyph
  (color-independent); minor open edge — resolve the exact thick-vs-dash mapping empirically against a
  deuteranopia/protanopia screenshot-filter run.
- Settings dialog scope: FILLED — theme, colors, undo depth, recent files, last directory; `Theme` is a
  record of semantic roles with light+dark variants.

---

## 5. Code-signing & distribution — #136, #135, #134, #128, #82

Cross-cutting (X4): single-maintainer custody. **Sequencing hazards:** #134/#135/#136 each edit the
same `installers` job in `release.yml` (second-to-land rebases); **#128 and #135 edit the *same* macOS
steps and are mutually exclusive — they must not both merge**; their README hunks (all rewriting #82's
"ship unsigned / right-click Open" text) collide too.

### #136 — GPG signatures for rpm & AppImage
- **Key-custody model (explicit blocking gate): offline primary + CI signing subkey / manual
  maintainer-side signing / Sigstore-only status quo?** NEEDS-MAINTAINER. Recommend **(a) offline
  certify-only primary + passphrase subkey in `RELEASE_GPG_KEY`/`RELEASE_GPG_PASSPHRASE`**. (b) is
  self-defeating — `rpmsign`/appimagetool mutate the file *after* CI attests/checksums it, so a
  re-uploaded manually-signed artifact fails `gh attestation verify`. (c) is a legitimate close-with-
  rationale outcome (a long-lived key is a real solo-project liability) but leaves the native-verify gap.
- Key algorithm: FILLED — **RSA-4096** (rpm client-side EdDSA verify only since 4.16/2020; ed25519
  can't be imported on EL8/SLES15/AL2). Choose ed25519 only if EL8-era clients are out of scope.
- Dry-run signing gate (secret-presence vs literal `push`): NEEDS-MAINTAINER — recommend
  **secret-presence gate** (rehearses import→sign→verify without publishing).
- Subkey expiry (none vs 2y): NEEDS-MAINTAINER (minor) — recommend **no expiry** for a solo project.
- deb per-file signing: FILLED — **out of scope permanently** (Debian signs repo metadata, not `.deb`s;
  `dpkg-sig` is dead). A signed apt/dnf repo is separate future work.

### #135 — macOS Developer ID signing + notarization
- **Apple Developer Program enrollment ($99/yr) — pay or not?** NEEDS-MAINTAINER (hard blocker; the
  only path — no free tier, no third-party signer). Recommend **enroll if macOS is a supported
  platform**, else close #135/#128 won't-fix with that rationale.
- **Notary credential type: App Store Connect API key vs Apple-ID + app-specific password?**
  NEEDS-MAINTAINER (**conflicts with #128**, which chose the other). Recommend **API key**
  (notarytool-preferred, 2FA-independent).
- **Signing method: jpackage-native `--mac-sign` vs #128's manual inside-out `codesign` + hardened
  runtime?** NEEDS-MAINTAINER. Recommend **try `--mac-sign` first, #128's sequence as the documented
  fallback** if the CI verify (`codesign --verify --deep --strict` / `spctl --assess`) fails. Caveat:
  the bsiever fork found `--mac-sign` omitted `--options runtime` → notary rejection (on JDK 21; may be
  a 21→25 delta — genuinely unproven).
- **Disposition of #128 vs #135.** NEEDS-MAINTAINER. Recommend the **split** (#135 owns pipeline
  plumbing; #128 owns the inside-out contingency + on-hardware Gatekeeper evidence + the `experimental`
  flip), then close #128 as superseded once the first signed release verifies.
- README publisher identity string (CN/Team ID): NEEDS-MAINTAINER — unknowable until enrollment.
- README timing: NEEDS-MAINTAINER (minor) — recommend **hold the whole PR until enrollment** (else a
  release could ship an unsigned dmg while the README claims signed).
- `notarytool --wait --timeout 30m`: FILLED — reasonable, keep generous.
- Keychain password (ephemeral vs stored secret): FILLED — **ephemeral per-run** (one fewer long-lived
  secret).
- Intel x86_64 leg: FILLED — out of scope; a future `macos-15-intel` leg inherits the path.

### #128 — macOS sign/notarize (bsiever fork's proven sequence)
- Shares the enrollment / credential-type / signing-method / disposition decisions with #135.
- **Hard-fail on an unsigned tag push, or ship a marked `*-unsigned.dmg`?** NEEDS-MAINTAINER. Recommend
  **marked-now, hard-fail-later** (flip to hard-fail on `push` when `HAS_MAC_CERT != 'true'` once signing
  is proven).
- **`experimental: true` flip — when and on whose evidence?** NEEDS-MAINTAINER (genuine dependency, not
  a code choice): requires a real dmg **installed on Apple-silicon hardware** + the observed pre-fix
  Gatekeeper refusal (P1) — impossible in CI. Blocks the DoD and #82's macOS verification debt.
- Secrets doc location (workflow comment vs `RELEASING.md`): NEEDS-MAINTAINER (minor) — inline now;
  create `docs/RELEASING.md` if the secret set stabilizes across #134/#135/#136.
- JVM entitlements file (`jls.entitlements`): FILLED (informational) — mandatory only if the inside-out
  path is chosen; jpackage-native adds no file.

### #134 — Windows Authenticode
- **SignPath.io OSS tier vs Azure Trusted Signing?** NEEDS-MAINTAINER (§7 first checkbox). Recommend
  **SignPath OSS** (free; publisher "SignPath Foundation" known pre-enrollment; service holds the key;
  its "must build from public CI" rule is already met). Azure ≈ $10/mo, own legal name, needs a tenant +
  identity validation. Classic OV/EV certs are rejected (post-2023 rules force the key into an HSM/token).
- **SignPath approval mode (manual-per-request vs auto).** NEEDS-MAINTAINER. Recommend **auto-approval**,
  else the action times out (600 s) on an unattended release.
- **`verify-windows-signatures` strict exactly-two-msi gate vs experimental-leg tolerance.**
  NEEDS-MAINTAINER. Recommend **keep strict** (turns a silently-absent installer into a red job).
- README fallback clause for pre-signing releases: FILLED — **keep** it until unsigned releases age out.
- Unsigned intermediate artifact retention: NEEDS-MAINTAINER (minor) — recommend `retention-days: 1`.
- Dry-run `force-sign` `workflow_dispatch` boolean: FILLED — adopt as drafted.

---

## 6. GUI / Wayland / UI testing — #101, #100, #91, #111

### #101 — Wayland CI rig (rig + `gui-wayland` lane exist; `JBR_SHA256` fill + first run already tracked)
- **No minimal-Swing control program though §9's falsification requires one.** NEEDS-MAINTAINER.
  Recommend an **always-on `HelloSwing` control step alongside JLS** so every run self-diagnoses a
  JLS-startup defect vs an upstream JBR/sway blocker.
- **P2 pixel-diff is computed but not gated (the DoD says implemented).** NEEDS-MAINTAINER. Recommend
  **setting the threshold from the first green run's actual AE value** (~10% of observed), not the blind
  1% guess.
- **Promotion to a required check — nothing schedules the "20 consecutive runs."** NEEDS-MAINTAINER.
  Recommend **adding a nightly `schedule:` cron** running only `gui-wayland`.
- weston fallback (named in §9, not wired): NEEDS-MAINTAINER (low) — add **reactively**, only if sway
  is shown to be the blocker.

### #100 — Wayland-native tracking (code sub-issues #102–#105 CLOSED; only #101 open)
- **Physical-desktop spot-check (§10) has no owner/gate/hardware.** NEEDS-MAINTAINER. Recommend a
  **maintainer-run scripted checklist on one real Wayland desktop (Mutter/KWin) per release**, recorded
  on the issue (CI can't cover it).
- **P3 matrix (Win/mac/X11 unchanged) — who runs it, what counts as pass?** NEEDS-MAINTAINER. Recommend
  **sequencing the P3 sign-off after #111** (so the Windows leg rests on a green suite) + manual GUI
  smoke; file macOS/X11 GUI-CI as separate scope.
- **#82 handoff: the bundled Linux runtime must ship WLToolkit (only JBR has it; mainline OpenJDK
  doesn't).** NEEDS-MAINTAINER. Recommend **surfacing this on #82 now**: default to **bundling JBR if
  its license permits redistribution**, else an "install JBR" story until mainline lands.

### #91 — Automated UI test harness (Layer 1 already built & green)
- **Layer-2 framework: AssertJ-Swing vs `java.awt.Robot`?** NEEDS-MAINTAINER. Recommend **`Robot` +
  `SwingUtilities.invokeAndWait`** (the issue's own documented fallback) given the JDK-25 baseline and
  AssertJ-Swing's dormancy; keep a `dispatchEvent`-only tier as the flake retreat.
- **Which display backend runs Layer-2 — Xvfb (X11) contradicts the #100/#101 "no X11" program.**
  NEEDS-MAINTAINER. Recommend **both lanes initially, converging to the #101 sway+JBR rig** (Xvfb+Robot
  is the pragmatic first mover; the Wayland rig is where WLToolkit interaction bugs surface). Reconcile
  the contradiction explicitly in the issue (it currently reads Xvfb-only).
- Layer-2 sequencing after #84: FILLED — honor the stated order (Layer 1 done; Layer 2 not before #84's
  state-machine extraction).
- **Flakiness quarantine policy (P5) rules are unwritten.** NEEDS-MAINTAINER. Recommend
  `@Tag("ui-interaction")` **excluded from the required gate + "one flake → quarantine + issue"**,
  reusing #101's 20-run bar to promote.
- P6 pilot (which UX issue converts a manual criterion first): NEEDS-MAINTAINER (minor) — recommend
  **#75** (menu accelerators; no rendering layer needed).

### #111 — Windows test-suite failures (all three reproduced)
- **CRLF round-trip.** Mechanism FILLED (`Circuit.save` uses `PrintWriter.println` → CRLF on Windows;
  test `canonicalize` splits on `\n`; and there is **no `.gitattributes`**). Fix choice
  NEEDS-MAINTAINER — recommend **(a) make the writer always emit `\n`** (the only option that makes real
  Windows saves match Linux and honors the format's reproducibility spec) **+ `.gitattributes` LF** for
  resource fixtures; test-only normalization would hide the real byte-divergence.
- **Leaked file handle blocking `@TempDir`.** Mechanism FILLED (`FileAbstractor.readText`/`readXZ` return
  a `Scanner` over a live handle; the test never closes it → Windows can't delete; the zip path already
  drains to memory and closes). Fix location NEEDS-MAINTAINER — recommend **(a) fix the load path**:
  read the bounded payload into memory and close the handle before returning (mirror `readZip`; the
  64 MiB cap already bounds it), which fixes real Windows users too.
- **Geometry baseline drift.** Diagnosis FILLED (the `TriState "invalid element"` lines are expected
  rejects / a red herring; the real drift is the font-metric-dependent Binder/Splitter/Constant entries).
  Fix NEEDS-MAINTAINER — recommend **(a) pin a bundled font via `Font.createFont`** (CI already installs
  `fonts-dejavu-core`); fall back to splitting the baseline into exact vs tolerance sets.
- **Scope/sequencing — re-enable the Windows suite as a gate, in what order vs #100 P3?**
  NEEDS-MAINTAINER. Recommend **land the three fixes, add a `windows-latest` advisory matrix leg, promote
  to required once green** — this also unblocks #100's P3 "Windows unchanged" record.

---

## 7. HDL interoperability — #59, #61, #62, #63

Stage-1 export shipped. Settle the **Memory-timing / x/z-coercion pair once** (X3).

### #59 — HDL umbrella
- Non-synthesizable export policy: FILLED (resolved in code — `HdlExporter.java:364-388` +
  `HdlPolicyTest`): warn-skip = Display/SigGen/Pause/Stop/Text/TestGen; topology = Wire/WireEnd/Jump;
  reject = SubCircuit/Memory/Mux/Decoder/StateMachine/TruthTable.
- **SubCircuit handling — rejected today, but classrooms are heavily hierarchical.** NEEDS-MAINTAINER.
  Recommend **(c) module-per-subcircuit-type + instantiate** (matches the research intent); or **(b)
  flatten inline** as a fast interim reusing the single-module walk.
- **Synthesizable elements still in the reject bucket (Mux/Decoder/StateMachine/TruthTable/Memory).**
  NEEDS-MAINTAINER. Recommend **(b) add the combinational four now** (case/casez templates), **defer
  StateMachine** (state-encoding choices) **and Memory** (timing — ties to #61-Q1).
- Target dialects: FILLED — Verilog-2005 structural + VHDL (ghdl `--std=93/02` verified);
  SystemVerilog/SystemC out of scope.

### #61 — Stage 2 Yosys import
- **Memory async-read timing** (accessTime defaults to 100 units; async ROM is zero-delay → waveform
  parity fails). NEEDS-MAINTAINER (X3). Recommend **import Memory with `time 0`** + a zero-delay-read
  parity test; fall back to **batch-only parity with a documented divergence**. Settles #59's Memory
  sub-decision too.
- **Clocked / multi-port memories** (JLS is async single-port). NEEDS-MAINTAINER. Recommend **reject
  with a teachable message at launch**, add an **opt-in `memory_map` bit-blast with a hard size cap
  (≤256 words)** later.
- Register async-clear pin vs keep `$adff` rejected: FILLED (#61 comment) — **keep `$adff` rejected**,
  corpus-evidence-driven, deferred (`Register` has only D,C).
- x/z coercion: FILLED — `x`→0, `z`→disabled TriState; keep identical to #63 (X3).

### #62 — Auto-layout companion
- **ELK relicensing checkpoint (`eclipse-elk/elk#1185`, milestoned ELK 0.12.0).** FILLED procedurally /
  CONTINGENT outcome: the **binding rule** is to re-check elk#1185 + the 0.12.0 release state **before**
  hand-rolling; if ELK has shipped with a GPLv3 secondary license, **link ELK directly** and shrink #62
  to JLS-side geometry realization; only hand-roll if 0.12.0 arrives without the relicense or stalls.
  **The maintainer must check `github.com/eclipse-elk/elk/issues/1185` at implementation date**
  (unreachable from this session; opened 2026-03-09, milestoned 0.12.0). This is the single most
  cost-inverting checkpoint in the HDL workstream.
- Heuristic-vs-ELK escalation boundary: FILLED — build the hand-roll behind a fixed interface; escalate
  to an **out-of-process** ELK runner if it fails the quantified rubric (0 overlaps hard-fail; ≤0.5
  crossings/net; ≤2.0× Manhattan wire-length; ≥90% L-to-R; ≤4× bbox).
- **Three showcase golden layouts — exact circuits/author/committed location.** NEEDS-MAINTAINER
  (narrow). Recommend committing the **ALU-slice, counter, and small-FSM** as authentic hand-drawn `.jls`
  goldens under `test/resources/hdl/layout-goldens/`, curated by whoever lands #62.
- Cycle/feedback + grid invariants: FILLED — break feedback at register outputs, route back-edges;
  12-px grid, WireEnds at exact port offsets, round-trip identical; import arrives as one undoable
  pre-selected unit.

### #63 — Stage 3 co-simulation
- **Combinational-path-through-black-box fixpoint iteration (the hardest problem, under-specified).**
  NEEDS-MAINTAINER (settle before building transport). Recommend **(a) delta-cycle to convergence
  reusing JLS's own post/react settle loop** (each subprocess exchange pure), bounded cap → oscillation
  error; add loop **detection as a diagnostic**; cache identical-input exchanges to bound latency (P3).
- **Co-sim transport (subprocess lockstep vs VPI/DPI vs cocotb — a written comparison is mandated).**
  NEEDS-MAINTAINER. Recommend **(a) per-component GHDL/Icarus subprocess over stdin/stdout behind a
  transport interface**, documenting why VPI (native build + JNI) and cocotb (Python runtime) are
  rejected.
- x/z coercion into 2-state: FILLED — adopt #61's rule verbatim (X3), with a per-component warning.
- Port-scan fallback + failure/lifecycle surface: FILLED — scanner defers to Yosys `write_json`, never
  becomes a parser; the addendum fully specifies the states, content-hash policy, ~5 s timeout,
  no-zombie reaping, and the editable-without-tools guarantee.

---

## 8. Test gaps & release tracking — #56, #33

Context: v5.0.0–v5.0.4 shipped; #33 is 24/26 sub-issues closed (only #56, #59 open); most of #56 has
landed. Test count is now **65 files / ~399 `@Test`** (not the body's "70 tests/12 classes").

### #56 — Test-suite gaps
- **Authentic JLS 4.1 corpus — source, owner, and (missing) fallback.** NEEDS-MAINTAINER. Source is
  adjudicated (MTU/Poplawski + GVSU/Kurmas) but **not in the repo**; the only committed fixture
  (`fork-4.6-shiftregister.jls`) is this fork's own writer output, which §10 forbids as a legacy proxy.
  Recommend **(a) download the MTU 4.1 `JLS.jar`, author circuits across all container formats (incl. an
  `orient 0` Display), commit under `test/fixtures/legacy-4.1/` with provenance + license**, and
  **write GVSU/Kurmas in as the explicit fallback** the issue currently lacks; don't let the synthetic
  interim silently become permanent.
- **Is the corpus still a blocker?** NEEDS-MAINTAINER — **no**: all its stated consumers (#57/#38/#79/
  #60/#61) shipped without it via synthetic fixtures. Recommend **downgrading it to a non-blocking
  hardening task**.
- **Editor-smoke coverage is gated on #91's nonexistent Layer 2.** NEEDS-MAINTAINER. Recommend
  **closing #56 down to the corpus and moving editor-smoke coverage into #91's Layer-2 acceptance
  criteria** (popup smoke must dispatch the `Action` directly, not a key event — per #37).
- `HelpTopicsTest` source-scan: FILLED — **drop** (obsolete; it already walks the help tree).
- Meta: #56's body is stale/mostly satisfied — NEEDS-MAINTAINER: **rewrite to its two live residuals or
  close + reparent**; fix the "70/12" figure and check off the landed items.

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

## 9. The gaps you actually need to fill (NEEDS-MAINTAINER quick-list)

Ordered by leverage. Everything not here is FILLED above and just needs your ✔.

**Money / accounts / custody (external, blocking):**
1. #135/#128 — Apple Developer Program enrollment ($99/yr): yes/no.
2. #134 — SignPath OSS vs Azure Trusted Signing (+ SignPath auto-approval).
3. #136 — GPG key-custody model (offline subkey / manual / Sigstore-only).

**External state to check at pickup:**
4. #93 — is there an Error Prone/NullAway release supporting JDK 25? (else annotation-only slice first)
5. #62 — has `eclipse-elk/elk#1185` shipped the GPLv3 relicense in ELK 0.12.0? (link ELK vs hand-roll)
6. #128 — Apple-silicon hardware run to observe Gatekeeper + flip `experimental`.

**Bookkeeping / re-scope (do these to unclog the tracker):**
7. #33 — reconcile ship policy with the shipped release, then close; reparent #56/#59 as post-1.0.
8. #56 — source-or-formally-defer the 4.1 corpus with a written fallback; move editor-smoke to #91.
9. #33 — create the severity/phase label taxonomy + milestone; close #80's deprecation window.

**Genuine design choices (medium):**
10. #94/#95 — the `SimEvent` record's hand-written `equals`/`hashCode` + the `todo`-sealing blast radius.
11. #77 — `InteractiveSimulator` core-vs-GUI; the elem model/render split + `TextMetrics`.
12. #75 — remove focus-follows-mouse (yes/no) + the final accelerator/mnemonic table + wire-start key.
13. #74 — zoom step model + keybindings + unbounded-canvas/no-shrink policy.
14. #76 — dark-mode source (system/light/dark pref) + per-platform L&F default + prefs backend.
15. #86 — subcircuit discoverability-only vs true read-only; right-click gesture migration.
16. #59/#61 — SubCircuit export strategy; which synthesizable elements leave the reject bucket; Memory
    `time 0` vs batch-only (X3).
17. #63 — the black-box fixpoint algorithm + the transport choice (subprocess vs VPI vs cocotb).
18. #111 — writer-emits-`\n` vs test-only normalization; load-path vs test-only handle fix; font pinning.
19. #91 — Layer-2 framework (Robot vs AssertJ-Swing) + display backend (Xvfb vs Wayland rig).

**Small / low-stakes:** #96 build-budget baseline & 26-leg policy; #101 control-program / P2 threshold /
nightly cron; #100 physical-desktop check owner; #81 keep string API vs #58 `Message`; #136 subkey
expiry; #134 artifact retention; #135 README timing & publisher string; #73 final sample-circuit pick &
tutorial-nav scope; #62 the three golden layouts.
