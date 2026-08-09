# Issue #407: TASK-0024: the calibration record stops describing experiments and states measurements, and its re-measurement procedure names a fixture that exists
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 14 sections away and the purpose is one sentence: **JLS's performance
claims should divide by numbers that came from somewhere, and the somewhere should
still exist next year.** That is aligned with the project's arc. This repository
already spends real effort on that instinct — `SHA256SUMS-installers-*` plus
build-provenance attestation in `README.md`, a byte-reproducible jar and
`bom.json`, `ARCHITECTURE.md`'s recorded decisions each carrying a *revisit
trigger*, and a family of tests (`ArchitectureRulesTest`, `HeadlessCoreRatchetTest`,
`NullMarkedRatchetTest`, `CollabSecurityRatchetTest`) whose job is to make a stated
property fail the build when it stops being true. A project that attests its
installers and then prices its roadmap off an unmeasured α with a 3.1× spread has
an inconsistency worth closing.

Where it goes wrong is the **carrier**. #407 puts the constants in prose and
polices the prose with a bespoke markdown parser hunting the literal token
`Still open:`. That is not a detail — it decides whether this work compounds or
rots, and the issue keeps admitting it. H3's refutation clause ("a §6 rewrite that
satisfies the parser while still stating no measurement") and §11's first threat
("its acceptance test can only check shape, not truth") are both confessions that
the mechanism cannot do the job it is built for. An issue that writes down why its
own gate is decorative and ships the gate anyway is asking to be reframed.

## Grounding caveat

`docs/machine-calibration.md` and `docs/virtual-hardware-parity.md` do not exist in
this checkout, and `2d0ca9d…` is not an object here (`git cat-file -t` fails; 273
commits reachable), so O1, O3, O4, O8 and O9 are unverifiable. What is verifiable
holds: 35 registered types against ten surviving `33`s under
`docs/capability-roadmap/`; `ProcessBuilder` 0× in `src/`, 15 test files (O6);
`src/jls/hdl/imp/NetlistImporter.java:40-47` carrying O7 verbatim; `riscv/gui/cpu.jls`
one of four tracked `.jls` files; `ARCHITECTURE.md:353-368` carrying the qualitative
#221 trigger and its binding equivalence criterion. The reframe does not rest on the
unverifiable half.

## Reframe 1: the record is a data artifact; the prose is generated

The repository already contains the pattern #407 should have copied, and #407
cites its weaker sibling instead. `test/jls/CliFlagTableTest.java`'s javadoc:
*"the flag table in JLSStart is the single authoritative CLI specification,
usage() is generated from it, and the parser accepts exactly the flags it lists."*
One typed table (`JLSStart.java:759`, `FlagSpec[] FLAGS`), a generated derivative
(`:1180`, "generated from the flag table so it cannot [drift]"), and a test that
the derivation holds. Drift is impossible, not merely detected.

`HotkeysHelpAccuracyTest` is the fallback shape — two files, regex reconciliation —
and its javadoc says why it must be: *"the keys and the source of truth live in two
files."* That is a constraint (the help page is shipped HTML), not a design to
imitate. #407 imitates it by choice for data under no such constraint, then in Open
Question 3 contemplates forking a *second* bespoke parser. The tell is that §7.10
already knows better for one quantity — the element count is checked "against the
**runtime** registry, not a regex over the source, so the assertion cannot itself go
stale" — and then leaves the nine constants the feature exists for as
regex-over-prose. The principle was found and not generalized.

**Concrete alternative.** Make the constants a typed tracked record, same shape as
`FlagSpec[]`:

```
test/jls/CalibrationRecord.java
  record Constant(String key, String unit, Measurement value /* nullable */,
                  String workload, LocalDate date, String hardware,
                  String method, int sourceIssue, int openedBy /* if unmeasured */)
```

What falls out, including things #407 cannot get:

- **H1/H3 become type-level.** "Discharged or explicitly still open" is
  `value != null || openedBy != 0`. A section cannot satisfy a parser while stating
  no measurement, because prose is not the store. "Workload, date, hardware,
  method" stops being a review checklist and becomes non-null fields. §6.6's
  node-count-and-pass-count rule and #335's criterion 6 become unrepresentable
  violations rather than rules a reviewer must remember.
