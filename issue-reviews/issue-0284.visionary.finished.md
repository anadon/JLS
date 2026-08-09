# Issue #284: Installer verification: first green msi/dmg install → launch → .jls open on real runners, then retire the experimental flags
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip away the workflow bookkeeping and #284 makes one claim about what JLS
should become: **the README's install promise should be a proven promise, not a
build-succeeded promise.** #82 states the capability as "a user with no
pre-existing JDK installs JLS, launches it from the shell, and double-clicks a
`.jls` file to open it in the editor." Today the matrix proves bytes were
produced; #284 wants it to prove the student's first ten minutes.

That end is right and squarely on the project's arc. The route is where I
disagree, on three counts — one of which makes the issue unable to complete as
written.

## The route pulls against the project's own evidentiary machinery

Through #101/#111/#188/#265 JLS has developed a consistent and genuinely good
pattern for exactly this class of claim ("does the real thing work on real
hardware?"): an executable rig (`wayland-rig.sh`, `x11-rig.sh`, `macos-rig.sh`,
`windows-rig.ps1`) + a selftest guarding the rig's own classification logic + a
control frame separating "JLS is broken" (exit 1) from "the environment withheld
something" (exit 2) + artifacts (logs, screenshots, non-blank pixel counts) + an
advisory CI lane that accrues a record + a stated promotion rule (20 consecutive
runs, ≤1 failure) before the flag drops.

For the same class of claim #284 proposes a weaker form: one green
`workflow_dispatch` dry-run plus a human double-clicking a file on their laptop
and typing the outcome into a comment. The issue is aware of the mismatch — §7.1
carves the ci.yml msi lane out because it "follows the separate 20-run rule."
That leaves the project holding its *lowest-frequency, highest-stakes* surface
to its *weakest* standard. The installers job fires only on tags and dispatches;
one green run has almost no power against the two flakiest runner classes in the
fleet, and learning that at tag time costs a red release you cannot retry without
cutting another tag.

Worse, human-attested evidence decays and cannot be re-run. §11 already concedes
it: #134's signing changes the msi bytes and "the signed artifact needs a
re-verification pass." So does every runner-image roll, jpackage/WiX bump, and
JDK floor raise. A verification whose only record is a sentence in a thread has
to be repurchased by hand each time — which means it won't be.

## Reframing A: make the double-click executable, and buy the oracle for it

#284 treats the double-click as inherently manual. It is not.

- **Windows:** `cmd /c start "" sample.jls` resolves the handler through exactly
  the path Explorer uses (`HKCU\Software\Classes\.jls` → ProgId →
  `shell\open\command`). That chain is assertable with `Get-ItemProperty`, and
  its `"%1"` is the direct analogue of the `%f` field-code assertion the Linux
  leg already makes at release.yml:464 — proof the idea works, one platform over.
- **macOS:** `open sample.jls` goes through LaunchServices, the same resolver
  Finder uses; `lsregister -f /Applications/JLS.app` forces registration on a
  hosted runner and `lsregister -dump` makes the binding assertable.

What actually blocks automation is the **oracle**. "Observe it open in the
editor" has no machine-readable signal: `JLSStart` sets the frame title to
`JLSInfo.version` and nothing else (`src/jls/JLSStart.java:1281`), while the
circuit name goes on a `JTabbedPane` tab (`src/jls/edit/Editor.java:185`),
invisible from outside the process. So:

