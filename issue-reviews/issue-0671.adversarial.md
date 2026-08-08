# Issue #671: TASK-C265-5: macOS gets its advisory JDK-26 leg, so the next JDK breaks a lane before it breaks a release
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Add `java: [25, 26]` to the `macos` job's matrix in `.github/workflows/ci.yml`
(currently a single hardcoded JDK 25, lines 259-297), mirroring `build`
(lines 28-117) and `windows` (lines 143-244), with JDK 26 advisory and the
byte-stable name `Build (macOS, JDK 25)` preserved for the JDK-25 leg.

## Findings, most severe first

### 1. "Mirroring the build and windows job shape" is a false equivalence, and the wrong reading silently breaks a global invariant

`build` and `windows` do **not** share one shape for advisory-ness:

- `build` (ci.yml:41): `continue-on-error: ${{ matrix.java != 25 }}` — a
  **per-leg** switch. JDK 25 is required; only JDK 26 is advisory.
- `windows` (ci.yml:156): `continue-on-error: true` at the **job level** —
  both legs advisory, because the Windows lane itself hasn't been promoted
  yet.
- `macos` today (ci.yml:263): also job-level `continue-on-error: true`, for
  the same reason — per #265's own roster, "Stage 2/3 (macOS): burn-in
  failure-taxonomy note ... promote `Build (macOS, JDK 25)` to required
  after a 20-run record" is listed as **"Planned (unfiled)"**, i.e. not done.

The issue's AC1 says "mirroring the `build` and `windows` job shape" as if
these were one pattern. They aren't, and the two readings produce opposite
outcomes:

- Copy `windows`'s shape (job-level `continue-on-error: true`): correct —
  matches macOS's actual, un-promoted state.
- Copy `build`'s shape (per-leg `continue-on-error: ${{ matrix.java != 25 }}`):
  this **drops the job-level advisory flag and makes the JDK-25 leg
  required/blocking**, i.e. promotes the macOS headless lane, before its
  20-run burn-in record exists. That directly violates #265's own Global
  Invariant 1: "No stage drops `continue-on-error` before its 20-run record
  ... an advisory lane can never wedge CI." Stage 7 (this task) is listed
  in #265 as independent of Stage 2/3 (no incoming edge in the mermaid
  graph), so nothing in the parent feature stops a contributor from reading
  AC1 literally and shipping the `build`-style leg-level toggle, which would
  make a not-yet-burned-in macOS lane a hard merge gate.
- **Recommendation:** AC1 must state explicitly which shape to copy —
  job-level `continue-on-error: true` (matching `windows`'s current,
  un-promoted state) — and should say so in the same sentence as the
  byte-stable-name requirement, not leave it implied by "mirroring."

### 2. The cache-key acceptance criterion is unverifiable and likely unsatisfiable by "mirroring"

AC4: "`cache: maven` keys correctly per JDK leg so the two legs do not
evict each other's caches." Two problems:

- `actions/setup-java`'s built-in `cache: maven` computes its key from
  `runner.os` + a hash of `**/pom.xml` — it does not incorporate
  `java-version` by default (this exact gap is the subject of upstream
  issue `actions/setup-java#705`, requesting an extra key component for
  precisely this multi-job-same-pom scenario). If that's still the
  behavior in the pinned `actions/setup-java@v5.7.0` (ci.yml:59), the two
  new macOS legs — same OS, same pom — would collide on cache key.
- Yet the task's own boundary is to *mirror* `build` and `windows`, and
  **both of those already run two-JDK matrices with plain `cache: maven`
  and no extra key component** (ci.yml:63, ci.yml:182). Mirroring them
  cannot simultaneously "fix" a collision that mirroring reproduces.
- #265's own "Costs/constraints" section (the parent feature) only claims
  "`cache: maven` keys **per-OS** automatically" — it never claims
  per-JDK-version keying, undercutting the premise that this already works
  correctly.
- No test, log assertion, or CI step is specified to verify AC4 is met — a
  PR could "satisfy" it with an unsubstantiated claim in the description.
  **Recommendation:** either drop AC4 (if the existing `build`/`windows`
  precedent is accepted as adequate), or make it concrete: add
  `matrix.java` to an explicit cache key (e.g. via `cache-dependency-path`
  or a hand-rolled `actions/cache` step) and require a linked run showing
  two distinct cache keys/hits in the step logs.

### 3. AC3 references a taxonomy that doesn't exist yet, despite the task claiming independence

AC3: "any JDK-26-only failure classified against the Stage 2 taxonomy
rather than muted." Stage 2 ("burn-in failure-taxonomy note") is listed in
#265's own task roster as **"Planned (unfiled)"** — no `docs/*.md` file
exists yet (confirmed: no macOS-taxonomy doc under `docs/`), and the only
extant taxonomy content is a category list embedded in #265's issue body
prose. The task header declares `ordering_after: []` ("independent of every
other stage"), but AC3 is not actually independent — it asks for
classification against an artifact that may not exist when this task runs.
**Recommendation:** either point AC3 at the concrete category list already
enumerated in #265 §5 item 5 (line-ending, path-separator, file-locking,
case-sensitivity, HeadlessException/AWT, `/proc`-gated, toolchain,
tcc-permission) so there's something to classify against today, or add
Stage 2 as a soft prerequisite instead of claiming full independence.

## What's solid

- The Stage-7 framing is accurate: #265's own roster lists "Stage 7
  (macOS): advisory JDK-26 leg on the macos job (matrix, mirroring
  build/windows)" verbatim, so the task is correctly scoped against its
  parent feature.
- The byte-stable-name requirement (AC2, second half) is correct and
  well-reasoned: templating the job name to
  `Build (macOS, JDK ${{ matrix.java }})` renders identically to today's
  literal `Build (macOS, JDK 25)` for the JDK-25 leg, so branch-protection
  registration genuinely doesn't churn.
- The boundary statement ("no source changes to make JDK 26 pass; a real
  JDK-26 divergence is a finding to file") is appropriately narrow and
  prevents scope creep into fixing whatever JDK 26 turns up.
