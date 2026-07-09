package jls.hdl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic legalization of JLS names into HDL identifiers
 * (issue #60). The rule, in order:
 *
 * <ol>
 * <li>Every character outside {@code [A-Za-z0-9_]} becomes {@code _}
 * (JLS allows any Unicode letter; Verilog-2005 does not).</li>
 * <li>An empty result, or one starting with a digit, is prefixed with
 * {@code id_} (cannot happen for names JLS itself validated, but the
 * rule must be total).</li>
 * <li>A reserved word gets {@code _} appended.</li>
 * <li>A name already taken gets {@code _2}, {@code _3}, ... appended,
 * in the (deterministic) order names are requested.</li>
 * </ol>
 *
 * One instance is one namespace: module ports, nets and register
 * variables all draw from it, so emitted identifiers never collide.
 */
final class HdlNames {

	/**
	 * IEEE 1364-2005 reserved words (Annex B). Deliberately Verilog
	 * only: renaming every "out" and "in" for VHDL's benefit would
	 * mangle the most common port names in this version's only target
	 * language. When the VHDL emitter lands it applies its own
	 * additional legalization pass.
	 */
	private static final Set<String> RESERVED = new HashSet<String>(
			Arrays.asList(
			"always", "and", "assign", "automatic", "begin", "buf",
			"bufif0", "bufif1", "case", "casex", "casez", "cell", "cmos",
			"config", "deassign", "default", "defparam", "design",
			"disable", "edge", "else", "end", "endcase", "endconfig",
			"endfunction", "endgenerate", "endmodule", "endprimitive",
			"endspecify", "endtable", "endtask", "event", "for", "force",
			"forever", "fork", "function", "generate", "genvar",
			"highz0", "highz1", "if", "ifnone", "incdir", "include",
			"initial", "inout", "input", "instance", "integer", "join",
			"large", "liblist", "library", "localparam", "macromodule",
			"medium", "module", "nand", "negedge", "nmos", "nor",
			"noshowcancelled", "not", "notif0", "notif1", "or", "output",
			"parameter", "pmos", "posedge", "primitive", "pull0",
			"pull1", "pulldown", "pullup", "pulsestyle_ondetect",
			"pulsestyle_onevent", "rcmos", "real", "realtime", "reg",
			"release", "repeat", "rnmos", "rpmos", "rtran", "rtranif0",
			"rtranif1", "scalared", "showcancelled", "signed", "small",
			"specify", "specparam", "strong0", "strong1", "supply0",
			"supply1", "table", "task", "time", "tran", "tranif0",
			"tranif1", "tri", "tri0", "tri1", "triand", "trior",
			"trireg", "unsigned", "use", "uwire", "vectored", "wait",
			"wand", "weak0", "weak1", "while", "wire", "wor", "xnor",
			"xor"));

	private final Set<String> used = new HashSet<String>();
	private final Map<String, String> renames =
			new LinkedHashMap<String, String>();

	/**
	 * Legalize a JLS-visible name and claim it in this namespace,
	 * recording the mapping when legalization changed it.
	 *
	 * @param jlsName The user's name.
	 *
	 * @return the claimed identifier.
	 */
	String reserve(String jlsName) {

		String id = unique(sanitize(jlsName));
		if (!id.equals(jlsName)) {
			renames.put(jlsName, id);
		}
		return id;
	} // end of reserve method

	/**
	 * Claim a synthesized (non-user-visible) name; no rename is
	 * recorded even if uniquification altered it.
	 *
	 * @param base The desired name (must not need sanitizing beyond
	 * what sanitize() does; it is applied anyway for safety).
	 *
	 * @return the claimed identifier.
	 */
	String synth(String base) {

		return unique(sanitize(base));
	} // end of synth method

	/** The recorded JLS-name-to-identifier changes, in claim order. */
	Map<String, String> renames() {

		return renames;
	} // end of renames method

	private String unique(String id) {

		String candidate = id;
		int n = 2;
		while (!used.add(candidate)) {
			candidate = id + "_" + n;
			n += 1;
		}
		return candidate;
	} // end of unique method

	/** Steps 1-3 of the rule (character set, first char, keywords). */
	private static String sanitize(String name) {

		StringBuilder sb = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i += 1) {
			char c = name.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
					|| (c >= '0' && c <= '9') || c == '_') {
				sb.append(c);
			}
			else {
				sb.append('_');
			}
		}
		String id = sb.toString();
		if (id.isEmpty() || Character.isDigit(id.charAt(0))) {
			id = "id_" + id;
		}
		if (RESERVED.contains(id)) {
			// Verilog is case-sensitive and its keywords are all
			// lowercase, so exact match is the right test
			id = id + "_";
		}
		return id;
	} // end of sanitize method

} // end of HdlNames class
