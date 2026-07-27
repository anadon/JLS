package jls.edit;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.elem.Element;
import jls.elem.Timed;

/**
 * GUI-side "Change Timing" dialog (issue #77). Moved from the former
 * {@code Element.DelayChange} inner dialog so the element model stays
 * headless; the editor's "Change Timing" action opens it through
 * {@link #open(Element)}. The dialog reads and writes the element's
 * propagation delay / access time through the {@link Timed} capability's
 * {@link Timed#getDelay()} / {@link Timed#setDelay(int)} accessors (issue
 * #78), so it works for any timed element without touching the model's
 * internals.
 */
public final class DelayChangeDialog {

	/**
	 * Prevents instantiation; this class only holds a static entry point.
	 */
	private DelayChangeDialog() {} // no instances

	/**
	 * Present the change-timing dialog for an element and apply the result.
	 * The "access time" vs "propagation delay" wording is the element's own
	 * concern, declared through the {@link Timed} capability (issue #78).
	 *
	 * @param el The element whose timing is being changed.
	 */
	public static void open(Element el) {

		if (el instanceof Timed timed) {
			new Form(timed, timed.usesAccessTime());
		}
	} // end of open method

	/**
	 * The change-timing form. Extends the base element dialog and applies
	 * the entered delay to the element.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		/** The element whose timing this form edits. */
		private final Timed element;
		/** Field to enter the delay or access time. */
		private final JTextField delayField = new JTextField(10);
		/** Keypad for the delay field. */
		private final KeyPad delayPad;

		/**
		 * Set up change dialog window.
		 *
		 * @param el The element whose timing is being changed.
		 * @param isMemory True if this is a memory element, false if not.
		 */
		private Form(Timed el, boolean isMemory) {

			// set up window title
			super("Change Timing", null);

			element = el;
			delayPad = new KeyPad(delayField, 10, 0, this);

			// set up window
			Container window = getContentPane();

			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel delay;
			if (isMemory) {
				delay = new JLabel("Memory access time: ", SwingConstants.RIGHT);
			}
			else {
				delay = new JLabel("Propagation delay: ", SwingConstants.RIGHT);
			}
			labelled(delay, delayField, "dialog.delaychange.delay");
			info.add(delay, BorderLayout.WEST);
			info.add(delayField, BorderLayout.CENTER);
			delayField.setText(el.getDelay() + "");
			info.add(delayPad, BorderLayout.EAST);
			window.add(info);

			confirmOnEnter(delayField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate and apply the new delay.
		 */
		@Override
		protected void validateAndAccept() {

			int temp = 0;
			try {
				temp = Integer.parseInt(delayField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (temp <= 0) {
				reject("Propagation delay must be greater than 0");
				return;
			}
			element.setDelay(temp);
			dispose();
		} // end of validateAndAccept method

	} // end of Form class

} // end of DelayChangeDialog class
