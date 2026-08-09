# Issue #792: TASK-C584-2: one mvn goal emits the in-jar help tree from source, preserving the Map.jhm topic-id and TOC contract, with HelpTopicsTest unchanged and green
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

CAP-35 (#519) names its evidence plainly: *"help is HTML 3.2 in a Swing pane — instantly
signals 1998"* on the bounce list, and *"ARCHITECTURE.md records 'hosted docs are the
planned future' with no owning issue — an orphaned decision."* The capstone wants two
things: a hosted, searchable, linkable manual, and docs that cannot silently contradict
the program.

#792 delivers neither. Read its acceptance criteria honestly: at completion, the jar
contains the same ~83 HTML pages it contains today, the same `Map.jhm`, the same TOC,
and `HelpTopicsTest` passes unchanged. The user-visible delta is zero by construction —
that is the point of AC-2 and AC-5-of-the-feature. What the task buys is a *generator*,
and the generator only pays off in #793 (static site) and #585 (hosting).

That is a defensible shape for a pipeline task. What makes it worth rethinking is the
*direction* the generator runs.

## Reframe 1 (load-bearing): generate the derived target, not the load-bearing one

This feature puts a new, untested code path **upstream** of the artifact with the
strongest invariants in the whole documentation system — the offline in-jar tree that
lab machines depend on, guarded by six contract tests, a kill criterion (KC-35-1), and a
recorded ARCHITECTURE.md decision. Everything downstream of the generator (the static
site) has *no* invariants yet, because it does not exist. The risk and the payoff are on
opposite sides of the seam.

Invert it. `resources/help/**` is 104 KB of HTML so plain it is barely markup: `<h1>`,
`<p>`, `<a href=…>`, `<img>`, and a handful of `<font color=…>` tags (see
`resources/help/elements/gates/and.html`). It has relative links only, no scripts, no
stylesheets, no network references (grep for `http` across the tree returns nothing), and
a machine-checked link graph courtesy of #70. ARCHITECTURE.md's own recorded decision
states the reason this discipline exists:

> help content stays plain HTML 3.2 with relative links and no viewer-specific markup,
> and the `HelpTopicsTest` link checker (#70) keeps it truthful, **so the same tree can
> be published to the web without rewriting**.

Take the project at its word. The shipping tree *is already* the single plain-text
source; the missing target is the site. Make the generator emit the site **from** the
in-jar tree — wrap each page in a template, emit the nav from `JLSHelpTOC.xml`, emit a
search index from the page text, rewrite topic ids to URLs for #585's AC-3. Then this
feature's five ACs hold trivially rather than needing to be defended: the in-jar tree is
unchanged because it is untouched, topic ids are stable because nothing regenerates them,
the diff report is empty, `HelpTopicsTest` is green because nothing moved, and #585 can
start next week instead of behind three sequenced tasks.

I am explicitly disregarding AC-1's "produces the in-jar help tree from the source tree"
as stated. The stated criterion assumes the in-jar tree must become an output. It does
not have to be, and making it one is the single largest source of risk in FEAT-C35-1.

## Reframe 2: if Markdown authoring is wanted, make the source tree mixed — never big-bang

The counter to Reframe 1 is real: authors should not have to write HTML 3.2 for a *new*
element page, and #791 exists to pick Markdown or AsciiDoc. Fine — but that goal does not
require converting 83 existing pages.

Make both `.md` and `.html` legal source forms in the same tree. The generator renders
`.md` → in-jar HTML and passes `.html` through **byte-identically**. Topic ids and TOC
entries are declared the same way regardless of source form.

This dissolves the most expensive and most failure-prone criterion in the whole feature —
#793's "a diff report compares generated output against the shipping tree page by page,
and every intentional difference is named." For an unmigrated page the diff is empty by
definition. Migration becomes opportunistic, one page at a time, reversible, and driven by
whoever is editing that page anyway. `elements/gates/and.html` gets converted the day
someone edits the AND-gate docs, not as part of a 83-page mechanical conversion that
someone must audit line by line for `<font color=yellow>` fidelity.

## Reframe 3: AC-4 is a symptom — make the topic id primary, not derived

AC-4 ("topic ids are stable across regeneration, so a code reference to a topic cannot
break because the generator ran") is testing away a hazard the design creates. If ids are
derived from paths or titles, slug drift is inevitable and a test only catches it after
someone has already renamed a page.

Make the id the primary key: each source page declares its topic id explicitly, and the
*path* is what the generator derives. Then AC-4 is not a property to test — it is not a
thing that can happen.

Then go one step further, because the project's whole arc points there. `Help.showTopic`
and `Help.enableHelpOnButton` take bare `String` topic ids at 8 call sites
(`src/jls/edit/InteractiveSimulator.java:156`, `TruthTableEditor.java:103`,
`StateMachineDialog.java:397`, `JLSStart.java:2177`, …), and `HelpTopicsTest` guards them
with `TOPICS_USED_BY_CODE` — a **hand-maintained list of 30 string literals**. That is
precisely the species of parallel list this project has been systematically deleting:
#85/#78 already replaced the hand-maintained palette-topic table with
`Palette.entries()` + `PaletteEntry.helpTopic()`, and the test's own comment says so.

A generator that already knows every topic id should emit a `HelpTopics` constants class
(or, better, fold element topic ids into #78's registry where `helpTopic()` already
lives). `Help.showTopic(HelpTopics.INTER_SIM)` makes a dangling topic reference a
*compile* error, and `TOPICS_USED_BY_CODE` stops existing. That is the drift-proofing
CAP-35 PF-4 is reaching for, available here as a side effect rather than as a separate
1–2 mw feature.

Consequence for this issue's title: "HelpTopicsTest unchanged" is exactly right as a
*migration oracle* and wrong as a permanent freeze. Say which one you mean in the issue,
or the next person will read the hand-maintained list as a ratified contract.

## Reframe 4 (the out-of-the-box one): "signals 1998" is a rendering complaint

None of #791/#792/#793 changes one pixel of what a student sees. The pipeline could land
complete, on time, all ACs green, and the bounce-list evidence that motivated CAP-35
would be exactly as true as before. KC-35-1 half-anticipates this ("if the in-jar renderer
forces the source format below usefulness, upgrade the viewer") but frames the viewer as a
*contingency*, priced only if the source format gets squeezed.

Price it first instead. `Help.buildWindow()` (`src/jls/Help.java:207`) constructs a bare
`JEditorPane` and never installs a `StyleSheet`. `HTMLEditorKit` accepts one
programmatically: body font and leading, heading scale, link color, code/`<tt>` styling,
margins, and colors drawn from the FlatLaf palette the project adopted in #153. That is
one file, roughly a day, works against today's content with no migration, no new build
step, and no new source format — and it moves the actual complaint further than the
entire three-task pipeline does. It also changes the input to #791: the "viewer-safe
subset" is a different, wider thing once the viewer has a stylesheet, so choosing the
source format before pricing the viewer gets the decision order backwards.

## The one criterion here I would keep and land immediately

AC-3 (offline-completeness) is the only criterion in #792 not already satisfied by
existing tests, and the gap is real. `HelpTopicsTest.shouldCheck` returns `false` for any
scheme-absolute link (`test/jls/HelpTopicsTest.java:138–142`), so both
`everyInlineLinkAndImageResolves` and `everyHelpFileIsReachableFromTheTopicMap` **skip**
`<img src="https://…">` today. A bundled page that silently depends on a network fetch
would pass. Zero such links exist right now, which is exactly why this is a cheap ratchet:
classify scheme-absolute `src`/`link rel=stylesheet` targets as failures (leaving `href`
prose links to external sites permitted), plant one violation, record the red transcript.
Five lines, no generator required, guards the load-bearing offline property from today
rather than from the end of a three-task chain.

## Where this leaves the issue

The invariants #792 names are the right ones — the offline lab machine is load-bearing,
the topic-id namespace is a genuine contract, #70's link checker is the floor. The
boundary comment on #584 (topic-id shape is a contract on #585's AC-3) is sharp work. My
objection is not to the constraints; it is that the task spends ~1–1.5 mw, plus the
migration risk of #793, to arrive at an artifact byte-identical to the one that ships
today, with the generator installed on the side of the seam that must not move.

Concretely, I would re-cut FEAT-C35-1 as: (1) the stylesheet/viewer upgrade priced and
landed first, since it answers the motivating complaint and redefines the "safe subset";
(2) the offline-fetch ratchet above, landed standalone; (3) a site generator that consumes
today's tree, unblocking #585 immediately; (4) mixed `.md`/`.html` source with pass-through
for legacy pages, so migration is per-page and the byte-auditable diff report is never
needed; (5) topic ids authored and emitted as typed constants, retiring
`TOPICS_USED_BY_CODE`. Steps 1–3 deliver visible value before any content moves at all.
