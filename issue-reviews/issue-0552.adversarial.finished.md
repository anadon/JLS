# Issue #552: FEAT-C27-5: the first three circuits teach themselves — stepped build-along lessons a newcomer completes from on-screen prompts alone
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-bounded against its two sibling features (#548, #550) and its
sibling capstone (#517/CAP-33) — the maintainer's own follow-up comment already
does useful deduplication work. But the acceptance criteria that matter most
(AC-2, AC-5) lean on artifacts that are either not defined precisely enough to
test, or not present in this checkout to verify against, and the "in-tool"
delivery option is scoped against a codebase seam that does not exist yet.

## Findings, most severe first

**1. (High) AC-2's verification instrument measures the wrong thing, and may not exist when this feature needs it.**
AC-2 says lesson 1's no-external-reading completability is "verified by the
capstone's scripted fresh-user protocol." That protocol is defined one level
up, at #511 (CAP-27) AC-2: *"A scripted fresh-user protocol (documented,
re-runnable) measures install→running-example in <10 minutes on Windows,
macOS, Linux."* That is a stopwatch instrument for the install→first-run
funnel, not a comprehension check for whether a specific lesson's prompts are
self-sufficient. A run could complete inside 10 minutes because the tester
already knew JLS, or could complete slowly-but-successfully by reading the
README — the protocol as described doesn't distinguish "the prompts alone
sufficed" from "the tester happened not to need anything else." Nothing in
`test/jls/ui/package-info.java`'s three-layer harness (headless model
asserts, Swing/Xvfb interaction, render-to-image) does comprehension testing
either — Layer 2/3 can simulate clicks, not judge whether a human found a
prompt clear. As written, AC-2 is gameable: a scripted click-through that
exercises every prompt in order will "pass" a timing-based protocol without
ever testing intelligibility, which is the actual claim AC-2 makes.
Additionally, this feature's `ordering_after` list does not include #511
itself, so it's unclear whether the fresh-user protocol is guaranteed to
exist by the time #552 needs to invoke it for its own acceptance.
*Recommendation:* either specify a distinct verification method for "no
external reading required" (e.g., an explicit n=5 usability trial in the
#381 style, with outcomes recorded as prose rather than a green bar — #381
itself warns "a green bar is not a user, and the project has already
mistaken one for the other once"), or narrow AC-2's claim to what the timing
protocol can actually show.

