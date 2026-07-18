package jls.collab.net;

/**
 * A handshake message that could not be accepted (issue #168): wrong
 * protocol magic or version, malformed structure, an over-cap field, a
 * failed decryption, a bad signature, or a finished-MAC mismatch. In
 * the #38 hostile-input style this is a rejection, never a best-effort
 * repair - the handshake that threw it is dead and must be abandoned.
 */
public class HandshakeRejected extends Exception {

	/** Serialization version (this exception is never sent anywhere). */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a rejection.
	 *
	 * @param reason What was wrong with the handshake message.
	 */
	public HandshakeRejected(String reason) {

		super(reason);
	} // end of constructor

} // end of HandshakeRejected class
