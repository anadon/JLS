# Issue #709: TASK-C536-2: a schematic exports as print-styled SVG with committed visual goldens, distinct from the shipped screen-styled export
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue is

TASK-C536-2 (part of #536/FEAT-C24-1, itself PF-1 of capstone #505/CAP-24) is
the actual print-styled SVG writer, built over TASK-C536-1's (#707) theme and
bundled-font substrate. `ordering_after: [TASK-C536-1]`. Grounded against
ARCHITECTURE.md, `src/jls/edit/CircuitRenderer.java`, `test/jls/SvgExportTest.java`,
`JLSStart.java`, and the full text of #707 (TASK-C536-1), #711 (TASK-C536-3),
#154, #723 (TASK-C540-1), #725 (TASK-C540-2), #536, and #540 — including the
prior adversarial reviews of #536 and #540 already on disk in this repo
(`issue-reviews/issue-0536.adversarial.md`, `issue-reviews/issue-0540.adversarial.md`),
which this review cross-checks rather than duplicates.

## Findings, most severe first

**1. (High) AC-4 depends on a task this issue never names as a dependency, and the omission is not covered transitively.**
AC-4 requires "every element on the palette-sweep fixture renders through its
print symbol" and cites "TASK-C540-1's ratchet." But TASK-C540-1 (#723,
fetched in full) is explicitly only the *mechanism* — a registry-keyed
mapping plus a totality test — and its own Outcome text says symbol authoring
happens *after* it, "against a red test rather than against a review
checklist." The actual symbols come from TASK-C540-2 (#725, fetched in
full): "every registered element type gets an ANSI/IEEE-91 distinctive print
symbol... turning TASK-C540-1's red ratchet green." #709's YAML
`ordering_after` lists only `[TASK-C536-1]` (#707); #707 in turn orders
after `[TASK-C540-1]` (#723) only — never #725. So the transitive graph
guarantees the *mapping mechanism* exists before #709 starts, but not that
any element actually *has* a symbol to render. AC-4 as worded cannot pass
(every element renders through its print symbol) until #725 lands, and
nothing in #709 says so. **Recommendation:** add `TASK-C540-2` (#725) to
`ordering_after`, or rewrite AC-4 to state explicitly what it can verify
before symbols exist (e.g., "every missing symbol is reported by name" with
none actually missing left unasserted).

**2. (High) AC-1's fixture — "the hazard-demo circuit" — does not exist anywhere in the repository, and its creation is unbudgeted.**
I grepped `examples/`, `test/`, `docs/`, and the full source tree for
"hazard"; the only hits are unrelated glitch-hazard comments in
`test/jls/SimulationSemanticsRegressionTest.java:52,618` and
`test/jls/elem/MemoryModelTest.java:378` — no circuit file, no `.jls`
fixture, no builder. This is the same gap flagged against #536's AC-1
(`issue-reviews/issue-0536.adversarial.md`, finding 5): the name is
inherited from #505's Outcome walkthrough and never actually scoped as a
deliverable anywhere in the chain. #709 is the first issue that must
*produce* the golden from this fixture, yet the 1-1.5 mw band has no line
for authoring, reviewing, or gaining consensus on a new example circuit.
**Recommendation:** name where the fixture comes from (new `examples/`
addition with an owning line item) or repoint AC-1 at a circuit the repo
already ships.

**3. (High) No CLI/invocation surface is named, and the obvious slot conflicts with an existing one.**
AC-3 says the exporter must be "reachable headlessly for CI and course-repo
use, not GUI-only" — but never says how. `JLSStart.java`'s existing `-i`
flag (extension validation ~line 1038, help text ~line 765) already accepts
`.svg` and produces the *screen*-styled export (#154). Two different
renderers that can each naturally emit `circuit.svg` need a way to be told
apart at the command line — a new flag, a `-i` modifier, a separate output
extension convention — and #709 states none of them. This is the exact gap
already flagged as unresolved at the feature level in
`issue-reviews/issue-0536.adversarial.md` finding 6; #709 is the task where
the CLI surface is actually built, and it inherits the ambiguity unchanged.
**Recommendation:** name the flag/subcommand explicitly; without it AC-3 is
not implementable as written.

**4. (Medium) AC-2's byte-identical golden for the untouched screen path contradicts the existing test suite's own documented rationale for not doing exactly that.**
`test/jls/SvgExportTest.java:16-24` states plainly: *"Deliberately no
full-document golden - text layout coordinates depend on the JDK's font
metrics, which differ across machines (the same reason CliImageExportTest
avoids pixel goldens)."* AC-2 asks for the opposite for the same code path:
*"a test asserts its output for the same circuit is byte-identical to the
pre-change golden."* Because AC-2 explicitly keeps #154 "unchanged," the
screen path does *not* inherit TASK-C536-1's bundled-font determinism work
(that's scoped to the print path only) — so a checked-in golden for it would
be exposed to the exact cross-machine font-metric variance the current test
file was written to avoid. Restricting the golden to a single pinned CI
runner would work but is never stated. **Recommendation:** either scope
AC-2's golden explicitly to one canonical CI lane/font environment, or
reconcile it with `SvgExportTest.java`'s existing determinism disclaimer
before writing the test.

**5. (Medium) AC-4's "missing symbol reported by name" behavior is untestable by construction, given what #723 already guarantees — a gameable gap.**
If TASK-C540-1's totality test (#723 AC-2, "fails when any type has no print
symbol") gates the build, then in any build that reaches #709's export code
at all, no registered element can lack a symbol — the "missing symbol"
branch AC-4 asks for is dead code under normal operation. #723 addresses
this for *its own* claim with an explicit falsification requirement (AC-3:
"a scratch element type registered without a print symbol turns the ratchet
red, and that red run's transcript is committed"). #709's AC-4 has no
equivalent — no falsification test is required to prove the "reported by
name" path actually fires and actually names the right element, rather than
being an untested `if (symbol == null) throw …` that never executes.
**Recommendation:** require the same falsification pattern #723 uses:
a test-only unregistered element type that drives AC-4's reporting path and
pins the message text.

**6. (Low) The "palette-sweep fixture" in AC-4 is a second nonexistent fixture, and it's unstated whether it's the same artifact #723/#725 use.**
Same grep (`palette-sweep`, full repo) returns nothing outside the
issue-reviews directory. If #709 is meant to reuse the identical circuit
#723's `PrintSymbolTotalityTest` and #725's committed goldens sweep, that
should be stated (shared fixture, one source of truth); if #709 needs its
own copy for the export-path assertion, that's separate unbudgeted work
layered on top of finding 2. Either way it isn't said.

**7. (Low) #709 sits downstream of an unresolved filing-precondition violation it doesn't acknowledge.**
`issue-reviews/issue-0540.adversarial.md` finding 1 established that #723
(TASK-C540-1) was filed against #505's own explicit blocking precondition
("Print symbol standard... **Blocks PF-5's filing**," OQ-1, unresolved as of
that review). #709 depends on #707, which depends on #723 — so #709
transitively inherits a foundational symbol-standard choice (ANSI-only vs.
ANSI+IEC) that could still be overturned by a REPLAN on #505. Not #709's
defect to fix, but worth a line acknowledging the inherited risk rather than
silence.

## What holds up

- The scope boundary against #154 is stated correctly and precisely — "this
  writer renders the print theme... the screen-styled export... stays
  exactly as it is" matches what `CircuitRenderer.java` and #154 actually do
  today; no scope creep into the shipped path.
- `ordering_after: [TASK-C536-1]` matches capstone #505's task sequencing
  exactly: #707 (TASK-C536-1) before #709 (TASK-C536-2) before #711
  (TASK-C536-3, whose own `ordering_after: [TASK-C536-2]` confirms the chain
  from the other end).
- The "consumes TASK-C540-1's ratchet" framing correctly identifies that a
  totality mechanism, not the symbols themselves, is the shared seam — the
  error is only in stopping the dependency graph one task short (finding 1).

## Verdict rationale

`needs-rework`: the task's shape and boundaries are right — it correctly
scopes itself against #154, sequences correctly within #536's own task
chain, and correctly identifies TASK-C540-1 as a shared seam. But its lead
acceptance criterion (AC-4) cannot be satisfied by anything this issue
actually depends on (finding 1), two of its four criteria hang off fixtures
that exist nowhere in the repository with no budgeted creation cost
(findings 2, 6), its CLI reachability criterion names no mechanism and
collides with an existing flag's semantics (finding 3), and its
regression-golden criterion asks for exactly the thing the current test
suite documents choosing not to do, without saying why this case is
different (finding 4). None of this indicts the underlying feature; it
indicts the issue's readiness to be picked up and built as specified.
