package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * Output pin of a subcircuit.
 * 
 * @author David A. Poplawski
 */
public class OutputPin extends Pin implements TriProp {

	// properties
	private boolean loadTriState = false;

	/**
	 * Create a new output pin.
	 * 
	 * @param circ
	 *            The circuit this pin will be in.
	 */
	public OutputPin(Circuit circ) {

		super(circ);
		orientation = JLSInfo.Orientation.RIGHT;
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string.
	 */
	public String toString() {
		
		return "OutputPin[" + super.toString() + "]";
	} // end of toString method

	/**
	 * Display dialog to get pin name and bits.
	 * 
	 * @param g
	 *            The Graphics object to use to determine the name's size.
	 * @param editWindow
	 *            The editor window this pin is displayed in.
	 * @param x
	 *            The x-coordinate of the last known mouse position.
	 * @param y
	 *            The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		return super.setup(g, editWindow, x, y, "Output");
	} // end of setup method

	/**
	 * Initialize internal info for this element. Most work done in superclass,
	 * but input point added here.
	 * 
	 * @param g
	 *            The Graphics object used to compute the size of the name.
	 */
	public void init(Graphics g) {

		super.init(g);
		if (orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input", this, 0, JLSInfo.spacing, bits));
		} else if (orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input", this, width, JLSInfo.spacing, bits));
		} else if (orientation == JLSInfo.Orientation.DOWN) {
			inputs.add(new Input("input", this, width / 2, 0, bits));
		} else if (orientation == JLSInfo.Orientation.UP) {
			inputs.add(new Input("input", this, width / 2, height, bits));
		}
	} // end of init method

	/**
	 * Draw this gate.
	 * 
	 * @param g
	 *            The graphics object to draw with.
	 */
	public void draw(Graphics g) {

		// set up shape
		int s = JLSInfo.spacing;
		Polygon p = new Polygon();
		if (orientation == JLSInfo.Orientation.RIGHT) {
			p.addPoint(x, y);
			p.addPoint(x + width - s, y);
			p.addPoint(x + width, y + height / 2);
			p.addPoint(x + width - s, y + height);
			p.addPoint(x, y + height);
		} else if (orientation == JLSInfo.Orientation.LEFT) {
			p.addPoint(x + s, y);
			p.addPoint(x, y + height / 2);
			p.addPoint(x + s, y + height);
			p.addPoint(x + width, y + height);
			p.addPoint(x + width, y);
		} else if (orientation == JLSInfo.Orientation.UP) {
			p.addPoint(x + width / 2, y);
			p.addPoint(x + width, y + s);
			p.addPoint(x + width, y + height);
			p.addPoint(x, y + height);
			p.addPoint(x, y + s);
		} else if (orientation == JLSInfo.Orientation.DOWN) {
			p.addPoint(x, y);
			p.addPoint(x + width, y);
			p.addPoint(x + width, y + height - s);
			p.addPoint(x + width / 2, y + height);
			p.addPoint(x, y + height - s);

		}
		// draw watched background
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillPolygon(p);
		}

		// draw context
		super.draw(g);

		// draw box
		g.setColor(Color.BLACK);
		g.drawPolygon(p);

		// draw name inside box
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D t = fm.getStringBounds(name, g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int bx = x;
		int by = y;
		int bwidth = width;
		int bheight = height;
		if (orientation == JLSInfo.Orientation.LEFT) {
			bx = x + s;
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.RIGHT) {
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.UP) {
			by = y + s;
			bheight = height - s;
		}
		else { // DOWN
			bheight = height - s;
		}
		int dx = (int) ((bwidth - tw) / 2);
		int dy = (int) ((bheight - th) / 2 + fm.getAscent());

		g.drawString(name, bx + dx, by + dy);

		// draw output
		inputs.get(0).draw(g);

	} // end of draw method

	/**
	 * Save this element.
	 * 
	 * @param output
	 *            The output writer.
	 */
	public void save(PrintWriter output) {

		output.println("ELEMENT OutputPin");
		if (getCircuit().isImported()) {
			SubCircuit sub = getCircuit().getSubElement();
			if (sub.getOutput(name).isTriState()) {
				output.println(" int tristate 1");
			}
		}
		super.save(output);
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name
	 *            The instance variable name.
	 * @param value
	 *            The instance variable value.
	 */
	public void setValue(String name, int value) {

		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name, value);
		}
	} // end of setValue method

