# Issue #737: TASK-C544-1: the Orca feasibility spike — can Swing deliver a live signal-state announcement at all, answered before the band is funded
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The instinct is right: gate a 5-8 mw band (#544) behind a cheap, falsifiable spike before committing. But the spike's own acceptance criteria contradict each other about what apparatus is allowed, and the "minimal slice" language leaves room to pass the test against a trivial widget instead of the actual named risk — which would hand #544 a "fund the band" verdict that never tested the thing CAP-26 §3.2 is actually worried about.

## Findings, most severe first

### 1. (High) AC#1 and AC#3 are in direct tension: capturing an Orca session requires apparatus AC#3 forbids standing up

AC#3: *"The spike runs in the existing Wayland rig and stands up no second display or accessibility apparatus."* AC#1: *"...a scripted Orca session in the #101 rig captures it."*

I checked what "the existing Wayland rig" actually contains. Issue #101's full capability statement, `scripts/wayland-rig.sh`, `scripts/x11-rig.sh`, `.devcontainer/Dockerfile`, and `.github/workflows/ci.yml` describe exactly one thing: booting the GUI on a JBR under headless sway/Xvfb and screenshotting/pixel-diffing it. A repo-wide grep for `orca|at-spi|atspi|dbus|atk` across `.github/workflows/ci.yml` and `.devcontainer/Dockerfile` returns nothing. There is no Orca install, no AT-SPI2 registry/D-Bus session, no `java-atk-wrapper` (the Java→ATK→AT-SPI2 bridge Orca needs — see `docs/standards-adoption/03-accessibility-conformance.md:164-174`), and no speech-capture mechanism anywhere in the project's tooling today. Getting a *real* scripted Orca session to *capture* a spoken announcement means installing and wiring up all of that inside the rig's container — which is exactly "accessibility apparatus" by any plain reading, and is a "second" one relative to the screenshot/pixel-diff apparatus #101 already has. As written, AC#1 cannot be honestly satisfied without doing the thing AC#3 forbids.

**Recommendation:** either drop "stands up no second... accessibility apparatus" from AC#3 and instead bound the *scope* of what gets installed (e.g., "adds Orca + AT-SPI2 + java-atk-wrapper to the existing rig's container image, nothing else"), or replace "a scripted Orca session... captures it" in AC#1 with a lower-apparatus proxy (e.g., a direct AT-SPI2 event-listener script asserting a `PropertyChangeEvent`/`object:text-changed` signal fires, bypassing Orca's speech synthesis entirely) and say so explicitly.

### 2. (High) "Minimal slice" is gameable against the actual named risk, not just against a weak spec

The risk CAP-26 §3.2 names, and that #544 itself documents, is whether Swing can deliver *live per-element* announcements through the project's actual UI: a single custom-painted canvas (`SimpleEditor`) that today exposes **zero** `AccessibleContext` children for circuit elements — confirmed in `docs/standards-adoption/03-accessibility-conformance.md:369-407` ("the gates, wires, and other elements drawn on it are not exposed as individual accessible objects... a screen reader can report that the canvas has focus but cannot enumerate, name, or describe the circuit's contents"). AC#1's "a minimal slice emits one live state-change announcement from a running simulation" does not say the slice must exercise this canvas architecture. A synthetic `JLabel`/`JButton` whose accessible value changes and fires `ACCESSIBLE_VALUE_PROPERTY` is a well-trodden Swing/AT-SPI path that would pass trivially and prove nothing about whether a dynamically-childed `AccessibleJComponent` over a custom-painted component (the architecture `03-accessibility-conformance.md:412-428` sketches as the real fix) survives contact with JAW/AT-SPI2/Orca on `WLToolkit`. A spike that passes this way and gets recorded as "fund the band" on #544 would fund 5-8 mw of work against a risk that was never actually tested.

**Recommendation:** name the slice's substrate explicitly — require it to route through at least a stub `AccessibleContext` override on a custom-painted `JComponent` (the same shape #544's real work needs), not an off-the-shelf accessible widget. Otherwise state plainly that the spike is only testing the JVM→JAW→AT-SPI2→Orca pipe in the abstract, and that the canvas-specific risk remains untested after a "fund" verdict.

### 3. (Medium) The project's own docs flag the packaging risk this spike depends on, and #737 doesn't account for it

