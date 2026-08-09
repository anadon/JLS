# Issue #494: Machine calibration, part 2 of 2: guest-side boot facts, the minimum SoC, what is still unmeasured, and how to re-measure (rescued from a branch that will be deleted)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Not documentation. It is a **citation-graph rescue**. Seventy-seven filed issues resolve
acceptance and kill criteria against `docs/machine-calibration.md:NNN`, and that path
exists only on a branch the maintainer has condemned. The pain is real and already
recorded in this very directory: `issue-reviews/issue-0407.adversarial.md` §2 spends a
whole finding on "the document this entire task edits does not exist anywhere in this
repository's history"; `issue-0300.adversarial.md`, `issue-0296.adversarial.md`,
`issue-0362.adversarial.md`, `issue-0335.visionary.md` and `issue-0842.visionary.md` all
hit the same wall independently. So the *goal* is correct and urgent. The **mechanism** —
paste 465 lines of prose into a GitHub issue body, in two parts, with a hand-written
line-number translation table — is the weakest of at least three available routes, and the
issue never considers the other two.

## The premise is factually wrong, and it is load-bearing

> "a link to a path or a commit on a deleted branch preserves nothing"

Deleting a branch deletes a **ref**, not a commit. `git tag rescue/machine-calibration
2d0ca9d` (or a lightweight ref under `refs/rescue/`) keeps the object reachable
permanently, costs one push, requires merging nothing, and does not contradict the ruling
that the branch will not be merged. GitHub then serves
`blob/rescue/machine-calibration/docs/machine-calibration.md#L676` — meaning **all 77
`:NNN` citations become live permalinks with a find-and-replace of the ref**, no
translation table, no two-part split, no verbatim re-typing, and the sibling branch-only
documents (`parity-contract.md`, `virtual-hardware-parity.md`) that §6.10/§6.11 reference
survive too instead of becoming "unresolvable" as this issue concedes they will.

That single move dissolves the entire problem this issue exists to solve. It should be
done today regardless of what happens to the rest of this review.

## The better durable route: land the file, not the transcript

A tag is preservation. It is not *reachability for the working project*. For that, the
right seam is the obvious one the issue never names: **commit `docs/machine-calibration.md`
to `master` as a single documentation file.** Cherry-picking one `.md` with zero code is a
categorically different act from merging `claude/jls-virtual-hardware-linux-njsoma`, and
nothing quoted here rules it out. It preserves line numbers *exactly* (that is what a file
is), it is greppable by every future reviewer who hits the wall above, it is diffable when
a number gets re-measured, and — decisively — it is **correctable**. An issue body is
append-only in practice; §6.2 says 121.5 and 245.5 are "unreconciled" and §6.1 says α was
"never measured," and both of those *will* change. A record whose whole value is truth
maintenance must live somewhere a commit can amend it.

The line-number translation table is itself the proof that the container is wrong. Note
which citations needed no table: "Section-number citations resolve directly against the
headings below, **which are unchanged**." Line numbers required hand-maintained
scaffolding; section anchors survived the container change for free. The generalizable rule
— worth writing down once, in `CONTRIBUTING.md`, and worth more than this rescue — is
**cite documents by section anchor, never by line number**. Line-number citation is what
made 77 issues fragile against a file move in the first place.

## Section 6 is not documentation. It is a backlog, and burying it is the real loss

