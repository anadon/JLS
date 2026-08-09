# Issue #760: TASK-C545-1: the README shows the product above the fold — two screenshots and a drawing-and-simulating GIF, with a drift check that fails on a missing image
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#511 verified the diagnosis and it is not in dispute: the README has zero images, a
switcher bounces in ten minutes, and this is the cheapest capstone on the board. #760
is the one task that converts "JLS is invisible" into "JLS is visible." I endorse the
goal without reservation.

What I want to argue with is the *substance* the issue assumes the images are made of.
#760 treats the three assets as **captured pixels** — hand-grabbed now, grabbed by
#586's compositor rig later — pinned by a check that the file path exists. Every other
externally-visible claim in this repository is a **build product pinned by an oracle**:
the jar and `bom.json` are byte-reproducible and CI re-checks them per push; the CLI
flag table is cross-checked by `CliFlagTableTest`; the help tree's every `href`/`src`
is resolved case-sensitively by `HelpTopicsTest`; the seam catalog is checked in both
directions; the semantics spec has golden oracles. A path-existence check over
hand-grabbed PNGs would be the weakest and the only decorative oracle in the tree — a
blank, stale, or wrong-UI image passes it forever.

The reframing is that JLS does not need to *photograph* itself. It can *render* itself,
and the code to do it already ships.

## Reframe 1: the still images are already a build product — today, with no rig

`CircuitRenderer.exportImage` (`src/jls/edit/CircuitRenderer.java:301`) draws through
the **same element paint path the editor canvas uses** — the class docstring says so
outright, and `draw(Graphics, Set, @Nullable SimpleEditor)` takes a null editor. `jls -i
out.svg circuit.jls` therefore emits, from a headless JVM with no display, the exact
pixels a reader would see inside the editor's canvas. Better: the SVG path was
deliberately made deterministic (fixed `defs` prefix, stable wire/part draw order) and
`SvgExportTest` asserts two exports are byte-identical across fresh loads, "so goldens
and reproducible-builds expectations hold."

That means the "circuit drawn" image can be a regenerable, byte-pinned artifact from day
one, produced by one documented command on a fresh clone, with the drift check being the
same kind the jar already has: regenerate, compare bytes, fail on difference. #511 PF-4
already knows this (`SVG renders (the -i out.svg export already ships)`); #760 does not
mention `-i` anywhere. The chrome (toolbar, menus, trace window) is the only part that
genuinely needs a screen grab, and it is the part a stranger looks at least.

## Reframe 2: the GIF is a render loop, not a screen recording

The adversarial review is right that no charted path produces a GIF: `wayland-rig.sh`
calls `grim` for single PNGs, #101 explicitly scopes out `wtype` interaction scripting,
and #586 disclaims anything beyond boot-and-screenshot. The conclusion drawn from that
is "defer the GIF." I think the conclusion is "stop trying to record a screen."

Two in-tree routes already exist:

