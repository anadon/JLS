package jls.hdl.yosys;

/**
 * One cell the importer cannot map (issue #61): where it is, what it
 * is, and the message telling the user what to do about it. The
 * validator reports every violation in a netlist at once, so a failed
 * import is a complete list of these, and the eventual import dialog
 * renders {@link #describe()} lines. The message is a teaching
 * surface (issue #61 addendum): it names the construct, why it is
 * out of scope for JLS, and the rewrite that will import.
 *
 * @param module The name of the module containing the cell.
 * @param cell The cell's netlist name (often auto-generated).
 * @param type The Yosys cell type, e.g. {@code $adff}.
 * @param sourceLocation The Verilog source location Yosys recorded
 * (e.g. {@code "counter.v:8.3-10.6"}), or "" when it recorded none.
 * @param message The teaching message: construct, reason, rewrite.
 */
public record CellViolation(String module, String cell, String type,
		String sourceLocation, String message) {

	/**
	 * The violation as one human-readable line: the source location
	 * when Yosys recorded one (the place the user should look),
	 * otherwise the module and cell name, then the message.
	 *
	 * @return the formatted line.
	 */
	public String describe() {

		String at;
		if (sourceLocation.isEmpty()) {
			at = "module \"" + module + "\", cell \"" + cell + "\" ("
					+ type + ")";
		}
		else {
			at = sourceLocation + " (" + type + ")";
		}
		return at + ": " + message;
	} // end of describe method

} // end of CellViolation record
