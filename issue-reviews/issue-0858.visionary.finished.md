# Issue #858: TASK-C581-1: `brew install --cask jls` installs the published dmg by its attested sha256, and the tap-versus-homebrew-cask decision is recorded with its reasons
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Under #581, under CAP-34 (#518), the claim is: *JLS should be reachable by the
command a macOS user already types for everything else, and the artifact they get
should be the same attested bytes the Releases page publishes.* That is a good
claim and it sits on the project's arc — the README already treats "which bytes,
verified how" as a first-class user-facing contract, and CAP-34's evidence (#510)
is honest that the incumbent ships four channels while JLS ships zero.

The route, though, is built on a premise the issue never tests, and the premise is
false. AC-3 stages a "fork in the road" between `homebrew/cask` and a project tap,
to be decided by review-cost arithmetic. There is no fork. Homebrew's documented
new-cask notability rule rejects software whose GitHub repo has fewer than 30
forks **and** fewer than 30 watchers **and** fewer than 75 stars; `brew audit
--new --cask` enforces it mechanically. `anadon/JLS` today: **9 forks, 3 watchers,
3 stars**. The `homebrew/cask` branch is closed before any arithmetic is done. The
executor will spend the task's budget computing a cost comparison for an option
that is not on the table.

That is not a nitpick, because the *whole discovery premise of PF-3 dies with it*.
A project-owned tap is not a listing. It has no store page, no screenshots (CAP-34
AC-4 is unsatisfiable here), and — the part nobody has said out loud — **no install
analytics**. Homebrew publishes install counts only for `homebrew/core` and
`homebrew/cask`; a third-party tap reports nothing. So the one channel of CAP-34's
three that would have fed PF-4's download-count KPI for free is exactly the one
that is ineligible. `brew install anadon/jls/jls` is a string only someone who
already read the README will ever type.

## The reframing

Homebrew here is not a discovery channel. It is an **upgrade channel**, and that
is still worth having: `brew upgrade` is the only mechanism in JLS's macOS story
that keeps a lab machine current without a human revisiting the Releases page.
Say that in the decision record, and the value proposition stops overclaiming.

Concretely, I would restate the task as: *stand up `anadon/homebrew-jls`, record
that the location was forced by notability rather than chosen by cost, and record
what the tap does and does not buy (upgrades: yes; discovery, listing, analytics:
no).* The 0.25–0.5 mw band is right for that; it is not right for the arithmetic
AC-3 asks for.

## The alternative the issue never considered: ship a formula, not only a cask

The dmg is Apple-silicon only (README: "Intel Macs: use the jar"), it is unsigned
by choice, and it is **not byte-reproducible** (`docs/dmg-reproducibility.md`: the
koly-only path ships, the two dmgs still differ). So a cask pins its sha256 to the
weakest-provenance artifact the project publishes, on the narrower half of the
Mac population, and inherits an OS refusal on first launch.

A Homebrew **formula** in the same tap inverts every one of those:

- `bin.write_jar_script libexec/"jls.jar", "jls"` with `depends_on "openjdk"` —
  the standard homebrew idiom for exactly this shape of program.
- It pins the **jar's** sha256 from `SHA256SUMS`, and the jar is the artifact the
  README calls bit-for-bit reproducible and attested. The channel's integrity
  claim gets *stronger*, not weaker.
- It works on Intel and Apple silicon alike, from one universal artifact.
- Formula installs are not quarantined. **The Gatekeeper problem disappears** —
  there is nothing to caveat, nothing to keep in sync with the README.
- It puts a `jls` command on `PATH` on macOS. That is the batch/autograder surface
  the container image exists to serve, and today a macOS instructor has no
  packaged way to get it. Given CAP-34's stated constituency, this is plausibly
  worth more than the GUI cask.

The cask should still exist — it is what gives `/Applications/JLS.app`, the dock
icon, and the `.jls` document association, which students want. But "cask over the
dmg" is one of two things the tap should carry, and the cheaper, better-attested,
wider-reaching one is the one nobody filed.

## Two smaller places the design pulls against the project

**AC-2's verification is a one-shot human note in a project that builds rigs.**
"Verified once and recorded" is the only style of assurance in JLS's distribution
work that decays. `scripts/macos-rig.sh` and its self-test exist precisely because
this project refuses that pattern; `release.yml:513-524` already mounts the dmg on
`macos-latest`, copies the `.app`, and runs `-h`. A `brew tap` + `brew install
--cask` + launch lane on `macos-latest` is a few lines next to that, re-runs every
release, and subsumes AC-4's audit script. Make it a lane, not a memory.

**The Gatekeeper caveat #859 wants copied verbatim may already be wrong.** macOS
15 removed the Control-click → Open bypass for apps that fail Gatekeeper; the
route is now System Settings → Privacy & Security → "Open Anyway". The README's
paragraph is the source of truth for #859's drift check, so an outdated
instruction would be enshrined in two places and asserted equal by CI — a drift
check that guarantees consistency, not correctness. Verify the workaround on
current macOS *in this task*, since this is the task that first puts a Mac in
front of the artifact.

And an option worth deciding rather than defaulting: a tap-served cask may pass
`--no-quarantine` guidance in caveats (or strip the xattr in `postflight`, which
`homebrew/cask` forbids but a tap permits). Brew has already verified the download
against the attested sha256; quarantine's marginal contribution over that is
XProtect's first-launch scan, not authenticity. Removing the friction entirely is
defensible — but only as an explicit, written decision, never as a silent
convenience. It does not touch the signing stance AC-5 protects; it changes only
whether the channel apologizes for macOS or routes around it.

## What I am disregarding

- **AC-3's premise.** Do not produce review-cost arithmetic to choose between two
  options when one is ineligible. Record the notability numbers and Homebrew's
  rule as the reason; spend the saved effort on the real question — whether a
  non-discoverable, analytics-free tap earns its per-release cost at all. If the
  honest answer is "barely", that is a finding CAP-34 needs, and it argues for
  sequencing PF-2 (winget) and PF-1 (Flathub) ahead of PF-3: both have real
  listings, neither has a notability gate, and winget is already CAP-34's own
  demo slice.
- **AC-2's "verified once".** Replace with a re-runnable `macos-latest` lane.

## Verdict

endorse-with-reframing. Build the tap; it is cheap and `brew upgrade` is real
value. But record the location as forced, not chosen; add the jar formula as the
tap's better half (Intel coverage, reproducible artifact, headless CLI, no
Gatekeeper); verify by CI lane rather than by anecdote; and hand CAP-34 back the
honest finding that its Homebrew leg buys upgrades, not discovery.
