# Issue #465: TASK-0103: five semiconductor device families with stated parameter tiers and stamp goldens — and an inspector that says before simulation whether a downloaded vendor file will load
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is meticulously argued and internally cross-referenced, but two of its central claims collapse on contact with the actual repository: (1) the evidence commit it says every citation is "re-derived at" does not exist in this repository's history, and (2) its flagship "open question" — that the CLI cannot support a flag named `-inspect` and must be resolved before any code is written — is factually wrong about the codebase it is filed against. The rest of the design (device tiering, Gummel–Poon-degenerates-to-Ebers–Moll, the `icheck` limiter contract) is well-reasoned, but the issue is not executable as filed: its real hard blocker (TASK-0097) is an unfiled, non-existent issue, so nothing here can start.

## Findings, most severe first

### 1. (Blocking) O2's flagship claim — "`-inspect` is unreachable, must resolve before the flag is written" — is factually false about this codebase

The issue spends an entire numbered observation (O2), an Open Question ("Blocks execution"), and a Method step ("Resolve the flag-name collision of O2 before writing the `FlagSpec` row") on the premise that JLS's CLI parser only supports single-letter flags with attached operands, so a multi-letter `-inspect` flag can never be reached — "the multi-letter name the corpus proposes is unreachable."

This is contradicted by the flag table the issue itself cites. `src/jls/JLSStart.java:759-789` (14 `FlagSpec` rows, confirmed) already contains five multi-letter flags: `vcd`, `export`, `board`, `pins`, `savetext`. The parser's own doc comment (lines 750-756) states the mechanism explicitly:

> "When one flag name is a prefix of another (`-v` / `-vcd`), the longest match wins, so `-vcd` is the VCD flag, never `-v` with the attached operand `cd` (issue #72)."

