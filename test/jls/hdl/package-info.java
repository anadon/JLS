/**
 * Test suite for {@code jls.hdl}, the exporter that renders JLS circuits
 * as synthesizable Verilog and VHDL (issue #60). These tests exercise the
 * end-to-end export pipeline: the shared circuit walk and its element
 * policy (skip-with-warning, whole-export rejection, and jump-alias net
 * fusion), the per-language emitters and their identifier legalization and
 * template choices, and the {@code -export} command-line flag. Correctness
 * is pinned with byte-for-byte golden files under {@code test/resources/hdl},
 * tool-free structural sanity checks over the emitted text, and optional
 * external-analyzer validation (iverilog and ghdl) that runs in CI and
 * self-skips where those toolchains are absent. Shared helpers here build
 * circuits in the on-disk text format through the real loader and assert
 * structural soundness of the generated HDL.
 */
package jls.hdl;
