# Issue #457: TASK-0076: a byte-lane write mask on Memory, so a drawn core does a sub-word store in one cycle instead of a read, a merge and a write
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Findings, most severe first

### 1. [High] A load-bearing citation points at a document that does not exist anywhere in the repository

The "Intended Audience & Impact" section grounds the device-bus use case in
`docs/machine-calibration.md §5.3's three-byte-address UART`. I checked: no
`docs/machine-calibration.md` exists at HEAD (`5b05d67`), on `master`, or
anywhere reachable — `git grep -l machine-calibration` across the working
tree only turns up two *other* review files written by sibling reviewers
about *other* issues, i.e. this is a repo-wide pattern, not a fluke of this
issue. `Glob docs/*` lists 25 files and none is it. Unlike the rest of the
issue's citations (which are pinned to a branch-only commit but do resolve
once redirected to master, see finding 2), this one has no known surviving
equivalent anywhere I could find — it may only ever have existed on the
RV32 experimentation branch that produced #199. The UART example is not
decorative: it is one of two named audiences motivating the feature at all,
and it cannot currently be verified by anyone picking up this issue.

**Recommendation:** either replace the citation with a real, resolvable
example (or drop it and keep the CAP-02/CAP-08 sub-word-store motivation,
which stands on its own), or, if the file is meant to land alongside other
FEAT-036 work, say so explicitly rather than presenting it as already-true
evidence.

### 2. [High] The evidence commit does not exist in the checkout, and the issue body was never corrected after its own comment flagged this

