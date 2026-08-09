# Issue #582: FEAT-C34-4: adoption is answered with release-asset download counts collected on a schedule into a tracked file, and stars stop being the number anyone quotes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this is really for

The outcome sentence is right and the project needs it: JLS should be able to *answer*
the adoption question from data instead of vibes. The parent capstone (#518) and the
review that commissioned it (#508) are unambiguous about why. #508's headline finding is
not "we don't know our numbers" — it is **"adoption of this repo is zero, and the one
live user is elsewhere"**: WashU CSE 260M teaches JLS through `bsiever/JLS`, and #508's
success condition is *"external courses running JLS labs within 18 months."*

That is the question. Release-asset download counts on `anadon/JLS` are structurally
incapable of answering it, and that is the core of this review.

## Objection 1: the instrument cannot see the user the project already has

A course that teaches from `bsiever/JLS` downloads from bsiever's releases. A course
that runs the container pulls `ghcr.io/anadon/jls`. A lab that uses the flake never
touches an asset. The one confirmed live deployment of JLS in the world would appear in
this series as **zero**, indefinitely — and the series would be read as "no adoption,"
which is exactly the wrong conclusion, because the real state is "adoption exists and is
pointed at someone else's fork."

Worse, the metric cannot distinguish its own failure modes. At the scale in question, a
200-student course downloading a jar once each is ~200 events, indistinguishable from
crawler and mirror traffic on a public releases page, and both are swamped by the
project's own CI and by whoever runs `gh attestation verify` in a loop. A KPI whose
noise floor is comparable to the entire signal it is meant to detect is not a KPI; it is
a number that will be quoted anyway because it exists. #508's complaint about stars was
never "the number is too small" — it was "the number is not about anything." Replacing a
number that is not about anything with a different number that is not about anything
executes the letter of the recommendation and misses its point.

Note also that the title's second clause is already true: nothing in-tree quotes stars.
The only README badge is OpenSSF Scorecard; `grep` finds no star count in `README.md`,
`docs/`, or `CHANGELOG.md`. The "retirement" half of this issue is rhetorical. The real
content of AC-4 is the *scope* sentence, and see objection 2 for how hard that is.

## Objection 2: "release-asset downloads" is not one number, and the channels don't join

AC-4 asks for a sentence saying whether Flathub/winget/Homebrew counters are in or out.
That sentence is load-bearing in a way the issue underestimates, because those counters
are not commensurable with each other or with GitHub's:

- GitHub gives **cumulative per-asset downloads** since upload, all-time, monotone.
- Flathub's public stats are **downloads including updates** — every existing user
  re-downloading on each release inflates it.
- Homebrew's analytics API reports **install events in 30/90/365-day windows** from
  opt-out telemetry — a rate, not a total, over a self-selected population.
- winget has **no public install counter** at all. Neither does GHCR (no public pull
  count), nor GitHub Packages Maven.

So the boundary note's "the channel columns join later" is optimistic: two of the three
CAP-34 channels have counters with incompatible semantics, one has none, and two
surfaces JLS *already ships* (container, Maven) can never be counted. Any single number
summed across these is meaningless, and a policy doc that says "these are in, these are
out" mostly documents which APIs happened to be easy.

## Objection 3: this would be the first bot that writes to master

Every workflow in `.github/workflows/` declares `contents: read` at the top level.
`contents: write` appears exactly twice — the release jobs that publish assets, and
`dependency-submission` in `ci.yml:767` — and **no workflow anywhere runs `git commit` or
`git push`.** A scheduled job that appends to a tracked file is a bot commit on master
on a timer: a new standing write credential, a new interaction with #443's required-check
hardening, and daily commit noise on a repository whose own review (#508) named tracker
and planning churn as a real cost. For a project whose README sells reproducibility,
attestation and signed provenance, the first repo-writing automation should be worth
more than a slow-moving counter.

## Alternative A (the reframing I would fund): a sightings ledger, not a download counter

Keep the *shape* of the issue — committed script, schedule, idempotent append to a
tracked file, loud failure, policy doc — and change what it queries. #508 found the real
user not from a counter but from **search**: a course site and an ACM CF'25 paper. That
is the recurring measurement worth automating:

- GitHub code search for `.jls` files outside this repo, and for `jls -b -t` in
  autograder configs and CI files.
- The fork network and its release activity (which is precisely how bsiever's lineage
  would surface, and would have surfaced months earlier).
- Scholarly/syllabus queries for "JLS logic simulator" / "Java Logic Simulator".
- New external issues, PRs and stars-as-events only as *pointers to people*, never as a
  score — a single star from a named instructor is worth more than a thousand downloads.

The script appends **candidate sightings** (who, evidence URL, first seen) to a tracked
file; a human promotes confirmed ones into `docs/adoption.md`, a register of known
deployments with an evidence link and a last-confirmed date. That register is the KPI:
*count of courses/institutions known to be running JLS labs*, which is verbatim what
#508's success condition is written in. It is the same 0.5–1 mw, the same automation
shape, and it measures the thing that decides whether the fork lives.

Pair it with the ≈0 mw item #508 ranked highest — contacting Bill Siever — and the
ledger has its first row on day one instead of four weeks of zeros.

## Alternative B: if counts stay, make them a typed table with no total

Not "the adoption metric" but one column of an honest instrument panel: rows = surface
(GitHub assets, Flathub, Homebrew, winget, GHCR, Maven), columns = counter semantics,
public/not, value, as-of. Cells legitimately read "no public counter." **Never summed.**
And write it in this project's own established genre — an ARCHITECTURE.md *recorded
decision*, with rationale, a revisit trigger, and a kill criterion in CAP-34's KC-34-1
style: *if the series is flat and near-zero for two quarters, the number is not the
bottleneck and collection stops.* A "one-line policy doc" is beneath the documentation
standard set by `docs/batch-interface.md` and the recorded-decision section.

## Alternative C: a different seam — snapshot at release, not on a timer

GitHub's asset counts are cumulative and monotone; the only genuine archival argument for
committing a series is that counts are lost if an asset or release is deleted or
re-tagged. That argument is fully served by sampling at **release time**, inside
`release.yml`, which already holds `contents: write`, already runs on tag push, and
already writes release artifacts — snapshot the *previous* release's counts as part of
the release. No new cron, no new standing write credential, no daily diff noise, and the
sampling interval matches the only cadence at which the number could change a decision.
This also disposes of AC-2's ambiguity (a dated row for a value that moves all day has no
well-defined "the" value for that date); the natural schema is append-on-change over
`(release, asset, count, observed_at)`, not one row per day.

## What survives from the issue as written

AC-1 (unauthenticated public API, locally runnable) and AC-5 (fail loudly rather than
append a zeroed row) are exactly right and generalize to any of the alternatives above —
AC-5 in particular is the kind of instinct that makes the rest of this repository
trustworthy. A ~50-line counter script is cheap enough to keep as a curiosity under
Alternative B. What I am rejecting is its promotion to *the* adoption KPI.

## What I am explicitly disregarding

- **AC-3 ("four consecutive scheduled runs recorded in-tree")** — this is evidence that
  a cron works, not that anything was learned. Four rows of near-zero satisfy it while
  the project remains exactly as ignorant as before. Under Alternative A the equivalent
  evidence is *one confirmed sighting*; under Alternative C it is four releases.
- **AC-4's framing** — "names release-asset downloads as the adoption KPI" is the claim I
  am contesting. The doc should name *known deployments* as the KPI and downloads as one
  uncounted-elsewhere floor.

## Verdict

**rethink.** The end is correct and overdue; the instrument is aimed at the one surface
that provably cannot see the project's only real user, and it hard-codes that aim into a
policy document. Retarget the same budget and the same script/schedule/tracked-file
design at discovering and registering *who is using JLS*, keep the download counter as an
unpromoted column, and move the collection to the release seam so the project's first
bot-authored commit is not spent on a number that will read zero all year.
