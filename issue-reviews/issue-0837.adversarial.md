# Issue #837: TASK-C572-3: the verdict is written down with its numbers and its ranked fallback, so PF-2 starts building instead of re-arguing
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#837 is TASK-C572-3, the third and final task under FEAT-C32-1 (#572): after
TASK-C572-1 (#833) measures CheerpJ load time and TASK-C572-2 (#835) checks
interaction fidelity, this task writes the committed go/no-go decision
document. The instinct to force a written, binding verdict (so PF-2, #573,
doesn't re-argue the mechanism) is sound project discipline and matches the
pattern already used for KC-32-1. But the task narrows the evidence the
decision document is required to cite relative to what its own dependencies
(#833, #835) were told to collect, offers no verdict category for a mixed
result, asks for a "verbatim" quote of text that exists in three
non-identical phrasings across the issue family, and closes with a
cross-issue AC that nothing checks.

## Findings, most severe first

### 1. [High] The decision document's required evidence is narrower than what its own dependencies were told to gather — licensing and network-purity can be silently dropped from the verdict

AC-1 says: "A dated decision document is committed in-tree recording go or
no-go, with the per-circuit load times and fidelity results cited inline."
That's it — load times and fidelity results. But #833 (TASK-C572-1) AC-5
says explicitly: "Payload size and any licensing constraint of the wrapping
toolchain are recorded alongside the timings — **they bear on the verdict as
much as the seconds do**." And #835 (TASK-C572-2) AC-3/AC-4 require
verifying the demo has no reachable save/upload/user-content path and
contacts no runtime network endpoint beyond static assets — both direct
proxies for whether CheerpJ's actual deployment (which, per the prior
review of #572, typically loads its runtime from Leaning Technologies' own
CDN) is compatible with the "static files only" and licensing constraints
the whole capstone depends on. #837 requires none of that in the committed
document. As written, an implementer can produce a fully AC-1-compliant
"go" verdict citing only speed and click-toggle results while a licensing
blocker or a live third-party CDN dependency recorded by the upstream tasks
never makes it into the record the maintainer actually reads.

**Recommendation:** amend AC-1 to require the document cite payload size,
licensing constraints, and the network-panel/read-only findings from #833
AC-5 and #835 AC-3/AC-4, not just load time and toggle-fidelity.

### 2. [High] "Records the KC-32-2 scope cliff verbatim" is unfalsifiable — three non-identical wordings of KC-32-2 exist in the issue family, and #837 never names which one is canonical

AC-4: "The document records the KC-32-2 scope cliff **verbatim**... citing
the CAP-19 (#500) closure." KC-32-2's actual origin is #516 (CAP-32):
"KC-32-2: Scope cliff: any feature request that makes this an *editor* in
the browser is out of scope by construction and cites the CAP-19 closure."
#572 paraphrases it as "anything that turns the demo into a browser
*editor* is out of scope by construction and cites the CAP-19 closure," and
#573 as "any feature request that makes this an *editor* in the browser is
out of scope by construction and cites the CAP-19 (#500) closure." These
are three distinct strings — none is byte-identical to another — and #837
cites neither #516 (the actual source of the KC-32 numbering) nor any other
anchor for which one counts as "verbatim." An implementer can paste any of
the three (or invent a fourth close paraphrase) and technically satisfy
AC-4; the criterion cannot be checked mechanically or even by a careful
human without first resolving an ambiguity the issue itself doesn't
acknowledge exists.

**Recommendation:** name #516 as the canonical source and quote its exact
KC-32-2 sentence, or drop "verbatim" in favor of "cites #516's KC-32-2 by
number and preserves its scope."

### 3. [Medium] No verdict category exists for a mixed/partial result, though the upstream tasks are structured to produce exactly that

AC-1 and AC-2 are strictly binary: "go or no-go," and on no-go, alternatives
ranked. But #835 (TASK-C572-2) AC-1 requires pass/fail to be recorded **per
circuit**, and #833 AC-2 requires per-circuit load times against the 15s
line. Nothing prevents (and the interaction-fidelity concerns raised against
CheerpJ's threading model in the prior review of #572 make it plausible)
a result where, say, two of the three example circuits pass both bars and
one — likely the more sequential/register-heavy one — fails. #837 gives the
document author no instruction for this case: force it into "go" (hiding a
known failure mode), force it into "no-go" (discarding two working
measurements), or invent a third category unsanctioned by the issue. Given
KC-32-1's binding, no-relitigation intent, an ambiguous split-result
protocol is exactly the gap that invites a future re-argument the whole task
chain exists to prevent.

**Recommendation:** add an AC defining how a split per-circuit result maps
to the binary go/no-go call (e.g., majority of three, or unanimous-required)
before the document is written, not after.

### 4. [Medium] AC-5's cross-issue reconciliation is asserted with no verification mechanism

AC-5: "#573's ordering note is reconciled with the verdict — the mechanism
named here is the mechanism that feature is built against, recorded in a
comment there." This is an action (post a comment on a different issue)
smuggled into an acceptance criterion with nothing to check that the
comment's content actually matches the verdict, or even that it was posted
at all, beyond someone's say-so. Compare the project's own stated practice
in ARCHITECTURE.md's Test layout section, where cross-cutting invariants
get a named enforcing test (`ElementConstructorContractTest`,
`HelpTopicsTest`, etc.); this is a cross-issue invariant with no analogous
enforcement, self-attested exactly like the AC-4/AC-5 gaps the sibling
reviews of #572 and #573 already flagged for this same issue family.

**Recommendation:** at minimum require the comment link be pasted into
#837's own closing comment, so the reconciliation is auditable from #837
without trusting a separate issue's history.

### 5. [Medium] The task inherits, without re-checking, the undefined "three biggest examples" corpus problem already flagged against #572

Neither #833 nor #835 (nor #837) names a metric for "biggest" (element
count? file size? subcircuit depth?), and the curated example set that is
supposed to supply the corpus (#548, FEAT-C27-2) is still open, not landed
— confirmed: no `resources/samples/` directory exists in this checkout, and
the only `.jls` files present are test fixtures explicitly off-limits as
samples per #73's fresh-authorship rule. #837's decision document will cite
"per-circuit load times and fidelity results" (AC-1) for whatever corpus
#833/#835 happened to use, but #837 adds no requirement that the document
name and justify that corpus. A verdict built on an unspecified, possibly
unrepresentative three-circuit sample is weak evidence for a "binding,
no-relitigation" decision, and #837 is the one place in the chain where
that weakness could be caught before it's cemented into a committed
document.

**Recommendation:** add an AC requiring the document name the corpus
selection metric and source, or block #837 on #548 landing (mirrors the
recommendation already made against #572).

### 6. [Low] "The winner named as the mechanism FEAT-C32-2 (#573) builds on" never states what #573 should do if the winner is fallback (b), a non-interactive artifact

The Outcome text's phrasing — "the winner named as the mechanism FEAT-C32-2
(#573) builds on" — is symmetric across all three possible outcomes (go;
no-go→(a) SVG+VCD player; no-go→(b) recorded video), but #573's own Outcome
text ("watches and pokes a running JLS example circuit") is written only
for the interactive outcomes. The prior review of #573 already flagged this
same contingency gap on the receiving end. #837 is the task actually
positioned to force the fix (its AC-5 is the reconciliation step), yet AC-5
only asks that the mechanism be "recorded in a comment" on #573, not that
#573's own Outcome/AC text be checked for consistency with a video-fallback
result.

**Recommendation:** if the verdict lands on fallback (b), AC-5's comment on
#573 should explicitly flag that #573's Outcome/AC-1 wording needs a rewrite
before that issue starts, not just record which mechanism won.

## What's solid

- AC-3 (documenting what evidence would reverse the verdict) is genuinely
  good practice and directly supports the "re-check, not fresh argument"
  goal the issue states — no gap here.
- Correctly declares `ordering_after: ["TASK-C572-1", "TASK-C572-2"]`,
  matching the real data dependency (you can't write the verdict before the
  measurements exist).
- Citing the CAP-19 (#500) `not_planned` closure as grounding for the scope
  cliff is consistent with how #516, #572, and #573 all use that closure —
  no internal contradiction there, just the wording-fidelity problem in
  finding 2.
- The core motivation — a written, binding decision so PF-2 doesn't
  relitigate the mechanism — is exactly the discipline KC-32-1 calls for and
  is worth keeping even after the fixes above.
