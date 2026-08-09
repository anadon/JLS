# Issue #380: TASK-0029: a screen reader reports each drawn element as a named accessible child, and the five gaps the keyboard checklist names get closed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

One sentence, from its own Intended Audience: **a blind student should be able to read a
circuit back out of JLS.** That end is correct, it is load-bearing for the whole CAP-26 band
(#544, #739, #741, #753, #754), and it is the difference between an instructor being allowed
to assign JLS and not. Nothing below disputes the goal.

What I dispute is that "override `getAccessibleContext()` on the canvas `JPanel`" is the route
to it. The repository already contains a researched answer to exactly this question, and it
says no. `docs/standards-adoption/03-accessibility-conformance.md:540-560` lays out the same
five-step JAAPI design this issue proposes — `AccessibleJComponent` subclass, one `Accessible`
proxy per element, `AccessibleSelection`, `CONTROLLER_FOR`/`CONTROLLED_BY` relations, a
traversal model — prices it at **8-15 maintainer-days**, and then writes:

> **Recommendation: do not build it as part of this project.** Instead, build the cheap
> equivalent that works with all three AT stacks *today*: a **keyboard-reachable circuit
> outline view** … Because it is a stock Swing component, it is accessible for free on
> Windows, macOS and Linux with no bridge-specific work. ~3-4 days. … Gate the full JAAPI
> canvas tree on an actual user request.

Issue #380 does not cite that document anywhere — not in §1 Background, not in §12 Related
Work, not in Open Question 5, which asks the very question §6 of that document answers with
numbers. **I am explicitly disregarding this issue's acceptance criteria**, because they
encode the rejected option and inherit three structural mistakes from it.

## Three things the issue gets structurally wrong

**1. The delivery gate is not #316 — it is the bundled runtime's missing bridge, and nobody owns it.**
`scripts/build-installer.sh:145` derives the jlink module set from `jdeps
--print-module-deps`. `jdk.accessibility` is a runtime-only module; nothing in the jar
references it, so it can never appear. Every `.msi` this project ships therefore bundles a
JRE **with no Java Access Bridge**, and Swing does not speak UIA — so NVDA and JAWS read
literally nothing from an installed JLS no matter how perfect the `AccessibleContext` is.
I searched: no open issue in the repo mentions `jdk.accessibility`. On Linux the bridge is
`java-atk-wrapper`, which the same document calls thinly maintained and which a jlink'd image
cannot pick up from the distro at all; on Wayland-native (a *supported* row in `README.md:176`)
there is no story whatsoever. So the highest-leverage accessibility change available — one
line appending the module, one `conf/accessibility.properties` write next to the existing
`clamp_mtimes` step, `Recommends: libatk-wrapper-java-jni` on the deb/rpm — is ~1 day, unfiled,
and **strictly prerequisite** to this issue producing any user-visible effect. This issue
spends 8-15 days populating an API that the shipped product does not export.

**2. The seam is cut in the wrong place, and a downstream task already says so.**
The issue puts the model inside `private class EditWindow extends JPanel`
(`src/jls/edit/SimpleEditor.java:1121`), inside a 5,852-line class it concedes is mid-
decomposition (#84), verified by four new `@Tag("display")` classes under Xvfb — the exact
substrate whose `rerunFailingTestsCount=2` masking the issue itself flags as O7 and then lists
again as a threat to validity. Meanwhile **#739 AC-3 requires "traversal is asserted headlessly
against the accessible model, so the assertion does not require a screen reader to run."** Its
consumer wants a headless model; this issue builds a display-only one. The description of a
circuit is not a property of a Swing component — it is a property of the document, and JLS's
core is deliberately headless (`HeadlessCoreRatchetTest`, ARCHITECTURE.md "Threading model").

**3. `SpatialIndex` cannot do the job P9 assigns it.**
`src/jls/SpatialIndex.java` is a uniform grid keyed by `Bounds`; its only query is
`query(Bounds) → Set<Element>` (`:189`), it is explicitly allowed to be stale (`:90`
`invalidate()`), and it has no notion of identity order. There is no way to get "child *i* in
stable-id order" out of it, and §7.10's claimed `O(log |E|)` per child is not a cost this data
structure can produce. The thing the issue actually wants already exists and is exactly right:
**`Circuit.getElementsInStableOrder()` (`src/jls/Circuit.java:479`)**, sorted by
`Element::getStableId`, already the canonical order for save, print paging and simulation
seeding (#166/#181/#182). P9 as written ("no second index; read `SpatialIndex`") is a false
constraint that pushes an implementer away from the correct existing accessor.

## The alternative: one headless circuit description, two thin consumers

**A. `jls.CircuitOutline` — a headless, AT-agnostic description of the document.** An immutable
value recomputed on op commit: for each element in `getElementsInStableOrder()`, its
`ElementId`, display name, role, bounds, current value, and its connections; plus **nets as
first-class entries**, derived from `WireNet`/`Put`. No AWT, no Swing, no display tag. Tested
by ordinary headless JUnit under the coverage floors that already apply to `jls` — which
removes the #316 dependency for the part that matters, removes §7.9's entire concurrency
section (an immutable snapshot cannot be queried mid-mutation, so "never throw into the
platform bridge" stops being a failure mode), and removes the O7 rerun-masking threat for
P1/P2/P3 outright.

**B1. A circuit outline panel — a stock `JTree`/`JList` beside the canvas.** Accessible for
free on AT-SPI, the Access Bridge and NSAccessibility, with no custom `AccessibleContext` and
no per-element proxy layer to maintain forever. Keyboard-navigable for free, so **#739's
traversal falls out with "no new focus model" (its AC-4) already satisfied by `JTree`**.
Useful to sighted users too — a structure view / netlist inspector is a feature, not an
accommodation, and that is what makes it survive maintenance. Revised 508 **E101.2 Equivalent
Facilitation** is the standard that blesses it, and #754's generated ACR can cite it by name.

**B2. The canvas `AccessibleContext`, later, as a ~60-line adapter over A** — gated on #737's
Orca verdict and on the bridge fix landing, not on #316. Under this ordering it is cheap,
because all the content already exists and is already tested.

Note what dissolves: **Open Question 1 (a/b/c) stops being a fork.** Nets are first-class
nodes in a tree, so "wire segments as children" never arises, and the alphabet a student
actually reasons in — "what drives input B of the adder" — is the natural one rather than one
reconstructed from an `AccessibleRelation` set. Note also the duplication risk the issue does
not see: `jls.hdl.HdlModel` (`Net`, `Port`, `Operand` records), `jls.collab.op.NetBlocks`/
`ElementBlocks`, and `Element.infoText()` (`src/jls/elem/Element.java:711`, overridden in ~20
element classes) are already three descriptions of this same graph. A fourth, welded to a
`JPanel`, is precisely the "second description mechanism" #739 AC-2 forbids. `CircuitOutline`
should absorb `infoText`, not sit beside it.

## Split the bundle

Items 2-5 (modal accelerator inertness, Probe/Modify/Timing behavioral firing, Alt-navigation,
the macOS keymap leg) share nothing with item 1 except a heading in
`docs/keyboard-a11y-verification.md:121-142`. That is document-shaped scoping, not
architecture-shaped. They are keyboard *test-coverage* chores; they unblock nothing, whereas
item 1 unblocks #739 and the CAP-26 band. Worse, #756 needs the same modal mechanism (H2) and
the issue's own boundary comment already concedes a first-lands-builds-it race. Filing them as
a separate "keyboard checklist residual" task lets the outline ship on the band's cadence and
lets the modal mechanism be owned once, deliberately.

## What to keep verbatim

The issue's engineering instincts are good even where its architecture is not, and these
transfer to the reframing unchanged:

- **Cardinality proportional to design, not drawing** — the single best idea in the document.
- **Role-map totality over `ElementRegistry.all()` as a build failure.** `ElementRegistry`
  exists now (ARCHITECTURE.md:117 is stale on this), `jls/boot/CoreModule.java:41` already
  iterates it, and `PaletteContractTest` is the pattern. Port this straight across.
- **Undo-stability keyed by `ElementId`, never by dense index** (H3, P3) — correct, and free
  once the model is built on `getElementsInStableOrder()`.
- **Red-on-break mutation for every new claim** (O4, P8) and **no `dispatchEvent` on a
  hardcoded reference** — the discipline that makes this repo's UI tests mean something.
- **"A passing `AccessibleContext` assertion is not a working screen reader"** (§11). Under the
  reframing this threat shrinks from fatal to manageable, because a stock `JTree` is what every
  bridge is best at reading.

## Recommended next moves

1. File and land the `jdk.accessibility` / `conf/accessibility.properties` / distro-`Recommends`
   fix with `AccessBridgeModuleTest`. ~1 day. Make it this issue's real `blocked_by`.
2. Rewrite this issue around `jls.CircuitOutline` (headless) + the outline panel, retaining the
   cardinality bound, the totality assertion, the id-keyed ordering and the mutation discipline.
3. Move items 2-5 to their own task; record the modal-mechanism ownership against #756.
4. Keep the canvas `AccessibleContext` as a named follow-up gated on #737, and let #754's ACR
   list the canvas as a documented exception with Equivalent Facilitation as the answer.
