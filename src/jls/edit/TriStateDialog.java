package jls.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.KeyPad;
import jls.core.Orientation;
import jls.elem.Element;
import jls.elem.TriState;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link TriState} (issue #77). Moved from
 * the former {@code TriState.TriStateCreate} inner dialog +
 * {@code TriState.setup}, so the model stays headless. The form applies
 * the chosen bit width and gate/control orientations to the element
 * through its public setters, then sizes and positions it.
 */
public final class TriStateDialog implements ElementDialog {

	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		TriState ts = (TriState) el;

		// show creation dialog
		Form form = new Form(ts);

		// don't do anything if user cancelled
		if (form.cancelled) {
			return false;
		}

		// set up inputs and outputs, then place the element
		ts.init(g);
		Point p = Placement.dropPoint(editWindow, x, y,
				ts.getWidth(), ts.getHeight());
		ts.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-tristate form. Extends the base element dialog; the
	 * output-orientation buttons switch which control-orientation buttons
	 * are visible.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends ElementFormDialog
			implements ActionListener {

		private final TriState ts;
		private boolean cancelled;
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 10);
		private final KeyPad bitsPad =
				new KeyPad(bitsField, 10, DEFAULT_BITS, this);
		private final JRadioButton oLeft = new JRadioButton("Left");
		private final JRadioButton oRight = new JRadioButton("Right", true);
		private final JRadioButton oUp = new JRadioButton("Up");
		private final JRadioButton oDown = new JRadioButton("Down");
		private final JRadioButton sLeft = new JRadioButton("Left");
		private final JRadioButton sRight = new JRadioButton("Right");
		private final JRadioButton sUp = new JRadioButton("Up");
		private final JRadioButton sDown = new JRadioButton("Down", true);
		private final JLabel olbl2 = new JLabel("Control Orientation");

		private Form(TriState ts) {

			super("Create TriState", "TRISTATE");
			this.ts = ts;
			cancelled = false;

			Container window = getContentPane();

			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Gates (bits): ", SwingConstants.RIGHT);
			info.add(bits, BorderLayout.WEST);
			info.add(bitsField, BorderLayout.CENTER);
			info.add(bitsPad, BorderLayout.EAST);
			window.add(info);

			JPanel orient = new JPanel(new GridLayout(3, 3));
			JPanel orient2 = new JPanel(new GridLayout(3, 3));
			ButtonGroup gr = new ButtonGroup();
			ButtonGroup gr2 = new ButtonGroup();
			gr.add(this.oLeft);
			gr.add(this.oRight);
			gr.add(this.oDown);
			gr.add(this.oUp);
			gr2.add(this.sDown);
			gr2.add(this.sUp);
			gr2.add(this.sLeft);
			gr2.add(this.sRight);
			orient.add(new JLabel(""));
			orient.add(this.oUp);
			orient.add(new JLabel(""));
			orient.add(this.oLeft);
			orient.add(new JLabel(""));
			orient.add(this.oRight);
			orient.add(new JLabel(""));
			orient.add(this.oDown);
			orient.add(new JLabel(""));

			orient2.add(new JLabel(""));
			orient2.add(this.sUp);
			orient2.add(new JLabel(""));
			orient2.add(this.sLeft);
			orient2.add(new JLabel(""));
			orient2.add(this.sRight);
			orient2.add(new JLabel(""));
			orient2.add(this.sDown);
			orient2.add(new JLabel(""));

			JLabel olbl = new JLabel("Output Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			olbl2.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl2);
			window.add(orient2);

			sLeft.setVisible(false);
			sRight.setVisible(false);

			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == oLeft || event.getSource() == oRight) {
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
			} else if (event.getSource() == oUp || event.getSource() == oDown) {
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
			}
		} // end of actionPerformed method

		@Override
		protected void validateAndAccept() {

			int bits;
			try {
				bits = Integer.parseInt(bitsField.getText());
			} catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must be at least 1 bit");
				return;
			}
			ts.setBits(bits);
			if (this.oLeft.isSelected()) {
				ts.setGateOrientation(Orientation.LEFT);
				if (this.sUp.isSelected()) {
					ts.setControlOrientation(Orientation.UP);
				} else if (this.sDown.isSelected()) {
					ts.setControlOrientation(Orientation.DOWN);
				}
			} else if (this.oRight.isSelected()) {
				ts.setGateOrientation(Orientation.RIGHT);
				if (this.sUp.isSelected()) {
					ts.setControlOrientation(Orientation.UP);
				} else if (this.sDown.isSelected()) {
					ts.setControlOrientation(Orientation.DOWN);
				}
			} else if (this.oDown.isSelected()) {
				ts.setGateOrientation(Orientation.DOWN);
				if (this.sLeft.isSelected()) {
					ts.setControlOrientation(Orientation.LEFT);
				} else if (this.sRight.isSelected()) {
					ts.setControlOrientation(Orientation.RIGHT);
				}
			} else if (this.oUp.isSelected()) {
				ts.setGateOrientation(Orientation.UP);
				if (this.sLeft.isSelected()) {
					ts.setControlOrientation(Orientation.LEFT);
				} else if (this.sRight.isSelected()) {
					ts.setControlOrientation(Orientation.RIGHT);
				}
			}
			dispose();
		} // end of validateAndAccept method

		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of TriStateDialog class
