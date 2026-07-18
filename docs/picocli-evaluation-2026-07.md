# picocli Evaluation: Spike Results and Recommendation (#156)

*2026-07-18. Deliverable of issue #156, the investigation issue the
library survey (`library-survey-2026-07.md` §3) filed for its qualified
picocli recommendation. A behavior-preservation spike was built against
picocli 4.7.7 and driven through a harness that mirrors every
parser-level assertion in `CliFlagTableTest` and `CliSmokeTest`, plus
the attached-operand and `-i` lookahead semantics those tests do not
cover. The spike, the harness, and the raw results are reproduced in
the appendices so the experiment can be re-run.*

## Verdict

**Rejected for the current CLI shape; re-evaluate if and when the CLI
grows subcommands.** The spike *confirms feasibility* — full contract
parity, 46/46 harness checks including byte-identical `-h` output and
exact `jls: error: …` wording and exit codes — but it also measures
the cost, and the cost lands almost exactly where the benefit was
supposed to be:

1. **The adapter code needed to preserve the pinned contract is about
   as large as the hand-rolled parser it would replace.** The contract
   shims (pre-tokenizer, exception-message translator, `-i` lookahead
   consumer, usage-text generator — §"What fidelity required" below)
   total ~150 lines; `JLSStart`'s entire hand parsing core
   (`parseCommandLine` + `operand` + `usageError` + `usageText`) is
   ~130 lines. The per-flag boilerplate picocli eliminates is the part
   JLS already generates from the `FLAGS` table (#71); the part picocli
   cannot express natively is exactly the pinned contract.
2. **Error-wording fidelity rests on parsing picocli's exception
   message strings, which are not API.** The contract's
   `option -t requires a test file operand` line must be recovered by
   regex from picocli's `"Unknown option: '-q'; Expected parameter for
   option '-t' but found '-q'"` — text that can change in any picocli
   release. The contract tests would catch a change, but that turns
   every picocli upgrade into a CLI-contract risk, inverting the
   dependency's value.
3. **The headline new capability — shell completion — does not need
   picocli.** The generated script was produced and sanity-checked
   (appendix C): it is a self-contained 226-line bash/zsh function
   built from a static list of the option names. The existing `FLAGS`
   table already holds every fact the generator used. A ~50-line
   generator over `FLAGS` (or a golden-tested checked-in script — the
   flag table changes a few times per year) delivers the same artifact
   with zero dependencies. If completion is wanted, file it as its own
   small issue.
4. **Maintenance fact-check (survey ground rule 4).** The survey text
   said "actively maintained (latest release June 2026)". Maven
   Central says otherwise: the latest release is **4.7.7, published
   2025-04-19** — ~15 months before this evaluation — and the
   metadata's `lastUpdated` confirms nothing newer has shipped.
   picocli is mature and stable rather than abandoned, but under the
   survey's own active-maintenance policy this is a caution flag, not
   a point in favor. (Upstream commit activity could not be checked
   from this session; the release ledger is authoritative for what a
   dependency would actually pin.)

What would change the answer: a real subcommand surface (`jls export`,
`jls sim`, `jls grade` …). picocli's model fits subcommands natively,
completion becomes genuinely hard to hand-roll (nested option sets),
and a subcommand CLI would be a *new* contract negotiated at design
time rather than an old one reproduced shim-for-shim. The single-dash,
attached-operand, longest-match contract JLS pins today is the shape
picocli is worst at; a `git`-style CLI is the shape it is best at.

## Spike method

- **Out-of-tree:** the spike (`JlsPicocliSpike.java`, appendix A) lives
  outside the build so `pom.xml` and the product remain untouched; it
  reimplements the twelve-flag `FLAGS` table on picocli 4.7.7's
  programmatic API (`CommandSpec`/`OptionSpec`), applies the same
  post-parse validations as `JLSStart.apply()`, and prints the parsed
  state (`PARSED batch=… circuit=…`) instead of running a circuit.
- **Harness:** a script (appendix B) runs the real
  `jls.JLS` (headless, from `target/classes`) and the spike side by
  side. For every usage-error and help case it asserts **identical exit
  code and byte-identical stderr**; for accepted command lines it
  asserts the spike's parsed state matches the hand parser's semantics
  (verified against `JLSStart.parseCommandLine` by reading, since the
  real CLI proceeds into runtime behavior on success).
- **Coverage:** all parser-level assertions of `CliFlagTableTest`
  (usage/table agreement, `-h` output, every-flag-accepted, `-vcd`
  longest match, unknown-flag wording) and `CliSmokeTest` (trailing
  operand, bare `-`, empty argument, `-d` validation trio, `--`
  end-of-flags), plus cases the tests do not pin: attached operands on
  single- and multi-character flags, attached/separated `-i` operands,
  flags after the positional, operands that look like flags.

**Result: 46/46 checks pass, zero divergences** — after the fidelity
work described next, which is the actual finding.

## What fidelity required (the measured cost)

Vanilla picocli, configured the obvious way, failed the contract in
four distinct places. Each needed a shim:

1. **Attached operands on multi-character flags reintroduce the #72
   misparse.** picocli's POSIX clustering resolves `-vcdout.vcd` as
   `-v` with operand `cdout.vcd` — silently, a printer name instead of
   a VCD file. The hand parser's longest-match rule picks `-vcd`. Fix:
   a pre-tokenizer that reimplements longest-match over the flag table
   and rewrites `-vcdout.vcd` → `-vcd=out.vcd` before picocli sees it
   (`=`-attachment, not token-splitting, so `-d-5` survives). The same
   pass rejects `-bx`/`-bh` wholesale (`unknown option -bx`), because
   the contract has no clustering at all. ~40 lines, and it *is* the
   hand parser's matching loop, kept.
2. **Exception translation.** A custom `IParameterExceptionHandler`
   maps picocli's exceptions to the pinned one-line diagnostics and
   exit 2. Straightforward for `UnmatchedArgumentException` /
   `MissingParameterException` — except that with
   `setUnmatchedOptionsAllowedAsOptionParameters(false)` (itself
   required, or `-t -q` silently takes `-q` as the test-file name)
   picocli reports the `-t -q` case as an `UnmatchedArgumentException`
   whose unmatched list is *empty*, and the offending option is only
   recoverable by regex from the message text. ~50 lines, brittle
   across upstream releases.
3. **The `-i` optional-operand lookahead.** "Consume the next token
   only if it names an image file" needs a custom
   `IParameterConsumer`. Two surprises: picocli's `ParseResult` does
   not report an option as matched when its consumer sets no value
   (so `-i` alone was invisible — an out-of-band sentinel was needed),
   and an *attached* `-i` operand must bypass the lookahead (the hand
   parser takes it unconditionally, then validates), which the
   consumer cannot distinguish without help from the pre-tokenizer.
   ~25 lines plus two static flags of shared state.
4. **Usage text.** The pinned `-h` output (`  -d time : set simulation
   time limit …`) is not a format picocli's help renderer produces;
   the spike regenerates it by iterating the `CommandSpec` model —
   preserving the #71 no-drift property, but that property already
   exists via `FLAGS`. ~25 lines.

## Investigation-task outcomes (from the issue checklist)

- **Spike / contract tests:** done out-of-tree; every contract
  assertion's expected stderr and exit code reproduced exactly (the
  JUnit classes themselves were not pointed at the spike, since that
  would require the dependency in `pom.xml`; the harness mirrors each
  assertion, including the exact-equality ones, and is stricter in the
  cases it adds).
