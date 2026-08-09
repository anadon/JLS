# Issue #816: TASK-C168-3: both peers see the same seven glyphs before Confirm is reachable, and a changed key for a known peer is loud rather than convenient
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and #816 is the only place in the whole
collab stack where a *human* is asked to make a security decision. Everything
below it is machine-checkable and already machine-checked: `Handshake` is a
reviewed TLS-1.3-RPK/SIGMA instantiation (`docs/collab-handshake-review.md`,
verdict CLEAN), the tamper-every-byte property is exhaustive
(`test/jls/collab/net/HandshakeTest.java`), `Sas` is a deterministic function of
the transcript (`src/jls/collab/net/Sas.java:82`). The residual risk the SAS
covers is exactly one thing: *the joiner connected to the wrong endpoint*. The
issue's job is to make that residual risk survive contact with a distracted
sophomore in a lab.

Judged against that, the issue is aimed correctly and its instincts (gate
Confirm, no Enter default, make KEY_CHANGED structurally different) are the
right instincts. But the design it inherits — mandatory seven-glyph comparison
on every join — optimizes the wrong variable. The dominant failure mode of
comparison ceremonies is not reflex-Enter; it is habituation: a dialog every
student sees on every join, every day of the semester, becomes a speed bump they
learn to clear without reading. And habituation is precisely what destroys the
issue's *second* half: if the ordinary verify dialog appears on every join, the
"loud" KEY_CHANGED dialog is just the ordinary dialog wearing red, arriving in
the same slot in the same muscle-memory sequence. The issue's two goals are in
tension with each other, and the issue does not notice.

## Reframing 1: pin the key in the invite, and the ceremony mostly disappears

The join string is *already* an out-of-band authenticated channel. It has to
be — #815 and research §5.2 step 1 have the sharer read `<ip>:<port>` across the
bench or paste it into the class chat. A channel that can carry an address can
carry a key commitment. Extend the join string from `ip:port` to
`ip:port#<pin>`, where `<pin>` is a truncation of the sharer's Ed25519
fingerprint (`IdentityKey.fingerprint()`, `src/jls/collab/net/IdentityKey.java:305`),
base32-encoded. 64 bits is 13 characters and costs 2^64 offline key-grinding to
forge; the human effort is one extra token in a string they were already reading
aloud. The joiner compares `session.link().peerFingerprint()`
(`SecureLink.java:120`) against the pin and refuses on mismatch — a check that
belongs in `jls.collab.net` as a `join(..., expectedFingerprint)` overload, is
purely headless, and needs no dialog at all.

This is the Syncthing / WireGuard shape (identity travels with the invite)
rather than the Signal shape (identity confirmed after connection). Magic
Wormhole does it more elegantly still with a short PAKE code, but SPAKE2/CPace
is not in the JDK and #168 invariant 6 forbids a new runtime dependency, so the
pin is the version JLS can actually build today.

What this buys: the common path becomes *silent and safe*. A MITM does not get a
dialog the student can click through — it gets a refused connection with a typed
error. The Verify dialog stops being a per-join tax and becomes the fallback for
the case it was designed for: an unpinned invite (someone typed an IP by hand,
or the pin was lost in a copy-paste). Rare dialogs are read; universal dialogs
are not. The KEY_CHANGED path then arrives against a background of *no* modal at
all, which is what actually makes it loud.

Note this does not delete #816 — it re-aims it. Every AC still needs to exist;
they just cover the fallback and the alarm rather than the default flow.

## Reframing 2: AC-2 is defending against the wrong reflex

"Confirm disabled until both glyph panels have rendered, no Enter-to-confirm"
is a control against a user who confirms *before the screen paints*. Nobody does
that. The user who defeats a SAS ceremony looks at a fully-rendered dialog,
does not compare anything, and clicks the enabled button. AC-2 leaves that user
entirely unprotected while adding load-order machinery to the dialog.

Note also that the render-gate requirement is self-inflicted: it exists because
the SAS is rendered as *images* that can fail to load. #814 already makes a
missing image a build failure, so the runtime gate guards a state #814 forbids.

