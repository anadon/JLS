# Issue #671: TASK-C265-5: macOS gets its advisory JDK-26 leg, so the next JDK breaks a lane before it breaks a release
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Stripped of the matrix mechanics, the claim is: *JLS should learn that the next JDK
broke it on macOS before a user does.* That claim is correct and worth serving. Almost
everything else in the task — the matrix leg, the PR trigger, the four acceptance
criteria — is a bad answer to it, and the bad answer is inherited from #265's roster
rather than derived from what macOS forward-compat risk actually looks like.

I am explicitly setting aside acceptance criteria 1 and 4, and the "advisory" wording
of criterion 2, for the reasons below.

## The signal this leg would carry is the one Linux already carries

`macos` (ci.yml:295-297) runs `mvn -B verify -Djacoco.skip=true` — no
`-Djls.test.headless=false`, unlike the Windows lane (ci.yml:244). So the macOS lane
today executes the *headless core suite only*. That is the most platform-independent
code in the repository: no Cocoa AWT peer, no Aqua LAF, no WindowServer, no
`java.awt.Robot`. A JDK-26 leg bolted onto it re-runs, on the scarcest runner class in
the fleet, almost exactly the byte-for-byte behaviours the Linux JDK-26 leg
(ci.yml:39-41) already covers.

The macOS-only JDK-26 risk lives precisely where this leg does not reach: the Cocoa
toolkit row of the README's supported-desktop matrix (README.md:173), the
`@Tag("display")` suite that #265 Stage 5 has not yet armed on macOS, and the
`macos-gui` WindowServer boot rig (ci.yml:594-598). #265 §6 asserts "Stages 7 and 8 are
mutually independent and parallel to everything." That is true mechanically and false
in value: **Stage 7 is worth roughly nothing before Stage 5, and a great deal after
it.** Landing it now buys a duplicate of the Linux signal at macOS runner prices.

Concrete alternative, if a single macOS JDK-26 probe must exist today: put it on
`macos-gui`, not on `macos`. Booting the real shaded jar on a real Quartz WindowServer
under JDK 26 is a forward-compat observation Linux structurally cannot make. Doubling
the headless suite is not.

## "Before it breaks a release" is backwards for macOS

Every `java-version:` in `release.yml` is 25 (lines 58, 142, 177, 340), and
`maven.compiler.release` is 25 (pom.xml:43). The macOS deliverable is a dmg with a
**bundled** JDK-25 runtime; README.md:15 says so explicitly ("self-contained installers
with a bundled Java runtime — no JDK needed"). A JDK-26 behaviour change therefore
*cannot* reach a dmg user until a maintainer bumps the bundled runtime. The
jar-on-your-own-JDK path (README.md:75) is the only exposed surface, and it is the
minority path on macOS.

So the value of JDK-26 knowledge is not smeared across every PR — it is concentrated
entirely on one future event: the 25→26 baseline flip. That reframing changes what
should be built.

## The reframing: one scheduled next-JDK workflow, not three matrix legs

ci.yml:3-6 states the nightly cron runs `gui-wayland` alone; `build`, `windows`, and
`macos` all carry `if: github.event_name != 'schedule'` (lines 30, 145, 261). The
result is inverted: the advisory forward-compat legs — the ones a reviewer is
*forbidden* to act on inside a PR, since they cannot block — fire on every PR, while
the fixed cadence where a maintainer would actually triage them skips them entirely.

The elegant cut is a separate `next-jdk.yml`, `schedule`-triggered, one matrix over
`os: [ubuntu-latest, windows-latest, macos-latest]` at the newest GA feature release,
running the full `verify` and opening/updating a single tracking issue on divergence.
That change:

- gives macOS the coverage #671 wants, with strictly more signal (it can arm the
  display suite and the boot rig on the JDK-26 leg without touching the gating lanes);
- **removes** the JDK-26 legs from `build` and `windows` too, cutting PR-path job count
  on three OSes rather than adding one;
- makes the forward-compat result a document someone reads, not a yellow check nobody
  clicks;
- matches the README's own singular promise — "an advisory (non-blocking) build on the
  newest GA feature release for early warning" (README.md:195-197), not one per OS.

This is a redirect of the whole Stage-7 family (#265 Stage 7, #111 Stage W5, and the
Linux matrix), which is why I file it as rethink rather than a local reframing.

