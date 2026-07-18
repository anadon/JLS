package jls.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Hand-rolled EDT-violation hook for the layer-2 suites (issue #162,
 * prediction P2): a {@link RepaintManager} that records every repaint or
 * revalidate that reaches Swing's paint pipeline off the event dispatch
 * thread. No dependency - the 2026-07 library survey rejected the
 * frameworks that bundle such a checker, so this is the
 * ThreadViolationChecker pattern reduced to what the suites need.
 *
 * Two tolerances keep it honest about Swing's actual contract:
 *
 * <ul>
 * <li>{@code Component.repaint()} is documented thread-safe, so a plain
 * off-EDT repaint from application code is not a violation - unless the
 * call originated <i>inside</i> javax.swing itself, which means some
 * Swing method that is not thread-safe was entered off the EDT and
 * repainted as a side effect.</li>
 * <li>{@code imageUpdate} repaints arrive on the image-fetcher thread by
 * design and are ignored.</li>
 * </ul>
 *
 * Per the package discipline the detector's assertion is pinned by
 * deliberately-failing tests in {@link jls.ui.EdtViolationDetectorTest};
 * those call the overridden hooks directly because a JDK &gt;= 9
 * {@code revalidate()} re-dispatches itself to the EDT, so a genuine
 * off-EDT arrival can no longer be synthesized from library-level code.
 */
final class EdtViolationDetector extends RepaintManager {

	/** The manager that was current before {@link #install()}. */
	private final RepaintManager previous;

	/** Violations recorded so far, each a formatted component + stack. */
	private final List<String> violations =
			Collections.synchronizedList(new ArrayList<String>());

	/**
	 * Remember the manager to restore on {@link #uninstall()}.
	 *
	 * @param previous The previously current repaint manager.
	 */
	private EdtViolationDetector(RepaintManager previous) {
		this.previous = previous;
	} // end of constructor

	/**
	 * Install a fresh detector as the current repaint manager.
	 *
	 * @return the installed detector, for later queries and uninstall.
	 */
	static EdtViolationDetector install() {
		EdtViolationDetector detector = new EdtViolationDetector(
				RepaintManager.currentManager(null));
		RepaintManager.setCurrentManager(detector);
		return detector;
	} // end of install method

	/**
	 * Restore the repaint manager that was current before install.
	 */
	void uninstall() {
		RepaintManager.setCurrentManager(previous);
	} // end of uninstall method

	/**
	 * Record a violation if this revalidate reached the paint pipeline
	 * off the EDT, then delegate to the real manager.
	 *
	 * @param component The invalid component.
	 */
	@Override
	public synchronized void addInvalidComponent(JComponent component) {
		check(component);
		super.addInvalidComponent(component);
	} // end of addInvalidComponent method

	/**
	 * Record a violation if this repaint reached the paint pipeline off
	 * the EDT from inside Swing, then delegate to the real manager.
	 *
	 * @param component The dirty component.
	 * @param x Dirty region x.
	 * @param y Dirty region y.
	 * @param w Dirty region width.
	 * @param h Dirty region height.
	 */
	@Override
	public void addDirtyRegion(JComponent component, int x, int y, int w,
			int h) {
		check(component);
		super.addDirtyRegion(component, x, y, w, h);
	} // end of addDirtyRegion method

	/**
	 * Classify the current call: off the EDT, tolerate the documented
	 * thread-safe repaint() path and image-observer updates; record
	 * everything else with its stack.
	 *
	 * @param component The component being repainted or revalidated.
	 */
	private void check(JComponent component) {
		if (SwingUtilities.isEventDispatchThread()) {
			return;
		}
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		boolean sawRepaint = false;
		boolean fromSwing = false;
		boolean imageUpdate = false;
		for (StackTraceElement frame : stack) {
			if (sawRepaint
					&& frame.getClassName().startsWith("javax.swing.")) {
				fromSwing = true;
			}
			if ("repaint".equals(frame.getMethodName())) {
				sawRepaint = true;
			}
			if ("imageUpdate".equals(frame.getMethodName())) {
				imageUpdate = true;
			}
		}
		if (imageUpdate || (sawRepaint && !fromSwing)) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(component.getClass().getName()).append(" touched on ")
				.append(Thread.currentThread().getName());
		for (StackTraceElement frame : stack) {
			sb.append("\n    at ").append(frame);
		}
		violations.add(sb.toString());
	} // end of check method

	/**
	 * How many violations have been recorded so far.
	 *
	 * @return the violation count.
	 */
	int violationCount() {
		return violations.size();
	} // end of violationCount method

	/**
	 * Fail if any EDT violation was recorded.
	 *
	 * @param context What was running, for the failure message.
	 */
	void assertClean(String context) {
		if (violations.isEmpty()) {
			return;
		}
		List<String> copy;
		synchronized (violations) {
			copy = new ArrayList<String>(violations);
		}
		throw new AssertionError(context + ": " + copy.size()
				+ " Swing EDT violation(s) detected:\n"
				+ String.join("\n", copy));
	} // end of assertClean method

} // end of EdtViolationDetector class
