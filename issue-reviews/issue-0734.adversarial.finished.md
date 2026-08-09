# Issue #734: TASK-C542-3: the shipped adder lab is screenshot-tested for state distinguishability in full grayscale and under all three dichromacies
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

TASK-C542-3, filed 2026-08-04, `part_of_feature: 542`, `ordering_after:
[TASK-C542-2]` (#731), no comments. It wants a new
`CvdStateDistinguishabilityTest` that renders "the shipped adder lab" and
asserts every wire-state pair stays distinguishable by a non-color
channel alone, in grayscale and under all three CVD simulations, with a
red-then-green discipline and no second display apparatus. The idea is
sound; the issue as written asks for a test over two pieces of
infrastructure that don't exist and a transform this issue does not own,
without budgeting either, and its own `ordering_after` is stale against
the task graph as it stands today.

## Findings, most severe first

**1. [High] Neither half of the test subject exists, and nothing in this issue budgets building it.**
No `.jls` fixture named or resembling an adder lab exists anywhere in the
repo (`find -iname "*.jls"` returns only
`test/fixtures/riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`, and `riscv/gui/cpu.jls`; no `resources/samples/`
directory). A repo-wide search for "shipped adder lab" turns up only this
issue and its own quotation inside #542's body — no issue anywhere in the
tracker owns creating this fixture, and #731 (TASK-C542-2, this issue's
sole stated prerequisite) doesn't mention it either. On the apparatus
side, `test/jls/ui/package-info.java` documents Layer 3 (rendering
assertions) as "starter present": the only thing that exists,
`RenderAssert`/`RenderBoundsTest`, checks that an element paints inside
its index bounds — a containment check, not a color/grayscale/CVD
distinguishability analysis. `ARCHITECTURE.md` independently calls
Layers 2-3 "reserved." #542's own adversarial review (finding 2, filed
the same day) already flagged this exact gap for the parent feature's
AC-2 and it is still unresolved in the checkout today. `band_mw: 1-1.5`
does not read as pricing "author a lab fixture, then build a
never-before-built pixel-to-non-color-channel measurement pipeline, then
write the assertions."
*Recommendation:* name the adder-lab fixture and the grayscale/CVD
measurement apparatus as explicit line items (or a named prerequisite
task) before this is picked up, and re-price the band once that scope is
visible.

**2. [High] The CVD transform this test needs is a separate, differently-owned deliverable, and this issue's `ordering_after` does not reflect that.**
AC-1 requires the test to run "under protanopia, deuteranopia and
tritanopia." That transform is not this issue's or #731's to build: it
is TASK-C543-1, filed as #876 (replacing #736, which was closed as a
near-verbatim duplicate the same day, 2026-08-08). #876's own boundary
notes say in plain text: *"#734 is a consumer, not a prerequisite —
#543's AC-3 requires #734's `CvdStateDistinguishabilityTest` to drive
through this path,"* and separately flag: *"#542's CI screenshot legs
need a CVD transform too, so the transform is arguably #542's to write
and this cluster's to wrap... a different answer is a `REPLAN:` on
#507."* That REPLAN does not exist (#507 carries no comment closing the
question). #876's `ordering_after` is `[TASK-C542-2]` (#731) — the same
as #734's — meaning both TASK-C542-3 and TASK-C543-1 are simultaneously
unblocked by the graph as currently ordered, with #734 depending on
#876's output but declaring no dependency on it. An implementer who
follows #734's stated ordering literally will either stall waiting on an
undeclared prerequisite or duplicate-build a second CVD transform,
exactly the outcome #876 was written to prevent.
*Recommendation:* add `#876` (or its issue number once stabilized) to
`ordering_after`, or file the REPLAN on #507 that both sibling issues
already say is needed before either AC ships.

