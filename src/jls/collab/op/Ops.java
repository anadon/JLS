package jls.collab.op;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.ElementId;

/**
 * Shared helpers for the op vocabulary (issue #167): stable-id
 * resolution and the save-format string escaping. The escaping mirrors
 * {@code Attribute.StringAttribute.save} and the loader's single
 * left-to-right unescape scan (issue #53) exactly; issue #79 may later
 * centralize both under the save-format stewardship work.
 */
final class Ops {

	private Ops() {

	} // end of constructor

	/**
	 * Find the element a stable id addresses.
	 *
	 * @param circuit The circuit to search.
	 * @param id The stable id.
	 *
	 * @return the element.
	 *
	 * @throws OpRejected if no element in the circuit has that id.
	 */
	static Element resolve(Circuit circuit, ElementId id)
			throws OpRejected {

		for (Element el : circuit.getElements()) {
			if (el.getStableId().equals(id)) {
				return el;
			}
		}
		throw new OpRejected("no element with stable id '" + id
				+ "' exists in the circuit");
	} // end of resolve method

	/**
	 * Escape a string value the way the save format's writer does:
	 * backslash, quote and newline become two-character sequences.
	 *
	 * @param value The raw string.
	 *
	 * @return the escaped form, without surrounding quotes.
	 */
	static String escape(String value) {

		String str = value.replace("\\", "\\\\");
		str = str.replace("\"", "\\\"");
		return str.replace("\n", "\\n");
	} // end of escape method

	/**
	 * Write one {@code String} typed line in the save-format idiom.
	 *
	 * @param output The output writer.
	 * @param key The field name.
	 * @param value The raw (unescaped) value.
	 */
	static void saveString(java.io.PrintWriter output, String key,
			String value) {

		output.println(" String " + key + " \"" + escape(value) + "\"");
	} // end of saveString method

	/**
	 * Write one {@code int} typed line in the save-format idiom.
	 *
	 * @param output The output writer.
	 * @param key The field name.
	 * @param value The value.
	 */
	static void saveInt(java.io.PrintWriter output, String key,
			int value) {

		output.println(" int " + key + " " + value);
	} // end of saveInt method

} // end of Ops class
