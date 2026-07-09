/**
 * UI-verification harness for the test suite (issue #91), built in layers
 * with the cheapest layer preferred per assertion:
 *
 * <ul>
 * <li><b>Layer 1 (this package, present):</b> headless, model-level
 * assertion helpers over {@link jls.Circuit} and {@link jls.elem.Element}
 * state -- element presence, absolute and relative grid location,
 * net-aware connectivity, dimensions, and characteristics (bits, watched
 * flag). See {@link jls.ui.CircuitAssert} and {@link jls.ui.GeometryAssert}.
 * No display, no Graphics, no Swing; safe on any CI runner.</li>
 *
 * <li><b>Layer 2 (future):</b> Swing-harness assertions -- menus, focus,
 * event dispatch, synthesized mouse/keyboard interaction driving the real
 * listener chain, run under Xvfb on CI. Candidates: AssertJ-Swing, with a
 * {@code java.awt.Robot} + {@code SwingUtilities.invokeAndWait} fallback.
 * New helpers for that layer belong in this package (e.g.
 * {@code EditorHarness}, {@code MenuAssert}) and must not be required by
 * Layer-1 tests.</li>
 *
 * <li><b>Layer 3 (future):</b> rendering assertions -- paint to a
 * BufferedImage without a window and make semantic checks (element paints
 * within its bounds, theme color roles used), never brittle pixel goldens.</li>
 * </ul>
 *
 * Meaningfulness discipline: every helper assertion in this package is
 * itself pinned by at least one deliberately-failing test
 * (assert-the-assertion, via {@code assertThrows(AssertionError.class, ...)})
 * so the harness cannot silently pass on an empty circuit. Keep that
 * invariant when adding helpers.
 */
package jls.ui;
