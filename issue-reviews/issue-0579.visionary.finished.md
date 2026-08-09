# Issue #579: FEAT-C34-1: a Linux user installs JLS from Flathub like any other desktop app, and the store page is a discovery surface with screenshots
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

The title welds two wants together. **Install** — "no JDK, no manual download, one
step" — and **discovery** — "the store page is a discovery surface with screenshots."

The install half is already delivered, four times over. `README.md:17-28` ships deb,
rpm, AppImage and a Nix flake for Linux, all bundled-runtime, all x86_64 + aarch64,
all attested. #510 scored install **4/5, "best-in-desktop-class"**. A Flathub listing
adds a fifth Linux way to do a thing JLS already does well. So the entire novel value
of #579 is the second half: **being seen**. Everything below judges it as a discovery
issue, because that is what it is.

## Where it pulls against the project's arc

**1. The project already decided this, and the failure to record the decision is the
real defect.** `docs/standards-adoption/10-desktop-and-housekeeping.md:81-102` carries
a section headed **"Flathub: recommend no"** with four evidenced reasons, and its
step 7 (`:347-352`) instructs recording *"Flathub: not pursued (recorded &lt;date&gt;)"* in
`ARCHITECTURE.md` § "Recorded decisions" with a named reopening trigger. That entry
does not exist — `ARCHITECTURE.md` has seven recorded decisions and none mentions
Flatpak. A week later a capstone re-proposed the declined thing, and neither #518 nor
#579 nor the four child tasks (#849, #852, #853, #854) cite the analysis. This is the
exact failure mode `ARCHITECTURE.md` § "Recorded decisions" exists to prevent —
"decisions that look like accidents until written down." The adversarial review makes
this a finding; from the visionary side it is the *product*: the durable output of
#579 is the missing record, not a manifest.

**2. The channel inverts every property JLS's distribution posture was built for.**
The trajectory is unmistakable in the tree: reproducible jar + `bom.json` +
`.buildinfo`, keyless cosign, GitHub attestations, a monthly `repro-installers.yml`
probe, `scripts/build-installer.sh` as *the single recipe used locally and by CI*, and
two self-hosted channels (`flake.nix`, `ghcr.io/anadon/jls`) with no gatekeeper. Every
one of those is "we can verify our own bytes, and nobody can change the terms under
us." Flathub is someone else's build infrastructure, someone else's review queue,
someone else's runtime lifecycle, and `SOURCE_DATE_EPOCH` and the double-build gates
do not carry across (`10-desktop-and-housekeeping.md:86-89`). At bus factor 1 that is
not a small trade — it is the first dependency in the distribution chain the project
cannot repair itself.

