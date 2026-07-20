package jls.hdl.imp;

/**
 * The product of a successful {@link NetlistImporter} run (issue #61):
 * the JLS plain-text save format for the imported circuit, ready to
 * load through the real loader, plus the {@link ImportSummary} the
 * post-import report renders. The save text realizes the placed layout
 * (issue #62) as {@code ELEMENT} blocks and {@code WireEnd} chains, so
 * loading it and re-saving yields a circuit indistinguishable from a
 * hand-drawn one.
 */
public final class ImportResult {

	/** The JLS plain-text circuit save format. */
	private final String saveText;

	/** The mapping and coercion report. */
	private final ImportSummary summary;

	/**
	 * Creates the result.
	 *
	 * @param saveText The JLS save-format text.
	 * @param summary The import summary.
	 */
	ImportResult(String saveText, ImportSummary summary) {

		this.saveText = saveText;
		this.summary = summary;
	} // end of constructor

	/**
	 * The imported circuit in JLS plain-text save format.
	 *
	 * @return the save-format text.
	 */
	public String saveText() {

		return saveText;
	} // end of saveText method

	/**
	 * The mapping and coercion report for the post-import dialog.
	 *
	 * @return the summary.
	 */
	public ImportSummary summary() {

		return summary;
	} // end of summary method

} // end of ImportResult class
