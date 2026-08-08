# Issue #857: TASK-C580-3: the winget submission-and-review cycle gets its cost written down, in the same ledger every channel reports to
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task framing away and #857 is not "write down how long winget took." It is
the enforcement organ for CAP-34's (#518) only kill criterion. KC-34-1 promises that a
channel costing >0.5 mw per release cycle "is dropped with the arithmetic recorded."
Without a durable record, that promise decays into a vibe, and CAP-34 becomes what
every distribution effort at bus factor 1 becomes: four channels nobody can afford and
nobody will admit to abandoning. The capstone even names the precedent it is trying not
to repeat ("the four-vendor drift lesson from CAP-21's KC-21-3"). So the end #857 serves
is real and important, and the project is right to file it before the channels multiply.

The ledger *as specified*, though, is the weakest version of that organ. It measures one
half of a two-sided decision, it is fed by the least trustworthy input in a repository
whose entire culture is machine-derived evidence, and its ordering guarantees the
sharing it exists to enforce will fail.

## Reframing 1 — a cost-only ledger cannot make the decision KC-34-1 pretends to make

A channel is not dropped because it is expensive. It is dropped because it is expensive
*relative to what it delivers*. 0.4 mw/cycle for a listing nobody installs is a worse
deal than 0.6 mw/cycle for the channel that carries most of the project's new users, and
a ledger with only a cost column will make exactly the wrong call in that case while
looking rigorous doing it.

CAP-34 already commissions the other half. Feature 582 — #861 (collector script,
per-release/per-asset download counts, appended idempotently to a tracked diffable file
with a documented schema and known-limitations note), #863 (the policy doc naming the
KPI and stating explicitly whether "Flathub / winget / Homebrew counters are included,
excluded, or reported as separate columns") — is a second tracked, append-only,
per-release, schema-documented data file about the same three channels, filed under the
same capstone on the same day. Two files, two format docs, two idempotency stories, two
places a future reader must join by hand to answer the one question the capstone
actually asks.

**Concrete alternative:** one `docs/distribution/channels.tsv` (or `.md` table) keyed by
`(release, channel)`, with two column families — *cost* (attention minutes, latency
hours, intervention flag) and *reach* (asset downloads / store installs, or an explicit
`unavailable` per #863 AC-2). #861's collector appends the reach columns; the channel
automation appends the cost columns. KC-34-1 then evaluates as a ratio on a single row,
CAP-34 AC-3 ("≥4 consecutive scheduled runs recorded in-tree") is satisfied by the same
accumulation, and #863's "are store counters in the number" question has a physical
answer — a column — instead of a prose one. This is one artifact and one schema doc
where the tracker currently plans two of each.

## Reframing 2 — derive the cost, do not remember it

AC-2 asks for "time spent on the submission, on review turnaround, and on any breakage,"
recorded by the single maintainer, weeks after the fact, as the input to a kill decision.
That is a stopwatch diary, and it is culturally foreign to this repository. JLS does not
trust hand-copied anything: #859 refuses a hand-copied caveat and asserts byte-equality
against the README instead; #861 AC-5 wants a fixture-driven parse test rather than a
trusted eyeball; the rigs (`scripts/wayland-rig-selftest.sh`,
`scripts/windows-rig-selftest.ps1`) insist a guard be shown red before it is trusted
green; releases carry `.buildinfo` and attestations precisely so nobody has to be
believed. A self-reported hours column is the one number in the tree with no
counter-check at all — and it is the one gating a channel's life.

Nearly all of it is already published, for free, by the platforms themselves:

- **Latency** — winget-pkgs PR `created_at` → `merged_at`; the cask PR or tap commit
  likewise. Exact, third-party, zero effort.
- **Intervention** — count of maintainer commits/comments on the submission PR after it
  opened, and whether the release workflow was re-run. A submission that merged with
  zero maintainer events cost ~0 attention; that is the common case and it should record
  itself.
- **Breakage** — the failed-workflow signal #856 AC-4 and #860 AC-3 already require to
  exist and be red.

The residual — the offline hour spent working out why the manifest validator rejected
the installer switches — is the only field a human must type, and AC-5's estimate flag
belongs precisely there. Recast that way the ledger is a derived artifact with a small
manual annex, and "review turnaround" becomes measured rather than remembered.

## Reframing 3 — attention and latency are different quantities with different verdicts

"Time spent" as one column silently merges two things that demand opposite responses.
Ten minutes of maintainer attention plus eleven days waiting on Microsoft's reviewers is
a *cheap* channel with a *slow* pipe. Under a single column and a 0.5 mw threshold it can
read as either fine or fatal depending on how the number was written, and neither reading
is right: killing winget because someone else's queue is slow fixes nothing JLS controls,
while masking real toil behind a fast merge defeats the criterion. Record them as two
columns. Cost over threshold triggers KC-34-1. Latency over threshold triggers a
different, weaker response — CAP-34 AC-2's "propagates to every channel by automation
within days" is what is failing, and the remedy is a staleness alarm (#860 AC-3's
"stale-but-green listing" fear), not a drop.

