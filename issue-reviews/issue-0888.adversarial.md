# Issue #888: CAP-39: a mixed-alphabet drawing — balanced-ternary datapath, byte-symbol bus, binary control — simulates, probes, autogrades and refuses honestly in one circuit, while every binary circuit stays byte-identical
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually asks for

A capstone ("CAP-39") that draws one circuit mixing balanced-ternary, a
`[0,255]` byte-symbol bus, and binary control, then simulates/probes/
autogrades/refuses it, while the entire existing binary golden corpus stays
byte-identical. It is filed as the disposition for #344/#361 after #295
(CAP-03) was closed "not planned" by the same-day product review (#508). It
requires #322, #344 and #361 — none of which have landed a single line of
code (verified below) — and it was amended by the issue's own top comment
before I finished reading it, which is itself a finding.

## Findings, most severe first

### 1. AC-5 / KC-39-2 describe an architecture its own required feature does not build (CRITICAL)
AC-5 requires "the fast tier and the generic tier agree on a seeded
differential corpus," and KC-39-2 says "if the two-tier differential oracle
cannot be made to hold... the generic tier is withdrawn and the program
falls back to the bounded fast tier alone" — i.e. this issue assumes a
*second*, unbounded-N implementation ("generic tier") exists alongside the
bounded plane-encoded kernel, with the two required to agree forever, or
else be walked back. But #344 (FEAT-028), the required feature that actually
owns the operator kernel, is unambiguous: "Radix ≥ 6. Refused on
arithmetic — see §3. **Not deferred, refused**, with the limit shown," and
its §3 derives the plane-count arithmetic (`⌈log2(r+3)⌉`) that makes radix 6
a hard wall, not a slower path. #361 (FEAT-029) inherits the same bounded
kernel and never mentions an arbitrary-N mode either. There is no "generic
tier" anywhere in the two features #888 requires for exactly this
functionality — AC-5/KC-39-2 are testing an implementation that nothing in
the dependency graph is scoped to build. Recommendation: either #344/#361
must be amended to add the generic-tier scope AC-5 presupposes, or AC-5 and
KC-39-2 must be struck/rewritten against the bounded-only kernel that
actually exists on the graph.

### 2. The issue's own comment retracts its data model but the body was never rewritten (CRITICAL)
The single comment on this issue (posted 11 minutes after the issue itself)
opens: "The domain lives on ports, not nets — supersedes PR #887," quoting
the maintainer: "a net carries values and width and **no alphabet**... This
deletes the 'net interval' concept the proposal was built on." It then
instructs: "Read every mention of 'the net's interval' in this capstone's
body as 'the driving port's domain.'" But the body's acceptance criteria are
literally written against the net model that was just deleted: AC-2 ("second
driver on a non-binary net"), the walkthrough ("Attempt a second driver on
the ternary net; read that refusal (non-binary nets are single-driver in
this iteration)"), and KC-39-3 ("Nothing drawable may reach `interval !=
[0,1]`" — interval-on-what is now ambiguous). The new AC-10 explicitly
states "No `WireNet` carries a domain field at any commit" — which leaves
the single-driver-on-a-non-binary-net refusal (AC-2, walkthrough step 6)
without a stated enforcement point: if the net has no domain, what,
mechanically, blocks a second differently-typed driver from attaching, and
where is that check now specified? Find-and-replace-by-comment is not a
substitute for rewriting the acceptance criteria against the corrected
model. Recommendation: this issue should not be worked until its own AC/
walkthrough section is edited in place to match the port-only model; a
reader-side text substitution instruction is not an executable spec.

### 3. `band_mw` cites a "§Cost" section that does not exist in this issue (HIGH)
The machine-readable header states `band_mw: "see §Cost — inherits #322 +
replanned #344 + amended #361"`. I read the full body: the section headings
present are Why/Outcome/Walkthrough/Acceptance criteria/Kill criteria/
Required features/Open questions/Governance motions. There is no `## Cost`
heading anywhere. Either this is a copy-paste artifact left over from the
capstone template (used elsewhere in this tracker, e.g. #322/#344 both have
real `## Cost` sections), or the cost for this specific capstone was never
actually computed — the pointer resolves to nothing. As filed, there is no
verifiable cost figure attached to #888 at all, only an unresolvable
citation to three other issues' costs, none of which sum cleanly (#344
alone documents a self-acknowledged 2.18x gap between its own band and its
own task-row sum, left as an open, unresolved question). Recommendation:
add a real `## Cost` section or correct the pointer.

### 4. Walkthrough step 8 depends on tooling that doesn't exist and isn't in scope anywhere on the graph (HIGH)
Step 8: "`lint radix` over the fixture reports zero implicit crossings
(inherited from CAP-03 step 5)." I verified against the live tree:
`src/jls/JLSStart.java`'s `FLAGS` table (the CLI's authoritative flag list,
per ARCHITECTURE.md) has no `lint` entry at all — `-h -b -i -s -t -d -p -v
-r -vcd -export -board -pins -savetext` is the complete list. The `--lint
radix` invocation is specified nowhere in #344 or #361 (I read both bodies
in full — #344's connection-refusal scope is editor-site and load-time
refusals only, never a linter subcommand). The only place `--lint radix`
is actually specified is #295 (CAP-03) itself, e.g. "Run `jls --lint radix
machines/t3-mixed.jls`; observe `implicit radix crossings: 0`" — and #295 is
the capstone #508's product review formally closed as "not planned" the
same day. #888 "inherits" a concrete CLI deliverable from a capstone that
was just declared not-planned, without adding it as scope to #344/#361 or
naming a fourth required feature/task that would build it. As written, step
8 of the acceptance walkthrough is unbuildable by anything currently on the
dependency graph. Recommendation: either add the `--lint` subcommand as
explicit scope somewhere in #344/#361, or drop step 8/replace it with a
mechanism the required features actually deliver.

### 5. Feasibility: three fully-unimplemented required features, filed the same day as a review that deprioritized their beneficiary (HIGH)
Verified against HEAD (`3b6d6ec`): `docs/simulation-semantics.md` §2 is
still literally titled "Value domain: two states plus HiZ" and states
"There is no unknown/X state anywhere in the simulator"; `grep -n radix
src/jls/elem/Put.java src/jls/elem/WireNet.java` returns nothing. #322,
#344 and #361 (the three `requires_features`) each list every one of their
tasks as "not filed"/"unfiled"/"ABSENT at <evidence commit>" — zero code
exists for any of them. #344's own Open Question 1 records an unresolved
2.18x gap between its cost band and its task-row sum ("Blocks nothing, but
must be answered before the band is used for scheduling" — i.e., not yet
answered). Meanwhile #508 — the maintainer-commissioned product-and-
direction review, posted five days before #888 and updated the same day
(2026-08-08) — prices the entire filed programme at "~600–1,700 mw (central
≈1,100 mw ≈ 22 maintainer-years as priced) against bus factor 1," explicitly
closes CAP-03 (the sole capstone that named #344/#361 as required) as "not
planned," and recommends a completely different two-quarter wedge (grading
integrity, a CPU-boots-Linux flare, accessibility, distribution) with
nothing resembling the N-ary program on the keep list. #888's body engages
with #508 only to note that CAP-03 "stays CLOSED" and that this issue is
the "disposition" for its orphaned dependents — it never engages with #508's
central arithmetic or bus-factor-1 warning, which apply with undiminished
force to reviving the same dependency chain under a new capstone number the
same day. Recommendation: before filing further child tasks, #888 should
state explicitly why it is exempt from #508's "the arithmetic does not
close" verdict, not merely that its formal predecessor capstone is closed.

### 6. Q6 is "BLOCKING" yet the walkthrough it blocks is presented as final acceptance criteria elsewhere in the same issue (MEDIUM)
Open Question 6: "Walkthrough final content (BLOCKING) — the acceptance demo
defines the element roster's completeness census. The steps above are the
proposal's candidates, offered for tuning." But the "Walkthrough (the
acceptance run)" section above it is written as the authoritative acceptance
run, and AC-1 through AC-10 (added by the comment) are phrased as fixed,
numbered, gradeable criteria — not "candidates... offered for tuning." A
reader cannot tell from this issue whether AC-1..AC-10 are binding today or
provisional pending Q6's resolution. Recommendation: either mark AC-1..AC-10
explicitly provisional-pending-Q6, or resolve Q6 before publishing them as
acceptance criteria.

### 7. AC-4/KC-39-1's performance gate names no actual baseline, and KC-39-1 makes the gate a hard, unappealable revert (MEDIUM)
AC-4 requires "no measurable regression for binary circuits against a
**named** baseline," and KC-39-1 escalates: "Any commit that moves a binary
golden byte or measurably slows the binary warm loop is reverted, not
renegotiated." No baseline — benchmark name, harness, numeric threshold, or
statistical test — is actually named anywhere in #888. "Measurable" is left
to whoever runs the benchmark to define, which is exactly the kind of
under-specified acceptance criterion that can be gamed in either direction:
a contributor can pick a noisy enough harness to hide a real regression, or
a reviewer can invoke KC-39-1's "reverted, not renegotiated" absolutism over
noise that was never a real regression. Recommendation: name the benchmark
and the tolerance in this issue (or point at the specific one #344/#322 are
supposed to establish) before KC-39-1 is treated as enforceable.

### 8. Prerequisite features' evidence pins to a commit that does not exist in this repository (MEDIUM)
#322, #344 and #361 all pin their extensive per-line code claims (file/line
citations, `git grep` counts, "ABSENT at 2d0ca9d") to `evidence_commit:
2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`. I checked: `git cat-file -e
2d0ca9d` fails ("Not a valid object name") and `git rev-list --all | grep
2d0ca9d` returns nothing across all 267 commits in this repository's
history. Every prerequisite feature's "supersession check at pickup" rule
(re-derive the cited evidence at the current commit before starting work)
is therefore unexecutable as written — there is no commit to check it
against. This doesn't necessarily mean the underlying claims are false (the
element count and CLI-flag claims I independently re-verified against HEAD
did check out), but it means #888's entire prerequisite chain rests on
citations that cannot be mechanically re-verified the way the issues
themselves demand. Recommendation: re-anchor #322/#344/#361's evidence
commit to something that actually resolves in this repo, or note plainly
that it is a hypothetical/planning-only reference.

### One thing that is solid
AC-6/AC-8's "every pre-existing element type reports `[0,1]`, swept over
`ElementRegistry`" rests on a real, checkable mechanism: `ElementRegistry.
java` exists at HEAD with exactly 35 registered types (`grep -c "new
ElementType(" ElementRegistry.java` → 35), matching the count all three
prerequisite issues cite. AC-7 ("a binary user's experience is nothing
happened") is likewise well-specified and testable against the existing
golden-corpus infrastructure (`BatchSimulationGoldenTest`, `SequentialGolden
Test`, `VcdExportGoldenTest`) named in ARCHITECTURE.md's Test layout
section. KC-39-3's migration-safety property ("nothing drawable may reach
`interval != [0,1]` until the element family lands") correctly mirrors
#344's own invariant 5. These are fine as written; move on.

## Caveat on ARCHITECTURE.md
ARCHITECTURE.md, which this review was told to ground against, currently
states "There is no element registry yet — issue #78 will introduce one and
collapse most of this." That is stale: `ElementRegistry.java` already exists
in the live tree and #78 (open) describes most of that program as already
landed via merged PRs (#238/#246/#261/#271). This is not a defect in #888
itself, but it means #888's implicit assumption that "sweep over
ElementRegistry" is a stable, uncontested mechanism should be treated with
some caution — #78 is still open and actively restructuring the same base
classes (`Element`/`LogicElement`) that #888's element-family work would
extend.
