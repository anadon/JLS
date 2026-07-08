package jls.elem;

import java.awt.Graphics;

/**
 * An input signal name in a truth table; the shared drawing and menu
 * behavior lives in SigEntry (#27 S4).
 *
 * @author David A. Poplawski
 */
public final class InputSig extends SigEntry {

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 * @param g A Graphics object to size the name with.
	 */
	public InputSig(TruthTable ttelem, String signal, Graphics g) {

		super(ttelem,signal,g);
	} // end of constructor

	protected void doRemove() {

		ttelem.removeInput(signal);
	} // end of doRemove method

	protected void doRename() {

		ttelem.renameInput(signal);
	} // end of doRename method

	protected void doMoveLeft() {

		ttelem.moveInputLeft(signal);
	} // end of doMoveLeft method

	protected void doMoveRight() {

		ttelem.moveInputRight(signal);
	} // end of doMoveRight method

} // end of InputSig class
