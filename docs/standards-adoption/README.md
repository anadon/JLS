# JLS standards-adoption playbook

`docs/standards-landscape.md` surveyed 304 entries and ranked the plausible
subset. This playbook takes the eleven items that survived that ranking and
prices them. The survey answers *what exists*; this answers *what to do on
Monday, and what to write down instead*.

## Sections

| File | Item |
|---|---|
| [`01-iec-ieee-symbols.md`](01-iec-ieee-symbols.md) | IEEE 91/91a-1991 and IEC 60617-12 schematic symbol conformance |
| [`02-openssf-badge.md`](02-openssf-badge.md) | OpenSSF Best Practices Badge — passing, silver, gold |
| [`03-accessibility-conformance.md`](03-accessibility-conformance.md) | VPAT/ACR, Section 508, EN 301 549, WCAG 2.2 |
| [`04-tool-qualification-and-scope.md`](04-tool-qualification-and-scope.md) | Safety-critical scope statement; ISO 26262 / IEC 61508 / DO-330 costed |
| [`05-riscv-compliance.md`](05-riscv-compliance.md) | riscv-arch-test / RISCOF and the RVI certification program |
| [`06-fpga-constraint-formats.md`](06-fpga-constraint-formats.md) | XDC, QSF, LPF pin-constraint emitters |
| [`07-waveform-formats.md`](07-waveform-formats.md) | EVCD and FST |
| [`08-ipxact-export.md`](08-ipxact-export.md) | IEEE 1685 IP-XACT export for subcircuits |
| [`09-cra-and-supply-chain.md`](09-cra-and-supply-chain.md) | EU CRA stance, SPDX SBOM, REUSE, SSDF, disclosure policy |
| [`10-desktop-and-housekeeping.md`](10-desktop-and-housekeeping.md) | AppStream, XDG base directories, IEC 60027-2 |
| [`11-costed-rejections.md`](11-costed-rejections.md) | SDF, EDIF, JEDEC JESD3-C, IEEE 1149.1 BSDL |

Every unverified claim across these twelve files is collected as a checklist in
[`OPEN-QUESTIONS.md`](OPEN-QUESTIONS.md) — 92 items the authors flagged, plus the
cross-section conflicts the grounding pass found and how each was left.

## Two things to read before acting

**One hard blocker, since resolved.** The licence-identifier conflict below was
answered after this playbook was written: the grant document was read, and the
project elected `GPL-3.0-or-later`. The paragraph is kept because the *reasoning*
still applies to any future identifier question, and because sections 09 and 10
still carry blockquotes describing the conflict as live. See
[`OPEN-QUESTIONS.md`](OPEN-QUESTIONS.md) for the resolution.

**The original blocker, for the record.** Sections 09 and 10 recommend *different*
SPDX licence identifiers — `GPL-3.0-or-later` (on `CONTRIBUTING.md`'s wording)
versus `GPL-3.0-only` (on `flake.nix`'s `licenses.gpl3Only`, `pom.xml`, and the
absence of any "or later" clause in `src/`). Both are internally honest on
their own evidence. The answer depends on whether David Poplawski's grant in
`pop_GPLv3.pdf` is version-3-only or version-3-or-later, which nobody in this
pass read. **No section's licence-identifier work should start until that
question is answered**, because `pom.xml`, `flake.nix`, `README.md`,
`CONTRIBUTING.md`, the AppStream `<project_license>`, and any `REUSE.toml` all
have to agree.

**Work order is not a relevance order.** The table below runs cheapest and
unconditional first so that an early-ending quarter still leaves readable
artifacts behind. That is deliberately *not* the same axis as
`docs/standards-landscape.md` §13, which separates §13.1 (conformance in the
logic-design space) from §13.2 (conformance the project owes as distributed
software). The OpenSSF badge sits second here on cost; it sits in §13.2 on
relevance, and it has no bearing on logic design whatever. Read both orderings
together, and do not let cheapness be mistaken for importance.

## How to read this playbook

Each numbered section takes one item and runs it through the same five
questions: what a conformance claim would actually assert (and what it would
not), the implementation procedure against this tree, how the claim gets
tested, who — if anyone — assesses it, and what it costs in maintainer-days
including the ways it goes wrong. The verdict vocabulary is four words.
**do-it** means the work stands on its own and needs no external
precondition. **do-it-if** means the engineering is sound and fully specified
but is gated on a named condition — a user asks, a course commits, a vendor
tool is installable, a document is purchased — and if the gate is shut the
correct action is zero lines of code plus a recorded decision. **defer** means
specified now, built later against a named revisit trigger; no section carries
a bare `defer` verdict, because in every case the deferral is expressed as a
gate inside a `do-it-if` section (the IP-XACT demand gate, the BSDL trigger)
rather than as a standalone posture. **do-not** means do not build it, and the
deliverable is a written negative rather than silence. The word
*certification* is doing at least four jobs across these sections and they are
not interchangeable: **self-assertion**, where nobody assesses anything and
the claim's whole value is the evidence published beside it (most of this
playbook); **a registry**, where a third party hosts or computes your claim
but does not review its substance (the OpenSSF badge entry, the REUSE badge,
the RISC-V arch-test-reports repository); **an accredited body**, where a paid
assessor issues a certificate against a scheme (TÜV et al. for ISO 26262 /
IEC 61508 — the only real assessor anywhere in the landscape, and the one
route this playbook declines outright); and **a regulator**, where the
assessment is legal rather than technical and reactive rather than applied-for
(CRA market surveillance authorities, the CISA attestation form, FAA/EASA
under DO-330). Read "who assesses it" in the table below as the load-bearing
column. Where it says *nobody*, the section's testing procedure is the only
thing standing between a claim and a lie.

