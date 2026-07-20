package jls.hdl.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage companion to {@link VhdlEntityScannerTest} (issues
 * #63, #159). The sibling suite pins the headline behaviours against the
 * committed VHDL goldens; this one drives every port mode, both range
 * directions, the full arithmetic operator set of the constant
 * evaluator, and the out-of-subset rejection paths. Every expected width
 * is computed by hand from the range arithmetic, never echoed from the
 * scanner.
 */
class VhdlEntityScannerCoverageTest {

	/**
	 * Scans one entity and returns it, failing if the source does not
	 * hold exactly one entity declaration.
	 * @param source the VHDL source text
	 * @return the single scanned entity
	 * @throws HdlScanException if scanning fails
	 */
	private static ScannedModule one(String source)
			throws HdlScanException {
		List<ScannedModule> entities = VhdlEntityScanner.scan(source);
		assertEquals(1, entities.size(), "expected exactly one entity");
		return entities.get(0);
	} // end of one method

	/**
	 * The width of the first port of a one-port entity whose type is
	 * under test.
	 * @param portDecl the text of the port declaration inside {@code
	 *        port(...)}
	 * @return the evaluated width
	 * @throws HdlScanException if scanning fails
	 */
	private static int width(String portDecl) throws HdlScanException {
		return one("entity e is port(" + portDecl + "); end e;")
				.ports().get(0).bits;
	} // end of width method

	/**
	 * Scans a source expected to be rejected, returning the exception.
	 * @param source the source text
	 * @return the thrown scan exception
	 */
	private static HdlScanException reject(String source) {
		return assertThrows(HdlScanException.class,
				() -> VhdlEntityScanner.scan(source));
	} // end of reject method

	// ------------------------------------------------------------------
	// port modes and range directions
	// ------------------------------------------------------------------

	@Test
	void ascendingToRangeGivesTheSameWidthAsDownto() throws Exception {
		assertEquals(8, width("a : in std_logic_vector(7 downto 0)"));
		assertEquals(8, width("a : in std_logic_vector(0 to 7)"));
	} // end of ascendingToRangeGivesTheSameWidthAsDownto method

	@Test
	void bufferAndInoutModesMapToOutAndInout() throws Exception {
		ScannedModule m = one("entity e is port("
				+ "a : buffer std_logic; b : inout std_logic;"
				+ " c : out std_logic); end e;");
		assertEquals(ScannedPort.Direction.OUT,
				m.ports().get(0).direction);
		assertEquals(ScannedPort.Direction.INOUT,
				m.ports().get(1).direction);
		assertEquals(ScannedPort.Direction.OUT,
				m.ports().get(2).direction);
	} // end of bufferAndInoutModesMapToOutAndInout method

	@Test
	void everyScalarTypeIsOneBitWide() throws Exception {
		assertEquals(1, width("a : in std_logic"));
		assertEquals(1, width("a : in std_ulogic"));
		assertEquals(1, width("a : in bit"));
		assertEquals(1, width("a : in boolean"));
	} // end of everyScalarTypeIsOneBitWide method

	@Test
	void everyVectorTypeTakesAConstraint() throws Exception {
		assertEquals(4, width("a : in std_ulogic_vector(3 downto 0)"));
		assertEquals(4, width("a : in signed(3 downto 0)"));
		assertEquals(4, width("a : in unsigned(0 to 3)"));
		assertEquals(2, width("a : in bit_vector(1 downto 0)"));
	} // end of everyVectorTypeTakesAConstraint method

	// ------------------------------------------------------------------
	// constant-expression evaluator
	// ------------------------------------------------------------------

	@Test
	void multiplicationDivisionModAndRemEvaluate() throws Exception {
		// 2*4-1 == 7 -> [7:0] is eight bits
		assertEquals(8, width("a : in signed(2 * 4 - 1 downto 0)"));
		// 15/4 == 3 -> [3:0] is four bits
		assertEquals(4, width("a : in signed(15 / 4 downto 0)"));
		// 10 mod 3 == 1 -> [1:0] is two bits
		assertEquals(2, width("a : in signed(10 mod 3 downto 0)"));
		// 10 rem 4 == 2 -> [2:0] is three bits
		assertEquals(3, width("a : in signed(10 rem 4 downto 0)"));
	} // end of multiplicationDivisionModAndRemEvaluate method

	@Test
	void powerAndUnarySignAndParenthesesEvaluate() throws Exception {
		// 2**3-1 == 7 -> eight bits
		assertEquals(8, width("a : in signed(2 ** 3 - 1 downto 0)"));
		// -(-3) == 3 -> [3:0] is four bits
		assertEquals(4, width("a : in signed(-(-3) downto 0)"));
		// (1+2) == 3 -> four bits
		assertEquals(4, width("a : in signed((1 + 2) downto 0)"));
	} // end of powerAndUnarySignAndParenthesesEvaluate method

	@Test
	void aGenericMayReferenceAnEarlierGeneric() throws Exception {
		ScannedModule m = one("entity e is generic("
				+ "A : integer := 2; B : integer := A * 3);\n"
				+ "port(d : in std_logic_vector(B - 1 downto 0));"
				+ " end e;");
		assertEquals(Long.valueOf(2), m.parameters().get("A"));
		assertEquals(Long.valueOf(6), m.parameters().get("B"));
		assertEquals(6, m.ports().get(0).bits);
	} // end of aGenericMayReferenceAnEarlierGeneric method

	@Test
	void aStringDefaultGenericIsHarmlessWhenNoWidthNeedsIt()
			throws Exception {
		ScannedModule m = one("entity e is generic("
				+ "INIT : std_logic_vector(3 downto 0) := \"0000\");\n"
				+ "port(a : in std_logic); end e;");
		assertEquals(1, m.ports().get(0).bits);
		assertFalse(m.parameters().containsKey("INIT"),
				"a string default is not an integer generic");
	} // end of aStringDefaultGenericIsHarmlessWhenNoWidthNeedsIt method

	@Test
	void divisionByZeroInAWidthIsRejected() {
		assertTrue(reject("entity e is port("
				+ "a : in std_logic_vector(8 / 0 downto 0)); end e;")
				.getMessage().contains("division by zero"));
	} // end of divisionByZeroInAWidthIsRejected method

	@Test
	void anEmptyRangeBoundIsRejected() {
		assertTrue(reject("entity e is port("
				+ "a : in std_logic_vector(downto 0)); end e;")
				.getMessage().contains("empty expression"));
	} // end of anEmptyRangeBoundIsRejected method

	@Test
	void aNumericLiteralPastLongRangeIsRejected() {
		assertTrue(reject("entity e is port(a : in std_logic_vector("
				+ "99999999999999999999 downto 0)); end e;")
				.getMessage().contains("out of range"));
	} // end of aNumericLiteralPastLongRangeIsRejected method

	@Test
	void aConstraintWithoutADirectionKeywordIsRejected() {
		assertTrue(reject("entity e is port("
				+ "a : in std_logic_vector(7 - 0)); end e;")
				.getMessage().contains("downto"));
	} // end of aConstraintWithoutADirectionKeywordIsRejected method

	// ------------------------------------------------------------------
	// structural rejection paths
	// ------------------------------------------------------------------

	@Test
	void twoPortClausesAreRejected() {
		assertTrue(reject("entity e is port(a : in std_logic);"
				+ " port(b : out std_logic); end e;")
				.getMessage().contains("two port clauses"));
	} // end of twoPortClausesAreRejected method

	@Test
	void aNameListWithoutACommaIsRejected() {
		assertTrue(reject("entity e is port("
				+ "a b : in std_logic); end e;")
				.getMessage().contains("expected ','"));
	} // end of aNameListWithoutACommaIsRejected method

	@Test
	void aPortWithoutATypeIsRejected() {
		assertTrue(reject("entity e is port(a : ); end e;")
				.getMessage().contains("without a type"));
	} // end of aPortWithoutATypeIsRejected method

	@Test
	void junkAfterAPortTypeIsRejected() {
		assertTrue(reject("entity e is port("
				+ "a : in std_logic foo); end e;")
				.getMessage().contains("after port type"));
	} // end of junkAfterAPortTypeIsRejected method

	@Test
	void aMalformedGenericIsRejected() {
		assertTrue(reject("entity e is generic("
				+ "BADGENERIC); port(a : in std_logic); end e;")
				.getMessage().contains("generic"));
	} // end of aMalformedGenericIsRejected method

	@Test
	void extendedIdentifiersAreRejected() {
		assertTrue(reject("entity e is port("
				+ "\\odd name\\ : in std_logic); end e;")
				.getMessage().contains("extended identifiers"));
	} // end of extendedIdentifiersAreRejected method

	@Test
	void anUnterminatedBlockCommentIsRejected() {
		assertTrue(reject("entity e is /* never closed\n")
				.getMessage().contains("block comment"));
	} // end of anUnterminatedBlockCommentIsRejected method

	@Test
	void anUnterminatedStringLiteralIsRejected() {
		assertTrue(reject("entity e is port(a : in std_logic := \"01\n")
				.getMessage().contains("string"));
	} // end of anUnterminatedStringLiteralIsRejected method

	@Test
	void doubledQuotesInsideAStringAreOneEscapedQuote() throws Exception {
		// the "" escape must not end the string early; the entity after
		// it still scans, proving the scanner stayed in sync
		ScannedModule m = one("entity e is generic("
				+ "MSG : string := \"a\"\"b\");\n"
				+ "port(z : in std_logic); end e;");
		assertEquals("e", m.name);
		assertEquals(1, m.ports().get(0).bits);
	} // end of doubledQuotesInsideAStringAreOneEscapedQuote method
}
