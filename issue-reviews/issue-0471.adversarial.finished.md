# Issue #471: TASK-0028 (RESIDUAL): the msi and dmg get a gate over a property that can actually hold, and no reproducibility claim survives without one — the BOM guard and the independent rebuild already shipped
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary judgment

The concrete, code-level claims in this issue check out. I re-derived every cited
`ci.yml`/`release.yml` line range against the repository at HEAD (`5311625`) and they match
almost verbatim: the BOM guard and independent perturbed rebuild at `ci.yml` L812-846 are real
and already shipped; the msi's single-build, `continue-on-error`, existence-only lane at
`ci.yml` L1000-1032 is real; the "payload IS byte-identical" comment at `ci.yml` L965-975/985-989
is real; the `SIGNPATH_ENROLLED` gating at `release.yml` L331-332/684 is real; `docs/reproducibility.md`'s
claim table (L13-20) matches; no `test/` file references `ReproducibilityScopeTest` or
`reproducibility.md`; zero `timeout-minutes` anywhere in `.github/workflows/`. On the engineering
substance, this is one of the more carefully grounded issues in this style. But the issue leans
on a citation apparatus (a "corpus" of maintainer rulings and evidence documents) that does not
exist in this repository, and its verification mechanism (a markdown-table-parsing JUnit test) is
underspecified in exactly the way that lets a fake gate satisfy it.

## Findings, most severe first

### 1. [HIGH] The Method's first checklist item, and every "D10"/"D16"/"rule N" citation, point at files that do not exist anywhere in this repo's reachable history

