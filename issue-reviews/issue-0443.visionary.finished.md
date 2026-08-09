# Issue #443: TASK-0027: all five installer legs become required checks, the bundled Wayland runtime is pinned by digest, and every OS asserts the .jls association README already claims
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the machine block and the three deliverables reduce to one sentence: *the README must not
make a promise no machine re-checks.* That is the same sentence behind `CliFlagTableTest`,
`HeadlessCoreRatchetTest`, `HotkeysHelpAccuracyTest`, `ArchitectureRulesTest` and the whole
`proofs/` habit. On that axis the issue is unambiguously with the project's arc, and I endorse
its intent. What I want to change is the *seam it cuts along*. It cuts along the CI matrix leg
(`experimental: true` → `false`). The promise lives one level up, on the published release, and
cutting there is cheaper, safer, and closes more of #338 than promotion does.

## Correction that reframes the risk model

The issue's central fear — H4, and §11's "converts a stalled download into a six-hour block on a
release" — does not survive reading the workflow's job graph. `release` publishes the GitHub
Release itself (`.github/workflows/release.yml:107-117`, `softprops/action-gh-release`, **not**
`draft`), and `installers:` runs `needs: release` (`:290`), *appending* assets to an
already-public release at `:653-657`. So:

- A required installer leg **cannot block a release**. The release is already live by the time
  the leg runs. Promotion converts a green run into a red run; it does not gate publication.
- Because `continue-on-error` is job-level, a failed smoke step still aborts that leg's remaining
  steps — sign, checksum, attest, upload. Today's actual user-visible failure is therefore **not**
  "a broken installer ships silently"; it is "the Releases page silently lacks the Windows msi",
  with a green checkmark next to it. The workflow already knows this and says so at `:672-678`
  ("a failed leg used to leave the run green with that installer silently absent"); #338 repeats
  it in Background. #443's Abstract and Intended-Audience section assert the opposite failure mode
  and build the whole justification on it.
- Promotion alone does not fix that harm either. A required leg that fails still leaves a
  published, incomplete release — now with a red run beside it. Users download from the Releases
  page, not from the Actions tab.

## Reframing 1 (primary): publish when complete, not before

Make the release **atomic** instead of making the legs required:

1. `release` publishes with `draft: true`.
2. `installers`, `maven-registry`, `container-image` attach their assets to the draft.
3. One terminal job — call it `publish-release` — asserts the asset set is complete against an
   in-tree manifest (every format × arch the README lists, each with a `SHA256SUMS-*` entry and an
   attestation), then flips draft → published.

This makes the user-facing sentence true *by construction*: if it is on the Releases page, every
installer the README names is there and was installed and launched on its own OS. It also
dissolves the issue's own worst risk — there is nothing to revert at 3am, because a stalled or
broken leg leaves a draft, not a half-release, and the maintainer publishes by hand or re-runs one
leg. `experimental` then decays into what it should always have been: a statement about a leg's
evidence quality, not a switch controlling whether users get a mutilated release.

Cost: a `draft: true`, one job, one manifest. Compare to §5's P3, whose evidence is a
workflow_dispatch dry run that by definition *does not publish anything* (`if: github.event_name
== 'push'` guards the upload, the attestation and the signing), i.e. the primary acceptance
evidence for promotion exercises everything except the path where the harm occurs.

Tension to argue at #338, not here: this inverts #338's invariant 3 ("a release is never
unpublished by a signing problem", `release.yml:663-665`). Draft-until-complete never unpublishes
anything — but it does move signing/verification *before* visibility, which is a deliberate policy
change and belongs in the parent's §4, not smuggled into a task.

## Reframing 2: one pins manifest, not two digits in one script

Arming `JBR_SHA256` at `scripts/build-installer.sh:298-299` is right and I endorse it, including
Open Question 2's "a mismatch is a hard failure" — a pin that falls back is not a pin. But the
task treats it as two literals, and the tree already shows why that is the wrong unit:

- `scripts/build-installer.sh:199-201` pins appimagetool per arch (armed).
- `scripts/build-installer.sh:298-299` pins JBR per arch (placeholder, **fail-closed** — falls back).
- `.github/workflows/ci.yml:380` pins the *same JBR* for the `gui-wayland` lane (placeholder,
  documented at `:367` as "an OPTIONAL hardening" and at `:430-440` **fail-open** — it downloads
  and runs the unverified archive anyway).

