package jls.elem;

/**
 * A truth-table signal-name header cell (#27 S4). Pure data (issue #77):
 * it holds the signal name and exposes the four column-command hooks; the
 * GUI ({@code jls.edit.DisplayBool}) draws the name and shows the
 * delete/rename/move menu that dispatches to these hooks. InputSig and
 * OutputSig differ only in which TruthTable methods those hooks call.
 *
 * @author David A. Poplawski
 */
public abstract class SigEntry extends Entry {

	// properties
	/** The name of this signal. */
	protected String signal;

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param signal The name of this signal.
	 */
	public SigEntry(TruthTable ttelem, String signal) {

		super(ttelem);
		this.signal = signal;
	} // end of constructor

	/**
	 * Get the name of this signal.
	 *
	 * @return the signal name.
	 */
	public String getSignal() {

		return signal;
	} // end of getSignal method

	/** Delete this signal's column. */
	public abstract void doRemove();

	/** Rename this signal. */
	public abstract void doRename();

	/** Move this signal's column left. */
	public abstract void doMoveLeft();

	/** Move this signal's column right. */
	public abstract void doMoveRight();

} // end of SigEntry class
