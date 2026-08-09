# Issue #865: TASK-C583-2: the sponsor question is answered by name or answered "none found, searched here" — and a dated go/no-go lands in-tree, with "no, with reasons" being a complete answer
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its ceremony, #865 asks the project to decide whether it will take
on a *distribution's* maintenance obligation at bus factor 1, and to leave a
dated record so the question stays answered. That is a genuinely good instinct
and it is native to this repository: `docs/standards-adoption/11-costed-rejections.md`
exists precisely to say "we decided not to, and here is the number", the
FlatLaf and picocli evaluation docs are verdicts with their measurements
attached, and ARCHITECTURE.md's "Recorded decisions" section pairs every
decision with a revisit trigger — which is exactly what #865's AC-4 asks for.
The genre is right. What I want to challenge is the *subject* of the decision,
the *place* it lands, and the belief that a sponsor search is the load-bearing
step.

## The decision is closer to already-made than #583 assumes

Three things already in-tree bear on the verdict, and none of them is cited by
#583, #864, or #865:

1. `docs/standards-adoption/10-desktop-and-housekeeping.md:70-73` states flatly
   that JLS's deb installs into `/opt/jls` (see
   `resources/packaging/resource-dir-linux/postinst`), "which is fine for a
   third-party package but is not the layout Debian or Fedora archives accept,
   so archive inclusion is not on the table either." A normative playbook
   section already reached the architectural half of the conclusion.
2. The custody stance behind #136 (README.md:62-70, SECURITY.md) refuses a
   project-held signing key at bus factor 1. That kills the obvious fallback
   the issue never names — a self-hosted signed apt repository — for the same
   reason KC-34-2 kills self-NMU. The two refusals rhyme, and together they
   mean "Debian-family users are served by the Releases-page deb and the
   AppImage" is not a consolation prize but the project's coherent position.
3. `flake.nix` already builds a de-vendored, system-JDK JLS with a desktop
   entry, MIME type, icon, and `gpl3Plus` metadata. That is the shape of build
   a distro archive requires — and it exists, tested, today.

Point 3 is the interesting one, because it inverts #864's framing. #864 is
scoped to price "what the bundled runtime obliges us to"; the repo's own
evidence is that the bundled runtime is a *packaging-script* property, not a
project property, and the un-bundled build is already a first-class output.
The real obstacle to Debian is not technical debt — it is that Debian wants a
*person* who is not us, indefinitely.

## The reframe: stop asking "should we maintain it?", ask "can someone else?"

