# Issue #447: TASK-0041: one subcircuit definition is stored once and referenced by N instances with bound parameters, instead of ten instances storing ten copies
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary judgment

This is an unusually rigorous issue: every file:line citation I spot-checked against the current tree (`src/jls/elem/SubCircuit.java:282,299,332`, `src/jls/Circuit.java:50,1479-1480,1664-1667`) lines up almost exactly with what is quoted, the O4 duplication numbers are reproducible arithmetic, and the scope-exclusion list (no width parameterization, no HDL export change, no import-to-subcircuit path) is explicit and enforced by a Definition-of-Done checkbox. The problems below are not "this issue is sloppy" — they are places where the issue's own stated rigor doesn't quite hold up against the code and against its own sibling issues.

## Findings, most severe first

**1. P4's load behavior for pre-split files silently commits to "auto-share by content," which the parent feature explicitly says is an unresolved, blocking decision — and the issue never mentions the conflict.**

`#357` (FEAT-017, the parent feature `#447` is a task of) states in its Open Questions: *"Does a pre-split file auto-share structurally identical copies on load? ... Recommended default: (a) never auto-share; offer it as an explicit, reported operation ... This is the decision that determines whether this feature is safe. **Blocks filing TASK-0041.**"* Its Definition-of-Done still lists this as unchecked: *"Open Question 1 ratified by a maintainer and its answer asserted by the test in §5.4, not merely implemented."*

`#447` §5 P4 says: *"Load a checked-in pre-split fixture whose `SubCircuit` bodies are inlined; observe it loads and that each inlined body is registered as a definition keyed by its digest."* §7.7 repeats it: *"pre-split files carry inlined bodies; the reader registers each as a definition keyed by digest (P4)."*

"Registered as a definition keyed by digest" is, functionally, auto-sharing by content: if a file has two pre-split `SubCircuit` instances with byte-for-byte-identical (post-canonicalization) bodies — which is precisely the common case this whole task exists to fix — registering each "keyed by digest" means the second registration collides with the first under the same key. The issue never states whether that collision (a) merges them into one shared definition (auto-share — option (c)/(b) that `#357` explicitly did *not* recommend), (b) is refused as a duplicate-key error, or (c) is avoided by keying pre-split bodies some other way. `#447`'s own six Open Questions never touch this; it directly reuses the vocabulary `#357` flagged as the single decision that "determines whether this feature is safe," without citing `#357`'s Open Question 1 or resolving it.

