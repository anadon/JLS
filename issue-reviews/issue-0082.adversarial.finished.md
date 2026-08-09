# Issue #82: Distribution: jpackage installers per OS and .jls file association — remove the bring-your-own-JDK barrier
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Read the issue body, all 10 comments, sub-issues #284 and #285 (both open,
correctly attached), and issue #134 and #443 that #82's comments cite.
Cross-checked citations against the checked-out tree: `README.md`,
`scripts/build-installer.sh`, `.github/workflows/release.yml`,
`src/jls/JLSStart.java`, `pom.xml`, `resources/packaging/`. The great
majority of file:line citations in the issue body and #284/#285 resolve
correctly against the current tree (`JLSStart.java:829`/`:104`,
`build-installer.sh`'s JBR block at ~289-345, the `--file-associations`
wiring, the deb smoke test's `%f`/`xdg-mime` assertions, the
`experimental:`/`continue-on-error` flags in `release.yml:297-312`). That
part of the discipline is real and worth crediting. The findings below are
about the things that don't hold up.

## Findings, most severe first

### 1. Two independent, uncoordinated completion paths for the same residual work (HIGH)

The issue's own machine block declares `requires_tasks: [284, 285]` and its
mermaid graph shows only `T284 --> F82` and `T285 --> F82`. But issue **#443**
(filed under a *different* parent, `#338`, tier `task`, "TASK-0027") states
in its own body: *"**#82** — **Closes** — this task is exactly its residual:
§5 P1-P4, §7's manual matrix, §10's uninstall threat"* and lists *"#82 closed
against the recorded run"* in its own Definition of Done. #443's §8 Method
contains, item-for-item, the same two actions #284 and #285 exist to do
(arm the JBR sha256 pins, flip the four `experimental: true` flags). #82's
own most recent comment (2026-08-04, "pass 2") *admits* this: *"#284, #285
and #443 are now a genuine three-way overlap on one body of work... a human
should choose."* Yet nothing in #82's body — not the YAML block, not the
mermaid graph, not the Completion Criteria — was updated to reflect that a
decision is pending or to say which path is authoritative. As written today,
whoever picks up #82 next has no way to tell from the issue itself whether
landing #284+#285 or landing #443 is what closes it, and two independent
efforts could race to close the same tracked scope, or #82 could be closed
by #443 while #284/#285 sit open forever as orphaned duplicates (or vice
versa). **Recommendation:** before any further work proceeds, #82 needs an
explicit `REPLAN:` that either retargets `requires_tasks` at `[443]` and
closes #284/#285 as duplicates, or formally supersedes #443's claim on this
scope — not another "escalated, a human should choose" comment.

### 2. The stated capability ("double-click a .jls file, it opens") has no CI regression guard on 2 of 3 desktop OSes (HIGH)

The feature's headline claim, restated in §1 Capability Statement, is that a
user "double-clicks a `.jls` file to open it in the editor" on every
supported OS. Checked `release.yml`'s three "Install and smoke-test the
installer" steps directly:
- Linux (lines ~453-470): installs the deb, then `grep -q '^Exec=.*%f'
  /opt/jls/lib/jls-JLS.desktop` and `xdg-mime query default
  application/x-jls-circuit` — genuinely asserts the association.
- Windows (lines ~490-508): `msiexec /i`, finds `JLS.exe`, runs `JLS.exe -h`,
  `msiexec /x`. **No association check at all.**
- macOS (lines ~513-524): `hdiutil attach`, copies the `.app`, runs
  `Contents/MacOS/JLS -h`. **No association check at all.**

