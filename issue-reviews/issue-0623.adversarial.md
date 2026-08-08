# Issue #623: TASK-C490-3: four terminations, two independent assertions — 5.500 V open, 3.300 V series-terminated, and the golden checked against the closed-form lattice to 1e-12
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Task 3 of 4 under FEAT-059 (#490), itself rung 3 of capstone CAP-18 (#313). It wants a
golden test (`ReflectionGoldenTest`) over four canonical transmission-line terminations,
checked two ways — byte-identical to a golden fixture and, separately, against the
closed-form reflection lattice to 1e-12 relative — plus a second test
(`EdgeRateCollapseTest`) that pins the "regime is entered by edge rate, not clock rate"
lesson at three edge rates. Nothing in `src/` or `test/` implements any of this yet
(confirmed: `grep -rl "TransmissionLine|ReflectionGoldenTest|EdgeRateCollapseTest" .`
returns nothing outside issue-review files).

## Findings, most severe first

**1. [CRITICAL] Both declared prerequisites are unbuilt — this task cannot be started, let alone landed.**
`ordering_after: ["TASK-C490-1", "TASK-C490-2"]` names the element itself (#618, confirmed
`state: open`) and the real-valued trace row (#620, confirmed `state: open` via
`search_issues`). Verified directly: no transmission-line element class exists in
`src/jls/elem/`, and `docs/simulation-semantics.md:44` (cited by #490) still admits only a
`BitSet` or null — there is no real-valued waveform anywhere in the codebase for a golden
test to assert against. Filing/reviewing this task now, with both inputs it needs still
nonexistent, invites it being picked up and built against a moving or absent target — the
same defect issue #625's review already found in its own sibling task. **Recommendation:**
mark this explicitly blocked (not just documented in YAML) or hold it out of the open-issue
list until #618 and #620 land.

**2. [HIGH] "Four canonical terminations" is asserted but only three are ever given numbers, anywhere in the cited chain.**
AC-1 requires the golden to cover "unterminated, series, parallel and a mismatched load"
(the phrase is inherited verbatim from #490's abstract). This issue's own Outcome section
works two configurations in full: the open far end with `R_s = 10 Ω` (the 7-term ring:
5.5000, 1.8333, 4.2778, 2.6481, 3.7346, 3.0103, 3.4931 V) and `R_s = 50 Ω` into the same
open end (flat 3.3000 V). Checking #490's own "Transformations" section, the only other
worked case is `R_L = 50 Ω` producing "2.7500 V flat" — that is a matched *load*
termination (`Gamma_L = 0`), not "a mismatched load." Nowhere in #623, #490, or the #313
excerpts fetched for this review is there a stated `R_s`/`R_L` pair, expected waveform, or
expected settling value for a genuine mismatched-load case (both ends reflecting, neither
matched, settling away from `V_src` at a ratio that isn't 1:1). AC-1 tells the implementer
to golden a fourth termination that no document in this chain has ever computed a number
for. **Recommendation:** either #490/#623 needs an explicit worked mismatched-load example
(parameters + expected V_k series) before this task starts, or AC-1 should be corrected to
name the three terminations that actually have derived numbers.

**3. [HIGH] The "separate, independent assertion" in AC-1/AC-3 has no code-independence requirement, so it can be satisfied in a way that defeats its own stated purpose.**
AC-3 is admirably honest that the 1e-12 check "catches transcription, regeneration and
floating-point-ordering errors, not modelling errors... since the implementation *is* the
closed-form superposition." But neither AC-1 nor AC-3 requires the test's reference
computation to be written independently of the production element's kernel. As stated, an
implementer can satisfy "two separate assertions" by calling the *same* `Gamma_s`/`V_k`
method from both the golden-comparison code and the "analytic cross-check" code — at which
point the second assertion is not independent at all, it is the first assertion computed
twice through one code path, and it would pass even if that shared method were wrong (the
scenario AC-1's last sentence — "the golden may never be regenerated to make the analytic
check pass" — is explicitly trying to prevent, just from the wrong angle). **Recommendation:**
state explicitly that the closed-form lattice must be reimplemented independently in the
test (e.g., hand-coded `Math.pow(gammaS, k)` arithmetic in the test file itself, not a call
into the element's evaluation method) — otherwise "two assertions" is one assertion wearing
a second hat.

**4. [MEDIUM] `EdgeRateCollapseTest`'s third data point is an unbounded inequality, which under-constrains the test.**
AC-2 states peaks of "166.7%, 132.3% and under 105%" for `t_r` = 50 ps / 1 ns / 5 ns. The
first two are exact target values (presumably with a numeric tolerance); the third is
open-ended — anything from 0% to 104.999% passes. A regression that collapsed the whole
edge-rate model to a flat, non-physical near-rail response (say 100.0%) would satisfy "under
105%" exactly as well as the intended near-critically-damped ~102–104% response would. Given
this test's stated purpose is to be "the test that fails if the source waveform is ever
silently idealised back to a step" — an idealized step at `t_r = 5 ns` would actually produce
close to 100% too, so the loose bound doesn't even obviously catch that failure mode.
**Recommendation:** give the 5 ns case a lower bound as well (e.g., "100.5–105%") or a
concrete target value with a stated tolerance, consistent with how the other two points are
specified.

**5. [MEDIUM] "Recorded as superseded where a reader will find it" (item 5) names no location.**
The corpus-correction requirement — that the CAP-18 document's erroneous 3.1914 V fourth
ring value be recorded as superseded rather than silently dropped or reproduced — is good
practice, but "where a reader will find it" is not a location a reviewer or CI can check.
Compare AC-1/AC-2, which name concrete test classes. A minimal compliant implementation
could satisfy this with one buried code comment nobody reads while a public-facing golden or
javadoc still silently reproduces (or silently drops) the 3.1914 figure. **Recommendation:**
name the artifact — e.g., "the superseded value and its correction appear in
`ReflectionGoldenTest`'s class javadoc" — the same discipline AC-3 already applies to what
the second assertion buys.

**6. [LOW] K18-1's REPLAN list omits the task that actually gates on it.**
Item 4 says a K18-1 failure means "REPLAN on #490 and #313," which is correct at the
feature/capstone level, but the concrete downstream consumer of this exact gate is
TASK-C490-4 (#625, per its own `ordering_after: ["TASK-C490-1", "TASK-C490-3"]"`, confirmed
in that issue's review). Naming #625 explicitly here would save a reader from having to
re-derive, from #490's dependency graph, which sibling task is actually waiting on this
verdict. Minor, since the REPLAN will cascade through #490 regardless.

## What's solid

- **The physics is verified correct.** I independently recomputed
  `V_k = 3.3(1 - (-2/3)^k)` for k=1..7 and reproduced 5.5000, 1.8333, 4.2778, 2.6481,
  3.7346, 3.0103, 3.4931 V exactly, matching the issue's stated series and its correction of
  the CAP-18 document's 3.1914 V (confirmed: `3.1914/3.3 = 0.96709`, not `1-(-2/3)^k` for any
  integer k, so the correction is legitimate, not merely asserted).
- **AC-3's honesty about what the 1e-12 check actually proves** (a round-trip bound, not a
  physics bound) is a genuinely good practice other acceptance criteria in this fleet lack —
  it heads off the obvious "why does an exact-agreement check mean anything" objection,
  modulo finding 3 above.
- **AC-4's stop condition is a real safety valve**, correctly routed to REPLAN rather than
  letting a implementer patch around a genuine modelling bug by tuning constants.
- **Test-naming convention matches the existing suite** — `ReflectionGoldenTest`/
  `EdgeRateCollapseTest` sit naturally alongside `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, and `VcdExportGoldenTest` already in `test/jls/`.

## Bottom line

The arithmetic this issue rests on is correct and its honesty about the limits of a
same-implementation cross-check is a strength, not a weakness — but the task is unstartable
today (both named prerequisites are open with zero code landed), its golden corpus is missing
a fully specified fourth case ("mismatched load" is named, never computed, anywhere in the
chain), its independence claim for the "separate assertion" has no enforcement mechanism, and
one of its two headline numeric criteria (the 5 ns edge-rate bound) is loose enough to pass a
broken implementation. Needs the mismatched-load numbers filled in and the independence/bound
gaps closed before anyone should pick it up, in addition to waiting on #618/#620.
