# Issue #579: FEAT-C34-1: a Linux user installs JLS from Flathub like any other desktop app, and the store page is a discovery surface with screenshots
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

FEAT-C34-1 proposes a Flathub distribution channel wrapping the existing
jpackage-built Linux installer. The scope boundary against sibling issues
(#338, #443, #511, #580/#581/#583) is drawn cleanly. But the issue does not
engage with an existing, dated, in-repo analysis of exactly this proposal
that concludes the opposite, and several of its acceptance criteria rest on
assumptions the codebase does not currently support.

## Findings, most severe first

### 1. [Critical] The issue contradicts a recorded in-repo recommendation against Flathub, without acknowledging or refuting it

`docs/standards-adoption/10-desktop-and-housekeeping.md` (last touched
2026-07-28, a week before #579 was filed 2026-08-04) contains a dedicated
section, **"Flathub: recommend no"** (lines 81–102), giving four specific,
evidenced reasons:

1. Flathub needs a manifest in a separate `flathub/io.github.anadon.JLS`
   repo "built on Flathub infrastructure against a Flatpak runtime — a
   second packaging pipeline alongside the jpackage one, with none of the
   reproducibility plumbing (`SOURCE_DATE_EPOCH`, the double-build gates in
   `.github/workflows/ci.yml`) carrying over."
2. "The sandbox is hostile to the tool's actual job": JLS's crash-recovery
   writes `<circuit>.jls~` next to the user's file
   (`src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`)
   and batch mode is invoked from shells/autograders; under Flatpak that
   needs `--filesystem=home` ("which reviewers push back on") and a
   `flatpak run` prefix that "breaks every command line in `README.md` and
   `docs/batch-interface.md`."
3. It "contradicts the recorded deployment model: single self-contained
   jar plus per-OS installers, no install step assumed, no network."
4. Screenshots must be HTTPS-reachable URLs at Flathub build time, and the
   project has no published website.

The doc's own implementation procedure (step 7, line ~349) instructs
recording *"Flathub: not pursued (recorded &lt;date&gt;)"* in
`ARCHITECTURE.md` § "Recorded decisions" — which had not yet happened when
this review was written (`ARCHITECTURE.md`'s "Recorded decisions" section
has no Flathub entry). #579 proposes exactly the channel this document
argues against, cites none of its four reasons, and offers no rebuttal.
Whichever side is right, an issue proposing to reverse a week-old documented
maintainer analysis should say so explicitly and address the arguments —
silently proceeding as if the analysis doesn't exist is the actual defect.

**Recommendation:** Before any work starts, either (a) close #579 against
the recorded decline and let the standards doc's reopening trigger ("a
course or lab asks for a Flatpak, or the sandbox gains a workable
document-portal story for `.jls~` siblings") govern reopening, or (b) amend
#579's body to explicitly rebut each of the four points with evidence, and
update `ARCHITECTURE.md` to record the reversal instead of the decline.

### 2. [High] AC-1 is satisfiable while the sandbox breaks JLS's actual editing workflow

JLS's file dialogs are plain Swing (`grep` for `JFileChooser`/`FileDialog`
hits `src/jls/edit/Editor.java` and `src/jls/JLSStart.java`), which has no
xdg-desktop-portal integration. Inside a default Flatpak sandbox (no
`--filesystem=host`/`--filesystem=home`), a `JFileChooser` opened from
File > Open can only browse the sandbox's own tree, not the user's
Documents folder — and per finding 1, granting `--filesystem=home` is
exactly what the standards doc says Flathub reviewers push back on for an
app whose actual justification is "let me read anywhere the user might
have a .jls file." AC-1 only requires: *"`flatpak install` from Flathub
yields a JLS that launches and opens a `.jls` file."* That is satisfiable
purely through portal-mediated launch (double-click in the file manager,
which forwards one already-selected file via `flatpak run
--file-forwarding`) without ever exercising the in-app File > Open dialog,
the Save As dialog, or the `.jls~` checkpoint write beside an arbitrary
user file. A reviewer could tick AC-1 while the sandboxed app is
functionally crippled for its normal editing workflow the moment a user
tries to open a second file from inside the running app.

**Recommendation:** AC-1 should explicitly require exercising File > Open
against a file outside any Flatpak-granted directory, and the checkpoint
write path, not just double-click launch.

### 3. [High] AC-2's premise — a single "existing bundled-runtime Linux build" — doesn't currently exist

AC-2 says the manifest "consumes the published artifact rather than
rebuilding without provenance." But `scripts/build-installer.sh:298-299`
still pins the Linux JBR runtime to a literal
`UNVERIFIED-PLACEHOLDER-fill-in-real-sha256-see-issue-101` for both
`linux-x64` and `linux-aarch64`; per the script's own `select_linux_runtime`
logic, an unarmed digest makes the build **silently fall back** to the
build JDK's own jlink image (X11/XWayland-only) rather than the
Wayland-capable JetBrains Runtime the "bundled-runtime" framing implies.
#579's own machine block acknowledges this exact issue exists
(`ordering_after: []  # rides the shipped bundled-runtime Linux build;
#443 TASK-0027 strengthens but does not gate`) and chooses to proceed
anyway. That means the artifact AC-2 pins a checksum to is, as of today,
undefined between two materially different runtimes depending on when the
Flathub manifest is generated relative to #443 landing — and nothing in
#579 requires waiting for or detecting which one it got.

**Recommendation:** Either make #579 `ordering_after: [443]` for real, or
add an explicit AC asserting which runtime the wrapped artifact bundles.

### 4. [Medium] jpackage's Linux layout is not Flatpak-relocatable — "wraps... rather than introducing a second build path" may not be achievable as stated

The standards doc (finding 1, reason 1) and the AppStream section
(`docs/standards-adoption/10-desktop-and-housekeeping.md:71`) both note
JLS's deb installs into a fixed `/opt/jls` (see
`resources/packaging/resource-dir-linux/postinst`), a layout jpackage does
not make relocatable. Flatpak requires everything installed under `/app`.
"Wrapping" the existing build therefore means unpacking an installer (deb
payload or AppImage) and re-rooting its paths for `/app` — itself a
distinct packaging step with its own maintenance burden, which is
substantively the "second build path" the issue's Outcome section says it
avoids. The issue's framing ("rather than introducing a second build path")
is a hidden assumption that the wrap is mechanical; it is not shown to be.

