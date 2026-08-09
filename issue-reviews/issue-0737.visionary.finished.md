# Issue #737: TASK-C544-1: the Orca feasibility spike — can Swing deliver a live signal-state announcement at all, answered before the band is funded
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the ceremony and #737 says: *before we spend 5–8 mw making a picture speak, find
out whether speech can get out of Swing at all.* The instinct is this project's own —
thin vertical slice, evidence over impression, a recorded kill criterion. Nothing below
disputes the gate. I dispute the experiment behind it: it measures a variable JLS does
not own, on the toolkit least likely to answer, with an apparatus its own AC-3 forbids,
and it leaves no code behind whichever way the answer falls.

## Three ways the experiment is aimed wrong

**1. The variable under test is the bridge, not Swing.** This repo already knows that
Swing can carry a live state change: `javax.accessibility` has had
`firePropertyChange(ACCESSIBLE_VALUE_PROPERTY / ACCESSIBLE_STATE_PROPERTY)` forever, and
`docs/standards-adoption/03-accessibility-conformance.md:400-440` sketches the exact
mapping (`CONTROLLER_FOR`/`CONTROLLED_BY` for netlist topology). The open question is
whether **java-atk-wrapper** — not in the JDK, "thinly maintained"
(`OPEN-QUESTIONS.md:77`), unable to be picked up by a jlink'd runtime's own `conf/` —
relays it. A red spike would report the state of one Debian package on one runner, and
KC-26-2 would kill a feature on it: a platform finding masquerading as a design finding.

**2. The rig's toolkit is a confound, not a control.** #101 runs a JBR `WLToolkit`
(Wakefield, experimental, unpinned — `ci.yml:363-380` still carries
`JBR_SHA256: UNVERIFIED-PLACEHOLDER`). Accessibility support in an experimental Wayland
peer is the least-likely place in the whole matrix for ATK hooks to be wired. Meanwhile
the *majority* of JLS's blind users are on Windows/NVDA via the Java Access Bridge and
on macOS/VoiceOver via the Cocoa peer, where the peer implements NSAccessibility from
`AccessibleContext` directly. A NO on WLToolkit tells you nothing about either, yet
AC-2 lets it re-scope the band and file a VPAT exception.

**3. AC-3 contradicts AC-1, and the contradiction is the tell.** "stands up no second
display or accessibility apparatus" — but the #101 rig has *no* accessibility apparatus:
`grep -n "orca\|atspi\|dbus\|speech" scripts/wayland-rig.sh` is empty; the devcontainer
installs sway/grim/wtype and no ATK anything. Capturing an Orca utterance headlessly
needs a D-Bus session, `at-spi2-registryd`, `speech-dispatcher` with a logging output
module, and JAW injected into a downloaded JBR image — precisely what
`03-accessibility-conformance.md:170-176` says **do not attempt**. AC-3 is the author
sensing that the apparatus is the real cost, then wishing it away — on a lane that
self-skips on a CDN hiccup and whose pixel gate is still uncalibrated
(`PIXEL_DIFF_MIN: "0"`).

## The reframe: make the model speak, and let the picture follow

Three moves. Each is cheaper than the spike, each leaves permanent code, and together
they retire KC-26-2 more convincingly than any Orca transcript.

