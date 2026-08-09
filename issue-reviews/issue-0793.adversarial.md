# Issue #793: TASK-C584-3: today's HTML migrates mechanically with a byte-auditable diff report, and the same source emits the static site target
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

#793 is `TASK-C584-3`, ordered after `TASK-C584-2` (#792), both children of `FEAT-C35-1` (#584). It asks for two things bundled into one task: (a) mechanically migrate the 83 existing help HTML pages (`resources/help/**`, 88 `Map.jhm` topic entries, 10 image assets) into a new plain-text source form with a committed conversion tool and a byte-auditable diff report against today's shipping tree, and (b) make the same `mvn` build also emit the static-site directory that `FEAT-C35-2` (#585) will later publish, with a test asserting topic-set parity between the two output targets.

## Findings, most severe first

### 1. [High] The declared ordering makes #792 unclosable without #793's own deliverable — a hidden circularity

#792 (`TASK-C584-2`, ordered *before* #793) requires: "one `mvn`-reachable goal produces the in-jar help tree that `resources/help` ships today, from the plain-text source... The generated tree passes today's `HelpTopicsTest` unchanged" (AC-2). `HelpTopicsTest` checks the *full, real* content set — every code-referenced topic id, every inline `href`/`img src` in all 83 pages, every palette element's help topic (`test/jls/HelpTopicsTest.java:36-50`). That test can only go green against a source tree containing the full migrated corpus.

But #793 — ordered *after* #792 — is the task that actually claims that corpus migration: "All existing help content is migrated to the source form by a mechanical conversion" (AC-1), and its outcome text literally calls itself "the migration." So as sequenced, #792 cannot honestly satisfy its own AC-2 ("`HelpTopicsTest` unchanged and green," title of #792) because the full source content #793 is supposed to produce doesn't exist yet when #792 is done. Either #792 silently already has to do the full migration (in which case #793's AC-1 is a duplicate, not new work, and #793's actual scope shrinks to just the diff report + static-site target), or #792 is only checkable against a toy/partial fixture and its stated acceptance criterion is unverifiable at #792's own completion. Recommend: make the ownership of "full corpus migration" explicit in one task only, and have the other task's ACs reference a partial/fixture-scale validation instead of unqualified `HelpTopicsTest` parity.

### 2. [Medium-High] AC-4 does not test the property the outcome text promises — gameable

Outcome text: "The same source tree also emits the static site directory... so the two outputs cannot diverge in content." The actual AC: "a test asserts every topic present in one is present in the other" (AC-4). That is topic-**id** set equality, not content equality. A generator whose static-site renderer and in-jar renderer diverge in body text, link targets, or image paths (e.g. one strips a nav fragment the other keeps, or one relativizes `img src` differently) would still pass AC-4 as long as both sides list the same topic ids in their manifests. This is exactly the kind of acceptance criterion that can be satisfied while the stated goal ("cannot diverge in content") silently fails. Recommend strengthening AC-4 to a per-topic rendered-content diff (after normalizing site-only chrome), not a manifest/id-set comparison.

### 3. [Medium] AC-2's "unexplained difference fails the migration" has no defined mechanism

"every intentional difference is named in the report — an unexplained difference fails the migration" — there is no format specified for *how* a difference gets "named" (a checked-in exceptions manifest? a free-text comment in the diff tool's output? a human reviewer's say-so in the PR?). As written this is self-certifying: a contributor can label every diff hunk "intentional: reflowed markup" and the gate passes, because nothing constrains what counts as an acceptable explanation or checks it mechanically. Recommend a machine-readable exceptions file (page + hunk keyed) that the diff tool cross-checks, with any unmatched hunk causing a non-zero exit — not merely "named" in prose.

### 4. [Medium] Static-site directory shape is designed without its consumer's contract

AC-3 has this task emit "the static site directory," but the actual shape requirements for that directory belong to #585 (`FEAT-C35-2`): versioned path segments (`/5.0.x/`, `latest` alias — #585 AC-1) and "every hosted page's URL is derivable from its in-jar topic id" (#585 AC-3). #793 doesn't list #585 in `ordering_after` or reference its URL-derivation requirement at all. Nothing stops #793 from picking a directory layout that #585 later has to restructure once it actually specifies URL derivation — wasted work, or worse, a layout change that reopens #793's "byte-auditable" guarantees. #584's own boundary note ("the hosted publication surface is FEAT-C35-2") correctly keeps *publishing* out of this task, but doesn't save it from needing to match #585's eventual URL contract for the directory it does emit.

### 5. [Medium] Scope/cost: this task bundles substantially more work than its sibling at the same size band

`resources/help` has 83 HTML pages, 88 `Map.jhm` topic entries, 10 binary assets (`find /home/user/JLS/resources/help -name "*.html" | wc -l` → 83; `Map.jhm`/`JLSHelpTOC.xml` → 198 lines). #793 carries the same `band_mw: 1-1.5` as #792, yet must additionally: run/validate the full mechanical migration, build and maintain a diff-audit tool with per-page exception tracking, wire a second build target into the same `mvn` goal, and add a cross-target parity test. That is a materially larger scope than #792's "build one generator, validate against the existing test," at the same declared size. Recommend re-banding, or trimming the static-site emission (AC-3) out to a follow-up task, consistent with #584's own note that FEAT-C35-2 is a separate feature.

### 6. [Low-Medium] Binary/image assets are unaddressed

`resources/help` ships 10 `.gif`/`.jpg` files (`keypad.jpg`, `down.gif`, `up.gif`, etc.) referenced by `img src` that `HelpTopicsTest` resolves case-sensitively (`test/jls/HelpTopicsTest.java:44-50`). #793's AC-1 ("help content is migrated") and AC-2 (diff report) are phrased entirely in terms of HTML pages; it's unstated whether the migration/diff tooling treats images as pass-through-unchanged (likely intent) or is expected to touch/re-encode them for the static-site target, and whether a path change would be caught by the diff report at all. Worth one explicit sentence in the AC.

### 7. [Low] Note only — ARCHITECTURE.md's recorded decision is correctly left alone here

ARCHITECTURE.md's "Help delivery: in-jar now, hosted docs are the planned future" decision is explicitly closed by #585, not #793 — that boundary is respected and doesn't need action in this issue.

## What's solid

- Scoping the *publish* step out to #585 while keeping only *emission* here (AC-3) matches #584's boundary note cleanly — one line, no issue.
- "Conversion tooling committed rather than run once and discarded" (AC-1) is good practice: avoids a throwaway one-off script and keeps re-migration possible if upstream content changes again before cutover.
- Depending on `TASK-C584-2` at all (rather than nothing) is directionally correct — a generator has to exist before a migration can be mechanically driven through it; the problem is the *content* timing, not the dependency's existence (see Finding 1).

## Recommendation

Do not start implementation against this issue as written. Resolve the #792/#793 sequencing contradiction first (Finding 1) — it determines who actually owns the full-corpus migration — then tighten AC-2 and AC-4 into mechanically checkable properties before treating either as a real gate.
