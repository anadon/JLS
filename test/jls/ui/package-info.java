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
 * <li><b>Layer 2 (present, growing):</b> Swing-harness assertions -- menus,
 * focus, event dispatch, synthesized mouse/keyboard interaction driving the
 * real listener chain. Substrate decision (issue #162, recorded per the
 * 2026-07 library survey): a real display server via {@code xvfb-run} on
 * CI ({@code -Djls.test.headless=false}, the {@code display}-tagged
 * surefire execution), driven by plain JUnit with
 * {@code SwingUtilities.invokeAndWait} and synthesized {@code AWTEvent}s
 * ({@code java.awt.Robot} where true OS-level input is needed).
 * AssertJ-Swing was rejected (unmaintained fork chain); Cacio-tta was
 * rejected (JDK-internals coupling). See
 * {@code DialogConstructionSmokeTest}, {@code EditorGestureTest},
 * {@code KeyPadDismissalTest}, {@code MenuBarSpecTest} (the P3
 * menu-bar expectation table), {@code InteractiveSimulatorSmokeTest}
 * (the Runner-thread/EDT handshake); new helpers for this layer belong
 * in this package and must not be required by Layer-1 tests. The
 * display suites run with {@code EdtViolationDetector} installed (issue
 * #162 P2, a hand-rolled RepaintManager hook), so off-EDT Swing access
 * fails the test that provoked it.</li>
 *
 * <li><b>Layer 3 (starter present):</b> rendering assertions -- paint to
 * a BufferedImage without a window and make semantic checks, never
 * brittle pixel goldens. Open: {@link jls.ui.RenderAssert} asserts an
 * element paints, and paints only inside its index bounds plus the
 * culling margin ({@code Circuit.DRAW_MARGIN}) -- the envelope the paint
 * pipeline itself relies on -- swept over a fixture by
 * {@code RenderBoundsTest}, headlessly.</li>
 * </ul>
 *
 * Meaningfulness discipline: every helper assertion in this package is
 * itself pinned by at least one deliberately-failing test
 * (assert-the-assertion, via {@code assertThrows(AssertionError.class, ...)})
 * so the harness cannot silently pass on an empty circuit. Keep that
 * invariant when adding helpers.
 */
package jls.ui;
