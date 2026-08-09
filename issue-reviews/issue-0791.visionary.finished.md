# Issue #791: TASK-C584-1: the help source format is chosen and recorded, and the in-jar viewer's safe subset is written down and enforced by the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Nominally: pick Markdown or AsciiDoc, write down what `JEditorPane` can render, lint
for it. Really: this is the first irreversible slice of FEAT-C35-1 (#584), whose end
is that 83 hand-maintained HTML files stop being a folk craft and become one auditable
source emitting two targets. The format choice is instrumental to that; on its own it
is a paragraph in a decision record. The risk this slice carries is that it lands two
prose documents and a lint rule and zero migrated content — a decision-shaped artifact
that nothing yet depends on.

The trajectory it must serve is already written down. ARCHITECTURE.md §"Help delivery:
in-jar now, hosted docs are the planned future (recorded 2026-07)" commits to exactly
this arc, including the interim rule: *"help content stays plain HTML 3.2 with relative
links and no viewer-specific markup, and the `HelpTopicsTest` link checker (#70) keeps
it truthful, so the same tree can be published to the web without rewriting."* The
issue is aligned with the project's arc. My objections are about where it cuts.

## The subset is being constrained at the wrong end of the pipeline

AC-3 says the build check "rejects **source** content that uses a construct outside the
viewer-safe subset." But the source will be Markdown, and Markdown has no constructs
that are viewer-unsafe — the *converter's HTML output* does. Constraining the source
means authoring and maintaining a mapping table ("a GFM table compiles to `<table>`,
which the viewer tolerates; a task list compiles to `<input type=checkbox>`, which it
does not"), and that table silently goes stale the first time the converter dependency
bumps a minor version.

Cut the seam one stage later. Check the **generated in-jar HTML**, and check it with
the actual renderer rather than against a written rule: `HTMLEditorKit`'s
`ParserDelegator` parses against the real HTML 3.2 DTD, so a headless test can walk
every emitted page and assert (a) no element outside an allowlist appears in the parsed
document, (b) the parser raises no errors, (c) every `href`/`img src` is relative. This
is precisely the shape of discipline the repo already prefers: `HelpTopicsTest` deliberately
compares against *"the exact resource names copied into the jar"* so a `down.gif` vs
`down.GIF` mismatch fails in CI rather than for a student, and `scripts/wayland-rig-selftest.sh`
asserts the assertion. Checking output-with-the-real-parser is that same instinct;
linting source-against-a-table is not.

Concrete: the allowlist should be **data, not prose** — one file (say
`resources/help/safe-tags.txt`, or a build-time properties file) that is the single
source of truth. The build check reads it. The documentation page required by AC-2 is
*generated* from it, not authored alongside it. Then AC-2 and AC-3 cannot drift from
each other, which is the failure mode "folklore becomes a written contract" is supposed
to eliminate and, written as two independent deliverables, quietly reintroduces.

## KC-35-1 is already answerable, and the answer is "no viewer upgrade needed"

AC-4 asks the subset to be priced before choosing. It can be priced right now from the
tree, and doing so keeps this task from spawning a viewer-replacement rabbit hole. The
complete tag census of `resources/help/**` (83 pages) is:

`p a b font h1 h2 td tr th table i li ul ol code pre img sub sup br kbd` — 20 distinct
element types, plus `html/head/title/body`.

CommonMark + GFM tables emits `h1–h6, p, ul/ol/li, table/tr/td/th, pre/code, blockquote,
a, img, hr, em, strong, br`. Every one of those is HTML 3.2 and inside what `JEditorPane`
renders — including tables, which the tree already uses. Going the other direction, the
only existing constructs Markdown does not emit are `<font>` (95 uses — presentational
cruft that should die in migration, not be preserved), and `<sub>`, `<sup>`, `<kbd>`
(7 uses total, all reachable via the inline-HTML passthrough that both CommonMark and
AsciiDoc permit). **The viewer is richer than the content.** The honest recorded answer
to KC-35-1 is: the subset does not force the source below usefulness, the priced viewer
upgrade is deferred, and the trigger for revisiting it is not markup but *search* and
*code styling* — which are #585's problem on the hosted target anyway.

## The format: Markdown, for a project-specific reason the issue does not state

Pick CommonMark + GFM tables + a YAML front-matter block. Not because Markdown is more
popular, but because of two facts about *this* repository:

1. **The docs/help boundary should be permeable.** ARCHITECTURE.md already declares
   `README`, `docs/*.md` and itself *"the normative home for contracts"* and in-jar help
   *"the student-facing manual"*. Those are 40+ Markdown files. Choosing Markdown means
   `docs/batch-interface.md` can be excerpted into, or promoted to, an in-jar page with
   no dialect translation; choosing AsciiDoc means two prose dialects in one repo forever
   and a permanent wall between the manual and the specs it should quote.
2. **Toolchain weight against a build the project keeps byte-reproducible.**
   `commonmark-java`/flexmark is a small build-time-only dependency; AsciidoctorJ drags
   JRuby. Both stay out of the shaded jar and out of `bom.json`, so neither disturbs the
   reproducibility guarantee — but one of them disturbs build time and the SBOM of the
   build environment recorded in `.buildinfo` considerably more than the other.

AsciiDoc's real advantages (includes, admonitions, attribute substitution, conditionals)
pay off on a 500-page manual. JLS has 83 short pages averaging a screen each. Record
that as the rejected alternative with that reason, not with a taste argument.

## The reframing I would actually make: the load-bearing decision here is the topic-id namespace

If I were re-cutting this task, the format choice would be a subordinate paragraph and
the headline would be: **the source tree's layout *is* the topic-id namespace, and
`Map.jhm` and `JLSHelpTOC.xml` become build outputs.**

Today `help/Map.jhm` is inherited JavaHelp metadata, hand-maintained, parsed by
`jls.Help` with a regex (`Help.java:134`), and cross-checked by `HelpTopicsTest` after
the fact. It is the last live piece of the library #11 removed. And it is quietly the
most consequential thing in this slice, because:

- `PaletteEntry.helpTopic()` keys into it, and `HelpTopicsTest.everyPaletteElementTypeHasAMappedHelpTopic`
  makes that a completeness contract (#85);
- #585 AC-3 requires *every hosted URL to be derivable from the in-jar topic id*, so the
  id space is a public contract about to be frozen by the hosted site;
- #584's own boundary comment names topic-id stability as the seam where the pair drifts,
  and asks AC-5 to record it explicitly.

Make the topic id a property of the source file — its path, or an `id:` in front-matter —
and generate both metadata files. Then #584 AC-2 stops being something a test verifies
after a risky migration and becomes structurally true; #585's URL derivation is free; a
new element's help page cannot be added without an id; and the JavaHelp residue is gone.
Choosing Markdown-vs-AsciiDoc is reversible with a converter run. Choosing (or drifting)
the id namespace is not. This slice should record **three** decisions — source format,
topic-id namespace and metadata derivation, safe-set-as-data — with the middle one first.

## Duplication to avoid

AC-1 and AC-2 want new records in `docs/`. But ARCHITECTURE.md's "Help delivery" section
*already* states the portability discipline in prose. Amend that recorded decision to
point at the new record rather than leaving two independent statements of the same rule;
the project's own convention is one recorded decision per question, with revisit triggers.
Similarly, AC-3's "planted violation ... with the transcript recorded" should be a negative
fixture *inside the test suite* (a page carrying a banned tag, asserted to fail the checker)
rather than a pasted transcript in a doc — transcripts rot, and `wayland-rig-selftest.sh`
is the precedent for the live version.

## Verdict

**endorse-with-reframing.** The decision needs making, it is on the project's declared arc,
and making it before the migration is right. Reframe as: (1) topic-id namespace and
generated `Map.jhm`/TOC as the primary decision; (2) CommonMark, with the two
repo-specific reasons above; (3) the safe set as one data file that the build check
consumes and the doc page is generated from, enforced against the **emitted HTML** using
`HTMLEditorKit`'s own parser; (4) KC-35-1 answered from the 20-tag census above — no
viewer upgrade in this slice. I am not disregarding the acceptance criteria, but I am
saying AC-3 as literally written ("rejects source content") should be re-worded to
target generated output, or it will encode a mapping table that outlives its accuracy.
