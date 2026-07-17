package jls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Helpers for writing circuit files in the three on-disk container formats
 * JLS accepts (plain text, zip with a JLSCircuit entry, XZ) and for opening
 * them through the FileAbstractor format-sniffing loader.
 */
final class FileFormatSupport {

	private FileFormatSupport() {
	}

	/** Write content as a plain text file. */
	static void writeText(File file, String content) throws Exception {
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
	}

	/** Write content the way historical JLS saved circuits: a zip archive with a JLSCircuit entry. */
	static void writeZip(File file, String content) throws Exception {
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
			zip.putNextEntry(new ZipEntry("JLSCircuit"));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
	}

	/** Write content the way the current save path does: XZ-compressed text. */
	static void writeXZ(File file, String content) throws Exception {
		try (FileOutputStream rawOut = new FileOutputStream(file);
				Writer out = new OutputStreamWriter(
						new XZOutputStream(rawOut, new LZMA2Options()),
						StandardCharsets.UTF_8)) {
			out.write(content);
		}
	}

	/** Open through the FileAbstractor format-sniffing chain. */
	static Scanner openWithFormatSniffer(File file) throws Exception {
		return FileAbstractor.openCircuit(file.getAbsolutePath());
	}

	/** Probe the file as a zip circuit archive; null if it isn't one. */
	static Scanner openAsZip(File file) throws Exception {
		try {
			return FileAbstractor.readZip(file);
		} catch (java.io.IOException rejected) {
			return null;
		}
	}

	/** Drain a scanner into a string, preserving line structure. */
	static String drain(Scanner scanner) {
		StringBuilder sb = new StringBuilder();
		while (scanner.hasNextLine()) {
			sb.append(scanner.nextLine()).append('\n');
		}
		scanner.close();
		return sb.toString();
	}

	/**
	 * A saved circuit minus its " String sid " lines, for comparisons
	 * against pre-#165 fixture text, which has none. Saves are otherwise
	 * byte-deterministic (#166: canonical order sorted by stable id, ids
	 * and refs assigned in that order), so this is the only masking a
	 * saved-content comparison ever needs - and none is needed when both
	 * sides went through a post-#165 save.
	 */
	static String stripStableIds(String saved) {
		StringBuilder kept = new StringBuilder();
		for (String line : saved.split("\n")) {
			if (!line.startsWith(" String sid ")) {
				kept.append(line).append('\n');
			}
		}
		return kept.toString();
	}
}