## The adoption table

Ordered by recommended work order, not by section number.

| # | Item | Verdict | Effort (maintainer-days) | Who assesses | Why |
|---|---|---|---|---|---|
| 1 | `04-tool-qualification-and-scope` — safety-critical scope statement | **do-it** | 1.5–3 | nobody (self) | Cheapest item here, removes a live liability ambiguity, and is the artifact that lets every future safety enquiry be answered with a URL. |
| 2 | `02-openssf-badge` — OpenSSF Best Practices, passing | **do-it** | 3–5 | registry (self-certified entry, publicly readable) | The tree already satisfies the criteria; the work is 67 honest answers plus the drift automation that keeps them true. Gold is closed by headcount, permanently. |
| 3 | `11-costed-rejections` — SDF, EDIF, JESD3-C, BSDL | do-it-if | 0.5 to record; 8–12 if JESD3-C phase 1 fires (20–30 for both phases), 15–25 if BSDL fires | nobody (self) | A rejection with no price on it is a shrug. Half a day converts four "never got to it" items into four decisions with reopening triggers. |
| 4 | `07-waveform-formats` — EVCD and FST | **do-not** | 3–5 (of which ~1 is the two recorded negatives; the rest is the gzip substitute) | nobody (self) | EVCD needs a strength model JLS does not have; FST's specification is one C file. Ship compression on the existing VCD writer and close both. |
| 5 | `09-cra-and-supply-chain` — CRA stance, SPDX SBOM, REUSE, SSDF, disclosure policy | do-it-if | 5–7 | mostly self; REUSE is computed by a third-party service; regulator route (CISA form / CE marking) explicitly declined | The stance is only credible shipped alongside the artifacts. Gated on no monetisation being under consideration, which would move it out of a maintainer's hands. |
| 6 | `03-accessibility-conformance` — VPAT/ACR, 508, EN 301 549, WCAG 2.2 | do-it-if | 13–22 (minimum viable ACR: 8) | nobody (self); read by university procurement | The certification most likely to be *asked for*. Gated on real AT access across three platforms — a half-audited ACR is worse than none. |
| 7 | `10-desktop-and-housekeeping` — AppStream, XDG base dirs, IEC 60027-2 | do-it-if | 5–8 | nobody (self); `appstreamcli` is a linter, Flathub is a publication review and is declined | Two worth doing in reduced form, one worth declining. Gated on not displacing items 1–6, which the survey ranks above it. |
| 8 | `05-riscv-compliance` — riscv-arch-test / RISCOF | do-it-if | 8–12 | self for the test-pass claim; registry (RVI trademark listing) unattainable and declined | The only item that would let JLS make a conformance claim about a *design*. Gated on owning a pinned external-toolchain lane forever. |
| 9 | `06-fpga-constraint-formats` — XDC, QSF, LPF | do-it-if | 8–10 | nobody (self); the vendor tool is the oracle, via a manual per-release acceptance record | Small printers over an existing port walk, but each one buys a permanent per-release checklist. Gated on a named board a named user owns. |
| 10 | `01-iec-ieee-symbols` — IEEE 91/91a, IEC 60617-12 | do-it-if | 8–12 | nobody (self) | The only entry where JLS goes from "draws something gate-shaped" to "conforms to the symbol standard". Gated hard on buying the primary documents; without them, build the feature and make no claim. |
| 11 | `08-ipxact-export` — IEEE 1685 for subcircuits | do-it-if | 3–5 | nobody (self) | Structurally free for the ports and structurally empty for everything else. Held behind a demand gate in the same posture as #212. |

