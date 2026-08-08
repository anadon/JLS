# Issue #544: FEAT-C26-3: a blind student builds and simulates a two-gate circuit by keyboard, hearing each element, each connection and each signal-state change as it happens
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## The claim, and what I accept of it

The claim is that JLS should become the first schematic simulator with a blind-accessible lab
path. I accept that without reservation. It fits the project's whole arc: an educational tool
whose differentiators are already *honesty artifacts* — the Okabe-Ito palette with a ΔE ratchet
(`src/jls/Theme.java`), the byte-pinned batch contract (`docs/batch-interface.md`), reproducible
builds, `docs/keyboard-a11y-verification.md`'s "red-on-break or it isn't evidence" discipline.
A blind lab path is the same move at a larger scale, and CAP-26 (#507) is right that no
competitor offers one.

What I am rethinking is the route, the seam, and the evidence gate. **I am explicitly
disregarding this issue's acceptance criteria as written** — all four — because the project has
already costed this exact design and recommended against it, in its own tree, at
`docs/standards-adoption/03-accessibility-conformance.md` (921 lines).

## The route this issue picks is the one the repo already rejected

§6 of that document, "The canvas disclosure", sizes precisely the design #544 + #380 implement:
override `getAccessibleContext()` on the canvas, one `Accessible` proxy per element,
`AccessibleRelation.CONTROLLER_FOR`/`CONTROLLED_BY` for topology, a navigation model layered on
the #75 caret. Its verdict, verbatim: *"Realistic cost: 8–15 maintainer-days ... plus permanent
maintenance on every new element type, plus a genuine risk that java-atk-wrapper surfaces none
of it to Orca. **Recommendation: do not build it as part of this project.**"* It then names the
alternative: *"a **keyboard-reachable circuit outline view** — a `JTree` or `JList` of elements,
their names, their values, and their connections, shown beside the canvas ... accessible for
free on Windows, macOS and Linux with no bridge-specific work. ~3–4 days."* — landing under
Revised 508 **E101.2 Equivalent Facilitation**, with the JAAPI canvas tree *gated on an actual
user request*.

The same document, under "External-tool validation with skip-when-absent", separately decides:
*"**Recommendation: do not add this to CI**"* for an AT-SPI/accerciser lane, citing the
`gui-wayland` lane's own history — twenty runs to earn promotion, an `UNVERIFIED-PLACEHOLDER`
checksum still in `ci.yml`, `PIXEL_DIFF_MIN` parked at 0 — as the honest cost of a fragile GUI
lane. #544's AC-1 makes exactly that lane the funding gate and the primary acceptance criterion.

Neither decision is cited or rebutted anywhere in #544, #380, #549 or #507. That is the finding:
the capstone's most expensive, most category-defining band is pointed at the route its own
research says not to take, on the evidence surface its own research says not to build.

## Reframe 1 — the accessible artifact is a *view*, not an annotation of the canvas

Ship a **Circuit Outline** view: a stock `JTree`/`JList` beside the canvas, one node per element,
connection children ("output → `and3` input B"), a live value column. Everything #544 wants
follows without inventing a mechanism:

- **Traversal** becomes tree navigation — a keymap every screen-reader user already owns, with
  nothing to teach, document, or keep in sync with `hotkeys.html`.
- **Connection context** is a child node, spoken by the platform peer with no relation-mapping
  work and no per-bridge behaviour to discover.
- **Live state** is a cell value change on a component whose peer already fires the right
  property events on all three bridges — the single riskiest thing in the issue becomes the
  thing you get by default.
- **It doesn't rot.** Sighted keyboard users, low-vision magnifier users and instructors use it,
  so it is exercised daily instead of being an AT-only surface nobody runs.
- **It lands outside `SimpleEditor`.** #380 O5 measures that class at 5,852 lines and warns that
  the scene model becomes "one more thing to move out" ahead of #84. A separate view is not.
- The "reduced announcement set" that #544 treats as a *degraded fallback requiring a named VPAT
  exception* becomes the primary design, honestly claimed.

## Reframe 2 — the graph walker already exists twice; do not write a third

`jls.hdl.HdlExporter.buildModel` → `jls.hdl.HdlModel` is already a **language-neutral structural
model of one circuit** — ports, internal nets, per-element statements, legalized names, renames
recorded — headless and golden-tested. That is the element graph with connection context this
issue proposes to reconstruct inside an `AccessibleContext`. Per-element phrasing has a seam too:
`Element.showCurrentValue`/`showInfo` (ARCHITECTURE.md's "Adding an element today", item 9) and
the batch watched-element report pinned byte-exactly by
`BatchSimulationGoldenTest.watchedElementsPrintInNameOrder`.

So the right first artifact is a **headless `CircuitNarrator`**: circuit → ordered traversal
transcript with names, roles, connection context and signal-change lines. Registry-keyed with a
totality test over `ElementRegistry.all()` (#380 §7.10 already demands exactly this shape), unit-
tested with golden files in the house style (`VcdExportGoldenTest`, `BatchSimulationGoldenTest`).
No display, no bridge, no Orca, runs in plain `mvn verify` on every platform forever.

This also collapses a duplication CAP-26 is currently paying for twice: PF-4's prose narrative
(3–4 mw) is a second walker over the same graph with the same per-element vocabulary. One
narration model funds both; the marginal cost of the tactile/prose export drops to formatting.

## Reframe 3 — the announcement stream is an op observer plus a sim listener, both headless

Every editor mutation already flows through one closed, validated, invertible, serializable
vocabulary with a **typed extension point of cardinality `many`**: `collab.op-observer` over
`jls.collab.op.OpSink` (`docs/operation-layer.md`, `docs/extension-points.md`). "Placed an AND
gate", "connected `or1` output to `not1` input" is a `CircuitOp` → sentence renderer registered
at that seam — not new plumbing inside the editor state machine. On the simulation side the
watched-element / `TraceSample` path is the existing signal-change feed.

Both sides then produce the same thing: a stream of sentences. The GUI's only remaining job is
to put those sentences where an AT already reads them. That decomposition moves most of the
5–8 mw band into headless, ratchetable, permanently-testable code and leaves a small irreducible
GUI tail — which is the difference between a band with a blow-out risk and one without.

## Reframe 4 — the evidence gate is aimed at the wrong platform, and certifies a config nobody ships

Repo evidence against Orca-in-the-#101-rig as the gate:

- **Linux Java→AT-SPI is java-atk-wrapper, not the JDK** — "thinly maintained", and *"a jlink'd
  bundled runtime has its own `conf/` and cannot pick up the distro's wrapper"* (§1). The shipped
  deb/rpm/AppImage therefore cannot reach Orca **at all**. A green CI lane would certify a
  configuration no student runs.
- **The rig is JBR + experimental `WLToolkit`.** JAW is an AWT-toolkit-level hook of X11 vintage;
  nothing in this tree evidences it under Wakefield. Meanwhile the one Linux configuration where
  Java a11y is known to work — X11 session, system JDK, `libatk-wrapper-java-jni` — is excluded
  by the README's "X11 is deliberately not part of this project's tooling", *even though* the
  display test substrate is in fact `xvfb-run` (X11) per `docs/keyboard-a11y-verification.md`.
  That contradiction should be resolved before 5–8 mw rests on it.
- **The spike as specified is unfalsifiable-for-us.** It can only fail for properties of JAW/JBR,
  not of JLS, and the recorded outcomes ("re-scope", "file the platform finding") are both
  "we learned something about someone else's stack".

Replace it with what §"Testing procedure" already calls *"the single most valuable artifact in
this project"*: `AccessibleTreeGoldenTest` — a byte-exact `depth | name | role | accessible-name |
focusable` golden that is simultaneously the regression guard and the ACR's evidence appendix —
plus a dated once-per-release manual AT pass in a new `docs/accessibility-at-checklist.md`,
styled on `docs/wayland-desktop-checklist.md`. Machine-verify the Java side; human-verify the
bridge side, per platform, recorded. That is precisely the "Evaluation Methods Used" a
procurement reviewer checks, and it is what #547's VPAT actually needs.

## Reframe 5 — the two things that decide whether this outcome is real are not in this issue

1. **`scripts/build-installer.sh` derives its jlink module set from `jdeps`, so
   `jdk.accessibility` can never appear: every shipped `.msi` bundles a runtime with no Java
   Access Bridge, and NVDA/JAWS get nothing at all from an installed JLS.** #544 relegates NVDA
   to "documentation plus a manual checklist" without noticing that the documented path is
   currently broken on the primary Windows distribution. One-line fix plus
   `AccessBridgeModuleTest`; ~1 mw. It is a *precondition* of this feature's NVDA claim, not a
   neighbour.
2. **The #75 keyboard caret is drawn in `selectionColor` = `(240,240,240)` on white ≈ 1.14:1** —
   the operability indicator this entire story stands on is invisible to a low-vision user, and
   `nonZero`/`watch`/`highlight` also fail 1.4.11.

Neither is owned by #544, #549 or #547 as filed. Pull (1) into this feature or make it an
`ordering_after` alongside #549; route (2) into FEAT-C26-1's palette work explicitly.

## On the #355/#380 boundary — the split is along the wrong seam

The pass-2 comment draws static-scene-model (#380) vs dynamic-traversal-and-speech (#544). But
#380 already specifies stable-id-keyed children, wires as `AccessibleRelation` src/dst pairs,
role totality over the registry, and `SpatialIndex`-backed queries. "Connection context" *is*
#380's relation set read aloud; "traversal" *is* arrow movement over #380's child order. On the
canvas-tree route there is nearly nothing left for #544 to own but the transport — two owners
maintaining one `AccessibleContext`. The seams that actually separate: **(i) description model**
(headless, registry-keyed, shared with PF-4) — **(ii) presentation surface** (outline view now;
canvas tree later, escalation only) — **(iii) live transport** (op observer + sim listener). Cut
there and #380 becomes optional rather than load-bearing.

## What I would keep verbatim

The anti-over-claiming discipline: NVDA documented not automated, exceptions named in the VPAT
rather than papered over, funding gated on a spike. That instinct is the project's best habit
and I am reusing it — my quarrel is only with what the spike spikes. Reframed: run the outline-
view prototype under Orca on a system JDK with `libatk-wrapper-java-jni` on a GNOME session,
**and** under NVDA on Windows with the bridge module present. One day, by hand, recorded. If a
stock `JTree` will not speak there, no JLS-side design will, and that is a finding worth having
before 5–8 mw is committed.

## The open question this feature should ask first

CAP-26 §1 step 2 asserts the outcome is "place, connect, simulate *on a grid, by keyboard*". A
blind engineer's native medium for circuits is structural text — and JLS already ships plain-text
`.jls` saves, Verilog export, and a Yosys-JSON import path (#61). It is genuinely unclear whether
the student wants to place gates on a grid or to author and understand a circuit without sight,
and the answer changes which 5–8 mw to spend. That belongs in #507's Open Question 1 user
validation, asked **before** the canvas-speech band is funded, not after.
