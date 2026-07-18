package jls.collab.net;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.AEADBadTagException;
import javax.crypto.KeyAgreement;

/**
 * The mutually authenticated key exchange between two JLS installs
 * (issue #168, research doc section 5.2 step 2): a three-message,
 * TLS-1.3-with-raw-public-keys-shaped handshake (the SIGMA
 * sign-and-mac construction TLS 1.3 instantiates) built entirely from
 * JDK primitives - X25519 ephemerals, Ed25519 identity signatures,
 * HKDF-SHA256, ChaCha20-Poly1305 - with no certificates and no new
 * dependency. This class is transport-agnostic: it turns received
 * message bytes into reply bytes, and the caller moves them (a socket
 * later; byte arrays in the loopback tests now).
 *
 * <p>Protocol, version 1. The joiner initiates; the session starter
 * responds. A running transcript accumulates every field both sides
 * see, exactly once, in order; {@code th} below is SHA-256 of the
 * transcript so far.</p>
 *
 * <pre>
 * m1  initiator - "JLSCOLLAB1" magic, version, 32 random bytes,
 *     ephemeral X25519 public key (X.509 DER, length-prefixed).
 * m2  responder - version, 16-byte session id, 32 random bytes,
 *     ephemeral X25519 public key; then, encrypted under the
 *     responder handshake key: the responder's Ed25519 identity key,
 *     display name, signature over "verify responder" + th, and a
 *     finished MAC over th.
 * m3  initiator - encrypted under the initiator handshake key: the
 *     initiator's identity key, display name, signature over
 *     "verify initiator" + th, and a finished MAC over th.
 * </pre>
 *
 * <p>Key schedule: the X25519 shared secret is the HKDF input; every
 * derivation binds the transcript hash into its expand info, so any
 * tamper anywhere diverges every later key. Handshake encryption keys
 * and finished-MAC keys derive from the post-m2-clear hash; the two
 * per-direction application keys and the 8-byte SAS secret derive
 * from the final hash - which covers both identity keys, both
 * ephemerals, both randoms, and the session id, giving the SAS the
 * binding research doc section 5.2 step 3 requires. Each handshake
 * key encrypts exactly one message, with a zero nonce.</p>
 *
 * <p>Both signatures cover the full transcript to that point
 * (including the peer's ephemeral and the signer's own identity key),
 * which is the TLS 1.3 CertificateVerify shape; the finished MACs
 * prove key confirmation. Completion criterion #163/#168: this
 * construction must be reviewed against its published pattern by a
 * second reader before merge.</p>
 *
 * <p>Every parse failure, cap violation, bad tag, bad signature, or
 * bad MAC is a {@link HandshakeRejected} and permanently fails the
 * handshake - rejection, never repair (#38 discipline).</p>
 */
public final class Handshake {

	/** The protocol magic opening every session (m1). */
	private static final byte[] MAGIC =
			"JLSCOLLAB1".getBytes(StandardCharsets.US_ASCII);

	/** The protocol version this implementation speaks. */
	private static final byte VERSION = 1;

	/** The HKDF extract salt for this protocol version. */
	private static final byte[] SALT =
			"JLSCOLLAB1 v1".getBytes(StandardCharsets.US_ASCII);

	/** Bytes of random freshness each side contributes. */
	private static final int RANDOM_BYTES = 32;

	/** Bytes in the session id the responder mints. */
	private static final int SESSION_ID_BYTES = 16;

	/** Bytes in an Ed25519 signature. */
	private static final int SIGNATURE_BYTES = 64;

	/** Bytes in a finished MAC (HMAC-SHA256). */
	private static final int FINISHED_BYTES = 32;

	/** Hostile-input cap: the most bytes any handshake message has. */
	private static final int MAX_MESSAGE_BYTES = 1024;

	/** Hostile-input cap: the most bytes a DER public key may have. */
	private static final int MAX_KEY_BYTES = IdentityKey.MAX_KEY_BYTES;

	/**
	 * The process-wide entropy source for randoms and session ids
	 * (SecureRandom is thread-safe; one instance serves every
	 * handshake).
	 */
	private static final SecureRandom RANDOM = new SecureRandom();

