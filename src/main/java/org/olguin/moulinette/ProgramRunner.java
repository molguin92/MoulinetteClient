package org.olguin.moulinette;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-16.
 * Part of org.olguin.moulinette.
 */
class ProgramRunner
{

    private String mainclassname;
    private boolean compiled;
    private File parentfolder;
    private String pathtojava;

    ProgramRunner(File mainclass, String java) throws IOException
    {
        this.mainclassname = mainclass.getName().split(".java")[0];
        this.parentfolder = mainclass.getParentFile();
        this.compiled = false;
        this.pathtojava = java;
    }

    /**
     * Compiles the program previous to running it. Sets the flag "compiled" to true if compilation was successful.
     * If not, writes error to stderr.
     *
     * @throws InterruptedException In case compilation is interrupted.
     * @throws IOException          In case of file error.
     * @throws CompileError         If no sources are found, or in case of an error in the source code.
     */
    void compile() throws InterruptedException, IOException, CompileError
    {
        // get all the source files in the folder
        File[] sources = this.parentfolder.listFiles(pathname ->
                                                     {
                                                         String name = pathname.getName();
                                                         return name.endsWith(".java");
                                                     });

        if (sources == null)
            throw new CompileError("No sources to compile.\n");


        String[] cmdarray = new String[sources.length + 1];
        cmdarray[0] = this.pathtojava + File.separator + "javac";
        for (int i = 0; i < sources.length; i++)
            cmdarray[i + 1] = sources[i].getName();

        // run compilation process
        Process compproc = Runtime.getRuntime().exec(cmdarray, null, this.parentfolder);
        BufferedInputStream stderr = new BufferedInputStream(compproc.getErrorStream());
        compproc.waitFor(10, TimeUnit.SECONDS);

        // check compilation result
        if (compproc.exitValue() != 0)
        {
            // if compilation was unsuccessful, collect STDERR output and throw a CompileError exception containing
            // this information.
            BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
            String err = "";
            String line;
            while ((line = reader.readLine()) != null)
            {
                err += line + "\n";
            }

            stderr.close();
            reader.close();

            throw new CompileError(err);
        }

        stderr.close();
        compiled = true;

    }

    /**
     * Runs the program handled by this ProgramRunner with the specified input. Takes also timeout and timeUnit
     * parameters to define a maximum run time for the program.
     * <p>
     * If the selected program is not compiled, this method throws a ProgramNotCompiled exception, and if the program
     * presents some runtime error it throws an ExecturionError.
     * <p>
     * Finally, if everything goes well, this method returns the output given by the program.
     * <p>
     * Note that linebreaks in the input are localized before being handed to the program, and they are standardized
     * to LF in the output string.
     * <p>
     * Also, in case of a ExecutionError, detailed information about the error is NOT given to avoid cheating.
     *
     * @param test_input The test input to be handed to the program through STDIN.
     * @param timeout    The timeout after which the program is to be interrupted.
     * @param timeUnit   Time unit for the timeout.
     * @return The program output in case of a successful run.
     * @throws ProgramNotCompiled   If the program has not been previously compiled.
     * @throws IOException          If problems arise while communicating with the program.
     * @throws InterruptedException If the program exceeds its timeout.
     * @throws ExecutionError       In case of a runtime error in the program.
     */
    String run(String test_input, int timeout, TimeUnit timeUnit)
            throws ProgramNotCompiled, IOException, InterruptedException, ExecutionError
    {
        if (!compiled)
        {
            throw new ProgramNotCompiled();
        }

        // LF -> CRLF
        test_input = localizeLinefeed(test_input);

        // run the program
        Process proc = Runtime.getRuntime()
                              .exec(new String[]{this.pathtojava + File.separator + "java", this.mainclassname,
                                                 "-classpath", this.parentfolder.getCanonicalPath()}, null,
                                    this.parentfolder);

        // set up communication streams
        BufferedInputStream stderr_stream = new BufferedInputStream(proc.getErrorStream());
        BufferedInputStream stdout_stream = new BufferedInputStream(proc.getInputStream());
        BufferedOutputStream stdin_stream = new BufferedOutputStream(proc.getOutputStream());

        BufferedReader stderr = new BufferedReader(new InputStreamReader(stderr_stream));
        BufferedReader stdout = new BufferedReader(new InputStreamReader(stdout_stream));
        BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(stdin_stream));

        StringBuilder outbuilder = new StringBuilder();

        stdin.write(test_input);

        Thread t = new Thread(() ->
                              {
                                  String line;
                                  try
                                  {
                                      while ((line = stdout.readLine()) != null)
                                      {
                                          outbuilder.append(line).append("\n");
                                      }
                                  }
                                  catch (IOException e)
                                  {
                                      e.printStackTrace();
                                      System.exit(-1);
                                  }
                              });

        t.start();
        stdin.close(); // <-- EOF

        String error = "";

        if (!proc.waitFor(timeout, timeUnit))
        {
            error = String.format("Timeout exceeded (%d seconds)!", timeout);
            throw new ExecutionError(error);
        }
        else if (proc.exitValue() != 0)
        {
            String line;
            while ((line = stderr.readLine()) != null)
                error += line + System.getProperty("line.separator");
            throw new ExecutionError(error);
        }

        t.join();

        stderr.close();
        stderr_stream.close();

        stdout.close();
        stdout_stream.close();

        stdin_stream.close();

        // CRLF -> LF
        return standardizeLinefeed(outbuilder.toString());
    }

    /**
     * Converts a localized string to a string with linux linebreaks.
     *
     * @param in The string to standardize.
     * @return A string with standardized line endings.
     */
    private static String standardizeLinefeed(String in)
    {
        return in.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }

    /**
     * Converts a standardized string to a string with localized linebreaks.
     *
     * @param in The string to localize.
     * @return A string with localized line endings.
     */
    private static String localizeLinefeed(String in)
    {
        return in.replaceAll("\\n", System.getProperty("line.separator"));
    }


    class CompileError extends Exception
    {
        String stderr;

        CompileError(String stderr)
        {
            super();
            this.stderr = stderr;
        }
    }

    class ProgramNotCompiled extends Exception
    {
        ProgramNotCompiled()
        {
            super();
        }
    }

    class ExecutionError extends Exception
    {
        private String stderr;

        ExecutionError(String stderr)
        {
            super();
            this.stderr = stderr;
        }

        String getErrorStub()
        {
            String[] split = stderr.split("\\r\\n|\\n|\\r");
            return split[0] + System.getProperty("line.separator") +
                    "\t..." + System.getProperty("line.separator") +
                    split[split.length - 1] + System.getProperty("line.separator");
        }
    }

}
