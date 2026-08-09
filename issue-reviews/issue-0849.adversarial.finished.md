# Issue #849: TASK-C579-1: a Flatpak manifest wraps the published, attested Linux artifact by checksum, and the built app launches and opens a .jls file
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#849 is TASK-C579-1, the first task in the chain CAP-34 (#518) → FEAT-C34-1
(#579) → **#849** → #852 → #853 → #854, and its outcome/AC set is a
reasonably tight scope: wrap a published, checksummed Linux release asset
in a Flatpak manifest, no rebuild, no unpinned download. But two problems
sit upstream of the code: the project's own in-tree standards analysis
already recommends against this exact channel for reasons #849 never
engages, and its own acceptance criteria are loose enough to be satisfied
without proving the thing that actually matters (a file opened from a
realistic host location).

## Findings, most severe first

**1. (Critical) The whole Flathub effort — of which #849 is the first
concrete step — contradicts the project's own recorded standards
analysis, and #849 does not acknowledge it.**
`docs/standards-adoption/10-desktop-and-housekeeping.md:81-99` is headed
"**Flathub: recommend no.**" and gives four reasons, the sharpest being:
"The sandbox is hostile to the tool's actual job. JLS is a file editor
whose recovery story writes `<circuit>.jls~` next to the user's file
(`src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`)
… Under Flatpak that means `--filesystem=home` (which reviewers push back
on) and a `flatpak run` invocation prefix that breaks every command line
in `README.md` and `docs/batch-interface.md`." Line 99 also names a
concrete blocker #849 doesn't touch ("Screenshots must be HTTPS-reachable
at Flathub build time"). The same doc instructs: "Record the decline the
way `ARCHITECTURE.md` § 'Recorded decisions' records the others" — but
`ARCHITECTURE.md`'s "Recorded decisions" section has no Flathub entry (I
read it in full), so the analysis exists in-tree, unrated and
unoverturned, and #849 proceeds as though it doesn't exist. #849's AC-5
("nothing here re-litigates #338/#443's installer matrix… this manifest
consumes their output") is careful about one kind of prior decision
(the installer matrix) while being silent about a much more consequential
one (whether to build this channel at all). This is not a hypothetical
concern I'm inventing for this review: the adversarial review of #852 —
the very next task in this chain — independently found the identical
conflict and rated #852 needs-rework on it. #849 is the earlier task and
the more natural place to resolve or explicitly overturn the standing
"no," since every downstream task inherits whatever #849 leaves unsettled.
**Recommendation:** before doing #849's manifest work, get #579 (or #518)
to either write an explicit `ARCHITECTURE.md` reversal that answers the
`.jls~`/sandbox objection, or shelve the Flathub chain. Filing code
against a standing "don't do this" without a recorded reversal is how a
maintainer ends up reviewing a manifest whose entire premise is disputed
in the same tree.

**2. (High) AC-2's verification is gameable: it never specifies sandbox
filesystem permissions, so it can pass while the realistic case (opening
a file from the user's actual home directory) is untested.**
AC-2 reads: "`flatpak-builder` produces an app that launches on a clean
system and opens a `.jls` file passed on the command line." The Outcome
paragraph repeats this almost verbatim: "Built locally, the resulting app
launches and opens a `.jls` file." Neither states where that file must
live, nor what `finish-args`/`--filesystem=` grant the manifest carries.
A verifier can satisfy AC-2 by placing the test `.jls` inside a path the
sandbox already has access to by default (e.g. bundled under `/app`, or
whatever Flatpak's default document-portal auto-grant covers) without
ever testing the case that matters pedagogically — a student's circuit
file sitting in `~/Documents` or wherever the assignment was downloaded.
Since #852's own adversarial review shows the codebase's `JFileChooser`
dialogs don't speak the document portal at all (`src/jls/edit/Editor.java:104,155`),
whether *this* manifest's permission story is even compatible with
realistic file locations is exactly the open question AC-2 should force
and currently doesn't.
**Recommendation:** name the `finish-args` the manifest grants as part of
AC-2 (or a new AC), and require the launch test to pass a file living
outside `/app` and outside any bundled sandbox-default path — e.g. a file
freshly created under the test harness's simulated `$HOME`.

**3. (High) AC-1/AC-4's premise — "the existing bundled-runtime Linux
build" — currently rests on an artifact whose own release pipeline
doesn't yet fail on breakage for one of the two named architectures.**
#443 (TASK-0027, open, `ordering_after: []` on #849) documents that at
its evidence commit the Linux aarch64 installer leg
(`ubuntu-24.04-arm`) carries `experimental: true` /
`continue-on-error: true` in `.github/workflows/release.yml` — only the
x86_64 leg (`ubuntu-latest`) is a required check. That means a broken or
silently-degraded aarch64 Linux release asset (deb/AppImage) can ship
today without failing CI — precisely the failure mode #443 exists to
close. #849's AC-4 says an architecture is declared "only if the
underlying release asset exists for it," but existence and CI-verified
integrity are different properties, and #849's `ordering_after: []`
means it isn't sequenced behind #443 landing. If #849's manifest declares
`aarch64` support before #443 promotes that leg to required, "wraps the
existing bundled-runtime Linux build" is wrapping an artifact this
project's own residual-tracking issue calls unverified.
**Recommendation:** either restrict AC-4 to x86_64 until #443 lands (the
only currently-required Linux leg), or add #443 to `ordering_after` and
say so explicitly rather than leaving the dependency implicit.

**4. (Medium) AC-1 doesn't say which release asset (`.deb` vs
`.AppImage`) the manifest wraps, and the two require materially
different extraction — an ambiguity #852 already has to assume an answer
to.**
The deb ships a `postinst` (`resources/packaging/resource-dir-linux/postinst`)
that runs host-level `xdg-mime install`/desktop-database updates on
package install — side effects that don't execute inside
`flatpak-builder`'s build sandbox and would need to be replicated
manually in the manifest's `install` phase if the deb is the source. The
AppImage instead needs `--appimage-extract-and-run` (or equivalent
extraction) to get at the payload, with no `postinst` to reason about at
all. #849 leaves the choice open, yet #852's AC-1/AC-2 assume the
manifest can independently "declare" the desktop entry/MIME/icon,
implicitly picking one extraction path without #849 having committed to
it (#852's own review flags this same gap from the other side).
**Recommendation:** name the source asset (deb or AppImage) explicitly in
AC-1, so #852 isn't inferring it.

**5. (Medium) AC-1's "no rebuild from source inside the manifest" is a
real technical constraint that a genuine Flathub submission is likely to
reject outright, and #849 states it as settled rather than as a risk.**
Flathub review commonly requires FOSS applications to build from source
inside the manifest; wrapping a pre-built third-party binary via a
`bin`/`extra-data`-style module is the pattern reserved for proprietary
software that cannot legally be rebuilt, which JLS (GPLv3, Maven-buildable)
is not. AC-1's justification is provenance ("the installed bytes trace
back to the same attested artifact"), which is a real and legitimate goal,
but it is in tension with Flathub's normal build-from-source expectation,
and #849 doesn't flag that tension — it will surface later, expensively,
at #853/#854 (store submission) rather than now when the manifest's shape
is still being decided.
**Recommendation:** add one sentence acknowledging the provenance-vs-build-from-source
tension and naming which side wins if Flathub review pushes back (e.g.
"ship the binary-wrapped manifest to a self-hosted repo if Flathub review
rejects it" — a real fallback, since #849's own manifest doesn't require
that the *Flathub* submission be blessed, only #853/#854 do).

## What's solid

- AC-3 ("the sha256 in the manifest is shown to match the release's
  published `SHA256SUMS` asset, and the check is scripted rather than
  eyeballed") is concrete, testable, and consistent with the project's
  existing reproducibility discipline (`docs/reproducibility.md` §3.1) —
  no changes needed.
- AC-4's "no architecture is declared that cannot be built" is the right
  discipline against overclaiming platform support — it just needs
  "exists" tightened to "CI-verified" per finding 3.
- The boundary AC-5 states toward #338/#443 (don't re-litigate the
  installer matrix, digest pin, or reproducibility verdicts) is correctly
  scoped as far as it goes; it simply stops one boundary short of the
  more important one (finding 1).
