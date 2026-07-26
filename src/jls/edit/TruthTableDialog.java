package jls.edit;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;

import jls.elem.Element;
import jls.elem.TruthTable;
import jls.util.Placement;

/**
 * GUI-side create/edit dispatcher for {@link TruthTable} (issue #77).
 * Moved from the former {@code TruthTable.setup} and {@code TruthTable.change};
 * a single class serves both, selected by the {@code creating} flag, and
 * registered twice (as creation and change dialog). The dialog window
 * itself is {@link TruthTableEditor}; this class runs it and applies the
 * outcome to the element through its public model methods.
 */
public final class TruthTableDialog implements ElementDialog {

	/** True for the creation dialog, false for the change (edit) dialog. */
	private final boolean creating;

	/**
	 * Create the dispatcher.
	 *
	 * @param creating True for creation, false for change.
	 */
	public TruthTableDialog(boolean creating) {

		this.creating = creating;
	} // end of constructor

	/**
	 * Present the truth table editor and apply the result.
	 *
	 * @param el The truth table being created or edited.
	 * @param g The graphics context (for sizing).
	 * @param editWindow The editor panel to parent the dialog to.
	 * @param x The drop/mouse x-coordinate.
	 * @param y The drop/mouse y-coordinate.
	 *
	 * @return for creation, true unless cancelled; for change, true if the
	 *         element must be re-placed in the circuit.
	 */
	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		TruthTable tt = (TruthTable) el;

		if (creating) {

			// show creation dialog
			new TruthTableEditor(tt);

			// don't do anything if user cancelled
			if (tt.wasCancelled())
				return false;

			// complete initialization
			tt.init(SwingTextMetrics.forGraphics(g));

			// save position
			Point p = Placement.dropPoint(editWindow,x,y,
					tt.getWidth(),tt.getHeight());
			tt.setXY(p.x,p.y);

			return true;
		}

		// change (edit) path: snapshot, show dialog, reconcile
		tt.beginChange();
		new TruthTableEditor(tt);

		// quit if cancelled
		if (tt.wasCancelled()) {
			return false;
		}

		return tt.finishChange(SwingTextMetrics.of(g));
	} // end of setup method

} // end of TruthTableDialog class
