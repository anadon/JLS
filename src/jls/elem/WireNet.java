package jls.elem;

import jls.*;
import jls.sim.*;
import java.util.*;

/**
 * Keeps track of all wires and wire ends in a "net", which is a multi-segment wire,
 * possibly with branches (a DAG).
 * 
 * @author David A. Poplawski
 */
public class WireNet {
	
	// properties
	// insertion order (file order for a loaded circuit) makes the
	// multi-driver resolution in propagate deterministic (issue #98, S1)
	/** The wire ends in this net, in insertion order. */
	private Set<WireEnd>ends = new LinkedHashSet<WireEnd>();	// the wire ends in this net
	/** The wires in this net, in insertion order. */
	private Set<Wire>wires = new LinkedHashSet<Wire>();	// the wires in this net
	/** The number of bits (0=not connected). */
	private int bits = 0;								// the number of bits (0=not connected)
	/** True once this net is connected to an Output. */
	private boolean hasinput = false;					// connected to an Output yet?
	/** True if this net is tri-stated. */
	private boolean triState = false;					// true if tri-stated

	/**
	 * Create an empty wire net; wires and wire ends are added as they
	 * are connected.
	 */
	public WireNet() {
	} // end of constructor

	/**
	 * Make a copy of this element.
	 * Wirenets are not copied.
	 * 
	 * @return a reference to a complete copy of this element.
	 */
	public Element copy() {
		return null;
	} // end of copy method
	
	/**
	 * Add a new wire end.
	 * 
	 * @param end The new wire end.
	 * @jls.testedby jls.edit.TriStateBundleConnectTest#freshEnd()
	 */
	public void add(WireEnd end) {
		
		ends.add(end);
	} // end of add method
	
	/**
	 * Remove wire end.
	 * 
	 * @param end The wire end to remove.
	 */
	public void remove(WireEnd end) {
		
		ends.remove(end);
	} // end of remove method
	
	/**
	 * Add a new wire.
	 * 
	 * @param wire The new wire.
	 */
	public void add(Wire wire) {
		
		wires.add(wire);
	} // end of add method
	
	/**
	 * Remove wire.
	 * 
	 * @param wire The wire to remove.
	 */
	public void remove(Wire wire) {
		
		wires.remove(wire);
	} // end of remove method
	
	/**
	 * Create a new wire net containing a given wire end (that must be in this net).
	 * 
	 * @param end The wire end that is part of the new net.
	 * 
	 * @return The new wire net.
	 */
	public WireNet makeNet(WireEnd end) {
		
		// unmark all wires and wire ends
		for (Wire w : wires) {
			w.mark(false);
		}
		for (WireEnd e : ends) {
			e.mark(false);
		}
		
		// mark all wires and wire ends connected to the given wire end
		end.traverse();
		
		// create new empty wire net
		WireNet net = new WireNet();
		
		// add marked wires and wire ends to the new wire net
		for (Wire w : wires) {
			if (w.isMarked()) {
				net.add(w);
				w.setNet(net);
			}
		}
		for (WireEnd e : ends) {
			if (e.isMarked()) {
				net.add(e);
				e.setNet(net);
			}
		}
		
		//  assume this wire net is not attached to any puts
		bits = 0;
		hasinput = false;
		
		// if any wire end is attached
		for (WireEnd e : net.ends) {
			if (e.isAttached()) {
				
				// set bits and, if an output set hasinput and possibly tristate
				Put put = e.getPut();
				net.bits = put.getBits();
				if (put instanceof Output out) {
					net.hasinput = true;
					if (out.isTriState()) {
						net.setTriState(true);
					}
				}
			}
		}
		
		// un-tristate any elements if needed
		if (!net.isTriState()) {
			for (WireEnd e : net.ends) {
				if (!e.isAttached())
					continue;
				Element el = e.getPut().getElement();
				if (el instanceof TriProp pin) {
					pin.setTriState(false);
				}
			}
		}
		
		return net;
	} // end of makeNet method
	
	/**
	 * See if there is anything in this wire net.
	 * 
	 * @return true if there is at least one wire end, false if not.
	 */
	public boolean isEmpty() {
		
		return ends.isEmpty();
	} // end of esEmpty method
	
