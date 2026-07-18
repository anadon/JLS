/**
 * Test suite for {@code jls.hdl.yosys}, the Verilog-import front end
 * that consumes external Yosys {@code write_json} netlists (issue
 * #61). These tests pin the strict JSON subset parser (syntax errors
 * with line/column, integer-only numbers, duplicate-key and
 * nesting-depth rejection), the typed netlist model (ports, cells,
 * bit-vector connections with 0/1/x/z constants, both numeric
 * parameter encodings), the restricted-cell validator and its
 * teachable reject messages (async reset, set/reset storage, wide
 * arithmetic, clocked/multi-port memories, pipeline leftovers), and
 * the dependency-detection primitives (version-banner parsing and
 * PATH search). Everything runs tool-free: fixtures are hand-written
 * netlist JSON, so no Yosys installation is required.
 */
package jls.hdl.yosys;
