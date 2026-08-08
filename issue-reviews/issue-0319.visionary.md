# Issue #319: FEAT-013: a saved file stops being accepted or refused as one unit — unknown optional sections are skipped and preserved, unknown required sections are refused by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two unrelated needs are fused into one mechanism:

- **N1 — graded forward compatibility.** An older build must not silently mangle or
  drop what a newer build wrote. This is a live, *shipped* hazard, not a future one:
  `docs/file-format.md:460-466` records the `initrle` silent drop (#47) and `:474-481`
  records `Memory`'s `sync` attribute (#199) loading a synchronous-write RAM as
  level-sensitive on any reader that predates it. Both exist at HEAD.
- **N2 — bulk binary payloads escaping the text body.** `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`
  (`src/jls/FileAbstractor.java:65`) measured against decompressed text, with escaped
  init text costing α ≈ 3.73, so one 16 MiB image is 93.2 % of the cap.

The "section" is an invented construct that serves both at once. That fusion is what
makes this a 5.5 mw attractor that seven features gate on, that must be sequenced
behind a second full-container rewrite (#334), and that regenerates the golden corpus
a second time. N1 and N2 want different shapes, and the issue's own evidence says so.

## The reframe I would take instead

**Split N1 and N2 and give each the cheapest mechanism that already fits the tree.**

### (A) Must-understand as a marker on the existing grammar, not a new container layer

JLS already has three hardwired forward-compatibility policies: unknown *attribute
name* → silently ignored (`docs/file-format.md:445-451`), unknown *item kind* → hard
error, unknown *tag* → hard error. The capability the six capstones want is not
sections; it is the ability to **declare** which of those behaviours a given piece of
data gets, instead of inheriting it from its syntactic category. One lexical marker on
existing items and records — a reserved name prefix, or an `opt` item kind beside
`int`/`long`/`Int`/`String`/`ref`/`pair`/`probe` — delivers exactly that. No frame, no
epoch, no container change, no golden regeneration, and it discharges #314's OQ-1 and
the two shipped hazards above today rather than after 5.5 mw and two upstream features.
Four of the six serving capstones' payloads are attribute- or record-shaped — radix
manifest, package/footprint bindings, signal-integrity attributes, per-view geometry
rows — and are served by (A) directly.

### (B) Version by minting a new name; delete the per-section version integer

The issue never cites a format that has solved this, and several have. PNG encodes
must-understand **in the chunk name itself** (case bit 5 of byte 0: critical vs
ancillary), plus a *safe-to-copy* bit; glTF uses `extensionsRequired` /
`extensionsUsed`; Matroska and HTTP carry no global version at all. Under
name-encoded semantics there is no `v_i` and no `V(id)`: a changed section is a new
name, and the load dispatch in §3 collapses from three cases to two — the
`id ∈ K ∧ v > V(id)` arm disappears. That deletes, in one move, the per-section version
integer, the format-epoch policy, the migration machinery, and criterion **I6**.

Note the irony the issue does not: per-section versioning exists so that whole-file
version bumps become unnecessary, yet this feature's own delivery *is* a whole-file
epoch bump, and #334's two-form reader epoch is pinned to end at it (#334 OQ-1(a)).
The mechanism's first act is the thing it exists to abolish.

### (C) Bulk payloads are bundle members or sidecars, not sections in a stream

**The corpus has already decided this three times independently and the issue has not
caught up.** Maintainer ruling D15 put the guest image in a sidecar — adopting this
issue's own OQ-4 recommended default — and the #343 edge was deleted for it (comment
5171267410). #363 OQ-1 may resolve the checkpoint to "a sidecar or a directory", in
which case, by the dedup comment's own admission, "#363 stops consuming this frame
entirely." Strip those and TASK-0034's raw `IMAGE` section has one consumer left
(#364). Three payload consumers drifting the same way is the design telling you where
the seam is.

The single most under-noticed asset in the tree: **`.jls` already has a multi-member
container, and it carries exactly one member.** `FileAbstractor` reads a zip whose
entry is `JLSCircuit` (`:298`). A stored (uncompressed) zip with additional members
beside it keeps the single-file, double-clickable, autograder-hashable artifact JLS's
distribution model rests on, needs no format invention — only a writer — and gives
binary payloads a home with no escaping, no α ≈ 3.73, no cap arithmetic. Paired with
the existing XZ/plain-text duality (#129), the exploded directory is the
version-control face of the same document.

## Where the issue's own evidence points away from its design

**I1 does not deliver the property its evidence measures.** The headline datum is a
git measurement: "one word changed in a 16 MiB image produces 51,223,498 B of `git diff`
… two disjoint edits to that image always CONFLICT." Git does not diff or merge
*sections*; it diffs and merges *files*, by hunks. I1 asserts byte-independence between
sections **within one file**, leaving two peers editing different sections of one file
still colliding in one blob — always so when a section is a rewritten binary payload.
Only member/sidecar granularity makes "disjoint edits merge" true. The measurement
argues for (C), not for the frame.

**Preserve-verbatim is unsound for annotating sections, and the first consumer is one.**
This is the substantive design gap. #318's `VIEW` section is rows of
`view:instancePath:sid → (x, y, w, h, orientation)` — it *references* the structure
section by stable id. An older reader that skips-and-preserves a `VIEW` section, then
deletes or re-parents elements and saves, writes back a section whose addresses dangle:
silent corruption of exactly the class this feature exists to end. #318's criterion 4 is
worded "edit an **unrelated** element", which avoids discovering it rather than
disproving it. A binary optional/required bit cannot express this. The frame needs a
third disposition — *drop-and-diagnose on structural edit* — or a declared dependency
(`depends-on: STRUCTURE`). PNG solved this in 1996 with the safe-to-copy bit; the
issue's §3 preservation law `save ∘ open (S_i) = S_i` is stated unconditionally and is
wrong for any section that annotates something the editor may change.

## Alignment with the project's arc

ARCHITECTURE.md's recorded decisions are uniformly *do not build the general mechanism
before a concrete consumer forces it*: the plugin loader was **removed** rather than
kept speculative (#80); out-of-process isolation is "reserved for a future untrusted-
provider case; it is not built speculatively" (#222); a second simulation strategy is
declined with a named revisit trigger (#221); i18n declined (2026-07). #319 rejects
per-*record* versioning as "speculative … not a reason to build it" while building
per-*section* versioning for six capstones of which none has landed, by the same
standard. Applying the house discipline: ship (A) now — it pays for itself against
#47 and #199 at HEAD — and let the first bulk payload that actually lands choose a
sidecar, with a written revisit trigger for the frame.

## What I am explicitly disregarding, and why

**I6 (migration test) and the format-epoch policy** — under (B) there is nothing to
migrate; a name is understood or it is not, and the epoch is the most expensive
deliverable here for a hedge rather than a capability. **Per-section version integers**
(capability item 1, half of §3's dispatch) — subsumed by name minting. **OQ-2 and
OQ-3** — per-member digests and per-member compression are things a manifest states;
they stop being open questions the moment the unit is a member.

What survives and is worth building: capability items 2, 3 and 6 — declared
must-understand, refuse-by-name with the name in the diagnostic, and a round trip that
does not destroy what it did not understand. Those are the feature. Sections, epochs
and version integers are one implementation of them, and not the cheapest or most
elegant one available to this tree.

## Recommendation

Endorse the capability; re-cut the decomposition. Re-scope TASK-0033 (#444) to the
must-understand marker on the existing grammar, with a third safe-to-preserve
disposition and no epoch — weeks to days, and it unblocks #314 and de-risks #318 now.
Re-scope TASK-0034 (#445) from a raw section inside the stream to a member/sidecar with
length and digest recorded in the text body, unifying it with D15 and #363 OQ-1 rather
than competing with them. Leave TASK-0071 (#395) as is. If that holds, `blocked_by: [334]`
weakens to a preference — (A) does not rewrite the container, so the goldens are not
regenerated twice — and seven downstream features stop waiting on a container rewrite
that waits on another container rewrite.