	/** Where a handshake instance is in the exchange. */
	private enum State {
		/** Initiator created; m1 not yet produced. */
		INITIATOR_START,
		/** Initiator produced m1; waiting to accept m2. */
		INITIATOR_SENT_HELLO,
		/** Responder created; waiting to accept m1. */
		RESPONDER_START,
		/** Responder produced m2; waiting to accept m3. */
		RESPONDER_SENT_REPLY,
		/** Exchange complete; the link is available. */
		COMPLETE,
		/** A message was rejected; this handshake is dead. */
		FAILED
	} // end of State enum

	/** True for the joiner, false for the session starter. */
	private final boolean initiator;

	/** This side's long-term identity. */
	private final IdentityKey identity;

	/** This side's fresh X25519 ephemeral pair. */
	private final KeyPair ephemeral;

	/** The running transcript: every field both sides see, in order. */
	private final ByteArrayOutputStream transcript =
			new ByteArrayOutputStream();

	/** Where this handshake is in the exchange. */
	private State state;

	/** The session id (minted by the responder, learned in m2). */
	private byte[] sessionId;

	/** The X25519 shared secret, once both ephemerals are known. */
	private byte[] sharedSecret;

	/** The responder-direction handshake encryption key. */
	private byte[] responderHandshakeKey;

	/** The initiator-direction handshake encryption key. */
	private byte[] initiatorHandshakeKey;

	/** The responder's finished-MAC key. */
	private byte[] responderFinishedKey;

	/** The initiator's finished-MAC key. */
	private byte[] initiatorFinishedKey;

	/** The peer's long-term public key, once authenticated. */
	private PublicKey peerIdentity;

	/** The peer's display name, once authenticated. */
	private String peerName;

	/** The completed link, once the exchange finishes. */
	private SecureLink link;

	/**
	 * Create a handshake in its starting state.
	 *
	 * @param initiator True for the joiner, false for the starter.
	 * @param identity This side's long-term identity.
	 */
	private Handshake(boolean initiator, IdentityKey identity) {

		this.initiator = initiator;
		this.identity = identity;
		this.ephemeral = freshEphemeral();
		this.state = initiator ? State.INITIATOR_START
				: State.RESPONDER_START;
	} // end of constructor

	/**
	 * Create the initiator side (the peer joining a session).
	 *
	 * @param identity The joiner's long-term identity.
	 *
	 * @return a handshake ready to produce {@link #firstMessage()}.
	 */
	public static Handshake initiate(IdentityKey identity) {

		return new Handshake(true, identity);
	} // end of initiate method

	/**
	 * Create the responder side (the peer that started the session and
	 * accepted the connection).
	 *
	 * @param identity The starter's long-term identity.
	 *
	 * @return a handshake ready for {@link #acceptFirst(byte[])}.
	 */
	public static Handshake respond(IdentityKey identity) {

		return new Handshake(false, identity);
	} // end of respond method

	/**
	 * Produce m1, the initiator hello.
	 *
	 * @return the message bytes to deliver to the responder.
	 *
	 * @throws IllegalStateException if this side is not an initiator
	 *             in its starting state.
	 */
	public byte[] firstMessage() {

		requireState(State.INITIATOR_START);
		byte[] random = new byte[RANDOM_BYTES];
		RANDOM.nextBytes(random);
		byte[] ephemeralDer = ephemeral.getPublic().getEncoded();
		ByteBuffer m1 = ByteBuffer.allocate(MAGIC.length + 1
				+ RANDOM_BYTES + 2 + ephemeralDer.length);
		m1.put(MAGIC);
		m1.put(VERSION);
		m1.put(random);
		m1.putShort((short) ephemeralDer.length);
		m1.put(ephemeralDer);
		byte[] message = m1.array();
		transcript.writeBytes(message);
		state = State.INITIATOR_SENT_HELLO;
		return message;
	} // end of firstMessage method

