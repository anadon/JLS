/**
 * JUnit 5 test suite for the {@link jls.edit} production package, which
 * implements the JLS schematic editor (SimpleEditor, Editor, and their
 * supporting policies). These tests are mostly targeted regression pins
 * for specific issues in the editing surface: the background checkpoint
 * writer and undo snapshots, wire-drawing gestures and the wire-drag
 * connect path, tri-state/normal bundle connect rules, wire-versus-element
 * collision symmetry, the delete key binding policy, and the Save As
 * duplicate-name check. Because the editor cannot be constructed under the
 * suite's headless mode (its window queries AWT toolkit state that throws
 * {@code HeadlessException}), behavior is exercised through pure functions
 * and model-only seams that SimpleEditor exposes for exactly this purpose,
 * rather than by driving live Swing components.
 */
package jls.edit;
