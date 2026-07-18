package jls.hdl.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hand-written scanner for Verilog module headers (issue #63). It
 * extracts, for every module in a source text, the module name, the
 * ports in declaration order with directions and evaluated bit widths
 * (both ANSI {@code module m(input [7:0] a, ...)} and non-ANSI
 * {@code module m(a, ...); input [7:0] a;} styles), and evaluated
 * parameter defaults. A minimal preprocessor handles object-like
 * {@code `define}/{@code `undef}, {@code `ifdef}/{@code `ifndef}/
 * {@code `elsif}/{@code `else}/{@code `endif}, and comments; width
 * expressions may use parameters, macros, decimal and based literals,
 * {@code + - * / % **}, parentheses, and {@code $clog2}.
 *
 * <p>This is deliberately a scanner, not a parser
 * (docs/hdl-support-research.md &#167;7.3): anything outside that
 * subset - function-like macros, {@code `include}, escaped
 * identifiers, array ports, SystemVerilog {@code .name()} port lists -
 * raises {@link HdlScanException} with a one-line reason, and the file
 * routes to the external Yosys {@code write_json} extraction path
 * instead (issue #63 &#167;9).</p>
 */
public final class VerilogHeaderScanner {

	/** Widest port the scanner will report; larger widths are junk. */
	private static final int MAX_BITS = 1_000_000;

	/** Macro-expansion passes allowed per line before assuming a loop. */
	private static final int MAX_EXPANSIONS = 200;

	/** Net-type and modifier keywords skipped inside declarations. */
	private static final Set<String> NET_TYPES = Set.of("wire", "reg",
			"logic", "tri", "tri0", "tri1", "wand", "wor", "uwire",
			"supply0", "supply1", "var", "signed", "unsigned");

	/** Parameter data-type keywords skipped inside parameter lists. */
	private static final Set<String> PARAM_TYPES = Set.of("integer",
			"int", "longint", "shortint", "byte", "logic", "reg", "wire",
			"time", "real", "realtime", "signed", "unsigned");

	/** Directives that carry no port information and are ignored. */
	private static final Set<String> IGNORED_DIRECTIVES = Set.of(
			"timescale", "default_nettype", "resetall", "celldefine",
			"endcelldefine", "begin_keywords", "end_keywords",
			"pragma", "line");

	/** The token stream of the whole preprocessed source. */
	private final List<Token> tokens;

	/** Cursor into {@link #tokens}. */
	private int pos;

	/**
	 * Private; use the static {@link #scan} entry point.
	 * @param tokens the token stream of the preprocessed source
	 */
	private VerilogHeaderScanner(List<Token> tokens) {
		this.tokens = tokens;
		this.pos = 0;
	} // end of VerilogHeaderScanner constructor

	/**
	 * Scans Verilog source text for module headers.
	 * @param source the complete Verilog source text
	 * @return every module found, in file order (possibly empty)
	 * @throws HdlScanException if the file is outside the scanned
	 *         subset or malformed; the message is a one-line reason
	 */
	public static List<ScannedModule> scan(String source)
			throws HdlScanException {
		String preprocessed = preprocess(stripComments(source));
		VerilogHeaderScanner scanner =
				new VerilogHeaderScanner(tokenize(preprocessed));
		return scanner.parseModules();
	} // end of scan method

	// ------------------------------------------------------------------
	// phase 1: comment stripping
	// ------------------------------------------------------------------

	/**
	 * Replaces {@code //} and block comments with spaces, preserving
	 * newlines (so later line numbers stay true) and string literals.
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
			} else if (c == '/' && i + 1 < source.length()
					&& source.charAt(i + 1) == '/') {
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
					if (d == '\\' && i < source.length()) {
						out.append(source.charAt(i));
						i += 1;
					} else if (d == '"') {
						break;
					}
				}
			} else {
				out.append(c);
				i += 1;
			}
		}
		return out.toString();
	} // end of stripComments method

	// ------------------------------------------------------------------
	// phase 2: minimal preprocessor
	// ------------------------------------------------------------------

	/** One level of `ifdef nesting during preprocessing. */
	private static final class Cond {
		/** Whether the enclosing context was emitting text. */
		final boolean parentActive;
		/** Whether any branch of this conditional has been taken. */
		boolean taken;
		/** Whether the current branch is emitting text. */
		boolean active;

		/**
		 * Opens a conditional level.
		 * @param parentActive whether the enclosing context is active
		 * @param firstBranch whether the first branch is taken
		 */
		Cond(boolean parentActive, boolean firstBranch) {
			this.parentActive = parentActive;
			this.taken = firstBranch;
			this.active = parentActive && firstBranch;
		}
	} // end of Cond class

	/**
	 * Applies the minimal preprocessor: object-like defines,
	 * conditionals, macro substitution. Inactive lines and directive
	 * lines become blank lines so line numbers survive.
	 * @param source comment-stripped source text
	 * @return the preprocessed text, macro-free
	 * @throws HdlScanException on unsupported or malformed directives
	 */
	private static String preprocess(String source)
			throws HdlScanException {
		String[] lines = source.split("\n", -1);
		StringBuilder out = new StringBuilder(source.length());
		Map<String, String> macros = new LinkedHashMap<String, String>();
		List<Cond> stack = new ArrayList<Cond>();
		int i = 0;
		while (i < lines.length) {
			int lineNo = i + 1;
			String line = lines[i];
			i += 1;
			String trimmed = line.trim();
			boolean active = stack.isEmpty()
					|| stack.get(stack.size() - 1).active;
			if (trimmed.startsWith("`")) {
				// join continuation lines (only directives may use them)
				int blanks = 0;
				while (trimmed.endsWith("\\") && i < lines.length) {
					trimmed = trimmed.substring(0, trimmed.length() - 1)
							+ "\n" + lines[i];
					i += 1;
					blanks += 1;
				}
				String word = directiveWord(trimmed);
				if (word.equals("ifdef") || word.equals("ifndef")) {
					String name = directiveArgument(trimmed, word, lineNo);
					boolean defined = macros.containsKey(name);
					boolean branch = word.equals("ifdef") == defined;
					stack.add(new Cond(active, branch));
				} else if (word.equals("elsif")) {
					Cond top = topOf(stack, "`elsif", lineNo);
					String name = directiveArgument(trimmed, word, lineNo);
					boolean branch = !top.taken
							&& macros.containsKey(name);
					top.active = top.parentActive && branch;
					top.taken = top.taken || branch;
				} else if (word.equals("else")) {
					Cond top = topOf(stack, "`else", lineNo);
					top.active = top.parentActive && !top.taken;
					top.taken = true;
				} else if (word.equals("endif")) {
					topOf(stack, "`endif", lineNo);
					stack.remove(stack.size() - 1);
				} else if (word.equals("define")) {
					if (active) {
						defineMacro(trimmed, macros, lineNo);
					}
				} else if (word.equals("undef")) {
					if (active) {
						macros.remove(directiveArgument(trimmed, word,
								lineNo));
					}
				} else if (word.equals("include")) {
					if (active) {
						throw new HdlScanException("`include is not"
								+ " supported by the header scanner",
								lineNo);
					}
				} else if (IGNORED_DIRECTIVES.contains(word)) {
					// carries no port information
				} else if (macros.containsKey(word)) {
					// a line beginning with a macro use, not a directive
					if (active) {
						out.append(expand(trimmed, macros, lineNo));
					}
					out.append('\n');
					for (int b = 0; b < blanks; b += 1) {
						out.append('\n');
					}
					continue;
				} else if (active) {
					throw new HdlScanException("unsupported compiler"
							+ " directive `" + word, lineNo);
				}
				// directive consumed: keep the line count with blanks
				out.append('\n');
				for (int b = 0; b < blanks; b += 1) {
					out.append('\n');
				}
			} else {
				if (active) {
					out.append(expand(line, macros, lineNo));
				}
				out.append('\n');
			}
		}
		if (!stack.isEmpty()) {
			throw new HdlScanException(
					"`ifdef without matching `endif at end of file");
		}
		return out.toString();
	} // end of preprocess method

	/**
	 * Returns the innermost conditional, or rejects a stray directive.
	 * @param stack the open-conditional stack
	 * @param what the directive name, for the error message
	 * @param line the 1-based source line of the directive
	 * @return the innermost open conditional
	 * @throws HdlScanException if no conditional is open
	 */
	private static Cond topOf(List<Cond> stack, String what, int line)
			throws HdlScanException {
		if (stack.isEmpty()) {
			throw new HdlScanException(
					what + " without matching `ifdef", line);
		}
		return stack.get(stack.size() - 1);
	} // end of topOf method

	/**
	 * Extracts the directive word following the backtick.
	 * @param trimmed the trimmed directive line
	 * @return the directive word (possibly empty)
	 */
	private static String directiveWord(String trimmed) {
		int j = 1;
		while (j < trimmed.length()
				&& isIdentChar(trimmed.charAt(j))) {
			j += 1;
		}
		return trimmed.substring(1, j);
	} // end of directiveWord method

	/**
	 * Extracts the single identifier argument of a directive.
	 * @param trimmed the trimmed directive line
	 * @param word the directive word already extracted
	 * @param line the 1-based source line, for error messages
	 * @return the identifier argument
	 * @throws HdlScanException if the argument is missing or malformed
	 */
	private static String directiveArgument(String trimmed, String word,
			int line) throws HdlScanException {
		String rest = trimmed.substring(1 + word.length()).trim();
		int j = 0;
		while (j < rest.length() && isIdentChar(rest.charAt(j))) {
			j += 1;
		}
		if (j == 0) {
			throw new HdlScanException(
					"`" + word + " needs a macro name", line);
		}
		return rest.substring(0, j);
	} // end of directiveArgument method

	/**
	 * Records an object-like macro definition; rejects function-like.
	 * @param trimmed the trimmed, continuation-joined `define line
	 * @param macros the macro table to update
	 * @param line the 1-based source line, for error messages
	 * @throws HdlScanException on a function-like or nameless define
	 */
	private static void defineMacro(String trimmed,
			Map<String, String> macros, int line)
			throws HdlScanException {
		String rest = trimmed.substring("`define".length());
		int j = 0;
		while (j < rest.length()
				&& Character.isWhitespace(rest.charAt(j))) {
			j += 1;
		}
		int start = j;
		while (j < rest.length() && isIdentChar(rest.charAt(j))) {
			j += 1;
		}
		if (start == j) {
			throw new HdlScanException("`define needs a macro name",
					line);
		}
		String name = rest.substring(start, j);
		if (j < rest.length() && rest.charAt(j) == '(') {
			throw new HdlScanException("function-like macro `" + name
					+ "(...) is not supported by the header scanner",
					line);
		}
		macros.put(name, rest.substring(j).trim()
				.replace('\n', ' '));
	} // end of defineMacro method

	/**
	 * Substitutes {@code `NAME} macro uses in one line, repeatedly,
	 * so macros may reference other macros.
	 * @param line the text to expand
	 * @param macros the macro table
	 * @param lineNo the 1-based source line, for error messages
	 * @return the fully expanded text
	 * @throws HdlScanException on an undefined macro or expansion loop
	 */
	private static String expand(String line, Map<String, String> macros,
			int lineNo) throws HdlScanException {
		String text = line;
		for (int round = 0; text.indexOf('`') >= 0; round += 1) {
			if (round >= MAX_EXPANSIONS) {
				throw new HdlScanException(
						"macro expansion does not terminate", lineNo);
			}
			int tick = text.indexOf('`');
			int j = tick + 1;
			while (j < text.length() && isIdentChar(text.charAt(j))) {
				j += 1;
			}
			String name = text.substring(tick + 1, j);
			String body = macros.get(name);
			if (body == null) {
				throw new HdlScanException(name.isEmpty()
						? "stray ` in source"
						: "undefined macro `" + name, lineNo);
			}
			text = text.substring(0, tick) + " " + body + " "
					+ text.substring(j);
		}
		return text;
	} // end of expand method

	/**
	 * @param c the character to classify
	 * @return true if c may appear in a Verilog identifier
	 */
	private static boolean isIdentChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	} // end of isIdentChar method

	// ------------------------------------------------------------------
	// phase 3: tokenizing
	// ------------------------------------------------------------------

	/** Token kinds the parser distinguishes. */
	private enum Kind {
		/** An identifier or keyword. */
		ID,
		/** An unsigned decimal digit run (underscores allowed). */
		NUM,
		/** A single punctuation character. */
		PUNCT,
		/** A complete string literal, quotes included. */
		STRING
	} // end of Kind enum

	/** One token with its 1-based source line. */
	private static final class Token {
		/** What sort of token this is. */
		final Kind kind;
		/** The token text (for PUNCT, a single character). */
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
	} // end of Token class

	/**
	 * Splits preprocessed text into tokens.
	 * @param text comment-free, macro-free source text
	 * @return the token list
	 * @throws HdlScanException on characters outside the subset
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
			} else if (Character.isLetter(c) || c == '_' || c == '$') {
				int start = i;
				while (i < text.length() && isIdentChar(text.charAt(i))) {
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
			} else if (c == '"') {
				int start = i;
				i += 1;
				while (i < text.length() && text.charAt(i) != '"') {
					i += text.charAt(i) == '\\' ? 2 : 1;
				}
				i += 1;
				out.add(new Token(Kind.STRING,
						text.substring(start, Math.min(i, text.length())),
						line));
			} else if (c == '\\') {
				throw new HdlScanException("escaped identifiers"
						+ " (backslash names) are not supported by the"
						+ " header scanner", line);
			} else {
				out.add(new Token(Kind.PUNCT, String.valueOf(c), line));
				i += 1;
			}
		}
		return out;
	} // end of tokenize method

	// ------------------------------------------------------------------
	// phase 4: parsing
	// ------------------------------------------------------------------

	/**
	 * Walks the token stream collecting every module header.
	 * @return the modules in file order
	 * @throws HdlScanException on malformed or out-of-subset modules
	 */
	private List<ScannedModule> parseModules() throws HdlScanException {
		List<ScannedModule> modules = new ArrayList<ScannedModule>();
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			if (t.kind == Kind.ID && (t.text.equals("module")
					|| t.text.equals("macromodule"))) {
				pos += 1;
				modules.add(parseModule());
			} else {
				pos += 1;
			}
		}
		return modules;
	} // end of parseModules method

	/**
	 * Parses one module from just after its {@code module} keyword to
	 * its {@code endmodule}.
	 * @return the scanned module
	 * @throws HdlScanException on malformed or out-of-subset content
	 */
	private ScannedModule parseModule() throws HdlScanException {
		Token nameTok = expect(Kind.ID, "module name");
		String name = nameTok.text;
		Map<String, Long> params = new LinkedHashMap<String, Long>();
		Set<String> unevaluable = new LinkedHashSet<String>();
		if (atPunct("#")) {
			pos += 1;
			expectPunct("(", "parameter list");
			parseParameterEntries(collectBalanced(")"), params,
					unevaluable);
		}
		List<Token> portSlice = new ArrayList<Token>();
		if (atPunct("(")) {
			pos += 1;
			portSlice = collectBalanced(")");
		}
		// skip to the end of the module header statement
		while (pos < tokens.size() && !atPunct(";")) {
			pos += 1;
		}
		expectPunct(";", "end of module header");

		boolean ansi = containsDirection(portSlice);
		List<String> order = new ArrayList<String>();
		Map<String, ScannedPort> declared =
				new LinkedHashMap<String, ScannedPort>();
		List<ScannedPort> ansiPorts = null;
		if (ansi) {
			ansiPorts = parseAnsiPorts(portSlice, params, unevaluable);
		} else {
			parsePlainNames(portSlice, order);
		}
		parseBody(name, ansi, params, unevaluable, declared);
		if (ansi) {
			return new ScannedModule(name, ansiPorts, params);
		}
		List<ScannedPort> ports = new ArrayList<ScannedPort>();
		for (String portName : order) {
			ScannedPort port = declared.get(portName);
			if (port == null) {
				throw new HdlScanException("port '" + portName
						+ "' of module '" + name + "' has no"
						+ " input/output/inout declaration");
			}
			ports.add(port);
		}
		return new ScannedModule(name, ports, params);
	} // end of parseModule method

	/**
	 * Scans a module body for parameter and non-ANSI port
	 * declarations, skipping function/task bodies whose {@code input}
	 * declarations are not ports.
	 * @param moduleName the module name, for error messages
	 * @param ansi whether the header used the ANSI port style
	 * @param params evaluated parameters, extended in place
	 * @param unevaluable names of parameters that failed to evaluate
	 * @param declared non-ANSI port declarations, filled in place
	 * @throws HdlScanException on malformed or out-of-subset content
	 */
	private void parseBody(String moduleName, boolean ansi,
			Map<String, Long> params, Set<String> unevaluable,
			Map<String, ScannedPort> declared) throws HdlScanException {
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			if (t.kind == Kind.ID) {
				switch (t.text) {
				case "endmodule":
					pos += 1;
					return;
				case "module":
				case "macromodule":
					throw new HdlScanException(
							"nested module declaration", t.line);
				case "function":
				case "task":
					skipUntilKeyword("end" + t.text);
					continue;
				case "parameter":
				case "localparam":
					pos += 1;
					parseParameterEntries(collectStatement(), params,
							unevaluable);
					continue;
				case "input":
				case "output":
				case "inout":
					if (ansi) {
						throw new HdlScanException("port declaration in"
								+ " the body of ANSI-style module '"
								+ moduleName + "'", t.line);
					}
					parseBodyPortDeclaration(params, unevaluable,
							declared);
					continue;
				default:
					break;
				}
			}
			pos += 1;
		}
		throw new HdlScanException("missing endmodule for module '"
				+ moduleName + "'");
	} // end of parseBody method

	/**
	 * Skips tokens up to and including a closing keyword.
	 * @param keyword the keyword that ends the region
	 * @throws HdlScanException if the keyword never appears
	 */
	private void skipUntilKeyword(String keyword)
			throws HdlScanException {
		int startLine = tokens.get(pos).line;
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			pos += 1;
			if (t.kind == Kind.ID && t.text.equals(keyword)) {
				return;
			}
		}
		throw new HdlScanException("missing " + keyword, startLine);
	} // end of skipUntilKeyword method

	/**
	 * Parses one non-ANSI port declaration statement, e.g.
	 * {@code output reg [WIDTH-1:0] q, r;}.
	 * @param params evaluated parameters for width expressions
	 * @param unevaluable names of parameters that failed to evaluate
	 * @param declared the map of declarations, extended in place
	 * @throws HdlScanException on malformed or out-of-subset content
	 */
	private void parseBodyPortDeclaration(Map<String, Long> params,
			Set<String> unevaluable, Map<String, ScannedPort> declared)
			throws HdlScanException {
		Token dirTok = tokens.get(pos);
		pos += 1;
		ScannedPort.Direction direction = direction(dirTok.text);
		List<Token> slice = collectStatement();
		int j = 0;
		while (j < slice.size() && slice.get(j).kind == Kind.ID
				&& NET_TYPES.contains(slice.get(j).text)) {
			j += 1;
		}
		int bits = 1;
		if (j < slice.size() && isPunct(slice.get(j), "[")) {
			int close = matchBracket(slice, j);
			bits = evaluateRange(slice.subList(j + 1, close), params,
					unevaluable);
			j = close + 1;
		}
		boolean sawName = false;
		while (j < slice.size()) {
			Token t = slice.get(j);
			if (t.kind != Kind.ID) {
				throw new HdlScanException("unexpected '" + t.text
						+ "' in port declaration", t.line);
			}
			String portName = t.text;
			j += 1;
			if (j < slice.size() && isPunct(slice.get(j), "[")) {
				throw new HdlScanException("array port '" + portName
						+ "' is not supported by the header scanner",
						t.line);
			}
			if (j < slice.size() && isPunct(slice.get(j), "=")) {
				j = skipToTopLevelComma(slice, j);
			}
			if (declared.containsKey(portName)) {
				throw new HdlScanException("port '" + portName
						+ "' declared twice", t.line);
			}
			declared.put(portName,
					new ScannedPort(portName, direction, bits));
			sawName = true;
			if (j < slice.size()) {
				if (!isPunct(slice.get(j), ",")) {
					throw new HdlScanException("unexpected '"
							+ slice.get(j).text
							+ "' in port declaration",
							slice.get(j).line);
				}
				j += 1;
			}
		}
		if (!sawName) {
			throw new HdlScanException("port declaration without a"
					+ " port name", dirTok.line);
		}
	} // end of parseBodyPortDeclaration method

	/**
	 * Parses an ANSI port list slice into ports. Direction, type and
	 * range stick until the next declaration changes them, matching
	 * Verilog-2001 semantics for {@code input [7:0] a, b}.
	 * @param slice the tokens between the port-list parentheses
	 * @param params evaluated parameters for width expressions
	 * @param unevaluable names of parameters that failed to evaluate
	 * @return the ports in declaration order
	 * @throws HdlScanException on malformed or out-of-subset content
	 */
	private List<ScannedPort> parseAnsiPorts(List<Token> slice,
			Map<String, Long> params, Set<String> unevaluable)
			throws HdlScanException {
		List<ScannedPort> ports = new ArrayList<ScannedPort>();
		ScannedPort.Direction direction = null;
		int bits = 1;
		for (List<Token> entry : splitTopLevel(slice)) {
			if (entry.isEmpty()) {
				throw new HdlScanException(
						"empty entry in port list");
			}
			int j = 0;
			Token first = entry.get(0);
			if (isPunct(first, ".")) {
				throw new HdlScanException("SystemVerilog .name()"
						+ " port lists are not supported by the header"
						+ " scanner", first.line);
			}
			if (first.kind == Kind.ID && isDirection(first.text)) {
				direction = direction(first.text);
				bits = 1;
				j += 1;
			}
			if (direction == null) {
				throw new HdlScanException("port list mixes plain names"
						+ " with directed declarations", first.line);
			}
			boolean sawType = false;
			while (j < entry.size() && entry.get(j).kind == Kind.ID
					&& NET_TYPES.contains(entry.get(j).text)) {
				sawType = true;
				j += 1;
			}
			if (sawType && j < entry.size()
					&& !isPunct(entry.get(j), "[")) {
				// a fresh type without a range resets to 1 bit
				bits = 1;
			}
			if (j < entry.size() && isPunct(entry.get(j), "[")) {
				int close = matchBracket(entry, j);
				bits = evaluateRange(entry.subList(j + 1, close),
						params, unevaluable);
				j = close + 1;
				if (j < entry.size() && isPunct(entry.get(j), "[")) {
					throw new HdlScanException("multi-dimensional port"
							+ " is not supported by the header scanner",
							entry.get(j).line);
				}
			}
			if (j >= entry.size() || entry.get(j).kind != Kind.ID) {
				throw new HdlScanException("expected a port name in"
						+ " port list", first.line);
			}
			Token nameTok = entry.get(j);
			j += 1;
			if (j < entry.size() && isPunct(entry.get(j), "[")) {
				throw new HdlScanException("array port '" + nameTok.text
						+ "' is not supported by the header scanner",
						nameTok.line);
			}
			if (j < entry.size() && !isPunct(entry.get(j), "=")) {
				throw new HdlScanException("unexpected '"
						+ entry.get(j).text + "' in port list",
						entry.get(j).line);
			}
			ports.add(new ScannedPort(nameTok.text, direction, bits));
		}
		return ports;
	} // end of parseAnsiPorts method

	/**
	 * Parses a non-ANSI port list: bare names only.
	 * @param slice the tokens between the port-list parentheses
	 * @param order the port-name order list, filled in place
	 * @throws HdlScanException if an entry is not a single identifier
	 */
	private void parsePlainNames(List<Token> slice, List<String> order)
			throws HdlScanException {
		if (slice.isEmpty()) {
			return;
		}
		for (List<Token> entry : splitTopLevel(slice)) {
			if (!entry.isEmpty() && isPunct(entry.get(0), ".")) {
				throw new HdlScanException("SystemVerilog .name()"
						+ " port lists are not supported by the header"
						+ " scanner", entry.get(0).line);
			}
			if (entry.size() != 1 || entry.get(0).kind != Kind.ID) {
				int line = entry.isEmpty() ? 0 : entry.get(0).line;
				throw new HdlScanException("port list entry is not a"
						+ " plain name; only simple ports are supported"
						+ " by the header scanner", line);
			}
			order.add(entry.get(0).text);
		}
	} // end of parsePlainNames method

	/**
	 * Parses parameter/localparam assignments from a token slice
	 * (either a {@code #(...)} list or a body statement), evaluating
	 * each default with the parameters bound so far.
	 * @param slice the tokens of the assignments
	 * @param params evaluated parameters, extended in place
	 * @param unevaluable names whose defaults failed to evaluate,
	 *        extended in place (fatal only if a width needs them)
	 * @throws HdlScanException on malformed assignments
	 */
	private void parseParameterEntries(List<Token> slice,
			Map<String, Long> params, Set<String> unevaluable)
			throws HdlScanException {
		for (List<Token> entry : splitTopLevel(slice)) {
			int j = 0;
			while (j < entry.size() && entry.get(j).kind == Kind.ID
					&& (entry.get(j).text.equals("parameter")
							|| entry.get(j).text.equals("localparam")
							|| PARAM_TYPES.contains(entry.get(j).text))) {
				j += 1;
			}
			if (j < entry.size() && isPunct(entry.get(j), "[")) {
				j = matchBracket(entry, j) + 1;
			}
			if (j + 1 >= entry.size() || entry.get(j).kind != Kind.ID
					|| !isPunct(entry.get(j + 1), "=")) {
				int line = entry.isEmpty() ? 0 : entry.get(0).line;
				throw new HdlScanException(
						"malformed parameter declaration", line);
			}
			String paramName = entry.get(j).text;
			List<Token> expr = entry.subList(j + 2, entry.size());
			try {
				params.put(paramName,
						new Eval(expr, params, unevaluable).parse());
			} catch (HdlScanException e) {
				// only fatal if a port width later needs this value
				unevaluable.add(paramName);
			}
		}
	} // end of parseParameterEntries method

	// ------------------------------------------------------------------
	// token-stream helpers
	// ------------------------------------------------------------------

	/**
	 * Consumes a token of the given kind or fails.
	 * @param kind the required token kind
	 * @param what what was expected, for the error message
	 * @return the consumed token
	 * @throws HdlScanException if the stream ends or mismatches
	 */
	private Token expect(Kind kind, String what)
			throws HdlScanException {
		if (pos >= tokens.size()) {
			throw new HdlScanException(
					"unexpected end of file; expected " + what);
		}
		Token t = tokens.get(pos);
		if (t.kind != kind) {
			throw new HdlScanException("expected " + what
					+ " but found '" + t.text + "'", t.line);
		}
		pos += 1;
		return t;
	} // end of expect method

	/**
	 * Consumes an exact punctuation token or fails.
	 * @param punct the required punctuation
	 * @param what what construct needed it, for the error message
	 * @throws HdlScanException if the stream ends or mismatches
	 */
	private void expectPunct(String punct, String what)
			throws HdlScanException {
		if (!atPunct(punct)) {
			int line = pos < tokens.size() ? tokens.get(pos).line : 0;
			throw new HdlScanException("expected '" + punct + "' ("
					+ what + ")", line);
		}
		pos += 1;
	} // end of expectPunct method

	/**
	 * @param punct the punctuation to test for
	 * @return true if the next token is exactly that punctuation
	 */
	private boolean atPunct(String punct) {
		return pos < tokens.size()
				&& isPunct(tokens.get(pos), punct);
	} // end of atPunct method

	/**
	 * @param t the token to test
	 * @param punct the punctuation text
	 * @return true if t is that punctuation
	 */
	private static boolean isPunct(Token t, String punct) {
		return t.kind == Kind.PUNCT && t.text.equals(punct);
	} // end of isPunct method

	/**
	 * Collects tokens up to (excluding) the closer that balances the
	 * already-consumed opener, tracking paren/bracket/brace nesting;
	 * consumes the closer.
	 * @param closer the closing punctuation ")" or "]"
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
				if (t.text.equals("(") || t.text.equals("[")
						|| t.text.equals("{")) {
					depth += 1;
				} else if (t.text.equals(")") || t.text.equals("]")
						|| t.text.equals("}")) {
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
	 * Collects tokens up to (excluding) the next top-level semicolon;
	 * consumes the semicolon.
	 * @return the collected slice
	 * @throws HdlScanException if the stream ends first
	 */
	private List<Token> collectStatement() throws HdlScanException {
		List<Token> slice = new ArrayList<Token>();
		int depth = 0;
		int startLine = pos < tokens.size() ? tokens.get(pos).line : 0;
		while (pos < tokens.size()) {
			Token t = tokens.get(pos);
			if (t.kind == Kind.PUNCT) {
				if (depth == 0 && t.text.equals(";")) {
					pos += 1;
					return slice;
				}
				if (t.text.equals("(") || t.text.equals("[")
						|| t.text.equals("{")) {
					depth += 1;
				} else if (t.text.equals(")") || t.text.equals("]")
						|| t.text.equals("}")) {
					depth -= 1;
				}
			}
			slice.add(t);
			pos += 1;
		}
		throw new HdlScanException("missing ';'", startLine);
	} // end of collectStatement method

	/**
	 * Splits a slice on top-level commas.
	 * @param slice the tokens to split
	 * @return the comma-separated entries, in order
	 */
	private static List<List<Token>> splitTopLevel(List<Token> slice) {
		List<List<Token>> entries = new ArrayList<List<Token>>();
		List<Token> current = new ArrayList<Token>();
		int depth = 0;
		for (Token t : slice) {
			if (t.kind == Kind.PUNCT) {
				if (t.text.equals("(") || t.text.equals("[")
						|| t.text.equals("{")) {
					depth += 1;
				} else if (t.text.equals(")") || t.text.equals("]")
						|| t.text.equals("}")) {
					depth -= 1;
				} else if (depth == 0 && t.text.equals(",")) {
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
	 * Finds the "]" matching the "[" at the given index.
	 * @param slice the tokens to search
	 * @param open the index of the "[" token
	 * @return the index of the matching "]"
	 * @throws HdlScanException if there is none
	 */
	private static int matchBracket(List<Token> slice, int open)
			throws HdlScanException {
		int depth = 0;
		for (int j = open; j < slice.size(); j += 1) {
			Token t = slice.get(j);
			if (isPunct(t, "[")) {
				depth += 1;
			} else if (isPunct(t, "]")) {
				depth -= 1;
				if (depth == 0) {
					return j;
				}
			}
		}
		throw new HdlScanException("missing ']'",
				slice.get(open).line);
	} // end of matchBracket method

	/**
	 * Advances past a default-value expression to the entry's end.
	 * @param slice the declaration tokens
	 * @param eq the index of the "=" token
	 * @return the index of the top-level "," ending the value, or the
	 *         slice length
	 */
	private static int skipToTopLevelComma(List<Token> slice, int eq) {
		int depth = 0;
		int j = eq;
		while (j < slice.size()) {
			Token t = slice.get(j);
			if (t.kind == Kind.PUNCT) {
				if (t.text.equals("(") || t.text.equals("[")
						|| t.text.equals("{")) {
					depth += 1;
				} else if (t.text.equals(")") || t.text.equals("]")
						|| t.text.equals("}")) {
					depth -= 1;
				} else if (depth == 0 && t.text.equals(",")) {
					return j;
				}
			}
			j += 1;
		}
		return j;
	} // end of skipToTopLevelComma method

	/**
	 * @param slice the port-list tokens
	 * @return true if any direction keyword appears (ANSI style)
	 */
	private static boolean containsDirection(List<Token> slice) {
		for (Token t : slice) {
			if (t.kind == Kind.ID && isDirection(t.text)) {
				return true;
			}
		}
		return false;
	} // end of containsDirection method

	/**
	 * @param word an identifier
	 * @return true if it is a Verilog direction keyword
	 */
	private static boolean isDirection(String word) {
		return word.equals("input") || word.equals("output")
				|| word.equals("inout");
	} // end of isDirection method

	/**
	 * @param word a Verilog direction keyword
	 * @return the corresponding scanned-port direction
	 */
	private static ScannedPort.Direction direction(String word) {
		switch (word) {
		case "input":
			return ScannedPort.Direction.IN;
		case "output":
			return ScannedPort.Direction.OUT;
		default:
			return ScannedPort.Direction.INOUT;
		}
	} // end of direction method

	// ------------------------------------------------------------------
	// constant-expression evaluation
	// ------------------------------------------------------------------

	/**
	 * Evaluates a {@code [msb:lsb]} range to a bit width.
	 * @param range the tokens between the brackets
	 * @param params evaluated parameters
	 * @param unevaluable names of parameters that failed to evaluate
	 * @return the width in bits, {@code abs(msb-lsb)+1}
	 * @throws HdlScanException if the range cannot be evaluated
	 */
	private static int evaluateRange(List<Token> range,
			Map<String, Long> params, Set<String> unevaluable)
			throws HdlScanException {
		int colon = -1;
		int depth = 0;
		for (int j = 0; j < range.size(); j += 1) {
			Token t = range.get(j);
			if (t.kind == Kind.PUNCT) {
				if (t.text.equals("(") || t.text.equals("[")) {
					depth += 1;
				} else if (t.text.equals(")") || t.text.equals("]")) {
					depth -= 1;
				} else if (depth == 0 && t.text.equals(":")) {
					colon = j;
					break;
				}
			}
		}
		int line = range.isEmpty() ? 0 : range.get(0).line;
		if (colon < 0) {
			throw new HdlScanException(
					"expected [msb:lsb] range", line);
		}
		long msb = new Eval(range.subList(0, colon), params,
				unevaluable).parse();
		long lsb = new Eval(range.subList(colon + 1, range.size()),
				params, unevaluable).parse();
		long bits = Math.abs(msb - lsb) + 1;
		if (bits < 1 || bits > MAX_BITS) {
			throw new HdlScanException("range [" + msb + ":" + lsb
					+ "] gives an unreasonable width", line);
		}
		return (int) bits;
	} // end of evaluateRange method

	/**
	 * A tiny recursive-descent evaluator for Verilog constant integer
	 * expressions: decimal and based literals, parameter names,
	 * {@code + - * / % **}, unary sign, parentheses, {@code $clog2}.
	 */
	private static final class Eval {

		/** The expression tokens. */
		private final List<Token> toks;
		/** Evaluated parameters visible to the expression. */
		private final Map<String, Long> params;
		/** Parameters whose defaults could not be evaluated. */
		private final Set<String> unevaluable;
		/** Cursor into {@link #toks}. */
		private int at;

		/**
		 * Builds an evaluator over a token slice.
		 * @param toks the expression tokens
		 * @param params evaluated parameters
		 * @param unevaluable parameters that failed to evaluate
		 */
		Eval(List<Token> toks, Map<String, Long> params,
				Set<String> unevaluable) {
			this.toks = toks;
			this.params = params;
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
		 * Parses multiplicative expressions.
		 * @return the value
		 * @throws HdlScanException on out-of-subset content
		 */
		private long mulDiv() throws HdlScanException {
			long v = power();
			while (at < toks.size()) {
				Token t = toks.get(at);
				if (isPunct(t, "*") && !(at + 1 < toks.size()
						&& isPunct(toks.get(at + 1), "*"))) {
					at += 1;
					v *= power();
				} else if (isPunct(t, "/") || isPunct(t, "%")) {
					at += 1;
					long r = power();
					if (r == 0) {
						throw new HdlScanException(
								"division by zero in width expression",
								t.line);
					}
					v = t.text.equals("/") ? v / r : v % r;
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
		 * Parses a primary: literal, parameter, call, or parentheses.
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
				if (at < toks.size() && isPunct(toks.get(at), "'")) {
					at += 1;
					return basedLiteral(t.line);
				}
				try {
					return Long.parseLong(t.text.replace("_", ""));
				} catch (NumberFormatException overflow) {
					// digits are guaranteed by the tokenizer, so only
					// a value past Long.MAX_VALUE lands here - reject
					// with the typed error, never a raw runtime one
					throw new HdlScanException(
							"numeric literal out of range", t.line);
				}
			}
			if (isPunct(t, "'")) {
				at += 1;
				return basedLiteral(t.line);
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
				if (t.text.equals("$clog2")) {
					at += 1;
					if (at >= toks.size()
							|| !isPunct(toks.get(at), "(")) {
						throw new HdlScanException(
								"$clog2 needs parentheses", t.line);
					}
					at += 1;
					long arg = addSub();
					if (at >= toks.size()
							|| !isPunct(toks.get(at), ")")) {
						throw new HdlScanException(
								"missing ')' after $clog2", t.line);
					}
					at += 1;
					return clog2(arg, t.line);
				}
				if (params.containsKey(t.text)) {
					at += 1;
					return params.get(t.text);
				}
				if (unevaluable.contains(t.text)) {
					throw new HdlScanException("parameter '" + t.text
							+ "' has a default the scanner cannot"
							+ " evaluate", t.line);
				}
				throw new HdlScanException("unknown name '" + t.text
						+ "' in constant expression", t.line);
			}
			throw new HdlScanException("cannot evaluate '" + t.text
					+ "' in constant expression", t.line);
		} // end of primary method

		/**
		 * Parses the base-and-digits part of a based literal, the
		 * apostrophe already consumed.
		 * @param line the literal's source line, for error messages
		 * @return the literal value
		 * @throws HdlScanException on x/z digits or malformed bases
		 */
		private long basedLiteral(int line) throws HdlScanException {
			if (at >= toks.size() || toks.get(at).kind != Kind.ID) {
				throw new HdlScanException(
						"malformed based literal", line);
			}
			String text = toks.get(at).text
					.toLowerCase(Locale.ROOT);
			at += 1;
			int k = 0;
			if (k < text.length() && text.charAt(k) == 's') {
				k += 1;
			}
			if (k >= text.length()) {
				throw new HdlScanException(
						"malformed based literal", line);
			}
			int radix;
			switch (text.charAt(k)) {
			case 'b':
				radix = 2;
				break;
			case 'o':
				radix = 8;
				break;
			case 'd':
				radix = 10;
				break;
			case 'h':
				radix = 16;
				break;
			default:
				throw new HdlScanException(
						"malformed based literal", line);
			}
			k += 1;
			String digits = text.substring(k).replace("_", "");
			if (digits.isEmpty() && at < toks.size()
					&& toks.get(at).kind == Kind.NUM) {
				digits = toks.get(at).text.replace("_", "");
				at += 1;
			}
			if (digits.isEmpty()) {
				throw new HdlScanException(
						"malformed based literal", line);
			}
			if (digits.indexOf('x') >= 0 || digits.indexOf('z')
					>= 0 || digits.indexOf('?') >= 0) {
				throw new HdlScanException("x/z digits in a width"
						+ " expression are not supported", line);
			}
			try {
				return Long.parseLong(digits, radix);
			} catch (NumberFormatException e) {
				throw new HdlScanException(
						"malformed based literal", line);
			}
		} // end of basedLiteral method

		/**
		 * Verilog's {@code $clog2}: ceiling of log base 2.
		 * @param value the argument
		 * @param line the call's source line, for error messages
		 * @return ceil(log2(value)); 0 for values below 2
		 * @throws HdlScanException on negative arguments
		 */
		private static long clog2(long value, int line)
				throws HdlScanException {
			if (value < 0) {
				throw new HdlScanException(
						"$clog2 of a negative value", line);
			}
			long bits = 0;
			long v = value - 1;
			while (v > 0) {
				bits += 1;
				v >>= 1;
			}
			return bits;
		} // end of clog2 method

	} // end of Eval class

} // end of VerilogHeaderScanner class
