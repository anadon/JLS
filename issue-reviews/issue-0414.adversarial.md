# Issue #414: TASK-0045: a drawn circuit exports as a Yosys JSON netlist that Yosys itself reads back with the same interface
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is exceptionally well-structured on the page — falsifiable hypotheses,
a full interface contract, an eleven-row statement-mapping requirement, explicit
determinism and round-trip properties. But its own evidence base is compromised,
and the author has already said so in a comment on this very issue. That comment
is not a minor caveat; it invalidates a load-bearing observation (O7) that several
predictions, completion-criteria items, and the falsification section depend on.
Anything that ships against this issue as written risks reinventing a policy that
doesn't exist on `master` and then citing tests that don't exist either.

## Findings, most severe first

### 1. (Critical) The issue's own `evidence_commit` is on a to-be-deleted branch, and a key observation (O7) does not exist on `master` — confirmed against the checkout

The issue pins `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and
builds O7 ("the export policy that bounds the writer's coverage") on a quoted
`HdlExporter.REJECTED` map with four entries and per-class rejection reasons
(Memory, SubCircuit, RegisterFile, FieldExtend), plus a cited test
`test/jls/hdl/HdlPolicyTest.java:392`
(`exportPolicyIsTotalOverTheElementRegistry`).

The issue's own comment (posted same day by the repo owner, referencing #493)
says plainly: *"part of what this issue cites is branch-only code — it does not
exist on `master` and never did"* and lists `src/jls/hdl/HdlExporter.java:460`
and `test/jls/hdl/HdlPolicyTest.java:392` as branch-only anchors, naming
`HdlExporter.REJECTED`, `REJECTED = Map.of`, and
`exportPolicyIsTotalOverTheElementRegistry` as symbols that don't exist on
master.

I verified this independently against the actual checkout at `/home/user/JLS`
(HEAD `e7731bd7b0c3076afeeb2fd9dbe53b43b1bf2fba`):

```
$ grep -n "REJECTED" src/jls/hdl/HdlExporter.java
(no output)
$ grep -n "exportPolicyIsTotalOverTheElementRegistry" test/jls/hdl/HdlPolicyTest.java
(no output)
```

What's actually on master (`src/jls/hdl/HdlExporter.java:88-90`):

```
 * <li><b>Reject</b> (inexpressible in this version, §9 escalation):
 * SubCircuit, Memory, and anything unrecognized.
 * Rejection lists <em>every</em> offender in one message and nothing
 * is written.</li>
