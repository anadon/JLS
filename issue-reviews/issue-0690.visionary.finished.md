# Issue #690: TASK-C524-3: the CLI can be asked which contract version it implements, so an adapter refuses an incompatible build by name instead of misgrading
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "a version query." The end is one property: **a mismatch between what an
adapter expects and what a build does must never produce a plausible score.**
Everything else — flag shape, machine-readable form, conformance clause — is
mechanism, and the issue has picked one mechanism (ask, then compare) without
noticing that JLS already solved this exact problem the other way round.

The precedent is `Circuit.readFormatHeader` (`src/jls/Circuit.java:732-774`).
The *file* declares `FORMAT n`; `FORMAT_VERSION = 2` (`Circuit.java:102`) is the
reader's own number; a file declaring a newer version is refused as
`NEWER_FORMAT` with a named next-step hint (`Circuit.java:777-779`). Note what
JLS does *not* do there: it does not export a "what format version do you read?"
query and leave every caller to compare. **The artifact declares, the tool
refuses.** That asymmetry is the whole reason the file format has never silently
half-loaded a newer circuit, and it is the design #690 should copy rather than
invert.

Also worth stating plainly, because it changes the sizing: at HEAD JLS has **no
version output on the CLI at all**. `FLAGS` (`JLSStart.java:759-789`) has 14
entries and none of them is `-version`; `JLSInfo.versionString`
(`JLSInfo.java:16-23`) is single-sourced from the pom and then surfaces only in
the window title (`JLSStart.java:1281`), About, and the crash handler. A headless
grading run today cannot record *which JLS produced this grade* — in a project
that ships an SBOM, reproducible jars, cosign signatures and build-provenance
attestations (README "Installing JLS"), the grading path is the one place where
artifact identity is thrown away. That is a bigger hole than the one #690 names,
and it is fixed by the same half-day of work.

## Reframing A (headline): make JLS refuse, not the adapter

Replace "query + adapter-side comparison" with a **precondition assertion the
tool evaluates**: `jls -b -contract 2 -check e.txt -report r.xml c.jls`. If this
build does not implement batch contract 2, JLS refuses before simulating, with
one stable `jls: error: ...` line naming both numbers, and produces no report.

Why this is strictly better than AC-1/AC-2 as filed:

- **It fails closed.** An adapter that forgets to query still misgrades; an
  adapter that passes `-contract` cannot. The check is welded to the same
  invocation that does the grading, so there is no window in which the two can
  disagree, and no "did anyone remember to call it?" question — which is exactly
  the hole the adversarial pass found in #525/#526/#528/#530, none of which
  budgets an AC for calling a version query.
