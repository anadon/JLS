package jls.elem;

/**
 * Capability interface (issue #78) for elements that carry a timing
 * value - a gate/logic propagation delay or a memory access time.
 *
 * <p>This is one of the capability interfaces that replace the base
 * class's boolean-gated method pairs (the {@code hasTiming()} predicate
 * plus the {@code getDelay()}/{@code setDelay()} accessors that default
 * to a no-op on {@link Element}). An element that owns a timing value
 * declares that fact once, in the type system, by implementing
 * {@code Timed}; call sites {@code instanceof}-check the capability
 * instead of the base class branching on concrete leaf types.
 *
 * <p>Concretely, this interface retires the {@code this instanceof
 * Memory} branch that {@link Element#changeTiming()} used only to pick
 * the timing dialog's wording ("access time" vs "propagation delay") -
 * the audit's "base knows its leaves" smell (F3). That distinction now
 * belongs to the element, through {@link #usesAccessTime()}.
 *
 * <p>The interface is deliberately headless: it names no AWT, Swing, or
 * editor type, so it stays inside the enforced core surface (issue #77)
 * and imposes no GUI dependency on the elements that implement it.
 */
public interface Timed {

	/**
	 * The element's current timing value, in simulation time units.
	 *
	 * @return the propagation delay or access time.
	 */
	int getDelay();

	/**
	 * Set the element's timing value.
	 *
	 * @param delay The new propagation delay or access time; callers
	 *              enforce that it is positive.
	 */
	void setDelay(int delay);

	/**
	 * Whether this element's timing value is a memory <em>access time</em>
	 * rather than a logic <em>propagation delay</em>. The two differ only
	 * in how the timing-change dialog labels the field; simulation treats
	 * both as an integer delay.
	 *
	 * @return {@code true} if the value is an access time (memory-style),
	 *         {@code false} for a propagation delay (the default).
	 */
	default boolean usesAccessTime() {

		return false;
	} // end of usesAccessTime method

} // end of Timed interface
