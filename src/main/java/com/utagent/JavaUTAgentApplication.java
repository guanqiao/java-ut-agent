package com.utagent;

import com.utagent.cli.CLICommand;
import picocli.CommandLine;

public class JavaUTAgentApplication implements Runnable {
    
    @Override
    public void run() {
        System.out.println("Java UT Agent - AI-powered JUnit 5 Test Generator");
        System.out.println("Use --help for usage information");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLICommand()).execute(args);
        System.exit(exitCode);
    }
}
