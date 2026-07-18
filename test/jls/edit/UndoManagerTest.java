package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Element;

/**
 * Unit tests for {@link UndoManager}, the undo/redo stack policy
 * extracted from {@code SimpleEditor} (issue #84, finding U7). The
 * manager was extracted precisely so these rules - no-op drop, depth
 * bound, base-snapshot floor, restore-before-touch - are assertable
 * headless, without constructing the editor; restore success and
 * failure are simulated through the injected {@link UndoManager.Restorer}.
 *
 * End-to-end behavior (a real undo through the editor's popup menu
 * restoring a deleted gate) stays pinned by
 * {@code jls.ui.EditorGestureTest} under the display substrate.
 */
class UndoManagerTest {

	private static final String CIRCUIT_TEXT =
			"CIRCUIT undotest\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n"
			+ " Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n"
			+ "ENDCIRCUIT\n";

	private Circuit circuit;

	/**
	 * Load the one-element circuit each test mutates for distinct
	 * snapshots.
	 *
	 * @throws Exception if the circuit text fails to load.
	 */
	@BeforeEach
	void loadCircuit() throws Exception {

		circuit = new Circuit("undotest");
		assertTrue(circuit.load(new Scanner(CIRCUIT_TEXT)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
	} // end of loadCircuit method

	/**
	 * Snapshot the circuit as it is now.
	 *
	 * @return the snapshot.
	 */
	private CircuitSnapshot snapshot() {

		return CircuitSnapshot.capture(circuit);
	} // end of snapshot method

	/**
	 * Move the circuit's element one grid space and snapshot, so
	 * successive calls produce genuinely distinct snapshots (the manager
	 * drops byte-identical ones).
	 *
	 * @return a snapshot different from all previous ones.
	 */
	private CircuitSnapshot changedSnapshot() {

		for (Element el : circuit.getElements()) {
			el.move(JLSInfo.spacing, 0);
			break;
		}
		return snapshot();
	} // end of changedSnapshot method

	/**
	 * A restorer that records what it was asked to restore and always
	 * succeeds.
	 */
	private static final class RecordingRestorer
			implements UndoManager.Restorer {

		/** Every snapshot passed to {@link #restore}, in order. */
		final List<CircuitSnapshot> restored =
				new ArrayList<CircuitSnapshot>();

		/**
		 * Record the snapshot and report success.
		 *
		 * @param snap The snapshot to restore.
		 *
		 * @return true always.
		 */
		@Override
		public boolean restore(CircuitSnapshot snap) {

			restored.add(snap);
			return true;
		} // end of restore method
	} // end of RecordingRestorer class

	/**
	 * Two captures of an unchanged circuit are byte-identical; the second
	 * push must be dropped so no-op edits never spend an undo slot.
	 */
	@Test
	void pushDropsNoOpSnapshots() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		manager.push(snapshot());
		assertEquals(1, manager.undoCount(),
				"an identical snapshot must not be pushed again");
	} // end of pushDropsNoOpSnapshots method

	/**
	 * The bottom snapshot is the base state pushed on first paint;
	 * undo with only that must do nothing and never call the restorer.
	 */
	@Test
	void undoRequiresMoreThanTheBaseSnapshot() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		RecordingRestorer restorer = new RecordingRestorer();
		assertFalse(manager.canUndo());
		assertFalse(manager.undo(restorer),
				"undo with only the base snapshot must report false");
		assertTrue(restorer.restored.isEmpty(),
				"the restorer must not run when there is nothing to undo");
		assertEquals(1, manager.undoCount());
	} // end of undoRequiresMoreThanTheBaseSnapshot method

	/**
	 * A successful undo restores the snapshot below the top and moves
	 * the top to the redo stack.
	 */
	@Test
	void undoRestoresThePreviousSnapshotAndEnablesRedo() {

		UndoManager manager = new UndoManager();
		CircuitSnapshot base = snapshot();
		manager.push(base);
		manager.push(changedSnapshot());
		assertTrue(manager.canUndo());
		assertFalse(manager.canRedo());

		RecordingRestorer restorer = new RecordingRestorer();
		assertTrue(manager.undo(restorer));
		assertEquals(1, restorer.restored.size());
		assertTrue(restorer.restored.get(0).sameAs(base),
				"undo must restore the state below the top");
		assertEquals(1, manager.undoCount());
		assertEquals(1, manager.redoCount());
		assertTrue(manager.canRedo());
		assertFalse(manager.canUndo(),
				"only the base is left, so a second undo must be unavailable");
	} // end of undoRestoresThePreviousSnapshotAndEnablesRedo method

