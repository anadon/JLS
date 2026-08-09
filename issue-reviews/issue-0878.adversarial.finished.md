# Issue #878: TASK-C232-1: the immutable, width-carrying signal value type exists in `jls.core` with its op set and its frozen field list — and nothing else in the tree changes yet
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is unusually well-evidenced against the code (every `git grep`/file-listing claim I
re-ran matched exactly), but it is not self-contained: two same-day follow-up comments silently
change its scope and its acceptance criteria in ways that contradict the issue body's own frozen
language, and its single most consequential number — the +32%/+16-byte justification for
freezing the field list — traces to a document that does not exist in this repository and that a
later comment explicitly forbids citing, yet the body's own checklist would accept exactly that
citation.

## Findings, most severe first

### 1. [Critical] The frozen-field-list completion criterion is satisfiable by citing an admittedly unrecoverable source

Body §5 requires: *"The class javadoc states the field list is frozen and states the measured
reason... Whichever number the executor can re-derive on master is the one that goes in the
javadoc, with its source named."* Completion Criteria repeats this as *"with its measured reason
and that measurement's source named."*

The only number in evidence — "+32% per op and +16 bytes per value" — comes from the direct
predecessor issue #475 (closed 2026-08-08, `state_reason: duplicate`, the task #878 explicitly
splits and replaces), which sourced it to `docs/plan/evidence/mvl-determination.md` section 3.2,
landed at commit `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`. I confirmed that commit does not
exist in this repository (`git cat-file -t 3a81a4a` → `fatal: Not a valid object name`), and
issue #493 independently documents it (and all of `docs/plan/**`, 195 files) as **"unrecoverable
by re-reading... never existed on `master`."**

A same-day comment on #878 (id 5227604595) acknowledges exactly this and imposes a hard rule:
*"no permalink to `3a81a4a` or to any `docs/plan/**` path may be added by this task... Re-measure,
do not cite."* That is the right rule — but it lives only in a comment, not in the body's
Completion Criteria checklist. An executor working from the checklist as written could satisfy
"source named" by naming the same unreachable document #475 already did, since the checklist text
never says the source must resolve on `master`.

**Recommendation:** fold "re-measure on the shipped type; no citation into `docs/plan/**` or to
`3a81a4a`" into the Completion Criteria checklist itself, as its own checked box, before work
starts. A requirement that exists only in a comment thread is not part of the gate an implementer
or reviewer will mechanically check.

### 2. [High] A same-day comment adds a second scope-boundary exception that contradicts the body's own "one permitted exception" language

Body §5 states the radix-accessor additions to `Put`/`WireNet` are *"the one permitted exception
to P8's diff boundary and must be called out in the PR as such"* — P8 itself requires `git diff
--stat` to touch **only** `src/jls/core`, `test/jls/core`, and `pom.xml`.

A second comment (id 5227612399, "Scope amendment — the N-ary interval reservation lands here")
adds: *"`Put` and `WireNet` gain `lo()` and `hi()`, both returning `0` and `1`... Nothing consumes
them yet."* This is a **second** exception to a diff boundary the body explicitly caps at one, and
it is not reflected anywhere in the visible Completion Criteria checklist or in P8's own text. An
executor who follows the checklist literally will either (a) add `lo()`/`hi()` and fail P8 as
written ("A diff that reaches `src/jls/elem`... fails review" — note `WireNet`/`Put` live in
`src/jls/elem`, so *both* exceptions already violate P8's literal path list, radix accessors
included), or (b) skip the amendment because the body's own frozen text says only one exception is
permitted. Either reading is defensible from the text as it stands, which is exactly the kind of
ambiguity that produces a PR the author and a reviewer disagree about in good faith.

**Recommendation:** rewrite P8 and §5 together so the diff boundary and its exception list are
one authoritative statement, incorporating the `lo()`/`hi()` addition explicitly, before filing.

### 3. [High] The parent feature (#232) and the sibling issues (#879, #881) tell a story #232 itself does not contain

#878's abstract asserts it is *"the first of two children #232 needs and has never had"* and that
#232 is `tier:feature` with *"three `planned_tasks` and zero filed children."* I verified the zero
filed children claim (`get_sub_issues` on #232 returns `[]`), but #232's own three `planned_tasks`
are **baseline allocation/GC profile**, **net-width histogram**, and a single **"representation
swap"** item described as `(long value, int width; long[] fallback above 64 bits)` — a packed
scalar representation with no mention of planes, four-state, or a sealed interface anywhere in
#232's body. There is no "type first / plumbing second" split recorded in #232 at all; that split
was negotiated entirely in #878's own comment thread, which describes three different review
passes independently filing near-duplicate issues (#878, #879, #881) and reconciling them
after the fact ("neither could see the other's filing... #881 closes into #878"). None of that
reconciliation is reflected back into #232, whose `blocks`/`planned_tasks` still show the original,
un-split three-task plan with everything "not filed." Anyone reading #232 in isolation has no way
to discover #878/#879 exist or that the plan changed.

Compounding this: #878's own YAML `blocks:` list contains only a placeholder
(`999999  # TASK-C232-2`), while the comment thread asserts the *real*, "more complete" edge set is
`blocks: [322, 344, 391, 422]` — introducing issue #391, which appears nowhere in the body's
`related:` list and is given no description at all. A reader of the machine-readable block alone
gets a materially incomplete dependency graph.

