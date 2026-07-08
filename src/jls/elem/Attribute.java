package jls.elem;

import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * One saved attribute of an element (issue #23): its name, its on-disk
 * type tag, how to read and write it on the element, and when to omit it
 * from a save. A single declaration drives saving, copying, and load
 * dispatch, so the three can no longer drift apart (the historical defect
 * class: an attribute added to save/load but missed in copy broke only
 * paste and undo).
 *
 * Elements declare a list of these (see {@link Element#savedAttributes()})
 * in exactly their historical save order; generic save output is
 * byte-identical to the handwritten code it replaces, which the round-trip
 * suite (#14) verifies.
 *
 * Implementations are anonymous subclasses next to the fields they access,
 * with named get/set methods, so stack traces stay readable.
 */
public abstract class Attribute {

	/** The attribute name as it appears in saved files. */
	protected final String name;

	protected Attribute(String name) {

		this.name = name;
	} // end of constructor

	/**
	 * Write this attribute's save line, or nothing when omitted.
	 *
	 * @param el The element to read from.
	 * @param output The writer to print to.
	 */
	public abstract void save(Element el, PrintWriter output);

	/**
	 * Copy this attribute's value from one element to another of the same
	 * class. Attributes that are not element state (e.g. the save-time id)
	 * override this to do nothing.
	 *
	 * @param from The element to read from.
	 * @param to The element to write to.
	 */
	public abstract void copy(Element from, Element to);

	/**
	 * Offer an int value from the loader.
	 *
	 * @return true if this attribute consumed it.
	 */
	public boolean setInt(Element el, String name, int value) {

		return false;
	} // end of setInt method

	/** Offer a long value from the loader. */
	public boolean setLong(Element el, String name, long value) {

		return false;
	} // end of setLong method

	/** Offer a BigInteger value from the loader. */
	public boolean setBigInt(Element el, String name, BigInteger value) {

		return false;
	} // end of setBigInt method

	/** Offer a String value from the loader. */
	public boolean setString(Element el, String name, String value) {

		return false;
	} // end of setString method

	/**
	 * An int-typed attribute (" int name value").
	 */
	public abstract static class IntAttribute extends Attribute {

		protected IntAttribute(String name) {

			super(name);
		} // end of constructor

		protected abstract int get(Element el);

		protected abstract void set(Element el, int value);

		/** Override to skip the save line (defaults, recomputed values). */
		protected boolean omitted(Element el) {

			return false;
		} // end of omitted method

		public void save(Element el, PrintWriter output) {

			if (!omitted(el)) {
				output.println(" int " + name + " " + get(el));
			}
		} // end of save method

		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		public boolean setInt(Element el, String name, int value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, value);
			return true;
		} // end of setInt method

	} // end of IntAttribute class

	/**
	 * A BigInteger-typed attribute (" Int name value"). Also accepts a
	 * long from the loader, for files saved by old JLS versions that
	 * wrote these values as longs.
	 */
	public abstract static class BigIntAttribute extends Attribute {

		protected BigIntAttribute(String name) {

			super(name);
		} // end of constructor

		protected abstract BigInteger get(Element el);

		protected abstract void set(Element el, BigInteger value);

		public void save(Element el, PrintWriter output) {

			output.println(" Int " + name + " " + get(el).toString());
		} // end of save method

		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		public boolean setBigInt(Element el, String name, BigInteger value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, value);
			return true;
		} // end of setBigInt method

		public boolean setLong(Element el, String name, long value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, BigInteger.valueOf(value));
			return true;
		} // end of setLong method

	} // end of BigIntAttribute class

	/**
	 * A String-typed attribute (" String name \"value\""), saved with the
	 * standard escaping.
	 */
	public abstract static class StringAttribute extends Attribute {

		protected StringAttribute(String name) {

			super(name);
		} // end of constructor

		protected abstract String get(Element el);

		protected abstract void set(Element el, String value);

		/** Override to skip the save line. */
		protected boolean omitted(Element el) {

			return false;
		} // end of omitted method

		public void save(Element el, PrintWriter output) {

			if (omitted(el)) {
				return;
			}
			String str = get(el).replace("\\", "\\\\");
			str = str.replace("\"", "\\\"");
			str = str.replace("\n", "\\n");
			output.println(" String " + name + " \"" + str + "\"");
		} // end of save method

		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		public boolean setString(Element el, String name, String value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, value);
			return true;
		} // end of setString method

	} // end of StringAttribute class

} // end of Attribute class
