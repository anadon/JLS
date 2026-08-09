# Issue #844: TASK-C573-3: the demo says out loud what it is not, and hands the visitor the installer — the funnel closes instead of dead-ending
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the packaging away and #844 asks for two sentences and a link: *here is what
the real tool does that this page cannot, and here is how to get it.* That is the
cheapest item in the entire CAP-32 (#516) stack and the only one that carries the
capstone's whole thesis — the demo is worthless as a demo; it is only worth
building as the mouth of a funnel. The issue is right about that, and right that a
demo which dead-ends is a curiosity.

It is wrong about where the work lives. #844 is written as a property of *demo
pages*, ordered behind #841 → #840 → #572's go/no-go, none of which exist, and one
of which (#572) may return a verdict that deletes the page it decorates. The
funnel is not a feature of the demo. It is a property of every public surface JLS
is about to grow, and it is the one piece of CAP-32 that needs no CheerpJ, no
example set, and no go/no-go to be true.

## The finding: JLS is about to build the same page three times

Read across the open tracker rather than down this branch of it:

- **#551 (FEAT-C27-4)** — published SVG gallery. AC-3: "the gallery links back to
  install instructions." AC-4: "renders acceptably in both light and dark browser
  themes."
- **#573/#844** — demo pages. AC-2: link to the installers. AC-5: legible in light
  and dark, and at phone width.
- **#519 (CAP-35)**, cited by #551's own boundary note — the hosted versioned
  manual and screenshot pipeline.

Three separately-owned static web surfaces, each independently acquiring an
install call-to-action, a theme story, a publish step, and (per #844's AC-2) a
link-checking lane. `README.md` is a fourth: lines 12–70 are already the most
carefully written install document in the project — per-arch asset names, the
SignPath publisher caveat, the macOS "unsigned by choice, not oversight"
Gatekeeper walkthrough, the NixOS flake, the RISC-V "no installer exists" honesty.
A demo page that grows its own platform-detecting install blurb will either
reproduce that nuance or, far more likely, hand a macOS visitor an unsigned `.dmg`
with no Gatekeeper instructions — a *worse* first impression than no link at all.

This project already knows the shape of the fix. ARCHITECTURE.md's recorded
decision "Help delivery: in-jar now, hosted docs are the planned future" names
"one source of truth" as the reason, and keeps help content portable *specifically
so the same tree can be published to the web without rewriting*, policed by
`HelpTopicsTest`'s link checker (#70). The discipline exists; #844 as written
opts out of it.

## Reframe 1 — one site seam, not one paragraph per page

Cut the seam at the publishing layer, not the demo. Build `site/` once: one
in-tree static-site build with one theme (light/dark tokens, responsive), one
publish workflow, one link check, and **one install/scope partial** generated from
a single source — the README install section extracted to `docs/install.md` and
transcluded, or a small committed manifest that also feeds the gallery and the
future manual. #844's real deliverable is then a ~40-line partial plus a build
rule, not a per-page authoring obligation. AC-1's "every demo page states…"
becomes structurally unfalsifiable instead of a thing a reviewer must re-check on
every page added, and #551 and #519 inherit it for free instead of re-earning it.

This is smaller than what #844 describes, not larger. It is also the only version
that survives the demo being cancelled.

## Reframe 2 — structural honesty beats a paragraph nobody reads

AC-1 and AC-3 ask for prose that states what the demo is not, in specific
capabilities. Prose is the weakest possible instrument here: it is read by nobody
who is already clicking, and it drifts the moment the desktop gains a capability.

If #572 goes CheerpJ-yes, the demo is the actual Swing GUI: the menus are *right
there*. Make the missing capability announce itself at the moment the visitor
reaches for it — File > Save, File > Open, the HDL export item — each disabled
item explaining in one line that this lives in the desktop tool, with the install
link. That is the KC-32-2 scope cliff stated exactly where "can I edit?" is being
asked (AC-3's own words), it cannot drift because the trigger points *are* the
absent capabilities, and it costs less than writing marketing-free feature copy
for N pages. If #572 falls to the SVG+VCD fallback, the same principle inverts:
the demo's control surface is the manifest — what you can touch is what the demo
does, and one honest line covers the rest.

**A trap AC-5 walks into.** A CheerpJ-hosted Swing GUI renders FlatLaf *light*
regardless of browser theme — ARCHITECTURE.md records the dark default as out of
scope behind ~126 hardcoded color call sites (#76). "Renders legibly in both light
and dark" can therefore only ever mean the page chrome, with a bright rectangle in
the middle of a dark page. #551's AC-4 has the better wording already ("or ships a
single theme deliberately and says so"); copy it, and let the demo say the desktop
app is light-themed today rather than implying otherwise.

## Reframe 3 — ship the funnel before the demo, on #551

This is the change I would actually make. #844's content is the only part of
CAP-32 that is mechanism-independent, and #551 (SVG gallery) is already inside the
≈1–2 mw distribution slice #508 funded. Put the scope statement and the verified
install path on the gallery **now**, before #572 spends its go/no-go, and watch
whether a static page of real circuits plus a clean install CTA moves installs at
all. If it does, #572/#573 have a measured reason to exist and a baseline to beat.
If it does not, the project has learned — for well under a milliweek — that the
bottleneck is not evaluation friction, and CAP-32's 3–6 mw is saved. Today CAP-32
is a bet placed with no instrument attached; this ordering turns it into an
experiment.

Note also that #508's planning ratchet ("no new tier:feature/tier:task until two
capstones close") postdates nothing here: #844 was filed the day after that review
landed, as one of three tasks under a feature under a capstone filed *after* the
review declined to disposition it. That is not a reason to refuse #844 — it is a
reason to keep it at the size described above.

## Reframe 4 — a funnel with no number is not a funnel

The Outcome paragraph claims the demo is "the top of the funnel the capstone is
actually measuring," and then no acceptance criterion measures anything.
Static-only hosting rightly forbids analytics (capstone AC-2, #38). But the
measurement already exists and is free: GitHub's release API reports per-asset
download counts. A committed `scripts/download-kpi.sh` plus a monthly workflow
appending to a CSV in-tree is the "download-count KPI" #508 asked for at item 7,
costs a rounding error, respects the privacy stance completely, and works whether
or not the demo ever ships. Add it here or file it as a sibling — but the funnel
should not close onto an unmeasured surface.

## On AC-2 and AC-4 specifically

I would replace both. AC-2 asks for a CI lane that fails when a release URL moves;
AC-4 asks for a one-time manual end-to-end walk. The first adds a
network-dependent lane to a CI culture that has gone out of its way to be hermetic
(`wayland-rig-selftest.sh` drives the rig against a stub with no network, no
compositor, no JBR; the BMP checks avoid ImageMagick to avoid brew/network). The
second expires at the next release. Better on both counts, and cheaper: link only
to `/releases/latest`, which GitHub guarantees; assert *asset-name* expectations in
the release workflow itself, which already enumerates and checksums every asset
(`SHA256SUMS-installers-<os>-<arch>`); and get link rot coverage from one repo-wide
checker over `README.md`, `docs/**`, and `site/**` — which pays for the ~40 doc
links already in the tree, not just this one.

## What I am disregarding, and what survives

I am setting aside AC-1's per-page authoring obligation (replaced by one generated
partial), AC-2's bespoke CI lane and AC-4's once-only walkthrough (replaced by a
stable URL, a release-workflow asset assertion, and a repo-wide link check), and
AC-5's both-themes promise (replaced by #551's deliberate-single-theme wording).
The reason is one sentence: every one of those is written as a property of a page
that does not exist yet and may never, when each is really a property of how JLS
publishes anything.

What survives untouched and is genuinely good: the refusal to let the demo imply
persistence or editing; naming capabilities instead of adjectives; and the
insistence that the visitor leaves with an installer rather than a shrug. Those
are the project's honesty ethos — the same instinct that wrote "unsigned by
choice, not oversight" into the README — applied to a new surface, and they should
be kept exactly as stated.

**Smallest correct next step:** extract the README install section to
`docs/install.md`, write the shared scope-and-install partial against it, land it
on #551's gallery, and start the download KPI — then let #572 decide whether the
demo pages ever exist to inherit it.
