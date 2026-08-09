# Issue #111: Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg (ex: Windows test-suite failures, fixed)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the seven stages away and the claim is: *a defect that only manifests on Windows
must not be able to reach a user.* That is a good claim and it is proven, not speculative —
the defect phase caught three real ones (CRLF-specialized `println` in `Circuit.save`, a
`Scanner` held over a live file stream in `FileAbstractor`, `autocrlf` rewriting
`test/resources/**` goldens). Two of the three were product bugs affecting real Windows
users, not CI artifacts. The goal stands.

What I want to challenge is the identification of *that goal* with *this mechanism*:
"run the entire `mvn verify` on three operating systems and make all of it required."
That equation is assumed everywhere in the body and never argued. It is expensive
(15 jobs in `ci.yml` today, 23 `continue-on-error` sites, three trackers — #111, #265,
#317 — plus a deduplication pass that has already had to adjudicate the boundary between
them twice), and, more importantly, it is not what caught the bugs.

## The evidence the project has already generated against its own plan

Look at how the three defects are actually *pinned* now:

- `DeterministicSaveTest.canonicalBytesAreIdenticalWhateverThePlatformNewline`
  (`test/jls/DeterministicSaveTest.java:106`) does not run on Windows. It **simulates**
  a CRLF platform by subclassing `PrintWriter` to specialize `println` the way the
  platform does, and asserts the save bytes are unchanged. It runs on Linux, in the
  required gate, in milliseconds.
- `ToolkitPolicy.decide(osName, waylandDisplay, display, override, hasWlToolkit)`
  (`src/jls/ToolkitPolicy.java:125`) takes the platform as a **parameter**.
  `ToolkitPolicyTest` exercises `"Linux"`, and would equally exercise `"Windows 11"`,
  on one machine.
- `MenuAcceleratorPolicy.menuMask(osName)` / `.isMac(osName)` — same idiom;
  `MenuAcceleratorPolicyTest:72` loops over `{"Mac OS X", "Linux", "Windows 11"}`.

So the repository has independently invented the same architectural move three times:
**make platform-dependence an injected value and test it exhaustively where compute is
free.** That move is the project's real trajectory — it is the same shape as
`SaveTags` (frozen table, no `Class.forName`), `HeadlessCoreRatchetTest` (an invariant
asserted structurally rather than by running headless somewhere else), and
`ExtensionPointCatalogTest` (a declared table cross-checked against reality in both
directions). #111 does not use that move once. It is the only large piece of recent work
that answers a platform question by *renting a machine of that platform* instead of by
*naming the platform-dependent seam and pinning it*.

Notice also that `FileHandleReleaseTest` — the pin for defect 2 — reads `/proc/self/fd`
and `assumeTrue`-skips off Linux (`test/jls/FileHandleReleaseTest.java:41`). W6 exists
purely to repair that decision. But the leak itself was **platform-independent**; only
its symptom (a locked file) was Windows-visible. The right fix is not a Windows
`Handle.exe` analogue; it is to stop observing an OS-specific proxy for a
JVM-level property.

## Alternative framing 1 — a hostile-platform profile, not a hostile platform

Add one surefire execution, on Linux, in the required gate, that runs the existing suite
under deliberately adversarial platform conditions: `-Dline.separator=\r\n`,
a non-UTF-8 `file.encoding`, `-Duser.language=tr` (the dotted-I trap that breaks every
naive `toLowerCase` on element tags), and — for the handle class — a
`Files.newByteChannel` wrapper or a JFR/`FileDescriptor` census rather than `/proc`.
Wire the same os.name injection into any code that still asks the platform directly
(`SimpleEditor.java:1454`, `:1648`, `JLSStart.java:1384`, `:1660`, `:1676` are the
remaining direct `System.getProperty("os.name")` call sites — five seams, all of which
could be parameters).

All three original defects die in that profile, in the required gate, in seconds, on
every PR, on contributors' laptops — *before* a Windows runner is asked anything. The
Windows lane then stops being the primary net and becomes what it is good at: a backstop
for what cannot be simulated (real Win32 file locking semantics, the actual window
station, the real toolchain).

This is not a reason to drop the Windows lane. It is a reason to stop treating
"promote everything to required on three OSes" as the definition of parity, because
the cheaper mechanism catches a strictly larger class of bug (it also catches
Windows-only defects in code paths no test currently reaches on any platform).

## Alternative framing 2 — a skip census is the parity primitive

