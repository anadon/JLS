package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests of headless CLI image export (issue #71): -i writes
 * PNG by default to circuit_file.png, an explicit operand chooses the
 * output path, the format follows the operand's extension (.jpg/.jpeg
 * for JPEG), and an unsupported extension is a usage error. The tests
 * assert file existence, size, container magic, and that the image
 * decodes with plausible dimensions - deliberately no pixel goldens,
 * which are brittle across JDK font rendering. -circuit selects a
 * subcircuit by dotted path; SVG text labels identify which level was
 * drawn.
 */
class CliImageExportTest {

	@TempDir
	Path tmp;

	/** Immutable capture of a finished CLI run: its process exit code and stderr text. */
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

	/**
	 * Write a minimal valid circuit (a constant wired to an output pin)
	 * in the on-disk text format, exactly as the editor saves it.
	 */
	private void writeCircuit(String fileName) throws Exception {
		String text = "CIRCUIT export\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT OutputPin\n"
				+ " int id 1\n int x 480\n int y 120\n"
				+ " int width 48\n int height 24\n"
				+ " String name \"out\"\n int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 2\n int x 24\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 3\n int x 36\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"input\"\n ref attach 1\n ref wire 2\n"
				+ "END\n"
				+ "ENDCIRCUIT\n";
		Files.writeString(tmp.resolve(fileName), text, StandardCharsets.UTF_8);
	}

	/**
	 * The body of a one-level circuit (a constant wired to an output
	 * pin named pin), without its ENDCIRCUIT line, so a caller can
	 * append SubCircuit elements before closing it.
	 */
	private static String levelBody(String name, String pin) {
		return "CIRCUIT " + name + "\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 60\n"
				+ " int width 24\n int height 24\n"
				+ " Int value 1\n int base 10\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT OutputPin\n"
				+ " int id 1\n int x 480\n int y 120\n"
				+ " int width 48\n int height 24\n"
				+ " String name \"" + pin + "\"\n int bits 1\n int watch 0\n"
				+ " String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 2\n int x 24\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"output\"\n ref attach 0\n ref wire 3\n"
				+ "END\n"
				+ "ELEMENT WireEnd\n"
				+ " int id 3\n int x 36\n int y 240\n"
				+ " int width 8\n int height 8\n"
				+ " String put \"input\"\n ref attach 1\n ref wire 2\n"
				+ "END\n";
	}

	/** A SubCircuit element wrapping the (unclosed) circuit body. */
	private static String subCircuit(int id, String body) {
		return "ELEMENT SubCircuit\n"
				+ " String orient \"RIGHT\"\n"
				+ " int id " + id + "\n int x 300\n int y 300\n"
				+ " int width 60\n int height 40\n"
				+ body
				+ "ENDCIRCUIT\n"
				+ "END\n";
	}

	/**
	 * Write a three-level circuit: top level cpu (output pin "out")
	 * holds subcircuit alu (pin "r"), which holds subcircuit inner
	 * (pin "q"). Each level's pin name identifies it in SVG output.
	 */
	private void writeNestedCircuit(String fileName) throws Exception {
		final String inner = levelBody("inner", "q");
		final String alu = levelBody("alu", "r") + subCircuit(10, inner);
		final String text = levelBody("cpu", "out") + subCircuit(20, alu)
				+ "ENDCIRCUIT\n";
		Files.writeString(tmp.resolve(fileName), text, StandardCharsets.UTF_8);
	}

	/** The SVG file's text labels, e.g. ">r<" for a pin named r. */
	private String svgText(String fileName) throws Exception {
		final File file = new File(tmp.toFile(), fileName);
		assertTrue(file.exists(), fileName + " was not written");
		return Files.readString(file.toPath(), StandardCharsets.UTF_8);
	}

	/** Assert the file exists, has the given magic bytes, and decodes. */
	private void assertDecodableImage(String fileName, int... magic)
			throws Exception {
		File file = new File(tmp.toFile(), fileName);
		assertTrue(file.exists(), fileName + " was not written");
		byte[] bytes = Files.readAllBytes(file.toPath());
		assertTrue(bytes.length > magic.length, fileName + " is empty");
		for (int i = 0; i < magic.length; i++) {
			assertEquals((byte) magic[i], bytes[i],
					fileName + " magic byte " + i);
		}
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
		assertNotNull(image, fileName + " does not decode as an image");
		assertTrue(image.getWidth() > 0 && image.getHeight() > 0,
				fileName + " has implausible dimensions");
	}