§6 lists nine experiments plus one capability gap, each with a named cheapest experiment,
an explicit settle criterion, and a duration estimate ("hours," "one afternoon," "days").
That is a *ready-to-file work queue*, and §6.11 even ships the mapping table. Pasted into
an issue body it can never be assigned, closed, or blocked-on; the citing issues will keep
pointing at prose. The reframing: **§6 becomes ten issues** (or one tracker with ten
sub-issues, which this repo's tooling supports), each closable by evidence. Then
"6.2 is settled" is a state the project can actually be in.

More importantly, the highest-value items in §6 **have nothing to do with booting Linux**,
and stapling them to a doomed Linux program is what will get them ignored:

- **§6.7, cross-JDK/cross-OS determinism.** README sells the container image to
  autograders, VCD export to autograders, and `docs/batch-interface.md` as a "documented
  stability contract." Nothing in the tree asserts a run is bit-identical across a JDK or
  OS change. That is a hole in the *shipped product promise* of an educational grading
  tool, justifiable with zero reference to RISC-V. It should be its own issue, framed that
  way, and it probably outranks everything else in the document — as §6.7 itself says.
- **§6.9, the `maxTime` event drop.** Verified at `src/jls/sim/Simulator.java:224-233`:
  `eventQueue.poll()` → `dupCheck.remove(event)` → `now = event.getTime()` →
  `if (now > maxTime) break`. `docs/simulation-semantics.md:35-40,107-120` specifies the
  truncation but is silent on the dedup eviction, so this is a **normative-spec gap in the
  project's own flagship spec**, not a Linux prerequisite.
- **§6.8 / §5.2, `Memory`.** `DENSE_CAPACITY_LIMIT = 1 << 22` gates on **word count** while
  its own code comment reasons in bytes ("32 MB of longs") — a width-blind gate that
  over-allocates 8× at 64-bit words and under-serves at 8-bit ones. Byte lanes are already
  owned by `docs/capability-roadmap/sweep-05-system-and-interfaces.md` §C and
  `riscv/README.md`'s scope note. Both are classroom-visible defects on their own terms.

## Alignment with the project's arc — where this genuinely fits

`ARCHITECTURE.md`'s "Simulation execution strategy" decision (#221) is the one place in the
tree this document belongs. It records the interpreter as the sole strategy, and names its
revisit trigger: *"a concrete CPU-scale design on the `riscv/` trajectory that is unusably
slow interactively."* That recorded decision currently carries **no measured evidence
whatsoever**. §8's status table is precisely the evidence — 1.2–6 h structural boot,
18,800–19,500 cycles/s live, 1.5 s/char — and the five normative rules (§2.5, §2.6, §4.5,
§4.6, §7.3 step 6) are exactly the citation discipline that decision needs to stay honest
under pressure. Land those ~40 lines next to the decision they govern and the arc gets
stronger. Land 1,124 lines of derivation in two issue bodies and it gets a rumor mill.

## The out-of-the-box read: this document's best output is a "no"

Taken at face value, §8 is a **decision-grade negative result**. 16 MiB is *exactly* the
dense-store cliff with zero headroom; the image alone is 99.2% of `MAX_CIRCUIT_TEXT_BYTES`
(`FileAbstractor.java:65`, verified), leaving ~0.5 MB for a circuit; the minimum UART is
three *byte* addresses and §6.8 says byte lanes do not exist; the default time limit is
1,920–2,300× short; α, the dominant input, has never been measured; and the honest
structural band is 1.2–6 hours. The mature move for a single-maintainer pedagogy tool is
not to keep funding the ten experiments — it is to write **one recorded decision in
`ARCHITECTURE.md`**, in the exact rationale-plus-revisit-trigger shape that section already
uses: *"Booting Linux on a drawn JLS circuit: out of scope. Here is what was measured, here
is what would have to change."* That is durable, it is two hundred words, it makes most of
the 77 citing issues closable rather than merely resolvable, and this rescue becomes its
linked appendix instead of its foundation.

## Disregarding the stated framing

I am setting aside the issue's implicit acceptance criterion — "reproduce sections 5–8
verbatim so citations resolve" — because verbatim-into-an-issue is the *fourth*-best way to
achieve it, behind (1) a rescue tag, (2) a single-file commit to `master`, and (3) §6-as-
issues. Verbatim fidelity is the right *requirement*; a GitHub issue body is the wrong
*store*. Nothing here disputes the content's value; §5 in particular is emulator-measured,
JLS-independent, and genuinely durable — which is exactly the argument for putting it
somewhere the project can maintain it.

## Concretely

1. Tag `2d0ca9d` before the branch is deleted. Today. Independent of everything below.
2. Commit `docs/machine-calibration.md` to `master` unmodified; rewrite the 77 citations to
   the in-tree path (line numbers survive intact) or to `§` anchors.
3. File §6.7, §6.9 and §6.8 as standalone issues framed against the classroom/autograder
   product, not against Linux.
4. Add a recorded decision to `ARCHITECTURE.md` under "Simulation execution strategy"
   citing §8's status table and stating the scope call on structural Linux boot.
5. Add "cite by section anchor, not line number" to `CONTRIBUTING.md`.
6. Close #494 and its part-1 sibling as superseded by (1)+(2).
