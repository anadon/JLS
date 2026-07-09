# Changelog

All notable changes to JLS are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and JLS uses
[semantic versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`) from
4.3.0 onward. A release is made by pushing a `v<version>` tag.

## [Unreleased] — 5.0.0-SNAPSHOT

*(Renumbered from 4.3.0-SNAPSHOT: the plugin-mechanism removal below is a
feature removal, which is a MAJOR version event under semantic versioning.)*

### Added
- Batch mode is now a documented, tested grading API (#72):
  `docs/batch-interface.md` is a normative spec — and a stability
  contract — for the `-t` test-vector grammar, the watched-element
  stdout format, the exit/stream contract, and the new waveform export.
- A `-vcd file` flag: batch runs export the value-change history of all
  watched signals as IEEE 1364-2001 VCD (readable by GTKWave/Surfer and
  autograders). JLS's two-state-plus-HiZ values map to `0`/`1`/`z`
  (never `x`); output is byte-deterministic and pinned by golden tests
  (#72).

### Removed
- The XML plugin loader (#80). It activated only when a literal `JLS.jar`
  was on the classpath — a name no artifact of this project ever shipped
  under — so it has been unreachable in every build; no plugin, manifest,
  or plugin documentation is known to exist anywhere. Anyone affected is
  invited to open an issue; a ServiceLoader-based extension registry is
  the recorded design direction if demand appears.

### Fixed
- Opening a circuit from disk records its directory again, so Save writes
  back to the source file instead of the filesystem root, and checkpoints
  (`.jls~`) land beside the circuit (#34).
- The unsaved-changes flag survives a failed save: it is now cleared only
  after the file write succeeds, so a write failure still prompts to save
  and still checkpoints (#41).
- A checkpoint queued before a save can no longer be written afterwards and
  resurrect stale content (#45).
- Every popup-menu action works again — a merge-conflict brace error had
  made all of them (rotate, flip, cut/copy/paste, undo/redo, probe, …)
  unreachable (#37).
- Print mode (`-p`/`-v`) actually loads the circuit file instead of
  printing blank pages (#48).
- Values wider than ~48 bits no longer silently become zero
  (`BitSetUtils.Create`) (#50).
- Saved strings containing backslashes round-trip correctly (#53).
- Circuits containing a `JumpEnd` load deterministically on every JVM
  (explicit constructor selection) (#55).
- A legacy `Display` with `orient 0` (Top) no longer corrupts its file on
  re-save (#57).
- Load failures report their own cause: no more stale error messages,
  swallowed subcircuit failures, or I/O errors disguised as format errors
  (#58).
- Wire hover highlights appear and disappear correctly again (#35).
- Undo/redo cancels any in-flight gesture first and refreshes other
  editors' Import menus, so nothing operates on the discarded circuit (#39).
- Invalid element parameters in circuit files (negative memory capacity,
  non-positive clock times, out-of-range truth-table entries, negative
  group indices) are rejected at load instead of crashing or livelocking
  the simulator; Memory and Clock dialogs enforce the same constraints (#52).
- Interactive simulator: Stop/Pause/Step can no longer be missed
  (volatile control state), UI updates happen on the event-dispatch
  thread at a bounded rate, Step always advances, and "Run (in
  background)" reliably suppresses UI updates for the whole run (#49).
- Malformed batch invocations print one `jls: error:` line and exit with
  status 2 (usage) or 1 (runtime failure) instead of crashing (#42).
- Assorted small fixes: hex/binary field maximums, doubled menu actions
  in the truth-table signal editor, crash reports no longer dump all
  system properties, Windows path handling, and more (#51).
- Batch mode prints watched elements in element-name order instead of
  hash-set iteration order, so the output is stable across runs, JVMs,
  and code changes; anyone diffing old batch output may see lines
  reordered (never reworded) once (#72).
- The batch signal trace no longer records a duplicate event on every
  reaction of an element that stays at HiZ (#72).

### Changed
- Command-line flags may now be longer than one letter, with
  longest-name matching: `-vcd` is the VCD export flag, no longer
  parsed as `-v` with the attached printer name `cd` (#72).
- The proprietary MTU license acceptance gate at startup is removed; JLS
  is GPLv3 (see `LICENSE` and `pop_GPLv3.pdf`) and the About dialog now
  says so (#40).
- The application version is single-sourced from the Maven build; the
  About dialog, window title, and crash reports no longer claim
  "4.1, built 2014" (#36).
- Crash reports point to the GitHub issue tracker instead of the original
  author's email.

### Security
- Circuit files are treated as untrusted input: decompression is bounded
  (zip and XZ), run-length-encoded memory images are capped by the
  declared capacity, the format sniffer no longer leaks a file descriptor
  per open, and the plugin manifest parser refuses DOCTYPEs and external
  entities (#38, #46).

## [4.2.0-alpha.1] — 2026-07-01

Post-modernization snapshot: Maven build, CI, headless test suite,
performance work, and de-duplication (the #16 program). Never released
as a binary.

## [4.1.5] — 2014-03-18

The last legacy-era state, self-reporting "JLS 4.1 build 5, 2014".
