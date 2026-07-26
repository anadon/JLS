package jls.hdl.board;

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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests of the -board/-pins export operands (issue #213),
 * subprocess style like CliVerilogExportTest: {@code jls -export out.v
 * -board icestick -pins pins.txt circuit.jls} writes the Verilog and a
 * .pcf next to it and exits 0; an unbindable port is one
 * {@code jls: error:} line, exit 1, and nothing — no HDL, no
 * constraint file, no temp file — reaches disk; the flag-pairing rules
 * are usage errors (exit 2).
 */
class CliBoardExportTest {

	@TempDir
	Path tmp;

	/** the exit code and captured stderr of one CLI subprocess run. */
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

	/** Write the blinky circuit and a bindings file into the temp dir. */
	private void writeFixture(String... bindingLines) throws Exception {
		Files.writeString(tmp.resolve("blinky.jls"),
				BoardFixtures.blinkyText(), StandardCharsets.UTF_8);
		Files.writeString(tmp.resolve("pins.txt"),
				String.join("\n", bindingLines) + "\n",
				StandardCharsets.UTF_8);
	}

	/** The complete, correct iCEstick bindings for blinky. */
	private static String[] goodBindings() {
		return new String[] {
				"sw[0] PMOD1", "sw[1] PMOD2", "clk CLK",
				"led[0] LED1", "led[1] LED2" };
	}

	@Test
	void boardExportWritesTheHdlAndTheConstraintsBesideIt()
			throws Exception {

		writeFixture(goodBindings());
		Result r = run("-export", "out.v", "-board", "icestick",
				"-pins", "pins.txt", "blinky.jls");
		assertEquals(0, r.exit, r.stderr);
		assertTrue(Files.exists(tmp.resolve("out.v")),
				"the HDL must be written");
		Path pcf = tmp.resolve("out.pcf");
		assertTrue(Files.exists(pcf),
				"the constraint file must land next to the HDL");
		String text = Files.readString(pcf, StandardCharsets.UTF_8);
		assertTrue(text.contains("set_io led[1] 98"), text);
		assertTrue(text.contains("set_io clk 21"), text);
		assertFalse(Files.exists(tmp.resolve("out.pcf.tmp")),
				"no temp file may survive");
	}

	@Test
	void anUnbindablePortWritesNothingAtAll() throws Exception {

		// led[1] is never bound: exit 1, an actionable error, and
		// neither the HDL nor any (partial) constraint file on disk
		writeFixture("sw[0] PMOD1", "sw[1] PMOD2", "clk CLK",
				"led[0] LED1");
		Result r = run("-export", "out.v", "-board", "icestick",
				"-pins", "pins.txt", "blinky.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertTrue(r.stderr.contains("led[1]"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.pcf")),
				"a partial constraint file must never reach disk");
		assertFalse(Files.exists(tmp.resolve("out.v")),
				"the HDL is withheld too - the export failed as a whole");
		assertFalse(Files.exists(tmp.resolve("out.v.tmp")));
		assertFalse(Files.exists(tmp.resolve("out.pcf.tmp")));
	}

	@Test
	void aMalformedBindingsFileFailsWithItsLineNumber()
			throws Exception {

		writeFixture("sw[0] PMOD1 PMOD2");
		Result r = run("-export", "out.v", "-board", "icestick",
				"-pins", "pins.txt", "blinky.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("line 1"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.v")));
		assertFalse(Files.exists(tmp.resolve("out.pcf")));
	}

	@Test
	void boardWithoutPinsIsAUsageError() throws Exception {

		writeFixture(goodBindings());
		Result r = run("-export", "out.v", "-board", "icestick",
				"blinky.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("-board and -pins"), r.stderr);
	}

	@Test
	void pinsWithoutExportIsAUsageError() throws Exception {

		writeFixture(goodBindings());
		Result r = run("-pins", "pins.txt", "blinky.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("require -export"), r.stderr);
	}

	@Test
	void anUnknownBoardNamesTheSupportedOnes() throws Exception {

		writeFixture(goodBindings());
		Result r = run("-export", "out.v", "-board", "basys3",
				"-pins", "pins.txt", "blinky.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("icestick"),
				"the supported boards must be listed: " + r.stderr);
	}

	@Test
	void aMissingBindingsFileIsARuntimeError() throws Exception {

		Files.writeString(tmp.resolve("blinky.jls"),
				BoardFixtures.blinkyText(), StandardCharsets.UTF_8);
		Result r = run("-export", "out.v", "-board", "icestick",
				"-pins", "nosuch.txt", "blinky.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("nosuch.txt"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.v")));
	}
}
