# Issue #78: Element authoring contract, final stage (H2): make the Element/LogicElement runtime-throw stubs compile-time obligations
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of verification

The issue's factual claims were independently checked against HEAD
(`5311625`, one unrelated checkpoint commit past the cited
`evidence_commit: 29afb26`) and hold up well:

- `Element.java:415-416` and `:432-434`, `LogicElement.java:467-469` and
  `:534-536` match the quoted stub bodies verbatim.
- `Element` is `sealed permits DisplayElement, LogicElement, Wire`
  (`Element.java:17-18`); `LogicElement` is `sealed ... implements Reacts
  permits Adder, ..., WireEnd, RegisterFile, FieldExtend` (`LogicElement.java:17-21`);
  `Reacts.initSim`/`.react` are already `abstract` (`sim/Reacts.java:17,27`).
- `WireEnd` (`extends LogicElement`, `WireEnd.java:17`) has no `react`
  override (`grep -c "void react" WireEnd.java` → 0) but *does* override
  `initSim` as an intentional no-op (`WireEnd.java:687-690`) — the issue's
  §2 observation 4 only claims the `react` gap, which is accurate, but
  undersells that `initSim` was already handled per-class while `react`
  was not, i.e. the "reliance" is narrower and more deliberate than the
  wording implies.
- Sampled every `LogicElement` leaf reachable through the nested sealed
  groups (`Group→Binder/Splitter`, `Pin→InputPin/OutputPin`, plus all
  top-level permits): every one already implements `init`/`initSim`/`react`.
  Only `WireEnd` is missing `react`, confirming the issue's inventory claim
  for that slice.
- Cited tests exist: `CapabilityInterfaceTest`, `PinFaceContractTest`,
  `ElementConstructorContractTest`, `AllElementsRoundTripTest`,
  `ElementRegistry.all()` (`ElementRegistry.java:132`).

So the technical premise is real, not invented, and the bulk of the "risk"
the issue worries about (a concrete element silently relying on a stub) does
not actually exist in the current tree — which is itself worth flagging (see
finding 3).

## Findings, most severe first

### 1. Open Question 1 offers a false dichotomy and undersells option (b)'s blast radius
The issue frames `WireEnd.react`'s disposition as two options: "(a) implement
an honest no-op/assert" or "(b) restructure so `WireEnd` does not implement
`Reacts`," and calls it "Rides along — does not block starting §8." But
option (b) is not a footnote: `WireEnd` is referenced in 39 files, and the
simulator's own dispatch loop treats every `LogicElement` uniformly —
`Simulator.java:152` (`initInputs`), `Simulator.java:197`
(`initSimulation`'s `lel.initSim(this)` call), plus `instanceof LogicElement`
dispatch sites in `SubCircuit.java:578,606`, `Circuit.java:1724`,
`HdlExporter.java:357`, `JLSStart.java:2684`, and
`edit/InteractiveSimulator.java:996`. Pulling `WireEnd` out from under
`LogicElement` (so it stops inheriting `Reacts`) means re-auditing every one
of those sites for whether they implicitly assumed "reachable via
`instanceof LogicElement`" includes wire ends — a cross-cutting hierarchy
change, not a same-PR rider. Worse, the issue's own §10 (Falsification
Criteria) names a *third* option elsewhere in the same document — "interface
segregation (narrower obligation interfaces)" — as the general fallback for
H2, but does not offer it for `WireEnd` specifically, even though splitting
`Reacts` into e.g. `Initializable`/`Reactive` would let `LogicElement`
require only `initSim` and let `react` be a separate obligation `WireEnd`
opts out of — without leaving the `LogicElement` hierarchy at all. That's
plausibly the cheapest fix and the issue doesn't surface it as an option.
**Recommendation:** rewrite Open Question 1 to list interface segregation
as option (c), and require the PR to show (via the grep-for-event-posting
check the issue itself proposes) that option (b)'s dispatch sites were
actually audited before choosing it — not just spot-checked.

