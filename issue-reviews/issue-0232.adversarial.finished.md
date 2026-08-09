# Issue #232: Simulation hot path: per-signal java.util.BitSet allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The base technical claim is real and well cited: `BitSetUtils.Create`, `ToInt`/`ToLong`,
and `SumCarry` do allocate/scan as described, and every line anchor I checked against
`evidence_commit: 29afb26` (and current HEAD `5311625`, which is a descendant with an
identical `src/`/`test/`/`pom.xml` tree) resolved exactly. The problem is not the
engineering premise; it is that the issue's 11-comment history — all same author,
spanning 2026-07-23 to 2026-08-08, with the last three comments landing within 90
minutes of each other on the review date — has churned the plan through at least four
incompatible states, leaves the machine-readable body stale against all of them, and
rests its central "H1 not refuted" claim on evidence that is either self-contradicted
within the thread or unreachable from the default branch. This needs a cleanup pass
before anyone should pick up work against it as written.

## Findings, most severe first

### 1. [Critical] The thread reverses its own central verdict within 90 minutes, same author, same day, and the issue body reflects neither state

Comment at 17:58 (`#issuecomment-5227391450`) states flatly: *"the answer to H1 is NOT
REFUTED: value-container overhead is 37.6% of loop time... So outcome (b) of §1 is the
live branch, not outcome (a)... It is run. Cite the master path."* — i.e., the baseline
profile `planned_tasks` row is "DISCHARGED, do not file."

Comment at 18:25 (`#issuecomment-5227494578`), same author, same day: *"That discharge
does not hold, and rows 1 and 2 above must still be filed and executed"* — because the
`318 ns/event`, `3.14 M events/s`, `37.6%`, `~62%`, `1.92 M PinChanged allocs`, and
`+32%/+16-byte` figures are all cited to `docs/plan/evidence/BRIEF.md` at commit
`3a81a4a7…`, and `git cat-file -t 3a81a4a7…` is pasted failing with `fatal: could not
get object info`.

I independently reproduced the disputed lookup and got a different answer than the
comment's transcript: `git cat-file -t 3a81a4a7d6a0f108ec201e632732d308cc02b3fc` and
`git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` both resolve (`commit`) in
this repository, and `docs/plan/evidence/BRIEF.md` exists at that commit — but only on
`remotes/origin/claude/jls-virtual-hardware-linux-njsoma`, a stray exploratory branch,
**not** on master and not an ancestor of HEAD. So the comment's literal transcript is
plausible only if it was run against a shallow/master-only checkout; either way, the
underlying documents genuinely are unreachable from the branch anyone would actually
work from. Meanwhile `docs/capability-roadmap/keystone-c-performance.md` — a *different*
document that *is* on master — independently contains a `318 ns/event` / `37.6%` figure
for a `riscv/build/k2000.jls` workload (verified: `grep -n "318 ns\|37.6%" docs/…`
matches). Two overlapping-but-not-identical evidence sets, one real-and-on-master, one
real-but-orphaned-on-a-side-branch, are being cited interchangeably across the thread
as if they were the same measurement.

**Recommendation:** before anyone starts the swap, resolve which figures are the
citable ones, re-measure on master if needed, and edit the issue body's `planned_tasks`
and Open Questions to state the *current* claim in one place — not distributed across
11 comments that disagree with each other.

### 2. [High] The recommended baseline corpus is explicitly scheduled for deletion by the same evidence base that recommends it

