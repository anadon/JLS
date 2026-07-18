/**
 * The interactive circuit editor: the Swing GUI through which a user builds and
 * modifies a {@link jls.Circuit} by placing, wiring, selecting, moving, and
 * deleting elements. {@link jls.edit.SimpleEditor} is the abstract editing
 * surface that manages the drawing canvas, the element tool palette, and the
 * mouse and keyboard editing gestures, while {@link jls.edit.Editor} layers on
 * the application's naming and file save/save-as/close behavior. Supporting
 * classes handle undo/redo state via compact {@link jls.edit.CircuitSnapshot}s
 * and platform-specific key bindings such as {@link jls.edit.DeleteKeyPolicy}.
 * This is the design-time counterpart to the runtime simulation packages: it
 * produces and edits circuits rather than executing them.
 */
package jls.edit;