The control that matches the real reflex is a challenge instead of a
confirmation: show the seven glyphs, then ask the user to pick their peer's
*n*-th glyph from eight candidates drawn from the 64-word vocabulary. Honest
arithmetic: this does not make bypass impossible, it converts a
click-through from p=1 to p≈1/8 per challenge (1/64 with two challenges), and it
makes the answer un-guessable without actually looking at the peer's screen —
which is the behaviour the whole ceremony is trying to buy. The selection logic
is a headless model (`Sas` plus a decoy generator), unit-testable with no
display; the display test only has to show that the dialog renders that model
and refuses to proceed on a wrong pick. **I am explicitly disregarding AC-2 as
written**: keep "no Enter-to-confirm default" (cheap and correct), drop the
render-gate, and spend the budget on the challenge.

## Reframing 3: the trust decision is in the wrong module

AC-3 and AC-4 are stated as dialog behaviour and therefore land in the most
expensive evidence substrate the project has — the sway/xvfb GUI lanes with
screenshots. But neither is a rendering property. AC-4 ("confirming writes
VERIFIED, declining leaves the store unchanged and the session closed — no
partially trusted state") is a pure state-machine property over `KnownPeers` and
session lifetime. AC-3's substance ("VERIFIED reconnect shows no dialog") is a
policy function, not a paint.

`test/jls/ui/package-info.java` states the project's own discipline: "built in
layers with the cheapest layer preferred per assertion." #816 as written pulls
against it. The cut I would make: a headless `TrustDecision` policy in
`jls.collab.net` (or a small `jls.collab.trust`) mapping
`(KnownPeers.Trust, pinned fingerprint present?, SAS)` →
`{PROCEED_SILENT, REQUIRE_VERIFY, REQUIRE_CHALLENGE, WARN_KEY_CHANGED, REFUSE}`.
`jls.collab.ui` becomes a dumb renderer of that enum. AC-3 and AC-4 become
Layer-1 tests that run on every `mvn verify` on every platform; the display
lanes are left holding only the genuinely display-shaped claim — that the dialog
shows *this live session's* SAS and not a stale, blank, or placeholder one,
which is the one thing AC-1 could assert that `SasTest`/`HandshakeTest` do not
already prove headlessly. As written, AC-1 ("both dialogs show the same seven
glyphs") re-tests transcript agreement through the most expensive possible lens.

## A model-layer defect this issue will otherwise inherit

`KnownPeers.check` (`src/jls/collab/net/KnownPeers.java:149`) returns
`KEY_CHANGED` when an unknown fingerprint arrives under a display name that
matches *any* stored record. The class javadoc says fingerprints are identity
and names are labels — and then the alarm keys on the label. Two consequences
#816 would faithfully render: a second lab machine legitimately named "Alex"
raises a false alarm, and an attacker who simply picks an unused name is
downgraded to `UNKNOWN`, i.e. the ordinary verify dialog rather than the warning
path. Under Reframing 1 the alarm becomes precise and worth its volume: *the key
at this pinned invite is not the key I verified for it*. #816 should not paper
over this in the UI; it should file the model fix or absorb it.

Second, "loud" should outlive the modal. `KnownPeers` records only
fingerprint + name + time, so a key change leaves no trace once dismissed. A
`previousFingerprint`/history line (a v2 store format; the parser is strict, so
this needs an explicit version bump) turns a momentary warning into a durable
fact the user can go back and look at — and gives the KEY_CHANGED dialog
something to show beyond a color.

## Does this strengthen the arc?

Yes, with a caveat worth stating. `docs/grand-architecture.md` places collab as
a peer consumer of `core` alongside gui/batch/HDL, and the transport half is
landed and reviewed; #816 is not speculative work. The caveat: for the actual
classroom problem — several students on one circuit over a week — the repo's own
`docs/capability-roadmap/lf-06-diff-merge-vcs.md` describes a file-level
diff/merge capability that four of five prerequisites are already shipped for
(`sid` identity, canonical serializer, closed op algebra) and that "no schematic
tool in any of the three surveyed classes ships in the box." Real-time P2P and
semantic merge are not competitors, but if collab's justification is ever
re-argued, that is the comparison to run. It does not change #816's disposition:
the transport exists, it is unusable without a trust UI, and shipping an
encrypted channel with no human verification path would be worse than either
alternative.

## Disposition

Endorse the issue's existence and its two goals. Reframe the design: pin the key
in the invite so the ceremony becomes the exception (Reframing 1), replace the
render-gate with a challenge (Reframing 2), move the trust decision below the UI
so AC-3/AC-4 stop being display-gated (Reframing 3), and fix or file the
name-keyed KEY_CHANGED semantics. AC-5 stands unchanged.
