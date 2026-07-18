package jls.collab.net;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The JDK-primitive crypto operations shared by {@link Handshake} and
 * {@link SecureLink} (issue #168): HKDF-SHA256 key derivation (JEP 478
 * {@code javax.crypto.KDF}), AES-256-GCM AEAD with counter
 * nonces, HMAC-SHA256, and SHA-256. Everything here is a thin,
 * deterministic wrapper - no algorithm choices are made outside this
 * file, and no crypto is hand-rolled: the one construction assembled
 * here (a 96-bit nonce as four zero bytes plus a big-endian 64-bit
 * counter) is the TLS 1.3 / Noise sequence-number nonce.
 *
 * Algorithm availability is a JLS platform guarantee (JDK 25 ships all
 * four), so a missing algorithm is an {@link IllegalStateException},
 * not a checked failure.
 */
final class Crypto {

	/** The AEAD authentication tag length, in bytes. */
	static final int TAG_BYTES = 16;

	/** The AEAD key length, in bytes. */
	static final int KEY_BYTES = 32;

	/** The GCM authentication-tag length in bits, from TAG_BYTES. */
	private static final int TAG_BITS = TAG_BYTES * 8;

	private Crypto() {

	} // end of constructor

	/**
	 * Derive key material with HKDF-SHA256.
	 *
	 * @param ikm The input key material (the DH shared secret).
	 * @param salt The extract salt (a fixed protocol label).
	 * @param label The expand label naming the derived key's purpose.
	 * @param transcriptHash The transcript hash bound into the
	 *            derivation (appended to the label as expand info).
	 * @param length How many bytes to derive.
	 *
	 * @return the derived key material.
	 */
	static byte[] hkdf(byte[] ikm, String label, byte[] salt,
			byte[] transcriptHash, int length) {

		byte[] labelBytes = label.getBytes(StandardCharsets.US_ASCII);
		byte[] info = new byte[labelBytes.length
				+ transcriptHash.length];
		System.arraycopy(labelBytes, 0, info, 0, labelBytes.length);
		System.arraycopy(transcriptHash, 0, info, labelBytes.length,
				transcriptHash.length);
		try {
			KDF hkdf = KDF.getInstance("HKDF-SHA256");
			HKDFParameterSpec spec = HKDFParameterSpec.ofExtract()
					.addIKM(ikm)
					.addSalt(salt)
					.thenExpand(info, length);
			return hkdf.deriveData(spec);
		} catch (NoSuchAlgorithmException
				| InvalidAlgorithmParameterException missing) {
			throw new IllegalStateException(
					"this JVM lacks HKDF-SHA256 support", missing);
		}
	} // end of hkdf method

	/**
	 * Encrypt and authenticate with AES-256-GCM.
	 *
	 * @param key The 32-byte AEAD key.
	 * @param counter The nonce counter (must never repeat per key).
	 * @param aad The additional authenticated data.
	 * @param plaintext The bytes to protect.
	 *
	 * @return the ciphertext with its 16-byte tag appended.
	 */
	static byte[] aeadSeal(byte[] key, long counter, byte[] aad,
			byte[] plaintext) {

		try {
			Cipher aead = Cipher.getInstance("AES/GCM/NoPadding");
			aead.init(Cipher.ENCRYPT_MODE,
					new SecretKeySpec(key, "AES"),
					new GCMParameterSpec(TAG_BITS, nonce(counter)));
			aead.updateAAD(aad);
			return aead.doFinal(plaintext);
		} catch (NoSuchAlgorithmException
				| NoSuchPaddingException missing) {
			throw new IllegalStateException(
					"this JVM lacks AES-GCM support", missing);
		} catch (InvalidKeyException
				| InvalidAlgorithmParameterException
				| IllegalBlockSizeException
				| BadPaddingException impossible) {
			// fixed-size keys and nonces from this file; encryption
			// with them cannot fail structurally
			throw new IllegalStateException(
					"AEAD sealing failed", impossible);
		}
	} // end of aeadSeal method

	/**
	 * Decrypt and verify AES-256-GCM ciphertext.
	 *
	 * @param key The 32-byte AEAD key.
	 * @param counter The expected nonce counter.
	 * @param aad The additional authenticated data.
	 * @param ciphertext The ciphertext with its tag appended.
	 *
	 * @return the plaintext.
	 *
	 * @throws AEADBadTagException if authentication fails - a
	 *             tampered, replayed, reordered, or garbage frame.
	 */
	static byte[] aeadOpen(byte[] key, long counter, byte[] aad,
			byte[] ciphertext) throws AEADBadTagException {

		try {
			Cipher aead = Cipher.getInstance("AES/GCM/NoPadding");
			aead.init(Cipher.DECRYPT_MODE,
					new SecretKeySpec(key, "AES"),
					new GCMParameterSpec(TAG_BITS, nonce(counter)));
			aead.updateAAD(aad);
			return aead.doFinal(ciphertext);
		} catch (AEADBadTagException bad) {
			throw bad;
		} catch (NoSuchAlgorithmException
				| NoSuchPaddingException missing) {
			throw new IllegalStateException(
					"this JVM lacks AES-GCM support", missing);
		} catch (InvalidKeyException
				| InvalidAlgorithmParameterException
				| IllegalBlockSizeException
				| BadPaddingException structural) {
			// GCM is a counter-mode AEAD: the only decryption failure
			// it reports is the bad tag caught above; anything else
			// means this file misused the API
			throw new IllegalStateException(
					"AEAD opening failed", structural);
		}
	} // end of aeadOpen method

	/**
	 * Compute HMAC-SHA256.
	 *
	 * @param key The MAC key.
	 * @param data The bytes to authenticate.
	 *
	 * @return the 32-byte MAC.
	 */
	static byte[] hmac(byte[] key, byte[] data) {

		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(data);
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks HMAC-SHA256 support", missing);
		} catch (InvalidKeyException impossible) {
			throw new IllegalStateException(
					"HMAC keying failed", impossible);
		}
	} // end of hmac method

	/**
	 * Compute SHA-256.
	 *
	 * @param data The bytes to hash.
	 *
	 * @return the 32-byte digest.
	 */
	static byte[] sha256(byte[] data) {

		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks SHA-256 support", missing);
		}
	} // end of sha256 method

	/**
	 * Render bytes as lowercase hex.
	 *
	 * @param data The bytes.
	 *
	 * @return two hex digits per byte.
	 */
	static String hex(byte[] data) {

		StringBuilder hex = new StringBuilder(data.length * 2);
		for (byte b : data) {
			hex.append(Character.forDigit((b >> 4) & 0xf, 16));
			hex.append(Character.forDigit(b & 0xf, 16));
		}
		return hex.toString();
	} // end of hex method

	/**
	 * Build the 96-bit AEAD nonce for a counter: four zero bytes
	 * followed by the counter, big-endian - the TLS 1.3 / Noise
	 * sequence-number nonce. Counters never repeat for a given key
	 * (handshake keys are used once; {@link SecureLink} increments
	 * per frame), so nonce reuse cannot occur.
	 *
	 * @param counter The frame counter.
	 *
	 * @return the 12-byte nonce.
	 */
	private static byte[] nonce(long counter) {

		ByteBuffer nonce = ByteBuffer.allocate(12);
		nonce.putInt(0);
		nonce.putLong(counter);
		return nonce.array();
	} // end of nonce method

} // end of Crypto class
