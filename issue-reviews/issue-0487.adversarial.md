# Issue #487: FEAT-060: electrical intent leaves JLS as a rule file an external DRC enforces — a board routed 25% over its declared maximum length fails, and the shortened one passes
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

Open issue, well-formed as a machine-readable roadmap entry (YAML block, mirrored
`blocked_by`/`blocks` edges, mermaid graph). I cross-checked the graph against #486,
#490 and #319 by fetching them directly: every edge #487 claims is mirrored exactly on
the far side. That bookkeeping is solid. The problems are in the evidentiary basis the
cost/scope claims rest on, in one load-bearing claim that is simply false as stated, and
in a gap in the one acceptance criterion the issue calls out as its strongest guarantee.

## Findings, most severe first

### 1. "Already funded" harness claim is false as stated — it's another unbuilt issue's future test

Evidence §6 says: *"The acceptance harness this rides is already funded. CAP-05 (#298)
invokes the external checker with `--severity-error --exit-code-violations` in its own
acceptance test."* I fetched #298: it is itself an **open, unimplemented capstone**, and
the `kicad-cli pcb drc --severity-error --exit-code-violations` invocation is step 3 of
its own prospective acceptance walkthrough ("Place and route the board in KiCad... Upload
it for roughly $30. Three weeks later a board arrives.") — not existing CI
infrastructure. A repo-wide grep for `severity-error`, `exit-code-violations`, and
`kicad-cli` returns zero hits anywhere in `src/`, `test/`, or `.github/workflows/`.
Calling this "already funded" implies reusable, shipped plumbing; in fact it is a second
from-scratch dependency on a sibling open issue that #487 does not even list in
`blocked_by`. This directly inflates the confidence behind the 5.5-9.5 mw cost band.
**Recommendation:** reword Evidence §6 to state the harness is *planned* by #298, not
funded, and either add #298 to `blocked_by` or explain why the two DRC invocations can be
built independently.

### 2. Cited planning-corpus evidence is dead on the default branch

Evidence §8 cites `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md` §7.1 and
`docs/plan/features/FEAT-060-si-constraint-authorship-and-pcb-constraint-export.md` as the
source for scope and the 5.5-9.5 mw cost band. I verified: `docs/plan/` does not exist
anywhere in the current tree. `git log` shows the entire corpus was deleted in commit
`742da74` — *"docs: remove the planning corpus now that it is encoded in issues"* — at
2026-08-03 21:11:01Z, which is **after** this issue was filed (18:57:45Z the same day)
and **before** its own follow-up comment (07:57:36Z the next day). The issue's own
Completion Criteria requires *"every cited evidence document and permalink resolves on
the default branch at close."* These two do not resolve today, and the removal commit's
own message ("now encoded in issues") implies the issue text itself should be the
citation, but the issue was never edited to say so. Anyone re-deriving the cost
reconciliation today has no document to check it against.
**Recommendation:** either restore the two files or strike the dead paths and replace
them with in-issue citations; do it before the Completion Criteria checkbox is trusted.

### 3. The external tool is never named, though its identity leaks through implementation details

The issue calls it only "the target board tool" / "an external tool JLS does not
control," yet Evidence §5 quotes verbatim internal field/struct names (`m_TrackWidth`,
`m_diffPairWidth`, `DRC_CONSTRAINT_T`, `NETCLASS`) and the worked CLI example emits a
`.kicad_dru` file — KiCad's real rule-file extension. Sibling issue #298 names it outright
(`kicad-cli`). Refusing to name the dependency in #487 while relying on its private
implementation details for the vocabulary decision (Open Question 1) is not a real
abstraction — it just forces whoever picks up the work to reverse-engineer which tool,
which version, and which container to pin. No image name, registry, or version-bump owner
is given for "container pinned by digest" (§ Re-planning Protocol acknowledges the tool's
vocabulary can change but assigns no owner to watch for it).
**Recommendation:** name the tool and a concrete pinned image/tag in the issue body.

