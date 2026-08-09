# Issue #324: FEAT-032: a running circuit exchanges bytes with a human or a script through one door granted at invocation, and the exchange replays without the human
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Findings, most severe first

### 1. CRITICAL — the load-bearing security decision and the cost figures cite a document and a commit that do not exist in this repository
The entire "sealing is structural, not stylistic" argument — the one thing this issue calls "the whole security argument" — rests on maintainer decision D7, quoted verbatim and attributed to `docs/plan/evidence/BRIEF.md` §12, "landed in `3a81a4a`". Checked against the actual tree and history:

```
$ git ls-tree -r --name-only HEAD | grep -i "plan/"
(no output)
$ git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7
fatal: git cat-file: could not get object info
$ git log --all --oneline | grep -i 3a81a4a
(no output)
```

Neither `docs/plan/` (the "57 feature documents" the whole DAG-derivation section claims to be read from) nor the `evidence_commit` this issue pins every code claim to (`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`) nor the commit D7 supposedly landed in (`3a81a4a`) exist anywhere in this repository's git history. This is the same defect class already documented against sibling issue #325 (`issue-0325.adversarial.md` finding 1), so it is a corpus-wide generation artifact rather than unique to #324 — but it still means D7, the sole cited rejection of "a plugin seam for host ports," is unverifiable from the codebase a filer would actually be working in.
**Recommendation:** Before TASK-0067 is filed, either commit the evidence documents for real at a resolvable commit, or restate D7's rationale directly in this issue's own body so the sealing decision doesn't depend on a phantom file.

### 2. HIGH — two of the five ordering-graph edges point to issues since closed as duplicates, and the DAG-closure claims built on them are now false
`blocked_by: [316, 330, 354]` and `blocks: [326, 345]` are asserted with "every edge written here is mirrored on the issue it names" and an explicit acyclicity proof walking through all five. Checked live state:
- **#330 (FEAT-050)**, named in `blocked_by` and walked as "`#330 (FEAT-050) → its own predecessors #315`" in the closure computation, is **closed, `state_reason: duplicate`**, closed 2026-08-04T07:47:19Z.
- **#326 (FEAT-038)**, named in `blocks` and drawn in the mermaid graph as `F032 --> F038`, is **closed, `state_reason: duplicate`**, closed 2026-08-04T07:50:55Z.

Both closures postdate #324's only comment (2026-08-04T07:10:10Z) by roughly 40–80 minutes, and #324 was never revisited afterward. The issue's own §Sequencing text treats #330 as a live gate — "FEAT-050 before the host-port row is more than documentation… Proceeding without it means criterion 8 passes and criterion 1's *enforcement* is by sealing alone" — and §2's "even after FEAT-050 opens a discovery path" line frames it as a future live threat surface. A dead FEAT-050 doesn't break the security argument (sealing holds regardless), but it does mean §6's "genuinely blocking" claim and the printed DAG closure are stale on inspection, exactly the failure mode #325's review flagged independently for the same author/pass.
**Recommendation:** Re-run the link pass on #324: drop or retarget the #330 edge (with a REPLAN noting sealing enforcement is self-sufficient per the issue's own §6 admission), and confirm what #326 was merged into before deciding whether `blocks: [326, 345]` should read `[345]` alone.

### 3. MEDIUM — invariant 2's "copy, don't invent" claim overstates what the cited test actually enforces
§1 criterion 2 and §4 invariant 2 require that nothing on the element reaction path touch **java.io, a host handle, or an extension lookup**, and say the enforcement mechanism "already exists at `2d0ca9d` and is the pattern to copy rather than invent" — `test/jls/SocketConfinementRatchetTest.java`. Reading that file and `test/jls/ArchitectureRulesTest.java`: the shipped rules ban `java.net.Socket`/`ServerSocket`/`DatagramSocket`/channel/`SocketFactory` construction outside `jls.collab.net` (`SocketConfinementRatchetTest.java:8-24`), Java object-serialization streams anywhere under `jls.collab` (`ArchitectureRulesTest.java:202`), and reflection in the collab stack (`:277`). There is no existing ArchUnit rule anywhere in `test/` that restricts `java.io.File`/`FileInputStream`/`RandomAccessFile`-style host-file access, and none that restricts `ServiceLoader`/reflective extension lookup outside `jls.collab`. Two of invariant 2's three prohibited surfaces (`java.io`, "an extension lookup") have no shipped idiom to copy at all — TASK-0067 has to invent those rules, not assemble them from an existing pattern, which the issue's "assembly rather than invention" framing (§2) understates.
**Recommendation:** Either narrow the "pattern to copy" claim to the socket half only, or scope TASK-0067 explicitly to include authoring two new ArchUnit rules (java.io confinement, ServiceLoader confinement) rather than implying they're a copy-paste of the existing one.