`git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails —
`fatal: ambiguous argument ... unknown revision`. The issue's own follow-up
comment (posted the same day, 2026-08-03) admits this: "declares
`evidence_commit: 2d0ca9d...`, which exists only on a branch that will not
be merged and will be deleted. The surviving equivalent is `master` at
`828822672...`." I independently confirmed the redirect works for the
code evidence: `git diff --stat 8288226..HEAD -- src/ test/` is empty, and
O2/O6/O7/O9's quoted `Memory.java`/`SimEvent.java` snippets match master
byte-for-byte (only the line numbers drift slightly, e.g. O2's cited
`:182-197` is `:181-196` on master — immaterial). But every one of the
~15 GitHub permalinks in the issue body still points at the dead commit,
and the Definition of Done explicitly requires "Every cited evidence
document and permalink resolves on the default branch at close" — a
criterion that was already false at filing and is still false in the body
text five days later, even though the fix (repoint to `8288226...`) is a
mechanical find-and-replace the comment already did the hard part of.
Leaving it live invites exactly the "case (b)" mismatch the comment found
for `HdlExporter.REJECTED` (a symbol named in §7.1 that does not exist on
master — confirmed independently: `src/jls/hdl/HdlExporter.java` has no
`REJECTED` constant; `Memory` is caught by the generic "SubCircuit,
Memory, and anything unrecognized" fallback instead) to recur undetected
in some other citation nobody has re-checked yet.

**Recommendation:** edit the issue body to repoint every permalink at
`8288226...` (or a later commit) before an executor starts, rather than
relying on a comment a skimming reader can miss.

### 3. [Medium] P7 is a hard acceptance criterion with no defined test mechanism — gameable as written

P7 ("must hold after"): "With the mode off, observe no per-write allocation
of an all-ones mask — a shared constant or a `null`-mask sentinel handled
once." §9's Data Collection says it is "asserted structurally (no
allocation in the mode-off completion path), not by heap measurement" —
but no concrete API or assertion technique is named. Contrast with the
sibling task #439 (same file, same class of claim about avoiding a
doubled allocation), which specifies a concrete
package-visible `boolean sharesBackingWith(WordStore other)` witness
*precisely so* this kind of "no extra allocation" claim is machine-checkable
rather than asserted by inspection. TASK-0076 has no equivalent. As written,
"asserted structurally" can be satisfied by a reviewer eyeballing the diff
for a stray `new BitSet()` — not a test that fails on a regression, and not
reproducible by CI. This is exactly the "verification could pass while the
real goal fails" pattern: a future refactor could reintroduce a per-write
allocation and no test in this issue's plan would catch it.

**Recommendation:** either name a concrete mechanism (e.g., a package-visible
static accessor exposing the sentinel and a test asserting reference
identity across two unmasked-write completions) mirroring #439's witness
pattern, or downgrade P7 from a blocking prediction to a code-review note.

### 4. [Medium] P2 — the criterion the issue itself calls the single highest-risk failure — is specified as one test vector, not a sweep

§9 describes P2 as testing "the same stimulus" (one write, one mask, one
address) against a lanes-off memory and a lanes-on-all-ones memory, and
comparing `storedAddresses()` and every stored word for that one write.
But #364 §5 criterion 6, which this issue quotes approvingly, names exactly
this equivalence "the single most likely silent-wrong behaviour in the
feature." A single-vector test can pass while the merge formula in §7.10
is subtly wrong for an untested width: `bits=8` (ℓ=1, the boundary where
"unselected lanes" is vacuously the whole word or nothing), a wide word on
the sparse path (`bits > 64`, which H3's own falsification note calls out
as the case that actually exercises the truncation-ordering bug), or
whatever width the executor happens to pick for the one fixture. The issue
elsewhere (H3's falsification criterion) explicitly warns "it cannot [hold]
for `bits > 64` — re-run the check at a wide word before believing it," which
implicitly concedes a narrow-width-only test is insufficient, yet P2 as
specified is exactly that narrow test.

**Recommendation:** specify P2 as a `@ParameterizedTest` over at least
{8, 32, 65-plus (sparse path)} bit widths and a couple of mask patterns,
not a single stimulus, matching the rigor #439 applies to its own
equal-cost-pair sweep (`denseStorageIsChosenByBytesNotByWordCount`).

### 5. [Medium] Two "blocks execution" Open Questions are answered implicitly inside the Method checklist's literal code before being resolved in the checklist's own ordering

Open Question 1 (byte-multiple requirement) and Open Question 2 (sentinel:
`null` vs. shared constant) are both flagged "Blocks execution," and §8's
first actionable step is "Decide the partial-lane rule first... before
writing it into the docs" — good ordering in prose. But step 4 of the same
checklist already writes the answer as fact: `new Input("WM", this, 0,
7*s, ceil(bits/8))` bakes in "require the byte multiple" (the recommended
but not yet ratified answer to Open Question 1) directly into sample code,
with no `WAIVED:`/decision marker at that step. An executor working from
the checklist rather than re-reading the recommendation prose could
implement the assumed answer without registering that a decision was
supposed to be made and recorded (per completion-criteria item "Every
decision in Open Questions & Decisions Needed is resolved... none left
blocking").

**Recommendation:** either fold the two blocking decisions into the
checklist itself as an explicit first checkbox with its own acceptance
line, or annotate step 4's code with a comment flagging which open question
it assumes.

### 6. [Low-Medium] Verification overhead is large relative to the code change, and larger than the cited 1.5-week estimate is likely to cover

The actual change is small: one saved attribute, one input, one record
component, one merge helper, one diagnostic. The prescribed verification
is not: 5 hypotheses, 9 predictions, 5 falsification criteria each with a
prescribed "next move," two documentation files updated in lockstep
(`docs/simulation-semantics.md` §8.4 *and* `docs/file-format.md` §5 plus
its §9 caveat list), a new parameterized fixture in
`AllElementsRoundTripTest`, a sync×lanes 2×2 cross-test, and an 18-item
Definition of Done. #364 prices TASK-0076 at 1.5 weeks; honoring every
checkbox literally (especially the structural witness gap in finding 3
and the parameterization gap in finding 4, once fixed) plausibly costs
more. Not a reason to reject the issue, but worth flagging before an
executor commits to the stated estimate.

## What holds up

- The core technical diagnosis (O1–O11) is accurate: independently
  verified against `src/jls/elem/Memory.java` and `src/jls/sim/SimEvent.java`
  on master — no `lanes`/`WM` anywhere, `Memory.init` builds five inputs and
  none is a mask, `DenseWordStore.put` truncates to one `long`
  (`words[addr] = asLongs.length == 0 ? 0 : asLongs[0]`), and the write is
  posted from `PinChanged` with data cloned at post time, exactly as O6
  claims.
- The #199 precedent (attribute written only when on, absent-means-old-
  behavior, input appended last) is a real, previously-shipped pattern in
  this file (`syncWrite`/`sync`/`clock`), and this task's plan to copy it
  is the right call, not a cargo-culted analogy.
- H3's insistence that the merge happen *before* `put`, not after, is
  correct and necessary given the truncation in `put` — verified directly.
- O8's "blast radius is one site" claim is verified true: exactly one
  `case MemoryWrite(int addr, BitSet data) ->` deconstruction pattern
  exists; the other 15 hits are bare `MemoryWrite _` type patterns in
  throw arms, unaffected by a new record component.
- The null-mask-means-all-ones default is well-reasoned, explicitly named
  as the highest-risk silent-wrong behavior, and backed by a dedicated
  test (even though that test's coverage is itself under-specified — see
  finding 4).
- Compatibility scoping (no `FORMAT` bump, byte-identical re-save with the
  mode off, stable input indices) is consistent with how `sync` (#199)
  actually shipped in this codebase.

## Verdict rationale

The engineering plan itself is sound and clearly informed by real, verified
code. But two citation-integrity problems (a dead evidence commit whose
links were never repaired even after the issue's own comment found part of
the damage, and a motivating document that doesn't exist at all) and two
acceptance-criteria gaps (P7 has no test mechanism; P2, the criterion the
issue calls highest-risk, is specified too narrowly to catch the failure
mode it names) are real defects an executor would hit. None require
re-scoping the feature — they require tightening the issue before or during
execution. Hence sound-with-concerns rather than needs-rework.
