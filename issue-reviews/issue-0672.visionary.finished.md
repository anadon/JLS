# Issue #672: TASK-C265-6: the /proc-gated probes get an lsof-based macOS observation, or a documented skip that names both test files
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is "an `lsof` path on macOS, or a documented skip." The
actual end it serves is one line from #265's invariant 4: *a platform-gated
assertion must not silently rot*. #672 is one of nine stages whose collective
point is that a regression cannot hide behind an OS boundary. Judged against
that arc, the issue is aiming at the right thing and framing it too narrowly in
three ways.

## Reframing 1: the choice is not "lsof or give up" — the JDK already ships the observation

Both probes want the same primitive: *how many descriptors does this process
hold?* On Linux they get it by listing `/proc/self/fd`
(`test/jls/FileHandleReleaseTest.java:42-57`,
`test/jls/UntrustedFileHardeningTest.java:74-85`). The JDK exposes exactly that
number portably, on Linux **and** macOS, with no subprocess and no parsing:

```java
((com.sun.management.UnixOperatingSystemMXBean)
    java.lang.management.ManagementFactory.getOperatingSystemMXBean())
        .getOpenFileDescriptorCount();
```

There is no `module-info.java` in this tree (classpath build), so
`jdk.management` is already in the module graph — this is an import, not an
infrastructure change. `UntrustedFileHardeningTest.sniffingCascadeDoesNotLeakFileDescriptors`
is *already* a count-delta test (`before`/`after` over 512 opens); swapping its
mechanism is a two-line edit that makes it assert on macOS unchanged.
`FileHandleReleaseTest` asks the strictly narrower question ("is an fd pointing
at *this file*"), but the leak class it guards is exactly one descriptor held by
the returned `Scanner` — a delta of zero across the open is the same regression
detector, and it is what the sibling test already trusts.

Against `lsof`: it is a fork+exec of a privileged-ish tool per observation,
sensitive to `/usr/sbin` on `PATH`, output-format-versioned, and slow enough
that a 512-iteration loop cannot use it per iteration. Choosing it commits both
this repo and #665 to parsing a text format forever. The MXBean has none of
that and costs one method call. **The issue's acceptance criteria offer a
binary that excludes the best option; I would not implement either branch as
written.**

## Reframing 2: the seam is a shared probe, not two files and a per-OS mechanism

The issue names two test files. There is a third: `BootListenerHygieneTest`
also reads `/proc/self/fd` and assume-skips off Linux
(`test/jls/BootListenerHygieneTest.java:197-215, 263-272`). Satisfying #672's
acceptance criteria word-for-word leaves that probe rotting on macOS while the
DoD box is ticked — the precise failure mode #265 invariant 4 exists to
prevent. Enumerating file names in acceptance criteria is what made that
possible; a shared seam makes the enumeration unnecessary.

Concretely: one package-private `HandleProbe` in `test/jls/` (alongside the
existing `FileFormatSupport`, `CircuitTextBuilder`, `SizeMeasurement` support
classes) with

- `static OptionalInt openDescriptorCount()` — `/proc/self/fd` where present,
  `UnixOperatingSystemMXBean` otherwise, empty on Windows;
- `static String unavailabilityReason()` — the one documented sentence, written
  once, quoted by every `assumeTrue` message;

and all three probes call it. The "documented narrow skip" then exists in one
place instead of three drifting copies, and the `ci.yml` Linux-only
justification (#265 invariant 5) becomes one line naming the probe, not a list
of test classes that will go stale the next time a fourth `/proc` reader is
written.

## Reframing 3: #665 and #672 are the same decision, filed twice

#665 (TASK-C111-5) is byte-for-byte the same task with "Windows / `Handle`,
`openfiles`, or an fd-count proxy" substituted for "macOS / lsof", and each
issue's fourth criterion is "record the choice so the *other* one can reuse or
diverge from it knowingly." Two tasks, two burn-ins, two records, one decision.
Under the shared-probe framing the ordering collapses: land `HandleProbe` with
the Linux and Unix-MXBean backends (this issue, ~0.3 mW), and #665 reduces to
adding a Windows backend or documenting its absence behind the same single
reason string. Windows also has the honest fallback the MXBean cannot give:
`@TempDir` cleanup failure *is* the observation there, which #665 already
notices. I would say so explicitly in both issues rather than letting them cite
each other in a loop.

## The deeper move: make the leak unrepresentable, then observe as defense in depth

All three read paths in `FileAbstractor` already return a `Scanner` over
in-memory bytes (`src/jls/FileAbstractor.java:278, 312, 342` —
`ByteArrayInputStream` twice, a `String` once). The invariant "no OS resource
escapes `openCircuit`" is therefore *structurally* true today, and this project
has an established, platform-free way to pin structural truths: the ratchet
family (`NotificationRatchetTest`, `HeadlessCoreRatchetTest`,
`SocketConfinementRatchetTest` on source text; `ArchitectureRulesTest` on
bytecode via ArchUnit, already a dependency). A ratchet asserting that every
`Scanner` returned from `FileAbstractor` is constructed over a byte array or a
`String` — and that `SeekableFileInputStream`/`ZipFile`/`FileInputStream` never
appear in a `return` expression there — runs on every runner, every OS, with
zero platform gating and zero flakiness, and it fails at the line that
reintroduces the defect rather than at a descriptor count three call frames
away.

Stronger still, and a real option rather than a rhetorical one: change
`openCircuit` to return the circuit text (a `String` or a small
`CircuitText` record) and let callers wrap it in `new Scanner(text)`. The leak
class then cannot be expressed by the signature at all, and the handle probes
demote from "the only guard" to "cheap corroboration on platforms where it is
free." That is the same instinct the project applied to the network surface —
confine the capability, then ratchet the confinement — rather than observing
misbehaviour after the fact.

If that refactor is out of band here, the ratchet alone is in band: it is a
smaller change than an `lsof` parser and it covers Windows, macOS, RISC-V, and
whatever runner comes next, for free.

## What I would keep from the issue as written

- The negative-control criterion ("a deliberately reintroduced leak is caught")
  is the strongest line in the issue and should survive any reframing. The
  project already has the idiom and the doctrine for it —
  `test/jls/ui/package-info.java`'s assert-the-assertion rule. Implement it as
  a real `assertThrows(AssertionError.class, ...)` case over a stub that holds a
  descriptor, not as a one-time manual experiment recorded in a comment; a
  manual check rots exactly like the assumption it is meant to protect.
- The refusal of a broad `assumeTrue(!isMac)`. Non-negotiable, and the shared
  probe makes it structurally hard to violate.
- The boundary ("observation mechanism only; the shipped fix must not
  regress") — correct, and the `String`-return refactor above is the one
  proposal that crosses it, so it belongs in its own issue if adopted.

## Concrete counter-proposal

1. `HandleProbe` test-support class: `/proc` backend, `UnixOperatingSystemMXBean`
   backend, single `unavailabilityReason()`.
2. Migrate all **three** `/proc` readers onto it; macOS now asserts rather than
   skips, with no `lsof` and no subprocess.
3. Assert-the-assertion test pinning that the probe fails on a deliberately
   held descriptor.
4. A `FileAbstractor` return-shape ratchet in the `SocketConfinementRatchetTest`
   idiom, so the invariant is enforced on every platform independent of any
   probe.
5. Rewrite #665 as "add the Windows backend / document its absence," and drop
   the mutual "record so the other can diverge" clause from both.

Steps 1-3 satisfy every substantive goal of #672 at lower cost than the `lsof`
branch; step 4 is the piece that makes the platform question stop mattering.
