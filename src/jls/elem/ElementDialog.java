package jls.elem;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import jls.Help;
import jls.JLSInfo;

/**
 * Shared skeleton for element create/modify dialogs (issue #26).
 *
 * Every element dialog historically re-wired the same structure by hand:
 * a modal window on the main frame, a BoxLayout content pane of form
 * fields, an OK/Cancel/Help button row, close-box-cancels, pack, center
 * on the cursor, show. The copies drifted in keyboard behavior. This base
 * owns the skeleton and makes the key handling uniform: Enter confirms
 * (OK is the default button), Escape cancels, closing the window cancels.
 *
 * Usage: subclass, build the form into {@link #getContentPane()} (already
 * BoxLayout, vertical), then call {@link #finishDialog()} last.
 * Override {@link #validateInputs()} to declare the element's parameter
 * constraints and {@link #validateAndAccept()} with the acceptance (call
 * dispose() on success); override {@link #cancelDialog()} if cancelling
 * needs more than dispose().
 *
 * Validation runs on OK (issue #52 addendum): invalid input never closes
 * the dialog and never mutates the element. The first violated constraint
 * is shown inline in the dialog - not as a modal popup - the offending
 * field receives focus with its text selected for immediate retyping, and
 * the message is set as the field's accessible description so it is
 * screen-reader reachable. The constraint message strings are the same
 * shared constants the file loader rejects with (one string, two
 * surfaces).
 */
@SuppressWarnings("serial")
public abstract class ElementDialog extends JDialog {

	/** The confirm button; also triggered by Enter. */
	protected final JButton ok = new JButton("OK");

	/** The cancel button; also triggered by Escape and the close box. */
	protected final JButton cancel = new JButton("Cancel");

	/** Inline validation-error message; hidden until an OK is rejected. */
	private final JLabel errorLabel = new JLabel();

	private final String helpTopic;

	/** True once finishDialog packed the window (so error messages repack). */
	private boolean packedToFit = false;

	/**
	 * One violated parameter constraint: the constraint message (the same
	 * shared constant the file loader uses for this rule, issue #52 P5)
	 * plus the form field holding the offending value, or null when the
	 * violation is structural rather than a field value.
	 */
	protected static final class Violation {

		private final String message;
		private final JComponent field;

		/**
		 * Create a violation.
		 *
		 * @param message The constraint, stated in domain terms.
		 * @param field The offending form field, or null.
		 */
		public Violation(String message, JComponent field) {

			this.message = message;
			this.field = field;
		} // end of constructor

		/**
		 * @return the constraint message.
		 */
		public String getMessage() {

			return message;
		} // end of getMessage method

		/**
		 * @return the offending form field, or null.
		 */
		public JComponent getField() {

			return field;
		} // end of getField method

	} // end of Violation class

