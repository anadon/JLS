# Issue #815: TASK-C168-2: Share opens a session and prints a join string, Join consumes it — and a listener still exists nowhere else in the program's life
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Create `jls.collab.ui` (does not exist yet — `find src/jls/collab -type d`
shows only `net`, `session`, `op`, `crdt`) with a Share dialog that binds a
`SessionListener` and shows a join string, and a Join dialog that drives
`SocketSession.join` to a completed handshake, while preserving the P3
boot-listener hygiene property. Five ACs. The scope boundary (stay below
`jls.collab.net`, bind only inside the Share handler) is sound and matches
the layering the codebase already enforces. Several ACs, though, either
contradict the project's own design doc, misdescribe what the cited tests
enforce, or leave a hole a minimally-compliant implementation can drive a
truck through.

## Findings, most severe first

**1. AC-1 stops at "completed handshake," silently dropping the SAS-verify gate the project's own security model requires before a session is trusted.**
`docs/collaborative-editing-research.md` §5.2 is explicit that connecting is
a four-step sequence — Start session → Join → **Verify** (SAS comparison,
explicit Confirm click) → Persist — and `SocketSession.join`'s own javadoc
says "[t]he caller compares `link()`'s SAS with the peer out of band before
trusting the channel... this method establishes the encrypted pipe, not the
human trust decision" (`SocketSession.java:107-110`). AC-1 asks only for "a
Join dialog [that] accepts that string and completes a real handshake
against the sharing instance" — nothing gates anything on SAS confirmation.
The Verify dialog is a *separate*, *later* task: #816 (TASK-C168-3) declares
`ordering_after: ["TASK-C168-1", "TASK-C168-2"]` and is the one that
"gates Confirm," reads `KnownPeers.Trust`, etc. Nothing in #815 states that
the Join dialog's resulting session must stay inert/unusable until #816's
gate exists, and nothing in #816 states it is retrofitting a block into
already-shipped, already-usable #815 code. Read literally, #815 can be
implemented and merged as a complete, working "join a live collaboration
session with one dialog, no human verification step" feature — exactly the
silent-MITM-acceptance shape the crypto layer was built to prevent.
**Recommendation:** add an AC to #815 requiring the post-handshake session to
be held in a non-propagating/inert state (or simply not surfaced as "joined"
in any editor-visible way) until a trust decision exists, or explicitly
sequence #815 behind #816 instead of the reverse.