	/**
	 * See if this net is already connected to an output.
	 * 
	 * @return true if already connected to an output, false if not.
	 */
	public boolean hasInput() {
		
		return hasinput;
	} // end of hasInput method
	
	/**
	 * Tell this wire net that it is now connected to an output.
	 */
	public void setInput() {
		
		hasinput = true;
	} // end of setInput method
	
	/**
	 * Not visible, so always return false.
	 *
	 * @return false.
	 */
	public boolean contains() {

		return false;
	} // end of contains method

	/**
	 * Not visible, so always return false.
	 *
	 * @return false.
	 */
	public boolean intersects() {

		return false;
	} // end of intersects method

	/**
	 * Not visible, so always return false.
	 *
	 * @return false.
	 */
	public boolean isInside() {

		return false;
	} // end of isInside method
	
	/**
	 * Set the number of bits in this wire net.
	 * 
	 * @param bits the number of bits.
	 */
	public void setBits(int bits) {
		
		this.bits = Math.max(this.bits,bits);
	} // end of setBits method
	
	/**
	 * Get the number of bits in this wire net.
	 * (0 implies the net is not connected to any elements yet)
	 * 
	 * @return the number of bits, or 0 if not connected.
	 */
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Add all wires and wire nets from given net into this one.
	 * 
	 * @param other The net to absorb.
	 */
	public void absorb(WireNet other) {
		
		for (WireEnd end : other.ends) {
			ends.add(end);
			end.setNet(this);
		}
		for (Wire wire : other.wires) {
			wires.add(wire);
			wire.setNet(this);
		}
	} // end of absorb method
	
	/**
	 * Check for this wire net being disconnected from any elements after some
	 * attached element is deleted.
	 * If no longer attached to some output, reset hasinput.
	 * Reset bits to max of whatever net is connected to (i.e., will become
	 * 0 if only connected to a variable-bit element like a constant).
	 * Reset tristate to false if no longer connected to a tristate.
	 * If no longer tristate, fix any attached elements.
	 */
	public void recheck() {
		
		hasinput = false;
		bits = 0;
		triState = false;
		for (WireEnd end : ends) {
			if (end.getPut() != null) {
				bits = Math.max(end.getPut().getBits(),bits);
			}
			if (end.getPut() instanceof Output out) {
				hasinput = true;
				if (out.isTriState()) {
					triState = true;
				}
			}
		}
		if (!triState) {
			for (WireEnd end : ends) {
				if (!end.isAttached())
					continue;
				Element el = end.getPut().getElement();
				if (el instanceof TriProp pin) {
					pin.setTriState(false);
				}
			}
		}
	} // end of recheck method
	
	/**
	 * See if more than one wire or wire end from this net overlaps with wire ends
	 * from another net.
	 * 
	 * @param other The other wire net.
	 * 
	 * @return true if more than one overlap, false if not.
	 */
	public boolean netOverlap(WireNet other) {
		
		int overlaps = 0;
		for (WireEnd end1 : ends) {
			for (WireEnd end2 : other.ends) {
				if (end1.intersects(end2)) {
					overlaps += 1;
				}
			}
		}
		for (Wire wire : wires) {
			for (WireEnd end2 : other.ends) {
				if (wire.contains(end2.getX(),end2.getY())) {
					overlaps += 1;
				}
			}
		}
		return overlaps > 1;
	} // end of netOverlap method
	
	/**
	 * Get all wire ends in this net.
	 * 
	 * @return all wire ends.
	 * @jls.testedby jls.edit.CtrlWGestureTest#startWireClearsSelectionAndSelectsNewEnd()
	 * @jls.testedby jls.edit.CtrlWGestureTest#startWireFromEmptySelectionMatchesOldBehavior()
	 * @jls.testedby jls.ui.CircuitAssert#reaches()
	 */
	public Set<WireEnd> getAllEnds() {
		
		return ends;
	} // end of getAllEnds method

	/**
	 * Make this wire net tri-state when loading circuit.
	 */
	public void loadTriState() {
		
		triState = true;
	} // end of setTriState method
	
