package jls.elem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jls.BitSetUtils;
import jls.JLSInfo;
import jls.KeyPad;
import jls.sim.Simulator;

/**
 * A single state.
 */
public class State {

	// properties
	private StateMachine machine;	// the state machine this state is in
	private String name;
	private boolean initial = false;
	private Vector<Out> outs = new Vector<Out>();
	private Set<Transition> trans = new HashSet<Transition>();
	private int x;
	private int y;
	private int diameter;
	private class Check {
		public int bits;
		public boolean isInput;	// false if output
		public int refs; // delete from map when 0
	};
	private Map <String,Check> bitmap = new HashMap<String,Check>();
	// signal names to number of bits, for consistency check

	// other temporary properties
	private int savex;
	private int savey;
	private boolean highlight;
	private Transition lastHighlighted;
	private Out buildOut;
	private Transition buildTrans;

	/**
	 * Outputs from this state.
	 */
	private class Out {

		public String signal;
		public int bits;
		public long value;

		public String toString() { return signal + "[" + bits + "] = " + value; }
	} // end of Out class

	/**
	 * Transitions from a given state
	 */
	private class Transition {

		public String signal = "";
		public int bits = 1;
		public boolean equal = true;
		public int value = 0;
		public boolean unconditional = true;
		public boolean other = false;
		public State nextState;
		public String nextStateName = null; // temporarily used during load
		public Vector<Point>points = new Vector<Point>();
		public Point highlight = null;
		public boolean highlighted;
	} // end of Transition class

	/**
	 * Create a new state.
	 * Make its diameter large enough to contain the name.
	 * 
	 * @param name The name of the state.
	 * @param g The Graphics object to use.
	 */
	public State(StateMachine machine, String name, Graphics g) {

		this.machine = machine;
		this.name = name;
		if (g != null) {
			FontMetrics fm = g.getFontMetrics();
			diameter = Math.max(fm.stringWidth("  " + name + "  "),JLSInfo.stateDiameter);
		}
	} // end of constructor