	/**
	 * Responder: accept m1 and produce m2.
	 *
	 * @param m1 The initiator hello as received.
	 *
	 * @return the reply bytes to deliver to the initiator.
	 *
	 * @throws HandshakeRejected if m1 is not a well-formed hello.
	 * @throws IllegalStateException if this side is not a responder in
	 *             its starting state.
	 */
	public byte[] acceptFirst(byte[] m1) throws HandshakeRejected {

		requireState(State.RESPONDER_START);
		try {
			ByteBuffer hello = checkedBuffer(m1, "m1");
			byte[] magic = new byte[MAGIC.length];
			need(hello, MAGIC.length, "m1", "protocol magic");
			hello.get(magic);
			if (!MessageDigest.isEqual(magic, MAGIC)) {
				throw new HandshakeRejected(
						"m1 does not begin with the JLSCOLLAB1 magic");
			}
			need(hello, 1, "m1", "version");
			byte version = hello.get();
			if (version != VERSION) {
				throw new HandshakeRejected("m1 speaks protocol version "
						+ version + "; this JLS speaks " + VERSION);
			}
			need(hello, RANDOM_BYTES, "m1", "random");
			byte[] random = new byte[RANDOM_BYTES];
			hello.get(random);
			byte[] peerEphemeralDer = lengthPrefixed(hello,
					MAX_KEY_BYTES, "m1", "ephemeral key");
			requireEmpty(hello, "m1");
			transcript.writeBytes(m1);
			computeShared(peerEphemeralDer);

			// build m2's clear part and derive the handshake keys from
			// the transcript that includes it
			sessionId = new byte[SESSION_ID_BYTES];
			byte[] responderRandom = new byte[RANDOM_BYTES];
			RANDOM.nextBytes(sessionId);
			RANDOM.nextBytes(responderRandom);
			byte[] ephemeralDer = ephemeral.getPublic().getEncoded();
			ByteBuffer clear = ByteBuffer.allocate(1 + SESSION_ID_BYTES
					+ RANDOM_BYTES + 2 + ephemeralDer.length);
			clear.put(VERSION);
			clear.put(sessionId);
			clear.put(responderRandom);
			clear.putShort((short) ephemeralDer.length);
			clear.put(ephemeralDer);
			byte[] clearPart = clear.array();
			transcript.writeBytes(clearPart);
			deriveHandshakeKeys();

			byte[] auth = buildAuth(false);
			byte[] ciphertext = Crypto.aeadSeal(responderHandshakeKey,
					0, aad("m2"), auth);
			ByteBuffer m2 = ByteBuffer.allocate(clearPart.length + 2
					+ ciphertext.length);
			m2.put(clearPart);
			m2.putShort((short) ciphertext.length);
			m2.put(ciphertext);
			state = State.RESPONDER_SENT_REPLY;
			return m2.array();
		} catch (HandshakeRejected rejected) {
			state = State.FAILED;
			throw rejected;
		}
	} // end of acceptFirst method

	/**
	 * Initiator: accept m2, authenticate the responder, and produce
	 * m3. After this returns, {@link #link()} is available on this
	 * side.
	 *
	 * @param m2 The responder reply as received.
	 *
	 * @return the finish bytes to deliver to the responder.
	 *
	 * @throws HandshakeRejected if m2 is malformed, fails decryption,
	 *             or fails responder authentication.
	 * @throws IllegalStateException if this side is not an initiator
	 *             that has produced m1.
	 */
	public byte[] acceptReply(byte[] m2) throws HandshakeRejected {

		requireState(State.INITIATOR_SENT_HELLO);
		try {
			ByteBuffer reply = checkedBuffer(m2, "m2");
			need(reply, 1, "m2", "version");
			byte version = reply.get();
			if (version != VERSION) {
				throw new HandshakeRejected("m2 speaks protocol version "
						+ version + "; this JLS speaks " + VERSION);
			}
			need(reply, SESSION_ID_BYTES, "m2", "session id");
			sessionId = new byte[SESSION_ID_BYTES];
			reply.get(sessionId);
			need(reply, RANDOM_BYTES, "m2", "random");
			byte[] responderRandom = new byte[RANDOM_BYTES];
			reply.get(responderRandom);
			byte[] peerEphemeralDer = lengthPrefixed(reply,
					MAX_KEY_BYTES, "m2", "ephemeral key");
			int clearLength = reply.position();
			byte[] ciphertext = lengthPrefixed(reply,
					maxAuthCiphertext(), "m2", "encrypted part");
			requireEmpty(reply, "m2");
			transcript.write(m2, 0, clearLength);
			computeShared(peerEphemeralDer);
			deriveHandshakeKeys();

			byte[] auth = openAuth(responderHandshakeKey, "m2",
					ciphertext);
			verifyAuth(auth, false);

			byte[] finish = buildAuth(true);
			byte[] finishCiphertext = Crypto.aeadSeal(
					initiatorHandshakeKey, 0, aad("m3"), finish);
			ByteBuffer m3 = ByteBuffer.allocate(2
					+ finishCiphertext.length);
			m3.putShort((short) finishCiphertext.length);
			m3.put(finishCiphertext);
			completeLink();
			return m3.array();
		} catch (HandshakeRejected rejected) {
			state = State.FAILED;
			throw rejected;
		}
	} // end of acceptReply method