	/**
	 * Copy this element.
	 */
	public Element copy() {

		OutputPin it = new OutputPin(circuit);
		it.name = name;
		it.orientation = orientation;
		it.bits = bits;
		it.watched = watched;
		it.inputs.add(inputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 * 
	 * @param info
	 *            The JLabel to display with.
	 */
	public void showInfo(JLabel info) {

		String value = ", value = " + BitSetUtils.toDisplay(currentValue, bits);
		String tri = "";
		if (inputs.get(0).isAttached()) {
			if (inputs.get(0).getWireEnd().getNet().isTriState())
				tri = " (tri-state) ";
		}
		info.setText(bits + " bit output pin" + tri + value);
	} // end of showInfo method

	/**
	 * Print current value to stdout.
	 * 
	 * @param qual
	 *            Qualified subcircuit name.
	 */
	public void printValue(String qual) {

		if (qual.equals("")) {
			System.out.printf("Output Pin %s: %s\n", name, BitSetUtils
					.toDisplay(currentValue, bits));
		} else {
			System.out.printf("Output Pin %s.%s: %s\n", qual, name, BitSetUtils
					.toDisplay(currentValue, bits));
		}

	} // end of printValue method

	/**
	 * Set this pin as tristate or not. If part of a subcircuit, propagate
	 * tristate status to output.
	 * 
	 * @param which
	 *            True to make this pin tristate, false to make it not.
	 */
	public void setTriState(boolean which) {

		if (!getCircuit().isImported())
			return;
		SubCircuit sub = getCircuit().getSubElement();
		Output put = (Output) sub.getPut(name);
		if (put != null)
			put.setTriState(which);
	} // end of setTriState

	/**
	 * See if this element is tristate at load time.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isLoadTriState() {

		return loadTriState;
	} // end of isLoadTriState

	/**
	 * Remove this element, but only if it is not part of a subcircuit.
	 * 
	 * @param circuit
	 *            The circuit this element is in.
	 */
	public void remove(Circuit circ) {

		if (circ.isImported()) {
			JOptionPane.showMessageDialog(JLSInfo.frame,
					"Can't remove output pin " + name + " from a subcircuit");
			return;
		}
		super.remove(circ);
	} // end of remove method

	/**
	 * Tells if an OutputPin is capable of rotatating, can only rotate when
	 * input has no attachment.
	 * 
	 * @return False if input has a wire attached, True otherwise
	 */
	public boolean canRotate() {
		return !inputs.get(0).isAttached();
	}

	/**
	 * This method will rotate the OutputPin if it is rotateable.
	 * 
	 * @param direction
	 *            The direction to rotate
	 * @param g
	 *            The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g) {
		if (direction == JLSInfo.Orientation.LEFT) {
			if (orientation == JLSInfo.Orientation.LEFT) {
				orientation = JLSInfo.Orientation.DOWN;
			} else if (orientation == JLSInfo.Orientation.DOWN) {
				orientation = JLSInfo.Orientation.RIGHT;
			} else if (orientation == JLSInfo.Orientation.RIGHT) {
				orientation = JLSInfo.Orientation.UP;
			} else if (orientation == JLSInfo.Orientation.UP) {
				orientation = JLSInfo.Orientation.LEFT;
			}

		} else if (direction == JLSInfo.Orientation.RIGHT) {
			if (orientation == JLSInfo.Orientation.LEFT) {
				orientation = JLSInfo.Orientation.UP;
			} else if (orientation == JLSInfo.Orientation.DOWN) {
				orientation = JLSInfo.Orientation.LEFT;
			} else if (orientation == JLSInfo.Orientation.RIGHT) {
				orientation = JLSInfo.Orientation.DOWN;
			} else if (orientation == JLSInfo.Orientation.UP) {
				orientation = JLSInfo.Orientation.RIGHT;
			}
		}
		inputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Tells if an OutputPin is capable of flipping, can only flip when input
	 * has no attachment.
	 * 
	 * @return False if input has a wire attached, True otherwise
	 */
	public boolean canFlip() {
		return !inputs.get(0).isAttached();
	}

	/**
	 * This method will flip an OutputPin
	 * 
	 * @param g
	 *            The current graphics context to facilitate recalculation of
	 *            size when flipping
	 */
	public void flip(Graphics g) {
		if (orientation == JLSInfo.Orientation.LEFT) {
			orientation = JLSInfo.Orientation.RIGHT;
		} else if (orientation == JLSInfo.Orientation.RIGHT) {
			orientation = JLSInfo.Orientation.LEFT;
		} else if (orientation == JLSInfo.Orientation.UP) {
			orientation = JLSInfo.Orientation.DOWN;
		} else if (orientation == JLSInfo.Orientation.DOWN) {
			orientation = JLSInfo.Orientation.UP;
		}
		inputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	// -------------------------------------------------------------------------------
	// Simulation
	// -------------------------------------------------------------------------------

	private BitSet currentValue = new BitSet();

	/**
	 * Get the current value.
	 * 
	 * @return the current value.
	 */
	public BitSet getCurrentValue() {

		if (currentValue == null)
			return null;
		else
			return (BitSet) currentValue.clone();
	} // end of getCurrentValue method

	/**
	 * Initialize current value to 0 or null.
	 * 
	 * @param sim
	 *            Unused.
	 */
	public void initSim(Simulator sim) {

		Input in = inputs.get(0);
		if (in.isAttached()) {
			WireEnd end = in.getWireEnd();
			if (end.isTriState()) {
				currentValue = null;
				return;
			}
		}
		currentValue = new BitSet();
	} // end of initSim method

	/**
	 * React to an event by sending the input value to the output pin in the
	 * containing subcircuit element, unless this circuit is not a subcircuit.
	 * 
	 * @param now
	 *            The current simulation time.
	 * @param sim
	 *            The simulator to post events to.
	 * @param todo
	 *            Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {

		// send to output
		Input in = inputs.get(0);
		BitSet value = in.getValue();
		if (value == null)
			currentValue = null;
		else
			currentValue = (BitSet) value.clone();
		if (circuit.isImported()) {
			SubCircuit sub = circuit.getSubElement(); // the subcircuit
														// element
			sub.send(this, value, now, sim);
		}

	} // end of react method

	/**
	 * Display current value.
	 * 
	 * @param where
	 *            Unused.
	 */
	public void showCurrentValue(Point where) {

		JOptionPane.showMessageDialog(JLSInfo.frame, BitSetUtils.toDisplay(
				currentValue, bits), "Information",
				JOptionPane.INFORMATION_MESSAGE);
	} // end of showCurrentValue method

} // end of OutputPin class
