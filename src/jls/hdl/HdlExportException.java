package jls.hdl;

/**
 * Thrown when a circuit contains elements the HDL exporter cannot
 * express (issue #60, §9 escalation). The message names every
 * offending element in one pass — type, name where the element has
 * one, and grid location — so the user learns the full repair job
 * from a single failure. Nothing is written when this is thrown.
 */
public class HdlExportException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates the exception with a message naming every offending element.
	 *
	 * @param message the full description of the unexportable elements.
	 */
	public HdlExportException(String message) {

		super(message);
	} // end of constructor

} // end of HdlExportException class
