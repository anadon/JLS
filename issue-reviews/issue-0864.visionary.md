# Issue #864: TASK-C583-1: the Debian ITP process and what a bundled runtime obliges us to is written down concretely, with the per-cycle cost derived rather than asserted
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the ACs away and #864 is the project buying the right to say *no* to
Debian without that "no" reading as neglect. It is the investigation half of
#583, under CAP-34 (#518), whose kill criterion KC-34-2 has already decided the
outcome in advance: "PF-5's ITP proceeds only if a Debian sponsor materializes;
self-NMU maintenance at bus factor 1 is refused by name." So the arithmetic is
not being derived to *reach* a verdict — it is being derived so a future
maintainer can re-check the verdict cheaply instead of re-deriving it. That is a
legitimate and characteristic thing for this project to want:
`docs/standards-adoption/11-costed-rejections.md` opens with the same thesis in
its own words — "A rejection with no price on it is a shrug."

The goal is right. The object of the derivation is wrong, and so is the cost
driver it picks. Three reframes, in increasing order of how much they change.

## Reframe 1 — the cost model prices a package nobody would ever propose

AC-2 makes "the Policy obligations **the bundled runtime** triggers" the centre
of the analysis, with de-bundling relegated to a trailing clause. But no Debian
package of JLS would ever bundle a JRE; the archive has `default-jre`. Modelling
the ITP cost of a vendored-runtime package is modelling a package that would be
rejected at first glance, so the resulting maintainer-week number describes a
counterfactual. The real obligation surface is the **shaded jar**, and the
repository lets you size it exactly:

- `pom.xml:232` `maven-shade-plugin` bundles four runtime dependencies —
  `org.tukaani:xz:1.12`, `org.jfree:org.jfree.svg:5.0.7`,
  `com.formdev:flatlaf:3.7.2`, `org.jspecify:jspecify:1.0.1`.
- Their footprint in the source tree is startlingly small: `org.tukaani` is
  referenced from exactly one file (`src/jls/FileAbstractor.java`), `org.jfree`
  from exactly one (`src/jls/edit/CircuitRenderer.java`), jspecify is
  annotations only (no runtime need at all).
- **FlatLaf is already optional at runtime.**
  `JLSStart.installLookAndFeel()` (`src/jls/JLSStart.java:994-1016`) catches the
  failure, prints one `jls: warning:` line, and falls back to the
  cross-platform look-and-feel. A JLS built against a system JDK with FlatLaf
  absent starts and works today.

That reduces the actual Debian dependency closure to *one* mandatory library
(XZ, already in the archive as a Java binding) plus *one* feature-scoped library
(jfreesvg, gating `-i out.svg`), with FlatLaf demotable to `Recommends:`. The
honest headline of this investigation is therefore not "a vendored runtime makes
this impossible" — it is "de-bundling is nearly free, and the blocker is
somewhere else entirely." A document written to AC-2 as scoped would never
discover that, because it was told where to look.

**I am explicitly disregarding AC-2 as written.** Replace "the obligations the
bundled runtime triggers" with "the obligations the *shipped artifact set*
triggers, enumerated from `pom.xml` and the jlink/jpackage pipeline in
`scripts/build-installer.sh`," and require the document to state, per
dependency, whether it exists in the archive, is optional at runtime, or would
need its own ITP.

## Reframe 2 — maintainer-weeks is the wrong unit; cadence is the binding constraint

AC-3 and AC-4 spend the whole budget on maintainer-weeks per cycle and per
freeze. But maintainer-weeks are not what would hurt JLS in Debian. Debian
stable freezes and then ships the frozen version for roughly two years. What
lands in students' hands is a JLS pinned to a point in time — while
`docs/capability-roadmap/` carries three keystones, eight long-form features and
six sweeps of active change, and the Java floor deliberately tracks the current
LTS. The obligation Debian creates is not "N weeks of packaging work"; it is
"support a two-year-old fork of yourself, in a course context, without being
able to ship the fix." That is a *qualitative* obligation the maintainer-week
table cannot express, and it is the argument that would actually convince a
reader.

