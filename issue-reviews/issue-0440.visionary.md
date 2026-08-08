# Issue #440: TASK-0019: the editor decomposition is a written plan measured against HEAD, and jls.edit stops being the one package whose coverage nothing defends
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Stripped of its apparatus, #440 asks for three things: (1) *the editor may not grow
untested*, (2) *the next extraction is startable cold from a written artifact*, and
(3) *no package is unfloored by accident*. Goal (3) is excellent and should ship
this week. Goals (1) and (2) are right, but the instruments chosen for them — a
`COVEREDRATIO` `PACKAGE` floor on `jls.edit`, and a new `docs/editor-decomposition-plan.md`
— are the wrong tools, and both already have better-fitting counterparts in this
repository. I am redirecting rather than reframing because what ships changes.

## 1. The floor is pinned to a boundary the decomposition exists to redraw

`docs/grand-architecture.md` §5 puts the future GUI module at `jls.edit / jls.ui`,
and §8 makes #84 "the gui-module counterpart to #77, proceeds as the op layer lands."
The decomposition's whole point is that `jls.edit` stops being one 84-file, 23,910-line
package. JaCoCo's `PACKAGE` element matches exact package names, not prefixes: the
moment TASK-0020 lands the mouse machine in `jls.edit.state` (or renderers move to
`jls.edit.render`), those lines leave the floored set entirely. Worse, they leave
*asymmetrically*. The extracted GoF State objects will be the best-covered code in
the editor — that is the reason to extract them — and removing above-average-coverage
lines from a package **lowers** the package ratio. A floor set with 0.5–1.0pt of
epsilon can therefore be tripped by a successful extraction and held by a failed one.
The issue floors the thing it is scheduling for demolition, and never analyses the
subpackage case in §7.11 or §11.

## 2. `COVEREDRATIO` over 23,910 lines is fungible, and the epsilon exceeds the signal

Two structural properties of a package ratio, neither examined in §7.10:

- **Fungibility.** `SimpleEditor.java` is 5,852 of 23,910 lines — 24.5%. The other
  83 files are renderers and dialogs, exactly the surface `test/jls/ui/`'s 34 files
  cover. A contributor who adds a 300-line fully covered helper anywhere in the
  package buys roughly 1.2 points of slack under which `SimpleEditor` may rot. The
  floor does not defend the editor; it defends an average over the editor's neighbours.
- **Granularity.** 100 uncovered lines added to a 25%-covered 23,910-line package
  moves LINE by ~0.11 points. Under the mandated 0.5–1.0pt epsilon (O9, the #233
  incident), roughly **500–900 lines of wholly untested editor code can land before
  the floor is even reached**. P4's constructive witness proves the rule *matches
  classes*; it does not prove the floor binds at the granularity of a real PR. The
  4,119 → 5,852 growth this issue is filed against would have taken ~500 lines to
  register.

## 3. The headline evidence is a size fact, and coverage cannot ratchet size

O1 states the thesis outright: "extraction without a floor has not been net reducing."
That is a claim about *concentration* — 5,852 lines in one class — and a coverage
ratio is invariant under concentration. Split `SimpleEditor` into twenty 300-line
classes with no test changes and every number in §7.10 is identical. The metric
being ratcheted is not the metric that failed.

## Alternative A (in-instrument, cheapest): `MISSEDCOUNT`, not `COVEREDRATIO`

JaCoCo rule limits take `MISSEDCOUNT`/`COVEREDCOUNT` with `maximum`/`minimum`, not
only ratios. A ceiling —

```xml
<limit><counter>LINE</counter><value>MISSEDCOUNT</value><maximum>N</maximum></limit>
```

— on `jls.edit` is strictly better here on four axes at once:

- **Not fungible.** A covered line costs 0. Adding tested code buys no slack for
  untested code; every uncovered line is charged 1:1. This is literally "the editor
  may not grow untested."
- **The measurement-basis trap collapses.** Headless misses ≥ display misses, so a
  ceiling set from the headless run is the *loose* side for everyone. H2, O8's
  whole demonstration, and Open Question 1 — which the issue marks **blocks
  execution** — stop being blocking. Record both numbers; nobody is locked out.
- **Extraction moves it the right way.** Lines leaving for `jls.edit.state` lower
  the count; the new package is caught by the inventory ratchet (deliverable 3).
- **The climb convention transposes cleanly:** floors only move up becomes ceilings
  only move down, one line in `CONTRIBUTING.md`.

Epsilon becomes a flat `+N` lines rather than a jitter budget on a ratio. If a
coverage rule on the editor's *logic* is still wanted, use `<element>CLASS</element>`
scoped to `jls.edit.SimpleEditor` — non-fungible by construction, and it says what
it means.

## Alternative B (house style): a size ratchet, which is what P6 is asking for