## AC 2 hides a live landmine, and the repo already solved it

Linux encodes the rule *per leg*: `continue-on-error: ${{ matrix.java != 25 }}`
(ci.yml:41) — semantically "the non-baseline JDK is advisory". Windows and macOS use
job-level `continue-on-error: true` (ci.yml:156, 263), meaning "this whole lane is
still burning in". Adding a JDK-26 leg under job-level `true` makes the two reasons
indistinguishable.

At #265 Stage 3 promotion the DoD requires dropping `continue-on-error` on the macOS
job. The instant that happens, the JDK-26 leg becomes merge-blocking — violating #265
§4 invariant 1 and doing the exact thing #671's own Outcome sentence swears against
("a forward-compat leg must never be able to wedge the merge queue"). AC 2 as written
is satisfied by the cheap wrong construction. Windows already carries this landmine
armed and pending Stage W1.

The repository contains the correct precedent verbatim: `installer-reproducibility-
aarch64` is "Kept a separate job rather than a matrix leg of the gate above so the
x86_64 required check keeps its stable name" (ci.yml:915-916) — the same
byte-stable-name concern AC 2 raises, already solved by *not* using a matrix. A
permanently-advisory probe whose status is orthogonal to the lane's promotion state
should be its own job. Then AC 2 stops being a constraint to honour and becomes
structurally impossible to violate, and `Build (macOS, JDK 25)` never appears in a
matrix expression at all.

## AC 4 asks for the wrong thing

`~/.m2` content is a function of `pom.xml`, not of the JDK that downloaded it — the
artifacts are the same jars. `actions/setup-java`'s maven cache key does not vary by
`java-version`, so the Linux and Windows JDK 25/26 legs already share one key and have
done so without incident. "Keys correctly per JDK leg so the two legs do not evict each
other's caches" would *double* cache storage against the repo's shared 10 GB budget
(already carrying the pinned JBR and the 2026-07-26 oss-cad-suite bundle) to hold
byte-identical content. The real phenomenon is a benign concurrent-save race, not
eviction. If this criterion survives at all it should read: verify the second leg gets
a cache hit on the shared key; do not introduce per-leg keys.

## The stage doesn't fit the machine it was filed in

#265's whole architecture is promotion discipline: advisory → 20-run record → required.
Stage 7 is the only entry in that roster that can *never* be promoted — a forward-compat
leg is advisory by definition until 26 becomes LTS, at which point it stops being a
forward-compat leg and becomes the baseline. A stage that cannot enter the staging
machine is a tell that it belongs in a different container.

And the transition it should actually be designed for — the 25→26 baseline flip — is
unowned by any issue in the roster: ~20 `java-version: 25` sites across five workflows,
`maven.compiler.release`, five "any JDK 25+" rows in the README matrix, and the flake.
A single `JLS_BASELINE_JDK` / `JLS_NEXT_JDK` source of truth (workflow-level `env`, or
a `.github/jdk-versions.env` sourced by both CI and the release workflow) would be worth
more to this project's next five years than any per-OS advisory leg, and would make the
next-JDK workflow above a two-line change instead of a fourth copy of the same YAML.

## Cost #265 waves away

#265 §1 says public repos bill `macos-latest` at zero. True for billing, not for
*concurrency*: hosted macOS runners are the scarcest class, and each PR already queues
`macos`, `macos-gui`, and `macos-installer-reproducibility`. A fourth concurrent macOS
job lengthens the tail of every PR to buy a signal no reviewer is permitted to act on.
On the nightly it costs zero PR latency and is read by the one person who can act.

## What I would do instead

1. File the next-JDK workflow (scheduled, 3-OS matrix, issue-reporting) and close #265
   Stage 7, #111 Stage W5, and the Linux `java: [25, 26]` matrix into it.
2. File the `JLS_BASELINE_JDK` single-source-of-truth task; it is the real deliverable
   hiding behind all three Stage-7 siblings.
3. If a macOS JDK-26 leg lands on `ci.yml` regardless, make it a **separate job**
   (`Forward-compat (macOS, JDK 26)`), permanently `continue-on-error: true`, never a
   matrix leg of `macos` — and sequence it after #265 Stage 5 so it exercises the Cocoa
   surface that justifies its existence.

The Boundary section ("a real JDK-26 divergence is a finding to file, not something this
task fixes") is exactly right and should survive into whatever replaces this task.
