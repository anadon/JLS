package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Pins TellUser's non-interactive (batch/headless) contract for issue
 * #81: every notification method must write a single-line diagnostic to
 * stderr in the CLI contract's format (#42), leave stdout untouched
 * (stdout is reserved for simulation output), never attempt a dialog,
 * and the question/prompt methods must return their documented safe
 * defaults (confirm: no, confirmOrCancel: cancel, prompt: null).
 *
 * These defaults were audited per call site (issue #81 residue,
 * 2026-07-18): every confirm/confirmOrCancel/prompt call in the
 * application is inside a GUI gesture handler (menu action, mouse
 * handler, or editor close path) that batch mode never constructs, so
 * the safe defaults are unreachable belt-and-braces rather than silent
 * behavior changes - which is why they stay defaults instead of
 * becoming hard "jls: error:" exits. This test makes that decision
 * load-bearing: if a future batch path does reach one of these
 * methods, this is the behavior it gets.
 */
class TellUserBatchContractTest {

	/**
	 * The captured result of running one TellUser call in forced batch
	 * mode: everything the call wrote to the real stdout and stderr.
	 *
	 * @param out What the call wrote to System.out (must stay empty).
	 * @param err What the call wrote to System.err.
	 */
	private record Streams(String out, String err) {

		/**
		 * Assert the batch-mode invariants every TellUser method
		 * shares: stdout untouched, and exactly one stderr line
		 * starting with the given prefix.
		 *
		 * @param prefix The required start of the stderr line.
		 */
		void assertSingleStderrLine(String prefix) {
			assertEquals("", out,
					"stdout is reserved for simulation output (#42)");
			assertTrue(err.startsWith(prefix),
					"expected stderr to start with '" + prefix
							+ "' but was: " + err);
			String body = err.endsWith(System.lineSeparator())
					? err.substring(0, err.length()
							- System.lineSeparator().length())
					: err;
			assertFalse(body.contains("\n"),
					"diagnostic must be a single line: " + err);
		}
	}

	/**
	 * Run one TellUser call with batch mode forced on and both standard
	 * streams captured, restoring all global state afterwards.
	 *
	 * @param call The TellUser call to exercise.
	 *
	 * @return what the call wrote to stdout and stderr.
	 */
	private static Streams inBatch(Runnable call) {

		boolean savedBatch = JLSInfo.batch;
		PrintStream savedOut = System.out;
		PrintStream savedErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try {
			JLSInfo.batch = true;
			System.setOut(new PrintStream(out, true,
					StandardCharsets.UTF_8));
			System.setErr(new PrintStream(err, true,
					StandardCharsets.UTF_8));
			call.run();
		} finally {
			JLSInfo.batch = savedBatch;
			System.setOut(savedOut);
			System.setErr(savedErr);
		}
		return new Streams(out.toString(StandardCharsets.UTF_8),
				err.toString(StandardCharsets.UTF_8));
	}

	/** Batch info is a bare "jls:" line on stderr, not a dialog. */
	@Test
	void infoWritesJlsLineToStderr() {
		Streams s = inBatch(() ->
				TellUser.info(null, "circuit saved", "Info"));
		s.assertSingleStderrLine("jls: circuit saved");
	}

	/** Batch warn uses the CLI contract's "jls: warning:" prefix. */
	@Test
	void warnWritesWarningLineToStderr() {
		Streams s = inBatch(() ->
				TellUser.warn(null, "unterminated input", "WARNING"));
		s.assertSingleStderrLine("jls: warning: unterminated input");
	}

	/** Batch error uses the CLI contract's "jls: error:" prefix. */
	@Test
	void errorWritesErrorLineToStderr() {
		Streams s = inBatch(() ->
				TellUser.error(null, "cannot open file", "Error"));
		s.assertSingleStderrLine("jls: error: cannot open file");
	}

	/** An unaskable yes/no question answers no, and says so on stderr. */
	@Test
	void confirmDefaultsToNo() {
		boolean[] answer = new boolean[1];
		Streams s = inBatch(() -> answer[0] =
				TellUser.confirm(null, "Delete transition?", "option"));
		assertFalse(answer[0],
				"batch mode must never take the destructive branch");
		s.assertSingleStderrLine("jls: warning: Delete transition?");
	}

	/** An unaskable three-way question answers cancel, never yes/no. */
	@Test
	void confirmOrCancelDefaultsToCancel() {
		TellUser.Answer[] answer = new TellUser.Answer[1];
		Streams s = inBatch(() -> answer[0] = TellUser.confirmOrCancel(
				null, "Save this circuit?", "Option"));
		assertEquals(TellUser.Answer.CANCEL, answer[0],
				"an unanswerable question must cancel the operation");
		s.assertSingleStderrLine("jls: warning: Save this circuit?");
	}

	/** An impossible prompt returns null, the documented cancel value. */
	@Test
	void promptDefaultsToNull() {
		String[] answer = new String[1];
		Streams s = inBatch(() ->
				answer[0] = TellUser.prompt(null, "Name?"));
		assertNull(answer[0]);
		s.assertSingleStderrLine("jls: warning: Name?");
	}

	/** The initial-value prompt overload shares the same contract. */
	@Test
	void promptWithInitialValueDefaultsToNull() {
		String[] answer = new String[1];
		Streams s = inBatch(() ->
				answer[0] = TellUser.prompt(null, "File name?", "a.bin"));
		assertNull(answer[0]);
		s.assertSingleStderrLine("jls: warning: File name?");
	}
}
