# Issue #769: TASK-C578-2: a validator checks a kit directory against the convention and names what is missing — and CI runs it over every shipped kit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the wording and the claim is: *a course kit must be a mechanically
checkable contract, not a prose description, because the person who has to
satisfy it is a stranger who cannot ask a maintainer.* That claim is
squarely on JLS's arc. This repository's whole character is normative
document plus a test that keeps the tree honest about it — `SaveTags`/
`FileFormatSpecTest` over `docs/file-format.md`, `CliFlagTableTest` over the
`FLAGS` table (#71), `HelpTopicsTest`'s palette-completeness check, and most
explicitly `docs/extension-points.md`: *"adding a typed-now row without a
constant, or a constant without a row, is a build failure."* #769 is that
same discipline pointed at content instead of code. Endorsed as a goal.

What I do not endorse is the artifact it names. "A validator" + "CI runs it"
describes a new standalone linter with its own CI lane, and three concrete
things in this tree say that is the wrong shape.

## Reframing 1 — the validator belongs in the jar, and CI is `mvn verify`

The issue's own Outcome says the point is that *"a third-party author gets a
list rather than a rejection."* A CI-lane script cannot do that: the author
is outside the repository. They have the jar (README: the self-contained jar
is the deployment model labs rely on) or the container image
(`ghcr.io/anadon/jls`, batch-only). A validator they cannot run is a
rejection with extra steps.

So: one implementation, `jls.kit.KitValidator`, headless-by-construction the
way `jls.sim.Simulator` is (#77, `HeadlessCoreRatchetTest` — the container
image has no display stack), exposed as one new `FlagSpec` row in
`src/jls/JLSStart.java:759` (`-kit <dir>`). Then the enforcement half is a
`test/jls/KitConventionTest` that calls the same class over every in-tree
kit. CI runs it because `mvn verify` runs it — no new lane, no second
implementation, no drift between what the author checks and what the build
checks. The precedent is already in tree:
`examples/autograde/autograde.py` is pinned by
`test/jls/AutogradeBridgeExampleTest.java`; do the same, but with the
validator inside the product rather than beside it.

The output contract is already written, too, and neither #767 nor #769
notices it: per ARCHITECTURE.md's CLI contract, findings are *results* and
belong on stdout, one line each with name and location; `jls: error:` on
stderr is reserved for the run failing; exit 0 clean / 1 violations found /
2 usage error. That gives the issue's third AC (required vs optional vs
malformed) a natural encoding as a severity column rather than three
bespoke report modes.

## Reframing 2 — the manifest grammar is the load-bearing decision, and it is undecided

#767 asks for "a metadata file" and #769 asks for a validator over it, and
neither says what it is written in. The tree makes that expensive:
`pom.xml` carries exactly four runtime dependencies — `org.tukaani:xz`,
`org.jfree.svg`, `flatlaf`, `jspecify`. There is no YAML or JSON parser. A
`kit.yaml` therefore drags a new library into a shaded, byte-reproducible
jar with a published CycloneDX `bom.json` and a GPL-compatibility audit,
for the sake of a five-field metadata file. That cost is invisible in a
0.5–1 mw band.

Two cheaper routes, both using something the project already owns:

- **`java.util.Properties`** for the manifest. Five keys — id, version,
  `jls-version-range`, `content-license`, lab list — is not a document that
  needs a document format.
- **Better: reuse `jls.module.ModuleManifest`'s vocabulary.**
  `src/jls/module/ModuleManifest.java` already validates id, `apiVersion`,
  and provides/requires/optional/after/before on construction, with
  defensive copies. #767's required metadata (identity, version, targeted
  JLS range, contents, license) is that record with two fields added. A kit
  is data, not a `JlsModule`, and must not become one — but the *grammar,
  id rules, and version-range semantics* should be one thing in this
  project, not two. Then `KitConventionTest` can follow
  `ExtensionPointCatalogTest`'s bidirectional cross-check exactly: a kit in
  tree without a spec row, or a spec row without a kit, fails the build.

