# Issue #585: FEAT-C35-2: the manual is on the web at a per-release URL a student can paste into an assignment, and in-app help offers the same page in a browser
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two different needs are fused into one URL scheme, and the fusion is the source of
every problem below.

1. **The syllabus URL** wants to be *stable across versions*. An instructor writes it
   once and the course runs for four years. It must never rot.
2. **The bug-report URL** wants to be *version-precise*. "I clicked this, on 5.0.1,
   and it said that."

`/5.0.x/` plus a `latest` alias serves (2) well and (1) badly: the instructor either
pins `/5.0.3/` (and by spring the students run 5.2 and read stale text) or uses
`latest` (and the link silently means something different each term). The third
consumer — the in-app "open in browser" button — is worse than either, because it
emits version-pinned URLs from binaries the project no longer controls.

The end this issue serves is real and the recorded decision it closes is genuinely
orphaned (ARCHITECTURE.md, "Help delivery: in-jar now, hosted docs are the planned
future"). I endorse the goal. Three reframings below change what gets built.

## Reframing 1 — the URL space is a frozen public identifier contract, and this project already solved that problem once

AC-3 ("every hosted page's URL is derivable from its in-jar topic id") reads like a
convenience. It is not. It promotes `/home/user/JLS/resources/help/Map.jhm`'s 84
topic ids into permanent public addresses. Those ids are 1998-era JavaHelp targets:
`edit.over`, `cutcopydel`, `sigformat`, `TRISTATE`, `top`. They were never designed
to be seen by a human, and today nothing stops a contributor renaming one — only
three call sites reference topics by literal in code (`InteractiveSimulator.java:156`,
`StateMachineDialog.java:397`, `TruthTableEditor.java:103`); the rest flow through
`ElementFormDialog`. The moment AC-1 ships, a rename breaks a syllabus.

JLS has already met this exact problem — an identifier that escaped into the wild and
may never break — and solved it well, in `/home/user/JLS/src/jls/elem/SaveTags.java`:
a frozen canonical table, an `ALIASES` map for renames, a `resolve()` that applies
aliases first, and `SaveTagsTest` guarding alias hygiene. The docs URL space is the
same object. The right shape is `HelpTopics` alongside `SaveTags`: frozen canonical
topic ids, an alias table, `resolve(id)`, and a test asserting no canonical id is ever
deleted. Then AC-3's "derivable" becomes true *by construction* rather than by
inspection, dead syllabus links redirect instead of 404, and the boundary note's
request to #584 ("record topic-id stability explicitly" in a diff report) upgrades
from an audit artifact to an enforced contract. Without this, #584 can rename an id,
pass all five of its ACs, and break every link this issue ever emitted.

## Reframing 2 — cut at a redirector seam, not at the publication seam

AC-1 and AC-2 as written create an **append-only obligation with no end date**: every
patch release emits a full immortal site copy, because binaries in the field compute
`/5.0.1/...` links forever. Add FEAT-C35-3's screenshots and each copy is megabytes,
not the current 488 KB of `resources/help`. Nobody prices this, and the release job
in `.github/workflows/release.yml` gains a deploy step that can fail *after* the tag
and artifacts exist — a partially-released state the workflow currently has no
concept of.

The alternative is one indirection. The in-app button computes a **stable, unversioned
resolver URL** — `/h/<topic-id>?v=<version>` — and the site resolves it: exact version
if published, else the nearest published version with an honest banner ("you are
running 5.0.1; this page documents 5.2"), else the GitHub blob at the tag. This is a
static-site-compatible design (a small index plus a resolver page), and it buys three
things the issue's scheme cannot:

- publication policy becomes free: publish every minor, not every patch, and garbage
  collect old copies, without breaking a single in-field binary;
- old-version fidelity has a free backstop — **git already stores every version
  immutably**, and `github.com/anadon/JLS/blob/v5.0.1/...` is already a per-release URL
  that will outlive any site;
- the resolver is where the offline story is honest. Today no code in `src/` calls
  `Desktop.browse` at all; this issue introduces that capability into a program whose
  entire identity is a self-contained offline jar. A button that hangs behind a lab
  proxy is worse than no button. It needs a fail-soft contract (copy-the-URL fallback,
  never a modal hang, and no network touch until the user clicks) stated as an AC,
  not discovered later.

## Reframing 3 — I am disregarding AC-5 as scoped: search belongs in both targets

AC-5 gives client-side search to the web reader and leaves the offline lab student —
the user the project repeatedly names as primary — with the same searchless
`JEditorPane` in `/home/user/JLS/src/jls/Help.java` they have today. That is a values
inversion against CAP-35's own thesis ("one source, multiple targets"), and it makes
the hosted site strictly better than the jar for the first time, which is precisely
how "offline parity is non-negotiable" starts eroding — not by breaking, by becoming
second-class.

The elegant version costs about the same milliwork: **FEAT-C35-1 emits the search
index as a build product into both targets**. For 83 pages the index is tens of
kilobytes. The web consumes it with JavaScript; `Help.java` consumes it with a filter
field over the existing TOC `JTree`. One artifact, two consumers — the capstone's
actual claim, demonstrated. AC-5's committed fixture terms then pin *both* renderers
at once, which is also a stronger test than the web-only version. If only one target
can have search first, it should be the jar.

## Two smaller things that pull against the arc

**Double-homing a normative document.** AC-2 requires the hosted manual to carry "the
batch-interface guide". But `docs/batch-interface.md` is 336 normative lines living in
the repo, while the in-jar batch page (`resources/help/simulator/batch/overview.html`)
is 30 lines of student prose. ARCHITECTURE.md is explicit that repo docs "are the
normative home for contracts; in-jar help is the student-facing manual." Publishing
the guide onto this site without deciding which copy is canonical creates a second
source of truth for a *stability contract* — the exact failure CAP-35 exists to
abolish. Decide it in this issue: either the site links out to the tagged GitHub blob
(cheap, honest, keeps one home), or `docs/*.md` enters the C35-1 pipeline and GitHub
rendering becomes the derived view (coherent, but far beyond a 1–2 mw band).

**Hosting custody is unstated.** The issue never says where this lives. A syllabus URL
is a decade-long promise made by a single-maintainer project; a personal domain that
lapses is worse than `anadon.github.io/JLS`. This deserves the same custody reasoning
the project already applied to release signing keys in #136 — a recorded decision, not
a default that happens at deploy time.

## What I would keep unchanged

AC-4 (hosted link integrity at `HelpTopicsTest` strictness) is exactly right and should
not be diluted — `/home/user/JLS/test/jls/HelpTopicsTest.java`'s seven assertions are
the floor. AC-1's insistence that publication be part of the release procedure rather
than a remembered step is also right, and is strengthened, not weakened, by Reframing 2:
a resolver-based site makes the deploy step small enough to be safely automatic.

## Recommended sequencing change

`ordering_after: [FEAT-C35-1]` is correct but insufficient. The frozen topic-id table
(Reframing 1) is a *prerequisite inside #584*, not a deliverable here — it must exist
before the first URL is published, because a public identifier space cannot be
retrofitted after the links are in syllabi. Move it explicitly into #584's scope and
this issue becomes what it should be: publication and addressing over a contract that
is already safe to address.
