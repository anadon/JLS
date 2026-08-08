# Issue #672: TASK-C265-6: the /proc-gated probes get an lsof-based macOS observation, or a documented skip that names both test files
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

Stage 8 of feature #265: `FileHandleReleaseTest` and `UntrustedFileHardeningTest` currently detect leaked file descriptors by listing `/proc/self/fd`, which doesn't exist on macOS, so both fall back to `assumeTrue(...)` and silently skip there. The task offers two accepted outcomes: give the probes an `lsof`-based observation on macOS, or replace the silent skip with a narrow, documented one naming both files and the reason. Confirmed against the live workflow: the `macos` job (`.github/workflows/ci.yml:259-297`) already runs the full `mvn -B verify -Djacoco.skip=true` suite headlessly, so these two test classes do execute on macOS today and do silently no-op on every one of their `/proc`-gated methods — the premise is accurate, not invented.

## Findings, most severe first

**1. (High) The acceptance criteria let an implementer always take the "give up" branch and still pass every checkbox.**
Both AC-1 and AC-2 are phrased as an either/or with no bar for how hard the `lsof` path must be tried before falling back:
> "Either the probes assert on an `lsof`-based observation on macOS, or ... a narrow documented skip..."
> "A deliberately reintroduced handle leak is caught on macOS by the chosen path, or the change states plainly which regression class is now unobserved there."
The issue body's own hedge — "if that proves unreliable on hosted runners" — sets no measurable threshold (how many flaky runs? what error signature counts as "unreliable"?). As written, a contributor can attempt `lsof -p $$` once, hit a runner-permission quirk, and immediately write the documented-skip branch, closing all four checkboxes without ever landing the macOS observation Stage 8 exists to produce. Recommend the AC require concrete evidence of the attempt (a run log or a note of the specific failure mode) before the skip branch is accepted, mirroring the run-log evidence bar TASK-C265-2/#668 used for the HDL toolchain arming.

**2. (High) "Names FileHandleReleaseTest and UntrustedFileHardeningTest" risks a class-wide skip that silently disables unrelated security tests.**
`UntrustedFileHardeningTest` has three `@Test` methods; only `sniffingCascadeDoesNotLeakFileDescriptors` (`test/jls/UntrustedFileHardeningTest.java:67-86`) touches `/proc`. The other two — `oversizedZipEntryIsRejected` (lines 30-48, the zip-bomb size-limit rejection) and `rleIsBoundedByDeclaredCapacity` (lines 51-64, the RLE-capacity hardening) — have nothing to do with file-descriptor observation and already run fine on macOS. The class's own doc comment cites "live malicious-attachment campaigns against this repository" (line 21) via SECURITY.md — these are exactly the tests you don't want an over-broad skip to touch. If the documented-skip branch is taken and an implementer reads "a skip names ... `UntrustedFileHardeningTest`" as license to skip the whole class (rather than the one gated method), two hardening tests for a documented live threat go dark on macOS as a side effect of a task that was only supposed to be about handle-release observation. This also sits in tension with invariant 4, which the issue itself cites: "every assumption is per-test with a one-line justification (#265 invariant 4)" — a class-level skip is precisely the "broad" pattern invariant 4 forbids. Recommend the issue explicitly say "the one `/proc`-gated method in each file" rather than naming the files.

**3. (Medium) `ordering_after: []` ("independent of every other stage") is already contradicted by the parent issue's own newest comment.**
The machine block claims:
> `ordering_after: []   # independent of every other stage`
But #265's most recent comment (posted 2026-08-08T17:20:50Z, the same day as this review and four days after #672 was filed) states the opposite for this exact task:
> "Ordering, recorded on both endpoints: #406 precedes #671, #672 and #673 — a platform's extra legs, probes and GUI-boot rig are only meaningful once `Build (macOS, JDK 25)` blocks."
#672 has zero comments of its own (confirmed via `issue_read get_comments`), so nothing on this issue reflects the update — a contributor who reads only #672, as this review's own assignment scopes reviewers to do, will see "independent of every other stage" and start immediately, unaware the parent tracker now wants #406 landed first. Per this repo's own convention (see #265 comment id 5227056475: "bodies are never edited... recorded by comment"), a REPLAN comment on #672 itself is the fix, not silence.

**4. (Medium) "Recorded so TASK-C111-5 can reuse or diverge" specifies no location.**
> "The mechanism and its evidence are recorded so TASK-C111-5 can reuse or diverge from the choice knowingly."
TASK-C111-5 is real and open (#665, confirmed via search) — the cross-reference isn't fabricated — but the AC doesn't say where "recorded" means: a PR description, a `docs/` note, or an issue comment on #672/#265/#665. Given #265's Stage-2 taxonomy note went through exactly this ambiguity before being nailed down as "a note in `docs/` or as a `ci.yml` job comment" (per #265 comment id 5227247068), #672 should say the same rather than leave the next implementer to guess, especially since the whole point is that #665 needs to find it later.

**5. (Low) No feasibility check that hosted macOS runners can actually run unprivileged `lsof -p <pid>` the way the design implies.**
`lsof` is a stock macOS binary (unlike the Homebrew-installed `iverilog`/`ghdl`/`yosys` in the same job), so this is a low risk, but the issue doesn't ask for it to be confirmed against a GitHub-hosted `macos-latest` runner specifically before committing to the observation path — recent macOS versions have tightened `lsof`'s ability to see other processes' fds, though a process listing its own is normally unrestricted. Worth a one-line confirmation in the eventual PR rather than assuming parity with local dev machines.

## What's solid

- The premise is verified, not assumed: the macOS lane genuinely runs both test classes today and genuinely no-ops their `/proc` assertions (`FileHandleReleaseTest.java:44`, `UntrustedFileHardeningTest.java:75`) — exactly the citations #265 invariant 4 uses.
- The Boundary section correctly fences scope: "Observation mechanism only; the shipped fix (containers draining reads before returning) must not regress" keeps this task from ballooning into a re-litigation of the #111 handle-release fix itself.
- AC-3 (no broad `assumeTrue(!isMac)`) is consistent with the existing code's style — both files already gate on `Files.isDirectory("/proc/self/fd")` rather than OS name, so this criterion asks for continuity, not a new pattern.
- The task's place in the #265 decomposition is real and current: #265's 2026-08-08 comment explicitly reconfirms #672/TASK-C265-6 as still open, in-scope, and not duplicated by #406 — this is not a stale or orphaned ask.

## Verdict rationale

The task is technically grounded (accurate premise, feasible mechanism, correctly bounded scope) but the acceptance criteria as written can be satisfied without delivering the actual goal, and the file-naming in AC-1 creates a real risk of an over-broad skip touching unrelated hostile-input hardening tests. The ordering claim also needs a REPLAN comment before work starts. None of this makes the task unsound, but none of it should be waved through as-is either.