- **Jar vs. vendoring — decided (conditionally on ever adopting):**
  take the **jar** (408 KB, 229 classes, multi-release with a proper
  JPMS `module-info` for Java 9+). Vendoring means importing
  `picocli/CommandLine.java`, a **1.24 MB single source file**
  (~12,000 lines), plus `AutoComplete.java` (55 KB) if completion
  generation is wanted — unreviewable in-tree, Apache-2.0 headers to
  preserve inside a GPLv3 tree, and manual updates. The size argument
  for vendoring evaporates once the source file is bigger than the
  jar.
- **Completion generated and sanity-checked:** yes (appendix C):
  226 lines, valid bash (`bash -n` clean), zsh-compatible via
  `bashcompinit`, `flag_opts` = `-h -b`, `arg_opts` = the ten
  operand-taking flags, falls back to filename completion for operand
  positions (`-o default`). Note the generated script is *static* —
  picocli is a build-time generator here, not a runtime need, which is
  what makes the no-dependency alternative viable.
- **Milestone to fold into:** none scheduled. The survey's gate ("next
  milestone that grows the flag table", e.g. #59 stages) is retired by
  this evaluation: adding flags to the existing table costs one
  `FlagSpec` row and one `apply()` case each — picocli does not beat
  that while the contract shims exist. The re-evaluation trigger is
  **subcommands**, not flag count.
