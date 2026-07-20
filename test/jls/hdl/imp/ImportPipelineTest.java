package jls.hdl.imp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.Circuit;
import jls.JLSInfo;
import jls.hdl.yosys.CellValidator;
import jls.hdl.yosys.CellViolation;
import jls.hdl.yosys.YosysNetlist;

/**
 * The issue-#61 end-to-end import leg: runs a real Yosys through the
 * restricted pass pipeline (report §6) - including
 * {@code techmap -map jls_map.v}, the techmap library authored in this
 * repo - on committed Verilog, then feeds the resulting
 * {@code write_json} netlist through {@link NetlistImporter}. When
 * {@code yosys} is not on {@code PATH} the whole test SKIPS (the
 * {@link jls.hdl.scan.YosysGroundTruthTest} pattern), so it is inert
 * without a toolchain and armed by the CI job that installs yosys.
 *
 * <p>Two things are proven here that the tool-free
 * {@link NetlistImporterTest} cannot: (1) {@code jls_map.v} is valid
 * and legalizes operator cells ({@code $xnor}) into JLS' supported set
 * - the emitted netlist passes {@link CellValidator} with zero
 * violations; and (2) a gate design elaborated by the real Yosys
 * imports into a circuit that loads through JLS' loader.</p>
 */
class ImportPipelineTest {

	/** Scratch directory for generated JSON. */
	@TempDir
	Path tmp;

	/** The committed corpus and techmap library. */
	private static final Path RES = Path.of("test", "resources", "hdl");

	@Test
	void techmapLegalizesXnorToTheSupportedSet() throws Exception {
		String yosys = requireYosys();
		YosysNetlist netlist = runPipeline(yosys,
				RES.resolve("import").resolve("imp_xnor.v"));
		List<CellViolation> violations = CellValidator.validate(netlist);
		assertEquals(List.of(), violations,
				"jls_map.v must legalize $xnor into the supported cell"
						+ " set; residual unsupported cells: " + violations);
	}

	@Test
	void gateDesignImportsIntoALoadableCircuit() throws Exception {
		String yosys = requireYosys();
		YosysNetlist netlist = runPipeline(yosys,
				RES.resolve("import").resolve("imp_gates.v"));

		// the pipeline must have left only mappable cells
		assertEquals(List.of(), CellValidator.validate(netlist),
				"pipeline output for imp_gates.v is not fully mappable");

		ImportResult result = NetlistImporter.importNetlist(netlist);
		Circuit circuit = new Circuit("imp_gates");
		assertTrue(circuit.load(new Scanner(result.saveText())),
				() -> "emitted circuit failed to load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "emitted circuit failed finishLoad: "
						+ JLSInfo.loadError);
		assertTrue(result.summary().elementCount() > 0,
				"import produced no elements");
	}

	/**
	 * Skips the test unless yosys is installed.
	 *
	 * @return the yosys executable path.
	 */
	private static String requireYosys() {
		String yosys = findOnPath("yosys");
		Assumptions.assumeTrue(yosys != null,
				"yosys not installed; skipping the end-to-end import leg");
		return yosys;
	}

	/**
	 * Runs the restricted import pipeline on one Verilog file and parses
	 * the emitted netlist.
	 *
	 * @param yosys The yosys executable path.
	 * @param verilog The Verilog source file.
	 *
	 * @return the parsed netlist.
	 *
	 * @throws Exception if yosys fails, times out, or emits bad JSON.
	 */
	private YosysNetlist runPipeline(String yosys, Path verilog)
			throws Exception {
		Path out = tmp.resolve(verilog.getFileName() + ".json");
		Path map = RES.resolve("jls_map.v").toAbsolutePath();
		String script = "read_verilog " + verilog.toAbsolutePath()
				+ "; hierarchy -auto-top; proc; opt_clean; memory -nomap;"
				+ " wreduce -memx; opt; dffunmap; pmuxtree; techmap -map "
				+ map + "; opt_clean; write_json " + out.toAbsolutePath();
		ProcessBuilder pb = new ProcessBuilder(yosys, "-q", "-p", script);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		p.getOutputStream().close();
		String log = new String(p.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
		if (!p.waitFor(120, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			throw new AssertionError("yosys timed out on " + verilog);
		}
		assertEquals(0, p.exitValue(),
				verilog + " pipeline failed:\n" + log);
		return YosysNetlist.parse(Files.readString(out));
	} // end of runPipeline method

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
	} // end of findOnPath method

} // end of ImportPipelineTest class
