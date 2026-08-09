# Issue #191: Deterministic macOS installer: reproducible (or bounded-residual) dmg
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is an unusually well-evidenced issue: every file:line permalink cited
against `29afb26` was checked against the current tree and matches (e.g.
`build-installer.sh#L407-L410`, `#L420-L466`, `normalize-dmg.py`'s
`_gpt_partitions`/`_apm_partitions`/`_locate_hfs`, the "Status at commit
time" staleness at `docs/dmg-reproducibility.md#L53-L56`). The technical
narrative — koly `SegmentID` outside UDIF checksums, HFS+ header at
`partition_start + 1024` behind a GPT/APM wrapper, Route-A round-trip
corrupting the image per CI run 29773635573 — is internally consistent and
verifiable from the repo, not asserted from memory. The remaining findings
are about what happens *after* this groundwork, where the acceptance
criteria loosen.

## Findings, most severe first

**1. (High) The bounded-residual close-out can be satisfied without ever measuring the residual it documents.**
Route A, as it exists on master, corrupts the image (`docs/dmg-reproducibility.md`
§2 Obs. 4: "full Route-A HFS+ round-trip — verify FAIL"). The doc's own
route-decision rule (§4, "Decision rule: run the measurement; if Route A's
residual is only F4 (± F6), prefer Route A + H2(b)") can only be *evaluated*
by running a working, non-corrupting Route A — but §4 also only offers an
*"Expected end state ... If measurement confirms that"* (i.e. a prediction,
not a result) for what Route A's residual would be. Method step 2 in the
issue lets the diagnosis be abandoned once it "exceeds the bound" and fall
through to H2(c) bounded-residual — but "that bound" is never defined (no
time-box, no attempt count, no LOC ceiling; Open Question 2 only says
"Recommended default: bound the Route-A corruption diagnosis... switch to
Route B if the diagnosis exceeds that bound"). Nothing stops an implementer
from trying Route A once, declaring the bound exceeded, skipping Route B
too, and writing the *predicted* F1–F6 set into `docs/reproducibility.md`
§5 as if it were measured. That satisfies DoD's "bounded residual
enumerated... no false reproducibility claim" on its face while directly
violating the Research Question's demand for "the exact, minimal residual
byte set" — a claim about *bytes measured*, not bytes hypothesized.
**Recommendation:** require the DoD's residual statement to cite a specific
CI run / `measure-dmg-repro.sh` artifact bundle for the *actual* enumerated
residual (not the doc's predicted F4-only set), and put a concrete cap
(e.g. "at most two diagnostic CI dispatches") on the Route-A effort before
H2(c) is allowed, so the escape valve can't be taken on iteration zero.

**2. (High) The critical path needs real macOS hardware; the issue budgets none of that cost for the agent meant to execute it.**
The issue's own Intended Audience names "LLM agents" as a consumer. But
diagnosing the Route-A corruption is inherently iterative (attach → patch →
convert → `hdiutil verify`, repeat), and the only place `hdiutil` exists is
`macos-latest` in CI (`docs/dmg-reproducibility.md`, line 6: "requires a
macOS machine — `hdiutil` exists nowhere else"). The probe workflow is
`workflow_dispatch` + a monthly cron and is `continue-on-error: true`
(`.github/workflows/repro-installers.yml` L26-27, L40) — each diagnostic
iteration is a full manual dispatch-and-wait cycle, not a local
edit-compile-test loop. Open Question 3 goes further and admits the P4
clean-VM/Gatekeeper check "needs a human with macOS hardware" outright.
No budget, iteration cap, or fallback is given for an agent that has no
such access. **Recommendation:** state explicitly in the issue what an
agent without interactive macOS access can and cannot close out (e.g. "P2
attribution and doc refresh are agent-completable via workflow_dispatch;
Route-A/B diagnosis and P4 require a human/macOS session and should be
handed off, not silently skipped").

**3. (Medium) Completion Criteria and Open Questions disagree about whether the P4 human check blocks closing #191.**
DoD lists as a checkbox: "Whatever ships passes... P4 (mount, launch,
`.jls` association, Gatekeeper right-click-Open on a clean VM/hardware,
human-verified where CI cannot)." Open Questions & Decisions Needed #3
says the same check "rides along until the shipping step" and is explicitly
scoped as *not* blocking ("Does not block execution... resolve when
recording results" is said of Q1; Q3 has no such disclaimer but is phrased
as deferrable custody, not a gate). As written, it's ambiguous whether an
implementer can tick the DoD box by reasoning "the human check happens at
release time, not in this issue" — i.e. close #191 having never actually
run P4. **Recommendation:** state plainly whether P4 is in-scope-and-blocking
for *this issue's* closure or is explicitly deferred to the release
pipeline (in which case remove it from this issue's Completion Criteria,
or mark it "tracked here, verified at ship time, not a precondition for
closing #191").

**4. (Medium) "Run P2" may already be satisfiable by the existing three-way-verify run, undermining the byte-attribution requirement.**
§2 Obs. 4 already reports, from CI run 29773635573, "two builds of one
commit do **not** hash equal at this commit" — worded almost exactly like a
P2 verdict. Yet §8 Method still lists "[ ] Run P2: dispatch the macOS probe
leg... and record the residual attribution" as outstanding, and DoD item 1
requires "P2 recorded... probe run + attribution." The three-way verify in
PR #196 was a corruption localization check (raw / koly-only / full
round-trip), not `measure-dmg-repro.sh`'s byte-range attribution bundle
(`koly.diff`, `raw-diff-ranges.txt`, etc. — `measure-dmg-repro.sh` L11-18).
An implementer could point at Obs. 4 as "P2, already done" and never run
the actual attribution harness, checking the DoD box without producing the
byte-range evidence the Research Question needs. **Recommendation:**
explicitly name which artifact (three-way verify vs. attribution bundle)
counts as "P2 recorded" in the DoD line itself.

**5. (Low-Medium) Route B is a substantial, risk-bearing rewrite of a currently-working, unsigned installer's only shipping path, for a cosmetic payoff.**
Per the doc's own estimate, Route B means reimplementing jpackage's dmg
Finder-phase behavior by hand ("`build-installer.sh` takes over ~40 lines
of jpackage behavior") including re-creating the `.jls` file-association
plist post-hoc (jpackage's `--type app-image` doesn't accept
`--file-associations` at all) and the license-sheet/background-layout
scripting jpackage currently gets for free. The value being purchased is
byte-identical volume UUID/dates on an installer that: is already unsigned
by policy (#128/#135), already discloses non-reproducibility as "expected"
in README.md ("the installers are *not* byte-reproducible... that is
expected"), and already has its integrity carried by a provenance
attestation per the issue's own framing. A hand-rolled imaging pipeline
that gets the Finder-phase scripting subtly wrong risks shipping a broken
or unlaunchable macOS build — the one artifact this project cannot easily
get human eyes on continuously. **Recommendation:** treat Route B as
optional/stretch given H2(c) is an explicitly sanctioned honest fallback,
and require the same three-way-verify + P4 checklist to gate Route B
merging as already gates Route A.

**6. (Low) Falsification Criterion 1 has no defined resolution path.**
"If the P2 diff shows differences outside the F1–F6 inventory... H1-refined
is wrong — investigate the jlink/jpackage staging independently... before
any imaging work" — open-ended "investigate," with no bound on scope, no
statement of whether that becomes a new issue or balloons #191's own scope.
Minor given the issue is `tier: task`, but worth a one-line disposition
rule (e.g. "file a follow-up issue, do not expand #191's Method").

## What's solid

- Every cited permalink and code claim checked out against the live repo
  (build-installer.sh clamp plumbing, dmg lane, normalize-dmg.py partition
  locator; docs/reproducibility.md §5's msi/dmg carve-out text matches).
- The safety invariant ("never patch UDZO data-fork bytes after convert")
  is stated once and enforced consistently everywhere it's referenced.
- Escape hatches (`JLS_SKIP_DMG_NORMALIZE`, `continue-on-error` CI,
  self-test-before-touching-a-real-dmg) are genuinely low-risk and already
  landed, so the disabled/broken Route A cannot regress a release today.
- Scope is explicitly fenced against payload/content drift (§7.12,
  §"Related Work" against #188/#190/#185) and the "no false reproducibility
  claim" honesty framing (#184 H3) is a good, testable north star even if
  the *evidence* backing that claim (Finding 1) needs tightening.

## Bottom line

The issue is technically well-grounded and its citations are accurate, but
its exit criteria have enough slack — an unquantified effort bound before
falling back to H2(c), an unresolved tension over whether the human P4
check gates closure, and ambiguity over what counts as "P2 recorded" — that
it could be closed with a documented-but-unmeasured residual claim and no
actual improvement in dmg determinism. Tighten the DoD wording per Findings
1, 3, and 4 before execution starts.
