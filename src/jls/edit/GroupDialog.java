package jls.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.core.Orientation;
import jls.elem.Binder;
import jls.elem.Element;
import jls.elem.Group;
import jls.util.Placement;

/**
 * GUI-side creation dialog shared by {@link jls.elem.Binder} and
 * {@link jls.elem.Splitter} (issue #77). Moved from the former
 * {@code Group.GroupCreate} + {@code Group.GetRanges} inner dialogs and
 * the two {@code setup} methods, so the model stays headless. The dialog
 * resolves the bundler/unbundler wording from the element type, applies
 * the chosen bit width, bit groups, and orientation to the group through
 * its public setters, then sizes and positions it.
 */
public final class GroupDialog implements ElementDialog {

	/**
	 * Creates a {@code GroupDialog}.
	 */
	public GroupDialog() {
	}

	/** Default bit width shown in the bits field. */
	private static final int DEFAULT_BITS = 2;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Group group = (Group) el;
		String type = (el instanceof Binder) ? "Bundler" : "Unbundler";

		// show creation dialog
		CreateForm form = new CreateForm(group, type);

		// don't do anything if user cancelled
		if (form.cancelled) {
			return false;
		}

		// complete initialization and place the element
		group.init(SwingTextMetrics.forGraphics(g));
		Point p = Placement.dropPoint(editWindow, x, y,
				group.getWidth(), group.getHeight());
		group.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-group form: bit width, single/grouped choice, and
	 * orientation. On a grouped choice it opens the {@link RangesForm} to
	 * pick the bit groups.
	 */
	@SuppressWarnings("serial")
	private static final class CreateForm extends ElementFormDialog {

		/** Group element being configured. */
		private final Group group;
		/** Element type, {@code "Bundler"} or {@code "Unbundler"}. */
		private final String type;
		/** True if the user cancelled the dialog. */
		private boolean cancelled;
		/** Entry field for the bit count. */
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 10);
		/** Keypad that feeds numeric input into {@link #bitsField}. */
		private final KeyPad bitsPad =
				new KeyPad(bitsField, 10, DEFAULT_BITS, this);
		/** Radio button selecting single-bit ranges. */
		private final JRadioButton single = new JRadioButton("Single Bits");
		/** Radio button selecting grouped bit ranges. */
		private final JRadioButton groupBits = new JRadioButton("Group Bits");
		/** Radio button selecting left orientation. */
		private final JRadioButton left = new JRadioButton("Left");
		/** Radio button selecting right orientation (default). */
		private final JRadioButton right = new JRadioButton("Right", true);
		/** Radio button selecting up orientation. */
		private final JRadioButton up = new JRadioButton("Up");
		/** Radio button selecting down orientation. */
		private final JRadioButton down = new JRadioButton("Down");

		/**
		 * Creates the create-group form.
		 *
		 * @param group group being configured
		 * @param type element type, {@code "Bundler"} or {@code "Unbundler"}
		 */
		private CreateForm(Group group, String type) {

			super("Create " + type,
					type.equals("Unbundler") ? "unbundle" : "bundle");
			this.group = group;
			this.type = type;
			cancelled = false;

			Container window = getContentPane();

			JPanel info = new JPanel(new BorderLayout());
			JLabel bits;
			if (type.equals("Unbundler")) {
				bits = new JLabel("Input Bits: ", SwingConstants.RIGHT);
			}
			else {
				bits = new JLabel("Output Bits: ", SwingConstants.RIGHT);
			}
			info.add(bits, BorderLayout.WEST);
			info.add(bitsField, BorderLayout.CENTER);
			info.add(bitsPad, BorderLayout.EAST);
			window.add(info);

			// set up single bits or grouped choice
			window.add(new JLabel(" "));
			JPanel choice = new JPanel(new GridLayout(1, 2));
			single.setSelected(true);
			choice.add(single);
			choice.add(groupBits);
			window.add(choice);
			ButtonGroup choices = new ButtonGroup();
			choices.add(single);
			choices.add(groupBits);

			// set up orientation radio buttons
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
			gr.add(up);
			gr.add(down);
			window.add(orients);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		@Override
		protected java.util.List<Violation> validateInputs() {

			int newBits;
			try {
				newBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new Violation(
						"Value not numeric, try again", bitsField));
			}
			String violated = Group.checkBits(newBits);
			if (violated != null) {
				return java.util.List.of(new Violation(violated, bitsField));
			}
			return java.util.List.of();
		} // end of validateInputs method

