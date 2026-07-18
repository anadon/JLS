/**
 * Exports JLS circuits to hardware description language source text
 * (issue #60). {@link jls.hdl.HdlExporter} walks a circuit once and
 * builds a language-neutral {@link jls.hdl.HdlModel} of ports, nets and
 * per-element statements, with all identifiers legalized by
 * {@link jls.hdl.HdlNames}; an {@link jls.hdl.HdlEmitter} then renders
 * that model as deterministic, byte-for-byte source, currently Verilog
 * ({@link jls.hdl.VerilogEmitter}) or VHDL ({@link jls.hdl.VhdlEmitter}).
 * All circuit-walking policy lives in the exporter and all syntax in the
 * emitters, so a new target language is added without touching the model
 * or the walker. Circuits containing elements the exporter cannot express
 * raise {@link jls.hdl.HdlExportException} and produce no output.
 */
package jls.hdl;
