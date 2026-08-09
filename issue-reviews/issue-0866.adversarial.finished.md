# Issue #866: TASK-C590-1: the positioning paragraph appears word-for-word in the README and the site about page, every clause pointing at something a reader can go check
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #866 (open, no comments), its parent FEAT-C36-3 (#590, open, 1
comment) and #590's own citations: #545 (FEAT-C27-1, README shop window,
open), #588 (FEAT-C36-1, comparison notes, open), and #510 (the niche
survey #866's positioning language derives from). Read README.md and
ARCHITECTURE.md. Searched the checkout for a "site" (no `_config.yml`,
no `mkdocs.yml`, no Pages workflow under `.github/workflows/`, no
`docs/site*`), for `docs/comparisons/` (does not exist), for a white
paper (no file anywhere), and for existing README/Markdown link-checking
infrastructure (none — `HelpTopicsTest` link-checks only the in-jar help
tree under `resources/help/**`, not README or external URLs).

## Findings, most severe first

**1. The task's own `ordering_after` omits half of the parent's declared dependency, and the missing half is exactly what AC-3 requires.**
#866's front matter states `ordering_after: ["#545 (the README shop
window this lands on)"]` only. But the parent #590 declares two:
`"#545 FEAT-C27-1 — the README shop window..."` **and** `"#520 PF-1 —
the comparison notes are what the statement points at as proof"`. #866's
own AC-3 requires "Each clause of the paragraph links to or names
specific verifiable evidence (a comparison note, the white paper, a
published measurement)" — i.e. it needs #588's comparison notes and a
white paper that isn't tracked under any issue number this checkout can
find (referenced only obliquely as "PF-2" in #588's boundary notes: "not
the white paper (PF-2)"). #588 is itself blocked on #300, #512, and #560
(none complete). #866 declares none of this as an ordering dependency,
so a contributor following #866's front matter literally would start
work believing only #545 must land first, then discover mid-task that
the evidence AC-3 demands does not exist and has no issue to point to.
*Recommendation:* add #588 (and whatever issue eventually owns the white
paper) to `ordering_after`, or explicitly scope AC-3 down to only the
evidence that already exists today.

