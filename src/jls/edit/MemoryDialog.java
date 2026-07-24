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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.TellUser;
import jls.Util;
import jls.elem.Element;
import jls.elem.Memory;
import jls.util.Placement;

/**
 * GUI-side creation/edit dialog for {@link Memory} (issue #77). Moved from
 * the former {@code Memory.MemoryEdit} inner dialog +
 * {@code Memory.setup}/{@code Memory.change}, so the model stays headless.
 * One instance is registered for creation and one for change; the form
 * applies the validated name/type/bits/capacity/initial-contents to the
 * memory through its public setters, then sizes and positions it.
 */
public final class MemoryDialog implements ElementDialog {

	/** The default capacity, in words (matches {@code Memory.defaultCapacity}). */
	private static final int DEFAULT_CAPACITY = 2;

	/** True for the create dialog, false for the edit dialog. */
	private final boolean creating;

	/**
	 * Creates a memory creation or change dialog.
	 *
	 * @param creating True for the create dialog, false for the edit
	 *        dialog.
	 */
	public MemoryDialog(boolean creating) {
		this.creating = creating;
	} // end of constructor

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Memory mem = (Memory) el;

		if (creating) {
			// show creation dialog
			Form form = new Form(mem, true, g);

			// don't do anything if user cancelled
			if (form.cancelled) {
				return false;
			}

			// complete initialization and place the element
			mem.init(SwingTextMetrics.forGraphics(g));
			Point p = Placement.dropPoint(editWindow, x, y,
					mem.getWidth(), mem.getHeight());
			mem.setXY(p.x, p.y);
			return true;
		}

		// change
		Form form = new Form(mem, false, g);

