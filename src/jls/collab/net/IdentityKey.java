package jls.collab.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Set;

/**
 * This install's long-term collaboration identity (issue #168, research
 * doc section 5.1): an Ed25519 signing keypair generated on first
 * collaborative use and persisted in the per-user config area with
 * owner-only permissions, plus the user-chosen display name bound to
 * it. The peer id is the public-key fingerprint - names are labels,
 * keys are identity, so two peers named "Alex" never collide.
 *
 * The file lives at {@code jls/collab-identity} under the XDG config
 * base, next to the #183 replica id, and holds the private key
 * (PKCS#8), the public key (X.509), and the display name in one small
 * binary record. A malformed or inconsistent file is an error, never a
 * silent regeneration: regenerating would change this install's
 * identity behind the user's back and trip every peer's key-change
 * warning (research doc section 5.2 step 4).
 */
public final class IdentityKey {

	/** The magic bytes opening the identity file. */
	private static final byte[] MAGIC =
			"JLSID1".getBytes(StandardCharsets.US_ASCII);

	/** The most bytes a UTF-8 display name may occupy. */
	public static final int MAX_NAME_BYTES = 96;

	/** The most characters a display name may have. */
	public static final int MAX_NAME_CHARS = 48;

	/** The most bytes an identity file may hold (hostile-input cap). */
	private static final int MAX_FILE_BYTES = 4096;

	/** The most bytes a DER-encoded Ed25519 key may occupy here. */
	static final int MAX_KEY_BYTES = 64;

	/** The long-term Ed25519 keypair. */
	private final KeyPair pair;

	/** The user-chosen display name bound to the key. */
	private final String displayName;

	/** The fingerprint, computed once: SHA-256 of the public key. */
	private final String fingerprint;

	/**
	 * Bind a keypair and display name into an identity. Private:
	 * callers arrive through {@link #loadOrCreate(Path)} or the
	 * package-private {@link #generate(String)}.
	 *
	 * @param pair The Ed25519 keypair.
	 * @param displayName The validated display name.
	 */
	private IdentityKey(KeyPair pair, String displayName) {

		this.pair = pair;
		this.displayName = displayName;
		this.fingerprint =
				fingerprintOf(pair.getPublic().getEncoded());
	} // end of constructor

	/**
	 * Where this install persists its collaboration identity: {@code
	 * jls/collab-identity} under the XDG config base ({@code
	 * $XDG_CONFIG_HOME}, or {@code ~/.config} when unset - the same
	 * convention the #183 replica id follows).
	 *
	 * @return the per-install identity file path.
	 */
	public static Path defaultFile() {

		String xdg = System.getenv("XDG_CONFIG_HOME");
		Path base = xdg == null || xdg.isEmpty()
				? Path.of(System.getProperty("user.home"), ".config")
				: Path.of(xdg);
		return base.resolve("jls").resolve("collab-identity");
	} // end of defaultFile method

	/**
	 * Load the identity persisted at the given path, or - only when no
	 * file exists there - generate a fresh one, persist it with
	 * owner-only permissions, and return it. The default display name
	 * for a fresh identity is the login name ({@code user.name}),
	 * sanitized to the display-name rules.
	 *
	 * @param file The identity file (normally {@link #defaultFile()}).
	 *
	 * @return the identity.
	 *
	 * @throws IOException if the file exists but is malformed or its
	 *             keys are inconsistent, or if reading/writing fails.
	 */
	public static IdentityKey loadOrCreate(Path file) throws IOException {

		if (Files.exists(file)) {
			return load(file);
		}
		IdentityKey fresh = generate(
				sanitizeName(System.getProperty("user.name")));
		fresh.store(file);
		return fresh;
	} // end of loadOrCreate method

