# Issue #797: TASK-C586-2: a hand-committed screenshot and a manifest entry pointing at something that no longer exists both fail the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "a check." The end is a property: **a picture in JLS's
documentation cannot depict a UI that no longer exists.** #586 states it well;
#797 is the enforcement half — the ratchet that makes the property true rather
than intended. That property is squarely aligned with the project's whole arc.
This is a repository that already treats *derived things must be re-derivable*
as a first principle: the jar and `bom.json` are byte-reproducible and CI
rebuilds and re-checks them on every push (`README.md`, `docs/reproducibility.md`),
`HelpTopicsTest` (#70) already refuses to let a help link rot, `ArchitectureRulesTest`
and the `*RatchetTest` family already refuse to let structural decisions rot.
Documentation imagery is one of the last corners of the tree still exempt.
Closing that exemption belongs on the roadmap.

What I disagree with is the *shape* of the enforcement, and I am explicitly
disregarding acceptance criteria 1 and 4 as written. Reasons follow.

## Ground truth: the corpus the ACs describe is not the corpus in the tree

Measured at the current checkout:

- **README references zero screenshots.** The only `![...]` in `README.md` is
  the OpenSSF Scorecard badge, an external SVG URL.
- **The "hosted manual" does not exist.** `ARCHITECTURE.md` § *Help delivery:
  in-jar now, hosted docs are the planned future (recorded 2026-07)* records
  hosted docs as a direction, not a surface.
- **The in-jar help references exactly ten images, and none is a screenshot.**
  `resources/help/**` is 83 HTML pages with ten `<img>` targets total:
  `elements/{up,down}.gif`, `elements/keypad.jpg`, and
  `images/{bind,const,extend,ipin,jumpend,jumpstart,opin}.gif`. These are
  palette/toolbar icon art — hand-authored source assets, the same family as
  `src/jls/edit/images/*.gif`. **No capture rig can regenerate them.**
- **The actual rotting screenshot corpus is the tutorial**, which neither #586
  nor #797 names: `src/jls/tutorial/{AornotB,AornotBprobe,halfadder,fulladder,counter,signext}.jpg`,
  referenced from `tutorial1–4.html` and shown by `jls.Tutorial` — a dialog
  distinct from `jls.Help`. Six inherited JPEGs of the editor showing circuits,
  the oldest and most rot-prone images in the project, outside the enumeration.

So AC-1 applied literally on day one turns the build red over ten legitimately
hand-authored icons that the manifest cannot own, while leaving the six images
that genuinely depict a UI untouched. Two of its three named surfaces are empty.
The criterion is simultaneously vacuous and hazardous. Any implementer will be
forced to invent an unwritten allowlist ("these gifs are art, not shots") on the
spot — and an allowlist invented under build-red pressure is exactly the artifact
that later rots.

## Reframing 1 (primary): make the property structural, so the lint disappears

AC-1 and AC-2 are two bespoke scanners guarding a *convention* — that generated
images and hand-authored images share a namespace and are told apart by a
manifest lookup. Cut the seam one level lower and both scanners become
unnecessary:

1. **Separate by location, not by lookup.** Captured images live under a single
   generated root (say `resources/help/generated/**` plus a `docs/images/generated/**`
   for repo docs). Source art stays where it is. "Hand-committed screenshot"
   stops being a thing a check must detect and becomes a thing the directory
   layout does not have a slot for.
2. **Enforce with regenerate-and-diff, the idiom this project already owns.**
   CI runs the #796 regeneration command and then `git diff --exit-code` over the
   generated root — precisely how the jar and `bom.json` are already policed.
   A hand-committed image fails because it is not reproduced. A manifest entry
   naming a dead window/pane/circuit fails because the generator errors, with the
   name in the message, for free — AC-2 is a property of a fail-closed generator,
   not of a second checker. One mechanism, already in the project's vocabulary,
   replaces two new ones.
3. **Keep AC-1's *referential* half where it already lives.** `HelpTopicsTest`
   (#70) already parses every `href`/`src` in the help tree and resolves it
   case-sensitively against the jar's resource names. "Every image referenced by
   the help is either a declared source asset or under the generated root" is
   three assertions inside a test that already walks that graph, not a new build
   step. Extending an existing checker also means the tutorial gets covered by
   the same walk once `src/jls/tutorial/**` is added to it — closing the gap
   above rather than institutionalizing it.

## Reframing 2: most of these images are not screenshots, and JLS can already render them headlessly

This is the bigger miss. #586/#797 assume one capture backend — drive the GUI on
#101's headless-sway rig — and therefore inherit #101's every weakness, which is
what AC-4 is reduced to apologizing for. But for a logic simulator, the majority
of doc-worthy images are **pictures of circuits**, and JLS already exports those
with no display server, no JBR, no compositor and no pixel tolerance:

```
java -jar jls.jar -i out.png circuit.jls     # also .jpg, and .svg
```

documented in `README.md`, specified as a stability surface in
`docs/batch-interface.md`, exercised by `test/jls/CliImageExportTest.java`,
and shipped in the headless container image. Every one of the six tutorial
JPEGs is a circuit picture that this path can produce from a `.jls` fixture.

So the manifest should declare a **backend per entry**, with `render` (headless
`-i`) as the default and `capture` (the sway rig) as the exception reserved for
entries that must show chrome — menus, dialogs, the trace window. Consequences:

- The `render` backend is deterministic by construction and needs no pixel
  tolerance at all, which makes #586 AC-4 moot for most of the set and makes
  **#797 AC-4's disclaimer clause unnecessary for those entries**: they do not
  touch `PIXEL_DIFF_MIN` or the JBR download, so there is nothing to disclose.
- The ratchet becomes deliverable *now*, against #411's still-open residual,
  rather than resting on a gate that #411 documents as disarmed
  (`PIXEL_DIFF_MIN: "0"` on both lanes) and a lane that fail-opens on a CDN miss.
- `-i out.svg` gives resolution-independent output for the eventual hosted
  manual — a better artifact than any raster capture, and one whose diff is
  human-readable in review.
- The `.jls` fixtures become the durable source of truth for what a picture
  shows, which is the honest dependency: an image rots when the *circuit* or the
  *renderer* changes, and both are in-tree.

I would restate #797's outcome as: *the generated-image root is reproducible
from the manifest and its fixtures, and CI proves it on every push* — with the
window/pane vocabulary confined to the small `capture` subset.

## A concrete rot this issue does not catch, and its two-line fix

`resources/help/images/{bind,const,extend,ipin,jumpend,jumpstart,opin}.gif` are
**byte-identical duplicates** of `src/jls/images/*.gif` (verified with `cmp`).
Seven copies of live UI art, kept in sync by nothing. This is exactly the failure
mode #586 names — an image outliving the UI it claims to show — already present,
and a capture manifest cannot see it because these are not captures. The fix is a
single assertion in `HelpTopicsTest`: every help image whose basename matches a
palette icon must be byte-identical to it (or better, the duplicate is deleted and
the help HTML points at the one copy). Cheaper than the entire manifest apparatus
and it retires a real instance of the disease.

## Alignment

The direction is right and the project needs it; the CAP-34 store listings and
CAP-27 surfaces downstream of #586 make a stale image a three-channel failure.
But #797 as written builds a bespoke gate over a corpus that is empty in two
places, unownable in the third, and silent about the one place rot actually
lives — and it accepts a substrate its own AC-4 admits is disarmed. Reframed
around the generated-root + regenerate-and-diff idiom the repository already
uses, with headless `-i` as the default backend, the same property is enforced
with less new machinery, no dependency on #411, and coverage of the images that
are actually rotting.

## Recommendation

- Keep the outcome. Rewrite AC-1: classify images as *source art* vs *generated*
  by location; assert the classification inside `HelpTopicsTest`, extended to
  `src/jls/tutorial/**`; drop "hosted manual" until it exists.
- Replace AC-2's second scanner with a fail-closed generator plus
  `git diff --exit-code` over the generated root in CI.
- Add an AC: the manifest declares a backend per entry, `render` (headless `-i`)
  by default; only `capture` entries may cite #101's rig, and only those carry
  AC-4's disclosure about `PIXEL_DIFF_MIN` and the download fail-open.
- Add an AC: the seven duplicated help icons are deduplicated or pinned
  byte-identical to their `src/jls/images` originals.
- AC-3's planted-failure discipline is right as written; keep it verbatim, and
  plant the third case — an image under the generated root that the generator
  does not reproduce.
