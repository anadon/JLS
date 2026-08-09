# Issue #831: TASK-C571-3: Digital's rejected-PR authors are invited by name — but only after the on-ramp exists, and with the kill criterion written down before the first invitation goes out
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the acceptance criteria away and #831 is one sentence: *convert Digital's
stranded patch authors into JLS contributors.* Everything else — the list, the
gate, the etiquette rule, the pre-registered kill date — is machinery around that
conversion. So the only question worth asking is whether the machinery is built
around the thing that would actually cause the conversion. It is not. The issue
is built around **who** to contact and **when**; it never asks **what we would
have to say that a stranger would care about**, and when you go looking for that
answer in the tracker you find it sitting in a different capstone entirely.

I am disregarding AC-2's gate and AC-5's metric as written. Both are defensible
in isolation and wrong in composition; my reasoning is below.

## 1. The gate is drawn from the wrong capstone

AC-2 gates invitations on #567 (templates), #568 (first-issue shelf), #316
(SimpleEditor decomposition). AC-3 says the invitation must point "to a specific
first issue and to the fork-CI feedback loop."

Read that as the recipient. They are a Digital user who wrote a patch Digital did
not take. The message says: a different Java simulator has issue templates, a
labeled shelf, and CI that runs on your fork. That is a description of *chores
available*. Nobody crosses a project boundary for chores.

Meanwhile #558 (FEAT-C29-2, under CAP-29 #513) is a `.dig` importer — "an
instructor whose course lives in Digital opens a `.dig` file and gets a working
circuit plus a migration report." #617 carries the geometry half so the imported
circuit *looks like the one they drew*. That is the sentence that lands: **your
circuits open here.** It is the only line in the invitation that is about the
recipient rather than about us. And it is absent from #831's gate, absent from
AC-3's required content, and owned by a capstone #831 never cites.

The seam is cut in the wrong place. CAP-30 gated PF-6 on CAP-30's own PF-1..3
because that is tidy inside one capstone's boundary, not because those three are
what an invitation needs. **The gate should be #558, plus #567/#568 for the
landing.** If #558 slips or hits its KC-29-1 stop-loss, that is genuine
information about whether this outreach has anything to offer — which is exactly
what a kill criterion is supposed to surface, and KC-30-2 as written cannot see it.

## 2. Harvest the patches, not the people

The deeper reframing: a rejected PR's durable asset is the **diff**, not the
author. AC-1 already requires reading every candidate's contribution and noting
"what the person contributed." That work product is a catalogue of features
Digital users wanted badly enough to implement and could not get. CAP-30 half
knows this — PF-5 is "Digital-wishlist headline features (dark mode #289,
dive-into-subcircuit, keybinding settings)." But PF-5 sources its wishlist from
*issues*, and #831 reads the *PRs*, and the two never connect.

Then the stronger move, which #831 does not contemplate at all: Digital is GPLv3
and JLS is GPL-3.0-or-later. A published rejected PR is **GPL'd source the author
already wrote and cannot land anywhere.** With their permission and their
authorship preserved, JLS can adopt the code, not merely the idea. That inverts
the whole transaction — instead of "please do work for us," it is "your work is
stranded; may we land it here, crediting you?" It is the highest-conversion
message available in this space and it costs the invitee nothing.

One real design consequence to record rather than discover later: JLS's or-later
election is the maintainer's own, and adopting GPL-3.0-only code would pin that
portion to v3-only. The ask should therefore include "and would you contribute it
under GPL-3.0-or-later" — a one-line addition to the invitation template, and a
reason the invitation needs a *drafted contract*, not just a tone.

## 3. The #316 gate is a category error, and #568 already says so

#568 AC-4: each curated first issue "is completable without touching `jls.edit`'s
SimpleEditor internals." If that holds, the on-ramp is complete without the
decomposition — the newcomer never opens the 5,852-line file. So #316 is on
#831's critical path only through CAP-30 AC-5's *code-inspection duel*: the fear
that a Digital-calibre reviewer opens `SimpleEditor.java` and leaves.

