# Issue #817: TASK-C168-4: two real machines on a LAN join and verify by reading glyphs aloud, and the record says which machines, which build, and what was on screen
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of procedure, #817 asserts one thing: *a loopback test does not prove the
Share/Join/Verify ceremony works between two hosts on a real network, so make the claim
by hand once and write it down.* That instinct is right and it is the project's instinct
— #100's headless-sway lane got a real-desktop spot-check for exactly this reason, and
`docs/wayland-desktop-checklist.md` exists because a software-rendered rig can diverge
from the thing users run.

But "the one claim CI cannot make" is not one claim. It is three, and they have very
different substrates:

1. **Two hosts, real NICs, non-loopback addresses, a firewall in the path.** Not
   irreducibly manual. Two containers on a user-defined bridge, or two network
   namespaces with distinct /24 addresses, give the JVM real sockets on real
   interfaces with a real routing decision. Nothing about that needs a human.
2. **The glyphs on screen are the glyphs of *this* handshake.** Also not manual — and,
   more importantly, *not currently pinned by anything*, see below.
3. **Two humans can compare seven spoken nouns and act correctly on the answer.** This
   one is genuinely human, and it is the smallest of the three.

#817 as written buys (3) at the price of doing (1) and (2) by eyeball, once, and filing
the result as a GitHub comment with attached screenshots. That is the inversion I want
to argue against.

## Reframing A — the LAN run is a rig, and this repo already knows how to build rigs

`scripts/` holds `wayland-rig.sh`, `x11-rig.sh`, `macos-rig.sh`, `windows-rig.ps1`,
`icestick-handoff.sh` — every one of them a wrapper around something that "obviously"
needed a human, and every one of them paired with a `-selftest` that proves the wrapper's
verdict logic without the exotic hardware. The `gui-wayland` lane boots a Swing GUI under
a compositor, asserts window presence via `swaymsg`, and screenshots it with `grim`, on
every push. This project's whole character is *convert the manual claim into a script,
then self-test the script*.

Concretely: `scripts/collab-lan-rig.sh` brings up two containers on a user-defined
bridge (distinct addresses, no loopback anywhere), runs a headless sway or Xvfb in each,
launches JLS in each, drives Share on one and Join on the other, screenshots both Verify
dialogs, and — the part a human cannot do reliably — asserts the two rendered glyph
panels are pixel-identical and that both sides' `Sas` values equal each other and equal
the transcript-derived value. Add `--drop-inbound` to install a DROP rule on the
listener side and assert the Join failure is the typed, user-readable one (#815 AC-4)
rather than a hang. Then AC-1, AC-3 and AC-4 of this issue become a lane that re-runs on
every push, and the screenshots become CI artifacts rather than issue attachments.

That rig is also the only way the reconnect claim (AC-4) stays true. A one-shot human
run proves "no second prompt" at one commit; the dialogs will change, and nothing will
notice.

## Reframing B — the ceremony is about the mismatch, and nobody is testing the mismatch

This is the finding I would act on first, and it spans #816 and #817 together.

`HandshakeTest` exhaustively tampers every byte of every handshake message and detects
100% of it. The *protocol* is pinned. What is not pinned anywhere is that the seven
images a user stares at were derived from this session's transcript. Read #816 AC-1:
"both Verify dialogs render the same seven glyph images with matching words". A dialog
that renders `Sas` of a constant passes it. So does a dialog that renders the peer's
claimed value instead of the locally derived one — the classic SAS implementation bug,
and the only bug in this whole feature that actually gets a student MITM'd. Then #817
AC-1 ("both operators see matching SAS glyphs and words") passes it a second time, by
two people who came into the room expecting a match and who will read seven nouns to
each other in a tone of confirmed agreement.

Both tasks verify the happy path of a ceremony whose entire reason for existing is the
unhappy path.

The fix is small and belongs in-tree: a test-only relay that terminates the joiner's
handshake and re-originates its own to the sharer — an honest MITM, buildable from
`SocketSession` and `SessionListener` as they already stand. Two assertions follow
mechanically: the two sides' SAS values differ (with overwhelming probability over 42
bits), and the two rendered panels differ. Run it headless in `mvn verify` and rendered
in the GUI lane. *Then* the human step has content: run the drill twice, once real and
once relayed, without telling the operators which is which, and record that they caught
it. That is a manual run that produces information. The one #817 specifies cannot fail
in any way that would surprise anyone.