I would move the center of gravity of this whole feature from a sponsor hunt to
**downstream packageability**. "Is JLS cheap for a distro maintainer who is not
us to package?" is a property of the repository, under the project's control,
testable in CI, and it pays out across Nixpkgs, Fedora, Arch/AUR, Guix, an
Ubuntu PPA, and Debian *simultaneously* — whereas a sponsor search pays out in
one distro or not at all. Section 10 of the standards playbook already gestures
at this ("if the project is ever packaged by a third party — nixpkgs, an Ubuntu
PPA, a distro maintainer — the metainfo already exists and is correct"); nobody
has made it a goal.

Concretely, in place of a sponsor hunt I would spend the same 0.5–1 mw on:

- `docs/packaging-for-distributions.md` — one page aimed at a *downstream*
  packager: build from the tagged tarball with a system JDK 25+, the jar is the
  product, here is the launcher, the `.desktop`, the icon, the MIME type, the
  AppStream metainfo, the man page, and here is what is deliberately *not*
  shipped (no vendored runtime, no project GPG key). `flake.nix` is the
  worked example and can be cited as such.
- A `contrib/debian/` skeleton (`control`, `rules`, `watch`) that a Debian
  Developer could adopt in an evening — offered, not maintained, and labelled
  as such. This is the artifact that actually attracts a sponsor: sponsors
  volunteer for packages that are already nearly done, not for invitations.
- **Nixpkgs proper** as the concrete, available win. No open issue proposes it
  (I searched: #849/#853–#860 cover Flathub/winget/Homebrew/KPI; nothing covers
  Nixpkgs). The derivation already exists in `flake.nix`, submission is one PR
  reviewed by nixpkgs committers, and version bumps are largely automated
  upstream by `r-ryantm`. It delivers CAP-34's stated outcome — JLS in a real
  distribution archive, one command away — at a fraction of the ITP cost, and
  it does not require the project to hold a key or become anyone's maintainer.

Under this reframe the sponsor question does not vanish; it demotes from *gate*
to *lagging indicator*, checked cheaply after readiness exists rather than
before it.

## I am explicitly disregarding AC-1 as written

AC-1 makes "the sponsor question is answered by name, or 'none found' with
venues and dates" the headline deliverable. I would strike it in favor of one
honest line in the decision record ("no ask was made; an ask is only rational
once `contrib/debian/` and a documented system-JDK build exist"). Reasons: a
cold post to debian-mentors for a package with no `debian/` directory and a
`/opt` layout asks a volunteer to do the packaging *and* the sponsoring, which
is why such asks go unanswered; and a "none found" recorded from that ask would
be evidence about the ask, not about Debian. This also dissolves, rather than
patches, two hazards the adversarial pass flagged — the searchless "searched
here" and naming a private individual in public git history — because under
readiness-first there is no named-person artifact until a Debian Developer
self-identifies in a public bug of their own accord.

## Where the verdict should land — and a duplication to avoid

#865 proposes a bespoke in-tree decision document. Two better homes already
exist, and using them costs less than the new file:

- **#857's per-channel maintenance ledger.** It is chartered as "one in-tree
  file, one row per release per channel", with the KC-34-1 threshold in its
  header and AC-5 explicitly requiring estimates to be marked as estimates.
  #864's maintainer-week arithmetic is an estimated Debian row in exactly that
  ledger. #583/#864/#865 never mention #857; writing a second cost artifact
  next to it is the drift KC-34-1 was invented to prevent.
- **ARCHITECTURE.md "Recorded decisions".** A short entry with a revisit
  trigger — the same form as the i18n non-goal — is the durable, discoverable
  record. AC-4's "what would change the answer" *is* a revisit trigger; say so
  and reuse the pattern instead of inventing a parallel one.

## One larger contradiction this work should be allowed to notice

`docs/standards-adoption/10` §(a) recommends **declining Flathub**, with
specific reasons (the sandbox is hostile to the `<circuit>.jls~` sibling-write
recovery path in `src/jls/edit/Editor.java`, and `flatpak run` breaks every
command line in `README.md` and `docs/batch-interface.md`) and instructs that
the decline be recorded the way ARCHITECTURE.md records decisions. CAP-34's
PF-1 (#579, #849, #853, #854) makes Flathub a planned feature anyway. No issue
owns that conflict. The channel-admission reasoning #865 is about to write —
recurring cost, does it require a person outside the project, does it require a
project-held key, what would flip it — is the exact machinery that conflict
needs, and Debian is the *least* consequential place to apply it first. If this
task is generalized into a one-page channel-admission policy with Debian as its
first worked instance, it retires #865, arms KC-34-1 for four other channels,
and forces the Flathub contradiction into the open. That is a strictly better
use of the same budget.

## Verdict

**endorse-with-reframing.** The instinct — decide, date it, state what would
reopen it, and treat "no, with reasons" as complete — is right and is this
project at its best. Keep AC-2, AC-3 (KC-34-2 is correctly load-bearing), AC-4,
and AC-5. Replace AC-1's sponsor hunt with downstream-packageability work and a
one-line note on why no ask was made; land the verdict as a row in #857's
ledger plus an ARCHITECTURE.md recorded decision rather than a new document;
and file Nixpkgs submission as the channel this capstone is missing.
