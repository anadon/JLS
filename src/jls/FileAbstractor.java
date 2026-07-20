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

import org.jspecify.annotations.Nullable;

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
 * Writing produces the current save format (XZ-compressed UTF-8 text) by
 * default, or the identical text uncompressed when the caller opts in
 * (issue #129) -- either way via a temp file and atomic rename, so a
 * crash mid-write can never leave a truncated file where a complete one
 * used to be.
 */
public final class FileAbstractor {

	/**
	 * The on-disk container a save is written in. XZ is the default
	 * (issue #21 chose it for size); PLAIN_TEXT is the opt-in interchange
	 * form (issue #129): the same circuit text minus the compression
	 * wrapper, so version control gets meaningful diffs and readers
	 * without an XZ decoder (the 4.6-4.10 fork lineage) can open the
	 * file. Reading needs no counterpart: openCircuit sniffs both.
	 */
	public enum Container {

		/** The default save container: XZ-compressed UTF-8 circuit text. */
		XZ,

		/** The opt-in interchange container: bare UTF-8 circuit text. */
		PLAIN_TEXT
	}

	/**
	 * Upper bound on the circuit text a container may expand to. Circuit
	 * files are shared between students and instructors by design, so a
	 * tiny archive inflating to gigabytes is treated as hostile, not as
	 * a big circuit (issue #38; SECURITY.md documents live attacks).
	 */
	static final long MAX_CIRCUIT_TEXT_BYTES = 64L << 20;

	/**
	 * Not instantiable: FileAbstractor is a static read/write utility.
	 */
	private FileAbstractor() {
	}

	/**
	 * Open a circuit file for reading, whatever its container format.
	 *
	 * @param filePath Path to a .jls or .jls~ file.
	 *
	 * @return a Scanner over the circuit text, or null with
	 *         JLSInfo.loadError describing why every format probe failed.
	 *
	 * @jls.testedby jls.CliTextSaveTest#theConvertedFileHoldsTheSameCircuit()
	 * @jls.testedby jls.ContainerMutationFuzzTest#loadMutant()
	 * @jls.testedby jls.ContainerMutationFuzzTest#theUnmutatedBaselinesActuallyLoad()
	 * @jls.testedby jls.FileAbstractorTest#bothContainersLoadTheSameCircuitText()
	 * @jls.testedby jls.FileAbstractorTest#emptyFileIsRejectedNotReturnedEmpty()
	 * @jls.testedby jls.FileAbstractorTest#invalidCircuitNameIsRejected()
	 * @jls.testedby jls.FileAbstractorTest#missingFileReportsWhy()
	 * @jls.testedby jls.FileAbstractorTest#plainTextWriteIsTheBareCircuitText()
	 * @jls.testedby jls.FileAbstractorTest#unreadableFileReportsPerFormatReasons()
	 * @jls.testedby jls.FileAbstractorTest#writeCircuitReplacesExistingFileAndLeavesNoTemp()
	 * @jls.testedby jls.FileAbstractorTest#writeCircuitRoundTripsThroughTheSniffer()
	 * @jls.testedby jls.FileFormatSupport#openWithFormatSniffer()
	 * @jls.testedby jls.FileHandleReleaseTest#assertOpenReleasesTheFile()
	 * @jls.testedby jls.UntrustedFileHardeningTest#oversizedZipEntryIsRejected()
	 * @jls.testedby jls.UntrustedFileHardeningTest#sniffingCascadeDoesNotLeakFileDescriptors()
	 * @jls.testedby jls.edit.CheckpointWriterTest#awaitWritten()
	 * @jls.testedby jls.edit.CheckpointWriterTest#checkpointIsWrittenAndLoadable()
	 */
	public static @Nullable Scanner openCircuit(String filePath) {

		String name = filePath.replaceAll("\\.jls~$", "");
		name = name.replaceAll("\\.jls$", "");
		if (Util.isValidFileName(name) == null) {
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.NOT_A_CIRCUIT,
					name + " is not a valid circuit file name",
					"Rename the file so its name starts with a letter "
							+ "and contains only letters, digits and "
							+ "underscores."));
			return null;
		}

		File file = new File(filePath);
		if (!file.isFile()) {
			JLSInfo.setLoadError(LoadError.of(LoadError.Category.IO_ERROR,
					"can't read " + filePath + ": no such file",
					"Check the path, and that the file has not been "
							+ "moved or deleted."));
			return null;
		}

		StringBuilder reasons = new StringBuilder();
		boolean overLimit = false;
		try {
			return readXZ(file);
		}
		catch (IOException ex) {
			overLimit |= isOverLimit(ex);
			reasons.append("not XZ (").append(reason(ex)).append(")");
		}
		try {
			return readZip(file);
		}
		catch (IOException ex) {
			overLimit |= isOverLimit(ex);
			reasons.append("; not zip (").append(reason(ex)).append(")");
		}
		try {
			return readText(file);
		}
		catch (IOException ex) {
			overLimit |= isOverLimit(ex);
			reasons.append("; not readable as text (").append(reason(ex)).append(")");
		}
		if (overLimit) {
			// a probe recognized the container but its contents blow
			// past the hostile-input cap (issue #38)
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.LIMIT_EXCEEDED,
					filePath + " could not be opened: " + reasons,
					"JLS refuses circuit files that expand past "
							+ (MAX_CIRCUIT_TEXT_BYTES >> 20) + " MiB; if "
							+ "this file is really yours, re-save it "
							+ "from a working copy of the circuit."));
			return null;
		}
		JLSInfo.setLoadError(LoadError.of(LoadError.Category.NOT_A_CIRCUIT,
				filePath + " is not in any known circuit file format: "
						+ reasons,
				"Make sure this is a .jls circuit file saved by JLS, "
						+ "not some other kind of file."));
		return null;
	}

	/**
	 * Whether a probe failure means the hostile-input size cap was hit,
	 * rather than a format mismatch.
	 *
	 * @param ex The exception a format probe rejected the file with.
	 * @return true if the failure was the circuit size limit, false otherwise.
	 */
	private static boolean isOverLimit(IOException ex) {

		String message = ex.getMessage();
		return message != null && message.contains("circuit size limit");
	}

	/**
	 * Write circuit text to the target file in the standard save format
	 * (XZ-compressed UTF-8 text). The write goes to a temp file that is
	 * atomically renamed over the target, so the previous complete file
	 * survives a crash mid-write.
	 *
	 * @param target      The file to (re)place.
	 * @param circuitText The complete circuit text, as produced by Circuit.save.
	 *
	 * @throws IOException if the temp file cannot be written or renamed
	 *         over the target.
	 *
	 * @jls.testedby jls.FileAbstractorTest#defaultWriteIsStillXZCompressed()
	 * @jls.testedby jls.FileAbstractorTest#plainTextWriteReplacesAnXZFileAtomically()
	 * @jls.testedby jls.FileAbstractorTest#writeCircuitReplacesExistingFileAndLeavesNoTemp()
	 * @jls.testedby jls.FileAbstractorTest#writeCircuitRoundTripsThroughTheSniffer()
	 */
	public static void writeCircuit(File target, String circuitText) throws IOException {

		writeCircuit(target, circuitText, Container.XZ);
	}

	/**
	 * Write circuit text to the target file in the named container:
	 * Container.XZ is the standard save format, Container.PLAIN_TEXT the
	 * opt-in uncompressed form (issue #129). Both containers share the
	 * temp-file/atomic-rename machinery, so the previous complete file
	 * survives a crash mid-write either way.
	 *
	 * @param target      The file to (re)place.
	 * @param circuitText The complete circuit text, as produced by Circuit.save.
	 * @param container   The on-disk container to wrap the text in.
	 *
	 * @throws IOException if the temp file cannot be written or renamed
	 *         over the target.
	 *
	 * @jls.testedby jls.FileAbstractorTest#bothContainersLoadTheSameCircuitText()
	 * @jls.testedby jls.FileAbstractorTest#plainTextWriteIsTheBareCircuitText()
	 * @jls.testedby jls.FileAbstractorTest#plainTextWriteReplacesAnXZFileAtomically()
	 */
	public static void writeCircuit(File target, String circuitText,
			Container container) throws IOException {

		File temp = new File(target.getPath() + ".tmp");
		try {
			try (FileOutputStream rawOut = new FileOutputStream(temp);
					Writer out = new OutputStreamWriter(
							container == Container.XZ
									? new XZOutputStream(rawOut,
											new LZMA2Options())
									: rawOut,
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
	 * @param file The file to read.
	 * @return a Scanner over the decompressed circuit text.
	 * @throws IOException if the file is not XZ or holds no text.
	 */
	static Scanner readXZ(File file) throws IOException {

		// the XZ constructor throws by design for every legacy zip or
		// plain-text file in the sniffing cascade; the raw stream must
		// not leak a file descriptor each time (issue #38)
		SeekableFileInputStream raw = new SeekableFileInputStream(file);
		SeekableXZInputStream xz;
		try {
			xz = new SeekableXZInputStream(raw);
		}
		catch (IOException ex) {
			raw.close();
			throw ex;
		}
		catch (RuntimeException ex) {
			raw.close();
			throw ex;
		}
		// drain before returning: a Scanner over the live stream would
		// keep the file open for as long as the caller holds it, and on
		// Windows an open handle blocks deletion (issue #111) - the zip
		// path already reads into memory, and the expansion is bounded
		byte[] contents;
		try (BoundedInputStream bounded = new BoundedInputStream(xz)) {
			contents = bounded.readAllBytes();
		}
		return nonEmpty(new Scanner(new ByteArrayInputStream(contents),
				StandardCharsets.UTF_8));
	}

	/**
	 * Read the file as a zip archive holding the circuit text in a
	 * JLSCircuit entry (or JLSCheckpoint, the legacy checkpoint name).
	 * The entry is drained before the ZipFile is closed: closing the
	 * ZipFile closes every stream obtained from it, which used to
	 * truncate any circuit larger than the Scanner's buffer (issue #2).
	 *
	 * @param file The file to read.
	 * @return a Scanner over the extracted circuit text.
	 * @throws IOException if the file is not a zip or has no circuit entry.
	 *
	 * @jls.testedby jls.FileFormatSupport#openAsZip()
	 */
	static Scanner readZip(File file) throws IOException {

		try (ZipFile archive = new ZipFile(file)) {
			ZipEntry entry = archive.getEntry("JLSCircuit");
			if (entry == null)
				entry = archive.getEntry("JLSCheckpoint");
			if (entry == null)
				throw new IOException("zip archive has no JLSCircuit entry");
			// bound the expansion: the declared entry size is
			// attacker-controlled and readAllBytes trusted it (issue #38)
			byte[] contents = archive.getInputStream(entry)
					.readNBytes((int) MAX_CIRCUIT_TEXT_BYTES + 1);
			if (contents.length > MAX_CIRCUIT_TEXT_BYTES) {
				throw new IOException("zip entry expands past the "
						+ (MAX_CIRCUIT_TEXT_BYTES >> 20)
						+ " MiB circuit size limit");
			}
			return nonEmpty(new Scanner(new ByteArrayInputStream(contents), StandardCharsets.UTF_8));
		}
	}

	/**
	 * Read the file as plain circuit text.
	 *
	 * @param file The file to read.
	 * @return a Scanner over the circuit text.
	 * @throws IOException if the file cannot be read or is empty.
	 */
	static Scanner readText(File file) throws IOException {

		if (file.length() > MAX_CIRCUIT_TEXT_BYTES) {
			throw new IOException("file exceeds the "
					+ (MAX_CIRCUIT_TEXT_BYTES >> 20)
					+ " MiB circuit size limit");
		}
		// read into memory rather than scanning the file directly, so no
		// handle stays open behind the returned Scanner (issue #111);
		// decode strictly, as Scanner(File) did - the text probe is what
		// rejects binary non-circuit files in the sniffing cascade
		byte[] contents = Files.readAllBytes(file.toPath());
		String text;
		try {
			text = StandardCharsets.UTF_8.newDecoder()
					.decode(java.nio.ByteBuffer.wrap(contents)).toString();
		} catch (java.nio.charset.CharacterCodingException ex) {
			throw new IOException("not UTF-8 text");
		}
		return nonEmpty(new Scanner(text));
	}

	/**
	 * An input stream that refuses to hand out more than
	 * MAX_CIRCUIT_TEXT_BYTES in total, so a hostile XZ stream cannot
	 * decompress without bound (issue #38).
	 */
	private static final class BoundedInputStream extends java.io.FilterInputStream {

		/** The number of bytes this stream may still hand out. */
		private long remaining = MAX_CIRCUIT_TEXT_BYTES;

		/**
		 * Wrap a stream, capping the total bytes it may hand out at
		 * MAX_CIRCUIT_TEXT_BYTES.
		 *
		 * @param in The underlying stream to bound.
		 */
		BoundedInputStream(java.io.InputStream in) {
			super(in);
		}

		/**
		 * Read one byte, failing once the running total exceeds the cap.
		 *
		 * @return the byte read, or -1 at end of stream.
		 * @throws IOException if the stream expands past the size cap.
		 */
		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b >= 0 && --remaining < 0) {
				throw new IOException(overrun());
			}
			return b;
		}

		/**
		 * Read into a buffer, failing once the running total exceeds the cap.
		 *
		 * @param buf The destination buffer.
		 * @param off The offset in buf to write the first byte at.
		 * @param len The maximum number of bytes to read.
		 * @return the number of bytes read, or -1 at end of stream.
		 * @throws IOException if the stream expands past the size cap.
		 */
		@Override
		public int read(byte[] buf, int off, int len) throws IOException {
			int n = super.read(buf, off, len);
			if (n > 0) {
				remaining -= n;
				if (remaining < 0) {
					throw new IOException(overrun());
				}
			}
			return n;
		}

		/**
		 * The message thrown when the bounded stream exceeds the cap.
		 *
		 * @return the size-limit error message.
		 */
		private static String overrun() {
			return "compressed stream expands past the "
					+ (MAX_CIRCUIT_TEXT_BYTES >> 20)
					+ " MiB circuit size limit";
		}
	}

	/**
	 * Guard against a container that decoded but held no circuit text,
	 * so an empty file is rejected rather than returned as a valid Scanner.
	 *
	 * @param scanner A Scanner over the decoded container contents.
	 * @return the same scanner when it has content.
	 * @throws IOException if the scanner has no tokens (the file is empty);
	 *         the scanner is closed first.
	 */
	private static Scanner nonEmpty(Scanner scanner) throws IOException {

		if (!scanner.hasNext()) {
			scanner.close();
			throw new IOException("no content");
		}
		return scanner;
	}

	/**
	 * A short human-readable reason for a probe failure, for the combined
	 * load-error message; falls back to the exception class name when the
	 * exception carries no message.
	 *
	 * @param ex The exception a format probe rejected the file with.
	 * @return its message, or the simple class name if there is none.
	 */
	private static String reason(IOException ex) {

		String message = ex.getMessage();
		return message == null || message.isEmpty() ? ex.getClass().getSimpleName() : message;
	}

} // end of FileAbstractor class
