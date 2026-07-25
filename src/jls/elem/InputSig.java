package jls.elem;

/**
 * An input signal name in a truth table; the shared data lives in
 * SigEntry (#27 S4).
 *
 * @author David A. Poplawski
 */
public final class InputSig extends SigEntry {

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 */
	public InputSig(TruthTable ttelem, String signal) {

		super(ttelem,signal);
	} // end of constructor

	/**
	 * Remove this input signal from the truth table.
	 */
	@Override
	public void doRemove() {

		ttelem.removeInput(signal);
	} // end of doRemove method

	/**
	 * Rename this input signal in the truth table.
	 */
	@Override
	public void doRename() {

		ttelem.renameInput(signal);
	} // end of doRename method

	/**
	 * Move this input signal one position to the left in the truth table.
	 */
	@Override
	public void doMoveLeft() {

		ttelem.moveInputLeft(signal);
	} // end of doMoveLeft method

	/**
	 * Move this input signal one position to the right in the truth table.
	 */
	@Override
	public void doMoveRight() {

		ttelem.moveInputRight(signal);
	} // end of doMoveRight method

} // end of InputSig class
