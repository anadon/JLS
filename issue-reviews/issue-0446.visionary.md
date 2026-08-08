# Issue #446: TASK-0040: a set of circuits becomes one distributable, license-carrying, digest-checked artifact that a circuit can reference by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the container talk away and the goal is one sentence: **a definition should be
referenceable from outside the file it was drawn in, and the reference should be
verifiable.** That is the capability #340 exists for, it is what makes #357's
definition/instance split worth having, and it is the single largest correction to
the fact recorded in `src/jls/elem/SubCircuit.java` — `save` inlines the entire
nested circuit per instance, so N uses are N drifting copies.

The `.jlslib` container is not that capability. It is *packaging for* that
capability. This issue has made the packaging the deliverable and left the
capability — resolution — as one sub-clause of Stage 3. I endorse the end and
reject the cut.

## Reframe 1 — a library is a `.jls` file, and the new file kind should not exist

The issue's own O6 is the argument against it. A `.jls` already carries N nested
`CIRCUIT` blocks recursively (`docs/file-format.md:362-364`); `Circuit.java`'s
`SubCircuit` arm already constructs a fresh `Circuit` per nested block. A file whose
top-level circuit holds N definitions *is already* a bag of definitions. Once #417
mints `defid` and `defdigest`, that file is already an indexable, digest-checkable
library. What is genuinely missing is three things, none of which is a container:

1. an **index section** (frame-level, #444) so the definitions can be enumerated
   without parsing every body;
2. a **provenance section** (also frame-level);
3. **resolution** — a `SubCircuit` carrying a `defid` and no body finding its
   definition somewhere.

Add (1) and (2) as optional sections of the *existing* format and the third falls out.
Consequences, all of them good:

- No second reader, no second writer, no second grammar, no second version integer,
  no second determinism story, no second hostile-input surface to re-argue.
- `-lib list` stops being a library verb and becomes **"what definitions are in this
  file"** — which is useful against a *student's* circuit, not only against a shipped
  library. Same for extract. The generalized verbs are worth more than the specialized
  ones and cost less.
- An instructor's library opens in the editor today, as a circuit, with no new file
  kind to explain to a class. That matters more for this project's audience than any
  property in § Interface.
- Open Question 1 **dissolves**. "Is `MAX_CIRCUIT_TEXT_BYTES` per definition or per
  library?" only exists because a container is imagined as N independent members. One
  framed stream is one budget, enforced by the `BoundedInputStream` that already
  exists (`src/jls/FileAbstractor.java:65`), and #38's arithmetic carries over
  unchanged rather than needing to be re-argued. An open question that is declared to
  block execution disappearing under a reframe is the strongest available evidence
  that the seam was cut in the wrong place.

The one property that appears to need a distinct file kind is "PROVENANCE is
required." It does not — see reframe 3. Provenance is a property of the *role* a file
plays (something you redistribute, something on a search path), not of its container.

## Reframe 2 — resolution first, container later or never

#340 says it plainly: "a plain `.jls` file is a resolution source in its own right,
and a library container is a second one", and the import row "is startable as soon as
TASK-0039 lands and does not wait on TASK-0040." Yet #446 blocks itself on #444 for a
frame it would only need if it built the second source first.

Build the first source first. Ship: `SubCircuit` with a `defid` and no body; a stated
search order; the three refusals #340's IC-8 names (missing / digest mismatch /
cycle). That is #340's criterion 7, priced at 3–6 mw, and it delivers the entire
observable capability — "my circuit references a definition living in another file,
and a changed definition is never silently absorbed" — with **zero new file formats**.
Then look at whether anyone still wants a container. My prediction is that what they
want is a *directory* with an index file, because that is what every instructor
already has and what `git` already versions, and the interesting question becomes
"what does JLS do with a folder on the search path", not "what sections does a
`.jlslib` have".

Note also the cycle invariant. #340's invariant 7 exists *only* because references
replace copies. #446 defines resolution (Stage 3) and never mentions cycle detection —
because it was thinking about containers, where the question does not arise. That
omission is itself a symptom of the wrong framing: this issue owns the resolution
function and does not own the hazard resolution creates.

## Reframe 3 — provenance: strict writer, informative reader

H5 asserts that requiring `PROVENANCE` prevents silent redistribution of unlicensed
material. It does not. A required field does not create a licence; it creates a field.
The people a hard reader-refusal actually stops are the instructor whose hand-built
fixture omitted a line and the third-party tool the issue explicitly wants to enable
(§ Intended Audience, bullet 2). The infringer types `LicenseRef-whatever`.

**I am disregarding P5 as stated.** The design that matches this project's own
supply-chain practice is the inverse asymmetry: JLS's SBOM and provenance attestations
are things the release *produces*, not gates on *consuming* — the README's whole
verification story is "here is what you can check", never "this refuses to run
unattested". Apply the same shape:

