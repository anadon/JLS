# Issue #859: TASK-C581-2: the cask's Gatekeeper caveat is the README's paragraph, asserted equal by a drift check — the user reads the workaround before macOS refuses the app
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery away and the goal is one sentence: *a macOS user who types
`brew install --cask jls` should learn the right-click-Open trick before
Gatekeeper refuses the app, and should never be told something the project no
longer believes.* That goal is right and squarely on JLS's arc. The project has
spent enormous effort making distribution honest — attestations, checksums, the
"scope of each guarantee" paragraph in README lines 53–60, SECURITY.md's custody
rationale for #136, the reproducibility split between jar and installers. A cask
caveat that quietly rots into a lie would be the one place that discipline
lapses. #859 is right to refuse that.

The *mechanism* is where I part company. #859 proposes: duplicate the README
prose into the cask, then police the duplicate with a byte-equality assertion, a
CI path filter, and a seeded-red test. That is 0.25 mw of policing built around
a copy that need not exist, mastered by the wrong file, at exactly the moment
CAP-34 (#518) is setting the pattern for three channels.

## Three things wrong with the seam, in ascending order

**1. The README is not a source of truth; it is a rendering.** README lines
37–43 are a single bullet whose subject is "how to get JLS on a Mac." It
interleaves the asset filename, the signing stance, the two issue citations
(#128, #135), the workaround, and "Intel Macs: use the jar below." AC-1 wants
"the Gatekeeper paragraph" extracted byte-equal from that — but there is no
paragraph there, only a sentence range inside a bullet, and the range that reads
correctly in a cask caveat is not the range that reads correctly in the README.
"the jar below" has no below in `brew info`. Either the cask ships a dangling
reference, or the README gets rewritten into context-free prose to serve a
machine extractor. AC-3's "stable marker" is the tell: it asks the README to
become a database with quotable rows.

**2. AC-1 forbids the better caveat.** Homebrew applies the quarantine xattr
itself, and `brew install --cask --no-quarantine jls` sidesteps the refusal
entirely — one command instead of a right-click dance a student has to
remember. That is a materially better first experience than reciting README
prose, and byte-equality *prohibits saying it*, because the README (correctly)
does not mention brew. The AC that exists to protect the user from stale text
also freezes the caveat at the least useful text available. (Whether the
`quarantine:` stanza is permitted in `homebrew/cask` versus documenting the flag
for the user is worth checking during TASK-C581-1; it also bears on #581's open
tap-vs-official-tap question.)

**3. If the cask lands in `homebrew/cask`, the check polices nothing.** A
committed check in `anadon/JLS` can only assert about a file in `anadon/JLS`.
Should the cask be submitted upstream — explicitly left open by #581 — the
shipped caveat lives in a repo the check cannot see, and the in-tree copy
becomes a third text asserting equality with a fourth. The drift check's value
is entirely contingent on a decision #581 deliberately deferred, and #859 never
notices.

Also worth recording: AC-5's "no independent copy of the stance is maintained
anywhere" is already false at filing time. The unsigned-by-choice stance is
restated in `docs/standards-landscape.md:472` and `:819`,
`docs/dmg-reproducibility.md:247` and `:267`, and `ISSUE-AMBIGUITIES-2026-07.md:319`
(which literally warns "keep that README hunk consistent"). #859 would add a
sixth copy and guard exactly one of the six edges.

## The reframing I would build instead

JLS already has the correct doctrine in-tree and just needs to apply it here.
`test/jls/CliFlagTableTest.java`'s header states it: *"the flag table in
JLSStart is the single authoritative CLI specification, usage() is generated
from it … These tests fail if anyone reverts to a hand-maintained flag list on
either side."* That is generation from one master plus an anti-regression test —
not equality between two hand-maintained copies. Applied here:

- One canonical fragment, e.g. `resources/distribution/macos-gatekeeper.txt`,
  holding the stance and the workaround as *context-free* prose plus the issue
  citations as structured metadata.
- The README's macOS bullet is **generated** around it (or checked to contain
  it) — the README becomes a consumer, not the master.
- The cask's `caveats` is **generated** from it by the same release-workflow
  step that #581 AC-2 already requires for version and sha256, with permission
  to append channel-specific text (`--no-quarantine`, `brew reinstall`).
- Byte-equality tests disappear, because there is nothing to compare. The
  anti-regression test becomes one assertion in the spirit of CliFlagTableTest:
  the generator is the only writer of these fields, and a hand-edited cask fails
  the round-trip.

This is strictly cheaper than #859 for the *second* channel onward, which is the
real point. CAP-34 ships winget (PF-2), Flathub (PF-1), and Homebrew (PF-3), and
CAP-27 (#511) shares a project description across all of them (#518 AC-4). Each
of those has a description field, a summary, a homepage, a license id, and
Flathub's appstream additionally wants a `<description>` and a caveat-equivalent.
#859's pattern costs one marker + one extractor + one CI filter + one seeded-red
test *per field per channel*. The fragment-and-generator pattern costs one
generator and one round-trip test for the whole matrix. Filing #859 as written
sets the per-field precedent on the very first field — that is the sense in which
this pulls against the project's arc rather than duplicating it.

I am therefore disregarding AC-1, AC-3, and AC-4 as written: byte-equality to a
README range, the README anchor marker, and the seeded-divergence test are
artifacts of the copy-and-police design and vanish under generation. AC-2 (CI
catches it) and AC-5 (one stance, one place) survive — indeed they are better
served, since AC-5 becomes true rather than aspirational once the other five
restatements point at the fragment.

## The out-of-the-box alternative worth costing first

CAP-34's outcome is "one command away on every mainstream channel." For macOS
there is a route that makes the Gatekeeper paragraph *have no reader at all*: a
Homebrew **formula** over the jar, not a cask over the dmg. The jar is the
artifact this project already declares byte-reproducible (README lines 92–95) —
unlike the installers, which are explicitly not — it depends on `openjdk` which
Homebrew already ships, it is invoked from a shell wrapper rather than launched
from Finder, and it carries no `.app` bundle for Gatekeeper to refuse. `brew
install jls` would then be the cheapest, most honest channel in the whole
capstone, and #859 would simply not exist.

The cost is real and I will not hide it: a formula gives no `/Applications`
entry and no `.jls` double-click association, and JLS's primary user is a
student drawing circuits in a GUI. But JLS's *other* well-served constituency —
autograders, lab machines, batch mode, the container image — is exactly the
formula's audience, and `brew install --cask jls` and `brew install jls` are not
mutually exclusive. My concrete suggestion: before building #859's machinery,
spend an hour deciding whether PF-3 should ship formula-first (Gatekeeper
paragraph never surfaces), cask-first, or both. Under KC-34-1's 0.5 mw
per-release threshold, the formula is the arithmetic-friendly leg.

## Bottom line

Keep the outcome; invert the direction of truth. The cask must not lie about the
signing stance — agreed, and worth CI enforcement. But the fix is one canonical
fragment feeding README, cask, and every future channel through the release
workflow, not a marker-anchored extraction that turns the README into a parsed
database and freezes the caveat at prose that cannot mention brew. And it is
worth asking, once, whether the macOS channel needs a caveat at all.