**3. AC-5 measures against the wrong denominator, in both directions.** The recurring
cost of a Flathub app is not primarily per-JLS-release (the update bot handles that,
which is why AC-3 is achievable); it is per-*freedesktop-runtime*-release — SDK EOLs
force manifest bumps on someone else's calendar, and any `finish-args` change
re-enters review. AC-5 counts "per-release review/update cost" and will therefore
report a flattering number while the real churn arrives out of band. On the benefit
side there is no numerator at all: PF-4 (#508's download KPI) is the only instrument
CAP-34 has for telling whether a channel reached anyone, and #579 declares
`ordering_after: []` rather than riding behind it. A channel shipped before its
measurement can never fire KC-34-1 honestly.

**4. The listing would carry a warning label on the page meant to sell the tool.**
JLS's file dialogs are plain Swing; AWT has no `xdg-desktop-portal` file chooser. A
usable sandboxed JLS therefore needs `--filesystem=home`, and Flathub renders an app's
`finish-args` as a user-facing permissions summary on the store page. AC-4's goal is a
discovery surface; the mechanism delivers a discovery surface that says "can read and
write all your data" above three screenshots of a teaching tool.

## Three alternatives, ranked

**A. Give #579 #583's shape: a recorded go/no-go where "no, with reasons" closes it
as completed.** CAP-34 already contains that pattern for the Debian ITP (PF-5). The
Flathub question has *more* prior analysis and *less* clear upside than the Debian one,
so it deserves at least the same treatment. Deliverable: the `ARCHITECTURE.md` entry
step 7 specifies, carrying the four reasons and the trigger already drafted at `:350-352`
("a course or lab asks for a Flatpak, or the sandbox gains a workable document-portal
story for `.jls~` siblings"). Cost: hours. Value: the next re-proposal reads the record
instead of re-deriving the argument, which is the only compounding asset here.

**B. Cut the seam at the metadata, not the channel.** The reusable artifact behind AC-4
is not a Flatpak manifest — it is `io.github.anadon.JLS.metainfo.xml`. Landscape #175's
reduced form is specced end to end in the same document (8 steps, `:228-360`) and does
not exist in the tree today: `resources/packaging/` has icons, three
`jls-association-*.properties`, and a `.desktop` template, and **no AppStream file at
all**. That single templated file makes the deb, rpm, AppImage and flake self-describing
to *every* AppStream consumer that ever indexes them — AppImageHub today, a PPA or
nixpkgs or distro packager later, and Flathub itself if the decline is ever reversed. It
is a strict prerequisite of #579, not a competitor to it, at roughly a fifth the band.
Be honest about what it does not do: metainfo alone creates no store listing. It makes
JLS *legible* to stores; #579 is about *entering* one. But no version of the Flathub work
is cheaper or better for having skipped it, and the metainfo is also where the unresolved
`GPL-3.0-only` vs `-or-later` question (`:264-278`) gets forced into the open — a good
thing to hit in a one-file PR rather than mid-submission.

**C. The reframe that dissolves the blocking objection — and it is not about Flatpak.**
Reason 2 of the recorded decline is that JLS writes `<circuit>.jls~` beside the user's
file (`src/jls/edit/Editor.java:103`). The document defends that placement well and I
accept the defense: it is document-scoped recovery on the `vim` model, it is named in
`DefaultExceptionHandler.java:160`, filtered in `JLSStart.java:2242`, deleted from the
circuit's directory at `JLSStart.java:2314`, and moving it wholesale breaks recovery for
a circuit carried on a USB stick. So do not move it — **degrade it**. Add one property:
*when the document's directory is not writable, JLS checkpoints to
`$XDG_STATE_HOME/jls/checkpoints/` keyed by the document path, tells the user where, and
recovers from both locations.* The sibling stays the default; nothing in the three call
sites changes for the normal case.

The point is that this is not a Flatpak concession. It is the same constraint JLS's
actual users already live under and the README already concedes — "lab machines you
cannot install onto", read-only course shares, mounted network volumes. Today a student
opening a circuit from a read-only share gets a checkpoint write that cannot succeed.
Fix that and you have (i) a real robustness win for the only user population the project
can name, (ii) a graceful story for `--filesystem=xdg-documents`-class sandboxing, and
(iii) the standards doc's own reopening trigger satisfied from *inside* the project
rather than by waiting on freedesktop. The Swing file-chooser gap remains and still
argues for `--filesystem=home`, so this does not make Flathub free — but it converts
the strongest recorded objection into a solved problem, and it is worth doing whether or
not a Flatpak ever ships.

## If the maintainer overrides the decline and ships anyway

Then cut it along a different seam than #579 draws. **Ship the editor, not the tool**:
declare the Flatpak the GUI editor only, and state in `README.md` that the batch surface
under Flatpak is the jar or `ghcr.io/anadon/jls` — which is already the documented
autograder path. That leaves `docs/batch-interface.md`'s stability contract untouched by
the sandbox instead of relying on a `flatpak run` prefix that rewrites every command line
in two normative documents. And replace AC-2 rather than defending it: JLS's *jar* is
byte-reproducible with a published `.buildinfo`, so a Flathub build from tagged source
whose jar checksum is asserted equal to the release jar is both stronger provenance than
"consume the prebuilt payload" and better aligned with Flathub's build-from-source
preference. AC-2 as written bets on a repackaging model the reviewers may simply decline;
the project already owns the better answer.

## On the stated acceptance criteria

I am disregarding AC-1 through AC-5. They are internally coherent and they audit the
wrong thing: each one measures whether a Flatpak was successfully produced, none measures
whether anyone found JLS because of it. The one criterion worth keeping is AC-5's
instinct that the channel must justify itself — but it should be stated as a benefit
threshold read off PF-4's KPI, with the channel dropped if it does not clear it, not as a
cost threshold read off a self-reported effort estimate.

## Verdict rationale

`redirect`. The want behind #579 — Linux users discovering JLS where they discover
software — is real, and #510's competitor comparison is a fair provocation. But the
instrument is wrong for this project's shape, the project already reasoned its way to
that conclusion and then failed to write it down, and the highest-value work in the
neighborhood is elsewhere and cheaper: record the decision (A), ship the metainfo (B),
and make JLS survive a non-writable document directory (C). Do those three and the
Flathub question can be reopened later on evidence, by a maintainer who inherits an
argument instead of re-running it.