### 4. MEDIUM — criterion 6 (CI refuses a golden produced under a live door) is gameable as specified
The mechanism given is: "The run mode is recorded in the artefact and the ratchet reads it." As written this is a self-reported field inside the golden text the run itself produces. Nothing in §3/§5 names an independent, out-of-band signal (a CI-side re-run, a signed build attribute, a hash tying the artefact to the invocation flags) that a hand-edited or carelessly regenerated golden can't spoof. A contributor who runs live, then hand-fixes the mode field to "null" before committing — or who runs null-port and pastes in a live-looking header for testing — satisfies the letter of "the artefact records the mode and the ratchet reads it" without satisfying the intent. This is exactly the shape of acceptance criterion the adversarial lens exists to catch: the test as described checks a field, not a fact.
**Recommendation:** Name the actual tamper-resistance mechanism (e.g., the mode line is computed by the ratchet from the same CLI/GUI invocation record CI itself captures, not merely present in a text file a human could hand-edit) before TASK-0069 is filed.

### 5. LOW — Open Question 1's "reserve now" recommendation cuts against its own stated rationale for sealing
§2 states the permit set "is the one thing that cannot be widened later without a source change," and Open Question 1 recommends reserving *both* a framebuffer and a block-device permit inside TASK-0067's sealed set now, before either payload shape is designed, on the grounds that "reserving is cheap, retrofitting a second grant model is not." But a permit reserved with the wrong shape (wrong address/handshake contract) is itself a second source change later — the issue never argues the reservation can be shape-agnostic, only that having *some* slot is better than none. That may still be the right call, but it should be argued as a real trade-off (a wrong-shaped reservation vs. no reservation) rather than asserted as costless.
**Recommendation:** State explicitly what changes if the reserved shape turns out wrong — is that a permitted "REPLAN: widen the permit," or does it trip the same "never walked back" language used for sealing itself?

## What's solid
- Every code citation checked byte-exact against HEAD: `Simulator.java:210-255` (`beforeEvent`/event loop), `Clock.java:392,421` (self-scheduling idiom), `JLSInfo.java:69` (`defaultTimeLimit`), `ArchitectureRulesTest.java:249` (`socketEndpointsAreConfinedToCollabNet`), and the zero-hit `System.in` / empty `src/jls/io/` / empty `machines/` claims all reproduce exactly as stated.
- The concurrency design (host thread offers, sim thread drains once per `beforeEvent` boundary, receive side self-schedules) is a correct, minimal reuse of an already-shipped idiom and is rightly called out as "the whole feature" — this is the strongest part of the issue.
- The polled-with-no-interrupt decision is well-justified (first consumer doesn't need an interrupt controller) and is explicitly tested as an absence rather than an omission — good acceptance-criteria discipline.
- The out-of-scope boundary against FEAT-006 (run-length ceiling), FEAT-050 (extension catalog), and FEAT-008 (GUI pane substrate) is drawn with named owners and each carries a concrete "what proceeding anyway costs" statement — genuinely useful even where (per finding 2) the FEAT-050 edge is now stale.
- The ordering edges to #316, #345, and #354 (three of the five) are correctly mirrored and both issues remain open/consistent — the machinery works when the target issue hasn't moved.

## Verdict rationale
The core engineering shape (sealed enumerated permits, ring + drain-at-beforeEvent + self-scheduled poll, polled three-address console, never-wall-clock transcript stamps) is sound and reuses real, verified seams rather than inventing new ones. But the issue cannot be filed as-is: its central security rationale depends on a phantom evidence document and commit (finding 1, shared with #325's defect), two of its five ordering edges are already stale against closed-duplicate issues with no REPLAN posted (finding 2), and one enforcement claim ("copy the existing ratchet") is inaccurate for two of the three surfaces it's meant to cover (finding 3). These are documentation/link-pass and specification-precision defects, not indictments of the underlying design — but a filer picking up TASK-0067 today would build against a citation that doesn't resolve and inherit a stale gating claim.