## Sequencing and dependencies

`docs/grand-architecture.md` §8 draws a spine because the architecture *has*
one: `#77` is genuinely on the critical path of three futures at once. This
playbook has no such spine, and inventing one would be dishonest. Eight of the
eleven items are mutually independent — `01`, `03`, `05`, `06`, `08`, `10` do
not touch each other at all, and `02` and `11` touch nothing but documents.
What exists instead is one short enabling chain, three pools of shared
machinery, and a set of preconditions that live *inside* individual sections.

```
04 intended-use.md ──┬── 09 step 1 pairs the CRA stance with it (drafting
  (scope statement)  │      already done → cite, don't rewrite)
                     └── 02 badge answers link it as recorded reasoning

09 supply-chain ────────── strengthens 02's contestable badge answers
  (SBOM/SSDF/policy)       (signed_releases, access_continuity); not blocking

shared: HdlExporter.buildModel port walk ── 06, 08, and 11's EDIF/BSDL
        (first mover pays the buildPorts extraction; others inherit it)

shared: scripts/build-installer.sh + release.yml ── 09, 10, and 03 §1
        (all three sit under the installer-reproducibility gate)

shared: recorded-decision machinery ── 11, 07, 05's #259, 08's gate, 10(c)
        (ARCHITECTURE.md + standards-landscape.md §13; batch into one PR)
```

**The one real enabling relationship** is `04` → `09`. The safety-critical
scope statement is drafted inside section 04 and section 09 folds it into the
same half-day as the CRA stance; doing 04 first turns a drafting job into a
citation. Section 02's most contestable badge answers likewise get better
after 09 lands, but the badge does not wait on it.

**Three pools of shared machinery, and the rule for each.** The
`HdlExporter.buildModel` port walk is the common substrate of `06`, `08`, and
the EDIF and BSDL emitters priced in `11` — whichever goes first pays for
extracting the port list into something reusable, and the others get it free.
In practice that is `06`, and the other two are unlikely ever to fire. The
packaging and release scripts are touched by `09` (SPDX SBOM at release), `10`
(AppStream rendering into deb/rpm/AppImage), and `03`'s Access Bridge fix (the
bundled-runtime module set) — all three land under the installer
reproducibility gate, so the rule is *sequence them, never interleave them in
one release*, and never let a wall-clock date reach a rendered file. The
accessibility evidence base — `docs/keyboard-a11y-verification.md`,
`docs/component-naming.md`, the UI harness — is owned entirely by `03` and
shared with nothing; the only coupling between accessibility and the rest of
the playbook is budget.

**Preconditions that are internal, not cross-item.** `03` cannot publish an
ACR before its own Access Bridge fix lands. `02` cannot reach silver until
coverage clears 80 %, which waits on the `#84`/`#91` editor decomposition —
an architectural dependency outside this playbook entirely. `01` wants the
`#77` gate model/render split settled before it starts. `05` should do the
Sail oracle swap (about 2 of its 8–12 days) before the RISCOF plugin, because
that slice delivers most of the verification value on its own. `06` has its
own internal slice order (arm nextpnr against the existing PCF golden, then
LPF, then XDC, then QSF). `10`'s stage 2 must not land in a release where
`jls.collab` is moving. None of these are reasons to reorder the table.

**A sensible order of work**, then, is the table's order and for one reason:
it runs cheapest-and-unconditional first, closes the books on everything the
project has decided against second, and only then spends real days on gated
engineering — so that if the quarter ends early, what shipped is the set of
artifacts an outsider can actually read.

