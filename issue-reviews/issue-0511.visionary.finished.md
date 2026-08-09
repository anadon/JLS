# Issue #511: CAP-27: a prospective user goes from first hearing of JLS to a running, understood example circuit in under ten minutes — without reading anything longer than a caption
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The goal is right, and it is the right goal to be biggest

Everything in #510 §2 converges on one fact: JLS is category-best on exactly one
axis (testing/grading, 5/5) and its evaluation cost is high enough that nobody
reaches that axis. A capstone whose outcome is "cut evaluation cost below the
bounce threshold" is correctly the cheapest thing on the board and correctly
gates CAP-16/21/29/33. I do not contest the outcome. I contest the seam the six
planned features cut along, and I am explicit below about which acceptance
criteria I would drop.

**The decomposition is cut by surface — README, menu, gallery page, lessons,
migration pages — when the project's architecture makes it a corpus-and-pipeline
problem.** Cutting by surface guarantees six hand-maintained artifacts that drift
apart; the third comment on this issue is already the first symptom (#579/#580
consume a screenshot set nobody owns, #586 proposes generating it, and the pass
"deliberately does not choose"). That ambiguity is not a filing accident. It is
what happens when you name the *places* the content appears instead of the *thing*
the content is derived from.

## Reframing 1 — the example library is the golden-test corpus (collapses PF-2, PF-4, most of PF-1, and settles the #579/#580/#586 fight)

The issue treats "every .jls in the repo is a test fixture" as the embarrassment.
Invert it. Make the curated example corpus *the* thing, and make every onboarding
surface a derived build product of it.

Concretely, one directory — say `examples/` — where each circuit ships as a
quadruple: the `.jls`, a caption, a `-t` test-vector file, and its expected batch
output. Then:

- `BatchSimulationGoldenTest` runs the corpus. AC-3's "each loads, simulates" stops
  being a manual check and becomes a build failure. An example that rots breaks CI.
- `-i out.svg` over the corpus in CI *is* PF-4. There is no gallery to curate; there
  is a gallery that regenerates. The README images (PF-1) come from the same run.
- The captions are one string used as gallery caption, Examples-menu tooltip, SVG
  alt text, and store-listing blurb. #579's prohibition ("consumes them and does not
  commission a second set") becomes structurally unbreakable rather than a rule
  somebody has to remember.
- The test-vector files make the corpus a live demonstration of JLS's only 5/5 axis.
  The example library and the grading pitch stop being two projects.

This also answers the third comment's open question without needing the evidence it
says it lacks. The producer is not #511, #519, or #586 — it is the corpus in-repo.
#586's headless-sway rig captures *editor chrome* (which genuinely drifts with the
UI); `-i out.svg` renders *circuits* (which drift with the file format, already
pinned by round-trip tests). Those are different artifacts with different drift
rates and the three-way fight dissolves once you name them separately.

The machinery for this already exists and is unsurfaced. `riscv/jlsbuild.py` is a
netlist compiler that emits FORMAT 1 `.jls` text and is validated against the real
batch simulator by `test_primitives.py`; `riscv/examples/` already holds `.s` sources
with `.clk.txt` vectors. The project has had a programmatic circuit-authoring path
the whole time and files this capstone as if circuits must be hand-drawn.

## Reframing 2 — PF-3's welcome pane is the wrong shape; JLS should simply always have a circuit open

`JLSStart` builds an empty `JTabbedPane` (`src/jls/JLSStart.java:1274`) and only
populates it if `startFile != null` (`:1296-1299`, routing to `open` at `:2220`).
#771 proposes to decide "starter circuit versus welcome pane, in writing." The
architecture already decides it.

A welcome pane is a **new chrome surface** in a codebase where ~126 hardcoded
chrome/canvas color sites already fight every look-and-feel (ARCHITECTURE.md, the
FlatLaf decision) and where #381's narrowed scope is precisely the scaling/theme
screenshot matrix across three OSes at four DPI settings. Every new pane enters that
matrix. A starter circuit enters nothing: it is `open(...)` on a path that already
runs, its startup cost is one load of a small file, and KC-27-1's per-commit
startup-time gate becomes trivially satisfiable instead of a thing to defend.

