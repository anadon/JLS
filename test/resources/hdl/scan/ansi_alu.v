// Header-scanner corpus (issue #63): ANSI port style with parameters,
// $clog2 in a width, shared direction across commas, and an inout.
module ansi_alu #(
	parameter WIDTH = 8,
	parameter DEPTH = 16,
	parameter ADDR = $clog2(DEPTH)
) (
	input  wire [WIDTH-1:0] a, b,
	input  wire [ADDR-1:0]  sel,
	input  wire             carry_in,
	output reg  [WIDTH-1:0] result,
	output wire             carry_out,
	inout  wire [7:0]       debug_bus
);

	/* the body is irrelevant to the header scanner,
	   but keeps the file legal for yosys ground truth */
	always @* begin
		result = a + b + carry_in + {{(WIDTH-1){1'b0}}, sel[0]};
	end
	assign carry_out = result[WIDTH-1];

endmodule
