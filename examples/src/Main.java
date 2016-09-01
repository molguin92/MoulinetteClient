import java.io.IOException;
import java.util.Scanner;

/**
 * Example of a simple java program which reads and writes to standard input and output respectively.
 */

public class Main
{
    public static void main(String[] args) throws IOException
    {
        /**
         * We'll use a scanner to read lines from standard input:
         */
        Scanner in = new Scanner(System.in);

        /**
         * And we can do stuff while there's input to be analyzed.
         *
         * If you run this program "by hand", it will not end until you press Ctrl-D on the console.
         */
        while (in.hasNextLine())
        {

            /**
             * Note that the Scanner returns the input line by line in STRING format, removing the line feed
             * characters (\n). This means that if we want to work with numbers, we first have to convert the string
             * from the input into a Java number primitive.
             */

            String line = in.nextLine();    // get input from STDIN
            int i = Integer.parseInt(line); // convert it to an Integer

            /**
             * Finally, to print results to STDOUT, no conversion is needed.
             */
            System.out.println(addOne(i));
        }
    }

    public static int addOne(int i)
    {
        return i + 1;
    }
}
