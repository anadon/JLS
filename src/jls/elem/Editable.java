package jls.elem;

/**
 * Capability interface (issue #78) for elements the user can modify
 * after they are created and placed.
 *
 * <p>This replaces the {@code canChange()} predicate that defaulted to
 * false on {@link Element}: an editable element declares the capability
 * once, in the type system, and call sites {@code instanceof}-check it.
 * Every element whose {@code canChange()} returned true did so
 * unconditionally, so the capability is purely class-level and the
 * interface needs no methods - the change dialog itself already lives
 * on the GUI side (issue #77's {@code ElementDialogs} registry), keyed
 * by element class, which keeps this interface headless.
 */
public interface Editable {

} // end of Editable interface