### 4. Fully blocked, and every listed blocker is itself unstarted

`blocked_by: [486, 336, 366, 318, 319]`. I fetched #486, #319 and #490 directly: all
three are open, and every one of #486's and #319's `planned_tasks` rows is marked "Not
filed" — none of the underlying code exists. Independently confirmed in-repo: no
`kicad`/`geda` package (`find . -iname "*kicad*" -o -iname "*geda*"` matches nothing under
`src/`), no `jls.netlist` package, `WireNet.java` has no length field, and
`Element.java:21` still documents only the file-local reassigned `id` with no stable net
identity (`stableId` exists only per-element, per `:24`). This is fine for a roadmap
placeholder, but the issue carries a full engineering apparatus (Global Invariants,
Integration Criteria, a REPLAN protocol) written as if ready to execute, when in fact zero
of its five hard prerequisites have landed and at least two of them (#486, #319) have not
even had their own child tasks filed yet.
**Recommendation:** no action needed beyond keeping this honest in status — but a reviewer
should not read the elaborate machinery here as evidence of readiness.

### 5. The one "un-gameable" criterion has a silent-skip escape hatch

The issue's marquee claim is that criterion 1 is *"the only criterion in CAP-18
verifiable outside JLS"* and that this is what makes it a real adjudicator rather than a
lint. But its own execution path is described as *"opt-in through the shipped
tool-locator plus assumption idiom"* — i.e., a JUnit `assumeTrue`/skip when the pinned
container isn't found. If a CI runner can't pull the image (registry hiccup, air-gapped
build, rotated digest), the test is **skipped**, not failed, and `mvn verify` still goes
green (Global Invariant 7). The Re-planning Protocol addresses "the external DRC cannot be
made to honour a rule file" (K18-2) as a named failure mode, but never addresses "the
check silently stops running" as a distinct, far more mundane one. The only guard offered
is a manual closing-comment obligation ("the container digest, the command and the exit
status in the closing comment") — a one-time human attestation, not a standing CI
invariant that a skipped run must fail the build. This is exactly the kind of gap an
adversarial reviewer should flag: the test that is supposed to be hardest to game is
gated by the easiest mechanism to accidentally disable.
**Recommendation:** add an explicit invariant that a skipped/assumption-failed DRC test
fails CI (or is treated as a release blocker), not merely a silent pass.

### 6. "Stackup" is used in the math but never captured anywhere in the data contract

The impedance-annotation transformation states the resolved track width comes from `w =
f(Z_0, stackup)`, but "stackup" (dielectric constant, copper weight, layer count) appears
nowhere in §3's Consumes/Provides list, nor as an attribute in any of the four planned
child scopes. Either JLS captures stackup somewhere undocumented, or the synthesis
function has an input with no declared source — as written it's a parameter smuggled into
the spec without an owner.
**Recommendation:** name where stackup comes from (a new authored attribute? a fixed
default? out of scope for v1 and w is left unresolved?) before the emitter's golden is
frozen, per the issue's own Open Question 2 caveat.

## What's solid (no action needed)

- The `blocked_by`/`blocks` graph is mirrored correctly on every hop I checked (#486,
  #490, #319) — the bookkeeping discipline here is unusually good for a hand-authored
  roadmap.
- The silent-drop rationale (§ Evidence 2, `docs/file-format.md:220-222`) is accurately
  quoted — I verified the file content word-for-word — and the vocabulary-vs-attribute
  design choice it motivates is sound.
- The failing-direction-first acceptance design (fail on the over-length board before
  counting the pass on the shortened one) is a genuinely good falsification guard,
  independent of finding #5 above about how it can be skipped.
- Scope boundary is sharply drawn: no netlist ownership, no transmission-line modeling, no
  impedance constraint, no per-section-versioning mechanism of its own — each explicitly
  ceded to a named sibling issue rather than re-implemented.
