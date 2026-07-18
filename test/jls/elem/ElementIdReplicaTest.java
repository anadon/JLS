package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Replica id resolution and from-scratch save reproducibility (issue
 * #183). The replica half of every fresh stable id used to be a random
 * UUID drawn once per JVM, so a circuit built from scratch and saved -
 * never round-tripped through a file - produced different sid bytes on
 * every process. The replica now resolves as: explicit override
 * ({@code jls.replicaId} property / {@code JLS_REPLICA_ID} env) first,
 * then the per-install persisted value, then a fresh draw that is
 * persisted for the next start. From-scratch saves are thereby
 * reproducible per install and byte-pinnable under the override; the
 * loaded-file path (which re-emits persisted sids) is untouched.
 */
class ElementIdReplicaTest {

	@TempDir
	Path tmp;

	/** A one-constant legacy circuit whose element we copy from. */
	private static final String ONE_CONSTANT =
			"CIRCUIT one\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n"
			+ " Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n"
			+ "ENDCIRCUIT\n";

	private Path replicaFile() {
		return tmp.resolve("jls").resolve("replica-id");
	}

	@Test
	void overrideWinsOverEverything() throws Exception {
		Files.createDirectories(replicaFile().getParent());
		Files.writeString(replicaFile(), "persisted0\n",
				StandardCharsets.UTF_8);
		assertEquals("override0", ElementId.resolveReplica("override0",
				"envvalue0", replicaFile()),
				"the system-property override must win");
	}

	@Test
	void environmentBeatsPersistedConfig() throws Exception {
		Files.createDirectories(replicaFile().getParent());
		Files.writeString(replicaFile(), "persisted0\n",
				StandardCharsets.UTF_8);
		assertEquals("envvalue0", ElementId.resolveReplica(null,
				"envvalue0", replicaFile()),
				"the environment override must beat persisted config");
	}

	@Test
	void persistedReplicaIsReadBack() throws Exception {
		Files.createDirectories(replicaFile().getParent());
		Files.writeString(replicaFile(), "persisted0\n",
				StandardCharsets.UTF_8);
		assertEquals("persisted0",
				ElementId.resolveReplica(null, null, replicaFile()),
				"the persisted per-install replica must be reused");
	}

	@Test
	void freshDrawIsPersistedAndReused() throws Exception {
		String first = ElementId.resolveReplica(null, null, replicaFile());
		assertTrue(first.matches("[0-9a-f]{32}"),
				"a fresh draw is 32 hex digits, got '" + first + "'");
		assertTrue(Files.isRegularFile(replicaFile()),
				"the fresh draw must be persisted for the next start");
		assertEquals(first,
				ElementId.resolveReplica(null, null, replicaFile()),
				"the next start must reuse the persisted draw - that is "
						+ "what makes one install's from-scratch saves "
						+ "reproducible run-to-run");
	}

	@Test
	void invalidCandidatesAreSkipped() throws Exception {
		Files.createDirectories(replicaFile().getParent());
		Files.writeString(replicaFile(), "NOT/valid!\n",
				StandardCharsets.UTF_8);
		// invalid override and env (bad charset), invalid persisted
		// value, and the reserved "legacy" replica must all be skipped
		String resolved = ElementId.resolveReplica("UPPER", "legacy",
				replicaFile());
		assertTrue(resolved.matches("[0-9a-f]{32}"),
				"invalid candidates must fall through to a fresh draw, "
						+ "got '" + resolved + "'");
		assertNotEquals("legacy", resolved,
				"the reserved legacy replica must never be minted fresh");
	}

	@Test
	void pinnedConstructionMintsReproducibleIds() throws Exception {
		List<String> first = mintThreePinned("feedface");
		List<String> second = mintThreePinned("feedface");
		assertEquals(first, second,
				"two identically-pinned construction runs must mint "
						+ "identical ids - the in-process image of two "
						+ "JVMs started with the same jls.replicaId");
		assertEquals("feedface:0", first.get(0),
				"pinned mints must start at the pinned counter");
	}

	/** Mint three ids under a pinned replica, as one 'process' would. */
	private static List<String> mintThreePinned(String replica)
			throws Exception {
		try (AutoCloseable pin = ElementId.pinForTesting(replica, 0)) {
			return List.of(ElementId.mintFresh().toString(),
					ElementId.mintFresh().toString(),
					ElementId.mintFresh().toString());
		}
	}

	@Test
	void pinnedFromScratchSavesAreByteIdentical() throws Exception {
		// two 'processes' with the same pinned replica build the same
		// circuit from never-persisted elements and save
		String first = buildFromScratchAndSave("feedface");
		String second = buildFromScratchAndSave("feedface");
		assertEquals(first, second,
				"identically-pinned from-scratch constructions must "
						+ "save byte-identically");
		assertTrue(first.contains(" String sid \"feedface:0\""),
				"the fresh element's sid must carry the pinned replica, "
						+ "saved:\n" + first);

		// and two installs (different replicas) legitimately differ -
		// identity is per-install, deliberately not content-derived
		assertNotEquals(first, buildFromScratchAndSave("cafebabe"),
				"different installs must keep distinct identities");
	}

	/**
	 * Copy a loaded constant into a new circuit under a pinned replica
	 * and save. The copy mints a fresh id, so the saved sid takes the
	 * pinned replica - the from-scratch path of issue #183.
	 */
	private static String buildFromScratchAndSave(String replica)
			throws Exception {
		Circuit source = new Circuit("one");
		assertTrue(source.load(new Scanner(ONE_CONSTANT)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(source.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		Element original = source.getElements().iterator().next();

		try (AutoCloseable pin = ElementId.pinForTesting(replica, 0)) {
			Element fresh = original.copy();
			Circuit scratch = new Circuit("scratch");
			scratch.addElement(fresh);
			StringWriter out = new StringWriter();
			try (PrintWriter writer = new PrintWriter(out)) {
				scratch.save(writer);
			}
			return out.toString();
		}
	}
}
