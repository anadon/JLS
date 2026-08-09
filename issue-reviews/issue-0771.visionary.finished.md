# Issue #771: TASK-C550-2: starter circuit versus welcome pane is decided in writing, and startup time becomes a per-commit regression check
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Two things are bundled here that have almost nothing to do with each other:

1. **An architecture decision** — starter circuit or welcome pane — that scopes what
   #770/#381 actually build.
2. **The project's first performance gate**, justified by CAP-27's kill criterion
   KC-27-1, defending against the possibility that the first-run surface makes
   `java -jar` slow.

The underlying aim is right and squarely on the project's arc: CAP-27 says the
on-ramp scored 2/5, #73/#381/#550/#770 are all converging on the same surface, and
K9/D9's "the first-year must never SEE the ECE/EE machinery" is a real constraint
that deserves a written decision. Nothing below disputes the destination. What I
dispute is the mechanism, the ordering, and where the decision gets filed.

## Where it pulls against the project

**1. The ordering is inverted; the gate defends a cost the decision can delete.**
A welcome pane is a `JPanel` with three `Action`-bound buttons (#381 P9). Its
contribution to startup is unmeasurable next to FlatLaf class loading in
`JLSStart.installLookAndFeel()` (`src/jls/JLSStart.java:994`) and `JFrame`
realization. A **starter circuit** is the only option with real boot cost: it means
`FileAbstractor.openCircuit` → `Circuit.load` → `finishLoad` plus a classpath scan
of `resources/samples/`, on the boot path. So the entire justification for AC-2/3/4
is contingent on AC-1 choosing the starter circuit. The issue builds a permanent
per-commit gate *first* (`ordering_after: [TASK-C550-1]` places it after the panel,
not after the decision) against a risk its own AC-1 may erase. Decide first; then
you know whether a startup instrument is needed at all.

**2. A wall-clock budget is the wrong species of gate for this codebase.**
Every ratchet JLS has is structural and deterministic: `HeadlessCoreRatchetTest`
(no AWT imports under `jls.sim`), `NotificationRatchetTest` (no raw `JOptionPane`),
`SocketConfinementRatchetTest` + `ArchitectureRulesTest` (bytecode), the
`SaveTags`/constructor contract tests, `ThemeTest`'s ΔE≥25 bar, #381's renderer
color allowlist with its non-vacuity clause. There is no benchmark harness, no JMH,
no timing assertion anywhere in `test/` — the one `assertTimeoutPreemptively` in
`BootListenerHygieneTest` is a deadlock detector, not a budget. Three concrete
frictions follow:

- **The GUI boot is not reachable from the per-commit lane.**
  `BootListenerHygieneTest` says in its own javadoc why it never calls
  `JLSStart.start()` or `new JLSStart()` (`System.exit`, `HeadlessException`). GUI
  startup is only measurable in the `@Tag("display")` suite or the rigs — and
  `mvn verify` sets `<excludedGroups>display</excludedGroups>` (pom.xml:271), while
  the required PR check does the same (ci.yml:759-760, 788). "Per-commit" therefore
  means the `gui-x11`/`gui-wayland`/`windows-gui`/`macos-gui` lanes — exactly the
  lanes pom.xml:289 retries twice because popup realization is nondeterministic.
  #381 §11 already names the failure mode: *"a new display test that only passes on
  the retry is a flake, not a pass."*
- **The existing instrument has 1-second resolution.** `scripts/x11-rig.sh` measures
  boot with a `sleep 1` poll loop and logs `"JLS window is up after ${WAITED_SECONDS}s"`
  (:263, :321). The threat model — loading one starter circuit — is 50–300 ms. Either
  a sub-second instrument is built from scratch, or the check is vacuous against the
  only regression it exists to catch.
- **The "calibrate from measurement" idiom already has an unpaid debt in the same
  lane.** ci.yml:509-513 carries `PIXEL_DIFF_MIN: "0"` with
  `TODO(maintainer): after the first green run, set this to ~10% of the observed AE
  value ... not a blind guess`. That is precisely AC-2's shape, and it is still
  uncalibrated. Adding a second record-now-threshold-later knob to the same lane
  entrenches a pattern the repo has already demonstrated it does not close out.

**3. AC-4 asks one check to assert three incommensurable things.** "Default palette
unchanged" is already pinned by `ThemeTest` — restating it here duplicates a live
ratchet. "No added conceptual load for a first-year drawing an adder" is a human
judgment; #381 §11 correctly classifies it as *"a K9 question, not a taste
question"* to be argued in the PR. Making it an acceptance criterion of a CI check
forces one of two bad resolutions: a vacuous proxy (count buttons ≤ 4) or a waiver.

**4. AC-3's non-vacuity demonstration is weaker than it reads.** Slowing the boot
path deliberately and recording the red run proves the check measures *something*.
For a timing gate the honest demonstration is a **sensitivity and false-positive
claim**: the smallest injected delay it reliably catches, and the spread across N
runs on the actual runner matrix. A check that only trips on a 5-second sleep cannot
defend against a 200 ms circuit load, and the recorded red run would conceal that.

**5. The decision is homed in the wrong place, with no revisit trigger.** AC-1 files
it as a comment on #550 — an issue that closes. ARCHITECTURE.md has a "Recorded
decisions" section with seven entries (i18n, help delivery, look-and-feel, plugin
removal, plugin trust boundary, extension seams, simulation strategy), and *every one*
carries a rationale **and** a revisit trigger. AC-1 has neither property. A decision
that binds #381, #770, #548 and CAP-27 PF-3 belongs there, in the same shape.

