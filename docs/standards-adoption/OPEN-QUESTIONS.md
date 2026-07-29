# Open questions and unverified facts

Every claim in [`README.md`](README.md) and the eleven sections beside it that
the authoring pass could **not** verify, collected in one place. The substance
is also marked inline in each section's `Sources` block; this file exists so the
list can be worked through as a checklist rather than rediscovered by reading
eleven documents.

Two rules for using it:

- **Nothing here should reach a syllabus, a purchase order, a published
  conformance claim, or a legal position until it is checked against the primary
  source.** Several are dated commercial facts or regulatory readings.
- Items are the authors' own flags. An item's absence is not evidence a claim
  was verified — it means no author raised it.

## Blocking

These stop specific work from starting.

- ~~pop_GPLv3.pdf: is David Poplawski's grant 'version 3 only' or 'version 3 or
  (at your option) any later version'? No agent read it. This single fact decides
  the sections 09/10 licence conflict and therefore pom.xml, flake.nix,
  README.md, CONTRIBUTING.md, the AppStream `<project_license>`, and
  REUSE.toml.~~ **RESOLVED — the licence-identifier work is unblocked.** The
  document was read: it is a letter of 16 January 2014 in which Michigan
  Technological University consents to the open-source release of JLS — "written
  by David Poplawski, Joshua Marshall, et al." — under GPLv3. It names GPLv3
  *without* the "or any later version" wording. The maintainer, who is one of the
  named authors, elected **`GPL-3.0-or-later`**; `pom.xml`, `flake.nix` and
  `README.md` were made consistent with `CONTRIBUTING.md` and the About dialog,
  which already said or-later. Section 09's Step 0 and section 10's step (a)1
  should both now encode `GPL-3.0-or-later`, and the blockquotes in those two
  files describing the conflict are historical. See the CHANGELOG entry and the
  README's "License and provenance" section for the recorded caveat that
  or-later is this project's election rather than something the letter grants.

- `src/jls/images/` (34 GIFs) is a near-duplicate of `src/jls/edit/images/` (33 GIFs, differing only by `go.GIF`) and is referenced by no `src/**.java`. This is a repo-hygiene finding, not a playbook error, but it bears on section 09's REUSE copyright audit (it doubles the icon-provenance surface for no benefit) and on section 01's palette-icon rendering. Someone should decide whether it is dead weight or a build input before the REUSE work starts.

## Per section

### 01-iec-ieee-symbols — IEEE 91/91a-1991 and IEC 60617-12 schematic symbol conformance (#43, #44, #45)

*Verdict: do-it-if · 8-12 maintainer-days*

- Publication status, current edition and price of ANSI/IEEE Std 91-1984 + 91a-1991 and IEC 60617-12 — all unverified. IEEE 91 may be withdrawn/inactive and IEC 60617 parts may exist only inside the IEC 60617 online database, which changes how the conformance document cites clauses.
- The exact clause numbers and specified proportions for every row of the scope table (negation circle diameter, dynamic-input triangle, three-state output indicator, the G/C/EN/A dependency wiring for Mux, Register and Memory). The symbol assignments in the section come from general engineering knowledge, not from the documents, and MUST be checked before any conformance claim is published.
- Whether IEC 60617-12 carries the distinctive shapes in an annex. If it does, the *existing* JLS gate drawing could be claimed too, which would be a cheap bonus and would change the framing of the whole item.
- Maintainer decision: does JLS buy the primary documents? This is the single gate between a real conformance claim and a cosmetic 'IEC-style' feature, and it decides whether the effort is 8-12 days or ~4.
- Maintainer decision: is `ShiftRegister` (a combinational barrel shifter, not an IEC `SRG n` stage-shifting register) recorded as a named non-conformance, or is the element reshaped? Recommendation is to record the non-conformance and change nothing.
- Maintainer decision: render rectangular palette icons at runtime from the symbol model (recommended, kills icon-vs-canvas drift permanently) or hand-author a parallel set of GIFs in src/jls/edit/images/ (cheaper, unverifiable).
- Whether the SVG golden survives the Linux/Windows/macOS CI matrix — JFreeSVG positions <text> from font metrics. Fallback is asserting on path data only, which needs a trial before the test strategy is committed.
- Priority: this competes with the VPAT/ACR item for the same budget, and docs/standards-landscape.md §12.d says the VPAT is the one most likely to actually be demanded of JLS. No issue, user report, or course in the tree currently asks for IEC symbols.

