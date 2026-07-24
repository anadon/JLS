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
import jls.core.Orientation;
import jls.elem.Decoder;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Decoder} (issue #77). Moved from the
 * former {@code Decoder.DecoderCreate} inner dialog + {@code Decoder.setup},
 * so the model stays headless. The form applies the chosen bit width and
 * orientation to the decoder through its public setters, then sizes and
 * positions it.
 */
public final class DecoderDialog implements ElementDialog {

	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Decoder decoder = (Decoder) el;

		// show creation dialog
		Form form = new Form(decoder);

		// don't do anything if user cancelled gate
		if (form.cancelled) {
			return false;
		}

		// complete initialization
		decoder.init(g);

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				decoder.getWidth(), decoder.getHeight());
		decoder.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-decoder form. Extends the base element dialog and applies
	 * the entered bits/orientation to the decoder.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends jls.elem.ElementDialog {

		private final Decoder decoder;
		private boolean cancelled;
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 10);
		private final KeyPad bitsPad =
				new KeyPad(bitsField, 10, DEFAULT_BITS, this);
		private final JRadioButton left = new JRadioButton("Left", true);
		private final JRadioButton right = new JRadioButton("Right");
		private final JRadioButton up = new JRadioButton("Up");
		private final JRadioButton down = new JRadioButton("Down");

		private Form(Decoder decoder) {

			// set up window title
			super("Create Decoder", "decoder");
			this.decoder = decoder;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Input Bits: ", SwingConstants.RIGHT);
			info.add(bits, BorderLayout.WEST);
			info.add(bitsField, BorderLayout.CENTER);
			info.add(bitsPad, BorderLayout.EAST);
			JPanel orient = new JPanel(new GridLayout(3, 3));
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			gr.add(down);
			gr.add(up);
			orient.add(new JLabel(""));
			orient.add(up);
			orient.add(new JLabel(""));
			orient.add(left);
			orient.add(new JLabel(""));
			orient.add(right);
			orient.add(new JLabel(""));
			orient.add(down);
			orient.add(new JLabel(""));
			window.add(info);
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must be at least 1 bit");
				return;
			}
			if (bits >= 32) {
				reject("Must be less than 32 bits");
				return;
			}
			decoder.setBits(bits);
			if (left.isSelected()) {
				decoder.setOrientation(Orientation.LEFT);
			}
			else if (right.isSelected()) {
				decoder.setOrientation(Orientation.RIGHT);
			}
			else if (up.isSelected()) {
				decoder.setOrientation(Orientation.UP);
			}
			else if (down.isSelected()) {
				decoder.setOrientation(Orientation.DOWN);
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of DecoderDialog class
