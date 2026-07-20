package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.Printable;
import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.sim.Simulator;

/**
 * Editor-driven printing coverage (issue #56's last residual): drives
 * the real "Print..." menu items' production code - {@link JLSStart}
 * reading the currently selected {@link jls.edit.Editor} tab's circuit
 * and booking it - without a real printer or the interactive print
 * dialog.
 *
 * {@link JLSStart#print(boolean)} itself calls
 * {@link java.awt.print.PrinterJob#printDialog()}, a modal native
 * dialog that blocks for user input and so cannot be driven headlessly
 * (there is nobody under Xvfb to click it). The dialog and the actual
 * submission to a {@code PrinterJob} are the only non-headless-safe
 * part of the path; everything upstream of them - which editor tab is
 * selected, and whether the whole circuit (with subcircuits) or just
 * the visible one is booked - is extracted into
 * {@link JLSStart#assemblePrintBook(boolean)} and is exactly what this
 * test drives, then renders every booked page into a
 * {@link BufferedImage} the same way {@link PrintPathSmokeTest} does
 * for the standalone {@link Circuit#print} path, proving the editor's
 * selection genuinely reaches the printable pages.
 *
 * Tagged {@code display}: {@code new JLSStart()} is a visible
 * {@code JFrame} (per {@link jls.ui.MenuBarSpecTest}'s
 * {@code bootMainWindow}), so this runs under the #162 substrate
 * ({@code xvfb-run -a mvn -B verify -Djls.test.headless=false}) and
 * self-skips headless.
 */
@Tag("display")
@Timeout(60)
class EditorPrintPathTest {

	/**
	 * Skip the whole class in headless runs; the display suite supplies
	 * a real or virtual display.
	 */
	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * A circuit with a state machine (booked as its own page plus an
	 * output-summary page - see {@link PrintPathSmokeTest}) and a
	 * subcircuit, so "Entire circuit" and "Just visible window" book a
	 * different number of pages.
	 *
	 * @return the loaded, finished circuit
	 * @throws Exception on any load failure
	 */
	private static Circuit fixture() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.constant(5);
		cb.clock(20, 10);
		cb.stateMachine(1, 1);
		cb.subCircuit("sub");
		int in = cb.inputPin("in1", 1);
		int out = cb.outputPin("out", 1);
		cb.wire(in, "output", out, "input");

		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * Construct the real JLS main window on the EDT, the same way
	 * {@link jls.ui.MenuBarSpecTest#bootMainWindow()} does: the
	 * constructor needs the static exception handler installed first,
	 * which normally only happens through {@code start()}.
	 *
	 * @return the constructed main window
	 * @throws Exception on reflection or EDT failure
	 */
	private static JLSStart bootMainWindow() throws Exception {
		Field handler = JLSStart.class.getDeclaredField("exHandler");
		handler.setAccessible(true);
		if (handler.get(null) == null) {
			handler.set(null, new DefaultExceptionHandler());
		}
		AtomicReference<JLSStart> ref = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> ref.set(new JLSStart()));
		return ref.get();
	}

	/**
	 * Render every page of a book into a {@link BufferedImage}, the
	 * same headless-safe verification {@link PrintPathSmokeTest} uses:
	 * {@link Printable#print} draws into any {@code Graphics2D}, so no
	 * printer or print service is needed to prove the pages render.
	 *
	 * @param book the book to render
	 */
	private static void renderEveryPage(Book book) throws Exception {
		BufferedImage image = new BufferedImage(850, 1100,
				BufferedImage.TYPE_INT_RGB);
		for (int page = 0; page < book.getNumberOfPages(); page++) {
			Graphics2D g = image.createGraphics();
			try {
				int result = book.getPrintable(page)
						.print(g, book.getPageFormat(page), page);
				assertEquals(Printable.PAGE_EXISTS, result,
						"page " + page + " must render");
			} finally {
				g.dispose();
			}
		}
	}

