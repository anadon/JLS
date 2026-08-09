# Issue #492: HDL export policy is not total over the element registry: an element type nobody classified is rejected with no reason, indistinguishable from a deliberate refusal
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what checks out

The forensic claims are, unusually for this kind of write-up, verifiably true against the actual working tree, not just the cited commit:

- `EXPORTED`/`SKIPPED`/`TOPOLOGY` and the reason-free `offenders.add(describe(el))` fall-through exist exactly as quoted (`src/jls/hdl/HdlExporter.java:422-463`, loop at `:176-197`). Line numbers are off by 1-6 from the issue's `421-438`/`175-197`, consistent with minor drift since the pinned `evidence_commit`.
- The registry really has 35 entries (`src/jls/elem/ElementRegistry.java:38-77`, counted directly), and `FieldExtend`, `Memory`, `RegisterFile`, `SubCircuit` are exactly the four with no bucket membership (verified by counting `EXPORTED`=22 + `SKIPPED`=6 + registered `TOPOLOGY` members (`WireEnd`,`JumpStart`,`JumpEnd`)=3 = 31; 35−31=4, matching the issue's set exactly).
- H4 (`Wire.class` in `TOPOLOGY` but not registered) is correct — `ElementRegistry.ALL` has no `Wire` entry, only `WireEnd`.
- `HdlPolicyTest.java` has no `ElementRegistry`/`ElementType` import and no registry-driven test (O5), and `memoryIsRejectedByName`/`subCircuitIsRejectedCleanly` (actual lines 63/77) assert with `.contains(...)`, not equality, so P4's "survives unmodified" claim is plausible.
- `HdlCircuitBuilder` genuinely has no `registerFile(...)`/`fieldExtend(...)` helper today (grepped, zero hits) — the issue correctly flags these as new work rather than assuming them.
- The issue is unusually self-auditing: its own comment (2026-08-08) re-derives every observation against `master` independently and catches two of its own claims propagating as false "already shipped" facts into #385/#384 (both since closed as duplicates). That is a point in its favor, not a hedge to discount.

## Findings, most severe first

### 1. The inlined "fix" diff ships an unverified workaround that the issue's own Open Questions and its own comment say must not ship yet — a direct self-contradiction

The issue's abstract insists the diff is authoritative ("the diff and its regression test are inlined here in full ... rather than linked"). That diff's `REJECTED` map (§8) includes, verbatim, for `FieldExtend`:

> `"field extend is not exported yet - it is a straightforward sign/zero extension and is expected to become exportable; until then, build it from Splitter, Binder and Extend"`

But Open Question 2 in the same issue body says: *"verify `FieldExtend`'s workaround before shipping it, since a wrong workaround in an error message actively misleads... Recommendation: do (b) for `FieldExtend` specifically. **Blocks execution** of that one map entry, not the rest."* The issue's own follow-up comment sharpens this further: *"Until AC-3 is discharged, land that one map entry **without the workaround clause**."*

So the issue simultaneously (a) hands the executor a copy-pasteable diff containing the unverified clause, and (b) tells them, buried three sections later, not to ship that exact clause. An executor who follows the "inline diff is the fix" framing and stops at §8's checklist will ship precisely the text the issue itself says is blocked. Nothing in the acceptance tests (P2/P3, `HdlPolicyTest`) checks the *content* of a reason string, only that one exists — so this can pass every stated check while shipping the forbidden text.

**Recommendation**: strip the workaround clause from the `FieldExtend` entry in the diff itself (not just in prose elsewhere) before this issue is picked up, or add an explicit `DoD` line item that fails review if the diff-as-written is applied unedited.

### 2. `SubCircuit`'s proposed reason tells the user to do something JLS has no feature for, and receives none of the scrutiny `FieldExtend`'s workaround gets

The proposed `REJECTED` reason for `SubCircuit` is: *"subcircuits cannot be exported yet ... flatten the circuit to export it"*. I grepped the tree for any flatten/inline/expand capability on `SubCircuit` in the editor (`src/jls/elem/SubCircuit.java`, `src/jls/edit/*.java`): there is none. The only "flatten" functionality in the codebase is Yosys-flatten advice given to users on the *import* side (`src/jls/hdl/imp/NetlistImporter.java:158-159,230`), unrelated to exporting a JLS-drawn circuit. A user reading this refusal has no button, menu item, or documented procedure in JLS to "flatten the circuit" — they would have to manually redraw the subcircuit's contents inline, which the message does not say.

This is the same failure mode Open Question 2 worries about for `FieldExtend` ("a wrong workaround in an error message actively misleads because it looks authoritative") but the issue applies that scrutiny asymmetrically — only to `FieldExtend`, not to `SubCircuit`, even though `SubCircuit`'s "workaround" is arguably worse: it names a feature that doesn't exist at all, versus `FieldExtend`'s decomposition, which at least corresponds to real elements (`Splitter`, `Binder`, `Extend` all exist and `FieldExtend`'s own javadoc, `src/jls/elem/FieldExtend.java:24-27`, describes itself as "exactly the immediate-generator/ALU boilerplate (Splitter + Extend + Binder) a datapath otherwise hand-wires" — so that one has independent textual corroboration in the codebase that `SubCircuit`'s does not).

**Recommendation**: either drop "flatten the circuit to export it" from the `SubCircuit` reason (state only that hierarchy isn't supported yet, per #385/#384/#358), or verify a manual flatten procedure exists and name it precisely, with the same rigor applied to `FieldExtend`.

### 3. "The fix is contained in `HdlExporter` plus one new test" (`blocked_by: []`) is in tension with the verification work the issue itself makes a landing gate

The dependency block states `blocked_by: []` and the abstract calls this a self-contained fix. But Open Question 2 makes verifying the `FieldExtend` workaround (build a fixture circuit using `Splitter`+`Binder`+`Extend`, simulate it, and demonstrate equivalence to a native `FieldExtend` across sign/zero-extend and multiple widths) a blocking precondition for that one map entry, and the DoD repeats this ("`FieldExtend`'s named workaround has been verified, not assumed"). That is meaningfully more work than "one new test" — it needs its own fixture corpus and a semantic-equivalence argument, not a unit test of `HdlExporter`. It is not a blocker in the dependency-graph sense (no other issue must land first), but it is scope beyond what "contained" suggests, and an estimator reading only the abstract/dependency block would underprice the ticket.

**Recommendation**: either explicitly scope the equivalence verification out of this issue (ship the reason without the workaround clause, as the comment says, and let a successor issue verify + restore it), or size the issue to include the verification work honestly.

### 4. Acceptance criteria for reason *correctness* are entirely unenforced — a subtly wrong or stale reason passes every test

The issue's own Threats to Validity says: *"The reasons are prose and nothing verifies their truth... no test will notice"* when `SubCircuit`'s reason goes stale after #385 lands module instantiation. This is honest, but it means P2/P3 (and the DoD's "per-type message assertions") only pin that *a* reason clause is present and contains the element name — not that it is accurate, actionable, or non-misleading. Combined with finding #2 above (the `SubCircuit` reason is already inaccurate at filing time, not just eventually), the "gameable acceptance criteria" risk is not hypothetical — it has already manifested in the diff the issue ships.

**Recommendation**: at minimum, add a completion-criterion reviewer checklist item requiring a human to manually reproduce every named workaround (not just read the sentence), mirroring what's already required for `FieldExtend`.

### 5. `RegisterFile`'s reason is presented as a considered decision but is explicitly conceded, in the issue's own follow-up comment, to likely be wrong

The `REJECTED` entry for `RegisterFile` says multi-port storage "maps to a technology-specific primitive," mirroring `Memory`'s reason. The issue's own comment (§5) says: *"`RegisterFile`'s reason is a copy of `Memory`'s claim, and #291 exists to refute that claim for the single-port case."* So the text this issue proposes to ship as an authoritative user-facing refusal reason is already known, by the issue's own later self-review, to be probably restatable or wrong once #873/#291 land. Because #492 is `blocked_by` for #873 (i.e., #492 must land first), there will be a real window where users are told something the maintainers already suspect is inaccurate. This is a reasonable interim state given the sequencing, but the issue should say so explicitly rather than presenting the reason as settled (Open Question 2 discusses only `FieldExtend` in these terms, not `RegisterFile`, despite equally strong grounds).

**Recommendation**: soften the `RegisterFile` reason to note the open design question explicitly ("a technology-specific primitive is assumed necessary; unconfirmed — see #873") rather than stating it as settled fact, or reference #873 in the string per Open Question 3's own follow-up plan.

### 6. Minor: the DoD is large enough (14 top-level items, several with sub-bullets, cross-referencing §5/§7/§10/§12) that partial compliance is easy to rationalize as "good enough"

Not a defect in the technical content, but a process risk: a checklist this long, covering falsification criteria, interface-contract re-verification, manual GUI verification with platform recording, and a mutation test (P6) pasted into the PR, invites an executor (especially an LLM agent, which this repo explicitly plans for) to satisfy the easy, mechanically-checkable items (green `mvn verify`) while quietly skipping the harder-to-automate ones (P6's mutation check, the manual GUI read-through, verifying `FieldExtend`'s workaround). The issue does provide `WAIVED:` comment discipline for skipped items, which mitigates this, but only if a reviewer actually cross-checks the PR against all 14 items rather than trusting a green CI badge.

**Recommendation**: no textual change needed; flag for the reviewer of the eventual PR to explicitly check off each DoD line rather than rely on CI status alone.

## Items that are solid, briefly

- Totality direction (`R ⊆ C`, not set equality) is correctly justified by H4/`Wire.class` and matches the actual registry contents — not over- or under-specified.
- `P4`/`P6`'s mutation-check discipline ("remove one class, confirm the test names exactly that type") is a genuine, cheap guard against a vacuously-green totality test, and is good practice given #315/#372's own observation that "a check that has never been seen to fail is vacuous."
- The scope boundaries (not exporting the four types, not touching #372's GUI tables, not building #375's reusable base) are drawn crisply and consistently with the sibling issues' own stated ownership — no scope-creep into those adjacent tasks.
- The `null`-reason retained arm (§7.10, for unregistered/plugin classes) is a real and correctly-reasoned safety property, not an afterthought.
- Disjointness (Open Question 1) is reasonably deferred as non-blocking, and the recommendation (add it, it's cheap) is sound.
