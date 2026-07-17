package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Seeded byte-mutation fuzzing of the three on-disk container formats
 * (issue #160). The hostile-input suite (issue #38) proves the loader
 * rejects a handful of hand-built attacks; this harness asserts the
 * broader contract from the LoadError taxonomy (issue #58) at
 * pseudo-random points: whatever bytes are in the file, the
 * format-sniffing open and the circuit load either succeed or classify
 * the failure - they never let an exception escape, and every
 * rejection publishes a structured LoadError.
 *
 * Deterministic seeds: failures must reproduce. Dependency-free by
 * decision - jqwik was rejected under the active-maintenance policy
 * (docs/library-survey-2026-07.md).
 */
class ContainerMutationFuzzTest {

	/** Mutants per container format. */
	private static final int MUTANTS = 150;

	@TempDir
	Path tmp;

	/** A small but representative circuit: elements, a wire, escapes. */
	private static final String CIRCUIT_TEXT = "CIRCUIT mutate\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n"
			+ " int width 24\n int height 24\n"
			+ " Int value 255\n int base 16\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT Text\n"
			+ " int id 1\n int x 60\n int y 120\n"
			+ " int width 24\n int height 24\n"
			+ " String text \"esc \\\\ \\\" \\n done\"\n"
			+ "END\n"
			+ "ELEMENT InputPin\n"
			+ " int id 2\n int x 120\n int y 240\n"
			+ " int width 48\n int height 24\n"
			+ " String name \"src\"\n int bits 4\n int watch 0\n"
			+ " String orient \"RIGHT\"\n"
			+ "END\n"
			+ "ELEMENT OutputPin\n"
			+ " int id 3\n int x 480\n int y 240\n"
			+ " int width 48\n int height 24\n"
			+ " String name \"dst\"\n int bits 4\n int watch 0\n"
			+ " String orient \"LEFT\"\n"
			+ "END\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 4\n int x 168\n int y 252\n"
			+ " int width 8\n int height 8\n"
			+ " String put \"output\"\n ref attach 2\n ref wire 5\n"
			+ "END\n"
			+ "ELEMENT WireEnd\n"
			+ " int id 5\n int x 480\n int y 252\n"
			+ " int width 8\n int height 8\n"
			+ " String put \"input\"\n ref attach 3\n ref wire 4\n"
			+ "END\n"
			+ "ENDCIRCUIT\n";

	/** Apply one random byte-level mutation. */
	private static byte[] mutate(byte[] original, Random rng) {
		byte[] bytes = Arrays.copyOf(original, original.length);
		switch (rng.nextInt(5)) {
		case 0: { // flip one bit
			int at = rng.nextInt(bytes.length);
			bytes[at] ^= (byte) (1 << rng.nextInt(8));
			return bytes;
		}
		case 1: { // overwrite one byte with a random value
			bytes[rng.nextInt(bytes.length)] = (byte) rng.nextInt(256);
			return bytes;
		}
		case 2: { // truncate at a random point
			return Arrays.copyOf(bytes, rng.nextInt(bytes.length));
		}
		case 3: { // insert a short burst of random bytes
			int at = rng.nextInt(bytes.length);
			byte[] burst = new byte[1 + rng.nextInt(16)];
			rng.nextBytes(burst);
			byte[] out = new byte[bytes.length + burst.length];
			System.arraycopy(bytes, 0, out, 0, at);
			System.arraycopy(burst, 0, out, at, burst.length);
			System.arraycopy(bytes, at, out, at + burst.length,
					bytes.length - at);
			return out;
		}
		default: { // duplicate a random region over another
			int from = rng.nextInt(bytes.length);
			int to = rng.nextInt(bytes.length);
			int len = Math.min(1 + rng.nextInt(64),
					bytes.length - Math.max(from, to));
			System.arraycopy(bytes, from, bytes, to, len);
			return bytes;
		}
		}
	}

	/**
	 * Open, load and finish-load a mutant. Success and classified
	 * rejection are both legal; an escaped exception or a rejection
	 * without a structured LoadError fails the run.
	 */
	private void loadMutant(File file, String label) {
		Scanner scanner;
		try {
			scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		} catch (RuntimeException | Error e) {
			throw new AssertionError(label
					+ ": openCircuit threw instead of classifying", e);
		}
		if (scanner == null) {
			assertTrue(JLSInfo.lastLoadError != null,
					label + ": a rejected open must publish a structured"
					+ " LoadError");
			return;
		}
		Circuit circuit = new Circuit("mutate");
		boolean ok;
		try {
			ok = circuit.load(scanner);
		} catch (RuntimeException | Error e) {
			throw new AssertionError(label
					+ ": load threw instead of classifying", e);
		} finally {
			scanner.close();
		}
		if (!ok) {
			assertTrue(JLSInfo.lastLoadError != null,
					label + ": a rejected load must publish a structured"
					+ " LoadError");
			return;
		}
		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			ok = circuit.finishLoad(g);
		} catch (Exception | Error e) {
			throw new AssertionError(label
					+ ": finishLoad threw instead of classifying", e);
		} finally {
			g.dispose();
		}
		if (!ok) {
			assertTrue(JLSInfo.lastLoadError != null,
					label + ": a rejected finishLoad must publish a"
					+ " structured LoadError");
		}
	}

	private void fuzzContainer(String container, byte[] valid)
			throws Exception {
		Random rng = new Random(container.hashCode());
		for (int i = 0; i < MUTANTS; i++) {
			byte[] mutant = mutate(valid, rng);
			File file = tmp.resolve("m" + i + ".jls").toFile();
			Files.write(file.toPath(), mutant);
			loadMutant(file, container + " mutant " + i
					+ " (seed " + container.hashCode() + ")");
		}
	}

	@Test
	void mutatedTextContainersAreClassifiedNeverThrown() throws Exception {
		File valid = tmp.resolve("valid.jls").toFile();
		FileFormatSupport.writeText(valid, CIRCUIT_TEXT);
		fuzzContainer("text", Files.readAllBytes(valid.toPath()));
	}

	@Test
	void mutatedZipContainersAreClassifiedNeverThrown() throws Exception {
		File valid = tmp.resolve("valid.jls").toFile();
		FileFormatSupport.writeZip(valid, CIRCUIT_TEXT);
		fuzzContainer("zip", Files.readAllBytes(valid.toPath()));
	}

	@Test
	void mutatedXzContainersAreClassifiedNeverThrown() throws Exception {
		File valid = tmp.resolve("valid.jls").toFile();
		FileFormatSupport.writeXZ(valid, CIRCUIT_TEXT);
		fuzzContainer("xz", Files.readAllBytes(valid.toPath()));
	}

	@Test
	void theUnmutatedBaselinesActuallyLoad() throws Exception {
		// positive control: if the base circuit stopped loading, the
		// three properties above would pass vacuously
		File text = tmp.resolve("basetext.jls").toFile();
		FileFormatSupport.writeText(text, CIRCUIT_TEXT);
		File zip = tmp.resolve("basezip.jls").toFile();
		FileFormatSupport.writeZip(zip, CIRCUIT_TEXT);
		File xz = tmp.resolve("basexz.jls").toFile();
		FileFormatSupport.writeXZ(xz, CIRCUIT_TEXT);
		for (File file : new File[] {text, zip, xz}) {
			Scanner scanner = FileAbstractor.openCircuit(
					file.getAbsolutePath());
			assertTrue(scanner != null,
					file.getName() + ": " + JLSInfo.loadError);
			Circuit circuit = new Circuit("mutate");
			assertTrue(circuit.load(scanner),
					file.getName() + ": " + JLSInfo.loadError);
			scanner.close();
			BufferedImage img = new BufferedImage(64, 64,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			assertTrue(circuit.finishLoad(g),
					file.getName() + ": " + JLSInfo.loadError);
			g.dispose();
		}
	}
}