Open Question 1 recommends anchoring the baseline-profile child to a `riscv/`-driven
fixture (`bench_kernel.py`/`jlsrun.py`). But the off-branch `BRIEF.md` that produced the
very numbers cited to justify skipping that profile states as its first "maintainer
directive": *"`riscv/` will be stripped entirely. It is remnant work... NOTHING may
depend on it, and no recommendation may route through `riscv/build_cpu.py` or
`riscv/jlsbuild.py` as a deliverable mechanism"* (`docs/plan/evidence/BRIEF.md` §0 D5,
verified present at `3a81a4a7…`). Comment `#issuecomment-5227391450` is aware of this
("this anchor lives under `riscv/`, which decision D5 schedules for deletion... a
profile re-taken after `riscv/` is deleted and before #413 lands has no fixture") but
does not resolve it — Open Question 1 stays open. Filing the baseline-profile child
against a fixture the project's own planning already marks for removal is a real
feasibility risk, not a hypothetical one.

**Recommendation:** name a corpus that is not simultaneously scheduled for deletion, or
sequence explicitly against #413's re-homing before filing the baseline child.

### 3. [High] Same-day child-issue thrash with an unfixed literal bug, none of it reflected back in #232's own body

Within roughly two hours on 2026-08-08: #475 (`tier:task`) was absorbed into #232 and
closed as duplicate; #878 (`TASK-C232-1`) was filed as a task child with its own YAML
`blocks:` list carrying a **literal placeholder** — `- 999999  # TASK-C232-2 …
replace with its number at the link pass` — that was never edited in the body, only
"corrected" by a later comment (issue bodies in this repo are stated elsewhere as
not-editable-after-filing, so the placeholder is permanent); and #881 was filed as a
*competing* attempt at the same "third roster row" and closed same day as a duplicate.
None of this is visible in #232's own machine block, which — as of this review — still
reads `planned_tasks:` with all three original rows and gives no indication that a
child now exists (`#878`) or that two other filing attempts (`#475`, `#881`) were tried
and closed. A contributor reading only the issue body, as the tier:feature template
instructs, would not discover #878 exists at all.

**Recommendation:** one edit to the body (or a single `REPLAN:` comment, per this
project's own convention) collapsing `planned_tasks` to reality: two rows discharged
(with the caveat from Finding 1), one row = `#878` (and its real successor, once it
exists — not `999999`).

### 4. [Medium] §5 criterion 3 (wide-bus check) is unsatisfiable as written, and the thread admits it without fixing it

*"Wide-bus integration check: circuits with >64-bit nets in the corpus produce
identical outputs through the `long[]` fallback (H3)."* Comment
`#issuecomment-5227391450` quotes the corpus's own risk table verbatim: *"no circuit in
the tree exceeds 32 bits"* — confirmed independently (`docs/capability-roadmap/
keystone-c-performance.md` §2/§9, and no `>64`-bit fixture found anywhere under
`riscv/` or `test/`). A criterion that requires "the corpus" to exercise a case the
corpus provably cannot exercise is not a criterion anyone can pass or fail; it will
either be quietly skipped at close-out or "satisfied" by a synthetic circuit nobody
agreed on in advance. This is exactly the kind of gameable acceptance criterion the
issue should not still contain after four rounds of self-review.

**Recommendation:** restate criterion 3 as a generated/synthetic-input property (a
purpose-built >64-bit test fixture or a property-based generator) rather than "the
corpus," before it's treated as gate-worthy.

### 5. [Medium] H1's falsification threshold was never pre-registered

§1 offers two outcomes: refutation (BitSet allocation "not a meaningful share") closes
the feature with no code shipped, or confirmation authorizes the swap. Nowhere does the
issue state a numeric bar for "meaningful" in advance. The same profile that's cited to
confirm H1 also shows event-queue machinery at 47.7% (bigger than the 37.6% value-type
share) and `PinChanged` payloads at 82.3% of all posted events — i.e., depending on
which line of the same report you read, "the biggest cost" is the queue, not the
value type, and the same author's document says so plainly ("If the question were
purely 'what is the cheapest way to make JLS faster', the answer would be the queue,
not the values"). Without a pre-registered number, whether 37.6% "refutes" or
"confirms" H1 is decided after the fact by whoever writes the closing comment.

**Recommendation:** state the H1 pass/fail threshold in the issue body before the
baseline profile is taken, not after.

### 6. [Medium] Scope has grown past what the title/abstract promise, into territory the issue itself once excluded

The title and abstract commit to "a value-typed (long,width) signal representation."
The only outstanding scope after the 2026-08-08 comments is the contract absorbed from
#475/carried into #878/#879, which specifies a **sealed interface** with a private
four-state `FourState` implementation (IEEE 1364 aval/bval plane pair), reserved
(unvalidated) radix-3/4 accessors consumed by #344, and reserved `getRadix()`/
`getDigits()` accessors for #419 — a materially larger surface than "long + width,"
and one that walks right up to the boundary the issue's own §1 "Out of scope" section
tried to draw around four-state semantics (owned by #322). The carried-forward
contract's own words concede the tension: *"#322 consumes it... but does not own it."*
That's a fine design position, but it is not what a reader is told to expect from the
title, and it means the eventual PR's "blast radius" (61 clone sites, 23 files, three
`SimEvent` payload types, `TraceSample`) is far larger than "swap a value type" implies.

**Recommendation:** either retitle/re-abstract to name the four-state surface
explicitly, or split it back out so #232 really is scoped to a plain `(long, width)`
value as advertised.

### 7. [Low] Stale `blocks: []` in the machine block versus repeated comment claims

The body's YAML declares `blocks: []`, but at least three comments assert #232 "blocks"
#322's, #344's, and #422's close-out. The issue's own DoD requires the machine block to
"agree with reality at close," but nothing enforces that mid-flight, so a reader who
trusts only the YAML (as the template instructs) currently gets the dependency graph
wrong.

### 8. [Positive — solid] The core evidence citations are unusually well-anchored

Every file:line citation tied to `evidence_commit: 29afb26` that I checked against the
current tree held exactly: `BitSetUtils.Create` allocation at `BitSetUtils.java:40`
(loop `:35-49`), `Create(BigInteger)` allocation at `:66` (`:60-73`), `ToInt`/`ToLong`
at `:127-137`/`:158-168`, `SumCarry`'s allocation at `:198` (`:196-226`); `SimEvent`'s
`NewValue`/`MemoryWrite` records at `SimEvent.java:39`/`~65`, its hashCode mixing fix at
`:186-192`; `TraceSample.java`'s HiZ-marker javadoc; the PIT gate's `targetClasses` at
`pom.xml:780-786` and thresholds at `:812-813`; `RegisterFile.java` (23 `BitSet` sites,
`grep -c` confirms) and `FieldExtend.java` (5 sites); and `Adder.java:420`
(`carry.set(0, sum.get(bits))`) really does read one bit past the port width off a
`BitSet` returned by `SumCarry`, which is the exact hazard the carried-forward contract
flags as "decide before widening `Adder`." This issue's citation discipline is better
than most of its siblings in this review batch — the problems here are process
(thread self-contradiction, stale body, unreachable evidence branch), not fabricated
file references.

## Bottom line

The engineering thesis is sound and the low-level citations check out. But the issue
as currently written is not safe to hand to an implementer: its own comment thread
disagrees with itself about whether the falsification gate has passed, its
recommended corpus is marked for deletion by the same evidence that's supposed to
justify skipping the profile, one acceptance criterion cannot be satisfied by the named
corpus, and same-day child-issue filings (including a literal `999999` placeholder)
are not reflected in the parent body. None of this requires re-litigating the technical
approach — it requires a single consolidating edit before work starts.
