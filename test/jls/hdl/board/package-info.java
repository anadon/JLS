/**
 * Test suite for {@code jls.hdl.board}, the board-aware side of HDL
 * export (issue #213): the built-in board table, the port-to-pin
 * bindings parser, and the PCF constraint emitter. Correctness is
 * pinned three ways: a byte-for-byte golden PCF for a small sequential
 * circuit bound to the iCEstick ({@code test/resources/hdl/board}),
 * unbindable-port tests proving every binding problem is reported
 * together and no constraint text is produced (prediction P3 of #213),
 * and end-to-end CLI tests of the {@code -board}/{@code -pins} export
 * operands showing the constraint file lands next to the HDL on
 * success and that a failed binding writes nothing at all. The shared
 * fixture builds its circuit in the on-disk text format through the
 * real loader, like the {@code jls.hdl} suite it extends.
 */
package jls.hdl.board;
