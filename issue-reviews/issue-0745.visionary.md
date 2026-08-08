# Issue #745: TASK-C544-5: the NVDA path ships as documentation and a manual checklist, stated as manual and never counted as automated coverage
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and #745 is one sentence: *the Windows
screen-reader claim must not be stronger than the evidence behind it, and #547
must be able to tell where the boundary is without asking a human.* That is the
right goal and it is squarely on the project's arc — the same discipline that
produced `docs/reproducibility.md`'s "the installers are *not* byte-reproducible"
paragraph and `docs/keyboard-a11y-verification.md`'s "Deliberately out of scope"
section. JLS's credibility is built on documents that say what was *not* verified.

But the issue answers that goal with an artifact — a hand-authored, NVDA-only
prose checklist — that pulls against three things the project has already
decided, and rests on a premise it never tests. All four are fixable, and fixing
them makes the task smaller, not larger.

## Four ways the chosen artifact fights the project's own trajectory

**1. It duplicates a three-platform artifact this repo has already designed, and
silently orphans macOS.** `docs/standards-adoption/03-accessibility-conformance.md`
("What only a human can do", ~lines 588-608) already specifies
`docs/accessibility-at-checklist.md` — a once-per-release manual AT spot-check in
the style of `docs/wayland-desktop-checklist.md`, with a table covering Linux/Orca
(AT-SPI via java-atk-wrapper), Windows 11/NVDA+JAWS (Java Access Bridge), and
macOS 14+/VoiceOver (Cocoa peer). That file does not exist yet. #745 proposes to
create a *different*, NVDA-only document covering a subset of the same ground.

The macOS omission is the tell. #544 frames the world as "Linux automated,
Windows manual", but the shipped platform matrix in README.md has three GUI rows,
CI has a `macos-gui` lane (`.github/workflows/ci.yml:594`), and VoiceOver is the
one bridge that needs *no* extra module because the Cocoa peer implements
NSAccessibility from `AccessibleContext` directly. Under #745 as written, a
VoiceOver user gets neither automation nor a checklist row, and #547's VPAT will
have to say "not evaluated" for the platform with the best odds of working. A
document whose stated virtue is honesty should not have a hole shaped like an
entire operating system.

