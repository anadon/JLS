# Issue #715: TASK-C530-2: the nbgrader gradebook export joins the four-way parity vectors, and the unit README runs as CI doc-test steps
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two things, welded together by adapter ownership rather than by kinship:

1. **The nbgrader unit contributes a per-student score vector** so `CrossPlatformScoreParityTest` (#719) has a fourth column.
2. **The unit's README is executed rather than merely written**, in a dedicated CI lane.

Both goals are right. Neither is well served by being the fourth instance of a
pattern that #699 (Gradescope), #705 (Classroom) and #710 (PrairieLearn) already
carry verbatim. The interesting question is not whether nbgrader should join the
parity claim — it should — but *where the invariant lives*. #715 places it in the
adapter. The project's own best architectural instinct places it upstream.

## Reframing 1: invert the emission direction — the score vector is a JLS artifact, not an adapter obligation

AC-1 says the gradebook export is "emitted in the shared parity-vector form, with
no post-hoc normalization, ready for `CrossPlatformScoreParityTest`." Multiply
that by four adapters and the shape of the design is: *four independent programs
each compute a score vector, and a test in CI checks that they agree.* #719's own
AC-2 senses the danger — "extraction is defined once, in the fixture, rather than
as four per-adapter normalizers" — but resolves it by putting the single
definition in the **test fixture** (#717's golden vectors), which means the
*shipped kit* still contains four score computations and only JLS's CI knows they
agree. An instructor who edits the Gradescope adapter gets no guarantee at all.

JLS already solved exactly this problem one level down, and #466 states the
solution in so many words:

> Because both factor through the same runner, verdict-list equality is a
> **theorem rather than a test target** — P3 asserts it anyway; if it fails, the
> two paths are not sharing a runner.

That is `TestVectorRunner`: one headless parse-run-compare entry point behind both
the CLI and the GUI panel. CAP-21 restates the identical problem at the platform
layer and then reaches for empirical four-way byte comparison instead of the same
structural move.

**The alternative.** Make the per-student score vector a first-party output of the
frozen CLI contract (PF-1 / #524), computed once from the xUnit report and the
rubric that #466 already requires (`examples/autograde/lab-01/` ships "a rubric
mapping checks to points" and a `grade.py` emitting "one xUnit file each plus a
summary"). Call it what it is — a canonical, byte-deterministic score vector,
goldened in the `GradeReportGoldenTest` style, carrying no timestamp, hostname or
duration for the same reason `GradeReport` carries none. Then:

- Each adapter becomes a **pure formatter**: vector → `results.json`, vector →
  Classroom summary, vector → PrairieLearn results, vector → nbgrader gradebook.
- #715's AC-1 stops being an obligation on the notebook. The hidden cells from
  #713 already invoke the pinned build as a subprocess; they read the vector the
  subprocess wrote. "No post-hoc normalization" becomes structurally impossible
  rather than a promise a reviewer has to police.
- #719 collapses from a four-way byte comparison across four native formats into
  one round-trip property per adapter: the formatter's output re-projects to the
  canonical vector unchanged. That is a *stronger* claim (it holds for every
  input, not just the 300-submission corpus) and a much cheaper one.
- The parity guarantee ships to instructors, not just to JLS's CI.

This also dissolves an ordering knot. #715 declares `ordering_after: [TASK-C530-1,
531]` — the adapter's export shape waits on the whole parity fixture, because the
fixture is where the vector shape is defined. Move the shape into the CLI contract
and #715 waits only on #524 and #713, and can land long before #717's 300
submissions exist.

## Reframing 2: AC-4 is aimed at the wrong artifact

"Two consecutive autograde runs of the full fixture class produce identical
gradebook bytes." nbgrader's gradebook is a SQLAlchemy/SQLite `gradebook.db`;
byte-identity there is a fight with someone else's storage engine — autoincrement
ids, page layout, freelist reuse, and per-submission timestamps nbgrader reads
from `timestamp.txt`. The CSV that `nbgrader export` produces is more tractable but
still carries `timestamp` and `duedate` columns by construction.

#466 already learned the general lesson and wrote it down as H5: determinism came
from *omitting* the three fields an xUnit writer adds by default. The notebook path
cannot omit them; it can only pin them, and pinning someone else's schema is a
weaker guarantee that will rot on their next release.

**I am disregarding AC-4 as written.** The honest criterion: byte-identity applies
to the canonical score vector the unit writes, which JLS owns end to end; the
gradebook database is compared *as a relation* — rows equal after a declared
projection (student, assignment, per-test score) — and is explicitly excluded from
byte comparison, with the exclusion written into the kit's docs so nobody later
reads AC-4 as a broken promise. That keeps CAP-21's claim exactly as strong as it
can honestly be, and stops a 0.5–1 mw task from blocking on SQLite page layout.

## Reframing 3: build one doc-test runner, not a fourth doc-test lane

AC-2 and AC-3 are, word for word in substance, #699's AC-2, #705's AC-2 and
#710's AC-2. Four issues each independently require "the README executes as
doc-tests in a dedicated adapter lane," hermetically. Written four times, that is
four ad-hoc extractors and four lane definitions to maintain against a `ci.yml`
already at 1145 lines and 16 jobs.

Written once, it is a general capability the project visibly wants and does not
have: a `DocTestRunner` that extracts fenced shell blocks from any markdown file
and executes them hermetically against declared prerequisites, plus **one**
parameterized lane over a manifest of (document, environment) pairs. The project's
instinct already runs this way — `scripts/wayland-rig-selftest.sh` drives the
unmodified rig against a stub toolchain to guard its classification logic, and the
repro-installers workflow re-derives published artifacts rather than trusting them.

And the payoff is far larger than four adapter READMEs. `README.md` is 368 lines of
precise, entirely unverified commands: `sha256sum -c SHA256SUMS`, two `cosign
verify` invocations with certificate-identity regexes, `gh attestation verify
oci://…`, `nix run github:anadon/JLS`, the `-savetext`/`-export`/`-i` examples.
`docs/vcd-interop.md` ships a "worked recipe," `docs/reproducibility.md` an
"independent-rebuild recipe." Every one of those is exactly the class of prose
CAP-21 AC-5 exists to protect, and none of it is protected today. A general runner
lets an instructor's README and the project's own install instructions rot in the
same lane — which is the version of this capability that strengthens the whole
project's arc rather than one platform adapter's corner of it.

## One thing that pulls against the project's grain, and what to do about it

AC-3 requires the lane to run "hermetically with no notebook-hosting service and
no network dependency." Hermetic Jupyter + nbgrader means vendoring a pinned Python
environment — wheels or an image digest — into a repository whose README states
plainly that "nothing beyond Maven and a JDK is required," which deliberately
refuses X11 in its own tooling, and which governs its supply chain with an SBOM, a
reproducible jar, cosign signatures and provenance attestations. A pip resolution
inside a JLS CI lane is a genuinely new class of dependency for this project, taken
on for the least-installed of the four platforms.

That is not a reason to drop nbgrader; it is a reason to size it honestly. Under
Reframing 1 the notebook adapter is a few cells that read a JSON/CSV vector JLS
wrote, and its correctness is checkable by a schema assertion that needs no Jupyter
at all. If a hermetic notebook lane still cannot be built without vendoring an
environment the project will not otherwise maintain, KC-21-3's spirit applies
directly — ship the unit as a documented recipe over the canonical vector, record
the gap, and let the general doc-test runner cover the three container adapters
that can be made hermetic cheaply. Three real platforms beat four, one of which is
carrying a Python toolchain into a Java repo to prove a property that a formatter
round-trip already proves.

## Verdict

**endorse-with-reframing.** The two underlying goals — nbgrader in the parity
claim, its README executed — are sound and belong in the kit. But: AC-1's
per-adapter emission obligation should become a consequence of a first-party
canonical score vector emitted by the frozen CLI contract, with adapters as pure
formatters (which also frees #715 from `ordering_after: 531`); AC-4's "identical
gradebook bytes" should be restated onto the artifact JLS owns, with the gradebook
compared relationally; and AC-2/AC-3's doc-test lane should be one general runner
over a manifest — pointed first at `README.md` and `docs/reproducibility.md`, which
need it more than any adapter README does.
