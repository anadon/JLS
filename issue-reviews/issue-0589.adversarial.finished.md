# Issue #589: FEAT-C36-2: the grading-contract white paper exists as one instructor-facing document — batch interface stability, determinism guarantees and provenance, in a form a course committee reads and CAP-21's kits link
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-written and has real discipline (explicit "do not invent guarantees" boundary, a thoughtful dedup note against #591). But it asks for a document whose subject matter is largely unbuilt, names an acceptance criterion (AC-4) that depends on artifacts nobody has scheduled, and leaves its central integrity mechanism (AC-3) unenforced by anything the repository would actually run.

## Findings, most severe first

**1. AC-4 depends on kits that are neither built nor ordered as prerequisites.**
AC-4 requires "the Gradescope, GitHub Classroom, PrairieLearn and nbgrader kits under #502 reference this document." Per #502 (CAP-21), those kits are PF-2 through PF-5, all listed `planned_features` with **no filed issue** — the CAP-21 background section states plainly "no file in the tree mentions Gradescope, PrairieLearn, nbgrader or GitHub Classroom" (verified: `grep -rli "gradescope\|prairielearn\|nbgrader" .` returns nothing in this checkout). Yet #589's `ordering_after` block names only `#300` and `#524` — not `#502` or any of PF-2..PF-5. AC-4 is therefore unenforceable at any point this issue could plausibly be closed under its own stated ordering. Either the ordering block is incomplete, or AC-4 should be split off / marked deferred until the kits exist.
*Recommendation:* add the kit PFs (once filed) to `ordering_after`, or rescope AC-4 to "kits link it as they ship" with an explicit note that AC-4 cannot close before #502's PF-2..PF-5 land.

**2. AC-3's enforcement discipline has no enforcement of its own.**
AC-3 says every guarantee must point at "the conformance suite, ratchet or test that would fail if it stopped being true." Nothing in the acceptance criteria names a check that keeps those *citations* honest over time — contrast with this repo's actual habit of pinning doc-to-code correspondence with a dedicated test (`ExtensionPointCatalogTest` cross-checks `docs/extension-points.md` against code in both directions; `HelpTopicsTest` is a link+completeness checker; `FileFormatSpecTest`/`SaveTagsTest` pin `docs/file-format.md`). Without an analogous test, AC-3 is gameable two ways: (a) a citation can point to a test that only loosely covers the claim, and nothing catches the mismatch at review time or later; (b) a cited test can be renamed or deleted in a later PR and the white paper silently goes stale, exactly the "trust, not verify" failure AC-3 claims to prevent.
*Recommendation:* name a concrete mechanism — e.g. a test that greps the white paper for `test:`/`ratchet:` citations and asserts each named class/method still exists and still passes — before counting AC-3 as met.

**3. AC-2(b)'s cross-axis determinism claim isn't something the codebase currently backs, and conflicts with the issue's own boundary note.**
AC-2(b) demands stating "what is byte-identical, across what axes (rerun, machine, JDK, platform)." I checked both places determinism is currently documented:
- `docs/batch-interface.md:215-216` claims only same-machine, same-run determinism ("two identical runs produce identical bytes... the golden tests compare byte-for-byte"). No JDK- or platform-crossing claim appears anywhere in that file.
- `docs/reproducibility.md` covers a *different* determinism axis entirely — build/artifact reproducibility (jar, BOM, some installers) — and explicitly carves out non-reproducible installers (msi, dmg) and the container image. It says nothing about simulation/batch *output* determinism.

So the paper is asked to make a claim (cross-JDK, cross-platform batch-output determinism) that no existing test or doc currently supports. That directly collides with the issue's own "Boundary notes" clause: "New guarantees are not invented here — if the paper wants to promise something the code does not yet do, that is a separate feature, not a paragraph." Either this axis needs a verification feature filed first, or AC-2(b) needs to be narrowed to what `docs/batch-interface.md` actually proves today (same-machine reruns), with cross-JDK/platform explicitly named as a limit under AC-5 instead of a guarantee under AC-2.
*Recommendation:* narrow AC-2(b) to the verified axis, or file the cross-JDK/platform verification as a prerequisite feature and add it to `ordering_after`.

**4. Two distinct "determinism" stories in-tree risk being conflated.**
Related to #3: `docs/reproducibility.md` (build reproducibility) and `docs/batch-interface.md` (simulation/output determinism) answer different questions with different exception lists. The issue doesn't tell the author which one — or both, clearly separated — AC-2(b) is about. A paper that blends them (e.g. citing the jar's byte-reproducibility as evidence for grading-run determinism) would technically satisfy a loose reading of AC-2 while misleading the actual audience (a course committee deciding whether *scores* are trustworthy, not whether *builds* are). This is exactly the kind of claim a skeptical instructor would catch and lose trust over.
*Recommendation:* AC-2 should explicitly require the paper to disambiguate build reproducibility from run/batch determinism as two named subsections, not one.

**5. The provenance section (AC-2c) documents a concept the codebase doesn't yet have.**
AC-2(c) asks for "how a score traces back to a circuit, a vector set and a build." There is currently no "score" artifact in JLS's batch mode — `docs/batch-interface.md` and the current CLI only emit watched-element value dumps and (per `JLSStart.java`) three exit statuses; the scoring/verdict layer is CAP-06 (#300), still open, and the "frozen CLI contract" that would give provenance a stable shape is FEAT-C21-1 (#524), also open (confirmed via `issue_read`: both `state: open`). #589 correctly lists both in `ordering_after`, which is good — but the practical effect is that AC-2(c) cannot be written truthfully until #300 and #524 both land, meaning this "1-2 mw" writing task is gated on two un-landed capstone-tier features whose own timelines are unestimated in this issue.
*Recommendation:* this is arguably fine as ordering discipline (it's already stated), but the issue should say explicitly that #589 cannot start meaningfully before #300 and #524 close, not just land in dependency-graph order — a "1-2 mw" band easily reads as "schedulable soon."

**6. AC-1's "stable URL" and "PDF handout" are underspecified.**
No GitHub Pages or docs-hosting pipeline exists in this repo (checked `.github/workflows/*.yml` for `pages`/`gh-pages`: none). "Published at a stable URL" most likely means a `main`-branch GitHub blob link, which is stable in *address* but not in *content* — unlike release artifacts (which get SHA256SUMS + attestation per README), the white paper has no versioning/pinning story, so a department that cites "JLS's grading contract" today has no guarantee the cited guarantees haven't loosened by the time a student's grade is contested a year later. Separately, "self-contained enough to be read as a PDF handout" doesn't say whether an actual rendered PDF must be produced and published (e.g., attached to releases) or whether this is purely a prose-style constraint — gameable either way, since nothing in the ACs checks for a PDF artifact.
*Recommendation:* specify whether a PDF is a real deliverable, and add a versioning convention (e.g., "guarantees are dated; changes get a dated changelog entry in the paper itself") so the document doesn't silently drift under a course that's already cited it.

**7. AC-4's "link check" only proves reachability, not fidelity to the stated intent.**
"a link check keeps that reference alive" can only assert the URL doesn't 404. The actual goal stated in the Outcome — "CAP-21's platform kits link it rather than restating it" — requires each kit's docs to *not* duplicate the contract in prose, which no automated check here catches. A kit author could add a working link and still paraphrase the guarantees inline (dual sources of truth, the exact problem AC-4 exists to prevent), and AC-4 would read as satisfied.
*Recommendation:* acceptable as a soft criterion, but call out explicitly that the "rather than restating it" clause is enforced by review discipline only, not by CI, so it doesn't get miscounted as automatically verified alongside the link check.

## What's solid

- The scope boundary against inventing new guarantees (Boundary notes ¶1) is exactly the right discipline for a claims document and is stated unambiguously.
- The #589/#591 deduplication comment is a genuinely useful piece of housekeeping — it correctly identifies the two issues as source-document vs. submission-act and explains why merging would lose information.
- AC-5 (limits stated as prominently as strengths) is a good, concrete anti-marketing-copy requirement, rare to see spelled out this explicitly in a feature issue.
- Ordering against #300 and #524 (rather than treating this as pure writing that can start immediately) shows the author is aware the subject matter is still being built — the gap is that the ordering isn't complete (see finding 1) and the band estimate doesn't reflect the wait (finding 5).
