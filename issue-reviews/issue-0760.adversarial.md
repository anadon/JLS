# Issue #760: TASK-C545-1: the README shows the product above the fold — two screenshots and a drawing-and-simulating GIF, with a drift check that fails on a missing image
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for two screenshots plus an animated GIF above the fold in
`README.md`, a build-time drift check on image paths, and a hand-vs-manifest
capture policy pegged to #586. The capture-policy and dependency reasoning
(AC-3) is genuinely careful. But two of the four acceptance criteria do not
actually gate what the Outcome promises, and the issue's own body is now
self-contradicted by its lone comment — which corrects an ordering edge and a
scope claim but was posted 2026-08-08T17:47:28Z and never folded back into
the body itself.

## Findings, most severe first

**1. [HIGH] AC-1's "animated GIF" has no chartered production path — the
capture infrastructure this issue leans on only takes still frames.**
AC-1: *"at least two screenshots plus an animated GIF of drawing-and-
simulating."* AC-3 routes capture through #586's rig when it lands, which is
explicitly `scripts/wayland-rig.sh` (issue #101's headless-sway apparatus,
confirmed at `/home/user/JLS/scripts/wayland-rig.sh:222-350`): it calls
`grim` to write single PNGs (`desktop-before.png`, `control.png`,
`desktop-after.png`) and nothing else — no video, no frame sequence, no GIF
encoder anywhere in the script or in `pom.xml`'s tool list. #586's own body
states the gap directly: *"Interaction scripting is not #101's ...
#91 owns those. Any manifest entry needing more than 'boot with this file
and screenshot' depends on that capability, and the dependency must be
stated rather than smuggled into this rig."* A GIF of drawing-and-simulating
is precisely "more than boot-and-screenshot" — it needs scripted mouse/wire
gestures over time, captured as a sequence, and encoded. Neither #586 nor
#760 states this dependency on #91; #760 just assumes the GIF is producible.
If #586 hasn't landed (the likely case — it's an open, multi-part feature
depending on #101's residuals), the only route to AC-1 is an unscripted,
by-hand screen recording with no committed reproduction recipe — which
sits awkwardly next to AC-4's *"nothing the README shows is a claim a fresh
clone cannot reproduce."* **Recommendation:** either state the #91
dependency explicitly and let this task ship the two screenshots now while
the GIF waits on scripted interaction capture, or scope the GIF out of this
task's AC-1 and file it as a follow-up once #91's interaction scripting
exists.

