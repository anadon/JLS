# Sum the integers 1..10 into x1 (= 55), then store the result to data
# memory word 0.  A classic loop: x2 is the counter, x3 the limit.
        addi x1, x0, 0        # accumulator = 0
        addi x2, x0, 1        # i = 1
        addi x3, x0, 11       # limit = 11
loop:
        add  x1, x1, x2       # acc += i
        addi x2, x2, 1        # i++
        blt  x2, x3, loop     # while i < 11
        sw   x1, 0(x0)        # store 55 to memory[0]