### 02-openssf-badge — OpenSSF Best Practices Badge - passing, silver, gold (#287)

*Verdict: do-it · 3-5 maintainer-days*

- No fee is asserted from the program's own description, not from a published fee schedule; bestpractices.dev itself is unreachable from this environment (outbound proxy returns 403 for that host), so every badge-app fact was read from the application's source repository on raw.githubusercontent.com instead of the live site.
- Criteria counts (67/55/23) come from the criteria.yml dump plus a third-party corroboration; the criteria set is versioned and revised upstream (the separate OpenSSF Baseline series was recently added to the same app), so counts and wording will drift.
- Whether jls.collab.net.Handshake carries a protocol-version field was not checked — it decides the honest answer to silver's crypto_algorithm_agility (SHOULD). Verify before answering.
- signed_releases is a judgement call the maintainer must make: the criterion's rationale assumes a published long-lived public key, while SECURITY.md deliberately refuses one (#136) in favour of keyless Sigstore attestations. Answering Met is defensible but contestable and should be flagged as such in the ledger.
- access_continuity (silver MUST) cannot be answered fully credibly by one person. Decide whether to recruit a named second GitHub repo admin, or to answer Met on the strength of reproducible in-repo builds, no project-held signing key, no project infrastructure, and GPL fork-ability — and say plainly which was chosen.
- internationalization (silver SHOULD) conflicts with the project's recorded refusal to internationalize until a course asks. Confirm the maintainer is content to answer Unmet-with-justification rather than treat it as a gap.
- Exact live coverage percentages were not measured — only the enforced pom.xml floors were read. Take the figure from a fresh headless `mvn clean verify` on JDK 25 before writing any number into the badge ledger.
- Whether the repository currently carries a `good first issue` label could not be checked offline; relevant only to gold's small_tasks, which is not being pursued.

### 03-accessibility-conformance — Accessibility conformance: VPAT/ACR, Section 508, EN 301 549, WCAG 2.2 (#209-#215, #256-#258)

*Verdict: do-it-if · 13-22 (minimum viable ACR: 8) maintainer-days*

