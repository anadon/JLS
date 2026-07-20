package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch-coverage companion to {@link IdentityKeyTest} (issues #168,
 * #159). The sibling suite pins the round-trip and fingerprint contract;
 * this one drives the identity-file reader's every truncation and cap
 * rejection, and the display-name sanitiser's byte- and char-limit
 * branches - the paths a hostile or corrupt file exercises.
 */
class IdentityKeyCoverageTest {

	/** The magic bytes an identity file opens with. */
	private static final byte[] MAGIC =
			"JLSID1".getBytes(StandardCharsets.US_ASCII);

	/**
	 * Writes {@code bytes} to a fresh file and asserts loading it fails
	 * with an IOException whose message names the problem.
	 * @param dir the temp directory
	 * @param bytes the malformed file content
	 * @param fragment a substring the message must contain
	 * @throws IOException if writing the fixture fails
	 */
	private static void loadFails(Path dir, byte[] bytes, String fragment)
			throws IOException {
		Path file = dir.resolve("bad-" + Math.abs(Arrays.hashCode(bytes)));
		Files.write(file, bytes);
		IOException e = assertThrows(IOException.class,
				() -> IdentityKey.loadOrCreate(file));
		assertTrue(e.getMessage().contains(fragment),
				"message was: " + e.getMessage());
	} // end of loadFails method

	// ------------------------------------------------------------------
	// malformed identity files
	// ------------------------------------------------------------------

	@Test
	void anOversizeFileIsRejectedBeforeParsing(@TempDir Path dir)
			throws IOException {
		byte[] huge = new byte[5000];
		System.arraycopy(MAGIC, 0, huge, 0, MAGIC.length);
		loadFails(dir, huge, "exceeds");
	} // end of anOversizeFileIsRejectedBeforeParsing method

	@Test
	void aFileShorterThanTheMagicIsRejected(@TempDir Path dir)
			throws IOException {
		loadFails(dir, new byte[] {'J', 'L', 'S'}, "too short");
	} // end of aFileShorterThanTheMagicIsRejected method

	@Test
	void aWrongMagicIsRejected(@TempDir Path dir) throws IOException {
		loadFails(dir, "XXXXXXyz".getBytes(StandardCharsets.US_ASCII),
				"JLSID1 magic");
	} // end of aWrongMagicIsRejected method

	@Test
	void aFileEndingBeforeAFieldLengthIsRejected(@TempDir Path dir)
			throws IOException {
		// magic then nothing: no room for the private-key length prefix
		loadFails(dir, MAGIC.clone(), "before the private key length");
	} // end of aFileEndingBeforeAFieldLengthIsRejected method

	@Test
	void aFieldOverItsCapIsRejected(@TempDir Path dir)
			throws IOException {
		// magic then a claimed private key of 100 bytes (cap is 64)
		byte[] bytes = new byte[MAGIC.length + 2];
		System.arraycopy(MAGIC, 0, bytes, 0, MAGIC.length);
		bytes[MAGIC.length] = 0;
		bytes[MAGIC.length + 1] = 100;
		loadFails(dir, bytes, "the cap is");
	} // end of aFieldOverItsCapIsRejected method

	@Test
	void aFieldTruncatedBelowItsClaimedLengthIsRejected(@TempDir Path dir)
			throws IOException {
		// magic, claim a 10-byte private key, then supply only 3 bytes
		byte[] bytes = new byte[MAGIC.length + 2 + 3];
		System.arraycopy(MAGIC, 0, bytes, 0, MAGIC.length);
		bytes[MAGIC.length] = 0;
		bytes[MAGIC.length + 1] = 10;
		loadFails(dir, bytes, "ends inside the private key");
	} // end of aFieldTruncatedBelowItsClaimedLengthIsRejected method

	@Test
	void trailingBytesAfterAValidRecordAreRejected(@TempDir Path dir)
			throws IOException {
		byte[] valid = validRecord(dir, "trailer");
		byte[] extended = Arrays.copyOf(valid, valid.length + 1);
		loadFails(dir, extended, "trailing bytes");
	} // end of trailingBytesAfterAValidRecordAreRejected method

	@Test
	void aValidRecordWithACorruptNameIsRejected(@TempDir Path dir)
			throws IOException {
		byte[] record = validRecord(dir, "x");
		// the display name is the final field; "x" is the last byte -
		// overwrite it with a control character validation must reject
		record[record.length - 1] = 0x01;
		loadFails(dir, record, "invalid display name");
	} // end of aValidRecordWithACorruptNameIsRejected method

	/**
	 * Produces the on-disk bytes of a genuine identity with the given
	 * name.
	 * @param dir the temp directory
	 * @param name the display name to bind
	 * @return the persisted identity record
	 * @throws IOException if persistence fails
	 */
	private static byte[] validRecord(Path dir, String name)
			throws IOException {
		Path seed = dir.resolve("seed-" + name);
		IdentityKey.generate(name).store(seed);
		return Files.readAllBytes(seed);
	} // end of validRecord method

	// ------------------------------------------------------------------
	// display-name sanitising and validation
	// ------------------------------------------------------------------

	@Test
	void sanitiseStripsControlCharactersAndTrims() {
		assertEquals("ab", IdentityKey.sanitizeName("ab"));
		assertEquals("alex", IdentityKey.sanitizeName("  alex  "));
	} // end of sanitiseStripsControlCharactersAndTrims method

	@Test
	void sanitiseTruncatesAnOverlongName() {
		String truncated = IdentityKey.sanitizeName("x".repeat(200));
		assertEquals(IdentityKey.MAX_NAME_CHARS, truncated.length(),
				"a long login name is clipped to the char limit");
	} // end of sanitiseTruncatesAnOverlongName method

	@Test
	void sanitiseHonoursTheByteBudgetForMultibyteNames() {
		// 40 three-byte characters is 120 bytes, over the 96-byte budget
		String result = IdentityKey.sanitizeName("€".repeat(40));
		assertTrue(result.getBytes(StandardCharsets.UTF_8).length
				<= IdentityKey.MAX_NAME_BYTES,
				"the sanitised name must fit the UTF-8 budget");
	} // end of sanitiseHonoursTheByteBudgetForMultibyteNames method

	@Test
	void aMultibyteNameOverTheByteBudgetIsRejected() {
		// 40 euro signs: 40 chars (within the 48 char limit) but 120
		// UTF-8 bytes (over the 96 byte limit)
		assertThrows(IllegalArgumentException.class,
				() -> IdentityKey.generate("€".repeat(40)));
	} // end of aMultibyteNameOverTheByteBudgetIsRejected method

	// ------------------------------------------------------------------
	// accessors
	// ------------------------------------------------------------------

	@Test
	void encodedPublicKeyAndPathAccessorsWork() {
		IdentityKey identity = IdentityKey.generate("dave");
		assertNotNull(identity.publicKeyEncoded());
		assertTrue(identity.publicKeyEncoded().length > 0);
		assertEquals(identity.fingerprint(),
				IdentityKey.fingerprintOf(identity.publicKeyEncoded()));
		assertNotNull(IdentityKey.defaultFile());
		assertTrue(IdentityKey.defaultFile().toString()
				.contains("collab-identity"));
	} // end of encodedPublicKeyAndPathAccessorsWork method
}
