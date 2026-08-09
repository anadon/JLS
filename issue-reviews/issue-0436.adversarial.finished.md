# Issue #436: TASK-0005: inserting one element into a saved circuit changes a bounded number of lines, because every reference names a permanent id instead of a save-time position
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The root-cause diagnosis is real and well evidenced, and I independently reproduced its
load-bearing claims against the checked-out tree (HEAD `d6bc8dd`, descendant of the issue's
canonical pin `8288226`). But the issue's own Method (§8) cannot pass its own Completion
Criteria (P1/P2) as written, the `blocked_by: 315` edge is contradicted by the issue's own
evidence, and the acceptance constant `C` is left unpinned. Two prior review comments on the
thread already found #1 and #3 below; I re-derived both independently against the code rather
than trusting the comments, and neither correction has been folded into the issue body itself —
which matters, since the body instructs "citations re-derived... at pickup," implying an
executor is meant to work from the body, not archaeology through the comment thread.

## Findings, most severe first

### 1. (Blocking) §8's checklist cannot satisfy P1/P2 — the issue's own evidence already proves it, but the body doesn't say so

O4's harness is wire-free `AndGate` blocks, and the issue states plainly: *"every one of the 196
non-inserted changed lines is an `int id` renumber alone."* I confirmed independently that `id`
is a base attribute documented as **never** omitted (`docs/file-format.md` base-attributes table,
"omitted when: never"), backed by `BASE_ATTRIBUTES` in `src/jls/elem/Element.java:200-203` and
the unconditional `for (Attribute attr : savedAttributes())` loop that emits it for every element.
§8's checklist only touches the reference-emitting site (`WireEnd.java:605-613`, confirmed still
the sole site via O3's `git grep`) and the loader's `ref`/`sref` arms — it never proposes changing
`id`'s emission. Applying §8 literally therefore changes **zero** bytes of O4's measured diff:
δ(200) stays 206, δ(800) stays 806, and P1/P2 — which the Completion Criteria gate on literally —
fail identically to the pre-fix state.

