package jls.elem;

/**
 * A truth-table value cell (0, 1, or don't-care) (#27 S4). Pure data
 * (issue #77): it holds the cell value; the GUI
 * ({@code jls.edit.DisplayBool}) draws it. InputVal and OutputVal differ
 * only in what a click does, which stays in the subclasses' selected()
 * methods.
 *
 * @author David A. Poplawski
 */
public abstract class ValEntry extends Entry {

	// properties
	/** The value displayed in this cell: 0 or 1, or 2 for don't-care. */
	int value = 0;  // 0 or 1, 2 = don't care

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 */
	public ValEntry(TruthTable ttelem, int value) {

		super(ttelem);
		this.value = value;
	} // end of constructor

	/**
	 * Get the value of this entry.
	 *
	 * @return the value.
	 */
	public int getValue() {

		return value;
	} // end of getValue method

} // end of ValEntry class
