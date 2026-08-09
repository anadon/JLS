# Issue #355: FEAT-011 (RESIDUAL): a screen reader reports a circuit rather than one opaque panel, and a first run offers a next move — keyboard operability itself already ships
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Stripped of the machine block, the claim is: *a blind student should be able to read a
circuit back, and a first-time user should not land on a blank canvas.* Both are right,
both serve the project's stated arc (an educational tool a course can assign), and the
first one is genuinely the difference between passing and failing a procurement review.
I endorse the goal without reservation.

What I am rejecting is the vehicle. This issue commits the project to the full Java
Accessibility API canvas scene model as *the* headline deliverable, at a 6–10 mw band,
and gates its own close on `git grep -c "getAccessibleChildrenCount" src/` returning
non-zero. The project already wrote down, in its own tree, why that is the wrong route —
and this issue does not cite the document that says so.

## The tree already contains the counter-proposal, and #355 pulls against it

`docs/standards-adoption/03-accessibility-conformance.md:412-444` walks the exact design
#380 §7 specifies — override `getAccessibleContext()` on the canvas inner class, one
lightweight `Accessible` proxy per element, `AccessibleRelation.CONTROLLER_FOR` /
`CONTROLLED_BY` for netlist topology, `AccessibleSelection` mirroring the editor
selection — prices it at **8–15 maintainer-days plus permanent per-element maintenance**,
flags "a genuine risk that java-atk-wrapper surfaces none of it to Orca", and then says:

> **Recommendation: do not build it as part of this project.** Instead, build the cheap
> equivalent that works with all three AT stacks *today*: a keyboard-reachable circuit
> outline view — a `JTree` or `JList` of elements, their names, their values, and their
> connections … ~3–4 days … Gate the full JAAPI canvas tree on an actual user request.

Its cost table (`:762-764`) lists the outline view at 3–4 days and marks the full JAAPI
scene model *(Rejected)*. Its closing priority call (`:817-818`): "If you only have budget
for one, **build the outline view and fix the contrast** — that helps an actual blind
student; the ACR only helps a procurement officer."

#355 and #380 are that rejected option, re-filed as required work, with the rejection
never mentioned. That is not a disagreement I can resolve for the maintainer — but a
feature whose whole premise was adjudicated against in-tree must argue with that
adjudication, not route around it.

## The prerequisite the issue never names, and which invalidates its own criteria

`scripts/build-installer.sh:145` still reads
`MODULES="$(jdeps --print-module-deps --ignore-missing-deps "$JAR")"`. `jdeps` reports
static dependencies; nothing in the jar references `jdk.accessibility`, so it can never
appear. **Every `.msi` this project ships bundles a runtime with no Java Access Bridge.**
`grep -rn AccessBridge test/ src/ scripts/` returns nothing at HEAD, so the pin that
document proposes was never added either.

Consequence for this feature specifically: land the entire 6–10 mw scene model exactly as
specified, and NVDA and JAWS users of the primary Windows distribution hear precisely what
they hear today — nothing. On Linux the bridge is `java-atk-wrapper`, not in the JDK, and
a jlink'd image cannot pick up the distro's copy. #380 §11 concedes this ("A passing
accessibility test is not a working screen reader") and then makes the concession
non-binding: the completion gate is a `git grep` count that a stub satisfies, plus one
manual pass on one unnamed platform. The feature's §2 says it was cut *by verification
substrate*. The substrate it chose measures the Java-side API and cannot see the only
thing that determines whether a student hears anything.

## The reframing: one projection, three consumers, and a feature sighted users also want

JLS already maintains three total, tested projections of the element vocabulary: the save
writer (`ElementRegistry`/`SaveTags`, totality enforced by `ElementRegistryTest`), the HDL
model (`HdlExporter.buildModel` → `HdlModel`: named instances, nets, port directions,
connectivity — *literally* name + role + relations, minus geometry), and the GUI renderer
registry (`ElementRenderers`). #380 proposes a fourth per-element table with its own
totality test and its own permanent maintenance tax on every new element type.

The elegant cut is one level lower: a headless **`CircuitDescription`** projection — per
element a stable id, a display name, a type label, a bounds, a current value, and its
driven/driving neighbours — derived once from `ElementRegistry` + `SpatialIndex` +
`WireNet`, in the same spirit as `HdlModel`. Then:

