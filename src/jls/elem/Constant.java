package jls.elem;

import jls.*;
import jls.edit.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;

import java.math.*;
import java.util.*;

/**
 * Constant output value.
 * 
 * @author David A. Poplawski
 */
public class Constant extends LogicElement implements ActionListener {
	
	// named constants
	private static final BigInteger defaultValue = BigInteger.ZERO;
	private static final int defaultBase = 10;
	
	// saved properties
	private BigInteger value = defaultValue;
	private int base = defaultBase;
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// running properties
	private static BigInteger previousValue = defaultValue;
	private static int previousBase = defaultBase;
	private static JLSInfo.Orientation previousOrientation = JLSInfo.Orientation.RIGHT;
	private boolean cancelled;
	private boolean changed;
	
	/**
	 * Create a new constant element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Constant(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Display dialog to get value and bits.
	 * 
	 * @param g The Graphics object to use to initialize sizes
	 * @param editWindow The editor window this constant will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new ConstantCreate(x+win.x,y+win.y);
		}
		else {
			new ConstantCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// save values for next create
		previousValue = value;
		previousBase = base;
		previousOrientation = orientation;
		
		// complete initialization
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
	 * Figures out height and width using font info from graphics object.
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {
		
		int s = JLSInfo.spacing;
		// set up size if there is a graphics object
		if (g != null) {
			
			// if element already has a size, use it
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(Util.convert(value,base,true))+s;
				width = Math.max((w+s/2)/s*s,2*s);	// ceiling in spacings
				if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
				{
					height = 0;	// not really, but bounding rectangle will be large enough
				}
				else
				{
					height = 2*s;
				}
			}
			
		}
		// create output
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			outputs.add(new Output("output",this,0,0,0));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			outputs.add(new Output("output",this,width,0,0));
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			outputs.add(new Output("output",this,width/2,0,0));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			outputs.add(new Output("output",this,width/2,height,0));
		}
	} // end of init method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw box
		Graphics2D gg = (Graphics2D)g;
		Rectangle r = getRect();
		gg.setColor(Color.BLACK);
		Rectangle drawn = new Rectangle(r.x,r.y,r.width,r.height);
		gg.draw(drawn);
		
		// draw value inside box
		FontMetrics fm = g.getFontMetrics();
		String str = Util.convert(value,base,true);
		Rectangle2D t = fm.getStringBounds(str,g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int dx = (int)((r.width-tw)/2);
		int dy = (int)((r.height-th)/2+fm.getAscent());
		if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
		{
			g.drawString(str,x+dx,y-JLSInfo.spacing/2+dy);
		}
		else
		{
			g.drawString(str,x+dx,y+dy);
		}
		
		// draw output
		outputs.get(0).draw(g);
		
	} // end of draw method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("base")) {
			base = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Set a long instance variable value (during a load).
	 * This is here for compatibility with old versions of JLS that saved the value
	 * of the constant as a long, not as a String (i.e., BigInteger).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, long value) {
		
		if (name.equals("value")) {
			this.value = BigInteger.valueOf(value);
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Set a BigInteger instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, BigInteger value) {
		
		if (name.equals("value")) {
			this.value = value;
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
		
		if (name.equals("orient")) {
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
	 * Copy this element.
	 */
	public Element copy() {
		
		Constant it = new Constant(circuit);
		it.value = value.add(BigInteger.ZERO);
		it.base = base;
		it.orientation = orientation;
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Constant");
		super.save(output);
		output.println(" Int value " + value.toString());
		output.println(" int base " + base);
		output.println(" String orient \"" + orientation.toString() + "\"");
		output.println("END");
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText("a constant value");
	} // end of showInfo method
	
	/**
	 * Get the rectangle bounding this element.
	 * 
	 * @return the rectangle bounding this element.
	 */
	public Rectangle getRect() {
		if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
		{
			return new Rectangle(x,y-JLSInfo.spacing/2,width,height+JLSInfo.spacing);
		}
		else
			return new Rectangle(x,y,width,height);
			
	} // end of getRect method
	
	/**
	 * Set values to those from previous constant element created.
	 */
	public void setToPrevious() {
		
		value = previousValue;
		base = previousBase;
		orientation = previousOrientation;
	} // end of setToPrevious method
	
