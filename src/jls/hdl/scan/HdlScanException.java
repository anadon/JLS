package jls.hdl.scan;

/**
 * Raised when an HDL source file steps outside the subset the header
 * scanners handle, or is malformed (issue #63). The message is a
 * one-line, user-facing reason ("function-like macros are not
 * supported", "cannot evaluate width expression"), because the HDL
 * component dialog shows it verbatim; files rejected this way are
 * candidates for the external Yosys extraction path rather than a
 * scanner extension (issue #63 &#167;9).
 */
public class HdlScanException extends Exception {

	/** Serialization version, required of every Exception subclass. */
	private static final long serialVersionUID = 1L;

	/** 1-based source line the problem was found on, or 0 if unknown. */
	private final int line;

	/**
	 * Builds an exception with no useful line number.
	 * @param message one-line, user-facing reason for the rejection
	 */
	public HdlScanException(String message) {
		this(message, 0);
	} // end of one-argument HdlScanException constructor

	/**
	 * Builds an exception pointing at a source line.
	 * @param message one-line, user-facing reason for the rejection
	 * @param line 1-based source line of the problem, or 0 if unknown
	 */
	public HdlScanException(String message, int line) {
		super(line > 0 ? message + " (line " + line + ")" : message);
		this.line = line;
	} // end of two-argument HdlScanException constructor

	/**
	 * Gets the source line the problem was found on.
	 *
	 * @return the 1-based source line of the problem, or 0 if unknown
	 */
	public int line() {
		return line;
	} // end of line method

} // end of HdlScanException class
