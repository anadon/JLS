# Issue #567: FEAT-C30-1: a drive-by human files a usable bug report in five minutes — plain issue templates and a ten-line quickstart sit above the contract prose
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "two markdown files exist." Its parent CAP-30 (#514) is betting that the one
reachable developer pool in this niche is Digital's stranded contributors, and
that this repo currently *repels* arrivals — both 2026 external PR authors
bounced (#4/#5, #187), zero external issues ever. #567 is the front door of that
bet. The outcome word in its own title is **usable**, and the acceptance criteria
never touch it: AC-1 through AC-4 verify that a form *submits*, not that what
arrives is diagnosable. Everything below follows from taking "usable" seriously.

I endorse the goal and the band. I would build something meaningfully different.

## Reframing 1: the chooser is the product, and "alongside — not replacing" is the bug

`/home/user/JLS/.github/ISSUE_TEMPLATE/` currently holds exactly three files:
`capstone.md` (9.9 KB), `feature.md` (11 KB), `scientific_task.md` (19 KB) — 40 KB
of authoring instructions about tier legality, DAG walks, machine blocks, and
REPLAN protocol. There is no `config.yml`, so a stranger's chooser today is:
three walls of planning apparatus plus a "Don't see your issue here? Open a blank
issue" footnote. A human *can already file*. The barrier was never a missing
template; it is the **signal the chooser emits** — this tracker is a machine's
workspace, and you are in the wrong building.

AC-1 explicitly preserves that signal: the plain pair appears "alongside — not
replacing — the planning-corpus templates." That is adding a welcome mat beside
three legal notices. The tier templates cannot be usefully filled from the
chooser anyway (feature.md rule A requires walking other issues' machine blocks
before you may even add an edge) — they are an *authoring convention for one
maintainer and his agents*, not a public intake surface.

Concrete alternative: move `capstone.md` / `feature.md` / `scientific_task.md` to
`docs/plan/templates/`, referenced from CONTRIBUTING and copy-pasted when filing.
The chooser becomes: **Bug report**, **Feature request**, and contact links. The
problem disappears rather than being worked around, and this discharges #508's
process finding ("planning prose belongs in `docs/` on master... issues become
thin") instead of deferring it. Note `docs/plan/` does not exist on master today —
#493 wants it; this feature is a natural first tenant.

The single highest-leverage file in the whole feature is the one the issue never
mentions: `.github/ISSUE_TEMPLATE/config.yml`. It owns the blank-issue escape
hatch and the contact links that route "how do I build a flip-flop" to
Discussions and vulnerabilities to SECURITY.md, before either becomes a bug
report a maintainer must triage.

## Reframing 2: don't ask for the environment — hand it over

AC-2 ("completable in five minutes; no field demands tier/ID conventions") is
unverifiable prose in a markdown template. As a YAML **issue form** it becomes
structural: five inputs, three of them dropdowns (OS, install method —
deb/rpm/AppImage/msi/dmg/jar/container/nix, JLS version), `required: true` where
it matters, auto-applied labels, and no "delete these HTML comments" failure
mode. Markdown templates are the 2019 answer; forms are how you make AC-2 true
by construction.

Then the deeper move. The fields a drive-by reporter gets wrong or omits are
exactly the ones **JLS already computes**:

- `src/jls/DefaultExceptionHandler.java:212-232` already assembles a
  privacy-audited environment block — `JLSInfo.versionString`, `java.version`,
  `java.vendor`, `java.vm.name`, `os.name/version/arch` — deliberately trimmed
  under #51 so it is safe to share, and it already prints
  "Please attach this file to a bug report at https://github.com/anadon/JLS/issues".
- `src/jls/ToolkitPolicy.java` knows which AWT toolkit was selected. Given
  README's six-row desktop matrix (X11 / XWayland / WLToolkit / headless), this is
  the single most diagnostic fact about a GUI bug — and no reporter on earth
  knows to include it.
- `jls.LoadError`'s seven-category taxonomy is the right first field for any
  "my file won't open" report, and is already in `JLSInfo.setLoadError`.

Promote that block to a `jls.BugReport.environment()` and surface it three ways:
`jls --bug-report` (works on a locked-down lab machine and in the container),
Help → "Report a bug…" opening a **prefilled form URL**
(`?template=bug.yml&version=…&os=…`) with clipboard fallback when `Desktop.browse`
is unavailable, and a "Copy diagnostics" button on `About` (`src/jls/About.java`,
which today shows the version and nothing else, while CONTRIBUTING:128 tells
reporters to gather version *and* OS *and* JDK from three different places).

That is the same 0.5–1 mw band spent on the product rather than on prose, and it
converts "usable" from an adjective into a mechanism.

## Reframing 3: the attachment contradiction, and the elegant exit already shipped

SECURITY.md opens with a loud warning about malicious "patch" attachments;
CONTRIBUTING:22-24 says attachments are never applied. Meanwhile the crash
handler tells users to *attach* `JLSerror`. A drive-by reporter meets a mixed
message at the exact moment they are trying to help. The plain template must
resolve it in one line — diagnostics welcome, archives never.

Better: the project already ships the fix and nobody connected it. README
documents `jls -savetext out.jls circuit.jls`, which rewrites a circuit as plain
diffable text. A bug template that says *"run `jls -savetext bug.jls yours.jls`
and paste it in a code fence"* gets a reviewable, greppable, zero-trust-boundary
reproducer inline — no attachment, no security exception, no round trip. For a
tool whose bugs are nearly always "this circuit does the wrong thing," that is
the field that makes reports usable.

## AC-3 is nearly already done — and aimed at the wrong end of the file

`/home/user/JLS/CONTRIBUTING.md:1-15` already opens with a greeting and a
two-command build (`mvn verify`, `java -jar target/jls-*.jar`). The delta AC-3
asks for is "clone" and "open a PR" — four lines, minutes of work. Priced as a
third of a 0.5–1 mw feature, it is padding.

The actual repellent is further down: bullets on value semantics (#94), sealed
dispatch (#95), the NullAway ratchet (#93), and a 55-line coverage-ratchet
section with JDK-specific epsilon-headroom lore all sit **above** "Reporting
bugs" at line 126. A person who wants to report a bug must scroll past the
mutation-testing climb convention to learn how. Apply #508's own prescription to
this file: a short front page (build, test, PR, report a bug, where to ask), with
the ratchets moved to `docs/contributing-contracts.md` and linked. Same
information, correct ordering, and the contract prose stays intact as AC-3
requires — just not in the first screen.

## Fit with the arc, and one risk

This strengthens CAP-30 and pulls against nothing in ARCHITECTURE.md. Two
tensions worth naming:

- **Register.** #508 recorded a planning ratchet ("no new tier:feature/tier:task
  until two capstones close") and #567 was filed the next day, on the very
  feature template it exists to counterbalance, complete with `feat_id`,
  `band_mw`, and `serves_capstones`. That is not a reason to reject it — but the
  issue that exists to stop the project talking to itself should be the first one
  visibly written in the other register.
- **Ordering.** #571's boundary comment is right that templates without replies
  are the current failure mode; #567 alone raises arrival rate on a tracker whose
  measured behavior is bouncing arrivals. Cheap mitigation that needs nothing
  from #571: state the real latency honestly in `config.yml`/the form footer
  (single maintainer, first response usually within a week). Honest latency beats
  silence, and it is one line.

## What I would change in the acceptance criteria

Disregarding AC-1's "alongside — not replacing": that clause preserves the
mechanism the feature exists to remove. Replace with "the chooser presents at
most the two human templates plus contact links; planning templates move to
`docs/plan/templates/`."

AC-4 ("one drive-by-style bug report filed end-to-end") is a fine smoke test but
is not evidence — a maintainer filing a simulated stranger proves the form
submits. The load-bearing criterion is mechanical and checkable: **every required
field of the bug form can be filled using only what JLS itself hands the user**
(`--bug-report` output plus the on-screen error), with no reference to any repo
document. Capstone AC-1 is then satisfied by the first real external report,
whenever it arrives — which is what the capstone actually measures anyway.