The single mechanism that would collapse four of these seven stages: make **executed-vs-skipped
legible and gated.** There are 41 test files containing assumptions and 25 `@Tag("display")`
classes; whether a given lane actually ran them is currently answered by humans reading
run logs and writing tracker comments.

Concretely: each lane emits its skip set (test id + assumption message) as a build
artifact; a checked-in `platform-skips.yaml` declares every legitimate skip per platform
with a reason and an owning issue; a single test (in the established
`*RatchetTest` idiom) fails the build when a lane skips something not declared, or
declares something it did not skip. That one artifact subsumes:

- **W2 close-out** ("run log shows iverilog/ghdl/yosys actually invoked") — becomes an
  assertion, not evidence-capture in a comment.
- **W3's real requirement** ("a zero-executed display run must fail rather than pass").
- **W6** — a documented narrow skip becomes a *machine-checked* declaration instead of a
  code comment nobody re-reads.
- Most of what #386 was filed for, and the fail-closed-vs-self-skip contradiction the
  2026-08-08 audit had to adjudicate by prose.

It also generalizes: adding macOS, or the riscv64 target the README already ships images
for, is a column in a table rather than a new nine-stage tracker.

## Disregarding two stated acceptance criteria, and why

**W4 (a Windows JaCoCo floor) should be deleted, not answered.** Open Question 1 asks
"per-OS floor vs shared floor"; the correct answer is *neither*. Coverage is a property
of the source, not of the operating system. A second floor is a second ratchet to
maintain, a second thing to lower under pressure, and it measures nothing the Linux floor
does not — while invariant 2 (Linux stays sole gating authority) already concedes the
point. What W4 actually wants is "the Windows lane isn't green by running less," and that
is the skip census above, measured in test identities rather than in line percentages.
Test-identity parity is both cheaper and strictly more precise than coverage parity.

**W3's "green and required" should narrow to the non-Robot subset.** The display suite
contains 11 `Robot`-driven files and already carries `rerunFailingTestsCount=2` in
`pom.xml` because Xvfb realization timing is nondeterministic. Promoting synthetic
pointer/keyboard gestures on a hosted Windows window station to a *required* check is
asking a known-flaky substrate to produce 20 consecutive clean runs; the likely outcome
is either a permanently advisory lane or quiet erosion of the promotion rule. Require
the dialog-construction / `HeadlessException` half — which is where the genuine
Windows-specific AWT risk lives — and keep the Robot gestures advisory with a named
successor issue.

## Where this pulls against the project's arc

Three features now stand over the same CI ground, and the last four comments on this
issue are boundary adjudications, roster corrections, and a create-audit that closed
four of six children as duplicates. After that audit, #111 uniquely owns exactly two
things: #665 (handle observation) and #666 (`GUI boot (Windows, WindowStation)`
promotion). Everything else is discharged through #406 and #386.

That is the strongest signal in the whole record. The issue's decomposition axis —
*by operating system* — cuts across the axis the work actually falls along —
*by suite family and by policy*. Cutting per-OS guarantees that Windows and macOS
trackers restate each other stage for stage, which is precisely what the dedup passes
keep discovering and then arguing are "siblings, not copies." They are siblings because
the seam was cut in the wrong place. The right seam is: **one parity contract
(the skip census + the per-suite arming table), one policy owner (#317), and
per-OS issues that hold only what is genuinely irreducible to a table row** — for
Windows that is the oss-cad-suite bundle pin and the window-station rig, and very
little else.

## What I would keep exactly as written

- The `advisory → 20-run record → required` discipline. It is the right invariant and
  the `installer-reproducibility-aarch64` precedent makes it concrete.
- Invariant 4: fix at source, never broad platform muting. This is the load-bearing
  cultural commitment and it visibly worked — the font-metric hypothesis was refuted
  rather than papered over.
- The rig exit 0/1/2 taxonomy (environment faults are never blamed on the app). That is
  genuinely good design and worth generalizing to every rig the project grows.
- W7. A real app-boot smoke check on a real window station is the one stage here that
  no amount of simulation on Linux can replace.

## Concrete restatement of the goal

> Windows-specific behavior is a named, injected parameter of JLS's code, exhaustively
> tested wherever tests are cheapest; every lane's skip set is declared and machine-checked
> so no lane can be green by running less; and one Windows lane plus one Windows app-boot
> rig gate the residue that only a real Windows machine can answer.

That is the same end #111 wants, reached with two mechanisms instead of seven stages,
and it survives the addition of a fourth platform.
