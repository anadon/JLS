## EU Cyber Resilience Act stance and supply-chain attestation upgrades (#187, #192, #197, #253, #255)

**Numbering note.** `#187`, `#192`, `#197`, `#253`, `#255` here are *registry entry
numbers from `docs/standards-landscape.md`* (§11.4 and §12.c), not GitHub issue
numbers. The repository's real issue numbers are already past 260 (`git log`:
"Merge pull request #266"), so these five identifiers collide with live issues and
must never be written as `#187` in a commit message or CHANGELOG line. Anything
landed from this section needs its own GitHub tracking issue; refer to the survey
entries as "landscape entry 187" in prose. This section also picks up landscape
entry **#171** (SPDX identifiers / REUSE), which the brief scopes in explicitly and
which §13.2 item 5 of the survey bundles with #187.

**Environment caveat for everything below.** The agent proxy in this working
environment returns HTTP 403 for `eur-lex.europa.eu`, `digital-strategy.ec.europa.eu`,
`policy.openssf.org`, and `en.wikipedia.org` (confirmed:
`curl "$HTTPS_PROXY/__agentproxy/status"` lists `eur-lex.europa.eu:443` under
`recentRelayFailures` with "gateway answered 403 to CONNECT"). **I have not read the
primary text of Regulation (EU) 2024/2847.** Every article number, recital number,
and date below came from secondary sources and is marked accordingly. Do not paste
the draft stance into `SECURITY.md` until someone has opened the OJ text in a browser
and checked the four citations flagged **[SECONDARY]**.

---

### What conformance actually means

This item is two unrelated kinds of claim wearing one label, and conflating them is
the main way it goes wrong.

#### (a) The CRA stance is a statement of *applicability*, not a conformity claim

Regulation (EU) 2024/2847 (the Cyber Resilience Act) regulates **products with
digital elements made available on the Union market in the course of a commercial
activity**. It is a New Legislative Framework instrument: in-scope products get CE
marking, an EU declaration of conformity, technical documentation, and — for the
higher categories — a notified body. There is **no such thing as "CRA certification"
for a project that is out of scope**. The only artifact a project like JLS can
produce is a *dated, reasoned, published position on why the obligations do not
currently attach to it*, plus the evidence that would make that position credible if
someone asked.

The three roles that matter, and where JLS sits:

| Role | CRA source **[SECONDARY]** | Test | JLS |
|---|---|---|---|
| **Manufacturer** | Art. 3(13); obligations in Art. 13 and Annex I | Develops/places a product with digital elements on the Union market *in the course of a commercial activity* | Not one — see the monetisation facts below |
| **Open-source software steward** | Art. 3(14); obligations in Art. 24 | A **legal person** providing sustained support for FOSS *intended for commercial activities*, playing a main role in its viability | Not one — JLS has no legal person behind it at all; the maintainer is a natural person and there is no foundation, company, or fiscal host |
| **Contributor / individual developer** | Recitals 15–19 | Contributes to a project not under their responsibility, or supplies FOSS outside a commercial activity | This is the category JLS's contributors sit in |

The load-bearing legal text, as reported by secondary sources and **not verified
against the OJ**:

- **Recital 18 [SECONDARY]:** "the mere circumstances under which the product with
  digital elements has been developed, or how the development has been financed,
  should not be taken into account when determining the commercial or non-commercial
  nature of that activity", and "the provision of products with digital elements
  qualifying as free and open-source software that are **not monetised** by their
  manufacturers should not be considered to be a commercial activity."
- **Recital 19 [SECONDARY]:** stewards get a "light-touch and tailor-made regulatory
  regime" and **cannot affix CE marking** — i.e. even a steward is not making a
  conformity claim.
- **Article 24 [SECONDARY]:** a steward must (1) put in place and **document a
  cybersecurity policy** covering secure development, vulnerability handling,
  voluntary reporting, and community information sharing; (2) cooperate with market
  surveillance authorities and hand over that documentation in an easily understood
  language; (3) report actively exploited vulnerabilities and severe incidents where
  it is involved in the development or hosts the development infrastructure; (4)
  inform affected users.
- **Dates [SECONDARY]:** entry into force December 2024; **Article 14 reporting
  obligations apply from 11 September 2026**; the **main body applies from
  11 December 2027**. Today is 2026-07-28, so the first of those is **six weeks
  away** — which is exactly why a recorded stance now is cheap and a recorded stance
  in 2027 is late.

**What JLS's claim is, exactly:** *the maintainer does not monetise JLS and does not
place it on the Union market in the course of a commercial activity; no legal person
acts as an open-source software steward for it; therefore the manufacturer
obligations (Art. 13, Annex I) and the steward obligations (Art. 24) do not attach as
of the date of the statement.* That is a factual assertion plus a reading of the
regulation, and it must be labelled as such.

**What JLS's claim is NOT:** not "JLS is CRA compliant"; not a CE marking; not an EU
declaration of conformity; not legal advice; not a statement about any third party
who redistributes JLS commercially. That last exclusion is the one most likely to be
misread — **a company that bundles JLS into a commercial product becomes the
manufacturer of that product**, and JLS's stance says nothing about their
obligations. The stance paragraph must say so out loud.

**The artifact a claim of conformance rests on:** the dated stance section itself,
plus the facts it recites (no paid support, no paid hosting, no donation channel tied
to the software, no dual licensing, no paid installers, no commercial trademark
licence). Those facts are the claim. If one becomes false the stance is void, and the
stance must name that.

#### (b) The four artifact upgrades are self-asserted conformance to published specs

None of these has an accrediting body. Each is a claim about a named artifact:

| Landscape entry | Document, precise edition | The claim, precisely | The artifact it rests on |
|---|---|---|---|
| **#187** SPDX | **ISO/IEC 5962:2021 is SPDX 2.2.1** *(recalled, UNVERIFIED in this pass — confirm on the ISO catalogue before printing either number)*. SPDX 2.3 and SPDX 3.0 are *not* the ISO edition | "Every release publishes an SPDX 2.3 JSON document listing the same components as the CycloneDX BOM" — say *2.3*, do not claim ISO/IEC 5962 unless you emit 2.2.1 | `bom.spdx.json` release asset, plus the cross-format equality test |
| **#171** REUSE | **REUSE Specification v3.3** (FSFE, Nov 2024; introduced `REUSE.toml` in 3.2) | "`reuse lint` exits 0 on a clean checkout" — REUSE conformance *is* what the tool says, there is no other definition | A green `reuse lint` CI step and (optionally) the `api.reuse.software` badge |
| **#192** SSDF | **NIST SP 800-218 v1.1** — 4 practice groups (PO/PS/PW/RV), **19 practices, 42 tasks** | "Here is a practice-by-practice mapping to JLS's actual controls, with the not-met rows named" — a self-assessment, not an attestation | `docs/ssdf-mapping.md` (to be created), each row citing a repo path or CI lane |
| **#197** ISO 29147 / 30111 | **ISO/IEC 29147:2018** (edition 2, reportedly reconfirmed 2024 — *edition/reconfirmation status UNVERIFIED*) disclosure; **ISO/IEC 30111:2019** handling. Note **ISO/IEC AWI 29147** — a revision is reportedly in preliminary work *(UNVERIFIED)*, so an edition qualifier is mandatory | "JLS's disclosure process follows the structure of 29147/30111" — a *structural alignment* claim, without clause citations unless the documents are actually purchased and read | The upgraded `SECURITY.md` disclosure section |
| **#255** EO 14028 / CISA form | CISA Secure Software Development Attestation Form (March 2024) | **JLS should make no claim here at all.** It is not a US federal contractor. See §"Certification" | — |