*Recommendation:* Either cite and explicitly resolve `#357` Open Question 1 inside this issue's own Open Questions section (it currently has none of the load-time auto-share language), or state plainly that P4's "registered ... keyed by digest" is registration-for-comparison only (each instance keeps its own definition entry; the digest is used solely to detect that two *identically-`defid`'d* entries agree/disagree, per Open Question 3) and is not content-based deduplication. As written, a competent implementer reading only `#447` could ship silent auto-share — the exact outcome `#357` calls unsafe.

**2. Open Question 3's "keyed by `defid`" answer is incompatible with pre-split files, which by construction have no `defid` — and the issue doesn't say what key they get.**

Open Question 3 recommends: *"keyed by `defid`, with the digest checked and a mismatch refused by name."* But a pre-split file predates `defid` entirely (it is inlined precisely because the reference mechanism didn't exist yet — `#417`, which mints `defid`, defaults it to `local:local:<nested-circuit-name>:0.0.0`, and O5 of this same issue establishes that the nested circuit's name is **the instance's name, not the definition's** — `"'CIRCUIT adder8' x 0"` in O4 is cited as proof of exactly this). So the default `defid` synthesized for two pre-split instances of one logical definition would differ per instance (different instance names → different synthesized `defid`), while P4 wants them to land in one digest-keyed table entry. The table's primary key (Open Question 4: canonical order "by `defid`") and P4's load-time key (digest) are two different keys for the same table, and the issue supplies no function reconciling them for the migration case. This is the same gap as Finding 1 seen from the data-model side, and it is exactly the kind of "two sources of truth" the issue itself warns against for `subElement` (H4/O5).

*Recommendation:* Add an explicit rule: how a pre-split body's synthesized `defid` is chosen (e.g., "first instance's name wins; later structurally-identical bodies with a different instance name are refused / kept separate / renamed with a recorded diagnostic") before implementation starts.

**3. The issue's own inventory of `Circuit.subElement` consumers (O5) is incomplete — a fourth, unexamined call site exists nine lines above the code it quotes.**

O5 quotes `Circuit.java:1479-1484`'s `FORMAT`-line branch and `setImported` as the two places `subElement` matters, and calls this "the single most invasive consequence of the split." But `Circuit.markChanged()` (current HEAD, `src/jls/Circuit.java:293-303`) also consults it:

```java
public void markChanged() {
    changed = true;
    index.invalidate();
    if (subElement != null) {
        subElement.getCircuit().markChanged();
    }
    ...
```

This propagates the "unsaved changes" dirty flag from a subcircuit *definition* up to "the" circuit that contains it — through the same single-valued `subElement` field the issue plans to replace with a set (Open Question 2, option (a), "recommended"). Once one definition backs N instances, "the circuit it is in" is ill-defined: marking a shared definition changed must now propagate to N containing circuits (or none, or the editor's dirty-flag semantics need to change entirely), and this call site is never mentioned in O5, in H4, in the Method checklist, or in the Threats to Validity section. H4 states "H4 refuted — it will not be; O5 shows `subElement` is consulted on the `FORMAT`-line branch" — treating O5 as an exhaustive inventory of `subElement` consultation sites when the inventory demonstrably missed one. This isn't a subtle miss: `markChanged` is a `@jls.testedby`-annotated, heavily-used method nine lines from the exact block O5 quotes.

*Recommendation:* Re-grep `git grep -n "subElement" src/jls/Circuit.java` (not just the two sites O5 names) before starting, fold `markChanged`'s propagation into Open Question 2's redesign, and add a prediction/test for "editing an instance of a shared definition marks the correct containing circuit(s) changed" — currently nothing in §5 or §8 covers this.

**4. This task's `blocked_by` list is machine-incomplete by the issue's own admission, and the missing prerequisite doesn't exist yet as a filed issue.**

`blocked_by: [417]` is the only ordering edge recorded, but §6 Materials states: *"Must exist first (frame): TASK-0033's section frame, which TASK-0040 already uses for libraries and which the `DEFINITION` section rides in. Not written as an ordering edge because its number does not exist at this filing."* The Method checklist's own step — *"Add the `DEFINITION` section in TASK-0033's frame"* — is therefore a step this task cannot execute today: it names a container format owned by an issue that has not been filed. §12 repeats this as a to-do ("A link pass must add it") rather than resolving it. Combined with Finding 1 above (an unresolved blocking design question on the parent feature) and the fact that `#417` (the one dependency that *is* recorded) is itself still open with none of its own predictions verified, a contributor picking this issue up today hits a hard stop within the first checklist item ("Re-verify O1–O9... Confirm #417 has landed") and a second, unaddressable stop at the `DEFINITION`-section step. That's expected in a dependency-staged backlog, but the issue's Status block presents `blocked_by: [417]` as the complete picture when it explicitly is not.

*Recommendation:* File TASK-0033 (or fold its minimal shape into this issue) before this issue is picked up for implementation, or add a visible "additionally blocked by: TASK-0033 (unfiled)" line rather than only a prose aside three sections later.

**5. The §7.9 concurrency safety claim ("nothing may mutate a definition from a simulation thread") has no enforcement mechanism inside this task's own scope, and the audit that would give it teeth is explicitly deferred to an unscheduled, unfiled "residual."**

`#357`'s own evidence section counts 25 `getSubCircuit()` call sites across 8 files, only 12 of which are inside `SubCircuit.java`; the other 13 include `BatchSimulator.java` (2), `InteractiveSimulator.java` (1), `CircuitRenderer.java` (1), `JLSStart.java` (3), and `SubCircuitDialog.java` (1) — several of them exactly the simulation/rendering code §7.9's safety sentence is worried about. `#357` classifies migrating all 25 as a separate, unfiled "residual" with no task id, explicitly outside both TASK-0041 (`#447`) and TASK-0042. `#447` H2 only estimates "~20 call sites in `SubCircuit` itself and in the editor" — undercounting and omitting the simulator/renderer/CLI sites entirely from its own risk accounting, even though those are precisely where a stale or concurrently-mutated shared `Circuit` reference would cause the worst failure (wrong simulation results, not just a bad save). §7.9 states the constraint as a documentation obligation ("must be stated... nothing may mutate a definition from a simulation thread") but names no test, no runtime guard, and no code-review checkpoint that would catch a violation before the (unscheduled) residual migration happens.

*Recommendation:* Either pull an audit of the simulator/renderer/CLI `getSubCircuit()` call sites into this task's Method section (even if the actual migration is residual work), or add an explicit acceptance item asserting no simulation-thread code path can currently reach a mutating `Circuit` operation through `getSubCircuit()` at the point this task closes — otherwise P3 ("must hold after: editing the definition changes every instance's simulation output") can pass in the new unit tests while a live thread-safety hazard ships to users running interactive simulations during an edit.

## What's solid

- The `formatVersionNeeded()` max-over-elements mechanism (O6) genuinely does support "bump `FORMAT` only for files that use the feature" (P5) — verified against current `src/jls/Circuit.java:1580-1587`.
- The three-copy-site inventory (O3) is accurate against current code and the fix strategy (reference-copy instead of deep-copy, keep `Util.copy`/`Util.partition` for the still-needed paste path) is coherent and correctly scoped.
- Explicit non-goals (width parameterization on 14 element classes, HDL export, import-to-subcircuit) are stated and defended with a real cost argument ("resisting the urge... is what keeps this two weeks"), and are checked in the Definition-of-Done rather than left as prose.
- P3's insistence on asserting through simulation output rather than bytes is a good anti-gaming discipline — it blocks the obvious false-positive of a save/load round trip that shares text but not the actual object graph.
- The atomic-rename / temp-file save guarantee (§7.11: "the previous complete file survives because `FileAbstractor` writes through a temp file and renames") correctly reuses an existing, verified property rather than re-inventing failure handling.

## Bottom line

The engineering is careful and the evidence is largely accurate, but the issue ships a load-time behavior (P4/§7.7's digest-keyed registration of inlined bodies) that reads as auto-sharing by content while its own parent feature (`#357`) flags exactly that question as unresolved and safety-critical, doesn't reconcile that behavior with its own recommended `defid`-keyed table design (Open Question 3), misses a real fourth consumer of the field it calls its most invasive obstacle (`Circuit.markChanged`), and defers the audit that would validate its stated concurrency safety claim to unscheduled, unfiled follow-up work. None of these are fatal to the design direction, but each would let an implementer land code that passes every literal test in §5/§8 while missing the actual goal (safe, non-surprising sharing).
