package jls.sim;

import java.util.BitSet;

import jls.elem.State;

/**
 * A simulation event (typically a signal value change).
 *
 * @author David A. Poplawski
 */
public final class SimEvent implements Comparable<SimEvent> {

	/**
	 * What the reacting element should do when this event fires
	 * (issue #95). Sealed over the records in this compilation unit so
	 * every react() can switch exhaustively with no default arm: adding
	 * a payload kind is a compile error at every consumer until it is
	 * handled. The records replace the old untyped Object todo and its
	 * sentinels (null meant "inputs changed", the String "off" meant
	 * "turn the tri-state output off").
	 */
	public sealed interface Payload {
	} // end of Payload interface

	/**
	 * An input signal changed: re-read the inputs and react to their
	 * current values (formerly the null todo sentinel).
	 */
	public record PinChanged() implements Payload {
	} // end of PinChanged record

	/**
	 * A scheduled output change arriving: drive the given value on the
	 * output (formerly a raw BitSet todo).
	 *
	 * @param value The value the output should take on.
	 */
	public record NewValue(BitSet value) implements Payload {
	} // end of NewValue record

	/**
	 * A scheduled turn-off arriving: stop driving the output
	 * (propagate the undriven/HiZ value; formerly the String "off"
	 * sentinel and null-BitSet postings).
	 */
	public record TriStateOff() implements Payload {
	} // end of TriStateOff record

	/**
	 * A state machine transition completing: enter the new state and
	 * assert its outputs.
	 *
	 * @param state The state the machine is entering.
	 */
	public record StateChanged(State state) implements Payload {
	} // end of StateChanged record

	/**
	 * A memory write completing: store the data in the addressed word.
	 *
	 * @param address The word address being written.
	 * @param data The value to store.
	 */
	public record MemoryWrite(int address, BitSet data) implements Payload {
	} // end of MemoryWrite record

	/**
	 * A memory read completing: drive the addressed word on the output.
	 *
	 * @param address The word address being read.
	 */
	public record MemoryRead(int address) implements Payload {
	} // end of MemoryRead record

	/**
	 * A truth table output change completing: drive the value on the
	 * output pin at the given position.
	 *
	 * @param position Index of the output pin in the outputs list.
	 * @param value The value the output pin should take on.
	 */
	public record TableOutput(int position, BitSet value) implements Payload {
	} // end of TableOutput record

	/** The next sequence number, assigned at construction (post order). */
	private static long sequence = 0;

	// properties (all set once in the constructor: a SimEvent is an
	// immutable value carrier, kept a plain class rather than a record
	// because its equals/hashCode intentionally exclude seq (so the
	// simulator's dupCheck set can coalesce duplicate postings) and
	// compare the callback by reference identity; see
	// jls.sim.SimEventDedupTest, issues #94 and #231)
	/** The simulation time this event fires at. */
	private final long time;
	/** The same-time tie-breaker: this event's global sequence number. */
	private final long seq;
	/** The element whose react runs when this event fires. */
	private final Reacts callBack;
	/** The event payload: what the reacting element should do. */
	private final Payload todo;

	/**
	 * Create a new event object with the given time and callback.
	 *
	 * @param time The time the event will occur
	 * @param callBack The object to tell when the event occurs.
	 * @param todo The payload saying what the reacting object should do
	 *             about this event. PinChanged means an input signal
	 *             has changed value.
	 */
	public SimEvent(long time, Reacts callBack, Payload todo) {

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
	 * object, and the same todo payload.
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
		return this.todo.equals(oth.todo);
	} // end of equals method

	/**
	 * Return a hash code for this object.
	 * This must be consistent with the equals method, hence objects
	 * that are equal will have the same hash code.  The hash mixes the
	 * time, the callback's identity hash (matching equals' reference
	 * comparison of the callback), and the payload's structural hash,
	 * so same-time events with different callbacks or payloads spread
	 * across hash buckets instead of colliding (issue #231).
	 *
	 * @return the mixed hash of time, callback identity, and payload.
	 */
	@Override
	public int hashCode() {

		int h = Long.hashCode(time);
		h = 31 * h + System.identityHashCode(callBack);
		h = 31 * h + todo.hashCode();
		return h;
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
	 * Get the todo payload.
	 *
	 * @return the payload saying what the reacting element should do.
	 */
	public Payload getTodo() {

		return todo;
	} // end of getTodo method

} // end of Event class
