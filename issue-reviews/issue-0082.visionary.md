# Issue #82: Distribution: jpackage installers per OS and .jls file association — remove the bring-your-own-JDK barrier
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

One sentence: *a student with no Java knowledge gets a working JLS in one double-click.* That
is the correct north star for this project, it is the single highest-leverage ergonomic change
in the whole backlog, and the work under #82 has substantially achieved it. `scripts/build-installer.sh`
(514 lines) is a genuinely good piece of architecture: one recipe, jar → `jdeps` → `jlink` →
`jpackage`, consumed byte-for-byte by release.yml, ci.yml's msi lane, and the repro probes, with
the `%f` field-code bug found and fixed by actually installing the thing. Nothing below argues
against that.

The reframing is about *what is left*, and about which of the remaining items actually serve the
north star versus which serve the issue's own bookkeeping.

## Observation 1: the residual is smaller than its tracking apparatus

Measured against the tree, the entire outstanding engineering content of #82 is:

1. two `sha256` strings (`scripts/build-installer.sh:298-299`),
2. one `workflow_dispatch` run of an already-authored five-leg matrix,
3. flipping four booleans (`release.yml:300,303,307,310`),
4. two association assertions inside smoke steps that already install the artifacts.

Against that sit: this feature issue, children #284 and #285, a third issue #443 that specifies
the *same* four items in far greater detail under a different parent (#338), a pass-1
deduplication comment, a pass-2 comment correcting pass 1, and an explicit escalation to a human
to decide whether lower-number-wins or the tier model governs. The coordination cost has
overtaken the work by a wide margin, and it is still growing — each pass adds prose to #82
without removing a placeholder from the script.

The elegant move is to make the problem disappear by ownership, not by argument:

- **Close #284 and #285 into #443.** #443 already contains their content, plus the two things
  they lack — a drift test (`InstallerMatrixPolicyTest`) so the promotion cannot silently regress,
  and the insistence that a fallen-back JBR build become a *build failure* rather than a warning.
  The "#443 is the higher number" objection is bookkeeping; the tier model is the substance.
- **Close #82 itself against its §1 capability, now, without waiting for #443.** Its capability
  statement — "a user with no pre-existing JDK installs JLS, launches it, double-clicks a `.jls`"
  — is *true today on Linux* and is true in practice on Windows/macOS (the artifacts build and
  self-smoke-test; only the gate is advisory). What #82 is actually waiting on is not a
  capability, it is an *acceptance surface*: legs required, digests armed, assertions per OS.
  #338 was created to own exactly that, and its own boundary note says so. A feature that stays
  open until its successor feature's acceptance criteria are met is a feature that has no
  closing condition of its own.

I am explicitly disregarding #82's Completion Criteria checklist here. Its items are not wrong,
they are *relocated*: every one of them is restated, more sharply, in #443 §14. Keeping both
lists alive guarantees the two drift, and the last three comments on this issue are that drift
happening in real time.

## Observation 2: the JBR bundle is the one piece pulling against the arc

Everything else in this feature reduces dependencies. This one adds a permanent, hand-fed one.

To ship Wayland-native `WLToolkit`, the Linux installers bundle a JetBrains Runtime fork pinned
to `jbrsdk-25.0.3-b508.16`, whose digests can only be obtained by a human on an unproxied machine
(`build-installer.sh:282-284`). That is not a one-time chore: it recurs at every JBR bump,
forever, in a project whose entire quality culture is "a check that re-runs beats a claim someone
remembers making." It is the only input in the pipeline that cannot be verified by CI. It also
grows the Linux artifact and puts a vendor fork between students and their runtime.

What it buys: correct rendering on sessions where `WAYLAND_DISPLAY` is set and `DISPLAY` is
*not*. On GNOME, KDE, and default sway configurations, XWayland is present and `DISPLAY` is set,
so the mainline jlink image already works — which is precisely why the fallback lane has been
shipping happily for a month. The genuinely XWayland-less session is a power-user configuration,
and that user can run the jar.

Concrete alternative, offered as a falsifiable swap for #285:

> Ship the mainline-JDK jlink image in the Linux installers. Make the launcher detect the
> Wayland-only case and *fail with a sentence* ("this session has no X server; install XWayland,
> or run the jar under a Wakefield/JBR runtime — see README") instead of an AWT stack trace.
> Narrow README:176 to say the Wayland-native row is supported *via the jar*, not via the
> installer. Revisit when mainline OpenJDK ships `WLToolkit`.

Test to decide it: count the target-audience distributions whose default session omits XWayland.
If that number is near zero, the JBR bundle is paying a permanent manual-provenance tax for a
population that can be served by one diagnostic message — and #285, the two placeholders, and
one whole class of supply-chain risk all vanish. If it is not near zero, arm the pins and I
withdraw this. Note the cost is not symmetric: keeping JBR also keeps `git grep
UNVERIFIED-PLACEHOLDER` non-zero, which #338's IC-2 makes a *required check* — so this one
decision blocks a gate two features away.

## Observation 3: the durable interface is the asset names, and nothing guards them

This is the piece #82 has not yet noticed about its own future. Its §3 names the release asset
set as what it modifies, but the asset naming lives as ad-hoc `mv` calls
(`build-installer.sh:266,470,505`) and a shell-built `SUMS=` string (`release.yml:643`), with no
test anywhere in `test/jls/` asserting the scheme — the drift-test idiom exists in the tree
(`ToolkitPolicyTest`, `MenuAcceleratorPolicyTest`) but is not applied here.

Meanwhile the roster now carries Flathub (#849), winget (#855 — "InstallerSha256 read from the
published checksums asset"), Homebrew cask (#858), a download-count collector keyed on per-asset
names (#861), and a Debian go/no-go (#864). All five parse `JLS-<version>-<arch>.<ext>` and
`SHA256SUMS-installers-<os>-<arch>`. A tidy-up rename inside `build-installer.sh` would break
four downstream automations in four different repositories, silently, at release time.

So sharpen §4 invariant 2 from *"one recipe"* to *"one published contract"*: freeze the asset
name grammar and the checksums-file name in a table (in `docs/`, or beside the README install
section), and assert it with a drift test that reads the script and the workflow. That is maybe
forty lines of test, it costs nothing now, and it is the difference between "adding a channel is
a manifest" and "adding a channel is an archaeology expedition." #82 is the natural owner because
it is the producer; every channel feature is a consumer.

## Observation 4: a truthfulness gap that invariant 5 already forbids

README:31-36 states as present fact that the Windows installers "are Authenticode-signed through
SignPath.io's open-source program," while #134 remains open on maintainer enrollment and
`release.yml`'s `SIGNPATH_ENROLLED` evaluates to false unless the secrets exist — every signing
step skips cleanly and an unsigned msi ships. §4 invariant 5 says the signing stance stays
"documented and truthful in the README as it changes." Today it documents a stance that has not
landed. This matters beyond tidiness: #859 asserts the Homebrew cask's caveat text *equal* to the
README's paragraph by a committed drift check, so the README is becoming a normative artifact
that other repositories mirror. Fix it with a conditional sentence ("releases from vX.Y onward
are signed; earlier assets are not — verify the publisher") before any channel starts copying it.

## Where this leaves the arc

The distribution surface is now ~10 channels for a single-jar Swing app with one maintainer, and
#82 sits at the hub of all of them. That is defensible *only* if the hub stays one recipe with a
frozen output contract and zero hand-fed inputs. Two of those three hold. Finish the third by
deleting the hand-fed input (Observation 2) or by automating its verification, freeze the output
contract (Observation 3), hand the acceptance surface to #338/#443 (Observation 1), and close
#82 having actually removed the bring-your-own-JDK barrier — which it did, a month ago.
