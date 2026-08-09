# Issue #835: TASK-C572-2: toggling an input and watching the trace either works in the browser or does not, per circuit — and the demo path is read-only by construction, not by intention
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this task is really for

Strip the CheerpJ framing and #835 is asking one question on behalf of CAP-32
(#516): *can a stranger with no JDK poke a JLS circuit and see it respond, from
static files, without JLS acquiring a server or an attack surface?* The four
substantive ACs (interaction works, it looks like JLS, nothing writable is
reachable, nothing is fetched at runtime) are not observations about a wrapped
jar. They are the **selection criteria for the mechanism**. Written as a
measurement task on an artifact TASK-C572-1 (#833) is presumed to have built,
they land in the wrong order: three of the four are properties that a mechanism
either grants for free or makes structurally unprovable, and no amount of
careful browser-panel recording changes which.

## Why the task as written probably cannot be executed

**The artifact under test likely cannot be built.** `pom.xml` sets
`maven.compiler.release` 25 and the enforcer requires JDK `[25,)`; the source
uses records (`src/jls/Help.java:196` `record TocEntry`), pattern-matching
`instanceof` throughout `src/jls/Circuit.java`, and pattern `switch`
(`src/jls/JLSStart.java:679`). CheerpJ's supported bytecode level has
historically trailed the JDK by many years (the 3.x runtime is Java 8-class).
Class-file v69 is not a version-flag away from that; it is a backport of the
whole codebase. **This is the first thing anyone should check, it costs an
afternoon, and neither #833 nor #835 schedules it.** #835 sits behind #833 in
`ordering_after`, so the fleet's most expensive spike task is queued behind a
build that may be impossible for a reason nobody has looked up.

**AC-4 is unsatisfiable by the mechanism it is meant to validate.** CheerpJ's
runtime is delivered from the vendor's CDN (`cjrtnc.leaningtech.com`);
self-hosting it is a licensing question, not a configuration one. So "no
network endpoint is contacted at runtime beyond fetching the static assets"
is likely **false by construction** for a CheerpJ build — as is #572 AC-5
("static files only") and CAP-32 AC-3 ("nothing that can die and take user
data with it"). A demo whose liveness depends on a third-party CDN is the
precise failure mode the capstone cites against simulator.io. For a project
whose README spends sixty lines on attestation, byte-reproducible jars, and
self-contained installers, adding a proprietary, remotely-served runtime to
the distribution story pulls hard against the arc.

**AC-3 is not verifiable under CheerpJ in the strong sense it claims.**
"Read-only by construction, verified by inspection of what the wrapper exposes,
not by the absence of a button" — but the wrapper exposes a JVM. `jls.edit.Editor`
extends `SimpleEditor` precisely to add save/save-as; `SimpleEditor` also runs a
background checkpoint writer (`writeCheckpointInBackground`) into a virtual FS.
Proving non-reachability inside an opaque JIT means proving something about
reflective element loading (`SaveTags.resolve` → `getConstructor(Circuit)`),
menu wiring, and CheerpJ's own filesystem shims. That is a research project, and
its honest answer is "we removed the menu items," which is exactly the "absence
of a button" the AC forbids.

## The reframing: fallback (a) is not a fallback, it is the aligned answer

KC-32-1 ranks "headless-rendered interactive SVG with a VCD-driven JS player"
as the consolation prize. In this repository it is **already most-built**:

- `src/jls/edit/CircuitRenderer.java:312-358` exports true vector SVG via
  JFreeSVG through *the same per-element paint path the editor uses*, in a
  deterministic draw order, byte-identical across runs
  (`test/jls/SvgExportTest.java`, `test/jls/ElementDrawSmokeTest.java`).
- `-vcd` emits IEEE 1364-2001 VCD, deterministic and verified in CI by a
  spec-derived parser (`test/jls/VcdExportGoldenTest.java`,
  `docs/batch-interface.md` §4 — a documented stability contract).
- Batch mode is headless by construction and enforced so
  (`HeadlessCoreRatchetTest`); `ghcr.io/anadon/jls` already runs exactly this
  surface for autograders.

Now watch every AC of #835 collapse:

| #835 AC | CheerpJ | SVG + precomputed VCD |
|---|---|---|
| AC-1 toggle → trace changes | measure, hope | **build invariant**: precompute one VCD per input vector at build time; a toggle selects a trace |
| AC-2 looks like JLS | screenshot pairs, eyeball | **identical by construction** — JLS's own draw path emitted it |
| AC-3 read-only | unprovable claim about a JVM | **no JLS code in the browser at all** |
| AC-4 no runtime network | false (vendor CDN) | trivially true, one-line CSP-testable |

The concrete design: for a curated circuit with *k* toggleable inputs, run
`jls -b -t <vector> -vcd` over the 2^k input assignments (small by curation) in
CI, emit `circuit.svg` plus a compact trace table, and ship a ~200-line vanilla
JS player that does nothing but recolor SVG nodes from the table as the scrubber
moves. Fidelity is then **JLS's own simulator output, golden-tested**, not an
approximation. The only source change needed is a per-element `<g id="...">`
wrapper at the two `ElementRenderers.draw(svg, el)` call sites in
`CircuitRenderer` — a genuinely small seam, and one that also serves #551's
static gallery and #574's "try it" links.

**Scope cliff this route must carry** (and #572 never states): the player must
**replay only, never evaluate**. A JS evaluator would be a second simulation
execution strategy, which ARCHITECTURE.md's recorded #221 decision forbids
("the event-queue interpreter remains JLS's *only* simulation execution
strategy"). Write that into the fallback's definition or it will drift there.

**Honest cost of the redirect:** a JS+SVG player is new code in a new language
in a single-maintainer Java project, and combinatorial precomputation caps
interaction at curated circuits with few inputs (a 32-bit datapath is not
toggleable this way). CheerpJ, if it worked, would need no new JLS code and
would generalize. That trade is real — but it is a trade between "a small
build-time artifact this repo's existing exports nearly emit" and "a proprietary
CDN-served runtime plus a Java-8 backport," and the second side has no path.

## Disregarding the stated acceptance criteria

I am explicitly setting aside AC-1 through AC-5 as written. They presuppose the
mechanism, and the mechanism is the thing in question. Restructure the spike:

1. **#833 → a one-day disqualification check, not a build.** Answer three
   lookups: (i) does any CheerpJ release accept Java 25 bytecode? (ii) can its
   runtime be self-hosted from static files, under what licence? (iii) what is
   the licence of the wrapper output for a GPL-3.0-or-later project? Any "no"
   ends PF-1 immediately, and #837's verdict document writes itself with better
   evidence than a stopwatch would have produced.
2. **#835 → measure the SVG+VCD route instead**, with its own #835-shaped ACs:
   payload size for the three chosen circuits, click-to-interactive (it will be
   a rounding error), and a demonstration that a toggle selects a precomputed
   trace. Keep AC-2's screenshot-pair discipline only as a regression guard on
   the SVG path, where it is cheap.
3. **Pick the three circuits first.** `examples/` contains only
   `autograde/autograde.py`; there is no curated example library in-tree (that
   is #548, open, and #572's `ordering_after` is empty even though CAP-32 orders
   after #511). "The three biggest example circuits" currently resolves to
   `riscv/gui/cpu.jls` and `test/fixtures/riscv-sum1to10.jls` — the latter being
   a 120 KB CPU that is the *worst* possible demo subject and unusable under any
   precomputation scheme. This task cannot pass AC-1 against a content set that
   does not exist.
4. **Tell #886 (share-by-link) early.** It records itself as `blocked_by` #572;
   under the SVG route a fragment-encoded *circuit* has nothing to run it, so
   that feature genuinely re-plans rather than quietly degrading — which its own
   KC-32-4-1 anticipates but nobody has triggered.

## Larger arc

CAP-32's real prize is evaluation-cost parity, and the survey's own permanence
pitch says the win is a page that cannot rot. The SVG+VCD route makes the demo a
**build artifact of the release pipeline** — reproducible, attestable, hostable
on GitHub Pages, dead-simple to regenerate at every tag — which is the same
property README claims for the jar and BOM. The CheerpJ route makes the demo a
**dependency on someone else's live service**. One of those is this project's
character; the other is the thing every recorded decision in ARCHITECTURE.md
has been steering away from.

## What would reverse this

A CheerpJ (or TeaVM/JWebAssembly) release that accepts current-JDK bytecode
*and* permits fully self-hosted static runtime delivery under a licence
compatible with GPL-3.0-or-later. Then the fidelity question becomes real again
and #835 as written is the right task. Until one of those exists, this issue is
measuring a thing that has not been shown to be buildable.
