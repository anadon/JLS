# Issue #489: Issue templates: record the tracker read-path quoting rule — `issue_read` deletes tag-shaped runs, `search_issues` is the lossless read path
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## The finding is true; I reproduced it in this session

Before anything else: the empirical claim is correct, and I did not have to take it on
faith. Reading #489 with `mcp__github__issue_read` returned `List` where the stored body
has `List<LoadError>`, empty backticks where it has `` `<word...>` ``, and `&#34;` where it
has `"`. Re-reading the same issue with `mcp__github__search_issues` + `fields: ["body"]`
returned all of it. F1–F4 hold. F5 follows from them by construction. The programme has a
real hazard here and the author has characterised it precisely.

So my objection is not to the fact. It is to the claim #489 makes about what JLS should
do with it, which is: transcribe 30 lines of prose into three files by hand, three times,
and gate acceptance on two MD5 sums.

## The block cannot be applied as instructed — verified

I reconstructed the capstone/feature block from the lossless read. `md5sum` of my 30 lines
is `aad569442a32e0e35432f0925d665ff2` — the exact value #489 gives — so what follows is
about the canonical text, not a transcription of mine.

Line 9 of that block reads:

```
      mermaid's `-->`) survive.
```

The block is to be inserted *inside* the templates' leading `<!-- ... -->` machine-read
comment. An HTML comment ends at the first `-->`. Simulating the insertion into
`feature.md` at the specified anchor (`     convenience rendering, never evidence.`):
the comment closes at line 91, and lines 10–30 of the block plus the file's own `-->`
fall outside it and render as visible prose at the top of every issue filed from that
template. Same defect in all three files. `capstone.md` and `feature.md` have no other
`-->` between `<!--` and their terminators (lines 78 and 82); `scientific_task.md` closes
at 104. Nothing protects them.

Now read the acceptance criteria against that: md5 matches, no tabs, no lookalikes,
nothing else changed, version-bump decision recorded. **Every box ticks on a broken
file.** An issue whose entire subject is "a silent transformation damaged text and the
tooling reported success" specifies a change whose own verification cannot see that the
text was damaged. That is not an unlucky typo — it is the mechanism failing in exactly
the way the issue documents, one layer up. It is the strongest available argument that
hand-copied prose in an HTML comment, checksummed in an issue body that no CI reads, is
not a mechanism at all.

## What the issue is actually for, and three seams cut wrong

The real goal is not "32 lines in three files." It is: **keep the evidence chain
trustworthy.** This tracker's whole tier model — 605 open issues, task → feature →
capstone, every claim carrying `file:line` at a pinned `evidence_commit` — is worth
exactly as much as the fidelity of its quotes. An agent that paraphrases `List<LoadError>`
into "a list of load errors," or rewrites a body from a stripped read, is dissolving the
one property those citations have. Defending that is squarely on the project's arc, and I
endorse the goal without reservation. The route misses at three seams.

**Seam 1 — wrong audience.** `.github/ISSUE_TEMPLATE/*.md` is delivered by GitHub's web
New Issue form. The agents that hit this bug file through `mcp__github__issue_write`,
which never renders a template. The block reaches its reader only if that reader goes and
opens the file — precisely the discipline it cannot be assumed to have, since the incident
is *about* agents not knowing the fact. Meanwhile the repository already owns a channel
that reaches every such agent unconditionally: `.claude/settings.json` and
`.claude/hooks/session-start.sh`, plus the conventional root `CLAUDE.md`/`AGENTS.md` that
does not yet exist here. Grep confirms the asymmetry: `issue_read` and `search_issues`
appear nowhere in this repository outside the two templates that would gain them.

**Seam 2 — wrong shape.** Three near-identical copies, diverging in three lines, pinned by
two checksums that must be kept in sync forever by hand. The templates already solved this
problem and solved it the other way: `feature.md` says "the scientific-task template's
rules 1–7 apply here, adapted to this tier"; `capstone.md` says "task rules 9–10 and
feature rules A–D read against this template." **Reference, not copy, is this project's own
established convention for shared tracker rules**, and #489 breaks it for its own rule
while adding the largest single block any of the three files would carry.

**Seam 3 — wrong scope of the claim.** The block's own heading is "QUOTING THROUGH THIS
TRACKER." That is false in the same shape as the false claim it exists to correct. The
stripping is not a property of the tracker (F3 says so explicitly); it is a defect in one
MCP client's response serialisation — an HTML sanitiser applied to a plain-text field.
When that client is fixed or swapped, 90 lines of permanent template text become permanent
misinformation, with "settled by experiment 2026-08-02, do not re-derive it" attached to
discourage anyone from checking.

## The alternative I would build instead

1. **One canonical doc: `docs/tracker-io.md`.** The finding, the two-path experiment, the
   sanitiser nuance the issue deliberately withheld, the operating rule — stated once. Add
   what the block omits and what actually bounds its lifetime: the client and version
   observed, and a re-check trigger. Root already carries `ISSUE-AMBIGUITIES-2026-07.md`,
   so in-tree process docs have precedent; `docs/` is where this belongs, not root.
2. **One line in each template**, inside the existing comment, containing no `-->`:
   `Quoting/rewriting through this tracker: read docs/tracker-io.md first.` Three lines
   total instead of ninety-six, no triplication, no MD5s, no self-inflicted comment
   termination, and consistent with the reference convention the templates already use.
3. **Turn the rule into a mechanism.** Prose cannot stop an agent from rewriting a body it
   never fully read; only a tool or a check can. Cheapest version:
   `scripts/issue-body.sh get <n>` that only ever uses the lossless path, so "read an issue
   body" has exactly one correct implementation and the rule stops needing to be known.
   Better still, extend `scientific_task.md` rule 9 — which *already* says "after any body
   edit re-fetch the issue and verify the rendered result before considering the edit
   done" — to "diff your update against the pre-edit lossless body." That kills the entire
   F5 class regardless of which read path the author used, and it costs one sentence in a
   rule that exists.
4. **Repair before you document.** #489 says three bodies were corrupted and then proposes
   no change to any of them (#404: "no change is proposed or needed"). For a programme
   whose currency is checkable citations, leaving corrupted evidence in place while adding
   a memo about how it got corrupted is backwards. The higher-value issue is: name the
   three bodies, restore them from the lossless path, record the restoration, *then* land
   the one-doc-plus-mechanism above.

## Disregarding the stated acceptance criteria — explicitly

I am setting aside all five checkboxes. Criteria 1–4 certify a file that renders 21 lines
of internal process lore into every future issue, and criterion 5 ships an unresolved
decision as a checkbox. Even after the `-->` is fixed, criteria that verify a checksum
rather than an outcome are the wrong instrument: the outcome that matters is "the next
agent reads a body losslessly before rewriting it," and no md5 of a comment can attest to
it.

## Where this sits in the project's arc

Not off-arc — the tracker is load-bearing infrastructure for a 605-issue programme, and
recording hard-won tooling facts is the same instinct that produced `docs/reproducibility.md`
and the normative specs. But the ratio deserves naming: three commits, one issue, and now
two reviews have gone into tracker meta-process, and the proposed artefact would make the
issue templates 15% longer for a fact about a third-party client. The reframing keeps all
of the value — one doc, one line per template, one mechanism, plus repair of the actual
damage — at roughly a tenth of the permanent surface, and without a defect that the issue's
own verification is blind to.
