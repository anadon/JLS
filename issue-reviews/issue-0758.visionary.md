# Issue #758: TASK-C549-2: accessible-name coverage is ratcheted, and the standing a11y checklist splits into automated checks and an explicitly manual remainder
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task framing and #758 wants one thing: **a11y claims that cannot outrun
their evidence**. It is the load-bearing middle of an arc — #756 makes reachability
a gate, #758 makes naming a gate and inventories what is actually proven, #753/#547
turn that inventory into a VPAT that can only print what passes. The goal is
squarely aligned with the project's deepest habit: `HeadlessCoreRatchetTest`,
`NotificationRatchetTest`, `ExtensionPointCatalogTest`, `HotkeysHelpAccuracyTest`,
`SaveTagsTest` — JLS already believes that a document asserting a property and a
test enforcing it must be mechanically tied. I endorse the end.

The route needs three changes. Two are architectural seams cut in the wrong place;
one is a taxonomy that quietly launders "unverified" into "manual".

## 1. The premise about the checklist is factually wrong, and the real gap is better

The issue says `docs/keyboard-a11y-verification.md`'s "146 lines stop being a wholly
manual document." They were never a manual document. Every one of its 20 rows across
the three tables already names a concrete automated test — `FocusFaithfulKeyboardTest`,
`KeyboardEditingFaithfulTest.arrowNudge…IsUndoableWithCtrlZ`, `MenuBarSpecTest`,
`PaletteButtonAccessibilityTest` — and line 10 gives the authoritative green bar
(`xvfb-run -a mvn -B verify -Djls.test.headless=false`). Lines 121–142 already
enumerate the uncovered remainder *with reasons*. Lines 108–119 already record five
red-on-break mutations. AC-2's stated work — classify rows as automated or manual —
is ~95% done and was done at authoring time.

What is genuinely missing is **linkage enforcement**. Nothing fails today if
`KeyboardEditingFaithfulTest.rotateKeyRotates…` is renamed, deleted, or `@Disabled`;
the doc silently becomes a list of ghosts. That is exactly the failure mode
`HotkeysHelpAccuracyTest` was built to prevent for `hotkeys.html`, and
`ExtensionPointCatalogTest` for `docs/extension-points.md` (it parses the markdown
table, extracts backticked ids, and cross-checks against the constants in *both*
directions — `test/jls/ExtensionPointCatalogTest.java:90-116`).

**Concrete alternative for AC-2/AC-4:** build `A11yVerificationCatalogTest` in that
exact idiom. Parse the doc's tables; for every row, resolve the named test class and
method reflectively off the test classpath; fail on any that does not exist, and on
any test method in the a11y suites (`FocusFaithful*`, `KeyboardEditing*`,
`MenuAccelerator*`, `MenuMnemonicAndAccessibleName*`, `PaletteButtonAccessibility*`,
`KeyPadAccessibility*`, `TabSelectionFocus*`) that no row claims. Two-way, like the
catalog test. That converts 146 lines of accurate prose into a checked index in a
fraction of the budget the issue's "convert every mechanizable row" framing implies,
and it is the artifact #753 actually wants to consume.

Better still: put the table in code — one record per behavior (id, behavior, signal,
test method or manual procedure, WCAG criterion) — and let the markdown be the
rendered view. #753 then keys the *same* records by criterion instead of maintaining
a second registry that has to agree with the first. AC-4's worry ("so #547's VPAT can
consume the split without re-deriving it") disappears rather than being managed.

## 2. AC-1's seam is dynamic; the honest seam for "in `src/`" is static

AC-1: "a ratchet fails when any focusable component in `src/` has a blank or missing
accessible name." A traversal-based ratchet cannot deliver that sentence. It sees
only the surfaces the harness boots, needs the Xvfb display lane, and therefore gates
in the slow leg rather than on every push. The set "focusable components in `src/`"
is not dynamically enumerable at all — it is a property of construction sites.

The codebase already has the stronger pattern, twice over:

- `NotificationRatchetTest` bans raw `JOptionPane` outside `src/jls/TellUser.java` by
  scanning source text. Headless, whole-tree, unfoolable.
- `DialogCoverageRatchetTest` reads bytecode with ArchUnit and never constructs a
  dialog — its javadoc calls headlessness "by design."
- `ElementFormDialog.labelled(label, field, name)` already sets `labelFor`, component
  name, and accessible name *in one call* (`docs/component-naming.md:41-48`);
  `SimpleEditor.makeElement` does the same from the tooltip
  (`src/jls/edit/SimpleEditor.java:2412-2413`).

**Concrete alternative for AC-1: make omission structurally impossible, then ratchet
the construction sites.** An ArchUnit rule over `target/classes`: no direct
instantiation of focusable Swing types (`JButton`, `JTextField`, `JTextArea`,
`JComboBox`, `JCheckBox`, `JList`, `JTable`, …) inside `jls.edit`/`jls.*` GUI code
outside the naming helpers and a shrinking allowlist. Only 10 `setAccessibleName`
call sites exist today across the whole tree — the coverage does not come from
diligence at each site, it comes from the helpers, and that is the invariant worth
gating. A newly added focusable control then fails in the plain headless build,
before the display lane ever runs, with a message that names the helper to use.

