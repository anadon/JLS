# Issue #188: Deterministic native installers: per-format byte-reproducibility program
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machine block away and #188 wants one thing: **a downstream party should be
able to establish that a shipped JLS installer contains what this repository's source
says it should**, without trusting the release runner. The issue's own Intended Audience
says exactly that ("verifiable without trusting the runner"). Everything else — five
formats, `SOURCE_DATE_EPOCH`, koly `SegmentID`, MSI relational-database streams — is a
*proxy* chosen for that end: byte-identity of the container file.

The proxy has now been pushed to its limit and the results are in. Three Linux formats
reach it. The msi cannot (`docs/windows-msi-determinism.md`: jpackage regenerates
component GUIDs and ProductCode with no override). The dmg has not, and Route A
*corrupts the image* (`hdiutil verify` FAIL, run 29773635573). The remaining program is
therefore two items: a macOS route decision (#191) and a Windows-VM install test carried
as `planned_tasks`. My claim is that the proxy is the wrong unit of measure, that the
evidence this feature itself produced is what shows it, and that a better unit dissolves
both remaining items while making the *user-facing* promise stronger than byte-identity
ever was.

Endorsement first, since it is real: the shared plumbing was the right architectural
call. Single-sourcing `SOURCE_DATE_EPOCH` from `project.build.outputTimestamp`
(`scripts/build-installer.sh:91-117`) so the jar gate (#44) and the installer gates pin
to one time authority is exactly the kind of seam that should be cut once and consumed
everywhere. `clamp_mtimes()` is 8 lines and buys three formats. Nothing below asks for
that to be undone.

## Reframing A (primary): verify the payload, not the container

**Proposal.** Define a canonical *installed-tree digest*: a merkle hash over the staged
app image — for each path, the relative path, mode bits, symlink target, and content
hash, sorted, with no timestamps. Compute it in `build-installer.sh` right after
`clamp_mtimes "$INPUT" "$RUNTIME"` (line 169), before any packager runs. Then one CI job
asserts two things:

1. the digest reproduces across two builds of one commit (the honest core of §4
   invariant 1, "no content change"), and
2. **every shipped installer, when extracted, yields that same digest.**

Publish it as a `PAYLOAD-DIGEST` release asset next to `SHA256SUMS-installers-*`.

**Why this is not a new capability but a small increment.** The per-format extraction is
already written and already runs in this repository: `release.yml:475-487` extracts the
rpm via `rpm2cpio | cpio`, runs the AppImage with `--appimage-extract-and-run`, line 520
mounts the dmg with `hdiutil attach`, and lines 258-261 record the deb and msi legs. The
extraction step exists as smoke-testing; this reframing reuses it as *verification*.

**Why it strictly dominates byte-identity as evidence.** Consider what each claim rules
out. A byte-identical deb tells a verifier the container is stable *on that runner
image*. It does not tell them a different jar went in — it only implies it because the
whole file matched. The payload digest tells them directly, and it tells them for all
five formats including the two the byte claim can never cover. Conversely, a
byte-different dmg is compatible with a bit-identical `JLS.app`; today nothing in the
repository can say so, and `docs/reproducibility.md:184-192` has to report the dmg as a
bare "not yet byte-identical" — the least informative honest statement available.

**Why it dissolves the two open items.** For the msi, the measurement already recorded
the answer: "the embedded cabinet stream (`Disk1Cab`) is byte-identical between the two
builds — the jlink runtime, the application jar, and every CFFILE match." Under the
payload unit the msi is *already passing* and always was; the "bounded residual" is
metadata about a relational database, i.e. packaging noise. For the dmg, #191's Route B
("own the imaging so Finder serializes the pinned values") is a macOS-only, fragile,
Finder-scripting effort to pin HFS+ volume-header bytes — on the one format whose app is
deliberately unsigned (README:37-43) and whose users must right-click-Open past
Gatekeeper anyway. Under the payload unit, the dmg lane becomes: mount, digest, compare.
That is achievable today with code already in `release.yml`.

**Boundary honesty.** A payload digest omits packaging metadata that genuinely affects
install behaviour: deb `control` and maintainer scripts, rpm scriptlets, `.desktop`
entries, `Info.plist`, MSI `Directory`/`Registry` rows. Include the extractable ones as a
second "control digest" per format; the MSI's semantic rows are the one place a
canonicalizer would still be needed, and that is precisely the place where the
`--diff` attribution tool already written can report rather than gate. This trade is
worth naming explicitly rather than hiding: the payload claim is *broader in coverage and
narrower in scope* than the byte claim, and it is the claim a student, an instructor, or
a Debian packager would actually act on.

## Reframing B: the Linux "Yes" is currently a determinism claim, not a reproducibility claim

This one I would land regardless of A, because the issue's own honesty gate (§4 invariant
2) demands it.

`docs/reproducibility.md` opens by quoting the Reproducible Builds definition — *"any
party can recreate bit-by-bit identical copies"* — and then puts the Linux installers in
the same **Yes** column as the jar (§1 line 18). But look at what the gate does
(`ci.yml:888-908`): two builds, back to back, same workspace path, same runner image,
same `dpkg-deb`, same `rpmbuild`, same JDK. Contrast the jar, which gets a *perturbed*
rebuild — different path, `TZ=Pacific/Kiritimati`, `LC_ALL=C`, `umask 077`
(`docs/reproducibility.md:133-139`) — and a per-release `.buildinfo` recording the exact
JDK and Maven.

The installers have neither. No release asset records which `dpkg-deb`, which `rpmbuild`,
which jpackage, or which appimagetool (pinned to 1.9.1 in-script, but not published per
release) produced the bytes. `dpkg-deb`'s compressor and its defaults have changed across
Debian releases; a verifier on a different distro will not reproduce that deb, and
nothing tells them which one to install. There is also no §3.x recipe for installers at
all — §3.3 exists only for the jar. So the promise "verifiable without trusting the
runner" is, as of `29afb26`, undelivered for every format, including the three marked Yes.

Concretely: emit an `installers.buildinfo` (JDK build, jpackage version, `dpkg-deb
--version`, `rpm --version`, appimagetool version + sha256, runner OS image) as a release
asset; add a §3.5 third-party installer-rebuild recipe; and add one perturbed leg
(different workspace path, different TZ/umask) to the existing Linux gate. That is the
same evidential bar #185 already set for the jar, applied to the artifacts this feature
claims. Until it exists, §1's table should say "Yes — deterministic on the CI runner
image", not "Yes — gated by CI" adjacent to the jar's bit-for-bit row.

## Concrete defect: the user-facing surface never got the supersession

DoD box 5 is ticked — "#184 Finding A superseded and `docs/reproducibility.md`/#185 scope
updated" — and `docs/reproducibility.md` was indeed updated three times. **README.md:53-60
still says the opposite**, in the section every installing user reads:

> the installers are *not* byte-reproducible (the native packaging tools embed
> wall-clock state), so rebuilding the same commit yourself will produce different
> checksums. That is expected; the jar and `bom.json` are the byte-reproducible
> artifacts […] while installer integrity rests on the attestation.

That is the pre-#194 world, stated as fact, on the front page. The entire payoff of this
feature — the reason a packager would care — is currently invisible to anyone who does
not open `docs/`. It is also the mirror image of the over-claiming defect #184 H3 warned
about, and the honesty gate is symmetric: an unclaimed true capability is as much a
documentation failure as an unfounded one. This is a five-line fix and should land before
any further route work on #191.

## Alignment with the project's arc, and where the seam was cut wrong

JLS is an educational simulator whose distribution story is already strong: checksums,
build-provenance attestation, Authenticode signing via SignPath, cosign on the image, a
Nix flake that builds from source. For the *stated* audience — students on lab machines,
instructors, autograders — attestation already answers "is this the maintainer's build".
The marginal beneficiary of byte-reproducibility is the distributor: Debian, Homebrew,
reproducible-central. And §6 of `docs/reproducibility.md` names the one action that
would actually reach them — submitting a rebuild recipe to reproducible-central — as
"future work" owned by nobody. Driving the dmg's koly bytes to zero serves no one on that
list; a published payload digest and a submitted rebuild recipe serve all of them.

Second structural observation. Five issues now restate the same sentence about per-format
verdicts (#184, #185, #188, #338, #471), two deduplication passes have been spent on
boundary maintenance in this issue's comments alone, and pass 2 records a live
double-ownership of the dmg outcome between #191 and #471 that it could not resolve. That
is not a process failure; it is the decomposition telling you something. The lanes were
cut along the **CI runner matrix** (§2: "the lanes split by OS runner"), which is a fact
about GitHub Actions, not about the problem. The problem's natural axis is
**container vs. payload** — one invariant, one gate, one register in
`docs/reproducibility.md` §1, with per-format work as PRs rather than a tree of issues
per operating system. Under that cut, #338's IC-3 totality check ("read the format list
out of `build-installer.sh`, fail on a format with no record") is not a separate
enforcement issue; it is the gate.

## What I would do with the remaining scope

I am explicitly disregarding §5 criterion 5 as written and the `planned_tasks` entry:

- **Do not** require #191 to reach a route decision on byte-identity. Close it on the
  bounded branch with the koly pin shipped and `hdiutil verify` green, and reinvest the
  Route B effort in the payload digest, which covers the dmg properly.
- **Do not** hold this feature's close-out on the clean-VM msi install test. By this
  issue's own scope boundary (§1: "Installer creation — #82"), verifying that a
  normalized msi installs, associates `.jls`, upgrades, and uninstalls is *installer
  behaviour*, not reproducibility. It only landed here as a waiver successor. HANDOFF it
  to #82, where the Windows VM rig question belongs, and empty `planned_tasks`.
- **Add** three items: the payload/control digest gate (Reframing A), the
  `installers.buildinfo` + §3.5 recipe + one perturbed leg (Reframing B), and the README
  correction.
- **Then** close #188 against a §1 table that says, for all five formats, exactly what a
  verifier can check and how — which is what the capability statement always meant.

If only one of these is done, do the README fix; if two, add the payload digest.
