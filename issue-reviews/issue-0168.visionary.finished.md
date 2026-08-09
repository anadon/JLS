# Issue #168: P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification (collab Stage 1a)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its machine block, #168 says: *two people who already know each other in
the physical world should be able to share a circuit with no server, no account, and
no certificate authority, and be able to prove no one is in the middle.* That claim is
right, and it is well aligned with the project's arc. Every other trust decision in
this repo is of the same shape — no maintainer GPG key (#136), no Apple Developer
enrollment (README), no CA-backed anything, integrity via checksums plus provenance.
A CA-less, account-less, verify-with-your-own-mouth design is the collab-shaped
version of the same conviction. The landed half deserves the praise it gets: JDK-only
primitives, `SessionListener` as the single socket-binding site, `BootListenerHygieneTest`
proving a default GUI start opens no port. That is a project saying "networking is a
liability we accept only on an explicit gesture," and it is exactly right.

What I am judging is not that. It is the **remaining** decomposition — now filed as
#814–#817 — and there I think the issue has cut along the wrong seam three times.

## Observation 1: the shipped output has no consumer, and the plan adds a fourth layer before the first vertical slice

At HEAD, `grep -rl 'jls\.collab' src/` outside `src/jls/collab/` returns exactly two
files, and both reach only the **op** layer (`SimpleEditor` imports `jls.collab.op.*`;
`JlsModules` imports `OpExtensionPoints`). Nothing in JLS reads `jls.collab.net`,
`jls.collab.crdt`, or `jls.collab.session`. That is 11,304 lines of collaboration code
whose only caller is its own test tree. The #348 boundary comment saw this and put a
"first-consumer contract audit" on #169; good. But #168's own plan then schedules two
more deliverables (a dialog suite, a manual LAN record) that still do not produce a
user-reachable end-to-end path, because the *session* layer that would carry a circuit
op across the wire is a different issue.

The highest-value next commit in this whole program is not the polished Verify dialog.
It is the thinnest possible vertical: **two JLS instances on one machine, Share → Join →
verify → one `AddElements` op crosses the wire and lands in the other canvas.** That
commit is worth more than #814–#817 combined, because it is the first thing that can
falsify the stack, and because every contract mismatch the unit tests missed shows up
in it. Concretely: I would make #168's close-out gate a two-instance loopback demo with
words-only verification, and let the dialog polish, the glyph art, and the two-machine
LAN run follow the thing they are meant to decorate. §6's "critical path: dialog child →
LAN record child → feature close-out" is horizontal layering discipline applied where a
vertical spike is cheaper and more informative.

## Observation 2: the join string is already an out-of-band channel — put the key in it

This is the reframing I most want on the record, and the issue never considers it.

`SessionListener` currently yields `<ip>:<port>`. `IdentityKey.fingerprint()` is already
a stable SHA-256 peer id, `KnownPeers.check(fingerprint, displayName)` is already keyed
on it, and `Trust.VERIFIED` already suppresses re-verification. So: **make the join
string `<ip>:<port>#<fingerprint-prefix>`.** The starter pastes it into the course chat,
the LMS, a Signal message, or writes it on the whiteboard — it has to travel out of band
anyway, that is the premise. The joiner's install pins that fingerprint before the
handshake; the handshake either matches it or is refused loudly. This is the Syncthing
device-ID / Magic Wormhole code construction, and it is strictly stronger than a passive
SAS-confirm dialog, because the authentication rides the channel the humans already
chose rather than a ritual bolted on afterwards.

What it does to the plan: the seven-glyph Verify dialog stops being the mandatory path
and becomes the **fallback** — used when the invite carried no fingerprint (typed IP,
read aloud over a noisy room) or when `KnownPeers` reports `KEY_CHANGED`. The mandatory
path becomes "it just connected, and here is who it connected to." The change to landed
code is small (a join-string encoder/decoder plus a pre-seeded expected fingerprint on
the joiner side); the change to the remaining scope is large, and all of it is removal.

## Observation 3: the SAS failure mode is habituation, not rendering — so make the human transmit, not witness

I1 gates Confirm on both glyph panels having rendered and forbids Enter-to-confirm. That
defends against the *accidental* click. It does nothing about the real thing that kills
SAS deployments: the user who clicks Confirm the fourth time without looking, because
nothing in the dialog requires them to have used the out-of-band channel at all. A
"rendered panels + no default button" gate is a UI interlock protecting a UI mistake; the
security property being asserted is a *human* one.

A better design, cheaper than the glyph-image path: **the joiner enters what the starter
read aloud.** Seven word fields with autocomplete over the 64-entry vocabulary (or seven
combo boxes), and JLS compares the entered string to its own derived `Sas`. Mismatch →
the session is refused, not warned about. The machine does the comparison; the human
cannot reach a joined session without having actually moved 42 bits across a channel an
attacker does not control. This is structurally unfakeable in a way that "click Confirm"
never is, it is testable headlessly (`Sas.equals` already exists and is the whole
oracle), and it converts the most display-gated, most licensing-risky part of the plan
into a model-layer assertion. Signal's own newer designs move in exactly this direction —
compare-and-mark rather than acknowledge.

## Observation 4: I am disregarding the glyph-image acceptance criteria (#814, and I1's image half)

Stated plainly, as the brief asks. The ~64 licence-cleared named glyph images are the
highest-cost, highest-risk, lowest-value item in the remaining scope, and the project's
own research document already retires the need for them:

> "comparison then works even rendered as words alone" — `docs/collaborative-editing-research.md` §5.2

The images exist in the plan only because the emoji-rendering caveat in §5.2 ruled out
font emoji. But §5.2's *recommendation* was images-with-word-labels; its *fallback* was
words alone, and the fallback is sufficient for the described use — two people reading
to each other across a lab bench or over a call. That channel is **spoken**, not visual.
Pictures do not help a spoken comparison; distinct, phonetically separated nouns do, and
`Sas.WORDS` is already exactly that list, already frozen as protocol order. Meanwhile the
images bring: a licensing escalation path that the issue's own §7 says ends in "record
the gap and stop"; a build-fails-on-missing-glyph rule (#814); 64 assets a single
maintainer must draw or vet; and a display-gated CI lane the whole slice then inherits.

If glyphs are wanted later, there is a third option the issue never lists beside
"draw originals" and "adapt an existing set": **generate them procedurally in Java2D** —
index → (shape × fill × mark) deterministic drawing. Zero assets, zero licence question,
byte-reproducible like the rest of the build, and testable headlessly by rendering to a
`BufferedImage` and asserting determinism and pairwise distinctness. That would be a nice
polish issue. It is not a precondition for shipping verification.

## Observation 5: the collab subsystem is invisible in ARCHITECTURE.md

ARCHITECTURE.md is this project's distinguishing virtue — every settled decision written
down with a revisit trigger, from i18n-as-non-goal to the plugin trust boundary. It does
not mention `jls.collab` anywhere except in passing inside the #222 plugin-trust entry.
No module-layout entry, no threading note, no recorded decision. Yet the collab
invariants are among the strongest in the repo: sockets confined to `jls.collab.net`,
no Java serialization under `jls.collab`, `jls.collab.ui` as the single Swing-permitted
collab package (`ArchitectureRulesTest.collabLayersAreHeadless`), listener binds only on
Share, `SecureLink` fail-closed. Right now those live only in tests and issue prose.
#168's dialog slice is the moment collab becomes user-visible, and it is the right moment
to land a "Collaboration" section in the module layout plus a recorded decision with a
revisit trigger. That costs an hour and is worth more than any of #814–#817.

## Where the issue is right, and should not be re-litigated

- The #168/#169 boundary ("who the peer is" vs "what the session is doing") is a genuinely
  good seam, and the two dedup comments defended it well. Keep it.
- `Transport` as a sealed frame seam, with `TransportContractTest` running the same suite
  against the loopback double and a real handshaken socket, is the best piece of design in
  the collab tree. It is the reason the session layer can be built and tested without a
  wire, and it should be the template for every future JLS boundary.
- "No listener outside an explicit Share" pinned by a runtime `/proc` probe, not just a
  structural ratchet, is exemplary and matches the #38 hostile-input discipline.
- Cipher swap to AES-256-GCM on a tool alert, rather than dismissing the alert, is the
  right call for a project whose users are institutions running scanners.

## Concrete counter-proposal for the remaining scope

1. **#815 (Share/Join)** — keep, but the join string carries the fingerprint (Obs. 2).
2. **#816 (Verify)** — reframe: words-only, joiner *enters* what was read aloud, machine
   compares (Obs. 3). Shown only for fingerprint-less joins and `KEY_CHANGED`.
3. **#814 (64 glyph images)** — descope to a later polish issue; if revived, procedural
   Java2D rather than sourced art (Obs. 4).
4. **New, ahead of #817** — two-instance loopback vertical: Share → Join → verify → one op
   crosses (Obs. 1). This becomes #168's close-out evidence.
5. **#817 (two-machine LAN record)** — keep as manual evidence, run after (4).
6. **ARCHITECTURE.md collab section + recorded decision** (Obs. 5), landing with (1).

Net effect: the remaining scope loses its licensing dependency, loses its hard display
gate for the security-critical path, gains a stronger security property, and produces a
running end-to-end demo weeks earlier than the current ordering allows.