**2. (High) The K9/D9 gate this issue relies on (AC-5) is not locatable in this checkout.**
AC-5 says "K9/D9 holds" and paraphrases it as "the default editing view gains
no new chrome." The only place the actual K9/D9 substance is spelled out is
#381: *"Maintainer directive K9, as restated by D9, governs the palette and
disclosure work... D9 is recorded in `docs/plan/evidence/BRIEF.md` §13, which
landed in `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`."* Neither the file nor
the commit exists in this repository — `docs/plan/evidence/BRIEF.md` is
absent (confirmed via glob) and `git log` reports `3a81a4a7...` as `fatal:
bad object` (unreachable). #552's own paraphrase ("no new chrome") is a
reasonable proxy, but an implementer or reviewer checking AC-5 against the
named authority cannot actually do so — the citation is dead in this tree.
*Recommendation:* quote the operative clause inline, the way #381 did
("the first-year must never SEE the ECE/EE machinery, but the machinery may
exist"), so the acceptance criterion is self-contained and doesn't depend on
an artifact this repo can't produce.

**3. (Medium) `ordering_after` omits #550, but AC-5 depends on the welcome pane #550 owns.**
AC-5 requires lessons be "entered deliberately (from the welcome pane or
Examples menu)." The welcome pane is FEAT-C27-3 (#550), which is itself
gated on #381 (not yet landed — confirmed: no `welcome`/`firstRun` symbol
exists anywhere in `src/jls/`, per #381's own O3 observation, still true at
HEAD). Yet #552's YAML frontmatter lists only `ordering_after: [548]`. If
#552 is picked up once #548 lands but before #550, half of AC-5's two
prescribed entry points (welcome pane) won't exist, and the feature can only
partially satisfy its own acceptance criterion. The issue body itself
acknowledges consuming entry points from both #550 and #548 ("Entry points
are consumed from FEAT-C27-3 (#550...) and FEAT-C27-2 (#548...)") — the
prose and the machine-readable ordering metadata disagree.
*Recommendation:* add 550 to `ordering_after`, or state explicitly that the
Examples-menu entry point alone is sufficient to start and the welcome-pane
entry point may land later.

**4. (Medium) "In-tool" delivery is scoped against a UI seam that doesn't exist, inside a fixed 3-4 mw band.**
The outcome text ("draw this, wire that, run it, watch here") describes
step-by-step guidance that reacts to what the learner has actually drawn —
implying live verification against editor/circuit state. But the only
existing in-tool sequential-content mechanism is `src/jls/Tutorial.java`: a
static paged `JEditorPane` dialog with Previous/Next buttons over bundled
HTML, with no connection to `SimpleEditor`'s state machine, no way to detect
"the learner placed the right gate" or "wired the right net." Building
genuine reactive step-checking would mean new instrumentation on the
5,852-line `SimpleEditor` (ARCHITECTURE.md's own count, flagged in #510 as
already the codebase's weakest elegance spot) — a materially different, more
expensive task than re-skinning `Tutorial.java` with more pages and content.
AC-4's content/presentation separation (KC-27-2) correctly hedges against
this by allowing the docs-pages fallback, but the issue never says which of
these two very different "in-tool" shapes the 3-4 mw band assumes, so the
estimate is only trustworthy for whichever one turns out to be intended.
*Recommendation:* state explicitly whether "in-tool" means (a) extending the
existing static `Tutorial` paging pattern, or (b) live circuit-state-aware
prompting: they have very different costs and only one is cheap enough to
plausibly fit the stated band.

**5. (Medium) No licensing statement for lesson content, despite explicit reuse by CAP-33.**
The Boundary section says "Content is shared with CAP-33's (#517) course
kits." #517 AC-4 in turn requires "Kit content carries clean licensing
(course materials under a stated open license distinct from code)." #552
authors that shared content but says nothing about what license it ships
under. Absent a statement, the lesson prose defaults to the repository's
GPLv3-or-later code license (per README's "Contributions are accepted under
the same terms"), which may not be the license CAP-33 wants for
"course materials... distinct from code." This is exactly the kind of gap
that surfaces late, when CAP-33 tries to redistribute the content and finds
its upstream source never declared a license compatible with that reuse.
*Recommendation:* state the intended license for lesson prose (even if it's
just "same as code, GPLv3-or-later, revisit if CAP-33 needs different terms")
before authorship starts.

**6. (Low-medium) "No longer than a caption" (AC-1) is an unquantified, easily-gamed bound.**
No file in the repo defines a caption length in characters/words (the
closest analogue, #548 AC-4, uses the identical unquantified phrase for
per-example exercise text). Two different reviewers could disagree on
whether a given step complies, and an implementer under schedule pressure
can trivially "satisfy" the letter of AC-1 with steps so terse they fail the
spirit of AC-2 (self-sufficient without external reading). *Recommendation:*
pin a concrete bound (e.g., character count matching whatever #548 lands on
for its captions) so the two features share one operational definition.

**7. (Low) AC-3's closing one-liner overlaps #548's per-example caption without a stated resolution.**
The maintainer's own comment resolves the *step 1 vs. #548 exercise line*
overlap ("authored under #548 and referenced here") but AC-3 is about the
*lesson's closing* statement, not step 1, and comment's disambiguation
doesn't cover it — it's still unstated whether AC-3's closing line must be
the identical string as #548's caption or an independently authored one.

## What holds up

- The #73/#548 non-duplication argument (recorded in the issue's own
  comment) is concrete and checks out against the actual landed PR #194 —
  worth taking at face value.
- The factual claim about existing tutorial content is accurate:
  `src/jls/Tutorial.java` does define exactly 4 pages ("Introduction",
  "4-Bit Counter", "Full Adder", "Sign Extension"), matching "the existing
  Help→Tutorial 4-page content."
- KC-27-2's structural requirement (author content separately from
  presentation) is sound engineering hygiene independent of which delivery
  mode wins, and is testable in principle regardless of finding 4 above.
- No security hazard: this is documentation/UX content with no new file
  format, network, or trust-boundary surface.
