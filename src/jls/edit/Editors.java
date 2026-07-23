package jls.edit;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import jls.Circuit;

/**
 * The circuit-to-editor association, inverted out of the model (issue
 * #77). A {@link Circuit} used to hold an {@link Editor} back-reference
 * (its {@code editor} field, {@code setEditor}/{@code getEditor}), which
 * made the headless model depend on the GUI. That edge is inverted here:
 * the editor side owns the mapping, and the model knows nothing about it.
 *
 * <p>The map is keyed by object identity ({@link IdentityHashMap}), so a
 * circuit is matched as itself regardless of {@code equals}. Entries are
 * added when an editor opens a circuit ({@link #register}) and removed
 * when it closes ({@link #unregister}); callers must keep that discipline
 * or a closed circuit lingers in the map. {@link #of} returns null for a
 * circuit with no editor — exactly the states the old null {@code editor}
 * field represented.
 */
public final class Editors {

	/** Circuit -> its editor, by identity. */
	private static final Map<Circuit, Editor> EDITORS =
			new IdentityHashMap<Circuit, Editor>();

	private Editors() {} // no instances

	/**
	 * Associate an editor with the circuit it edits (replaces
	 * {@code circuit.setEditor(editor)} for a non-null editor).
	 *
	 * @param circuit The circuit being edited.
	 * @param editor The editor editing it.
	 */
	public static void register(Circuit circuit, Editor editor) {
		EDITORS.put(circuit, editor);
	} // end of register method

	/**
	 * Drop a circuit's editor association (replaces
	 * {@code circuit.setEditor(null)}). No-op if it had none.
	 *
	 * @param circuit The circuit whose editor is going away.
	 */
	public static void unregister(Circuit circuit) {
		EDITORS.remove(circuit);
	} // end of unregister method

	/**
	 * The editor currently editing a circuit, or null if none (replaces
	 * {@code circuit.getEditor()}).
	 *
	 * @param circuit The circuit to look up.
	 * @return its editor, or null if it is not being edited.
	 */
	public static @Nullable Editor of(Circuit circuit) {
		return EDITORS.get(circuit);
	} // end of of method

} // end of Editors class
