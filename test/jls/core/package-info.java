/**
 * JUnit 5 test suite for the {@link jls.core} production package, the
 * headless model core extracted in issue #77. These tests pin the value
 * types that replace AWT geometry in the core — notably that
 * {@link jls.core.Bounds} reproduces {@link java.awt.Rectangle} bug for
 * bug ({@code BoundsGeometryParityTest}) — so that geometry moved off AWT
 * stays byte-identical.
 */
package jls.core;