And the parse loop (`JLSStart.java:849-858`) implements exactly that: it scans all `FLAGS` entries whose `flag` is a prefix of the argument body and picks the longest match. Adding `new FlagSpec("inspect", ...)` to the table makes `-inspect` resolve to that row over `-i` immediately — no design decision, no free-letter hunt, no CLI-grammar change. This is the *same* mechanism, already shipped, already tested (issue #72), for the near-identical `-v`/`-vcd` collision the issue's own committed code comments call out by name.

The measured command in O2 (`jls -inspect ... → error: option -i ... nspect`) is real, but it only demonstrates that no `FlagSpec("inspect", ...)` row exists yet — which is trivially true before this task adds one. It does not demonstrate unreachability. The issue's own "Recommended default" — hunt for "a free single letter" and treat introducing a multi-letter form as "a CLI-grammar change with its own compatibility surface" — is not just unnecessary, it contradicts an established, tested convention in the file it cites as evidence.

**Impact**: this is listed as a criterion that "Blocks execution" and gates a Method step and a Definition-of-Done item. An implementer following the issue literally would either stall on a non-problem or invent an unnecessary single-letter flag, burning a scarce single-letter slot for no reason.

**Recommendation**: strike O2's "unreachable" framing and Open Question 1 entirely. Replace with: "add `new FlagSpec(\"inspect\", Arity.REQUIRED, \"file\", \"a vendor library file\", ...)` to `FLAGS`, following the `-vcd`/`-export` pattern; `CliFlagTableTest` and the help-pinning tests already exist to catch any accidental collision."

### 2. (Severe) The evidence commit does not exist in this repository

Every "Observation" in the issue is prefaced with "All citations re-derived at `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`" and "`git diff --stat 2d0ca9d HEAD -- src/ test/` is empty." `git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` on this checkout returns "fatal: bad object" — the commit is unreachable in this repo's history (checked against `master` and all local/remote branches). The issue's own "branch HEAD at filing," `839fb3a`, is likewise not a valid object here.

Some individual claims do independently check out against current HEAD (no `src/jls/analog/`, `git grep` for gummel/shichman/mosfet/spice3f5 returns nothing, 14 `FlagSpec` rows, no `NOTICE` file, `HeadlessCoreRatchetTest`'s `BASELINE` is empty), so the factual substance of O1, O3(partially), O4 holds up independently. But an issue whose entire methodology is "measured, not assumed" and "re-derive before trusting" (its own words, § Threats to Validity) should not cite a specific SHA for verification and have that SHA be unresolvable. This makes every uncorroborated claim (parameter counts in O5/O6, the exact `icheck` line count in O7, the BSIM4 SLOC/parameter count) unverifiable by a reviewer or implementer working from this checkout, and is a bad precedent for a document this insistent on "arithmetic, not precedent."

**Recommendation**: re-pin to a commit that actually resolves in `anadon/jls`, or drop commit-pinning and cite `HEAD`/file paths only.

### 3. (Severe) The real hard blocker (TASK-0097) is not filed, has no issue number, and cannot be tracked

`blocked_by` in the YAML lists only `402`. The prose immediately below contradicts the machine-readable block: "this task is **also blocked by TASK-0097** ... TASK-0097 is not filed." The same is true of the referenced `#351` (FEAT-046) and `#331` (FEAT-049), both of which list TASK-0097 as "unfiled" in their own trackers. So the actual prerequisite for any of this work — `MnaMatrix`, the Newton residual/Jacobian types, the escape ladder every limiter feeds — exists nowhere as a trackable artifact. There is no way to check "is TASK-0097 done" other than re-reading prose in three different issues.

This is a real feasibility risk, not a paperwork nit: the Definition of Done includes "`blocked_by: 402` (TASK-0099) has landed, and TASK-0097 has landed, or the dependency was waived per rule 10" — i.e. the issue's own completion bar names a dependency that has no address. Anyone picking this up cannot even open the blocking issue to check its status.

**Recommendation**: file TASK-0097 before this issue is actionable, or explicitly mark #465 as un-startable and route it to a holding label until TASK-0097 exists.

### 4. (Moderate) `jls.analog` is not currently in the headless ratchet's policed package list — O3's claim of automatic inheritance is not yet true

O3 claims "`jls.analog` is created by TASK-0097 as a policed-from-birth leaf and `jls.analog.models` inherits that." Checked against `test/jls/HeadlessCoreRatchetTest.java`, `CORE_PACKAGE_PREFIXES` currently contains only `src/jls/sim/`, `src/jls/elem/`, `src/jls/hdl/`, `src/jls/module/`, `src/jls/core/` — no `src/jls/analog/` entry (unsurprising, since the package doesn't exist yet). This is consistent with the plan (TASK-0097 is supposed to add the prefix when it creates the package) but the issue states it as settled fact rather than as an obligation on TASK-0097 that #465 depends on and should verify, not assume, at pickup time.

**Recommendation**: add an explicit Method step confirming `src/jls/analog/` (and `src/jls/analog/models/`) actually appears in `CORE_PACKAGE_PREFIXES` before writing any device code, rather than assuming TASK-0097 did it correctly.

### 5. (Moderate) Underspecified/gameable acceptance criterion: P9's "warns once" is testable only for the single-parameter case actually written

P9 (`anUnknownSpice3f5ParameterWarnsOnceAndLoads()`) and Open Question 2 pin "once per parameter name per file." But nothing in § Predictions or § Falsification Criteria requires a test with *two* unknown parameters, or the *same* unknown parameter appearing on *two different device instances* in one file (the fifty-diodes-with-`KF`" example used to motivate the rule). A single-parameter, single-instance test can pass while a naive "warn on first unknown token globally, not deduped per name" implementation also passes, and a scoping bug (warn once *per file* vs. once *per name* vs. once *ever* across the whole process) would not be caught. The stated motivating scenario (fifty diodes) is never turned into an actual assertion.

**Recommendation**: require the golden/test fixture to contain at least two devices sharing an unknown parameter name and assert exactly one warning line for that name, plus a second unrelated unknown parameter name to assert warnings are keyed correctly.

### 6. (Moderate) H1/P3's "bit-identical" claim is asserted only for the case where `VAF/VAR/IKF/IKR` are *absent*, not where they are present-but-defaulted

Stage 1's algebra is airtight for absent parameters, and P3 correctly insists on bit-identity rather than tolerance (this is one of the issue's genuinely good calls — see Solid Points). But nothing in the predictions distinguishes "parameter key absent from the card" from "parameter present with an explicit value equal to the SPICE default" (e.g., a vendor card that writes `IKF=1e30` to mean "effectively infinite," a real-world pattern). If `MosCommon`/`BjtModel` implements defaulting by parsing every declared key into a `double` regardless of presence, a card that spells out the default explicitly could take a different code path than one that omits it, silently reintroducing the "genuine second model" H1 is designed to catch — and P3 as specified would not catch it, because P3 only exercises the omitted-key case.

**Recommendation**: add a second bit-identity assertion using a card that explicitly states `VAF`/`VAR`/`IKF`/`IKR` at their SPICE default values, not merely omits them.

### 7. (Minor) Scope-creep risk in Open Question 4 is correctly flagged but left "rides along" rather than excluded

Open Question 4 ("is the behavioral op-amp element in this task or a sibling?") is answered "recommended default: a sibling task," which is right, but the issue still leaves H6 ("op-amps cost zero incremental model work") and P11-adjacent material inside this task's falsification criteria. Since H6's own falsification move is "name the kind and cost it; do not invent an op-amp `.model` type, which is explicitly excluded" — good — but the measurement obligation (checking the inspector's output "over the corpus" per § Threats to Validity) has no owner or deadline, and could be quietly skipped since it isn't in the Definition of Done as a hard line item, only as prose in § Threats to Validity.

**Recommendation**: promote the "op-amp free" corpus re-check to an explicit Definition-of-Done checkbox, or drop the claim from this issue's scope since it's admittedly single-sample evidence (one transimpedance amplifier).

### 8. (Minor) Licensing groundwork (D8) is sound but the issue defers the actual attribution audit without a named owner

The GPL-3.0-or-later / BSD-absorption reasoning (O4, D8) is correct — verified against `LICENSE` and `pom.xml` (`GNU General Public License v3.0 or later`). Creating `NOTICE` is a real and correctly-scoped deliverable (P14). However, the issue never names who verifies that the *specific* 156-line limiting apparatus being ported is actually BSD-licensed at its source (ngspice's `spice3f5` limiting code has a documented, occasionally contested licensing history across forks) — it's asserted as fact ("the 156-line Modified-BSD limiting apparatus") without a citation to the specific upstream file/commit/license header being absorbed. Given D8's own emphasis that "the genuine hazard is narrow and specific: GPL-INCOMPATIBLE licenses," this is the one place a license audit is load-bearing and it's under-specified.

**Recommendation**: name the exact upstream source file(s) and their license header/commit hash for the limiting apparatus before Method step "Port `Limiting.java` first" begins.

## Points that are solid (no action needed)

- H1/P3 correctly insists on bit-identity rather than a tolerance for the Gummel–Poon/Ebers–Moll degeneracy — a tolerance-based test would hide exactly the defect class it exists to catch. Good design.
- O7/P4/H4's `icheck` "limited step forces another Newton iteration regardless of residual" framing correctly identifies the worst failure mode (silent, reproducible, wrong answers) and requires a dedicated adversarial test rather than trusting the port by inspection.
- Sizing parameter tiers against SpiceSharp's 15/143-ish counts rather than ngspice's 88/143 (O5/O6) is a reasonable, arithmetic-backed scope boundary consistent with D8's "scope the models, not the solver."
- D-A14 (libraries are data, not curated/redistributed) and the corresponding "committed redistributable fixture, licence recorded" requirement (P12, Open Question 3) correctly anticipates a real distribution/licensing trap for a teaching tool that reads vendor files.
- Deferring MOSFET level 3 with a stated re-entry cost (1.5–3.0 mw) and asserting the refusal with a named diagnostic (P8) rather than silently failing is a defensible, well-specified deferral.
