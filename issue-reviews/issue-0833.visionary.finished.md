# Issue #833: TASK-C572-1: the jar is CheerpJ-wrapped and served from static files, and click-to-interactive is timed on the three biggest examples against the 15-second line
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the stopwatch away and #833 is one sentence: *make the zero-install evaluation
path real enough to judge.* That end is right, and CAP-32 (#516) argues it well —
#510's teardowns name zero-install as the only structural moat the web tools have,
and a stranger's first click is where JLS currently loses. Nothing below disputes
the capstone.

What I dispute is the apparatus. #833 builds a CheerpJ wrapper and then times it,
which encodes two assumptions the issue never states: that CheerpJ can host *this*
jar at all, and that "run the whole Swing editor in a sandbox" is the natural first
attempt with the SVG+VCD player as a consolation prize. Both are wrong, and the
second is wrong in a way that pulls against the project's recorded architecture.

## The gate that should run before the stopwatch

AC-5 files "payload size and any licensing constraint" *alongside* the timings, as
if they were colour commentary on a number. They are the gate. Four preconditions
are checkable in under an hour, before a single build script exists, and any one of
them ends the spike:

1. **Bytecode level.** `pom.xml:43` sets `<maven.compiler.release>25</maven.compiler.release>`;
   the README's floor is JDK 25+ and follows the current LTS. CheerpJ's shipped
   runtimes have historically tracked Java 8, with Java 11+ arriving late. If the
   available CheerpJ cannot execute Java 25 class files, AC-1 is unreachable and
   AC-2's timings never happen. This is the single most likely outcome of the whole
   spike and it costs one `cheerpjfy` invocation to discover.
2. **Where the runtime comes from.** CheerpJ's loader conventionally pulls its
   runtime from Leaning Technologies' CDN. AC-1 and AC-5 both say "static files
   only, no backend" — a third-party CDN is not a backend the project stands up, but
   it is emphatically a service that can die, which is precisely the property
   CAP-32 AC-3 sells ("nothing that can die and take user data with it", the
   anti-simulator.io pitch). Whether the runtime can be self-hosted, and under what
   licence, decides whether this mechanism can honour the capstone's own promise.
3. **Response headers.** If the runtime needs COOP/COEP for `SharedArrayBuffer`,
   GitHub Pages cannot set them, and "static hosting" quietly becomes "static
   hosting on something that isn't Pages."
4. **Licence.** CheerpJ is a proprietary runtime. JLS is GPL-3.0-or-later, and the
   project's recorded posture (ARCHITECTURE.md, §"Plugin trust boundary", #222)
   is to keep third-party licence hazards on a *subprocess* boundary specifically
   to sidestep in-process linking questions. Wrapping the entire program inside a
   closed runtime is the opposite move, and it deserves a decision, not a footnote.

Reordering these ahead of the wrapper is not pedantry — it changes the issue from
"build an apparatus, then measure" to "disprove cheaply, then measure only if you
must." That alone is most of the 0.5–1 mw band back.

## The reframing: the fallback is the primary, and it is mostly already shipped

CAP-32 ranks CheerpJ first and "headless-rendered interactive SVG with a JS signal
player driven by pre-computed VCD" second. Read against the codebase, that ranking
is inverted, because alternative (a) is assembled almost entirely from surfaces this
project has already built, tested and declared stable:

- **SVG render:** `CircuitRenderer.exportImage` (`src/jls/edit/CircuitRenderer.java:301`)
  already emits SVG through JFreeSVG (`pom.xml:70-72`, `org.jfree.svg` 5.0.7),
  drawing element by element at line 348. `-i out.svg` ships today and #551 is
  already building a gallery on it.
- **Signal data:** `BatchSimulator`'s `-vcd` export (`src/jls/sim/BatchSimulator.java`,
  `toVcd`/`vcdValue`) is a documented stability contract (`docs/batch-interface.md`
  §3.2) with golden tests (`VcdExportGoldenTest`).
- **Missing piece:** the SVG has no per-element identity. The comment at
  `CircuitRenderer.java:313` is explicit — ".svg output needs no per-element work."
  A VCD-driven player needs `<g id="net-…">` handles to recolour. JFreeSVG exposes
  `SVGHints.KEY_BEGIN_GROUP`/`KEY_END_GROUP` rendering hints for exactly this;
  wrapping the existing per-element loop emits stable ids from inside one method.

