package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Package-documentation completeness ratchet: every package in the
 * source and test trees carries a {@code package-info.java}. The #186
 * documentation pass gave every package one; this keeps a new package
 * from shipping without (which happened to {@code jls.collab.op},
 * created between that pass's snapshot and its merge). Zero-tolerance:
 * there is no exemption list - a package with code gets a package
 * comment.
 */
class PackageInfoRatchetTest {

	@Test
	void everyPackageHasPackageInfo() throws IOException {
		TreeSet<String> missing = new TreeSet<String>();
		for (Path root : List.of(Path.of("src"), Path.of("test"))) {
			try (Stream<Path> walk = Files.walk(root)) {
				walk.filter(Files::isDirectory)
						.filter(PackageInfoRatchetTest::hasJavaFiles)
						.filter(dir -> !Files.exists(
								dir.resolve("package-info.java")))
						.forEach(dir -> missing.add(dir.toString()));
			}
		}
		assertTrue(missing.isEmpty(),
				"packages without package-info.java - document what the"
						+ " package is for: " + missing);
	}

	private static boolean hasJavaFiles(Path dir) {
		try (Stream<Path> files = Files.list(dir)) {
			return files.anyMatch(f -> f.toString().endsWith(".java")
					&& !f.getFileName().toString()
							.equals("package-info.java"));
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

} // end of PackageInfoRatchetTest class
