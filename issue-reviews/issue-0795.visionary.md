# Issue #795: TASK-C585-2: in-app help computes its "open in browser" URL from the topic id, and the hosted site gets the same link strictness plus working search
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four ACs away and the want underneath is one sentence: **a page of the JLS
manual should have an address a human can say out loud.** An instructor pastes it in a
syllabus; a student pastes it in a bug report; a TA says "read 5.0.7/elements/memory".
Everything else here — the derivation rule, the version pinning, the fallback reporting,
the link CI — exists to make that address *trustworthy* (it resolves, and it resolves to
the version in front of you) and to make it *not rot* (nobody hand-maintains it).

That goal is right and it strengthens the arc. CAP-35 (#519) correctly diagnoses JLS as
having strong documentation *discipline* and weak documentation *delivery*; #584 builds
the pipeline; #794 publishes it; this issue is the one that makes the published thing
reachable from inside the program. I endorse the goal. I do not endorse three of the four
acceptance criteria as framed, for reasons below.

## Reframe 1: derive from the page path, not the topic id (AC-1 is factually unbuildable as written)

AC-1 says every hosted page's URL is derivable **from its in-jar topic id**. It is not, and
the repository proves it: `resources/help/Map.jhm` has 84 `mapID` entries resolving to 83
distinct pages — `picking` and `create.elem` both map to
`editor/editing/picking.html`. Topic id → page is not injective. A rule keyed on topic id
either mints two hosted URLs for one page (canonicalization problem, split search index,
two things to link in a syllabus) or needs a hand-maintained exception, which is exactly
the hand-maintenance the outcome statement forbids.

The rule that already exists and is already the right one: **the hosted path *is* the in-jar
resource path.** `Map.jhm` topic → `elements/components/mux.html` → `<base>/<version>/elements/components/mux.html`.
Composition of a map JLS already loads with string concatenation. "Asserted over the whole
topic set" then degenerates to a shape test — which is *good*: a rule you cannot get wrong
does not need a large test to defend it. Rewrite AC-1 as "derivable from the topic's page,
by identity of relative path", and the aliasing problem disappears rather than being
handled.

Stronger version of the same move: don't let Java hold the rule at all. #584 emits both
targets from one generator; have that generator also emit the base URL and the path mapping
into the jar as a generated resource, alongside `version.properties`. Then the rule is not
"asserted to agree" — there is only one copy of it, and agreement is structural. That is
the same discipline `src-filtered/jls/version.properties` already uses to single-source the
version from `pom.xml`.

## Reframe 2: version-fallback policy belongs on the site, not in the frozen jar

AC-2 asks the running program to link to its own version, fall back to `latest` when its
version has no published site, and *report* the fallback. Read that as an architecture
statement and it says: a jar shipped in 2026 makes runtime decisions about the layout of a
website in 2031. That is the wrong side of the seam. The jar is immutable forever; the site
is editable any afternoon. **Policy belongs on the mutable side.**

Also, the jar cannot honestly evaluate the condition. "Has my version been published?" is a
network question, and #585's boundary note plus #584 AC-3 make network-freedom
non-negotiable. Probing would leak exactly where the boundary note warns it would leak.

Two substitutions that give more than AC-2 asks for, at less cost:

- **Publication status is a build-time fact, not a runtime discovery.** `pom.xml` is
  `5.0.5-SNAPSHOT` today; `JLSInfo.loadVersion()` already falls back to `"dev"`. A
  `-SNAPSHOT`/`dev` build is *by construction* unpublished — link it at `latest` and say so
  in the affordance's own label ("development build — opens the latest published manual").
  A release build's version is published by construction, because #794 AC-1 makes
  publishing part of cutting the release. No probe, no ambiguity, no silent anything.
- **The residual case — a release whose publish step failed — is handled by the site's
  `404.html`.** GitHub Pages routes unknown paths there; it can show "no manual is published
  for JLS 5.0.9; here is 5.0.9's page in the latest manual" with a real link. That reports
  the fallback *where the reader's eyes already are*, and it stays fixable after the jar has
  shipped. In-app reporting reports it to someone who has already left for the browser.

Add one small thing the issue never considers and that this design makes nearly free: a
`-Djls.help.site=<base>` override, mirroring the existing `-Djls.toolkit=` precedent. A
university that mirrors the manual on an intranet — the exact locked-down-lab population
this project keeps designing for — then gets working "open in browser" links with no code
change, and the project gets an escape hatch if it ever moves hosts and every jar in the
field is pointing at a dead domain.

## Reframe 3: the primitive is "copy this page's URL", not "open in browser"

The feature's own justification is *pasting*: syllabus, bug report. `Desktop.browse` does
not serve pasting, and it is unavailable or unreliable in precisely the environments this
project takes seriously — a lab image with no default browser, the WLToolkit path, a kiosked
account. Make the canonical URL **visible and copyable** in the help viewer (a footer line
showing `…/5.0.7/elements/components/mux.html`, plus a Copy button), and make "open in
browser" the *secondary* action that can degrade to "we showed you the URL; copy it" when
`Desktop.isDesktopSupported()` is false. Copy-first is strictly more useful, always works,
and is the affordance that literally matches the stated user story.

Mechanically: put the rule in a small pure class (`jls.HelpSite`: version + topic → URI),
not inside `Help`'s Swing listeners. `Help` is a static-singleton GUI class; the suite runs
headless, and AC-1's whole-topic-set assertion needs the rule reachable without a frame.

## Reframe 4 (the biggest one): search is a property of the content, not of the web target

AC-4 gives search to the hosted site. But ARCHITECTURE.md's recorded reason for wanting
hosted docs at all is "searchability, linkable pages, one source of truth" — and the student
who most needs search is the one mid-assignment on the offline lab machine, reading the
in-jar manual, which has *no* search and gets none from this issue.

83 pages of HTML 3.2 is a tiny corpus. A build-time inverted index over it is tens of
kilobytes. Build that index **once, in #584's generator, as a shared artifact**, ship it
both on the site (consumed by ~50 lines of vanilla JS — do not pull in Lunr/Pagefind for
this size) and *in the jar* (consumed by a search box in `jls.Help`). Then:

- the two searches are the same search, and cannot return different answers for the same
  term — which is the same drift-proofing instinct #587 applies to claims;
- the committed fixture set in AC-4 tests one index and covers both surfaces;
- offline search lands, which is a bigger real win for the actual user than hosted search;
- and one of the two stated motivations for hosting is satisfied *without* the network,
  which the recorded ARCHITECTURE decision should be updated to reflect.

I am explicitly proposing work outside this task's stated scope here. If it doesn't fit,
the fallback is to at least require that #584 emit the index as a named artifact so the
in-jar search is a later consumer rather than a later rewrite.

## Where AC-3 duplicates work the project already has

AC-3 wants hosted link-integrity CI "with the strictness the in-jar tree gets". If the site
is the structural mirror of Reframe 1, `HelpTopicsTest` (#70) *already* checks every
`href`/`img src` in that content, case-sensitively. Re-running a general link crawler over
the site re-proves what the unit test proved, on a slower loop. The links a mirror site adds
that the jar does not have are exactly the *generated chrome*: nav, breadcrumbs, the version
switcher, `latest` aliasing, and any absolute outbound links. Scope AC-3 to (a) a
mirror-invariant test — the set of emitted site pages equals the set of in-jar pages, path
for path — plus (b) chrome-link checking. That is a smaller, faster, more honest check that
fails for reasons a human can act on. If the site is *not* a mirror, say so out loud in the
issue, because then Reframe 1 and AC-1's derivability both need rethinking.

## An unowned obligation this task creates

#585 says it "closes ARCHITECTURE.md's orphaned 'hosted docs are the planned future'
decision". It closes it by choosing the **opposite** of what that decision anticipated: the
recorded text says "when it happens, the in-app viewer shrinks to context-sensitive basics
pointing at the site", while #584/#585 hold full offline parity as non-negotiable and this
task only *adds* a link to the viewer. That is the better call — but nobody owns editing the
decision. Neither #794 nor #795 has an AC for amending ARCHITECTURE.md's "Help delivery"
section, and the "portability discipline until then" paragraph becomes stale the moment
#584 lands. Add that edit to this task; a recorded decision that describes a plan the
project has abandoned is worse than no record.

## Summary of proposed changes to the ACs

1. AC-1: derive from the topic's **page path**, not the topic id (84 ids, 83 pages); emit
   base + mapping from #584's generator so agreement is structural, not asserted.
2. AC-2: replace runtime fallback-and-report with build-time publication status
   (`SNAPSHOT`/`dev` → `latest`, labelled) plus a site-side `404.html` that reports the
   fallback to the reader. Add `-Djls.help.site=` for mirrors.
3. AC-3: scope to a mirror-invariant test plus generated-chrome links; don't re-crawl what
   `HelpTopicsTest` already proves.
4. AC-4: build the search index as a shared artifact and give the in-jar viewer search too.
5. New: amend ARCHITECTURE.md's "Help delivery" decision, which this work contradicts.
6. New: surface the canonical URL as copyable text; browser launch is the secondary,
   degradable action.
