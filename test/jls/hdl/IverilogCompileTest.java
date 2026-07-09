package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * External-simulator validation, Digital's CI pattern (issue #60):
 * when {@code iverilog} is installed, every committed golden export
 * must compile as Verilog-2005. Locally without the tool the test
 * SKIPS (JUnit assumption), so the suite stays green on machines
 * without an HDL toolchain; CI installs iverilog to arm it.
 * Simulation parity against JLS batch goldens is the next slice.
 */
class IverilogCompileTest {

	@TempDir
	Path tmp;

	@Test
	void everyGoldenCompilesUnderIverilog() throws Exception {
		String iverilog = findOnPath("iverilog");
		Assumptions.assumeTrue(iverilog != null,
				"iverilog not installed; skipping external compile check");

		List<Path> goldens = new ArrayList<>();
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(
				Path.of("test", "resources", "hdl"), "*.v")) {
			dir.forEach(goldens::add);
		}
		Collections.sort(goldens);
		assertEquals(false, goldens.isEmpty(), "no goldens found");

		for (Path golden : goldens) {
			// the @VERSION@ token lives in a comment; compile as-is
			Path out = tmp.resolve(golden.getFileName() + ".vvp");
			ProcessBuilder pb = new ProcessBuilder(iverilog, "-g2005",
					"-o", out.toString(),
					golden.toAbsolutePath().toString());
			pb.redirectErrorStream(true);
			Process p = pb.start();
			p.getOutputStream().close();
			String output = new String(p.getInputStream().readAllBytes(),
					StandardCharsets.UTF_8);
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				throw new AssertionError("iverilog timed out on " + golden);
			}
			assertEquals(0, p.exitValue(),
					golden + " does not compile under iverilog:\n" + output);
		}
	}

	/** Locate a tool on PATH, or null (never installs anything). */
	private static String findOnPath(String tool) {
		String path = System.getenv("PATH");
		if (path == null) {
			return null;
		}
		for (String dir : path.split(File.pathSeparator)) {
			if (dir.isEmpty()) {
				continue;
			}
			Path candidate = Path.of(dir, tool);
			try {
				if (Files.isExecutable(candidate)
						&& !Files.isDirectory(candidate)) {
					return candidate.toString();
				}
			} catch (SecurityException ignored) {
				// unreadable PATH entry: keep looking
			}
		}
		return null;
	}

} // end of IverilogCompileTest class
