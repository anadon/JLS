package jls.hdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool-free structural sanity checks over emitted Verilog (issue #60):
 * balanced module/endmodule, every port in the header declared exactly
 * once (and vice versa), and every identifier referenced in a
 * statement declared as a port, wire or reg. This is not a Verilog
 * parser - it leans on the emitter's pinned formatting - but it
 * catches the failure modes golden diffs alone cannot explain:
 * references to undeclared nets, undeclared ports, unbalanced blocks.
 */
final class VerilogStructure {

	private VerilogStructure() {
	}

	private static final Pattern IDENTIFIER =
			Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	/** Sized literals like 4'hb / 1'b0 / 8'd15, so 'h'/'b' don't scan
	 *  as identifiers. */
	private static final Pattern SIZED_LITERAL =
			Pattern.compile("\\d+'[bdh][0-9a-fA-FxzXZ]+");

	private static final Set<String> KEYWORDS = Set.of(
			"module", "endmodule", "input", "output", "wire", "reg",
			"assign", "always", "posedge", "negedge", "or", "if",
			"case", "endcase", "default");

	static void assertSane(String verilog) {

		String body = stripCommentsAndLiterals(verilog);

		assertEquals(1, count(body, "\\bmodule\\b"),
				"exactly one module expected");
		assertEquals(1, count(body, "\\bendmodule\\b"),
				"exactly one endmodule expected");

		// ports named in the header
		Matcher header = Pattern
				.compile("\\bmodule\\s+([A-Za-z_][A-Za-z0-9_]*)"
						+ "\\s*(?:\\(([^)]*)\\))?;")
				.matcher(body);
		assertTrue(header.find(), "module header not found");
		Set<String> headerPorts = new HashSet<String>();
		if (header.group(2) != null) {
			for (String port : header.group(2).split(",")) {
				if (!port.trim().isEmpty()) {
					headerPorts.add(port.trim());
				}
			}
		}

		// declarations
		Set<String> declaredPorts = new HashSet<String>();
		Set<String> declared = new HashSet<String>();
		List<String> statementLines = new ArrayList<String>();
		for (String line : body.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("module")
					|| trimmed.startsWith("endmodule")) {
				continue;
			}
			Matcher decl = Pattern
					.compile("^(input|output|wire|reg)\\b"
							+ "(?:\\s*\\[[^\\]]*\\])?\\s+"
							+ "([A-Za-z_][A-Za-z0-9_]*)")
					.matcher(trimmed);
			if (decl.find()) {
				String kind = decl.group(1);
				String name = decl.group(2);
				assertTrue(declared.add(name),
						name + " is declared more than once");
				if (kind.equals("input") || kind.equals("output")) {
					declaredPorts.add(name);
				}
				// a reg declaration may carry an initializer expression
				int eq = trimmed.indexOf('=');
				if (eq >= 0) {
					statementLines.add(trimmed.substring(eq + 1));
				}
			}
			else {
				statementLines.add(trimmed);
			}
		}

		assertEquals(headerPorts, declaredPorts,
				"module header ports and input/output declarations differ");

		// every identifier used in a statement must be declared
		for (String line : statementLines) {
			Matcher ids = IDENTIFIER.matcher(line);
			while (ids.find()) {
				String id = ids.group();
				if (KEYWORDS.contains(id)) {
					continue;
				}
				assertTrue(declared.contains(id),
						"\"" + id + "\" is referenced but never declared"
								+ " (in: " + line + ")");
			}
		}
	} // end of assertSane method

	private static String stripCommentsAndLiterals(String verilog) {

		StringBuilder sb = new StringBuilder();
		for (String line : verilog.split("\n", -1)) {
			int comment = line.indexOf("//");
			sb.append(comment >= 0 ? line.substring(0, comment) : line)
					.append('\n');
		}
		return SIZED_LITERAL.matcher(sb).replaceAll(" ");
	} // end of stripCommentsAndLiterals method

	private static int count(String text, String regex) {

		Matcher m = Pattern.compile(regex).matcher(text);
		int n = 0;
		while (m.find()) {
			n += 1;
		}
		return n;
	} // end of count method

} // end of VerilogStructure class