**2. AC-3 misattributes its own enforcement, and half of it is not checkable by the cited mechanism.**
"`AC-3: ... no Swing type is referenced from any jls.collab package other
than jls.collab.ui, enforced by the existing ArchUnit rules
(collabLayersAreHeadless, socketEndpointsAreConfinedToCollabNet)`." Reading
`ArchitectureRulesTest.java`: `collabLayersAreHeadless` (`:150-160`) is
indeed the Swing-confinement rule. `socketEndpointsAreConfinedToCollabNet`
(`:249-266`) checks something unrelated — that `java.net.Socket`/
`ServerSocket`/channel classes are referenced only from `jls.collab.net` —
it says nothing about Swing at all. Citing it here as backing the Swing
claim is simply wrong. Worse, the same AC's first clause — "All UI work
happens via `invokeLater`" — is not enforceable by either cited rule:
ArchUnit's bytecode rules here check class-level dependency *existence*
(does X reference Swing at all), not call-site threading discipline (is
this particular Swing mutation wrapped in `invokeLater`). A dialog that
touches Swing directly from the socket-reading thread passes both cited
rules unchanged. **Recommendation:** split AC-3 into the part ArchUnit
actually checks (package confinement) and the part that needs a runtime/
EDT-violation check (`test/jls/ui/EdtViolationDetector`, if it can be made
to cover collab dialogs, or a new assertion) — and fix the citation.

**3. AC-1's own test is satisfiable entirely on loopback, so it can pass while validating none of the feature's actual purpose.**
The design doc's join string is meant to list "all candidate addresses
(LAN address first)" (`docs/collaborative-editing-research.md:344-346`) for
real cross-machine use — the entire point of Share/Join. `SessionListener`
today exposes exactly one bound `address()`/`port()` pair, and its own
javadoc recommends `bindLoopback` as "the safe choice... before a host opts
into LAN exposure" (`SessionListener.java:83-90`). AC-1 asks only for a
"display-tagged test on the Linux GUI lanes" showing Join "completes a real
handshake against the sharing instance" — a test entirely satisfiable with
both dialogs on one CI runner talking over `127.0.0.1`, never touching LAN
address enumeration or display. Nothing in the issue requires exercising
multi-address candidate listing, so the AC can be fully green while the
feature's real target scenario (two lab machines) is never exercised end to
end. **Recommendation:** either state explicitly that address-candidate
listing is out of scope for this task (and say what the join string
contains for now — loopback-only?), or add a criterion that forces at least
one non-loopback address path to be exercised or documented as deferred.

**4. No acceptance criterion requires the Share/Join dialogs to be reachable from the running editor.**
The Outcome talks about "the Share action's handler" as though a menu item
or toolbar entry already exists to trigger it, but nothing in the five ACs
requires wiring one into `SimpleEditor.makeElements` or the menu bar (which
`test/jls/ui/MenuBarSpecTest` pins as an expectation table elsewhere in the
suite). A literal-minimum implementation can satisfy every AC by exposing
Share/Join as directly-instantiable dialog classes driven only from test
code, shipping a feature no user can actually open. **Recommendation:** add
an AC requiring a discoverable trigger (menu item/toolbar button) and note
the corresponding `MenuBarSpecTest` update.

**5. AC-1/AC-5's GUI-lane testing implies a two-live-instance harness that does not exist yet, and the band likely doesn't price it.**
The existing GUI-boot rigs (`scripts/wayland-rig.sh`, `scripts/x11-rig.sh`)
boot and screenshot **one** JLS window; the "loopback two-instance harness"
referenced in `SessionListener`/`SocketSessionTest` javadoc is a net-layer
(socket-only, no Swing) fixture. Driving two real dialog-bearing JLS
instances to a completed handshake inside `gui-wayland`/X11 Xvfb lanes is
new CI infrastructure, not reuse of what's there — the same shape of
under-costed-precedent problem flagged on sibling issue #816's AC-1 (citing
#101 as precedent for capability #101 doesn't provide). **Recommendation:**
size this explicitly as new dual-instance harness work, or scope AC-1 down
to same-JVM two-dialog-instance testing and say so.

**6. AC-4's "typed, user-readable failure" is weak enough that a single generic catch-all satisfies it.**
`SessionListener.bind`/`accept` throw plain `IOException`; `SocketSession.join`
throws `IOException` or `HandshakeRejected`. AC-4 never requires the UI to
distinguish these (bind failure vs. handshake rejection vs. network
unreachable) in the message shown to the user — "never a silent no-op and
never a stack trace dialog" is satisfied by one generic "could not connect"
`TellUser` popup for every failure mode, which is testable and green but
gives a student no way to tell "wrong join string" from "peer isn't
listening" from "the other side rejected you." **Recommendation:** require
the message to name the failure category (bind/connect/handshake-rejected),
mirroring the `LoadError` taxonomy discipline already established elsewhere
in the codebase.

## What's solid

- The scope fence in the Outcome — "no crypto, no socket construction moves
  above `jls.collab.net`" — matches the layering already pinned by
  `transportKnowsNothingOfCircuits` and `socketEndpointsAreConfinedToCollabNet`;
  this task doesn't ask for anything the architecture forbids.
- `ordering_after: []` is honestly stated: #815 has no real dependency on
  #814 (glyph assets) — Share/Join as scoped never touches `Sas` or its
  images, unlike #816 which legitimately needs both.
- AC-2's reuse of `BootListenerHygieneTest`'s existing property (rather than
  inventing a parallel test) is the right instinct and consistent with how
  that test's javadoc already describes itself as the runtime half of P3.