- **Simulate half.** `BatchSimulator` is headless by construction and advances event
  time; `CircuitRenderer` paints a circuit at whatever state it is in. Step the
  simulator, paint a frame, repeat — then encode with the JDK's own animation-capable
  GIF writer (`ImageIO`'s `GIFImageWriter.canWriteSequence`). No compositor, no JBR, no
  `wtype`, no `grim`, no #91 dependency, and deterministic to the byte because nothing
  outside the JVM touches it.
- **Draw half.** `test/jls/ui/EditorGestureSupport` already boots a real `Editor` and
  drives its mouse state machine with synthetic `MouseEvent`s on the EDT — "deterministic
  and fast instead of fighting Robot/Xvfb timing." That *is* the drawing script, already
  written, already replayed in CI's `display` lane. Painting the canvas after each
  gesture yields the drawing frames.

The payoff is larger than one image. A frame generator of this shape is a renderer
regression oracle: the same frames can carry `RenderAssert`-style semantic checks, and
"no image outlives the UI it depicts" becomes true *per build* for canvas content rather
than per release. That is CAP-35's stated outcome, reached without CAP-35's apparatus.

## Reframe 3: don't build `ReadmeOnboardingTest` — generalize the checker that exists

AC-2 asks for a bespoke check that README-referenced image paths exist. `HelpTopicsTest`
(#70) already implements exactly that algorithm, more carefully: a regex over
`href`/`src`, resolution against the names as they land in the jar, case-sensitive so
`down.gif` vs `down.GIF` fails. Writing a second, weaker copy for one file is the
duplication this project's own architecture notes keep warning about.

The elegant version is one `DocLinkTest` over README + `docs/*.md` + the help tree. The
README today carries roughly thirty unverified relative links — `docs/reproducibility.md`,
`docs/vcd-interop.md`, `scripts/wayland-rig.sh`, `.devcontainer/Dockerfile`,
`pop_GPLv3.pdf` — none of which any test resolves. Same implementation cost, an order of
magnitude more surface covered, and it lands the check in the family it belongs to
instead of inventing a one-off.

## Reframe 4: "above the fold" is a document-architecture problem, not a placement problem

The README is 368 lines and lines 12–147 are a packaging and supply-chain manual: deb,
rpm, AppImage, Nix, MSI, Authenticode custody, DMG Gatekeeper instructions, SHA256SUMS,
attestation scope, cosign invocations, GPG-signing rationale. Putting three images at
line 4 does not make a stranger see the product for ten seconds; it makes them see the
product for three seconds and then scroll into `sudo apt install ./jls_*.deb`.

The structural fix — and the one that actually delivers #511's outcome — is a split:
**README becomes the shop window** (one line of what it is, the images, a sixty-second
quickstart, the #762 positioning block, links out), and **INSTALL.md inherits the
packaging manual it currently is**. That is a genuinely small diff, it is the change that
makes "above the fold" structurally true rather than pixel-true, and it un-blocks #762,
which wants the first screen to state positioning rather than describe `.deb` filenames.
If a 0.5–1 mw task cannot authorize that move, the issue should say plainly that it is
buying a veneer over a reference document, and file the split — because two siblings now
need it.

## Reframe 5: the screenshot's subject should be a circuit the reader can then open

`examples/` contains one Python autograder and no circuits; the only `.jls` files in the
tree are three test fixtures and `riscv/gui/cpu.jls`. So the "circuit drawn" image has no
subject yet, and #760's ordering does not mention PF-2 (#511's example library) at all.
Bind the README images to circuits that *ship* in `examples/`, and the image stops being
an advertisement and becomes a door: "this is `examples/alu-4bit.jls`; open it from
File > Examples." One asset then serves the shop window and the on-ramp, and Reframe 1's
regeneration command has something durable to point at.

## On AC-3: I am disregarding the hand-capture/migration framing

AC-3 carefully preserves a conflict rather than dissolving it: hand-commit now, state
that you did, and accept that #797 will later turn the image red by design. The comment
defends this as a "known migration." A known migration is still rework, and it is
avoidable for the price of a data file. Land the **manifest schema** here — `path`,
`source` circuit, `command`, `capture: generated|manual`, `as_of` version — and register
these three images in it. #586 then implements a runner against a populated manifest and
#797's ratchet has something to check on day one; nothing ever needs migrating. Under
Reframes 1–2 the stills and the GIF are `capture: generated` immediately and the conflict
never arises at all.

## Where this strengthens the arc

JLS's real differentiators are exactly the ones a stranger cannot see: a normative
semantics spec with golden oracles, a documented grading contract, byte-reproducible
builds. A shop window built out of *generated, byte-pinned renders of shipped example
circuits* is not merely cheaper than a screenshot rig — it is the same claim as the rest
of the project, made visually. The hand-captured version says "trust this picture." The
generated version says "run this command and get this picture," which is what every other
artifact in this repository already says.

## Recommendation

Endorse the outcome; rewrite the route. (1) Produce the stills via `-i out.svg` from
shipped `examples/` circuits, pinned by a byte-identity golden in the `SvgExportTest`
idiom — no capture rig, no #586 dependency. (2) Produce the GIF by stepping
`BatchSimulator` and `EditorGestureSupport` and encoding frames with `ImageIO`, not by
recording a compositor; state explicitly that the screen-recording route is rejected.
(3) Replace the bespoke `ReadmeOnboardingTest` with a repo-wide `DocLinkTest` extending
#70's checker. (4) Split INSTALL.md out of README so "above the fold" is architectural.
(5) Land the #586 manifest schema here as data so #797 never has a migration to perform.
