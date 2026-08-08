# Issue #560: FEAT-C28-4: JLS, Digital and Logisim-Evolution run the same workloads on the same machine with a published harness — and the reported table includes at least one workload a competitor wins
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

A committed, documented harness runs JLS, Digital, and Logisim-Evolution on the same
workloads on the same machine, publishes an honest table (including at least one
workload a competitor wins), and states each competitor's configuration fairly.
`ordering_after` names FEAT-C28-1 (#554, the JLS-side benchmark suite) and FEAT-C28-2
(#555, the methodology doc). This is PF-4 of capstone #512 CAP-28.

## Findings, most severe first

**1. A cited dependency is already dead — the "engine stack" reference in this issue's own boundary notes is stale.**
The boundary notes say: "**#476 TASK-0063 / #475 TASK-0056** are the engine stack
expected to move JLS's side of the table; re-running the comparison after they land is
a re-publication under FEAT-C28-3's staleness discipline." As of today, `#475
TASK-0056` is **closed, `state_reason: "duplicate"`** (closed 2026-08-08, the same day
as this review). #560 offers no pointer to whatever issue superseded it. Anyone
picking up #560 later and trying to decide "has the engine stack landed yet, do I
need to re-run the comparison" will hit a closed-as-duplicate issue with no forwarding
address inside #560 itself. This is exactly the kind of citation rot the project's own
"KC-36-1 / stale claims are pulled, not defended" discipline (see #520) warns about —
except here it's an internal cross-reference, not a competitor claim.
*Recommendation:* before work starts, resolve #475's duplicate target and either
update #560's boundary note or add a redirect comment.

**2. AC-3 ("at least one workload a competitor wins") is satisfiable without ever running the flagship workload the whole capstone exists to answer.**
CAP-28's Kill Criterion KC-28-1 — quoted approvingly in #560's own Outcome — is about
"the flagship workload" (the RV32I CPU / `k2000`-class fixture) specifically: "If the
head-to-head shows JLS >5x behind Digital on the flagship workload... publication
proceeds anyway." But #560's AC-2 only requires "the same workloads" across the three
tools, without requiring the flagship CPU-scale fixture to be one of them, and AC-3
only requires "at least one workload where a competitor wins" — anywhere in whatever
workload set is chosen. Nothing stops an implementer from comparing three small,
JLS-favorable circuits, letting Digital "win" on some minor metric (e.g. GUI redraw
latency, which is irrelevant to simulation throughput), and closing AC-3 while never
running the one comparison (JLS vs. Digital's published "120 kHz simulated processor
clock") that #512's evidence line and this issue's own Outcome paragraph exist to
settle. The stated verification (three ACs about workload parity and one honest loss)
can pass while the real goal (an honest answer to Digital's 120 kHz claim) is dodged.
*Recommendation:* add an AC requiring the workload set to include a CPU-scale fixture
comparable in kind to the one behind Digital's published clock-rate claim, not merely
"the same workloads across all three, whichever those turn out to be."

**3. Feasibility gap: no evidence either competitor exposes a scriptable, GUI-free "run N cycles as fast as possible" mode comparable to JLS's `-b -t` batch mode — and this project's own tooling is deliberately GUI/X11-averse.**
ARCHITECTURE.md and README are explicit and repeated on this point: "X11 is
deliberately not part of this project's tooling: no X server, no XWayland, no X11
utilities" (README, "Optional development tools"), and CI/the dev container are
headless-by-construction. JLS's own batch interface (`-b -t`, `docs/batch-interface.md`)
is what makes "same workload, same machine" tractable for JLS's own side. AC-2 assumes
that same tractability exists for Digital and Logisim-Evolution ("with hardware,
versions, and settings stated for each") without establishing that either tool has a
documented, scriptable headless throughput-benchmark mode. If either tool's fastest
honest path is "drive the GUI and time it," the "same workload on the same machine"
claim risks silently comparing JLS's optimized headless loop against a competitor's
GUI-interactive loop — passing AC-2's letter while violating AC-4's "no strawman
setups" in spirit. This also means the harness likely cannot run inside this project's
own headless CI/dev container, which the issue never flags as an open question.
*Recommendation:* AC-2 or AC-4 should require the issue (or its implementer, before
starting) to record how each competitor is driven to a comparable headless/batch mode,
or explicitly document that GUI-driven timing was unavoidable and why that is still fair.

**4. AC-1's "a third party can re-run the full head-to-head on their own machine" conflates two different reproducibility claims and is easy to satisfy on paper without being useful.**
AC-2 requires all three tools to run "on the same machine" (i.e., the *original*
measurement is single-machine, controlling for hardware variance). AC-1 then asks that
"a third party can re-run the full head-to-head on their own machine" — necessarily a
*different* machine from the original. Nothing in the ACs requires the relative
ranking (which tool is faster on which workload) to be stable across that hardware
change, only that the harness executes. A harness that runs cleanly on a third party's
laptop but produces a different competitor-wins-here result than the published table
would technically satisfy AC-1 while undermining the table's credibility — and no AC
catches that divergence. *Recommendation:* AC-1 should require the harness to report
relative ratios (JLS/Digital, JLS/Logisim-Evolution) as the primary artifact, with
absolute numbers as a secondary column, since ratios are what plausibly transfers
across hardware.

**5. AC-4's "fair... no strawman setups" and AC-1's "documented" are unverifiable by inspection — there is no falsifiable check.**
"Their recommended settings, current release, version pinned" is good language, but
nothing defines who adjudicates fairness if a Digital or Logisim-Evolution maintainer
later disputes the configuration (e.g., disagrees the settings used were their
"recommended" ones). Contrast with #520 CAP-36's PF-1, which requires competitor
claims to be "cited to their own tracker rather than to our opinion" — #560 has no
analogous requirement to cite the competitor's own documentation for what its
"recommended settings" are. Without that citation requirement, AC-4 reduces to
self-attestation.
*Recommendation:* require each competitor's configuration to link to that competitor's
own docs/README section establishing it as recommended, mirroring #520's citation
discipline.

**6. Deep, currently-unstarted dependency chain not fully surfaced in `ordering_after`.**
#560 lists `ordering_after: [#554, #555]`, but #554 (FEAT-C28-1, the JLS-side suite)
itself orders after #413 (TASK-0025, "re-home the calibration fixture and delete
`riscv/`") — a large, still-open task. As verified in this checkout, `riscv/` and
`riscv/bench_kernel.py` are still present on disk and `k2000.jls` is still untracked,
confirming #413 has not landed. So #560 is transitively gated behind at least three
substantial, currently-open pieces of work (#413 → #554 → #555 → #560), none of which
#560 states directly — a picker-upper reading only #560 will not see the #413
dependency without following the chain themselves. This is a real scheduling/scope
risk, not a blocker on the issue's own correctness, but the issue could save a future
reader the chain-walk.
*Recommendation:* note the transitive dependency on #413 explicitly, or at minimum
flag that #560 cannot start meaningfully until `riscv/`'s CPU-scale fixture has a
permanent tracked home.

**7. `band_mw: "0.5-1"` looks optimistic given items 3–5 above.**
Sibling feature #554 (the JLS-only benchmark suite, no third-party tools involved) is
estimated at "1-2" mw. #560 additionally requires standing up, configuring, and fairly
operating two entire third-party GPL applications with unknown headless/batch
capabilities, resolving what "recommended settings" means for each, and documenting
version pinning — for less estimated effort than the JLS-only suite. This is inherited
from #512 PF-4's own estimate rather than invented here, but it's worth flagging as
inherited optimism rather than independently re-derived.

**8. Minor: the Outcome's factual claim about Digital's "120 kHz" figure is not self-contained.**
The Outcome states as fact that "Digital's '120 kHz simulated processor clock' claim
gets an answer that is a measurement, not a counter-claim," but the only citation for
that number lives in #512's `evidence:` field ("Digital teardown... published 120 kHz
processor claim"), one hop away and itself not sourced to a Digital-side document or
URL in either issue. AC-4 demands fairness and accurate sourcing of *competitor*
claims but the issue doesn't hold its own headline claim about a competitor to that
same citation bar.
*Recommendation:* AC-4 or the Outcome paragraph should link the actual source of the
"120 kHz" figure (Digital's README/wiki/release notes) so the harness is answering a
claim that's traceable rather than folklore repeated across three issues.

## What's solid

- The producer/citer boundary against #588 (FEAT-C36-1) is explicit and was already
  checked by the author in a comment on this issue — no duplicate scope, clean split
  between "the measurement" (#560) and "the notes" (#588).
- KC-28-1's "an unfavorable result publishes anyway" governing principle, inherited
  from #512 and restated here, is a strong anti-gaming device against the single
  biggest risk in any competitor benchmark (publication bias) — genuinely well-designed.
- The separation from #520 CAP-36 ("prose write-ups and venue publication... belong to
  CAP-36's features") is clean and avoids scope creep into the write-up/venue-submission
  programme.
- AC-4's requirement to pin competitor versions and use their own recommended settings
  is the right instinct, even though (per finding 5) it lacks a verification mechanism.