- **Refuse to write** a redistributable artifact without provenance. That is where the
  obligation actually attaches and where the author is present to fix it.
- **Read and report.** `-lib list` prints the provenance; a definition instantiated or
  extracted carries its origin forward; a file on the search path with no provenance
  loads and says so once, by name.
- Record the SPDX string; **do not bundle a snapshot of the SPDX list** (OQ2's
  recommended default). `docs/library-survey-2026-07.md`'s ground rules 2 and 4 —
  small and few, nothing unmaintained — apply to vocabularies as much as to jars, and
  a bundled identifier list is an external vocabulary this project must track forever
  for no consumer. Validate on write if at all.

## Reframe 4 — the vocabulary gate entrenches a stopgap the tree is trying to retire

`ElementVocabulary`'s own javadoc: *"This is the stopgap constant list the issue #170
plan sanctions until the element registry (issue #78) exists; when the registry lands,
this class should delegate to it and the reconciliation is to be recorded on issue
#78."* The registry has landed — `src/jls/elem/ElementRegistry.java` is the live tag
table, and `docs/component-naming.md` already sources element slugs from it. O5's
35-vs-34 gap is not a designed asymmetry between two vocabularies; it is the
measurable residue of a reconciliation that has not happened.

#446's OQ5 recommends reusing `ElementVocabulary` and documenting the `TestGen`
consequence. That adds a second consumer to a class whose stated destiny is to be
deleted, and locks a behavioural difference between two loaders for the same bytes —
which the issue's own § Threats to Validity predicts will be filed as a bug and
"fixed" by widening #170's security surface. Building a thing you already predict will
be misread is a design smell, not a documentation problem.

The elegant route is the one #78 already points at: put the **policy on the registry
row**. `ElementType` gains admissibility flags — writable / network-admissible /
library-admissible — and the three vocabularies become three views of one table.
`TestGen` is then one row with two flags off, self-documenting, with `ElementRegistryTest`
as the standing oracle. `ElementVocabulary` shrinks to a delegating façade and #170's
confinement is *strengthened*, not widened, because the allowlist can no longer drift
from the registry. This is a smaller change than the `.jlslib` grammar and it retires
a recorded stopgap instead of adding a tenant to it.

## Reframe 5 — make P11 a ratchet, not a grep pasted in a PR

D7 ("data, not plugins") is right, is consistent with ARCHITECTURE.md's #222 stance
(code providers are a separate, gated, opt-in surface), and is worth mechanizing. The
issue concedes its own grep is weak. This project's culture is ratchet tests —
`HeadlessCoreRatchetTest`, `NotificationRatchetTest`, `ExtensionPointCatalogTest` —
and the same instrument applies here: forbid `java.lang.reflect`, `ClassLoader`,
`ServiceLoader`, `ProcessBuilder` imports in the resolution package, permanently, with
no baseline. This is now *achievable* precisely because #78 replaced `Class.forName`
with registry factories, so no reflection is needed on the load path at all. A
standing test beats a grep in a PR description, and it makes D7 enforceable by the
build rather than by a reviewer remembering to read D7.

## What I would keep from this issue verbatim

The digest-mismatch message naming all three values (P4); refusal-over-placeholder for
an unresolved reference (P9) — that principle is the best sentence in the issue and
should be lifted to the feature; byte-identical writes with both pins stated in the
test (P3); the hostile-compressed-input test (P13); birth under the headless ratchet
with no baseline entry (P12). None of those depend on `.jlslib` existing.

## One structural note on the CLI

`JLSStart.FLAGS` is a flat table of `FlagSpec(name, Arity.NONE|OPTIONAL|REQUIRED, …)`.
`-lib list <file>` and `-lib extract <defid> <out> <file>` are a **verb grammar with
variable operand counts** — a new grammar class in a table whose generated usage text
is pinned by two tests. If the verbs survive the reframe they should be plain flags in
the existing shape (`-inventory <file>`, `-extract <defid> <file>`), which also reads
better against `-savetext` and `-export`, the two flags they most resemble.

## Verdict

**endorse-with-reframing.** The end — verifiable, attributable, referenceable
definitions — is central to the project's arc and under-served today. The stated
acceptance criteria I am explicitly disregarding are: the `.jlslib` file kind as a
distinct container (fold the index and provenance sections into the existing format);
P5's hard reader refusal on missing provenance (make the writer strict instead); OQ2's
bundled SPDX snapshot; and OQ5's reuse of `ElementVocabulary` (reconcile against the
registry instead, per #78's own recorded direction). Sequence resolution before
packaging: #340's criterion 7 against plain `.jls` files delivers the capability, and
whether a container is still wanted afterwards is a question the field will answer
better than this issue's six open questions can.
