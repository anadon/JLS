package jls.edit;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.Help;
import jls.Util;
import jls.elem.TruthTable;

/**
 * GUI-side create/edit dialog for a {@link TruthTable} (issue #77). Moved
 * from the former {@code TruthTable.TTEditor} inner class; it owns the
 * {@link DisplayBool} view and applies the entered name and signal edits
 * to the element through its public model methods, so the model stays
 * headless.
 *
 * @author David A. Poplawski
 */
@SuppressWarnings("serial")
public final class TruthTableEditor extends ElementFormDialog
		implements ActionListener {

	/** Initial width of the edit dialog, in pixels. */
	private static final int dialogWidth = 300;
	/** Initial height of the edit dialog, in pixels. */
	private static final int dialogHeight = 500;

	// properties
	/** The truth table element being created or edited. */
	private final TruthTable ttelem;
	/** The table display view. */
	private final DisplayBool disp;
	/** Text field for typing the name of a new input signal. */
	private JTextField inputField = new JTextField(10);
	/** Text field for typing the name of a new output signal. */
	private JTextField outputField = new JTextField(10);
	/** Text field for editing the element's name. */
	private JTextField nameField = new JTextField(10);

	/**
	 * Initialize and show dialog.
	 *
	 * @param ttelem The truth table element being created or edited.
	 */
	public TruthTableEditor(TruthTable ttelem) {

		// set up window title
		super("Edit Truth Table",null);

		this.ttelem = ttelem;

		// set up display
		disp = new DisplayBool(ttelem);
		ttelem.setDisplayRefresher(disp::refresh);
		ttelem.setEditParent(this);

		// set not cancelled
		ttelem.setCancelled(false);

		// set up window
		Container window = getContentPane();
		window.setLayout(new BorderLayout());

		// add components
		JScrollPane pane = new JScrollPane(disp);
		window.add(pane,BorderLayout.CENTER);
		JPanel other = new JPanel(new BorderLayout());
		JPanel info = new JPanel(new BorderLayout());
		JPanel labels = new JPanel(new GridLayout(3,1));
		labels.add(new JLabel("new input: ",SwingConstants.RIGHT));
		labels.add(new JLabel("new output: ",SwingConstants.RIGHT));
		labels.add(new JLabel("name: ",SwingConstants.RIGHT));
		info.add(labels,BorderLayout.WEST);
		JPanel inputs = new JPanel(new GridLayout(3,1));
		inputs.add(inputField);
		inputs.add(outputField);
		inputs.add(nameField);
		nameField.setText(ttelem.getName());
		info.add(inputs,BorderLayout.CENTER);
		other.add(info,BorderLayout.NORTH);
		other.add(getErrorLabel(),BorderLayout.CENTER);
		JPanel okCancel = new JPanel(new GridLayout(1,3));
		okCancel.add(ok);
		okCancel.add(cancel);
		JButton help = new JButton("Help");
		Help.enableHelpOnButton(help, "truth");
		okCancel.add(help);
		other.add(okCancel,BorderLayout.SOUTH);
		window.add(other,BorderLayout.SOUTH);

		// add listeners (OK, Cancel, Escape and the close box are
		// wired by the shared dialog base, issue #26)
		inputField.addActionListener(this);
		outputField.addActionListener(this);
		confirmOnEnter(nameField);
		installDialogBehavior();

		// lay out the table once the window exists
		addWindowListener (
				new WindowAdapter() {
					/** Lays out the table display once the dialog window is open. */
					@Override
					public void windowOpened(WindowEvent event) {
						disp.doLayout(ttelem.getInputNames(),
								ttelem.getOutputNames(),ttelem.getTable(),null);
						disp.repaint();
					}
				}
		);

		// finish up: place relative to the owner window (#104)
		setSize(dialogWidth,dialogHeight);
		setLocationRelativeTo(getOwner());
		setVisible(true);
	} // end of constructor

	/**
	 * Listen for the new-signal field events.
	 *
	 * @param event The event object.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		if (event.getSource() == inputField) {
			ttelem.addInput(inputField.getText().trim());
			inputField.setText("");
		}
		else if (event.getSource() == outputField) {
			ttelem.addOutput(outputField.getText().trim());
			outputField.setText("");
		}
	} // end of actionPerformed method

	/**
	 * Check the form against the truth table constraints (issue #52).
	 *
	 * @return the violated constraints, empty if the form is valid.
	 */
	@Override
	protected java.util.List<Violation> validateInputs() {

		java.util.List<Violation> violations =
				new java.util.ArrayList<Violation>();
		String tname = nameField.getText().trim();
		if (tname.isEmpty() || !Util.isValidName(tname)) {
			violations.add(new Violation("Missing or invalid element name",
					nameField));
		}
		else if (!tname.equals(ttelem.getName())
				&& ttelem.getCircuit().hasName(tname)) {
			violations.add(new Violation("Duplicate element name",
					nameField));
		}
		if (ttelem.getInputNames().size() == 0
				|| ttelem.getOutputNames().size() == 0) {
			violations.add(new Violation(TruthTable.SIGNALS_CONSTRAINT,
					inputField));
		}
		return violations;
	} // end of validateInputs method

	/**
	 * Apply the validated form to the truth table.
	 */
	@Override
	protected void validateAndAccept() {

		String tname = nameField.getText().trim();
		ttelem.acceptName(tname);
		dispose();
	} // end of validateAndAccept method

	/**
	 * Cancel the edit.
	 */
	@Override
	protected void cancelDialog() {

		// restore info
		ttelem.restoreFromCancel();
		dispose();
	} // end of cancelDialog method

} // end of TruthTableEditor class
