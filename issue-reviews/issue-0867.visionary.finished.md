# Issue #867: TASK-C590-2: a release-announcement checklist exists in-tree — what the writeup must show, which venues, who posts — so a flare moment is not spent on a bare git tag
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#867 is the smallest possible expression of a real and correct claim about what JLS
should become: **a project whose distribution of evidence is engineered to the same
standard as its distribution of bits.**

The asymmetry is stark and visible in one file. `README.md` spends roughly a hundred
lines on how a stranger obtains and verifies JLS: per-arch installers, SHA256SUMS per
OS/arch, signed build-provenance attestations, keyless cosign on the container, a
CycloneDX BOM, byte-reproducible jar with a `.buildinfo` and an independent-rebuild
recipe, an explicit and reasoned *refusal* to hold a GPG key. That is a supply chain
designed by someone who thinks about how truth reaches a reader. The same repository has
no answer at all to "how does the reader learn there is something to fetch." Releases go
out as `git push --tags` into `release.yml` with `generate_release_notes: true`. Every
gram of rigor is downstream of a discovery event that nothing in the project produces.

CAP-36 (#520) diagnoses this correctly and cheaply: in this niche prominence flows from
published, citable evidence and instructor word-of-mouth, not features, and the evidence
is nearly free because the competitor failures are already catalogued in their own
trackers. #867 is the 0.25 mw slice of that. The goal is right. What I want to change is
the *shape* of the artifact, on three counts.

## 1. The file as specified is an orphan; the runbook it belongs in already half-exists

`docs/release/` does not exist. But four separate documents already write checks against
"the release checklist" as though it does:

- `docs/standards-adoption/09-cra-and-supply-chain.md:642` — "should be added to the
  release checklist rather than pretended into CI"; again at `:787` ("re-read the CRA
  stance").
- `docs/standards-adoption/02-openssf-badge.md:528` — "fold it into the existing release
  checklist".
- `docs/standards-adoption/08-ipxact-export.md:422` and `OPEN-QUESTIONS.md:143` — "a
  manual once-per-release checklist entry in the style of
  `docs/wayland-desktop-checklist.md`".
- `docs/wayland-desktop-checklist.md:1` — the only one that actually shipped, and it
  records its results as a *comment on issue #100*, not in-tree.

So the project has an emergent, unnamed, distributed release procedure whose parts point
at a center that was never built. Landing `docs/release/announcement-checklist.md` as a
standalone leaf adds a fifth orphan to that set.

**The better seam:** this task creates `docs/release/README.md` — *the* release runbook,
the thing those four documents are already citing — with the announcement as one gated
section inside it (§"If this is a flare release"). Same word count, same one sitting, but
it gives the Wayland spot-check, the CRA re-read, the OpenSSF re-check and any future
per-release manual step a home to be folded into, and it gives the announcement section
the one property it most needs: it sits directly in the path of someone who is already
cutting a release, rather than in a file they must remember exists. A checklist nobody
opens is failing in the same way #590's comment says a checklist nobody runs is failing.

## 2. AC-3's "flare moment" has a mechanical definition available and the issue does not take it

The adversarial pass is right that AC-3 is self-graded. But the fix is not a rubric in
prose — it is that this tracker already carries the vocabulary. Issues are tiered
`tier:capstone` / `tier:feature` / `tier:task`, and `docs/capability-roadmap/` names
keystones and long-fronts explicitly.

**A flare release is one whose CHANGELOG entry closes a `tier:capstone` issue, or first
ships a capability named as a keystone in `docs/capability-roadmap/`.** That definition
is one sentence, cannot be met by a patch release, cannot be dodged for a real one, and
is checkable by anyone with the tracker open. It also inherits the project's own judgment
about what is significant instead of asking a tired maintainer to re-derive it at 2am.
That single line does more for AC-3 than any amount of "you'll know it when you see it."

## 3. The reframe I actually want: durable placement beats perishable posting

This is where I set aside the issue's framing. AC-1 makes "the named target venues this
niche actually reads" the checklist's center of gravity. But CAP-36's own evidence line
says prominence here flows from **papers and course adoption** — and #590's boundary note
concedes that posting is "an action item, not an outcome we control." A list of link
aggregators is the generic-startup move, it decays within a day, and it is precisely the
part of the plan with the weakest evidence behind it in a repository that otherwise
refuses unevidenced claims.

The high-leverage channel for a tool like this is not a post; it is **being findable in
the places a person already searches when they have the problem** — and those placements
are permanent, compound across every future flare, and mostly cost one PR each:

- The distribution indexes `docs/standards-adoption/10-desktop-and-housekeeping.md`
  already analyses in detail — AppStream metainfo (`:17`), AppImageHub listing (`:74`,
  `:641`), nixpkgs / PPA / distro archives (`:78`) — with Flathub reasoned-down rather
  than ignored (`:81`). That document is a placement strategy that nobody has connected
  to the announcement work.
- Comparison-surface presence: Wikipedia's logic-simulator lists, the awesome-* lists,
  GitHub topic tags, and — once #588 lands — the two comparison notes themselves, which
  are the artifact an instructor googling "Logisim Evolution test vectors sequential"
  will actually hit.
- Package-manager reachability the README already brags about (`nix run
  github:anadon/JLS`, `ghcr.io/anadon/jls`) but which no index points at.

**So: make the checklist two-part.** Part A, *durable placements*, is a standing table —
one row per channel, listed once, each row either done (with a link) or carrying the
issue that owns it; it is reviewed at every flare and mostly already satisfied. Part B,
*the moment*, is the perishable one — the writeup, the artifact, the posts. Part A is
where the compounding is; Part B is where the anxiety is. Today's issue writes only
Part B.

This reframe also dissolves AC-5 rather than answering it. "What to do when a venue's
norms forbid self-promotion" has a clean structural answer once Part A exists: *you do
not post there — you make the thing findable and let someone else post it*, and you
record that choice in the row. The honest answer stops being an apology and becomes the
default strategy.

## Form, not prose — and the tie to #866/#868

AC-4 asks for shortness by exhortation. Make it structural instead: ship the section as a
**fill-in form**, not instructions — blanks for tag, flare justification (which capstone
closed), writeup URL, artifact command, per-venue post URL, competitor-recheck date. A
form is followed under pressure; prose is skimmed. And the *filled* copy is exactly the
ledger row #868 needs, so the three sibling tasks collapse into one mechanism rather than
three prose disciplines: #866 supplies the single-sourced positioning paragraph with its
drift check and link coverage, #867 supplies the form, #868 files the first filled row.
Any competitor claim in the form is a *link into #588's notes*, never a restatement —
which removes the drift risk the adversarial pass flags, because there is only ever one
copy of the claim standard.

## The flare is already in the tree

Consistent with the #868 visionary pass, and worth stating in #867 because #867 owns the
definition: `riscv/` contains a single-cycle RV32I CPU built from stock JLS elements —
no plugin, no special mode — differentially fuzzed against an independent reference
emulator (`fuzz_diff.py`), runnable headless from a signed multi-arch container,
exportable to structural Verilog. That is a flare that already happened and was never
announced. The AC-3 definition should therefore explicitly admit **already-shipped,
never-announced capability milestones**, or the discipline will sit idle waiting on a
Linux boot that `riscv/README.md` shows is architecturally distant (word-granularity
`lw`/`sw`, no CSRs, no MMU).

## Alignment

Endorsed as to purpose: this is the cheapest slice of the project's most under-served
axis, it is non-code, and it pulls with the arc rather than against it. Reframed as to
form: put it in a release runbook the repo is already implicitly citing, define the
trigger off the tracker's own tier labels, split durable placement from perishable
posting, and ship it as a form whose filled copy is the audit record. Do that and #867
stops being a document about a moment and becomes the missing center of a release
procedure that four other documents have been assuming for months.
