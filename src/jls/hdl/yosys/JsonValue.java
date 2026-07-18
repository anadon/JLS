package jls.hdl.yosys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable JSON value with a strict parser, sized exactly to what
 * Yosys {@code write_json} emits (issue #61). JLS deliberately carries
 * no JSON library dependency, and the netlist subset is small: objects
 * (insertion-ordered, duplicate keys rejected), arrays, strings with
 * the standard escapes, integer numbers, and the {@code true} /
 * {@code false} / {@code null} literals. Non-integer numbers (fraction
 * or exponent forms) are rejected with a clear message — they never
 * occur in a netlist, and a bit index or parameter that arrived as
 * {@code 1.5} is a malformed file, not data to round. Netlists are
 * untrusted input, so nesting depth is capped rather than letting a
 * hostile file overflow the parse stack.
 *
 * Accessors are typed and unforgiving: asking an array for its object
 * entries throws {@link IllegalStateException}. Callers that face
 * user-supplied files ({@link jls.hdl.yosys.YosysNetlist}) test the
 * kind first and raise {@link jls.hdl.yosys.NetlistFormatException}
 * with path context; the hard throw here only guards coding errors.
 */
public final class JsonValue {

	/** Nesting depth beyond which parsing fails (hostile-input cap). */
	private static final int MAX_DEPTH = 200;

	/** The kinds of JSON value. */
	private enum Kind {
		OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
	} // end of Kind enum

	/** Which kind of value this is. */
	private final Kind kind;

	/**
	 * The payload: a {@code Map<String, JsonValue>} for objects, a
	 * {@code List<JsonValue>} for arrays, a {@code String}, a
	 * {@code Long}, a {@code Boolean}, or null for the null literal.
	 */
	private final Object value;

	/**
	 * Creates a value; instances only come from the parser.
	 *
	 * @param kind Which kind of value this is.
	 * @param value The payload for that kind.
	 */
	private JsonValue(Kind kind, Object value) {

		this.kind = kind;
		this.value = value;
	} // end of constructor

	/**
	 * Parses one complete JSON document.
	 *
	 * @param text The document text.
	 *
	 * @return the root value.
	 *
	 * @throws NetlistFormatException if the text is not exactly one
	 * well-formed JSON value in the accepted subset; the message
	 * carries the 1-based line and column of the problem.
	 */
	public static JsonValue parse(String text)
			throws NetlistFormatException {

		Parser parser = new Parser(text);
		JsonValue root = parser.parseValue(0);
		parser.skipWhitespace();
		if (!parser.atEnd()) {
			throw parser.error("unexpected text after the JSON value");
		}
		return root;
	} // end of parse method

	/**
	 * Reports whether this value is an object.
	 *
	 * @return true when this value is an object.
	 */
	public boolean isObject() {

		return kind == Kind.OBJECT;
	} // end of isObject method

	/**
	 * Reports whether this value is an array.
	 *
	 * @return true when this value is an array.
	 */
	public boolean isArray() {

		return kind == Kind.ARRAY;
	} // end of isArray method

	/**
	 * Reports whether this value is a string.
	 *
	 * @return true when this value is a string.
	 */
	public boolean isString() {

		return kind == Kind.STRING;
	} // end of isString method

	/**
	 * Reports whether this value is an (integer) number.
	 *
	 * @return true when this value is a number.
	 */
	public boolean isNumber() {

		return kind == Kind.NUMBER;
	} // end of isNumber method

	/**
	 * The members of this object, in document order.
	 *
	 * @return an unmodifiable name-to-value map.
	 *
	 * @throws IllegalStateException if this value is not an object.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, JsonValue> asObject() {

		if (kind != Kind.OBJECT) {
			throw new IllegalStateException(
					"not a JSON object: " + kind);
		}
		return Collections.unmodifiableMap(
				(Map<String, JsonValue>) value);
	} // end of asObject method

	/**
	 * The elements of this array, in document order.
	 *
	 * @return an unmodifiable element list.
	 *
	 * @throws IllegalStateException if this value is not an array.
	 */
	@SuppressWarnings("unchecked")
	public List<JsonValue> asArray() {

		if (kind != Kind.ARRAY) {
			throw new IllegalStateException(
					"not a JSON array: " + kind);
		}
		return Collections.unmodifiableList((List<JsonValue>) value);
	} // end of asArray method

	/**
	 * This value as a string.
	 *
	 * @return the string payload.
	 *
	 * @throws IllegalStateException if this value is not a string.
	 */
	public String asString() {

		if (kind != Kind.STRING) {
			throw new IllegalStateException(
					"not a JSON string: " + kind);
		}
		return (String) value;
	} // end of asString method

	/**
	 * This value as an integer.
	 *
	 * @return the number payload.
	 *
	 * @throws IllegalStateException if this value is not a number.
	 */
	public long asLong() {

		if (kind != Kind.NUMBER) {
			throw new IllegalStateException(
					"not a JSON number: " + kind);
		}
		return ((Long) value).longValue();
	} // end of asLong method

	/**
	 * The recursive-descent parser. One instance parses one document;
	 * position is tracked as a plain index and converted to line and
	 * column only when constructing an error.
	 */
	private static final class Parser {

		/** The document being parsed. */
		private final String text;

		/** The current position, as a 0-based index into text. */
		private int pos = 0;

		/**
		 * Creates a parser over one document.
		 *
		 * @param text The document text.
		 */
		Parser(String text) {

			this.text = text;
		} // end of constructor

		/**
		 * Parses one value starting at the current position.
		 *
		 * @param depth The current nesting depth, for the cap.
		 *
		 * @return the parsed value.
		 *
		 * @throws NetlistFormatException on any syntax error.
		 */
		JsonValue parseValue(int depth) throws NetlistFormatException {

			if (depth > MAX_DEPTH) {
				throw error("nesting deeper than " + MAX_DEPTH
						+ " levels; refusing the file");
			}
			skipWhitespace();
			if (atEnd()) {
				throw error("unexpected end of input");
			}
			char c = text.charAt(pos);
			switch (c) {
			case '{':
				return parseObject(depth);
			case '[':
				return parseArray(depth);
			case '"':
				return new JsonValue(Kind.STRING, parseString());
			case 't':
				expectWord("true");
				return new JsonValue(Kind.BOOLEAN, Boolean.TRUE);
			case 'f':
				expectWord("false");
				return new JsonValue(Kind.BOOLEAN, Boolean.FALSE);
			case 'n':
				expectWord("null");
				return new JsonValue(Kind.NULL, null);
			default:
				if (c == '-' || (c >= '0' && c <= '9')) {
					return parseNumber();
				}
				throw error("unexpected character '" + c + "'");
			}
		} // end of parseValue method

		/**
		 * Parses an object; the current character is '{'.
		 *
		 * @param depth The current nesting depth.
		 *
		 * @return the parsed object.
		 *
		 * @throws NetlistFormatException on any syntax error, and on
		 * a duplicate member name — Yosys never emits one, and
		 * silently keeping the last value would hide corruption.
		 */
		private JsonValue parseObject(int depth)
				throws NetlistFormatException {

			pos += 1;
			Map<String, JsonValue> members =
					new LinkedHashMap<String, JsonValue>();
			skipWhitespace();
			if (!atEnd() && text.charAt(pos) == '}') {
				pos += 1;
				return new JsonValue(Kind.OBJECT, members);
			}
			while (true) {
				skipWhitespace();
				if (atEnd() || text.charAt(pos) != '"') {
					throw error("expected a member name string");
				}
				String name = parseString();
				skipWhitespace();
				if (atEnd() || text.charAt(pos) != ':') {
					throw error("expected ':' after member name");
				}
				pos += 1;
				JsonValue member = parseValue(depth + 1);
				if (members.put(name, member) != null) {
					throw error("duplicate member name \"" + name
							+ "\"");
				}
				skipWhitespace();
				if (atEnd()) {
					throw error("unterminated object");
				}
				char c = text.charAt(pos);
				if (c == ',') {
					pos += 1;
					continue;
				}
				if (c == '}') {
					pos += 1;
					return new JsonValue(Kind.OBJECT, members);
				}
				throw error("expected ',' or '}' in object");
			}
		} // end of parseObject method

		/**
		 * Parses an array; the current character is '['.
		 *
		 * @param depth The current nesting depth.
		 *
		 * @return the parsed array.
		 *
		 * @throws NetlistFormatException on any syntax error.
		 */
		private JsonValue parseArray(int depth)
				throws NetlistFormatException {

			pos += 1;
			List<JsonValue> elements = new ArrayList<JsonValue>();
			skipWhitespace();
			if (!atEnd() && text.charAt(pos) == ']') {
				pos += 1;
				return new JsonValue(Kind.ARRAY, elements);
			}
			while (true) {
				elements.add(parseValue(depth + 1));
				skipWhitespace();
				if (atEnd()) {
					throw error("unterminated array");
				}
				char c = text.charAt(pos);
				if (c == ',') {
					pos += 1;
					continue;
				}
				if (c == ']') {
					pos += 1;
					return new JsonValue(Kind.ARRAY, elements);
				}
				throw error("expected ',' or ']' in array");
			}
		} // end of parseArray method

		/**
		 * Parses a string; the current character is '"'.
		 *
		 * @return the decoded string content.
		 *
		 * @throws NetlistFormatException on an unterminated string, a
		 * bad escape, or a raw control character.
		 */
		private String parseString() throws NetlistFormatException {

			pos += 1;
			StringBuilder sb = new StringBuilder();
			while (true) {
				if (atEnd()) {
					throw error("unterminated string");
				}
				char c = text.charAt(pos);
				pos += 1;
				if (c == '"') {
					return sb.toString();
				}
				if (c == '\\') {
					sb.append(parseEscape());
				}
				else if (c < 0x20) {
					pos -= 1;
					throw error("raw control character in string");
				}
				else {
					sb.append(c);
				}
			}
		} // end of parseString method

		/**
		 * Parses one escape sequence; the backslash is consumed.
		 *
		 * @return the character the escape denotes.
		 *
		 * @throws NetlistFormatException on an unknown escape or a
		 * malformed \\u sequence.
		 */
		private char parseEscape() throws NetlistFormatException {

			if (atEnd()) {
				throw error("unterminated string");
			}
			char c = text.charAt(pos);
			pos += 1;
			switch (c) {
			case '"':
				return '"';
			case '\\':
				return '\\';
			case '/':
				return '/';
			case 'b':
				return '\b';
			case 'f':
				return '\f';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\t';
			case 'u':
				if (pos + 4 > text.length()) {
					throw error("truncated \\u escape");
				}
				String hex = text.substring(pos, pos + 4);
				int code;
				try {
					code = Integer.parseInt(hex, 16);
				}
				catch (NumberFormatException e) {
					throw error("bad \\u escape \"\\u" + hex + "\"");
				}
				pos += 4;
				return (char) code;
			default:
				pos -= 1;
				throw error("unknown escape '\\" + c + "'");
			}
		} // end of parseEscape method

		/**
		 * Parses a number; the current character starts one. Only
		 * integers are accepted (see the class comment).
		 *
		 * @return the parsed number value.
		 *
		 * @throws NetlistFormatException on a malformed number, a
		 * fraction/exponent form, or a value outside long range.
		 */
		private JsonValue parseNumber() throws NetlistFormatException {

			int start = pos;
			if (text.charAt(pos) == '-') {
				pos += 1;
			}
			int digits = 0;
			while (!atEnd() && text.charAt(pos) >= '0'
					&& text.charAt(pos) <= '9') {
				pos += 1;
				digits += 1;
			}
			if (digits == 0) {
				pos = start;
				throw error("malformed number");
			}
			if (!atEnd() && (text.charAt(pos) == '.'
					|| text.charAt(pos) == 'e'
					|| text.charAt(pos) == 'E')) {
				pos = start;
				throw error("non-integer number; a Yosys netlist"
						+ " contains only integers");
			}
			String token = text.substring(start, pos);
			try {
				return new JsonValue(Kind.NUMBER,
						Long.valueOf(Long.parseLong(token)));
			}
			catch (NumberFormatException e) {
				pos = start;
				throw error("number out of range: " + token);
			}
		} // end of parseNumber method

		/**
		 * Consumes an exact literal word (true/false/null).
		 *
		 * @param word The expected word at the current position.
		 *
		 * @throws NetlistFormatException if the text differs.
		 */
		private void expectWord(String word)
				throws NetlistFormatException {

			if (!text.startsWith(word, pos)) {
				throw error("unexpected characters (expected \""
						+ word + "\")");
			}
			pos += word.length();
		} // end of expectWord method

		/** Skips JSON whitespace (space, tab, newline, return). */
		void skipWhitespace() {

			while (!atEnd()) {
				char c = text.charAt(pos);
				if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
					return;
				}
				pos += 1;
			}
		} // end of skipWhitespace method

		/**
		 * Reports whether the whole document has been consumed.
		 *
		 * @return true at end of input.
		 */
		boolean atEnd() {

			return pos >= text.length();
		} // end of atEnd method

		/**
		 * Builds a syntax error carrying the 1-based line and column
		 * of the current position.
		 *
		 * @param what The description of the problem.
		 *
		 * @return the exception, ready to throw.
		 */
		NetlistFormatException error(String what) {

			int line = 1;
			int column = 1;
			for (int i = 0; i < pos && i < text.length(); i += 1) {
				if (text.charAt(i) == '\n') {
					line += 1;
					column = 1;
				}
				else {
					column += 1;
				}
			}
			return new NetlistFormatException("netlist JSON, line "
					+ line + ", column " + column + ": " + what);
		} // end of error method

	} // end of Parser class

} // end of JsonValue class
