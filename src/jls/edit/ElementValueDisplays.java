package jls.edit;

import jls.elem.Element;
import jls.elem.Memory;

/**
 * GUI-side dispatch for "show this element's current value" (issue #77).
 * Most watchable elements report their value through {@code TellUser}
 * (headless-safe) from {@link Element#showCurrentValue(int,int)}; the one
 * element with a rich Swing window — {@link Memory} — has its window here
 * so the model holds no Swing. The editor calls this instead of
 * {@code element.showCurrentValue(...)} directly.
 */
public final class ElementValueDisplays {

	private ElementValueDisplays() {
	} // no instances

	/**
	 * Show an element's current value: the memory-contents window for a
	 * {@link Memory}, otherwise the element's own value report.
	 *
	 * @param el The element whose value to show.
	 * @param x The screen x-coordinate of the request.
	 * @param y The screen y-coordinate of the request.
	 */
	public static void show(Element el, int x, int y) {

		if (el instanceof Memory mem) {
			MemoryContentsDialog.show(mem);
			return;
		}
		el.showCurrentValue(x, y);
	} // end of show method

} // end of ElementValueDisplays class