- **The comparison is written once, in Java, conformance-tested once** — instead
  of four times across four adapter languages (`run_autograder` shell, a GitHub
  Action, PrairieLearn's `externalGrader`, an nbgrader cell), each with its own
  chance to compare version strings lexically and get `10 < 9`.
- **Third-party adapters get it free.** JLS's grading users are instructors, not
  the four platforms in CAP-21; the university lab script nobody upstreams is the
  most likely misgrader and the least likely to implement a handshake.
- **No new exit status.** Map the refusal to **status 2 (usage error)**: the
  caller asked for something this build cannot provide. That keeps #466's H4
  discipline intact (3 is reachable only via `-check`) and keeps the frozen
  status table at four rows. The adapter distinguishes it by the documented error
  text, not by a fifth number. (A dedicated status 4 is the alternative; it buys
  easier discrimination at the cost of widening the very table #524 is freezing.
  I would not pay that.)

Prior art beyond JLS's own: `cmake_minimum_required`, `terraform`'s
`required_version`, `python_requires`. All of them are assertions evaluated by
the tool, none is an interrogation performed by the caller.

## Reframing B: the durable pin belongs in the lab, not the adapter

Push A one step further and the adapter disappears from the story entirely.

Ask which artifact survives longest. The adapter shim is the *least* durable
thing in the system — copied into a course repo, forked per instructor, rewritten
when Gradescope changes its image. The **lab** is the durable one: #466 is
building `examples/autograde/lab-01/` (circuit, `-t` vectors, expectations,
rubric, README) precisely because "the worked lab is the deliverable course
authors actually copy." A lab authored in 2027 gets re-run in 2031.

So put the pin where the durable artifact is: an optional first-line production
in the **expectations grammar** — `CONTRACT 2` — that #466 is writing into
`docs/batch-interface.md` §2.5 *right now*. JLS refuses to grade a lab that
declares a contract it does not implement. That is `FORMAT n` transposed onto
grading, exactly, and it protects all four platforms plus every unshared local
script with zero adapter code.

The timing argument is the sharp one: **§2.5 does not exist yet, so this
production is free today and a versioned contract change tomorrow.** #690 is
ordered *after* #687, which is ordered after #686, which is ordered after #466.
By the time #690 is picked up under the filed ordering, the cheapest moment to do
this will have passed.

## Reframing C: one self-describing manifest, not a batch-contract-only query

AC-1 asks for "a documented invocation returning the implemented contract
version." Do not mint a batch-specific one. `docs/capability-roadmap/lf-07-api-and-platform.md`
already argues JLS's one-verb CLI is "the project conceding the principle and
then stopping at the smallest possible instance of it," and designs `jls.api` and
a `--serve` protocol *each under `docs/batch-interface.md` §6's identical
promise*. A `-batch-contract-version` flag guarantees a second and third query
flag later.

Emit one manifest from one `-version` invocation, stable `key value` lines:

```
jls 5.0.5
batch-contract 2
file-format 2
```

`file-format 2` is free — it is `Circuit.FORMAT_VERSION`, already the number a
build refuses newer files against, and today it is knowable only by opening the
jar. Later lines (`api 1`, `serve 1`) are additive under §6's own "additions that
cannot break a conforming consumer" rule. `-h` is the working precedent for the
mechanics: `Arity.NONE`, handled in `apply` (`JLSStart.java:1025-1029`), exits
before any operand is required — which is AC-3 satisfied for free.

And **the same numbers belong in the report**. #466 §7.6 specifies the xUnit
report as one `testcase` per expectation with no `timestamp`/`hostname`/`time`
attribute — and **no version attribute either**. The #524 thread flags this as a
live precondition ("if #466 ships its report schema without a version field, this
issue freezes an unversioned schema"), and reading #466's §7.6 confirms the field
is not there. An nbgrader cell holding only `results.xml`, or a grade dispute
opened six weeks after the run, cannot invoke anything; a report that names its
contract and its build is self-identifying forever. One attribute on the root
element closes both the AC-4 "same source of truth" question and the provenance
hole named above.

## Ordering: this is the first task of C524, not the third

Under B and C, #690's payload is a manifest, a report attribute, and a refusal
path — all additive, all independent of the freeze, all *cheaper before* the
contract is frozen than after. The filed order (`ordering_after: TASK-C524-2`)
has it landing last, which means (a) the expectations grammar closes without a
`CONTRACT` line, (b) every report emitted between now and the freeze is
permanently anonymous, and (c) AC-4 has to reach backwards for a "source of
truth" that #687 — a *policy* plus a CI gate — never promised to produce. Land
the number first and #687's ratchet has something concrete to ratchet: a test
asserting that the manifest integer, the report attribute, and the frozen
document's declared contract number are the same value.

## Acceptance criteria I am disregarding

- **AC-2 ("a reference adapter check demonstrates refusal") — drop it.** Under A
  and B there is no adapter-side refusal to demonstrate, because the refusal is
  inside JLS. What remains worth doing is durable and already has a home:
  `examples/autograde/autograde.py` is a shipped reference adapter pinned in CI
  by `test/jls/AutogradeBridgeExampleTest.java`. Have it pass `-contract N` and
  assert the named refusal against a stub reporting a wrong number. That is
  continuously checked rather than demonstrated once, and it needs no new
  fixture, no new owner, and no scope loan from #525/#526/#528/#530.
- **AC-3's "no license state" — drop the clause.** JLS has no license state; the
  MTU EULA gate was removed (`src/jls/JLS.java:40-41`). An unfalsifiable clause
  inside a frozen contract is dead weight. "No circuit, no lab" is the real and
  sufficient requirement.

## Does it strengthen the arc?

Yes, with one caveat. `docs/grand-architecture.md:34-37` treats the headless
surface as a co-equal front end, and a front end that cannot say what it is is
not co-equal — the GUI has had an About box all along. The caveat is lf-07's:
building *batch-contract-specific* version machinery canonizes the one-verb CLI
at the moment the roadmap wants to move past it. Reframing C is the whole
mitigation: one manifest that names every versioned surface JLS has, growing
additively as `jls.api` and `--serve` arrive, rather than one flag per freeze.

## Verdict

**endorse-with-reframing.** The end is right, uniquely unowned, and — per the
#524 review, which found `docs/batch-interface.md` already normative and already
held by five test classes — this is the only part of FEAT-C21-1 that is not
substantially true at HEAD. Keep it, and change four things: let **JLS** refuse
rather than the adapter (A); put the durable pin in the **lab's expectations
file**, in the `FORMAT n` idiom, while §2.5 is still unwritten (B); ship one
**self-describing manifest** plus a report attribute rather than a batch-only
query (C); and **land it first** in the C524 sequence, not last, so the ratchet
in #687 has a concrete number to enforce and no grading artifact is ever emitted
anonymous.
