# Issue #493: The evidence_commit every filed issue declares (2d0ca9d) is on a branch that will be deleted — read citations at master 8288226 instead, except in seven code files and 195 planning docs
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## The measurements in the issue are sound; the premise underneath them is not

I checked the load-bearing claims against the checkout at `/home/user/JLS` and they hold.
`src/jls/hdl/HdlExporter.java` has exactly three policy buckets — `EXPORTED:422`,
`SKIPPED:431`, `TOPOLOGY:436` — and no `REJECTED` map anywhere. `src/jls/elem/SaveTags.java`
has no `FieldExtend` or `RegisterFile` row. `docs/plan/` does not exist. The line-shift
examples resolve: master `test/jls/ElementRegistryTest.java:45` is
`everyLoadableElementClassIsRegistered`, master `test/jls/hdl/HdlPolicyTest.java:88` is
`rejectionListsEveryOffenderInOneMessage`, master `src/jls/hdl/HdlExporter.java:1103` is the
`UnionFind` class. §3 is real work, carefully done, and I am not disputing a line of it.

The premise is another matter. **The branch has not been deleted.** As of today
`claude/jls-virtual-hardware-linux-njsoma` is live on origin at `742da74`, and `2d0ca9d` is
an ancestor of it — I fetched the commit through the GitHub API and it resolves, message
`Merge remote-tracking branch 'origin/master' into claude/jls-virtual-hardware-linux-njsoma`,
2026-08-02. `claude/jls-project-review-505pnf` is likewise live at `07a0bea`. Every permalink
this issue declares dying is, right now, working. Ninety-odd `claude/*` branches sit on origin
untouched; nothing about this repository's actual habits suggests these two are about to be
the exception.

And the replacement pin is already decaying by the same mechanism it warns about. The second
comment's table says `8288226` is "yes — **it is the head**". Master is now `c5cee1b`. That
claim was false within a day of being written. A prose statement about a moving ref cannot
stay true, and this issue is 200 lines of prose statements about moving refs, already carrying
two rounds of corrections in five days — the "unrecoverable" row retracted, a second dead pin
(`07a0bea`) discovered *after* the warning existed and produced *by an agent citing the
warning's own rule*, a fourth pin (`3a81a4a`) found garbage-collected outright.

That last one is the whole lesson in one line. `3a81a4a` is gone because nothing referenced
it. `2d0ca9d` is fine because something does. Reachability is the entire problem, and it is a
one-command decision — not a documentation problem.

## What this issue is really for, and why the shape is wrong for JLS

Read as a claim about what JLS should become, #493 says: *provenance for this project lives in
tracker prose, and readers pay a per-citation tax forever* (§6: for every issue you pick up,
consult a three-way table before trusting any anchor). That pulls hard against the arc every
other part of this repository takes.

ARCHITECTURE.md's method is unmistakable: a claim that must stay true gets an executable
enforcer. `HelpTopicsTest`'s link checker keeps the help tree truthful. `ExtensionPointCatalogTest`
cross-checks constants against `docs/extension-points.md` **in both directions**.
`HeadlessCoreRatchetTest` enforces the no-AWT boundary. `NotificationRatchetTest` prevents raw
`JOptionPane` call sites reappearing. `CliFlagTableTest` pins the flag table. The "Recorded
decisions" section exists precisely so that decisions that "look like accidents until written
down" are in-tree, diffable, and carry revisit triggers. README says repo documents "are the
normative home for contracts."

Against that, the second comment's remedy for the recurrence is a paragraph instructing future
agents to run `git merge-base --is-ancestor` by hand and to remember that `--contains` is not a
substitute. That is a lint rule written as a plea. It has already failed once — its own §5
warning did not stop `07a0bea`.

Worse is the rescue pattern the first comment records: `docs/parity-contract.md` (940 lines),
`docs/machine-calibration.md` (1,124), `docs/virtual-hardware-parity.md` (2,094) transcribed
into issues #494–#499 with hand-built "concordances" mapping line numbers to sections. That is
using the tracker as a filesystem — no diff, no review, no CI, no line anchors, and a
*documented lossy read path* (#489: reading a body corrupts tag-shaped runs, and writing it
back persists the corruption). The comment even records the absurdity honestly: #423's
completion criteria require `docs/parity-contract.md` "is ratified", and whoever executes #423
must first copy the text out of an issue body back into the tree. The document was already in
git. It is still in git, on `njsoma`.

