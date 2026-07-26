package jls.edit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.jspecify.annotations.Nullable;

import jls.KeyPad;
import jls.TellUser;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Gate;
import jls.util.Placement;

/**
 * GUI-side creation dialog for the simple {@link Gate} leaves (AND, OR,
 * NAND, NOR, XOR, NOT, and Extend) (issue #77). Moved from the former
 * {@code Gate.GateCreate} inner dialog + {@code Gate.setup(...)} +
 * {@code Extend.setup}, so the gate models stay headless. The dialog reads
 * the gate's kind name to lay out the form and apply the chosen input
 * count, gate count, and orientation to the gate through its public
 * setters, then sizes and positions it. DELAY has its own dialog
 * ({@link DelayGateDialog}).
 */
public final class GateDialog implements ElementDialog {

	/**
	 * Creates a {@code GateDialog}.
	 */
	public GateDialog() {
	}

	/** Default number of inputs shown in the creation dialog. */
	private static final int DEFAULT_INPUTS = 2;
	/** Default number of gates (bits) shown in the creation dialog. */
	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Gate gate = (Gate) el;
		String type = gate.getKindName();

		// show creation dialog
		Form form = new Form(gate, type);

		// don't do anything if user canceled gate
		if (form.cancelled) {
			return false;
		}

		// get info
		if (type.equals("NOT") || type.equals("DELAY")
				|| type.equals("Extend")) {
			gate.setNumInputs(1);
		}
		gate.setDelay(gate.getDefaultDelay());

		// set up inputs and outputs
		gate.init(SwingTextMetrics.forGraphics(g));

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				gate.getWidth(), gate.getHeight());
		gate.setXY(p.x, p.y);

		// remember the accepted settings for the next gate of this kind
		gate.rememberPrevious();

		// Extend must have at least 2 output bits
		if ("Extend".equals(type) && gate.getBits() < 2) {
			TellUser.error(editWindow, "must have at least 2 bits of output",
					"Error");
			return false;
		}

		return true;
	} // end of setup method

	/**
	 * Dialog box to set multi-input gate parameters (number of inputs,
	 * number of gates). Used by all simple gates (nand, and, nor, or, xor)
	 * and Extend. Extends the base element dialog and applies the entered
	 * inputs/bits/orientation to the gate.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		/** The gate being created. */
		private final Gate gate;
		/** True if the creation dialog was cancelled. */
		private boolean cancelled;
		/** Button to repeat the previously created gate's settings. */
		private @Nullable JButton repeat;
		/** Text field for the number of inputs. */
		private final JTextField inputsField =
				new JTextField(DEFAULT_INPUTS + "", 5);
		/** Text field for the number of gates (bits). */
		private final JTextField gatesField =
				new JTextField(DEFAULT_BITS + "", 5);
		/** Keypad for the inputs field. */
		private final KeyPad inputsPad =
				new KeyPad(inputsField, 10, DEFAULT_INPUTS, this);
		/** Keypad for the gates field. */
		private final KeyPad gatesPad =
				new KeyPad(gatesField, 10, DEFAULT_BITS, this);
		/** Left orientation choice. */
		private final JRadioButton left = new JRadioButton("left");
		/** Up orientation choice. */
		private final JRadioButton up = new JRadioButton("up");
		/** Down orientation choice. */
		private final JRadioButton down = new JRadioButton("down");
		/** Right orientation choice. */
		private final JRadioButton right = new JRadioButton("right");
		/** The type of gate (e.g. "AND"). */
		private final String type;

		/**
		 * Set up dialog window.
		 *
		 * @param gate The gate being created.
		 * @param type The type of gate (e.g. "AND").
		 */
		private Form(Gate gate, String type) {

			// set up window title
			super("Create " + type + " Gate", type);
			this.gate = gate;

			// set not canceled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up input panel
			this.type = type;
			JPanel info = null;
			if (type.equals("NOT") || type.equals("XOR")
					|| type.equals("Extend")) {
				info = new JPanel(new GridLayout(1,2));
			}
			else {
				info = new JPanel(new GridLayout(2,2));
				JLabel inputs = new JLabel("Inputs: ",SwingConstants.RIGHT);
				info.add(inputs);
				JPanel in = new JPanel(new FlowLayout());
				in.add(inputsField);
				in.add(inputsPad);
				info.add(in);
			}
			JLabel gates;
			if (type.equals("Extend")) {
				gates = new JLabel("Outputs: ",SwingConstants.RIGHT);
				gatesField.setText("2");
			}
			else {
				gates = new JLabel("Gates (bits): ",SwingConstants.RIGHT);
			}

			info.add(gates);
			JPanel ga = new JPanel(new FlowLayout());
			ga.add(gatesField);
			ga.add(gatesPad);
			info.add(ga);
			window.add(info);

			// set up orientation panel
			window.add(new JLabel(" "));
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
			ButtonGroup group = new ButtonGroup();
			group.add(left);
			group.add(up);
			group.add(down);
			group.add(right);
			right.setSelected(true);
			window.add(orients);

			// set up repeat button
			if (!type.equals("Extend")) {
				window.add(new JLabel(" "));
				JPanel rep = new JPanel();
				repeat = new JButton("Repeat Previous " + type + " Gate");
				repeat.setBackground(Color.yellow);
				rep.add(repeat);
				window.add(rep);
				repeat.addActionListener(this);
			}

			confirmOnEnter(inputsField);
			confirmOnEnter(gatesField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the gate.
		 */
		@Override
		protected void validateAndAccept() {

			int numInputs;
			int bits;
			try {
				numInputs = Integer.parseInt(inputsField.getText());
				bits = Integer.parseInt(gatesField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric");
				return;
			}
			if (numInputs < 2) {
				reject("Must have at least 2 inputs");
				return;
			}
			if (bits < 1 && !"Extend".equals(type)) {
				reject("Must have at least 1 gate (bit)");
				return;
			}
			gate.setNumInputs(numInputs);
			gate.setBits(bits);
			if (left.isSelected()) {
				gate.setOrientation(Orientation.LEFT);
			}
			else if (right.isSelected()) {
				gate.setOrientation(Orientation.RIGHT);
			}
			else if (up.isSelected()) {
				gate.setOrientation(Orientation.UP);
			}
			else {
				gate.setOrientation(Orientation.DOWN);
			}
			inputsPad.close();
			gatesPad.close();
			dispose();
		} // end of validateAndAccept method

		/**
		 * React to the repeat previous button.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			// if repeat previous button, set previous values
			if (event.getSource() == repeat) {
				gate.setToPrevious();
				inputsField.setText(""+gate.getNumInputs());
				gatesField.setText(""+gate.getBits());
				left.setSelected(false);
				right.setSelected(false);
				up.setSelected(false);
				down.setSelected(false);
				Orientation orientation = gate.getOrientation();
				if (orientation == Orientation.LEFT)
					left.setSelected(true);
				else if (orientation == Orientation.RIGHT)
					right.setSelected(true);
				else if (orientation == Orientation.UP)
					up.setSelected(true);
				else
					down.setSelected(true);
			}
		} // end of actionPerformed method

		/**
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			inputsPad.close();
			gatesPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of GateDialog class
