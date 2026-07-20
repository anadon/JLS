// jls_map.v - the JLS import techmap library (issue #61, §6).
//
// Yosys' restricted import pipeline
//
//   read_verilog; hierarchy -auto-top; proc; opt_clean; memory -nomap;
//   wreduce -memx; opt; dffunmap; pmuxtree; techmap -map jls_map.v;
//   opt_clean; write_json
//
// legalizes a synthesizable design into the ~15-cell set JLS' fixed
// teaching element vocabulary can represent. A handful of word-level
// operator cells have no direct JLS element and no built-in Yosys
// lowering into the restricted set; this library rewrites each into
// cells that ARE in CellValidator's SUPPORTED set ($not, $and, $or,
// $xor and the $reduce_* family), so nothing downstream ever sees an
// $xnor / $eq / $ne. It is authored here and validated in CI by
// ImportPipelineTest, which runs the full pipeline under a real Yosys
// and asserts the emitted netlist contains only mappable cells.
//
// Each rule reproduces the cell's exact function; signedness is carried
// through so width extension inside the surviving $xor stays correct.

// xnor(A,B) = ~(A ^ B), bit for bit.
(* techmap_celltype = "$xnor" *)
module _jls_techmap_xnor (A, B, Y);
	parameter A_SIGNED = 0;
	parameter B_SIGNED = 0;
	parameter A_WIDTH = 1;
	parameter B_WIDTH = 1;
	parameter Y_WIDTH = 1;
	input [A_WIDTH-1:0] A;
	input [B_WIDTH-1:0] B;
	output [Y_WIDTH-1:0] Y;
	wire [Y_WIDTH-1:0] xored;
	\$xor #(
		.A_SIGNED(A_SIGNED), .B_SIGNED(B_SIGNED),
		.A_WIDTH(A_WIDTH), .B_WIDTH(B_WIDTH), .Y_WIDTH(Y_WIDTH)
	) stage_xor (.A(A), .B(B), .Y(xored));
	\$not #(
		.A_SIGNED(0), .A_WIDTH(Y_WIDTH), .Y_WIDTH(Y_WIDTH)
	) stage_not (.A(xored), .Y(Y));
endmodule

// ne(A,B) = | (A ^ B): any differing bit makes the result 1.
(* techmap_celltype = "$ne" *)
module _jls_techmap_ne (A, B, Y);
	parameter A_SIGNED = 0;
	parameter B_SIGNED = 0;
	parameter A_WIDTH = 1;
	parameter B_WIDTH = 1;
	parameter Y_WIDTH = 1;
	localparam WIDTH = A_WIDTH > B_WIDTH ? A_WIDTH : B_WIDTH;
	input [A_WIDTH-1:0] A;
	input [B_WIDTH-1:0] B;
	output [Y_WIDTH-1:0] Y;
	wire [WIDTH-1:0] xored;
	\$xor #(
		.A_SIGNED(A_SIGNED), .B_SIGNED(B_SIGNED),
		.A_WIDTH(A_WIDTH), .B_WIDTH(B_WIDTH), .Y_WIDTH(WIDTH)
	) stage_xor (.A(A), .B(B), .Y(xored));
	\$reduce_or #(
		.A_SIGNED(0), .A_WIDTH(WIDTH), .Y_WIDTH(Y_WIDTH)
	) stage_or (.A(xored), .Y(Y));
endmodule

// eq(A,B) = ~ne(A,B) = ~ | (A ^ B).
(* techmap_celltype = "$eq" *)
module _jls_techmap_eq (A, B, Y);
	parameter A_SIGNED = 0;
	parameter B_SIGNED = 0;
	parameter A_WIDTH = 1;
	parameter B_WIDTH = 1;
	parameter Y_WIDTH = 1;
	localparam WIDTH = A_WIDTH > B_WIDTH ? A_WIDTH : B_WIDTH;
	input [A_WIDTH-1:0] A;
	input [B_WIDTH-1:0] B;
	output [Y_WIDTH-1:0] Y;
	wire [WIDTH-1:0] xored;
	wire differ;
	\$xor #(
		.A_SIGNED(A_SIGNED), .B_SIGNED(B_SIGNED),
		.A_WIDTH(A_WIDTH), .B_WIDTH(B_WIDTH), .Y_WIDTH(WIDTH)
	) stage_xor (.A(A), .B(B), .Y(xored));
	\$reduce_or #(
		.A_SIGNED(0), .A_WIDTH(WIDTH), .Y_WIDTH(1)
	) stage_or (.A(xored), .Y(differ));
	\$not #(
		.A_SIGNED(0), .A_WIDTH(1), .Y_WIDTH(Y_WIDTH)
	) stage_not (.A(differ), .Y(Y));
endmodule
