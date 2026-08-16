# CAP-35 (#519) readiness review

**Verdict: ready-with-gaps** — start now on the unsequenced slices (#799, #791, #792, #794, #796); five named gaps must be reconciled before #585's later ACs and before capstone closure.

Tree walked: #519 → FEAT #584 (TASKs #791, #792, #793), FEAT #585 (#794, #795), FEAT #586 (#796, #797, #798), FEAT #587 (#799, #800, #801). All 4 features and all 11 tasks are open native sub-issues. External ordering: #101 (open), #91 (open). Load-bearing code claims were checked against the master checkout.

## 1. Decomposition

**Sound overall.** Every planned feature (PF-1..PF-4 → #584-#587) is filed, open, and a native child; each carries tasks that are open and mutually boundary-confirmed. No double ownership found — the #792/#793, #797/#798, #799-vs-#524, and #586-vs-#101/#91 boundaries are explicit and stated identically from both sides. No required child is closed.

Three unowned-scope holes:

- **G1 — #585 AC-2's resolver fallback tiers are self-declared unassigned.** #585's own Boundary section says neither #794 (storage/addressing only) nor #795 (computed link, running-version → latest) builds the `/h/<topic-id>?v=<version>` resolver's tier-2/tier-3 behavior (nearest-published-version with banner; immutable GitHub blob at tag). A feature AC with no owning task. Additionally unpriced: AC-8 pins GitHub Pages, which is static hosting — a query-param resolver with fallback logic can only be a client-side JS redirect page there; no task names or costs that mechanism.
- **G2 — CAP AC-3's "Contracts" root has no producer.** #519's Outcome and AC-3 require the hosted site to carry a **Contracts** root — `docs/*.md` published verbatim with matching anchors. #585's rewritten Outcome deliberately does the opposite ("Normative documents stay single-homed… the hosted site links out to its tagged GitHub blob rather than republishing it"), and #584 leaves docs/*.md subsumption as an explicit open question with a recommended default of "keep the trees separate." No child or task publishes `docs/*.md` on the site. All four features can close while CAP AC-3 fails as written. The children's single-homing decision is deliberate and well-argued; the capstone body is the stale side — but the contradiction is unreconciled and must be resolved at capstone level (amend #519 AC-3, or file the Contracts-root task) before closure.
- **G3 — nobody owns "migration is complete."** #584 AC-1/AC-5 describe a finished state (both targets derived "from a single iteration over one shared topic list"; `resources/help/**` becomes pure uncommitted build output; corpus-wide normalized-equivalence with a committed waiver list). #793 rewrote itself to opportunistic per-page migration — "an unmigrated page is not a defect to track," equivalence checks scoped to pages a PR touches, no completion criterion. All three of #584's tasks can close with two pages migrated and `resources/help` still hand-maintained HTML, leaving #584 AC-1/AC-5 (and the "one source per content root" half of CAP AC-1) unmet. Either #584's ACs need softening to match #793's incremental endpoint, or a completion task is missing.

## 2. Acceptance-criteria composition

- **CAP AC-1** (one build, both targets, one source per root): composes for the site direction the day #792 lands (site generated from today's in-jar tree). The "one source" endpoint depends on G3.
- **CAP AC-2 is stale against its own children — intent composes, letter does not.** AC-2 says fact blocks are emitted "from the extended `ElementType`/`PaletteEntry`/`Attribute` descriptors." #587 AC-3 and #800 explicitly reject extending `ElementType`/`PaletteEntry` (verified: `src/jls/elem/ElementType.java` carries only tag/aliases/elementClass/factory, with #78's scoping recorded in its class comment) and instead read the live instance (`LogicElement.getAllPuts()`, verified at `LogicElement.java:328`) plus the `Attribute` registry, adding a new `ElementParameter` record. #584's boundary already records this re-scope as agreed. The children's design is stronger (nothing hand-authored to drift), so the capstone passes on intent — but #519's AC-2 text should be brought in line so the closure check isn't judged against a rejected mechanism.
- **CAP AC-3**: does not compose (G2).
- **CAP AC-4**: composes. #796/#797/#798 deliver the -i figure pipeline (Class 1, byte-identity, `CircuitRenderer.exportImage` verified) and the chrome pipeline with the "#101 gaps closed **or named as accepted, compensated risk**" leg satisfied by design: release-time fail-closed independent of the advisory lane, provenance gating instead of pixel tolerance, and #411 (open) explicitly disclosed per #797 AC-4.
- **CAP AC-5**: two legs already green (hotkey, missing element page); the new flag leg is delivered by #799 but **narrower than the CAP text**: the reverse scan resolves `-`-tokens only inside `jls`-invocation code spans. A bogus flag named in free prose in `docs/batch-interface.md` escapes. The narrowing is well-reasoned (kills the ignore-list problem) but is a literal scope reduction against AC-5 worth acknowledging at closure.
- **CAP AC-6**: composes — #792 AC-2 (byte-identity for untouched files, `git diff` empty) plus #793 AC-2/AC-4 (per-page equivalence, font-tag fixes called out as deliberate corrections) together are the "reviewed diff report."
- **#585 AC-3/AC-5 are blocked on a handshake #584 has not made.** Both declare themselves blocked "until #584 accepts" (a) a frozen, alias-preserving topic-id table guarding post-migration renames and (b) a shared search-index build product. #584's AC list contains neither. #792 de facto builds a search index and canonical topic→URL mapping, but the *freeze/alias* guarantee exists nowhere. This unresolved cross-issue ask sits directly on #795's critical path (its AC-1/AC-4 repeat the block). **G4.**

## 3. Dependency chains

Acyclic; every `ordering_after` edge points at an open issue. #101/#91 are correctly scoped to the chrome-shot slice only; both are open, filed, and in-repo (funded) — no external unfunded prerequisite. #101's weaknesses are tracked at open #411 and, because #796 AC-4 fails the release procedure closed regardless of the lane's advisory status, they do not block start.

Edge hygiene issues (will mislead a scheduler that trusts `ordering_after` alone):

- **#793 declares `ordering_after: []` but cannot land before #791 or #792**: its body renders pages "through the format TASK-C584-1 picks" and "adds the `.md`-rendering step to #792's pipeline." Real edges, unrecorded.
- **#795's true blockers (G4) are prose-only**, not representable in its `ordering_after: [TASK-C585-1]`.
- **#800's edge to #793 is soft**: fact-block emission into the hand-maintained element pages (explicitly out of #793's migration scope) doesn't technically need the 44-page migration; the edge lengthens the critical path conservatively rather than incorrectly.
- Cosmetic: #519's PF-4 text names closed #26 as a future consumer of the element metadata; #798/#796 vs #586 use two taxonomies (Class 1/2/3 vs Tier A/B/C with a new `component` tier) — #798 is sequenced last and extends the schema, so it's evolution, not conflict, but the manifest schema will change once mid-feature.

## 4. Staleness and cost

**Evidence that verifies against HEAD** (all checked in the working tree): `Map.jhm` 84 topic ids → 83 distinct pages; 83-page help corpus; 95 `<font>` tags; 8 yellow / 16 pink / 2 cyan low-contrast uses; `PIXEL_DIFF_MIN` record-only in `scripts/wayland-rig.sh`; `java.awt.Desktop` unused anywhere in `src/`; `JEditorPane` viewer in `Help.buildWindow` (~line 207); `usageText()` generated from `FLAGS`; `commandLineFlags()` accessor present; `HelpTopicsTest.shouldCheck` skipping scheme-absolute links; the seven `resources/help/images/*.gif` duplicates byte-identical to `src/jls/images`; `RenderAssert`/`RenderBoundsTest`, `ExtensionPointCatalogTest`, `FileFormatSpecTest`, `AllElementsRoundTripTest`, `BatchSimulationGoldenTest` all present; call-site line numbers `JLSStart.java:2177`, `InteractiveSimulator.java:156`, `StateMachineDialog.java:397`, `TruthTableEditor.java:103` all resolve. This corpus was clearly written against the real tree.

Stale details (none start-blocking):

- **Page-split counts drifted**: today it's 41 element / 42 non-element pages, not the 39/44 that #584/#793 state (the 83 total still holds). #793's "44 non-element pages" scope wording needs a count refresh at pickup.
- #792 says "8 call sites" of `Help.showTopic`/`enableHelpOnButton`; there are 5 (the four named plus `ElementFormDialog.java:190`, which passes a dynamic `PaletteEntry.helpTopic()` — relevant to its constants-class refactor).
- **G5 — cost bands are contradicted by the decomposition.** Task-band sums: #584 = 3.5-5 (matches its 3-5), #585 = 2-3.5 (vs 2-3, plus G1's unbudgeted resolver tiers), **#586 = 2.5-4 (vs its stated 1-2)**, **#587 = 3-5 (vs its stated 1-2)**. Total task sum ≈ 11-17.5 mw against the capstone's declared 6-10. The plan is roughly 1.5x its own band; re-band #586/#587 and the capstone before commitments are made on the 6-10 number.
- ARCHITECTURE.md's stale "no element registry yet" line is known, flagged inside #587/#800 with correct read-the-code guidance — handled.

## Verdict

**ready-with-gaps.** Start immediately, in this order: #799 (the capstone's own demo slice, fully unsequenced), #791, #792 → #794, #796 — none of these touch a gapped AC. Before their dependents proceed, resolve: **G4** (#584 must accept or explicitly re-home the frozen topic-id/alias table and shared-search-index commitments; #795 is blocked on it), **G1** (assign or descope #585 AC-2's resolver tiers, and price the static-hosting JS-resolver reality), **G3** (give migration-completion an owner or soften #584 AC-1/AC-5). Before capstone closure, reconcile the #519 body itself: **G2** (Contracts root vs. the children's single-homing decision) and the stale AC-2 descriptor wording; refresh the cost bands (**G5**). No cycle, no dependency on a closed issue, no unfunded external prerequisite.
