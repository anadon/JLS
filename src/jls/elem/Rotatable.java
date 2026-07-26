package jls.elem;

import jls.core.Orientation;

/**
 * Capability interface (issue #78) for elements the user can reorient -
 * rotate a quarter turn and/or flip (mirror).
 *
 * <p>Like {@link Timed} and {@link Watchable}, this replaces one of
 * {@link Element}'s boolean-gated capability pairs: the
 * {@code canRotate()}/{@code canFlip()} predicates plus the
 * {@code rotate()}/{@code flip()} mutators that defaulted to
 * "unsupported" on the base class. An element that can be reoriented
 * declares that once, in the type system, by implementing
 * {@code Rotatable}; call sites {@code instanceof}-check the capability
 * instead of trusting a base-class default.
 *
 * <p>The predicates remain instance methods rather than being subsumed
 * by the {@code instanceof} check because they are attachment gates,
 * not constants: an element that could rotate when free refuses once a
 * wire is attached to any of its puts. The {@code instanceof} check
 * answers "can this kind of element ever be reoriented"; the predicate
 * answers "can this one, right now".
 *
 * <p>Rotation and flipping share one interface because they share the
 * attachment gate and the rebuild-the-puts mechanics, but an element
 * may support only one of the two (jump points and subcircuits flip
 * without rotating), so each half defaults to "unsupported" and an
 * implementor overrides the half it provides.
 *
 * <p>The interface is headless: {@link jls.core.Orientation} and
 * {@link jls.core.TextMetrics} are core types, so it stays inside the
 * enforced core surface (issue #77) and imposes no GUI dependency.
 */
public interface Rotatable {

	/**
	 * Whether this element can rotate right now. Typically false once
	 * any put has a wire attached, since rotating rebuilds the puts.
	 *
	 * @return true if the element currently supports rotation.
	 */
	default boolean canRotate() {

		return false;
	} // end of canRotate method

	/**
	 * Rotate the element a quarter turn. Only called when
	 * {@link #canRotate()} is true.
	 *
	 * @param direction The direction to rotate: {@code LEFT} for
	 *        counterclockwise, {@code RIGHT} for clockwise.
	 * @param g The current text metrics for use in recalculating size.
	 */
	default void rotate(Orientation direction,
			jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		throw new UnsupportedOperationException("Rotate");
	} // end of rotate method

	/**
	 * Whether this element can flip right now. Typically false once any
	 * put has a wire attached, since flipping rebuilds the puts.
	 *
	 * @return true if the element currently supports flipping.
	 */
	default boolean canFlip() {

		return false;
	} // end of canFlip method

	/**
	 * Flip (mirror) the element. Only called when {@link #canFlip()} is
	 * true.
	 *
	 * @param g The current text metrics for use in recalculating size.
	 */
	default void flip(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		throw new UnsupportedOperationException("Flip");
	} // end of flip method

} // end of Rotatable interface