- **Recommendation written into the survey:** `library-survey-2026-07.md`
  §3 now carries the verdict and points here.

## Appendix A — spike source (`JlsPicocliSpike.java`, picocli 4.7.7)

Compile with `javac -cp picocli-4.7.7.jar` and run with both on the
classpath. `--emit-usage` prints the model-generated usage text;
`--emit-bash-completion` prints the completion script; any other
argument vector is parsed as a `jls` command line.

```java
package jlsspike;

import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Issue #156 spike: reimplement the JLSStart FLAGS table on picocli
 * 4.7.7 and measure how much of the pinned CLI contract
 * (CliFlagTableTest / CliSmokeTest / docs/batch-interface.md section 1)
 * survives. The spike parses, validates, and then prints the parsed
 * state as "PARSED k=v ..." on stdout (instead of running a circuit),
 * so a harness can compare parser-level behavior against the real jls.
 */
public final class JlsPicocliSpike {

	/** One row of the flag table, mirroring JLSStart.FlagSpec. */
	private static final class Row {
		final String flag;
		final int arity; // 0 none, 1 required, 2 optional
		final String operandName;
		final String operandWhat;
		final String description;

		Row(String flag, int arity, String operandName, String operandWhat,
				String description) {
			this.flag = flag;
			this.arity = arity;
			this.operandName = operandName;
			this.operandWhat = operandWhat;
			this.description = description;
		}
	}

	private static final Row[] ROWS = {
		new Row("h", 0, null, null, "print this message and exit"),
		new Row("b", 0, null, null, "run in batch (headless) mode"),
		new Row("i", 2, "imagefile", "an image file",
				"export an image of the circuit (default circuit_file.png; use .jpg/.jpeg for JPEG, .svg for SVG)"),
		new Row("s", 1, "file", "a startup file", "startup parameter file"),
		new Row("t", 1, "file", "a test file", "test input file"),
		new Row("d", 1, "time", "a time limit",
				"set simulation time limit (a positive integer)"),
		new Row("p", 1, "printer", "a printer name",
				"print the whole circuit (subcircuits too) to the named printer"),
		new Row("v", 1, "printer", "a printer name",
				"print only the top level of the circuit to the named printer"),
		new Row("r", 1, "printer", "a printer name",
				"print the signal trace to the named printer"),
		new Row("vcd", 1, "file", "a VCD output file",
				"write watched-signal waveforms to the named VCD file (batch mode)"),
		new Row("export", 1, "file", "an output file",
				"export the circuit as Verilog-2005 (.v) or VHDL (.vhd/.vhdl), chosen by the file extension"),
		new Row("savetext", 1, "file", "an output file",
				"re-save the circuit to the named .jls file as plain (uncompressed) text"),
	};

	private static boolean isImageFileName(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".png") || lower.endsWith(".jpg")
				|| lower.endsWith(".jpeg") || lower.endsWith(".svg");
	}

	/**
	 * Set by ImageOperandConsumer when -i is matched: picocli's
	 * ParseResult does not report an option as matched when its custom
	 * consumer sets no value, so the spike tracks it out of band.
	 */
	private static boolean imageExportRequested;

	/**
	 * Set by the attached-operand pre-pass when -iX was written with the
	 * operand attached: the hand parser takes an attached operand
	 * unconditionally (then validates it), and only applies the
	 * image-file-name lookahead to a separated operand.
	 */
	private static boolean imageOperandForced;

	/** Lookahead consumer reproducing the -i optional-operand rule. */
	private static final class ImageOperandConsumer
			implements IParameterConsumer {
		public void consumeParameters(Stack<String> args, ArgSpec argSpec,
				CommandSpec commandSpec) {
			imageExportRequested = true;
			if (!args.isEmpty()
					&& (imageOperandForced || isImageFileName(args.peek()))) {
				argSpec.setValue(args.pop());
			}
			imageOperandForced = false;
		}
	}

	private static CommandSpec buildSpec() {
		CommandSpec spec = CommandSpec.wrapWithoutInspection(
				(Callable<Integer>) JlsPicocliSpike::runParsed);
		spec.name("jls");
		for (Row r : ROWS) {
			OptionSpec.Builder b = OptionSpec.builder("-" + r.flag)
					.description(r.description);
			if (r.arity == 0) {
				b.arity("0").type(boolean.class);
			}
			else if (r.arity == 1) {
				b.arity("1").type(String.class).paramLabel(r.operandName);
			}
			else {
				b.arity("0..1").type(String.class).paramLabel(r.operandName)
						.parameterConsumer(new ImageOperandConsumer());
			}
			spec.addOption(b.build());
		}
		spec.addPositional(PositionalParamSpec.builder()
				.paramLabel("circuit_file").index("0").arity("0..1")
				.type(String.class).build());
		return spec;
	}

	private static CommandSpec SPEC;
	private static CommandLine CMD;

	/** Usage text generated from the picocli model (no-drift property). */
	static String usageText() {
		StringBuilder text = new StringBuilder();
		text.append("usage: jls [ flags ] [ -- ] [ circuit_file ]\n");
		for (OptionSpec o : SPEC.options()) {
			text.append("  ").append(o.longestName());
			if (o.arity().max() == 1 && o.arity().min() == 1) {
				text.append(' ').append(o.paramLabel());
			}
			else if (o.arity().max() == 1 && o.arity().min() == 0) {
				text.append(" [").append(o.paramLabel()).append(']');
			}
			text.append(" : ").append(o.description()[0]).append('\n');
		}
		text.append("operands may also be attached to the flag: -tfile, -d10000\n");
		text.append("'--' ends flag processing so operands may begin with '-'\n");
		text.append("JVM property -Djls.toolkit=default|wayland overrides Wayland toolkit auto-selection\n");
		text.append("exit status: 0 success, 1 runtime failure, 2 usage error\n");
		text.append("example: jls -b -sstartup -d10000 counter.jls\n");
		return text.toString();
	}

	private static String operandWhat(String optionName) {
		for (Row r : ROWS) {
			if (("-" + r.flag).equals(optionName)) {
				return r.operandWhat;
			}
		}
		return "an";
	}

	/** Contract-format usage-error reporter: one line, exit 2. */
	private static void usageError(String message) {
		System.err.println("jls: error: " + message
				+ "; run 'jls -h' for usage");
		System.exit(2);
	}

	/** Maps picocli parse exceptions onto the pinned error wording. */
	private static final class ContractExceptionHandler
			implements IParameterExceptionHandler {
		public int handleParseException(ParameterException ex, String[] args) {
			String msg;
			java.util.regex.Matcher expected = java.util.regex.Pattern
					.compile("Expected parameter for option '(-[^']+)'")
					.matcher(ex.getMessage() == null ? "" : ex.getMessage());
			if (expected.find()) {
				// an option-like token where an operand was required:
				// "Unknown option: '-q'; Expected parameter for option
				// '-t' but found '-q'" (getUnmatched() is empty here)
				String name = expected.group(1);
				msg = "option " + name + " requires "
						+ operandWhat(name) + " operand";
			}
			else if (ex instanceof UnmatchedArgumentException) {
				String tok = ((UnmatchedArgumentException) ex)
						.getUnmatched().get(0);
				if (tok.startsWith("-")) {
					msg = "unknown option " + tok;
				}
				else {
					msg = "arguments after circuit file not allowed: " + tok;
				}
			}
			else if (ex instanceof MissingParameterException) {
				java.util.List<ArgSpec> missing =
						((MissingParameterException) ex).getMissing();
				String name;
				if (!missing.isEmpty()) {
					name = ((OptionSpec) missing.get(0)).longestName();
				}
				else {
					// "Expected parameter for option '-t' but found '-q'"
					// carries no ArgSpec; recover the option from the text
					java.util.regex.Matcher m = java.util.regex.Pattern
							.compile("option '(-[^']+)'")
							.matcher(ex.getMessage());
					name = m.find() ? m.group(1) : "?";
				}
				msg = "option " + name + " requires "
						+ operandWhat(name) + " operand";
			}
			else {
				msg = ex.getMessage();
			}
			System.err.println("jls: error: " + msg
					+ "; run 'jls -h' for usage");
			return 2;
		}
	}

	private static String opt(String name) {
		OptionSpec o = SPEC.optionsMap().get(name);
		Object v = o.getValue();
		return v == null ? null : v.toString();
	}

	private static boolean flag(String name) {
		Object v = SPEC.optionsMap().get(name).getValue();
		return v != null && (Boolean) v;
	}

	/** Post-parse semantics: -h, validation, then dump parsed state. */
	private static Integer runParsed() {
		if (flag("-h")) {
			System.err.print(usageText());
			return 0;
		}
		String image = opt("-i");
		boolean imgexport = imageExportRequested;
		if (image != null && !isImageFileName(image)) {
			usageError("option -i output file must end in .png, .jpg, .jpeg or .svg: "
					+ image);
		}
		String d = opt("-d");
		long timeLimit = 0;
		if (d != null) {
			try {
				timeLimit = Long.parseLong(d);
			}
			catch (NumberFormatException ex) {
				usageError("time limit not an integer: " + d);
			}
			if (timeLimit <= 0) {
				usageError("option -d requires a positive integer time limit, got "
						+ d);
			}
		}
		String export = opt("-export");
		if (export != null) {
			String lower = export.toLowerCase(Locale.ROOT);
			if (!lower.endsWith(".v") && !lower.endsWith(".vhd")
					&& !lower.endsWith(".vhdl")) {
				usageError("option -export output file must end in .v, "
						+ ".vhd or .vhdl: " + export);
			}
		}
		String savetext = opt("-savetext");
		if (savetext != null
				&& (!savetext.endsWith(".jls") || !savetext
						.replaceAll("\\.jls$", "")
						.matches("[A-Za-z][A-Za-z0-9_]*"))) {
			usageError("option -savetext output file must be a .jls "
					+ "file named like a circuit (letters, digits and "
					+ "underscores, starting with a letter): " + savetext);
		}
		String circuit = null;
		for (PositionalParamSpec p : SPEC.positionalParameters()) {
			Object v = p.getValue();
			if (v != null) {
				circuit = v.toString();
			}
		}
		System.out.println("PARSED batch=" + flag("-b")
				+ " imgexport=" + imgexport
				+ " imageFile=" + image
				+ " circuit=" + circuit
				+ " testFile=" + opt("-t")
				+ " paramFile=" + opt("-s")
				+ " timeLimit=" + (d == null ? "null" : timeLimit)
				+ " printerP=" + opt("-p")
				+ " printerV=" + opt("-v")
				+ " printerR=" + opt("-r")
				+ " vcdFile=" + opt("-vcd")
				+ " exportFile=" + export
				+ " textSaveFile=" + savetext);
		return 0;
	}

	/**
	 * Spike entry point. Args mode "--emit-usage" prints the generated
	 * usage text on stdout, "--emit-bash-completion" the completion
	 * script; anything else is parsed as a jls command line.
	 *
	 * @param args the command line under test.
	 */
	public static void main(String[] args) {
		SPEC = buildSpec();
		CMD = new CommandLine(SPEC);
		CMD.setUnmatchedOptionsAllowedAsOptionParameters(false);
		CMD.setParameterExceptionHandler(new ContractExceptionHandler());
		if (args.length == 1 && args[0].equals("--emit-usage")) {
			System.out.print(usageText());
			return;
		}
		if (args.length == 1 && args[0].equals("--emit-bash-completion")) {
			System.out.print(picocli.AutoComplete.bash("jls", CMD));
			return;
		}
		// contract pre-scan: JLSStart rejects empty arguments anywhere
		// and a bare '-' before the end-of-flags marker
		boolean endOfFlags = false;
		for (String a : args) {
			if (a.isEmpty()) {
				usageError("empty argument");
			}
			if (a.equals("--")) {
				endOfFlags = true;
			}
			if (!endOfFlags && a.equals("-")) {
				usageError("bare '-' is not a valid option");
			}
		}
		System.exit(CMD.execute(preTokenize(args)));
	}

	/**
	 * Attached-operand pre-pass. picocli's POSIX clustering handles
	 * attached operands for single-character options (-tfile, -d10000)
	 * but resolves an attached operand on a multi-character option by
	 * the FIRST matching single character (-vcdout.vcd becomes -v with
	 * operand cdout.vcd), the exact issue-72 misparse the hand parser's
	 * longest-match rule prevents. This pass reproduces longest-match by
	 * splitting attached operands off multi-character flags, and marks
	 * an attached -i operand as unconditional for the lookahead
	 * consumer, matching the hand parser.
	 *
	 * @param args the raw command line.
	 *
	 * @return the command line with attached operands split into
	 *         separate tokens where picocli's model differs from the
	 *         contract.
	 */
	private static String[] preTokenize(String[] args) {
		java.util.List<String> out = new java.util.ArrayList<>();
		boolean endOfFlags = false;
		for (int i = 0; i < args.length; i += 1) {
			String arg = args[i];
			if (!endOfFlags && arg.equals("--")) {
				endOfFlags = true;
				out.add(arg);
				continue;
			}
			if (endOfFlags || arg.length() < 2 || arg.charAt(0) != '-') {
				out.add(arg);
				continue;
			}
			String body = arg.substring(1);
			Row match = null;
			for (Row r : ROWS) {
				if (body.startsWith(r.flag) && (match == null
						|| r.flag.length() > match.flag.length())) {
					match = r;
				}
			}
			String attached = match == null ? ""
					: body.substring(match.flag.length());
			if (match != null && !attached.isEmpty()) {
				if (match.arity == 0) {
					// no clustering in the contract: -bh, -bx are unknown
					// options as a whole, never -b plus more flags
					usageError("unknown option " + arg);
				}
				if (match.arity == 2) {
					imageOperandForced = true;
				}
				// '=' attachment, not a separate token, so operands that
				// begin with '-' (e.g. -d-5) survive the rewrite
				out.add("-" + match.flag + "=" + attached);
			}
			else {
				out.add(arg);
			}
		}
		return out.toArray(new String[0]);
	}
}
```