**2. [HIGH] "Above the fold" is unenforceable by the only check the issue
specifies — a gameable acceptance criterion.** AC-1 requires the images
"positioned above the fold"; AC-2 is the sole automated gate and only
checks that *referenced paths exist on disk*. Nothing in AC-2 (or anywhere
else in the issue) verifies position. "Above the fold" is also not a
well-defined quantity for a GitHub-rendered Markdown file — it depends on
viewport width, GitHub's collapsed-sections behavior, and light/dark theme
rendering, none of which the issue pins down. A PR could commit three
correctly-pathed images at the very bottom of the 369-line README (current
`README.md` runs "Installing JLS" → "Running from jar" → … → "Contributing")
and AC-2 would pass while the actual Outcome ("a stranger scrolling ...
sees circuits ... before reading a paragraph") is not met. **Recommendation:**
either drop "above the fold" from the machine-checked ACs and make it an
explicit PR-review item (as #381 did for its screenshot matrix — "not a
JUnit assertion, must not be dressed as one"), or add a mechanical proxy
(e.g., assert the first image reference occurs before line N / before the
first `##` heading) so the check actually gates position.

**3. [MEDIUM] The issue body and its own comment now disagree, and the body
was never edited to match.** The body's `ordering_after: [381]` and the
sentence *"consumes that baseline rather than restating it"* are directly
contradicted by the sole comment (posted same day, later timestamp),
titled *"ORDERING EDGE DROPPED"*: *"The edge was backwards even before
today ... #381 no longer plans a README pass at all ... this issue is the
sole owner of the README image set."* That comment is correct against the
evidence it cites (#381's own body confirms its README item was struck and
reassigned to #545/#760/#762), but a reader or picker who reads only the
issue body — which is what task-queue tooling and this review's own
instructions treat as primary — will follow the stale `ordering_after: [381]`
edge and the stale "consumes, doesn't restate" framing. An issue whose
correction lives entirely in a comment, uningested into the machine-readable
YAML block, is a live footgun for exactly the kind of automation this
project's issue format is designed for. **Recommendation:** edit the issue
body's YAML (`ordering_after: []`) and the Outcome paragraph before this is
picked up; don't leave the correction as a comment-only patch.

**4. [MEDIUM] "At least two screenshots" is underspecified relative to the
sibling issues this one claims to consume — content is unconstrained and
gameable.** #73's planned task (which #760's own comment says #760 now
solely owns) specified content: *"Two screenshots (editor with a circuit;
interactive sim with trace)."* #760's AC-1 drops that specificity and just
says "at least two screenshots ... an animated GIF of drawing-and-simulating,"
relying on CAP-27 AC-1 by reference rather than restating the content
constraint. As written, two near-duplicate screenshots of the same idle
editor window (no drawn circuit, no simulation) would satisfy AC-1's literal
count and AC-2's path-existence check. **Recommendation:** either inline
#73's content constraint into AC-1 explicitly, or add it to AC-3 alongside
the currency requirement, so the two stills are pinned to "circuit drawn"
and "simulation running," not left to the implementer's taste.

**5. [LOW-MEDIUM] AC-2's drift check only catches a missing path, not a
stale image — AC-3's "depict the current UI" claim has no ongoing
enforcement.** Once images are hand-captured (the realistic near-term
path per AC-3's own fallback), nothing in this issue's method re-verifies
that a screenshot still matches the UI after a later PR changes toolbar
icons, dialogs, or theme defaults (e.g., #153's FlatLaf default, or #381's
planned dark-theme work, both of which touch exactly the chrome a
screenshot would show). AC-3's "depict the current UI" is therefore a
one-time-true, unratcheted claim — the same failure mode #586/#797 exist to
close, and which this task is explicitly allowed to punt on ("if #586's
pipeline has not landed, they are hand-captured and that is stated"). That
punt is reasonable given #586's status, but the issue should say plainly
that AC-3's "current" clause is unenforced until #586/#797 land, rather than
implying AC-3 is fully satisfied by a one-time capture + a sentence in the
PR.

**6. [LOW] Scope ambiguity: "above the fold" plausibly requires
restructuring the README's opening, which is not explicitly authorized.**
The current `README.md` opens directly into "Installing JLS" (line 12) with
no room carved out for a media block before it. Satisfying AC-1 as written
means either inserting a new section before "Installing JLS" or shortening/
moving existing content down — a structural edit beyond "add two images and
a GIF." The issue doesn't say which, and #545's "shop-window delta" framing
suggests #760 should stay minimal. Worth a one-line clarification in the
method rather than leaving it to the implementer to decide how much of the
existing README moves.

## What's solid

- AC-3's conditional (manifest-driven if #586 has landed, hand-captured-
  and-stated if not) is well-reasoned and correctly anticipates the future
  collision with #797's "hand-committed image fails the build" ratchet —
  better handled here than in most of the sibling issues reviewed alongside
  this one.
- AC-4's grounding in #73 §4's "nothing shown that a fresh clone cannot
  reproduce" invariant is the right reuse of an existing, tested project
  norm rather than inventing a new one.
- The division of labor against #762 (comparison table + badge curation)
  is clean — no functional overlap between what #760 and #762 each claim.
- The self-review comment's citation trail (quoting #381's actual struck
  README item, #545's actual "shop-window delta" framing) is accurate
  against the issues it cites — the analysis is right, it is just not
  folded into the authoritative body.

## Verdict rationale

Two of four acceptance criteria (the GIF and "above the fold") cannot be
mechanically verified by anything this issue describes, and one is not
obviously producible with the tooling this repository actually has. The
capture-policy reasoning is sound, but the core "shop window" claim can be
satisfied on paper while missing the point. Rework before implementation:
tighten AC-1 (GIF feasibility/dependency, screenshot content) and AC-2
(position enforcement or explicit human-review carve-out), and fold the
comment's correction into the body.
