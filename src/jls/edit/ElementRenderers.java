package jls.edit;

import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jls.elem.Element;

/**
 * The GUI-side element draw registry (issue #77). Top-level element
 * drawing is dispatched through {@link #draw} instead of calling
 * {@code element.draw(g)} directly, so an element whose rendering has
 * moved off AWT into an {@link ElementRenderer} is drawn by that
 * renderer, while an unconverted element still draws itself. The registry
 * is empty until the element wave populates
 * {@link BuiltinElementRenderers#install()}, so this dispatch is
 * byte-identical to the direct call when no renderer is registered.
 */
public final class ElementRenderers {

	private static final Map<Class<?>, ElementRenderer> BY_TYPE =
			new HashMap<Class<?>, ElementRenderer>();

	static {
		BuiltinElementRenderers.install();
	}

	private ElementRenderers() {} // no instances

	/**
	 * Register the renderer for an element type.
	 *
	 * @param type The element class.
	 * @param renderer Its renderer.
	 */
	public static void register(Class<? extends Element> type,
			ElementRenderer renderer) {
		BY_TYPE.put(type, renderer);
	} // end of register method

	/**
	 * Draw an element through its registered renderer, or through the
	 * element's own {@code draw} if none is registered.
	 *
	 * @param g The graphics to draw on.
	 * @param el The element to draw.
	 */
	public static void draw(Graphics g, Element el) {
		@Nullable ElementRenderer renderer = BY_TYPE.get(el.getClass());
		if (renderer != null) {
			renderer.draw(g, el);
		} else {
			ElementRenderSupport.drawHighlight(g, el);
		}
	} // end of draw method

} // end of ElementRenderers class
