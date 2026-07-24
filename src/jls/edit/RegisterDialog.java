package jls.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.Util;
import jls.core.Geometry;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Register;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link Register} (issue #77). Moved
 * from the former {@code Register.RegisterEdit} inner dialog +
 * {@code Register.setup}/{@code Register.change}. One instance is
 * registered for creation and one for change; the {@code creating} flag
 * selects which fields the form shows and whether a grown name forces a
 * reposition. Results are applied to the register through its public
 * setters so the model stays headless.
 */
public final class RegisterDialog implements ElementDialog {

	private final boolean creating;

	/**
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public RegisterDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Register reg = (Register) el;

		if (creating) {

			// show creation dialog
			Form form = new Form(reg, true, g);

			// don't do anything if user cancelled gate
			if (form.cancelled) {
				return false;
			}

			// complete initialization
			reg.init(g);

			// save position
			Point p = Placement.dropPoint(editWindow, x, y,
					reg.getWidth(), reg.getHeight());
			reg.setXY(p.x, p.y);
			return true;
		}

		// change: display dialog
		Form form = new Form(reg, false, g);

		// if element got bigger, detach and make user re-position
		if (form.nameChanged) {
			reg.resizeForNewName(g);
			return true;
		}
		return false;
	} // end of setup method

	/**
	 * The create/modify register form. Extends the base element dialog
	 * and applies the entered characteristics to the register.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		private final Register reg;
		/** True if creating a new register, false if modifying one. */
		private final boolean creating;
		/** Graphics saved for the valueFits check. */
		private final Graphics saveg;
		/** True if the user cancelled the dialog. */
		private boolean cancelled;
		/** True if a modify changed the name to one that no longer fits. */
		private boolean nameChanged;

		private final JTextField nameField;
		private final JTextField bitsField;
		private final JTextField valueField = new JTextField("");
		private final KeyPad bitsPad;
		private final KeyPad valuePad;
		private final JRadioButton base2 = new JRadioButton("2");
		private final JRadioButton base10 = new JRadioButton("10");
		private final JRadioButton base16 = new JRadioButton("16");
		private final JRadioButton latch = new JRadioButton("Level-Trig");
		private final JRadioButton posFF = new JRadioButton("Pos-Trig");
		private final JRadioButton negFF = new JRadioButton("Neg-Trig");
		private final JRadioButton left = new JRadioButton("Left");
		private final JRadioButton right = new JRadioButton("Right");
		private final JRadioButton up = new JRadioButton("Up");
		private final JRadioButton down = new JRadioButton("Down");

		private Form(Register reg, boolean creating, Graphics saveg) {

			// set up window title
			super("Create Register", "register");

			this.reg = reg;
			this.saveg = saveg;

			// set not cancelled
			cancelled = false;

			// save create/modify
			this.creating = creating;

			nameField = new JTextField(reg.getName());
			bitsField = new JTextField(reg.getBits() + "");
			bitsPad = new KeyPad(bitsField, 10, reg.getBits(), this);
			valuePad = new KeyPad(valueField, 16,
					reg.getInitialValue().longValue(), this);

			// set up window
			Container window = getContentPane();

			// set up types
			JLabel tlbl = new JLabel("Trigger");
			tlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(tlbl);
			JPanel types = new JPanel(new GridLayout(1,3));
			types.add(latch);
			latch.setToolTipText("Level-triggered");
			types.add(posFF);
			posFF.setToolTipText("{positive,leading,rising}-edge-triggered");
			types.add(negFF);
			negFF.setToolTipText("{negative,trailing,falling}-edge-triggered");
			switch (reg.getTypeName()) {
			case "latch":
				latch.setSelected(true);
				break;
			case "pff":
				posFF.setSelected(true);
				break;
			case "nff":
				negFF.setSelected(true);
				break;
			}
			latch.setHorizontalAlignment(SwingConstants.CENTER);
			posFF.setHorizontalAlignment(SwingConstants.CENTER);
			negFF.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup tgroup = new ButtonGroup();
			tgroup.add(latch);
			tgroup.add(posFF);
			tgroup.add(negFF);
			window.add(types);

			// set up inputs
			int rows = 2;
			if (creating) {
				rows = 3;
			}
			window.add(new JLabel(" "));
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(rows,1,1,5));
			JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
			labels.add(name);
			if (creating) {
				JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
				labels.add(bits);
			}
			JLabel init = new JLabel("Initial value: ",SwingConstants.RIGHT);
			labels.add(init);
			info.add(labels,BorderLayout.WEST);

			JPanel fields = new JPanel(new GridLayout(rows,1,1,5));
			fields.add(nameField);
			if (creating) {
				JPanel b = new JPanel(new BorderLayout());
				b.add(bitsField,BorderLayout.CENTER);
				b.add(bitsPad,BorderLayout.EAST);
				fields.add(b);
			}
			JPanel i = new JPanel(new BorderLayout());
			valueField.setText(Util.convert(reg.getInitialValue(),reg.getBase(),false));
			i.add(valueField,BorderLayout.CENTER);
			i.add(valuePad,BorderLayout.EAST);
			valuePad.setBase(reg.getBase());
			fields.add(i);
			info.add(fields,BorderLayout.CENTER);
			window.add(info);

			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1,3));
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
			switch (reg.getBase()) {
			case 2:
				base2.setSelected(true);
				break;
			case 10:
				base10.setSelected(true);
				break;
			case 16:
				base16.setSelected(true);
				break;
			}
			window.add(bases);

			//Setup orientation radio buttons
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(3,3));
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
			switch(reg.getOrientation()) {
			case LEFT:
				left.setSelected(true);
				break;
			case RIGHT:
				right.setSelected(true);
				break;
			case UP:
				up.setSelected(true);
				break;
			case DOWN:
				down.setSelected(true);
				break;
			}
			window.add(orients);

			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);

			confirmOnEnter(nameField);
			confirmOnEnter(bitsField);
			confirmOnEnter(valueField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and apply it to the register.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			if (tname.isEmpty() || !Util.isValidName(tname)) {
				reject("Missing or invalid name");
				return;
			}
			int tbits = reg.getBits();
			BigInteger tinitialValue = BigInteger.ZERO;
			if(left.isSelected())
			{
				reg.setOrientation(Orientation.LEFT);
			}
			else if(right.isSelected())
			{
				reg.setOrientation(Orientation.RIGHT);
			}
			else if(up.isSelected())
			{
				reg.setOrientation(Orientation.UP);
			}
			else if(down.isSelected())
			{
				reg.setOrientation(Orientation.DOWN);
			}
			try {
				if (creating) {
					tbits = Integer.parseInt(bitsField.getText());
				}
				tinitialValue = new BigInteger(valueField.getText(),reg.getBase());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric");
				return;
			}
			if (tbits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			if (tinitialValue.bitLength() > tbits) {
				reject("Value too large for number of bits");
				return;
			}
			if (!creating) {
				reg.getCircuit().removeName(reg.getName());
			}
			if (!reg.getCircuit().addName(tname)) {
				reject("Duplicate name");
				return;
			}
			reg.setName(tname);
			reg.setBits(tbits);
			reg.applyInitialValue(tinitialValue);
			if (latch.isSelected())
				reg.setTypeName("latch");
			else if (negFF.isSelected())
				reg.setTypeName("nff");
			else
				reg.setTypeName("pff");
			bitsPad.close();
			valuePad.close();
			reg.getCircuit().markChanged();
			if (!creating && !valueFits(tname)) {
				nameChanged = true;
			}
			else {
				nameChanged = false;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * See if the given value fits in the box on the screen.
		 *
		 * @param value The value to check.
		 * @return true if it fits, false if not.
		 */
		private boolean valueFits(String value) {

			int s = Geometry.SPACING;
			FontMetrics fm = saveg.getFontMetrics();
			int w = fm.stringWidth(value)+s;
			int rw = Math.max((w+s/2)/s*s,2*s);
			return rw <= (reg.getWidth()-s);
		} // end of valueFits method

		/**
		 * React to radix buttons.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),reg.getBase());
				reg.setBase(2);
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			}
			else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),reg.getBase());
				reg.setBase(10);
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			}
			else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),reg.getBase());
				reg.setBase(16);
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method

		/**
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of RegisterDialog class