## Alternative A — replace the timing budget with a boot-work ratchet

The property actually worth defending is not "startup takes under N ms on a runner"
but **"nothing new was put on the boot path."** That is deterministic,
machine-independent, and blocks in the ordinary lane:

- Extend the harness `BootListenerHygieneTest` already established — it drives real
  boot cores and asserts a structural absence (no LISTEN socket appears). Add a
  **boot-path work census**: snapshot
  `ManagementFactory.getClassLoadingMXBean().getLoadedClassCount()` and the set of
  initialized top-level packages across the boot core, and ratchet it against a
  written allowlist with a non-empty clause, exactly as #381 §7.10 specifies for the
  renderer colors. A regression then fails with *"boot now initializes
  jls.hdl.yosys"* rather than *"1.34 s on a noisy runner."*
- Add the one assertion that actually encodes the starter-circuit risk: **the GUI
  boot path performs no circuit parse and no classpath directory enumeration** (or,
  if the starter circuit wins, exactly one parse of exactly one named resource).
  That is the real invariant; wall-clock time is its noisy shadow.
- Move wall-clock measurement to where this project already puts empirical
  observations: an **advisory, non-blocking** number recorded by the GUI rigs (the
  README's own "advisory (non-blocking) build on the newest GA feature release"
  pattern), trended in the artifacts directory, with a nightly-only alert. #381 §9
  states the principle the issue should inherit: *"The screenshot matrix is not a
  JUnit assertion and must not be dressed as one."* A wall-clock budget is the same
  category of thing.

This is strictly cheaper than the issue as written, blocks in the required check
rather than the flaky one, and cannot be gamed by a runner having a good day.

## Alternative B — the starter-circuit/welcome-pane binary is false

The issue treats this as A-or-B with a loser to be "explicitly not built." There is a
third framing that dissolves it and is better aligned with the capstone's actual
outcome (*"a running, understood example circuit in under ten minutes"*):

**Open a real, editable adder as the first-run canvas, and let its `Text` elements be
the welcome copy**, with the three shared `Action`s as an unobtrusive strip rather
than a modal pane. D9's operative example is literally *"a first-year drawing an
adder."* A welcome pane leaves the user three clicks and a file dialog away from a
running circuit; a starter circuit is one "Run" away. It also has no dependency on
#548 — note that #770 AC-3 already concedes Open Example may not resolve and must
"degrade to the #381 sample baseline," i.e. the welcome pane's highest-value button
may ship dead, while a starter circuit cannot.

The honest counterweight, which the recorded decision must address and the issue
never raises: a starter circuit creates an **unsaved-buffer question** (is it dirty?
does closing prompt to save? what is its filename? does File→New replace it?). That
is the real cost of option A — not milliseconds — and it is not mentioned anywhere in
#771, #770, #550 or #511. A decision that weighs "startup time" but not "what happens
when the student hits Ctrl-W on the starter circuit" is weighing the wrong axis.

## What I am disregarding, and why

I am **disregarding AC-2, AC-3 and AC-4 as written**. A per-commit wall-clock startup
budget is not a gate this project can hold honestly today: it cannot run in the
required lane, its only existing instrument has 1-second granularity, and its
threshold-calibration idiom already has an open unpaid TODO in the very lane it would
live in. AC-4's palette clause duplicates `ThemeTest`; its conceptual-load clause is
not machine-checkable and should stay a PR argument, as #381 §11 already has it.

I **endorse AC-1's intent** and would strengthen it: record the decision in
ARCHITECTURE.md's "Recorded decisions" section, in that section's established shape —
rationale plus an explicit revisit trigger (e.g. *"revisit if a fresh-user protocol
run under CAP-27 AC-2 shows subjects stalling before the first simulation"*) — and
cross-link it from #550 rather than the reverse.

## Minimal restatement I would file instead

- **TASK-C550-2a (decide):** record starter-circuit-vs-welcome-pane in ARCHITECTURE.md
  with K9/D9 rationale, a revisit trigger, and an explicit answer to the
  unsaved-buffer question if the starter circuit wins. Loser not built. Blocks #770.
- **TASK-C550-2b (ratchet):** extend `BootListenerHygieneTest`'s harness with a
  boot-path work census (loaded-class/package allowlist, non-empty, reasons written)
  and a "no unexpected circuit parse or classpath scan on boot" assertion. Runs in
  the required lane. Red-state recorded by adding a real disallowed call, not a sleep.
- **TASK-C550-2c (observe, advisory):** the GUI rigs record boot wall-clock into the
  artifacts directory as a trended, non-blocking number; nightly-only alerting; no
  threshold until at least 20 runs of the matrix exist to derive one from — and
  close out the `PIXEL_DIFF_MIN` TODO with the same data while the instrument is warm.

That sequence delivers KC-27-1's substance — first launch lands somewhere and cannot
silently get slow — without introducing this repository's first flaky gate.