- Whether VPAT 2.5 is still ITI's current revision and its exact publication date — download the template fresh from itic.org rather than reusing a copy from another vendor's ACR.
- Whether EN 301 549 V3.2.1 (2021-03, WCAG 2.1 baseline) is still the harmonised text or a V4.x referencing WCAG 2.2 has superseded it; the clause-11 sub-numbering (11.5.2.1–11.5.2.17, 11.6, 11.7, 11.8) is quoted from V3.2.1 and must be re-checked against the version in force on the report date.
- Exact clause lettering of the Revised 508 Standards provisions cited (E205.4, E207.2's four-SC non-web-software exemption, E101.2 Equivalent Facilitation) — the substance is stable but the lettering should be verified against the current Access Board text.
- Publication date of the WCAG 2.2-era WCAG2ICT Group Note; cite the version actually used by URL and date in the ACR's Evaluation Methods field.
- ADA Title II final rule specifics — 89 FR 31320, 24 April 2024, and the 24 April 2026 / 26 April 2027 compliance dates and 50,000-population threshold — should be confirmed against the Federal Register before being quoted anywhere in the report.
- Current maintenance state and packaging of java-atk-wrapper (libatk-wrapper-java-jni) on major Linux distributions; the Linux Orca story depends entirely on it and it is thinly maintained. Do not promise Orca support before testing it.
- Third-party ACR/audit pricing ($5k–$25k) and the "ACR no older than 1–2 years" procurement convention are recollected industry practice, not sourced figures — get real quotes if that path is considered.
- Maintainer decision: does the maintainer have access to a Windows machine, a Mac, and a GNOME session with real AT? If not, the recommendation is to publish ACCESSIBILITY.md (one day) instead of a half-audited ACR.
- Maintainer decision: close the WCAG 2.5.7 dragging gap with new click-to-pick/click-to-drop interaction modes (~1.5 days) or disclose it as Partially Supports — a keyboard alternative does not satisfy 2.5.7.
- Whether EN 301 549 clause 5.9 (Simultaneous User Actions) is satisfied — Shift+click multi-select and space-drag panning need confirmed sequential alternatives; I could not establish whether multi-select has a keyboard-only path beyond Ctrl+A.
- Recolouring Theme.DEFAULT is a joint optimisation against ThemeTest's existing ΔE≥25 colour-vision constraint and the new ≥3:1 contrast floor; the suggested substitutions (#D55E00, #009E73) satisfy contrast but the ΔE leg must be re-run, and no replacement for the lavender highlight was derived.
- A new GitHub issue must be opened to own this work. Note the namespace collision: docs/standards-landscape.md registry entries #209–#215/#256–#258 are not GitHub issues, and GitHub issue #210 is the component-naming scheme while registry #210 is EN 301 549.

### 04-tool-qualification-and-scope — Safety-critical scope statement, and what tool qualification would actually require (#223-#236)

*Verdict: do-it · 1.5-3 maintainer-days*

- Cost and elapsed time for a TUV SUD / TUV Rheinland / exida tool assessment: no published fee schedule exists and I refused to invent a number. Anyone planning this must get three written quotes against a defined scope; the section says so explicitly rather than giving a placeholder figure.
- DO-254 clause 11.4 tool assessment and the FAA/EASA guidance endorsing DO-330 in that context (AC 20-152A, EASA CM-SWCEH-002) - current revisions and precise applicability are unverified and must be read in the primary documents before citation.
- Whether EN 50716:2023 consolidates/supersedes EN 50128 and EN 50657 - unverified. The drafted statement names both EN 50128 and EN 50716 so it is correct either way, but the landscape survey entry #230 should be checked.
- Exact table numbering and per-ASIL method recommendations inside ISO 26262-8 clause 11, and the sub-clause numbers under IEC 61508-3 7.4.4 - the mechanisms are stated confidently, the numbering is not re-verified against the standards.
- Maintainer decision: whether test assertion 7 (pinning docs/standards-landscape.md so it can no longer say the statement is 'currently absent from' README/SECURITY, line 551) is wanted - it couples two otherwise independent documents. Marked optional in the section.
- Maintainer decision: whether 'do not use JLS as the design-entry tool of record for...' in the drafted section 2 reads as advice or as a use restriction. If a reviewer thinks it is too close to a GPLv3 section 7 further restriction, weaken to 'JLS is not suitable as...'.
- Issue number to file for this work, and the numbering collision it must call out: repo issue #223 is the extension-points catalog already recorded in ARCHITECTURE.md, while landscape #223 is ISO 26262-8. The issue body must say 'landscape #223'.
- Whether the in-jar help tree and the About dialog should eventually carry the statement. The section recommends against both now (help-page orphan rule at HelpTopicsTest.java:317, Map.jhm + TOC cost, HTML 3.2 discipline) but that is a judgement call the maintainer may reverse if a GUI user asks.

### 05-riscv-compliance — RISC-V architectural compliance: riscv-arch-test / RISCOF, and the RVI certification program (#65, #259)

*Verdict: do-it-if · 8-12 maintainer-days*

- RVI cost, elapsed time, membership prerequisite, validity period, and renewal burden for a 'RISC-V Compatible' listing: unverified — riscv.org brand guidelines and both RVI Confluence pages returned HTTP 403 to automated fetch.
- Whether a structural gate-level model living inside a teaching logic simulator is an eligible DUT for an RVI listing at all (RVI's language is 'RTL or a functional model') — unverified, and moot if the profile route stays closed.
- The exact current test-file inventory of riscv-test-suite/rv32i_m/I/src at the SHA to be pinned; must be enumerated from the checkout rather than from memory (GitHub access for that repo was blocked in this session).
- Whether upstream remains RISCOF or moves to an ACT4-era runner: an 'act4' branch exists in riscv/riscv-arch-test alongside riscv-non-isa/riscv-arch-test main/dev. Pick the framework after reading the current upstream README.
- Whether fence.tso is genuinely mandatory for RVI20U32 (one search result asserts it; not confirmed against the ratified Profiles 1.0 PDF), and whether the CPU's decode-to-NOP handling of FENCE would satisfy fence-01.
- Whether the RISCOF-generated test prologues for the I bucket touch CSRs at all; if RVMODEL_BOOT must write mtvec, the binutils >= 2.38 restriction on CSR mnemonics under -march=rv32i forces raw .word encodings and the tests would need traps the CPU does not have.
- Maintainer decision: is sub-word load/store (lb/lh/lbu/lhu/sb/sh, plus possibly Memory's sync-write attribute from issue #199) in scope now, which unlocks the align buckets for +2-3 days, or deferred as a separate datapath item?
- Maintainer decision: should the offline signature-golden lane block PRs in ci.yml, or does the nightly external-toolchain workflow suffice? The recommendation is the former, since the latter cannot be trusted to stay green.

### 06-fpga-constraint-formats — Vendor pin-constraint emitters: XDC, QSF, LPF (#82) and the board table

*Verdict: do-it-if · 8-10 maintainer-days*

- Whether Lattice Diamond/Synplify flattens bus bits to `sw_0` where nextpnr-ecp5 expects the bracketed `sw[0]` form the shipped PCF emitter already uses — decides the LPF COMP naming and cannot be settled without running Diamond.
- Whether Quartus wants quoted or unquoted `-to sw[0]` for bus bits; a mismatch is silently *ignored* rather than an error, so only a manual Fitter-log read catches it.
- Whether ubuntu-latest packages `nextpnr-ice40`/`nextpnr-ecp5` (and the ECP5 chip database) under those names, and whether the pinned oss-cad-suite bundle on the Windows lane ships nextpnr — both degrade to a test skip if absent, but they decide how much automated evidence actually exists.
- Exact current revisions of AMD UG903, the Intel Quartus Prime Settings File Reference Manual, and the Lattice LPF reference, plus Vivado/Quartus install sizes and Diamond's free-licence ECP5 device coverage — all cited by name but unverified here.
- Which boards are actually wanted. All pin letters/numbers in the playbook's examples (Basys 3, DE10-Lite, ULX3S) are illustrative and must be transcribed from vendor master constraint files at implementation time; the maintainer must also decide whether a board may be documented as supported without an on-hardware acceptance run.
- Whether the `-pins` two-token grammar should ever gain per-binding attributes (pull=up, drive=8). Recommended refusal, but it is a documented-format deviation decision, not a coding one.
- Whether QSF should carry FAMILY/DEVICE/TOP_LEVEL_ENTITY globals (making the file standalone) or pins only (making it purely sourceable) — recommended: emit the globals, but it is untested against a real project until the first Quartus acceptance run.

### 07-waveform-formats — Waveform formats: EVCD and FST (#67, #68)

*Verdict: do-not · 3-5 maintainer-days*

- IEEE 1364-2001 clause-18 subclause numbering (four-state vs extended VCD) and the equivalent clause numbers in the current IEEE 1800 revision — both standards are paywalled and were not opened. The repo currently cites '1364-2001 section 18', a superseded document; the successor cross-reference should be added but only with numbers read off purchased text.
- The EVCD port-state character table, exact $var port / $dumpports syntax, and whether the two strength fields are the 0–7 Verilog strength levels. Recollected, not read. Does not change the do-not verdict (the blocker is upstream: JLS has no strength or direction data) but must be settled before anyone attempts an implementation.
- Whether a zlib-only FST — using only the gzip-flavoured section variants and avoiding LZ4/FastLZ entirely — is accepted by current GTKWave and by Surfer/wellen. This is the single load-bearing unknown in any FST cost estimate: if yes the codec question dissolves; if no, FST requires a new shaded dependency (org.lz4:lz4-java) against the project's active-maintenance policy, plus BOM and reproducibility consequences. Answer it against fstapi.h's block-type enum before trusting the 15–25 day range.
- The exact license header on fstapi.c/fstapi.h (commonly described as MIT/permissive). A GPLv3 project can absorb a permissive file but a Java transliteration is a derivative work either way — verify before any vendoring or porting decision.
- Whether GTKWave and Surfer read .vcd.gz transparently, and whether vcd2fst/fst2vcd still ship in current Debian/Ubuntu and Homebrew GTKWave packages. Both are believed true and both are load-bearing: the first for the README/vcd-interop claim, the second for both the FST deferral argument and the proposed interop test. The CI interop arm is designed to convert belief into observation before the docs assert anything.
- Whether any JVM FST writer exists on Maven Central. Searched from knowledge, not from a live query in this environment — redo the search and date it in the recorded decision so the negative is honest.
- Maintainer call: arm the interop test in CI by adding gtkwave to .github/workflows/ci.yml's best-effort apt list (pulls GTK/X dependencies onto the runner, which sits awkwardly with the README's explicit 'X11 is deliberately not part of this project's tooling' stance), or leave it unarmed and run it from a once-per-release manual checklist alongside docs/wayland-desktop-checklist.md. I recommend arming it; either way the choice should be recorded.
- Maintainer call: has any actual user complained about VCD trace size? If not, even the gzip path is speculative feature work and the correct response is a one-line docs note that `gzip out.vcd` exists. Note also that toVcd() materializes the whole dump as a String before writing, so if size ever does become a complaint the first fix is streaming the emitter, not changing container.

### 08-ipxact-export — IEEE 1685 IP-XACT export for subcircuits (#4)

*Verdict: do-it-if · 3-5 maintainer-days*

- The 2022 port nesting (port/wire/direction and wire/vectors/vector/left|right) was verified only against a 1685-2014 sample component; model.xsd for 1685-2022 must be read before the first golden is minted.
- Only ieee-1685-2022/component.xsd and identifier.xsd were confirmed Apache-2.0. The whole include graph (busInterface, model, commonStructures, constraints, generator, subInstances, plus transitive includes) needs a per-file license header check before vendoring into a GPLv3 repo — if any file is not Apache-2.0 the offline schema-validation plan collapses.
- Total size on disk of the 1685-2022 XSD set is unverified; confirm it is acceptable under test/resources/ before committing.
- Whether Kactus2 has a headless/CLI mode usable from a JUnit test, and its binary name, is unverified. If it does not, the external round-trip must be demoted to a manual once-per-release checklist entry in the style of docs/wayland-desktop-checklist.md.
- IEEE 1685-2022 standard text list price is unverified and was deliberately not guessed; also unverified is the exact clause/annex holding the semantic consistency rules the XSD cannot express.
- The 2014/2009 Accellera schema license text was not read directly — only reported second-hand as restrictive. Verify before assuming 2022 is the only vendorable revision.
- Maintainer decision: whether to declare the emitted VLNV scheme (vendor io.github.anadon, library = .jls file stem, name = circuit name unmangled, version = literal 1.0) a stability contract in the same commit that first emits one. Recommended yes, since the fields become a de facto interface immediately.
- Maintainer decision: which of the two demand-gate conditions counts as opened — a named consumer naming their tool and revision, or a bus/register-map feature landing in JLS that gives IP-XACT real content to carry.

### 09-cra-and-supply-chain — EU Cyber Resilience Act stance and supply-chain attestation upgrades (#187, #192, #197, #253, #255)

*Verdict: do-it-if · 5-7 maintainer-days*

- PRIMARY CRA TEXT UNREAD: this environment's agent proxy returns HTTP 403 for eur-lex.europa.eu, digital-strategy.ec.europa.eu, cyberresilienceact.eu, policy.openssf.org and en.wikipedia.org (confirmed via curl "$HTTPS_PROXY/__agentproxy/status"). Every article/recital/date in the section — Art. 3(13)/3(14), Art. 14, Art. 24, recitals 18/19, Annex I Part II point (1), and the dates 11 Sep 2026 and 11 Dec 2027 — is marked [SECONDARY] and must be checked against the OJ text in a browser before the stance is published.
- Is JLS GPL-3.0-only or GPL-3.0-or-later? README.md:347 and pom.xml:21 say v3.0; CONTRIBUTING.md:138 says or-later. I did not read pop_GPLv3.pdf (Poplawski's signed grant) — if that grant is v3-only, the whole tree is GPL-3.0-only and CONTRIBUTING.md is the line that must change. Everything in Steps 2, 4 and the LicenseIdentityTest depends on this decision.
- Copyright-holder provenance for the inherited tree: src/**, resources/help/**, and src/jls/edit/images/*.gif descend from Poplawski's JLS 4.1 and were then modified by this fork. REUSE forces an explicit two-holder annotation and nobody has ever audited which files are inherited, which are fork-authored, and which are generated (scripts/GenerateIcons.java suggests some icons are). This audit, not the tooling, is the day of work in Step 4.
- PS.1 of the SSDF mapping (protect code from unauthorized change) needs the repository's actual GitHub branch-protection settings, which are not visible in the tree. Verify before writing the row; a mapping with an assumed row is worse than one with an honest 'not met'.
- Does anything downstream parse SHA256SUMS positionally? release.yml's verify-windows-signatures job (line 679) and scripts/build-container.sh were not read in full. Adding a bom.spdx.json line is order-insensitive for sha256sum -c but could break a positional consumer.
- Is a schematic editor/simulator on CRA Annex III (important) or Annex IV (critical)? My reading is no — it would be a default-category product on the self-assessment route — but I could not read the annexes. This only matters in the counterfactual where the out-of-scope reading fails, but it is the first question a market-surveillance enquiry would ask.
- Status of EO 14306 (June 2025) and OMB M-26-05 (January 2026) with respect to the CISA Secure Software Development Attestation Form: verified only from law-firm client alerts, not the EO or memorandum text. The recommendation (JLS should not file the form at all) does not depend on it, but landscape entry #255 should be re-checked before anything is written about it.
- ISO/IEC 30111:2019 purchase price not found; ISO/IEC 29147:2018 listed at CHF 179 (secondary source). Search results suggest 29147:2018 may be readable in full on the ISO Online Browsing Platform — worth five minutes to check, since the recommendation is to buy neither.
- Does ISO/IEC 5962:2021 correspond to SPDX 2.2.1 (my knowledge, unverified this pass)? This determines whether the SPDX 2.3 output cyclonedx-cli produces may be described as ISO/IEC 5962 conformant. Recommendation is to claim 'SPDX 2.3 JSON' and avoid the question.

### 10-desktop-and-housekeeping — Packaging and desktop-integration housekeeping: AppStream, XDG base directories, IEC 60027-2 (#165, #174, #175)

*Verdict: do-it-if · 5-8 maintainer-days*

- java.util.prefs backing-store locations on Windows (HKCU\Software\JavaSoft\Prefs\jls) and macOS (~/Library/Preferences/com.apple.java.util.prefs.plist) are taken from the OpenJDK implementation, not observed — the existing windows and macos CI jobs could confirm both in one throwaway step before the UserPrefs migration is written
- Whether java.prefs (and transitively java.xml) currently appears in `jdeps --print-module-deps` for the shaded jar, and therefore how much the jlink image and installer bytes change when java.util.prefs is dropped. The comment at scripts/build-installer.sh:143-144 asserting 'no java.util.prefs usage' is already stale and must be fixed either way.
- Whether the project's license is GPL-3.0-only or GPL-3.0-or-later: no source file carries an 'or later' clause, pom.xml:19-23 says plain v3.0, flake.nix says gpl3Only — the maintainer must confirm before it is written into an AppStream project_license tag.
- Exact appstreamcli behavior: the flag name for suppressing network access (assumed --no-net), and whether a copyleft metadata_license is rejected or merely warned. Check `appstreamcli validate --help` on the runner.
- AppStream spec revision (~1.0) and XDG Base Directory spec revision (0.8) were not re-checked against the publishing pages in this pass.
- Flathub's current submission requirements and review turnaround were not fetched; no number should go into a project doc without checking docs.flathub.org. Moot if the recommended decline is accepted.
- Whether the AppImage's top-level JLS.desktop and the deb's jls-JLS.desktop should be unified or handled by per-format @DESKTOP_ID@ substitution — the playbook recommends substitution to avoid orphaning postinst's fallback .desktop copy on upgrade, but the maintainer may prefer one name everywhere.
- Whether to accept that the rpm ships its metainfo only under /opt (no template.spec override) — the recommendation — or to take on a JDK-version-coupled spec template override.

### 11-costed-rejections — The costed rejections: SDF (#89), EDIF (#74), JEDEC JESD3-C (#83), IEEE 1149.1 BSDL (#129)

*Verdict: do-it-if · 0.5 to record the decisions; 8-12 if JESD3-C phase 1 is triggered (20-30 for both phases), 15-25 if BSDL is triggered maintainer-days*

- JESD3-C field list and both checksum algorithms are stated from secondary sources only — the archive.org full-text mirror returned HTTP 403 to automated fetching. The primary document (free from JEDEC with registration) must be read before any .jed writer code is written.
- All IEEE price figures are unverified: standards.ieee.org, webstore.ansi.org and the reseller pages all returned HTTP 403. IEEE 1149.1-2013 reseller listings ranged $148-$396; IEEE 1497-2001's price could not be retrieved at all. Treat the range, not any number.
- Whether the maintainer has institutional IEEE Xplore access. This is the single decision that flips BSDL from 'defer indefinitely' to 'defer pending a course request' — without it the project cannot honestly claim IEEE 1149.1 conformance at any effort level.
- GAL fuse-map sizes (16V8 = 2194, 22V10 = 5892) came from GALer/GALasm documentation, not from a Microchip/Lattice datasheet. Any device model must cite the datasheet as primary.
- Whether current Debian/Ubuntu urjtag packages still build the bsdl2jtag binary. The entire BSDL testing story depends on it being installable in CI; if it is not, BSDL loses its independent-consumer check and drops to the same unprovable status as EDIF.
- EIA-548/618/682 custodianship after EIA dissolved in 2011, and the exact edifLevel/keywordLevel conformance semantics — neither was verified. Only matters if the EDIF WONTFIX is ever revisited.
- Whether any instructor has actually asked for a PLD lab. This is the sole trigger for the one item with a positive verdict; the survey records no demand and I found none in the tree or issue references.
- Redistribution terms for the Atmel/Microchip ATF150x fitter binaries — unverified, which is why the recommendation scopes device support to GAL22V10 and GAL16V8 only.

## Raised by the grounding pass

The verification agent's own unresolved list, beyond the two blocking items above.

- Every external fact in the playbook remains maintainer-verifiable only. The largest clusters, all now explicitly marked: IEEE 91-1984/91a-1991 and IEC 60617-12 status, edition and price, and whether 60617-12 exists as a citable part or only as a database subscription (01); the OpenSSF criteria counts 67/55/23 and the absence of any fee (02); VPAT 2.5 currency, the harmonised EN 301 549 version, WCAG 2.2 and Revised 508 dates, the 4.1.1-removal rule, and third-party ACR pricing (03); ISO 26262-8 / IEC 61508-3 / DO-330 clause structure and EN 50716 consolidation (04); the entire RVI trademark process, fee, and eligibility, plus whether fence.tso is mandatory for RVI20U32 (05); UG903/Quartus/Diamond revisions and whether Ubuntu packages nextpnr-* (06); IEEE 1364/1800 VCD clause numbers, the EVCD state-character table, fstapi.c's licence, and whether a zlib-only FST is readable (07); IP-XACT 2022 port nesting, the 2014/2009 schema licence text, and Kactus2's headless mode (08); the entire CRA reading — Art. 3(13)/3(14), Art. 14, Art. 24, recitals 18/19, and the 11 Sep 2026 / 11 Dec 2027 dates — none of which was read from the OJ because eur-lex 403'd, plus EO 14306 / OMB M-26-05 status and ISO/IEC 5962 = SPDX 2.2.1 (09); AppStream spec version and appstreamcli's metadata_license behaviour (10); JESD3-C field list and checksums, GAL fuse-map sizes, IEEE 1149.1-2013 price, and Microchip ATF device availability (11).

- Section 02's Sources block cites `docs/other.md`, `docs/api.md`, `app/models/project.rb` and `README.md` 'same repository' — meaning the coreinfrastructure/best-practices-badge repo, not JLS. They sit under a 'Primary (external)' heading so the reading is available, but a reader skimming for in-tree paths could mistake them for JLS files. Left as written; worth a disambiguating prefix if these sections are ever merged into one document.

- Section 03 asserts that every `.msi` from the release pipeline ships a runtime with no Java Access Bridge, because jdeps cannot see `jdk.accessibility`. The reasoning is sound and build-installer.sh:145 is exactly as quoted, but the claim is about a built artifact nobody in this pass ran. It drives the section's highest-priority fix and its 'do not publish an ACR first' gate, so it should be confirmed against a real MSI before the ACR work is scheduled.

- Section 09's PS.1 row ('protect code from unauthorized change') is marked 'verify and cite; I could not confirm the settings from the tree'. GitHub branch-protection settings are not in the repository and cannot be checked from a checkout. This is the one SSDF row that must be answered from the repo settings UI before docs/ssdf-mapping.md is written, and the section is right to refuse to guess.

## Cross-section conflicts found and how they were left

- LICENCE IDENTIFIER — the one hard conflict. Section 09 (Step 0) recommends `GPL-3.0-or-later` on the strength of CONTRIBUTING.md:138; section 10 (step (a)1) recommends `GPL-3.0-only` on the strength of flake.nix:78 `licenses.gpl3Only`, pom.xml:19-23, and the absence of any 'or later' clause in src/. Both propose editing pom.xml's <licenses> block; section 09 additionally proposes a LicenseIdentityTest that asserts one identifier across README/CONTRIBUTING/SECURITY, which would go red against section 10's AppStream <project_license>. Verified in tree: 0 of 886 tracked files carry an SPDX identifier, 0 .java files contain 'GNU General Public', 1 src file (About.java) contains 'Copyright'. Both sections are internally honest; neither is wrong on its own evidence. Marked in both files; NOT resolved by me, because the answer depends on pop_GPLv3.pdf, which no agent read.

- §13 RANKING NUMBERING — sections 01, 02, 03, 04 and 05 all cite `docs/standards-landscape.md` §13 as a single merged ten-item list ('§13 item 1/3/4/5', 'third of ten'). Commit 9ab4797 (2026-07-28 20:58) split it into §13.1 (logic-design) and §13.2 (institutional/project) and says so in the document itself. Those five sections were drafted at 20:45-20:57, before the commit; sections 07, 08, 09, 10, 11 (21:06 onwards) use the current numbering correctly. Every stale citation now resolves either to nothing or, worse, into the §12.i foundry table. Corrected in all five files.

- BATCH-INTERFACE §1 SCOPE — sections 01 and 11 treat any new CLI flag as an edit to `docs/batch-interface.md` §1; section 08 (IP-XACT) correctly reads §1 as covering only the `-b` batch synopsis and deferring to JLSStart.FLAGS. Verified: §1's synopsis line omits `-i`, `-export`, `-board`, `-pins`, `-savetext` entirely. Left unreconciled this would have three sections applying three different stability-contract obligations to the same file. Corrected 01 and 11 to match 08's reading; kept §1/§3 in scope for `-sdf` alone, since that flag would change batch simulation output itself.

- SVG EXPORT ORDERING — section 03 injects `<title>`/`<desc>` into CircuitRenderer.exportImage's SVG; section 01 commits SVG goldens (test/resources/symbols/*.svg) produced by that same path. Compatible but order-dependent: whichever lands second regenerates the other's goldens. Both correctly note SVG is not a documented stability contract, so this is a golden update, not a deviation. Sequencing note added to both.

- PALETTE ICON DIRECTORY — section 01 says the palette GIFs are in `src/jls/edit/images/` (33 files, correct, matches PaletteEntry.java:34); section 03 says `src/jls/images/` (34 files, an unreferenced duplicate). Section 01's icon-rendering proposal and section 03's target-size measurement would have pointed an implementer at two different directories. Corrected in 03.