	/**
	 * Responder: accept m3 and authenticate the initiator. After this
	 * returns, {@link #link()} is available on this side.
	 *
	 * @param m3 The initiator finish as received.
	 *
	 * @throws HandshakeRejected if m3 is malformed, fails decryption,
	 *             or fails initiator authentication.
	 * @throws IllegalStateException if this side is not a responder
	 *             that has produced m2.
	 */
	public void acceptFinish(byte[] m3) throws HandshakeRejected {

		requireState(State.RESPONDER_SENT_REPLY);
		try {
			ByteBuffer finish = checkedBuffer(m3, "m3");
			byte[] ciphertext = lengthPrefixed(finish,
					maxAuthCiphertext(), "m3", "encrypted part");
			requireEmpty(finish, "m3");
			byte[] auth = openAuth(initiatorHandshakeKey, "m3",
					ciphertext);
			verifyAuth(auth, true);
			completeLink();
		} catch (HandshakeRejected rejected) {
			state = State.FAILED;
			throw rejected;
		}
	} // end of acceptFinish method

	/**
	 * The completed, encrypted link.
	 *
	 * @return the link carrying this session's frames.
	 *
	 * @throws IllegalStateException if the handshake has not
	 *             completed.
	 */
	public SecureLink link() {

		requireState(State.COMPLETE);
		return link;
	} // end of link method

	/**
	 * Build this side's authentication block: identity key, display
	 * name, signature over the transcript, finished MAC - each
	 * appended to the transcript as produced, mirroring
	 * {@link #verifyAuth(byte[], boolean)} on the peer.
	 *
	 * @param asInitiator Whether to sign with the initiator label.
	 *
	 * @return the authentication plaintext.
	 */
	private byte[] buildAuth(boolean asInitiator) {

		byte[] identityDer = identity.publicKeyEncoded();
		byte[] name = identity.displayName()
				.getBytes(StandardCharsets.UTF_8);
		transcript.writeBytes(identityDer);
		transcript.writeBytes(name);
		byte[] signature = identity.sign(
				signaturePayload(asInitiator, transcriptHash()));
		transcript.writeBytes(signature);
		byte[] finished = Crypto.hmac(asInitiator
				? initiatorFinishedKey : responderFinishedKey,
				transcriptHash());
		transcript.writeBytes(finished);
		ByteBuffer auth = ByteBuffer.allocate(2 + identityDer.length
				+ 2 + name.length + SIGNATURE_BYTES + FINISHED_BYTES);
		auth.putShort((short) identityDer.length);
		auth.put(identityDer);
		auth.putShort((short) name.length);
		auth.put(name);
		auth.put(signature);
		auth.put(finished);
		return auth.array();
	} // end of buildAuth method

	/**
	 * Decrypt an authentication block.
	 *
	 * @param key The handshake key the block was sealed under.
	 * @param which The message name, for error messages and AAD.
	 * @param ciphertext The sealed block.
	 *
	 * @return the authentication plaintext.
	 *
	 * @throws HandshakeRejected if the tag does not verify - a
	 *             tampered or mis-keyed handshake.
	 */
	private byte[] openAuth(byte[] key, String which,
			byte[] ciphertext) throws HandshakeRejected {

		try {
			return Crypto.aeadOpen(key, 0, aad(which), ciphertext);
		} catch (AEADBadTagException bad) {
			throw new HandshakeRejected("the encrypted part of "
					+ which + " failed authentication - the channel is"
					+ " tampered or the keys disagree");
		}
	} // end of openAuth method

