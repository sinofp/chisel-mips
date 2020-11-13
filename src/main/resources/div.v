module div (
    input         clk,
    input         start,
    input         sign,
    input  [31:0] dividend,
    input  [31:0] divider,
    output        ready,
    output [31:0] quotient,
    output [31:0] remainder
);

  reg [63:0] dividend_copy, divider_copy, diff;
  reg [31:0] quotient_u;
  wire not_same_sign = dividend[31] ^ divider[31];
  // 被除数和除数符号不同的情况下，将商的符号求反
  assign quotient  = sign && not_same_sign ? -quotient_u : quotient_u;
  // 余数的符号和被除数符号一致
  assign remainder = sign && dividend[31] == 1'b1 ? -dividend_copy[31:0] : dividend_copy[31:0];

  reg [4:0] cnt;
  assign ready = !cnt;

  initial cnt = 0;

  always @(posedge clk)
    if (ready && start) begin
      cnt = 32;
      quotient_u = 0;
      dividend_copy = {32'd0, sign && dividend[31] == 1 ? -dividend : dividend};
      divider_copy = {1'b0, sign && divider[31] == 1 ? -divider : divider, 31'd0};
    end else begin
      diff = dividend_copy - divider_copy;
      quotient_u = quotient_u << 1;
      if (!diff[63]) begin
        dividend_copy = diff;
        quotient_u[0] = 1'd1;
      end
      divider_copy = divider_copy >> 1;
      cnt = cnt - 1;
    end
endmodule
