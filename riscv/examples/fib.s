# Compute the first several Fibonacci numbers, leaving fib(10)=55 in x5,
# and store fib(1..8) into consecutive data-memory words.
        addi x1, x0, 0        # a = 0
        addi x2, x0, 1        # b = 1
        addi x6, x0, 0        # store pointer (byte offset)
        addi x7, x0, 10       # count
loop:
        add  x5, x1, x2       # next = a + b
        sw   x2, 0(x6)        # mem[ptr] = b
        addi x6, x6, 4        # ptr += 4
        mv   x1, x2           # a = b
        mv   x2, x5           # b = next
        addi x7, x7, -1
        blt  x0, x7, loop     # while count > 0
