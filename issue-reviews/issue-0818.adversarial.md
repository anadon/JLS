# Issue #818: TASK-C184-1: the pinned container build runs twice at one commit and its JLS-controlled layer digests are compared — the claim that was never exercised
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Run the pinned container build (`APT_SNAPSHOT` + `SOURCE_DATE_EPOCH` + `rewrite-timestamp=true`, landed for #184) twice at one commit, diff the JLS-controlled layer digests, and record the result on #184. AC-1 recommends "two `workflow_dispatch` dry runs of `release.yml` that build without publishing" as the method.

## Findings, most severe first

### 1. (Critical) The recommended verification method structurally cannot exercise the mechanism it's supposed to validate

The very thing #184 flagged as needing validation is `rewrite-timestamp=true` on layer-tarball mtimes. Both the Dockerfile and `build-container.sh` say this in so many words:

- `resources/packaging/Dockerfile` (lines 29-32): "BuildKit clamps image-config and history timestamps to it (layer-tarball mtimes additionally need the push path's `rewrite-timestamp=true` output option)."
- `scripts/build-container.sh` (lines 75-95): the multi-arch `buildx build` only adds `--output type=image,push=true,rewrite-timestamp=true` when `PUSH=1` (line 82); otherwise "PLATFORMS without PUSH=1: build-only, results stay in the buildx cache" (line 84) — i.e. **no image is exported at all**, nothing to pull layer digests from.
- `.github/workflows/release.yml` line 213: `PUSH: ${{ github.event_name == 'push' && '1' || '0' }}` — a `workflow_dispatch` run is never a `push` event, so `PUSH` is always `0` on a dry run.

Consequence: AC-1's own recommended method — "two `workflow_dispatch` dry runs" — is incapable in principle of ever invoking `rewrite-timestamp=true`, because that flag only fires on the tag-push path, which `release.yml` explicitly reserves for real releases (pushing, signing, attesting are all gated `if: github.event_name == 'push'`, lines 204-245). There is no dry-run code path that both pushes an image and applies rewrite-timestamp. The only way to actually exercise what #184 needs validated is either (a) two real tag releases (disruptive: mints two real GHCR-published versions, two real GitHub Releases, two cosign signings — directly against AC-1's "build without publishing" framing), or (b) a local `docker buildx build ... PUSH=1` against a scratch registry outside CI (not what AC-1 recommends and not mentioned anywhere in the issue).

