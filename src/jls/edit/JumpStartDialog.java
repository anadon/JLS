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

import jls.KeyPad;
import jls.Util;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.JumpStart;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link JumpStart} (issue #77). Moved from
 * the former {@code JumpStart.StartCreate} inner dialog + {@code
 * JumpStart.setup}, so the model stays headless. The form applies the
 * chosen wire name, bit width, and orientation to the jump start through
 * its public setters, registering the name with the circuit, then sizes
 * and positions it.
 */
public final class JumpStartDialog implements ElementDialog {

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		JumpStart start = (JumpStart) el;

		// show creation dialog
		Form form = new Form(start);

		// don't do anything if user cancelled
		if (form.cancelled) {
			return false;
		}

		// complete initialization
		start.init(g);

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				start.getWidth(), start.getHeight());
		start.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-wire-start form. Extends the base element dialog and
	 * applies the entered name/bits/orientation to the jump start.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		private final JumpStart start;
		private boolean cancelled;
		/** The text field for the wire name. */
		private final JTextField nameField = new JTextField("",12);
		/** The text field for the bit width. */
		private final JTextField bitsField = new JTextField("1",5);
		/** The key pad for entering the bit width. */
		private final KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		/** Selects left orientation. */
		private final JRadioButton left = new JRadioButton("left");
		/** Selects right orientation. */
		private final JRadioButton right = new JRadioButton("right");

		private Form(JumpStart start) {

			// set up window title
			super("Create Wire Start","start");
			this.start = start;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(2,1,1,5));
			JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
			labels.add(name);
			JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
			labels.add(bits);
			info.add(labels,BorderLayout.WEST);

			JPanel data = new JPanel(new GridLayout(2,1,1,5));
			data.add(nameField);
			JPanel bitsPanel = new JPanel(new BorderLayout());
			bitsPanel.add(bitsField,BorderLayout.CENTER);
			bitsPanel.add(bitsPad,BorderLayout.EAST);
			data.add(bitsPanel);
			info.add(data,BorderLayout.CENTER);
			window.add(info);

			// set up orientation panel
			window.add(new JLabel(" "));
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(1,3));

			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(left);
			group.add(right);
			left.setSelected(true);
			window.add(orients);

			confirmOnEnter(nameField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText();
			if (!Util.isValidName(tname)) {
				reject("Invalid name");
				return;
			}
			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Bits not numeric");
				return;
			}
			if (bits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			if (!start.getCircuit().addName(tname)) {
				reject("Name already used");
				return;
			}
			if (right.isSelected()) {
				start.setOrientation(Orientation.RIGHT);
			}
			else {
				start.setOrientation(Orientation.LEFT);
			}
			start.getCircuit().addJumpStart(tname,start);
			start.setName(tname);
			start.setBits(bits);
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of JumpStartDialog class
