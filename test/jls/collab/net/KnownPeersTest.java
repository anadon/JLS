package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The trust store's contract (issue #168, prediction P4 at the model
 * layer, per the #91 layering): a fingerprint the user SAS-verified
 * earlier classifies as VERIFIED (the UI skips the verify dialog), a
 * never-seen key is UNKNOWN (run the SAS flow), and an unknown key
 * claiming a display name already bound to a different key is
 * KEY_CHANGED (the loud-warning dialog path). Trust data is strict on
 * disk: malformed stores are errors, never repairs.
 */
class KnownPeersTest {

	/** A fingerprint-shaped constant for peer one. */
	private static final String KEY_A = "a".repeat(64);

	/** A fingerprint-shaped constant for peer two. */
	private static final String KEY_B = "b".repeat(64);

	@Test
	void verifiedPeersSkipVerificationAcrossRestart(
			@TempDir Path config) throws IOException {
		Path file = config.resolve("known-peers");
		KnownPeers store = KnownPeers.load(file);
		assertEquals(KnownPeers.Trust.UNKNOWN,
				store.check(KEY_A, "alex"),
				"a never-seen peer runs the SAS flow");
		store.recordVerified(KEY_A, "alex", 1234L);
		assertEquals(KnownPeers.Trust.VERIFIED,
				store.check(KEY_A, "alex"));

		KnownPeers reloaded = KnownPeers.load(file);
		assertEquals(KnownPeers.Trust.VERIFIED,
				reloaded.check(KEY_A, "alex"),
				"verification must survive a restart");
		assertEquals("alex", reloaded.nameOf(KEY_A));
	}

	@Test
	void aChangedKeyForAKnownNameIsLoud(@TempDir Path config)
			throws IOException {
		KnownPeers store = KnownPeers.load(
				config.resolve("known-peers"));
		store.recordVerified(KEY_A, "alex", 1234L);
		assertEquals(KnownPeers.Trust.KEY_CHANGED,
				store.check(KEY_B, "alex"),
				"an unknown key under a verified name is the"
						+ " key-change warning path");
		assertEquals(KnownPeers.Trust.UNKNOWN,
				store.check(KEY_B, "sam"),
				"an unknown key under an unknown name is just new");
	}

	@Test
	void verifiedKeysStayVerifiedUnderAnyClaimedName(
			@TempDir Path config) throws IOException {
		KnownPeers store = KnownPeers.load(
				config.resolve("known-peers"));
		store.recordVerified(KEY_A, "alex", 1234L);
		assertEquals(KnownPeers.Trust.VERIFIED,
				store.check(KEY_A, "renamed alex"),
				"the key is the identity; the name is a label");
	}

	@Test
	void awkwardNamesRoundTripThroughTheStore(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("known-peers");
		KnownPeers store = KnownPeers.load(file);
		String awkward = "quote \" and \\ backslash";
		store.recordVerified(KEY_A, awkward, 99L);
		assertEquals(awkward, KnownPeers.load(file).nameOf(KEY_A));
	}

	@Test
	void malformedStoresAreErrors(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("known-peers");
		Files.writeString(file, "not the header\n",
				StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> KnownPeers.load(file));

		Files.writeString(file, "JLS known peers v1\nnonsense line\n",
				StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> KnownPeers.load(file));

		Files.writeString(file, "JLS known peers v1\n" + KEY_A
				+ " notanumber \"alex\"\n", StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> KnownPeers.load(file));

		Files.writeString(file, "JLS known peers v1\n" + KEY_A
				+ " 5 \"alex\"\n" + KEY_A + " 6 \"sam\"\n",
				StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> KnownPeers.load(file),
				"a repeated fingerprint is a malformed store");
	}

	@Test
	void recordInputsAreValidated(@TempDir Path config)
			throws IOException {
		KnownPeers store = KnownPeers.load(
				config.resolve("known-peers"));
		assertThrows(IllegalArgumentException.class,
				() -> store.recordVerified("short", "alex", 1L));
		assertThrows(IllegalArgumentException.class,
				() -> store.recordVerified(KEY_A, "a\nb", 1L));
	}

} // end of KnownPeersTest class
