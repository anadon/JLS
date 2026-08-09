# Issue #463: TASK-0097: a headless MNA transient solver in pure Java — device stamps, sparse LU with a totally ordered pivot tie-break, Newton with junction limiting, and LTE timestep control
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Read the issue body in full (14 sections, DoD checklist, 10 predictions, 5
hypotheses). Cross-checked its file:line citations against the actual
checkout (`src/jls/Circuit.java`, `src/jls/elem/Element.java`,
`docs/file-format.md`, `pom.xml`), searched the tree for the documents it
treats as normative evidence, checked git history for the pinned
`evidence_commit`, and read the parent feature (#351) and two sibling
tasks (#402, #397) it cites for consistency.

## Findings, most severe first

**1. The evidentiary spine — `docs/plan/evidence/BRIEF.md` and
`11-analog-determination.md` — does not exist anywhere in this repository,
and the pinned `evidence_commit` cannot be found in 267 commits of history.**
The issue's entire cost/design argument (D8 "plausibly reimplement", D10
"absence is not a refusal", the specific ngspice divergence numbers in
§ Intended Audience and § Threats to Validity, the "seven JVM
configurations" digest-stability claim behind H1, the RC/rectifier
calibration numbers) is sourced to `docs/plan/evidence/BRIEF.md §13` and
`11-analog-determination.md §1.3/§1.4/§4.1/§4.2/§4.4`. Neither file exists
anywhere in `/home/user/JLS` (`find … -iname '*determination*'` and
`find … -iname 'BRIEF.md'` both return nothing), and `git rev-parse
--is-shallow-repository` plus a full search of 267 reachable commits
(2026‑07‑16 → 2026‑08‑08) turns up neither `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`
(this issue's `evidence_commit`) nor `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`
(BRIEF.md's claimed landing commit). Sibling issues #351, #402, #397 cite
the identical two phantom artifacts, so this is systemic to the batch, not
a one-off typo — but that makes it worse, not better: an executor picking
up #463 today cannot verify a single one of the measured numbers the issue
uses to justify porting SPICE instead of orchestrating it. Recommendation:
before this issue is actionable, either commit `BRIEF.md` and the
determination doc to the tree, or strip the numeric claims down to what
this task itself will (re-)measure, and drop the specific-commit pinning
convention since it is currently unverifiable and erodes trust in every
other citation in the issue.

**2. Three "Blocks execution" open questions are left unresolved in a
document filed as `tier: task` (i.e., ready to execute), not as a design
proposal.** Open Questions 1–3 (`RELTOL`/`VNTOL`/`ABSTOL` values, whether
the Markowitz product is part of the pivot tie-break, and what hash backs
the digest) are each explicitly marked "Blocks execution." P1 cannot be
written without Q1, P5 cannot be written without Q2, P8 cannot be written
without Q3 — i.e., three of the ten predictions are blocked by the issue's
own admission, yet §14's DoD does not gate on resolving them before work
starts, only on resolving them "or explicitly deferred" by close. This
inverts the normal order: a task ready for an engineer to pick up should
not still be doing algorithm design (Markowitz-in-tie-break is a real
numerical-methods decision, not a formality) inside "Open Questions."
Recommendation: resolve Q1–Q3 (they each carry a "Recommended" default
already) before filing/estimating, or explicitly split them into a
prerequisite design task.

**3. H1's headline claim (cross-platform, cross-JIT-state bit-identity) is
not covered by any automated "must-hold" prediction — only by manual,
undated, non-CI verification.** § Intended Audience's flagship promise is
"an analog answer that is byte-identical on every platform." The only
must-pass prediction that actually tests reproducibility is P8, "run the
same fixture twice in one JVM" — same process, same JIT state, same
platform. The cross-JIT-flag (`-Xint`, `-XX:-UseFMA -XX:UseAVX=0`) and
cross-JDK-version checks that would actually stress H1 are demoted to
"Manual verification with platform" in §9 ("record the JDK build") — i.e.
not a CI gate, not a repeatable regression test, and not in the DoD as an
automated check (the DoD line is "Two runs in one JVM, and one run each
under -Xint and …, produce identical digests" with no automation
requirement attached). A PR could satisfy every "must hold" prediction and
DoD checkbox while still harboring a platform- or JIT-dependent bug the
whole task exists to prevent, provided the manual runs are done once,
by hand, and never re-run. Recommendation: turn at least the `-Xint`
variant into an automated CI job (a second Maven/surefire profile), not a
one-time manual note.

**4. P3, the stated "anti-cheat" assertion, is explicitly acknowledged by
the issue itself to be gameable, and the mitigation is a human judgment
call, not a test.** §10's own falsification note: "If P3 passes trivially
(the error is non-zero because the solver is simply wrong), P1's
tolerance is too loose. Tighten it against the derived bound rather than
widening P3." That is a correct observation but it is not an executable
check — nothing in §5/§9/§14 defines a *quantitative* bound on how close
to zero P1's error must be, only that P1's tolerance is "derived" from
RELTOL/VNTOL/ABSTOL (itself unresolved per Finding 2). A reviewer reading
a green CI run cannot distinguish "correct solver, small analytic error"
from "buggy solver, coincidentally small error" without re-deriving the
bound by hand every time. Recommendation: state the expected order of
magnitude of the RC-corner error explicitly (e.g., "must be within
[x, y] of the trapezoidal LTE bound, not just less than τ") so P1+P3
together bound the error from both sides, not just above.

**5. Scope-vs-estimate mismatch, visible in the issue's own numbers.** The
task lists nine new production classes (`MnaMatrix`, `LuFactorization`,
`Newton`, `Trapezoidal`, `TimestepController`, `Devices`, `LinearFastPath`,
`AnalogSample`, plus the digest harness) plus three test classes, a
from-scratch total-order pivot rule, a Newton escape ladder with named
convergence rungs, LTE-based adaptive step control, and cross-platform
determinism auditing — while self-describing as "the core slice" at 2
maintainer-weeks and admitting the true cost is "3.5-5 maintainer-weeks."
The parent feature (#351) independently states its four named task rows
sum to 8.0 mw against a 17.5–26 mw band — a 3.25× unexplained gap it
labels "Open Question 1" and explicitly declines to resolve ("the
residual has no task id"). #463 inherits that same optimism without
flagging it locally. Recommendation: either cost this task at the
admitted 3.5–5 week figure everywhere it appears (not the 2-week "core
slice" figure used in the headline estimate), or explicitly scope out one
of {escape ladder, LinearFastPath, digest harness} into a follow-up task.

**6. The issue depends on an undefined external rule system
("rule 2," "rule 3," "rule 6," "rule 8," "rule 10") that is not present or
linked anywhere in this repository.** Phrases like "per rule 10" (waiver
protocol), "rule 6" (supersession check), "RULE 3, THE OBSERVED FAILURE"
appear throughout §§ Status, Observations, and the DoD checklist, but no
file in the repo (`CONTRIBUTING.md`, `docs/*.md`, `.github/**`) defines
what these numbered rules are. A reviewer cannot check compliance with a
rule whose text is not available. This is the same unverifiable-external-
dependency problem as Finding 1, applied to process rather than technical
evidence. Recommendation: either link the governing document or inline
the rule text the DoD items depend on.

**7. Internal tension on "no `.jls` format change" versus the actual
persisted-parameter story.** §7.3/§7.12 correctly state analog parameters
will be `String` items and that this task "must not pre-empt" the
double-item-kind decision — consistent with `Element.java`'s four
`setValue` overloads (`int` L344, `long` L359, `BigInteger` L374, `String`
L389, verified in the checkout — no `double`) and with
`docs/file-format.md`'s closed item grammar (L125–141, verified). That
part is solid and internally consistent. But §6 "Materials & Apparatus"
says this task "consumes them programmatically and does not add a
parser," while device *parameters* (resistor values, etc.) are only
meaningful via SPICE-suffix parsing (`4.7k`, `10n`) — which sibling task
#402 owns and which is itself only "being filed concurrently," not landed.
So TASK-0097's own test fixtures (RC step, rectifier) must either hardcode
raw doubles in test code (fine, and consistent with "no parser") or the
task silently needs a minimal ad hoc numeric literal path that isn't
`Devices`' documented consumer surface. Not a blocking contradiction, but
worth a one-line clarification in §6 that fixtures are Java-literal-driven
only, to close the gap before someone builds a parser here by accident.

## What's solid (no rework needed)

- **O3 (HashSet vs. `getElementsInStableOrder`) is accurate.** Verified in
  the live tree: `Circuit.java:47` (`private Set<Element> elements = new
  HashSet<Element>();`) and `Circuit.java:479-485`
  (`getElementsInStableOrder`, sorted by `Element::getStableId`) match the
  issue's citation almost exactly (current code is generics-typed; the
  issue quotes an older raw-type rendering, immaterial to the point).
- **O4 (closed item-kind grammar, no `double` setValue) is accurate**,
  confirmed against `docs/file-format.md:125-141` and
  `Element.java`'s four overloads.
- **O5 (JaCoCo `PACKAGE` exact-match, `jls.edit` unfloored precedent) is
  accurate** — `pom.xml` line numbers differ slightly from the citation
  (current `<rule><element>PACKAGE</element>` block is at different exact
  lines than 426-430/812-813 quoted, likely drift from the phantom
  evidence commit) but the substance — exact package matching, mutation
  threshold 80/testStrength 82, and `jls.edit`'s "deliberately unfloored"
  status — all check out live.
- **The "port, don't orchestrate" argument is sound in principle**, given
  the project's stated single-offline-jar constraint (confirmed in
  README.md) — an external `ProcessBuilder`-linked solver would indeed
  break that promise, independent of whether the specific ngspice
  divergence numbers can be verified.
- **The JDK/toolchain requirement (JDK 25) matches the project's actual
  `maven.compiler.release` (pom.xml:43).**
- **ArchUnit is a real, already-used dependency** (`pom.xml:110-111`, two
  existing test files use `com.tngtech.archunit`), so TASK-0098's promised
  StrictMath-enforcement rule is technically plausible on this stack, not
  speculative tooling.
- **The `@NullMarked`/`package-info.java` + ratchet-test convention is
  real and already used** (`test/jls/NullMarkedRatchetTest.java`,
  `test/jls/PackageInfoRatchetTest.java`, and multiple existing
  `package-info.java` files with the same pattern), so that specific DoD
  item is achievable exactly as described.

## Net assessment

The technical design (MNA + sparse LU + Newton + trapezoidal LTE, the
totally-ordered pivot tie-break, the digest-over-raw-bits golden format)
is competent and internally coherent, and its file-level citations against
this codebase (O3–O5) hold up under direct verification. But the issue is
not yet safe to execute as filed: its central evidentiary basis (Finding
1) is unverifiable in this repository, three of its ten predictions are
blocked on unresolved open questions the issue itself flags as
execution-blocking (Finding 2), its flagship determinism claim is tested
only manually rather than by CI (Finding 3), and its own stated "core
slice" estimate contradicts its own stated realistic estimate (Finding 5).
None of these are fatal to the underlying idea, but a reviewer should push
back before assigning this to an implementer.