Keep a dynamic sweep, but demote it from *gate* to *oracle*: assert the naming
predicate inside the surfaces `DialogConstructionSmokeTest` already constructs —
which `DialogCoverageRatchetTest` already proves complete over every
`ElementFormDialog` subclass. That is a handful of lines on infrastructure that
exists, versus a new enumeration mechanism with its own exemption list.

## 3. Merge the traversal with #756 instead of building a second one

#756 AC-1 walks every dialog and window surface enumerating focusable controls and
asks *is it reachable*. #758 AC-1 walks the same set and asks *is it named*. Two
tasks, two ratchets, two surface registries, two exemption lists, two falsification
apparatuses — for two predicates over one enumeration. And the arc keeps coming:
focus-visible (WCAG 2.4.7), label-in-name (2.5.3), mnemonic uniqueness, focus order.
Each will want the same walk.

**Cut the seam along the enumeration, not along the predicate.** One
`OperabilitySweep` that yields a record per (surface, control) — component name,
accessible name, `labelFor` source, reachable-from-initial-focus, mnemonic — and
predicates as thin, independently-failing assertions over it. #756 owns the sweep and
the reachability predicate; #758 adds the naming predicate as a ~30-line addition and
spends its remaining budget on the catalog linkage above, which is where the real
value is. Ordering already says `ordering_after: [TASK-C549-1]`, so this costs
nothing in sequencing — it is purely a decision to extend rather than parallel-build.

## 4. AC-4's two-way split is the wrong taxonomy — I am disregarding it as written

This is the one place I think the issue actively pulls against the arc it serves.

"Automated | manual" is two buckets for three real states. The doc's deferred section
lists things that are *not verified by anyone*: mac-only keymap (policy-pinned only —
"this Linux substrate cannot exercise the mac keymap behaviorally"), Alt+letter menu
opening, modal-dialog accelerator inertness, the canvas AT boundary. Reclassifying
those as "manual checklist with an execution procedure" manufactures recurring human
labor for a single maintainer and, worse, lets #547's VPAT print them as
*manual-checklist claims* — a category #547 explicitly accepts. A manual claim nobody
executes is a false claim with extra steps, and CAP-26 exists precisely to prevent
that. The mac keymap row is the proof: no procedure this project can run exists, so a
"manual" label there is structurally unrunnable.

**Reframe to three categories, with "exception" as the default for anything not
automated:** `automated` (names a resolving, passing test), `manual` (names a
procedure, an owner, and a cadence — and is admissible only when someone will
actually run it, e.g. the once-per-release Wayland desktop spot-check pattern in
`docs/wayland-desktop-checklist.md`), `exception` (named limit with a reason,
consumed by #547 as a named exception rather than a claim). The doc's existing
"Deliberately out of scope / deferred" section already *is* the exception list with
reasons; it should be promoted to a first-class category, not converted into
manual claims. This is a strictly stronger honesty guarantee than AC-4 asks for.

## 5. AC-3's per-row mutations should be measured, not hand-committed

AC-3 wants every automated row to keep a mutation that turns it red — 15 more
demonstrations on top of the doc's existing five. Hand-committed transcripts rot: they
are prose about a code state that no longer exists, and nothing detects the rot.

The project already adopted PIT (`docs/mutation-testing-trial-2026-07.md` §6, weekly
via `.github/workflows/mutation.yml`, `mutationThreshold` 80 / `testStrengthThreshold`
82) — but scoped to `jls.sim`, `BitSetUtils`, `Util`, `SpatialIndex`, `jls.collab.op`
with `excludedGroups: display`, precisely because a headless run cannot kill mutants
in `jls.edit`. The trial doc's own scope-expansion note gives the recipe: move the run
under `xvfb-run … -Djls.test.headless=false` if the scope grows. That is the systemic
answer to AC-3 — a measured mutation floor over the a11y surface in `jls.edit`, where
a vacuous row shows up as a surviving mutant automatically and permanently. Where a
one-shot demonstration is still wanted, make it executable rather than narrative:
`scripts/wayland-rig-selftest.sh` is the in-project precedent for driving unmodified
machinery against a seeded defect and asserting the verdict.

## Net

The destination is right and central to the project's whole evidentiary posture. The
route as written builds a second traversal (#756 has one), converts a document that
is already converted, launders unverified behavior into "manual", and pins vacuity
with prose. Reframed: extend #756's sweep with a naming predicate, gate naming
statically at the construction sites in the headless build, replace the
classify-the-rows work with a two-way catalog↔test linkage test in the
`ExtensionPointCatalogTest` idiom, adopt a three-category taxonomy that defaults to
named exception, and let PIT under the display substrate carry AC-3. Same outcome,
smaller footprint, and every piece lands on a seam the project already trusts.
