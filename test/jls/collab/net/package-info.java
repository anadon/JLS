/**
 * Tests for the collab session-security foundation (issue #168):
 * handshake completion and SAS agreement with an exhaustive
 * tamper-every-byte property (prediction P1), the hostile frame corpus
 * with typed rejections and no pre-allocation (P2), identity-key
 * persistence with owner-only permissions, and the known-peers trust
 * model (reconnect skips verification, key change warns - P4's model
 * layer). The socket-confinement ratchet (P3) lives in {@code
 * jls.SocketConfinementRatchetTest} beside the repo's other ratchets.
 */
package jls.collab.net;
