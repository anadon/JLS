package jls.collab.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The listening endpoint a session starter opens when it chooses Share
 * (issue #168, collab Stage 1a): the one place in JLS that binds a
 * server socket, and only ever after an explicit gesture. A default
 * GUI start and batch mode construct no listener, so no port is opened
 * without a human asking (issue #168 P3, pinned by {@code
 * jls.SocketConfinementRatchetTest} and {@code
 * jls.ArchitectureRulesTest}).
 *
 * <p>Binding is separate from accepting so the join string can be
 * shown - the bound {@link #port()} is known the instant {@link
 * #bind(InetAddress, int)} returns, before anyone connects. {@link
 * #accept(IdentityKey)} then blocks for one connection and runs the
 * responder (starter) side of the {@link Handshake} over it, returning
 * a live {@link SocketSession} whose SAS the two humans compare out of
 * band. A handshake that fails to authenticate closes its accepted
 * socket and propagates the rejection; the listener stays open for the
 * next attempt.</p>
 *
 * <p>The default bind address is loopback ({@link #bindLoopback(int)}),
 * the safe choice for the loopback two-instance harness and for a host
 * that has not opted into LAN exposure; {@link #bind(InetAddress, int)}
 * takes an explicit address for real peer-to-peer use. The listener
 * carries no circuit semantics and constructs its sockets only here,
 * inside {@code jls.collab.net}.</p>
 */
public final class SessionListener implements Closeable {

	/** The backlog of one: a session accepts a single peer at a time. */
	private static final int BACKLOG = 1;

	/** The bound server socket this listener owns and closes. */
	private final ServerSocket serverSocket;

	/**
	 * Wrap a bound server socket. Package-private: {@link
	 * #bind(InetAddress, int)} is the only constructor path.
	 *
	 * @param serverSocket The already-bound server socket.
	 */
	private SessionListener(ServerSocket serverSocket) {

		this.serverSocket = serverSocket;
	} // end of constructor

	/**
	 * Bind a listener to a specific address and port. A port of zero
	 * asks the system for an ephemeral port, which {@link #port()} then
	 * reports - the usual choice for tests and for a join string the
	 * starter reads back to the joiner.
	 *
	 * @param address The address to bind (loopback, a LAN address, or a
	 *            wildcard); never a remote address.
	 * @param port The port to bind, or zero for a system-chosen one.
	 *
	 * @return the bound listener, ready to {@link #accept(IdentityKey)}.
	 *
	 * @throws IOException if the address and port cannot be bound.
	 */
	public static SessionListener bind(InetAddress address, int port)
			throws IOException {

		ServerSocket serverSocket = new ServerSocket();
		try {
			serverSocket.bind(new InetSocketAddress(address, port),
					BACKLOG);
		} catch (IOException failed) {
			SocketSession.closeQuietly(serverSocket);
			throw failed;
		}
		return new SessionListener(serverSocket);
	} // end of bind method

	/**
	 * Bind a listener to the loopback address and a port. The
	 * conservative default: reachable only from the same host, which is
	 * the loopback two-instance harness's world and a safe starting
	 * point before a host opts into LAN exposure.
	 *
	 * @param port The port to bind, or zero for a system-chosen one.
	 *
	 * @return the bound listener.
	 *
	 * @throws IOException if the port cannot be bound.
	 */
	public static SessionListener bindLoopback(int port)
			throws IOException {

		return bind(InetAddress.getLoopbackAddress(), port);
	} // end of bindLoopback method

	/**
	 * The port this listener is bound to. Known as soon as the bind
	 * returns, so the starter can show the join string before any peer
	 * connects.
	 *
	 * @return the bound local port.
	 */
	public int port() {

		return serverSocket.getLocalPort();
	} // end of port method

	/**
	 * The address this listener is bound to.
	 *
	 * @return the bound local address.
	 */
	public InetAddress address() {

		return serverSocket.getInetAddress();
	} // end of address method

	/**
	 * Accept one incoming connection and run the responder (starter)
	 * side of the handshake over it. Blocks until a peer connects. On a
	 * handshake rejection the accepted socket is closed and the
	 * rejection propagates; the listener itself stays open for another
	 * attempt.
	 *
	 * @param identity This install's long-term identity.
	 *
	 * @return the live, encrypted session with the peer.
	 *
	 * @throws IOException if accepting or a socket read/write fails.
	 * @throws HandshakeRejected if the joiner's handshake messages do
	 *             not authenticate.
	 */
	public SocketSession accept(IdentityKey identity)
			throws IOException, HandshakeRejected {

		Socket socket = serverSocket.accept();
		try {
			return SocketSession.driveResponder(socket, identity);
		} catch (IOException | HandshakeRejected | RuntimeException failed) {
			SocketSession.closeQuietly(socket);
			throw failed;
		}
	} // end of accept method

	/**
	 * Close the server socket, stopping the listener. Any session
	 * already returned by {@link #accept(IdentityKey)} keeps its own
	 * connection; this only stops new ones. Idempotent.
	 *
	 * @throws IOException if closing the server socket fails.
	 */
	@Override
	public void close() throws IOException {

		serverSocket.close();
	} // end of close method

} // end of SessionListener class
