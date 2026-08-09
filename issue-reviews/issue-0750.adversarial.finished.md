# Issue #750: TASK-C546-3: one command emits narrative and tactile SVG together for the same circuit, byte-identically on every platform
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

One CLI command emits, for a single circuit, both the prose narrative
(TASK-C546-1, #747) and the tactile SVG (TASK-C546-2, #749) as a
bundle, byte-identical across Linux/macOS/Windows CI, default-pair
with an explicit-flag opt-out for partial output. This is the
integration task at the top of the FEAT-C546 (#546) / CAP-26 (#507)
chain.

## Findings, most severe first

### 1. [Critical] AC-2's cross-platform byte-identity claim contradicts this codebase's own documented precedent, and the issue misstates that precedent

The issue asserts: *"Determinism is asserted the way every other
export in this project asserts it — identical bytes on Linux, macOS
and Windows."* That is not accurate for the artifact this task is
actually bundling (the SVG). The closest existing analogue,
`test/jls/SvgExportTest.java:16-25`, says the opposite in so many
words:

> "Deliberately no full-document golden - text layout coordinates
> depend on the JDK's font metrics, which differ across machines (the
> same reason `CliImageExportTest` avoids pixel goldens)."

That test only asserts byte-identity *within one run on one machine*
(export twice, or export from several fresh loads on the same host) —
never across the three CI platforms. `docs/reproducibility.md:172-192`
independently documents that this project already tried and explicitly
gave up on full cross-platform byte identity for platform-rendered
artifacts: the `msi` and `dmg` installers are "not yet reproducible,"
for reasons rooted in platform toolchain divergence, and each carries
its own dedicated tracking issue (#190, #191) rather than being waved
through. The exports that genuinely are byte-identical across
platforms in this project — VCD (`test/jls/VcdExportGoldenTest.java`),
Verilog, plain-text save — are all pure text with no glyph rendering
in the pipeline. `grep -r "createFont\|DejaVu" src/` turns up nothing:
JLS bundles no font, so a Windows or macOS CI runner rendering the same
circuit will reach for whatever system font is present there, not the
Linux runner's DejaVu (`README.md:218-219` documents DejaVu as a
Debian-package dependency, not a bundled asset). A tactile SVG that
draws labels the way the existing `CircuitRenderer` SVG path does will
hit exactly the font-metric divergence `SvgExportTest` was written to
route around, and the CI matrix already has all three OSes
(`.github/workflows/ci.yml`: `ubuntu-latest`, `macos-latest`,
`windows-latest`) so the failure would surface immediately, not
theoretically.

**Recommendation:** either (a) specify that the tactile SVG's text
content is emitted as fixed vector paths / a bundled embedded font
rather than AWT-rendered glyphs — a real design commitment this issue
doesn't make and #749 doesn't make either — or (b) narrow AC-2 to
"byte-identical per OS family" (matching the actual, narrower guarantee
this project ships for the deb/rpm/AppImage lane) and correct the
"the way every other export... asserts it" framing, which currently
overstates settled precedent that doesn't exist for rendered output.

### 2. [High] AC-1's "documented layout" is unspecified and gameable

*"one command emits both artifacts for the same circuit, into a
documented layout"* names no file-naming scheme, no directory
convention, nothing to check mechanically beyond "two files exist and
some doc mentions where." Contrast with #749's AC-2, which is concrete
enough to fail a specific bad input ("a deliberately too-thin line or
too-tight spacing fails the lint"). As written, AC-1 could be
satisfied by any ad hoc pairing (`out.txt` + `out.svg` in the cwd, or a
`out/` directory, or a zip) plus a paragraph in a markdown file calling
it documented — a test named `AccessibleBundleTest` built against
whatever the implementer happens to choose would trivially pass and
tell a reviewer nothing about whether a course repo's tooling can
actually consume the pairing programmatically (matching narrative to
SVG by filename, e.g.). **Recommendation:** pin the layout in the
issue itself (e.g., `<circuit-basename>.narrative.txt` +
`<circuit-basename>.tactile.svg` in one target directory, or a single
manifest file listing both), the way `docs/batch-interface.md` pins
the `-t`/VCD contracts elsewhere in this project.

### 3. [Medium] AC-3's "reachable from CI and a course repo" is not a falsifiable criterion

"The command runs headlessly and is reachable from CI and a course
repo" bundles one testable claim (headless — no AWT touched, checkable
the way `HeadlessCoreRatchetTest` does for the simulator per
`ARCHITECTURE.md:54-56`) with two that aren't: "reachable from CI" and
"reachable from a course repo" are true of essentially any CLI flag
JLS ships, including ones with no relation to accessibility. There is
no proposed flag name, exit-code contract, or stdout/stderr shape
(compare the CLI contract recorded in `ARCHITECTURE.md:197-202`: one
`jls: error: ...` line, exit 0/1/2). Without that, "reachable" can't
be distinguished from "exists." **Recommendation:** state the actual
flag surface (e.g. `-accessible-export [-narrative-only|-tactile-only]
out-dir circuit.jls`) and fold it into `CliFlagTableTest`/
`docs/batch-interface.md` the way every other batch flag in this
project is specified, rather than leaving it as a prose aspiration.

### 4. [Medium] The chain this task sits atop assumes infrastructure not yet shown to exist

#747 (TASK-C546-1, the narrative half this task bundles) requires
"every element type in *the registry* contributes a describable
phrase; an unmapped type fails the build" — but `ARCHITECTURE.md:115-118`
records, as of HEAD, "There is no element registry yet — issue #78
will introduce one and collapse most of this." If #747's "registry" is
#78's not-yet-built element registry, #750 cannot close AC-1 (which
requires #747 to have landed, transitively via #749's own
`ordering_after: [TASK-C546-1]`) until #78 lands too — a dependency
that appears nowhere in #750's `ordering_after` or CAP-26's blocking
graph. This may be a naming collision rather than a real blocker (the
"registry" in #747 might mean #542/#542's wire-state-encoding registry,
which is a different, narrower thing already staged via
`ordering_after: [TASK-C542-2]`) — but the issue text doesn't
disambiguate, and it's exactly the kind of ambiguity that turns into a
silent scope surprise for whoever implements #750 last in the chain and
discovers the artifact they're bundling doesn't build.

