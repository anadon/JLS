package jls.elem;

/**
 * Capability interface (issue #78) for elements whose live value the
 * user can "watch" - have JLS annotate on the circuit during simulation.
 *
 * <p>Like {@link Timed}, this replaces one of {@link Element}'s
 * boolean-gated capability pairs: the {@code canWatch()} predicate plus
 * the {@code isWatched()}/{@code setWatched(boolean)} state that default
 * to "never watched" on the base class. An element that can be watched
 * declares it once by implementing {@code Watchable}; call sites
 * {@code instanceof}-check the capability rather than trusting a
 * base-class default that most elements silently inherit.
 *
 * <p>The interface names no AWT, Swing, or editor type, so it stays
 * inside the enforced headless core surface (issue #77): whether an
 * element is watched is model state, independent of how the GUI renders
 * the annotation.
 */
public interface Watchable {

	/**
	 * Whether this element is currently marked as watched.
	 *
	 * @return {@code true} if the element is being watched.
	 */
	boolean isWatched();

	/**
	 * Mark this element as watched or not.
	 *
	 * @param state {@code true} to watch the element, {@code false} to
	 *             stop.
	 */
	void setWatched(boolean state);

} // end of Watchable interface