1. **The outline view** (`JTree` over that projection) is the accessible surface. Stock
   Swing, so AT-SPI/UIA/NSAccessibility carry it for free on all three platforms with zero
   bridge-specific code, and 508 **E101.2 Equivalent Facilitation** contemplates exactly
   this. It is also a *navigator* — the thing anyone editing a CPU-scale circuit on the
   `riscv/` trajectory (#200–#202) wants, and the natural host for search, "go to element",
   and the diff/merge lane (`docs/capability-roadmap/lf-06-diff-merge-vcs.md`). A feature
   sighted users open daily gets maintained; an API surface only a JUnit assertion ever
   reads rots, which is the fate `docs/…/03-accessibility-conformance.md` predicts for the
   proxy layer.
2. **SVG `<title>`/`<desc>`** on the existing `-i out.svg` export (§4 of the same document,
   0.5 days) — the circuit drawing acquires a text alternative on a path that already
   walks every element, and lab reports and hosted docs get it too.
3. **The JAAPI canvas tree, if it is ever funded**, becomes a thin adapter over the same
   projection rather than a second element table — and the Orca spike #737 can be run
   against the outline view first, cheaply, before anyone commits a band to the canvas.

That is the "different seam" answer: the accessible model is not a UI feature, it is a
*fourth backend on a projection JLS already knows how to build totally*. Cut there and the
35-type role-totality criterion stops being new work and becomes a property of an existing
registry walk.

## Disregarding the stated acceptance criteria, explicitly

I am setting aside three of this issue's Completion Criteria as measuring the wrong thing:

- `git grep -c "getAccessibleChildrenCount" src/` non-zero — satisfiable by a stub, and
  green on a Windows build where no AT can reach it.
- §5 criterion 1 (role totality over all 35 registered types) — right instinct, wrong
  home; it belongs to the projection, not to a canvas-specific mapping.
- §5 criterion 2 ("the same accessible child set under `DARK` as under `DEFAULT`") — this
  is the *one* cross-task assertion the issue offers as the reason the two halves are one
  feature, and §3 defeats it in the same breath: "the accessible model does not render."
  The assertion is a tautology. Nothing binds the two halves.

Replace them with: a dated AT session log naming tool and platform, run against a
*packaged installer* rather than `java -jar` on a full JDK; and an `AccessBridgeModuleTest`
pinning `jdk.accessibility` in the installer module set, the source-grep compensating
control this repo already uses in `KeyPadAccessibilityPinTest`.

## The container has already dissolved; finish the job

The 2026-08-08 comment strikes half the title: "a first run offers a next move" now belongs
to #550/#511/#548, better specified than here. What is left under #381 is a three-platform
FlatLaf scaling and screenshot matrix, which the comment itself concedes "sits oddly under
this feature's capability statement" and which collides with #289. Meanwhile the dark
variant — the other half of #381 — is blocked on the ~126 hardcoded-foreground sweep
(`src/jls/Theme.java:27-31`) and on #286, and does nothing for the contrast defect that is
live *today in the shipped default*: the #75 keyboard caret is drawn in `selectionColor`
`(240,240,240)` on `Color.white`, ≈**1.14:1**, against 3:1 required by 1.4.11. The keyboard
feature this whole issue calls "shipped" has an indicator a low-vision user cannot see.
A third theme variant is a worse use of a maintainer-week than fixing the first one.

So: retire #355 as a container. Re-home #380 under the CAP-26 accessibility line (#507/#544)
where its cross-generation consumer #739 already lives; re-home #381's matrix to #76's
residual or the display lane (#162/#91); let #550/#548 own onboarding, which they already do.

## Suggested ordering, if the goal is a student who can read a circuit

1. `jdk.accessibility` in the installer module set + `conf/accessibility.properties` +
   `Recommends: libatk-wrapper-java-jni` on deb/rpm, pinned by a test. ~1 day. Without
   this, everything downstream is invisible on the flagship distribution.
2. Canvas focus ring and the 1.4.11 contrast fixes in `Theme.DEFAULT`. ~2 days.
3. `CircuitDescription` projection + outline view. ~4–5 days. Run Orca/NVDA/VoiceOver
   against it; that session, not a grep, is the evidence.
4. SVG `<title>`/`<desc>`. ~0.5 days.
5. Record an `ARCHITECTURE.md` decision — "canvas scene model deliberately not exposed;
   equivalent facilitation via the outline view" — with the revisit trigger being a real
   user or institution asking, mirroring the i18n and look-and-feel entries.
6. Fund the JAAPI canvas tree only if (5)'s trigger fires or #737 comes back green, and
   build it as an adapter over the projection from (3).

That reaches the stated audience — the blind student, the instructor, the procurement
office — in roughly a quarter of this feature's band, and every step is verifiable by
something a user can hear.
