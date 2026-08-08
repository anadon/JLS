# Issue #354: FEAT-006: an hours-long batch run has no unannounced ceiling — it can be suspended, watched, interrupted cleanly, and address a real guest image
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

All four code claims were verified against current HEAD (`3b6d6ec`, shallow
clone; the pinned `evidence_commit` `2d0ca9dcd9db78b36c3cc7...` is
unresolvable in this checkout — see finding 6): `JLSInfo.java:69`
(`defaultTimeLimit = 100000000`), `Simulator.java:224-234` (`dupCheck.remove`
before the `now > maxTime` clamp), `BatchSimulator.java:75-90` (`pause`
identical to `stop`, with the "doesn't make sense to pause" javadoc intact),
and `Memory.java:1224/1234/1072/1085-1086` (`DENSE_CAPACITY_LIMIT = 1<<22`,
eager `long[]`+`BitSet` allocation). The "second full copy" claim for
TASK-0013 is also real: `Memory.java:1309` is `mem = initMem.copy();` inside
`initSim`. The four `git grep -c` negative-evidence claims (`NO_TIME_LIMIT`,
`addShutdownHook`) were reproduced against HEAD and are still true. The
`blocked_by`/`blocks` mirror against #353 is consistent both directions
(#353's `blocks` lists 354 with the matching rationale). The scope-boundary
carve-outs (#232, the `MAX_CIRCUIT_TEXT_BYTES` file bound, FEAT-036, FEAT-035)
are accurately distinguished from what this issue touches.

## Findings, most severe first

**1. (High) The planned heartbeat contradicts the batch interface's own normative stream contract, and the issue never names that document for review.**
`docs/batch-interface.md` is marked "Status: normative, and a stability
contract" and states the exit-status/stream table plainly:
```
| status | meaning          | streams                                        |
|--------|------------------|------------------------------------------------|
| 0      | run completed    | results on stdout (section 3), stderr empty    |
```
Global Invariant 4 here says: *"stdout stays frozen. The heartbeat goes to
stderr. Every byte a conforming batch consumer sees on stdout is unchanged."*
Once a user opts into the heartbeat (Open Question 3's recommended default),
a status-0 run will write to stderr — unconditionally breaking
`docs/batch-interface.md`'s documented promise, which changing per that
document's own rules "requires a CHANGELOG entry and either a major version
bump or a compatibility flag that preserves the old behavior." Global
Invariant 5 names `docs/file-format.md` as the review target for the *memory*
change but no invariant, no §3 interface note, and no Completion Criteria
item names `docs/batch-interface.md` at all for the *heartbeat* change.
**Recommendation:** add an explicit invariant requiring
`docs/batch-interface.md` to be amended (with the CHANGELOG/version-bump
process it itself mandates) before TASK-0014 lands, or restate "stderr
empty" there as conditional on heartbeat being disabled.

**2. (High) The byte-budget arithmetic in §3 conflates JVM-internal storage cost with guest-visible capacity, leaving Open Question 2 gameable by word-width choice.**
$B_{\text{dense}}(n) = 8.125n$ bytes is stated as "independent of the
declared word width whenever bits ≤ 64" — true, because `DenseWordStore`
always backs every word with a `long` (`Memory.java:1085`) regardless of the
declared `bits`. But the guest-RAM narrative that motivates the whole
feature ("a 32-bit dense store tops out at exactly 16 MiB... they meet with
zero headroom") is a *different* quantity: guest-visible bytes at 32-bit
width are `4n`, not `8.125n` — a ~2x internal/guest ratio. At an 8-bit
(byte-addressable) word width, the same $n_G = G$ words needed for a
$G$-byte guest image cost `8.125n_G` internally — a ~8x ratio. Open Question
2's recommended default ("state $B_{\max}$ as a number giving at least 2x
over a 16 MiB 32-bit guest") only pins the 2x figure for the 32-bit case; it
never states which word width the acceptance fixture (§5 criterion 4, "a
fixture with a memory sized for a 12 MiB guest at the declared word width")
must use. Because the ratio swings 4x between 8-bit and 32-bit words, a
narrow implementation could satisfy criterion 4 at 32-bit while leaving an
8-bit guest image (arguably the more realistic RISC-V byte-addressable
target for CAP-02/CAP-03) with far thinner real headroom than "2x" implies.
**Recommendation:** pin the canonical word width(s) the acceptance fixture
and headroom factor must be validated against before TASK-0013 is filed, not
left as "the declared word width" with no default.

**3. (Medium) The shutdown-hook/sim-thread race that would reproduce the very defect being fixed is undescribed.**
§3's Concurrency model says: *"The shutdown hook runs on the JVM's hook
thread and may only flush and close — it must not read the queue."* That
constrains what the hook may touch, but not how it avoids racing the sim
thread, which is concurrently writing to the same output stream (results on
stdout per `docs/batch-interface.md` §1, VCD samples via
`BatchSimulator.toVcd`/probe traces, per #353). "Flush and close" invoked by
the JVM hook thread while the sim thread is mid-write to that stream is
exactly the "interrupt truncates output mid-record" failure this issue's own
Abstract cites as the problem to fix. No lock, flag, or handoff is specified
between the two threads. **Recommendation:** state explicitly how the hook
learns the sim thread has reached a safe point (e.g., `beforeEvent()` checks
an interrupt flag and the hook blocks on a latch) before closing the stream.

**4. (Medium) The critical-path adjudication (TASK-0011) is unresolved, feeds directly into TASK-0012 and TASK-0014, and its interaction with pause/resume is unanalyzed.**
Open Question 1 recommends re-queuing a past-limit event rather than the
current silent drop, explicitly because "the interactive simulator lets a
user raise the limit and resume against the same queue." But TASK-0014's
`pause`/resume is a *different* resumption path than "raise the limit and
resume," and the issue's own criterion 2 (pause/resume byte-identity against
an uninterrupted control run) never states what happens if an event is
re-queued while paused, or whether a re-queued event can fire out of
`dupCheck`-consistent order relative to newly posted events after resume.
§6 calls TASK-0011 → TASK-0012 "necessity, not convention" because the
sentinel makes the clamp "unreachable" — but that argument is really about
whether removing the clamp's reachability *is itself* the adjudicated
decision, not about whether re-queue semantics compose safely with pause.
The composition is asserted only for "pause is not stop" (§5 criterion 2)
and separately for "the past-limit disposition is observable" (§5 criterion
3); nothing composes the two, despite TASK-0014 explicitly depending on
TASK-0012's sentinel and TASK-0011's adjudication landing first.
**Recommendation:** add an integration criterion (or fold into criterion 1)
asserting pause/resume behavior specifically around a re-queued past-limit
event, or state explicitly that this composition is out of scope and why
that is safe.

**5. (Low) Global Invariant 5's named review document does not currently document the field being changed.**
Invariant 5 says the byte-budget change "is reviewed against
`docs/file-format.md`, not decided inside the element." But
`docs/file-format.md`'s `Memory` attribute row (line 307) documents `init`,
`initrle`, and `sync` — `cap` (`Memory.java:443`,
`output.println(" int cap " + capacity);`), the actual saved capacity field
TASK-0013 touches, appears nowhere in that document. The named review target
has no existing baseline to check the change against for the one field that
matters. **Recommendation:** either point TASK-0013 at adding the missing
`cap` documentation as part of its own scope, or don't cite
`docs/file-format.md` as if it already constrains this field.

**6. (Low) Evidence commit is unresolvable in this checkout; content claims independently reverified against HEAD instead.**
`git cat-file -e 2d0ca9dcd9db78b36c3cc7...` fails ("bad object") in this
shallow clone (267 commits, `git rev-parse --is-shallow-repository` = true),
as does the later commit `3a81a4a7...` cited for
`docs/plan/evidence/BRIEF.md` (which also does not exist anywhere in the
current tree — `find . -iname BRIEF.md` returns nothing). This is most
likely a shallow-clone limitation rather than a fabricated hash, since every
line-level code claim checked out exactly against current HEAD. But it does
mean the headline motivating figures — "a structural boot is measured at
1.66-1.72 h and needs at least 12 MiB of guest RAM" — rest on a document
this reviewer could not locate or verify at all. **Recommendation:** land
`docs/plan/evidence/BRIEF.md` (or fetch full history) so the boot-time/RAM
figures driving the CAP-02/CAP-03 motivation are checkable, not just cited.

## What holds up

- Every code anchor for the four ceilings (time limit, dupCheck-before-clamp
  drop, pause≡stop, dense memory cliff) is accurate and current.
- The out-of-scope carve-outs (#232's per-event cost, the file-decompression
  bound at `FileAbstractor.java:65`, FEAT-036's write-mask, FEAT-035's
  checkpoint serialization) are correctly distinguished from this feature's
  actual surface — no scope creep detected there.
- `-d`'s existing "positive integer" validation
  (`JLSStart.java` "-d requires a positive integer time limit") is correctly
  identified as the branch TASK-0012 must leave alone while adding a
  sentinel spelling.
- The `blocked_by: [353]` / `blocks: [354]` edge is mirrored correctly and
  the stated rationale (raising the limit without fixing #353's quadratics
  "converts a fast failure into a slow one") is consistent with #353's own
  text.