	/**
	 * The "Entire circuit" menu item's production path
	 * ({@code assemblePrintBook(true)}), driven against a real editor
	 * tab in a real {@link JLSStart}: books the visible circuit plus its
	 * state machine (and output-summary) and subcircuit pages, and
	 * every one of them renders.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void entireCircuitBooksTheSelectedEditorsCircuitAndSubcircuits()
			throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			Circuit circuit = fixture();
			SwingUtilities.invokeAndWait(
					() -> jls.setupEditor(circuit, circuit.getName()));

			AtomicReference<Book> bookRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(
					() -> bookRef.set(jls.assemblePrintBook(true)));
			Book book = bookRef.get();

			assertNotNull(book, "an editor tab is selected");
			// circuit + state machine + its output summary + subcircuit
			assertTrue(book.getNumberOfPages() >= 4,
					"expected at least 4 booked pages, got "
							+ book.getNumberOfPages());
			renderEveryPage(book);
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * The "Just visible window" menu item's production path
	 * ({@code assemblePrintBook(false)}): books only the visible
	 * circuit's own page, fewer pages than "Entire circuit" gets for the
	 * identical selection, and it still renders.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void justVisibleWindowBooksOnlyTheSelectedEditorsOwnPage()
			throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			Circuit circuit = fixture();
			SwingUtilities.invokeAndWait(
					() -> jls.setupEditor(circuit, circuit.getName()));

			AtomicReference<Book> allBookRef = new AtomicReference<>();
			AtomicReference<Book> visibleBookRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				allBookRef.set(jls.assemblePrintBook(true));
				visibleBookRef.set(jls.assemblePrintBook(false));
			});
			Book allBook = allBookRef.get();
			Book visibleBook = visibleBookRef.get();

			assertNotNull(visibleBook, "an editor tab is selected");
			assertEquals(1, visibleBook.getNumberOfPages(),
					"just the visible circuit's own page is booked");
			assertTrue(
					visibleBook.getNumberOfPages() < allBook
							.getNumberOfPages(),
					"visible-window booking must be smaller than the"
							+ " entire-circuit booking for the same"
							+ " selection");
			renderEveryPage(visibleBook);
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * Recursively locate a {@link JMenuItem} by its exact label within a
	 * menu element subtree, so the real "Print..." menu items can be
	 * dispatched instead of calling {@link JLSStart#print(boolean)}
	 * directly - proving the menu is actually wired to the print path.
	 *
	 * @param element the menu element to search (a menu bar, menu, or item)
	 * @param label the exact item text to match
	 * @return the matching item, or null if none is found
	 */
	private static JMenuItem findMenuItem(MenuElement element, String label) {
		if (element instanceof JMenuItem item
				&& !(element instanceof JMenu)
				&& label.equals(item.getText())) {
			return item;
		}
		for (MenuElement child : element.getSubElements()) {
			JMenuItem found = findMenuItem(child, label);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * With no editor tab selected (a freshly booted main window),
	 * {@code assemblePrintBook} returns null and {@code print} returns
	 * immediately - the guard that keeps the menu items from ever
	 * reaching {@code PrinterJob} (and its dialog) with nothing to
	 * print. The two real "Print..." menu items are dispatched here (not
	 * {@code print()} called directly), so the menu-to-{@code print()}
	 * wiring is exercised end to end; this is the one editor-selection
	 * state in which clicking them is headless-safe, precisely because
	 * the null-book guard fires before the interactive dialog.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void withNoEditorSelectedAssemblyYieldsNothingAndPrintIsANoop()
			throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			AtomicReference<Book> bookRef = new AtomicReference<>();
			AtomicReference<JMenuItem> entireRef = new AtomicReference<>();
			AtomicReference<JMenuItem> visibleRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				bookRef.set(jls.assemblePrintBook(true));
				JMenuBar bar = jls.getJMenuBar();
				JMenuItem entire = findMenuItem(bar, "Entire circuit");
				JMenuItem visible = findMenuItem(bar, "Just visible window");
				entireRef.set(entire);
				visibleRef.set(visible);
				// safe to click the real menu items: with no editor
				// selected, print() returns on the null-book guard before
				// it would ever show the interactive print dialog
				if (entire != null) {
					entire.doClick();
				}
				if (visible != null) {
					visible.doClick();
				}
			});
			assertNull(bookRef.get(),
					"no editor tab selected must assemble no book");
			assertNotNull(entireRef.get(),
					"the \"Entire circuit\" print menu item must exist");
			assertNotNull(visibleRef.get(),
					"the \"Just visible window\" print menu item must exist");
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}
}
