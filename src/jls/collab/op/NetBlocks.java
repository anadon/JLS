package jls.collab.op;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.LogicElement;
import jls.elem.Put;
import jls.elem.WireEnd;

/**
 * The wire-net transplant helper (issue #167): serialize a whole wire
 * net as the exact per-end blocks {@code WireEnd.save} writes, and
 * parse such blocks back strictly. The wiring sibling of
 * {@link ElementBlocks} - a net travels as the byte form its own save
 * method produces, reconstructed by replaying the file loader's exact
 * algorithm, so a transplanted net is indistinguishable from a loaded
 * one.
 *
 * A wire-end block references other elements by the save format's
 * file-local {@code int} ids, which mean nothing outside a whole-file
 * save. A net serialization therefore renumbers locally: the elements
 * the net attaches to (by one of their puts) get local ids
 * {@code 0..k-1} in stable-id order, and the net's own ends get local
 * ids {@code k..k+n-1} in stable-id order. The attachment anchors
 * travel alongside the blocks as stable ids ({@link AddWire#attach}),
 * index-aligned with those local ids, so the blocks stay byte-exact
 * while every cross-reference outside the net is stable (issue #165).
 */
final class NetBlocks {

	/**
	 * One parsed wire-end block: everything {@code WireEnd.save} writes,
	 * with references still in the op's local numbering.
	 *
	 * @param sid The end's declared stable id.
	 * @param x The end's x-coordinate.
	 * @param y The end's y-coordinate.
	 * @param triState Whether the end's net is tri-state.
	 * @param attach The local id of the element a put of which this end
	 *            attaches to, or -1 when unattached.
	 * @param putName The name of the put attached to, or null when
	 *            unattached.
	 * @param wireRefs The local ids of the ends this end is wired to, in
	 *            the block's (save) order.
	 * @param probes Probe names keyed by the other end's local id, for
	 *            the wires that carry probes.
	 */
	record EndSpec(ElementId sid, int x, int y, boolean triState,
			int attach, @Nullable String putName, List<Integer> wireRefs,
			Map<Integer, String> probes) {
	} // end of EndSpec record

	/**
	 * Prevents instantiation; this class holds only static helpers.
	 */
	private NetBlocks() {

	} // end of constructor

	/**
	 * Serialize a whole wire net as an {@link AddWire} op: the inverse
	 * of {@link RemoveWire}. The given ends must be every end of one
	 * net, already sorted by stable id.
	 *
	 * The save-time {@code int} ids of the ends and of the elements the
	 * net attaches to are renumbered into the op's local numbering
	 * while the blocks are written, then restored to their prior
	 * values. The restore matters: although every circuit save
	 * reassigns all of these ids, HDL export consumes them directly
	 * for element ordering and synthesized net names, so serializing a
	 * net must leave no observable trace.
	 *
	 * @param ends The net's wire ends, in stable-id order.
	 *
	 * @return the op that reconstructs the net byte-exactly.
	 */
	static AddWire toAddWire(List<WireEnd> ends) {

		List<Element> targets = new ArrayList<Element>();
		for (WireEnd end : ends) {
			Put p = end.getPut();
			LogicElement owner = p == null ? null : p.getElement();
			if (owner != null && !targets.contains(owner)) {
				targets.add(owner);
			}
		}
		targets.sort(Comparator.comparing(Element::getStableId));
		List<Element> renumbered =
				new ArrayList<Element>(targets.size() + ends.size());
		renumbered.addAll(targets);
		renumbered.addAll(ends);
		int[] priorIds = new int[renumbered.size()];
		for (int i = 0; i < renumbered.size(); i += 1) {
			priorIds[i] = renumbered.get(i).getID();
		}
		try {
			List<ElementId> attach =
					new ArrayList<ElementId>(targets.size());
			for (int i = 0; i < targets.size(); i += 1) {
				targets.get(i).setID(i);
				attach.add(targets.get(i).getStableId());
			}
			int base = targets.size();
			for (int i = 0; i < ends.size(); i += 1) {
				ends.get(i).setID(base + i);
			}
			List<String> blocks = new ArrayList<String>(ends.size());
			for (WireEnd end : ends) {
				blocks.add(ElementBlocks.saveBlock(end));
			}
			return new AddWire(attach, blocks);
		} finally {
			for (int i = 0; i < renumbered.size(); i += 1) {
				renumbered.get(i).setID(priorIds[i]);
			}
		}
	} // end of toAddWire method

