package jls.edit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.Util;
import jls.core.Orientation;
import jls.elem.Constant;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link Constant} (issue #77). Moved
 * from the former {@code Constant.ConstantCreate}/{@code ConstantChange}
 * inner dialogs plus {@code Constant.setup}/{@code Constant.change}, so the
 * model stays headless. One instance is registered for creation (value,
 * radix, orientation, repeat-previous) and one for change (value/radix with
 * a fit check that resizes the element when the new value no longer fits).
 */
public final class ConstantDialog implements ElementDialog {

	/** Default constant value, mirroring the element's own default. */
	private static final BigInteger DEFAULT_VALUE = BigInteger.ZERO;
	/** Default display radix, mirroring the element's own default. */
	private static final int DEFAULT_BASE = 10;

	private final boolean creating;

	/**
	 * @param creating True for the create dialog, false for the edit
	 *        (change) dialog.
	 */
	public ConstantDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Constant c = (Constant) el;

		if (creating) {
			// show creation dialog
			CreateForm form = new CreateForm(c);
			if (form.cancelled) {
				return false;
			}
			// save values for next create
			c.saveAsPrevious();
			// complete initialization
			c.init(g);
			// save position
			Point p = Placement.dropPoint(editWindow, x, y,
					c.getWidth(), c.getHeight());
			c.setXY(p.x, p.y);
			return true;
		}

		// change
		ChangeForm form = new ChangeForm(c, g);
		if (form.cancelled) {
			return false;
		}
		// record a change to the circuit
		c.getCircuit().markChanged();
		// if bigger, detach and resize
		if (form.changed) {
			c.resizeToFit(g);
			return true;
		}
		// no need to reposition
		return false;
	} // end of setup method

	/**
	 * The create-constant form: value, radix, orientation, and a
	 * repeat-previous button. Applies the entered value/radix/orientation
	 * to the constant through its public setters.
	 */
	@SuppressWarnings("serial")
	private static final class CreateForm extends jls.elem.ElementDialog
			implements ActionListener {

		private final Constant c;
		private boolean cancelled;
		private JButton repeat;
		private final JTextField valueField =
				new JTextField(DEFAULT_VALUE + "", DEFAULT_BASE);
		private final KeyPad valuePad =
				new KeyPad(valueField, 16, DEFAULT_VALUE.longValue(), this);
		private final JRadioButton base2 = new JRadioButton("2");
		private final JRadioButton base10 = new JRadioButton("10");
		private final JRadioButton base16 = new JRadioButton("16");
		private final JRadioButton left = new JRadioButton("Left");
		private final JRadioButton right = new JRadioButton("Right", true);
		private final JRadioButton up = new JRadioButton("Up");
		private final JRadioButton down = new JRadioButton("Down");

		private CreateForm(Constant c) {

			super("Create Constant", "const");
			this.c = c;
			cancelled = false;

			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel inputs = new JLabel("Value: ", SwingConstants.RIGHT);
			info.add(inputs, BorderLayout.WEST);
			info.add(valueField, BorderLayout.CENTER);
			info.add(valuePad, BorderLayout.EAST);
			valuePad.setBase(10);
			window.add(info);

			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1, 3));
			bases.add(base2);
			bases.add(base10);
			bases.add(base16);
			base2.setHorizontalAlignment(SwingConstants.CENTER);
			base10.setHorizontalAlignment(SwingConstants.CENTER);
			base16.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(base2);
			group.add(base10);
			group.add(base16);
			base10.setSelected(true);
			window.add(bases);

			// set up repeat
			window.add(new JLabel(" "));
			JPanel rep = new JPanel();
			repeat = new JButton("Repeat Previous Value");
			repeat.setBackground(Color.yellow);
			rep.add(repeat);
			window.add(rep);

			// Setup orientation radio buttons
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

			repeat.addActionListener(this);
			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);

			confirmOnEnter(valueField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			try {
				c.setValue(new BigInteger(valueField.getText(), c.getBase()));
				if (left.isSelected()) {
					c.setOrientation(Orientation.LEFT);
				} else if (right.isSelected()) {
					c.setOrientation(Orientation.RIGHT);
				} else if (up.isSelected()) {
					c.setOrientation(Orientation.UP);
				} else if (down.isSelected()) {
					c.setOrientation(Orientation.DOWN);
				}
			} catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == repeat) {
				c.setToPrevious();
				base2.setSelected(false);
				base10.setSelected(false);
				base16.setSelected(false);
				if (c.getBase() == 2) {
					base2.setSelected(true);
				} else if (c.getBase() == 10) {
					base10.setSelected(true);
				} else {
					base16.setSelected(true);
				}
				valueField.setText(c.getValue().toString(c.getBase()));
				valuePad.setBase(c.getBase());
				valuePad.reset();
				System.out.println(c.getOrientation());
				if (c.getOrientation() == Orientation.LEFT) {
					left.setSelected(true);
				} else if (c.getOrientation() == Orientation.RIGHT) {
					right.setSelected(true);
				} else if (c.getOrientation() == Orientation.UP) {
					up.setSelected(true);
				} else {
					down.setSelected(true);
				}
			} else if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(2);
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			} else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(10);
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			} else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(16);
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of CreateForm class

	/**
	 * The change-constant form: value and radix, with a fit check. Applies
	 * the new value/radix to the constant and records whether it no longer
	 * fits and must be resized.
	 */
	@SuppressWarnings("serial")
	private static final class ChangeForm extends jls.elem.ElementDialog
			implements ActionListener {

		private final Constant c;
		private final Graphics g;
		private boolean cancelled;
		private boolean changed;
		private final JTextField valueField;
		private final KeyPad valuePad;
		private final JRadioButton base2 = new JRadioButton("2");
		private final JRadioButton base10 = new JRadioButton("10");
		private final JRadioButton base16 = new JRadioButton("16");

		private ChangeForm(Constant c, Graphics g) {

			super("Change Constant", "const");
			this.c = c;
			this.g = g;
			valueField = new JTextField(
					Util.convert(c.getValue(), c.getBase(), false), 10);
			valuePad = new KeyPad(valueField, 16, DEFAULT_VALUE.longValue(),
					this);

			Container window = getContentPane();

			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel inputs = new JLabel("Value: ", SwingConstants.RIGHT);
			info.add(inputs, BorderLayout.WEST);
			info.add(valueField, BorderLayout.CENTER);
			valueField.setText(c.getValue().toString(c.getBase()));
			valuePad.setBase(c.getBase());
			info.add(valuePad, BorderLayout.EAST);
			window.add(info);

			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1, 3));
			bases.add(base2);
			bases.add(base10);
			bases.add(base16);
			if (c.getBase() == 2) {
				base2.setSelected(true);
			} else if (c.getBase() == 10) {
				base10.setSelected(true);
			} else if (c.getBase() == 16) {
				base16.setSelected(true);
			}
			valuePad.setBase(c.getBase());
			ButtonGroup group = new ButtonGroup();
			group.add(base2);
			group.add(base10);
			group.add(base16);
			window.add(bases);

			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);

			confirmOnEnter(valueField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			// get base
			if (base2.isSelected()) {
				c.setBase(2);
			} else if (base10.isSelected()) {
				c.setBase(10);
			} else {
				c.setBase(16);
			}

			// get value
			BigInteger temp = BigInteger.ZERO;
			try {
				temp = new BigInteger(valueField.getText(), c.getBase());
			} catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}

			// cancel if the new value is the same as the old value
			// (value comparison: == on BigInteger never fired, #51)
			if (temp.equals(c.getValue())) {
				cancelDialog();
				return;
			}
			c.setValue(temp);

			// decide if the element must be redrawn
			if (!c.valueFits(g, Util.convert(temp, c.getBase(), true))) {
				changed = true;
			} else {
				changed = false;
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(2);
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			} else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(10);
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			} else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty()) {
					val = new BigInteger(valueField.getText(), c.getBase());
				}
				c.setBase(16);
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of ChangeForm class

} // end of ConstantDialog class