## Reframing 4 — the row should be an output of the automation, not a downstream chore

#856's own outcome text names the failure mode: a version bump should be "a workflow
output rather than a thing someone has to remember three days later." #857 then creates
exactly such a thing-to-remember for the cost record. The stronger seam is the one the
sibling tasks already cut: the workflow step that submits the manifest (#856) or updates
the cask (#860) writes the ledger row when the submission closes. Then #860 AC-4 and the
Flathub equivalent are satisfied by the shared writer rather than by three separate
promises to use the same file, and the ledger cannot silently stop being kept — which is
the realistic death of every hand-kept ledger.

## Reframing 5 — pre-register the estimate, which also dissolves the ordering

`ordering_after: ["TASK-C580-2 (a cycle must have happened to be costed)"]` is the
sequencing bug at the heart of this issue. AC-3 exists so that #579's and #581's tasks
"record into the same file rather than inventing their own" — yet #858 orders after
nothing and #860 orders only after its own siblings, so Homebrew can land, with #860
AC-4's obligation to write into "the shared per-channel maintenance ledger," before that
ledger exists. The one thing #857 is uniquely for is thereby scheduled last.

A schema needs no data. Invert it: **the ledger and its threshold policy ship first,
ordering_after `[]`, blocking #856, #858, #860 and the Flathub tasks** — and the first
row for each channel is written *before* that channel ships, carrying the
pre-registered cost estimate, flagged as an estimate exactly as AC-5 requires. Then the
measured row lands beside it and the ledger records *forecast error*, not just cost.
That is what turns KC-34-1 from a post-hoc rationalization ("well, 0.45, close enough")
into a falsifiable commitment made before the answer was known. It costs nothing, it is
the same instinct as showing a guard red before trusting it green, and it makes AC-5
load-bearing rather than a caveat.

## Does it strengthen the arc, or pull against it?

It strengthens it, in a way worth naming: JLS's README already lists deb, rpm, AppImage,
Nix flake, signed MSI, dmg, multi-arch container, jar and Maven. The gap #510 found was
*discoverability*, not installability — and adding three more channels to a
nine-artifact matrix at bus factor 1 is precisely where a project acquires obligations it
cannot retire. An enforceable retirement mechanism is the thing that makes CAP-34 safe to
attempt at all, and it deserves more standing than a 0.25-mw task hanging off the winget
feature. It should be a CAP-34-level artifact (or a recorded decision in ARCHITECTURE.md's
"Recorded decisions" section, which is where this repo already keeps durable policy), and
its scope should extend to the per-release toil the project *already* carries and has
never costed — e.g. `docs/wayland-desktop-checklist.md`, a manual once-per-release
procedure whose results are recorded as issue comments rather than in-tree.

## Verdict

**endorse-with-reframing.** The end is right and under-served, not over-served; keep the
ledger. Change five things: merge it with feature 582's KPI series into one per-channel
record with cost *and* reach columns; derive latency and intervention from
platform-published PR timestamps rather than memory; split attention from latency because
they trigger different verdicts; write the row from the channel automation rather than as
a downstream chore; and ship the schema first with pre-registered estimates so it blocks
the channels instead of trailing them. I am explicitly setting aside AC-2's
"ordering_after a cycle must have happened" premise — the first row should predate the
first cycle — and AC-1's implicit assumption that this file is cost-only.
