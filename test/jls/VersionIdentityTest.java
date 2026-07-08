package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The running app carries exactly one version identity, single-sourced
 * from the pom via a filtered resource (issue #36, audit finding P1):
 * JLSInfo used to hardcode 4.1 "built on March 18, 2014" while the pom
 * said 4.2.0-SNAPSHOT.
 */
class VersionIdentityTest {

	@Test
	void versionStringIsSemverOrDev() {
		String v = JLSInfo.versionString;
		assertTrue(v != null && !v.isEmpty());
		assertTrue(v.matches("\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9.]+)?") || v.equals("dev"),
				"unexpected version string: " + v);
	}

	@Test
	void testsSeeTheFilteredPomVersion() {
		// under the Maven build the filtered resource must be present,
		// so the fallback "dev" indicates broken filtering
		assertTrue(!JLSInfo.versionString.equals("dev"),
				"jls/version.properties was not filtered into the classpath");
	}
}
