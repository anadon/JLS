# Legacy JLS 4.1 corpus — provenance & status

This directory is the designated home for an **authentic JLS 4.1 circuit corpus**
(real `.jls` files produced by the original David A. Poplawski / Michigan Tech
JLS 4.1, not this fork's writer output). It is currently **empty** — see
issue [#56](https://github.com/anadon/JLS/issues/56) for the full thread.

## Status: deferred, non-blocking

Per the maintainer-approved disposition of #56 (documented in
`ISSUE-AMBIGUITIES-2026-07.md` §8 and `ISSUE-PROGRESS-2026-07-19.md`, #56):

- The authentic-4.1-corpus requirement is **downgraded to a non-blocking future
  hardening task**. It does not gate any current work.
- All consumers that were originally expected to depend on this corpus
  (#57, #38, #79, #60, #61) have already shipped using **synthetic fixtures**
  instead (see `test/fixtures/fork-4.6-shiftregister.jls` and the golden/CLI/
  print tests under `test/jls/`). The corpus is confirmed to no longer be an
  upstream blocker for any of them.
- The authentic MTU 4.1 `JLS.jar` was **not downloaded** for this fixture set:
  `pages.mtu.edu` is network-blocked from this environment's egress, and no
  synthetic file is being fabricated and mislabeled as authentic legacy
  output — that would defeat the point of the corpus.
- The **written fallback source**, should real legacy circuits ever need to be
  sourced, is the **GVSU (Grand Valley State University) / Zach Kurmas
  `JLSCircuitTester` course materials** — recorded here as the explicit
  fallback the issue previously lacked.
- Acquiring, licensing, and/or authoring real 4.1-era circuits (across all
  container formats, including an `orient 0` Display) under this directory —
  with proper provenance and license notes — is **deferred to the maintainer**,
  to be done with normal (unblocked) egress rather than from this sandboxed
  environment.

## Scope note

This resolves only the **corpus** item of #56. #56 also carries an
**editor-driven printing residual** (interactive, editor-invoked print flow,
as distinct from the existing headless print coverage in
`test/jls/PrintPathSmokeTest.java` and `test/jls/PrintPageOrderTest.java`)
that is **not** addressed by this note. #56 should **not** be considered fully
closed on the strength of this document alone.
