// Combinational gate design for the issue-#61 end-to-end import leg:
// after the restricted pipeline it is $and/$or/$not word-level cells,
// which NetlistImporter maps to JLS AndGate/OrGate/NotGate + pins.
module imp_gates(
	input  [3:0] a,
	input  [3:0] b,
	input  [3:0] c,
	output [3:0] y,
	output [3:0] p
);
	assign p = a & b;          // fanout: p and the OR both read a&b
	assign y = (a & b) | ~c;
endmodule
