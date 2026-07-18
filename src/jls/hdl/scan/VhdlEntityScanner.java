package jls.hdl.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hand-written scanner for VHDL entity headers (issue #63). It
 * extracts, for every entity declaration in a source text, the entity
 * name, the ports in declaration order with modes and evaluated bit
 * widths, and evaluated integer generic defaults. VHDL needs no
 * preprocessor, so this is simpler than
 * {@link VerilogHeaderScanner}: strip comments, find
 * {@code entity NAME is ... end}, read the {@code generic} and
 * {@code port} clauses. Supported port types are {@code std_logic},
 * {@code std_ulogic}, {@code bit}, {@code boolean} (1 bit) and
 * {@code std_logic_vector}, {@code std_ulogic_vector},
 * {@code bit_vector}, {@code signed}, {@code unsigned} with a
 * {@code (A downto B)} or {@code (A to B)} constraint whose bounds may
 * use generics and {@code + - * / mod rem **}.
 *
 * <p>Deliberately a scanner, not a parser (docs/hdl-support-research.md
 * &#167;7.3): record types, unconstrained ports, extended identifiers
 * and other out-of-subset constructs raise {@link HdlScanException}
 * with a one-line reason (issue #63 &#167;9).</p>
 */
public final class VhdlEntityScanner {

	/** Widest port the scanner will report; larger widths are junk. */
	private static final int MAX_BITS = 1_000_000;

	/** Port types that are one bit wide. */
	private static final Set<String> SCALAR_TYPES = Set.of("std_logic",
			"std_ulogic", "bit", "boolean");

	/** Port types that take an (A downto B) / (A to B) constraint. */
	private static final Set<String> VECTOR_TYPES = Set.of(
			"std_logic_vector", "std_ulogic_vector", "bit_vector",
			"signed", "unsigned");

	/** The token stream of the whole source. */
	private final List<Token> tokens;

	/** Cursor into {@link #tokens}. */
	private int pos;

	/**
	 * Private; use the static {@link #scan} entry point.
	 * @param tokens the token stream of the source
	 */
	private VhdlEntityScanner(List<Token> tokens) {
		this.tokens = tokens;
		this.pos = 0;
	} // end of VhdlEntityScanner constructor

	/**
	 * Scans VHDL source text for entity headers. Entity
	 * <em>instantiations</em> ({@code entity work.foo}) and component
	 * declarations are ignored; only entity declarations are reported.
	 * @param source the complete VHDL source text
	 * @return every entity declared, in file order (possibly empty)
	 * @throws HdlScanException if the file is outside the scanned
	 *         subset or malformed; the message is a one-line reason
	 */
	public static List<ScannedModule> scan(String source)
			throws HdlScanException {
		VhdlEntityScanner scanner = new VhdlEntityScanner(
				tokenize(stripComments(source)));
		return scanner.parseEntities();
	} // end of scan method

	// ------------------------------------------------------------------
	// comment stripping and tokenizing
	// ------------------------------------------------------------------

	/**
	 * Replaces {@code --} line comments and VHDL-2008 {@code /*}
	 * block comments with spaces, preserving newlines and strings.
	 * @param source the raw source text
	 * @return the source with comments blanked out
	 * @throws HdlScanException on an unterminated block comment/string
	 */
	private static String stripComments(String source)
			throws HdlScanException {
		StringBuilder out = new StringBuilder(source.length());
		int i = 0;
		int line = 1;
		while (i < source.length()) {
			char c = source.charAt(i);
			if (c == '\n') {
				line += 1;
				out.append(c);
				i += 1;
			} else if (c == '-' && i + 1 < source.length()
					&& source.charAt(i + 1) == '-') {
				while (i < source.length() && source.charAt(i) != '\n') {
					i += 1;
				}
			} else if (c == '/' && i + 1 < source.length()
					&& source.charAt(i + 1) == '*') {
				int startLine = line;
				i += 2;
				out.append("  ");
				while (true) {
					if (i >= source.length()) {
						throw new HdlScanException(
								"unterminated block comment", startLine);
					}
					char d = source.charAt(i);
					if (d == '*' && i + 1 < source.length()
							&& source.charAt(i + 1) == '/') {
						out.append("  ");
						i += 2;
						break;
					}
					out.append(d == '\n' ? '\n' : ' ');
					if (d == '\n') {
						line += 1;
					}
					i += 1;
				}
			} else if (c == '"') {
				int startLine = line;
				out.append(c);
				i += 1;
				while (true) {
					if (i >= source.length()) {
						throw new HdlScanException(
								"unterminated string literal", startLine);
					}
					char d = source.charAt(i);
					out.append(d);
					i += 1;
					if (d == '"') {
						// "" inside a string is an escaped quote
						if (i < source.length()
								&& source.charAt(i) == '"') {
							out.append('"');
							i += 1;
						} else {
							break;
						}
					}
				}
			} else {
				out.append(c);
				i += 1;
			}
		}
		return out.toString();
	} // end of stripComments method

	/** Token kinds the parser distinguishes. */
	private enum Kind {
		/** An identifier or keyword (VHDL is case-insensitive). */
		ID,
		/** An unsigned decimal digit run (underscores allowed). */
		NUM,
		/** Punctuation; {@code :=} is a single token. */
		PUNCT,
		/** A character literal like {@code '0'}, quotes included. */
		CHAR,
		/** A string literal, quotes included. */
		STRING
	} // end of Kind enum

	/** One token with its 1-based source line. */
	private static final class Token {
		/** What sort of token this is. */
		final Kind kind;
		/** The token text as written. */
		final String text;
		/** 1-based source line the token starts on. */
		final int line;

		/**
		 * Builds a token.
		 * @param kind what sort of token this is
		 * @param text the token text
		 * @param line 1-based source line
		 */
		Token(Kind kind, String text, int line) {
			this.kind = kind;
			this.text = text;
			this.line = line;
		}

		/**
		 * Case-insensitive keyword test.
		 * @param word the lower-case keyword to compare with
		 * @return true if this is an ID matching the keyword
		 */
		boolean isWord(String word) {
			return kind == Kind.ID
					&& text.toLowerCase(Locale.ROOT).equals(word);
		}
	} // end of Token class

	/**
	 * Splits VHDL text into tokens.
	 * @param text comment-free source text
	 * @return the token list
	 * @throws HdlScanException on out-of-subset characters
	 */
	private static List<Token> tokenize(String text)
			throws HdlScanException {
		List<Token> out = new ArrayList<Token>();
		int i = 0;
		int line = 1;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '\n') {
				line += 1;
				i += 1;
			} else if (Character.isWhitespace(c)) {
				i += 1;
			} else if (Character.isLetter(c)) {
				int start = i;
				while (i < text.length()
						&& (Character.isLetterOrDigit(text.charAt(i))
								|| text.charAt(i) == '_')) {
					i += 1;
				}
				out.add(new Token(Kind.ID, text.substring(start, i),
						line));
			} else if (Character.isDigit(c)) {
				int start = i;
				while (i < text.length()
						&& (Character.isDigit(text.charAt(i))
								|| text.charAt(i) == '_')) {
					i += 1;
				}
				out.add(new Token(Kind.NUM, text.substring(start, i),
						line));
			} else if (c == '\'' && i + 2 < text.length()
					&& text.charAt(i + 2) == '\'') {
				out.add(new Token(Kind.CHAR, text.substring(i, i + 3),
						line));
				i += 3;
			} else if (c == '"') {
				int start = i;
				i += 1;
				while (i < text.length() && text.charAt(i) != '"') {
					i += 1;
				}
				i += 1;
				out.add(new Token(Kind.STRING,
						text.substring(start, Math.min(i, text.length())),
						line));
			} else if (c == '\\') {
				throw new HdlScanException("extended identifiers"
						+ " (backslash names) are not supported by the"
						+ " header scanner", line);
			} else if (c == ':' && i + 1 < text.length()
					&& text.charAt(i + 1) == '=') {
				out.add(new Token(Kind.PUNCT, ":=", line));
				i += 2;
			} else {
				out.add(new Token(Kind.PUNCT, String.valueOf(c), line));
				i += 1;
			}
		}
		return out;
	} // end of tokenize method

	// ------------------------------------------------------------------
	// parsing
	// ------------------------------------------------------------------

	/**
	 * Walks the token stream collecting every entity declaration.
	 * @return the entities in file order
	 * @throws HdlScanException on malformed or out-of-subset entities
	 */
	private List<ScannedModule> parseEntities()
			throws HdlScanException {
		List<ScannedModule> entities = new ArrayList<ScannedModule>();
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			// only "entity NAME is" opens a declaration; direct
			// instantiation ("entity work.foo") has no "is" here
			if (t.isWord("entity") && pos + 2 < tokens.size()
					&& tokens.get(pos + 1).kind == Kind.ID
					&& tokens.get(pos + 2).isWord("is")) {
				pos += 3;
				entities.add(parseEntity(tokens.get(pos - 2).text));
			} else {
				pos += 1;
			}
		}
		return entities;
	} // end of parseEntities method

	/**
	 * Parses one entity from just after its {@code is} keyword to the
	 * {@code ;} closing its {@code end}.
	 * @param name the entity name
	 * @return the scanned entity
	 * @throws HdlScanException on malformed or out-of-subset content
	 */
	private ScannedModule parseEntity(String name)
			throws HdlScanException {
		Map<String, Long> generics = new LinkedHashMap<String, Long>();
		Map<String, Long> byLower = new LinkedHashMap<String, Long>();
		Set<String> unevaluable = new LinkedHashSet<String>();
		List<ScannedPort> ports = new ArrayList<ScannedPort>();
		boolean sawPorts = false;
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			if (t.isWord("generic") && pos + 1 < tokens.size()
					&& isPunct(tokens.get(pos + 1), "(")) {
				pos += 2;
				parseGenerics(collectBalanced(")"), generics, byLower,
						unevaluable);
			} else if (t.isWord("port") && pos + 1 < tokens.size()
					&& isPunct(tokens.get(pos + 1), "(")) {
				if (sawPorts) {
					throw new HdlScanException("entity '" + name
							+ "' has two port clauses", t.line);
				}
				sawPorts = true;
				pos += 2;
				parsePorts(collectBalanced(")"), byLower, unevaluable,
						ports);
			} else if (t.isWord("end")) {
				while (pos < tokens.size()
						&& !isPunct(tokens.get(pos), ";")) {
					pos += 1;
				}
				if (pos < tokens.size()) {
					pos += 1;
				}
				return new ScannedModule(name, ports, generics);
			} else {
				pos += 1;
			}
		}
		throw new HdlScanException(
				"missing end for entity '" + name + "'");
	} // end of parseEntity method

	/**
	 * Parses a generic clause slice: entries separated by top-level
	 * semicolons, each {@code names : type [:= default]}.
	 * @param slice the tokens between the parentheses
	 * @param generics evaluated defaults as written, filled in place
	 * @param byLower the same values keyed by lower-cased name, for
	 *        case-insensitive lookup in width expressions
	 * @param unevaluable lower-cased names whose defaults failed to
	 *        evaluate (or that have none), extended in place
	 * @throws HdlScanException on malformed entries
	 */
	private static void parseGenerics(List<Token> slice,
			Map<String, Long> generics, Map<String, Long> byLower,
			Set<String> unevaluable) throws HdlScanException {
		for (List<Token> entry : splitTopLevel(slice, ";")) {
			int colon = findTopLevel(entry, ":");
			int line = entry.isEmpty() ? 0 : entry.get(0).line;
			if (colon < 0) {
				throw new HdlScanException(
						"malformed generic declaration", line);
			}
			List<String> names = parseNameList(entry.subList(0, colon));
			int assign = findTopLevel(entry, ":=");
			for (String genericName : names) {
				String lower = genericName.toLowerCase(Locale.ROOT);
				if (assign < 0) {
					unevaluable.add(lower);
					continue;
				}
				try {
					long value = new Eval(
							entry.subList(assign + 1, entry.size()),
							byLower, unevaluable).parse();
					generics.put(genericName, value);
					byLower.put(lower, value);
				} catch (HdlScanException e) {
					// only fatal if a port width later needs it
					unevaluable.add(lower);
				}
			}
		}
	} // end of parseGenerics method

	/**
	 * Parses a port clause slice: entries separated by top-level
	 * semicolons, each {@code names : [mode] type [:= default]}.
	 * @param slice the tokens between the parentheses
	 * @param generics evaluated generics keyed by lower-cased name
	 * @param unevaluable lower-cased generics without usable values
	 * @param ports the port list, filled in place in declaration order
	 * @throws HdlScanException on malformed or out-of-subset entries
	 */
	private static void parsePorts(List<Token> slice,
			Map<String, Long> generics, Set<String> unevaluable,
			List<ScannedPort> ports) throws HdlScanException {
		for (List<Token> entry : splitTopLevel(slice, ";")) {
			int colon = findTopLevel(entry, ":");
			int line = entry.isEmpty() ? 0 : entry.get(0).line;
			if (colon < 0) {
				throw new HdlScanException(
						"malformed port declaration", line);
			}
			List<String> names = parseNameList(entry.subList(0, colon));
			int j = colon + 1;
			if (j >= entry.size()) {
				throw new HdlScanException(
						"port declaration without a type", line);
			}
			// mode defaults to "in" when omitted
			ScannedPort.Direction direction = ScannedPort.Direction.IN;
			Token modeTok = entry.get(j);
			if (modeTok.isWord("in")) {
				j += 1;
			} else if (modeTok.isWord("out")
					|| modeTok.isWord("buffer")) {
				direction = ScannedPort.Direction.OUT;
				j += 1;
			} else if (modeTok.isWord("inout")) {
				direction = ScannedPort.Direction.INOUT;
				j += 1;
			} else if (modeTok.isWord("linkage")) {
				throw new HdlScanException("linkage ports are not"
						+ " supported by the header scanner",
						modeTok.line);
			}
			int bits = parseType(entry, j, generics, unevaluable);
			for (String portName : names) {
				ports.add(new ScannedPort(portName, direction, bits));
			}
		}
	} // end of parsePorts method

	/**
	 * Parses the type mark (and constraint) of one port entry.
	 * @param entry the port entry tokens
	 * @param j the index of the type mark
	 * @param generics evaluated generics keyed by lower-cased name
	 * @param unevaluable lower-cased generics without usable values
	 * @return the width in bits
	 * @throws HdlScanException on out-of-subset types
	 */
	private static int parseType(List<Token> entry, int j,
			Map<String, Long> generics, Set<String> unevaluable)
			throws HdlScanException {
		if (j >= entry.size() || entry.get(j).kind != Kind.ID) {
			int line = entry.isEmpty() ? 0
					: entry.get(Math.min(j, entry.size() - 1)).line;
			throw new HdlScanException(
					"port declaration without a type", line);
		}
		Token typeTok = entry.get(j);
		String type = typeTok.text.toLowerCase(Locale.ROOT);
		if (SCALAR_TYPES.contains(type)) {
			checkTrailer(entry, j + 1);
			return 1;
		}
		if (!VECTOR_TYPES.contains(type)) {
			throw new HdlScanException("port type '" + typeTok.text
					+ "' is not supported by the header scanner; use"
					+ " std_logic/std_logic_vector or the Yosys path",
					typeTok.line);
		}
		j += 1;
		if (j >= entry.size() || !isPunct(entry.get(j), "(")) {
			throw new HdlScanException("unconstrained port type '"
					+ typeTok.text + "' is not supported by the header"
					+ " scanner", typeTok.line);
		}
		int close = matchParen(entry, j);
		List<Token> range = entry.subList(j + 1, close);
		checkTrailer(entry, close + 1);
		int split = -1;
		boolean downto = false;
		int depth = 0;
		for (int k = 0; k < range.size(); k += 1) {
			Token t = range.get(k);
			if (isPunct(t, "(")) {
				depth += 1;
			} else if (isPunct(t, ")")) {
				depth -= 1;
			} else if (depth == 0
					&& (t.isWord("downto") || t.isWord("to"))) {
				split = k;
				downto = t.isWord("downto");
				break;
			}
		}
		if (split < 0) {
			throw new HdlScanException(
					"expected (A downto B) or (A to B) constraint",
					typeTok.line);
		}
		long left = new Eval(range.subList(0, split), generics,
				unevaluable).parse();
		long right = new Eval(range.subList(split + 1, range.size()),
				generics, unevaluable).parse();
		long bits = downto ? left - right + 1 : right - left + 1;
		if (bits < 1 || bits > MAX_BITS) {
			throw new HdlScanException("range (" + left
					+ (downto ? " downto " : " to ") + right
					+ ") gives an unreasonable width", typeTok.line);
		}
		return (int) bits;
	} // end of parseType method

	/**
	 * Checks that whatever follows a port's type is only a default
	 * value ({@code := ...}) or nothing.
	 * @param entry the port entry tokens
	 * @param j the index just past the type
	 * @throws HdlScanException on trailing junk
	 */
	private static void checkTrailer(List<Token> entry, int j)
			throws HdlScanException {
		if (j < entry.size() && !isPunct(entry.get(j), ":=")) {
			throw new HdlScanException("unexpected '"
					+ entry.get(j).text + "' after port type",
					entry.get(j).line);
		}
	} // end of checkTrailer method

	/**
	 * Parses a comma-separated identifier list.
	 * @param slice the tokens before the colon
	 * @return the names in order
	 * @throws HdlScanException if the list is malformed
	 */
	private static List<String> parseNameList(List<Token> slice)
			throws HdlScanException {
		List<String> names = new ArrayList<String>();
		boolean expectName = true;
		for (Token t : slice) {
			if (expectName) {
				if (t.kind != Kind.ID) {
					throw new HdlScanException("expected a name but"
							+ " found '" + t.text + "'", t.line);
				}
				names.add(t.text);
				expectName = false;
			} else {
				if (!isPunct(t, ",")) {
					throw new HdlScanException("expected ',' but"
							+ " found '" + t.text + "'", t.line);
				}
				expectName = true;
			}
		}
		if (names.isEmpty() || expectName) {
			int line = slice.isEmpty() ? 0 : slice.get(0).line;
			throw new HdlScanException("malformed name list", line);
		}
		return names;
	} // end of parseNameList method

	// ------------------------------------------------------------------
	// token-stream helpers
	// ------------------------------------------------------------------

	/**
	 * @param t the token to test
	 * @param punct the punctuation text
	 * @return true if t is that punctuation
	 */
	private static boolean isPunct(Token t, String punct) {
		return t.kind == Kind.PUNCT && t.text.equals(punct);
	} // end of isPunct method

	/**
	 * Collects tokens up to (excluding) the ")" balancing the
	 * already-consumed "("; consumes the ")".
	 * @param closer the closing punctuation, always ")"
	 * @return the collected slice
	 * @throws HdlScanException if the stream ends first
	 */
	private List<Token> collectBalanced(String closer)
			throws HdlScanException {
		List<Token> slice = new ArrayList<Token>();
		int depth = 0;
		int startLine = pos < tokens.size() ? tokens.get(pos).line : 0;
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			if (t.kind == Kind.PUNCT) {
				if (depth == 0 && t.text.equals(closer)) {
					pos += 1;
					return slice;
				}
				if (t.text.equals("(")) {
					depth += 1;
				} else if (t.text.equals(")")) {
					depth -= 1;
				}
			}
			slice.add(t);
			pos += 1;
		}
		throw new HdlScanException("missing '" + closer + "'",
				startLine);
	} // end of collectBalanced method

	/**
	 * Splits a slice on a top-level separator.
	 * @param slice the tokens to split
	 * @param separator the separating punctuation, e.g. ";"
	 * @return the separated entries, in order
	 */
	private static List<List<Token>> splitTopLevel(List<Token> slice,
			String separator) {
		List<List<Token>> entries = new ArrayList<List<Token>>();
		List<Token> current = new ArrayList<Token>();
		int depth = 0;
		for (Token t : slice) {
			if (t.kind == Kind.PUNCT) {
				if (t.text.equals("(")) {
					depth += 1;
				} else if (t.text.equals(")")) {
					depth -= 1;
				} else if (depth == 0 && t.text.equals(separator)) {
					entries.add(current);
					current = new ArrayList<Token>();
					continue;
				}
			}
			current.add(t);
		}
		if (!current.isEmpty() || !entries.isEmpty()) {
			entries.add(current);
		}
		return entries;
	} // end of splitTopLevel method

	/**
	 * Finds a punctuation token at paren depth zero.
	 * @param slice the tokens to search
	 * @param punct the punctuation to find
	 * @return the index, or -1 if absent
	 */
	private static int findTopLevel(List<Token> slice, String punct) {
		int depth = 0;
		for (int j = 0; j < slice.size(); j += 1) {
			Token t = slice.get(j);
			if (t.kind == Kind.PUNCT) {
				if (t.text.equals("(")) {
					depth += 1;
				} else if (t.text.equals(")")) {
					depth -= 1;
				} else if (depth == 0 && t.text.equals(punct)) {
					return j;
				}
			}
		}
		return -1;
	} // end of findTopLevel method

	/**
	 * Finds the ")" matching the "(" at the given index.
	 * @param slice the tokens to search
	 * @param open the index of the "(" token
	 * @return the index of the matching ")"
	 * @throws HdlScanException if there is none
	 */
	private static int matchParen(List<Token> slice, int open)
			throws HdlScanException {
		int depth = 0;
		for (int j = open; j < slice.size(); j += 1) {
			Token t = slice.get(j);
			if (isPunct(t, "(")) {
				depth += 1;
			} else if (isPunct(t, ")")) {
				depth -= 1;
				if (depth == 0) {
					return j;
				}
			}
		}
		throw new HdlScanException("missing ')'",
				slice.get(open).line);
	} // end of matchParen method

	// ------------------------------------------------------------------
	// constant-expression evaluation
	// ------------------------------------------------------------------

	/**
	 * A tiny recursive-descent evaluator for VHDL constant integer
	 * expressions: decimal literals, generic names (case-insensitive),
	 * {@code + - * / mod rem **}, unary sign, parentheses.
	 */
	private static final class Eval {

		/** The expression tokens. */
		private final List<Token> toks;
		/** Evaluated generics keyed by lower-cased name. */
		private final Map<String, Long> generics;
		/** Lower-cased generics without usable values. */
		private final Set<String> unevaluable;
		/** Cursor into {@link #toks}. */
		private int at;

		/**
		 * Builds an evaluator over a token slice.
		 * @param toks the expression tokens
		 * @param generics evaluated generics keyed by lower-cased name
		 * @param unevaluable lower-cased generics without values
		 */
		Eval(List<Token> toks, Map<String, Long> generics,
				Set<String> unevaluable) {
			this.toks = toks;
			this.generics = generics;
			this.unevaluable = unevaluable;
			this.at = 0;
		} // end of Eval constructor

		/**
		 * Evaluates the whole slice as one expression.
		 * @return the expression value
		 * @throws HdlScanException if the expression is outside the
		 *         evaluator's subset
		 */
		long parse() throws HdlScanException {
			if (toks.isEmpty()) {
				throw new HdlScanException("empty expression");
			}
			long v = addSub();
			if (at < toks.size()) {
				throw new HdlScanException("cannot evaluate '"
						+ toks.get(at).text + "' in constant expression",
						toks.get(at).line);
			}
			return v;
		} // end of parse method

		/**
		 * Parses additive expressions.
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long addSub() throws HdlScanException {
			long v = mulDiv();
			while (at < toks.size() && (isPunct(toks.get(at), "+")
					|| isPunct(toks.get(at), "-"))) {
				boolean plus = toks.get(at).text.equals("+");
				at += 1;
				long r = mulDiv();
				v = plus ? v + r : v - r;
			}
			return v;
		} // end of addSub method

		/**
		 * Parses multiplicative expressions including
		 * {@code mod}/{@code rem}.
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long mulDiv() throws HdlScanException {
			long v = power();
			while (at < toks.size()) {
				Token t = toks.get(at);
				boolean mod = t.isWord("mod");
				boolean rem = t.isWord("rem");
				if (isPunct(t, "*") && !(at + 1 < toks.size()
						&& isPunct(toks.get(at + 1), "*"))) {
					at += 1;
					v *= power();
				} else if (isPunct(t, "/") || mod || rem) {
					at += 1;
					long r = power();
					if (r == 0) {
						throw new HdlScanException(
								"division by zero in width expression",
								t.line);
					}
					if (mod) {
						v = Math.floorMod(v, r);
					} else if (rem) {
						v = v % r;
					} else {
						v = v / r;
					}
				} else {
					break;
				}
			}
			return v;
		} // end of mulDiv method

		/**
		 * Parses power expressions ({@code **}, right-associative).
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long power() throws HdlScanException {
			long base = unary();
			if (at + 1 < toks.size() && isPunct(toks.get(at), "*")
					&& isPunct(toks.get(at + 1), "*")) {
				int line = toks.get(at).line;
				at += 2;
				long exp = power();
				if (exp < 0 || exp > 62) {
					throw new HdlScanException(
							"unreasonable exponent in width expression",
							line);
				}
				long v = 1;
				for (long e = 0; e < exp; e += 1) {
					v *= base;
				}
				return v;
			}
			return base;
		} // end of power method

		/**
		 * Parses unary sign.
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long unary() throws HdlScanException {
			if (at < toks.size() && isPunct(toks.get(at), "-")) {
				at += 1;
				return -unary();
			}
			if (at < toks.size() && isPunct(toks.get(at), "+")) {
				at += 1;
				return unary();
			}
			return primary();
		} // end of unary method

		/**
		 * Parses a primary: literal, generic name, or parentheses.
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long primary() throws HdlScanException {
			if (at >= toks.size()) {
				throw new HdlScanException(
						"truncated constant expression");
			}
			Token t = toks.get(at);
			if (t.kind == Kind.NUM) {
				at += 1;
				return Long.parseLong(t.text.replace("_", ""));
			}
			if (isPunct(t, "(")) {
				at += 1;
				long v = addSub();
				if (at >= toks.size() || !isPunct(toks.get(at), ")")) {
					throw new HdlScanException(
							"missing ')' in constant expression",
							t.line);
				}
				at += 1;
				return v;
			}
			if (t.kind == Kind.ID) {
				String lower = t.text.toLowerCase(Locale.ROOT);
				if (generics.containsKey(lower)) {
					at += 1;
					return generics.get(lower);
				}
				if (unevaluable.contains(lower)) {
					throw new HdlScanException("generic '" + t.text
							+ "' has no default the scanner can"
							+ " evaluate", t.line);
				}
				throw new HdlScanException("unknown name '" + t.text
						+ "' in constant expression", t.line);
			}
			throw new HdlScanException("cannot evaluate '" + t.text
					+ "' in constant expression", t.line);
		} // end of primary method

	} // end of Eval class

} // end of VhdlEntityScanner class
