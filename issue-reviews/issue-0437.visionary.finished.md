# Issue #437: TASK-0006: a saved circuit is plain canonical text by default, and the autosave and undo containers are decided rather than inherited
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the ceremony and the claim is: **the artifact JLS produces should be the artifact
people work with.** Every downstream ambition in this repository already assumes that.
`Circuit.stateHash()` (`src/jls/Circuit.java:1548`) is SHA-256 over the *saved text*, not
over the saved *file*. #166's byte-identical saves, #165's permanent ids, #436's stable
references, #171/#163's convergence oracle, #167's operation layer, #307's KiCad round
trip, and the batch/grading surface in `docs/batch-interface.md` are all statements about
canonical text. The XZ wrapper is the one place where the thing on disk is not the thing
the project reasons about. Flipping it is with the arc, not against it. Endorsed as a
direction.

The reframing is about *how much machinery survives the flip*. As written, #437 keeps a
write-side container **choice** and then spends most of its length paying for it: five open
questions, three of which "block execution", a GUI file-filter inversion the issue's own
§11 flags as the least-instrumented change in the task, a flag name that becomes
misleading and is "recorded rather than fixed", and a completion criterion that is a
`git grep` over six prose sites. None of that is the flip. All of it is the cost of
retaining a user-facing knob for compression.

## Reframing A (recommended): the container is a property of the file you opened, not a preference the user sets

Rule: **JLS writes back the container it read; a file that has no prior container is
canonical text.** `-savetext` remains the one explicit normalizer.

What dissolves:

- **Open Question 1** ("does opening an XZ file pin the container?") stops being a question.
  Pinning *is* the rule, and it is the option the issue already recommends — it just is not
  derivable from the issue's own framing, which is why it had to be asked.
- **Open Question 5** ("add a compressed Save As filter?") disappears with its answer. There
  is nothing to select: legacy files keep their container by the rule, new files are text.
  This deletes the `Editor.saveAs` change entirely — the filter-identity comparison at
  `src/jls/edit/Editor.java:202-204` is removed rather than inverted. §11's "least-
  instrumented change here" becomes zero lines of GUI change instead of an inverted branch
  asserted at `Editor` level.
- **The `-savetext` name stops being misleading.** Under #437 as written, the flag re-saves
  as text when text is already the default — redundant. Under this rule it means "normalize
  this file's container", which is exactly what a user with a legacy XZ or zip `.jls` wants
  and cannot otherwise get. A threat the issue records as unfixable is fixed by the framing.
- **Open Question 4** ("is XZ write support retained?") gets a sharper answer than "yes":
  retained as *fidelity to an existing file*, not as a preference. The `org.tukaani.xz`
  writer stays, reachable, with no new surface.

What it costs: a user cannot newly *choose* XZ from the GUI. That cost is already paid by
the maintainer's own decision D1 (filesystem compression, user-side `xz`), and it is smaller
than it looks — because `openCircuit` sniffs content and never the name, `xz circuit.jls &&
mv circuit.jls.xz circuit.jls` produces a file JLS opens today. **External compression is
already a first-class supported workflow.** A Save As filter would be JLS re-implementing
`xz(1)` inside a file chooser.

## Reframing B (the more radical seam): push the encoding decision down to sections, not sideways to a knob

#319 (FEAT-013) owns per-section framing and the raw/optional section that bulk payloads
(Memory images, RISC-V kernel images) move into. After #319 the honest statement about a
`.jls` is not "this file is text" or "this file is compressed" — it is "each section
declares its own encoding, and the structural sections are text." That is where the format
is going, and #334 §1 out-of-scope already says so.

Read against that trajectory, adding a **file-level** compression knob to the Save As
chooser in 2026 is building the thing #319 will have to reconcile away. The seam to cut
along is: **TASK-0006 should abolish the file-level container as a user-visible decision,
leaving exactly one file-level container concept — a read-side sniff for historical files —
so that when #319 lands, encoding is decided in precisely one place.** Concretely this means
demoting `FileAbstractor.Container` from "what the user picked" to "what this file turned out
to be", and deleting `Circuit.saveContainer`'s setter from the GUI path (Reframing A already
does this). The 64 MiB `MAX_CIRCUIT_TEXT_BYTES` pressure the issue flags in §11 is then
#319's to relieve at the section level, which is exactly where the issue says it belongs.

## The autosave question is asked in a binary that the format does not have

Open Question 2 offers {XZ, PLAIN_TEXT} for `.jls~` and recommends XZ. The format has
**three** containers and the reader accepts all three (`FileAbstractor.readZip`,
`src/jls/FileAbstractor.java:295`, which already accepts the `JLSCheckpoint` entry name at
`:300` — a name that exists *because checkpoints used to be zip*). The checkpoint's stated
constraint is write volume on a hot path, on a single background executor, on every edit
(`SimpleEditor.writeCheckpointInBackground`, `:202-223`). Neither offered option serves that
constraint:

- **XZ** is LZMA2 at `new LZMA2Options()` defaults (`FileAbstractor.java:225-229`) — order
  MB/s. On a large circuit that is seconds of CPU per checkpoint, burned on a laptop
  battery, for a file that is never reviewed and never committed.
- **Plain text** is the full text written to disk on every coalesced edit — the write volume
  the question was trying to avoid.

**Deflate is the answer the constraint actually points at**, and the project has already
reached that conclusion once: `CircuitSnapshot` deflates the exact same save text in memory
for exactly the same reason (`src/jls/edit/CircuitSnapshot.java:32-33`, `:66-67`). Writing
checkpoints as a zip with a `JLSCircuit` entry needs `java.util.zip` and no new dependency,
and lands on a container the reader has accepted since JLS 4.1.

The unification is the real prize, and it collapses Open Questions 2 and 3 into one
decision. Today the circuit is serialized twice per edit into byte-identical canonical text:
once at `SimpleEditor.java:5529-5533` for the checkpoint, once at `:5631` via
`CircuitSnapshot.capture` for undo. **One capture, one deflate, two consumers — the undo
stack holds the bytes, the checkpoint writer spills the same bytes to `.jls~`.** Then "which
container does autosave use?" has a structural answer instead of a policy answer: *whatever
the undo snapshot is, because it is the same object.* That is the version of P5 worth
writing — a test that the checkpoint payload is the snapshot payload cannot be silently
retargeted by any future default, because there is no default left in the path.

(Care needed: `pushCopy` captures pre-mutation state and `markChanged` post-mutation, so the
unification is "the post-edit snapshot is both the checkpoint and the next undo entry", not a
one-line substitution. It is still one serialization per edit instead of two.)

## Assert the invariant, not the negation of the old default

§9 asserts the flip via renamed tests: `defaultWriteIsBarePlainText()`, `aFreshCircuitDefaults
ToThePlainTextContainer()`. Those pin "not XZ", which is a fact about the past. The durable
statement is available for free and is strictly stronger:

> `sha256sum <file>` == `circuit.stateHash()` for every default save.

It cannot pass under any wrapper, so it subsumes P1 and P2; it is #334's integration
criterion 2 satisfied at the task rather than deferred to close-out; and it survives #319,
because it stays true as long as the structural text is what lands on disk. Pair it with a
`FileFormatSpecTest` assertion so `docs/file-format.md` §1 cannot drift — which also
addresses §11's "comment drift" threat structurally instead of by a `git grep` in a
checklist.

## The audience benefit that does not need this issue at all

The instructor hash-oracle motivation in § Intended Audience is real and is **already
deliverable without touching the format**: `Circuit.stateHash()` exists and is content-
determined (`DeterministicSaveTest#stateHashIsContentDetermined`), but is exposed nowhere on
the CLI — `JLSStart.FLAGS` (`:760-788`) has no `-hash`. A five-line `FlagSpec` gives every
autograder a container-independent oracle today, works on the XZ files students saved last
semester, and keeps working after #319 makes "the bytes of the file" a less useful hash than
"the hash of the canonical text". That flag should be filed as its own issue rather than
ridden in on the container flip; naming it here so the flip is not credited with a benefit it
is not the shortest path to.

## Trajectory check

- **Strengthens the arc:** yes. Nothing on the roadmap wants a compressed default; several
  things (#436, #171, #167, #307, #334) want canonical text and currently work around its
  absence.
- **Duplicates:** nothing. #129 shipped the mechanism; this is the policy, correctly scoped
  as such.
- **Pulls against:** only in the one respect above — a new GUI compression knob would pull
  against #319's section-level framing. Drop the knob and the tension is gone.
- **Ordering:** the `blocked_by: [436]` edge is cheap to honour and I would not fight it,
  but note it is weaker than stated *for this repository*: #334 §5's census shows all four
  tracked `.jls` fixtures are already text, so the Θ(N) amplification is already visible here
  and #436's ratchet does not need the flip. The real exposure window is users' repositories
  between releases — which means the constraint is "ship both in one release", not "land both
  in one order".

## What I would change in the acceptance criteria

I am not disregarding them wholesale — the flip itself, the `MAX_CIRCUIT_TEXT_BYTES`
invariant, and the load-compatibility claims are right. I would strike three and add two:

- **Strike** the `Editor.saveAs` filter inversion and the "add a compressed filter" item
  (Reframing A removes the code instead of inverting it).
- **Strike** Open Questions 1, 4 and 5 as answered-by-construction.
- **Replace** the "no comment still says XZ is the default" `git grep` criterion with a
  `FileFormatSpecTest` assertion plus a single named source of truth; a grep in a checklist
  is not a mechanism.
- **Add** the `sha256(file) == stateHash()` criterion in place of the "not XZ magic" pair.
- **Add** "the checkpoint payload is the undo snapshot's payload", replacing the container
  policy for `.jls~` with a structural one.
