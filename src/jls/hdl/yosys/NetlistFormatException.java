package jls.hdl.yosys;

/**
 * Thrown when a Yosys {@code write_json} netlist cannot be understood
 * (issue #61): the text is not well-formed JSON, or it is JSON whose
 * shape does not match the {@code write_json} schema (a missing
 * {@code "modules"} object, a port without a direction, a bit entry
 * that is neither a net number nor one of the constant strings, and
 * so on). The message always names the location of the problem —
 * line and column for syntax errors, the module/cell/port path for
 * shape errors — because the person reading it is debugging a
 * generated file they did not write. Distinct from an import
 * <em>rejection</em> (a well-formed netlist containing cells outside
 * the restricted set), which is reported through
 * {@link jls.hdl.yosys.CellValidator} instead.
 */
public class NetlistFormatException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception.
	 *
	 * @param message a description locating and explaining the
	 * malformed construct.
	 */
	public NetlistFormatException(String message) {

		super(message);
	} // end of constructor

} // end of NetlistFormatException class
