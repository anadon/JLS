import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * GuiDriver -- a pure-JDK, offline "Squish-class" GUI automation driver for
 * the JLS Java Logic Simulator.
 *
 * It boots the genuine JLS application in-process (jls.JLS.main), then drives
 * it with REAL OS-level input via java.awt.Robot: the same XTEST mouse and
 * keyboard events a human at the keyboard produces. Component targeting is
 * object-based -- it walks the live Swing tree and reads getLocationOnScreen()
 * -- so we click the actual "File" menu, the actual palette button, the actual
 * dialog field, never a hard-coded pixel. This is the same strategy commercial
 * tools like froglogic Squish and open-source AssertJ-Swing/Jemmy use; here it
 * is hand-rolled in the JDK so it needs no network and no license.
 *
 * Nothing here writes .jls text: every element and wire that ends up in the
 * circuit gets there because the editor's own event handlers ran in response
 * to a real click or drag -- construction strictly through the GUI.
 */
public final class GuiDriver {

	final Robot robot;
	private final Rectangle screen;
	public static boolean VERBOSE = false;

	private static void trace(String s) {
		if (VERBOSE) {
			System.err.println("[wire] " + s);
			System.err.flush();
		}
	}

	public GuiDriver() throws Exception {
		robot = new Robot();
		robot.setAutoDelay(30);
		robot.setAutoWaitForIdle(true);
		screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
	}

	// ---------------------------------------------------------------
	// boot
	// ---------------------------------------------------------------