		// if name too big, resize and detach
		if (form.changed) {
			mem.reinit(SwingTextMetrics.forGraphics(g));
			return true;
		}
		return false;
	} // end of setup method

	/**
	 * The create/modify memory form. Extends the base element dialog and
	 * applies the entered values to the memory.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		/** The memory element being created or edited. */
		private final Memory mem;
		/** Graphics saved so nameFits can measure name widths. */
		private final Graphics saveg;
		/** True if the user cancelled the dialog. */
		private boolean cancelled;
		/** True if the memory's bits/name changed and it must be resized. */
		private boolean changed;
		/** True if creating a new memory element, false if changing one. */
		private final boolean create;

		/** Entry field for the memory's name. */
		private final JTextField nameField;
		/** Entry field for the memory's bit width. */
		private final JTextField bitsField;
		/** Entry field for the memory's capacity, in words. */
		private final JTextField capacityField;
		/** Keypad wired to {@link #bitsField}. */
		private final KeyPad bitsPad;
		/** Keypad wired to {@link #capacityField}. */
		private final KeyPad capacityPad;
		/** Radio button selecting RAM as the memory type. */
		private final JRadioButton ram = new JRadioButton("RAM");
		/** Radio button selecting ROM as the memory type. */
		private final JRadioButton rom = new JRadioButton("ROM");
		/** Button that loads the initial contents from a file. */
		private final JButton fromFile = new JButton("from File");
		/** Button that opens the built-in initial-value editor. */
		private final JButton builtIn = new JButton("Built In");
		/** Working copy of the built-in initial value text, committed on OK. */
		String tempInit;

		/**
		 * Builds the memory creation/change form and populates its fields
		 * from the given memory.
		 *
		 * @param mem The memory element to create or edit.
		 * @param create True to build the create form, false for the
		 *        change form.
		 * @param g The Graphics used to measure name widths.
		 */
		private Form(Memory mem, boolean create, Graphics g) {

			// set up window title
			super(create ? "Create Memory" : "Change Memory", "memory");

			this.mem = mem;
			this.saveg = g;
			this.create = create;
			this.tempInit = mem.getInitialValue();
			this.nameField = new JTextField(mem.getName());
			this.bitsField = new JTextField(mem.getBits() + "", 10);
			this.capacityField = new JTextField(DEFAULT_CAPACITY + "", 10);
			this.bitsPad = new KeyPad(bitsField, 10, mem.getBits(), this);
			this.capacityPad =
					new KeyPad(capacityField, 10, DEFAULT_CAPACITY, this);

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up types
			if (create) {
				JLabel tlbl = new JLabel("Type");
				tlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
				window.add(tlbl);
				JPanel types = new JPanel(new GridLayout(1,2));
				types.add(ram);
				ram.setToolTipText("Random Access Memory");
				ram.setHorizontalAlignment(SwingConstants.CENTER);
				types.add(rom);
				rom.setToolTipText("Read Only Memory");
				rom.setHorizontalAlignment(SwingConstants.CENTER);
				ButtonGroup group = new ButtonGroup();
				group.add(ram);
				group.add(rom);
				window.add(types);
			}

			// set up inputs
			if (create) {
				window.add(new JLabel(" "));
				JPanel info = new JPanel(new BorderLayout());
				JPanel labels = new JPanel(new GridLayout(3,1,1,5));
				JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
				labels.add(name);
				JLabel bits = new JLabel("Bits/Word: ",SwingConstants.RIGHT);
				labels.add(bits);
				JLabel words = new JLabel("Capacity (words): ",SwingConstants.RIGHT);
				labels.add(words);
				info.add(labels,BorderLayout.WEST);

				JPanel fields = new JPanel(new GridLayout(3,1,1,5));
				fields.add(nameField);
				JPanel b = new JPanel(new BorderLayout());
				b.add(bitsField,BorderLayout.CENTER);
				b.add(bitsPad,BorderLayout.EAST);
				fields.add(b);

				JPanel i = new JPanel(new BorderLayout());
				i.add(capacityField,BorderLayout.CENTER);
				i.add(capacityPad,BorderLayout.EAST);
				capacityPad.setBase(10);
				fields.add(i);
				info.add(fields,BorderLayout.CENTER);
				window.add(info);
			}
			else {
				JPanel info = new JPanel(new BorderLayout());
				JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
				info.add(name,BorderLayout.WEST);
				info.add(nameField,BorderLayout.CENTER);
				window.add(info);
			}


			// set up initial values buttons
			window.add(new JLabel(" "));
			JLabel ilbl = new JLabel("Initial Memory Contents:");
			ilbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(ilbl);
			JPanel inits = new JPanel(new GridLayout(1,2));
			inits.add(builtIn);
			builtIn.setToolTipText("open dialog to set initial values");
			inits.add(fromFile);
			fromFile.setToolTipText("open dialog to set file name");
			window.add(inits);

			// set up listeners
			confirmOnEnter(nameField);
			confirmOnEnter(bitsField);
			confirmOnEnter(capacityField);
			fromFile.addActionListener(this);
			builtIn.addActionListener(this);

			finishDialog();
		} // end of constructor

		/**
		 * Check the form against the shared memory constraints (issue
		 * #52): a rejected dialog must leave the memory unchanged.
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			java.util.List<Violation> violations =
					new java.util.ArrayList<Violation>();
			String tname = nameField.getText().trim();
			if (tname.isEmpty() || !Util.isValidName(tname)) {
				violations.add(new Violation("Missing or invalid name",
						nameField));
			}
			else if (!tname.equals(mem.getName())
					&& mem.getCircuit().hasName(tname)) {
				violations.add(new Violation("Duplicate name", nameField));
			}
			int tbits = mem.getBits();
			int tcapacity = mem.getCapacity();
			if (create) {
				try {
					tbits = Integer.parseInt(bitsField.getText());
					String violated = Memory.checkBits(tbits);
					if (violated != null) {
						violations.add(new Violation(violated, bitsField));
					}
				}
				catch (NumberFormatException ex) {
					violations.add(new Violation("Value not numeric",
							bitsField));
				}
				try {
					tcapacity = Integer.parseInt(capacityField.getText(),10);
					String violated = Memory.checkCapacity(tcapacity);
					if (violated != null) {
						violations.add(new Violation(violated, capacityField));
					}
				}
				catch (NumberFormatException ex) {
					violations.add(new Violation("Value not numeric",
							capacityField));
				}
				if (!ram.isSelected() && !rom.isSelected()) {
					violations.add(new Violation("Pick RAM or ROM", ram));
				}
			}
			if (violations.isEmpty()) {
				String msg = mem.initOK(tempInit,tcapacity,tbits,false);
				if (msg != null) {
					violations.add(new Violation(
							msg + " in built in initialization", null));
				}
			}
			return violations;
		} // end of validateInputs method

		/**
		 * Apply the validated form to the memory element.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			int tbits = mem.getBits();
			int tcapacity = mem.getCapacity();
			boolean tram = false;
			if (create) {
				try {
					tbits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					reject("Value not numeric", bitsField);
					return;
				}
				try {
					tcapacity = Integer.parseInt(capacityField.getText(),10);
				}
				catch (NumberFormatException ex) {
					reject("Value not numeric", capacityField);
					return;
				}
				tram = ram.isSelected();
			}

			// everything is ok, so make it permanent
			if (!mem.getName().isEmpty())
				mem.getCircuit().removeName(mem.getName());
			mem.getCircuit().addName(tname);
			mem.setName(tname);
			mem.setInitialValue(tempInit);
			if (create) {
				mem.setType(tram);
				mem.setBits(tbits);
				mem.setCapacity(tcapacity);
				bitsPad.close();
				capacityPad.close();
			}
			mem.getCircuit().markChanged();
			if (!create && !nameFits(tname)) {
				changed = true;
			}
			else {
				changed = false;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * React to the initial-contents buttons.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == fromFile) {

				// get file name
				String file  = TellUser.prompt(this,
						"File name?", mem.getFileName());
				if (file == null)
					return;
				file = file.trim();
				while (!file.isEmpty() && !Util.isValidName(file)) {
					TellUser.error(this,
							"Missing or invalid file name, try again", "Error");
					file = TellUser.prompt(this,
							"File name?", mem.getFileName());
					if (file == null)
						return;
					file = file.trim();
				}
				mem.setFileName(file);
				mem.setInitialValue("");
			}
			else if (event.getSource() == builtIn) {

				// construct input dialog
				final JDialog init = new JDialog(this,true);
				Container window = init.getContentPane();
				window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
				final JTextArea area = new JTextArea(tempInit,10,12);
				JScrollPane pane = new JScrollPane(area);
				window.add(pane);

				Action okAction = new AbstractAction("ok") {
					/**
					 * Accept the edited initial-contents text, validate it,
					 * and close the built-in dialog.
					 *
					 * @param event The event object for this action.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						tempInit = area.getText();
						String msg = mem.initOK(tempInit,Integer.MAX_VALUE,Integer.MAX_VALUE,false);
						if (msg != null) {
							TellUser.error(getParent(),
									msg, "Error");
							return;
						}
						mem.setFileName("");
						init.dispose();
					}
				};
				Action cancelAction = new AbstractAction("cancel") {
					/**
					 * Discard the edits and close the built-in dialog.
					 *
					 * @param event The event object for this action.
					 */
					@Override
					public void actionPerformed(ActionEvent event) {
						init.dispose();
					}
				};
				window.add(new JLabel(" "));
				JPanel okCancel = new JPanel(new GridLayout(1,2));
				okCancel.add(new JButton(okAction));
				okCancel.add(new JButton(cancelAction));
				window.add(okCancel);

				init.pack();
				Point loc = getLocation();
				init.setLocation(loc);
				init.setVisible(true);
			}
		} // end of actionPerformed method

		/**
		 * Cancel this element.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

		/**
		 * See if the given name fits in the box on the screen.
		 *
		 * @param name The value to check.
		 *
		 * @return true if it fits, false if not.
		 */
		public boolean nameFits(String name) {

			FontMetrics fm = saveg.getFontMetrics();
			int w = fm.stringWidth(" " + name + " ");
			return w <= mem.getWidth();

		} // end of nameFits method

	} // end of Form class

} // end of MemoryDialog class
