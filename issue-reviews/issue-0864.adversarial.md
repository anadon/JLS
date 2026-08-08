# Issue #864: TASK-C583-1: the Debian ITP process and what a bundled runtime obliges us to is written down concretely, with the per-cycle cost derived rather than asserted
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually asks for

The investigation half of FEAT-C34-5 (#583, optional PF-5 of CAP-34/#518):
a committed, document-only deliverable that (1) writes out the Debian
Intent-To-Package steps and who does each, (2) names the Policy
obligations JLS's bundled runtime triggers with citations, (3) derives a
maintainer-week cost per release cycle and per freeze, and (4) separates
onboarding cost from steady-state cost. AC-5 explicitly forbids touching
`scripts/build-installer.sh` or the shipped deb. The companion issue #865
(TASK-C583-2, already reviewed in this repo at
`issue-reviews/issue-0865.adversarial.md`) is the decision half, gated on
a named sponsor per KC-34-2 in #518; that chain is internally consistent
and not re-litigated here. #864's own AC-5 boundary against #443
(TASK-0027) and #338 (FEAT-010) is verified correct: both of those issues
own installer-matrix gating and reproducibility, neither touches the
Debian question, so there is no overlap or contradiction there.

## Findings, most severe first

### 1. AC-2 scopes "Policy obligations" to the runtime only, but the real embedded-code surface is bigger — and the issue's own wording knows it (HIGH)
AC-2: "The Policy obligations *the bundled runtime* triggers are named
specifically... including what a de-bundled build would require instead."
The Outcome paragraph, by contrast, names the general category as
"vendored-code and embedded-library rules chief among them" — plural,
not runtime-specific. `pom.xml:232-238` (`maven-shade-plugin`, goal
`shade`) confirms the built jar is a fat jar that bundles three
third-party libraries into the JLS artifact: `org.tukaani:xz:1.12`
(`pom.xml:62-68`), `org.jfree:org.jfree.svg:5.0.7` (`:69-79`), and
`com.formdev:flatlaf:3.7.2` (`:80-91`) — the last carrying its own
comment noting it bundles native FlatLaf resources. A real Debian
package would need each of these either already present in the Debian
archive as a Java library package (`libtukaani-xz-java` or similar,
`libjfreesvg-java`, a FlatLaf package) that JLS builds against, or its
own separate ITP — on top of the jlink-trimmed JRE the parenthetical
already flags. As literally scoped, AC-2 is satisfied by a document that
discusses only the bundled JVM runtime and says nothing about the three
shaded libraries, which would understate the true obligation surface and
therefore the cost arithmetic AC-3 is supposed to derive from it.
Recommendation: reword AC-2 to require enumerating every embedded
component the shade/jlink pipeline produces (query `pom.xml`'s
dependency list plus the shade plugin config, not just the runtime), not
only the runtime.

### 2. AC-2's "citations to the relevant Policy sections" assumes a tidier source landscape than Debian's, and is gameable as worded (MEDIUM)
Nothing in AC-2 requires the cited section numbers to be checked against
the actual current text of Debian Policy — a plausible-sounding but wrong
or stale citation satisfies the letter ("citations... are named") as
written. Compounding this: Debian's actual guidance on bundled JVM
runtimes and embedded code copies is not concentrated in one clean,
citable "Policy section." It spans Debian Policy §4.13 ("Embedded code
copies"), the separately-maintained Debian Java Policy document (not
part of the Policy Manual proper), and ftp-master rejection precedent
(the REJECT-FAQ) for what counts as an acceptable embedded copy — none of
which the issue's phrasing ("citations to the relevant Policy sections")
anticipates as anything other than a single tidy source. A document that
cites only Policy §4.13 and calls it done would technically satisfy AC-2
while missing the Java-specific practice that actually governs whether
Debian's Java team would accept a bundled-runtime desktop app.
Recommendation: name the source set explicitly in AC-2 (Policy Manual +
Debian Java Policy + current ftp-master practice) rather than leaving
"the relevant Policy sections" open to a single-document reading, and
require citations to be checked against the live text at time of writing
(with the check date recorded, the way other docs in this repo — e.g.
`docs/dmg-reproducibility.md`, `docs/OPEN-QUESTIONS.md` — record
verification dates for external claims).

### 3. AC-3's "showing the arithmetic" has no evidentiary floor for the input assumptions (MEDIUM)
"Estimated in maintainer-weeks... with the derivation shown (steps,
assumed durations, frequency) rather than a bare total." This closes the
easy gaming vector (a bare number) but opens a subtler one: an internally
consistent set of *invented* assumed durations ("uploading a package
takes 2 hours," "a freeze review takes 1 day") satisfies "derivation
shown" without being grounded in anything real. Nothing requires anchoring
the estimate to an observed comparator — e.g., a real single/small-team
Debian package's actual per-freeze workload, or the Debian
`wnpp`/`mentors` process's documented typical timelines. Recommendation:
require at least one cited external comparator (a real package's
maintenance history, or Debian's own published median times for
sponsorship/upload/freeze review) so a reader can sanity-check the
per-step duration assumptions, not just re-multiply them.

### 4. "Per release cycle" has no fixed cadence to derive against, in this project (LOW-MEDIUM)
AC-3 asks for cost "per release cycle" but JLS itself has no documented
release cadence — README.md and CHANGELOG.md describe *what* a release
contains, not how often one is cut; no cadence word ("monthly,"
"quarterly," "on tag push only") appears in either file or in
`docs/reproducibility.md`. Since Debian's own freeze/upload cost is
partly a function of how often JLS would need a fresh upload, an
undefined release cadence makes "per release cycle" an assumption the
document must invent rather than one it can cite. This isn't fatal — the
author can simply state an assumed cadence — but the issue doesn't ask
for that assumption to be flagged as an assumption, so a reader could
mistake an invented cadence for a documented project fact.
Recommendation: AC-3 should require the assumed JLS release cadence to be
stated explicitly and flagged as an assumption if none is documented
elsewhere in the repo.

### 5. AC-1's "who performs it" will trivially collapse to "the maintainer" for nearly every step, which is fine but not remarked on (LOW)
At bus factor 1 (confirmed by ARCHITECTURE.md and the project's own
recorded single-maintainer stance, independently corroborated by
#443/#338's "bus factor 1" framing), essentially every ITP step —
filing, packaging, upload, ongoing maintenance — has only one candidate
performer except sponsorship, which #865 owns separately. AC-1 as written
will produce a document where four of five step categories name the same
person, which is a correct and useful finding for #865's decision, but
the AC doesn't ask the document to say so explicitly (i.e., "this is not
a staffing plan with multiple owners, it's one person's calendar"). Minor
wording addition would make the concentration of effort visible rather
than implicit in a role column that keeps repeating the same name.

## Things that are solid

- The #864/#865 split (investigation vs. decision) and the arithmetic
  that #583's 1-2 mw budget equals #864's 0.5-1 plus #865's 0.5-1 is
  correct and not double-counted.
- AC-5's boundary against #443/#338 is accurate: both of those issues
  verifiably own installer-matrix promotion and reproducibility/signing,
  not the Debian ITP question, so "this task produces a document only"
  doesn't conflict with any other open issue's scope.
- The core premise — that the deb already builds (README.md's "Installing
  JLS" section, `scripts/build-installer.sh` per #443) but that building
  is a different question from carrying Debian's ongoing maintenance
  obligation at bus factor 1 — is factually grounded, not asserted; the
  jpackage-produced deb genuinely does bundle a jlink-trimmed runtime
  (README's "self-contained installers with a bundled Java runtime"),
  which is exactly the trigger AC-2 is investigating.
