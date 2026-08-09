# Issue #772: TASK-C578-3: kit content carries its own open license and an authoring doc a stranger can follow — reviewed by a named external instructor whose feedback is addressed or refused by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the three deliverables apart and only one question is underneath them: **can the
kit convention live outside this repository?** The license and the authoring doc are the
two things that make a convention portable — they answer "may I reuse this?" and "how do
I build one?". The third deliverable, a named external instructor's review, is not a
thing this project can build; it is a validation of the whole of CAP-33 (#517), wearing
the costume of a 0.5–1 mw task's acceptance criterion.

That mismatch is the review. The first two-thirds of the issue is right, cheap, and
mis-ordered. The last third should be disregarded as written; I say why in §4.

## 1. Licensing: don't document a policy, adopt an existing machine-checked one

AC-4 asks for "a stated open license, distinct from the code license." The repository is
greenfield here: exactly one `LICENSE` at root, zero `SPDX-License-Identifier` headers
anywhere in `src/`, no `LICENSES/` directory, no `REUSE.toml`. So the choice of mechanism
is free, and the project's own trajectory already names the right one — CycloneDX
`bom.json` per release, OpenSSF Scorecard, signed provenance attestations, and
`docs/standards-adoption/10-desktop-and-housekeeping.md:48` already reasoning correctly
about `metadata_license` needing to differ from `project_license`. This project speaks
SPDX fluently everywhere except in its own tree.

The concrete alternative: **REUSE 3.x**. A `LICENSES/` directory carrying the license
text, `SPDX-License-Identifier` in every kit text file, and a `REUSE.toml` covering the
binary `.jls` starter circuits that cannot hold a comment. Then AC-4 stops being an
assertion a reviewer must audit and becomes `reuse lint` exiting 0 in CI — and #769's
validator inherits license checking rather than inventing a bespoke field check.

And **pick the license in this issue rather than writing a doc about picking one**. The
issue leaves the actual decision unmade, which guarantees it gets made ad hoc later. The
substantive choice: `CC-BY-SA-4.0` is one-way compatible with `GPL-3.0-or-later`, so kit
prose can be lifted into the in-jar help tree (`resources/help/**`) without a relicensing
problem — a real consideration given ARCHITECTURE.md:252's plan to grow that content.
`CC-BY-4.0` trades that away for the thing instructors actually do: fork a lab into a
university LMS with local modifications and no reciprocal-publication obligation. I would
take `CC-BY-4.0` for the labs and `CC-BY-SA-4.0` only if help-tree reuse is planned.
Either way: one line in the README's "License and provenance" section, decided once.

## 2. The authoring doc already has a house shape — name it

The README's Documentation list establishes a pairing this project uses deliberately:
`docs/batch-interface.md` is normative, `docs/vcd-interop.md` is the *informative recipe*
for the same surface, and its runnable example lives in `examples/autograde/autograde.py`
with a CI test (`test/jls/AutogradeBridgeExampleTest.java`) keeping the example honest.
That is exactly the shape #767 and this issue jointly need: `docs/course-kit.md`
(normative, from #767) plus an informative companion whose worked example is the
Donzellini pack (#575), CI-validated by #769.

The issue says "an authoring doc" with no reference to that family, which is how a fourth
documentation style gets invented. Naming the pairing costs a sentence and settles the
doc's voice, location, and its obligation to carry a runnable example.

## 3. The bigger reframing: make the tool be the doc, and make the subject be *other people's* kits

This is where the issue and its sibling pull against their own outcome. #769 puts the
validator in CI over in-tree kits; this issue's AC-4 says "every in-tree kit". But a
third-party instructor's kit will never be in this tree — that is the entire point of the
convention. A rule enforced only inside the repository that outsiders cannot write to is
not a convention; it is a lint rule with delusions.

The seam this project already knows how to cut: **publish a contract, ship the checker in
the binary, let other people's files be the subject.** That is precisely what
`docs/batch-interface.md` + `-t` + the `ghcr.io/anadon/jls` container do for grading. The
kit equivalent:

- `jls --check-kit <dir>` — #769's validator as a CLI flag alongside `-b`, `-i`,
  `-export`, `-savetext`. A stranger gets it by installing JLS. No checkout, no Maven, no
  CI access, no maintainer.
- `jls --new-kit <dir>` — scaffolds a validating skeleton: manifest with
  `content-license:` pre-filled, a `LICENSES/` directory, one stub lab with vectors.

This makes most of AC-2 disappear rather than satisfying it. "Walks a third-party author
from an empty directory to a validating kit" becomes two commands, and "including how to
state their own content license" becomes generated output with a placeholder they edit —
prose that can go stale replaced by a scaffold that cannot. The remaining doc explains
*why* the parts exist, which is the job prose is actually good at.

## 4. AC-5: I am disregarding this acceptance criterion as written

Three reasons, in ascending order of importance.

**It is single-counterparty with no fallback.** One person is named (via #509). If Dr.
Siever declines, goes quiet, or is mid-semester, the task cannot close and #578 stalls
behind it. Nothing in the issue says what happens then.

**It spends the project's scarcest asset on the wrong ask.** #509 is the only external
demand signal in the tracker, and its 2026-08-08 ordering-correction comment establishes
the sequence explicitly: the fork-delta audit and the corpus lane are free, need no
counterparty, and exist to make the criteria conversation "a conversation about a
demonstrated artifact instead of a promise." An instructor's attention is
non-renewable — you get roughly one good ask before you become a mailing list. Spending
it on "please read our packaging documentation" *before* his own CSE 260M labs
demonstrably load, simulate and grade on a tagged release here is an inversion of #509's
own reasoning, filed by a task that orders itself after #509 and therefore looks compliant.

**The strong form of this criterion is authorship, not review.** The evidence that a
convention is adoptable is a kit built by someone who did not design it — not an opinion
about the doc that describes it. #577's CSE 260M kit *is* that artifact. If it exists,
AC-5 is redundant; if it does not, AC-5's review is a weak proxy standing in for it.

Replacement criterion, same evidence, falsifiable, no named person:

> A kit authored entirely outside this repository, by someone who is not a maintainer,
> validates against a released JLS build; every point at which the author got stuck is
> recorded, with each item addressed or refused by name and each refusal carrying its
> reason.

This can be discharged by Siever, by a TA, by a student, or by an unrelated instructor,
and it measures behavior rather than sentiment. **Keep the "addressed or refused by name,
refusal carries its reason" discipline verbatim** — it is the best sentence in the issue,
it is what stops external feedback from being decorative, and it is already the same
vocabulary as #509 AC-2's "already-fixed-here / port / decline-with-reason". Those two
should be one project-level rule, not two local ones.

## 5. Ordering inversion worth fixing today

`ordering_after: ["TASK-C578-2", 509]` is backwards for the license half. #767's manifest
is required to carry "its content license" — so the license decision is a *dependency* of
the task two positions ahead of this one, and this issue orders itself two positions
behind it. Choosing `CC-BY-4.0`, adding `LICENSES/`, and writing one README line is
roughly 0.1 mw, needs neither the validator nor the adoption relationship, and unblocks
both #767 and #769. Split it out and do it first.

## 6. The arc-level risk this issue makes visible

CAP-33 self-describes as "substantially a NON-CODE capstone." JLS's distinctive
competence, as evidenced by ARCHITECTURE.md's recorded decisions, the normative spec
family, and byte-reproducible builds, is **contracts** — durable, machine-checked,
carrying revisit triggers. Course content has the opposite maintenance profile: it rots
against textbook editions, it needs pedagogy review the single maintainer cannot
self-supply (KC-33-2 concedes exactly this), and it draws from the same finite attention
as the simulator.

The framing that keeps the arc intact: **JLS owns the kit contract and the checker, and
does not become a content publisher.** One reference kit in tree, permanently — the
Donzellini pack — and every other kit out of tree, merely validated. Concretely that
rewrites AC-4 from "every in-tree kit" (a set the project has committed to growing) to
"the reference kit, plus a manifest field every kit must carry" (a set that stays at
one). This belongs in ARCHITECTURE.md §Recorded decisions in the house style, with a
revisit trigger, because "kits accumulate in tree" is what happens by default when nobody
writes down that they shouldn't.

## Disposition

Endorse the license and the authoring doc; do them sooner, smaller, and with existing
mechanisms (REUSE/SPDX, the normative+informative doc pair, a `--new-kit` scaffold).
Disregard AC-5 as written and replace it with the outside-authorship criterion above,
keeping its named-disposition discipline. Reserve the one external ask for #509's corpus
and criteria conversation, where it converts into adoption rather than into commentary.