		@Override
		protected void validateAndAccept() {

			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again", bitsField);
				return;
			}
			group.setBits(bits);

			// set up ranges
			if (single.isSelected()) {
				for (int b = 0; b < bits; b += 1) {
					group.addRange(new Group.Entry(b, b));
				}
			}
			else {
				dispose();
				RangesForm ranges = new RangesForm(group, type);
				if (ranges.cancelled) {
					cancelled = true;
				}
			}
			if (left.isSelected()) {
				group.setOrientation(Orientation.LEFT);
			}
			else if (right.isSelected()) {
				group.setOrientation(Orientation.RIGHT);
			}
			else if (up.isSelected()) {
				group.setOrientation(Orientation.UP);
			}
			else if (down.isSelected()) {
				group.setOrientation(Orientation.DOWN);
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of CreateForm class

	/**
	 * The pick-bit-groups form: choose which bundle bits map to each
	 * narrow put. Applies the chosen groups to the element in order.
	 */
	@SuppressWarnings("serial")
	private static final class RangesForm extends ElementFormDialog
			implements ActionListener {

		/** Group element being configured. */
		private final Group group;
		/** Element type label passed in from the calling form. */
		private final String type;
		/** True if the user cancelled the dialog. */
		private boolean cancelled;
		/** Model backing the {@link #choose} list of selectable bits. */
		private final DefaultListModel<Group.Entry> pick =
				new DefaultListModel<Group.Entry>();
		/** List of bits available to choose from. */
		private final JList<Group.Entry> choose =
				new JList<Group.Entry>(pick);
		/** Model backing the {@link #chosen} list of picked bit groups. */
		private final DefaultListModel<Group.Entry> picked =
				new DefaultListModel<Group.Entry>();
		/** List of bit groups the user has picked. */
		private final JList<Group.Entry> chosen =
				new JList<Group.Entry>(picked);
		/** Button that moves selected bits into the chosen list. */
		private final JButton add = new JButton(">>");
		/** Button that removes the selected group from the chosen list. */
		private final JButton remove = new JButton("<<");
		/** Button that moves the selected group to the top of the list. */
		private final JButton upjumper = new JButton("Move bundle to top");
		/** Button that moves the selected group up one position. */
		private final JButton upshifter = new JButton("Move bundle up");
		/** Button that moves the selected group down one position. */
		private final JButton downshifter = new JButton("Move bundle down");
		/** Button that moves the selected group to the bottom of the list. */
		private final JButton downjumper =
				new JButton("Move bundle to bottom");

		/**
		 * Creates the pick-bit-groups form.
		 *
		 * @param group group being configured
		 * @param type element type label used for wording
		 */
		private RangesForm(Group group, String type) {

			super("Pick Bit Groups",
					type.equals("unbundle") ? "unbundle" : "bundle");
			this.group = group;
			this.type = type;
			cancelled = false;

			Container window = getContentPane();

			// set up selections
			for (int b = 0; b < group.getBits(); b += 1) {
				pick.addElement(new Group.Entry(new int[]{b}));
			}
			choose.setSelectedIndex(0);

			// set up automatic disabling of already-bundled bits
			choose.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					boolean allow = true;
					Group.Entry e = (Group.Entry) pick.get(index);
					if (e.isPicked()) {
						allow = false;
					}
					super.getListCellRendererComponent(list, value, index,
							isSelected && allow, cellHasFocus && allow);
					return this;
				}
			});

			// set up list display
			JPanel lists = new JPanel(new FlowLayout());

			JPanel leftList = new JPanel(new BorderLayout());
			JLabel left = new JLabel("choose from", SwingConstants.CENTER);
			leftList.add(left, BorderLayout.NORTH);
			JScrollPane chpane = new JScrollPane(choose);
			leftList.add(chpane, BorderLayout.CENTER);
			lists.add(leftList);

			JPanel buttons = new JPanel(new GridLayout(2, 1));
			buttons.add(add);
			buttons.add(remove);
			lists.add(buttons);

			JPanel rightList = new JPanel(new BorderLayout());
			JLabel right = new JLabel("chosen", SwingConstants.CENTER);
			rightList.add(right, BorderLayout.NORTH);
			chosen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane rpane = new JScrollPane(chosen);
			rightList.add(rpane, BorderLayout.CENTER);
			lists.add(rightList);

