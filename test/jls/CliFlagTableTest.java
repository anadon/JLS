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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Documentation drift tests (issue #71): the flag table in JLSStart is
 * the single authoritative CLI specification, usage() is generated from
 * it, and the parser accepts exactly the flags it lists. These tests
 * fail if anyone reverts to a hand-maintained flag list on either side.
 */
class CliFlagTableTest {

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

	private static Set<Character> tableFlags() {
		Set<Character> flags = new TreeSet<>();
		for (char f : JLSStart.commandLineFlags()) {
			flags.add(f);
		}
		return flags;
	}

	@Test
	void usageDocumentsExactlyTheParserFlags() {
		Set<Character> documented = new TreeSet<>();
		for (String line : JLSStart.usageText().split("\n")) {
			if (line.startsWith("  -")) {
				documented.add(line.charAt(3));
			}
		}
		assertEquals(tableFlags(), documented,
				"usage() and the parser must document the same flags");
	}

	@Test
	void helpPrintsTheGeneratedUsageAndExitsZero() throws Exception {
		Result r = run("-h");
		assertEquals(0, r.exit, r.stderr);
		assertEquals(JLSStart.usageText(), r.stderr,
				"-h must print exactly the table-generated usage text");
	}

	@Test
	void everyTableFlagIsAcceptedByTheParser() throws Exception {
		for (char flag : tableFlags()) {
			Result r = run("-" + flag);
			assertFalse(r.stderr.contains("unknown option"),
					"-" + flag + " is in the flag table but the parser "
							+ "rejected it: " + r.stderr);
		}
	}

	@Test
	void aFlagOutsideTheTableIsRejectedByName() throws Exception {
		Set<Character> table = tableFlags();
		char unknown = 0;
		for (char c = 'a'; c <= 'z'; c++) {
			if (!table.contains(c)) {
				unknown = c;
				break;
			}
		}
		assertTrue(unknown != 0, "no free flag letter left to test with");
		Result r = run("-" + unknown);
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("unknown option -" + unknown), r.stderr);
	}
}