**2. It ships prose into a pipeline that #547 declares must be mechanical.**
#547's thesis is "no criterion claimed without a named passing test, checked
mechanically", with the kill criterion "if the generator cannot be made
mechanical, no hand-authored VPAT ships". #745's AC-2 hands that generator a
paragraph of English ("the document states plainly which claims rest on
automated Orca coverage") and AC-4 hands it a promise a human made at review time
("no claim in the checklist is stronger than what the Orca automation asserts").
Both are exactly the class of unverifiable input #547 exists to refuse. The issue
says the VPAT can "consume it without guessing" — but parsing a hand-written
markdown table *is* guessing.

**3. It documents a Windows configuration that today carries no accessibility
bridge at all.** `scripts/build-installer.sh:145` derives the jlink module set
from `jdeps --print-module-deps`, which reports *static* dependencies.
`jdk.accessibility` — the module carrying the Java Access Bridge — can never
appear, because nothing in the jar references it, and the string appears nowhere
else in the script. So every `.msi` this project publishes bundles a runtime NVDA
and JAWS cannot read. A tester running #745's checklist against the primary
Windows distribution records FAIL on every row; a tester running it against
`java -jar` on a full JDK with `jabswitch -enable` records something no Windows
*user* has. Neither is the honest evidence the issue is trying to produce.
`03-accessibility-conformance.md` §1 identifies this as "the single most
consequential finding in the tree and it is a one-line change" — and a search of
open issues finds nobody owns it. #380 §7.2 even records the platform belief
wrongly ("UIA on Windows"; Swing does not expose UIA), which is how a gap like
this survives.

**4. The premise "NVDA is not automatable in the project's CI" is asserted, not
spiked — in a feature that gates its Linux twin on a spike.** #544's whole
funding structure is a feasibility gate: #737 exists solely to answer "can Swing
deliver a live announcement through Orca at all" before money is committed. #745
makes the opposite-signed claim about the other platform with no experiment
whatsoever. Meanwhile `scripts/windows-rig.ps1` / the `windows-gui` lane
(`ci.yml:722`) already boots the real `jls-*.jar` on windows-latest's **real
interactive window station** — no Xvfb, no compositor — and screenshots it. That
is precisely the substrate an NVDA session needs, and it was built for a
different reason and is already green. NVDA installs unattended, and its own
project drives it under test by reading spoken output out of its debug log; that
is a plausible path, not a certainty, which is exactly what a timeboxed spike is
for. Declaring a platform unautomatable while owning a working GUI rig on it is
the kind of asymmetry this project normally catches.

## The reframing: one manifest, three renderings, one bridge invariant

I am setting aside AC-1 through AC-4 as written — not because the outcome is
wrong, but because they specify a hand-authored artifact where the project's own
seam is a generated one.

**(A) Make the capability list data, not prose.** Introduce one machine-readable
capability manifest — `test/resources/a11y/at-capabilities.*`, in the house
golden-file style of `BatchSimulationGoldenTest` / `VcdExportGoldenTest`. One row
per capability (element traversal, connection announcement, simulation-state
announcement, …), each carrying: the capability id, the expected spoken
substance, the automated test that asserts it (or empty), and the platforms on
which the claim is manual. Then:

- #743's Orca test **parametrizes over the manifest**, so a capability with no
  passing Orca assertion cannot silently be listed as automated;
- the NVDA/VoiceOver checklist markdown is **generated** from the manifest, so
  AC-1 and AC-3 become "the generator emits a row per capability with an action
  and an expected utterance", not "an author remembered";
- #547's VPAT **joins** on the manifest instead of parsing English, and AC-4
  ("no manual claim stronger than the automated one") becomes a test —
  `AtClaimStrengthTest`: for every capability, the manual row's asserted
  substance is a subset of the automated row's, and any Windows-only gap is a
  named, typed field rather than a sentence someone might forget to write.

That single change converts #745 from a documentation task into a ~0.3 mw
generator plus a manifest, and it converts three of the four acceptance criteria
from review-time promises into build-time properties. It also makes the document
un-rottable: today's checklist decays the moment #743 adds a capability, because
nothing connects them.

**(B) Cut along the bridge seam, not the screen-reader seam.** Orca and NVDA are
not two independent sources of truth; they are two *renderers* of one
`AccessibleContext` tree and one event stream that JLS produces. Orca reads it
through java-atk-wrapper/AT-SPI, NVDA through the Java Access Bridge, VoiceOver
through the Cocoa peer. The thing JLS actually controls, and the thing #380 is
building, is the model — and that is testable **headlessly, on every push, on
every platform**, with no screen reader anywhere (the `AccessibleTreeGoldenTest`
already specified in `03-accessibility-conformance.md`).

Reframed that way, the manual surface collapses. The manual session no longer
re-verifies N capabilities on Windows; it verifies exactly one proposition —
*does the Java Access Bridge carry this tree and these events to NVDA at all* —
plus a small spot-check that the utterances are intelligible. Everything else is
inherited from the headless model test. That is both cheaper and a stronger
claim, because it says *why* Windows is expected to behave like Linux rather than
asserting parity row by row and hoping.

**(C) Order the prerequisite honestly.** `ordering_after: [TASK-C544-2]` is the
wrong edge. The real prerequisite is the `jdk.accessibility` module fix plus the
`accessibility.properties` write in `scripts/build-installer.sh`, pinned by a
source-grep test in the pattern of `KeyPadAccessibilityPinTest`. File it — it is
unowned today, it is roughly a day, and until it lands #745 cannot produce a
truthful row about the artifact Windows users install.

**(D) Spike before you concede.** Add a timeboxed sibling to #737: can the
existing `windows-gui` rig install NVDA unattended, drive the two-gate build, and
capture spoken output? Two outcomes, both good. It passes → the "manual" framing
of #544's fourth AC dissolves and Windows joins the automated tier. It fails →
#745 ships with a *recorded reason* instead of an assumption, which is the only
version of "stated as manual" that survives a reviewer asking "how do you know?".

## What I would keep verbatim

The outcome sentence, and the instinct behind AC-4. "No claim stronger than what
the automation asserts" is the single most valuable line in the issue and the
reason CAP-26's paperwork will be believed. My objection is only that it is
currently enforced by a human reading two documents side by side, when the
project has a well-worn habit of enforcing exactly this class of promise with a
test (`HotkeysHelpAccuracyTest` pins `hotkeys.html` against `EditOp.accelerator`
for precisely the same reason: docs must not be able to silently desync).

## Concrete restatement of the task

1. Land the Access Bridge module fix (new issue, blocks this one).
2. Timeboxed NVDA-automation spike on the existing `windows-gui` rig; record the
   answer either way.
3. Create the capability manifest; make #743's Orca test read it.
4. Generate `docs/accessibility-at-checklist.md` from the manifest — **all three
   platforms**, Orca / NVDA+JAWS / VoiceOver — in the shape of
   `docs/wayland-desktop-checklist.md`, including its dated results template
   (which already satisfies AC-3's version/date requirement, so do not reinvent
   it).
5. `AtClaimStrengthTest` enforces AC-4 mechanically; #547 consumes the manifest,
   not the markdown.

Same outcome, one artifact instead of two, three fewer places for the truth to
drift, and a Windows claim that describes the build users actually install.
