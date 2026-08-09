# Issue #765: TASK-C577-3: content licensing is settled in writing before any adapted CSE 260M material ships, and the adapted kit conforms to the convention and runs the cohort workflow
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of its packaging, #765 asks for one thing: **make the first course kit real
rather than hypothetical.** Everything else — the licensing agreement, the conformance
check against #578, the walk through #576 — is instrumentation for that claim. CAP-33
(#517) says so outright: PF-3 is "the demo slice ... the only course kit backed by a
real course, a named instructor and a stated conditional interest rather than a
hypothesis."

The route this issue picks to get there is: **we take Dr. Siever's course material, we
adapt it, we relicense-negotiate it, and we ship it out of anadon/JLS.** That is the
step I want to challenge, because there is a route to the same end that is strictly
better on every axis the project cares about — and it makes the hardest acceptance
criterion in the issue evaporate rather than be satisfied.

## The reframing: ship the mechanism, not the content

Invert the direction of the artifact flow. Instead of JLS acquiring rights to
redistribute CSE 260M material, **Dr. Siever publishes his own kit, from his own repo,
under his own license, and JLS validates it.**

Concretely:

- #578's kit convention grows one thing it does not currently have: a **kit manifest**
  (`kit.toml` / `kit.json`) with a mandatory `content-license` SPDX field, an `author`
  field, and — this is the load-bearing addition — the ability for a kit to be
  *referenced* rather than *vendored*: name, upstream URL, pinned commit/tag, sha256.
- `jls --kit validate <dir>` (or a small `scripts/kit-validate`) is the #578 AC-2
  validator. It runs over any kit directory, wherever it came from.
- The JLS tree carries a **kit registry** — one flat `docs/kits.md` or `kits.toml`
  listing known kits: name, author, license, URL, last-validated version. Zero content,
  zero licensing exposure, near-zero maintenance.
- The first entry in that registry is `bsiever/cse260m-jls-kit`, authored by the person
  who wrote the labs, licensed by the person who owns them.

What this buys, measured against the project's own goals:

1. **#578 AC-5 ("a named external instructor reviews the kit and the authoring doc")
   stops being a review and becomes a *use*.** An instructor who successfully authors a
   conforming kit from the doc alone is a hundred times stronger evidence for
   FEAT-C33-4's thesis — "an instructor outside this project authors and publishes one
   without asking a maintainer how" — than the same instructor reading our adaptation of
   his own labs and saying it looks fine. #765 as written has us author the kit and then
   ask him to grade our homework. The reframing has him do the thing the capstone claims
   is possible.
2. **AC-1 disappears.** There is no redistribution, so there is no content-licensing
   agreement to negotiate, no signature to wait on, no "held" state to record. The
   unschedulable external dependency is removed from the critical path instead of being
   ceremonially documented.
3. **It matches the grain of the tree.** Look at what this repository is good at:
   `docs/batch-interface.md`, `docs/file-format.md`, `docs/simulation-semantics.md` —
   normative specs with validators and CI ratchets behind them; reproducible jars,
   CycloneDX BOMs, cosign signatures, provenance attestations. This project's whole
   demonstrated competence is *publishing contracts and enforcing them*, not curating
   content. The tree today holds four fixtures and one example script. CAP-33 is
   self-described as "substantially a NON-CODE capstone," and #765 is the point where
   that pulls hardest against the established grain. The registry-plus-validator shape
   converts CAP-33's content ambition back into the project's actual strength: JLS
   becomes the package manager, not the packages.
4. **It survives the maintainer.** A vendored course kit rots the moment the instructor
   revises his labs and nobody upstreams the change. A pinned reference to his repo
   tracks reality by construction, and a stale pin is a visible CI signal rather than
   invisible drift.

I am explicitly disregarding acceptance criterion 1 (the written licensing agreement as
a gate) and the premise of criterion 2 (that the shipped kit is ours to ship). Criteria
3 and 4 survive, reworded: the *referenced* kit is walked end to end through #576's
distribute → collect → grade cycle in CI, and content licensing is machine-checked
rather than asserted in prose.

## Second reframing: content licensing is a repo-wide property, not a per-kit note

AC-4 — "content licensing is kept distinct from code licensing, and both are stated" —
is the right instinct implemented at the wrong altitude. Stated *where*? By whom?
Checked how? As written this yields a paragraph in a README that the next kit's author
will not read.

The project already has the machinery queued: **issue #171 (SPDX identifiers / REUSE)**
appears in `docs/standards-landscape.md:456` and `docs/standards-adoption/10-desktop-and-housekeeping.md:274`
as a COULD. Promote it here and the problem inverts: every file in the tree carries an
SPDX tag, `REUSE.toml` declares the defaults, `reuse lint` runs in CI, and the kit
validator's `content-license` check is one more consumer of the same fact. "Both are
stated" becomes a build gate rather than a sentence. That also generalizes to every
future kit for free, which the per-kit prose does not.

There is a prerequisite the issue does not see. **The project's own code license
identifier is currently in dispute across four files** —
`docs/standards-adoption/10-desktop-and-housekeeping.md:259-274` records that §10 wants
`GPL-3.0-only` (matching `flake.nix` and `pom.xml`) while §09 wants `GPL-3.0-or-later`
(matching `CONTRIBUTING.md:138` and `README.md:347`), blocked on reading
`pop_GPLv3.pdf`. You cannot honestly satisfy "both are stated" while one of the two is
three different strings depending on which file you open. That decision is cheap, is
already scoped, and should land before any kit makes a licensing claim adjacent to it.

## The assumption underneath #761 that this issue inherits

#761 asserts that compatibility fixtures are "the half that requires no
content-licensing agreement." That is doing a great deal of unexamined work. Committing
another author's lab circuits into a GPL-3.0 repository is redistribution whatever
directory they land in; `test/fixtures/` is not a legal category. The split #577/#761/
#765 draws — fixtures free, kit gated — may be exactly backwards in risk terms, since
the fixtures land *first* and *without* a conversation.

The same reframing dissolves this half too, and does it in the project's existing
idiom: a **corpus manifest** of upstream URLs plus sha256 pins, fetched in CI, with the
hashes recorded in tree. This repo already reasons fluently in pinned digests and
attestations (`SHA256SUMS`, `.buildinfo`, cosign, `gh attestation verify`). A pinned
corpus manifest is more faithful to #763's actual claim — "grades identically to the
origin fork" — than a snapshot copy, because the pin names the exact origin revision
being compared against. The real cost is honest and should be stated: a network fetch in
CI is a flaky dependency and upstream deletion is a real failure mode, so the lane needs
a cache and a clear skip-with-signal behavior. That is a smaller, more tractable problem
than the one the vendoring route creates.

## Where the issue is right

The insistence that a held kit is "a legitimate outcome recorded rather than a blocker
worked around" is the correct instinct and should survive any reframing. So should the
distinction between #765's artifacts and #509's relationship — that boundary is drawn
cleanly and the fleet should not blur it. My objection is not that the issue is careless;
it is that it accepted "we adapt and ship their labs" as the only shape a first kit can
take, and then did rigorous work inside that shape.

## Concrete recommendation

1. Retitle and rescope #765 to **"the kit convention supports externally authored,
   externally licensed kits, and one is registered and validated end to end."**
2. Add the manifest fields (`content-license`, `author`, `upstream`, `rev`, `sha256`)
   and the reference-vs-vendor distinction to #578 before it freezes its convention —
   this is the seam, and it is cheap now and expensive later.
3. Move the licensing conversation with Dr. Siever wholly onto #509, where the
   relationship already lives, and reframe it from "may we redistribute your labs" to
   "would you publish a kit — here is the doc and the validator."
4. Promote #171 (SPDX/REUSE) as the mechanism for AC-4, and settle the
   `GPL-3.0-only` vs `-or-later` identifier first.
5. Keep #517 PF-4's Donzellini-mapped pack as the one canonical in-tree kit — original
   content, our license, no entanglement. One worked instance we own plus a registry of
   instances we do not is the durable configuration.

If the maintainer rejects the inversion and wants a vendored kit, the issue as written
is coherent and the acceptance criteria are checkable. But it will have spent the
project's single strongest external relationship on obtaining permission to do the less
valuable of the two available things.