## Appendix B — harness cases and results

Every `compare` case asserts identical exit status **and**
byte-identical stderr between the real CLI (headless
`java -cp target/classes jls.JLS`, `JAVA_TOOL_OPTIONS` stripped as the
contract tests do) and the spike. Every `spike_state` case asserts the
spike's parsed state reproduces the hand parser's semantics.

| # | Case | Kind | Result |
| --- | --- | --- | --- |
| 1 | `-h` help text and exit 0 | compare | PASS |
| 2 | `-x` unknown option | compare | PASS |
| 3 | `-t` trailing, no operand | compare | PASS |
| 4 | `-vcd` longest match, missing operand | compare | PASS |
| 5 | bare `-` | compare | PASS |
| 6 | empty argument `""` | compare | PASS |
| 7 | `-d0` zero time limit | compare | PASS |
| 8 | `-d-5` negative attached | compare | PASS |
| 9 | `-d 0` separated zero | compare | PASS |
| 10 | `-dabc` non-integer | compare | PASS |
| 11 | `-icircuit.txt` attached bad extension | compare | PASS |
| 12 | `-export foo.txt` bad extension | compare | PASS |
| 13 | `-savetext 9bad.jls` bad name | compare | PASS |
| 14 | `a.jls extra.jls` two positionals | compare | PASS |
| 15 | `-t -b` operand is a known flag | compare | PASS |
| 16 | `-t -q` operand is an unknown flag | compare | PASS |
| 17–20 | trailing `-s`, `-p`, `-export`, `-savetext` | compare | PASS |
| 21–32 | each of the 12 table flags accepted | spike | PASS |
| 33 | generated usage text byte-identical to real `-h` | compare | PASS |
| 34 | `-tfile` attached single-char | spike_state | PASS |
| 35 | `-d10000` attached numeric | spike_state | PASS |
| 36 | `-vcdout.vcd` attached multi-char (longest match) | spike_state | PASS |
| 37 | `-exportx.v` attached multi-char | spike_state | PASS |
| 38 | `-i out.svg` separated image operand consumed | spike_state | PASS |
| 39 | `-i c.jls` separated non-image NOT consumed | spike_state | PASS |
| 40 | `-i` alone sets image export with default file | spike_state | PASS |
| 41 | `-b -- -odd.jls` end-of-flags positional | spike_state | PASS |
| 42 | `c.jls -t t1` flags after positional | spike_state | PASS |
| 43 | `-iout.png` attached image operand | spike_state | PASS |
| 44 | `-savetextc.jls` attached | spike_state | PASS |
| 45 | `-bx` attached junk on arity-0 flag | compare | PASS |
| 46 | `-hx` attached junk on arity-0 flag | compare | PASS |

