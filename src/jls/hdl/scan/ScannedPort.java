package jls.hdl.scan;

/**
 * One port scanned from an HDL module or entity header (issue #63):
 * its name as written in the file, its direction, and its width in
 * bits with all parameters/generics and ranges already evaluated.
 * Immutable; produced by {@link VerilogHeaderScanner} and
 * {@link VhdlEntityScanner} inside a {@link ScannedModule}.
 */
public final class ScannedPort {

	/** Which way data flows through the port, as declared in the file. */
	public enum Direction {
		/** An input port ({@code input} / {@code in}). */
		IN,
		/** An output port ({@code output} / {@code out} / {@code buffer}). */
		OUT,
		/** A bidirectional port ({@code inout}). */
		INOUT
	} // end of Direction enum

	/** The port name exactly as written in the source file. */
	public final String name;

	/** The declared direction. */
	public final Direction direction;

	/** The evaluated width in bits, always at least 1. */
	public final int bits;

	/**
	 * Builds an immutable scanned-port descriptor.
	 * @param name the port name exactly as written in the source
	 * @param direction the declared direction
	 * @param bits the evaluated width in bits, at least 1
	 */
	ScannedPort(String name, Direction direction, int bits) {
		this.name = name;
		this.direction = direction;
		this.bits = bits;
	} // end of ScannedPort constructor

	/**
	 * @return a compact human-readable form, e.g. {@code "in a[4]"}
	 */
	@Override
	public String toString() {
		return direction.toString().toLowerCase(java.util.Locale.ROOT)
				+ " " + name + "[" + bits + "]";
	} // end of toString method

} // end of ScannedPort class
