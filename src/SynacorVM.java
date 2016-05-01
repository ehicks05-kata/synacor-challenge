import java.util.*;

public class SynacorVM
{
    int[] memory = new int[32768];
    private int[] registers = new int[8];
    private Stack<Integer> stack = new Stack<>();
    private List<String> log = new ArrayList<>();
    private static List<String> commandNames = Arrays.asList("halt", "set", "push", "pop", "eq", "gt", "jmp", "jt",
            "jf", "add", "mult", "mod", "and", "or", "not", "rmem", "wmem", "call", "ret", "out", "in", "noop");

    public void run()
    {
        int commandIndex = 0;
        while (commandIndex > -1)
        {
            final int COMMAND = memory[commandIndex];
            final int a = memory[commandIndex + 1];
            final int b = memory[commandIndex + 2];
            final int c = memory[commandIndex + 3];
            addLogEntry(commandIndex, COMMAND, a, b, c);

            if (COMMAND > 21)
                System.out.println();

            if (COMMAND ==  0) halt();
            if (COMMAND ==  1) set(a, b);
            if (COMMAND ==  2) push(a);
            if (COMMAND ==  3) pop(a);
            if (COMMAND ==  4) eq(a, b, c);
            if (COMMAND ==  5) gt(a, b, c);
            if (COMMAND ==  6) commandIndex = jmp(a);
            if (COMMAND ==  7) commandIndex = jt(commandIndex, a, b);
            if (COMMAND ==  8) commandIndex = jf(commandIndex, a, b);
            if (COMMAND ==  9) add(a, b, c);
            if (COMMAND == 10) mult(a, b, c);
            if (COMMAND == 11) mod(a, b, c);
            if (COMMAND == 12) and(a, b, c);
            if (COMMAND == 13) or(a, b, c);
            if (COMMAND == 14) not(a, b);
            if (COMMAND == 15) rmem(a, b);
            if (COMMAND == 16) wmem(a, b);
            if (COMMAND == 17) commandIndex = call(commandIndex, a);
            if (COMMAND == 18) commandIndex = ret();
            if (COMMAND == 19) out(a);
            if (COMMAND == 20) in(a);
            if (COMMAND == 21) noop();

            if (Arrays.asList(0, 6, 7, 8, 17, 18).contains(COMMAND)) commandIndex += 0;
            if (Arrays.asList(21).contains(COMMAND)) commandIndex += 1;
            if (Arrays.asList(2, 3, 19, 20).contains(COMMAND)) commandIndex += 2;
            if (Arrays.asList(1, 14, 15, 16).contains(COMMAND)) commandIndex += 3;
            if (Arrays.asList(4, 5, 9, 10, 11, 12, 13).contains(COMMAND)) commandIndex += 4;
        }
    }

    private void addLogEntry(int commandIndex, int COMMAND, int a, int b, int c)
    {
        String logEntry = String.format("%5s %4s: %5s %5s %5s", commandIndex, commandNames.get(COMMAND), a, b, c);
        logEntry += COMMAND == 19 ? String.format(" %1s", getCharacterFromAsciiCode(a)) : "  ";

        logEntry += "  |  ";
        for (Integer integer : registers)
            logEntry += String.format("%5s ", integer);

        logEntry += "  |  ";
        for (Integer integer : stack)
            logEntry += String.format("%5s ", integer);
        log.add(logEntry);
    }

    private void saveValue(int address, int value)
    {
        saveValue(address, value, false);
    }

    private void saveValue(int address, int value, boolean isRegisterAddress)
    {
        if (value > 32775)
            value = value % 32768;

        boolean isRegister = address > 32767 || isRegisterAddress;
        if (isRegister)
        {
            if (address > 8)
                address -= 32768;
            else
                address -= 1;
            registers[address] = value;
        }
        else
            memory[address] = value;
    }

    private int getValue(int value)
    {
        return getValue(value, false);
    }

    private int getValue(int value, boolean getFromMemory)
    {
        if (value > 32775)
            value = value % 32768;
        if (value <= 32767)
        {
            if (getFromMemory)
                return memory[value];
            else
                return value;
        }

        // convert literal to register
        value -= 32768;
        if (value <= 7)
            return registers[value];

        return 0;
    }

    private boolean isRegister(int value)
    {
        return ((value >= 1 && value <= 8) || (value >= 32768 && value <= 32775));
    }

    // ---- OPCODES ----

    /**
     * halt: 0 <br>
     * stop execution and terminate the program
     */
    private void halt()
    {
        System.out.println("\r\nCommands Log:");
        for (String logEntry : log)
            System.out.println(logEntry.replaceAll("\n", " "));

        System.exit(0);
    }

    /**
     * set: 1 a b <br>
     * set register 'a' to the value of 'b'
     */
    private void set(int a, int b)
    {
        if (!isRegister(a))
        {
            System.out.println("Tried to set to a non-register address");
            halt();
        }

        saveValue(a, getValue(b), true);
    }

    /**
     * push: 2 a <br>
     * push 'a' onto the stack
     */
    private void push(int a)
    {
        stack.push(getValue(a));
    }

