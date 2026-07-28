## OpenSSF Best Practices Badge - passing, silver, gold (#287)

### What conformance actually means

**The document.** The *FLOSS Best Practices Criteria*, published and
operated by the OpenSSF Best Practices Working Group at
`https://www.bestpractices.dev` (formerly the CII Best Practices Badge).
The criteria are versioned as YAML in the badge application's own
repository — `criteria/criteria.yml` in
`github.com/coreinfrastructure/best-practices-badge` (the `ossf/`
org name redirects to the same repo) — and that file, not the rendered
HTML, is the authoritative text. Each criterion carries flags that
decide what an answer must supply: `met_url_required`,
`met_justification_required`, `na_allowed`, `na_justification_required`.

**The levels.** Three cumulative levels, called the "metal series":

| Level | Internal id | Criteria count | Prerequisite |
|---|---|---|---|
| passing | level 0 | 67 | — |
| silver | level 1 | 55 | `achieve_passing` |
| gold | level 2 | 23 | `achieve_silver` |

(Counts confirmed against the criteria.yml dump; the same counts are
quoted by third-party summaries. The site *also* now hosts a separate
**OpenSSF Baseline** series — `baseline-1/2/3`, badge image endpoint
`/projects/<id>/baseline` — which is a different criteria set and is
**not** what this item targets. Do not conflate them.)

**What a badge claim actually asserts.** The badge is **pure
self-certification**. No auditor, no registry review, no accredited
body. The artifact a claim rests on is *the project's own entry in the
BadgeApp database* — a per-criterion answer (`Met` / `Unmet` / `N/A`,
plus a URL and/or free-text justification for each) submitted by a
GitHub account that has proven control of the repository. The badge SVG
is a rendering of that entry. Anyone can read every answer at
`https://www.bestpractices.dev/projects/<id>` and check it against the
tree; the credibility of the claim is entirely the credibility of those
answers.

**What is being claimed and what is not.** Earning `passing` claims that
JLS follows a named set of open-source development practices — it does
**not** claim the software is secure, audited, fit for safety-critical
use, or conformant to any of the EDA/IEEE standards catalogued in
`docs/standards-landscape.md`. It sits in the same evidentiary tier as
the OpenSSF Scorecard badge already in `README.md:3`: a public,
machine-readable statement about process, backed by in-repo artifacts.

**The realistic target for JLS.**

- **passing** — achievable now. Every MUST is already satisfiable from
  the tree; the work is answering 67 questions honestly and writing two
  short docs. **Recommended.**
- **silver** — reachable on documentation alone *except for one hard
  numeric gate*: `test_statement_coverage80` (MUST, 80 % statement
  coverage). The JaCoCo bundle LINE floor in `pom.xml:366` is **0.535**,
  i.e. the real headless measurement is ~54 %. That is a multi-month
  test-writing program, not a badge task. **Recommended as a stated
  goal, not as this month's work.**
