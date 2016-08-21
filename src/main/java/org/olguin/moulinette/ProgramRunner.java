package org.olguin.moulinette;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 2016-08-16.
 * Part of org.olguin.moulinette.
 */
public class ProgramRunner {

    private String mainclassname;
    private String pathtofolder;
    private boolean compiled;

    private String pathtojava;

    public ProgramRunner(File mainclass, String java) throws IOException {
        this.mainclassname = mainclass.getName().split(new String(".java"))[0];
        this.pathtofolder = mainclass.getParentFile().getCanonicalPath();
        this.compiled = false;
        this.pathtojava = java;
    }

    /**
     * Compiles the program previous to running it. Sets the flag "compiled" to true if compilation was succesful.
     * If not, writes error to stderr.
     */
    public void compile() {
        try {
            File folder = new File(this.pathtofolder);
            File[] sources = folder.listFiles(pathname -> {
                String name = pathname.getName();
                return name.substring(name.lastIndexOf('.')).equals(".java");
            });

            String src = "";
            for(File f: sources)
                src += this.pathtofolder + File.separator + f.getName() + " ";

            Process compproc = Runtime.getRuntime().exec(this.pathtojava + File.separator + "javac " + src);
            BufferedInputStream stderr = new BufferedInputStream(compproc.getErrorStream());
            compproc.waitFor(10, TimeUnit.SECONDS);

            if (compproc.exitValue() != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
                String err = "";
                String line;
                while ((line = reader.readLine()) != null) {
                    err += line + "\n";
                }

                stderr.close();
                reader.close();

                throw new CompileError(err);
            }

            stderr.close();
            compiled = true;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (CompileError compileError) {
            System.err.print(compileError.stderr);
        }

    }


    public String run(String test_input, int timeout, TimeUnit timeUnit) throws ProgramNotCompiled, IOException, InterruptedException, ExecutionError {
        if (!compiled) {
            throw new ProgramNotCompiled();
        }

        Process proc = Runtime.getRuntime().exec(this.pathtojava + File.separator + "java "
                + "-classpath " + this.pathtofolder
                + " " + this.mainclassname);
        BufferedInputStream stderr_stream = new BufferedInputStream(proc.getErrorStream());
        BufferedInputStream stdout_stream = new BufferedInputStream(proc.getInputStream());
        BufferedOutputStream stdin_stream = new BufferedOutputStream(proc.getOutputStream());

        BufferedReader stderr = new BufferedReader(new InputStreamReader(stderr_stream));
        BufferedReader stdout = new BufferedReader(new InputStreamReader(stdout_stream));
        BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(stdin_stream));

        stdin.write(test_input, 0, test_input.length());
        stdin.flush();
        stdin.close(); // <-- EOF

        proc.waitFor(timeout, timeUnit);

        if (proc.exitValue() != 0) {
            String line;
            while((line = stderr.readLine()) != null)
                System.err.println(line);
            throw new ExecutionError();
        }

        String out = "";
        String line;
        while ((line = stdout.readLine()) != null)
        {
            out += line + "\n";
        }

        stderr.close();
        stderr_stream.close();

        stdout.close();
        stdout_stream.close();

        stdin_stream.close();

        return out;
    }


    public class CompileError extends Exception {
        String stderr;

        CompileError(String stderr) {
            super();
            this.stderr = stderr;
        }
    }

    public class ProgramNotCompiled extends Exception {
        public ProgramNotCompiled() {
            super();
        }
    }

    public class ExecutionError extends Exception {
        public ExecutionError() {
            super();
        }
    }

}