	/**
	 * Draw this state.
	 * 
	 * @param g The Graphics object to use.
	 */
	public void draw(Graphics g) {

		// set up
		int d = diameter;
		int r = d/2;
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int height = ascent + descent;

		// draw transitions
		for (Transition tr : trans) {

			// set line color
			Color color = Color.black;
			if (tr.highlighted) {
				color = JLSInfo.highlightColor;
			}

			// if no midpoints, draw straight from this state to the other
			if (tr.points.isEmpty()) {

				// draw line from start state edge to end state edge
				int w = tr.nextState.x - x;
				int h = y - tr.nextState.y;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist); // start edge
				int dyf = (int)Math.rint(h*r/dist);
				int or = tr.nextState.diameter/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.x-dxt;	// end edge
				int endy = tr.nextState.y+dyt;
				g.setColor(color);
				g.drawLine(x+dxf,y-dyf,endx,endy);

				// draw arrow
				double angle = SMUtil.getAngle(w,h);
				SMUtil.drawArrow(endx,endy,angle,g);

				// draw condition info
				int midx = (x+dxf+endx)/2;
				int midy = (y-dyf+endy)/2;
				drawCond(tr,midx,midy,angle,g);
			}

			// otherwise draw transition in segments
			else {

				// draw first segment
				int nx = tr.points.get(0).x;
				int ny = tr.points.get(0).y;
				int w = nx - x;
				int h = y - ny;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist);
				int dyf = (int)Math.rint(h*r/dist);
				g.setColor(color);
				g.drawLine(x+dxf,y-dyf,nx,ny);

				// draw condition info
				int midx = (x+dxf+nx)/2;
				int midy = (y-dyf+ny)/2;
				drawCond(tr,midx,midy,SMUtil.getAngle(w,h),g);

				// draw all but last segment
				int px = nx;
				int py = ny;
				for (int i=1; i<tr.points.size(); i+=1) {
					nx = tr.points.get(i).x;
					ny = tr.points.get(i).y;
					g.drawLine(px,py,nx,ny);
					px = nx;
					py = ny;
				}

				// draw last segment
				w = tr.nextState.x - px;
				h = py - tr.nextState.y;
				dist = Math.sqrt(w*w+h*h);
				int or = tr.nextState.diameter/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.x-dxt;	// end edge
				int endy = tr.nextState.y+dyt;
				g.drawLine(px,py,endx,endy);

				// draw arrow
				double angle = SMUtil.getAngle(w,h);
				SMUtil.drawArrow(endx,endy,angle,g);

				// draw highlight point if there is one
				if (tr.highlight != null) {
					int pd = JLSInfo.pointDiameter;
					g.setColor(JLSInfo.highlightColor);
					g.fillOval(tr.highlight.x-pd/2,tr.highlight.y-pd/2,pd,pd);
				}
			}
		}

		// show initial if necessary
		if (initial) {
			g.setColor(JLSInfo.initialStateColor);
			g.fillOval(x-r,y-r,d+1,d+1);
		}

		// highlight if necessary
		if (highlight) {
			g.setColor(JLSInfo.highlightColor);
			g.fillOval(x-r,y-r,d+1,d+1);
		}

		// draw circle
		g.setColor(Color.BLACK);
		g.drawOval(x-r,y-r,d,d);

		// draw name
		int width = fm.stringWidth(name);
		g.drawString(name,x-width/2,y-height/2+ascent);

	} // end of draw method

	/*
	 * Draw the condition information on a transition.
	 * 
	 * @param trans A transition.
	 * @param x The x-coordinate of the middle of a line segment.
	 * @param y The y-coordinate of the middle of a line segment.
	 * @param angle The angle of a line segment (in degrees).
	 * @param g The Graphics object to use.
	 */
	public void drawCond(Transition trans, int x, int y, double angle, Graphics g) {

		String cond = "";
		if (trans.other) {
			cond = "else";
		}
		else if (!trans.unconditional) {
			String rel = " != ";
			if (trans.equal) {
				rel = " = ";
			}
			cond = trans.signal + "[" + trans.bits + "]"+ rel + trans.value;
		}
		FontMetrics fm = g.getFontMetrics();
		int descent = fm.getDescent();
		int width = fm.stringWidth(cond);

		if (angle == 0.0 || angle == 180) {
			g.drawString(cond,x-width/2,y-descent-1);
		}
		else if (0.0 < angle && angle <= 90.0 || 180.0 < angle && angle < 270.0) {
			g.drawString(cond,x-width,y-descent-1);
		}
		else {
			g.drawString(cond,x+1,y-descent-1);
		}
	} // end of drawCond method

	/**
	 * Get the bounds of this state.
	 * 
	 * @param g The graphics object to use.
	 * 
	 * @return The bounds of this state and all of its outward transitions.
	 */
	public Rectangle getBounds(Graphics g) {

		// add circle to bounds
		int d = diameter;
		int r = d/2;
		Rectangle bounds = new Rectangle(x-r,y-r,d,d);

		// add transition to bounds
		for (Transition tr : trans) {

			// add points to bounds
			for (Point p : tr.points) {
				bounds.add(p);
			}

			// add condition to bounds
			if (tr.points.isEmpty()) {

				// direct
				int w = tr.nextState.x - x;
				int h = y - tr.nextState.y;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist); // start edge
				int dyf = (int)Math.rint(h*r/dist);
				int or = tr.nextState.diameter/2;
				int dxt = (int)Math.rint(w*or/dist);
				int dyt = (int)Math.rint(h*or/dist);
				int endx = tr.nextState.x-dxt;	// end edge
				int endy = tr.nextState.y+dyt;
				double angle = SMUtil.getAngle(w,h);
				int midx = (x+dxf+endx)/2;
				int midy = (y-dyf+endy)/2;
				bounds.add(boundCond(tr,midx,midy,angle,g));
			}

			// otherwise get bounds of segments
			else {

				// draw first segment
				int nx = tr.points.get(0).x;
				int ny = tr.points.get(0).y;
				int w = nx - x;
				int h = y - ny;
				double dist = Math.sqrt(w*w+h*h);
				int dxf = (int)Math.rint(w*r/dist);
				int dyf = (int)Math.rint(h*r/dist);
				int midx = (x+dxf+nx)/2;
				int midy = (y-dyf+ny)/2;
				bounds.add(boundCond(tr,midx,midy,SMUtil.getAngle(w,h),g));
			}
		}
		return bounds;
	} // end of getBounds method

	/*
	 * Get the bounds on the condition.
	 * If g is null, then return a 0x0 rectangle at x,y.
	 * 
	 * @param trans A transition.
	 * @param x The x-coordinate of the middle of a line segment.
	 * @param y The y-coordinate of the middle of a line segment.
	 * @param angle The angle of a line segment (in degrees).
	 * @param g The Graphics object to use.
	 * 
	 * @return The bounds of the condition on this transition.
	 */
	public Rectangle boundCond(Transition trans, int x, int y, double angle, Graphics g) {

		if (g == null) {
			return new Rectangle(x,y,0,0);
		}
		String cond = "";
		if (trans.other) {
			cond = "else";
		}
		else if (!trans.unconditional) {
			String rel = " != ";
			if (trans.equal) {
				rel = " = ";
			}
			cond = trans.signal + "[" + trans.bits + "]"+ rel + trans.value;
		}
		FontMetrics fm = g.getFontMetrics();
		int descent = fm.getDescent();
		int height = descent + fm.getAscent();
		int width = fm.stringWidth(cond);

		if (angle == 0.0 || angle == 180) {
			return new Rectangle(x-width/2,y,width,height);
		}
		else if (0.0 < angle && angle <= 90.0 || 180.0 < angle && angle < 270.0) {
			return new Rectangle(x-width,y,width,height);
		}
		else {
			return new Rectangle(x,y,width,height);
		}
	} // end of boundCond method

	/**
	 * Make a copy of this state.
	 * * 
	 * @param it The new state machine.
	 * 
	 * @return A copy of this state.
	 */
	public State copy(StateMachine it) {

		// create new state and add to map
		State newState = new State(it,name,null);
		//stateMap.put(name,newState);

		// fill in basic info
		newState.x = x;
		newState.y = y;
		newState.diameter = diameter;
		newState.initial = initial;

		// make copy of all outs
		for (Out out : outs) {
			Out newOut = new Out();
			newOut.signal = new String(out.signal);
			newOut.value = out.value;
			newOut.bits = out.bits;
			newState.outs.add(newOut);
		}

		// make copy of all transitions
		for (Transition tran : trans) {
			Transition newTrans = new Transition();
			newTrans.signal = new String(tran.signal);
			newTrans.unconditional = tran.unconditional;
			newTrans.other = tran.other;
			newTrans.equal = tran.equal;
			newTrans.value = tran.value;
			newTrans.bits = tran.bits;
			newTrans.nextStateName = new String(tran.nextState.getName());
			for (Point p : tran.points) {
				newTrans.points.add(new Point(p.x,p.y));
			}
			newState.trans.add(newTrans);
		}

		// make a copy of the signal bitmap
		newState.bitmap = new HashMap<String,Check>();
		for (String sig : bitmap.keySet()) {
			Check ch = bitmap.get(sig);
			Check newch = new Check();
			newch.bits = ch.bits;
			newch.isInput = ch.isInput;
			newch.refs = ch.refs;
			newState.bitmap.put(sig, newch);
		}
		return newState;
	} // end of copy method

	/**
	 * Replace all symbolic links to next states with actual references.
	 * 
	 * @param it The new state machine.
	 */
	public void linkTrans(StateMachine it, Map<String,State>stateMap) {

		for (Transition tran : trans) {
			if (tran.nextStateName != null) {
				tran.nextState = stateMap.get(tran.nextStateName);
			}
		}
	} // end of linkTrans method

	/**
	 * Save information about this state.
	 * 
	 * @param output The PrintWriter to write to.
	 */
	public void save(PrintWriter output) {

		output.println(" String state \"" + name + "\"");
		output.println("  int x " + x);
		output.println("  int y " + y);
		output.println("  int diameter " + diameter);
		output.println("  int init " + (initial ? 1 : 0));
		for (Out out : outs) {
			output.println("  String output \"" + out.signal + "\"");
			output.println("   long value " + out.value);
			output.println("   int bits " + out.bits);
		}
		for (Transition tr : trans) {
			if (tr.unconditional)
				output.println("  String trans \"always\"");
			else if (tr.other)
				output.println("  String trans \"else\"");
			else {
				output.println("  String trans \"" + tr.signal + "\"");
				if (tr.equal)
					output.println("   int eq 0");
				else
					output.println("   int eq 1");
				output.println("   int value " + tr.value);
				output.println("   int bits " + tr.bits);
			}
			output.println("   String next \"" + tr.nextState.getName() + "\"");
			for (Point p : tr.points) {
				output.println("    pair " + p.x + " " + p.y);
			}
		}
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {

		if (name.equals("x")) {
			x = value;
		} else if (name.equals("y")) {
			y = value;
		} else if (name.equals("diameter")) {
			diameter = value;
		} else if (name.equals("init")) {
			if (value == 0)
				initial = false;
			else
				initial = true;
		}
	} // end of setValue method

	/**
	 * Set an output String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String name, String value) {

		buildOut = new Out();
		buildOut.signal = value;
		outs.add(buildOut);
	} // end of setOutputValue method

	/**
	 * Set an output int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String name, int value) {

		if (name.equals("bits")) {
			buildOut.bits = value;
			Check ch = bitmap.get(buildOut.signal);
			if (ch == null) {
				ch = new Check();
				ch.bits = buildOut.bits;
				ch.isInput = false;
				ch.refs = 1;
				bitmap.put(buildOut.signal,ch);
			}
			else {
				ch.refs += 1;
			}
		}
	} // end of setOutputValue method

	/**
	 * Set an output long instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setOutputValue(String name, long value) {

		if (name.equals("value")) {
			buildOut.value = value;
		}
	} // end of setOutputValue method

	/**
	 * Set an transition String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setTransValue(String name, String value) {

		if (name.equals("trans")) {
			buildTrans = new Transition();
			buildTrans.unconditional = false;
			buildTrans.other = false;
			if (value.equals("always"))
				buildTrans.unconditional = true;
			else if (value.equals("else"))
				buildTrans.other = true;
			else 
				buildTrans.signal = value;
			trans.add(buildTrans);
		}
		else if (name.equals("next")) {

			// link to state if possible
			for (State state : machine.getStates()) {
				if (state.getName().equals(value)) {
					buildTrans.nextState = state;
					return;
				}
			}

			// else save name for link to be set when state is read in
			buildTrans.nextStateName = value;
		}
	} // end of setTransValue method

	/**
	 * Set a transition int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setTransValue(String name, int value) {

		if (name.equals("eq")) {
			if (value == 0)
				buildTrans.equal = true;
			else
				buildTrans.equal = false;
		}
		else if (name.equals("value")) {
			buildTrans.value = value;
		}
		else if (name.equals("bits")) {
			buildTrans.bits = value;
			Check ch = bitmap.get(buildTrans.signal);
			if (ch == null) {
				if (!buildTrans.unconditional && !buildTrans.other) {
					ch = new Check();
					ch.bits = buildTrans.bits;
					ch.isInput = true;
					ch.refs = 1;
					bitmap.put(buildTrans.signal,ch);
				}
			}
			else {
				ch.refs += 1;
			}
		}
	} // end of setTransValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v1 The second value.
	 */
	public void setTransPair(int v1, int v2) {

		buildTrans.points.add(new Point(v1,v2));

	} // end of setPair method

	/**
	 * Fix any transitions pointing at this state symbolically to real references.
	 */
	public void fixTrans() {

		// fix any transitions ending in this state
		for (State state : machine.getStates()) {
			for (Transition tr : state.trans) {
				if (tr == buildTrans)
					continue;	// catch it later
				if (tr.nextStateName != null && tr.nextStateName.equals(name)) {
					tr.nextState = this;
					tr.nextStateName = null;
				}
			}
		}
	} // end of fixTrans method

	/**
	 * Get the number of bits in a given input signal.
	 * If this state doesn't have a transition using that signal,
	 * return 0.
	 * 
	 * @param signal The input signal name.
	 * 
	 * @return the number of bits, or 0 if signal not used.
	 */
	public int inputBits(String signal) {

		for (Transition tran : trans) {
			if (tran.signal.equals(signal))
				return tran.bits;
		}
		return 0;
	} // end of inputBits method

	/**
	 * Get the number of bits in a given output signal.
	 * If this state doesn't have an output using that signal,
	 * return 0.
	 * 
	 * @param signal The output signal name.
	 * 
	 * @return the number of bits, or 0 if signal not used.
	 */
	public int outputBits(String signal) {

		for (Out out : outs) {
			if (out.signal.equals(signal))
				return out.bits;
		}
		return 0;
	} // end of inputBits method

	/**
	 * See if a given point is inside this state.
	 * 
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean contains(int x, int y) {

		int r = diameter/2;
		if ((this.x-x)*(this.x-x)+(this.y-y)*(this.y-y) < r*r) {
			return true;
		}
		else {
			return false;
		}
	} // end of contains method

	/**
	 * See if this state overlaps another state.
	 * 
	 * @param other the other state
	 */
	public boolean overlaps(State other) {

		int d = JLSInfo.stateDiameter;
		int ox = other.x;
		int oy = other.y;
		if ((x-ox)*(x-ox)+(y-oy)*(y-oy) <= d*d) {
			return true;
		}
		else {
			return false;
		}
	} // end of overlaps method

	/**
	 * See if this state is completely inside the given rectangle.
	 * 
	 * @param rect The rectangle.
	 * 
	 * @return true if it is inside, false if not.
	 */
	public boolean isInside(Rectangle rect) {

		return rect.contains(getRect());
	} // end of isInside method

	/**
	 * Set/reset highlight property of this state.
	 * 
	 * @param which True to highlight, false not to.
	 */
	public void setHighlight(boolean which) {

		highlight = which;
	} // end of setHighlight method

	/**
	 * Move state by a given amount.
	 * Also move every point in this state's transitions if the 
	 * end state of those transitions is also selected.
	 * 
	 * @param dx The distance to move in the x direction.
	 * @param dy The distance to move in the y direction.
	 * @param selected The selected states.
	 */
	public void move(int dx, int dy, Set<State> selected) {

		x += dx;
		y += dy;
		for (Transition tr : trans) {
			if (selected.contains(tr.nextState)) {
				for (Point p : tr.points) {
					p.translate(dx,dy);
				}
			}
		}
	} // end of move method

	/**
	 * Move state to new position.
	 * 
	 * @param x The new x-coordinate.
	 * @param y The new y-coordinate.
	 */
	public void moveTo(int x, int y) {

		this.x = x;
		this.y = y;
	} // end of moveTo method

	/**
	 * Save the current position of this state.
	 */
	public void savePosition() {

		savex = x;
		savey = y;
	} // end of savePosition method

	/**
	 * Restore the saved position of this state.
	 */
	public void restorePosition() {

		x = savex;
		y = savey;
	} // end of restorePosition method

	/**
	 * Get bounding rectangle for this state.
	 * 
	 * @return the bounding rectangle.
	 */
	public Rectangle getRect() {

		int d = diameter;
		int r = d/2;
		return new Rectangle(x-r,y-r,d,d);
	} // end of getRect method

	/**
	 * Get the name of this state.
	 * 
	 * @return the name.
	 */
	public String getName() {

		return name;
	} // end of getName method

	/**
	 * See if this state has any transitions.
	 * 
	 * @return true if it does, false if it does not.
	 */
	public boolean hasTransitions() {

		return !trans.isEmpty();
	} // end of hasTransitions method

	/**
	 * Change the name of this state if it fits.
	 * 
	 * @param name The new name.
	 * @param g The Graphics object to use.
	 */
	public boolean changeName(String name, Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth("  " + name + "  ");
		if (w > diameter) {
			return false;
		}
		else {
			this.name = name;
			return true;
		}
	} // end of changeName method

	/**
	 * Make a new transition from this state.
	 * 
	 * @param to The state the transition is to.
	 * @param points The list of points in the transition (the last one will not be used).
	 */
	public void newTransition(State to, Vector<Point> points, JDialog mainDialog) {

		// if transition is to the same state, then make sure
		// there are at least three points in the transition else
		// it won't look good
		if (this == to) {
			if (points.size() < 3) {
				return;
			}
		}

		// make a new transition
		Transition newTrans = new Transition();
		newTrans.nextState = to;
		newTrans.signal = "";
		newTrans.bits = -1;
		for (int i=0; i<points.size()-1; i+=1) {
			newTrans.points.add(points.get(i));
		}

		// load transition with known info
		for (Transition oldTrans : trans) {
			if (!oldTrans.signal.equals("")) {
				newTrans.signal = oldTrans.signal;
			}
			if (oldTrans.bits != 0) {
				newTrans.bits = oldTrans.bits;
			}
		}

		// get condition info
		CreateTrans ct = new CreateTrans(newTrans,this,mainDialog);

		// if it wasn't canceled...
		if (!ct.wasCancelled()) {

			// get properties of existing transition set
			boolean hasUnconditional = false;
			boolean hasOther = false;
			String signal = "";
			BitSet tested = new BitSet();
			int bits = 0;
			for (Transition oldTrans : trans) {
				if (oldTrans.unconditional) {
					hasUnconditional = true;
				}
				else if (oldTrans.other) {
					hasOther = true;
				}
				else if (oldTrans.equal) {
					signal = oldTrans.signal;
					bits = oldTrans.bits;
					tested.set(oldTrans.value);
				}
				else {
					signal = oldTrans.signal;
					bits = oldTrans.bits;
					BitSet temp = new BitSet();
					temp.set(oldTrans.value);
					temp.flip(0,1<<bits);
					tested.or(temp);
				}
			}

			// now test new transition for problems
			if (hasUnconditional) {
				JOptionPane.showMessageDialog(mainDialog,
						"State already has an unconditional transition",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (newTrans.unconditional && !trans.isEmpty()) {
				JOptionPane.showMessageDialog(mainDialog,
						"Can't add an unconditional transition",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (newTrans.other) {
				if (trans.isEmpty()) {
					JOptionPane.showMessageDialog(mainDialog,
							"State can't have just an else transition",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (hasOther) {
					JOptionPane.showMessageDialog(mainDialog,
							"State already has an else transition",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (tested.cardinality() == 1<<bits) {
					JOptionPane.showMessageDialog(mainDialog,
							"Existing transitions cover all possible cases",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				trans.add(newTrans);
				return;
			}
			if (!signal.equals("") && !signal.equals(newTrans.signal)) {
				JOptionPane.showMessageDialog(mainDialog,
						"Can't test different signals in same state",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (bits > 0 && bits != newTrans.bits) {
				JOptionPane.showMessageDialog(mainDialog,
						"Bits differ",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// check for inconsistent use of signal throughout state machine
			Check ch = bitmap.get(newTrans.signal);
			if (ch == null) {
				if (!newTrans.unconditional && !newTrans.other) {
					ch = new Check();
					ch.bits = newTrans.bits;
					ch.isInput = true;
					ch.refs = 1;
					bitmap.put(newTrans.signal,ch);
				}
			}
			else {
				if (!ch.isInput) {
					JOptionPane.showMessageDialog(mainDialog,
							"This signal is already an output", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (ch.bits != newTrans.bits) {
					JOptionPane.showMessageDialog(mainDialog,
							"Bits not consistent with previous use of this signal",
							"Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				ch.refs += 1;
			}
			if (newTrans.equal) {
				if (tested.get(newTrans.value)) {
					JOptionPane.showMessageDialog(mainDialog,
							"This value already tested in an existing transition",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				tested.set(newTrans.value);
				if (hasOther && tested.cardinality() == 1<<bits) {
					JOptionPane.showMessageDialog(mainDialog,
							"New transition would make else transition impossible",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				trans.add(newTrans);
				return;
			}
			if (!newTrans.equal) {
				if (hasOther) {
					JOptionPane.showMessageDialog(mainDialog,
							"New transition would make else transition impossible",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				BitSet temp = new BitSet();
				temp.set(newTrans.value);
				temp.flip(0,1<<bits);
				if (temp.intersects(tested)) {
					JOptionPane.showMessageDialog(mainDialog,
							"These values already tested in existing transition(s)",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				trans.add(newTrans);
			}
		}
	} // end of newTransition method

	/**
	 * Turn on/off initial state status.
	 * Not responsible for turning off initial state status in another state.
	 * 
	 * @param which True to make this state the initial state, false to make it
	 * 				not be initial.
	 */
	public void setInitial(boolean which) {

		initial = which;
	} // end of setInitial method

	/**
	 * Get the location of the center of this state.
	 * 
	 * @return The x,y coordinates of the center.
	 */
	public Point getLocation() {

		return new Point(x,y);
	} // end of getLocation method

	/**
	 * Remove all transitions from this state to the given state
	 * (because the given state is being removed).
	 * 
	 * @param other The state being removed.
	 */
	public void removeTrans(State other) {

		Set <Transition> removes = new HashSet<Transition>();
		for (Transition tr : trans) {
			if (tr.nextState == other) {
				removes.add(tr);

				// clean up signal use info
				Check ch = bitmap.get(tr.signal);
				if (ch != null) {
					ch.refs -= 1;
					if (ch.refs == 0) {
						bitmap.remove(tr.signal);
					}
				}
			}
		}
		trans.removeAll(removes);
	} // end of removeTrans method

	/**
	 * Highlight any transition corner that is close to a given point.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return the point if there is one, else null.
	 */
	public Point highlightTransPoints(int x, int y) {

		int ds = JLSInfo.pointDiameter*JLSInfo.pointDiameter;
		for (Transition tr : trans) {
			tr.highlight = null;
			for (Point p : tr.points) {
				if ((p.x-x)*(p.x-x)+(p.y-y)*(p.y-y) < ds) {
					tr.highlight = p;
					return p;
				}
			}
		}
		return null;
	} // end of highlightTransPoints method

	/**
	 * Highlight any transition that has a line segment close to a given point.
	 * Also save reference to the highlighted transition so it can be deleted
	 * if the user wants to.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return true if some transition is highlighted, false if none
	 */
	public boolean highlightTrans(int xp, int yp) {

		// for all transitions...
		int d = JLSInfo.pointDiameter;
		for (Transition tran : trans) {

			// un-highlight it
			tran.highlighted = false;

			// if a single segment line...
			if (tran.points.isEmpty()) {
				double maind =
					Line2D.ptSegDist(x,y,tran.nextState.x,tran.nextState.y,xp,yp);
				if (maind < d && !contains(xp,yp) && !tran.nextState.contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}
			}
			else {

				// check out first segment
				Point p = tran.points.get(0);
				double maind = Line2D.ptSegDist(x,y,p.x,p.y,xp,yp);
				if (maind < d && !contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}

				// check out middle segments
				for (int i=1; i<tran.points.size(); i+=1) {
					Point p1 = tran.points.get(i-1);
					Point p2 = tran.points.get(i);
					maind = Line2D.ptSegDist(p1.x,p1.y,p2.x,p2.y,xp,yp);
					if (maind < d) {
						tran.highlighted = true;
						lastHighlighted = tran;
						return true;
					}
				}

				// check out last segment
				p = tran.points.get(tran.points.size()-1);
				maind = Line2D.ptSegDist(p.x,p.y,tran.nextState.x,tran.nextState.y,xp,yp);
				if (maind < d && !tran.nextState.contains(xp,yp)) {
					tran.highlighted = true;
					lastHighlighted = tran;
					return true;
				}
			}
		}
		return false;
	} // end of highlightTrans method

	/**
	 * Delete last highlighted transition.
	 */
	public void deleteLastHighlighted() {

		// clean up signal use info
		Check ch = bitmap.get(lastHighlighted.signal);
		if (ch != null) {
			ch.refs -= 1;
			if (ch.refs == 0) {
				bitmap.remove(lastHighlighted.signal);
			}
		}

		// remove transition
		trans.remove(lastHighlighted);

		// if all that's left is an else transition, make it be unconditional
		if (trans.size() == 1) {
			for (Transition tran : trans) {
				if (tran.other) {
					tran.other = false;
					tran.unconditional = true;
				}
			}
		}
	} // end of deleteLastHighlighted method

	/**
	 * Delete all transitions from this state.
	 */
	public void deleteAllTrans() {

		// clean up input bit map
		for (Transition tran : trans) {
			Check ch = bitmap.get(tran.signal);
			if (ch != null) {
				ch.refs -= 1;
				if (ch.refs == 0) {
					bitmap.remove(tran.signal);
				}
			}
		}

		trans.clear();
	} // end of deleteAllTrans method

	/**
	 * Get the maximum width (in pixels) of all input and output names in
	 * this state.
	 * 
	 * @param fm A FontMetrics object to use.
	 * 
	 * @return the maximum width.
	 */
	public int getWidthInfo(FontMetrics fm) {

		int width = 0;
		for (Out out : outs) {
			width = Math.max(width,fm.stringWidth(out.signal));
		}
		for (Transition tr : trans) {
			width = Math.max(width,fm.stringWidth(tr.signal));
		}
		return width;
	} // end of getWidthInfo method

	/**
	 * Get set of all input signals used by transitions from this state.
	 * 
	 * @return the set of input signal names.
	 */
	public Set<String> getInputs() {

		Set<String> inputs = new HashSet<String>();
		for (Transition tr : trans) {
			if (!tr.signal.equals("") && !tr.signal.equals("else"))
				inputs.add(tr.signal);
		}
		return inputs;
	} // end of getInputs method

	/**
	 * Get set of all output signals asserted by this state.
	 * 
	 * @return the set of output signal names.
	 */
	public Set<String> getOutputs() {

		Set<String> outputs = new HashSet<String>();
		for (Out out : outs) {
			outputs.add(out.signal);
		}
		return outputs;
	} // end of getInputs method

	/**
	 * Dialog to create info about a transition.
	 */
	private class CreateTrans extends JDialog implements ActionListener {

		// properties
		private Transition trans;
		private State myState;
		private JButton ok = new JButton("ok");
		private JButton cancel = new JButton("cancel");
		private JTextField signalField = new JTextField(10);
		private JButton equalOrNot = new JButton("=");
		private JTextField valueField = new JTextField(10);
		private KeyPad valuePad = new KeyPad(valueField,10,0,this);
		private JTextField bitsField = new JTextField(10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		private JRadioButton conditional = new JRadioButton("conditional");
		private JRadioButton unconditional = new JRadioButton("unconditional");
		private JRadioButton otherwise = new JRadioButton("if no other condition");
		private boolean cancelled;

		/**
		 * Initialize dialog.
		 * 
		 * @param tr The transition to edit.
		 */
		public CreateTrans(Transition tr, State st, JDialog theDialog) {

			// set up
			super(theDialog,"Create Transition",true);
			cancelled = false;

			// save working transition and its state
			trans = tr;
			myState = st;

			// get window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));

			// set up signal/value
			JPanel sigval = new JPanel(new BorderLayout());
			JPanel sig = new JPanel(new GridLayout(3,1));
			sig.add(new JLabel("signal",SwingConstants.CENTER));
			sig.add(signalField);
			sig.add(new JLabel(" "));
			sigval.add(sig,BorderLayout.WEST);
			JPanel other = new JPanel(new BorderLayout());
			JPanel misc = new JPanel(new GridLayout(3,1));
			misc.add(new JLabel(" "));
			misc.add(equalOrNot);
			misc.add(new JLabel("bits: ", SwingConstants.RIGHT));
			other.add(misc,BorderLayout.WEST);
			JPanel values = new JPanel(new GridLayout(3,1));
			values.add(new JLabel("value",SwingConstants.CENTER));
			JPanel val = new JPanel(new BorderLayout());
			val.add(valueField,BorderLayout.CENTER);
			val.add(valuePad,BorderLayout.EAST);
			values.add(val);
			JPanel bits = new JPanel(new BorderLayout());
			bits.add(bitsField,BorderLayout.CENTER);
			bits.add(bitsPad,BorderLayout.EAST);
			values.add(bits);
			other.add(values,BorderLayout.CENTER);
			sigval.add(other,BorderLayout.CENTER);
			window.add(sigval);

			// add radio buttons
			window.add(new JLabel(" "));
			window.add(conditional);
			if (trans.signal.equals("")) {
				window.add(unconditional);
			}
			window.add(otherwise);
			ButtonGroup g = new ButtonGroup();
			g.add(conditional);
			g.add(unconditional);
			g.add(otherwise);

			// add ok/cancel
			window.add(new JLabel(" "));
			JPanel okcancel = new JPanel(new GridLayout(1,2));
			okcancel.add(ok);
			ok.setBackground(Color.GREEN);
			okcancel.add(cancel);
			cancel.setBackground(Color.PINK);
			window.add(okcancel);

			// give initial values
			signalField.setText(tr.signal);
			if (tr.equal) {
				equalOrNot.setText("=");
			}
			else {
				equalOrNot.setText("!=");
			}
			valueField.setText(tr.value+"");
			if (tr.bits == -1)
				bitsField.setText("1");
			else
				bitsField.setText(tr.bits+"");
			if (tr.unconditional) {
				unconditional.setSelected(true);
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}
			else if (tr.other) {
				otherwise.setSelected(true);
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}
			else if (!tr.signal.equals("")) {
				signalField.setEditable(false);
				bitsField.setEditable(false);
			}

			// add listeners
			ok.addActionListener(this);
			cancel.addActionListener(this);
			equalOrNot.addActionListener(this);
			conditional.addActionListener(this);
			unconditional.addActionListener(this);
			otherwise.addActionListener(this);

			// set up window close listener to cancel element
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancelled = true;
							dispose();
						}
					}
			);

			// finish up
			pack();
			setLocation(100,100);
			setVisible(true);
		} // end of constructor

		/**
		 * React to events.
		 * 
		 * @param event The event object.
		 */
		public void actionPerformed(ActionEvent event) {

			// handle ok button
			if (event.getSource() == ok) {

				// handle unconditional transition
				if (unconditional.isSelected()) {
					trans.unconditional = true;
					trans.other = false;
					dispose();
					return;
				}

				// handle else transition
				if (otherwise.isSelected()) {
					trans.unconditional = false;
					trans.other = true;
					dispose();
					return;
				}

				// check for missing signal name
				if (signalField.getText().equals("")) {
					JOptionPane.showMessageDialog(this,
							"Missing signal name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// check for invalid signal names
				if (signalField.getText().equals("else")) {
					JOptionPane.showMessageDialog(this,
							"Invalid signal name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (signalField.getText().equals("clock")) {
					JOptionPane.showMessageDialog(this,
							"Invalid signal name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// if input signal already exists, get bit count for it
				int hasBits = -1;
				for (State st : machine.getStates()) {
					for (Transition tr : st.trans) {
						if (tr.signal.equals(signalField.getText())) {
							hasBits = tr.bits;
						}
					}
				}
				
				// get value and bits from dialog
				int tempValue = 0;
				int tempBits = 0;
				try {
					tempValue = Integer.parseInt(valueField.getText());
					tempBits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Invalid numeric value", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// make sure bits match with existing input
				if (hasBits > 0 && tempBits != hasBits) {
					JOptionPane.showMessageDialog(this,
							"Bits don't match with previous signal specification of " +
							hasBits + " bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				int newbits = Integer.parseInt(bitsField.getText());
				if (trans.bits != -1 && trans.bits != newbits) {
					JOptionPane.showMessageDialog(this,
							"Bits don't match with previous signal specification of " +
							trans.bits + " bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// make sure value will fit
				if (Math.log(tempValue+1)/Math.log(2) > tempBits) {
					JOptionPane.showMessageDialog(this,
							"Value too large for number of bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// finish conditional transition
				trans.unconditional = false;
				trans.other = false;
				trans.signal = signalField.getText();
				if (equalOrNot.getText().equals("=")) {
					trans.equal = true;
				}
				else {
					trans.equal = false;
				}
				trans.value = Integer.parseInt(valueField.getText());
				trans.bits = newbits;
				dispose();
			}
			
			// cancel new transition
			else if (event.getSource() == cancel) {
				cancelled = true;
				dispose();
			}
			
			// save equality type check
			else if (event.getSource() == equalOrNot) {
				if (equalOrNot.getText().equals("=")) {
					equalOrNot.setText("!=");
				}
				else {
					equalOrNot.setText("=");
				}
			}
			
			// handle new conditional transition
			else if (event.getSource() == conditional) {
				if (trans.signal.equals("")) {
					signalField.setEditable(true);
					valueField.setEditable(true);
					bitsField.setEditable(true);
				}
				else {
					signalField.setEditable(false);
					valueField.setEditable(true);
					bitsField.setEditable(false);

					// find unused value and put in value field
					BitSet used = new BitSet();
					int bits = 0;
					for (Transition tran : myState.trans) {
						bits = trans.bits;
						used.set(tran.value);
					}
					int val = used.nextClearBit(0);
					if (val < 1<<bits) {
						valueField.setText(val+"");
					}
				}
			}
			
			// handle new unconditional or else transition
			else if (event.getSource() == unconditional ||
					event.getSource() == otherwise) {
				signalField.setEditable(false);
				valueField.setEditable(false);
				bitsField.setEditable(false);
			}

		} // end of actionPerformed method

		/**
		 * See if this dialog was canceled.
		 * 
		 * @return true if canceled, false if not.
		 */
		public boolean wasCancelled() {

			return cancelled;
		} // end of wasCancelled method

	} // end of CreateTrans class

	/**
	 * Show outputs, no editing possible.
	 */
	public void showOuts(Point mp, JDialog theDialog) {

		// set up dialog
		final JDialog show = new JDialog(theDialog,false);
		Container window = show.getContentPane();
		addOuts(window);
		JButton close = new JButton("close window");
		close.setBackground(Color.green);
		window.add(close);
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				show.dispose();
			}
		});

		// finish up
		show.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		show.pack();
		Point dp = theDialog.getLocationOnScreen();
		if (mp == null) {
			Dimension md = theDialog.getSize();
			Dimension sd = show.getSize();
			show.setLocation(dp.x+(md.width-sd.width)/2,dp.y+(md.height-sd.height)/2);
		}
		else {
			show.setLocation(mp.x+dp.x,mp.y+dp.y);
		}
		show.setVisible(true);
	} // end of showOuts method

	/**
	 * Add labels for each output signal to a container.
	 * Give the container a grid layout of the appropriate size.
	 * 
	 * @param window The container to add to.
	 */
	public void addOuts(Container window) {

		// set up container
		window.setBackground(Color.WHITE);
		window.setLayout(new GridLayout(outs.size()+2,1));

		// add title
		JLabel title = new JLabel("Outputs for state: " + name);
		title.setOpaque(true);
		title.setBackground(Color.LIGHT_GRAY);
		window.add(title);

		// add labels, one per output
		for (Out out : outs) {
			window.add(new JLabel(out.toString(),SwingConstants.CENTER));
		}

	}

	/**
	 * Edit outputs.
	 */
	public void editOuts(JDialog theDialog) {
		new EditOutputs(theDialog);
	} // end of editOuts method

	/**
	 * Dialog to create outputs for a state.
	 */
	private class EditOutputs extends JDialog implements ActionListener {

		// properties
		private JButton close = new JButton("close window");
		private JList<Out> outList;
		DefaultListModel<Out> model;
		private JButton add = new JButton("add new output");
		private JButton delete = new JButton("delete selected output");
		private JTextField signalField = new JTextField(10);
		private JTextField valueField = new JTextField("1");
		private KeyPad valuePad = new KeyPad(valueField,10,0,this);
		private JTextField bitsField = new JTextField("1");
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		private boolean cancelled;

		/**
		 * Initialize dialog.
		 */
		public EditOutputs(JDialog theDialog) {

			// set up
			super(theDialog,"Edit Outputs",true);
			cancelled = false;

			// get window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));

			// set up output list
			model = new DefaultListModel<Out>();
			for (Out output : outs) {
				model.addElement(output);
			}
			outList = new JList<Out>(model);
			outList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane pane =  new JScrollPane(outList);
			pane.setSize(400,400);
			window.add(pane);

			// set up add/delete buttons
			JPanel addDel = new JPanel(new FlowLayout());
			addDel.add(add);
			addDel.add(delete);
			window.add(addDel);

			// set up signal/value
			JPanel sigval = new JPanel(new BorderLayout());
			JPanel sig = new JPanel(new GridLayout(3,1));
			sig.add(new JLabel("signal",SwingConstants.CENTER));
			sig.add(signalField);
			sig.add(new JLabel(" "));
			sigval.add(sig,BorderLayout.WEST);
			JPanel other = new JPanel(new BorderLayout());
			JPanel misc = new JPanel(new GridLayout(3,1));
			misc.add(new JLabel(" "));
			misc.add(new JLabel("="));
			misc.add(new JLabel("bits: ", SwingConstants.RIGHT));
			other.add(misc,BorderLayout.WEST);
			JPanel values = new JPanel(new GridLayout(3,1));
			values.add(new JLabel("value",SwingConstants.CENTER));
			JPanel val = new JPanel(new BorderLayout());
			val.add(valueField,BorderLayout.CENTER);
			val.add(valuePad,BorderLayout.EAST);
			values.add(val);
			JPanel bits = new JPanel(new BorderLayout());
			bits.add(bitsField,BorderLayout.CENTER);
			bits.add(bitsPad,BorderLayout.EAST);
			values.add(bits);
			other.add(values,BorderLayout.CENTER);
			sigval.add(other,BorderLayout.CENTER);
			window.add(sigval);

			// add close window button
			window.add(new JLabel(" "));
			JPanel cl = new JPanel(new GridLayout(1,1));
			close.setBackground(Color.GREEN);
			cl.add(close);
			window.add(cl);

			// add listeners
			close.addActionListener(this);
			add.addActionListener(this);
			delete.addActionListener(this);

			// set up window close listener to close window
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							dispose();
						}
					}
			);

			// finish up
			pack();
			setLocation(100,100);
			setVisible(true);
		} // end of constructor

		/**
		 * React to events.
		 * 
		 * @param event The event object.
		 */
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == close) {
				dispose();
			}
			else if (event.getSource() == add) {
				Out newOut = new Out();
				newOut.signal = signalField.getText().trim();
				if (newOut.signal.equals("")) {
					JOptionPane.showMessageDialog(this,
							"Missing signal name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					newOut.value = Integer.parseInt(valueField.getText());
					newOut.bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value or bits not valid", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (Math.log(newOut.value+1)/Math.log(2) > newOut.bits) {
					JOptionPane.showMessageDialog(this,
							"Value too large for number of bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// make sure this output doesn't conflict with existing outputs
				// in this state
				for (Out out : outs) {
					if (out.signal.equals(newOut.signal)) {
						JOptionPane.showMessageDialog(this,
								"Already an output with this name", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}

				// check for consistentcy for this signal throughout state machine
				Check ch = bitmap.get(newOut.signal);
				if (ch == null) {
					ch = new Check();
					ch.bits = newOut.bits;
					ch.isInput = false;
					ch.refs = 1;
					bitmap.put(newOut.signal,ch);
				}
				else {
					if (ch.isInput) {
						JOptionPane.showMessageDialog(this,
								"This signal is already an input", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (ch.bits != newOut.bits) {
						JOptionPane.showMessageDialog(this,
								"Bits not consistent with previous use of this signal",
								"Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
					ch.refs += 1;
				}
				outs.add(newOut);
				model.addElement(newOut);
			}
			else if (event.getSource() == delete) {
				int pos = outList.getSelectedIndex();
				if (pos >= 0) {
					Out out = (Out)model.getElementAt(pos);
					Check ch = bitmap.get(out.signal);
					ch.refs -= 1;
					if (ch.refs == 0) {
						bitmap.remove(out.signal);
					}
					outs.remove(pos);
					model.remove(pos);
				}
			}

		} // end of actionPerformed method

		/**
		 * See if this dialog was cancelled.
		 * 
		 * @return true if cancelled, false if not.
		 */
		@SuppressWarnings("unused")
		public boolean wasCancelled() {

			return cancelled;
		} // end of wasCancelled method

	} // end of EditOutputs class

	/**
	 * Determine the maximum printing width of this state.
	 * 
	 * @param g The Graphics object to use to determine sizes.
	 * 
	 * @return the maximum printing width.
	 */
	public int maxWidth(Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		int max = fm.stringWidth("state: " + name);
		for (Out out : outs) {
			max = Math.max(max,fm.stringWidth(out.signal + "=" + out.value));
		}
		return max;
	} // end of maxWidth method

	/**
	 * Determine the maximum printing height of this state.
	 * 
	 * @param g The Graphics object to use to determine sizes.
	 * 
	 * @return the maximum printing height.
	 */
	public int maxHeight(Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		int height = fm.getAscent() + fm.getDescent();
		return (outs.size()+1)*height;
	} // end of maxHeight method

	/**
	 * Print this state's outputs.
	 * 
	 * @param g The Graphics object to use.
	 * @param x The x-coordinate of the upper left corner of the print area.
	 * @param y The y-coordinate of the upper left corner of the print area.
	 */
	public void print(Graphics g, int x, int y) {

		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int height = ascent + fm.getDescent();
		g.drawString("State: " + name,x,y+ascent);
		y += height;
		for (Out out : outs) {
			g.drawString(out.signal + "=" + out.value,x,y+ascent);
			y += height;
		}
	} // end of print method

	//-----------------------------
	// simulation
	//-----------------------------

	/**
	 * Get the next state from a given one given the current input values.
	 * 
	 * @return The next state according, or null of no next state.
	 */
	public State getNextState() {

		// set up default return value
		State newState = null;

		// check transitions looking for match
		for (Transition tran : trans) {


			// if unconditional, then simply go to its next state
			if (tran.unconditional)
				return tran.nextState;

			// if "else", save nextState in case no other transition matches
			else if (tran.other) {
				newState = tran.nextState;
				continue;
			}

			// if a match with a conditional, then go to its next state
			else {
				BitSet inp = machine.getInput(tran.signal).getValue();
				if (inp ==  null)
					inp = new BitSet();
				long inputValue = BitSetUtils.ToLong(inp);
				if (tran.equal && inputValue == tran.value)
					return tran.nextState;
				else if (!tran.equal && inputValue != tran.value)
					return tran.nextState;
			}
		}

		// return the default (null or "else")
		return newState;
	} // end of getNextState method

	/**
	 * Send all out values of this state to the state machine outputs.
	 * Unspecified outputs will be made 0.
	 * 
	 * @param mach This state machine.
	 * @param now The current simulation time.
	 * @param sim The simulator.
	 */
	public void sendOutputs(StateMachine mach, long now, Simulator sim) {

		Set<Output> sent = new HashSet<Output>();

		// send out explicitly specified values
		for (Out out : outs) {
			Output output = machine.getOutput(out.signal);
			BitSet value = BitSetUtils.Create(out.value);
			output.propagate(value,now,sim);
			sent.add(output);
		}

		// send 0 to all unspecified outputs
		for (Output output : machine.getOutputs()) {
			if (!sent.contains(output)) {
				output.propagate(new BitSet(),now,sim);
			}
		}
	} // end of sendOutputs method

	/**
	 * Get the x-coordinate of this state.
	 * 
	 * @return the x-coordinate.
	 */
	int getX() {

		return x;
	} // end of getX method

	/**
	 * Get the y-coordinate of this state.
	 * 
	 * @return the y-coordinate.
	 */
	int getY() {

		return y;
	} // end of getY method

	/**
	 * Set the x-coordinate of this state
	 * 
	 * @param newx The new x-coordinate.
	 */
	void setX(int newx) {

		x = newx;
	} // end of setX method


	/**
	 * Set the y-coordinate of this state
	 * 
	 * @param newy The new y-coordinate.
	 */
	void setY(int newy) {

		y = newy;
	} // end of setY method

	/**
	 * See if this state is the initial state.
	 * 
	 * @return true if it is, false if it is not.
	 */
	boolean isInitial() {

		return initial;
	} // end of isInitial method

	/**
	 * Get total number of output signals from this state.
	 * 
	 * @return the total number of output signals.
	 */
	int numOuts() {

		return outs.size();
	} // end of numOuts method

} // end of State class