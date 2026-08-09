# Issue #284: Installer verification: first green msi/dmg install → launch → .jls open on real runners, then retire the experimental flags
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The issue's factual claims check out against the repo at HEAD (`5311625`,
one trivial `actions/setup-java` dep bump past the pinned `evidence_commit`
`29afb26`): the experimental flags exist exactly where cited
(`.github/workflows/release.yml:301,308,312`), the per-OS smoke steps exist
exactly as described (`release.yml:490-511` Windows, `release.yml:513-524`
macOS, `release.yml:453-479` Linux), the `--file-associations` wiring is
real for both msi and dmg
(`scripts/build-installer.sh:477`, `:409`, and the three
`resources/packaging/jls-association-*.properties` files), and
`JLSStart.parseCommandLine` at `src/jls/JLSStart.java:829` is the right
anchor. No citation is stale, no code claim is fabricated. The design
problems are in what the acceptance criteria actually verify versus what
the issue's own audience-impact paragraph promises.

## Findings, most severe first

### 1. The automated half of the verification (P1) never exercises the capability the issue is about

The Windows and macOS "Install and smoke-test the installer" steps run
`JLS.exe -h` / `Contents/MacOS/JLS -h` after install
(`release.yml:504-505`, `:524`) — a CLI usage print, chosen explicitly to
avoid touching AWT/a display (see the job comment at `release.yml:443-452`).
Neither step ever tests the `.jls` file association that #82's own
"Intended Audience & Impact" section names as the point: "double-click a
`.jls` file, JLS opens it." Compare with the Linux leg, which at least
asserts the desktop entry's `%f` field code and queries
`xdg-mime query default` (`release.yml:463-470`) — still not a real
double-click, but closer to the claim than Windows/macOS get. So a fully
green P1 dry-run (Completion Criteria box 1) proves install-and-launch on
Windows/macOS, and proves nothing about association — despite Observation
2 and the in-tree comment at `release.yml:262-265` characterizing a green
smoke run as "*is* the 'installed and checked on hardware' verification
#82 has been waiting on." That is an overstatement the issue inherits
without qualifying: the smoke step is *necessary* evidence, not
*sufficient* evidence, for the H2 capability. The issue does correctly
split H1 (P1, automated) from H2 (P2, manual) in its own hypothesis
section, so the checklist is not technically wrong — but a future closer
skimming Observation 2's language, rather than the Predictions section,
could reasonably conclude the automated run alone is enough. **Recommendation:** rewrite Observation 2 / the release.yml comment to state
plainly that the smoke step verifies install+launch only, and that
association verification is exclusively carried by the manual P2 step —
or better, extend the Windows/macOS smoke steps to invoke the association
mechanism headlessly (e.g. `Start-Process -FilePath test.jls` on Windows,
`open -a` / LaunchServices query on macOS) rather than shelling directly
to the binary, which at minimum tests ProgID/UTI registration without
needing a display.

### 2. The manual double-click check (P2) has no evidentiary bar and no assigned executor

