/**
 * Imports synthesizable Verilog into JLS by delegating parsing and
 * elaboration to an external Yosys (issue #61, Stage 2 of the #59 HDL
 * path). JLS never parses Verilog itself: Yosys runs a fixed pass
 * pipeline that legalizes the design into a restricted ~15-cell set
 * and writes a JSON netlist, and this package consumes that file.
 * {@link jls.hdl.yosys.YosysLocator} and
 * {@link jls.hdl.yosys.YosysVersion} detect the user-installed
 * dependency; {@link jls.hdl.yosys.JsonValue} and
 * {@link jls.hdl.yosys.YosysNetlist} read the {@code write_json}
 * output into a typed model; {@link jls.hdl.yosys.CellValidator}
 * checks every cell against the restricted set, collecting a
 * {@link jls.hdl.yosys.CellViolation} — with the Verilog source
 * location and a teaching message — for each construct outside it.
 * Rejection is all-or-nothing and happens before any circuit is
 * built. Malformed netlist files raise
 * {@link jls.hdl.yosys.NetlistFormatException}. The pieces still to
 * come, in dependency order: the pinned pass pipeline runner
 * (subprocess, off-EDT, cancellable), the {@code jls_map.v} techmap
 * library, the cell-to-element mapper with its Splitter/Binder mesh,
 * layout (issue #62), and the {@code File → Import} wiring. Like the
 * rest of {@code jls.hdl}, everything here is headless.
 */
package jls.hdl.yosys;
