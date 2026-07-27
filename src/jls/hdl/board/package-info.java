/**
 * Board-aware HDL export (issue #213): emits the pin-constraint file
 * that maps an exported design's top-level ports to physical FPGA pins
 * on a named development board, so the export is one toolchain command
 * from a bitstream. {@link jls.hdl.board.Board} models a board as a
 * name, a constraint-file format, and a table of named pins;
 * {@link jls.hdl.board.Boards} is the small built-in board table
 * (deliberately small — hypothesis H2 of #213 caps it so JLS never
 * grows a general board-description language).
 * {@link jls.hdl.board.PinBindings} reads the user's port-to-pin
 * bindings file, and {@link jls.hdl.board.PcfEmitter} walks the same
 * {@link jls.hdl.HdlModel} port set the HDL emitters render and writes
 * icestorm/nextpnr-ice40 PCF text. Binding problems are all-or-nothing:
 * every unbindable port is reported in one
 * {@link jls.hdl.HdlExportException} and no constraint text is
 * produced, so a partial or invalid constraint file can never reach
 * disk. Like the rest of {@code jls.hdl}, everything here is headless
 * and reached only through the {@code jls.JLSStart} CLI wiring point
 * ({@code -export} plus {@code -board}/{@code -pins}).
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated { }, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.hdl.board;

import org.jspecify.annotations.NullMarked;