	/**
	 * Only a successful restore may touch the stacks: a restorer that
	 * fails (the snapshot no longer loads) must leave undo and redo
	 * exactly as they were.
	 */
	@Test
	void failedRestoreLeavesTheStacksUntouched() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		manager.push(changedSnapshot());

		assertFalse(manager.undo(snap -> false),
				"a failed restore must report false");
		assertEquals(2, manager.undoCount(),
				"a failed restore must not pop the undo stack");
		assertEquals(0, manager.redoCount(),
				"a failed restore must not grow the redo stack");
		assertTrue(manager.canUndo(),
				"the undo must still be available for a retry");
	} // end of failedRestoreLeavesTheStacksUntouched method

	/**
	 * Redo restores the most recently undone snapshot and moves it back
	 * to the undo stack, returning the manager to its pre-undo shape.
	 */
	@Test
	void redoReappliesTheUndoneSnapshot() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		CircuitSnapshot changed = changedSnapshot();
		manager.push(changed);
		assertTrue(manager.undo(new RecordingRestorer()));

		RecordingRestorer restorer = new RecordingRestorer();
		assertTrue(manager.redo(restorer));
		assertEquals(1, restorer.restored.size());
		assertTrue(restorer.restored.get(0).sameAs(changed),
				"redo must restore the state undo moved aside");
		assertEquals(2, manager.undoCount());
		assertEquals(0, manager.redoCount());
		assertTrue(manager.canUndo());
		assertFalse(manager.canRedo());
	} // end of redoReappliesTheUndoneSnapshot method

	/**
	 * Redo with an empty redo stack must do nothing and never call the
	 * restorer.
	 */
	@Test
	void redoWithNothingUndoneDoesNothing() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		RecordingRestorer restorer = new RecordingRestorer();
		assertFalse(manager.canRedo());
		assertFalse(manager.redo(restorer));
		assertTrue(restorer.restored.isEmpty(),
				"the restorer must not run when there is nothing to redo");
	} // end of redoWithNothingUndoneDoesNothing method

	/**
	 * A new edit invalidates the undone future: clearRedos (called from
	 * the editor's markChanged) must empty the redo stack.
	 */
	@Test
	void clearRedosEmptiesTheRedoStack() {

		UndoManager manager = new UndoManager();
		manager.push(snapshot());
		manager.push(changedSnapshot());
		assertTrue(manager.undo(new RecordingRestorer()));
		assertEquals(1, manager.redoCount());

		manager.clearRedos();
		assertEquals(0, manager.redoCount());
		assertFalse(manager.canRedo());
	} // end of clearRedosEmptiesTheRedoStack method

	/**
	 * The depth bound is read through the injected supplier on every
	 * push, and the stack holds at most depth + 1 snapshots (the base
	 * state plus depth undoable ones) with the oldest discarded first -
	 * the exact capacity the pre-extraction editor had, so undo history
	 * does not shrink or grow across the #84 extraction.
	 */
	@Test
	void pushClampsToTheConfiguredDepth() {

		int depth = 3;
		UndoManager manager = new UndoManager(() -> depth);
		List<CircuitSnapshot> pushed = new ArrayList<CircuitSnapshot>();
		for (int i = 0; i < 10; i += 1) {
			CircuitSnapshot snap = changedSnapshot();
			pushed.add(snap);
			manager.push(snap);
		}
		assertEquals(depth + 1, manager.undoCount(),
				"the undo stack must hold at most depth + 1 snapshots");

		// undoing to the floor must walk the newest snapshots in reverse:
		// 10 pushed, 4 retained, so undo restores #8, #7, #6 (0-based)
		RecordingRestorer restorer = new RecordingRestorer();
		while (manager.undo(restorer)) {
			// keep undoing until the base floor stops it
		}
		assertEquals(depth, restorer.restored.size());
		for (int i = 0; i < depth; i += 1) {
			assertTrue(restorer.restored.get(i)
					.sameAs(pushed.get(pushed.size() - 2 - i)),
					"undo must restore the newest retained snapshots, "
							+ "oldest ones discarded");
		}
	} // end of pushClampsToTheConfiguredDepth method

} // end of UndoManagerTest class
