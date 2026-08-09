# Issue #845: TASK-C574-1: the README's "Try it in your browser" link lands on a running circuit, and a dead demo URL turns a lane red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the yaml and the five ACs and #845 is one sentence: *a stranger who lands on
the README should be able to see JLS working before deciding whether to install it.*
That goal is right and it is the terminal step of a well-argued chain (#510 survey →
CAP-32 #516 → #572 spike → #573 static pages → #574 integration → this task). Nothing
in the chain pulls against the project's arc; CAP-32 was scoped precisely to buy the
zero-install evaluation win *without* becoming a web app, and #573's "static files, no
backend, nothing that can die and take user data with it" is the same permanence
argument the README already makes about reproducible jars and provenance attestations.
The demo, framed that way, is not a pivot — it is the shop window of a tool whose whole
pitch is durability.

But #845 as written picks two mechanisms that are, on a whole-project reading, the
wrong seams to cut along. Both have simpler and more durable alternatives already
present in this repository.

## Reframing 1: the demo link's integrity is a build invariant, not an HTTP probe

AC-3 asks CI to fetch the demo URL and turn a lane red when it is dead. That is a
network check for a resource **this repository will itself publish**. Its sibling task
#847 (TASK-C574-2) already got this right for the gallery:

> AC-3: A check fails the build when the gallery claims a demo link whose target is not
> present in the demo bundle.

That is an offline, deterministic, zero-flake invariant. #845 should adopt exactly the
same shape: the README's demo link is derived from (or checked against) the same example
manifest #847 names, and CI asserts that the path the README advertises exists in the
demo bundle at the commit that publishes it. If the demo is published from in-tree
static files — which #573's AC-3 ("deployment is a file copy") all but guarantees — then
"is the demo URL alive" collapses into "does this file exist in the bundle we just
built," and the HTTP check has nothing left to catch except a hosting outage, which
failing a *build* lane cannot fix and should not report.

This is not a hypothetical style preference: the repo already does link checking this
way. `test/jls/HelpTopicsTest.java` (issue #70) resolves every `href`/`src` in the
bundled help tree against the exact resource names copied into the jar, case-sensitively,
offline, as a JUnit test. That is the project's established idiom for "a link that
claims something must claim something true." A one-URL curl in a workflow is a second,
weaker mechanism for the same job.

## Reframing 2: if an external link check is built, build the one the repo actually needs

There is **no link checking of any kind** over `README.md`, `docs/*.md`, `ARCHITECTURE.md`
or `CHANGELOG.md` today — I checked all six workflows (`ci`, `codeql`, `mutation`,
`release`, `repro-installers`, `scorecard`) and found nothing. Meanwhile the README
carries roughly twenty external links (scorecard.dev, signpath.io, JetBrainsRuntime,
steveicarus.github.io, ghcr, the Releases page) and about thirty relative links into
`docs/` and `scripts/`. Standing up link-checking infrastructure whose declared coverage
is *one URL* is the least valuable version of that infrastructure. Two lanes, and the
demo URL is simply one row in each:

- **Blocking, offline, every push:** every relative link and anchor in every tracked
  markdown file resolves to a file that exists. Catches the failure that actually
  happens in this repo — a doc gets renamed and eight references rot — and it never
  flakes. Natural home: a JUnit test next to `HelpTopicsTest`, so repo docs and in-jar
  help are checked by one discipline rather than two.
- **Non-blocking, external, nightly cron:** HTTP reachability of outbound URLs, reported
  as an issue or an annotation. The `gui-wayland` lane already establishes the
  nightly-cron precedent in this repo.

AC-3's literal reading — a dead *or redirected-to-error* target fails a lane on every
push — makes every PR hostage to third-party uptime, rate limits, and Cloudflare
interstitials. For a single-maintainer project whose CI already runs five GUI-boot rigs
across four operating systems plus Agda proof verification, adding a flaky blocking lane
is a net loss of signal. I would state plainly: **AC-3 as written should not be
implemented.** The manifest invariant above plus a cron-scheduled external sweep serves
the stated intent ("a broken shop window is worse than no shop window") strictly better.

## Reframing 3: the README's first screen is the real problem, and it does not need #573

Read the README as a stranger. Line 1 is the title. Line 3 is an OpenSSF Scorecard badge.
Lines 5–10 say what JLS is, correctly and well. Line 12 begins `## Installing JLS`, and
from there to roughly line 130 the reader is inside Authenticode publisher caveats,
`SHA256SUMS-installers-<os>-<arch>` asset names, a paragraph explaining that installers
are *not* byte-reproducible while the jar is, and a rationale for the absence of a GPG
signature. This is an excellent maintainer-grade verification dossier. It is not a shop
window. There is not one image in 368 lines of a README for a *graphical circuit editor*.

Dropping a "Try it in your browser" link above that wall improves it, but the marginal
win is small compared to the restructure the issue never proposes: a first screen that
answers *what is this / what does it look like / try it / install it* in that order,
with the security and verification prose demoted to a `docs/verification.md` that the
install section links once. AC-5's "try, then install — rather than competing for the
same attention" is gesturing at exactly this and then declining to do it.

Crucially, **most of that work does not depend on #573 at all.** JLS already exports
resolution-independent SVG (`-i out.svg`, README line 134) and #551 is a static SVG
gallery. A hero render of a real circuit plus a two-line pitch is available today, costs
nothing to operate, and cannot rot. I would decouple #845 into:

1. **Now, unblocked:** the README gains a shop-window top — hero SVG, one-line "what the
   demo/gallery is", and the try-then-install ordering AC-5 asks for, pointing initially
   at the gallery. Offline link invariant lands with it.
2. **When #573 exists:** the try-link's target changes from the gallery to the running
   demo. That is a one-line diff, not a task.

This matters because of where #845 sits in the tree. It is gated behind #573, which is
gated behind #572, which is a **binding go/no-go that may return no-go** — and #516's own
provenance note explicitly invites the maintainer to close the whole capstone as inside
CAP-19's (#500) refusal. #845 is a 0.25 mw leaf on a branch that might be pruned entirely,
yet it is the only issue in the chain whose value is largely independent of whether
CheerpJ works. Leaving the README's first-impression fix stranded behind a speculative
Swing-in-browser spike is the one genuine misalignment here.

## A gap none of the five ACs covers: demo/release version coherence

The classic failure mode of a hosted demo is not a 404 — it is a demo silently three
releases behind the installers, so the stranger evaluates a JLS that no longer exists and
the funnel converts them onto a different product than the one they saw. AC-1 through
AC-5 check that the link *resolves*; nothing checks that it resolves to something
*current*. Add: the demo bundle is published from a release tag, and the demo page (or
the README line) names the version it runs. This is cheap at build time and impossible to
retrofit once the drift is a year old.

## Alternatives considered and rejected

- **"Open in GitHub Codespaces" badge.** The devcontainer already exists and can bake a
  JetBrains Runtime for the GUI, so this is a genuinely running JLS with nothing installed
  locally — and a one-line README change available today. It fails AC-2's account-gating
  rule and is the wrong audience (contributors, not evaluators), so it does not replace
  the demo. It is still worth adding under Contributing for what it does serve.
- **Container one-liner as the "try it" path.** `docker run --rm ghcr.io/anadon/jls -b -t
  tests circuit.jls` is zero-install for the technical evaluator and already documented,
  but it is headless by construction and shows nothing. Not a substitute for a shop window.
- **Doing nothing until #572 reports.** Defensible on cost, but it leaves the README with
  no visual and no evaluation path for however long the spike takes, which is the exact
  gap #510 §4 identified as structural.

## What I would keep unchanged

AC-2 (static target, no operated/account-gated/tracking redirect) is the sharpest
criterion in the issue and should survive any reframing — it is the permanence property
that distinguishes this from every dead demo link on the internet, and it is checkable.
AC-4 (one line saying read-only, curated) is right and cheap, and prevents the
over-promise that would otherwise generate the exact in-browser-editor requests KC-32-2
exists to refuse.

## Recommendation

Endorse the goal; rework two ACs and unblock the rest. Replace AC-3's HTTP probe with
#847's manifest/bundle invariant plus a nightly external sweep covering the whole repo,
not one URL. Make AC-1's "above the fold" machine-checkable (the link appears before the
first `##` heading) rather than viewport-dependent prose. Split the README restructure out
of the #573 dependency so the first-impression fix ships now with a gallery/SVG target and
swaps to the demo URL later. Add a version-coherence criterion.
