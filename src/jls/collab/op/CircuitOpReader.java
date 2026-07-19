package jls.collab.op;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import jls.elem.ElementId;

/**
 * Parse one serialized {@link CircuitOp} block (issue #167): the exact
 * inverse of each op's {@code save}. Strict by design - this grammar is
 * the future network surface (issues #163/#170), so unknown kinds,
 * unknown fields, malformed lines, missing fields, and oversized input
 * are all rejections, never best-effort repairs.
 *
 * Grammar (save-format idiom):
 *
 * <pre>
 * OP &lt;kind&gt;
 *  String &lt;key&gt; "&lt;escaped value&gt;"
 *  int &lt;key&gt; &lt;value&gt;
 * END
 * </pre>
 */
public final class CircuitOpReader {

	/** Hostile-input caps (issue #38 discipline). */
	private static final int MAX_IDS = 10_000;
	/** Longest escaped string value accepted for ordinary fields, in characters. */
	private static final int MAX_STRING = 10_000;
	/** Most element blocks accepted in one op block. */
	private static final int MAX_BLOCKS = 1_000;
	/** Most lines accepted in one op block before its END line. */
	private static final int MAX_LINES = MAX_IDS + 16;

	/**
	 * Prevent instantiation: this class is all static methods.
	 */
	private CircuitOpReader() {

	} // end of constructor

	/**
	 * Read the next op block from the scanner.
	 *
	 * @param input The input, positioned at an {@code OP} line.
	 *
	 * @return the parsed op.
	 *
	 * @throws OpRejected if the input is not a well-formed op block.
	 */
	public static CircuitOp read(Scanner input) throws OpRejected {

		if (!input.hasNextLine()) {
			throw new OpRejected("expected an OP line, found end of input");
		}
		String header = input.nextLine();
		if (!header.startsWith("OP ")) {
			throw new OpRejected("expected an OP line, found '"
					+ clip(header) + "'");
		}
		String kind = header.substring(3).trim();

		List<ElementId> ids = new ArrayList<ElementId>();
		List<String> blocks = new ArrayList<String>();
		String name = null;
		Integer dx = null;
		Integer dy = null;
		Integer cw = null;
		int lines = 0;
		while (true) {
			if (!input.hasNextLine()) {
				throw new OpRejected("op block for '" + kind
						+ "' is missing its END line");
			}
			if (lines >= MAX_LINES) {
				throw new OpRejected("op block for '" + kind
						+ "' exceeds " + MAX_LINES + " lines");
			}
			String line = input.nextLine();
			lines += 1;
			if (line.equals("END")) {
				break;
			}
			if (line.startsWith(" String id ")) {
				if (ids.size() >= MAX_IDS) {
					throw new OpRejected("op block for '" + kind
							+ "' lists more than " + MAX_IDS + " ids");
				}
				ids.add(parseId(unquote(line.substring(11), "id")));
			} else if (line.startsWith(" String block ")) {
				if (blocks.size() >= MAX_BLOCKS) {
					throw new OpRejected("op block for '" + kind
							+ "' lists more than " + MAX_BLOCKS
							+ " element blocks");
				}
				blocks.add(unquote(line.substring(14), "block",
						ElementBlocks.MAX_BLOCK));
			} else if (line.startsWith(" String name ")) {
				if (name != null) {
					throw new OpRejected("duplicate 'name' field");
				}
				name = unquote(line.substring(13), "name");
			} else if (line.startsWith(" int dx ")) {
				dx = parseInt(dx, line.substring(8), "dx");
			} else if (line.startsWith(" int dy ")) {
				dy = parseInt(dy, line.substring(8), "dy");
			} else if (line.startsWith(" int cw ")) {
				cw = parseInt(cw, line.substring(8), "cw");
			} else {
				throw new OpRejected("unrecognized line '" + clip(line)
						+ "' in op block for '" + kind + "'");
			}
		}

		switch (kind) {
		case "ToggleWatched":
			requireFields(kind, ids.size() == 1 && blocks.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw == null);
			return new ToggleWatched(ids.get(0));
		case "AttachProbe":
			requireFields(kind, ids.size() == 1 && blocks.isEmpty()
					&& name != null && !name.isEmpty() && dx == null
					&& dy == null && cw == null);
			return new AttachProbe(ids.get(0), name);
		case "RemoveProbe":
			requireFields(kind, ids.size() == 1 && blocks.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw == null);
			return new RemoveProbe(ids.get(0));
		case "RotateElement":
			requireFields(kind, ids.size() == 1 && blocks.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw != null && (cw == 0 || cw == 1));
			return new RotateElement(ids.get(0), cw == 1);
		case "FlipElement":
			requireFields(kind, ids.size() == 1 && blocks.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw == null);
			return new FlipElement(ids.get(0));
		case "MoveElements":
			requireFields(kind, !ids.isEmpty() && blocks.isEmpty()
					&& name == null && dx != null && dy != null
					&& cw == null);
			return new MoveElements(ids, dx, dy);
		case "AddElements":
			requireFields(kind, !blocks.isEmpty() && ids.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw == null);
			return new AddElements(blocks);
		case "RemoveElements":
			requireFields(kind, !ids.isEmpty() && blocks.isEmpty()
					&& name == null && dx == null && dy == null
					&& cw == null);
			return new RemoveElements(ids);
		default:
			throw new OpRejected("unknown op kind '" + clip(kind) + "'");
		}
	} // end of read method