	/**
	 * Generate a fresh identity in memory, without touching disk. The
	 * loopback test harness uses this to run two identities in one
	 * process; {@link #loadOrCreate(Path)} uses it on first run.
	 *
	 * @param displayName The display name to bind.
	 *
	 * @return the fresh identity.
	 *
	 * @throws IllegalArgumentException if the name breaks the
	 *             display-name rules.
	 */
	static IdentityKey generate(String displayName) {

		validateDisplayName(displayName);
		try {
			KeyPairGenerator generator =
					KeyPairGenerator.getInstance("Ed25519");
			return new IdentityKey(generator.generateKeyPair(),
					displayName);
		} catch (NoSuchAlgorithmException missing) {
			// Ed25519 has shipped in every JDK since 15; JLS requires 25
			throw new IllegalStateException(
					"this JVM lacks Ed25519 support", missing);
		}
	} // end of generate method

	/**
	 * Persist this identity at the given path, creating parent
	 * directories as needed. On POSIX filesystems the file is created
	 * with owner-only permissions (mode 600) before a single key byte
	 * is written; elsewhere the platform default applies (research doc
	 * section 6.4 records key storage as config-dir-with-restrictive-
	 * permissions).
	 *
	 * @param file The identity file path.
	 *
	 * @throws IOException if writing fails.
	 */
	void store(Path file) throws IOException {

		byte[] priv = pair.getPrivate().getEncoded();
		byte[] pub = pair.getPublic().getEncoded();
		byte[] name = displayName.getBytes(StandardCharsets.UTF_8);
		ByteBuffer record = ByteBuffer.allocate(MAGIC.length + 6
				+ priv.length + pub.length + name.length);
		record.put(MAGIC);
		record.putShort((short) priv.length);
		record.put(priv);
		record.putShort((short) pub.length);
		record.put(pub);
		record.putShort((short) name.length);
		record.put(name);
		if (file.getParent() != null) {
			Files.createDirectories(file.getParent());
		}
		if (Files.getFileAttributeView(file.getParent() == null
				? file.toAbsolutePath().getParent() : file.getParent(),
				java.nio.file.attribute.PosixFileAttributeView.class)
				!= null) {
			Set<PosixFilePermission> ownerOnly = PosixFilePermissions
					.fromString("rw-------");
			FileAttribute<Set<PosixFilePermission>> attribute =
					PosixFilePermissions.asFileAttribute(ownerOnly);
			Files.deleteIfExists(file);
			Files.createFile(file, attribute);
		}
		Files.write(file, record.array());
	} // end of store method

	/**
	 * Load and validate the identity file: magic, structure, key
	 * parsing, and a sign/verify probe proving the private and public
	 * halves belong together.
	 *
	 * @param file The identity file path.
	 *
	 * @return the identity.
	 *
	 * @throws IOException if the file is malformed or inconsistent.
	 */
	private static IdentityKey load(Path file) throws IOException {

		if (Files.size(file) > MAX_FILE_BYTES) {
			throw new IOException("identity file " + file + " exceeds "
					+ MAX_FILE_BYTES + " bytes");
		}
		ByteBuffer record = ByteBuffer.wrap(Files.readAllBytes(file));
		byte[] magic = new byte[MAGIC.length];
		if (record.remaining() < MAGIC.length) {
			throw new IOException("identity file " + file
					+ " is too short to be an identity file");
		}
		record.get(magic);
		if (!MessageDigest.isEqual(magic, MAGIC)) {
			throw new IOException("identity file " + file
					+ " does not start with the JLSID1 magic");
		}
		byte[] priv = lengthPrefixed(record, MAX_KEY_BYTES,
				"private key", file);
		byte[] pub = lengthPrefixed(record, MAX_KEY_BYTES,
				"public key", file);
		byte[] name = lengthPrefixed(record, MAX_NAME_BYTES,
				"display name", file);
		if (record.hasRemaining()) {
			throw new IOException("identity file " + file + " has "
					+ record.remaining() + " trailing bytes");
		}
		String displayName = new String(name, StandardCharsets.UTF_8);
		KeyPair pair;
		try {
			KeyFactory factory = KeyFactory.getInstance("Ed25519");
			PrivateKey privateKey = factory.generatePrivate(
					new PKCS8EncodedKeySpec(priv));
			PublicKey publicKey = factory.generatePublic(
					new X509EncodedKeySpec(pub));
			pair = new KeyPair(publicKey, privateKey);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException bad) {
			throw new IOException("identity file " + file
					+ " holds keys this JVM cannot parse: "
					+ bad.getMessage(), bad);
		}
		try {
			validateDisplayName(displayName);
		} catch (IllegalArgumentException bad) {
			throw new IOException("identity file " + file
					+ " holds an invalid display name: "
					+ bad.getMessage(), bad);
		}
		IdentityKey identity = new IdentityKey(pair, displayName);
		byte[] probe = new byte[32];
		if (!verify(pair.getPublic(), probe, identity.sign(probe))) {
			throw new IOException("identity file " + file + " holds a"
					+ " private key that does not match its public key");
		}
		return identity;
	} // end of load method