	/**
	 * Parse one wire-end block strictly: exactly the line sequence
	 * {@code WireEnd.save} writes, in its order, and nothing else.
	 * Anything a save could not have produced - unknown or reordered
	 * lines, an uneditable end, an end with no wires, references outside
	 * the op's local numbering - is a rejection, never a repair.
	 *
	 * @param block The block text, from {@code ELEMENT WireEnd} through
	 *            {@code END}.
	 * @param attachCount How many attachment anchors the op carries
	 *            (local ids {@code 0..attachCount-1}).
	 * @param endCount How many end blocks the op carries (local ids
	 *            {@code attachCount..attachCount+endCount-1}).
	 * @param index This block's position among the op's blocks.
	 *
	 * @return the parsed end.
	 *
	 * @throws OpRejected if the block is not exactly a well-formed
	 *             wire-end save block.
	 */
	static EndSpec parseEnd(String block, int attachCount, int endCount,
			int index) throws OpRejected {

		if (block.length() > ElementBlocks.MAX_BLOCK) {
			throw new OpRejected("a wire-end block exceeds "
					+ ElementBlocks.MAX_BLOCK + " characters");
		}
		String[] lines = block.split("\n", -1);
		// saveBlock output ends every line, END included, with '\n'
		if (lines.length < 7 || !lines[lines.length - 1].isEmpty()
				|| !lines[lines.length - 2].equals("END")) {
			throw new OpRejected("a wire-end block must end with an END "
					+ "line");
		}
		int endLine = lines.length - 2;
		int at = 0;
		if (!lines[at].equals("ELEMENT WireEnd")) {
			throw new OpRejected("a wire-end block must start with an "
					+ "'ELEMENT WireEnd' line");
		}
		at += 1;
		int id = intLine(lines[at], " int id ");
		at += 1;
		if (id != attachCount + index) {
			throw new OpRejected("wire-end block " + index + " declares "
					+ "local id " + id + ", not its position in the op's "
					+ "numbering");
		}
		int x = intLine(lines[at], " int x ");
		at += 1;
		int y = intLine(lines[at], " int y ");
		at += 1;
		if (x < 0 || y < 0) {
			throw new OpRejected(
					"a wire end would arrive off the canvas");
		}
		ElementId sid = parseSid(stringLine(lines[at], " String sid "));
		at += 1;
		boolean triState = false;
		if (at < endLine && lines[at].equals(" int tristate 1")) {
			triState = true;
			at += 1;
		}
		int attach = -1;
		String putName = null;
		if (at < endLine && lines[at].startsWith(" String put ")) {
			putName = stringLine(lines[at], " String put ");
			at += 1;
			if (putName.isEmpty()) {
				throw new OpRejected(
						"an attachment needs a non-empty put name");
			}
			if (at >= endLine) {
				throw new OpRejected("a put line must be followed by its "
						+ "'ref attach' line");
			}
			attach = intLine(lines[at], " ref attach ");
			at += 1;
			if (attach < 0 || attach >= attachCount) {
				throw new OpRejected("a wire end attaches to anchor "
						+ attach + ", which the op does not carry");
			}
		}
		List<Integer> wireRefs = new ArrayList<Integer>();
		Map<Integer, String> probes = new LinkedHashMap<Integer, String>();
		while (at < endLine) {
			int ref = intLine(lines[at], " ref wire ");
			at += 1;
			if (ref < attachCount || ref >= attachCount + endCount) {
				throw new OpRejected("a wire references end " + ref
						+ ", which the op does not carry");
			}
			if (ref == id) {
				throw new OpRejected("a wire end may not be wired to "
						+ "itself");
			}
			if (wireRefs.contains(ref)) {
				throw new OpRejected("a wire end lists end " + ref
						+ " twice");
			}
			wireRefs.add(ref);
			if (at < endLine && lines[at].startsWith(" probe ")) {
				String rest = lines[at].substring(7);
				int space = rest.indexOf(' ');
				if (space <= 0) {
					throw new OpRejected("a probe line must carry the "
							+ "other end's id and a quoted name");
				}
				int probeRef = parseInt(rest.substring(0, space),
						"probe");
				if (probeRef != ref) {
					throw new OpRejected("a probe line must follow the "
							+ "'ref wire' line of the wire it is on");
				}
				String name = unquote(rest.substring(space + 1), "probe");
				if (name.isEmpty()) {
					throw new OpRejected(
							"a probe needs a non-empty name");
				}
				probes.put(ref, name);
				at += 1;
			}
		}
		if (wireRefs.isEmpty()) {
			throw new OpRejected("a wire end needs at least one wire");
		}
		return new EndSpec(sid, x, y, triState, attach, putName,
				wireRefs, probes);
	} // end of parseEnd method

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
	private static ElementId parseSid(String text) throws OpRejected {

		try {
			return ElementId.parse(text);
		} catch (IllegalArgumentException ex) {
			throw new OpRejected(ex.getMessage() == null ? ex.toString()
					: ex.getMessage());
		}
	} // end of parseSid method

