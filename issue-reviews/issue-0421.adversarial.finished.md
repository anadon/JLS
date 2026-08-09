# Issue #421: TASK-0066: a subcircuit instance's implementation can be switched mid-run and the continuation is either byte-identical or a named divergence with an index and a port
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## 1. [HIGH] The normative document this task implements does not exist in the repository

§1 (Background) states as fact: "`docs/parity-contract.md` exists at `2d0ca9d` **in draft**... This task implements §5.2's harness and §5.3's null test." The parent feature #325 goes further, quoting exact prose and line ranges (`docs/parity-contract.md:3-7`, §2.2 `:132-142`, §3 `:262`, §4 `:402`, §5.2 `:479`, §5.3 `:514`, §6 `:572`) and a specific commit (`b299d63`) that supposedly demoted it to unratified.

I checked: `docs/parity-contract.md` is **absent** from the working tree (`Glob docs/parity-contract.md` → no match; `Glob docs/*.md` lists 25 other docs, none of them this one; `Grep parity-contract` across the repo finds only prior reviewer output files, no source hits). There is no way to verify what §5.2/§5.3 actually require, what the "observation function," "coverage rule," "refusal set," or "abstraction banner" (all explicitly delegated to this absent document — "the parent feature #325 owns [these]; this issue does not restate them") actually say.

This is not a cosmetic gap. §1 explicitly disclaims restating the contract on the theory that #325 owns it and cites it verbatim; #325 in turn treats the document as load-bearing normative text with pinned line numbers. If the document does not exist for the executor to read, the only place the semantics are actually written down is §7.10's math in *this* issue — which conflicts with the issue's own delegation structure. A contributor cannot "reference, never restate" a document that isn't there. Open Question 1 ("Is `docs/parity-contract.md` ratified by landing this harness?") is unanswerable as posed: you cannot ratify a document that doesn't exist at the pickup checkout.

**Recommendation:** Before this task is picked up, someone must resolve whether `docs/parity-contract.md` was ever actually committed (and is missing due to a bad pin/rebase) or was never committed at all. If the latter, this issue's dependency chain on it (Open Question 1, the "referenced not restated" delegation) needs rewriting to make §7.10 the sole normative source, and #325 needs the same correction.

## 2. [HIGH] Evidence commit is unreachable in this checkout

