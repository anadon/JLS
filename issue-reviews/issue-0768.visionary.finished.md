# Issue #768: TASK-C548-3: every example carries a caption and a suggested exercise, and a shipped example without either fails the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and the claim is: **curation must be a
property the build can check, not a promise a maintainer makes once.** That is
exactly right, and it is the project's own idiom — `HelpTopicsTest`'s
palette-coverage completeness test, `ExtensionPointCatalogTest`'s bidirectional
catalog cross-check, `NotificationRatchetTest`, `HeadlessCoreRatchetTest`.
JLS has repeatedly decided that a rule nobody can regress is worth more than a
rule written down. #768 applies that decision to the example corpus. Endorse the
end without reservation.

The mechanism is where it pulls against the project. Three things are wrong in
kind, not in degree, and one much better route is available that the issue never
considers.

## 1. AC-1 and AC-4 together mint two sources of truth for one sentence

AC-1 wants the caption "in a form the Examples menu and the gallery can read."
AC-4 wants the same caption "also visible in the opened circuit itself (a header
`Text` element or equivalent)." Read literally, that is a sidecar manifest plus
a string baked into the `.jls`, with nothing binding them. The first example
whose caption is improved in one place and not the other ships a menu that lies
about the circuit it opens — and the failure is invisible, because both halves
are individually non-empty and the AC-2 ratchet passes.

The format already offers a single home at zero cost. `docs/file-format.md` §5:
*"Unknown attribute names are silently ignored"*; §9: adding an attribute to an
existing element type needs **no version bump**. And §9's silent-drop caveat —
the reason `initrle` and `sync` are hazards — does not bite here, because a
dropped caption changes no simulation behavior. So:

> The caption is the header `Text` element. The exercise is a `String exercise`
> attribute on that same element. There is one string, it lives in the circuit,
> and nothing can drift from it.

The menu and the gallery then *derive* rather than duplicate. To keep
KC-27-1's startup-time gate honest (#764 must not XZ-decompress ten circuits to
build a menu), ship a generated index and cross-check it against the circuits
in both directions — precisely the `ExtensionPointCatalogTest` shape #223
already established, and the `Map.jhm`/`JLSHelpTOC.xml`/`HelpTopicsTest`
triangle already in the tree. AC-4 stops being a second copy and becomes the
*only* copy; AC-1 becomes a derivation test.

This also pays #551 and #573 for free. #551 renders the set through `-i out.svg`
from the shipped circuits: if the caption is *in* the circuit, it is in the SVG,
and a gallery whose captions cannot diverge from its images is a strictly better
artifact than one that stitches two files together at build time.

## 2. AC-3's relative length bound is a misreading of CAP-27, and it rewards padding

