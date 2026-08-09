# Issue #581: FEAT-C34-3: `brew install --cask jls` installs the dmg, and the cask's caveat tells a macOS user about Gatekeeper before they hit it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this is really for

Two wants are bundled: (a) a macOS user should be able to install JLS the way they
install everything else, and (b) the unsigned-app friction should be disclosed before
it bites. Both are legitimate, and the second is unusually well-motivated — the README
(`README.md:37-44`) already states the unsigned-by-choice stance plainly, and #443 §12
explicitly forbids silently upgrading it. The issue's instinct — *surface the stance at
the point of install, don't re-litigate it* — is the right instinct.

The route chosen to get there does not survive contact with the three systems it
depends on: Homebrew's admission policy, macOS's current Gatekeeper behavior, and JLS's
own release pipeline. Each of the three breaks a different acceptance criterion, and
together they point at a different seam.

## Three facts the issue does not account for

**1. `brew install --cask jls` — the literal title — is inadmissible today.**
homebrew-cask's notability gate (as I know it: ~75 stars / 30 forks / 30 watchers for a
GitHub-hosted project) is checked before review. `anadon/JLS` currently reports **3
stars, 9 forks, 3 watchers** (GitHub API, this session). Every threshold is missed by
an order of magnitude. The issue's own escape hatch — "own tap or homebrew/cask, either
satisfies the outcome" — is not equivalent: after `brew tap anadon/jls` the short token
does resolve, so AC-1's *command string* is reachable, but the parent capstone's actual
promise (#518: "JLS is **one command away**", "each store listing is a **discovery
surface**") is not. A tap is only findable by someone already reading the README — at
which point the dmg link is two lines above. **A tap channel delivers ~0% of the
capstone's discovery value at 100% of its per-release maintenance cost.** That is the
kill criterion KC-34-1 should have been written as, and AC-5's mw arithmetic will never
detect it because AC-5 measures cost, not reach.

**2. AC-3 enshrines the wrong text in the wrong context, and it is decaying.**
The "README's Gatekeeper paragraph" is not a paragraph; it is a markdown bullet
(`README.md:37-44`) containing issue cross-references (`#128, #135`), a rationale about
Apple Developer Program fees, and the sentence "Intel Macs: use the jar below."
Copying it *verbatim* into a `caveats` block produces text that is partly nonsense to a
brew user (there is no "below"; `#128` is not a URL) and partly wrong (a cask user did
not double-click anything in Finder; they typed a command). Worse, the instruction
itself is stale on current macOS: Apple removed the Control-click→Open bypass for
unnotarized apps in macOS 15, redirecting users to System Settings → Privacy & Security
→ "Open Anyway". An equality assertion between two texts does not make either correct;
it makes both wrong together, on a schedule set by Apple. **AC-3 is a drift *detector*
where the project's own idiom is drift *prevention*** — `src-filtered/version.properties`
is filtered from `pom.xml` precisely so the version cannot diverge. Single-source the
Gatekeeper text as a fragment both README and cask render from, and AC-3's test has
nothing left to assert.

**3. The dmg leg cannot yet fail a release.** #443 O3 records `experimental: true` for
four of five installer legs, `macos-latest` among them (`release.yml:308`), and states
the dmg smoke test has never gone green on a real run. The dmg is also **aarch64-only**;
a cask without `depends_on arch: :arm64` hands Intel users a broken install, and README
sends them to the jar instead. So #581 proposes exposing, to a public package manager
with automatic upgrades, an artifact the project itself does not yet consider
release-blocking, on a subset of the hardware brew runs on. `ordering_after: []` with the
note "#443 strengthens but does not gate" reads the dependency backwards: publishing a
channel is exactly what converts "a leg we haven't proven" into "a broken upgrade on
someone else's machine."

## The reframing: cut at the jar, not the .app

The whole issue exists because the seam was drawn at the `.app`/dmg. Move it one layer
down and three problems vanish at once.

**A Homebrew *formula* over the jar** — `depends_on "openjdk"`, install
`jls-<version>.jar` into `libexec`, `bin/jls` shim — gives:

- **No Gatekeeper.** Formula-installed files are not quarantined; nothing launches
  through LaunchServices. There is no caveat to write, no README text to copy, no
  drift check to maintain. *The problem the issue is built around ceases to exist.*
- **Both architectures, and RISC-V-shaped honesty.** The jar is arch-independent; Intel
  Macs stop being a documented exception.
- **A stronger integrity story than AC-1 asks for.** README is explicit that the
  installers are *not* byte-reproducible while the jar and `bom.json` are, with a
  published `.buildinfo` and an independent-rebuild recipe (`docs/reproducibility.md`).
  A formula's `sha256` therefore pins an artifact a third party can *reconstruct*, not
  merely attest. AC-1 settles for the weaker of the two available guarantees.
- **Alignment with what already works.** `nix profile install github:anadon/JLS` and
  `ghcr.io/anadon/jls` are both self-hosted, gatekeeper-free channels the project
  already ships. A jar-based formula is the same shape.

The honest cost: no `.app` bundle, no Dock icon, no `.jls` double-click association —
which matters for students, and #443 treats that association as a load-bearing README
promise. So the synthesis is **both, with the cask demoted to a convenience**: formula
as the primary macOS brew path; cask (in the tap) for users who want Finder integration,
installed with the documented `--no-quarantine` flag so Gatekeeper never fires either.
Verify the current flag semantics before committing to that wording — but note the
shape: *the cask is the one channel where the Gatekeeper wall can be removed rather than
narrated*, and the issue spends its entire outcome section narrating it.

**Second reframing, capstone-wide:** #579/#580/#581 each independently specify "a
release-workflow step updates version and sha256." Three hand-rolled bump-and-PR
mechanisms against three vendors is the four-vendor drift lesson CAP-21's KC-21-3
already taught. One `distribution/channels.toml` (version, sha256s pulled from the
existing `SHA256SUMS-installers-*` assets, shared CAP-27 description, the single-sourced
Gatekeeper fragment) rendered by one script into all channel manifests is the seam these
three features are collectively missing — and it makes AC-4 ("carries the shared
description") a structural fact instead of a per-channel checklist item.

## What I am disregarding, and why

- **AC-3 (verbatim equality with README) — disregarded.** Replace with: the Gatekeeper
  text lives in exactly one file; README and any caveat both render it; divergence is
  unrepresentable. Detecting drift is strictly worse than making it impossible, and the
  repo already knows this.
- **AC-1's `brew install --cask jls` on a clean machine — disregarded as stated.**
  Unreachable in homebrew/cask at 3 stars; reachable in a tap only after `brew tap`,
  which is not the promise. State the real command in the README or don't ship it.
- **`ordering_after: []` — disregarded.** Gate on #443's macOS leg going green and
  non-experimental. Publishing a channel over an unproven artifact is how a packaging
  bug becomes a user's broken upgrade.

## What I would keep

The refusal to re-decide signing here is exactly right and should survive any redesign:
#82 §10, #128, #135 own the stance, #338/TASK-0028 own criterion 5, and this work must
not upgrade the claim by implication. Likewise AC-5's cost recording — extended, per
above, to record *reach* alongside cost, so a channel that costs little and reaches
nobody is still correctly killed.

## Restated outcome I would endorse

> A macOS user can `brew install anadon/jls/jls` and get a working `jls` command backed
> by the byte-reproducible jar, with no Gatekeeper interaction because none is needed;
> an optional cask in the same tap installs the app bundle for `.jls` association, and
> both manifests are generated from one in-tree channel description that also sources
> the README's macOS text — gated on the dmg leg being a required check.

That is smaller than #581, strictly more honest about what Homebrew will accept, and it
retires the caveat-drift problem instead of institutionalizing it.
