# Issue #815: TASK-C168-2: Share opens a session and prints a join string, Join consumes it — and a listener still exists nowhere else in the program's life
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Not "two dialogs." #168's capability statement is the real target: *two humans
establish a channel whose man-in-the-middle resistance they can check.* Everything
headless for that already exists on master — `Handshake` (873 lines,
transcript-bound), `SecureLink`, `Sas`, `KnownPeers`, `SessionListener`,
`SocketSession`, `LoopbackTransport`, plus four ratchets pinning the layering.
`jls.collab.ui` is the last unbuilt floor of a mostly-built building, and #815 is
where the building first becomes visible to a user. That makes it the issue that
sets the user-facing contract for the whole collab program. It is scoped as if it
were plumbing.

I endorse building `jls.collab.ui`. I do not endorse this cut of it.

## 1. The slice ships connection without trust, and it lands first

`ordering_after: []` on #815; #816 (Verify + key-change) is ordered *after* it.
So there is an intended master state where JLS has a Share menu item that binds a
port and a Join dialog that "drives `SocketSession` to a completed handshake" —
with no SAS surface anywhere. `SocketSession.join`'s own javadoc says it plainly:
"this method establishes the encrypted pipe, not the human trust decision."
`KnownPeers` is never consulted by any AC here. AC-4 asks for typed failures on
*refused* connections; nothing asks what happens on a *successful* connection to
the wrong peer.

That state contradicts #168 §4 invariant 4's spirit and its §1 capability
statement, and it is the exact shape of security regression the whole feature was
built to avoid: an encrypted channel that feels safe and is unauthenticated to the
human. Encrypted-but-unverified is worse than plaintext because the padlock is
implied. A slice boundary that produces it — even transiently, even on a branch
that will be followed by #816 — is the wrong boundary.

## 2. Reframing A (the simple one): the SAS is already word-renderable — ship Verify now

`Sas.words()` exists today and returns seven English words from the fixed 64-word
vocabulary. The research doc anticipated this exactly: bundle glyph images, but
"comparison then works even rendered as words alone" (§5.2 rendering caveat).
#168's own §2 rationale rejected splitting glyph assets from the Verify dialog
("the Verify dialog is untestable without the images") — but that premise is
false. A word-only Verify dialog is fully testable, fully usable, and reads aloud
across a lab bench better than pictures do (the PGP word list precedent the
research doc cites is words, not images).

Concretely: **fold #815 and a word-only #816 into one landing; demote #814 (64
licence-cleared glyph images, with its licensing-failure escalation path and its
build gate) to optional polish behind the words.** One landing, no unverified
window, one dialog family, and the highest-risk task in the feature — the one
whose re-plan protocol says "record the gap and stop; do not vendor" — stops being
on the critical path to a working session. #817's two-machine record can then run
against words.

## 3. Reframing B (the leveraged one): the join string is the design artifact, and the ACs don't design it

#815 is the only issue in the program that decides what a join string *is*. Its
ACs treat it as an opaque token — "displays a join string," "accepts that string."
The research doc §5.2 asks for `<ip>:<port>` with *all candidate addresses listed,
LAN first*; `SessionListener` today exposes one bound address and
`SessionListener.bindLoopback` is the documented default. Nothing in the codebase
enumerates candidate addresses. A Share action that passes AC-1 on the loopback
GUI lanes can ship something that cannot do what #817 requires.

Worse, an `ip:port` join string throws away free authentication. Syncthing device
IDs, `ssh` host-key pinning, and Magic Wormhole codes all put a key commitment in
the thing the human copies. **Put the starter's identity fingerprint in the join
string.** `SecureLink.peerFingerprint()` already exposes the authenticated peer
key; the joiner compares it against the fingerprint it parsed out of the string
and closes on mismatch. No protocol change, no transcript change, no violation of
#168 invariant 5 — a caller-side check on landed API.