Reframed as a question the project can answer once and reuse: **what is the
maximum artifact staleness JLS tolerates on a distribution channel?** Pick a
number (say: median shipped version older than one minor release, or older than
N months, is declined). Then Debian, Flathub, winget, Homebrew, nixpkgs and a
PPA all resolve mechanically against one policy line instead of six bespoke
investigations. This is exactly the house pattern already in use — the "revisit
triggers" convention in `ARCHITECTURE.md` § Recorded decisions, and KC-34-1's
">0.5 mw per release cycle is dropped" line in #518 — generalised one notch. A
staleness policy plus KC-34-1's cost line together *are* the channel decision
procedure; #864 as chartered writes one channel's homework instead of the rule.

## Reframe 3 — the deliverable already has a home, and part of it is already written

#864 says "a committed document" without saying where, and #583 does the same.
Two in-tree homes already exist and using either costs less than a new file:

- **`docs/standards-adoption/11-costed-rejections.md`** is the genre, verbatim:
  four entries, six fixed subheadings each, each pricing a rejection and naming
  what would reopen it. Debian archive inclusion belongs there as entry 5, and
  inherits the template — including the "what would change the answer" habit
  that #583's AC-5 asks for as if it were new.
- **`docs/standards-adoption/10-desktop-and-housekeeping.md:66-74`** already
  contains the single most decisive finding of the investigation #864 charters:
  JLS's deb installs into `/opt/jls` (`resources/packaging/resource-dir-linux/postinst`),
  "which is fine for a third-party package but is not the layout Debian or
  Fedora archives accept, so archive inclusion is not on the table either." The
  same file already works a *recorded decline* end to end for Flathub, with the
  reasoning style #864 wants. Neither #583 nor #864 cites it. This is the
  clearest duplication risk in the chain, and it also means part of AC-1/AC-2 is
  answerable by reference rather than by research.

A third, higher-value output nobody has asked for: `docs/standards-landscape.md`
line 467 marks entry 177 **Debian Policy Manual — HAVE**. That is not true in
the sense a reader will take it. JLS emits a `dpkg`-installable `.deb`; it does
not conform to Debian Policy, and the `/opt` layout above proves it. Correcting
that row — HAVE → "installable artifact only; archive conformance declined,
see 11-costed-rejections §5" — is worth more to the project's honesty than the
maintainer-week table, and costs a line.

## The out-of-the-box alternative the issue never considers

Because de-bundling turns out to be cheap (Reframe 1), there is a route that
makes the sponsorship problem dissolve rather than get answered: **ship the
packaging, not the package**. A `contrib/debian/` skeleton plus a one-page
`docs/packaging-for-distributions.md` (build from the tagged tarball with a
system JDK 25+, jar is the product, here are the `.desktop`/MIME/AppStream/icon
assets, here is what is deliberately not shipped) converts JLS from "a project
asking Debian for a favour" into "a project a downstream packager can adopt in
an evening" — and it serves nixpkgs, an Ubuntu PPA, Fedora and Debian at once.
`flake.nix` already exists as the worked de-bundled example. This is also the
only thing that plausibly attracts the sponsor KC-34-2 demands, since sponsors
adopt nearly-finished packages, not invitations. (#865's visionary review
reaches the same readiness-first conclusion from the sponsor side; the two
halves agree, which is worth noting because it means the reframe survives being
approached from either end of #583.)

One tension to name honestly rather than route around: the "just host an apt
repo" shortcut is *not* freely available here. A signed `Release` file needs a
project-held key, and #136 declined exactly that custody for the rpm and
AppImage. Any cost document that proposes a self-hosted repo must either cite
#136 and argue the case changed, or drop the option.

## Where this leaves the issue

Endorse the question, endorse the 0.5–1 mw band, endorse writing it down. Change
three things: derive the cost of the package that would actually be proposed (a
de-bundled, system-JDK build with a two-library closure), make cadence/staleness
rather than maintainer-weeks the load-bearing variable and state it as a reusable
project-wide policy, and land the result as entry 5 of
`11-costed-rejections.md` — citing, not re-deriving,
`10-desktop-and-housekeeping.md`'s `/opt` finding — while fixing the misleading
"HAVE" on standards-landscape entry 177. Done that way this task strengthens
CAP-34's whole channel roster instead of finishing one optional square on it,
and it produces a rule the next channel question can be decided by rather than a
document about Debian.
