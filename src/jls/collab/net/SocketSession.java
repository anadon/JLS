package jls.collab.net;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * One live, mutually authenticated, encrypted TCP session between two
 * JLS installs (issue #168, collab Stage 1a): the socket the crypto
 * core was deliberately built without. A {@link Handshake} is
 * transport-agnostic - it turns received message bytes into reply
 * bytes and leaves the moving to a caller - and {@link SecureLink}
 * carries opaque frames over any {@link InputStream}; this class is
 * that caller and that stream, and nothing more. It drives the
 * three-message exchange over a socket's byte streams, then wraps the
 * resulting link so application payloads flow as capped, counter-nonce
 * AEAD frames in each direction.
 *
 * <p>Framing has two layers, each length-prefixed and capped before a
 * single body byte is read (the #38 hostile-input discipline). The
 * handshake phase delimits each of m1/m2/m3 with a four-byte
 * big-endian length, rejected above {@value #HANDSHAKE_FRAME_CAP}
 * bytes - well clear of the largest legitimate handshake message, so a
 * hostile length aborts before any allocation. The application phase
 * hands the socket's input stream straight to {@link
 * SecureLink#open(InputStream)}, whose own four-byte prefix and
 * {@value SecureLink#MAX_PAYLOAD_BYTES}-byte cap govern from there.</p>
 *
 * <p>This class holds no threads of its own: {@link #receive()} blocks
 * the calling thread until a frame arrives or the peer closes, which
 * is exactly the "one I/O thread per session" shape issue #168 asks
 * for - the session layer above spins that thread and marshals results
 * onto the EDT through {@code jls.collab.ui}. It carries no circuit
 * semantics whatsoever ({@code jls.ArchitectureRulesTest} pins that);
 * a payload's meaning is the Stage 1b vocabulary's business.</p>
 *
 * <p>The socket is bound to loopback or a caller-chosen address by
 * {@link SessionListener}; a default GUI start and batch mode
 * construct no listener and therefore no session, so no port is ever
 * opened without an explicit Share/Join gesture (issue #168 P3).</p>
 */
public final class SocketSession implements Closeable {

	/**
	 * Hostile-input cap on a single handshake message's length prefix.
	 * The largest legitimate handshake message (m2, carrying an
	 * ephemeral key and the sealed identity block) is well under a
	 * kilobyte; this ceiling gives generous headroom while still
	 * bounding the one allocation the transport makes per message, so a
	 * peer that claims a huge length is rejected before any buffer is
	 * sized to it.
	 */
	static final int HANDSHAKE_FRAME_CAP = 8192;

	/**
	 * The default read timeout, in milliseconds, applied while the
	 * handshake is in flight. A peer that stalls mid-handshake must not
	 * pin the accepting thread forever; once the exchange completes the
	 * timeout is cleared and {@link #receive()} blocks indefinitely on
	 * demand, the normal shape for a long-lived session.
	 */
	static final int HANDSHAKE_TIMEOUT_MILLIS = 30_000;

	/** The connected socket this session owns and closes. */
	private final Socket socket;

	/** The socket's buffered input stream, shared across frame reads. */
	private final InputStream in;

	/** The socket's output stream, written under the frame lock. */
	private final OutputStream out;

	/** The completed link carrying this session's frames. */
	private final SecureLink link;

	/**
	 * Bind a connected socket to the link its handshake produced.
	 * Package-private: only the drivers in this class and {@link
	 * SessionListener} construct sessions.
	 *
	 * @param socket The connected socket, now owned by this session.
	 * @param in The socket's (buffered) input stream.
	 * @param out The socket's output stream.
	 * @param link The completed secure link over that socket.
	 */
	private SocketSession(Socket socket, InputStream in, OutputStream out,
			SecureLink link) {

		this.socket = socket;
		this.in = in;
		this.out = out;
		this.link = link;
	} // end of constructor

	/**
	 * Join a session: connect to a starter, run the joiner (initiator)
	 * side of the handshake over the socket, and return the live
	 * session. The caller compares {@link #link()}'s SAS with the peer
	 * out of band before trusting the channel (issue #168 SAS
	 * verification); this method establishes the encrypted pipe, not
	 * the human trust decision.
	 *
	 * @param host The starter's address.
	 * @param port The starter's listening port.
	 * @param identity This install's long-term identity.
	 * @param connectTimeoutMillis How long to wait for the TCP connect,
	 *            in milliseconds; zero waits for the system default.
	 *
	 * @return the live, encrypted session.
	 *
	 * @throws IOException if the connect or a socket read/write fails.
	 * @throws HandshakeRejected if the starter's handshake messages do
	 *             not authenticate; the socket is closed first.
	 */
	public static SocketSession join(InetAddress host, int port,
			IdentityKey identity, int connectTimeoutMillis)
			throws IOException, HandshakeRejected {

		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port),
					connectTimeoutMillis);
			return driveInitiator(socket, identity);
		} catch (IOException | HandshakeRejected | RuntimeException failed) {
			closeQuietly(socket);
			throw failed;
		}
	} // end of join method

	/**
	 * Run the initiator (joiner) side of the handshake over an
	 * already-connected socket and wrap the result. The socket read
	 * timeout is held tight for the handshake, then cleared for the
	 * open-ended application phase.
	 *
	 * @param socket The connected socket to drive.
	 * @param identity This install's long-term identity.
	 *
	 * @return the live session over that socket.
	 *
	 * @throws IOException if a socket read or write fails.
	 * @throws HandshakeRejected if the responder's messages do not
	 *             authenticate.
	 */
	static SocketSession driveInitiator(Socket socket, IdentityKey identity)
			throws IOException, HandshakeRejected {

		socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
		InputStream in = new BufferedInputStream(socket.getInputStream());
		OutputStream out = socket.getOutputStream();
		Handshake handshake = Handshake.initiate(identity);
		writeMessage(out, handshake.firstMessage());
		byte[] m2 = readMessage(in);
		writeMessage(out, handshake.acceptReply(m2));
		SecureLink link = handshake.link();
		socket.setSoTimeout(0);
		return new SocketSession(socket, in, out, link);
	} // end of driveInitiator method

	/**
	 * Run the responder (starter) side of the handshake over an
	 * accepted socket and wrap the result. Called only by {@link
	 * SessionListener#accept(IdentityKey)}.
	 *
	 * @param socket The accepted socket to drive.
	 * @param identity This install's long-term identity.
	 *
	 * @return the live session over that socket.
	 *
	 * @throws IOException if a socket read or write fails.
	 * @throws HandshakeRejected if the joiner's messages do not
	 *             authenticate.
	 */
	static SocketSession driveResponder(Socket socket, IdentityKey identity)
			throws IOException, HandshakeRejected {

		socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
		InputStream in = new BufferedInputStream(socket.getInputStream());
		OutputStream out = socket.getOutputStream();
		Handshake handshake = Handshake.respond(identity);
		byte[] m1 = readMessage(in);
		writeMessage(out, handshake.acceptFirst(m1));
		byte[] m3 = readMessage(in);
		handshake.acceptFinish(m3);
		SecureLink link = handshake.link();
		socket.setSoTimeout(0);
		return new SocketSession(socket, in, out, link);
	} // end of driveResponder method

	/**
	 * The completed link: its SAS is what two humans compare out of
	 * band, and its peer fingerprint and display name identify who is
	 * on the other end.
	 *
	 * @return the secure link this session carries frames over.
	 */
	public SecureLink link() {

		return link;
	} // end of link method

	/**
	 * The peer's remote address, for display in the session UI and for
	 * logging a connection.
	 *
	 * @return the connected peer's address.
	 */
	public InetAddress peerAddress() {

		return socket.getInetAddress();
	} // end of peerAddress method

	/**
	 * Seal a payload and write its frame to the peer. The payload's
	 * meaning is opaque here; the link enforces the size cap and the
	 * counter nonce.
	 *
	 * @param payload The opaque payload bytes (may be empty).
	 *
	 * @throws IOException if the socket write fails.
	 * @throws FrameRejected if the payload is over the cap or the link
	 *             has been poisoned by an earlier failure.
	 */
	public void send(byte[] payload) throws IOException, FrameRejected {

		byte[] frame = link.seal(payload);
		out.write(frame);
		out.flush();
	} // end of send method

	/**
	 * Block until the next frame arrives, then decrypt and return its
	 * payload. A clean stream end - the peer closing on a frame
	 * boundary - returns null.
	 *
	 * @return the next payload, or null if the peer closed cleanly.
	 *
	 * @throws IOException if the socket read fails.
	 * @throws FrameRejected if the frame is over-cap, truncated, or
	 *             fails authentication (tampered, replayed, or
	 *             reordered), which poisons the link for good.
	 */
	public byte[] receive() throws IOException, FrameRejected {

		return link.open(in);
	} // end of receive method

	/**
	 * Close the underlying socket, ending the session. Idempotent and
	 * safe to call from a finally block.
	 *
	 * @throws IOException if closing the socket fails.
	 */
	@Override
	public void close() throws IOException {

		socket.close();
	} // end of close method

	/**
	 * Write one length-delimited handshake message: a four-byte
	 * big-endian length prefix followed by the bytes.
	 *
	 * @param out The stream to write to.
	 * @param message The handshake message bytes.
	 *
	 * @throws IOException if the write fails.
	 */
	private static void writeMessage(OutputStream out, byte[] message)
			throws IOException {

		DataOutputStream data = new DataOutputStream(out);
		data.writeInt(message.length);
		data.write(message);
		data.flush();
	} // end of writeMessage method

	/**
	 * Read one length-delimited handshake message. The four-byte length
	 * is validated against zero and {@value #HANDSHAKE_FRAME_CAP}
	 * before the body buffer is allocated, so a hostile length cannot
	 * force a large allocation.
	 *
	 * @param in The stream to read from.
	 *
	 * @return the message bytes.
	 *
	 * @throws IOException if the stream ends early or the read fails.
	 * @throws HandshakeRejected if the declared length is negative or
	 *             over the transport cap.
	 */
	private static byte[] readMessage(InputStream in)
			throws IOException, HandshakeRejected {

		byte[] prefix = new byte[4];
		readFully(in, prefix, "handshake message length");
		int length = ((prefix[0] & 0xff) << 24)
				| ((prefix[1] & 0xff) << 16)
				| ((prefix[2] & 0xff) << 8)
				| (prefix[3] & 0xff);
		if (length < 0 || length > HANDSHAKE_FRAME_CAP) {
			throw new HandshakeRejected("a handshake message declares "
					+ length + " bytes; the transport cap is "
					+ HANDSHAKE_FRAME_CAP);
		}
		byte[] message = new byte[length];
		readFully(in, message, "handshake message body");
		return message;
	} // end of readMessage method

	/**
	 * Fill a buffer from a stream or fail if it ends first.
	 *
	 * @param in The stream to read from.
	 * @param buffer The buffer to fill completely.
	 * @param what The field name, for the error message.
	 *
	 * @throws IOException if the stream ends before the buffer fills or
	 *             the read fails.
	 */
	private static void readFully(InputStream in, byte[] buffer,
			String what) throws IOException {

		int at = 0;
		while (at < buffer.length) {
			int got = in.read(buffer, at, buffer.length - at);
			if (got < 0) {
				throw new EOFException("the connection ended inside the "
						+ what);
			}
			at += got;
		}
	} // end of readFully method

	/**
	 * Close a socket or server socket, swallowing any failure. Used on
	 * the error path so the original exception is the one that
	 * propagates.
	 *
	 * @param endpoint The socket or server socket to close.
	 */
	static void closeQuietly(Closeable endpoint) {

		try {
			endpoint.close();
		} catch (IOException ignored) {
			// the original failure is the one worth reporting
		}
	} // end of closeQuietly method

} // end of SocketSession class
