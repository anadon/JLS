package jls.elem;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.math.*;
import jls.*;
import jls.sim.*;

/**
 * Signal generator.
 * 
 * @author David A. Poplawski
 */
public class SigGen extends SigSim {
	
	// named constants
	private final String title = " Signal Generator ";
	private final int size = 300;	// width and height of dialog
	
	// saved properties
	private String signals = "";
	
	// running properties
	private boolean cancelled = false;

	/**
	 * Create new element.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public SigGen(Circuit circuit) {
		
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
	 * @return false if canceled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// make sure there isn't a signal generator in the circuit already
		for (Element el : circuit.getElements()) {
			if (el instanceof SigGen) {
				JOptionPane.showMessageDialog(JLSInfo.frame,
						"Only one signal generator per circuit", "Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new EditSignals(x+win.x,y+win.y,true);
		}
		else {
			new EditSignals(pos.x+win.x,pos.y+win.y,true);
		}
		
		// don't do anything if user canceled element
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
	 * Initialize this element.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		// do nothing if no graphics object
		if (g == null)
			return;
		
		// do nothing if element already has a size
		if (width != 0 || height != 0)
			return;
		
		int s = JLSInfo.spacing;
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(title);
		width = (w+s-1)/s*s;
		height = 2*s;
	} // end of init method

	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		
		// draw title
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		int w = fm.stringWidth(title);
		g.drawString(title,x+(width-w)/2,y+(height-hi)/2+ascent);
		
	} // end of draw method
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT SigGen");
		super.save(output);
		String str = signals.replace("\\","\\\\");
		str = str.replace("\"","\\\"");
		str = str.replace("\n","\\n");
		output.println(" String signals \"" + str + "\"");
		output.println("END");
	} // end of save method

	/**
	 * Set a string instance variable value (during a load).
	 * 
	 * @param name The name of the instance variable.
	 * @param value The value to set it to.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("signals")) {
			signals = value;
		}
		super.setValue(name,value);
	} // end of setValue method

	/**
	 * Make a copy of this element.
	 * 
	 * @return an exact copy of this element.
	 */
	public SigGen copy() {
		
		SigGen it = new SigGen(circuit);
		super.copy(it);
		it.signals = signals;
		return it;
	} // end of copy method

	/**
	 * Signal generators can be changed.
	 * 
	 * @return true.
	 */
	public boolean canChange() {
		
		return true;
	} // end of canChange method

	/**
	 * Show edit dialog.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return false.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
		
		// show dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new EditSignals(x+win.x,y+win.y,false);
		}
		else {
			new EditSignals(pos.x+win.x,pos.y+win.y,false);
		}
		
		if (!cancelled) {
			circuit.markChanged();
		}
		return false;
	} // end of change method

	/**
	 * Dialog to get text information from user.
	 */
	private class EditSignals extends JDialog implements ActionListener {
		
		// properties
		private JTextArea textArea = new JTextArea();
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");

		/**
		 * Initialize the dialog at a given position.
		 * 
		 * @param x The x-coordinate of the upper left of the dialog box.
		 * @param y The y-coordinate of the upper left of the dialog box.
		 * @param creating True if creating, false if changing.
		 */
		public EditSignals(int x, int y, boolean creating) {

			super(JLSInfo.frame,"Create Signal Specification",true);
			
			// set up GUI
			Container window = getContentPane();
			window.setLayout(new BorderLayout());
			if (!creating) {
				textArea.setText(signals);
			}
			JScrollPane pane = new JScrollPane(textArea);
			window.add(pane, BorderLayout.CENTER);
			JPanel buttons = new JPanel();
			buttons.setLayout(new GridLayout(1,3));
			buttons.add(ok);
			buttons.add(cancel);
			ok.setBackground(Color.green);
			cancel.setBackground(Color.pink);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"siggen",null);
			buttons.add(help);
			window.add(buttons, BorderLayout.SOUTH);
			getRootPane().setDefaultButton(ok);
			
			// add listeners
			ok.addActionListener(this);
			cancel.addActionListener(this);
			
			// make it visible
			setSize(size,size);
			setLocation(x-size/2,y-size/2);
			setVisible(true);
		} // end of constructor
		
		/**
		 * React to buttons.
		 * 
		 * @param event The event object for this event.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok) {
				String newSignals = textArea.getText();
				if (newSignals.equals(signals)) {
					cancelled = true;
				}
				signals = newSignals;
			}
			else if (event.getSource() == cancel) {
				cancelled = true;
			}
			dispose();
		} // end of actionPerformed method
		
	} // end of EditSignals class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Parse signal specification and post all events.
	 * If signal generator is in an imported circuit, do nothing.
	 * 
	 * @param sim The simulator.
	 */
	public void initSim(Simulator sim) {
		
		// do nothing if in an imported circuit
		if (circuit.isImported())
			return;
		
		Scanner input = new Scanner(signals);
		
		super.initSim(sim,input);
	} // end of initSim method
	
	/**
	 * Print or display an error about the signal specification.
	 * 
	 * @param msg An error message.
	 */
	protected void specError(String msg) {
		
		if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
			System.out.println("error in test file");
			System.out.println(msg);
			System.exit(1);
		}
		else {
			JOptionPane.showMessageDialog(JLSInfo.frame,"error in test file: " + msg);
			return;
		}
	} // end of specError method
	
} // end of SigGen class
