package jls.hdl.imp;

/**
 * Thrown when a validated Yosys netlist cannot be realized as a JLS
 * circuit by {@link NetlistImporter} (issue #61). Two shapes reach
 * here: constructs the {@link jls.hdl.yosys.CellValidator} already
 * rejected (relayed with its teaching messages), and constructs the
 * validator accepts but this importer increment does not yet realize -
 * a hierarchy instance, a cell type outside the mapper's table, a
 * bit-level slice or concatenation that would need the Splitter/Binder
 * mesh, or a width mismatch that would need an Extend. Either way no
 * partial circuit is produced: the message enumerates every problem so
 * a single failed import tells the whole story (issue #61 P2, addendum
 * "reject-list ergonomics").
 */
public final class ImportException extends Exception {

	/** For serialization compatibility. */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception.
	 *
	 * @param message The full, multi-line explanation of every reason
	 * the netlist could not be imported.
	 */
	public ImportException(String message) {

		super(message);
	} // end of constructor

} // end of ImportException class
