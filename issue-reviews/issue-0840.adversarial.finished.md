# Issue #840: TASK-C573-1: one example runs on a hosted static page whose deployment is a file copy — the demo exists before it has a catalogue
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#840 is TASK-C573-1, the first of at least three tasks (#841, #844 follow)
decomposing FEAT-C32-2 (#573) into an incremental build: get one example
circuit hosted and running end-to-end before scaling to the full curated
set. That incremental shape is sound engineering discipline in the
abstract. But the task is written as a build ticket for a mechanism that
does not exist yet — its sole `ordering_after` entry, #572, has not
produced a go/no-go, and the task inherits #573's unresolved "read-only by
construction" and "static files only" claims without naming how either
becomes true for the one mechanism actually on the table (CheerpJ). It also
never names which example circuit it means, despite the parent feature's
content source (#548) still being unfiled work.

## Findings, most severe first

### 1. [Critical] The task's premise — "the mechanism that won #572's go/no-go" — does not exist

Quoted: *"The mechanism that won #572's go/no-go becomes a real hosted
page..."* and the YAML's `ordering_after: ["#572 (its go/no-go names this
page's mechanism)"]`. I fetched #572's comments directly:
`issue_read(method:get_comments, issue:572)` returns exactly one comment, a
feature-deduplication boundary note ("No merge — sequential, rule 3(b)")
that explicitly does **not** run the spike or declare go/no-go. #572 is
still `state: open`. #572's own AC-4 requires "an explicit written go/no-go"
before any downstream work proceeds, and #572's own prior adversarial review
(`issue-reviews/issue-0572.adversarial.md`, finding 1) independently found
the spike's input corpus doesn't even exist yet. #841 (TASK-C573-2, the next
task in this same chain) reaches the identical conclusion in its own review.
There is currently no mechanism for #840 to build against — the task cannot
be started in good faith today, only speculatively pre-built against a
guess (almost certainly CheerpJ, since it's #572's primary candidate).

**Recommendation:** either re-state `ordering_after` as a hard block (not
build until #572 posts its go/no-go comment) or, if the intent is to spike
ahead of #572's formal verdict, say so explicitly and drop the "the
mechanism that won" language, which asserts a decision that hasn't been
made.

### 2. [High] No named example circuit, and no tie to the curated set that's supposed to supply it

The Outcome text says "carrying exactly one example circuit" but never says
which one, on what selection basis, or where it comes from. The parent
feature #573's AC-4 requires the eventual page to "serve the CAP-27 curated
example set (#548), each with its caption" — but #548 is open, and
`find . -ipath "*resources/samples*"` and `find . -iname "*curated*"`
(run against this checkout) both return nothing: no curated example set
exists in the repository today. #840's `ordering_after` cites only #572,
not #548, so the task appears free to invent an ad hoc circuit rather than
draw from the (not-yet-authored) curated set. That risks throwaway work:
whatever gets hosted here may need to be replaced once #548 lands with its
own selection criteria (category coverage, captions, exercises per #548
AC-2/AC-4), and nothing in #840 says the first example must be reusable
rather than disposable.

**Recommendation:** name the specific circuit (or the selection rule) and
state explicitly whether this first hosted example is throwaway or is meant
to become one of #548's eventual ten-plus, with a caption compatible with
#548 AC-4's format.

### 3. [High] AC-3's "read-only by construction... verified by inspection" restates an unresolved design gap from the parent feature, with a weaker, ungated check

Quoted: *"AC-3: The page is read-only by construction — no save, upload, or
user-content path exists in the shipped bundle; verified by inspection and
recorded."* #573's own adversarial review (finding 2) already flagged that
CheerpJ's default mode runs the actual Swing GUI unmodified — same
`File > Open`/`File > Save As` menu paths, same reflective circuit loader
`ARCHITECTURE.md` describes — so "read-only by construction" is not true of
that artifact unless something strips or disables the file-menu I/O for the
demo build, and neither #572 nor #573 nor #840 names that stripping step.
"Verified by inspection and recorded" additionally has no test or CI
artifact backing it — it is a prose sign-off, gameable by a reviewer simply
asserting the property held at spot-check time, with nothing to catch a
later mechanism change (e.g., a CheerpJ version bump that re-exposes a
previously grayed-out menu item) from silently reintroducing a write path.
This matters more here than in #573 because #840 is the task that actually
ships bytes to a public host — the first place this gap becomes a real,
externally reachable liability rather than a design note.

**Recommendation:** name the concrete mechanism (build flag, menu-item
removal patch, or CheerpJ config) that makes read-only true, and back AC-3
with an automated check (e.g., a build-time assertion that the shipped
bundle's DOM/menu tree contains no save/open action) rather than "verified
by inspection."

### 4. [Medium] AC-4/AC-5 reproducibility covers the local build but not the runtime dependency the leading mechanism actually has

Quoted: *"AC-5: A rebuild from the same inputs produces the same bundle
contents, so what is hosted traces back to a known build."* This is a
build-determinism claim about files this repository controls. But CheerpJ's
standard deployment path loads its runtime loader from a third-party CDN
(Leaning Technologies) at every page view, not at build time — a live
external dependency the "file copy" deployment model doesn't touch and
"rebuild produces same bundle" doesn't detect if it changes or disappears.
This is the same gap #573's review flagged (finding 3) against #573's
"zero operational upkeep" claim; #840 inherits it verbatim into AC-4/AC-5
without addressing it, even though #840 is the task that would actually
stand the page up. A bundle can be byte-reproducible from committed inputs
while the hosted demo still silently breaks the day the CDN changes terms
or goes dark — exactly the "nothing that can die and take user data with
it" property the issue's own title claims to deliver, undermined by the one
thing outside the repository's control.

**Recommendation:** either require self-hosting/vendoring the CheerpJ (or
winning-mechanism) runtime as part of "the deployment," or add an AC that
names the external runtime dependency and states the fallback if it becomes
unavailable.

### 5. [Medium] AC-1 drops the load-time threshold that #572/#573 both treat as load-bearing

#572 AC-1 sets a hard go/no-go number ("go threshold: ≤15 s"), and #573
AC-1 restates it ("reaches a running example in <30 seconds from click").
#840's AC-1 only says *"A stock browser with no JDK reaches a running
example circuit from a link, and can toggle an input and see the trace
change"* — no time bound at all. Since #840 is meant to prove the "page's
operational shape... before the content set is scaled," dropping the timing
requirement here is a real gap: a demo that takes two minutes to become
interactive still passes AC-1 as literally written, silently regressing the
metric the entire mechanism decision (#572) exists to protect, with no
downstream task obligated to re-check it against a single example.

**Recommendation:** restate the same threshold (≤15 s or <30 s, whichever
this task family has settled on) in AC-1, not just in the sibling issues.

### 6. [Low] AC-1's demo-quality bar is trivially satisfiable by an unrepresentative circuit

"Toggle an input and see the trace change" is satisfied by a single AND
gate wired to one switch — a circuit that proves the mechanism plumbing
works but demonstrates nothing about JLS's actual value (registers, memory,
sequential behavior — the classroom content #548 is meant to showcase).
Since finding 2 already shows no circuit is named, there's nothing stopping
"exactly one example" from being the least interesting circuit that
satisfies the letter of AC-1.

**Recommendation:** require the chosen example to include at least one
stateful element (register, memory, or clock) so the spike also proves the
sim-thread/EDT ordering risk #572's review (finding 4) already flagged, not
just that a wire toggles.

### 7. [Low] Relationship between this task's build and #572's spike artifact is unstated

#572's own AC-1/AC-2 already produce "a running example circuit a visitor
can poke" (as a measurement artifact) before #840 exists. #840 doesn't say
whether it reuses/hardens that spike output or builds fresh — if the former,
a throwaway feasibility spike quietly becomes the shipped production
artifact with no stated review gate between "prove it's possible" and
"host it publicly"; if the latter, the same integration work (CheerpJ
wrapping, threading behavior, hosting config) is done twice with no
acknowledgment of the duplication.

**Recommendation:** state explicitly whether #840 hardens #572's spike
build or starts over, and if hardening, name what changes between "spike"
and "shipped."

## What's solid

- The incremental shape — one example proven end-to-end before scaling to
  the full catalogue — is the right instinct; it mirrors #572's own
  "measurement before commitment" discipline and would catch integration
  problems (deployment pipeline, hosting config, threading) cheaply if the
  mechanism question were actually settled first.
- AC-2's requirement that the publish step be "reproducible from a
  committed script or workflow" (not manual) is a good, checkable bar and
  consistent with this project's existing reproducibility discipline
  (README's jar/BOM reproducibility story, `docs/reproducibility.md`).
- The `part_of_feature: 573` / `ordering_after` scaffolding correctly
  identifies that this is a sub-task, not an independent feature — the
  decomposition itself is legible.

## Recommendation

Do not schedule implementation on #840 until #572 has posted an actual
go/no-go comment naming the mechanism — right now the task's Outcome text
("The mechanism that won #572's go/no-go") describes a decision that has
not been made. Once it lands: name the specific example circuit (and
whether it's disposable or destined for #548's eventual set), replace AC-3
and AC-4/AC-5's prose assertions with checks tied to the named mechanism's
actual properties (menu-stripping for read-only, runtime-hosting story for
"zero upkeep"), and restate the load-time threshold #572/#573 both already
carry so this task doesn't silently drop the one number the whole spike
exists to protect.
