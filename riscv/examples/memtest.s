# Exercise loads/stores and arithmetic: store two values, read them back,
# and compute their sum and difference.
        addi x1, x0, 0x123
        addi x2, x0, 0x456
        sw   x1, 0(x0)
        sw   x2, 4(x0)
        lw   x3, 0(x0)
        lw   x4, 4(x0)
        add  x5, x3, x4       # 0x579
        sub  x6, x4, x3       # 0x333
        sw   x5, 8(x0)
