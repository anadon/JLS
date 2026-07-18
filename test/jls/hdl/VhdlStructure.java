package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool-free structural sanity checks over emitted VHDL (issue #60), the
 * sibling of {@link VerilogStructure}: exactly one entity and one
 * architecture, balanced processes, every port and signal declared
 * exactly once (case-insensitively - VHDL identifiers that differ only
 * in case collide), and every identifier referenced in the architecture
 * body declared as a port or signal. This is not a VHDL parser - it
 * leans on the emitter's pinned formatting - but it catches the failure
 * modes golden diffs alone cannot explain: references to undeclared
 * signals and unbalanced design units. {@link GhdlCompileTest} is the
 * real-analyzer check when ghdl is installed.
 */
final class VhdlStructure {

	private VhdlStructure() {
	}

	private static final Pattern IDENTIFIER =
			Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

	/** Language words and ieee names the emitter's templates use. */
	private static final Set<String> KEYWORDS = Set.of(
			"library", "use", "all", "ieee", "std_logic_1164",
			"numeric_std", "entity", "is", "port", "in", "out", "end",
			"architecture", "structural", "of", "signal", "std_logic",
			"std_logic_vector", "unsigned", "downto", "begin", "process",
			"if", "then", "else", "elsif", "when", "others", "not",
			"and", "or", "xor", "nand", "nor", "rising_edge",
			"falling_edge", "with", "select");

	static void assertSane(String vhdl) {

		String body = stripCommentsAndLiterals(vhdl);

		assertEquals(1, count(body, "\\bentity\\s+\\w+\\s+is\\b"),
				"exactly one entity expected");
		assertEquals(1, count(body, "\\bend\\s+entity\\b"),
				"exactly one end entity expected");
		assertEquals(1, count(body, "\\barchitecture\\s+\\w+\\s+of\\b"),
				"exactly one architecture expected");
		assertEquals(1, count(body, "\\bend\\s+architecture\\b"),
				"exactly one end architecture expected");
		assertEquals(count(body, "\\bprocess\\s*\\("),
				count(body, "\\bend\\s+process\\b"),
				"process/end process must balance");

		// declarations: entity ports and architecture signals
		Set<String> declared = new HashSet<String>();
		List<String> statementLines = new ArrayList<String>();
		boolean inBody = false;
		for (String line : body.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			if (trimmed.startsWith("begin")) {
				inBody = true;
				continue;
			}
			if (trimmed.startsWith("end architecture")) {
				inBody = false;
				continue;
			}
			Matcher port = Pattern
					.compile("^([A-Za-z][A-Za-z0-9_]*)\\s*:\\s*(in|out)\\b")
					.matcher(trimmed);
			Matcher signal = Pattern
					.compile("^signal\\s+([A-Za-z][A-Za-z0-9_]*)\\s*:")
					.matcher(trimmed);
			if (port.find()) {
				declare(declared, port.group(1));
			}
			else if (signal.find()) {
				declare(declared, signal.group(1));
				// a signal declaration may carry an initializer
				int assign = trimmed.indexOf(":=");
				if (assign >= 0) {
					statementLines.add(trimmed.substring(assign + 2));
				}
			}
			else if (inBody) {
				statementLines.add(trimmed);
			}
		}

		// every identifier used in the body must be declared
		for (String line : statementLines) {
			Matcher ids = IDENTIFIER.matcher(line);
			while (ids.find()) {
				String id = ids.group();
				if (KEYWORDS.contains(id.toLowerCase(Locale.ROOT))) {
					continue;
				}
				assertTrue(declared.contains(id.toLowerCase(Locale.ROOT)),
						"\"" + id + "\" is referenced but never declared"
								+ " (in: " + line + ")");
			}
		}
	} // end of assertSane method

	private static void declare(Set<String> declared, String name) {

		assertTrue(declared.add(name.toLowerCase(Locale.ROOT)),
				name + " is declared more than once (case-insensitively)");
	} // end of declare method

	private static String stripCommentsAndLiterals(String vhdl) {

		StringBuilder sb = new StringBuilder();
		for (String line : vhdl.split("\n", -1)) {
			int comment = line.indexOf("--");
			sb.append(comment >= 0 ? line.substring(0, comment) : line)
					.append('\n');
		}
		// string literals ("0101") then character literals ('Z')
		String text = sb.toString().replaceAll("\"[01]*\"", " ");
		return text.replaceAll("'.'", " ");
	} // end of stripCommentsAndLiterals method

	private static int count(String text, String regex) {

		Matcher m = Pattern.compile(regex).matcher(text);
		int n = 0;
		while (m.find()) {
			n += 1;
		}
		return n;
	} // end of count method

} // end of VhdlStructure class