**3. [Medium] AC-4's "the display substrate the rest of the display-tagged suite uses" names an apparatus that conflicts with this project's own recorded architecture.**
`test/jls/ui/package-info.java` defines "the display-tagged suite" (Layer
2) as running under **`xvfb-run`** on CI (`-Djls.test.headless=false`) —
i.e., a real X11 server. But `README.md`'s "Optional development tools"
section states plainly: *"X11 is deliberately not part of this project's
tooling: no X server, no XWayland, no X11 utilities,"* and the project's
actual, already-shipping screenshot mechanism (issue #101,
`scripts/wayland-rig.sh`) uses a headless **sway/Wayland** compositor
plus `grim` — no xvfb anywhere in that pipeline. Layer 3, meanwhile
(where a Java2D-rendered semantic screenshot test would most naturally
live per the package-info taxonomy), is headless-BufferedImage and needs
no display server at all. AC-4 picks none of these explicitly, but its
wording ("the display substrate the rest of the display-tagged suite
uses") points at the xvfb apparatus specifically — the one candidate
that directly contradicts the recorded "no X11" project stance.
*Recommendation:* AC-4 should name the substrate explicitly (most likely
Layer 3's headless `BufferedImage` render, which needs no display server
and sidesteps the X11/Wayland conflict entirely) rather than deferring
to an ambiguous "the rest of the suite," which currently has at least two
non-equivalent, mutually exclusive candidates in the same codebase.

**4. [Medium] AC-3's red-run recording requirement is a narrative process step bolted onto an acceptance criterion, not something CI or a merged PR can verify.**
"A deliberately flattened encoding... turns the test red, and that red
run is recorded before any pass is counted" has no artifact requirement —
no CI transcript link, no committed log, no test-history assertion. As
written it's gameable exactly the way this lens is asked to hunt for: a
reviewer can accept "trust me, I saw it fail" in a PR description, or the
red run could be against a stale/different assertion than the one that
ships. Compare `scripts/wayland-rig-selftest.sh`, which pins its
failure-classification behavior in code against a stub toolchain rather
than asking a human to attest to having watched a failure once.
*Recommendation:* require the red run's evidence to be reproducible in
CI (e.g., a `@Disabled`-toggle or a companion test asserting the flattened
fixture fails, checked into the repo) rather than a claim in the PR
narrative.

**5. [Medium] AC-1's "every state pair" silently inherits an assumption about which states get a non-color encoding, and today's code doesn't support it for two of the five states.**
`WireRenderer.strokeFor` (`src/jls/edit/WireRenderer.java:43-53`) encodes
exactly three states via stroke: HiZ (dashed), non-zero (thick/round),
zero (thin). `WireRenderer.draw` (lines 63-72) selects the *touch* and
*highlight* colors independently of that stroke logic — a touched wire
and an otherwise-identical untouched wire of the same value get the same
stroke, differing only by color. So today, touch-vs-non-touch and
highlight-vs-non-highlight are **not** distinguishable by any non-color
channel, and #542's own review independently counts five color states
(touch/highlight/nonZero/wireOff/wireZero) against #731's differently-worded
"high, low, HiZ, bus value, error" list — neither list matches the other
or the code. AC-1 doesn't enumerate which states it means, so it inherits
whatever #731 ships without saying so, and if #731 doesn't add
touch/highlight encodings, this test's own AC-1 is unsatisfiable as
literally written ("every state pair").
*Recommendation:* enumerate the state set this test is scoped to (ideally
by reference to #731's registry once it lands, not a hardcoded prose
list), and confirm touch/highlight are either in scope with an encoding
plan or explicitly out of scope with a reason.

**6. [Low] No structural sub-issue link to the parent feature.**
`issue_read(get_parent, #734)` returns `null` despite `part_of_feature:
542` in the YAML front matter — the same "prose-only relationship" gap
#542's own review flagged for #729. An implementer browsing #542 in the
GitHub UI has no automatic way to discover this task exists.
*Recommendation:* link #734 as a real GitHub sub-issue of #542.

## What's solid

- The core distinction from the existing floor is well-drawn: AC-2's
  "not on residual colour difference surviving the filter" correctly
  targets the actual gap between `ThemeTest`'s in-memory palette
  delta-E check and a real anti-aliased render, which is a genuine blind
  spot the palette-level test cannot see.
- Reusing the existing display-tagged suite instead of building a second
  screenshot matrix (AC-4's intent, even if its wording is ambiguous per
  finding 3) is the right instinct given #76/#542/#543 already describe
  overlapping screenshot apparatus three times.
- The red-then-green discipline in AC-3 (even though its verification
  gap is finding 4) is the right shape of criterion for a
  distinguishability assertion — it forces the test to prove it can
  actually fail before anyone trusts a pass.

## Verdict

**needs-rework.** The concept is a legitimate tightening over the
existing palette-only floor, but the issue asks for a test over a
fixture and an apparatus that don't exist and aren't budgeted (finding
1), depends on a CVD transform owned by a sibling task-cluster without
declaring that dependency in its own `ordering_after` (finding 2, and
the conflict is already self-disclosed by the sibling issue), names a
display substrate that is internally ambiguous and one reading of which
contradicts this project's recorded "no X11" stance (finding 3), has an
unverifiable process step standing in for a CI-checked one (finding 4),
and silently inherits a state-set assumption the current code doesn't
fully support (finding 5). None of this kills the feature; all of it
needs a body edit — plus a REPLAN on #507 for the ordering conflict —
before implementation starts.