	/**
	 * Require one exact int-valued line.
	 *
	 * @param line The line.
	 * @param prefix The exact prefix the line must have.
	 *
	 * @return the int value after the prefix.
	 *
	 * @throws OpRejected if the line has a different shape.
	 */
	private static int intLine(String line, String prefix)
			throws OpRejected {

		if (!line.startsWith(prefix)) {
			throw new OpRejected("expected a '" + prefix.trim()
					+ "' line in a wire-end block, found '" + clip(line)
					+ "'");
		}
		return parseInt(line.substring(prefix.length()), prefix.trim());
	} // end of intLine method

	/**
	 * Parse one strict int value.
	 *
	 * @param text The value text.
	 * @param what The field, for the message.
	 *
	 * @return the value.
	 *
	 * @throws OpRejected if not an integer.
	 */
	private static int parseInt(String text, String what)
			throws OpRejected {

		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			throw new OpRejected("the value of '" + what + "' should be "
					+ "an integer, but '" + clip(text) + "' is not");
		}
	} // end of parseInt method

	/**
	 * Require one exact quoted-string line.
	 *
	 * @param line The line.
	 * @param prefix The exact prefix the line must have.
	 *
	 * @return the unquoted value.
	 *
	 * @throws OpRejected if the line has a different shape.
	 */
	private static String stringLine(String line, String prefix)
			throws OpRejected {

		if (!line.startsWith(prefix)) {
			throw new OpRejected("expected a '" + prefix.trim()
					+ "' line in a wire-end block, found '" + clip(line)
					+ "'");
		}
		return unquote(line.substring(prefix.length()), prefix.trim());
	} // end of stringLine method

	/**
	 * Unwrap one quoted value the way {@code WireEnd.save} writes it:
	 * surrounding quotes around raw text with no interior quote (the
	 * writer performs no escaping here, so a value a save could not have
	 * produced is a rejection).
	 *
	 * @param raw The rest of the line, including the surrounding quotes.
	 * @param what The field, for the message.
	 *
	 * @return the unquoted value.
	 *
	 * @throws OpRejected if not exactly one quoted value.
	 */
	private static String unquote(String raw, String what)
			throws OpRejected {

		if (raw.length() < 2 || raw.charAt(0) != '"'
				|| raw.charAt(raw.length() - 1) != '"'
				|| raw.substring(1, raw.length() - 1).indexOf('"') >= 0) {
			throw new OpRejected("the value of '" + what + "' should be "
					+ "a quoted string, but '" + clip(raw) + "' is not");
		}
		return raw.substring(1, raw.length() - 1);
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

} // end of NetBlocks class
