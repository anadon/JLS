package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jls.sim.BatchSimulator;

/**
 * The runtime complement to the network-surface confinement ratchets
 * (issue #168, prediction P3, the listener-hygiene half): the two
 * headless boot paths - batch simulation and the default GUI start's
 * command-line parse - must open no network listener.
 *
 * <p>{@link SocketConfinementRatchetTest} (source-level) and
 * {@code ArchitectureRulesTest.socketEndpointsAreConfinedToCollabNet}
 * (bytecode-level) pin structurally that socket construction lives only
 * under {@code jls.collab.net}; today the only bind sites there
 * ({@code SessionListener}, {@code SocketSession}) are never referenced
 * from a boot path, so nothing binds during startup. This test adds the
 * missing behavioural pin: it drives the real headless boot cores and
 * observes that no LISTEN socket appears.</p>
 *
 * <p>Two independent assertions guard each path. The portable one is an
 * {@link org.junit.jupiter.api.Assertions#assertTimeoutPreemptively
 * assertTimeoutPreemptively}: a future regression that put an
 * {@code accept()} in the boot path would block forever and time out.
 * The precise one, Linux-gated, cross-references this JVM's socket file
 * descriptors ({@code /proc/self/fd}) with the LISTEN rows of
 * {@code /proc/self/net/tcp} and {@code tcp6}, snapshotting the set of
 * listening sockets this process owns before and after the boot path and
 * asserting boot introduced none. On a platform without {@code /proc}
 * the precise assertion is skipped and the timeout remains the pin.</p>
 *
 * <p>By design this test drives only the safe headless cores:
 * {@link BatchSimulator#runSim()} (the batch boot core) and
 * {@link JLSStart#parseCommandLine(String[])} (the GUI-branch selector).
 * It never calls {@code JLSStart.start()} or {@code new JLSStart()},
 * which call {@link System#exit} and, on the GUI branch, throw
 * {@code HeadlessException}; the window's socket-freedom is already
 * covered by the confinement ratchets and its display path is out of
 * this slice's scope.</p>
 */
class BootListenerHygieneTest {

    /** The Linux LISTEN state code in {@code /proc/net/tcp}'s st field. */
    private static final String TCP_LISTEN = "0A";

    /**
     * Build a tiny circuit in the on-disk text format - one constant
     * wired to a watched output pin - exactly as the editor saves it,
     * mirroring {@code BatchSimulationGoldenTest}'s CircuitBuilder idiom.
     *
     * @return the circuit text.
     */
    private static String tinyCircuitText() {

        StringBuilder text = new StringBuilder("CIRCUIT hygiene\n");
        // constant, id 0
        text.append("ELEMENT Constant\n")
            .append(" int id 0\n")
            .append(" int x 60\n int y 60\n")
            .append(" int width 24\n int height 24\n")
            .append(" Int value 1\n")
            .append(" int base 10\n")
            .append(" String orient \"RIGHT\"\n")
            .append("END\n");
        // output pin, id 1
        text.append("ELEMENT OutputPin\n")
            .append(" int id 1\n")
            .append(" int x 480\n int y 120\n")
            .append(" int width 48\n int height 24\n")
            .append(" String name \"out\"\n")
            .append(" int bits 1\n")
            .append(" int watch 1\n")
            .append(" String orient \"RIGHT\"\n")
            .append("END\n");
        // wire the constant's output to the pin's input as two ends
        appendWireEnd(text, 2, 0, "output", 3);
        appendWireEnd(text, 3, 1, "input", 2);
        return text.append("ENDCIRCUIT\n").toString();
    } // end of tinyCircuitText method

    /**
     * Append one attached wire end, as the editor saves it.
     *
     * @param text     The circuit text being built.
     * @param id       This wire end's element id.
     * @param attachTo The element id this end attaches to.
     * @param put      The put name on that element.
     * @param otherEnd The id of the wire's other end.
     */
    private static void appendWireEnd(StringBuilder text, int id,
            int attachTo, String put, int otherEnd) {

        text.append("ELEMENT WireEnd\n")
            .append(" int id ").append(id).append('\n')
            .append(" int x ").append(12 * id).append('\n')
            .append(" int y 240\n")
            .append(" int width 8\n int height 8\n")
            .append(" String put \"").append(put).append("\"\n")
            .append(" ref attach ").append(attachTo).append('\n')
            .append(" ref wire ").append(otherEnd).append('\n')
            .append("END\n");
    } // end of appendWireEnd method

    @Test
    void batchModeOpensNoListener() throws Exception {

        Circuit circuit = new Circuit("hygiene");
        assertTrue(circuit.load(new Scanner(tinyCircuitText())),
                () -> "load failed: " + JLSInfo.loadError);
        assertTrue(circuit.finishLoad(null),
                () -> "finishLoad failed: " + JLSInfo.loadError);

        BatchSimulator sim = new BatchSimulator();
        sim.setCircuit(circuit);
        sim.setTimeLimit(1_000_000);

        Set<Long> before = listeningSocketInodes();
        // a future accept()-in-boot regression would block here and the
        // preemptive timeout would fail the test; the real batch core
        // runs to quiescence in microseconds
        assertTimeoutPreemptively(Duration.ofSeconds(30), sim::runSim,
                "batch simulation must run to quiescence without blocking"
                        + " on any network accept");
        Set<Long> after = listeningSocketInodes();

        assertNoNewListeners(before, after,
                "batch simulation boot must open no network listener");
    } // end of batchModeOpensNoListener method

