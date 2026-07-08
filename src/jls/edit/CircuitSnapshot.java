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

	private final byte[] deflated;

	private CircuitSnapshot(byte[] deflated) {

		this.deflated = deflated;
	} // end of constructor

	/**
	 * Capture the current state of a circuit.
	 *
	 * @param circuit The circuit to snapshot.
	 *
	 * @return the snapshot.
	 */
	public static CircuitSnapshot capture(Circuit circuit) {

		// Circuit.save clears the changed flag as a side effect; a
		// snapshot must observe, not modify
		boolean changed = circuit.hasChanged();
		StringWriter text = new StringWriter();
		try (PrintWriter output = new PrintWriter(text)) {
			circuit.save(output);
		} finally {
			if (changed) {
				circuit.markChanged();
			}
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
	 */
	public Circuit restore(String name, Graphics g) {

		String text = new String(inflate(deflated), StandardCharsets.UTF_8);
		Circuit restored = new Circuit(name);
		try {
			if (!restored.load(new Scanner(text)) || !restored.finishLoad(g)) {
				return null;
			}
		} catch (Exception ex) {
			JLSInfo.loadError = "snapshot restore failed: " + ex.getMessage();
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
	 */
	public boolean sameAs(CircuitSnapshot other) {

		return other != null && Arrays.equals(deflated, other.deflated);
	} // end of sameAs method

	/**
	 * Retained size of this snapshot, for measurements.
	 *
	 * @return the deflated byte count.
	 */
	public int retainedBytes() {

		return deflated.length;
	} // end of retainedBytes method

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
