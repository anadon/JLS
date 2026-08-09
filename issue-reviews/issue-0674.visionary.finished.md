# Issue #674: TASK-C350-1: a campaign is a committed, diffable file naming its jobs, their inputs and their expected artifacts — and a malformed one is refused, never repaired
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the ceremony and #674 is asking for one thing JLS has never had: **a durable,
reviewable name for a batch run.** Everything else in #350 — dispatch, collection,
aggregation, failure rows, multi-host — is defined over that name, and so is the
grading capstone (#300, "one command over a directory and 300 reports") and multi-seed
verification (#306). Cutting it first is correct, and the cut is well-placed.

It is also the *third* time this project has reached for the same concept and the first
time anyone has proposed building it. `docs/capability-roadmap/AMENDMENT.md` (Appendix,
"Reproducible simulation runs as a citable artifact") sketches "a run manifest binding
circuit hash + test-vector hash + JLS version + any seed, such that the same manifest
reproduces a byte-identical VCD and stdout report anywhere" — unsized, unanalysed, and
flagged as lost otherwise. #300's Open Question 1 independently proposes a report
artifact (xUnit XML + sidecar JSON). #674 proposes a job description. These are the same
object viewed from three angles, and the issue treats its own version as sui generis.

That is where the trajectory argument bites, and where I want to change four things.

## Reframe 1 — the campaign description is a lockfile, not a hand-authored input

The issue says "committed, diffable" and reasons from there to "authored". Look at the
audiences: a directory of 300 submissions, a parameter sweep, a multi-seed run. Nobody
hand-writes 300 records, and nobody hand-writes a 10×10×10 sweep. Either the format grows
a generator construct (cross-product, substitution — it becomes a language, and then
injectivity of artifact naming becomes genuinely hard rather than free) or it accepts
that the file is machine-produced. The issue never confronts this and the omission will
be discovered by the first real user, who will write a shell loop instead.

The resolution is already this repository's house style: `bom.json`, `SHA256SUMS`, the
per-release `.buildinfo` are all **generated, committed, and diffed**. Make the campaign
description the same kind of object — `jls -campaign-plan submissions/ > campaign.txt`
emits it, the instructor commits it, the diff review that #674 wants happens on a
generated file, and a re-plan against a late submission is a one-line diff. Every
property the issue actually argues for (reviewability, refusal of malformed input, no
runtime dependence) survives; the impossible ergonomics do not. Diffability is a
property of the *bytes*, not of who typed them.

## Reframe 2 — the job record should be the batch invocation vocabulary, not a new one

`src/jls/JLSStart.java`'s `FLAGS` table is already "the single authoritative flag list"
(ARCHITECTURE.md, "Module layout"), pinned by `test/jls/CliFlagTableTest.java`, and
`docs/batch-interface.md` §1 is already a normative, frozen specification of what one
batch run *is*: circuit file, `-t` vectors, `-s` params, `-d` limit, `-vcd`, `-i`, plus a
three-value exit contract. A job is exactly that tuple. If the job record's fields are
that tuple, three things follow that no acceptance criterion in #674 asks for but every
user will want:

1. **There is no second vocabulary to keep in sync.** #350 invariant 4 says the batch
   contract is consumed unchanged; the strongest form of "consumed unchanged" is
   "reused verbatim as the job schema".
2. **"Expected artifacts" stops being a separately-declared list and becomes derived.**
   A job that names `-vcd` is expected to produce a VCD; a job that names nothing but the
   circuit is expected to produce the §3 stdout report. Declaring the artifact list
   independently of the flags that produce it creates a second source of truth that can
   disagree with the first — a defect class the format can simply not have.
3. **One failing job out of 300 is reproducible by hand.** Paste the record's flags after
   `jls -b` and you are debugging the student's circuit, not the campaign runner. For a
   grading tool this is the single most valuable ergonomic property in the whole feature
   and it appears nowhere in #350's criteria.

## Reframe 3 — key the jobs, and acceptance criterion 3 disappears

AC-3 asks for a collision check over an artifact-naming *function* `path(j) = f(desc(j))`
with injectivity verified at read time. That framing imports a whole design problem —
what is `f`, is it a path template, who reviews the template — and path templates are
precisely the construct that invites non-injectivity (`{name}.vcd` over two students
named `smith`). The problem vanishes if the description is a **map keyed by job name**:
the key *is* the artifact prefix, and a duplicate key is a duplicate key. Then

- the collision check is the parser's ordinary duplicate-key diagnostic, not a separate
  pass, and it names both jobs with both line numbers for free — AC-3 is discharged by
  the same code path as AC-2 rather than by new machinery;
- `f` is the identity, so injectivity is not a proof obligation, it is a type.

The stronger variant worth costing: make the path a **content hash of the canonicalised
job record**. Then a collision means the two jobs *are* the same job (harmless), and the
artifact store becomes a cache — re-running a campaign after one late submission does one
simulation, not 300. That converts #350's Open Question 4 (resume vs. restart on
eviction) from a checkpointing prerequisite into a non-question. The cost is that paths
stop being human-readable, which fights the pedagogy audience; the hybrid — `name/hash`
— keeps both. Either way, do not ship an arbitrary naming function with a bolted-on
injectivity test.

## Reframe 4 — order by canonical key, not by file order (this one is a defect, not taste)

AC-1 says the job list is "ordered ... derived from the description rather than from any
runtime event", which permits *file order*. That is not enough for what #350 §3 actually
promises. If the aggregate folds in file order, then moving a line in the description —
adding a late submission, sorting the file — changes the aggregate's bytes even though
no job's result changed, and two campaign reports across that edit cannot be diffed. The
project has already made this exact mistake once and fixed it: `docs/batch-interface.md`
§3.2 records that watched-element stdout order "was `HashSet` iteration order and
therefore unstable; it is now pinned" to element-name order, and §4.2 pins VCD
declaration order the same way. The campaign aggregate is the fourth artifact class in
this project that must be byte-stable, and it should inherit the same rule verbatim:
**Unicode code point order over job keys**. Say so in this issue, not in #679's.

## Open Question 2 (serialization) — answer it from the tree, not from taste

Line-oriented UTF-8 text with a `FORMAT n` header, exactly like `docs/file-format.md`.
The reasoning is not aesthetic:

- **Zero new dependencies.** The runtime deps are `xz`, `org.jfree.svg`, `flatlaf`,
  `jspecify` (`pom.xml`). Pulling SnakeYAML or a JSON parser into the shaded jar for a
  reader adds a CycloneDX BOM row, a reproducibility surface, and a CVE feed for a file
  format with ~six fields.
- **The refusal machinery already exists and is tested.** `jls.LoadError` is a record
  with a fixed `Category` taxonomy (`MALFORMED`, `LIMIT_EXCEEDED`, `NEWER_FORMAT`, …)
  plus line, element and an actionable hint — which is *precisely* AC-2's "named, located
  diagnostic". Reuse it (add a category if needed) rather than inventing a second
  diagnostic style; ARCHITECTURE.md's "Error-reporting contracts" section is the standard
  a new reader is measured against.
- **The doc-drift guard already exists.** `FileFormatSpecTest` fails when the normative
  document and the code disagree. A campaign format spec should land with the same guard,
  or the "specified in tree" of AC-1 decays within two releases.
- **Hostile input is already the assumed case.** SECURITY.md treats `.jls` files as
  untrusted because they are "routinely shared between students and instructors"; a
  campaign file arrives by the same route and names *write* destinations. AC-2 lists no
  refusal for absolute paths or `..` escapes in artifact names, and that is the one
  malformation that matters, because the reader's output steers a writer. The 64 MiB
  container cap (`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`, issue #38) has an analogue here
  too: a job-count cap.

## Open Question 1 (who owns the vocabulary) — "whoever ships first" is the wrong rule

#350 §7 proposes a race: whichever of #350 and #300 lands first owns the job and
aggregation format. A coin flip guarantees that one of the two rewrites, and the
duplication is *already underway* — #300's Open Question 1 recommends xUnit XML plus a
sidecar JSON for counterexamples while #674 is choosing a serialization independently.
Neither should own it. **The batch contract should own it**: `docs/batch-interface.md` is
already normative, already frozen with a stated stability promise (§6), and both features
are consumers of the batch surface rather than of each other. Adding a §7 "job and
campaign description" to that document — or a sibling `docs/campaign-format.md` carrying
the same promise — resolves the ownership question permanently and by construction,
costs nothing extra in this task, and removes the integration-time discovery that §7
exists to prevent. That is the answer I would record for OQ1, in this change.

(Note this also disposes of a real hazard: the aggregate report and the per-student
verdict report are *not* the same artifact — one is a fold over jobs, the other is a
verdict about a circuit — and the ownership fight has been conflating them. The job
description is shared; the report is not.)

## I am disregarding the stated Boundary

"Format and reader only — no dispatch, no execution" cuts along the wrong seam. A format
with no consumer is validated against imagined needs; the first real consumer (#676)
will discover the missing field, and the "committed, diffable, reviewable" file will get
a format bump in its second month. The risk in this feature is not in the format, it is
in parallel dispatch — that is where byte-identity dies. So cut there instead: land
**format + reader + a serial, one-worker `jls -campaign` that runs jobs in canonical key
order and writes the aggregate**. That is a small increment over what #674 already
specifies, it makes the format falsifiable on day one, it delivers #350's own claim
("useful on one machine on the first day") in the *first* task rather than the second,
and it leaves #676 with exactly the risky part — worker count — to prove byte-identity
against a landed serial oracle. A 1-worker serial reference implementation is precisely
what integration criterion 1 needs to diff against.

## Out-of-the-box option, costed and declined

`proofs/` contains machine-checked Agda for the spatial index, bridged to the Java by
named assumptions and `ProofBridgeTest`. Injectivity of a naming function is a genuinely
provable statement and the culture exists here. Under Reframe 3 it becomes unnecessary —
identity functions need no proof — which is the better outcome. Worth noting only as
evidence that "prove it" was available and that making the property structural is
cheaper.

## What I would accept as done

The AC list as written, plus: keys, not a naming function; canonical key order stated;
path-escape and job-count refusals enumerated; diagnostics through `LoadError`; the
vocabulary recorded in the batch contract with a spec-drift test; a `-campaign-plan`
generator sketched even if not landed; and one worked example that is a directory of
submissions rather than a toy pair, because that is the campaign the two named audiences
actually have.

**Verdict: endorse-with-reframing.** The object is right, the sequencing is right, and it
should be built now. But it should be built as a generated run manifest over the existing
batch invocation vocabulary, keyed rather than templated, canonically ordered, owned by
`docs/batch-interface.md`, and landed with a serial runner attached.