	/** Launch the real JLS app on a daemon thread and wait for its frame. */
	public static Frame boot() throws Exception {
		Thread t = new Thread(() -> {
			try {
				jls.JLS.main(new String[] {});
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}, "jls-main");
		t.setDaemon(true);
		t.start();
		long deadline = System.currentTimeMillis() + 40000;
		while (System.currentTimeMillis() < deadline) {
			Frame f = jls.JLSInfo.frame;
			if (f != null && f.isShowing()) {
				Thread.sleep(500);
				// Neutralize JLS's global uncaught-exception handler: it pops
				// a MODAL "unexpected internal error" dialog that would block
				// the driver's invokeAndWait calls and hang the run. A driver
				// failure should surface as a stack trace, not a stuck modal.
				Thread.setDefaultUncaughtExceptionHandler((th, e) -> {
					System.err.println("UNCAUGHT on " + th.getName() + ": " + e);
					e.printStackTrace();
				});
				return f;
			}
			Thread.sleep(100);
		}
		throw new RuntimeException("JLS frame did not appear");
	}

	/** Park the frame at a known spot so canvas coordinates are stable. */
	public void placeFrame(Frame f, int w, int h) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			f.setLocation(0, 0);
			f.setSize(w, h);
			f.setAlwaysOnTop(true);
			f.toFront();
			f.requestFocus();
		});
		robot.delay(300);
	}

	// ---------------------------------------------------------------
	// component lookup (object-based targeting)
	// ---------------------------------------------------------------

	private void collect(Component c, List<Component> out) {
		out.add(c);
		if (c instanceof Container) {
			for (Component k : ((Container) c).getComponents()) {
				collect(k, out);
			}
		}
	}

	/** Every component under every showing window, top-down. */
	public List<Component> allComponents() {
		List<Component> out = new ArrayList<>();
		for (Window w : Window.getWindows()) {
			if (w.isShowing()) {
				collect(w, out);
			}
		}
		return out;
	}

	public Component find(Predicate<Component> p) {
		for (Component c : allComponents()) {
			try {
				if (p.test(c)) {
					return c;
				}
			} catch (Exception ignore) {
				// a component may be mid-teardown; skip it
			}
		}
		return null;
	}

	public Component waitFind(Predicate<Component> p, String what, long ms)
			throws Exception {
		long deadline = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < deadline) {
			Component c = find(p);
			if (c != null && c.isShowing()) {
				return c;
			}
			robot.delay(60);
		}
		throw new RuntimeException("timed out waiting for: " + what);
	}

	// common matchers ------------------------------------------------

	public static Predicate<Component> menuText(String t) {
		return c -> c instanceof JMenu && t.equals(((JMenu) c).getText());
	}

	public static Predicate<Component> menuItemText(String t) {
		return c -> c instanceof JMenuItem
				&& t.equals(((JMenuItem) c).getText()) && c.isShowing();
	}

	public static Predicate<Component> buttonText(String t) {
		return c -> c instanceof AbstractButton
				&& t.equals(((AbstractButton) c).getText()) && c.isShowing();
	}

	/** A palette element button carries the element name as its tooltip. */
	public static Predicate<Component> paletteTip(String tip) {
		return c -> c instanceof AbstractButton
				&& tip.equals(((AbstractButton) c).getToolTipText())
				&& c.isShowing();
	}

	public static Predicate<Component> anyTextField() {
		return c -> c instanceof JTextComponent && c.isShowing()
				&& ((JTextComponent) c).isEditable();
	}

	// ---------------------------------------------------------------
	// real input
	// ---------------------------------------------------------------

	public Point center(Component c) {
		Point p = c.getLocationOnScreen();
		Dimension d = c.getSize();
		return new Point(p.x + d.width / 2, p.y + d.height / 2);
	}

	public void clickAt(int x, int y) {
		robot.mouseMove(x, y);
		robot.delay(20);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(20);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(20);
	}

	public void click(Component c) {
		Point p = center(c);
		clickAt(p.x, p.y);
	}

	/** Press-move-release drag in screen coordinates, with interstitial
	 *  moves so the editor's drag handlers see motion (Robot best practice). */
	public void drag(int x0, int y0, int x1, int y1) {
		robot.mouseMove(x0, y0);
		robot.delay(40);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(40);
		int steps = 8;
		for (int i = 1; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps;
			int y = y0 + (y1 - y0) * i / steps;
			robot.mouseMove(x, y);
			robot.delay(15);
		}
		robot.delay(40);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(40);
	}

	public void tap(int keyCode) {
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
	}

	/** Pin keyboard focus onto the edit canvas via the real focus
	 *  subsystem, the reliable path under a headless WM (mirrors the
	 *  in-repo harness's focusCanvas()). */
	public void focusCanvas() throws Exception {
		Component cv = canvas();
		for (int i = 0; i < 100; i++) {
			SwingUtilities.invokeAndWait(cv::requestFocusInWindow);
			Component o = java.awt.KeyboardFocusManager
					.getCurrentKeyboardFocusManager().getFocusOwner();
			if (o == cv) {
				return;
			}
			robot.delay(20);
		}
	}

	/** Dispatch a key press to the current focus owner (the canvas), the
	 *  faithful key path the repo's construction tests use: it routes
	 *  through the component's real InputMap/ActionMap -- the same action a
	 *  physical key fires -- without depending on Robot key routing, which
	 *  is unreliable under a headless WM. */
	public void keyToCanvas(int keyCode) throws Exception {
		keyToCanvasMod(keyCode, 0);
	}

	public void keyToCanvasMod(int keyCode, int mods) throws Exception {
		Component owner = java.awt.KeyboardFocusManager
				.getCurrentKeyboardFocusManager().getFocusOwner();
		if (owner == null) {
			owner = canvas();
		}
		final Component o = owner;
		final long w = System.currentTimeMillis();
		SwingUtilities.invokeAndWait(() -> {
			o.dispatchEvent(new KeyEvent(o, KeyEvent.KEY_PRESSED, w, mods,
					keyCode, KeyEvent.CHAR_UNDEFINED));
			o.dispatchEvent(new KeyEvent(o, KeyEvent.KEY_RELEASED, w + 1, mods,
					keyCode, KeyEvent.CHAR_UNDEFINED));
		});
		robot.delay(40);
	}

	/** Bring the editor to a clean idle state before a palette action:
	 *  dismiss any stray dialog with Cancel, re-assert the frame, and press
	 *  Escape on the canvas. The editor's setup() only opens a creation
	 *  dialog when idle, so accumulated state from a prior gesture would
	 *  otherwise silently swallow the next palette click. */
	public void ensureIdle() throws Exception {
		for (int i = 0; i < 3 && dialogUp(); i++) {
			AbstractButton cancel = null;
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog && w.isShowing()) {
					for (Component c : descend(w)) {
						if (c instanceof AbstractButton b && b.isShowing()
								&& ("Cancel".equals(b.getText())
										|| "OK".equals(b.getText()))) {
							cancel = b;
						}
					}
				}
			}
			if (cancel != null) {
				click(cancel);
			}
			robot.delay(150);
		}
		Frame fr = jls.JLSInfo.frame;
		if (fr != null) {
			final Frame ff = fr;
			SwingUtilities.invokeAndWait(() -> {
				ff.setAlwaysOnTop(true);
				ff.toFront();
			});
		}
		focusCanvas();
		keyToCanvas(KeyEvent.VK_ESCAPE);
		robot.delay(80);
	}

	/** The newly-added element that is not a wire artifact (coincidence
	 *  placement also spawns WireEnds/Wires). */
	private jls.elem.Element newRealElement(
			java.util.Set<jls.elem.Element> before) {
		for (jls.elem.Element e : currentCircuit().getElements()) {
			if (!before.contains(e) && !(e instanceof jls.elem.WireEnd)
					&& !(e instanceof jls.elem.Wire)) {
				return e;
			}
		}
		return null;
	}

	/** Select a single element with a rubber-band drag around it (a plain
	 *  click would enter move mode and clear the selection on release).
	 *  Encloses only this element, so call it while the element is isolated
	 *  from neighbours. */
	public void select(jls.elem.Element el) throws Exception {
		focusCanvas();
		java.awt.Rectangle r = el.getRect();
		Point p0 = modelToScreen(r.x - 8, r.y - 8);
		Point p1 = modelToScreen(r.x + r.width + 8, r.y + r.height + 8);
		drag(p0.x, p0.y, p1.x, p1.y);
		robot.delay(100);
	}

	/** Toggle "watch" on an element (batch mode reports watched registers /
	 *  output pins): select it, then fire the editor's Ctrl+W watch action. */
	public void watch(jls.elem.Element el) throws Exception {
		select(el);
		keyToCanvasMod(KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK);
		robot.delay(120);
		// a watch dialog may pop (choose watched puts); accept it
		if (dialogUp()) {
			AbstractButton ok = dialogButton("OK");
			if (ok != null) {
				click(ok);
			}
			waitNoDialog(3000);
		}
	}

	public void shiftTap(int keyCode) {
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}

	/** Type a short ASCII string with real key events. */
	public void type(String s) {
		for (char ch : s.toCharArray()) {
			typeChar(ch);
			robot.delay(10);
		}
	}

	private void typeChar(char ch) {
		if (ch >= 'a' && ch <= 'z') {
			tap(KeyEvent.VK_A + (ch - 'a'));
		} else if (ch >= 'A' && ch <= 'Z') {
			shiftTap(KeyEvent.VK_A + (ch - 'A'));
		} else if (ch >= '0' && ch <= '9') {
			tap(KeyEvent.VK_0 + (ch - '0'));
		} else if (ch == '_') {
			shiftTap(KeyEvent.VK_MINUS);
		} else if (ch == '-') {
			tap(KeyEvent.VK_MINUS);
		} else if (ch == ' ') {
			tap(KeyEvent.VK_SPACE);
		} else if (ch == '.') {
			tap(KeyEvent.VK_PERIOD);
		} else if (ch == ',') {
			tap(KeyEvent.VK_COMMA);
		} else if (ch == ':') {
			shiftTap(KeyEvent.VK_SEMICOLON);
		} else if (ch == '/') {
			tap(KeyEvent.VK_SLASH);
		} else if (ch == '\n') {
			tap(KeyEvent.VK_ENTER);
		} else {
			throw new RuntimeException("no key mapping for char: " + ch);
		}
	}

	/** Select-all then delete, so a field can be overwritten cleanly. */
	public void clearField() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		tap(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		tap(KeyEvent.VK_DELETE);
	}

	// ---------------------------------------------------------------
	// high-level gestures
	// ---------------------------------------------------------------

	public void menu(String menuName, String itemName) throws Exception {
		Exception last = null;
		for (int attempt = 0; attempt < 4; attempt++) {
			try {
				Component m = waitFind(menuText(menuName),
						"menu " + menuName, 5000);
				click(m);
				robot.delay(300);
				Component it = find(menuItemText(itemName));
				long d = System.currentTimeMillis() + 2500;
				while (it == null && System.currentTimeMillis() < d) {
					robot.delay(60);
					it = find(menuItemText(itemName));
				}
				if (it == null || !it.isShowing()) {
					// the popup did not open; press Escape and retry
					keyToCanvasSafe(KeyEvent.VK_ESCAPE);
					throw new RuntimeException("menu item " + itemName
							+ " did not appear");
				}
				click(it);
				robot.delay(250);
				return;
			} catch (Exception e) {
				last = e;
				robot.delay(300);
			}
		}
		throw new RuntimeException("menu(" + menuName + "," + itemName
				+ ") failed after retries", last);
	}

	private void keyToCanvasSafe(int keyCode) {
		try {
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		} catch (Exception ignore) {
			// best effort
		}
	}

	public void shot(String path) throws Exception {
		robot.delay(120);
		BufferedImage img = robot.createScreenCapture(screen);
		ImageIO.write(img, "png", new File(path));
		System.out.println("shot: " + path);
	}

	// ---------------------------------------------------------------
	// model access (reading the screen, like a human -- never building)
	// ---------------------------------------------------------------

	/** The Editor of the currently selected tab, or null. */
	public jls.edit.Editor currentEditor() {
		Component tp = find(c -> c instanceof javax.swing.JTabbedPane);
		if (tp instanceof javax.swing.JTabbedPane tabs) {
			Component sel = tabs.getSelectedComponent();
			if (sel instanceof jls.edit.Editor ed) {
				return ed;
			}
		}
		// fall back: any Editor component
		Component c = find(x -> x instanceof jls.edit.Editor);
		return (jls.edit.Editor) c;
	}

	/** The editor's info label text (shows the overlap/connection reason
	 *  in red when a wire commit is blocked); read via reflection so the
	 *  driver can see WHY a connection was refused. */
	public String infoText() {
		try {
			java.lang.reflect.Field f =
					jls.edit.SimpleEditor.class.getDeclaredField("info");
			f.setAccessible(true);
			javax.swing.JLabel l = (javax.swing.JLabel) f.get(currentEditor());
			return "'" + l.getText() + "'";
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}

	public jls.Circuit currentCircuit() {
		jls.edit.Editor ed = currentEditor();
		return ed == null ? null : ed.getCircuit();
	}

	/** The scrolling edit canvas (the viewport view that owns mouse
	 *  listeners), used to map model coordinates to screen pixels. */
	public Component canvas() {
		jls.edit.Editor ed = currentEditor();
		if (ed == null) {
			return null;
		}
		// the canvas is a JScrollPane's viewport view under the editor
		for (Component c : descend(ed)) {
			if (c instanceof javax.swing.JScrollPane sp) {
				Component v = sp.getViewport().getView();
				if (v != null && v.getMouseListeners().length > 0) {
					return v;
				}
			}
		}
		// fall back: the biggest showing panel under the editor
		Component best = null;
		for (Component c : descend(ed)) {
			if (c.isShowing() && c.getWidth() > 400 && c.getHeight() > 400) {
				if (best == null
						|| c.getWidth() * c.getHeight()
								> best.getWidth() * best.getHeight()) {
					best = c;
				}
			}
		}
		return best;
	}

	// ---------------------------------------------------------------
	// coordinate mapping and construction gestures
	// ---------------------------------------------------------------

	/** Map a model (grid) point to an absolute screen pixel, assuming the
	 *  canvas is at zoom 1 and scrolled to the origin (the state a freshly
	 *  opened editor is in and which the builder never disturbs). */
	public Point modelToScreen(int mx, int my) {
		Component cv = canvas();
		Point o = cv.getLocationOnScreen();
		return new Point(o.x + mx, o.y + my);
	}

	/** Editable text fields inside the currently showing dialog, in
	 *  component order (name before bits, value before radix, ...). */
	public List<JTextComponent> dialogFields() {
		List<JTextComponent> out = new ArrayList<>();
		for (Window w : Window.getWindows()) {
			if (w instanceof java.awt.Dialog && w.isShowing()) {
				for (Component c : descend(w)) {
					if (c instanceof JTextComponent t && t.isEditable()
							&& t.isShowing()) {
						out.add(t);
					}
				}
			}
		}
		return out;
	}

	public AbstractButton dialogButton(String text) {
		for (Window w : Window.getWindows()) {
			if (w instanceof java.awt.Dialog && w.isShowing()) {
				for (Component c : descend(w)) {
					if (c instanceof AbstractButton b && text.equals(b.getText())
							&& b.isShowing()) {
						return b;
					}
				}
			}
		}
		return null;
	}

	public AbstractButton waitDialogButton(String text, long ms)
			throws Exception {
		long deadline = System.currentTimeMillis() + ms;
		AbstractButton b = null;
		while (b == null && System.currentTimeMillis() < deadline) {
			b = dialogButton(text);
			if (b == null) {
				robot.delay(60);
			}
		}
		return b;
	}

	public boolean dialogUp() {
		for (Window w : Window.getWindows()) {
			if (w instanceof java.awt.Dialog && w.isShowing()) {
				return true;
			}
		}
		return false;
	}

	private void waitNoDialog(long ms) throws Exception {
		long deadline = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < deadline) {
			if (!dialogUp()) {
				return;
			}
			robot.delay(50);
		}
	}

	/**
	 * Place one element through the palette and its real creation dialog,
	 * then commit it at model cell (mx,my).
	 *
	 * @param tip    palette tooltip (element name), e.g. "adder".
	 * @param fields values to type into the dialog's editable fields, in
	 *               order; a null entry leaves that field at its default.
	 * @param radios radio-button labels to select (e.g. "10" for a decimal
	 *               constant), or null.
	 * @return the element that appeared in the circuit.
	 */
	public jls.elem.Element placeElement(String tip, String[] fields,
			String[] radios, int mx, int my) throws Exception {
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());

		Component pal = waitFind(paletteTip(tip), "palette " + tip, 5000);
		click(pal);
		// wait for the creation dialog
		long d = System.currentTimeMillis() + 5000;
		while (!dialogUp() && System.currentTimeMillis() < d) {
			robot.delay(50);
		}
		robot.delay(150);

		if (fields != null) {
			List<JTextComponent> fs = dialogFields();
			for (int i = 0; i < fields.length && i < fs.size(); i++) {
				if (fields[i] == null) {
					continue;
				}
				click(fs.get(i));
				clearField();
				type(fields[i]);
			}
		}
		if (radios != null) {
			for (String r : radios) {
				AbstractButton rb = dialogButton(r);
				if (rb != null) {
					click(rb);
				}
			}
		}
		AbstractButton ok = dialogButton("OK");
		click(ok);
		waitNoDialog(4000);
		robot.delay(200);

		// element is now "chosen" and following the pointer: move into the
		// canvas so it follows, then click to commit at the target cell
		Point s = modelToScreen(mx, my);
		robot.mouseMove(s.x, s.y);
		robot.delay(150);
		clickAt(s.x, s.y);
		robot.delay(200);

		for (jls.elem.Element e : currentCircuit().getElements()) {
			if (!before.contains(e)) {
				return e;
			}
		}
		throw new RuntimeException("placement of " + tip + " did not add "
				+ "an element");
	}

	/**
	 * Place an element and position it at an EXACT model cell using the
	 * keyboard construction path (issue #75): after the creation dialog the
	 * element is "chosen" and already in the circuit, so read its drop
	 * position, arrow-key nudge it grid-step by grid-step to the target, and
	 * press Enter to commit. Deterministic -- no mouse-follow guesswork -- so
	 * connected ports can be made to coincide exactly.
	 */
	public jls.elem.Element placeExact(String tip, String[] fields,
			String[] radios, int tx, int ty) throws Exception {
		ensureIdle();
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());
		// open the creation dialog, retrying the palette click if it does
		// not appear (a click can be lost under the headless WM)
		boolean opened = false;
		for (int attempt = 0; attempt < 3 && !opened; attempt++) {
			Component pal = waitFind(paletteTip(tip), "palette " + tip, 5000);
			click(pal);
			long d = System.currentTimeMillis() + 3000;
			while (!dialogUp() && System.currentTimeMillis() < d) {
				robot.delay(50);
			}
			opened = dialogUp();
			if (!opened) {
				ensureIdle();
			}
		}
		if (!opened) {
			try {
				Component pal = find(paletteTip(tip));
				System.err.println("palette '" + tip + "' loc="
						+ (pal != null && pal.isShowing()
								? pal.getLocationOnScreen() : "??")
						+ " frame bounds=" + (jls.JLSInfo.frame != null
								? jls.JLSInfo.frame.getBounds() : "null"));
				shot(System.getProperty("java.io.tmpdir") + "/"
					+ "jls-open-fail.png");
			} catch (Exception ignore) {
			}
			throw new RuntimeException("creation dialog for " + tip
					+ " never opened");
		}
		robot.delay(150);
		if (fields != null) {
			List<JTextComponent> fs = dialogFields();
			for (int i = 0; i < fields.length && i < fs.size(); i++) {
				if (fields[i] == null) {
					continue;
				}
				click(fs.get(i));
				clearField();
				type(fields[i]);
			}
		}
		if (radios != null) {
			for (String r : radios) {
				AbstractButton rb = dialogButton(r);
				if (rb != null) {
					click(rb);
				}
			}
		}
		AbstractButton okb = waitDialogButton("OK", 4000);
		if (okb == null) {
			try {
				shot(System.getProperty("java.io.tmpdir") + "/"
					+ "jls-ok-fail.png");
			} catch (Exception ignore) {
			}
			throw new RuntimeException("OK button not found for " + tip);
		}
		click(okb);
		waitNoDialog(4000);
		robot.delay(200);

		jls.elem.Element el = newRealElement(before);
		if (el == null) {
			throw new RuntimeException("no element appeared for " + tip);
		}
		// nudge from the drop position to the target with arrow keys
		focusCanvas();
		int step = jls.JLSInfo.spacing;
		int guardX = 400;
		while (el.getX() != tx && guardX-- > 0) {
			keyToCanvas(el.getX() < tx ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
		}
		int guardY = 400;
		while (el.getY() != ty && guardY-- > 0) {
			keyToCanvas(el.getY() < ty ? KeyEvent.VK_DOWN : KeyEvent.VK_UP);
		}
		keyToCanvas(KeyEvent.VK_ENTER); // commit placement
		robot.delay(150);
		if (el.getX() != tx || el.getY() != ty) {
			System.err.println("placeExact " + tip + " wanted (" + tx + ","
					+ ty + ") got (" + el.getX() + "," + el.getY() + ") step="
					+ step);
		}
		return el;
	}

	/** Place a Memory element (RAM/ROM), optionally typing its initial
	 *  contents into the dialog's "Built In" editor (the genuine GUI way a
	 *  user enters a program into ROM). `contents` is "addr value" hex, one
	 *  pair per line. Fields order in the Create Memory dialog is
	 *  [name, bits, capacity]. */
	public jls.elem.Element placeMemory(String name, int bits, int cap,
			boolean rom, String contents, int tx, int ty) throws Exception {
		ensureIdle();
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());
		click(waitFind(paletteTip("memory, various types"), "mem palette", 5000));
		long dd = System.currentTimeMillis() + 5000;
		while (!dialogUp() && System.currentTimeMillis() < dd) {
			robot.delay(50);
		}
		robot.delay(200);
		if (rom) {
			AbstractButton r = dialogButton("ROM");
			if (r != null) {
				click(r);
			}
		}
		List<JTextComponent> fs = dialogFields();
		String[] vals = {name, String.valueOf(bits), String.valueOf(cap)};
		for (int i = 0; i < vals.length && i < fs.size(); i++) {
			click(fs.get(i));
			clearField();
			type(vals[i]);
		}
		if (contents != null) {
			// "Built In" opens a JTextArea editor for the initial contents
			click(dialogButton("Built In"));
			robot.delay(400);
			javax.swing.JTextArea area = null;
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog && w.isShowing()
						&& !"Create Memory".equals(((java.awt.Dialog) w).getTitle())) {
					for (Component c : descend(w)) {
						if (c instanceof javax.swing.JTextArea ta
								&& ta.isShowing()) {
							area = ta;
						}
					}
				}
			}
			if (area != null) {
				click(area);
				clearField();
				type(contents);
				robot.delay(100);
				// the Built In editor's OK button is lowercase "ok"
				AbstractButton ok = null;
				for (Window w : Window.getWindows()) {
					if (w instanceof java.awt.Dialog dlg && w.isShowing()
							&& !"Create Memory".equals(dlg.getTitle())) {
						for (Component c : descend(w)) {
							if (c instanceof AbstractButton b && b.isShowing()
									&& ("ok".equalsIgnoreCase(b.getText()))) {
								ok = b;
							}
						}
					}
				}
				if (ok != null) {
					click(ok);
				}
				robot.delay(300);
			} else {
				System.err.println("placeMemory: Built In text area not found");
			}
		}
		if (VERBOSE) {
			try {
				shot(System.getProperty("java.io.tmpdir") + "/"
					+ "jls-mem-dlg.png");
			} catch (Exception ignore) {
			}
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog dlg && w.isShowing()) {
					System.err.println("[mem] dialog up: '" + dlg.getTitle()
							+ "'");
				}
			}
		}
		click(dialogButton("OK"));
		waitNoDialog(4000);
		robot.delay(200);
		jls.elem.Element el = newRealElement(before);
		if (el == null) {
			if (VERBOSE) {
				for (Window w : Window.getWindows()) {
					if (w instanceof java.awt.Dialog dlg && w.isShowing()) {
						System.err.println("[mem] STILL up: '" + dlg.getTitle()
								+ "'");
						for (Component c : descend(w)) {
							if (c instanceof javax.swing.JLabel l
									&& l.getText() != null
									&& !l.getText().isBlank()) {
								System.err.println("   label: " + l.getText());
							}
						}
					}
				}
			}
			throw new RuntimeException("no Memory element appeared");
		}
		focusCanvas();
		int gx = 400;
		while (el.getX() != tx && gx-- > 0) {
			keyToCanvas(el.getX() < tx ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
		}
		int gy = 400;
		while (el.getY() != ty && gy-- > 0) {
			keyToCanvas(el.getY() < ty ? KeyEvent.VK_DOWN : KeyEvent.VK_UP);
		}
		keyToCanvas(KeyEvent.VK_ENTER);
		robot.delay(150);
		return el;
	}

	/** Place a Constant with a chosen orientation so its OUTPUT port lands
	 *  exactly on (portX,portY) -- coincidence-connecting it to a sink. The
	 *  orientation ("Up"/"Down"/"Left"/"Right") is chosen so the constant's
	 *  body extends AWAY from the sink element, which is how mid-body input
	 *  ports (shifter amount, adder Cin, memory CS/OE) get driven without the
	 *  jump/body overlapping the element. */
	public jls.elem.Element placeConstDriving(String value, String radix,
			String orient, int portX, int portY) throws Exception {
		ensureIdle();
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());
		boolean opened = false;
		for (int attempt = 0; attempt < 3 && !opened; attempt++) {
			click(waitFind(paletteTip("constant value"), "const palette", 5000));
			long dl = System.currentTimeMillis() + 3000;
			while (!dialogUp() && System.currentTimeMillis() < dl) {
				robot.delay(50);
			}
			opened = dialogUp();
			if (!opened) {
				ensureIdle();
			}
		}
		robot.delay(150);
		List<JTextComponent> fs = dialogFields();
		if (!fs.isEmpty()) {
			click(fs.get(0));
			clearField();
			type(value);
		}
		AbstractButton rb = dialogButton(radix);
		if (rb != null) {
			click(rb);
		}
		AbstractButton ob = dialogButton(orient);
		if (ob != null) {
			click(ob);
		}
		click(waitDialogButton("OK", 4000));
		waitNoDialog(4000);
		robot.delay(200);
		jls.elem.Element el = newRealElement(before);
		if (el == null) {
			throw new RuntimeException("no constant appeared");
		}
		jls.elem.Put op = null;
		for (jls.elem.Put p : el.getAllPuts()) {
			if (p instanceof jls.elem.Output) {
				op = p;
			}
		}
		int offX = op == null ? 0 : op.getX() - el.getX();
		int offY = op == null ? 0 : op.getY() - el.getY();
		int ex = portX - offX;
		int ey = portY - offY;
		focusCanvas();
		int gx = 500;
		while (el.getX() != ex && gx-- > 0) {
			keyToCanvas(el.getX() < ex ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
		}
		int gy = 500;
		while (el.getY() != ey && gy-- > 0) {
			keyToCanvas(el.getY() < ey ? KeyEvent.VK_DOWN : KeyEvent.VK_UP);
		}
		keyToCanvas(KeyEvent.VK_ENTER);
		robot.delay(150);
		return el;
	}

	/** Place a ROM whose initial contents come from a program file loaded
	 *  through the dialog's "from File" button (assemble -> file -> load,
	 *  the genuine way software reaches a CPU). fileName must be a bare
	 *  valid name (letters/digits/_); the file is read from the working
	 *  directory at simulation time. */
	public jls.elem.Element placeRomFile(String name, int bits, int cap,
			String fileName, int tx, int ty) throws Exception {
		ensureIdle();
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());
		click(waitFind(paletteTip("memory, various types"), "mem palette", 5000));
		long dd = System.currentTimeMillis() + 5000;
		while (!dialogUp() && System.currentTimeMillis() < dd) {
			robot.delay(50);
		}
		robot.delay(200);
		AbstractButton rom = dialogButton("ROM");
		if (rom != null) {
			click(rom);
		}
		List<JTextComponent> fs = dialogFields();
		String[] vals = {name, String.valueOf(bits), String.valueOf(cap)};
		for (int i = 0; i < vals.length && i < fs.size(); i++) {
			click(fs.get(i));
			clearField();
			type(vals[i]);
		}
		// "from File" -> input prompt for the (bare) file name
		click(dialogButton("from File"));
		robot.delay(400);
		JTextComponent pathField = null;
		for (Window w : Window.getWindows()) {
			if (w instanceof java.awt.Dialog dlg && w.isShowing()
					&& !"Create Memory".equals(dlg.getTitle())) {
				for (Component c : descend(w)) {
					if (c instanceof JTextComponent t && t.isEditable()
							&& t.isShowing()) {
						pathField = t;
					}
				}
			}
		}
		if (pathField != null) {
			click(pathField);
			clearField();
			type(fileName);
			robot.delay(100);
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog dlg && w.isShowing()
						&& !"Create Memory".equals(dlg.getTitle())) {
					for (Component c : descend(w)) {
						if (c instanceof AbstractButton b && b.isShowing()
								&& "OK".equals(b.getText())) {
							click(b);
						}
					}
				}
			}
			robot.delay(300);
		} else {
			System.err.println("placeRomFile: path field not found");
		}
		click(dialogButton("OK"));
		waitNoDialog(4000);
		robot.delay(200);
		jls.elem.Element el = newRealElement(before);
		if (el == null) {
			throw new RuntimeException("no ROM element appeared");
		}
		focusCanvas();
		int gx = 400;
		while (el.getX() != tx && gx-- > 0) {
			keyToCanvas(el.getX() < tx ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
		}
		int gy = 400;
		while (el.getY() != ty && gy-- > 0) {
			keyToCanvas(el.getY() < ty ? KeyEvent.VK_DOWN : KeyEvent.VK_UP);
		}
		keyToCanvas(KeyEvent.VK_ENTER);
		robot.delay(150);
		return el;
	}

	/** Place a JumpEnd ("connect to a named wire") that joins the named net,
	 *  positioned so its element origin lands at (tx,ty). Drives the
	 *  name-selector list in the creation dialog, then keyboard-nudges to the
	 *  target and commits. */
	public jls.elem.Element placeJumpEnd(String name, int tx, int ty)
			throws Exception {
		ensureIdle();
		java.util.Set<jls.elem.Element> before =
				new java.util.HashSet<>(currentCircuit().getElements());
		Component pal = waitFind(paletteTip("connect to a named wire"),
				"jumpend palette", 5000);
		click(pal);
		long d = System.currentTimeMillis() + 5000;
		while (!dialogUp() && System.currentTimeMillis() < d) {
			robot.delay(50);
		}
		robot.delay(200);
		// select the wire name in the dialog's JList (poll: it may settle)
		javax.swing.JList<?> list = null;
		long jld = System.currentTimeMillis() + 3000;
		while (list == null && System.currentTimeMillis() < jld) {
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog && w.isShowing()) {
					for (Component c : descend(w)) {
						if (c instanceof javax.swing.JList<?> jl && jl.isShowing()) {
							list = jl;
						}
					}
				}
			}
			if (list == null) {
				robot.delay(80);
			}
		}
		if (list != null) {
			int idx = -1;
			for (int i = 0; i < list.getModel().getSize(); i++) {
				if (name.equals(String.valueOf(list.getModel().getElementAt(i)))) {
					idx = i;
					break;
				}
			}
			if (idx < 0) {
				idx = 0;
			}
			java.awt.Rectangle cell = list.getCellBounds(idx, idx);
			Point lo = list.getLocationOnScreen();
			clickAt(lo.x + cell.width / 2, lo.y + cell.y + cell.height / 2);
			robot.delay(120);
		} else {
			System.err.println("placeJumpEnd(" + name + "): no JList found; "
					+ "dialogs up:");
			for (Window w : Window.getWindows()) {
				if (w instanceof java.awt.Dialog dlg && w.isShowing()) {
					System.err.println("  '" + dlg.getTitle() + "'");
				}
			}
			try {
				shot(System.getProperty("java.io.tmpdir") + "/"
					+ "jls-je-fail.png");
			} catch (Exception ignore) {
			}
		}
		// wait for the OK button (the dialog may still be settling)
		AbstractButton ok = null;
		long okd = System.currentTimeMillis() + 4000;
		while (ok == null && System.currentTimeMillis() < okd) {
			ok = dialogButton("OK");
			if (ok == null) {
				robot.delay(60);
			}
		}
		if (ok != null) {
			click(ok);
		} else if (dialogUp()) {
			// last resort: commit the dialog with Enter
			keyToCanvas(KeyEvent.VK_ENTER);
		}
		waitNoDialog(4000);
		robot.delay(200);

		jls.elem.Element el = newRealElement(before);
		if (el == null) {
			throw new RuntimeException("no JumpEnd appeared");
		}
		// (tx,ty) is the target for the JumpEnd's OUTPUT port; its offset from
		// the element origin depends on the label width, so read it live.
		jls.elem.Put op = null;
		for (jls.elem.Put p : el.getAllPuts()) {
			if (p instanceof jls.elem.Output) {
				op = p;
			}
		}
		int offX = op == null ? 36 : op.getX() - el.getX();
		int offY = op == null ? 0 : op.getY() - el.getY();
		int ex = tx - offX;
		int ey = ty - offY;
		focusCanvas();
		int gx = 400;
		while (el.getX() != ex && gx-- > 0) {
			keyToCanvas(el.getX() < ex ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
		}
		int gy = 400;
		while (el.getY() != ey && gy-- > 0) {
			keyToCanvas(el.getY() < ey ? KeyEvent.VK_DOWN : KeyEvent.VK_UP);
		}
		keyToCanvas(KeyEvent.VK_ENTER);
		robot.delay(150);
		return el;
	}

	/** Draw a wire from one put to another along an explicit orthogonal
	 *  route (model-space bend points between the pins), committing a vertex
	 *  at each bend. Routing through clear space keeps the editor's overlap
	 *  guard from blocking the final commit and lets connect() attach the
	 *  destination pin. */
	public void wirePath(jls.elem.Put from, int[][] bends, jls.elem.Put to)
			throws Exception {
		focusCanvas();
		Point a = modelToScreen(from.getX(), from.getY());
		robot.mouseMove(a.x, a.y);
		robot.delay(120);
		keyToCanvas(KeyEvent.VK_W);
		robot.delay(120);
		clickAt(a.x, a.y); // anchor + attach source
		robot.delay(100);
		Point prev = a;
		for (int[] p : bends) {
			Point s = modelToScreen(p[0], p[1]);
			robot.mouseMove((prev.x + s.x) / 2, (prev.y + s.y) / 2);
			robot.delay(30);
			robot.mouseMove(s.x, s.y);
			robot.delay(80);
			clickAt(s.x, s.y);
			robot.delay(90);
			prev = s;
		}
		Point b = modelToScreen(to.getX(), to.getY());
		robot.mouseMove((prev.x + b.x) / 2, (prev.y + b.y) / 2);
		robot.delay(30);
		robot.mouseMove(b.x, b.y);
		robot.delay(100);
		clickAt(b.x, b.y); // commit dest + attach
		robot.delay(120);
		keyToCanvas(KeyEvent.VK_ESCAPE);
		robot.delay(80);
	}

	/** Draw a wire from one put to another with real input: hover the
	 *  source pin, press W to start the wire (it attaches to the put under
	 *  the pointer), move to the destination pin, and click to commit the
	 *  vertex there (attaching to that put). */
	public void wire(jls.elem.Put from, jls.elem.Put to) throws Exception {
		Point a = modelToScreen(from.getX(), from.getY());
		Point b = modelToScreen(to.getX(), to.getY());
		trace("focusCanvas");
		focusCanvas();
		// start the wire exactly on the source pin: hover it, then fire the
		// W action (it drops a wire end at the pointer). In the startwire
		// state this single end follows the mouse, so commit a vertex right
		// here first -- that anchors the end on the source put (connect()
		// attaches the coincident put) and spawns a fresh floating end.
		trace("move source");
		robot.mouseMove(a.x, a.y);
		robot.delay(120);
		trace("W");
		keyToCanvas(KeyEvent.VK_W);
		robot.delay(120);
		trace("click source");
		clickAt(a.x, a.y);
		robot.delay(100);
		// extend the floating end to the destination pin and commit: connect()
		// attaches the coincident dest put and finishes the wire
		trace("move dest");
		robot.mouseMove((a.x + b.x) / 2, (a.y + b.y) / 2);
		robot.delay(40);
		robot.mouseMove(b.x, b.y);
		robot.delay(100);
		trace("click dest");
		clickAt(b.x, b.y);
		robot.delay(100);
		// end the gesture if anything is still in flight
		trace("escape");
		keyToCanvas(KeyEvent.VK_ESCAPE);
		robot.delay(80);
		trace("done");
	}

	private List<Component> descend(Component root) {
		List<Component> out = new ArrayList<>();
		collect(root, out);
		return out;
	}

	/** Dump every component under any dialog window newer than the main
	 *  frame -- class, text, tooltip, bounds -- so a probe run can see the
	 *  real widget layout of an element creation dialog. */
	public void dumpDialogs() {
		for (Window w : Window.getWindows()) {
			if (!(w instanceof java.awt.Dialog) || !w.isShowing()) {
				continue;
			}
			System.out.println("DIALOG: " + ((java.awt.Dialog) w).getTitle()
					+ " " + w.getBounds());
			for (Component c : descend(w)) {
				String txt = "";
				if (c instanceof AbstractButton b) {
					txt = "btn='" + b.getText() + "'";
				} else if (c instanceof JTextComponent t) {
					txt = "text='" + t.getText() + "' editable="
							+ t.isEditable();
				} else if (c instanceof javax.swing.JComboBox<?> cb) {
					txt = "combo sel='" + cb.getSelectedItem() + "'";
				} else if (c instanceof javax.swing.JLabel l) {
					txt = "label='" + l.getText() + "'";
				}
				if (!txt.isEmpty()) {
					System.out.println("   " + c.getClass().getSimpleName()
							+ " " + txt + " showing=" + c.isShowing());
				}
			}
		}
	}

	// ---------------------------------------------------------------
	// self-test entry point: File > New, then screenshot the editor
	// ---------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		String out = args.length > 0 ? args[0]
				: "/tmp/gui-newproof.png";
		String name = args.length > 1 ? args[1] : "riscv";

		Frame f = boot();
		GuiDriver g = new GuiDriver();
		g.placeFrame(f, 1900, 1180);
		g.shot(out.replace(".png", "-0boot.png"));

		g.menu("File", "New");
		// the New action pops a modal input dialog for the circuit name
		Component field = g.waitFind(anyTextField(), "new-circuit name field",
				6000);
		g.click(field);
		g.robot.delay(100);
		g.type(name);
		g.shot(out.replace(".png", "-1typed.png"));
		Component ok = g.waitFind(buttonText("OK"), "OK button", 4000);
		g.click(ok);
		g.robot.delay(600);

		g.shot(out.replace(".png", "-2editor.png"));
		System.out.println("done; components now: " + g.allComponents().size());
		EventQueue.invokeLater(() -> {
			// leave the app up for follow-on scripts
		});
		System.out.flush();
		System.exit(0);
	}
}
