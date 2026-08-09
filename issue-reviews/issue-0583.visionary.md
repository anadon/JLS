# Issue #583: FEAT-C34-5: the question "should JLS be a real Debian package?" gets a written go/no-go with its maintenance arithmetic, and "no, with reasons" is a legitimate answer
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not Debian. The deliverable is *closure* — retiring a question that will otherwise
resurface every time someone notices the deb exists and asks why it is not in the
archive. The issue says this out loud ("a recorded 'no, with reasons' closes this
feature as completed"), and that instinct is exactly right and worth defending
against anyone who reads it as low-value paperwork. A project at bus factor 1 is
made or broken by its ability to convert recurring open questions into recorded,
dated, re-checkable answers, and this repo has already built the machinery for
that: `ARCHITECTURE.md` § "Recorded decisions" (7 entries), and
`docs/standards-adoption/11-costed-rejections.md`, whose opening line —
"A rejection with no price on it is a shrug" — is the thesis of #583 stated a
month earlier.

That is the first problem. The answer already exists in-tree, at lower resolution,
and #583 does not cite it.

## The answer is already recorded — and a sibling PF contradicts the same document

`docs/standards-adoption/10-desktop-and-housekeeping.md` (lines 67–76) states:

> JLS's deb installs into `/opt/jls` (see
> `resources/packaging/resource-dir-linux/postinst`), which is fine for a
> third-party package but is not the layout Debian or Fedora archives accept, so
> archive inclusion is not on the table either.

Confirmed against the tree: `postinst` lines 28–36 reference `/opt/jls/lib/...`.
So the project's own playbook has already answered #583 "no" on FHS grounds. #583
as written will re-derive that from zero, and — because it never names §10 —
could land contradicting it.