	@Test
	void defaultExportIsPngNamedAfterTheCircuit() throws Exception {
		writeCircuit("export.jls");
		Result r = run("-i", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertDecodableImage("export.png", 0x89, 'P', 'N', 'G');
	}

	@Test
	void explicitOperandChoosesPathAndJpegFollowsTheExtension() throws Exception {
		writeCircuit("export.jls");
		Result r = run("-i", "shot.jpg", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertDecodableImage("shot.jpg", 0xFF, 0xD8);
		assertFalse(new File(tmp.toFile(), "export.png").exists(),
				"the default output must not be written when a path is given");
	}

	@Test
	void attachedOperandIsAcceptedLikeOtherFlags() throws Exception {
		writeCircuit("export.jls");
		Result r = run("-ishot.png", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertDecodableImage("shot.png", 0x89, 'P', 'N', 'G');
	}

	@Test
	void svgOperandProducesAVectorImage() throws Exception {
		// vector export (issue #154): same paint path, SVG output
		writeCircuit("export.jls");
		Result r = run("-i", "shot.svg", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		File file = new File(tmp.toFile(), "shot.svg");
		assertTrue(file.exists(), "shot.svg was not written");
		String svg = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		assertTrue(svg.contains("<svg"), "no <svg element in output");
		assertTrue(svg.contains("</svg>"), "unterminated SVG document");
		assertFalse(new File(tmp.toFile(), "export.png").exists(),
				"the default output must not be written when a path is given");
	}

	@Test
	void unsupportedImageExtensionIsAUsageError() throws Exception {
		writeCircuit("export.jls");
		Result r = run("-ishot.bmp", "export.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertTrue(r.stderr.contains("-i"), r.stderr);
		assertFalse(new File(tmp.toFile(), "shot.bmp").exists());
	}

	@Test
	void imageExportWithoutACircuitFileIsARuntimeError() throws Exception {
		Result r = run("-i");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
	}

	@Test
	void doubleDashBeforeTheCircuitFileStillExports() throws Exception {
		writeCircuit("export.jls");
		Result r = run("-i", "--", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertDecodableImage("export.png", 0x89, 'P', 'N', 'G');
	}

	@Test
	void circuitOptionExportsTheNamedSubcircuitWithADerivedDefaultName()
			throws Exception {
		writeNestedCircuit("cpu.jls");
		final Result r = run("-i", "-circuit", "alu", "cpu.jls");
		assertEquals(0, r.exit, r.stderr);
		assertDecodableImage("cpu-alu.png", 0x89, 'P', 'N', 'G');
		assertFalse(new File(tmp.toFile(), "cpu.png").exists(),
				"the top-level default must not be written for -circuit");
	}

	@Test
	void circuitOptionDrawsOnlyTheNamedLevel() throws Exception {
		writeNestedCircuit("cpu.jls");
		final Result r = run("-i", "alu.svg", "-circuit", "alu", "cpu.jls");
		assertEquals(0, r.exit, r.stderr);
		final String svg = svgText("alu.svg");
		assertTrue(svg.contains(">r<"), "alu's own pin is missing");
		assertTrue(svg.contains(">inner<"), "alu's subcircuit block is missing");
		assertFalse(svg.contains(">out<"), "the top level was drawn");
	}

	@Test
	void circuitOptionFollowsADottedPathIntoNestedSubcircuits()
			throws Exception {
		writeNestedCircuit("cpu.jls");
		final Result r = run("-i", "inner.svg", "-circuitalu.inner", "cpu.jls");
		assertEquals(0, r.exit, r.stderr);
		final String svg = svgText("inner.svg");
		assertTrue(svg.contains(">q<"), "inner's own pin is missing");
		assertFalse(svg.contains(">r<"), "the alu level was drawn");
	}

	@Test
	void circuitOptionNamingNoSubcircuitIsARuntimeError() throws Exception {
		writeNestedCircuit("cpu.jls");
		// each miss lists the subcircuits at the deepest level the path
		// reached; inner exists, but only inside alu - paths start at
		// the top
		final String[][] cases = {
			{"nope", "top-level subcircuits: alu"},
			{"inner", "top-level subcircuits: alu"},
			{"alu.nope", "subcircuits in alu: inner"},
			{"alu.nope.deeper", "subcircuits in alu: inner"},
			{"alu.inner.nope", "alu.inner has no subcircuits"},
		};
		for (String[] c : cases) {
			final Result r = run("-i", "-circuit", c[0], "cpu.jls");
			assertEquals(1, r.exit, c[0] + ": " + r.stderr);
			assertTrue(r.stderr.contains("has no subcircuit " + c[0]
					+ "; " + c[1]), r.stderr);
		}
		assertFalse(new File(tmp.toFile(), "cpu-nope.png").exists());
	}

	@Test
	void subcircuitListingSortsNumberedNamesNaturally() throws Exception {
		// group1 holds HalfAdder2, HalfAdder10, HalfAdder1 (file order)
		final String group1 = levelBody("group1", "g")
				+ subCircuit(10, levelBody("HalfAdder2", "a"))
				+ subCircuit(11, levelBody("HalfAdder10", "b"))
				+ subCircuit(12, levelBody("HalfAdder1", "c"));
		Files.writeString(tmp.resolve("top.jls"),
				levelBody("top", "out") + subCircuit(20, group1)
						+ "ENDCIRCUIT\n",
				StandardCharsets.UTF_8);
		final Result r = run("-i", "-circuit", "group1.HalfAdder3", "top.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("has no subcircuit group1.HalfAdder3; "
				+ "subcircuits in group1: HalfAdder1, HalfAdder2, HalfAdder10"),
				r.stderr);
	}

	@Test
	void malformedCircuitPathIsAUsageError() throws Exception {
		writeNestedCircuit("cpu.jls");
		for (String path : new String[] {"alu..inner", "alu.", "9alu"}) {
			final Result r = run("-i", "-circuit", path, "cpu.jls");
			assertEquals(2, r.exit, path + ": " + r.stderr);
			assertTrue(r.stderr.contains(
					"option -circuit requires a subcircuit name"), r.stderr);
		}
	}

	@Test
	void circuitOptionWithoutImageExportIsAUsageError() throws Exception {
		writeNestedCircuit("cpu.jls");
		final Result r = run("-b", "-circuit", "alu", "cpu.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("option -circuit requires -i"),
				r.stderr);
	}

	@Test
	void batchRunAcceptsAPositiveTimeLimit() throws Exception {
		// positive control for the -d validation (issue #71): a legal
		// time limit must still be accepted in both operand forms
		writeCircuit("export.jls");
		Result r = run("-b", "-d", "5000", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		r = run("-b", "-d5000", "export.jls");
		assertEquals(0, r.exit, r.stderr);
	}
}
