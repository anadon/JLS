package jls.elem;

import jls.*;
import jls.elem.Gate.Orientation;
import jls.sim.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import java.io.PrintWriter;
import java.util.*;

/**
 * Starting point of a named wire.
 * 
 * @author David A. Poplawski
 */
public class JumpStart extends LogicElement implements TriProp {
	
	// default value
	private static final int defaultBits = 1; 
	
	// saved properties
	private String name;
	private int bits = defaultBits;
	private boolean watched = false;
	
	// running properties
	private boolean cancelled;
	private JumpStart me;
	
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.LEFT;
	
	/**
	 * Create a new adder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public JumpStart(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Display dialog to get characteristics.
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
		me = this;
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new StartCreate(x+win.x,y+win.y);
		}
		else {
			new StartCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled
		if (cancelled)
			return false;
		
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
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {
		
		if (g != null) {
			
			if (width == 0 && height == 0) {
				
				// set up size
				int s = JLSInfo.spacing;
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ")+s;
				width = Math.max((w+s/2)/s*s,2*s);	// ceiling in spacings
				height = 0;	// not really, but bounding rectangle will be large enough
			}
			
		}
		
		// create input
		if(orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}
		
		// save name in jumpstart list in this circuit
		circuit.addJumpStart(name,this);
		
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// highlight if corresponding end is selected
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd))
				continue;
			JumpEnd jend = (JumpEnd)el;
			if (name.equals(jend.getName())) {
				if (el.highlight) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(getRect());
				}
			}
		}
		
		// set up corners
		int s = JLSInfo.spacing;
		int top = y-s/2;
		int bottom = y+s/2;
		
		// draw watched background
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillRect(x, top, width-s, bottom-top);
		}
		
		// draw box
		if(orientation == JLSInfo.Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x,top,x,bottom);
			g.drawLine(x,top,x+width-s,top);
			g.drawLine(x,bottom,x+width-s,bottom);
			g.drawArc(x+width-3*s/2,top,s,s,-90,180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width,y,x+width-s/4,y-s/4);
			g.drawLine(x+width,y,x+width-s/4,y+s/4);
		}
		else if(orientation == JLSInfo.Orientation.RIGHT){
			g.setColor(Color.BLACK);
			g.drawLine(x+width,top,x+width,bottom);
			g.drawLine(x+s,top,x+width,top);
			g.drawLine(x+s,bottom,x+width,bottom);
			g.drawArc(x+s/2,top,s,s,-90,-180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x,y,x+s/4,y-s/4);
			g.drawLine(x,y,x+s/4,y+s/4);
		}
		
		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == JLSInfo.Orientation.LEFT)
			tx = x+(width-s-w)/2+JLSInfo.pointDiameter/2;
		else 
			tx = x+(width+0-w)/2+JLSInfo.pointDiameter/2;
		g.drawString(name,tx,y-h/2+ascent);
		
		// draw input
		inputs.get(0).draw(g);
		
	} // end of draw method
	
	/**
	 * Get the rectangle bounding this element.
	 * 
	 * @return the rectangle bounding this element.
	 */
	public Rectangle getRect() {
		
		return new Rectangle(x,y-JLSInfo.spacing/2,width,height+JLSInfo.spacing);
	} // end of getRect method
	
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
	 * Set a string instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("name")) {
			this.name = value;
			circuit.addName(value);
		} else if(name.equals("orientation")) {
			orientation = JLSInfo.Orientation.valueOf(value);
		}else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT JumpStart");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int bits " + bits);
		output.println(" int watch " + (watched ? 1 : 0));
		output.println(" String orientation \"" + orientation + "\"");
		output.println("END");
	} // end of save method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		JumpStart it = new JumpStart(circuit);
		it.name = name;
		it.bits = bits;
		it.watched = watched;
		it.orientation = orientation;
		it.inputs.add(inputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Get the name of this jumpstart.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the direction of this jumpstart.
	 * 
	 * @return orientation
	 */
	public JLSInfo.Orientation getOrientation() {
		return orientation;
	}
	
	/**
	 * Get the number of bits in this element.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit wire name, value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method
	
	/**
	 * Remove this element and all jump ends with the same name.
	 
	 * @param circ A reference back to the circuit the element is in.
	 */
	public void remove(Circuit circ) {
		
		// remove from list of jump starts and list of names in circuit
		circuit.removeName(name);
		circ.removeJumpStart(name);
		
		// remove corresonding jump ends
		Set<Element> rems = new HashSet<Element>();
		for (Element el : circ.getElements()) {
			if (el instanceof JumpEnd) {
				JumpEnd end = (JumpEnd)el;
				if (name.equals(end.getName()))
					rems.add(el);
			}
		}
		for (Element el : rems) {
			el.remove(circ);
		}
		
		// remove itself
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached());
	}
	
	/**
	 * This method will flip an adder
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

		inputs.clear();
		
		if(orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}
	}

	/**
	 * A jump start can be watched.
	 * 
	 * @return true.
	 */
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this jumpstart is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this jumpstart is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
	/**
	 * Dialog box to set input pin characteristics.
	 */
	private class StartCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField nameField = new JTextField("",12);
		private JTextField bitsField = new JTextField("1",5);
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		private JRadioButton left = new JRadioButton("left");
		private JRadioButton right = new JRadioButton("right");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private StartCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Wire Start",true);
			
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
			left.setSelected(true);
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
				JLSInfo.hb.enableHelpOnButton(help,"start",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
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
				String tname = nameField.getText();
				if (!Util.isValidName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Invalid name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Bits not numeric", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bits < 1) {
					JOptionPane.showMessageDialog(this,
							"Must have at least 1 bit", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!circuit.addName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Name already used", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (right.isSelected()) {
					orientation = JLSInfo.Orientation.RIGHT;
				}
				else {
					orientation = JLSInfo.Orientation.LEFT;
				}
				circuit.addJumpStart(tname,me);
				name = tname;
				bitsPad.close();
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this element.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of StartCreate class

	/**
	 * Set this element to tri-state or not.
	 * Propagate tri-state property to the other end(s) of the jump.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd))
				continue;
			JumpEnd jend = (JumpEnd)el;
			if (getName().equals(jend.getName()))
				jend.setTriState(which);
		}
	} // end of setTriState method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet currentValue = new BitSet();
	private Set<JumpEnd> jumpEnds = new HashSet<JumpEnd>();
	
	/**
	 * Get the current value.
	 * 
	 * @return the current value.
	 */
	public BitSet getCurrentValue() {
		
		if (currentValue == null)
			return null;
		else
			return (BitSet)currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Initialize this element by creating a set of all matching jump ends
	 * and setting the current value to 0.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// create set of matching jump ends
		jumpEnds.clear();
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd))
				continue;
			JumpEnd jend = (JumpEnd)el;
			if (name.equals(jend.getName())) {
				jumpEnds.add(jend);
			}
		}
		
		// set current value to 0
		currentValue = new BitSet();
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		
		// get the input value
		currentValue = inputs.get(0).getValue();
		if (currentValue != null)
			currentValue = (BitSet)currentValue.clone();
		
		// send to all matching jump ends
		for (JumpEnd jend : jumpEnds) {
			BitSet newValue = null;
			if (currentValue != null)
				newValue = (BitSet)currentValue.clone();
			sim.post(new SimEvent(now,jend,newValue));
		}
	} // end of react method
	
} // end of JumpStart class