P6 requires "every row of the plan states a target line count, so 'did the extraction
land' is answered by a number," and §11 concedes the plan's line ranges will rot.
Both disappear if the plan's numbers live in a test. This repo already has the
pattern, four times over: `HeadlessCoreRatchetTest` (shrinking baseline, "never add
a line"), `NotificationRatchetTest`, `ArchitectureRulesTest`, and — the closest
template — `test/jls/ui/DialogCoverageRatchetTest`, a `Map<String,String>` of
exemption→reason with "shrink, never grow" in its Javadoc. An `EditorSizeRatchetTest`
holding `Map<String,Integer>` caps (`SimpleEditor` 5852, `StateMachineDialog` 1929,
`InteractiveSimulator` 1437) trips at line 5,853, needs no measurement basis, no
epsilon, no `mvn clean`, no display lane, and no JDK matrix reasoning. It would have
caught the 42% growth on the first offending PR instead of the 500th line.

## Alternative C: the plan already exists — it is `docs/operation-layer.md`

§Background asserts `git ls-tree docs/` "matches nothing outside `docs/plan/`". The
working tree has 25 top-level documents under `docs/` and no `docs/plan/` at all,
and one of them is the artifact #440 wants to write. `docs/operation-layer.md`
§"Mutation-site inventory (§7 step 1)" is a per-gesture table — commit point, target
op kind, migration status, pinning test — covering watch toggle, rotate, flip, probe,
move-selection, placement drop, jump-end, delete selection, wire-net delete, paste,
wire-attach, wire-draw cancel; §"What lands next" is the ordering; §"Layering" is the
enforced boundary. It is maintained, tied to `MoveGestureTest`/`DeleteGestureTest`,
and cited by ARCHITECTURE.md's arc.

More importantly it records the seam that actually works. `SimpleEditor.deleteSelectionPlan`
(`src/jls/edit/SimpleEditor.java:872`) and `moveSelectionPlan` (`:1053`) are **static,
Swing-free functions from gesture intent to `List<CircuitOp>`**, pinned headlessly by
tests whose own Javadoc notes "the editor itself cannot be constructed headless."
That is the answer to "how does editor code become coverable" — not GoF State objects
first, and not a coverage floor. The two columns #440 genuinely adds — target line
count, and how each AWT dependency is inverted — belong as columns on that existing
table. A second document that re-plans the same gestures against `EditWindow`'s line
ranges will diverge from it within one wave.

## Keep, unchanged in intent: the floor inventory

Deliverable 3 is the strongest part of this issue and is independent of the other two.
Two refinements:

- **Reuse `PackageInfoRatchetTest`'s walker.** §7.10 Stage 1's π transform is already
  implemented there over `src` and `test`. Two implementations of "enumerate the
  packages" is exactly the drift the inventory exists to prevent; put the new assertion
  in that file or beside it.
- **Open Question 2's answer is (d), not (b).** A YAML file under `config/` introduces
  a format and a parser this project does not otherwise have; `DialogCoverageRatchetTest.REPRESENTED`
  is the house answer — an in-test constant map, reason beside assertion, reviewed in
  the same diff. That deletes one "blocks execution" question and one §7.11 failure mode.

## Acceptance criteria I am explicitly disregarding

Open Question 1 (headless vs display basis, marked blocking); P3, P4, P7 and the
`pom.xml`-comment-states-the-basis criterion; and `grep -c 'deliberately unfloored'`
returning 0 in both files. All of these are downstream of choosing a `COVEREDRATIO`
`PACKAGE` rule. Under Alternative A they are replaced by a ceiling whose basis is
non-blocking; under B they are unnecessary. The `jls.edit` prose exemptions in
`pom.xml:408-411` and `CONTRIBUTING.md:106-108` should still be replaced in the same
commit — but with "governed by the `jls.edit` line-ceiling and size ratchet," not with
a raise-only ratio rule.

## What I would ship instead, in order

1. **Floor inventory ratchet**, in-test exemption map, reusing the existing package
   walker. Unblocks immediately; no measurement.
2. **`EditorSizeRatchetTest`**, seeded at today's counts for the three god classes.
   One afternoon; catches the exact regression #440 was filed against.
3. **A `LINE MISSEDCOUNT` ceiling on `jls.edit`** from a headless `mvn clean verify`,
   plus a `CLASS`-scoped rule on `SimpleEditor` if a ratio is still wanted.
4. **Two columns added to `docs/operation-layer.md`'s inventory** (target line count,
   AWT-inversion cost per row), with the #84 §7 tick-list folded in. No new document.

Net effect: TASK-0020 and TASK-0021 unblock without waiting on a blocking decision,
the ratchet measures the property that actually failed, and the plan lives where the
next executor is already reading.