### 5. [Low] Band size doesn't obviously cover finding #1's real cost

`band_mw: 0.5-1` reads as "wire two existing outputs together and add
a CI assertion." If AC-2 is taken literally (true cross-platform byte
identity for a glyph-rendered SVG), the fix belongs upstream in the
rendering pipeline (embedded font or full path-vectorized text) — work
of a different order than 0.5-1 mw, and not costed anywhere in #546 or
#507 either (#546's own cost section covers PF-4 at 3-4 mw total, all
narrative+lint+SVG work, none earmarked for font/rendering
determinism). This is the same risk as #1, restated as a planning gap
rather than a technical one.

### What's solid

- AC-4 (single-command default pair, explicit flag for a partial
  bundle) is concrete and directly testable — no notes.
- The test-name traceability back to CAP-26 (#507) is exact:
  `AccessibleBundleTest`/AC-3 and `AccessibleExportDeterminismTest`/AC-6
  both match #507's §4 verbatim, and the `ordering_after` chain
  (#750 → #749 → #747 → #542's TASK-C542-2) is internally consistent
  once traced, not contradictory on its face.
- Scope boundary against #536/#540 (camera-ready visual export) is
  inherited correctly from #546 and not re-litigated or blurred here.

## Verdict rationale

The headline acceptance criterion (byte-identical tactile SVG across
three OSes) is asserted as settled precedent this project already
proved out — it is not; the nearest actual precedent
(`SvgExportTest.java`) was written specifically to avoid making that
claim, for a documented, still-unaddressed reason (JDK font-metric
divergence, no bundled font). Two of the four acceptance criteria
(layout, reachability) are underspecified enough to pass without
proving the thing a course repo or CI actually needs. None of this
means the outcome is wrong — a single accessible-bundle command is a
sound idea and AC-4 is well-formed — but the issue needs the
determinism claim corrected or the SVG rendering approach specified,
and AC-1/AC-3 need concrete, checkable definitions before implementation
starts.