- **§6 is generated between markers**, with a staleness test: `usage()`'s
  relationship to `FLAGS`, applied to markdown. §6.11's bidirectional map (P4/O9) —
  which #407 correctly names as the thing that rots when §6 is rewritten — cannot
  rot, because both sides project from one table.
- **TASK-0026's ratchet reads the same table for its thresholds.** #335 §3 already
  promises "a set of named, banded constants consumed by the ratchet and by every
  capstone budget" — a *data* deliverable, and TASK-0024 is the task that should
  produce it. As filed, #407 writes prose and TASK-0026 hand-copies numbers out of
  it into assertions; that second copy drifts first. Capstone budgets (#295, #301,
  #304, #306, #312) then cite constants by key, so a re-measurement that moves α
  mechanically names the budgets needing re-pricing, instead of relying on #407's
  "post a `STATUS:` comment on #335."

## Reframe 2: §7 should ship a self-test, not a walkthrough

P9 is honest that "walked end to end on a clean clone" has no automated
substitute — but this repository does not believe that. `scripts/` carries
`icestick-handoff-selftest.sh`, `macos-rig-selftest.sh`, `wayland-rig-selftest.sh`,
`x11-rig-selftest.sh`, `windows-rig-selftest.ps1`: a standing convention that a
manual rig procedure ships with a script proving its preconditions. §7 should get
`scripts/calibration-selftest.sh` alongside them. A procedure validated by one
operator on one platform (§11 concedes this) is validated until the second person
tries it — and #335's Integration Criterion 5 will be discharged the same way at
close-out.

## Reframe 3: the unblocked third should not wait ten weeks

O2/P3 (ten stale `33`s), O6/P7 (the `src/` scope) and O7/P8 (the `$dff` wording)
depend on nothing — not #377, not #379, not the fixture, not the deletion. They are
verifiable against the tree checked out today. #407 parks them behind a 5–10
maintainer-week critical path while its own Intended Audience section explains why
that is wrong: *"An agent that re-derives from those documents inherits the error."*
File them as a one-hour task now.

The class is also larger than ten integers: `docs/` carries **2,045** backticked
in-tree path citations, **558** with explicit `:line` anchors, across 258 distinct
source files. Line anchors in a live tree are a rot engine; the count errors are the
visible instance. The elegant version of Open Question 3 is not "which class owns the
element count" but a citation convention (a marker separating "exists at HEAD" from
"proposed") plus one `DocCitationsTest` resolving every asserted citation — which
would have caught this error before it was copied ten times, and which correctly
would not fire on the comparison citations into Logisim/Digital trees that a naive
existence check false-positives on.

## The #221 trigger

P6 is right that the equivalence criterion (`ARCHITECTURE.md:359-368`) must stay
textually unchanged; it is the most load-bearing sentence in the file. But
quantifying the *trigger* in prose reproduces the original problem one level up:
"below 10 kcycles/s on the #202 golden's CPU" (keystone-c-performance.md:150) beats
"unusably slow" only if something evaluates it. Under the reframe the threshold is
a row in the same table the ratchet reads and `ARCHITECTURE.md` names the key. A
recorded decision whose trigger nobody evaluates is a decision with no trigger.

## Acceptance criteria I am explicitly disregarding

- **"`MachineCalibrationDocTest` fails at the pre-change commit and passes at the
  fix commit."** The class should not exist in the specified form; its P1 assertion
  is the one the issue predicts can be satisfied vacuously. Replace with a test over
  the typed record plus a generated-section staleness check.
- **"Each discharged §6 entry states its workload, date, hardware and method"** as a
  prose-review item. Keep it as a schema constraint; drop it as a checklist.
- **"The parser must not become a shared markdown utility; if a second document
  needs one, that is a new decision"** (§7.5). The second, third and fourth already
  need one. Deferring buys a fifth bespoke parser.
- **The blocking of P3/P7/P8 behind #377, #379 and TASK-0025.** Ship those now.

## Why endorse-with-reframing, not rethink

The end state is right and unavoidable: someone must sit down with #377's and #379's
numbers and make the record honest. What changes is where the numbers live (typed
record, not prose), what the gate checks (schema and derivation, not markdown shape),
what §7 ships with (a self-test, not a transcript), and when the free corrections
land. Open Question 1 — six of ten subsections with no owner — is the strongest
signal the reframe is needed: with a typed record, "still open, owned by nobody" is
a query, not a prose audit.