Two of these are worth doing on their own merits and two are marginal; that judgement
is recorded per item in the implementation steps.

---

### Implementation procedure

#### Step 0 — Resolve the licence identifier. Everything else depends on it.

The tree currently disagrees with itself, and REUSE will not let you paper over it:

- `README.md:347` — "GNU General Public License v3.0"
- `CONTRIBUTING.md:138` — "JLS is GPLv3-**or-later**"
- `pom.xml:21` — `<name>GNU General Public License v3.0</name>` (free text, no SPDX id)
- `LICENSE` — the GPLv3 text; `pop_GPLv3.pdf` — Poplawski's signed grant
- **Zero of 886 tracked files carry an SPDX identifier or any licence header**
  (verified: `git ls-files | xargs grep -l SPDX-License-Identifier` → 0;
  `git ls-files '*.java' | xargs grep -l "GNU General Public"` → 0)

**Recommendation: `GPL-3.0-or-later`.** `CONTRIBUTING.md` already binds every
contributor to it, so it is the identifier the project has actually been operating
under; narrowing to `GPL-3.0-only` would contradict a live contribution term. Record
the decision as a one-paragraph entry under ARCHITECTURE.md's "Recorded decisions",
and fix `README.md` to match.

> **Cross-section conflict — resolve before either lands.** Section 10
> (packaging/AppStream) reaches the **opposite** recommendation for the same
> decision: it proposes `project_license` = `GPL-3.0-only`, on the strength of
> `flake.nix:78` (`licenses.gpl3Only`), `pom.xml:19-23` ("GNU General Public
> License v3.0"), and the absence of any "or later" clause in `src/`. This
> section weighs `CONTRIBUTING.md:138` ("GPLv3-**or-later**") more heavily.
> Both cannot be right, and both propose editing `pom.xml`'s `<licenses>` block.
> **One identifier must be chosen once**, recorded in `ARCHITECTURE.md`, and
> then applied to `pom.xml`, `flake.nix`, `README.md`, `CONTRIBUTING.md`, the
> AppStream `<project_license>`, and `REUSE.toml` in a single change —
> `LicenseIdentityTest` (below) exists precisely to keep them from diverging
> again. The decision hinges on the `pop_GPLv3.pdf` question immediately below;
> do not let two PRs pick different answers.

**Open question for the maintainer:** confirm the
Poplawski grant in `pop_GPLv3.pdf` is "version 3 or (at your option) any later
version" and not v3-only — I did not read the PDF, and if it is v3-only the whole
tree is `GPL-3.0-only` and `CONTRIBUTING.md:138` is the line that must change.

#### Step 1 — The CRA stance (0.5 day). Lives in `SECURITY.md`.

**Where it lives — recommendation, not a survey.** Put it in `SECURITY.md`, as a new
top-level section placed immediately *before* "## Reporting a vulnerability", with a
one-line pointer from `README.md`'s "License and provenance" section
(`README.md:345–350`). Rationale: `SECURITY.md` is the file a market-surveillance
enquiry, a university procurement office, or a downstream packager actually opens;
GitHub surfaces it in the Security tab; and it already carries the threat model the
stance leans on. Do **not** create `docs/cra-stance.md` — a regulatory position
buried in `docs/` alongside 23 research documents will not be found by the people it
is written for, and a second home invites drift.

The same edit should carry the **safety-critical scope statement** that
`docs/standards-landscape.md` §12.a flags as "currently absent from `SECURITY.md` and
`README.md` and a genuine documentation gap". It is two sentences, it is the correct
answer to entries #223/#225/#227, and it belongs in the same "what this software is
and is not for" section. Doing it in a separate PR wastes the second review.

**Draft, ready to paste** (after the **[SECONDARY]** citations are checked against
EUR-Lex):

> ## Regulatory scope and intended use
>
> **Not for safety-critical use.** JLS is an educational logic simulator. It is not
> qualified, and will never be submitted for qualification, as a software tool under
> ISO 26262-8 §11 (TCL1–3), IEC 61508-3 §7.4.4 (T1/T2/T3), DO-330 (TQL-1..5),
> EN 50128, or IEC 62304. Do not use JLS output as evidence in a safety case, and do
> not use JLS to design anything whose failure can hurt someone. This is a scope
> decision, not a statement about the software's quality; qualification requires an
> audited process and a maintained safety manual that a single-maintainer free
> project cannot sustain.
>
> **EU Cyber Resilience Act (Regulation (EU) 2024/2847) — position as of
> YYYY-MM-DD, JLS x.y.z.** *This is the maintainer's position, not legal advice, and
> the Commission's implementing guidance is still settling.*
>
> JLS is free software distributed at no charge under GPL-3.0-or-later. As of the
> date above the maintainer does not monetise it in any form: there is no paid
> support, no paid hosting or SaaS offering, no priority-support or
> maintenance-contract tier, no dual or commercial licensing, no paid installers, no
> commercial trademark licence, and no donation, sponsorship, or funding channel tied
> to the software or to its development. Development is unpaid and unsponsored.
> Distribution is via public GitHub Releases, the GitHub Packages registry, GHCR, and
> a Nix flake, all at no charge to anyone.
>
> On that basis the maintainer's reading is that JLS is **not placed on the Union
> market in the course of a commercial activity**, that the maintainer is therefore
> **not a "manufacturer"** within the meaning of the Regulation, and that **no legal
> person acts as an "open-source software steward"** for JLS — there is no company,
> foundation, or fiscal host behind it. The manufacturer obligations and the steward
> obligations are read as not attaching to this project as it currently stands.
>
> **What this statement does not say.** It is not a claim of conformity, not a CE
> marking, and not an EU declaration of conformity — no such thing exists for a
> product outside the Regulation's scope, and stewards cannot affix CE marking in any
> case. It says nothing about **you**: if you integrate JLS into a product you place
> on the Union market in the course of a commercial activity, you are the
> manufacturer of that product and its CRA obligations are yours, not this project's.
>
> **What would invalidate it.** Any of the monetisation facts above becoming false;
> the formation of a legal entity that provides sustained support for JLS in a way
> intended for commercial activities; or Commission guidance that reads the
> commercial-activity test differently. The maintainer will revise or withdraw this
> section rather than let it go stale, and it carries a date and a version for that
> reason.
>
> **What JLS does regardless of scope.** The artifacts the Regulation expects of an
> in-scope product already exist here, because they are good practice on their own
> terms and not because anyone is required to produce them: a machine-readable SBOM
> with every release (CycloneDX `bom.json`, SPDX `bom.spdx.json`), reproducible
> builds with a published `.buildinfo`
> ([`docs/reproducibility.md`](docs/reproducibility.md)), signed build-provenance
> attestations, a coordinated vulnerability-disclosure process (below), a documented
> threat model, and automated dependency and static-analysis scanning. A mapping of
> JLS's development practices to NIST SP 800-218 (SSDF) is in
> [`docs/ssdf-mapping.md`](docs/ssdf-mapping.md).

Note the last paragraph is what makes the stance credible: it turns "we think we are
out of scope" into "and here is the evidence we would produce if we weren't". Do not
ship the stance without the artifacts, or it reads as a dodge.

**Stability contracts touched: none.** This is documentation.

#### Step 2 — Make the CycloneDX BOM carry a real SPDX identifier (0.25 day). Do this first; it is free.

Verified against the built BOM (`python3 -c ... target/bom.json`):

```
metadata component: jls   licenses: [{'license': {'name': 'GNU General Public License v3.0', ...}}]
components: 4
  org.tukaani xz 1.12                → ['0BSD']                                (SPDX id resolved)
  org.jfree   org.jfree.svg 5.0.7    → ['GNU General Public License (GPL)']    (free text, no id)
  com.formdev flatlaf 3.7.2          → ['Apache-2.0']                          (SPDX id resolved)
  org.jspecify jspecify 1.0.0        → ['Apache-2.0']                          (SPDX id resolved)
```

Two of five licence entries are free-text names rather than SPDX identifiers,
including **JLS's own**. An SBOM consumer that filters on `license.id` sees JLS as
unlicensed. Fix:

1. In `pom.xml:19–24`, set `<name>GPL-3.0-or-later</name>` and
   `<url>https://spdx.org/licenses/GPL-3.0-or-later.html</url>` so
   `cyclonedx-maven-plugin` resolves an `id`. (Maven's `<name>` is free text; an SPDX
   id is a legal value and is what every SBOM tool wants.)
2. `org.jfree.svg`'s POM is upstream's and cannot be fixed here. Either accept the
   free-text row, or add a `licenseChoice` override in the plugin configuration
   (`pom.xml:585–599`). **Recommendation: accept it and note it** — inventing a
   licence id for someone else's artifact is worse than reporting what their POM
   says.
3. Verify by rebuilding and re-running the `python3` one-liner above; the metadata
   component must show `'id': 'GPL-3.0-or-later'`.

**Worth doing on its own merits: unambiguously yes.** It costs minutes and fixes a
BOM that currently misreports the product's own licence.

#### Step 3 — SPDX SBOM alongside CycloneDX (1 day)

**Is a second SBOM format actually worth it?** Technically, almost no: the BOM has
**four components**, all direct, all with clean coordinates. There is no information
in an SPDX rendering of that graph which is not in the CycloneDX rendering. The case
is entirely about consumers — some procurement and vulnerability-management pipelines
ingest only SPDX, and the CRA's Annex I Part II point (1) requires an SBOM "in a
commonly used, machine-readable format" **[SECONDARY]** without naming one, so
carrying both removes an argument. **Recommendation: do it, but cheaply and at
release time, and be honest in the docs that it is a convenience rendering of the
same four rows.**

**Design decision — generation route.** Three options; pick the third.

1. `org.spdx:spdx-maven-plugin` in the `package` phase. Rejected: it is a second
   independent dependency-graph walker, so the two SBOMs can disagree about the same
   build; and its current release line is 1.0.4-SNAPSHOT-era with SPDX 2.3 output
   (**unverified** beyond a search result), which is a weak thing to pin a release
   asset on.
2. GitHub's dependency-graph SPDX export API. Rejected: it describes what GitHub
   inferred from the repo, not what this build produced, and the `dependency-submission`
   job (`.github/workflows/ci.yml:753`) already covers that surface.
3. **Convert the existing `bom.json`.** One pinned converter, one input, so the SPDX
   document is *derived from* the CycloneDX document and cannot disagree with it by
   construction. `CycloneDX/cyclonedx-cli convert` targets SPDX JSON 2.3;
   `spdx/cdx2spdx` is the SPDX project's own converter. **Recommendation:
   `cyclonedx-cli`, SHA-pinned as a downloaded release binary** — it is the tool
   whose input format JLS actually produces, and the conversion is the documented
   direction (CycloneDX→SPDX loses less than the reverse).

**Design decision — where it runs, and the reproducibility trap.** This is the part
that will bite.

`.github/workflows/ci.yml:787` (`reproducibility` job) diffs
`sha256sum jls-*.jar bom.json` across three builds (same-runner, and a perturbed
rebuild with a different path, `TZ=Pacific/Kiritimati`, `LC_ALL=C`, `umask 077`), and
`docs/reproducibility.md` §1 lists the CycloneDX SBOM as **bit-for-bit
reproducible**. SPDX 2.x documents carry a `creationInfo.created` RFC3339 timestamp
and a `documentNamespace` that conventionally embeds a UUID — **both nondeterministic
by default**. Wiring an unnormalized SPDX generator into `package` therefore either
(a) breaks the reproducibility gate, or (b) sneaks an irreproducible artifact into a
release set whose headline property is reproducibility.

**Recommendation: normalize it and make it reproducible, then gate it.** Concretely:

1. Add a `scripts/make-spdx-sbom.sh` that: converts `target/bom.json` →
   `target/bom.spdx.json` with the pinned converter; then post-processes with `jq` (or
   a tiny Python script alongside `scripts/normalize-msi.py` and
   `scripts/normalize-dmg.py`, which is the established house pattern for exactly this
   kind of determinism post-pass) to set `creationInfo.created` from
   `project.build.outputTimestamp` (`pom.xml:47`, currently `2026-07-16T00:00:00Z`) and
   `documentNamespace` to a version-derived constant such as
   `https://github.com/anadon/JLS/spdx/jls-<version>`, and to sort every array by a
   stable key.
2. Wire it into `scripts/build-installer.sh`'s peers only as far as the release needs:
   invoke it from `.github/workflows/release.yml` after `mvn -B verify …buildinfo`
   (around line 92), *before* the checksum step.
3. Extend `.github/workflows/release.yml:95` to
   `sha256sum jls-*.jar bom.json bom.spdx.json jls-*.buildinfo > SHA256SUMS`, add
   `target/bom.spdx.json` to the release `files:` list (line 113), and add it to the
   `attest-build-provenance` `subject-path` (line 103) so it carries provenance like
   every other asset.
4. Extend the `reproducibility` job's three `sha256sum jls-*.jar bom.json` lines
   (`.github/workflows/ci.yml:804`, `:813`, `:830`) to include `bom.spdx.json`. This is
   the step that converts "we believe it's deterministic" into "CI fails if it isn't",
   and it is the whole reason to do the normalization.
5. Update `docs/reproducibility.md` §1's artifact table with an `SPDX SBOM |
   bom.spdx.json | **Yes** — bit-for-bit` row, and §3.3's `sha256sum` command.
6. Update `README.md`'s jar section (~line 86–95) and `SECURITY.md`'s verification
   recipe to mention both formats.

**Migration/compatibility.** Purely additive: one new release asset and one new line
in `SHA256SUMS`. `sha256sum -c SHA256SUMS` is order-insensitive, so nothing
downstream that consumes `SHA256SUMS` breaks. **Check before landing:** whether
`.github/workflows/release.yml`'s `verify-windows-signatures` job (line 679) or
`scripts/build-container.sh` parses `SHA256SUMS` positionally — I did not read those
in full.

**Stability contracts touched: none.** The batch interface, `.jls` format, and
simulation semantics are untouched. The *release asset set* is not a documented
stability contract, but `docs/reproducibility.md` §1 is a published claim and must be
updated in the same commit.

#### Step 4 — REUSE compliance across the tree (1.5 days). The costly one.

**The real numbers, verified:**

| Population | Count | Can carry an inline header? |
|---|---|---|
| Tracked files total | 886 | — |
| `src/**/*.java` | 292 | yes |
| `test/**/*.java` | 237 | yes |
| `resources/**` (help tree: 83 HTML + 9 GIF + 7 JPEG, icons, packaging metadata) | 106 | HTML yes, GIF/JPEG no |
| `test/resources/**` (byte-exact goldens) | 73 | **NO — see below** |
| `riscv/**` (`.v`, `.vhdl`, `.s`, `.py`) | 25 | yes |
| `docs/*.md` | 23 | yes, but ugly |
| Binary (GIF 76, JPEG 7, PNG 2, ICNS 2, ICO 1, PDF 1) | ~89 | no — needs `.license` sidecar or `REUSE.toml` |

**The golden-file landmine.** `test/resources/hdl/*.v` and `*.vhdl` are
**byte-for-byte goldens** for `VerilogExportGoldenTest` and `VhdlExportGoldenTest`
(`test/jls/hdl/package-info.java:8`: "pinned with byte-for-byte golden files under
`test/resources/hdl`"). Running `reuse annotate` over `test/` prepends an SPDX header
to each of them, the golden tests go red, and the tempting "fix" — making
`VerilogEmitter`/`VhdlEmitter` emit the header too — **changes the emitted HDL, which
is an interop surface** consumed by `iverilog`/`ghdl`/Yosys and documented in
`docs/hdl-support-research.md`. Never annotate `test/resources/`.

**Design decision — headers vs `REUSE.toml`.** Two routes:

- **Per-file headers** (`reuse annotate --copyright="..." --license="GPL-3.0-or-later"`).
  Mechanically trivial to *run*; produces a **529-file Java diff** plus ~89 `.license`
  sidecars. This collides head-on with `CONTRIBUTING.md:38`: "never land a repo-wide
  `final`/formatting sweep — churn hides real diffs." It also breaks `git blame` for
  every file's first line and invalidates nothing else.
- **`REUSE.toml`** (REUSE spec 3.2+). One file at the repo root with path/glob-scoped
  `[[annotations]]` blocks. Achieves `reuse lint` exit 0 with **one new file and zero
  churn**, and lets each population get the right copyright holder.

**Recommendation: `REUSE.toml` only.** It gets the same conformance claim for a
fraction of the cost and none of the churn, and it is the route the spec added
precisely for existing codebases. Per-file headers can be added later, opportunistically,
as files are touched — and `REUSE.toml` and per-file headers coexist fine (the header
wins for that file). Note `REUSE.toml` and the older `.reuse/dep5` are **mutually
exclusive**; use `REUSE.toml`.

**Steps:**

1. Create `LICENSES/GPL-3.0-or-later.txt` containing the exact SPDX-registered licence
   text. Keep the root `LICENSE` as well — GitHub's licence detection and the Debian
   packaging both key on it — and annotate the duplicate in `REUSE.toml`.
2. Create `REUSE.toml` with per-population blocks. The **expensive part is not the
   syntax, it is the copyright audit**, which REUSE forces and which nothing else in
   this repository has ever done:
   - `pop_GPLv3.pdf` — copyright **David A. Poplawski**, not the project. It is his
     signed grant document.
   - `src/**` and `resources/help/**` — substantially inherited from Poplawski's JLS
     4.1 and then modified by this fork. Honest annotation is a two-holder block
     (`SPDX-FileCopyrightText: David A. Poplawski` + the fork maintainer), not a
     single line.
   - `src/jls/edit/images/*.gif` — inherited toolbar icons; same provenance question.
     `scripts/GenerateIcons.java` exists, so some may be generated — check which.
   - `test/resources/hdl/*` — generated *by JLS* from JLS-authored fixtures; project
     copyright.
   - `flake.lock`, `.gitattributes`, `.editorconfig` — trivial/metadata; REUSE still
     wants them annotated.
3. Add a `licensing` job to `.github/workflows/ci.yml` (**to be created** — no such
   lane exists today):

   ```yaml
     licensing:
       name: REUSE lint
       if: github.event_name != 'schedule'
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
         - name: reuse lint
           run: pipx run reuse==<pinned> lint
   ```

   Keep it a **separate job**, not a step in `build`: a licensing failure should be
   legible in five seconds and must not sit behind the 20-minute JDK matrix. Runtime
   is ~20–40 s including the pipx install; the marginal CI cost is negligible against
   the existing 17-job workflow. Pin the version — `reuse` has shipped
   behaviour-changing majors (4.x → 5.x → 6.x).
4. Add the `api.reuse.software` badge to `README.md` next to the OpenSSF Scorecard
   badge (`README.md:3`). It is computed by FSFE's service from the public repo — no
   submission, no fee — and it goes red by itself if compliance lapses.

**Worth doing on its own merits:** **yes, but modestly.** The concrete payoff is that
a 886-file tree with **zero** copyright headers currently gives a downstream packager
or a university legal review no per-file provenance at all, and the audit in step 2 is
work that has genuine value independent of any badge. The badge itself is decoration.

#### Step 5 — SSDF (NIST SP 800-218 v1.1) self-assessment (1 day)

Create **`docs/ssdf-mapping.md`** (to be created) — one table, 19 rows, each citing a
repo path or a CI lane, with not-met rows stated plainly. JLS already meets a
strikingly large fraction; the doc is assembly, not work. Verified evidence available
today:

| SSDF practice | JLS evidence (verified paths) |
|---|---|
| **PO.1/PO.2** define requirements, roles | `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`. Honest row: single maintainer; roles are not separated, and that is a stated limitation, not a gap to paper over |
| **PO.3** supporting toolchains | `pom.xml` pins every plugin version; `.github/dependabot.yml` covers maven, github-actions, and docker; every action in `ci.yml`/`release.yml` is SHA-pinned |
| **PO.4** criteria/metrics for software security | JaCoCo coverage ratchet + PIT mutation ratchet (`CONTRIBUTING.md` "Coverage ratchet"; `.github/workflows/mutation.yml`); SpotBugs threshold High; CodeQL |
| **PO.5** secure development environments | `.devcontainer/Dockerfile`, `flake.nix`; CI runs on ephemeral runners with `permissions: contents: read` by default |
| **PS.1** protect code from unauthorized change | GitHub branch protection — **verify and cite; I could not confirm the settings from the tree** |
| **PS.2** provide a verification mechanism | `SHA256SUMS`, `SHA256SUMS-installers-*`, Sigstore keyless provenance (`gh attestation verify`), cosign on container images, Authenticode via SignPath — all documented in `SECURITY.md` "Release artifact signing & verification" |
| **PS.3** archive and protect each release | `.buildinfo` per release + `docs/reproducibility.md`; **this is the strongest row in the table** and most projects cannot fill it |
| **PW.1** design to meet security requirements | `SECURITY.md` threat model (untrusted `.jls` files); `docs/collaborative-editing-research.md` §6 |
| **PW.2** review the design | `ARCHITECTURE.md` "Recorded decisions"; `docs/grand-architecture.md` §4.3 plugin trust boundary |
| **PW.4** reuse well-secured software | four runtime dependencies total (verified from `bom.json`); dependabot; `dependencyConvergence` enforcer rule (`pom.xml:620`) |
| **PW.5/PW.6** secure coding, compile with hardened settings | `-Werror` + NullAway + JSpecify (`CONTRIBUTING.md` "Nullness is a compiler-checked contract"), SpotBugs, Spotless, doclint gate |
| **PW.7** review/analyze human-readable code | PR-only contribution flow (`CONTRIBUTING.md`), CodeQL required on every PR |
| **PW.8** test executable code | `mvn verify`; golden-file suites (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, `RiscvCpuGoldenTest`); fuzzers (`ContainerMutationFuzzTest`, `GenerativeRoundTripFuzzTest`); hostile-input suite (`UntrustedFileHardeningTest`) |
| **PW.9** secure default configuration | headless-core boundary (`HeadlessCoreRatchetTest`); socket confinement (`SocketConfinementRatchetTest`); listener created only on an explicit Share gesture, so batch and default GUI open no port (`SECURITY.md`) |
| **RV.1** identify vulnerabilities | dependabot, CodeQL weekly + per-PR, OpenSSF Scorecard weekly (`.github/workflows/scorecard.yml`), fuzz suites |
| **RV.2** assess, prioritize, remediate | `SECURITY.md` "Reporting a vulnerability" — thin today; Step 6 thickens it |
| **RV.3** root cause analysis | `CONTRIBUTING.md`: "Every bug fix carries a regression test that fails before the fix and passes after" — a literal RV.3 control |

**Worth doing on its own merits: yes, and it is the best-value item in this section.**
It costs one day, invents nothing, and produces the single page that answers both a
hypothetical CRA market-surveillance request and the security questionnaire a
university procurement office will send. It also surfaces the two or three genuine
gaps (branch protection, no separation of duties, no documented remediation SLA) that
are otherwise invisible.

**Do not fill in the CISA Secure Software Development Attestation Form.** See
§"Certification".

#### Step 6 — ISO/IEC 29147 + 30111 alignment for `SECURITY.md` (0.5–1 day)

**Read the existing process first — the gap really is small.** `SECURITY.md:44–56`
already has: a private reporting channel (GitHub security advisories), what to include
(version, reproduction, impact), an acknowledgement target ("within two weeks"), a
coordinated-disclosure preference, credit unless declined, and a scope statement
(circuit files are untrusted input; parser crashes, resource exhaustion, and code
execution from a hostile file are in scope). That is already most of ISO/IEC 29147's
*receive* half.

What is missing, mapped to what the standards are structurally about (**stated
without clause citations — I have not purchased or read either document**):

**29147-shaped (external-facing) additions:**

1. **Supported versions.** Which releases receive fixes. Today nothing says. A short
   table or one sentence ("the latest release only") is enough and is what every
   reporter and packager asks first.
2. **A remediation and disclosure timeline.** Acknowledgement ≤ 14 days already
   exists; add a target for triage, a target for a fix or a status update, and a
   default disclosure horizon (90 days is the common default) with an explicit "we
   will tell you if we need longer, and why".
3. **Where advisories are published.** GitHub Security Advisories + `CHANGELOG.md` +
   the release notes. Say it, so a consumer can subscribe.
4. **CVE assignment.** GitHub is a CNA and can assign a CVE through the GHSA
   workflow. State whether the project will request one, and for what severity.
5. **Explicit out-of-scope list.** Worth naming: bugs reachable only by the user's own
   circuit file with no sharing vector; anything requiring a modified JLS; the
   deliberately-unsigned macOS app (`README.md:37–43`, decisions #128/#135); the
   absence of project-held GPG signatures on rpm/AppImage (`SECURITY.md` #136).
6. **Safe harbour and no-bounty.** One sentence each. Reporters ask; silence reads as
   hostile.
7. **Single-maintainer availability caveat and escalation path.** This is the honest
   one, and 29147's whole premise is that a reporter knows what to expect. If the
   maintainer is unreachable for a month, say what a reporter should do.

**30111-shaped (internal-process) additions** — a short "How a report is handled"
subsection, four or five bullets: intake → verification and reproduction → severity
assessment → fix developed on a private fork with a regression test (per
`CONTRIBUTING.md`) → release + advisory + CHANGELOG entry → reporter notified. Then
one honest paragraph: there is one maintainer, so there is no separation between
triage and fix, and reports involving the maintainer's own commits have no
independent reviewer.

**Worth doing on its own merits: yes.** It is the cheapest item here after Step 2, and
the missing "supported versions" and "what happens after you report" lines are things
reporters actually need, independent of any standard.

**Do not buy the ISO documents for this.** CHF 179 for 29147:2018 **[SECONDARY]**,
30111:2019 price unverified, plus a revision (ISO/IEC AWI 29147) is in preliminary
work — so any clause citation you buy today has a shelf life. Make the *structural
alignment* claim without clause numbers. If a procurement questionnaire later demands
clause-level mapping, buy them then.

---

### Testing procedure

The house rule applies: **every sentence in the stance and the mapping doc is either
checked by CI or it is a promise nobody keeps.** Conformance here is proven by four
mechanisms.

#### 1. New JUnit tests (repo style: `FooTest.java` under `test/`, `Path.of("docs", …)` from `user.dir` per `ExtensionPointCatalogTest.java:93` and `FileFormatSpecTest.java:52`)

- **`test/jls/LicenseIdentityTest.java`** (to be created). The cheap invariants a Java
  test can own without reimplementing `reuse lint`:
  - `pom.xml`'s `<licenses><name>` equals exactly `GPL-3.0-or-later`;
  - `LICENSES/GPL-3.0-or-later.txt` exists and is non-empty;
  - `README.md`, `CONTRIBUTING.md`, and `SECURITY.md` all name the same identifier —
    a regex over the three files asserting no file mentions a *different* GPL SPDX id.
    This is the test that would have caught today's `README.md:347` vs
    `CONTRIBUTING.md:138` disagreement.
  - `REUSE.toml` exists and `.reuse/dep5` does **not** (they are mutually exclusive).
- **`test/jls/SbomConsistencyTest.java`** (to be created). The cross-format oracle,
  and the reason to derive rather than dual-generate:
  - parse `target/bom.json` and `target/bom.spdx.json`;
  - assert a **bijection** between CycloneDX `components[].purl` and SPDX
    `packages[].externalRefs` PACKAGE-MANAGER purls — same set, same versions, same
    count (4 today);
  - assert the SPDX `creationInfo.created` equals the `project.build.outputTimestamp`
    value, and `documentNamespace` contains no UUID (i.e. the normalization ran);
  - assert the SPDX document's declared licence for the JLS package is
    `GPL-3.0-or-later`.
  - Skip cleanly with `Assumptions.assumeTrue(Files.exists(spdx))` so a plain
    `mvn test` (no `package` phase, no release script) does not fail — the same
    skip-when-absent discipline README describes for the `iverilog` HDL tests.
- **`test/jls/SbomCoversJarTest.java`** (to be created). **The one that catches the
  failure that actually matters.** `maven-shade-plugin` (`pom.xml:232`) bundles
  dependencies into `target/jls-*.jar`; nothing today checks that the BOM lists what
  the jar contains. Walk the shaded jar's top-level package roots
  (`org/tukaani/`, `org/jfree/svg/`, `com/formdev/flatlaf/`, `org/jspecify/`), map each
  to a BOM component, and fail if a root has no component or a component has no root
  (allowing a declared exception list for `META-INF` and JLS's own `jls/`). Regression
  it turns red: someone adds a runtime dependency and the BOM silently under-reports —
  which is the single worst failure mode for an SBOM claim, and the one a CRA-shaped
  or procurement-shaped consumer would catch instead of you.

#### 2. Golden-file strategy

Do **not** golden the whole `bom.spdx.json`. Its content changes with every version
bump and every dependency update, so a byte-exact golden would be a maintenance tax
that gets `-Dtest.update` rubber-stamped and stops meaning anything. That is the
opposite of the house pattern, where goldens pin things that are *supposed* to be
frozen (`docs/batch-interface.md`'s VCD profile, `test/resources/hdl/*`).

Golden the **normalized projection** instead: a sorted, one-line-per-component
`purl@version license-id` rendering, written to
`test/resources/sbom/components.golden` (to be created). It changes exactly when the
dependency set or a licence changes — which is precisely when a human should look —
and the diff is one readable line. `SbomConsistencyTest` produces that projection from
*both* formats and compares each against the same golden, so a converter regression
shows up as a one-line diff rather than a 4000-line JSON diff.

#### 3. External-tool validation, skip-when-absent

The repo already has this discipline (README: HDL-export tests "compile the generated
Verilog with `iverilog` when it is installed and skip cleanly when it is not").
Reuse it:

- **`reuse lint`** — the real REUSE gate. Not a JUnit test; a CI step, because it is a
  Python tool and shelling out from JUnit to an optional tool is worse than a lane.
- **SPDX schema validation** — `pyspdxtools` (SPDX's own Python tools) or
  `sbom-utility validate`. Run in the same CI lane; **do not** make `mvn verify` depend
  on a Python toolchain.
- **`cyclonedx-cli validate`** on `bom.json` — free, and catches plugin-upgrade
  regressions.

#### 4. CI lane changes (`.github/workflows/`)

| File | Change | Type |
|---|---|---|
| `ci.yml` | **new `licensing` job**: `reuse lint`, `cyclonedx-cli validate`, `pyspdxtools` validate. ~40 s | to be created |
| `ci.yml:804, :813, :830` | extend the three `sha256sum jls-*.jar bom.json` invocations in the `reproducibility` job to include `bom.spdx.json` | edit |
| `release.yml:95` | add `bom.spdx.json` to the `SHA256SUMS` line | edit |
| `release.yml:103` | add `target/bom.spdx.json` to `attest-build-provenance`'s `subject-path` | edit |
| `release.yml:113` | add `target/bom.spdx.json` to the release `files:` list | edit |

The `reproducibility` job edit is the highest-value line in this whole section: it is
what turns "our SPDX SBOM is reproducible" from a claim into a gate.

#### 5. Property / fuzz opportunities

Mostly not worth it here, and saying so is better than inventing work. The one genuine
opportunity: parameterize `SbomConsistencyTest`'s bijection check over a synthetic
BOM with components added and removed, to prove the comparison is not vacuously
passing on an empty set. Ten lines; guards against the classic "the assertion never
ran" bug — the same failure mode `CONTRIBUTING.md` warns about for JaCoCo rule
`include` patterns ("a floor that has never been seen to fail should be assumed
vacuous").

#### 6. What regression turns each test red

- Adding a runtime dependency → `SbomCoversJarTest` (if it misses the BOM) and the
  components golden (if it doesn't).
- A dependency whose POM has no resolvable SPDX licence → the components golden shows
  a free-text name instead of an id.
- Bumping `cyclonedx-maven-plugin` or the SPDX converter in a way that changes output
  shape → the components golden and `SbomConsistencyTest`.
- Reintroducing nondeterminism into the SBOM (unpinned timestamp, UUID namespace) →
  the `reproducibility` job's perturbed rebuild.
- Adding any file without copyright info → `reuse lint`.
- Changing the licence identifier in one doc and not the others →
  `LicenseIdentityTest`.
- **Not caught by anything, by design:** the CRA stance going stale because a
  monetisation fact changed. No test can check that. This is why the stance carries a
  date, a version, and an explicit invalidation list — the mitigation is editorial,
  and it should be added to the release checklist rather than pretended into CI.

---

### Certification / conformance procedure

**Every item in this section is self-asserted. There is no body to apply to, no
registry to join, no fee to pay, and no certificate to receive.** Stated plainly
because the section heading invites the opposite assumption.

#### EU Cyber Resilience Act

- **Who assesses:** for an out-of-scope product, **nobody**. There is no submission,
  no registry, no fee, no validity period, no renewal. Market surveillance authorities
  in the Member States are the enforcement mechanism, and they act reactively.
- **If JLS were in scope** (it is the maintainer's reading that it is not): as a
  default-category product with digital elements it would be **self-assessed** —
  internal production control, no notified body — producing technical documentation,
  an EU declaration of conformity, and CE marking, plus the Annex I Part I (product
  security) and Part II (vulnerability handling, including the SBOM at point (1))
  requirements for the support period. Only "important" (Annex III) and "critical"
  (Annex IV) products need third-party involvement. *My reading, unverified:* a
  schematic editor and simulator is on neither annex. **Confirm against the primary
  text before relying on it.**
- **If a legal person ever stewards JLS:** Article 24 **[SECONDARY]** — a documented
  cybersecurity policy, cooperation with market surveillance authorities, reporting of
  actively exploited vulnerabilities and severe incidents via the single reporting
  platform, and user notification. **No CE marking** (recital 19 **[SECONDARY]**).
  Note that the Article 24 policy is, in substance, `SECURITY.md` plus
  `docs/ssdf-mapping.md` — which is a second reason to write them now: if the
  category ever changes, the artifact already exists.
- **Dates to diary [SECONDARY]:** 11 September 2026 (Article 14 reporting), 11
  December 2027 (main application).
- **What invalidates the stance:** monetisation in any form; formation of a legal
  entity stewarding the project for commercially-intended use; Commission guidance
  reading the commercial-activity test differently. Also — and this is the one to
  watch — a *downstream* commercial redistributor does not invalidate the stance, but
  will absolutely ask JLS for CRA artifacts anyway, which is the practical reason to
  have the SBOM/SSDF/disclosure set on the shelf.

**What a credible self-assertion consists of, concretely:** (1) it is dated and
version-stamped; (2) it recites the *facts* it depends on rather than asserting the
conclusion bare; (3) it labels itself as the maintainer's reading and not legal
advice; (4) it names what would invalidate it; (5) it distinguishes what it says about
the project from what it does not say about downstream integrators; (6) it points at
real artifacts. The draft in Step 1 does all six. A stance that says only "the CRA
does not apply to us" fails all six and is worse than silence, because it invites
reliance.

#### NIST SP 800-218 (SSDF) — landscape entries #192 and #255

- **Who assesses:** nobody. SSDF is a framework, not a certification scheme. There is
  no SSDF certificate from NIST or anyone else.
- **The CISA Secure Software Development Attestation Form** (published March 2024
  under EO 14028 / OMB M-22-18 and M-23-16) is the one place SSDF turns into a filed
  document — and **JLS should not file it**. It is a representation made by a software
  producer *selling to a US federal agency*, signed by a company officer, with False
  Claims Act exposure attached. JLS sells to nobody.
- **Status caveat [SECONDARY, and important]:** the mandate around that form was
  materially changed by **EO 14306 (June 2025)**, which reportedly removed the
  requirement for contractors to submit attestations to CISA and struck the associated
  FAR update, and by **OMB M-26-05 (January 2026)**, which reportedly rescinded
  M-22-18's approach in favour of an agency-led risk-based one. I verified this only
  from law-firm client alerts, not from the EO or memorandum text. Treat landscape
  entry #255 as **in flux**, and do not write anything into `SECURITY.md` that depends
  on the form's status.
- **What a credible self-assertion consists of:** `docs/ssdf-mapping.md` with all 19
  practices listed — *including the ones JLS does not meet* — each row citing a repo
  path or a CI lane a reader can click. A mapping with no "not met" rows is not an
  assessment, it is marketing.

#### REUSE / SPDX identifiers — landscape entry #171

- **Who assesses:** self-asserted, but with the closest thing to third-party
  verification available in this section: **`api.reuse.software`**, FSFE's service,
  computes compliance from the public repository and serves a live badge. No
  submission, no fee, no validity period; it turns red by itself when compliance
  lapses. That is a better property than most certificates.
- **Cost:** zero. **Elapsed time:** the badge is live as soon as the service polls.
- **What invalidates it:** any file landing without copyright/licensing info — which
  the CI lane catches before merge, so in practice the badge and the gate are the same
  fact.

#### SPDX SBOM — landscape entry #187

- **Who assesses:** nobody. Validation is schema validation (`pyspdxtools`,
  `sbom-utility`), which is a correctness check, not a certification.
- **Precision requirement:** ISO/IEC 5962:2021 **is SPDX 2.2.1**. If the pipeline
  emits SPDX 2.3 (which `cyclonedx-cli convert` targets), the honest claim is "SPDX
  2.3 JSON", *not* "ISO/IEC 5962". Getting this wrong is the kind of small overclaim
  that discredits an otherwise careful compliance page. Say the version number.

#### ISO/IEC 29147 and 30111 — landscape entry #197

- **Who assesses:** **nobody, for these two documents.** They are process standards
  with no certification scheme of their own. Organizations that want a certificate get
  **ISO/IEC 27001** (which *is* certifiable, by an accredited body, typically a
  three-year cycle with annual surveillance audits and four- to five-figure costs) and
  use 29147/30111 as supporting practice. ISO 27001 is entry #239 in the landscape and
  is out of reach and out of scope for a single-maintainer project — say so and move
  on.
- **Cost of the documents themselves:** ISO/IEC 29147:2018 is CHF 179 **[SECONDARY]**;
  ISO/IEC 30111:2019 price **unverified**. Search results suggest 29147:2018 may be
  viewable in full on the ISO Online Browsing Platform — **unverified**, and worth
  five minutes to check before spending anything.
- **Revision risk:** **ISO/IEC AWI 29147** ("Cybersecurity — Vulnerability disclosure
  processes") appears in ISO's catalogue as work in progress **[SECONDARY]**. Any
  clause-level citation written today has a shelf life. This is the concrete reason to
  make the structural claim without clause numbers.
- **What a credible self-assertion consists of:** the upgraded `SECURITY.md` — a
  published policy, a named channel, stated timelines, a supported-versions statement,
  a published-advisories location, and an honest description of the internal handling
  stages including the single-maintainer limitation.

---

### Effort, risk, and failure modes

#### Sizing

| Step | Maintainer-days | Reasoning |
|---|---|---|
| 0 — resolve `GPL-3.0-only` vs `-or-later` | 0.25 | Reading `pop_GPLv3.pdf`, one decision, three file edits |
| 1 — CRA stance + safety-critical scope statement | 0.5 | Drafting is done below; the cost is one careful read of the OJ text to check the four **[SECONDARY]** citations |
| 2 — SPDX id in the CycloneDX BOM | 0.25 | Two lines in `pom.xml`, one rebuild, one verification |
| 3 — SPDX SBOM at release + normalization + repro gate | 1.0 | One script, five workflow-line edits, `docs/reproducibility.md` update; the normalization post-pass is the only real work and `scripts/normalize-*.py` are the templates |
| 4 — REUSE via `REUSE.toml` + CI lane + badge | 1.5 | Tooling is an afternoon; the **copyright-holder audit** across the Poplawski inheritance is the day |
| 5 — `docs/ssdf-mapping.md` | 1.0 | 19 practices × find and verify the citation; nothing is invented |
| 6 — `SECURITY.md` 29147/30111 upgrade | 0.75 | Seven external additions, one internal-process subsection |
| Tests (`LicenseIdentityTest`, `SbomConsistencyTest`, `SbomCoversJarTest`, golden) | 1.0 | `SbomCoversJarTest`'s jar walk is the fiddly one |
| **Total** | **~6.25** | Call it **5–7 maintainer-days**, spread over four or five PRs |

Add **+1.0 day and a 529-file diff** if per-file SPDX headers are chosen over
`REUSE.toml`. Do not.

#### Top three ways this goes wrong

1. **The stance is read as legal advice, or written as a legal conclusion.** A
   maintainer stating flatly "the CRA does not apply to JLS" is asserting a legal
   conclusion about a regulation whose Commission guidance is still settling, without
   counsel. Two concrete harms: a European institutional user treats the paragraph as
   a compliance certificate and skips their own assessment; or the maintainer's
   circumstances change (a sponsorship, a support contract, a foundation) and a stale
   paragraph is now a false public statement. *Mitigation:* the six properties of a
   credible self-assertion in §Certification — especially the dated facts and the
   invalidation list — plus adding "re-read the CRA stance" to the release checklist.
2. **Attestation theater.** Two SBOM formats, a 19-row mapping table, and a badge —
   none regenerated, none checked, quietly diverging from the build. The specific
   catastrophic version: **the SBOM under-reports the shaded jar** after a dependency
   is added, so JLS ships a document that affirmatively misstates its own contents.
   That is worse than shipping no SBOM. *Mitigation:* `SbomCoversJarTest`, the
   components golden, and the `reproducibility`-job extension are all non-optional;
   if any of the three is cut, cut Step 3 entirely.
3. **The REUSE sweep collides with the golden files and the anti-churn rule.**
   `reuse annotate` over `test/**` prepends headers to `test/resources/hdl/*.v` and
   `*.vhdl`, which are byte-for-byte goldens (`test/jls/hdl/package-info.java:8`).
   The golden tests go red; the tempting fix is to make `VerilogEmitter` emit the
   header, which **changes an interop surface** consumed by `iverilog`, `ghdl`, and
   Yosys. Separately, a 529-file header diff violates `CONTRIBUTING.md:38` head-on.
   *Mitigation:* `REUSE.toml` only, `test/resources/` never annotated, and if headers
   are ever added they land as one mechanically-generated commit containing nothing
   else.

#### Do NOT do this if

- **Any monetisation is under consideration** — paid support, GitHub Sponsors tied to
  the software, a commercial fork, paid installers. Then the stance is not a
  documentation task, it is a question for someone qualified, and publishing a
  paragraph that will need retracting in six months is worse than publishing nothing.
- **The `reproducibility` job will not be extended to cover `bom.spdx.json`.** An
  ungated second SBOM is a second thing that can silently disagree with the first,
  inside a project whose headline supply-chain property is bit-for-bit
  reproducibility. Ship one SBOM, well-gated, over two SBOMs, one of them unchecked.
- **REUSE would land as per-file headers in a PR that also does anything else.**
- **`docs/ssdf-mapping.md` would claim practices CI does not demonstrate.** A mapping
  is only worth writing if its "not met" rows are honest; the PS.1 branch-protection
  row in particular must be verified against the repository settings, not assumed.
- **Someone proposes buying ISO/IEC 29147 and 30111 to do Step 6.** The structural
  alignment is achievable from the standards' publicly documented scope, a revision is
  in progress, and the money is better spent on nothing at all.
- **The stance would be written without the artifacts.** "We are out of scope, and
  here is our SBOM, provenance, disclosure process, and SSDF mapping anyway" is
  credible. "We are out of scope" alone is a dodge, and reads as one.

---

### Sources

**Repository paths (all verified by reading in this pass):**

- `/home/user/JLS/docs/standards-landscape.md` — §11.4 entries 186–198, §12.c entries
  245–255, §13.2 items 2/4/5, §12.a's recorded absence of a safety-critical scope
  statement
- `/home/user/JLS/SECURITY.md` — existing disclosure process (lines 44–56), signing
  and verification model (lines 58–102), collaboration threat model
- `/home/user/JLS/README.md` — installer/verification story (lines 48–70), jar + BOM
  (lines 86–95), licence statement (lines 345–350), badge row (line 3)
- `/home/user/JLS/CONTRIBUTING.md` — GPLv3-or-later (line 138), anti-churn rule (line
  38), regression-test rule, coverage/mutation ratchet conventions
- `/home/user/JLS/ARCHITECTURE.md` — "Recorded decisions" section (the format the
  licence-identifier decision should follow), test layout
- `/home/user/JLS/pom.xml` — `<licenses>` block (19–24), `project.build.outputTimestamp`
  (47), dependencies (63–124), `maven-shade-plugin` (232–257),
  `cyclonedx-maven-plugin` 2.9.2 (585–599), enforcer `dependencyConvergence` (620)
- `/home/user/JLS/target/bom.json` — 4 components; two of five licence entries carry a
  free-text `name` and no SPDX `id`, including JLS's own
- `/home/user/JLS/.github/workflows/ci.yml` — `dependency-submission` (753),
  `reproducibility` (787) and its three `sha256sum jls-*.jar bom.json` lines (804,
  813, 830), installer-reproducibility lanes (855, 912, 989, 1053)
- `/home/user/JLS/.github/workflows/release.yml` — buildinfo build (~92), `SHA256SUMS`
  (95), `attest-build-provenance` (103), release `files:` (113),
  `verify-windows-signatures` (679)
- `/home/user/JLS/.github/workflows/scorecard.yml`, `codeql.yml`,
  `/home/user/JLS/.github/dependabot.yml`
- `/home/user/JLS/docs/reproducibility.md` — §1 specified-artifact table, §3.3 rebuild
  recipe, §4 CI gate
- `/home/user/JLS/test/jls/ExtensionPointCatalogTest.java:93` and
  `/home/user/JLS/test/jls/FileFormatSpecTest.java:52` — the house pattern for a test
  that reads a repo document
- `/home/user/JLS/test/jls/hdl/package-info.java:8` — "byte-for-byte golden files under
  `test/resources/hdl`"
- File census: `git ls-files` → 886 tracked; 292 `src/**/*.java`, 237 `test/**/*.java`,
  106 `resources/**` (of which 83 HTML, 9 GIF, 7 JPEG), 73 `test/resources/**`,
  25 `riscv/**`, 23 `docs/*.md`; ~89 binary files **repo-wide** (76 GIF — 33 in
  `src/jls/edit/images/`, 34 in the unreferenced `src/jls/images/` duplicate,
  9 under `resources/help/` — plus 7 JPEG, 2 PNG, 2 ICNS, 1 ICO, 1 PDF).
  **Zero** files contain `SPDX-License-Identifier` or a GPL header.

**External documents — status of verification:**

- **Regulation (EU) 2024/2847 (Cyber Resilience Act).** **PRIMARY TEXT NOT READ.**
  `eur-lex.europa.eu` returns HTTP 403 through this environment's agent proxy
  (confirmed in `curl "$HTTPS_PROXY/__agentproxy/status"`), as do
  `digital-strategy.ec.europa.eu`, `cyberresilienceact.eu`, `policy.openssf.org`, and
  `en.wikipedia.org`. Everything marked **[SECONDARY]** — Art. 3(13)/3(14), Art. 14,
  Art. 24, recitals 18/19, Annex I Part II point (1), and the dates 11 Sep 2026 /
  11 Dec 2027 — came from: the OpenRegulatoryCompliance WG whitepaper
  `github.com/orcwg/orcwg/blob/main/cyber-resilience-sig/whitepapers/stewards-and-cra.md`
  (fetched successfully), and search-index summaries of the Commission's CRA summary
  page, `craact.eu`, `streamlex.eu`, and `cra-facts.com`. **Read the OJ text before
  publishing the stance.**
- **NIST SP 800-218 v1.1 (SSDF).** Structure (4 groups PO/PS/PW/RV, **19 practices, 42
  tasks**) verified from multiple secondary sources; the document itself is free from
  NIST and should be read directly when writing `docs/ssdf-mapping.md`.
- **CISA Secure Software Development Attestation Form; EO 14028; EO 14306 (June 2025);
  OMB M-22-18, M-23-16, M-26-05 (January 2026).** **UNVERIFIED against primary
  sources.** The claim that EO 14306 removed the CISA-submission mandate and struck the
  FAR update, and that M-26-05 rescinded M-22-18's approach, comes from law-firm client
  alerts (Wiley, Davis Wright Tremaine) surfaced by search. Nothing in this section
  should depend on it.
- **SPDX / ISO/IEC 5962:2021.** The ISO edition corresponds to SPDX 2.2.1 — my
  knowledge, **unverified in this pass**. `cyclonedx-cli` converts to SPDX JSON 2.3;
  `spdx/cdx2spdx` is the SPDX project's converter; `org.spdx:spdx-maven-plugin` exists
  but its current line is around 1.0.4-SNAPSHOT (2026-06) and reportedly still emits
  SPDX 2.3 — **unverified**.
- **REUSE Specification v3.3** (FSFE, released November 2024 with `reuse` tool 5.0.0;
  `REUSE.toml` introduced in 3.2 and mutually exclusive with `.reuse/dep5`); badge
  service at `api.reuse.software`. Verified from `reuse.software` search results;
  spec page `reuse.software/spec-3.3/` not fetched directly.
- **ISO/IEC 29147:2018** (edition 2, reviewed and confirmed 2024, CHF 179) and
  **ISO/IEC 30111:2019**; **ISO/IEC AWI 29147** revision in progress. From ISO
  catalogue search results; **prices and revision status unverified**, 30111 price not
  found. Possible free full text on the ISO OBP — **unverified**.
- **OpenSSF Best Practices Badge** (landscape entry #287, adjacent to this section):
  `bestpractices.dev` was also blocked by the proxy in this session; nothing here
  depends on it.
