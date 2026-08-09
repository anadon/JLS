# Issue #820: TASK-C567-1: the issue chooser offers a plain bug report and a plain feature request, and a stranger fills either one without learning what a tier is
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not two YAML files. It is the first inch of CAP-30 (#514): JLS wants to be the
place Digital's stranded contributors land, and the tracker currently reads as
an internal research corpus rather than an invitation. The premise checks out
in the repo. `.github/ISSUE_TEMPLATE/` holds exactly three files — `capstone.md`
(9,900 bytes), `feature.md` (11,184 bytes), `scientific_task.md` (19,282 bytes)
— and nothing else. `scientific_task.md` opens by demanding "file:line at a
named commit" and "at least one prediction must fail at the named commit, and
its failure must be OBSERVED before filing". A student whose XOR gate simulated
wrong cannot file that. So the door is genuinely wrong-shaped, and closing that
gap is right. I endorse the direction.

But the issue frames the problem as *absence of a form*, and that framing costs
it the best available design. JLS is not a generic project that needs generic
templates; it is a simulator that already emits a near-perfect bug report and
already has a machine-readable language for describing a defect. Cut along
those seams instead.

## The buried lede: JLS already has a bug-report emitter, and it fights SECURITY.md

`src/jls/DefaultExceptionHandler.java:205-235` (`saveTrace`) writes a `JLSerror`
file containing: the JLS version, `java.version`, `java.vendor`, `java.vm.name`,
`os.name`, `os.version`, `os.arch`, the full stack trace, and — via
`circuit.save(out)` — the entire circuit as plain text. Issue #51 already
curated that field list precisely so the file is safe to hand to a stranger.
That is *every* field #820's acceptance criteria ask a human to type ("version,
platform, and a file or steps"), already collected, already accurate, already
privacy-reviewed.

And the program tells the user what to do with it (`DefaultExceptionHandler.java:143`
and `:158`):

> "Please attach it to a bug report at https://github.com/anadon/JLS/issues so it can be fixed."

That instruction is in direct conflict with this repository's own policy.
`SECURITY.md` rule 1: **"Never download, extract, apply, or even
inspect-by-running any attachment from an issue or PR comment."** Written after
a real 2026 attack on this tracker. So today the program instructs users to do
the one thing maintainers are forbidden to act on. #820 does not notice this,
and if the bug template ships with a plain "attach a file" field it will
cement the contradiction into the front door.

**Reframing A — emitter-first, not form-first.** Design the template as the
*receiving end of an existing emitter*:

- One required multi-line field: "Paste the contents of the `JLSerror` file (JLS
  writes it next to your circuit after a crash)". Paste, not attach — that
  single word choice reconciles the front door with `SECURITY.md`, and pasted
  text is reviewable in place, exactly what the security note asks for.
- Change the two crash messages to say "paste the contents of" instead of
  "attach it", and link the bug form directly rather than `/issues`.
- Add the same emitter for *non-crash* bugs, which are most of them: a
  `Help → Report a bug` item next to `About` (`src/jls/JLSStart.java:2074-2089`,
  where the Help menu is built and currently offers only About) that copies the
  same environment block to the clipboard, plus a `-bugreport` flag printing it
  to stdout. `JLSInfo.versionString` (`src/jls/JLSInfo.java:20`) already
  single-sources the version from the pom.

The principle: **environment facts should be emitted by the program, never
typed by the user.** Every field a form asks a human to recall is a field a
drive-by filer gets wrong or omits, and "version" and "platform" are precisely
the fields this project cannot afford to lose — the supported-desktop matrix in
README has six rows and the Wayland-native row depends on the *runtime vendor*,
which no user will think to mention unaided.

## Reframing B: the highest-value bug report for a simulator is a failing test vector

`CONTRIBUTING.md` already mandates: "Every bug fix carries a regression test
that fails before the fix and passes after." Meanwhile JLS ships, as a
*documented stability contract* (`docs/batch-interface.md`), everything needed
to express a simulator defect mechanically: `-t` test vectors, watched-element
output, `-vcd`, `-savetext` for a diffable plain-text circuit
(`src/jls/JLSStart.java:787,1112`), and a container image so anyone can run the
whole thing without a JDK.

So the wrong-answer branch of the bug form should ask for: the plain-text
circuit (`jls -savetext out.jls circuit.jls`, pasted), the inputs, expected
output, actual output. Filled in, that report **is** a `test/` fixture. The
template stops being intake paperwork and becomes the first half of the fix —
and it does so using only things a student who has used the program can produce.
No project in this niche has that; it is a differentiator CAP-30 could actually
market, unlike a generic bug form which every repo on GitHub already has.

