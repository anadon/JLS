package jls.collab.op;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import jls.Circuit;
import jls.JLSInfo;
import jls.LoadError;
import jls.elem.Element;

/**
 * The element-transplant helper (issue #167): serialize one element as a
 * standalone save-format block, and load such a block into a target
 * circuit. This is the machinery behind {@link AddElements} and the true
 * inverse of {@link RemoveElements} - an element travels between
 * circuits (or, later, between collaborating peers - issue #163) as the
 * exact bytes its own {@code save} method writes, read back through the
 * exact reader the file loader uses, so a transplanted element is
 * indistinguishable from a loaded one.
 */
final class ElementBlocks {

	/**
	 * Hostile-input cap (issue #38 discipline): the longest element
	 * block, in characters, that {@link #load} or the op reader accepts.
	 */
	static final int MAX_BLOCK = 100_000;

	private ElementBlocks() {

	} // end of constructor

	/**
	 * Serialize one element as a standalone block, byte-identical on
	 * every platform: the element's own save output with every line
	 * terminated by a canonical {@code '\n'} (the same discipline as
	 * {@code Circuit.save}, issue #166).
	 *
	 * @param el The element to serialize.
	 *
	 * @return the block text, from its {@code ELEMENT} line through its
	 *         {@code END} line.
	 */
	static String saveBlock(Element el) {

		StringWriter text = new StringWriter();
		PrintWriter out = new PrintWriter(text) {
			/**
			 * Terminate each println with a canonical '\n' instead of
			 * the platform line separator.
			 */
			@Override
			public void println() {
				write('\n');
			}
		};
		el.save(out);
		out.flush();
		return text.toString();
	} // end of saveBlock method

	/**
	 * Construct and load the element a block describes. The element is
	 * owned by the given circuit (its attribute setters that register
	 * names register them there), but the read itself runs through a
	 * scratch circuit's loader so the target's load bookkeeping is never
	 * touched. The caller is responsible for {@code init} and
	 * {@code addElement} - this method only builds the element.
	 *
	 * Strict by design: a block that is not exactly one well-formed
	 * element, of a kind this vocabulary may add, with a declared stable
	 * id, is a rejection, never a repair.
	 *
	 * @param owner The circuit the new element will belong to.
	 * @param block The block text, from {@code ELEMENT} through
	 *            {@code END}.
	 *
	 * @return the loaded element, not yet initialized or added.
	 *
	 * @throws OpRejected if the block is oversized, malformed, of an
	 *             excluded or unknown type, or lacks a stable id.
	 */
	static Element load(Circuit owner, String block) throws OpRejected {

		if (block.length() > MAX_BLOCK) {
			throw new OpRejected("an element block exceeds " + MAX_BLOCK
					+ " characters");
		}
		Scanner in = new Scanner(block);
		if (!in.hasNext() || !in.next().equals("ELEMENT")) {
			throw new OpRejected(
					"an element block must start with an ELEMENT line");
		}
		if (!in.hasNext()) {
			throw new OpRejected("an element block ends where its type "
					+ "name should be");
		}
		String type = in.next();
		if (type.equals("Wire") || type.equals("WireEnd")) {
			throw new OpRejected("wires travel through the wiring op "
					+ "kinds, not through element blocks");
		}
		if (type.equals("SubCircuit")) {
			throw new OpRejected("subcircuits travel through the "
					+ "subcircuit-import op kind, not through element "
					+ "blocks");
		}
		Element el;
		try {
			Class<? extends Element> c =
					Class.forName("jls.elem." + type)
							.asSubclass(Element.class);
			el = c.getConstructor(Circuit.class).newInstance(owner);
		} catch (ReflectiveOperationException ex) {
			throw new OpRejected("'" + type + "' is not an element type "
					+ "this version of JLS can add");
		} catch (ClassCastException ex) {
			throw new OpRejected("'" + type + "' is not an element type "
					+ "this version of JLS can add");
		}
		Circuit loader = new Circuit("");
		if (!loader.loadElement(el, in)) {
			LoadError err = JLSInfo.lastLoadError;
			throw new OpRejected("the block for element type '" + type
					+ "' does not load"
					+ (err == null ? "" : ": " + err.getDetail()));
		}
		if (in.hasNext()) {
			throw new OpRejected("the block for element type '" + type
					+ "' has content after its END line");
		}
		if (!el.hasFileStableId()) {
			throw new OpRejected("the block for element type '" + type
					+ "' does not declare a stable id (sid)");
		}
		return el;
	} // end of load method

} // end of ElementBlocks class
