# Issue #613: TASK-C487-3: a tool JLS does not control renders the verdict — a board routed 25% over its declared maximum FAILS the external DRC naming the net, and the shortened one passes
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

`#613` is TASK-C487-3, the third of four planned tasks under FEAT-060
(`#487`, SI constraint authorship + PCB constraint export), itself the
second rung of CAP-18 (`#313`). Its job: run a KiCad-class external DRC
against a committed, over-length board fixture and its shortened twin,
asserting the failing direction first. The framing — fail-first, opt-in,
hermetic-by-absence, K18-2's narrow-the-claim escape hatch — is genuinely
careful. But the central feasibility claim ("it joins the harness CAP-05
already funds") does not hold up against CAP-05's own text, the ordering
graph omits the dependency that claim implies, and the one criterion that
actually proves the check ran ("naming the net") is unspecified to the
point of being satisfiable by a much weaker test than the one intended.

## Findings

### 1. [High] "Joins the harness CAP-05 already funds" is not true of anything that currently exists

The body's central feasibility argument is: *"it joins the harness CAP-05
(#298) already funds — it does not build a second one."* Checked against
`#298` itself, this doesn't hold. `#298`'s own text, discussing the same
`kicad-cli pcb drc` invocation, says verbatim: *"an opt-in test that is
always skipped in CI is a test that has never run. Pin the container by
digest and assert that the lane actually executed, or AC-2 is
decorative."* That is `#298` flagging its own container-digest-pin
mechanism as an unresolved threat to validity, not documenting funded
infrastructure. Independently: `.github/workflows/*.yml` has zero
references to `kicad` anywhere in this repo, and there is no Dockerfile,
compose file, or digest pin for any KiCad tooling. The only real,
existing "opt-in through a shipped locator plus assumption idiom" in this
codebase is `test/jls/hdl/ToolLocator.java` — a `PATH`/`PATHEXT` scanner
for local binaries (`iverilog`, `ghdl`), paired with
`Assumptions.assumeTrue`. That is a different mechanism from pinning and
invoking a Docker container by digest; #613 cites the two as one already
"shipped" thing. **Recommendation:** either name the specific PR/commit
that builds the container-digest harness (if one exists) or strike the
"already funds" claim and price this task as the one that builds
container-based opt-in tooling for the first time in this repo.

### 2. [High] The dependency this task actually needs is missing from `ordering_after`

`ordering_after: ["TASK-C487-2"]` — the rule-file emitter (`#611`, still
open) — is the only listed prerequisite. But per finding 1, the
container-digest-pinned invocation mechanism this task's own acceptance
criteria require does not exist in CAP-05 (`#298`) or anywhere else, and
`#298` is not listed in `ordering_after` at all, nor is any task under it
that would build that harness. As filed, an implementer picking this task
up after `#611` lands has a rule file to test against but no working,
reusable container-invocation harness to "join" — they would have to
build the container-digest-pin-and-detect-executed mechanism themselves,
which is exactly the scope `#613`'s framing says it is avoiding.
**Recommendation:** add the actual harness-building work (in `#298` or a
new task) to `ordering_after`, or absorb that work explicitly into this
task's scope and re-cost it.

### 3. [Medium] "Fails, naming the net" has no specified assertion mechanism — gameable toward a weaker test

AC1 requires the external checker to "exit nonzero with a violation
naming the net," and the closing bullet says "Container digest, command
and exit status recorded." Nothing in the issue specifies: which
`kicad-cli` output format is parsed (`kicad-cli pcb drc` supports both
human-readable and `--format json` reports, per `#298`'s own quoted
invocation), what string or field constitutes "naming the net," or how
the test distinguishes "failed for the intended length-violation reason,
naming this net" from "failed for an unrelated reason, exit code
nonzero." As written, a test that only checks `exitCode != 0` fully
satisfies AC1's exit-status clause while leaving "naming the net"
unverified in code — indistinguishable, per the issue's own stated
standard ("a check that passes on a board that should fail is
indistinguishable from a check that is not running"), from a check that
asserts the wrong thing. **Recommendation:** name the exact `kicad-cli`
invocation and output format, and require the test to parse the specific
net identifier out of the violation record, not just assert a nonzero
exit code.

### 4. [Medium] No container image or version is named anywhere

The issue requires "the checker's container pinned by digest" three
times (Outcome, AC1, AC2) but never names the image (e.g. a KiCad
version/registry) to pin. Contrast `#487`'s Open Question 1, which
records the DRC rule vocabulary it verified "first-hand" against a
specific tool reading. Without a named image, "pinned by digest" is an
aspiration rather than a reproducible artifact, and two implementers
could reasonably pin two different KiCad releases with different rule
schemas — silently changing what "passes"/"fails" means. **Recommendation:**
name the image and tag/version this task pins the initial digest against,
even if the digest itself is recorded at implementation time.

### 5. [Low] No evidence-commit grounding, unlike its sibling tasks and parent feature

`#487` (the parent feature) and `#611`/`#616` (sibling tasks, per the
existing peer review of `#616`) anchor claims to specific files, lines,
and a pinned commit (`evidence_commit: 2d0ca9d...` in `#487`). `#613` has
none: no evidence-commit line, no file path, no code citation anywhere in
the body — not even a pointer to where the fixture files
(`test/fixtures/...`) or the eventual `KicadSiDrcTest` class should live.
For a task whose acceptance hinges entirely on an external tool's exact
behavior, this is a real gap relative to the standard the rest of this
planning corpus sets for itself. **Recommendation:** add a pointer to the
expected test location and the specific `-si`/`-export` CLI surface
(`#487` names `jls -export clk.net -si clk.kicad_dru design.jls`; `#613`
never repeats or confirms it) this task will exercise.

### 6. [Low] "Board fixture routed 25% over its declared maximum" presumes a mechanism this task does not own and does not cite

AC1's fixture requires a per-net declared maximum length, which is
FEAT-058's (`#486`) attribute, consumed here but never named in `#613`'s
body (it is named explicitly in `#487`'s math, `L_routed = 1.25 L_max`,
but `#613` only says "declared maximum" without citing `#486`). A reader
of `#613` alone, without independently pulling `#487`, cannot tell where
the "declared maximum" comes from or confirm it is stable at the time
this task is implemented. **Recommendation:** cite `#486` directly.

## What's solid

- The fail-first ordering rationale — "a check that passes on a board
  that should fail is indistinguishable from a check that is not
  running" — is exactly the right adversarial framing for an
  externally-adjudicated test, and it is enforced as an explicit,
  sequenced acceptance criterion (AC1 before AC2), not just stated as
  philosophy.
- AC3 (opt-in, hermetic-by-absence, never a required gate a missing
  container turns green) correctly generalizes the existing
  `ToolLocator`/`Assumptions.assumeTrue` skip pattern already used for
  `iverilog`/`ghdl` in this codebase, and explicitly forecloses the
  "missing tool reads as passing" failure mode.
- K18-2's re-planning path (demote to advisory, narrow the written claim
  from "a tool enforces" to "a documented format," record on `#487` and
  `#313`) is a well-designed escape hatch: a narrowed claim is defined as
  a successful outcome, which removes the incentive to silently stall or
  to fake a passing container run.
- Running external tooling out-of-process rather than linking against it
  matches this repo's recorded plugin-trust-boundary decision
  (`ARCHITECTURE.md`, "external tool integrations... already sit on that
  subprocess boundary... sidesteps GPLv3 in-process-linking hazards") —
  no licensing hazard from the KiCad dependency as scoped.

## Verdict rationale

Not `should-not-proceed`: the failing-direction-first design and the
K18-2 narrowing path are sound engineering that should survive a rework.
Not `sound-with-concerns`: the task's headline feasibility argument
("joins a harness CAP-05 already funds") is checked against `#298`'s own
text and does not hold, the ordering graph omits the dependency that
claim implies exists, and the one criterion that actually distinguishes
"the check ran" from "the check is decorative" (naming the net) has no
specified assertion mechanism — three findings that go to whether this
task is buildable as filed, not polish. This needs the harness dependency
and the net-naming assertion mechanism specified before implementation
starts.
