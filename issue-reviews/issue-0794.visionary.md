# Issue #794: TASK-C585-1: cutting a release publishes the manual under a versioned path and repoints latest, and an older version's URL keeps serving its own content
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Strip the mechanism away and the goal is one sentence: **a URL that an instructor can
paste into a syllabus in September and that still means the same thing in April.** Not
"we have a docs site" — JLS already has web-readable normative docs on GitHub
(`README.md`, `docs/*.md`, per ARCHITECTURE.md's help-delivery decision). What is
missing is a *rendered, addressable, version-pinned* form of the student-facing manual
that lives in `resources/help/**` and is today reachable only from inside the running
jar. That gap is real and worth closing. The goal is endorsed without reservation.

The mechanism, though, is where the issue quietly makes three architectural choices it
never names, and one of them is load-bearing enough to reframe the task.

## Reframing 1 (the headline): publish first, migrate second — drop `ordering_after: [TASK-C584-3]`

#794 blocks on #793, which blocks on #792, which blocks on #791: publication waits for
the entire source-format decision, generator, and mechanical migration (a 3–4 mw chain)
before a single URL exists. That ordering is not required by anything, and the repo
itself says so.

`resources/help/**` is 95 files, 488 KB, pure HTML with relative links and **zero
external references** (`grep -rl http resources/help --include=*.html` returns nothing).
ARCHITECTURE.md's recorded decision states the reason outright:

> **Portability discipline until then:** help content stays plain HTML 3.2 with
> relative links and no viewer-specific markup, and the `HelpTopicsTest` link checker
> (#70) keeps it truthful, **so the same tree can be published to the web without
> rewriting.**

The tree was deliberately kept publishable-as-is, and #794 is the moment that
discipline was saving up for — but the task chain spends it last instead of first.
Concretely: a deploy job that copies `resources/help/**` into `gh-pages/<version>/`
and repoints `latest` is roughly a day of work today, satisfies AC-1, AC-2 and AC-3
verbatim, and makes AC-4 trivially true (copying files into a web branch cannot make
the jar reach the network).

Why this is better and not merely faster:

1. **It de-risks #791–#793 by giving them a live consumer.** Choosing a source format
   and a topic-id → URL rule against a hypothetical site is guesswork; choosing them
   against a published site with real URLs and real search-engine behaviour is
   engineering.
2. **Versioning is exactly the mechanism that makes shipping early safe.** The usual
   objection — "URLs will churn when the migration lands" — does not apply. `/5.0.3/`
   URLs are frozen the moment they are published; the migration only has to hold the
   topic-id → URL rule from the version where it lands forward. TASK-C585-2's computed
   "open in browser" link is version-scoped by its own AC-2, so it is unaffected.
3. **It separates two genuinely independent concerns** that the current chain fuses:
   *how content is authored* (#584 cluster) and *how releases address published
   content* (#585 cluster). Those seams should not be welded together by an ordering
   edge.

I am explicitly setting aside `ordering_after: [TASK-C584-3]`. Keep the dependency in
the other direction only: #793's second target must emit into the layout #794 already
publishes.

## Reframing 2: the site is a cache, not an archive — make AC-3 a checkable invariant

AC-3 — "an older version's URL keeps serving that older version's content" — is
currently a **custodial promise**: it holds as long as nobody clobbers the branch, the
Pages deployment does not lose history, and every deploy is correctly additive. That is
the weakest kind of guarantee, and it is verified in the issue by the weakest kind of
check ("verified against a previously published version", i.e. look at it once).

JLS's whole distribution trajectory rejects custodial promises. The jar and BOM are
**bit-for-bit reproducible from the tagged source**; every release ships a `.buildinfo`
and a provenance attestation; `repro-installers.yml` re-derives artifacts in CI;
`docs/reproducibility.md` publishes the independent-rebuild recipe; the README goes out
of its way to explain exactly which guarantee each artifact carries. The documentation
site should inherit that posture rather than opting out of it.

The reframing: **the site for version X is a pure function of tag vX.** Hosting is then
a cache of a derivable thing, not the unique copy of an unrecoverable thing. Two
concrete consequences:

- **New AC (determinism):** the emitted site directory is byte-reproducible from the
  tag — no build timestamps in the HTML, no wall-clock or random ids, stable ordering
  in the client-side search index (#795). This is not optional once you want the next
  bullet, and it is one line of policy now versus a painful retrofit after the search
  index lands.
- **Replace "verified against a previously published version" with a CI check:** on
  each release, check out the previous tag, regenerate its site, and byte-compare
  against what is currently served at `/<prev-version>/`. A silent clobber, a partial
  deploy, or a generator that stopped being version-faithful all turn the build red.
  That converts AC-3 from a promise into an invariant, and it costs one job.

The payoff beyond rigor: if Pages is ever lost, misconfigured, or has to be migrated to
another host, recovery is `for tag in $(git tag); do rebuild; done` rather than an
archaeology exercise. It also makes retention *safe*: old patch versions can be pruned
to keep the branch small (this matters once #586's screenshots multiply the per-version
footprint well past 488 KB) precisely because they can be regenerated on demand.

## Reframing 3 (smaller, but nearly free): one tree, not two

FEAT-C35-1 emits two targets and then spends #793 AC-4 on a test asserting the two
carry the same topic set. Parity asserted is weaker than parity by construction. An
alternative worth pricing before #793 commits: the site *is* the in-jar tree, plus a
nav shell, a search index, and a stylesheet layered on at publish time — not a second
rendering of the source. Divergence then becomes impossible rather than detected, and
#793 AC-4 evaporates. This may lose on aesthetics (an in-jar-shaped page is a plain
page on the web), so I flag it as a decision to make deliberately in #791/#793, not as
a demand on #794. But #794 shipping against `resources/help/**` directly, per
Reframing 1, is the cheapest way to find out how good that plain page actually looks.

## Three design points the issue does not consider

1. **Deploy mechanism decides what AC-3 even means, and the issue never picks one.**
   `actions/deploy-pages` uploads a *complete* site every time — under it, "keep serving
   old versions" means reconstructing every prior version on every release (which
   Reframing 2 makes tractable, and nothing else does). A `gh-pages` branch is
   naturally accretive and makes old versions persist by default. These are opposite
   failure modes: one fails loudly and expensively, the other fails silently. Name the
   choice in the issue. My recommendation: `gh-pages` branch (accretive) **plus** the
   regeneration audit from Reframing 2 — cheap steady state, real invariant.

2. **The docs publish must not be able to break a release.** AC-1 says publishing is
   "part of the release procedure", which reads as "in the release job". `release.yml`
   is deliberately hardened: `permissions: contents: read` at the top, each job
   elevating only what it uses, every action pinned to a full SHA, and the repo carries
   an OpenSSF Scorecard badge that grades exactly this. Adding `pages: write` or a
   branch push into the job that signs provenance widens the blast radius of the most
   security-sensitive job in the repo for the least security-sensitive output. Put
   publishing in a **separate, idempotent, re-runnable job** gated on the release job
   succeeding. "No manual step" is satisfied by *triggered by the tag*, not by *same
   job* — and a Pages hiccup then costs a re-run rather than a poisoned release.

3. **Nobody has thought about search engines, and for this audience that is the actual
   failure mode.** Students do not paste syllabus URLs; they Google "JLS memory element".
   With five versions published and no `rel=canonical` or `robots` policy, Google will
   happily serve `/5.0.0/` forever — the exact stale-content problem the versioning was
   meant to solve, arriving through the side door. Concrete addition: `latest` is the
   only indexed tree; every versioned page carries `<link rel="canonical">` pointing at
   its `latest` counterpart when one exists; superseded versions get `noindex` and a
   visible "you are reading docs for 5.0.0; the current release is 5.1.0" banner (a
   build product, not hand-maintained). This is standard practice in every mature docs
   toolchain and is cheap now, invasive later.

## On AC-4

AC-4 (offline parity, asserted by #792's in-jar test) is ceremonial here — copying files
onto a web host cannot make the jar reach the network, and the test it names belongs to
another task. The invariant is genuinely at risk in **#795**, where computed
"open in browser" links are the seam through which network dependence could leak into
the in-app experience. Keeping the line costs nothing, but do not mistake it for
verification of anything this task does.

## Summary

The goal is right and squarely on the project's arc: JLS's distribution story is about
addressable, verifiable, version-pinned artifacts, and the manual has been the one
user-facing artifact with no address. Endorse it — with the ordering inverted so
publication ships against today's already-portable help tree instead of waiting on the
authoring migration, and with AC-3 restated as a reproducibility invariant (the site
for a version is a pure function of its tag, audited in CI) rather than a promise not
to clobber a branch. Both changes make the task smaller *and* stronger, and both are
the project already applying to documentation the standard it applies to every other
artifact it ships.
