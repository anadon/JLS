package jls.hdl;

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
 * End-to-end tests of the -export CLI flag (issue #60), subprocess
 * style like CliImageExportTest: {@code jls -export out.v circuit.jls}
 * loads headlessly, writes Verilog and exits 0; warnings go to stderr;
 * a rejection is one {@code jls: error:} line, exit 1, and no file -
 * not even a temp file - reaches disk.
 */
class CliVerilogExportTest {

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

	private void write(String fileName, HdlCircuitBuilder cb)
			throws Exception {
		Files.writeString(tmp.resolve(fileName), cb.buildText(),
				StandardCharsets.UTF_8);
	}

	/** a NOT gate between two pins. */
	private static HdlCircuitBuilder inverter() {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("export");
		int a = cb.inputPin("a", 1);
		int not = cb.gate("NotGate", 1, 1);
		int y = cb.outputPin("y", 1);
		cb.wire(a, "output", not, "input0");
		cb.wire(not, "output", y, "input");
		return cb;
	}

	@Test
	void exportWritesVerilogAndExitsZero() throws Exception {
		write("export.jls", inverter());
		Result r = run("-export", "out.v", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertEquals("", r.stderr, "no warnings expected");
		String text = Files.readString(tmp.resolve("out.v"),
				StandardCharsets.UTF_8);
		assertTrue(text.startsWith("// export - Verilog-2005 export"),
				text);
		assertTrue(text.contains("module export (a, y);"), text);
		assertTrue(text.contains("endmodule"), text);
		assertFalse(Files.exists(tmp.resolve("out.v.tmp")),
				"the temp file must be renamed away");
	}

	@Test
	void attachedOperandIsAcceptedLikeOtherFlags() throws Exception {
		write("export.jls", inverter());
		Result r = run("-exportout.v", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertTrue(Files.exists(tmp.resolve("out.v")));
	}

	@Test
	void skipWarningsGoToStderrAndExportStillSucceeds() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("export");
		int a = cb.inputPin("a", 1);
		int display = cb.display(1);
		int y = cb.outputPin("y", 1);
		cb.wire3(a, "output", display, "input0", y, "input");
		write("export.jls", cb);

		Result r = run("-export", "out.v", "export.jls");
		assertEquals(0, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: warning:"), r.stderr);
		assertTrue(r.stderr.contains("Display"), r.stderr);
		assertTrue(Files.exists(tmp.resolve("out.v")));
	}

	@Test
	void rejectionIsExitOneAndWritesNothing() throws Exception {
		HdlCircuitBuilder cb = new HdlCircuitBuilder("export");
		cb.memory("mem", 8, 16);
		cb.subCircuit("sub1");
		write("export.jls", cb);

		Result r = run("-export", "out.v", "export.jls");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertTrue(r.stderr.contains("Memory"), r.stderr);
		assertTrue(r.stderr.contains("SubCircuit"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.v")),
				"a rejected export must write nothing");
		assertFalse(Files.exists(tmp.resolve("out.v.tmp")),
				"a rejected export must leave no temp file");
	}

	@Test
	void unknownHdlExtensionIsAUsageError() throws Exception {
		// .vhdl/.vhd select the VHDL emitter (CliVhdlExportTest); an
		// extension that is neither language is still a usage error
		write("export.jls", inverter());
		Result r = run("-export", "out.txt", "export.jls");
		assertEquals(2, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
		assertTrue(r.stderr.contains("-export"), r.stderr);
		assertFalse(Files.exists(tmp.resolve("out.txt")));
	}

	@Test
	void exportWithoutACircuitFileIsARuntimeError() throws Exception {
		Result r = run("-export", "out.v");
		assertEquals(1, r.exit, r.stderr);
		assertTrue(r.stderr.contains("jls: error:"), r.stderr);
	}

	@Test
	void deterministicAcrossRuns() throws Exception {
		write("export.jls", inverter());
		assertEquals(0, run("-export", "one.v", "export.jls").exit);
		assertEquals(0, run("-export", "two.v", "export.jls").exit);
		assertEquals(Files.readString(tmp.resolve("one.v")),
				Files.readString(tmp.resolve("two.v")),
				"same circuit, same bytes");
	}

} // end of CliVerilogExportTest class
