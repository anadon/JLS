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
import jls.elem.Adder;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Adder} (issue #77). Moved from the
 * former {@code Adder.AdderCreate} inner dialog + {@code Adder.setup}, so
 * the model stays headless. The form applies the chosen bit width and
 * orientation to the adder through its public setters, then sizes and
 * positions it.
 */
public final class AdderDialog implements ElementDialog {

	/**
	 * Creates an {@code AdderDialog}.
	 */
	public AdderDialog() {
	}

	/** Default bit width offered when the create-adder form is opened. */
	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Adder adder = (Adder) el;

		// show creation dialog
		Form form = new Form(adder);

		// don't do anything if user cancelled
		if (form.cancelled) {
			return false;
		}

		// set propagation delay based on number of bits, complete
		// initialization, and place the element
		adder.resetPropDelay();
		adder.init(SwingTextMetrics.forGraphics(g));
		Point p = Placement.dropPoint(editWindow, x, y,
				adder.getWidth(), adder.getHeight());
		adder.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-adder form. Extends the base element dialog and applies
	 * the entered bits/orientation to the adder.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		/** The adder being configured by this form. */
		private final Adder adder;
		/** Whether the user cancelled the dialog. */
		private boolean cancelled;
		/** Entry field for the number of input bits. */
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 10);
		/** Numeric keypad wired to {@link #bitsField}. */
		private final KeyPad bitsPad =
				new KeyPad(bitsField, 10, DEFAULT_BITS, this);
		/** Selects leftward orientation. */
		private final JRadioButton left = new JRadioButton("Left");
		/** Selects rightward orientation. */
		private final JRadioButton right = new JRadioButton("Right", true);
		/** Selects upward orientation. */
		private final JRadioButton up = new JRadioButton("Up");
		/** Selects downward orientation. */
		private final JRadioButton down = new JRadioButton("Down");

		/**
		 * Creates the create-adder form for the given adder.
		 *
		 * @param adder the adder to configure
		 */
		private Form(Adder adder) {

			super("Create Adder", "adder");
			this.adder = adder;
			cancelled = false;

			Container window = getContentPane();

			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Input Bits: ", SwingConstants.RIGHT);
			info.add(bits, BorderLayout.WEST);
			info.add(bitsField, BorderLayout.CENTER);
			info.add(bitsPad, BorderLayout.EAST);
			window.add(info);

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

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			} catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must be at least 1 bit");
				return;
			}
			adder.setBits(bits);
			if (left.isSelected()) {
				adder.setOrientation(Orientation.LEFT);
			} else if (right.isSelected()) {
				adder.setOrientation(Orientation.RIGHT);
			} else if (up.isSelected()) {
				adder.setOrientation(Orientation.UP);
			} else if (down.isSelected()) {
				adder.setOrientation(Orientation.DOWN);
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of AdderDialog class
