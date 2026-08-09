# Issue #728: TASK-C554-2: the benchmark fixtures are tracked in-tree outside `riscv/`, including at least two circuits below CPU scale
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what the issue claims

TASK-C554-2, a task under FEAT-C28-1 (#554), part of CAP-28 (#512). Two acceptance
criteria in scope: (1) every fixture the benchmark suite measures is tracked outside
`riscv/`, verified by a `riscv/`-deleted run passing; (2) at least two fixtures below
CPU-scale, each censused (node count, element mix), join the suite; the CPU-scale
anchor itself is consumed from #413 rather than re-homed here.

## Findings, most severe first

### 1. [HIGH] Miscited acceptance criterion — "CAP-28 AC-3" is the wrong AC
The second bullet reads: *"At least two fixtures below CPU scale are measured, each
with its node count stated (**CAP-28 AC-3**)."* I fetched #512 (CAP-28) directly. Its
actual AC-3 is: *"AC-3: The published number's fixture is tracked in-tree (not
`riscv/`, per D5)."* That is a single-fixture tracking requirement, not a
below-CPU-scale multi-fixture requirement. The claim #728 is actually making —
multiple fixtures at more than one scale — is verbatim FEAT-C28-1 (#554)'s **own**
AC-3: *"At least two fixtures below CPU scale (standard small circuits) are measured,
so published numbers exist at more than one scale."* #728 cites its grandparent
capstone's AC-3 when it means its direct parent feature's AC-3. Anyone auditing this
task against "CAP-28 AC-3" will find an unrelated criterion and could reasonably
conclude the citation — and by extension the whole acceptance bullet — is fabricated.
This is exactly the failure mode a sibling reviewer already caught elsewhere in this
family (`issue-0742.adversarial.md`: *"Citation is imprecise... #742 copies the
stronger, narrower... language [that] is not actually in CAP-28 AC-5"*).
**Recommendation:** change the citation to `#554 AC-3` (or `FEAT-C28-1 AC-3`).

### 2. [MEDIUM] Mixed reference style in the issue's own dependency list repeats a defect already flagged on a sibling issue in this exact cluster
`ordering_after: [413, "TASK-C554-1"]` cites one dependency by bare issue number
(`413`) and the other by bare task tag with **no issue number** (`"TASK-C554-1"` — I
confirmed via `mcp__github__issue_read` this is #726). `issue-0738.adversarial.md`,
reviewing another task in this same C557/CAP-28 family, flags precisely this pattern
as a convention break: *"`#738`'s own header breaks that convention:
`ordering_after: [\"TASK-C557-1\"]` gives the task tag with no issue number, forcing a
reader to search for it."* #728 makes the identical mistake for its own sibling
dependency. **Recommendation:** write `ordering_after: [413, "#726 TASK-C554-1"]`.

### 3. [MEDIUM] Acceptance criteria name no concrete verification mechanism, unlike the sibling task this one is modeled on
Compare to #413 (TASK-0025), the task #728 explicitly consumes from: #413's Materials
& Apparatus section names exact test classes (`CalibrationFixtureTest`,
`NoRiscvDirectoryReferencesTest`), a defined census data structure (§7.6) with a hard
equality assertion (P3), and a red-before/green-after regression requirement. #728's
Boundary section is one line — "Fixtures and their census." — and neither AC bullet
names a test, a CI job, or a census-record format. As written, "a run with `riscv/`
deleted from the working tree passes" and "each fixture is censused" could both be
satisfied by an ad hoc manual check reported once in a PR description with nothing
enforcing it on the next commit — the exact silent-rot failure mode #413 was filed to
close for the CPU-scale anchor. **Recommendation:** name the ratchet this task adds or
extends (e.g. reuse #413's `NoRiscvDirectoryReferencesTest`, and add a per-fixture
census-equality test mirroring #413 P3) rather than leaving "passes" and "censused"
undefined.

### 4. [MEDIUM] "Below CPU scale" has no numeric ceiling and no diversity requirement, so the letter of AC-2 can be met while its stated purpose is not
`docs/capability-roadmap/keystone-c-performance.md` (verified on disk) censuses the
CPU-scale anchor at 225 logic elements / 297 wire nets / 522 total nodes. #728's
Outcome explains the *purpose* of the sub-CPU-scale fixtures: "JLS has published
numbers at more than one scale rather than one number that only describes a
processor" — i.e., scale *diversity* matters. But the acceptance bullet only requires
"two or more standard small circuits," each merely "censused." Two near-identical
trivial fixtures (e.g. a 2-bit and a 3-bit counter, same dominant element type) would
satisfy "≥2, censused, below CPU scale" literally while defeating the stated intent —
a number attributable to "a workload" still ends up describing the same workload
twice. **Recommendation:** state a numeric ceiling (e.g. "under 50 logic elements")
and require the fixtures differ in dominant element type — which the issue's own
suggested pair (a counter: sequential-heavy; a memory loop: `Memory`-heavy) already
satisfies, so this is a matter of writing down what was implicitly intended, not
inventing new scope.

### 5. [LOW] Fixture destination is left unstated despite being a proven silent-failure mechanism in this exact repository
I verified `.gitignore` L8-10: `*.jls` is ignored repo-wide except
`!test/fixtures/**/*.jls`. #413 O6 documents this as forced, not a style choice: *"A
fixture committed anywhere other than `test/fixtures/` will be silently dropped by
`git add`."* #728's acceptance criteria say only "outside `riscv/`," never
"`test/fixtures/`," and say nothing about a `.gitattributes` line for any
non-`.jls` companion file (a clock vector `.txt`), which #413 §7.7 explicitly had to
add. Low severity because CI would eventually surface the missing file (the
`riscv/`-deleted run would fail to find it) rather than pass silently — but it costs
an implementer a confusing debugging session #413 already paid for and documented.
**Recommendation:** state the destination explicitly (`test/fixtures/`) and note the
`.gitattributes` line needed for any new non-`.jls` fixture asset.

## What is solid

- **The ordering dependency on #413 is present and correctly directed.** Two separate
  maintainer-review comments (on #554 and on #413, both 2026-08-08) argue #728 needs
  an `ordering_after`/edge to #413 specifically so it "consumes the re-homed anchor,
  not re-homes it a second time." #728's YAML header already lists `413` first in
  `ordering_after`, and the Boundary section states the same thing in prose ("The
  CPU-scale anchor is consumed from #413's re-homing rather than re-homed again
  here"). The concern those comments raise is already addressed in the issue as
  filed.
- **The task/feature boundary is clean.** #728 restricts itself to fixtures and their
  census, leaving the harness/command to #726 (TASK-C554-1) and the machine-readable
  output contract to #730 (TASK-C554-3) — matching #554's own documented
  producer/consumer chain and not attempting to re-build either sibling's scope.
- **The rationale for censusing (node count, element mix) is well-grounded**, echoing
  #413's own argument that events/cycle is a property of the circuit, not the engine,
  so an uncensused fixture silently re-bases every downstream figure.

## Note on scope of this review

I did not evaluate #726 or #730 in depth (out of scope for #728), but cross-checking
them was necessary to confirm the `"TASK-C554-1"` reference resolves and that #728's
"a run... passes" precondition (a working harness) is realistically providable only
after #726 lands — which the existing ordering already anticipates.