    @Test
    void defaultGuiStartOpensNoListener() throws Exception {

        Path circuitFile = Files.createTempFile("jls-hygiene", ".jls");
        Files.write(circuitFile,
                tinyCircuitText().getBytes(StandardCharsets.UTF_8));

        boolean savedBatch = JLSInfo.batch;
        try {
            // start clean so the assertion proves the parse selected the
            // GUI branch, not that a prior test left the flag unset
            JLSInfo.batch = false;
            String[] args = { circuitFile.toString() };

            Set<Long> before = listeningSocketInodes();
            // parseCommandLine is the mode selector start() dispatches on;
            // driving it (never start()) exercises the default-GUI decision
            // headlessly. The timeout still guards against a parse-time bind.
            assertTimeoutPreemptively(Duration.ofSeconds(30),
                    () -> JLSStart.parseCommandLine(args),
                    "command-line parsing must not block on any network"
                            + " accept");
            Set<Long> after = listeningSocketInodes();

            assertFalse(JLSInfo.batch,
                    "no -b flag must select the GUI branch (batch == false)");
            assertNoNewListeners(before, after,
                    "default GUI start's parse must open no network listener");
        } finally {
            JLSInfo.batch = savedBatch;
            Files.deleteIfExists(circuitFile);
        }
    } // end of defaultGuiStartOpensNoListener method

    /**
     * Assert that a boot path introduced no new listening socket owned by
     * this JVM. Linux-gated: on a platform without {@code /proc/self/net}
     * the precise check is skipped (the surrounding timeout assertion
     * remains the portable pin), so the test never gives a false pass by
     * measuring nothing on a platform it cannot inspect.
     *
     * @param before The listening inodes owned before the boot path.
     * @param after  The listening inodes owned after it.
     * @param what   The message describing the pinned boot path.
     */
    private static void assertNoNewListeners(Set<Long> before,
            Set<Long> after, String what) {

        assumeTrue(procNetAvailable(),
                "the /proc listener probe needs Linux /proc/self/net");
        Set<Long> introduced = new TreeSet<Long>(after);
        introduced.removeAll(before);
        assertEquals(Set.of(), introduced, what
                + " (new LISTEN socket inodes owned by this JVM: "
                + introduced + ")");
    } // end of assertNoNewListeners method

    /**
     * Whether this platform exposes the {@code /proc} tables the precise
     * probe reads.
     *
     * @return true on Linux with {@code /proc/self/net/tcp}.
     */
    private static boolean procNetAvailable() {

        return Files.exists(Path.of("/proc/self/net/tcp"));
    } // end of procNetAvailable method

    /**
     * The inodes of the LISTEN sockets this JVM currently owns: the
     * intersection of the socket inodes named by {@code /proc/self/fd}
     * and the inodes of the LISTEN rows in {@code /proc/self/net/tcp} and
     * {@code tcp6}. An empty set on a non-{@code /proc} platform, where
     * the precise assertion is skipped anyway.
     *
     * @return the owned listening-socket inodes.
     *
     * @throws IOException if a {@code /proc} table cannot be read.
     */
    private static Set<Long> listeningSocketInodes() throws IOException {

        if (!procNetAvailable()) {
            return Set.of();
        }
        Set<Long> ownFdInodes = ownSocketFdInodes();
        Set<Long> listening = new HashSet<Long>();
        for (String table : List.of("/proc/self/net/tcp",
                "/proc/self/net/tcp6")) {
            Path path = Path.of(table);
            if (!Files.exists(path)) {
                continue;
            }
            for (String line : Files.readAllLines(path)) {
                String[] cols = line.trim().split("\\s+");
                // sl local rem st ... uid timeout inode : st is col 3,
                // inode is col 9; the header row has no numeric st
                if (cols.length < 10 || !TCP_LISTEN.equals(cols[3])) {
                    continue;
                }
                try {
                    long inode = Long.parseLong(cols[9]);
                    if (ownFdInodes.contains(inode)) {
                        listening.add(inode);
                    }
                } catch (NumberFormatException notARow) {
                    // header or malformed row; skip
                }
            }
        }
        return listening;
    } // end of listeningSocketInodes method

    /**
     * The socket inodes this JVM holds open, read from the
     * {@code socket:[inode]} targets of {@code /proc/self/fd} symlinks.
     *
     * @return the socket inodes of this process's open file descriptors.
     *
     * @throws IOException if the fd directory cannot be listed.
     */
    private static Set<Long> ownSocketFdInodes() throws IOException {

        Set<Long> inodes = new HashSet<Long>();
        Path fdDir = Path.of("/proc/self/fd");
        if (!Files.isDirectory(fdDir)) {
            return inodes;
        }
        try (Stream<Path> fds = Files.list(fdDir)) {
            fds.forEach(fd -> {
                try {
                    String target = Files.readSymbolicLink(fd).toString();
                    if (target.startsWith("socket:[")
                            && target.endsWith("]")) {
                        inodes.add(Long.parseLong(target.substring(
                                "socket:[".length(),
                                target.length() - 1)));
                    }
                } catch (IOException | NumberFormatException vanished) {
                    // the fd closed between listing and reading, or its
                    // target was not a plain socket inode; skip it
                }
            });
        }
        return inodes;
    } // end of ownSocketFdInodes method

} // end of BootListenerHygieneTest class
