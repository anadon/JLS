# Issue #866: TASK-C590-1: the positioning paragraph appears word-for-word in the README and the site about page, every clause pointing at something a reader can go check
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#510 §2 is blunt: JLS is category-best on exactly one axis (grading, 5/5), top-tier
on semantics rigor and code elegance, and invisible on all of it. A stranger who
lands on this repository today reads three screens about Authenticode publishers,
SHA256SUMS scope, and single-maintainer GPG custody rationale before learning that
JLS draws circuits. The identity paragraph is the cheapest possible correction:
one paragraph, days of work, no engineering. I endorse publishing it.

What I want re-cut is nearly all of the mechanism. Four of the five ACs enforce
*consistency between two copies of the text*; one enforces *truth of the text*.
That ratio is inverted. Two identical copies of an unbacked sentence are a
perfectly achieved nothing. The claim this issue should be defending is not
"README equals site" — it is "paragraph equals repository."

## Reframing 1 — cut the competitor out of the paragraph entirely

This is the one I care about, and it means disregarding AC-5's premise rather
than satisfying it.

AC-5 inherits FEAT-C36-1's citation standard so competitor claims in the
paragraph stay sourced and fresh. But look at what #510 §5 actually proposes to
put on the front door: "welcoming the contributors and courses that Digital's
decline is stranding." That clause has three defects no citation standard fixes.
Its truth is controlled by hneemann, not by us — one release from Digital and the
centerpiece of our README is stale. KC-36-1's retraction discipline then applies
to the project's front door, which is the one page you cannot gracefully retract
from; #588's notes are dated documents that can be pulled, a README is not.
And it aims an obituary at the exact audience #510 §5 wants to recruit — Digital's
rejected contributors, who are motivated by admiration for Digital, not by its
decline.

The elegant version states only what JLS can invalidate about itself:

> JLS is a maintained digital logic simulator for coursework: simulation with a
> written, normative semantics contract ([docs/simulation-semantics.md]),
> autograding as a documented stability interface rather than a scraped
> ([docs/batch-interface.md]), and reproducible, provenance-attested builds on
> every desktop platform ([docs/reproducibility.md]).

Every clause there is falsifiable against an artifact in this tree, today. None
of it needs a freshness recheck against anyone else's tracker. AC-5 becomes
vacuous because there is no competitor claim to source — and the comparison work
lives where it belongs, in #588's dated, retractable notes, which the paragraph
may *link* to without *asserting* on their behalf. "Successor in the Digital
tradition" is a good thing for a comparison note to argue at length; it is a bad
thing for a permanent one-paragraph identity statement to assert in passing.

## Reframing 2 — the drift check that matters is paragraph↔repository, not README↔site

AC-2 asks for a check that fails when two copies diverge. But byte-divergence
between two mechanically-generated copies is a near-impossible failure; the real
failure mode is the paragraph quietly becoming false as the project changes.

This project already has the right instinct everywhere else. `CliFlagTableTest`
(#71) refuses a hand-maintained flag list on either side. `HotkeysHelpAccuracyTest`
pins help *content* against the accelerators `EditOp` actually binds.
`HelpTopicsTest`, `TutorialContentTest`, `FileFormatSpecTest` do the same for
their prose. #587 exists specifically to generalize that shape into docu-tests.
The positioning paragraph should be the first *claim-bearing* prose that joins
that family — a claim ledger:

| clause | evidence anchor | what the test asserts |
|---|---|---|
| written semantics contract | `docs/simulation-semantics.md` + `SimulationSemanticsRegressionTest` | doc exists, named test class exists and is in the suite |
| autograding as documented interface | `docs/batch-interface.md` + `TellUserBatchContractTest`, `VcdExportGoldenTest` | same |
| reproducible builds | `docs/reproducibility.md` + `.github/workflows/repro-installers.yml` | doc and lane exist |
| maintained | the release-cadence/download file #582 tracks | file exists and its newest entry is within N months |

Cost is roughly AC-2's, and it buys something AC-2 does not: the day someone
deletes `docs/simulation-semantics.md` or lets the cadence lapse, the README's
boast fails a lane instead of becoming a lie. That is the same trade the whole
`docs/` tree already made.

## Reframing 3 — source it through #584, do not build a parallel include+diff

AC-1's "site about page" does not exist. There is no `site/`, no gh-pages branch,
no Pages workflow. The site is FEAT-C35-1 (#584: one plain-text source tree, two
targets) and its publication sibling; `ordering_after` names only #545. As
written, AC-1 is unsatisfiable, and the tempting fix — a bespoke snippet-include
plus a byte-compare script — builds a second single-source-many-targets mechanism
three months before #584 builds the real one, then makes #584 absorb or duplicate it.

Cut along #584's seam instead: the paragraph is a content asset in the doc source
tree, and every surface renders it. That immediately buys a surface the issue
never considered — the **in-jar Help "About JLS" page**, which is what an offline
lab machine shows the students who already have the tool in front of them. If the
claim is worth making to a stranger with a network, it is worth making to the
user who is holding the thing.

Byte-identity is also the wrong invariant across those media. A README, a web
page and a `JEditorPane` over `resources/help/**` have different link syntaxes and
different relative bases; forcing bytes to match forces absolute github.com URLs
into all three, which breaks offline rendering and pushes README readers off the
repo. Assert *sentence* identity with links resolved per target — which is exactly
what `HelpTopicsTest` already does for the in-jar tree ("resolves case-sensitively
the way a jar resolves"). Same discipline, correct invariant.

## Reframing 4 — this is one README edit, not two

#545 AC-1 rewrites the top of the README (screenshots, GIF, comparison table,
badge curation) and #545 AC-4 adds a README drift check. #866 rewrites the top of
the README and adds a README drift check. Two issues rewriting the same twenty
lines with two separate checks is how you get a conflicting merge and two
half-drift-checks. Better split: **#866 owns the source file, the wording, and the
claim ledger; #545 owns the README's shape and lands the include.** The paragraph
must sit above the install section — first prose after the title — because putting
"modern, maintained, easy" immediately *after* the supply-chain wall is the exact
juxtaposition #510 scored 2/5 on learning on-ramp.

## The cheap surface nobody in this chain has claimed

The highest-traffic positioning text JLS owns is not the README body and not a
site: it is the GitHub **repo description and topics** — the ~120 characters in
search results, link previews, and every "related projects" list — plus the social
preview image. Those live outside the tree, are covered by no drift check, and are
read by an order of magnitude more strangers than scroll the README. A one-line
rule ("the repo description is the paragraph's first sentence") plus an optional
API-reading check is a few hours and probably outweighs AC-2 in reach. Worth an
addendum here or a sibling task under #590.

## What I would keep verbatim

AC-3's "a clause without backing is removed rather than softened" is the best
sentence in the issue and should survive every reframing above — it is the same
instinct as KC-36-1 and as this repo's refusal to ship an unenforced doc. AC-4's
link coverage is right too, though note the repository has no link-checking lane
today (no lychee, no markdown-link-check in `.github/workflows/`); AC-4 quietly
assumes machinery that #587 has to build first.

## Verdict

**endorse-with-reframing.** Publish the paragraph — it is days of work against
JLS's worst-scored axis. But: drop the Digital-decline clause and with it AC-5's
whole premise (competitor claims belong in #588's retractable notes, not the front
door); replace AC-2's copy-equality check with a clause→artifact claim ledger in
the `CliFlagTableTest`/`HotkeysHelpAccuracyTest` family; render every surface from
#584's pipeline including in-jar Help rather than building a one-off include+diff
against a site that does not exist; and land the README edit inside #545 rather
than beside it.
