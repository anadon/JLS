# Issue #750: TASK-C546-3: one command emits narrative and tactile SVG together for the same circuit, byte-identically on every platform
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Strip the wrapping and #750 asserts two properties about the CAP-26 (#507) blind
lab path, neither of which is really about a command:

- **A. Joint identity.** The student's prose narrative and the student's swell-paper
  sheet describe the same circuit.
- **B. Reproducibility.** The artifacts are a function of the circuit, not of the
  machine that made them.

The issue proposes one mechanism for both: co-invocation for A, a three-platform
byte comparison for B. Both mechanisms are weaker than the properties they stand
for, and the second one is not the convention the issue claims to be following.

## A. Co-invocation is the weakest available form of joint identity

The failure the outcome paragraph names — "the two artifacts a student receives
cannot describe different versions of the design" — happens *after* the export.
A course repo checks in `adder-narrative.txt` and `adder-tactile.svg`; someone
edits the circuit and re-runs the SVG half for a page-size fix; a TA emails one
file. A single invocation at time T says nothing about the pair at time T+1, and
AC-4's "partial bundle only by explicit flag" defends a moment nobody was in
danger during. It buys the appearance of the guarantee, not the guarantee.

The property that survives the files leaving the machine is **verifiable joint
provenance**: each artifact carries the digest of the circuit text it was derived
from, the JLS version, the `FORMAT` version of the source, and (for the SVG) the
guideline edition TASK-C546-2's lint enforced. Then the disability-services office
holding two files can *check* that they match — which is the actual user of this
property, and the one person the co-invocation design leaves with no recourse.

This project already thinks exactly this way at every other boundary and has never
brought it inward: `SHA256SUMS`, `bom.json`, `.buildinfo`, build-provenance
attestations (README), the `FORMAT 1` header that makes a save self-describing
(`src/jls/Circuit.java`, `readFormatHeader`). The accessible bundle should be the
first *content* artifact to carry its own provenance rather than the first one
whose integrity depends on how it was invoked.

## B. The determinism criterion, as ordered, cannot be met — and cites a convention that isn't there

AC-2 says byte-identity is asserted "the way every other export in this project
asserts it." It is not. `docs/reproducibility.md` §4 records the actual convention:
a **same-runner double build** plus an **independent perturbed rebuild** varying
`TZ`, `LC_ALL`, `umask` and workspace path — both on one Linux runner, over the jar
and BOM only (`.github/workflows/ci.yml`, `reproducibility` job, line ~798). No
JLS export asserts cross-OS byte identity today, and nothing in CI collects
artifacts from several OS jobs and diffs them. A JUnit class named
`AccessibleExportDeterminismTest`, run independently on three machines, structurally
cannot compare bytes across those machines; it can only compare against something
checked in.

Worse, the two lanes AC-2 depends on are advisory: `windows` (ci.yml:146) and
`macos` (ci.yml:259) are both `continue-on-error: true`, promoted to required only
after 20 near-clean runs. A determinism gate that cannot fail the build is a report,
not a gate.

And the feasibility is decided upstream of this issue, not in it.
`test/jls/SvgExportTest.java`'s own javadoc records the blocker verbatim:
"Deliberately no full-document golden - text layout coordinates depend on the JDK's
font metrics, which differ across machines." The existing SVG path
(`src/jls/edit/CircuitRenderer.java:312-360`) has already been engineered for
*same-machine* determinism — fixed `setDefsKeyPrefix("jls")`, an explicit
`drawOrder` comparator so `HashSet` iteration never reaches the bytes — and stops
exactly at the font boundary; `FontMetrics`/`stringWidth`/`drawString` appear ~300
times across `src/jls/edit`. So whether AC-2 can pass is settled by whether
TASK-C546-2's tactile emitter draws through `Graphics2D` or emits from the model.
#750 is `ordering_after: [TASK-C546-2]`, which means the decision that determines
its pass/fail is made in an issue that does not own the criterion.

## The reframe: the bundle is a manifest, not a command

