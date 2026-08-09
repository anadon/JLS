# Issue #840: TASK-C573-1: one example runs on a hosted static page whose deployment is a file copy — the demo exists before it has a catalogue
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is actually for

Strip the yaml away and #840 is a de-risking move with one genuinely good idea in it: **prove the
operational shape (link → load → poke → nothing to operate) on one circuit before anyone curates a
set.** That inverts the dependency chain its own parent carries — #573 is `ordering_after` #548, the
curated-examples feature that does not exist yet — and it is right to. Hosting shape and content
volume are independent risks and should be retired independently.

The idea I want to reframe is the other half, the half #840 inherits silently: the *mechanism* is
whatever #572's CheerpJ go/no-go names, so the cheapest issue in the capstone (one page, one circuit,
a file copy) is gated behind the most expensive and most uncertain one (wrap the whole Swing jar,
measure it, ≤15 s). That ordering is backwards, and the codebase says so.

## The finding that changes the calculus

`test/jls/HeadlessCoreRatchetTest.java:90` reads `BASELINE = Set.of()`. **The ratchet is empty.** As
of HEAD, `jls.sim`, all 74 files of `jls.elem`, `jls.hdl`, `jls.core`, `jls.module`, plus `Circuit`,
`FileAbstractor`, `LoadError` and `BitSetUtils` import no `java.awt`, no `javax.swing`, no
`jls.edit`. Issue #77's shrinking baseline has already reached zero. The model, the element
behaviour, the event loop, and the load path are a clean, GUI-free library today.

CAP-32's chosen mechanism — CheerpJ around the shipped jar — takes that hard-won separation and
throws it away in the one place it would pay off most: it ships the editor, the Swing toolkit, the
save/save-as paths, the checkpoint writer and the file dialogs into a browser, then tries to argue
the result is "read-only by construction." #840's own AC-3 says *"no save, upload, or user-content
path exists in the shipped bundle; verified by inspection."* Against a CheerpJ-wrapped full jar that
inspection fails honestly: every one of those paths exists in the bundle. You can only claim they are
unreachable through the UI, which is a different and weaker claim than the one written down.

## The trilemma nobody has laid out, and #840 is where it must be surfaced

Whatever #840 builds becomes the substrate #573, #574 and #886 inherit. Three candidate substrates,
and they do not carry the same things:

| | CheerpJ whole jar | SVG + precomputed trace player | headless core → wasm |
|---|---|---|---|
| AC-3 read-only *by construction* | argued, not structural | **structural** — bundle contains no JLS code | structural — no editor compiled in |
| AC-2 "file copy, nothing to operate" | undercut: the CheerpJ runtime is fetched from a third-party CDN at load; self-hosting it is a licensing question, not a `cp` | **holds absolutely** | holds |
| Bundle | tens of MB | ~100s of KB | few MB |
| #886 share-by-link (needs a *live* simulator) | yes | **no — void** | yes |
| Duplicate simulation semantics | no | no (traces come from the real simulator) | no |
| Cost to first hosted page | after #572 concludes | days, on machinery that already ships | a real spike |

The CDN row is the one that should stop the room. CAP-32's whole pitch is the anti-simulator.io
permanence property — *nothing that can die and take the demo with it* — and the default CheerpJ
deployment makes the page's ability to run at all contingent on someone else's host and licence
terms. #840 as written would pass AC-2 on a technicality while violating the property AC-2 exists to
protect. If CheerpJ ultimately wins, **AC-2 must be rewritten to demand a self-contained bundle,
verified by loading the page with every non-origin request blocked.** That is a load-bearing change,
not a nitpick.

## Reframing 1 — build #840 now, on the fallback, and let it become the measurement

I am disregarding `ordering_after: ["#572 …"]`. Build the one-example page on fallback (a), the
SVG + VCD-driven player, immediately. It is nearly free because the project already shipped both
halves: `-i out.svg` renders any circuit headlessly through JFreeSVG
(`src/jls/edit/CircuitRenderer.java:314`, flag at `src/jls/JLSStart.java:765`, with
`setDefsKeyPrefix("jls")` already there for stable output), and `-vcd` emits traces with a golden
test pinning them (`test/jls/VcdExportGoldenTest.java`). AC-4 and AC-5 — regenerable from a committed
command, byte-identical on rebuild — come almost for free by riding the jar's existing
reproducibility machinery (`docs/reproducibility.md`, the `.buildinfo` recipe); they do not come free
from a third-party AOT packager whose determinism is not yours to control.