Likewise, whatever checks a kit's `-t` vector files must parse them with the
same code `-t` uses (`jls.elem.TestGen`/`SigSim`, reached from
`JLSStart.java:251`), never a re-implemented regex. A second parser for a
documented stability contract is how the convention drifts from the tool
even while the validator stays green.

## Reframing 3 — validate by running, not by looking (I am setting aside the boundary here)

This is the one I would argue hardest for, and it crosses the stated
boundary between #769 and #575 deliberately.

A structural validator is the weakest true statement you can make about a
course kit. A kit with `schedule.md`, `rubric.md`, and eight vector files
that grade nothing passes. The check that actually protects an instructor is
#575's AC-2: the reference solution grades green, a planted-defect variant
grades red. That check is the same shape as
`BatchSimulationGoldenTest`/`AutogradeBridgeExampleTest`, and #576's AC-3
walks a near-identical cycle. Three issues are each building a variant of
one CI motion.

Concrete alternative: **make the manifest executable.** Each lab entry
declares its starter, its reference solution, its planted-defect variant,
and its vectors; `jls -kit <dir>` resolves them and runs each through batch
mode, asserting the declared verdict. Structural conformance then falls out
as a precondition — "grading vectors missing for lab 04, declared at
`kit.properties:22`" is a *better* missing-part message than a schema
error, because it names the part by the use it was about to be put to. One
lane, one artifact, and #575's AC-2 and this issue's AC-2/AC-4 become the
same green run rather than three.

The seam this issue cuts along is artifact type — doc (#767), checker
(#769), license and review (#772). The seam I would cut along is guarantee:
(a) *a kit is something JLS can load and run* — executable manifest plus
runner, which is #767+#769+#575 AC-2 fused; (b) *a kit is something a
stranger can author* — authoring doc, content license, external review,
which is #772 unchanged. Under that cut, the specification document is
mostly derivable from what the runner demands, which is also the only way it
stays true.

## One thing the trio never asks, and one alignment tension

**Never asked: what a kit is when it leaves the repository.** Every AC here
says "in-tree kit." But an instructor receives a kit as a download from a
stranger. This project has unusually strong machinery for exactly that
problem — SHA256SUMS, build-provenance attestations, cosign, a reproducible
jar — and the kit story ignores all of it. A kit should be a single
verifiable artifact (a zip with a manifest, checksummed, attested when
first-party) that `-kit` validates *before* an instructor opens anything,
not only a directory the maintainer already trusts. Relatedly, ARCHITECTURE's
#222 trust-boundary decision should get its content clause here: **a kit is
data-only; a kit carrying executable content is invalid**, asserted by the
validator, mirroring collab's closed data-only op vocabulary. That is a
missing acceptance criterion with real value and it costs almost nothing.

**Tension: a validator over one kit is a tautology.** This project is
disciplined about demand gates — #212's external providers wait for one,
i18n is a recorded non-goal with named revisit triggers, the plugin loader
was removed rather than maintained speculatively. #578's only real gate is
AC-5's named external instructor, and #772 places it *after* this task. A
convention with a single instance is a description of that instance; the
validator only starts earning its keep at the second kit, which is #577's
CSE 260M corpus. I would order this task after that second kit exists, or
at minimum have the #509 instructor see the layout before the checker is
built around it. Building the enforcement before the second subject is the
one place this issue pulls against the project's own habits.

## Net

Endorse the guarantee; reframe the artifact. Ship the validator inside the
jar behind a `-kit` flag, enforce it with a JUnit test in `mvn verify`
rather than a new lane, reuse `ModuleManifest`'s grammar and `TestGen`'s
parser instead of inventing either, and make the check behavioral so it
fuses with #575 AC-2 instead of shadowing it. Add the data-only criterion.
Sequence it after a second kit exists.