	/**
	 * Tells if a constant is capable of rotatating, can only rotate when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 *  This method will rotate the constant if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
		}
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a constant is capable of flipping, can only flip when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 * This method will flip a constant
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			orientation = JLSInfo.Orientation.LEFT;
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			orientation = JLSInfo.Orientation.DOWN;
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			orientation = JLSInfo.Orientation.UP;
		}
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Dialog box to set multi-input gate parameters (number of inputs, number of gates).
	 * Used by all simple gates (nand, and, nor, or, xor, not).
	 */
	private class ConstantCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton repeat;
		private JButton cancel = new JButton("Cancel");
		private JTextField valueField = new JTextField(defaultValue+"",defaultBase);
		private KeyPad valuePad = new KeyPad(valueField,16,defaultValue.longValue(),this);
		private JRadioButton base2 = new JRadioButton("2");
		private JRadioButton base10 = new JRadioButton("10");
		private JRadioButton base16 = new JRadioButton("16");
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private ConstantCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Constant",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel inputs = new JLabel("Value: ",SwingConstants.RIGHT);
			info.add(inputs,BorderLayout.WEST);
			info.add(valueField,BorderLayout.CENTER);
			info.add(valuePad,BorderLayout.EAST);
			valuePad.setBase(10);
			window.add(info);
			
			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1,3));
			bases.add(base2);
			bases.add(base10);
			bases.add(base16);
			base2.setHorizontalAlignment(SwingConstants.CENTER);
			base10.setHorizontalAlignment(SwingConstants.CENTER);
			base16.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(base2);
			group.add(base10);
			group.add(base16);
			base10.setSelected(true);
			window.add(bases);
			
			// set up repeat
			window.add(new JLabel(" "));
			JPanel rep = new JPanel();
			repeat = new JButton("Repeat Previous Value");
			repeat.setBackground(Color.yellow);
			rep.add(repeat);
			window.add(rep);
			
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
				JLSInfo.hb.enableHelpOnButton(help,"const",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			valueField.addActionListener(this);
			repeat.addActionListener(this);
			cancel.addActionListener(this);
			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);
			
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
		 * React to ok, reset and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok || event.getSource() == valueField) {
				try {
					value = new BigInteger(valueField.getText(),base);
					//bits = Integer.parseInt(bitsField.getText());
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
						
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				dispose();
			}
			else if (event.getSource() == repeat) {
				setToPrevious();
				base2.setSelected(false);
				base10.setSelected(false);
				base16.setSelected(false);
				if (base == 2)
					base2.setSelected(true);
				else if (base == 10)
					base10.setSelected(true);
				else {
					base16.setSelected(true);
				}
				valueField.setText(value.toString(base));
				valuePad.setBase(base);
				valuePad.reset();
				System.out.println(orientation);
				if (orientation == JLSInfo.Orientation.LEFT)
					left.setSelected(true);
				else if (orientation == JLSInfo.Orientation.RIGHT)
					right.setSelected(true);
				else if (orientation == JLSInfo.Orientation.UP)
					up.setSelected(true);
				else
					down.setSelected(true);
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 2;
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			}
			else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 10;
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			}
			else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 16;
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method
		
		/**
		 * Cancel this gate.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of ConstantCreate class
	
	/**
	 * Constants can be changed.
	 * 
	 * @return true.
	 */
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	/**
	 * Show change dialog.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return false.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
	
		// save g for valueFits method
		saveg = g;
		
		// display dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new ConstantChange(x+win.x,y+win.y);
		}
		else {
			new ConstantChange(pos.x+win.x,pos.y+win.y);
		}
		
		// no change if cancelled
		if (cancelled)
			return false;
		
		// record a change to the circuit
		circuit.markChanged();
		
		// if bigger, detach and resize
		if (changed) {
			detach();
			width = 0;
			height = 0;
			init(g);
			return true;
		}
		
		// no need to reposition
		return false;
	
	} // end of change method
	
	private Graphics saveg;	// saved by change method, used by valueFits method
	
	/**
	 * See if the given value fits in the box on the screen.
	 * 
	 * @param value The value to check.
	 * 
	 * @return true if it fits, false if not.
	 */
	public boolean valueFits(String value) {
		
		int s = JLSInfo.spacing;
		FontMetrics fm = saveg.getFontMetrics();
		int w = fm.stringWidth(value)+s;
		int rw = Math.max((w+s/2)/s*s,2*s);
		return rw <= width;
		
	} // end of valueFits method
	
	/**
	 * Display dialog letting user change the value of the constant.
	 * New value must fit in current element.
	 */
	private class ConstantChange extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField valueField = new JTextField(Util.convert(value,base,false),10);
		private KeyPad valuePad = new KeyPad(valueField,16,defaultValue.longValue(),this);
		private JRadioButton base2 = new JRadioButton("2");
		private JRadioButton base10 = new JRadioButton("10");
		private JRadioButton base16 = new JRadioButton("16");
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private ConstantChange(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Change Constant",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel inputs = new JLabel("Value: ",SwingConstants.RIGHT);
			info.add(inputs,BorderLayout.WEST);
			info.add(valueField,BorderLayout.CENTER);
			valueField.setText(value.toString(base));
			valuePad.setBase(base);
			info.add(valuePad,BorderLayout.EAST);
			window.add(info);
			
			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1,3));
			bases.add(base2);
			bases.add(base10);
			bases.add(base16);
			if (base == 2)
				base2.setSelected(true);
			else if (base == 10)
				base10.setSelected(true);
			else if (base == 16)
				base16.setSelected(true);
			valuePad.setBase(base);
			ButtonGroup group = new ButtonGroup();
			group.add(base2);
			group.add(base10);
			group.add(base16);
			window.add(bases);
			
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
				JLSInfo.hb.enableHelpOnButton(help,"const",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			valueField.addActionListener(this);
			cancel.addActionListener(this);
			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);
			
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
		 * React to ok, reset and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			// if ok, check new value and set
			if (event.getSource() == ok || event.getSource() == valueField) {
				
				// get base
				if (base2.isSelected())
					base = 2;
				else if (base10.isSelected())
					base = 10;
				else
					base = 16;
				
				// get value
				BigInteger temp = BigInteger.ZERO;
				try {
					temp = new BigInteger(valueField.getText(),base);
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// cancel if the new value is the same as the old value
				if (temp == value) {
					cancel();
				}
				value = temp;
				
				// decide if the element must be redrawn
				if (!valueFits(Util.convert(temp,base,true))) {
					changed = true;
				}
				else {
					changed = false;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 2;
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			}
			else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 10;
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			}
			else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 16;
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method

		/**
		 * Cancel this gate.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of ConstantChange class
	
	/**
	 * This element has a quick change ability.
	 * 
	 * @return true.
	 */
	public boolean quickChange() {
		
		return true;
	} // end of quickChange method
	
	private SimpleEditor sed;
	private JMenu quick = new JMenu("shortcuts");
	private JMenuItem zero = new JMenuItem("0");
	private JMenuItem one = new JMenuItem("1");
	private JMenuItem plus = new JMenuItem("add 1");
	private JMenuItem minus = new JMenuItem("subtract 1");
	private boolean menuMade = false;
	
	/**
	 * Create menu and submenus for quick changes to the constant value.
	 * 
	 * @return menu.
	 */
	public JMenuItem setupQuickMenu(SimpleEditor sed) {
		
		if (menuMade)
			return quick;
		this.sed = sed;
		menuMade = true;
		quick.add(zero);
		quick.add(one);
		quick.add(plus);
		quick.add(minus);
		zero.addActionListener(this);
		one.addActionListener(this);
		plus.addActionListener(this);
		minus.addActionListener(this);
		return quick;
	} // end of setupQuickChange method
	
	/**
	 * Respond to quick change button pushes.
	 * 
	 * @param event The event object for the button push.
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (event.getSource() == zero) {
			value = BigInteger.ZERO;
		}
		else if (event.getSource() == one) {
			value = BigInteger.ONE;
		}
		else if (event.getSource() == plus) {
			value = value.add(BigInteger.ONE);
		}
		else if (event.getSource() == minus) {
			value = value.subtract(BigInteger.ONE);
			value = value.max(BigInteger.ZERO);
		}
		Editor ed = getCircuit().getEditor();
		if (ed != null) {
			sed.quickReset();
			ed.repaint();
		}
	} // end of actionPerformed method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by generating an output and sending it.
	 * 
	 * @param sim The simulator to post events to.
	 */
	public void initSim(Simulator sim) {
		
		// create value
		BitSet bitval = BitSetUtils.Create(value);
		
		// set output so propagate will work
		BitSet opposite = (BitSet)bitval.clone();
		opposite.flip(0);	// at least one bit different
		Output out = outputs.get(0);
		out.setValue(opposite);
		
		// post output event
		sim.post(new SimEvent(0,this,bitval));
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// get the new output value
		BitSet newValue = (BitSet)todo;
		
		// send correct number of bits to output
		Output out = (Output)(outputs.toArray()[0]);
		if (!out.isAttached())
			return;
		int bits = out.getWireEnd().getBits();
		BitSet mask = new BitSet(bits);
		mask.set(0,bits);
		newValue.and(mask);
		out.propagate(newValue,now,sim);
		
	} // end of react method
	
} // end of Constant class