Doing this first converts #572 from *threshold-versus-nothing* into a real comparison: a live page
with measured numbers already exists, and CheerpJ has to beat it on something a visitor can feel. A
spike whose declared fallback costs about what the spike costs should do the fallback first — it is
the branch that is certain to work and is reusable under every outcome.

## Reframing 2 — the seam the issue never considers

The bigger prize: the substrate that carries *both* the demo and #886 is not CheerpJ-around-the-GUI,
it is **`jls.sim` + `jls.elem` + `Circuit` compiled to WebAssembly with no editor at all.** The
project has already paid the entire architectural price for this — that is what an empty ratchet
baseline *means* — and CAP-32 is about to route around it. In that world the demo page is: an SVG
render for the picture, the real JLS core for the behaviour, zero Swing, zero save path, and one
normative simulation semantics (`docs/simulation-semantics.md`) rather than two.

Honest caveats, because this is a spike and not a plan: the Java-to-wasm toolchains (TeaVM, or
CheerpJ pointed at core classes only) lag the language level `pom.xml` targets, `FileAbstractor`'s XZ
dependency would need the demo to ship plain-text circuits (it can — the reader sniffs), and 74
element classes is real surface to compile. Those are measurable in a week. My claim is only that
this option deserves to be on #572's ranked list, and today it is not on it at all.

## The trap to name explicitly

Do **not** hand-write a JavaScript logic simulator for the demo. It is the obvious shortcut and it
would fork the project's normative semantics into a second implementation that nobody tests against
the golden VCDs. Precomputed traces avoid this (the trace *is* the real simulator's output); wasm
avoids it (there is only one simulator). A JS reimplementation is the one route that pulls against
the project's arc, and it should be written into the issue as a refusal.

## Where the stated acceptance criteria need to change

- **AC-1** ("toggle an input and see the trace change") is where mechanism and *example choice*
  couple, and #840 treats the example as arbitrary. Under precomputed traces the demo circuit must be
  one whose stimulus space fits a file: enumerate a bounded input lattice at build time, and make the
  generator **fail the build** when the enumeration exceeds a stated size budget. A refusal recorded
  in CI is honest; silently shipping 40 MB of traces is not. Pick the circuit to fit the mechanism,
  and say in the record that you did.
- **AC-3** should be verified by an allow-listed bundle manifest asserted in CI, not by prose. Under
  the SVG route that assertion is three lines and reads: the bundle is one SVG, one trace file, one
  script.
- **New AC**: the bundle fetches nothing from a non-origin host at load; verified with network
  blocked.
- **New AC**: the generator takes a *list* of `.jls` paths. Then the catalogue is not a follow-up
  project — #573 becomes "add nine more paths," and #840's best instinct (demo before catalogue)
  becomes permanent architecture instead of a one-time sequencing trick.

## One page, not two

#551 (FEAT-C27-4) publishes a static SVG gallery of the same examples via Pages, generated by the
same `-i out.svg` export, linked from the same README. #840 as filed stands up a *second* static-page
pipeline with its own deploy script and its own README link. Under the SVG route these are the same
artifact separated only by whether a card has a time axis. Build one Pages deployment, one
regeneration script, one link, and let the demo be the gallery card that learned to move. Two
pipelines for one page is the duplication this capstone should not acquire on its very first task.

## Verdict

**endorse-with-reframing.** The outcome — one hosted example, static, read-only, regenerable,
reproducible, before any catalogue — is exactly right and should ship. The reframing: cut the
`ordering_after` tie to #572 and build it on the SVG+trace path now; converge it with #551 rather
than forking a second pipeline; harden AC-2 into self-containment and AC-3 into a CI-checked
manifest; make the generator list-driven so the catalogue is free; and put the wasm-core option — the
one the empty `HeadlessCoreRatchetTest` baseline has already bought and paid for — onto #572's ranked
list before that go/no-go becomes binding on everything downstream, including #886.
