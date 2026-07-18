// Header-scanner corpus (issue #63): the minimal preprocessor subset -
// object-like `define (including macros built from macros), `ifdef with
// an `else branch, and ignorable directives.
`timescale 1ns / 1ps
`default_nettype wire

`define BYTE 8
`define WORD (2 * `BYTE)

`ifdef WIDE
`define BUS_BITS `WORD
`else
`define BUS_BITS `BYTE
`endif

module macro_widths (
	input  [`BUS_BITS-1:0] d,
	output [`BUS_BITS-1:0] q
);

	assign q = d;

endmodule
