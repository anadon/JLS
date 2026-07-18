/**
 * Hand-written HDL header scanners for black-box components (issue #63).
 * {@link jls.hdl.scan.VerilogHeaderScanner} reads Verilog module headers
 * (ANSI and non-ANSI port styles, parameters, a minimal
 * {@code `define}/{@code `ifdef} preprocessor) and
 * {@link jls.hdl.scan.VhdlEntityScanner} reads VHDL entity headers
 * (generics, no preprocessor); both produce
 * {@link jls.hdl.scan.ScannedModule} port lists for the HDL component
 * dialog. These are deliberately scanners, not parsers
 * (docs/hdl-support-research.md &#167;7.3): any construct outside the
 * classroom subset raises {@link jls.hdl.scan.HdlScanException} with a
 * one-line reason, and such files route to the external Yosys
 * {@code write_json} extraction path rather than growing the scanner
 * (issue #63 &#167;9). Everything here is dependency-free, headless, and
 * pure text processing.
 */
package jls.hdl.scan;