	/**
	 * Parse and verify the peer's authentication block, appending each
	 * field to the transcript exactly as the peer did while building
	 * it: identity key and name first, then - against the hash at that
	 * point - the signature, then the finished MAC.
	 *
	 * @param auth The authentication plaintext.
	 * @param fromInitiator Whether the block claims the initiator
	 *            role.
	 *
	 * @throws HandshakeRejected if the block is malformed, the name
	 *             breaks the display-name rules, the signature does
	 *             not verify, or the finished MAC does not match.
	 */
	private void verifyAuth(byte[] auth, boolean fromInitiator)
			throws HandshakeRejected {

		String which = fromInitiator ? "m3" : "m2";
		ByteBuffer block = ByteBuffer.wrap(auth);
		byte[] identityDer = lengthPrefixed(block, MAX_KEY_BYTES,
				which, "identity key");
		byte[] nameBytes = lengthPrefixed(block,
				IdentityKey.MAX_NAME_BYTES, which, "display name");
		need(block, SIGNATURE_BYTES, which, "signature");
		byte[] signature = new byte[SIGNATURE_BYTES];
		block.get(signature);
		need(block, FINISHED_BYTES, which, "finished MAC");
		byte[] finished = new byte[FINISHED_BYTES];
		block.get(finished);
		requireEmpty(block, which);

		String name = decodeName(nameBytes, which);
		PublicKey claimed = parseIdentity(identityDer, which);
		transcript.writeBytes(identityDer);
		transcript.writeBytes(nameBytes);
		if (!IdentityKey.verify(claimed,
				signaturePayload(fromInitiator, transcriptHash()),
				signature)) {
			throw new HandshakeRejected("the identity signature in "
					+ which + " does not verify - the transcript or the"
					+ " claimed key is wrong");
		}
		transcript.writeBytes(signature);
		byte[] expected = Crypto.hmac(fromInitiator
				? initiatorFinishedKey : responderFinishedKey,
				transcriptHash());
		if (!MessageDigest.isEqual(expected, finished)) {
			throw new HandshakeRejected("the finished MAC in " + which
					+ " does not match - key confirmation failed");
		}
		transcript.writeBytes(finished);
		peerIdentity = claimed;
		peerName = name;
	} // end of verifyAuth method

	/**
	 * Derive the application keys and SAS from the final transcript
	 * and mark the handshake complete.
	 */
	private void completeLink() {

		byte[] finalHash = transcriptHash();
		byte[] initiatorToResponder = Crypto.hkdf(sharedSecret,
				"app i2r", SALT, finalHash, Crypto.KEY_BYTES);
		byte[] responderToInitiator = Crypto.hkdf(sharedSecret,
				"app r2i", SALT, finalHash, Crypto.KEY_BYTES);
		byte[] sasSecret = Crypto.hkdf(sharedSecret, "sas", SALT,
				finalHash, 8);
		link = new SecureLink(
				initiator ? initiatorToResponder : responderToInitiator,
				initiator ? responderToInitiator : initiatorToResponder,
				Sas.fromSecret(sasSecret),
				Crypto.hex(sessionId),
				IdentityKey.fingerprintOf(peerIdentity.getEncoded()),
				peerName);
		state = State.COMPLETE;
	} // end of completeLink method

	/**
	 * Compute the X25519 shared secret with the peer's ephemeral.
	 *
	 * @param peerEphemeralDer The peer's ephemeral key, X.509 DER.
	 *
	 * @throws HandshakeRejected if the key does not parse or the
	 *             agreement rejects it (for example a small-order
	 *             point).
	 */
	private void computeShared(byte[] peerEphemeralDer)
			throws HandshakeRejected {

		try {
			PublicKey peerEphemeral = KeyFactory.getInstance("X25519")
					.generatePublic(
							new X509EncodedKeySpec(peerEphemeralDer));
			KeyAgreement agreement = KeyAgreement.getInstance("X25519");
			agreement.init(ephemeral.getPrivate());
			agreement.doPhase(peerEphemeral, true);
			sharedSecret = agreement.generateSecret();
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks X25519 support", missing);
		} catch (InvalidKeySpecException | InvalidKeyException
				| IllegalStateException bad) {
			throw new HandshakeRejected("the peer's ephemeral key is"
					+ " not a usable X25519 key: " + bad.getMessage());
		}
	} // end of computeShared method

