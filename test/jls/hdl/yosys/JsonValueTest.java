package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The strict JSON subset parser behind netlist reading (issue #61):
 * well-formed documents decode with order preserved, and every
 * malformed shape a generated-but-untrusted file could take is
 * rejected with a located message rather than mis-read.
 */
class JsonValueTest {

	/** Fixtures read better with single quotes; swap them for real ones. */
	private static String json(String singleQuoted) {
		return singleQuoted.replace('\'', '"');
	}

	@Test
	void parsesNestedStructure() throws Exception {
		JsonValue root = JsonValue.parse(json(
				"{ 'a': [1, -2, 'three'], 'b': { 'c': 4 } }"));
		assertTrue(root.isObject());
		JsonValue a = root.asObject().get("a");
		assertTrue(a.isArray());
		assertEquals(1, a.asArray().get(0).asLong());
		assertEquals(-2, a.asArray().get(1).asLong());
		assertEquals("three", a.asArray().get(2).asString());
		JsonValue c = root.asObject().get("b").asObject().get("c");
		assertEquals(4, c.asLong());
	}

	@Test
	void preservesObjectMemberOrder() throws Exception {
		JsonValue root = JsonValue.parse(json(
				"{ 'z': 1, 'a': 2, 'm': 3 }"));
		List<String> names =
				new ArrayList<String>(root.asObject().keySet());
		assertEquals(List.of("z", "a", "m"), names,
				"document order matters for deterministic imports");
	}

	@Test
	void decodesStringEscapes() throws Exception {
		JsonValue root = JsonValue.parse(json(
				"'a\\n\\t\\\\ \\u0041 \\' done'"));
		assertEquals("a\n\t\\ A \" done", root.asString());
	}

	@Test
	void parsesEmptyContainers() throws Exception {
		assertTrue(JsonValue.parse("{}").asObject().isEmpty());
		assertTrue(JsonValue.parse("[]").asArray().isEmpty());
	}

	@Test
	void rejectsTrailingGarbage() {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse("{} extra"));
		assertTrue(e.getMessage().contains("after the JSON value"),
				e.getMessage());
	}

	@Test
	void rejectsNonIntegerNumbers() {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse("[1.5]"));
		assertTrue(e.getMessage().contains("non-integer"),
				e.getMessage());
		assertThrows(NetlistFormatException.class,
				() -> JsonValue.parse("[1e3]"));
	}

	@Test
	void rejectsDuplicateMemberNames() {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse(json("{ 'a': 1, 'a': 2 }")));
		assertTrue(e.getMessage().contains("duplicate"),
				e.getMessage());
	}

	@Test
	void rejectsUnterminatedString() {
		assertThrows(NetlistFormatException.class,
				() -> JsonValue.parse(json("{ 'a")));
	}

	@Test
	void rejectsHostileNestingDepth() {
		StringBuilder deep = new StringBuilder();
		for (int i = 0; i < 300; i += 1) {
			deep.append('[');
		}
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse(deep.toString()));
		assertTrue(e.getMessage().contains("nesting"),
				e.getMessage());
	}

	@Test
	void errorsCarryLineAndColumn() {
		NetlistFormatException e = assertThrows(
				NetlistFormatException.class,
				() -> JsonValue.parse(json("{\n  'a': }")));
		assertTrue(e.getMessage().contains("line 2"), e.getMessage());
	}

	@Test
	void typedAccessorsGuardTheKind() throws Exception {
		JsonValue array = JsonValue.parse("[]");
		assertThrows(IllegalStateException.class, array::asObject);
		assertThrows(IllegalStateException.class, array::asString);
		assertThrows(IllegalStateException.class, array::asLong);
	}

} // end of JsonValueTest class
