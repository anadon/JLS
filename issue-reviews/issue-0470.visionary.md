# Issue #470: TASK-0021: the dialog sweep starts asserting behaviour — a constraint string is checked on the surface it was written for, an OK handler is exercised, and a display lane that ran nothing turns red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two ends, bundled into one task, that do not belong together:

1. **#52's one-string-two-surfaces contract should actually hold on the dialog surface.** A student
   typing `-5` into a capacity field must see the message the loader would report, and the element
   must not mutate.
2. **The display lane must not be able to lie.** `Tests run: 25 … Skipped: 25`, `mvn exit=0` is the
   single most valuable observation in the issue (O7).

A third deliverable — `GestureAssert` — is smuggled in and serves neither end; it belongs to the
editor decomposition (#84, which absorbed #441).

Both real ends are right. The route chosen for (1) — 25 refusal cases plus 25 commit cases, hand
written per dialog, driven through `Robot` under a window-manager-less Xvfb, keyed to per-element
constants — is the expensive, brittle, and *incomplete* way to reach it, and the issue's own
falsification criteria predict the collapse. I am disregarding the per-dialog case matrix, the
"validation case per dialog" ratchet, and the Open-Question-1 recommendation, and keeping O7's gate.

## Why the stated route collapses on contact (evidence at HEAD)

**H1's premise is false for roughly five sixths of the sweep.** The whole tree publishes exactly six
constraint helpers across four element classes:

```
src/jls/elem/Clock.java:52,69   checkCycleTime, checkOneTime
src/jls/elem/Group.java:41      checkBits
src/jls/elem/Memory.java:73,87  checkCapacity, checkBits
src/jls/elem/ShiftRegister.java:62  checkBits
```

Meanwhile **20 dialogs call `reject(...)`** and only 6 override `validateInputs()`. `ClockDialog`
and `MemoryDialog` show the pattern: the parse-and-check is written twice, once in `validateInputs`
and again in `validateAndAccept`, and the messages that are *not* backed by an element constant are
inline literals — `"Value not numeric"`, `"Missing or invalid name"`, `"Pick RAM or ROM"`,
`"Duplicate name"`. Section 10 says a dialog whose rejection path does not surface a published string
is exempted with a filed issue. Applied honestly that exempts about 21 of 25 entries on day one —
and T4, the issue's own exemption-drift threat, fires by construction. The ratchet would then assert
that four dialogs are validated and twenty-one have open tickets.

**H3 is already refuted at the evidence commit.** `DialogCoverageRatchetTest.SWEPT` holds **26**
names; `DialogConstructionSmokeTest` has **25** `@Test` methods and **24** distinct
`constructAndCancel(...)` element names (`DelayChange` and `Element` appear in `SWEPT` but are never
constructed by name). The list's own comment claims "this list failing to match the sweep is exactly
the signal the ratchet exists to give" — but nothing derives one from the other, so the signal does
not exist. Building a second ratchet on the same hand-maintained string set inherits the defect.

**Open Question 1 is not open.** The issue offers accessible name / component index / test-only
registry, prefers accessible name, and leans on #758. The answer already shipped: `docs/component-naming.md`
is the normative scheme, `ElementFormDialog.labelled()` assigns `dialog.<slug>.<field>` at **46 call
sites across 22 dialogs**, `dialog.ok` / `dialog.cancel` are set in the base constructor, and
`ComponentIdentityTest` already enforces it on the live app. Component names are also *stronger* than
accessible names for this job: accessible names are human labels and collide across dialogs ("Bits"
appears many times), component names are unique by construction. The stated blocker is a solved
problem, and picking the weaker option would couple this task to #758 for no gain.

## The reframing: declare the constraint, and the 50 cases disappear

The seam is already half-built and pointed the right way. `ElementFormDialog.confirmDialog()` is
`validateInputs() → showViolation | validateAndAccept()` — a clean gate. `Violation` is already
(message, field). `Attribute` in `jls.elem` is already "declarative save/load/dialog plumbing for
element parameters (issue #52)". The missing move is one step, not fifty tests:

1. **Give a constrained parameter a declaration.** Extend `Attribute` (or add a sibling `Constraint`)
   with a total `@Nullable String check(value)` and the field name it addresses
   (`dialog.memory.capacity`). The six existing `check*` helpers are the first six registrations;
   nothing about the loader half changes.
2. **Make `ElementFormDialog.validateInputs()` walk the declarations by default.** Resolve each
   declared field by the `dialog.<slug>.<field>` name `labelled()` already assigned. The 20 `reject()`
   sites migrate into declarations; the duplicated parse/check in `validateAndAccept` disappears with
   them.
3. **Two ArchUnit rules, headless, un-retryable, and total over future dialogs:**
   - no class in `jls.edit` may pass a *string literal* to `reject(...)` or `new Violation(...)` —
     every constraint message must be a `jls.elem` constant. This *enforces* #52's one-string
     contract for every dialog and every message. The 25 hand-written cases only spot-check the
     values the test author happened to type.
   - no `ElementFormDialog` subclass may call `reject(` from `validateAndAccept()` — validation lives
     in the gate, so "invalid input never closes the dialog and never mutates the element" is
     structural rather than re-asserted 25 times.
4. **One headless table test over the declaration set**: each declared constraint gets a rejecting and
   an accepting value, asserted against the declared constant. Runs in the ordinary lane. No display,
   no `assumeFalse`, no `rerunFailingTestsCount`, no three-run rerun audit (P8 evaporates), and it is
   exhaustive by construction because the ratchet is over the declarations, not over a hand list.
5. **One *generic* parameterized display test**, driven from the same declarations: look up the named
   field, type the rejecting value, click `dialog.ok`, assert the error label text equals the declared
   constant, the window is still showing, and the element is unmutated; then the accepting value and
   assert commit. That is the only thing a display can prove that headless cannot — *that the wiring
   exists* — and it is one method, not fifty.

Net: the flaky surface shrinks from ~50 Robot cases to one parameterized case; the exemption set
shrinks from "hard dialogs" to "elements that declare no constrained parameter", which is a fact
about the model rather than an escape hatch; and a new element that ships a constrained parameter
*cannot* ship an unvalidated dialog — strictly stronger than "has a validation case".

## Trajectory: this reframe is the project's own arc, the issue as written pulls against it

ARCHITECTURE.md's "Adding an element today (the honest list)" is sixteen manual steps, with #78's
element registry named as the recorded direction that collapses them. The issue as written adds a
seventeenth ("and write two display validation cases, or file an exemption"). A declared constraint is
a *column of the registry table* — the same declare-once-derive-everywhere move that
`docs/component-naming.md` already made for component identity (slugs sourced from `ElementRegistry`;
`SimpleEditor.paletteSlug` refuses to build a toolbar for an unregistered tag). It feeds #78 rather
than taxing it. The recorded i18n non-goal reinforces this: inline English is fine, but the constant
discipline is the only thing that would ever make i18n cheap, and the "no literal in `reject()`" rule
is that step at near-zero cost.

## Split the executed-count gate out and land it this week — with a simpler mechanism

O7 is the best finding in the issue and is independent of everything above. It should be its own PR.
But §7.6's mechanism — read `target/surefire-reports/*.xml` as `(tests, failures, errors, skipped)`
and assert `tests - skipped > 0` — couples a test to build output and to Surefire's report schema.
Invert the skip instead: the arming signal already exists and `pom.xml` already threads it
(`jls.test.headless`, `-Djava.awt.headless=${jls.test.headless}`, `<groups>display</groups>`). One
shared JUnit `ExecutionCondition` (or `DisplayTests.requireDisplay()` in `@BeforeAll`) replaces the
~30 `Assumptions.assumeFalse(GraphicsEnvironment.isHeadless())` call sites: **assume when unarmed,
fail when armed-but-headless.** No XML parsing, no build-output coupling, one edit point, and #756's
future reachability ratchet inherits it free. H4 is satisfied more directly than the issue proposes.

## Drop `assertInteractionState`, and move `GestureAssert` to #84

`assertInteractionState(editor, state)` needs `SimpleEditor.State` (declared at
`src/jls/edit/SimpleEditor.java:770`, package-private in `jls.edit`) to be reachable from `jls.ui` —
a production change §7.12 explicitly forbids. Worse in principle: the nine internal state names are
precisely what #84's extraction intends to relocate. A net woven from internal state names is a
decomposition *brake*, not a safety net. The outcome-shaped helpers are the right ones and need no
production change: `assertSelected` / `assertNothingSelected` / `assertDragRejected(reason)` /
`assertOpsEmitted(EditOp...)` — `EditOp` is already public and already documented as a "pure,
headless-safe descriptor". Those belong in the extraction's own issue, where their consumer lives.

## What I would ask the maintainer to change

- Retarget this task to: the declarative constraint seam (1–2), the two ArchUnit rules (3), the
  headless table test (4), and the single generic display case (5). Delete the 25×2 case matrix and
  the validation-case ratchet from the acceptance criteria.
- Close Open Question 1 now with `docs/component-naming.md`'s `dialog.<slug>.<field>`; drop the
  implied dependence on #758.
- File the `SWEPT`-vs-sweep discrepancy (26 / 25 / 24) as its own small fix: derive the ratchet's set
  from the sweep's `@MethodSource` rather than restating it.
- Split O7's gate into its own PR with the `ExecutionCondition` mechanism; it protects every display
  test in the tree today.
- Move `GestureAssert` to #84 and drop `assertInteractionState`.
- P8's three-CI-run manual rerun audit is a symptom, not a control. If the reframe lands, only one
  display case remains and the retry policy stops hiding anything worth hiding.
