# Issue #852: TASK-C579-2: double-clicking a .jls file in a Flatpak install opens JLS — portal-mediated association, sandbox permissions stated and minimal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#852 is written as packaging bookkeeping — declare a MIME type, a desktop entry, an
icon, and a small permission list. It is not. It is the first place in the CAP-34
(#518) channel push where JLS's *own* file model meets a filesystem it does not
control. The manifest task (#849) can succeed while knowing nothing about JLS's
internals; #852 cannot. Every genuinely hard thing about the Flathub channel is in
this issue, and the issue frames all of it as configuration.

That framing is visible in AC-4, whose worked example — "external tool invocation" —
is not a thing JLS does. `grep -rn "Runtime.getRuntime().exec\|ProcessBuilder"
src/` returns nothing; `iverilog`/`ghdl`/`yosys` are *test and development*
dependencies (README "Optional development tools"), never invoked by the shipped
app. The example was written from a general model of "what Flatpak breaks" rather
than from this codebase. The two capabilities that actually break are elsewhere, and
neither is named.

## Finding 1: AC-3 is not a property a manifest can have

AC-3 requires that "opening and saving work through the file-chooser portal". JLS
opens and saves through `javax.swing.JFileChooser` — four construction sites, in
`src/jls/edit/Editor.java` and `src/jls/JLSStart.java`. Swing's chooser is an
in-process pure-Java widget that walks `java.io.File`. It does not speak
`org.freedesktop.portal.FileChooser`, and neither does AWT's `FileDialog` on the
Linux toolkits (`XToolkit`, `WLToolkit`). Portal mediation is a D-Bus conversation,
not a permission flag; nothing you write in a manifest makes an in-process chooser
portal-backed.

So AC-3 has exactly three honest resolutions, and the issue should name which one it
means:

1. **Add a portal-backed chooser** behind a seam. That is real work and a real
   dependency question: `pom.xml` carries four runtime dependencies (xz, jfree-svg,
   flatlaf, jspecify), all small and self-contained, and a D-Bus stack is a
   different order of commitment for a project whose stated deployment model is one
   self-contained jar.
2. **Grant named directories** (`--filesystem=xdg-documents:rw` and friends) and say
   plainly that JLS's chooser is in-process, so the permission set is a
   named-directory grant rather than a portal claim. Still far short of
   `--filesystem=home`, and *true*.
3. **Grant `--filesystem=home`** — the outcome `docs/standards-adoption/10-desktop-and-housekeeping.md`
   predicted ("which reviewers push back on") and which AC-3 exists to forbid.

An AC that claims a property the toolkit cannot deliver will be met by whoever
implements it writing `--filesystem=home` and a comment justifying it, which is
AC-3 inverted. Decide now, in the issue.

## Finding 2: JLS addresses documents by directory-plus-name, not by handle

The deeper mismatch. JLS reconstructs paths by string concatenation from a
directory and a circuit name:

- `src/jls/edit/Editor.java:62` — save target directory
- `src/jls/edit/Editor.java:103` — `getDirectory() + "/" + getName() + ".jls~"`
- `src/jls/edit/Editor.java:365` — `getDirectory() + "/" + getName() + ".jls"`
- `src/jls/edit/SimpleEditor.java:5528` — the checkpoint writer
- `src/jls/JLSStart.java:2314` — checkpoint deletion on open

The document portal hands the app a path like `/run/user/1000/doc/<hash>/circ.jls`,
whose *directory* contains that one document and nothing else, and is not writable
for new entries. Saving in place survives that. Writing `<name>.jls~` beside the
file does not, and neither does "Save As" into the same directory. Crash recovery —
the mechanism `src/jls/DefaultExceptionHandler.java:160` tells the user to rely on,
`docs/file-format.md` specifies, and `docs/collaborative-editing-research.md` cites
as the bound on worst-case loss — degrades to silence inside the sandbox. AC-4
would file that as "a known limitation of the channel". Losing crash recovery is not
a footnote; it is the one guarantee a student notices only when they need it.

## Reframe A (primary): cut the seam inside JLS, and do it first

Make #852 about a single `DocumentLocation` policy — one object that answers "what
is this document's save target", "where does its checkpoint go", "may I create a
sibling" — and route those five call sites through it.
`src/jls/FileAbstractor.java` already declares itself "the single read/write path
for circuit files (issue #15)"; these five sites bypass that intent by rebuilding
paths themselves. The policy's sandbox arm puts checkpoints in
`$XDG_STATE_HOME/jls/checkpoints/` keyed by the document's real path, with the
original path recorded inside the checkpoint and surfaced in the recovery message.

Why this is the better goal:

- It is the *only* thing that makes AC-3's "minimum permission set" reachable. As
  long as JLS needs to create siblings, the minimum is directory access.
- It is testable headlessly, in `mvn verify`, on every push — unlike everything else
  in this issue, which is verifiable only by a human on a GNOME or KDE desktop.
- It pays off outside Flatpak: a `.jls` opened from read-only lab media or a network
  share hits the same wall today, silently; a future macOS App Sandbox hits it; the
  in-tree collaboration work (`src/jls/collab`) already has a document identity that
  wants exactly this notion.
- **It makes the kill criterion cheap.** KC-34-1 says the channel gets dropped if it
  costs more than 0.5 mw per cycle, and TASK-C579-4 (#854) only measures that
  *after* all four tasks land. If the sandbox work is a JLS-internal improvement,
  dropping the channel forfeits nothing. If it is manifest configuration, everything
  spent here evaporates.

Sequencing consequence: `ordering_after: [TASK-C579-1]` is wrong. Whether the
sandbox story is workable at all determines whether the manifest is worth writing.
Put the seam first, and give it an issue that does not depend on Flathub surviving.

## Reframe B: one desktop-integration description, rendered per channel

There are already three spellings of the same desktop entry —
`resources/packaging/resource-dir-linux/JLS.desktop` (deb/rpm, installed as
`jls-JLS.desktop`), an inline heredoc in `scripts/build-installer.sh:227-237`
(`JLS.desktop`), and `flake.nix:53-63` (`jls.desktop`) — plus a jpackage-generated
`jls-JLS-MimeInfo.xml` derived from
`resources/packaging/jls-association-linux.properties`, plus a metainfo template
already designed but unowned in `docs/standards-adoption/10`. #852 would add a
fourth desktop spelling and the first hand-written shared-mime-info XML. That is
fragmentation bought with one channel.

Instead: make `resources/packaging/desktop/` the single source — one `.desktop.in`,
one shared-mime-info XML, one metainfo template — rendered per channel with the
right desktop-id, and run `desktop-file-validate` plus AppStream validation over
*all* rendered outputs. AC-2 as written validates only the Flatpak copies, which is
the least valuable place to put the check: the other four channels ship today and
are unguarded.

Two concrete wins fall out immediately, for every channel at once:

- **`.jls~` is not in the MIME glob anywhere.** `JLSStart.java:181,294,563` accepts a
  checkpoint as a start file and `JLSStart.java:2242` filters for it in the chooser,
  but no channel associates it — a student whose JLS crashed cannot double-click
  their recovery file.
- **The type is glob-only, and the format is content-sniffed.** README tells external
  tools to "sniff the content rather than trusting the extension" (a `.jls` may be
  XZ, zip, or plain text), while the MIME definition trusts only the extension. A
  `<magic>` clause for the plain-text container's `FORMAT 1` / `CIRCUIT` prefix
  costs one XML element. (Do not add magic for the XZ header — it would collide with
  `application/x-xz`; say so in the file.)

**I am disregarding AC-5 as written.** "The `.jls` association behaviour of the
native installers is not modified by this task in any way" is right about
*behaviour* and wrong about *source*: it forbids the only change that stops this
issue from making the project worse. Keep the behavioural guarantee — it is
testable — and drop the prohibition on touching the shared description.

## Reframe C: name the channel's role instead of apologising for it

The other real casualty is the CLI. Every command line in `README.md` and
`docs/batch-interface.md` becomes `flatpak run --command=... io.github.anadon.JLS`,
and a sandboxed CWD makes autograder use awkward. AC-4 would record that as a
limitation. Better: state it as a design split the project has *already made*. The
container image is documented as "batch mode only… headless by construction"; the
jar is the portable path. So: **Flatpak is the GUI channel; the container and the
jar are the batch channels.** Export no `jls` CLI wrapper from the Flatpak at all.
The known-limitations paragraph becomes one sentence of policy instead of a list of
regrets.

One small piece of good news the issue misses: `scripts/build-installer.sh`'s
`select_linux_runtime` bundles JetBrains Runtime, so the app is Wayland-native.
`--socket=wayland` genuinely suffices on a Wayland-only session — a rare case where
the minimal set is minimal *and* aligned with the project's stated "X11 is
deliberately not part of this project's tooling". `--socket=fallback-x11` plus
`--share=ipc` remain needed for X11 sessions; say which is which in the manifest
comments AC-3 asks for.

## Trajectory

`docs/standards-adoption/10-desktop-and-housekeeping.md` recommends *declining*
Flathub, and one of its four reasons is precisely the sandbox/`.jls~` mismatch this
issue inherits. CAP-34 (#518) reverses that on competitive grounds (#510:
Logisim-Evolution ships four channels, JLS ships none) — a legitimate reversal, but
nowhere in #518, #579 or #852 is the earlier reasoning engaged, and the doc's
reopening trigger names "a workable document-portal story for `.jls~` siblings" as
the condition. That story is Reframe A. Build it, record the supersession in
`ARCHITECTURE.md` § Recorded decisions, and the reversal becomes a decision rather
than a drift.

## What I would change in the issue

- Replace AC-3's portal claim with the chosen resolution from Finding 1, stated
  explicitly.
- Add an AC: crash recovery works inside the sandbox, verified by a headless test of
  the location policy — not documented as a limitation.
- Rewrite AC-4's example: the casualty is the CLI surface, not external tools.
- Amend AC-5 to guarantee native association *behaviour*, and require the shared
  description that behaviour is rendered from.
- Move the checkpoint/sibling-write seam ahead of #849, in its own issue, so it
  survives KC-34-1.

Endorsed as a goal; the scope is one layer too shallow, and the band (0.25–0.5 mw)
prices the manifest edits rather than the work.
