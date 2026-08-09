# Issue #729: TASK-C542-1: tritanopia joins the verified colour-vision set at the existing delta-E floor, for every shipped theme
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

#729 (TASK-C542-1) proposes adding a tritanopia LMS transform alongside
`ThemeTest`'s existing deuteranopia/protanopia matrices and extending the
25 CIE76 delta-E distinguishability floor to it, "for every theme
constant that exists at the time of landing," iterated rather than
hardcoded. The methodology it extends is real and already working in the
codebase (`test/jls/ThemeTest.java`). But the acceptance criteria, read
together against the actual `Theme` class, demand something the codebase
currently treats as neither testable nor desirable — a passing tritanopia
floor for `Theme.CLASSIC`, a theme whose entire reason for existing is to
reproduce the pre-#76 palette byte-for-byte and which is *already*,
intentionally, allowed to fail the same floor under deuteranopia.

## Findings, most severe first

**1. [High] AC-2 + AC-3, read together, require adjusting `Theme.CLASSIC`'s palette — directly contradicting the class's own documented invariant and its own pinning test.**
AC-2: "the assertion covers tritanopia... for every theme constant that
exists at the time of landing." AC-3: "the assertion iterates the shipped
theme set rather than a hard-coded list." `Theme.all()` returns
`List.of(DEFAULT, CLASSIC)` (`src/jls/Theme.java:86`), so "every theme
constant"/"the shipped theme set" is two themes, not one. But
`Theme.java:69-72`'s own javadoc states `CLASSIC` "reproduces the
original JLS palette exactly... so no user is forced off the old
colors," and `ThemeTest.classicReproducesTheLegacyPalette`
(lines 42-54) pins its ten fields to `Color.green`/`Color.pink`/
`Color.red`/etc. exactly. More directly:
`ThemeTest.classicValueVersusTouchPairCollidesUnderDeuteranopia`
(lines 111-125) is a *passing* test that asserts CLASSIC's red/green pair
**collides** under deuteranopia today — this is documented as "the
provable defect at HEAD," accepted deliberately for legacy continuity,
not a bug pending a fix. AC-4 then says "if a shipped theme fails under
tritanopia... the palette is adjusted — the floor is not lowered... because
it is new." Applied literally to CLASSIC (which AC-2/AC-3 put in scope),
this requires either (a) changing CLASSIC's colors — breaking
`classicReproducesTheLegacyPalette` and the "exactly" promise in
`Theme.java:71`, or (b) silently carving CLASSIC out of the iteration —
which falsifies AC-3's own stated rationale ("a theme added later is
covered without editing the test") since CLASSIC is *already* shipped and
already known to fail an equivalent floor. The issue never states which
of these it wants; as written it is self-contradicting against the
existing green test suite.
*Recommendation:* explicitly scope AC-2/AC-3 to `Theme.DEFAULT` only (matching
what `defaultWireStatesStayDistinguishableForDichromats` already does for
the two existing filters), or add an explicit CLASSIC-exemption clause
with a citation to the deuteranopia precedent, before "iterates the
shipped theme set" is implemented literally.

