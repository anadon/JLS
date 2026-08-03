# FEAT-014 - Stable addressing and per-view geometry in the shared model

**Status:** proposed | **Cost:** 11-17 mw | **Owner program:** P3 |
**Spine rank:** S7, S11, S12, S18

## Capability delivered

Anything a person can point at in a JLS design has one name that survives a
save, a reopen, a merge, a rename and a subcircuit sharing migration: an
address of the form view, instance path, stable id. Nets and wire groups get
that identity too, not just elements. Geometry stops being one record per
artifact and becomes one record per artifact per view, so a schematic position,
a breadboard position and an analog-canvas position for the same component
coexist without overwriting each other, and an editor that has never heard of a
view preserves that view's geometry verbatim instead of dropping it.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | "one artifact, N views" is exactly this feature; without per-view geometry the disciplines collide on one geometry record |
| CAP-04 | required | breadboard geometry lives in its own section keyed by the same instance identity as the schematic |
| CAP-05 | required | refdes must be a pure function of circuit content, which means a stable instance identity |
| CAP-12 | required | analog is a second view over one model; its geometry belongs in its own versioned section |
| CAP-13 | beneficial | refdes assignment and cross-probing key off stable addresses |
| CAP-02 | beneficial | addresses the boundary being toggled and the probes being read |
| CAP-03 | beneficial | addresses the toggled boundary and the probed nets |
| CAP-17 | required | a watched element inside one partition needs a name that does not depend on which partition it landed in |
| CAP-18 | required | the back-annotated routed length is a second view's datum about a first view's net, addressed the same way every other view is. Note the half that shipped is the wrong half: per-element permanent identity landed with #165, but a constraint is attached to a **net**, and stable net identity did not. Added 2026-08-03 under D16: the filed issue #318 declares `serves_capstones: [... 313 ...]` and #313 carries 318 in `requires_features`; this table's omission was a transcription defect |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-013 | The per-view geometry section is only safe if a reader that does not know the view can skip it and write it back unchanged; that is exactly must-understand section semantics, and it does not exist without FEAT-013 |
| FEAT-015 | The view discriminator lands on the geometric ops and needs their inverses to be exact; a discriminator added to ops that still require a `Graphics` cannot be exercised headlessly |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0035 | Stable identity for instances, nets and groups | The addressing scheme itself, plus the uniqueness test that must survive shared definitions |
| TASK-0036 | Per-view geometry section and the op view discriminator | The storage half and the mutation half of "geometry is per view" |

## Acceptance criteria

1. `view:instancePath:sid` is specified in a normative document and implemented,
   and a test asserts every address in a nested fixture is unique.
2. The uniqueness test passes on a fixture where one subcircuit definition is
   instantiated more than once *and the definition is shared, not copied* -
   this is the case flat ids fail.
3. Nets and wire groups have stable ids that survive save, reopen and a
   permutation of element order.
4. A reader built before a view existed opens a file containing that view's
   geometry section, edits an unrelated element, saves, and the unknown
   section is byte-identical in the output.
5. Every geometric op carries a view discriminator, and for each op
   `apply` then `invert().apply` returns the canonical save to its prior
   bytes - on both live and save/load-restored circuits.
6. Two views' geometry for one artifact can differ, and moving the artifact in
   one view provably does not change the other view's record.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - per-view op streams are fictional without this addressing, but #163 does not track it |
| - | stable addressing, net identity and per-view geometry | **no issue** - the registry records the whole of decisions D1/D2/D3 as untracked |

Element-level stable ids are **already shipped** (recorded work, issue #165,
now closed - do not cite it as open). This feature extends that identity to
instance paths, nets and groups.

## Design notes

The flat `sid` is design-unique today only by accident: `SubCircuit.save`
(`src/jls/elem/SubCircuit.java:282-289`) writes `getSubCircuit().save(output)`,
inlining the whole definition per instance, so each instance carries its own id
namespace. The moment definitions are shared (FEAT-017) that accident ends and
two instances of one definition present the same `sid` for different artifacts.
KiCad hit exactly this and rewrote around `SCH_SHEET_PATH` UUID chaining;
`10-capstone-plan.md` §2.2 records that precedent and the resulting 11-17 wk
band for S7 + S18 + S11 + S12.

The order that matters: instance identity (S7) is cheap and urgent, net identity
(S18) is the expensive half, and per-view geometry (S11/S12) is what CAP-01 and
CAP-04 actually consume. Shipping S7 alone buys the refdes determinism CAP-05
needs without paying for views.

Do not put hierarchical *net naming* here. Net names are FEAT-004's; what this
feature adds is the instance-path qualifier that FEAT-004's convention consumes
once hierarchy exists. Splitting it the other way produces two naming schemes.

## Risks

- **Legacy content.** Every existing `.jls` predates net and group identity, so
  identity must be mintable at load time deterministically, or 100% of existing
  content diffs on first save. This is the hazard `new-p11-p12-diff-api.md` C8
  names for element ids and it applies at least as hard here.
- **This feature is urgent only because of FEAT-017.** If parameterized shared
  definitions are deferred indefinitely, the flat `sid` keeps working and this
  feature's priority is only the view half. Say so rather than overstating.
- **The 11-17 mw band is four spine rows.** Funding one row and claiming the
  feature is misreporting; the acceptance criteria above are deliberately
  separable so partial funding can be stated honestly.

## Evidence

- Per-instance inlining that makes flat ids accidentally unique:
  `src/jls/elem/SubCircuit.java:282-289`; measured sharing factor 1.00x
  (`BRIEF.md` §7).
- Element stable identity at HEAD: `src/jls/elem/Element.java:24-26`,
  `:619-622`, `:634`, `:646`.
- Cost and the KiCad precedent: `10-capstone-plan.md` §2.1 rows S7 (2-3 wk),
  S11 (3-4 wk), S12 (2-4 wk), S18 (4-6 wk); §2.2 sums them at 11-17 wk.
- Must-understand section semantics as the carrier: `BRIEF.md` §11 D3.
- Op inverses as the existing contract the discriminator must not break:
  `docs/operation-layer.md` "Contract" section.
- **Cost reconciliation.** Band 11-17 mw. Tasks named for it: TASK-0035,
  TASK-0036, totalling 4 wk. The named tasks are the leading, dividable slices
  of this feature, not the whole of it; the residual has no task id, because
  the registry's task space is closed at TASK-0112. Do not read 4 wk as the
  feature.