Every O1–O9 observation and every code quote is pinned to `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`. `git show 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails with `fatal: bad object` — the commit is not in this repository's history at all (current HEAD is `bd54461`, a "Checkpoint: issue review snapshots" commit). Method step 1 ("Re-verify O1–O9... re-derive line numbers if HEAD has moved") anticipates *drift*, not *unreachability*. As it happens, spot-checking O2–O9 against the current tree shows the quoted code is accurate at today's line numbers (`SubCircuit.java:33-35,621-652`, `Simulator.java:165-243`, `SigGen.java:168-173`, `Memory.java:1395-1397`, `pom.xml:452-470,812-813`, `HeadlessCoreRatchetTest.java:74-90` all match), and the `imported`-guard claim about `Clock`/`Stop`/`Pause`/`Display` (O5) also checks out (zero grep hits in all four files). So the technical content is trustworthy even though the cited commit hash cannot be resolved — but that's luck, not verification the issue itself provides for. A future executor who takes "re-verify O1–O9" literally has no commit to diff against.

## 3. [MEDIUM] P9's leaking-element criterion is gameable by design

P9 requires: "Each of `Clock`, `Stop`, `Pause` and `Display` inside a boundary produces either a **named refusal** or a documented accepted loss — enumerated, one test per element." Open Question 2 recommends refusing `Stop`/`Pause` (they reach the simulator directly — confirmed at `docs/simulation-semantics.md` §11) and accepting `Clock`/`Display` as documented losses.

But the *recommendation* is explicitly non-binding ("Recommended default") and the actual completion criterion only requires "a per-element refusal or documented accepted loss and a test each" — it does not require the *correct* choice per element. A "test" that merely asserts "a comment exists documenting X as an accepted loss" is trivially satisfiable regardless of whether that choice is semantically sound. In particular, nothing in §5/§9/§14 stops an implementer from declaring `Stop` or `Pause` an "accepted loss" too — which would be actively wrong (they reach the simulator directly, so their leak silently changes what the surrounding run does) but would still satisfy the letter of P9 as written. The Open Question's recommendation needs to be promoted to a hard requirement in §14, not left as a "rides along"-style suggestion sitting one level below a load-bearing correctness distinction.

**Recommendation:** Make "`Stop` and `Pause` MUST be named refusals; `Clock` and `Display` MAY be documented accepted losses" a completion-criterion sentence, not an Open Question recommendation, and require the P9 tests to assert on which disposition class each element got (refusal-with-diagnostic-text vs. accepted-loss-with-recorded-cost), not merely that some disposition exists.

## 4. [MEDIUM] "Zero events posted" (P4) is ambiguously scoped against the engine's own seeding behavior

O7 establishes that `initSimulation` posts time-0 events for every element as part of ordinary DUT bring-up (`SubCircuit.initSim` at `:592-600`, the outer walk at `Simulator.java:196-200`), and §7.8 explicitly relies on this: "reset happens once, at `initSimulation`..., which also hands the index-0 observation for free." P4 then asks the harness to assert **zero events were posted** when ports mismatch, "before any simulation." It is not stated whether "zero events" means (a) `compare()` must detect a port mismatch before ever calling `initSimulation` on either DUT (i.e., before any of O7's ordinary seeding fires), or (b) zero events beyond whatever `initSimulation`'s own bookkeeping naturally produces. §7.10's ordering — `PortMismatch` evaluated before `Inconclusive`/`Divergent`/`Equivalent`, "before any event is posted" — reads like (a), which requires that a `Boundary`'s port list be obtainable from the loaded `Circuit`/`SubCircuit` instance without ever calling `initSim`/`initSimulation`. Nothing in O2 (which only describes `inmap`/`outmap` as fields, not their population point relative to `initSim`) confirms that ports are actually available pre-`initSimulation`. If they are not, P4 as stated is either unimplementable without a new introspection path (contradicting H2's "no new restriction" framing) or the "zero events" bar will quietly become interpretation (b), which is a materially weaker claim than the prose promises.

**Recommendation:** Pin down, with a file:line citation, where `inmap`/`outmap` (or their sizes/port names) become available relative to `SubCircuit.initSim`, and state explicitly in §7.10/§7.11 whether "zero events posted" is measured against a DUT that has never had `initSimulation` called at all.

## 5. [MEDIUM] Hard blocking dependency is unmet, and the "waiver" escape hatch undercuts the falsification discipline elsewhere in the issue

`blocked_by: [389]`, and #389 (fetched) is **open**, not landed — it hasn't even resolved its own four blocking Open Questions (manifest stdout/stderr, `implDelay` default, `FIDELITY` directive syntax, `-fidelity` per-instance form). Method step 1 allows proceeding anyway "per rule 10" waiver. That's a reasonable escape hatch in isolation, but it sits awkwardly next to how strict the rest of the issue is about falsifiability — e.g. §10's "H4 refuted — the hard stop... stop: do not proceed" and P2's "the single most important test in the task." A waived blocker means `Dut(Circuit, instancePath, implId)` and "two `Dut`s distinguished by `implId`" (both load-bearing in §7.3/§12) may have to be built against a `SubCircuitImpl`/`implId` shape that doesn't exist yet or changes shape after #389 actually lands, which risks rework of the very state-encoding (§7.7) this task calls "designed once" for #363 to consume later.

**Recommendation:** Either land #389 first, or make explicit in this issue what a waived-dependency implementation is allowed to assume about `implId`'s type/shape so the rework risk is bounded rather than open-ended.

## 6. Solid parts (one line each)

- O2/O3 (the `inmap`/`outmap` boundary and the exact `react`/`send` methods) are accurate and the "no new restriction" framing (H1) is well-supported by the actual code.
- O4's claim that `runUntilQuiescent` is "the existing loop body plus one termination condition" is accurate against `Simulator.java:215-243` and is a genuinely minimal, well-scoped engine change.
- O5's leaking-element enumeration (verified: zero "imported" hits in `Clock.java`/`Stop.java`/`Pause.java`/`Display.java`) is measured, not asserted, and is a legitimately useful red flag for scope.
- §11's self-critique (harness-correctness-is-the-whole-risk, budget-tuning risk, coverage-floor cost) is unusually honest for a spec document and pre-empts several complaints a reviewer would otherwise raise.
- The null-toggle-as-hard-stop discipline (H4/§10) is a good falsification design: it correctly refuses to let any comparison result be reported once the state-mapping identity fails.

## Note on scope

Everything else in the issue — the `runUntilQuiescent` design, the `Verdict` sealed-arm structure, the reflective/bytecode tests (P6/P7), the coverage-floor cost warning — is internally consistent and reasonably scoped to "two weeks." The severity here is concentrated in the citation integrity problem (finding 1) and its downstream effects (findings 3–4), which is why the verdict is needs-rework rather than sound-with-concerns: a reviewer cannot sign off on a task whose primary normative source cannot be located and re-checked.
