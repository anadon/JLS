package jls.hdl.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VerilogHeaderScanner} (issue #63): both port
 * styles, parameters and macros in widths, the minimal preprocessor,
 * and - just as important - clean rejection of everything outside the
 * scanned subset, since rejected files route to the Yosys extraction
 * path instead of growing the scanner into a parser (issue #63
 * &#167;9). The corpus files scanned here are cross-checked against
 * Yosys ground truth by {@link YosysGroundTruthTest} when the tool is
 * installed.
 */
class VerilogHeaderScannerTest {

	/** Where the committed scanner corpus lives. */
	private static final Path CORPUS =
			Path.of("test", "resources", "hdl", "scan");

	/**
	 * Reads one corpus file.
	 * @param name the file name under the corpus directory
	 * @return the file text
	 * @throws IOException if the corpus file is missing
	 */
	private static String corpus(String name) throws IOException {
		return Files.readString(CORPUS.resolve(name));
	} // end of corpus method

	/**
	 * Asserts one port's full shape.
	 * @param port the scanned port
	 * @param name the expected name
	 * @param direction the expected direction
	 * @param bits the expected width
	 */
	private static void assertPort(ScannedPort port, String name,
			ScannedPort.Direction direction, int bits) {
		assertEquals(name, port.name, "port name");
		assertEquals(direction, port.direction,
				"direction of " + name);
		assertEquals(bits, port.bits, "width of " + name);
	} // end of assertPort method

	@Test
	void ansiCorpusScansAllPortsAndParameters() throws Exception {
		List<ScannedModule> modules =
				VerilogHeaderScanner.scan(corpus("ansi_alu.v"));
		assertEquals(1, modules.size());
		ScannedModule m = modules.get(0);
		assertEquals("ansi_alu", m.name);
		assertEquals(Long.valueOf(8), m.parameters().get("WIDTH"));
		assertEquals(Long.valueOf(16), m.parameters().get("DEPTH"));
		assertEquals(Long.valueOf(4), m.parameters().get("ADDR"));
		assertEquals(7, m.ports().size());
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 8);
		assertPort(m.ports().get(1), "b", ScannedPort.Direction.IN, 8);
		assertPort(m.ports().get(2), "sel",
				ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(3), "carry_in",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(4), "result",
				ScannedPort.Direction.OUT, 8);
		assertPort(m.ports().get(5), "carry_out",
				ScannedPort.Direction.OUT, 1);
		assertPort(m.ports().get(6), "debug_bus",
				ScannedPort.Direction.INOUT, 8);
	} // end of ansiCorpusScansAllPortsAndParameters method

	@Test
	void nonAnsiCorpusScansHeaderOrderWithBodyDirections()
			throws Exception {
		List<ScannedModule> modules =
				VerilogHeaderScanner.scan(corpus("nonansi_counter.v"));
		assertEquals(1, modules.size());
		ScannedModule m = modules.get(0);
		assertEquals("nonansi_counter", m.name);
		assertEquals(Long.valueOf(4), m.parameters().get("WIDTH"));
		assertEquals(Long.valueOf(15), m.parameters().get("MAX"));
		assertEquals(5, m.ports().size());
		assertPort(m.ports().get(0), "clk",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(1), "rst",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(2), "en",
				ScannedPort.Direction.IN, 1);
		assertPort(m.ports().get(3), "count",
				ScannedPort.Direction.OUT, 4);
		assertPort(m.ports().get(4), "tc",
				ScannedPort.Direction.OUT, 1);
	} // end of nonAnsiCorpusScansHeaderOrderWithBodyDirections method

	@Test
	void macroCorpusResolvesWidthsThroughDefinesAndIfdef()
			throws Exception {
		List<ScannedModule> modules =
				VerilogHeaderScanner.scan(corpus("macro_widths.v"));
		assertEquals(1, modules.size());
		ScannedModule m = modules.get(0);
		assertEquals("macro_widths", m.name);
		assertEquals(2, m.ports().size());
		assertPort(m.ports().get(0), "d", ScannedPort.Direction.IN, 8);
		assertPort(m.ports().get(1), "q",
				ScannedPort.Direction.OUT, 8);
	} // end of macroCorpusResolvesWidthsThroughDefinesAndIfdef method

	@Test
	void definingTheIfdefGuardSelectsTheOtherBranch() throws Exception {
		String wide = "`define WIDE 1\n" + corpus("macro_widths.v");
		ScannedModule m = VerilogHeaderScanner.scan(wide).get(0);
		assertPort(m.ports().get(0), "d",
				ScannedPort.Direction.IN, 16);
		assertPort(m.ports().get(1), "q",
				ScannedPort.Direction.OUT, 16);
	} // end of definingTheIfdefGuardSelectsTheOtherBranch method

	@Test
	void directionAndRangeStickAcrossCommasUntilChanged()
			throws Exception {
		ScannedModule m = VerilogHeaderScanner.scan(
				"module m(input [3:0] a, b, output c, input d);\n"
						+ "endmodule\n").get(0);
		assertEquals(4, m.ports().size());
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(1), "b", ScannedPort.Direction.IN, 4);
		assertPort(m.ports().get(2), "c",
				ScannedPort.Direction.OUT, 1);
		assertPort(m.ports().get(3), "d", ScannedPort.Direction.IN, 1);
	} // end of directionAndRangeStickAcrossCommasUntilChanged method

	@Test
	void basedLiteralsAndPowerWorkInWidths() throws Exception {
		ScannedModule m = VerilogHeaderScanner.scan(
				"module m #(parameter N = 2 ** 3)\n"
						+ "(input [4'd7:1'b0] a, output [N-1:0] y);\n"
						+ "endmodule\n").get(0);
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 8);
		assertPort(m.ports().get(1), "y",
				ScannedPort.Direction.OUT, 8);
	} // end of basedLiteralsAndPowerWorkInWidths method

	@Test
	void commentsAnywhereInTheHeaderAreIgnored() throws Exception {
		ScannedModule m = VerilogHeaderScanner.scan(
				"module /* name follows */ m // trailing\n"
						+ "( input /* mid */ [7:0] a // eol\n"
						+ ", output b );\n"
						+ "endmodule\n").get(0);
		assertEquals("m", m.name);
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 8);
		assertPort(m.ports().get(1), "b",
				ScannedPort.Direction.OUT, 1);
	} // end of commentsAnywhereInTheHeaderAreIgnored method

	@Test
	void modulesWithoutPortsScanAsEmpty() throws Exception {
		List<ScannedModule> modules = VerilogHeaderScanner.scan(
				"module a; endmodule\nmodule b(); endmodule\n");
		assertEquals(2, modules.size());
		assertEquals("a", modules.get(0).name);
		assertTrue(modules.get(0).ports().isEmpty());
		assertEquals("b", modules.get(1).name);
		assertTrue(modules.get(1).ports().isEmpty());
	} // end of modulesWithoutPortsScanAsEmpty method

	@Test
	void multipleModulesComeBackInFileOrder() throws Exception {
		List<ScannedModule> modules = VerilogHeaderScanner.scan(
				"module first(input a); endmodule\n"
						+ "module second(output b); endmodule\n");
		assertEquals(2, modules.size());
		assertEquals("first", modules.get(0).name);
		assertEquals("second", modules.get(1).name);
	} // end of multipleModulesComeBackInFileOrder method

	@Test
	void functionLikeMacrosAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"`define MAX(a,b) ((a)>(b)?(a):(b))\n"
								+ "module m(input x); endmodule\n"));
		assertTrue(e.getMessage().contains("function-like"),
				e.getMessage());
	} // end of functionLikeMacrosAreRejected method

	@Test
	void includeDirectiveIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"`include \"defs.vh\"\n"
								+ "module m(input x); endmodule\n"));
		assertTrue(e.getMessage().contains("`include"),
				e.getMessage());
	} // end of includeDirectiveIsRejected method

	@Test
	void undefinedMacroIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [`NOPE-1:0] x); endmodule\n"));
		assertTrue(e.getMessage().contains("undefined macro"),
				e.getMessage());
	} // end of undefinedMacroIsRejected method

	@Test
	void escapedIdentifiersAreRejected() {
		assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input \\weird!name );\nendmodule\n"));
	} // end of escapedIdentifiersAreRejected method

	@Test
	void arrayPortsAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [3:0] a [0:1]); endmodule\n"));
		assertTrue(e.getMessage().contains("array port"),
				e.getMessage());
	} // end of arrayPortsAreRejected method

	@Test
	void dotNamePortListsAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(.a(x)); endmodule\n"));
		assertTrue(e.getMessage().contains(".name()"),
				e.getMessage());
	} // end of dotNamePortListsAreRejected method

	@Test
	void unknownNameInWidthIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [MYSTERY-1:0] a);"
								+ " endmodule\n"));
		assertTrue(e.getMessage().contains("MYSTERY"),
				e.getMessage());
	} // end of unknownNameInWidthIsRejected method

	@Test
	void widthNeedingAnUnevaluableParameterIsRejected() {
		// the parameter default is out of subset (x digits); only a
		// width that needs it makes that fatal
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m #(parameter P = 4'bxx01)\n"
								+ "(input [P:0] a); endmodule\n"));
		assertTrue(e.getMessage().contains("P"), e.getMessage());
	} // end of widthNeedingAnUnevaluableParameterIsRejected method

	@Test
	void unevaluableParameterIsHarmlessWhenNoWidthNeedsIt()
			throws Exception {
		ScannedModule m = VerilogHeaderScanner.scan(
				"module m #(parameter P = 4'bxx01)\n"
						+ "(input [7:0] a); endmodule\n").get(0);
		assertPort(m.ports().get(0), "a", ScannedPort.Direction.IN, 8);
		assertEquals(false, m.parameters().containsKey("P"));
	} // end of unevaluableParameterIsHarmlessWhenNoWidthNeedsIt method

	@Test
	void nonAnsiPortWithoutDeclarationIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(a, b);\ninput a;\nendmodule\n"));
		assertTrue(e.getMessage().contains("'b'"), e.getMessage());
	} // end of nonAnsiPortWithoutDeclarationIsRejected method

	@Test
	void missingEndmoduleIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input a);\n"));
		assertTrue(e.getMessage().contains("endmodule"),
				e.getMessage());
	} // end of missingEndmoduleIsRejected method

	@Test
	void unbalancedIfdefIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"`ifdef X\nmodule m(input a); endmodule\n"));
		assertTrue(e.getMessage().contains("`endif"),
				e.getMessage());
	} // end of unbalancedIfdefIsRejected method

} // end of VerilogHeaderScannerTest class