**Recommendation:** before work starts, update #232's body to reflect the actual current plan (the
type/plumbing split, and that #475/#881 are superseded), and move the corrected `blocks` edge set
into #878's own YAML block rather than leaving it to be reconstructed from prose comments.

### 4. [Medium] The frozen four-state plane shape is scoped to serve #322, not to #232, and #232 never asked for it

#232 (the feature #878 claims to be `part_of_feature`) is framed purely as a GC/allocation
performance investigation; its own "representation swap" task text specifies a packed
`long`/`long[]` value, not a plane-encoded value. The two-plane `FourState` (aval/bval, IEEE 1364)
half of #878's sealed interface exists to serve #322 ("four-state semantics consume this type;
they do not own it") — a feature #878 explicitly disclaims ownership of. Freezing that shape as
part of a task billed to #232, on the strength of a now-admittedly-unrecoverable performance
figure (finding #1), commits #232 irreversibly to a design decision driven by a feature it isn't
chartered to serve. If a fresh, on-tree measurement (which the comment thread now requires)
contradicts the +32%/+16-byte figure, the field list "freeze" language in the body gives no
process for un-freezing it other than the general re-planning machinery on #232 — which, per
finding #3, doesn't currently know this task exists.

### 5. [Medium] P2's width-96 generator case is underspecified against the stated `toLong` failure mode

Body §5 states `toLong` on a width > 64 must throw, naming the reason. Prediction P2 says the
property-based generator "compares the op set against `BitSet` on random operands at widths **1,
8, 32, 64, 96**" — plainly including width 96 in "the op set." Taken literally, this asks the same
generator to assert both "matches `BitSet`" and "throws" for `toLong` at width 96, which is
unsatisfiable as written. The issue never states which ops are exempted from the 96-wide case
(presumably the bitwise/slice/concat/extend ops only, with `toLong`/`toBigInteger` handled
separately) — an implementer has to infer the exemption rather than being told it.

### 6. [Low] Predecessor's still-open decisions are dropped, not carried forward or explicitly deferred

#475 (the closed predecessor) flagged three genuinely open questions: `Adder`'s carry shape
(blocks widening `Adder`), whether `Binary` keeps `BitSet` internally or moves to `long[]`, and a
cost-reconciliation between a 2-week registry estimate and a 4.5-week planning-doc estimate. #878
carries forward only the naming decision (§6) from that set. The Adder question is presumably now
TASK-C232-2's problem since #878 touches no call sites outside `jls.core`, but the issue never
says so explicitly — a reader has to assume rather than be told which of #475's open items were
deliberately punted downstream versus simply lost in the three-way #878/#879/#881 split.

## What is solid (verified, no rework needed)

- **O1** (empty `jls.core`: exactly `Bounds`, `Geometry`, `GridPoint`, `GridSize`, `Orientation`,
  `SegmentGeometry`, `TextMetrics`, `package-info`) — verified exactly against the working tree.
- **O2** (61 defensive `clone()` sites: 59 in `src/jls/elem` + 2 in `src/jls/sim`) — verified
  exactly via `git grep -o "clone()"`.
- **O3** (`SimEvent.equals`/`hashCode` structural/mixing behavior) — verified against
  `src/jls/sim/SimEvent.java` lines 162–192.
- **O5** (`jls.core` has no JaCoCo package rule) — verified against `pom.xml`'s JaCoCo config,
  which floors `jls`, `jls.sim`, `jls.elem`, `jls.collab.op` only.
- **§6's naming conflict** between `docs/capability-roadmap/keystone-b-migration.md`
  (`LogicVector`/`.coerceUndrivenToZero()`) and `keystone-c-performance.md`
  (`LogicValue`/`.zeroFill()`) is real — both documents exist on this tree and use the two names
  exactly as the issue describes. Correctly identified and correctly flagged for resolution.
- The `evidence_commit` hash itself, `828822672fc3a8e2cb6da25192472079f04c29dd`, is a real commit
  present in the repository (confirmed via `git cat-file -e` and `git rev-parse 8288226`), unlike
  the `2d0ca9d` hash #493 warns the whole `#295-#492` sweep against — #878 correctly avoided that
  trap.
- P8's mechanically-checkable diff-boundary gate is, apart from finding #2's contradiction, a good
  pattern for keeping a "type only" task from eating its plumbing-widening successor.

## Verdict rationale

The evidence discipline is genuinely strong — every structural claim about the code checked out.
What pulls this to **needs-rework** is that the issue's authoritative surface (the body) and its
actual current scope/rules (two comments filed the same day) disagree with each other in ways an
executor cannot resolve without reading and cross-referencing a fast-moving comment thread that
itself references three other issues (#879, #881, #887) not otherwise linked from the body. The
single most load-bearing number in the whole task (the field-list freeze justification) is, by the
issue's own thread, sourced from a document proven not to exist — and the body's checklist doesn't
yet say so. None of this is unfixable; it needs the body edited to absorb the comment-thread
corrections before anyone picks it up.