	/**
	 * Derive the four handshake-phase keys from the shared secret and
	 * the post-hello transcript hash.
	 */
	private void deriveHandshakeKeys() {

		byte[] helloHash = transcriptHash();
		responderHandshakeKey = Crypto.hkdf(sharedSecret, "hs resp",
				SALT, helloHash, Crypto.KEY_BYTES);
		initiatorHandshakeKey = Crypto.hkdf(sharedSecret, "hs init",
				SALT, helloHash, Crypto.KEY_BYTES);
		responderFinishedKey = Crypto.hkdf(sharedSecret, "fin resp",
				SALT, helloHash, Crypto.KEY_BYTES);
		initiatorFinishedKey = Crypto.hkdf(sharedSecret, "fin init",
				SALT, helloHash, Crypto.KEY_BYTES);
	} // end of deriveHandshakeKeys method

	/**
	 * The bytes an identity signature covers: a role label plus the
	 * transcript hash at signing time.
	 *
	 * @param asInitiator Which role label to bind.
	 * @param transcriptHash The hash the signature covers.
	 *
	 * @return the signature payload.
	 */
	private static byte[] signaturePayload(boolean asInitiator,
			byte[] transcriptHash) {

		byte[] label = (asInitiator ? "JLSCOLLAB1 verify initiator"
				: "JLSCOLLAB1 verify responder")
				.getBytes(StandardCharsets.US_ASCII);
		byte[] payload = new byte[label.length + transcriptHash.length];
		System.arraycopy(label, 0, payload, 0, label.length);
		System.arraycopy(transcriptHash, 0, payload, label.length,
				transcriptHash.length);
		return payload;
	} // end of signaturePayload method

	/**
	 * SHA-256 of the transcript so far.
	 *
	 * @return the 32-byte hash.
	 */
	private byte[] transcriptHash() {

		return Crypto.sha256(transcript.toByteArray());
	} // end of transcriptHash method

	/**
	 * The additional authenticated data for a handshake message.
	 *
	 * @param which The message name ("m2" or "m3").
	 *
	 * @return the AAD bytes.
	 */
	private static byte[] aad(String which) {

		return ("JLSCOLLAB1 " + which)
				.getBytes(StandardCharsets.US_ASCII);
	} // end of aad method

	/**
	 * The largest sealed authentication block the protocol allows:
	 * the field caps plus the AEAD tag.
	 *
	 * @return the cap in bytes.
	 */
	private static int maxAuthCiphertext() {

		return 2 + MAX_KEY_BYTES + 2 + IdentityKey.MAX_NAME_BYTES
				+ SIGNATURE_BYTES + FINISHED_BYTES + Crypto.TAG_BYTES;
	} // end of maxAuthCiphertext method

	/**
	 * Decode and validate a peer display name.
	 *
	 * @param nameBytes The raw UTF-8 bytes.
	 * @param which The message name, for the error.
	 *
	 * @return the validated name.
	 *
	 * @throws HandshakeRejected if the bytes are not valid UTF-8 or
	 *             the name breaks the display-name rules.
	 */
	private static String decodeName(byte[] nameBytes, String which)
			throws HandshakeRejected {

		String name;
		try {
			name = StandardCharsets.UTF_8.newDecoder()
					.decode(ByteBuffer.wrap(nameBytes)).toString();
		} catch (CharacterCodingException bad) {
			throw new HandshakeRejected("the display name in " + which
					+ " is not valid UTF-8");
		}
		try {
			IdentityKey.validateDisplayName(name);
		} catch (IllegalArgumentException bad) {
			throw new HandshakeRejected("the display name in " + which
					+ " is invalid: " + bad.getMessage());
		}
		return name;
	} // end of decodeName method

