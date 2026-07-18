package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The identity key's contract (issue #168, research doc section 5.1):
 * generated once and persisted with owner-only permissions, loaded
 * back byte-identical across restarts, fingerprinted as the peer id,
 * and strict about malformed or inconsistent files - a broken identity
 * file is an error, never a silent regeneration (regenerating would
 * trip every peer's key-change warning).
 */
class IdentityKeyTest {

	@Test
	void firstUseCreatesAndPersistsAnIdentity(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("jls").resolve("collab-identity");
		IdentityKey first = IdentityKey.loadOrCreate(file);
		assertTrue(Files.isRegularFile(file),
				"the identity must persist on first use");
		IdentityKey second = IdentityKey.loadOrCreate(file);
		assertEquals(first.fingerprint(), second.fingerprint(),
				"a reload must produce the same identity");
		assertEquals(first.displayName(), second.displayName());
	}

	@Test
	void theIdentityFileIsOwnerOnly(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("collab-identity");
		if (Files.getFileAttributeView(config,
				PosixFileAttributeView.class) == null) {
			return; // permissions are POSIX-only; nothing to assert
		}
		IdentityKey.loadOrCreate(file);
		Set<PosixFilePermission> permissions =
				Files.getPosixFilePermissions(file);
		assertEquals(Set.of(PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE), permissions,
				"the private key file must be mode 600");
	}

	@Test
	void fingerprintsAreStableHexAndGroupable() {
		IdentityKey identity = IdentityKey.generate("carol");
		assertTrue(identity.fingerprint().matches("[0-9a-f]{64}"),
				"a fingerprint is 64 lowercase hex characters");
		String display = identity.displayFingerprint();
		assertEquals(16, display.split(" ").length,
				"the display form groups the fingerprint by four");
		assertEquals(identity.fingerprint(),
				display.replace(" ", ""));
	}

	@Test
	void signaturesVerifyAndTamperedDataDoesNot() {
		IdentityKey identity = IdentityKey.generate("carol");
		byte[] data = "signed bytes".getBytes();
		byte[] signature = identity.sign(data);
		assertTrue(IdentityKey.verify(identity.publicKey(), data,
				signature));
		data[0] ^= 1;
		assertFalse(IdentityKey.verify(identity.publicKey(), data,
				signature));
	}

	@Test
	void garbageIdentityFilesAreErrors(@TempDir Path config)
			throws IOException {
		Path file = config.resolve("collab-identity");
		Files.write(file, "not an identity file".getBytes());
		assertThrows(IOException.class,
				() -> IdentityKey.loadOrCreate(file));
	}

	@Test
	void aMismatchedKeyPairIsAnError(@TempDir Path config)
			throws IOException {
		Path mine = config.resolve("mine");
		Path other = config.resolve("other");
		IdentityKey.loadOrCreate(mine);
		IdentityKey.loadOrCreate(other);

		// splice the other identity's public key into this file
		byte[] record = Files.readAllBytes(mine);
		byte[] donor = Files.readAllBytes(other);
		ByteBuffer reader = ByteBuffer.wrap(record).position(6);
		int privateLength = reader.getShort() & 0xffff;
		int publicAt = 6 + 2 + privateLength + 2;
		ByteBuffer donorReader = ByteBuffer.wrap(donor).position(6);
		int donorPrivateLength = donorReader.getShort() & 0xffff;
		int donorPublicAt = 6 + 2 + donorPrivateLength + 2;
		int publicLength = ByteBuffer.wrap(record)
				.position(publicAt - 2).getShort() & 0xffff;
		System.arraycopy(donor, donorPublicAt, record, publicAt,
				publicLength);
		Files.write(mine, record);

		assertThrows(IOException.class,
				() -> IdentityKey.loadOrCreate(mine),
				"a private key that does not match its public key"
						+ " must be an error");
	}

	@Test
	void displayNameRulesAreEnforced() {
		assertThrows(IllegalArgumentException.class,
				() -> IdentityKey.generate(""));
		assertThrows(IllegalArgumentException.class,
				() -> IdentityKey.generate("a\nb"));
		assertThrows(IllegalArgumentException.class,
				() -> IdentityKey.generate("x".repeat(49)));
		assertEquals("anonymous", IdentityKey.sanitizeName(null));
		assertEquals("anonymous", IdentityKey.sanitizeName(""));
		assertEquals("alex", IdentityKey.sanitizeName(" alex "));
	}

} // end of IdentityKeyTest class
