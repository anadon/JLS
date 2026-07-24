package jls.elem;

/**
 * Marks part of the vertical line separating the inputs from the outputs
 * of the truth table. Pure data (issue #77): the vertical line is drawn
 * GUI-side by {@code jls.edit.DisplayBool}.
 *
 * @author David A. Poplawski
 */
public final class VLine extends Entry {

	/**
	 * Construct a new VLine.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public VLine(TruthTable ttelem) {

		super(ttelem);
	} // end of constructor

} // end of VLine class