**2. AC-3's own escape hatch can gut the paragraph's entire point, and nothing prevents that reading from passing review (gameable acceptance criterion).**
Quote: "if a clause has nothing a reader can go verify, it is cut rather
than softened." Given finding 1 — comparison notes and white paper do
not exist yet — a strict-but-lazy implementation of AC-3 is to *delete*
every comparison-derived claim (the "maintained successor," the
Digital-decline framing, the testing/grading superiority claim from
#510 §5) because none of it currently links to a committed artifact,
leaving a paragraph that says something anodyne and unfalsifiable-free
but also communicates nothing of what #590's Outcome section wants (a
stranger being told "what JLS claims to be... with links to the
comparison notes and white paper as its evidence"). Every AC in #866
(AC-1, AC-2, AC-4, AC-5) is satisfiable by that emptied paragraph. The
issue never states a floor on what the paragraph must *contain*, only a
ceiling on what it must *not* claim without backing — so "technically
compliant and useless" is a legitimate reading of the text as written.
*Recommendation:* pin the required content (e.g. "must state the Digital
tradition/maintained-successor claim from #510 §5, or explain in the PR
why it was cut") so AC-3 can't be satisfied by silent deletion.

**3. AC-1 and AC-2 assume a "site about page" this repository has no way to build, deploy, or diff against.**
No static-site generator config, GitHub Pages workflow, `docs/site/`
directory, or any other web-presence artifact exists in this checkout.
ARCHITECTURE.md is explicit that this is not an oversight: "Help
delivery: in-jar now, hosted docs are the planned future direction...
when it happens, the in-app viewer shrinks to context-sensitive basics
pointing at the site" (recorded 2026-07) — i.e. the site is *planned*,
not built. AC-2 requires "a committed drift check [that] fails when the
two copies diverge," which presupposes the site's about-page source is
either vendored into this repo or fetched over the network during CI.
Neither is described, and network-dependent CI checks are absent
elsewhere in this project's testing philosophy (headless, hermetic:
`HeadlessCoreRatchetTest`, `UntrustedFileHardeningTest`, the batch suite
running fully offline). *Recommendation:* either scope #866 to wait on
whatever issue stands up the site (currently none exists), or narrow
AC-1/AC-2 to "single-sourced within this repo, rendered into whatever
site build eventually consumes it" and drop the byte-identical-drift
requirement until a site build pipeline exists to enforce it against.

**4. AC-4 presupposes link-checking infrastructure that does not cover README/site prose today.**
Quote: "Every link in the paragraph is covered by the repository's link
checking" — phrased as if such coverage already exists. The only link
checker found, `HelpTopicsTest`, checks the in-jar help tree
(`resources/help/**`), a wholly different surface with different tooling
(HTML 3.2, `Map.jhm`, `JLSHelpTOC.xml`). No `markdown-link-check`,
`lychee`, or equivalent runs over README.md or any `docs/*.md` file
anywhere in `.github/workflows/`. Building README/site link-checking
from scratch is real, unscoped work this "0.25-0.5 mw" task doesn't
budget for. *Recommendation:* either state plainly that AC-4 includes
standing up new link-checking tooling for README/docs prose, or point at
an existing (even if different-surface) mechanism that can be extended
cheaply, and say which.

**5. The paragraph's likely content (per #510 §5) makes a claim about a live competitor's health with no freshness/recheck discipline, unlike its sibling issue.**
The positioning statement this task publishes is sourced from #510 §5:
"JLS is the maintained, modern successor in the Digital tradition...
welcoming the contributors and courses that Digital's decline is
stranding." "Digital's decline" is a claim about another actively
maintained project's trajectory (commit velocity, contributor
rejections) — the kind of claim that goes stale the moment Digital's
maintainer resumes activity or a fork appears (#510 §5 itself warns:
"the window is real but not permanent"). #866's sibling #588
(FEAT-C36-1) builds in an explicit staleness gate for exactly this kind
of claim (AC-4: "a documented recheck step exists, and the note states
plainly that a claim found fixed upstream is retracted rather than
defended"). #866 has no equivalent — AC-4 here only covers dead links,
not facts that were true when written and false by the time a reader
checks. *Recommendation:* either import #588's recheck discipline for
any competitor-health claim in the paragraph, or restrict the paragraph
to durable, structural facts (license, semantics rigor, grading
determinism) and drop trajectory language entirely.

**6. "Byte-identical" across a Markdown README and an HTML site page is under-specified.**
AC-1: "byte-identical, sourced from one file rather than duplicated by
hand." A Markdown link `[text](url)` and an HTML `<a href="url">text</a>`
are not byte-identical even when they render the same, so literal
byte-identity requires either the site to consume the same Markdown
source (unspecified renderer/pipeline — none exists per finding 3) or a
templating step that strips this down to plain prose common to both
(also unspecified). As written, an implementer cannot tell whether
"byte-identical" means the shared source file's bytes or the two
rendered outputs' bytes — those are different engineering problems.
*Recommendation:* state which representation must match byte-for-byte,
and name (or defer to #590/whatever future issue) the rendering pipeline
that makes that comparison meaningful.

## What's solid

- Single-sourcing one paragraph into both surfaces rather than hand-duplicating it is the right instinct and matches an existing repo convention (#545 AC-4 requires an analogous drift check for README image paths).
- AC-3's falsifiability principle ("adjectives standing in for evidence get cut") is a good general policy and is consistent with #510's own adversarially-verified, receipts-based tone.
- The task is cleanly bounded away from #867 (the announcement-checklist task) and #868 (the exercise-it-once task) — no functional overlap with either sibling.
- No code, security, or licensing surface is touched; failure modes here are documentation-quality and process risks, not runtime risk.
