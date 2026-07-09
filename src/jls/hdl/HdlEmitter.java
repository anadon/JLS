package jls.hdl;

/**
 * Renders an {@link HdlModel} as source text in one hardware
 * description language (issue #60). {@link VerilogEmitter} is the
 * first implementation; a VHDL emitter slots in here without touching
 * the model or the circuit walker.
 */
public interface HdlEmitter {

	/**
	 * Render the model as complete HDL source text. The output must be
	 * deterministic: same model, same bytes.
	 *
	 * @param model The model to render.
	 *
	 * @return the source text.
	 */
	String emit(HdlModel model);

	/**
	 * The conventional file extension for this language, without the
	 * dot (e.g. "v").
	 *
	 * @return the extension.
	 */
	String fileExtension();

} // end of HdlEmitter interface
