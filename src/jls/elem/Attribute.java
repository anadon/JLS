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

	/**
	 * Create a new attribute declaration.
	 *
	 * @param name The attribute name as it appears in saved files.
	 */
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
	 * @param el The element to write to.
	 * @param name The attribute name read from the file.
	 * @param value The loaded value.
	 * @return true if this attribute consumed it.
	 */
	public boolean setInt(Element el, String name, int value) {

		return false;
	} // end of setInt method

	/**
	 * Offer a long value from the loader.
	 *
	 * @param el The element to write to.
	 * @param name The attribute name read from the file.
	 * @param value The loaded value.
	 * @return true if this attribute consumed it.
	 */
	public boolean setLong(Element el, String name, long value) {

		return false;
	} // end of setLong method

	/**
	 * Offer a BigInteger value from the loader.
	 *
	 * @param el The element to write to.
	 * @param name The attribute name read from the file.
	 * @param value The loaded value.
	 * @return true if this attribute consumed it.
	 */
	public boolean setBigInt(Element el, String name, BigInteger value) {

		return false;
	} // end of setBigInt method

	/**
	 * Offer a String value from the loader.
	 *
	 * @param el The element to write to.
	 * @param name The attribute name read from the file.
	 * @param value The loaded value.
	 * @return true if this attribute consumed it.
	 */
	public boolean setString(Element el, String name, String value) {

		return false;
	} // end of setString method

	/**
	 * An int-typed attribute (" int name value").
	 */
	public abstract static class IntAttribute extends Attribute {

		/**
		 * Create a new int-typed attribute declaration.
		 *
		 * @param name The attribute name as it appears in saved files.
		 */
		protected IntAttribute(String name) {

			super(name);
		} // end of constructor

		/**
		 * Read the int value from the element.
		 *
		 * @param el The element to read from.
		 * @return the current value.
		 */
		protected abstract int get(Element el);

		/**
		 * Write the int value onto the element.
		 *
		 * @param el The element to write to.
		 * @param value The value to store.
		 */
		protected abstract void set(Element el, int value);

		/**
		 * Override to skip the save line (defaults, recomputed values).
		 *
		 * @param el The element to read from.
		 * @return true if the save line should be omitted.
		 */
		protected boolean omitted(Element el) {

			return false;
		} // end of omitted method

		/**
		 * Write the " int name value" save line, unless omitted.
		 */
		@Override
		public void save(Element el, PrintWriter output) {

			if (!omitted(el)) {
				output.println(" int " + name + " " + get(el));
			}
		} // end of save method

		/**
		 * Copy the int value from one element to another.
		 */
		@Override
		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		/**
		 * Consume a loaded int value if its name matches this attribute.
		 */
		@Override
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

		/**
		 * Create a new BigInteger-typed attribute declaration.
		 *
		 * @param name The attribute name as it appears in saved files.
		 */
		protected BigIntAttribute(String name) {

			super(name);
		} // end of constructor

		/**
		 * Read the BigInteger value from the element.
		 *
		 * @param el The element to read from.
		 * @return the current value.
		 */
		protected abstract BigInteger get(Element el);

		/**
		 * Write the BigInteger value onto the element.
		 *
		 * @param el The element to write to.
		 * @param value The value to store.
		 */
		protected abstract void set(Element el, BigInteger value);

		/**
		 * Write the " Int name value" save line.
		 */
		@Override
		public void save(Element el, PrintWriter output) {

			output.println(" Int " + name + " " + get(el).toString());
		} // end of save method

		/**
		 * Copy the BigInteger value from one element to another.
		 */
		@Override
		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		/**
		 * Consume a loaded BigInteger value if its name matches this attribute.
		 */
		@Override
		public boolean setBigInt(Element el, String name, BigInteger value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, value);
			return true;
		} // end of setBigInt method

		/**
		 * Consume a loaded long value (from old files) if its name matches.
		 */
		@Override
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

		/**
		 * Create a new String-typed attribute declaration.
		 *
		 * @param name The attribute name as it appears in saved files.
		 */
		protected StringAttribute(String name) {

			super(name);
		} // end of constructor

		/**
		 * Read the String value from the element.
		 *
		 * @param el The element to read from.
		 * @return the current value.
		 */
		protected abstract String get(Element el);

		/**
		 * Write the String value onto the element.
		 *
		 * @param el The element to write to.
		 * @param value The value to store.
		 */
		protected abstract void set(Element el, String value);

		/**
		 * Override to skip the save line.
		 *
		 * @param el The element to read from.
		 * @return true if the save line should be omitted.
		 */
		protected boolean omitted(Element el) {

			return false;
		} // end of omitted method

		/**
		 * Write the " String name \"value\"" save line with escaping, unless
		 * omitted.
		 */
		@Override
		public void save(Element el, PrintWriter output) {

			if (omitted(el)) {
				return;
			}
			String str = get(el).replace("\\", "\\\\");
			str = str.replace("\"", "\\\"");
			str = str.replace("\n", "\\n");
			output.println(" String " + name + " \"" + str + "\"");
		} // end of save method

		/**
		 * Copy the String value from one element to another.
		 */
		@Override
		public void copy(Element from, Element to) {

			set(to, get(from));
		} // end of copy method

		/**
		 * Consume a loaded String value if its name matches this attribute.
		 */
		@Override
		public boolean setString(Element el, String name, String value) {

			if (!this.name.equals(name)) {
				return false;
			}
			set(el, value);
			return true;
		} // end of setString method

	} // end of StringAttribute class

	/**
	 * A String-typed attribute holding a JLSInfo.Orientation. Saves the
	 * enum name; loading an unknown string leaves the orientation
	 * unchanged, as every handwritten loader did.
	 */
	public abstract static class OrientationAttribute extends StringAttribute {

		/**
		 * Create a new orientation attribute declaration.
		 *
		 * @param name The attribute name as it appears in saved files.
		 */
		protected OrientationAttribute(String name) {

			super(name);
		} // end of constructor

		/**
		 * Read the orientation from the element.
		 *
		 * @param el The element to read from.
		 * @return the current orientation.
		 */
		protected abstract jls.JLSInfo.Orientation getOrientation(Element el);

		/**
		 * Write the orientation onto the element.
		 *
		 * @param el The element to write to.
		 * @param value The orientation to store.
		 */
		protected abstract void setOrientation(Element el,
				jls.JLSInfo.Orientation value);

		/**
		 * The orientation's enum name, for the String save line.
		 */
		@Override
		protected String get(Element el) {

			return getOrientation(el).toString();
		} // end of get method

		/**
		 * Parse a saved orientation name, leaving it unchanged if unknown.
		 */
		@Override
		protected void set(Element el, String value) {

			setOrientation(el, jls.JLSInfo.Orientation.parse(value,
					getOrientation(el)));
		} // end of set method

	} // end of OrientationAttribute class

} // end of Attribute class