**Gameable failure mode**: a worker following the letter of AC-1 will run two dry runs, diff whatever they can get from the native single-arch build step (the only one that's `--load`-ed via plain `docker build`, per `release.yml` lines 187-190), declare a match under AC-3, and update docs to describe container reproducibility as demonstrated — while the actual shipped artifact (the multi-arch, pushed, `rewrite-timestamp`-clamped manifest) was never touched. AC-3's "described exactly as far as the evidence reaches and no further" is the right instinct but doesn't force the executor to notice the push/dry-run gap; nothing in the AC text requires stating explicitly that dry-run evidence does not cover the push path.

**Inverse failure mode**: even a good-faith attempt using only the native build step risks a false AC-4 trigger — two non-push builds may show differing layer digests purely because tar mtimes on apt-installed files were never clamped (no `rewrite-timestamp`), not because of any real non-determinism. AC-4's suggested causes ("apt cache state, `ldconfig` output, jlink ordering") don't include "the comparison never applied the flag the codebase says is required," so a worker could burn effort chasing a phantom residual, or file a spurious `tier:task` under #184.

**Recommendation**: rewrite AC-1 to either (a) require the comparison run against real pushed builds (two tag releases, or one real release plus one authorized local `PUSH=1` rebuild — which is what #184's own "Open Questions" section lists as the actual alternative, option (b), that #818's body dropped), or (b) explicitly scope the claim to "config/history timestamp determinism only, tarball-mtime determinism unverified" if dry runs are truly the only available method. As written, "recommended" steers straight at a method that cannot produce the evidence AC-2 asks for.

### 2. (High) No existing tooling emits the "digest table" AC-2 requires

`scripts/build-container.sh` never runs anything like `docker inspect --format '{{.RootFS.Layers}}'` or `docker history` (confirmed by grep — the only "digest" output in the script is the pushed manifest-list digest on the `PUSH=1` path, lines 92-94, a single digest, not a per-layer table). `release.yml`'s container-image job has no step that captures or prints layer digests either. AC-2 asks for "the full digest table," but producing one requires writing new inspection/logging code — either a new script step or manual `docker inspect` invocation against the runner — that this task doesn't scope as work. The issue treats this as "run it twice and compare" when it also requires adding instrumentation that doesn't exist, which is scope creep hidden inside what reads like a pure verification task.

**Recommendation**: add an explicit AC (or a note under AC-1) that building the digest-table extraction step is in scope, and specify where it lives (a new flag on `build-container.sh`, e.g. `--print-layers`, or a workflow step) so it isn't invented ad hoc and left uncommitted.

### 3. (Medium) Feasibility: does the executor have Actions-trigger and docker-daemon access?

#184's own history is explicit that "the implementing sandbox had no docker daemon" (comment 5013109788) and that P2 "could NOT be demonstrated in the sandbox." #818 assigns essentially the same kind of work to (presumably) the same kind of sandboxed agent, with no stated change in environment. Triggering `workflow_dispatch` on `anadon/JLS` requires write access to Actions on that specific repository; nothing in #818 confirms the assignee has it, and `ordering_after: []` / `ok` to proceed implies no blocking dependency was recognized even though "can this agent actually run a GH Actions workflow twice and read the run logs" is exactly the kind of prerequisite the previous attempt was blocked on. If the same constraint holds, this task is unactionable as scoped and should either name a human-triggered step explicitly or be blocked on confirming Actions access.

**Recommendation**: state explicitly who/what triggers the two dry runs (maintainer manually, or an agent with confirmed `actions: write`), rather than leaving it implicit.

### 4. (Low) `band_mw: "0.25-0.5"` cost estimate doesn't account for findings #1-#3

If this sizing assumes "click workflow_dispatch twice, diff two digests," it undercounts: writing new digest-extraction tooling (#2), resolving the push/no-push mismatch (#1) — which may require a design decision, not just execution — and confirming trigger access (#3) are all non-trivial. Not fatal, but the estimate likely needs revisiting once AC-1 is fixed.

## What's solid

- AC-2's "record ... in a comment on #184" is consistent with #184's own Completion Criteria checklist item ("command and output recorded in a closing comment") and with the boundary comment (5181646511) establishing #818 as #184's filed P2 slice — no contradiction there.
- AC-5's explicit "leave msi/dmg/installer boundaries untouched" correctly respects the #184/#188/#338 deduplication boundary recorded in #184's comments (installer reproducibility is owned by #188/#191, not this issue) — good scope discipline.
- AC-3/AC-4's "claim no more than the evidence shows, and don't touch docs until a rerun passes" framing is the right discipline in principle, matching `docs/reproducibility.md` §5's existing honest handling of the msi/dmg gap — it's just undermined in practice by finding #1.
- The technical grounding (APT_SNAPSHOT, SOURCE_DATE_EPOCH propagation, rewrite-timestamp=true, file/line citations implied) all checks out against the actual code at `resources/packaging/Dockerfile` and `scripts/build-container.sh`.

## Verdict rationale

`needs-rework`: the technical premise is sound and the issue is well-grounded in the real code, but AC-1's recommended verification method cannot produce evidence for the mechanism (`rewrite-timestamp`) it exists to validate, AC-2 assumes tooling that doesn't exist, and feasibility (Actions trigger + docker daemon access) is left unconfirmed given the prior attempt's documented failure on exactly that point. Fix AC-1 to name a method that actually reaches the push path (or explicitly narrow the claim), add the digest-extraction step to scope, and confirm executor access before this is ready to run.
