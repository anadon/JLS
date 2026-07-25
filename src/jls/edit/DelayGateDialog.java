package jls.edit;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
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
import jls.elem.DelayGate;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link DelayGate} (issue #77). Moved from the
 * former {@code DelayGate.DelayCreate} inner dialog + {@code DelayGate.setup},
 * so the model stays headless. The form applies the chosen propagation delay,
 * gate count, and orientation to the delay gate through its public setters,
 * then sizes and positions it.
 */
public final class DelayGateDialog implements ElementDialog {

	/**
	 * Creates a {@code DelayGateDialog}.
	 */
	public DelayGateDialog() {
	}

	/** Default propagation delay shown in the creation dialog. */
	private static final int DEFAULT_DELAY = 1;
	/** Default number of gates (bits) shown in the creation dialog. */
	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		DelayGate delay = (DelayGate) el;

		// show creation dialog
		Form form = new Form(delay);

		// don't do anything if user canceled gate
		if (form.cancelled) {
			return false;
		}

		// set up inputs and outputs
		delay.setNumInputs(1);
		delay.init(SwingTextMetrics.forGraphics(g));

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				delay.getWidth(), delay.getHeight());
		delay.setXY(p.x, p.y);

		return true;
	} // end of setup method

	/**
	 * The create-delay-gate form. Extends the base element dialog and applies
	 * the entered delay/bits/orientation to the delay gate.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		/** The delay gate being created. */
		private final DelayGate delay;
		/** True if the user cancelled this dialog. */
		private boolean cancelled;
		/** Entry field for the propagation delay. */
		private final JTextField delayField =
				new JTextField(DEFAULT_DELAY + "", 5);
		/** Entry field for the number of gates (bits). */
		private final JTextField gatesField =
				new JTextField(DEFAULT_BITS + "", 5);
		/** Keypad for entering the propagation delay. */
		private final KeyPad delayPad =
				new KeyPad(delayField, 10, DEFAULT_DELAY, this);
		/** Keypad for entering the number of gates (bits). */
		private final KeyPad gatesPad =
				new KeyPad(gatesField, 10, DEFAULT_BITS, this);
		/** Selects the left orientation. */
		private final JRadioButton left = new JRadioButton("left");
		/** Selects the up orientation. */
		private final JRadioButton up = new JRadioButton("up");
		/** Selects the down orientation. */
		private final JRadioButton down = new JRadioButton("down");
		/** Selects the right orientation. */
		private final JRadioButton right = new JRadioButton("right");

		/**
		 * Set up create dialog window.
		 *
		 * @param delay The delay gate being created.
		 */
		private Form(DelayGate delay) {

			// set up window title
			super("Create DELAY Gate", "DELAY");
			this.delay = delay;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up input panel
			JPanel info = new JPanel(new GridLayout(2, 2));
			JLabel inputs = new JLabel("Propagation Delay: ",
					SwingConstants.RIGHT);
			info.add(inputs);
			JPanel in = new JPanel(new FlowLayout());
			in.add(delayField);
			in.add(delayPad);
			info.add(in);
			JLabel gates = new JLabel("Gates (bits): ", SwingConstants.RIGHT);
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
			ButtonGroup group = new ButtonGroup();
			group.add(left);
			group.add(right);
			group.add(up);
			group.add(down);
			right.setSelected(true);
			window.add(orients);

			confirmOnEnter(delayField);
			confirmOnEnter(gatesField);
			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			int propDelay;
			int bits;
			try {
				propDelay = Integer.parseInt(delayField.getText());
				bits = Integer.parseInt(gatesField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must have at least 1 gate");
				return;
			}
			delay.setDelay(propDelay);
			delay.setBits(bits);
			if (left.isSelected()) {
				delay.setOrientation(Orientation.LEFT);
			}
			else if (right.isSelected()) {
				delay.setOrientation(Orientation.RIGHT);
			}
			else if (up.isSelected()) {
				delay.setOrientation(Orientation.UP);
			}
			else {
				delay.setOrientation(Orientation.DOWN);
			}
			delayPad.close();
			gatesPad.close();
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			delayPad.close();
			gatesPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of DelayGateDialog class