	/**
	 * Create the dialog shell (not yet visible).
	 *
	 * @param title The window title.
	 * @param helpTopic The help topic id for the Help button, or null for
	 *            no Help button.
	 */
	protected ElementDialog(String title, String helpTopic) {

		super(JLSInfo.frame, title, true);
		this.helpTopic = helpTopic;
		getContentPane().setLayout(
				new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		errorLabel.setForeground(Color.red);
		errorLabel.setAlignmentX(CENTER_ALIGNMENT);
		errorLabel.setVisible(false);
	} // end of constructor

	/**
	 * Append the error label and button row, install uniform key
	 * handling, then pack, center on the owner window (owner-relative
	 * placement, issue #104 - no absolute screen coordinates), and show
	 * (modal: this blocks until the dialog is disposed). Call last in
	 * the subclass constructor.
	 */
	protected void finishDialog() {

		// error label and button row
		Container window = getContentPane();
		window.add(new JLabel(" "));
		window.add(errorLabel);
		JPanel buttons = new JPanel(new GridLayout(1, helpTopic == null ? 2 : 3));
		buttons.add(ok);
		buttons.add(cancel);
		if (helpTopic != null) {
			JButton help = new JButton("Help");
			Help.enableHelpOnButton(help, helpTopic);
			buttons.add(help);
		}
		window.add(buttons);

		installDialogBehavior();

		// finish up GUI
		packedToFit = true;
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	} // end of finishDialog method

	/**
	 * Install the uniform behavior: OK (and Enter) runs validation then
	 * acceptance, Escape and the close box cancel. finishDialog calls
	 * this; a dialog that lays out its own button row (the big editors)
	 * must instead place {@link #ok}, {@link #cancel} and
	 * {@link #getErrorLabel()} itself and call this before showing the
	 * window.
	 */
	protected final void installDialogBehavior() {

		// uniform keyboard behavior: Enter confirms, Escape cancels
		ok.setBackground(Color.green);
		cancel.setBackground(Color.pink);
		getRootPane().setDefaultButton(ok);
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				confirmDialog();
			}
		});
		ActionListener doCancel = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				cancelDialog();
			}
		};
		cancel.addActionListener(doCancel);
		getRootPane().registerKeyboardAction(doCancel,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		// closing the window cancels
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelDialog();
			}
		});
	} // end of installDialogBehavior method

	/**
	 * Wire a form field so Enter inside it confirms the dialog, matching
	 * the historical per-dialog behavior of text fields.
	 *
	 * @param field A component with addActionListener (e.g. JTextField).
	 */
	protected void confirmOnEnter(javax.swing.JTextField field) {

		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				confirmDialog();
			}
		});
	} // end of confirmOnEnter method

	/**
	 * The OK path: check the declared constraints first; on a violation
	 * report it inline and keep the dialog open (the element must not be
	 * mutated), otherwise clear any stale error and run the acceptance.
	 */
	protected final void confirmDialog() {

		List<Violation> violations = validateInputs();
		if (!violations.isEmpty()) {
			showViolation(violations.get(0));
			return;
		}
		clearError();
		validateAndAccept();
	} // end of confirmDialog method

	/**
	 * Check the form against the element's parameter constraints, in form
	 * order. Called on OK before {@link #validateAndAccept()}; a nonempty
	 * result keeps the dialog open with the first violation shown inline
	 * and its field focused. Must not mutate the element. Default: no
	 * constraints.
	 *
	 * @return the violated constraints, empty if the form is valid.
	 */
	protected List<Violation> validateInputs() {

		return Collections.emptyList();
	} // end of validateInputs method

	/**
	 * The inline error label, for dialogs that lay out their own content
	 * instead of calling finishDialog (which places it automatically).
	 *
	 * @return the label violations are reported in.
	 */
	protected final JLabel getErrorLabel() {

		return errorLabel;
	} // end of getErrorLabel method

	/**
	 * Show a violation inline: message in the error label, focus moved to
	 * the offending field with its text selected for retyping, and the
	 * message set as the accessible description of both label and field
	 * (issue #52 addendum, P6).
	 *
	 * @param violation The violated constraint.
	 */
	private void showViolation(Violation violation) {

		errorLabel.setText(violation.getMessage());
		errorLabel.setVisible(true);
		errorLabel.getAccessibleContext()
				.setAccessibleDescription(violation.getMessage());
		JComponent field = violation.getField();
		if (field != null) {
			field.getAccessibleContext()
					.setAccessibleDescription(violation.getMessage());
			if (field instanceof JTextField) {
				((JTextField) field).selectAll();
			}
			field.requestFocusInWindow();
		}
		// a packed dialog may need to grow to show the whole message;
		// fixed-size editors just use the reserved label space
		if (packedToFit) {
			pack();
		}
	} // end of showViolation method

	/**
	 * Hide the inline error message (input became valid).
	 */
	private void clearError() {

		errorLabel.setText("");
		errorLabel.setVisible(false);
	} // end of clearError method

	/**
	 * Show a validation error inline and keep the dialog open, without an
	 * offending field to focus. Prefer declaring constraints in
	 * {@link #validateInputs()}, which also handles focus and selection.
	 *
	 * @param message The problem to report.
	 */
	protected void reject(String message) {

		showViolation(new Violation(message, null));
	} // end of reject method

	/**
	 * Show a validation error inline, focus the offending field with its
	 * text selected, and keep the dialog open.
	 *
	 * @param message The problem to report.
	 * @param field The offending form field.
	 */
	protected void reject(String message, JComponent field) {

		showViolation(new Violation(message, field));
	} // end of reject method

	/**
	 * Apply the (already validated) form and dispose(). Runs only after
	 * {@link #validateInputs()} returned no violations; residual checks
	 * may still reject(...) and return without disposing.
	 */
	protected abstract void validateAndAccept();

	/**
	 * Cancel the dialog. Default just disposes; override to record
	 * cancellation.
	 */
	protected void cancelDialog() {

		dispose();
	} // end of cancelDialog method

} // end of ElementDialog class
