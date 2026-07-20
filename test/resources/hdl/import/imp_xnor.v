// Exercises jls_map.v: `~^` yields a $xnor cell that the techmap
// library must rewrite into $xor + $not so the emitted netlist holds
// only CellValidator-supported cells.
module imp_xnor(
	input  [3:0] a,
	input  [3:0] b,
	output [3:0] y
);
	assign y = a ~^ b;
endmodule