	/**
	 * Read one length-prefixed field from the identity record.
	 *
	 * @param record The record buffer, positioned at a 2-byte length.
	 * @param cap The most bytes the field may hold.
	 * @param what The field name, for the message.
	 * @param file The file path, for the message.
	 *
	 * @return the field bytes.
	 *
	 * @throws IOException if the field is missing, truncated, or over
	 *             its cap.
	 */
	private static byte[] lengthPrefixed(ByteBuffer record, int cap,
			String what, Path file) throws IOException {

		if (record.remaining() < 2) {
			throw new IOException("identity file " + file
					+ " ends before the " + what + " length");
		}
		int length = record.getShort() & 0xffff;
		if (length > cap) {
			throw new IOException("identity file " + file + " claims a "
					+ what + " of " + length + " bytes; the cap is "
					+ cap);
		}
		if (record.remaining() < length) {
			throw new IOException("identity file " + file
					+ " ends inside the " + what);
		}
		byte[] field = new byte[length];
		record.get(field);
		return field;
	} // end of lengthPrefixed method

	/**
	 * The peer id: lowercase hex SHA-256 of the DER-encoded public key.
	 *
	 * @return the 64-character fingerprint.
	 */
	public String fingerprint() {

		return fingerprint;
	} // end of fingerprint method

	/**
	 * A fingerprint formatted for human reading: sixteen groups of four
	 * hex digits separated by spaces.
	 *
	 * @return the display form of {@link #fingerprint()}.
	 */
	public String displayFingerprint() {

		return groupFingerprint(fingerprint);
	} // end of displayFingerprint method

	/**
	 * The display name bound to this identity.
	 *
	 * @return the name (a label only; the fingerprint is the identity).
	 */
	public String displayName() {

		return displayName;
	} // end of displayName method

	/**
	 * This identity's public key.
	 *
	 * @return the Ed25519 public key.
	 */
	public PublicKey publicKey() {

		return pair.getPublic();
	} // end of publicKey method

	/**
	 * The DER (X.509 SubjectPublicKeyInfo) encoding of the public key,
	 * as sent inside handshake authentication messages.
	 *
	 * @return a fresh copy of the encoded key.
	 */
	public byte[] publicKeyEncoded() {

		return pair.getPublic().getEncoded();
	} // end of publicKeyEncoded method

	/**
	 * Sign data with the long-term private key.
	 *
	 * @param data The bytes to sign.
	 *
	 * @return the 64-byte Ed25519 signature.
	 */
	public byte[] sign(byte[] data) {

		try {
			Signature signer = Signature.getInstance("Ed25519");
			signer.initSign(pair.getPrivate());
			signer.update(data);
			return signer.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException
				| SignatureException impossible) {
			// the key was generated or validated by this class
			throw new IllegalStateException(
					"Ed25519 signing failed", impossible);
		}
	} // end of sign method

