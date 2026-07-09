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
import jls.util.Placement;

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
				TellUser.error(JLSInfo.frame,
						"Only one signal generator per circuit", "Error");
				return false;
			}
		}
		
		// show creation dialog
		new EditSignals(true);
		
		// don't do anything if user canceled element
		if (cancelled)
			return false;
		
		// complete initialization
		init(g);
		
		// save position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
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
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes. The
	// handwritten save escaped backslash, quote and newline exactly as
	// Attribute.StringAttribute does.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("signals") {
			protected String get(Element el) { return ((SigGen)el).signals; }
			protected void set(Element el, String v) { ((SigGen)el).signals = v; }
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Make a copy of this element.
	 *
	 * @return an exact copy of this element.
	 */
	public SigGen copy() {

		SigGen it = new SigGen(circuit);
		super.copy(it);
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
		new EditSignals(false);
		
		if (!cancelled) {
			circuit.markChanged();
		}
		return false;
	} // end of change method

	/**
	 * Dialog to get text information from user.
	 */
	private class EditSignals extends ElementDialog {

		// properties
		private JTextArea textArea = new JTextArea();

		/**
		 * Initialize the dialog at a given position.
		 *
		 * @param creating True if creating, false if changing.
		 */
		public EditSignals(boolean creating) {

			super("Create Signal Specification","siggen");

			// set up GUI
			Container window = getContentPane();
			if (!creating) {
				textArea.setText(signals);
			}
			JScrollPane pane = new JScrollPane(textArea);
			pane.setPreferredSize(new Dimension(size,size));
			window.add(pane);

			finishDialog();
		} // end of constructor

		/**
		 * Accept the signal specification.
		 */
		protected void validateAndAccept() {

			String newSignals = textArea.getText();
			if (newSignals.equals(signals)) {
				cancelled = true;
			}
			signals = newSignals;
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this edit.
		 */
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

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
		
		if (JLSInfo.batch && JLSInfo.frame == null) {
			System.out.println("error in test file");
			System.out.println(msg);
			System.exit(1);
		}
		else {
			TellUser.error(JLSInfo.frame,"error in test file: " + msg, "Error");
			return;
		}
	} // end of specError method
	
} // end of SigGen class