	/**
	 * Make this wire net tri-state or not.
	 * Also change any attached elements.
	 * 
	 * @param which True to make it tri-state, false to make it not.
	 */
	public void setTriState(boolean which) {
		
		// set initial value
		if (which)
			value = null;
		
		// set this net
		triState = which;
		
		// for each wire end...
		for (WireEnd end : ends) {
			
			// skip unattached ends
			if (!end.isAttached())
				continue;
			
			// skip ends attached to outputs
			if (end.getPut() instanceof Output)
				continue;
			
			// if attached to a tri-state propagating element...
			Element el = end.getPut().getElement();
			if (el instanceof TriProp tel) {
				
				// propagate tri-state
				tel.setTriState(which);
			}
		}
	} // end of setTriState method
	
	/**
	 * Find out if this wire net is tri-state.
	 * 
	 * @return true if tri-state, false if not.
	 */
	public boolean isTriState() {
		
		return triState;
	} // end of isTriState method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
		
	/** The current value on this net (null when tri-stated off). */
	private BitSet value = new BitSet(1);
	/** True once a bus conflict has been reported, until it clears. */
	private boolean conflictReported = false;	// bus-conflict warned already? (#98, S1)

	/**
	 * Set the value on this net.
	 * Should only be used by initSim.
	 * 
	 * @param value The value.
	 */
	public void setValue(BitSet value) {
		
		if (value == null)
			this.value = null;
		else
			this.value = (BitSet)value.clone();
	} // end of setValue method

	/**
	 * Get the current value on this net.
	 * 
	 * @return the current value.
	 */
	public BitSet getValue() {
		
		if (value == null)
			return null;
		else
			return (BitSet)value.clone();
	} // end of getValue method
	
	/**
	 * Send a copy of the value to all inputs this net is connected to.
	 * 
	 * @param value The value to send.
	 * @param now The current time.
	 * @param sim The simulator object to post events to.
	 */
	public void propagate(BitSet value, long now, Simulator sim) {
		
		// if tristate, resolve the value actually driven: null (HiZ) if
		// every driver is off, otherwise the first active driver in net
		// order (the order the wire ends were added to the net - file
		// order for a loaded circuit).  With at most one active driver -
		// the only configuration with defined meaning - that driver wins.
		// Two or more simultaneously active drivers with different values
		// are a bus conflict: the resolution stays deterministic (first
		// active driver in net order) and the user is told once, until
		// the conflict clears (issue #98, S1).
		if (triState) {
			BitSet actual = null;
			boolean conflict = false;
			for (WireEnd end : ends) {
				if (!end.isAttached())
					continue;
				Put p = end.getPut();
				if (!(p instanceof Output out))
					continue;
				if (out.getValue() != null) {
					if (actual == null) {
						actual = out.getValue();
					}
					else if (!actual.equals(out.getValue())) {
						conflict = true;
					}
				}
			}
			if (!conflict) {
				conflictReported = false;
			}
			else if (!conflictReported) {
				conflictReported = true;
				TellUser.warn(JLSInfo.frame,
						"bus conflict at time " + now
						+ ": two or more tri-state drivers are simultaneously"
						+ " active with different values on one wire net;"
						+ " the first active driver in net order wins",
						"Simulation");
			}
			value = actual;
		}
		
		// for each wire end attached to an input...
		for (WireEnd end : ends) {
			if (!end.isAttached())
				continue;
			Put p = end.getPut();
			if (!(p instanceof Input))
				continue;
			
			// make a copy of the value
			BitSet newValue = null;
			if (value != null)
				newValue = (BitSet)value.clone();
			
			// send it to the input
			Input inp = (Input)p;
			inp.setValue(newValue);
			Reacts element = (Reacts) p.getElement();
			sim.post(new SimEvent(now,element,null));
		}
		
		// keep a copy for probes
		if (value == null)
			this.value = null;
		else
			this.value = (BitSet)value.clone();

		// feed probed nets to the batch VCD trace (issue #200): a probe
		// names this net, so its value history is the net's. This is a
		// no-op in the interactive engine (Simulator.probeSample is
		// empty) and cheap otherwise - one field check per wire.
		for (Wire wire : wires) {
			if (wire.hasProbe()) {
				sim.probeSample(wire.getProbe(), bits, now, this.value);
			}
		}

	} // end of propagate method

} // end of WireNet class