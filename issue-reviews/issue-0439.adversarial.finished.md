# Issue #439: TASK-0013: memory capacity is a byte budget with stated headroom, and initializing a memory stops allocating a second copy of it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What was checked

All in-tree code citations (O1–O10) were re-verified against current HEAD
(`5b05d67`, shallow clone, 269 commits deep): `src/jls/elem/Memory.java`
lines 1033 (`private interface WordStore`), 1072/1156 (`DenseWordStore`/
`SparseWordStore`), 1094–1097 (dense copy constructor), 1119–1124 (`put`
truncation), 1131–1138 (`addresses()`), 1221–1237 (`DENSE_CAPACITY_LIMIT`,
`newWordStore`), 1308–1309 (`mem = initMem.copy()`), 57–76 (capacity
validity string/method), 92–94 (`MAX_INIT_WORDS`) all match the issue's
quotes verbatim. `test/jls/elem/DialogValidationTest.java:46`
(`memoryCapacityRuleIsOneStringOnTwoSurfaces`) and
`test/jls/elem/MemoryInitEncodingTest.java:163`
(`rleMemorySimulatesLikeRawMemory`) exist as cited. None of the three new
`MemoryModelTest` methods the issue prescribes exist yet, so there is no
naming collision. The issue has zero comments.

## Findings, most severe first

