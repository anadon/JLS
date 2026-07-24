package jls.edit;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jls.JLSInfo;
import jls.TellUser;
import jls.elem.Element;
import jls.elem.SigGen;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link SigGen} (issue #77). Moved
 * from the former {@code SigGen.EditSignals} inner dialog +
 * {@code SigGen.setup}/{@code SigGen.change}. One instance is registered
 * for creation (with the one-per-circuit guard) and one for change.
 */
public final class SigGenDialog implements ElementDialog {

	private static final int SIZE = 300; // width and height of dialog

	private final boolean creating;

	/**
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public SigGenDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		SigGen sg = (SigGen) el;

		if (creating) {
			// make sure there isn't a signal generator already
			for (Element e : sg.getCircuit().getElements()) {
				if (e instanceof SigGen) {
					TellUser.error(JLSInfo.frame,
							"Only one signal generator per circuit", "Error");
					return false;
				}
			}
			Form form = new Form(sg, true);
			if (form.cancelled) {
				return false;
			}
			sg.init(SwingTextMetrics.forGraphics(g));
			Point p = Placement.dropPoint(editWindow, x, y,
					sg.getWidth(), sg.getHeight());
			sg.setXY(p.x, p.y);
			return true;
		}

		// change
		Form form = new Form(sg, false);
		if (!form.cancelled) {
			sg.getCircuit().markChanged();
		}
		return false;
	} // end of setup method

	/**
	 * The signal-specification form.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		private final SigGen sg;
		private boolean cancelled;
		private final JTextArea textArea = new JTextArea();

		private Form(SigGen sg, boolean creating) {

			super("Create Signal Specification", "siggen");
			this.sg = sg;

			Container window = getContentPane();
			if (!creating) {
				textArea.setText(sg.getSignals());
			}
			JScrollPane pane = new JScrollPane(textArea);
			pane.setPreferredSize(new Dimension(SIZE, SIZE));
			window.add(pane);

			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			String newSignals = textArea.getText();
			if (newSignals.equals(sg.getSignals())) {
				cancelled = true;
			}
			sg.setSignals(newSignals);
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of SigGenDialog class
