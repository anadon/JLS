# Issue #838: TASK-C333-5: the same design at partition counts 1, 2, 4 and 8 produces byte-identical output under both the loopback and the reordering double, and nothing in the output reveals the count
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This task files the "invariance suite" evidence artefact for FEAT-056 (#333),
a not-yet-built distributed/partitioned simulation capability. The task
itself is well-scoped as *a test suite*, and its citations into the
existing `ChaosTransport`/`LoopbackTransport` doubles check out. But the
acceptance criteria assert unconditional "byte-identical" guarantees whose
sole stated caveat lives in a document that does not exist in this repo,
the criteria depend on infrastructure (partition files, boundary
marshalling, the barrier protocol, the refusal path) that is 100% unbuilt
and whose own prerequisite tasks are all still open, and the negative
assertion in AC-3 — the one criterion the parent issue calls out by name as
the hardest to pin down — is left just as unpinned here as the parent
warned against.

## Findings, most severe first

**1. The load-bearing citation for the "byte-identical" claim's own caveat does not exist in the repo.**
Parent #333 says: *"every byte-identity claim in criteria 1, 2 and 5 rests
on that assumption"* — the assumption being cross-platform determinism —
and cites `docs/parity-contract.md:469-477` verbatim for the caveat that
"bit-identical" currently means "bit-identical on one platform." I ran
`Glob docs/*.md` and there is no `parity-contract.md` in this repository
(confirmed absent; the string only appears inside other agents' issue-review
output files, never under `docs/`). AC-1 and AC-2 of #838 assert flatly
"is empty" / "matches... every time" with no single-platform qualifier
inherited into the task itself. If the suite runs across the project's own
multi-platform CI (JDK 25 lane, advisory newest-GA lane, macOS headless
lane per #265) before TASK-C333-1 (#830, the cross-platform determinism
experiment) lands, a genuine platform-level nondeterminism (e.g. hash-order
or floating rounding differences) would be indistinguishable from a real
partition-count bug, and AC-5's mandated response — "raised as a
`REPLAN:`... treated as a revert" — would fire on the wrong cause.
*Recommendation:* either restate AC-1/AC-2 as explicitly single-platform
until #830 lands, or add #830 to `ordering_after`, and separately flag that
`docs/parity-contract.md` needs to be created (or the citation fixed) before
any of #333's byte-identity language can be trusted as written.

**2. `ordering_after` names two prerequisites; the real prerequisite closure is much larger and mostly unbuilt.**
`ordering_after` lists only TASK-C333-3 (#834, the advance rule) and
TASK-C333-4 (#836, the refusal). But per #333's own mermaid graph
(`P1 marshalling → P2 protocol → P3 invariance suite`) and its "Consumes"
section, this suite also needs: TASK-C333-2 (#832, boundary marshalling —
open), and, more fundamentally, all of FEAT-055/#332 (partition files and
boundary description — open, with its five subtasks #600/601/602/604/606
all open too), FEAT-014/#318 (partition-independent watch naming — open),
and FEAT-051's consumer review (#348 is closed, so that piece is done). A
`Grep -i partition` over `src/` and `test/` turns up **zero** hits related
to distributed/partitioned simulation — the only "partition" hits in the
codebase (`src/jls/Circuit.java:155,404,1354`) are the unrelated wire-net
graph partitioning routine. There is no partitioned run mode, no boundary
frame, no barrier, and no fixture format to write "committed fixtures... a
cut that actually crosses nets" (AC-4) against. This task cannot be started,
let alone finished, until a stack of currently-open work lands; the terse
`ordering_after` list understates that by omission, and a scheduler reading
only #838 could reasonably (and wrongly) conclude #832 is not a blocker.
*Recommendation:* expand `ordering_after` to the true critical path, or add
an explicit note that FEAT-055 (#332) and TASK-C333-2 (#832) are transitive
blockers even though not listed.

**3. AC-3's "structural check" is exactly the gameable half the parent issue itself warns about, and #838 does not close the gap.**
#333 body states plainly: *"A structural check on the artefact format
asserts no partition identifier, count, or field derived from either
appears in any output — this is the half each protocol child could satisfy
its own tests while violating."* #838 restates this near-verbatim as AC-3
but adds no concrete specification: no enumerated field list, no schema
reference, no definition of "structural" versus "grep for the literal
digits 1/2/4/8." As written, an implementer could satisfy AC-3 with a
`grep -c "partition"` over the VCD/watched-output text and call it done,
while a partition identifier leaks through timing jitter, event ordering
in a tie, or a hidden field name like `shard` or `worker` that a literal
string check misses — precisely the failure mode the parent issue predicts.
*Recommendation:* AC-3 needs a concrete check design (e.g., diff the output
byte-for-byte across counts *and* assert the artefact schema's field set is
identical to the count-1 baseline's field set) before this is actionable,
not just restated intent.

**4. AC-1's diagnostic requirement quietly requires new tooling that doesn't exist and isn't decomposed anywhere.**
AC-1: *"a failure names the first differing byte and the signal it belongs
to, not a bare mismatch."* This requires mapping a byte offset in a VCD
waveform dump back to the signal it belongs to — i.e., a VCD-aware
structural diff, not a plain `diff`/`cmp`. I searched the existing test
suite (`test/jls/VcdExportGoldenTest.java`, `VcdProbeExportTest.java`) and
the whole tree for existing "first differing byte" / signal-attribution
diff logic and found none. This is a real, non-trivial piece of tooling
(parse VCD, map byte ranges to `$var` declarations) that the task assumes
into existence without listing it as deliverable scope, cost, or an
AC of its own. At band `3-4 mw` this risk of underestimation is material.
*Recommendation:* either explicitly scope "byte-to-signal VCD diff
attribution" as a named sub-deliverable with its own estimate, or relax
AC-1 to a coarser guarantee (e.g., name the differing VCD `$var` line
number) that existing golden-test infrastructure can support more cheaply.

**5. AC-5 is a process/governance rule, not a testable acceptance criterion.**
"Any partition-count-dependent result found is raised as a `REPLAN:` on
#333 and treated as a revert, not absorbed as a fixture adjustment" cannot
be verified by CI, code review of the harness, or any automated means — it
describes what a human must do in response to a failure, not a property of
the suite. Listing it alongside AC-1..AC-4 as if it were equally
verifiable is misleading; it will either be silently un-verified at close,
or someone will invent a proxy check (e.g., "a REPLAN comment exists") that
doesn't actually establish the discipline was followed.
*Recommendation:* move AC-5 out of the acceptance-criteria list into a
"process note" or a completion-checklist item, distinct from the
machine-checkable ACs.

**6. Feasibility/cost risk: the band (3-4 mw) does not obviously cover the true scope once findings 1-4 are priced in.**
Building fixtures with a real cross-net cut (AC-4) requires FEAT-055's
partition-file format to exist and be stable; building the byte-to-signal
diff tool (finding 4) is new infrastructure; and the suite must run
correctly across the same multi-platform CI matrix whose determinism is
itself unverified (finding 1). None of that is reflected in the 3-4 mw
band, and #333 itself admits its overall 10-18 mw band for this whole
feature is "unvalidated by decomposition." This specific task inherits
that uncertainty without flagging it.
*Recommendation:* re-price after FEAT-055 and the marshalling/protocol
tasks land and their actual artefact/fixture shape is known, rather than
carrying the current estimate forward unexamined.

## What's solid

- The `ChaosTransport` citation (`test/jls/collab/net/ChaosTransport.java:19-21`) is accurate: the javadoc there does describe a deterministic bounded holdback, never a wall-clock sleep, exactly as AC-2 requires — this AC is well-grounded in real, already-shipped code.
- Testing over both `LoopbackTransport` and `ChaosTransport` as the two "doubles" is a sound test-design choice given what already exists in `src/jls/collab/net/` and `test/jls/collab/net/`.
- Scoping this as a distinct "evidence" task separate from the protocol task (rather than letting the protocol author grade their own homework at count 2 only) is a good process decision, explicitly justified in #333's rationale, and is worth keeping even after the rework above.
- The `ArchitectureRulesTest.java:249-262` socket-confinement rule cited by the parent feature is real and correctly described, and nothing in #838 proposes violating it.

## Verdict rationale

`needs-rework`: the task's own acceptance criteria are internally sound in
intent but (a) rest on a citation to a nonexistent document for their core
"byte-identical" guarantee, (b) understate their true prerequisite chain,
(c) reproduce rather than close the exact gameability gap the parent issue
warned about, and (d) bury a nontrivial new-tooling requirement inside a
one-line AC. None of this is fatal to the concept — the suite is a
reasonable idea — but it should not be picked up as scoped until these are
addressed.
