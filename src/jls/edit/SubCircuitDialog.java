package jls.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.Util;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.SubCircuit;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link SubCircuit} (issue #77). Moved from
 * the former {@code SubCircuit.SubCreate} inner dialog +
 * {@code SubCircuit.setup}, so the model stays headless. The form names
 * the subcircuit and applies the chosen orientation to the element through
 * its public setters, then sizes and positions it.
 */
public final class SubCircuitDialog implements ElementDialog {

	/**
	 * Creates a {@code SubCircuitDialog}.
	 */
	public SubCircuitDialog() {
	}

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		SubCircuit sc = (SubCircuit) el;

		// show creation dialog
		Form form = new Form(sc);

		// don't do anything if user cancelled
		if (form.cancelled) {
			return false;
		}

		// complete initialization and place the element
		sc.init(SwingTextMetrics.forGraphics(g));
		Point p = Placement.dropPoint(editWindow, x, y,
				sc.getWidth(), sc.getHeight());
		sc.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-subcircuit form. Extends the base element dialog and names
	 * the subcircuit and applies the entered orientation.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		/** The subcircuit being created. */
		private final SubCircuit sc;
		/** Whether the user cancelled the dialog. */
		private boolean cancelled;
		/** Entry field for the subcircuit name. */
		private final JTextField nameField = new JTextField("", 12);
		/** Radio button selecting left-facing orientation. */
		private final JRadioButton left = new JRadioButton("Left");
		/** Radio button selecting right-facing orientation. */
		private final JRadioButton right = new JRadioButton("Right", true);

		/**
		 * Builds and shows the create-subcircuit form.
		 *
		 * @param sc the subcircuit to configure
		 */
		private Form(SubCircuit sc) {

			super("Create Subcircuit", "import");
			this.sc = sc;
			cancelled = false;

			Container window = getContentPane();

			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel name = new JLabel("Name: ", SwingConstants.RIGHT);
			labelled(name, nameField, "dialog.subcircuit.name");
			info.add(name, BorderLayout.WEST);
			info.add(nameField, BorderLayout.CENTER);
			window.add(info);

			// Setup orientation radio buttons
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(3, 3));
			orients.add(new JLabel(""));
			orients.add(new JLabel(""));
			orients.add(new JLabel(""));
			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			orients.add(new JLabel(""));
			orients.add(new JLabel(""));
			orients.add(new JLabel(""));
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			window.add(orients);

			confirmOnEnter(nameField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			if (tname.length() < 1 || !Util.isValidName(tname)) {
				reject("Invalid name");
				return;
			}
			if (!sc.getCircuit().addName(tname)) {
				reject("Name already used");
				return;
			}
			if (left.isSelected()) {
				sc.setOrientation(Orientation.LEFT);
			} else if (right.isSelected()) {
				sc.setOrientation(Orientation.RIGHT);
			}
			sc.setName(tname);
			sc.getSubCircuit().setName(tname);
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of SubCircuitDialog class
