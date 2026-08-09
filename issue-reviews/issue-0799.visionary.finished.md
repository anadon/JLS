# Issue #799: TASK-C587-1: every documented flag resolves to a FLAGS entry and every FLAGS entry is documented — the doc side of the triangle CliFlagTableTest never reads
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "a doc-facing assertion exists." The end is: **a student or an
autograder author can trust that a flag named in JLS's prose is a flag JLS
has.** #587 states it plainly — "documentation that contradicts the program
fails CI." #799 is the CLI slice of that.

That end is squarely aligned with the project's arc. `docs/batch-interface.md`
is a declared stability contract whose every claim "carries a code anchor";
ARCHITECTURE.md records help delivery as heading toward "one source of truth";
`ExtensionPointCatalogTest` and `FileFormatSpecTest` already bind two normative
documents to code in both directions. Nothing here pulls against the project.

The mechanism, though, cuts along the wrong seam — and it cuts across the very
pattern issue #71 established on this exact surface.

## The seam #71 already chose: generation, not assertion

`usage()` is not *checked against* `FLAGS`; it is *generated from* `FLAGS`
(`JLSStart.usageText()`, src/jls/JLSStart.java:1190). `CliFlagTableTest` exists
only to stop someone reverting to a hand-maintained list. And
`docs/batch-interface.md:24` does not duplicate the flag list at all — it
*defers*: "The flag table in `src/jls/JLSStart.java` (`FLAGS`) is the single
authoritative flag list; `jls -h` prints usage generated from it."

So the "triangle" framing is off. Two of the three sides are machine artifacts
projected from one table. #799 proposes to make the third side a *scraped prose
corpus* — and scraping is the strictly weaker tool precisely where the project
has already demonstrated the stronger one works.

## Evidence that the scrape is the hard way

I ran the extraction #799 asks for against the three named corpora. It is not
close to clean:

- **README.md** is full of non-JLS dash tokens: `-jar`, `-Djls.laf`,
  `-Dawt.toolkit.name`, plus `-it` / `-f` / `-c` / `-v` from docker, cosign,
  `sha256sum -c`, and `-x86_64` / `-aarch64` from installer *filenames*.
- **docs/batch-interface.md:317** yields a phantom flag `-driven`, from the
  phrase "a `` `-t` ``-driven fixture."
- **resources/help/execution/execution.html** (the one help page that documents
  flags at all) writes attached-operand forms: `-icounter`, `-vprinter`,
  `-sparameters`, `-tinputsigs`, `-tname`, `-sname`, `-rprinter`, `-pprinter`,
  `-dtime`, `-d1000`. Under a naive extractor every one of these is a
  "documented flag with no `FLAGS` entry" — a red build on correct docs.

The only way to make that extractor pass is an ignore list. An ignore list is
an un-asserted hole that grows silently — the exact failure mode this ratchet
exists to prevent, reintroduced inside the ratchet.

Second, AC-2 fails on day one as written. `-board` and `-pins` appear in
**none** of the three named sources; they occur only in research and roadmap
documents (`docs/icestick-bitstream-handoff.md`,
`docs/capability-roadmap/*`). Implementing AC-2 literally means either the
first commit is red until someone writes README prose for two FPGA-adjacent
flags, or the "named sources" list quietly grows to include roadmap
speculation — making forward-looking design notes load-bearing user
documentation.

## The reframing: generate the table, resolve the prose

I am not disregarding the outcome; I am replacing the mechanism.

**(A) One generated flag table.** #799 is `ordering_after: [TASK-C584-2]` —
by the time it runs, #584's single-source doc pipeline that "emits two targets
from one source, no file hand-edited" already exists. Make `FLAGS` an *input*
to that pipeline. Emit one normative table (a `docs/cli-flags.md`, or a
marker-delimited region inside `batch-interface.md` §1) with a row per
`FlagSpec`: flag, arity, operand name, description, plus a "explained in"
column naming the narrative anchor. The test is then "regenerate and compare
bytes" — the same shape as the reproducible-jar check the project already runs
on every push.

This kills AC-2 as a category: an undocumented new flag is *impossible*, not
merely detected. It kills #587's AC-4 concern too — the table flows into both
generated targets from one source, so it cannot be true in the jar and false on
the site. And `-board`/`-pins` become a non-event.

**(B) One resolver, not a regex.** Expose the parser's own longest-match rule:

```java
static @Nullable String resolveFlagToken(String token);  // "-icounter" -> "i", "-vcd" -> "vcd", "-q" -> null
```

package-visible in `JLSStart`, and used by `parseCommandLine` itself so a second
copy cannot fossilize. `-icounter`, `-tinputsigs`, `-d1000` resolve correctly
*by construction* rather than by ignore list. #587's own dedup comment already
asked for this ("they should read it through one accessor rather than two
hand-rolled reflections") for #524's benefit; #799 is the issue that should
land it.

**(C) Scope extraction structurally.** Do not scan prose. Scan only fenced
blocks and code spans whose command word is `jls` (or that follow `java -jar
…jls….jar`), and only tokens after that word. `-jar`, `-Djls.laf`, docker/cosign
flags, and `-x86_64` filenames vanish because they are structurally outside the
corpus — no ignore list. `` `-t` ``-driven vanishes because the code span's
content is exactly `-t`.

With (A) the reverse direction is free; (B)+(C) reduce the forward direction to
the one thing generation genuinely cannot fix: free prose naming a flag the
parser would reject. That residue is small, real, and cheaply asserted.

## The larger arc this touches

If #799 ships as a fourth bespoke doc scraper — after `HelpTopicsTest`,
`HotkeysHelpAccuracyTest`, `ExtensionPointCatalogTest`, `FileFormatSpecTest` —
#587's AC-2 and AC-3 will each grow a fifth and sixth, with three
independently-written HTML/markdown readers and three private notions of "what
counts as a documented claim." The reusable seam #587 as a whole is missing is
a **doc corpus abstraction**: one test-side helper enumerating doc sources
(repo markdown, README, the in-jar tree, the generated site) and yielding
anchored code spans and typed table rows. #799 is the cheapest place to
introduce it, and the flag table is the smallest honest first client.

The deeper statement of the arc: JLS's documentation should be *hand-written
prose surrounding generated islands*, where every enumerable fact — flags,
element ports, accelerators, save-format tags — is projected from the program
and the ratchets guard only the prose that surrounds them. That is "docs cannot
lie" achieved by construction. A ratchet tells you the docs are wrong after you
have written them wrong; generation means you cannot write them wrong.

## What I would keep verbatim

AC-3 (both failure directions demonstrated with recorded transcripts) and AC-4
(the message names the file and the contradicted source of truth) are exactly
right and survive the reframing unchanged — with the note that under (A) the
"planted undocumented flag" negative check becomes "planted hand-edit of a
generated region," which is a stronger demonstration, not a weaker one. AC-5
(`CliFlagTableTest` keeps passing unmodified) also stands; nothing above touches
it, since `resolveFlagToken` is additive.