I am explicitly disregarding AC-1 as the criterion that matters. "Both operators see
matching glyphs" is the trivial case; "the operators refused to Confirm when the glyphs
differed" is the claim #168 actually promises its users.

## Reframing C — address selection is code, not a "discrepancy"

AC-5 lists address selection alongside timeouts and firewall behaviour as something to
be "recorded as a finding with a named owner issue". But `docs/collaborative-editing-research.md`
§5.2 step 1 already specifies the behaviour — the join string lists *all* candidate
addresses, LAN first — and `grep -rn "NetworkInterface" src/` returns nothing. #815's
AC-1 says only "displays a join string". On any machine carrying `docker0`, `virbr0`,
`tailscale0` or a VPN tun — i.e. a maintainer's machine and half of a university lab —
whatever single address that dialog picks is a coin flip, and the failure mode is a
student typing an address the other side cannot route to.

So the most likely outcome of #817-as-written is that the LAN run *discovers* the
missing address-enumeration work and files it, after the dialogs have landed and after a
human has spent an afternoon. Move it left: give `SessionListener`/#815 an explicit
candidate-address enumeration with a documented ordering (site-local IPv4 first, then
ULA/link-local IPv6 with scope, loopback last and labelled), pinned by a headless test
against synthesised interface sets. Then the LAN run *checks* address selection instead
of tripping over it.

## Where the evidence should live

AC-2's parenthetical — "in-tree (or in the closing comment on #168)" — is the one clause
I would strike outright. This repository's manual-evidence precedent is unambiguous and
in-tree every time: `docs/wayland-desktop-checklist.md` (a standing per-release
procedure with a results template), `docs/keyboard-a11y-verification.md`,
`docs/icestick-bitstream-handoff.md`. A closing comment is unversioned, undiffable,
un-greppable, and rots silently the first time the Share dialog's wording changes. Ship
`docs/collab-lan-checklist.md` with a filled-in results block, exactly the Wayland
shape, and let the GitHub comment be a pointer to it. Screenshots likewise belong as
rig artifacts, not as blobs hanging off an issue — this is a project that publishes
reproducible jars and provenance attestations; its trust-ceremony evidence should not be
the one artifact stored outside the repo.

## What is genuinely left for humans

After A, B and C, the residue is real but small, and it is a per-release checklist row
rather than a task: an actual Wi-Fi/switch path (containers on a bridge do not exercise
AP client isolation, which is exactly what a campus network does), a stock distro
firewall in its default state (ufw/firewalld, not an iptables rule the rig installed
itself), and the legibility question — can two people distinguish "acorn" from "anchor"
and "bell" from "shell" across a lab bench, in the order shown, without miscounting to
seven. That last one is a property of the *vocabulary* (#814/`Sas.WORDS`), not of this
run, and it is worth a deliberate look: the current 64 contain `apple`/`arrow`,
`bell`/`balloon`, `cloud`/`clover`, `moon`/`mushroom`, `rose`/`rocket`, `river`/`rainbow`
— several near-minimal-pair openings for a spoken protocol, and one of the few things a
human drill can genuinely measure.

## Alignment with the arc

#168 serves #163, whose destination is classrooms — many machines, campus networks,
students who will not debug a socket. One two-host run at one commit is a shallow sample
of that world, and it closes a feature on evidence that cannot be re-taken. The rig
version samples it on every push and leaves a per-release human drill for the parts a
container cannot fake. That is the same trajectory the project already chose for Wayland,
X11, macOS, Windows and the iCEstick handoff; #817 as written is the only manual-evidence
task in the tree that does not follow it.

Keep the issue. Keep the human run. Change what it is a run *of*: land the rig and the
MITM drill first (as work in #815/#816 or as a new sibling), then have two humans execute
the mismatch drill on a real network and sign an in-tree checklist. Same afternoon, a
claim that survives the next refactor.
