// Header-scanner corpus (issue #63): Verilog-1995 non-ANSI port style,
// names in the header and directions declared in the body, with body
// parameters used in port widths and a function whose input is no port.
module nonansi_counter (clk, rst, en, count, tc);

	parameter WIDTH = 4;
	parameter MAX = 2 ** WIDTH - 1;

	input clk, rst, en;
	output [WIDTH-1:0] count;
	output tc;

	reg [WIDTH-1:0] count;

	// a function input must never be mistaken for a port
	function [WIDTH-1:0] bump;
		input [WIDTH-1:0] value;
		begin
			bump = value + 1'b1;
		end
	endfunction

	assign tc = (count == MAX);

	always @(posedge clk) begin
		if (rst)
			count <= {WIDTH{1'b0}};
		else if (en)
			count <= bump(count);
	end

endmodule