Worse, the sibling feature under the same capstone does contradict it. §10 line 17:
"**Do not** pursue Flathub", with three costed reasons (second packaging pipeline
with none of the `SOURCE_DATE_EPOCH` plumbing carrying over; sandbox hostile to
`<circuit>.jls~` sibling-file recovery and to `flatpak run`-prefixed batch
invocations that break every command line in `README.md` and
`docs/batch-interface.md`; contradicts the recorded single-jar deployment model).
CAP-34 PF-1 (#579, with tasks #849/#852/#853/#854) ships Flathub anyway, and #852
is literally "double-clicking a .jls file in a Flatpak install opens JLS —
portal-mediated association", i.e. the exact objection §10 raised, re-encountered
as a task. CAP-34 was filed from #508/#510 without reconciling against the
standards-adoption playbook.

**The highest-value thing #583 can do is not answer the Debian question.** It is to
be the issue that forces that reconciliation: one distribution-posture document
that either supersedes §10 with dated reasons or defers to it, so the project stops
holding two positions on the same subject. That is a reframing of scope, not a
reduction of it.

## The arithmetic AC-2 will miss

AC-1 names "the packaging-policy obligations JLS's bundled runtime would trigger".
The bundled JRE is the visible blocker and the smaller one. The larger one is the
shaded fat jar. `pom.xml` shades four runtime dependencies —
`org.tukaani:xz`, `org.jfree:org.jfree.svg`, `com.formdev:flatlaf`,
`org.jspecify:jspecify` — and Debian forbids vendored/embedded copies. Archive
inclusion therefore means: unshade the jar, resolve against system jars, and for
every dependency not already in the archive, *package and maintain that library
too*. FlatLaf and JSpecify are the likely gaps. A maintainer-weeks estimate that
counts only JLS understates by a multiplier, and the multiplier is recurring, not
one-time — each of those libraries inherits the same per-release and per-freeze
obligation. Any go/no-go that omits this line is not arithmetic, it is a mood.

Add: `/opt` → FHS relayout (so the deb CI builds and the archive build diverge
permanently, doubling the packaging surface `scripts/build-installer.sh` owns),
`maven-repo-helper`/`javahelper` idiom, and the Debian Java team's own capacity.

## AC-3 is ordered backwards

AC-3 makes the verdict depend on a named sponsor. Searching debian-mentors for a
sponsor *before* establishing that the package is policy-admissible spends a week
on something that cannot be accepted regardless. Invert it: admissibility first
(deps in archive? FHS layout? no bundled runtime?), and if any leg fails, the
sponsor question is moot and never needs asking. KC-34-2 is still honored — it
says the ITP proceeds only if a sponsor exists, not that a sponsor must be hunted
before the cheaper disqualifier is checked.

## The reframing I would actually build

**Stop asking "should JLS be the Debian maintainer?" Ask "is JLS a good upstream?"**

Debian separates upstream from maintainer; #583 fuses them and then correctly
recoils from the fused obligation. The recurring cost the issue fears — per
release, per freeze, per security NMU — is the *maintainer's*. Upstream's
obligation is small, one-time, and non-recurring:

- a release tarball that builds without network against system dependencies
  (an unshaded profile, not a new build system),
- no bundled runtime on that path,
- FHS-respecting install targets available as a build option,
- AppStream metainfo, `.desktop`, MIME type, man page, machine-readable licensing,
- SemVer + a changelog a packager can read.

JLS already ships the desktop entry and `application/x-jls-circuit`
(`resources/packaging/resource-dir-linux/JLS.desktop`), SemVer, `CHANGELOG.md`, a
byte-reproducible jar with a `.buildinfo`, and a CycloneDX BOM; §10 already specs
the metainfo file and notes its real payoff is precisely "if the project is ever
packaged by a third party (nixpkgs, an Ubuntu PPA, a distro maintainer), the
metainfo already exists and is correct." The gap is the unshaded build path and the
FHS option — call it 0.5–1 mw, once.

That work answers Debian, Fedora, openSUSE, Guix, Arch/AUR, and any future
downstream *simultaneously*, costs nothing recurring, needs no sponsor, and cannot
be revoked by one person losing interest. It is strictly stronger than a document
that says no, because it makes "yes" available to anyone who wants to carry it
without JLS carrying anything. **I would file the deliverable as a
"downstream packaging contract" (a short `docs/` page plus a `-Ppackager` Maven
profile), with the Debian go/no-go as one worked example inside it.**

## The other thing this document should decide

The README already observes that "Debian tooling verifies signed *repository*
metadata rather than individual `.deb` files, so the deb has never carried an
embedded signature either." The concrete institutional need behind "be a Debian
package" is usually a lab admin deploying to 200 machines from config management —
which an **apt repository over the existing deb** serves completely, with no ITP,
no sponsor, no policy compliance, and no freeze cycle. That route is never
considered in #583.

It has one honest collision: an apt repo needs an OpenPGP key, and `SECURITY.md`
§ "Release artifact signing & verification" (#136) refuses a project-held key at
bus factor 1 — *except* where "a concrete downstream requirement" justifies one
"scoped to that requirement". So the most useful sentence this document can
contain is not about Debian at all: it is the named trigger for #136's own
exception clause. "If an identified institution deploys JLS via apt at scale, that
is the concrete downstream requirement #136 contemplates; we generate a key scoped
to that repository and nothing else." That is a far better AC-5 ("what would change
the answer") than "a sponsor appears", because it is a condition the project can
recognize when it arrives rather than one it must go looking for.

## Verdict and disposition

Endorse-with-reframing. Keep the feature; keep "no, with reasons" as a legitimate
outcome; keep the band. Change four things:

1. **Cite and reconcile `docs/standards-adoption/10-desktop-and-housekeeping.md`.**
   The `/opt/jls` finding is the existing answer; the document either upgrades it or
   defers to it. Reconciling PF-1's Flathub work with §10's "recommend no" belongs
   in the same pass — file it against #579, but #583 is where the conflict is
   visible.
2. **Site the decision in an existing home** — `ARCHITECTURE.md` § "Recorded
   decisions" or a `docs/standards-adoption/` section — not a new orphan file.
   AC-5's "re-check rather than re-derivation" only works if the next person can
   find it, and this repo has 30+ `docs/` files already.
3. **Reorder AC-3 behind admissibility**, and expand AC-2's derivation to include
   the unshading/dependency-packaging multiplier, not just the bundled runtime.
4. **Widen the outcome from "Debian ITP?" to "what does JLS owe a downstream
   packager, and what will it never owe?"** — with the packageability contract as
   the deliverable and the Debian verdict as its first worked example.

I am not disregarding the acceptance criteria; every one of AC-1…AC-5 survives
inside the wider frame, and AC-4's "if go, file follow-on work separately" becomes
easier to honor because the follow-on is a build profile rather than an
open-ended maintainership.
