package jls;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Propagates the JaCoCo agent into the JVMs the CLI tests spawn.
 *
 * The CLI suites exercise JLSStart end to end through real
 * {@code java -cp ... jls.JLS} subprocesses, but the coverage agent
 * only instruments the surefire JVM - so everything those tests
 * actually execute used to read as uncovered (JLSStart measured 4.7%
 * despite the whole flag table being driven). Adding this JVM's own
 * agent argument to the child command line makes the measurement
 * honest; the agent appends to the shared exec file with file
 * locking, so concurrent child JVMs are safe.
 *
 * When the tests run without JaCoCo (plain IDE runs), there is no
 * agent argument to copy and this contributes nothing.
 */
public final class CoverageAgent {

	private CoverageAgent() {
	}

	/**
	 * The JaCoCo agent argument(s) of the current JVM, if any.
	 *
	 * @return JVM arguments to add to a spawned java command line;
	 *         empty when no coverage agent is attached.
	 */
	public static List<String> jvmArgs() {

		List<String> args = new ArrayList<String>();
		for (String arg : ManagementFactory.getRuntimeMXBean()
				.getInputArguments()) {
			if (arg.startsWith("-javaagent:") && arg.contains("jacoco")) {
				args.add(arg);
			}
		}
		return args;
	}
}
