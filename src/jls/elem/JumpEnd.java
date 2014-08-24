package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;

/**
 * Receiving end of a named wire.
 *  
 * @author David A. Poplawski
 */
public class JumpEnd extends LogicElement {
	
	// default value
	private static final int defaultBits = 1; 
	
	// saved properties
	private int bits = defaultBits;
	private String name;
	
	// running properties
	private boolean cancelled;
	private boolean loadTriState = false;
	
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	/**
	 * Create a new wire jump end.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public JumpEnd(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Create a new wire jump end, skipping the dialog.
	 * 
	 * @param circuit The circuit this element is part of.
	 * @param name The name of the wire jump start this end should match.
	 */
	public JumpEnd(Circuit circuit, String name) {
		
		super(circuit);
		this.name = name;
	}
	
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
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		
		if(name == null) {
			if (pos == null) {
				new EndCreate(x+win.x,y+win.y);
			}
			else {
				new EndCreate(pos.x+win.x,pos.y+win.y);
			}
		}
		else {
			bits = circuit.getJumpStart(name).getBits();
			if(circuit.getJumpStart(name).getOrientation() == JLSInfo.Orientation.LEFT)
				orientation = JLSInfo.Orientation.RIGHT;
			else
				orientation = JLSInfo.Orientation.LEFT;
			cancelled = false;
		}
		// don't do anything if user cancelled
		if (cancelled)
			return false;
		
		// complete initialization
		init(g);
		
		// set tri-state status
		JumpStart start = circuit.getJumpStart(name);
		Input in = start.getInput("input");
		if (in.isAttached()) {
			if (in.getWireEnd().getNet().isTriState()) {
				for (Output out : outputs) {
					out.setTriState(true);
				}
			}
		}
		
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
		
		// create output
		Output out;
		if(orientation == JLSInfo.Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}
		
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// highlight if corresponding start is selected
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpStart))
				continue;
			JumpStart jstart = (JumpStart)el;
			if (name.equals(jstart.getName())) {
				if (el.highlight) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(getRect());
				}
			}
		}
		
		// draw box
		int s = JLSInfo.spacing;
		int top = y-s/2;
		int bottom = y+s/2;
		
		if(orientation == JLSInfo.Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,top,x+width-s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x,top,s,s,-90,-180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width-s/2,y,x+width-s/4,y-s/4);
			g.drawLine(x+width-s/2,y,x+width-s/4,y+s/4);
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,top,x+s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x+width-s,top,s,s,-90,180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x+s/2,y,x+s/4,y-s/4);
			g.drawLine(x+s/2,y,x+s/4,y+s/4);
		}
		
		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == JLSInfo.Orientation.RIGHT)
			tx = x+s/2+(width-s-w)/2+JLSInfo.pointDiameter/2;
		else if(orientation == JLSInfo.Orientation.LEFT)
			tx = x+s/2+(width-2*s-w)/2+JLSInfo.pointDiameter/2;
		g.drawString(name,tx,y-h/2+ascent);
		
		// draw output
		outputs.get(0).draw(g);
		
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
		} else if (name.equals("tristate")) {
			loadTriState = true;
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
		
		output.println("ELEMENT JumpEnd");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int bits " + bits);
		output.println(" String orientation \"" + orientation + "\"");
		if (outputs.get(0).isTriState())
			output.println(" int tristate 1");
		output.println("END");
	} // end of save method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		JumpEnd it = new JumpEnd(circuit);
		it.name = name;
		it.bits = bits;
		it.outputs.add(outputs.get(0).copy(it));
		it.orientation = orientation;
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Get the name of this jump end.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit wire connection, value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method

	/**
	 * Set this element to tri-state or not.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !(outputs.get(0).isAttached());
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

		outputs.clear();
		
		// create output
		Output out;
		if(orientation == JLSInfo.Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}
	}
	
	/**
	 * Dialog box to set jump end characteristics.
	 */
	private class EndCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JList starts;
		private JRadioButton left = new JRadioButton("left");
		private JRadioButton right = new JRadioButton("right");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param type The type of pin ("Input" or "Output").
		 */
		private EndCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Wire End",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up jumpstart name list
			JLabel heading = new JLabel("Select Wire Name",SwingConstants.CENTER);
			heading.setAlignmentX((float)0.5);
			window.add(heading);

			starts = new JList<String>((String[]) circuit.getJumpStartNames().toArray());
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
				JLSInfo.hb.enableHelpOnButton(help,"end",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
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
			
			// if ok button pushed...
			if (event.getSource() == ok) {
				if (starts.getSelectedIndex() < 0) {
					JOptionPane.showMessageDialog(this,
							"Nothing selected", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				name = (String)starts.getSelectedValue();
				bits = circuit.getJumpStart(name).getBits();
				if (right.isSelected()) {
					orientation = JLSInfo.Orientation.RIGHT;
				}
				else {
					orientation = JLSInfo.Orientation.LEFT;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this jump end.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of EndCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet currentValue = new BitSet();
	
	/**
	 * Initialize this element by setting its output to 0.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set output to 0 or off
		Output out = outputs.get(0);
		if (out.isTriState()) {
			currentValue = null;
			out.setValue(null);
		}
		else {
			currentValue = new BitSet();
			BitSet bitval = new BitSet(1);
			out.setValue(bitval);
		}
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo The value to send along.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// get the input value
		BitSet value = (BitSet)todo;
		currentValue = null;
		if (value != null)
			currentValue = (BitSet)value.clone();
		
		// send to output
		Output out = outputs.get(0);
		if (value == null)
			out.propagate(null,now,sim);
		else
			out.propagate((BitSet)value.clone(),now,sim);
	
	} // end of react method

} // end of JumpEnd method
