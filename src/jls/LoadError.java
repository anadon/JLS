package jls;

/**
 * A structured description of a single circuit-load failure (issue #58
 * addendum). Every failure carries four parts a reader can act on:
 * a category (why, from a small fixed taxonomy tests assert on), the
 * most specific location known (line number and/or element), a
 * human-readable detail, and one actionable next step.
 *
 * The rendered text is published through {@link JLSInfo#setLoadError},
 * which keeps the legacy {@link JLSInfo#loadError} string in sync as a
 * derived view, so every existing front end (GUI dialogs, the batch
 * "jls: error:" contract, import, checkpoint recovery) shows the same
 * message without change.
 */
public final class LoadError {

	/**
	 * The fixed failure taxonomy. Tests assert on these labels, keeping
	 * the detail wording free to improve (issue #58 section 10).
	 */
	public enum Category {

		/** The stream died mid-read (Scanner swallows IOException). */
		IO_ERROR("I/O error"),

		/** The content was never a JLS circuit save at all. */
		NOT_A_CIRCUIT("not a circuit file"),

		/** A circuit save that stops making sense partway through. */
		MALFORMED("malformed file"),

		/**
		 * The file's FORMAT header declares a save-format version newer
		 * than this JLS understands (issue #79) - the file itself is
		 * fine, the reader is too old.
		 */
		NEWER_FORMAT("file needs a newer JLS"),

		/**
		 * An element type this version of JLS does not know
		 * (version/format mismatch in the issue's terms).
		 */
		UNKNOWN_ELEMENT("unknown element"),

		/** A known element rejected its own parameter values. */
		ELEMENT_ERROR("invalid element"),

		/** The file blows past a hostile-input cap (issue #38). */
		LIMIT_EXCEEDED("expansion limit exceeded");

		private final String label;

		/**
		 * Bind a category to its user-visible label.
		 *
		 * @param label the fixed text shown for this category.
		 */
		Category(String label) {
			this.label = label;
		}

		/**
		 * The user-visible name of this category.
		 *
		 * @return the label, e.g. "malformed file".
		 *
		 * @jls.testedby jls.elem.OrientationGeometryTest#errorCategory()
		 */
		public String label() {
			return label;
		}
	} // end of Category enum

	private final Category category;
	private final String detail;
	private final int line;
	private final String element;
	private final String hint;

	/**
	 * Create a load error.
	 *
	 * @param category Which kind of failure this is (never null).
	 * @param detail   What went wrong, in words a student can read.
	 * @param line     The 1-based line the loader had reached, or 0 if
	 *                 no line is known (e.g. the file never opened).
	 * @param element  The element being read, e.g. "'mem' (Memory)", or
	 *                 null if no element is in play.
	 * @param hint     One actionable next-step sentence, or null.
	 */
	public LoadError(Category category, String detail, int line,
			String element, String hint) {

		this.category = category;
		this.detail = detail;
		this.line = line;
		this.element = element;
		this.hint = hint;
	} // end of constructor

	/**
	 * Create a load error with no line or element context.
	 *
	 * @param category Which kind of failure this is.
	 * @param detail   What went wrong.
	 * @param hint     One actionable next-step sentence, or null.
	 *
	 * @return the new load error.
	 */
	public static LoadError of(Category category, String detail, String hint) {

		return new LoadError(category, detail, 0, null, hint);
	} // end of of method

	/**
	 * Get which kind of failure this is.
	 *
	 * @return the failure category.
	 *
	 * @jls.testedby jls.FormatHeaderTest#malformedFormatVersionFailsAsMalformed()
	 * @jls.testedby jls.FormatHeaderTest#newerFormatVersionFailsAsNeedsNewerJls()
	 * @jls.testedby jls.FormatHeaderTest#truncatedFormatHeaderFailsAsMalformed()
	 * @jls.testedby jls.LoadErrorReportingTest#badElementParameterNamesElementLineAndConstraint()
	 * @jls.testedby jls.LoadErrorReportingTest#garbageBytesAreReportedAsNotACircuitFile()
	 * @jls.testedby jls.LoadErrorReportingTest#midStreamIOExceptionIsReportedAsIOError()
	 * @jls.testedby jls.LoadErrorReportingTest#nonIntegerAttributeValueReportsAttributeAndLine()
	 * @jls.testedby jls.LoadErrorReportingTest#truncatedFileNamesCategoryLineElementAndNextStep()
	 * @jls.testedby jls.LoadErrorReportingTest#unknownElementTagNamesTagCategoryAndUpgradeHint()
	 * @jls.testedby jls.elem.OrientationGeometryTest#errorCategory()
	 */
	public Category getCategory() {

		return category;
	} // end of getCategory method

	/**
	 * Get the human-readable detail of the failure.
	 *
	 * @return what went wrong, in words.
	 */
	public String getDetail() {

		return detail;
	} // end of getDetail method

	/**
	 * Get the line number where the failure hit.
	 *
	 * @return the 1-based line the loader had reached, or 0 if unknown.
	 *
	 * @jls.testedby jls.LoadErrorReportingTest#truncatedFileNamesCategoryLineElementAndNextStep()
	 */
	public int getLine() {

		return line;
	} // end of getLine method

	/**
	 * Get the element the loader was reading, if any.
	 *
	 * @return the element being read when the failure hit, or null.
	 */
	public String getElement() {

		return element;
	} // end of getElement method

	/**
	 * Get the actionable next step, if any.
	 *
	 * @return the next-step sentence, or null.
	 */
	public String getHint() {

		return hint;
	} // end of getHint method

	/**
	 * Render the four-part message: category (why), location (where),
	 * detail (what), and the next step. Front ends prefix the file name
	 * and operation ("can't open X: ...", "jls: error: X is not a valid
	 * circuit file: ..."), so this text starts at the category.
	 *
	 * @return the full user-visible message.
	 *
	 * @jls.testedby jls.FormatHeaderTest#malformedFormatVersionFailsAsMalformed()
	 * @jls.testedby jls.FormatHeaderTest#newerFormatVersionFailsAsNeedsNewerJls()
	 * @jls.testedby jls.StableElementIdTest#duplicateFileIdsAreRejected()
	 */
	public String render() {

		StringBuilder msg = new StringBuilder(category.label());
		if (line > 0 || element != null) {
			msg.append(" (");
			if (line > 0) {
				msg.append("line ").append(line);
			}
			if (element != null) {
				if (line > 0) {
					msg.append(", ");
				}
				msg.append("element ").append(element);
			}
			msg.append(")");
		}
		msg.append(": ").append(detail);
		if (msg.charAt(msg.length() - 1) != '.') {
			msg.append('.');
		}
		if (hint != null && !hint.isEmpty()) {
			msg.append(' ').append(hint);
		}
		return msg.toString();
	} // end of render method

	/**
	 * @return the rendered message, so the error reads well when logged or
	 *         string-concatenated.
	 */
	@Override
	public String toString() {

		return render();
	} // end of toString method

} // end of LoadError class
