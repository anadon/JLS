package jls.edit;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.TellUser;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.WireEnd;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link JumpEnd} (issue #77). Moved from the
 * former {@code JumpEnd.EndCreate} inner dialog + {@code JumpEnd.setup}, so
 * the model stays headless. Keeps the #131 fail-fast when no named wires
 * exist, the editor's match-gesture preset-name bypass (name already set,
 * no dialog), and the tri-state propagation from the matching jump start's
 * net. Applies the selected name/bits/orientation through the element's
 * public setters, then sizes and positions it.
 */
public final class JumpEndDialog implements ElementDialog {

	/**
	 * Creates a {@code JumpEndDialog}.
	 */
	public JumpEndDialog() {
	}

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		JumpEnd end = (JumpEnd) el;
		Circuit circuit = end.getCircuit();

		// show creation dialog
		boolean cancelled;
		if (end.getName() == null) {

			// fail fast when there is nothing to connect to: the selection
			// dialog would show an empty list and could never be completed
			// (#131; prior art bsiever/JLS@26053a00)
			if (circuit.getJumpStartNames().isEmpty()) {
				TellUser.error(editWindow,JumpEnd.NO_NAMED_WIRES,"Error");
				return false;
			}
			Form form = new Form(end);
			cancelled = form.cancelled;
		}
		else {
			JumpStart preset = circuit.getJumpStart(end.getName());
			if (preset == null) {
				throw new IllegalStateException(
						"no jump start named " + end.getName());
			}
			end.setBits(preset.getBits());
			if (preset.getOrientation() == Orientation.LEFT)
				end.setOrientation(Orientation.RIGHT);
			else
				end.setOrientation(Orientation.LEFT);
			cancelled = false;
		}
		// don't do anything if user cancelled
		if (cancelled)
			return false;

		// complete initialization
		end.init(SwingTextMetrics.forGraphics(g));

		// set tri-state status
		JumpStart start = circuit.getJumpStart(end.getName());
		if (start == null) {
			throw new IllegalStateException(
					"no jump start named " + end.getName());
		}
		Input in = start.getInput("input");
		if (in.isAttached()) {
			// isAttached() guarantees a non-null wire end
			WireEnd wireEnd = in.getWireEnd();
			if (wireEnd == null) {
				throw new IllegalStateException(
						"attached input has no wire end");
			}
			if (wireEnd.getNet().isTriState()) {
				end.setTriState(true);
			}
		}

		// save position
		Point p = Placement.dropPoint(editWindow,x,y,
				end.getWidth(),end.getHeight());
		end.setXY(p.x,p.y);

		return true;
	} // end of setup method

	/**
	 * The create-wire-end form. Extends the base element dialog and applies
	 * the selected name/bits/orientation to the jump end.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog {

		/** The jump end being configured by this form. */
		private final JumpEnd end;
		/** The circuit containing the jump end, used to look up jump starts. */
		private final Circuit circuit;
		/** Whether the user cancelled the dialog. */
		private boolean cancelled;
		/** List of named wire (jump start) names to pick from. */
		private JList<String> starts;
		/** Selects leftward orientation. */
		private final JRadioButton left = new JRadioButton("left");
		/** Selects rightward orientation. */
		private final JRadioButton right = new JRadioButton("right");

		/**
		 * Creates the create-wire-end form for the given jump end.
		 *
		 * @param end the jump end to configure
		 */
		private Form(JumpEnd end) {

			// set up window title
			super("Create Wire End","end");
			this.end = end;
			this.circuit = end.getCircuit();

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up jumpstart name list
			JLabel heading = new JLabel("Select Wire Name",SwingConstants.CENTER);
			heading.setAlignmentX((float)0.5);
			window.add(heading);

			starts = new JList<String>(circuit.getJumpStartNames().toArray(new String[0]));
			starts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			starts.setVisibleRowCount(Math.min(circuit.getJumpStartNames().size(),10));
			JScrollPane pane = new JScrollPane(starts);
			window.add(pane);

			// highlight name if there is only one
			if (circuit.getJumpStartNames().size() == 1) {
				starts.setSelectedIndex(0);
			}

			// set up orientation panel
			window.add(new JLabel(" "));
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(1,3));

			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(left);
			group.add(right);
			right.setSelected(true);
			window.add(orients);

			finishDialog();
		} // end of constructor

		@Override
		protected void validateAndAccept() {

			if (starts.getSelectedIndex() < 0) {
				reject("Nothing selected");
				return;
			}
			String name = (String)starts.getSelectedValue();
			end.setName(name);
			JumpStart jumpStart = circuit.getJumpStart(name);
			if (jumpStart == null) {
				throw new IllegalStateException(
						"no jump start named " + name);
			}
			end.setBits(jumpStart.getBits());
			if (right.isSelected()) {
				end.setOrientation(Orientation.RIGHT);
			}
			else {
				end.setOrientation(Orientation.LEFT);
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of JumpEndDialog class
