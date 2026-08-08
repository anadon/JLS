# Issue #539: FEAT-C24-4: N clock cycles of a canvas region become a deterministic APNG/GIF with signal-value overlays, encoded in pure Java
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

#539 is PF-4 of the CAP-24 (#505) print-figure capstone: an instructor-facing
animated capture (APNG, with a GIF alternative) of N clock cycles of a canvas
region, with signal-value overlays, derived from a recorded run. The issue
carries a "Disposition note" up front stating that #508 (the August 2026
product-direction review) recommends *cutting* PF-4 and that the issue exists
only so the cut can be adjudicated explicitly via REPLAN on #505 — "Do not
fund ahead of that REPLAN." That framing, verified against #508's actual body
below, is accurate. The problems are in the acceptance criteria and cited
support underneath it.

## Findings, most severe first

**1. (High) The acceptance criteria depend on an input format — "a recorded
run artifact" — that does not exist anywhere in the codebase, and the issue
never says what it actually is.** `grep`ing `src/` and `test/` for
`Recording`/`RecordedRun`/`SessionRecording`/`Replay`-style classes and for
the phrase "recorded run" anywhere in `ARCHITECTURE.md` or the normative
`docs/*.md` returns nothing. The phrase is borrowed from #498 §7.2 (rescued
verbatim from `docs/virtual-hardware-parity.md`), which describes a **future,
unbuilt** milestone (M2: "A GUI session records and replays in batch
byte-identically... `SessionBoundaryRatchetTest`") in a document whose own
header states plainly: **"It is explicitly non-normative... Nothing in it may
be cited as settled policy."** So AC-1/AC-4's load-bearing input — "recorded
run" — is either (a) the existing VCD/`BatchSimulator` trace-sample export,
which already exists and could be named, or (b) the not-yet-built M2 session
recording, which does not. The issue picks neither and leaves an
implementer to guess which contract to build against.
Recommendation: name the concrete artifact type (existing VCD export?
`TraceSample` list? a new bespoke format?) or explicitly block filing on M2
landing first.

**2. (High) AC-1's test fixture, "the hazard-demo run," does not exist in the
repository and is not scoped as part of this issue.** `test/fixtures/`
contains exactly `riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`, and
`headless-canary-gate.jls`; `riscv/gui/cpu.jls` is the only other circuit
file in the tree. No "hazard" or "hazard-demo" circuit exists anywhere
(confirmed by a repo-wide grep, filtering out the many unrelated
"race/glitch hazard" documentation hits in `docs/capability-roadmap/**`,
`docs/simulation-semantics.md`, etc., none of which is a circuit fixture).
The issue's own `ordering_after: []` frontmatter names no dependency that
would create this fixture. AC-1 is therefore unimplementable as literally
written until a fixture appears from somewhere unstated.

**3. (High) AC-4's supporting citation is misapplied — I fetched #498 and
read §7.2 directly.** AC-4 reads: *"aligning with the recording-is-the-
contract discipline (#498 §7.2)."* §7.2 in full is titled *"`docs/vcd-
interop.md` and #63 — recording, not reopening"* and is a policy correction
about whether **live interactive GUI grading** or **batch replay of a
transcript** should count as the supported autograding surface in the
~150-KB virtual-hardware/Linux-boot design study — a question about
interactive-vs-batch *grading determinism*. It contains zero mentions of
figures, exports, schematics, animation, or multi-artifact composition. The
borrowed phrase "the recording, not the session, is the contract" is a
different sentence about a different problem. This is not a new mistake:
the sibling issue #541 (same CAP-24 cluster, same author, filed the same
day) makes the identical citation for the identical reason and was
independently flagged for it in that issue's own review — #539 repeats the
error rather than establishing its own, feature-specific rationale for why
the animation must come from one fixed recorded run (which is a sound
requirement on its own merits; it just is not what #498 §7.2 says).

**4. (Medium) AC-2's "declared size budget" is never declared.** *"a
32-cycle APNG capture has deterministic frame count and timing metadata and
stays under a declared size budget"* — no number, range, or unit appears
anywhere in the issue. A criterion phrased as "stays under X" with no X is
not falsifiable: an implementer can set the budget to an arbitrarily large
value (or omit an actual enforced check and just assert against the
observed output size) and trivially satisfy the letter of AC-2 while the
real goal — a reasonably sized course-repo artifact — goes unverified.

**5. (Medium) AC-1's actual claims are not covered by any named test, and
AC-2 doesn't cover what AC-1 promises.** Only AC-2 names a test
(`AnimationCaptureTest`, inherited verbatim from #505's AC-5), and its scope
is limited to a 32-cycle capture's frame count, timing metadata, and size —
it says nothing about signal-value overlay content, the hazard-demo
scenario specifically, or the GIF format at all. AC-1's three distinguishing
claims — hazard-demo input, GIF alternative, signal values actually
overlaid and correct — have no machine-checked criterion anywhere in the
issue. This is exactly the gameable-acceptance-criteria pattern the fleet
has already caught on sibling issues in this same cluster (#541, #875): an
implementation could ship `AnimationCaptureTest` green while never producing
a correct GIF or a single legible overlay, and by the issue's own written
contract that would count as done.

**6. (Medium) The "pure Java" cost is understated and asymmetric between the
two formats the issue treats as a pair.** The only existing image-export
code path (`src/jls/edit/CircuitRenderer.java:376-382`) calls
`ImageIO.write(image, format, file)` for `png`/`jpg` only — no `gif`, and
nothing APNG-related exists anywhere in `src/` (confirmed by grep for
`ImageIO|GifWriter|PngWriter|acTL|fcTL|fdAT` across `src/`). The JDK's
bundled `ImageIO` PNG writer does **not** support the `acTL`/`fcTL`/`fdAT`
animation chunks APNG requires — producing a conformant APNG in pure Java
with no new dependency means hand-writing the animation chunk layer on top
of (or instead of) `ImageIO`'s single-frame PNG writer. GIF, by contrast, has
a JDK-bundled `ImageIO` writer already and is comparatively close to free.
The issue's title and AC-1 present "APNG (and GIF alternative)" as a matched
pair of near-equal cost; the actual engineering lift is lopsided, and
nothing in the issue or in #505's cost line (PF-4: "2-3 mw") calls that out
as the likely risk driver, unlike PF-1's analogous text-metrics risk which
#505 §3 flags explicitly as "the known-hard part."

**7. (Medium) The issue simultaneously says "don't build this" and hands a
contributor implementation-grade, testable acceptance criteria.** The
disposition note's instruction is unambiguous — "Do not fund ahead of that
REPLAN" — but AC-1 through AC-4 read like a normal, ready-to-implement
feature spec, including a concrete test class name. A contributor who skims
past the disposition note (reasonable: it is prose above a structured AC
section, not a machine-enforced block) has everything needed to start work
against the maintainer's stated intent. Recommendation: gate the AC section
itself behind the REPLAN, e.g. a literal "criteria below are provisional
and non-binding until REPLAN" marker on the AC heading, not only in the
preceding paragraph.

**8. (Low) "Signal-value overlays" is undefined and the determinism bar
around it is inconsistent with the rest of the capstone.** No specification
of which signals are overlaid (all watched signals? a user-selected subset?
does bus width affect layout?), what encoding (hex/binary/decimal), or
where per-frame. #505 §3 risk 1 treats cross-platform byte-identical text
rendering as **the** known-hard risk for the sibling PF-1 (SVG/PDF
schematic) — the same rendered-text problem necessarily recurs for overlay
text baked into animation frames, yet #539's AC-2 tests only frame count,
timing metadata, and size, never byte-identity across platforms. Either the
overlay text is exempt from the capstone's general determinism bar (#505
AC-2), and that exemption should be stated, or it is not and AC-2 is
missing a cross-platform check.

**9. (Low) Label plausibly wrong.** `area:gui` is applied, but the Outcome
describes a capture "from a recorded run" — the same batch/CLI-oriented
framing the sibling PF issues in this cluster use for their producing
artifacts (already flagged as a likely mislabel one level up, in #541 AC-4's
"available headlessly for CI/course-repo use" requirement, and independently
in #874's review). If PF-4 is meant to be invokable headlessly like its
siblings, `area:batch` (or both labels) fits better.

## What holds up

- The disposition note's quote of #508 is accurate: I fetched #508 directly
  and its capstone-disposition text reads verbatim *"CAP-24 #505 figure-export
  slice — 2-3 mw slice retires the hard risk; **cut PF-4 animation**; ≈9-14 mw
  realistic"* — #539 is not misrepresenting its own cited authority, unlike
  several findings above about how it applies other citations.
- AC-2's test name and description are a faithful, exact copy of #505's own
  AC-5 (`AnimationCaptureTest`, "32-cycle APNG capture has deterministic
  frame count and timing metadata and stays under a declared size budget"),
  so the feature-level issue is internally consistent with its parent
  capstone at least at that one point.
- The MP4-exclusion boundary note matches #505's Open Question 4 exactly
  (native encoders violate the pure-Java/single-jar constraint), and is a
  genuinely well-motivated, non-negotiable constraint given how the project
  ships (single self-contained jar, documented in README.md's installer
  section) — no scope creep here.
- Filing the issue at all, rather than silently dropping PF-4, is the
  correct process move given #505's own Re-planning Protocol requires "Every
  response ends in a `REPLAN:` comment" and planned features resolve "via
  REPLAN when filed" — #539 is doing exactly the bookkeeping #505 asks for.

## Verdict rationale

`needs-rework`: the scope boundary and the disposition-note honesty are
sound, but the acceptance criteria underneath it are not implementable or
verifiable as written. Two of the four ACs depend on artifacts that do not
exist in the repository and are not brought into scope by this issue
(the "recorded run" input format, the "hazard-demo" fixture); the one
citation meant to justify the recorded-run requirement (#498 §7.2) does not
support it on inspection; the one declared size budget is never actually
declared; and the issue's headline claims (GIF alternative, correct
overlays, the named demo scenario) have no test coverage at all, leaving
only a narrow slice (frame count/timing/size on APNG) machine-checked. None
of this is a case for `should-not-proceed` — the underlying feature concept,
its boundary against MP4, and its honest disposition framing are all
defensible — but a REPLAN or an editing pass should fix the citation, name
the real input format, name or file the missing fixture, state the size
budget, and add test coverage for GIF and overlay correctness before any
implementation starts.
