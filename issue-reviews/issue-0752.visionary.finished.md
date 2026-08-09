# Issue #752: TASK-C575-5: the pack's provenance is auditable — original prose and circuits only, under a stated kit content license, with the DEEDS boundary recorded in writing
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two different things are bundled under the word "provenance":

1. **A defensive posture toward one rightsholder.** The pack's selling point is that it
   tracks the Donzellini/Springer text that DEEDS instructors already teach from
   (#517 CAP-33: "the course must port even though the files cannot"). The nearer the
   pack sits to that book, the more the boundary between *maps to chapter 7* and
   *reproduces chapter 7* matters.
2. **Machine-checkable licensing metadata for shipped content** — per-file copyright and
   license records, a content license distinct from the code license, and a CI check.

Concern 1 is genuinely specific to this pack. Concern 2 is not specific to anything: it is
the tree-wide licensing work the project has already costed, and it belongs on those rails.

## The reinvention: AC-3 and AC-4 are REUSE, hand-rolled and scoped to one directory

`docs/standards-adoption/09-cra-and-supply-chain.md` (Step 4, landscape entry #171)
already specifies exactly the mechanism AC-4 describes: `REUSE.toml` +
`LICENSES/<id>.txt` + a separate `licensing` CI job running a pinned `reuse lint`, with
`api.reuse.software` as live third-party verification. That doc's own sizing says the
expensive part is not the tooling but **the copyright audit across the Poplawski
inheritance** — 529 Java files whose two-holder provenance nobody has ever established
(`docs/standards-adoption/OPEN-QUESTIONS.md:155`), plus the `test/resources/hdl` golden
landmine and `CONTRIBUTING.md:38`'s anti-churn rule.

**The lab pack is greenfield.** Every file is new, single-authored, with no inherited
holder and no golden-byte constraint. It is the cheapest possible first population for
REUSE in this tree, and per-file SPDX headers are *free* here — no churn, no `git blame`
damage, no sidecars for binaries (a `.jls` starter is text-or-XZ; the plain-text save
mode from `-savetext` makes headers viable even there).

Mapped onto REUSE, three of this issue's four criteria stop being deliverables and become
consequences:

| AC | Bespoke form | REUSE form |
|---|---|---|
| AC-1 per-lab original-work + named author | a prose provenance file | `SPDX-FileCopyrightText: 2026 <author>` per file |
| AC-3 kit content license distinct from code | a statement in the pack | a different `SPDX-License-Identifier` under the kit path + `LICENSES/CC-BY-4.0.txt` |
| AC-4 a check for unrecorded third-party content | a new pack-local script | `reuse lint` exit 0, plus a public badge that goes red on its own |

There is no CI `licensing` job today (`.github/workflows/ci.yml` has 13 jobs; none is it).
Standing one up for the pack costs an afternoon, is reusable by #578's validator, and
starts a supply-chain claim the project already wants. Writing a pack-local provenance
format instead produces a second, weaker, unbadged mechanism that #578's validator ("runs
in CI over every shipped kit") will later have to subsume or contradict.

**Concrete license recommendation, which belongs on #578 once rather than per-pack:**
`CC-BY-4.0` for prose, and something code-shaped (`CC0-1.0` or `MIT`) for starter `.jls`
circuits and `-t` vector files. A starter circuit is functional and instructors will fork
it into private course materials; `CC-BY-SA` on it would infect their pack — precisely the
adoption friction CAP-33 exists to remove. SPDX makes that split expressible per file;
prose cannot express it without ambiguity.

## The reframing that makes the DEEDS problem mostly disappear

AC-2 pairs a written boundary statement with "no `.pbs` file in the tree". The grep is
near-worthless as assurance — nobody commits a `.pbs` by accident, and the real exposure
is paraphrased prose, echoed exercise numbering, and figure-shaped circuits, none of which
a file-extension check sees. A prose boundary statement is likewise a description of
discipline, not the discipline itself.

**Make the mapping data, not content.** Author each lab topic-first and
textbook-independent — "4-bit ripple-carry adder", "Moore FSM traffic controller" — and
keep the chapter correspondence in a separate table:

```
kits/donzellini/mappings/donzellini-springer-3e.toml
  [[map]] chapter = 7  labs = ["adder-ripple-4", "adder-cla-4"]
```

Four things follow at no extra cost:

- **The boundary becomes structural.** If no lab ever quotes a chapter title, an exercise
  number, or a figure, "reproduces chapter 7" is not a thing a reviewer must police; it is
  a shape the authoring template cannot produce. Chapter-number-to-topic correspondence is
  a fact about a book, not expression from it.
- **The blast radius collapses.** If a rightsholder ever objects, the remedy is deleting
  one 40-line table, not the pack.
- **The audience multiplies.** A second mapping file for Harris & Harris, Mano, or Nelson
  costs an hour and reaches instructors who never used DEEDS. #517's thesis is "adopt a
  course, not a tool"; binding the only pack to one syllabus narrows that to one syllabus.
  The mapping seam is the same work and generalizes.
- **#575 AC-3 ("each lab declares the chapter it maps to") is satisfied by lookup**, and
  the pack keeps working when the text reaches a new edition and renumbers.

This is the seam I would cut along, and it lands in #744 (which defines the lab format)
rather than here — which is the point: a boundary enforced by format costs nothing later.

## A gap the issue does not consider: inbound content terms

AC-1 "names the author" and assumes the author can license the work. Two ways that fails,
both live for this project:

- **`CONTRIBUTING.md:139` says every contribution comes in under GPL-3.0-or-later.** There
  is no inbound path for CC-BY content. A pack shipping under a content license the
  contribution agreement does not admit is a contradiction that will surface with the
  first outside lab — and #578 AC-4 explicitly aims an authoring doc at third-party kit
  authors, making that certain rather than possible. The pack cannot state an outbound
  content license without CONTRIBUTING.md stating the inbound one.
- **University ownership of instructor-authored material.** This repository exists in its
  current form because MTU had to consent in writing (`pop_GPLv3.pdf`, README "License and
  provenance"). #577 AC-3 carries that discipline for Dr. Siever's corpus. A lab authored
  by any university employee needs the same slot, and "names the author" does not provide
  one. The record should name the *holder*, and cite the grant where holder ≠ author.

## What I would keep, drop, and move

- **Keep and sharpen:** the DEEDS boundary note (AC-2's first half) — reframed as the
  authoring rule above plus a short rationale, so it is a constraint on how labs are
  written, not an assertion about labs already written.
- **Drop:** the `.pbs` grep, and the bespoke per-pack check. Replace with `reuse lint`.
- **Move to #578:** AC-3 and AC-4. #578 already owns "the content license distinct from
  the code license" (its AC-4) and "a validator … CI runs it over every shipped kit" (its
  AC-2). Restating both here creates two owners for one rule; the pack-side obligation is
  only *conform and be the first subject*.
- **Add:** the inbound-terms criterion — CONTRIBUTING.md states content terms, and the
  provenance record names the rights holder with a grant citation where it is not the
  author.

So reframed, this task shrinks well below its 0.25–0.5 mw band, and the difference buys
the mapping-table generalization that turns a DEEDS-refugee pack into a textbook-agnostic
one. I endorse the outcome; I am disregarding AC-2's asset-absence check and AC-4's
bespoke check as the wrong mechanisms for it.
