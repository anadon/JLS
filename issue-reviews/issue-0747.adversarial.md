# Issue #747: TASK-C546-1: a circuit reads out as a part-to-whole prose narrative a blind student can follow linearly
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task is correctly scoped relative to its neighbors (narrative now, tactile
SVG in #749, bundle command in #750) and its determinism/totality criteria are
solid and cheaply testable against existing precedent in this codebase. But
two of its four acceptance criteria have a gap between what is stated and what
is checkable: AC2 cites "the cited guidelines" without any guideline ever
being cited anywhere in the traceable chain (#747 → #546 → #507), and AC4's
stated mechanism (ban string concatenation) does not verify its stated goal
(translatability). There is also a hidden assumption about the shape of data
#731 (TASK-C542-2) actually promises to deliver.

## Findings, most severe first

### 1. AC2's "the cited guidelines" cites nothing, anywhere in the chain — the checklist test has no external referent to be checked against

AC2 reads: "Narrative ordering passes a guideline checklist test asserting
part-to-whole structure per the cited guidelines." No guideline is named or
linked in this issue's body. Tracing up: the parent feature #546 uses nearly
identical wording ("Narrative ordering passes the guideline checklist test
(part-to-whole per the cited guidelines)") and also names nothing. The
capstone #507 (CAP-26) step 3 says "a structured prose narrative ordered
part-to-whole **per the cited tactile-graphics guidelines**" — which resolves
the pronoun to BANA, the tactile-*graphics* line-width/spacing/symbol
standard that #749 (TASK-C546-2) implements for the SVG. BANA does not
publish rules for prose narrative ordering; conflating "cited guidelines" for
a graphics-embossing spec with a text-structuring rule is a category error
inherited from the capstone and never caught or corrected by #747, which is
the one issue actually implementing the narrative. As written, whoever picks
this up must invent both the checklist *and* the standard it purports to
enforce — a "guideline checklist test" that checks a fixture against rules
the implementer made up passes trivially and proves nothing about the stated
goal (a blind student can actually follow the narrative linearly).

**Recommendation:** Name an actual source before work starts — e.g. the
DIAGRAM Center's Image Description Guidelines, POET training materials, or
WCAG 1.4.5/1.1.1's "meaningful sequence" language — the same way BANA is
(under-)named for the SVG in #749. Without a citable external rule, AC2
cannot be told apart from "the author's opinion of good prose," which is not
a build-checkable acceptance criterion despite being phrased as one.

### 2. AC4's testable mechanism does not verify its own stated goal — a template-based implementation passes the letter of the check while failing the goal

AC4: "The narrative ships in English and its format does not preclude
translation — no string concatenation that assumes English word order in the
structural layer." The only falsifiable clause here is "no string
concatenation." Concrete failure scenario: an implementer writes
`String.format("%s feeds %s through the %s gate.", src, dst, gateType)`.
This contains zero `+` string concatenation, so any lint/test keyed on the
literal absence of concatenation passes. But the placeholder *positions* in
that format string are exactly as English-word-order-locked as concatenation
would have been — a language with different word order (e.g. verb-final)
cannot be served by reordering arguments into the same template without
rewriting the template itself, which is fine for real i18n tooling (that's
what message catalogs are for) but is indistinguishable, under this AC's
stated check, from the naive case the criterion claims to forbid. The gap is
that "assumes English word order" is a semantic property of the *template*,
not a syntactic property of whether `+` appears in the code.

**Recommendation:** State the actual property being verified — e.g.
"positional/named placeholders only, no computed word ordering in the
structural layer, verified by a lint over `String.format`/message-template
call sites, not merely absence of `+`" — or drop the mechanism clause and
leave AC4 as an unverified aspiration (which would at least be honest about
what's actually being tested).

### 3. Hidden assumption: #731 (TASK-C542-2) is not shown to produce the kind of data #747 says it "consumes"

The Outcome section states: "It consumes TASK-C542-2's registry-keyed
state-to-encoding data for describing state rather than inventing a second
vocabulary." I read #731's actual body: its AC1 is "Every distinct wire state
has a non-colour encoding — thickness, dash pattern, glyph, or a declared
combination — resolved through a registry-keyed mapping." That is a mapping
from wire-state to *visual rendering parameters* (line thickness, dash
pattern, glyph shape) for the GUI `Theme` seam — nothing in #731 promises
human-readable English descriptions of states. The only thing #747 can
actually reuse from #731 without further design work is the *domain* of state
identifiers (high/low/HiZ/bus-value/error) that the registry is keyed by, not
the encoding values themselves (a dash pattern has no obvious prose
rendering). "Rather than inventing a second vocabulary" implies #731 already
defines a textual vocabulary; it defines a visual one. This risks either (a)
#747 quietly inventing the textual vocabulary anyway — the exact outcome the
sentence says it's avoiding — or (b) discovering this gap mid-implementation
and needing a REPLAN on #731 to add a textual-label field it doesn't
currently commit to.

**Recommendation:** Before starting, confirm (via #731 or a REPLAN comment)
that the registry keys used for state identity will carry, or will be
extended to carry, canonical human-readable labels — don't assume the visual
encoding map already is that vocabulary.

### 4. "One command" in AC1 is not reconciled with #750's bundle-command contract, and the CLI surface this creates is subject to the project's own stability promise

AC1: "One command emits a prose narrative for any circuit." Three tasks later
in the same feature, #750 (TASK-C546-3) states: "a single accessible-export
command produces both the prose narrative and the tactile SVG... Emitting one
artifact without the other is possible only by an explicit flag; the default
is the pair." #747 gives no indication whether its "one command" is a
standalone verb #750 will later fold into a flag (`--narrative-only`?) on the
bundle command, or whether it's meant to anticipate that shape from the
start. README documents that "the batch interface... is a documented
stability contract" (`docs/batch-interface.md`) — so whatever flag or command
name #747 ships now cannot be freely renamed once released without a
compatibility concern, yet nothing in #747 or #750 commits to a shared naming
convention up front.

**Recommendation:** Either #747 states explicitly that its command is
provisional/internal until #750 lands, or the two issues pre-agree the final
flag shape (e.g. `-narrative` as one of the flags `-accessible-bundle` will
later imply by default) so #747 doesn't have to be reworked for a CLI surface
change three tasks later.

### 5. Output destination and format are unspecified against an explicit existing CLI convention

Neither the file extension, the CLI flag name, nor stdout-vs-file destination
is stated. `ARCHITECTURE.md`'s CLI contract reserves stdout for batch
results and directs diagnostics to stderr; existing export flags (`-i`,
`-export`, `-vcd`) all take an optional output path. #750's own AC1 refers to
"a documented layout" for the bundle — implying #747 must already have picked
a file-naming/placement convention for the narrative artifact, but #747 never
states one. This is a small but real spec gap: two different implementers
could reasonably choose stdout-emit vs. file-emit and both would satisfy
AC1's letter ("emits a prose narrative") while producing incompatible
interfaces for #750 to unify later.

**Recommendation:** State the destination convention (file, following the
`-i`/`-export` precedent, named after the circuit file) explicitly in the
acceptance criteria, not left to be inferred.

### 6. `ordering_after` may understate a hard functional dependency on #731

The YAML front matter carries only `ordering_after: [TASK-C542-2]`, no
`blocked_by` (contrast #78, which carries an explicit `blocked_by: []`
field). But AC4 functionally requires #731's data to exist for the
"describing state" clause to be satisfiable at all (see finding 3). If #731
is deprioritized or reworked out of sequence, #747 cannot fully satisfy its
own AC4 — a risk that a soft "ordering_after" hint doesn't flag as clearly as
a hard dependency would.

**Recommendation:** If the dependency is truly load-bearing (which the
Outcome text suggests it is), express it as a blocking dependency rather than
an ordering hint, or explicitly scope AC4's state-description clause to
degrade gracefully if #731 hasn't landed.

## What's solid

- AC1's determinism scope ("deterministic for a given circuit," not
  byte-identical across platforms) correctly leaves the stronger
  cross-platform guarantee to #750's `AccessibleExportDeterminismTest**,
  avoiding scope creep into a neighbor's job.
- AC3 (every registered element type must contribute a describable phrase or
  fail the build) is a well-precedented, concretely feasible pattern: the
  repo already has exactly this shape of totality test
  (`ElementRegistry.java`, `ElementRegistryTest.java`, and the
  `CapabilityInterfaceTest`/`PinFaceContractTest` lineage cited in #78) to
  copy from — low implementation risk.
- The i18n framing is handled correctly: it cites the closed #85 decision and
  states its "reopen condition stands untouched" rather than re-litigating
  the recorded non-goal — this matches `ARCHITECTURE.md`'s actual recorded
  decision and revisit triggers, and matches how #546 and #507 both phrase
  the same boundary.

## Note on grounding

`ARCHITECTURE.md`'s "Adding an element today" section states "There is no
element registry yet — issue #78 will introduce one," but the checked-out
tree already has `src/jls/elem/ElementRegistry.java` and
`test/jls/ElementRegistryTest.java`, and #78's own body describes the
registry as landed except for one compile-time-obligations stage. This is a
staleness in the grounding document, not a defect in #747, but it means AC3's
premise ("the registry," present tense) is realistic — the registry really
does exist — even though the file I was pointed to for grounding says
otherwise.