The payoff is structural: a MITM against the *joiner* becomes impossible without
any human comparison at all. The SAS then only has to authenticate the joiner *to
the starter* — one direction, one screen, and in the classroom case ("whoever is
at the bench") the Roster/admission model of research §5.3 is the honest place for
that decision anyway. The join string becomes a versioned, typed grammar owned by
`jls.collab.net` (`JoinString.parse`/`format`, candidate address list, fingerprint,
version tag, hostile-input capped like everything else in that package) rather than
string concatenation inside a dialog. That is a wire format users paste into chat;
it deserves the same treatment as the `.jls` file format, which this project
documents normatively.

## 4. Reframing C (the architectural one): cut the seam headless, not at the dialog

AC-3 enforces `invokeLater` for UI work. That guards the harmless direction. The
dangerous direction is unstated: `SessionListener.accept()` and
`SocketSession.join()` both block, and the handshake is three network round trips.
If those run in a Share action's handler, the EDT freezes on a hostile or absent
peer. ARCHITECTURE.md's threading model has one rule for exactly this class of
mistake, and it is not `invokeLater`.

The issue's own framing — "binding happens only inside the Share action's handler"
— puts session lifecycle *in a Swing listener*. That is why AC-1 and AC-2 have to
be display-gated tests on two CI lanes, and why AC-4's "typed, user-readable
failure" is a code-review judgement rather than a type.

Alternative: **a headless `SessionHost` in `jls.collab.session`** — an explicit
state machine (`idle → listening(joinString) → handshaking → awaitingVerify →
live → closed`, plus a `SessionFailure` typed terminal) that owns the session
thread and exposes a listener interface. `jls.collab.ui` becomes a thin view that
renders states and calls three methods. Consequences:

- AC-2 ("a listener appears only after Share and disappears when the session
  closes") becomes a headless JUnit test — `BootListenerHygieneTest` can extend
  its `/proc` probe over `SessionHost.share()`/`close()` directly, which is a
  *stronger* pin than a GUI-lane assertion and runs on every platform.
- AC-4's typed failure is an enum the compiler checks, in the same taxonomy
  tradition as `LoadError` and `OpRejected`.
- #816's VERIFIED/KEY_CHANGED gating hangs off states, not dialog code.
- #171 replication gets its attach point for free: `SessionHost` is where a
  `Transport` meets an `OpSink`. Right now nothing in `src/` outside
  `jls.collab.net` references `Transport` at all — the seam has no consumer, and
  #815 as written still doesn't give it one.
- Display-gated tests shrink to "the dialog renders and the button is wired,"
  which is what GUI lanes are actually good at.

`collabLayersAreHeadless` already permits this: `jls.collab.session` is
Swing-free by rule, and `Roster`/`ReachabilityTracker` are already sitting there
waiting for a host to live in.

## 5. Share currently shares nothing

After #815 lands, two instances hold a live encrypted socket over which zero bytes
of circuit ever flow. `CollabModule.register` contributes no `OpSink`; #171 is
unfiled work. A student who clicks Share and a student who clicks Join get a
connection and no collaboration. That is a Potemkin menu entry in a shipped
installer, and it will generate the bug report "collaboration doesn't work."

Two honest routes, both better than shipping it plain: (a) keep the whole
`jls.collab.ui` surface behind an explicit opt-in (`-Djls.collab=on` or an
unreleased-feature note), so the GUI lanes exercise it while users don't meet it
half-built; or (b) make the first user-visible landing actually do something — the
smallest real payload over the established `Transport` is a one-shot **circuit
push**: starter sends the current circuit as save-format text, joiner opens it
read-only. That reuses `Circuit.save`/`FileAbstractor` verbatim, needs no CRDT, no
roster, no op replication, and turns "Share" into a feature a lab could use next
week. It also exercises the frame cap and the whole stack end to end with a
payload, which is a far better #817 record than "the handshake completed."

## 6. Smaller alignment notes

- ARCHITECTURE.md — the contributor's map — has no `jls.collab` entry at all: not
  in the module layout, not in the threading model, not in the recorded decisions,
  despite ~3.5k lines of collab source on master. The first *user-visible* collab
  surface is the right moment to fix that, and it costs a paragraph.
- `SessionListener.bindLoopback` as the conservative default is right, but nothing
  decides *who chooses* LAN exposure or how it is surfaced. That is a security-UX
  decision this issue will make by accident if it doesn't make it deliberately.

## What I would keep unchanged

The boot-listener hygiene property (AC-2) is exactly right and is the best thing in
this issue — "binding happens only inside an explicit gesture" is a property worth
preserving by construction, and the `/proc` probe already pins it. The
`invokeLater` and no-Swing-below-`ui` rules (AC-3) are correct as far as they go.
Requiring the Linux GUI lanes green (AC-5) is right.

## Verdict

**rethink.** The end — a user can start and join a session from the GUI — is on the
project's arc and should be built. The shape should change: (1) land Share, Join,
and a **word-only** Verify as one increment, so no commit on master ever offers an
unverified encrypted channel, and demote the glyph-image task off the critical
path; (2) specify the join string as a versioned `jls.collab.net` grammar carrying
candidate addresses *and* the starter's fingerprint, which the joiner pins; (3) cut
the seam at a headless `SessionHost` state machine so lifecycle, failure taxonomy,
and listener hygiene are testable without a display and #171 has somewhere to
attach. I am explicitly setting aside AC-1's "Share/Join only" scope and #168 §2's
claim that Verify cannot ship without images — `Sas.words()` on master refutes it.