```

Two classes, a generic catch-all, and **no per-class reason strings**, no
`RegisterFile`, no `FieldExtend` — versus the issue's four-entry map with
bespoke prose reasons for each. This is not a cosmetic line-number drift; the
policy shape itself differs (2 rejected classes + catch-all vs. 4 named
classes with individual justifications), and the "totality test that keeps it
honest" (`HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`) is absent.

**Impact on this issue's contract:**
- **P11** ("the writer's advertised coverage equals `HdlExporter`'s (O7)") is
  written against a policy that doesn't exist as described.
- **§7.12 claim 4** and **Completion Criteria item 4** ("writer's coverage
  equals the export policy's") inherit the same problem.
- **§10 Falsification** for H1 and the whole "coverage inheritance" formula in
  §7.10 assume `REJECTED` is a stable, reviewable artifact to inherit from.
- Rule: the abstract claims "At `2d0ca9d` JLS reads the Yosys `write_json`
  netlist format" as the ground truth executors should re-derive against — but
  the *task's own maintainer* has flagged that ground truth as partly fictional
  relative to the branch that will actually ship (master).

**Recommendation:** Do not execute this issue as written. Re-derive O7 (and any
other anchor in the #493 branch-only list) against `master` before the mapping
table (Open Question 4, which is execution-blocking) is written, since the
refusal list's exact membership depends on what `HdlExporter` actually rejects
today (`SubCircuit`, `Memory`, catch-all) — not the four-class map quoted in
the issue body. This alone should stop execution until #492/#493 land or the
issue is amended.

### 2. (High) The `blocked_by: []` claim is now stale given finding #1

The YAML frontmatter states `blocked_by: []` and the surrounding comment
insists "nothing must land first. HdlModel and the exporter walk exist at
`2d0ca9d` and this task reads them unchanged." But the exporter walk the task
"reads unchanged" is the branch-only version. If #492 (the issue that,
per the owner's comment, is what actually delivers the `REJECTED`-equivalent
policy on master) hasn't landed, this task has an undeclared soft dependency:
either the mapping table is written against the *current*, simpler two-class
rejection policy on master (in which case the elaborate four-reason O7 quote is
decorative and misleading), or the task should wait for #492. The issue does
not resolve this itself — the resolution lives in a comment, not in the
body's dependency graph, which is exactly the kind of drift the issue's own
"rule 6" (re-verify before pickup) is meant to catch but here needed a
same-day correction.

**Recommendation:** Update `blocked_by` or add an explicit note resolving
whether #492 must land first, rather than leaving the correction stranded in
a comment thread that a future executor might not read before starting.

### 3. (Medium) `StateMachineStatement` refusal is the one real design judgment call, decided by the issue itself rather than left open

Open Question 1 asks "Refuse `StateMachineStatement`, or lower it?" and then
immediately supplies "Recommended default: refuse it," marked "Blocks
execution." That's fine as a default, but the issue simultaneously claims (in
"Threats to Validity") that *"a class of drawn circuit exports to Verilog but
not to a netlist, which is a coverage asymmetry a user will notice and which
must be documented rather than discovered."* No acceptance criterion actually
requires that documentation to exist in a user-facing location (docs,
`--help`, or the export error message beyond "refuse it by name with the
reason") — P10/P11 only test that the *code* has no `default` arm and that
coverage doesn't exceed policy. A contributor could satisfy every checked box
while leaving the asymmetry undocumented anywhere a student would find it
before running into the refusal message.

**Recommendation:** Add an explicit, testable criterion (e.g. a specific
sentence in `docs/batch-interface.md` or a `docs/hdl-export.md` coverage table)
rather than relying on prose intent that no test enforces.

### 4. (Medium) The Yosys-round-trip acceptance test (P9) is both "the strongest check" and structurally the easiest to game via skip-when-absent

The issue calls P9 *"the strongest check"* — "Yosys itself reads the emitted
file and reports the same module interface JLS declared" — but also mandates
(correctly, for CI reproducibility) that it skip cleanly when Yosys isn't on
`PATH` via `ToolLocator.findOnPath` (O9). The issue's own Threats to Validity
section admits: *"P9 skips where Yosys is absent, which is most contributor
machines. The strongest evidence in this task therefore runs on a subset of
runs."* Combined with Completion Criteria's requirement to merely *"name the
CI leg that ran it... in the PR"* (an assertion in prose, not a required green
badge check on the merge gate), a PR could merge with the flagship claim of
the issue title ("Yosys itself reads it back") never having actually executed
in CI — only claimed in a comment. There's no criterion that CI is configured
to *guarantee* a Yosys-present leg exists (vs. simply hoping a contributor's
local machine or a manually-named CI job ran it once).

**Recommendation:** Make "a CI leg with Yosys installed, running P9 to green,
non-skipped" a hard, automatically-checked completion criterion (e.g. grep the
CI log for "SKIPPED" absent on a named job), not a PR-comment attestation.

### 5. (Low-Medium) Golden-count correction is honest, but "byte-exact" goldens over 37+33(+~70 new) files raises review-cost risk understated by the issue

O10 corrects the corpus size from "~20 paired" to "37 `.v` and 33 `.vhdl`"
(verified: `git ls-tree` counts on the current HEAD reproduce 37 and 33
exactly). The issue is honest about this correction, which is good — but it
still frames golden-writing as routine ("Golden-writing effort scales with
it") without revising any size/time estimate elsewhere in the document (there
is no story-point or maintainer-week estimate to revise, so this is a mild
omission rather than a contradiction). Combined with the acknowledged future
regeneration risk from #373 (§7.12 claim 6, "Golden regeneration is expected
exactly once"), a reviewer facing 70+ new/changed `.json` golden files plus a
possible full regeneration shortly after has a large, low-signal diff to
police for the one thing that actually matters (§10: "If a `.v` or `.vhdl`
golden moves: stop").

**Recommendation:** Fine as-is if the reviewer discipline in §10 is actually
followed; flagging only because the issue doesn't budget review time/effort
anywhere, unlike its unusually careful treatment of every other risk.

### 6. (Low) Vendored schema licensing is handled correctly, but the pin-update mechanism is undefined

§7.2 and the Materials section require vendoring `lib/yosys.schema.json5`
(MIT) with its notice, "because CI has no network." Good practice, correctly
flagged as Open Question 2 with "pin both" as the recommended default. But
there's no criterion for *how* the pin gets refreshed when Yosys's schema
changes upstream (no re-vendor script, no version-drift test comparing the
vendored copy's declared version against the installed Yosys's, beyond the
generic "Threats to Validity" prose warning that "an upstream schema change is
invisible until someone updates the pin"). This is acknowledged as a known
gap rather than hidden, so it's a minor finding, not a blocking one.

**Recommendation:** Consider a low-cost drift check (e.g. a skipped-when-absent
test asserting the installed Yosys's `--version` matches a recorded pin)
rather than leaving staleness undiscoverable.

## What holds up

- **O1, O2, O3, O4, O5, O6, O8, O9, O10** were spot-checked directly against
  the current checkout (not the stale evidence commit) and reproduce correctly:
  the CLI really does refuse `.json` (allowlist at `JLSStart.java:1088-1090` on
  current HEAD, same two-decision-point shape as quoted); `JsonValue` really
  has nine reader-only public methods and zero public constructors; the eleven
  `Statement` subclasses in `HdlModel.java` are exactly as listed, at the exact
  line numbers quoted, even on current HEAD (not just the stale evidence
  commit); `YosysNetlist.BIT_*` sentinels and `CellValidator.SUPPORTED`'s
  nineteen cell strings match verbatim; `HdlEmitter`/`HdlExtensionPoints.EXPORTER`
  match verbatim; the 37/33 golden counts reproduce exactly.
- The **no-`default`-arm switch** requirement (P10) is a genuinely good,
  cheaply-enforced totality mechanism — compiler-checked rather than
  review-checked.
- **H1/H2/H3 as falsifiable hypotheses with named refutation actions** is a
  strong template; each has a concrete "next move," which most issues lack.
- **No new dependency / no `ProcessBuilder` in `src/`** constraint is checked
  against a real `git grep` and is consistent with the recorded architecture
  decision (`ARCHITECTURE.md`'s "Plugin trust boundary" section: Yosys
  integration deliberately stays out-of-process / subprocess-only, done here by
  keeping the tool invocation in the test tree only).
- Scope boundaries against sibling issues (#61 reader, #320 mapper parity,
  #358 hierarchy, #373 naming) are drawn clearly and consistently with what
  those issues are described as owning.

## Bottom line

The issue is analytically strong in form but its foundational observation O7
is confirmed-wrong against `master` by both my own inspection and the author's
own same-day retraction comment. Multiple downstream acceptance criteria
(P11, §7.12 claim 4, Completion Criteria item 4, part of §10) are written
against that wrong foundation. This is fixable — the fix is "re-derive O7
against master, revise the mapping-table's refusal-list starting point
accordingly" — but it is not something an executor should paper over silently,
since the exact refusal set (2 classes + catch-all vs. 4 named classes) changes
what "coverage inheritance" (P11) actually has to check.
