package jls.edit;

import java.awt.Graphics;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import jls.Circuit;
import jls.JLSInfo;

/**
 * A compact undo snapshot of a circuit (issue #18, stage 1): the circuit
 * serialized in the standard save format and deflated, instead of a live
 * deep copy of the whole object graph. Restoring runs the ordinary
 * load/finishLoad path, so snapshot semantics are exactly save/load
 * semantics — which the round-trip suite (#14) pins.
 *
 * Two snapshots of an unchanged circuit are byte-identical, which lets the
 * undo stack drop no-op entries (aborted dialogs, cancelled gestures)
 * for free.
 */
public final class CircuitSnapshot {

	/** The deflated save-format bytes of the captured circuit. */
	private final byte[] deflated;

	/**
	 * Wrap already-deflated save bytes; use {@link #capture} to create one.
	 *
	 * @param deflated The deflated save-format bytes to retain.
	 */
	private CircuitSnapshot(byte[] deflated) {

		this.deflated = deflated;
	} // end of constructor

	/**
	 * Capture the current state of a circuit.
	 *
	 * @param circuit The circuit to snapshot.
	 *
	 * @return the snapshot.
	 *
	 * @jls.testedby jls.StableElementIdTest#undoRestorePreservesIds()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#capturePreservesTheChangedFlag()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#captureRestoreReproducesTheCircuit()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#changedCircuitSnapshotsDifferently()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#restoreDoesNotPerturbSnapshotIdentity()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#unchangedCircuitSnapshotsIdentically()
	 */
	public static CircuitSnapshot capture(Circuit circuit) {

		StringWriter text = new StringWriter();
		try (PrintWriter output = new PrintWriter(text)) {
			circuit.save(output);
		}
		return new CircuitSnapshot(deflate(
				text.toString().getBytes(StandardCharsets.UTF_8)));
	} // end of capture method

	/**
	 * Restore this snapshot into a fresh circuit via the normal load path.
	 *
	 * @param name The name for the restored circuit (load keeps it when
	 *            non-empty rather than taking the name from the text).
	 * @param g A graphics context for element sizing, or null.
	 *
	 * @return the restored circuit, or null if the snapshot did not load
	 *         (JLSInfo.loadError says why).
	 *
	 * @jls.testedby jls.StableElementIdTest#undoRestorePreservesIds()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#captureRestoreReproducesTheCircuit()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#restoreDoesNotPerturbSnapshotIdentity()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 */
	public Circuit restore(String name, Graphics g) {

		String text = new String(inflate(deflated), StandardCharsets.UTF_8);
		Circuit restored = new Circuit(name);
		try {
			if (!restored.load(new Scanner(text)) || !restored.finishLoad(g)) {
				return null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JLSInfo.setLoadError(jls.LoadError.of(
					jls.LoadError.Category.MALFORMED,
					"snapshot restore failed"
							+ (ex.getMessage() == null ? ""
									: ": " + ex.getMessage()),
					"Undo/redo state may be inconsistent - save your "
							+ "work under a new name and reopen it."));
			return null;
		}
		return restored;
	} // end of restore method

	/**
	 * Whether another snapshot captured identical circuit content.
	 *
	 * @param other The other snapshot.
	 *
	 * @return true if the serialized content is identical.
	 *
	 * @jls.testedby jls.edit.CircuitSnapshotTest#changedCircuitSnapshotsDifferently()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#restoreDoesNotPerturbSnapshotIdentity()
	 * @jls.testedby jls.edit.CircuitSnapshotTest#unchangedCircuitSnapshotsIdentically()
	 */
	public boolean sameAs(CircuitSnapshot other) {

		return other != null && Arrays.equals(deflated, other.deflated);
	} // end of sameAs method

	/**
	 * Retained size of this snapshot, for measurements.
	 *
	 * @return the deflated byte count.
	 *
	 * @jls.testedby jls.edit.CircuitSnapshotTest#snapshotIsCompact()
	 */
	public int retainedBytes() {

		return deflated.length;
	} // end of retainedBytes method

	/**
	 * Compress save-format bytes with the fastest deflate setting.
	 *
	 * @param data The raw bytes to compress.
	 *
	 * @return the deflated bytes.
	 */
	private static byte[] deflate(byte[] data) {

		Deflater deflater = new Deflater(Deflater.BEST_SPEED);
		try {
			deflater.setInput(data);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream(
					Math.max(64, data.length / 4));
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return out.toByteArray();
		} finally {
			deflater.end();
		}
	} // end of deflate method

	/**
	 * Decompress bytes produced by {@link #deflate}; truncated input yields
	 * whatever inflated so far, leaving the loader to report the failure.
	 *
	 * @param data The deflated bytes to expand.
	 *
	 * @return the inflated bytes.
	 *
	 * @throws IllegalStateException if the data is not a valid deflate stream.
	 */
	private static byte[] inflate(byte[] data) {

		Inflater inflater = new Inflater();
		try {
			inflater.setInput(data);
			ByteArrayOutputStream out = new ByteArrayOutputStream(
					Math.max(64, data.length * 4));
			byte[] buffer = new byte[8192];
			while (!inflater.finished()) {
				int n = inflater.inflate(buffer);
				if (n == 0 && inflater.needsInput()) {
					break; // truncated input; loader will report the failure
				}
				out.write(buffer, 0, n);
			}
			return out.toByteArray();
		} catch (DataFormatException ex) {
			throw new IllegalStateException("corrupt undo snapshot", ex);
		} finally {
			inflater.end();
		}
	} // end of inflate method

} // end of CircuitSnapshot class
