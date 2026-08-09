# Issue #408: TASK-0004: a fixture carrying an attribute no element declares fails the build instead of loading in silence
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is an unusually well-grounded issue — nearly every `file:line` citation
I re-derived against the current checkout (`src/jls/Circuit.java:1067,1078,
1089,1105,1116`; `:89` for `lineNumber`; `Element.java:344`; `WireEnd.java:
626`) matches exactly, and the `docs/file-format.md` quotes are verbatim.
That precision makes the defects below more concerning, not less: they are
concrete, checkable claims that turn out to be wrong or incomplete, not
vague risk. Ranked by severity.

## 1. [HIGH] The fixtures as specified are git-ignored — `.gitignore` does not exempt `test/resources/`, only `test/fixtures/`

The issue's own Background section (§1) argues the corpus is safe to commit
by citing `.gitignore` L8-L10: *"exempts `test/fixtures/**/*.jls` from the
repo-wide `*.jls` ignore (#56, closed)."* But every fixture path in §8's
method list and §7.6's manifest example lives under
`test/resources/silent-loss/` — a **different** directory than the one the
cited exemption covers.

Verified directly against the checkout:

```
$ cat .gitignore
...
*.jls
# committed test fixtures are exempt from the *.jls ignore (issue #56)
!test/fixtures/**/*.jls
...
$ touch test/resources/probe-ignore-test.jls
$ git check-ignore -v test/resources/probe-ignore-test.jls
.gitignore:8:*.jls	test/resources/probe-ignore-test.jls
$ git ls-files | grep -i '\.jls$'
riscv/gui/cpu.jls
test/fixtures/fork-4.6-shiftregister.jls
test/fixtures/headless-canary-gate.jls
test/fixtures/riscv-sum1to10.jls
```

No `.jls` file is tracked anywhere under `test/resources/` today. The issue
conflates two different gates: `.gitattributes`' `test/resources/** -text`
rule (§7.7, correctly cited) *does* cover the chosen directory, but
`.gitignore`'s exemption does not. As written, `git add
test/resources/silent-loss/unknown-int.jls` silently no-ops (or requires
`-f`, which the method checklist never mentions and which undermines P5 —
a fixture nobody remembered to `-f`-add never reaches the index, so "add a
fixture with no manifest row and observe the run fails naming it" can
never be exercised in CI on a forgotten file).

**Recommendation:** either move the corpus under `test/fixtures/silent-loss/`
(the directory the existing exemption already covers, and the one the
sibling task #404 gestures at as "the tracked fixture" precedent), or add
`!test/resources/silent-loss/**/*.jls` to `.gitignore` as an explicit line
item in §8's method checklist. Verify with `git check-ignore` before
closing, exactly as §7.7 already says to do for `.gitattributes` — the
issue tells the executor to "verify rather than assume" for one gate and
then fails to do so for the other.

## 2. [MEDIUM] O5's "carried-over line number" hazard does not exist for the code path the corpus actually exercises

O5 states: *"the line counter is `static`. Two circuits loaded in one JVM
without a reset therefore see carried-over numbers. This is a real hazard
for a `@ParameterizedTest` that loads every fixture in one run."* This
drives P6 ("run the whole corpus twice in one JVM; observe identical
asserted line numbers") and a standing instruction in §7.8: *"If the static
proves unstable, fix the static — do not weaken the assertion."*

Checked against the actual `load()` entry point:

```java
public boolean load(Scanner input) {
    // a fresh load must not report a previous load's failure (#58)
    JLSInfo.setLoadError(null);
    lineNumber = 1;
    boolean ok = readFormatHeader(input) && loadCircuit(input);
    ...
```

(`src/jls/Circuit.java`, inside `load`.) `lineNumber` is unconditionally
reset to `1` at the top of every `load()` call, before any parsing begins.
Sequential single-threaded loads in one JVM — which is exactly what a
default (non-parallel) JUnit `@ParameterizedTest` does — never see a
carried-over value from a prior, already-completed load. The only real
hazard from a `private static` counter is a data race under *concurrent*
execution, and that is already independently forbidden by §7.9's "must not
run its fixture cases in parallel." P6 as framed therefore pins a
non-issue: it will pass trivially on the current code and contributes no
real signal about the hazard the issue believes it guards against.

**Recommendation:** drop the "carried-over numbers" framing from O5, or
replace it with the real risk (a `@ParameterizedTest` accidentally opted
into parallel execution). Don't send a future contributor chasing "fix the
static" for a bug that isn't there.

## 3. [MEDIUM] Hard dependency on #404, whose diagnostic text and aggregation policy are still open questions

`blocked_by: [404]` is correct and explicit, but #404 itself (fetched and
read) has **unresolved** Open Questions — the aggregation cap `C` is
"rides along," and whether the warning list lives on `Circuit` or a
returned load result is also "rides along." The manifest (§7.6) asserts
substrings rather than exact text specifically to survive this, which is a
reasonable mitigation the issue itself calls out, but it doesn't remove the
coupling: the `count` field in `expected.txt` is an exact integer, and
#404's aggregation-cap decision (still undecided) directly determines what
count a 1,000-attribute-style fixture — or even the multi-attribute
`dirty` case #404's own O3 constructs — would report. If #404 lands with a
per-element or per-file aggregation shape different from what TASK-0004's
author guessed while drafting manifest rows, every count column needs a
second pass, which is real rework the "2 d = 0.4 wk" estimate in #314's
cost table doesn't obviously price in.

**Recommendation:** none needed beyond what the issue already does (Threats
to Validity §11 flags the text-drift risk) — but flag explicitly in
Completion Criteria that the manifest's `count` column, not just its
substrings, must be re-derived against #404's *landed* aggregation
behavior, not against a guess made before #404 merges.

## 4. [LOW] Manifest substrings are a self-graded acceptance criterion

Nothing in §7.6 or §8 sets a floor on how specific a manifest row's
substrings must be. The worked example (`AndGate`, `notAnAttribute`, `22`)
is appropriately tight, but the contract as written would equally accept a
manifest row whose "required substrings" are trivially satisfiable (e.g.
just the element tag, or a digit that appears elsewhere in the diagnostic
by coincidence), silently weakening exactly the regression protection the
corpus exists to provide. H2's negative control (§4, O6) guards the
*under-reporting / over-reporting* boundary well, but nothing in the
contract stops a *loose but technically-passing* manifest row on the
positive fixtures. This is a soft process gap, not a defect in the design.

**Recommendation:** add one Completion Criteria line requiring each
manifest row's substrings to include the attribute name, the element tag,
and the line number at minimum (already true for the worked example — just
not stated as a requirement of the row format in §7.6).

## 5. [LOW] `evidence_commit` unverifiable in this checkout, but corroborated independently

`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not exist in this
checkout's history (`git cat-file -e` fails; the tracked history here is
squashed to 273 commits with synthetic "Checkpoint: issue review snapshots"
merge points, apparently specific to this review harness). I could not
directly confirm "`src/` and `test/` are byte-identical between that
commit and HEAD" via `git diff --stat`. That said, every specific
`file:line` claim independently re-checks out against current HEAD (see
summary), so this reads as a tooling/checkout limitation rather than
evidence the issue's technical claims are stale. Flagging only so a future
executor re-derives the SHA rather than trusting this review's inability to
`git show` it.

## What's solid (one line each)

- O1/O3/O4 (the silent-drop mechanism and its five call sites) are exactly
  right against current `Circuit.java` and `Element.java`.
- O6 (WireEnd's hand-written override as the negative control) is verified
  correct, including the `ref`→`int`-overload routing that makes `attach`/
  `wire` share a code path with `tristate`.
- The scope boundary against #314/#319/#323 (reject-vs-report policy,
  format-epoch, foreign-format migration reports all explicitly
  out-of-scope) is clean and non-overlapping.
- `docs/file-format.md`'s `initrle`/`sync` historical citations (§8 method
  bullet, `sync` correctly noted as #199, closed) check out verbatim.
- Both manifest directions (fixture-with-no-row, row-with-no-fixture) are
  symmetric and explicitly required — a common one-sided gap this issue
  avoids.

## Bottom line

The technical grounding is excellent, which is exactly why finding #1 is
disqualifying as filed: the corpus this task exists to build cannot be
committed to the path the issue specifies without either a silent
`git add -f` workaround nobody is told to use, or a `.gitignore` change the
task checklist omits. That's a mechanical blocker, not a design
disagreement, and it is cheap to fix — but it must be fixed before someone
picks this up and discovers eight fixture files that never made it into a
commit.
