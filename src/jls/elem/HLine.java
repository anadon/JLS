package jls.elem;

/**
 * Marks part of the horizontal line below the signal names. Pure data
 * (issue #77): the horizontal line is drawn GUI-side by
 * {@code jls.edit.DisplayBool}.
 *
 * @author David A. Poplawski
 */
public final class HLine extends Entry {
	/**
	 * Construct a new HLine.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public HLine(TruthTable ttelem) {

		super(ttelem);
	} // end of constructor

} // end of HLine class
