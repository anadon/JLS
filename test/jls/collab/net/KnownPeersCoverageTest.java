package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch-coverage companion to {@link KnownPeersTest} (issues #168,
 * #159): the store's remaining reader rejections (empty file, unquoted
 * name, invalid name), the tolerated blank lines, and the plain
 * accessors the model layer reads.
 */
class KnownPeersCoverageTest {

	/** A fingerprint-shaped constant. */
	private static final String KEY_A = "a".repeat(64);

	/** A second fingerprint-shaped constant. */
	private static final String KEY_B = "b".repeat(64);

	/** The store's header line. */
	private static final String HEADER = "JLS known peers v1";

	/**
	 * Writes store content and asserts loading it throws.
	 * @param file the store file
	 * @param content the file text
	 */
	private static void loadThrows(Path file, String content) {
		try {
			Files.writeString(file, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("could not write fixture", e);
		}
		assertThrows(IOException.class, () -> KnownPeers.load(file));
	} // end of loadThrows method

	@Test
	void anEmptyFileHasNoHeaderAndIsRejected(@TempDir Path config) {
		loadThrows(config.resolve("known-peers"), "");
	} // end of anEmptyFileHasNoHeaderAndIsRejected method

	@Test
	void aLineWithoutAQuotedNameIsRejected(@TempDir Path config) {
		loadThrows(config.resolve("known-peers"),
				HEADER + "\n" + KEY_A + " 5 unquoted\n");
	} // end of aLineWithoutAQuotedNameIsRejected method

	@Test
	void aLineWithAnInvalidNameIsRejected(@TempDir Path config) {
		// a name of 49 characters is over the 48-character limit the
		// shared display-name validation enforces
		String tooLong = "x".repeat(49);
		loadThrows(config.resolve("known-peers"),
				HEADER + "\n" + KEY_A + " 5 " + '"' + tooLong + '"' + "\n");
	} // end of aLineWithAnInvalidNameIsRejected method

	@Test
	void blankLinesInTheStoreAreTolerated(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("known-peers");
		String content = HEADER + "\n\n" + KEY_A + " 7 " + '"' + "alex"
				+ '"' + "\n\n" + KEY_B + " 8 " + '"' + "sam" + '"' + "\n";
		Files.writeString(file, content, StandardCharsets.UTF_8);
		KnownPeers store = KnownPeers.load(file);
		assertEquals(2, store.fingerprints().size());
		assertTrue(store.fingerprints().contains(KEY_A));
		assertEquals("alex", store.nameOf(KEY_A));
		assertEquals("sam", store.nameOf(KEY_B));
	} // end of blankLinesInTheStoreAreTolerated method

	@Test
	void nameOfAnUnknownFingerprintIsNull(@TempDir Path config)
			throws IOException {
		KnownPeers store = KnownPeers.load(config.resolve("known-peers"));
		assertNull(store.nameOf(KEY_A));
		assertTrue(store.fingerprints().isEmpty());
	} // end of nameOfAnUnknownFingerprintIsNull method

	@Test
	void reRecordingAFingerprintReplacesItsNameInPlace(
			@TempDir Path config) throws IOException {
		Path file = config.resolve("known-peers");
		KnownPeers store = KnownPeers.load(file);
		store.recordVerified(KEY_A, "alex", 1L);
		store.recordVerified(KEY_A, "alexandra", 2L);
		assertEquals(1, store.fingerprints().size(),
				"the same key must not create a second record");
		assertEquals("alexandra", KnownPeers.load(file).nameOf(KEY_A));
	} // end of reRecordingAFingerprintReplacesItsNameInPlace method

	@Test
	void theDefaultFileLivesUnderTheConfigTree() {
		assertTrue(KnownPeers.defaultFile().toString()
				.contains("known-peers"));
	} // end of theDefaultFileLivesUnderTheConfigTree method
}
