package jls.elem;

import java.awt.Graphics;

/**
 * An output signal name in a truth table; the shared drawing and menu
 * behavior lives in SigEntry (#27 S4).
 *
 * @author David A. Poplawski
 */
public final class OutputSig extends SigEntry {

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 * @param g A Graphics object to size the name with.
	 */
	public OutputSig(TruthTable ttelem, String signal, Graphics g) {

		super(ttelem,signal,g);
	} // end of constructor

	/**
	 * Remove this output signal from the truth table.
	 */
	@Override
	protected void doRemove() {

		ttelem.removeOutput(signal);
	} // end of doRemove method

	/**
	 * Rename this output signal in the truth table.
	 */
	@Override
	protected void doRename() {

		ttelem.renameOutput(signal);
	} // end of doRename method

	/**
	 * Move this output signal one position to the left in the truth table.
	 */
	@Override
	protected void doMoveLeft() {

		ttelem.moveOutputLeft(signal);
	} // end of doMoveLeft method

	/**
	 * Move this output signal one position to the right in the truth table.
	 */
	@Override
	protected void doMoveRight() {

		ttelem.moveOutputRight(signal);
	} // end of doMoveRight method

} // end of OutputSig class