CAP-27 (#511) says a stranger reaches a running, understood example "without
reading anything longer than a caption." That is an **absolute** bound on the
reading burden of the on-ramp. AC-3 converts it into a **relative** comparison
between two strings of the same example — "each exercise is no longer than its
caption" — which is a different proposition and a worse one.

Concretely: caption "4-bit ripple-carry adder" (24 chars) with exercise "Change
the low bit of A and watch the carry propagate" (47 chars) is a good pair, and
AC-3 fails it. The cheapest way to turn the bar green is to *lengthen the
caption* — so the metric moves the one quantity CAP-27 actually cares about in
the wrong direction. It is a proxy that is anti-correlated with its target.
"Longer" is also unspecified across characters, words, and rendered width, which
matters at these lengths.

Replace with what CAP-27 actually asked for: an absolute budget on each string
(a caption-sized bound, single line, no embedded newline — pick the number from
what the menu item and the gallery caption line can render without truncation)
and, if a joint bound is wanted, one on caption+exercise together. Same
mechanical assertion, no perverse gradient. **I am explicitly disregarding AC-3
as written.**

## 3. The sample record is being invented three times, in three issues

- #764 AC-1/AC-3: the menu lists the shipped circuits, wired to shared `Action`s.
- #766 AC-3: category lives "in data the menu reads." AC-4: provenance recorded.
- #768 AC-1: caption and exercise live "in a form the menu and gallery can read."

That is one data structure — id, file, category, caption, exercise, provenance,
and later lesson/difficulty for #552 and #517 — accreting one field per issue,
owned by none of them, with the menu (#764) built *before* it knows what it will
display. #548's own dedup comment insists on "one corpus, one mechanism"; the
corpus got that discipline and its metadata did not.

Reframe the ordering: whichever of #766/#768 lands first defines the whole
record — not just its own field — and the other fills it in. Since #766 is
`ordering_after` #764 and #768 follows #766, the natural home is #766, leaving
#768 as what its title actually promises: **the ratchet, plus the prose.** That
is a cleaner, smaller, better issue than the one filed.

## 4. The reframe I would actually build: attach the exercise to the element the student must touch

This is the out-of-the-box route, and it is the one I would push hardest.

A caption tells a student what a circuit *is*. It does not make it *understood*,
and CAP-27's outcome sentence is "running, **understood**." The documented
ten-minute bounce is not "I could not tell what this file was" — it is "I opened
it, pressed simulate, and nothing moved." A free-floating sentence at the top of
the canvas does not fix that. What fixes it is telling the student *which thing
to poke*.

JLS already has the seam. §8 of the format: `sid` is a permanent per-element
identity, minted once, preserved across save/load/undo/checkpoint, and
explicitly *"metadata: it never affects simulation."* So put the suggested
exercise on the element it is about:

```
ELEMENT InputPin
  String sid "…"
  String exercise "Toggle me and watch Carry propagate"
END
```

What this buys, none of which the issue's flat string buys:

- **The exercise is verifiable, not just non-empty.** "Every example carries an
  exercise" becomes "every example has at least one element carrying an
  `exercise` attribute" — automatically non-vacuous, and automatically pointing
  at an element that exists, because it *is* an element.
- **It is renderable as an affordance.** The editor can highlight or tooltip
  that element on first open. That is a first-run gesture, not a paragraph, and
  it is what actually gets a stranger from "opened" to "running."
- **It composes with the corpus's own stimulus requirement.** #548 AC-3 already
  demands every example simulate under the batch simulator headless, so each
  circuit must ship drivable inputs. The exercise should name one of them; a
  test can assert the named element is togglable rather than trusting prose.
- **It seeds #552 and CAP-27 AC-5.** A stepped build-along lesson "completable
  by following on-screen prompts only" is an *ordered sequence of
  element-anchored prompts*. Built this way, #768 lays that mechanism's first
  stone instead of a dead-end string that #552 must route around.

The caption stays where §1 puts it (the header `Text` element, one copy). The
exercise moves from "a second caption" to "a pointer with words on it."

## 5. Free authorship the corpus work has not noticed

`src/jls/tutorial/` ships `halfadder.jpg`, `fulladder.jpg`, `counter.jpg`,
`signext.jpg`, `AornotB.jpg`, `AornotBprobe.jpg` across four HTML pages. The
tutorial already teaches a half adder, a full adder, a counter, and a sign
extender — as **screenshots of circuits that do not exist as files**. Those are
four of #766's ten, their captions are already written in `tutorial1-4.html`,
and #73's fresh-authorship rule is satisfied trivially (this project authored
them). Making the tutorial's pictures openable is a better use of #766's budget
than authoring ten new circuits, and it retires a duplication — a manual that
shows a circuit it cannot open — that nobody has filed.

## What I would keep exactly as filed

AC-2 is the heart of the issue and needs no change: a scratch example added
without metadata must turn the bar red, with the transcript recorded. That
red-state discipline is what #381 §9 demands of every ratchet in this project
and it is the reason this task is worth its 0.5–1 mw.

## Verdict

**endorse-with-reframing.** The goal — curation enforced by the build — is
aligned with the project's whole ratchet culture and with CAP-27's gate. Three
changes before implementation: (1) one home for the caption, in the circuit,
with menu and gallery deriving and a bidirectional cross-check; (2) drop AC-3's
relative length bound for absolute per-string budgets; (3) define the sample
record once, in #766, and let this issue be the ratchet plus the prose. And one
change I would argue for on the merits: make the exercise an element-anchored
attribute rather than a second sentence, because that is the version that gets a
stranger from *opened* to *understood*, and it is the version #552 can build on.
