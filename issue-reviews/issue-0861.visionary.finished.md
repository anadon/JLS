# Issue #861: TASK-C582-1: a committed script collects per-asset download counts and appends them idempotently to a tracked file — re-running a recorded date changes nothing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Strip the mechanics and #861 is the first honest instrument this project has ever
pointed at itself. #508's verdict was arithmetic about a number nobody was measuring:
"adoption of this repo is zero — and the one live user is elsewhere." Stars were quoted
because stars were the only counter lying around. #582/#861 replace a bad proxy with a
real observation, and that instinct is exactly right and exactly in character: this is
the same repository that refuses to let a checksum imply provenance, states in the README
precisely what an attestation covers and what it does not, and declines a signing key
that would add rotation risk without adding a guarantee (#136). Measuring downloads
rather than stars is that discipline applied to the project's own self-report.

So I endorse the intent without reservation. My argument is with the *shape* of the
instrument, because as specified it will answer a question nobody needs while remaining
blind to the one question #508 says decides this project's future.

## Reframing 1 (the important one): the collector is scoped to the wrong repository

anadon/JLS has six releases and, per #508, essentially no users. bsiever/JLS has the
users — WashU CSE 260M points students at *that* fork's releases, documented in an
ACM CF'25 paper. A collector hard-scoped to `anadon/JLS` will therefore record a year of
near-zeros and will be unable to distinguish the two hypotheses that demand opposite
responses:

- "JLS is a tool nobody wants" → kill criteria fire, the roadmap contracts.
- "JLS is a tool people want and they get it from someone else" → #508 item 1
  (reconcile with the live user base) is the whole strategy, and the KPI's job is to
  *size the prize* and later to *show the reconciliation working*.

The public releases API serves any public repository with the same unauthenticated call.
Making the collector take a repository list — `anadon/JLS` plus the fork lineage
(bsiever/JLS, and any other public fork that publishes releases) — and carrying `repo` as
the leading key column costs one loop and one column. It converts the file from a vanity
series into the only piece of evidence that could ever confirm or refute the central
claim of the August 2026 review. **If I could change one thing about this task, this is
it.** It also gives the series a non-degenerate baseline on day one: a comparison is
readable at n=1 observation, where a self-only series needs months before it says
anything at all.

Cost is inside the filed 0.25–0.5 mw band. The only new obligation is a sentence in the
AC-4 schema note saying the lineage rows are observations of *other people's* public
release assets, collected from the public API, folded into no aggregate without being
labelled — which is the same care the README already takes with the fork-format caveats.

## Reframing 2: "one row per date/release/asset" defeats AC-2's own purpose

Run the arithmetic AC-2 never runs. Six releases today, each carrying roughly fifteen to
twenty-five assets (two deb arches, rpm, two AppImages, two MSIs, dmg, jar, `bom.json`,
`.buildinfo`, and a `SHA256SUMS-*` per os/arch). A daily row per date/release/asset is
~120 rows appended per day — ~44k rows in the first year, growing with every release,
and with the fork lineage folded in, multiples of that. Nearly every one of those rows
will be numerically identical to yesterday's, because the counters are at or near zero.

AC-2 justifies the format with "a git diff shows exactly what changed." A 120-line daily
diff in which zero to one lines carry information shows exactly the opposite. The signal
is destroyed by the encoding chosen to reveal it.

**Alternative: append on change, plus a heartbeat.** A cumulative counter that did not
move carries no information; the only facts worth recording are (a) a counter moved, and
(b) the collector ran and observed. So write one row per *changed* (repo, tag, asset)
observation, plus exactly one heartbeat row per run recording the run timestamp and the
count of assets observed. This is lossless — every historical value is reconstructible by
carrying the last observation forward to the next heartbeat — and it makes `git log -p`
on the file a literal adoption narrative: every line ever added is a download that
happened. For a project at JLS's volume the file stays a few hundred lines for years, and
the first real download is a one-line commit somebody will notice. It also fixes AC-3's
requirement for free: a re-run on a recorded date observes no change and appends nothing
but a heartbeat, so idempotency stops being special-case logic and becomes a property of
the encoding.

I am explicitly disregarding AC-2's "one row per date/release/asset" here. It is the
mechanically obvious schema, not the one that serves the stated outcome.

## Reframing 3: an observation is immutable; a changed observation is an event

AC-3 permits the recorded row to be "replaced in place." That is the one option that
should be forbidden. GitHub's `download_count` is not monotone in practice: deleting and
re-uploading an asset, or re-cutting a tag, resets it to zero. A same-day re-run after
such a reset would, under "replace in place," silently overwrite the higher earlier
observation and erase the only evidence that anything anomalous happened. Under
append-on-change the same event appends a row showing the count going *down*, which is
precisely the shape of anomaly a maintainer wants shoved in their face — and which the
sibling task #862's "fail loudly rather than write a lie" instinct should also cover.
Immutable observations, never rewritten, also mean the file can never be the source of a
merge conflict resolved by picking the wrong side.

## Reframing 4: per-asset counts answer a *lane* question, not a volume question

The strategically interesting content of this data is not the total. It is which lane
users are in: deb versus AppImage versus MSI versus dmg versus bare jar versus riscv64.
That single distribution decides CAP-34's channel ordering (why build the Homebrew cask
before Flathub if nobody ever takes the dmg?), tells the project whether the RISC-V and
Wayland lanes have any constituency at all, and is the only way the KPI feeds back into
engineering rather than into a slide. AC-4 asks for a schema note; it should carry an
explicit, committed lane taxonomy — asset-name glob → (os, arch, channel) — so
"AppImage versus deb over six months" is a one-line query and not an afternoon of regex
archaeology. That taxonomy is also the seam where CAP-34's future Flathub/winget/Homebrew
counters attach without reopening the schema, which is the thing #582 AC-4 promises the
policy doc will have to be able to say.