So the feature-level integration criterion I2 ("the per-OS install → launch
→ double-click-`.jls` matrix is fully green") is, for Windows and macOS,
never actually exercised by CI — only by the one-time manual check #284's
P2 defers to a human on real hardware, with no mechanism to re-run it.
Once #284 closes, a future regression in `jls-association-windows.properties`
or `jls-association-macos.properties` (or in how jpackage consumes them)
would ship silently forever; nothing in the release pipeline would go red.
Issue #443 independently reaches the identical conclusion — *"Only the deb
leg checks the mime association... Neither [msi nor dmg] asserts the
property README claims"* — which shows this gap is already known
elsewhere in the tracker, but #82's own children (#284/#285) and its own
Completion Criteria do **not** require closing it; that work is scoped only
to #443, reinforcing finding #1's ambiguity about what actually finishes #82.
**Recommendation:** #82's Completion Criteria (or #284's DoD) should
require a durable, non-interactive association assertion on Windows/macOS,
not accept a one-off manual click-through as satisfying I2 permanently.

### 3. README currently states a signing claim #82's own cited sibling calls a "live documentation defect" (MEDIUM)

`README.md:31-33` (checked in the tree) states as present tense: "The
installers are Authenticode-signed through SignPath.io's open-source
program, so the publisher shown by Windows is **SignPath Foundation**."
#82's own §1 Scope Boundary says code signing is "#134 (open; maintainer
enrollment remains)." Issue #134 itself, fetched directly, states: *"every
downloadable msi is unsigned while README.md already asserts present-tense
signing — a live documentation defect,"* and its own Observation 3 confirms
zero signed msi exists (`osslsigncode verify` → "MSI file has no
signature"). #82's Global Invariant 5 claims "the signing stance stays
documented and truthful in the README as it changes" — but the README is
*already* untruthful today, at #82's own pinned evidence_commit. #82 is not
`blocked_by` #134, and its Completion Criteria only checks "README Install
section verified accurate against the shipped asset set at close" in
general terms — nothing forces the closer to notice this specific,
already-flagged-elsewhere false claim. #82 could close while the README
still overstates Windows signing status.

### 4. Landed-evidence script's own stated JDK requirement is wrong for the default path (MEDIUM)

`scripts/build-installer.sh`'s header (cited by #82 as landed evidence, and
copied verbatim into #285's Materials & Apparatus) says: *"Requirements:
JDK 17+ on PATH (jdeps/jlink/jpackage ship with the JDK; jpackage is final
since JDK 16, JEP 392)."* But `pom.xml:43` pins
`<maven.compiler.release>25</maven.compiler.release>`, and the script's
default path runs `mvn package` unless `JLS_SKIP_BUILD=1` is set. `mvn
package` on a bare JDK 17 will fail outright — `--release 25` is not a
valid target before JDK 25 exists. The "JDK 17+" claim is true only for the
jpackage/jlink/jdeps *tooling* floor, not for actually running the script
as documented. Anyone following the script's own stated requirements
without already having a JDK 25+ around (or a pre-built jar) hits a build
failure the issue's "Landed before this decomposition" section never
flags as a caveat.

### 5. The ARM manual-verification waiver is pre-decided before anyone tried the alternative (LOW-MEDIUM)

§ Open Questions recommends, as the default, waiving the manual
double-click check for `ubuntu-24.04-arm` and `windows-11-arm` "no ARM
hardware available." But the release matrix already runs those exact OSes
as real GitHub-hosted VMs for the build step — an interactive session on
the same runner class (e.g. via a debug/tmate-style action) or asking
whether the maintainer has any ARM hardware at all isn't discussed before
recommending the waiver. Presenting "waive it" as the *recommended default*
before that's been tried invites a permanent, rubber-stamped verification
gap on two of five installer legs, precisely the kind of `WAIVED:` the
issue's own Completion Criteria says must "name its successor" rather than
just assert impossibility.

### 6. Process overhead is disproportionate to the remaining engineering content (LOW)

Ten essay-length comments (dependency YAML blocks, mermaid graphs,
"adjudication" ledgers, multiple "REPLAN" corrections, a two-part
"boundary note"/"correction to boundary note" covering nine other issues)
sit on top of a feature whose actual remaining work is: run a workflow
dry-run, click through an installer twice, and paste two sha256 hashes.
This is not merely a style complaint — finding #1 (the #443 duplication)
is a direct, observed consequence of this much cross-referenced bookkeeping
outrunning anyone's ability to keep it internally consistent. The tracking
apparatus has itself become a source of the exact "dangling owner" failure
mode its own Re-planning Protocol says it exists to prevent.

## What's solid (no rework needed)

- The core scope boundary (RISC-V excluded for a real cross-compilation
  reason, byte-reproducibility deferred to #188, auto-update explicitly
  never) is coherent and matches the code.
- The macOS-unsigned-by-choice stance is honestly stated and correctly
  tied to closed decisions (#128/#135), and the README's Gatekeeper
  instructions match that stance.
- File/line citations for the JBR pin block, the `%f` fix, and
  `JLSStart.parseCommandLine`/`startFile` all resolve correctly at HEAD —
  the "double-click contract" description in §3 is accurate to the code.
- The per-OS narrowing / falsification design (a failing OS ships
  jar-only, documented) is a reasonable, genuinely falsifiable acceptance
  discipline, in contrast to the vaguer waivers noted above.