`docs/standards-adoption/OPEN-QUESTIONS.md:77`: *"Current maintenance state and packaging of java-atk-wrapper (libatk-wrapper-java-jni) on major Linux distributions; the Linux Orca story depends entirely on it and it is thinly maintained. Do not promise Orca support before testing it."* `03-accessibility-conformance.md:164-174` goes further and recommends *against* bundling JAW into any jlink'd runtime at all, treating it as a system-package, best-effort story. AC#2's three-way verdict (fund / re-scope / stop-and-file-platform-finding) has no bucket for "inconclusive because the CI base image's JAW package is missing, broken, or version-mismatched" — a realistic outcome distinct from "Swing genuinely cannot do this." As written, that outcome gets force-fit into "stop and file the platform finding," which would misrepresent a packaging problem as a Swing/JDK limitation and could kill a fundable feature on an artifact of the CI image rather than the platform.

**Recommendation:** add a fourth recorded outcome (or a sub-clause of the "stop" branch) for "blocked by AT bridge packaging/availability in this CI image, not by Swing itself," with a next step (try the system-JDK + `libatk-wrapper-java-jni` path `03-accessibility-conformance.md` recommends for end users, applied to the CI container) before that branch is allowed to resolve to "stop."

### 4. (Medium) The failure-recording bar in AC#1 is a low, self-reported threshold

*"...or the attempt is recorded as failing with the mechanism that blocked it named."* This only requires naming *a* blocking mechanism, not exhausting the reasonably available configurations. Hitting the first obstacle (e.g., JAW absent from the pinned JBR/container) and recording "blocked: no ATK bridge" would satisfy AC#1 literally while leaving untried the project's own documented workaround (system JDK + `libatk-wrapper-java-jni`, per finding 3). Given AC#2 lets that recorded failure justify "stop and file the platform finding" for the whole band, a shallow attempt has an outsized, hard-to-reverse effect.

**Recommendation:** require the failing record to state which of the known mitigation paths (system JDK, `libatk-wrapper-java-jni`, disabling pixman/software rendering, etc.) were tried and ruled out, not just the first blocker hit.

### 5. (Low) band_mw 0.5-1 is asserted, not derived, and looks optimistic once finding 1's apparatus gap is counted

This issue is visibly the answer to a sibling review's recommendation on #544 ("cost and assign the spike explicitly, e.g. 0.5-1 mw, timeboxed") — but #737 just supplies that number without showing how it was derived. If the spike also has to stand up Orca + AT-SPI2 + a D-Bus session + java-atk-wrapper from scratch inside a container that has none of it today (finding 1), and then debug all of that under headless sway's pixman software rendering (which the #101 rig's own docs admit is "not a GPU desktop" and can diverge from real compositors), 0.5-1 unit of effort is a tight budget for infrastructure bring-up plus the actual Swing experiment.

**Recommendation:** split the estimate into "stand up the AT-SPI/Orca harness" and "run the Swing experiment against it," and cost them separately, or explicitly note the 0.5-1 figure assumes the harness already exists (in which case, say where it comes from).

### 6. (Low) Machine-block linkage to #544/#756 is prose-only

`part_of_feature: 544` and `ordering_after: [TASK-C549-1]` (#756, confirmed open) are YAML in the issue body, not GitHub sub-issue/dependency links — `has_parent` on #737 reports `false`. Nothing stops someone from starting this spike before #756 lands, and nothing enforces that AC#2's "verdict recorded on #544" actually happens as a comment there. This is a repo-wide convention, not unique to #737, but it means a "funding condition" for a 5-8 mw band rests on a maintainer remembering to post a comment rather than on anything CI-checkable.

**Recommendation:** at minimum, use GitHub's native sub-issue relationship for #737→#544 so the linkage is queryable, matching the discipline the completion-criteria checklists elsewhere in this cluster otherwise aspire to.

## What's solid

- The three-way verdict shape (fund / re-scope with a named VPAT exception / stop and file a platform finding) is a genuinely good risk-management structure, inherited correctly from #544.
- AC#4 — commit the transcript (or its absence) as evidence rather than resting the verdict on an impression — is a concrete, well-specified evidence-discipline requirement with no gap to flag.
- Keeping the spike inside the existing rig rather than provisioning a whole new CI lane is the right cost-consciousness instinct, even though finding 1 shows the "no apparatus" language overshoots what that instinct can actually deliver.

## Verdict rationale

The spike is the right move in principle, and two of its four criteria (AC#2's verdict structure, AC#4's evidence bar) are solid as written. But AC#1 and AC#3 make contradictory promises about what the spike is allowed to install, the "minimal slice" language lets the test pass without ever touching the canvas architecture that is the actual named risk, and the project's own accessibility docs flag a packaging risk (java-atk-wrapper) that AC#2's verdict taxonomy has no room for. These need text-level fixes to the acceptance criteria before the spike is scheduled, not a re-derivation of the whole plan. **needs-rework.**
