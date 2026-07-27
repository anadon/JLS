package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * CI wiring for the example autograde bridge (issue #216):
 * {@code examples/autograde/autograde.py} must grade the committed
 * shift-register fixture green over the real batch CLI, exactly as
 * docs/vcd-interop.md tells an instructor to run it. The bridge is a
 * consume-the-outputs harness -- run {@code jls -b -vcd}, parse the
 * stdout report and the VCD -- and deliberately not co-simulation
 * (issue #63 rejected a live transport).
 *
 * <p>Toolchain gating follows the {@link jls.hdl.IverilogCompileTest}
 * pattern: without {@code python3} on PATH the test SKIPS via a JUnit
 * assumption, so the suite stays green on machines without Python;
 * CI's Ubuntu runners have python3 and arm it.
 */
class AutogradeBridgeExampleTest {

	@Test
	void bridgeExampleGradesTheFixtureGreen() throws Exception {
		String python = findOnPath("python3");
		Assumptions.assumeTrue(python != null,
				"python3 not installed; skipping autograde bridge example");

		Path script = Path.of("examples", "autograde", "autograde.py");
		assertTrue(Files.isRegularFile(script),
				"example bridge missing: " + script);

		String java = System.getProperty("java.home")
				+ File.separator + "bin" + File.separator + "java";
		List<String> cmd = new ArrayList<String>();
		cmd.add(python);
		cmd.add(script.toAbsolutePath().toString());
		// everything after -- is the JLS command the bridge drives:
		// this JVM's java + classpath, as the CLI suites spawn it
		cmd.add("--");
		cmd.add(java);
		cmd.addAll(jls.CoverageAgent.jvmArgs());
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("jls.JLS");

		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		pb.environment().remove("JAVA_TOOL_OPTIONS");
		Process p = pb.start();
		p.getOutputStream().close();
		String output = new String(p.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
		if (!p.waitFor(180, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			throw new AssertionError("autograde bridge timed out:\n"
					+ output);
		}
		assertEquals(0, p.exitValue(),
				"autograde bridge failed:\n" + output);
		assertTrue(output.contains("PASS"),
				"bridge exited 0 without reporting PASS:\n" + output);
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

} // end of AutogradeBridgeExampleTest class