**1. (Critical) The issue's central design target — `docs/machine-calibration.md` §5.2 — does not exist anywhere in this repository, past or present, and the whole headroom number hangs off it.**
`git log --all -- '**/machine-calibration.md'` returns nothing across the
full 269-commit shallow history, `git show origin/master:docs/machine-calibration.md`
fails with "path does not exist," and a repo-wide grep finds the string
only inside *other* review files in `issue-reviews/` (sibling reviewers hit
the same wall on #354, which cites the same document). The issue quotes it
verbatim with a commit-pinned permalink (O5: "**12 MiB is the practical
floor for a shell; 16 MiB is the number to design to**... This matches real
silicon: the KianV uLinux SoC..."), and that quote is the *only* source for
the "16 MiB 32-bit guest" target the whole headroom arithmetic in §7.10 and
Open Question 1 is built on (`B_max ≥ h · 34,078,720`). `docs/plan/evidence/BRIEF.md`
(cited for the 15.87 bytes/word save-text figure) is equally absent —
`docs/plan/` does not exist at all. The evidence commit `2d0ca9d` itself is
unresolvable (`git cat-file -e` fails; `git rev-parse --is-shallow-repository`
is true), so the claim that these files are "present at 2d0ca9d" cannot be
checked either. **Recommendation:** before any executor picks this up,
confirm on the actual default branch whether `docs/machine-calibration.md`
exists; if it doesn't, Open Question 1 has no citable basis at all and the
issue's central premise (a *stated* target sized against a *named* document)
collapses to "someone should pick a number." The DoD's own line — "Every
cited evidence document and permalink resolves on the default branch at
close" — is, as filed, already unsatisfiable.

**2. (High) `sharesBackingWith`, the mandated non-reflective structural witness, cannot be called from the test that is supposed to use it, because `WordStore` and the fields that hold it are `private` to `Memory`.**
`src/jls/elem/Memory.java:1033`: `private interface WordStore {`. `:982`
and `:987`: `private @Nullable WordStore mem;` / `private @Nullable WordStore initMem;`.
§7.5 proposes `boolean sharesBackingWith(WordStore other)` as
"package-private" on `WordStore`, and §11 explicitly rules out the
reflection the issue's own O3/O4 probe used ("that is legitimate for a
red-state observation but is **not** how the acceptance tests should be
written — they use the package-private witness and public element
construction"). But making the *method* package-private does nothing if
the *type* `WordStore` stays `private` inside `Memory` — a class in the
same package (`test/jls/elem/MemoryModelTest.java`) cannot name a private
member type of another class, so it cannot declare a variable to hold the
result of `sharesBackingWith`, nor obtain a `WordStore` to call it on (both
`mem` and `initMem` are themselves private fields with no accessor). The
plan as written needs at least two more visibility changes it never
mentions: widening `WordStore` itself to package-private, and adding some
package-private way to reach `mem`/`initMem` from `Memory` — or the whole
witness collapses back to the reflection §11 forbids. §7.4's "no public API
is added or changed" is technically true but papers over this gap.
**Recommendation:** the issue's checklist item for the witness should
either spell out the accompanying visibility widening on `WordStore` and a
package-private accessor on `Memory`, or specify a witness that lives
entirely on `Memory` (e.g. `boolean initSharesBackingWithRunning()`)
so no test file ever needs to name `WordStore`.

**3. (Medium) Internal contradiction: `blocked_by: []` says nothing gates execution, but three Open Questions are individually marked "Blocks execution."**
The YAML block states `blocked_by: []` with the comment "nothing must land
first," yet Open Questions 1–3 each say "Blocks execution" — and Open
Question 1 additionally requires cross-issue coordination ("mirror the
answer onto #354... Do not decide it twice"), which is a real dependency
the `blocked_by`/`blocks` graph never records. An executor following the
dependency graph literally would start immediately; an executor reading
the prose would stop at the first bullet of §8 and go open #354 first.
**Recommendation:** either drop "Blocks execution" language for
decisions the issue also says are "decidable by the executor," or record
the #354 coordination as a real (if soft) dependency instead of leaving it
as an inline aside.

**4. (Medium) Disproportionate process overhead for the actual code change.**
The fix is, by the issue's own description: replace one `int` constant and
its predicate with a `long` byte-budget predicate, add a system-property
read, add one log line, and add one new `WordStore` implementation behind
an existing four-method interface (O8) — plausibly a few hundred lines
including tests. The issue demands: two formal hypothesis sets (H1–H4),
nine falsifiable predictions with LaTeX cost models, ten falsification
criteria, a "threats to validity" section, and a 20-item Definition of Done
including things like "no document or comment shipped by this task
conflates the 15.87 bytes/word... with the 8.125 bytes/word..." — a
criterion that is not mechanically checkable and depends on a document
(finding 1) that may not exist. This is scope-creep in the *process*, not
the code: a reviewer or executor can spend more time satisfying the DoD's
prose criteria than writing the fix. **Recommendation:** trim the DoD to
what CI can actually gate (the new tests, `mvn verify`, `git diff` on
`MAX_INIT_WORDS`) and drop criteria that only a human reading comments can
judge, or accept that "done" here is inherently a judgment call and say so.

**5. (Low) The headroom recommendation is self-undermined by the `int`-capacity ceiling it also states.**
§7.10 recommends `B_max ≥ 2 × 34,078,720 ≈ 68,157,440` bytes so a 16 MiB
guest sits inside with 2x headroom, but Threats to Validity correctly notes
`capacity` is `int` (O9), capping word count at `2^31 - 1` regardless of
`B_max`. That's not a defect in the issue's logic — both facts are stated —
but it means the "declared headroom" is headroom in *bytes budgeted*, not
headroom in *what the field can ever hold*, and nothing in the issue checks
whether `8.125 × B_max`'s implied word count could itself overflow `int`
math inside the new predicate (`B_dense(n) = 8n + n/8`, computed in what
width?). §7.10's formula `n ≤ ⌊8·B_max/65⌋` is stated as a derived
constraint but never appears in the Method checklist (§8) as something a
test must assert; O3's own probe notes an `int`-overflow near this exact
boundary ("the multiplication overflowing `int` before the widening cast").
**Recommendation:** add an explicit acceptance check (or at least a
checklist line) that `B_dense(capacity)` is computed in `long` arithmetic
end to end, given the issue already caught the analogous overflow once in
its own probe code.

## What's solid

- The core diagnosis (O1–O4) — a word-count cliff blind to `bits`, and an
  eager `initMem.copy()` — is accurate against the current codebase, not
  just the pinned commit; I re-derived it independently by reading
  `Memory.java` directly.
- H4/P6 (the budget must never become a load-time refusal) is correctly
  scoped against the real tripwire test (`DialogValidationTest:46`), and
  the scope carve-outs (not touching `MAX_INIT_WORDS`, not touching
  `DenseWordStore.get`'s `BitSet.valueOf` allocation which #232 owns) are
  precise and cite real code, not aspirational code.
- O7's truncation contract and O6's ascending-address contract are real,
  correctly quoted, and correctly flagged as the easiest thing for a new
  `WordStore` implementation to get subtly wrong.

## Note

The evidence-commit note limits the "byte-identical between 2d0ca9d and
839fb3a" claim to `src/, test/, pom.xml, scripts/, resources/,
.github/workflows/, proofs/, examples/` — `docs/` is conspicuously excluded
from that list, which is consistent with `docs/machine-calibration.md`
never having existed in this line of history at all (finding 1).
