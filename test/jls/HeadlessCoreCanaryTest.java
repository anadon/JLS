package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The runtime embedder canary for issue #77 P2. The bytecode rule
 * ({@code ArchitectureRulesTest.coreDependsOnNoGuiClasses}) proves the
 * headless core references no AWT/Swing/editor type; this test proves the
 * stronger runtime property that <em>loading and simulating a real circuit
 * loads none of those classes into the JVM</em> - the property a headless
 * embedder (a server, a CI grader, a batch grader) actually depends on.
 *
 * <p>It forks a fresh JVM - like {@link CliSmokeTest} - running the
 * headless round trip ({@link HeadlessCanaryMain}: load a {@code .jls},
 * finish the load with no graphics, run a {@link jls.sim.BatchSimulator},
 * print the settled values) under {@code -Xlog:class+load:file=...}, and
 * asserts the class-load log names no {@code java.awt.}, {@code
 * javax.swing.}, or {@code jls.edit.} class. Crucially the child runs with
 * {@code -Djava.awt.headless} UNSET: if any core path touched AWT it would
 * really load the toolkit here, so a clean log is a genuine guarantee, not
 * an artifact of headless mode swallowing the classes.
 */
class HeadlessCoreCanaryTest {

	@TempDir
	Path tmp;

	/** Class-name prefixes that must never appear in the child's load log. */
	private static final Pattern FORBIDDEN = Pattern.compile(
			"\\b(java\\.awt\\.|javax\\.swing\\.|jls\\.edit\\.)");

	/**
	 * Load and simulate a fixture in a fresh JVM and return the loaded
	 * classes that belong to AWT, Swing, or the editor (empty when clean).
	 */
	private List<String> guiClassesLoadedFor(String fixture) throws Exception {
		Path log = tmp.resolve("classload-"
				+ Path.of(fixture).getFileName() + ".log");
		Path circuit = Path.of(System.getProperty("user.dir"), fixture);
		assertTrue(Files.isRegularFile(circuit),
				"fixture not found: " + circuit
						+ " (run tests from the repository root)");

		String java = System.getProperty("java.home")
				+ File.separator + "bin" + File.separator + "java";
		List<String> cmd = new ArrayList<>();
		cmd.add(java);
		cmd.addAll(jls.CoverageAgent.jvmArgs());
		// NOTE: -Djava.awt.headless is deliberately NOT set - we want a
		// real toolkit load to show up if the core ever reaches for one.
		cmd.add("-Xlog:class+load:file=" + log.toAbsolutePath());
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("jls.HeadlessCanaryMain");
		cmd.add(circuit.toString());

		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(new File(System.getProperty("user.dir")));
		pb.environment().remove("JAVA_TOOL_OPTIONS");
		Process p = pb.start();
		p.getOutputStream().close();
		String stdout = drain(p.getInputStream());
		String stderr = drain(p.getErrorStream());
		assertTrue(p.waitFor(120, TimeUnit.SECONDS), "canary run timed out");
		assertEquals(0, p.exitValue(),
				"headless round trip failed: " + stderr);
		assertTrue(stdout.contains("settled:"),
				"canary did not report a settled value: " + stdout);

		List<String> classes = Files.readAllLines(log, StandardCharsets.UTF_8);
		return classes.stream()
				.filter(line -> FORBIDDEN.matcher(line).find())
				// strip the "[..s][info][class,load] " prefix and " source:.."
				.map(line -> line.replaceFirst("^.*class,load\\]\\s*", "")
						.replaceFirst("\\s+source:.*$", ""))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	@Test
	void smallGateAndPinsCircuitLoadsNoGuiClasses() throws Exception {
		List<String> gui =
				guiClassesLoadedFor("test/fixtures/headless-canary-gate.jls");
		assertTrue(gui.isEmpty(),
				"loading + simulating a headless circuit loaded GUI classes"
						+ " (issue #77): " + gui
						+ "\nthe headless core must not reach for AWT, Swing,"
						+ " or the editor at runtime");
	}

	@Test
	void realForkCircuitLoadsNoGuiClasses() throws Exception {
		List<String> gui = guiClassesLoadedFor(
				"test/fixtures/fork-4.6-shiftregister.jls");
		assertTrue(gui.isEmpty(),
				"loading + simulating a real circuit loaded GUI classes"
						+ " (issue #77): " + gui);
	}

	private static String drain(InputStream in) throws Exception {
		return new String(in.readAllBytes(), StandardCharsets.UTF_8);
	}
}