This is not a contingent risk to discover during implementation: Open Question 2 ("Does the `id`
base attribute keep being emitted per block?") is marked *"Blocks execution if H2 is confirmed,"*
and O4's own data already entails H2 is true for the exact fixtures the issue itself specifies.
The issue is self-contradictory as filed: it instructs the executor to write the ratchet, in
order, before resolving a question its own evidence already answers, and §7 (Interface & Data
Contract) never states that the `id` base attribute's emission rule changes — a normative promise
(`docs/file-format.md`'s "never omitted") that this task must break to succeed. A prior comment on
this thread (id 5227258817) reaches the same conclusion and proposes a fix (stop emitting `id` for
`sref`-form blocks); I confirmed that correction is consistent with the code, but it lives only in
a comment, not the body.

**Recommendation:** amend the issue body — not just a comment — to add a §8 line item that stops
emitting `int id N` for `sref`-form blocks, update the `docs/file-format.md` base-attribute table's
"never" to state the `sref`-form exception, and add the corresponding `§7.1`/`§7.6` contract
language. Until that lands in the body, this issue is not actionable as filed.

### 2. (High) The acceptance constant C is not pinned, only "recommended"

Open Question 3 asks "What is the stated constant C, and on which fixture?" and answers only with
a *"Recommended default"* (≤12 lines, ≤2 hunks, borrowed from #334, on an unnamed "wired fixture
with interleaved replicas"). Completion Criteria then requires *"The ratchet's constant C is a
literal in the test with a comment stating its derivation"* — but the issue never commits to that
number or that fixture; it defers the choice to the executor. That is gameable exactly the way
§11's own Threats to Validity warns about for a different reason: an executor can choose a
generous C, or a fixture that isn't the worst case (e.g. omits interleaved replicas, contra O5's
warning that a `legacy`-replica fixture always inserts at rank 0), and the ratchet will go green
while the stated intent — "diff proportional to the edit, not the file" — is only partially met.

**Recommendation:** pin C and name the exact fixture (element count, wire density, replica
interleave pattern) in the issue body before work starts, not left as a "recommended default."

### 3. (High) `blocked_by: 315` is contradicted by the issue's own evidence

The machine block states this task is `blocked_by: [315]` because it "rewrites every record kind
through the writer's save-tag table." But O3 in the same issue confirms via `git grep` that
`WireEnd.java:605-613` is the **only** reference-emitting site in `src/` — no other record kind is
touched. I independently verified `src/jls/elem/SaveTags.java` maps element **type tags**
(`"AndGate" -> AndGate.class`, per its own class doc: *"A `.jls` save names each element's type
with a tag token"*) — a completely different namespace from the `sref`/`sprobe` *item kinds* this
task adds, which are parsed by an `if/else` chain in `Circuit.load` (confirmed at
`Circuit.java:1107-1140`) and are unrelated to `SaveTags`. The stated blocking rationale does not
hold against the codebase. A later comment on the thread reaches the same conclusion and proposes
striking the edge — I confirm that correction independently.

**Recommendation:** strike `blocked_by: 315` from the machine block in the body; keep #315 only
under `related`.

### 4. (Medium) Stale forward references left uncorrected in the body

The body states three times that TASK-0006 "does not exist yet" (machine-block note, § Related
Work, the closing mermaid diagram). A later comment records it as filed at #437. The body itself
is unedited. An executor working from the body alone — which the issue's own § Threats to Validity
tells them to do ("re-derive before trusting") — will carry a wrong number into any new cross-link
they file.

**Recommendation:** edit the body directly (all three occurrences plus the mermaid node label) to
`#437` rather than leaving the correction stranded in a comment.

### 5. (Medium) The golden-regeneration "no stray changed line" guarantee has no automated check

§ Compatibility states *"A regenerated golden that changes more than its reference and `id` lines
is a bug in this task,"* and Completion Criteria requires exceptions to be "called out in the PR" —
but the issue's own §11 admits *"the review criterion is the only guard, and it is a human one."*
Given the issue's own §9 lists five-plus byte-pinned golden suites plus HDL goldens plus a docs
worked example that all get regenerated in the same commit, this is precisely the shape of change
a reviewer skims rather than verifies line-by-line.

**Recommendation:** have `DiffStabilityRatchetTest` (or a sibling) assert programmatically that
every changed line in the regeneration diff matches a `ref|sref|probe|sprobe|id` pattern, rather
than relying on PR review discipline as the only enforcement.

### 6. (Medium) Unstated interaction between "stop emitting `id`" and `HdlExporter`'s `getID()` dependence

The DoD requires `src/jls/hdl/HdlExporter.java` to stay untouched, citing its several `getID()`
call sites. If Open Question 2 is resolved by dropping the *saved* `int id N` line for `sref`-form
blocks (finding #1's likely fix), the issue never states whether `Circuit.save`'s dense-id
assignment loop (`el.setID(id)` at `Circuit.java:1501`, confirmed present) keeps running for every
element regardless of block form. If it stops, `HdlExporter`'s `getID()`-derived net names change
silently, violating the "HdlExporter untouched" DoD line in effect even though the file itself is
untouched. If it keeps running, that's a necessary design point the Interface & Data Contract
(§7.4/§7.6) never states.

**Recommendation:** add an explicit contract line: `Circuit.save`'s dense-id assignment continues
unconditionally for every element (so `getID()`/`HdlExporter` behavior is unchanged); only the
**emission** of the `id` line as a saved attribute is conditioned on the block's reference form.

## What holds up

- **The core defect is real and reproducible.** I independently confirmed O1 (`Element.java:22`,
  dense id "reassigned on every save"), O2 (`Circuit.java:1494-1503`, sort-then-assign), O3
  (`WireEnd.java:606,611`, the sole ref-emitting site), O6 (`Circuit.FORMAT_VERSION = 2` at
  `Circuit.java:102`), and O7/O8 (the loader's `ref` arm at `Circuit.java:1107-1116` parses a bare
  int; `sid` is a quoted string per `docs/file-format.md`) against the current checkout, not just
  the issue's citations.
- **Prerequisites are correctly scoped as closed decisions.** #165 (stable ids) and #166
  (canonical save order) are genuinely landed — `getStableId()`/`ElementId` exist and
  `Circuit.save`'s sort is keyed on it, confirmed at `Circuit.java:483,1496`.
- **The hostile-input framing is appropriately conservative.** Duplicate-sid refusal is already
  implemented (`Circuit.java:1300-1334`, confirmed), and the issue's warning not to route `sref`
  parsing through `ElementId.parse`'s counter-advancing path is well-founded — I confirmed the
  `AtomicLong.getAndUpdate` call lives outside `parse()` (`ElementId.java:178` vs. `parse` at
  `:245`), so the concern is real and precisely located, not hypothetical.
- **Falsifiability structure (H1-H3, P1-P6) is genuinely testable**, not decorative — each
  prediction names a concrete measurement and a concrete failure mode.

## Verdict rationale

`needs-rework`: the issue is well-researched and the underlying problem is real, but the Method
section as filed cannot satisfy its own Completion Criteria (finding #1), an explicit blocking
dependency is factually wrong against the current codebase (finding #3), and the acceptance
threshold is deferred rather than fixed (finding #2). These are not stylistic nits — an executor
following the body as written would do real work and then hit a wall the issue itself predicts but
does not resolve. The fixes are known (a review comment sketches them) but have not been merged
into the authoritative body text.
