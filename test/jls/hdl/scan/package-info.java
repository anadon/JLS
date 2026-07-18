/**
 * Tests for the issue #63 HDL header scanners. The Verilog and VHDL
 * scanners get direct unit tests over inline sources plus a committed
 * corpus under {@code test/resources/hdl/scan/}; the corpus
 * expectations are hard-coded here and, when a {@code yosys} binary is
 * on the PATH, independently cross-checked against Yosys
 * {@code write_json} ground truth (issue #63 P2). The committed VHDL
 * export goldens double as a second scanner corpus. Rejection tests
 * pin the scanner-never-becomes-a-parser boundary: out-of-subset files
 * must fail with a one-line reason, not parse wrongly.
 */
package jls.hdl.scan;
