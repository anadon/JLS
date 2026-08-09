# Issue #777: TASK-C552-1: lesson content is a data format authored apart from any presentation layer, and lesson 1 is written in it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is "a lesson data format." The actual purpose, read up
the chain, is narrower and better: CAP-27 (#511) AC-5 wants lesson 1
*completable from on-screen prompts alone*, and KC-27-2 wants that outcome to
survive if the in-tool panel gets cut. #777 reads that kill criterion as "so
don't couple content to widgets" and reaches for a presentation-free container.

That is the right instinct aimed at the wrong failure mode. The thing that
would actually kill this outcome is not an `<b>` tag leaking into the content.
It is a lesson whose steps do not, when followed, produce the circuit it claims
to build — and prose steps in any container cannot be tested for that. AC-3's
test ("contains no presentation markup and no in-tool widget references") is a
lint against angle brackets; it is evidence of tidiness, not of the lesson
working. AC-2 ("no longer than a caption") and AC-4 ("ends with the circuit
running") are likewise unassertable over an opaque prose blob.

## Where it pulls against the project's arc

**It is the third content system for the same job.** `src/jls/tutorial/tutorial1.html`
is already stepped build-along prose for a combinational circuit (A + ~B: click
the OR gate, the dialog appears, take the defaults, place it, wire it) and
tutorial3 is the full adder. `resources/help/**` is a second tree, and
ARCHITECTURE.md's recorded decision already pins it as presentation-independent
by discipline: "help content stays plain HTML 3.2 with relative links and no
viewer-specific markup ... so the same tree can be published to the web without
rewriting," policed by `HelpTopicsTest`'s link checker and completeness test.
The "author once, render in-tool and as docs" seam #777 wants already exists and
already has a ratchet test. #552 says the 4-page tutorial is "superseded for the
first-run path but not removed" — so #777 as written leaves three parallel
copies of the same instructional content, none of which knows about the others.

**It forks the example registry #548 warns about.** #548's ordering note is
explicit: "do not fork a second sample mechanism." Every curated example already
carries a caption and a suggested exercise. A lesson is that record with steps
added, not a second registry keyed by the same ten circuits.

**"Data format" invites a dependency the jar does not want.** The runtime deps
are xz, jfree.svg, flatlaf, jspecify — no YAML or JSON parser, and the shaded
self-contained jar is the deployment model. The house idiom for in-tree data is
the line-oriented save-format grammar with a strict reader that rejects rather
than repairs (`docs/file-format.md`, `CircuitOpReader`). The YAML in the issue
header is issue metadata; it should not become the lesson container.

## The reframing: a lesson is executable, not merely inert

Make the lesson's payload the *circuit states it passes through*, with prose
carried alongside as captions. Concretely, a lesson is:

1. the shipped example circuit from #548 (the end state),
2. an ordered step list, each step = one caption + the circuit mutation it
   performs,
3. nothing else.

Express the mutation in the vocabulary the project already built: the
`OP <kind> … END` grammar of `jls.collab.op` (#167, `docs/operation-layer.md`) —
`AddElements`, `SetElementConfig`, `AddWire`, with `ElementBlocks` guaranteeing
an added element is byte-indistinguishable from a loaded one. What that buys:

- **AC-3 becomes structural rather than asserted.** The payload is ops and
  strings; there is nowhere to write a widget reference. No lint needed.
- **A real oracle.** A headless test replays the op script against an empty
  circuit and asserts the result is byte-identical to the shipped example's
  canonical save (#166). Lesson and example cannot drift. This is the first
  mechanized proxy anyone has proposed for "lesson 1 builds circuit 1."
- **AC-4 for free.** The final state runs under `BatchSimulator` against the
  example's vectors — the same `SampleCircuitsTest` shape #548 AC-3 already
  plans. One test covers both features.
- **Better docs pages than prose.** Render step *i* as caption + an SVG of the
  circuit after step *i*, generated at build time by the existing `-i out.svg`
  exporter (#154). A picture-per-step build-along is the SimCast pattern #510
  actually identified, and it hands PF-4's gallery most of its machinery. A
  prose-only format gives PF-4 nothing.
- **A stronger in-tool panel if the band allows.** "Show me" applies the step's
  ops through `OpSink` (one gesture, one undo snapshot — already the contract),
  and progress can be checked by comparing the learner's circuit to the step's
  expected state. If the band does not allow it, KC-27-2 fires and the identical
  content ships as docs. The kill criterion is honoured harder, not softer.

**Cheap variant if the op layer is not ready.** #167 is mid-migration; if the
kinds do not yet cover what lesson 1 needs, degrade to: a lesson is a directory
of numbered `.jls` snapshots plus a captions file. Still executable (each
snapshot loads through the ordinary reader; the last simulates), still
driftless, still zero presentation, and it adds no dependency on #167 —
`ordering_after` stays `[TASK-C548-2]`. Authoring is "save as" at each step.
This may well be the true 1–1.5 mw shape; the op-script version is the upgrade.

## Disregarding a stated acceptance criterion

I am explicitly rejecting **AC-3 as written**. "No presentation markup, asserted
by a test" is the wrong property to pin. Replace it with: *the lesson's steps,
executed in order, produce the shipped example circuit byte-for-byte, and the
final state simulates* — asserted headlessly. Presentation independence then
follows from the payload's type instead of being policed by a string lint.

I would also amend **AC-1**: "documented in-tree" should mean a normative
`docs/lesson-format.md` in the family of `file-format.md` / `batch-interface.md`
/ `extension-points.md`, with strict-reader semantics (unknown field → reject,
never repair) and a catalog test — that is the house style, and leaving it to a
README paragraph is how the third content system got here.

And I would add a deliverable the issue omits: **name what this replaces.**
Lesson 1's captions should be `tutorial1.html`'s prose split into steps, with a
recorded plan to retire `src/jls/tutorial/**` once lessons render both ways.
Otherwise #552 ships an on-ramp with three competing tutorials.

## Costs I am not hand-waving

Op scripts are unpleasant to hand-write; they should be *recorded* (the editor
already funnels gestures through `OpSink`, so a debug capture mode is small) or
avoided via the snapshot variant. The replay test is new work, but it displaces
the presentation lint and the bespoke parser, so the band is roughly neutral;
the per-step SVG rendering is optional and belongs to the docs task, not here.
One incidental note: separating caption text from mutations makes lesson
translation cheap later — a bonus consistent with the i18n revisit triggers, not
a reason to build i18n now.

## Bottom line

The goal is right and well-placed in CAP-27's arc; the mechanism is the weakest
version of it. Ship the lesson as an executable artifact anchored to the example
circuit, folded into #548's curated-example record, specified in the save-format
idiom — and the presentation-independence the issue asks for stops being a
promise policed by a lint and becomes a property of the type.