Total: **46 PASS, 0 DIFF.** Before the three fidelity shims were
added, the same harness reported 5 divergences: `-vcdout.vcd` silently
parsed as `-v cdout.vcd`, `-exportx.v` rejected as unknown,
`-icircuit.txt` reported `unknown option -circuit.txt`, `-i` alone not
registered as matched, and `-bx` reported as `unknown option -x`
instead of `-bx`; plus a handler crash on the empty-`getUnmatched()`
form of `UnmatchedArgumentException`. Those before/after deltas are
the evaluation's evidence, not incidental bugs.

## Appendix C — completion script facts

Generated via `picocli.AutoComplete.bash("jls", cmd)` from the spike's
model (picocli 4.7.7):

- 226 lines, self-contained bash function + `complete` registration;
  `bash -n` clean; zsh supported through `bashcompinit` (script
  handles it, per its own header).
- `flag_opts` = `'-h' '-b'`; `arg_opts` = `'-i' '-s' '-t' '-d' '-p'
  '-v' '-r' '-vcd' '-export' '-savetext'` — all twelve flags, exactly
  the table.
- Operand positions fall back to readline filename completion
  (`complete … -o default`); with `File`-typed option models picocli
  would emit explicit `compgen -f` per option.
- The script is static output — nothing in it requires picocli at
  runtime, which is why a generator over the existing `FLAGS` table
  (or a checked-in, golden-tested script) is the cheaper route to the
  same artifact.

## Version facts checked (2026-07-18)

| Fact | Value | Source |
| --- | --- | --- |
| Latest picocli release | 4.7.7 | Maven Central `maven-metadata.xml` |
| Release date | 2025-04-19 | jar/manifest timestamps, metadata `lastUpdated` 20250419 |
| Jar size | 417,640 bytes (229 classes) | downloaded artifact |
| JPMS | `META-INF/versions/9/module-info.class` (multi-release) | jar listing |
| License | Apache-2.0 | project metadata (GPLv3-compatible, ground rule 1) |
| Vendoring unit | `CommandLine.java` 1,238,820 bytes + `AutoComplete.java` 55,057 bytes | `picocli-4.7.7-sources.jar` |
