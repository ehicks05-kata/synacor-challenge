import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SynacorDemo
{
    public static void main(String[] args)
    {
        if (args.length > 0)
            loadSaveGame(args[0]);
        else
        {
            SynacorVM vm = new SynacorVM();
            vm.memory = loadProgram();
            vm.run();
        }
    }

    private static void loadSaveGame(String filename)
    {
        try
        {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
            SynacorVM vm = (SynacorVM) ois.readObject();
            vm.run();
        }
        catch (IOException | ClassNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static int[] loadProgram()
    {
        int[] program = new int[32768];
        byte[] programBytes = new byte[0];
        try
        {
            programBytes = Files.readAllBytes(Paths.get("challenge.bin"));
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(programBytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int i = 0;
        while (byteBuffer.hasRemaining())
        {
            Short myShort = byteBuffer.getShort();
            int myInt = myShort.intValue();
            if (myInt < 0)
            {
                myInt *= -1;
                int diff = 32768 - myInt;
                myInt += diff * 2;
            }
            program[i] = myInt;
            i++;
        }
        return program;
    }

    private static void dumpMemory(SynacorVM vm)
    {
        System.out.printf("  %5d-%5d:", 0, 10);
        for (int i = 0; i < vm.memory.length; i++)
        {
            if (i > 0 && i % 10 == 0)
            {
                System.out.println();
                System.out.printf("  %5d-%5d:", i, i + 10);
            }
            System.out.printf("  %7d", vm.memory[i]);
        }
    }
}
