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
 * which are brittle across JDK font rendering.
 */
class CliImageExportTest {

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
