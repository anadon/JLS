package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests of the headless plain-text re-save (issue #129):
 * -savetext loads a circuit file in any container and rewrites it as
 * uncompressed circuit text, so upstream-authored files can be opened by
 * fork lineages without an XZ reader and diffed under version control.
 * The output is validated as a real circuit file: it must reload to an
 * equivalent circuit, and re-converting it must not change its content.
 */
class CliTextSaveTest {

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

	/**
	 * A minimal circuit in the on-disk text form. Wire-free, so content
	 * comparisons can use FileFormatSupport.canonicalize (ref lines
	 * change with the unstable save-time ids).
	 */
	private static String circuitText(String name) {
		return "CIRCUIT " + name + "\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " Int value 5\n int base 10\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT Constant\n"
				+ " int id 1\n int x 120\n int y 96\n"
				+ " int width 24\n int height 24\n"
				+ " Int value 255\n int base 16\n"
				+ " String orient \"LEFT\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n";
	}

	@Test
	void anXZSaveIsRewrittenAsPlainText() throws Exception {
		// the input is what a default (XZ) save produces -- exactly the
		// file the fork lineage cannot open (issue #129, P1)
		FileFormatSupport.writeXZ(tmp.resolve("in.jls").toFile(),
				circuitText("in"));

		Result r = run("-savetext", "out.jls", "in.jls");
		assertEquals(0, r.exit, r.stderr);

		byte[] out = Files.readAllBytes(tmp.resolve("out.jls"));
		assertFalse(out.length >= 1 && (out[0] & 0xFF) == 0xFD,
				"the output must not be an XZ stream");
		String text = new String(out, StandardCharsets.UTF_8);
		// writers declare the minimal version the circuit needs
		// (Circuit.formatVersionNeeded()), and this fixture uses no
		// version-2 features, so the header must say 1 even while
		// Circuit.FORMAT_VERSION is newer
		assertTrue(text.startsWith("FORMAT 1\nCIRCUIT out\n"),
				"the output must be bare circuit text named after the "
						+ "output file, got: "
						+ text.substring(0, Math.min(40, text.length())));
		assertTrue(text.endsWith("ENDCIRCUIT\n"), "truncated output");
	}

	@Test
	void theConvertedFileHoldsTheSameCircuit() throws Exception {
		// same base name in a subdirectory, so the input text and the
		// converted output are comparable including the CIRCUIT line
		Files.createDirectory(tmp.resolve("sub"));
		FileFormatSupport.writeXZ(tmp.resolve("sub/in.jls").toFile(),
				circuitText("in"));

		Result r = run("-savetext", "in.jls", "sub/in.jls");
		assertEquals(0, r.exit, r.stderr);

		assertEquals(FileFormatSupport.canonicalize(circuitText("in")),
				FileFormatSupport.canonicalize(
						Files.readString(tmp.resolve("in.jls"))),
				"the plain-text save must hold the identical circuit");

		// and it reloads through the real sniffer/loader stack
		Scanner input = FileAbstractor.openCircuit(
				tmp.resolve("in.jls").toString());
		assertTrue(input != null, () -> "converted file must reopen: "
				+ JLSInfo.loadError);
		Circuit circ = new Circuit("in");
		assertTrue(circ.load(input), () -> "load failed: " + JLSInfo.loadError);
		input.close();
		assertTrue(circ.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		assertEquals(2, circ.getElements().size());
	}

	@Test
	void reConvertingThePlainTextOutputChangesNothing() throws Exception {
		FileFormatSupport.writeXZ(tmp.resolve("in.jls").toFile(),
				circuitText("in"));
		assertEquals(0, run("-savetext", "out.jls", "in.jls").exit);
		String first = Files.readString(tmp.resolve("out.jls"));

		// converting the output onto itself must preserve the content
		// (canonicalized: element order and ids are not stable per load)
		Result r = run("-savetext", "out.jls", "out.jls");
		assertEquals(0, r.exit, r.stderr);
		assertEquals(FileFormatSupport.canonicalize(first),
				FileFormatSupport.canonicalize(
						Files.readString(tmp.resolve("out.jls"))),
				"plain-text re-save of a plain-text save must hold the "
						+ "same circuit");
	}

	@Test
	void attachedOperandWinsOverTheDashSPrefix() throws Exception {
		// "-savetextout.jls" must resolve to -savetext with the attached
		// operand, never to -s with the operand "avetextout.jls"
		FileFormatSupport.writeXZ(tmp.resolve("in.jls").toFile(),
				circuitText("in"));
		Result r = run("-savetextout.jls", "in.jls");
		assertEquals(0, r.exit, r.stderr);
		assertTrue(Files.exists(tmp.resolve("out.jls")),
				"the attached operand must name the output file");
	}

	@Test
	void missingOperandIsAUsageError() throws Exception {
		Result r = run("-savetext");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("option -savetext requires"), r.stderr);
	}

	@Test
	void nonJlsOutputIsAUsageError() throws Exception {
		FileFormatSupport.writeXZ(tmp.resolve("in.jls").toFile(),
				circuitText("in"));
		Result r = run("-savetext", "out.txt", "in.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("-savetext"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.txt")));
	}

	@Test
	void invalidCircuitNameOutputIsAUsageError() throws Exception {
		// the output must reopen later, so it obeys circuit-file naming
		FileFormatSupport.writeXZ(tmp.resolve("in.jls").toFile(),
				circuitText("in"));
		Result r = run("-savetext", "9bad.jls", "in.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("-savetext"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("9bad.jls")));
	}

	@Test
	void withoutACircuitFileIsARuntimeError() throws Exception {
		Result r = run("-savetext", "out.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
	}
}
