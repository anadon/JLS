package jls.hdl.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage companion to {@link VerilogHeaderScannerTest} (issues
 * #63, #159). The sibling suite pins the headline behaviours against a
 * committed corpus; this one drives the preprocessor's conditional and
 * macro machinery, the constant-expression evaluator's full operator and
 * literal set, and the out-of-subset rejection paths that route a file
 * to the external Yosys extractor. Every expected width is computed by
 * hand from the range arithmetic, never by echoing the scanner.
 */
class VerilogHeaderScannerCoverageTest {

	/**
	 * Scans one module and returns it, failing if the source does not
	 * hold exactly one module.
	 * @param source the Verilog source text
	 * @return the single scanned module
	 * @throws HdlScanException if scanning fails
	 */
	private static ScannedModule one(String source)
			throws HdlScanException {
		List<ScannedModule> modules = VerilogHeaderScanner.scan(source);
		assertEquals(1, modules.size(), "expected exactly one module");
		return modules.get(0);
	} // end of one method

	/**
	 * The width of the single port of a one-port module whose width
	 * expression is under test.
	 * @param widthExpr the text inside the {@code [ ... :0]} range
	 * @param prefix any leading text (parameters, defines) before module
	 * @return the evaluated width
	 * @throws HdlScanException if scanning fails
	 */
	private static int width(String prefix, String widthExpr)
			throws HdlScanException {
		ScannedModule m = one(prefix + "module m(input ["
				+ widthExpr + ":0] a); endmodule\n");
		return m.ports().get(0).bits;
	} // end of width method

	// ------------------------------------------------------------------
	// preprocessor: conditionals
	// ------------------------------------------------------------------

	@Test
	void elsifBranchIsTakenWhenItsGuardIsDefined() throws Exception {
		String src = "`define B 1\n"
				+ "`ifdef A\n`define W 4\n"
				+ "`elsif B\n`define W 8\n"
				+ "`else\n`define W 16\n`endif\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(8, one(src).ports().get(0).bits);
	} // end of elsifBranchIsTakenWhenItsGuardIsDefined method

	@Test
	void elseBranchIsTakenWhenNoGuardMatched() throws Exception {
		String src = "`ifdef A\n`define W 4\n"
				+ "`elsif B\n`define W 8\n"
				+ "`else\n`define W 16\n`endif\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(16, one(src).ports().get(0).bits);
	} // end of elseBranchIsTakenWhenNoGuardMatched method

	@Test
	void ifndefTakesItsBodyWhenTheNameIsUndefined() throws Exception {
		String src = "`ifndef NOPE\n`define W 5\n`endif\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(5, one(src).ports().get(0).bits);
	} // end of ifndefTakesItsBodyWhenTheNameIsUndefined method

	@Test
	void nestedConditionalsResolveInnermostFirst() throws Exception {
		String src = "`define A 1\n`define B 1\n"
				+ "`ifdef A\n`ifdef B\n`define W 3\n`endif\n`endif\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(3, one(src).ports().get(0).bits);
	} // end of nestedConditionalsResolveInnermostFirst method

	@Test
	void undefRemovesAMacroSoLaterUseIsUndefined() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`define W 8\n`undef W\n"
						+ "module m(input [`W-1:0] a); endmodule\n"));
		assertTrue(e.getMessage().contains("undefined macro"),
				e.getMessage());
	} // end of undefRemovesAMacroSoLaterUseIsUndefined method

	@Test
	void strayElseWithoutIfdefIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`else\n"
						+ "module m(input a); endmodule\n"));
		assertTrue(e.getMessage().contains("`else"), e.getMessage());
	} // end of strayElseWithoutIfdefIsRejected method

	@Test
	void strayEndifWithoutIfdefIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`endif\n"
						+ "module m(input a); endmodule\n"));
		assertTrue(e.getMessage().contains("`endif"), e.getMessage());
	} // end of strayEndifWithoutIfdefIsRejected method

	// ------------------------------------------------------------------
	// preprocessor: macros and directives
	// ------------------------------------------------------------------

	@Test
	void macrosMayReferenceOtherMacros() throws Exception {
		String src = "`define BASE 4\n`define W `BASE\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(4, one(src).ports().get(0).bits);
	} // end of macrosMayReferenceOtherMacros method

	@Test
	void aLineBeginningWithAMacroUseExpands() throws Exception {
		String src = "`define HEAD module m(input a)\n"
				+ "`HEAD ; endmodule\n";
		ScannedModule m = one(src);
		assertEquals("m", m.name);
		assertEquals(1, m.ports().size());
	} // end of aLineBeginningWithAMacroUseExpands method

	@Test
	void continuationLinesJoinIntoOneDefine() throws Exception {
		String src = "`define W 2 + \\\n  6\n"
				+ "module m(input [`W-1:0] a); endmodule\n";
		assertEquals(8, one(src).ports().get(0).bits);
	} // end of continuationLinesJoinIntoOneDefine method

	@Test
	void ignoredDirectivesCarryNoPortInformation() throws Exception {
		String src = "`timescale 1ns/1ps\n`default_nettype none\n"
				+ "module m(input [3:0] a); endmodule\n"
				+ "`resetall\n";
		assertEquals(4, one(src).ports().get(0).bits);
	} // end of ignoredDirectivesCarryNoPortInformation method

	@Test
	void unsupportedCompilerDirectiveIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`nonsense\n"
						+ "module m(input a); endmodule\n"));
		assertTrue(e.getMessage().contains("unsupported compiler"),
				e.getMessage());
	} // end of unsupportedCompilerDirectiveIsRejected method

	@Test
	void aNamelessDefineIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`define\n"
						+ "module m(input a); endmodule\n"));
		assertTrue(e.getMessage().contains("macro name"), e.getMessage());
	} // end of aNamelessDefineIsRejected method

	@Test
	void nonTerminatingMacroExpansionIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan("`define X `X\n"
						+ "module m(input [`X:0] a); endmodule\n"));
		assertTrue(e.getMessage().contains("does not terminate"),
				e.getMessage());
	} // end of nonTerminatingMacroExpansionIsRejected method

	// ------------------------------------------------------------------
	// constant-expression evaluator
	// ------------------------------------------------------------------

	@Test
	void hexAndOctalBasedLiteralsEvaluate() throws Exception {
		assertEquals(16, width("", "4'hF"));
		assertEquals(64, width("", "8'o77"));
	} // end of hexAndOctalBasedLiteralsEvaluate method

	@Test
	void signedBasedLiteralAndSpacedDigitsEvaluate() throws Exception {
		// 's' size marker, then the base and digits as a separate run
		assertEquals(16, width("", "'d 15"));
	} // end of signedBasedLiteralAndSpacedDigitsEvaluate method

	@Test
	void clog2RoundsUpToTheBitCount() throws Exception {
		// $clog2(256) == 8, so [8-1:0] is eight bits wide
		assertEquals(8, width("", "$clog2(256)-1"));
	} // end of clog2RoundsUpToTheBitCount method

	@Test
	void divisionModuloAndParenthesesEvaluate() throws Exception {
		assertEquals(4, width("", "16/4-1"));
		assertEquals(3, width("", "17%5"));
		assertEquals(8, width("", "(3+4)*1"));
	} // end of divisionModuloAndParenthesesEvaluate method

	@Test
	void unaryPlusAndMinusEvaluate() throws Exception {
		// (-(-7)) == 7, so [7:0] is eight bits
		assertEquals(8, width("", "-(-7)"));
		assertEquals(8, width("", "+7"));
	} // end of unaryPlusAndMinusEvaluate method

	@Test
	void divisionByZeroInAWidthIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> width("", "8/0"));
		assertTrue(e.getMessage().contains("division by zero"),
				e.getMessage());
	} // end of divisionByZeroInAWidthIsRejected method

	@Test
	void anUnreasonableExponentIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> width("", "2**99"));
		assertTrue(e.getMessage().contains("exponent"), e.getMessage());
	} // end of anUnreasonableExponentIsRejected method

	@Test
	void xzDigitsInAWidthLiteralAreRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> width("", "4'b1x01"));
		assertTrue(e.getMessage().contains("x/z digits"), e.getMessage());
	} // end of xzDigitsInAWidthLiteralAreRejected method

	@Test
	void aMalformedBasedLiteralIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> width("", "4'q7"));
		assertTrue(e.getMessage().contains("based literal"),
				e.getMessage());
	} // end of aMalformedBasedLiteralIsRejected method

	@Test
	void aRangeWithoutAColonIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [8] a); endmodule\n"));
		assertTrue(e.getMessage().contains("range"), e.getMessage());
	} // end of aRangeWithoutAColonIsRejected method

	@Test
	void anUnreasonableWidthIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [2000000:0] a); endmodule\n"));
		assertTrue(e.getMessage().contains("unreasonable"),
				e.getMessage());
	} // end of anUnreasonableWidthIsRejected method

	// ------------------------------------------------------------------
	// module structure
	// ------------------------------------------------------------------

	@Test
	void macromoduleKeywordOpensAModule() throws Exception {
		ScannedModule m = one(
				"macromodule m(input [3:0] a); endmodule\n");
		assertEquals("m", m.name);
		assertEquals(4, m.ports().get(0).bits);
	} // end of macromoduleKeywordOpensAModule method

	@Test
	void functionAndTaskBodiesAreSkippedNotTreatedAsPorts()
			throws Exception {
		String src = "module m(a, b);\n"
				+ "input a;\noutput b;\n"
				+ "function f;\ninput x;\nbegin f = x; end\nendfunction\n"
				+ "task t;\ninput y;\nbegin end\nendtask\n"
				+ "localparam LP = 2;\n"
				+ "endmodule\n";
		ScannedModule m = one(src);
		assertEquals(2, m.ports().size());
		assertEquals("a", m.ports().get(0).name);
		assertEquals("b", m.ports().get(1).name);
	} // end of functionAndTaskBodiesAreSkippedNotTreatedAsPorts method

	@Test
	void nonAnsiNetTypesAndBodyRangesAreHonoured() throws Exception {
		String src = "module m(a, q);\n"
				+ "input wire [7:0] a;\n"
				+ "output reg [3:0] q;\n"
				+ "endmodule\n";
		ScannedModule m = one(src);
		assertEquals(8, m.ports().get(0).bits);
		assertEquals(4, m.ports().get(1).bits);
		assertEquals(ScannedPort.Direction.OUT, m.ports().get(1).direction);
	} // end of nonAnsiNetTypesAndBodyRangesAreHonoured method

	@Test
	void nestedModuleDeclarationIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input a);\nmodule n(input b);"
								+ " endmodule\nendmodule\n"));
		assertTrue(e.getMessage().contains("nested"), e.getMessage());
	} // end of nestedModuleDeclarationIsRejected method

	@Test
	void aPortDeclaredTwiceIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(a);\ninput a;\ninput a;\nendmodule\n"));
		assertTrue(e.getMessage().contains("twice"), e.getMessage());
	} // end of aPortDeclaredTwiceIsRejected method

	@Test
	void aBodyArrayPortIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(a);\ninput [3:0] a [0:1];\n"
								+ "endmodule\n"));
		assertTrue(e.getMessage().contains("array port"),
				e.getMessage());
	} // end of aBodyArrayPortIsRejected method

	@Test
	void aMultiDimensionalAnsiPortIsRejected() {
		HdlScanException e = assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(
						"module m(input [3:0][1:0] a); endmodule\n"));
		assertTrue(e.getMessage().contains("multi-dimensional"),
				e.getMessage());
	} // end of aMultiDimensionalAnsiPortIsRejected method

	@Test
	void mixingDirectedAndPlainPortsIsRejected() {
		// an ANSI list that starts with a bare name before any direction
		assertTrue(reject("module m(a, input b); endmodule\n")
				.getMessage().contains("mixes"));
	} // end of mixingDirectedAndPlainPortsIsRejected method

	/**
	 * Scans a source expected to be rejected, returning the exception.
	 * @param source the source text
	 * @return the thrown scan exception
	 */
	private static HdlScanException reject(String source) {
		return assertThrows(HdlScanException.class,
				() -> VerilogHeaderScanner.scan(source));
	} // end of reject method

	@Test
	void ansiPortWithADefaultIsAcceptedButTrailingJunkIsNot()
			throws Exception {
		// '=' after a name in an ANSI list is a default and is allowed
		ScannedModule m = one(
				"module m(input [3:0] a = 4'd0); endmodule\n");
		assertEquals(4, m.ports().get(0).bits);
		assertTrue(reject("module m(input [3:0] a b); endmodule\n")
				.getMessage().contains("unexpected"));
	} // end of ansiPortWithADefaultIsAcceptedButTrailingJunkIsNot method

	@Test
	void anUnterminatedBlockCommentIsRejected() {
		assertTrue(reject("module m(input a); /* never closed\n")
				.getMessage().contains("block comment"));
	} // end of anUnterminatedBlockCommentIsRejected method

	@Test
	void anUnterminatedStringLiteralIsRejected() {
		assertTrue(reject("module m(input a);\n$display(\"oops\n")
				.getMessage().contains("string"));
	} // end of anUnterminatedStringLiteralIsRejected method

	@Test
	void aFunctionBodyWithoutItsEndKeywordIsRejected() {
		assertTrue(reject("module m(a);\ninput a;\nfunction f;\n")
				.getMessage().contains("endfunction"));
	} // end of aFunctionBodyWithoutItsEndKeywordIsRejected method
}
