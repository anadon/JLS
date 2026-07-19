/**
 *
 */
package jls;

import java.awt.Component;
import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

/**
 * All communication from JLS to the user funnels through this class.
 *
 * @author Josh Marshall
 *
 * The single seam through which JLS talks to the user (issue #81):
 * severity-typed notifications, confirmations and prompts, aware of
 * whether the run is interactive.
 *
 * In GUI mode each method opens a consistently parented dialog (the
 * caller's component, falling back to the application frame). In batch
 * or headless mode no dialog is ever attempted - attempting one throws
 * HeadlessException, bypassing the System.exit that follows and turning
 * a user error into a crash report (#42). Instead, diagnostics go to
 * stderr using the CLI contract's line format ("jls: error:" /
 * "jls: warning:"), keeping stdout pure for simulation output, and the
 * interactive methods return safe defaults (confirm: no, confirmOrCancel:
 * cancel, prompt: null).
 *
 * Intended for anticipated issues, not unexpected events intended for
 * the DefaultExceptionHandler class.
 *
 * Raw JOptionPane use in application code is a test failure
 * (NotificationRatchetTest); this class must stay the only place that
 * touches it.
 */
public class TellUser {

	/**
	 * Create a TellUser. Never needed: every method is static.
	 */
	public TellUser() {
	}

	/**
	 * The answer to a three-way (yes/no/cancel) question. Closing the
	 * dialog counts as CANCEL: an unanswered question must never take
	 * the destructive branch.
	 */
	public enum Answer {
		/** The user said yes. */
		YES,
		/** The user said no. */
		NO,
		/** The user cancelled or closed the dialog, or the run is batch/headless. */
		CANCEL
	}

	/**
	 * Whether a dialog can and should actually be shown. In batch mode
	 * (-b) dialogs are forbidden by the CLI contract even if a display
	 * exists; in headless mode they would throw HeadlessException.
	 *
	 * @return true if a dialog may be shown.
	 */
	static private boolean interactive() {
		return !JLSInfo.batch && !GraphicsEnvironment.isHeadless();
	}

	/**
	 * The component to parent a dialog on: the caller's component if it
	 * gave one, else the application frame (never a bare null, which
	 * can center the dialog on the wrong monitor).
	 *
	 * @param parent The caller's component, or null for the application frame.
	 *
	 * @return the component to parent the dialog on.
	 */
	static private Component parentFor(Component parent) {
		return parent != null ? parent : JLSInfo.frame;
	}

	/**
	 * Tell the user something informational.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param message The message to show.
	 * @param title The dialog title.
	 */
	static public void info(Component parent, String message, String title) {

		if (!interactive()) {
			// diagnostics go to stderr; stdout is reserved for
			// simulation output (#42)
			System.err.println("jls: " + message);
			return;
		}
		JOptionPane.showMessageDialog(parentFor(parent), message, title,
				JOptionPane.INFORMATION_MESSAGE);
	} // end of info method

	/**
	 * Warn the user about something suspicious but survivable.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param message The message to show.
	 * @param title The dialog title.
	 */
	static public void warn(Component parent, String message, String title) {

		if (!interactive()) {
			System.err.println("jls: warning: " + message);
			return;
		}
		JOptionPane.showMessageDialog(parentFor(parent), message, title,
				JOptionPane.WARNING_MESSAGE);
	} // end of warn method

	/**
	 * Tell the user something went wrong.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param message The message to show.
	 * @param title The dialog title.
	 */
	static public void error(Component parent, String message, String title) {

		if (!interactive()) {
			System.err.println("jls: error: " + message);
			return;
		}
		JOptionPane.showMessageDialog(parentFor(parent), message, title,
				JOptionPane.ERROR_MESSAGE);
	} // end of error method

	/**
	 * Ask the user a yes/no question.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param question The question to ask.
	 * @param title The dialog title.
	 *
	 * @return true if the user said yes; false if the user said no or
	 *         closed the dialog, or always in batch/headless mode (the
	 *         safe default).
	 */
	static public boolean confirm(Component parent, String question,
			String title) {

		if (!interactive()) {
			System.err.println("jls: warning: " + question
					+ " (cannot ask in batch mode, assuming no)");
			return false;
		}
		return JOptionPane.showConfirmDialog(parentFor(parent), question,
				title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	} // end of confirm method

	/**
	 * Ask the user a yes/no question that can also be cancelled
	 * (e.g. "save before closing?").
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param question The question to ask.
	 * @param title The dialog title.
	 *
	 * @return the user's answer; closing the dialog counts as CANCEL,
	 *         and batch/headless mode always answers CANCEL (the safe
	 *         default).
	 */
	static public Answer confirmOrCancel(Component parent, String question,
			String title) {

		if (!interactive()) {
			System.err.println("jls: warning: " + question
					+ " (cannot ask in batch mode, cancelling)");
			return Answer.CANCEL;
		}
		int result = JOptionPane.showConfirmDialog(parentFor(parent),
				question, title, JOptionPane.YES_NO_CANCEL_OPTION);
		if (result == JOptionPane.YES_OPTION)
			return Answer.YES;
		if (result == JOptionPane.NO_OPTION)
			return Answer.NO;
		return Answer.CANCEL;
	} // end of confirmOrCancel method

	/**
	 * Ask the user to type something in.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param message What to ask for.
	 *
	 * @return what the user typed, or null if the user cancelled or the
	 *         run is batch/headless (the safe default).
	 */
	static public String prompt(Component parent, String message) {

		if (!interactive()) {
			System.err.println("jls: warning: " + message
					+ " (cannot prompt in batch mode)");
			return null;
		}
		return JOptionPane.showInputDialog(parentFor(parent), message);
	} // end of prompt method

	/**
	 * Ask the user to type something in, with an initial value already
	 * filled in.
	 *
	 * @param parent The component to parent the dialog on, or null for
	 *               the application frame.
	 * @param message What to ask for.
	 * @param initial The initial value shown in the input field.
	 *
	 * @return what the user typed, or null if the user cancelled or the
	 *         run is batch/headless (the safe default).
	 */
	static public String prompt(Component parent, String message,
			String initial) {

		if (!interactive()) {
			System.err.println("jls: warning: " + message
					+ " (cannot prompt in batch mode)");
			return null;
		}
		return JOptionPane.showInputDialog(parentFor(parent), message,
				initial);
	} // end of prompt method
}
