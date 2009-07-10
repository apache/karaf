package org.apache.felix.karaf.gshell.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.karaf.gshell.console.OsgiCommandSupport;

@Command(scope = "shell", name = "echo", description="Echo or print arguments to STDOUT")
public class EchoCommand extends OsgiCommandSupport
{
    @Option(name="-n", description="Do not print the trailing newline character")
    private boolean trailingNewline = true;

    @Argument(description="Arguments")
    private List<String> args;

    protected Object doExecute() throws Exception {
        if (args != null) {
            int c=0;

            for (String arg : args) {
                System.out.print(arg);
                if (++c + 1 < args.size()) {
                    System.out.print(" ");
                }
            }
        }

        if (trailingNewline) {
            System.out.println();
        }

        return null;
    }
}