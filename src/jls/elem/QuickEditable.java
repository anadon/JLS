package jls.elem;

/**
 * Capability interface (issue #78) for elements that offer a "quick
 * change" (shortcut) menu in the editor's option popup.
 *
 * <p>This replaces the {@code quickChange()} predicate that defaulted
 * to false on {@link Element}: an element with a shortcut menu declares
 * the capability once, in the type system, and call sites
 * {@code instanceof}-check it. The Swing menu itself already lives on
 * the GUI side (issue #77's {@code jls.edit.ElementQuickMenus}), so
 * this interface stays headless. Only {@link Constant} has a quick
 * menu today.
 */
public interface QuickEditable {

} // end of QuickEditable interface
