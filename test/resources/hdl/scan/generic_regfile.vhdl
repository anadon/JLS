-- Header-scanner corpus (issue #63): a VHDL entity with generics used
-- in port widths, every supported mode, and both range directions.
library ieee;
use ieee.std_logic_1164.all;

entity generic_regfile is
  generic (
    WIDTH : integer := 16;
    DEPTH : positive := 8;
    ABITS : natural := 3
  );
  port (
    clk     : in std_logic;
    we      : in std_logic;
    waddr   : in std_logic_vector(ABITS - 1 downto 0);
    wdata   : in std_logic_vector(WIDTH - 1 downto 0);
    raddr   : in std_logic_vector(ABITS - 1 downto 0);
    rdata   : out std_logic_vector(WIDTH - 1 downto 0);
    status  : buffer std_logic;
    scan_io : inout std_logic_vector(0 to DEPTH - 1)
  );
end entity generic_regfile;