	/**
	 * Parse a claimed Ed25519 identity key.
	 *
	 * @param identityDer The X.509 DER encoding.
	 * @param which The message name, for the error.
	 *
	 * @return the parsed key.
	 *
	 * @throws HandshakeRejected if the encoding does not parse.
	 */
	private static PublicKey parseIdentity(byte[] identityDer,
			String which) throws HandshakeRejected {

		try {
			return KeyFactory.getInstance("Ed25519").generatePublic(
					new X509EncodedKeySpec(identityDer));
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks Ed25519 support", missing);
		} catch (InvalidKeySpecException bad) {
			throw new HandshakeRejected("the identity key in " + which
					+ " is not a parsable Ed25519 key");
		}
	} // end of parseIdentity method

	/**
	 * Wrap a received message, enforcing the null and size caps.
	 *
	 * @param message The received bytes.
	 * @param which The message name, for the error.
	 *
	 * @return a buffer over the message.
	 *
	 * @throws HandshakeRejected if the message is null or over-cap.
	 */
	private static ByteBuffer checkedBuffer(byte[] message,
			String which) throws HandshakeRejected {

		if (message == null) {
			throw new HandshakeRejected(which + " is missing");
		}
		if (message.length > MAX_MESSAGE_BYTES) {
			throw new HandshakeRejected(which + " has "
					+ message.length + " bytes; the cap is "
					+ MAX_MESSAGE_BYTES);
		}
		return ByteBuffer.wrap(message);
	} // end of checkedBuffer method

	/**
	 * Reject a message with fewer bytes remaining than a field needs.
	 *
	 * @param buffer The message buffer.
	 * @param count How many bytes the field needs.
	 * @param which The message name, for the error.
	 * @param what The field name, for the error.
	 *
	 * @throws HandshakeRejected if the field would run off the end.
	 */
	private static void need(ByteBuffer buffer, int count,
			String which, String what) throws HandshakeRejected {

		if (buffer.remaining() < count) {
			throw new HandshakeRejected(which + " ends inside its "
					+ what);
		}
	} // end of need method

	/**
	 * Read one length-prefixed field from a message.
	 *
	 * @param buffer The message buffer, positioned at a 2-byte length.
	 * @param cap The most bytes the field may hold.
	 * @param which The message name, for the error.
	 * @param what The field name, for the error.
	 *
	 * @return the field bytes.
	 *
	 * @throws HandshakeRejected if the field is missing, truncated,
	 *             or over its cap.
	 */
	private static byte[] lengthPrefixed(ByteBuffer buffer, int cap,
			String which, String what) throws HandshakeRejected {

		need(buffer, 2, which, what + " length");
		int length = buffer.getShort() & 0xffff;
		if (length > cap) {
			throw new HandshakeRejected(which + " claims a " + what
					+ " of " + length + " bytes; the cap is " + cap);
		}
		need(buffer, length, which, what);
		byte[] field = new byte[length];
		buffer.get(field);
		return field;
	} // end of lengthPrefixed method

	/**
	 * Reject trailing bytes after a fully parsed message.
	 *
	 * @param buffer The message buffer.
	 * @param which The message name, for the error.
	 *
	 * @throws HandshakeRejected if bytes remain.
	 */
	private static void requireEmpty(ByteBuffer buffer, String which)
			throws HandshakeRejected {

		if (buffer.hasRemaining()) {
			throw new HandshakeRejected(which + " has "
					+ buffer.remaining() + " trailing bytes");
		}
	} // end of requireEmpty method

	/**
	 * Enforce the state machine: each message method runs exactly once
	 * and in order, on the right role.
	 *
	 * @param expected The state this call requires.
	 *
	 * @throws IllegalStateException if the handshake is elsewhere -
	 *             including permanently failed.
	 */
	private void requireState(State expected) {

		if (state != expected) {
			throw new IllegalStateException("this handshake is in state "
					+ state + ", not " + expected);
		}
	} // end of requireState method

	/**
	 * Generate a fresh X25519 ephemeral pair.
	 *
	 * @return the ephemeral pair, used for exactly this handshake.
	 */
	private static KeyPair freshEphemeral() {

		try {
			return KeyPairGenerator.getInstance("X25519")
					.generateKeyPair();
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException(
					"this JVM lacks X25519 support", missing);
		}
	} // end of freshEphemeral method

} // end of Handshake class
