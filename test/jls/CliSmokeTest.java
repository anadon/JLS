package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end CLI contract smoke tests (issue #42, audit findings M4/M5):
 * every malformed invocation must terminate with a one-line diagnostic
 * on stderr and the contract's exit status - exit 2 for usage errors,
 * exit 1 for runtime failures - and never a stack trace, crash file, or
 * headless dialog attempt.
 */
class CliSmokeTest {

	@TempDir
	Path tmp;

	private static final class Result {
		final int exit;
		final String stderr;

		Result(int exit, String stderr) {
			this.exit = exit;
			this.stderr = stderr;
		}
	}

	private Result run(String... args) throws Exception {
		String java = System.getProperty("java.home")
				+ File.separator + "bin" + File.separator + "java";
		List<String> cmd = new ArrayList<>();
		cmd.add(java);
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("jls.JLS");
		for (String a : args) {
			cmd.add(a);
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(tmp.toFile());
		pb.environment().remove("JAVA_TOOL_OPTIONS");
		Process p = pb.start();
		p.getOutputStream().close();
		String stderr = drain(p.getErrorStream());
		drain(p.getInputStream());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "CLI run timed out");
		return new Result(p.exitValue(), stderr);
	}

	private static String drain(InputStream in) throws Exception {
		return new String(in.readAllBytes(), StandardCharsets.UTF_8);
	}

	private void assertNoCrashFile() {
		assertFalse(new File(tmp.toFile(), "JLSerror").exists(),
				"a user error must never produce a crash file");
	}

	@Test
	void trailingFlagWithoutOperandIsAUsageError() throws Exception {
		Result r = run("-t");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error: option -t requires"),
				r.stderr);
		assertNoCrashFile();
	}

	@Test
	void bareDashIsAUsageError() throws Exception {
		Result r = run("-");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertNoCrashFile();
	}

	@Test
	void emptyArgumentIsAUsageError() throws Exception {
		Result r = run("");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertNoCrashFile();
	}

	@Test
	void unknownFlagIsAUsageError() throws Exception {
		Result r = run("-x");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error: unknown option -x"),
				r.stderr);
		assertNoCrashFile();
	}

	@Test
	void batchWithMissingFileIsARuntimeError() throws Exception {
		Result r = run("-b", "nosuch.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertFalse(r.stderr.contains("Exception"),
				"no stack trace on a user error: " + r.stderr);
		assertNoCrashFile();
	}

	@Test
	void batchWithInvalidFileIsARuntimeError() throws Exception {
		File bad = new File(tmp.toFile(), "bad.jls");
		java.nio.file.Files.writeString(bad.toPath(), "this is not a circuit");
		Result r = run("-b", "bad.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertNoCrashFile();
	}
}
