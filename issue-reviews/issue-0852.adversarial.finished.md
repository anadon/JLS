# Issue #852: TASK-C579-2: double-clicking a .jls file in a Flatpak install opens JLS — portal-mediated association, sandbox permissions stated and minimal
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C579-2 is the MIME/portal step of the Flathub chain: CAP-34 (#518)
→ FEAT-C34-1 (#579) → TASK-C579-1 (#849, manifest) → **TASK-C579-2
(#852, this issue)** → TASK-C579-3 (#853) → TASK-C579-4 (#854). Its
five ACs are individually reasonable-sounding, but two things undercut
the whole task before code is written: the project's own internal
standards analysis already recommends against this channel for
exactly the reason this task's outcome paragraph names ("opening and
saving through the portal rather than through blanket home-directory
access"), and the acceptance criteria assume a portal-mediated file
dialog that the actual codebase does not have and cannot get for free.

## Findings, most severe first

**1. (Critical) AC-3's "portal-mediated open/save" is incompatible with how JLS actually opens and saves files, and no sibling task in the chain closes the gap.**
`src/jls/edit/Editor.java:104` and `:155` build the Open/Save dialogs
with `javax.swing.JFileChooser` — a pure in-process Swing widget that
walks `java.io.File` listings directly against the real filesystem. It
does not speak the `org.freedesktop.portal.FileChooser` D-Bus
interface; nothing in Swing/AWT does that automatically. Under a
sandbox with the "minimum permission set" AC-3 asks for (no
`--filesystem=home`), a `JFileChooser` shows only the sandbox's own
restricted view (`/app`, a handful of exposed paths) — not the user's
real home directory — so "opening and saving work through the
file-chooser portal" is not a property the existing dialog gains by
adding sandbox permissions; it requires either (a) replacing
`JFileChooser` with hand-written code that drives the portal's D-Bus
API directly (asynchronous call, a returned document-portal path, no
existing library dependency in `pom.xml` for it), or (b) granting
`--filesystem=home`/`--filesystem=host`, which is precisely the
blanket access AC-3 says to avoid and which the project's own
standards doc says "reviewers push back on" (see finding 2). AC-3 as
written asserts a destination state without acknowledging the
implementation gap between it and `Editor.java`'s current dialogs, and
neither this issue nor #849 (the manifest task it depends on) scopes
that work.
**Recommendation:** either scope the `JFileChooser` → portal-aware
dialog replacement explicitly (as its own AC or its own task, since it
is nontrivial Swing/D-Bus integration work, not a manifest permission
line), or drop AC-3's portal claim down to what is actually
achievable — e.g. `--filesystem=home:ro` for open plus documented
Save-As limitations — and say so plainly instead of asserting the
portal behavior as a given.

**2. (Critical) The whole Flathub effort — including this task — contradicts the project's own recorded standards analysis, and #852 does not acknowledge it.**
`docs/standards-adoption/10-desktop-and-housekeeping.md:81-99` ("Flathub:
recommend no") gives the sandbox objection in the exact terms this
issue's own Outcome paragraph raises: "The sandbox is hostile to the
tool's actual job. JLS is a file editor whose recovery story writes
`<circuit>.jls~` next to the user's file
(`src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`)
… Under Flatpak that means `--filesystem=home` (which reviewers push
back on)." Line 352 names the exact reopening trigger: "the sandbox
gains a workable document-portal story for `.jls~` siblings." That
trigger is not shown to be satisfied anywhere in this issue, #849, or
#579 — see finding 3, which shows the checkpoint file is in fact the
concrete failure this trigger describes. `ARCHITECTURE.md`'s "Recorded
decisions" section (verified by reading it in full) has no "Flathub:
not pursued" entry despite the standards doc recommending one be
added (line 349) — so the analysis exists in-tree but was never
ratified or overturned, and this task chain proceeds as though it was
never written. (Issue #853's adversarial review already raised this
at the task-chain level; it applies with equal force here since #852
is the task that most directly collides with the cited sandbox
objection.)
**Recommendation:** resolve this at #579 before doing #852's work —
either write an explicit `ARCHITECTURE.md` reversal that answers the
`.jls~`/portal objection (which finding 3 shows is still unanswered)
or shelve the chain.

**3. (High) AC-4's "known limitation" catch-all is exactly where the checkpoint-file failure belongs, and the issue never names it — an easy way for AC-4 to pass while the real problem ships silently.**
`SimpleEditor.java` writes crash-recovery checkpoints via
`writeCheckpointInBackground`, and `Editor.java:103` computes the
target path as `circuit.getDirectory() + "/" + circuit.getName() +
".jls~"` — a raw string-concatenated filesystem path in the *same
directory* as the open file. Under AC-3's minimal portal permissions,
a file opened through the document portal is exposed to the sandbox as
a single bind-mounted path (typically under
`/run/user/<uid>/doc/<id>/<filename>`), not as a writable parent
directory — so this string-built sibling path will not resolve to a
location next to the user's real file, and the write will fail. Per
`ARCHITECTURE.md`'s crash-recovery description, checkpoint writes are
part of the same exception-swallowing discipline `UserPrefs` uses
elsewhere in the project, so this failure will be silent: no crash
recovery for any circuit edited in the Flatpak build, with no error
surfaced to the user. AC-4 requires "any capability that does not work
… is recorded as a known limitation … rather than discovered by a
user," but the issue gives no enumeration procedure, and the most
severe candidate (checkpoint recovery, a core data-loss-prevention
feature) is not named anywhere in #852, #849, or #579. As written,
AC-4 could be satisfied by writing down some other, less severe
limitation (e.g. "external tool invocation doesn't work") while the
`.jls~` gap goes unrecorded and gets discovered by a user exactly the
way AC-4 says it must not be.
**Recommendation:** name `.jls~` checkpoint recovery explicitly as an
in-scope item for AC-4's audit, and decide up front whether it is (a)
accepted as a documented Flatpak limitation, (b) fixed by relocating
checkpoints under a portal-writable location for Flatpak builds
specifically (a real code change, not a manifest change), or (c) the
reason this channel should not ship at all — which is what the
standards doc's reopening trigger (finding 2) is actually asking.

**4. (High) External tool integrations conflict with "minimum permission set" and AC-4 doesn't resolve which way that conflict is settled.**
`ARCHITECTURE.md`'s "Plugin trust boundary" section names Yosys (#61),
GHDL/Icarus (#63), and ELK (#62) as subprocess-boundary external tool
integrations the project already ships against. Under Flatpak's
sandbox, invoking an arbitrary user-installed binary on the host
generally requires `--talk-name=org.freedesktop.Flatpak` plus
`flatpak-spawn --host`, or bundling the tools inside the Flatpak
runtime/extension — either of which is additional permission surface
or additional packaging scope this issue does not mention, and either
choice trades against AC-3's "minimum that achieves this" framing
(every extra permission needs a justifying comment per AC-3, but
AC-4's own example — "external tool invocation" — implies the task's
default answer is "doesn't work," without saying whether that default
was chosen to keep permissions minimal or simply not investigated).
**Recommendation:** state explicitly whether external-tool invocation
is (a) permanently out of scope for the Flatpak channel (document it
under AC-4 with the reason), or (b) planned via `flatpak-spawn --host`
with the trust implications spelled out (any host binary becomes
reachable, which is a materially larger trust surface than a `.jls`
MIME association) — right now the issue leaves this undecided while
implying via AC-3's "minimum" framing that it has been thought through.

**5. (Medium) AC-1's verification is unscripted and unlike the rigor the sibling tasks in this cluster demand elsewhere.**
AC-1 reads: "A `.jls` file opened from a desktop file manager launches
the Flatpak-installed JLS with that file loaded, on a stock GNOME or
KDE session." That is an interactive, manual, one-time check with no
recorded evidence artifact required — contrast #849 (TASK-C579-1)
AC-3: "the check is scripted rather than eyeballed," and #443
(TASK-0027), which requires a named CI/`workflow_dispatch` run URL as
primary evidence for exactly this class of "does the OS association
actually resolve" claim (P5/P6 in #443). #852 could be satisfied by an
undocumented one-off manual test on a maintainer's machine that is
never reproduced, unlike the standard this same issue chain sets for
its sibling tasks.
**Recommendation:** require a recorded verification artifact (a
screen recording, a scripted `flatpak run` + `xdg-mime query` check
under a headless compositor akin to `scripts/wayland-rig.sh`, or a CI
job) rather than an unscripted "worked when I tried it" claim.

**6. (Medium) AC-2's "validated … in CI or a committed script" has no re-run guarantee, so a later manifest edit can silently break the association with nothing to catch it.**
"in CI or a committed script" is satisfied by a script that exists in
the repo but is never invoked automatically. Compare the standards
doc's own recommendation for the (declined) AppStream work: "Add a
step to the existing `installer-reproducibility` job … so a regression
… is caught independently" — i.e., the house pattern for this class of
claim is a CI gate, not a committed-but-optional script. As written,
AC-2 can be satisfied once at task-close time and then bit-rot
silently the next time the manifest, `.desktop` entry, or MIME
declaration is touched.
**Recommendation:** require the `desktop-file-validate`/appstream
validation step to run in CI on every push that touches the Flatpak
manifest or `.desktop` files, not merely exist as a local script.

**7. (Medium) MIME-type identity between the Flatpak `.desktop`/AppStream declaration and the native installers is unstated, risking association collisions on a machine with both installed.**
The deb/rpm/AppImage installers register `application/x-jls-circuit`
(`resources/packaging/jls-association-linux.properties`). AC-2 says
"The MIME type, desktop entry and icon are declared in the manifest,"
but does not say the Flatpak manifest must declare the *same* MIME
type id, nor address what happens on a system where a user has both a
native package and the Flatpak installed (which application wins the
`.jls` double-click — a real, not hypothetical, scenario given the
README documents multiple coexisting install channels for Linux).
AC-5 ("the native installers' association behaviour … is not modified
by this task in any way") only guarantees the native side is
untouched in isolation; it says nothing about the combined-system
outcome.
**Recommendation:** add an explicit AC (or a note) requiring the
Flatpak manifest to declare `application/x-jls-circuit` verbatim, and
record, even briefly, the expected/observed resolution order when both
channels are installed on one machine.

**8. (Medium) Feasibility of #852 rests on an unverified premise in its direct dependency, #849, which this issue does not flag.**
#852 is `ordering_after: ["TASK-C579-1 (the manifest)"]` — i.e., #849.
#849's AC-1 commits to a manifest that "fetches the published Linux
release asset by URL and sha256 — no rebuild from source inside the
manifest." Flatpak/Flathub manifests conventionally build or extract
raw binaries; wrapping a `.deb`/AppImage (which embeds a `postinst`
that runs `xdg-mime install` and other host-level side effects never
executed inside a `flatpak-builder` sandbox) is an atypical shape that
#849 itself doesn't fully resolve, and #853's adversarial review
already flags this as a feasibility risk for Flathub review acceptance.
#852's own AC-1/AC-2 (desktop entry + MIME + icon "declared in the
manifest") implicitly assume the manifest can express these things
independently of whatever `.deb`/AppImage internals #849 ends up
wrapping — reasonable, but unstated, and worth one sentence given how
much of #852 depends on #849's shape.
**Recommendation:** add a one-line dependency note acknowledging that
#852's desktop-entry/MIME/icon declarations are manifest-level and
independent of #849's binary-fetch mechanism, so a reviewer doesn't
have to infer that from context.

## What's solid

- AC-5's boundary discipline (native installers' association untouched)
  is correct and consistent with the pass-2 dedup comment on #579,
  which confirms #443/#82 own the native `.desktop`/`xdg-mime`
  mechanism and #852 owns the Flatpak-specific one — no scope
  collision there.
- The instinct behind AC-3 — minimum permission, each grant justified
  in-manifest — is the right posture for a sandboxed app; the problem
  is that it is asserted as already-achievable rather than scoped as
  the nontrivial engineering work finding 1 shows it to be.
- AC-4's framing ("recorded … rather than discovered by a user") is the
  right policy in principle; the issue simply doesn't do the
  enumeration itself (finding 3).