One artifact absorbs both properties and both problems.

Emit, alongside the pair, a small **bundle manifest** — the circuit source digest,
the JLS version, the source `FORMAT` version, the guideline edition, and the sha256
of each artifact — and:

1. **A becomes checkable by the holder.** The pair is joined by content, not by
   invocation. Re-emitting one half is now detectable rather than forbidden, so
   AC-4 relaxes from a correctness claim to what it actually is: a good CLI default.
2. **B becomes a normal unit test.** Check the manifest of a fixture circuit into
   `test/` as a golden. Every lane — including the required Linux one — recomputes
   and compares. That *is* cross-platform byte identity, expressed as something a
   test class can do, and it stops depending on two advisory lanes to be meaningful.
3. **Determinism gets a construction constraint, not just an assertion.** Mirror
   `HeadlessCoreRatchetTest` (ARCHITECTURE.md: `jls.sim` imports no AWT/Swing/edit):
   a ratchet asserting the accessible-export package imports no AWT/font/default-
   locale/wall-clock API. That is determinism proved on one machine by construction,
   and it is the criterion that belongs *in* TASK-C546-1 and -2, where the emitters
   are written, rather than bolted on afterward here.
4. **Adopt the perturbation axes already in CI** (#185: `TZ=Pacific/Kiritimati`,
   `LC_ALL=C`, `umask 077`, renamed workspace) for the bundle export in the required
   lane. Strictly stronger than three same-defaults runners, already implemented,
   and it catches the locale-formatted number and directory-order bugs a
   three-OS diff would also catch — a week earlier and in a blocking lane.

Net: #750 keeps its outcome, sheds a criterion it cannot enforce, and gains one it
can.

## The seam I would cut differently: an export job, not a bespoke mode

JLS's CLI is strictly one-mode-one-artifact today — `start()` dispatches through an
`if / else if` chain (`JLSStart.java:282`, `:363`), `guiSessionRequested()`
(`:930`) enumerates the four headless modes, and combining two silently runs one.
#750 mints the first multi-artifact mode. That crack is worth opening deliberately
and exactly once, because at least three consumers want the same shape: this bundle;
CAP-24's camera-ready print (#540/#536), which will want diagram-plus-legend; and
autograders, which today invoke JLS three or four times (README's container recipe
runs `-b -t`, then separately `-vcd`, `-i`, `-export`), reloading and re-simulating
each time.

So: design **one invocation → N artifacts → one manifest**, with the accessible
bundle as its first instance and the autograder trio as the obvious second. The
manifest above is the same manifest. Given `docs/batch-interface.md` is a stability
contract, the manifest needs a version marker from day one, the same way saves have
`FORMAT 1` — and the bundle must be added to that document and to README's list of
what the container's headless surface offers, or AC-3's "reachable from CI and a
course repo" is a documentation edit nobody scheduled.

## What I am explicitly disregarding

**AC-4.** Keep pair-by-default as ergonomics; drop it as an acceptance criterion.
Under the manifest, a lone tactile SVG is a legitimate, self-describing artifact,
and the criterion as written mostly makes the regenerate-one-half case awkward
while defending nothing. Guaranteeing "cannot describe different versions" by
restricting the CLI is a guarantee about JLS's users' obedience; guaranteeing it by
digest is a guarantee about the files.

**AC-2's phrasing.** Replace "byte-identical across the three CI platforms" with
"the manifest golden matches in every lane, the export package passes the
no-nondeterministic-API ratchet, and the bundle reproduces under the #185
perturbation axes in a required lane."

## Alignment

Strengthens the arc: this is the one place in CAP-26 where the deliverable leaves
the machine and lands in a stranger's hands, and it is the natural home for the
project's provenance discipline to finally point inward. Pulls against it as
written: it asserts a determinism convention the repo does not have, on lanes that
cannot fail, for an emitter whose font-metric dependence is already documented as
the reason goldens were avoided — and it hard-codes a one-off CLI mode where a
reusable job seam costs about the same and pays for #540 and the autograder path
on the way past.
