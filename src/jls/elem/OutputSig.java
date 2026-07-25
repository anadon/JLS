package jls.elem;

/**
 * An output signal name in a truth table; the shared data lives in
 * SigEntry (#27 S4).
 *
 * @author David A. Poplawski
 */
public final class OutputSig extends SigEntry {

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 */
	public OutputSig(TruthTable ttelem, String signal) {

		super(ttelem,signal);
	} // end of constructor

	/**
	 * Remove this output signal from the truth table.
	 */
	@Override
	public void doRemove() {

		ttelem.removeOutput(signal);
	} // end of doRemove method

	/**
	 * Rename this output signal in the truth table.
	 */
	@Override
	public void doRename() {

		ttelem.renameOutput(signal);
	} // end of doRename method

	/**
	 * Move this output signal one position to the left in the truth table.
	 */
	@Override
	public void doMoveLeft() {

		ttelem.moveOutputLeft(signal);
	} // end of doMoveLeft method

	/**
	 * Move this output signal one position to the right in the truth table.
	 */
	@Override
	public void doMoveRight() {

		ttelem.moveOutputRight(signal);
	} // end of doMoveRight method

} // end of OutputSig class
