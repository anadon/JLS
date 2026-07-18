package jls.hdl.yosys;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Yosys version, parsed from the banner {@code yosys -V} prints
 * (issue #61). Yosys versions itself as {@code major.minor}, with an
 * optional {@code +commits} suffix on builds past a release tag; a
 * full banner looks like {@code "Yosys 0.38+92 (git sha1 ...)"}. The
 * importer needs this for exactly one decision: is the installed
 * Yosys at least {@link #MINIMUM}? The dependency dialog reports the
 * detected version verbatim, so an unparseable banner is returned as
 * absence, never an exception (addendum P4: the missing/too-old path
 * must produce a dialog, not a stack trace).
 *
 * @param major The major version (0 for every Yosys to date).
 * @param minor The minor version (e.g. 38).
 * @param commits The commits-past-release count from a {@code +n}
 * suffix, or 0 for an exact release.
 */
public record YosysVersion(int major, int minor, int commits)
		implements Comparable<YosysVersion> {

	/**
	 * The oldest Yosys the fixed pass pipeline is pinned against
	 * (issue #61 addendum names 0.38): {@code $bmux} emission,
	 * {@code $mem_v2}, and {@code dffunmap} behavior are all
	 * comfortably stable by then, and CI pins the same floor.
	 */
	public static final YosysVersion MINIMUM =
			new YosysVersion(0, 38, 0);

	/**
	 * Recognizes a version inside a banner: an optional word
	 * "Yosys", then major.minor and an optional +commits.
	 */
	private static final Pattern BANNER = Pattern.compile(
			"(?:^|\\s)(?:Yosys\\s+)?([0-9]{1,6})\\.([0-9]{1,6})"
					+ "(?:\\+([0-9]{1,8}))?");

	/**
	 * Extracts the version from {@code yosys -V} output (or a bare
	 * version string).
	 *
	 * @param banner The text to scan; null is treated as empty.
	 *
	 * @return the version, or empty when no version is recognizable
	 * in the text.
	 */
	public static Optional<YosysVersion> parse(String banner) {

		if (banner == null) {
			return Optional.empty();
		}
		Matcher m = BANNER.matcher(banner);
		if (!m.find()) {
			return Optional.empty();
		}
		int major = Integer.parseInt(m.group(1));
		int minor = Integer.parseInt(m.group(2));
		int commits = m.group(3) == null ? 0
				: Integer.parseInt(m.group(3));
		return Optional.of(new YosysVersion(major, minor, commits));
	} // end of parse method

	/**
	 * Whether this version satisfies a floor.
	 *
	 * @param floor The required minimum.
	 *
	 * @return true when this version is the floor or newer.
	 */
	public boolean atLeast(YosysVersion floor) {

		return compareTo(floor) >= 0;
	} // end of atLeast method

	@Override
	public int compareTo(YosysVersion other) {

		if (major != other.major) {
			return Integer.compare(major, other.major);
		}
		if (minor != other.minor) {
			return Integer.compare(minor, other.minor);
		}
		return Integer.compare(commits, other.commits);
	} // end of compareTo method

	@Override
	public String toString() {

		String base = major + "." + minor;
		return commits == 0 ? base : base + "+" + commits;
	} // end of toString method

} // end of YosysVersion record
