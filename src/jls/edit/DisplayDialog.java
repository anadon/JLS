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
import jls.elem.Display;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Display} (issue #77). Moved from the
 * former {@code Display.DispCreate} inner dialog + {@code Display.setup},
 * so the model stays headless. The form applies the chosen bit width and
 * radix to the display through its public setters, then sizes and
 * positions it.
 */
public final class DisplayDialog implements ElementDialog {

	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Display disp = (Display) el;

		// show creation dialog
		Form form = new Form(disp);

		// don't do anything if user cancelled element
		if (form.cancelled) {
			return false;
		}

		// complete initialization and place the element
		disp.init(SwingTextMetrics.forGraphics(g));
		Point p = Placement.dropPoint(editWindow, x, y,
				disp.getWidth(), disp.getHeight());
		disp.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-display form. Extends the base element dialog and applies
	 * the entered bits/radix to the display.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		private final Display disp;
		private boolean cancelled;
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 5);
		private final KeyPad bitsPad =
				new KeyPad(bitsField, 10, DEFAULT_BITS, this);
		private final JRadioButton b2 = new JRadioButton("2");
		private final JRadioButton b10 = new JRadioButton("10");
		private final JRadioButton b16 = new JRadioButton("16");

		private Form(Display disp) {

			// set up window title
			super("Create Display", "display");
			this.disp = disp;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up bits panel
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Bits: ", SwingConstants.RIGHT);
			info.add(bits, BorderLayout.WEST);
			info.add(bitsField, BorderLayout.CENTER);
			info.add(bitsPad, BorderLayout.EAST);
			window.add(info);

			// set up radix panel
			window.add(new JLabel(" "));
			JLabel dlbl = new JLabel("Display Radix");
			dlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(dlbl);
			JPanel radix = new JPanel(new GridLayout(1, 3));
			radix.add(b2);
			radix.add(b10);
			radix.add(b16);
			ButtonGroup rgroup = new ButtonGroup();
			rgroup.add(b2);
			rgroup.add(b10);
			rgroup.add(b16);
			b10.setSelected(true);
			window.add(radix);

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
				reject("Must be at least one bit");
				return;
			}
			disp.setBits(bits);

			int base;
			if (b2.isSelected())
				base = 2;
			else if (b10.isSelected())
				base = 10;
			else
				base = 16;
			disp.setBase(base);
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			bitsPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of DisplayDialog class
