package jls.elem;

import jls.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

/**
 * Superclass of input and output pins.
 * Contains common data and methods.
 * 
 * @author David A. Poplawski
 */
public abstract class Pin extends LogicElement {
	
	// saved properties
	protected String name;
	protected int bits;
	protected boolean watched = false;
	protected JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// editting properties
	protected boolean cancelled;
	
	/**
	 * Create a new input pin.
	 * 
	 * @param circ The circuit this pin will be in.
	 */
	public Pin(Circuit circ) {
		
		super(circ);
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string.
	 */
	public String toString() {
		
		return name + ",bits=" + bits + ",watched=" + watched + ",hashCode=" + hashCode();
	} // end of toString method
	
	/**
	 * Get the name of this pin.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this pin.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Display dialog to get pin name and bits.
	 * 
	 * @param g The Graphics object to use to determine the name's size.
	 * @param editWindow The editor window this pin is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * @param type The type of pin ("Input" or "Output").
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y, String type) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new PinCreate(x+win.x,y+win.y,type);
		}
		else {
			new PinCreate(pos.x+win.x,pos.y+win.y,type);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// finish up
		init(g);
		
		// save position
		Point p = MouseInfo.getPointerInfo().getLocation();
		p.x -= win.x;
		p.y -= win.y;
		if (p != null) {
			super.setXY(p.x-width/2,p.y-height/2);
		}
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Computes the size of the pin as a function of the name.
	 * 
	 * @param g Graphics object used to compute the size of the name.
	 */
	public void init(Graphics g) {
		
		// set up size if needed
		if (g != null) {
			
			if (width == 0 && height == 0) {
				int s = JLSInfo.spacing;
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ");
				if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
				{
					width = Math.max((w+s/2)/s*s,2*s)+s;	// ceiling in spacings
					height = 2*s;
				}
				else
				{
					width = Math.max((w+s/2)/s*s,2*s);
					if(width % (2*s) != 0)
					{
						width += s;
					}
					height = 3*s;
				}
			}
		}
	} // end of init method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("bits")) {
			bits = value;
		} else if (name.equals("watch")) {
			if (value == 0)
				watched = false;
			else
				watched = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("name")) {
			this.name = value;
			circuit.addName(value);
		}
		else if (name.equals("orient")) {
			if(value.equals("LEFT"))
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Pins cannot be copied (copy/paste).
	 * 
	 * @return false.
	 */
	public boolean canCopy() {
		
		return false;
	} // end of canCopy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int bits " + bits);
		output.println(" int watch " + (watched ? 1 : 0));
		output.println(" String orient \"" + orientation.toString() + "\"");
		output.println("END");
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit input pin");
	} // end of showInfo method
	
	/**
	 * Remove this element.
	 * Take name out of circuit's list of names used.
	 */
	public void remove(Circuit circ) {
		
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Dialog box to set input pin characteristics.
	 */
	private class PinCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField nameField = new JTextField("",12);
		private JTextField bitsField = new JTextField("1",5);
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param type The type of pin ("Input" or "Output").
		 */
		private PinCreate(int x, int y, String type) {
			
			// set up window title
			super(JLSInfo.frame,"Create " + type + " Pin",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(2,1,1,5));
			JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
			labels.add(name);
			JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
			labels.add(bits);
			info.add(labels,BorderLayout.WEST);
			
			JPanel data = new JPanel(new GridLayout(2,1,1,5));
			data.add(nameField);
			JPanel bitsPanel = new JPanel(new BorderLayout());
			bitsPanel.add(bitsField,BorderLayout.CENTER);
			bitsPanel.add(bitsPad,BorderLayout.EAST);
			data.add(bitsPanel);
			info.add(data,BorderLayout.CENTER);
			window.add(info);
			
			//Setup orientation radio buttons
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(3,3));
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
			gr.add(down);
			gr.add(up);
			window.add(orients);
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,type,null);
			okCancel.add(help);
			window.add(okCancel);
			
			nameField.addActionListener(this);
			ok.addActionListener(this);
			cancel.addActionListener(this);
			
			// set up window close listener to cancel gate
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancel();
						}
					}
			);
			
			// finish up GUI
			pack();
			Dimension d = getSize();
			setLocation(x-d.width/2,y-d.height/2);
			setVisible(true);
		} // end of constructor
		
		/**
		 * React to events.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			// if ok button pushed or enter(return) typed in name field,
			// then check name for validity
			if (event.getSource() == ok || event.getSource() == nameField) {
				try {
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Bits not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bits < 1) {
					JOptionPane.showMessageDialog(this,
							"Must have at least 1 bit", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				String tname = nameField.getText().trim();
				if (tname.length() < 1 || !Util.isValidName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Missing or invalid name, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!circuit.addName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Name already used, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				name = tname;
				if(left.isSelected())
				{
					orientation = JLSInfo.Orientation.LEFT;
				}
				else if(right.isSelected())
				{
					orientation = JLSInfo.Orientation.RIGHT;
				}
				else if(up.isSelected())
				{
					orientation = JLSInfo.Orientation.UP;
				}
				else if(down.isSelected())
				{
					orientation = JLSInfo.Orientation.DOWN;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this pin.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of PinCreate class
	
	/**
	 * A pin be watched.
	 * 
	 * @return true.
	 */
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this pin is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this pin is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
} // end of Pin class
