# Issue #526: FEAT-C21-3: a repo generated from the in-tree Classroom starter grades itself on push — the jls-grade Action annotates failing tests on the exact circuit files and reports Classroom points
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of platform vocabulary, #526 is a bid for one thing: **a student pushes a
drawing and gets told, on the drawing, what is wrong with it.** Classroom points are
bookkeeping; the annotation is the product. CAP-21 (#502) frames the whole kit as
"delivery" — four adapters that carry an existing verdict to four vendors — but this
one adapter is the only one of the four that owns a *feedback surface* rather than a
score envelope. That is where its value and its unowned risk both sit.

The direction is right and aligned. `README.md` sells JLS as a university tool with a
signed multi-arch headless container explicitly labelled "for autograders and CI";
`docs/batch-interface.md` §6 already treats the grading surface as a frozen contract;
`examples/autograde/autograde.py` is the measure of the gap (grading by three literal
stdout lines, and `docs/capability-roadmap/lf-04-formal-and-grading.md:50-53` notes a
submission wrong on 254 of 256 inputs passes it). Nothing in the project pulls against
carrying that onto GitHub Classroom. My objections are all about *where the seam is
cut*, not whether to cut.

## 1. The load-bearing acceptance criterion rests on a capability nobody owns

"Annotates failing tests on the exact circuit files" is the sentence that distinguishes
this issue from its three siblings (per the dedup note in the comment). GitHub check
annotations are `(path, start_line, end_line, message)`. Two preconditions follow, and
the issue states neither:

- **The submitted `.jls` must be plain text.** Current JLS saves XZ (`README.md`
  "Circuit files"; `docs/file-format.md:58`). An annotation on line 1 of a binary blob
  is noise. So the starter template must *mandate* `-savetext` / Save-As-plain-text —
  a policy decision with real student-workflow consequences (it also makes diffs work,
  which is a gift, but it must be decided, documented and enforced by a pre-flight
  check in the workflow, not discovered).
- **A failing test must map to a line.** Today nothing produces that mapping. But the
  material is already in the tree: `docs/file-format.md:103-122` fixes the canonical
  layout at one `ELEMENT` record per line, and `LoadError` (`src/jls/LoadError.java:25-31`)
  *already* carries `(line, element)` because the loader tracks its position.

**Reframing:** the real feature hiding inside #526 is **circuit source provenance** —
`Circuit.load` retaining an element → source-line map, and the frozen xUnit report
(#524) emitting `file` + `line` on every `<testcase>`/`<failure>`. Cut there and the
Classroom-specific work collapses to a template plus an off-the-shelf JUnit-XML
annotator, *and* the same map pays for GUI jump-to-error, HDL-export diagnostics that
name a drawing location, lf-06's diff/merge line, and every future adapter. Cut where
#526 currently cuts, and the mapping gets invented inside a GitHub Action — the least
reusable, least testable place in the whole system, reachable only through a vendor
runner. This is the single highest-leverage change I would make to the plan, and it
belongs in #524's contract (or a new sibling), not here.

## 2. Four adapters are one program with four printers; parity should be structural

CAP-21 AC-1 asks a 300-submission corpus to *empirically verify* that four independent
adapters emit byte-identical score vectors, and KC-21-1 pre-writes the retreat if they
cannot. That is a lot of apparatus to defend a property that is free by construction:
one scoring core (xUnit → an ordered score vector, canonically serialized, no
timestamps/locale) plus four thin serializers into results.json, Classroom points,
PrairieLearn results and an nbgrader gradebook. Parity then cannot fail, because there
is one code path; #531's fixture degrades from "prove four programs agree" to "pin four
printers", which is a golden test of the kind this repo already runs by the dozen.

This also dodges the trap in AC-3 as written here ("the Action's *score summary* is
byte-identical to the other adapters' score vectors"). Classroom points are computed
and rendered by GitHub's own `classroom-resources` graders; JLS does not control those
bytes and should never claim to. Compare the artifact JLS emits, never the platform's
rendering of it. I regard AC-3's current wording as a KC-21-1 trigger waiting to fire
for a reason that has nothing to do with grading.

The pass-1 dedup verdict ("no merge", rule 3(c)) is fine as issue bookkeeping and
should not be read as an architectural finding: four distinct *outcomes* are entirely
compatible with one implementation and four ~50-line skins. Issue-level dedup rules
adjudicate text; they do not authorize four parallel implementations.

## 3. Do not ship a Marketplace Action at all

Open Question 3 ("publication under whose org?") exists only because the design assumes
a published, versioned `jls-grade` Action. Delete the assumption and the question
disappears along with a permanent maintenance surface: a moving `v1` tag, Dependabot
churn, action-supply-chain review, and a second release cadence for a single-maintainer
project that already ships six installer formats.

The starter template's workflow can instead run the artifact JLS already publishes:

```
docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls@sha256:<digest> -b -t hidden.t sub.jls
```

That *is* the pin (a digest, not a version string), the cache (the runner's image
layer cache), and the provenance — with the further move no autograder kit in this
space offers: a `cosign verify` step in the starter workflow, so the instructor can
prove the grader binary came from this repository's release run
(`.github/workflows/release.yml:229-243`). The kit's differentiator becomes *verifiable
grading*, not another Marketplace tile. I would drop AC-2's "pins and caches" phrasing
entirely: with a digest it is a documentation sentence, not an engineering task.

## 4. The payload is aimed one generation too low

As specified, #526 delivers what a good Logisim harness already delivers: a red X on a
file and a point total. Meanwhile `docs/capability-roadmap/lf-04-formal-and-grading.md`
argues — convincingly, and with element-level anchors — that JLS's unique position is
that it holds *both ends, the drawing and the verdict*, and that the counterexample
rendered on the student's own schematic is the thing no commercial or open tool can do.

JLS can already export SVG (`src/jls/JLSStart.java:765`, `-i out.svg`). So the Classroom
adapter's natural payload is: for each failing test, the minimized failing input vector
as a replayable `-t` file, plus an SVG/PNG of *the student's own circuit* with the
failing output highlighted — uploaded as a workflow artifact, linked from the check
summary, with the counterexample table inline in the annotation text. (Be honest about
the mechanics: GitHub markdown will not render a data-URI image, so the picture is an
artifact link, not an inline embed. The counterexample table inline is free.)

That reframing costs little here — it is mostly "carry whatever the verdict layer
produces, and don't flatten it to a boolean" — but it must be designed in *now*,
because an adapter whose interface is "test name + pass/fail + points" will have to be
reopened to carry a counterexample later. Concretely: make the adapter's contract with
#524 be "render every field of the xUnit failure, including attachments", not "extract
a score".

## 5. Keep the drift tax off the push path

CAP-21 risk 1 calls platform drift "the permanent tax" and this issue's AC-5 wants
runner-image drift to surface in a dedicated CI lane. Right instinct, one refinement:
make it a **nightly, non-blocking** lane, exactly as `gui-wayland` already does in
`.github/workflows/ci.yml`. A vendor changing an Ubuntu runner image must not block a
gate-fix PR to the simulator; a red lane that blocks unrelated work gets ignored, and
an ignored lane detects nothing.

## What I am disregarding, and why

- **AC-2's "pins and caches a specific JLS build"** — replaced by a container digest
  plus a cosign verification step (§3). The stated criterion measures effort that the
  release pipeline has already spent.
- **AC-3's byte-identity of the Action's *summary*** — JLS does not own those bytes
  (§2). Compare emitted score-vector artifacts; let the platform render.
- **Open Question 3 (Marketplace org)** — dissolved, not answered, by not publishing an
  Action (§3).

I would keep AC-1 (it is the outcome), AC-4 (recorded-artifacts-only is a genuine
normative constraint and cheap to honour here), and AC-5's doc-test — the scripted
README that executes in CI is the difference between a kit and a blog post.

## Bottom line

The goal is right and this is the correct second platform for a free university tool.
The issue is scoped as an integration when its hard, valuable, reusable core is a
*model* change — element source provenance surfaced through the frozen report — and it
buys a maintenance surface (a published Action) it does not need. Reframed: land
provenance in #524's contract, implement one scoring core with four printers, ship a
starter workflow over the signed container digest, and let the annotation carry a
counterexample rather than a checkmark. That version is smaller, strictly more useful
to the other three adapters, and it is the version that makes the four-way parity claim
true by construction instead of by fixture.