It is also better product. A welcome pane is a menu *about* the tool; a loaded
starter circuit with a probe already blinking is the tool. The ten-minute clock
stops at "running, understood example circuit" — landing directly in one is the
shortest possible path to that state, and it needs no on-screen prompts, which is
also most of AC-5.

Keep `File > Open Example` and `Help > Tutorial` (already at `JLSStart.java:2094`)
as the discoverable next moves. Drop the pane.

## Reframing 3 — the README needs subtraction, not a GIF on top

README.md is 368 lines. Lines 12–330 are installer checksums, provenance
attestation scope, GPG custody rationale, the Wayland toolkit matrix, xz container
archaeology, and the dev-container recipe. This is a maintainer's document. PF-1
proposes adding screenshots "above the fold," which produces a packaging document
with a picture at the top.

The 1-mw version with more effect than the GIF: cut the README to ~60 lines — what
it is, one image, one install line per OS, one example, links — and move verification
detail to `docs/install-verification.md` and desktop support to
`docs/desktop-support.md`. ARCHITECTURE.md's own recorded direction supports this
("repo documents … are the normative home for contracts") — contracts belong in
`docs/`, not in the shop window. A prospective user who reads line 50 of the current
README learns that JLS installers are not byte-reproducible. That is true, important,
and catastrophic as the third thing a stranger reads.

## Where the feature set pulls against the project's recorded arc

**PF-4 + PF-5 + PF-6 are one thing: JLS gets a documentation site.** ARCHITECTURE.md
records that hosted, versioned web documentation *is* the planned direction and that
the in-app viewer shrinks to context-sensitive basics when it arrives. Filing the
gallery, the lessons, and the migration pages as three separate onboarding features
risks three static-site mechanisms and no site. File the site once, as the recorded
architectural item it is, and let this capstone be its first three pages.

**PF-5's in-tool variant should not be built at all.** The recorded direction says the
in-app viewer shrinks; building stepped in-tool lesson tooling now is building the
thing the architecture says will be removed. KC-27-2 already concedes docs pages
survive the outcome. Ship the docs variant, delete the in-tool option from the plan
rather than leaving it as a band-overrun escape hatch.

## What I would drop, explicitly

**AC-2 as written** — "a scripted fresh-user protocol measures install→running-example
in <10 minutes on Windows, macOS, Linux." Three-OS timed observation is a capstone-
sized cost inside a capstone, it is the one criterion the coverage pass could not
assign an owner, and the second comment shows it drifting between #73, #381, and here.
More to the point, it measures the wrong clock. A stranger's ten minutes includes
*deciding to download*, which no protocol run by people who already have JLS installed
will ever capture. Replace it with two cheap falsifiable proxies: (a) an n=5
screen-recorded trial on **one** OS — claim it here per the second comment's
recommendation, since an unfalsifiable ten-minute claim is exactly the defect to
avoid; and (b) a CI-enforced structural check that the corpus renders, simulates, and
carries captions, which is the part that actually rots.

## The persona this capstone does not name

"A prospective user" is undifferentiated, and every AC is GUI-shaped. But #510 says
the browser-first student is *structurally unwinnable* without a web story, while the
grading instructor is JLS's one winnable segment — and for that person the ten-minute
path does not involve the GUI at all. `docker run --rm -v "$PWD:/work"
ghcr.io/anadon/jls -b -t tests circuit.jls` is zero-install, multi-arch, and already
shipping. A "ten minutes to a graded circuit" on-ramp — one container command, one
example with vectors, one exit code — costs a fraction of a milliweek on top of
Reframing 1's corpus and lands squarely on the only axis where JLS is category-best.
It is not in the six features. It should be PF-0.

## Verdict

Endorse the outcome; re-cut the features. Keep PF-1 as *subtraction plus generated
images*; merge PF-2 and PF-4 into one corpus-with-vectors feature that CI renders and
tests; reduce PF-3 to "first launch opens a starter circuit" and retire the welcome
pane; merge PF-4/5/6 into the recorded docs-site direction and drop PF-5's in-tool
variant; add the container-grading on-ramp as PF-0. Replace AC-2's three-OS protocol
with one recorded n=5 trial plus CI structural gates. The result is fewer moving
parts, one source of truth for every downstream consumer including CAP-34's store
listings, and — unlike the current plan — an on-ramp that cannot silently go stale.
