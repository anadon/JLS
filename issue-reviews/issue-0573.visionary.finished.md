# Issue #573: FEAT-C32-2: the curated examples run on a static demo page — no backend, no accounts, no persistence, nothing that can die and take user data with it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the CAP-32 vocabulary and #573 is one sentence: *a stranger should be able to
see a real JLS circuit computing before deciding whether to install anything.* That
goal is squarely on the project's arc. The README is already a shop window (installers
for six platforms, checksums, attestations, a container image); #551 wants a gallery;
#519 wants hosted docs; ARCHITECTURE.md's help-delivery decision already says hosted
web docs are "the planned future" and keeps in-jar help portable "so the same tree can
be published to the web without rewriting." Nothing here pulls against the project.

What I do not endorse is the shape: #573 has no content of its own. It is a hosting
wrapper whose identity is delegated to #572's go/no-go, and the mechanism #572 prefers
contradicts the property #573 puts in its own title.

## The load-bearing contradiction: CheerpJ versus "nothing that can die"

AC-3 says static files only, deployment is a file copy, zero operational upkeep, and
the title names the anti-simulator.io permanence property. The favored mechanism is a
CheerpJ-wrapped jar. CheerpJ is a closed-source commercial runtime whose standard
deployment loads its loader and pulls JRE class data from Leaning Technologies'
servers at page load. That is a third-party service in the critical path of a page
whose entire pitch is that no service is in the critical path. It can die and take the
demo with it. It is also precisely the "no CDN at runtime" line #500 §3 risk 5 drew.
Self-hosting the runtime blunts this, but then AC-1's sub-30-second budget is being
spent on tens of megabytes of JRE over a school network, and "deployment is a file
copy" becomes a file copy of a vendored proprietary blob into a GPLv3 repo.

Two more properties break under the same mechanism:

- **AC-2 "read-only by construction" becomes read-only by configuration.** What
  CheerpJ hosts is `SimpleEditor` — the actual editor state machine, with its palette,
  its dialogs, and its save path. Read-only means greying out menus. That is a
  configuration you can regress, not a construction you cannot.
- **KC-32-2's scope cliff becomes unenforceable.** The cliff says any request that
  turns this into a browser editor is out of scope by construction. If the artifact
  *is* the editor with buttons disabled, every such request is a one-line diff away,
  and the maintainer is defending a policy rather than a structure. #886, filed four
  days after this issue, already walks toward the edge: user circuits arriving in a
  URL fragment is user content in the browser, which is the thing AC-2 says will never
  exist here.

## The reframing: the demo page is a generated artifact of the batch surface

I would define #573's page independently of #572, out of outputs JLS already ships and
already gates:

- `-i out.svg` (JLSStart.java:765/821, org.jfree.svg 5.0.7) renders the schematic.
  Shipped, tested, and already the mechanism #551 rides.
- `-vcd` (`BatchSimulator`, pinned by `VcdExportGoldenTest`, documented as a stability
  contract in `docs/batch-interface.md`) supplies wire values over time.
- `-t` test vectors (same contract) supply the stimulus. #548 already requires every
  curated example to carry "a suggested exercise" — that sentence *is* a stimulus
  scenario.
- Stable element ids (#165) are the join key between the SVG geometry and the VCD
  signal names.

The page is then a schematic with a time scrubber and a small set of scenario buttons:
pick a stimulus, watch wires change color, scrub back and forth. For combinational
examples you can enumerate the entire input space up front (n inputs, 2^n vectors —
free below a dozen), so free-form input toggling is fully available there, not
degraded at all. Sequential examples get scripted scenarios plus scrubbing. That is an
honest, bounded, *measurable* loss of fidelity relative to running the real GUI, and
it is the only loss.

What that buys, item for item against this issue's own criteria:

- **AC-1** stops being a coin flip: an SVG plus a few kilobytes of VCD and a hundred
  lines of vendored JS is a one-second page, not a thirty-second budget.
- **AC-2** becomes literally true. There is no code path that accepts user content
  because there is no code that can consume a circuit — only a recorded trace.
- **AC-3** becomes true without an asterisk. The page is bytes in the repo.
- **AC-4/AC-5** are unchanged and get easier: the honest install pitch writes itself —
  *this page replays circuits we chose; the tool simulates the ones you draw.*
- **KC-32-2 becomes self-enforcing.** You cannot grow an editor out of a trace player
  without writing a simulator, and #221 (sole discrete-event strategy) plus #500's
  closure already forbid that. The cliff stops being a policy and becomes a wall.

This is #572's ranked fallback (a), and I am arguing it is not a consolation prize but
the better primary. It is a *viewer*, in the same category as GTKWave and Surfer, which
`docs/vcd-interop.md` already blesses — not a second engine. #500 died on KC-19-1
because PF-2 was a JS *simulator*; it explicitly accepted the framing that "the
recording, not a live session, is the contract" (§7.2). A VCD player is that framing
with the part that killed CAP-19 removed.

CheerpJ then has a good place: a progressive enhancement. A "run the real editor"
button on the same page that lazy-loads it. If CheerpJ vanishes, one button vanishes;
the demo survives. That inverts today's dependency, where a no-go on #572 deletes #573
entirely.

## The seam nobody owns: JLS has no website

`.github/workflows` contains ci, codeql, mutation, release, repro-installers and
scorecard. There is no Pages workflow and no `site/` tree. Yet #551 must "publish via
the repository or GitHub Pages," #573 must host static files, #574 must add links
between those pages, #519 wants a hosted versioned manual, and ARCHITECTURE.md already
commits the help tree to eventual publication. Five scopes each carry their own clause
about publishing a static page and none of them owns the publishing substrate.

That is the architectural cut this issue never considers. One `scripts/build-site.sh`
producing a `site/` tree from committed sources plus regenerated exports, one Pages
deploy, and every one of those issues becomes a page contributed to it. It also
satisfies #551's AC-2 (one scripted regeneration command) and this issue's AC-3
(deployment is a file copy) with the same machinery, and it is the natural home for
the hosted help when that decision matures.

## Disregarding the dedup ruling's premise

The pass-2 comment rules #551 and #573 not-duplicates because "one is a picture, the
other is a running simulator." I am setting that aside, because it is a mechanism
distinction that only holds while the mechanism is CheerpJ. Under the reframing above
they are one page: a gallery card carries a schematic *and* a play button, generated by
one command from one corpus (#548). #574 then collapses to an anchor and a README line.
Three features at 1–2 + 1–2 + 0.5–1 mw become roughly one, with no go/no-go gating any
of it — and the risk asymmetry the comment was protecting disappears rather than being
managed, because there is no longer an unresolved mechanism to couple to.

## What I would keep exactly as written

The refusals. No backend, no accounts, no persistence, no export feature, no
per-circuit HTML artifacts, no in-browser editor. Those are the best part of this
issue and they are what makes it a legitimate narrowing of #500 rather than a re-run of
it. My argument is only that the CheerpJ mechanism cannot keep those promises, and
that a trace-replay page keeps them structurally instead of by discipline.
