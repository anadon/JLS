package jls.sim;

/**
 * A simulation event (typically a signal value change).
 * 
 * @author David A. Poplawski
 */
public final class SimEvent implements Comparable<SimEvent> {
	
	/** The next sequence number, assigned at construction (post order). */
	private static long sequence = 0;

	// properties (all set once in the constructor: a SimEvent is an
	// immutable value carrier, kept a plain class rather than a record
	// because its equals/hashCode are intentionally non-structural --
	// equals excludes seq so the simulator's dupCheck set can coalesce
	// duplicate postings; see jls.sim.SimEventDedupTest, issue #94)
	/** The simulation time this event fires at. */
	private final long time;
	/** The same-time tie-breaker: this event's global sequence number. */
	private final long seq;
	/** The element whose react runs when this event fires. */
	private final Reacts callBack;
	/** The event payload; null means "inputs changed, re-read them". */
	private final Object todo;
	
	/**
	 * Create a new event object with the given time and callback.
	 *
	 * @param time The time the event will occur
	 * @param callBack The object to tell when the event occurs.
	 * @param todo An object containing information about what the
	 *             reacting object should do about this event.
	 *             Null typically means an input pin has changed value.
	 */
	public SimEvent(long time, Reacts callBack, Object todo) {
		
		this.time = time;
		seq = sequence;
		this.callBack = callBack;
		this.todo = todo;
		sequence += 1;
	} // end of constructor
	
	/**
	 * Compares this SimEvent with another.
	 * Only the time and seq is used since compareTo is only used by the event
	 * queue.
	 *
	 * @param other The object to compare this one with.
	 *
	 * @return -1 if this object's time/seq is less than other's, 0 if the
	 *            times/seqs are equal, and +1 if this object's time/seq is
	 *            greater than other's.
	 */
	@Override
	public int compareTo(SimEvent other) {
		
		// compare times first
		if (this.time < other.time)
			return -1;
		if (this.time > other.time)
			return 1;
		
		// times are the same, so compare sequence numbers
		if (this.seq < other.seq)
			return -1;
		if (this.seq > other.seq)
			return 1;
		
		// this shouldn't happen since sequence numbers are unique
		return 0;
	} // end of compareTo method
	
	/**
	 * Decide whether this JLSEvent object and another are equal.
	 * They will be equal if the have the same time, the same callback
	 * object, and the same todo object.
	 *
	 * @param other The object to compare this one with.
	 *
	 * @return true if the objects are equal, false otherwise.
	 */
	@Override
	public boolean equals(Object other) {

		if (!(other instanceof SimEvent oth))
			return false;

		if (this.time != oth.time)
			return false;
		if (this.callBack != oth.callBack)
			return false;
		return this.todo == null ? oth.todo == null :
			this.todo.equals(oth.todo);
	} // end of equals method
	
	/**
	 * Return a hash code for this object.
	 * This must be consistent with the equals method, hence objects
	 * that are equal will have the same hash code.
	 * All objects with the same time have the same hash code.
	 *
	 * @return the time of the event.
	 */
	@Override
	public int hashCode() {
		
		return (int)time;
	} // end of hashCode method
	
	/**
	 * Get the time of the event.
	 *
	 * @return the time.
	 */
	public long getTime() {
		
		return time;
	} // end of getTime method
	
	/**
	 * Get the object that reacts to this event.
	 *
	 * @return a reference to the object that reacts to this event.
	 */
	public Reacts getCallBack() {
		
		return callBack;
	} // end of getCallBack method
	
	/**
	 * Get the todo object
	 *
	 * @return the todo object.
	 */
	public Object getTodo() {
		
		return todo;
	} // end of getTodo method
	
} // end of Event class
