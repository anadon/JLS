/**
 * Tests for the collab session-security foundation (issue #168):
 * handshake completion and SAS agreement with an exhaustive
 * tamper-every-byte property (prediction P1), the hostile frame corpus
 * with typed rejections and no pre-allocation (P2), identity-key
 * persistence with owner-only permissions, and the known-peers trust
 * model (reconnect skips verification, key change warns - P4's model
 * layer). The socket-confinement ratchet (P3) lives in {@code
 * jls.SocketConfinementRatchetTest} beside the repo's other ratchets.
 * The transport seam (issue #163) is covered three ways: {@code
 * LoopbackTransportTest} pins the in-memory pair's own semantics,
 * {@code TransportContractTest} runs one contract suite against both
 * the loopback pair and a real handshaken socket pair so the test
 * double cannot drift from the real transport, and {@code
 * ChaosTransportTest} pins the seeded drop/duplicate/reorder/partition
 * decorator ({@code ChaosTransport}, in this tree) that the
 * replication stack's fault-injection tests will drive.
 */
package jls.collab.net;