### 2. `Element.copy()`'s premise is already contradicted by evidence the issue itself cites
§2 observation 2 quotes the current javadoc: "wires and wire ends are
copied by the editor, not through `copy()`" — then in the same paragraph
notes `WireEnd` *does* override `copy()` (`WireEnd.java:164`) and returns a
real `WireEnd`, not null. Verified: `WireEnd.copy()` constructs a new
`WireEnd`, calls the separate `copy(Element it)` field-copy helper
(`Element.java:442`, distinct from the no-arg `copy()` at `Element.java:432`),
and returns it — i.e. `WireEnd` is copied *through* `copy()`, directly
contradicting the javadoc claim that motivates H2b's "attempt removal"
default. The issue reports this discrepancy as raw evidence but never
flags it as a problem with the premise, so whoever picks up H2b may design
around a stated rationale ("wire ends bypass `copy()`") that is factually
false for `WireEnd` (only true for `Wire`, which genuinely has no `copy`).
**Recommendation:** correct or retire the "wires and wire ends are copied by
the editor" javadoc language before using it as evidence for H2b's design;
treat `Wire` and `WireEnd` as having different `copy()` dispositions, not a
shared one.

### 3. Ceremony-to-work ratio: the audit shows there is almost nothing left to discover
The issue's own §2 evidence, and this review's independent leaf-by-leaf
check, show 21 of 22 concrete `LogicElement` types already implement all
three methods; the only outstanding items are (a) `WireEnd.react`'s
disposition and (b) `copy()`'s null placeholder. That is a small, mostly
mechanical change (delete 3 bodies, make abstract, add one contract test,
resolve two open questions) wrapped in a 14-section research-paper template
(YAML front matter, mermaid diagram, H2/H2b hypotheses, P1-P4 predictions,
§7.1-7.12 interface contract, 13-item Definition of Done). This is a
process-cost risk, not a correctness one: the elaborate structure gives
more surface area for "is this Definition-of-Done item satisfied" disputes
than the underlying diff will have lines changed. **Recommendation:** none
required to proceed, but the assignee should not let the template's breadth
imply the work is bigger than it is — the §8 checklist's first bullet (the
inventory table) is nearly this review's grep table already.

### 4. Minor: the §8 inventory instruction is imprecise about what's already covered
"`ElementRegistry.all()` (+ `Wire`/`WireEnd`)" implies both `Wire` and
`WireEnd` sit outside the registry and need manual addition to the audit.
In fact `WireEnd` is already a registry entry (`ElementRegistry.java:76`,
tag `"WireEnd"`); only `Wire` genuinely needs manual inclusion. Low
severity — worst case someone double-checks `WireEnd` twice — but worth a
one-word fix so the inventory table's provenance is unambiguous.

### 5. P2 is weaker than it reads, though not truly gameable
"commenting out any concrete element's `init`/`initSim`/`react` produces a
compile error ... spot-checked on one element" sounds like it under-verifies
a claim about "any" element. In practice this is safe *because* the
enforcement mechanism is a compiler-level abstract-method check, which is
structurally uniform across every subclass — a single spot-check is
legitimate evidence for the general claim once H2 is implemented via
`abstract`/interface obligations (not per-element logic). Flagged for
completeness, not as a real defect: the wording could still mislead a
reviewer unfamiliar with why one spot-check suffices.

## What's solid (no further comment)
- The three cited stub locations and their exact text are accurate.
- The sealed-hierarchy/interface mechanism the issue proposes to lean on
  already exists and already does exactly this kind of enforcement
  elsewhere (`CapabilityInterfaceTest`, `PinFaceContractTest` are real,
  working precedents for the "registry-driven contract test" §8 asks for).
- Falsification criteria (§10) and Completion Criteria (§14) are concrete
  and checkable, not vague.
- Scope discipline is real: "adjacent work discovered en route ... filed as
  new issues" and the explicit non-goals (rotation-as-transform,
  descriptor-driven builder) keep the diff bounded.
- `blocked_by: []` checks out — no open PR touching `Element.java`/
  `LogicElement.java` was found, and the cited #201/#271 supersession
  language is consistent with the merged-program narrative in §1.

## Verdict rationale
The core hypothesis is well-evidenced and the acceptance tests are mostly
sound, so this does not need a rewrite. But finding 1 (an incomplete option
set that hides a real cross-cutting risk behind "rides along") and finding 2
(an H2b premise the issue's own evidence contradicts) are the kind of gaps
that should be closed in the issue text before implementation starts, not
discovered mid-PR. Hence **sound-with-concerns**, not **sound**.