1. **Put the open circuit's name in the frame title** — `setTitle(name + " — " +
   JLSInfo.version)`. A few lines, the universal desktop convention, a real UX
   win on its own merits, and the missing observable.
2. **Both GUI rigs already enumerate window titles** — System Events on macOS,
   `MainWindowTitle` on Windows — with control-frame classification and
   screenshots already written. Add an *installed-artifact* mode: install the
   built installer, `open`/`start` a distinctively named probe circuit, assert
   the JLS window title contains that name. The manual ritual becomes an exit
   code.
3. **Run those lanes advisory in ci.yml, accrue, promote by the standing rule.**

The `experimental: true` flags then stop being a question. They exist today only
because the release path is the only place install evidence could accrue — which
is backwards. Move accrual to CI and the flags fall out as a consequence rather
than a judgment call.

## Reframing B: the release workflow is the wrong home for verification

Separable but related: `release.yml`'s installers job does double duty as
publisher and test bed. Different failure economics. Publishing should be boring
and fail-closed; verification should be frequent, cheap, and allowed to be red.
Splitting them (nightly `installer-acceptance` in ci.yml, publish-only on tags)
is the seam #284 never looks for — and the same seam already cut for
`repro-installers.yml`.

## The hard finding: H2's argv[1] contract is very likely false on macOS

#82 §3 states the double-click contract as "the OS passes the file path as
`argv[1]`," and #284's H2 inherits it for both Windows and macOS. On Windows and
Linux it holds (`"%1"`, `%f`). **On macOS that is not how documents are
delivered.** `--file-associations` yields `CFBundleDocumentTypes`, which makes
LaunchServices route `.jls` to JLS.app — but Finder then sends a
`kAEOpenDocuments` Apple Event; it does not append the path to `argv`. In Java
that event surfaces exclusively through `java.awt.Desktop.setOpenFileHandler`
(legacy `com.apple.eawt.OpenFilesHandler`).

Grepping the tree for `Desktop`, `setOpenFileHandler`, `OpenFilesHandler`,
`apple.eawt`: **zero occurrences in `src/`.** `JLSStart` opens exactly one
`startFile`, sourced only from `parseCommandLine`.

So the predicted P2 observation on macOS is not "works" and not "nothing
happens" but the nastiest third option: **JLS launches to an empty untitled
editor.** Association looks right, app comes up, the student's circuit silently
isn't there. The existing macOS smoke step (`Contents/MacOS/JLS -h`,
release.yml:513) cannot see this — `-h` never involves LaunchServices at all.

And §10's falsification instruction misdirects the fix: "the association metadata
is wrong — fix `jls-association-*.properties`, not the launcher." No content in
that properties file can make Cocoa pass argv. Followed literally, it sends the
implementer to edit a file that cannot fix the problem and then, per #82 §7, to
narrow macOS to jar-only — descoping a supported platform on a wrong diagnosis.

**The missing piece is an architectural seam, not a metadata field:** JLS has no
*document-open port* — no single "open this path into a new tab" entry point that
both the CLI operand and an OS-delivered open event can call. Adding one pays on
every platform: on any OS, double-clicking a second `.jls` while JLS is running
has nowhere to go today, because the OS reuses the running instance rather than
starting a second process with a fresh `argv`. That is a live gap for exactly
the audience #82 names — a student with two circuits open in a lab.

## What I am disregarding from the stated acceptance criteria

- **"P2 verified manually on both OSes"** — replaced by the rig assertion above.
  A human attestation is not evidence this project accepts anywhere else, and it
  cannot survive #134.
- **"Flip `experimental: false` … on one green run"** — replaced by the 20-run
  rule applied to a CI lane where 20 runs are attainable.
- **The ARM Open Question** — struck entirely. Under Reframing A there is no
  manual check to waive on ARM, because there is no manual check anywhere. The
  question dissolves rather than being answered.

## Sequencing I would run

1. Frame title carries the circuit name (tiny, independently valuable).
2. `Desktop.setOpenFileHandler` wired to the same open path as the CLI operand,
   headless-safe, with a model-level test that both routes land on one entry
   point. **This is the real prerequisite #284 is missing.**
3. Installed-artifact mode in `macos-rig.sh` / `windows-rig.ps1`, plus the
   Windows registry-chain assertion mirroring the Linux `%f` check.
4. Advisory CI lanes; accrue; promote; release flags drop as a consequence.

## Verdict

**endorse-with-reframing.** The goal — a fail-closed release matrix backed by
real install evidence — is right and belongs on the critical path to closing #82.
But this should not be executed as "run a dry-run, click a file, flip two
booleans." It should be "give the app a document-open port and a title-bar
oracle, extend the rigs that already exist to the installed artifact, and let the
flags fall out of the standing promotion rule." As written, its most likely
outcome is a false `REFUTED:` on macOS that costs the project a supported
platform for a reason fixable in about twenty lines of `JLSStart`.
