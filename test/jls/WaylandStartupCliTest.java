package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end CLI tests for the startup toolkit policy (issue #105),
 * following CliSmokeTest's forked-JVM pattern but with control over the
 * child's WAYLAND_DISPLAY/DISPLAY environment and without forcing
 * java.awt.headless, so the policy actually runs.
 *
 * GUI-intent runs only ever happen where the outcome is a guaranteed
 * process exit: either the policy's single error line (stock JDK,
 * simulated Wayland-only session) or the legacy HeadlessException path
 * (DISPLAY unset means stock OpenJDK on Linux auto-detects headless).
 * Both are guarded by assumptions (Linux, no WLToolkit in the test JDK)
 * so a JBR or a macOS dev machine never pops a real window.
 */
class WaylandStartupCliTest {

	@TempDir
	Path tmp;

	private static final class Result {
		final int exit;
		final String stdout;
		final String stderr;

		Result(int exit, String stdout, String stderr) {
			this.exit = exit;
			this.stdout = stdout;
			this.stderr = stderr;
		}
	}

	/**
	 * Fork a JLS run with a controlled display environment.
	 *
	 * @param waylandDisplay child's WAYLAND_DISPLAY, or null to unset.
	 * @param display        child's DISPLAY, or null to unset.
	 * @param jvmFlags       extra -D flags (never java.awt.headless=true
	 *                       here: the policy must see a GUI-capable run
	 *                       unless a test passes it explicitly).
	 * @param args           JLS command-line arguments.
	 */
	private Result run(String waylandDisplay, String display,
			List<String> jvmFlags, String... args) throws Exception {
		String java = System.getProperty("java.home")
				+ File.separator + "bin" + File.separator + "java";
		List<String> cmd = new ArrayList<>();
		cmd.add(java);
		cmd.addAll(jls.CoverageAgent.jvmArgs());
		cmd.addAll(jvmFlags);
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("jls.JLS");
		for (String a : args) {
			cmd.add(a);
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(tmp.toFile());
		pb.environment().remove("JAVA_TOOL_OPTIONS");
		pb.environment().remove("WAYLAND_DISPLAY");
		pb.environment().remove("DISPLAY");
		if (waylandDisplay != null) {
			pb.environment().put("WAYLAND_DISPLAY", waylandDisplay);
		}
		if (display != null) {
			pb.environment().put("DISPLAY", display);
		}
		Process p = pb.start();
		p.getOutputStream().close();
		String stderr = drain(p.getErrorStream());
		String stdout = drain(p.getInputStream());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "CLI run timed out");
		return new Result(p.exitValue(), stdout, stderr);
	}

	private static String drain(InputStream in) throws Exception {
		return new String(in.readAllBytes(), StandardCharsets.UTF_8);
	}

	private static void assumeLinux() {
		String os = System.getProperty("os.name", "");
		assumeTrue(os.toLowerCase(Locale.ROOT).contains("linux"),
				"policy GUI paths are only exercised on Linux");
	}

	private static void assumeStockJdk() {
		assumeFalse(ToolkitPolicy.runtimeHasWlToolkit(),
				"test JDK ships WLToolkit; the no-toolkit error path "
						+ "cannot be exercised on it");
	}

	@Test
	void waylandOnlySessionWithoutWlToolkitPrintsOneActionableLine()
			throws Exception {
		assumeLinux();
		assumeStockJdk();
		Result r = run("wayland-9", null, List.of());
		assertEquals(1, r.exit, r.stderr);
		// exactly one stderr line, per the CLI contract (#42/#105 P1)
		assertTrue(r.stderr.startsWith("jls: error: "), r.stderr);
		assertEquals(1, r.stderr.strip().split("\n").length, r.stderr);
		assertTrue(r.stderr.contains("DISPLAY"), r.stderr);
		assertTrue(r.stderr.contains("JBR")
				|| r.stderr.contains("Wakefield"), r.stderr);
		// the misleading generic headless message is gone for this case
		assertFalse(r.stdout.contains("No usable display device"),
				r.stdout);
		assertFalse(r.stderr.contains("Exception"), r.stderr);
	}

	@Test
	void forcedDefaultOnWaylandOnlyRestoresTheLegacyPath()
			throws Exception {
		// the escape hatch must force the untouched-default branch
		// (#105 P4): with DISPLAY unset, stock OpenJDK on Linux
		// auto-detects headless and JLS lands in its legacy
		// HeadlessException handler
		assumeLinux();
		assumeStockJdk();
		Result r = run("wayland-9", null,
				List.of("-Djls.toolkit=default"));
		assertEquals(1, r.exit, r.stdout + r.stderr);
		assertTrue(r.stdout.contains("No usable display device"),
				r.stdout + r.stderr);
	}

	@Test
	void forcedWaylandWithoutWlToolkitFailsEvenOnX11() throws Exception {
		assumeStockJdk();
		// safe on any OS: the override is checked before everything else
		Result r = run(null, ":0", List.of("-Djls.toolkit=wayland"));
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.startsWith("jls: error: "), r.stderr);
		assertTrue(r.stderr.contains("jls.toolkit"), r.stderr);
	}

	@Test
	void unrecognizedToolkitOverrideFailsWithOneLine() throws Exception {
		Result r = run(null, null, List.of("-Djls.toolkit=cocoa"));
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.startsWith("jls: error: "), r.stderr);
		assertTrue(r.stderr.contains("cocoa"), r.stderr);
		assertEquals(1, r.stderr.strip().split("\n").length, r.stderr);
	}

	@Test
	void batchModeIsUnaffectedOnAWaylandOnlySession() throws Exception {
		// a Wayland-only grading box must keep working with stock
		// OpenJDK: batch never needs a display (#105 P3); note no
		// java.awt.headless flag is passed — batch sets it itself
		Result r = run("wayland-9", null, List.of(), "-b", "nosuch.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertTrue(r.stderr.contains("nosuch.jls"), r.stderr);
		assertFalse(r.stderr.contains("Wayland"),
				"batch must never hit the toolkit policy: " + r.stderr);
	}

	@Test
	void usageErrorsAreUnaffectedOnAWaylandOnlySession() throws Exception {
		Result r = run("wayland-9", null, List.of(), "-x");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error: unknown option -x"),
				r.stderr);
	}

	@Test
	void helpIsUnaffectedAndDocumentsTheEscapeHatch() throws Exception {
		Result r = run("wayland-9", null, List.of(), "-h");
		assertEquals(0, r.exit, r.stderr);
		assertEquals(JLSStart.usageText(), r.stderr,
				"-h must print exactly the table-generated usage text");
		assertTrue(r.stderr.contains("-Djls.toolkit=default|wayland"),
				r.stderr);
	}
}