Two consumers of one artifact, one convention (#101's), opposite security semantics, three
places to edit on every JBR bump. Give the repo `resources/packaging/pins.yaml` — `{name, version,
url template, per-arch sha256, policy: require|warn, revisit}` — one shell/Java verify helper, and
one drift test asserting no entry is a placeholder and every fetch site reads from the manifest.
That makes #338's IC-2 (`grep -rn "UNVERIFIED\|PLACEHOLDER"`) a property of one file, gives #101
and #285 the same edit instead of three, and turns the JBR bump from "remember both scripts" into
one row. It also gives the JBR pin an honest **expiry**: JBR is a workaround for mainline OpenJDK
lacking WLToolkit (README:148-160 says so). A `revisit:` field plus the existing monthly
`repro-installers.yml` probe makes "can we drop JBR yet?" a scheduled question rather than
folklore.

## Reframing 3: a claim ledger, so #111, #265 and #443 stop being the same issue three times

#443, #111 (Windows lanes), #265 (macOS lanes) and #338 IC-1 are one argument — "an advisory check
is not a check" — re-litigated per job by four issues, each proposing its own bespoke YAML-parsing
test (§6 here proposes `InstallerMatrixPolicyTest`). Build the general thing once:
`resources/claims.yaml` mapping each user-visible promise to the check that asserts it and its
status — README line → asset/leg/step → `asserted | advisory | unasserted (reason)`. One drift test
in the `CliFlagTableTest` idiom reads README, `release.yml`, `ci.yml` and the manifest, and fails
when a promise has no asserting check, when a check is advisory without a recorded reason, or when
an entry names a leg the matrix no longer has (that is §5's P4 anti-vacuity clause, for free and
repo-wide). #443's three deliverables then become three rows, not a bespoke test class, and
#111/#265 inherit the mechanism instead of rebuilding it.

The ledger would also immediately catch claims this task never looked at, which is the point:
README:31-36 states flatly that "The installers are Authenticode-signed through SignPath.io" while
the signing path is gated on `SIGNPATH_ENROLLED` (`release.yml:584`, `:595`) and skips silently
when the secrets are absent; README:92-95 claims a published `.buildinfo` reproducibility recipe.
The task is scrupulous about one unasserted README sentence and blind to its neighbours, because
its unit of work is a matrix leg rather than a claim.

## The association goal, done better than a registry query

§11 is right that "the registry key exists" is a weak assertion, and Open Question 4 correctly
refuses to guess the macOS query. Two further facts the issue does not carry:

- The Linux assertion it holds up as the model is **warn-only**. `release.yml:466-470` emits
  `::warning::` and passes when `xdg-mime query default` does not name JLS. So "asserted on Linux"
  is really "mime type, icon and `%f` field code asserted; default-handler binding best-effort."
  Promoting on the strength of that model propagates a soft check to three OSes.
- README already contradicts its own universal claim: README:22-24 says the AppImage association
  "comes along only if your desktop integrates AppImages". The honest target is the four
  *installer-managed* formats, not "every OS".

Concrete alternative needing no new product surface: assert the two halves whose conjunction *is*
the promise. (a) Resolve the handler from the OS and compare the **resolved executable path** to
the installed launcher (`xdg-mime`+`Exec=`, `HKCU:\Software\Classes\...\shell\open\command`,
`lsregister -dump` / `LSCopyDefaultRoleHandlerForContentType`); (b) run that resolved command on a
real `.jls` in a mode that touches no AWT — the launcher already accepts `-savetext out.jls
in.jls` (README:139-141), so a successful round-trip proves the binary the OS points at can
actually open the file. Together that is "the OS routes `.jls` to a JLS that can read it", which is
strictly stronger than any key-presence check and weaker than nothing only in the double-click GUI
step, which no headless runner can honestly assert anyway.

## What I am disregarding, and why

- **§14's "flip four flags in one commit" and §5's P1 as the deliverable.** Under Reframing 1 the
  flags stop being load-bearing, and #338 §6 already permits arming legs one at a time. Optimizing
  the promotion for cheap revert optimizes the wrong variable: the blast radius, not the rollback.
- **P8 (the per-OS artifact-size table).** It is a nice datum with no consumer and no failing
  condition — "a human noticing a size regression" (§7.6). Either make it a check with a bound, or
  drop it from a task whose thesis is that unchecked records rot.
- **The task's `blocked_by: []`.** Its own parent declares `blocked_by: [317]` *because* the
  installer legs need the platform lanes to be required, and #338's mermaid draws `N317 → T0027`
  explicitly. The child silently deletes an edge the parent asserts. Also unaddressed: #338 names
  **#284** ("installer verification on real runners, then retire the experimental flags") and
  **#285** ("arm the pinned JBR sha256s") as issues that "should be resolved as that task's issue
  rather than duplicated" — #443 is that task and cites neither. Filing a third issue over two
  existing ones is the duplication #338 warned about.

## Verdict

Endorse the destination — armed digest with hard failure, an association assertion that names the
resolved application, and no advisory check standing in for a real one. Reframe the route: publish
the release only when it is complete (which makes promotion a hygiene change rather than a risk),
put every fetched artifact's digest in one manifest with an expiry, and express the "no advisory
check" rule once as a claim ledger that #111, #265 and #338 can share, instead of a bespoke
matrix-parsing test that this task alone will own.