While at it: exclude nothing quietly, but do state in the header note that at these
volumes the project's own automation and generic crawlers are a material share of any
non-zero count. A number this small is mostly robots until proven otherwise, and the
project's whole documentation style is to say that out loud rather than let a reader
over-read a figure.

## Alignment: AC-5 points at a test harness this repository does not have

"Unit coverage" in a Maven repo reads as JUnit under `test/`, and ARCHITECTURE.md's test
layout is entirely JUnit 5 element/loader/simulation contracts. A JUnit test for a
downloads collector would be a foreign body there. The repository already has the right
convention, arrived at four times independently: `scripts/*-selftest.sh` — pure-shell
guards that drive the *unmodified* script against a stubbed toolchain with no network,
wired into CI as their own step (`icestick-handoff-selftest.sh` at ci.yml:56,
`wayland-rig-selftest.sh` at :413, `x11-rig-selftest.sh` at :536, `macos-rig-selftest.sh`
at :613). AC-5's fixture-parse and idempotency checks are exactly that pattern:
`scripts/download-counts-selftest.sh` feeding a recorded API response to the real
collector via a `--from-json` input path, asserting the appended rows and the no-op
re-run. Reading AC-5 as "add a JUnit test" would fork the project's testing story for no
gain; reading it as "add the fifth selftest" strengthens an existing convention. Note
also that `riscv/test_primitives.py` exists and is wired into no workflow — the one
non-shell script test in the tree is the one nothing runs.

The `--from-json` path is worth having for its own sake: it makes the fixture the same
code path as production, and it makes a bad run replayable rather than re-fetchable.

## Where this sits in the arc

It strengthens it, and cheaply. #508's process finding was that planning was consuming
its own capacity; a 0.25–0.5 mw instrument that turns the largest open strategic question
into a checkable series is the opposite of that failure. It duplicates nothing (#590 is
what the project *says*; this is what the project *knows*). Its one structural risk is
the one #862's reviewer names — that this becomes a bespoke one-off rather than the first
entry in a metrics ledger the repo already needs for mutation scores and Scorecard
history. Written as proposed here (repo-keyed, append-on-change, immutable observations,
documented taxonomy), the collector is ledger-shaped by construction, and #862 can lift
the schedule and commit convention around it without redesign.

**Verdict: endorse-with-reframing.** Build it. Key it by repository and include the fork
lineage from day one; append only on change with a per-run heartbeat; never rewrite an
observation; commit the lane taxonomy alongside the schema note; and test it as the fifth
`scripts/*-selftest.sh`, not as JUnit. AC-2's row-per-day and AC-3's replace-in-place are
the two clauses I would strike.
