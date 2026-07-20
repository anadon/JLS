package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage companion to {@link JsonValueTest} (issues #61, #159).
 * The sibling suite pins the headline decode behaviour; this one drives
 * the literal parsers ({@code true}/{@code false}/{@code null}), the
 * kind-predicate accessors, and the many one-line rejection messages the
 * strict subset raises on an untrusted netlist.
 */
class JsonValueCoverageTest {

	/** Fixtures read better with single quotes; swap them for real ones. */
	private static String json(String singleQuoted) {
		return singleQuoted.replace('\'', '"');
	} // end of json method

	/**
	 * Asserts a document is rejected and its message names the problem.
	 * @param text the document text
	 * @param fragment a substring the located message must contain
	 */
	private static void rejects(String text, String fragment) {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse(text));
		assertTrue(e.getMessage().contains(fragment),
				"message was: " + e.getMessage());
	} // end of rejects method

	// ------------------------------------------------------------------
	// literals and predicates
	// ------------------------------------------------------------------

	@Test
	void parsesTheThreeKeywordLiterals() throws Exception {
		// true/false/null decode; they are none of the typed kinds
		JsonValue arr = JsonValue.parse("[true, false, null]");
		assertTrue(arr.isArray());
		JsonValue t = arr.asArray().get(0);
		JsonValue f = arr.asArray().get(1);
		JsonValue n = arr.asArray().get(2);
		assertFalse(t.isObject());
		assertFalse(t.isString());
		assertFalse(f.isNumber());
		assertFalse(n.isArray());
	} // end of parsesTheThreeKeywordLiterals method

	@Test
	void kindPredicatesAgreeWithTheParsedShape() throws Exception {
		assertTrue(JsonValue.parse("{}").isObject());
		assertTrue(JsonValue.parse("[]").isArray());
		assertTrue(JsonValue.parse(json("'s'")).isString());
		assertTrue(JsonValue.parse("42").isNumber());
		assertEquals(42, JsonValue.parse("42").asLong());
		assertEquals("s", JsonValue.parse(json("'s'")).asString());
	} // end of kindPredicatesAgreeWithTheParsedShape method

	@Test
	void asArrayRejectsANonArray() throws Exception {
		JsonValue object = JsonValue.parse("{}");
		assertThrows(IllegalStateException.class, object::asArray);
	} // end of asArrayRejectsANonArray method

	// ------------------------------------------------------------------
	// object and array structure errors
	// ------------------------------------------------------------------

	@Test
	void anEmptyDocumentIsRejected() {
		rejects("   ", "unexpected end of input");
	} // end of anEmptyDocumentIsRejected method

	@Test
	void anUnexpectedLeadingCharacterIsRejected() {
		rejects("@", "unexpected character");
	} // end of anUnexpectedLeadingCharacterIsRejected method

	@Test
	void aBrokenKeywordLiteralIsRejected() {
		rejects("tru", "expected");
		rejects("nul", "expected");
	} // end of aBrokenKeywordLiteralIsRejected method

	@Test
	void anObjectMemberWithoutAStringKeyIsRejected() {
		rejects(json("{ 1: 2 }"), "member name string");
	} // end of anObjectMemberWithoutAStringKeyIsRejected method

	@Test
	void anObjectMemberWithoutAColonIsRejected() {
		rejects(json("{ 'a' 2 }"), "expected ':'");
	} // end of anObjectMemberWithoutAColonIsRejected method

	@Test
	void anObjectMissingItsCommaOrCloseIsRejected() {
		rejects(json("{ 'a': 1 'b': 2 }"), "expected ',' or '}'");
	} // end of anObjectMissingItsCommaOrCloseIsRejected method

	@Test
	void anUnterminatedObjectIsRejected() {
		rejects(json("{ 'a': 1"), "unterminated object");
	} // end of anUnterminatedObjectIsRejected method

	@Test
	void anArrayMissingItsCommaOrCloseIsRejected() {
		rejects("[1 2]", "expected ',' or ']'");
	} // end of anArrayMissingItsCommaOrCloseIsRejected method

	@Test
	void anUnterminatedArrayIsRejected() {
		rejects("[1", "unterminated array");
	} // end of anUnterminatedArrayIsRejected method

	// ------------------------------------------------------------------
	// string and number errors
	// ------------------------------------------------------------------

	@Test
	void aRawControlCharacterInAStringIsRejected() {
		rejects(json("'line\nbreak'"), "control character");
	} // end of aRawControlCharacterInAStringIsRejected method

	@Test
	void anUnknownStringEscapeIsRejected() {
		rejects(json("'\\x'"), "unknown escape");
	} // end of anUnknownStringEscapeIsRejected method

	@Test
	void aTruncatedUnicodeEscapeIsRejected() {
		rejects(json("'\\u00'"), "truncated");
	} // end of aTruncatedUnicodeEscapeIsRejected method

	@Test
	void aBadHexUnicodeEscapeIsRejected() {
		rejects(json("'\\uZZZZ'"), "bad");
	} // end of aBadHexUnicodeEscapeIsRejected method

	@Test
	void aLoneMinusSignIsAMalformedNumber() {
		rejects("-", "malformed number");
	} // end of aLoneMinusSignIsAMalformedNumber method

	@Test
	void aNumberBeyondLongRangeIsRejected() {
		rejects("99999999999999999999999", "out of range");
	} // end of aNumberBeyondLongRangeIsRejected method

	@Test
	void aValidUnicodeEscapeDecodes() throws Exception {
		assertEquals("é", JsonValue.parse(json("'\\u00e9'"))
				.asString());
	} // end of aValidUnicodeEscapeDecodes method
}