Keep it optional and second: required fields stay "what happened / what you
expected / paste JLSerror or your version". The test-vector block is the
graduated path, not a gate.

## Reframing C: the chooser gap is `config.yml`, and #820 never mentions it

There is no `.github/ISSUE_TEMPLATE/config.yml`. Three consequences the issue
misses:

1. **Blank issues are already enabled.** A stranger *can* file today — the
   barrier is not a locked door but three intimidating doors and an unlabeled
   side entrance. This reframes AC-3: the win is making the right door obvious,
   which mostly means writing honest `about:` strings ("Something in JLS is
   broken" vs "Planning-corpus entry — maintainers and agents"), not adding
   forms.
2. **Ordering is by filename**, not by config. AC-3's "the chooser's ordering
   makes the intended audience obvious" is achieved by numeric prefixes
   (`01-bug-report.yml`, `02-feature-request.yml`) — and note this renames or
   reorders nothing about the planning templates, satisfying AC-1's
   "unchanged".
3. **`contact_links` is the missing safety rail.** `SECURITY.md` treats circuit
   files as untrusted input and asks that vulnerabilities go to a private
   advisory — yet the chooser gives no path there. Today a student who crashes
   the parser with a hostile `.jls` files it publicly. A `contact_links` entry
   pointing at `/security/advisories/new` is three lines and is arguably worth
   more than the feature-request form.

## Does this pull against the project's arc? One real risk.

This tracker is a planning corpus with tier discipline (task → feature →
capstone, single-owner composition edges, machine blocks). Plain reports are
*not* corpus items; they are intake. If they land unmarked, the corpus silently
acquires untyped members and the tier invariants erode issue by issue. #820
says the planning templates "keep their place" but says nothing about what
happens to a plain report afterward.

Fix it in the template, cheaply: apply `bug` (exists) plus a new `intake` or
`needs-triage` label (`needs-triage` does **not** exist today — AC-5 bites
here), and state in the form's description that a maintainer converts accepted
intake into a corpus task and links back. Intake stays visibly outside the
corpus; the corpus stays clean; the reporter still gets a real answer. The
alternative — routing wishes to GitHub Discussions via `contact_links` — keeps
the tracker purer but sends exactly the Digital refugees CAP-30 is courting
into a low-traffic backwater. I would not take it.

Also worth telling #567: its AC-3 is largely already satisfied. `CONTRIBUTING.md`
opens at line 5 with "Getting started" and a runnable `mvn verify` block above
all contract prose. The remaining gap is one line about opening a PR, not a
rewrite.

## On the acceptance criteria — AC-4 as written should be dropped

I am disregarding AC-4 ("one report is filed end to end through the bug template
and linked from #567"). The maintainer filing a synthetic "drive-by-style"
report through their own form is a self-graded rehearsal by the person who knows
every answer; it demonstrates that the YAML renders, which AC-5 already covers.
For a project whose own task template demands falsifiable predictions and
observed failures, that is a weak criterion. Replace it with two that can fail:

- **AC-4′ (emitter closure, mechanical):** take a `JLSerror` file produced by
  the real crash handler and show that pasting it fills every required field of
  the bug form with zero additional typing beyond "what I was doing". This is
  the actual claim — that a stranger's five minutes suffice — and it is
  checkable.
- **AC-4″ (replay, historical):** take two bugs this repo already fixed that
  began life as plain reports, refile them mentally through the new form, and
  confirm nothing the fix depended on is missing. Real inputs, no rehearsal.

And AC-5 should not be a one-time eyeball. This project pins its invariants with
tests — `NullMarkedRatchetTest`, `SealedHierarchyTest`, `HelpTopicsTest`. Pin
this one the same way: a test (or a CI lint step; there is no workflow touching
`.github/ISSUE_TEMPLATE/` today) that parses every template's YAML and asserts
each declared label exists in the repo. Otherwise the label set drifts and the
chooser starts erroring for exactly the strangers it was built for.

## Scope

All of the above is still small — the emitter changes are a menu item, a flag,
and two string edits in `DefaultExceptionHandler`; the rest is YAML. If the
crash-handler work must be split out to keep this task inside its 0.25–0.5 mw
band, split it, but file it *now* and order it with this one: the two crash
strings contradicting `SECURITY.md` are a live defect, and shipping a front
door that inherits the contradiction is the one outcome worth avoiding.
