# Issue #665: TASK-C111-5: the file-handle probes stop being Linux-only knowledge — Windows observes handle release, or says in writing why it cannot
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is task W6 of feature #111 (Windows platform parity), giving
`FileHandleReleaseTest` and `UntrustedFileHardeningTest` — currently gated
on `/proc/self/fd` — a Windows observation path or a documented, narrow
skip. The task is well-anchored in real code (verified below) and the
cross-issue bookkeeping (#111, #265) is internally consistent. The
problems are in the acceptance criteria: one criterion overclaims what
the current CI configuration can actually guarantee, one is imprecise
about which tests are in scope, and the three suggested mechanisms carry
unexamined feasibility/licensing costs the issue never asks anyone to
weigh.

## Findings, most severe first

### 1. (High) AC #2's "cannot pass unnoticed either way" is false under the current CI configuration

The issue states: *"A deliberately reintroduced handle leak is caught on
Windows (if observation landed) or is demonstrably caught by the existing
`@TempDir` cleanup failure (if the skip route was taken) — the regression
cannot pass unnoticed either way."*

But `.github/workflows/ci.yml:156` currently sets `continue-on-error: true`
on the entire `windows` job (both JDK legs), and `#111`'s own status table
confirms W1 (promoting the headless Windows lane to a required check) is
still "Planned (unfiled)". A red Windows test today does not block a
merge and, being advisory, is easy for a reviewer to ignore in the PR
checks list. So a reintroduced leak *can* pass unnoticed on the path that
actually gates merges (the required Linux lane) even after this task
lands — "cannot pass unnoticed" is true only once W1 also lands, which
this task's `ordering_after: []` explicitly disclaims any dependency on.
**Recommendation:** either soften AC #2 to "is caught and reported by the
Windows lane" (drop the unnoticed-regression guarantee), or add an
explicit note that the guarantee is conditional on W1's promotion,
consistent with `ordering_after: []`.

### 2. (Medium) AC #1/#3 name `UntrustedFileHardeningTest` as if the whole class is `/proc`-gated, but only one of its three tests is

Verified in `test/jls/UntrustedFileHardeningTest.java`: only
`sniffingCascadeDoesNotLeakFileDescriptors` (line 74) gates on
`/proc/self/fd`. `oversizedZipEntryIsRejected` and
`rleIsBoundedByDeclaredCapacity` test size-limit and RLE-capacity
rejection and have nothing to do with file handles or `/proc`. The
issue's phrasing — "a narrow documented skip names
`FileHandleReleaseTest` and `UntrustedFileHardeningTest`" — reads as
class-level, which invites a class-level `@DisabledOnOs(WINDOWS)` (or
equivalent) that would also silently disable the unrelated
zip-bomb/RLE-capacity hardening tests on Windows. That is exactly the
"broad platform mute" AC #3 forbids, and it would satisfy a naive reading
of AC #1 ("names ... `UntrustedFileHardeningTest`") while quietly
regressing hardening coverage — a way the stated verification could pass
while the real goal (narrow, per-test skip) fails.
**Recommendation:** name the skip target as
`UntrustedFileHardeningTest.sniffingCascadeDoesNotLeakFileDescriptors`
explicitly, not the class.

### 3. (Medium-High) Feasibility and licensing costs of the two named mechanisms are unexamined

- **`openfiles`** requires "maintain objects list" enabled via
  `fsutil behavior set` and historically a reboot to take effect on the
  querying machine; this is awkward-to-infeasible to arrange reliably on
  ephemeral, single-use `windows-latest` hosted runners, and the issue
  gives no acknowledgement of this or a fallback if it doesn't work.
- **`Handle` (Sysinternals)** is Microsoft-owned proprietary freeware
  requiring EULA acceptance (`-accepteula`) and is fetched from
  `live.sysinternals.com` — an unpinned live download. The project has an
  established pattern for exactly this class of risk one job below in
  the same file: the Windows HDL toolchain step pins a specific
  `OSS_CAD_URL` and verifies `OSS_CAD_SHA256` before extracting
  (`ci.yml:164-224`). The issue sets no equivalent expectation (pinned
  URL/hash, licensing note) for `Handle.exe`, despite `#111` invariant 4's
  general "fixed at the source, never broad muting" discipline suggesting
  the project cares about this kind of rigor.
**Recommendation:** add an acceptance bullet requiring, if `Handle` is
chosen, a pinned download with hash verification (mirroring the
oss-cad-suite pattern) and an explicit one-line note of its licensing
terms; if `openfiles` is chosen, a bullet requiring confirmation it works
without a reboot on `windows-latest`, or documented rejection of that
option for that reason.

### 4. (Medium) "an fd-count proxy" is unspecified to the point of being gameable

No acceptance criterion defines what a passing fd-count proxy must
actually detect versus tolerate. A process-wide handle-count delta is
noisy (GC, JIT compilation, AWT/Swing internals can all move it) — a
naive implementation could be tuned against the one crafted
regression-leak test in AC #2 without being sound against real leaks
(e.g. thresholded so loosely it never fires, or so tightly it's flaky and
gets silenced later). This lets AC #2 pass while the actual goal (a
trustworthy Windows observation) fails.
**Recommendation:** require the fd-count proxy (if chosen) to isolate the
delta to file-type handles specifically (e.g. via
`GetProcessHandleCount`/handle-type filtering rather than raw process
handle count), or require a documented noise-floor/threshold rationale.

### 5. (Low) AC #4's "recorded" has no specified location

"The mechanism decision ... is recorded with its evidence" doesn't say
where (a `#111` comment, a code comment, `docs/`). Trivially satisfiable
by a throwaway line anywhere, which weakens its stated purpose of letting
`#265` Stage 8 "reuse or diverge from it knowingly."
**Recommendation:** specify the record's home (e.g., a comment on
`FileHandleReleaseTest`/`UntrustedFileHardeningTest` plus a `#111` status
comment, matching how the defect-phase fixes are documented there today).

## What checks out

- The `b9c787d` reference is accurate: `git show b9c787d` confirms it is
  "Windows portability: canonical newlines, no held file handles (#111)"
  and matches the described fix (drain-into-memory, `FileHandleReleaseTest`
  added). The Boundary section's "must not regress" framing is well-anchored.
- `ordering_after: []` ("independent of every other W stage") matches
  `#111`'s own Sequencing section verbatim ("W6 is independent and
  parallel to everything") — no contradiction there.
- The `/proc/self/fd` gating claim for `FileHandleReleaseTest` is accurate
  in full: all three of its tests route through `fdOpenOn`, which
  `assumeTrue`s on `/proc/self/fd` (line 44).
- Cross-references to `#111` invariant 4 and to `#265` Stage 8 (macOS
  `lsof` analogue) are both real and consistent with those issues' current
  bodies — no dangling or fabricated citation found.
- The scope is properly bounded: it explicitly excludes the underlying
  handle-release fix (already shipped) and the macOS analogue (`#265`),
  which keeps this task from creeping into either.
