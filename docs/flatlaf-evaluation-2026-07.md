# FlatLaf Evaluation (#153)

*July 2026. The investigation issue #153 tracks whether JLS should adopt
[FlatLaf](https://github.com/JFormDesigner/FlatLaf) as its Swing
look-and-feel, superseding the recorded "force cross-platform Metal, same
everywhere" decision (`JLSStart.java`, ergonomics audit finding U4). This
document records what was measured, what was audited, what could not be
verified in a headless environment, and the resulting recommendation. It
is the evaluation deliverable named in the last checkbox of #153; the
one-line verdict is mirrored in
[`library-survey-2026-07.md`](library-survey-2026-07.md) §1.*

## What was measured (verified 2026-07-18, JDK 25.0.3, Linux)

**Artifact.** `com.formdev:flatlaf:3.7.2` (released 2026-07-09,
Apache-2.0), pulled from Maven Central: **1,016,186 bytes**, 405 entries,
and its POM declares **zero runtime dependencies** — the "one jar, no
transitives" claim in the library survey holds.

**JDK 25 behavior, headless.** A spike program on OpenJDK 25.0.3 with
`java.awt.headless=true`:

- `UIManager.setLookAndFeel(new FlatLightLaf())` installs cleanly; real
  components (`JButton`, `JMenuBar`, `JTabbedPane`, `JTextField`,
  `JEditorPane` — the widget set JLS's dialogs and help viewer use)
  instantiate with FlatLaf UI delegates (`FlatButtonUI`).
- Runtime theme switching works: installing `FlatDarkLaf` plus
  `SwingUtilities.updateComponentTreeUI(...)` flips the palette
  (`Panel.background` 242,242,242 → 60,63,65) with no restart. This is
  the mechanism a future light/dark toggle would use.

**Through JLS's own code path.** The `-Djls.laf` selection seam added to
`JLSStart` by this issue (see "What shipped" below) was driven end to end:

- `java -Djls.laf=com.formdev.flatlaf.FlatLightLaf -cp <jls>:flatlaf-3.7.2.jar ...`
  → FlatLaf Light installed; same for `FlatDarkLaf`.
- Same flag with the FlatLaf jar *absent* → exactly one
  `jls: warning:` line on stderr and a clean fallback to Metal. An
  experiment can never make JLS unlaunchable.

**Shaded-jar size delta.** Baseline shaded jar
(`mvn package`, 5.0.5-SNAPSHOT): **1,183,765 bytes**. Merging
`flatlaf-3.7.2.jar` into it with the shade plugin's exclusion filters
applied (signatures, `module-info.class`) yields **2,197,375 bytes** —
a delta of **1,013,610 bytes (~0.97 MiB, ×1.86)**. Notable relative to
the current jar, trivial in absolute terms for a desktop app; the cost
in `docs/library-survey-2026-07.md` ("~1 MB") is confirmed. The jar
includes FlatLaf's bundled Windows/Linux natives (used only for custom
window decorations; loaded lazily, harmless elsewhere).

**Test-suite impact.** `grep` over `test/` finds no reference to
`UIManager`, `LookAndFeel`, or Metal; no test instantiates the
`JLSStart` frame (the only place the look-and-feel is set), and the
layer-1 `jls.ui` helpers are model-level (no Swing). The default
headless surefire execution and the CLI drift suites pass with the seam
in place (`mvn test -Dtest=LookAndFeelPolicyTest,CliFlagTableTest,WaylandStartupCliTest`:
20/20 green). Adopting FlatLaf as the GUI default would not touch the
test suite; layer-2 display-tagged tests construct dialogs under the
JVM default look-and-feel independently of `JLSStart`.

## Hardcoded-color audit (what fights a dark theme)

Two distinct classes of hardcoded color exist, with different remedies:

**(a) Canvas painting colors** — drawn on the circuit editor's own
`Graphics`, not themed by any look-and-feel. `JLSInfo`:
`touchColor` green, `highlightColor` pink, `selectionColor` (240,240,240),
`watchColor` cyan, `nonZeroColor` red, `initialStateColor` lightGray,
`gridColor` (240,240,240), `backgroundColor` white; plus pervasive
`Color.black` element outlines in `jls/elem/*`. Under FlatLaf **Light**
these render exactly as today (the canvas is a white panel either way).
Under a **dark** theme the white canvas would clash visually but still
function. Remedy: route canvas colors through a theme-role object
(ergonomics audit U4/U8 direction) — required before a dark *default*,
not before adopting FlatLaf light.

**(b) Swing-chrome `setBackground`/`setForeground` calls** — these fight
*every* look-and-feel including today's Metal, and FlatLaf light merely
makes them look more alien:

- `SimpleEditor`: cyan status strip (`:380`, `:384`, `:3112`), yellow
  hint messages (`:3117-3141`), white import buttons (`:1791`, `:1838`),
  red/black `info` foregrounds (many sites).
- `InteractiveSimulator`: green/yellow/cyan/pink/red control buttons
  (`:93-117`), yellow status bar (`:992`), gray radix buttons
  (`:210-234`).
- `ElementDialog`: green OK / pink Cancel (`:172-173`), red error label
  (`:124`).
- `StateMachine` (cyan messages `:946`, white edit area `:1000`),
  `State` (green close `:1614`, white window `:1642`), `Memory`
  (white labels `:1784`, `:1798`), `Constant`/`Gate` (yellow repeat
  `:453`/`:835`), `KeyPad` (white `:43`), `Trace` (white `:88`).

None of these block adoption of FlatLaf **light** as the default — they
degrade it aesthetically exactly as they degrade Metal. They are the
follow-up work (shared with #76's color half) and a prerequisite only
for shipping a dark theme.

## Comparables (maintenance state re-verified July 2026)

| Candidate | Verdict | Evidence |
| --- | --- | --- |
| **FlatLaf 3.7.2** | **Adopt (recommended)** | Apache-2.0; release 9 days before this evaluation; zero transitives; JDK 25 verified above |
| System/native L&F | Reject as default | No dark-mode API; per-platform rendering variance for the custom canvas; still reachable via `-Djls.laf=system` |
| Radiance | Reject | Active (`radiance-theming` 9.0.0, 2026-07-06 on Maven Central) but API-invasive and far heavier than a drop-in `LookAndFeel`; no benefit over FlatLaf for JLS's needs |
| Darklaf | Reject | `darklaf-core` 3.1.1 is the latest on Maven Central, published 2025-09-16 — ten months stale at evaluation time; fails the survey's active-maintenance ground rule against FlatLaf's cadence |
| Metal (status quo) | Keep as fallback | Remains the built-in default today and the permanent `-Djls.laf=metal` escape hatch |

## What could not be verified here (open before adoption ships)

This evaluation ran in a headless Linux container: no X11/Wayland
display, no Windows or macOS. Still open, and required before the
default is switched:

- The screenshot matrix from #153: Linux X11/Wayland, Windows, macOS at
  100%/125%/200% scale, light and dark — especially Linux
  *fractional*-scale quality and FlatLaf window decorations.
- A visual pass over the ~30 element dialogs under FlatLaf light.
- Theme QA on the three installer targets (#188 family).

The `-Djls.laf` seam exists precisely so this QA needs no rebuild:
`java -cp jls.jar:flatlaf-3.7.2.jar -Djls.laf=com.formdev.flatlaf.FlatLightLaf jls.JLS`.

## Recommendation

**Adopt FlatLaf (light theme) as the default look-and-feel**, gated only
on the cross-OS screenshot matrix above. Adoption commit checklist:

1. Add `com.formdev:flatlaf:3.7.2` (or current) to `pom.xml` runtime
   scope; SBOM/Dependabot pick it up automatically.
2. Change the default arm of `JLSStart.installLookAndFeel()` from the
   cross-platform class to `FlatLightLaf`, keeping `-Djls.laf=metal` as
   the documented escape hatch and Metal as the automatic fallback.
3. Re-record the look-and-feel decision in `ARCHITECTURE.md`
   (deliberately superseding "force Metal, same everywhere"), citing
   this document.
4. File the follow-ups out of scope here: `flatlaf-extras`/`FlatSVGIcon`
   toolbar-icon redraw (the other half of #76), chrome-color cleanup
   (audit §(b)), and a dark-theme toggle once canvas colors are themed
   (audit §(a), U8 preferences work).

`flatlaf-extras` is **not** needed for the core adoption (it adds the
SVG icon support, another ~100 KiB); keeping it a separate follow-up
keeps the adoption diff one dependency and a few lines.

## What shipped with this issue

The evaluation seam, in the spirit of #153's "spike + fallback flag"
task but dependency-free: `JLSStart.lookAndFeelClassName()` /
`installLookAndFeel()` honor `-Djls.laf=metal|system|<class>` (default
`metal`, i.e. today's behavior is unchanged), a broken explicit
selection warns once and falls back instead of exiting, the generated
usage text documents the property alongside `-Djls.toolkit`, and
`test/jls/LookAndFeelPolicyTest.java` pins all of it headlessly.
