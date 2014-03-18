package jls.sim;

/**
 * Implemented by all elements that react to simulator events
 * (input/output signal changes).
 * 
 * @author David A. Poplawski
 */
public interface Reacts {
	
	/**
	 * Initializes the element.
	 * Typically output pins are given values and internal state is initialized.
	 * 
	 * @param sim A reference to the simulator in case the element needs to post an event.
	 */
	public abstract void initSim(Simulator sim);

	/**
	 * 
	 * @param now The current simulation time.
	 * @param sim A reference to the simulator in case element needs to post an event.
	 * @param todo Information needed to react to an event.
	 *             This will typically be null for input pin signal changes.
	 */
    public abstract void react(long now, Simulator sim, Object todo);

    } // end of Reacts interface