## The redirect: three operations that delete the problem instead of narrating it

**1. Preserve the objects. `git tag evidence/2026-08-sweep 2d0ca9d && git push origin
evidence/2026-08-sweep`** (and the same for `07a0bea`), or push `refs/evidence/*` refs if a
tag in the release namespace is unwelcome. Cost: two commands. What it buys: every `2d0ca9d`
permalink in 198 issues keeps working permanently, the 195 "unrecoverable" planning documents
become readable again, §3's seven-file table stops being a trap and becomes a footnote, and
the three rescue-transcription issues become unnecessary. Compare against the cost actually
paid so far — a 200-line erratum, 43 individual per-issue comments, 10 more on the second
round, six rescue issues carrying 4,158 transcribed lines, and a recorded obligation on #423.
This should have been the first move on 2026-08-03 and it is still available today.

**2. Land the planning corpus, don't transcribe it.** One PR putting `docs/parity-contract.md`,
`docs/machine-calibration.md`, `docs/virtual-hardware-parity.md` — and whichever of
`docs/plan/**` is actually normative — onto master. Line-level pinning returns for free, the
text becomes reviewable and diffable, #423's ratification criterion becomes executable, and the
concordances evaporate. If 34,612 lines of planning is more than master should absorb, that is
a signal worth stating plainly rather than routing around: **plan artifacts are being produced
faster than the tree can absorb them**, and issue bodies are being used as the overflow store.
605 open issues and 890 review files under `issue-reviews/` say the same thing. The right
response to that is pruning, not a second storage tier with worse durability properties than
the first.

**3. Change what a citation *is*.** `.github/ISSUE_TEMPLATE/scientific_task.md:151` defines
`evidence_commit` as "SHA all §2 citations are pinned to", and §3 of this issue is a
three-page demonstration that `path:NNN@sha` is perishable in three independent ways: the ref
dies, the line moves, the file never existed. The durable anchor already exists everywhere else
in this project — a *name*. `HdlExporter.classifiedElementClasses()` survives every refactor
that `HdlExporter.java:460` does not; so do `SaveTags.resolve`, a `SaveTags` row by tag text, an
`ExtensionPoint` constant, a `LoadError` category, a test method name. Amend the three templates
to require symbol + quoted text, with `path:line` demoted to an optional convenience. Then a
line shift is not an incident, and #493 becomes a one-time migration note rather than a permanent
reading protocol.

**4. (The ratchet, if the tracker stays load-bearing.)** The check the second comment writes as
instructions is three lines of shell. Make it a CI lane: for every open issue, resolve
`evidence_commit` and fail if it is not an ancestor of `origin/master` *or* a preserved
`refs/evidence/*`; resolve each `path:line` and fail if the path is gone; where a quotation is
present, grep for it. That is `HelpTopicsTest`'s link checker pointed at the tracker instead of
`resources/help/**` — the same idea this project already trusts, in the one place it currently
doesn't. It would have caught `07a0bea` the hour it was written.

## What I am explicitly disregarding

§6's operating procedure and the "durable replacement pin" framing. Not because the content is
wrong — I verified it — but because it institutionalizes the loss as permanent when the objects
are still on origin, and because a document that needed two substantive corrections in five days
is not durable by any reading of the word. §5's per-issue classification and §3's branch-only
line table should survive as a **migration table with an expiry date**: the day the tag is
pushed and the docs land, most of both is moot. What should not survive is the expectation that
every future reader of 198 issues consults a prose table before believing a citation.

The narrow instruction I fully endorse and would keep verbatim: **do not bulk-rewrite issue
bodies** (#489). A storage medium whose read path is known to corrupt data is one more argument
for moving provenance out of it entirely — which is what recommendations 1–3 do.

## Where this strengthens the project

§4 is the best part of the issue and points the right way already: the branch-only code is
correctly reclassified as *proposed work* owned by #488 / #491 / #492. That is the healthy
seam — code that isn't on master is a proposal, and a proposal belongs in an issue. Documents
and evidence are the opposite: they belong in the tree, under review, with a ref that keeps them
reachable. #493 currently has those two categories swapped.
