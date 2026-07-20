/**
 * The session-security foundation of collaborative editing (issue
 * #168, collab Stage 1a): per-install identity, mutually authenticated
 * key exchange, short-authentication-string verification, and capped
 * AEAD framing - built entirely from JDK primitives (X25519, Ed25519,
 * HKDF-SHA256 via JEP 478, AES-256-GCM), with no new dependency
 * and no circuit semantics whatsoever: a {@link
 * jls.collab.net.SecureLink} carries opaque byte payloads only.
 *
 * <ul>
 * <li>{@link jls.collab.net.IdentityKey} - the install's long-term
 * Ed25519 identity, persisted with owner-only permissions; the
 * public-key fingerprint is the peer id (research doc section
 * 5.1).</li>
 * <li>{@link jls.collab.net.Handshake} - the three-message,
 * TLS-1.3-with-raw-public-keys-shaped exchange producing per-direction
 * session keys and a transcript-bound SAS secret (section 5.2).</li>
 * <li>{@link jls.collab.net.Sas} - seven named glyphs both humans
 * compare out of band; the Signal/Telegram construction (section
 * 5.2 step 3).</li>
 * <li>{@link jls.collab.net.SecureLink} - per-frame encryption with
 * counter nonces, hard caps, typed rejection, and fail-closed
 * poisoning (section 6.4).</li>
 * <li>{@link jls.collab.net.KnownPeers} - persisted trust: verified
 * keys skip re-verification; a changed key for a known name is a loud
 * warning (section 5.2 step 4).</li>
 * <li>{@link jls.collab.net.SessionListener} - the one place in JLS
 * that binds a server socket, and only after an explicit Share
 * gesture; binding is separate from accepting so the join string shows
 * before any peer connects.</li>
 * <li>{@link jls.collab.net.SocketSession} - a live TCP session: it
 * drives the handshake over a socket's byte streams, then carries
 * {@link jls.collab.net.SecureLink} frames in both directions with one
 * blocking I/O call per receive (the caller owns the session thread).
 * </li>
 * </ul>
 *
 * The crypto core is transport-agnostic; the socket layer is the thin
 * shell that moves its bytes. Socket construction lives only here (the
 * socket-confinement ratchet {@code jls.SocketConfinementRatchetTest}
 * and {@code jls.ArchitectureRulesTest} pin that), so a default GUI
 * start and batch mode - which construct no listener - open no port.
 * The join/verify and key-change dialogs are the following #168 slice,
 * under {@code jls.collab.ui}. No Swing is imported here (enforced by
 * {@code jls.ArchitectureRulesTest}) and no Java object serialization
 * exists in the protocol (enforced by the same ratchet test). Frame
 * and handshake parsing both follow the #38 hostile-input discipline:
 * length caps checked before allocation, typed rejections, never
 * repair.
 */
package jls.collab.net;