The issue instructs, as literally the first bullet of §8 Method: *"Update
`docs/plan/tasks/TASK-0028-installer-reproducibility-rebuild-and-signing.md` to strike its
BOM-guard and independent-rebuild items, which shipped (section 1, D16)."* It also repeatedly
cites `docs/plan/evidence/BRIEF.md` §12/§14 for rulings "D10" and "D16" that are load-bearing for
its reasoning (e.g., "the absence of enrolment is not an argument against the capability — it is
a named cost with a named owner").

I confirmed `docs/plan/` does not exist anywhere in the current tree:
```
$ find . -path "*docs/plan*"
(no output)
```
Git history shows why: commit `742da745c6e5eac3da161ef6d4a1fee9ac2e38ee`, message *"docs: remove
the planning corpus now that it is encoded in issues"*, deleted the entire corpus — including
`BRIEF.md` and every `TASK-*`/`FEAT-*` doc — on 2026-08-03, the same day issue #471 was filed
(the issue was created at 17:52 UTC; the removal commit is timestamped 21:11 UTC). Worse, neither
the evidence commit this issue anchors to (`2d0ca9d`) nor the commit it says landed `BRIEF.md`
(`3a81a4a`) is an ancestor of the current mainline HEAD — both sit on a diverged branch
(`git merge-base --is-ancestor 2d0ca9d... HEAD` → `no`; same for `3a81a4a`). So an engineer
picking this issue up against the real default branch cannot open the file the checklist tells
them to edit, and cannot look up what "D10" or "D16" actually ruled — the normative basis for
several of the issue's judgment calls is unreachable.

**Recommendation:** Either restore/re-derive the specific rulings this issue depends on as inline
text in the issue body (so it's self-contained), or strike the corpus citations and the dead
checklist item before work starts. As written, whoever picks this up will stall on step one.

### 2. [MEDIUM] The scope-drift test's stated contract can be satisfied by a gate that gates nothing

§7.10 defines the drift predicate as: a row's claimed scope must have `gate(r) ∈ J`, where `J` is
just the **set of job names** in `ci.yml` — "the test reads job **names**, not step bodies, so a
job's internals may change freely" (§7.3, explicit). P5's acceptance is "deleting a gate job makes
it fail." Nothing in P3-P6 or §7.11 requires the *content* of `installer-payload-reproducibility`
or the dmg gate to actually perform the digest comparison correctly — a job named right that
always exits 0 (e.g., a job whose extraction step silently no-ops, or whose comparison is
`diff a a` instead of `diff first second`) satisfies `ReproducibilityScopeTest` in full while the
underlying claim ("payload-reproducible") remains false. The issue does anticipate part of this
in Threat T2 ("a gate that cannot fail... 7.11 requires a hard failure on an unextractable
payload... PR should include a deliberately-perturbed run") — but that safeguard is scoped to the
*payload-extraction* job, not to the *drift test itself*, and nothing in the Definition of Done
asks for a perturbed-input run of the drift test's job-name check independent of the payload job.
A reviewer who only checks "P5 passes, P6 passes, T2's perturbed run pasted" can still miss a
gate job whose internal comparison is a no-op that happens to be named correctly.

**Recommendation:** Add an explicit completion-criterion requiring the perturbed-run evidence
(T2) to be produced for *both* new gate jobs (msi and dmg), not just implied by the general threat
list, and require the PR to show the job's log actually contains two distinct digests being
compared (not just "job passed").

### 3. [MEDIUM] Citation/provenance fragility undermines the issue's own "re-derive at pickup" discipline

The issue is unusually disciplined about telling future readers to re-derive citations ("Citations
re-derived at `2d0ca9d`... citations re-derived if HEAD had moved" — Definition of Done, last
bullet). But its own anchor commit is not reachable from the branch anyone would actually be
working on, so "re-derive" isn't a small diff-refresh, it's redoing the archaeology from scratch.
I verified the *content* claims still hold on current mainline (the msi/BOM/SignPath code is
functionally identical modulo a Dependabot `setup-java` SHA bump), so this doesn't invalidate the
technical conclusions — but it's a structural defect in how the issue is meant to be consumed,
consistent with Finding 1.

### 4. [LOW-MEDIUM] H3's cited precedent is weaker than claimed

§4 H3 says the scope drift test is "in the family of the existing `CliFlagTableTest` drift tests."
I read `test/jls/CliFlagTableTest.java`: it diffs an **in-code** flag table (`JLSStart.FLAGS`)
against the CLI parser and the generated `usage()` text — a source-vs-source consistency check.
`ReproducibilityScopeTest` as specified instead parses a **prose markdown table**
(`docs/reproducibility.md`) with a hand-rolled parser against YAML job names — a fundamentally
more fragile document-vs-document check, which is exactly why the issue's own Threat T3 exists
("a drift test with a permissive parser... `theTableParsesAndHasRows()` exists for exactly
this"). Citing `CliFlagTableTest` as precedent slightly overstates how well-trodden this pattern
is; it is a related idiom, not the same risk class.

### 5. [LOW] Scope is very large for what ships

The actual deliverable is: two new CI jobs (one Windows, one macOS), one new JUnit test class,
and doc edits to two files. The issue spends 14 sections — hypotheses, falsifiable predictions,
data-transformation formulas in LaTeX for what are essentially two shell `diff` invocations,
concurrency-model analysis for single-threaded sequential CI steps — before reaching the actual
5-line Method checklist that matters. This isn't wrong, but it is disproportionate process
overhead relative to engineering effort, and it raises real review cost: a reviewer has to wade
through formal machinery to find the 6 file-level checklist items that constitute the change.

## What's solid (no further action needed)

- The core technical claim — that the msi lane is the only installer with zero reproducibility
  gate, and that the payload (vs. whole-file) is the correct thing to gate given jpackage's known
  limitation — is accurate and well-evidenced against the actual `ci.yml`.
- Recognizing #184/#185 as substantially shipped rather than re-litigating them is correct; the
  cited line ranges match the current BOM-guard and perturbed-rebuild code exactly.
- Treating SignPath enrolment (O4/#134) as a maintainer action rather than engineering work, with
  a `WAIVED:`-style fallback (P8), is the right call and avoids inflating scope with unblockable
  work.
- Explicitly forbidding re-enabling dmg HFS+ normalization and reintroducing a whole-file msi gate
  (§13 Out of scope, Threat T1) correctly encodes hard-won prior-art lessons from #190/#191 and
  guards against a predictable regression.
- `timeout-minutes` discipline (P7, dependency on #374) is properly scoped as "don't make it
  worse," not "fix it here" — avoids scope creep into #374's territory.

## Bottom line

The engineering plan is sound and the code-level observations are accurate. The verdict is
sound-with-concerns rather than sound because (a) the issue's checklist literally opens on an
edit to a file that has been deleted from the repository, with no fallback text for the rulings
it depends on, and (b) the machine-checkable acceptance criterion (`ReproducibilityScopeTest`) as
specified can be satisfied by a gate job that exists and is named correctly without actually
verifying anything, which is precisely the failure mode (a claim outrunning its gate) the issue
exists to prevent.
