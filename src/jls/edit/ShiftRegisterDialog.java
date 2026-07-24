package jls.edit;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.ShiftRegister;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link ShiftRegister} (issue #77). Moved
 * from the former {@code ShiftRegister.ShiftRegisterCreate} inner dialog +
 * {@code ShiftRegister.setup}, so the model stays headless. The form
 * applies the validated bit width, shift kind and orientations to the
 * shift register through its public setters, then sizes and positions it.
 */
public final class ShiftRegisterDialog implements ElementDialog {

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		ShiftRegister sr = (ShiftRegister) el;

		// show creation dialog
		Form form = new Form(sr);

		// don't do anything if user cancelled the element
		if (form.cancelled) {
			return false;
		}

		// complete initialization and place the element
		sr.init(SwingTextMetrics.of(g));
		Point p = Placement.dropPoint(editWindow, x, y,
				sr.getWidth(), sr.getHeight());
		sr.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create shift-register form. Extends the base element dialog and
	 * applies the entered values to the shift register.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		private final ShiftRegister sr;
		private boolean cancelled;

		private final JTextField bitsField =
				new JTextField(ShiftRegister.defaultBits() + "", 5);
		private final KeyPad bitsPad;
		private final JRadioButton shiftLeft =
				new JRadioButton("Shift Left", true);
		private final JRadioButton shiftRight = new JRadioButton("Shift Right");
		private final JRadioButton shiftRightArith =
				new JRadioButton("Shift Right Arithmetic");
		private final JRadioButton oLeft = new JRadioButton("Left");
		private final JRadioButton oRight = new JRadioButton("Right", true);
		private final JRadioButton oUp = new JRadioButton("Up");
		private final JRadioButton oDown = new JRadioButton("Down");
		private final JRadioButton sLeft = new JRadioButton("Left");
		private final JRadioButton sRight = new JRadioButton("Right");
		private final JRadioButton sUp = new JRadioButton("Up");
		private final JRadioButton sDown = new JRadioButton("Down", true);
		private final JLabel olbl2 = new JLabel("Amount Orientation");

		private Form(ShiftRegister sr) {

			super("Create Shift Register", "shiftregister");
			this.sr = sr;
			cancelled = false;
			bitsPad = new KeyPad(bitsField, 10, ShiftRegister.defaultBits(),
					this);

			// set up window
			Container window = getContentPane();

			// set up input panel
			JPanel info = new JPanel(new GridLayout(1, 2));
			JLabel gates = new JLabel("Bits: ", SwingConstants.RIGHT);
			info.add(gates);
			JPanel ga = new JPanel(new FlowLayout());
			ga.add(bitsField);
			ga.add(bitsPad);
			info.add(ga);
			window.add(info);

			// set up shift kind
			JLabel tlbl = new JLabel("Shift Type");
			tlbl.setAlignmentX(CENTER_ALIGNMENT);
			window.add(tlbl);
			JPanel types = new JPanel(new GridLayout(3, 1));
			ButtonGroup tgroup = new ButtonGroup();
			tgroup.add(shiftLeft);
			tgroup.add(shiftRight);
			tgroup.add(shiftRightArith);
			types.add(shiftLeft);
			types.add(shiftRight);
			types.add(shiftRightArith);
			JPanel containing = new JPanel();
			containing.add(types);
			containing.setAlignmentX(CENTER_ALIGNMENT);
			window.add(containing);

			// set up orientation radio buttons
			JPanel orient = new JPanel(new GridLayout(3, 3));
			JPanel orient2 = new JPanel(new GridLayout(3, 3));
			ButtonGroup gr = new ButtonGroup();
			ButtonGroup gr2 = new ButtonGroup();
			gr.add(oLeft);
			gr.add(oRight);
			gr.add(oDown);
			gr.add(oUp);
			gr2.add(sDown);
			gr2.add(sUp);
			gr2.add(sLeft);
			gr2.add(sRight);
			orient.add(new JLabel(""));
			orient.add(oUp);
			orient.add(new JLabel(""));
			orient.add(oLeft);
			orient.add(new JLabel(""));
			orient.add(oRight);
			orient.add(new JLabel(""));
			orient.add(oDown);
			orient.add(new JLabel(""));

			orient2.add(new JLabel(""));
			orient2.add(sUp);
			orient2.add(new JLabel(""));
			orient2.add(sLeft);
			orient2.add(new JLabel(""));
			orient2.add(sRight);
			orient2.add(new JLabel(""));
			orient2.add(sDown);
			orient2.add(new JLabel(""));

			JLabel olbl = new JLabel("Output Orientation");
			olbl.setAlignmentX(CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			olbl2.setAlignmentX(CENTER_ALIGNMENT);
			window.add(olbl2);
			window.add(orient2);

			sLeft.setVisible(false);
			sRight.setVisible(false);

			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == oLeft || event.getSource() == oRight) {
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
			}
			else if (event.getSource() == oUp || event.getSource() == oDown) {
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
			}
		} // end of actionPerformed method

		@Override
		protected java.util.List<ElementFormDialog.Violation> validateInputs() {

			int newBits;
			try {
				newBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new ElementFormDialog.Violation(
						"Value not numeric, try again", bitsField));
			}
			String violated = ShiftRegister.checkBits(newBits);
			if (violated != null) {
				return java.util.List.of(
						new ElementFormDialog.Violation(violated, bitsField));
			}
			return java.util.List.of();
		} // end of validateInputs method

		@Override
		protected void validateAndAccept() {

			int newBits;
			try {
				newBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again", bitsField);
				return;
			}
			sr.setDataBits(newBits);
			if (shiftRight.isSelected()) {
				sr.setShiftLogicalRight();
			}
			else if (shiftRightArith.isSelected()) {
				sr.setShiftArithmeticRight();
			}
			else {
				sr.setShiftLogicalLeft();
			}
			if (oLeft.isSelected()) {
				sr.setOutputOrientation(Orientation.LEFT);
				if (sUp.isSelected()) {
					sr.setAmountOrientation(Orientation.UP);
				}
				else if (sDown.isSelected()) {
					sr.setAmountOrientation(Orientation.DOWN);
				}
			}
			else if (oRight.isSelected()) {
				sr.setOutputOrientation(Orientation.RIGHT);
				if (sUp.isSelected()) {
					sr.setAmountOrientation(Orientation.UP);
				}
				else if (sDown.isSelected()) {
					sr.setAmountOrientation(Orientation.DOWN);
				}
			}
			else if (oDown.isSelected()) {
				sr.setOutputOrientation(Orientation.DOWN);
				if (sLeft.isSelected()) {
					sr.setAmountOrientation(Orientation.LEFT);
				}
				else if (sRight.isSelected()) {
					sr.setAmountOrientation(Orientation.RIGHT);
				}
			}
			else if (oUp.isSelected()) {
				sr.setOutputOrientation(Orientation.UP);
				if (sLeft.isSelected()) {
					sr.setAmountOrientation(Orientation.LEFT);
				}
				else if (sRight.isSelected()) {
					sr.setAmountOrientation(Orientation.RIGHT);
				}
			}
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

} // end of ShiftRegisterDialog class
