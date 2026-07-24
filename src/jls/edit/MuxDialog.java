package jls.edit;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
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
import jls.elem.Mux;
import jls.util.Placement;

/**
 * GUI-side creation dialog for {@link Mux} (issue #77). Moved from the
 * former {@code Mux.MuxCreate} inner dialog + {@code Mux.setup}, so the
 * model stays headless. The form applies the chosen input count, bit
 * width, and output/selector orientations to the element through its
 * public setters, then sizes and positions it.
 */
public final class MuxDialog implements ElementDialog {

	private static final int DEFAULT_INPUTS = 2;
	private static final int DEFAULT_BITS = 1;

	@Override
	public boolean setup(Element el, Graphics g, JPanel editWindow,
			int x, int y) {

		Mux mux = (Mux) el;

		// show creation dialog
		Form form = new Form(mux);

		// don't do anything if user cancelled element
		if (form.cancelled) {
			return false;
		}

		// complete initialization
		mux.init(g);

		// save position
		Point p = Placement.dropPoint(editWindow, x, y,
				mux.getWidth(), mux.getHeight());
		mux.setXY(p.x, p.y);
		return true;
	} // end of setup method

	/**
	 * The create-multiplexor form. Extends the base element dialog; the
	 * output-orientation buttons switch which selector-orientation buttons
	 * are visible.
	 */
	@SuppressWarnings("serial")
	private static final class Form extends jls.elem.ElementDialog
			implements ActionListener {

		private final Mux mux;
		private boolean cancelled;
		private final JTextField inputsField =
				new JTextField(DEFAULT_INPUTS + "", 5);
		private final JTextField bitsField =
				new JTextField(DEFAULT_BITS + "", 5);
		private final KeyPad inputsPad =
				new KeyPad(inputsField, 10, DEFAULT_INPUTS, this);
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
		private final JLabel olbl2 = new JLabel("Selector Orientation");

		private Form(Mux mux) {

			// set up window title
			super("Create Multiplexor", "mux");
			this.mux = mux;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up input panel
			JPanel info = null;
			info = new JPanel(new GridLayout(2, 2));
			JLabel inputs = new JLabel("Inputs: ", SwingConstants.RIGHT);
			info.add(inputs);
			JPanel in = new JPanel(new FlowLayout());
			in.add(inputsField);
			in.add(inputsPad);
			info.add(in);

			JLabel gates;
			gates = new JLabel("Bits: ", SwingConstants.RIGHT);
			info.add(gates);
			JPanel ga = new JPanel(new FlowLayout());
			ga.add(bitsField);
			ga.add(bitsPad);
			info.add(ga);
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

			confirmOnEnter(inputsField);
			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		/**
		 * React to output orientation buttons.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if(event.getSource() == oLeft || event.getSource() == oRight)
			{
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
			}
			else if(event.getSource() == oUp || event.getSource() == oDown)
			{
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
			}
		} // end of actionPerformed method

		/**
		 * Validate the form and create the mux.
		 */
		@Override
		protected void validateAndAccept() {

			int ni;
			int b;
			try {
				ni = Integer.parseInt(inputsField.getText());
				b = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (ni < 2) {
				reject("Must have at least 2 inputs");
				return;
			}
			if (b < 1) {
				reject("Must be at least one bit");
				return;
			}
			mux.setNumInputs(ni);
			mux.setBits(b);
			if(this.oLeft.isSelected())
			{
				mux.setOutputOrientation(Orientation.LEFT);
				if(this.sUp.isSelected())
				{
					mux.setSelectorOrientation(Orientation.UP);
				}
				else if(this.sDown.isSelected())
				{
					mux.setSelectorOrientation(Orientation.DOWN);
				}
			}
			else if(this.oRight.isSelected())
			{
				mux.setOutputOrientation(Orientation.RIGHT);
				if(this.sUp.isSelected())
				{
					mux.setSelectorOrientation(Orientation.UP);
				}
				else if(this.sDown.isSelected())
				{
					mux.setSelectorOrientation(Orientation.DOWN);
				}
			}
			else if(this.oDown.isSelected())
			{
				mux.setOutputOrientation(Orientation.DOWN);
				if(this.sLeft.isSelected())
				{
					mux.setSelectorOrientation(Orientation.LEFT);
				}
				else if(this.sRight.isSelected())
				{
					mux.setSelectorOrientation(Orientation.RIGHT);
				}
			}
			else if(this.oUp.isSelected())
			{
				mux.setOutputOrientation(Orientation.UP);
				if(this.sLeft.isSelected())
				{
					mux.setSelectorOrientation(Orientation.LEFT);
				}
				else if(this.sRight.isSelected())
				{
					mux.setSelectorOrientation(Orientation.RIGHT);
				}
			}
			inputsPad.close();
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this mux.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			inputsPad.close();
			bitsPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of Form class

} // end of MuxDialog class