## Total cost and what it buys

| Bucket | Days | Contents |
|---|---|---|
| Unconditional (`do-it`) | **4.5–8** | `04` scope statement, `02` passing badge |
| Closing the books | **3.5–5.5** | `11` four recorded rejections (0.5), `07` two recorded negatives plus the gzip substitute (3–5) |
| Gated (`do-it-if`), if every gate opens | **45–76** | `09` 5–7, `03` 8–22, `10` 5–8, `05` 8–12, `06` 8–10, `01` 8–12, `08` 3–5 |
| **Everything, every gate open** | **53–90** | |
| Realistic near-term | **~20–30** | the two `do-it`s, the recorded decisions, `09`, and `03` at its 8-day minimum-viable ACR |

Against that, the specified-but-declined work in the same eleven sections
comes to roughly **240–440 maintainer-days** — SDF 25–40, EDIF 5–8, JESD3-C
both phases 20–30, BSDL 15–25, an FST writer 15–25, a full JAAPI canvas scene
model 8–15, and a first safety-tool qualification 150–300 — plus a 10–30 day
per-release tax in perpetuity for the qualification route alone, and a silver
coverage program and a permanently unreachable gold badge that no number of
days would buy. The ratio is the point: this playbook's main output is the
several hundred days it declines with reasons attached.

**What the recommended set changes.** A university procurement office can, for
the first time, answer its own questionnaire without emailing the maintainer —
an ACR naming a version and a commit, a security policy structured against
29147/30111, an SBOM in two formats with a reproducibility gate behind it, a
supply-chain badge, and a plain statement of what the tool is and is not
engineered for. An instructor gets a citable answer to "is this safe to assign"
and, if the gated items fire, a symbol mode that matches a European curriculum
and an ISA-compliance result with a pass count and an exclusion list rather
than a claim. A contributor gets a repository where every conformance claim is
pinned by a test that fails the build, and where the standards the project has
*refused* are written down with prices and reopening triggers, so the same
proposals stop arriving.

**What it does not change.** None of it is certification in the sense the EDA
industry means the word: that is a foundry certifying a tool version against a
PDK version under NDA, and it is closed to this project by access, not by
merit. The badge is not a security assurance. The ACR is not a warranty and
does not make the canvas usable by a blind student — that is a different,
larger piece of engineering priced inside section 03. The CRA stance is one
maintainer's reading, not a compliance determination. Nothing here qualifies
JLS for safety-critical use, and section 04 exists precisely to say so
permanently rather than leave it open. And no amount of this work moves JLS
across any of the tier boundaries below; it makes the tool's *own* standing
legible, which is a different thing from extending its reach.

## What is deliberately not in this playbook

- **Tiers 7–9 of the survey — physical implementation, mask and lithography
  data preparation, and fab/test/packaging.** Half the landscape by volume and
  none of it by relevance: those tiers belong to five or six other tool
  classes downstream of design entry, and the most useful thing a
  schematic-first educational simulator can do about them is emit a clean
  netlist and get out of the way.
- **Verification-methodology standards — UVM, PSS, PSL (and SVA with them).**
  Every one of them presupposes a constrained-random or assertion testbench
  language and a simulator that executes it; JLS's verification story is test
  vectors plus differential testing, so adopting any of them would mean
  building a testbench language first and then claiming conformance second.
- **Timing, power, and library formats — SDC, Liberty, UPF/CPF, and SDF.**
  They all describe a technology cell library and a timing engine that JLS
  does not have and has decided not to acquire; SDF is priced explicitly in
  section 11 as a permanent scope boundary, and the rest fail for the same
  reason with no separate argument needed.
- **Safety tool qualification as a goal.** It appears here only as a costed
  scope limit: section 04 walks the ISO 26262 / IEC 61508 / DO-330 routes in
  full, arrives at a number, and then shows the blocker is verification
  independence and a QMS rather than effort — which is why the deliverable is
  a scope statement and not a roadmap item.
- **Foundry and EDA-ecosystem certification (survey §12.i)**, named once for
  completeness: it is the regime that actually gates commercial participation,
  and it is closed to this project by NDA and PDK access rather than by any
  judgment about the tool.
