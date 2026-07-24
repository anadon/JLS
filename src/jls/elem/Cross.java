package jls.elem;

/**
 * Marks the cross where the horizontal and vertical lines of the truth
 * table meet. Pure data (issue #77): the cross is drawn GUI-side by
 * {@code jls.edit.DisplayBool}.
 *
 * @author David A. Poplawski
 */
public final class Cross extends Entry {

	/**
	 * Construct a new Cross.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public Cross(TruthTable ttelem) {

		super(ttelem);
	} // end of constructor

} // end of Cross class