**A. A `SignalNarrator` on the seam that already exists.** `Simulator.afterEvent`
(`src/jls/sim/Simulator.java:269`) and `probeSample` (`:285`) are protected hooks
already overridden by both `BatchSimulator` (`:140`) and `InteractiveSimulator`
(`edit/InteractiveSimulator.java:879`) — the VCD exporter rides them today. A narrator
turning that change stream into short spoken-ready phrases ("OR gate 1 output goes high
at 40 ns") is a headless, `jls.sim`-side component with **golden-file tests in plain
`mvn verify`**, no display, no bridge, no Orca. Elements already carry
`showCurrentValue`/`showInfo` text for the status line — the vocabulary is half-written.
This is also PF-4's engine: the part-to-whole prose narrative is the same narrator in a
different tense. #737 as written spends the band's funding condition and produces zero
reusable artifact; this produces the one artifact two planned features both need.

**B. Invert which surface is authoritative for a keyboard user.** The band assumes the
blind path runs *through the canvas*, so the canvas must be made to speak — 400–700
lines of per-element `Accessible` proxies plus permanent per-element maintenance, which
this repo's own analysis already rejected (`:437`) in favor of a stock-Swing **circuit
outline view** (~3–4 days, accessible for free on JAB, NSAccessibility and AT-SPI alike,
legitimate under 508 E101.2 Equivalent Facilitation). Push that further than the doc
does: not a companion pane — **the surface a screen-reader user drives**. Focus lives in
a `JTree` of elements and a `JTable` of watched signals; the canvas mirrors the tree's
selection. A live state change becomes "the focused row's accessible value changed, fire
a property change" — the most well-trodden path in every AT stack, rather than the
rarest. The hard question does not get answered; it stops being asked. It is also how
blind engineers work in EDA-adjacent tooling: the netlist is primary, the schematic
derived. JLS is unusually ready for that — `-savetext`, a documented `.jls` grammar,
Verilog export and the batch contract make the text-first view of a circuit the
project's most mature asset already.

**C. Three tiers of evidence, not one scripted session.** The project already has the
right pattern and uses it for exactly this class of risk: a headless CI ratchet plus a
dated once-per-release manual desktop check (`docs/wayland-desktop-checklist.md`).
Apply it here — (i) a headless JUnit assertion that the accessible object's value
changed and the property-change event fired (JLS's actual responsibility, red-on-break,
runs today); (ii) an AT-SPI-level listener (`pyatspi`/accerciser) asserting the event
*crossed the bridge* — scriptable, stable, and the real subject of the spike; (iii) a
dated Orca/NVDA/VoiceOver session recorded in the `docs/accessibility-at-checklist.md`
the a11y doc already asks for. One scripted Orca-in-CI criterion conflates all three
and is the only one of the three that will still be green in a year.

## The priority inversion, stated plainly

I am disregarding AC-1 through AC-4 as the funding condition. The honest funding
condition for a screen-reader band is **that the shipped installers can reach a screen
reader at all**, and today they cannot: `scripts/build-installer.sh:145` derives jlink
modules via `jdeps --print-module-deps`, so `jdk.accessibility` can never appear and
**every released `.msi` bundles a runtime with no Java Access Bridge** — NVDA and JAWS
get nothing from an installed JLS; the deb/rpm carry no `Recommends:
libatk-wrapper-java-jni`. That is a ~1-day fix with a source-grep pin
(`AccessBridgeModuleTest`, the `KeyPadAccessibilityPinTest` pattern), and until it lands
a green Orca spike proves something about a JDK no JLS user has. Two more one-to-two-day
items outrank the spike on delivered value: the #75 caret at **1.14:1** contrast
(`SimpleEditor.java:2392-2404`, `selectionColor` == the grid color) and the
`nonZero`/`watch` wire roles at 2.10:1 and 2.31:1 against 1.4.11's 3:1 floor. The
keyboard feature the whole band stands on has an indicator a low-vision user cannot see.

## What I would file instead of #737

1. **Packaging + contrast fix** (~2–3 days): `jdk.accessibility` in the module set,
   `conf/accessibility.properties` written inside the mtime-clamp step, `Recommends:` on
   deb/rpm, caret and wire-role recolor against the joint ΔE≥25 + 3:1 constraint. This
   is the real gate; nothing downstream is meaningful without it.
2. **`SignalNarrator` + headless golden** (~3–4 days): the change stream as text, in
   `jls.sim` under `HeadlessCoreRatchetTest`'s discipline. Delivers PF-3's and PF-4's
   shared core before any AT question is asked.
3. **Bridge-reachability spike, correctly scoped** (~2 days): a *stock* `JLabel`/`JTree`
   whose accessible value changes on a timer, observed by an AT-SPI listener in the #101
   rig, plus one dated manual Orca session. If a stock component's value change does not
   cross the bridge, nothing custom ever will — the same kill answer at a third of the
   cost, and about the bridge, which is the true variable.

## Cost of the redirect, honestly

This crosses issue boundaries. #544's AC-2 (`OrcaLabSessionTest` asserting spoken state
changes in CI) becomes a two-tier claim, and #355's DoD line — `git grep -c
"getAccessibleChildrenCount" src/` non-zero on the canvas component — presumes the
canvas-scene-model route this deprioritizes. Both need a REPLAN: a real cost, paid once,
against a band whose current shape is 5–8 mw of custom proxies with permanent
per-element maintenance and a documented risk that the Linux bridge surfaces none of it.
CAP-26 §1 step 2 survives intact — the two-gate circuit is still built by keyboard and
still heard element by element; it is heard through a list the AT already knows how to
read, instead of through a canvas we would spend the band teaching it to read.