- **gold** — **structurally unreachable for a single-maintainer
  project, permanently, by design of the criteria.** Three gold MUSTs
  cannot be met by one person: `bus_factor` ("The project MUST have a
  'bus factor' of 2 or more"), `contributors_unassociated` ("The project
  MUST have at least two unassociated significant contributors"), and
  `two_person_review` ("at least 50 % of all proposed modifications
  reviewed before release by a person other than the author").
  `git log --format='%an'` on this repository shows exactly one human
  identity across three author strings (`Josh Marshall`, `Anadon`, and
  `Claude <noreply@anthropic.com>` — agent commits authored under the
  maintainer's direction, not an independent contributor) plus
  `dependabot[bot]`. Gold is not a matter of effort; it is a matter of
  headcount. **Do not pursue gold. Say so in writing rather than
  leaving it as an open aspiration.**

---

### Implementation procedure

#### Phase 0 — decide the scope and record it (0.25 day)

1. Add a short recorded decision to `ARCHITECTURE.md` (its "recorded
   scope decisions" section) or to a new `docs/openssf-badge.md`:
   *JLS targets `passing` now, treats `silver` as a standing goal gated
   on the #66/#159 coverage program, and will not pursue `gold` while
   the project has one maintainer, because `bus_factor`,
   `contributors_unassociated`, and `two_person_review` are headcount
   requirements.* This kills the recurring "why not gold?" question
   permanently and is the same move `docs/standards-landscape.md` **§13.3**
   ("Deliberately not recommended") makes for safety-tool qualification.

#### Phase 1 — register and answer the passing criteria (1 day)

2. **Register.** Sign in at `https://www.bestpractices.dev` with the
   GitHub account that owns `anadon/JLS` (OAuth; GitHub login is the
   path that lets the site verify repo control and pre-fill fields).
   Choose "Add project", enter the repo URL `https://github.com/anadon/JLS`.
   The site assigns a numeric project id. Record that id in
   `docs/openssf-badge.md` — every downstream URL is derived from it.
3. **Answer all 67 passing criteria** from the ledger below. The site
   autofills several from GitHub API data (license, repo public, HTTPS);
   review each autofill rather than accepting it.
4. **Create the two missing artifacts** the passing level needs:
   - `docs/openssf-badge.md` (**to be created**) — the in-repo mirror of
     the answers: one row per criterion, the answer, and the in-tree
     evidence path or the justification text. This file is the source of
     truth the maintainer edits; the web form is a copy of it. It is
     also what the drift test in the next section reads.
   - Nothing else. `SECURITY.md`, `CONTRIBUTING.md`,
     `CODE_OF_CONDUCT.md`, `CHANGELOG.md`, `ARCHITECTURE.md`,
     `docs/reproducibility.md`, and `pom.xml` already carry every
     passing-level evidence anchor.

**Passing-level ledger.** (`M` = MUST, `S` = SHOULD, `G` = SUGGESTED.
"Met" means answerable from the tree today with the cited path.)

*Basics*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `description_good` | M | Met | `README.md:5-11` |
| `interact` | M | Met | GitHub issues; `CONTRIBUTING.md` "Making changes" |
| `contribution` | M | Met | `CONTRIBUTING.md` |
| `contribution_requirements` | S | Met | `CONTRIBUTING.md` (style, tests, `mvn verify`, NullAway/sealed rules) |
| `floss_license` | M | Met | `LICENSE` (GPL-3.0), `pop_GPLv3.pdf` |
| `floss_license_osi` | G | Met | GPL-3.0 is OSI-approved |
| `license_location` | M | Met | `LICENSE` at repo root |
| `documentation_basics` | M | Met | `README.md`, `ARCHITECTURE.md`, in-jar help (`jls.Help`) |
| `documentation_interface` | M | Met | `docs/batch-interface.md`, `docs/file-format.md`, `jls -h` flag table (`jls.JLSStart.FLAGS`) |
| `sites_https` | M | Met | GitHub-hosted; no project-run site |
| `discussion` | M | Met | GitHub issues |
| `english` | S | Met | all docs English |
| `maintained` | M | Met | active commits, `CHANGELOG.md` `[Unreleased] — 5.0.5-SNAPSHOT` |

*Change control*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `repo_public` | M | Met | `github.com/anadon/JLS` |
| `repo_track` | M | Met | git |
| `repo_interim` | M | Met | branch-per-issue, merge PRs (`git log` shows `Merge pull request #266 from anadon/ci/265-macos-test-lane`) |
| `repo_distributed` | G | Met | git |
| `version_unique` | M | Met | `pom.xml` version; `v*` tags |
| `version_semver` | G | Met | `CHANGELOG.md:4-6` |
| `version_tags` | G | Met | release is "push a `v<version>` tag" (`.github/workflows/release.yml`) |
| `release_notes` | M | Met | `CHANGELOG.md` (Keep a Changelog) |
| `release_notes_vulns` | M | Met | no CVEs to date; the CHANGELOG convention covers it. Answer Met with that justification |

*Reporting*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `report_process` | M | Met | `CONTRIBUTING.md` "Reporting bugs" |
| `report_tracker` | S | Met | GitHub issues |
| `report_responses` | M | Met | maintainer asserts; issue history supports it |
| `enhancement_responses` | S | Met | same |
| `report_archive` | M | Met | GitHub issues are public and permanent |
| `vulnerability_report_process` | M | Met | `SECURITY.md` "Reporting a vulnerability" |
| `vulnerability_report_private` | M | Met | GitHub private security advisories, `SECURITY.md` |
| `vulnerability_report_response` | M | Met | `SECURITY.md`: acknowledgement within two weeks |

*Quality*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `build` | M | Met | `mvn verify`, `README.md` "Building from source" |
| `build_common_tools` | G | Met | Maven |
| `build_floss_tools` | S | Met | Maven + OpenJDK/Temurin |
| `test` | M | Met | 237 test files under `test/` |
| `test_invocation` | S | Met | `mvn verify` |
| `test_most` | G | **Unmet (honest)** | bundle LINE ~54 % (`pom.xml:366`). Answer Unmet; it is only SUGGESTED |
| `test_continuous_integration` | G | Met | `.github/workflows/ci.yml` on push/PR/nightly |
| `test_policy` | M | Met | `CONTRIBUTING.md`: "Every bug fix carries a regression test" |
| `tests_are_added` | M | Met | same |
| `tests_documented_added` | G | Met | `CONTRIBUTING.md` |
| `warnings` | M | Met | `pom.xml:172` `-Xlint:deprecation,removal,unchecked` |
| `warnings_fixed` | M | Met | `pom.xml:173` `-Werror` |
| `warnings_strict` | G | Met | `-Werror` + NullAway `pom.xml:185` + SpotBugs `pom.xml:628` |

*Security*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `know_secure_design` | M | Met | `SECURITY.md` threat model; `docs/collaborative-editing-research.md` §6 |
| `know_common_errors` | M | Met | CodeQL (`.github/workflows/codeql.yml`), SpotBugs, hostile-input taxonomy (issue #38) |
| `crypto_published` | M | Met | AES-256-GCM, SHA-256, HKDF, X25519, Ed25519 — `src/jls/collab/net/Crypto.java` |
| `crypto_call` | S | Met | JDK primitives only; `Crypto.java` docstring: "no crypto is hand-rolled" |
| `crypto_floss` | M | Met | OpenJDK JCE |
| `crypto_keylength` | M | Met | AES-256, X25519/Ed25519 (~128-bit security) |
| `crypto_working` | M | Met | no MD5/SHA-1/DES/RC4 anywhere in `src/jls/collab/net/` |
| `crypto_weaknesses` | S | Met | same |
| `crypto_pfs` | S | Met | X25519 ephemerals per session (`Handshake.java`) |
| `crypto_password_storage` | M | **N/A** | JLS stores no user passwords. Justify |
| `crypto_random` | M | Met | `src/jls/collab/net/Handshake.java:110` `SecureRandom`; `src/jls/elem/ElementId.java:48` |
| `delivery_mitm` | M | Met | HTTPS releases + `SHA256SUMS` + `gh attestation verify` (`README.md`, `SECURITY.md`) |
| `delivery_unsigned` | M | Met | same |
| `vulnerabilities_fixed_60_days` | M | Met | none reported; assert |
| `vulnerabilities_critical_fixed` | S | Met | same |
| `no_leaked_credentials` | M | Met | GitHub secret scanning on a public repo; release secrets are GH Actions secrets (`release.yml` SignPath step) |

*Analysis*

| Criterion | Req | JLS | Evidence / fix |
|---|---|---|---|
| `static_analysis` | M | Met | SpotBugs (threshold High) + NullAway, both in `mvn verify` |
| `static_analysis_common_vulnerabilities` | G | Met | CodeQL on every push/PR + weekly cron |
| `static_analysis_fixed` | M | Met | `mvn verify` fails on new findings; `CONTRIBUTING.md` bans blanket `config/spotbugs-exclude.xml` entries |
| `static_analysis_often` | G | Met | every push |
| `dynamic_analysis` | G | Met | `test/jls/GenerativeRoundTripFuzzTest.java`, `test/jls/ContainerMutationFuzzTest.java`, `riscv/fuzz_diff.py` |
| `dynamic_analysis_unsafe` | G | **N/A** | Java is memory-safe; no ASAN/valgrind equivalent applies |
| `dynamic_analysis_enable_assertions` | G | Met | Surefire enables assertions by default; `pom.xml:270,283` argLines do not disable them |
| `dynamic_analysis_fixed` | M | Met | fuzz suites are in `mvn verify` and gate merges |

5. **Embed the badge.** In `README.md`, put it on the line immediately
   after the existing Scorecard badge (`README.md:3`), so the two
   supply-chain claims sit together:

   ```markdown
   [![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/anadon/JLS/badge)](https://scorecard.dev/viewer/?uri=github.com/anadon/JLS)
   [![OpenSSF Best Practices](https://www.bestpractices.dev/projects/<ID>/badge)](https://www.bestpractices.dev/projects/<ID>)
   ```

   Note the image endpoint is CDN-served and rate-limit-friendly; the
   JSON form (`/projects/<ID>/badge.json`) is what the CI drift check
   below uses. Adding the badge also satisfies silver's
   `documentation_achievements` ("MUST identify and hyperlink to any
   achievements, including this best practices badge").

6. **Cheap wins to do at the same time** (they cost hours and move
   silver/gold criteria):
   - **Sign the `v*` tags** (`version_tags_signed`, SUGGESTED at
     silver). `git config gpg.format ssh` + `tag.gpgSign true`, publish
     the allowed-signers entry in `SECURITY.md`. This does **not**
     conflict with `SECURITY.md`'s no-project-GPG-key decision (#136):
     that decision is about *release artifact* signatures with custody
     and revocation burden, not about tag authorship. State the
     distinction in `SECURITY.md` so the two do not read as
     contradictory.
   - **Add a `good first issue` label** and a couple of labelled issues
     (`small_tasks`, gold MUST — pointless on its own, but it also
     improves the "somewhere to start" sentence at the end of
     `README.md`).

#### Phase 2 — silver (documentation half: ~4 days; coverage half: months)

7. **Create `docs/governance.md`** (**to be created**) covering three
   silver MUSTs at once, each `met_url_required`:
   - `governance` — decision model. Honest text for JLS: BDFL/single
     maintainer, decisions recorded as issues with acceptance criteria,
     normative changes require a documented deviation (the pattern
     `docs/batch-interface.md` and `docs/file-format.md` already use).
   - `roles_responsibilities` — maintainer, contributor, and the
     release role; what each may merge and what each must check.
   - `access_continuity` — "MUST be able to continue with minimal
     interruption if any one person dies, is incapacitated, or is
     otherwise unable...". A single maintainer **can** answer this
     credibly, but only with a real plan, not a sentence. The credible
     answer for JLS: (a) everything needed to build and release is
     in-repo and reproducible (`docs/reproducibility.md`,
     `scripts/build-installer.sh` is the single recipe used both
     locally and by CI); (b) there is **no** project-held long-lived
     signing key to lose (`SECURITY.md` §"Release artifact signing" —
     the keyless model is an access-continuity *asset*, and this is
     worth stating explicitly); (c) no project-controlled domain,
     server, or infrastructure exists to be orphaned; (d) a named
     second person holds GitHub repo admin. **(d) requires recruiting
     one human.** Without (d), answer Met on the strength of (a)-(c)
     plus GPL-3.0 fork-ability and say so plainly in the justification —
     it is a defensible but contestable answer, and the honest framing
     is "the project can be continued by anyone; it cannot be
     *continued by the same account*".

8. **Create `docs/assurance-case.md`** (**to be created**) —
   `assurance_case` is a silver MUST with `met_url_required` and is the
   single largest genuinely-new writing task. It is not a threat model
   restated; it is an argument structure: *claim → sub-claims →
   evidence*. JLS is unusually well placed here because the evidence
   already exists as enforced tests. Sketch:
   - **Top claim:** "JLS is adequately secure against its stated threat
     model: untrusted `.jls` files and untrusted collaboration peers."
   - **Sub-claim 1 — hostile file input cannot escape as an unclassified
     failure.** Evidence: the `LoadError` taxonomy (issue #58),
     `test/jls/CircuitLoadErrorTest.java`,
     `test/jls/LoadErrorReportingTest.java`,
     `test/jls/ContainerMutationFuzzTest.java`,
     `test/jls/GenerativeRoundTripFuzzTest.java`.
   - **Sub-claim 2 — the network attack surface is confined and opt-in.**
     Evidence: `SocketConfinementRatchetTest`, `ArchitectureRulesTest`,
     `test/jls/CollabSecurityRatchetTest.java`, the no-Java-serialization
     ban, listener-only-on-explicit-Share (`SECURITY.md` §"Listener
     hygiene").
   - **Sub-claim 3 — the session crypto is standard constructions used
     correctly.** Evidence: `src/jls/collab/net/Crypto.java`,
     `Handshake.java`, the tamper-every-byte property test in
     `test/jls/collab/net/HandshakeTest.java`.
   - **Sub-claim 4 — the shipped bytes are the reviewed source.**
     Evidence: `docs/reproducibility.md`, `project.build.outputTimestamp`
     (`pom.xml:47`), CycloneDX SBOM, Sigstore provenance, the
     `repro-installers.yml` probe.
   - **Sub-claim 5 — defects are caught before release.** Evidence:
     `-Werror` + NullAway + SpotBugs + CodeQL + the JaCoCo ratchet + the
     PIT mutation ratchet (`pitest` profile, `mutation.yml`).
   - **Residual risks, named:** GUI code (`jls.edit`) is deliberately
     unfloored for coverage; macOS artifacts are unsigned by decision
     (#128/#135); installers are not byte-reproducible.
   Two to three pages. Cite issue numbers and use RFC 2119 keywords per
   house docs style.

9. **Extend `SECURITY.md`** with a short "How reports are handled"
   subsection (triage → fix → advisory → credit → disclosure timeline)
   to make `vulnerability_response_process` a clean Met rather than an
   inference from the two-week acknowledgement sentence. This also
   moves landscape entry #197 (ISO/IEC 29147/30111) from COULD toward
   HAVE at near-zero cost.

10. **Answer the remaining silver criteria.** The ones that are *not*
    already Met from the tree:

| Criterion | Req | JLS today | Action |
|---|---|---|---|
| `test_statement_coverage80` | M | **FAIL** — ~54 % (`pom.xml:361-372`) | **Hard blocker.** The #66/#159 coverage program is the only path. `na_allowed` exists but an N/A here would be dishonest |
| `assurance_case` | M | Missing | write `docs/assurance-case.md` (step 8) |
| `governance` | M | Missing | `docs/governance.md` |
| `roles_responsibilities` | M | Missing | `docs/governance.md` |
| `access_continuity` | M | Missing | `docs/governance.md`; see caveat in step 7 |
| `documentation_roadmap` | M | Partial | issue #33 (tracking) + `docs/grand-architecture.md`. Needs one stable URL — link both from `docs/openssf-badge.md` |
| `documentation_achievements` | M | Partial | satisfied once the badge lands in `README.md` |
| `bus_factor` | S | **Unmet** | SHOULD — answer Unmet with the single-maintainer justification. Legal at silver |
| `dco` | S | Unmet | either adopt `Signed-off-by` in `CONTRIBUTING.md` or answer Unmet with justification. Recommend: **adopt it**, it is one CONTRIBUTING paragraph and a `git commit -s` habit |
| `crypto_algorithm_agility` | S | Unmet/unclear | `Crypto.java` pins exactly one suite deliberately. Answer Unmet with the justification that a single suite plus a versioned handshake is the chosen agility mechanism — **verify first** whether `Handshake` carries a protocol-version field; if it does not, adding one is the cheap fix and belongs to #168, not to this item |
| `crypto_credential_agility` | M | Met | identity keypair is generated per install at `jls/collab-identity`, replaceable; key-change is surfaced loudly (`SECURITY.md`) |
| `crypto_tls12` | S | **N/A** | JLS supports no TLS. Justify: SIGMA-shaped raw-public-key handshake, `SECURITY.md` §"Handshake" |
| `crypto_certificate_verification` | M | **N/A** | no X.509/CA anywhere; peer authentication is SAS + pinned known-peers |
| `crypto_verification_private` | M | **N/A** | same |
| `crypto_used_network` | S | Met | all session traffic is AEAD-encrypted per direction with counter nonces |
| `sites_password_security` | M | **N/A** | no project-operated site with user passwords |
| `build_standard_variables` | M | **N/A** | Maven project; `CC`/`CFLAGS` do not apply |
| `installation_standard_variables` | M | **N/A** | jpackage/native installers, not a POSIX `make install` with `DESTDIR`. Justify with `scripts/build-installer.sh` |
| `build_non_recursive` | M | Met | single-module `pom.xml` |
| `build_repeatable` | M | Met | `docs/reproducibility.md` |
| `build_preserve_debug` | S | Met | javac debug info on by default |
| `installation_common` | M | Met | deb/rpm/AppImage/msi/dmg + Nix flake (`README.md`) |
| `installation_development_quick` | M | Met | `mvn verify` from clean checkout, `.devcontainer/` |
| `external_dependencies` | M | Met | `pom.xml` dependencies block; optional dev tools listed in `README.md` |
| `dependency_monitoring` | M | Met | `.github/dependabot.yml` (maven, github-actions, docker), CycloneDX SBOM |
| `updateable_reused_components` | M | Met | no vendored sources (xz was de-vendored — see `pom.xml` dependency comment) |
| `interfaces_current` | S | Met | javadoc built with `-Werror` (`pom.xml:567`) |
| `automated_integration_testing` | M | Met | `ci.yml` on every push/PR across Linux/Windows/macOS |
| `regression_tests_added50` | M | Met | `CONTRIBUTING.md` "Every bug fix carries a regression test" |
| `test_policy_mandated` | M | Met | same |
| `tests_documented_added` | M | Met | same |
| `warnings_strict` | M | Met | `-Werror` |
| `coding_standards` | M | Met | `CONTRIBUTING.md` "Match the surrounding style", value-semantics (#94) and sealed-dispatch (#95) rules, `.editorconfig` |
| `coding_standards_enforced` | M | Met | Spotless (`pom.xml:658`), `-Werror`, NullAway, SpotBugs, `SealedHierarchyTest`, `NullMarkedRatchetTest`, `ArchitectureRulesTest` |
| `implement_secure_design` | M | Met | `SECURITY.md`, headless-core boundary, socket confinement, allowlisted vocabulary |
| `input_validation` | M | Met | `LoadError` taxonomy, frame caps, `ElementVocabulary` allowlist |
| `hardening` | S | **N/A**-ish | local desktop app, no server. Answer N/A with justification; note the jar is a single self-contained artifact with no runtime network |
| `signed_releases` | M | **Judgement call** | Sigstore keyless attestations + cosign + Authenticode via SignPath. The criterion's rationale assumes a published *public key*. Answer Met, justification = `SECURITY.md` §"Release artifact signing & verification": signatures are cryptographic and identity-bound via transparency log, deliberately keyless. Flag this in `docs/openssf-badge.md` as the one answer a reader might contest |
| `maintenance_or_update` | M | Met | `CHANGELOG.md` + the loader's acceptance of every historical container format (`README.md` "Circuit files") is literally an upgrade path |
| `report_tracker` | M | Met | GitHub issues |
| `vulnerability_report_credit` | M | Met | `SECURITY.md`: "credit is given unless you ask otherwise" |
| `vulnerability_response_process` | M | Partial | step 9 |
| `static_analysis_common_vulnerabilities` | M | Met | CodeQL |
| `dynamic_analysis_unsafe` | M | **N/A** | memory-safe language |
| `internationalization` | S | Unmet | the project **deliberately rejects i18n until a course asks**. Answer Unmet with that justification — SHOULD-unmet with a justification is a legal silver answer and is the honest one |
| `accessibility_best_practices` | S | Met | `docs/keyboard-a11y-verification.md`, `docs/component-naming.md`, `KeyPadAccessibilityPinTest`, `MenuAcceleratorPolicyTest` |
| `code_of_conduct` | M | Met | `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1) |
| `contribution_requirements` | M | Met | `CONTRIBUTING.md` |
| `documentation_architecture` | M | Met | `ARCHITECTURE.md` |
| `documentation_security` | M | Met | `SECURITY.md` + `docs/collaborative-editing-research.md` §6 |
| `documentation_quick_start` | M | Met | `README.md` "Installing JLS" / "Building from source" |
| `documentation_current` | M | Met | docs carry `file`/method anchors and are drift-tested (`FileFormatSpecTest`, `CliFlagTableTest`, `HotkeysHelpAccuracyTest`) |

#### Phase 3 — gold: do not attempt

11. Record the gold assessment in `docs/openssf-badge.md` and stop.
    For completeness, the 23 gold criteria against JLS:

| Criterion | JLS | Note |
|---|---|---|
| `achieve_silver` | Blocked | coverage gate |
| `bus_factor` (MUST ≥2) | **Blocked** | one maintainer |
| `contributors_unassociated` (MUST ≥2) | **Blocked** | one human identity in `git log` |
| `two_person_review` (MUST ≥50 %) | **Blocked** | self-merged PRs |
| `copyright_per_file` | Fail, fixable | 292 files under `src/`; **1** contains "Copyright", **0** contain `SPDX-License-Identifier` |
| `license_per_file` | Fail, fixable | same. A header sweep is mechanical but is exactly the repo-wide churn `CONTRIBUTING.md` warns against — if done, do it as one isolated commit with no other change, and say so in the commit message |
| `repo_distributed` | Met | git |
| `small_tasks` | Fail, fixable | add `good first issue` label + labelled issues |
| `require_2FA` / `secure_2FA` | Met | GitHub enforces 2FA; maintainer asserts non-SMS |
| `code_review_standards` | Fail, fixable | `CONTRIBUTING.md` describes contributor obligations, not reviewer checks. Would need a written review checklist |
| `build_reproducible` | Met (with caveat) | jar + `bom.json` are bit-reproducible (`docs/reproducibility.md`); installers deliberately are not (`README.md`) — disclose in the justification |
| `test_invocation` | Met | `mvn verify` |
| `test_continuous_integration` | Met | `ci.yml` |
| `test_statement_coverage90` | **Blocked** | ~54 % |
| `test_branch_coverage80` | **Blocked** | bundle BRANCH floor 0.505 (`pom.xml:371`) |
| `crypto_used_network` (MUST) | Met | AEAD session traffic |
| `crypto_tls12` (MUST) | N/A | no TLS |
| `hardened_site` | Met | GitHub serves the headers; JLS runs no site |
| `security_review` (MUST, within 5 years) | Fail, fixable | `docs/assurance-case.md` plus a dated review record would satisfy it |
| `hardening` (MUST) | N/A | desktop app |
| `dynamic_analysis` (MUST) | Met | the two fuzz suites + `riscv/fuzz_diff.py` |
| `dynamic_analysis_enable_assertions` (SHOULD) | Met | Surefire default |

    Note the shape of this table: **gold is 4 headcount/coverage
    blockers and ~5 hours of paperwork.** That asymmetry is the
    argument for writing the decision down once instead of revisiting
    it.

#### Stability-contract impact

**None.** Nothing in this item touches `docs/batch-interface.md`,
`docs/file-format.md`, the `.jls` format, the CLI grammar, or saved
files. No migration story is needed; no existing user or circuit file is
affected. The only source-tree changes proposed are new `docs/*.md`
files, a `README.md` badge line, a `SECURITY.md` subsection, one new
test class, and one CI lane. The single item that would touch every
source file — gold's `copyright_per_file`/`license_per_file` header
sweep — is explicitly **not** recommended, precisely because it is the
kind of repo-wide churn `CONTRIBUTING.md` forbids.

---

### Testing procedure

The badge is self-asserted, so "proving conformance" means two distinct
things: (a) proving the in-repo evidence the answers cite is real and
still there, and (b) proving the *published answers* have not drifted
from the tree. These need different mechanisms because the test suite
must stay offline (`mvn verify` runs with no network) while the drift
check inherently needs the network.

**1. `test/jls/BadgeEvidenceTest.java` (to be created) — offline ledger
integrity.** Follows the house drift-test pattern already used by
`test/jls/FileFormatSpecTest.java` (parses a normative `.md` and checks
its claims against reality) and `test/jls/CliFlagTableTest.java`. It
reads `docs/openssf-badge.md` and asserts:

  - every row's answer is one of `Met` / `Unmet` / `N/A`;
  - every criterion id in the ledger is in the known criterion-name set
    pinned in the test (a frozen list, in the style of
    `jls.elem.SaveTags` and `NullMarkedRatchetTest` — so a criteria-set
    change upstream is noticed as a red test rather than silently);
  - the ledger covers **all 67 passing criteria** (and, once silver is
    targeted, all 55 silver ones) — a missing row is a failure, so
    upstream additions cannot be quietly ignored;
  - every in-tree path cited as evidence **exists** (`Files.exists`);
    this is the check that goes red when someone deletes or renames
    `SECURITY.md`, `CODE_OF_CONDUCT.md`, `.github/dependabot.yml`,
    `docs/reproducibility.md`, `docs/assurance-case.md`, or
    `docs/governance.md`;
  - every `Met` row has non-empty evidence, and every `N/A` and every
    `Unmet` SHOULD/MUST row has a non-empty justification (mirrors the
    BadgeApp's own `met_url_required` / `na_justification_required`
    flags);
  - the `README.md` badge line is present and its numeric project id
    matches the id recorded in `docs/openssf-badge.md` (guards against a
    copy-paste of someone else's badge).

**2. Machine-checkable criterion assertions, in the same test.** A
handful of answers are claims about build configuration, not about a
file existing. Assert those directly so a config regression turns the
badge ledger red:

  - `warnings` / `warnings_fixed` / `warnings_strict`: parse `pom.xml`
    and assert `-Werror` and `-Xlint:deprecation,removal,unchecked` are
    still compiler args, and that the NullAway `-Xplugin:ErrorProne`
    arg is still on the default build (not only in the `errorprone`
    profile);
  - `build_reproducible`: assert `project.build.outputTimestamp` is set;
  - `static_analysis`: assert the SpotBugs plugin binds to a build phase
    and that `config/spotbugs-exclude.xml` exists;
  - `dependency_monitoring`: assert `.github/dependabot.yml` still lists
    the `maven`, `github-actions`, and `docker` ecosystems;
  - `test_statement_coverage80` (once silver is targeted): assert the
    ledger's recorded coverage figure is not *above* the JaCoCo bundle
    LINE floor in `pom.xml` — i.e. the ledger cannot claim more coverage
    than the ratchet enforces.

**Golden-file strategy.** No byte-exact golden file is appropriate here
— the ledger is prose. The house golden-file mechanism under
`test/resources/` is the wrong tool; the frozen *criterion-name set*
inside `BadgeEvidenceTest` plays the ratchet role instead, exactly as
`NullMarkedRatchetTest` and `HeadlessCoreRatchetTest` do for their
invariants.

**3. CI drift lane (network) — `.github/workflows/badge.yml`
(to be created), or a new job in `ci.yml`.** Runs weekly on cron and on
`workflow_dispatch` only — **never a required PR check**, following the
precedent set by `mutation.yml` (schedule/dispatch-only, but a red run
is a regression to fix, not noise). It:

  - fetches `https://www.bestpractices.dev/projects/<ID>/badge.json` and
    fails if `badge_level` is below the level recorded in
    `docs/openssf-badge.md` (the BadgeApp tracks `lost_passing_at`, so a
    badge genuinely can be lost when answers change);
  - fetches `https://www.bestpractices.dev/projects/<ID>.json` and
    diffs each `<criterion>_status` field against the ledger's answer
    column, failing on any mismatch — this is the check that catches the
    real failure mode, which is *the web entry and the repo disagreeing*
    after someone edits one and not the other;
  - respects the documented rate limit (≈1 request/second for non-badge
    endpoints; badge images are CDN-served and cheap) — two requests a
    week is far inside it;
  - is `continue-on-error: false` but tolerant of transport failure:
    a 5xx or timeout should skip, not fail, so an upstream outage does
    not produce a false red. Follow the skip-when-absent discipline
    already used for external tools in
    `test/jls/hdl/IverilogCompileTest.java:34` and
    `test/jls/hdl/GhdlCompileTest.java:35`
    (`Assumptions.assumeTrue(tool != null, ...)`).

**4. Property/fuzz opportunities.** None that are specific to this item.
The existing fuzz suites (`test/jls/GenerativeRoundTripFuzzTest.java`,
`test/jls/ContainerMutationFuzzTest.java`, `riscv/fuzz_diff.py`) are
*evidence for* the `dynamic_analysis` criteria rather than a testing
mechanism for the badge itself; the assurance case should cite them by
path.

**5. What regression turns the tests red.** Concretely:
`SECURITY.md`/`CODE_OF_CONDUCT.md`/`docs/governance.md`/
`docs/assurance-case.md` deleted or renamed; `-Werror` dropped from
`pom.xml`; the SpotBugs or NullAway binding removed from the default
build; `dependabot.yml` narrowed; the README badge id changed or the
badge line removed; a new upstream criterion appearing that the ledger
does not cover; the published badge level dropping; any published
per-criterion answer edited on the web without the matching ledger edit.

**6. A human step that automation cannot replace.** Once per release
(fold it into the existing release checklist alongside
`docs/wayland-desktop-checklist.md`'s once-per-release spot-check), the
maintainer re-reads the `Unmet`/`N/A` justifications — the three
judgement calls (`signed_releases`, `access_continuity`,
`internationalization`) are the rows most likely to go quietly stale.

---

### Certification / conformance procedure

**Who assesses it: nobody but you.** This is plain self-assertion. There
is no registry review, no accredited body, no regulator, no
conformity-assessment scheme, and no auditor. The BadgeApp README
describes itself as "a simple web application that lets projects
self-certify that they meet the criteria and show a badge." The only
external gate is proof of repository control via GitHub OAuth. Whatever
credibility the badge carries comes from the fact that every answer is
public and checkable against a public repository — which is exactly why
the in-repo ledger and the drift lane above matter more than the badge
graphic does.

**Submission steps.**

1. Sign in at `https://www.bestpractices.dev` with the GitHub account
   owning `anadon/JLS`.
2. "Add project" → repository URL. A numeric project id is assigned;
   the entry is created at 0 % and is public immediately.
3. Fill in the 67 passing questions. Answers save incrementally; the
   entry shows a percentage until every MUST is `Met`/`N/A` and every
   SHOULD is `Met`/`N/A`/`Unmet-with-justification`.
4. At 100 % the `passing` badge is issued instantly — no review queue,
   no waiting period.
5. Silver and gold are additional tabs on the same entry; each requires
   the level below to be at 100 % first.
6. Paste the badge markdown into `README.md` (step 5 of Phase 1).

**Cost.** **Free.** No application fee, no membership requirement, no
OpenSSF or Linux Foundation membership needed. (Verified in substance
from the program description — "voluntarily self-certifying at no cost";
I have found no fee schedule anywhere and am confident there is none,
but treat the absence of a fee as *asserted, not contractually
verified*.)

**Elapsed time.** Registration to `passing` badge: **one working
session**, bounded by how long it takes to write 67 honest answers, not
by any external process. There is no assessment latency at all.

**Evidence package.** There is no package to submit. The "evidence" is
the URL and free-text justification typed into each criterion's field.
Nothing is uploaded; nothing is archived by the program on your behalf.
This is precisely why `docs/openssf-badge.md` should exist: **the badge
program keeps no copy of your reasoning that you control.** If the
entry is lost or the account is lost, the reasoning is gone unless it is
in the repository.

**Validity period and renewal.** There is **no expiry and no renewal
fee**. Reading the BadgeApp's own `projects_to_remind` logic
(`app/models/project.rb`): reminder emails go only to entries that are
**below** 100 % passing, that have not been updated in >30 days
(`LAST_UPDATED_REMINDER`, default 30), that have not been reminded in
the last 60 days (`LAST_SENT_REMINDER`), whose owner has notification
emails enabled — capped at `BADGEAPP_MAX_REMINDERS` (default 2) per run.
**A completed badge is therefore never nagged.** Entries also carry
`lost_passing_at` / `lost_baseline_1_at` columns, so the app does model
badge *loss* — which happens when the project's own answers change, not
by lapse of time.

**Maintenance burden, honestly.** Two things drive it:
  - **Upstream criteria change.** The criteria set is versioned and does
    change (the OpenSSF Baseline series was added to the same app
    recently). New or reworded criteria can move an entry off 100 %.
    Budget **~0.5 day/year** to re-read the diff of `criteria/criteria.yml`
    and re-answer.
  - **Your own project changing.** Deleting `SECURITY.md`, dropping
    `-Werror`, or letting CI rot invalidates answers. The
    `BadgeEvidenceTest` + weekly drift lane above is the mechanism that
    converts this from "trust the maintainer to remember" into "the
    build tells you."

**What invalidates it.** Only three things: the maintainer changing an
answer to `Unmet` on a MUST; the upstream criteria set adding a MUST the
project does not meet; or the entry being deleted. Nothing external
revokes it. Because it is self-asserted, the real risk is not revocation
— it is **the badge staying green while the underlying practice
regresses**, i.e. the badge becoming a lie. That is the risk the testing
section exists to close.

**What a credible self-assertion looks like for JLS.** (a) Every `Met`
answer cites a URL that resolves to a specific file or line in the
repository, not to the repository root. (b) Every `N/A` carries a
one-sentence *reason*, not "n/a". (c) The three contestable answers —
`signed_releases` (keyless, no published public key),
`access_continuity` (one maintainer), `internationalization` (deliberate
refusal) — are answered with the project's actual reasoning and a link
to the recorded decision, so a sceptical reader sees a decision rather
than a dodge. (d) The reasoning lives in the repository under version
control, not only in a web form.

---

### Effort, risk, and failure modes

**Sizing.**

| Scope | Maintainer-days | Reasoning |
|---|---|---|
| Passing badge, registered and displayed | **1.5** | 67 answers ≈ 0.75 day (most are one lookup each and the tree already has the evidence); `docs/openssf-badge.md` ≈ 0.5 day; README/registration/decision record ≈ 0.25 day |
| `BadgeEvidenceTest` + weekly drift lane | **1.0** | one test class in an established house pattern, one small workflow modelled on `scorecard.yml`/`mutation.yml` |
| Silver, documentation half only | **3.5** | `docs/assurance-case.md` ≈ 2 days (it is a real argument, and the residual-risk section takes thought); `docs/governance.md` ≈ 0.75; `SECURITY.md` response-process ≈ 0.25; 55 answers + N/A justifications ≈ 0.5 |
| Silver, coverage half (`test_statement_coverage80`) | **not days — a program** | 54 % → 80 % bundle LINE. `jls.edit` is deliberately unfloored pending the #84/#91 editor decomposition, and it is a large share of the ~69k lines. This is the #66/#159 work, sequenced behind an architectural change |
| Gold | **do not attempt** | 4 blockers no amount of effort clears |

**Headline: 3-5 maintainer-days delivers a displayed `passing` badge
plus the automation that keeps it honest.** That is the recommended
scope. Silver's documentation is another ~3.5 days and can be written
*now* — the docs are worth having regardless of the badge — but the
silver badge itself does not issue until coverage clears 80 %.

**Top three ways this goes wrong.**

1. **The badge becomes a lie.** The single largest risk. The entry hits
   100 %, the badge goes in the README, and eighteen months later CI has
   drifted, a doc has moved, and nobody re-answers. A green badge over a
   regressed practice is worse than no badge, because it is a public
   claim. *Mitigation:* the offline `BadgeEvidenceTest` (fails the
   build) plus the weekly network drift lane (fails visibly). Do not
   ship the badge without both.
2. **Answer inflation to reach 100 %.** The pull toward answering `Met`
   or `N/A` on the marginal criteria — `test_most`, `signed_releases`,
   `access_continuity`, `hardening`, and above all
   `test_statement_coverage80` (which is `na_allowed`, and an N/A there
   would be straightforwardly false) — is real, because 99 % shows no
   badge. *Mitigation:* the ledger's rule that a coverage claim may not
   exceed the enforced JaCoCo floor, and a standing instruction in
   `docs/openssf-badge.md` that `Unmet` on a SHOULD is a legal,
   acceptable answer. Sit at silver-99 % honestly rather than take
   silver dishonestly.
3. **Scope creep into the criteria.** Reading the gold list produces an
   urge to add SPDX headers to 292 files, adopt a DCO bot, spin up a
   review-standards document, and recruit a co-maintainer — none of
   which was the reason for doing this, and the header sweep of which
   directly violates `CONTRIBUTING.md`'s no-repo-wide-churn rule.
   *Mitigation:* the Phase 0 recorded decision. Gold is closed. Write it
   down and stop.

**Do NOT do this if:**

- The intent is to *reach gold*. It cannot be reached with one
  maintainer; pursuing it means either recruiting a genuine second
  maintainer (a fine goal, but a different project) or fabricating
  answers.
- The intent is to reach silver *this quarter*. `test_statement_coverage80`
  is a hard numeric gate ~26 points above the current enforced floor,
  and it is gated behind editor decomposition (#84/#91). Attempting it
  as a badge task would mean either bad tests written for the counter or
  a false N/A.
- The automation in the testing section is not going to be built. A
  self-asserted badge with no drift detection is a claim the project
  cannot keep. In that case, do nothing — the existing Scorecard badge
  already covers the supply-chain half and is *externally* computed,
  which is a stronger claim than a stale self-assertion.
- It would be presented to institutional users as a security assurance.
  It is not one. If an institution needs an assurance artifact, the
  right item is the VPAT/ACR work (`docs/standards-landscape.md`
  **§13.2 item 1**, line 753 — the survey split §13 into §13.1/§13.2 in
  commit `9ab4797`; the badge itself is §13.2 item 3, line 760), not this
  badge.

---

### Sources

**Primary (external).**
- `criteria/criteria.yml`, `coreinfrastructure/best-practices-badge` —
  the authoritative criteria text, level assignment, and per-criterion
  `met_url_required` / `na_allowed` / `*_justification_required` flags.
  Retrieved 2026-07-28. Level counts 67/55/23 cross-checked against the
  same file and against third-party summaries.
- `docs/other.md`, same repository — verbatim requirement text for
  `bus_factor`, `contributors_unassociated`, `access_continuity`,
  `two_person_review`, `small_tasks`, `require_2FA`, `governance`,
  `roles_responsibilities`, `documentation_achievements`,
  `signed_releases`, `build_reproducible`, `copyright_per_file`,
  `license_per_file`, `hardened_site`, `security_review`,
  `code_review_standards`.
- `docs/api.md`, same repository — the read API: `GET /projects/:id.json`,
  `GET /projects/:id/badge` (SVG, CDN-served) and `.json`, the
  `badge_level` ∈ {`in_progress`,`passing`,`silver`,`gold`} and
  `tiered_percentage` (0-300) fields, and the ≈1 req/s guidance.
- `app/models/project.rb`, same repository — `projects_to_remind`:
  reminders only for entries below 100 % passing, >30 days stale
  (`LAST_UPDATED_REMINDER`), ≥60 days since last reminder
  (`LAST_SENT_REMINDER`), capped by `BADGEAPP_MAX_REMINDERS` (default 2);
  `lost_passing_at` / `lost_baseline_1_at` columns; **no expiry logic**.
- `README.md`, same repository — self-certification model, badge URL form.
- OpenSSF Baseline announcement (openssf.org blog, Feb 2026) — the
  separate `baseline-1/2/3` series and its `/projects/<id>/baseline`
  badge endpoint, noted only to keep it distinct from the metal series.

**Repository evidence (all verified in tree at HEAD).**
`README.md:3` (existing Scorecard badge line), `README.md` install /
build / circuit-file / documentation sections; `CONTRIBUTING.md`;
`SECURITY.md`; `CODE_OF_CONDUCT.md`; `CHANGELOG.md:1-6`;
`ARCHITECTURE.md`; `LICENSE`; `pop_GPLv3.pdf`; `.editorconfig`;
`.github/dependabot.yml`; `.github/workflows/{ci,codeql,scorecard,mutation,release,repro-installers}.yml`;
`pom.xml` — `:47` `project.build.outputTimestamp`, `:172-173`
`-Xlint`/`-Werror`, `:185` NullAway, `:263-296` Surefire executions,
`:348-372` JaCoCo bundle floors (INSTRUCTION 0.545 / LINE 0.535 /
BRANCH 0.505), `:567` javadoc `-Werror`, `:628` SpotBugs, `:658`
Spotless, `pitest` profile; `config/spotbugs-exclude.xml`;
`src/jls/collab/net/Crypto.java`; `src/jls/collab/net/Handshake.java:110`;
`src/jls/elem/ElementId.java:48`; `test/` (237 test files) including
`FileFormatSpecTest.java`, `CliFlagTableTest.java`,
`HeadlessCoreRatchetTest.java`, `NullMarkedRatchetTest.java`,
`ArchitectureRulesTest.java`, `CollabSecurityRatchetTest.java`,
`GenerativeRoundTripFuzzTest.java`, `ContainerMutationFuzzTest.java`,
`hdl/IverilogCompileTest.java:34`, `hdl/GhdlCompileTest.java:35`;
`docs/reproducibility.md`, `docs/keyboard-a11y-verification.md`,
`docs/component-naming.md`, `docs/grand-architecture.md`,
`docs/standards-landscape.md` (§1 HAVE table, §11.4 entries 186-198,
§12.h entry 287 line 661, §13.2 item 3 line 760); `scripts/build-installer.sh`;
`riscv/README.md:53` (`fuzz_diff.py`).

**To be created (do not cite as existing):**
`docs/openssf-badge.md`, `docs/governance.md`, `docs/assurance-case.md`,
`test/jls/BadgeEvidenceTest.java`, `.github/workflows/badge.yml`.

**Unverified / marked uncertain.**
- The **absence of any fee** is asserted from the program's own
  description, not from a published fee schedule. I could not reach
  `bestpractices.dev` directly from this environment (the outbound proxy
  returns 403 for that host), so all badge-app facts above come from the
  application's own source repository on `raw.githubusercontent.com`,
  which is the same code that runs the site.
- **Criteria counts (67/55/23)** are derived from the criteria.yml dump
  and corroborated by a third-party summary; they will drift as the
  criteria set is revised.
- Whether `jls.collab.net.Handshake` carries a **protocol-version field**
  was not checked; it determines the honest answer to
  `crypto_algorithm_agility` (silver SHOULD). Check before answering.
- **Exact current coverage percentages.** The floors in `pom.xml` are
  facts; the live measurement is by construction slightly above them.
  Take the number from a fresh `mvn clean verify` on JDK 25, headless,
  before writing it into the ledger — per `CONTRIBUTING.md`'s coverage
  conventions, an unclean rerun unions with prior coverage.
- Whether GitHub's issue labels currently include `good first issue`
  could not be checked offline; relevant only to the gold `small_tasks`
  criterion, which is not being pursued.
