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
		cmd.addAll(jls.CoverageAgent.jvmArgs());
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

	private static Set<String> tableFlags() {
		Set<String> flags = new TreeSet<>();
		for (String f : JLSStart.commandLineFlags()) {
			flags.add(f);
		}
		return flags;
	}

	@Test
	void usageDocumentsExactlyTheParserFlags() {
		Set<String> documented = new TreeSet<>();
		for (String line : JLSStart.usageText().split("\n")) {
			if (line.startsWith("  -")) {
				documented.add(line.substring(3).split(" ")[0]);
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
		for (String flag : tableFlags()) {
			Result r = run("-" + flag);
			assertFalse(r.stderr.contains("unknown option"),
					"-" + flag + " is in the flag table but the parser "
							+ "rejected it: " + r.stderr);
		}
	}

	@Test
	void longestFlagNameWinsOverSingleLetterPrefix() throws Exception {
		// "-vcd" must resolve to the VCD flag (here: missing its file
		// operand, a usage error), never to "-v" with the attached
		// printer name "cd" (issue #72)
		Result r = run("-vcd");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("option -vcd requires"), r.stderr);
	}

	@Test
	void aFlagOutsideTheTableIsRejectedByName() throws Exception {
		Set<String> table = tableFlags();
		String unknown = null;
		for (char c = 'a'; c <= 'z'; c++) {
			if (!table.contains(String.valueOf(c))) {
				unknown = String.valueOf(c);
				break;
			}
		}
		assertTrue(unknown != null, "no free flag letter left to test with");
		Result r = run("-" + unknown);
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("unknown option -" + unknown), r.stderr);
	}
}