	/**
	 * Verify an Ed25519 signature.
	 *
	 * @param key The claimed signer's public key.
	 * @param data The bytes that were signed.
	 * @param signature The signature to check.
	 *
	 * @return true if the signature is valid; false for a bad
	 *         signature or a structurally unusable one.
	 */
	public static boolean verify(PublicKey key, byte[] data,
			byte[] signature) {

		try {
			Signature verifier = Signature.getInstance("Ed25519");
			verifier.initVerify(key);
			verifier.update(data);
			return verifier.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException
				| SignatureException bad) {
			return false;
		}
	} // end of verify method

	/**
	 * The fingerprint of any DER-encoded public key: lowercase hex
	 * SHA-256. Handshake code uses this to identify the peer.
	 *
	 * @param encodedPublicKey The X.509 DER encoding.
	 *
	 * @return the 64-character fingerprint.
	 */
	static String fingerprintOf(byte[] encodedPublicKey) {

		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(encodedPublicKey);
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(Character.forDigit((b >> 4) & 0xf, 16));
				hex.append(Character.forDigit(b & 0xf, 16));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks SHA-256 support", missing);
		}
	} // end of fingerprintOf method

	/**
	 * Format a 64-character fingerprint for human reading.
	 *
	 * @param fingerprint The plain hex fingerprint.
	 *
	 * @return sixteen groups of four separated by spaces.
	 */
	static String groupFingerprint(String fingerprint) {

		StringBuilder grouped = new StringBuilder(fingerprint.length()
				+ fingerprint.length() / 4);
		for (int i = 0; i < fingerprint.length(); i += 4) {
			if (i > 0) {
				grouped.append(' ');
			}
			grouped.append(fingerprint, i,
					Math.min(i + 4, fingerprint.length()));
		}
		return grouped.toString();
	} // end of groupFingerprint method

	/**
	 * Enforce the display-name rules shared by identity files and
	 * handshake authentication messages: 1-{@value #MAX_NAME_CHARS}
	 * characters, at most {@value #MAX_NAME_BYTES} UTF-8 bytes, no ISO
	 * control characters (a name is a label shown in dialogs and the
	 * peer panel, so it must not smuggle terminal escapes or
	 * newlines).
	 *
	 * @param name The candidate name.
	 *
	 * @throws IllegalArgumentException if the name breaks a rule.
	 */
	static void validateDisplayName(String name) {

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException(
					"a display name cannot be empty");
		}
		if (name.length() > MAX_NAME_CHARS) {
			throw new IllegalArgumentException("a display name cannot"
					+ " exceed " + MAX_NAME_CHARS + " characters");
		}
		if (name.getBytes(StandardCharsets.UTF_8).length
				> MAX_NAME_BYTES) {
			throw new IllegalArgumentException("a display name cannot"
					+ " exceed " + MAX_NAME_BYTES + " UTF-8 bytes");
		}
		for (int i = 0; i < name.length(); i += 1) {
			if (Character.isISOControl(name.charAt(i))) {
				throw new IllegalArgumentException("a display name"
						+ " cannot contain control characters");
			}
		}
	} // end of validateDisplayName method

	/**
	 * Turn a raw login name into a legal default display name,
	 * falling back to {@code "anonymous"} when the raw value is
	 * missing or unsalvageable.
	 *
	 * @param raw The {@code user.name} system property, or null.
	 *
	 * @return a name that passes {@link #validateDisplayName(String)}.
	 */
	static String sanitizeName(String raw) {

		if (raw == null) {
			return "anonymous";
		}
		StringBuilder cleaned = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length()
				&& cleaned.length() < MAX_NAME_CHARS; i += 1) {
			char ch = raw.charAt(i);
			if (!Character.isISOControl(ch)) {
				cleaned.append(ch);
			}
		}
		String candidate = cleaned.toString().strip();
		while (candidate.getBytes(StandardCharsets.UTF_8).length
				> MAX_NAME_BYTES) {
			candidate = candidate.substring(0, candidate.length() - 1);
		}
		return candidate.isBlank() ? "anonymous" : candidate;
	} // end of sanitizeName method

} // end of IdentityKey class
