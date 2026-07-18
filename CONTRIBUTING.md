# Contributing to JLS

Thanks for helping improve JLS. It is a teaching tool; correctness and
simplicity beat cleverness.

## Getting started

```sh
mvn verify          # compile (warnings are errors), tests, SpotBugs
java -jar target/jls-*.jar
```

JDK 25 or newer and Maven are the only requirements (the Java floor
follows the current LTS and is revisited each LTS cycle). Sources live in
`src/`, tests in `test/` (JUnit 5, headless — no display needed).

## Making changes

- **Open or comment on an issue first** for anything beyond a trivial fix.
  The open issues carry file:line evidence and acceptance criteria;
  #33 is the tracking issue that orders the current program of work.
- **Contributions arrive as pull requests with reviewable diffs.** Per
  [SECURITY.md](SECURITY.md), attachments on issues/comments are never
  applied — do not ask maintainers to extract archives.
- **Every bug fix carries a regression test** that fails before the fix
  and passes after. The headless harness in `test/` has models for
  loader round-trips, golden simulations, and CLI smoke tests.
- **Match the surrounding style.** The codebase keeps the original
  author's layout (tabs, `// end of X method` trailers); new code should
  read like the code around it.
- **Value semantics by default** (#94): in new and touched code, fields
  and locals are `final` unless mutation is the point; standalone value
  carriers (all fields assigned once, no identity semantics, no
  supertype) are `record`s; collection-returning accessors hand out
  `List.copyOf`/unmodifiable views rather than internal state. Do not
  make a class a record if its `equals`/`hashCode` are intentionally
  non-structural (see `jls.sim.SimEvent`), and never land a repo-wide
  `final`/formatting sweep — churn hides real diffs.
- `mvn verify` must be green: it runs the test suite and SpotBugs
  (threshold High). Do not add blanket entries to
  `config/spotbugs-exclude.xml`; new entries need a `Class` scope and a
  justification.
- **Nullness is a compiler-checked contract** (issue #93): the default
  build runs [NullAway](https://github.com/uber/NullAway) with
  [JSpecify](https://jspecify.dev/) annotations, enforced only inside
  packages whose `package-info.java` declares `@NullMarked`. The ratchet
  convention: a `@NullMarked` package never becomes unmarked (the list in
  `test/jls/NullMarkedRatchetTest.java` only grows), and new packages are
  born `@NullMarked`. Prefer honest `@Nullable` annotations and explicit
  checks over `@SuppressWarnings("NullAway")`; every suppression needs a
  justification comment.
- **Pull requests must also pass CodeQL code scanning**, which runs
  automatically on every PR. Findings appear as a check on the PR and
  under the repository's **Security tab → Code scanning** (alert details
  require being logged in with access to the repo's security views).
  Fix real findings; if one is a false positive, say so in the PR so a
  maintainer can dismiss it with a recorded reason.

## Coverage ratchet

`mvn verify` enforces a JaCoCo coverage ratchet (issues #66 and #159):
bundle-wide floors for the INSTRUCTION, LINE, and BRANCH counters, plus
per-package floors that keep the tested core (`jls`, `jls.sim`,
`jls.elem`, `jls.collab.op`) un-regressable. The current floor values
live in `pom.xml` under the `coverage-ratchet` execution — that comment
block, not this file, is the source of truth for the numbers.

The climb convention:

- **Floors only ever move up.** A PR that raises coverage should also
  raise the nearest floor (bundle and/or package) to just below the new
  measurement, so the gain cannot silently erode later. "Just below"
  means a small margin (a few tenths of a point) to absorb JDK jitter.
- **Raise floors from headless numbers only.** The floors are pinned to
  a plain `mvn verify` with no display so the build passes anywhere.
  CI's display-substrate run (`xvfb-run` with `-Djls.test.headless=false`)
  exercises the dialog and editor suites too and measures higher — do
  not copy those numbers into the floors.
- **Use `mvn clean verify` when touching floors.** The JaCoCo agent
  appends to `target/jacoco.exec`, so an unclean rerun unions with
  prior coverage and a floor that should trip never will. Relatedly, a
  floor that has never been seen to fail should be assumed vacuous: rule
  `include` patterns must be dot-form package names (`jls.collab.op`,
  not `jls/collab/op` — the slash form silently matches nothing).
- `jls.edit` is deliberately unfloored until the editor decomposition
  work (#84/#91) makes that code testable headlessly; do not add a
  floor there that would either bind at ~0% or block unrelated PRs.
- Milestone: when the headless bundle LINE ratio crosses 0.50, the PIT
  mutation-testing evaluation (#161) is unblocked.

## Reporting bugs

Use the issue tracker. Include the JLS version (Help → About), your OS
and JDK, the smallest circuit file that reproduces the problem, and the
`JLSerror` file if one was produced.

## Reporting security issues

See [SECURITY.md](SECURITY.md) for the disclosure policy.

## License

JLS is GPLv3-or-later. By contributing you agree your contribution is
licensed under the same terms.
