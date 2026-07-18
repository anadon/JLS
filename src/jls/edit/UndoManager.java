package jls.edit;

import java.util.Stack;
import java.util.function.IntSupplier;

import jls.JLSInfo;

/**
 * The undo/redo bookkeeping extracted from {@code SimpleEditor} (issue
 * #84, finding U7): the two snapshot stacks, the no-op-drop and
 * depth-bound push policy, and the pop/push transitions of undo and redo.
 *
 * The manager owns only stack policy. Actually restoring a snapshot -
 * rebuilding the circuit through the load path, relinking subcircuits,
 * pointing the simulator at the new instance - stays in the editor and is
 * injected per call as a {@link Restorer}, so the transition rules are
 * unit-testable headless (the surefire JVM runs with
 * {@code java.awt.headless=true}, where the editor itself cannot be
 * constructed) while the editor keeps its Swing-bound restore. A restore
 * can fail (the snapshot no longer loads); the stacks are only touched
 * after the restorer reports success, exactly as the pre-extraction code
 * behaved.
 *
 * Capacity (U7 decision on issue #84): whole-circuit snapshots are kept -
 * measured cheap at classroom scale ({@code CircuitSnapshotTest}
 * bounds them under 100 bytes/element) - and the depth bound is read
 * through an injected supplier so issue #76 can turn it into a live
 * preference without touching this class; until then it degrades to
 * {@link JLSInfo#undoStackDepth}.
 */
final class UndoManager {

	/**
	 * How the editor restores a snapshot when undo or redo needs it
	 * installed as the edited circuit. Injected per call so the stack
	 * transitions can be tested without a display.
	 */
	interface Restorer {

		/**
		 * Restore a snapshot and install it as the edited circuit.
		 *
		 * @param snap The snapshot to restore.
		 *
		 * @return true if the snapshot was restored and installed, false
		 *         if it failed to load (the current circuit is untouched
		 *         and the stacks must not change).
		 */
		boolean restore(CircuitSnapshot snap);
	} // end of Restorer interface

	/** Undone-to states, bottom is oldest; the base snapshot sits at the bottom. */
	private final Stack<CircuitSnapshot> undos = new Stack<CircuitSnapshot>();

	/** States undone away from, top is the next redo. */
	private final Stack<CircuitSnapshot> redos = new Stack<CircuitSnapshot>();

	/** Where the depth bound is read from on each push. */
	private final IntSupplier depth;

	/**
	 * Create a manager whose depth bound is
	 * {@link JLSInfo#undoStackDepth}, the pre-#76 hardcoded default.
	 */
	UndoManager() {

		this(() -> JLSInfo.undoStackDepth);
	} // end of constructor

	/**
	 * Create a manager with an injected depth bound, read afresh on every
	 * push - the seam issue #76 turns into a preference, and what the
	 * unit tests use to exercise the bound cheaply.
	 *
	 * @param depth Supplies the maximum number of undoable states; the
	 *              undo stack holds at most depth + 1 snapshots (the
	 *              base state plus depth undoable ones - preserved
	 *              exactly from the pre-extraction code so undo capacity
	 *              does not change).
	 *
	 * @see jls.edit.UndoManagerTest#pushClampsToTheConfiguredDepth()
	 */
	UndoManager(IntSupplier depth) {

		this.depth = depth;
	} // end of constructor

	/**
	 * Push a snapshot of the current state. A snapshot identical to the
	 * top of the stack is dropped - aborted dialogs and cancelled
	 * gestures serialize identically, so no-op edits never spend an undo
	 * slot - and when the stack is full the oldest snapshot is discarded
	 * from the bottom.
	 *
	 * @param snap The snapshot to push.
	 *
	 * @see jls.edit.UndoManagerTest#pushDropsNoOpSnapshots()
	 * @see jls.edit.UndoManagerTest#pushClampsToTheConfiguredDepth()
	 */
	public void push(CircuitSnapshot snap) {

		// an aborted or no-op change serializes identically to the top
		// of the stack and is not pushed again (#18)
		if (!undos.isEmpty() && undos.peek().sameAs(snap)) {
			return;
		}

		// see if undo stack is full
		if (undos.size() > depth.getAsInt()) {

			// delete bottom of stack
			undos.remove(0);
		}

		// save for undo
		undos.push(snap);
	} // end of push method