			JPanel shifters = new JPanel(new GridLayout(4, 1));
			shifters.add(upjumper);
			shifters.add(upshifter);
			shifters.add(downshifter);
			shifters.add(downjumper);
			lists.add(shifters);

			window.add(lists);

			// set up listeners
			add.addActionListener(this);
			remove.addActionListener(this);
			upjumper.addActionListener(this);
			upshifter.addActionListener(this);
			downshifter.addActionListener(this);
			downjumper.addActionListener(this);

			finishDialog();
		} // end of constructor

		@Override
		protected java.util.List<Violation> validateInputs() {

			if (picked.size() == 0) {
				return java.util.List.of(new Violation("No groups chosen",
						chosen));
			}
			return java.util.List.of();
		} // end of validateInputs method

		@Override
		protected void validateAndAccept() {

			// generate range entries
			for (int i = 0; i < picked.size(); i += 1) {
				Group.Entry e = (Group.Entry) (picked.elementAt(i));
				group.addRange(new Group.Entry(e.getValues()));
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == add) {

				// make sure no bit is already picked
				// (only can happen for a Bundler)

				int[] selected = choose.getSelectedIndices();
				for (int s : selected) {
					Group.Entry ent = (Group.Entry) (pick.elementAt(s));
					if (ent.isPicked()) {
						choose.removeSelectionInterval(s, s);
					}
				}

				selected = choose.getSelectedIndices();

				// Don't go indexing things that don't exist
				if (selected.length == 0) return;

				// add entry to the end of the list (user can resort it themselves)
				picked.addElement(new Group.Entry(selected));

				// mark chosen bits as picked if a bundler
				if (type.equals("Bundler")) {
					for (int p : selected) {
						Group.Entry ent = (Group.Entry) (pick.elementAt(p));
						ent.setPicked(true);
						pick.setElementAt(ent, p);
					}
				}

				// move selection to next unchosen bit
				int first = -1;
				int where = (selected[selected.length - 1] + 1) % pick.size();
				for (int i = 0; i < pick.size(); i += 1) {
					Group.Entry ent = (Group.Entry) (pick.elementAt(where));
					if (!ent.isPicked()) {
						first = where;
						break;
					}
					where = (where + 1) % pick.size();
				}
				if (first != -1) {
					choose.setSelectedIndex(first);
				}
				else {
					choose.clearSelection();
				}
			}

			else if (event.getSource() == remove) {

				// remove from chosen list (if something to remove)
				int where = chosen.getSelectedIndex();
				if (where < 0)
					return;
				Group.Entry ent = (Group.Entry) picked.elementAt(where);
				picked.removeElement(ent);

				for (int i : ent.getValues()) {
					Group.Entry e = (Group.Entry) pick.elementAt(i);
					e.setPicked(false);
					pick.setElementAt(e, i);
				}
			}
			else if (event.getSource() == upjumper) {
				int selected = chosen.getSelectedIndex();
				Group.Entry a = (Group.Entry) picked.getElementAt(selected);
				while (selected > 0) {
					Group.Entry b = (Group.Entry) picked.getElementAt(selected - 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected - 1);
					selected -= 1;
				}
				chosen.setSelectedIndex(0);
			}
			else if (event.getSource() == upshifter) {
				int selected = chosen.getSelectedIndex();
				if (selected > 0) {
					Group.Entry a = (Group.Entry) picked.getElementAt(selected);
					Group.Entry b = (Group.Entry) picked.getElementAt(selected - 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected - 1);
					chosen.setSelectedIndex(selected - 1);
				}
			}
			else if (event.getSource() == downshifter) {
				int selected = chosen.getSelectedIndex();
				if (selected >= 0 && selected < picked.getSize() - 1) {
					Group.Entry a = (Group.Entry) picked.getElementAt(selected);
					Group.Entry b = (Group.Entry) picked.getElementAt(selected + 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected + 1);
					chosen.setSelectedIndex(selected + 1);
				}
			}
			else if (event.getSource() == downjumper) {
				int selected = chosen.getSelectedIndex();
				Group.Entry a = (Group.Entry) picked.getElementAt(selected);
				while (selected >= 0 && selected < picked.getSize() - 1) {
					Group.Entry b = (Group.Entry) picked.getElementAt(selected + 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected + 1);
					selected += 1;
				}
				chosen.setSelectedIndex(picked.getSize() - 1);
			}
		} // end of actionPerformed method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of RangesForm class

} // end of GroupDialog class
