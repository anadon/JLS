/**
 * Test suite for the {@code jls.util} production package, which holds small,
 * stateless utility helpers used across the JLS circuit editor. These are
 * JUnit&nbsp;5 unit tests that exercise the pure coordinate arithmetic behind
 * element placement, verifying the centering, canvas-center fallback, and
 * never-crash guarantees of the {@code Placement} helper. The tests run fully
 * headless: canvases are sized but never displayed, so no GUI or window system
 * is required.
 */
package jls.util;