That is the real unknown in this entire capstone, and no issue in the C32 tree
touches it. **The spike worth running is: can the shipped SVG exporter emit stable
per-net handles, and can a VCD drive them in a browser?** Measured on the same
circuits, the answer arrives with a payload in the low hundreds of kilobytes, no
third-party runtime, no CDN, no licence question, and a click-to-interactive figure
that will not be near 15 seconds — it will be near one. And it pays forward twice:
#551's static gallery becomes hoverable for free, and #886's URL-fragment sharing is
plausible against a lightweight player and frankly absurd against a multi-megabyte
Java runtime.

## "Read-only by construction" is false for the CheerpJ path

FEAT-C32-1 AC-3 and its sibling #835 require the demo path be read-only *by
construction, not by intention*. A wrapped jar cannot satisfy that. The artifact
being served is the whole editor: `SimpleEditor`'s ~4k-line mouse state machine,
the File/Save-As menu, the undo stacks, and `writeCheckpointInBackground` writing
`.jls~` files (ARCHITECTURE.md, "Threading model"). Read-only there means "we hid
the menu items and the sandbox has no disk" — read-only by *configuration*, which is
exactly the distinction #835 says it will not accept. An SVG plus a JS scrubber has
no save path to disable; the property is structural because there is no editor
present. The capstone's strictest constraint is satisfied for free by the mechanism
it ranked second.

## Alignment with the project's arc

`docs/grand-architecture.md` §1 states the load-bearing fact plainly: "The
architecture may not assume a network, a server, or an install step." The same
document names the two co-equal front ends, and the batch surface — VCD, SVG,
`-t` vectors — is the one with a written stability contract and byte-reproducible
outputs. Every neighbouring decision leans the same way: collab is "pure-P2P, no
server"; external tools sit behind subprocess boundaries; artefacts are reproducible
and attested.

A demo built from `-i out.svg` and `-vcd` *is* that architecture pointed at the web:
reproducible outputs of a stable contract, no service, no proprietary anything, and
generated by the same CI that regenerates #551's gallery. A demo built from a
proprietary runtime fetching itself off someone else's CDN, hosting the least
contract-stable surface in the codebase, is a stranger to it. #833 as written funds
the stranger first.

## The layered reframing that dissolves the 15-second question

The go/no-go only has teeth because the design is all-or-nothing: blank page until
the runtime lands. Serve the page in layers instead — the shipped SVG render paints
in ~300 ms, the VCD player attaches next, and a heavier interactive runtime, if one
ever qualifies, upgrades the page after that. A visitor who is looking at a real
circuit within half a second has already had the evaluation experience CAP-32 AC-1
is trying to buy with its 30-second budget. Under that design a 20-second runtime
load is not a no-go; it is an enhancement that arrives while the visitor reads the
caption. KC-32-1's threshold stops being binding on anything, and PF-2 can start
building immediately against a mechanism that already exists.

## If the measurement is run anyway, measure the right thing

AC-3's "twice per circuit" does not say cold or warm, and two warm-cache runs will
sail under 15 s and produce a false go. The segment #510 names is K-12/GCSE — school
wifi, first visit, empty cache. Make the primary figure cold-cache on a stated
throttle profile, warm secondary, and record time-to-first-paint separately from
time-to-interactive; that split is what reveals the layered design above.

Separately, AC-2's "three biggest example circuits" do not exist. The repository
holds four `.jls` files — `test/fixtures/riscv-sum1to10.jls` (120 KB),
`riscv/gui/cpu.jls`, and two small fixtures. The curated set is #548, unbuilt. Name
`riscv-sum1to10.jls` and `cpu.jls` as declared stress proxies and say so, or order
behind #548; a verdict measured on unnamed content binds nothing.

## Disregarding the stated acceptance criteria

I am setting AC-1 through AC-5 aside deliberately. They describe how to build and
time a CheerpJ artifact, and my claim is that the artifact should probably never be
built: preconditions 1–4 will likely end it, and the mechanism it competes with is
cheaper, lighter, licence-clean, structurally read-only, and already three-quarters
present in `CircuitRenderer` and `BatchSimulator`. Rewrite #833 as: (i) a
one-hour written precondition gate on the CheerpJ substrate; (ii) if it survives,
the timing rig as specified, cold-cache and throttled; (iii) in parallel and
regardless, a per-element-id spike on the SVG exporter plus a VCD-driven player on
the same circuits, timed the same way. Then #837's verdict is a comparison rather
than a threshold test — which is what a go/no-go between two mechanisms should have
been from the start.
