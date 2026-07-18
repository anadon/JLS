package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link EdtViolationDetector} per the package's
 * assert-the-assertion discipline: the detector must flag off-EDT
 * pipeline access, must stay quiet for the tolerated cases, and
 * {@code assertClean} must actually fail when a violation was recorded.
 *
 * The genuine library-level violation used here is off-EDT component
 * <em>construction</em>: {@code new JPanel()} runs {@code updateUI}/
 * {@code setUI}, which repaints from inside javax.swing - exactly the
 * "Swing entered off the EDT" signature the detector exists to catch
 * (and a real rule of post-1.6 Swing: construction belongs on the EDT
 * too). A second pin drives the {@code addInvalidComponent} hook
 * directly, because on current JDKs {@code revalidate()} re-routes
 * itself to the EDT, so that arrival can no longer be synthesized from
 * library-level code.
 *
 * Everything here is headless-safe: constructing a JPanel and the
 * RepaintManager machinery need no display, so this suite runs in the
 * default execution and guards the detector on every build, not just
 * under the #162 display lane.
 */
class EdtViolationDetectorTest {

	/**
	 * Build a JPanel on the EDT, where construction is legal.
	 *
	 * @return the panel.
	 * @throws Exception If EDT dispatch fails.
	 */
	private static JPanel panelBuiltOnEdt() throws Exception {
		AtomicReference<JPanel> ref = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> ref.set(new JPanel()));
		return ref.get();
	} // end of panelBuiltOnEdt method

	/**
	 * Off-EDT Swing construction is a violation, and assertClean
	 * reports it (the deliberately-failing pin for the helper).
	 */
	@Test
	void offEdtConstructionIsFlagged() {
		EdtViolationDetector detector = EdtViolationDetector.install();
		try {
			// surefire's test thread is never the EDT; JPanel's
			// updateUI/setUI repaints from inside javax.swing
			new JPanel();
			assertTrue(detector.violationCount() > 0,
					"off-EDT construction must be recorded");
			AssertionError reported = assertThrows(AssertionError.class,
					() -> detector.assertClean("pin"));
			assertTrue(reported.getMessage().contains("JPanel"),
					"the report should name the touched component");
		} finally {
			detector.uninstall();
		}
	} // end of offEdtConstructionIsFlagged method

	/**
	 * An off-EDT revalidate arrival at the manager is a violation -
	 * driven through the overridden hook directly, the same entry point
	 * Swing itself calls.
	 *
	 * @throws Exception If EDT dispatch fails.
	 */
	@Test
	void offEdtRevalidateArrivalIsFlagged() throws Exception {
		JPanel panel = panelBuiltOnEdt();
		EdtViolationDetector detector = EdtViolationDetector.install();
		try {
			detector.addInvalidComponent(panel);
			assertEquals(1, detector.violationCount(),
					"off-EDT addInvalidComponent must be recorded");
		} finally {
			detector.uninstall();
		}
	} // end of offEdtRevalidateArrivalIsFlagged method

	/**
	 * The same arrivals on the EDT are not violations, and a clean
	 * detector's assertClean passes.
	 *
	 * @throws Exception If the EDT round-trip fails.
	 */
	@Test
	void onEdtAccessIsClean() throws Exception {
		EdtViolationDetector detector = EdtViolationDetector.install();
		try {
			SwingUtilities.invokeAndWait(() -> {
				JPanel panel = new JPanel();
				panel.repaint();
				detector.addInvalidComponent(panel);
				detector.addDirtyRegion(panel, 0, 0, 10, 10);
			});
			assertEquals(0, detector.violationCount(),
					"EDT access must not be recorded");
			detector.assertClean("clean run");
		} finally {
			detector.uninstall();
		}
	} // end of onEdtAccessIsClean method

	/**
	 * A plain application-code repaint() off the EDT is documented
	 * thread-safe and must be tolerated - this drives the real
	 * {@code Component.repaint()} plumbing into the installed manager
	 * from the test thread.
	 *
	 * @throws Exception If EDT dispatch fails.
	 */
	@Test
	void offEdtPlainRepaintIsTolerated() throws Exception {
		JPanel panel = panelBuiltOnEdt();
		EdtViolationDetector detector = EdtViolationDetector.install();
		try {
			panel.repaint();
			assertEquals(0, detector.violationCount(),
					"repaint() is thread-safe and must not be flagged");
		} finally {
			detector.uninstall();
		}
	} // end of offEdtPlainRepaintIsTolerated method

} // end of EdtViolationDetectorTest class
