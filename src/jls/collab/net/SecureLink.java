package jls.collab.net;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.crypto.AEADBadTagException;

/**
 * The encrypted point-to-point channel a completed {@link Handshake}
 * yields (issue #168): opaque payload frames, each sealed with
 * ChaCha20-Poly1305 under a per-direction key with a strictly
 * incrementing counter nonce. The wire form of a frame is a four-byte
 * big-endian ciphertext length followed by the ciphertext; payloads
 * are capped at {@value #MAX_PAYLOAD_BYTES} bytes and an over-cap
 * length is rejected <em>before</em> any body byte is read or any
 * buffer allocated (#38 resource-exhaustion discipline).
 *
 * Because the nonce is the frame counter, a replayed, reordered,
 * dropped-then-resumed, or tampered frame fails tag verification -
 * and after any failure the link is poisoned: an authenticated channel
 * that has seen a bad tag cannot re-synchronize trust, so every later
 * seal or open is rejected too. The transport layer above reacts by
 * closing the connection.
 *
 * This class carries bytes only - no circuit semantics, no Java object
 * serialization, ever (research doc section 6.1). What the bytes mean
 * is the Stage 1b vocabulary's business.
 */
public final class SecureLink {

	/** The most payload bytes one frame may carry. */
	public static final int MAX_PAYLOAD_BYTES = 1 << 20;

	/** The additional authenticated data binding frames to v1. */
	private static final byte[] FRAME_AAD =
			"JLSCOLLAB1 frame".getBytes(StandardCharsets.US_ASCII);

	/** The AEAD key for frames this side sends. */
	private final byte[] sendKey;

	/** The AEAD key for frames this side receives. */
	private final byte[] receiveKey;

	/** The short authentication string both humans compare. */
	private final Sas sas;

	/** The session id, lowercase hex. */
	private final String sessionId;

	/** The authenticated peer's public-key fingerprint. */
	private final String peerFingerprint;

	/** The authenticated peer's claimed display name. */
	private final String peerDisplayName;

	/** The counter naming the next frame this side seals. */
	private long sendCounter;

	/** The counter the next received frame must decrypt under. */
	private long receiveCounter;

	/** Once true, every seal and open is rejected. */
	private boolean poisoned;

	/**
	 * Bind a completed handshake's keys and peer facts into a link.
	 * Package-private: only {@link Handshake} constructs links.
	 *
	 * @param sendKey The AEAD key for this side's outgoing frames.
	 * @param receiveKey The AEAD key for incoming frames.
	 * @param sas The derived short authentication string.
	 * @param sessionId The session id, lowercase hex.
	 * @param peerFingerprint The peer's key fingerprint.
	 * @param peerDisplayName The peer's claimed display name.
	 */
	SecureLink(byte[] sendKey, byte[] receiveKey, Sas sas,
			String sessionId, String peerFingerprint,
			String peerDisplayName) {

		this.sendKey = sendKey;
		this.receiveKey = receiveKey;
		this.sas = sas;
		this.sessionId = sessionId;
		this.peerFingerprint = peerFingerprint;
		this.peerDisplayName = peerDisplayName;
	} // end of constructor

	/**
	 * The short authentication string for the verify dialog: both
	 * humans compare these glyphs out of band before trusting the
	 * link.
	 *
	 * @return the SAS.
	 */
	public Sas sas() {

		return sas;
	} // end of sas method

	/**
	 * The session id the responder minted, in lowercase hex.
	 *
	 * @return the 32-character session id.
	 */
	public String sessionId() {

		return sessionId;
	} // end of sessionId method

	/**
	 * The authenticated peer's identity: the fingerprint of the
	 * Ed25519 key that signed the handshake.
	 *
	 * @return the 64-character lowercase hex fingerprint.
	 */
	public String peerFingerprint() {

		return peerFingerprint;
	} // end of peerFingerprint method

	/**
	 * The display name the peer claimed in the handshake. A label
	 * only - {@link #peerFingerprint()} is the identity; the
	 * {@link KnownPeers} store decides whether the pair is trusted.
	 *
	 * @return the peer's display name.
	 */
	public String peerDisplayName() {

		return peerDisplayName;
	} // end of peerDisplayName method

	/**
	 * Seal a payload into its wire frame.
	 *
	 * @param payload The opaque payload bytes (may be empty).
	 *
	 * @return the wire frame: length prefix plus ciphertext.
	 *
	 * @throws FrameRejected if the payload is over the cap or the
	 *             link is poisoned.
	 */
	public byte[] seal(byte[] payload) throws FrameRejected {

		requireHealthy();
		if (payload == null) {
			throw poison("a frame payload cannot be null");
		}
		if (payload.length > MAX_PAYLOAD_BYTES) {
			throw poison("a frame payload of " + payload.length
					+ " bytes exceeds the cap of " + MAX_PAYLOAD_BYTES);
		}
		byte[] ciphertext = Crypto.aeadSeal(sendKey, sendCounter,
				FRAME_AAD, payload);
		sendCounter += 1;
		ByteBuffer frame = ByteBuffer.allocate(4 + ciphertext.length);
		frame.putInt(ciphertext.length);
		frame.put(ciphertext);
		return frame.array();
	} // end of seal method