	/**
	 * Reject a kind whose field combination is wrong.
	 *
	 * @param kind The op kind being parsed.
	 * @param wellFormed Whether the fields match the kind's shape.
	 *
	 * @throws OpRejected if not well formed.
	 */
	private static void requireFields(String kind, boolean wellFormed)
			throws OpRejected {

		if (!wellFormed) {
			throw new OpRejected("op block for '" + kind
					+ "' does not have that kind's fields");
		}
	} // end of requireFields method

	/**
	 * Parse a stable id, converting the unchecked parse failure into a
	 * rejection.
	 *
	 * @param text The id's string form.
	 *
	 * @return the id.
	 *
	 * @throws OpRejected if malformed.
	 */
	private static ElementId parseId(String text) throws OpRejected {

		try {
			return ElementId.parse(text);
		} catch (IllegalArgumentException ex) {
			throw new OpRejected(ex.getMessage());
		}
	} // end of parseId method

	/**
	 * Parse one int field value, rejecting duplicates and non-numbers.
	 *
	 * @param existing The previously seen value for this key, or null.
	 * @param text The raw value text.
	 * @param key The field name, for the message.
	 *
	 * @return the parsed value.
	 *
	 * @throws OpRejected on a duplicate or malformed value.
	 */
	private static Integer parseInt(Integer existing, String text,
			String key) throws OpRejected {

		if (existing != null) {
			throw new OpRejected("duplicate '" + key + "' field");
		}
		try {
			return Integer.valueOf(text.trim());
		} catch (NumberFormatException ex) {
			throw new OpRejected("the value of '" + key
					+ "' should be an integer, but '" + clip(text)
					+ "' is not");
		}
	} // end of parseInt method

	/**
	 * Decode a quoted, escaped string value: the single left-to-right
	 * scan that exactly inverts the writer's escaping (issue #53).
	 *
	 * @param raw The rest of the line, including the surrounding quotes.
	 * @param key The field name, for the message.
	 *
	 * @return the decoded string.
	 *
	 * @throws OpRejected if no quoted value is present or it is too long.
	 */
	private static String unquote(String raw, String key)
			throws OpRejected {

		return unquote(raw, key, MAX_STRING);
	} // end of unquote method

	/**
	 * Decode a quoted, escaped string value against an explicit length
	 * cap - element blocks are legitimately much longer than names.
	 *
	 * @param raw The rest of the line, including the surrounding quotes.
	 * @param key The field name, for the message.
	 * @param max The longest escaped value accepted, in characters.
	 *
	 * @return the decoded string.
	 *
	 * @throws OpRejected if no quoted value is present or it is too long.
	 */
	private static String unquote(String raw, String key, int max)
			throws OpRejected {

		int start = raw.indexOf('"');
		int end = raw.lastIndexOf('"');
		if (start < 0 || end <= start) {
			throw new OpRejected("the value of '" + key
					+ "' should be a quoted string, but '" + clip(raw)
					+ "' is not");
		}
		String escaped = raw.substring(start + 1, end);
		if (escaped.length() > max) {
			throw new OpRejected("the value of '" + key + "' exceeds "
					+ max + " characters");
		}
		StringBuilder value = new StringBuilder(escaped.length());
		for (int i = 0; i < escaped.length(); i += 1) {
			char ch = escaped.charAt(i);
			if (ch == '\\' && i + 1 < escaped.length()) {
				char next = escaped.charAt(i + 1);
				if (next == 'n') {
					value.append('\n');
					i += 1;
				} else if (next == '\\' || next == '"') {
					value.append(next);
					i += 1;
				} else {
					// not a writer-produced escape; keep it verbatim
					value.append(ch);
				}
			} else {
				value.append(ch);
			}
		}
		return value.toString();
	} // end of unquote method

	/**
	 * Clip untrusted text for an error message.
	 *
	 * @param text The text.
	 *
	 * @return at most 60 characters of it.
	 */
	private static String clip(String text) {

		return text.length() <= 60 ? text : text.substring(0, 60) + "…";
	} // end of clip method

} // end of CircuitOpReader class
