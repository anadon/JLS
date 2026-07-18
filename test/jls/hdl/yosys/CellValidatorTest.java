package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The restricted-cell gatekeeper (issue #61): pipeline survivors and
 * hierarchy instances pass; async-reset storage, set/reset storage,
 * wide arithmetic and non-ROM memories are rejected with teaching
 * messages carrying the Verilog source location; internal cells the
 * pipeline should have eliminated are named as JLS bugs; and every
 * violation in a netlist is reported in one pass (predictions P2/P5).
 */
class CellValidatorTest {

	/** Fixtures read better with single quotes; swap them for real ones. */
	private static String json(String singleQuoted) {
		return singleQuoted.replace('\'', '"');
	}

	/**
	 * Wraps cell bodies into a one-module netlist.
	 *
	 * @param cells The members of the "cells" object.
	 *
	 * @return the parsed netlist.
	 */
	private static YosysNetlist netlistWith(String cells)
			throws NetlistFormatException {
		return YosysNetlist.parse(json(
				"{ 'modules': { 'top': { 'cells': { " + cells
						+ " } } } }"));
	}

	/**
	 * Builds one minimal cell body.
	 *
	 * @param type The cell type.
	 * @param src The src attribute, or "" for none.
	 * @param parameters The members of the "parameters" object.
	 *
	 * @return the cell body text (single-quoted).
	 */
	private static String cell(String type, String src,
			String parameters) {
		String attributes = src.isEmpty() ? ""
				: " 'attributes': { 'src': '" + src + "' },";
		return "{ 'type': '" + type + "'," + attributes
				+ " 'parameters': { " + parameters + " } }";
	}

	@Test
	void pipelineSurvivorsAndInstancesPass() throws Exception {
		YosysNetlist netlist = netlistWith(
				"'g1': " + cell("$and", "", "")
				+ ", 'g2': " + cell("$mux", "", "")
				+ ", 'g3': " + cell("$dff", "", "")
				+ ", 'g4': " + cell("$reduce_xor", "", "")
				+ ", 'g5': " + cell("$tribuf", "", "")
				+ ", 'g6': " + cell("$add", "", "")
				+ ", 'child': " + cell("blinker", "", ""));
		assertEquals(List.of(), CellValidator.validate(netlist));
	}

	@Test
	void asyncResetTeachesTheSyncRewrite() throws Exception {
		YosysNetlist netlist = netlistWith("'ff': "
				+ cell("$adff", "counter.v:8.3-10.6", ""));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(1, violations.size());
		CellViolation v = violations.get(0);
		assertEquals("$adff", v.type());
		assertEquals("counter.v:8.3-10.6", v.sourceLocation());
		assertTrue(v.message().contains("asynchronous reset"),
				v.message());
		assertTrue(v.message().contains("synchronously"),
				v.message());
		assertTrue(v.message().contains("always @(posedge clk)"),
				"the message must show the rewrite: " + v.message());
		assertTrue(v.describe().contains("counter.v:8.3-10.6"),
				v.describe());
		assertTrue(v.describe().contains("$adff"), v.describe());
	}

	@Test
	void wideArithmeticPointsAtStructuralDecomposition()
			throws Exception {
		YosysNetlist netlist = netlistWith("'m': "
				+ cell("$mul", "alu.v:14.9-14.14", ""));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(1, violations.size());
		assertTrue(violations.get(0).message().contains("Adder"),
				violations.get(0).message());
	}

	@Test
	void setResetStorageIsItsOwnLesson() throws Exception {
		YosysNetlist netlist = netlistWith("'sr': "
				+ cell("$dffsr", "", ""));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(1, violations.size());
		assertTrue(violations.get(0).message()
				.contains("set/reset"), violations.get(0).message());
	}

	@Test
	void pipelineLeftoverIsNamedAsAJlsBug() throws Exception {
		YosysNetlist netlist = netlistWith("'s': "
				+ cell("$sub", "alu.v:9.3-9.20", ""));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(1, violations.size());
		CellViolation v = violations.get(0);
		assertTrue(v.message().contains("$sub"), v.message());
		assertTrue(v.message().contains("pipeline"), v.message());
		assertTrue(v.message().contains("bug"), v.message());
		assertTrue(v.message().contains("not an error in your"),
				"never blame the user for a mapping gap: "
						+ v.message());
	}

	@Test
	void asyncReadRomIsSupported() throws Exception {
		YosysNetlist netlist = netlistWith("'rom': "
				+ cell("$mem_v2", "rom.v:5.3-7.6",
						"'WR_PORTS': 0, 'RD_PORTS': 1,"
						+ " 'RD_CLK_ENABLE': '0'"));
		assertEquals(List.of(), CellValidator.validate(netlist));
	}

	@Test
	void clockedReadMemoryIsRejected() throws Exception {
		YosysNetlist netlist = netlistWith("'ram': "
				+ cell("$mem_v2", "ram.v:6.3-9.6",
						"'WR_PORTS': 0, 'RD_PORTS': 1,"
						+ " 'RD_CLK_ENABLE': '1'"));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(1, violations.size());
		assertTrue(violations.get(0).message()
				.contains("single-port"),
				violations.get(0).message());
	}

	@Test
	void writtenMemoryIsRejected() throws Exception {
		YosysNetlist netlist = netlistWith("'ram': "
				+ cell("$mem_v2", "",
						"'WR_PORTS': 1, 'RD_PORTS': 1,"
						+ " 'RD_CLK_ENABLE': '0'"));
		assertEquals(1, CellValidator.validate(netlist).size());
	}

	@Test
	void multiReadPortMemoryIsRejected() throws Exception {
		YosysNetlist netlist = netlistWith("'ram': "
				+ cell("$mem_v2", "",
						"'WR_PORTS': 0, 'RD_PORTS': 2,"
						+ " 'RD_CLK_ENABLE': '00'"));
		assertEquals(1, CellValidator.validate(netlist).size());
	}

	@Test
	void everyViolationIsReportedInOnePass() throws Exception {
		YosysNetlist netlist = netlistWith(
				"'a': " + cell("$adff", "f.v:3.1-5.4", "")
				+ ", 'ok': " + cell("$dff", "", "")
				+ ", 'b': " + cell("$div", "f.v:8.5-8.10", "")
				+ ", 'c': " + cell("$sub", "", ""));
		List<CellViolation> violations =
				CellValidator.validate(netlist);
		assertEquals(3, violations.size(),
				"all violations surface in one failed import");
		assertEquals("$adff", violations.get(0).type());
		assertEquals("$div", violations.get(1).type());
		assertEquals("$sub", violations.get(2).type());
	}

	@Test
	void describeFallsBackToTheCellPath() throws Exception {
		YosysNetlist netlist = netlistWith("'noloc': "
				+ cell("$pow", "", ""));
		CellViolation v = CellValidator.validate(netlist).get(0);
		assertTrue(v.describe().contains("module \"top\""),
				v.describe());
		assertTrue(v.describe().contains("cell \"noloc\""),
				v.describe());
	}

} // end of CellValidatorTest class
