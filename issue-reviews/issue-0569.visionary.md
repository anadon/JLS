# Issue #569: FEAT-C30-4: the extension API is published with a stability statement — the plugin story Digital never offered, backed by a jar built outside the repo
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Read through CAP-30 (#514), this is not a documentation issue. It is a **recruitment
instrument**. The bet is that Digital's stranded contributor pool will land somewhere,
and that "we publish an extension API and promise to keep it working" is the
differentiator Digital cannot match. AC-1 and AC-3 are packaging; **AC-2 — a per-seam
stability taxonomy plus a deprecation-notice policy — is the entire product**, and the
adversarial comment of 2026-08-08 is right that nothing else in the tree carries it
(`ModuleManifest.apiVersion` is "major = break, no ranges" — a mechanism, not a promise).

I am accepting that reading and then disagreeing with its scope in two ways.

## Reframing A (primary): this is JLS's compatibility policy, not the plugin API's

The issue proposes to author a stability vocabulary *for the extension seams*. But JLS
already has four independent, mutually unaware stability vocabularies, and this would be
the fifth:

| Surface | Where the promise lives | What it says |
|---|---|---|
| Batch/grading interface | `docs/batch-interface.md` §6 (L324–336) | `-t` grammar, stdout, VCD "frozen as specified"; break = CHANGELOG + major bump **or** compat flag |
| Save format | `docs/file-format.md` L278–284, L480–484 | tags are "frozen identifiers… frozen forever"; `FORMAT` line is the forward-compat valve |
| Extension points | `docs/extension-points.md` L15–17 | *ids* never change once shipped — id stability only, silent on contract types |
| Module SPI | `src/jls/module/ModuleManifest.java:22–28` | single-integer `apiVersion`, major = break |
| Releases | `CHANGELOG.md:5` | semver `MAJOR.MINOR.PATCH` |

Nothing defines "breaking change" once. Nothing defines a notice period anywhere.
`docs/batch-interface.md` §6 is, in substance, already 80% of what #826 wants to write —
authored for a different surface, in different words, with no tiering and no deprecation
window. #826 as scoped will produce a sixth dialect that applies only to a seam set with
zero external consumers.

**The elegant cut is one normative `docs/compatibility.md`** that defines the
frozen / evolving / internal taxonomy and the deprecation-notice policy *once*, over
every published surface JLS has: CLI flags and exit codes, the `-t` grammar, the batch
report format, the VCD profile, the `.jls` grammar and tag table, extension-point ids and
contract types, and — when LF-07 lands — `jls.api` and the stdio protocol. Each existing
normative doc then loses its bespoke promise paragraph and gains a one-line header
("Tier: frozen, per `docs/compatibility.md`"); `docs/extension-points.md` gains a
**Stability** column beside its existing Status column.

This is strictly better for the audience the capstone names. "This project has a
compatibility policy covering everything it publishes" is a far stronger signal to a
developer deciding where to invest than "this project has a plugin doc" — and it is
*cheaper*, because three of the surfaces already have their answers written and just need
to be spoken in one voice. It also makes the tiering honest: the three `pending` rows
(`hdl.importer` #61/#62, `app.command` #84, `gui.theme` #76) fall out naturally as
`internal`/unpublished rather than forcing the "publish as evolving vs. hold" dilemma the
boundary comment's consequence 3 warns about.

## Reframing B: cut along the demand gate, not along the document

#212's Completion Criteria say it plainly: *"built because a named requester asked
(REPLAN records who), or this feature closed/parked… never built speculatively."* The
standing decision from #80 is **hold**. So the dependency chain reads:

- #569 exists to attract outside developers →
- #569 orders after #212 →
- #212 will not be built until an outside developer asks.

**The feature is a precondition for its own precondition.** That circularity is not a
detail; it is why PF-4 has no schedulable start date. There are exactly two honest exits:
either CAP-30 *is* the named requester (in which case someone must write the REPLAN on
#212 recording the capstone as the demand, and the gate is spent on a strategic bet rather
than on a real course), or #569 must be re-cut so that the half which does not need the
mechanism ships first.

Reframing A makes the second exit available, and I think it is the right one. The
compatibility policy plus tier labels over the four typed-now seams (`elem.element-provider`,
`gui.palette-contributor`, `hdl.exporter`, `collab.op-observer`) needs **no** new mechanism
and **no** demand gate — those constants exist today and `ExtensionPointCatalogTest`
already pins them. Only AC-3/AC-4 (walkthrough to a loadable jar, outside builder) need
#212. So: #825 + #826 become an ungated policy feature that can land this quarter, and
#827 rides #212's gate with the outreach that would actually produce a stranger.

## Reframing C: the surface with real consumers is not this one

`docs/capability-roadmap/lf-07-api-and-platform.md` is the strongest document in this repo
on the subject of "who is outside JLS trying to reach in," and it never mentions element
providers. Its five workarounds are all *in-tree, today*: `riscv/jlsbuild.py` (322 lines
re-implementing the save format in Python), `test/jls/CircuitTextBuilder.java` (422 lines,
the project's own tests unable to construct the model without serializing), five regexes in
`riscv/jlsrun.py` over prose output, 193 KB of clock-vector text to say `advanceCycles(2000)`,
one JVM per experiment. AMENDMENT.md L49–56 calls this the sharpest finding of the sweep:
four survey entries "discharged against a capability nobody scoped."

That is the asymmetry worth naming. **The element-provider seam has zero users and a demand
gate. The batch stdout format has a scraper in this repository and is frozen precisely
because of it.** Publishing a stability statement is exactly right; aiming it only at the
seam with no users, while the surface with users is governed by a promise written in a
different doc in a different vocabulary, gets the priority backwards. Reframing A fixes
this without new work: the same policy that labels `elem.element-provider` also labels the
batch report format — and pre-positions the tier vocabulary that `jls.api` will need on day
one, rather than authoring a seventh dialect then.

## One mechanism note

#826 AC-3 wants a committed check pinning frozen-seam signatures. Build it as **one
tier-parameterized signature ratchet**, not a seam-only check. The repo already has four
instances of this exact mechanic (`FileFormatSpecTest`, `ExtensionPointCatalogTest`,
`VcdExportGoldenTest`, `HeadlessCoreRatchetTest`), and LF-07 independently proposes an
API-surface signature file for `jls.api`. A check that reads the tier table and asserts
"every member of every `frozen` surface matches its checked-in signature" serves both, and
means `jls.api`'s ratchet is a table row rather than a new test.

## On the criterion worth keeping exactly as written

AC-4 / #827's AC-3 — *someone other than the maintainer completes the walkthrough from
published text alone, and every place they guessed becomes a doc fix* — is the best
acceptance criterion in this feature and one of the few genuinely external-validity tests
in the tracker. Do not soften it. But recognize what it costs: it is simultaneously CAP-30
AC-6 and (per the boundary comment) subsumes #212's I1, and it cannot be satisfied by
anyone the maintainer recruits as a favor without becoming theatre. It belongs scheduled
with PF-6's outreach, not with the doc work.

## Where the endorsement stands

The Outcome is correct and the boundary against #212/#223 holds. What I am disregarding is
the framing of AC-1 and AC-3 as the drivers: AC-1 is largely shipped (the adversarial
comment's AC-1′ is right), and AC-3 is gated on a mechanism that is gated on demand this
feature is supposed to create. The durable value is AC-2, and AC-2 is under-scoped —
it should be a project-wide compatibility policy that the plugin story is a *projection*
of, shippable now, with the walkthrough and the outside builder following #212 whenever
its gate honestly opens.