	/**
	 * Read and open the next frame from a wire stream. The length
	 * prefix is checked against the cap before the body is read, so an
	 * attacker-supplied length never causes an allocation.
	 *
	 * @param wire The stream carrying frames.
	 *
	 * @return the payload, or null if the stream ended cleanly on a
	 *         frame boundary.
	 *
	 * @throws FrameRejected if the link is poisoned, the length is
	 *             over-cap, the stream ends mid-frame, or the
	 *             ciphertext fails authentication.
	 */
	public byte[] open(InputStream wire) throws FrameRejected {

		requireHealthy();
		byte[] prefix = new byte[4];
		int first;
		try {
			first = wire.read();
			if (first < 0) {
				return null;
			}
			prefix[0] = (byte) first;
			readFully(wire, prefix, 1, "length prefix");
			int length = ByteBuffer.wrap(prefix).getInt();
			if (length < Crypto.TAG_BYTES) {
				throw poison("a frame ciphertext of " + length
						+ " bytes is shorter than its own tag");
			}
			if (length > MAX_PAYLOAD_BYTES + Crypto.TAG_BYTES) {
				throw poison("a frame ciphertext of " + length
						+ " bytes exceeds the cap of "
						+ (MAX_PAYLOAD_BYTES + Crypto.TAG_BYTES));
			}
			byte[] ciphertext = new byte[length];
			readFully(wire, ciphertext, 0, "ciphertext");
			return openCiphertext(ciphertext);
		} catch (IOException failed) {
			throw poison("reading a frame failed: "
					+ failed.getMessage());
		}
	} // end of open method

	/**
	 * Open one complete wire frame held in memory (the loopback tests'
	 * entry point; transport code uses {@link #open(InputStream)}).
	 *
	 * @param frame The full wire frame: prefix plus ciphertext.
	 *
	 * @return the payload.
	 *
	 * @throws FrameRejected if the frame is malformed, over-cap, or
	 *             fails authentication, or the link is poisoned.
	 */
	public byte[] openFrame(byte[] frame) throws FrameRejected {

		requireHealthy();
		if (frame == null || frame.length < 4) {
			throw poison("a wire frame needs at least its 4-byte"
					+ " length prefix");
		}
		int length = ByteBuffer.wrap(frame).getInt();
		if (length < Crypto.TAG_BYTES) {
			throw poison("a frame ciphertext of " + length
					+ " bytes is shorter than its own tag");
		}
		if (length > MAX_PAYLOAD_BYTES + Crypto.TAG_BYTES) {
			throw poison("a frame ciphertext of " + length
					+ " bytes exceeds the cap of "
					+ (MAX_PAYLOAD_BYTES + Crypto.TAG_BYTES));
		}
		if (frame.length - 4 != length) {
			throw poison("a wire frame declares " + length
					+ " ciphertext bytes but carries "
					+ (frame.length - 4));
		}
		byte[] ciphertext = new byte[length];
		System.arraycopy(frame, 4, ciphertext, 0, length);
		return openCiphertext(ciphertext);
	} // end of openFrame method

	/**
	 * Whether this link has been poisoned by a rejected frame.
	 *
	 * @return true once any seal or open has failed.
	 */
	public boolean isPoisoned() {

		return poisoned;
	} // end of isPoisoned method

	/**
	 * Decrypt a ciphertext under the receive key and expected counter.
	 *
	 * @param ciphertext The frame ciphertext with its tag.
	 *
	 * @return the payload.
	 *
	 * @throws FrameRejected if the tag fails - tampered, replayed,
	 *             reordered, or garbage.
	 */
	private byte[] openCiphertext(byte[] ciphertext)
			throws FrameRejected {

		try {
			byte[] payload = Crypto.aeadOpen(receiveKey,
					receiveCounter, FRAME_AAD, ciphertext);
			receiveCounter += 1;
			return payload;
		} catch (AEADBadTagException bad) {
			throw poison("frame " + receiveCounter + " failed"
					+ " authentication - tampered, replayed, reordered,"
					+ " or not a frame");
		}
	} // end of openCiphertext method

	/**
	 * Fill a buffer from the stream or reject the frame as truncated.
	 *
	 * @param wire The stream.
	 * @param buffer The buffer to fill.
	 * @param from The first index to fill.
	 * @param what The field name, for the error.
	 *
	 * @throws IOException if the stream itself fails.
	 * @throws FrameRejected if the stream ends before the buffer
	 *             fills.
	 */
	private void readFully(InputStream wire, byte[] buffer, int from,
			String what) throws IOException, FrameRejected {

		int at = from;
		while (at < buffer.length) {
			int got = wire.read(buffer, at, buffer.length - at);
			if (got < 0) {
				throw poison("the stream ended inside a frame's "
						+ what);
			}
			at += got;
		}
	} // end of readFully method

	/**
	 * Reject any use of a poisoned link.
	 *
	 * @throws FrameRejected if a frame has already been rejected.
	 */
	private void requireHealthy() throws FrameRejected {

		if (poisoned) {
			throw new FrameRejected("this link failed earlier and"
					+ " cannot carry more frames");
		}
	} // end of requireHealthy method

	/**
	 * Poison the link and build the rejection to throw.
	 *
	 * @param reason What was wrong.
	 *
	 * @return the rejection (thrown by the caller, so the compiler
	 *         sees the throw at the call site).
	 */
	private FrameRejected poison(String reason) {

		poisoned = true;
		return new FrameRejected(reason);
	} // end of poison method

} // end of SecureLink class
