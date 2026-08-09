# Issue #405: TASK-0010: a waveform dump is written as it is produced, and its cost is proportional to the changes it contains
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The technical core is unusually well grounded: every file:line citation checked against
HEAD (`src/jls/sim/BatchSimulator.java` lines 359, 384, 401-411, 455, 459, 472, 509, 538)
matches exactly, the named golden tests
(`VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte`,
`.testVectorStimulusVcdMatchesGoldenAndCoversHiZ`, `VcdProbeExportTest.probedNetAppearsInVcd`)
exist with those exact names, and the algorithmic claim (O(|T|×|S|) lookups for O(|C|)
output) is a correct reading of the current loop at lines 455-468. The scoping discipline
(explicitly not touching `docs/batch-interface.md` §4's VCD grammar, not conflating with
#232's value-representation work) is genuinely careful. That said, several structural
problems mean this issue is not ready to execute as filed.

## Findings, most severe first

**1. [HIGH] Not actually executable yet — `blocked_by: [373]`, and #373 is open with its own unresolved chain.**
`blocked_by: [373]` is correct per #353 §6 ("TASK-0008 before TASK-0010 is necessity, not
convention") and I verified #373 is open, unmerged. But #373 itself names two more
prerequisites, TASK-0005 and TASK-0007, that "do not exist yet" as filed issues ("Two
sibling tasks are being filed concurrently and their numbers do not exist yet") and lists
five of its own "Blocks execution" open questions. So #405's actual start date depends on
a task that depends on issues not yet filed. The issue's own Definition of Done ("`blocked_by`
#373 has landed, or the dependency was waived per rule 10") acknowledges the risk but does
not name a fallback owner or timeline if #373 stalls — a single-maintainer project (per
README/ARCHITECTURE) has no stated escalation path here.
**Recommendation:** either land #373 first (as designed) or, if this issue is picked up
now, require the waiver decision be made explicit in the very first PR comment rather than
left implicit until close-out.

**2. [HIGH] The stated motivation is not delivered under the issue's own recommended default — impact/scope mismatch.**
"Intended Audience & Impact" leads with: *"A grading run that dumps a waveform currently
needs peak heap proportional to the whole dump, three times over... Long runs simply
cannot produce a dump they cannot hold."* That reads as a promise to fix unbounded memory
growth for long runs. But O7 explicitly identifies the real unbounded structure —
`eventTrace`/`probeTrace`, which retain every `TraceSample` for the whole run — and Open
Question 1's **recommended default is (b): don't fix it, just document that peak memory is
proportional to total recorded changes**, deferring the actual bound to unfiled/uncosted
FEAT-006 (#354). Under that default, this task removes three *transient* full-dump copies
(a real, worthwhile win) but does **not** make "long runs that cannot produce a dump they
cannot hold" possible — the retained-sample structure that made them impossible in the
first place is untouched and now explicitly documented as still unbounded. The audience
section and the Open Question 1 default are in tension; a reader of only the abstract/
impact section would reasonably expect the long-run memory problem to be solved here.
**Recommendation:** soften the Intended Audience claim to scope it to the *rendering* path
specifically (which is what P1/H3 actually test), or fold the O7 bound into this task's
scope instead of deferring it silently to a feature (#354) that has no estimate of its own
in the fetched text.

**3. [MED] The evidence commit does not exist in this repository's git history.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` (and the short form `2d0ca9d`)
resolves to nothing: `git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails with
"could not get object info", and `git log --all --format='%H' | grep 2d0ca9d` returns
nothing across all 276 commits in this checkout. Yet every line-number citation in O1-O9
happens to match current HEAD exactly, so the content is verifiably accurate against *this*
tree — but not against the commit the issue claims to be pinned to, and not verifiable the
way the issue itself insists ("Re-verify O1–O9 at the executor's checkout; re-derive line
numbers if HEAD has moved," Method step 1; "Threats to Validity" §11 also flags line-number
drift as a risk but assumes the commit itself is checkable). An executor cannot run the
literal `git diff --stat 2d0ca9d HEAD` command the issue's own §2 preamble presents as
already having been run ("`git diff --stat 2d0ca9d HEAD -- src/ test/ pom.xml` produces no
output") — that command fails outright in this checkout. Since the same hash appears
verbatim in sibling issues #373 and #353, this looks like a corpus-wide fixture/tooling
artifact rather than a one-off typo, but it still breaks this issue's own verifiability
contract.
**Recommendation:** replace `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` with a resolvable
commit (or a tag) before execution starts, and re-verify O1-O9 were in fact re-derived
against a real commit, not copy-pasted from a template.

**4. [MED] Most of the exhaustive evidence and line-pinning will be invalidated by the very prerequisite this issue waits on, before this issue can execute.**
#373 (the blocking prerequisite) modifies `BatchSimulator.java:401-435` — the exact
header/`$var`-declaration fold region O6 says "cannot stream first-pass" and that this
issue's plan touches directly ("Introduce `writeVcd(Appendable)` emitting the header
(unchanged fold, unchanged `_probe` loop, unchanged `$var` order)..."). #373 adds an
escape-or-reject validation step at `BatchSimulator.java:426-435` for probe names. So by
design, the code this issue's O1-O9, the mathematical formalization in §7.10, and every
`#L4xx` anchor describe will have already changed in that region by the time #405 is
actually picked up — a large fraction of the issue's very detailed evidence work (the
formal $g_{2d0ca9d}$/$g_{\text{after}}$ derivation, the specific append-order argument) is
effectively disposable and must be re-derived, which the estimate (4 d, from #353's task
table) does not appear to price in beyond a generic "re-derive line numbers if HEAD has
moved."
**Recommendation:** either re-verify O1-O9 against #373's post-landing tree before
starting implementation (make this an explicit, budgeted step, not an aside), or note
explicitly that the 4-day estimate assumes zero material rework from #373's landing and is
therefore optimistic.

**5. [MED] P1's "per-line constant K" and P2's "ratio bounded by a constant" are never given numeric values — gameable acceptance criteria.**
H3 states peak transient allocation must be "independent of run length: the largest single
`append` handed to the sink is bounded by a per-line constant," and §7.10 restates this as
"$\max_i |a_i| \le K$ for... a per-line constant $K$." Nowhere in the issue — not in §5
Predictions, not in §9 Data Collection, not in the Method checklist — is $K$ given an
actual number or even a formula (e.g., "max line length across all vcdValue outputs plus a
small header margin"). Similarly P2's ratio bound ("inner-loop work / emitted lines...
bounded by a constant... not as a wall-clock threshold") never states what constant.
Whoever writes `VcdStreamingTest.java` therefore picks both thresholds themselves with no
issue-level guidance, which is workable for a correctly-implemented streaming writer (the
natural bound is small) but is a real gap in a document that is otherwise fastidious about
naming exact numbers everywhere else (86.9%/93.1%/96.0%/97.5%, 1,112,009 characters,
1.53/2.16/3.06/4.34). A sloppy implementation with, say, a per-timestamp `StringBuilder`
buffering an entire timestamp's worth of value lines would still pass a generously chosen
$K$ and a generously chosen ratio bound, while still not being "streamed" in the spirit H3
intends.
**Recommendation:** name $K$ concretely (e.g., "no single append exceeds 4× the longest
possible single value line for this signal set") so the test can't be satisfied by picking
a convenient threshold after the fact.

**6. [LOW] §7.3's "no hostile-input surface here... all trusted" claim is in tension with the very issue this task is blocked on.**
§7.3 states: "`eventTrace`... and `probeTrace`... — all internal, all trusted (produced by
this class's own `sample`/`probeSample` hooks). No user-supplied bytes enter this path."
But `probeTrace`'s *keys* are probe names, and #373 O5-O7 (which this issue cites and is
blocked on) establishes that probe names are user/peer-supplied strings reachable from the
GUI, from collaborative ops, and from loaded files, with **no validation today**, and that
they flow unescaped into exactly the `$var` line this issue's writer renders
(`BatchSimulator.java:426-435` again). Calling this "no hostile-input surface" is technically
defensible only under a narrow reading of "upstream of this method call," but it undersells
that the data this method renders is not fully trusted content, and a reader relying on
§7.3 at face value could reasonably (and wrongly) conclude probe-name hygiene is out of
scope for streaming-writer correctness (e.g., "does a malicious/malformed probe name that
requires escaping break sink-based writing the same way it broke the old builder?").
**Recommendation:** narrow the claim to "no *new* hostile-input surface is introduced by
this task" and cross-reference #373 explicitly here rather than asserting trust outright.

**7. [LOW] O4's headline figures aren't reproducible from the artifacts the issue actually includes.**
O4 reports "128 signals, time limit 200,000" producing a 1,112,009-character dump, but the
full driver given in O3 only sweeps `n ∈ {8, 16, 32, 64}` at time limit 20,000. The program
that produced O4's numbers is referred to only as `T10d` and is not included in the issue
body, so O4 cannot be independently reproduced from what's written here (unlike O2/O3,
which are fully self-contained). Minor, since O4's qualitative point ("one allocation holds
the whole dump") is already established by O1's code reading, but it's a real gap in an
issue that otherwise treats "reproducible from the text as given" as a hard bar.
**Recommendation:** either include the O4 driver or note explicitly that it's a trivial
extension of O3 left as an exercise.

## What's solid (no action needed)

- The algorithmic defect (O2) and the triple-materialization defect (O1) are both real and
  correctly cited against current HEAD.
- The byte-identity safety argument (P3, tied to concrete named golden tests) is the right
  backstop for a rewrite whose only acceptable outcome is "invisible to users."
- "Deletion, not deprecation" for `toVcd()` (§7.4) is a good discipline call, and P4's
  `grep` check is a genuinely hard-to-game acceptance criterion (unlike findings 5 above).
- The temp-and-rename decision for `writeVcd()` (§7.11, Open Question 2) correctly
  identifies a real new failure mode (partial file on disk) introduced by streaming that
  didn't exist with atomic `Files.write`, and addresses it rather than ignoring it.
- Explicit non-goals (§13: not touching #232's value representation, not touching #72's
  VCD grammar, not touching #376's parse/load work) are drawn at defensible, verified-disjoint
  boundaries.