**Recommendation:** Name the actual re-rooting mechanism (e.g. `flatpak-builder`
`type: archive` over the AppImage, or extracting the deb payload) as part
of scope, and reconcile the "no second build path" claim with it.

### 5. [Medium] AC-4's screenshot dependency is real but unordered, and Flathub's hosting requirement isn't addressed

AC-4 requires screenshots "sourced from the same set CAP-27 (#511)
produces," but #579's `ordering_after: []` declares no gating dependency,
and at #511's own filing every screenshot-producing planned feature
(PF-1 README shop window, PF-4 gallery page) is listed `unfiled` — i.e. no
screenshot asset exists yet. Separately, the standards doc notes AppStream
`<screenshots>` entries must be **URLs**, reachable over HTTPS at Flathub
build/review time, and the project currently has no published website
(`ARCHITECTURE.md` § "Help delivery" records that hosted docs are a
*planned future*, not shipped). #579 never states where the three
required screenshots will actually be hosted for Flathub to fetch — a gap
the source document it should be building on (#511) doesn't resolve either,
since #511 is itself unstarted.

**Recommendation:** Add an explicit dependency edge on #511's screenshot
deliverable, and specify the hosting mechanism (GitHub raw URLs pinned to
a tag, or GitHub Pages) before filing this as startable.

### 6. [Medium] AC-5's kill criterion is gameable by construction

AC-5: "if it exceeds 0.5 mw per cycle, KC-34-1 applies and the channel is
dropped with the arithmetic written down." But AC-3 only requires the
automation be "verified once end to end" — a single successful run. There
is no stated minimum number of release cycles before the 0.5 mw figure is
trusted, and no stated measurement method (labor-hours logged how, by
whom, against what baseline). A single frictionless first run could
satisfy both AC-3 and produce a rosy AC-5 number that never gets revisited
— even though Flathub's review process commonly requires a fresh manual
review whenever a manifest's `finish-args` (sandbox permissions) change,
which is a realistic recurring cost this issue never budgets for. Compare
the rigor #338/#443 apply to their own falsification criteria (named ICs,
explicit re-verification, a documented protocol for "a residual cannot be
bounded") — #579 states a kill criterion with none of that discipline
behind it.

**Recommendation:** Require the AC-5 measurement to span at least 2–3
release cycles, and name what counts as "review/update cost" concretely
(hours spent responding to Flathub reviewer requests + CI minutes +
manifest-diff review, or similar).

### 7. [Low] Unresolved GPL identifier ambiguity is a latent trap for the metainfo `<project_license>` tag

The same standards-adoption document flags (its own "Cross-section
conflict" callout) that the project has **not** settled whether it is
`GPL-3.0-only` or `GPL-3.0-or-later` — `flake.nix:78` says
`licenses.gpl3Only`, no `src/` file carries an or-later clause, but
`CONTRIBUTING.md:138` binds contributors to "GPLv3-or-later," and
`README.md`'s own License section calls the or-later election "this
project's own," unresolved against `pop_GPLv3.pdf`. A Flathub submission
needs a `<project_license>` SPDX identifier in its AppStream metainfo.
#579 doesn't mention this at all, so whoever executes it risks picking
whichever identifier is convenient and adding a fourth disagreeing file to
the two the standards doc already found in conflict.

**Recommendation:** Resolve the license-identifier question (tracked
elsewhere per the standards doc) before, not during, this feature's
execution.

### 8. [Low] AC-1's aarch64 clause allows silent scope narrowing

"...on both x86_64 and aarch64 **if the manifest declares them**" lets the
feature ship x86_64-only without anyone having to own that decision or
record why aarch64 was dropped — contrast with the discipline #338 states
as an explicit invariant ("No claim is narrowed silently").

## What's solid

- The boundary notes against #338/#443 ("must not re-litigate the matrix,
  the digest pin, or the reproducibility verdicts") are accurate and
  correctly scoped — confirmed against both issues' actual content.
- Consuming CAP-27's screenshots rather than commissioning a second set
  (per the pinned issue comment) is the right de-duplication call, even
  though the dependency isn't gated (finding 5).
- AC-2's underlying principle — checksum traceability to the attested
  release asset, no unprovenanced channel-specific rebuild — is directionally
  correct and consistent with the project's overall distribution posture;
  the flaw is only in which artifact that currently resolves to (finding 3).

## Verdict rationale

`needs-rework`, not `should-not-proceed`: the channel itself may be
viable once the runtime pin lands (#443) and screenshots exist (#511), but
the issue as filed does not engage with a directly contradicting, dated,
in-repo analysis (finding 1) and rests AC-1/AC-2 on assumptions (portal-free
file access, a stable bundled runtime) the codebase does not currently
satisfy. The maintainer should resolve finding 1 explicitly — accept the
prior decline and close, or file a documented reversal — before any
manifest work begins.