That fear is real and the proposed remedy is 12–20 mw (per #316's own band
discussion) to fix it. But look at what a reviewer would actually find in this
tree: `docs/simulation-semantics.md` as a normative spec, a batch interface that
is a written stability contract, JaCoCo package floors and a promoted PIT
mutation ratchet over `jls.sim.*`/`jls.collab.op.*`, NullAway with a
never-unmarks ratchet, sealed-hierarchy dispatch pinned by a test, reproducible
builds with `.buildinfo`. That is a *better* first impression than most projects
of any size. The honest fix for AC-5 is a paragraph in ARCHITECTURE.md: "yes,
`SimpleEditor` is 5,852 lines; here is #316, the measured extraction plan, and
here is why nothing else in the tree looks like that." Visible honesty about your
worst file persuades a good engineer more than hiding it would. **Drop #316 from
AC-2's gate.** The round-2 ordering comment on #571 already narrowed this edge
once for AC-1; it did not go far enough.

## 4. Pre-registration is right; the metric measures the wrong funnel

AC-5 is the best thing in this issue and I want that on the record — pre-agreeing
a kill date and honoring it either way is rare discipline, and the #571 boundary
note is correct that retirement-with-evidence is a *completed* outcome.

The metric is wrong. KC-30-2 fires on "zero external PRs over two quarters," which
is the **whole** funnel. Two confounds, both fatal:

- Invite 15 people, get zero replies, but three drive-by PRs arrive through the
  templates → KC-30-2 does not fire, and the recruitment half survives on credit
  it did not earn.
- Zero PRs arrive at all → recruitment is retired when the real failure may be
  the shelf, the templates, the niche, or the absence of #558.

The recruitment half has its own funnel and it is trivially measurable:
invitations sent, replies of any kind, PRs attributable to an invitation. Set the
threshold there — *n≥10 invitations, zero replies of any kind, one quarter* is a
decisive result, and it arrives two quarters sooner than KC-30-2's.

## 5. The proportion is the finding

Concretely, this task is: read some Digital PRs, write ten names and one line each
into a file, send ten messages, write a date down. Call it an afternoon. It
currently carries five acceptance criteria, a dependency on the largest refactor
in the tracker, a sibling task, a parent feature, a capstone, and a review fleet.
#508 §5's diagnosis — that the tracker reads as an internal monologue — is a risk
this issue instantiates rather than escapes. The most aligned version of #831 is
one that *shrinks*: a list is a file, the gate is one sentence, the kill criterion
is a number and a date.

## The alternative I would build instead

1. **Pilot first, at n=3, now.** Pick three authors whose rejected patch maps onto
   something JLS already has or plausibly wants. Ask a *question*, not a
   solicitation: "your patch is stranded upstream; would you want it to live
   somewhere?" Zero engagement from three well-chosen people falsifies the
   premise for an hour of work — before #567/#568 are justified by an audience
   nobody has confirmed exists. One interested reply gives you a named human whose
   needs can *shape* the shelf, so the first curated issue is one they'd want.
   AC-4's "one message per person, no repeat solicitation" is what makes this look
   forbidden; it is over-tight. Distinguish a research contact from a pitch, and
   the pilot costs no one their one invitation.
2. **Make the patch catalogue the deliverable**, with the person list as a
   by-product. It feeds PF-5, it survives KC-30-2's retirement, and it is useful
   even if every invitation is ignored — which is the property #831's current
   deliverable lacks entirely.
3. **Re-gate on #558**, and make "your `.dig` circuits open in JLS, here is the
   migration report" a required element of AC-3's invitation text.
4. **Offer adoption**, with authorship and an or-later relicense ask, wherever the
   rejected patch is code JLS actually wants.
5. **Kill on the recruitment funnel's own numbers**, one quarter, threshold stated
   as replies-per-invitation.

## Verdict

**rethink.** The end is right and the project needs it: JLS is bus-factor-1 with a
serious engineering substrate and no second pair of hands. But the design points
the invitation at chores instead of at the migration bridge the tracker already
plans, gates it behind the most expensive item in the graph on reasoning #568
itself contradicts, spends the cheap falsifying experiment last, and measures the
kill criterion on a funnel that is not the one being killed. Keep the outcome,
keep AC-1's no-bulk-mail discipline and AC-4's venue respect, keep the
pre-registration instinct — and rebuild the gate, the payload, the metric, and the
ordering around the asset that actually gives a Digital author a reason to look.
