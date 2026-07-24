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
import jls.elem.InputPin;
import jls.elem.Pin;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Pin} and its concrete subtypes
 * {@link InputPin} and {@link jls.elem.OutputPin} (issue #77). Moved from
 * the former {@code Pin.PinCreate} inner dialog plus {@code Pin.setup}, so
 * the model stays headless. The pin kind ("Input"/"Output") drives the
 * dialog title and is derived from the concrete element type; a single
 * dialog instance is registered for each pin class (pins have no change
 * dialog).
 */
public final class PinDialog implements ElementDialog {

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Pin pin = (Pin) el;
		String type = (el instanceof InputPin) ? "Input" : "Output";

		// show creation dialog
		CreateForm form = new CreateForm(pin, type);

		// don't do anything if user cancelled gate
		if (form.cancelled) {
			return false;
		}

		// finish up
		pin.init(g);

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				pin.getWidth(), pin.getHeight());
		pin.setXY(p.x, p.y);

		return true;
	} // end of setup method

	/**
	 * Dialog box to set pin name, bits, and orientation. Applies the
	 * entered values to the pin through its public setters, registering the
	 * name with the circuit exactly as the former inner dialog did.
	 */
	@SuppressWarnings("serial")
	private static final class CreateForm extends ElementFormDialog {

		private final Pin pin;
		private boolean cancelled;
		/** The text field for the pin name. */
		private final JTextField nameField = new JTextField("", 12);
		/** The text field for the bit width. */
		private final JTextField bitsField = new JTextField("1", 5);
		/** The key pad for entering the bit width. */
		private final KeyPad bitsPad = new KeyPad(bitsField, 10, 1, this);
		/** Selects left orientation. */
		private final JRadioButton left = new JRadioButton("Left");
		/** Selects right orientation (the default). */
		private final JRadioButton right = new JRadioButton("Right", true);
		/** Selects up orientation. */
		private final JRadioButton up = new JRadioButton("Up");
		/** Selects down orientation. */
		private final JRadioButton down = new JRadioButton("Down");

		/**
		 * Set up dialog window.
		 *
		 * @param pin The pin being created.
		 * @param type The type of pin ("Input" or "Output").
		 */
		private CreateForm(Pin pin, String type) {

			// set up window title
			super("Create " + type + " Pin", type);
			this.pin = pin;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(2, 1, 1, 5));
			JLabel name = new JLabel("Name: ", SwingConstants.RIGHT);
			labels.add(name);
			JLabel bits = new JLabel("Bits: ", SwingConstants.RIGHT);
			labels.add(bits);
			info.add(labels, BorderLayout.WEST);

			JPanel data = new JPanel(new GridLayout(2, 1, 1, 5));
			data.add(nameField);
			JPanel bitsPanel = new JPanel(new BorderLayout());
			bitsPanel.add(bitsField, BorderLayout.CENTER);
			bitsPanel.add(bitsPad, BorderLayout.EAST);
			data.add(bitsPanel);
			info.add(data, BorderLayout.CENTER);
			window.add(info);

			//Setup orientation radio buttons
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(3, 3));
			orients.add(new JLabel(""));
			orients.add(up);
			orients.add(new JLabel(""));
			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			orients.add(new JLabel(""));
			orients.add(down);
			orients.add(new JLabel(""));
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			up.setHorizontalAlignment(SwingConstants.CENTER);
			down.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			gr.add(down);
			gr.add(up);
			window.add(orients);

			confirmOnEnter(nameField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the pin.
		 */
		@Override
		protected void validateAndAccept() {

			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Bits not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			String tname = nameField.getText().trim();
			if (tname.length() < 1 || !Util.isValidName(tname)) {
				reject("Missing or invalid name, try again");
				return;
			}
			if (!pin.getCircuit().addName(tname)) {
				reject("Name already used, try again");
				return;
			}
			pin.setBits(bits);
			pin.setName(tname);
			if (left.isSelected()) {
				pin.setOrientation(Orientation.LEFT);
			}
			else if (right.isSelected()) {
				pin.setOrientation(Orientation.RIGHT);
			}
			else if (up.isSelected()) {
				pin.setOrientation(Orientation.UP);
			}
			else if (down.isSelected()) {
				pin.setOrientation(Orientation.DOWN);
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this pin.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of CreateForm class

} // end of PinDialog class