	/**
	 * Discard all redo states. Called when a new edit is made: the
	 * undone future it invalidates can no longer be redone.
	 *
	 * @see jls.edit.UndoManagerTest#clearRedosEmptiesTheRedoStack()
	 */
	public void clearRedos() {

		redos.clear();
	} // end of clearRedos method

	/**
	 * Whether undo has anywhere to go. The bottom snapshot is the base
	 * state pushed on first paint, so one snapshot means nothing to undo.
	 *
	 * @return true if a call to {@link #undo} can restore a prior state.
	 *
	 * @see jls.edit.UndoManagerTest#undoRequiresMoreThanTheBaseSnapshot()
	 */
	public boolean canUndo() {

		return undos.size() > 1;
	} // end of canUndo method

	/**
	 * Whether redo has anywhere to go.
	 *
	 * @return true if a call to {@link #redo} can reapply an undone state.
	 *
	 * @see jls.edit.UndoManagerTest#undoRestoresThePreviousSnapshotAndEnablesRedo()
	 */
	public boolean canRedo() {

		return !redos.isEmpty();
	} // end of canRedo method

	/**
	 * Undo: restore the previous snapshot, then move the current one to
	 * the redo stack. Only a successful restore may touch the stacks -
	 * on failure both stacks are exactly as before.
	 *
	 * @param restorer Installs the snapshot as the edited circuit.
	 *
	 * @return true if a state was restored, false if there was nothing
	 *         to undo or the restore failed.
	 *
	 * @see jls.edit.UndoManagerTest#undoRestoresThePreviousSnapshotAndEnablesRedo()
	 * @see jls.edit.UndoManagerTest#failedRestoreLeavesTheStacksUntouched()
	 */
	public boolean undo(Restorer restorer) {

		// no undo left if stack only has a copy of the original circuit
		if (!canUndo()) {
			return false;
		}

		// restore the previous snapshot first; only a successful
		// restore may touch the stacks
		CircuitSnapshot current = undos.get(undos.size() - 1);
		CircuitSnapshot previous = undos.get(undos.size() - 2);
		if (!restorer.restore(previous)) {
			return false;
		}
		undos.pop();
		redos.push(current);
		return true;
	} // end of undo method

	/**
	 * Redo: restore the most recently undone snapshot, then move it back
	 * to the undo stack. Only a successful restore may touch the stacks.
	 *
	 * @param restorer Installs the snapshot as the edited circuit.
	 *
	 * @return true if a state was restored, false if there was nothing
	 *         to redo or the restore failed.
	 *
	 * @see jls.edit.UndoManagerTest#redoReappliesTheUndoneSnapshot()
	 * @see jls.edit.UndoManagerTest#redoWithNothingUndoneDoesNothing()
	 */
	public boolean redo(Restorer restorer) {

		// if nothing on the redo stack, then there is nothing to do
		if (redos.isEmpty()) {
			return false;
		}

		// restore the snapshot first; only a successful restore may
		// touch the stacks
		CircuitSnapshot next = redos.peek();
		if (!restorer.restore(next)) {
			return false;
		}
		redos.pop();
		undos.push(next);
		return true;
	} // end of redo method

	/**
	 * The number of snapshots on the undo stack, base state included.
	 * Test and future-UI seam (the tracker notes undo availability has
	 * no UI indication today).
	 *
	 * @return the undo stack size.
	 */
	public int undoCount() {

		return undos.size();
	} // end of undoCount method

	/**
	 * The number of snapshots on the redo stack.
	 *
	 * @return the redo stack size.
	 */
	public int redoCount() {

		return redos.size();
	} // end of redoCount method

} // end of UndoManager class
