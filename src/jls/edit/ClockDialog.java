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
import jls.elem.Clock;
import jls.elem.Element;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link Clock} (issue #77). Moved from
 * the former {@code Clock.ClockCreate} inner dialog + {@code Clock.setup}/
 * {@code Clock.change}, so the model stays headless. One instance is
 * registered for creation and one for change; the form applies the chosen
 * cycle time, one time, and orientation to the clock through its public
 * setters, then sizes and positions it.
 */
public final class ClockDialog implements ElementDialog {

	private static final int DEFAULT_CYCLE_TIME = 2;
	private static final int DEFAULT_ONE_TIME = DEFAULT_CYCLE_TIME / 2;

	private final boolean creating;

	/**
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public ClockDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Clock clock = (Clock) el;

		if (creating) {
			// show creation dialog
			Form form = new Form(clock);

			// don't do anything if user cancelled
			if (form.cancelled) {
				return false;
			}

			// complete initialization and place the element
			clock.init(SwingTextMetrics.forGraphics(g));
			Point p = Placement.dropPoint(editWindow, x, y,
					clock.getWidth(), clock.getHeight());
			clock.setXY(p.x, p.y);
			return true;
		}

		// change
		Form form = new Form(clock);
		if (!form.cancelled) {
			clock.getCircuit().markChanged();
		}
		return false;
	} // end of setup method

	/**
	 * The create/edit-clock form. Extends the base element dialog and
	 * applies the entered cycle time, one time, and orientation to the
	 * clock.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		private final Clock clock;
		private boolean cancelled;
		private final JTextField cycleTimeField;
		private final JTextField oneTimeField;
		private final JRadioButton left = new JRadioButton("Left");
		private final JRadioButton right = new JRadioButton("Right", true);
		private final JRadioButton up = new JRadioButton("Up");
		private final JRadioButton down = new JRadioButton("Down");

		private Form(Clock clock) {

			// set up window title
			super("Create Clock", "clock");
			this.clock = clock;

			// set not cancelled
			cancelled = false;

			// set up fields and keypads
			cycleTimeField = new JTextField(clock.getCycleTime() + "", 10);
			oneTimeField = new JTextField(clock.getOneTime() + "", 10);
			KeyPad cycleTimePad =
					new KeyPad(cycleTimeField, 10, DEFAULT_CYCLE_TIME, this);
			KeyPad oneTimePad =
					new KeyPad(oneTimeField, 10, DEFAULT_ONE_TIME, this);

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());

			JPanel labels = new JPanel(new GridLayout(3, 1, 1, 5));
			JLabel ctime = new JLabel("Cycle Time: ", SwingConstants.RIGHT);
			labels.add(ctime);
			JLabel otime = new JLabel("One Time: ", SwingConstants.RIGHT);
			labels.add(otime);
			info.add(labels, BorderLayout.WEST);

			JPanel fields = new JPanel(new GridLayout(2, 1, 1, 5));
			JPanel ct = new JPanel(new BorderLayout());
			ct.add(cycleTimeField, BorderLayout.CENTER);
			ct.add(cycleTimePad, BorderLayout.EAST);
			fields.add(ct);
			JPanel ot = new JPanel(new BorderLayout());
			ot.add(oneTimeField, BorderLayout.CENTER);
			ot.add(oneTimePad, BorderLayout.EAST);
			fields.add(ot);
			info.add(fields, BorderLayout.CENTER);
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

			confirmOnEnter(cycleTimeField);
			confirmOnEnter(oneTimeField);
			finishDialog();
		} // end of constructor

		/**
		 * Check the times against the shared clock constraints (issue
		 * #52): a rejected dialog must leave the clock unchanged.
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			int newCycleTime;
			int newOneTime;
			try {
				newCycleTime = Integer.parseInt(cycleTimeField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new Violation(
						"Value not numeric, try again", cycleTimeField));
			}
			try {
				newOneTime = Integer.parseInt(oneTimeField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new Violation(
						"Value not numeric, try again", oneTimeField));
			}
			String violated = Clock.checkCycleTime(newCycleTime);
			if (violated != null) {
				return java.util.List.of(new Violation(violated, cycleTimeField));
			}
			violated = Clock.checkOneTime(newCycleTime, newOneTime);
			if (violated != null) {
				return java.util.List.of(new Violation(violated, oneTimeField));
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Set the clock parameters from the validated form.
		 */
		@Override
		protected void validateAndAccept() {

			int newCycleTime;
			int newOneTime;
			try {
				newCycleTime = Integer.parseInt(cycleTimeField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again", cycleTimeField);
				return;
			}
			try {
				newOneTime = Integer.parseInt(oneTimeField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again", oneTimeField);
				return;
			}
			clock.setCycleTime(newCycleTime);
			clock.setOneTime(newOneTime);
			if(left.isSelected())
			{
				clock.setOrientation(Orientation.LEFT);
			}
			else if(right.isSelected())
			{
				clock.setOrientation(Orientation.RIGHT);
			}
			else if(up.isSelected())
			{
				clock.setOrientation(Orientation.UP);
			}
			else if(down.isSelected())
			{
				clock.setOrientation(Orientation.DOWN);
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this element.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of ClockDialog class