    /**
     * pop: 3 a <br>
     * remove the top element from the stack and write it into 'a'; empty stack = error
     */
    private void pop(int a)
    {
        saveValue(a, stack.pop());
    }

    /**
     * eq: 4 a b c <br>
     * set 'a' to 1 if 'b' is equal to 'c'; set it to 0 otherwise
     */
    private void eq(int a, int b, int c)
    {
        boolean eq = getValue(b) == getValue(c);
        saveValue(a, eq ? 1 : 0);
    }

    /**
     * gt: 5 a b c <br>
     * set 'a' to 1 if 'b' is greater than 'c'; set it to 0 otherwise
     */
    private void gt(int a, int b, int c)
    {
        boolean gt = getValue(b) > getValue(c);
        saveValue(a, gt ? 1 : 0);
    }

    /**
     * jmp: 6 a <br>
     * jump to 'a'
     */
    private int jmp(int a)
    {
        return getValue(a);
    }

    /**
     * jt: 7 a b <br>
     * if 'a' is nonzero, jump to 'b'
     */
    private int jt(int i, int a, int b)
    {
        if (getValue(a) != 0)
            i = getValue(b);
        else
            i += 3;
        return i;
    }

    /**
     * jf: 8 a b <br>
     * if 'a' is zero, jump to 'a'
     */
    private int jf(int i, int a, int b)
    {
        if (getValue(a) == 0)
            i = getValue(b);
        else
            i += 3;
        return i;
    }

    /**
     * add: 9 a b c <br>
     * assign into 'a' the sum of 'b' and 'c' (modulo 32768)
     */
    private void add(int a, int b, int c)
    {
        int sum = getValue(b) + getValue(c);
        sum %= 32768;
        saveValue(a, sum);
    }

    /**
     * mult: 10 a b c <br>
     * store into 'a' the product of 'b' and 'c' (modulo 32768)
     */
    private void mult(int a, int b, int c)
    {
        int product = getValue(b) * getValue(c);
        product = product % 32768;
        saveValue(a, product);
    }

    /**
     * mod: 11 a b c <br>
     * store into 'a' the remainder of 'b' divided by 'c'
     */
    private void mod(int a, int b, int c)
    {
        int mod = getValue(b) % getValue(c);
        saveValue(a, mod);
    }

    /**
     * and: 12 a b c <br>
     * stores into 'a' the bitwise and of 'b' and 'c'
     */
    private void and(int a, int b, int c)
    {
        int and = getValue(b) & getValue(c);
        saveValue(a, and);
    }

    /**
     * or: 13 a b c <br>
     * stores into 'a' the bitwise or of 'b' and 'c'
     */
    private void or(int a, int b, int c)
    {
        int or = getValue(b) | getValue(c);
        saveValue(a, or);
    }

    /**
     * not: 14 a b <br>
     * stores 15-bit bitwise inverse of 'b' in 'a'
     */
    private void not(int a, int b)
    {
        int mask = Integer.parseInt("00000000000000000111111111111111", 2);

        int not = ~getValue(b);

        not = not & mask;

        saveValue(a, not);
    }

    /**
     * rmem: 15 a b <br>
     * read memory at address 'b' and write it to 'a'
     */
    private void rmem(int a, int b)
    {
        int value = getValue(b, true);

        if (isRegister(b))
            value = getValue(value, true);

        saveValue(a, value);
    }

    /**
     * wmem: 16 a b <br>
     * write the value from 'b' into memory at address 'a'
     */
    private void wmem(int a, int b)
    {
        int value = getValue(b);

        int writeToAddress = a;
        if (isRegister(a))
            writeToAddress = getValue(a);

        saveValue(writeToAddress, value);
    }

    /**
     * call: 17 a <br>
     * write the address of the next instruction to the stack and jump to 'a'
     */
    private int call(int i, int a)
    {
        stack.push(i + 2);
        i = getValue(a);
        return i;
    }

    /**
     * ret: 18 <br>
     * remove the top element from the stack and jump to it; empty stack = halt
     */
    private int ret()
    {
        int i;
        int value = stack.pop();
        i = getValue(value);
        return i;
    }

    /**
     * out: 19 a <br>
     * write the character represented by ascii code 'a' to the terminal
     */
    private void out(int a)
    {
        System.out.print(getCharacterFromAsciiCode(a));
    }

    private char getCharacterFromAsciiCode(int a)
    {
        return (char) getValue(a);
    }

    /**
     * in: 20 a <br>
     * read a character from the terminal and write its ascii code to 'a';
     * it can be assumed that once input starts,
     * it will continue until a newline is encountered;
     * this means that you can safely read whole lines from the keyboard and
     * trust that they will be fully read
     */
    List<Character> charBuffer = new ArrayList<>();
    private void in(int a)
    {
        int value;
        if (charBuffer.size() == 0)
        {
            System.out.println("$ ");
            String in = new Scanner(System.in).nextLine();
            for (Character aChar : in.toCharArray())
                charBuffer.add(aChar);
            charBuffer.add('\n');
        }

        value = charBuffer.remove(0);
        saveValue(a, value);
    }

    /**
     * noop: 21 <br>
     * no operation
     */
    private void noop()
    {

    }
}