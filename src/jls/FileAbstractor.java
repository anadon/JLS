package jls;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableXZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * The single read/write path for circuit files (issue #15).
 *
 * Reading accepts every container format JLS has ever written under the
 * .jls / .jls~ names -- XZ-compressed text (current saves), a zip archive
 * with a JLSCircuit entry (historical saves; JLSCheckpoint for legacy
 * checkpoints), and plain text -- by trying each in turn. Unlike the
 * previous per-caller implementations, a failure to open records why each
 * format probe rejected the file in {@link JLSInfo#loadError} instead of
 * silently returning null.
 *
 * Writing produces the current save format (XZ-compressed UTF-8 text) via
 * a temp file and atomic rename, so a crash mid-write can never leave a
 * truncated file where a complete one used to be.
 */
public final class FileAbstractor {

	private FileAbstractor() {
	}

	/**
	 * Open a circuit file for reading, whatever its container format.
	 *
	 * @param filePath Path to a .jls or .jls~ file.
	 *
	 * @return a Scanner over the circuit text, or null with
	 *         JLSInfo.loadError describing why every format probe failed.
	 */
	public static Scanner openCircuit(String filePath) {

		String name = filePath.replaceAll("\\.jls~$", "");
		name = name.replaceAll("\\.jls$", "");
		if (Util.isValidFileName(name) == null) {
			JLSInfo.loadError = name + " is not a valid circuit file name. "
					+ "It must start with a letter and contain letters, "
					+ "digits and underscores.";
			return null;
		}

		File file = new File(filePath);
		if (!file.isFile()) {
			JLSInfo.loadError = "can't read " + filePath + ": no such file";
			return null;
		}

		StringBuilder reasons = new StringBuilder();
		try {
			return readXZ(file);
		}
		catch (IOException ex) {
			reasons.append("not XZ (").append(reason(ex)).append(")");
		}
		try {
			return readZip(file);
		}
		catch (IOException ex) {
			reasons.append("; not zip (").append(reason(ex)).append(")");
		}
		try {
			return readText(file);
		}
		catch (IOException ex) {
			reasons.append("; not readable as text (").append(reason(ex)).append(")");
		}
		JLSInfo.loadError = filePath + " is not in any known circuit file format: " + reasons;
		return null;
	}

	/**
	 * Write circuit text to the target file in the standard save format
	 * (XZ-compressed UTF-8 text). The write goes to a temp file that is
	 * atomically renamed over the target, so the previous complete file
	 * survives a crash mid-write.
	 *
	 * @param target      The file to (re)place.
	 * @param circuitText The complete circuit text, as produced by Circuit.save.
	 */
	public static void writeCircuit(File target, String circuitText) throws IOException {

		File temp = new File(target.getPath() + ".tmp");
		try {
			try (Writer out = new OutputStreamWriter(
					new XZOutputStream(new FileOutputStream(temp), new LZMA2Options()),
					StandardCharsets.UTF_8)) {
				out.write(circuitText);
			}
			try {
				Files.move(temp.toPath(), target.toPath(),
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			}
			catch (AtomicMoveNotSupportedException ex) {
				Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally {
			temp.delete();
		}
	}

	/**
	 * Read the file as XZ-compressed text.
	 *
	 * @throws IOException if the file is not XZ or holds no text.
	 */
	static Scanner readXZ(File file) throws IOException {

		return nonEmpty(new Scanner(
				new SeekableXZInputStream(new SeekableFileInputStream(file)),
				StandardCharsets.UTF_8));
	}

	/**
	 * Read the file as a zip archive holding the circuit text in a
	 * JLSCircuit entry (or JLSCheckpoint, the legacy checkpoint name).
	 * The entry is drained before the ZipFile is closed: closing the
	 * ZipFile closes every stream obtained from it, which used to
	 * truncate any circuit larger than the Scanner's buffer (issue #2).
	 *
	 * @throws IOException if the file is not a zip or has no circuit entry.
	 */
	static Scanner readZip(File file) throws IOException {

		try (ZipFile archive = new ZipFile(file)) {
			ZipEntry entry = archive.getEntry("JLSCircuit");
			if (entry == null)
				entry = archive.getEntry("JLSCheckpoint");
			if (entry == null)
				throw new IOException("zip archive has no JLSCircuit entry");
			byte[] contents = archive.getInputStream(entry).readAllBytes();
			return nonEmpty(new Scanner(new ByteArrayInputStream(contents), StandardCharsets.UTF_8));
		}
	}

	/**
	 * Read the file as plain circuit text.
	 *
	 * @throws IOException if the file cannot be read or is empty.
	 */
	static Scanner readText(File file) throws IOException {

		return nonEmpty(new Scanner(file, StandardCharsets.UTF_8));
	}

	private static Scanner nonEmpty(Scanner scanner) throws IOException {

		if (!scanner.hasNext()) {
			scanner.close();
			throw new IOException("no content");
		}
		return scanner;
	}

	private static String reason(IOException ex) {

		String message = ex.getMessage();
		return message == null || message.isEmpty() ? ex.getClass().getSimpleName() : message;
	}

} // end of FileAbstractor class
