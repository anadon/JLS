package jls.elem;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
 * BoxLayout, vertical), then call {@link #finishDialog(int, int)} last.
 * Override {@link #validateAndAccept()} with validation + acceptance
 * (call dispose() on success), and {@link #cancelDialog()} if cancelling
 * needs more than dispose().
 */
@SuppressWarnings("serial")
public abstract class ElementDialog extends JDialog {

	/** The confirm button; also triggered by Enter. */
	protected final JButton ok = new JButton("OK");

	/** The cancel button; also triggered by Escape and the close box. */
	protected final JButton cancel = new JButton("Cancel");

	private final String helpTopic;

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
	} // end of constructor

	/**
	 * Append the button row, install uniform key handling, then pack,
	 * center on the given point, and show (modal: this blocks until the
	 * dialog is disposed). Call last in the subclass constructor.
	 *
	 * @param x The x-coordinate to center on.
	 * @param y The y-coordinate to center on.
	 */
	protected void finishDialog(int x, int y) {

		// button row
		Container window = getContentPane();
		window.add(new JLabel(" "));
		JPanel buttons = new JPanel(new GridLayout(1, helpTopic == null ? 2 : 3));
		ok.setBackground(Color.green);
		buttons.add(ok);
		cancel.setBackground(Color.pink);
		buttons.add(cancel);
		if (helpTopic != null) {
			JButton help = new JButton("Help");
			Help.enableHelpOnButton(help, helpTopic);
			buttons.add(help);
		}
		window.add(buttons);

		// uniform keyboard behavior: Enter confirms, Escape cancels
		getRootPane().setDefaultButton(ok);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				validateAndAccept();
			}
		});
		ActionListener doCancel = new ActionListener() {
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
			public void windowClosing(WindowEvent e) {
				cancelDialog();
			}
		});

		// finish up GUI
		pack();
		Dimension d = getSize();
		setLocation(x - d.width / 2, y - d.height / 2);
		setVisible(true);
	} // end of finishDialog method

	/**
	 * Wire a form field so Enter inside it confirms the dialog, matching
	 * the historical per-dialog behavior of text fields.
	 *
	 * @param field A component with addActionListener (e.g. JTextField).
	 */
	protected void confirmOnEnter(javax.swing.JTextField field) {

		field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				validateAndAccept();
			}
		});
	} // end of confirmOnEnter method

	/**
	 * Show a validation error and keep the dialog open.
	 *
	 * @param message The problem to report.
	 */
	protected void reject(String message) {

		JOptionPane.showMessageDialog(this, message, "Error",
				JOptionPane.ERROR_MESSAGE);
	} // end of reject method

	/**
	 * Validate the form; on success apply it and dispose(), on failure
	 * reject(...) and return without disposing.
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
