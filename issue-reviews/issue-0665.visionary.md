# Issue #665: TASK-C111-5: the file-handle probes stop being Linux-only knowledge — Windows observes handle release, or says in writing why it cannot
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The outcome sentence is right and I keep it verbatim: the handle probes must stop
being Linux-only knowledge. #111 invariant 4 says failures get fixed at the source
and platform exceptions stay narrow and justified; a probe that `assumeTrue`s itself
out of existence on two of three supported platforms is the largest standing
violation of that invariant in the tree, and it guards a defect class the project
has already been bitten by once (`b9c787d`, the locked circuit file).

But the acceptance criteria encode a false dichotomy — *find a Windows handle
observer, or write down why you couldn't* — and I am disregarding them, along with
#111 Open Question 2 as posed. Both arms of the dichotomy are bad, and the third
route the issue never considers is strictly better than either: **stop observing the
operating system's handle table at all, and assert the contract that the handle
table is a proxy for.** That route is pure `java.nio`, needs no external tool on any
platform, makes the probes non-vacuous on Windows *and* macOS, and dissolves #265
Stage 8 (the `lsof` analogue) as a side effect.

## Why both stated arms are dead ends

**Arm A (Handle / openfiles / fd-count proxy).** `handle.exe` is a proprietary
Sysinternals binary behind a click-through EULA (`-accepteula`), not redistributable,
and needs elevation for full handle enumeration. `openfiles.exe` needs
`openfiles /local on` plus a reboot before it tracks local handles — impossible on
hosted runners. There is no Java-visible `GetProcessHandleCount`, so the "fd-count
proxy" arm means a JNI/JNA dependency added to a test suite. Every one of those
pulls against this project's most consistent instinct. README and #111 pin
`oss-cad-suite` by URL *and* sha256; the jar and BOM are byte-reproducible; #136
declined GPG signing rather than ship a custody story weaker than the attestation it
already had; #128/#135 declined Apple signing on the same reasoning; README states
flatly that "X11 is deliberately not part of this project's tooling." A GPLv3
educational simulator that refuses a signing key on custody grounds should not
download an EULA-gated closed-source Microsoft utility into a CI lane to learn a fact
Windows will tell it for free.

**Arm B (documented narrow skip).** This ships a comment. AC 2's fallback — "is
demonstrably caught by the existing `@TempDir` cleanup failure" — is weaker than it
sounds: JUnit surfaces that as a container-level teardown `IOException`, unattributed
to any test method, indistinguishable at a glance from runner flake, and dependent on
`CleanupMode` defaults nobody has pinned. It is exactly the accident that started
#111; enshrining an accident as the regression gate is not parity.

## The reframing: assert the contract, not the kernel

The invariant is not "no entry in `/proc/self/fd`". It is **`openCircuit` returns a
snapshot** — after it returns, the returned `Scanner` is independent of the file on
disk. That is observable in ordinary Java on every platform JLS supports:

1. **Snapshot (portable, primary).** After `openCircuit` returns, truncate the file
   to zero length or overwrite it with garbage, *then* drain the `Scanner` and assert
   it still yields the full original circuit text and still `load`s. A stream-backed
   `Scanner` cannot survive that on any OS; a `ByteArrayInputStream`/`String`-backed
   one is untouched. This catches the exact `b9c787d` defect on Linux, Windows and
   macOS with zero platform code.
2. **Deletability (portable, teeth on Windows).** Assert `Files.delete(file)`
   succeeds immediately after `openCircuit` returns. Windows' mandatory sharing
   semantics *is* the handle observer, already installed on the runner. #111's own
   defect history is the evidence AC 2 asks for: the original leak came through
   `SeekableFileInputStream` (`RandomAccessFile`) and `ZipFile`, both of which the
   JDK opens without `FILE_SHARE_DELETE` — which is precisely why `@TempDir` cleanup
   failed. Honest caveat, and the reason (1) is primary: a *future* leak introduced
   via `Files.newInputStream` would share-delete and slip past (2). Keep both.
3. **Count via MXBean (Linux + macOS, replaces `/proc` parsing).**
   `com.sun.management.UnixOperatingSystemMXBean.getOpenFileDescriptorCount()` gives
   `UntrustedFileHardeningTest.sniffingCascadeDoesNotLeakFileDescriptors` its 512-open
   delta on both Unixes with no `/proc` listing and **no `lsof`** — so #265 Stage 8
   ceases to be a stage and becomes a line of shared helper code.
4. **`/proc` symlink identity (Linux, optional extra precision).** Keep it, but as an
   *additional* assertion inside an already-meaningful test, never as the gate.

Concretely: one new `test/jls/FileHandleObservation.java` helper exposing
`assertOpenTookASnapshot(Path, Scanner)` and `assertNoHandleRetained(Path)`, consumed
by both existing test classes. No `assumeTrue` at the top of any method; only step
3/4 is conditional, and no test is ever vacuous anywhere. That satisfies invariant 4
far better than the narrow documented skip the issue is prepared to settle for.

## A second, deeper cut worth considering

The probes exist only because `openCircuit` returns a `Scanner` — an object whose
type permits it to be stream-backed. Change the return to `String` (or a small
`CircuitText` record) and the leak class becomes *unrepresentable at the API
boundary*: nothing that could hold a descriptor crosses it, and there is no
observation problem left to port to Windows. The cost is mechanical: seven production
call sites (`JLSStart` ×6, `FileFormatSupport` in tests), each of which immediately
feeds `Circuit.load(Scanner)`; all three readers already build their `Scanner` over
memory, so the drain already happened. What remains after the change is one *internal*
obligation — `readXZ`/`readZip`/`readText` close what they open during the sniffing
cascade — enforceable by the step-3 count probe plus an ArchUnit rule forbidding any
stream type from escaping `FileAbstractor`. ArchUnit is already a dependency and
`ArchitectureRulesTest` is already the home for exactly this kind of rule.

That also untangles a conflation the issue inherits. Two different invariants are
currently welded together by the word "handle": (i) *the returned value is
memory-backed* (a per-open contract, portable, assertion 1), and (ii) *the three-probe
sniffing cascade closes its own probes* (an internal resource discipline, a count or a
structural rule). They read as one problem only because both happened to be tested
through `/proc`. Splitting them is what makes Windows stop looking hard.

## Alignment with the larger arc

Redirected this way, the stage strengthens #111 rather than merely completing it: the
probes become real assertions on the advisory Windows lane *before* W1 promotion, so
W1's 20-run record accrues over a suite that is actually testing something on Windows.
It removes a stage from #265 instead of adding one. And it keeps the CI toolchain
inventory — which this project treats as a security surface, not a convenience —
exactly where it is. The issue as written would grow that inventory by a proprietary
binary to learn something `Files.delete` already reports.

## What I would keep from the issue as filed

The boundary is correct and I would not touch it: observation only, the `b9c787d`
drain must not regress. AC 3 (no broad platform mute) survives intact and is better
served. AC 4 survives with its answer inverted: record in #111 Open Question 2 that
the mechanism decision was *neither* Handle nor openfiles nor a proxy, but "the OS's
own delete semantics plus a snapshot assertion", with the reasoning above as its
evidence — and note that #265 Stage 8 should be closed as absorbed rather than
"reused or diverged from".