Completion Criteria item 2 is "P2 verified manually on both OSes;
procedure and outcome recorded" and §9 asks for "manual-verification
notes (OS version, click path, **screenshot if easy**)." "If easy" makes
the only tamper-resistant evidence (a screenshot) optional, so the box can
be checked with a prose claim alone. Given this repository's issue corpus
is otherwise extremely evidence-disciplined (run IDs, permalinks pinned to
commits, `diffoscope` attribution, etc. — see #134, #188 above), this is a
conspicuous gap: nothing stops "STATUS: verified on my Windows box" from
closing the issue without a reviewable artifact. Separately, the issue
never says *who* performs P2 — materials list "a Windows machine and an
Apple-silicon Mac (or clean VMs)" but assigns no owner. If this issue is
worked by an autonomous agent (as the repo's issue-authoring style
strongly suggests is normal here), the agent has no GUI/mouse access to a
real Windows or macOS desktop and structurally cannot perform P2 itself —
the issue has no fallback, escalation, or human hand-off step for that
case, just an implicit assumption someone with hardware will do it.
**Recommendation:** make the screenshot (or an equivalent machine-checkable
artifact, e.g. a MIME/UTI registry dump) mandatory, not "if easy," and add
an explicit note on who is expected to execute P2 (maintainer, or a named
hand-off point) if the assignee is an agent.

### 3. A cheaper automated substitute for P2 exists and isn't considered

`ci.yml` already has `macos-gui` (`GUI boot (macOS, WindowServer)`,
`ci.yml:594-595`) and `windows-gui` (`GUI boot (Windows, WindowStation)`,
`ci.yml:722-723`) rigs that boot the real shaded jar under a real window
server and take a screenshot, built for #101/#111/#265. Those rigs already
solve "run the real GUI on a real Windows/macOS box in CI and produce
photographic proof it rendered." Issue #284 doesn't mention reusing that
machinery to launch the *installed* binary via `Start-Process test.jls` /
`open test.jls` and screenshot the result, which would upgrade P2 from an
unenforceable manual claim to a CI artifact. This isn't a blocking defect
— the rigs test the jar, not the installer, and wiring them together is
real work — but it's a missed option that would have closed Finding 1 and
2 at once, worth raising before work starts rather than after.

### 4. "small packaging defect" in the fix-vs-refute branch is undefined

§8 Method: "If a smoke step fails: fix if a small packaging defect;
otherwise post `REFUTED:` ... and follow §10." Nothing bounds "small."
Falsification Criteria (§10) only address the case where the smoke step
*can't* be made green at all (msiexec/hdiutil policy failures) — they say
nothing about the gray zone where a fix is possible but non-trivial (e.g.
a WiX `--file-associations` misconfiguration, a `--win-per-user-install`
path bug). Under the current wording an executor could legitimately fix an
actual packaging bug of real complexity while calling it "small," which
is scope-creep risk in the other direction from REFUTED-too-eagerly.
**Recommendation:** replace "small" with a concrete boundary, e.g. "a
single-file change to `build-installer.sh` or the `.properties` file with
no new external dependency and no `scripts/build-installer.sh` recipe
divergence between local/CI" (echoing Global Invariant 2 in #82).

### 5. Windows/macOS smoke steps don't verify clean uninstall, unlike the Linux leg

The Linux smoke step runs `sudo apt-get remove -y -qq jls` after
verifying the mime/desktop entries (`release.yml:467`), giving some signal
that removal is clean. The Windows step does run `msiexec /x` at the end
(`release.yml:510-511`) but only checks the exit code, not that
`JLS.exe`/registry keys/the file association actually disappeared; the
macOS step doesn't uninstall at all — it just `rm -rf`s the copied `.app`
bundle (`release.yml:524`), which isn't an uninstall path a real user
would take (there's no installer-driven removal step to fail). This is
outside the issue's stated scope (install → launch → open, not
uninstall-cleanliness), so it's not a blocker, but worth a one-line
acknowledgment that "no orphaned association after removal" is untested
on two of three OSes.

### 6. ARM-leg disposition is left as an "Open Question," but Completion Criteria references it as settled

Completion Criteria item 3 reads "`experimental: false` for windows-latest
and macos-latest ... (ARM per the Open Question, with `WAIVED:` if
applicable)" while the Open Questions section calls the ARM policy a
"Recommended default" that "does not block execution" — i.e., not
actually adjudicated. This is a minor internal tension rather than a
contradiction (the parent #82 carries the identical unresolved question
verbatim, so #284 is consistent with its parent, not diverging from it),
but it means the DoD checkbox for ARM can't actually be ticked
unambiguously without a decision that this issue defers to a future
reader's judgment call. Low severity since it's explicitly called out and
non-blocking, but a genuinely open item shouldn't be phrased inside a
"Completion Criteria" checklist as if resolution were mechanical.

## What's solid

- All permalink citations (`release.yml` line numbers, `ci.yml:995-999`
  20-run rule, `JLSStart.java:829`) resolve correctly at current HEAD;
  only a trivial unrelated dep-bump commit sits between the pinned
  `evidence_commit` and HEAD, so no re-derivation is actually needed yet.
- The `--file-associations` wiring this issue is implicitly relying on
  (`scripts/build-installer.sh:477` Windows, `:409` macOS, and the three
  `resources/packaging/jls-association-*.properties` files) is real and
  matches the issue's description — this isn't a paper feature being
  verified, the plumbing genuinely exists.
- The falsification criteria (§10) correctly separate "can't ever go
  green on this runner class" (narrows #82's cross-platform claim,
  documented, doesn't force a fake pass) from "installs but association is
  wrong" (fix the `.properties` file, not the launcher) — a well-designed,
  hard-to-game failure taxonomy for the cases it does cover.
- Cross-issue consistency is good: the ordering note ("#134 signing will
  change msi bytes after this task — re-verify") matches #134's own text
  almost verbatim, #188's byte-reproducibility scope and #265's test-lane
  scope are correctly excluded rather than silently absorbed, and the
  parent/child edges (`part_of_feature: 82`, `blocked_by: []`) match #82's
  own decomposition table exactly. No orphaned or contradictory
  cross-references found.
- Scope is tightly bounded: explicitly excludes ci.yml msi-lane promotion,
  signing, and reproducibility, each with a one-line reason and a pointer
  to the owning issue — low risk of scope creep into adjacent programs.

## Verdict rationale

Nothing here should stop the task from being picked up — the factual
groundwork is accurate and the scope is disciplined. But the acceptance
criteria as written let the issue be closed on the strength of an
automated check that doesn't test the claimed capability (Finding 1) plus
a manual check with no mandatory evidence and no assigned executor
(Finding 2), which together create a real path to a false "verified"
close. `sound-with-concerns`: rework the P1 smoke steps and the P2
evidence bar before treating this as done, not before starting it.
