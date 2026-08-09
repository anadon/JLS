# Issue #854: TASK-C579-4: the store page is a shop window — description and three screenshots from the shared set — and the per-release review cost is written down against the kill threshold
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C579-4 is the "dress the Flathub listing" task in the chain
CAP-34 (#518) → FEAT-C34-1 (#579) → TASK-C579-3 (#853, submit to
Flathub) → TASK-C579-4 (#854, description + screenshots + cost
accounting). Read in isolation the five ACs are clean and mostly
checkable. Read against the rest of the repository, the task sits
downstream of two unresolved problems it does not itself raise: the
project's own standards-adoption analysis explicitly recommends
against pursuing Flathub at all, and the screenshot asset AC-1/AC-3
depend on has no agreed producer as of today.

## Findings, most severe first

**1. (Critical) The whole Flathub effort contradicts the project's own recorded analysis, and nothing in this issue or its ancestors acknowledges it.**
`docs/standards-adoption/10-desktop-and-housekeeping.md` is an
in-tree, apparently-authoritative playbook entry for exactly this
topic. Its verdict table reads: "AppStream metainfo (#175) | Do it in
the reduced form: one metainfo file, shipped in deb/rpm/AppImage,
validated in CI. **Do not pursue Flathub.**" The rationale section
("Flathub: recommend no") gives four concrete reasons: it needs a
second packaging pipeline with none of the reproducibility plumbing
CI currently gates on; the sandbox is "hostile to the tool's actual
job" — JLS writes `<circuit>.jls~` next to the user's file
(`src/jls/edit/Editor.java:103`, `src/jls/edit/SimpleEditor.java:5388`)
and is driven from shells/autograders in batch mode, which under
Flatpak means `--filesystem=home` "which reviewers push back on" and a
`flatpak run` prefix that "breaks every command line in `README.md`
and `docs/batch-interface.md`"; it "contradicts the recorded
deployment model: single self-contained jar plus per-OS installers, no
install step assumed, no network"; and screenshots must be
HTTPS-reachable at Flathub build time, which the project (no hosted
site) cannot cheaply satisfy. The document goes on to recommend
recording the decline as an `ARCHITECTURE.md` "Recorded decisions"
entry titled "Flathub: not pursued." That entry does not exist —
`ARCHITECTURE.md`'s "Recorded decisions" section (read in full for
this review) covers i18n, help delivery, look-and-feel, the plugin
mechanism and boundary, extension points, and simulation strategy, but
never Flathub. None of #518 (CAP-34), #579 (FEAT-C34-1), #853
(TASK-C579-3), or #854 cites, disputes, or overturns the
standards-adoption verdict; the capstone/feature/task chain simply
proceeds as though the decline never happened.
**Recommendation:** before doing #854's polish work, resolve this at
the capstone level: either write the reversal explicitly (a new
`ARCHITECTURE.md` decision superseding the standards-adoption playbook
entry, with the sandbox/`.jls~`/deployment-model objections answered)
or shelve the TASK-C579-* chain. Shipping screenshots for a channel
the project's own document says not to pursue is effort spent on a
question that was already answered "no" once, silently.

**2. (High) AC-1 and AC-3 depend on a producer artifact that is explicitly undecided, and #854 does not declare the dependency.**
Both AC-1 ("sourced from the CAP-27 (#511) set") and AC-3
("regenerable from the shared capture pipeline") assume a settled,
existing screenshot pipeline owned by #511. A same-day-as-this-review
comment on #511 (2026-08-08, the newest comment on that issue) says
the opposite: "there are three plausible homes for one artifact" — a
hand-curated set in #511, a generated set from #586 (FEAT-C35-3,
sway-rig build products), or a split — and "this pass deliberately
does not choose... What is decidable now is that the artifact needs
one named owner before #579 or #580 reaches AC-4." A companion comment
on #579 makes the same point from the CAP-34 side: "a store listing is
downstream of #586's capture manifest, and three channels break at
once if that manifest goes stale." #854's `ordering_after` names only
`TASK-C579-3`; it draws no edge to #511 or #586. #854's own body
compounds this — it asserts the screenshots come "from the same set
CAP-27 (#511) produces, not a second commissioned set" as settled fact,
which the 2026-08-08 review comment says is precisely not yet true.
**Recommendation:** add the ordering edge once the producer is chosen
(#511 vs #586 vs split), and do not start AC-1/AC-3 work before that —
otherwise the executor is forced into exactly the "second commissioned
set" the issue prohibits, just to unblock itself.

**3. (Medium) AC-4's "the same place the other channels record theirs" names no such place, so the AC is gameable.**
No file in this repository currently records per-channel maintainer-
week costs. The closest existing mechanism, #582 (CAP-34 PF-4,
"Download KPI"), is a different metric (release-asset download counts,
not review-cost mw) and is itself only planned, not built. Without a
named ledger, AC-4 can be satisfied by writing the number literally
anywhere — a stray issue comment, a paragraph in this issue's closing
note — and still technically "record" it, while the KC-34-1 arithmetic
in AC-5 becomes unauditable next cycle because nobody knows where to
look.
**Recommendation:** name the actual artifact (e.g. a row in a tracked
cost table alongside whatever winget/Homebrew end up using) before
work starts, or file the prerequisite that creates it.

**4. (Medium) A single review cycle is a weak basis for a "per cycle" cost claim, and the chain's own acceptance criteria only ask for one.**
TASK-C579-3's AC-3 asks for the propagation automation to be
"demonstrated end to end once, with the resulting PR linked" — there
is no committed Flathub submission yet, so #854's AC-4/AC-5 will
necessarily be evaluated against a single, setup-heavy first cycle
(initial Flathub review is well known to run slower than routine
version-bump reviews). Extrapolating "per cycle" cost, and a possible
KC-34-1 drop decision, from n=1 risks either a false pass (first cycle
looks cheap because the bot did the work and a human wasn't yet
needed) or a false kill (first cycle looks expensive because it's the
only one that includes onboarding friction).
**Recommendation:** require at least two cycles of recorded cost
before AC-5's drop/keep arithmetic is treated as decisive, or
explicitly caveat a single-cycle number as provisional in the writeup
AC-5 asks for.

**5. (Low) "the source of each [screenshot] recorded" has no defined schema, so it can't be checked by anything other than a human skim.**
AC-1 requires recording where each screenshot came from but specifies
no format (metainfo comment? commit trailer? separate manifest?).
Given AC-3's premise that "a stale screenshot is a fixable drift
rather than a lost original," an automated drift check needs a
machine-readable source mapping, not prose.
**Recommendation:** pin a concrete schema, e.g. a companion
`screenshots.json`/manifest mapping each shipped image to its
#511/#586 source id and capture date, checked in CI the same way AC-2
checks the metainfo itself.

## What's solid

- AC-2 (AppStream validation in CI) is concrete and matches the
  standards-adoption document's own recommended
  `appstreamcli validate --pedantic` CI step — objectively checkable,
  no notes.
- The task correctly scopes itself as a *consumer* of CAP-27's asset
  set rather than commissioning a duplicate, and correctly orders
  after TASK-C579-3 (#853) for the submission itself — the intent is
  right; only the missing producer edge (finding 2) undermines it.
- KC-34-1's 0.5 mw/cycle threshold and the "arithmetic shown, dropping
  is legitimate" framing are consistent with how #579 and #518 state
  the same criterion — no internal contradiction on the numbers
  themselves, only on how soon they can be trusted (finding 4).