**2. [High] AC-1's "derivation cited" is gameable, and a single fixed 3×3 matrix is a known-weaker simplification specifically for tritanopia.**
The existing `DEUTERANOPIA`/`PROTANOPIA` constants (`ThemeTest.java:158-167`)
are literal matrices with a one-line comment ("Missing-M-cone projection
in LMS space") and a class-level mention of "Vienot et al. 1999" — no DOI,
page, or formula walkthrough. Viénot, Brettel & Mollon (1999) is
specifically about protanopia and deuteranopia; the commonly cited method
for simulating tritanopia (Brettel, Viénot & Mollon 1997) uses a
piecewise transform (two matrices selected by which side of a separation
plane through the confusion point a color falls on), because the tritan
confusion locus does not admit the same single-projection trick the other
two dichromacies do. AC-1 as written ("a tritanopia transform is added
alongside the existing... transforms, with its derivation cited") does
not require validating that a single-matrix simplification is adequate
for tritanopia, nor does it set a bar for what "cited" means beyond what
the existing two constants already do informally. An implementer can add
any plausible-looking 3×3 array with a one-line "// Vienot 1999"-style
comment, and both the letter of AC-1 and the mechanical test in AC-2 will
pass regardless of whether the matrix is a defensible tritanopia
simulation.
*Recommendation:* name the specific paper/formula and require the
citation to include enough (page/equation number, or a URL) that a
reviewer can check the matrix against the source, and explicitly discuss
whether a single-matrix approximation is acceptable for tritanopia or
whether the piecewise Brettel et al. 1997 method is required.

**3. [Medium] The transform is scoped to `ThemeTest` (test-only), but a sibling issue in the same cluster needs the same three matrices in production code, and #729 sets up no shared source of truth.**
`issue-reviews/issue-0543.adversarial.md` (finding 5, already on file)
documents: "`ThemeTest.java:158-167` already hardcodes
protanopia/deuteranopia matrices for #542's delta-E ratchet, #729 (open)
will add tritanopia to that same test-only set, and #876 separately needs
all three matrices in production code — with no shared source of truth
required by any issue's acceptance criteria." #729's AC-1 only requires
the transform be added "alongside the existing... transforms" — i.e.,
inside `ThemeTest` as another private `double[][]` constant — with no
requirement to place it somewhere importable by non-test code. If #876
(the live in-app CVD preview) lands after #729, the two either duplicate
the tritanopia matrix with no test enforcing they stay numerically
identical, or #876 has to refactor #729's placement. #729's own
`ordering_after: []` declares no dependency on this, despite the
in-fact dependency #543's review already surfaced.
*Recommendation:* either place the new transform (and ideally the
existing two) in a small shared, non-test utility class that both
`ThemeTest` and any future production CVD-preview code can import, or
explicitly note in the issue that test-only duplication is accepted and
name the owner responsible for reconciling it with #876 later.

**4. [Medium] The parent link (`part_of_feature: 542`) is prose-only; the structural relationship doesn't exist on GitHub.**
`issue_read(get_parent, #729)` returns `null`. The companion review of
#542 independently found `issue_read(get_sub_issues, #542)` returns `[]`
and that #542's own AC-1 is a near-verbatim, uncited restatement of
#729's Outcome. So the only place the #542↔#729 relationship exists is
the YAML front-matter block inside #729's own body — an implementer
who finds #729 through search or the issue list, rather than through
#542, has no structural cue that a broader capstone (#542/CAP-26/#507)
constrains how "shipped theme set" and "palette is adjusted" should be
interpreted (e.g., whether CLASSIC's known deuteranopia failure is
meant to be grandfathered under the wider feature's rules).
*Recommendation:* file #729 as a real GitHub sub-issue of #542 (and vice
versa) rather than relying on a machine-readable comment nobody but a
tool reads.

**5. [Low] AC-4's "the failure is recorded" names no artifact, so it can't be verified — and the project already has a mechanism for exactly this that the issue doesn't point at.**
"the failure is recorded and the palette is adjusted" specifies neither
where a failure gets recorded nor what counts as adequate. Nothing in
`mvn verify`'s test suite can check that a "record" exists; a PR could
satisfy the letter of AC-4 with a commit message alone, or with nothing
at all beyond the color values changing. `ARCHITECTURE.md`'s own
"Recorded decisions" section (e.g. the FlatLaf and plugin-mechanism
entries, each with a dated header, rationale, and revisit trigger) is
this project's established convention for exactly this class of
deliberate-compromise documentation, and #729 doesn't cite it.
*Recommendation:* point AC-4 at that mechanism explicitly ("add a Recorded
decision entry to `ARCHITECTURE.md`") so the criterion is checkable by a
reviewer instead of resting on good faith.

**6. [Low] `band_mw: 0.5-1` assumes the work is purely additive test coverage; AC-4's palette-adjustment contingency is unbudgeted.**
If `Theme.DEFAULT` (the only theme that can plausibly satisfy AC-2/AC-3
per finding 1) fails under tritanopia, satisfying the floor means
re-tuning colors that already satisfy 15 wire-state pairs × 2 existing
filters × the >100 delta-E flagship-pair test — a joint color-optimization
problem, not a one-line matrix addition. The stated 0.5-1 maintainer-week
band reads as if AC-4's contingency branch never fires.
*Recommendation:* either scope the estimate to the pure-addition case and
flag the palette-redesign branch as unbounded/needing its own estimate,
or spend a cycle checking whether DEFAULT actually needs adjustment
before committing to the band.

## What's solid

- The core technique — extend the existing Vienot-LMS/CIE76 methodology
  that already works for two dichromacies to a third — is sound and
  follows a pattern the codebase already executes cleanly
  (`ThemeTest.java:80-125`).
- AC-3's underlying principle (iterate `Theme.all()` rather than
  hardcode theme names, so new themes are covered by construction) is a
  genuinely good engineering habit, independent of the CLASSIC conflict
  it currently produces.

## Verdict

**needs-rework.** The methodology is sound and the extensibility
principle (AC-3) is good practice, but AC-2/AC-3/AC-4 combined currently
demand an outcome (CLASSIC passing, or being silently exempted from, a
floor it is already-and-intentionally allowed to fail) that contradicts
an existing, passing, documented test and the `Theme` class's own stated
purpose for CLASSIC. AC-1's citation bar is also loose enough to be
satisfied without addressing the real technical wrinkle that tritanopia
simulation is not simply "a third matrix" in the same form as the other
two. Scope AC-2/AC-3 explicitly (finding 1), tighten AC-1's citation
requirement (finding 2), and name a shared-ownership plan for the
matrices given #876's production-code need (finding 3) before landing.
