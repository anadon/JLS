# Issue #704: TASK-C535-2: a canvas click adds the signal in an external viewer and the viewer's cursor moves JLS's time — one way only, and absent the viewer nothing is lost
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Stripped of mechanism, the claim is: *a JLS session should be continuous with the
tooling a student will meet in industry.* CAP-23 (#504) names it "the professional
handoff" and CAP-23 §3.5 admits the mechanism is speculative ("cursor-sync protocols
are young"). The value being bought is largely an **identity claim** — JLS is not a toy,
it plugs into real waveform tooling — and only secondarily a workflow.

That is a good thing to want. The socket is not how to get it.

## The prerequisite nobody owns, and why it changes the whole answer

The outcome says "clicking a wire in JLS adds that signal in the viewer." A viewer adds
a signal *from a waveform it has loaded*. **JLS's interactive session produces no
waveform artifact at all.**

```
$ grep -rniE 'vcd' src/jls/edit/InteractiveSimulator.java src/jls/edit/Trace.java
(no output)
```

Every VCD line in the tree is in `src/jls/sim/BatchSimulator.java` (`setVcdFile`,
`writeVcd`, `toVcd`, ~L335–L475); `jls.edit.InteractiveSimulator` (1437 lines) and
`jls.edit.Trace` (626 lines) hold their history in `TraceSample`/`Change` lists that
never leave the process. Nothing in CAP-23's PF-1..PF-6, in FEAT-C23-6 (#535), in
TASK-C535-1 (#702), or in this task says "the interactive session emits a waveform."
#405 (TASK-0010) streams the *batch* dump and is blocked on #373.

So #704 as filed is either unimplementable, or it silently smuggles in a much more
valuable unfiled prerequisite. Once you file that prerequisite honestly, the socket
mostly evaporates — which is the reframing.

## Reframing 1 (recommended): the recording is the handoff

Fund **"an interactive session is a recording device"**: the live simulator writes its
session as a VCD as it runs, through the same emitter batch mode uses. Then:

- *"Open in external viewer"* = flush the recording, launch whatever the user has,
  point it at the file. Works with GTKWave **and** Surfer, and with any viewer that
  ever exists. No version pin, no protocol, no handshake.
- *"Clicking a wire adds that signal"* = write the signal selection into the viewer's
  **own session file** — GTKWave's `.gtkw` save file, Surfer's state/command file — and
  reload. That is a text artifact JLS can emit and CI can pin **byte-for-byte against a
  golden**, exactly like `VcdExportGoldenTest` already does for the VCD. Offline,
  hermetic, no GUI in the loop.
- The cursor time rides in the same sidecar, JLS → viewer.

This delivers three things the socket does not: a student's exploratory session becomes
a reproducible artifact they can attach to a bug report; the grading lineage
(#300/#306/#502 — #508's *first-funded* wedge) gains an interactive-to-batch bridge; and
`RewindEqualsReplayTest`-style claims get a real oracle. It is strictly larger value at
strictly lower risk, and it lands on the arc #508 is paying for rather than beside it.

What it does not deliver is viewer-cursor → JLS-time. Judge that half on its own merits:
a student whose eyes are in the waveform viewer is not watching the JLS canvas, and
CAP-23's own thesis is that the *in-tool* loop is the differentiator no competitor has.
The back-channel is the least valuable half of the least defensible feature in the
capstone. Cut it.

## Reframing 2 (if the scope survives anyway): pin a protocol, not a binary

AC-2 — "the viewer version is pinned in-tree; an unpinned or mismatched viewer is
refused **by name**" — is name-based trust, and it makes JLS's compatibility surface a
third party's release cadence. The repo already has the right pattern for this class of
boundary: `docs/grand-architecture.md` §4.3 / the #222 ratification say an
out-of-process integration gets **a message schema and a transport, in its own design
issue**. The correct shape is a documented JSON command vocabulary with a version
handshake and capability negotiation — refuse on *capabilities*, degrade on unknown
commands, work with any viewer that speaks it. Then JLS is implementing an open
protocol (a thing a fork with zero adoption can plausibly be *cited for*), not driving
one program.

## The architectural cost the issue does not price

Today the guarantee "external tools cannot influence a JLS simulation" is **structural**:
there is no live channel. #63 rejected co-simulation, #216 closed on the file handoff,
and `docs/vcd-interop.md` states it normatively ("Graders must not depend on interacting
with a running simulation"). AC-4 replaces that structural guarantee with a **policy
guarantee plus a ratchet test** — permanently, and for a feature the funded plan wants
cut. A socket into a running simulator, once it exists, is the thing every future
"couldn't we just…" request points at; the test asserts today's code has no path, not
that next year's will not. "There is no channel" is a stronger and cheaper invariant
than "there is a channel and a test says nothing flows the wrong way." Spending the
project's first bidirectional live IPC on a cursor is a bad trade for a boundary this
project has defended three separate times.

## AC-1 cannot buy what it claims to buy

- The tree already records the opposite decision: `test/jls/VcdExportGoldenTest.java`'s
  header — *"GTKWave/Surfer validation stays manual, outside CI."*
- The existing external-tool lane (`test/jls/hdl/ToolLocator.java`, `iverilog`, `ghdl`,
  `yosys`, `python3`) gates **headless CLI** tools that skip when absent. A GPU-rendering
  desktop waveform viewer is a different class of dependency in a project whose identity
  is the offline self-contained jar and reproducible builds.
- The half AC-1 exists to prove — *viewer cursor moves, JLS follows* — requires
  originating a gesture inside a third-party GUI. A scripted test will inject the event
  on the wire instead, at which point the pinned binary contributes nothing to that
  assertion but flakiness. The honest test is a conformance test against an in-repo fake
  peer, plus a recorded manual check — the `VcdExportGoldenTest` precedent, already
  ratified.
- `test/jls/ui/package-info.java` layer 2 is "present, growing," not a harness that
  drives a foreign process today.

## Alignment

- It **pulls against** the capstone's own thesis. CAP-23's abstract is that the in-tool
  loop is what nobody else has; this task builds the exit ramp to somebody else's tool.
- It **duplicates** a seam that should be built once: session recording (#405 / #498 M2)
  is the shared substrate for handoff, bug reports, grading artifacts, and rewind oracles.
  Two mechanisms for "get this run into a viewer" is the same drift §3.1 forbids for
  cause-chain models.
- It is **self-cancelling as filed**. #535's boundary note and this issue's own
  "Recorded before work starts" say #508 recommends cutting it, and it is filed anyway
  "for capstone coverage completeness." A planning graph is a model of the plan; keeping
  the model complete is not a reason for work to exist, and #508's process findings
  already indict tracker mass. A one-line REPLAN on #504 carries the same information at
  zero cost.
- Priced at 1–1.5 mw against bus factor 1, zero external adoption, and a live user base
  on a *different fork* — this is the last mw in the capstone I would spend.

## What I would keep, and what I am explicitly disregarding

**Keep as free wins:** AC-3 (degradable — true today and stays true under Reframing 1),
AC-5 (GTKWave remains the documented file path — already normative in
`docs/vcd-interop.md`), and AC-4's *intent*, best honoured by leaving the channel absent.

**Disregarding deliberately:** AC-1 and AC-2. Not because a viewer bridge is
unachievable, but because they specify a CI shape this repo has already decided against
and a trust model (`refuse by name`) that its own architecture document supersedes. I am
also disregarding the issue's framing that "add signal" and "move cursor" are one
feature: the JLS→viewer direction is a file artifact, the viewer→JLS direction is a
socket, and bundling them is what forces the expensive answer for both.

## Suggested shape

1. File "the interactive session is a recording": live VCD emission from
   `InteractiveSimulator` through `BatchSimulator`'s emitter, against #405/#498 M2.
2. Add "open in external viewer" as recording + viewer session-file sidecar, pinned by a
   golden. That is the professional handoff, offline and version-tolerant, at a fraction
   of 1 mw.
3. Close #704 with a REPLAN on #504 resolving PF-6's sync half into (1)+(2) and recording
   that the viewer→JLS back-channel is dropped per #508 §6 and the #63/#216 stance.

*(Incidental, while verifying: `ARCHITECTURE.md:170` cites
`src/jls/sim/InteractiveSimulator.java`; the file is `src/jls/edit/InteractiveSimulator.java`.)*
